package io.github.gdict.core

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class MddStructureDiagnosticTest {

    private val mddPath = System.getProperty("mdd.path")
        ?: """D:\workspace\Gdict\Cambridge_English_Pronouncing_Dictionary_18th.mdd"""

    @Test
    fun analyzeMddStructure() {
        val mddFile = File(mddPath)
        if (!mddFile.exists()) {
            println("MDD not found: $mddPath")
            return
        }

        println("======= MDD 结构诊断 =======")
        println("File: ${mddFile.name}")
        println("Size: ${mddFile.length()} bytes")

        RandomAccessFile(mddFile, "r").use { raf ->
            val fileLen = raf.length()
            println("\n--- File header (first 20 bytes) ---")
            val first20 = ByteArray(20)
            raf.readFully(first20)
            println("Raw hex: ${first20.joinToString(" ") { "%02x".format(it) }}")

            val headerLenBytes = ByteArray(4)
            System.arraycopy(first20, 0, headerLenBytes, 0, 4)
            val headerLen = ((headerLenBytes[0].toInt() and 0xFF shl 24) or
                    (headerLenBytes[1].toInt() and 0xFF shl 16) or
                    (headerLenBytes[2].toInt() and 0xFF shl 8) or
                    (headerLenBytes[3].toInt() and 0xFF))
            println("Header length (big-endian int32): $headerLen")

            raf.seek(4)
            val headerData = ByteArray(minOf(headerLen.toInt(), fileLen.toInt() - 4))
            raf.readFully(headerData)

            val headerStr = try {
                String(headerData, Charsets.UTF_16LE)
            } catch (e: Exception) {
                String(headerData, Charsets.UTF_8)
            }
            println("\n--- Header content (first 3000 chars) ---")
            println(headerStr.take(3000))

            println("\n--- Searching for CSS references ---")
            val cssPatterns = listOf(
                Regex("""cepd18\.css""", RegexOption.IGNORE_CASE),
                Regex("""\.css""", RegexOption.IGNORE_CASE),
                Regex("""<style""", RegexOption.IGNORE_CASE),
                Regex("""stylesheet""", RegexOption.IGNORE_CASE)
            )
            for (pattern in cssPatterns) {
                val matches = pattern.findAll(headerStr).toList()
                if (matches.isNotEmpty()) {
                    println("  Pattern '${pattern.pattern}': ${matches.size} matches")
                    for (match in matches.take(5)) {
                        val start = maxOf(0, match.range.first - 30)
                        val end = minOf(headerStr.length, match.range.last + 30)
                        println("    ...${headerStr.substring(start, end)}...")
                    }
                } else {
                    println("  Pattern '${pattern.pattern}': NOT FOUND")
                }
            }

            println("\n--- Header analysis ---")
            val lines = headerStr.split("\n").filter { it.trim().isNotEmpty() }
            println("Total non-empty lines: ${lines.size}")
            for ((i, line) in lines.take(50).withIndex()) {
                println("  [$i] ${line.take(200)}")
            }

            println("\n--- After header position ---")
            val afterHeaderPos = 4 + headerLen.toInt()
            println("Position after header: $afterHeaderPos / $fileLen")
            if (afterHeaderPos < fileLen) {
                raf.seek(afterHeaderPos)
                val afterHeader = ByteArray(minOf(100, (fileLen - afterHeaderPos).toInt()))
                raf.readFully(afterHeader)
                println("Next 100 bytes hex: ${afterHeader.joinToString(" ") { "%02x".format(it) }}")
                println("Next 100 bytes text: ${String(afterHeader, Charsets.ISO_8859_1)}")
            }
        }
    }
}
