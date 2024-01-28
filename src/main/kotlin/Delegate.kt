import kotlin.reflect.KProperty

class StringDelegate(private var text: String) {

    operator fun getValue(thisRef: Any, property: KProperty<*>): String {
        println("getValue is called - ${property.name} - $property")
        return text
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
        println("setValue is called - ${property.name} - $property")
        text = value
    }

}

class Man {
    var job: String by StringDelegate("earthgee")
    val age: String by StringDelegate("30")
}

fun main() {

    val man = Man()
    println(man.job)
    man.job = "Code"
    println(man.age)

}