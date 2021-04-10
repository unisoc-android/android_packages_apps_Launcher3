#!/bin/bash
#=================stable command======================#
#croot
#make clean-Launcher3 -j16
#make RunLauncherRoboTests -j16
#=================stable command======================#
PRODUCT_OUT=$(get_build_var PRODUCT_OUT)

OUT_DIR=$(get_build_var OUT_DIR)

LAUNCHER_SOURCE_DIR=packages/apps/Launcher3

BASE_DIR=$(cd `dirname -- $0` && pwd)

TEST_CASE=emptyCase

#default use mmm command
COMMAND=mmm

#default build launcher apk
BUILD_LAUNCHER=true

#default run test
RUN_TEST=true

#default run test
IS_SHOWHELP=false

#init args
if [[ -n "$1" ]]; then
    case "$1" in
    -A)
        COMMAND=mmma
        echo "will use mmma command build launcher apk"
        ;;
    -T)
        BUILD_LAUNCHER=false
        echo "will only run robo test"
        if [[ -n "$2" ]]; then
            TEST_CASE=$2
        fi
        ;;
    --h)
        echo "**********************************HELP*********************************"
        echo "* default use mmm command build launcher apk & run robo test all case.*"
        echo "* --h: show help info.                                                *"
        echo "* -A: use mmma command build launcher apk & run robo test.            *"
        echo "* -T: only run robo test all case, don't build launcher apk.          *"
        echo "* -T arg2: only run robo test arg2 case, don't build launcher apk.    *"
        echo "**********************************HELP*********************************"
        BUILD_LAUNCHER=false
        RUN_TEST=false
        IS_SHOWHELP=true
        ;;
    *)
        echo "warning: input args is error, please check the args!!!"
        BUILD_LAUNCHER=false
        RUN_TEST=false
        ;;
    esac
fi

if [[ "$IS_SHOWHELP" = "false" ]]; then
    if [[ -z "$PRODUCT_OUT" ]]; then
        echo "warning: can't find product out path, please check input! eg. use "source ./runRoboTest.sh" command"
        BUILD_LAUNCHER=false
        RUN_TEST=false
    fi
fi

if [[ "$BUILD_LAUNCHER" = "true" ]] || [[ "$RUN_TEST" = "true" ]]; then
    echo "----------------Unisoc Launcher3 RoboTest Begin----------------"

    #changes to root dir
    croot
fi

if [[ "$BUILD_LAUNCHER" = "true" ]]; then
    #clean obj
    echo "step1: clean out resource"

    #clean common obj
    COMMON_OBJ_DIR=${OUT_DIR}/target/common/obj/APPS
    rm -rf ${COMMON_OBJ_DIR}/Launcher3_intermediates
    rm -rf ${COMMON_OBJ_DIR}/Launcher3Go_intermediates
    rm -rf ${COMMON_OBJ_DIR}/Launcher3GoIconRecents_intermediates
    rm -rf ${COMMON_OBJ_DIR}/Launcher3QuickStep_intermediates
    rm -rf ${COMMON_OBJ_DIR}/Launcher3QuickStepGo_intermediates
    echo "step1.1: common obj path clean done"

    #clean product obj
    PRODUCT_OBJ_DIR=${PRODUCT_OUT}/obj/APPS
    rm -rf ${PRODUCT_OBJ_DIR}/Launcher3_intermediates
    rm -rf ${PRODUCT_OBJ_DIR}/Launcher3Go_intermediates
    rm -rf ${PRODUCT_OBJ_DIR}/Launcher3GoIconRecents_intermediates
    rm -rf ${PRODUCT_OBJ_DIR}/Launcher3QuickStep_intermediates
    rm -rf ${PRODUCT_OBJ_DIR}/Launcher3QuickStepGo_intermediates
    echo "step1.2: product obj path clean done"

    #clean apk
    APK_DIR=${PRODUCT_OUT}/product/priv-app
    rm -rf ${APK_DIR}/Launcher3
    rm -rf ${APK_DIR}/Launcher3Go
    rm -rf ${APK_DIR}/Launcher3GoIconRecents
    rm -rf ${APK_DIR}/Launcher3QuickStep
    rm -rf ${APK_DIR}/Launcher3QuickStepGo
    echo "step1.3: apk clean done"

    #build
    echo "step2: build app & running test"
    ${COMMAND} ${LAUNCHER_SOURCE_DIR} -j16
fi

if [[ "$RUN_TEST" = "true" ]]; then
    if [[ "$TEST_CASE" = "emptyCase" ]]; then
        echo "running all test case"
        make RunLauncherRoboTests -j16
    else
        echo "running test case :$TEST_CASE"
        make RunLauncherRoboTests ROBOTEST_FILTER=${TEST_CASE} -j16
    fi

    REPORT_FILE=${PRODUCT_OUT}/obj/ROBOLECTRIC/RunLauncherRoboTests_intermediates/test-output.xml
    echo "test done. test report is:$REPORT_FILE"
fi

if [[ "$BUILD_LAUNCHER" = "true" ]] || [[ "$RUN_TEST" = "true" ]]; then
    #restore to base dir
    cd ${BASE_DIR}

    echo "----------------Unisoc Launcher3 RoboTest End----------------"
fi
