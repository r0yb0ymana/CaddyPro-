# Spec: Basic Hole Map (R1)

**Status:** Draft
**Priority:** P0 (Core feature - on-course visual)
**Estimated Effort:** 4-5 sessions (2-4hr blocks)
**Depends On:** Player Profile + Bag (club data), Shot Logger (active round context)

## Problem Statement

A golfer on the course needs a visual understanding of the hole: where hazards are, how far to the green, and where they are relative to the target. CaddyPro's Hole Map provides satellite imagery overlaid with course hazards (bunkers, water, OB) and a prominent distance-to-green readout, enabling smarter shot decisions.

## User Stories

1. As a golfer, I want to see a satellite view of my current hole so I can visualize the layout.
2. As a golfer, I want hazards (bunkers, water, OB) highlighted on the map so I know what to avoid.
3. As a golfer, I want to see my distance to the green center so I can select the right club.
4. As a golfer, I want to navigate between holes during my round so I can plan ahead.
5. As a golfer, I want to flag incorrect map data so it can be improved over time.
6. As a golfer, I want the map to work even without OSM data, showing satellite imagery only.

## Screens

### 1. Hole Map Screen
**Route:** `/hole-map?hole={holeNumber}`
**When:** Accessed from shot logger or main navigation

#### Map View
- Satellite imagery via Google Maps
- Player position shown with blue location indicator
- Green center marked with prominent green marker
- Hazard overlays: bunker (sand/tan), water (blue), OB (red), trees (dark green)

#### Distance HUD
- Prominent distance to green center (48sp, JetBrains Mono)
- "to green center" label below
- Updates as player moves

#### Hole Navigation
- Previous/Next hole controls at bottom
- Hole number displayed in top bar
- Cycles through available greens from OSM data

#### Flag Incorrect Data
- Small flag button in top bar
- Opens dialog for description text
- Saves locally for Tier 2 contribution seeding

## Acceptance Criteria

### Map Rendering
- AC1: Satellite imagery renders with Google Maps SDK
- AC2: Map centers on player GPS location
- AC3: Player position shown with standard location indicator
- AC4: Map style supports dark theme UI controls overlay

### Hazard Overlays
- AC5: Bunker hazards render as sand/tan colored polygons (35% alpha)
- AC6: Water hazards render as blue polygons (35% alpha)
- AC7: OB areas render as red polygons (25% alpha)
- AC8: Tree hazards render as dark green polygons (20% alpha)
- AC9: Green center shown with PerformanceLime marker
- AC10: Hazard data fetched from Overpass API (OSM)
- AC11: Courses without OSM data show satellite-only view (graceful degradation)

### Distance Display
- AC12: Distance to green center displayed in DataLarge style (48sp)
- AC13: Distance updates as player moves
- AC14: Distance shown in yards (imperial) or meters (metric) per user preference
- AC15: Haversine formula used for distance calculation

### Hole Navigation
- AC16: Previous/Next hole controls cycle through available greens
- AC17: Current hole number displayed in top bar
- AC18: When launched from shot logger, opens on current hole

### Data Management
- AC19: Overpass API data cached in Room for 7 days
- AC20: Cache-first strategy: return cached data if available
- AC21: Graceful degradation on API failure (show satellite only)
- AC22: "Flag incorrect data" button present and functional
- AC23: Flag reports saved locally to Room

### Performance
- AC24: Location permission requested with standard Android flow
- AC25: Loading state shown while fetching course data
- AC26: Map interaction remains smooth with overlays rendered

## Task Breakdown

| Task | Description | Estimate |
|------|-------------|----------|
| 1 | Google Maps SDK setup + basic map rendering | 2-3 hrs |
| 2 | Overpass API service + course data models + Room caching | 3-4 hrs |
| 3 | Hazard overlay rendering + green markers | 2-3 hrs |
| 4 | Distance HUD + hole navigation + flag data | 3-4 hrs |
| 5 | Navigation integration + shot logger link + tests | 3-4 hrs |

## Technical Notes

- Google Maps Compose: `com.google.maps.android:maps-compose`
- Overpass API: POST to `https://overpass-api.de/api/interpreter`
- OSM tags: `golf=green`, `golf=bunker`, `natural=water`, `golf=water_hazard`
- Green center: centroid of green polygon
- Haversine formula for GPS distance calculation
- Room caching with 7-day TTL for Overpass data
