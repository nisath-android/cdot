package com.naminfo.cdot_vc.utils

import java.util.concurrent.atomic.AtomicBoolean

open class Event<out T>(private val content: T) {
    private val handled = AtomicBoolean(false)

    fun consumed(): Boolean {
        return handled.get()
    }

    fun consume(handleContent: (T) -> Unit) {
        if (!handled.get()) {
            handled.set(true)
            handleContent(content)
        }
    }
}
