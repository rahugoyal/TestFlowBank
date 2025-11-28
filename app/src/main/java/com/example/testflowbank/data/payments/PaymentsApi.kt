package com.example.testflowbank.data.payments

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface PaymentsApi {
    @GET
    suspend fun simulatePayment(@Url url: String): Response<ResponseBody>
}