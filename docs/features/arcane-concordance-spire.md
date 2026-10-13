# Arcane Concordance: the Concord Spire

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 25) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 17, Master to Architect stages (Jugcraft Expeditions to Shared
wonders). Roadmap step 25.
Primary specialty and supported player role: the Lampwrights (Lantern Spire), the Greenwardens (Verdant Spire) and the
Starwatchers (Star Spire), with the Circlewrights' rite in each; built alone or by a party.

Builds on research and practices ([First Light](arcane-concordance-first-light.md) and every tradition's), the
[stages](arcane-concordance-progression.md) (the Master stage founds a spire, a raised one opens a route to Architect),
[rituals and Ley Charge](arcane-concordance-rituals.md) (the foundation is a circle of Ley Pylons; the Kindling is a
ritual), [Courier Posts and porters](arcane-concordance-logistics.md) (upkeep deliveries), the
[shared effect boundary](arcane-concordance-composition.md) (its light and its reveal) and Jugcraft's parties. Contract
and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What already existed, and what this reuses

Nothing in the Concordance asked for a long, maintained undertaking: a ritual is minutes, a Conclave project a few
days of deliveries. This step adds one wonder built from the systems already verified, and adds no new resource, no new
transport and no new ritual machinery: its structures are ritual structures checked by the ritual step's
`StructureValidator`, its rite is an ordinary ritual at an ordinary Lesser Circle (a new hook, `Rituals.listen`, tells it
when one completes), its Ley Charge comes from Ley Pylons, its deliveries are Courier requests carried by porters, its
light and reveal go through `ConcordanceEffects`, its demonstrations are practices the research engine records, and
its party sharing is Jugcraft's own.

## Player experience

A **Master** of the Concordance crafts a **Spire Heart** (gold, luminous matter, a Circle Anchor and polished
deepslate) and places it where the spire will stand. Sneaking and using it with an empty hand chooses a
**configuration** (it says what each needs); using it founds the spire.

| Configuration | Needs | Practice | Crown (over the shaft) | Upkeep a day | Its field |
|---|---|---|---|---|---|
| Lantern Spire | First Light mastered, Circle Lore understood | rituals | glowstone, a Lumen Sconce | 4 glowstone dust, 8 Ley Charge | Kindled light in the dark open air within 24 blocks (4 a pulse), so nothing hostile spawns there |
| Verdant Spire | Verdant Husbandry mastered, Circle Lore understood | tending living devices | a moss block, a flowering azalea | 8 bone meal, 6 Ley Charge | the crops within 16 blocks grow a step (6 a pulse) |
| Star Spire | Celestial Attunement mastered, Circle Lore understood | observing the sky | an amethyst block, a lightning rod | 2 glow ink sacs, 8 Ley Charge | its keepers within 24 blocks regain a Focus and hostile creatures there are revealed (6 a pulse) |

A pulse comes every five seconds while the spire works.

**Raising it**, in four phases, each checked by the heart every second:

1. **The Foundation**: four Ley Pylons two blocks out on each side and four Warding Stones on the diagonals.
2. **The Shaft**: three blocks of dark stone (polished deepslate, deepslate bricks, tiles or chiseled deepslate) stacked
   on the heart, and the configuration's practice carried through twice by its keepers after the phase began.
3. **The Crown**: the configuration's crown on the shaft, and **the Kindling**, a ritual at a Lesser Circle within 12
   blocks of the heart (8 glowstone dust, 2 gold ingots, an ender pearl; 8 Focus, 2 Ley Charge), completed by its keepers.
4. **The Kindling**: its upkeep held three days in a row.

**Keeping it.** From the Kindling on, each day the spire takes its configuration's item from the heart's store (nine
slots: put it in by hand, by hopper, or let a courier bring it) and Ley Charge from its own pylons. While the store holds
less than two days' upkeep, the heart files a request (as its keeper's) at the nearest Courier Post within 16 blocks the
keeper may use, and files no other until that one closes: a porter bound to that post brings the rest. A spire also
needs **attendance**: its configuration's practice carried through by its keepers at least once a week. The field works
while the spire is raised, whole, attended and its last day's upkeep was met; otherwise it rests and says why.

**Sharing.** Its keeper sneaking with an empty hand shares it with their party (or takes it back): then every member's
practices count, any member may fill its store and take part in its rite, and their pylons lend to it.

**Inspecting.** An empty hand on the heart, or `/jugcraft concordance spire`, shows its phase and exactly what it
still needs (each missing or wrong part with its place), its upkeep, its store and its pylons' Ley, its field, and its
attendance. Jade shows the same at a glance when installed.

**Interruptions and repairs lose nothing.** The spire is the world's record, not the heart's: a broken heart drops its
store, its courier requests are cancelled (their cargo goes back), and a heart put back where it stood answers as before.
A missing or wrong part rests the field and says which; putting it back wakes it. A day without upkeep takes nothing.
An unloaded spire owes nothing for the days it missed. `/jugcraft concordance spire realign <configuration>` changes
the configuration (before the crown nothing is lost; after it the foundation and shaft stand and the crown, its rite and
its days are done again), and `/jugcraft concordance spire abandon confirm` forgets the spire; what was built stays.

**Raised**, the spire records the milestone `jugcraft:spire_raised` for its keeper and everyone who contributed (on
their next join if they were away), which opens a second route to the **Architect** stage.

## How it works

**The rules** (`concordance/wonder`, pure Java). `SpireDefinition` (the wonder: heart, stage, rite, attendance, pulse,
phases), `SpireConfiguration` (research, practice, crown, upkeep, field), `SpireState` (the world's record of one
spire) and `Spires`, which decides founding, what a phase still needs, a day's upkeep (met or why not, and what it
takes), the field's state and realigning; `WonderParser` reads both kinds strictly and checks them against the
structures, rituals, research, practices and stages.

**On the server** (`concordance/spire`). `SpireRecord` (saved data `jugcraft:spires`) holds every spire keyed by where
its heart stands. `SpireHeartBlockEntity` is a nine-slot container with no screen (couriers deliver to it, hoppers fill
it) that looks at its spire every second (`ConcordSpire.work`): the structures through its phase are checked by the
ritual step's validator, the day's upkeep is applied when due, and the next phase begins when nothing is missing. Its
field pulses only on the server's own tick, a pulse after the heart loads and every five seconds after.
`ConcordSpire` hears practices from the research engine and the Kindling from `Rituals.listen`, files courier requests,
and keeps commands. The heart is drawn by GeckoLib from synchronised state (its status), never from client claims.

## Connections

- Input producer: research mastery (founding), practices (phases and attendance), rituals (the Kindling), Ley Pylons
  (Ley Charge), Courier Posts and porters (upkeep), glowstone, bone meal, glow ink and building blocks.
- Output consumer: the field (light, growth, Focus and reveals); the `jugcraft:spire_raised` milestone and the Architect
  stage; advancements.
- Technology connection: deliveries through the courier ledger; hoppers into the heart.
- Magic connection: the Concordance's research, rituals, Ley Charge and shared effects.
- Reachable entry path: the Master stage → a Spire Heart → the foundation (pylons and warding stones are early ritual
  pieces) → practices → the crown → the Kindling (Circle Lore understood) → three days of upkeep.
- Solo, trade and cooperative routes: every phase can be done alone; a party shares the practices, the rite and the
  upkeep; couriers can bring what others make.
- Specialty use without other branches: each configuration needs one tradition's mastery and Circle Lore.
- Mastery: the raised spire, its advancement and the Architect route.

## Balance

- Completion depends on maintained capability, not a material bill: the blocks are modest (four pylons, four warding
  stones, three stone, two crown blocks, the heart), but the phases need the configuration's mastery, its practice
  carried through, a ritual and three days of upkeep held in a row, and the field needs the same upkeep and a practice
  every week for as long as it is to work.
- The field is bounded: at most 4 or 6 things a pulse, every five seconds, within 16 or 24 blocks; light only in open
  air above solid ground where it is dark, growth a step at a time on unripe crops, one Focus at a time.
- Upkeep is taken whole or not at all, and never more than a day's.

## Multiplayer and persistence

- Server authority: founding, phases, upkeep, attendance and the field are the server's; practices and rites come only
  from what the research engine and the circles recorded; deliveries are the courier ledger's.
- Permissions: only its keeper's side counts for it, fills its store or takes part in its rite; only its keeper shares,
  realigns or abandons it; its light obeys the shared boundary's protections (a protected town is never lit).
- Persistence: `jugcraft:spires` (the world); the heart keeps only its store, owner, chosen configuration, open request
  and status. Breaking the heart, unloading or restarting loses nothing.
- Disable behaviour: with `concordance.enabled=false` the spire answers nothing and its field rests; everything is kept.

## Dependencies and assets

No new dependency. Framework use:

- **GeckoLib**: the Spire Heart's model and its three animations (dormant, raising, working), chosen from its synced status.
- **Jade** (optional): the heart's phase, status and upkeep.
- **Fabric API**: saved data, the join event, commands, the item transfer the couriers use.
- **Modonomicon**: a new **Wonders** codex category (the spire, its configurations, keeping it).
- **SmartBrainLib**: through the existing porters, which carry its deliveries; no new creature.
- **GuiLib**: not used: the heart's messages, the command and Jade are the interface, and nothing needs a screen.

Art: the **Spire Heart** is a GeckoLib block (`tools/concordance_spire_models.py`) that fills its block exactly. From
the ground up:

- a foot of dark dressed stone;
- a lower strap of blackened steel, bolted in brass;
- four stone corner posts round a chamber, with a window on each side;
- an upper strap like the lower one;
- a full-width steel top plate, the seat the shaft stands on. It carries a violet founding ring round a small lens,
  which the shaft covers once built.

In the chamber, a violet-white gem stood on its corner floats in a brass cage of two square frames and four ribs. One
rib is marked with a violet inlay so its turn reads. The three animations:

- dormant: the gem rests small and low;
- raising: the gem breathes;
- working: the cage turns once in eight seconds while the gem turns the other way and pulses.

The 64×64 sheet (`tools/concordance_spire_art.py`) and the 16×16 icon map (`tools/item_icons/spire_heart.txt`) are
drawn fresh, in flat tones lit from the top left, as the top tier's dieselpunk direction asks
([ART_DIRECTION.md](../ART_DIRECTION.md)). The heart's selection box is the whole block, as its model is.

Provenance:

- The owner library was searched for keystones, pedestals, plinths, pillars, altars, cores, crystals, lenses, cages,
  gyroscopes, rings, dark steel, deepslate, brass, rivets and machine cores.
- One set is used, for its colours only. The steel's five tones are colours of the owner's dark-steel blocks in
  `art/owner-library/originals/Blocks/Big Cannons and Mounted Guns/textures/block/`: `solidsteel.png` and
  `reactorchambertop.png`. The second is also the reference for the plate's lens, drawn fresh in violet.
- No pixels or layouts are copied. The art module's docstring names every other candidate and why it was not used.
- The stone, brass, gem and violet are the Concordance's own palettes.
- Nothing is traced, sampled or recoloured from Mojang's files.

## Verification

- `python3 tools/check_mod_data.py`: PASS (1597 material IDs, data files and recipe audit). The new step 25 checks
  (`check_spire`) confirm:
  - the Java mirrors the generator's limits;
  - the wonder and its configurations on disk are the generator's;
  - each configuration's research, practice, crown and renewable upkeep exist;
  - the Kindling can be completed alone from items that can be relied on;
  - the heart is registered, made, mined and drops itself;
  - its GeckoLib model, animations and sheet are the art modules' own, every box fits the sheet, and the icon is its
    map;
  - nothing breaks or removes a block;
  - the rite and the practices are heard from the circles and the research engine;
  - the Architect stage has the spire's route;
  - every message has its text.
- `python3 scripts/check_repository.py` and `python3 tools/check_icon_maps.py`: PASS. Regenerating the data and textures
  on a fresh copy reproduces the committed files byte for byte.
- The step 25 harness passes **70 checks** against the generator's JSON (the shipped files and a compact copy alike):
  - founding: the stage, then each configuration's own research;
  - each phase's needs in order: the structure, practices never counted beyond what the phase asks, a rite counted only
    for the phase that asks for it, then the days;
  - the daily upkeep: not before it falls due; too little Ley or too few items, damage or absence take nothing; three
    days held finish the Kindling;
  - the raised spire: it works, rests when damaged, unattended or unsupplied, and wakes again;
  - a long absence owes one day;
  - realigning before and after the crown, and sharing;
  - the parser's refusals and cross-references: a missing rite, practice, research or stage, and a single configuration.
- With step 25's data, the step 24 harness still passes its 36 checks over a graph of 76 nodes. The spire's milestone
  gate is a twelfth practice gate, and the Kindling a new ritual node. The generator's audit finds the raised spire
  reachable by one player alone. Architect now has no single mandatory step, because the Conclave and the spire are
  independent routes.
- Game tests added: `ConcordanceSpireGameTests` (ten).
- CI: run 37655111143 (commit ff992934) passes the whole Build workflow on its first run: it builds, passes the data
  checks and all 1042 required server game tests (the ten above among them), and the three client test shards pass.

Not yet run: any client (the heart's model, animations and icon, the codex pages, Jade), a two-client dedicated server
(a party raising and keeping a spire, one of them offline at the raising), and days of real play with a porter keeping
a spire supplied.

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

New registrations only: block and item `jugcraft:spire_heart` and its block entity; saved data `jugcraft:spires`;
advancements `jugcraft:concord_spire_founded` and `jugcraft:concord_spire_raised`; structures
`jugcraft:spire_foundation`, `jugcraft:spire_shaft` and `jugcraft:spire_crown_*`; ritual `jugcraft:spire_kindling`;
block tag `jugcraft:concordance/spire_stone`; data folders `concordance/wonder` and `concordance/wonder_configuration`;
milestone `jugcraft:spire_raised` and its practice gate. Removing them needs a migration.

Open items:

- One wonder with three configurations; more wonders would reuse the same rules.
- The field pulses only while the heart's chunk is loaded, like any block.
