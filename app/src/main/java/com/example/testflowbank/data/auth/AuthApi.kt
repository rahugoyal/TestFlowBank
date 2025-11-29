package com.example.testflowbank.data.auth

import retrofit2.http.Body
import retrofit2.http.POST
data class LoginResponse(
    val token: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

interface AuthApi {
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
}