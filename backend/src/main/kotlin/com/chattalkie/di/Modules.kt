package com.chattalkie.di

import com.chattalkie.repositories.*
import com.chattalkie.services.*
import com.chattalkie.socket.ChatController
import org.koin.dsl.module

val appModule = module {
    // MinIO Client (Should use config, strict hardcoded fallback for ensuring compilation)
    single { 
        io.minio.MinioClient.builder()
            .endpoint("http://127.0.0.1:9000")
            .credentials("minioadmin", "minioadmin123") // TODO: Move to config
            .build()
    }

    // Repositories (Singletons)
    single<UserRepository> { UserRepositoryImpl() }
    single<GroupRepository> { GroupRepositoryImpl() }
    single<MessageRepository> { MessageRepositoryImpl() }
    single<InviteRepository> { InviteRepositoryImpl() }
    single { StorageRepository(get()) }

    // Services (Singletons, injecting Repositories)
    single { UserService(get()) }
    single { GroupService(get()) }
    single { MessageService(get()) }
    single { InviteService(get(), get()) }
    single { AuthService(get()) }
    single { ChatService(get(), get(), get()) }
    single { SyncService(get(), get(), get()) }
    single { PresenceService() } // Added PresenceService

    // Socket Controller (Singleton, injecting ChatService and PresenceService)
    single { ChatController(get(), get()) }
}
