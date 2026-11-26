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

package com.davils.konnect.core.port

import com.davils.kore.core.arguments.conversion.ConversionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

class PortConvertersTest : FunSpec({
    context("the converter of ports") {
        test("reads a port") {
            PortConverters.port.convert("8080") shouldBe Port.parse("8080")
            PortConverters.port.typeName shouldBe "port"
        }

        test("refuses what is no port, without repeating it") {
            listOf("", "65536", "-1", "08080", " 80").forEach { raw ->
                shouldThrow<ConversionException> { PortConverters.port.convert(raw) }
            }
            checkAll(hostileText) { raw ->
                val failure = runCatching { PortConverters.port.convert(raw) }.exceptionOrNull()

                failure?.message.shouldBePlainAscii()
            }
        }
    }

    context("the converter of port ranges") {
        test("reads a range and refuses what is none") {
            PortConverters.portRange.convert("8000-8080") shouldBe PortRange.parse("8000-8080")
            PortConverters.portRange.typeName shouldBe "port range"
            shouldThrow<ConversionException> { PortConverters.portRange.convert("8080-8000x") }
        }
    }
})
