package com.stockvaluationservice.stockvaluationservice.requestlogging

import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream

class CachedBodyServletInputStream(cachedBody: ByteArray?) : ServletInputStream() {

    private val log = LoggerFactory.getLogger(CachedBodyServletInputStream::class.java)
    private val cachedBodyInputStream: InputStream

    init {
        cachedBodyInputStream = ByteArrayInputStream(cachedBody)
    }

    override fun isFinished(): Boolean {
        try {
            return cachedBodyInputStream.available() == 0
        } catch (e: IOException) {
            log.error("Error", e)
        }
        return false
    }

    override fun isReady(): Boolean {
        return true
    }

    override fun setReadListener(readListener: ReadListener) {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return cachedBodyInputStream.read()
    }
}