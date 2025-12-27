#ifndef ANDROID_MUGEN_CONFIG_H
#define ANDROID_MUGEN_CONFIG_H

#ifdef __i386__
  #include "config_x86.h"
#elif __x86_64__
  #include "config_x86_64.h"
#elif __arm__
  #include "config_armeabi_v7a.h"
#elif __aarch64__
  #include "config_arm64_v8a.h"
#else
  #error "Architeture without configure file."
#endif

#endif
