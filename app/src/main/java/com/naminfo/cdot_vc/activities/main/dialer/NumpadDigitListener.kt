package com.naminfo.cdot_vc.activities.main.dialer

interface NumpadDigitListener {
    fun handleClick(key: Char)
    fun handleLongClick(key: Char): Boolean
}