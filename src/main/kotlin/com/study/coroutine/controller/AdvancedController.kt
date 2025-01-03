package com.study.coroutine.controller

import com.study.coroutine.config.validator.DateString
import com.study.coroutine.exception.InvalidParameter
import com.study.coroutine.service.AdvancedService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class AdvancedController(
    private val service: AdvancedService,
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

