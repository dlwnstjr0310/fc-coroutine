package com.study.coroutine.controller

import com.study.coroutine.model.Article
import com.study.coroutine.service.ArticleService
import com.study.coroutine.service.QryArticle
import com.study.coroutine.service.ReqCreate
import com.study.coroutine.service.ReqUpdate
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/article")
class ArticleController(
    private val service: ArticleService,
) {

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody request: ReqCreate): Article {
        return service.create(request)
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: Long): Article {
        return service.get(id)
    }

//    @GetMapping("/all")
//    suspend fun getAll(@RequestParam title: String?): Flow<Article> {
//        return service.getAll(title)
//    }

    // /article/all?title=foo&authorId=1&from=20210101
    @GetMapping("/all")
    suspend fun getAll(request: QryArticle): Flow<Article> {
//        return service.getAll(request)
        return service.getAllCached(request)
    }

    @PutMapping("/{id}")
    suspend fun update(@PathVariable id: Long, @RequestBody request: ReqUpdate): Article {
        return service.update(id, request)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: Long) {
        service.delete(id)
    }

}