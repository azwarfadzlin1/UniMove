package com.example.unimove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FeedPost(
    val id: String = "",
    val username: String = "",
    val content: String = "", // Legacy formatted string
    val runName: String = "",
    val distanceKm: Double = 0.0,
    val durationSeconds: Int = 0,
    val avgPace: String = "",
    val timeStr: String = "",
    val likes: Int = 0,
    val isLiked: Boolean = false,
    val comments: Int = 0,
    val timestamp: Timestamp? = null,
    val pathSegments: List<List<Map<String, Double>>> = emptyList()
)

class CommunityFeedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _posts = MutableStateFlow<List<FeedPost>>(emptyList())
    val posts: StateFlow<List<FeedPost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchFeed()
    }

    private fun fetchFeed() {
        _isLoading.value = true
        firestore.collection("community_feed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postList = snapshot.documents.map { doc ->
                        val username = doc.getString("username") ?: "Unknown User"
                        val runName = doc.getString("runName") ?: "Running Session"
                        val distanceKm = doc.getDouble("distanceKm") ?: 0.0
                        val durationSeconds = doc.getLong("durationSeconds")?.toInt() ?: 0
                        val pace = doc.getString("avgPace") ?: ""
                        val ts = doc.getTimestamp("timestamp")
                        
                        val pathSegments = mutableListOf<List<Map<String, Double>>>()
                        try {
                            val routePointsRaw = doc.get("routePoints") as? List<*>
                            routePointsRaw?.forEach { segmentMap ->
                                (segmentMap as? Map<*, *>)?.get("points")?.let { pointsList ->
                                    val segment = mutableListOf<Map<String, Double>>()
                                    (pointsList as? List<*>)?.forEach { pointMap ->
                                        val p = pointMap as? Map<*, *>
                                        val lat = (p?.get("lat") as? Number)?.toDouble() ?: 0.0
                                        val lng = (p?.get("lng") as? Number)?.toDouble() ?: 0.0
                                        segment.add(mapOf("lat" to lat, "lng" to lng))
                                    }
                                    if (segment.isNotEmpty()) pathSegments.add(segment)
                                }
                            }
                        } catch (e: Exception) {}
                        
                        val currentUserId = auth.currentUser?.uid ?: ""
                        val likedBy = doc.get("likedBy") as? List<*> ?: emptyList<Any>()
                        val isLiked = likedBy.contains(currentUserId)
                        
                        // Use description field if exists, otherwise fallback to legacy content
                        val description = doc.getString("content") ?: doc.getString("description") ?: ""

                        FeedPost(
                            id = doc.id,
                            username = username,
                            content = description,
                            runName = runName,
                            distanceKm = distanceKm,
                            durationSeconds = durationSeconds,
                            avgPace = pace,
                            timeStr = formatTimestamp(ts),
                            likes = doc.getLong("likes")?.toInt() ?: 0,
                            isLiked = isLiked,
                            comments = doc.getLong("comments")?.toInt() ?: 0,
                            timestamp = ts,
                            pathSegments = pathSegments
                        )
                    }
                    _posts.value = postList
                }
                _isLoading.value = false
            }
    }

    fun toggleLike(postId: String, currentIsLiked: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = firestore.collection("community_feed").document(postId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (snapshot.exists()) {
                val likedBy = snapshot.get("likedBy") as? List<*> ?: emptyList<Any>()
                val alreadyLiked = likedBy.contains(uid)
                
                val newLikedBy = if (alreadyLiked) {
                    likedBy.filter { it != uid }
                } else {
                    likedBy + uid
                }
                
                val newLikes = newLikedBy.size
                
                transaction.update(docRef, "likes", newLikes, "likedBy", newLikedBy)
            }
        }.addOnFailureListener {
            // Handle error silently or implement error state if needed
        }
    }

    private fun formatTimestamp(ts: Timestamp?): String {
        if (ts == null) return "Unknown"
        val date = ts.toDate()
        return SimpleDateFormat("dd MMM, yyyy • h:mm a", Locale.getDefault()).format(date)
    }
}
