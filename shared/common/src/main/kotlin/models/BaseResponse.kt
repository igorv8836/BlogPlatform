package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val message: T
)