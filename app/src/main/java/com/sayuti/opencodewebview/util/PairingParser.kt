package com.sayuti.opencodewebview.util

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

sealed interface PairingParseResult {
    data class Success(val pairingInfo: PairingInfo) : PairingParseResult
    data class Error(val message: String) : PairingParseResult
}

data class PairingInfo(
    val baseUrl: String,
    val pairUrl: String,
    val candidateBaseUrls: List<String> = listOf(baseUrl),
    val candidatePairUrls: List<String> = listOf(pairUrl),
    val code: String? = null
)

object PairingParser {

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun parse(rawInput: String?): PairingParseResult {
        if (rawInput.isNullOrBlank()) {
            return PairingParseResult.Error("Input cannot be empty")
        }

        val trimmed = rawInput.trim()

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return parseJsonPayload(trimmed)
        }

        return parseUrlInput(trimmed)
    }

    private fun parseJsonPayload(jsonString: String): PairingParseResult {
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            val map = mapAdapter.fromJson(jsonString)
                ?: return PairingParseResult.Error("Invalid JSON structure")

            val code = map["code"]?.toString()?.takeIf { it.isNotBlank() }

            val rawUrlsList = mutableListOf<String>()
            val urlsValue = map["urls"]
            if (urlsValue is List<*>) {
                urlsValue.mapNotNull { it?.toString()?.takeIf { url -> url.isNotBlank() } }
                    .forEach { rawUrlsList.add(it) }
            }

            val singleUrl = map["url"]?.toString()?.takeIf { it.isNotBlank() }
            if (singleUrl != null && !rawUrlsList.contains(singleUrl)) {
                rawUrlsList.add(singleUrl)
            }

            if (rawUrlsList.isEmpty() && code == null) {
                return PairingParseResult.Error("JSON payload must contain 'urls' or 'code'")
            }

            if (rawUrlsList.isEmpty()) {
                return PairingParseResult.Error("JSON payload contains no server URLs")
            }

            val extractedCandidateBaseUrls = mutableListOf<String>()
            var extractedCodeFromUrl: String? = null

            for (rawUrlStr in rawUrlsList) {
                val httpUrl = parseHttpUrl(rawUrlStr) ?: continue
                val originBase = extractOriginBaseUrl(httpUrl)
                if (!extractedCandidateBaseUrls.contains(originBase)) {
                    extractedCandidateBaseUrls.add(originBase)
                }
                if (extractedCodeFromUrl == null) {
                    extractedCodeFromUrl = extractCodeFromPath(httpUrl.pathSegments)
                }
            }

            if (extractedCandidateBaseUrls.isEmpty()) {
                return PairingParseResult.Error("No valid HTTP or HTTPS server URL found in JSON payload")
            }

            val finalCode = code ?: extractedCodeFromUrl
            val candidatePairUrls = if (finalCode != null) {
                extractedCandidateBaseUrls.map { OpenCodeUtils.buildPairUrl(it, finalCode) }
            } else {
                extractedCandidateBaseUrls
            }

            PairingParseResult.Success(
                PairingInfo(
                    baseUrl = extractedCandidateBaseUrls.first(),
                    pairUrl = candidatePairUrls.first(),
                    candidateBaseUrls = extractedCandidateBaseUrls,
                    candidatePairUrls = candidatePairUrls,
                    code = finalCode
                )
            )
        } catch (e: Exception) {
            PairingParseResult.Error("Failed to parse JSON payload: ${e.message ?: "Invalid JSON"}")
        }
    }

    private fun parseUrlInput(urlInput: String): PairingParseResult {
        var targetUrl = urlInput.trim()

        if (targetUrl.contains("://")) {
            val scheme = targetUrl.substringBefore("://").lowercase()
            if (scheme != "http" && scheme != "https") {
                return PairingParseResult.Error("Invalid URL scheme. Only HTTP and HTTPS are supported.")
            }
        } else {
            targetUrl = "https://$targetUrl"
        }

        val httpUrl = parseHttpUrl(targetUrl)
            ?: return PairingParseResult.Error("Invalid link or QR code format")

        val baseUrl = extractOriginBaseUrl(httpUrl)
        val codeFromPath = extractCodeFromPath(httpUrl.pathSegments)

        val pairUrl = if (codeFromPath != null) {
            OpenCodeUtils.buildPairUrl(baseUrl, codeFromPath)
        } else if (httpUrl.encodedPath != "/" && httpUrl.encodedPath.isNotBlank()) {
            httpUrl.toString().trimEnd('/')
        } else {
            baseUrl
        }

        return PairingParseResult.Success(
            PairingInfo(
                baseUrl = baseUrl,
                pairUrl = pairUrl,
                candidateBaseUrls = listOf(baseUrl),
                candidatePairUrls = listOf(pairUrl),
                code = codeFromPath
            )
        )
    }

    private fun parseHttpUrl(rawUrl: String): HttpUrl? {
        val trimmed = rawUrl.trim()
        val urlToTry = if (!trimmed.contains("://")) "https://$trimmed" else trimmed
        val parsed = urlToTry.toHttpUrlOrNull() ?: return null
        if (parsed.scheme != "http" && parsed.scheme != "https") {
            return null
        }
        return parsed
    }

    private fun extractOriginBaseUrl(httpUrl: HttpUrl): String {
        val scheme = httpUrl.scheme
        val host = httpUrl.host
        val port = httpUrl.port
        val defaultPort = if (scheme == "https") 443 else 80

        return if (port == defaultPort) {
            "$scheme://$host"
        } else {
            "$scheme://$host:$port"
        }
    }

    private fun extractCodeFromPath(pathSegments: List<String>): String? {
        for (i in 0 until pathSegments.size - 2) {
            if (pathSegments[i] == "auth" && pathSegments[i + 1] == "connect") {
                val candidate = pathSegments[i + 2]
                if (candidate.isNotBlank()) {
                    return candidate
                }
            }
        }
        return null
    }
}
