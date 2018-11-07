package com.pluscubed.velociraptor.api.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(ways: List<Way>): List<Long>

    // https://stackoverflow.com/a/39298241
    @Query(
        """
        SELECT * FROM way
        WHERE clat between :lat - 0.01 and :lat + 0.01 and clon between :lon - 0.01 and :lon + 0.01
        ORDER BY ((:lat - clat)*(:lat - clat) + ((:lon - clon)*(:lon - clon)* :coslat2)) ASC
        LIMIT 10
        """
    )
    fun selectByCoord(lat: Double, coslat2: Double, lon: Double): List<Way>

    @Query(
        """
        DELETE FROM way
        WHERE :timestamp - timestamp >  604800000;
    """
    )
    fun cleanup(timestamp: Long)

    @Query("DELETE FROM way")
    fun clear()
}