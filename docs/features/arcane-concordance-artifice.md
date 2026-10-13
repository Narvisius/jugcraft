# Arcane Concordance: equipment construction and enhancement

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 19) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 11, Practitioner stage (Jugcraft Workshops). Roadmap step 19.
Primary specialty and supported player role: the Runesmiths. Forge rings, then reforge, inscribe, socket, bond, repair
or salvage them, knowing exactly what each step keeps and destroys.

Builds on [research and notes](arcane-concordance-sharing.md) and Focus ([First Light](arcane-concordance-first-light.md)).
Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

Once First Light is understood, examining smithing stock (flint, a gold or iron nugget, lapis lazuli, nether quartz)
begins **Runesmithing**; studying one at a Lampwright's Bench (or reading notes) understands it.

**Forging.** Build an **Artificer's Bench** (iron, an amethyst shard, smooth stone and a smithing table) and use it with
ingots in your main hand: 4 copper, iron or gold ingots, or 1 netherite ingot, and 8 Focus forge a **Resonant Ring**.
Its craft **quality** (crude 15%, sound 55%, fine 24%, masterwork 6%) and its **affixes** (random properties such as
Arcane Power, Vitality, Fortune, Swiftness, Warding and Might, at most one of a kind) are rolled once, on the server,
and saved on the ring before you see them.

**Capacity.** A ring's resonance capacity is its substrate's (copper 4, iron 5, gold 7, netherite 9), plus its quality
(crude -1, sound 0, fine +1, masterwork +2), plus 1 while bonded. Affixes, runes and gems each use part of it; nothing
is ever added beyond it. Substrates differ in sockets (1 to 3), rune slots (1 or 2), durability and which affixes they
can roll; the Rune of Radiance takes only gold or netherite.

**Enhancing** (the ring in your main hand, at the bench, with in your other hand):

| Other hand | Process | Keeps | Destroys | Costs |
|---|---|---|---|---|
| nothing | inspect | everything | nothing | nothing |
| redstone | reforge | substrate, quality, runes, gems, bond, wear | affixes (new ones are rolled) | 1 redstone, 6 Focus |
| a rune's ingredient | inscribe | everything | nothing | the ingredient, 4 Focus |
| a gem | socket | everything | nothing | the gem |
| shears | unsocket | everything but the last gem, which comes back whole | nothing | 1 shears durability |
| a lead | bond (or unbond) | everything | the bond (unbonding) | 10 Focus to bond |
| its substrate's ingot | repair | everything | nothing | enough ingots to mend its wear |
| flint, twice | salvage | the gems (returned) | quality, affixes, runes, bond | the ring; 2 ingots come back (none for netherite) |

Runes: Radiance (glowstone dust: arcane power), Swiftness (sugar), Warding (obsidian), Vigor (golden carrot). Gems:
amethyst (arcane power), emerald (luck), diamond (health), lapis (armour), quartz (attack damage).

**Wearing.** Wear the ring in a **ring slot** (Trinkets). It gives its properties to its wearer, or only to its bonded
player if bonded. It wears one point every 30 seconds worn; at its last point it is **dull** and gives nothing until
repaired, but it never breaks, so nothing on it is ever lost to wear.

## How it works

**Definitions** (`data/<ns>/concordance/{substrate,gem,rune,affix}/`, read by `ArtificeParser` into `ArtificeCatalog`,
`concordance/artifice`, pure Java). The parser refuses a salvage that would give back as much as forging costs, an
affix range that runs backwards and unknown fields; it names unknown affixes on a substrate, unknown substrates on a
rune, substrates that could roll no affix even when crude, and any item that would be two things at the bench.

**The rules** (`artifice/Forge.java`): capacity, use, compatibility and every process. `Forge.Process` lists, for each
process, the parts it keeps and destroys; the bench's inspect and the codex show this table. The only randomness is a
seed: forging rolls quality and affixes from a seed the server draws and saves on the ring; reforging rolls from that
seed and the ring's next reforge count. So the same ring always reforges to the same next roll: looking at it,
cancelling, disconnecting, reopening anything or reloading the world never rolls again, and the outcome is fixed and
saved on the ring before the player is told.

**The ring** (`smithy/ResonantRingItem`, component `jugcraft:artifice`): a Trinkets Updated ring (`TrinketCallback`).
Trinkets asks it for its modifiers when it is put on and removes them when it is taken off, so each applies exactly
once; the ring has no hand modifiers, so holding it does nothing. Each statistic becomes an attribute modifier: vanilla
attributes and Spell Power's `spell_power:arcane`, which Spell Engine's spells read when they scale. Modifier ids are
the slot's id plus the stat's index, so re-equipping never stacks.

**The bench** (`smithy/ArtificerBenchBlock`, `smithy/Artificery`): every process works out the ring's next state with
`Forge`, writes it to the ring, takes its cost, records the practice, and only then tells the player. Salvage asks for a
second use within ten seconds; the confirmation is held on the server against the ring's exact state.

## Connections

- Input producer: First Light (Runesmithing needs it understood); copper, iron, gold and netherite ingots; gems and rune
  ingredients from mining, farming and the Nether; Focus.
- Output consumer: the wearer's attributes, Spell Power's arcane power for Spell Engine spells (the Concordance's
  invocations scale with it), and salvage's ingots and gems.
- Technology connection: substrates are Jugcraft's and vanilla's metals; the bench needs a smithing table.
- Magic connection: Focus pays for forging, reforging, inscribing and bonding; arcane power strengthens Radiance spells.
- Reachable entry path: First Light understood → examine flint → study at the bench → build an Artificer's Bench →
  forge a copper ring.
- Solo, trade and cooperative routes: entirely solo; unbonded rings can be traded with everything on them; a bonded
  ring serves only its bond until unbonded.
- Specialty use without other branches: needs only First Light.
- Mastery: three different enhancements (reforge, inscribe, socket, bond, repair) master Runesmithing.

## Balance

- **No positive loop**: salvage gives back less of the substrate than forging costs (2 of 4 ingots; none of 1
  netherite) and only the gems that were set; runes, affixes and Focus are spent for good. The checker refuses any
  substrate whose salvage would equal its cost.
- **Bounded rings**: `tools/check_mod_data.py` works out, for each substrate, the most of each attribute any ring could
  add (best affix of each kind, best runes for its slots, best gem in every socket, ignoring capacity, so a real ring
  adds less) and checks it against a cap: arcane power 8, health 14, luck 6, speed 15%, armour 10, attack damage 4.
- Reforging costs redstone and Focus each time and never improves quality; the next roll is fixed, so it cannot be
  shopped for by looking.

## Multiplayer and persistence

- Server authority: every roll and process is the server's; the client sees the ring's synced component. Bench uses
  are rate-limited.
- Permissions: anyone may enhance a ring they hold, but only its bonded player may unbond it, and a bonded ring gives
  nothing to anyone else.
- Persistence: the ring's substrate, quality, seed, reforge count, affixes, runes, gems and bond in `jugcraft:artifice`;
  its wear in vanilla damage. Salvage confirmations are not saved (a restart only means confirming again).
- PvP: rings add attributes under the server's normal rules.
- Disable behaviour: with `concordance.enabled=false` the bench does nothing; rings keep their components.

## Dependencies and assets

No new dependency. Framework use:

- **Trinkets Updated**: the ring is a `TrinketCallback` item in the `hand/ring` slot, which Jugcraft gives players
  (`data/trinkets/entities/jugcraft.json`, `data/trinkets/tags/item/hand/ring.json`); Trinkets applies and removes its
  modifiers once.
- **Spell Power**: `spell_power:arcane` as an affix, rune and gem statistic.
- **Spell Engine**: no direct integration. Spells cast with Spell Engine read the wearer's spell power, so a ring's
  arcane power reaches them through Spell Power; rings are not spell containers.
- **JEI (optional)**: an information page on the ring (where and how it is forged; never its rolls).
- **Modonomicon**: a new **Artifice** codex category (Runesmithing, Forging, Enhancing with the keep-and-destroy table).
- **GeckoLib**: not used; a ring has no animation worth one. **GuiLib**: not used; the bench's inspect is the preview.

Art (`tools/concordance_artifice_art.py`, `tools/item_icons/artificer_bench.txt` and `resonant_ring.txt`): the
Artificer's Bench's three faces (a stone bench with a steel working plate, a violet rune circle, an amethyst inlay,
brass corner caps and three ingots kept under the slab) and the bench and ring icons (a brass band with a set amethyst).
Everything is drawn fresh; the stone's colours are sampled from the owner's grey and dark stone swatches
(`art/owner-library/previews/grey-stone-variants.png`, `dark-teal-stone.png`), only as colours, and the rest are
Jugcraft's own ramps (the Concordance's brass and violet, the owner's chosen steel). The library's rings, gems, benches
and brass were looked at and not used; the module's docstring says why. The art has not been shown to the owner yet.

## Verification

- `python3 tools/check_mod_data.py`: new step 19 checks (`check_artifice`): the Java qualities, limits, costs and bench
  tools equal the generator's; the definitions are the generator's; salvage is always less than cost; nothing is two
  things at the bench; every substrate's most possible attribute is under its cap; every process, refusal, quality,
  substrate, affix, gem, rune and attribute has its text; the ring is a Trinkets ring with no vanilla modifiers; the
  Trinkets slot data exists; recipes, faces and icons. The pure-package rule covers `concordance/artifice`.
- The pure core compiles with JDK 21. The step 19 harness passes **32 checks** against the generated data: the same
  seed forges the same ring; 16,000 forgings never exceed capacity or their quality's affix count, never repeat a group
  and use only their substrate's affixes; qualities follow their weights; reforging gives the same roll however often it
  is previewed, counts each reforge and keeps substrate, quality, runes, gems and bond; the rules table; rune
  compatibility, slots and capacity; sockets; unsocketing returns the gem; bonds add capacity, serve only their player
  and cannot be unbonded while the capacity is needed; salvage never gains; repair takes whole items; stats; and the
  parser's refusals.
- Game tests added: `ConcordanceArtificeGameTests` (six): a ring is forged and saved before it is shown (cost and Focus
  taken; its roll is its saved seed's); reforging never rerolls (inspecting changes nothing; the reforge is the seed's
  roll; it keeps quality, runes and gems); gems, runes and bonds keep their rules (a diamond set and given back whole;
  a full socket refuses; a rune inscribed; a lead bonds; modifiers for the bond alone, one per stat; no one else can
  unbond; a dull ring gives nothing); salvage needs confirming and never gains; repair mends wear with the fewest
  ingots; and every statistic the data names is a registered attribute, Spell Power's included.
- CI: run 37625123313 (commit 50b1800b) builds, passes the data checks and all 996 required server game tests (the six
  above among them); client test shards 0 and 1 pass, and shard 2 stopped before running any test when Loom could not
  download Minecraft's assets. The next run, 37628449524 (commit e2b7feb6, which also carries step 20), passes the whole
  Build workflow, every client shard included.

Not yet run: any client (the bench, the ring's tooltip and icon, the codex pages, JEI), a two-client dedicated server,
and equipping a ring in a real Trinkets slot (the tests call the ring's Trinkets callback directly).

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

New registrations only: block and item `jugcraft:artificer_bench`; item `jugcraft:resonant_ring`; data component
`jugcraft:artifice`; research `jugcraft:runesmithing`; practice `jugcraft:artifice`; item tag
`jugcraft:artifice_specimens`; data folders `concordance/substrate`, `concordance/gem`, `concordance/rune` and
`concordance/affix`; Trinkets data giving players the hand ring slot. Removing them needs a migration.

Open items:

- Only rings: amulets, charms and enhanced weapons or armour would use the same rules with other Trinkets slots or items.
- A GuiLib preview screen (step 26); the bench's inspect is the preview, and it never shows a roll before it is made.
- The ring's tooltip shows its affixes' raw values; the bench's inspect shows them in words (the rules are not sent to
  clients).
