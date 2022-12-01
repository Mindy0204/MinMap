package com.mindyhsu.minmap.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.Result
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(private val repository: MinMapRepository) : ViewModel() {

    lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    val GOOGLE_SING_IN_RESULT = 1
    private lateinit var accountResult: GoogleSignInAccount
    private var hasUserData = MutableLiveData<Boolean>()

    private val _isSignIn = MutableLiveData<Boolean>()
    val isSignIn: LiveData<Boolean>
        get() = _isSignIn

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    init {
        initGoogleSignInAndFirebaseAuth()
    }

    private fun initGoogleSignInAndFirebaseAuth() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(MinMapApplication.instance.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(MinMapApplication.instance, gso)

        val account = GoogleSignIn.getLastSignedInAccount(MinMapApplication.instance)
        _isSignIn.value = account != null

        auth = Firebase.auth
    }


    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            accountResult = completedTask.getResult(ApiException::class.java)

            // Set Firebase auth
            accountResult.idToken?.let { signInGoogleWithFirebaseAuth(it) }
        } catch (e: ApiException) {
            Timber.i("sing in exception=${e.message}")
        }
    }

    private fun signInGoogleWithFirebaseAuth(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.i("signInGoogleWithFirebaseAuth success")

                auth.currentUser?.let { user ->
                    UserManager.id = user.uid
                    UserManager.image = accountResult.photoUrl.toString()
                    UserManager.name = accountResult.displayName
                    accountResult.displayName?.let {
                        getFCMToken()
                    }
                }
            } else {
                task.exception?.let {
                    Timber.d("signInGoogleWithFirebaseAuth => Request error=${it.message}")
                }
            }
        }
    }

    private fun getFCMToken() {
        coroutineScope.launch {
            val fcmToken = when (val result = repository.getFCMToken()) {
                is Result.Success -> {
                    error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    ""
                }
                is Result.Error -> {
                    error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    ""
                }
                else -> {
                    error.value =
                        MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                    ""
                }
            }
            setUser(
                id = UserManager.id ?: "",
                image = accountResult.photoUrl.toString(),
                name = UserManager.name ?: "",
                fcmToken = fcmToken
            )
        }
    }

    private fun setUser(id: String, image: String, name: String, fcmToken: String) {
        coroutineScope.launch {
            val result = repository.setUser(id, image, name, fcmToken)

            hasUserData.value = when (result) {
                is Result.Success -> {
                    error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    null
                }
                is Result.Error -> {
                    error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    null
                }
                else -> {
                    error.value =
                        MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                    null
                }
            }
            _isSignIn.value = UserManager.id != null
        }
    }
}