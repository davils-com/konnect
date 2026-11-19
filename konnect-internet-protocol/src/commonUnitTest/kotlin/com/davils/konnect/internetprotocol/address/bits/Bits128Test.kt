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

import com.davils.konnect.internetprotocol.IpVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val HALF: Int = 64
private const val WIDTH: Int = 128
private const val LOW_BITS_ABOVE_HALF: Int = 70
private const val SIX_ONES: ULong = 0b111111uL
private const val EIGHT: ULong = 8uL
private const val THREE: Int = 3

private val all: Bits128 = Bits128(high = ULong.MAX_VALUE, low = ULong.MAX_VALUE)
private val firstOfHigh: Bits128 = Bits128(high = 1uL, low = 0uL)

class Bits128Test : FunSpec({
    context("trailing zeros") {
        test("counts every bit of zero") {
            Bits128.ZERO.trailingZeros shouldBe WIDTH
        }

        test("counts the zeros of the low half") {
            Bits128(high = 0uL, low = EIGHT).trailingZeros shouldBe THREE
        }

        test("counts across the halves when the low half is zero") {
            firstOfHigh.trailingZeros shouldBe HALF
        }
    }

    context("bitwise operations") {
        test("intersect keeps the bits set in both") {
            all intersect firstOfHigh shouldBe firstOfHigh
        }

        test("union keeps the bits set in either") {
            Bits128.ONE union firstOfHigh shouldBe Bits128(high = 1uL, low = 1uL)
        }

        test("complement flips every bit") {
            Bits128.ZERO.complement() shouldBe all
        }

        test("shifting right moves bits from the high into the low half") {
            firstOfHigh shr 1 shouldBe Bits128.ofLow(ULong.MAX_VALUE xor (ULong.MAX_VALUE shr 1))
        }

        test("shifting right by a half or more leaves only the high half") {
            firstOfHigh shr HALF shouldBe Bits128.ONE
        }

        test("shifting right by zero changes nothing") {
            all shr 0 shouldBe all
        }

        test("shifting right by the full width leaves nothing") {
            all shr WIDTH shouldBe Bits128.ZERO
        }
    }

    context("arithmetic") {
        test("adding carries from the low into the high half") {
            Bits128(high = 0uL, low = ULong.MAX_VALUE) + Bits128.ONE shouldBe firstOfHigh
        }

        test("adding past the top is no number") {
            (all + Bits128.ONE).shouldBeNull()
        }

        test("adding high halves past the top is no number") {
            (Bits128(high = ULong.MAX_VALUE, low = 0uL) + firstOfHigh).shouldBeNull()
        }

        test("subtracting borrows from the high half") {
            firstOfHigh - Bits128.ONE shouldBe Bits128(high = 0uL, low = ULong.MAX_VALUE)
        }

        test("subtracting below zero is no number") {
            (Bits128.ZERO - Bits128.ONE).shouldBeNull()
        }
    }

    context("masks") {
        test("no ones is zero") {
            Bits128.lowOnes(0) shouldBe Bits128.ZERO
        }

        test("ones above a half spill into the high half") {
            Bits128.lowOnes(LOW_BITS_ABOVE_HALF) shouldBe Bits128(high = SIX_ONES, low = ULong.MAX_VALUE)
        }

        test("the full width is all ones") {
            Bits128.lowOnes(WIDTH) shouldBe all
        }
    }

    context("bytes") {
        test("read back from the bytes they are written to") {
            val bits = Bits128(high = firstOfHigh.high, low = EIGHT)

            Bits128.fromByteArray(bits.toByteArray(IpVersion.V6)) shouldBe bits
        }
    }

    context("identity") {
        test("orders by the high half before the low half") {
            (firstOfHigh > Bits128(high = 0uL, low = ULong.MAX_VALUE)) shouldBe true
            (Bits128.ONE < Bits128(high = 0uL, low = EIGHT)) shouldBe true
        }

        test("equal bits are equal and share a hash code") {
            Bits128(high = 1uL, low = EIGHT) shouldBe Bits128(high = 1uL, low = EIGHT)
            Bits128(high = 1uL, low = EIGHT).hashCode() shouldBe Bits128(high = 1uL, low = EIGHT).hashCode()
            Bits128(high = 1uL, low = EIGHT) shouldNotBe "Bits128"
        }

        test("names both halves") {
            Bits128(high = 1uL, low = EIGHT).toString() shouldBe "Bits128(high=1, low=8)"
        }
    }
})
