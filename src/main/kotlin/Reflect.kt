import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible

data class Human(private var jobName: String)

fun readMember(obj: Any) {
    val clazz:KClass<out Any> = obj::class
    val className = clazz.simpleName
    clazz.memberProperties.forEach {
        val fieldName = it.name
        it.getter.isAccessible = true
        val fieldValue = it.getter.call(obj)
        println("${className}.${fieldName} = $fieldValue - ${it is KMutableProperty1}")

        tryModify(obj, it)
    }

}

private fun tryModify(obj: Any, property: KProperty1<out Any, *>) {
    if(property.name == "jobName"
        && property is KMutableProperty1
        && property.setter.parameters.size == 2
        && property.returnType.classifier == String::class) {
        property.setter.isAccessible = true
        property.setter.call(obj, "loser")
        println(obj.toString())
    }
}

fun main() {
    readMember("earthgee")
    readMember(Human("coder"))
}