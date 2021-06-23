package com.jyotishapp.jyotishi

import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jyotishapp.jyotishi.Common.Constant
import com.jyotishapp.jyotishi.Common.Constant.DEFAULT_USER_URL
import com.jyotishapp.jyotishi.LiveStreamViewModel.RecyclerViewState.Initial
import com.jyotishapp.jyotishi.LiveStreamViewModel.RecyclerViewState.StopScroll
import com.jyotishapp.jyotishi.Models.ActiveUser
import com.jyotishapp.jyotishi.Models.ChatUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LiveStreamViewModel : ViewModel() {

    private val _usersList: MutableStateFlow<List<Int>> = MutableStateFlow(listOf())
    val usersList: StateFlow<List<Int>> get() = _usersList

    private val _appInitialised: MutableLiveData<Boolean> = MutableLiveData()
    val appInitialised: LiveData<Boolean> get() = _appInitialised

    private val _chatsList: MutableStateFlow<List<ChatUser>> = MutableStateFlow(listOf())
    val chatsList: StateFlow<List<ChatUser>> get() = _chatsList

    private val _activeUsersList: MutableStateFlow<List<ActiveUser>> = MutableStateFlow(listOf())
    val activeUsersList: StateFlow<List<ActiveUser>> get() = _activeUsersList

    private val _currentUserName: MutableStateFlow<String> = MutableStateFlow("")
    val currentUserName: StateFlow<String> get() = _currentUserName

    private val _recyclerViewStatus: MutableStateFlow<RecyclerViewState> = MutableStateFlow(Initial)
    val recyclerViewStatus: StateFlow<RecyclerViewState> get() = _recyclerViewStatus

    private val _giftAmount: MutableLiveData<Int> = MutableLiveData()
    val giftAmount: LiveData<Int> get() = _giftAmount

    private var dbRef: DatabaseReference? = null
    private var dbRefKey: String? = null

    sealed class RecyclerViewState {

        object Initial: RecyclerViewState()
        object StopScroll: RecyclerViewState()
    }

    fun reset() {

        val list = _usersList.value.toMutableList()
        list.clear()
        _usersList.value = list

        val list2 = _chatsList.value.toMutableList()
        list2.clear()
        _chatsList.value = list2

        val list3 = _activeUsersList.value.toMutableList()
        list3.clear()
        _activeUsersList.value = list3

        _appInitialised.value = false
        _recyclerViewStatus.value = Initial
        dbRef = null
    }

    fun setAppInitialised() {
        _appInitialised.value = true
    }

    fun setProfile() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->

            FirebaseDatabase.getInstance().getReference("Users").child(uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(Users::class.java)
                            user?.let {
                                _currentUserName.value = it.name
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {

                        }
                    })
        }
    }

    fun setGiftAmount(amount: Int) {

        _giftAmount.value = amount
    }

    fun markChatsAsStopped() {

        _recyclerViewStatus.value = StopScroll
    }

    fun markChatsAsInitial() {

        _recyclerViewStatus.value = Initial
    }

    fun setActive() =
            viewModelScope.launch{

        currentUserName.collect {  userName ->

            if(userName.isNotEmpty()) {

                val activeUser = ActiveUser(
                        userName,
                        DEFAULT_USER_URL
                )

                dbRef = FirebaseDatabase.getInstance().getReference("StreamActiveUsers")

                dbRefKey = dbRef?.push()?.key

                dbRefKey?.let { key ->

                    dbRef?.child(key)?.setValue(activeUser)
                }
            }
        }
    }

    fun setInActive() {

        dbRefKey?.let { key ->

            dbRef?.child(key)?.removeValue()
        }
    }

    fun addUser(userId: Int) {
        _usersList.value += userId
    }

    fun removeUser(userId: Int) {
        _usersList.value -= userId
    }

    fun addChat(chat: ChatUser) {

        _chatsList.value += chat
    }

    fun addActiveUser(user: ActiveUser) {

        _activeUsersList.value += user
    }

    fun removeActiveUser(user: ActiveUser) {

        _activeUsersList.value -= user
    }
}