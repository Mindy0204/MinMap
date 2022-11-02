package com.mindyhsu.minmap

import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.data.Event
import java.text.SimpleDateFormat

class CheckEventViewModel(private val eventDetail: Event) : ViewModel() {

    val eventLocation = eventDetail.place
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val eventTime = dateFormat.format(eventDetail.time.toDate())

}