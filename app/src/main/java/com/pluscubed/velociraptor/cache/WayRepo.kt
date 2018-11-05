package com.pluscubed.velociraptor.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WayRepo(private val wayDao: WayDao) {
    suspend fun put(ways: List<Way>): List<Long> = withContext(Dispatchers.IO) {
        wayDao.put(ways)
    }

    suspend fun selectByCoord(lat: Double, coslat2: Double, lon: Double): List<Way> = withContext(Dispatchers.IO) {
        wayDao.selectByCoord(lat, coslat2, lon)
    }

    suspend fun cleanup(timestamp: Long) = withContext(Dispatchers.IO) {
        wayDao.cleanup(timestamp)
    }
}