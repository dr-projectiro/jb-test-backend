import com.github.javafaker.Faker
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom

class TeamMemberDataGenerator(
    private val nameGenerator: Faker,
    private val allTeamMembersCount: Int,
    private val projectsCount: Int,
    private val skills: List<String>,
    private val managersSkill: String,
    private val timezones: List<String>,
    private val minWorkingDayDurationHours: Int,
    private val maxWorkingDayDurationHours: Int,
    private val minWorkingDayStartAtHours: Int,
    private val maxWorkingDayStartAtHours: Int) {
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
        // (count ceo and project managers as already created users)
        val commonTeamMembersCount = allTeamMembersCount - 1 - projectsToProjectManagersMap.size

        val commonTeamMembers = generateCommonTeamMembersForProjects(
            projectsToProjectManagersMap, commonTeamMembersCount)

        return commonTeamMembers.plus(projectsToProjectManagersMap.values).plus(ceoTeamMember)
    }

    private fun generateCeoTeamMember(managersSkill: String) = TeamMemberEntity(
        getNextMemberId(),
        listOf(managersSkill),
        nameGenerator.name().firstName(), nameGenerator.name().lastName(),
        // let's consider our ceo has no manager, has no current project, has no holidays, etc
        null, null, null, null,
        generateCeoWorkingHours())

    private fun generateCeoWorkingHours() = WorkingHoursEntity(
        timezones.random(),
        localIsoTimeFromHours(minWorkingDayStartAtHours),
        localIsoTimeFromHours(minWorkingDayStartAtHours + maxWorkingDayDurationHours))

    private fun generateRandomWorkingHours(): WorkingHoursEntity {
        val duration = (minWorkingDayDurationHours..maxWorkingDayDurationHours).random()
        val workingHoursStart = (minWorkingDayStartAtHours..(maxWorkingDayDurationHours - duration)).random()
        return WorkingHoursEntity(
            timezones.random(),
            localIsoTimeFromHours(workingHoursStart),
            localIsoTimeFromHours(workingHoursStart + duration))
    }

    private fun generateProjectManagers(
        projects: List<ProjectEntity>,
        ceoTeamMember: TeamMemberEntity) =
        projects.associateWith { project ->
            TeamMemberEntity(
                getNextMemberId(),
                skills.threeRandoms().plus(managersSkill),
                nameGenerator.name().firstName(), nameGenerator.name().lastName(),
                ceoTeamMember.convertToManagerId(),
                getRandomDayWithinOneWeekRange(),
                getRandomDayWithinOneWeekRange(),
                project,
                generateRandomWorkingHours())
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
                assignedProject,
                generateRandomWorkingHours())
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
    private val onHolidaysNow: Boolean? = null,
    private val workingNow: Boolean? = null,
    private val projectId: Int? = null,
    private val mustHaveAllSkills: List<String>? = null,
    private val currentTime: Long) {

    private fun matches(teamMember: TeamMemberEntity) {
        val criteria = {}
    }

    private fun matchesProject(teamMember: TeamMemberEntity) = projectId?.let {
        teamMember.currentProject?.id == it
    } ?: true

    private fun matchesSkills(teamMember: TeamMemberEntity) = mustHaveAllSkills?.let {
        teamMember.skills.containsAll(it)
    } ?: true

    private fun matchesIsWorkingNow(teamMember: TeamMemberEntity) {


        TODO("conjunct with matches is on holidays now")
    }

    private fun matchesIsOnHolidaysNow(teamMember: TeamMemberEntity) {
        TODO("calculate timestamp which is the end of the last holiday day and compare it with now")
    }
}

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

data class WorkingHoursEntity(
    val timezone: String,

    @SerializedName("start")
    val startLocalIsoTime: String,

    @SerializedName("end")
    val endLocalIsoTime: String)

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
    val onHolidaysTillIsoDate: String?, // consider holidays include this date

    @SerializedName("free_since")
    val freeSinceIsoDate: String?,

    @SerializedName("current_project")
    val currentProject: ProjectEntity?,

    @SerializedName("working_hours")
    val workingHours: WorkingHoursEntity) {

    fun convertToManagerId() = ManagerId(id, firstName, lastName)
}
