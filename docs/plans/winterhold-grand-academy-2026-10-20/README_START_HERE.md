# Jugcraft | Winterhold Grand Academy implementation pack
Planning package version: 2026-10-20. Target: Minecraft Java 26.3 + Fabric; Java 25.

## What this ZIP is
A design and delivery package, **not an implemented Minecraft structure or mod**. Includes a copyable Work handoff, spatial plan, machine-readable room/connection data, teacher roster, interactive room inventory, secret designs, weather/fog contract, integration/testing matrix and commit-by-commit work order.

## Read in order
1. `00_REPOSITORY_BASELINE.md` (verified documentation vs hypotheses)
2. `01_MASTER_WORK_PROMPT.md` (paste into ChatGPT Work with GitHub connected)
3. `02_SITE_AND_FLOORPLAN.md`, then `data/rooms.csv`, `data/connections.csv`, `data/layout.json`
4. `03_ROOM_FUNCTIONS.md`, `04_TEACHERS_NPCS.md`, `05_SECRETS_EVENTS.md`, `06_PERPETUAL_WINTER.md`
5. `07_PROGRESS_AND_BUILD_ORDER.md` and `08_TESTING_ACCEPTANCE.md`

## Non-negotiable experience
Recognizable Skyrim College **skeleton**; larger grand usable academy; existing Concordance magic; apprentices and instructors with recognizable reused humanoid models and differentiated robes; unique rooms and secrets; persistent localized snow/fog with an optional reduced-particles mode; careful server/client separation.

## Design vs implementation
Every room size and coordinate is a *proposed design target*, not a confirmed generator measurement. Work must survey current source and branches, revise data if constraints demand, make a genuine implementable milestone and publish a draft PR. Do not present this planning ZIP as a `.nbt` structure, datapack, or working compiled mod.
