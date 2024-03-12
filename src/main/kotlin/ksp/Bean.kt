package ksp

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

data class District(var name: String)

data class Location(var lat: Double, val lng: Double)

data class Company(var name: String, var location: Location, var district: District)

data class Speaker(var name: String, var age: Int, var company: Company)

data class Talk(var name: String, var speaker: Speaker)

fun <T:Any> T.deepcopy1(): T {
    if(!this::class.isData) {
        return this
    }

    return this::class.primaryConstructor!!.let { primaryConstructor ->
        primaryConstructor.parameters.associateWith { parameter ->
            (this::class as KClass<T>).declaredMemberProperties
                .first { it.name == parameter.name }
                .get(this)?.deepcopy1()
        }.let(primaryConstructor::callBy)
    }
}

fun main() {
    val talk = Talk("hello world",
        Speaker("earthgee", 18,
            Company("earth",
                Location(0.0,0.0), District("xxx")
            )
        ))

    val deepCopy1 = talk.deepcopy1()
    deepCopy1.name = "hello world deep copy1"
    deepCopy1.speaker.company = Company("xxx",
        Location(1.0,1.0), District("xxx2"))

//    val copyTalk = talk.copy()
//    copyTalk.name = "hello world copy"
//    copyTalk.speaker.company = Company("xxx",
//        Location(1.0,1.0), District("xxx"))

    println(talk)
    println(deepCopy1)

}