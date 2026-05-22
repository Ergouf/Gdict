package io.github.gdict.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

object RipeMD128 {
    private const val BLOCK_SIZE = 64

    fun digest(data: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476

        val padded = padMessage(data)

        for (offset in padded.indices step BLOCK_SIZE) {
            val block = padded.copyOfRange(offset, offset + BLOCK_SIZE)
            val x = ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN)

            var a = h0; var b = h1; var c = h2; var d = h3
            var ap = h0; var bp = h1; var cp = h2; var dp = h3

            for (j in 0 until 64) {
                var t = add(a, f(j, b, c, d), x.getInt(RL[j] * 4), K(j))
                t = rol(SL[j], t)
                a = d; d = c; c = b; b = t

                t = add(ap, f(63 - j, bp, cp, dp), x.getInt(RR[j] * 4), Kp(j))
                t = rol(SR[j], t)
                ap = dp; dp = cp; cp = bp; bp = t
            }

            val t = add(h1, c, dp)
            h1 = add(h2, d, ap)
            h2 = add(h3, a, bp)
            h3 = add(h0, b, cp)
            h0 = t
        }

        return ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(h0).putInt(h1).putInt(h2).putInt(h3).array()
    }

    private fun f(j: Int, x: Int, y: Int, z: Int): Int = when {
        j < 16 -> x xor y xor z
        j < 32 -> (x and y) or (z and x.inv())
        j < 48 -> (x or y.inv()) xor z
        else -> (x and z) or (y and z.inv())
    }

    private fun K(j: Int): Int = when {
        j < 16 -> 0x00000000
        j < 32 -> 0x5A827999
        j < 48 -> 0x6ED9EBA1.toInt()
        else -> 0x8F1BBCDC.toInt()
    }

    private fun Kp(j: Int): Int = when {
        j < 16 -> 0x50A28BE6.toInt()
        j < 32 -> 0x5C4DD124.toInt()
        j < 48 -> 0x6D703EF3.toInt()
        else -> 0x00000000
    }

    private fun rol(s: Int, x: Int): Int = (x shl s) or (x ushr (32 - s))

    private fun add(vararg args: Int): Int {
        var sum = 0
        for (arg in args) sum += arg
        return sum and 0xFFFFFFFF.toInt()
    }

    private fun padMessage(data: ByteArray): ByteArray {
        val bitLen = data.size * 8L
        val padLen = (BLOCK_SIZE - ((data.size + 9) % BLOCK_SIZE)) % BLOCK_SIZE + 9
        val result = ByteArray(data.size + padLen)
        System.arraycopy(data, 0, result, 0, data.size)
        result[data.size] = 0x80.toByte()
        val bb = ByteBuffer.wrap(result, result.size - 8, 8).order(ByteOrder.LITTLE_ENDIAN)
        bb.putLong(bitLen)
        return result
    }

    private val RL = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
        3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
        1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2
    )
    private val RR = intArrayOf(
        5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
        6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
        15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
        8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14
    )
    private val SL = intArrayOf(
        11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
        7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
        11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
        11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12
    )
    private val SR = intArrayOf(
        8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
        9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
        9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
        15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8
    )
}
