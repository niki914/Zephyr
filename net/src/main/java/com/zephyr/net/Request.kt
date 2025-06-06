package com.zephyr.net

import com.google.gson.Gson
import com.zephyr.log.logD
import com.zephyr.log.logE
import com.zephyr.net.bean.NetResult
import com.zephyr.net.bean.NetResult.Error
import com.zephyr.net.bean.NetResult.Success
import com.zephyr.net.bean.StreamNetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import java.io.BufferedReader

const val ServiceBuilderTag = "net request"

private fun <T> Call<T>.getUrl(): String = request().url().toString()

var requestShowJson = false

fun <T> Response<T>.getErrorBodyString() = errorBody()?.string() ?: ""


/**
 * 流式请求 flow
 *
 * 发送四种状态:
 * Start
 * Data: data: T?
 * Error: code: Int?, msg: String
 * Complete: isSuccess: Boolean
 */
inline fun <reified T> Response<ResponseBody>.requestStream(): Flow<StreamNetResult<T>> = flow {
    tryEmit(StreamNetResult.Start)
    var isSuccess = false
    var errorSent = false
    try {
        if (isSuccessful) {
            val reader = body()?.byteStream()?.bufferedReader()
            if (reader != null) {
                isSuccess = handleStreamResponse(reader)
                withContext(Dispatchers.IO) {
                    try {
                        reader.close()
                    } catch (t: Throwable) {
                        t.logE(ServiceBuilderTag)
                    }
                }
            } else {
                logE(ServiceBuilderTag, "byte stream reader is null")
                tryEmit(StreamNetResult.Error(null, "byte stream reader is null"))
                errorSent = true
            }
        } else {
            val errorString = getErrorBodyString()
            logE(ServiceBuilderTag, errorString)
            tryEmit(StreamNetResult.Error(code(), errorString))
            errorSent = true
        }
    } catch (t: Throwable) {
        val tString = t.stackTraceToString()
        logE(ServiceBuilderTag, tString)
        tryEmit(StreamNetResult.Error(null, tString))
        errorSent = true
    }
    runCatching {
        if (!isSuccess && !errorSent)
            tryEmit(StreamNetResult.Error(null, "unknown error"))
        tryEmit(StreamNetResult.Complete(isSuccess))
    }
}

suspend inline fun <reified T> FlowCollector<StreamNetResult<T>>.handleStreamResponse(reader: BufferedReader): Boolean {
    val gson = Gson()
    var isSuccess = false
    while (true) {
        val line =
            withContext(Dispatchers.IO) {
                try {
                    reader.readLine()
                } catch (t: Throwable) {
                    t.logE(ServiceBuilderTag)
                    null
                }
            } ?: break

        when {
            line.isEmpty() || line.startsWith(":") -> continue

            line.startsWith("data: ") -> {
                val data = line.substring(6)
                if (data == "[DONE]") {
                    logE(ServiceBuilderTag, "data is \"[DONE]\"")
                    isSuccess = true
                    break
                }

                try {
                    val streamData = gson.fromJson(data, T::class.java)
                    if (requestShowJson)
                        logE(ServiceBuilderTag, streamData.toPrettyJson())
                    tryEmit(StreamNetResult.Data(streamData))
                } catch (t: Throwable) {
                    t.logE(ServiceBuilderTag)
                    tryEmit(StreamNetResult.Error(null, "gson parse error"))
                }
            }
        }
    }
    return isSuccess
}

suspend inline fun <reified T> FlowCollector<T>.tryEmit(t: T) = try {
    emit(t)
    logE(ServiceBuilderTag, "emit: ${t!!::class.simpleName}")
} catch (t: Throwable) {
    t.logE(ServiceBuilderTag)
}

/**
 * 同步请求方法
 */
@JvmName("requestExecute1")
fun <T> Call<T>.requestExecute(
    callback: (NetResult<T>) -> Unit
) = try {
    val response = execute()
    handleOnResponse(response, callback)
} catch (t: Throwable) {
    handleOnFailure(t, callback)
}

/**
 * 异步请求方法
 */
@JvmName("requestEnqueue1")
fun <T> Call<T>.requestEnqueue(
    callback: (NetResult<T>) -> Unit
) = enqueue(object : Callback<T> {
    override fun onResponse(call: Call<T>, response: Response<T>) {
        handleOnResponse(response, callback)
    }

    override fun onFailure(call: Call<T>, throwable: Throwable) {
        handleOnFailure(throwable, callback)
    }
})

/**
 * 挂起请求方法
 */
@JvmName("requestSuspend1")
suspend fun <T> Call<T>.requestSuspend(
    callback: (NetResult<T>) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val response = awaitResponse()
        handleOnResponse(response, callback)
    } catch (t: Throwable) {
        handleOnFailure(t, callback)
    }
}

@JvmName("requestExecute2")
fun <T> requestExecute(
    call: Call<T>,
    callback: (NetResult<T>) -> Unit
) = call.requestExecute(callback)

@JvmName("requestEnqueue2")
fun <T> requestEnqueue(
    call: Call<T>,
    callback: (NetResult<T>) -> Unit
) = call.requestEnqueue(callback)

@JvmName("requestSuspend2")
suspend fun <T> requestSuspend(
    call: Call<T>,
    callback: (NetResult<T>) -> Unit
) = call.requestSuspend(callback)


private fun <T> Call<T>.handleOnResponse(
    response: Response<T>?,
    callback: (NetResult<T>) -> Unit
) = response?.run {
    val url = getUrl()
    when {
        isSuccessful -> {
            logD(ServiceBuilderTag, "[${code()}] request succeed:\n$url")
            if (requestShowJson)
                logD(ServiceBuilderTag, "body:\n${body().toPrettyJson()}")
            callback(Success(body()))
        } // 成功

        else -> {
            val errorBodyString = getErrorBodyString()
            logE(ServiceBuilderTag, "[${code()}] request failed at:\n$url")
            if (requestShowJson)
                logE(ServiceBuilderTag, "error body:\n${errorBodyString.toPrettyJson()}")
            val errorString = errorBodyString.ifBlank { message() ?: "Unknown error" }
            callback(Error(code(), errorString))
        } // 其他失败情况
    }
} ?: callback(Error(null, "response is null"))

private fun <T> Call<T>.handleOnFailure(
    throwable: Throwable?,
    callback: (NetResult<T>) -> Unit
) {
    if (throwable == null) return
    val url = getUrl()
    val throwableString = throwable.stackTraceToString()
    logE(ServiceBuilderTag, "[] request failed at:\n$url")
    logE(ServiceBuilderTag, "\nthrowable:\n$throwableString")
    callback(Error(null, throwableString))
}