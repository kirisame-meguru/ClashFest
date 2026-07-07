@file:Suppress("UNUSED_VARIABLE")

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import groovy.json.JsonSlurper
import java.net.URL
import java.util.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        google()
        maven("https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")
    }
    dependencies {
        classpath(libs.build.android)
        classpath(libs.build.kotlin.common)
        classpath(libs.build.kotlin.serialization)
        classpath(libs.build.ksp)
        classpath(libs.build.golang)
    }
}

// Branding single source of truth. Every app-identity value (names, schemes, repo,
// user-agent, VPN session) is read from this one file and fed to applicationId,
// resValue, manifestPlaceholders and BuildConfig below. Forking / re-skinning = edit
// branding.json (see docs/branding.md). This is fork/app identity — distinct from the
// operator per-subscription "Brand*" feature under */branding/.
val brandingFile = rootProject.file("branding.json")
require(brandingFile.exists()) { "branding.json missing at repo root — see docs/branding.md" }

@Suppress("UNCHECKED_CAST")
val branding = JsonSlurper().parse(brandingFile) as Map<String, Any?>
fun brandStr(key: String): String =
    (branding[key] as? String) ?: error("branding.json missing string key: $key")

subprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")
    }

    val isApp = name == "app"

    apply(plugin = if (isApp) "com.android.application" else "com.android.library")

    fun queryConfigProperty(key: String): Any? {
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        } else {
            return null
        }
        return localProperties.getProperty(key)
    }

    extensions.configure<BaseExtension> {
        buildFeatures.buildConfig = true
        defaultConfig {
            if (isApp) {
                val customApplicationId = queryConfigProperty("custom.application.id") as? String?
                applicationId = customApplicationId.takeIf { it?.isNotBlank() == true } ?: brandStr("applicationId")
            }

            project.name.let { name ->
                namespace = if (name == "app") "com.github.kr328.clash"
                else "com.github.kr328.clash.$name"
            }

            minSdk = 21
            targetSdk = 35

            versionName = "0.9.0"
            versionCode = 900000

            resValue("string", "release_name", "v$versionName")
            resValue("integer", "release_code", "$versionCode")

            // Branding fields, injected into every module's BuildConfig from branding.json.
            buildConfigField("String", "BRAND_APP_NAME", "\"${brandStr("appName")}\"")
            buildConfigField("String", "BRAND_USER_AGENT_PRODUCT", "\"${brandStr("userAgentProduct")}\"")
            buildConfigField("String", "BRAND_LOG_TAG", "\"${brandStr("logTag")}\"")
            buildConfigField("String", "BRAND_PACKAGE_ID", "\"${brandStr("packageId")}\"")
            buildConfigField("String", "BRAND_PRIMARY_SCHEME", "\"${brandStr("primaryScheme")}\"")
            buildConfigField("String", "BRAND_UPDATE_REPO", "\"${brandStr("updateRepo")}\"")
            buildConfigField("String", "BRAND_REPO_URL", "\"${brandStr("repoUrl")}\"")
            buildConfigField("String", "BRAND_TELEGRAM_URL", "\"${brandStr("telegramUrl")}\"")
            buildConfigField("String", "BRAND_VPN_SESSION", "\"${brandStr("vpnSessionName")}\"")

            // Manifest deep-link scheme (app manifest resolves ${brandPrimaryScheme}).
            manifestPlaceholders["brandPrimaryScheme"] = brandStr("primaryScheme")

            // Brand strings that were static in strings.xml, now sourced from branding.json.
            // Each is emitted in exactly the module that owns/consumes it (no cross-module
            // resource-merge collision).
            when (project.name) {
                // design owns the user-facing name strings; the app manifest and design
                // layouts both resolve them transitively (app depends on design), so a
                // single definition here avoids a cross-module resource-merge collision.
                "design" -> {
                    resValue("string", "application_name", brandStr("appName"))
                    resValue("string", "launch_name", brandStr("appName"))
                    resValue("string", "clashfest_repo_url", brandStr("repoUrl"))
                    resValue("string", "clashfest_telegram_url", brandStr("telegramUrl"))
                }
                "service" -> {
                    resValue("string", "vpn_session_name", brandStr("vpnSessionName"))
                }
            }

            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            }

            externalNativeBuild {
                cmake {
                    abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                }
            }

            if (!isApp) {
                consumerProguardFiles("consumer-rules.pro")
            } else {
                setProperty("archivesBaseName", "${brandStr("packageId")}-v$versionName")
            }
        }

        ndkVersion = "29.0.14206865"

        compileSdkVersion(defaultConfig.targetSdk!!)

        if (isApp) {
            packagingOptions {
                resources {
                    excludes.add("DebugProbesKt.bin")
                }
            }
        }

        productFlavors {
            flavorDimensions("feature")

            val removeSuffix = (queryConfigProperty("remove.suffix") as? String)?.toBoolean() == true

            create("alpha") {
                isDefault = true
                dimension = flavorDimensionList[0]
                if (!removeSuffix) {
                    versionNameSuffix = ".Alpha"
                }

                if (isApp && !removeSuffix) {
                    applicationIdSuffix = ".alpha"
                }
            }

            create("meta") {

                dimension = flavorDimensionList[0]
                if (!removeSuffix) {
                    versionNameSuffix = ".Meta"
                }

                if (isApp && !removeSuffix) {
                    applicationIdSuffix = ".meta"
                }
            }
        }

        sourceSets {
            getByName("meta") {
                java.srcDirs("src/foss/java")
            }
            getByName("alpha") {
                java.srcDirs("src/foss/java")
            }
        }

        signingConfigs {
            val keystore = rootProject.file("signing.properties")
            if (keystore.exists()) {
                create("release") {
                    val prop = Properties().apply {
                        keystore.inputStream().use(this::load)
                    }

                    storeFile = rootProject.file("release.keystore")
                    storePassword = prop.getProperty("keystore.password")!!
                    keyAlias = prop.getProperty("key.alias")!!
                    keyPassword = prop.getProperty("key.password")!!
                }
            }
        }

        buildTypes {
            named("release") {
                isMinifyEnabled = isApp
                isShrinkResources = isApp
                signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
            named("debug") {
                versionNameSuffix = ".debug"
            }
        }

        buildFeatures.apply {
            dataBinding {
                isEnabled = name != "hideapi"
            }
        }

        if (isApp) {
            this as AppExtension

            splits {
                abi {
                    isEnable = true
                    isUniversalApk = true
                    reset()
                    include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                }
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    // Align Kotlin bytecode with Java (fixes: javac 21 vs Kotlin 21 mismatch when defaults differ).
    afterEvaluate {
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL

    doLast {
        val sha256 = URL("$distributionUrl.sha256").openStream()
            .use { it.reader().readText().trim() }

        file("gradle/wrapper/gradle-wrapper.properties")
            .appendText("distributionSha256Sum=$sha256")
    }
}