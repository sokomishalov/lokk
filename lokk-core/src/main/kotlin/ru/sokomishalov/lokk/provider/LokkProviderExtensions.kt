/**
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.sokomishalov.lokk.provider

import ru.sokomishalov.lokk.provider.model.LokkChallengerDTO
import ru.sokomishalov.lokk.provider.model.LokkFailure
import ru.sokomishalov.lokk.provider.model.LokkSuccess
import java.net.InetAddress
import java.time.Duration
import java.time.Duration.ZERO
import java.time.ZonedDateTime.now

/**
 * @author sokomishalov
 */

/**
 * @param name        name of lock
 * @param atLeastFor  will hold the lock for this duration, at a minimum
 * @param atMostFor   will hold the lock for this duration, at a maximum
 * @param action      to execute the given [action] under distributed lock.
 * @return            the return value of the action or null if locked
 */
suspend inline fun <reified T> LokkProvider.withLokk(
        name: String = "lock",
        atLeastFor: Duration = ZERO,
        atMostFor: Duration = atLeastFor,
        ifLocked: () -> T? = { null },
        action: () -> T
): T? {
    require(name.isNotBlank()) { "Lock name must be not empty" }
    require(atMostFor >= atLeastFor) { "Invalid lock durations" }

    val now = now()
    val lockAtLeastUntil = now + atLeastFor
    val lockAtMostUntil = now + atMostFor

    val lokkResult = tryLock(LokkChallengerDTO(
            name = name,
            lockBy = nodeName,
            lockUntil = lockAtMostUntil
    ))

    return when (lokkResult) {
        is LokkSuccess -> try {
            action()
        } finally {
            release(lokkResult.lokkDTO.copy(lockedUntil = when {
                lockAtLeastUntil.isAfter(now()) -> lockAtLeastUntil
                else -> now()
            }))
        }
        is LokkFailure -> ifLocked()
    }
}

val LokkProvider.nodeName: String by lazy {
    System.getenv("HOSTNAME").ifBlank { null }
            ?: System.getenv("COMPUTERNAME").ifBlank { null }
            ?: runCatching { InetAddress.getLocalHost().hostAddress }.getOrNull()
            ?: "localhost"
}