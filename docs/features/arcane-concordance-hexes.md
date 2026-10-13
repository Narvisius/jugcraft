# Arcane Concordance: sympathy, curses, wards and dreams

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 22) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 14, Practitioner stage (Jugcraft Workshops). Roadmap step 22.
Primary specialty and supported player role: the Hexweavers (Tether, Hollow), who link, curse and ward, and the
Dreamwalkers (Echo, Tide), who dream and bring dreamglass back. Countermeasures (the scrying glass, remedies and ward
sigils) are for everyone.

Builds on the [shared effect boundary](arcane-concordance-composition.md) (every curse pulse and the moving ward go
through it, with its multiplayer rules) and First Light. Contract and checklist:
[ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md). Not to be confused with the cauldron's
[hex brews](even-more-fall-additions.md#hex-brews), a separate feature.

## Player experience

Once First Light is understood, examining a likeness (a fermented spider eye, a cobweb, a poisonous potato, a rabbit's
foot, an armor stand) begins **Sympathy**; examining something from the edge of dreams (chorus fruit, an ender pearl, a
white bed, a spore blossom) begins **Dreamwalking**. Studying either at a Lampwright's Bench (or reading notes)
understands it.

**Links.** A **taglock** (a bottle, string and an iron nugget) used on a creature takes a **link** to it: strength 100,
losing one every 12 s, so it fades in twenty minutes. Never to yourself, never to a player the server's rules do not let
you harm (PvP off, or your party), never to anyone warded against linking.

**Curses.** With a linked taglock in your main hand and a curse's reagent in the other, use the taglock:

| Curse | Reagent | Pulse | Lasts | Link | Focus | Remedy |
|---|---|---|---|---|---|---|
| Lethargy | cobweb | Slowness I for 10 s every 20 s | 5 min | 30 | 6 | sugar |
| Misfortune | rabbit's foot | Bad Luck I for 30 s every 30 s | 10 min | 20 | 4 | amethyst shard |
| Frailty | fermented spider eye | Weakness I for 10 s every 20 s | 5 min | 40 | 8 | blaze powder |

A curse is cast only if, at that moment, the link is alive and strong enough, its target is found in your dimension
within 128 blocks, the rules let you harm it, it is not warded against cursing, it carries fewer than three curses and
not this one, and you have the Focus. The reagent and Focus are taken only then. Each pulse is checked again: it skips
while you are offline, in another dimension or more than 128 blocks away, or while the bearer is warded; it lifts for
good when its time is over or the rules stop letting you harm them (PvP switched off, or you joined their party). A
cursed player is told only that something weighs on them.

**Investigation and remedies.** A **scrying glass** (anyone may make and use one) used with an empty other hand
looks at the curses on you, for 2 Focus a look: the first names each curse and its remedy, the second names who cast
it, unless the caster is warded against scrying. With the remedy in your other hand it lifts the curses that remedy
answers (the remedy is spent). With a taglock in your other hand it reads the link: its strength, and where its
target is (unless they are warded against scrying). `/jugcraft concordance hexes` lists what you know.

**Wards.** A **ward sigil** (paper, dreamglass and a category's ingredient) wards you for twenty minutes against one
kind of operation: linking (string), cursing (amethyst shard), scrying (ink sac) or moving (iron ingot: no one else's
Concordance effect pushes you). A fresh sigil renews a ward; wards never stack.

**Dreams.** At an **Oneiric Censer** (gold, a phantom membrane and dark-oak slabs), at night, a Dreamwalker in survival
or adventure mode with 10 Focus and nothing worn in an accessory slot uses the censer to dream. Everything they carry
(hotbar, inventory, armour, off hand) goes into one escrow on them; they dream in adventure mode with nothing, for at
most three minutes, within 24 blocks of their body. Three dream wisps gather at once and one more every 30 s; catching
one (touching it) is one dreamglass, at most eight. Using the censer again wakes them.

However a dream ends, they wake with exactly what they carried, in the same slots, where their body lay, and never
with more experience than they had: experience gained in the dream is gone, and experience spent there (an
enchantment, a repair) stays spent. Anything picked up while dreaming falls at their feet. The dreamglass caught comes
back: all of it on waking, when the time runs out, on leaving the server or when a crash is found on the next join;
half on straying (more than 24 blocks, or another dimension) or being hurt; none on dying, and then the body dies with
what it really carried.

## How it works

**The rules** (`concordance/hex`, `concordance/dream`, pure Java). `Hexes` decides, from a `Situation` the server
fills at the moment of use (target found, same dimension, distance, warded, allowed, time), whether a link may be
taken, a curse cast, a pulse given, skipped or lifted, and what an investigation learns; every refusal has a reason.
`Link` fades; `Ward` guards one `WardCategory` until it ends and renews rather than stacks. `DreamRules` decides when a
dream must end, how many wisps have gathered, what it brings back, and which experience the dreamer wakes with.
Curses are data (`data/<ns>/concordance/curse/`, read strictly by `HexParser`: at most level II, pulses of at most
30 s at most every 5 s, at most ten minutes, one curse per reagent, no curse lifted by its own reagent).

**Links, curses and wards on the server** (`concordance/sympathy`). A taglock holds its link as a data component
(`jugcraft:taglock`); a ward sigil names its category in `jugcraft:ward`. Curses live on their bearer
(`jugcraft:curses`, lost on death) and wards on their player (`jugcraft:wards`). Each second the server checks every
loaded cursed creature: each curse is revalidated (`Hexes.pulse`) and its status given through the shared effect
boundary as a harmful effect caused by its caster, so the boundary's own rules (PvP, parties, creative protection,
tolerances) apply again at impact. The moving ward is a guard on that boundary: any push whose actor is not its bearer
is refused.

**Dreams on the server** (`concordance/dreaming`). The expedition (`DreamExpedition`, attachment
`jugcraft:dream_expedition`) is the one transaction: its id, when it began and ends, the body's dimension and place,
the dreamer's game mode and experience, how many wisps have gathered and been caught, the censer, and the escrow, slot
by slot. It is saved with the player, in the same record as the inventory, so at every save either the dreamer holds
their own things and no expedition exists, or the expedition holds them and the dreamer holds only what the dream gave.
`Dreaming.end` takes back whatever the dreamer holds, puts the escrow back into its slots, removes the expedition and
restores the rest in one server tick; ending a dream that is not open does nothing. It runs on waking, on the time
running out, straying or leaving the body's dimension (checked every half second), on harm, before death (the death event),
on leaving the server, and on joining with a dream still open (a crash). A dreamer who died some way the death event
did not see keeps the expedition on the dead body (ending it there does nothing) until they respawn; the new body then
gets the escrow and game mode back (`Dreaming.recover`), once. Wisps are GeckoLib entities that are never saved and
fade the moment their dreamer is not dreaming; only their dreamer can catch them, by touch, and nothing can pick or
strike them (no projectile stops at one). A wisp gathers only where creatures are live (a loaded, ticking chunk); where
its place is not, it gathers nearer the censer, so none the dream counts is lost where nobody can reach it.

## Connections

- Input producer: First Light (both researches need it understood); vanilla reagents and remedies; dreams make
  dreamglass, which ward sigils need.
- Output consumer: curses hinder creatures and players (where the rules allow); wards protect against Concordance
  operations; Sympathy's and Dreamwalking's mastery.
- Technology connection: none beyond crafting.
- Magic connection: curses and the moving ward go through the shared effect boundary; Focus pays for casting,
  investigating and dreaming.
- Reachable entry path: First Light understood → examine a cobweb → study it at the bench → craft a taglock → link a
  zombie → cast Lethargy with a cobweb. Dreams: examine a white bed → study it → craft an Oneiric Censer → dream at night
  → catch wisps → dreamglass → ward sigils.
- Solo, trade and cooperative routes: solo; dreamglass and sigils can be traded; a friend can scry and remedy for
  themselves (the glass needs no research).
- Specialty use without other branches: needs only First Light.
- Mastery: three different curses cast (Sympathy); three dreams come back from (Dreamwalking).

## Balance

- Curses are hindrances only (slowness, weakness, bad luck; mining fatigue and hunger allowed by the data rules), at
  level I here and never above II, for minutes, and cost Focus and a reagent each. At most three lie on a creature.
- Remedies are common items; the scrying glass needs no research, so every curse has a counter anyone can reach.
- A dream costs 10 Focus and gives at most eight dreamglass; nothing else is gained in a dream.
- No new currency or ore; no positive loop (dreamglass is made only by dreaming and spent on sigils).

## Multiplayer and persistence

- Server authority: links, curses, wards, the escrow and every rule are the server's; the client only draws.
- Permissions: links and curses against players need the server's PvP setting and respect parties
  (`ConcordanceEffects.mayHarm`), checked when linking, casting and at every pulse; the boundary checks again at impact.
  Cross-dimension actions are refused; a curse waits while its caster is elsewhere.
- Persistence: links in their taglocks; curses on their bearers; wards on their players; expeditions and dream counts
  on their players. Censers save nothing.
- Disable behaviour: with `concordance.enabled=false` nothing can be linked, cast, warded or dreamt; existing curses do
  not pulse; data is kept.

## Dependencies and assets

No new dependency. Framework use:

- **Fabric API**: data components, attachments (the expedition is saved with the player), events (join, disconnect,
  damage, death, respawn) and commands.
- **The shared effect boundary** (Jugcraft): every curse pulse, the dream's night vision and the moving ward's guard.
- **Spell Engine**: not used. A curse travels through a link held in a taglock, not as a cast spell, a projectile or
  an area, so no Spell Engine delivery applies; wards are items. (Morning question 35.)
- **GeckoLib**: the Oneiric Censer (idle; swinging and smoking while anyone dreams by it) and the dream wisp (drifting).
- **SmartBrainLib**: not used: a wisp only drifts round a point and decides nothing.
- **Modonomicon**: a new **Hexes and Dreams** codex category: Sympathy (links, curses with every reagent, pulse, link
  strength, Focus and remedy, investigation and remedies, wards) and Dreamwalking (the censer, waking).
- **GuiLib**: not used; the scrying glass and `/jugcraft concordance hexes` are the inspection. **Trinkets**: read only,
  to refuse a dream while anything is worn there.

Art (`tools/concordance_hexes_art.py`, five maps in `tools/item_icons/`, the GeckoLib models in
`tools/concordance_hex_models.py`): icons for the taglock (a corked vial holding a red thread, its string running to a
paper tag with a violet mark), the scrying glass (an upright hand mirror of dark violet-blue glass in a gold rim with
an amethyst crown), the ward sigil (a folded paper strip with a violet-ink ring round a dreamglass shard), dreamglass
(a pale violet and blue shard) and the Oneiric Censer; the censer's 64x64 sheet (a dark-oak foot, a brass post and arm,
a pierced brass bowl and lid glowing violet, a pale violet smoke plume) and the wisp's 32x32 sheet (a violet-white core
and two crossed flame-shaped petals). Everything is drawn fresh in the Concordance's palettes with three new colour
ramps (smoke, wisp core, petals); the owner's library was searched for vials, bottles, mirrors, lenses, charms, paper,
shards, censers, incense, lanterns, smoke and wisps and nothing fits (the module's docstring lists the candidates and
why each was not used); two of the owner's crystal reference sheets were looked at for style only. The taglock's
loose string and the censer's smoke are drawn without an outline, as the icon maps declare. The art has not been
shown to the owner yet.

## Verification

- `python3 tools/check_mod_data.py`: new step 22 checks (`check_sympathy`): the Java's link, curse, ward and dream
  numbers and ward categories equal the generator's; the curses on disk are the generator's and each is bounded; curse
  pulses go through the boundary as harmful under its multiplayer rules and every cast and pulse is revalidated; the
  moving ward guards the boundary; the expedition is one persistent attachment never copied on death, and dreams end
  on join, disconnect, death and respawn and when the dreamer leaves the body's dimension, never giving back spent
  experience; every reason, end and
  message has its text; every ward has a sigil recipe made with dreamglass; the GeckoLib models and animations are the
  generator's, the clips played exist and every box fits its sheet; the icons are their maps. The pure-package rule
  covers `concordance/hex` and `concordance/dream`.
- The pure core compiles with JDK 21 and the rules load the generated curses (three) with no problems. The step 22
  harness passes **40 checks**: links fade and expire; casting refuses, in order, no link, a faded link, another
  dimension, a missing target, distance, the multiplayer rules, a ward, a weak link, the same curse twice, a fourth
  curse and too little Focus; pulses rest, skip while elsewhere, far or warded, and lift when over or no longer allowed;
  investigation names then traces, and a scrying ward hides the caster; remedies; links refuse oneself, wards and the
  rules; wards guard one category, renew without stacking and expire; dream rewards by ending, ends by time, straying
  or dimension, wisps three then one every 30 s up to eight, and spent experience staying spent; and the parser's
  refusals.
- Game tests added: `ConcordanceHexGameTests` (eight): links are taken by touch and respect the rules; a curse is cast
  (reagent and Focus taken, not twice), pulses Slowness through the boundary, is named then traced (hidden by a
  scrying ward) and lifted by its remedy; a cursing ward skips a pulse and a curse lifts where the rules forbid harm; a
  ward sigil wards one category and a moving ward stops another's push (not its bearer's own); a dream holds everything
  in one escrow and gives back exactly that (same slots, experience, mode; picked-up things dropped; one dreamglass;
  ending twice gives nothing; the expedition saves and reads back whole; spent experience stays spent); more wisps
  gather with time and the dream ends when its time is over; an unseen death keeps the dream on the dead body and the
  respawned body gets the escrow back once; and a dream refuses creative mode, too little Focus and anything worn in an
  accessory slot.
- CI: the first runs on this step (commit ea28d223) found three faults, fixed in 9e49e692 (Fabric API 0.161 has no
  world-change event, so the dream check ends a dream that leaves its dimension), 380fc147 (a dreamer's game mode is
  read from their abilities) and a1088c59 (wisps are caught by touch and are not pickable: a pickable wisp stopped
  another test's harpoon). Run 37640896275 (commit a1088c59) passes the whole Build workflow: it builds, passes the
  data checks and all 1017 required server game tests (the eight above among them), and the client test shards
  pass.

Not yet run: any client (the censer's and wisp's models and animations, the icons, the codex pages), a two-client
dedicated server (two players cursing and warding each other with PvP on and off; a dreamer disconnecting and
rejoining), and a real crash mid-dream (the tests check the expedition's save and the recovery paths directly).

## World and event applicability

Works everywhere. Dreams need night where the dreamer is (`isBrightOutside` false): in a dimension with fixed time
(the Nether, the End) it always counts as night. No seasonal content.

## Rollout and open questions

New registrations only: items `jugcraft:taglock`, `jugcraft:scrying_glass`, `jugcraft:ward_sigil`,
`jugcraft:dreamglass`; block and item `jugcraft:oneiric_censer` and its block entity; entity `jugcraft:dream_wisp`;
data components `jugcraft:taglock` and `jugcraft:ward`; attachments `jugcraft:curses`, `jugcraft:wards`,
`jugcraft:dream_expedition` and `jugcraft:dreams`; researches `jugcraft:sympathy` and `jugcraft:dreamwalking`;
practices `jugcraft:sympathy` and `jugcraft:dream`; item tags `jugcraft:sympathy_specimens` and
`jugcraft:dream_specimens`; data folder `concordance/curse`. Removing them needs a migration (an open expedition holds
a player's things: never remove `jugcraft:dream_expedition` without ending every dream first).

Open items:

- Curses are three hindrances; harmful curses (damage over time) are deliberately absent.
- Links are taken only by touch; taglocks made from a creature's hair or a player's lost item are a possible extension.
- A dream is the real world seen with nothing: the dreamer can still open chests and fight; what they pick up falls at
  their feet on waking.
