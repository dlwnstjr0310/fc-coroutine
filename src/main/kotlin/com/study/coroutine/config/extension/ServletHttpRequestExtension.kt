package com.study.coroutine.config.extension

import org.springframework.http.server.reactive.ServerHttpRequest

const val KEY_TXID = "txid"
const val format = "yyyyMMdd"
private val mapReqIdToTxid = HashMap<String, String>()

var ServerHttpRequest.txid: String?
    get() {
        return mapReqIdToTxid[this.id]
    }
    set(value) {
        if (value == null) {
            mapReqIdToTxid.remove(id)
        } else {
            mapReqIdToTxid[id] = value
        }
    }
