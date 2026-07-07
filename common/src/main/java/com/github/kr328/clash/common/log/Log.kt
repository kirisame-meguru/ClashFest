package com.github.kr328.clash.common.log

object Log {
    // Read from branding.json via BuildConfig.BRAND_LOG_TAG, using the same defensive
    // reflection as debugLoggingEnabled below; falls back to the default brand name.
    private val TAG: String by lazy {
        runCatching {
            val clazz = Class.forName("com.github.kr328.clash.common.BuildConfig")
            clazz.getField("BRAND_LOG_TAG").get(null) as String
        }.getOrDefault("ClashFest")
    }
    private val debugLoggingEnabled: Boolean by lazy {
        runCatching {
            val clazz = Class.forName("com.github.kr328.clash.common.BuildConfig")
            clazz.getField("DEBUG").getBoolean(null)
        }.getOrDefault(false)
    }

    fun i(message: String, throwable: Throwable? = null) =
        if (debugLoggingEnabled) android.util.Log.i(TAG, message, throwable) else Unit

    fun w(message: String, throwable: Throwable? = null) =
        android.util.Log.w(TAG, message, throwable)

    fun e(message: String, throwable: Throwable? = null) =
        android.util.Log.e(TAG, message, throwable)

    fun d(message: String, throwable: Throwable? = null) =
        if (debugLoggingEnabled) android.util.Log.d(TAG, message, throwable) else Unit

    fun v(message: String, throwable: Throwable? = null) =
        if (debugLoggingEnabled) android.util.Log.v(TAG, message, throwable) else Unit

    fun f(message: String, throwable: Throwable) =
        android.util.Log.wtf(message, throwable)
}
