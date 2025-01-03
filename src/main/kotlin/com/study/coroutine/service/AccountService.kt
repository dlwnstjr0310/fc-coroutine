package com.study.coroutine.service

import com.study.coroutine.config.Locker
import com.study.coroutine.model.Article
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.study.coroutine.exception.NoArticleFound as NoAccountFound
import com.study.coroutine.repository.ArticleRepository as AccountRepository

private val logger = KotlinLogging.logger { }

@Service
class AccountService(
    private val repository: AccountRepository,
    private val locker: Locker
) {

    suspend fun get(id: Long): ResAccount {
        return repository.findById(id)?.toResAccount() ?: throw NoAccountFound("id: $id")
    }

    @Transactional
    suspend fun deposit(id: Long, amount: Long) {

        val key = SimpleKey(AccountService::deposit.name, id)

        locker.lock(key) {
            repository.findArticleById(id)?.let { account ->
                delay(3000)
                account.balance += amount
                repository.save(account)
            } ?: throw NoAccountFound("id : $id")
        }
    }

}

data class ResAccount(
    var id: Long,
    val balance: Long,
)

fun Article.toResAccount(): ResAccount {
    return ResAccount(
        id = this.id,
        balance = this.balance,
    )
}