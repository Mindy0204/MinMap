package com.mindyhsu.minmap.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.data.*

interface MinMapRepository {

    suspend fun getDirection(
        startLocation: String,
        endLocation: String,
        apiKey: String,
        mode: String
    ): Result<MapDirection>

    suspend fun setUser(uid: String, image: String, name: String, fcmToken: String): Result<Boolean>

    suspend fun getUserEvent(userId: String): Result<String>

    fun getLiveEventId(userId: String): MutableLiveData<String>

    suspend fun getCurrentEvent(currentEventId: String): Result<Event>

    suspend fun updateMyLocation(userId: String, myGeo: GeoPoint): Result<Boolean>

    fun updateFriendLocation(participantIds: List<String>): MutableLiveData<List<User>>

    suspend fun getFriend(userId: String): Result<List<String>>

    suspend fun sendEvent(event: Event): Result<String>

    suspend fun updateUserCurrentEvent(userId: List<String>, currentEventId: String): Result<Boolean>

    suspend fun updateChatRoomCurrentEvent(participants: List<String>, currentEventId: String): Result<String>

    suspend fun finishEvent(userId: String, eventId: String, chatRoomId: String): Result<Boolean>

    suspend fun getChatRoom(userId: String): Result<List<ChatRoom>>

    suspend fun getChatRoomByCurrentEventId(currentEventId: String): Result<String>

    fun getLiveChatRoom(userId: String): MutableLiveData<List<ChatRoom>>

    suspend fun getUserById(usersIds: List<String>): Result<List<User>>

    fun getMessage(chatRoomId: String, userId: String): MutableLiveData<List<Message>>

    suspend fun sendMessage(chatRoomId: String, message: Message): Result<Boolean>

    suspend fun setFriend(userId: String, friendId: String): Result<String>

    suspend fun getFCMToken(): Result<String>
}
