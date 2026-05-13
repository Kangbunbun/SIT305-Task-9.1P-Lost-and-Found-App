package com.example.lostfoundapp.data

data class LostFoundItem(
    val id: Int = 0,
    val postType: String,
    val contactName: String,
    val phone: String,
    val description: String,
    val category: String,
    val dateTime: String,


    val location: String,
    val imageUri: String,


    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)