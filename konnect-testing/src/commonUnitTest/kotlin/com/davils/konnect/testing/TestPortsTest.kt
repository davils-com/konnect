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

package com.davils.konnect.testing

import com.davils.konnect.core.port.PortCategory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val LAST_INDEX: Int = 16_383
private const val ALL_DYNAMIC: Int = 16_384
private const val SIZE: Int = 10

class TestPortsTest : FunSpec({
    context("dynamic") {
        test("hands out the ports of the dynamic range") {
            TestPorts.dynamic().toString() shouldBe "49152"
            TestPorts.dynamic(LAST_INDEX).toString() shouldBe "65535"
            TestPorts.dynamic().category shouldBe PortCategory.Dynamic
        }

        test("refuses an index outside the dynamic range") {
            shouldThrow<IllegalArgumentException> { TestPorts.dynamic(-1) }
            shouldThrow<IllegalArgumentException> { TestPorts.dynamic(ALL_DYNAMIC) }
        }
    }

    context("dynamicRange") {
        test("starts at the first dynamic port") {
            TestPorts.dynamicRange().toString() shouldBe "49152"
            TestPorts.dynamicRange(SIZE).toString() shouldBe "49152-49161"
            TestPorts.dynamicRange(ALL_DYNAMIC).size shouldBe ALL_DYNAMIC
        }

        test("refuses an empty or too large range") {
            shouldThrow<IllegalArgumentException> { TestPorts.dynamicRange(0) }
            shouldThrow<IllegalArgumentException> { TestPorts.dynamicRange(ALL_DYNAMIC + 1) }
        }
    }
})
