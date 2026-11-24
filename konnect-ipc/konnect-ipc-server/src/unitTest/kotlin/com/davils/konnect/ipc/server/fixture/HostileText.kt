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

package com.davils.konnect.ipc.server.fixture

import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map

private const val MOST_PIECES: Int = 64
private const val LONG_PIECE: Int = 100
private const val SHORTEST_ECHO: Int = 12
private const val PRINTABLE_FIRST: Char = ' '
private const val PRINTABLE_LAST: Char = '~'

private val ipcFragments: List<String> = listOf(
    "a", "z", "0", "9", ".", "-", "_", "/", "\\", "..", "S-1-5-18", "-1", "S", "konnect", "٠", "１",
    "x".repeat(LONG_PIECE),
)

private val hostilePieces: Arb<List<String>> =
    Arb.list(Arb.element(HostileFragments.all + ipcFragments), 0..MOST_PIECES)

internal val hostileText: Arb<String> = hostilePieces.map { pieces -> pieces.joinToString(separator = "") }

internal fun String?.shouldNotEcho(input: String) {
    val text = orEmpty()
    withClue("a message must be printable ASCII") {
        text.all { character -> character in PRINTABLE_FIRST..PRINTABLE_LAST } shouldBe true
    }
    withClue("a message must be safe to show") {
        isSafeToShow(text) shouldBe true
    }
    if (input.length < SHORTEST_ECHO) return
    withClue("a message must not quote its input") {
        text.contains(input) shouldBe false
    }
}
