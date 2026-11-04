package com.adnlv.guesswhat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKeys: List<RemoteKeys>)

    @Query("SELECT * FROM remote_keys WHERE feedKey = :feedKey AND articleUrl = :url")
    suspend fun getRemoteKeys(feedKey: String, url: String): RemoteKeys?

    @Query("DELETE FROM remote_keys WHERE feedKey = :feedKey")
    suspend fun deleteFeedKeys(feedKey: String)
}
