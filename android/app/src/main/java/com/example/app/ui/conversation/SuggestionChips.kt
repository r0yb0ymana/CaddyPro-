package caddypro.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FlowRow of tappable suggestion chips.
 *
 * Used in clarification messages to present intent suggestions.
 * Chips automatically wrap to multiple rows if needed.
 *
 * Spec reference: navcaddy-engine.md A3
 * Plan reference: navcaddy-engine-plan.md Task 18
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionChips(
    suggestions: List<ClarificationSuggestion>,
    onSuggestionClick: (ClarificationSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

/**
 * Individual suggestion chip.
 *
 * @param suggestion The clarification suggestion to display
 * @param onClick Callback when chip is tapped
 */
@Composable
private fun SuggestionChip(
    suggestion: ClarificationSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = suggestion.label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    )
}
