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

package com.davils.konnect.ipc.core.telemetry

import com.davils.kollect.core.trace.Span
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.kore.core.annotation.DavilsInternal
import kotlin.coroutines.cancellation.CancellationException

@PublishedApi
internal const val CANCELLED_ERROR_TYPE: String = "cancelled"

@PublishedApi
internal const val FAILED_ERROR_TYPE: String = "failed"

private const val ERROR_TYPE_ATTRIBUTE: String = "error.type"

/**
 * Runs [block] and ends this span with how it ended.
 *
 * A block that returns ends the span with [SpanOutcome.Ok]. A block that throws ends it with [SpanOutcome.Error] and
 * the attribute `error.type`: `cancelled` for a [CancellationException] and `failed` for anything else. The
 * exception itself, whose message may quote input, is never put on the span, and it reaches the caller unchanged.
 * The function is inline, so [block] may suspend when it is called from a coroutine.
 *
 * @param T the type of the result.
 * @receiver the span to end.
 * @param block the work of the span.
 * @return what [block] returned.
 * @since 1.0.0
 */
@DavilsInternal
public inline fun <T> Span.traced(block: () -> T): T {
    var errorType: String? = FAILED_ERROR_TYPE
    try {
        return block().also { errorType = null }
    } catch (cancellation: CancellationException) {
        errorType = CANCELLED_ERROR_TYPE
        throw cancellation
    } finally {
        endTraced(errorType)
    }
}

@PublishedApi
internal fun Span.endTraced(errorType: String?) {
    if (errorType == null) {
        end(SpanOutcome.Ok)
        return
    }
    attribute(key = ERROR_TYPE_ATTRIBUTE, value = errorType)
    end(SpanOutcome.Error)
}
