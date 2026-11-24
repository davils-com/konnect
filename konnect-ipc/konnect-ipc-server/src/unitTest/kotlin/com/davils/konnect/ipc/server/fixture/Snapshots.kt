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

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.SumPoint

internal fun MeterRegistry.sumOf(name: String, vararg labels: Pair<String, String>): Double {
    val snapshot = snapshot()
    val family = checkNotNull(snapshot.family(MetricName.of(name))) { "$name was not declared" }
    val point = family.points.single { point -> labels.all { (key, value) -> point.labels[LabelKey.of(key)] == value } }
    return checkNotNull(point as? SumPoint) { "$name is no sum" }.value
}
