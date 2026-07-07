package com.github.kr328.clash.log

import com.github.kr328.clash.BuildConfig

object SystemLogcat {
    private val command = arrayOf(
        "logcat",
        "-d",
        "-s",
        "Go",
        "DEBUG",
        "AndroidRuntime",
        BuildConfig.BRAND_LOG_TAG,
        "LwIP",
    )

    fun dumpCrash(): String {
        return try {
            val process = Runtime.getRuntime().exec(command)

            val result = process.inputStream.use { stream ->
                stream.reader().readLines()
                    .filterNot { it.startsWith("------") }
                    .joinToString("\n")
            }

            process.waitFor()

            result.trim()
        } catch (e: Exception) {
            ""
        }
    }
}
