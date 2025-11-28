package com.example.testflowbank.data.profile

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: ProfileApi
) {
    suspend fun getProfile() = api.getProfile()
}