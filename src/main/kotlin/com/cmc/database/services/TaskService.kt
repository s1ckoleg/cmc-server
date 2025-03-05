package com.cmc.database.services

import com.cmc.database.DatabaseFactory.dbQuery
import com.cmc.database.tables.Tasks
import com.cmc.models.Task
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class TaskService {
    
    suspend fun getAllTasks(): List<Task> = dbQuery {
        Tasks.selectAll()
            .map { it.toTask() }
    }
    
    suspend fun getTasksByUserId(userId: Int): List<Task> = dbQuery {
        Tasks.select { Tasks.userId eq userId }
            .map { it.toTask() }
    }
    
    suspend fun getTaskById(id: Int): Task? = dbQuery {
        Tasks.select { Tasks.id eq id }
            .map { it.toTask() }
            .singleOrNull()
    }
    
    suspend fun createTask(task: Task): Task? = dbQuery {
        val insertStatement = Tasks.insert {
            it[title] = task.title
            it[description] = task.description
            it[completed] = task.completed
            it[userId] = task.userId ?: throw IllegalArgumentException("User ID is required")
        }
        
        insertStatement.resultedValues?.singleOrNull()?.toTask()
    }
    
    suspend fun updateTask(id: Int, task: Task): Boolean = dbQuery {
        Tasks.update({ Tasks.id eq id }) {
            it[title] = task.title
            it[description] = task.description
            it[completed] = task.completed
            it[updatedAt] = LocalDateTime.now()
            if (task.userId != null) {
                it[userId] = task.userId
            }
        } > 0
    }
    
    suspend fun markTaskAsCompleted(id: Int): Boolean = dbQuery {
        Tasks.update({ Tasks.id eq id }) {
            it[completed] = true
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }
    
    suspend fun deleteTask(id: Int): Boolean = dbQuery {
        Tasks.deleteWhere { Tasks.id eq id } > 0
    }
    
    private fun ResultRow.toTask(): Task = Task(
        id = this[Tasks.id],
        title = this[Tasks.title],
        description = this[Tasks.description],
        completed = this[Tasks.completed],
        userId = this[Tasks.userId]
    )
} 