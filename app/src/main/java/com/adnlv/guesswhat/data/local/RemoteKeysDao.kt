package com.adnlv.guesswhat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: List<RemoteKeys>)

    @Query("SELECT * FROM remote_keys WHERE articleUrl = :url")
    suspend fun getRemoteKeys(url: String): RemoteKeys?

    @Query(
        """
        DELETE FROM remote_keys
        WHERE (:category IS NULL AND category IS NULL AND searchQuery IS NULL)
        OR (category = :category AND searchQuery IS NULL)
        """
    )
    suspend fun deleteHeadlinesKeys(category: String?)

    @Query("DELETE FROM remote_keys WHERE searchQuery = :query")
    suspend fun deleteSearchKeys(query: String)
}
