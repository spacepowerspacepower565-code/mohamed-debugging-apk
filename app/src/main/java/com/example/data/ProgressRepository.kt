package com.example.data

import kotlinx.coroutines.flow.Flow

class ProgressRepository(private val dao: UserProgressDao) {
    val progressFlow: Flow<UserProgress?> = dao.getProgressFlow()

    suspend fun getProgressOnce(): UserProgress {
        val existing = dao.getProgressOnce()
        if (existing == null) {
            val initial = UserProgress()
            dao.insertProgress(initial)
            return initial
        }
        return existing
    }

    suspend fun saveProgress(progress: UserProgress) {
        dao.insertProgress(progress)
    }

    suspend fun updateProgress(progress: UserProgress) {
        dao.updateProgress(progress)
    }
}
