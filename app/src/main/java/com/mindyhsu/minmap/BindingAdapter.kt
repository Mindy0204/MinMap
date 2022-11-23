package com.mindyhsu.minmap

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mindyhsu.minmap.network.LoadApiStatus

fun bindImage(imageView: ImageView, imageUrl: String?) {
    imageUrl?.let {
        val imgUri = it.toUri().buildUpon().scheme("https").build() // handle null in uris
        Glide.with(imageView.context)
            .load(imgUri)
            .apply(RequestOptions().placeholder(R.mipmap.icon_profile).error(R.mipmap.icon_profile))
            .into(imageView)
    }
}

fun bindApiStatus(view: ProgressBar, status: LoadApiStatus?) {
    when (status) {
        LoadApiStatus.LOADING -> view.visibility = View.VISIBLE
        LoadApiStatus.DONE, LoadApiStatus.ERROR -> view.visibility = View.GONE
        else -> view.visibility = View.VISIBLE
    }
}