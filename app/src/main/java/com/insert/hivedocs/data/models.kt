package com.insert.hivedocs.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar

@Keep
data class Article(
    @DocumentId val id: String = "",
    val title: String = "",
    val resume: String = "",
    val author: String = "",
    val approved: Boolean = false,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),

    val ratingCount: Long = 0,
    val ratingSum: Double = 0.0,
    val articleUrl: String = ""
) {
    @Suppress("unused")
    constructor() : this("", "", "", "", false, 2024, 0, 0.0, "")
}

@Keep
data class UserProfile(
    val isAdmin: Boolean = false
)

@Keep
data class Review(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "Anônimo",
    val rating: Float = 0f,
    val comment: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

@Keep
data class Reply(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "Anônimo",
    val replyText: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)