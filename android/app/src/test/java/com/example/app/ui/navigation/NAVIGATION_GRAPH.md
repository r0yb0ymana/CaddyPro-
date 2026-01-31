# CaddyPro Navigation Graph

## Visual Navigation Map

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CaddyPro Navigation                            │
└─────────────────────────────────────────────────────────────────────────┘

                              ┌──────────────┐
                              │     Home     │
                              │   (Start)    │
                              └──────┬───────┘
                                     │
                   ┌─────────────────┼─────────────────┐
                   │                 │                 │
                   ▼                 ▼                 ▼
            ┌────────────┐    ┌────────────┐   ┌────────────┐
            │   Detail   │    │ Live Caddy │   │Round Start │
            │  (Legacy)  │    │    HUD     │   │  (TODO)    │
            └────────────┘    └─────┬──────┘   └─────┬──────┘
                                    │                 │
                                    │                 │ onRoundStarted
                                    │                 │ (popUpTo Home)
                                    │                 │
                                    │◄────────────────┘
                                    │
                      ┌─────────────┼─────────────┐
                      │             │             │
                      ▼             ▼             ▼
              ┌─────────────┐ ┌──────────┐ ┌──────────┐
              │   Weather   │ │  Stats   │ │  Shot    │
              │    Check    │ │  Lookup  │ │  Rec     │
              └─────────────┘ └──────────┘ └──────────┘
                      │             │             │
                      └─────────────┼─────────────┘
                                    │
                                    ▼
                            ┌───────────────┐
                            │  Round End    │
                            │   Summary     │
                            │  (roundId)    │
                            └───────┬───────┘
                                    │
                                    │ onNavigateBack
                                    │ (to Home)
                                    │
                                    ▼
                              ┌──────────┐
                              │   Home   │
                              └──────────┘
```

## Navigation Routes

### CADDY Module Routes

| Destination | Route | Parameters | Type |
|-------------|-------|------------|------|
| LiveCaddy | `caddy/live_caddy` | None | data object |
| RoundStart | `caddy/round_start` | `?course={name}` (optional) | data class |
| RoundEndSummary | `caddy/round_end_summary/{roundId}` | `roundId: Long` (required) | data class |
| ShotRecommendation | `caddy/shot_recommendation` | `?yardage=&lie=&wind=` (all optional) | data class |
| WeatherCheck | `caddy/weather` | None | data object |
| StatsLookup | `caddy/stats` | `?type={statType}` (optional) | data class |
| ClubAdjustment | `caddy/club_adjustment` | `?clubId={id}` (optional) | data class |
| ScoreEntry | `caddy/score_entry` | `?hole={number}` (optional) | data class |
| CourseInfo | `caddy/course_info` | `?id={courseId}` (required) | data class |

## Voice Query Routing

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Voice Query Routing Flow                         │
└─────────────────────────────────────────────────────────────────────┘

   User Voice Input
         ▼
   ┌──────────────────────┐
   │  Voice Recognition   │
   │  (System/Google)     │
   └──────────┬───────────┘
              ▼
   ┌──────────────────────┐
   │  Intent Classifier   │
   │  (Task 24)           │
   └──────────┬───────────┘
              ▼
   ┌──────────────────────┐
   │   RoutingTarget      │
   │   (module, screen,   │
   │    parameters)       │
   └──────────┬───────────┘
              ▼
   ┌──────────────────────┐
   │ NavCaddyDestination  │
   │  .fromRoutingTarget  │
   └──────────┬───────────┘
              ▼
   ┌──────────────────────┐
   │  NavCaddyNavigator   │
   │    .navigate()       │
   └──────────┬───────────┘
              ▼
   ┌──────────────────────┐
   │   NavController      │
   │   (Jetpack Nav)      │
   └──────────┬───────────┘
              ▼
   ┌──────────────────────┐
   │   Target Screen      │
   │   Composable         │
   └──────────────────────┘
```

### Voice Query Examples

| Query | IntentType | Destination | Parameters |
|-------|-----------|-------------|------------|
| "What's the play?" | STRATEGY_REQUEST | LiveCaddy | None |
| "Where should I aim?" | STRATEGY_REQUEST | ShotRecommendation | yardage, lie |
| "Where's the bailout?" | STRATEGY_REQUEST | LiveCaddy | expandStrategy=true |
| "Club up or down?" | CLUB_ADJUSTMENT | ClubAdjustment | None |
| "What's the weather?" | WEATHER_CHECK | WeatherCheck | None |
| "How am I hitting driver?" | STATS_LOOKUP | StatsLookup | type=driving |

## Deep Linking

### Supported Deep Link URIs

```
caddy://live_caddy
caddy://round_start
caddy://round_start?course=Pebble%20Beach
caddy://round_end_summary/123
caddy://shot_recommendation?yardage=150&lie=fairway
caddy://weather
caddy://stats?type=putting
```

### Deep Link Sources

1. **Notifications**
   - "Resume Round" → `caddy://live_caddy`
   - "Round Complete" → `caddy://round_end_summary/{id}`

2. **Widgets**
   - "Quick Start at {Course}" → `caddy://round_start?course={name}`
   - "Live Caddy" → `caddy://live_caddy`

3. **Share Links**
   - "Check out my round" → `caddy://round_end_summary/{id}`

4. **Voice Assistant**
   - Google Assistant shortcuts → various routes

## Back Stack Management

### Example: Full Round Flow

```
Initial State:
[Home]

After "Start Round":
[Home] → [RoundStart]

After Round Started (popUpTo Home):
[Home] → [LiveCaddy]

After navigating to Weather:
[Home] → [LiveCaddy] → [Weather]

After Back from Weather:
[Home] → [LiveCaddy]

After End Round (popUpTo LiveCaddy, inclusive):
[Home] → [RoundEndSummary]

After Back from Summary:
[Home]
```

### PopUpTo Scenarios

| From | To | popUpTo | inclusive | Result Stack |
|------|----|---------|-----------|--------------|
| RoundStart | LiveCaddy | Home | false | [Home, LiveCaddy] |
| LiveCaddy | RoundEndSummary | LiveCaddy | true | [Home, RoundEndSummary] |
| Any | Home | graph.start | true | [Home] |

## State Restoration

### Screens with State Parameters

| Screen | Parameter | Restoration |
|--------|-----------|-------------|
| RoundEndSummary | roundId: Long | Fully restorable from route |
| ShotRecommendation | yardage, lie, wind | Fully restorable from route |
| RoundStart | courseName | Fully restorable from route |
| LiveCaddy | None | Always same state |

### Process Death Handling

When the app process is killed by Android:
1. NavController saves current route
2. On restart, route is parsed
3. Parameters are extracted from route string
4. Screen is recreated with same parameters

Example:
```
Before death: caddy/round_end_summary/123
After restart: roundId = 123 extracted from route
```

## Navigation Testing Strategy

### Unit Tests (NavCaddyDestination)
- Route string generation
- Parameter encoding
- RoutingTarget conversion
- Validation of required parameters

### Unit Tests (NavCaddyNavigatorImpl)
- Navigation method calls
- Back stack manipulation
- PopUpTo behavior
- Clear back stack behavior

### Integration Tests
- Complete navigation flows
- Voice query routing
- Deep link handling
- State restoration

### UI Tests (Future)
- Composable navigation
- Animation transitions
- Back button behavior
- Deep link manifest integration

## Performance Considerations

### Route Generation
- **Data objects**: O(1) - constant string
- **Data classes with params**: O(n) where n = number of parameters
- **URL encoding**: O(m) where m = string length

### Memory
- Data objects are singletons (shared instance)
- Data classes allocated per navigation (small footprint)
- Route strings interned by NavController

### Navigation Speed
- Type-safe compilation (no reflection)
- Direct NavController method calls
- Minimal string manipulation

## Security Considerations

### Route Injection Prevention
- All parameters properly URL encoded
- Long type for roundId (no SQL injection risk)
- No eval() or dynamic code execution

### Deep Link Validation
- Manifest intent filters validate host/scheme
- Parameter types validated at conversion
- Required parameters throw exceptions if missing

### PII Protection
- No sensitive data in route strings
- roundId is opaque identifier
- Course names are public information

## Accessibility

### Screen Transitions
- TalkBack announces screen changes
- Focus moves to first focusable element
- Screen title announced via Scaffold

### Back Navigation
- Hardware back button supported
- System back gesture supported
- Consistent back stack behavior

### Reduced Motion
- Respects system animation preferences
- NavController handles transition animations
- Can be disabled in settings

## Android Version Compatibility

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **NavController**: Compatible with all supported versions
- **Data objects**: Requires Kotlin 1.7+

## Future Enhancements

### Planned Features
1. **Nested Navigation Graphs**
   - Separate graph for Live Caddy sub-flows
   - Module-based graph separation

2. **Shared Element Transitions**
   - Smooth transitions between screens
   - Shared weather HUD elements

3. **Multi-Stack Navigation**
   - Bottom navigation with preserved stacks
   - Tab-based navigation

4. **Dynamic Navigation**
   - Feature flags control route availability
   - A/B testing different flows

### Deep Link Enhancements
1. **App Links Verification**
   - Digital Asset Links for verified domains
   - Automatic app opening from web links

2. **Universal Links**
   - Cross-platform deep linking
   - Web fallback for non-installed users

3. **Smart Routing**
   - Context-aware navigation (active round → LiveCaddy)
   - Time-based routing (morning → practice mode)

## Troubleshooting

### Common Issues

**Issue**: Navigation doesn't occur
- Check route string matches NavHost definition
- Verify NavController is not null
- Check for typos in route constants

**Issue**: Arguments not passed correctly
- Verify NavType matches parameter type
- Check URL encoding for special characters
- Ensure required arguments are provided

**Issue**: Back navigation unexpected
- Review popUpTo configuration
- Check inclusive flag on popUpTo
- Verify launchSingleTop usage

**Issue**: Process death loses state
- Ensure parameters in route string
- Check Parcelable implementation for complex types
- Use SavedStateHandle for additional state

---

**Last Updated**: 2026-01-30
**Task**: Task 23 - Add Live Caddy Routes to Navigation
**Status**: Implementation Complete
