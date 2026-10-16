package com.dark.terminalpty

object TerminalPty {
    data class PtySpawn(val pid: Int, val fd: Int)

    data class PipeSpawn(val pid: Int, val stdoutFd: Int, val stderrFd: Int, val stdinFd: Int)

    fun spawnPty(cwd: String, argv: Array<String>, env: Array<String>, rows: Int, cols: Int): PtySpawn? {
        val r = nativeSpawnPty(cwd, argv, env, rows, cols) ?: return null
        if (r.size != 2) return null
        return PtySpawn(r[0].toInt(), r[1].toInt())
    }

    fun spawnPipes(cwd: String, argv: Array<String>, env: Array<String>): PipeSpawn? {
        val r = nativeSpawnPipes(cwd, argv, env) ?: return null
        if (r.size != 4) return null
        return PipeSpawn(r[0].toInt(), r[1].toInt(), r[2].toInt(), r[3].toInt())
    }

    fun read(fd: Int, timeoutMs: Int): ByteArray? = nativeRead(fd, timeoutMs)

    fun write(fd: Int, data: ByteArray): Int = nativeWrite(fd, data)

    fun resize(fd: Int, rows: Int, cols: Int): Boolean = nativeResize(fd, rows, cols)

    fun killGroup(pid: Int, signal: Int): Boolean = nativeKillGroup(pid, signal)

    fun waitExit(pid: Int, timeoutMs: Int): Int = nativeWaitExit(pid, timeoutMs)

    fun close(fd: Int): Boolean = nativeClose(fd)

    const val SIGINT = 2
    const val SIGTERM = 15
    const val SIGKILL = 9

    @JvmStatic private external fun nativeSpawnPty(cwd: String, argv: Array<String>, env: Array<String>, rows: Int, cols: Int): LongArray?
    @JvmStatic private external fun nativeSpawnPipes(cwd: String, argv: Array<String>, env: Array<String>): LongArray?
    @JvmStatic private external fun nativeRead(fd: Int, timeoutMs: Int): ByteArray?
    @JvmStatic private external fun nativeWrite(fd: Int, data: ByteArray): Int
    @JvmStatic private external fun nativeResize(fd: Int, rows: Int, cols: Int): Boolean
    @JvmStatic private external fun nativeKillGroup(pid: Int, signal: Int): Boolean
    @JvmStatic private external fun nativeWaitExit(pid: Int, timeoutMs: Int): Int
    @JvmStatic private external fun nativeClose(fd: Int): Boolean

    init {
        System.loadLibrary("terminalpty")
    }
}
