# Clone extra repos if missing
smart_clone() {
    local url=$1; local path=$2; local branch=$3
    if [ ! -d "$path" ]; then
        if [ -n "$branch" ]; then git clone "$url" -b "$branch" "$path"; else git clone "$url" "$path"; fi
    fi
}
smart_clone https://github.com/kenway214/packages_apps_GameBar packages/apps/GameBar
#smart_clone https://github.com/mayuresh-sources/prebuilts_calyx_datura prebuilts/calyx/datura
smart_clone https://github.com/mayuresh-sources/hardware_dolby hardware/dolby sony-1.0
#smart_clone https://github.com/TogoFire/packages_apps_ViPER4AndroidFX packages/apps/ViPER4AndroidFX
smart_clone https://github.com/LineageOS/android_hardware_xiaomi hardware/xiaomi lineage-23.2
smart_clone https://github.com/Fucking-Projekt/kernel_batu kernel/xiaomi/stone
smart_clone https://github.com/Fucking-Projekt/vendor_batu vendor/xiaomi/stone
