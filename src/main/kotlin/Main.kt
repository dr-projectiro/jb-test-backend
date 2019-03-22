import com.github.javafaker.Faker
import io.javalin.Context
import io.javalin.Javalin
import java.lang.IllegalArgumentException
import java.lang.System.currentTimeMillis
import com.google.gson.JsonSyntaxException

const val PAGE_SIZE = 8

fun main() {
    println("Starting JetBridge test backend serving team members")

    println("Generating data ...")
    val data = generateData()

    println("Starting web server ...")
    Javalin.create()
        .get("/team") { context -> handleGetTeam(data, context) }
        .put("/team/:team-member-id/project") { context -> handleChangeProject(data, context) }
        .port(15050).start()
}

fun handleChangeProject(data: List<TeamMemberEntity>, context: Context) {
    try {
        val newProjectId = context.body().toInt()
        val teamMemberId = context.pathParam("team-member-id").toInt()
        // check projectId is valid
        val project = synchronized(data) {
            data.map { it.currentProject }.firstOrNull { it?.id == newProjectId }
        }
        if (project == null) {
            context.status(400)
            context.result("Invalid project id: $newProjectId")
            return
        }
        // check team member id is valid
        // (no need to use synchronized here as we're addressing final 'properly published' data)
        val teamMember = data.firstOrNull { it.id == teamMemberId }
        if (teamMember == null) {
            context.status(400)
            context.result("Invalid team member id: $teamMemberId")
            return
        }
        // change project
        synchronized(data) {
            teamMember.currentProject = project
        }
        context.status(200)
        context.json(true)
    } catch (ex: IllegalArgumentException) {
        context.status(400)
        context.result("Body must contain target project valid id (int)")
    } catch (t: Throwable) {
        handleServerSideError(context, t)
    }

}

fun handleGetTeam(data: List<TeamMemberEntity>, context: Context) {
    try {
        val filter = getFilterFromHttpContext(context)
        val pageNumber = getPageQueryParam(context)
        val responsePage =
            extractPage(synchronized(data) { data.filter { filter.matches(it) } }, PAGE_SIZE, pageNumber)
        context.status(200)
        context.json(responsePage)
    } catch (ex: IllegalArgumentException) {
        // bad input, let's inform client about details
        context.status(400)
        context.result("Bad request, details: ${ex.message}")
    } catch (t: Throwable) {
        handleServerSideError(context, t)
    }

}

fun handleServerSideError(context: Context, t: Throwable) {
    t.printStackTrace()
    context.status(500)
    context.result(
        "Internal server error. " +
                "Please contact nechiporenko.evgeniy@gmail.com for details")
}

fun <T : Any> extractPage(data: List<T>, pageSize: Int, pageNumber: Int): ResponsePage<T> {
    val pageStartIndex = (pageNumber - 1) * pageSize // inclusive,
    val pageEndIndex = pageNumber * pageSize // exclusive

    val queriedPageOfData =
        if (hasPage(data.size, pageSize, pageNumber))
        // truncate range of queried data to fit actual list bounds
            data.subList(maxOf(0, pageStartIndex), minOf(data.size, pageEndIndex))
        else
        // a page outside of actual bounds was queried
            emptyList()

    return ResponsePage(
        queriedPageOfData,
        hasPage(data.size, pageSize, pageNumber - 1),
        hasPage(data.size, pageSize, pageNumber + 1),
        pageNumber)
}

fun hasPage(dataSize: Int, pageSize: Int, pageNumber: Int): Boolean {
    val pageStartIndex = (pageNumber - 1) * pageSize // inclusive,
    val pageEndIndex = pageNumber * pageSize // exclusive

    return pageStartIndex <= dataSize - 1 && pageEndIndex > 0
}

fun getPageQueryParam(context: Context): Int {
    return try {
        context.queryParam("page")?.toInt() ?: 1
    } catch (ex: NumberFormatException) {
        throw IllegalArgumentException(
            "Page must always be an integer; " +
                    "possible query is: /team?page=2")
    }
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

fun getProjectFilterPart(context: Context): List<Int>? {
    return try {
        context.queryParam("project")?.let { convertFromJsonIntArray(it) }
    } catch (ex: NumberFormatException) {
        throw IllegalArgumentException(
            "Project id must always be an integer; " +
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
        if (skillParams.all { matchesSkill(it) }) {
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
    allTeamMembersCount = 82,
    projectsCount = 8,
    skills = listOf("Python", "JS", "Angular", "Java", "Kotlin", "Android", "iOS", "Docker"),
    managersSkill = "Management",
    timezones = listOf(
        "UTC", "EST", "America/New_York", "America/Winnipeg",
        "America/Toronto", "Africa/Tunis", "Europe/Paris"),
    minWorkingDayDurationHours = 5,
    maxWorkingDayDurationHours = 11,
    minWorkingDayStartAtHours = 7,
    maxWorkingDayStartAtHours = 11
).generateData()

data class ResponsePage<T : Any>(
    val items: List<T>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val page: Int)
