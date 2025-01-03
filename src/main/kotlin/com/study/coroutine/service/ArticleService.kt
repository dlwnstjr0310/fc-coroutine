package com.study.coroutine.service

import com.study.coroutine.config.extension.format
import com.study.coroutine.config.extension.toLocalDate
import com.study.coroutine.config.validator.DateString
import com.study.coroutine.exception.NoArticleFound
import com.study.coroutine.model.Article
import com.study.coroutine.repository.ArticleRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ArticleService(
    private val repository: ArticleRepository,
    private val dbClient: DatabaseClient,
) {

    suspend fun create(request: ReqCreate): Article {
        return repository.save(request.toArticle())
    }

    suspend fun get(id: Long): Article {
        return repository.findById(id) ?: throw NoArticleFound("id: $id")
    }

    suspend fun getAll(title: String? = null): Flow<Article> {
        return if (title.isNullOrBlank()) {
            repository.findAll()
        } else {
            repository.findAllByTitleContains(title)
        }
    }

    suspend fun getAllCached(request: QryArticle): Flow<Article> {

        val params = HashMap<String, Any>()

        var sql = dbClient.sql(
            """
            SELECT id, title, body, author_id, created_at, updated_at
            FROM   TB_ARTICLE
            WHERE  1=1
            ${
                request.title.query {
                    params["title"] = it.trim().let { "%$it%" }
                    "AND title LIKE :title"
                }
            }
            ${
                request.authorId.query {
                    params["authorId"] = it
                    "AND author_id IN (:authorId)"
                }
            }
            ${
                request.from.query {
                    params["from"] = it.toLocalDate(format)
                    "AND created_at >= :from"
                }
            }
            ${
                request.to.query {
                    params["to"] = it.toLocalDate(format).plusDays(1)
                    "AND created_at < :to"
                }
            }
        """.trimIndent())

        params.forEach { (key, value) -> sql = sql.bind(key, value) }

        return sql.map { row ->
            Article(
                id = row["id"] as Long,
                title = row["title"] as String,
                body = row["body"] as String?,
                authorId = row["author_id"] as Long,
            ).apply {
                createdAt = row["created_at"] as LocalDateTime?
                updatedAt = row["updated_at"] as LocalDateTime?
            }
        }.flow()
    }

    suspend fun update(id: Long, request: ReqUpdate): Article {
        val article = repository.findById(id) ?: throw NoArticleFound("id: $id")

        return repository.save(article.apply {
            request.title?.let { title = it }
            request.body?.let { body = it }
            request.authorId?.let { authorId = it }
        })
    }

    suspend fun delete(id: Long) {
        return repository.deleteById(id)
    }

}

fun <T> T?.query(f: (T) -> String): String {
    return when {
        this == null -> ""
        this is String && this.isBlank() -> ""
        this is Collection<*> && this.isEmpty() -> ""
        this is Array<*> && this.isEmpty() -> ""
        else -> f.invoke(this)
    }
}

data class ReqUpdate(
    val title: String? = null,
    val body: String? = null,
    val authorId: Long? = null,
)

data class ReqCreate(
    val title: String,
    val body: String? = null,
    val authorId: Long? = null,
) {
    fun toArticle(): Article {
        return Article(
            title = this.title,
            body = this.body,
            authorId = this.authorId
        )
    }
}

data class QryArticle(
    val title: String?,
    val authorId: List<Long>?,
    @DateString
    val from: String?,
    @DateString
    val to: String?,
)