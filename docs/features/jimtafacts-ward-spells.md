# Jimtafacts defensive and travel spells

Implementation by OpenAI Codex (GPT-6), 23 October 2026. Local only; no build,
tests or game launch has run for this change yet. Execute the prepared tests as
part of the owner's requested final combined validation.

## Player outcomes

The supplied twelve spells now have distinct native implementations:

| Spell | Native behavior |
|---|---|
| Abyssal Shroud | Six seconds of attack evasion into collision-free, loaded, permitted ground; bounded search, no fall/void immunity. |
| Arcane Shackle | Travelling bolt creates a fifteen-health breakable anchor; source `.015` restraint force, eleven-second lifetime and twelve-block break distance. |
| Heartstop | Ten seconds of post-armour damage deferral. Persistent debt is repaid once on expiration or cleansing, including after reconnect. Death clears the obligation normally. |
| Planar Sight | Forty seconds of viewer-only outlines for living creatures within 32 blocks, plus supplied ash particle behavior. |
| Pocket Dimension | A private, persistent room allocated once per player from the original 16×32×16 structure; portal/frame returns to the saved entry point. |
| Telekinesis | A held continuous cast tracks one permitted enemy and applies the source interpolated attraction formula; bounded speed, loaded destinations, claims and PvP. |
| Evasion | One automatic dodge to safe permitted ground within a minute; excludes fall/void/bypass damage. |
| Portal | Paid-once two-cast link within two minutes. Temporary endpoints last five minutes; crafted frame links persist. |
| Recall | Four-second out-of-combat return to the existing bed/anchor respawn route, falling back to spawn through vanilla's resolver. |
| Invisibility | Ten seconds of concealment including equipment/name rendering; mobs lose and cannot reacquire the target. Damaging another entity reveals the caster. |
| Scapegoat | Original animated twenty-health decoy runs toward the aimed ground for fifteen seconds; permitted nearby pursuers target it, no drops. |
| Shield | Source fifteen-health, twenty-second shield plane; original two-layer geometry and animation, projectile interception and crossing-attack blocking. |

All casting stays in the existing spellbook/Spell Engine selector and pays shared
Focus. Level-one effect sizes, cast animations and source cooldowns are retained;
Focus replaces the separate source mana pool. Telekinesis pays one Focus every
two ticks. Recasts do not spend again. Creative/administrative bypasses are not
introduced.

## Original art and source inspection

The supplied `originals/Oct23/Jimtafacts/assets/irons_spellbooks` textures,
Scapegoat geometry and animation are copied byte-for-byte. Original class files
were inspected statically with trusted CFR, never loaded or executed:

- `ShieldModel`, `ShieldTrimModel`, `ShieldRenderer`: seven surface cuboids,
  sixteen trim cuboids, original 64×32 UVs and weighted sine swirl formula.
- `PortalRenderer` and `PortalFrameRenderer`: original one-by-two-block quads,
  ten horizontal frames, two ticks per frame; frame surface offset `.40625`.
- `EnderChainRenderer`: two crossed textured planes with the source three-tick
  appearance and repeating chain coordinates.
- `ScapegoatModel`: original four-leg cosine gait (`.6662`, `1.4`) added to the
  original `scapegoat_run_accent` keyframes, triggered every twenty ticks.
- `PortalFrameBlock`: original two-block halves, slab outlines and open-centre
  collision shapes. The source square/colour portal textures provide dyeing.
- `data/irons_spellbooks/structure/pocket_room.nbt`: coordinates and palette
  indices are retained; only namespace/current block-state serialization is
  adapted. Voidstone connection states and frame halves are registered rather
  than substituted. There is no procedural replacement room.

Source archives and retained notices stay in the owner library. Generator copy
records are part of the common spellbook resource manifest.

## Persistence, permissions and bounds

Frame links save reciprocal UUID identities and dimension/position. Replacing a
frame at the same coordinates cannot hijack the old link. Pairing requires both
owners/party permissions and current claim permission; dyeing and unlinking use
the same checks. Endpoint travel also checks the traveller's permissions. An
unloaded destination waits; frames never force-load it. Players may unlink with
sneak-use and an empty hand. Dye consumes one dye only when the colour changes.
Only connection state and colour are synced, never owner IDs or destination data.

The frame recipe keeps the source pattern and two-frame result, using existing
arcane ingots and an ender pearl. Netherite ingots replace the unavailable source
mithril ingot; no duplicate metal/progression is introduced. Two-block breaking
drops one frame and invalidates the reciprocal connection.

Pocket rooms have a saved owner allocation, 256-block spacing and 2,048-room
server cap. Only explicit first entry places a template. Return points persist
and respawning exits the pocket. The room's wall/frame cannot be harvested;
entering other rooms is prevented. No background generation or chunk loader is
installed. Portal travel has a shared forty-tick lockout to prevent immediate
return loops, including when the player leaves by using the frame.

Heartstop debt persists independently from the effect and is cleared before the
single repayment call. Its damage type bypasses armor/effect/invulnerability
reductions so defense is not applied twice. Totem/death handling still passes
through the ordinary damage pipeline. Deferred amount is bounded to one million;
overflow is dealt immediately. No save/load or cleanse route simply deletes debt.

Entities and searches are bounded: eight nearby ward entities per caster,
four decoys, sixteen travellers per portal pulse, 64 projectile candidates per
shield tick, 128 mob targets for concealment cleanup. Shackle/telekinesis verify
current damage, claim, party and loaded-destination authority throughout use.

## Adaptations and remaining source differences

Portal travel currently carries players, not arbitrary mobs, vehicles or dropped
items. Frames retain source geometry and connected effects but do not introduce
the source automatic cross-dimension chunk loading. Shackle has one breakable
anchor instead of eight separately pickable NeoForge chain parts. Shield's native
interception charges four health per intercepted projectile; direct/hitscan
crossing attacks use their real incoming damage. Its mesh and collision plane
are source-derived, but it does not register 25 NeoForge multipart hit targets.
Safe dodge requires a valid destination and caps displacement to twelve blocks.

Planar Sight implements creature outlines and ash but not the old source custom
fog function. The private realm currently uses the native dark fog/sky settings;
the original supplied sky/cloud textures are preserved but are not yet a custom
sky renderer. These differences are not claimed as original-engine parity.

## Final validation to run

- `JimtaWardGameTests`: Heartstop capture/repayment, concealment target lifecycle,
  rotated shield crossing, protected telekinesis, frame binding and sanitized sync.
- `scripts/tests/test_jimta_ward_assets.py`: byte-identical models/animations/skins,
  unchanged original room layout, and debt repayment tags.
- Combined client/server playtest: every spell's original cast animation; Scapegoat
  gait/accent; moving chain/shield/portal visuals; Portal across dimensions and
  chunk unload/reload; frame dye, upper/lower breaking and reconnect persistence;
  pocket room return, death and obstruction; adversarial two-player claims/PvP;
  Heartstop cleanse/reconnect/totem; invisibility from a second player's camera.

None of those results is represented as passed until the combined run occurs.
