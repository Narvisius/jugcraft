# Arcane Concordance: Ember, part 2 (the Hearthbinder's regalia)

Status: merged to main ([#295](https://github.com/jimbozoomer-byte/jugcraft/pull/295), integrated by
[#277](https://github.com/jimbozoomer-byte/jugcraft/pull/277)); see Verification for what has run.
Proposal issue: none; the owner asked for their supplied magic content to be built into Jugcraft (9 October 2026) and
authorized it at the top of [CLAUDE.md](../../CLAUDE.md). This part follows [Ember, part 1](arcane-concordance-ember.md).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Practitioner stage, beside part 1. The regalia's power matters only to Hearthbinding's
invocations; the two fire sets are also plain cloth and leather-grade armour for anyone.
Primary specialty and supported player role: the Hearthbinders; casters who fight with fire, in light armour.

## Player experience

- **Lesser Focus of Fire** and **Focus of Fire**: worn in the new **Spell Focus** slot, +2 and +4 fire Spell Power.
  Cinderbolt and Hearthflare add half a point of damage for each whole point, so the lesser focus adds 1 and the focus 2.
  A focus holds no Focus (the resource): Focus is a player's own.
- **Fire Bangle**: worn in one of the two new **Bracelet** slots. Once you understand Hearthbinding, your own melee blows
  (within reach of the weapon in your hand) leave the creature **smouldering** for 3 seconds, wherever you may harm it.
  It gives no Spell Power, and a second bangle adds nothing.
- Two fire sets, each piece half a point of fire Spell Power (only four pieces together add a point of damage), repaired
  with wool, fire resistant as items, worn as the owner's own 3D model:
  - **Pyromaniac's Hood, Tunic, Pants and Shoes** (light): cloth, a little less protection than leather (5 in all), made
    of wool alone. Worn with the owner's light fire texture.
  - **Pyromancer's Hat, Robes, Leggings and Boots** (medium): wool and gold, leather's protection (7 in all), longer-lasting.
    Worn with the owner's medium fire texture.
  The owner's light fire texture is the medium sheet, byte for byte, so the two sets look alike when worn; their icons are
  the owner's own for each piece.
- At its most (a Focus of Fire and four pieces) the regalia gives **+6 fire Spell Power: Cinderbolt deals 6, Hearthflare
  7**. Spell Power's own attribute enchantments (Sunfire and its kind) cannot be put on either set.
- A new codex entry, **A Hearthbinder's Regalia** (Hearth category), shows each recipe and explains the numbers.
- **Hearthflare now sounds with the owner's four fire recordings**, one picked at random.

## How it works

- Data (`tools/concordance_ember.py`, part 2 section): the eleven items, their recipes, the two fire sets (`ARMOR_SETS`),
  the owner's two slots as Trinkets slots (`data/trinkets/slots/chest/spell_focus.json`, `hand/bracelet.json`, the item tags
  and `data/trinkets/entities/jugcraft_ember.json`), a repair tag for each set, the codex entry and the Hearthflare sound
  list. `tools/gear.py` puts the sets in `#minecraft:head_armor` and its kin (one writer per file);
  `tools/concordance_equivalence.py` excludes the eleven from the Assayer's Scale.
- Java (`concordance/ember`): `EmberGear` registers the items, the two sets' armour materials and two listeners. The foci are
  `EmberTrinketItem`s, whose fire Spell Power is a Trinkets callback modifier (as the Resonant Ring's), so it counts only
  worn in a slot that applies effects, never in the hand. The sets are `EmberArmorItem`s (GeckoLib `GeoItem`s), each with
  a vanilla `spell_power:fire` modifier in its own slot, and each knowing its set (its texture and equipment asset) and the
  model it is worn as (`EmberGear.ARMOR_MODEL`, the owner's medium model, for both). The bangle's blow is a `ServerLivingEntityEvents.AFTER_DAMAGE`
  listener: a player's own melee hit (vanilla's player-attack damage, dealt in person, not mounted, landing within
  `BANGLE_REACH_MARGIN` (a block) of the reach of the weapon in hand: their blows, its sweep and Spell Engine's melee
  weapon skills, but not a Shock arc's second foe or a skill landing farther off), with the bangle worn and Hearthbinding
  understood, applies Smoulder through `ConcordanceEffects.apply`. An
  `EnchantmentEvents.ALLOW_ENCHANTING` listener refuses `#spell_power:requires_matching_attribute` enchantments on the
  sets.
- Client: `EmberClient` hands GeckoLib one `EmberArmorRenderer` for each set (through `EmberHooks`, as the guns do): the
  set's texture on the owner's model (GeckoLib's `withAltModel`). GeckoLib poses the
  model's armour bones to the wearer; the renderer then hides the slim sleeves (vanilla draws the same sleeves for every
  arm width), draws nothing on babies and small armour stands, and adds vanilla's armour glint on an enchanted piece.
- Nothing ticks: Spell Power is Trinkets' and vanilla's attribute modifiers; the blow is a damage event.

## Connections

- Input producer: Overworld gold, amethyst, coal or charcoal, string, diamonds and wool (crafting table). The light set
  needs wool alone.
- Output consumer: Cinderbolt and Hearthflare (fire Spell Power, part 1); creatures through the shared effect boundary
  (the bangle's Smoulder); the step 11 combat benchmark.
- Technology connection: none needed; the gear is crafted, repaired and enchanted as vanilla gear.
- Magic connection: fire Spell Power, the school the Hearthbinders' invocations scale with; the bangle's blow needs
  Hearthbinding understood and goes through the same PvP, party, claim and tolerance rules as every harmful effect.
- Reachable entry path: every input has an Overworld source in the progression graph (`tools/concordance_progression.py`;
  `check_ember_regalia` checks each recipe). The power is useful from Hearthbinding understood (part 1's entry path).
  No research, practice or stage requires any of these items, so no unlock can become circular.
- Solo, trade and cooperative routes: entirely solo; the items are ordinary stacks.
- Specialty use without other branches: needs only First Light and Hearthbinding.

## Balance

- **Units.** Fire Spell Power is counted in points above the school's base; Cinderbolt and Hearthflare add
  `floor(0.5 × points)` damage (part 1's scaling, `compose/Plan.scaled`).

  | Worn | Fire Spell Power | Cinderbolt | Hearthflare (each of at most 4) |
  |---|---|---|---|
  | nothing | 0 | 3 | 4 |
  | Lesser Focus of Fire | 2 | 4 | 5 |
  | Focus of Fire | 4 | 5 | 6 |
  | four pieces of either set | 2 | 4 | 5 |
  | Focus of Fire and three pieces | 5.5 | 5 | 6 |
  | Focus of Fire and the whole set (the most) | 6 | 6 | 7 |

- **The ceiling, +6, is the best arcane Resonant Ring a player can forge** (affix 2, rune 1, gems 3:
  `tools/concordance_artifice.py`, checked by `check_mod_data`). Cinderbolt stays below the Lance (base 3 against 5).
  The bangle gives no Spell Power, so the two Bracelet slots add nothing to it.
- **Sunfire is refused on the sets.** Spell Power lets its attribute enchantments onto any item that carries one of their
  attributes; on a set, Sunfire V on four pieces would multiply fire Spell Power (and an arcane ring's arcane) by 1.6:
  (1 + 6) x 1.6 = 11.2, which is 10.2 above the base, so Cinderbolt 8 and Hearthflare 9 instead of 6 and 7. `EmberGear`
  refuses that tag on both sets; ordinary armour enchantments still apply.
- **The two sets give the same fire.** The light set is the cheap one (wool alone, less protection, ×7 durability); the
  medium set costs five gold for leather's protection and ×10 durability. Pieces mix freely: any four add the point.
- **Partial sets.** Half points are lost to the rounding, so fewer than four pieces add no damage; the codex and the
  tooltips say so, and `theRegaliaRaisesTheFireToItsCeiling` checks it (three pieces: 5.5, Cinderbolt 5).
- **Armour.** The light set 1/2/1/1 (5) at seven times vanilla's base durability; the medium set leather's 1/3/2/1 (7) at
  ten times (leather: five); both leather's enchantability. Both are below chain (12) and iron (15), so neither undercuts
  fighters' armour.
- **The bangle's blow** is 3 seconds of Smoulder (about 3 damage at vanilla's burning pace, refreshed, never faster), the
  same as the Arms Ember boon on a weapon (`weapons/ArmVariants`) and less than the brazier's Ignite trait (4 s). It costs
  no Focus, as weapon boons cost nothing; it needs Hearthbinding understood, and water, rain, Fire Resistance and fire
  immunity answer it. Worn with an Ember-boon weapon the two set the same fire; nothing stacks.
- **No conversion loops:** no recipe makes a material, and nothing turns back into its inputs.
- **Benchmark.** A new step 11 character, "Geared hearthbinder (+6 fire, Pyromancer's)" (leather, wooden sword,
  Hearthguard, Cinderbolt and Hearthflare mastered at +6 fire), must pass the acceptance rules, and rule 4 now also
  requires its Cinderbolt and Hearthflare to exceed the plain ones and its Hearthguard to be unchanged. It wins all five
  encounters, with 18, 11, 17, 3 and 20 health left; like the geared striker, not unharmed in all of them. The same kit
  with no fire gear wins three, with a Lesser Focus four, with a Focus of Fire all five; the whole regalia nearly
  doubles its damage per Focus (0.74 to 1.45). The model has no on-hit statuses, so the bangle's blow is not in it, and
  it counts Smoulder as control rather than damage. Adding the character found a gap in the model, which could not apply
  Hearthguard's Fire Resistance and so kept beginning the cast instead of fighting; helpful statuses now land in the
  model by their stacking rule, as on the server ([baselines](arcane-concordance-baselines.md#findings)).

## Multiplayer and persistence

- Server authority: Spell Power is applied by Trinkets and vanilla on the server; damage is decided in the server's
  impact (part 1). The bangle's blow is decided on the server from the damage event; it asks the shared effect boundary,
  so PvP off, parties, claims (`Authority.mayStrike`), creative and spectator players, tolerance tags and the feature
  switch all refuse it. No packets or client claims are added.
- Failure behaviour: no Smoulder before Hearthbinding is understood, for a bangle in a cosmetic slot or taken off, for a
  mounted, ranged, spell (of a school), blocked or zero-damage blow, for one landing more than a block beyond the reach
  of the weapon in hand (a Shock arc's second foe, a far weapon skill), or where the wearer may not harm the creature. A
  focus in the hand or a cosmetic slot gives nothing. Armour trims: the smithing table accepts them on both sets (the
  pieces are in `#minecraft:<slot>_armor`, which `#minecraft:trimmable_armor` includes) and spends the template and
  material, but neither the worn GeckoLib model nor the icon draws them; only the tooltip shows the trim. Any worn trinket refuses a dream expedition ("accessories", unchanged).
- Persistence: vanilla item stacks; Trinkets saves its slots. No new saved data. Save compatibility: additive.
- Disable behaviour: with `concordance.enabled=false` the recipes do not load and the blow does nothing; the items, slots
  and their Spell Power stay registered (Ember's invocations are refused then, so the power does nothing).

## Dependencies and assets

No new dependency. Framework use: **Trinkets Updated** (two slots given by data; the foci's callback modifiers; the
bangle read on the server), **Spell Power** (the fire attribute; its enchantment tag refused on the sets), **GeckoLib**
(the sets' armour renderer: Jugcraft's first GeckoLib armour), **Fabric API** (the damage and enchanting events),
**Modonomicon** (the codex entry).

**Provenance.** Every file below is from the owner's magic collection (`art/owner-library/originals/Magic`, supplied
8 October 2026, [MAGIC_ASSETS.md](../../art/owner-library/MAGIC_ASSETS.md)). The owner says they used other Minecraft
mods as a starting base and then "completely redid and remade all the stuff"
([PROVENANCE.md](../../art/owner-library/PROVENANCE.md)); this record repeats that declaration, which is not an
independent rights audit. The project uses the files under the owner's project-use authorization
([LICENSE_POLICY.md](../../LICENSE_POLICY.md), owner-supplied collections). The source folders' names (`ars_jymbaumental`,
`ars_jimbaux`, `curios`) are kept here as labels; nothing in the game uses them. The imported files carry no credits or
notices (PNG chunks, Ogg comments and the model's JSON were read), and `catalog/legacy-metadata.csv` has no row for them.
They are byte-for-byte copies under Jugcraft names (the model's JSON with its line ends made LF), written by
`tools/owner_art.py` from `tools/concordance_ember.py` `OWNER_FILES` and checked byte for byte by `check_mod_data`.
`python3 tools/owner_art.py --provenance` prints this table:

| Runtime file (under `assets/jugcraft`) | Owner's file | SHA-256 of the owner's file |
|---|---|---|
| `geckolib/models/armor/pyromancers.geo.json` | `originals/Magic/assets/ars_jymbaumental/geo/medium_armor_e.geo.json` | `64af3733c491f627be2f0c9db733a4fcb15d5d2f131ceb9df2562e846ff3c862` |
| `sounds/concordance/pyro_1.ogg` | `originals/Magic/assets/ars_jimbaux/sounds/pyro_1.ogg` | `084454e70b5fb7eed6b06a13ae00c437a8fb8142d43e0364a326c4e79c1a4b13` |
| `sounds/concordance/pyro_2.ogg` | `originals/Magic/assets/ars_jimbaux/sounds/pyro_2.ogg` | `f9ba821f42db8b68cdf9fe844ea88865927e487e55f0761714000b3beb3e5acb` |
| `sounds/concordance/pyro_3.ogg` | `originals/Magic/assets/ars_jimbaux/sounds/pyro_3.ogg` | `ebdfb5c1b15bfa406dd731e6e1177c5025994c856ecf52e1464a9b07eb824769` |
| `sounds/concordance/pyro_4.ogg` | `originals/Magic/assets/ars_jimbaux/sounds/pyro_4.ogg` | `72beff23321a75cdd5e36a813d292633ab09d2de17f8291fd00ba97be90dedbf` |
| `textures/armor/pyromancers.png` | `originals/Magic/assets/ars_jymbaumental/textures/armor/medium_armor_fire.png` | `79206b3ff67aab7418da0db5913f5c11a73cdade1ad04ebe487a879d05f63169` |
| `textures/armor/pyromaniacs.png` | `originals/Magic/assets/ars_jymbaumental/textures/armor/light_armor_fire.png` | `79206b3ff67aab7418da0db5913f5c11a73cdade1ad04ebe487a879d05f63169` |
| `textures/gui/sprites/container/slots/bracelet.png` | `originals/Magic/assets/curios/textures/slot/bangle_slot.png` | `feacac923ed1d401bb156632be60df8afd5e084d72c9575ea04ad264312762b2` |
| `textures/gui/sprites/container/slots/spell_focus.png` | `originals/Magic/assets/curios/textures/slot/an_focus_slot.png` | `26d6888eb48dbbdd0ab57d00bf5318f480ce97982849b9743ce44f7135ba4440` |
| `textures/item/fire_bangle.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_bangle.png` | `52f4ff0b6f348557e3887a9c083de397e163c074a3b322c0364e3d65c6a6f987` |
| `textures/item/fire_focus.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_focus.png` | `2be5aadceb1d2ef0ef5d1494be15827b3144bbf61cb402b3a978be36109e521d` |
| `textures/item/lesser_fire_focus.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/lesser_fire_focus.png` | `ca5cd0ed2f5fc6a30c13b88bd34b079b65bce6f8f784721b70f976bf813f6da4` |
| `textures/item/pyromancers_boots.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_boots.png` | `43b83951779e2fe72e66eedc50a5dddbc1fa7340cb2415278a725441fbdf7080` |
| `textures/item/pyromancers_hat.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_hat.png` | `cfa17fc8717ea415fc8bb7c802a8880e5f5e6b3a290f43b83094cd498a57d1ca` |
| `textures/item/pyromancers_leggings.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_leggings.png` | `f9f98a196c763443c13f3995463ba2e733bea870761c22f9b8a74d66ab820237` |
| `textures/item/pyromancers_robes.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_robes.png` | `3092b3092e05ccf6547fc083a2d513d953ef54ebccbc4af09287b1d97b21ae8b` |
| `textures/item/pyromaniacs_hood.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_hood.png` | `2ac52a3e61c3a082e746756dd4b71cdf97d7cbf4a30a84ca549b426edd2aff8c` |
| `textures/item/pyromaniacs_pants.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_pants.png` | `52d9e7f19ea94a9955dd73cd89f7e6dc6549c629329f3ff98fc5a68c3c813c9e` |
| `textures/item/pyromaniacs_shoes.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_shoes.png` | `06a9993cc122c51a2d66834262e4836ca8cb13d24189d709ff5191872527975d` |
| `textures/item/pyromaniacs_tunic.png` | `originals/Magic/assets/ars_jymbaumental/textures/item/fire_tunic.png` | `0c4eac28494a9797628595ba90461d9979d41ddae53529cab92dcdd6b33d3789` |

Also taken from the owner's files, as data rather than copies:
- the eleven item models (`models/item/<id>.json`), each the owner's plain generated model with its texture renamed,
  generated from the library file each run;
- the eleven display names, from `assets/ars_jymbaumental/lang/en_us.json`. Only these names are taken: the tooltips,
  codex text and recipes are Jugcraft's, and every other string in that file (guide pages, the owner's behaviour notes,
  supporter text) is left out;
- the two slots, ported from the owner's Curios data (`data/ars_jymbaumental/curios/slots/an_focus.json`, one slot
  named "Spell Focus" in `ars_jimbaux/lang/en_us.json`; `bracelet.json`, two slots) to Trinkets slots in the chest and
  hand groups. The foci get their own slot, as the owner made them, rather than sharing the Hearthstone's necklace;
- Hearthflare's sound list: the owner's `fire_family_2` event plays these four files (`assets/ars_jimbaux/sounds.json`).

Ids renamed: the owner's `fire_hat`, `fire_robes`, `fire_leggings` and `fire_boots` are `pyromancers_hat`,
`pyromancers_robes`, `pyromancers_leggings` and `pyromancers_boots` here, and `fire_hood`, `fire_tunic`, `fire_pants` and
`fire_shoes` are `pyromaniacs_hood`, `pyromaniacs_tunic`, `pyromaniacs_pants` and `pyromaniacs_shoes`, because the
owner's naming gives `fire_helmet` and `fire_leggings_heavy` to the heavy set and Jugcraft's ids cannot be renamed after
release. The display names are the owner's.

The light set's worn texture: at the owner's request ("use my light armor texture for the light set too"), it is the
owner's `light_armor_fire.png`, imported as `textures/armor/pyromaniacs.png`. That file is the medium sheet byte for byte,
and like the owner's air, earth and water light textures it is laid out for the medium model (measured: the medium model
draws 71% of its faces from opaque pixels on each of them, the owner's light model `light_armor_e.geo.json` 21%, since it
declares a 64x64 sheet). So the light set is worn on the medium model, where the texture fits; the light model is not
imported.

Part 1's art: the library has no invocation-style spell icons (its fire glyphs are component tiles: Ignite, Flare, Burst,
Conflagrate) and no fire ward, so the four drawn spell icons stay; the owner's 18x18 `hellfire.png` is their icon for a
different status, Magic Burn, kept for that later slice, so the drawn Smoulder icon stays; the owner's projectile sounds
(`fire_projectile_*`) are stereo, which Minecraft plays without position or falloff, so Cinderbolt keeps its drawn cue
until a slice that makes them mono.

## Verification

Run here (no game):
- `python3 tools/owner_art.py --check`: every imported owner file matches its source.
- `python3 tools/check_mod_data.py`: passes, with `check_ember_regalia` (the Java's numbers, ids and wiring; the
  foci's modifiers only through Trinkets; the blow through the effect boundary only; the Sunfire refusal; the
  renderer posing before hiding; each item's model, icon and name; Overworld recipes; the slots; the model's bones and
  UVs; the sounds; the drawn and imported files kept apart).
- `check_ember_regalia` was mutation-tested: seventeen injected faults (a Java power constant, modifiers on a trinket
  item, the renderer not posing the bones, the client hook not set, the blow igniting directly, Sunfire allowed, a
  Nether ingredient, a renamed armour bone, a drawn Hearthflare cue, a slot without the owner's icon, a renamed display
  name, a piece missing from its armour tag, the benchmark's ceiling, a light piece registered in the medium set, the
  light set's protection, its repair tag, a light piece missing from its armour tag) were each reported, as was an
  eighteenth added with the review fixes (the bangle's blow counting beyond the weapon's reach), and a changed
  byte in an imported icon was reported by `tools/owner_art.py --check`; the clean tree passes.
- `python3 scripts/check_repository.py` and `python3 tools/check_icon_maps.py` pass (the icon check's eleven warnings are
  older and about other items).
- The provenance point below was re-checked from the files: 166 differing pixels of 8,192; the top half equal to the
  medium sheet; the Blockbench project's embedded `firenando_magma.png` pixel-identical to the library's.

Game tests added (they run in CI's Build workflow; results are recorded below once it has run):
- `ConcordanceEmberGearGameTests` (server): the eleven items, slots and both sets' numbers, tags, sets and model; the
  foci's Trinkets modifiers, asked through the wearer's own slots as Trinkets asks; the regalia raising Cinderbolt to 6,
  and three pieces adding nothing; the bangle's blow and its six refusals (before Hearthbinding, not a melee hit, a
  claimed creature, beyond the weapon's reach, from the saddle, bangle off); Sunfire
  refused on both sets and Protection accepted.
- `ConcordanceEmberGearClientGameTests` (client): GeckoLib's renderer for all eight pieces, one for each set, each
  wearing the owner's model in its set's own texture; the model with every bone, both sets' textures; screenshots of the light and medium sets on stands side by side, the medium set enchanted,
  on a zombie and on a small stand, in four views; the player standing and sneaking; the real Trinkets slot on the
  ticking player (Focus of Fire and the set: 6; the lesser focus: 4); the eleven icons.
- `ConcordanceBaselineGameTests`: the new character through the acceptance rules.
- `Step11Harness`, run here over the generated data: the benchmark with the new character and helpful statuses in the
  model; acceptance holds and two runs give the same tables.

CI (the Build workflow, dispatched on this branch):
- Run 37977238174 (commit `f52a86b0`) did not compile the server test: `Items.WHITE_WOOL` does not exist in 26.3. The
  test now looks the wool and the netherite helmet up by id.
- Run 37980438983 (commit `043a810a`, with the light set) compiled. Both server jobs ran 1177 game tests, and 1176
  passed, the five `ConcordanceEmberGearGameTests` among them. The one failure, in both, was
  `ConcordanceBaselineGameTests.baselinesHoldOnTheLoadedRules`: it still counted seven kits. The table it logged
  showed the model gap described under Balance (the hearthbinder losing three encounters). All three client shards
  passed, `ConcordanceEmberGearClientGameTests` among them (shard 1: 29.5 s, its twelve screenshots taken). Shard 1
  took 28 min 54 s of its job's 30: Guns, Styx and Armor Tiers, which `tools/select_client_tests.py` had put at 13 to
  28 s from their length, took 261, 221 and 123 s and had all landed in that job. They and Arms and ArmsMotion now have
  measured weights, so a full run shares them out (about 24 minutes a job by the estimates).
- Run 37986172795 (commit `2da07f9c`: eight kits, helpful statuses in the model, the weights): **every job passed**.
  Both server jobs: **"All 1177 required tests passed"**, and the baseline tables the server logged are identical, row
  for row, to `Step11Harness`'s over the generated data. The three client shards passed in 27:01, 25:14 and 21:20 (the
  slowest had been 28:54); the slowest is still within 3 minutes of its job's limit, so the client tests' total time is
  a watch item.
- Run 37989465048 (commit `0a1eb90e`, with the review fixes: the bangle's reach, the renderer checks, the Trinkets slot
  access, the centred row): **every job passed**. Both server jobs: **"All 1177 required tests passed"**, the bangle's
  new beyond-reach case and the slot-access focus tests among them. The three client shards passed, the per-set
  renderer check among them, in 28:40, 26:28 and 20:19; the slowest job took 29:32 of its 30 minutes, so the client
  tests now run in four jobs (see `tools/select_client_tests.py` and `docs/TESTING.md`).
- Run 37993306024 (commit `1043421e`, the four client jobs): **every job passed**. The four client shards took 21:51,
  17:39, 19:22 and 13:13; the slowest job 22:41 of its 30 minutes.

Not yet run: a person looking at the screenshots (the worn model, the sleeves following a zombie's raised arms and a
sneaking player, the glint, the small stand showing nothing); the Spell Focus and Bracelet slot icons in the Trinkets
screen (no client test opens it); listening to the owner's recordings in game; a two-client server.

## World and event applicability

No worldgen, creatures, loot or seasons.

## Rollout and open questions

- New stable ids: items `jugcraft:lesser_fire_focus`, `fire_focus`, `fire_bangle`, `pyromaniacs_hood`, `pyromaniacs_tunic`,
  `pyromaniacs_pants`, `pyromaniacs_shoes`, `pyromancers_hat`, `pyromancers_robes`, `pyromancers_leggings`,
  `pyromancers_boots`; equipment assets `jugcraft:pyromaniacs` and `jugcraft:pyromancers`; GeckoLib model
  `jugcraft:armor/pyromancers`; tags `jugcraft:repairs_pyromaniacs_gear` and `jugcraft:repairs_pyromancers_gear`; Trinkets
  slots `chest/spell_focus` and
  `hand/bracelet` (`data/trinkets/entities/jugcraft_ember.json`); codex entry `hearth/regalia`; sound files
  `jugcraft:concordance/pyro_1` to `pyro_4`. Save compatibility: additive.
- **Deferred, each for a concrete reason:**
  - **The owner's light model** (`light_armor_e.geo.json`): no light-layout fire texture exists for it (see the light
    set's texture above). A light-layout fire sheet from the owner would let the light set wear its own shape.
  - **Netherguard's (heavy) set:** one file-specific point (raised once, per PROVENANCE.md): rows 64-127 of
    `textures/armor/heavy_armor_fire.png` differ in 166 of 8,192 pixels from `textures/entity/firenando_magma.png`,
    and that PNG is pixel-identical to a texture embedded in `geo/fire_golem.bbmodel`, a Blockbench project dated
    25 May 2022 whose embedded paths name an "Ars-Elemental" source tree. Whether those specific files are the owner's
    rework is the owner's to settle; nothing else in the library is held back for it. When settled, the planned repair is
    the helm's pivot (y 20 to 24 in the runtime copy, so it turns with the head).
  - **Caster Tome of Fire:** a tome that casts one fixed invocation needs a new instrument gate, and the owner's tome
    stores mana, which the Focus rules forbid.
  - **The foci's cheaper invocations and the sets' fire ward:** the cost floor leaves almost no room (mastered
    Hearthspark is at it), and Fabric API has no damage-reduction event (it would need a mixin).
  - **Worn foci and bangles** are not drawn on the body (the library has no worn model for them).
  - **Armour trims on the sets** are kept on the item but not drawn (see Failure behaviour): GeckoLib's armour renderer
    has no trim layer, and the owner's model has no trim texture to draw.
- Later slices (the owner's files mapped, not imported): the Flarecannon familiar and its charm, the fire turret, fire
  relay, Magmatic Current Elevator, the Magic Burn status, the fire mage, and the fire slime and mermaid skins.
