package org.naminfo.utils

import android.util.Log
import java.net.SocketTimeoutException
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE

class ImageUploadService {
    companion object {
        private const val TAG = "SoapService"
        var METHOD_NAME = "UploadFileString_1"
        var NAMESPACE = "www.Baytalkitec.com"
        var URL: String = "http://192.168.1.31/HTTP/Mobion.asmx"
        var SOAP_ACTION = "$NAMESPACE/$METHOD_NAME"
    }

    var FileName = "fileName"
    var Content = "filecontent"
    var BOF = "bof"
    var EOF = "eof"
    var subscriberid = "subscriberid"
    var filetype = "filetype"

    var Envelope_class: SoapSerializationEnvelope? = null
    private var request: SoapObject? = null

    init {
        try {
            request = getSoapObject()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addProperty(
        Name: String,
        Value: Any,
        bof: Boolean,
        eof: Boolean,
        subscriberid: String,
        filetype: String
    ) {
        try {
            request?.apply {
                addProperty(FileName, Name)
                addProperty(Content, Value)
                addProperty(BOF, bof)
                addProperty(EOF, eof)
                addProperty(this@ImageUploadService.subscriberid, subscriberid)
                addProperty(this@ImageUploadService.filetype, filetype)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSoapObject(): SoapObject? {
        return try {
            SoapObject(NAMESPACE, METHOD_NAME)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getEnvelope(Soap: SoapObject): SoapSerializationEnvelope? {
        return try {
            val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
            envelope.dotNet = true
            envelope.setOutputSoapObject(Soap)
            envelope
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun callWebService(): SoapObject? {
        try {
            Log.i(TAG, "File sharing IP address : $URL")
            var response: SoapObject? = null
            val envelope = getEnvelope(request!!)
            envelope?.bodyIn = request
            val androidHttpTransport = HttpTransportSE(URL)

            androidHttpTransport.debug = true
            try {
                androidHttpTransport.call(SOAP_ACTION, envelope)
                response = envelope?.response as SoapObject
                val resp = response.getProperty(0).toString()
                // val messageStatus = ReadXmlFile.getXmlValuesFromXml(androidHttpTransport.responseDump, "soap:Body", "status")
                if (resp.contains("success", ignoreCase = true)) {
                    Log.i(TAG, "file download url in server : $resp")
                }

                Log.d(TAG, "AndroidRequest : ${androidHttpTransport.requestDump}")
                Log.d(TAG, "AndroidResponse : ${androidHttpTransport.responseDump}")
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "ksoap time out occurred : ${e.message} response : $response")
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Exception occurred in ksoap2 : ${e.message}")
                Log.e(TAG, "AndroidRequest : ${androidHttpTransport.requestDump}")
                Log.e(TAG, "AndroidResponse : ${androidHttpTransport.responseDump}")
                return null
            }
            return response
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /*private fun sendUrl(e: NgnHistorySMSEvent, contentUrl: String, type: String, localFileUrl: String, remoteNum: String) {
        try {
            Log.i(TAG, "Send url to remote party $remoteNum")
            val modifiedUrl = StringBuilder().apply {
                append(GeneralSettings.SPLIT_URL)
                append(contentUrl)
                append(GeneralSettings.SPLIT_URL)
                append(type)
            }
            Log.i(TAG, "Modified url for send to remote party : $modifiedUrl")
            ScreenMain.com_chat.sendMultimediaMessage(modifiedUrl.toString(), e, remoteNum)
        } catch (e1: Exception) {
            e1.printStackTrace()
        }
    }*/
}
