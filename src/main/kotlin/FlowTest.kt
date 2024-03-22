import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

//kotlin破解协程 flow
suspend fun main() {
//    simpleFlow1()

//    triggerSimpleFlow()

    testFlowConflate()
}

//简单flow
suspend fun simpleFlow1() {
    val intFlow = flow<Int> {
        (1..3).forEach {
            emit(it)
            log("flow emit $it")
            delay(100)
        }
    }

    //flowOn 运行时绑定调度器
    GlobalScope.launch {
        intFlow.flowOn(Dispatchers.IO)
            .collect {
                log("flow collect $it")
            }
    }.join()
}

//flow消费和触发分离
fun createSimpleFlow2() = flow<Int> {
    (1..3).forEach {
        emit(it)
        delay(100)
    }
}.onEach { log("flow collect $it") }

suspend fun triggerSimpleFlow() {
//    GlobalScope.launch {
//        createSimpleFlow2().collect()
//    }.join()

    createSimpleFlow2().launchIn(GlobalScope).join()
}

//flow的背压
suspend fun testFlowConflate() {
    GlobalScope.launch {
        flow<Int> {
            List(100) {
                emit(it)
            }
        }.conflate()
            .collect {
               delay(100)
                log("flow collect $it")
            }
    }.join()
}






