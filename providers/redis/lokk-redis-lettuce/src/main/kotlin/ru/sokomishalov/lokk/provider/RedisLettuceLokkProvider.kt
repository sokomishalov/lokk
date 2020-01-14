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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.time.Duration
import java.time.ZonedDateTime.now

class RedisLettuceLokkProvider(
        private val host: String = "localhost",
        private val port: Int = 6379,
        private val client: RedisClient = RedisClient.create(RedisURI.create(host, port)),
        private val connection: StatefulRedisConnection<String, String> = client.connect()
) : LokkProvider {

    companion object {
        const val KEY_PREFIX: String = "lokk:"

        // fixme remove
        private val OBJECT_MAPPER = ObjectMapper().apply {
            registerModule(KotlinModule())
            registerModule(JavaTimeModule())
        }
    }

    override suspend fun tryLock(lokkInfo: LokkInfo): Boolean {
        val expireAfterMs = Duration.between(now(), lokkInfo.lockedUntil).toMillis()

        val lockAcquired = connection
                .reactive()
                .set(lokkInfo.redisKey, lokkInfo.redisValue, SetArgs().nx().px(expireAfterMs))
                .awaitFirstOrNull()

        return when {
            lockAcquired.isNullOrBlank().not() -> true
            else -> {
                val value = connection.reactive().get(lokkInfo.redisKey).awaitFirstOrNull()
                value?.deserializeValue()?.lockedUntil?.isBefore(now()) ?: true
            }
        }
    }

    override suspend fun release(lokkInfo: LokkInfo) {
        connection
                .reactive()
                .set(lokkInfo.redisKey, lokkInfo.redisValue, SetArgs().xx())
                .awaitFirstOrNull()
    }


    private val LokkInfo.redisKey: String
        get() = "$KEY_PREFIX$name"

    private val LokkInfo.redisValue: String
        get() = OBJECT_MAPPER.writeValueAsString(this)

    private fun String.deserializeValue(): LokkInfo {
        return OBJECT_MAPPER.readValue(this)
    }
}