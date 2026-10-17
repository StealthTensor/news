package app.vaartalu.data

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Response

const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

fun newHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(30, TimeUnit.SECONDS)
    .build()

fun Response.bodyString(): String = checkNotNull(body) { "empty response body" }.string()
