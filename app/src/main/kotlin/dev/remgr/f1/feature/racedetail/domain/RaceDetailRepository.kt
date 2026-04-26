package dev.remgr.f1.feature.racedetail.domain

import dev.remgr.f1.feature.racedetail.domain.model.RaceDetailModel

interface RaceDetailRepository {
    suspend fun getRaceDetail(meetingKey: Int, sessionKey: Int): RaceDetailModel
}
