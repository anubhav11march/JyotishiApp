package com.jyotishapp.jyotishi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LiveStreamViewModel: ViewModel() {

    private val _usersList: MutableStateFlow<List<Int>> = MutableStateFlow(listOf())
    val usersList: StateFlow<List<Int>> get() = _usersList

    fun reset() {
        val list = _usersList.value.toMutableList()
        list.clear()
        _usersList.value = list
    }

    fun addUser(userId: Int) {
        _usersList.value += userId
    }

    fun removeUser(userId: Int) {
        _usersList.value -= userId
    }
}