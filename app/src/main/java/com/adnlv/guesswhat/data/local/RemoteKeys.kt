package com.adnlv.guesswhat.data.local

import androidx.room.Entity

@Entity(tableName = "remote_keys", primaryKeys = ["feedKey", "articleUrl"])
data class RemoteKeys(val feedKey: String, val articleUrl: String, val prevKey: Int?, val nextKey: Int?)
