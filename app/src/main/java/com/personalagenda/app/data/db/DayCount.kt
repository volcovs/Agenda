package com.personalagenda.app.data.db

/** Projection: number of events on a given day (epoch-day). */
data class DayCount(val day: Long, val count: Int)
