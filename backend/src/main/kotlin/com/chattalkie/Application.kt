package com.chattalkie

import com.chattalkie.di.appModule
import com.chattalkie.plugins.*
import com.chattalkie.utils.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    JwtConfig.initialize(jwtSecret, jwtIssuer, jwtAudience)

    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureDatabases()
    configureWebSockets()
    configureHTTP()
    configureRouting()
}
