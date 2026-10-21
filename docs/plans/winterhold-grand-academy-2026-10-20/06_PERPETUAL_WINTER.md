# The College's microclimate | localized permanent snow and light fog
This is a **desired new feature**, not established repo functionality. Existing Jugcraft fog machine/particles are possible reusable components, subject to source inspection.

## Player-visible behavior
- At bridge approach the air chills, snow gently starts falling, wind hum replaces normal ambience, and old rune lamps brighten. Near the courtyard there is continuous delicate snow and a **light** blue-gray mist; impressive yet readable.
- The area keeps this winter atmosphere in a desert, savanna, forest, jungle or other biome, **regardless of actual world weather**. A player outside the area sees normal weather; two players can observe different local atmospheres simultaneously.
- Visual transition over ~12–24 blocks at boundary; strong snow around the College and subtle background flurries on elevated bridge. Fog never obscures safe navigation or makes hostile enemies invisible.
- Ground snowcaps and icicles should be authored/generated as stable decorative structure parts instead of relying on global natural snow layers that could change nearby terrain.

## Required technical boundaries
1. Server owns persisted region geometry (academy instance ID, world/dimension, center and bounded shape), but does **not** change global weather or biome values.
2. Client receives validated region state only and computes whether camera is inside or approaching, using a smooth distance falloff. If client rendering lacks a supported local fog API, use nearby short-lived translucent particle wisps + ambient audio rather than undocumented global fog hacks.
3. Particle spawning is budgeted by nearby camera visibility, distance, frame/time budget, reduced-particles setting, and no per-block/per-tick world scan. Avoid networked particles in a giant radius every tick.
4. Snow should be perceptual, not cold-biome simulation: no accidental farming freeze, ice conversion, crop or weather damage outside academy; preserve actual server weather, biome, lightning and seasons.
5. No weather griefing, and no global shader/sky replacement. Respect client accessibility settings and render distance, with configuration for off/low/normal effects. Collision remains unchanged.
6. Indoors reduce snowfall or suppress it except designated magical atriums/roof openings; courtyard and sky bridges remain snowy. Manage indoor visibility without expensive ray tracing every frame; use known volumes/roof classification or a safe bounded check.
7. On exit/unload/world change, remove ambience promptly. In multiplayer, region state must not leak across worlds/dimensions.
8. Biome-independent region generation still respects placement and cliff suitability rules. Prefer naturally cold sites if configured, but climate aura works anywhere it is placed.

## Suggested tuning targets (provisional)
- Academy courtyard radius ~66–76 blocks plus bridge corridor, with atmospheric boundary fade of 16 blocks.
- Mist visually mild: dominant architecture remains readable at 60 blocks in normal settings; provide stronger cinematic option only if optional.
- Snow audio subtle; accents on gusts; no constant loud wind inside living areas.
- Snowflake density tested low/normal/particles-minimal; performance evaluated with 2 clients and measured FPS/frametime.

## Test scenarios
Different-biome placements; one player inside and one outside; rain/thunder outside while snow looks constant inside; indoor suppression; client low particles; reload; dimension travel; chunk boundaries; multiple generated Colleges (if allowed); controller config; screenshot from bridge and courtyard. Confirm actual biome still same using debug/system APIs.
