package com.awper.lightscore

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val EasternZone = ZoneId.of("America/New_York")
private val ScheduleDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun mlbSlateDate(now: ZonedDateTime = ZonedDateTime.now(EasternZone)): String {
    val easternNow = now.withZoneSameInstant(EasternZone)
    val slateDay = if (easternNow.toLocalTime().isBefore(LocalTime.of(7, 0))) {
        easternNow.toLocalDate().minusDays(1)
    } else {
        easternNow.toLocalDate()
    }
    return slateDay.format(ScheduleDateFormatter)
}
