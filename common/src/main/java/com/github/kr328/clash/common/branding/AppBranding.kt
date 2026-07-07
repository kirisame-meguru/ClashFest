package com.github.kr328.clash.common.branding

import com.github.kr328.clash.common.BuildConfig

/**
 * Single source of truth for this fork's app identity, at runtime. Every field is fed
 * from the repo-root `branding.json` by the root `build.gradle.kts` (as `BRAND_*`
 * BuildConfig fields), so re-skinning the app = editing that one file. See
 * `docs/branding.md`.
 *
 * NOTE: this is *fork / app* identity. It is deliberately distinct from the operator
 * per-subscription branding feature (`BrandHeaders`, `BrandThemeApplier`, …) in the
 * neighbouring `*/branding/` packages, which applies a VPN operator's brand pushed over
 * `X-Brand-*` HTTP headers.
 */
object AppBranding {
    /** Display / product name, e.g. "ClashFest". */
    val appName: String = BuildConfig.BRAND_APP_NAME

    /** HTTP User-Agent product token (before the `/version`). */
    val userAgentProduct: String = BuildConfig.BRAND_USER_AGENT_PRODUCT

    /** Logcat tag. */
    val logTag: String = BuildConfig.BRAND_LOG_TAG

    /** Lowercase package/slug used in filenames, companion protocol id, archive base. */
    val packageId: String = BuildConfig.BRAND_PACKAGE_ID

    /** Primary deep-link scheme, e.g. "clashfest". */
    val primaryScheme: String = BuildConfig.BRAND_PRIMARY_SCHEME

    /** GitHub `owner/repo` used for self-update release lookups. */
    val updateRepo: String = BuildConfig.BRAND_UPDATE_REPO

    /** Full repository URL shown in the About screen. */
    val repoUrl: String = BuildConfig.BRAND_REPO_URL

    /** Deep-link prefix, e.g. "clashfest://". */
    val deepLinkPrefix: String
        get() = "$primaryScheme://"

    /** GitHub Releases API endpoint for the latest fork release. */
    val updateApiEndpoint: String
        get() = "https://api.github.com/repos/$updateRepo/releases/latest"

    /** HTTP User-Agent for the given app version, e.g. "ClashFest/0.9.0". */
    fun userAgent(version: String): String = "$userAgentProduct/$version"
}
