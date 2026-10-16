// terminal_pty.c — governed PTY/pipe spawner for ToolNeuron's Laptop terminal effect.
//
// Strategy (modern Android, no tricks): the child executes /system/bin/sh (the
// platform shell) as the app uid, inside the app-private workspace. No bundled
// binaries, no proot, no root, no targetSdk lowering, no exec from app data.
// Deterministic cleanup: children run in their own session/process group and are
// killed via killpg + reaped via waitpid.

#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <pty.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>
#include <android/log.h>

#define TAG "tn:terminal_pty"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static char **build_vec(JNIEnv *env, jobjectArray arr) {
    jsize n = arr ? (*env)->GetArrayLength(env, arr) : 0;
    char **out = (char **)calloc((size_t)n + 1, sizeof(char *));
    if (!out) return NULL;
    for (jsize i = 0; i < n; i++) {
        jstring s = (jstring)(*env)->GetObjectArrayElement(env, arr, i);
        const char *c = (*env)->GetStringUTFChars(env, s, NULL);
        out[i] = strdup(c ? c : "");
        (*env)->ReleaseStringUTFChars(env, s, c);
        (*env)->DeleteLocalRef(env, s);
    }
    out[n] = NULL;
    return out;
}

static void free_vec(char **v) {
    if (!v) return;
    for (char **p = v; *p; p++) free(*p);
    free(v);
}

static jlongArray make_pair(JNIEnv *env, jlong a, jlong b) {
    jlongArray r = (*env)->NewLongArray(env, 2);
    if (!r) return NULL;
    jlong vals[2] = { a, b };
    (*env)->SetLongArrayRegion(env, r, 0, 2, vals);
    return r;
}

JNIEXPORT jlongArray JNICALL
Java_com_dark_terminalpty_TerminalPty_nativeSpawnPty(
    JNIEnv *env, jclass cls, jstring jcwd, jobjectArray jargv, jobjectArray jenv,
    jint rows, jint cols) {
    (void)cls;
    const char *cwd = (*env)->GetStringUTFChars(env, jcwd, NULL);
    char **argv = build_vec(env, jargv);
    char **envp = build_vec(env, jenv);
    if (!argv || !argv[0]) {
        LOGE("spawnPty: empty argv");
        if (cwd) (*env)->ReleaseStringUTFChars(env, jcwd, cwd);
        free_vec(argv);
        free_vec(envp);
        return NULL;
    }

    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = (unsigned short)(rows > 0 ? rows : 30);
    ws.ws_col = (unsigned short)(cols > 0 ? cols : 100);

    int master = -1;
    pid_t pid = forkpty(&master, NULL, NULL, &ws);
    if (pid < 0) {
        LOGE("forkpty failed: %s", strerror(errno));
        if (cwd) (*env)->ReleaseStringUTFChars(env, jcwd, cwd);
        free_vec(argv);
        free_vec(envp);
        return NULL;
    }
    if (pid == 0) {
        // child: forkpty already did setsid + TIOCSCTTY + dup2
        if (cwd && cwd[0] && chdir(cwd) != 0) _exit(127);
        execve(argv[0], argv, envp);
        _exit(127);
    }
    if (cwd) (*env)->ReleaseStringUTFChars(env, jcwd, cwd);
    free_vec(argv);
    free_vec(envp);

    int fl = fcntl(master, F_GETFL, 0);
    if (fl >= 0) fcntl(master, F_SETFL, fl | O_NONBLOCK);
    return make_pair(env, (jlong)pid, (jlong)master);
}

JNIEXPORT jlongArray JNICALL
Java_com_dark_terminalpty_TerminalPty_nativeSpawnPipes(
    JNIEnv *env, jclass cls, jstring jcwd, jobjectArray jargv, jobjectArray jenv) {
    (void)cls;
    const char *cwd = (*env)->GetStringUTFChars(env, jcwd, NULL);
    char **argv = build_vec(env, jargv);
    char **envp = build_vec(env, jenv);
    if (!argv || !argv[0]) {
        LOGE("spawnPipes: empty argv");
        if (cwd) (*env)->ReleaseStringUTFChars(env, jcwd, cwd);
        free_vec(argv);
        free_vec(envp);
        return NULL;
    }

    int in_pipe[2] = { -1, -1 };
    int out_pipe[2] = { -1, -1 };
    int err_pipe[2] = { -1, -1 };
    if (pipe(in_pipe) != 0 || pipe(out_pipe) != 0 || pipe(err_pipe) != 0) {
        LOGE("pipe failed: %s", strerror(errno));
        goto fail;
    }

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork failed: %s", strerror(errno));
        goto fail;
    }
    if (pid == 0) {
        setsid();
        if (cwd && cwd[0] && chdir(cwd) != 0) _exit(127);
        dup2(in_pipe[0], 0);
        dup2(out_pipe[1], 1);
        dup2(err_pipe[1], 2);
        for (int fd = 3; fd < 1024; fd++) close(fd);
        execve(argv[0], argv, envp);
        _exit(127);
    }

    close(in_pipe[0]);
    close(out_pipe[1]);
    close(err_pipe[1]);
    if (cwd) (*env)->ReleaseStringUTFChars(env, jcwd, cwd);
    free_vec(argv);
    free_vec(envp);

    int fl = fcntl(out_pipe[0], F_GETFL, 0);
    if (fl >= 0) fcntl(out_pipe[0], F_SETFL, fl | O_NONBLOCK);
    fl = fcntl(err_pipe[0], F_GETFL, 0);
    if (fl >= 0) fcntl(err_pipe[0], F_SETFL, fl | O_NONBLOCK);

    jlongArray r = (*env)->NewLongArray(env, 4);
    if (!r) return NULL;
    jlong vals[4] = { (jlong)pid, (jlong)out_pipe[0], (jlong)err_pipe[0], (jlong)in_pipe[1] };
    (*env)->SetLongArrayRegion(env, r, 0, 4, vals);
    return r;

fail:
    if (in_pipe[0] >= 0) close(in_pipe[0]);
    if (in_pipe[1] >= 0) close(in_pipe[1]);
    if (out_pipe[0] >= 0) close(out_pipe[0]);
    if (out_pipe[1] >= 0) close(out_pipe[1]);
    if (err_pipe[0] >= 0) close(err_pipe[0]);
    if (err_pipe[1] >= 0) close(err_pipe[1]);
    if (cwd) (*env)->ReleaseStringUTFChars(env, jcwd, cwd);
    free_vec(argv);
    free_vec(envp);
    return NULL;
}

// Returns: bytes read (>0), empty array = EOF, null = timeout or error.
JNIEXPORT jbyteArray JNICALL
Java_com_dark_terminalpty_TerminalPty_nativeRead(JNIEnv *env, jclass cls, jint fd, jint timeoutMs) {
    (void)cls;
    struct pollfd p;
    p.fd = fd;
    p.events = POLLIN;
    p.revents = 0;
    int r = poll(&p, 1, timeoutMs);
    if (r < 0) {
        if (errno == EINTR) return NULL;
        return NULL;
    }
    if (r == 0) return NULL;

    unsigned char buf[8192];
    ssize_t n = read(fd, buf, sizeof(buf));
    if (n < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) return NULL;
        return NULL;
    }
    jbyteArray out = (*env)->NewByteArray(env, (jsize)n);
    if (!out) return NULL;
    if (n > 0) (*env)->SetByteArrayRegion(env, out, 0, (jsize)n, (const jbyte *)buf);
    return out;
}

JNIEXPORT jint JNICALL
Java_com_dark_terminalpty_TerminalPty_nativeWrite(JNIEnv *env, jclass cls, jint fd, jbyteArray jdata) {
    (void)cls;
    jsize len = (*env)->GetArrayLength(env, jdata);
    if (len <= 0) return 0;
    jbyte *data = (*env)->GetByteArrayElements(env, jdata, NULL);
    if (!data) return -1;
    jint total = 0;
    int spins = 0;
    while (total < len && spins < 200) {
        ssize_t n = write(fd, data + total, (size_t)(len - total));
        if (n > 0) {
            total += (jint)n;
            continue;
        }
        if (n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
            struct pollfd p;
            p.fd = fd;
            p.events = POLLOUT;
            p.revents = 0;
            if (poll(&p, 1, 500) <= 0) break;
            spins++;
            continue;
        }
        break;
    }
    (*env)->ReleaseByteArrayElements(env, jdata, data, JNI_ABORT);
    return total == len ? total : -1;
}

JNIEXPORT jboolean JNICALL
Java_com_dark_terminalpty_TerminalPty_nativeResize(JNIEnv *env, jclass cls, jint fd, jint rows, jint cols) {
    (void)env;
    (void)cls;
    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = (unsigned short)(rows > 0 ? rows : 30);
    ws.ws_col = (unsigned short)(cols > 0 ? cols : 100);
    return ioctl(fd, TIOCSWINSZ, &ws) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_dark_terminalpty_TerminalPty_nativeKillGroup(JNIEnv *env, jclass cls, jint pid, jint sig) {
    (void)env;
    (void)cls;
    if (pid <= 0) return JNI_FALSE;
    if (kill(-pid, sig) == 0) return JNI_TRUE;
    return errno == ESRCH ? JNI_TRUE : JNI_FALSE;
}

// Returns: exit code 0..255; 128+signal if signaled; -1 timeout; -2 error (not our child).
JNIEXPORT jint JNICALL
Java_com_dark_terminalpty_TerminalPty_nativeWaitExit(JNIEnv *env, jclass cls, jint pid, jint timeoutMs) {
    (void)env;
    (void)cls;
    if (pid <= 0) return -2;
    int waited = 0;
    for (;;) {
        int status = 0;
        pid_t r = waitpid((pid_t)pid, &status, WNOHANG);
        if (r == (pid_t)pid) {
            if (WIFEXITED(status)) return WEXITSTATUS(status);
            if (WIFSIGNALED(status)) return 128 + WTERMSIG(status);
            return -2;
        }
        if (r < 0) return errno == EINTR ? -1 : -2;
        if (waited >= timeoutMs) return -1;
        usleep(10 * 1000);
        waited += 10;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_dark_terminalpty_TerminalPty_nativeClose(JNIEnv *env, jclass cls, jint fd) {
    (void)env;
    (void)cls;
    if (fd < 0) return JNI_FALSE;
    return close(fd) == 0 ? JNI_TRUE : JNI_FALSE;
}
