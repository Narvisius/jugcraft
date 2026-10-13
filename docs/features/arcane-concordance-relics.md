# Arcane Concordance: relics and shrines

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 20) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 12, Practitioner stage (Jugcraft Workshops). Roadmap step 20.
Primary specialty and supported player role: the Runesmiths' keepers of old things. Carry, wear and install relics
that work exactly where they are meant to, and say why they do not elsewhere.

Builds on [Ley Charge and its pylons](arcane-concordance-rituals.md), the [shared effect
boundary](arcane-concordance-composition.md) and [Runesmithing](arcane-concordance-artifice.md) (the Trinkets slots).
Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

Once First Light is understood, examining something that has outlived its maker (a Heart of the Sea, a Nautilus Shell,
a Totem of Undying, an Eye of Ender or a Recovery Compass) begins **Relic Lore**; studying one at a Lampwright's Bench
(or reading notes) understands it.

**Relics.** Four relics, each with its own purpose, charge and places:

| Relic | Works when | Does | Every | Charge | Also |
|---|---|---|---|---|---|
| Wardlight Lantern | held in the off hand | hostile creatures within 12 blocks glow (3 s) | 2 s | 1 of 64 | |
| | installed in a Reliquary Shrine | hostile creatures within 24 blocks glow | 2 s | 1 | |
| Hearthstone | worn as a necklace (Trinkets) | Regeneration I on its wearer (5 s), when hurt and ten seconds calm | 10 s | 2 of 48 | binds to the first player it serves |
| | installed | Regeneration I on its owner's hurt party within 8 blocks | 10 s | 1 | |
| Stormglass Orb | held in the main hand under the open sky | Speed I (3 s) | 2 s | 1 of 32 | cannot be installed |
| Owlsight Circlet | worn on the head in the dark | Night Vision (22 s) | 10 s | 1 of 32 | drawn on the head |

**Everywhere else a relic does nothing**: carried loose in the inventory, worn for show in a Trinkets cosmetic slot,
held in the other hand, worn in another slot, or kept in a chest, a shulker box or a bundle (nothing inside a container
is ever looked at). `/jugcraft concordance relics` lists every relic you carry, where it is, and whether it works or
exactly why not: the wrong place (with the places it does work), cannot be installed, bound to someone else, needs the
open sky, needs the dark, not calm, nothing to mend, out of charge, resting until its next pulse, held back by your
other relics this second, nothing in reach, or a stronger effect already holding.

**The Reliquary Shrine** (polished blackstone, a lodestone, an amethyst block and candles). Use a relic on an empty
shrine to **install** it, if it has an installed way of working; it starts at once, and the shrine lights. Use any other
relic on it (or one that cannot be installed) to **recharge** it from the Ley Pylons within 2 blocks: each Ley Charge
gives 4 charge, until full. An installed relic is charged from those pylons as it works, 8 a second. Use the shrine with
an empty hand to take its relic back. Only its owner (whoever placed it, or first installed in it) and their party may
install, recharge or take back; anyone else is told what it is doing. Breaking it drops its relic.

## How it works

**Definitions** (`data/<ns>/concordance/relic/`, read by `RelicParser` into `RelicCatalog`, `concordance/relic`, pure
Java). A relic names its item, capacity, whether it binds, and its modes; each mode names the contexts it works in, its
target (self, allies or hostiles) and range, the status it gives with its amplifier and duration, its interval and
cost, and what it needs (the open sky, the dark, calm, someone hurt, dimensions). The parser refuses "carried loose" or
"worn for show" as a context, a context in two modes, a self mode with a range or installed (a shrine has no holder), an
allies or hostiles mode without a range, a pulse that costs nothing or more than the relic holds, unknown fields and
numbers out of bounds.

**The contexts** (`relic/Context`): main hand, off hand, head, chest, legs, feet, a Trinkets slot, installed; and two
that never work and exist to be reported: carried loose and worn for show. **The rules** (`relic/Relics.check`): the
context first, then the bond, the dimension, the sky, the dark, calm, someone hurt, the charge and the interval; the
first that fails is the reason.

**On the server** (`concordance/reliquary`). Every second each online player's relics are found in a fixed order
(`Reliquary.find`): the hands, the four armour slots, the Trinkets slots (Trinkets Updated's attachment; a cosmetic
slot, or one whose effects Trinkets disables, is "worn for show"), then the inventory, only to report. Each relic is
checked; one that may pulse finds its targets (itself; its holder's party within range; hostile creatures within range;
only hurt ones when it mends; at most 12, nearest first), gives its status through the **shared effect boundary** (one
event and ledger per pulse; helpful to self and allies, harmful to hostile creatures so their tolerances apply), and
only if that changed something spends its charge, records the pulse and binds an unbound binding relic, all on the
relic's own `jugcraft:relic` component. A shrine (`ReliquaryShrineBlockEntity`) does the same each second for its
installed relic, as its owner.

**Aggregation and rate limits.** One player's carried and worn relics pulse at most twice in any second together; one
owner's installed relics at most four times, however many shrines. A relic held back is not spent and tries again at
the next check. Statuses stack by the boundary's rule (the stronger holds), so two relics giving the same effect never
add up, and the second spends nothing. Shrine uses are rate-limited.

## Connections

- Input producer: First Light (Relic Lore needs it understood); Ley Charge from Ley Pylons (Lumen Sconces' Radiance or a
  Verdant Heart's Verdance converted there); gold, amethyst, prismarine, a Heart of the Sea, an Eye of Ender, blaze
  powder, blackstone and a lodestone for the recipes.
- Output consumer: the holder's and their party's statuses (detection, regeneration, speed, night vision); Relic Lore's
  mastery.
- Technology connection: the shrine draws on the same Ley Pylons as rituals and the garden; relics are worn in the
  Trinkets slots Runesmithing introduced.
- Magic connection: every effect goes through the Concordance's shared effect boundary and its stacking rule.
- Reachable entry path: First Light understood → examine a Nautilus Shell → study at the bench → craft a Wardlight
  Lantern (vanilla materials) → a Reliquary Shrine by a Ley Pylon recharges it.
- Solo, trade and cooperative routes: entirely solo; relics can be traded (a bound Hearthstone serves only its player);
  a shrine serves its owner's party.
- Specialty use without other branches: needs only First Light (and a Ley Pylon to recharge).
- Mastery: relics serving in three different contexts master Relic Lore.

## Balance

- **Charge is spent, never made**: Ley Charge becomes relic charge (4 for 1) and never back; recharging fills at most to
  capacity (the last Ley Charge may fill it only in part). No positive loop.
- Effects are modest vanilla statuses at level I, never longer than 30 seconds, bounded by the parser (amplifier ≤ 1,
  duration ≤ 30 s, range ≤ 32, interval ≥ 1 s) and stacked by the strongest rule.
- A pulse that would change nothing (nothing in reach, a stronger effect holding, nobody hurt) spends nothing.
- Budgets: 2 pulses a second per player, 4 per shrine owner; at most 12 targets a pulse.

## Multiplayer and persistence

- Server authority: where a relic is, whether it works, its targets and its charge are all decided on the server; the
  client sees the relic's synced component (charge and bond) and the shrine's status for its animation.
- Permissions: a shrine serves its owner's party; a binding relic serves only its player; harmful effects follow the
  boundary's friendly-fire rules.
- Persistence: a relic's charge, bond and last pulse on its `jugcraft:relic` component; a shrine's relic, owner and
  status in its block entity. The budgets and the last-hurt times are not saved (a restart forgets a second, and makes
  everyone calm).
- PvP: relics only help their holder's party and reveal hostile creatures; no relic harms a player.
- Disable behaviour: with `concordance.enabled=false` no relic or shrine works; everything keeps its components.

## Dependencies and assets

No new dependency. Framework use:

- **Trinkets Updated**: the Hearthstone is worn in the `chest/necklace` slot, which Jugcraft gives players
  (`data/trinkets/entities/jugcraft_relics.json`, `data/trinkets/tags/item/chest/necklace.json`); the server reads the
  slots through `TrinketsApi.getAttachment` and treats cosmetic slots as worn for show.
- **GeckoLib**: the Reliquary Shrine (`geckolib/models/block/reliquary_shrine.geo.json`): its crystal rests while empty,
  turns while it holds a relic and spins with its halo while it works, by the status the server sends.
- **Jade (optional)**: the shrine's relic, its charge, and whether it works or exactly why not.
- **LambDynamicLights (optional)**: the Wardlight Lantern glows in hand; the shrine itself gives block light while it
  holds a relic.
- **Modonomicon**: a new **Relics** codex category (Relic Lore, where relics work, each relic, the shrine, many relics).
- **Spell Engine / Spell Power**: not used; relics give statuses through the shared boundary, not spells, so they are
  not scaled by spell power.
- **GuiLib**: not used; the command and Jade are the relic's diagnostics.

Art (`tools/concordance_relics_art.py`, `tools/item_icons/{wardlight,hearthstone,stormglass,owlsight_circlet,reliquary_shrine}.txt`):
the five icons are fresh 16x16 maps (a hexagonal brass lantern with violet-white glass and an amethyst finial; a warm
heart-shaped stone on a gold chain; a storm-grey glass orb with a lightning bolt in a copper claw; a thin gold circlet
with a green eye; the shrine). The shrine's 64x64 GeckoLib sheet is painted box by box: blackstone in colours sampled
from the owner's `cut_black_sandstone.png` and `blackstone_spines.png` (`art/owner-library/originals/Blocks/biomes and
tree blocks/`), with amethyst inlays, cream candles and a brass halo. The circlet's worn layer adapts the owner's
Trinket Type Mod crown (`.../Trinket Type Mod/entity/crown.png`): its golds and its two plain lower rows become the
band; the points and patterned row are dropped and the red gem becomes a green eye. Colours only are taken; the
originals are untouched and nothing is read at generation time. The art has not been shown to the owner yet.

## Verification

- `python3 tools/check_mod_data.py`: new step 20 checks (`check_relics`): the Java's budgets, limits, contexts (in
  order) and shrine numbers equal the generator's; the definitions are the generator's; every relic works somewhere,
  never carried loose or worn for show, gives only an allowed status within the limits, has a recipe, a registration
  and an interval of whole checks; relics are never looked for inside containers and their charge never turns back into
  Ley Charge; effects go through the shared boundary; every reason, context, mode and message has its text; the shrine's
  animations are the ones its block entity plays; Jade, Trinkets data, the circlet's worn layer, the sheet and the icons
  agree. The pure-package rule covers `concordance/relic`.
- The pure core compiles with JDK 21, and the rules load the generated relics with no problems. The step 20 harness
  passes **44 checks** against them: every relic has a mode exactly in its named contexts; carried loose or worn for
  show none works; elsewhere each says wrong_context or cannot_install and lists where it works; the sky, the dark,
  calm, someone hurt, charge, the interval and the bond refuse in order; spending and charging stay within bounds; the
  budget allows two pulses a second per holder (four per shrine owner), slides, takes nothing when asked and survives a
  clock moved back; and the parser refuses loose or cosmetic contexts, empty or doubled contexts, self modes with a
  range or installed, ranged modes without one, free or over-capacity pulses and unknown fields.
- Game tests added: `ConcordanceRelicGameTests` (seven): a relic works only in its contexts (the Wardlight in the main
  hand does nothing and says where it works; in the off hand it reveals a spider for one charge, then rests; loose or
  worn for show it does nothing; each relic is found where it is, once; the report spends nothing); a relic says why it
  cannot activate (no charge, a roof, the dark, unhurt, just hurt, then mends and binds); relics are found in a real
  Trinkets necklace slot and worked there; a shrine works its installed relic (installed and lit, works at once, draws
  two Ley Charge and spends one charge, recharges a Stormglass instead of installing it, keeps relic, charge and owner
  through a save, refuses a stranger, gives the relic back to its owner and goes dark); a shrine installs only what can
  be installed (and nothing for someone without Relic Lore); relics share a budget (the third waits unspent) and never
  add up (a second Wardlight spends nothing); and a bound Hearthstone serves only its player, worn or installed.
- CI: run 37628449524 (commit e2b7feb6) passes the whole Build workflow: it builds, passes the data checks and all 1003
  required server game tests (the seven above among them), and the client test shards pass.

Not yet run: any client (the shrine's model and animations, the icons, the circlet on the head, Jade, the codex pages),
a two-client dedicated server, and an evening of play with several relics.

## World and event applicability

Works everywhere; "the dark" is night in the Overworld and always in dimensions without day. No seasonal content.

## Rollout and open questions

New registrations only: block and item `jugcraft:reliquary_shrine` and its block entity; items `jugcraft:wardlight`,
`jugcraft:hearthstone`, `jugcraft:stormglass` and `jugcraft:owlsight_circlet`; data component `jugcraft:relic`;
research `jugcraft:relic_lore`; practice `jugcraft:relic_pulse`; item tag `jugcraft:relic_specimens`; data folder
`concordance/relic`; Trinkets data giving players the necklace slot. Removing them needs a migration.

Open items:

- The shrine does not draw its installed relic (the crystal and halo stand for it).
- More relics, and relics for the legs, feet and chest slots, would use the same contexts.
