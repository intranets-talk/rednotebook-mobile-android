package com.example.rednotebook.data.network

import com.example.rednotebook.data.model.Entry
import com.example.rednotebook.data.model.EntryBody
import com.example.rednotebook.data.model.MonthRef
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RedNotebookApi {

    @GET("months")
    suspend fun getMonths(): List<MonthRef>

    @GET("entries/{year}/{month}")
    suspend fun getMonthEntries(
        @Path("year") year: Int,
        @Path("month") month: Int
    ): List<Entry>

    @GET("entries/{year}/{month}/{day}")
    suspend fun getEntry(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Path("day") day: Int
    ): Entry

    @PUT("entries/{year}/{month}/{day}")
    suspend fun saveEntry(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Path("day") day: Int,
        @Body body: EntryBody
    ): Response<Unit>

    @DELETE("entries/{year}/{month}/{day}")
    suspend fun deleteEntry(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Path("day") day: Int
    ): Response<Unit>

    @GET("search")
    suspend fun search(@Query("q") query: String): List<Entry>

    @Multipart
    @POST("attachments/{date}")
    suspend fun uploadAttachment(
        @Path("date") date: String,
        @Part file: MultipartBody.Part
    ): AttachmentResponse
}

data class AttachmentResponse(
    val filename: String,
    val url: String,
    val token: String,
    val size: Long
)
