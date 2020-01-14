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

import org.junit.AfterClass
import org.junit.ClassRule
import ru.sokomishalov.lokk.provider.tck.LokkModelProviderTck

/**
 * @author sokomishalov
 */
class RedisLokkModelProviderTest : LokkModelProviderTck() {

    companion object {
        @get:ClassRule
        val redis: RedisTestContainer = createDefaultRedisContainer()

        @AfterClass
        @JvmStatic
        fun stop() = redis.stop()
    }

    override val lokkProvider: LokkProvider by lazy {
        redis.start()
        RedisLettuceLokkProvider(client = redis.createRedisClient())
    }
}