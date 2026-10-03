#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <libgen.h>
#include <errno.h>
#include <android/log.h>

#define TAG "ZakuroCC"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

int main(int argc, char **argv) {
    if (argc >= 2 && (strcmp(argv[1], "--version") == 0 || strcmp(argv[1], "-v") == 0)) {
        printf("cc (Zakuro Android AArch64 C Compiler & Linker)\n");
        return 0;
    }

    char exe_path[1024];
    ssize_t len = readlink("/proc/self/exe", exe_path, sizeof(exe_path) - 1);
    if (len <= 0) {
        strcpy(exe_path, ".");
    } else {
        exe_path[len] = '\0';
    }
    char *dir = dirname(exe_path);

    // Keep nativeLibraryDir in LD_LIBRARY_PATH so libmold.so finds libzstd.so and libc++_shared.so
    char ld_path[2048];
    const char *old_ld = getenv("LD_LIBRARY_PATH");
    if (old_ld && old_ld[0]) {
        snprintf(ld_path, sizeof(ld_path), "%s:%s:/system/lib64", dir, old_ld);
    } else {
        snprintf(ld_path, sizeof(ld_path), "%s:/system/lib64", dir);
    }
    setenv("LD_LIBRARY_PATH", ld_path, 1);

    char tcc_path[1024];
    snprintf(tcc_path, sizeof(tcc_path), "%s/libtcc.so", dir);

    char mold_path[1024];
    snprintf(mold_path, sizeof(mold_path), "%s/libmold.so", dir);

    const char *data_dir = getenv("ZAKURO_DATA_DIR");
    if (!data_dir || !data_dir[0]) {
        data_dir = "/data/data/com.fearkov.zakuro/files";
    }

    char inc_tcc[1024], inc_ndk[1024], inc_arch[1024], compat_obj[1024];
    snprintf(inc_tcc, sizeof(inc_tcc), "-I%s/toolchain/tcc_include", data_dir);
    snprintf(inc_ndk, sizeof(inc_ndk), "-I%s/toolchain/include", data_dir);
    snprintf(inc_arch, sizeof(inc_arch), "-I%s/toolchain/include/aarch64-linux-android", data_dir);
    snprintf(compat_obj, sizeof(compat_obj), "%s/toolchain/libtcc_compat.o", data_dir);

    int is_shared = 0;
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-shared") == 0) {
            is_shared = 1;
            break;
        }
    }

    if (is_shared) {
        // Linker mode: use mold for high-performance multi-module linking with range extension trampolines
        LOGI("Linking shared library with mold...");
        char **new_argv = malloc((argc + 16) * sizeof(char *));
        int idx = 0;
        new_argv[idx++] = mold_path;
        for (int i = 1; i < argc; i++) {
            new_argv[idx++] = argv[i];
        }
        if (access(compat_obj, R_OK) == 0) {
            new_argv[idx++] = compat_obj;
        }
        new_argv[idx++] = "-L/system/lib64";
        new_argv[idx++] = "-lc";
        new_argv[idx++] = "-lm";
        new_argv[idx] = NULL;

        execv(mold_path, new_argv);
        LOGE("execv failed for mold %s: %s", mold_path, strerror(errno));
        perror("execv mold failed");
        return 1;
    } else {
        // Compiler mode: use tcc for blazing fast compilation (58 files in 4 seconds)
        char **new_argv = malloc((argc + 16) * sizeof(char *));
        int idx = 0;
        new_argv[idx++] = tcc_path;
        new_argv[idx++] = inc_tcc;
        new_argv[idx++] = inc_ndk;
        new_argv[idx++] = inc_arch;
        for (int i = 1; i < argc; i++) {
            new_argv[idx++] = argv[i];
        }
        new_argv[idx] = NULL;

        execv(tcc_path, new_argv);
        LOGE("execv failed for tcc %s: %s", tcc_path, strerror(errno));
        perror("execv tcc failed");
        return 1;
    }
}
