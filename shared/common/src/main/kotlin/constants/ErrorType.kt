package com.example.constants

enum class ErrorType(val message: String) {
    INVALID_CREDENTIALS("Invalid credentials"),
    GENERAL("Something went wrong"),
    UNAUTHORIZED("Unauthorized"),
    FORBIDDEN("Forbidden"),
    INCORRECT_BODY("Incorrect body"),
    NULL_RESPONSE("Null answer"),
}