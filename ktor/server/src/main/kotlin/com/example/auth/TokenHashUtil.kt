package com.example.auth

import java.security.MessageDigest

fun hashToken(token: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray())
        .joinToString("") { "%02x".format(it) }
