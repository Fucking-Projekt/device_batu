#!/vendor/bin/sh

# Huaqin kernel driver built-in sysfs node
SYSFS_PCBA="/sys/class/huaqin/interface/hw_info/pcba_config"

# Fallback if the sysfs node is -ENOENT
if [ ! -f "$SYSFS_PCBA" ]; then
    echo "[PCBA] Sysfs node not found!"
    exit 1
fi

# Read the value of sysfs (example: PCBA_M17P_MP_GL)
PCBA_RAW=$(cat "$SYSFS_PCBA")

if [ -z "$PCBA_RAW" ]; then
    echo "[PCBA] Sysfs content is empty!"
    exit 1
fi

# Split string using IFS '_'
# Token:
# $1 = PCBA
# $2 = Project Code (M17 / M17P / M17X / K19J / K19K)
# $3 = HW Stage     (P0_1 / P1 / P2 / MP)
# $4 = Region       (CN / GL / IN / JP)
OLD_IFS="$IFS"
IFS='_'
set -- $PCBA_RAW
IFS="$OLD_IFS"

PROJECT="$2"
STAGE="$3"
REGION="$4"

# Handle if there is a P0_1 version (sliding token)
if [ "$3" = "P0" ]; then
    STAGE="P0_1"
    REGION="$5"
fi

# Parse HW Level (Stage)
case "$STAGE" in
    "MP"|"PREM")
        HWLEVEL="MP"
        REV="0"
        ;;
    "P0"*|"P0_1")
        HWLEVEL="P0"
        REV="1"
        ;;
    "P1")
        HWLEVEL="P1"
        REV="1"
        ;;
    "P2")
        HWLEVEL="P2"
        REV="2"
        ;;
    *)
        HWLEVEL="MP"
        REV="0"
        ;;
esac

# Logic Mapping based on Project & Region (according to reverse r2)
case "$PROJECT" in
    "M17P")
        case "$REGION" in
            "IN")
                HWC="IN"
                BATCH="3563B"
                TINY="19"
                MODELCERT="22111317PI"
                ;;
            "GL"|"GLOBAL")
                HWC="GLOBAL"
                BATCH="6335B"
                TINY="19"
                MODELCERT="22111317PG"
                ;;
            *)
                HWC="GLOBAL"
                BATCH="6335B"
                TINY="19"
                MODELCERT="22111317PG"
                ;;
        esac
        ;;
    "M17")
        case "$REGION" in
            "IN")
                HWC="IN"
                BATCH="3563"
                TINY="17"
                MODELCERT="22111317I"
                ;;
            "GL"|"GLOBAL")
                HWC="GLOBAL"
                BATCH="3563"
                TINY="17"
                MODELCERT="22111317G"
                ;;
            "CN")
                HWC="CN"
                BATCH="3563"
                TINY="17"
                MODELCERT="22101317C"
                ;;
            *)
                HWC="GLOBAL"
                BATCH="3563"
                TINY="17"
                MODELCERT="22111317G"
                ;;
        esac
        ;;
    "M17X")
        HWC="CN"
        BATCH="3563"
        TINY="17"
        MODELCERT="22101317C"
        ;;
        
    "K19J"|"K19K")
        if [ "$REGION" = "JP" ]; then
            HWC="JP"
            BATCH="9KJa"
            TINY="19"
            MODELCERT="22021119KR"
        else
            HWC="GLOBAL"
            BATCH="1835"
            TINY="18"
            MODELCERT="22021119G"
        fi
        ;;
    *)
        HWC="GLOBAL"
        BATCH="6335B"
        TINY="19"
        MODELCERT="22111317PG"
        ;;
esac

# Formatter HWVersion: %s.%s.%s -> BATCH.TINY.REV (example: 6335B.19.0)
HWVERSION="${BATCH}.${TINY}.${REV}"

# Set Properties
setprop ro.vendor.boot.hwc "$HWC"
setprop ro.vendor.boot.hwlevel "$HWLEVEL"
setprop ro.vendor.boot.hwversion "$HWVERSION"
setprop ro.vendor.boot.modelcert "$MODELCERT"
setprop ro.boot.hardware.revision "$HWLEVEL-$HWC-$HWVERSION"
setprop ro.hardware.revision "$HWLEVEL-$HWC-$HWVERSION"
setprop ro.boot.hwrev "$HWLEVEL-$HWC-$HWVERSION"
setprop ro.revision "$HWLEVEL-$HWC-$HWVERSION"
