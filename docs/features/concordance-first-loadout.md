# Concordance first loadout: what a new caster makes, and where the rest comes from

Status: implemented for Concordance Chunk A (unit 4: the acquisition and JEI part of ticket A-07, and the acquisition
half of A1); it compiled and its server tests passed in #337's CI on `d4c07bf56` (see Verification). Concordance Chunk
B, unit 1 (B-02) adds the Yeti King's and the Cinder Tyrant's loot ([the chunk's contract](concordance-chunk-b.md)), not
merged; its data checks pass locally and its server tests pass in CI (runs 38514144531 and 38519234934). A-07's other
two asks are recorded elsewhere: the cast-pose rule in
[the presentation record](arcane-concordance-presentation.md#one-owner-for-the-arms-chunk-a-ticket-a-07), and the ritual
and pylon inspection in [the encyclopedia's first slice](jugcraft-encyclopedia.md#first-slice-concordance-chunk-a-unit-4).
Proposal issue: none. The owner chose Chunk A on 10 October 2026; [the chunk's contract](concordance-chunk-a.md) holds
the scope, and [the first-chunk tickets](../plans/concordance-expansion-2026-10-10/12-detailed-first-chunk-tickets.md)
(A-07) and [the delivery chunks](../plans/concordance-expansion-2026-10-10/09-claude-delivery-chunks.md) (A1) the asks.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: the Concordance's first steps (First Light understood, the Practitioner stage) in a fresh
survival world; the optional branches reach Hearthbinding, Relic Lore, Circle Lore and the lairs.
Primary specialty and supported player role: a first caster or supporter, and anyone looking up where a ritual's or a
lair boss's things come from.

## Player experience

**A first magic loadout from what survival already makes.** No recipe is added. A new player can make, in the
Overworld: the **Initiate's Wand**, a **Lampwright's Bench**, a **Kindled Lantern** (a bench working, once First Light is
understood), the owner's Ember regalia (the **Pyromancer's** set, or the cheaper **Pyromaniac's**, the **Lesser Focus
of Fire** and the **Fire Bangle**) and, as an optional branch, the owner's **Wayfaring trinkets**. Every one is the
owner's design or a Concordance item that already had a recipe or a working; none of it is new.

**The owner's armour tiers stay creative-only.** None of the 18 tiers in `tools/gear.py` `ARMOR_TIERS` (72 pieces),
the **Wayfarer** among them, gains a recipe, a drop, a shop or a trade here. They are still had only from the creative
tab, as the owner asked when each was made ([bloodthorn-armor.md](bloodthorn-armor.md),
[four-armor-designs.md](four-armor-designs.md), [armor-designs-8-october.md](armor-designs-8-october.md)). This record
does not claim that any of them became obtainable; how each is won is the owner's to decide (see Rollout).
`check_acquisition` fails if a recipe (its result, results or byproducts), a bench working, a ritual, the Assayer's
Scale, a loot table, a town shop, a villager trade, a commission or a project starts to give one before this record and
`tools/acquisition.py` say so. It reads data, and only the loot class's and the boss package's reward and deed lines;
other drops coded in Java are not seen (see Known limits).

**JEI says where ritual and boss things come from.** With JEI installed, each thing that only a ritual makes or only a
lair boss gives now has an information page (JEI's "Information" tab, shown when you look up the item's recipes), since
no recipe shows them:

| Item | What its page says, with the default data |
|---|---|
| Adept's Wand | Not crafted or traded: the Adept's Attunement turns an Initiate's Wand into it (keeping what was inscribed and tuned on it) at a Circle Anchor in a Lesser Circle, with Circle Lore understood; its offerings, Focus, steps and Ley Charge |
| Vesper Scythe | Vesperine, the Last Reaper, in the Hollow Acre (the Last Rites, at night): 15% in each share of her reward, and always on a player's first victory over her |
| Needle Rapier | Madame Tatterlace, in the Spindle Loft (a Cursed Spindle used on a Spinning Wheel, at night): 15% in each share, and always on a player's first victory over her |
| Reaper's Shade | 3 to 6 in each share of Vesperine's reward |
| Gossamer Silk | 4 to 8 in each share of Madame Tatterlace's reward |
| Dirge Skull, Requiem Skull | 50% each in each share of Vesperine's reward |
| Reaper's Hood | Crafted from 5 Reaper's Shade; or 20% in each share of Vesperine's reward, and another 10% chance during the Halloween event unless the server turns `lairs.event_loot` off |
| Tatterlace's Headdress | 20% in each share of Madame Tatterlace's reward, and another 10% chance during the Halloween event unless `lairs.event_loot` is off |
| Golden Thimble | 25% in each share of Madame Tatterlace's reward |
| Yeti Fur | The Yeti King, in the Glacier Hall (a Frost Horn blown on snow or ice, at night): 4 to 8 in each share of his reward |
| Glacier Maul, Rimeclaw | 15% in each share of the Yeti King's reward of one of his two trophies, 7.5% each, never both; always one of the two on a player's first victory over him |
| Yeti Mitten | 25% in each share of the Yeti King's reward |
| The Yeti King's Crown | 20% in each share of the Yeti King's reward |
| Tyrant Scale | The Cinder Tyrant, in the Cinder Kiln (a Kiln Seal pressed into a magma block in the Nether, the Volcano or the Cinder Barrens, at any hour): 3 to 6 in each share of his reward |
| Cinderbrand, Magmaw | 15% in each share of the Cinder Tyrant's reward of one of his two trophies, 7.5% each, never both; always one of the two on a player's first victory over him |
| Salamander Charm | 25% in each share of the Cinder Tyrant's reward |
| The Tyrant's Crest | 20% in each share of the Cinder Tyrant's reward |

Every boss page ends with who is paid, the rule of [lair encounter credit](lair-encounter-credit.md) (it names the kinds
of part, not their thresholds, so it stays true if the owner moves those), per boss: "A share goes to each player who
earns it in the fight by a blow, healing, a ward or an objective" for Vesperine, Madame Tatterlace and the Yeti King
(his objective is the column, B-P3), and "... by a blow, healing or a ward" for the Cinder Tyrant, who names no
objective (B-P4). `check_acquisition` holds the sentence to each boss's code: the boss is paid through `LairRewards`
from its fight's `Encounter`, and "an objective" is named exactly when its package credits a deed. For example, the
Vesper Scythe's page reads:

> Not crafted or traded. Vesperine, the Last Reaper, gives it in the Hollow Acre, which the Last Rites open at night.
> With the default data, each share of her reward has a 15% chance of it, and a player's first victory over her always
> brings it. A share goes to each player who earns it in the fight by a blow, healing, a ward or an objective.

and the Glacier Maul's (the Rimeclaw's is the same):

> Not crafted or traded. The Yeti King gives it in the Glacier Hall, which a Frost Horn blown on snow or ice opens at
> night. With the default data, each share of his reward has a 15% chance of one of his two trophies (7.5% each), and a
> player's first victory over him always brings one of the two. A share goes to each player who earns it in the fight
> by a blow, healing, a ward or an objective.

and the Salamander Charm's:

> Not crafted or traded. The Cinder Tyrant gives it in the Cinder Kiln, which a Kiln Seal pressed into a magma block in
> the Nether, the Volcano or the Cinder Barrens opens at any hour. With the default data, each share of his reward has a
> 25% chance of it. A share goes to each player who earns it in the fight by a blow, healing or a ward.

### The route (A1)

The first success itself (First Light understood, then a cast) is
[the journey record's](arcane-concordance-journey.md#the-first-success); composing without commands is the
[Spell Composer's](arcane-concordance-composer.md). This table is what a player makes along the way, with the
ingredients the recipe files on disk actually register.

Edge kinds: **required** (the first success needs it), **recommended** (the useful loadout), **trade-supported** (the
town sells an input), **optional** (a branch), **optional-boss** (from a lair boss; never the only route to anything).

| # | Step | Prerequisites | Registered ingredients | Edge |
|---|---|---|---|---|
| 1 | First Light understood | none | Sneak and use a luminous specimen (`#jugcraft:luminous_specimens`: amethyst shard, glowstone dust, glowcap, glimmerbloom, jack-o'-lantern mushroom, glow ink sac, glow lichen, glow berries), then one in light 4 or less; then three different ones in the dark, a study at a Lampwright's Bench, or another player's Research Notes | required; trade-supported (the Curiosities stall sells 4 glow ink sacs for 4 Jugs) |
| 2 | Initiate's Wand | none to craft; its invocations need First Light understood | `recipe/initiate_wand.json`: 1 `#jugcraft:concordance/luminous_matter` (an amethyst shard or glowstone dust), 1 stick, 1 copper ingot | required |
| 3 | Lampwright's Bench | none | `recipe/lampwright_bench.json`: 2 copper ingots, 1 luminous matter, 3 planks, 2 sticks | recommended (every research's study, and the lantern) |
| 4 | Kindled Lantern | First Light understood; a Lampwright's Bench | working `concordance/working/kindle_lantern.json`: 1 lantern and 1 amethyst shard (glowstone dust does not serve here); it starts with 8 Radiance and holds 64 | recommended; trade-supported (the General Store sells a lantern for 3 Jugs) |
| 5 | Pyromancer's set, or the Pyromaniac's | none to make or wear; its fire Spell Power counts for Hearthbinding's Cinderbolt and Hearthflare | `recipe/pyromancers_{hat,robes,leggings,boots}.json`: 19 wool (`#minecraft:wool`) and 5 gold ingots in all; `recipe/pyromaniacs_{hood,tunic,pants,shoes}.json`: 24 wool | recommended |
| 6 | Lesser Focus of Fire | none to wear (the Spell Focus slot); its power counts for Hearthbinding's invocations | `recipe/lesser_fire_focus.json`: 2 string, 2 gold ingots, 2 coal or charcoal (`#minecraft:coals`), 1 amethyst shard | recommended |
| 7 | Hearthbinding understood | First Light understood | Examine three different things that hold fire (coal, charcoal and a torch are had from the start), then a bench study or notes | optional (it gives the regalia its use) |
| 8 | Fire Bangle | its smouldering blow needs Hearthbinding understood | `recipe/fire_bangle.json`: 4 gold ingots, 1 coal or charcoal, 1 amethyst shard | recommended |
| 9 | Wayfaring trinkets (Leather Belt, Angelic Feather, Kraken Shell, Infernal Claws, Angelheart Vial, Phoenix Down, Amphibian Boot, Ice Breaker) | worn only once Relic Lore is understood: two different relic specimens examined (Heart of the Sea, Nautilus Shell, Totem of Undying, Eye of Ender, Recovery Compass; in the Overworld without luck, a Nautilus Shell and a raid evoker's Totem), then a bench study or notes | their own recipes (`tools/concordance_trinkets.py`), all of Overworld things | optional |
| 10 | Adept's Wand | Circle Lore understood (examine a compass, lead, calcite or chiseled stone bricks, then two different ones, then a bench study or notes); a Lesser Circle | ritual `concordance/ritual/adept_attunement.json`: 1 Initiate's Wand, 4 amethyst shards, 2 gold ingots, 4 glowstone dust; one practitioner, 6 Focus, 5 steps, 2 Ley Charge from each pylon a step (40 in all: one full 15-Radiance pour from a Kindled Lantern into each pylon, or 1,000 JE per Ley Charge). The circle: a Circle Anchor (1 amethyst shard, 2 gold ingots, 1 compass, 3 chiseled stone bricks), 4 Ley Pylons (each 1 amethyst shard, 2 copper ingots, 1 lapis lazuli, 3 stone bricks), 8 Warding Stones (4 a craft: 4 stone bricks, 4 amethyst shards, 1 calcite) | optional; trade-supported (the General Store sells a compass for 8 Jugs and the Curiosities stall a lead for 6: two different circle specimens) |
| 11 | Vesperine's loot | the Hollow Acre: the Last Rites at night in the Overworld (a headstone, 4 lit candles within 4 blocks, a Mourning Wreath on or beside it, the Death Knell rung within 4 blocks; [hollow-acre.md](hollow-acre.md)) | `recipe/death_knell.json`: gold ingot, iron nugget, bone; `recipe/mourning_wreath.json`: 4 mourning flowers, 1 vine | optional-boss; trade-supported (the Curiosities stall sells 4 candles for 3 Jugs, the Florist 4 lilies of the valley for 2) |
| 12 | Madame Tatterlace's loot | the Spindle Loft: a Cursed Spindle used on a Spinning Wheel at night in the Overworld ([spindle-loft.md](spindle-loft.md)) | `recipe/cursed_spindle.json`: 2 spider eyes, 3 string, 2 gold ingots, 1 amethyst shard, 1 stick; `recipe/spinning_wheel.json`: 3 planks, 3 sticks, 1 string | optional-boss |
| 13 | The Yeti King's loot | the Glacier Hall: a Frost Horn blown at night in the Overworld standing on snow or ice ([glacier-hall.md](glacier-hall.md)) | `recipe/frost_horn.json`: 2 leather, 2 gold ingots, 2 snow blocks, 1 goat horn | optional-boss; trade-supported in winter (the Seasonal Stall sells 8 snow blocks for 3 Jugs under its winter and December themes) |
| 14 | The Cinder Tyrant's loot | the Cinder Kiln: a Kiln Seal pressed into a magma block in the Nether, the Volcano or the Cinder Barrens, at any hour ([cinder-kiln.md](cinder-kiln.md)) | `recipe/kiln_seal.json`: 2 blaze powder (the Nether's), 4 obsidian, 1 magma block, 2 gold ingots | optional-boss |

**The town is a seller of little on this route, and a buyer of much.** The walled town ([walled-town.md](walled-town.md))
stands near the start of every new world and gives 20 Jugs on a first visit inside its wall. Its **General Store buys**
amethyst shards (8 for 4 Jugs), copper ingots (16 for 5), gold ingots (4 for 8), leather (4 for 3), white wool (8 for
3), feathers (16 for 2), string and coal; **it does not sell them**. What the route can buy is listed above: a lantern, a
compass, glow ink sacs, a lead, candles and lilies of the valley (`data/jugcraft/town/shops.json`); outside those, the
Seasonal Stall's themed offers add red wool in December (4 for 3 Jugs), which the fire sets take, and coloured candles
in their seasons. The only villager
trades in the mod's data are the Halloween Peddler's and the Retro Trader's, neither on this route.

**No circular unlock.** Steps 1 to 6 can be done with Overworld vanilla things alone, each needing only steps above it.
No research, recipe or stage needs anything a boss gives: the bosses' materials make only the Reaper's Hood, string
(`recipe/string_from_gossamer_silk.json`), white wool (`recipe/white_wool_from_yeti_fur.json`), magma cream
(`recipe/magma_cream_from_tyrant_scale.json`) and cheaper alternatives of the lairs' own entry items
(`recipe/mourning_wreath_from_shade.json`, `recipe/cursed_spindle_from_silk.json`, `recipe/frost_horn_from_fur.json`,
`recipe/kiln_seal_from_scales.json`), whose plain recipes stay. The lairs need no magic.

### Every item this chunk touches (A1)

| Items | Role | How it is had |
|---|---|---|
| `initiate_wand` | instrument | crafted |
| `adept_wand` | instrument | ritual only (no recipe) |
| `kindled_lantern` | held light, a Radiance holder | bench working |
| `lampwright_bench` | station | crafted |
| `pyromaniacs_{hood,tunic,pants,shoes}`, `pyromancers_{hat,robes,leggings,boots}` | armour (fire regalia) | crafted |
| `lesser_fire_focus`, `fire_focus`, `fire_bangle` | worn in Trinkets slots | crafted (the Focus of Fire from the lesser one, 2 diamonds and 2 gold ingots) |
| `leather_belt`, `angelic_feather`, `kraken_shell`, `infernal_claws`, `angelheart_vial`, `phoenix_down`, `amphibian_boot`, `ice_breaker` | worn in Trinkets slots | crafted; worn only after Relic Lore is understood |
| `vesper_scythe`, `needle_rapier` | weapons (Arms VII trophies) | loot only (and always on a player's first victory) |
| `reaper_hood` | head costume, no armour value | crafted and loot |
| `tatterlace_headdress` | head costume | loot only |
| `golden_thimble` | off-hand projectile deflector | loot only |
| `dirge_skull`, `requiem_skull` | trophy blocks, worn as costumes | loot only; decorative |
| `reaper_shade`, `gossamer_silk` | materials | loot only |
| `glacier_maul`, `rimeclaw` | weapons (Arms VII trophies) | loot only, one of the Yeti King's two (and always one of them on a player's first victory) |
| `yeti_mitten` | off-hand ward against freezing | loot only |
| `yeti_king_crown` | head costume | loot only |
| `yeti_fur` | material | loot only |
| `cinderbrand`, `magmaw` | weapons (Arms VII trophies) | loot only, one of the Cinder Tyrant's two (and always one of them on a player's first victory) |
| `salamander_charm` | off-hand charm against hot ground | loot only |
| `tyrant_crest` | head costume | loot only |
| `tyrant_scale` | material | loot only |
| the 18 owner armour tiers' 72 pieces (`wayfarer_*` among them) | armour | creative-only |
| `hades_scythe`, `sentinel_longsword`, `frost_knight_greatsword`, `wight_king_zweihander`, `reaper_scythe`, `sentinel_shield` | the armour sets' arms and shield | creative-only (`check_arms_variants` forbids their recipes and drops until the owner settles how a set is won) |
| the 12 trophies of the 6 bosses not yet built (`hagthorn`, `bogfang` and the rest) | weapons | listed only in `loot_table/bosses/<boss>.json`, which nothing rolls: not had in survival yet |

### Each selected item (A-07)

| Item | Model and role | Acquisition and gap | Ingredients on disk | Knowledge needed | Compared with | Equivalence | Owner art and runtime view |
|---|---|---|---|---|---|---|---|
| Initiate's Wand | handheld item model; the first instrument (main hand) | crafted; JEI lists its recipe in single-player, but on a server only with JEI there too (see Limits) | route step 2 | none to craft; First Light understood to cast | holds spells of up to 8 capacity, 6 targets, 48 work, 1 branch (the Adept's: 12, 8, 72, 2) | excluded | Jugcraft-drawn (`tools/item_icons/initiate_wand.txt`); seen in `ConcordanceJourneyClientGameTests` |
| Kindled Lantern | lit and unlit item models; a held light and Radiance holder, the Ley source for pylons | bench working; JEI's Lampwright's Bench category already shows it | route step 4 | First Light understood | nothing comparable | excluded | Jugcraft-drawn (`tools/item_icons/kindled_lantern.txt`); made at the bench in `ConcordanceGameTests`; held in hand in `EncyclopediaClientGameTests` (screenshot `jugcraft_first_loadout_kindled_lantern`, not yet run) |
| Pyromancer's set (Pyromaniac's the cheaper) | the owner's item icons; worn as the owner's GeckoLib model in each set's texture | crafted; no gap | route step 5 | none to wear; its +0.5 fire Spell Power a piece counts only for Hearthbinding's invocations (four pieces: +1 damage) | Pyromancer's 7 armour (1/3/2/1, leather's), 10x durability, enchantability 15; Pyromaniac's 5, 7x; Thallite 15, 13x, 18, Regrowth; Steel 19, 25x, toughness 1.5; the creative-only Wayfarer 20 (netherite's), 50x, 30 | excluded | the owner's, with SHA-256 table: [Ember regalia](arcane-concordance-ember-regalia.md); seen in `ConcordanceEmberGearClientGameTests` (`jugcraft_ember_regalia_*`) |
| Lesser Focus of Fire, Fire Bangle | the owner's item icons; Spell Focus and Bracelet slots | crafted; no gap | route steps 6 and 8 | none to wear; their power and the bangle's blow need Hearthbinding understood | +2 fire Spell Power (+1 damage; the Focus of Fire +4) | excluded | as the set |
| Wayfaring trinkets (optional) | the owner's icons; belt and boot drawn worn | crafted; gap: worn only after Relic Lore | route step 9 | Relic Lore understood | utility and safety (a Charm slot; food paying a fall's, drowning's or fire's harm; swimming; death saves; the Ice Breaker's landing wave) | excluded | the owner's, with SHA-256 table: [Wayfaring](arcane-concordance-trinkets.md); seen in `ConcordanceWayfaringClientGameTests` (`jugcraft_wayfaring_*`) |
| Adept's Wand | handheld item model; the second instrument | ritual; JEI's Circle Anchor category already shows the offerings; **gaps fixed here**: no information page said it is not crafted or what the ritual needs, and it was missing from the equivalence exclusions | route step 10 | Circle Lore understood | see the wand row | **excluded (added here)** | Jugcraft-drawn (`tools/item_icons/adept_wand.txt`); made in `ConcordanceRitualGameTests`; held in hand in `EncyclopediaClientGameTests` (screenshot `jugcraft_first_loadout_adept_wand`, not yet run) |
| Vesper Scythe, Needle Rapier | Arms VII trophies (icon and 3D in-hand model) | loot only; **gap fixed here**: no JEI page | none | none | a steel arm with a boon (Harvest; Stitch), twice steel's durability | not catalogued, so the scale refuses it (a boss trophy is not mundane matter) | Jugcraft-drawn (`tools/arms_variants_art.py`); in frames and on stands in `ArmsVIIClientGameTests` (`jugcraft_arms_vii_frames`, `jugcraft_arms_vii_rack_*`); a first kill brings the scythe in `VesperineClientGameTests` |

Not selected for the loadout, and unchanged: the Reaper's Hood, Tatterlace's Headdress, Golden Thimble and skull
trophies (costumes and a curio from the lairs; their art is Jugcraft's, see [vesperine.md](vesperine.md) and
[tatterlace.md](tatterlace.md)), and everything creative-only above.

## Connections

- Existing input producer: survival's own crafting and the Lampwright's Bench; the town's shops for a lantern, a compass,
  a lead, glow ink sacs, candles and lilies of the valley; the lairs for their loot.
- Existing output consumer: the Concordance's instruments and invocations, Hearthbinding's fire Spell Power, the
  Trinkets slots, the lairs' fights (the loadout is a caster's or a warder's kit there), and JEI.
- Technology connection: none needed; a Ley Pylon also takes electricity (1,000 JE a Ley Charge).
- Magic connection: First Light (required), Hearthbinding, Relic Lore and Circle Lore (optional).
- Reachable entry path: the route table; nothing in it waits on a later tier, a boss or another dimension.
- Required vs optional; trade and solo routes: only steps 1 and 2 are required. Everything is made solo; the town's
  sales are a convenience, never the only route.
- How this specialty stays useful without mastering every other branch: the regalia and wands are worth having at the
  Practitioner stage, and a player who never fights a boss loses nothing on the route.

## Balance and automation

- **No new recipe, drop, trade or number.** Every cost in the route table is a recipe, working or ritual that already
  existed; the only data that changed is the equivalence exclusion tag and JEI's rows and words.
- **The regalia is a caster's set, not plate.** The Pyromancer's set has leather's protection (7); players may well keep
  wearing iron, Thallite or Steel. Its worth is fire Spell Power once Hearthbinding is understood.
- **Why the Wayfarer is not on the route.** Its numbers are netherite's defense (20) with 50x durability and
  enchantability 30 (`tools/gear.py`). Any first-route cost would put it above Steel (19) and Thallite (15) for leather
  and wool, against the owner's record that these sets "should cost at least a netherite set's worth"
  ([four-armor-designs.md](four-armor-designs.md)). Two ways to give it a route are offered to the owner under Rollout.
- **The Adept's Wand is never weighed.** It joins `#jugcraft:equivalence/excluded`, as the equivalence record already
  said of wands; `check_equivalence` now requires every instrument there. (Before, the scale refused a plain one as not
  catalogued and an inscribed one as magical; now any is refused as "more than matter". Nothing could be dissolved
  either way.)
- No conversion is added, so no loop is possible.

## Multiplayer and persistence

- Server authority is unchanged: recipes, workings, rituals and loot are the server's, as before.
- The JEI pages are a client copy of the default data (below), read once per JEI reload; they hold no state.
- No saved data, registration or id changes. The excluded tag gains an entry; removing it again needs no migration.
- Disable behaviour: with JEI absent nothing loads (the plugin is reached only through JEI's entrypoint). The pages are
  still shown when a server has switched the Concordance or the lairs (the `agriculture` feature) off; they describe the
  default.

## Dependencies and assets

- No new dependency or pin. JEI (31.8.0.49, optional, already locked) shows the pages through
  `IRecipeRegistration.addIngredientInfo(ItemLike, Component...)`, the overload the plugin already called for the
  Resonant Ring and the alchemy pages (JEI's API documents it since 19.18.3).
- No art. The pages' words are generated by `tools/acquisition.py` into `en_us.json`.

**How it works.** `tools/acquisition.py` is a pure data module that writes each page from the generators' own tables:
`tools/vesperine.py` (`CHANCES`, `SHADE`, `EVENT_HOOD_CHANCE`, `RECIPES`), `tools/tatterlace.py` (`CHANCES`, `SILK`,
`EVENT_HEADDRESS_CHANCE`), `tools/yeti_king.py` (`FUR`, `TROPHIES`, `TROPHY_CHANCE`, `CHANCES`; since Chunk B),
`tools/cinder_tyrant.py` (`SCALES`, `TROPHIES`, `TROPHY_CHANCE`, `CHANCES`; since Chunk B),
`tools/concordance_rituals.py` (`RITUALS`, `STRUCTURES`, `RESEARCH`), `tools/lairs.py` (the lairs' names and tokens,
and the Kiln Seal's lands) and `tools/biomes.py` (those lands' names); no number is typed twice. `generate_material_data.py` writes the rows to
`assets/jugcraft/recipe_view.json` (`"acquisition_info"`: an item and a key each) and the words to `en_us.json`
(`jei.jugcraft.acquire.<key>`). `client/compat/JugcraftJeiPlugin.addAcquisitionInfo` reads the rows and adds a page for
each registered item. The same module lists the loadout (`LOADOUT`, `OPTIONAL`) and the creative-only tiers
(`CREATIVE_ONLY`) for the checker. Since Concordance Chunk B, unit 4 ([the winter record](arcane-concordance-winter.md);
not yet merged), `OPTIONAL` also holds Rime's travel kit (the two water foci, the Water Bangle and the twelve pieces of
the three water sets), made by recipe as the Focus of Fire is; they are Rime's, not part of First Light's loadout, and
have no JEI information page (JEI shows their recipes). `tools/concordance_journey.py` holds each of the fifteen to the
same rule as the loadout (one player, the Overworld, before the Adept stage), and
`ConcordanceGameTests.FIRST_LOADOUT_RECIPES` asks the server for their recipes too (`theFirstLoadoutIsMadeInSurvival`
passed in CI run 38549876709; not merged).

**Limits, said plainly.**
- The pages are written from the data the mod ships. A server whose data pack changes a boss's loot table or the
  Adept's Attunement is not reflected; every page says "with the default data" for that reason. The first-victory
  trophy is code (`VesperineLoot`, `TatterlaceLoot`, `YetiKingLoot`, `CinderTyrantLoot`), not data.
- `loot_table/bosses/vesperine.json` and `tatterlace.json` (written by `tools/arms_variants.py`, as every boss's trophy
  table is) are rolled by nothing; the bosses' rewards are `loot_table/entities/<boss>.json`. A data pack that edits
  those two `bosses/` tables changes nothing in game. The Yeti King's and the Cinder Tyrant's are rolled:
  `YetiKingLoot` and `CinderTyrantLoot` roll `loot_table/bosses/yeti_king.json` and `cinder_tyrant.json` on a player's
  first victory when their share brought neither of the boss's trophies.
- JEI's own crafting pages list Jugcraft's crafting recipes only when JEI's recipe sync reaches the client: its Fabric
  half synchronises the `minecraft:` recipe serializers through Fabric's recipe sync, so a single-player world (whose
  built-in server runs JEI) has them, but a dedicated server without JEI sends none and JEI falls back to vanilla's
  own recipes. That is why the Reaper's Hood's page names its recipe. (Read in JEI's 31.9 source; the pin is 31.8.0.49.)
- The pages show exact chances; whether they should is an owner decision (below).

## Verification

Ran here (11 October 2026):
- `python3 tools/generate_material_data.py`: regenerated; the only changes to generated files are the ten
  `jei.jugcraft.acquire.*` words in `en_us.json`, the `acquisition_info` rows in `recipe_view.json` and
  `jugcraft:adept_wand` in `tags/item/equivalence/excluded.json`.
- `python3 tools/check_mod_data.py`: **passes** ("PASS: 2149 material IDs, data files and recipe audit"), with the new
  `check_acquisition` (each page, the loadout and the creative-only tiers against the files on disk, as its docstring
  lists) and the new equivalence rule (every instrument excluded). Nothing in `check_arms_variants` is relaxed (a
  trophy or a set's arm still has no recipe of its own); the unit adds one rule to it, for the handbook's trophy page: a
  `tools/arms_variants.py` `BOSSES` entry names a lair (a `tools/lairs.py` `LAIRS` key) exactly when
  `loot_table/entities/<boss>.json` exists ([arms-vii.md](arms-vii.md)).
- The new audit was also run with deliberate faults, one at a time and in memory only: a generator chance moved, the
  scythe's pool removed, the shade count changed, an item with no page added to her table, a creative-only tier given a
  recipe, a loadout piece without its recipe or with the wrong working, the first-kill line or the table key removed
  from `VesperineLoot.java`, the Halloween chance changed, a recipe and a shop made to give a "not crafted or traded"
  thing, the ritual's gold and Focus changed, and the server test's recipe list changed. Each was reported. After a
  review found that the first version read only a recipe's top-level `result`, the sources were widened; on a scratch
  copy of the data, a cutting recipe's `results` and a pulverizing recipe's byproduct made to give a "not crafted"
  thing, a bench working and a project reward made to give a Wayfarer piece, a second ritual made to give the Vesper
  Scythe or the Adept's Wand, and the Assayer's Scale's catalogue given the Needle Rapier were each reported (and the
  same catalogue entry with `"form": false` was not). Taking the
  Adept's Wand out of `EXCLUDED` was reported by `check_equivalence`.
- `python3 scripts/check_repository.py`: **passes**.
- `python3 tools/concordance_journey.py` (and so `check_journey`): **passes**, with a new rule that every piece of
  `tools/acquisition.py` `LOADOUT` is made by one player alone, in the Overworld, before the Adept stage, in the
  progression graph (`tools/concordance_progression.py`: each piece's recipes or bench working, down to what is found
  in the world). Run with a piece nothing makes, and with one that needs the Nether's blaze powder (the Hearthstone),
  each was reported.

Passed in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953)): the server game test `ConcordanceGameTests.theFirstLoadoutIsMadeInSurvival`, which asks the server
for the recipe of every first-loadout piece a crafting table makes (the optional trinkets and the Focus of Fire
included); `check_acquisition` keeps its list the generator's. The Kindled Lantern's working is already exercised by
`ConcordanceGameTests.benchWorkingsMakeAndChargeALantern`, and the Adept's Attunement by `ConcordanceRitualGameTests`.

`JugcraftJeiPlugin` compiles in CI. Not run: the pages in a client with JEI (no test opens JEI); a person following
the route in survival.

Concordance Chunk B, unit 1 (B-02), the Yeti King's pages; ran here (14 October 2026):
- `python3 tools/generate_material_data.py`: the only generated changes are the Yeti King's five
  `jei.jugcraft.acquire.*` words and the encyclopedia's dossier words in `en_us.json`, and five more `acquisition_info`
  rows (15 in all) in `recipe_view.json`; the ten existing pages are unchanged.
- `python3 tools/check_mod_data.py`: **passes**, with `check_acquisition` generalised: each boss's loot class named in
  its row (the Yeti King's is `lair/yeti/YetiKingLoot.java`); each single thing one roll of a pool of its own; a pair of
  trophies one pool of their own, at the stated chance, each as likely; the boss's trophy table, one pool, rolled on a
  first victory that brought neither; a Halloween extra only where a page states one; the boss paid through
  `LairRewards` from its `Encounter`; and "an objective" named exactly when its package credits a deed.
- Deliberate faults, one at a time on a scratch copy, each reported: his trophies split into two pools, their pool's
  chance moved, a weight on one, two rolls, bonus rolls, the mitten's pool holding the crown, the mitten's chance and the
  fur's count moved, an item with no page in his table, his trophy table given a chance or split, the first-kill roll
  removed or pointed at his loot table, `trophy()` forgetting the Rimeclaw, `TROPHIES` naming another boss's table, a
  Halloween chance added, his payer dropped, the old `YetiKingLoot.reward` payment restored, `BOSS` renamed, the column
  deed removed, the old derived loot-class path, a row with both a trophy and a pair, the generator's chance moved
  without regenerating, a recipe making the mitten, a shop selling Yeti Fur, a chest table giving the Rimeclaw, and on
  the older pages Vesperine's first-kill line, her scythe's pool holding the hood, Madame Tatterlace's thread deed and
  the hood's Halloween chance. Adding #336's Mire Hag table with no pages was not reported, as intended.
- `python3 scripts/check_repository.py` and `python3 tools/concordance_journey.py`: **pass**.
- `YetiKingGameTests.hisLootIsWhatHisPagesSay` (400 shares against the pages' chances) **passes in CI** (runs
  38509414542, 38514144531 and 38519234934, by workflow_dispatch; not merged).
- Not run: the pages in a client with JEI.

Concordance Chunk B, unit 1 (B-02), the Cinder Tyrant's pages (B-D1); ran here (14 October 2026):
- `python3 tools/generate_material_data.py`: the only generated changes are his five `jei.jugcraft.acquire.*` words and
  the Cinder Kiln's three dossier words in `en_us.json`, and five more `acquisition_info` rows (20 in all) in
  `recipe_view.json`; the fifteen existing pages are unchanged.
- `python3 tools/check_mod_data.py`: **passes**; `check_acquisition` holds his row as it holds the Yeti King's, his
  pages naming no objective because `lair/tyrant` credits no deed (B-P4).
- Deliberate faults, one at a time on a scratch copy, each reported: his row naming an objective, a deed credited in
  `lair/tyrant`, his trophies split into two pools, the Salamander Charm's chance moved in his table, and his payer
  dropped (with eight against `check_encyclopedia`, in [the chunk's contract](concordance-chunk-b.md)).
- `python3 scripts/check_repository.py` and `python3 tools/concordance_journey.py`: **pass**.
- `CinderTyrantGameTests.hisLootIsWhatHisPagesSay` (400 shares: 3 to 6 Tyrant Scales each, never both trophies, and
  the Cinderbrand, the Magmaw, the charm and the crest each within five standard deviations of 7.5%, 7.5%, 25% and
  20%) **passes in CI** (the same three runs).
- Not run: the pages in a client with JEI.

## World and event applicability

The route is the Overworld's; nothing needs the Nether or the End (the Eye of Ender is only one of five relic
specimens). The lairs keep their containment and their own rules. Seasonal: the Halloween event adds a roll of
seasonal candy to each share and the extra hood and headdress chance the pages state; nothing on the route depends on
the event, and with the default `lairs.off_season` the rites open the lairs on any night.

## Rollout and open questions

Owner decisions (all proposed; none is taken by this change):

| Decision | Proposal | Status |
|---|---|---|
| A-D10: how the Wayfarer (and the other owner tiers) are won | Keep them creative-only for Chunk A. Later, for the Wayfarer, choose between (a) re-tiering it as a light caster's set (for example the Pyromancer's 1/3/2/1 at 10x durability) with a leather, wool and luminous-matter recipe, or (b) keeping netherite's numbers and winning it by smithing a netherite piece with leather (a netherite upgrade template, the netherite piece and leather: at least a netherite set's worth, as the owner's record asks). The numbers stay the owner's. | Proposed |
| A-D15: spoiler policy for acquisition text | JEI's pages and the encyclopedia's lair dossiers state the exact chances (7.5%, 15%, 20%, 25%, 50%) and the first-victory guarantee, always "with the default data"; the alternative is to name the boss and say "sometimes". The encyclopedia's discovery rule is A-D9, proposed in [the chunk's contract](concordance-chunk-a.md#decisions). | Proposed |
| A-D11: should a shop sell first-route materials? | Today the town sells a lantern, a compass, a lead, glow ink sacs, candles and lilies of the valley (and, at the Seasonal Stall in December, red wool), and buys amethyst, copper, gold, wool and leather. Selling amethyst shards (say) would answer chapter 9's question on crafting- or trade-oriented acquisition, but the town's rule that no item is both sold and bought (`tools/town_shops.py`, checked by `check_mod_data`) means the General Store would stop buying it. | Proposed |

Known limits and findings:
- A-07's presentation rule (a cast pose never overriding a gun reload or an arms action, in first and third person
  and for other players) is a separate change of the same unit, recorded in
  [the presentation record](arcane-concordance-presentation.md#one-owner-for-the-arms-chunk-a-ticket-a-07).
- `check_acquisition` reads data, and only the loot class's and the boss package's reward and deed lines; other drops
  coded in Java are not seen (a creature's drops coded in its class, such as `creatures/scary/ScaryDrops`, which already
  drops steel armour), so a Java drop of a "not crafted" thing or of a creative-only tier would pass the data checks;
  review catches it.
- `tools/concordance_progression.py` gives `minecraft:ender_eye` no dimension although its blaze powder is the
  Nether's; the progression audits may count it as an Overworld relic specimen. It predates this chunk and is not
  changed here; Relic Lore stays reachable in the Overworld through a Nautilus Shell and a Totem of Undying.
- The pages are static text in English (`en_us.json`), like the rest of the mod's words.
- The pages cover the four bosses on `Encounter` that `tools/acquisition.py` lists: Vesperine, Madame Tatterlace and,
  since Chunk B, the Yeti King and the Cinder Tyrant. A boss with a table and no pages (the Mire Hag #336, the Crypt
  Lich #350/#353) is not held to pages until it gets a row and `LairRewards`.
- Reverting: drop `tools/acquisition.py`'s rows and words, the plugin's `addAcquisitionInfo` and the audit; nothing in
  a world depends on them.
