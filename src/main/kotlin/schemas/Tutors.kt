package com.example.schemas

import com.example.UUIDSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
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
    val rating: Double,
    val profilePicture: String?,
    val password: String
)

// val format = Json { prettyPrint = true }

/*

@Serializable
data class Subjects(
    val subject: String,
    val rate: Int
)
*/

/*

@Serializable
data class TutorResponse(
    val response: String,
    val tutors: List<Tutor>
)
*/


@Serializable
data class Tutor(
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
    val tutors: List<Tutor>
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

    object Tutors : Table("tutors") {
        val id = uuid("id").autoGenerate()
        val name = varchar("name", length = 255)
        val phone = varchar("phone", length = 20).nullable()
        val email = varchar("email", length = 255)
        val bio = text("bio")
        val subjects = jsonb<Map<String, Int>>("subjects", Json { prettyPrint = true }) //jsonb(name = "subjects")
        val languages_spoken = array<String>("languages_spoken")
        val location = varchar("location", length = 255).nullable()
        val rating = decimal("rating", precision = 3, scale = 2).default(5.00.toBigDecimal())
        val profilePicture = text("profile_picture").nullable()
        val password = text("password")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Tutors)

            //exec("SELECT set_config('ai.ollama_host', 'http://host.docker.internal:11434', false);")

            println("Init Transaction complete")
        }
    }

    suspend fun create(tutor: ExposedTutor): UUID = dbQuery {
        Tutors.insert {
            it[name] = tutor.name
            it[phone] = tutor.phone
            it[email] = tutor.email
            it[bio] = tutor.bio
            //it[subjects] = tutor.subjects
            it[languages_spoken] = tutor.languages
            it[location] = tutor.location
            it[rating] = tutor.rating.toBigDecimal()
            it[profilePicture] = tutor.profilePicture
            it[password] = tutor.password
        }[Tutors.id]
    }

    suspend fun getAllTutors(): List<ExposedTutor> {
        println("get all 1/2")

        var ttt = listOf<ExposedTutor>()

        return dbQuery {
            println("in query")
            transaction {
                ttt = Tutors.selectAll()
                    .map {
                        ExposedTutor(
                            name = it[Tutors.name],
                            phone = it[Tutors.phone],
                            email = it[Tutors.email],
                            bio = it[Tutors.bio],
                            subjects = it[Tutors.subjects],
                            languages = it[Tutors.languages_spoken],
                            location = it[Tutors.location],
                            rating = it[Tutors.rating].toDouble(),
                            profilePicture = it[Tutors.profilePicture],
                            password = it[Tutors.password],
                        )
                    }.toList()
            }
            println("get all 2/2")

            ttt.forEach { println(it) }

            ttt
        }
    }

    suspend fun chatbot(query: String): String? {
        println("chatbot 1/2")
        return dbQuery{
            transaction {
                // response = generateRagResponse(query)
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

        println("chatbot 2/2")
    }

    suspend fun read(id: UUID): ExposedTutor? {
        return dbQuery {
            Tutors.selectAll().where { Tutors.id eq id }
                .map {
                    ExposedTutor(
                        //it[Tutors.id],
                        it[Tutors.name],
                        it[Tutors.phone],
                        it[Tutors.email],
                        it[Tutors.bio],
                        it[Tutors.subjects],
                        it[Tutors.languages_spoken],
                        it[Tutors.location],
                        it[Tutors.rating].toDouble(),
                        it[Tutors.profilePicture],
                        it[Tutors.password]
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun update(id: UUID, tutor: ExposedTutor) {
        dbQuery {
            Tutors.update({ Tutors.id eq id }) {
                it[name] = tutor.name
                it[phone] = tutor.phone
                it[email] = tutor.email
                it[bio] = tutor.bio
                //it[subjects] = tutor.subjects
                it[languages_spoken] = tutor.languages
                it[location] = tutor.location
                it[rating] = tutor.rating.toBigDecimal()
                it[profilePicture] = tutor.profilePicture
                it[password] = tutor.password
            }
        }
    }

    suspend fun delete(id: UUID) {
        dbQuery {
            Tutors.deleteWhere { Tutors.id eq id }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}