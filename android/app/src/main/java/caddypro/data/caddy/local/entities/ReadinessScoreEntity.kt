package caddypro.data.caddy.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting readiness scores.
 *
 * Stores historical readiness data for trend analysis and offline access.
 * Maps to domain [ReadinessScore] model via mapper extensions.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 3
 * Acceptance criteria: A2 (Readiness impacts strategy)
 *
 * Indexes:
 * - timestamp: primary key for chronological ordering and range queries
 *
 * @property timestamp Unix timestamp (epoch milliseconds) when score was computed (primary key)
 * @property overall Overall readiness score (0-100)
 * @property hrvScore HRV contribution score 0-100 (null if unavailable)
 * @property sleepScore Sleep quality contribution score 0-100 (null if unavailable)
 * @property stressScore Stress level contribution score 0-100 (null if unavailable)
 * @property source Data source: "WEARABLE_SYNC" or "MANUAL_ENTRY"
 */
@Entity(
    tableName = "readiness_scores",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class ReadinessScoreEntity(
    @PrimaryKey
    val timestamp: Long,

    @ColumnInfo(name = "overall")
    val overall: Int,

    @ColumnInfo(name = "hrv_score")
    val hrvScore: Double?,

    @ColumnInfo(name = "sleep_score")
    val sleepScore: Double?,

    @ColumnInfo(name = "stress_score")
    val stressScore: Double?,

    @ColumnInfo(name = "source")
    val source: String
)
