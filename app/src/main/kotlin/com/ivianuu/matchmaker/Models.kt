/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.matchmaker

import com.ivianuu.essentials.android.prefs.DataStoreModule
import com.ivianuu.injekt.Provide
import kotlinx.serialization.Serializable

@Serializable data class MatchMakerPrefs(
  val teamSize: Int = 2,
  val preferNewPlayers: Boolean = true,
  val distinctTeams: Boolean = true,
  val players: Set<String> = emptySet(),
  val currentMatch: Match? = null,
  val previousMatches: List<Match> = emptyList()
) {
  companion object {
    @Provide val prefModule = DataStoreModule("match_maker_prefs") { MatchMakerPrefs() }
  }
}

fun MatchMakerPrefs.resetMatches() = copy(
  currentMatch = null,
  previousMatches = emptyList()
)

@Serializable data class Match(
  val team1: List<String>,
  val team2: List<String>,
  val starterTeam: Int
)
