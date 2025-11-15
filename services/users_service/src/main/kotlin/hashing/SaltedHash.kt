package com.example.hashing

data class SaltedHash(
    val hash: String,
    val salt: String
)