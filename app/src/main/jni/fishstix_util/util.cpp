#include <jni.h>
#include <cpu-features.h>
#include <string>
#include <functional>
#include <string>
#include <cctype>
#include <stdlib.h>

#ifdef DEBUG
#include <android/log.h>
#define LOGD(LOG_TAG, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGV(LOG_TAG, ...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGE(LOG_TAG, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_TAG "DosBoxTurbo"
#endif
