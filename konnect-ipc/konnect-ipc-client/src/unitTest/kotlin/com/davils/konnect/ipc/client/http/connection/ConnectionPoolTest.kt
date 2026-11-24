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

package com.davils.konnect.ipc.client.http.connection

import com.davils.konnect.testing.ipc.TestIpc
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.ktor.utils.io.availableForRead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private const val PAUSE: Long = 20L

private suspend fun withPool(
    maxIdle: Int = 2,
    maxConnections: Int = 2,
    idleTimeout: Duration = 1.seconds,
    block: suspend (ConnectionPool, Opened, TestTimeSource) -> Unit,
) {
    val scope = CoroutineScope(SupervisorJob())
    val time = TestTimeSource()
    try {
        val pool = ConnectionPool(
            maxIdle = maxIdle,
            maxConnections = maxConnections,
            idleTimeout = idleTimeout,
            timeSource = time,
        )
        block(pool, Opened(scope), time)
    } finally {
        scope.cancel()
    }
}

class ConnectionPoolTest : FunSpec({
    context("a lease") {
        test("opens a connection when none is idle and takes an idle one otherwise") {
            withPool { pool, opened, _ ->
                val first = pool.lease { opened.open() }
                pool.release(first, isReusable = true)

                pool.idleCount shouldBe 1
                pool.lease { opened.open() } shouldBeSameInstanceAs first
                opened.count shouldBe 1
            }
        }

        test("gives its place back when the connection cannot be opened") {
            withPool(maxConnections = 1) { pool, _, _ ->
                shouldThrow<IllegalStateException> { pool.lease { error("refused") } }

                pool.freePermits shouldBe 1
            }
        }

        test("waits while every connection is in use and gives up its place when it is cancelled") {
            withPool(maxConnections = 1) { pool, opened, _ ->
                coroutineScope {
                    val first = pool.lease { opened.open() }
                    val waiting = async { pool.lease { opened.open() } }
                    delay(PAUSE)

                    waiting.isActive shouldBe true
                    waiting.cancelAndJoin()
                    pool.release(first, isReusable = true)
                    pool.freePermits shouldBe 1
                    pool.lease { opened.open() } shouldBeSameInstanceAs first
                    opened.count shouldBe 1
                }
            }
        }
    }

    context("a release") {
        test("counts once, however often it is repeated") {
            withPool(maxConnections = 1) { pool, opened, _ ->
                val connection = pool.lease { opened.open() }

                pool.release(connection, isReusable = false)
                pool.release(connection, isReusable = false)

                pool.freePermits shouldBe 1
                connection.isOpen shouldBe false
            }
        }

        test("closes a connection that may not be used again, that is closed or for which there is no room") {
            withPool(maxIdle = 1) { pool, opened, _ ->
                val refused = pool.lease { opened.open() }
                val closed = pool.lease { opened.open() }
                closed.close()

                pool.release(refused, isReusable = false)
                pool.release(closed, isReusable = true)

                refused.isOpen shouldBe false
                pool.idleCount shouldBe 0
            }
            withPool(maxIdle = 0) { pool, opened, _ ->
                val connection = pool.lease { opened.open() }

                pool.release(connection, isReusable = true)

                connection.isOpen shouldBe false
            }
        }

        test("closes a connection that holds bytes nobody asked for") {
            withPool { pool, _, _ ->
                val scope = CoroutineScope(SupervisorJob())
                val pair = TestIpc.connections()
                val connection = pool.lease { HttpConnection(pair.client, scope) }
                pair.server.write("unasked".encodeToByteString())
                while (connection.input.availableForRead == 0) yield()

                pool.release(connection, isReusable = true)

                pool.idleCount shouldBe 0
                scope.cancel()
            }
        }
    }

    context("an idle connection") {
        test("that waited longer than the idle timeout is closed when a lease looks for one") {
            withPool { pool, opened, time ->
                val stale = pool.lease { opened.open() }
                pool.release(stale, isReusable = true)
                time += 2.seconds

                val fresh = pool.lease { opened.open() }

                stale.isOpen shouldBe false
                (fresh === stale) shouldBe false
                opened.count shouldBe 2
            }
        }

        test("is kept while it waited less than the idle timeout") {
            withPool { pool, opened, time ->
                val kept = pool.lease { opened.open() }
                pool.release(kept, isReusable = true)
                time += 0.5.seconds

                pool.lease { opened.open() } shouldBeSameInstanceAs kept
            }
        }
    }

    context("closing the pool") {
        test("closes the idle connections and every connection released later") {
            withPool { pool, opened, _ ->
                val idle = pool.lease { opened.open() }
                val busy = pool.lease { opened.open() }
                pool.release(idle, isReusable = true)

                pool.closeAll()
                pool.release(busy, isReusable = true)

                idle.isOpen shouldBe false
                busy.isOpen shouldBe false
                pool.idleCount shouldBe 0
            }
        }
    }
})
