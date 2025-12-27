#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
const char* strchr(const char* str, int character);
const char* strpbrk(const char* str1, const char* str2);
const char* strrchr(const char* str, int character);
const void* memchr(const void* ptr, int value, size_t num);
const char* strstr(const char* str1, const char* str2);
*/
char* strchr(const char* str, int character);
char* strpbrk(const char* str1, const char* str2);
char* strrchr(const char* str, int character);
void* memchr(const void* ptr, int value, size_t num);
char* strstr(const char* str1, const char* str2);
void* memset(void* ptr, int value, size_t num);
void* memcpy(void* destination, const void* source, size_t num);
void* memmove(void* destination, const void* source, size_t num);
int strcoll(const char* str1, const char* str2);
char* strcpy(char* destination, const char* source);
char* strncpy(char* destination, const char* source, size_t num);
char* strcat(char* destination, const char* source);
char* strncat(char* destination, const char* source, size_t num);
int memcmp(const void* ptr1, const void* ptr2, size_t num);
int strcmp(const char* str1, const char* str2);
int strncmp(const char* str1, const char* str2, size_t num);
size_t strxfrm(char* destination, const char* source, size_t num);
size_t strcspn(const char* str1, const char* str2);
size_t strspn(const char* str1, const char* str2);
char* strtok(char* str, const char* delimiters);
char* strerror(int errnum);
size_t strlen(const char* str);

#ifdef __cplusplus
}
#endif
