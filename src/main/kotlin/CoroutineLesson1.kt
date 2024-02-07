import java.util.Collections.EMPTY_MAP
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.random.Random

fun main() {
    requestSingleImg()
}

fun requestMultImg() {
    val urls = listOf("1", "2", "3")
    val countDownLatch = CountDownLatch(urls.size)
    val map = urls.map { it to "" }
        .toMap(ConcurrentHashMap<String, String>())

    urls.map {  url ->
        asyncBitmap(url, onSuccess = {
            map[url] = it
            countDownLatch.countDown()
        }, onError = {
            countDownLatch.countDown()
        })
    }
    countDownLatch.await()
    map.values.forEach { println(it) }
}

fun requestSingleImg() {
    val onSuccess = { img: String ->
        println("thread:${Thread.currentThread().name} $img")
    }

    val onError = { _: Throwable ->
        //none
    }

    asyncBitmap("hello world", onSuccess, onError)
}

fun asyncBitmap(url: String,
                onSuccess: (String) -> Unit,
                onError: (Throwable) -> Unit) = thread {
     try {
         downloadUrl(url).also { onSuccess(it) }
     } catch (e: Exception) {
         onError(e)
     }
}

private fun downloadUrl(url: String): String {
    val time = Random.Default.nextInt(1, 5)
    println("url:$url downloadTime:$time")
    Thread.sleep(time * 1000L)
    return url
}