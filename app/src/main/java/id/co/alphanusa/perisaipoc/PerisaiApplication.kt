package id.co.alphanusa.perisaipoc

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Entry point aplikasi. Anotasi [HiltAndroidApp] memicu pembuatan container Hilt
 * sehingga seluruh lapisan data & domain bisa di-inject.
 */
@HiltAndroidApp
class PerisaiApplication : Application()
