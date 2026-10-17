package app.vaartalu

import android.app.Application
import app.vaartalu.data.ManifestRepository
import app.vaartalu.data.NewPipeDownloader
import app.vaartalu.data.YouTubeLiveResolver
import app.vaartalu.data.newHttpClient
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class AppGraph(
    val http: OkHttpClient,
    val manifest: ManifestRepository,
    val youTube: YouTubeLiveResolver,
)

class VaartaluApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        val http = newHttpClient()
        NewPipe.init(NewPipeDownloader(http), Localization("en", "IN"), ContentCountry("IN"))
        graph = AppGraph(
            http = http,
            manifest = ManifestRepository(this, http, BuildConfig.MANIFEST_URL),
            youTube = YouTubeLiveResolver(http),
        )
    }
}
