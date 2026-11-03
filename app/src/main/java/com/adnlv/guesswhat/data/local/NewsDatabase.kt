package com.adnlv.guesswhat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ArticleEntity::class, RemoteKeys::class],
    version = 2,
    exportSchema = false
)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun remoteKeysDao(): RemoteKeysDao
}
