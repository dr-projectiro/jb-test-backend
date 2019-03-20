import com.github.javafaker.Faker
import io.javalin.Context
import io.javalin.Javalin
import java.lang.IllegalArgumentException
import java.lang.System.currentTimeMillis
import com.google.gson.JsonSyntaxException


fun main() {
    println("Starting JetBridge test backend serving team members")

    println("Generating data ...")
    val data = generateData()

    println("Starting web server ...")
    Javalin.create().get("/team") { context ->
        try {
            val filter = getFilterFromHttpContext(context)
            val tempResult = data.filter { filter.matches(it) }
            TODO("paginate then")
        } catch (ex: IllegalArgumentException) {
            // bad input, let's inform client about details
            context.status(400)
            context.result("Bad request, details: ${ex.message}")
        } catch (ex: Throwable) {
            // server-side exception/error
            ex.printStackTrace()
            context.status(500)
            context.result(
                "Internal server error. " +
                        "Please contact nechiporenko.evgeniy@gmail.com for details")
        }
    }.port(15050).start()
}

fun getFilterFromHttpContext(context: Context): Filter {
    val (skillList, skillCombinationOp) = getSkillsFilterPart(context)

    return Filter(
        context.queryParam("holidays")?.toBoolean(),
        context.queryParam("working")?.toBoolean(),
        getProjectFilterPart(context),
        skillCombinationOp,
        skillList,
        currentTimeMillis())
}

fun getProjectFilterPart(context: Context): Int? {
    return try {
        context.queryParam("project")?.toInt()
    } catch (ex: NumberFormatException) {
        throw IllegalArgumentException("Project id must always be an integer; " +
                "possible query is: /team?project=2")
    }
}

fun getSkillsFilterPart(context: Context): Pair<List<String>, BooleanOp?> {
    try {
        // skills filter
        // we accept only flat conjuncts/disjuncts
        // acceptable request: skill=a AND skill=b AND skill=c (query: /team?skill=a&skill=b&skill=c)
        // acceptable request: skill=a OR skill=b OR skill=c (query: /team?skill=[a, b, c])
        // non-acceptable formula: skill=a AND (skill=b OR skill=c) (query would be: /team?skill=a&skill=[b, c]
        val skillParams = context.queryParams("skill")

        // there is no skill filtering in this request
        if (skillParams.isEmpty()) {
            return Pair(emptyList(), null)
        }
        // there is single 'skill' query param which is an array (/team?skill=[a, b, c])
        if (skillParams.size == 1 && matchesJsonSkillArray(skillParams[0])) {
            return Pair(convertFromJsonStringArray(skillParams[0]), BooleanOp.OR)
        }

        // there are several skill query params (/team?skill=a&skill=b&skill=c)
        if (!skillParams.all { matchesSkill(it) }) {
            // or we have many 'skill' query param, each is skill string
            return Pair(skillParams, BooleanOp.AND)
        }
        throw badSkillFormatException()
    } catch (ex: JsonSyntaxException) {
        // we don't tell user what's wrong with their query for simplicity;
        // let's just emphasize correct query format again
        throw badSkillFormatException()
    }
}

fun badSkillFormatException() = IllegalArgumentException(
    "Bad input for skills; " +
            "possible queries are:  /team?skill=a&skill=b&skill=c or /team?skill=[a, b, c]")

fun generateData() = TeamMemberDataGenerator(
    nameGenerator = Faker(),
    allTeamMembersCount = 52,
    projectsCount = 6,
    skills = listOf("Python", "JS", "Angular", "Java", "Kotlin", "Android", "iOS", "Docker"),
    managersSkill = "Management",
    timezones = listOf("PST", "IST"),
    minWorkingDayDurationHours = 5,
    maxWorkingDayDurationHours = 11,
    minWorkingDayStartAtHours = 9,
    maxWorkingDayStartAtHours = 11
).generateData()