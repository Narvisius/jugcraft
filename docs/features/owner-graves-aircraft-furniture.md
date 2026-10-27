# October 22: town motion, protected graves and owner furniture/aircraft

Status: combined release candidate, based on live main `f904df3fd3ca0b0631ed604f7010f0624220f76a`.
Not deployed, merged, published or promoted to the player updater. AI assistance: OpenAI Codex (GPT-6).

The town fixes below already shipped in `0.1.0-alpha.20261022.3`. This candidate adds planes, furniture and graves; the separate Nether repair work is excluded.

## Town fixes

- Sleeping townsfolk reject movement input and navigation while asleep; roaming, decorating and return-home goals cannot restart during sleep.
- City flak selects only targets with a valid direct firing solution. The high-angle fallback remains available to player-operated artillery, but city crews do not use it against unreachable nearby targets.
- Guard walkers measure displacement between client ticks, which includes network interpolation. Their existing articulated legs animate from that displacement. Teleports do not count as walking.
- A seated, uniformed town guard is drawn in the walker cockpit.

## Death graves

The original owner's marker model/textures are retained. Jugcraft captures native death drops after Minecraft has applied death rules, rather than clearing inventories in a generic death event.

- Personal `/jugcraft keepinventory`, global keep-inventory, spectator deaths and boss-lair dimensions bypass ordinary graves.
- A boss-lair death keeps everything in place, as keepInventory does, there only ([lair-keep-inventory.md](lair-keep-inventory.md)). Armour, offhand, Trinkets slots, hotbar layout and all experience stay with the player, so nothing normally drops there.
- Anything still dropped in a lair (by another mod, say) goes through the same exact native death-stack interception. It is routed into the existing Grave Goods attachment instead of a grave. It does not rely on finding newly spawned item entities nearby, and cannot collect an unrelated nearby item as the dead player's belongings.
- Grave Goods are returned on the server tick after respawn, once Fabric has copied the player's death-persistent attachments. This also recovers retained goods on an alive player's reconnect outside a lair; clearing the attachment prevents repeat collection.
- Totem saves do not run the death-drop capture. Vanishing and accessory drop rules still run before capture.
- Main inventory, equipment, offhand and actual dropped accessories are stored in world saved data with their complete item components.
- Only the owner can recover a grave. Full inventories leave remaining items safely stored. Recovery is repeat-safe.
- Markers do not overwrite blocks or claims, force-load chunks, or expose an inventory to hoppers. If no safe marker position exists, the stored items remain recoverable through `/jugcraft graves`.
- Marked graves require the owner to reach the loaded grave area. If an administrator removes a marker, its stored contents survive; recovery never removes a replacement block.
- Experience follows existing Minecraft rules. This is item retention, not an XP insurance change.

## Supplied content

Originals are preserved locally under `art/owner-library/originals/Oct22/`; the intake manifest records 15,870 files with hashes. Old namespaces, notices and credits are retained in the original collection. Supplied compiled classes were read as static compatibility references, not executed.

### Ready-made furniture

267 registered blocks, 18 associated items, 317 adapted crafting/recoloring recipes and 28 painting variants. Runtime copies retain supplied models and textures. Native/shared Jugcraft systems implement connected furniture, real beds, seating, 27-slot storage and furnace cooking. The hammer cycles supplied variants or rotates furniture; cushions, sheets and dyes change supported upholstery. Stacked ornaments drop the corresponding item count. Furniture also joins the supplied vanilla bed/tool and conventional dye tags.

Compatibility adaptations are confined to runtime copies: modern loot keys, corrected reversed box bounds, separation of overlapping faces, a native bow-sprite atlas alias, and drawing tall trophies across two blocks without drawing the entire figure twice. The tall witch trophy retains the full hat; only its inventory miniature is scaled to fit Minecraft's supported model bounds.

### Aircraft

Seven craft: Airship, Bamboo Hopper, Biplane, Cargo Airship, Gyrodyne, Quadrocopter and Warship. The 27 supplied aircraft/component items and supplied recipes are registered. Supplied Blockbench meshes, texture layers and rotor/control-surface animation expressions are adapted to the Jugcraft renderer. Cargo layouts retain their supplied capacity, including the 80-slot cargo airship.

Server-authoritative flight, fuel, ownership/party access, saved inventory, constrained equipment slots, upgrade modifiers, mounted weapon handling and a cargo/equipment interface are implemented locally. Sneak-use opens equipment; use boards. WASD steers, jump climbs, sprint descends, attack fires an equipped weapon, use activates a booster or an equipped telescope. Fixed-wing craft need forward speed to climb. Stale input expires. Ammunition is paid by the server, shots are range-limited, and bombs use Jugcraft's non-terrain-damaging blast system.

The Gyrodyne is muscle-powered: pedaling spins its rotor, consumes hunger and stops working when the pilot is too hungry. Supplied engine audio follows powered craft. Equipped banners use vanilla's pattern layers on the supplied meshes; weapons follow the pilot's aim and recoil. Gyroscope instrument upgrades enable flight readouts. Large cargo screens scale to fit smaller windows, including their mouse coordinates. Undyed craft retain their authored colors.

## Artisan workstation and local design library

The supplied workstation model and material icon are retained. A native Fabric editor now offers front/top previews, named designs, cuboid and flat-panel parts, position/size controls, eight vanilla materials and stool/table/lamp/cabinet presets. Save, browse, read-held and delete-saved operate on a bounded local library, `jugcraft/furniture/library.json`; no remote design service or executable imports are involved.

Build pays the server-computed material cost. Recycle returns 75% of the cost. Server checks workstation proximity, claim permissions, inventory space and request rate before changing items. Designs retain their name and parts on placement, breaking, creative picking, saving and grave recovery. Optional features are a seat, 27-slot storage, toggleable light, three bounded particle effects and three interaction sounds. Storage takes priority over seating; sneak-use toggles lights.

This is a bounded native adaptation, not full source-editor parity: one-block designs, at most 32 parts, 64 saved designs and eight material presets. Oversized/rotated individual parts, arbitrary textures and the source online marketplace are not included. Custom furniture currently uses a common inventory icon; the placed design renders its actual geometry. Native validation rejects oversized, malformed and deeply nested input.

Recipes: the artisan workstation uses iron, planks and a crafting table; two planks and a stick make eight furniture materials. `tools/artisan_furniture.py` reproduces wrappers and copies supplied workstation art without changing the originals.

## Acceptance and deployment

- Complete combined checks and an isolated existing-world loading/saving rehearsal; record exact results below.
- Owner playtests flight feel, seating, audio and the custom editor. The flight model is a native adaptation, not exact parity with every source equation.
- Real two-account hosted acceptance remains separate from simulated-player server tests.
- Ask the owner before any live stop/restart. After confirmation, warn connected players and allow the full 30 seconds before stopping. Back up, replace only managed changes, verify startup/join, then promote the matching client update. Keep world, settings and extra mods.

## Validation log

### Aircraft and Nether follow-up

Empty-hand use repairs a damaged unoccupied craft by 2.5% of its maximum hull,
with a five-tick cooldown and 0.5 exhaustion in survival. Sneak-use still opens
equipment. Ammunition uses the supplied 100-credit loads: cannon costs 10 per shot,
crossbow 50 per volley, bombs 20 per volley. Remaining credits and arrow components
survive saving. Every configured weapon mount fires with an independent cooldown.
Native normal/tipped/spectral arrows preserve their effects and cannot be collected
as free ammunition. Authority checks run before impact effects. Cannons now launch
moving rounds with collision, lifetime/range limits and no block destruction;
bombs use existing claim-aware artillery. Equipment access is rechecked against claims.

Durability and acceleration are applied as source multipliers rather than absolute
values; Gyrodyne speed no longer multiplies the already-large muscle speed by 25.
Vertical stability uses its supplied upgrade modifier. The native flight model remains deliberately distinct from the full source equations. Handling needs owner feedback. The October 22 follow-up corrects source cockpit pivots so the hull, seat and weapon mounts share a ground clearance offset.

All 48 focused server regressions passed (`biome-aircraft-review/tests-2.log`),
including ammunition/save behavior, spectral projectiles, moving cannon collision
and lifetime. Nether repairs were tested in that older broad local batch; they are excluded from this isolated planes/furniture/graves candidate.

### Earlier batch validation

- Initial focused server regression run passed all 24 tests after correcting a real death-drop interception bug. The original hook caught accessories but missed the main inventory; the corrected hook captures the native item-creation path shared by inventory, armor, offhand and accessory drops.
- The first rendered client gallery passed and produced 27 screenshots. Visual inspection caught a clipped cargo screen, backward-facing pilot and legacy trophy/atlas errors; those were corrected afterward. That first run is not final visual acceptance of the refinements.
- The full offline audit caught legacy furniture loot keys, rectangular-skin assumptions and tall model bounds. The modern loot conversion, texture-dimension handling and tall-model adaptation have been corrected. A subsequent focused art check examined 22,189 block models and reported zero crafted-furniture errors; a separate bounds scan found zero invalid trophy elements.
- The expanded server regression run passed all **26 required tests** (`regressions-7.log`), including actual death capture, accessory retention, dead-owner recovery rejection, save-data round trips, keep-inventory/totem exemptions, furniture drops and muscle-powered flight.
- The full offline audit passed **6,553 material IDs**, data and recipes; it inspected **22,189 block/item models**, 3,729 quad parts and 176 combined models (`data-audit-4.log`). The subsequent client check exposed aircraft item-atlas omissions and an obsolete chain sprite; these runtime references are corrected without redrawing the supplied skins.
- The second client gallery confirmed the pilot faces forward and the tall trophies render. Its cargo-click assertion used the old mouse-button number instead of the current left-button constant; that test was corrected. The gallery also exposed a duplicate unscaled cargo background, now fixed.
- Repository structure/documentation checks passed. No dedicated two-player acceptance or hosted upgrade has been performed for this batch.
- The third client run passed the scaled cargo click and rendered all seven aircraft/screens without the new-content missing-texture warnings. Its actual boss death uncovered a weakness in the existing nearby-drop recovery; the native capture now hands those stacks directly to Grave Goods. The next run exposed the respawn-listener ordering problem, corrected by returning goods after attachment transfer.
- **Final combined client run passed**: `OwnerBatchClientGameTests,LairClientGameTests` (`client-5.log`, 3m 37s). It exercised the 267-block gallery, seven aircraft and equipment screens, actual mouse pickup from the scaled cargo screen, real boss-dimension death and respawn with exactly three diamonds returned and no ordinary grave or remaining claimable bundle, plus the existing Hollow Acre end-to-end suite. Final screenshots are retained in the local `guard-graves-review/screenshots-final` evidence folder.
- **Final expanded server run passed all 38 required tests** after both boss-recovery fixes (`regressions-8.log`): DeathGrave, CityDefense, TownLife, PlayerRules, OwnerFurnitureAircraft, Lair and LairEncounter. These are focused regressions, not the entire repository's server suite.
- The same run successfully assembled `build/libs/jugcraft-0.1.0-alpha.jar` as a local development artifact. It is not a player release and has not been installed into the owner's Modrinth profile, uploaded to the server or promoted to the updater.


### Isolated October 22–23 candidate validation

- All 46 focused combined server tests passed, including workstation payment/refusal, saved cabinet contents and break drops, custom furniture/aircraft grave recovery, aircraft cargo/ammo, keep-inventory exemptions and lair recovery.
- The three-class rendered run passed: artisan editor save/build/delete and storage sync, all 267 ready-made furniture blocks, all seven aircraft/equipment screens, scaled cargo interaction and real lair death/respawn. Screenshot review then corrected aircraft cockpit-origin ground offsets; the final visual rerun follows that correction.
- The first full server sweep found a recipe collision: three wool matched both sheets and backpack sleeping bags. Furniture sheets now also require string; the existing sleeping-bag recipe remains intact. A regression covers the distinct sheet recipe.
- The resource audit passes 6,556 material IDs and all data/recipe checks. It caught and corrected a missing Nether Engine resource/recipe and modern workstation loot fields. No Nether terrain changes are included.
- Acquisition records were refreshed from the actual server registry: 315 new item types, zero invalid ingredients and a documented paid-workstation route for custom designs. The value catalogue adds those 315 records without changing any existing item value; convenience-only transmutation output restrictions remain unchanged.
- Windows contributors with long checkout paths should use Git's `core.longpaths=true`; one preserved compiled source-reference path exceeds Windows' legacy 260-character limit. Those files are not shipped or executed by Jugcraft.

- Final focused server run passes all **51 required tests**, including all 16 sheet/sleeping-bag recipe pairs, seven-aircraft powered/unpowered flight, artisan, graves, town and lair regressions (`combined-final-focused.log`). The preceding full sweep passed 2,281 of 2,282 tests; its sole failing sleeping-bag case is fixed and covered by this focused rerun.
- Final economy/acquisition regression checks pass all **15 tests**; acquisition reports still show zero invalid ingredients. The earlier full packaging/Python suite passed all 195 tests and Windows updater helper checks passed 79 assertions.

- The corrected ground offsets passed the three-class client rerun (`combined-final-client.log`). Screenshot review then found an omitted artisan atlas directory. That registration is fixed; the focused editor rerun (`artisan-final-atlas.log`) passes and renders the actual built cabinet, workstation and lamps without the artisan missing-texture warnings.
- Canonical regeneration originally removed the new language, aircraft sound, mining-tag and atlas additions. `owner_workshop_shared.py` now reapplies narrowly scoped metadata after shared generation, rejects conflicting entries and is idempotent. A full canonical generation followed by reconciliation reproduced all five expected resource objects; only the intended artisan atlas addition differs from the prior commit.
- Existing-world rehearsal on a **copy of the earlier local test world**, bound only to `127.0.0.1:25574`, loaded, saved, stopped and restarted successfully. A drawer retained exactly seven diamonds, and its workstation and cargo airship persisted. Evidence: `workshop-world-first-start.log` and `workshop-world-restart.log`. This is not a hosted-world/two-account test. Nineteen of 21 pre-existing config files stayed byte-identical; Jugcraft rewrote its timestamp without changing settings, and RPG Series regenerated its diagnostic loot report.
- All 16 GitHub client shards and the full optional-integrations-absent server build passed on `1b76341b1b`. The remaining mod job stopped at the shared-regeneration check, now fixed. The final branch push must complete its own CI before merging; these earlier results do not substitute for that gate.
- The final candidate is prepared locally as `0.1.0-alpha.20261022.4`; publication, channel promotion, hosted two-account acceptance and the owner-confirmed live restart remain separate release steps. No world reset is included.
