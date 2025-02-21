package com.example

import com.example.schemas.ChatbotResponse
import com.example.schemas.TutorService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name

fun Application.configureDatabases() {
    install(ContentNegotiation) {
        json()
    }

    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5433/postgres",
        user = "postgres",
        driver = "org.postgresql.Driver",
        password = "postgres",
    )

    println(database.name)

    //val userService = UserService(database)
    val tutorService = TutorService(database)

    /*val json = Json {
        serializersModule = SerializersModule {
            contextual(UUIDSerializer)
        }
    }*/

    routing {
        get("/tutors") {
            val tutors = tutorService.getAllTutors()

            call.respond(tutors)
        }

        get("chatbot") {
            val query = call.receive<String>()
            val response = tutorService.chatbot(query)

            val jsonResponse = response?.let { Json.decodeFromString<ChatbotResponse>(it) }

            call.respond(HttpStatusCode.OK, jsonResponse ?: "nothing")
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
