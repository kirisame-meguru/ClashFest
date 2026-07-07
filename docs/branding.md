# Branding — single source of truth

All app/fork **identity** lives in one file: [`branding.json`](../branding.json) at the repo
root. The root [`build.gradle.kts`](../build.gradle.kts) parses it once (`JsonSlurper`) and feeds
every value into `applicationId`, `resValue`, `manifestPlaceholders` and each module's
`BuildConfig` (`BRAND_*`). Runtime code reads it through
[`AppBranding`](../common/src/main/java/com/github/kr328/clash/common/branding/AppBranding.kt).

**To re-skin / fork the app, edit `branding.json` and rebuild.** Do not hunt for hardcoded
strings — there shouldn't be any left.

> This is **fork / app** identity. It is separate from the *operator per-subscription* branding
> feature (`BrandHeaders`, `BrandThemeApplier`, `BrandLogoFetcher` under `*/branding/`), which
> applies a VPN operator's brand pushed at runtime over `X-Brand-*` HTTP headers.

## Fields

| Key | Example | Drives |
|---|---|---|
| `appName` | `ClashFest` | launcher label + `<application>` label (`resValue application_name` / `launch_name`, app module); `BuildConfig.BRAND_APP_NAME`; companion display-name fallback; HWID-diagnostics text |
| `applicationId` | `com.nemu.clashfest.clash` | Gradle `applicationId` (base; flavor suffixes `.alpha`/`.meta` still apply). Overridable per-build via `custom.application.id` in `local.properties` (higher precedence) |
| `packageId` | `clashfest` | `archivesBaseName` (`clashfest-v<ver>`); companion protocol `APP_ID`; update-APK / CSV-export filenames; `BuildConfig.BRAND_PACKAGE_ID` |
| `primaryScheme` | `clashfest` | deep-link `<data android:scheme>` via `${brandPrimaryScheme}` manifest placeholder; `AppBranding.deepLinkPrefix`; `BuildConfig.BRAND_PRIMARY_SCHEME` |
| `compatSchemes` | `["clash","clashmeta"]` | documentation only — these stay hardcoded in the manifest as compatibility aliases |
| `vpnSessionName` | `ClashFest` | `resValue vpn_session_name` (service module) → `VpnService.Builder.setSession(...)`; `BuildConfig.BRAND_VPN_SESSION` |
| `userAgentProduct` | `ClashFest` | HTTP User-Agent token (`<product>/<version>`) for subscription + self-update requests; `BuildConfig.BRAND_USER_AGENT_PRODUCT` |
| `logTag` | `ClashFest` | logcat tag (`Log`) and `SystemLogcat` crash-dump filter; `BuildConfig.BRAND_LOG_TAG` |
| `updateRepo` | `kirisame-meguru/ClashFest` | self-update GitHub Releases lookup (`AppBranding.updateApiEndpoint`); `BuildConfig.BRAND_UPDATE_REPO` |
| `repoUrl` | `https://github.com/kirisame-meguru/ClashFest` | About-screen repo link (`resValue clashfest_repo_url`, design module); `BuildConfig.BRAND_REPO_URL` |
| `telegramUrl` | `https://t.me/nemux_dev` | About-screen Telegram link (`resValue clashfest_telegram_url`, design module) |

## Not driven by `branding.json` (rebrand these separately)

- **Launcher icons / banner** — regenerate under `app/src/main/res/mipmap-*/ic_launcher*.webp`,
  `mipmap-anydpi-v26/ic_launcher*.xml`, `drawable/ic_launcher_{foreground,background}.xml`,
  `drawable-*/ic_launcher_monochrome.png`, and `mipmap-xhdpi/ic_banner.png`. Design source:
  `design/ClashFest.png` / `design/ClashFest_notxt.png`.
- **Source package / `namespace`** `com.github.kr328.clash` — legacy ClashForAndroid package,
  pervasive; not a rebrand target.
- **`versionName` / `versionCode`** — carried verbatim from upstream `Nemu-x/ClashFest`.
- **Core / geo-data download URLs** (`MetaCubeX/*`, jsdelivr) — the mihomo core, not fork identity.
