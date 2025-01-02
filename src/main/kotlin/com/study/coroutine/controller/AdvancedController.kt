package com.study.coroutine.controller

import com.study.coroutine.service.AdvancedService
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class AdvancedController(
    private val service: AdvancedService,
) {

    @GetMapping("/test/mdc")
    suspend fun testRequestTxid() {
        logger.debug { "Hello MDC Txid" }
        delay(100)
        service.mdc1()
        logger.debug { "Bye MDC Txid" }
    }

}
