package di

import com.intellij.ide.ApplicationInitializedListener
import org.koin.core.context.GlobalContext.startKoin

class MyPluginStartupActivity : ApplicationInitializedListener {
    override fun componentsInitialized() {
        startKoin {
            modules(appModule)
        }
    }
}