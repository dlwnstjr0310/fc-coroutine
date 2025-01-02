package com.study.coroutine.repository

import com.study.coroutine.model.Article
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : CoroutineCrudRepository<Article, Long> {

    suspend fun findAllByTitleContains(title: String): Flow<Article>

}