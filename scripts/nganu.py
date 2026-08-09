import os, re, sys

def get_content(p):
    with open(p, "r") as f: return f.read()
def put_content(p, c):
    with open(p, "w") as f: f.write(c)

def generate_overrides_mk():
    path = "device/xiaomi/stone/lineage_stone.mk"
    if not os.path.exists(path): return
    content = get_content(path)
    match = re.search(r"PRODUCT_BUILD_PROP_OVERRIDES \+= \\(.*?)\n\n", content, re.DOTALL)
    if not match: match = re.search(r"PRODUCT_BUILD_PROP_OVERRIDES \+= \\(.*)$", content, re.DOTALL)
    
    if match:
        mk_vars = []
        for line in match.group(1).strip().split("\n"):
            line = line.strip().rstrip("\\").strip()
            if "=" in line:
                key, val = line.split("=", 1)
                v = val.strip().replace('"', '').replace("'", "")
                mk_vars.append(f"{key.strip()} := {v}")
        put_content("device/xiaomi/stone/overrides.mk", "# Auto-generated\n" + "\n".join(mk_vars) + "\n")
        
        git_ignore = "device/xiaomi/stone/.gitignore"
        if os.path.exists(git_ignore):
            if "overrides.mk" not in get_content(git_ignore):
                with open(git_ignore, "a") as f: f.write("\noverrides.mk\n")
        else:
            put_content(git_ignore, "overrides.mk\n")

def patch_product_mk():
    p = "build/make/core/product.mk"
    if not os.path.exists(p): return
    c = get_content(p)
    target = "PRODUCT_SYSTEM_MANUFACTURER"
    extra = "\n\n_product_single_value_vars += PRODUCT_VENDOR_NAME PRODUCT_VENDOR_MODEL PRODUCT_VENDOR_DEVICE PRODUCT_VENDOR_BRAND PRODUCT_VENDOR_MANUFACTURER PRODUCT_PRODUCT_NAME PRODUCT_PRODUCT_MODEL PRODUCT_PRODUCT_DEVICE PRODUCT_PRODUCT_BRAND PRODUCT_PRODUCT_MANUFACTURER PRODUCT_SYSTEM_EXT_NAME PRODUCT_SYSTEM_EXT_MODEL PRODUCT_SYSTEM_EXT_DEVICE PRODUCT_SYSTEM_EXT_BRAND PRODUCT_SYSTEM_EXT_MANUFACTURER PRODUCT_ODM_NAME PRODUCT_ODM_MODEL PRODUCT_ODM_DEVICE PRODUCT_ODM_BRAND PRODUCT_ODM_MANUFACTURER"
    if target in c and "PRODUCT_VENDOR_NAME" not in c:
        put_content(p, c.replace(target, target + extra))

def patch_product_config_mk():
    p = "build/make/core/product_config.mk"
    if not os.path.exists(p): return
    c = get_content(p)
    
    # We NO LONGER inject "export TZ" here because AOSP forbids it.
    # It is handled in vendorsetup.sh instead.

    # Helper and Overrides include
    helper = """
# Include custom overrides
-include device/xiaomi/stone/overrides.mk

AX_PRE_system := Sys
AX_PRE_vendor := Vdr
AX_PRE_product := Pdt
AX_PRE_system_ext := Sex
AX_PRE_odm := Odm

# Exhaustive branding lookup with strict partition priority
define ax-get-branding
$(strip $(eval _p := $(AX_PRE_$(1)))\\
$(if $(filter Brand,$(2)),$(if $($(_p)ProductBrand),$($(_p)ProductBrand),$(if $($(_p)Brand),$($(_p)Brand),$(3))),\\
$(if $(filter Device,$(2)),$(if $($(_p)DeviceName),$($(_p)DeviceName),$(if $($(_p)Device),$($(_p)Device),$(3))),\\
$(if $(filter Manufacturer,$(2)),$(if $($(_p)ProductManufacturer),$($(_p)ProductManufacturer),$(if $($(_p)Manufacturer),$($(_p)Manufacturer),$(3))),\\
$(if $(filter Model,$(2)),$(if $($(_p)ProductModel),$($(_p)ProductModel),$(if $($(_p)Model),$($(_p)Model),$(3))),\\
$(if $(filter Name,$(2)),$(if $($(_p)DeviceProduct),$($(_p)DeviceProduct),$(if $($(_p)Name),$($(_p)Name),$(3))),$(3)))))))
endef
"""
    target = "PRODUCT_SYSTEM_MANUFACTURER := $(PRODUCT_MANUFACTURER)\nendif"
    if "define ax-get-branding" not in c:
        c = c.replace(target, target + "\n" + helper)
    
    c = c.replace("PRODUCT_OTA_ENFORCE_VINTF_KERNEL_REQUIREMENTS := true", "PRODUCT_OTA_ENFORCE_VINTF_KERNEL_REQUIREMENTS := false")
    put_content(p, c)

def patch_sysprop_mk():
    p = "build/make/core/sysprop.mk"
    if not os.path.exists(p): return
    lines = get_content(p).splitlines()
    new_lines = []
    i = 0
    while i < len(lines):
        l = lines[i]
        # Inject TZ=Asia/Jakarta to any inline date calls if they exist
        l = l.replace("`date ", "`TZ=Asia/Jakarta date ")
        
        if 'ro.product.$(1).brand=$(PRODUCT_BRAND)' in l:
            new_lines.append('    echo "ro.product.$(1).brand=$(call ax-get-branding,$(1),Brand,$(PRODUCT_BRAND))" >> $(2);\\')
            new_lines.append('    echo "ro.product.$(1).device=$(call ax-get-branding,$(1),Device,$(PRODUCT_DEVICE))" >> $(2);\\')
            new_lines.append('    echo "ro.product.$(1).manufacturer=$(call ax-get-branding,$(1),Manufacturer,$(PRODUCT_MANUFACTURER))" >> $(2);\\')
            new_lines.append('    echo "ro.product.$(1).model=$(call ax-get-branding,$(1),Model,$(PRODUCT_MODEL))" >> $(2);\\')
            new_lines.append('    echo "ro.product.$(1).name=$(call ax-get-branding,$(1),Name,$(PRODUCT_NAME))" >> $(2);\\')
            i += 5; continue

        if 'if [[ $(BUILD_NUMBER_FROM_FILE) =~ ^eng\\. ]]; then \\' in l:
            new_lines.append('    if [ -n "$(BuildNumber)" ]; then \\')
            new_lines.append('        echo "ro.$(1).build.version.incremental=$(BuildNumber)" >> $(2);\\')
            new_lines.append('    elif [[ $(BUILD_NUMBER_FROM_FILE) =~ ^eng\\. ]]; then \\')
            new_lines.append('        echo "ro.$(1).build.version.incremental=`$(DATE_FROM_FILE) +%s`" >> $(2);\\')
            new_lines.append('    else \\')
            new_lines.append('        echo "ro.$(1).build.version.incremental=$(BUILD_NUMBER_FROM_FILE)" >> $(2);\\')
            new_lines.append('    fi; \\')
            i += 5; continue

        new_lines.append(l); i += 1
    put_content(p, "\n".join(new_lines) + "\n")

def patch_gen_build_prop():
    p = "build/soong/scripts/gen_build_prop.py"
    if not os.path.exists(p): return
    c = get_content(p)
    # Bypass validator
    c = re.sub(r"if key not in config:.*?sys\.exit\(1\)", "if key not in config: config[key] = value", c, flags=re.DOTALL)
    
    # WIB Hardcode: ensure date uses Asia/Jakarta
    # Since vendorsetup exports TZ, we don't strictly need to pass env here, but let's be safe.
    c = re.sub(r'config\["Date"\] = subprocess\.check_output\(\["date", "-u?d", f"@{raw_date}".*?\)\.strip\(\)', 
               'config["Date"] = subprocess.check_output(["date", "-d", f"@{raw_date}"], env={**os.environ, "TZ": "Asia/Jakarta"}, text=True).strip()', c)

    if "partition_map =" not in c:
        lines = c.splitlines()
        new_lines = []
        mapping = {"system": "Sys", "system_ext": "Sex", "vendor": "Vdr", "product": "Pdt", "odm": "Odm"}
        for l in lines:
            if "return args" in l:
                new_lines.append("  override_config(config)")
                new_lines.append(f"  partition_map = {mapping}")
                new_lines.append("  p_pre = partition_map.get(args.partition, \"Unknown\")")
                new_lines.append("  for prop in [\"Brand\", \"Device\", \"Manufacturer\", \"Model\", \"Name\"]:")
                new_lines.append("    target = \"Product\" + prop if prop in [\"Brand\", \"Manufacturer\", \"Model\"] else (\"DeviceProduct\" if prop == \"Name\" else \"DeviceName\")")
                new_lines.append("    search_keys = [p_pre+\"Product\"+prop, p_pre+\"Device\"+prop, p_pre+prop, p_pre+\"DeviceName\"]")
                new_lines.append("    for k in search_keys:")
                new_lines.append("      if k in config:")
                new_lines.append("        config[target] = config[k]")
                new_lines.append("        if prop == \"Device\": config[\"DeviceName\"] = config[k]")
                new_lines.append("        break")
            new_lines.append(l)
        c = "\n".join(new_lines) + "\n"
    put_content(p, c)

def patch_soong_config_go():
    p = "build/soong/android/config.go"
    if not os.path.exists(p): return
    c = get_content(p)
    target = 'if strings.HasPrefix(defaultCert, "vendor/evolution-priv/") {'
    replacement = 'if strings.HasPrefix(defaultCert, "vendor/evolution-priv/") || strings.HasPrefix(defaultCert, "vendor/lineage-priv/") {'
    if "vendor/lineage-priv/" not in c and target in c:
        put_content(p, c.replace(target, replacement))

def patch_make_config_mk():
    p = "build/make/core/config.mk"
    if not os.path.exists(p): return
    c = get_content(p)
    target = 'else ifneq ($(filter vendor/evolution-priv/%,$(DEFAULT_SYSTEM_DEV_CERTIFICATE)),)'
    replacement = 'else ifneq ($(filter vendor/evolution-priv/% vendor/lineage-priv/%,$(DEFAULT_SYSTEM_DEV_CERTIFICATE)),)'
    if "vendor/lineage-priv/" not in c and target in c:
        put_content(p, c.replace(target, replacement))

if __name__ == "__main__":
    generate_overrides_mk()
    patch_product_mk()
    patch_product_config_mk()
    patch_sysprop_mk()
    patch_gen_build_prop()
    patch_soong_config_go()
    patch_make_config_mk()
    print("NGANU_SUCCESS")
