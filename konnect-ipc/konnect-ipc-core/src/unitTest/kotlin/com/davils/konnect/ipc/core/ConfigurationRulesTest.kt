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

package com.davils.konnect.ipc.core

import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.fixture.HELPER
import com.davils.konnect.ipc.core.fixture.shouldNotEcho
import com.davils.kore.core.validation.validate
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private const val SUBJECT: String = "checked"
private const val CASES: Int = 500

private fun codesOf(address: IpcAddress?, bufferSize: Int): List<ViolationCode> {
    val report = Checked(address, bufferSize).validate(SUBJECT)
    return report.violations.map { violation -> violation.code }
}

class ConfigurationRulesTest : FunSpec({
    context("the rules of a configuration") {
        test("accept an address and a buffer between the limits") {
            codesOf(HELPER, Limits.MIN_BUFFER_SIZE) shouldBe emptyList()
            codesOf(HELPER, Limits.MAX_BUFFER_SIZE) shouldBe emptyList()
        }

        test("report a missing address and a buffer outside the limits") {
            codesOf(address = null, bufferSize = Limits.MAX_BUFFER_SIZE + 1) shouldBe listOf(
                ViolationCode.REQUIRED,
                IpcViolationCodes.INVALID_BUFFER_SIZE,
            )
            codesOf(HELPER, Limits.MIN_BUFFER_SIZE - 1) shouldBe listOf(IpcViolationCodes.INVALID_BUFFER_SIZE)
        }

        test("never quote the buffer size they refused") {
            checkAll(CASES, Arb.int()) { size ->
                val report = Checked(HELPER, size).validate(SUBJECT)

                report.violations.forEach { violation -> violation.message.shouldNotEcho(size.toString()) }
            }
        }
    }

    context("codes") {
        test("keep their stable values") {
            IpcViolationCodes.NOBODY_ALLOWED.value shouldBe "konnect.ipc.nobody-allowed"
            IpcViolationCodes.INVALID_BUFFER_SIZE.value shouldBe "konnect.ipc.invalid-buffer-size"
        }
    }
})
