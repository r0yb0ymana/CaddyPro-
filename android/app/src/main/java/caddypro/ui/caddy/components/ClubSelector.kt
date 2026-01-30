package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caddypro.ui.theme.CaddyProTheme
import com.example.app.domain.navcaddy.models.Club
import com.example.app.domain.navcaddy.models.ClubType

/**
 * Club selector grid for shot logging.
 *
 * Displays clubs in a responsive grid with:
 * - Large touch targets (56dp height minimum)
 * - Clear visual selection state
 * - Club name displayed prominently
 * - Distance shown as secondary info
 *
 * Design optimized for outdoor visibility and one-tap selection.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 17
 *
 * @param clubs List of clubs available for selection
 * @param selectedClub Currently selected club (null if no selection)
 * @param onClubSelected Callback when a club is tapped
 * @param modifier Modifier for the grid container
 */
@Composable
fun ClubSelector(
    clubs: List<Club>,
    selectedClub: Club?,
    onClubSelected: (Club) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(clubs) { club ->
            ClubChip(
                club = club,
                isSelected = club.id == selectedClub?.id,
                onClick = { onClubSelected(club) }
            )
        }
    }
}

/**
 * Individual club chip button.
 *
 * Material 3 FilterChip with:
 * - Selected/unselected states
 * - Large touch target (56dp height)
 * - Club name in bold
 * - Distance shown below name
 *
 * @param club The club to display
 * @param isSelected True if this club is currently selected
 * @param onClick Callback when chip is tapped
 */
@Composable
private fun ClubChip(
    club: Club,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = club.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        },
        modifier = modifier
            .height(56.dp)  // Large touch target per spec
            .fillMaxWidth(),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

// Preview functions for development and testing

@Preview(name = "Club Selector - None Selected", showBackground = true)
@Composable
private fun PreviewClubSelectorNoneSelected() {
    CaddyProTheme {
        ClubSelector(
            clubs = previewClubs(),
            selectedClub = null,
            onClubSelected = {}
        )
    }
}

@Preview(name = "Club Selector - One Selected", showBackground = true)
@Composable
private fun PreviewClubSelectorOneSelected() {
    CaddyProTheme {
        ClubSelector(
            clubs = previewClubs(),
            selectedClub = previewClubs()[3],
            onClubSelected = {}
        )
    }
}

@Preview(name = "Club Selector - Driver Selected", showBackground = true)
@Composable
private fun PreviewClubSelectorDriverSelected() {
    CaddyProTheme {
        ClubSelector(
            clubs = previewClubs(),
            selectedClub = previewClubs().first { it.type == ClubType.DRIVER },
            onClubSelected = {}
        )
    }
}

@Preview(name = "Club Selector - Few Clubs", showBackground = true)
@Composable
private fun PreviewClubSelectorFewClubs() {
    CaddyProTheme {
        ClubSelector(
            clubs = previewClubs().take(5),
            selectedClub = null,
            onClubSelected = {}
        )
    }
}

/**
 * Preview helper - generates sample club list.
 */
private fun previewClubs(): List<Club> {
    return listOf(
        Club(
            id = "1",
            name = "Driver",
            type = ClubType.DRIVER,
            estimatedCarry = 250
        ),
        Club(
            id = "2",
            name = "3W",
            type = ClubType.WOOD,
            estimatedCarry = 230
        ),
        Club(
            id = "3",
            name = "5W",
            type = ClubType.WOOD,
            estimatedCarry = 210
        ),
        Club(
            id = "4",
            name = "4H",
            type = ClubType.HYBRID,
            estimatedCarry = 190
        ),
        Club(
            id = "5",
            name = "5i",
            type = ClubType.IRON,
            estimatedCarry = 180
        ),
        Club(
            id = "6",
            name = "6i",
            type = ClubType.IRON,
            estimatedCarry = 170
        ),
        Club(
            id = "7",
            name = "7i",
            type = ClubType.IRON,
            estimatedCarry = 160
        ),
        Club(
            id = "8",
            name = "8i",
            type = ClubType.IRON,
            estimatedCarry = 145
        ),
        Club(
            id = "9",
            name = "9i",
            type = ClubType.IRON,
            estimatedCarry = 135
        ),
        Club(
            id = "10",
            name = "PW",
            type = ClubType.WEDGE,
            estimatedCarry = 120
        ),
        Club(
            id = "11",
            name = "GW",
            type = ClubType.WEDGE,
            estimatedCarry = 105
        ),
        Club(
            id = "12",
            name = "SW",
            type = ClubType.WEDGE,
            estimatedCarry = 90
        ),
        Club(
            id = "13",
            name = "LW",
            type = ClubType.WEDGE,
            estimatedCarry = 75
        ),
        Club(
            id = "14",
            name = "Putter",
            type = ClubType.PUTTER,
            estimatedCarry = 0
        )
    )
}
