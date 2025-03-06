package com.example.schemas

import com.example.InstantSerializer
import com.example.UUIDSerializer
import com.example.dbQuery
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*


@Serializable
data class Message(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    /*@Serializable(with = UUIDSerializer::class)
    val chatId: UUID,*/
    @Serializable(with = UUIDSerializer::class)
    val studentId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tutorId: UUID,
    val senderRole: String,
    val message: String?,
    val attachmentUrl: List<String>?,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant? = null,
    //val readStatus: Boolean = false
)

@Serializable
data class StudentMessage(
    @Serializable(with = UUIDSerializer::class)
    val studentId: UUID,
    val message: String?,
    val attachmentUrl: List<String>? = null
) : MessageInterface

@Serializable
data class TutorMessage(
    @Serializable(with = UUIDSerializer::class)
    val tutorId: UUID,
    val message: String?,
    val attachmentUrl: List<String>? = null
) : MessageInterface


@Serializable
sealed interface MessageInterface


class MessageService(database: Database) {

    object MessagesTable : Table("messages") {
        val id = uuid("id").autoGenerate()

        //val chatId = uuid("chat_id")
        val studentId = uuid("student_id")
        val tutorId = uuid("tutor_id")
        val senderRole = varchar("sender_role", 50)
        val text_message = text("message").nullable()
        val attachmentUrl = array<String>("attachment_url").nullable()
        val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
        //val readStatus = bool("read_status").default(false)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(MessagesTable)
        }
    }

    suspend fun sendMessage(sentMessage: MessageInterface, otherID: UUID): UUID = dbQuery {
        println("sendMessage called")

        transaction {
            println("transaction 1/2")

            val messageID: UUID

            when (sentMessage) {
                is StudentMessage -> {
                    messageID = MessagesTable.insert {
                        it[studentId] = sentMessage.studentId
                        it[tutorId] = otherID
                        it[senderRole] = "student"
                        it[text_message] = sentMessage.message
                        it[attachmentUrl] = sentMessage.attachmentUrl
                        //it[timestamp] = message.timestamp.atZone(ZoneId.systemDefault()).toInstant()
                        //it[readStatus] = message.readStatus
                    }[MessagesTable.id]

                    println("StudentMessage")
                }

                is TutorMessage -> {
                    messageID = MessagesTable.insert {
                        it[studentId] = otherID
                        it[tutorId] = sentMessage.tutorId
                        it[senderRole] = "tutor"
                        it[text_message] = sentMessage.message
                        it[attachmentUrl] = sentMessage.attachmentUrl
                        //it[timestamp] = message.timestamp.atZone(ZoneId.systemDefault()).toInstant()
                        //it[readStatus] = message.readStatus
                    }[MessagesTable.id]

                    println("TutorMessage")
                }
            }

            println("transaction 2/2")

            messageID
        }
    }

    suspend fun getChatMessages(studentId: UUID, tutorId: UUID): List<Message> = dbQuery {
        println("getChatMessages called")

        transaction {
            val result = MessagesTable.selectAll().where {
                (MessagesTable.studentId eq studentId) and (MessagesTable.tutorId eq tutorId)
            }.map {
                Message(
                    id = it[MessagesTable.id],
                    studentId = it[MessagesTable.studentId],
                    tutorId = it[MessagesTable.tutorId],
                    senderRole = it[MessagesTable.senderRole],
                    message = it[MessagesTable.text_message],
                    attachmentUrl = it[MessagesTable.attachmentUrl]?.toList(),
                    timestamp = it[MessagesTable.timestamp],
                    //readStatus = it[MessagesTable.readStatus]
                )
            }

            println(result)

            result
        }
    }


}


