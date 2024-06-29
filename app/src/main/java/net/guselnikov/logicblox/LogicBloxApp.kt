package net.guselnikov.logicblox

import android.app.Application
import net.guselnikov.logicblox.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class LogicBloxApp: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidLogger()
            androidContext(this@LogicBloxApp)
            modules(appModule)
        }
    }
}