package com.chattalkie.app.data

object FakeAuthRepository {
    private val users = mutableMapOf(
        "makrofaj" to User(
            name = "Makrofaj",
            username = "makrofaj",
            password = "123456"
        )
    )
    
    fun login(username: String, password: String): Boolean {
        val user = users[username]
        return user != null && user.password == password
    }
    
    fun register(name: String, username: String, password: String): Boolean {
        if (users.containsKey(username)) {
            return false // Username already exists
        }
        users[username] = User(name, username, password)
        return true
    }
    
    fun getUser(username: String): User? {
        return users[username]
    }
}

data class User(
    val name: String,
    val username: String,
    val password: String
)
