package com.adnlv.guesswhat.model

import androidx.annotation.StringRes
import com.adnlv.guesswhat.R

enum class NewsCategory(val apiValue: String, @StringRes val titleRes: Int) {
    BUSINESS("business", R.string.category_business),
    ENTERTAINMENT("entertainment", R.string.category_entertainment),
    GENERAL("general", R.string.category_general),
    HEALTH("health", R.string.category_health),
    SCIENCE("science", R.string.category_science),
    SPORTS("sports", R.string.category_sports),
    TECHNOLOGY("technology", R.string.category_technology)
    ;

    companion object {
        val DEFAULT = TECHNOLOGY
    }
}
