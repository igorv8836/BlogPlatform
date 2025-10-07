package com.example.constants

class ConflictException(message: String? = null) : Exception(message)
class UnauthorizedException(message: String? = null) : Exception(message)
class ForbiddenException(message: String? = null) : Exception(message)

class IncorrectBodyException(message: String? = null) : Exception(message)
class IncorrectQueryParameterException(message: String? = null) : Exception(message)