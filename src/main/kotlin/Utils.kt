import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val gsonConverter = Gson()
val stringListType = object : TypeToken<List<String>>() {}.type
val intListType = object : TypeToken<List<Int>>() {}.type

// gets three random items from the list
fun <T> List<T>.threeRandoms(): List<T> =
    if (size <= 3) {
        this
    } else {
        val first = random()
        val second = minus(first).random()
        val third = minus(listOf(first, second)).random()
        listOf(first, second, third)
    }

fun localIsoTimeFromHours(localHoursIn24Format: Int) =
    LocalTime.of(localHoursIn24Format, 0).format(DateTimeFormatter.ISO_TIME)

fun matchesJsonSkillArray(str: String) = str.matches(Regex("\\[\"\\w+\"(, \"\\w+\")*]"))

fun matchesSkill(str: String) = str.matches(Regex("\\w+"))

fun convertFromJsonStringArray(jsonStringArray: String) =
    gsonConverter.fromJson<List<String>>(jsonStringArray, stringListType)

fun convertFromJsonIntArray(jsonIntArray: String) =
    gsonConverter.fromJson<List<Int>>(jsonIntArray, intListType)
