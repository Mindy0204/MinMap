package com.mindyhsu.minmap.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.chat.DialogItem
import com.mindyhsu.minmap.data.*

class DefaultMinMapRepository(private val remoteDataSource: MinMapDataSource) :
    MinMapRepository {
    override suspend fun getDirection(
        startLocation: String,
        endLocation: String,
        apiKey: String,
        mode: String
    ): Result<MapDirection> {
        return remoteDataSource.getDirection(startLocation, endLocation, apiKey, mode)
    }

    override suspend fun setUser(uid: String, image: String, name: String): Result<Boolean> {
        return remoteDataSource.setUser(uid, image, name)
    }

    override suspend fun getUserEvent(userId: String): Result<String> {
        return remoteDataSource.getUserEvent(userId)
    }

    override fun getLiveEventId(userId: String): MutableLiveData<String> {
        return remoteDataSource.getLiveEventId(userId)
    }

    override suspend fun getCurrentEvent(currentEventId: String): Result<Event> {
        return remoteDataSource.getCurrentEvent(currentEventId)
    }

    override suspend fun updateMyLocation(userId: String, myGeo: GeoPoint): Result<Boolean> {
        return remoteDataSource.updateMyLocation(userId, myGeo)
    }

    override fun updateFriendsLocation(participantIds: List<String>): MutableLiveData<List<User>> {
        return remoteDataSource.updateFriendsLocation(participantIds)
    }

    override suspend fun sendEvent(event: Event): Result<Boolean> {
        return remoteDataSource.sendEvent(event)
    }

    override suspend fun finishEvent(userId: String): Result<Boolean> {
        return remoteDataSource.finishEvent(userId)
    }

    override suspend fun getChatRoom(userId: String): Result<List<ChatRoom>> {
        return remoteDataSource.getChatRoom(userId)
    }

    override suspend fun getLiveChatRoom(userId: String): MutableLiveData<List<ChatRoom>> {
        return remoteDataSource.getLiveChatRoom(userId)
    }

    override suspend fun getUsersById(usersIds: List<String>): Result<List<User>> {
        return remoteDataSource.getUsersById(usersIds)
    }

    override fun getMessage(
        chatRoomId: String,
        userId: String
    ): MutableLiveData<List<Message>> {
        return remoteDataSource.getMessage(chatRoomId, userId)
    }

    override suspend fun sendMessage(chatRoomId: String, message: Message): Result<Boolean> {
        return remoteDataSource.sendMessage(chatRoomId, message)
    }
}