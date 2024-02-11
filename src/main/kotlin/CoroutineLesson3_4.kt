import kotlin.coroutines.*

class LogInterceptor: ContinuationInterceptor {

    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor

    override fun <T> interceptContinuation(continuation: Continuation<T>) =
        LogContinuation(continuation)

}

class LogContinuation<T>(private val continuation: Continuation<T>)
    : Continuation<T> by continuation {

    override fun resumeWith(result: Result<T>) {
        println("before resumeWith:${result}")
        continuation.resumeWith(result)
        println("after resumeWith")
    }

}

private fun startInterceptorCoroutine() {
    val coroutineContext = LogInterceptor()

    suspend {
        1
    }.startCoroutine(object: Continuation<Int>{
        override val context: CoroutineContext
            get() = coroutineContext

        override fun resumeWith(result: Result<Int>) {
            println("resumeWith:${result}")
        }

    })
}

fun main() {
    startInterceptorCoroutine()
}