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

import com.davils.kore.testing.HostileFragments
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map

private const val LONGEST_HOSTILE_TEXT: Int = 64
private const val PRINTABLE_FIRST: Char = ' '
private const val PRINTABLE_LAST: Char = '~'

private val networkFragments: List<String> = listOf("0", "1", "8", "9", "-", ".", ":", "/", "\u0660", "\uFF11")

private val hostilePieces: Arb<List<String>> =
    Arb.list(Arb.element(HostileFragments.all + networkFragments), 0..LONGEST_HOSTILE_TEXT)

internal val hostileText: Arb<String> = hostilePieces.map { pieces -> pieces.joinToString(separator = "") }

internal fun String?.shouldBePlainAscii() {
    val text = orEmpty()
    withClue("a message that must be printable ASCII") {
        text.all { character -> character in PRINTABLE_FIRST..PRINTABLE_LAST } shouldBe true
    }
}
