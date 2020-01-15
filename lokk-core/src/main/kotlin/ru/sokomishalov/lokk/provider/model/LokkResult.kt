package ru.sokomishalov.lokk.provider.model

/**
 * @author sokomishalov
 */
sealed class LokkResult<T>

data class LokkSuccess(
        val lokkDTO: LokkDTO
) : LokkResult<LokkDTO>()

data class LokkFailure(
        val throwable: Throwable? = null,
        val reason: String? = throwable?.message
) : LokkResult<LokkDTO>()