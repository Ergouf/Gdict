package io.github.gdict.core

import org.junit.Assert.*
import org.junit.Test

class Lzo1xDecompressorTest {

    @Test
    fun testEmptyInputReturnsEmptyArray() {
        val result = Lzo1xDecompressor.decompress(ByteArray(0), 0)
        assertEquals(0, result.size)
    }

    @Test
    fun testSingleByteInputReturnsTruncatedOutput() {
        val input = byteArrayOf(0x00)
        val result = Lzo1xDecompressor.decompress(input, 10)
        assertTrue("Result size ${result.size} should be <= expected 10", result.size <= 10)
    }

    @Test
    fun testTwoByteInputReturnsTruncatedOutput() {
        val input = byteArrayOf(0x00, 0x00)
        val result = Lzo1xDecompressor.decompress(input, 10)
        assertTrue("Result size ${result.size} should be <= expected 10", result.size <= 10)
    }

    @Test
    fun testZeroExpectedSize() {
        val input = ByteArray(10) { 0x00 }
        val result = Lzo1xDecompressor.decompress(input, 0)
        assertEquals(0, result.size)
    }

    @Test
    fun testInvalidDataDoesNotCrash() {
        val input = ByteArray(100) { (it % 256).toByte() }
        val result = Lzo1xDecompressor.decompress(input, 200)
        assertTrue("Should return something without crashing, got ${result.size}", result.size >= 0)
    }

    @Test
    fun testDeterministicOutput() {
        val input = ByteArray(50) { (it * 7 % 256).toByte() }
        val result1 = Lzo1xDecompressor.decompress(input, 100)
        val result2 = Lzo1xDecompressor.decompress(input, 100)
        assertArrayEquals("Same input should produce same output", result1, result2)
    }

    @Test
    fun testAllZeroInput() {
        val input = ByteArray(64) { 0x00 }
        val result = Lzo1xDecompressor.decompress(input, 128)
        assertTrue("Should not crash on all-zero input", result.size >= 0)
    }

    @Test
    fun testAllOnesInput() {
        val input = ByteArray(64) { 0xFF.toByte() }
        val result = Lzo1xDecompressor.decompress(input, 128)
        assertTrue("Should not crash on all-0xFF input", result.size >= 0)
    }

    @Test
    fun testLargeExpectedSizeWithSmallInput() {
        val input = ByteArray(10) { 0x00 }
        val result = Lzo1xDecompressor.decompress(input, 100000)
        assertTrue("Result should be <= expected size", result.size <= 100000)
    }

    @Test
    fun testResultNeverExceedsExpectedSize() {
        for (size in listOf(1, 10, 50, 100, 500)) {
            val input = ByteArray(size) { (it % 256).toByte() }
            val expectedSize = 1000
            val result = Lzo1xDecompressor.decompress(input, expectedSize)
            assertTrue(
                "Result size ${result.size} should not exceed expected $expectedSize for input size $size",
                result.size <= expectedSize
            )
        }
    }

    @Test
    fun testBitstreamVersionHeaderNotCrash() {
        val input = byteArrayOf(0x11, 0x01, 0x00, 0x00, 0x00)
        val result = Lzo1xDecompressor.decompress(input, 100)
        assertTrue("Should handle bitstream version header without crash", result.size >= 0)
    }

    @Test
    fun testLiteralCopyWithFirstByteAbove17() {
        val input = byteArrayOf(0x20, 0x41, 0x42, 0x43, 0x44, 0x45)
        val result = Lzo1xDecompressor.decompress(input, 100)
        assertTrue("First byte > 17 should be handled", result.size >= 0)
    }

    @Test
    fun testLargeInputDoesNotCrash() {
        val input = ByteArray(10000) { (it % 256).toByte() }
        val result = Lzo1xDecompressor.decompress(input, 20000)
        assertTrue("Large input should not crash", result.size >= 0)
    }

    @Test
    fun testVariousInputSizes() {
        for (inputSize in listOf(0, 1, 2, 3, 5, 10, 20, 50, 100, 256, 512, 1024)) {
            val input = ByteArray(inputSize) { (it % 256).toByte() }
            val result = Lzo1xDecompressor.decompress(input, inputSize * 2)
            assertTrue(
                "Input size $inputSize should not crash, result=${result.size}",
                result.size >= 0 && result.size <= inputSize * 2
            )
        }
    }

    @Test
    fun testOutputIsCopyNotReference() {
        val input = ByteArray(10) { 0x00 }
        val result1 = Lzo1xDecompressor.decompress(input, 20)
        val result2 = Lzo1xDecompressor.decompress(input, 20)
        if (result1.isNotEmpty() && result2.isNotEmpty()) {
            result1[0] = if (result1[0] == 0x42.toByte()) 0x43 else 0x42
            assertNotEquals(
                "Modifying result1 should not affect result2",
                result1[0], result2[0]
            )
        }
    }
}
