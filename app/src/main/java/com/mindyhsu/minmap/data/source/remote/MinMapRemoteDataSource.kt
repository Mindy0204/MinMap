package com.mindyhsu.minmap.data.source.remote

import androidx.lifecycle.MutableLiveData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.chat.DialogItem
import com.mindyhsu.minmap.data.*
import com.mindyhsu.minmap.data.MapDirection
import com.mindyhsu.minmap.data.source.MinMapDataSource
import com.mindyhsu.minmap.network.MinMapApi
import com.mindyhsu.minmap.util.Util.getString
import com.mindyhsu.minmap.util.Util.isInternetConnected
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object MinMapRemoteDataSource : MinMapDataSource {

    private const val PATH_USERS = "users"
    private const val PATH_EVENTS = "events"
    private const val PATH_CHAT_ROOMS = "chatRooms"

    private const val FIELD_CURRENT_EVENT = "currentEvent"
    private const val FIELD_GEO_HASH = "geoHash"
    private const val FIELD_PARTICIPANTS = "participants"
    private const val FIELD_ID = "id"
    private const val FIELD_NAME = "name"
    private const val FIELD_MESSAGES = "messages"
    private const val FIELD_TIME = "time"
    private const val FIELD_SENDER_ID = "senderId"

    override suspend fun getDirection(
        startLocation: String,
        endLocation: String,
        apiKey: String,
        mode: String
    ): Result<MapDirection> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            // this will run on a thread managed by Retrofit
            val listResult =
                MinMapApi.retrofitService.getDirection(startLocation, endLocation, apiKey, mode)

            Result.Success(listResult)
        } catch (e: Exception) {
            Timber.d("getDirection => exception=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun setUser(uid: String, image: String, name: String): Result<Boolean> =
        suspendCoroutine { continuation ->
            Timber.d("setUser => setUser uid=$uid")
            val userRef = FirebaseFirestore.getInstance().collection(PATH_USERS).document(uid)
            FirebaseFirestore.getInstance().runTransaction { transaction ->
                Timber.d("setUser => transaction uid=$uid")

                val snapshot = transaction.get(userRef)
                if (snapshot.data == null) {
                    transaction.set(userRef, User(id = uid, image = image, name = name))
                    Timber.d("setUser => After set user=${snapshot.data}")
                } else {
                    Timber.d("setUser => User=${snapshot.data}")
                }
            }.addOnCompleteListener { task ->
                Timber.d("setUser =>addOnCompleteListener")

                if (task.isSuccessful) {
                    Timber.d("setUser => Set/ Update user=${task.result}")
                    continuation.resume(Result.Success(true))
                } else {
                    task.exception?.let {
                        Timber.d("setUser => Set/ Update documents error=${it.message}")
                        continuation.resume(Result.Error(it))
                        return@addOnCompleteListener
                    }
                    continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                }
            }
        }

    override suspend fun getUserEvent(userId: String): Result<String> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_USERS).document(userId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("getUserEvent => id=${task.result.id}, data=${task.result.data}")
                        val currentEventId = task.result.data?.get(FIELD_CURRENT_EVENT) as String
                        continuation.resume(Result.Success(currentEventId))
                    } else {
                        task.exception?.let {
                            Timber.d("getUserEvent => Get documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override fun getLiveEventId(userId: String): MutableLiveData<String> {
        val liveData = MutableLiveData<String>()
        FirebaseFirestore.getInstance().collection(PATH_USERS).document(userId)
            .addSnapshotListener { document, exception ->
                Timber.i("getLiveUser addSnapshotListener detect")
                val data = document?.toObject(User::class.java)
                if (data?.currentEvent == null) {
                    liveData.value = ""
                } else {
                    liveData.value = data.currentEvent
                }

                exception?.let {
                    Timber.d("getLiveUser => Get documents error=${it.message}")
                }
            }
        return liveData
    }

    override suspend fun getCurrentEvent(currentEventId: String): Result<Event> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_EVENTS).document(currentEventId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("getCurrentEvent => id=${task.result.id} + data=${task.result.data}")
                        val currentEvent = task.result.toObject(Event::class.java)!!
                        continuation.resume(Result.Success(currentEvent))
                    } else {
                        task.exception?.let {
                            Timber.d("getCurrentEvent => Get documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override suspend fun updateMyLocation(userId: String, myGeo: GeoPoint): Result<Boolean> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_USERS).document(userId)
                .update(FIELD_GEO_HASH, myGeo).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("updateMyLocation => Update user=${task.result}")
                        continuation.resume(Result.Success(true))
                    } else {
                        task.exception?.let {
                            Timber.d("updateMyLocation => Update documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override fun updateFriendsLocation(participantIds: List<String>): MutableLiveData<List<User>> {
        val liveData = MutableLiveData<List<User>>()
        val userList = mutableListOf<User>()
        FirebaseFirestore.getInstance().collection(PATH_USERS).whereIn(FIELD_ID, participantIds)
            .addSnapshotListener { documents, exception ->
                Timber.i("updateFriendsLocation addSnapshotListener detect")

                documents?.let {
                    userList.clear()
                    for (document in it.documents) {
                        val user = document?.toObject(User::class.java)
                        user?.let { userList.add(it) }
                    }
                }

                exception?.let {
                    Timber.d("updateFriendsLocation => Get documents error=${it.message}")
                }
                liveData.value = userList
            }
        return liveData
    }

    override suspend fun sendEvent(event: Event): Result<Boolean> =
        suspendCoroutine { continuation ->
            val events = FirebaseFirestore.getInstance().collection(PATH_EVENTS)
            val document = events.document()

            event.id = document.id

            document.set(event).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("sendEvent => event=${task.result}")
                    continuation.resume(Result.Success(true))
                } else {
                    task.exception?.let {
                        Timber.d("sendEvent => Set documents error=${it.message}")
                        continuation.resume(Result.Error(it))
                        return@addOnCompleteListener
                    }
                    continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                }
            }
        }

    override suspend fun finishEvent(userId: String): Result<Boolean> =
        suspendCoroutine { continuation ->
            val updates = hashMapOf<String, Any>(
                FIELD_CURRENT_EVENT to ""
            )

            FirebaseFirestore.getInstance().collection(PATH_USERS).document(userId).update(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("finishEvent => event=${task.result}")
                        continuation.resume(Result.Success(true))
                    } else {
                        task.exception?.let {
                            Timber.d("finishEvent => Update documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override suspend fun getChatRoom(userId: String): Result<List<ChatRoom>> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS)
                .whereArrayContains(FIELD_PARTICIPANTS, userId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val chatRoomList = mutableListOf<ChatRoom>()
                        for (document in task.result) {
                            Timber.d("getChatRoom => chatRoom id=${document.id}, data=${document.data}")
                            val chatRoom = document.toObject(ChatRoom::class.java)
                            chatRoomList.add(chatRoom)
                        }
                        continuation.resume(Result.Success(chatRoomList))
                    } else {
                        task.exception?.let {
                            Timber.d("getChatRoom => Get documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override suspend fun getUsersById(usersIds: List<String>): Result<List<User>> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_USERS).whereIn(FIELD_ID, usersIds)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userNameListWithIds = mutableMapOf<String, String>()
                        val userList = mutableListOf<User>()
                        for (document in task.result) {
                            Timber.d("getUserById => user id=${document.id}, data=${document.data}")

                            userNameListWithIds[document.data[FIELD_ID] as String] =
                                document.data[FIELD_NAME] as String

                            val users = document.toObject(User::class.java)
                            userList.add(users)
                        }
                        continuation.resume(Result.Success(userList))
                    } else {
                        task.exception?.let {
                            Timber.d("getUserById => Get documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override suspend fun getMessages(chatRoomId: String, userId: String): Result<List<Message>> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS).document(chatRoomId)
                .collection(FIELD_MESSAGES).orderBy(FIELD_TIME)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val dataList = mutableListOf<Message>()
                        for (document in task.result) {
                            Timber.d("getMessages => message id=${document.id}, data=${document.data}")
                            val message = document.toObject(Message::class.java)
                            dataList.add(message)
                        }
                        continuation.resume(Result.Success(dataList))
                    } else {
                        task.exception?.let {
                            Timber.d("getMessages => Get documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override suspend fun sendMessages(
        senderId: String,
        text: String,
        time: Timestamp
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }
}