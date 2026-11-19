/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.konnect.internetprotocol.address.bits

private const val HALF: Int = ULong.SIZE_BITS
private const val BYTE_MASK: ULong = 0xFFuL
private const val HASH_FACTOR: Int = 31

internal class Bits128(val high: ULong, val low: ULong) : Comparable<Bits128> {
    val trailingZeros: Int
        get() = low.countTrailingZeroBits().takeIf { low != 0uL } ?: HALF + high.countTrailingZeroBits()

    infix fun intersect(other: Bits128): Bits128 = Bits128(high = high and other.high, low = low and other.low)

    infix fun union(other: Bits128): Bits128 = Bits128(high = high or other.high, low = low or other.low)

    fun complement(): Bits128 = Bits128(high = high.inv(), low = low.inv())

    infix fun shr(count: Int): Bits128 {
        if (count >= HALF) return ofLow(high.shiftedRight(count - HALF))
        return Bits128(
            high = high.shiftedRight(count),
            low = low.shiftedRight(count) or high.shiftedLeft(HALF - count),
        )
    }

    operator fun plus(other: Bits128): Bits128? {
        val sumLow = low + other.low
        val carry = 1uL.takeIf { sumLow < low } ?: 0uL
        val partialHigh = high + other.high
        val sumHigh = partialHigh + carry
        if (partialHigh < high || sumHigh < partialHigh) return null
        return Bits128(high = sumHigh, low = sumLow)
    }

    operator fun minus(other: Bits128): Bits128? {
        if (other > this) return null
        val borrow = 1uL.takeIf { other.low > low } ?: 0uL
        return Bits128(high = high - other.high - borrow, low = low - other.low)
    }

    override fun compareTo(other: Bits128): Int {
        val byHigh = high.compareTo(other.high)
        if (byHigh != 0) return byHigh
        return low.compareTo(other.low)
    }

    override fun equals(other: Any?): Boolean = other is Bits128 && high == other.high && low == other.low

    override fun hashCode(): Int = HASH_FACTOR * high.hashCode() + low.hashCode()

    override fun toString(): String = "Bits128(high=$high, low=$low)"

    companion object {
        val ZERO: Bits128 = ofLow(0uL)

        val ONE: Bits128 = ofLow(1uL)

        fun ofLow(low: ULong): Bits128 = Bits128(high = 0uL, low = low)

        fun lowOnes(count: Int): Bits128 = Bits128(high = onesOf(count - HALF), low = onesOf(count))

        fun fromByteArray(bytes: ByteArray): Bits128 = bytes.fold(ZERO) { bits, byte -> bits.appended(byte) }

        private fun Bits128.appended(byte: Byte): Bits128 {
            val carried = low.shiftedRight(HALF - Byte.SIZE_BITS)
            val next = low.shiftedLeft(Byte.SIZE_BITS) or (byte.toULong() and BYTE_MASK)
            return Bits128(high = high.shiftedLeft(Byte.SIZE_BITS) or carried, low = next)
        }

        private fun onesOf(count: Int): ULong {
            val clamped = count.coerceIn(minimumValue = 0, maximumValue = HALF)
            return ULong.MAX_VALUE.shiftedRight(HALF - clamped)
        }
    }
}

private fun ULong.shiftedLeft(count: Int): ULong = (this shl count).takeIf { count < HALF } ?: 0uL

private fun ULong.shiftedRight(count: Int): ULong = (this shr count).takeIf { count < HALF } ?: 0uL
