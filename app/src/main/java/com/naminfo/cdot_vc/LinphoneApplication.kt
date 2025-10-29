package com.naminfo.cdot_vc

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.naminfo.cdot_vc.activities.main.contact.data.CallTypeWithPhoneNumber
import com.naminfo.cdot_vc.core.*
import org.linphone.core.Factory
import org.linphone.core.LogCollectionState
import org.linphone.core.LogLevel
import org.linphone.core.tools.Log
import org.linphone.mediastream.Version


class LinphoneApplication : Application(), ImageLoaderFactory {
    companion object {

        lateinit var context: Context

        @SuppressLint("StaticFieldLeak")
        lateinit var corePreferences: CorePreferences

        @SuppressLint("StaticFieldLeak")
        lateinit var callTypeWithPhoneNumberList: ArrayList<CallTypeWithPhoneNumber>

        @SuppressLint("StaticFieldLeak")
        lateinit var coreContext: CoreContext
        val appName = "CDOT_VC"
        private fun createConfig(context: Context) {
            if (::corePreferences.isInitialized) {
                return
            }

            Factory.instance().setLogCollectionPath(context.filesDir.absolutePath)
            Factory.instance().enableLogCollection(LogCollectionState.Enabled)

            // For VFS
            Factory.instance().setCacheDir(context.cacheDir.absolutePath)

            corePreferences = CorePreferences(context)
            corePreferences.copyAssetsFromPackage()

            /*if (corePreferences.vfsEnabled) {
                CoreContext.activateVFS()
            }*/

            val config = Factory.instance().createConfigWithFactory(
                corePreferences.configPath,
                corePreferences.factoryConfigPath
            )
            corePreferences.config = config


            Factory.instance().setLoggerDomain(appName)
            Factory.instance().enableLogcatLogs(corePreferences.logcatLogsOutput)
            if (corePreferences.debugLogs) {
                Factory.instance().loggingService.setLogLevel(LogLevel.Message)
            }

            android.util.Log.i("[$appName]","[Application] Core config & preferences created")
        }

        fun ensureCoreExists(
            context: Context,
            pushReceived: Boolean = false,
            service: CoreService? = null,
            useAutoStartDescription: Boolean = false,
            skipCoreStart: Boolean = false
        ): Boolean {
            if (::coreContext.isInitialized && !coreContext.stopped) {
                Log.i("[$appName]","[Application] Skipping Core creation (push received? $pushReceived)")
                return false
            }

            Log.i("[$appName]",
                "[Application] Core context is being created ${if (pushReceived) "from push" else ""}"
            )
            coreContext = CoreContext(
                context,
                corePreferences.config,
                service,
                useAutoStartDescription
            )
            if (!skipCoreStart) {
                coreContext.start()
            }
            return true
        }

        fun contextExists(): Boolean {
            return ::coreContext.isInitialized
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        val appName = getString(R.string.app_name)
        android.util.Log.i("[$appName]", "Application is being created")
        createConfig(applicationContext)
        android.util.Log.i("[$appName]","Application Created")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
                add(SvgDecoder.Factory())
                if (Version.sdkAboveOrEqual(Version.API28_PIE_90)) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }
}
