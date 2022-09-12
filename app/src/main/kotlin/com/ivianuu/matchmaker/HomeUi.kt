/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.matchmaker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.FloatingActionButtonElevation
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import com.ivianuu.essentials.data.DataStore
import com.ivianuu.essentials.state.action
import com.ivianuu.essentials.state.bind
import com.ivianuu.essentials.ui.common.SimpleListScreen
import com.ivianuu.essentials.ui.common.VerticalList
import com.ivianuu.essentials.ui.dialog.TextInputKey
import com.ivianuu.essentials.ui.layout.align
import com.ivianuu.essentials.ui.material.ListItem
import com.ivianuu.essentials.ui.material.Scaffold
import com.ivianuu.essentials.ui.material.Subheader
import com.ivianuu.essentials.ui.material.TextButton
import com.ivianuu.essentials.ui.material.TopAppBar
import com.ivianuu.essentials.ui.navigation.Model
import com.ivianuu.essentials.ui.navigation.ModelKeyUi
import com.ivianuu.essentials.ui.navigation.Navigator
import com.ivianuu.essentials.ui.navigation.RootKey
import com.ivianuu.essentials.ui.navigation.push
import com.ivianuu.essentials.ui.prefs.SliderListItem
import com.ivianuu.essentials.ui.prefs.SwitchListItem
import com.ivianuu.injekt.Provide

@Provide object HomeKey : RootKey

@Provide val homeUi = ModelKeyUi<HomeKey, HomeModel> {
  SimpleListScreen("Match maker") {
    item {
      Card(
        modifier = Modifier
          .padding(16.dp)
          .fillMaxWidth(),
        border = BorderStroke(1.dp, LocalContentColor.current.copy(alpha = 0.12f)),
        elevation = 0.dp
      ) {
        Column(
          modifier = Modifier.padding(8.dp)
        ) {
          @Composable fun Match(match: Match) {
            Row {
              Column {
                Text("Team 1")

                match.team1.forEach {
                  Text(it)
                }
              }

              Text("VS")

              Column {
                Text("Team 2")

                match.team2.forEach {
                  Text(it)
                }
              }
            }

            Text("Team ${match.starterTeam} begins")

            Spacer(Modifier.height(16.dp))
          }

          val currentMatch = currentMatch

          if (currentMatch != null) {
            Match(currentMatch)
          } else {
            Text(
              "No current match"
            )
          }

          previousMatches.forEach { Match(it) }

          FloatingActionButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = makeMatch,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
          ) {
            Icon(R.drawable.ic_refresh)
          }

          FloatingActionButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = resetMatches,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
          ) {
            Icon(R.drawable.ic_delete)
          }
        }
      }
    }

    item {
      Subheader { Text("Players") }
    }

    if (players.isNotEmpty()) {
      items(players.toList()) { player ->
        ListItem(
          title = { Text(player) },
          trailing = {
            IconButton(onClick = { removePlayer(player) }) { Icon(R.drawable.ic_delete) }
          }
        )
      }
    } else {
      item {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(
            modifier = Modifier
              .padding(horizontal = 16.dp, vertical = 8.dp),
            text = "No players",
            style = MaterialTheme.typography.button
          )
        }
      }
    }

    item {
      TextButton(
        modifier = Modifier.padding(horizontal = 8.dp)
          .fillMaxWidth(),
        onClick = addPlayer
      ) {
        Text(
          modifier = Modifier.align(Alignment.CenterStart),
          text = "Add player.."
        )
      }
    }

    item {
      Subheader { Text("Settings") }
    }

    item {
      SliderListItem(
        value = teamSize,
        onValueChange = updateTeamSize,
        valueRange = 1..10,
        title = { Text("Team size") },
        valueText = { Text(it.toString()) }
      )
    }

    item {
      SwitchListItem(
        value = preferNewPlayers,
        onValueChange = updatePreferNewPlayers,
        title = { Text("Prefer new players") }
      )
    }

    item {
      SwitchListItem(
        value = distinctTeams,
        onValueChange = updateDistinctTeams,
        title = { Text("Distinct teams") }
      )
    }
  }
}

data class HomeModel(
  val currentMatch: Match?,
  val previousMatches: List<Match>,
  val players: Set<String>,
  val addPlayer: () -> Unit,
  val removePlayer: (String) -> Unit,
  val teamSize: Int,
  val updateTeamSize: (Int) -> Unit,
  val preferNewPlayers: Boolean,
  val updatePreferNewPlayers: (Boolean) -> Unit,
  val distinctTeams: Boolean,
  val updateDistinctTeams: (Boolean) -> Unit,
  val makeMatch: () -> Unit,
  val resetMatches: () -> Unit
)

@Provide fun homeModel(
  makeMatchUseCase: MakeMatchUseCase,
  navigator: Navigator,
  pref: DataStore<MatchMakerPrefs>
) = Model {
  val prefs = pref.data.bind(MatchMakerPrefs())

  HomeModel(
    currentMatch = prefs.currentMatch,
    previousMatches = prefs.previousMatches,
    players = prefs.players,
    addPlayer = action {
      val player = navigator.push(
        TextInputKey(
          label = "Player name..",
          predicate = { it.isNotEmpty() }
        )
      )?.trim() ?: return@action

      pref.updateData { copy(players = (players + player).sorted().toSet()) }
    },
    removePlayer = action { player ->
      pref.updateData { copy(players = players - player) }
    },
    teamSize = prefs.teamSize,
    updateTeamSize = action { value -> pref.updateData { resetMatches().copy(teamSize = value) } },
    preferNewPlayers = prefs.preferNewPlayers,
    updatePreferNewPlayers = action { value -> pref.updateData { copy(preferNewPlayers = value) } },
    distinctTeams = prefs.distinctTeams,
    updateDistinctTeams = action { value -> pref.updateData { copy(distinctTeams = value) } },
    makeMatch = action { makeMatchUseCase() },
    resetMatches = action { pref.updateData { resetMatches() } }
  )
}
