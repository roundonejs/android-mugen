# The ARMv7 is significanly faster due to the use of the hardware FPU
APP_ABI := armeabi-v7a x86 arm64-v8a x86_64
APP_STL := stlport_static

APP_PLATFORM := android-9
APP_OPTIM := release

NDK_TOOLCHAIN_VERSION := 4.9
