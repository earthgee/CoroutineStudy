import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.coroutines.*

//suspendCoroutine的原理解析
suspend fun getLength(text: String): Int = suspendCoroutine { continuation ->
    thread {
        Thread.sleep(1000L)
        continuation.resume(text.length)
    }
}

//解构continuation
fun deconstructGetLength() {
    val func = ::getLength as (String, Continuation<Int>) -> Any?

    func("earthgee", object: Continuation<Int> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Int>) {
            println(result.getOrNull())
        }

    })
}

//使用原始api创建协程
private fun testStartCoroutine() {
    val continuation = object: Continuation<String> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<String>) {
            println("reuslt = ${result.getOrNull()}")
        }

    }

    ::block2.startCoroutine(continuation)
}

suspend fun block2(): String {
    println("Hello")
    delay(1000L)
    println("World")
    return "Result"
}

fun main() = runBlocking{
//    println(getLength("earthgee"))

    deconstructGetLength()

    Thread.sleep(2000L)

//    testStartCoroutine()
//    Thread.sleep(2000L)

}