package com.sayuti.opencodewebview.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingParserTest {

    @Test
    fun `parse empty or blank input returns error`() {
        val resultNull = PairingParser.parse(null)
        assertTrue(resultNull is PairingParseResult.Error)
        assertEquals("Input cannot be empty", (resultNull as PairingParseResult.Error).message)

        val resultBlank = PairingParser.parse("   ")
        assertTrue(resultBlank is PairingParseResult.Error)
        assertEquals("Input cannot be empty", (resultBlank as PairingParseResult.Error).message)
    }

    @Test
    fun `parse valid JSON QR payload with single url and code`() {
        val json = """{"code":"testCode123","urls":["https://server.opentunnel.xyz"]}"""
        val result = PairingParser.parse(json)

        assertTrue(result is PairingParseResult.Success)
        val success = result as PairingParseResult.Success
        assertEquals("https://server.opentunnel.xyz", success.pairingInfo.baseUrl)
        assertEquals("https://server.opentunnel.xyz/auth/connect/testCode123", success.pairingInfo.pairUrl)
        assertEquals("testCode123", success.pairingInfo.code)
    }

    @Test
    fun `parse valid JSON QR payload with multiple urls preserves order`() {
        val json = """
            {
                "code": "myCode456",
                "urls": [
                    "https://first.opentunnel.xyz:8443",
                    "https://second.opentunnel.xyz"
                ]
            }
        """.trimIndent()

        val result = PairingParser.parse(json)

        assertTrue(result is PairingParseResult.Success)
        val success = result as PairingParseResult.Success
        assertEquals("https://first.opentunnel.xyz:8443", success.pairingInfo.baseUrl)
        assertEquals("https://first.opentunnel.xyz:8443/auth/connect/myCode456", success.pairingInfo.pairUrl)
        assertEquals(2, success.pairingInfo.candidateBaseUrls.size)
        assertEquals("https://first.opentunnel.xyz:8443", success.pairingInfo.candidateBaseUrls[0])
        assertEquals("https://second.opentunnel.xyz", success.pairingInfo.candidateBaseUrls[1])
        assertEquals("https://first.opentunnel.xyz:8443/auth/connect/myCode456", success.pairingInfo.candidatePairUrls[0])
        assertEquals("https://second.opentunnel.xyz/auth/connect/myCode456", success.pairingInfo.candidatePairUrls[1])
    }

    @Test
    fun `parse JSON payload with singular url field`() {
        val json = """{"code":"code789","url":"https://single.opentunnel.xyz"}"""
        val result = PairingParser.parse(json)

        assertTrue(result is PairingParseResult.Success)
        val success = result as PairingParseResult.Success
        assertEquals("https://single.opentunnel.xyz", success.pairingInfo.baseUrl)
        assertEquals("https://single.opentunnel.xyz/auth/connect/code789", success.pairingInfo.pairUrl)
    }

    @Test
    fun `parse JSON payload without code but with connect path in url`() {
        val json = """{"urls":["https://server.com/auth/connect/pathCode"]}"""
        val result = PairingParser.parse(json)

        assertTrue(result is PairingParseResult.Success)
        val success = result as PairingParseResult.Success
        assertEquals("https://server.com", success.pairingInfo.baseUrl)
        assertEquals("https://server.com/auth/connect/pathCode", success.pairingInfo.pairUrl)
        assertEquals("pathCode", success.pairingInfo.code)
    }

    @Test
    fun `parse JSON payload with invalid content returns error`() {
        val invalidJson = """{"foo":"bar"}"""
        val result = PairingParser.parse(invalidJson)

        assertTrue(result is PairingParseResult.Error)
        assertEquals("JSON payload must contain 'urls' or 'code'", (result as PairingParseResult.Error).message)
    }

    @Test
    fun `parse full pair link URL`() {
        val input = "https://myserver.opentunnel.xyz/auth/connect/connectCode99"
        val result = PairingParser.parse(input)

        assertTrue(result is PairingParseResult.Success)
        val success = result as PairingParseResult.Success
        assertEquals("https://myserver.opentunnel.xyz", success.pairingInfo.baseUrl)
        assertEquals("https://myserver.opentunnel.xyz/auth/connect/connectCode99", success.pairingInfo.pairUrl)
        assertEquals("connectCode99", success.pairingInfo.code)
    }

    @Test
    fun `parse base URL with custom port and no code`() {
        val input = "http://192.168.1.50:3000"
        val result = PairingParser.parse(input)

        assertTrue(result is PairingParseResult.Success)
        val success = result as PairingParseResult.Success
        assertEquals("http://192.168.1.50:3000", success.pairingInfo.baseUrl)
        assertEquals("http://192.168.1.50:3000", success.pairingInfo.pairUrl)
        assertNull(success.pairingInfo.code)
    }

    @Test
    fun `parse URL with invalid scheme returns error`() {
        val input = "ftp://fileserver.com"
        val result = PairingParser.parse(input)

        assertTrue(result is PairingParseResult.Error)
        assertEquals("Invalid URL scheme. Only HTTP and HTTPS are supported.", (result as PairingParseResult.Error).message)
    }

    @Test
    fun `buildPairUrl helper produces expected auth connect URL`() {
        val pairUrl = OpenCodeUtils.buildPairUrl("https://server.com/", "abcCode")
        assertEquals("https://server.com/auth/connect/abcCode", pairUrl)
    }
}
