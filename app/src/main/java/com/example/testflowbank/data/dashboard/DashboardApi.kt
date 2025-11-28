package com.example.testflowbank.data.dashboard

import retrofit2.Response
import retrofit2.http.GET

data class DummyUser(
    val id: Int?,
    val firstName: String?,
    val lastName: String?,
    val email: String?
)

data class DummyCart(
    val id: Int?,
    val total: Double?,
    val totalProducts: Int?,
    val totalQuantity: Int?
)

interface DashboardApi {
    @GET("users/1")
    suspend fun getUser(): Response<DummyUser>

    @GET("carts/user/1")
    suspend fun getCart(): Response<DummyCart>
}