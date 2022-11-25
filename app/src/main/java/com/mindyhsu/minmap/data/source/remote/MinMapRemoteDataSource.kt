package com.mindyhsu.minmap.data.source.remote

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.*
import com.mindyhsu.minmap.data.MapDirection
import com.mindyhsu.minmap.data.source.MinMapDataSource
import com.mindyhsu.minmap.main.*
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
    private const val FIELD_EVENT_ID = "eventId"
    private const val FIELD_NAME = "name"
    private const val FIELD_FRIENDS = "friends"
    private const val FIELD_MESSAGES = "messages"
    private const val FIELD_TIME = "time"
    private const val FIELD_SENDER_ID = "senderId"
    private const val FIELD_LAST_MESSAGES = "lastMessage"
    private const val FIELD_LAST_UPDATE = "lastUpdate"
    private const val FIELD_STATUS = "status"

    private val sharedPreferencesChatRoom =
        MinMapApplication.instance.getSharedPreferences(KEY_CHAT_ROOM, Context.MODE_PRIVATE)
    private var chatRoomNum: Int?
        set(value) {
            if (value != null) {
                sharedPreferencesChatRoom.edit().putInt(KEY_CHAT_ROOM, value).apply()
            }
        }
        get() {
            return sharedPreferencesChatRoom.getInt(KEY_CHAT_ROOM, 0)
        }

    private var keyChatRoomId = ""
    private val sharedPreferencesMessage =
        MinMapApplication.instance.getSharedPreferences(KEY_MESSAGE, Context.MODE_PRIVATE)
    private var messageNum: Int?
        set(value) {
            if (value != null) {
                sharedPreferencesMessage.edit().putInt(keyChatRoomId, value).apply()
            }
        }
        get() {
            return sharedPreferencesMessage.getInt(keyChatRoomId, 0)
        }

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
                MinMapApi.retrofitService.getDirection(startLocation, endLocation, mode, apiKey)

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

                val document = transaction.get(userRef)
                if (document.data == null) {
                    transaction.set(userRef, User(id = uid, image = image, name = name))
                    Timber.d("setUser => After set user=${document.data}")
                } else {
                    Timber.d("setUser => User=${document.data}")
                }
            }.addOnCompleteListener { task ->
                Timber.i("setUser =>addOnCompleteListener")

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

                if (data?.currentEvent == "") {
                    liveData.value = ""
                } else {
                    liveData.value = data?.currentEvent

                    // New event notification
                    Intent().also { intent ->
                        intent.action = EVENT_INTENT_FILTER
                        MinMapApplication.instance.sendBroadcast(
                            intent.putExtra(KEY_EVENT, KEY_EVENT)
                        )
                    }
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

    override fun updateFriendLocation(participantIds: List<String>): MutableLiveData<List<User>> {
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

    override suspend fun getFriend(userId: String): Result<List<String>> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_USERS).document(userId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("getFriend => id=${task.result.id}, data=${task.result.data}")
                        val friendList = task.result.data?.get(FIELD_FRIENDS) as List<String>
                        val friends = friendList.filter { it != userId }
                        continuation.resume(Result.Success(friends))
                    } else {
                        task.exception?.let {
                            Timber.d("getFriend => Get documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override suspend fun sendEvent(event: Event): Result<String> =
        suspendCoroutine { continuation ->
            val events = FirebaseFirestore.getInstance().collection(PATH_EVENTS)
            val document = events.document()

            event.id = document.id

            document.set(event).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("sendEvent => event=${task.result}")
                    continuation.resume(Result.Success(event.id))
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

    override suspend fun updateUserCurrentEvent(
        userId: List<String>,
        currentEventId: String
    ): Result<Boolean> = suspendCoroutine { continuation ->
        FirebaseFirestore.getInstance().runBatch { batch ->
            for (id in userId) {
                val docRef = FirebaseFirestore.getInstance().collection(PATH_USERS).document(id)
                batch.update(docRef, FIELD_CURRENT_EVENT, currentEventId)
            }
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.d("sendEventToUser => event=${task.result}")
                continuation.resume(Result.Success(true))
            } else {
                task.exception?.let {
                    Timber.d("sendEventToUser => Update documents error=${it.message}")
                    continuation.resume(Result.Error(it))
                    return@addOnCompleteListener
                }
                continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
            }
        }
    }

    override suspend fun updateChatRoomCurrentEvent(
        participants: List<String>,
        currentEventId: String
    ): Result<String> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS)
                .whereEqualTo(FIELD_PARTICIPANTS, participants)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        Timber.d("updateChatRoomEvent => event=${task.result}")
                        val snapshot = task.result
                        if (snapshot.isEmpty) {
                            val chatRoomRef =
                                FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS)
                            val document = chatRoomRef.document()
                            val newChatRoom = ChatRoom(
                                eventId = currentEventId,
                                id = document.id,
                                participants = participants
                            )
                            document.set(newChatRoom)
                            continuation.resume(Result.Success(document.id))
                        } else {
                            continuation.resume(Result.Success(snapshot.first().id))
                            FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS)
                                .document(snapshot.first().id)
                                .update(hashMapOf<String, Any>("eventId" to currentEventId))
                        }
                    } else {
                        task.exception?.let {
                            Timber.d("updateChatRoomEvent => Update documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override suspend fun finishEvent(
        userId: String,
        eventId: String,
        chatRoomId: String
    ): Result<Boolean> =
        suspendCoroutine { continuation ->
            val userUpdate = hashMapOf<String, Any>(
                FIELD_CURRENT_EVENT to ""
            )

            // Clean user's current event
            FirebaseFirestore.getInstance().collection(PATH_USERS).document(userId)
                .update(userUpdate)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.i("finishEvent => Finish update user current event")

                        // Count how many participant arrive
                        val eventRef = FirebaseFirestore.getInstance().collection(PATH_EVENTS)
                            .document(eventId)
                        FirebaseFirestore.getInstance().runTransaction { transaction ->
                            val document = transaction.get(eventRef)
                            document.data?.let {

                                // Not all participants arrive
                                if (it[FIELD_STATUS] != ((it[FIELD_PARTICIPANTS] as List<String>).size - 1).toLong()) {
                                    val eventUpdate = hashMapOf<String, Any>(
                                        FIELD_STATUS to (it[FIELD_STATUS] as Long) + 1
                                    )
                                    transaction.update(eventRef, eventUpdate)
                                } else {
                                    // All participants arrive
                                    Timber.d("finishEvent => Current event status=${it[FIELD_STATUS]}")

                                    // Clean chat room eventId
                                    val chatRoomUpdate = hashMapOf<String, Any>(
                                        FIELD_EVENT_ID to ""
                                    )
                                    FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS)
                                        .document(chatRoomId)
                                        .update(chatRoomUpdate)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Timber.i("finishEvent => Finish update chat room event id")
                                            } else {
                                                task.exception?.let {
                                                    Timber.d("finishEvent => Update chat room event id error=${it.message}")
                                                    continuation.resume(Result.Error(it))
                                                    return@addOnCompleteListener
                                                }
                                                continuation.resume(
                                                    Result.Fail(
                                                        MinMapApplication.instance.getString(
                                                            R.string.you_know_nothing
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                }
                            }
                        }.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Timber.i("finishEvent => Finish update event status")
                                continuation.resume(Result.Success(true))

                            } else {
                                task.exception?.let {
                                    Timber.d("finishEvent => Update event status error=${it.message}")
                                    continuation.resume(Result.Error(it))
                                    return@addOnCompleteListener
                                }
                                continuation.resume(
                                    Result.Fail(
                                        MinMapApplication.instance.getString(
                                            R.string.you_know_nothing
                                        )
                                    )
                                )
                            }
                        }


                    } else {
                        task.exception?.let {
                            Timber.d("finishEvent => Update user current event error=${it.message}")
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

    override suspend fun getChatRoomByCurrentEventId(currentEventId: String): Result<String> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS)
                .whereEqualTo(FIELD_EVENT_ID, currentEventId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        var chatRoomId = ""
                        task.result.documents[0]?.let {
                            Timber.d("getChatRoomByCurrentEventId => chatRoom id=${it.id}, data=${it.data}")
                            chatRoomId = it.id
                        }
                        continuation.resume(Result.Success(chatRoomId))
                    } else {
                        task.exception?.let {
                            Timber.d("getChatRoomByCurrentEventId => Get documents error=${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                    }
                }
        }

    override fun getLiveChatRoom(userId: String): MutableLiveData<List<ChatRoom>> {
        val liveData = MutableLiveData<List<ChatRoom>>()
        val chatRoomList = mutableListOf<ChatRoom>()
        FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS)
            .whereArrayContains(FIELD_PARTICIPANTS, userId)
            .addSnapshotListener { documents, exception ->
                Timber.i("getLiveChatRoom addSnapshotListener detect")

                if (chatRoomNum == 0) {
                    chatRoomNum = documents?.size()
                } else {
                    // There's new chat room
                    if (chatRoomNum != documents?.size() && chatRoomNum!! < documents?.size()!!) {
                        Intent().also { intent ->
                            intent.action = CHAT_ROOM_INTENT_FILTER
                            MinMapApplication.instance.sendBroadcast(
                                intent.putExtra(
                                    KEY_CHAT_ROOM,
                                    (documents.size().minus(chatRoomNum!!)).toString()
                                )
                            )
                        }
                        chatRoomNum = documents.size()
                    }
                }

                documents?.let {
                    chatRoomList.clear()
                    for (document in it.documents) {
                        val chatRoom = document?.toObject(ChatRoom::class.java)
                        chatRoom?.let { chatRoomList.add(it) }
                    }
                }

                exception?.let {
                    Timber.d("getLiveChatRoom => Get documents error=${it.message}")
                }
                liveData.value = chatRoomList
            }
        return liveData
    }

    override suspend fun getUserById(usersIds: List<String>): Result<List<User>> =
        suspendCoroutine { continuation ->
            // There's limit (10 queries) of Firebase whereIn filter so query all users now

            // Future Upgrade
//            FirebaseFirestore.getInstance().collection(PATH_USERS).whereIn(FIELD_ID, usersIds)

            FirebaseFirestore.getInstance().collection(PATH_USERS)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userNameListWithIds = mutableMapOf<String, String>()
                        val userList = mutableListOf<User>()
                        for (document in task.result) {

                            // If no limit in the future, we could deprecate these logics
                            for (userId in usersIds) {
                                if (document.id == userId) {
                                    Timber.d("getUserById => user id=${document.id}, data=${document.data}")

                                    userNameListWithIds[document.data[FIELD_ID] as String] =
                                        document.data[FIELD_NAME] as String

                                    val users = document.toObject(User::class.java)
                                    userList.add(users)
                                }
                            }

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

    override fun getMessage(
        chatRoomId: String,
        userId: String
    ): MutableLiveData<List<Message>> {
        val liveData = MutableLiveData<List<Message>>()
        val messageList = mutableListOf<Message>()
        FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS).document(chatRoomId)
            .collection(FIELD_MESSAGES).orderBy(FIELD_TIME)
            .addSnapshotListener { documents, exception ->
                Timber.i("getMessage addSnapshotListener detect")

                var currentMessageNum = 0
                documents?.let {
                    // The number of message which send from friends
                    for (document in it) {
                        if (document.data[FIELD_SENDER_ID] != userId) {
                            currentMessageNum += 1
                        }
                    }
                }

                if (sharedPreferencesMessage.all[chatRoomId] == null) {
                    keyChatRoomId = chatRoomId
                    messageNum = currentMessageNum
                } else {
                    val messageStored = sharedPreferencesMessage.all[chatRoomId] as Int
                    if (currentMessageNum > messageStored) {
                        // Send broadcast
                        Intent().also { intent ->
                            intent.action = MESSAGE_INTENT_FILTER
                            MinMapApplication.instance.sendBroadcast(
                                intent.putExtra(
                                    KEY_MESSAGE,
                                    (currentMessageNum.minus(messageStored).toString())
                                )
                            )
                        }
                        keyChatRoomId = chatRoomId
                        messageNum = currentMessageNum
                    }
                }

                documents?.let {
                    messageList.clear()
                    for (document in it.documents) {
                        val message = document?.toObject(Message::class.java)
                        message?.let { messageList.add(it) }
                    }
                }
                liveData.value = messageList

                exception?.let {
                    Timber.d("getMessage => Get documents error=${it.message}")
                }
            }
        return liveData
    }

    override suspend fun sendMessage(chatRoomId: String, message: Message): Result<Boolean> =
        suspendCoroutine { continuation ->
            var isChatRoomInfoUpdate = false
            FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS).document(chatRoomId).update(
                mapOf(
                    FIELD_LAST_MESSAGES to message.text,
                    FIELD_LAST_UPDATE to message.time
                )
            ).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.i("sendMessages")
                    isChatRoomInfoUpdate = true
                } else {
                    task.exception?.let {
                        Timber.d("sendMessages => Set documents error=${it.message}")
                        continuation.resume(Result.Error(it))
                        return@addOnCompleteListener
                    }
                    continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                }
            }

            val messages =
                FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS).document(chatRoomId)
                    .collection(FIELD_MESSAGES)
            val document = messages.document()
            message.id = document.id
            document.set(message).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.i("sendMessages")
                    if (isChatRoomInfoUpdate) {
                        continuation.resume(Result.Success(true))
                    }
                } else {
                    task.exception?.let {
                        Timber.d("sendMessages => Set documents error=${it.message}")
                        continuation.resume(Result.Error(it))
                        return@addOnCompleteListener
                    }
                    continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                }
            }
        }

    override suspend fun setFriend(userId: String, friendId: String): Result<String> =
        suspendCoroutine { continuation ->
            var isUserFriendUpdate = false

            // Update my friend list
            FirebaseFirestore.getInstance().collection(PATH_USERS).document(userId).update(
                mapOf(FIELD_FRIENDS to FieldValue.arrayUnion(friendId))
            ).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.i("setFriend => Set my friend")
                    isUserFriendUpdate = true
                } else {
                    task.exception?.let {
                        Timber.d("setFriend => Set documents error=${it.message}")
                        continuation.resume(Result.Error(it))
                        return@addOnCompleteListener
                    }
                    continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                }
            }

            // Update friend's friend list
            FirebaseFirestore.getInstance().collection(PATH_USERS).document(friendId).update(
                mapOf(FIELD_FRIENDS to FieldValue.arrayUnion(userId))
            ).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.i("setFriend")
                    isUserFriendUpdate = true
                } else {
                    task.exception?.let {
                        Timber.d("setFriend => Set documents error=${it.message}")
                        isUserFriendUpdate = false
                        continuation.resume(Result.Error(it))
                        return@addOnCompleteListener
                    }
                    continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                }
            }

            // Create a chat room
            val document = FirebaseFirestore.getInstance().collection(PATH_CHAT_ROOMS).document()
            val chatRoom = ChatRoom(id = document.id, participants = listOf(userId, friendId))

            document.set(chatRoom).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("setFriend create chat room => chatRoom=${task.result}")
                    if (isUserFriendUpdate) {
                        Timber.d("setFriend create chat room => chatRoom id=${document.id}")
                        continuation.resume(Result.Success(document.id))
                    }
                } else {
                    task.exception?.let {
                        Timber.d("setFriend create chat room => Set documents error=${it.message}")
                        continuation.resume(Result.Error(it))
                        return@addOnCompleteListener
                    }
                    continuation.resume(Result.Fail(MinMapApplication.instance.getString(R.string.you_know_nothing)))
                }
            }
        }
}