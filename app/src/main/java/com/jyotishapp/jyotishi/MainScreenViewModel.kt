package com.jyotishapp.jyotishi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainScreenViewModel: ViewModel() {

    private val _streamStatus: MutableStateFlow<StreamStatus> = MutableStateFlow(StreamStatus.None)
    val streamStatus: StateFlow<StreamStatus> get() = _streamStatus

    sealed class StreamStatus{

        object None: StreamStatus()
        object Started: StreamStatus()
        data class StillInStream(val message: String? = null): StreamStatus()
        data class NotStarted(val message: String? = null): StreamStatus()
    }

    fun setStreamStatus(status: StreamStatus) {

        when (status) {

            is StreamStatus.None -> Unit

            is StreamStatus.Started -> {

                _streamStatus.value = StreamStatus.Started
            }

            is StreamStatus.StillInStream -> {

                _streamStatus.value = StreamStatus.StillInStream("Please try joining again soon!")
            }

            is StreamStatus.NotStarted -> {

                _streamStatus.value = StreamStatus.NotStarted("No ongoing livestream, please try later!")
            }
        }
    }
}