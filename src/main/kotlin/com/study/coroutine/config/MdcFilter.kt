package com.study.coroutine.config

import io.micrometer.context.ContextRegistry
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.*

const val KEY_TXID = "txid"

@Order(1)
@Component
class MdcFilter : WebFilter {

    init {
        Hooks.enableAutomaticContextPropagation()
        ContextRegistry.getInstance().registerThreadLocalAccessor(
            KEY_TXID,
            { MDC.get(KEY_TXID) },
            { value -> MDC.put(KEY_TXID, value) },
            { MDC.remove(KEY_TXID) }
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val txid = exchange.request.headers[KEY_TXID]?.firstOrNull() ?: "${UUID.randomUUID()}"

        MDC.put(KEY_TXID, txid)

        return chain.filter(exchange).contextWrite {
            Context.of(KEY_TXID, txid)
        }
    }
}