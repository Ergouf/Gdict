package io.github.gdict.core

import java.util.Arrays

object Lzo1xDecompressor {

    private const val M2_MAX_OFFSET = 0x0800
    private const val MAX_255_COUNT = (Int.MAX_VALUE / 255) - 2
    private const val MIN_ZERO_RUN_LENGTH = 4

    fun decompress(input: ByteArray, expectedSize: Int): ByteArray {
        val out = ByteArray(expectedSize)
        var ip = 0
        var op = 0
        val ipEnd = input.size
        val opEnd = expectedSize
        var t: Int
        var next: Int = 0
        var state = 0
        var mPos: Int
        var gotoMatchNext = false

        if (ipEnd <= 2) return Arrays.copyOf(out, op)

        var bitstreamVersion = 0
        if (ipEnd >= 5 && (input[ip].toInt() and 0xFF) == 17) {
            bitstreamVersion = input[ip + 1].toInt() and 0xFF
            ip += 2
        }

        if ((input[ip].toInt() and 0xFF) > 17) {
            t = (input[ip].toInt() and 0xFF) - 17
            ip++
            if (t < 4) {
                next = t
                gotoMatchNext = true
            } else {
                if (op + t > opEnd || ip + t > ipEnd) return Arrays.copyOf(out, op)
                for (i in 0 until t) out[op++] = input[ip++]
                state = 4
            }
        }

        while (true) {
            if (gotoMatchNext) {
                gotoMatchNext = false
                state = next
                t = next
                if (t > 0) {
                    if (ip + t > ipEnd) return Arrays.copyOf(out, op)
                    if (op + t > opEnd) return Arrays.copyOf(out, op)
                    while (t > 0) {
                        out[op++] = input[ip++]
                        t--
                    }
                }
                if (ip >= ipEnd) break
                t = input[ip++].toInt() and 0xFF
            } else {
                if (ip >= ipEnd) break
                t = input[ip++].toInt() and 0xFF
            }

            if (t < 16) {
                if (state == 0) {
                    if (t == 0) {
                        val ipLast = ip
                        while (ip < ipEnd && input[ip].toInt() and 0xFF == 0) {
                            ip++
                        }
                        if (ip >= ipEnd) break
                        val offset = ip - ipLast
                        if (offset > MAX_255_COUNT) return Arrays.copyOf(out, op)
                        t += (offset shl 8) - offset
                        t += 15 + (input[ip++].toInt() and 0xFF)
                    }
                    t += 3
                    if (op + t > opEnd || ip + t > ipEnd) return Arrays.copyOf(out, op)
                    for (i in 0 until t) out[op++] = input[ip++]
                    state = 4
                    continue
                } else if (state != 4) {
                    next = t and 3
                    mPos = op - 1 - (t shr 2) - ((input[ip++].toInt() and 0xFF) shl 2)
                    if (mPos < 0 || op + 2 > opEnd) return Arrays.copyOf(out, op)
                    out[op++] = out[mPos++]
                    out[op++] = out[mPos]
                    gotoMatchNext = true
                    continue
                } else {
                    next = t and 3
                    mPos = op - (1 + M2_MAX_OFFSET) - (t shr 2) - ((input[ip++].toInt() and 0xFF) shl 2)
                    t = 3
                }
            } else if (t >= 64) {
                next = t and 3
                mPos = op - 1 - ((t shr 2) and 7) - ((input[ip++].toInt() and 0xFF) shl 3)
                t = (t shr 5) - 1 + 2
            } else if (t >= 32) {
                t = (t and 31) + 2
                if (t == 2) {
                    val ipLast = ip
                    while (ip < ipEnd && input[ip].toInt() and 0xFF == 0) {
                        ip++
                    }
                    if (ip >= ipEnd) return Arrays.copyOf(out, op)
                    val offset = ip - ipLast
                    if (offset > MAX_255_COUNT) return Arrays.copyOf(out, op)
                    t += (offset shl 8) - offset
                    t += 31 + (input[ip++].toInt() and 0xFF)
                    if (ip + 2 > ipEnd) return Arrays.copyOf(out, op)
                }
                next = (input[ip].toInt() and 0xFF) or ((input[ip + 1].toInt() and 0xFF) shl 8)
                mPos = op - 1 - (next shr 2)
                next = next and 3
                ip += 2
            } else {
                if (ip + 2 > ipEnd) return Arrays.copyOf(out, op)
                next = (input[ip].toInt() and 0xFF) or ((input[ip + 1].toInt() and 0xFF) shl 8)
                if ((next and 0xFFFC) == 0xFFFC && (t and 0xF8) == 0x18 && bitstreamVersion != 0) {
                    if (ip + 3 > ipEnd) return Arrays.copyOf(out, op)
                    t = (t and 7) or ((input[ip + 2].toInt() and 0xFF) shl 3)
                    t += MIN_ZERO_RUN_LENGTH
                    if (op + t > opEnd) return Arrays.copyOf(out, op)
                    for (i in 0 until t) out[op++] = 0
                    next = next and 3
                    ip += 3
                    gotoMatchNext = true
                    continue
                } else {
                    mPos = op - ((t and 8) shl 11)
                    t = (t and 7) + 2
                    if (t == 2) {
                        val ipLast = ip
                        while (ip < ipEnd && input[ip].toInt() and 0xFF == 0) {
                            ip++
                        }
                        if (ip >= ipEnd) return Arrays.copyOf(out, op)
                        val offset = ip - ipLast
                        if (offset > MAX_255_COUNT) return Arrays.copyOf(out, op)
                        t += (offset shl 8) - offset
                        t += 7 + (input[ip++].toInt() and 0xFF)
                        if (ip + 2 > ipEnd) return Arrays.copyOf(out, op)
                        next = (input[ip].toInt() and 0xFF) or ((input[ip + 1].toInt() and 0xFF) shl 8)
                    }
                    ip += 2
                    mPos -= next shr 2
                    next = next and 3
                    if (mPos == op) {
                        return Arrays.copyOf(out, op)
                    }
                    mPos -= 0x4000
                }
            }

            if (mPos < 0 || op + t > opEnd) return Arrays.copyOf(out, op)
            out[op++] = out[mPos++]
            out[op++] = out[mPos++]
            var count = t - 2
            while (count > 0) {
                out[op++] = out[mPos++]
                count--
            }

            state = next
            t = next
            if (t > 0) {
                if (ip + t > ipEnd) return Arrays.copyOf(out, op)
                if (op + t > opEnd) return Arrays.copyOf(out, op)
                while (t > 0) {
                    out[op++] = input[ip++]
                    t--
                }
            }
        }

        return Arrays.copyOf(out, op)
    }
}
