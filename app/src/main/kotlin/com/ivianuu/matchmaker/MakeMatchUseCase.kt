/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.matchmaker

import com.ivianuu.essentials.ResourceProvider
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.logging.Logger
import com.ivianuu.essentials.logging.log
import com.ivianuu.essentials.time.Clock
import com.ivianuu.essentials.util.Toaster
import com.ivianuu.essentials.util.showToast
import com.ivianuu.injekt.Provide
import kotlinx.coroutines.flow.first
import kotlin.math.max
import kotlin.random.Random

fun interface MakeMatchUseCase : suspend () -> Unit

@Provide fun makeMatchUseCase(
  clock: Clock,
  logger: Logger,
  pref: DataStore<MatchMakerPrefs>,
  resourceProvider: ResourceProvider,
  toaster: Toaster
) = MakeMatchUseCase {
  val prefs = pref.data.first()

  val combinedPlayerSize = prefs.teamSize * 2

  val previousMatches = listOfNotNull(prefs.currentMatch) + prefs.previousMatches

  log { "make match p $previousMatches s ${prefs.teamSize}" }

  // check if we have enough players for the team size
  if (prefs.players.size < combinedPlayerSize) {
    showToast("Not enough players!")
    pref.updateData {
      copy(
        currentMatch = null,
        previousMatches = previousMatches
      )
    }
    return@MakeMatchUseCase
  }

  val playCount = prefs.players
    .associateWith { player ->
      previousMatches.count { match ->
        player in match.team1 || player in match.team2
      }
    }

  log { "play count $playCount" }

  val maxPlays = playCount.maxByOrNull { it.value }?.value ?: 0

  log { "max plays $maxPlays" }

  val lastOccurringMatchIndices = prefs.players
    .associateWith { player ->
      previousMatches.indexOfFirst {
        player in it.team1 || player in it.team2
      }.takeIf { it != -1 } ?: if (previousMatches.isNotEmpty()) previousMatches.size else -1
    }

  log { "last occurring match indices $lastOccurringMatchIndices" }

  val recencyFactors = prefs.players
    .associateWith { player ->
      val lastOccurringMatchIndex = lastOccurringMatchIndices[player]!!

      when (lastOccurringMatchIndex) {
        -1 -> 0
        0 -> -3 * (lastOccurringMatchIndices.maxByOrNull { it.value }?.value ?: 1)
        else -> max(
          0,
          4 * lastOccurringMatchIndex
        )
      }
    }

  log { "recency factors $recencyFactors" }

  val playersForTheNextMatch = buildList<String> {
    prefs.players.shuffled(Random(clock().inWholeMilliseconds)).forEach { player ->
      repeat(maxOf(1, 10 + recencyFactors[player]!!)) {
        add(player)
      }
    }
  }
    .also {
      log { "player pool count ${it.sortedBy { it }.groupBy { it }.mapValues { it.value.size }}" }
    }
    .shuffled(Random(clock().inWholeMilliseconds))
    .distinct()
    .take(combinedPlayerSize)

  log { "players for the next match $playersForTheNextMatch" }

  val teamCounts = buildMap {
    previousMatches.forEach { match ->
      fun putIfNeeded(team: List<String>) {
        if (team.all { it in playersForTheNextMatch }) {
          val teamKey = team.sorted().joinToString()
          put(teamKey, getOrElse(teamKey) { 0 } + 1)
        }
      }

      putIfNeeded(match.team1)
      putIfNeeded(match.team2)
    }
  }.toList().sortedByDescending { it.second }.toMap()

  log { "team count $teamCounts" }

  val allPossibleTeams = playersForTheNextMatch
    .combinations(prefs.teamSize)
    .map { it.sorted() }
    .distinct()

  log { "all possible teams $allPossibleTeams" }

  val teamCountFactor = 2f

  val possibleTeamsForTheNextMatch = buildList<List<String>> {
    allPossibleTeams.shuffled(Random(clock().inWholeMilliseconds)).forEach { team ->
      repeat(maxOf(1, previousMatches.size - ((teamCounts[team.joinToString()] ?: 0) * teamCountFactor).toInt())) {
        add(team)
      }
    }
  }
    .also {
      log { "team pool count ${it.groupBy { it }.mapValues { it.value.size }}" }
    }
    .shuffled(Random(clock().inWholeMilliseconds))
    .distinct()

  val team1 = possibleTeamsForTheNextMatch[0]
  val team2 = possibleTeamsForTheNextMatch
    .first { candidate ->
      candidate.none { it in team1 }
    }
  val starterTeam = if (Random(clock().inWholeMilliseconds).nextBoolean()) 1 else 2

  log { "team 1 $team1 vs team 2 $team2 starter $starterTeam" }

  pref.updateData {
    copy(
      currentMatch = Match(
        team1 = team1,
        team2 = team2,
        starterTeam = starterTeam
      ),
      previousMatches = previousMatches
    )
  }
}

private fun <T : Comparable<T>> List<T>.combinations(size: Int): List<List<T>> {
  if (size == 1) {
    val result = mutableListOf<List<T>>()
    for (item in this) {
      result += listOf(item)
    }
    return result
  }

  val result = mutableListOf<List<T>>()

  for (i in 0..this.size - size) {
    val firstItem = this[i]
    val additionalItems = this.subList(i + 1, this.size)
      .combinations(size - 1)

    for (additional in additionalItems) {
      val combination = mutableListOf<T>()
      combination += firstItem
      combination += additional
      result += combination
    }
  }

  return result
}
