@file:Suppress("unused")

package ru.sokomishalov.lokk.provider.model

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

/**
 * @author sokomishalov
 */
fun LokkChallengerDTO.success(lockedAt: ZonedDateTime = now()): LokkResult<LokkDTO> = LokkSuccess(LokkDTO(name = name, lockedBy = lockBy, lockedAt = lockedAt, lockedUntil = lockUntil))

fun LokkChallengerDTO.failure(exception: Throwable = LokkException): LokkResult<LokkDTO> = LokkFailure(exception)