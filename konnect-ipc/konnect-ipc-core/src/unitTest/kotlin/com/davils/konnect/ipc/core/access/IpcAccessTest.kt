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

@file:OptIn(DavilsInternal::class)

package com.davils.konnect.ipc.core.access

import com.davils.konnect.ipc.core.IpcViolationCodes
import com.davils.konnect.ipc.core.fixture.ALICE
import com.davils.konnect.ipc.core.fixture.ALICE_PROCESS
import com.davils.konnect.ipc.core.fixture.MALLORY
import com.davils.konnect.ipc.core.fixture.alicePeer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val LOCAL_SYSTEM: String = "S-1-5-18"

private fun codesOf(block: IpcAccessBuilder.() -> Unit): List<ViolationCode> {
    val builder = IpcAccessBuilder()
    builder.block()
    return builder.validate().violations.map { violation -> violation.code }
}

class IpcAccessTest : FunSpec({
    context("users") {
        test("on windows are security identifiers") {
            IpcUser.WindowsSid.LOCAL_SYSTEM.toString() shouldBe LOCAL_SYSTEM
            IpcUser.WindowsSid(LOCAL_SYSTEM) shouldBe IpcUser.WindowsSid.LOCAL_SYSTEM
            IpcUser.WindowsSid(LOCAL_SYSTEM).hashCode() shouldBe LOCAL_SYSTEM.hashCode()
            IpcUser.WindowsSid.LOCAL_SYSTEM.equals(LOCAL_SYSTEM) shouldBe false
            shouldThrow<IllegalArgumentException> { IpcUser.WindowsSid("alice") }
        }

        test("on unix are user names") {
            IpcUser.UnixUser.ROOT.toString() shouldBe "root"
            IpcUser.UnixUser("root") shouldBe IpcUser.UnixUser.ROOT
            IpcUser.UnixUser("root").hashCode() shouldBe "root".hashCode()
            IpcUser.UnixUser.ROOT.equals("root") shouldBe false
            listOf(" ", "", "a\nb").forEach { name ->
                shouldThrow<IllegalArgumentException> { IpcUser.UnixUser(name) }
            }
        }
    }

    context("the default access") {
        test("lets in the current user and nobody else") {
            IpcAccess.CURRENT_USER.permits(ALICE, currentUser = ALICE) shouldBe true
            IpcAccess.CURRENT_USER.permits(MALLORY, currentUser = ALICE) shouldBe false
            IpcAccess.CURRENT_USER.isCurrentUserOnly shouldBe true
            ipcAccess {} shouldBe IpcAccess.CURRENT_USER
        }

        test("lets in nobody whose identity is unknown") {
            IpcAccess.CURRENT_USER.permits(user = null, currentUser = null) shouldBe false
        }
    }

    context("an access with named users") {
        test("lets in every named user once") {
            val access = ipcAccess {
                isCurrentUserAllowed = false
                allow(MALLORY)
                allow(MALLORY)
            }

            access.permits(MALLORY, currentUser = ALICE) shouldBe true
            access.permits(ALICE, currentUser = ALICE) shouldBe false
            access.users shouldBe listOf(MALLORY)
            access.isCurrentUserOnly shouldBe false
        }

        test("is reported when it lets in nobody") {
            codesOf { isCurrentUserAllowed = false } shouldBe listOf(IpcViolationCodes.NOBODY_ALLOWED)
            shouldThrow<ValidationException> { ipcAccess { isCurrentUserAllowed = false } }
        }

        test("cannot be changed once it was built") {
            val builder = IpcAccessBuilder()
            builder.build()

            shouldThrow<IllegalStateException> { builder.allow(MALLORY) }
            shouldThrow<IllegalStateException> { builder.isCurrentUserAllowed = false }
        }
    }

    context("equality") {
        test("compares who is let in") {
            val first = ipcAccess { allow(MALLORY) }
            val second = ipcAccess { allow(MALLORY) }

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe IpcAccess.CURRENT_USER
            first.equals(MALLORY) shouldBe false
            first shouldNotBe ipcAccess {
                isCurrentUserAllowed = false
                allow(MALLORY)
            }
            first.toString() shouldBe "IpcAccess(isCurrentUserAllowed=true, users=[mallory])"
        }
    }

    context("peers") {
        test("of unknown identity name nobody") {
            IpcPeer.UNKNOWN shouldBe IpcPeer(user = null, processId = null)
            IpcPeer.UNKNOWN.toString() shouldBe "IpcPeer(user=unknown, processId=unknown)"
            IpcPeer.UNKNOWN.hashCode() shouldBe IpcPeer(user = null, processId = null).hashCode()
        }

        test("are equal when their user and process are") {
            alicePeer shouldBe IpcPeer(ALICE, ALICE_PROCESS)
            alicePeer.hashCode() shouldBe IpcPeer(ALICE, ALICE_PROCESS).hashCode()
            alicePeer shouldNotBe IpcPeer(ALICE, processId = null)
            alicePeer shouldNotBe IpcPeer(MALLORY, ALICE_PROCESS)
            alicePeer.equals(ALICE) shouldBe false
            alicePeer.toString() shouldBe "IpcPeer(user=alice, processId=4711)"
        }
    }
})
