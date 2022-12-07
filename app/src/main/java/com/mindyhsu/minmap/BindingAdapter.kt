package com.mindyhsu.minmap

import android.widget.ImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

fun bindImage(imageView: ImageView, imageUrl: String?) {
    imageUrl?.let {
        val imgUri = it.toUri().buildUpon().scheme("https").build() // handle null in uris
        Glide.with(imageView.context)
            .load(imgUri)
            .apply(RequestOptions().placeholder(R.mipmap.icon_profile).error(R.mipmap.icon_profile))
            .into(imageView)
    }
}
