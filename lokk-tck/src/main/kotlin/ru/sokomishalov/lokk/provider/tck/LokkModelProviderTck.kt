/**
 * Copyright (c) 2019-present Mikhael Sokolov
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
@file:Suppress("FunctionName")

package ru.sokomishalov.lokk.provider.tck

/**
 * @author sokomishalov
 */

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.sokomishalov.lokk.provider.LokkProvider
import ru.sokomishalov.lokk.provider.withLokk
import java.time.Duration.ofMinutes
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author sokomishalov
 */
abstract class LokkModelProviderTck {

    protected abstract val lokkProvider: LokkProvider

    @Test
    fun `Lock at least for duration`() {
        val counter = AtomicInteger(0)
        val iterations = 5

        repeat(iterations) {
            runBlocking {
                lokkProvider.withLokk(name = "lockAtLeastFor", atLeastFor = ofMinutes(10)) {
                    counter.incrementAndGet()
                }
            }
        }

        assertEquals(1, counter.get())
    }

    @Test
    fun `Lock at most for duration`() {
        val counter = AtomicInteger(0)
        val iterations = 5

        repeat(iterations) {
            runBlocking {
                lokkProvider.withLokk(name = "lockAtMostFor", atMostFor = ofMinutes(10)) {
                    counter.incrementAndGet()
                }
            }
        }

        assertEquals(iterations, counter.get())
    }

    @Test
    fun `Lock while async operations`() {
        val first = AtomicInteger(0)
        val second = AtomicInteger(0)

        runBlocking {
            listOf(
                    async {
                        delay(50)
                        lokkProvider.withLokk(atMostFor = ofMinutes(1)) {
                            first.incrementAndGet()
                        }
                    },
                    async {
                        lokkProvider.withLokk(atMostFor = ofMinutes(1)) {
                            delay(50)
                            second.incrementAndGet()
                        }
                    }
            ).awaitAll()
        }

        assertEquals(0, first.get())
        assertEquals(1, second.get())
    }
}