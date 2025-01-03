package com.study.coroutine.controller

import com.study.coroutine.config.validator.DateString
import com.study.coroutine.exception.InvalidParameter
import com.study.coroutine.service.AccountService
import com.study.coroutine.service.AdvancedService
import com.study.coroutine.service.ExternalApi
import com.study.coroutine.service.ResAccount
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class AdvancedController(
    private val service: AdvancedService,
    private val externalApi: ExternalApi,
    private val accountService: AccountService,
) {

    @GetMapping("/test/mdc")
    suspend fun mdc() {
        logger.debug { "Hello MDC Txid" }
        delay(100)
        service.mdc1()
        logger.debug { "Bye MDC Txid" }
    }

    @PutMapping("/test/error")
    suspend fun error(@RequestBody @Valid request: ReqErrorTest) {
        logger.debug { "request : $request" }

        if (request.message == "error") {
            throw InvalidParameter(request, request::message, "code", "custom")
//            throw RuntimeException("asdf")
        }
    }

    @GetMapping("/external/delay")
    suspend fun delay() {
        externalApi.delay()
    }

    @GetMapping("/external/circuit/{flag}", "/external/circuit", "/external/circuit/")
    suspend fun testCircuitBreaker(@PathVariable flag: String): String {
        return externalApi.testCircuitBreaker(flag)
    }

    @GetMapping("/account/{id}")
    suspend fun getAccount(@PathVariable id: Long): ResAccount {
        return accountService.get(id)
    }

    @PutMapping("/account/{id}/{amount}")
    suspend fun deposit(@PathVariable id: Long, @PathVariable amount: Long): ResAccount {
        accountService.deposit(id, amount)
        return accountService.get(id)
    }

}

data class ReqErrorTest(
    @field:NotEmpty
    @field:Size(min = 3, max = 10)
    val id: String?,

    @field:Positive
    @field:NotNull
    @field:Max(100)
    val age: Int?,

    @field:DateString
    val birthday: String?,

    val message: String? = null,
)

