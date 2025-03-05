package com.cmc.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PortfolioEntries : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val cryptoId = integer("crypto_id").references(Coins.id)
    val quantity = decimal("quantity", 20, 8)
    val entryPrice = decimal("entry_price", 20, 8)
    val entryDate = datetime("entry_date").default(LocalDateTime.now())
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
} 