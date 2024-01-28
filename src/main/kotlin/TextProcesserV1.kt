import java.io.File

data class WordFreq(val word: String, val frequency: Int)

class TextProcesserV1 {

    fun processText(text: String): List<WordFreq> {
        return text
            .clean(text)
            .split(" ")
            .getWordCount()
            .sortByFrequency()
    }

    //使用系统库
    fun processTextV2(text: String): List<WordFreq> {
        return text
            .replace("[^A-Za-z]".toRegex(), " ")
            .trim()
            .split(" ")
            .filter {
                it != ""
            }.groupBy { it }
            .map { WordFreq(it.key, it.value.size) }
            .sortedByDescending { it.frequency }
    }

    //使用inline
    fun processTextV3(text: String): List<WordFreq> {
        return text
            .clean(text)
            .split(" ")
            .getWordCount()
            .mapToList {
                WordFreq(it.key, it.value)
            }.sortedByDescending { it.frequency }
    }

    fun processFile(file: File): List<WordFreq> {
        val text=file.readText(Charsets.UTF_8)
        return processTextV3(text)
    }

    fun List<String>.getWordCount(): Map<String, Int> {
        val map = hashMapOf<String, Int>()
        for (word in this) {
            if (word == "") continue
            val trimWord = word.trim()
            val count = map.getOrDefault(trimWord, 0)
            map[trimWord] = count + 1
        }
        return map
    }

    fun Map<String, Int>.sortByFrequency(): MutableList<WordFreq> {
        val list = mutableListOf<WordFreq>()
        for (entry in this) {
            if (entry.key == "") continue
            val freq = WordFreq(entry.key, entry.value)
            list.add(freq)
        }
        list.sortByDescending { it.frequency } // 以词频降序排序
        return list
    }

    private inline fun <T> Map<String, Int>.mapToList(
        transform: (Map.Entry<String, Int>) -> T): MutableList<T> {
        val list = mutableListOf<T>()
        for(item in this) {
            list.add(transform(item))
        }
        return list;
    }

}

fun String.clean(text: String) = text.replace("[^A-Za-z]".toRegex(), " ").trim()

fun main() {
//    println(File("").absolutePath)
    println(TextProcesserV1().processFile(File("article")))

}