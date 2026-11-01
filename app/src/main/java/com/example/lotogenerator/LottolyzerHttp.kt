package com.example.lotogenerator

import org.jsoup.Jsoup
import java.io.IOException

/** Browser-like downloader for Lottolyzer. Some Lottolyzer pages return a
 * reduced/empty response to generic Android/Java user agents, so all lottery
 * clients use the same desktop-browser headers and retry policy. */
object LottolyzerHttp {
    private const val TIMEOUT = 10_000
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36"

    fun get(url: String): String {
        val response = Jsoup.connect(url)
            .userAgent(UA)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .header("Upgrade-Insecure-Requests", "1")
            .referrer("https://en.lottolyzer.com/")
            .timeout(TIMEOUT)
            .maxBodySize(0)
            .followRedirects(true)
            .ignoreHttpErrors(true)
            .execute()
        if (response.statusCode() !in 200..299) {
            throw IOException("Server HTTP ${response.statusCode()}")
        }
        val html = response.body()
        if (html.length < 500) throw IOException("Server returned an empty page")
        return html
    }
}
