package com.eyecare.pro

import androidx.activity.ComponentActivity

/**
 * Empty host Activity used only by androidTest.
 *
 * Keeping this Activity in the instrumentation APK lets Compose tests install their
 * own content without launching MainActivity, which initializes the production UI.
 */
class ComposeTestActivity : ComponentActivity()
