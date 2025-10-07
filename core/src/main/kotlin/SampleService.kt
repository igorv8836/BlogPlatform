package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Data(val value: String)

interface SampleService {
    suspend fun hello(data: Data): String
}

class SampleServiceImpl : SampleService {
    override suspend fun hello(data: Data): String {
        return "Server: ${data.value}"
    }
}

