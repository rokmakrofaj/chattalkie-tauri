package com.chattalkie.domain

open class DomainException(message: String) : RuntimeException(message)

class UserAlreadyExistsException(username: String) : DomainException("Username '$username' is already taken")
class InvalidCredentialsException : DomainException("Invalid username or password")
class ResourceNotFoundException(resource: String, id: String) : DomainException("$resource with id $id not found")
class PermissionDeniedException(message: String) : DomainException(message)
class ValidationException(message: String) : DomainException(message)
