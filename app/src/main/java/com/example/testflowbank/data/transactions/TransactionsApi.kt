package com.example.testflowbank.data.transactions

import retrofit2.Response
import retrofit2.http.GET

data class PostDto(
    val id: Int,
    val title: String,
    val body: String
)

interface TransactionsApi {
    @GET("posts")
    suspend fun getTransactions(): Response<List<PostDto>>
}