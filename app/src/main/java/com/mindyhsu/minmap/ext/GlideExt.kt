package com.mindyhsu.minmap.ext

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mindyhsu.minmap.R

fun ImageView.glide(imageUrl: String) {
    Glide.with(this)
        .load(imageUrl)
        .apply(RequestOptions().placeholder(R.mipmap.icon_profile).error(R.mipmap.icon_profile))
        .into(this)
}
