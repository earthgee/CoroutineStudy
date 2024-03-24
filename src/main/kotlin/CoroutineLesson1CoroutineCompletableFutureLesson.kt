import java.util.concurrent.CompletableFuture
import kotlin.random.Random

fun main() {
    val urls = listOf("1", "2", "3")
    urls.map {
        requestCompletableFuture(it)
    }.let {  futureList ->
        CompletableFuture.allOf(*futureList.toTypedArray())
            .thenApply {
                futureList.map {
                    it.get()
                }
            }
    }.thenAccept { list ->
        println("Thread:${Thread.currentThread().name} ${list.toString()}")
    }.join()
}

private fun requestCompletableFuture(url: String): CompletableFuture<String> {
    return CompletableFuture.supplyAsync {
        downloadUrl(url)
    }
}

private fun downloadUrl(url: String): String {
    val time = Random.Default.nextInt(1, 5)
    println("url:$url downloadTime:$time")
    Thread.sleep(time * 1000L)
    return url
}