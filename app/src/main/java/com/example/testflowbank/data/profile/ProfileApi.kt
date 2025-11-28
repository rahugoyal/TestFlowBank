package com.example.testflowbank.data.profile

import retrofit2.Response
import retrofit2.http.GET

data class ProfileUser(
    val id: Int,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?
)

interface ProfileApi {
    @GET("users/1")
    suspend fun getProfile(): Response<ProfileUser>
}