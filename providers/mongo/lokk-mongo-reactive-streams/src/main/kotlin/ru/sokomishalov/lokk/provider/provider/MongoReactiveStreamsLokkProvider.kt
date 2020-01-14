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
package ru.sokomishalov.lokk.provider.provider

import com.mongodb.ConnectionString
import com.mongodb.ErrorCategory.DUPLICATE_KEY
import com.mongodb.ErrorCategory.fromErrorCode
import com.mongodb.MongoServerException
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document
import ru.sokomishalov.lokk.provider.LokkInfo
import ru.sokomishalov.lokk.provider.LokkProvider
import java.util.*

/**
 * @author sokomishalov
 */

class MongoReactiveStreamsLokkProvider(
        private val host: String = "localhost",
        private val port: Int = 27017,
        private val databaseName: String = "lokkDB",
        private val collectionName: String = "lokk",
        private val client: MongoClient = MongoClients.create(ConnectionString("mongodb://${host}:${port}/${databaseName}"))
) : LokkProvider {

    companion object {
        private const val ID_FIELD = "_id"
        private const val LOCKED_UNTIL_FIELD = "lockedUntil"
        private const val LOCKED_AT_FIELD = "lockedAt"
        private const val LOCKED_BY_FIELD = "lockedBy"
    }

    /**
     * There are three possible situations:
     * 1. The lock document does not exist yet - it is inserted - we have the lock
     * 2. The lock document exists and lockUntil before now - it is updated - we have the lock
     * 3. The lock document exists and lockUntil after now - Duplicate key exception is thrown
     */
    override suspend fun tryLock(lokkInfo: LokkInfo): Boolean {
        return try {
            getCollection()
                    .findOneAndUpdate(
                            and(eq(ID_FIELD, lokkInfo.name), lte(LOCKED_UNTIL_FIELD, Date())),
                            combine(
                                    set(LOCKED_UNTIL_FIELD, Date.from(lokkInfo.lockedUntil.toInstant())),
                                    set(LOCKED_AT_FIELD, Date.from(lokkInfo.lockedAt.toInstant())),
                                    set(LOCKED_BY_FIELD, lokkInfo.lockedBy)
                            ),
                            FindOneAndUpdateOptions().upsert(true)
                    )
                    .awaitFirstOrNull()
            true
        } catch (e: MongoServerException) {
            when (fromErrorCode(e.code)) {
                DUPLICATE_KEY -> false
                else -> throw e
            }
        }
    }

    override suspend fun release(lokkInfo: LokkInfo) {
        getCollection()
                .findOneAndUpdate(
                        eq(ID_FIELD, lokkInfo.name),
                        combine(set(LOCKED_UNTIL_FIELD, Date.from(lokkInfo.lockedUntil.toInstant())))
                )
                .awaitFirstOrNull()
    }

    private fun getCollection(): MongoCollection<Document> {
        return client
                .getDatabase(databaseName)
                .getCollection(collectionName)
    }
}