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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val TWO_TO_THE_64: String = "18446744073709551616"
private const val TWO_TO_THE_128: String = "340282366920938463463374607431768211456"
private const val TWO_TO_THE_128_LESS_ONE: String = "340282366920938463463374607431768211455"
private const val BILLION: ULong = 1_000_000_000uL
private const val EXPONENT_64: Int = 64
private const val EXPONENT_127: Int = 127
private const val EXPONENT_128: Int = 128

private fun allButOne(): AddressCount {
    val halves = AddressCount.ofPowerOfTwo(EXPONENT_127)
    val lower = List(EXPONENT_127) { exponent -> AddressCount.ofPowerOfTwo(exponent) }
    return lower.fold(halves) { total, count -> total + count }
}

class AddressCountTest : FunSpec({
    context("creation") {
        test("knows zero and the largest count") {
            AddressCount.ZERO.toString() shouldBe "0"
            AddressCount.MAX.toString() shouldBe TWO_TO_THE_128
            AddressCount.ofPowerOfTwo(EXPONENT_128) shouldBe AddressCount.MAX
        }

        test("holds powers of two") {
            AddressCount.ofPowerOfTwo(0) shouldBe AddressCount.of(1uL)
            AddressCount.ofPowerOfTwo(EXPONENT_64).toString() shouldBe TWO_TO_THE_64
        }

        test("refuses an exponent outside 0..128") {
            shouldThrow<IllegalArgumentException> { AddressCount.ofPowerOfTwo(-1) }
            shouldThrow<IllegalArgumentException> { AddressCount.ofPowerOfTwo(EXPONENT_128 + 1) }
        }
    }

    context("decimal text") {
        test("shows small and large values without separators") {
            AddressCount.of(BILLION).toString() shouldBe "1000000000"
            AddressCount.of(ULong.MAX_VALUE).toString() shouldBe "18446744073709551615"
            allButOne().toString() shouldBe TWO_TO_THE_128_LESS_ONE
        }
    }

    context("addition") {
        test("carries from the low into the high half") {
            AddressCount.of(ULong.MAX_VALUE) + AddressCount.of(1uL) shouldBe AddressCount.ofPowerOfTwo(EXPONENT_64)
        }

        test("reaches the largest count exactly") {
            allButOne() + AddressCount.of(1uL) shouldBe AddressCount.MAX
            AddressCount.MAX + AddressCount.ZERO shouldBe AddressCount.MAX
            AddressCount.ZERO + AddressCount.MAX shouldBe AddressCount.MAX
        }

        test("refuses a sum above the largest count") {
            shouldThrow<ArithmeticException> { AddressCount.MAX + AddressCount.of(1uL) }
            shouldThrow<ArithmeticException> { AddressCount.of(1uL) + AddressCount.MAX }
            shouldThrow<ArithmeticException> { AddressCount.MAX + AddressCount.MAX }
            shouldThrow<ArithmeticException> { allButOne() + allButOne() + AddressCount.of(2uL) }
        }

        test("refuses a sum that wraps past the largest count") {
            val half = AddressCount.ofPowerOfTwo(EXPONENT_127)

            shouldThrow<ArithmeticException> { allButOne() + half }
        }
    }

    context("comparison") {
        test("orders by value") {
            (AddressCount.ZERO < AddressCount.of(1uL)) shouldBe true
            (AddressCount.ofPowerOfTwo(EXPONENT_64) > AddressCount.of(ULong.MAX_VALUE)) shouldBe true
            (allButOne() < AddressCount.MAX) shouldBe true
            (AddressCount.MAX > allButOne()) shouldBe true
        }

        test("is equal only to the same value") {
            AddressCount.of(2uL).hashCode() shouldBe AddressCount.of(2uL).hashCode()
            AddressCount.of(2uL) shouldNotBe AddressCount.of(1uL)
            AddressCount.MAX shouldNotBe AddressCount.ZERO
            AddressCount.of(2uL) shouldNotBe "2"
        }
    }

    context("toULongOrNull") {
        test("returns a value that fits into 64 bits") {
            AddressCount.of(ULong.MAX_VALUE).toULongOrNull() shouldBe ULong.MAX_VALUE
        }

        test("returns null for a larger value") {
            val twoToThe64 = AddressCount.ofPowerOfTwo(EXPONENT_64)

            twoToThe64.toULongOrNull().shouldBeNull()
            AddressCount.MAX.toULongOrNull().shouldBeNull()
        }
    }
})
