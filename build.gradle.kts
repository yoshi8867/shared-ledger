// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}

// Fix: AGP brings javapoet 1.10 which lacks ClassName.canonicalName().
// Hilt requires javapoet 1.13.0+. Force the correct version on the buildscript classpath.
buildscript {
    dependencies {
        classpath("com.squareup:javapoet:1.13.0")
    }
}