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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import java.math.BigInteger

private const val CASES: Int = 500
private const val EXPONENT_126: Int = 126
private const val EXPONENT_128: Int = 128
private const val MAX_TERMS: Int = 3

class AddressCountConversionsTest : FunSpec({
    context("toBigInteger") {
        test("keeps the value of the bounds") {
            AddressCount.ZERO.toBigInteger() shouldBe BigInteger.ZERO
            AddressCount.MAX.toBigInteger() shouldBe BigInteger.ONE.shiftLeft(EXPONENT_128)
        }

        test("agrees with BigInteger on sums of powers of two") {
            val exponents = Arb.list(Arb.int(0..EXPONENT_126), 1..MAX_TERMS)

            checkAll(CASES, exponents) { terms ->
                val counts = terms.map { exponent -> AddressCount.ofPowerOfTwo(exponent) }
                val powers = terms.map { exponent -> BigInteger.ONE.shiftLeft(exponent) }
                val count = counts.fold(AddressCount.ZERO) { total, term -> total + term }
                val expected = powers.fold(BigInteger.ZERO) { total, term -> total + term }

                count.toBigInteger() shouldBe expected
            }
        }
    }
})
