package com.mindyhsu.minmap.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.map.SendInvitationViewModel

class SendInvitationViewModelFactory(
    private val repository: MinMapRepository,
    private val eventLocation: LatLng,
    private val eventLocationName: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SendInvitationViewModel::class.java)) {
            return SendInvitationViewModel(repository, eventLocation, eventLocationName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}