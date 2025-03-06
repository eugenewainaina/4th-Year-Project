package com.example

import com.example.schemas.ChatbotResponse
import com.example.schemas.MessageInterface
import com.example.schemas.MessageService
import com.example.schemas.StudentMessage
import com.example.schemas.TutorMessage
import com.example.schemas.TutorService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlinx.serialization.modules.contextual


suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }


fun Application.configureDatabases() {
    install(ContentNegotiation) {
        json(Json{
            serializersModule = SerializersModule {
                contextual(UUIDSerializer)
            }
        }) // original: json().
    // Current block version:
    // solves the SerializationException by ensuring that the UUID class is properly serialized and deserialized.
    }

    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5433/postgres",
        user = "postgres",
        driver = "org.postgresql.Driver",
        password = "postgres",
    )

    println(database.name)

    val tutorService = TutorService(database)
    val messageService = MessageService(database)


    routing {
        get("/tutors") {
            val tutors = tutorService.getAllTutors()

            call.respond(tutors)
        }

        get("/chatbot") {
            val query = call.request.queryParameters["query"]

            if (query != null) {
                val response = tutorService.chatbot(query)
                val jsonResponse = response?.let { Json.decodeFromString<ChatbotResponse>(it) }

                call.respond(HttpStatusCode.OK, jsonResponse ?: "nothing")
            } else {
                call.respond(HttpStatusCode.BadRequest, "Query parameter is missing")
            }
        }

        // student calls this route
        post("/student_send_message") {
            val message = call.receive<StudentMessage>()
            val tutorId = call.queryParameters["tutor_id"]?.let { UUID.fromString(it) }

            if (tutorId == null) {
                call.respond(HttpStatusCode.BadRequest, "Tutor ID is missing")
                return@post
            }

            val response = messageService.sendMessage(message, tutorId)

            call.respond(HttpStatusCode.OK, response)
        }

        // for both
        post("/send_message") {
            println("send message api called")
            val message = call.receive<MessageInterface>()
            val otherId = call.queryParameters["other_id"]?.let { UUID.fromString(it) }

            if (otherId == null) {
                call.respond(HttpStatusCode.BadRequest, "Other Person's ID is missing")
                return@post
            }

            val response = messageService.sendMessage(message, otherId)

            call.respond(HttpStatusCode.OK, response)
        }

        // tutor calls this route
        post("/tutor_send_message") {
            val message = call.receive<TutorMessage>()
            val studentId = call.queryParameters["student_id"]?.let { UUID.fromString(it) }

            if (studentId == null) {
                call.respond(HttpStatusCode.BadRequest, "Student ID is missing")
                return@post
            }

            val response = messageService.sendMessage(message, studentId)

            call.respond(HttpStatusCode.OK, response)
        }

        // gets all messages btwn tutor and student
        get("/get_messages") {
            val tutorId = call.queryParameters["tutor_id"]?.let { UUID.fromString(it) }
            val studentId = call.queryParameters["student_id"]?.let { UUID.fromString(it) }

            if (tutorId == null) {
                call.respond(HttpStatusCode.BadRequest, "Tutor ID is missing")
                return@get
            }
            if (studentId == null) {
                call.respond(HttpStatusCode.BadRequest, "Student ID is missing")
                return@get
            }

            val response = messageService.getChatMessages(studentId = studentId, tutorId = tutorId)

            call.respond(HttpStatusCode.OK, response)
        }

        /*
        // Create user
        post("/users") {
            val user = call.receive<ExposedUser>()
            val id = userService.create(user)
            call.respond(HttpStatusCode.Created, id)
        }

        // Read user
        get("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = userService.read(id)
            if (user != null) {
                call.respond(HttpStatusCode.OK, user)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Update user
        put("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = call.receive<ExposedUser>()
            userService.update(id, user)
            call.respond(HttpStatusCode.OK)
        }

        // Delete user
        delete("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            userService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
        */
    }
}
