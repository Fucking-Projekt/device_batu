# Clone extra repos if missing
smart_clone() {
    local url=$1; local path=$2; local branch=$3
    if [ ! -d "$path" ]; then
        if [ -n "$branch" ]; then git clone "$url" -b "$branch" "$path"; else git clone "$url" "$path"; fi
    fi
}
smart_clone https://github.com/swiitch-OFF-Lab/hardware_dolby hardware/dolby sony-A17
smart_clone https://github.com/LineageOS/android_hardware_xiaomi hardware/xiaomi lineage-24.0
smart_clone https://github.com/Fucking-Projekt/kernel_batu kernel/xiaomi/stone
smart_clone https://github.com/Fucking-Projekt/vendor_batu vendor/xiaomi/stone
