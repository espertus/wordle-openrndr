import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.openrndr.template"
version = "0.4.0"

val applicationMainClass = "GUIKt"

/**  ## additional ORX features to be added to this project */
val orxFeatures = setOf<String>(
    "orx-gui",
    "orx-panel",
)

/** ## additional OPENRNDR features to be added to this project */
val openrndrFeatures = setOfNotNull(
    if (DefaultNativePlatform("current").architecture.name != "arm-v8") "video" else null
)

/** ## configure the type of logging this project uses */
enum class Logging { NONE, SIMPLE, FULL }

val applicationLogging = Logging.FULL

// ------------------------------------------------------------------------------------------------------------------ //

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.runtime)
    alias(libs.plugins.gitarchive.tomarkdown).apply(false)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.logging)

    when (applicationLogging) {
        Logging.NONE -> {
            runtimeOnly(libs.slf4j.nop)
        }
        Logging.SIMPLE -> {
            runtimeOnly(libs.slf4j.simple)
        }
        Logging.FULL -> {
            runtimeOnly(libs.log4j.slf4j)
            runtimeOnly(libs.jackson.databind)
            runtimeOnly(libs.jackson.json)
        }
    }
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(libs.junit)
}

// ------------------------------------------------------------------------------------------------------------------ //

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

// ------------------------------------------------------------------------------------------------------------------ //

project.setProperty("mainClassName", applicationMainClass)
tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes["Main-Class"] = applicationMainClass
            attributes["Implementation-Version"] = project.version
        }
        minimize {
            exclude(dependency("org.openrndr:openrndr-gl3:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
            exclude(dependency("org.slf4j:slf4j-simple:.*"))
            exclude(dependency("org.apache.logging.log4j:log4j-slf4j-impl:.*"))
            exclude(dependency("com.fasterxml.jackson.core:jackson-databind:.*"))
            exclude(dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:.*"))
        }
    }
    named<org.beryx.runtime.JPackageTask>("jpackage") {
        doLast {
            when (OperatingSystem.current()) {
                OperatingSystem.WINDOWS, OperatingSystem.LINUX -> {
                    copy {
                        from("data") {
                            include("**/*")
                        }
                        into("build/jpackage/openrndr-application/data")
                    }
                }
                OperatingSystem.MAC_OS -> {
                    copy {
                        from("data") {
                            include("**/*")
                        }
                        into("build/jpackage/openrndr-application.app/data")
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------------------------ //

tasks.register<Zip>("jpackageZip") {
    archiveFileName.set("openrndr-application.zip")
    from("$buildDir/jpackage") {
        include("**/*")
    }
}
tasks.findByName("jpackageZip")?.dependsOn("jpackage")

// ------------------------------------------------------------------------------------------------------------------ //

runtime {
    jpackage {
        imageName = "openrndr-application"
        skipInstaller = true
        if (OperatingSystem.current() == OperatingSystem.MAC_OS) {
            jvmArgs.add("-XstartOnFirstThread")
            jvmArgs.add("-Duser.dir=${"$"}APPDIR/../Resources")
        }
    }
    options.set(
        listOf(
            "--strip-debug",
            "--compress",
            "1",
            "--no-header-files",
            "--no-man-pages"
        )
    )
    modules.set(listOf("jdk.unsupported", "java.management"))
}

// ------------------------------------------------------------------------------------------------------------------ //

tasks.register<org.openrndr.extra.gitarchiver.GitArchiveToMarkdown>("gitArchiveToMarkDown") {
    historySize.set(20)
}

// ------------------------------------------------------------------------------------------------------------------ //

class Openrndr {
    val openrndrVersion = libs.versions.openrndr.get()
    val orxVersion = libs.versions.orx.get()
    val ormlVersion = libs.versions.orml.get()

    // choices are "orx-tensorflow-gpu", "orx-tensorflow"
    val orxTensorflowBackend = "orx-tensorflow"

    val os = if (project.hasProperty("targetPlatform")) {
        val supportedPlatforms =
            setOf("windows", "macos", "linux-x64", "linux-arm64")
        val platform: String = project.property("targetPlatform") as String
        if (platform !in supportedPlatforms) {
            throw IllegalArgumentException("target platform not supported: $platform")
        } else {
            platform
        }
    } else when (OperatingSystem.current()) {
        OperatingSystem.WINDOWS -> "windows"
        OperatingSystem.MAC_OS -> when (val h =
            DefaultNativePlatform("current").architecture.name) {
            "aarch64", "arm-v8" -> "macos-arm64"
            else -> "macos"
        }
        OperatingSystem.LINUX -> when (val h =
            DefaultNativePlatform("current").architecture.name) {
            "x86-64" -> "linux-x64"
            "aarch64" -> "linux-arm64"
            else -> throw IllegalArgumentException("architecture not supported: $h")
        }
        else -> throw IllegalArgumentException("os not supported")
    }

    fun orx(module: String) = "org.openrndr.extra:$module:$orxVersion"
    fun orml(module: String) = "org.openrndr.orml:$module:$ormlVersion"
    fun openrndr(module: String) =
        "org.openrndr:openrndr-$module:$openrndrVersion"

    fun openrndrNatives(module: String) =
        "org.openrndr:openrndr-$module-natives-$os:$openrndrVersion"

    fun orxNatives(module: String) =
        "org.openrndr.extra:$module-natives-$os:$orxVersion"

    init {
        repositories {
            if (listOf(
                    openrndrVersion,
                    orxVersion,
                    ormlVersion
                ).any { "SNAPSHOT" in it }
            ) {
                mavenLocal()
            }
            maven(url = "https://maven.openrndr.org")
        }
        dependencies {
            runtimeOnly(openrndr("gl3"))
            runtimeOnly(openrndrNatives("gl3"))
            implementation(openrndr("openal"))
            runtimeOnly(openrndrNatives("openal"))
            implementation(openrndr("application"))
            implementation(openrndr("svg"))
            implementation(openrndr("animatable"))
            implementation(openrndr("extensions"))
            implementation(openrndr("filter"))
            if ("video" in openrndrFeatures) {
                implementation(openrndr("ffmpeg"))
                runtimeOnly(openrndrNatives("ffmpeg"))
            }
            for (feature in orxFeatures) {
                implementation(orx(feature))
            }
            if ("orx-tensorflow" in orxFeatures) runtimeOnly("org.openrndr.extra:$orxTensorflowBackend-natives-$os:$orxVersion")
            if ("orx-kinect-v1" in orxFeatures) runtimeOnly(orxNatives("orx-kinect-v1"))
            if ("orx-olive" in orxFeatures) implementation(libs.kotlin.script.runtime)
        }
    }
}

val openrndr = Openrndr()
