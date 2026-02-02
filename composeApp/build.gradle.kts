import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

// 从环境变量或默认值获取版本号
val appVersion = System.getenv("APP_VERSION") ?: "1.0.0"

// 创建生成 BuildConfig 的任务
val generateBuildConfig = tasks.register("generateBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig/jvm/main")
    outputs.dir(outputDir)

    doLast {
        val buildConfigDir = outputDir.get().asFile
        buildConfigDir.mkdirs()
        val buildConfigFile = File(buildConfigDir, "BuildConfig.kt")
        buildConfigFile.writeText("""
            package domain

            object BuildConfig {
                const val VERSION = "$appVersion"
            }
        """.trimIndent())
    }
}

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.coroutinesCore)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.ktor.client.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.pdfbox)
                implementation(libs.slf4j.simple)
                implementation(libs.ktor.client.cio)
            }
        }
    }
}

kotlin.sourceSets.named("jvmMain") {
    kotlin.srcDir("build/generated/buildconfig/jvm/main")
}

// 让编译任务依赖 BuildConfig 生成任务
tasks.named("compileKotlinJvm") {
    dependsOn(generateBuildConfig)
}


compose.desktop {
    application {
        mainClass = "MainKt"

        val appVersion = System.getenv("APP_VERSION") ?: "1.0.0"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "PdfImageAdjuster"
            packageVersion = appVersion

            windows {
                // Windows 特定配置
                menuGroup = "PDF Tools"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                iconFile.set(project.file("src/jvmMain/resources/icons/windows/app.ico"))
            }

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icons/macos/app.icns"))
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icons/linux/app.png"))
            }
        }
    }
}
