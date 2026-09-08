package com.codecraft.contactvault.data.database.repository

import com.codecraft.contactvault.data.database.dao.RecentlyViewedDao
import com.codecraft.contactvault.data.database.entity.RecentlyViewedEntity
import com.codecraft.contactvault.domain.repository.RecentlyViewedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRecentlyViewedRepository(
    private val recentlyViewedDao: RecentlyViewedDao
) : RecentlyViewedRepository {

    override fun getRecentlyViewedContactIds(limit: Int): Flow<List<Long>> {
        return recentlyViewedDao.getRecentlyViewed(limit).map { list ->
            list.map { it.contactId }
        }
    }

    override suspend fun markContactViewed(contactId: Long) {
        recentlyViewedDao.insertRecentlyViewed(
            RecentlyViewedEntity(contactId = contactId, viewedAt = System.currentTimeMillis())
        )
    }
}
