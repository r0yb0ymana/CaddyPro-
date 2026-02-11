package caddypro.domain.navcaddy.navigation

import caddypro.domain.navcaddy.models.Module
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for NavCaddyDestination route generation.
 *
 * Verifies:
 * - Route strings are correctly formatted
 * - Parameters are properly encoded
 * - Optional parameters handled correctly
 */
class NavCaddyDestinationTest {

    // CADDY Module Tests

    @Test
    fun `ClubAdjustment without clubId generates base route`() {
        val destination = NavCaddyDestination.ClubAdjustment(clubId = null)

        assertEquals("caddy/club_adjustment", destination.toRoute())
        assertEquals(Module.CADDY, destination.module)
        assertEquals("club_adjustment", destination.screen)
    }

    @Test
    fun `ClubAdjustment with clubId includes parameter`() {
        val destination = NavCaddyDestination.ClubAdjustment(clubId = "7i")

        assertEquals("caddy/club_adjustment?clubId=7i", destination.toRoute())
    }

    @Test
    fun `ShotRecommendation without parameters generates base route`() {
        val destination = NavCaddyDestination.ShotRecommendation()

        assertEquals("caddy/shot_recommendation", destination.toRoute())
    }

    @Test
    fun `ShotRecommendation with all parameters includes all in route`() {
        val destination = NavCaddyDestination.ShotRecommendation(
            yardage = 150,
            lie = "fairway",
            wind = "10mph_headwind"
        )

        assertEquals(
            "caddy/shot_recommendation?yardage=150&lie=fairway&wind=10mph_headwind",
            destination.toRoute()
        )
    }

    @Test
    fun `ShotRecommendation with partial parameters includes only provided ones`() {
        val destination = NavCaddyDestination.ShotRecommendation(
            yardage = 175,
            lie = "rough"
        )

        assertEquals(
            "caddy/shot_recommendation?yardage=175&lie=rough",
            destination.toRoute()
        )
    }

    @Test
    fun `RoundStart without course generates base route`() {
        val destination = NavCaddyDestination.RoundStart()

        assertEquals("caddy/round_start", destination.toRoute())
    }

    @Test
    fun `RoundStart with course name encodes spaces`() {
        val destination = NavCaddyDestination.RoundStart(courseName = "Pebble Beach")

        assertEquals("caddy/round_start?course=Pebble%20Beach", destination.toRoute())
    }

    @Test
    fun `ScoreEntry without hole generates base route`() {
        val destination = NavCaddyDestination.ScoreEntry()

        assertEquals("caddy/score_entry", destination.toRoute())
    }

    @Test
    fun `ScoreEntry with hole includes parameter`() {
        val destination = NavCaddyDestination.ScoreEntry(hole = 7)

        assertEquals("caddy/score_entry?hole=7", destination.toRoute())
    }

    @Test
    fun `RoundEnd generates correct route`() {
        val destination = NavCaddyDestination.RoundEnd

        assertEquals("caddy/round_end", destination.toRoute())
    }

    @Test
    fun `WeatherCheck generates correct route`() {
        val destination = NavCaddyDestination.WeatherCheck

        assertEquals("caddy/weather", destination.toRoute())
    }

    @Test
    fun `StatsLookup without type generates base route`() {
        val destination = NavCaddyDestination.StatsLookup()

        assertEquals("caddy/stats", destination.toRoute())
    }

    @Test
    fun `StatsLookup with type includes parameter`() {
        val destination = NavCaddyDestination.StatsLookup(statType = "putting")

        assertEquals("caddy/stats?type=putting", destination.toRoute())
    }

    @Test
    fun `CourseInfo includes required courseId`() {
        val destination = NavCaddyDestination.CourseInfo(courseId = "pebble-beach-123")

        assertEquals("caddy/course_info?id=pebble-beach-123", destination.toRoute())
        assertEquals(Module.CADDY, destination.module)
    }

    // COACH Module Tests

    @Test
    fun `DrillScreen without parameters generates base route`() {
        val destination = NavCaddyDestination.DrillScreen()

        assertEquals("coach/drill", destination.toRoute())
        assertEquals(Module.COACH, destination.module)
    }

    @Test
    fun `DrillScreen with drillId includes parameter`() {
        val destination = NavCaddyDestination.DrillScreen(drillId = "drill-123")

        assertEquals("coach/drill?drillId=drill-123", destination.toRoute())
    }

    @Test
    fun `DrillScreen with focusArea includes parameter`() {
        val destination = NavCaddyDestination.DrillScreen(focusArea = "putting")

        assertEquals("coach/drill?focusArea=putting", destination.toRoute())
    }

    @Test
    fun `DrillScreen with both parameters includes both`() {
        val destination = NavCaddyDestination.DrillScreen(
            drillId = "drill-456",
            focusArea = "driver"
        )

        assertEquals("coach/drill?drillId=drill-456&focusArea=driver", destination.toRoute())
    }

    @Test
    fun `PracticeSession generates correct route`() {
        val destination = NavCaddyDestination.PracticeSession

        assertEquals("coach/practice", destination.toRoute())
        assertEquals(Module.COACH, destination.module)
    }

    // RECOVERY Module Tests

    @Test
    fun `RecoveryOverview generates correct route`() {
        val destination = NavCaddyDestination.RecoveryOverview

        assertEquals("recovery/overview", destination.toRoute())
        assertEquals(Module.RECOVERY, destination.module)
    }

    @Test
    fun `RecoveryDataEntry without type generates base route`() {
        val destination = NavCaddyDestination.RecoveryDataEntry()

        assertEquals("recovery/data_entry", destination.toRoute())
        assertEquals(Module.RECOVERY, destination.module)
    }

    @Test
    fun `RecoveryDataEntry with type includes parameter`() {
        val destination = NavCaddyDestination.RecoveryDataEntry(dataType = "hrv")

        assertEquals("recovery/data_entry?type=hrv", destination.toRoute())
    }

    // SETTINGS Module Tests

    @Test
    fun `EquipmentManagement generates correct route`() {
        val destination = NavCaddyDestination.EquipmentManagement

        assertEquals("settings/equipment", destination.toRoute())
        assertEquals(Module.SETTINGS, destination.module)
    }

    @Test
    fun `SettingsScreen without key generates base route`() {
        val destination = NavCaddyDestination.SettingsScreen()

        assertEquals("settings/settings", destination.toRoute())
        assertEquals(Module.SETTINGS, destination.module)
    }

    @Test
    fun `SettingsScreen with key includes parameter`() {
        val destination = NavCaddyDestination.SettingsScreen(settingKey = "profile")

        assertEquals("settings/settings?key=profile", destination.toRoute())
    }

    @Test
    fun `HelpScreen generates correct route`() {
        val destination = NavCaddyDestination.HelpScreen

        assertEquals("settings/help", destination.toRoute())
        assertEquals(Module.SETTINGS, destination.module)
    }
}
