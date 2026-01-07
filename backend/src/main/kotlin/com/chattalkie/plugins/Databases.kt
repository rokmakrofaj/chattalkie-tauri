package com.chattalkie.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.chattalkie.database.tables.*

fun Application.configureDatabases() {
    val databaseUrl = environment.config.property("database.url").getString()
    val databaseUser = environment.config.property("database.user").getString()
    val databasePassword = environment.config.property("database.password").getString()

    val config = HikariConfig().apply {
        jdbcUrl = databaseUrl
        username = databaseUser
        password = databasePassword
        driverClassName = if (databaseUrl.startsWith("jdbc:h2")) "org.h2.Driver" else "org.postgresql.Driver"
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)

    transaction {
        // Initial Schema Creation / Migration
        SchemaUtils.createMissingTablesAndColumns(Users, Messages, Friends, Groups, GroupMembers, Sessions, InviteLinks, DeletedItems)
    }
}
