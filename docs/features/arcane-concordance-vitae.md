# Arcane Concordance: Crimson resources and living equipment

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 16) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 8, Practitioner stage (Jugcraft Workshops). Roadmap step 16.
Primary specialty and supported player role: the Crimson Vigil. Power at a personal cost: useful in an emergency and for
a specialist's weapon, never a general-purpose generator.

Builds on [typed resources](arcane-concordance-sharing.md) (Vitae), [research and notes](arcane-concordance-sharing.md)
and Focus ([First Light](arcane-concordance-first-light.md)). Contract and checklist:
[ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

Once First Light is understood, examining something that bleeds or stings (sweet berries, crimson roots, crimson fungus,
nether wart, a spider eye) begins **Crimson Rites**; studying one at a Lampwright's Bench (or reading notes) understands it.

**Three things, kept apart.** **Health** is vanilla's, healed by anything. **Vitae** is what an offering makes of it,
held in a **Crimson Chalice** (gold, redstone, a glass bottle; up to 32). **Offering exhaustion** is what offering leaves
behind: it clears one point every two minutes and by nothing else. The HUD (while a chalice or blade is held) and
`/jugcraft concordance vitae` say all three apart, with how long until exhaustion clears; never as one shared bar.

**An offering** (use the chalice): 4 health for up to 4 Vitae, plus 3 points of exhaustion. It never leaves you below 8
health, never comes within 5 seconds of the last, and takes no health when the chalice has no room. What it yields falls
with your exhaustion: 0 to 3 points, all of it; 4 to 7, half; 8 to 11, a quarter; at 12, no offering at all (rounded
down). Healing to full does not reset any of this, so a careful offering now and then gives far more than many in a row.

**Crimson Surge** (sneak and use the chalice): 6 Vitae become 6 Focus at once, at most once a minute and never beyond
full Focus. For the moment a fight runs you dry.

**The Thornheart Blade** (an iron sword, sweet berries, nether wart, redstone) is living equipment. It grows through
three kinds of deed: **slaying** hostile creatures, **enduring** harm while you wield it, and being **nourished** (use the
chalice with the blade in your other hand: 2 Vitae for 8 vigor). Each deed pays by how often its kind of creature or
harm has come up that day (4, 2, 1, 1, then nothing), each kind of deed pays at most 24 a day, and the stages need
several kinds: **Awakened** (20 points, two kinds with 5), **Grown** (60, every kind with 10), **Flourishing** (150,
every kind with 30). Each stage adds 1 attack damage while the blade has **vigor**; every blow on a living creature
spends 1. Without vigor its powers sleep and it fights as iron, but nothing it has grown is lost. Growing a blade you
wield to its second stage masters Crimson Rites.

## How it works

**Data** (`data/<ns>/concordance/offering/`, read by `CrimsonCatalog` in `concordance/crimson`): a rite's health,
Vitae, exhaustion, floor and cooldown. The loader refuses a rite that gives more Vitae than the health it takes, adds no
exhaustion, has no floor, or names an unknown field.

**Offerings** (`crimson/Offerings.java`, pure): an offering is refused, with no health taken, when it is too soon, past
the exhaustion limit, would leave the giver under the floor, or would yield more than the chalice holds. Otherwise its
yield is the rite's Vitae times the efficiency for the exhaustion before it, rounded down. The Vigil takes the health
with `setHealth` in exactly one place (`Vigil.offer`), so it never passes through armour, totems or death, and nothing
else in the Vigil touches health.

**Exhaustion** (`crimson/Exhaustion.java`): points and a stamp, worked out from the game time when read (no per-player
ticking), saved on the player (`jugcraft:offering_state`, kept through death so dying never resets it, synced to that
player alone for their HUD).

**Growth** (`crimson/Growth.java`, the blade's `jugcraft:growth` component): per-kind points, today's subjects (at most
32 remembered, keyed by game-time day so turning the clock changes nothing) and vigor. Deeds come from events on the
server: a hostile creature's death credited to a player wielding the blade (`ServerLivingEntityEvents.AFTER_DEATH`) and
damage a wielder survives (`AFTER_DAMAGE`, keyed by damage type). The blade's attack bonus is its own attribute modifier
(`jugcraft:thornheart_growth`), rebuilt whenever its stage or vigor changes, so vanilla applies it exactly once when the
blade is taken in hand and removes it once when it is put away.

## Connections

- Input producer: First Light (Crimson Rites needs it understood); the player's own health; any healing (food,
  potions, regeneration, other mods' spells) refills health but never exhaustion.
- Output consumer: Focus (the surge), the Thornheart Blade's vigor and growth; Vitae is a typed resource
  (`ResourceKind.VITAE`) that later steps can draw on.
- Technology connection: crafted from gold, redstone, glass, an iron sword and Nether plants.
- Magic connection: Focus for every invocation and composed spell; a typed resource kept apart from Ley Charge,
  essences and Astral Resonance.
- Reachable entry path: First Light understood → examine sweet berries or crimson roots → study at the bench → craft a
  chalice.
- Solo, trade and cooperative routes: entirely solo; chalices and blades can be traded (a blade keeps its growth).
- Specialty use without other branches: needs only First Light.

## Balance

- **No unlimited generator**: Vitae comes only from offerings, never more than the health taken, and an hour of
  offering gives at most `(12 + 30) / 3 = 14` offerings at 4 Vitae, 56 Vitae, whatever heals the giver;
  `tools/check_mod_data.py` checks every rite against a bound of 60 an hour. The step 16 harness offers as often as it
  is allowed for an hour: 10 Vitae with no healing (the floor stops it), and 20 whether the giver heals a point a second
  or is healed to full every second: healing changes nothing once exhaustion is the limit.
- A surge gives at most 1 Focus per Vitae and at most once a minute; natural Focus returns 90 an hour.
- **No trivial development**: two hundred zombies in a day pay a blade 8 points; ten kinds of enemy pay the daily cap
  of 24; a zombie farm run for 40 days never wakes a blade (one kind of deed cannot reach any stage); varied use for 40
  days reaches Flourishing (harness).
- The blade's best bonus (+3 at Flourishing) costs vigor, which costs Vitae, which costs health and time.

## Multiplayer and persistence

- Server authority: offerings, surges, deeds, growth and vigor are the server's; the client shows the synced state and
  plays the gesture. Uses are rate-limited.
- Persistence: Vitae on the chalice (`jugcraft:vitae`), growth on the blade (`jugcraft:growth`), offering state on the
  player (`jugcraft:offering_state`, kept through death).
- PvP: nothing here hurts another player; the blade is a sword under the server's own PvP rules.
- Disable behaviour: with `concordance.enabled=false` the chalice does nothing, deeds stop counting and the blade
  spends no vigor; everything stays registered.

## Dependencies and assets

No new dependency. Framework use:

- **Player Animation Library**: an original offering gesture (`player_animations/vigil_offering.json`: the chalice
  hand lifts to the chest, the other hand presses to it, a breath, both lower), on a Jugcraft layer of its own, played
  by every client that sees the player when the server says an offering was made (`VigilGesturePayload`).
  **ArmsMotion coordination**: ArmsMotion poses the arms of a player holding a Jugcraft arm; the gesture is skipped for
  such a player, so the two never pose the same arms at once (the offering itself still happens). The Thornheart Blade
  is not one of ArmsMotion's arms, so it keeps vanilla's sword animation.
- **Modonomicon**: a new **Vigil** codex category (Crimson Rites, offerings and efficiency, Crimson Surge, the
  Thornheart Blade).
- **Spell Power, Trinkets**: not used. The blade's bonus is a plain attack-damage modifier (it is a sword, not a
  spellcasting item), and none of these items is an accessory, so a Trinkets slot would add nothing.
- **Spell Engine healing**: needs no special case. Exhaustion is independent of health, so no source of healing, Spell
  Engine's included, can reset the offering loop.
- **Jade**: nothing to show: the Vigil has no blocks or creatures. The HUD line, tooltips and the `vitae` command
  distinguish health, Vitae, exhaustion and recovery.
- **GeckoLib**: not used yet. A GeckoLib-animated blade (a heart that beats with its vigor) needs GeckoLib's item
  renderer, which could not be checked without a local build; the blade is a plain item model for now (an open item).

Art: two 16x16 item icon maps in `tools/item_icons/` (the chalice in vanilla gold with garnet Vitae; the blade in iron
with crimson veins, a garnet guard and a leather grip, on the outline of Jugcraft's own greatsword map), drawn by
`tools/concordance_crimson_art.py`. Nothing is taken from the owner's library or from Mojang's files. The icons have not
been shown to the owner yet.

## Verification

- `python3 tools/check_mod_data.py`: new step 16 checks (`check_crimson`): the Java exhaustion, efficiency, growth,
  stage, chalice, surge and blade numbers equal the generator's; every rite gives no more Vitae than the health it takes,
  can be made from full health and stays under the hourly bound; a surge gives no more Focus than its Vitae; health is
  set in exactly one place; mastery comes from the blade's growth; every message, stage and HUD word has its text; the
  icons are their maps; the gesture's clip exists and the client skips ArmsMotion's players. The pure-package rule
  covers `concordance/crimson`.
- The pure core compiles with JDK 21. The step 16 harness passes **34 checks** against the generated data: the rite
  loads; exhaustion recovers a point a period, keeps partial recovery when added to and never passes its limit;
  efficiency falls by band; each refusal (too soon, too weak, a full vessel, exhausted) and the half and quarter yields;
  the hour of offering above; two hundred zombies pay 8, ten kinds pay the daily cap, a day remembers a bounded number of
  subjects, one kind alone never wakes the blade and two kinds do; forty days of varied use reach Flourishing while a
  forty-day zombie farm never wakes it; nourishing fills vigor and counts as a deed; and the parser's refusals.
- Game tests added: `ConcordanceVigilGameTests` (seven): healing does not reset the offering (4 health for 4 Vitae and 3
  exhaustion; not again at once; healed to full, the second still counts the first's exhaustion; the third yields half);
  an offering keeps its limits (the floor, a full chalice, a novice); exhaustion clears only with time (refused at the
  limit, healing changes nothing, a quarter after three periods); a surge turns 6 Vitae into 6 Focus, not again within a
  minute, never beyond full; the blade grows by varied deeds only (fifty zombies pay 8 and leave it dormant; varied deeds
  wake it; fed, it carries its modifier; a blow spends vigor; days of varied use grow it to its second stage, which
  masters Crimson Rites); a real kill with the blade counts (the death event); and Vitae, growth and offering state
  survive a save.
- CI: run 37610863921 (commit 0a73cf15) passes the whole Build workflow: it builds, passes the data checks and all 981
  required server game tests (the seven above among them), and the client test shards pass.

Not yet run: any client (the gesture, HUD line, icons and codex pages in game), a two-client dedicated server, and an
hour of real offering against the bound.

## World and event applicability

No worldgen, creatures or seasons.

## Rollout and open questions

- New stable ids: items `jugcraft:crimson_chalice`, `jugcraft:thornheart_blade`; components `jugcraft:vitae`,
  `jugcraft:growth`; attachment `jugcraft:offering_state`; attribute modifier `jugcraft:thornheart_growth`; research
  `jugcraft:crimson_rites`; practice `jugcraft:living_growth`; rite `jugcraft:offering`; item tag
  `jugcraft:crimson_specimens`; payload `jugcraft:vigil_gesture`; player animation
  `jugcraft:vigil_offering`.
- Save compatibility: additive.
- Known limit: the blade is not in `#minecraft:swords` (the Concordance's tag writer would replace the arms
  generator's file), so sword enchantments are not offered for it at the table.
- Owner review: the icons, the offering numbers and blood magic as the Crimson Rites' theme were approved as built on
  7 October 2026.
- Open: a GeckoLib blade.
