import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.random.Random

fun main() {
    val urls = listOf("1", "2", "3")
    urls.map {
        requestFuture(it)
    }.map {
        println("Thread:${Thread.currentThread().name} ${it.get()}")
    }
}

private val ioExecutor = Executors.newCachedThreadPool()

private fun requestFuture(url: String): Future<String> =
    ioExecutor.submit(Callable {
        downloadUrl(url)
    })
private fun downloadUrl(url: String): String {
    val time = Random.Default.nextInt(1, 5)
    println("url:$url downloadTime:$time")
    Thread.sleep(time * 1000L)
    return url
}