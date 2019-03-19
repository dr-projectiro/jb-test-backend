import com.github.javafaker.Faker
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom

const val ALL_TEAM_MEMBERS_COUNT = 52
const val ALL_PROJECTS_COUNT = 5

// work time params in hours in 24h format
const val MIN_WORK_TIME_START = 9
const val MAX_WORK_TIME_END = 21
const val MIN_WORK_TIME_DURATION = 4
const val MAX_WORK_TIME_DURATION = 8

const val MANAGEMENT_SKILL = "Management"
val ALL_SKILLS = listOf(
    "Python",
    "JS",
    "Angular",
    "Java",
    "Kotlin",
    "Android",
    "iOS",
    "Docker"
)

class Dao {

    private val teamMembers = TeamMemberDataGenerator(
        nameGenerator = Faker(),
        allTeamMembersCount = ALL_TEAM_MEMBERS_COUNT,
        projectsCount = ALL_PROJECTS_COUNT,
        skills = ALL_SKILLS,
        managersSkill = MANAGEMENT_SKILL)

    fun fetchTeamMembers(filterOptions: FilterOptions, currentTime: String) {
        //ISO_OFFSET_DATE
    }
}

class TeamMemberDataGenerator(
    private val nameGenerator: Faker,
    private val allTeamMembersCount: Int,
    private val projectsCount: Int,
    private val skills: List<String>,
    private val managersSkill: String) {
    private var memberIdCounter = 0

    fun generateData(): List<TeamMemberEntity> {
        // generate required count of projects
        val projects = generateProjects(nameGenerator, projectsCount)

        // generate ceo team member (it's just the same TeamMemberEntity as other guys)
        val ceoTeamMember = generateCeoTeamMember(managersSkill)

        // generate project managers for projects: 1 manager for 1 project
        // let's have them in the map: ProjectEntity (project) -> TeamMemberEntity (project manager)
        val projectsToProjectManagersMap = generateProjectManagers(projects, ceoTeamMember)

        // calculate how much room we have for common team members
        // common team member = one who is not ceo and is not project manager
        val commonTeamMembersCount = ALL_TEAM_MEMBERS_COUNT -
                1 - // ceo is already created
                projectsToProjectManagersMap.size // project managers are already created

        val commonTeamMembers = generateCommonTeamMembersForProjects(
            projectsToProjectManagersMap, commonTeamMembersCount)

        return commonTeamMembers.plus(projectsToProjectManagersMap.values).plus(ceoTeamMember)
    }

    private fun generateCeoTeamMember(managersSkill: String) = TeamMemberEntity(
        getNextMemberId(),
        listOf(managersSkill),
        nameGenerator.name().firstName(), nameGenerator.name().lastName(),
        // let's consider our ceo has no manager, has no current project, has no holidays, etc
        null, null, null, null)

    private fun generateProjectManagers(
        projects: List<ProjectEntity>,
        ceoTeamMember: TeamMemberEntity) =
        projects.associateWith { project ->
            TeamMemberEntity(
                getNextMemberId(),
                ALL_SKILLS.threeRandoms().plus(MANAGEMENT_SKILL),
                nameGenerator.name().firstName(), nameGenerator.name().lastName(),
                ceoTeamMember.convertToManagerId(),
                getRandomDayWithinOneWeekRange(),
                getRandomDayWithinOneWeekRange(),
                project
            )
        }

    private fun generateCommonTeamMembersForProjects(
        projectToProjectManagerMap: Map<ProjectEntity, TeamMemberEntity>,
        commonTeamMembersCount: Int) =
        (1..commonTeamMembersCount).map {
            val assignedProject = projectToProjectManagerMap.keys.random()
            return@map TeamMemberEntity(
                getNextMemberId(),
                skills.threeRandoms(),
                nameGenerator.name().firstName(), nameGenerator.name().lastName(),
                projectToProjectManagerMap[assignedProject]!!.convertToManagerId(),
                getRandomDayWithinOneWeekRange(),
                getRandomDayWithinOneWeekRange(),
                assignedProject
            )
        }

    private fun generateProjects(nameGenerator: Faker, projectsCount: Int) =
        (1..projectsCount)
            .map { i -> ProjectEntity(i, nameGenerator.app().name()) }

    private fun getRandomDayWithinOneWeekRange(): String {
        val localDateNow = LocalDate.now()
        val singleWeekDayCount = 7L
        val offsetToNow = ThreadLocalRandom.current().nextLong(2 * singleWeekDayCount) - singleWeekDayCount
        return localDateNow.plusDays(offsetToNow).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun getNextMemberId() = ++memberIdCounter
}

data class FilterOptions(
    val onHolidaysNow: Boolean? = null,
    val workingNow: Boolean? = null,
    val projectId: Int? = null,
    val mustHaveAllSkills: List<String>? = null)

data class TeamMembersPage(
    val items: List<TeamMemberEntity>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val page: Int)

data class ManagerId(
    val id: Int,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String)

data class ProjectEntity(
    val id: Int,

    @SerializedName("project_name")
    val projectName: String)

data class TeamMemberEntity(
    val id: Int,

    val skills: List<String>,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String,

    @SerializedName("manager_id")
    val managerId: ManagerId?,

    @SerializedName("on_holidays_till")
    val onHolidaysTill: String?,

    @SerializedName("free_since")
    val freeSince: String?,

    @SerializedName("current_project")
    val currentProject: ProjectEntity?) {

    fun convertToManagerId() = ManagerId(id, firstName, lastName)
}
