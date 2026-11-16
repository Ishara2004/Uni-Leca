package com.example

import android.app.Application
import android.content.Context

class UniLecaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        fun container(context: Context): AppContainer =
            (context.applicationContext as UniLecaApplication).container
    }
}
