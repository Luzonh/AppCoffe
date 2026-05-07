// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        // Check that you have the following line (if not, add it):
        google()  // Google's Maven repository
    }

    dependencies {
        classpath ("com.google.gms:google-services:4.4.2")
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id ("com.android.library") version "7.1.3" apply false
}