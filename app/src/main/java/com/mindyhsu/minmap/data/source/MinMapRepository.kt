package com.mindyhsu.minmap.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.chat.DialogItem
import com.mindyhsu.minmap.data.*

interface MinMapRepository {

    suspend fun getDirection(
        startLocation: String,
        endLocation: String,
        apiKey: String,
        mode: String
    ): Result<MapDirection>

    suspend fun getUserEvent(userId: String): Result<String>

    fun getLiveEventId(userId: String): MutableLiveData<String>

    suspend fun getCurrentEvent(currentEventId: String): Result<Event>

    suspend fun updateMyLocation(userId: String, myGeo: GeoPoint): Result<Boolean>

    fun updateFriendsLocation(participantIds: List<String>): MutableLiveData<List<User>>

    suspend fun sendEvent(event: Event): Result<Boolean>

    suspend fun finishEvent(userId: String): Result<Boolean>

    suspend fun getChatRoom(userId: String): Result<List<ChatRoom>>

    suspend fun getUsersById(usersIds: List<String>): Result<List<User>>

    suspend fun getMessages(chatRoomId: String, userId: String): Result<List<Message>>

    suspend fun sendMessages(senderId: String, text: String, time: Timestamp): Result<Boolean>
}