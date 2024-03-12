import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

suspend fun main() {
    log(1)
    val job = CoroutineScope(CoroutineName("Hello")
            + MyNewContinuationInterceptor1()
            + MyNewContinuationInterceptor2()).launch {
        log(2)
        delay(1000L)
        log(4)
        log(coroutineContext[ContinuationInterceptor])
    }

    log(3)
    job.join()
    log(5)
}

private class MyNewContinuationInterceptor1: ContinuationInterceptor {

    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return object: Continuation<T> {
            override val context: CoroutineContext
                get() = continuation.context

            override fun resumeWith(result: Result<T>) {
                log("<interceptor1> $result")
                continuation.resumeWith(result)
            }

        }
    }


}

private class MyNewContinuationInterceptor2: ContinuationInterceptor {

    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return object: Continuation<T> {
            override val context: CoroutineContext
                get() = continuation.context

            override fun resumeWith(result: Result<T>) {
                log("<interceptor2> $result")
                continuation.resumeWith(result)
            }

        }
    }


}