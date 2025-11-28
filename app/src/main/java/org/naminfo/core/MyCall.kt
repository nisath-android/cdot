package org.naminfo.core

import org.linphone.core.Call
import org.linphone.core.CallParams

class MyCall(private val original: Call) : Call by original {

    override fun acceptWithParams(params: CallParams?): Int {
        val newParams = params ?: original.core.createCallParams(original)
        newParams?.addCustomHeader("X-Test", "Value")
        // newParams?.addCustomSdpMediaAttribute()
        newParams?.addCustomSdpAttribute("E2E_RCVR_MAT", "D4C3B21A")
        newParams?.addCustomSdpAttribute("E2E_SNDR_MAT", "A1B2C3D4")
        newParams?.customContents

        return original.acceptWithParams(params)
    }
}
