import kotlinx.coroutines.delay
import kotlin.coroutines.*

suspend fun main() {
//    continuation.resume(Unit)

//    startCoroutine()
//
//    Thread.sleep(3000)

}

//不带receiver的启动协程
private val continuation = suspend {
    println("thread:${Thread.currentThread().name}")
    1
}.createCoroutine(object: Continuation<Int> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Int>) {
        println("thread:${Thread.currentThread().name}")
        println("resumeWith:$result")
    }

})

//带有receiver的启动协程
fun <R, T> launchCoroutine(receiver: R, block: suspend R.() -> T) {
    block.startCoroutine(receiver, object: Continuation<T> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<T>) {
            println("thread:${Thread.currentThread().name}")
            println("resumeWith:$result")
        }

    })
}

class ProducerScope<T> {
    suspend fun produce(value: T) {
//        delay(1000L)
        println("produce:$value")
    }
}

fun startCoroutine(){
    launchCoroutine(ProducerScope<Int>()) {
        println("thread:${Thread.currentThread().name}")
        produce(1024)
        println("thread:${Thread.currentThread().name}")
        delay(1000L)
        println("thread:${Thread.currentThread().name}")
        produce(2048)
    }
}
