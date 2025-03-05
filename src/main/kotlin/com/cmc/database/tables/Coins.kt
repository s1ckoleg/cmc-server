package com.cmc.database.tables

import org.jetbrains.exposed.sql.Table

object Coins : Table("coins") {
    val id = integer("id").autoIncrement()
    val ticker = varchar("ticker", 20).uniqueIndex()
    val name = varchar("name", 100)
    
    override val primaryKey = PrimaryKey(id)
} 