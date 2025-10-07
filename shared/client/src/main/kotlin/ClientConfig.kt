package com.example

data class ClientConfig(
    val baseUrl: String,
    val timeouts: Timeouts = Timeouts(),
) {
    data class Timeouts(val connectMs: Long = 1000, val requestMs: Long = 2000, val socketMs: Long = 2000)
}