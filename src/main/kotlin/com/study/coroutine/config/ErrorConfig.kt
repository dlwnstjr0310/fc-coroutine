package com.study.coroutine.config

import com.study.coroutine.config.extension.KEY_TXID
import com.study.coroutine.config.extension.txid
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerRequest

private val logger = KotlinLogging.logger {}

@Configuration
class ErrorConfig {

    @Bean
    fun errorAttribute(): DefaultErrorAttributes {

        return object : DefaultErrorAttributes() {

            override fun getErrorAttributes(
                request: ServerRequest,
                options: ErrorAttributeOptions?
            ): MutableMap<String, Any> {


                val req = request.exchange().request

                val txid = req.txid ?: ""

                MDC.put(KEY_TXID, txid)

                try {
                    logger.debug { "request id : ${request.exchange().request.id}" }
                    super.getError(request).let { e ->
                        logger.error(e.message ?: "Internal Server Error", e)
                    }

                    return super.getErrorAttributes(request, options).apply {
                        remove("requestId")
                        put(KEY_TXID, txid)
                    }
                } finally {
                    req.txid = null
                    MDC.remove(KEY_TXID)
                }
            }

        }

    }

}