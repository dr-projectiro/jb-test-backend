import com.fasterxml.jackson.annotation.JsonProperty
import com.github.javafaker.Faker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.util.*
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
        val workingHoursStart = (minWorkingDayStartAtHours..maxWorkingDayStartAtHours).random()
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

data class Filter(
    private val onHolidaysNow: Boolean? = null,
    private val workingNow: Boolean? = null,
    private val projects: List<Int>? = null,
    private val skillCombinationOperator: BooleanOp?,
    private val mustHaveAllSkills: List<String>? = null,
    private val currentTimeMillis: Long) {

    fun matches(teamMember: TeamMemberEntity) =
        matchesProject(teamMember) &&
                matchesSkills(teamMember) &&
                matchesIsOnHolidaysNow(teamMember) &&
                matchesIsWorkingNow(teamMember)

    private fun matchesProject(teamMember: TeamMemberEntity) =
        projects?.contains(teamMember.currentProject?.id) ?: true

    private fun matchesSkills(teamMember: TeamMemberEntity) = mustHaveAllSkills?.let {
        when (skillCombinationOperator) {
            BooleanOp.AND -> teamMember.skills.containsAll(it)
            BooleanOp.OR -> teamMember.skills.intersect(it).isNotEmpty()
            null -> true
        }
    } ?: true

    private fun matchesIsWorkingNow(teamMember: TeamMemberEntity): Boolean {
        if (workingNow == null) {
            return true
        }
        val timeZone = TimeZone.getTimeZone(teamMember.workingHours.timezone).toZoneId()
        val timeZoneOffset = timeZone.rules.getOffset(Instant.ofEpochMilli(currentTimeMillis))

        // current local time in team member timezone
        val currentLocalTime = LocalDateTime.ofEpochSecond(
            currentTimeMillis / 1000, 0,
            timeZoneOffset).toLocalTime()
        val workDayStart = LocalTime.parse(teamMember.workingHours.endLocalIsoTime)
        val workDayEnd = LocalTime.parse(teamMember.workingHours.startLocalIsoTime)
        val isMemberCurrentlyWorking = currentLocalTime.isBefore(workDayEnd) && currentLocalTime.isAfter(workDayStart)

        // team member matches if they work and we want working ones OR they don't work and we want not working ones
        return workingNow == isMemberCurrentlyWorking
    }

    private fun matchesIsOnHolidaysNow(teamMember: TeamMemberEntity): Boolean {
        if (onHolidaysNow == null || teamMember.onHolidaysTillIsoDate == null) {
            return true
        }

        val timeZone = TimeZone.getTimeZone(teamMember.workingHours.timezone).toZoneId()
        val timeZoneOffset = timeZone.rules.getOffset(Instant.ofEpochMilli(currentTimeMillis))
        // current local time in team member timezone
        val currentLocalDate = LocalDateTime.ofEpochSecond(
            currentTimeMillis / 1000, 0,
            timeZoneOffset).toLocalDate()

        val holidaysLastDay = LocalDate.parse(teamMember.onHolidaysTillIsoDate, ISO_LOCAL_DATE)
        return currentLocalDate.isBefore(holidaysLastDay.plusDays(1))
    }
}

data class ManagerId(
    val id: Int,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String)

data class ProjectEntity(
    val id: Int,

    @JsonProperty("project_name")
    val projectName: String)

data class WorkingHoursEntity(
    val timezone: String,

    @JsonProperty("start")
    val startLocalIsoTime: String,

    @JsonProperty("end")
    val endLocalIsoTime: String)

data class TeamMemberEntity(
    val id: Int,

    val skills: List<String>,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("manager_id")
    val managerId: ManagerId?,

    @JsonProperty("on_holidays_till")
    val onHolidaysTillIsoDate: String?, // consider holidays include this date

    @JsonProperty("free_since")
    val freeSinceIsoDate: String?,

    @JsonProperty("current_project")
    val currentProject: ProjectEntity?,

    @JsonProperty("working_hours")
    val workingHours: WorkingHoursEntity) {

    fun convertToManagerId() = ManagerId(id, firstName, lastName)
}

enum class BooleanOp { AND, OR }
