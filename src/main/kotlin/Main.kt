import kotlinx.coroutines.*
import java.lang.Exception

suspend fun main(args: Array<String>) {
    //simpleCoroutine()

//    simpleAsync()

//    traversal()

//    coroutinePC()

//    asyncMulti()

    testCoroutineContext()
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

//16 父子协程
//join 等待所有子协程执行完毕
//cancel 会取消所有子协程
fun coroutinePC() = runBlocking {
    val parentJob: Job
    var job1: Job? = null
    var job2: Job? = null
    var job3: Job? = null

    parentJob = launch {
        job1 = launch {
            println("1 start")
            delay(100L)
            println("1 end")
        }
        job2 = launch {
            println("2 start")
            delay(2000L)
            println("2 end")
        }
        job3 = launch {
            println("3 start")
            delay(5000L)
            println("3 end")
        }
    }

    delay(50L) // 确保所有子 Job 已正常启动，且尚未结束(否则下面的遍历会错误)
    parentJob.children.forEachIndexed { index, job ->
        when(index) {
            0 -> println("job is job1: ${job1 === job}")
            1 -> println("job is job2: ${job2 === job}")
            2 -> println("job is job3: ${job3 === job}")
        }
    }

    parentJob.join()
    println("Process end!")
}

//16 async优化
//多任务并发
fun asyncMulti() = runBlocking {

    suspend fun getResult1(): String {
        delay(1000L).also { return "Result1" }
    }

    suspend fun getResult2(): String {
        delay(1000L).also { return "Result2" }
    }

    suspend fun getResult3(): String {
        delay(1000L).also { return "Result3" }
    }

    val result: List<String>
    val time = kotlin.system.measureTimeMillis {
        val deferred1 = async { getResult1() }
        val deferred2 = async { getResult2() }
        val deferred3 = async { getResult3() }
        result = listOf(deferred1.await(), deferred2.await(), deferred3.await())
    }

    println("Time: $time")
    println(result)
}

//17 协程上下文
fun testCoroutineContext() = runBlocking {
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        println("${coroutineContext[CoroutineName]?.name} - ${throwable.message}")
        println(throwable.stackTraceToString())
    }
    GlobalScope.launch(CoroutineName("earthgee") + exceptionHandler) {
        throw Exception("haha")
    }.join()
}

