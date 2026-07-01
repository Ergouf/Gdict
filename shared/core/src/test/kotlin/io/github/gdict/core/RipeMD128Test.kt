package io.github.gdict.core

import org.junit.Assert.*
import org.junit.Test

class RipeMD128Test {

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    @Test
    fun testEmptyString() {
        val expected = "cdf26213a150dc3ecb610f18f6b38b46"
        val result = RipeMD128.digest(ByteArray(0)).toHexString()
        assertEquals(expected, result)
    }

    @Test
    fun testSingleCharA() {
        val expected = "86be7afa339d0fc7cfc785e72f578d33"
        val result = RipeMD128.digest("a".toByteArray()).toHexString()
        assertEquals(expected, result)
    }

    @Test
    fun testAbc() {
        val expected = "c14a12199c66e4ba84636b0f69144c77"
        val result = RipeMD128.digest("abc".toByteArray()).toHexString()
        assertEquals(expected, result)
    }

    @Test
    fun testMessageDigest() {
        val expected = "9e327b3d6e523062afc1132d7df9d1b8"
        val result = RipeMD128.digest("message digest".toByteArray()).toHexString()
        assertEquals(expected, result)
    }

    @Test
    fun testAlphabetLowercase() {
        val expected = "fd2aa607f71dc8f510714922b371834e"
        val result = RipeMD128.digest("abcdefghijklmnopqrstuvwxyz".toByteArray()).toHexString()
        assertEquals(expected, result)
    }

    @Test
    fun testAlphabetMixedCase() {
        val expected = "601ab34c07a83be57fdc67611af179ee"
        val input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val result = RipeMD128.digest(input.toByteArray()).toHexString()
        assertEquals(expected, result)
    }

    @Test
    fun testNumericString() {
        val expected = "3f45ef194732c2dbb2c4a2c769795fa3"
        val input = "1234567890" + "1234567890" + "1234567890" + "1234567890" +
                "1234567890" + "1234567890" + "1234567890" + "1234567890"
        val result = RipeMD128.digest(input.toByteArray()).toHexString()
        assertEquals(expected, result)
    }

    @Test
    fun testOutputSizeIs16Bytes() {
        val result = RipeMD128.digest("test".toByteArray())
        assertEquals("RIPEMD-128 output should be 16 bytes", 16, result.size)
    }

    @Test
    fun testOutputSizeIs16BytesForEmptyInput() {
        val result = RipeMD128.digest(ByteArray(0))
        assertEquals("RIPEMD-128 output should be 16 bytes for empty input", 16, result.size)
    }

    @Test
    fun testDeterministic() {
        val input = "determinism test".toByteArray()
        val result1 = RipeMD128.digest(input).toHexString()
        val result2 = RipeMD128.digest(input).toHexString()
        assertEquals("Same input should produce same hash", result1, result2)
    }

    @Test
    fun testDifferentInputsProduceDifferentHashes() {
        val hash1 = RipeMD128.digest("hello".toByteArray()).toHexString()
        val hash2 = RipeMD128.digest("world".toByteArray()).toHexString()
        assertNotEquals("Different inputs should produce different hashes", hash1, hash2)
    }

    @Test
    fun testSingleByteDifference() {
        val input1 = byteArrayOf(0x00)
        val input2 = byteArrayOf(0x01)
        val hash1 = RipeMD128.digest(input1).toHexString()
        val hash2 = RipeMD128.digest(input2).toHexString()
        assertNotEquals("Single byte difference should produce different hash", hash1, hash2)
    }

    @Test
    fun testExactly64Bytes() {
        val input = ByteArray(64) { it.toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testExactly56Bytes() {
        val input = ByteArray(56) { it.toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testMultiBlockMessage() {
        val input = ByteArray(200) { (it % 256).toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testLargeInput() {
        val input = ByteArray(10000) { (it % 256).toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testAllZeroBytes() {
        val input = ByteArray(100) { 0x00 }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
        val hex = result.toHexString()
        assertFalse("All-zero input should not produce all-zero hash", hex.all { it == '0' })
    }

    @Test
    fun testAllOneBytes() {
        val input = ByteArray(100) { 0xFF.toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testUtf8String() {
        val input = "中文测试".toByteArray(Charsets.UTF_8)
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testTwoBlockBoundary() {
        val input = ByteArray(128) { it.toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testPaddingBoundary55Bytes() {
        val input = ByteArray(55) { it.toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testPaddingBoundary56Bytes() {
        val input = ByteArray(56) { it.toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testPaddingBoundary63Bytes() {
        val input = ByteArray(63) { it.toByte() }
        val result = RipeMD128.digest(input)
        assertEquals(16, result.size)
    }

    @Test
    fun testBitLengthInPadding() {
        val input7 = ByteArray(7) { 0x41 }
        val result7 = RipeMD128.digest(input7).toHexString()
        val input8 = ByteArray(8) { 0x41 }
        val result8 = RipeMD128.digest(input8).toHexString()
        assertNotEquals(
            "Different length inputs should produce different hashes even with same content",
            result7, result8
        )
    }
}
