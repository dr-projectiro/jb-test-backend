import com.github.javafaker.Faker

fun main() {
    println("Starting JetBridge test backend serving team members")

    println("Generating data ...")
    val data = generateData()

    println("Starting web server ...")
    TODO("Start web server")
}

fun generateData() = TeamMemberDataGenerator(
    nameGenerator = Faker(),
    allTeamMembersCount = 52,
    projectsCount = 6,
    skills = listOf("Python","JS","Angular","Java","Kotlin","Android","iOS","Docker"),
    managersSkill = "Management",
    timezones = listOf("PST", "IST"),
    minWorkingDayDurationHours = 5,
    maxWorkingDayDurationHours = 11,
    minWorkingDayStartAtHours = 9,
    maxWorkingDayStartAtHours = 11
).generateData()