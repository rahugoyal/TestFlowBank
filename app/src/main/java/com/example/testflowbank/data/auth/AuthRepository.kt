package com.example.testflowbank.data.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi
) {
    suspend fun login(email: String, password: String): LoginResponse =
        api.login(LoginRequest(email, password))
}