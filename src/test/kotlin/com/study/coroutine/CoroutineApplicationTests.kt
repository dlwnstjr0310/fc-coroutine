package com.study.coroutine

import com.study.coroutine.model.Article
import com.study.coroutine.repository.ArticleRepository
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class CoroutineApplicationTests(
    @Autowired private val repository: ArticleRepository
) : StringSpec({

    "context load".config(false) {
        runBlocking {
            val prev = repository.count()
            repository.save(Article(title = "test", body = "test"))
            val curr = repository.count()
            curr shouldBe prev + 1
        }
    }
})
