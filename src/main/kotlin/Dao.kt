import com.github.javafaker.Faker
import com.google.gson.annotations.SerializedName
import org.ajbrown.namemachine.NameGenerator

const val ALL_TEAM_MEMBERS_COUNT = 52
const val MAX_PROJECT_MEMBER_COUNT = 5
const val MIN_PROJECT_MEMBER_COUNT = 2

// work time params in hours in 24h format
const val MIN_WORK_TIME_START = 9
const val MAX_WORK_TIME_END = 21
const val MIN_WORK_TIME_DURATION = 4
const val MAX_WORK_TIME_DURATION = 8

class Dao {

    private val teamMembers = createTeamMembers(ALL_TEAM_MEMBERS_COUNT)

    fun fetchTeamMembers(
        filterOptions: FilterOptions,
        currentTime: String) {
        //ISO_OFFSET_DATE
    }

    private fun createTeamMembers(teamMembersCount: Int): List<TeamMemberEntity> {
//        Faker().team()
        return emptyList()
    }

    private fun generateProjects(projectCount: Int): List<ProjectPrototypeEntity>
}

data class ProjectPrototypeEntity(
    val id: String,
    val )

data class FilterOptions(
    val onHolidaysNow: Boolean? = null,
    val workingNow: Boolean? = null,
    val projectId: Int? = null,
    val mustHaveAllSkills: List<String>? = null
)

data class TeamMembersPage(
    val items: List<TeamMemberEntity>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val page: Int
)

data class UserProfileEntity(
    val id: Int,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String
)

data class ProjectEntity(
    val id: Int,

    @SerializedName("project_name")
    val projectName: String
)

data class TeamMemberEntity(
    val skills: List<String>,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String,

    @SerializedName("manager_id")
    val managerId: UserProfileEntity,

    @SerializedName("on_holidays_till")
    val onHolidaysTill: String,

    @SerializedName("free_since")
    val freeSince: String,

    @SerializedName("current_project")
    val currentProject: ProjectEntity
)
