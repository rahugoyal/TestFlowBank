package com.example.testflowbank.data.auth

import retrofit2.http.Body
import retrofit2.http.POST

// The ReqRes login response is: { "token": "QpwL5tke4Pnpja7X4" }
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