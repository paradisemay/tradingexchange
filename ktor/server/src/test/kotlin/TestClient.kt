package com.example

import kotlinx.serialization.json.*

// Расширения для удобного парсинга JSON в тестах
fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.content ?: ""
