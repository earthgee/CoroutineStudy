package coroutineFirst

import log
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import java.util.concurrent.TimeUnit
import kotlin.coroutines.*

fun main() {
    log("before coroutine")
//    asyncCalcMd5("test.zip") {
//        log("in coroutine. Before suspend")
//        val result: String = suspendCoroutine { continuation ->
//            //executor.submit {
//                log("in suspend block")
//                continuation.resume(calcMd5(continuation.context[FilePath]?.path))
//                log("after resume")
//            //}
//        }
//        log("in coroutine. after suspend, result = $result")
//    }

    //封装后
    launch(FilePath("test.zip") + CommonPool) {
        log("in coroutine. Before suspend")
        val result: String = suspendCoroutine { continuation ->
            //executor.submit {
                log("in suspend block")
                continuation.resume(calcMd5(this[FilePath]?.path))
                log("after resume")
            //}
        }
        log("in coroutine. after suspend, result = $result")
    }

    log("after coroutine")
    CommonPool.pool.awaitTermination(10000, TimeUnit.MILLISECONDS)
}

class FilePath(val path: String): AbstractCoroutineContextElement(FilePath) {
    companion object Key: CoroutineContext.Key<FilePath>
}

fun asyncCalcMd5(path: String, block: suspend () -> Unit) {
    val continuation = object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = FilePath(path) + CommonPool

        override fun resumeWith(result: Result<Unit>) {
            if(result.isSuccess) {
                log("resume: ${result.getOrNull()}")
            } else {
                log(result.exceptionOrNull()?.toString())
            }
        }
    }
    block.startCoroutine(continuation)
}

private fun calcMd5(path: String?): String {
    log("calc md5 for $path")
    Thread.sleep(1000)
    path?: return ""
    return System.currentTimeMillis().toString()
}

//切线程测试
private val executor = Executors.newSingleThreadExecutor {
    Thread(it, "scheduler")
}

//切线程测试2
open class Pool(val pool: ForkJoinPool):
    AbstractCoroutineContextElement(ContinuationInterceptor),
    ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        PoolContinuation(pool, continuation.context.fold(continuation) { continuation, element ->
        if (element != this@Pool && element is ContinuationInterceptor)
            element.interceptContinuation(continuation) else continuation })

}

private class PoolContinuation<T>(val pool: ForkJoinPool,
                                  val continuation: Continuation<T>) :
    Continuation<T> by continuation {

    override fun resumeWith(result: Result<T>) {
        if(isPoolThread()) continuation.resumeWith(result)
        else pool.execute { continuation.resumeWith(result) }
    }

    fun isPoolThread(): Boolean =
        (Thread.currentThread() as? ForkJoinWorkerThread)?.pool == pool

}

object CommonPool: Pool(ForkJoinPool.commonPool())

//封装
class StandaloneCoroutine(override val context: CoroutineContext): Continuation<Unit> {
    override fun resumeWith(result: Result<Unit>) {
        if(result.isSuccess) {
            log("resume: ${result.getOrNull()}")
        } else {
            log(result.exceptionOrNull()?.toString())
        }
    }

}

//fun launch(context: CoroutineContext, block: suspend () -> Unit)
//    = block.startCoroutine(StandaloneCoroutine(context))

//带有receiver的launch
fun launch(context: CoroutineContext, block: suspend CoroutineContext.() -> Unit)
    = block.startCoroutine(context, StandaloneCoroutine(context))


