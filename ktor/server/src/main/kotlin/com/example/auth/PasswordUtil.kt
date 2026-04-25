package com.example.auth

import de.mkammerer.argon2.Argon2Factory

private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

fun hashPassword(password: String): String =
    argon2.hash(3, 65536, 1, password.toCharArray())

fun verifyPassword(password: String, hash: String): Boolean =
    argon2.verify(hash, password.toCharArray())
