/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.matchmaker

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.ResourceProvider
import com.ivianuu.essentials.rubik.Rubik
import com.ivianuu.essentials.ui.AppTheme
import com.ivianuu.essentials.ui.material.EsTheme
import com.ivianuu.essentials.ui.material.EsTypography
import com.ivianuu.essentials.ui.material.colors
import com.ivianuu.essentials.ui.material.editEach
import com.ivianuu.injekt.Provide

object MatchMakerTheme {
  val Primary = Color(0xFF778BEB)
  val Secondary = Color(0xFFF7D794)
}

@Provide fun matchMakerTheme(resourceProvider: ResourceProvider) = AppTheme { content ->
  EsTheme(
    lightColors = colors(
      isLight = true,
      primary = MatchMakerTheme.Primary,
      primaryVariant = MatchMakerTheme.Primary,
      secondary = MatchMakerTheme.Secondary,
      secondaryVariant = MatchMakerTheme.Secondary
    ),
    darkColors = colors(
      isLight = false,
      primary = MatchMakerTheme.Primary,
      primaryVariant = MatchMakerTheme.Primary,
      secondary = MatchMakerTheme.Secondary,
      secondaryVariant = MatchMakerTheme.Secondary
    ),
    typography = EsTypography.editEach { copy(fontFamily = Rubik) },
    content = content
  )
}
