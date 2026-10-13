# Arcane Concordance: Wayfaring (the owner's belt, boots and charms; trinkets parts 1, 1b and 1c)

Status: parts 1 and 1b merged to main ([#295](https://github.com/jimbozoomer-byte/jugcraft/pull/295), integrated by
[#277](https://github.com/jimbozoomer-byte/jugcraft/pull/277)); part 1c implemented on branch
`claude/awesome-davinci-iwv3b9`. See Verification for what has run.
Proposal issue: none; the owner asked for their supplied magic content to be built into Jugcraft (9 October 2026), named
this slice ("the trinkets (Relics/Reliquary)") as the one after the fire school, and authorized it at the top of
[CLAUDE.md](../../CLAUDE.md).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Practitioner stage, beside [Relic Lore](arcane-concordance-relics.md) (roadmap step 20),
which these items need understood before they can be put on.
Primary specialty and supported player role: anyone who travels, explores and fights away from home; no school.

Builds on [relics and shrines](arcane-concordance-relics.md) (Relic Lore and the Trinkets slots), the [shared effect
boundary](arcane-concordance-composition.md) and [Ember part 2](arcane-concordance-ember-regalia.md) (the owner's art
imported as supplied, slots ported to Trinkets). Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

Once **Relic Lore** is understood, eight worn things can be put on, in three Trinkets slots: Trinkets' own **Belt** slot,
a new **Charm** slot (one; a Leather Belt gives a second) and a new **Feet** slot (two). The names, icons and kinds of
effect are the owner's; the numbers are Jugcraft's.

| Item | Worn in | What it does |
|---|---|---|
| **Leather Belt** | Belt | One more Charm slot. It cannot be taken off while that slot holds a charm. |
| **Angelic Feather** | Charm | A fall's harm comes from your food instead of your health (half a food point for each point of harm, before armour) when your food bar can pay for it; and you jump a little higher (about 1.42 blocks instead of 1.25, still short of a fence). |
| **Kraken Shell** | Charm | The same for drowning. |
| **Infernal Claws** | Charm | The same for fire, burning and hot floors (magma blocks); not lava. |
| **Angelheart Vial** | Charm | A blow that would kill you leaves you on 2 hearts with Regeneration II for 5 seconds. The vial is used up. |
| **Phoenix Down** | Charm | A blow that would kill you leaves you at full health with Regeneration II and Fire Resistance for 10 seconds, and the down becomes an Angelic Feather where it was worn. Until then it is a feather as well (its fall and jump). |
| **Amphibian Boot** | Feet | You swim faster (water movement +0.5, half of Depth Strider's most), and your air lasts about twice as long. |
| **Ice Breaker** | Feet | +0.1 knockback resistance. A fall that hurts you sends a wave through the ground: the nearest hostile creatures (up to 12) within 3 blocks, a block more for each 4 points of harm (at most 6), take 2 damage, are thrown back and are slowed for 2 seconds (the Ender Dragon, Wither, Warden and Elder Guardian take the harm but are neither thrown nor slowed). |

- Two of a kind never add up: a second feather, boot or Ice Breaker adds nothing, and a feather and a Phoenix Down jump as
  one. A second vial waits its turn for the next death; a vial worn beside a Phoenix Down answers first.
- A death save does not answer the void or `/kill` (harm that passes through invulnerability), a totem held in either
  hand (the totem answers, as vanilla's), or a death in a dream (the dream ends as it would).
- A codex entry, **Belts, Charms and Boots** (Relics category), shows every recipe and says all of this.
- **Worn, the Leather Belt and the Amphibian Boot show on the body** (part 1b), as the owner drew them: a brown leather
  strap round the waist with a gold buckle in front, and on each foot a green boot with a white cuff, a grey toe cap and
  a fin on the heel. Other players should see them too (not yet tried with two clients), and so does the figure in the
  inventory; you do not, in first person. The charms are not drawn, nor is the Ice Breaker (the owner drew no worn sheet
  for it).
- **Armour worn over them hides them** (part 1c, as the owner asked): a chestplate or leggings hide the belt, and boots
  hide the boots. An elytra hides neither, and leggings leave the boots showing over them, as vanilla's boots are drawn
  over leggings. **Show my worn trinkets**, a switch in the Concordance settings (from Mod Menu, or the Concordance
  settings key in Controls, unbound at first), hides your own belt and boots, and everyone who sees you sees your choice,
  as with vanilla's cape and skin layers; you see theirs. It is on at first.
- The kinetic belt that links two pulleys (`jugcraft:belt`, unchanged) is now called the **Drive Belt** in English, so
  the two belts are told apart.

## How it works

- Data (`tools/concordance_trinkets.py`): the eight items, their recipes (seven shaped, and the owner's shapeless Phoenix
  Down), the two new slots (`data/trinkets/slots/legs/charm.json` and `feet/boots.json`: the owner's icons, no cosmetic
  copies), the item tags for all three slots, `data/trinkets/entities/jugcraft_wayfaring.json` (players get all three),
  the codex entry and the slot names. `tools/concordance_equivalence.py` excludes the eight from the Assayer's Scale;
  `tools/concordance_progression.py` gains leather and the golden apple as Overworld sources.
- Java (`concordance/trinket`):
  - `WornTrinketItem` is a Trinkets callback. Its attributes are Trinkets modifiers added while it is worn where Trinkets
    applies effects, each named `jugcraft:wayfaring/<kind>/<attribute>`, never by slot, so two of a kind give one
    modifier (Trinkets replaces a modifier with the same name and keeps it while any copy is worn). The Phoenix Down's
    kind is the feather's. `canEquip` asks whether the wearer understands Relic Lore; it runs on the server and, for
    one's own screen, on the client, which holds its own player's research. The belt's `canUnequip` refuses while a
    Charm slot beyond the first holds something, then defers to Trinkets' default (Curse of Binding).
  - `Wayfaring` registers the items, the Charm slot's count attribute (`SlotAttributes.createAttributeForSlot`; the belt's
    modifier is +1 on it) and three listeners:
    - `ALLOW_DAMAGE`: a worn feather or down (plain fall damage), shell (drowning) or claws (`in_fire`, `on_fire`,
      `campfire`, `hot_floor`) refuses the harm if Relic Lore is understood and the food bar can pay `2 × harm`
      exhaustion: whole food points come straight off the bar (vanilla caps exhaustion at 40, so a big blow could not
      be charged as exhaustion), the rest as exhaustion. A blow the charm refuses never starts vanilla's hurt cooldown
      (that comes later in `hurtServer`), and fire and hot floors try to hurt every tick, so the charm keeps its own:
      within 10 ticks of a blow it took, a blow costs only what it is bigger by (`Wayfaring.absorb`).
    - `ALLOW_DEATH`, in its own phase ordered before the default one: the death saves. Dreaming's listener runs in the
      default phase and ends an open dream as a death; only before it can a save see the dream and decline.
    - `AFTER_DAMAGE`: after a plain fall (`minecraft:fall`, not an ender pearl's landing) that hurt the wearer, on foot,
      the Ice Breaker's wave.
  - Everything the slice does to a creature goes through `ConcordanceEffects.apply` as an item's effect: the wave's harm,
    throw and Slowness, at `Enemy` creatures only (the boundary lets anyone fight them, whatever claims say; its
    tolerance tags keep the Ender Dragon, Wither, Warden and Elder Guardian from being thrown or slowed, though the
    harm lands), and the saves' statuses on the wearer. What shows is the shared signs (`Signs.show`).
- Client: Trinkets draws the slots with the owner's icons (GUI sprites under `textures/gui/sprites/container/slots/`);
  the items are the owner's plain generated models. The slice's client Java is all part 1c's: the render element,
  `WayfaringClient` (which registers it, called from `ConcordanceClient`, and sends the player's choice) and the Show my
  worn trinkets setting in `ConcordanceClientOptions` and `ConcordanceSettingsScreen` (below).
- Part 1b, drawn on the body by Trinkets' data-driven renderer:
  - The owner drew a worn sheet for the belt and for the boot (`jymbelics/textures/models/items/`), each a box-UV net
    (vanilla's `ModelPart` cube layout) with no geometry. `tools/concordance_trinkets.py` `WORN` fits boxes to them: the
    belt a 9 × 2 × 5 strap round the torso's bottom rows and a 4 × 3 × 1 buckle on its front; the boot a
    6 × 7 × 6 boot a pixel round the foot, a 6 × 3 × 2 toe cap, a 6 × 1 × 6 cuff (an open ring: the sheet leaves its top
    and bottom empty) and a 3 × 5 fin on the heel, its fronds pointing back, drawn as its two sides each lifted 0.05 px
    along its own normal (`DecorDraw.TWO_SIDED_LIFT`: 26.3's cutout types may not cull, and a reversed twin on one
    plane would fight; the two halves are one silhouette, so the nearer hides the other). Between them the nets
    read every opaque texel of both sheets; the belt sheet's one faint texel (alpha 38, at 28, 6) lies in no net and is
    never drawn.
  - The generator writes a block model for each (`models/item/leather_belt_worn.json`,
    `amphibian_boot_worn_left.json` and `amphibian_boot_worn_right.json`), every face reading its net's rectangle of
    the sheet, and a render definition for each item (`assets/jugcraft/trinkets/<item>.json`) whose `model` elements
    attach the belt to the player model's `body` at the bottom of the torso (offset `[0, -1, 0]`) and the boot to each
    leg at its sole. One block-model unit is one pixel of the player model, and Trinkets turns south to the wearer's
    front and east to their left.
  - The sheets are read from the items atlas (`textures/item/<item>_worn.png`, imported as supplied); Trinkets bakes its
    models from the block and item atlases and adds no atlas of its own.
  - Each box stands 0.15 px further out than fitted (vanilla's `CubeDeformation`; `armor_models.SKIN_GAP`), because the
    fitted belt lies exactly where vanilla leggings draw the body (0.5 px out) and the fitted boot where vanilla boots
    draw the legs (1.0 px out): sharing those planes, they would flicker. A box set on another's face (the buckle, toe,
    cuff and fin) moves out with that face instead, so no two faces of a model facing the same way share a plane (the
    generator's `model_writer.finish_elements` leaves them as fitted); a seated box's inner face lies back to back on
    its seat, where it never shows (the buckle's back inside the closed buckle; the toe's back and the cuff's bottom are
    clear on the sheet). The toe's top stays where the owner put it, on the line where
    the boot's front ends, so it hides none of the row above. The two boots overlap between the legs, as vanilla's do,
    so the left one stands a further 0.1 px out (its toe's top 0.1 px lower) and is drawn in front there.
  - `check_wayfaring_worn` holds the models to the rules `armor_models` keeps 3D armour to, computed from the faces: a
    face over the body stands 0.15 px off the skin and its outer layer (so the belt's underside is 0.15 px inside the
    torso) and 0.1 px off vanilla armour's shells (0.5 and 1.0 px out), and two models' faces stay 0.1 px apart where
    they overlap standing (the two boots between the legs). The soles are the one exception, kept only 0.1 px off each:
    the right 0.15 px below the foot (between the skin and the pants layer), the left 0.35 px (past it), so as little of
    the owner's bottom row is buried in the ground as those gaps allow (0.15 px off would bury a third to a half of it).
    It also checks that Trinkets anchors each model where the generator places it (the part's bottom middle), and that
    no other render definition, in any namespace or folder and by id or tag, draws one of the slice's items.
  - The sheet is the left boot: the owner's icon shows it from its outer side, toe to the left, with the fin's bright
    half outward, which the sheet puts on the boot's left side. The right boot is its mirror image (vanilla's `mirror`),
    so both show the bright half outward.
  - Faces the owner left empty (the toe's back, the cuff's top and bottom) are drawn as vanilla draws a whole box: they
    show nothing.
- Part 1c, hidden under armour and by each wearer's own choice:
  - `client/trinket/UnlessCoveredTrinketElement` is a Trinkets render element of Jugcraft's own, `jugcraft:unless_covered`,
    which `WayfaringClient` adds to Trinkets' element types (`TrinketRenderElements.ID_MAPPER`) when the client starts,
    before any resources load. It holds other elements (`then`) and draws them only while the wearer has not hidden them
    and none of the armour slots it names (`armour`) holds armour drawn on the body. It passes its elements' models on to
    Trinkets, which otherwise asks only the top-level element, so they are baked.
  - Each render definition is now one such element round its `model` elements, written by the generator with the slots
    from `WORN_COVERED_BY`: the chest and legs for the belt (a chestplate alone would leave the buckle standing out
    through it), the feet for the boots. Leggings do not hide the boots, which are worn over them, as vanilla draws its
    boots over leggings.
  - Trinkets runs a definition's elements each time it takes a wearer's render state: each frame, for every player drawn
    and for the inventory's figure, with the wearer itself. The element reads the armour from the wearer
    (`getItemBySlot`), not from the render state, which is given its armour only after Trinkets has run. Armour drawn on
    the body there is what vanilla's armour layer asks for: an item equippable in that slot that names an equipment asset
    (Jugcraft's 3D sets and Ember's GeckoLib sets name one too), except anything with the glider component (an elytra
    is drawn as wings, off the waist; a chestplate or boots given that component by a command or data pack is still
    drawn on the body but does not hide them).
  - The setting is `concordance.worn_trinkets` in `config/jugcraft-client.properties` (`true` at first), turned in the
    Concordance settings screen (Cloth Config) with the others there. The client sends it to the server
    (`WornDisplayPayload`, `jugcraft:worn_trinkets_shown`, a single true or false) when it joins a world and each time the
    settings are saved, and only to a server that takes it.
  - The server (`concordance/trinket/WornDisplay`) takes a choice only for the player who sent it. One that changes
    nothing is dropped. A change is made at once if the player's last was at least 10 ticks ago (the Concordance's
    `RateGate`); one that comes sooner waits for its tick, and a newer choice replaces it, so the last one sent always
    lands while a client sending as fast as it can makes no more traffic than a player using the switch. It keeps the
    choice on the player as a Fabric data attachment, `jugcraft:worn_trinkets_hidden` (there only while hidden), which
    Fabric sends to that player and to every client that can see them, and to any that comes to see them later. It is
    kept through death (`copyOnDeath`) and not saved: the client sends the choice again whenever it joins.
  - The element reads that attachment on the wearer, as it reads their armour, each time Trinkets draws them, so armour
    put on or off, or a wearer's new choice arriving, shows on the next frame. A wearer whose choice has not arrived (just
    after they join, until their client's message lands) shows their belt and boots.
  - What that test counts as covering though it hardly does (the owner keeps it so, 10 October 2026: "They can hide them
    its ok"): the Rocket Pack and the Scuba Tank (chest; straps, with
    most of the waist bare) hide the belt; so, when nothing else covers the waist, do six of Jugcraft's 3D pieces, drawn
    in 3D only, that stop short of the strap (the bronze, steel, Reforged White Diamond and Sunset Gem chestplates end at
    or above it, and the Sentinel and Banana leggings have no waist piece); and Wool Socks hide the boots (the socks
    cover the foot as boots do, but are worn under boots and drawn inside them). Twenty-one other 3D chestplates and
    leggings cover only part of the strap (from about 1 to 2 of its 2.3 px) and hide all of it, and Ember's GeckoLib
    leggings (legs only) reach a little way up it. Armour from other mods drawn only by a Fabric armour renderer, with
    no equipment asset, does not hide them. Trinkets' own `minecraft:equipment_replace` render element (cosmetic armour
    from other Trinkets content) and client mods that hide armour change what is drawn in a slot without changing its
    stack; the belt and boots follow the stack, so they show through cosmetic armour over an empty slot and stay
    hidden where an override blanks real armour.
- Nothing ticks but `WornDisplay`, which looks each tick at choices waiting for their tick and does nothing while none
  waits: the attributes are Trinkets modifiers and the rest answers damage events.

## Connections

- Tier and unlock: Practitioner, after Relic Lore understood (examine relic-like things, then study one at a
  Lampwright's Bench). Before that the items can be crafted but not put on.
- Input producer: Overworld leather, iron, gold, amethyst, phantom membranes, feathers, prismarine shards, an ink sac, a
  nautilus shell, magma blocks, golden apples, glass bottles, dried kelp and flint (crafting table).
- Output consumer: the wearer (falls, drowning, fire, death, swimming, air, knockback); hostile creatures, through the
  shared effect boundary (the wave); the Charm slot (the belt).
- Costs: materials only. A vial is used up by its save (a golden apple each); a Phoenix Down costs three vials and
  becomes a feather. The charms' protection is paid for in food. No Focus, charge or energy.
- Technology connection: none needed. Magic connection: Relic Lore gates every item; the wave and the saves' statuses are
  Concordance item effects.
- Reachable entry path: every recipe input has an Overworld source in the progression graph (`check_wayfaring` checks
  each); Relic Lore's own entry path is unchanged. No research, practice or stage requires these items, so no unlock can
  become circular.
- Solo, trade and cooperative routes: entirely solo; the items are ordinary stacks.

## Balance

- **Units.** Harm in health points; exhaustion as vanilla counts it (4 is a point of saturation or food); attributes as
  vanilla's added values; ticks.
- **The charms' protection is paid in food**, at half a food point per point of harm before armour, and only for a blow
  the bar can pay for in full (otherwise it lands, costing nothing). A 10-block fall (7 harm) costs 3.5 points;
  standing in fire about a point a second (one blow every half second, as vanilla's hurt cooldown lets them land);
  drowning about one a second. On Peaceful the food bar refills by itself, so there the charms cost nothing. The Free Runners and the charged Exosuit boots already prevent fall harm outright, from an armour slot.
- **Death saves against the totem.** A Totem of Undying cannot be crafted; the vial can, so it does less: 2 hearts and 5
  seconds of Regeneration II (about 2 more hearts), against the totem's 1 health with 45 seconds of Regeneration II, 5 of
  Absorption II and 40 of Fire Resistance. Each costs a golden apple (8 gold). The Phoenix Down (3 vials) rises at full
  health. One save per death, and a held totem keeps its turn.
- **The wave** is 2 damage, a 0.6-block-a-tick throw and 2 seconds of Slowness I to at most 12 hostile creatures, and only
  after a fall that hurt the wearer (the feather's absorbed falls make none). A Hearthbinder's Cinderbolt deals 3.
- **Attributes.** Jump +0.03 (below a fence), water movement +0.5 (the attribute's most is 1, Depth Strider III's), oxygen
  bonus +1 (Respiration I's), knockback resistance +0.1 (one netherite piece's).
- **No conversion loops:** no recipe makes a material; the Phoenix Down's feather was one of its inputs.

## Multiplayer and persistence

- Server authority: Trinkets applies the modifiers on the server; the absorption, saves and wave are decided on the server
  from damage events. The client's `canEquip` reads only its own synced research, as the server does. Part 1c's one
  packet is a player's own choice whether their belt and boots show: the server takes it only for its sender, at most
  one change every 10 ticks, and it decides nothing but that drawing.
- Part 1b is drawn on each client from the worn stacks Trinkets already sends to everyone tracking the wearer; Jugcraft
  sends nothing. With Trinkets' cosmetic slots on (a server setting, off by default), a cosmetic Leather Belt is drawn
  in place of the worn one, as Trinkets draws cosmetics; the Feet slot has no cosmetic copies. Trinkets sends a wearer's
  cosmetic stacks to other players only when they change (a player coming into view is sent the worn stacks only), so
  until then they see the worn belt, or none. With Trinkets' equipment hiding on (also a server setting, off by
  default), a wearer can also hide them that way, slot by slot.
- Part 1c is decided on each client from the armour that client already sees on the wearer and from the wearer's own
  choice, which the server sends to everyone who sees them (and to anyone who comes to see them later), so every screen
  shows a wearer the same way. A player who hides theirs sees them hidden too (in third person and on the inventory's
  figure). The choice lasts through death and through changing dimension, and is sent again on every join; until it
  arrives (a moment after joining) the belt and boots show.
- Failure behaviour:
  - before Relic Lore: cannot be put on (an item put on by other means, such as a command, gives its attributes but
    answers no damage event);
  - held, loose or in a container: nothing. In a cosmetic belt slot (if a server enables Trinkets' cosmetic slots): no
    Charm slot, and it stays with its wearer through death (Trinkets would otherwise drop a cosmetic stack and keep it
    too); it can always be taken off;
  - the charms against a blow the food bar cannot pay for, or with an empty bar: the harm lands; the claws against lava:
    the harm lands;
  - the saves against the void, `/kill`, a held totem or a death in a dream: no save, nothing spent;
  - the wave after an ender pearl, a fall that did no harm, or from the saddle; on a creature that is not hostile (a
    villager, an animal, another player): nothing; on the Ender Dragon, Wither, Warden or Elder Guardian: the harm only;
  - the belt with its added charm: stays on; were its slot to go another way (a command), Trinkets drops the charm at
    the wearer's feet;
  - the worn trinkets choice: a change within 10 ticks of the last waits for its tick, and only the newest lands; one
    sent for nothing new is dropped; a choice waiting when its player leaves is forgotten (their client sends its setting
    again on joining); a client on a server without Jugcraft sends nothing.
- Persistence: vanilla item stacks; Trinkets saves its slots and the belt's slot-count modifier (as a persistent
  modifier named `jugcraft:wayfaring/leather_belt/slot_count/legs/charm`, which must never change: a renamed one would
  leave a Charm slot behind). No new saved data: part 1c's choice is kept on the player while they play and sent again
  on each join, never saved. Save compatibility: additive.
- Disable behaviour: with `concordance.enabled=false` the recipes do not load and nothing answers the damage events; the
  items, slots and attributes stay registered.
- Dreams: any worn trinket still refuses a dream ("accessories", unchanged). A pre-existing gap, not changed here:
  `Dreaming.end` gives back only the main inventory, so a trinket put on during a dream leaves the dream with the dreamer.

## Dependencies and assets

No new dependency. Framework use: **Trinkets Updated** (three slots given by data, one defined by Trinkets; the
callback modifiers, including a slot-count attribute; `canEquip`/`canUnequip`; part 1b: its data-driven renderer, render
definitions under `assets/jugcraft/trinkets/` with `model` elements; part 1c: a render element type of Jugcraft's own,
added to its element registry), **Fabric API** (the three damage events and an event phase; part 1c: a payload from
the client and a data attachment Fabric sends to everyone who sees the wearer), **Modonomicon** (the codex entry),
**Cloth Config** (part 1c: the setting's switch, on the existing Concordance settings screen).

**Provenance.** The files below are from the owner's library (`art/owner-library/originals`): the magic collection
(supplied 8 October 2026, [MAGIC_ASSETS.md](../../art/owner-library/MAGIC_ASSETS.md); folders `jymbelics` and
`jymbaquary`) and, for the charm slot icon, the Blocks folder's `Trinket Type Mod`. The owner says they used other
Minecraft mods as a starting base and then "completely redid and remade all the stuff"
([PROVENANCE.md](../../art/owner-library/PROVENANCE.md)); the base mods here are, by Jugcraft's reading of the folders
and the owner's name for this slice ("Relics/Reliquary"), a relics mod and a reliquary mod. This record repeats that
declaration, which is not an independent rights audit. The project uses the files under the owner's project-use authorization
([LICENSE_POLICY.md](../../LICENSE_POLICY.md), owner-supplied collections). The source folders' names are kept here as
labels; nothing in the game uses them. Read for notices: the five `jymbaquary` icons carry Photoshop 23.1 XMP packets
(created 23 January 2022), the Leather Belt and Ice Breaker strips Photoshop 22.1 packets (October 2021 and April 2022),
the Amphibian Boot and both slot icons only colour chunks; the Leather Belt's worn sheet carries nothing beyond its
image data, and the Amphibian Boot's worn sheet an empty `eXIf` chunk (a TIFF header with no entries); none names an
author or rights, and `catalog/legacy-metadata.csv` has no row for them. They are byte-for-byte copies under Jugcraft names (an animation
sidecar with its line ends made LF), written by `tools/owner_art.py` from `tools/concordance_trinkets.py` `OWNER_FILES`
and `OWNER_BLOCKS_FILES` and checked byte for byte by `check_mod_data`. `python3 tools/owner_art.py --provenance trinkets`
prints this table:

| Runtime file (under `assets/jugcraft`) | Owner's file | SHA-256 of the owner's file |
|---|---|---|
| `textures/gui/sprites/container/slots/feet.png` | `originals/Magic/assets/jymbelics/textures/slot/empty_feet_slot.png` | `f15fb69d3d0fd147f48457560eb5f8bf58529dde17318a5ac3e3f509fbc0c551` |
| `textures/item/amphibian_boot.png` | `originals/Magic/assets/jymbelics/textures/item/amphibian_boot.png` | `0bb844a015db5be3274df912fdbe3bb6d5d3a017611f1e05ee186f85301b2b40` |
| `textures/item/amphibian_boot.png.mcmeta` | `originals/Magic/assets/jymbelics/textures/item/amphibian_boot.png.mcmeta` | `39092f18e788de94796dbcf85e8ccf5a2b22ada0ba7c0ea3d902b6b5d1f1618b` |
| `textures/item/amphibian_boot_worn.png` | `originals/Magic/assets/jymbelics/textures/models/items/amphibian_boot.png` | `b49a953815d18cd271bab28042f7eba1197542edf4fa7f6f330bb7b758bb3808` |
| `textures/item/angelheart_vial.png` | `originals/Magic/assets/jymbaquary/textures/item/angelheart_vial.png` | `d8bfd161cf04c3c17b52206293773d6cce3cbe2638333f3e1698b22c8eba1b78` |
| `textures/item/angelic_feather.png` | `originals/Magic/assets/jymbaquary/textures/item/angelic_feather.png` | `3fb19d8705af7c911642b814290ebf20cb8b1faa79cc4286b477864a75f78827` |
| `textures/item/ice_breaker.png` | `originals/Magic/assets/jymbelics/textures/item/ice_breaker.png` | `099c1a26b62e134470276ded1fb1e20a68eabd4cf2aaf434979170e4cd8c8f0c` |
| `textures/item/ice_breaker.png.mcmeta` | `originals/Magic/assets/jymbelics/textures/item/ice_breaker.png.mcmeta` | `575cb57f14a7855ae586fd5a4eeec790435ece5d322f27d2b2f5a8c114b5e02e` |
| `textures/item/infernal_claws.png` | `originals/Magic/assets/jymbaquary/textures/item/infernal_claws.png` | `4aba40a2562033221254dcb507a3021b1fd475d07ad07c2b1ac059870b79c7d2` |
| `textures/item/kraken_shell.png` | `originals/Magic/assets/jymbaquary/textures/item/kraken_shell.png` | `02b8741b3f6a0ea5d8a0096a0250074158a61cacee41b68ae5bb11abefd68ede` |
| `textures/item/leather_belt.png` | `originals/Magic/assets/jymbelics/textures/item/leather_belt.png` | `b0c028bbb6f45e6f0e8493abec416b12d67960d29e83871d5d3d2d7026febaf2` |
| `textures/item/leather_belt.png.mcmeta` | `originals/Magic/assets/jymbelics/textures/item/leather_belt.png.mcmeta` | `f14cc62fbcf862476d19facd33cb4fa569616b597497eb8c5b110028edd0521c` |
| `textures/item/leather_belt_worn.png` | `originals/Magic/assets/jymbelics/textures/models/items/leather_belt.png` | `c4ecad15566e4bf996d74f0e9edf3c2a132734a6a27015b4a056d28e058704f3` |
| `textures/item/phoenix_down.png` | `originals/Magic/assets/jymbaquary/textures/item/phoenix_down.png` | `c35cf03feb47ee12c3b26ce7aa5c435a5f98be38bd648b2fb17037afd7d3f4c2` |
| `textures/gui/sprites/container/slots/charm.png` | `originals/Blocks/Trinket Type Mod/slot/empty_charm_slot.png` | `9862b3a181dff7ae5cb791bf093239613a7cfe9e30d729c5ec9ac80185a9fd72` |

Also taken from the owner's files, as data rather than copies:
- the eight item models (`models/item/<id>.json`), each the owner's plain generated model with its texture renamed
  (the `jymbelics` ones write their parent as `item/generated` and carry an empty `overrides` list, both dropped);
- the eight display names, from `jymbelics/lang/en_us.json` and `jymbaquary/lang/en_us.json`. Only the names are taken:
  the tooltips, codex text and numbers are Jugcraft's, and the rest of those files (relic levelling and experience text,
  guide pages, other items) is left out;
- the kind of effect each item has, from the owner's item descriptions (a fall's, drowning's and fire's harm taken for
  hunger; a vial that saves once and breaks; a down that saves at full health and reverts to a feather, keeping the
  feather's effects; swimming and breath; knockback resistance and a landing shockwave; a belt adding charm slots);
- the Feet slot (two, named "Feet": `data/jymbelics/curios/slots/feet.json`) and the charm slot (one, more from belts),
  ported to Trinkets slots in the feet and legs groups;
- the boxes the belt and boot are drawn with (part 1b), fitted to the nets of the owner's worn sheets, which came with
  no geometry; the changes made for the game, none to a texel: the boxes stand 0.15 px further out than fitted (0.25 px
  for the left boot), the toe's top stays at its fitted height (the left's 0.1 px lower), the soles lie 0.15 and 0.35 px
  below the feet, the right boot is drawn mirrored, and each part's model is anchored where Trinkets attaches it (How it
  works, above);
- the Phoenix Down recipe (`data/jymbaquary/recipe/phoenix_down.json`: three vials and a feather, shapeless), its ids
  renamed. The owner's other recipes use the base mod's own ingredients (mob drops and essences it adds), so Overworld
  recipes are used instead.

Not imported, and why: the owner's chest-loot additions for the feather and vial (progression never relies on loot), the
other worn sheets beside the belt's and boot's in `jymbelics/textures/models/items/` (for items this slice does not
have), the other 161 items of the two folders (later parts, below).

## Verification

Local (this branch, before CI):
- `python3 tools/check_mod_data.py`: PASS, with the new `check_wayfaring`. Mutation test: 32 deliberate breakages, each
  caught (among them a changed number, the save moved to the default phase or stripped of its dream or void guard, the
  claws taking lava, the Phoenix Down named apart from the feather, a modifier named by slot, an open `canEquip` or belt,
  a cosmetic charm slot, Trinkets' belt slot redefined, a Nether ingredient, an unconditioned recipe, a renamed item, the
  two belts sharing a name, the charm's cooldown or payment check removed, and a cosmetic belt locked on or dropped);
  every file restored after.
- `python3 tools/owner_art.py --check` (every imported file matches its source), `python3 scripts/check_repository.py`
  and `python3 tools/check_icon_maps.py`: PASS (the icon check's 11 warnings are older items').
- No Minecraft jar here: compilation and the game tests run only in CI.

Part 1b, local (before CI):
- `python3 tools/owner_art.py` (wrote the two worn sheets) and `python3 tools/generate_material_data.py` (the models and
  render definitions); run again, it changed nothing (CI's "generated JSON is up to date" step).
- `python3 tools/check_mod_data.py`: PASS, with `check_wayfaring_worn` and the art check (4,540 models, the three new
  ones among them). Mutation test of `check_wayfaring_worn`, through the function itself: 30 deliberate breakages, each
  caught by the rule meant for it (among them each sole on the pants layer, on vanilla leggings or 0.05 px off, the
  belt's underside back at the torso's bottom, growth below the gaps, the two boots' faces 0.05 px apart or sharing
  planes, a second model on the body on the strap's planes, a belt box on the chestplate's shell, a changed anchor, a
  stray render definition for the Ice Breaker or a charm, by id, by tag, in a subfolder or in another namespace, a
  malformed definition, a hand-edited model or definition, a worn sheet given an animation, a texel's shift of a net,
  and a Java renderer for the boot); every file restored after.
- `python3 tools/owner_art.py --check` (every imported file matches its source), `python3 tools/check_icon_maps.py`,
  `python3 scripts/check_repository.py` and `python3 tools/concordance_delivery.py`: PASS.
- Orthographic previews of the generated models on a grey body (the part 1b design's preview script, not the game):
  the strap with its buckle in front, the boots as the icon draws them. Before writing anything, three reviewers checked
  the design against Trinkets' source: one confirmed the orientation, one re-derived every face's texels by its own
  code, one emulated CI's Python steps.

CI:
- Run 38009567435 (commit `fc441fff`): everything compiled. Both server jobs: 1,183 of 1,184 tests passed; the one
  failure was this slice's test expecting a claimed zombie to be spared by the wave, which the boundary's rule never
  does (anyone may fight monsters), so the test now checks an immune foe instead. The client tests passed in all four
  jobs; `ConcordanceWayfaringClientGameTests` ran in the fourth and logged "Charm slots with the belt: 2 on the server, 2
  on the client" and "attributes worn: as designed", and saved `jugcraft_wayfaring_inventory` and
  `jugcraft_wayfaring_icons`.
- The review that followed (five reviewers, each finding checked by a second) also found: the charms charged food for
  every tick of fire (a refused blow never starts vanilla's hurt cooldown) and took any blow for at most ten food
  points (vanilla caps exhaustion), both fixed as described under How it works; a cosmetic belt could be locked on and
  would be duplicated on death, both fixed; the Phoenix Down's recipe page and three docs still calling the drive belt
  a leather belt, fixed.
- Run 38012291924 (commit `fdcd3f3c`, the review fixes): the new food-cost test passed; both server jobs failed on one
  case only, again a wrong expectation of this slice's test: it took the boundary's immune tag to spare the Elder
  Guardian from the wave, but tolerance governs only harmful control (throws and statuses), so the harm lands and only
  the slowing is refused. The test now checks exactly that, and the docs say so. One client job also failed, in an Arms
  VIII test this change does not touch (a thrown javelin came down without striking its pig; the same test passed in
  the run before, and nothing it uses changed); the next run repeats it.
- Run 38014767529 (commit `7a68731a`, the boss test fix): every job passed, the mod job on its second attempt. All
  1,185 server tests passed without the optional integrations and on the mod job's re-run, every Wayfaring test among
  them; the four client jobs passed, the javelin test among them. The mod job's first attempt failed one test, Arms
  VIII's `harpoon_hauls_its_catch` ("The harpoon took 0.0, not 6.0 (come down at 1.49 2.00 8.93)"), which this change
  does not touch and which has failed the same way before ([cakes](cakes.md)): simulating that test's throw, about
  0.85% of throws come down just short of the pig through the throw's random spread, where this one did.
- Run 38038861880 (commit `007cda98`, part 1b): every job passed; the new client checks compiled and ran in the third
  client job, which logged "worn models: definitions resolved to non-empty elements, models present, sheets stitched".
  Its twelve worn shots, looked at in the job log's 480 × 270 previews by the assistant that wrote this part (a person
  has not looked at them yet), show the strap with its gold buckle at the waist and the green boots with their white
  cuffs and toe caps, from every side, over iron leggings and boots, over the chestplate (the buckle through it) and
  sneaking, with no missing texture. They also showed the creative guide every creative player is given held in front
  of the belt, and the body about 8° off north (a teleport turns only the head); the test now empties the hand and sets
  the body's turn.
- The review that followed (four reviewers, each finding checked by a second) also found: the client test's count of
  elements described as a check of their parts (reworded: `check_wayfaring_worn` holds the parts); the definition check
  crashing on a malformed file and blind to tag targets, subfolders and other namespaces, the vanilla shells tested
  over too small an area, two models on one part never compared and the anchor never checked (all fixed in the check);
  the claim that no two faces of a model share a plane (true only of faces facing the same way, now said); and limits
  the record left out (cosmetic stacks' sync, equipment hiding, armour drawn toward the camera; below).
- Run 38044387828 (commit `e091d654`, the review fixes): every job passed, the mod job on its second attempt. Its
  first attempt failed one test, Pixel Hollows' `every_village_has_one_shop` (a desert village with no shop), which this
  change does not touch and which the [Retro Trader](retro-trader.md), [Walled Town](walled-town.md),
  [World Designer](world-designer.md) and [Fall Additions](fall-additions.md) records already list as failing now and
  then (its re-layout budget is finite); it passed in the same commit's optional-integrations job and on the re-run. The
  client test logged the same three lines, and its twelve worn shots (again the log's previews, looked at by the
  assistant) show the hand empty and the body square to the camera: the strap and buckle from the front, the strap from
  behind, the boots from every quarter with their fins behind, over iron leggings and boots, and the buckle through the
  chestplate.
- Run 38064602632 (commit `5a584a09`, part 1c as first made: hidden under armour, and a switch that hid everyone's
  belts and boots on one computer): every job passed. Both server jobs ran 1,277 tests, all passing. The client test
  ran in the second of the four client jobs and logged "worn models: the belt drawn on the body and the boot on each
  leg, models present, sheets stitched" and "hidden under armour and by the setting: as designed". Its new shots, looked
  at in the log's 480 × 270 previews by the assistant that wrote this part (a person has not looked at them yet), show
  an iron chestplate with no belt and the green boots below it, iron boots with the belt and buckle above them, and with
  the setting off neither, with no missing texture; the other worn shots are as before. The inventory shot showed the
  creative inventory, not Trinkets' slots (the test's player is in creative, and vanilla gives a creative player the
  creative screen), as it had since part 1; the test now puts the player in survival for that shot.
- Run 38070788686 (commit `3abbad16`: each wearer's choice seen by everyone, and the review's fixes): every job passed.
  Both server jobs ran 1,278 tests, all passing, one more than before (the new `eachWearerChoosesWhetherTheirTrinketsShow`;
  the log names only failures, so it is counted, not seen by name), and each re-ran two other features' tests once (an
  Arms VIII javelin test and a Madame Tatterlace test), which passed. The client test ran in the first client job and
  logged "worn models: the belt drawn on the body and the boot on each leg, models present, sheets stitched" and "hidden
  under armour and by the setting: as designed", now with the elytra, the element's own armour test, and every turn of
  the setting made through the server and checked once its answer came back. Its shots (the log's previews, looked at
  by the assistant that wrote this part) show the same as the run before (no belt under the chestplate, no green boots
  under iron boots, neither with the setting off, this time turned through the server), and the inventory shot is now
  the survival inventory, with Trinkets' slot buttons beside the figure and the green boots on it (the belt is already
  off by then).

Part 1c, local (before CI; run on the branch, then again after the branch was restarted from main at `2bc6e94f`,
once #277 had merged parts 1 and 1b):
- `python3 tools/generate_material_data.py` wrote the wrapped render definitions and the setting's words; run again, it
  changed nothing.
- `python3 tools/check_mod_data.py`: PASS, with `check_wayfaring_worn`'s new rules (then mostly looking for text in the
  source; the review below found that some held less than this list says, and they were tightened): `WORN_COVERED_BY`
  names exactly the drawn items, each with distinct armour slots; the definitions use Jugcraft's element; the element
  reads the setting and the wearer's armour, asks for an equipment asset and no glider, and passes its models on;
  `WayfaringClient` registers it and is called; the setting loads and saves with `true` at first, its switch defaults on
  and is saved; its words are in the language file. Mutation test of `check_wayfaring_worn`, through the function
  itself: part 1b's 30 breakages and 13 more, each caught by the rule meant for it (`WORN_COVERED_BY` leaving out the
  boot, naming a hand or naming a slot twice; another element id; the element not checking gliders, ignoring the setting
  or not passing its models on; the element not registered, or `WayfaringClient` never called; the setting off at first;
  the switch off by default or never saved; the setting's words missing); every file restored after.
- `python3 tools/owner_art.py --check`, `python3 tools/check_icon_maps.py`, `python3 scripts/check_repository.py` and
  `python3 tools/concordance_delivery.py`: PASS.
- Jugcraft's 3D armour (`tools/armor_models.py`) compared with the worn models by script, for the one pairing part 1c
  still draws together, leggings under the boots (Under armour, below).

Part 1c, the choice made each wearer's own and seen by everyone (the owner, 10 October 2026: "I want other players to
see it too"; local, before CI):
- `python3 tools/generate_material_data.py` wrote the setting's new words and the codex line; run again, it changed
  nothing.
- `python3 tools/check_mod_data.py`: PASS, with `check_wayfaring_worn`'s rules changed and added: the element reads the
  wearer's choice (`WornDisplay.hidden`) and never this computer's setting; the client sends the setting on joining and
  on each save, and only to a server that takes it; the attachment is sent to everyone who sees the wearer, kept
  through death and not saved; the receiver takes a choice only for its sender, rate-limited both where a change is
  made at once and where a waiting one lands, and forgets a waiting choice when its player leaves; `Wayfaring`
  registers it. Mutation test: the 43 breakages before (one now the element ignoring the wearer's choice) and 14 more,
  57 in all, each caught by the rule meant for it (among the new: the element reading this computer's setting instead;
  the client not sending on joining, sending without asking whether the server takes it, or sending something else;
  the attachment sent to the wearer only, lost at death or saved; the receiver choosing for another player; either
  rate limit gone; a waiting choice never forgotten; `WornDisplay` never registered; either of the setting's saves not
  sending); every file restored after.
- `python3 tools/owner_art.py --check`, `python3 tools/check_icon_maps.py`, `python3 scripts/check_repository.py` and
  `python3 tools/concordance_delivery.py`: PASS.

The review of part 1c as first made (four reviewers, on commit `5a584a09`, each finding checked by a second; 15 of 17
held) found, besides wording the choice's change had already replaced: six of Jugcraft's 3D pieces that stop short of
the waist still hide the belt, and the Wool Socks were misdescribed (both now in the limits above and the owner's
question); the "Under armour" figures covered only the seven 3D sets there were before #277 (recomputed for all 20,
with two chestplates' leg plates that the right boot's fin passes through); armour given the glider component does not
hide them, and Trinkets' own cosmetic-armour overrides go unseen (both now in the limits); the new rules of
`check_wayfaring_worn` mostly looked for text in the source, and failed on harmless reflows (they now check each
definition's structure and the code with its comments taken out, and accept reflows); nothing put on an elytra (the
client test now does) and CI would have picked no client test for a change to the element alone (the test now asks
the element's own armour test, and CI's choice picks it for each of the slice's new classes); a mistyped value of the
setting, or of the Focus line's, turned it off for good (now only "false" does); and the Concordance contract's status
paragraph and the Ember records still said those parts were only on this branch. Two findings did not hold.

Part 1c review fixes, local (before CI):
- `python3 tools/generate_material_data.py` wrote the setting's and the codex's new words; run again, it changed
  nothing.
- `python3 tools/check_mod_data.py`: PASS. Mutation test: 66 breakages, each caught by the rule meant for it (the 57
  before and nine more: the definitions unwrapped, or naming no armour, generator and files alike; the asset, slot or
  glider test dropped, the glider one left named in a comment; the switch saving into another setting or seeded from
  one; `set()` ignoring the setting; another setting saved under its key); and five harmless reflows (the registration,
  the switch's value, tooltip and save call, and the receiver, split over lines) still pass; every file restored after.
- `python3 tools/owner_art.py --check`, `python3 tools/check_icon_maps.py`, `python3 scripts/check_repository.py` and
  `python3 tools/concordance_delivery.py`: PASS. `tools/select_client_tests.py`'s choice, asked of each of the slice's
  new classes alone (the element, `WayfaringClient`, `WornDisplay`, `WornDisplayPayload`), picks the Wayfaring client
  test.

Tests:
- Server, `ConcordanceWayfaringGameTests`: the items and slots as designed (sizes, the owner's icons, no cosmetic copies,
  tags, every attribute present, never weighed, the two belts' names); only Relic Lore puts them on and two of a kind
  never add up (each item's modifiers from two slots, a feather and a down jumping once); the belt keeps its charm; the
  charms take their own harm from food and nothing else (not lava, not held, not with an empty bar, not before Relic
  Lore); a blow costs half a food point a point of harm, a second within the hurt cooldown only what it is bigger by,
  and one the bar cannot pay for lands; the vial, then the down, answer death through the real event, and the void, a held totem and Relic Lore not
  understood do not; a vial put on during a dream does not answer a death in it, and the dream ends as a death; the
  wave reaches a zombie and not a villager or one beyond a light fall's radius, harms but does not slow an Elder
  Guardian, does nothing after an ender pearl or before Relic Lore, and reaches at most twelve; and (part 1c) each
  wearer's choice as the server takes it: hidden and shown again, a choice that changes nothing dropped, a change within
  10 ticks of the last waiting for its tick and replaced by a newer one, the newest landing, and the choice's
  attachment sent to clients, kept through death and not saved.
- Client, `ConcordanceWayfaringClientGameTests`, on the real ticking player: the belt's second Charm slot on the server
  and the client; the worn attributes each present once; what Trinkets needs to draw part 1b: no Java renderer in the
  way, the belt's and boot's render definitions loaded and, run as Trinkets runs them (with the real player, against a
  stand-in that records where each model would be attached), drawing the belt's model on the body and the boot's on
  each leg, the worn models among the client's resources, the worn sheets in the items atlas and no definition for the
  Ice Breaker (a model that failed to load would be baked as the game's missing-model cube, which these checks cannot
  tell apart: only the shots show the owner's models); part 1c, the same way, with the setting turned on first, whatever
  this client had, and put back after: the belt and boots drawn bare, the belt hidden under an iron chestplate and under
  iron leggings with the boots still drawn, the boots hidden under iron boots with the belt still drawn, both drawn
  under an elytra, both hidden with the setting off and both drawn again with it on, and in each state the element's
  own armour test (`covered`) agreeing with what is hidden. Each turn of the setting is made as the settings screen makes
  it, which sends it to the server, and checked once the server's player and this client's agree with it: what this
  client draws follows the choice as it came back from the server, as it goes to everyone who sees the player. Then,
  the hand emptied and the body set facing north, the player
  wearing them from the front and from behind (whole and closer), from each quarter, under an iron chestplate, under
  iron boots, with the setting off, and sneaking; the slot gone again without the belt; screenshots of the eight icons
  and of the survival inventory, where Trinkets shows its slots (the player is put in survival for it, and the test
  checks that screen opened). The screenshots are for a person to look at; nothing judges a picture.

Not yet run: a person looking at the screenshots (part 1b's included), the Trinkets screen opened by hand (the slot
icons and the grey slot names before Relic Lore), a real fall, drowning or burning in survival, and a two-client
dedicated server (where the other player should see the belt and boots, hidden under the armour they wear and when
they have chosen to hide them: CI has one client, so no test sees another player's choice arrive, though Fabric sends it
to every client the same way it sends it back to its own). Part 1b in a client without Sodium (CI's client jobs run
with it). Part 1c under armour other than vanilla iron (Jugcraft's 3D sets, Ember's GeckoLib sets, the Rocket Pack,
Scuba Tank and Wool Socks), its switch turned by hand on the settings screen (the test turns the setting in code, as
the screen's save does), and the choice through a death, a change of dimension and a rejoin.

## World and event applicability

No worldgen, creatures, loot or seasons.

## Rollout and open questions

- New stable ids: items `jugcraft:leather_belt`, `angelic_feather`, `kraken_shell`, `infernal_claws`, `angelheart_vial`,
  `phoenix_down`, `amphibian_boot`, `ice_breaker`; Trinkets slots `legs/charm` and `feet/boots`
  (`data/trinkets/entities/jugcraft_wayfaring.json`, which also gives players Trinkets' `legs/belt`); modifier ids
  `jugcraft:wayfaring/<kind>/<attribute>`; codex entry `relics/wayfaring`. Save compatibility: additive. The kinetic
  belt's English name changed (Leather Belt to Drive Belt); its id did not. Part 1b adds resources only, none saved:
  models `jugcraft:item/leather_belt_worn`, `amphibian_boot_worn_left` and `amphibian_boot_worn_right`, textures
  `jugcraft:item/leather_belt_worn` and `amphibian_boot_worn`, and Trinkets render definitions `jugcraft:leather_belt` and
  `jugcraft:amphibian_boot`. Part 1c adds the render element type `jugcraft:unless_covered` (client only, named by those
  two definitions), the client setting `concordance.worn_trinkets`, the payload `jugcraft:worn_trinkets_shown` and the
  player attachment `jugcraft:worn_trinkets_hidden` (sent to clients, never saved); nothing saved in a world.
- **Simplified from the owner's text, for a concrete reason:**
  - the Ice Breaker's faster falling: a gravity modifier would also shorten every jump (and undo the feather's), so it
    is left out;
  - the Ice Breaker's grip on ice and the Amphibian Boot's speed in the rain: no event or attribute gives them without a
    mixin or a per-tick check;
  - the Phoenix Down's "variety of buffs": two (Regeneration II and Fire Resistance);
  - the base mods' relic levelling and experience: Relic Lore's mastery already stands for it.
- **Deferred:** the Ice Breaker on the body (the owner drew no worn sheet for it; `check_wayfaring_worn` fails once one
  appears in the library and is not drawn); the charms on the body (no worn sheets); the other worn relics (parts 2, 3
  and 5); the held relics and staves (parts 4a and 4b, after the Ars-based core).
- **Part 1b's limits** (drawn by Trinkets): only in third person (and the inventory's figure); on an invisible wearer
  the belt and boots still show, as vanilla armour does (Trinkets does not check invisibility, and part 1c's element
  does not either, to match armour); seen from below, a skin whose pants layer has its underside drawn shows it over the
  right sole, which lies 0.1 px above it; between the legs the left boot is drawn over the right, as one of vanilla's
  boots is.
- **Under armour** (part 1c). Armour is drawn a little toward the camera (1/4096 of its distance; Spell Engine's copy of
  vanilla's armour render type shows the setting, inferred for vanilla's own), and the belt and boots, drawn as items,
  are not, so armour whose faces lie just inside theirs shows through, flickering, from about 256 blocks per pixel of
  the gap. Part 1b had that over vanilla leggings and boots (from about 38 blocks) and over some of Jugcraft's 3D
  armour (from 8 to 19 blocks: the Bloodthorn boots and chestplate, and at the buckle the Hades chestplate and the steel
  and bronze leggings). All of that armour now hides what it met. Still drawn with the boots are leggings and
  chestplates. Vanilla leggings lie 0.65 px inside the boots (from about 166 blocks). Jugcraft's 3D leggings (all 20
  sets in `tools/armor_models.py`) were compared by a one-off script, only where their faces lie square to the leg: the
  closest lie 0.33 px behind the left boot's fin (the Reaper's back strip: from about 84 blocks) and 0.36 px inside the
  left boot's sides and cuff (the Dread Knight, Valkyrie, Wayfarer and Spartan skirts and the Frost Knight and Wight
  King cuisses: from about 92 blocks), and the steel and bronze leggings' low front plates, which the toe cap pokes
  through, 0.38 px inside it (from about 97 blocks); some stand outside the boots and cover them there (the Pharaoh
  lames, the Bloodthorn tassets, the Sunset Gem hems); and only the steel, bronze, Pharaoh and Hades leggings reach
  below the soles (in the ground while standing), their linings and the Hades skirts by 0.05 to 0.35 px and the lowest
  tilted plates by up to 0.61 px (steel and bronze) and 0.39 px (Pharaoh). Two chestplates hang a plate behind the
  right leg, the Spartan's (to 6.75 px below the hip) and the Wayfarer's (to 7.25 px), which the top of the right
  boot's heel fin (5 to 10 px below the hip) passes through, standing up to 2.7 px out behind them.
  `check_wayfaring_worn` does not compare the worn models with Jugcraft's 3D armour, Ember's GeckoLib sets or other
  mods' armour.
- **For the owner to decide** (nothing above waits on them): whether the guns in the Reliquary folder become one more
  gun line; whether narrow mixins are approved for the relics that need them (walking on water or lava, slippery
  ground, Riptide without rain, a chorus fruit's teleport, barter results, mob neutrality, healing hooks, the backstab
  bonus); names and designs for the 12 unnamed legacy icons; the terms of `jymbelics/sounds/ricochet.ogg`, whose tags
  name a sound-effects publisher (it matters only for a later part's Shadow Glaive); for part 1b, whether the sheet is
  the left boot, as read from the icon (if it is the right, `WORN_MIRRORED` changes to the left leg); and, for part 1c,
  whether a player should also be able to hide everyone else's belts and boots on their own screen (the first version
  of the switch did that; the owner asked for the choice to be seen by other players, and it now hides the player's
  own, for everyone). Decided: the pieces the armour test misjudges (the Rocket Pack, Scuba Tank and Wool Socks, and the
  six 3D pieces that stop short of the waist) keep hiding them.
- Skipped: `researching_table` (removed from the base mod; Relic Lore and Artifice cover it), `relic_experience_bottle`
  (levelling), `blank_rune` (a model with no texture), `witch_hat` (its id is taken by Jugcraft's own Witch Hat).
