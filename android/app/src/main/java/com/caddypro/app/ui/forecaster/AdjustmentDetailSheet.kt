package com.caddypro.app.ui.forecaster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.caddypro.app.domain.model.AdjustedClubDistance
import com.caddypro.app.domain.model.WeatherData
import com.caddypro.app.ui.theme.CaddyProColors
import com.caddypro.app.ui.theme.DataTextStyles
import com.caddypro.app.ui.theme.JetBrainsMonoFontFamily
import com.caddypro.app.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Adjustment Detail Bottom Sheet
 *
 * AC7: Tapping a club card shows breakdown of each adjustment factor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentDetailSheet(
    club: AdjustedClubDistance,
    weather: WeatherData?,
    useMetric: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val unitLabel = if (useMetric) "m" else "yds"

    fun toDisplayUnit(yards: Int): Int = if (useMetric) (yards * 0.9144).roundToInt() else yards
    fun toDisplayUnit(yards: Double): Double = if (useMetric) yards * 0.9144 else yards

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Large)
        ) {
            // Club name
            Text(
                text = club.clubName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            // Base carry
            AdjustmentRow(
                label = "Base Carry",
                value = "${toDisplayUnit(club.baseCarry)} $unitLabel",
                isTotal = false
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.Small),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Temperature effect
            val tempEffect = toDisplayUnit(club.temperatureEffect)
            AdjustmentRow(
                label = "Temperature",
                value = formatEffect(tempEffect, unitLabel),
                detail = weather?.let { "${it.temperatureC.roundToInt()}°C" },
                effectColor = effectColor(tempEffect)
            )

            // Altitude effect
            val altEffect = toDisplayUnit(club.altitudeEffect)
            AdjustmentRow(
                label = "Altitude",
                value = formatEffect(altEffect, unitLabel),
                detail = null,
                effectColor = effectColor(altEffect)
            )

            // Wind effect
            val windEffect = toDisplayUnit(club.windEffect)
            AdjustmentRow(
                label = "Wind",
                value = formatEffect(windEffect, unitLabel),
                detail = weather?.let {
                    val windDisplay = if (useMetric) "${it.windSpeedKmh.roundToInt()} km/h"
                    else "${(it.windSpeedKmh * 0.621371).roundToInt()} mph"
                    windDisplay
                },
                effectColor = effectColor(windEffect)
            )

            // Humidity effect
            val humEffect = toDisplayUnit(club.humidityEffect)
            AdjustmentRow(
                label = "Humidity",
                value = formatEffect(humEffect, unitLabel),
                detail = weather?.let { "${it.humidity}%" },
                effectColor = effectColor(humEffect)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.Small),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Adjusted carry total
            AdjustmentRow(
                label = "Adjusted Carry",
                value = "${toDisplayUnit(club.adjustedCarry)} $unitLabel",
                isTotal = true
            )

            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
        }
    }
}

@Composable
private fun AdjustmentRow(
    label: String,
    value: String,
    detail: String? = null,
    isTotal: Boolean = false,
    effectColor: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = if (isTotal) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = value,
            style = if (isTotal) DataTextStyles.DataSmall
            else MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFontFamily),
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = effectColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatEffect(effect: Double, unitLabel: String): String {
    val rounded = effect.roundToInt()
    return when {
        rounded > 0 -> "+$rounded $unitLabel"
        rounded < 0 -> "$rounded $unitLabel"
        else -> "0 $unitLabel"
    }
}

@Composable
private fun effectColor(effect: Double): androidx.compose.ui.graphics.Color {
    val rounded = effect.roundToInt()
    return when {
        rounded > 0 -> CaddyProColors.PerformanceLime
        rounded < 0 -> CaddyProColors.DangerZone
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
