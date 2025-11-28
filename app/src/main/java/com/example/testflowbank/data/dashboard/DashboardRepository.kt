package com.example.testflowbank.data.dashboard

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val api: DashboardApi
) {
    suspend fun getUser() = api.getUser()
    suspend fun getCart() = api.getCart()
}