package com.example.schemas

import com.example.LocalDateSerializer
import com.example.UUIDSerializer
import com.example.dbQuery
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.*


@Serializable
data class ExposedTutor(
    val name: String,
    val phone: String?,
    val email: String,
    val bio: String,
    val subjects: Map<String, Int>,
    val languages: List<String>,
    val location: String?,
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate,
    val rating: Double,
    val profilePicture: String?,
    val password: String
)

// val format = Json { prettyPrint = true }


@Serializable
data class SuggestedTutor(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val subjects_taught: Map<String, Int>,
    val languages_spoken: List<String>,
    val rating: Double,
    val bio: String
)

@Serializable
data class ChatbotResponse(
    val response: String,
    val tutors: List<SuggestedTutor>
)


/*

fun generateRagResponse(query: String): String? {
    return transaction {
        exec("SELECT set_config('ai.ollama_host', 'http://host.docker.internal:11434', false);")
        exec("SELECT generate_rag_response('$query') as response;") { rs ->
            if (rs.next()) {
                rs.getString("response")
            } else {
                null
            }
        }
    }
}
*/


class TutorService(database: Database) {

    object TutorsTable : Table("tutors") {
        val id = uuid("id").autoGenerate()
        val name = varchar("name", length = 255)
        val phone = varchar("phone", length = 20).nullable()
        val email = varchar("email", length = 255)
        val bio = text("bio")
        val subjects = jsonb<Map<String, Int>>("subjects", Json { prettyPrint = true })
        val languages_spoken = array<String>("languages_spoken")
        val location = varchar("location", length = 255).nullable()
        val dateOfBirth = date("date_of_birth")
        val rating = decimal("rating", precision = 3, scale = 2).default(5.00.toBigDecimal())
        val profilePicture = text("profile_picture").nullable()
        val password = text("password")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(TutorsTable)

            //exec("SELECT set_config('ai.ollama_host', 'http://host.docker.internal:11434', false);")

            println("Init Transaction complete")
        }
    }

    suspend fun create(tutor: ExposedTutor): UUID = dbQuery {
        TutorsTable.insert {
            it[name] = tutor.name
            it[phone] = tutor.phone
            it[email] = tutor.email
            it[bio] = tutor.bio
            //it[subjects] = tutor.subjects
            it[languages_spoken] = tutor.languages
            it[location] = tutor.location
            it[dateOfBirth] = tutor.dateOfBirth
            it[profilePicture] = tutor.profilePicture
            it[password] = tutor.password
        }[TutorsTable.id]
    }

    suspend fun getAllTutors(): List<ExposedTutor> {
        println("get all 1/2")

        var ttt = listOf<ExposedTutor>()

        return dbQuery {
            println("in query")
            transaction {
                ttt = TutorsTable.selectAll()
                    .map {
                        ExposedTutor(
                            name = it[TutorsTable.name],
                            phone = it[TutorsTable.phone],
                            email = it[TutorsTable.email],
                            bio = it[TutorsTable.bio],
                            subjects = it[TutorsTable.subjects],
                            languages = it[TutorsTable.languages_spoken],
                            location = it[TutorsTable.location],
                            dateOfBirth = it[TutorsTable.dateOfBirth],
                            rating = it[TutorsTable.rating].toDouble(),
                            profilePicture = it[TutorsTable.profilePicture],
                            password = it[TutorsTable.password],
                        )
                    }
            }
            println("get all 2/2")

            ttt.forEach { println(it) }

            println("Number: " + ttt.count())

            ttt
        }
    }

    suspend fun chatbot(query: String): String? {
        println("chatbot 1/2")

        return dbQuery{
            transaction {
                // response = generateRagResponse(query)
                exec("SELECT set_config('ai.ollama_host', 'http://host.docker.internal:11434', false);")
                println("local ollama selected")
                // TODO fix error with single quote ' in query
                exec("SELECT generate_rag_response('$query') as response;") { rs ->
                    if (rs.next()) {
                        println("second exec done")

                        println("LLM response: " + rs.getString("response"))

                        rs.getString("response")
                    } else {
                        null
                    }
                }
            }
        }

        println("chatbot 2/2")
    }

    suspend fun read(id: UUID): ExposedTutor? {
        return dbQuery {
            TutorsTable.selectAll().where { TutorsTable.id eq id }
                .map {
                    ExposedTutor(
                        //it[Tutors.id],
                        it[TutorsTable.name],
                        it[TutorsTable.phone],
                        it[TutorsTable.email],
                        it[TutorsTable.bio],
                        it[TutorsTable.subjects],
                        it[TutorsTable.languages_spoken],
                        it[TutorsTable.location],
                        it[TutorsTable.dateOfBirth],
                        it[TutorsTable.rating].toDouble(),
                        it[TutorsTable.profilePicture],
                        it[TutorsTable.password]
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun update(id: UUID, tutor: ExposedTutor) {
        dbQuery {
            TutorsTable.update({ TutorsTable.id eq id }) {
                it[name] = tutor.name
                it[phone] = tutor.phone
                it[email] = tutor.email
                it[bio] = tutor.bio
                //it[subjects] = tutor.subjects
                it[languages_spoken] = tutor.languages
                it[location] = tutor.location
                it[dateOfBirth] = tutor.dateOfBirth
                it[rating] = tutor.rating.toBigDecimal()
                it[profilePicture] = tutor.profilePicture
                it[password] = tutor.password
            }
        }
    }

    suspend fun delete(id: UUID) {
        dbQuery {
            TutorsTable.deleteWhere { TutorsTable.id eq id }
        }
    }
}