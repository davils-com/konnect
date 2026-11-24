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

package com.davils.konnect.ipc.core.access

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.fixture.hostileText
import com.davils.konnect.ipc.core.fixture.shouldNotEcho
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val CASES: Int = 1_000
private const val MOST_SUB_AUTHORITIES: Int = 15
private val SID_SHAPE: Regex = Regex("S-1(-[0-9]+)+")

private val wellFormedSids: Arb<String> = Arb.list(Arb.int(min = 0), 1..MOST_SUB_AUTHORITIES).map { numbers ->
    numbers.joinToString(separator = "-", prefix = "S-1-")
}

class IpcUserPropertyTest : FunSpec({
    context("security identifiers") {
        test("of up to fifteen sub-authorities are accepted") {
            checkAll(CASES, wellFormedSids) { text ->
                IpcUser.WindowsSid(text).value shouldBe text
            }
        }

        test("read from hostile text are refused or have the shape of an identifier within the limit") {
            checkAll(CASES, hostileText) { text ->
                val isValid = IpcUser.WindowsSid.isValid(text)

                val isShaped = text.length <= Limits.MAX_SID_LENGTH && SID_SHAPE.matches(text)

                (!isValid || isShaped) shouldBe true
            }
        }

        test("are refused with a message that never quotes the text") {
            checkAll(CASES, hostileText) { text ->
                val failure = runCatching { IpcUser.WindowsSid(text) }.exceptionOrNull()

                failure?.message.shouldNotEcho(text)
            }
        }
    }

    context("unix user names") {
        test("read from hostile text are refused or free of control characters within the limit") {
            checkAll(CASES, hostileText) { text ->
                val isValid = IpcUser.UnixUser.isValid(text)
                val isShown = text.length <= Limits.MAX_USER_NAME_LENGTH && text.none(Char::isISOControl)

                val isName = isShown && text.isNotBlank()

                (!isValid || isName) shouldBe true
            }
        }

        test("are refused with a message that never quotes the text") {
            checkAll(CASES, hostileText) { text ->
                val failure = runCatching { IpcUser.UnixUser(text) }.exceptionOrNull()

                failure?.message.shouldNotEcho(text)
            }
        }
    }
})
