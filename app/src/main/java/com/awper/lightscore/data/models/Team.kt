package com.awper.lightscore.data.models

import kotlinx.serialization.Serializable

@Serializable data class TeamInfo(val id: Int, val name: String, val abbreviation: String, val teamName: String? = null)
