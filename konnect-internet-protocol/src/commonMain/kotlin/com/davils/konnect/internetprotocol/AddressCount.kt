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

package com.davils.konnect.internetprotocol

import com.davils.konnect.internetprotocol.address.bits.Bits128

private const val LIMB_BITS: Int = 32
private const val LIMB_MASK: ULong = 0xFFFF_FFFFuL
private const val CHUNK: ULong = 1_000_000_000uL
private const val CHUNK_DIGITS: Int = 9
private const val HASH_FACTOR: Int = 31
private const val MAX_EXPONENT: Int = 128
private const val TOO_LARGE_MESSAGE: String = "An address count cannot exceed 2^128"

/**
 * The number of addresses in a range or a network: a whole number from `0` to `2^128`, both included.
 *
 * A count is exact on every platform, also for the whole IPv6 address space, whose `2^128` addresses do not fit into
 * 128 bits. Counts are ordered by their value, and [toString] shows the value in decimal. On the JVM, an extension
 * turns a count into a `java.math.BigInteger`. Instances are immutable.
 *
 * @since 1.0.0
 */
public class AddressCount private constructor(
    private val isFull: Boolean,
    private val bits: Bits128,
) : Comparable<AddressCount> {
    /**
     * Adds [other] to this count.
     *
     * @param other the count to add.
     * @return the sum.
     * @throws ArithmeticException if the sum is above `2^128`.
     * @since 1.0.0
     */
    public operator fun plus(other: AddressCount): AddressCount {
        val isAnyFull = isFull || other.isFull
        val sum = bits + other.bits ?: return overflowed(first = bits, second = other.bits, isAnyFull = isAnyFull)
        return checked(isFull = isFull, isOtherFull = other.isFull, bits = sum)
    }

    /**
     * Returns the value of this count as an unsigned 64-bit number, if it fits into one.
     *
     * @return the value, or `null` if it is `2^64` or more.
     * @since 1.0.0
     */
    public fun toULongOrNull(): ULong? = bits.low.takeIf { !isFull && bits.high == 0uL }

    internal fun lessTwo(): AddressCount {
        val remainder = bits - Bits128.ofLow(2uL) ?: Bits128.ZERO
        return AddressCount(isFull = false, bits = remainder)
    }

    /**
     * Compares this count with [other] by their values.
     *
     * @param other the count to compare with.
     * @return a negative number, zero or a positive number as this count is below, equal to or above [other].
     * @since 1.0.0
     */
    override fun compareTo(other: AddressCount): Int {
        val byFull = isFull.compareTo(other.isFull)
        if (byFull != 0) return byFull
        return bits.compareTo(other.bits)
    }

    /**
     * Tells whether [other] is a count of the same value.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal count.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is AddressCount && isFull == other.isFull && bits == other.bits

    /**
     * Computes a hash code from the value.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * isFull.hashCode() + bits.hashCode()

    /**
     * Shows the value of this count in decimal, such as `256` or `340282366920938463463374607431768211456`.
     *
     * @return the value in decimal, without separators.
     * @since 1.0.0
     */
    override fun toString(): String {
        val limbs = limbsOf(isFull = isFull, bits = bits)
        val chunks = buildList {
            while (limbs.any { limb -> limb != 0uL }) {
                add(divideInPlace(limbs))
            }
        }
        if (chunks.isEmpty()) return "0"
        return render(chunks)
    }

    /**
     * The smallest and the largest count and the creation of counts.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The count `0`.
         *
         * @since 1.0.0
         */
        public val ZERO: AddressCount = AddressCount(isFull = false, bits = Bits128.ZERO)

        /**
         * The count `2^128`, the number of all IPv6 addresses and the largest count.
         *
         * @since 1.0.0
         */
        public val MAX: AddressCount = AddressCount(isFull = true, bits = Bits128.ZERO)

        /**
         * Creates the count of [value].
         *
         * @param value the value of the count.
         * @return the count.
         * @since 1.0.0
         */
        public fun of(value: ULong): AddressCount = AddressCount(isFull = false, bits = Bits128.ofLow(value))

        /**
         * Creates the count `2^exponent`, such as the number of addresses in a network with `exponent` host bits.
         *
         * @param exponent the power of two, from `0` to `128`.
         * @return the count.
         * @throws IllegalArgumentException if [exponent] is outside `0..128`.
         * @since 1.0.0
         */
        public fun ofPowerOfTwo(exponent: Int): AddressCount {
            require(exponent in 0..MAX_EXPONENT) { "An exponent must be in 0..$MAX_EXPONENT, but was $exponent" }
            if (exponent == MAX_EXPONENT) return MAX
            return AddressCount(isFull = false, bits = Bits128.lowOnes(exponent) + Bits128.ONE ?: Bits128.ZERO)
        }

        internal fun ofSpan(first: Bits128, last: Bits128): AddressCount {
            val distance = last - first ?: Bits128.ZERO
            val size = distance + Bits128.ONE
            return size?.let { bits -> AddressCount(isFull = false, bits = bits) } ?: MAX
        }

        private fun checked(isFull: Boolean, isOtherFull: Boolean, bits: Bits128): AddressCount {
            val areBothFull = isFull && isOtherFull
            val isAnyFull = isFull || isOtherFull
            val hasBits = isAnyFull && bits != Bits128.ZERO
            if (areBothFull || hasBits) throw ArithmeticException(TOO_LARGE_MESSAGE)
            return AddressCount(isFull = isAnyFull, bits = bits)
        }

        private fun overflowed(first: Bits128, second: Bits128, isAnyFull: Boolean): AddressCount {
            val high = first.high + second.high + carryOf(first = first.low, second = second.low)
            val wrapped = Bits128(high = high, low = first.low + second.low)
            if (isAnyFull || wrapped != Bits128.ZERO) throw ArithmeticException(TOO_LARGE_MESSAGE)
            return MAX
        }

        private fun carryOf(first: ULong, second: ULong): ULong = 1uL.takeIf { first + second < first } ?: 0uL

        private fun limbsOf(isFull: Boolean, bits: Bits128): MutableList<ULong> = mutableListOf(
            1uL.takeIf { isFull } ?: 0uL,
            bits.high shr LIMB_BITS,
            bits.high and LIMB_MASK,
            bits.low shr LIMB_BITS,
            bits.low and LIMB_MASK,
        )

        private fun divideInPlace(limbs: MutableList<ULong>): ULong {
            var remainder = 0uL
            limbs.indices.forEach { index ->
                val current = remainder shl LIMB_BITS or limbs[index]
                limbs[index] = current / CHUNK
                remainder = current % CHUNK
            }
            return remainder
        }

        private fun render(chunks: List<ULong>): String = buildString {
            append(chunks.last())
            val lower = chunks.dropLast(1)
            lower.asReversed().forEach { chunk -> append(chunk.toString().padStart(CHUNK_DIGITS, '0')) }
        }
    }
}
