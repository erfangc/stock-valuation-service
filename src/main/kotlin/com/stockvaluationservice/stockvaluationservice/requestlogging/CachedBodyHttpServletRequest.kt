package com.stockvaluationservice.stockvaluationservice.requestlogging

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

class CachedBodyHttpServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    private val cachedBody: ByteArray?

    init {
        this.cachedBody = request.inputStream.readAllBytes()
    }

    @Throws(IOException::class)
    override fun getInputStream(): ServletInputStream {
        return CachedBodyServletInputStream(cachedBody)
    }

    @Throws(IOException::class)
    override fun getReader(): BufferedReader {
        // Create a reader from cachedContent
        // and return it
        val byteArrayInputStream = ByteArrayInputStream(cachedBody)
        return byteArrayInputStream.bufferedReader()
    }

}