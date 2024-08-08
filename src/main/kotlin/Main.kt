import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Exception
import java.util.concurrent.Executors

suspend fun main(args: Array<String>) {
//    simpleCoroutine()

//    simpleAsync()

    traversal()

//    coroutinePC()

//    asyncMulti()

//    testCoroutineContext()

//    channelTest()

//    channelReceiveTest()

//    flowSimple()

//    flowComplex()

//    flowDispatch()

//    coroutineLockText()

//    coroutineActor()

//    coroutineFunction()

//    coroutineException()

//    testCoroutineSource()
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
        job1?.invokeOnCompletion {
            println("1 complete cause by ${it?.message}")
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
//    parentJob.children.forEachIndexed { index, job ->
//        when(index) {
//            0 -> println("job is job1: ${job1 === job}")
//            1 -> println("job is job2: ${job2 === job}")
//            2 -> println("job is job3: ${job3 === job}")
//        }
//    }

    parentJob.cancel()
    delay(10000L)
//    println("Process end!")
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
    val exceptionHandler = coroutineLite.CoroutineExceptionHandler { coroutineContext, throwable ->
        println("${coroutineContext[CoroutineName]?.name} - ${throwable.message}")
        println(throwable.stackTraceToString())
    }
    GlobalScope.launch(CoroutineName("earthgee") + exceptionHandler) {
        throw Exception("haha")
    }.join()
}

//19 channel
//或使用consumeEach
fun channelTest() = runBlocking {
    val channel = Channel<Int>()
    launch {
        (1..3).forEach {
            channel.send(it)
            log("send: $it")
        }
        channel.close()
    }
    launch {
        for(i in channel) {
            log("Receive:$i")
        }
    }
    log("end")
}

fun log(text: Any?) = println(Thread.currentThread().name + " " + text)

//19 channel,使用receive
fun channelReceiveTest() = runBlocking {
    val channel = produce {
        (1..2).forEach {
            send(it)
            log("send: $it")
        }
    }
    while(!channel.isClosedForReceive) {
        val result = channel.receiveCatching()
        log("Receive: ${result.getOrNull()}")
    }
}

//20 flow
fun flowSimple() = runBlocking {
    flow {
        (1..5).forEach {
            emit(it)
        }
    }.filter { it > 2 }
        .map { it * 2 }
        .take(2)
        .collect {
            println(it)
        }
}

//20 flow切换线程
fun flowComplex() = runBlocking {
    flow {
        log("emit")
        emit(1)
    }.filter {
        log("filter")
        it > 0
    }.flowOn(Dispatchers.IO)
        .collect {
            log("collect")
        }
}

//21 上下游切换线程
val dispatcher = Executors.newSingleThreadExecutor { Thread(it, "earthgee") }.asCoroutineDispatcher()
val scope = CoroutineScope(dispatcher)

fun flowDispatch() = runBlocking {
    flow {
        log("emit")
        emit(1)
    }.filter {
        log("filter")
        it > 0
    }.onEach {
        log("collect")
    }.launchIn(scope)
}

//22 并发锁
fun coroutineLockText() = runBlocking {
    var i = 0
    val mutex = Mutex()
    val jobs = mutableListOf<Job>()
    repeat(10) { index ->
        val job = launch(Dispatchers.Default) {
            log("coroutine $index")
            delay(1000L)
            repeat(1000) {
                mutex.withLock {
                    i++
                }
            }
        }
        jobs.add(job)
    }
    jobs.joinAll()
    log("i = $i")
}

//22 actor
sealed class Msg
object AddMsg: Msg()
class ResultMsg(val deferred: CompletableDeferred<Int>): Msg()
fun coroutineActor() = runBlocking {
    val actor: SendChannel<Msg> = actor {
        var counter = 0
        for(msg in channel) {
            when(msg) {
                is AddMsg -> {
                    counter++
                }
                is ResultMsg -> {
                    msg.deferred.complete(counter)
                }
            }
        }
    }

    val jobs = mutableListOf<Job>()
    repeat(10) { index ->
        val job = launch(Dispatchers.Default) {
            log("$index")
            delay(1000L)
            repeat(1000) {
                actor.send(AddMsg)
            }
        }
        jobs.add(job)
    }

    jobs.joinAll()

    val deferred = CompletableDeferred<Int>()
    actor.send(ResultMsg(deferred))
    val result = deferred.await()
    actor.close()
    log("i = ${result}")
}

//22 函数式思想处理并发
fun coroutineFunction() = runBlocking {
    (1..10).map { num ->
        async(Dispatchers.Default) {
            log("$num")
            delay(1000L)
            var counter = 0
            repeat(1000) {
                counter++
            }
            return@async counter
        }
    }.awaitAll().sum().also {
        log("i = $it")
    }
}

//23 协程的异常与取消
fun coroutineException() = runBlocking {
    val job = launch(Dispatchers.Default) {
        var i = 0
        while(true) {
            try {
                delay(500L)
            } catch (e: CancellationException) {
                println("${e.message}")
                throw e
            }
            i++
            println("i = $i")
        }
    }

    job.invokeOnCompletion {
        println("invoke compelte")
    }

    delay(2000L)
    job.cancel()
    job.join()
    println("end")
}

//协程源码
fun testCoroutineSource() {
    var label = 0
    label = label or Int.MIN_VALUE
    println(label)
}

//协程源码探究
fun test() {
    val coroutineDispatcher = newSingleThreadContext("ctx")
    GlobalScope.launch(coroutineDispatcher) {
        println("first coroutine start")
        async(Dispatchers.IO) {
            println("second coroutine start")
            delay(100)
            println("second coroutine end")
        }.await()
        println("first coroutine end")
    }
    Thread.sleep(500)
}





