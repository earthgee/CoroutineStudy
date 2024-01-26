import kotlinx.coroutines.*

suspend fun main(args: Array<String>) {
    //simpleCoroutine()

    simpleAsync()

//    traversal()
}

//14
fun simpleCoroutine() {
    //job
    GlobalScope.launch {
        println("coroutine start")
        delay(1000)
    }

    println("main")
    Thread.sleep(2000)
    println("main done")

}

//14
suspend fun simpleAsync() {
    val deferred: Deferred<String> = GlobalScope.async {
        println("In async - ${Thread.currentThread().name}")
        delay(100)
        "hello world"
    }

    Thread.sleep(1000)
    println("In result - ${Thread.currentThread().name}")
    println("result - ${deferred.await()}")
}

//15
//挂起函数
suspend fun getUserInfo(): String {
    withContext(Dispatchers.IO) {
        delay(1000L)
    }
    return "earthgee"
}

suspend fun getFriendList(): String {
    withContext(Dispatchers.IO) {
        delay(2000L)
    }
    return "no,any"
}

suspend fun traversal() {
    println(getUserInfo())
    println(getFriendList())
}





