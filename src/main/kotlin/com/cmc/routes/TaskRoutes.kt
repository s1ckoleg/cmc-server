package com.cmc.routes

import com.cmc.database.services.TaskService
import com.cmc.models.Task
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.taskRoutes() {
    val taskService = TaskService()
    
    routing {
        route("/tasks") {
            // Get all tasks
            get {
                // Optional filter by userId
                val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                
                val tasks = if (userId != null) {
                    taskService.getTasksByUserId(userId)
                } else {
                    taskService.getAllTasks()
                }
                
                call.respond(tasks)
            }
            
            // Get task by ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@get
                }
                
                val task = taskService.getTaskById(id)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
                    return@get
                }
                
                call.respond(task)
            }
            
            // Create new task
            post {
                try {
                    val task = call.receive<Task>()
                    val newTask = taskService.createTask(task)
                    
                    if (newTask != null) {
                        call.respond(HttpStatusCode.Created, newTask)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create task"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                }
            }
            
            // Update task
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@put
                }
                
                try {
                    val task = call.receive<Task>()
                    val updated = taskService.updateTask(id, task)
                    
                    if (updated) {
                        val updatedTask = taskService.getTaskById(id)
                        if (updatedTask != null) {
                            call.respond(updatedTask)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to retrieve updated task"))
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                }
            }
            
            // Update task completion status
            patch("/{id}/complete") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@patch
                }
                
                val updated = taskService.markTaskAsCompleted(id)
                if (updated) {
                    val updatedTask = taskService.getTaskById(id)
                    if (updatedTask != null) {
                        call.respond(updatedTask)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to retrieve updated task"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
                }
            }
            
            // Delete task
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@delete
                }
                
                val deleted = taskService.deleteTask(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
                }
            }
        }
    }
} 