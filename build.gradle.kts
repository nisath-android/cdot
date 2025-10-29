// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
}


gradle.projectsEvaluated {
            val osName = System.getProperty("os.name").lowercase()

            // Locate base config directory
            val baseDir = when {
                osName.contains("win") -> File(System.getenv("APPDATA"), "Google")
                osName.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/Google")
                else -> File(System.getProperty("user.home"), ".config/Google")
            }

            // Find Android Studio installation directory
            val studioDir = baseDir.listFiles()
                ?.firstOrNull { it.name.startsWith("AndroidStudio") && it.isDirectory }

            val studioName = studioDir?.name?.replace("AndroidStudio", "Android Studio ") ?: "Unknown"

            // New method: read version from product-info.json (JetBrains format)
            val productInfoJson = studioDir?.resolve("product-info.json")
            val studioVersion = when {
                productInfoJson?.exists() == true -> {
                    val text = productInfoJson.readText()
                    "\"version\":\\s*\"([^\"]+)\"".toRegex().find(text)?.groupValues?.get(1) ?: "Unknown"
                }
                else -> "Unknown"
            }

            // Detect Linphone SDK version
            var linphoneVersion: String? = null
            rootProject.allprojects {
                configurations.matching { it.name == "implementation" || it.name == "api" }.all {
                    dependencies.forEach { dep ->
                        if (dep.group?.contains("linphone", ignoreCase = true) == true ||
                            dep.name.contains("linphone", ignoreCase = true)
                        ) {
                            linphoneVersion = dep.version ?: "Local AAR / Custom Build"
                        }
                    }
                }
            }
            if (linphoneVersion == null) linphoneVersion = "Not Found"

            println(
                """
        ─────────────── ENVIRONMENT CHECK ───────────────
        🧰 Android Studio : $studioName ($studioVersion)
        ⚙️ Gradle          : ${gradle.gradleVersion}
        🧩 AGP             : ${com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION}
        🧠 Kotlin          : ${org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION}
        ☕ Java            : ${System.getProperty("java.version")}
        📞 Linphone SDK    : $linphoneVersion
        ─────────────────────────────────────────────────
        """.trimIndent()
            )
        }



