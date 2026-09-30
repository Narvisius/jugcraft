# Mining and prospecting

Status: implemented in source (PR #25); **not yet played**. Compiles in CI; game tests cover it, and the client test screenshots the prospector screen.
Proposal issue: none. The owner selected "Mining & prospecting" directly on 30 September 2026 and asked for a prospector that "should only give an idea of whats in like the nearby 9 chunks … not so specific as one chunk" with a "steampunk digital" interface.
Owner: @jimbozoomer-byte
Target milestone and tier: the prospector comes after the workshop tier (it needs a basic circuit); the ore drill comes after steel.
Primary specialty and supported player role: exploration and engineering

## Player experience
- The **Geo-Resonance Prospector** is a hand tool. Right-click it to survey the 3×3 chunks around you.
  - A brass instrument with an amber CRT opens.
  - Each ore family found gets up to five valve-tube bars and a rough depth band (shallow, middle or deep).
  - It never says which chunk or block the ore is in.
- The **Ore Drill** is a 2-tall powered derrick. It mines the ores in a 9×9 column below it, all the way down, and refills each hole with rock.

Details: [TECH_TREE.md → Mining and prospecting](../TECH_TREE.md#mining-and-prospecting).

## Connections
- Input producer: power networks.
- Output consumer: ore processing. The drill gives whole ore blocks, so the crusher, pulverizer and ore washer still decide the yield.
- Technology connection: the drill is a processing machine, so it has upgrades, side configuration, eject, redstone modes and comparator output.
- Magic connection: none yet.
- Reachable entry path:
  - The prospector needs brass plates, a copper wire, a glass pane and a basic circuit.
  - The drill needs steel plates, a steel gear, two basic circuits, a machine casing and a diamond pickaxe.

## Balance and automation
- **Survey:** every second column is sampled, from the bottom of the world to 16 blocks above the player.
  - The sample count becomes a signal of 1–5.
  - A quarter of the time the signal is nudged one step up or down, so two surveys in one place can differ.
  - The depth band is from the average height: shallow at Y ≥ 40, middle at 0–39, deep below 0.
  - There is a 3-second cooldown.
- **Drill:**
  - One ore block every 40 ticks at 32 JE/t (1,280 JE per ore). Speed and efficiency upgrades apply.
  - It checks one 81-block layer per tick while looking for the next ore.
  - It only takes blocks in `c:ores`. Deepslate ores are refilled with deepslate, netherrack ores with netherrack, and everything else with stone.
  - **No free metal:** a drilled ore equals a silk-touch mined one.

## Multiplayer and persistence
- Server-authoritative. The survey runs on the server; the client only gets the vague readings (icon, signal, depth), never positions.
- The drill saves how far it has got (`cursor`) and its items, energy, sides and upgrades.
- The drill does not check claims or spawn protection. On a shared server, add it to the claim plugin's block rules before release.

## Dependencies and assets
Fabric API networking, the transfer API and the `c:ores` convention tags. Original models and textures (MIT).

## Verification
- `tools/check_mod_data.py` passes.
- Game tests (CI):
  - `surveyFindsNearbyOre`
  - `oreDrillMinesOreBelow`
- The client game test screenshots `jugcraft_prospector` from a real survey.
- Not run: client play, two players, performance under many drills.

## World and event applicability
Not applicable.

## Rollout and open questions
- Claim and protection integration for the drill.
- The drill mines without a visible shaft. A drill-string animation or particles are a later polish.
- Survey range upgrades (bigger radius, filters) could come later.
