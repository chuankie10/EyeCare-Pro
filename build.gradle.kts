buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 built-in Kotlin uses the KGP on the buildscript classpath.
        // Pin a newer KGP that is documented as compatible with AGP 9.3.1.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
        // Use the published Compose plugin marker from Maven Central; this avoids
        // relying on a transient Gradle Plugin Portal marker lookup in CI.
        classpath("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
}
