package com.example.schemas

import com.example.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime


@Serializable
data class Message(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    /*@Serializable(with = UUIDSerializer::class)
    val chatId: UUID,*/
    @Serializable(with = UUIDSerializer::class)
    val studentId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tutorId: UUID,
    val senderRole: String,
    val recipientRole: String,
    val message: String?,
    val attachmentUrl: List<String>?,
    val timestamp: String,
    val readStatus: Boolean
)


class MessageService(private val database: Database) {

    object Messages : Table("messages") {
        val id = uuid("id").autoGenerate()
        //val chatId = uuid("chat_id")
        val studentId = uuid("student_id")
        val tutorId = uuid("tutor_id")
        val senderRole = varchar("sender_role", 50)
        val recipientRole = varchar("recipient_role", 50)
        val text_message = text("message").nullable()
        val attachmentUrl = array<String>("attachment_url").nullable()
        val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
        val readStatus = bool("read_status").default(false)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Messages)
        }
    }

    suspend fun createMessage(message: Message): UUID = dbQuery {
        Messages.insert {
            //it[id] = message.id
            //it[chatId] = message.chatId
            it[studentId] = message.studentId
            it[tutorId] = message.tutorId
            it[senderRole] = message.senderRole
            it[recipientRole] = message.recipientRole
            it[text_message] = message.message
            it[attachmentUrl] = message.attachmentUrl
            it[timestamp] = Instant.now()// LocalDateTime.i .now() // LocalDateTime.parse(message.timestamp)
            it[readStatus] = message.readStatus
        }[Messages.id]
    }

   /* suspend fun getMessages(chatId: UUID): List<Message> = dbQuery {
        Messages.select { Messages.chatId eq chatId }
            .map {
                Message(
                    id = it[Messages.id],
                    //chatId = it[Messages.chatId],
                    studentId = it[Messages.studentId],
                    tutorId = it[Messages.tutorId],
                    senderRole = it[Messages.senderRole],
                    recipientRole = it[Messages.recipientRole],
                    message = it[Messages.message],
                    attachmentUrl = it[Messages.attachmentUrl]?.toList(),
                    timestamp = it[Messages.timestamp].toString(),
                    readStatus = it[Messages.readStatus]
                )
            }
    }*/

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}