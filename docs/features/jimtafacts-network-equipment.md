# Jimtafacts network fixtures and equipment

Local implementation by OpenAI Codex (GPT-6), October 23,2026. The owner's supplied
`art/owner-library/originals/Oct23/Jimtafacts/applied energentics reborn by jimbo/`
remains unchanged. Classes were read statically through trusted CFR/javap; no supplied
classes, JARs, ROMs or programs were executed. Original source names/notices remain in the library.

## Implemented player routes

- Certus/nether quartz axes, hoes, shovels, picks and swords retain source iron-tier stats,
  source repair materials and original item art. Fluix upgrades use the supplied smithing
  template/model,750durability,7.2000003speed,2.4attackbonus and14enchantability.
  Fluix tools retain FortuneI and the sword LootingI using native enchantment components.
  Crafting/inventory ticks restore the intrinsic minimum; higher enchantments remain.
- Both quartz wrenches use existing Jugcraft machine/pipe rotation and dismantling;
  network machines add their native orientation. All actions check claims and ownership.
- Both50-use cutting knives open a name-press engraving interface using the original
  `quartzknife.png`. One metal ingot and one durability produce one named press; holding
  the same knife is required throughout the session. Inscriber slot0holds the reusable
  press, slot1the item; its existing powered workshop mechanism writes the name.
- Charged Staff consumes300JE for its extra electrical strike. Entropy Manipulator
  consumes1600JE per heat/cool action or eight-second ignition hit. Source ten explicit
  entropy conversions are represented, with sand/glass,clay/terracotta,netherrack/brick
  smelts. Water placement checks adjacent permission boundaries. Protected blocks,
  containers and denied targets do not consume charge or change.
- Color Applicator consumes100JE and an offhand dye for vanilla wool/carpet/terracotta/
  concrete/powder/glass/panes or a permitted sheep. Its original45-element mesh, UVs,
  three original AEColor tint palettes and native colored/uncolored selector are kept.
- Matter Cannon loads/unloads a single64-round magazine by sneak-use, using offhand
  material. Supplied matter-ball and nugget weights determine damage through the source
  `ceil(weight/20)` formula. Each shot costs1600JE. The source255-block reach is clipped
  at unloaded chunks; penetration is capped at4blocks, protected blocks/containers stop
  the ray, and entity damage uses shared PvP/party/claim rules. No projectile tick entities
  or chunk loading are introduced. The magazine survives saving as a component.
- Memory Card sneak-use copies only same-type filter/threshold/polarity. Normal use
  restores those settings after current permission checks. It cannot copy owner, energy,
  cell contents, pattern jobs or physical upgrades. The original LED plus eight1×1identity
  quads are retained; a native settings hash drives their source palettes.
- Network Tool reports the loaded,permission-bounded network's online state,device
  count,JE and cell slots. Sneak-use uses the wrench. It explicitly reports that native
  networks have no AE channel simulation.
- Quartz Fixture retains all four authored standing/wall/odd meshes, six orientations,
  support checks,waterlogging,light15and a bounded occasional spark.
- Quartz Glass/Vibrant Glass use original pane/frame pixels and source face edge masks,
  including connections between both glass types. The vibrant version emits15light.
  Multipart adaptation substitutes block-seeded pane variation for runtime UV jitter.
- Growth Accelerator retains both supplied models and six orientations. Shared JE enters
  the axis ports. A1600JE buffer spends160JE once per second while enabled, examining at
  most six adjacent loaded certus/amethyst budding blocks,one random face each. It never
  invokes arbitrary neighboring randomTick code. Both source and destination permission
  are checked, vanilla stages/waterlogging remain, and certus degradation is preserved.

All added equipment has reachable crafting/smithing routes using existing crystals,
JE cells and processors. Matter balls also have a native clay/cobble/quartz-dust route.
The shared charger/charging station supplies powered tools. No second energy currency
or ore source was introduced.

## Explicit adaptations and remaining source gaps

These are native functional integrations, not full AE2 parity. Paint projectiles/lumen
native surface splotches and a finite paint magazine are implemented; arbitrary layered source splats remain unsupported.
The cannon uses a64-round native magazine rather than the source512-byte cell/upgrades.
Entropy's arbitrary recipe-driven smelting and fire/TNT fallback are not enabled.
Network Tool now has a real nine-slot upgrade-card wallet. Smart cable bars display routed native channel usage.
The source has quartz fixtures and fluix building materials, but no separate fluix-light
fixture mesh; no fabricated recolored fixture is presented as an original asset.

The generated [complete source registry mapping](../../art/owner-library/catalog/jimtafacts-ae-registry.json)
lists each source item definition as `functional_native`, `reused_existing`, `internal_source_only` or `pending`.
Source debug icons are recorded explicitly; models alone are never counted as gameplay.
Smart/dense/colored cables, P2P and quantum storage bridges are locally implemented. Spatial storage covers bounded 2³/16³ static building exchange; larger migration and chunk anchors remain pending. It is an implementation inventory, not test or original-VM parity evidence.

## Validation state

No build or tests run during this work, at the owner's request. Static API inspection
caught the current Inventory return-item API's Prediction argument and the six-direction
network orientation change. Authored tests cover knife payment/session invalidation,
memory filter sanitization, protected entropy/growth, source geometry/UV retention,
native selectors, full source inventory accounting and byte-identical glass pixels.
Final combined compilation,server/client tests and the owner's visual playtest remain.

Registration hooks:DigitalFixtures.register after materials;DigitalEquipment.register
after existing network components/controls;DigitalEquipmentClient.register on client.
Generator helpers:`jimtafacts_network_fixtures`,`jimtafacts_network_equipment`;both take
the parent's source/model/texture callbacks. The equipment helper also generates native
ammo/palette constants from source data. Registry map generation receives actual native
IDs from the central generator.
