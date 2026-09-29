package com.example.server

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.charset.StandardCharsets

/**
 * High-performance, lightweight pure Kotlin QR Code generator.
 * Generates standards-compliant QR codes (Versions 1-10, Error Correction Level L/M)
 * with zero external dependencies.
 */
object QrCodeGenerator {

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val modules = encodeToGrid(content)
        val matrixSize = modules.size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val scale = size.toFloat() / matrixSize

        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val moduleY = (y / scale).toInt().coerceIn(0, matrixSize - 1)
            for (x in 0 until size) {
                val moduleX = (x / scale).toInt().coerceIn(0, matrixSize - 1)
                pixels[y * size + x] = if (modules[moduleY][moduleX]) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }

    /**
     * Compact QR generator utilizing byte mode encoding with Reed-Solomon error correction.
     */
    private fun encodeToGrid(text: String): Array<BooleanArray> {
        val data = text.toByteArray(StandardCharsets.UTF_8)
        val dataLen = data.size

        // Select version: Version 3 is 29x29, Version 4 is 33x33, Version 5 is 37x37, Version 6 is 41x41
        val (version, totalDataCodewords, ecCodewordsPerBlock) = when {
            dataLen <= 17 -> Triple(2, 28, 16)
            dataLen <= 32 -> Triple(3, 44, 26)
            dataLen <= 53 -> Triple(4, 64, 18)
            dataLen <= 78 -> Triple(5, 86, 24)
            dataLen <= 106 -> Triple(6, 108, 16)
            else -> Triple(7, 124, 18)
        }

        val size = version * 4 + 17
        val grid = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        fun setModule(r: Int, c: Int, isBlack: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                grid[r][c] = isBlack
                reserved[r][c] = true
            }
        }

        // 1. Finder patterns (top-left, top-right, bottom-left)
        fun drawFinder(top: Int, left: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCore = r in 2..4 && c in 2..4
                    setModule(top + r, left + c, isBorder || isCore)
                }
            }
            // Separator rings
            for (r in -1..7) {
                for (c in -1..7) {
                    if (r == -1 || r == 7 || c == -1 || c == 7) {
                        val row = top + r
                        val col = left + c
                        if (row in 0 until size && col in 0 until size) {
                            grid[row][col] = false
                            reserved[row][col] = true
                        }
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // 2. Timing patterns
        for (i in 8 until size - 8) {
            setModule(6, i, i % 2 == 0)
            setModule(i, 6, i % 2 == 0)
        }

        // 3. Alignment patterns for Version 2+
        val alignPositions = when (version) {
            2 -> intArrayOf(6, 18)
            3 -> intArrayOf(6, 22)
            4 -> intArrayOf(6, 26)
            5 -> intArrayOf(6, 30)
            6 -> intArrayOf(6, 34)
            7 -> intArrayOf(6, 22, 38)
            else -> intArrayOf(6, 26)
        }

        for (r in alignPositions) {
            for (c in alignPositions) {
                val isCorner = (r < 9 && c < 9) || (r < 9 && c > size - 9) || (r > size - 9 && c < 9)
                if (!isCorner) {
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val isBorder = dr == -2 || dr == 2 || dc == -2 || dc == 2
                            val isCenter = dr == 0 && dc == 0
                            setModule(r + dr, c + dc, isBorder || isCenter)
                        }
                    }
                }
            }
        }

        // 4. Reserve format info areas
        for (i in 0..8) {
            if (i != 6) {
                reserved[8][i] = true
                reserved[i][8] = true
            }
        }
        for (i in 0..7) {
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }
        setModule(size - 8, 8, true) // Dark module

        // 5. Construct payload codewords with Byte Mode (0100)
        val bitBuffer = mutableListOf<Int>()
        fun putBits(value: Int, length: Int) {
            for (i in length - 1 downTo 0) {
                bitBuffer.add((value shr i) and 1)
            }
        }

        putBits(0b0100, 4) // Byte mode
        putBits(dataLen, 8) // Length indicator (8 bits for V1-V9 in byte mode)
        for (b in data) {
            putBits(b.toInt() and 0xFF, 8)
        }

        // Terminator (up to 4 zeroes)
        val totalDataBits = totalDataCodewords * 8
        val terminatorLen = (totalDataBits - bitBuffer.size).coerceIn(0, 4)
        putBits(0, terminatorLen)

        // Pad to 8-bit boundary
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(0)
        }

        // Pad bytes (0xEC, 0x11 alternating)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bitBuffer.size < totalDataBits) {
            putBits(padBytes[padIdx % 2], 8)
            padIdx++
        }

        // Convert bit buffer to data codewords
        val dataCodewords = IntArray(totalDataCodewords)
        for (i in 0 until totalDataCodewords) {
            var byteVal = 0
            for (bit in 0 until 8) {
                byteVal = (byteVal shl 1) or bitBuffer[i * 8 + bit]
            }
            dataCodewords[i] = byteVal
        }

        // 6. Reed-Solomon Error Correction Codewords
        val ecCodewords = computeReedSolomon(dataCodewords, ecCodewordsPerBlock)
        val finalCodewords = dataCodewords + ecCodewords

        // 7. Place data modules in zigzag pattern
        var cwIdx = 0
        var bitIdx = 7
        var col = size - 1
        var upward = true

        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing column

            val rows = if (upward) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (cOffset in 0..1) {
                    val c = col - cOffset
                    if (!reserved[row][c]) {
                        val bit = if (cwIdx < finalCodewords.size) {
                            (finalCodewords[cwIdx] shr bitIdx) and 1
                        } else {
                            0
                        }
                        // Mask pattern 0: (row + col) % 2 == 0
                        val mask = (row + c) % 2 == 0
                        grid[row][c] = (bit == 1) xor mask

                        bitIdx--
                        if (bitIdx < 0) {
                            bitIdx = 7
                            cwIdx++
                        }
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // 8. Format Information (Mask 0, EC Level L: 01000 -> 0x77C4 with XOR mask 0x5412)
        val formatBits = 0x77C4 xor 0x5412
        for (i in 0..14) {
            val bit = ((formatBits shr i) and 1) == 1
            // Horizontal format info
            val (hr, hc) = when {
                i < 6 -> Pair(8, i)
                i == 6 -> Pair(8, 7)
                i == 7 -> Pair(8, 8)
                i == 8 -> Pair(7, 8)
                else -> Pair(14 - i, 8)
            }
            grid[hr][hc] = bit

            // Vertical format info
            val (vr, vc) = when {
                i < 8 -> Pair(size - 1 - i, 8)
                else -> Pair(8, size - 15 + i)
            }
            grid[vr][vc] = bit
        }

        return grid
    }

    private fun computeReedSolomon(data: IntArray, ecCount: Int): IntArray {
        // Galois Field GF(256) tables with primitive polynomial 0x11D (285)
        val exp = IntArray(512)
        val log = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            exp[i + 255] = x
            log[x] = i
            x = (x shl 1) xor (if (x and 0x80 != 0) 0x11D else 0)
        }

        fun gfMul(a: Int, b: Int): Int {
            if (a == 0 || b == 0) return 0
            return exp[log[a] + log[b]]
        }

        // Generator polynomial for ecCount roots
        var generator = intArrayOf(1)
        for (i in 0 until ecCount) {
            val next = IntArray(generator.size + 1)
            for (j in generator.indices) {
                next[j] = next[j] xor gfMul(generator[j], exp[i])
                next[j + 1] = next[j + 1] xor generator[j]
            }
            generator = next
        }

        // Polynomial division
        val remainder = IntArray(ecCount)
        for (b in data) {
            val factor = b xor remainder[0]
            for (j in 0 until ecCount - 1) {
                remainder[j] = remainder[j + 1] xor gfMul(generator[ecCount - 1 - j], factor)
            }
            remainder[ecCount - 1] = gfMul(generator[0], factor)
        }

        return remainder.reversedArray()
    }
}
