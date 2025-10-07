package com.example.utils

import java.time.OffsetDateTime
import java.time.ZoneOffset

object TimeUtils {
    fun currentUtcOffsetDateTime() = OffsetDateTime.now(ZoneOffset.UTC)
}