package com.study.coroutine.controller

import com.study.coroutine.model.Article
import com.study.coroutine.repository.ArticleRepository
import com.study.coroutine.service.ArticleService
import com.study.coroutine.service.ReqCreate
import com.study.coroutine.service.ReqUpdate
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.temporal.ChronoUnit

@SpringBootTest
@ActiveProfiles("test")
class ArticleControllerTest(
    @Autowired private val service: ArticleService,
    @Autowired private val repository: ArticleRepository,
    @Autowired private val context: ApplicationContext
) : StringSpec({

    val client = WebTestClient.bindToApplicationContext(context).build()

    fun getSize(title: String? = null): Int {
        return client.get().uri("/article/all${title?.let { "?title=$it" } ?: ""}").accept(APPLICATION_JSON)
            .exchange()
            .expectBodyList(Article::class.java)
            .returnResult().responseBody?.size ?: 0
    }

    beforeTest {
        repository.deleteAll()
    }

    "create" {
        val request = ReqCreate("title test", "r2dbc coroutine sample", 1234)

        client.post().uri("/article").accept(APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("title").isEqualTo(request.title)
            .jsonPath("body").isEqualTo(request.body ?: "")
            .jsonPath("authorId").isEqualTo(request.authorId ?: 0)
    }

    "get" {

        val created = client.post().uri("/article").accept(APPLICATION_JSON)
            .bodyValue(ReqCreate("title test", "r2dbc coroutine sample", 1234))
            .exchange()
            .expectBody(Article::class.java)
            .returnResult().responseBody!!

        val read = client.get().uri("/article/${created.id}").accept(APPLICATION_JSON)
            .exchange()
            .expectBody(Article::class.java)
            .returnResult().responseBody!!

        read.id shouldBe created.id
        read.title shouldBe created.title
        read.body shouldBe created.body
        read.authorId shouldBe created.authorId
        read.createdAt?.truncatedTo(ChronoUnit.SECONDS) shouldBe created.createdAt?.truncatedTo(ChronoUnit.SECONDS)
        read.updatedAt?.truncatedTo(ChronoUnit.SECONDS) shouldBe created.updatedAt?.truncatedTo(ChronoUnit.SECONDS)
    }

    "get all" {
        service.create(ReqCreate("title1"))
        service.create(ReqCreate("title2"))
        service.create(ReqCreate("title matched"))

        getSize() shouldBe 3
        getSize("matched") shouldBe 1
    }

    "update" {
        val created = service.create(ReqCreate("title 1"))

        client.put().uri("/article/${created.id}").accept(APPLICATION_JSON).bodyValue(
            ReqUpdate(
                title = "updated title"
            )
        ).exchange()

        val read = client.get().uri("/article/${created.id}").accept(APPLICATION_JSON)
            .exchange()
            .expectBody(Article::class.java)
            .returnResult().responseBody!!

        read.title shouldBe "updated title"
    }

    "delete" {
        val created = service.create(ReqCreate("title 1"))

        val prevCount = repository.count()

        client.delete().uri("/article/${created.id}").accept(APPLICATION_JSON).exchange()

        repository.count() shouldBe prevCount - 1
        repository.existsById(created.id) shouldBe false
    }

})
