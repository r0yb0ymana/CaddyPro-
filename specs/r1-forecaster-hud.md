# Spec: Forecaster HUD (R1)

**Status:** Draft
**Priority:** P0 (Core feature - real-time strategy)
**Estimated Effort:** 3-4 sessions (2-4hr blocks)
**Depends On:** Player Profile + Bag (needs club carry distances)

## Problem Statement

A golfer standing on the fairway needs to know *actual* carry distances, not textbook numbers. Temperature, wind, altitude, and humidity all change how far a ball flies. CaddyPro's Forecaster HUD gives the player real-time adjusted distances for every club in their active bag, so they pick the right club every time.

## User Stories

1. As a golfer, I want to see current weather conditions at my location so I understand the playing environment.
2. As a golfer, I want to see adjusted carry distances for each club based on current weather so I pick the right club.
3. As a golfer, I want to see wind direction and speed clearly so I can factor it into my shot planning.
4. As a golfer, I want the adjustments explained so I understand why my 7-iron plays longer or shorter today.
5. As a golfer, I want forecaster data available even with spotty signal so it works mid-round.

## Screens

### 1. Forecaster HUD Screen
**Route:** `/forecaster`
**When:** Main tab in bottom navigation

#### Weather Header
| Data Point | Source | Display |
|-----------|--------|---------|
| Temperature | OpenWeatherMap | e.g., "18C" (JetBrains Mono) |
| Wind Speed | OpenWeatherMap | e.g., "15 km/h" (JetBrains Mono) |
| Wind Direction | OpenWeatherMap | Compass arrow + bearing (e.g., "NW 315") |
| Humidity | OpenWeatherMap | e.g., "72%" |
| Conditions | OpenWeatherMap | Icon + label (e.g., "Partly Cloudy") |
| Last Updated | Cached timestamp | e.g., "2 min ago" |

#### Club Distance Cards
- List of all clubs from the active bag
- Grouped by type: Driver, Woods, Hybrids, Irons, Wedges, Putter
- Each card shows:
  - Club name
  - Base carry distance (dimmed)
  - **Adjusted carry distance** (large, prominent, JetBrains Mono)
  - Adjustment delta (e.g., "+3 yds" or "-5 yds", colored green/red)
  - Mini breakdown on tap: temp factor, wind factor, altitude factor

#### Wind Rose Widget
- Circular compass showing wind direction relative to intended shot direction
- Arrow indicates headwind/tailwind/crosswind
- Color coded: green (tailwind), red (headwind), amber (crosswind)

### 2. Adjustment Detail Sheet
**Type:** Bottom sheet modal
**Trigger:** Tap any club card

| Factor | Display | Example |
|--------|---------|---------|
| Base Carry | Original distance | "150 yds" |
| Temperature | +/- from 20C baseline | "+2 yds (28C)" |
| Altitude | +/- from sea level | "+3 yds (120m)" |
| Wind | Head/tail/cross component | "-7 yds (15 km/h head)" |
| Humidity | +/- from baseline | "+0 yds (72%)" |
| **Adjusted Carry** | Final number | **"148 yds"** |

## Data Models

### WeatherData (Room cache + API)
```
WeatherData {
    id: UUID (PK)
    latitude: Double
    longitude: Double
    temperatureC: Double
    windSpeedKmh: Double
    windDirectionDeg: Int
    humidity: Int
    conditionCode: Int
    conditionDescription: String
    feelsLikeC: Double
    pressureHpa: Int
    fetchedAt: Instant
    synced: Boolean
}
```

### AdjustedClubDistance (computed, not persisted)
```
AdjustedClubDistance {
    clubId: UUID
    clubName: String
    clubType: ClubType
    baseCarry: Int
    adjustedCarry: Int
    delta: Int
    temperatureEffect: Double
    altitudeEffect: Double
    windEffect: Double
    humidityEffect: Double
}
```

### LocationData (transient)
```
LocationData {
    latitude: Double
    longitude: Double
    altitudeM: Double
    accuracy: Float
}
```

## Carry Distance Adjustment Formula

```
adjustedCarry = baseCarry
    * temperatureFactor(tempC)     // ~1yd per 5C from 20C baseline
    * altitudeFactor(altM)         // ~2% per 300m above sea level
    * windFactor(windSpeed, windDir, shotDir)  // headwind/tailwind component
    * humidityFactor(humidity)     // minimal effect, ~1yd
```

### Factor Details

**Temperature Factor:**
- Baseline: 20C
- Effect: ~1 yard per 5C deviation per 150 yards of carry
- Formula: `1.0 + ((tempC - 20.0) / 5.0) * (baseCarry / 150.0) * (1.0 / baseCarry)`
- Simplified: `1.0 + (tempC - 20.0) * 0.00133`

**Altitude Factor:**
- Baseline: sea level (0m)
- Effect: ~2% per 300m above sea level
- Formula: `1.0 + (altitudeM / 300.0) * 0.02`
- Melbourne courses: ~30-80m elevation, minimal effect

**Wind Factor:**
- Decompose wind into headwind/tailwind component based on shot direction
- Headwind: reduces carry ~1% per 5 km/h
- Tailwind: increases carry ~0.5% per 5 km/h (asymmetric effect)
- Crosswind: minimal carry effect, primarily affects direction
- Formula: `1.0 + (tailwindComponent / 5.0) * factor` where factor is 0.005 tailwind, -0.01 headwind

**Humidity Factor:**
- Higher humidity = slightly longer (less dense air)
- Effect: ~1 yard per 25% humidity change from 50% baseline
- Formula: `1.0 + (humidity - 50) * 0.00003`
- Practically negligible, included for completeness

## API Integration

### OpenWeatherMap
- **Endpoint:** Current Weather Data API (`/data/2.5/weather`)
- **API Key:** Stored in BuildConfig
- **Rate Limit:** 60 calls/minute (free tier), cache for 10 minutes
- **Parameters:** lat, lon, units=metric, appid
- **Response fields used:** `main.temp`, `main.humidity`, `main.pressure`, `wind.speed`, `wind.deg`, `weather[0].id`, `weather[0].description`

### Location
- Android FusedLocationProviderClient
- Request location updates when Forecaster tab is active
- Coarse location sufficient for weather (fine location for altitude)
- Altitude from GPS (accuracy varies, acceptable for this purpose)
- Permission: ACCESS_FINE_LOCATION (needed for altitude)

## Caching Strategy

- Weather data cached in Room with `fetchedAt` timestamp
- Refresh weather every 10 minutes while HUD is visible
- Show cached data immediately on screen open, refresh in background
- If cache is older than 30 minutes, show "Weather data may be outdated" warning
- If no cache exists and no network, show base distances with "No weather data" indicator

## Acceptance Criteria

### Weather Display
- [ ] AC1: Current temperature, wind speed, wind direction, humidity displayed on HUD
- [ ] AC2: Weather data refreshes automatically every 10 minutes
- [ ] AC3: Last-updated timestamp shown so golfer knows data freshness
- [ ] AC4: Weather conditions icon matches current conditions (sun, cloud, rain, etc.)

### Distance Adjustments
- [ ] AC5: Every club in active bag shows adjusted carry distance
- [ ] AC6: Adjustment delta shown as "+/- X yds" with color coding (green=longer, red=shorter)
- [ ] AC7: Tapping a club card shows breakdown of each adjustment factor
- [ ] AC8: Adjustments recalculate when weather data refreshes
- [ ] AC9: Putter excluded from carry adjustments (always shows base distance)

### Wind
- [ ] AC10: Wind direction displayed as compass bearing with arrow
- [ ] AC11: Wind speed displayed in km/h (metric) or mph (imperial) per user preference

### Location
- [ ] AC12: App requests location permission with clear rationale
- [ ] AC13: Altitude used for altitude adjustment factor
- [ ] AC14: Location updates while Forecaster tab is active

### Offline / Caching
- [ ] AC15: Cached weather shown immediately on screen open
- [ ] AC16: Stale data warning shown when cache older than 30 minutes
- [ ] AC17: Base distances shown with "No weather data" when no cache and no network
- [ ] AC18: Weather cache persists across app restarts via Room

### Units
- [ ] AC19: Distances respect user's preferred units (yards/metres)
- [ ] AC20: Wind speed respects preferred units (km/h or mph)
- [ ] AC21: Temperature always in Celsius (Australia market)

### Theme Compliance
- [ ] AC22: All screens use CaddyPro dark theme
- [ ] AC23: Adjusted distances rendered in JetBrains Mono (large, prominent)
- [ ] AC24: Base distances rendered dimmed
- [ ] AC25: Touch targets minimum 48dp
- [ ] AC26: Performance Lime used for positive adjustments, Danger Zone for negative

## Task Breakdown

### Task 1: Weather Data Layer (2hr)
- OpenWeatherMap API client (Ktor or Retrofit)
- WeatherData Room entity + DAO
- WeatherRepository: fetch from API, cache in Room
- Weather data mapping (API response -> domain model)
- Unit tests for weather repository and caching

### Task 2: Carry Adjustment Engine (2hr)
- CarryAdjustmentCalculator with all four factors
- Unit tests for each factor (temperature, altitude, wind, humidity)
- Edge cases: extreme temps, no wind, high altitude, zero humidity
- AdjustedClubDistance mapping from active bag + weather

### Task 3: Location Services (1hr)
- LocationRepository wrapping FusedLocationProviderClient
- Permission request flow with rationale dialog
- Altitude extraction from location updates
- Unit tests for location data handling

### Task 4: Forecaster HUD Screen (3hr)
- ForecasterScreen composable (main tab)
- Weather header section
- Club distance card list (grouped by type)
- Wind rose widget
- ForecasterViewModel: combines weather + bag + location
- Loading, error, and empty states

### Task 5: Adjustment Detail Sheet (1hr)
- AdjustmentDetailSheet bottom sheet composable
- Factor breakdown display
- Animated transitions

### Task 6: Polish + Review (2hr)
- Verify all acceptance criteria
- Theme compliance audit
- Offline scenarios testing
- Units switching verification
- Edge case handling (no active bag, empty bag, location denied)

## Dependencies

- Player Profile + Bag spec complete (club carry distances exist)
- OpenWeatherMap API key configured in BuildConfig
- Location permissions in AndroidManifest.xml
- FusedLocationProviderClient (Google Play Services)

## Out of Scope

- Shot direction input (R2: will integrate with Hole Map for automatic shot direction)
- Historical weather trends
- Multi-day forecasts
- Weather alerts/warnings
- Altitude from DEM data (GPS altitude is sufficient for MVP)
- Barometric pressure adjustments (included in temperature factor approximation)

## Open Questions

1. **Shot direction:** For MVP, assume a default shot direction (North) since we don't have hole map integration yet. User cannot set shot direction manually in R1. Wind factor will show headwind/tailwind assuming northward play, with a note that accuracy improves with Hole Map in R2.
2. **Resolution:** For MVP, use a simple "Shot Direction" toggle (N/S/E/W/NE/NW/SE/SW) to approximate the intended shot direction. Full integration with Hole Map comes in R2.
