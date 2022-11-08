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

    override suspend fun sendEvent(event: Event): Result<Boolean> {
        return remoteDataSource.sendEvent(event)
    }

    override suspend fun finishEvent(userId: String): Result<Boolean> {
        return remoteDataSource.finishEvent(userId)
    }

    override suspend fun getChatRoom(userId: String): Result<List<ChatRoom>> {
        return remoteDataSource.getChatRoom(userId)
    }

    override suspend fun getUsersById(usersIds: List<String>): Result<List<User>> {
        return remoteDataSource.getUsersById(usersIds)
    }

    override suspend fun getMessages(chatRoomId: String, userId: String): Result<List<Message>> {
        return remoteDataSource.getMessages(chatRoomId, userId)
    }

    override suspend fun sendMessages(
        senderId: String,
        text: String,
        time: Timestamp
    ): Result<Boolean> {
        return remoteDataSource.sendMessages(senderId, text, time)
    }
}