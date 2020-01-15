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

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import ru.sokomishalov.lokk.provider.model.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

class RedisLettuceLokkProvider(
        private val host: String = "localhost",
        private val port: Int = 6379,
        private val client: RedisClient = RedisClient.create(RedisURI.create(host, port)),
        private val connection: StatefulRedisConnection<String, String> = client.connect()
) : LokkProvider {

    companion object {
        const val DELIMITER: String = ":"
        const val KEY_PREFIX: String = "lokk"
    }

    override suspend fun tryLock(lokkChallenger: LokkChallengerDTO): LokkResult<LokkDTO> {
        val lockedAt = now()

        val lockAcquired = connection
                .reactive()
                .set(
                        lokkChallenger.redisKey,
                        lokkChallenger.serializeValue(lockedAt),
                        SetArgs().nx().px(Duration.between(lockedAt, lokkChallenger.lockUntil).toMillis())
                )
                .awaitFirstOrNull()

        return when {
            lockAcquired.isNullOrBlank().not() -> lokkChallenger.success(lockedAt = lockedAt)
            else -> {
                val result = connection
                        .reactive()
                        .get(lokkChallenger.redisKey)
                        .awaitFirstOrNull()
                        ?.deserializeValue(lokkChallenger.name)
                        ?.lockedUntil
                        ?.isBefore(now())
                        ?: true

                when {
                    result -> lokkChallenger.success(lockedAt = lockedAt)
                    else -> lokkChallenger.failure()
                }
            }
        }
    }

    override suspend fun release(lokkDTO: LokkDTO) {
        connection
                .reactive()
                .set(lokkDTO.redisKey, lokkDTO.serializeValue(), SetArgs().xx())
                .awaitFirstOrNull()
    }


    private val LokkChallengerDTO.redisKey: String get() = "${KEY_PREFIX}${DELIMITER}${name}"
    private val LokkDTO.redisKey: String get() = "${KEY_PREFIX}${DELIMITER}${name}"

    private fun LokkChallengerDTO.serializeValue(lockedAt: ZonedDateTime = now()): String = "${lockBy}${DELIMITER}${lockedAt.toEpochSecond()}${DELIMITER}${lockUntil.toEpochSecond()}"
    private fun LokkDTO.serializeValue(): String = "${lockedBy}${DELIMITER}${lockedAt.toEpochSecond()}${DELIMITER}${lockedUntil.toEpochSecond()}"

    private fun String.deserializeValue(name: String): LokkDTO {
        return split(DELIMITER).let {
            LokkDTO(
                    name = name,
                    lockedBy = it[0],
                    lockedAt = it[1].toLong().zdt,
                    lockedUntil = it[2].toLong().zdt
            )
        }
    }

    private val Long.zdt: ZonedDateTime get() = ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneId.systemDefault())
}