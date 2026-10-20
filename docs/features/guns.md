# Guns: the scrap guns (slice 1), the iron set (slice 2), the lever set (slice 3), the black powder guns (slice 4), the attachments (slices 5 and 7), the guns in use (slice 6), the scopes (slice 7b), the hand guns (slice 8), the service arms (slice 8B), the heavy weapons (slice 8C), the energy weapons (slice 8D), the marksman rifles (slice 9A), the automatic weapons (slice 9B), the second energy weapons (slice 9C), the pump shotguns (slice 9D), the tactical grip and laser sight (slice 9E), the aiming polish (slice 9F), the Trench Lobber's grenades (slice 9G), the launchers (slice 10A), coil and plasma (slice 10B), the double-barrels (slice 10C), the sculk guns (slice 10D), the Cell Rack (slice 10E), enemies with guns (slice 10F), two guns at once (slice 10G), the Nether guns (slices 11A and 11B), the End guns (slice 12A), the copper guns (slice 12B), the lone guns (slice 12C), the frontier guns (slices 13A to 13D), the brass sidearms (slice 14A), the brass carbines (slice 14B) and the brass quick-firers (slice 14C)

Status:
- **Slices 1 to 12C are in `main`:** 10A to 11B with #320 on 11 October 2026; 12A to 12C with #352 on 13 October 2026, which closed their own pull requests (#332, #338 and #344). The lines below say where each was built.
- **Slice 1** (the Rust Midge, Patchwork Carbine and Thunderpipe): implemented on `claude/guns` (#248), awaiting review. It is stacked on `claude/owner-gun-models` (#247), which adds the owner's gun models to the owner asset library.
- **Slice 2** (the iron set: the Warden Pistol, Riveter SMG and Haymaker; [below](#slice-2-the-iron-set)): implemented on `claude/guns-iron` (#252), stacked on slice 1, awaiting review.
- **Slice 3** (the lever set: the Longhorn Rifle, Drover Rifle and Coach Gun; [below](#slice-3-the-lever-set)): implemented on `claude/guns-lever` (#253), stacked on slice 2, awaiting review.
- **Slice 4** (the black powder guns: the Duelling Pistol, Line Musket and Bellmouth, and the Paper Cartridge; [below](#slice-4-the-black-powder-guns)): implemented on `claude/guns-powder` (#255), stacked on slice 3, awaiting review.
- **Slice 5** (the attachments: silencers, a muzzle brake, an extended barrel, magazines, stocks and grips; [below](#slice-5-the-attachments)): implemented on `claude/guns-attachments` (#256), stacked on slice 4, awaiting review.
- **Slice 6** (the guns in use: muzzle flash, spent casings, a zoom when aiming and the hold seen from outside; [below](#slice-6-the-guns-in-use)): implemented on `claude/guns-polish` (#259), stacked on slice 5, awaiting review.
- **Slice 7** (finishing the attachments: bayonets that stab, and the five guns whose parts use shared textures; [below](#slice-7-bayonets-and-the-shared-texture-guns)): implemented on `claude/guns-attachments-2` (#260), stacked on slice 6, awaiting review.
- **Slice 7b** (the scopes: the Long Scope, Medium Scope and Reflex Sight; [below](#slice-7b-the-scopes)): implemented on `claude/guns-scopes` (#265), stacked on slice 7, awaiting review.
- **Slice 8** (the hand guns: the Bulldog Pistol, Marshal Revolver and Sapper Revolver; [below](#slice-8-the-hand-guns)): implemented on `claude/guns-revolvers` (#268), stacked on slice 7b, awaiting review.
- **Slice 8B** (the service arms: the Sentry Pistol, Garrison Rifle and Breacher; [below](#slice-8b-the-service-arms)): implemented on `claude/guns-service`, stacked on slice 8, awaiting review.
- **Slice 8C** (the heavy weapons: the Trench Lobber, Thresher and Stoker; [below](#slice-8c-the-heavy-weapons)): implemented on `claude/guns-heavy`, on the integration branch that holds slices 7b to 8B, awaiting review.
- **Slice 8D** (the energy weapons: the Beam Pistol, Stormlock Rifle and Linesman, and the Energy Cell they run on; [below](#slice-8d-the-energy-weapons)): implemented on `claude/guns-energy`, stacked on slice 8C, awaiting review.
- **Slice 9A** (the marksman rifles: the Picket Rifle, Ranger Rifle and Kestrel Rifle; [below](#slice-9a-the-marksman-rifles)): implemented on `claude/guns-marksman` (#285), stacked on slice 8D, awaiting review.
- **Slice 9B** (the automatic weapons: the Rattler Pistol, Bronco SMG and Squall Rifle; [below](#slice-9b-the-automatic-weapons)): implemented on `claude/guns-automatic` (#286), stacked on slice 9A, awaiting review.
- **Slice 9C** (the second energy weapons: the Spikedriver, Seam Cutter and Caisson Pistol; [below](#slice-9c-the-second-energy-weapons)): implemented on `claude/guns-energy-2` (#287), stacked on slice 9B, awaiting review.
- **Slice 9D** (the pump shotguns: the Sledge, Highwayman and Throttle; [below](#slice-9d-the-pump-shotguns)): implemented on `claude/guns-pump` (#289), stacked on slice 9C, awaiting review.
- **Slice 9E** (the Tactical Grip and the Laser Sight; [below](#slice-9e-the-tactical-grip-and-the-laser-sight)): implemented on `claude/guns-tactical` (#290), stacked on slice 9D, awaiting review.
- **Slice 9F** (the aiming polish: the hands and the fitted stocks kept off the sights; [below](#slice-9f-the-aiming-polish)): implemented on `claude/guns-aiming` (#291), stacked on slice 9E, awaiting review.
- **Slice 9G** (the Trench Lobber's grenades: it loads the chemical grenades too; [below](#slice-9g-the-trench-lobbers-grenades)): implemented on `claude/guns-lobber-grenades`, stacked on slice 9F, awaiting review.
- **Slice 10A** (the launchers: the Earthmover, Skylark Rifle and Bullfrog; [below](#slice-10a-the-launchers)): implemented on `claude/guns-launchers`, based on `main`, awaiting review.
- **Slice 10B** (coil and plasma: the Solenoid Rifle, Votive Rifle and Glowmouth; [below](#slice-10b-coil-and-plasma)): implemented on `claude/guns-coil-plasma`, stacked on slice 10A, awaiting review.
- **Slice 10C** (the double-barrels: the Mule, Fowler and Culverin; [below](#slice-10c-the-double-barrels)): implemented on `claude/guns-double-barrels`, stacked on slice 10B, awaiting review.
- **Slice 10D** (the sculk guns: the Undertone Rifle, Murmur SMG and Reverb; [below](#slice-10d-the-sculk-guns)): implemented on `claude/guns-sculk`, stacked on slice 10C, awaiting review.
- **Slice 10E** (the Cell Rack, which charges six Energy Cells at once; [below](#slice-10e-the-cell-rack)): implemented on `claude/guns-cell-rack`, stacked on slice 10D, awaiting review.
- **Slice 10F** (enemies with guns: the Raider Gunner, who carries and fires the service arms; [below](#slice-10f-enemies-with-guns)): implemented on `claude/guns-raider-gunners`, stacked on slice 10E, awaiting review.
- **Slice 10G** (two guns at once: a one-handed gun in each hand, both fired; [below](#slice-10g-two-guns-at-once)): implemented on `claude/guns-dual-pistols`, stacked on slice 10F, awaiting review.
- **Slice 11A** (the Nether guns: the Tusker, Cinder Repeater and Bastion Rifle; [below](#slice-11a-the-nether-guns)): implemented on `claude/guns-nether`, stacked on slice 10G, awaiting review.
- **Slice 11B** (the rest of the Nether guns: the Ashfall Pistol, Goldbristle Carbine and Crackling; [below](#slice-11b-the-rest-of-the-nether-guns)): implemented on `claude/guns-nether-2`, stacked on slice 11A, awaiting review.
- **Slice 12A** (the End guns: the Chitin SMG, Hermit Rifle and Borer; [below](#slice-12a-the-end-guns)): implemented on `claude/guns-12a`, off `main`, awaiting review.
- **Slice 12B** (the copper guns: the Penny Pistol, Conductor Carbine and Magpie SMG; [below](#slice-12b-the-copper-guns)): implemented on `claude/guns-12b`, stacked on slice 12A, awaiting review.
- **Slice 12C** (the lone guns: the Tuppence Pistol, Goshawk Revolver and Lament Rifle; [below](#slice-12c-the-lone-guns)): implemented on `claude/guns-12c`, stacked on slice 12B, awaiting review.
- **Slice 13A** (the frontier guns: the Tabby Revolver, Lookout Rifle and Ammonite Carbine; [below](#slice-13a-the-frontier-guns)): implemented on `claude/guns-13a`, built on slice 12C and brought onto `main` once 12C was in it, awaiting review.
- **Slice 13B** (more frontier guns: the Fencepost Carbine, Waystation Shotgun and Highball Rifle; [below](#slice-13b-more-frontier-guns)): implemented on `claude/guns-13b`, stacked on slice 13A, awaiting review.
- **Slice 13C** (a punt gun, a repeater and a heavier rifle: the Flatboat Gun, Windlass Musket and Millstone Rifle; [below](#slice-13c-a-punt-gun-a-repeater-and-a-heavier-rifle)): implemented on `claude/guns-13c`, stacked on slice 13B, awaiting review.
- **Slice 13D** (a revolving launcher, a carbine revolver and a musket of light: the Fencepost Thumper, Parley Revolver and Lantern Musket; [below](#slice-13d-a-revolving-launcher-a-carbine-revolver-and-a-musket-of-light)): implemented on `claude/guns-13d`, stacked on slice 13C, awaiting review.
- **Slice 14A** (the brass sidearms: the Holler Pistol, Minuet Pistol and Half-Crown Revolver; [below](#slice-14a-the-brass-sidearms)): implemented on `claude/guns-14a`, stacked on slice 13D, awaiting review.
- **Slice 14B** (the brass carbines: the Holler Carbine, Minuet Carbine and Grange Carbine; [below](#slice-14b-the-brass-carbines)): implemented on `claude/guns-14b`, stacked on slice 14A, awaiting review.
- **Slice 14C** (the brass quick-firers: the Sprocket Gun, Navvy Shotgun and Freeholder Rifle; [below](#slice-14c-the-brass-quick-firers)): implemented on `claude/guns-14c`, stacked on slice 14B, awaiting review.
- **Not yet played:** the Java compiles only in CI, and the game tests there are the only runs.
Proposal issue: none. The owner asked on 7 October 2026: "I want to start working on the Guns plugin which I want to base off of the Mod Scorched Guns 2 I have models and animations that I have created already on the github in the "Blocks" folder for that part". The owner's answers:
- on the files: "the files in the blocks folder are all mine I made all of them myself and have all the rights to them they are inspired by scorched guns 2 but I made all of them including the animations";
- on the models, with the upload: "I have rights for all these";
- the first guns: "Gnat, Makeshift, Boomstick";
- the names: "Propose names";
- the eight sounds that carry other sources' tags: "I have the rights";
- the next guns ("Ok lets do more!!!"): all four sets offered, the iron set, the lever rifles, the black powder guns and the attachments, each in its own slice; names: "Propose names".
- what next ("ok what next"): all four offered, each its own pull request: gun polish (this slice 6), finishing the attachments, more guns, and the flaky tests.
- the scopes: on 8 October 2026, with the reticles and lens rims uploaded ("heres reticles and vignette"), asked what the scopes should use, they answered that they made those files and to use them (see [Dependencies and assets](#dependencies-and-assets)); and asked what next ("What next?"), they chose all four further gun sets offered, each its own slice.
- the heavy weapons: on 9 October 2026, offered how each would work (the Hammer GL firing the existing grenades, which break no blocks; the Gattaler spinning up for about ¾ s and firing rifle rounds; the Kiln Gun burning blaze powder and setting creatures, not blocks, alight), they answered "yes to all, do the heavy weapons next".
- the energy weapons: in the same answer ("yes to all") they took the offer that the energy weapons charge from the energy system; then, on 9 October 2026, "do the energy weapons next".
- the next part: on 10 October 2026, "Ok lets do the next part". Offered four more gun sets and three smaller follow-ups, each its own pull request, they chose all of them: "Marksman rifles (Recommended), Automatic weapons, Energy weapons II, Pump shotguns" and "Tactical grip + laser, Aiming polish, Lobber gas grenades". The marksman rifles are slice 9A, the automatic weapons slice 9B, the second energy weapons slice 9C, the pump shotguns slice 9D, the first follow-up, the tactical grip and the laser sight, slice 9E, the second, the aiming polish, slice 9F, and the third, the Lobber's grenades, slice 9G.
- the round after: on 10 October 2026, asked "Look good what do we need next", and offered more of their gun sets and some systems to add alongside them, each its own pull request, they chose "Launchers (Recommended), Coil and plasma, Double-barrels, Sculk guns" and "Energy Cell rack (Recommended), Enemies with guns, Dual pistols, Javelin test fix". The launchers are slice 10A, coil and plasma slice 10B, the double-barrels slice 10C and the sculk guns slice 10D; the Energy Cell rack is slice 10E, the enemies with guns slice 10F and the dual pistols slice 10G.
- the next round: on 10 October 2026, with those built, they asked "Ok lets continue and do the next thing we need to". No choice was offered this time: the next of their unbuilt guns were taken up, a set at a time, each its own pull request, starting with three of the six they made in the piglins' crimson, blackstone and gold. The Nether guns are slice 11A, and the other three slice 11B.
- the round after: on 13 October 2026, with the Nether guns in `main`, they asked "Ok lets continue and do the next thing". The next of their unbuilt guns were taken up as before: the three they made in the shulkers' purpur, each fed from a shulker box, are slice 12A.
- and again: on 13 October 2026, with slice 12A up for review, they asked again "Ok lets continue and do the next thing". Three of the five guns they made in copper are slice 12B.
- and once more: on 13 October 2026, with slice 12B up for review, they asked again "Ok lets continue and do the next thing". Slice 12C takes up one of the two copper guns left and the two guns of their library that stand alone in their materials, the one of iron and the one of echo shards. The last copper gun, the Whizzbanger, waits for the model its animations were made for ([below](#the-whizzbanger-waits)).
- and after that: on 13 October 2026, with slice 12C up for review, they asked again "Ok lets continue and do the next thing". Slice 13A takes up three of the fourteen guns of their library whose item models name plain stone as their base texture: a revolver, a lever-action rifle and a carbine.
- and next: on 14 October 2026, with slice 13A up for review, they asked "Ok lets continue and do the next thing or what should we do?". The recommendation was to keep on with their guns; slice 13B takes up three more of the same fourteen: a lever-action carbine, a lever-action shotgun and a single-shot rifle.
- and then: on 14 October 2026, with slice 13B up for review, they asked "What could we work on next?". Offered more of their guns and some follow-ups, they answered "continue" to the recommendation: slice 13C takes up three more of the same fourteen, a punt gun, a flintlock repeater and a heavier lever-action rifle.
- and after that: on 14 October 2026, with slice 13C up for review, they asked "Ok what sohuld we work on next". Offered three more of their guns first among other work, they answered "Yeah lets do slice 13D then when thats done suggest 2 and 3 again": slice 13D takes up three more of the same fourteen, a revolving grenade launcher, a revolver that takes a stock and an energy musket.
- and then: on 14 October 2026, with slice 13D up for review, they were offered again their treated-brass guns (option 2) or two small fixes (option 3), and answered "Lets do option 2 and then option 3". Slice 14A takes up the first three of the fourteen guns of their library whose item models name treated brass as their base texture: a heavy pistol, a target pistol and a break-top revolver.
- and next: on 14 October 2026, with slice 14A up for review, they asked "Begin the next part always be offering to start the next part at the end begin 14B". Slice 14B takes up three more of the same fourteen: the two conversions of slice 14A's pistols into carbines, and a carbine fed from a box magazine.
- and after that: on 15 October 2026, with slice 14B up for review and offered the next part, they answered "yes start 14C". Slice 14C takes up three more of the same fourteen, whose animations are one family: an automatic rifle fed from a pan magazine, an automatic shotgun and a quick-firing rifle.

Owner: jimbozoomer-byte (models, textures, animations and sounds: the owner; implementation: Claude Opus 5.5).
Target milestone and tier: early firearms, after the first copper and iron. These are the bottom rungs of a gun line the owner's library holds well over a hundred more of.
Primary specialty and supported player role: combat (ranged). It serves anyone who fights, and players who would rather not melee.

## Player experience
Three guns, each the owner's model with the owner's animations:

| | Rust Midge | Patchwork Carbine | Thunderpipe |
|---|---|---|---|
| The owner's gun | Rusty Gnat | Makeshift Rifle | Boomstick |
| What it is | a copper machine pistol | a stockless carbine | a sawn-off double barrel |
| Fires | held, automatic | one shot each pull | one shot each pull, 8 pellets |
| Damage | 2 a bullet | 6 | 2.5 a pellet (20 if all land) |
| Rate | 6.7 a second (every 3 ticks) | 3.3 a second (every 6 ticks) | 2.5 a second (every 8 ticks) |
| Holds | 20 | 10 | 2 |
| Reload | 2.35 s, the magazine | 2.4 s, the magazine | 0.25 s, then 0.6 s a shell, then 0.65 s |
| Spread, hip / aimed | 4° / 1.5° | 2° / 0.25° | 9° / 6° |
| Range | 48 blocks | 96 | 24 |
| Round | Light Round | Rifle Round | Buckshot Shell |

**Controls:**
- **Left click** fires (held, for the Midge). With a gun in hand the attack button never mines or punches.
- **Right click** (held) aims down the sights: the gun slides so its sight sits on the crosshair, the spread tightens, and you walk at 60%.
- **R** reloads. **H** inspects. Both are rebindable under Jugcraft's controls. Iris shader reload defaults to **F8** and JEI recipes to **Y**. See the [October 18 controls/crash repair](gun-controls-oct18.md).
- Pulling the trigger on an empty gun clicks, and reloads it if you carry its rounds.

**What you see:**
- In first person you see the gun and your own arms (your skin, wide or slim). The owner's animations play: draw, idle, shoot (and aimed shoot), inspect, and the reloads.
  - The Midge's magazine drops out and the new one slides in.
  - The Thunderpipe's left hand fetches each shell and pushes it into the breech.
- The animations' sound cues play the owner's sounds: mag out, mag in, slap, bolt, rack, rustle and the rest.
- Each shot kicks the view up a little: more for a heavy shot, less when aimed.
- The counter beside the hotbar shows the rounds loaded and the rounds you carry, or "Reloading".
- Guns have no inventory or hotbar durability-style bar; ammunition remains in the held-gun counter and tooltip.
- Other players see your gun fire and reload in your hand, and hear it.
- In the inventory, on the ground and in frames each gun shows as its 3D model, as the owner's do.

**Hits:** a shot is instant along your look, strayed by the spread, to the first block or creature in range.
- Blocks it strikes throw up dust.
- A creature takes the damage at once. Guns fire faster than vanilla's half-second shield after a hit, so the bullet damage type passes it (`minecraft:bypasses_cooldown`) and each shot counts.
- The Thunderpipe's pellets on one creature land as one hit.

**The rounds:** crafted cheaply from early metal.

| Round | Recipe | Makes |
|---|---|---|
| Light Round | copper ingot, lead nugget, gunpowder (in a column) | 8 |
| Rifle Round | brass nugget, lead nugget, gunpowder (in a column) | 4 |
| Buckshot Shell | paper and lead nugget, paper and gunpowder (each beside paper), brass nugget below | 4 |

**The guns** are crafted at a crafting table:
- **Rust Midge:** copper ingots and iron ingots round a lever.
- **Patchwork Carbine:** three iron ingots over planks, a lever and a copper ingot.
- **Thunderpipe:** two iron ingots over planks and a lever.

## Slice 2: the iron set
Three more of the owner's guns, a step up from the scrap guns, in iron and brass:

| | Warden Pistol | Riveter SMG | Haymaker |
|---|---|---|---|
| The owner's gun | Defender Pistol | Greaser SMG | Bruiser |
| What it is | an iron service pistol, held in one hand | an iron submachine gun | a short pump shotgun, fired one-handed |
| Fires | one shot each pull | held, automatic | one shot each pull, 8 pellets |
| Damage | 4 | 2.5 a bullet | 3 a pellet (24 if all land) |
| Rate | 4 a second (every 5 ticks) | 6.7 a second (every 3 ticks) | 1.4 a second (every 14 ticks) |
| Holds | 12 | 30 | 5 |
| Reload | 2.35 s, the magazine | 2.25 s, the magazine | 0.35 s, then 0.55 s a shell, then 1.15 s |
| Spread, hip / aimed | 2.5° / 0.75° | 3° / 1° | 7° / 5° |
| Range | 56 blocks | 48 | 28 |
| Round | Light Round | Light Round | Buckshot Shell |

**What you see:** the owner's animations, as for the scrap guns.
- The Warden Pistol and the Haymaker are held in the right hand alone; the left hand comes in only to reload. The pistol's slide kicks back on each shot, its magazine drops, and the left hand brings in the new one. The Haymaker's left hand feeds each shell, then works the pump.
- The Riveter is held in both hands; its bolt runs on each shot, and the magazine drops out and back.

**Crafting** (a crafting table, each round the same as before):
- **Warden Pistol:** three iron ingots over a lever and a brass ingot.
- **Riveter SMG:** three iron ingots over a brass ingot, a lever and an iron ingot.
- **Haymaker:** three iron ingots over a brass ingot, a lever and planks.

**Connections:** brass ingots (brass is alloyed from copper and zinc, the zinc switch's ore) join the iron. No new round, no new material: the pistol and SMG fire Light Rounds and the Haymaker Buckshot Shells.

**Balance:** starting numbers, a little above the scrap guns. The Riveter's 16.7 damage a second empties 30 rounds in 4.5 seconds for 75 damage; the Haymaker's 24 at point blank outdoes the Thunderpipe's 20 but holds five and fires slower.

**How the models were built** (as for the scrap guns, `tools/guns.py`):

| Gun | Bones |
|---|---|
| Warden Pistol | `gun_body2` > `gun_body` (main and standard barrel); `bolt` (the owner's `receiver`, the slide); `magazine` and `magazine_2` (both the standard magazine: the reload drops one and brings the other in) |
| Riveter SMG | `gun_body2` > `gun_body` (main, standard barrel and sights); `bolt`; `magazine` |
| Haymaker | `gun_body2` > `gun_body` (main); `barrel` (the pump: the shot and the pump slide it back) |

- **Hands:** the right hand on each grip (pistol (8, 1.9, 15.3), SMG (8, 1.45, 15.85), Haymaker (8, 1.7, 15.75)); the Riveter's left on the fore-end (8, 2, 8.5).
- **One-handed guns:** the Warden's and Haymaker's idles hide the left arm, so its rest point is given for a reload keyframe instead (`"hand_pose"` in `BUILDS`):
  - the Warden's left hand is at the magazine's base (8, 0.35, 15.3) at 1.75 s into the reload. Through the insert (1.37 to 1.7 s) the hand stays within a pixel of the new magazine;
  - the Haymaker's is under the barrel (8, 4.3, 9) at 0.29 s into the reload's end. Through the pump (0.33 to 0.58 s) it stays about 2 px under the barrel as both slide back.
- **Arms:** run down, back and out from each hand, as for the scrap guns (each arm's direction is set in its reference pose).
- **Sounds:** each gun's own shot (the Warden fires the library's iron pistol shot); the Haymaker's loop plays the shell insert, as the Thunderpipe's does.

## Slice 3: the lever set
The owner's two lever rifles and the Callwell, offered as the "lever rifles". The Callwell turned out to be an over-and-under shotgun that breaks open to load, not a lever gun; it is here as the owner chose it, as the Coach Gun.

| | Longhorn Rifle | Drover Rifle | Coach Gun |
|---|---|---|---|
| The owner's gun | Marlin | Winnie | Callwell |
| What it is | a heavy lever-action rifle | a lever-action rifle with a long tube | an over-and-under shotgun that breaks open |
| Fires | one shot, then the lever is worked | the same | one shot each pull, 8 pellets |
| Damage | 8 | 6.5 | 3 a pellet (24 if all land) |
| Rate | 1.5 a second (every 13 ticks: the lever) | 1.5 a second | 2.5 a second (every 8 ticks) |
| Holds | 6 | 10 | 2 |
| Reload, a round at a time | 0.4 s, then 0.55 s a round, then 0.7 s | 0.4 s, then 0.6 s a round, then 0.85 s | 0.6 s, then 0.65 s a shell, then 0.65 s |
| Spread, hip / aimed | 1.5° / 0.15° | 2° / 0.3° | 6° / 4° |
| Range | 120 blocks | 100 | 32 |
| Round | Rifle Round | Rifle Round | Buckshot Shell |

**What you see:** after each shot the rifles' levers swing down and back, and the gun cants as the lever is worked. The Longhorn's left hand carries each cartridge to the receiver. The Coach Gun's barrels tip open on their hinge for loading and snap shut.

**Crafting** (a crafting table):
- **Longhorn Rifle:** two iron ingots and a brass ingot over planks and a lever.
- **Drover Rifle:** two iron ingots and a brass ingot over a lever and planks.
- **Coach Gun:** two iron ingots over a brass ingot, a lever and planks.

**Connections and balance:** the same metals as the iron set; no new round or material. The rifles are the long-range option: the Longhorn's 8 a shot at 120 blocks against the Patchwork Carbine's 6 at 96, at under half the carbine's rate. The Coach Gun sits between the Thunderpipe and the Haymaker. Starting numbers.

**How the models were built** (`tools/guns.py`): the lever loops and the Callwell's barrels are not separate parts in the owner's files, so a bone's part can now be some of a part's elements:

| Gun | Bones |
|---|---|
| Longhorn Rifle | `gun_body` (main but its 8th element, standard barrel, sights); `lever` (main's 8th element: the flat lever loop), turning about (8, 2, 12.7); `bolt`; `shell` (a drawn cartridge) |
| Drover Rifle | `gun_body` (main but its 11th element, standard barrel, sights, hammer); `lever` (main's 11th element), turning about (8, 2.3, 12.9); empty `bolt` and `shell` (its animations never show the round) |
| Coach Gun | `gun_body` (main's elements the owner named "main"); `barrel` (those named "barrel": both barrels, the rib, the bead and the fore-end), tipping about the hinge (8, 1.9, 11.6); empty `bolt` and `shell` |

- **The cartridge:** a 1 × 1 × 3 px brass case with a lead tip, drawn into an empty corner of the Longhorn's atlas copy. Its rest place keeps it beside the left hand while the hand carries it in (2 to 3 px off through the carry), and puts it at the receiver's side as it goes in.
- **Hands:** the right hand on each wrist, low on it, (8, 1.7 to 1.9, 16.4). Placed at the wrist's middle, the fist reached up to the sight line when aiming and hid the target in a first-person preview. The left hands are on the fore-ends.
- **Sounds:** the rifles' lever cue plays the library's lever sound (a new shared event, `guns.lever`); each loop's insert cue plays the shell insert. Shots: the Longhorn the library's heavy rifle shot, the Drover the cowboy rifle's, the Coach Gun the brass shotgun's.

## Slice 4: the black powder guns
Muzzle-loaders: one heavy shot, then a long reload (bite the cartridge, pour, drop the ball, ram it home). They are the cheapest guns, iron, wood and a flint, so they also make an early way in.

| | Duelling Pistol | Line Musket | Bellmouth |
|---|---|---|---|
| The owner's gun | Flintlock Pistol | Musket | Blunderbuss |
| What it is | a flintlock pistol, held in one hand | a long flintlock musket | a flintlock blunderbuss with a flared muzzle |
| Fires | one shot | one shot | one shot, 10 balls |
| Damage | 9 | 14 | 2.5 a ball (25 if all land) |
| Holds | 1 | 1 | 1 |
| Reload | 3.7 s | 3.9 s | 3.9 s |
| Spread, hip / aimed | 4° / 2° | 2.5° / 0.75° | 12° / 9° |
| Range | 32 blocks | 64 | 20 |
| Round | Paper Cartridge | Paper Cartridge | Paper Cartridge |

**The round:** the **Paper Cartridge**, a lead ball and its powder in paper: paper, a lead nugget and gunpowder in a column make 4. A new round, since none of the others suits a muzzle-loader; its icon is a map (`tools/item_icons/paper_cartridge.txt`, canvas and iron).

**Crafting** (a crafting table): the Duelling Pistol, two iron ingots and a flint over planks; the Line Musket, three iron ingots over a flint and two planks; the Bellmouth, a copper ingot and two iron ingots over a flint and two planks.

**What you see:** the owner's animations: the hammer falls and a flash of priming fire jumps from the pan; on the reload the gun tips up, the ball goes down the muzzle and the ramrod drives it home in strokes. The Duelling Pistol is held in one hand; the left comes in to load.

**How the models were built:**
- **Hammer:** the owner's part, a flat cock on the lock's right side, on the `hammer` bone, turning about its foot.
- **Ball, ramrod and flash:** the animations move them on bones that had no parts. They are drawn here, each one box in an empty corner of the gun's atlas copy (the Musket's flash is the owner's own part):
  - a 1 px lead ball, resting where the reload's first hold puts it at the muzzle;
  - a half-pixel iron ramrod with a brass tip (6, 12 and 10 px long), resting where the reload's farthest reach puts its back end at the muzzle, in line with the bore, so its strokes drive it in;
  - a 1 px priming flash at the pan.
- **Shown only while moved:** the renderer now hides every such prop (shell, ball, ramrod, flash) unless an animation is moving it, as it already did the Thunderpipe's shell. The idle, which leaves them alone, would otherwise show them at rest.
- **The Duelling Pistol's left hand** (hidden by its idle) is placed at the muzzle, holding the ball, 0.71 s into the reload.
- **The Bellmouth's left arm** hangs from a `left_arm2` bone, which its reload slides back 5.75 px.
- **Sounds:** each gun fires the library's black powder shot. The reload cues `insert`, `metal` and `jam` play the library's insert and metal sounds (new shared events: `guns.insert`, `guns.metal`, `guns.jam`).
- **Known limit:** at about half a second into the Duelling Pistol's reload, its left hand comes close to the view with the pistol tipped up, and the arm covers much of the screen (first-person preview) for under half a second. Play will tell whether it wants a different anchor for that hand.

**Balance:** starting numbers. The Line Musket hits hardest of any gun so far (14) but needs 3.9 s to reload; the Bellmouth's 25 at point blank falls off fast.

## Slice 5: the attachments
The owner's parts include, for most guns, a set of attachments drawn to fit that gun: silencers, a muzzle brake, an extended barrel, magazines, stocks and grips. Slice 5 makes each kind one item that fits every gun with such a part, and shows it on each gun as that gun's own part.

**The attachments** (each the owner's item model, with its texture; a gun takes one in each slot):

| Attachment | Slot | Effect |
|---|---|---|
| Silencer | barrel | damage −5%, shot sound −65% |
| Baffled Silencer | barrel | shot sound −80% |
| Muzzle Brake | barrel | aimed spread −15%, kick −50% |
| Extended Barrel | barrel | range +30%, hip spread −15%, aimed spread −15% |
| Extended Magazine | magazine | rounds +50%, reload time +15% |
| Speed Magazine | magazine | reload time −35% |
| Light Stock | stock | hip spread −15% |
| Weighted Stock | stock | aimed spread −30%, kick −40% |
| Wooden Stock | stock | hip spread −10%, aimed spread −15%, kick −25% |
| Light Grip | grip | hip spread −20% |
| Vertical Grip | grip | kick −35% |

The silencers and the muzzle brake sit at the muzzle in front of the barrel; the Extended Barrel takes the barrel's place, the magazines the magazine's, and on the Thunderpipe a stock takes the place of its pistol grip (the owner's stocks carry their own grip). The grips go under the fore-end.

**Which guns take which** (where the owner made the part for the gun, on the gun's own texture):

| Gun | Barrel | Magazine | Stock | Grip |
|---|---|---|---|---|
| Rust Midge | Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel | Extended Magazine, Speed Magazine | Light Stock, Weighted Stock, Wooden Stock | – |
| Patchwork Carbine | Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel | Extended Magazine, Speed Magazine | Light Stock, Weighted Stock, Wooden Stock | Light Grip, Vertical Grip |
| Thunderpipe | Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel | – | Light Stock, Weighted Stock, Wooden Stock | Light Grip, Vertical Grip |
| Warden Pistol | Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel | Extended Magazine, Speed Magazine | – | – |
| Riveter SMG | Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel | Extended Magazine, Speed Magazine | Light Stock, Weighted Stock, Wooden Stock | – |
| Haymaker | Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel | – | – | – |
| Longhorn Rifle | Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel | – | Light Stock, Weighted Stock, Wooden Stock | Light Grip, Vertical Grip |

The Drover Rifle, the Coach Gun and the muzzle-loaders took none in slice 5: the owner drew their attachments on textures shared between guns (`carabine_grips`, `flintlock_stocks`, `musket_bayonets` and others), and a GeckoLib model draws from one texture. Slice 7 packs those into each gun's atlas and adds the bayonets ([below](#slice-7-bayonets-and-the-shared-texture-guns)), and slice 7b the scopes ([below](#slice-7b-the-scopes)); the tactical grip waits for a later slice.

**Fitting and taking off** (a crafting table, or the 2 × 2 grid):
- A gun and an attachment it takes, alone in the grid, give the gun with the attachment fitted. The rounds loaded stay. An attachment the gun wore in that slot comes off and stays in the grid where the new one lay.
- A gun with attachments and shears give the gun without the attachment fitted last; that attachment stays in the grid where the gun lay, and the shears are kept.
- The recipe book does not list these (they are special recipes, as the Skeleton Key's copying); each attachment's tooltip says how, and names the guns it fits.
- A gun whose larger magazine comes off keeps the rounds it held beyond its own magazine until they are fired; it will not reload until it is below its own.

**What you see:** the attachment on the gun, in your hands, in other players' hands and in the inventory; it moves with the part it is fixed to (a magazine drops out with the reload, the Haymaker's silencer rides its pump). A Speed Magazine's change plays the owner's reload faster, an Extended Magazine's a little slower, so the animation ends when the reload does. The tooltips list each attachment's effects, green where it helps and red where it costs, and a gun's tooltip its attachments and its numbers with them.

**Crafting** (a crafting table):
- Silencer: an iron ingot, wool, an iron ingot in a row; the Baffled Silencer: a brass ingot either side of a Silencer.
- Muzzle Brake: an iron nugget, a brass ingot, an iron nugget in a row; Extended Barrel: two iron ingots and a brass ingot in a row.
- Extended Magazine: an iron ingot over a brass ingot over an iron ingot; Speed Magazine: a brass ingot over a slime ball over a brass ingot.
- Light Stock: two sticks and leather in a row; Weighted Stock: two planks and an iron ingot; Wooden Stock: two planks and leather.
- Light Grip: leather over a stick; Vertical Grip: an iron ingot over a stick over leather.

**How the models were built** (`tools/guns.py`, `effective_bones()`): each gun's model gains, under the bone holding a slot's standard part (or under `gun_body`), an `att_<id>` bone for each attachment it takes, with that bone's pivot, so the attachment rides the barrel or magazine as the animations move them. Where an attachment replaces the standard part, the part moves to a `std_<slot>` bone of its own. The Warden Pistol's magazine slot is on two bones (the magazine and the one its reload brings in), so it has a second set suffixed `_2`. Every attachment part is re-baked and compared face for face with the owner's, as the guns' own parts are; the props' atlas corners are checked to stay clear of every attachment's texture. The renderer shows a fitted attachment's bone and hides a replaced part's; nothing else in the model or the animations changes.

**Connections:** iron, brass, wool, leather, sticks, planks and a slime ball, all early. Each attachment is a choice, not a strict upgrade: the Silencer costs a little damage, the Extended Magazine reload time; the slots keep a gun from stacking two of a kind.

**Balance:** starting numbers. The largest changes: a Riveter SMG with an Extended Magazine holds 45; a Patchwork Carbine with an Extended Barrel reaches 125 blocks; a Weighted Stock and a Muzzle Brake together leave 30% of the kick.

**Known limits:** the left hand stays where the owner's animations put it, so it does not move onto a vertical grip; a silenced shot is quieter, not shorter-ranged: other players within 16 blocks still hear it, softly (no gun's shot is sent to players farther away than that).

## Slice 6: the guns in use
Slice 6 adds no items. It makes a shot look like one, and a gun look held, using the owner's flash and casing art and the cues the owner put in the animations.

**Muzzle flash:**
- Each shot shows one of the owner's four flash frames (`Big Cannons and Mounted Guns/textures/muzzleflash*.png`) at the gun's `muzzle` locator, full bright, for two ticks. It swells a little and fades.
- Each shot picks its frame and its turn about the barrel from its time, so a burst flickers.
- The frame faces back down the barrel, so the shooter sees a star from the hip. Two copies cross along the barrel, so a player beside them sees the flash too.
- **Size,** in the model's pixels, by the round: light 5, rifle 7, buckshot 8, paper cartridge 10 (black powder flares widest).
- **Barrel attachments:** a Muzzle Brake or an Extended Barrel moves the flash to its own front (a `muzzle_<attachment>` locator, on the bore at the front of the part). A Silencer or a Baffled Silencer hides it.
- **Black powder:** a muzzle-loader's shot also blows a white cloud in front of the muzzle.
- **Who sees it:** everyone near the shooter. The player's own shots show as they fire; other players' shots show when the server's `GunActionPayload` arrives.

**Spent casings:**
- The owner's animations cue `eject_casing` where a case comes out: at the start of the shot for most guns, and as the Coach Gun breaks open to load. The Drover Rifle's also cues one as its loading ends. The Line Musket has none.
- At the cue, the round's case flies out of the ejection port to the gun's right and a little up. It tumbles, falls and lies where it lands for two or three seconds. A left-handed player's go to the left.
- **Each case** is the owner's art: a Light Round's is `small_copper_casing`, a Rifle Round's `large_brass_casing`, a Buckshot Shell's `shotgun_shell`. They are particles: `jugcraft:light_round_casing`, `rifle_round_casing` and `buckshot_shell_casing`.
- A paper cartridge leaves no case. The Duelling Pistol's and the Bellmouth's cue puffs a little smoke from the lock instead.
- The other cues (`loaded`, `end_reload`, `loop_end`, `reload_end`) mark points in a reload that the server's timing already covers, and show nothing.

**Zoom:** aimed down the sights, the view narrows, eased in and out with the aim.

| Guns | Field of view, aimed |
|---|---|
| Thunderpipe, Haymaker, Bellmouth | 92% |
| Rust Midge, Warden Pistol, Coach Gun, Duelling Pistol | 90% |
| Riveter SMG | 88% |
| Patchwork Carbine, Line Musket | 82% |
| Drover Rifle | 80% |
| Longhorn Rifle | 75% |

It multiplies vanilla's own modifier (sprinting, a speed effect), and the camera smooths it as it does vanilla's. Scopes (a later slice) will narrow it further.

**Seen from outside:**
- The gun arm comes up along the look, so the gun points where the player looks. The owner's third-person transforms are made for a raised arm: hanging at the side, the gun pointed at the ground.
- For a gun held in both hands, the other arm comes across to the fore-end, as vanilla holds a loaded crossbow.
- The Warden Pistol, the Haymaker and the Duelling Pistol are one-handed (their idle hides the left arm). The other arm stays as vanilla poses it.
- The arms follow the head up and down, keep vanilla's crouch, and a left-handed player's are mirrored.
- The other arm keeps to its own work while it eats or holds up a shield.
- Not while swimming, gliding or asleep. Armor is posed from the same state, so it follows.

**How:**
- `client/guns/GunEffects`: the shots, the casings and the smoke.
- `GunFlashLayer`: a GeckoLib render layer at the muzzle locator's bone.
- `GunCasingParticle`: the cases.
- `GunLooks`: each gun's hold and zoom, the flash sizes, and the attachments that hide the flash.
- `GunPose`: the hold. It is called from the arms motion hooks (`ArmsRenderStateMixin`, `ArmsHumanoidModelMixin`) that already pose players.
- `GunView.fov`: the zoom.
- **One new client mixin,** `GunFovMixin`:
  - It multiplies what `AbstractClientPlayer.getFieldOfViewModifier` returns; Fabric API has no field of view event.
  - It takes no arguments and is optional (`require = 0`): a renamed method would load without the zoom, and the client game test would fail and say so.
- **`tools/guns.py`:** the numbers (`ZOOM`, `FLASH_SIZE`, `CASINGS`, `two_handed()`, `hides_flash`), the locators and the art copies. `check_guns` holds the Java to them.

**Server and saves:** nothing changes on the server, and nothing is saved. The flash, casings, smoke, zoom and pose are drawn by each client from what it already knows. The three casing particle types are registered on both sides, as particle types must be.

**Known limits:**
- Aimed down the sights, the gun's own body stands between the eye and the muzzle and hides most of the flash. The black powder guns' bigger flash and cloud still show around it (CI screenshots below).
- The flash's timing comes from the shot, not from the animation, so a remote player's flash shows when their shot reaches you.
- Cases do not bounce or roll; they stop where they land.
- The zoom does not follow the field of view effects slider, as the spyglass's does not.

## Slice 7: bayonets and the shared-texture guns
Slice 7 finishes the attachments the owner modelled for these twelve guns, except the scopes.

**Bayonets** (the grip slot, now called "under-barrel": a gun has a bayonet or a grip, not both):

| Bayonet | Stab | Crafting |
|---|---|---|
| Iron Bayonet | 4 | an iron ingot over an iron nugget |
| Steel Bayonet | 5 | a steel ingot over an iron nugget |
| Diamond Bayonet | 5.5 | a diamond over an iron nugget |
| Netherite Bayonet | 6 | a Diamond Bayonet, a netherite ingot and a netherite upgrade template, at a smithing table |

Stab is in half hearts. On 19 October 2026 the Diamond Bayonet went from 5 to 5.5 (a provisional figure for the owner to confirm), because it stabbed no harder than the Steel Bayonet; `eachBayonetStabsHarderThanTheTierBelow` now checks that every tier beats the one below.
Fork [run 39303243194](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39303243194) failed the material data check because `tools/guns.py` still said 5; with it matched, [run 39311203238](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39311203238) on 6aa2a5fbf was green: the data checks, the build, every server game test including the new one, and all eight client shards.

- The owner's art for each: the item icon, and the blade on each gun that takes one. The owner's fourth bayonet is anthralite, a Scorched Guns metal Jugcraft does not have. Its grey blade and wrapped grip read as steel, so it is the Steel Bayonet (a proposed name).
- **Which guns:** the Patchwork Carbine, Thunderpipe, Longhorn Rifle, Drover Rifle, Coach Gun, Line Musket and Bellmouth take all four. The pistols, the SMG and the Haymaker have no bayonet parts.
- **Stabbing:** V (the "Stab with bayonet" key) thrusts the gun forward and back. The server strikes the nearest creature along the look within the player's reach, stopping at blocks:
  - for the bayonet's damage, as a melee blow, with a sword's push;
  - at most once every 12 ticks, and not while reloading;
  - past the same ally, mount and protection checks a bullet uses.
- **Others see it:** the server tells the clients that see the stabber (`GunActionPayload.STAB`); they thrust the stabber's arms forward.
- Bayonets change none of the gun's numbers. The owner's animations have no stab, so the thrust is drawn in code: the gun driven 0.35 blocks forward on screen and back over 6 ticks, and the arms 4 pixels forward seen from outside.

**The shared-texture guns:** the owner drew some guns' attachment parts on textures shared between guns. GeckoLib draws a model from one texture, so slice 5 left them out. `tools/guns.py` now builds each gun one atlas:
- the gun's own texture, plus the shared textures its parts use;
- packed into room the gun's own texture leaves free: clear blocks that no face's UVs and no prop reach (`atlas_layout()`, `own_footprint()`);
- a 64 px atlas grows to 128 to make room. Every atlas stays a square of 128 or less, the texture rule for packed textures.

| Gun | Now takes | Its atlas |
|---|---|---|
| Drover Rifle | Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel, Light Grip, the bayonets | 128, with `advanced_silencer`, `carabine_grips`, `greaser_smg_barrels` and `makeshift_rifle_bayonets` in its free room |
| Coach Gun | Light Grip, the bayonets | 128, with `carabine_grips` and `makeshift_rifle_bayonets` |
| Duelling Pistol | Light, Weighted and Wooden Stocks | 64 grown to 128, with `flintlock_stocks` |
| Line Musket | the three stocks, Light and Vertical Grips, the bayonets | 64 grown to 128, with `musket_bayonets`, `musket_stocks` and `musket_grips` |
| Bellmouth | Light and Vertical Grips, the bayonets | 64 grown to 128, with `musket_bayonets` and `musket_grips` |

- Each face's UVs are moved into the atlas from its own texture, and the face check compares them that way. Every attachment part re-bakes face for face as before.
- A new check holds the written atlas to its parts: the owner's files and the props, placed as the layout says, never over a pixel the gun's own texture uses.
- The Muzzle Brake's and Extended Barrel's flash comes from their front on the Drover too (slice 6's locators).

**Connections:** iron, steel (Jugcraft's), diamond and netherite, as their tools are; the Netherite Bayonet is an upgrade, as netherite tools are. A bayonet is a choice against a grip: a melee blow against less spread or kick.

**Balance:** a bayonet stab is weaker than a sword of its metal (a sword is 6 to 8), and a stab every 12 ticks. It is a last resort, not a melee weapon.

**Known limits:**
- **No scopes in this slice:** the owner's scope models draw their lenses with reticle and vignette textures (`scguns:effect/...`) that were not in the library then, and where a scope sits on each gun is not in the library either (Scorched Guns keeps it in gun data). They came next, in [slice 7b](#slice-7b-the-scopes).
- **The tactical grip:** the Drover's and Coach Gun's `tact_grip` parts are not an item yet.
- **The thrust is code, not an animation:** the owner may want to animate a stab.

## Slice 7b: the scopes
The owner's three scopes, on a fifth attachment slot, "optic". A scope takes the place of the gun's iron sights.

| Scope | Aimed through it (first person) | Spread | Crafting |
|---|---|---|---|
| Long Scope | the view through the scope fills the screen; the view narrows to 30% | aimed −50%, from the hip +25% | a spyglass between two brass ingots |
| Medium Scope | the same, narrowing to 50% | aimed −30%, from the hip +10% | a glass pane between two brass ingots |
| Reflex Sight | the gun stays in view, its window on the middle of the screen with the red dot on it; the view narrows to 85% | aimed −15% | a glass pane over redstone over an iron nugget |

- **Which guns:** the Longhorn Rifle, Drover Rifle and Riveter SMG. These are the guns the owner made to take one: their parts include iron sights and a `no_sights` stand-in (nothing on the rifles, a rail on the Riveter). The owner's Brawler has them too: the Bulldog Pistol (slice 8) takes all three.
- **On the gun:** the owner made no scope part for each gun, so each scope's own item model is mounted on it.
  - Its mount's foot stands on the centre line, on the receiver where the iron sights stood, midway along them, or on the Riveter's rail (`optic_mount()`, `optic_foot()`; `MOUNTS` overrides a gun).
  - The iron sights hide (the slot's standard part); the Riveter's rail shows under the scope.
  - Its lens planes (the reticle and the lens rim) stay off the gun model, so from outside the tube is open.
  - The scope textures (64 px) would not fit a gun's 128 px atlas whole. They are packed piece by piece instead: each rect of pixels a scope's faces use, a pixel apart, into the room left (`scope_islands()`, `pack_islands()`). The rest of each atlas is unchanged, the Drover's shared textures included.
- **Aiming:** the scope's eyepiece slides onto the middle of the screen: its `sight_<scope>` locator, at the height of the owner's `.scmeta` camera. Near full aim (90%, `GunScope`):
  - a magnifying scope fills the screen with the view through it, as a spyglass does: the owner's reticle and lens rim on a square as tall as the screen, black beside it. The gun drops out of sight meanwhile;
  - a reflex sight puts its red dot on the middle of the screen, over the gun;
  - either way the crosshair is left out.
- **Zoom and the mouse:** in first person a scope's zoom takes the place of the gun's own (`GunView`). The mouse turns the player as much more slowly, as vanilla slows it for a spyglass (`GunMouseMixin`, which scales the mouse's gathered movement just before vanilla turns the player by it; Fabric API has no event for it).
- **Others see** the scope on the gun, in third person and in the inventory, like any attachment.

**Connections:** brass (copper and zinc), glass and redstone; the Long Scope takes a spyglass (amethyst and copper), so it comes a little later.

**Balance:** a choice, not an upgrade: the two magnifying scopes are clumsier from the hip, and the optic slot holds one. Starting numbers for the owner.

**Save compatibility:** new items `jugcraft:long_scope`, `medium_scope` and `reflex_sight`. The attachments component now holds up to five ids (one a slot); saved guns with up to four load as before. `guns.enabled=false` gates the three new recipes.

**Known limits:**
- The view through a scope is drawn flat over the screen, not through the lens of the model. Scorched Guns can draw the world a second time into the lens; that doubles the drawing, so it is left out.
- The owner's finer reticle `long_scope_reticle.png` is not used: both scope models name `long_scope_reticle2`.
- The laser sight (also in the library) is not an item yet.
- With the HUD hidden (F1) there is no view through the scope, though the gun still drops away.
- Where each scope sits was read from the gun's parts, not given by the owner.

## Slice 8: the hand guns
The first of the four further gun sets the owner chose on 8 October 2026: three guns held in one hand. One breaks open for a single heavy round; two are six-shot revolvers that load a round at a time.

| | Bulldog Pistol | Marshal Revolver | Sapper Revolver |
|---|---|---|---|
| The owner's gun | Brawler | Longarm | Trenchur |
| What it is | a hand cannon that breaks open to load its one round | a long-barrelled revolver, loaded through a gate | a short revolver whose cylinder swings out to load |
| Fires | one shot | one shot each pull | one shot each pull |
| Damage | 11 | 5 | 4.5 |
| Rate | one shot, then the reload | 2.5 a second (every 8 ticks) | 3.3 a second (every 6 ticks) |
| Holds | 1 | 6 | 6 |
| Reload | 2.25 s | 0.55 s, then 1.25 s a round, then 0.55 s | 0.45 s, then 0.6 s a round, then 0.65 s |
| Spread, hip / aimed | 3° / 1° | 2° / 0.5° | 2.5° / 0.8° |
| Range | 56 blocks | 72 | 48 |
| Round | Rifle Round | Light Round | Light Round |
| Takes | the two silencers, the muzzle brake, the extended barrel and the three scopes | the three stocks | the two silencers, the muzzle brake and the extended barrel |

**Crafting** (a crafting table):
- **Bulldog Pistol:** two iron ingots and a brass ingot over planks.
- **Marshal Revolver:** two iron ingots and a brass ingot over a brass ingot, a lever and planks.
- **Sapper Revolver:** an iron ingot and a brass ingot over a lever and planks.

**What you see:** the owner's animations.
- **Bulldog Pistol:** the hammer snaps forward on the shot. To reload, the barrel tips down on its hinge, the spent case is thrown, and the left hand brings a rifle round up and drops it into the breech before the barrel snaps shut.
- **Marshal Revolver:** the gun turns its gate side up, and the left hand loads a round at a time while the cylinder turns.
- **Sapper Revolver:** the cylinder swings out to the left on its crane. The left hand loads a round at a time, a spent case dropping out each time, and the cylinder swings back.

**How the models were built:**
- **The Bulldog's hammer** is the main part's ninth element, a flat plate, on the `bolt` bone; the shot drives it forward.
- **The Bulldog's barrel** (its `stan_barrel` part) turns on the `barrel` bone about a hinge at the front of the frame's lug, where the opened barrel stands just clear of the frame. Its reload also names an `extended_barrel` bone, which it never moves: that bone is empty, and a fitted Extended Barrel rides the barrel bone.
- **The Bulldog's round** had no part. It is drawn here as a box on the `shell` bone (a brass case with a lead tip, in an empty corner of the atlas), resting where the reload's offsets bring it into the opened breech, bullet first: its middle is a pixel down the bore as it shrinks away, 1.54 s in.
- **The Marshal's cylinder** is the six elements of its main part that the owner grouped as the magazine, on the `cylinder_magazine` bone, turning about its own axis. Its `magazine` bone is left empty: the reload's last step swings that bone back as if the cylinder had swung out, which the Longarm's never does. Its hammer is the main part's last element, turning about its foot.
- **The Sapper's cylinder** (its `drum` part) swings out on the `magazine` bone about the crane's hinge, below and to the left of it, and its hammer part turns about its foot.
- **The left hands,** hidden by each idle, are placed for a reload keyframe:
  - the Bulldog's 1.17 s in, holding the round, set back toward the shoulder so the round shows past the fingers;
  - the Marshal's at the gate behind the cylinder, on the right, 0.42 s into each round;
  - the Sapper's behind the swung-out cylinder's outer chamber, 0.29 s into each round.
- **The left arms** are aimed to come up from below the screen for the whole time they show, fitted over every frame of the reload (and the revolvers' inspect), not just the keyframe. The right arms are as before.
- **Sounds:**
  - **Shots:** the Bulldog fires the library's heavier rifle shot, the Marshal the brass revolver's and the Sapper the brass pistol's. The library's plain revolver and pistol shots carry other sources' tags, so they are not used.
  - **Reloads:** their cues play the shared events.
  - **`stop_mag_tracking`,** a cue in the revolvers' reloads, shows nothing.

**Connections:** iron, brass (copper and zinc) and a lever; the rounds as before.

**Balance:** starting numbers.
- **Bulldog:** 11 in one shot is the hardest one-handed hit so far, but every shot costs a 2.25 s reload and a rifle round.
- **Marshal:** six aimed shots of 5 at long range.
- **Sapper:** six quicker, looser shots of 4.5, and a quicker reload.

**Save compatibility:** new items `jugcraft:bulldog_pistol`, `marshal_revolver` and `sapper_revolver`; nothing saved changes. `guns.enabled=false` gates their recipes as it does the others'.

**Known limits:**
- The Bulldog's hinge, its round's rest and the three left hands were fitted to the owner's animations in a first-person preview; the owner did not give them.
- The revolvers' reloads bring the gun up close to the view, and the arms cover much of the screen while they load (as the Duelling Pistol's do). Play will tell whether the hands want other anchors.
- The Marshal's cylinder turns 70° for each round and turns back, as the owner's animation has it, rather than a sixth of a turn onward.

## Slice 8B: the service arms
The second of the four further gun sets: three magazine-fed guns a step up from the iron and brass sets, made of steel, the tier where the mod's machines turn dieselpunk.

| | Sentry Pistol | Garrison Rifle | Breacher |
|---|---|---|---|
| The owner's gun | Mak MkII | Stigg | Combat Shotgun |
| What it is | a steel service pistol, held in one hand | a steel assault rifle with a curved magazine | a pump shotgun fed from a box magazine |
| Fires | one shot each pull | for as long as the trigger is held | one shot each pull, 8 pellets |
| Damage | 5 | 4 | 3 a pellet (24 if all land) |
| Rate | 4 a second (every 5 ticks) | 6.7 a second (every 3 ticks) | 1.25 a second (every 16 ticks: the pump) |
| Holds | 8 | 30 | 6 |
| Reload (a magazine) | 2.35 s | 2.65 s | 2.6 s |
| Spread, hip / aimed | 2° / 0.6° | 3° / 0.6° | 7° / 5° |
| Range | 64 blocks | 80 | 28 |
| Round | Light Round | Rifle Round | Buckshot Shell |
| Takes | the barrel attachments, both magazines, the three stocks | all of those, the light grip, the four bayonets and the three scopes | the same as the Garrison Rifle |

**Crafting** (a crafting table):
- **Sentry Pistol:** three steel ingots over a lever and a brass ingot.
- **Garrison Rifle:** three steel ingots over a brass ingot, a lever and planks.
- **Breacher:** two steel ingots over a brass ingot, a lever and planks.

**What you see:** the owner's animations.
- **Sentry Pistol:** the bolt snaps back on each shot. To reload, the empty magazine drops out of the grip and the left hand pushes a new one up into it.
- **Garrison Rifle:** the bolt rides back with each shot. To reload, the left hand pulls the magazine and seats a new one, and the bolt is worked.
- **Breacher:** each shot is followed by a pump of the fore-end, with the left hand riding it. To reload, the magazine is swapped.

**How the models were built:**
- **Gun bodies:** each turns about its grip, where the right hand holds it.
- **Bolts:** the shot moves each gun's bolt part. On the Stigg that is the receiver's two sides and the charging handle. On the Combat Shotgun it is the pump: the fore-end under the barrel and the handle beside the receiver that rides with it.
- **Magazines:** the reload drops the magazine and brings a new one in on `magazine_2`. Each turns about the point that keeps its top in the well:
  - the Mak's top;
  - the Stigg's middle;
  - the Combat Shotgun's top.
- **The left hands:**
  - **Sentry:** its idle hides the left arm. The hand is placed 1.46 s into the reload, at the base of the new magazine as it pushes it up.
  - **Garrison Rifle:** the hand sits under the handguard. Through the reload it comes within about a pixel of the magazine both where it pulls it and where it seats the new one.
  - **Breacher:** the hand sits under the pump and rides it through each shot, within 0.2 px.
- **The left arms:**
  - **Sentry:** fitted to come up from below the screen through the reload. Its inspect turns the arm so differently that one direction cannot suit both; the reload is favoured.
  - **The two long guns:** the same as the other two-handed guns.
- **Sights:** the Stigg's rear sight is its sights part, with the front post on the barrel. The Combat Shotgun's sights are a front post and a ring at the back. Both guns take the three scopes, mounted where those sights stood.
- **Aimed, the Garrison Rifle is held further out:**
  - Its receiver runs back under the line of sight to just short of the eye. Each shot slides the bolt 2.6 px back and the whole gun 1.4 px.
  - Aimed at the hip's depth, the bolt came past the eye, and an aimed shot filled the screen with the gun ([CI, below](#verification)).
  - Aimed, it is now held 4 px further out (`eye_relief` in `BUILDS`, `GunLooks.EYE_RELIEF`). In a preview of the shot, that keeps all of it at least 3.5 px from the eye.
  - The hip view is the owner's.
- **Sounds:** the shots are the library's scrapper shot (Sentry), its scorched rifle shot (Garrison) and the Combat Shotgun's own. None carries another source's tag. The reload cues play the shared events.

**Connections:** steel from the steel foundry (coke and iron, no power needed), brass and a lever. The rounds are as before.

**Balance:** starting numbers.
- **Sentry:** fewer, harder shots than the Warden Pistol.
- **Garrison:** the first automatic that fires rifle rounds. It lands about 27 a second while its 30 rounds last, and every one of them is a rifle round.
- **Breacher:** the Haymaker's punch, with a magazine reload in place of loading a shell at a time.

**Save compatibility:** new items `jugcraft:sentry_pistol`, `garrison_rifle` and `breacher`; nothing saved changes. `guns.enabled=false` gates their recipes.

**Known limits:**
- The Sentry's left arm points awkwardly at moments of its inspect.
- The tactical grip the Stigg and Combat Shotgun have parts for is not an attachment yet.
- The Combat Shotgun's extended-barrel texture (`combat_shotgun_ext_barrel.png`) is not used: its extended barrel part draws on the gun's own texture.

## Slice 8C: the heavy weapons
Three more of the owner's guns, each firing something other than a bullet or firing it differently, in steel.

| | Trench Lobber | Thresher | Stoker |
|---|---|---|---|
| The owner's gun | Hammer GL | Gattaler | Kiln Gun |
| What it is | a pump-action grenade launcher fed from a box magazine | a drum-fed rotary gun | a flamethrower |
| Fires | one Grenade each pull: the field chemistry branch's frag grenade | rifle rounds, for as long as the trigger is held, once its barrels have spun up | a burst of flame, for as long as the trigger is held |
| Damage | the Grenade's burst: 16 at its centre, less out to 4 blocks; it breaks no block | 3 | 2 a burst to each creature in the jet, and it sets them alight for 4 s |
| Rate | about 1.4 a second (every 14 ticks: the pump) | 10 a second (every 2 ticks), after ¾ s (15 ticks) of spin-up | 5 bursts a second (every 4 ticks) |
| Holds | 6 grenades | 60 rounds | 32 bursts of fuel |
| Reload (a magazine) | 2.65 s | 3.9 s | 3.05 s |
| Spread, hip / aimed | 3° / 1° (the grenade strays by about this) | 4° / 2° | the jet's half width: 10° / 6° |
| Range | a level shot carries about 24 blocks | 64 blocks | 8 blocks |
| Ammunition | Grenade (`jugcraft:grenade`) | Rifle Round | blaze powder: each is four bursts |
| Takes | both magazines, the three stocks and the three scopes | nothing | the three stocks |

**Crafting** (a crafting table):
- **Trench Lobber:** five steel ingots in a ring around a brass ingot, over a lever and planks.
- **Thresher:** five steel ingots around a piston (to turn the barrels), over two brass ingots and a lever.
- **Stoker:** two steel ingots and a flint and steel (the igniter) over a brass ingot, a lever and a bucket (the fuel tank).

**How they fire** (the server decides, `guns/GunShots`):
- **Trench Lobber:** each shot lobs a Grenade from the eye along the look, as fast as the grenade launcher throws one (2.5 blocks a tick), strayed by about the spread. It flies as a thrown grenade does and bursts where it hits, hurting living things within 4 blocks, the shooter too if they stand that close, as with the launcher. It never breaks a block.
- **Thresher:** the client says, each tick the trigger is held, that the barrels are turning (`GunSpinPayload`). The server counts the ticks itself: a shot is refused until they have turned for the spin-up, 15 ticks less 2 for packets that come unevenly, unbroken. A gap of more than 4 ticks with no word, the trigger let go, starts the count again. Players who see the gunner see the barrels spin up (`GunActionPayload.SPIN`).
- **Stoker:** each burst reaches every creature the shooter may strike (the same allies, mounts and protection rules as a bullet) that is within 8 blocks, inside the jet (the part of it nearest the jet's middle at most the spread off the look) and with no block between it and the eye. Each takes the burst's damage as fire and burns for 4 s. The damage type `jugcraft:flame` is tagged:
  - `minecraft:is_fire`: fire-proof creatures, fire resistance and Fire Protection guard against it;
  - `minecraft:no_knockback`: the jet does not push its target out of reach;
  - `minecraft:bypasses_cooldown`: each burst counts, as each bullet does.

  The flame sets no block alight.
- **Ammunition:** the Grenade is the field chemistry branch's frag grenade. Blaze powder fuels four bursts. A reload takes whole items, so topping up a Stoker one burst short takes a whole powder and the other three bursts are lost.

**What you see:** the owner's animations.
- **Trench Lobber:** each shot is followed by a pump of the fore-grip, with the left hand riding it, and the leaf sight rattles. To reload, the magazine drops out and is seated again.
- **Thresher:** the barrels spin from the moment the trigger is pulled, speeding up over the spin-up and running down over 1.5 s after it is let go. The spin comes from code, since no animation moves them. Each shot jolts the gun. To reload, the drum on its left side comes off and a new one goes on, and the carry handle is worked forward like a lever.
  - **Seen from outside,** it is carried at the hip. The owner's third-person transform tilts it 68.25° up off the arm, made for an arm hanging low; raised along the look as other guns are, it pointed at the sky. So its holder's arms hang that much lower (`GunLooks.TILT`, which `tools/check_mod_data.py` checks against the owner's transform through `tools/guns.py tilt()`), and it points along the look.
- **Stoker:** each burst throws flames along the look to about its reach, and a jet of the owner's pilot flame leaves the nozzle and shrinks away. To reload, its drum turns and a fuel can comes out of its left side and goes back.

**How the models were built:**
- **Bone names:** the owner named the parts' pieces: the Hammer GL's `bolt`, the Kiln Gun's `barrel` group and the Gattaler's `Drum` group. Each gun body turns about its grip in the right hand.
- **Trench Lobber:**
  - The bolt is its pump: a sleeve on the rod under the barrel, with a fore-grip angled down to the left. Each shot slides it back and rolls it about the rod.
  - The leaf sight flips about its hinge.
  - Its scopes ride the gun body, not the flapping sight, through a new `"mounts"` entry in `BUILDS`.
- **Thresher:**
  - The barrels turn about the bore's middle.
  - The carry handle on top is the owner's `grip`: the reload works it forward about its feet on the body's sides.
  - The left hand holds the left side of the front plate, by the barrels' root. The owner's animations rest it on the carry handle, and their reload takes it from there straight to the drum. But the owner's display carries the gun at the hip with its back by the eye, and on the handle the hand and forearm filled the screen (the first push's CI shots, [below](#verification)). From the front plate the same moves keep it below the gun.
  - The right hand holds the rear grip low.
- **Stoker:**
  - The two tubes and their collar are the `barrel` group. They hinge up for a shell-at-a-time reload the Stoker does not use, since it loads by the can.
  - The wide drum is the owner's `cylinder_magazine`.
  - The owner's parts have no can, so the magazine bone carries one: a brass can with steel ends, 3 × 3 × 2.6 px, drawn into a free corner of the atlas. It rests inside the drum, out of sight, until the reload pulls it out.
- **The flame:**
  - The shoot animation moves a `flame` bone 5.5 px back at twice its size and then out, shrinking.
  - It carries a 4 × 4 × 8 px box at the nozzle, its back end at the bone's pivot. The four long faces show the owner's pilot flame (`spitfire_flame.png`, its first frame), each with the tip forward; the ends are open.
  - GeckoLib runs a face's u the other way on a box's east face than on its west, and its v the other way on the top than on the bottom. So the flame's faces each get their own block of the atlas corner.
  - It shows only while a shot moves it, as the other props do.
- **The arms:** fitted in a first-person preview so that, idle, they leave the screen in the same directions as the service arms' do.
  - **By the eye:** an arm that comes within a tenth of a block of the eye is left out for those frames (`GunArmsLayer`, for every gun), as vanilla leaves out a thrown item just leaving the eye.
  - **Why:** the owner's animations were made for another mod's arms, and a few bring a hand so near the eye that the arm running from it toward its shoulder reaches the camera. In a later CI run ([run 37996214844](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37996214844), 9 October 2026), the Lobber's pump brought its left sleeve to the camera at the moment of the shot, and the sleeve filled the left half of the screen. Sweeping the preview over every gun's animations finds a few such moments in the earlier slices too, such as the Garrison Rifle's and Patchwork Carbine's inspections and the Marshal Revolver's reload.
- **Sights:**
  - **Trench Lobber:** aiming looks through the leaf sight.
  - **Stoker:** aiming looks over the top of its body, near the front.
  - **Thresher:** none. The owner's display carries it at the hip, its back by the eye. In the preview, sliding any point of it onto the middle of the screen brought the grip across the eye. Aimed, it is steadier (half the spread) and the view narrows a little, but it is not moved (`"sight": None`).
- **Sounds:**
  - **Shots:** the library's grenade launcher shot (Lobber), the new rifle shot (Thresher) and the short flamethrower burst (Stoker).
  - **Not used:** the library's machine gun and second heavy rifle shots both carry another sound pack's copyright tags (Magic Sound Effects, "Real Guns Sound Pack 2"), so neither is used. The grenade launcher shot's tags name an artist (`ARTIST=Cameron`, FL Studio 20); the new rifle and flamethrower sounds carry only a date or editor tag.
  - **New shared events:** the Lobber's pump (`pump`, `pump_half`).
- **Not used from the owner's files:** the Kiln Gun's shell-at-a-time reload animations (the Stoker loads by the can) and the `flame.json` model in the library. That model credits another author and draws vanilla's fire texture.

**Connections:**
- **Steel:** from the steel foundry.
- **Grenades:** from the field chemistry branch (`PetroItems.GRENADE`).
- **Blaze powder:** from the Nether's blazes.
- **Other parts:** a piston, flint and steel, and a bucket from vanilla.

**Balance:** starting numbers.
- **Trench Lobber:** an area weapon. Its burst is the Grenade's, so it is no stronger than a thrown grenade, only further and faster to aim.
- **Thresher:** the most rounds a second of any gun, about 30 damage a second, at the cost of the spin-up and of rifle rounds.
- **Stoker:**
  - **Damage:** about 10 a second to everything in its short cone, and fire.
  - **Fuel:** a powder is four bursts (0.8 s), so a full tank is 8 powders, about 4 blaze rods, for 6.4 s of flame.
  - **Fireproof foes:** Nether creatures that are fireproof shrug it off.

**Save compatibility:** new items `jugcraft:trench_lobber`, `thresher` and `stoker`; the damage type `jugcraft:flame`; nothing saved changes. `guns.enabled=false` gates their recipes.

**Known limits:**
- **Grenades:** the Lobber fired frag Grenades only. Since slice 9G it loads the chemical grenades too, one kind a magazine ([below](#slice-9g-the-trench-lobbers-grenades)).
- **Arms:** the Thresher's left hand holds the front plate, not the carry handle the owner's animations rest it on (by the eye at the hip, there it filled the screen). So in the reload it drops toward the drum without taking it, and the handle is worked forward without it.
- **The Stoker is held close:** the owner's first-person transform holds its back by the eye, so it fills the lower right of the screen. It is left as the owner made it.
- **Flame:** the flame is lit by the world's light like the rest of the gun, so at night it is darker than a flame should be.
- **Not played:** none of it has been played yet. The jet's reach and width, the spin-up and the kick of ten shots a second want play to set.

## Slice 8D: the energy weapons
Three more of the owner's guns, past steel, that run on charge from the energy system.

| | Beam Pistol | Stormlock Rifle | Linesman |
|---|---|---|---|
| The owner's gun | Raygun | Teslock Rifle | Arc Worker |
| What it is | a ray pistol held in one hand, that breaks open to load | a coil rifle loaded a charge at a time, with a lever under its grip | a short-range arc thrower loaded a cell at a time |
| Fires | a beam through every creature in its line, to the first block | a bolt that leaps from its mark to two more creatures close by | arcs that find the creatures in front of it and leap between them, for as long as the trigger is held |
| Damage | 6 to each creature in the beam | 9 to its mark, then 5.4 and 3.24 | 4 to its mark, then 2.4 and 1.44 |
| Rate | 2.5 a second (every 8 ticks) | about 1.4 a second (every 14 ticks) | 3.3 a second (every 6 ticks) |
| Holds | 8 shots | 5 charges | 6 charges |
| Reload | 2.4 s, broken open | 0.9 s to open, 0.85 s a charge, 1 s to close | 0.4 s to open, 0.65 s a cell, 0.6 s to close |
| Spread, hip / aimed | 1.5° / 0.5° | 2° / 0.5°, the cone its arc seeks in | 15° / 10°, the cone its arc seeks in |
| Range | 48 blocks | 64 blocks | 12 blocks |
| Charge a round | 400 JE | 750 JE | 250 JE |
| Takes | the three stocks | both grips, the four bayonets and the three scopes | the three stocks and the three scopes |

**The Energy Cell** (`jugcraft:energy_cell`), their ammunition:
- **Charge:** it holds 10,000 JE, twice or three times that with capacity modules, and stacks alone, as the powered tools do. Its bar shows its charge in amber, and its tooltip the JE.
- **Charging:** hang it on a Charging Station (steel tier). It fills at the station's rate: up to 512 JE a tick, 256 on copper cable, so in 20 to 40 ticks (1 to 2 s).
- **Looks:** charged, it is the owner's glowing cell (`energy_cell.png`, its three frames run by an `.mcmeta`); spent, the owner's empty cell (`empty_cell.png`).
- **Reloading:** a reload pools the charge of every cell in the inventory and draws each round's charge from them in inventory order; the cells stay. A full cell is 25 of the Beam Pistol's shots, 13 of the Stormlock's charges, 40 of the Linesman's. With every cell spent a reload does not start, and says so ("Your Energy Cells are spent. Fill them at a Charging Station."). The gun's counter shows the shots its cells hold.
- **Only the guns draw on it.** It is not the portable battery `docs/MACHINE_ROADMAP.md` plans.

**Crafting** (a crafting table; the recipes need both the guns and the machines switches, since without the Charging Station a cell never fills):
- **Energy Cell:** one from a copper cable over two glass panes either side of a redstone, over a brass ingot.
- **Beam Pistol:** two steel ingots and an amethyst shard (its lens) over an advanced circuit and a brass ingot.
- **Stormlock Rifle:** two steel ingots, a lightning rod (its forked emitter), copper cable (its coil), an advanced circuit, a brass ingot and planks (its stock).
- **Linesman:** a lightning rod, two steel ingots, two copper cables, an advanced circuit and two brass ingots.

**How they fire** (the server decides, `guns/GunShots`):
- **Beam:** from the eye along the look, strayed by the spread, to the first block within range. Every creature in its line that the shooter may strike (the same allies, mounts and protection rules as a bullet) takes the damage; it passes through them all.
- **Arc:**
  - It leaps to the creature nearest the aim: within range, at most the spread off the look and with no block between it and the eye. Of two equally near the aim, it takes the nearer.
  - From each creature it strikes, it leaps to the nearest other within 4 blocks with no block between them, at most twice. Each takes 60% of the damage before it.
  - Finding none, it strikes the first block along the look, harmlessly.
  - Other code is asked whether the shooter may strike a creature (`AttackEntityCallback`) only for the creatures the arc would strike, in turn.
- **Damage type** `jugcraft:zap`, tagged:
  - `minecraft:no_knockback`: the Linesman's quick arcs keep their mark in reach;
  - `minecraft:bypasses_cooldown`: each shot counts, as each bullet does.

  It is neither a projectile nor fire: Protection guards against it, Projectile and Fire Protection do not.
- **Blocks:** neither a beam nor an arc touches a block.

**What you see:** the owner's animations.
- **Beam Pistol:** each shot jolts it. To reload, the left hand tips the barrel down on its hinge, to 140°, a new cell goes in and the barrel snaps shut.
- **Stormlock Rifle:** to reload, a latch flips up off the cylinder, the cylinder turns a charge at a time, and the lever under the grip is worked down and back to close it.
- **Linesman:** each loop of its reload brings a cell in from the left hand and down into the lower battery tube; its bolt slides back and closes.
- **The shots:**
  - Every client that sees the shooter, the shooter's own too, is told where the shot went (`GunTracePayload`).
  - A beam is cyan light from the muzzle to its end; an arc is electric sparks jagging from the muzzle to each creature it leapt to.
  - The muzzle flash is the owner's frames tinted cyan-white, and the Beam Pistol's and Linesman's casing cues vent sparks.
- **Sounds:** the library's ray gun shot (Beam Pistol), its shock shot (Stormlock) and its short laser shot (Linesman). The charges going in play the insert sound.

**How the models were built:**
- **Beam Pistol:**
  - **Barrel:** the main part's 4th to 9th elements: the bore, its rings, the rod and the emitter's plates. It hinges at the bottom of its back end.
  - **Shell bone:** empty; the owner's animations keep it at scale 0.
  - **Left hand:** held in one hand, so its hand point is where it takes the barrel, 0.29 s into the reload.
  - **Aiming:** the coil at its back stands out either side of it, between its sight and the eye, so aimed it is held 4 px further out (`"eye_relief"`).
- **Stormlock Rifle:**
  - **Cylinder:** the owner's `mag`, four elements, turns about the bore.
  - **Latch:** two elements, hinged at its front, flips up. Hinged at its back, it swung down into the body.
  - **Lever:** the loop under the grip, the owner's `lever`, turns about its front. The right hand holds the wrist through the loop.
  - **Sights:** the owner's sights part, which a scope replaces.
- **Linesman:**
  - **Bolt:** the owner's `Bolt` (capital B), the block over its battery tubes.
  - **Cell:** the reload's cell is a prop, 1.5 × 1.5 × 3 px: a steel cap, the green glass and a copper cap, in the owner's cell art's colours. It rests behind the lower tube's mouth, which the loop's last move puts it in as it shrinks away.
- **Shared textures:**
  - The Beam Pistol's stocks draw on `raygun_stocks.png`, packed whole into its atlas.
  - The Linesman's stocks draw on the Rust Midge's whole 128 px atlas, which can never sit beside its own. `tools/guns.py` packs just the pixels those stocks use, as it does a scope's.
- **The arms:** fitted in a first-person preview so that they leave the screen as the other guns' do.

**Connections:**
- **The energy system:** the Energy Cell is a `Chargeable` item (`jugcraft:energy`, in JE), filled at the Charging Station from any generator, battery bank and cable network. There is no second power system.
- **Parts:** steel from the steel foundry; advanced circuits from the circuit assembler; copper cable.
- **Vanilla:** amethyst, lightning rods, redstone and glass.

**Balance:** starting numbers.
- **The trade:** the energy weapons hurt less a shot than the steel guns, but need no rounds crafted: a cell refills for the cost of the power. Their shots also do what bullets do not: a beam hits every creature in a line, and an arc finds its mark and spreads.
- **Beam Pistol:** 15 damage a second to one creature, more through a line.
- **Stormlock Rifle:** about 13 a second to one creature; 17.6 a shot across three.
- **Linesman:** about 13 a second to one creature, 26 across three, close in and without fine aim.
- **Power:**
  - A full cell is 10,000 JE: 25 Beam Pistol shots, 13 Stormlock charges or 40 Linesman charges.
  - A Battery Box (400,000 JE) fills forty cells.
  - For comparison, the power bow's shot is 500 JE and the power katana's blow 1,000.

**Save compatibility:** new items `jugcraft:beam_pistol`, `stormlock_rifle`, `linesman` and `energy_cell`, and the damage type `jugcraft:zap`. The cell's charge is the shared `jugcraft:energy` component. Nothing saved changes. `guns.enabled=false` or `machines.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **Particles:** a beam or arc is drawn from about where the muzzle is (ahead of the eye, a little right and down), not from the model's muzzle locator, so in first person it starts a little off the gun.
- **Charging:** cells charge one at a time, on the station's cradle. Slice 10E's [Cell Rack](#slice-10e-the-cell-rack) charges six at once.
- **The Beam Pistol's coil** stands out either side of its back in first person, as the owner's model has it.
- **Aimed** (the CI screenshots of 9 October): the Beam Pistol's back rises a little past the crosshair, over the target's middle, and the Linesman's broad back fills the lower middle of the view. It is the shared aiming polish item ([below](#rollout-and-open-questions)), larger on these two.
- **Not played:** none of it has been played yet. The arcs' reach, their seeking cones and the JE costs want play to set.

## Slice 9A: the marksman rifles
The first of the four gun sets the owner chose on 10 October 2026: three semi-automatic rifles in steel, the steadiest aimed and the farthest reaching of the guns.

| | Picket Rifle | Ranger Rifle | Kestrel Rifle |
|---|---|---|---|
| The owner's gun | M3 Marksman | MK43 Rifle | Whistler |
| What it is | a steel marksman's rifle with a peep sight on a short rail | a heavy semi-automatic rifle, its handle lifted and drawn back to reload | a copper-bright rifle loaded from the top with a clip |
| Fires | one shot each pull | one shot each pull | one shot each pull |
| Damage | 8 | 10 | 9 |
| Rate | 2.5 a second (every 8 ticks) | 2 a second (every 10 ticks) | 2.2 a second (every 9 ticks) |
| Holds | 10 | 10 | 8, a clip |
| Reload | 2.15 s, a magazine | 2.25 s, a magazine | 2.75 s, a clip |
| Spread, hip / aimed | 2° / 0.1° | 2.5° / 0.15° | 2° / 0.15° |
| Range | 128 blocks | 120 | 128 |
| The view aimed | narrowed to 0.7 | 0.75 | 0.7 |
| Round | Rifle Round | Rifle Round | Rifle Round |
| Takes | the barrel attachments, both magazines, the light grip and the four bayonets | both magazines, the three stocks, the light grip and the four bayonets | the barrel attachments, the three stocks, the light grip, the four bayonets and the three scopes |

**Crafting** (a crafting table):
- **Picket Rifle:** three steel ingots over a steel ingot, a lever and a brass ingot.
- **Ranger Rifle:** three steel ingots over a brass ingot, a lever and planks, with planks under the lever for its stock.
- **Kestrel Rifle:** three steel ingots over a lever between two copper ingots.

**What you see:** the owner's animations.
- **Picket Rifle:** its charging handle, on the right of the receiver, snaps back with each shot. To reload, the left hand swings the magazine down and back out of the well and seats a new one, and the handle is worked.
- **Ranger Rifle:** the handle along the right of its top cover rides back with each shot. To reload, the gun is rolled over, the handle is lifted upright and drawn back, the magazine is changed, and the handle is slammed home.
- **Kestrel Rifle:** each shot kicks it back and rolls it a little. To reload, the bolt is drawn back and held, a clip goes in from the top, and the bolt runs home. It throws no casings: its animations cue none.
- **Sounds:** the library's sniper shot (Picket), its old rifle shot (Ranger) and its iron rifle shot (Kestrel). None carries a tag naming another source. The library's revolver shot carries the same sound pack's copyright tags as its machine gun shot (Magic Sound Effects), so it is not used either. The reload cues play the shared events.

**How the models were built:**
- **Gun bodies:** each turns about its grip, where the right hand holds it: the middle of the grip, as on the Garrison Rifle.
- **Picket Rifle:**
  - **Bolt:** the owner's bolt part, the charging handle.
  - **Magazine:** turns about its top, in the well.
  - **Sights:** a peep, the sights part's ring at the back of its rail; the front post is on the barrel. The owner made it no stand-in for its sights (no `no_sights` part), so it takes no scope.
- **Ranger Rifle:**
  - **Handle:** the main part's 31st to 34th elements, a bar along the right of the top cover on a leg at its front. It turns about the foot of the leg and rides the bolt bone, which holds nothing else: the shot slides the bolt back, and the reload lifts the handle, then draws the bolt.
  - **Sights:** the main part's notch on the top cover and post on the rib. Like the Picket, it takes no scope.
  - **Empty bones:** its animations also move a `seal` (a charm that sways with each shot, as on the revolvers) and six `Flames` at the muzzle. Neither is among the owner's parts, so their bones are empty and the shot shows the shared muzzle flash. Its `magazine_2` stays hidden (scale 0) in the owner's reload.
- **Kestrel Rifle:**
  - **Bolt:** the main part's elements the owner named `bolt`: the carrier along the right of the receiver and its handle. It turns a little about its own axis as it is caught back.
  - **Sights:** a peep (the sights part), which a scope replaces, mounted where the peep stood, and a post (the main part's).
  - **Light stock:** the owner's light stock for the Whistler stands 3.15 px behind its place and 0.9 px above it. Its wrist block and collars are the same as its wooden and weighted stocks', which meet the grip, moved by exactly that, so fitted it floated behind the gun. `tools/guns.py` moves it into place as it reads it (`PART_SHIFTS`). The library's file is unchanged, and the moved part is checked face for face like the rest.
- **The hands:** the right on each grip's middle; the left half a pixel under the fore-end, as on the Garrison Rifle. The arms run as the other rifles' do. Checked in first-person and side previews, idle, firing and through each reload.
- **Aimed, each is held 2 px further out** (`"eye_relief"`):
  - Each shot kicks the gun back toward the eye (the Picket 1.4 px, the Kestrel 1.6), and the Ranger's handle slides 2.6 px back beside its sights.
  - In a preview of the aimed shot at the hip's depth, the back of each came within about 2 px of the eye (the near plane is 0.8 px) and filled the bottom of the view. Held 2 px further out, the nearest of each is about 4 px away and the sights stay large; at 4 px further the gun shrank behind the right arm.

**Connections:**
- **Parts:** steel from the steel foundry, brass, copper, a lever and planks, as the service arms take. Rifle Rounds as before.
- **Their place:** the long-range rifles of the steel tier. The Longhorn and Drover Rifles (iron and brass, worked between shots) reach 120 and 100 blocks; these reach 120 to 128, aim tighter, and need no working.

**Balance:** starting numbers.
- **Damage:** each lands about 20 a second, as the Patchwork Carbine does (6 every 6 ticks), but in fewer, harder shots, of which armour turns aside a smaller share.
- **Against the Garrison Rifle:** it lands more a second up close (about 27). The marksman rifles are for reach and precision: aimed, a shot strays at most 0.26 blocks at 100 blocks.
- **Between them:** the Ranger hits hardest; the Kestrel's clip of eight reloads slowest, and only it takes a scope; the Picket aims steadiest.

**Save compatibility:** new items `jugcraft:picket_rifle`, `ranger_rifle` and `kestrel_rifle`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The tactical grip** these three have parts for is not an attachment yet. It is a pull request of its own, with the laser sight.
- **Aimed,** as on the other rifles, the gun's back sits below the crosshair and the right fist over the lower middle of the view. The aiming polish is a pull request of its own.
- **The Ranger's seal and flames** move in the owner's animations, but no part holds them, so nothing shows.
- **The Kestrel throws no casings;** its animations cue none.
- **Not played:** none of it has been played yet.

## Slice 9B: the automatic weapons
The second of the gun sets the owner chose on 10 October 2026: three steel guns that fire for as long as the trigger is held, all on Light Rounds.

| | Rattler Pistol | Bronco SMG | Squall Rifle |
|---|---|---|---|
| The owner's gun | Auvtomag | Jr Wristbreaker | Gale |
| What it is | a machine pistol, held in one hand | a short submachine gun with a broad barrel shroud, fired in one hand | an air rifle with a gas canister and a gauge on its left side |
| Damage | 3 | 3.5 | 2.5 |
| Rate | 6.7 a second (every 3 ticks) | 6.7 a second (every 3 ticks) | 10 a second (every 2 ticks) |
| Holds | 20 | 25 | 40, a canister |
| Reload | 2.4 s, a magazine | 2.65 s, a magazine | 2.85 s, a canister |
| Spread, hip / aimed | 4° / 2° | 5° / 2.5° | 3° / 0.8° |
| Range | 48 blocks | 40 | 64 |
| The view aimed | narrowed to 0.9 | 0.9 | 0.85 |
| Round | Light Round | Light Round | Light Round |
| Takes | the barrel attachments, both magazines and the three scopes | the barrel attachments and both magazines | the three stocks, the light grip, the four bayonets and the three scopes |

**Crafting** (a crafting table):
- **Rattler Pistol:** three steel ingots over a redstone (its sear), a lever and a brass ingot.
- **Bronco SMG:** two steel ingots and a brass ingot over a redstone, a lever and a brass ingot.
- **Squall Rifle:** three steel ingots over a piston (its pump), a lever and planks.

**What you see:** the owner's animations.
- **Rattler Pistol:** its slide snaps back with each shot, and a casing flies. To reload, the empty magazine drops out of the grip, the left hand pushes a new one up into it, and the slide is racked.
- **Bronco SMG:** the charging handles either side of its receiver ride back with each shot, and it bucks up. To reload, the left hand pulls the magazine from ahead of the grip and seats a new one, and the bolt is worked.
- **Squall Rifle:** the needle of the gauge on its gas canister jumps with each shot. To reload, the clamp at the canister's front swings aside, the canister is twisted off and carried away, and a new one goes on and clanks home.
- **Casings:** the Rattler throws one with each shot; the Bronco's and the Squall's animations cue none.
- **Sounds:** the library's short iron rifle crack (Rattler; its "enchanted" shot, which has no ring after it), its second new rifle shot (Bronco) and its air gun shot (Squall). None carries a tag naming another source; the new rifle shot carries only a date. The Squall's canister clanks with the ramrod's metal sound under its own event, `clank` ("Canister clanks").

**How the models were built:**
- **Gun bodies:** each turns about its grip, where the right hand holds it.
- **Rattler Pistol:**
  - **Slide:** the owner's receiver part, on the bolt bone, which each shot drives back. Its rear notch is on the slide, its front post on the frame.
  - **Scopes:** its sights part is empty, so it sits on the slide too: a scope and its rail (the owner's `no_sights`) ride the slide.
  - **Magazine:** only its base plate shows, under the grip; the reload drops it 14 px.
  - **Left hand:** held in one hand, its idle hides the left arm. The hand point is where it takes the new magazine's base, 1.67 s into the reload.
- **Bronco SMG:**
  - **Bolt:** the owner's bolt part, the two charging handles.
  - **Magazine:** turns about its top, in the well ahead of the grip.
  - **Left hand:** held in one hand. The hand point is under the seated magazine's base, 1.25 s into the reload, as it pushes it home.
  - **Seal:** its animations name a `seal` (a charm that sways, as on the revolvers) that no part holds; the bone is empty.
- **Squall Rifle:**
  - **Canister:** the main part's 19th, 21st, 22nd and 33rd elements, with the riser to the gauge (the 23rd and 24th) and the gauge's dial and cap (the 20th and 27th), on the magazine bone. It turns about the point the owner turned those elements about.
  - **Gauge:** the needle (the 26th element) on the gauge bone, about the dial's middle, riding the canister.
  - **Clamp:** the 18th element, on the `magazine2` bone, swung aside for the canister to come out.
  - **Not used:** the owner's separate needle part is the same needle, unturned. The bolt bone is empty: only the draw names it, and holds it still.
- **Aimed, each is held further out** (`"eye_relief"`): in a preview of the aimed shot at the hip's depth, the Rattler's slide came within 1 px of the eye and the Bronco's receiver 0.4 px, closer than the near plane (0.8 px), so a shot would have cut through the view; the Squall's back came within 1.6 px. Held 4, 4 and 3 px further out, the nearest of each is 4.4 to 5 px away.
- **The arms:** the right on each grip, as on the other guns. The one-handed guns' left arms run as the Sentry Pistol's and the Garrison Rifle's do; checked in first-person and side previews, idle, firing and through each reload.

**Connections:**
- **Parts:** steel from the steel foundry, brass, redstone, a lever, a piston and planks. Light Rounds as before.
- **Their place:** the automatic guns of the steel tier, after the copper Rust Midge and the iron Riveter SMG; the Garrison Rifle is the automatic on rifle rounds.

**Balance:** starting numbers.
- **Damage a second:** the Rattler 20, the Bronco 23, the Squall 25, while their 20, 25 and 40 rounds last. The Rust Midge lands 13 and the Riveter SMG 17; the Garrison Rifle 27, on rifle rounds.
- **Between them:** the hand guns are wild from the hip and short-ranged; the Bronco hits hardest a shot and strays most. The Squall is steadier, reaches furthest, and holds the most, but reloads slowest.

**Save compatibility:** new items `jugcraft:rattler_pistol`, `bronco_smg` and `squall_rifle`, and the sound event `jugcraft:guns.clank`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The one-handed guns' reloads** lift the gun toward the upper right, and in the preview the right forearm fills that side for a moment; the left hand mostly stays below the view.
- **The Squall's needle** is a tenth of a pixel wide, so its jump is hard to see.
- **Not played:** none of it has been played yet.

## Slice 9C: the second energy weapons
The third of the gun sets the owner chose on 10 October 2026: three more energy weapons, on slice 8D's Energy Cells and shots.

| | Spikedriver | Seam Cutter | Caisson Pistol |
|---|---|---|---|
| The owner's gun | Railworker | CR4K Mining Laser | Hyperbaria |
| What it is | a rail pistol held in one hand, its magazine and a lever on its left side | a cutting laser carried at the hip, its core standing out of its left side | a pressure pistol held in one hand, a tall tank on top |
| Fires | a beam through every creature in its line, to the first block | a short beam through every creature in its line, for as long as the trigger is held | a bolt that leaps from its mark to two more creatures close by |
| Damage | 12 to each creature in the beam | 1.5 to each creature in the beam | 5 to its mark, then 3 and 1.8 |
| Rate | 1.25 a second (every 16 ticks) | 10 a second (every 2 ticks) | 2.5 a second (every 8 ticks) |
| Holds | 6 charges | 60 | 10 |
| Reload | 3.35 s: a magazine, then the lever | 3 s: the core | 2.85 s: the tank |
| Spread, hip / aimed | 1.5° / 0.3° | 2° / 1°, from the hip (no sights) | 6° / 3°, the cone its arc seeks in |
| Range | 64 blocks | 16 | 24 |
| The view aimed | narrowed to 0.85 | 0.95 | 0.9 |
| Charge a round | 800 JE | 100 JE | 300 JE |
| Takes | both magazines and the three stocks | nothing | nothing |

**Crafting** (a crafting table; as slice 8D's, the recipes need both the guns and the machines switches):
- **Spikedriver:** three steel ingots over a copper cable (its rails), an advanced circuit and a brass ingot.
- **Seam Cutter:** two copper ingots (its copper body) and an amethyst shard (its lens) over a steel ingot, an advanced circuit and a brass ingot.
- **Caisson Pistol:** a lightning rod (its arc's emitter) and two steel ingots over an advanced circuit and a brass ingot.

**How they fire:** as slice 8D's (above): the beam passes through every creature in its line that the shooter may strike and stops at the first block; the arc leaps from the creature nearest the aim within the cone to the nearest within 4 blocks, twice, each taking 60% of the damage before it. Neither touches a block. Each shot is `jugcraft:zap` damage, which pushes nothing back and counts every shot, so each of the Seam Cutter's ten a second lands.

**What you see:** the owner's animations.
- **Spikedriver:** each shot drives the rings at the back of its right side back and the tip of its muzzle recoils. To reload, the magazine swings down and out of its left side and a new one swings in under the left hand; then the left hand takes the lever on its left side and swings it back and round, 145°, to charge the rails, and returns it.
- **Seam Cutter:** each shot jolts it at the hip. To reload, its core is drawn out of its left side and drops away as the left hand lets go below, a new one goes in, and the carry handle on top is flicked forward like a lever.
- **Caisson Pistol:** each shot drives its bolt back. To reload, the tall tank is lifted off and tossed away to the left and a new one comes down into place, and the left hand draws the bolt back.
- **Sparks:** where the Spikedriver's and the Caisson's animations cue a casing, they vent sparks, as slice 8D's do; the Seam Cutter's cue none.
- **Sounds:** the library's rail shot (Spikedriver), its second laser shot (Seam Cutter) and its plasma shot (Caisson Pistol). None names another source: the rail and plasma shots carry no tags, the laser shot only a container's format tag (`isommp42`).

**How the models were built:**
- **Spikedriver:**
  - **Lever:** the frame standing out of its left side (the main part's 34th, 35th and 37th to 43rd elements), swung back about its back end. The left hand's moves in the reload take it from the magazine to the lever's free end and back with it.
  - **Bolt:** the two rings standing up at the back of its right side (the 2nd and 3rd elements), which ride back with each shot. They are flat plates the owner textured on their backs only, facing the eye.
  - **Tip:** the tip of its muzzle (the 29th element), which recoils.
  - **gun_body2 and gun_body3:** empty, about the grip: they carry the whole gun as the lever is worked.
  - **Magazines:** its own, and the Extended and Speed Magazines (the owner's `ext_mag` and `speed_mag` parts), on its left side. It is the first energy weapon to take a magazine: the Extended Magazine holds nine charges.
  - **Left hand:** held in one hand, its idle hides the left arm. The hand point is on the new magazine as it is held in, 1.25 s into the reload.
  - **Aiming:** it has no sights; aimed, it is looked along over the top of its back. Its back came within 2.6 px of the eye through the aimed shot, so it is held 2 px further out (`"eye_relief"`).
- **Seam Cutter:**
  - **Held as the Thresher is:** the owner's third-person transform tilts it 72.75° up off the arm, made, as the Gattaler's, for an arm hanging at the hip; seen from outside, its holder's arms hang that much lower (`GunLooks.TILT`).
  - **Right hand:** on the slanted grip behind its copper body (the main part's 3rd element).
  - **Core:** the brown cylinder standing out of its left side (the 1st and 44th to 49th elements), where the left hand holds it; the reload draws it out to the left about its inner end.
  - **Carry handle:** the handle above the grip (the 4th and 9th), the owner's `grip`, flicked forward about its foot, as on the Thresher.
  - **No sights:** its handle stands between the eye and its sight posts, so, as the Thresher's, aiming it only steadies it and narrows the view.
  - **The arms:** the owner's idle turns this gun's arm bones otherwise than the other guns', so each arm's way to the shoulder is turned to leave the screen as theirs do.
- **Caisson Pistol:**
  - **Tank:** the owner's `mag` (the main part's elements so named), about the middle of its foot.
  - **Bolt:** the owner's bolt part, along the top of its back. The small element named `bolt` at the foot of its grip is a copy of the bolt's knob, a pommel, and stays with the body.
  - **gun_body2:** empty, about the grip; it holds the gun 0.7 px forward throughout, as the owner's animations do, and rolls it as the bolt is worked.
  - **Left hand:** held in one hand. The hand point is on the bolt drawn back, 2.375 s into the reload.
  - **Sight:** the small ring at the left of the tank's foot. Aimed, its back stays 4.6 px from the eye, so it needs no eye relief.
- **Checked** in first-person and side previews: idle, aimed, fired, through each reload and inspection, and the Spikedriver with each attachment.

**Connections:**
- **The energy system:** Energy Cells, filled at the Charging Station, as slice 8D's.
- **Parts:** steel from the steel foundry, advanced circuits from the circuit assembler, copper cable, copper, amethyst, a lightning rod and brass.
- **Their place:** past steel, with slice 8D's.

**Balance:** starting numbers.
- **Power:** like slice 8D's, about 0.015 damage to one creature a JE: a full cell (10,000 JE) is 12 Spikedriver shots, 100 of the Seam Cutter's (ten seconds of fire) or 33 of the Caisson Pistol's.
- **Spikedriver:** 15 damage a second to each creature in its line, as the Beam Pistol, in harder, slower shots that stray less and reach further.
- **Seam Cutter:** 15 a second to each creature in its line, for six seconds a magazine, within 16 blocks.
- **Caisson Pistol:** 12.5 a second to one creature, 24.5 across three. Its arc seeks wider and nearer (6° from the hip, 24 blocks) than the Stormlock's (2°, 64), narrower and further than the Linesman's (15°, 12).

**Save compatibility:** new items `jugcraft:spikedriver`, `seam_cutter` and `caisson_pistol`; nothing saved changes. `guns.enabled=false` or `machines.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Seam Cutter cuts no blocks.** The owner's gun is a mining laser, but no beam touches a block (slice 8D's rule). Mining with it would want its own design: protected land, drops and tool tiers.
- **The Caisson Pistol's inspection** brings the left hand to its bolt near the eye, and in the preview the left sleeve covers the right third of the view for about a second and a half. A search over the arm's direction found none that keeps it much smaller.
- **Not played:** none of it has been played yet. The cones, the JE costs and the Seam Cutter's rate want play to set.

## Slice 9D: the pump shotguns
The fourth of the gun sets the owner chose on 10 October 2026: three steel shotguns loaded a shell at a time and worked after every shot. The Sledge and the Throttle are pumps. The owner's Turnpike, the set's long gun, is worked by the bolt on its right side instead: its animations never pump it.

| | Sledge | Highwayman | Throttle |
|---|---|---|---|
| The owner's gun | Killer 23 | Turnpike | Venturi |
| What it is | a heavy pump shotgun with a bird's-head grip and a wooden fore-end | a long shotgun worked by a bolt on its right side, with a ring sight | a short pump shotgun on a pistol grip, its fore-end on rods and a bulb under its muzzle |
| Damage | 4 a pellet, eight pellets: 32 a shot | 3 a pellet: 24 a shot | 3 a pellet: 24 a shot |
| Rate | 1 a second (every 20 ticks) | 1 a second (every 20 ticks) | 1.25 a second (every 16 ticks) |
| Holds | 4 shells | 7 | 6 |
| Reload, a shell at a time | 0.4 s to open, 0.7 s a shell, 0.7 s to close: 3.9 s for four | 0.9 s, 0.65 s a shell, 0.8 s: 6.25 s for seven | 0.5 s, 0.65 s a shell, 1.1 s: 5.5 s for six |
| Spread, hip / aimed | 8° / 6° | 6° / 2.5° | 6.5° / 4.5° |
| Range | 24 blocks | 40 | 28 |
| The view aimed | narrowed to 0.9 | 0.8 | 0.88 |
| Takes | the barrel attachments, the three stocks, the light grip and the bayonets | the same, and the three scopes | the three stocks (in its pistol grip's place) and the three scopes |

Each fires Buckshot Shells (slice 1). The reload times are the owner's animations' (their opening, each loop and their closing).

**Crafting** (a crafting table; the guns switch):
- **Sledge:** three steel ingots over a steel ingot, a lever and planks: four steel for its heavy barrel and frame, the lever for its action and the planks for its fore-end.
- **Highwayman:** three steel ingots over planks, a lever and a brass ingot, with a second brass ingot under the first (its long barrel's fittings). Until 14 October 2026 it had no second brass ingot, and its grid was the Garrison Rifle's mirrored: a crafting table matches a shaped recipe mirrored too, so the two could not both be made from it.
- **Throttle:** two steel ingots and a copper ingot (its bulb) over planks, a lever and a brass ingot.

**How they fire:** as the other shotguns: each pellet strays within the spread to the first creature or block in its path, and a creature takes the damage of every pellet that lands on it at once (`jugcraft:bullet`, which counts every shot). A shot cuts a reload short and fires what is loaded.

**What you see:** the owner's animations.
- **Sledge:** with each shot the left hand pumps the wooden fore-end back and forward. To reload, the pump is drawn back and held open, the left hand pushes each shell in, and the pump is closed.
- **Highwayman:** each shot drives the bolt on the right of the receiver back and home as the right hand comes forward off the grip. To reload, the right hand draws the bolt back and holds it open, the left hand brings each shell up into the port under the receiver, and the bolt is closed.
- **Throttle:** with each shot the left hand pumps the fore-end on its rods back and forward, the bulb rocks and a spent hull flies out (and one more as the reload ends, where the owner's animation cues it). To reload, the bulb under the muzzle is twisted open, the left hand loads each shell under the receiver, and the bulb is twisted shut with a clank and the pump closed.
- **Sounds:** the library's other take of the scrap rifle's shot (its `enchanted_fire`, for the Sledge), its other take of the Thunderpipe's (the Highwayman) and its plasma shotgun's blast (the Throttle). None names another source: the Sledge's carries no tags, the Highwayman's only the program it was made in (`Software=FL Studio 20`), the Throttle's one empty tag. Each shell goes in with the insert sound, as on the other shotguns, and the Throttle's bulb shuts with the Squall Rifle's clank (`GunAnimations.GUN_SOUND_ALIASES`).

**How the models were built:**
- **Sledge:**
  - **Pump:** the owner's bolt part, the wooden fore-end under the barrel and a stud on the receiver's left side, which ride back 2 px with each shot.
  - **Sights:** none of its parts is a sight. Its rear notch is the pair of posts on top of the receiver's front (the main part's 16th and 17th elements), its front post on the barrel's muzzle.
  - **Empty bones:** its shell bone stays at scale 0 in the owner's animations. It and the magazine bone hold nothing, nor do the scriptures and no_sights bones that only its inspection moves.
  - **Aiming:** held 6 px further out (`"eye_relief"`): through the aimed shot its kick otherwise brought the top of its grip 1.9 px behind the eye.
- **Highwayman:**
  - **Bolt:** the owner's bolt part, the bolt in the port on the right of the receiver and its handle. Its fore-end is part of the main part and stays put.
  - **Shell:** the reload's shell bone carries a Buckshot Shell in (`PROPS`): a 1 × 1 × 3 px red hull with a brass head, which the left hand brings up through the port under the receiver, where it shrinks into the gun. It shows only while the reload moves it.
  - **Sights:** a ring on top of the receiver's front and a post ahead of it. A scope takes the ring's place (its no_sights part is empty).
  - **Aiming:** held 6 px further out: its kick otherwise brought the top of its grip 2 px behind the eye.
- **Throttle:**
  - **Pump:** the owner's bolt part, the fore-end on the rods under the barrel.
  - **Bulb:** the main part's 17th to 19th, 25th and 26th elements, on the owner's magazine bone, twisted 25° about its own axis before the shells go in and back after.
  - **Stocks:** a stock takes the place of its pistol grip (the owner's `stan_grip`), as on the Thunderpipe.
  - **Sights:** a ring on top of the receiver's front and a post at the muzzle. A scope takes the ring's place.
  - **Aiming:** held 2 px further out: the top of its grip came within 1.2 px of the eye through the aimed shot.
- **Checked** in first-person and side previews: idle, aimed, fired, through each reload and inspection, each gun with each attachment, the Highwayman's shell through its loading, and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** Buckshot Shells (slice 1), from lead, brass, gunpowder and paper.
- **Parts:** steel from the steel foundry, a lever, planks, brass and copper.
- **Their place:** the steel tier, with slice 8B's Breacher and slice 9A's rifles.

**Balance:** starting numbers.
- **A shot, every pellet landing:** the Sledge 32, the Highwayman and the Throttle 24, as the Haymaker, Coach Gun and Breacher; the Thunderpipe 20.
- **A second, while the tube lasts:** the Sledge 32, the Throttle 30 (as the Breacher), the Highwayman 24. The double barrels fire their two shells faster (the Thunderpipe 50, the Coach Gun 60) and are empty sooner.
- **The trade-offs:** the Sledge hits hardest but holds four and reaches 24 blocks; the Highwayman holds the most, reaches furthest and keeps its pellets together aimed, but does the least a second and reloads longest; the Throttle fires quickest.
- Nothing converts back, so there is no loop.

**Save compatibility:** new items `jugcraft:sledge`, `highwayman` and `throttle`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Sledge's reload** brings the left hand up past the middle of the view as it pushes each shell in (about 7.7 px from the eye), as on the older shell-loaders.
- **The Highwayman's right hand** comes forward off the grip as each shot works the bolt, but in this rig it stops below and behind the bolt's handle. The arm models the owner's animations were made for would show where it was meant to reach.
- **The Throttle's bulb** is twisted open before the shells go in, but they go in under the receiver, as the owner's animations have it.
- **The tactical grip parts** the Killer 23 and the Turnpike carry (`tact_grip`) wait for the tactical grip attachment, a later pull request.
- **Not played:** none of it has been played yet. The spreads and the Sledge's damage want play to set.

## Slice 9E: the tactical grip and the laser sight
The first of the smaller follow-ups the owner chose on 10 October 2026 ("Tactical grip + laser"): two attachments from the owner's art.

| | Tactical Grip | Laser Sight |
|---|---|---|
| The owner's art | the tactical grip parts (`tact_grip`) ten of these guns carry; the Vertical Grip's item model | the laser sight: its item model, line of sight and textures, its short beam included |
| Slot | under the barrel, as the other grips and the bayonets | the scopes' slot |
| What it does | 90% of the spread from the hip, 80% of the kick | 70% of the spread from the hip; a red dot where the gun points; aimed, the view narrowed to 0.9 and its dot on the middle of the screen |
| Fits | the Drover Rifle, Coach Gun, Garrison Rifle, Breacher, Picket, Ranger and Kestrel Rifles, Squall Rifle, Sledge and Highwayman | the twelve guns that take the scopes, but for the Breacher and the Trench Lobber |
| Crafting | an iron ingot over leather | an iron ingot, a redstone torch (its emitter) and an amethyst shard (its lens) |

**The Tactical Grip:** the owner made a `tact_grip` part for ten of these guns, a stubby vertical grip under the fore-end in each gun's own colours (the Drover Rifle's and the Coach Gun's on the shared grips texture). The library has no item model for it on its own. The Drover Rifle's is the Vertical Grip's grip, collar and end cap without its rail clamp, the same pieces of the same texture, so the attachment's item is the owner's Vertical Grip model with only those three pieces (`"model_elements"`). It sits between the Light Grip (80% of the spread from the hip) and the Vertical Grip (65% of the kick), doing a little of each.

**The Laser Sight:**
- **On the gun:** it stands where the iron sights were, as the scopes do: a dark housing a pixel right of the gun's middle, a blue light at its back, and the owner's short red beam out of its front (the elements on its `laser` texture, a second texture its item model draws on: `"more_textures"`).
- **Aimed:** the eye looks along its left side, on the owner's line of sight (its `.scmeta`), and its dot shows on the middle of the screen as the Reflex Sight's does.
- **The dot:** while a gun with a Laser Sight is held, at the hip or aimed, the client draws a red dot each tick where the look first meets a block or a creature within the gun's range (`client/guns/GunLaser`). That is where an unstrayed shot lands, since every shot leaves the eye along the look. Other players see it too, from any player within 64 blocks.
- **How the dot is drawn:** it is the owner's red dot (the Reflex Sight's), a particle at full brightness that lasts two ticks, so it follows the aim without a trail. It grows with distance, to stay a few pixels across on the screen, and shows whatever the particle setting.

**Not on the Breacher or the Trench Lobber:** the pieces of a scope's texture that its faces use are packed into the spare room of each gun's atlas. The Breacher's atlas, holding three scopes' pieces already, has no room left for the Laser Sight's. Nor has the Trench Lobber's, and its grenades arc below a straight laser anyway. The laser's pieces are packed after the scopes', so adding it moved none of the pieces already placed: every gun's model keeps every bone it had, and the new pieces are in free room.

**Server authority:** the attachments change only the numbers the server already reads from the gun's own stack (`GunItem.spec`) and the kick. The dot is drawn on the client only and changes nothing.

**Connections:** iron, leather, a redstone torch and an amethyst shard; the guns that take them run from the iron tier to the steel tier.

**Balance:** starting numbers. The Laser Sight's 70% is the most any attachment narrows the spread from the hip (the Light Grip's is 80%), and it does nothing aimed, where the scopes help.

**Save compatibility:** new items `jugcraft:tactical_grip` and `laser_sight`, and the particle `jugcraft:laser_dot`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The dot is a particle:** it can trail the aim by up to a tick, and it glows at full brightness in the dark.
- **No beam through the air:** the owner's short beam shows on the gun; nothing is drawn between it and the dot.
- **Aimed, the housing stands beside the crosshair:** the eye looks along the laser's left side, on the owner's line of sight, so its dark housing and red front fill the view just right of the crosshair and hide part of a target there (CI run 38038657220).
- **Not played:** none of it has been played yet.

## Slice 9F: the aiming polish
The second of the follow-ups the owner chose on 10 October 2026 ("Aiming polish"): aimed down a gun's sights, the hands and the fitted stocks are kept off them.

**What was wrong:** in the CI screenshots of each slice since 8 October (the latest, run 38036998697), aiming down the sights showed:
- **the right fist** over the lower middle of the screen, on nearly every gun with sights. On the Longhorn Rifle, Drover Rifle, Duelling Pistol, Line Musket and Bellmouth it covered half the screen.
- **the Light and Weighted Stocks** coming up under the crosshair as a block. The CI screenshots show it on the Rust Midge, Riveter SMG, Garrison Rifle, Sledge and Highwayman, and previews without the fist in front on the Sentry Pistol, Linesman and Breacher too. The Garrison Rifle's Light Stock reached the crosshair itself.

**Why:**
- **The fist:** aiming slides the gun until its sight is on the middle of the screen, at the hip's depth (and its eye relief). That brings the grip under the eye, from a fifth of a block from it (the Coach Gun) to three quarters (the Rattler Pistol). The arms run mostly downward from the grips, but the fist itself, the player model's at four pixels across, fills that much of the view so near.
- **More eye relief does not cure it:** pushing the gun further out shrinks the fist but brings it nearer the middle of the view (previews at 4, 8 and 12 pixels more).
- **The stocks:** the attachment stocks stand about as high as the gun's back, so held where the hip view holds them they rise to just under the sight line. The guns' own stocks, drawn aimed, run out of the bottom of the view as a rifle's stock does.

**What changes** (`client/guns/GunArmsLayer`, `GunRenderer`):
- **The arms** shrink about the hands as the aim comes in, to half their size at full aim (`GunArmsLayer.AIMED_SIZE`). The hand still holds the grip and the arm still runs toward its shoulder; the fist now sits at the bottom of the gun, below its sights. A gun without sights (the Thresher, the Seam Cutter), which stays at the hip, keeps its arms as they are.
- **A fitted stock** is left out of the player's own view once they are halfway into aiming (`GunRenderer.SHOULDERED`). Aimed, a stock is set against the shoulder, under and behind the eye. The guns' own stocks stay, and a stock that replaces one of a gun's own parts leaves that part out still.

**Unchanged:** the view from the hip, the guns' models, animations and sights, their eye relief, the view from outside and what other players see, and everything on the server.

**Server authority:** none involved; it changes only how the player's own client draws their gun.

**Save compatibility:** nothing saved and no ids.

**Known limits:**
- **The change is seen during the aim:** the arms shrink over the aim's ease (four ticks, a fifth of a second), and a fitted stock disappears halfway through it and comes back halfway out.
- **Half size** was chosen from previews, not play. At the hip the arms stay the player model's own size, so they are drawn at two sizes.
- **One aimed screenshot not explained:** in the first CI run, the Riveter SMG with its second set of attachments showed both arms out of place aimed ("Slice 9F in CI" below). Its other aimed shots and every other gun's did not.
- **Not played:** none of it has been played yet.

## Slice 9G: the Trench Lobber's grenades
The third of the follow-ups the owner chose on 10 October 2026 ("Lobber gas grenades", offered as "Let the Trench Lobber load the chemical grenades from the field chemistry branch as well as the frag Grenade").

**What it does:** the Trench Lobber loads any grenade: the frag Grenade, and the field chemistry branch's chlorine, smoke and thermite grenades and flashbang. Each shot lobs the kind loaded, which goes off where it lands as it does thrown or from the grenade launcher: a burst, a chlorine cloud, a smoke screen, a pool of thermite or a flash ([field chemistry](field-chemistry.md)). None breaks a block.

**One kind a magazine**, as a gun's magazine holds one kind of round (`GunShots.reloadAmmo`). A reload loads:
1. the grenade in the other hand, if there is one, as the grenade launcher takes it first;
2. else more of the kind the Lobber holds, while the inventory has any, so that a top-up keeps its kind whatever comes first in the inventory;
3. else the first grenade in the inventory.

A reload of another kind than the Lobber holds first puts the grenades it held back in the inventory (any that do not fit drop at the player's feet), then fills the magazine with the new kind.

**What the player sees:** the counter's second line names the grenade the next reload would load and how many of it are to hand. The gun's tooltip names the grenade it holds. With no grenade at all, a reload says "No grenades to load."

**Server authority:** the server chooses the kind from its own copy of the player's hands and inventory when the reload starts. When it ends, the server puts the old grenades back and takes the new ones, and each shot lobs the kind its copy of the gun holds. The client chooses the same way only to play the reload and fill the counter.

**Save compatibility:** the kind is a new component on the gun, `jugcraft:loaded_grenade`, an item id. A Lobber that has only held frag Grenades carries none, so a Lobber saved before this slice holds frag Grenades as before; one naming an item no longer known, or one that is not a grenade, holds frag Grenades too. The component is the slice's only new registration: no new items or recipes. A build from before this slice does not know the component; how it loads a Lobber saved with one was not tested, so back the world up before going back to one.

**Balance:** each grenade does what it does thrown or launched, so the Lobber is no stronger with them than the grenade launcher, only quicker: six grenades a magazine, one every 0.7 s against the launcher's 1.5 s.

**Known limits:**
- **One kind a magazine:** a magazine of mixed grenades is not possible; changing kind changes all of them.
- **Not played:** none of it has been played yet.

## Slice 10A: the launchers
The first of the gun sets the owner chose on 10 October 2026 ("Launchers (Recommended)"): two guns that fire the rocketry branch's High-Explosive Rockets, and a grenade launcher.

| | Earthmover | Skylark Rifle | Bullfrog |
|---|---|---|---|
| The owner's gun | Dozier RL | Rocket Rifle | Blooper |
| What it is | a shoulder rocket launcher with a drum of four rockets on top of its tube | a break-open rifle that fires one rocket at a time, fast and flat | a stubby grenade launcher with a sliding barrel |
| What it fires | High-Explosive Rockets | High-Explosive Rockets | grenades, any kind (as the Trench Lobber, [slice 9G](#slice-9g-the-trench-lobbers-grenades)) |
| The burst | 24 at its centre (12 hearts), falling off to nothing at 5 blocks: the Rocket Launcher's | the same | the grenade's own: a frag Grenade's 16 at its centre over 4 blocks |
| Rocket speed | 3 blocks a tick (60 a second), as the Rocket Launcher's | 4.5 blocks a tick (90 a second) | (lobbed as the grenade launcher throws) |
| Rate | 1 a second (every 20 ticks) | one shot, then the reload | one shot, then the reload |
| Holds | 4 rockets | 1 | 1 grenade |
| Reload | 3.05 s | 2.4 s | 2.25 s |
| Spread, hip / aimed | 2.5° / 1° | 1.5° / 0.25° | 3° / 1.5° |
| Range | 72 blocks | 135 | 20 (how far a level shot carries) |
| The view aimed | narrowed to 0.85 | 0.8 | 0.92 |
| Takes | the light and tactical grips, the bayonets and the three scopes | the three stocks, the light and tactical grips and the bayonets | the same as the Skylark Rifle |

The reload times are the owner's animations'.

**How a rocket flies** (`GunShots.rocket`, the rocketry branch's `CombatRocket`): from the eye along the look, strayed by about the spread in degrees, straight and untouched by gravity. It bursts where it hits anything, or in the air at the end of the gun's range: its fuse is the range over the speed, 24 ticks for the Earthmover's and 30 for the Skylark Rifle's. The burst is the Rocket Launcher's high-explosive one (`Blast`): it hurts living things only (the shooter too, if they stand within 5 blocks of it), walls shield from it, and it never breaks, moves or burns a block. A rocket saved in flight keeps what is left of its fuse.

**What they load:** the rocket guns take High-Explosive Rockets from the inventory, a rocket a round. Homing rockets stay the Rocket Launcher's: a rocket gun neither loads nor fires them. The Bullfrog loads any grenade, the one in the other hand first, as the Trench Lobber does, and lobs the kind it holds.

**Crafting** (a crafting table; the guns switch, and for the rocket guns the machines switch too, since their rockets come from the rocket workshop):
- **Earthmover:** three steel plates over a copper ingot, a tripwire hook and a copper ingot: the plates for its tube, the copper for its drum and the hook for its trigger, as the Rocket Launcher has.
- **Skylark Rifle:** three steel plates over planks, a lever and a tripwire hook: its stock and its breech.
- **Bullfrog:** an iron ingot and two copper ingots over planks and a lever.

**What you see:** the owner's animations.
- **Earthmover:** each shot kicks the launcher back, and the backblast (the owner's fire part) flares out behind the tube at twice its size and dies back toward it. To reload, the launcher is rolled over to the left, the drum twisted free, pulled out to the left and down, and a full one brought back and twisted home with a clank.
- **Skylark Rifle:** each shot kicks it back and up. To reload, the barrel breaks open downward on its hinge, the left hand brings up a rocket and slides it nose first into the breech, and the barrel snaps shut.
- **Bullfrog:** each shot drives the barrel back along the rod under it and flaps the leaf sight. To reload, the gun is brought in and tipped up, the left hand racks the barrel back, holds the muzzle while a grenade is seated in its mouth, and pushes the barrel home. The draw swings the barrel up shut, as the Skylark Rifle's does.
- **In flight** a rocket is drawn as the Rocket Launcher's are: its item, trailing smoke and flame.
- **Spent cases:** none. Where the owner's animations cue one, the gun puffs smoke, as a muzzle-loader's lock does.
- **Sounds:** the library's bazooka shot (the Earthmover), its rocket rifle shot (the Skylark Rifle) and its second air gun shot, a hollow thump (the Bullfrog). None names another source: the first two carry no tags, the third only the program it was made in (`Software=Lavf59.27.100`). The Earthmover's drum clanks as it twists free and home (the Squall Rifle's clank, `GunAnimations.GUN_SOUND_ALIASES`); the Skylark Rifle's rocket and the Bullfrog's grenade go in with the insert sound.

**How the models were built:**
- **Earthmover:**
  - **On the shoulder:** its owner display holds it as a shoulder launcher: the grip at the hip guns' depth, the tube running on past the right of the head. In first person the tube's back fills the right of the view, and the backblast is behind the eye.
  - **Drum:** the owner's drum part, on the magazine bone, under the cylinder_magazine bone; both turn about the drum's middle. The reload twists the drum free (cylinder_magazine), pulls it out and brings it back (magazine).
  - **Backblast:** the owner's fire part (drawn on the owner's `gyrojet_flames` texture, packed into the gun's atlas), on the flame bone, which the renderer shows only while a shot moves it. It turns about its own middle, so at twice its size it starts at the tube's back end.
  - **Sights:** a frame with a centre dot, on a bracket off the left side of the body; the aim looks through the dot. A scope stands on the rail under it (the owner's no_sights part, `MOUNTS`), off the left side, beside the drum. The Laser Sight, longer and wider, would run into the drum there, so the Earthmover does not take it.
  - **Empty bones:** the bolt bone, which only the draw names.
- **Skylark Rifle:**
  - **Texture:** the owner's is two frames, one above the other, its flame flickering in a corner no part draws on. The gun takes the first frame (`FRAMED`).
  - **Barrel:** the main part's elements the owner named "barrel" (the tube, its front post and the fore-end under it), hinged at the bottom of the receiver's front. Its grips and bayonets ride it (`"mounts"`), so they open with it.
  - **Rocket:** the reload's shell bone carries a High-Explosive Rocket (`PROPS`): a 1.5 × 1.5 × 5 px steel body with a red nose and a dark nozzle. It rests where the reload's offsets, turned and scaled, bring its nose to the opened breech as the carry ends, in line with the barrel, and it shrinks to nothing as it slides in. It shows only while the reload moves it.
  - **Sights:** the notch on the back of the receiver and the post at the muzzle.
  - **Empty bones:** the flame bone, which only the shot's scale moves.
  - **Aiming:** held 3 px further out (`"eye_relief"`): through the aimed shot its kick otherwise brought the back of its receiver to the near plane.
- **Bullfrog:**
  - **Barrel:** the owner's barrel part on the standard_barrel bone, which each shot drives back; under the barrels bone, which the reload racks back; under the barrel bone, which the draw swings shut about the bottom of the receiver's front.
  - **Grenade:** the reload's shell bone, on the barrel, carries a grenade (`PROPS`): a 1.4 × 1.4 × 3 px olive body with a steel cap. It rests inside the barrel, where the reload's offset brings it into the muzzle's mouth.
  - **Sights:** the leaf sight on the back of the receiver (the main part's elements the owner named "sights"), on its own bone so that it flaps about its foot; the aim looks through its ring.
- **The props drawn the right way round:** the rocket and the grenade have a front and a back, and GeckoLib reads a box's east face and top from back to front. So those two faces get blocks of their own in the atlas, drawn back to front (`ORIENTED`, `draw_oriented()`). The older props share one block among their four long faces, so their east face and top show it the other way round: the Highwayman's and Thunderpipe's shells, the cartridges, the Linesman's cell and the ramrods, each one or two pixels across. They are left as they were.
- **Checked** in first-person and side previews: idle, aimed, fired, through each reload, draw and inspection; the Earthmover with each scope, the Laser Sight (which runs into its drum), a grip and a bayonet; the Skylark Rifle and the Bullfrog with a stock and a grip; the Skylark Rifle's rocket through its carry; and the nearest point of each gun to the eye through its aimed shot. The arms' directions were tried against others through the reloads (`armfit.py`); the usual ones stayed.

**Connections:**
- **Rockets:** the rocketry branch's High-Explosive Rockets, from the rocket workshop (four from two solid propellant, two guncotton and a rocket casing), the Rocket Launcher's ([rocket-launcher.md](rocket-launcher.md)).
- **Grenades:** the field chemistry branch's ([field-chemistry.md](field-chemistry.md)).
- **Parts:** steel plates from the machines, copper, tripwire hooks, levers, planks and iron.
- **Their place:** the rocket guns at the electronics tier, beside the Rocket Launcher, whose rockets they share; the Bullfrog is iron and copper, but its grenades come from field chemistry.

**Balance:** starting numbers.
- **A rocket** does what the Rocket Launcher's does. The Rocket Launcher fires one every 2 s from the inventory: 12 a second. The Earthmover fires its four a second apart and then reloads for 3 s: about 16 a second over a drum and its reload. The Skylark Rifle fires one and reloads for 2.4 s: 10 a second, flying faster, further and steadier.
- **The Bullfrog** lobs one grenade a reload, about one every 2.25 s, against the grenade launcher's 1.5 s and the Trench Lobber's six at 0.7 s.
- **Nothing converts back,** so there is no loop.

**Server authority:** the server spawns each rocket from its own copy of the player's position and look, at the gun's speed, with the gun's fuse, and its burst is the server's. Its rockets and the player's inventory are the server's, as for every gun.

**Save compatibility:** new items `jugcraft:earthmover`, `skylark_rifle` and `bullfrog`. A rocket in flight now saves what is left of its fuse (`fuse`); one saved before has none and gets the Rocket Launcher's lifetime, as it would have. `guns.enabled=false` turns the three recipes off, and `machines.enabled=false` the rocket guns'; the items stay registered. A build from before this slice does not know the three items, and does not read a rocket's fuse; how it loads a world holding them was not tested, so back the world up before going back to one.

**Known limits:**
- **The Earthmover in first person:** its tube's back fills the right of the view, as its owner display holds it on the shoulder. A scope on its side rail stands close by the eye, so at the hip it is large.
- **Homing rockets** are the Rocket Launcher's alone; the rocket guns do not load them.
- **The Bullfrog's barrel** slides in its reload and swings in its draw, as the owner's animations have it. Its grenade sits in the muzzle's mouth, hidden from the shooter by the barrel.
- **Not played:** none of it has been played yet. The rocket speeds, the ranges and the Bullfrog's reload want play to set.

## Slice 10B: coil and plasma
The second of the gun sets the owner chose on 10 October 2026 ("Coil and plasma", offered as the Gauss Rifle, Plasgun and Plasmabuss "running on the Energy Cells like the other energy weapons"): three more energy weapons, on slice 8D's Energy Cells and shots.

| | Solenoid Rifle | Votive Rifle | Glowmouth |
|---|---|---|---|
| The owner's gun | Gauss Rifle | Plasgun | Plasmabuss |
| What it is | a coil rifle, rings along its barrel and a glowing battery on its left | a plasma rifle, a wax seal and its parchment hanging from its flank | a plasma blunderbuss, a glowing canister either side of its chamber and a crank on top |
| Fires | a beam through every creature in its line, to the first block | a short beam through every creature in its line, for as long as the trigger is held | a bolt that leaps from its mark to two more creatures close by |
| Damage | 14 to each creature in the beam | 3 to each creature in the beam | 10 to its mark, then 6 and 3.6 |
| Rate | 1.25 a second (every 16 ticks) | 6.7 a second (every 3 ticks) | 1 a second (every 20 ticks) |
| Holds | 5 charges | 30 | 4 |
| Reload | 2.6 s: the magazine | 2.5 s: the magazine | 0.4 s, then 0.65 s a charge, then 0.9 s |
| Spread, hip / aimed | 1.5° / 0.15° | 3° / 1° | 12° / 9°, the cone its arc seeks in |
| Range | 128 blocks, the farthest of the energy weapons | 48 | 14 |
| The view aimed | narrowed to 0.75 | 0.85 | 0.95 |
| Charge a round | 1,100 JE | 200 JE | 750 JE |
| Takes | both magazines, the three stocks, the light and tactical grips, the bayonets, the three scopes and the Laser Sight | the same | nothing |

The reload times are the owner's animations'.

**Crafting** (a crafting table; as the other energy weapons', the recipes need both the guns and the machines switches):
- **Solenoid Rifle:** two copper cables (its rings) and a steel ingot over a steel ingot, an advanced circuit and a brass ingot.
- **Votive Rifle:** three steel ingots over a glass pane (its windows), an advanced circuit and a brass ingot.
- **Glowmouth:** a copper ingot (its bell) and two steel ingots over planks (its stock), an advanced circuit and a brass ingot.

**How they fire:** as slice 8D's (above): the beam passes through every creature in its line that the shooter may strike and stops at the first block; the arc leaps from the creature nearest the aim within the cone to the nearest within 4 blocks, twice, each taking 60% of the damage before it. Neither touches a block. Each shot is `jugcraft:zap` damage.

**What you see:** the owner's animations.
- **Solenoid Rifle:** each shot kicks it back, drives the knob in the slot on its right side back and slides the battery on its left out and back. To reload, the gun is lifted and tipped up toward the face, the battery slides out, the magazine swings down and away to the left and a new one comes up into the well; the knob is racked and the battery slides home.
- **Votive Rifle:** each shot drives the plate along the top of its receiver back and swings the seal. To reload, the gun is rolled to the right, the vent on the chamber's left lifts out, the plate is drawn back and held, the magazine swings down and out of the well and a new one is slapped home; then the plate is let go and the vent drops back.
- **Glowmouth:** each shot kicks it and jolts its crank. To reload, the gun is rolled to the left; for each charge the left hand brings an Energy Cell up from the lower left and pushes it into the left side of the chamber, where it shrinks away; then the left hand pulls the crank back and over, 106°, and returns it.
- **Sparks:** where the animations cue a casing, they vent sparks, as slice 8D's do.
- **Sounds:** the library's gauss shot (Solenoid Rifle), its nerve pinch shot, short and sharp (Votive Rifle), and its second plasma shot (Glowmouth). None names another source: none of the three carries a tag. The Glowmouth's charges go in with the insert sound, as the Stormlock's do.

**How the models were built:**
- **Solenoid Rifle:**
  - **Battery:** the owner's capital-B `Battery` bone: the battery on the receiver's left, a block and a glowing plate on an arm (the main part's 13th to 15th and 18th elements), which the shots and the reload slide out to the left.
  - **Bolt:** the knob in the slot on the right of the receiver (the 23rd element).
  - **Magazines:** its own, under the receiver, and the Extended and Speed Magazines (the owner's `ext_mag` and `speed_mag` parts).
  - **Sights:** the owner's ring on the back of the receiver, a peep, and the post on the muzzle.
  - **Texture:** its glowing plate's own texture is three frames, one above the other; the gun takes the first (`FRAMED`), so it glows but does not flicker.
  - **The atlas:** its stocks, grips and bayonets draw on textures it shares with other guns, packed whole into its atlas beside its own. That left the scopes' textures, packed piece by piece, too little room. So for this gun alone pieces of a scope's texture within a pixel of each other go in together, each pair joined wherever that takes no more room (`joined_rects()`, tried only when a gun's pieces do not fit one by one). Every other gun's atlas came out the same as before.
  - **Empty bones:** `magazine_2`, `default_mag` and `scriptures`, which its animations move but no part of it fits.
  - **Aiming:** held 1 px further out (`"eye_relief"`): through the aimed shot its kick brought the back of its receiver within a pixel of the eye.
- **Votive Rifle:**
  - **Bolt:** the plate along the top of its receiver (the main part's 7th element).
  - **Vent:** the louvre on the left of its chamber (the 22nd), which the reload lifts out and back.
  - **Seal:** the owner's `Seal` group, a wax seal and the parchment hanging from it (the 26th and 27th elements, drawn on the owner's `seal_2` texture), on the `scriptures` bone, which swings it about the seal.
  - **Magazines:** its own, swung down and back out of the well about its top, and the Extended and Speed Magazines.
  - **Sights:** the channel between the two rails on the back of its receiver, and the post on the muzzle.
  - **Texture:** its glowing windows' own texture is three frames; the gun takes the first.
  - **Aiming:** held 2 px further out: through the aimed shot the end of its long grip reached the eye.
- **Glowmouth:**
  - **One part:** the owner's gun is a single part of 55 elements.
  - **Crank:** the owner's group so named (the main part's 52nd to 55th elements), the lever on top of the back of its chamber, pulled back about its axle.
  - **Charge:** the loop's shell bone carries an Energy Cell (`PROPS`), drawn as the Linesman's: 1.5 × 1.5 × 3 px, a grey cap on a green body. It rests where the loop's last carry brings it into the left side of the chamber and shrinks to nothing there. It shows only while the reload moves it.
  - **Sights:** the notch between the two posts on top of the chamber's back (the 29th and 30th elements); there is no front post, and the aim runs over the muzzle's bell.
  - **Aiming:** held 3 px further out: through the aimed shot the end of its long grip came 0.74 px past the eye.
- **Checked** in first-person and side previews: idle, aimed, fired, through each reload, draw and inspection; the Solenoid and Votive Rifles with each scope, the Laser Sight, each stock, both grips, a bayonet and both magazines; the Glowmouth's charge through its loop and its crank through the reload's end; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **The energy system:** Energy Cells, filled at the Charging Station, as slice 8D's.
- **Parts:** steel from the steel foundry, advanced circuits from the circuit assembler, copper cable, glass, copper, planks and brass.
- **Their place:** past steel, with slices 8D's and 9C's.

**Balance:** starting numbers.
- **Power:** like the other energy weapons, about 0.013 to 0.015 damage to one creature a JE: a full cell (10,000 JE) is 9 Solenoid shots, 50 of the Votive Rifle's (seven and a half seconds of fire) or 13 of the Glowmouth's.
- **Solenoid Rifle:** 17.5 damage a second to each creature in its line, a little over the Spikedriver's 15, in heavier shots that stray the least of the energy weapons' and reach twice as far; 70 to each creature over a magazine.
- **Votive Rifle:** 20 a second to each creature in its line, for 4.5 s a magazine, within 48 blocks: more than the Seam Cutter's 15 a second and three times its reach, at the same damage a JE.
- **Glowmouth:** 10 a second to one creature, 19.6 across three, for four shots; over a magazine and its 3.9 s reload, about 5 a second to one. Its arc seeks nearly as wide as the Linesman's (12° from the hip, against 15°) and hits far harder, more slowly.

**Save compatibility:** new items `jugcraft:solenoid_rifle`, `votive_rifle` and `glowmouth`; nothing saved changes. `guns.enabled=false` or `machines.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Solenoid Rifle's reload** lifts the gun toward the face while the left hand changes the magazine beneath it. In the previews the left arm covers a quarter to a half of the view from 0.5 to 1.7 s into the reload; at the frames measured the same way, the Picket, Kestrel and Garrison Rifles' reloads cover a fifth at most. A search over the arm's direction, still running down to the shoulder, found at best about a seventh less, so the usual direction stayed.
- **The glowing parts do not flicker:** the owner's three-frame textures for the Solenoid's plate and the Votive's windows show their first frame.
- **Not played:** none of it has been played yet. The cones, the JE costs and the Solenoid's reach want play to set.

## Slice 10C: the double-barrels
The third of the gun sets the owner chose on 10 October 2026 ("Double-barrels", offered as the Super Shotgun, Doublet and Handcannon, "heavy shotguns using the existing pellet shots"): three guns that fire a spread of pellets, each loaded all at once.

| | Mule | Fowler | Culverin |
|---|---|---|---|
| The owner's gun | Super Shotgun | Doublet | Handcannon |
| What it is | a sawn-off double-barrel with a wooden pistol grip, which breaks open to load | a double-barrelled flintlock, with the Blunderbuss's hammer on its lock | a stubby iron barrel on a wooden pistol stock, fired one-handed |
| Fires | 10 pellets a barrel | 8 balls a barrel | 5 heavy balls |
| Damage | 2.5 a pellet (25 if all land) | 3 a ball (24) | 5 a ball (25) |
| Rate | the second barrel 0.4 s after the first | 0.5 s after the first | one shot |
| Holds | 2 | 2 | 1 |
| Reload | 1.75 s: both shells at once | 4.85 s: a ball rammed down each barrel | 3.7 s |
| Spread, hip / aimed | 9° / 7° | 8° / 5° | 9° / 7° |
| Range | 24 blocks | 28 | 18 |
| The view aimed | narrowed to 0.92 | 0.88 | 0.94 |
| Round | Buckshot Shell | Paper Cartridge | Paper Cartridge |
| Takes | nothing | the Light and Vertical Grips and the four bayonets | nothing |

The reload times are the owner's animations'.

**Crafting** (a crafting table; the guns switch, as every gun):
- **Mule:** two iron ingots and a brass ingot over an iron ingot, a lever and planks (the Coach Gun's materials, with an iron ingot more).
- **Fowler:** three iron ingots over an iron ingot, a flint and planks.
- **Culverin:** two iron ingots over a flint and planks.

**How they fire:** as the other pellet guns: each pellet goes from the eye along the look, strayed by the spread, to the first creature or block in its way, and each round loads at the end of the reload. Nothing new is worked out on the server.

**What you see:** the owner's animations.
- **Mule:** each shot kicks it back and up. To reload, the gun is tipped and its barrels drop open about the hinge, 72.5°, by 0.46 s; the left hand comes in to them, and at about 1.2 s they snap shut. Drawn, it snaps shut; inspected, it is broken open and closed again.
- **Fowler:** each shot drops the hammer, and a flash of priming fire jumps from the pan, as on the Bellmouth. To reload, the gun is tipped up, a ball drops into each muzzle (by 0.8 s), and the ramrod is drawn out from under the barrels and rams the left barrel (at 2.3 s), then the right (at 3.25 s), and is put back.
- **Culverin:** held in the right hand alone. Each shot drops the cock and a flash jumps from the pan. To reload, the gun is tipped up, the left hand comes up with the ball to the muzzle (0.7 s), and the gun's own ramrod is drawn from under the barrel, raised to the bore and rammed home.
- **Spent rounds:** the Mule throws a spent shell with each shot; the Fowler and the Culverin puff smoke from the lock and leave a cloud before the muzzle, as slice 4's muzzle-loaders do.
- **Sounds:** the library's shotgun blast (the Mule, as the Thunderpipe fires), its black powder shot (the Fowler, as slice 4's flintlocks fire) and its cannon shot, unused till now (the Culverin). The reloads cue the shared reload sounds; the Mule's shells go in with the shell sound, as the Coach Gun's do. The cannon shot carries no tag.

**How the models were built:**
- **Mule:**
  - **Breaking open:** its barrels, fore-end and top rib, and the strap under the fore-end (the main part's 1st, 7th and 12th to 16th elements) ride the `barrel` bone, which tips them down about the hinge at the frame's front. The frame, the fences round the breech and the grip stay.
  - **Empty bones:** `bolt`, and `shell`, which the reload keeps shrunk to nothing: no shells are seen going in.
  - **Sights:** the top of the frame over the breech, behind the top rib.
  - **Aiming:** held 2.5 px further out (`"eye_relief"`): through the aimed shot its kick brought the back of its grip 0.53 px past the eye.
- **Fowler:**
  - **Hammer:** the owner's own `hammer` part for it, on the lock's right side, drawn on the Blunderbuss's texture and turning about its foot as the Bellmouth's does.
  - **Balls, ramrod and flash:** the reload drops a ball into each muzzle (`ball` and `ball2`) and moves the ramrod (`ram`) on a bone (`ram2`) that shifts it from the left barrel to the right between the two rammings. None had a part, so each is one box drawn in an empty corner of its atlas copy, as slice 4's are: two 1 px lead balls, a half-pixel iron ramrod with a brass tip, 9 px long, and a 1 px priming flash at the pan. Each shows only while an animation moves it; the renderer now knows the second ball (`GunRenderer.PROPS`).
  - **The left arm** hangs from a `left_arm2` bone, which the reload slides back, as the Bellmouth's does.
  - **Sights:** over the raised rib between the barrels, clear of the muzzles' rings.
  - **The atlas:** its own texture is as big as the largest atlas, so the textures its hammer and grips share with other guns (the Blunderbuss's, and the Musket's grips') could not sit beside it whole. They go in piece by piece, as the scopes' do (`texture_islands()`, tried only when a gun's shared textures do not fit whole). Its bayonets draw on its own texture. Every other gun's atlas came out the same as before.
  - **Aiming:** held 3 px further out: through the aimed shot its butt plate came 0.82 px past the eye.
- **Culverin:**
  - **One-handed:** as the Duelling Pistol, its idle hides the left arm, whose hand is placed at the muzzle, holding the ball, 0.71 s into the reload.
  - **Cock:** the cock and the jaw on it (the main part's 15th and 16th elements), on the lock's right side, turning about the cock's lowest corner.
  - **Ramrod:** the owner's own, the rod under the barrel (the 13th element), on the `ram` bone.
  - **Ball and flash:** drawn here, as the Fowler's: a 1 px lead ball and a 1 px priming flash.
  - **Sights:** the notch between the two posts on top of the breech, raised to clear the barrel's top edge and the muzzle's swell, which stand higher.
  - **Aiming:** through the aimed shot its back stays 1.95 px from the eye, so it needs no eye relief.
- **Checked** in first-person and side previews: idle, aimed, fired, through each reload, draw and inspection; the Mule's barrels through the break; the Fowler's balls into both muzzles and the ramrod down each barrel, and the Fowler with its grips and bayonets, held, aimed, fired, reloading and inspected; the Culverin's ball, ramrod and left arm; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Buckshot Shell (slice 1) and the Paper Cartridge (slice 4); nothing new.
- **Parts:** iron, brass, flint, a lever and planks.
- **Their place:** with the iron guns. The Mule beside the Coach Gun; the Fowler and the Culverin beside slice 4's flintlocks.

**Balance:** starting numbers, at point blank with every pellet landing.
- **Mule:** 25 a shot, the Bellmouth's pellets, two shots 0.4 s apart and both shells back in 1.75 s: 50 every 2.15 s, about 23 a second. That is the most of the shotguns (the Coach Gun's is about 16, the Sledge's about 19), for a wider spread than the Coach Gun's (9° against 6°) and less reach (24 blocks against 32). Its pellets are no heavier than the Bellmouth's because its reload is the owner's quick one.
- **Fowler:** 24 a shot, two shots, then 4.85 s: about 9 a second, against the Bellmouth's 6.4, reaching 28 blocks against 20 with a tighter spread, for more iron.
- **Culverin:** the Bellmouth's 25 a shot in five heavy balls, in one hand, every 3.7 s: about 7 a second. Its spread is tighter than the Bellmouth's (9° against 12°), its reach shorter (18 blocks against 20).
- **A round:** each spends one, as every pellet gun does; nothing converts back.

**Save compatibility:** new items `jugcraft:mule`, `fowler` and `culverin`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Mule's shells are not seen going in:** the owner's model has none, and its reload keeps the bone for them shrunk to nothing.
- **The Mule sinks to the bottom of the view** while it is open, about 0.5 to 1.2 s into its reload: only the back of its frame shows, the left hand coming in beside it, as the Coach Gun does while it loads.
- **The Culverin's left arm** comes over the top middle of the view about 0.7 s into its reload, bringing the ball to the muzzle. Of the directions that still run to the shoulder, the one it takes covered the least in the previews, about three-quarters of the usual direction's cover.
- **Not played:** none of it has been played yet. The spreads and the Mule's damage want play to set.

## Slice 10D: the sculk guns
The fourth of the gun sets the owner chose on 10 October 2026 ("Sculk guns", offered as the Sculk Resonator, Whispers and Echoes 2, "the teal sculk-coloured set"): three guns grown from the deep dark, on the rounds the other guns fire.

| | Undertone Rifle | Murmur SMG | Reverb |
|---|---|---|---|
| The owner's gun | Sculk Resonator | Whispers | Echoes 2 |
| What it is | a sculk-grown rifle: glowing cells under its barrel, tendrils on its receiver | a sculk-grown machine gun: a glowing crystal in a window of its receiver, tendrils hanging under its barrel | a sculk-grown double-barrel whose barrels turn about their bore to load, tendrils on its receiver |
| Fires | one shot each pull | for as long as the trigger is held | 10 pellets a barrel |
| Damage | 9 | 3 | 3.5 a pellet (35 if all land) |
| Rate | 2.9 a second (every 7 ticks) | 10 a second (every 2 ticks), as fast as any gun | the second barrel 0.4 s after the first |
| Holds | 12 | 24 | 2 |
| Reload | 2.4 s: the magazine | 2.4 s: the magazine | 0.8 s, then 0.85 s a shell, then 0.95 s |
| Spread, hip / aimed | 2.5° / 0.3° | 3.5° / 1.5° | 7° / 5° |
| Range | 96 blocks | 48 | 32 |
| The view aimed | narrowed to 0.82 | 0.9 | 0.9 |
| Round | Rifle Round | Light Round | Buckshot Shell |
| Takes | the three stocks, the three scopes and the Laser Sight | both magazines, the scopes and the Laser Sight | the Light Grip and the Tactical Grip, the scopes and the Laser Sight |

The reload times are the owner's animations'.

**Crafting** (a crafting table; the guns switch, as every gun). Each takes steel and a lever, as the other steel guns, and an echo shard and sculk from the deep dark:
- **Undertone Rifle:** three steel ingots over an echo shard, a lever and sculk.
- **Murmur SMG:** two steel ingots and an echo shard over a lever and sculk.
- **Reverb:** two steel ingots over an echo shard, a lever and sculk.

**How they fire:** as the other bullet guns: each bullet or pellet goes from the eye along the look, strayed by the spread, to the first creature or block in its way. A magazine loads at the end of its reload, the Reverb a shell after each shell's time. Nothing new is worked out on the server.

**What you see:** the owner's animations. The Sculk Resonator's and the Whispers' are the same but for where the left hand rests.
- **Undertone Rifle and Murmur SMG:** each shot kicks the gun back, drives the rib along its top back and sways its tendrils. To reload, the gun is tipped and rolled; the magazine is drawn down and back out of the well and a new one pushed home; then the rib is let go. The tendrils sway all through it.
- **Reverb:** each shot kicks it back and drives its rib back. To reload, the gun is rolled to one side and its barrels turn 67° about their bore; each shell goes in with a nudge of the barrels; then they turn back and the gun comes level.
- **Spent rounds:** each throws a spent case with each shot, as the other guns firing its round do.
- **Sounds:** the library's sculk shot, unused till now (the Undertone Rifle), its soft beam shot (the Murmur SMG) and its shulker shot (the Reverb). None names another source: the sculk shot's one tag names its encoder, the beam shot's its container, and the shulker shot's the online video editor it was saved from and its encoder. The Reverb's shells go in with the shell sound, as the Coach Gun's do.

**How the models were built:**
- **Tendrils:** the owner's flat planes, drawn on the tendril texture (its first of sixteen frames), on the `seal` bone the animations sway. The owner left their edges, which have no area, untextured (`#missing`); those faces are dropped (`flat_face()`).
- **Glowing parts:** the Undertone Rifle's cells and the Murmur SMG's crystal draw on the Sculk Resonator's glowing texture, the Reverb's crystal on its own; each texture is three frames, and the guns take the first (`FRAMED`), so they glow but do not flicker.
- **Undertone Rifle:**
  - **Bolt:** the rib along its top (the main part's 8th element).
  - **Tendrils:** the four on its receiver (the 37th to 40th), swaying about their middle.
  - **Magazine:** the flat plate under its well (the 44th), drawn down and out with each reload.
  - **Sights:** the owner's ring on the back of its receiver, a peep, over the post at the front of its barrel.
  - **Aiming:** held 2 px further out (`"eye_relief"`): through the aimed shot the back of its bolt came to 0.76 px from the eye, inside the near plane (0.8 px).
- **Murmur SMG:**
  - **Bolt:** the rib along its top (the 3rd element).
  - **Tendrils:** the two hanging under its barrel (the 24th and 25th), swinging fore and aft about their tops.
  - **Magazine:** its own part, and the Extended and Speed Magazines in its place.
  - **Sights:** the same ring as the Undertone Rifle's.
  - **Aiming:** held 1 px further out: through the aimed shot its back came within 1.61 px of the eye.
- **Reverb:**
  - **Barrels:** its own part, two barrels with a rib between, a breech block and a muzzle collar, turning about their bore.
  - **Bolt:** the rib along its top (the 4th element).
  - **Tendrils:** the four on its receiver (the 24th to 27th).
  - **Empty bones:** `magazine` and `magazine_2`, which its shots hold still but no part of it fits.
  - **Sights:** the notch between the two posts on the back of its receiver.
  - **Aiming:** held 1 px further out: through the aimed shot its back came within 1.61 px of the eye.
- **The atlases:** the Sculk Resonator's and the Echoes 2's own textures fill only part of their 128-pixel squares, so the textures they share sit whole beside them; the Whispers' atlas grows from 64 to 128 to hold them. Every other gun's atlas came out the same as before.
- **Checked** in first-person and side previews: idle, aimed (the arms at half size), fired, through each reload, draw and inspection; each gun's bolt, tendrils and magazine or barrels through their animations; each with every attachment it takes; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Rifle Round, the Light Round and the Buckshot Shell; nothing new.
- **Parts:** steel and a lever, as the steel guns; an echo shard, from the deep dark's ancient cities, and sculk.
- **Their place:** past steel, a reward for reaching the deep dark.

**Balance:** starting numbers, at point blank with every shot landing.
- **Undertone Rifle:** 9 a shot, 2.9 a second, for twelve shots; over a magazine and its 2.4 s reload, about 17 a second, as the Garrison Rifle's (17) and more than the Picket Rifle's (14) or the Kestrel Rifle's (12). In return its aimed spread is wider than the marksman rifles' (0.3° against 0.1° to 0.15°) and its reach shorter (96 blocks against 120 to 128).
- **Murmur SMG:** 30 a second while it fires, for 2.4 s; over a magazine and its reload, about 15 a second, as the Squall Rifle's (15) and the Bronco SMG's (14), above the Riveter SMG's (11). It spends its rounds as fast as any gun.
- **Reverb:** 35 a shot, the hardest of the shotguns' (the Sledge's is 32), two shots 0.4 s apart and 3.45 s to load both: about 18 a second, as the Sledge's (19), holding two shells to its four.
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:undertone_rifle`, `murmur_smg` and `reverb`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The glowing parts and the tendrils do not move on their own:** the owner's frames would make them pulse and curl; the guns take the first frame of each.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 10E: the Cell Rack
The first of the systems the owner chose on 10 October 2026 to go with the gun sets ("Energy Cell rack (Recommended)", offered as "A block that charges several Energy Cells at once."): a rack of six cradles that charges every Energy Cell standing in it at once, from cables.

| | Cell Rack | Charging Station, for comparison |
|---|---|---|
| Holds | six Energy Cells, two shelves of three | one powered tool or Energy Cell |
| Takes from cables | up to 1,024 JE a tick (256 on copper cable) | the same |
| Buffer | 50,000 JE | the same |
| Gives | up to 1,024 JE a tick, shared evenly among the cells not yet full, at most 512 to one cell | up to 512 JE a tick |
| Six spent cells (60,000 JE) | about 3 s on silver or aluminum cable, 12 s on copper, with power enough | about 6 s and 12 s, a cell at a time |
| Size | one block | two blocks tall |
| Charges | Energy Cells only | powered tools and Energy Cells |

**Crafting** (a crafting table; it needs both the guns and the machines switches, as the Energy Cell does): five steel plates, two copper cables, an advanced circuit and a battery box. These are the Charging Station's parts, with a steel plate in place of its redstone lamp.

**Using it:**
- **Placing:** it faces you, its shelves toward you.
- **A cell in:** use it holding an Energy Cell. The cell stands in the cradle you point at, or in the nearest free one if that is taken. One cell goes in each use; with all six cradles full, nothing happens.
- **A cell out:** use it with an empty hand to take the cell from the cradle nearest where you point. A line over the hotbar gives its charge, for example "Energy Cell: 10,000 / 10,000 JE".
- **Its buffer:** used with an empty hand while it holds no cells, it gives its own charge: "Cell Rack: 20,000 / 50,000 JE".
- **Power:** cables connect on any side; its ports are on the back and both sides.
- **Hoppers:** from above or the sides, a hopper stands Energy Cells in its empty cradles and puts in nothing else. From below, a hopper takes out only full cells. So a hopper line can feed it spent cells and carry the full ones away. Item pipes reach it through the same rules (Fabric's item storage wraps it as it does a chest); that is not tested.
- **Breaking it:** mined with a pickaxe, it drops itself, as the Charging Station does; broken any way, it drops the cells in it, each keeping its charge.

**What you see:**
- **The block:** the power gear's electric look, the Charging Station's textures: a graphite plinth edged with high-voltage stripes, two shelves each with a glowing strip along its front, and a back panel with vented sides. Each shelf has three cradles: a cup the cell's foot stands in, with contacts on the back panel behind it. A status screen on the cap lights while it charges, and the rack gives off a little light then (5, the Charging Station's 7). It is drawn by `cell_rack_model()` in `tools/guns.py`.
- **The cells:** each stands upright in its cup, in front of its contacts. They are the Energy Cell's own icon, turned 45° so the cell stands on end with its terminal at the top, about 5 px tall. A charged cell glows as it does in the hand, and a spent one is dark.
- **Checked** in previews of the block model from the front, from three-quarters on either side, from behind and from above, lit and unlit, with a cell standing in each cradle (the cells drawn as the renderer turns them).

**How it works:**
- **Sharing:** each tick, the rack counts the cells not yet full. It divides up to 1,024 JE of its buffer between them evenly, no more than 512 to one: one or two cells take 512 each, three 341, six 170. A cell takes no more than fills it. A full cell takes nothing, and a hopper below may take it.
- **Accounting:** every JE that leaves the buffer goes into a cell. Nothing is lost or gained.
- **Where you point:** the cradle is worked out on the server from the point you used, turned into the rack's own frame for its facing (`CellRackBlock.cradleAt`). Cradles along a shelf count as nearer than those across the shelves.
- **The buffer:** fills from cables like the Charging Station's. A rack full of full cells keeps its buffer for the next ones.

**Connections:**
- **Input producers:** any generator, Battery Box and cable network of the machines (JE), as for the Charging Station.
- **Output consumers:** the energy weapons, through the Energy Cells they load from (slices 8D, 9C and 10B).
- **Its place:** the steel tier, with the Charging Station; it needs an advanced circuit and a battery box, from the machines' tree.
- **Required vs optional:** optional. The Charging Station still fills cells one at a time; the rack is for players who run many energy weapons.

**Balance:** the Cell Rack fills cells at up to twice the Charging Station's rate in all, but only Energy Cells, and costs the station's parts. It cannot make charge: it gives the cells only what it takes from cables. A Battery Box (400,000 JE) still fills forty cells.

**Save compatibility:** a new block `jugcraft:cell_rack`, its item and its block entity type. The block entity saves its buffer (`energy`) and its six cradles (`Items`, with each cell's charge). Nothing saved before changes. `guns.enabled=false` or `machines.enabled=false` turns its recipe off; the block, its item and its block entity stay registered, so a placed rack and its cells survive.

**Known limits:**
- **Flat cells:** the cells are their icons, as an item frame draws an item, so they are thin seen from the side.
- **No comparator output** and no charge shown on the block itself; the screen only says whether it is charging.
- **Not played:** none of it has been played yet.

## Slice 10F: enemies with guns
The second of the systems the owner chose on 10 October 2026 to go with the gun sets ("Enemies with guns", offered as "Mobs that carry and fire your guns and can drop them."): the **Raider Gunner**, a fourth kind of raider infantry ([raiders.md](raiders.md)), who carries one of the owner's service arms (slice 8B) and fires it.

| | Sentry Pistol | Garrison Rifle | Breacher |
|---|---|---|---|
| Carried by | 4 gunners in 10 | 3 in 10 | 3 in 10 |
| Fired from | up to 16 blocks | 24 | 8 |
| A shot | 2.5, half the gun's 5 | bursts of three, 2 a round (the gun's 4) | 8 pellets of 1.5 (12 if all land) |
| Between shots | 1.55 s | 0.2 s in a burst, 1.55 s after it | 1.55 s |
| Magazine and reload | 8, then 2.35 s | 30, then 2.65 s | 6, then 2.6 s |
| Over a magazine and its reload, every shot landing | about 1.4 a second | 2.7 | 6 at point blank |

The gunner has 20 health, 2 armour and the grenadier's pace (0.28).

**Where they come from:**
- **Raids:** gunners march in place of some of the grunts: one at raid levels 1 and 2, two at 3 and 4, three at 5. Each party is as large as before ([raiders.md](raiders.md#raids)).
- **Camps:** one of the two grunts holding a raider camp is now a gunner.
- **Summoned:** `/summon jugcraft:raider_gunner`; a summoned one belongs to no raid and despawns like any monster.
- **The guns switched off** (`guns.enabled=false`): grunts come in the gunners' places, so no gun can be won this way. Gunners already in the world stay.

**How it fights:**
- **Its quarry:** as every raider's: players and the town's folk, and whatever hurts it.
- **Closing in:** it goes after its target until it can see it from within its gun's reach, then stands and fires.
- **A shot:** from its eye at the middle of its target, strayed by the gun's spread from the hip, through the players' bullet code (`GunShots.bullets`). Each bullet goes to the first block or creature in the gun's range.
- **Its rate:** the Garrison Rifle fires bursts of three at its own interval; the pistol and the shotgun fire one shot. After each, the gunner waits 30 ticks.
- **Its magazine:** it counts its rounds, and when they run out it reloads for the gun's own reload time. It needs no rounds of its own.
- **Its side:** its bullets pass through raiders, as raiders' grenades do, and through marker stands and whatever it rides. Anything else in the way is hit.
- **Difficulty:** the bullet damage type scales against players as any monster's attack does. Easy halves a hit and adds one (at most the whole hit); Hard adds half. Against creatures, a hit is as above.

**What it drops** (to a player's kill):
- **Its gun,** about one time in twelve: 8.5%, vanilla's chance for a mob's equipment, more with Looting. Nothing is loaded in it.
- **Rounds:** 0 to 3 of its gun's ammunition: Light Rounds, Rifle Rounds or Buckshot Shells. The loot table picks them by the gun in its hand (`jugcraft:entities/raider_gunner`).

**What you see:**
- **The gunner:** a grey-green greatcoat, a belt of brass rounds across the chest, a soft field cap and goggles. It is drawn by `tools/raiders.py` beside the other uniforms.
- **The gun:** held raised along its look as a player holds one (slice 6's hold, now given to any mob holding a gun): the rifle and the shotgun in both hands, the pistol in one.
- **Its shots:** the clients that see it play the gun's shot and reload animations and its flash, as they do for another player. Everyone near hears the gun's own sound.

**How it works:**
- **`guns/MobGuns`:** the arms (`ARMS`), the share of damage, the burst, the pause and the drop chance, and the AI goal (`FireGoal`) that fires and reloads. It fires through the bullet code the players' shots now share with it, and tells the clients that see the gunner what its gun does (`GunActionPayload`).
- **`raiders/RaiderInfantry`:** the gunner's role. It carries a gun picked by the arms' weights. The gun gets its animation id on the server as it is picked, since a mob's equipment is not ticked as an inventory's is.
- **Its numbers:** in `tools/guns.py` (`MOB_*`). `tools/check_mod_data.py` checks `MobGuns` against them, and that each arm fires bullets. The gunner's stats and the raids' party table are checked against `tools/raiders.py`, as the other raiders' are.

**Connections:**
- **The raiders** (batch 57): a fourth kind of infantry, in raids and camps.
- **The guns:** the service arms (slice 8B) and their rounds. The fight is a second way to them, rare beside crafting.
- **Defences:** as against every raider: the tower guns, the sentries and the town's guards.

**Balance:**
- **The gunners' rate:** at half the guns' damage, in bursts and with reloads, the pistol and the rifle do less a second than a grunt's blows (at least 5 a hit, once a second). The shotgun does about as much only at point blank; it closes to 8 blocks, where its pellets spread.
- **No new raiders:** a raid's party is as large as before; raids still come at most once every few days.
- **The drops:** a gun one time in twelve, empty. Rounds only to a player's kill. Nothing converts back.

**Save compatibility:**
- A new entity, `jugcraft:raider_gunner`. Its gun is saved with it as any mob's equipment is. Nothing saved before changes.
- A raid saved in the middle keeps its members.
- **Going back** to a build from before this slice: that build does not know the gunner. How it loads a world holding one was not tested, so back the world up first.

**Known limits:**
- **Bullet guns only:** gunners fire only guns that fire bullets. The beam, arc, flame, grenade and rocket guns stay the players' alone.
- **No tactics:** they stand and fire once they see their target within reach. They do not take cover, flank or strafe.
- **The owner's gun-mob art:** the owner's library holds textures and sounds for Scorched Guns 2-style gun mobs: the Blunderer, Dissident, Hornlin, Cog Knight and others. It does not hold their models, so they could not be built here. With the models, they could carry the guns by the same code.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 10G: two guns at once
The third of the systems the owner chose on 10 October 2026 ("Dual pistols", offered as "A pistol in each hand."): a one-handed gun in each hand, and both fire.

**Which guns:** the thirteen held in one hand, those whose owner's idle hides the left arm (`tools/guns.py` `one_handed()`, `JugcraftGuns.ONE_HANDED`).
- **The guns:** the Warden Pistol, Haymaker, Duelling Pistol, Bulldog Pistol, Marshal Revolver, Sapper Revolver, Sentry Pistol, Beam Pistol, Rattler Pistol, Bronco SMG, Spikedriver, Caisson Pistol and Culverin.
- **Pairs:** any two, alike or not. Each says in its tooltip that it pairs.
- **Not these:** a gun held in both hands beside one in the other hand is no pair. The other gun stays as before: carried, not fired.

**Using them:**
- **The triggers:** left click fires the main hand's gun, as ever. Right click fires the other's: a press for a shot, held for an automatic (the Rattler Pistol, the Bronco SMG).
- **Each gun its own:** each keeps its own rate, magazine, sound, animations, flash and casings.
- **No aiming:** right click is the other trigger, so neither gun is aimed down its sights. From the hip each strays half as far again as one gun alone (`DUAL_SPREAD`, 1.5).
- **Reloading:** R reloads the main hand's gun, or the other's when the main one is full or has nothing to load. An empty gun's trigger reloads that gun.
- **One reload at a time:** while a magazine is out, neither gun fires. A shot of either cuts a shell-at-a-time reload short.
- **The other keys:** H inspects both; V stabs with the main gun's bayonet.
- **The counters:** each gun's counter is on its own side of the hotbar, the other hand's beyond the off-hand slot.
- **Laser Sight:** both guns point along the look, so one dot shows if either has a Laser Sight.
- **Doors and villagers:** a right click that opens a door or talks to a villager does that and fires nothing, as with any item in hand.

| | One Sentry Pistol | Two Sentry Pistols |
|---|---|---|
| From the hip, strays up to | 2° | 3° each |
| A magazine, every shot landing | 8 shots, 40 damage | 16 shots, 80 |
| At the triggers' fastest | 20 a second, for 2 s | 40 a second, for 2 s |
| Then | 2.35 s of reloading | 4.7 s: one gun after the other |
| Over a magazine and its reload | about 9 a second | about 12 |

The Garrison Rifle, for comparison, does about 17 a second over its 30 rounds and their reload, aimed.

**What you see:**
- **First person:** each gun in its own hand, each with that hand's arm. A gun drawn in a left hand is the same model as in the right, not its mirror image. So its grip hand's arm is drawn as the left arm, running out to the left shoulder: the right hand's way to its shoulder, mirrored across the gun.
- **From outside:** both arms raised along the look, each with its gun.

**How it works:**
- **`GunItem.dual`:** whether a creature holds a one-handed gun in each hand.
- **`GunItem.use`:** with two guns it neither aims nor counts the click as used, so neither gun dips out of view and back. On the client, the click that reaches the other hand pulls its trigger (`GunHooks.offTrigger`, `GunsClient`).
- **The server** (`GunShots.fire` and `reload`, by hand): the trigger and reload payloads say which hand. It fires or reloads the other hand's gun only while the player holds two one-handed guns. Each hand has its own trigger credit, so each gun keeps its own rate. A player reloads one gun at a time, and the other hand's reload stops once the guns are no longer two.
- **Other clients:** `GunActionPayload` and `GunTracePayload` say which hand's gun, so they play the right gun's shot and draw an energy pistol's beam from its side.
- **Its numbers:** `ONE_HANDED` and `DUAL_SPREAD` are in `tools/guns.py`; `tools/check_mod_data.py` checks `JugcraftGuns` against them.

**Connections:** the one-handed guns of slices 2 to 10C, with their rounds. No new item, recipe or round.

**Balance:**
- **More shots, less aim:** two guns fire twice as fast as one, but neither can be aimed, and each strays half as far again.
- **Reloads:** they reload one after the other, so over a magazine and its reload two pistols do about a third more than one. In a short fight they do twice as much.
- **Cost:** two guns to craft, and twice the rounds a second.
- **Against the rifles:** a rifle aimed still does more over time, and from further off.

**Save compatibility:** nothing new is saved: a gun in either hand is the same item as before. The payloads changed (they carry the hand), so a client and a server must be the same build, as with any change to the mod.

**Known limits:**
- **No mirror image:** a gun in the left hand is the same model as in the right, so its ejection port faces the same way. Its casings fly out to its own side.
- **The left arms are worked out:** they are mirrored from the right hand's by a rule, not placed by hand. In the CI screenshots the left hand holds its pistol from below, its arm rising from the bottom left of the screen, as the right hand's does on the right.
- **Left-handed players:** their main gun is drawn in the left hand, so its arms are now mirrored too; before, they ran in across the screen. No screenshot shows a left-handed player.
- **Reloading both:** that takes two presses of G, or each empty gun's trigger.
- **Not played:** none of it has been played yet.

## Slice 11A: the Nether guns
The first of the owner's guns taken up after the round of 10 October 2026 was built (the owner asked "Ok lets continue and do the next thing we need to"): three of the six guns they made in the piglins' crimson, blackstone and gold, the Mangalitsa, Vulcanic Repeater and Trotters. Their bullets set what they hit alight.

| | Tusker | Cinder Repeater | Bastion Rifle |
|---|---|---|---|
| The owner's gun | Mangalitsa | Vulcanic Repeater | Trotters |
| What it is | a pump shotgun: a crimson stock, a long black shroud over its barrel, gold bands and a gold diamond on its receiver | a repeater with a pistol grip: a crimson body, a black shroud over its short barrel, a magazine ahead of its grip, and a gold diamond set with a glowing stone on its side | a long rifle with a crimson fore-end and grip, gold bands, and a trapdoor breech on top of its receiver |
| Fires | 8 pellets a shot, the pump worked after each | for as long as the trigger is held | one heavy shot each pull, the trapdoor flipped open after each |
| Damage | 3 a pellet (24 if all land) | 3.5 | 14 |
| Rate | 1.4 a second (every 14 ticks) | 6.7 a second (every 3 ticks) | 1.1 a second (every 18 ticks) |
| Holds | 6 | 20 | 5 |
| Reload | 0.6 s, then 0.6 s a shell, then 0.65 s | 2.4 s: the magazine | 0.75 s, then 0.65 s a round, then 0.8 s |
| Spread, hip / aimed | 6.5° / 4.5° | 3° / 1.2° | 2° / 0.15° |
| Range | 28 blocks | 48 | 112 |
| The view aimed | narrowed to 0.9 | 0.88 | 0.8 |
| Round | Buckshot Shell | Light Round | Rifle Round |
| Takes | the silencers, the Muzzle Brake and Extended Barrel, the three stocks, the Light and Tactical Grips, the four bayonets, the scopes and the Laser Sight | the same, and both magazines | the same as the Tusker |

The reload times and the rates are the owner's animations': the Tusker's pump and the Bastion Rifle's trapdoor are worked in full between shots.

**Crafting** (a crafting table; the guns switch, as every gun). Each takes steel and a lever, as the other steel guns, and gold and gilded blackstone, which is found only in the Nether's bastion remnants:
- **Tusker:** two steel ingots over a gold ingot, a lever and gilded blackstone.
- **Cinder Repeater:** two steel ingots and a gold ingot over a lever and gilded blackstone.
- **Bastion Rifle:** three steel ingots over a gold ingot, a lever and gilded blackstone.

**How they fire:** as the other bullet guns: each bullet or pellet goes from the eye along the look, strayed by the spread, to the first creature or block in its way. A magazine loads at the end of its reload, the Tusker and the Bastion Rifle a round after each round's time.
- **Alight:** each creature their bullets hurt is set alight for 4 seconds, as the Stoker's flame sets it alight (slice 8C, `GunShots.BURN_SECONDS`): fire's own damage, one a second. Fireproof creatures shrug it off, and water puts it out. A shotgun's pellets on one creature set it alight once.
- **No block** is set alight, by a hit or a miss.
- The server works it out with the shot (`JugcraftGuns.INCENDIARY`); nothing new is sent to clients.

**What you see:** the owner's animations.
- **Tusker:** each shot kicks it back and up; then the pump is worked back and home under the left hand, throwing out the spent shell. To reload, the gun is tipped and the pump half drawn; each shell is pushed in from below; then the pump is let home.
- **Cinder Repeater:** each shot kicks it back a little, snaps the rib along its top back and home, and rocks the gold diamond on its side. To reload, the gun is rolled and lifted; the magazine is swung down out of its well and a new one pushed home and slapped; then the rib is worked.
- **Bastion Rifle:** each shot kicks it back; then the right hand leaves the grip, flips the trapdoor up and over to the left, throwing out the case, and works the bolt, the gun nudged forward as it does. To reload, the trapdoor is flipped open and the bolt drawn; the left hand brings a round at a time up to the breech; then the trapdoor is shut and the bolt sent home.
- **Spent rounds:** each throws a spent shell or case with each shot, as the other guns firing its round do: the Tusker's as its pump comes back, the Bastion Rifle's as its trapdoor opens.
- **Sounds:** the Haymaker's shotgun blast (the Tusker); the library's other Rust Midge shot, unused till now (the Cinder Repeater); the Bulldog Pistol's heavy rifle shot (the Bastion Rifle). None names another source: the new one's only tag is the program it was made with (`Software=FL Studio 20`). The library's other unused shots were passed over: its pistol, revolver, machine gun, gyrojet and second heavy rifle shots carry tags naming an artist or a sound pack, its carbine shot an engine's (`ENCODER=UnrealEngine4`), and its second rocket rifle sound is a quiet whoosh, not a shot. The Tusker's shells and the Bastion Rifle's rounds go in with the shell sound.

**How the models were built:**
- **Tusker:**
  - **Bolt:** the pump, its own part, under the barrel.
  - **Sights:** the owner's ring at the back of its receiver, between two wings, over the post at its front.
  - **Aiming:** held 1 px further out (`"eye_relief"`): through the pump stroke the top of its grip came to 0.57 px from the eye, inside the near plane (0.8 px).
- **Cinder Repeater:**
  - **Bolt:** the rib along its top (the main part's 29th element).
  - **Seal:** the gold diamond and its glowing stone (the 27th and 28th), rocking about their middle.
  - **Magazine:** its own part, and the Extended and Speed Magazines in its place. Its `magazine_2` bone, which only its shots hold still, is empty.
  - **The glowing stone** draws on the Pyroclastic Flow's glowing texture, which is three frames; the gun takes the first (`FRAMED`), so it glows but does not flicker.
  - **Sights:** the owner's ring at the back of its receiver over the notch at its front.
  - **Aiming:** held 1 px further out: through the aimed shot its rib came within 1.88 px of the eye.
- **Bastion Rifle:**
  - **Trapdoor:** the lid on top of its receiver (the main part's 9th element), hinged along its left edge, which is the only way its animations turn it without its passing through the receiver.
  - **Bolt:** its own part, the bolt and its handle on the right. Its faces draw on the Turnpike's texture (slice 9D), packed piece by piece, as the scopes' are.
  - **The round** a reload carries in: a brass case with a lead tip on the `shell` bone (`PROPS`), resting where the loop's last carry brings it into the open breech; it shrinks to nothing there.
  - **The whole gun** rides `gun_body2`, which each shot pushes 1.2 px forward as the bolt is worked. Its `magazine` bone holds nothing.
  - **Left out:** the main part's 27th element, the same box as its 1st with no texture on any face.
  - **Sights:** the owner's ladder on its receiver over the post at the front of its fore-end.
  - **Aiming:** held 6 px further out, as the Highwayman is: the shot's kick brought the back of its grip 2.61 px past the eye.
- **The left arms:** chosen in first-person previews to cover as little of the screen as they can through each gun's reload, still running down and back from the hand. The Bastion Rifle's left hand comes right up to the breech with each round, so its arm crosses the view for a moment, as the Highwayman's does.
- **The atlases:** the Tusker's own texture leaves the scopes' and the Laser Sight's pieces too little room one by one, so for it every group of pieces goes in joined from the first (`pack_groups()`); a gun whose pieces fit as before is packed as before, and every other gun's files came out the same.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the Tusker's pump, the Cinder Repeater's rib, diamond and magazine, and the Bastion Rifle's trapdoor, bolt and round through their animations; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Buckshot Shell, the Light Round and the Rifle Round; nothing new.
- **Parts:** steel and a lever, as the steel guns; gold, and gilded blackstone from the bastion remnants.
- **Their place:** past steel, a reward for raiding a bastion.
- **The fire** is the Stoker's (slice 8C).

**Balance:** starting numbers, at point blank with every shot landing.
- **Tusker:** 24 a shot, 34 a second while it fires; over its six shells and their reload (4.85 s), about 16 a second, as the Sledge's (16) and above the Throttle's (14) and the Highwayman's (13).
- **Cinder Repeater:** 23 a second while it fires, for 3 s; over a magazine and its 2.4 s reload, about 13 a second, as the Bronco SMG's (14), below the Murmur SMG's (15).
- **Bastion Rifle:** 14 a shot, as the Line Musket's ball, five times before a reload: 16 a second while it fires, but its trapdoor is slow to load (4.8 s for five), so over its rounds and their reload about 7.5 a second, below the marksman rifles' (12 to 14). It reaches nearly as far as they do (112 blocks against their 120 to 128) and is as steady aimed as the Solenoid Rifle (0.15°).
- **The fire** adds about one a second to a creature that burns, and more shots only keep it burning: a shotgun's pellets light it once.
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:tusker`, `cinder_repeater` and `bastion_rifle`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The glowing stone does not pulse:** the owner's frames would make it flicker; the gun takes the first.
- **In the Nether** most creatures are fireproof (blazes, ghasts, striders, magma cubes, wither skeletons, zombified piglins), so there the fire seldom counts; piglins and hoglins do burn.
- **The Bastion Rifle's loading arm** crosses the view for a moment with each round.
- **Aimed, the Bastion Rifle's bolt** (a diamond under the sight) and its handle show just under the crosshair, across the lower part of what it aims at.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 11B: the rest of the Nether guns
The other three of the six guns the owner made in the piglins' crimson, blackstone and gold, taken up after slice 11A in the same round ("Ok lets continue and do the next thing we need to"): the Pyroclastic Flow, Freyr and Ribs Glory. Their bullets set what they hit alight too.

| | Ashfall Pistol | Goldbristle Carbine | Crackling |
|---|---|---|---|
| The owner's gun | Pyroclastic Flow | Freyr | Ribs Glory |
| What it is | a pistol held in one hand: a crimson frame and grip, gold bands, a gold diamond at its back, a long black barrel with a vented jacket, and a tank under the barrel | a short carbine with a pistol grip: a crimson body, a black receiver, a gold ring behind its short barrel, a magazine at its left side, and a diamond set with a glowing stone at its back | a long pump shotgun in gold: a curved dark grip, a dark pump under the barrel, a black shroud over the middle of the barrel and ribs at its muzzle |
| Held | in one hand: it pairs with another (slice 10G) | in both hands | in both hands |
| Fires | one shot each pull | one shot each pull | 6 pellets a shot, the pump worked after each |
| Damage | 6 | 7 | 4 a pellet (24 if all land) |
| Rate | 2.5 a second (every 8 ticks) | 2.9 a second (every 7 ticks) | 1 a second (every 20 ticks) |
| Holds | 8 | 12 | 5 |
| Reload | 2.35 s: the tank | 2.05 s: the magazine | 0.3 s, then 0.5 s a shell, then 0.55 s |
| Spread, hip / aimed | 2.5° / 0.8° | 2.5° / 0.4° | 5° / 3° |
| Range | 56 blocks | 88 | 36 |
| The view aimed | narrowed to 0.9 | 0.85 | 0.9 |
| Round | Light Round | Rifle Round | Buckshot Shell |
| Takes | the three stocks, the scopes and the Laser Sight | the silencers, the Muzzle Brake and Extended Barrel, both magazines, the three stocks, the Light and Tactical Grips, the four bayonets, the scopes and the Laser Sight | the three stocks, the Light and Tactical Grips and the four bayonets |

The reload times and the Crackling's rate are the owner's animations': its pump is worked in full between shots.

**Crafting** (a crafting table; the guns switch, as every gun). Each takes steel, gold, a lever and gilded blackstone, as slice 11A's do:
- **Ashfall Pistol:** a steel ingot and a gold ingot over a lever and gilded blackstone.
- **Goldbristle Carbine:** two steel ingots and a gold ingot over a gold ingot, a lever and gilded blackstone.
- **Crackling:** two gold ingots and a steel ingot over a steel ingot, a lever and gilded blackstone.

**How they fire:** as slice 11A's: bullets or pellets from the eye along the look, each creature they hurt set alight for 4 seconds (`JugcraftGuns.INCENDIARY`), no block. A magazine or a tank loads at the end of its reload, the Crackling a shell after each shell's time.

**What you see:** the owner's animations.
- **Ashfall Pistol:** held out in the right hand; each shot kicks it back and up. To reload, the left hand comes in and pushes the plate at the front of the tank forward; the tank swings out to the left and down, a new one swings in, and the plate is drawn back.
- **Goldbristle Carbine:** each shot kicks it back a little, snaps the plate along its top back and home, and rocks the diamond at its back. To reload, the gun is rolled; the magazine is pulled out of its left side and a new one pushed home and slapped.
- **Crackling:** as the Sledge (slice 9D), whose animations its own follow: each shot kicks it back, then the left hand pumps the fore-end back and forward. To reload, the pump is drawn back and held open and the gun rolled onto its side, the left hand pushes each shell in, and the pump is closed.
- **Spent rounds:** the Ashfall Pistol and the Goldbristle Carbine throw a case with each shot; the Crackling's animations, as the Sledge's, cue none.
- **Sounds:** the Sentry Pistol's shot (the Ashfall Pistol), the Patchwork Carbine's (the Goldbristle Carbine) and the Coach Gun's (the Crackling). The library's shots not used yet are silenced takes, rocket, flamethrower and jetpack sounds and wind-ups, or carry tags naming another source (slice 11A), so none was added. The Ashfall Pistol's plate goes with the half pump sound and its tank with the magazine's; the Crackling's shells go in with the shell sound.

**How the models were built:**
- **Ashfall Pistol:**
  - **One hand:** its idle hides the left arm, so it is among the one-handed guns (`JugcraftGuns.ONE_HANDED`, slice 10G); the left hand's place is given for the reload's push of the plate (0.4583 s, `"hand_pose"`).
  - **Slider:** the plate at the front of the tank (the main part's 23rd to 25th elements), as the owner's groups name it.
  - **Tank:** the tank itself (the 26th and 27th), which the owner's groups hang from the slider, so the plate carries it; it turns about its neck, where it meets the plate.
  - **No bolt:** its parts have none, so the `bolt` bone each shot moves holds nothing, nor do the magazine bones its shots scale.
  - **Sights:** the owner's notch at the back of its receiver over the post at its front.
  - **Aiming:** at the hip's depth: through the aimed shot its back stays 3.83 px from the eye.
- **Goldbristle Carbine:**
  - **Bolt:** the plate along its top (the main part's 7th element), which each shot drives 2.6 px back, as the Vulcanic Repeater's rib.
  - **Seal:** the diamond and its glowing stone (the 19th and 20th), rocking about their middle. The stone draws on the Pyroclastic Flow's glowing texture, its first frame, as the Cinder Repeater's does.
  - **Magazine:** its own part, at the left side, and the Extended and Speed Magazines in its place.
  - **Bayonets:** drawn on the Jackhammer's texture, merged into its atlas as shared textures are (slice 7).
  - **Sights:** the owner's ring at the back of its receiver, between two wings.
  - **Aiming:** held 1 px further out, as the Cinder Repeater: through the aimed shot its top plate came within 2.81 px of the eye.
- **Crackling:**
  - **The Sledge's body:** the owner's Ribs Glory is the Killer 23 (slice 9D) lengthened, with the same grip, receiver and poses, a longer pump and tube, and its own long barrel: a shroud over its middle and ribs at its muzzle.
  - **Bolt:** the pump, its own part. Its `shell`, `magazine`, `scriptures` and `no_sights` bones hold nothing, as the Sledge's.
  - **Left out:** its standard barrel part, the Killer 23's, the same box as its own barrel's back two thirds; drawn together, the two flickered.
  - **No barrel attachments** (`MISFITS`): the owner's barrel parts for it are the Killer 23's, made for a barrel that ends at z −1.2, while its own runs on to −8.2. The silencers and the Muzzle Brake would sit inside its shroud, halfway along it, and the Extended Barrel inside its barrel.
  - **Sights:** the pair of posts on its receiver, as the Sledge's; its front post was on the Killer 23's standard barrel, so there is none.
  - **Aiming:** held 6 px further out, as the Sledge: the shot's kick brought the top of its grip 1.8 px past the eye.
- **The arms:** the Ashfall Pistol's and the Goldbristle Carbine's left arms chosen in first-person previews to cover as little of the screen as they can (the carbine's a third of what it first covered), still running from the hand toward the shoulder. The Crackling's are the Sledge's: its left hand comes up to the port with each shell, so the arm crosses the view for a moment, as the Sledge's does.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the Ashfall Pistol's plate and tank, the Goldbristle Carbine's plate, diamond and magazine, and the Crackling's pump through their animations; each with the attachments it takes; and the nearest point of each gun to the eye through its aimed shot.

**Connections:** as slice 11A's: the Light Round, the Rifle Round and the Buckshot Shell; steel, gold, a lever and gilded blackstone from the bastion remnants; the Stoker's fire. Nothing new.

**Balance:** starting numbers, at point blank with every shot landing, over a load and its reload.
- **Ashfall Pistol:** 6 a shot, 15 a second while it fires; over its tank and its 2.35 s reload about 9 a second, as the Sentry Pistol's (9). A pair (slice 10G) does about 12, as two Sentry Pistols do.
- **Goldbristle Carbine:** 7 a shot, 20 a second while it fires; over its magazine and its 2.05 s reload about 13 a second: between the Patchwork Carbine's (11) and the Undertone Rifle's (16), as the marksman rifles' (11 to 14), but reaching less far (88 blocks against their 120 to 128) and less steady aimed (0.4° against 0.1° to 0.15°).
- **Crackling:** 24 a shot, as the Tusker's, but slower: over its five shells and their reload (3.35 s) about 14 a second, as the Throttle's (14), below the Tusker's and the Sledge's (16) and above the Highwayman's (13). From the hip its six heavy pellets keep closer than any other shotgun's; aimed, only the Highwayman's keep closer, and only the Highwayman reaches further (40 blocks).
- **The fire** adds about one a second to a creature that burns, as slice 11A's.
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:ashfall_pistol`, `goldbristle_carbine` and `crackling`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Ashfall Pistol has no moving bolt:** its parts have none.
- **The Ashfall Pistol's tank** swings out and back in by itself while the left hand holds the plate, as the owner animated it.
- **The Crackling takes no barrel attachments:** the owner's are too short for its barrel. Parts made for it would fit.
- **The Crackling has no front sight:** it is aimed by the notch on its receiver over the top of its shroud.
- **The Crackling's loading arm** crosses the view for a moment with each shell, as the Sledge's does.
- **The glowing stone does not pulse,** as the Cinder Repeater's.
- **In the Nether** most creatures are fireproof, as for slice 11A's.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 12A: the End guns
The next of the owner's guns, taken up after the Nether guns reached `main` (on 13 October 2026 the owner asked "Ok lets continue and do the next thing"): the three they made in the shulkers' purpur, the Carapice, Shellurker and Weevil. Each is fed from a shulker box with the shulker peering out of it, cabled into the gun. Their bullets make what they hit float, as a shulker's do.

| | Chitin SMG | Hermit Rifle | Borer |
|---|---|---|---|
| The owner's gun | Carapice | Shellurker | Weevil |
| What it is | a short machine gun in purpur: a curved grip, a block on top of its receiver behind a ring sight, a short shroud with a post, and the shulker box under its receiver | the same body, longer: its muzzle wrapped in a curled purpur shell | a heavy machine gun carried at the hip: a D handle at its back, a long pale barrel with a post at its muzzle, an eight-sided drum and a crank on its left side, and the shulker box under its front |
| Fires | for as long as the trigger is held | the same | the same |
| Damage | 3 | 5 | 5.5 |
| Rate | 6.7 a second (every 3 ticks) | 5 a second (every 4 ticks) | 4 a second (every 5 ticks) |
| Holds | 24 | 20 | 40 |
| Reload | 2.8 s: the magazine | 2.55 s: the magazine | 3.9 s: the magazine |
| Spread, hip / aimed | 3.5° / 1.2° | 3° / 0.5° | 4° / 1.5° |
| Range | 48 blocks | 80 | 72 |
| The view aimed | narrowed to 0.88 | 0.85 | 0.95 |
| Round | Light Round | Rifle Round | Rifle Round |
| Takes | the scopes and the Laser Sight | the three stocks, the scopes and the Laser Sight | nothing |

The reload times are the owner's animations'.

**Crafting** (a crafting table; the guns switch, as every gun). Each takes steel and a lever, as the other steel guns, and a shulker shell and purpur, from the End's cities:
- **Chitin SMG:** two steel ingots and a shulker shell over a lever and a purpur block.
- **Hermit Rifle:** three steel ingots over a shulker shell, a lever and a purpur block.
- **Borer:** three steel ingots over a lever between two shulker shells, over a purpur block.

**How they fire:** as the other bullet guns: each bullet goes from the eye along the look, strayed by the spread, to the first creature or block in its way. A magazine loads at the end of its reload.
- **Float:** each creature their bullets hurt floats for 1 second (`GunShots.LIFT_TICKS`, 20 ticks): the game's own Levitation at its first level, as a shulker's bullet gives it (a shulker's lasts 10 seconds). It rises about three quarters of a block in that second. Each hit starts it again, so under steady fire it goes on rising, nearly a block a second. When it ends the creature falls, and a fall of more than 3 blocks hurts it, as any fall does. A longer spell of Levitation already on it is kept.
- **Only where the shot may strike:** a creature the shooter may not hurt (`GunShots.allowed`) is neither hurt nor lifted.
- The server works it out with the shot (`JugcraftGuns.LEVITATING`); nothing new is sent to clients, which show the effect as the game always does.

**What you see:** the owner's animations.
- **Chitin SMG:** each shot kicks it back and drives the block on top of its receiver back and home. To reload, the gun is lifted and tipped up toward the face, the block held back; the left hand draws the shulker box down and away, brings it back and pushes it home, and the block is let go.
- **Hermit Rifle:** each shot kicks it back and drives the block back, as the Chitin SMG's. To reload, the gun is lifted and tipped toward the face; the left hand draws the box down and away and pushes it home; then the block is worked.
- **Borer:** each shot drives the barrel 2.9 px back into the body and out again. To reload, the gun is pushed out and tipped; the left hand pulls the box down and away and brings it back; then the crank on its left is pushed forward and let back.
- **Spent rounds:** the Chitin SMG and the Hermit Rifle throw a case with each shot; the Borer's animations cue none.
- **Sounds:** the Riveter SMG's shot (the Chitin SMG); the library's shulker shot, the Reverb's (the Hermit Rifle); the Garrison Rifle's (the Borer). The library's shots not used yet are silenced takes, rocket, flamethrower and jetpack sounds and wind-ups, or carry tags naming another source (slice 11A), so none was added. The boxes go out and in with the magazine's sounds, the Borer's crank with the lever's.

**How the models were built:**
- **Chitin SMG:**
  - **Bolt:** the owner's receiver part, the block on top of the receiver.
  - **Magazine:** the shulker box under the receiver: the main part's last six elements (the box's tray and lid plates and the shulker's head), with the two cables into its top and the loop of cable ahead of it (the 10th to 12th), so they go with it. Its `magazine_2` and `scriptures` bones hold nothing.
  - **The whole gun** rides `gun_body2`, which the reload eases back and forth.
  - **Sights:** the owner's ring ahead of the receiver block, over the post on its shroud. A scope stands on the owner's rail (its `no_sights` part).
  - **Aiming:** at the hip's depth: through the aimed shot its back stays 1.69 px from the eye.
- **Hermit Rifle:**
  - **The same body** as the Chitin SMG's (the owner's two share their back half and their animations), its bolt the receiver part and its magazine the shulker box with its cables (the main part's 28th to 30th and last six elements). Its `magazine_2`, `default_mag` and `scriptures` bones hold nothing.
  - **The curled shell** at its muzzle is part of its body.
  - **Sights:** the owner's ring on its receiver, over the post at its muzzle.
  - **Aiming:** held 2 px further out (`"eye_relief"`): the shot's kick brought the back of its receiver to the eye.
- **Borer:**
  - **At the hip,** as the Thresher (slice 8C), whose poses the owner's Weevil shares: the right hand holds the D handle at its back, the left the barrel's root, just ahead of the box. Seen from outside it is carried low, as the owner's third-person transform tilts it 66.75° (`GunLooks.TILT`).
  - **Barrel:** its own part, slid back with each shot and through the reload.
  - **Magazine:** the owner's group so named in the main part (its first nine elements: the box, the shulker's head and the cables).
  - **Grip:** the owner's name for the crank on the left (the main part's 37th, 38th, 45th and 48th to 52nd elements), turned about its foot as the reload ends. The drum stays.
  - **No sights:** its receiver stands above the line from its front post, and the owner made it no aimed shot. Aiming steadies it and narrows the view, as the Thresher's.
- **The arms:**
  - **The Chitin SMG and the Hermit Rifle** take the Solenoid Rifle's arms (slice 10B), whose poses theirs are. Their reloads, like the Solenoid's, lift the gun toward the face while the left hand changes the box beneath it, so the left arm covers part of the view for a moment. A search over the arm's direction found at best about an eighth less, as for the Solenoid, so the usual direction stayed.
  - **The Borer's** were both chosen in first-person previews to cover as little of the screen as they can, still running back toward the shoulders: the left arm's share of the screen about seven tenths of what it was with the Thresher's directions, the right arm's about half.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the receiver blocks, the boxes, the Borer's barrel and crank through their animations; the scopes, the stocks and the Laser Sight; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Light Round and the Rifle Round; nothing new.
- **Parts:** steel and a lever, as the steel guns; shulker shells and purpur from the End's cities (purpur is also made from chorus fruit).
- **Their place:** past steel, a reward for reaching the End's cities.
- **The float** is the game's own Levitation, as a shulker's bullet gives.

**Balance:** starting numbers, at point blank with every shot landing.
- **Chitin SMG:** 3 a shot, 20 a second while it fires, for 3.6 s; over a magazine and its 2.8 s reload about 11 a second, as the Riveter SMG's and the Rattler Pistol's (11), below the Cinder Repeater's (13).
- **Hermit Rifle:** 5 a shot, 25 a second while it fires, for 4 s; over a magazine and its 2.55 s reload about 15 a second, as the Squall Rifle's (15), below the Garrison Rifle's (17). It reaches as far (80 blocks) and is a little steadier aimed (0.5° against 0.6°).
- **Borer:** 5.5 a shot, 22 a second while it fires, for 10 s; over its forty rounds and their 3.9 s reload about 16 a second, below the Thresher's (17), which fires faster but must spin up first.
- **The float** does no damage of its own. Held up under fire a creature climbs nearly a block a second: a whole magazine of the Borer's, every shot landing, lifts it about ten blocks, and the fall then costs it about 7. A magazine of the Chitin SMG's or the Hermit's lifts it about four, a fall that costs 1 or 2.
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:chitin_smg`, `hermit_rifle` and `borer`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Chitin SMG's and the Hermit Rifle's reloads** bring the left arm across part of the view for a moment, as the Solenoid Rifle's does.
- **The Borer has no sights:** it is fired from the hip, as the Thresher is.
- **Attachments:** the owner made no barrel parts, magazines or grips for these three, and no stocks for the Chitin SMG or anything for the Borer, so those take none.
- **The float lifts what the game lets float:** the game's own rules for Levitation apply.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 12B: the copper guns
The next of the owner's guns, taken up after slice 12A in the same round (on 13 October 2026 the owner asked again "Ok lets continue and do the next thing"): three of the five they made in copper, the Scrapper, LLR Director and Birdfeeder. They are early guns of copper and iron, as the Rust Midge is.

| | Penny Pistol | Conductor Carbine | Magpie SMG |
|---|---|---|---|
| The owner's gun | Scrapper | LLR Director | Birdfeeder |
| What it is | a short copper pistol: a dark grip, a cocking knob on each side, a notch and post on top, and a long magazine standing out of its left side | a copper carbine with a black shroud over its barrel, a ring sight at the back, a slide under the barrel and a magazine in its left side | a copper submachine gun on the same lines, with a long magazine standing out of its left side |
| Held | in one hand: it pairs with another (slice 10G) | in both hands | in both hands |
| Fires | one shot each pull | one shot each pull, the slide worked after each | for as long as the trigger is held |
| Damage | 4 | 8 | 2.5 |
| Rate | 3.3 a second (every 6 ticks) | 1.3 a second (every 15 ticks) | 5 a second (every 4 ticks) |
| Holds | 10 | 8 | 32 |
| Reload | 2.8 s: the magazine | 2.45 s: the magazine | 2.65 s: the magazine |
| Spread, hip / aimed | 3° / 1° | 2° / 0.3° | 3.5° / 1.5° |
| Range | 48 blocks | 96 | 40 |
| The view aimed | narrowed to 0.9 | 0.82 | 0.88 |
| Round | Light Round | Rifle Round | Light Round |
| Takes | the silencers, the Muzzle Brake and Extended Barrel and the three stocks | those, both magazines, the Light and Tactical Grips, the four bayonets, the scopes and the Laser Sight | those but the magazines |

The reload times and the Conductor Carbine's rate are the owner's animations': its slide is worked in full between shots.

**Crafting** (a crafting table; the guns switch, as every gun). Each takes copper ingots, an iron ingot and a lever, as the Rust Midge does:
- **Penny Pistol:** two copper ingots over a lever and an iron ingot (a two-by-two recipe, so it can be made in the inventory).
- **Conductor Carbine:** three copper ingots over a lever between two iron ingots.
- **Magpie SMG:** three copper ingots over a lever and an iron ingot.

**How they fire:** as the other bullet guns: each bullet goes from the eye along the look, strayed by the spread, to the first creature or block in its way. A magazine loads at the end of its reload. Nothing else follows a hit.

**What you see:** the owner's animations.
- **Penny Pistol:** held out in the right hand; each shot kicks it back and up and drives the knobs back, throwing a case. To reload, the left hand comes in under the frame and the gun is rolled; the magazine is drawn out to the left and dropped away, a new one swung in and slapped home; then the knobs are racked.
- **Conductor Carbine:** each shot kicks it back; then the left hand works the slide back and forward. To reload, the gun is rolled; the magazine swings out and away and a new one is brought in and slapped home; then the slide is worked.
- **Magpie SMG:** each shot kicks it back. To reload, the gun is rolled; the magazine slides out along its length and swings away, a new one swings in and slides home; then the bolt is racked.
- **Spent rounds:** the Penny Pistol throws a case with each shot; the Conductor Carbine's and the Magpie SMG's animations cue none.
- **Sounds:** the library's Scrapper shot, the Sentry Pistol's (the Penny Pistol); the Drover Rifle's (the Conductor Carbine); the Rust Midge's (the Magpie SMG). The library's shots not used yet are silenced takes, rocket, flamethrower and jetpack sounds and wind-ups, or carry tags naming another source (slice 11A), so none was added. The slide racks with each shot, the magazines go out and in with the magazine's sounds.

**How the models were built:**
- **Penny Pistol:**
  - **One hand:** its idle hides the left arm, so it is among the one-handed guns (`JugcraftGuns.ONE_HANDED`, slice 10G); the left hand's place is given for the reload's first hold under the frame (0.25 s, `"hand_pose"`).
  - **Bolt:** the owner's bolt part, the knob on each side of the receiver.
  - **Magazine:** the owner's magazine part, on the `sliding_magazine` bone, which the reload draws out to the left. The `magazine` and `magazine_2` bones its shots scale hold nothing.
  - **Sights:** the notch on its back over the post at its front (the main part's 12th and 13th elements, and its 16th).
  - **Aiming:** at the hip's depth: its back stays more than 5 px from the eye.
  - **Its other animations:** the owner also made "carbine" animations, for when a stock is fitted and it is held in both hands. They are not used: with a stock it is still held in one hand.
- **Conductor Carbine:**
  - **Slide:** the owner's bolt part is the slide under the barrel with the knob on the right of the receiver, which the left hand works.
  - **Magazine:** its own part, in the left side of the receiver, and the Extended and Speed Magazines in its place; `magazine_2` holds nothing.
  - **Sights:** the owner's ring at the back of the receiver, over the post at the front.
  - **Aiming:** held 2.5 px further out (`"eye_relief"`): the shot's kick brought the back of its receiver 0.64 px past the eye.
- **Magpie SMG:**
  - **Magazine:** its own part (the owner's `sliding_magazine` group), on the `sliding_magazine` bone, which hangs from the `magazine` bone that swings it away.
  - **Bolt:** its animations name a `bolt` bone in some places and a `Bolt` bone in others; it has no bolt part, so both hold nothing.
  - **Sights:** the owner's ring at the back of the receiver, over the post at the front.
  - **Aiming:** held 2 px further out: the shot's kick brought the back of its receiver to the eye.
- **Particle cues:** the Penny Pistol's and the Magpie SMG's reloads cue `reset_mag_position` and `resume_mag_tracking`, for a magazine the owner's mod slid along as its rounds were spent. Here they show nothing (`QUIET_CUES`), as `loaded` does. One of the Scrapper's carbine animations names `end_reload` with a space after it, so the check reads the names trimmed.
- **The arms:** chosen in first-person previews to cover as little of the screen as they can, still running back toward the shoulder: the Penny Pistol's both, and the two long guns' right arms, run straighter back. The long guns' left arms keep the usual direction, which a search could better only by a few per cent.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the knobs, the slide and the magazines through their animations; each with the attachments it takes; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Light Round and the Rifle Round; nothing new.
- **Parts:** copper ingots, an iron ingot and a lever, as the Rust Midge.
- **Their place:** the first guns, beside the Rust Midge, once copper and iron are smelted.

**Balance:** starting numbers, at point blank with every shot landing, over a load and its reload.
- **Penny Pistol:** 4 a shot, 13 a second while it fires; about 7 a second, as the Rust Midge's (7.5), below the Warden and Sentry Pistols' (9). A pair (slice 10G) does about 9.
- **Conductor Carbine:** 8 a shot; about 7.6 a second, above the lever rifles' (5 to 6) and below the Patchwork Carbine's (11), which fires faster but hits for less (6). It reaches as far as the Patchwork Carbine (96 blocks) and is nearly as steady aimed (0.3° against 0.25°).
- **Magpie SMG:** 2.5 a shot, 12.5 a second for its 6.4 s magazine; about 9 a second, between the Rust Midge's (7.5) and the Riveter SMG's (11), its magazine half as long again as the Rust Midge's.
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:penny_pistol`, `conductor_carbine` and `magpie_smg`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The side magazines do not move as rounds are spent:** the owner's mod slid the Birdfeeder's along as it emptied. At the start of the Magpie SMG's reload its magazine steps to where an empty one would be.
- **The Penny Pistol stays one-handed with a stock:** the owner's carbine animations for it are not used.
- **The Penny Pistol's hand** covers much of so small a pistol.
- **The Conductor Carbine and the Magpie SMG throw no cases:** their animations cue none.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 12C: the lone guns
The next of the owner's guns, taken up after slice 12B in the same round (on 13 October 2026 the owner asked again "Ok lets continue and do the next thing"): the UMAX Pistol, one of the two copper guns slice 12B left; the Ultra Knight Hawk, the one gun of their library made of iron; and the Forlorn Hope, the one made of echo shards. The other copper gun, the Whizzbanger, waits ([below](#the-whizzbanger-waits)).

| | Tuppence Pistol | Goshawk Revolver | Lament Rifle |
|---|---|---|---|
| The owner's gun | UMAX Pistol | Ultra Knight Hawk | Forlorn Hope |
| What it is | a copper pistol: a barrel over a fat tube, a receiver block at the back that slides with each shot, a lever loop under it, a ring sight over a front post | a long white revolver: a heavy barrel, a cylinder that swings out to the left, a hammer at the back | a sculk-grown rifle: a long barrel hung with sculk, a glowing echo-shard window in the receiver, a bolt handle on the right, a magazine under the barrel and a raked grip |
| Held | in one hand: it pairs with another (slice 10G) | in one hand: it pairs with another | in both hands |
| Fires | one shot each pull | one shot each pull | one shot each pull, the bolt worked after each |
| Damage | 5 | 9 | 16 |
| Rate | 3.3 a second (every 6 ticks) | 2 a second (every 10 ticks) | about 1 a second (every 19 ticks) |
| Holds | 8 | 6 | 5 |
| Reload | a round at a time: 0.4 s to start, 0.6 s a round, 0.25 s to finish (5.45 s from empty) | a round at a time: 0.5 s, 1 s a round, 0.7 s (7.2 s from empty) | 2.65 s: the magazine |
| Spread, hip / aimed | 2.5° / 1° | 2° / 0.6° | 2° / 0.05° |
| Range | 56 blocks | 64 | 160 |
| The view aimed | narrowed to 0.9 | 0.9 | 0.7 |
| Round | Light Round | Rifle Round | Rifle Round |
| Takes | the three stocks, the scopes and the Laser Sight | nothing (the owner made it no parts) | the three stocks, the Light and Vertical Grips, the four bayonets, the scopes and the Laser Sight |

The reload times and the Lament Rifle's rate are the owner's animations': its bolt is worked in full between shots.

**Crafting** (a crafting table; the guns switch, as every gun):
- **Tuppence Pistol:** three copper ingots over a copper ingot, a lever and an iron ingot, as the other copper guns take copper, iron and a lever.
- **Goshawk Revolver:** iron and a lever only: three iron ingots over a lever and an iron ingot, and one more iron ingot under that.
- **Lament Rifle:** as the sculk guns (slice 10D): three steel ingots over a lever between two echo shards, over a block of sculk.

**How they fire:** as the other bullet guns: each bullet goes from the eye along the look, strayed by the spread, to the first creature or block in its way. The Tuppence Pistol and the Goshawk Revolver load a round at a time, and a shot cuts the loading short, as the other round-at-a-time guns do; the Lament Rifle's magazine loads at the end of its reload. Nothing else follows a hit.

**What you see:** the owner's animations.
- **Tuppence Pistol:** held out in the right hand; each shot kicks it and drives the receiver block back, throwing a case. To reload, the gun is rolled and the left hand brings each round up from below into the gate under the back of the tube; as the reload ends the block is worked, throwing a case. In the inspection the lever under it swings down and back.
- **Goshawk Revolver:** held out in the right hand; each shot kicks it up hard and flicks the hammer. To reload, the gun is rolled and the cylinder swung out to the left; the left hand comes in for each round, a case falling with each; then the cylinder is flicked shut.
- **Lament Rifle:** each shot kicks it back; then the gun cants and the bolt is drawn back and pushed home. To reload, the gun is turned; the magazine under the barrel swings down about its front, a new one goes in and is slapped home; then the bolt is worked.
- **Spent rounds:** the Tuppence Pistol's animations cue a case with each shot and one as its reload ends; the Goshawk Revolver's one each time its loading loop runs; the Lament Rifle's none. (How many the client shows depends on how far each animation plays: in CI, 1 and 3 over a shot and a one-round reload.)
- **Sounds:** the Sapper Revolver's shot (the Tuppence Pistol), the Bulldog Pistol's (the Goshawk Revolver) and the Picket Rifle's (the Lament Rifle). The library's shots not used yet are silenced takes, rocket, flamethrower and jetpack sounds and wind-ups, or carry tags naming another source (slice 11A), so none was added.

**How the models were built:**
- **Tuppence Pistol:**
  - **One hand:** its idle hides the left arm, so it is among the one-handed guns (`JugcraftGuns.ONE_HANDED`, slice 10G); the left hand's place is given for the reload's push of a round into the gate (0.2917 s, `"hand_pose"`).
  - **Bolt:** the receiver block at the back and the knob behind it (the main part's 1st, 2nd and 21st elements).
  - **Lever:** the loop under the receiver, a flat plane (the main part's 4th element), turned about its front.
  - **Round:** a Light Round the reload carries in (`PROPS`), resting behind the mouth of the tube, where the loop's last carry brings it; it shrinks to nothing there. The `magazine` bone its shots scale holds nothing.
  - **Sights:** the owner's ring at the back (its sights part) over the post at the front.
  - **Aiming:** held 1 px further out (`"eye_relief"`): the receiver block's travel brought it to 0.8 px from the eye.
- **Goshawk Revolver:**
  - **One hand:** its idle hides the left arm, so it pairs too; the left hand's place is given at the back of the swung-out cylinder, for the loop's push of a round (0.4583 s).
  - **Cylinder:** the owner's drum part on the `magazine` bone, turned about the crane under its left side, so it swings out to the left; the `bolt`, `seal` and `shell` bones its animations move hold nothing.
  - **Hammer:** the owner's hammer part, a flat plane at the back, turned about its foot.
  - **Sights:** the notch at the back over the post at the front.
  - **Aiming:** at the hip's depth: its hammer stays 1.7 px from the eye.
- **Lament Rifle:**
  - **Bolt:** the owner's bolt group, the handle on the right of the receiver.
  - **Magazine:** the owner's mag group under the barrel, turned about its front; the `empty_mag` bone the reload throws out holds nothing, nor does the `seal`.
  - **Texture:** the owner's is seven frames, one above another, its echo-shard window glowing from frame to frame; the gun draws on the first (`FRAMED`, as the sculk guns do).
  - **Sights:** the owner's ring at the back of the receiver over the post on its front.
  - **Aiming:** held 6 px further out: as the bolt is worked the gun cants, and the back of its grip came 4 px past the eye.
- **The arms:** chosen in first-person previews to cover as little of the screen as they can, still running back toward the shoulder: both of the two hand guns', the Goshawk Revolver's right arm chosen over the flick that shuts its cylinder and its inspection as well. The Lament Rifle's keep the usual directions: its right arm stays behind the gun, and a search could better its left only by a few per cent.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the block, the lever, the round, the cylinder, the bolt and the magazine through their animations; each with the attachments it takes; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Light Round and the Rifle Round; nothing new.
- **Parts:** copper, iron and a lever (the Tuppence Pistol); iron and a lever (the Goshawk Revolver); steel, a lever, echo shards and sculk (the Lament Rifle).
- **Their place:** the Tuppence Pistol among the first guns, with the copper guns; the Goshawk Revolver once iron is smelted, needing no brass, unlike the other revolvers; the Lament Rifle with the sculk guns, past steel and the deep dark.

**Balance:** starting numbers, at point blank with every shot landing, over a load and its reload.
- **Tuppence Pistol:** 5 a shot, 17 a second while it fires; about 5.3 a second, below the Penny Pistol's (7), above the Sapper Revolver's (4.4). It can be topped up a round at a time.
- **Goshawk Revolver:** 9 a shot, 18 a second while it fires; about 5.6 a second, above the other revolvers' (the Sapper's 4.4, the Marshal's 2.8) and the Bulldog Pistol's (4.9).
- **Lament Rifle:** 16 a shot, twice the Picket Rifle's; about 12.4 a second, as the Kestrel Rifle's (12.2), below the Picket's (13.9). It reaches further than any (160 blocks) and is the steadiest aimed (0.05°).
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:tuppence_pistol`, `goshawk_revolver` and `lament_rifle`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Lament Rifle throws no cases, nor an empty magazine:** its animations cue no cases, and the owner made no part for the magazine its reload throws out.
- **The Lament Rifle's window does not glow from frame to frame:** the gun draws on the first of the seven.
- **The Goshawk Revolver takes nothing:** the owner made no parts for it.
- **Not played:** none of it has been played yet. The numbers want play to set.

### The Whizzbanger waits
The owner's other copper gun is a hand cannon fired by striking a pin with a hammer held in the left hand; after each shot the left hand puts the hammer away, tips the spent shell out and loads the next. Its animations move the hammer and the cannon's body about pivots of the model the owner's mod animated, which is not in the library: built from the item model, the hammer comes nowhere near the pin it strikes. A fit of the hammer's pivot to land its face on the pin at the shot found none that also held the hammer sensibly at rest. With the owner's animated model (its `.geo.json` or Blockbench project), it can follow as its own slice.

## Slice 13A: the frontier guns
The next of the owner's guns, taken up after slice 12C in the same round (on 13 October 2026, with slice 12C up for review, the owner asked again "Ok lets continue and do the next thing"): three of the fourteen guns in their library whose item models name plain stone as their base texture, the Micina, the Red Raydar and the Spirulida. A revolver, a lever-action rifle and a lever-cycled carbine, they are named for the frontier.

| | Tabby Revolver | Lookout Rifle | Ammonite Carbine |
|---|---|---|---|
| The owner's gun | Micina | Red Raydar | Spirulida |
| What it is | a gunmetal revolver: a slotted top strap over the barrel, a cylinder that swings out to the left, a hammer at the back and a wooden grip | a lever-action rifle: a long barrel over a fore-end, a lever loop under the receiver and a raked stock | a carbine: a bundle of barrels caged in copper bands, a long curved magazine hooked under it, a lever loop under the grip and a cocking piece on top at the back |
| Held | in one hand: it pairs with another (slice 10G) | in both hands | in both hands |
| Fires | one shot each pull | one shot each pull, the lever worked after each | one shot each pull |
| Damage | 6 | 7 | 5 |
| Rate | 2.5 a second (every 8 ticks) | about 1.5 a second (every 13 ticks) | 2.5 a second (every 8 ticks) |
| Holds | 6 | 8 | 16 |
| Reload | a round at a time: 0.45 s to start, 0.6 s a round, 0.65 s to finish (4.7 s from empty) | a round at a time: 0.65 s, 0.45 s a round, 0.85 s (5.1 s from empty) | 3 s: the magazine |
| Spread, hip / aimed | 2.5° / 0.8° | 2° / 0.2° | 2.5° / 0.4° |
| Range | 56 blocks | 112 | 88 |
| The view aimed | narrowed to 0.9 | 0.8 | 0.85 |
| Round | Light Round | Rifle Round | Rifle Round |
| Takes | the Silencer, the Baffled Silencer, the Muzzle Brake and the Extended Barrel | the three stocks, the Light and Tactical Grips, the four bayonets, the scopes and the Laser Sight | the three stocks |

The reload times and the rates are the owner's animations': the Lookout Rifle's lever is worked in full between shots.

**Crafting** (a crafting table; the guns switch, as every gun):
- **Tabby Revolver:** iron and brass, as the other revolvers: an iron ingot, a brass ingot and an iron ingot over a lever and a plank.
- **Lookout Rifle:** as the lever rifles: two iron ingots and a brass ingot over a lever between two planks, for its longer stock.
- **Ammonite Carbine:** three copper ingots, for its copper bands, over an iron ingot, a lever and a plank.

**How they fire:** as the other bullet guns: each bullet goes from the eye along the look, strayed by the spread, to the first creature or block in its way. The Tabby Revolver and the Lookout Rifle load a round at a time, and a shot cuts the loading short, as the other round-at-a-time guns do; the Ammonite Carbine's magazine loads at the end of its reload. Nothing else follows a hit.

**What you see:** the owner's animations.
- **Tabby Revolver:** held out in the right hand. Drawn, it is tossed end over end and caught. Each shot kicks it and drops the hammer. To reload, the gun is rolled and the cylinder swung out to the left; the left hand comes in for each round, a case falling with each; then the cylinder is flicked shut.
- **Lookout Rifle:** each shot kicks it; then the gun cants and the lever is swung down and back, throwing a case. To reload, the gun is rolled and the left hand pushes each round into the gate; as the reload ends the lever is worked again.
- **Ammonite Carbine:** each shot kicks it and drives the cocking piece back, throwing a case. To reload, the gun is tipped; the curved magazine is pulled down and away, a new one swung up into place and slapped home; then the lever is swung down, drawing the cocking piece back, and pushed home.
- **Spent rounds:** the Tabby Revolver's animations cue a case each time its loading loop runs; the Lookout Rifle's one with each shot and one as its reload ends; the Ammonite Carbine's one with each shot.
- **Sounds:** the Marshal Revolver's shot (the Tabby Revolver), the Longhorn Rifle's (the Lookout Rifle) and the Ranger Rifle's (the Ammonite Carbine). The Lookout Rifle's animation cues the magazine going in as each round goes in; it plays the shell-in sound there, as the Longhorn and Drover Rifles do (`GUN_SOUND_ALIASES`). The library's shots not used yet are silenced takes, rocket, flamethrower and jetpack sounds and wind-ups, or carry tags naming another source or tool (slices 11A and 12C), so none was added.

**How the models were built:**
- **Tabby Revolver:**
  - **One hand:** its idle hides the left arm, so it is among the one-handed guns (`JugcraftGuns.ONE_HANDED`, slice 10G); the left hand's place is given at the back of the swung-out cylinder, for the loop's push of a round (0.3016 s, `"hand_pose"`).
  - **The toss:** the owner put every part of it on a second body bone (`gun_body_2`), which its draw alone turns, end over end, about the frame under the cylinder; the other animations move the outer `gun_body`, about the grip.
  - **Cylinder:** the owner's drum part on the `magazine` bone, turned about the crane under its left side, so it swings out to the left, as the Goshawk Revolver's; the `bolt`, `seal` and `shell` bones its animations move hold nothing.
  - **Hammer:** the owner's hammer part, a flat plane at the back, turned about its foot: it stands cocked and falls forward at each shot.
  - **Barrel:** its standard barrel is the owner's part, so the barrel attachments fit it; the Baffled Silencer, the owner's part for it, stands 0.3 px clear of the barrel's end.
  - **Sights:** the notch at the back of the frame over the post at the front.
  - **Aiming:** at the hip's depth: its hammer stays 3.9 px from the eye.
- **Lookout Rifle:**
  - **Lever:** the loop under the receiver (the main part's element the owner named "lever"), turned about its front where it meets the receiver; the `bolt` and `shell` bones hold nothing (its reload keeps the round at scale 0).
  - **Sights:** the owner's notch at the back of the receiver over the post at the muzzle; the scopes stand on the receiver in its place.
  - **Aiming:** held 3.5 px further out (`"eye_relief"`): as the lever is worked the gun cants, and the back of its stock came 1.8 px past the eye.
- **Ammonite Carbine:**
  - **Magazine:** the curved magazine under it (the main part's elements the owner named "default_mag") on the `Magazine` bone, turned about where it meets the receiver.
  - **Lever:** the loop under the grip (named "lever"), turned about its front.
  - **Bolt:** the cocking piece on top at the back (the main part's 41st to 43rd elements), drawn back with each shot and by the lever.
  - **Empty bone:** the `gun_body2` bone its reload slides forward as the lever is worked holds nothing.
  - **Sights:** the owner's notch at the back of the receiver.
  - **Aiming:** held 3 px further out: the back of its stock came 1.4 px past the eye.
- **The arms:** chosen in first-person previews to cover as little of the screen as they can, still running back toward the shoulder: the Tabby Revolver's right arm for its hold, its shots, its loading and its inspection, its left for its loading; the two long guns' for their hold, their shots, their reloads and their inspections.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the hammer, the cylinder, the lever, the magazine and the cocking piece through their animations; each with the attachments it takes; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Light Round and the Rifle Round; nothing new.
- **Parts:** iron, brass, a lever and a plank (the Tabby Revolver); iron, brass, a lever and planks (the Lookout Rifle); copper, iron, a lever and a plank (the Ammonite Carbine).
- **Their place:** the Tabby Revolver and the Lookout Rifle with the other revolvers and the lever rifles, which take brass too; the Ammonite Carbine among the first guns, with the copper guns.

**Balance:** starting numbers, at point blank with every shot landing, over a load and its reload.
- **Tabby Revolver:** 6 a shot, 15 a second while it fires; about 5.4 a second, as the Tuppence Pistol's (5.3), below the Goshawk Revolver's (5.6), above the Sapper Revolver's (4.4), which it matches in its reload and its attachments but outhits (6 to 4.5).
- **Lookout Rifle:** 7 a shot, about 11 a second while it fires; about 5.8 a second, between the Drover Rifle's (5) and the Longhorn Rifle's (6.3). It holds two fewer than the Drover Rifle and hits a little harder, and reaches 112 blocks to the Drover's 100 and the Longhorn's 120.
- **Ammonite Carbine:** 5 a shot, 12.5 a second while it fires; about 8.9 a second, as the Magpie SMG's (9), below the Patchwork Carbine's (11.8), which reaches further (96 blocks to 88) but holds ten to its sixteen.
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:tabby_revolver`, `lookout_rifle` and `ammonite_carbine`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Ammonite Carbine's `gun_body2` moves nothing:** its reload slides that bone forward as the lever is worked, but no part of the owner's model is named for it.
- **The Baffled Silencer on the Tabby Revolver** stands 0.3 px clear of the barrel, as the owner's part has it.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 13B: more frontier guns
The next of the owner's guns, taken up after slice 13A (on 14 October 2026, with slice 13A up for review, the owner asked "Ok lets continue and do the next thing or what should we do?"): three more of the guns whose item models name plain stone as their base texture, the Fencer Carabine, the Callwell Terminal and the Saketini. A lever-action carbine loaded through its stock, a lever-action shotgun and a single-shot rifle, they follow slice 13A's frontier guns. Their fourth, the Fencer Thumper, waits ([below](#the-fencer-thumper-waits)).

| | Fencepost Carbine | Waystation Shotgun | Highball Rifle |
|---|---|---|---|
| The owner's gun | Fencer Carabine | Callwell Terminal | Saketini |
| What it is | a lever-action carbine: a long barrel over a fore-end, a hammer on the right of the lock, a lever loop under it and a stock with a lid on top that opens to load the tube inside | a lever-action shotgun without a shoulder stock: a barrel over a long fore-end, a drum under the action and a lever loop behind it | a single-shot rifle: a long barrel that swings aside to be loaded, a hammer on the right of the lock, a lever loop under it and a raked stock |
| Held | in both hands | in both hands | in both hands |
| Fires | one shot each pull, the lever worked and the hammer cocked after each | eight pellets each pull, the lever worked after each | one shot, then a reload |
| Damage | 10 | 3 a pellet | 15 |
| Rate | about 1.1 a second (every 18 ticks) | about 1.2 a second (every 17 ticks) | one a load |
| Holds | 7 | 8 | 1 |
| Reload | a round at a time: 0.75 s to start, 1 s a round, 1.35 s to finish (9.1 s from empty) | a shell at a time: 0.45 s, 0.65 s a shell, 0.9 s (6.55 s from empty) | 1.65 s |
| Spread, hip / aimed | 2° / 0.3° | 6.5° / 4.5° | 2° / 0.1° |
| Range | 104 blocks | 28 | 128 |
| The view aimed | narrowed to 0.82 | 0.9 | 0.75 |
| Round | Rifle Round | Buckshot Shell | Rifle Round |
| Takes | the Light and Tactical Grips and the four bayonets | those and the barrel attachments | those, the barrel attachments, the scopes and the Laser Sight |

The reload times and the rates are the owner's animations'.

**Crafting** (a crafting table; the guns switch, as every gun):
- **Fencepost Carbine:** two iron ingots and a brass ingot over a lever and two planks, as the lever guns.
- **Waystation Shotgun:** a brass ingot and two iron ingots over an iron ingot (its drum), a lever and a plank. (First set as two iron ingots and a brass ingot over the same, which was the Mule's recipe; changed so the two do not clash.)
- **Highball Rifle:** three steel ingots over a lever and a plank, a marksman's rifle of the steel tier.

**How they fire:** as the other bullet guns, and the Waystation Shotgun as the other shotguns: each bullet or pellet goes from the eye along the look, strayed by the spread, to the first creature or block in its way. The Fencepost Carbine and the Waystation Shotgun load a round at a time, and a shot cuts the loading short; the Highball Rifle's round goes in at the end of its reload. Nothing else follows a hit.

**What you see:** the owner's animations.
- **Fencepost Carbine:** each shot kicks it, throwing a case, and drops the hammer; then the lever is swung down and back and the hammer cocked. To reload, the gun is rolled and the lid on the stock flipped open; the right hand brings each round back to it; then the lid is shut and the lever worked.
- **Waystation Shotgun:** each shot kicks it, throwing a case; then the gun cants and the lever is swung down and back. To reload, the gun is rolled and the left hand carries each shell up to the back of the drum; as the reload ends a case is thrown and the lever worked.
- **Highball Rifle:** each shot kicks it and drops the hammer, and the right hand cocks it again. To reload, the gun is turned and the barrel swung aside, throwing a case; a round goes in and the barrel is swung home.
- **Spent rounds:** the Fencepost Carbine's and the Waystation Shotgun's animations cue a case with each shot, and the shotgun's one more as its reload ends; the Highball Rifle's one each reload.
- **Sounds:** the Kestrel Rifle's shot (the Fencepost Carbine), the Coach Gun's (the Waystation Shotgun; the owner's Callwell, of which the Terminal is a kind) and the Thresher's (the Highball Rifle). The Fencepost Carbine's and the Waystation Shotgun's animations cue the magazine going in as each round or shell goes in; each plays the shell-in sound there, as the other lever guns do (`GUN_SOUND_ALIASES`). The library's shots not used yet are silenced takes, rocket, flamethrower and jetpack sounds and wind-ups, or carry tags naming another source or tool (slices 11A and 12C), so none was added.

**How the models were built:**
- **Fencepost Carbine:**
  - **Lid:** the plate on top of the stock (the main part's elements the owner put in their lid group, the 20th and 21st), turned up and over about its front.
  - **Hammer:** the owner's hammer part on the right of the lock, turned about its foot: it falls forward at the shot and is cocked again as the lever closes.
  - **Lever:** the loop under the lock (the main part's 24th element), turned about its front.
  - **Empty bones:** the `bolt` and `flash` bones its animations move hold nothing.
  - **Sights:** the notch on the receiver over the post at the muzzle.
  - **Aiming:** held 6 px further out (`"eye_relief"`), as the Lament Rifle: as the lever is worked the gun cants back toward the eye. Its long stock still reaches 1 px past the eye then, but 2 px or more below the line of sight, out of view.
- **Waystation Shotgun:**
  - **Lever:** the loop behind the drum (the main part's element the owner named "lever"), turned about its front.
  - **Shell:** the shell the loading loop carries in is the owner's (the main part's element they named "shell"), resting inside the fore-end; the renderer shows it only while the loop moves it.
  - **Barrel:** its standard barrel is the owner's part, so the barrel attachments fit it.
  - **Sights:** the notch on the receiver over the post at the muzzle.
  - **Aiming:** held 1 px further out: as it kicks, the back of its grip came 0.9 px from the eye.
- **Highball Rifle:**
  - **Barrel:** the owner's standard barrel part, swung aside about a hinge under the left of the breech.
  - **Hammer:** the owner's hammer part on the right of the lock, turned about its foot.
  - **Lever:** the loop under the lock (the main part's 13th element), turned about its front in the inspection.
  - **Empty bone:** the `bolt` bone its draw names holds nothing.
  - **Sights:** the owner's notch over the post at the muzzle; the scopes stand on the receiver in its place.
  - **Aiming:** held 3 px further out: the back of its stock came 1.2 px past the eye.
- **The arms:** chosen in first-person previews to cover as little of the screen as they can, still running back toward the shoulder, through each gun's hold, shot, reload and inspection.
  - **The Fencepost Carbine's right arm** mattered most: its hand carries each round back to the lid on the stock, within a few pixels of the eye. There the game leaves an arm out while any of it is within a tenth of a block of the eye, and draws it again as it leaves.
  - **First choice:** in CI ([run 38500777704](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38500777704)), the reload screenshot showed it across the right half of the view, just outside that reach. Checked against the game's own rule at every hundredth of a second, it covered a fifth to two fifths of the view for about a tenth of a second twice in every round's loading.
  - **Now:** it runs down and in from the hand. It is left out for most of each round's loading, and covers at most a sixth of the view when drawn. Its hold, shot and inspection look as before. In CI ([run 38506772653](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38506772653)), the reload screenshot shows the gun rolled with its lid open and the arm out of the view.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the lid, the hammers, the levers, the shell and the swinging barrel through their animations; each with the attachments it takes; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Rifle Round and the Buckshot Shell; nothing new.
- **Parts:** iron, brass, a lever and planks (the Fencepost Carbine and the Waystation Shotgun); steel, a lever and a plank (the Highball Rifle).
- **Their place:** the Fencepost Carbine and the Waystation Shotgun with the other lever guns, which take brass too; the Highball Rifle with the marksman rifles, past steel.

**Balance:** starting numbers, at point blank with every shot (and pellet) landing, over a load and its reload.
- **Fencepost Carbine:** 10 a shot, the hardest of the lever rifles (the Longhorn Rifle's 8); about 4.8 a second, as the Drover Rifle's (5). It is the slowest of them to load, a round a second.
- **Waystation Shotgun:** 24 a shot; about 15.4 a second, as the Throttle's (15.2), between the Highwayman's (13.7) and the Haymaker's (17). It holds eight, more than any shotgun loaded a shell at a time.
- **Highball Rifle:** 15 a shot; about 9.1 a second, below the Skylark Rifle's (10), a single-shot rocket rifle, and the marksman rifles' (12 to 15), which hold eight or ten. It reaches as far as the furthest of them (128 blocks) and aims as steadily as the Picket Rifle (0.1°).
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:fencepost_carbine`, `waystation_shotgun` and `highball_rifle`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Fencepost Carbine's `flash` bone shows nothing:** its shot scales that bone up and away, a flash at the lock the owner's mod drew; here the muzzle flash every gun shows stands for it.
- **The Highball Rifle's barrel swings aside about a hinge chosen here:** the owner's model gave no pivot for it.
- **The Fencepost Carbine's right arm at the end of a reload:** as the lever is worked, it passes close to the eye. For about three hundredths of a second it covers much of the view, as it did with the first choice of arm.
- **Not played:** none of it has been played yet. The numbers want play to set.

### The Fencer Thumper waits
The owner's other Fencer gun is a revolving gun whose cylinder turns with each shot. Its animations cue the turn ("rotate_cylinder") for the owner's mod to make in code, and nothing here turns a cylinder from a cue yet; built now, its cylinder would stand still. It can follow as its own slice, with that turn added.

## Slice 13C: a punt gun, a repeater and a heavier rifle
The next of the owner's guns, taken up after slice 13B (on 14 October 2026, with slice 13B up for review, the owner asked "What could we work on next?" and, offered this set among others, answered "continue"): three more of the guns whose item models name plain stone as their base texture, the Big Bore, the Repeating Musket and the Winnie Millend. A punt gun, a flintlock repeater and a heavier lever-action rifle, they follow slices 13A's and 13B's frontier guns.

| | Flatboat Gun | Windlass Musket | Millstone Rifle |
|---|---|---|---|
| The owner's gun | Big Bore | Repeating Musket | Winnie Millend |
| What it is | a punt gun: a long, wide barrel ahead of a block-shaped chamber with a handle on its right, a hammer over it and a short stock | a flintlock repeater: a short barrel, a drum across the breech with the magazine tube through it standing out to the left, a hammer and pan on the right and a stock | a heavy lever-action rifle: a long barrel over a fore-end, a lever loop under the lock and a stock |
| Held | in both hands | in both hands | in both hands |
| Fires | sixteen pellets, then a reload | one shot each pull, the drum turned after each | one shot each pull, the lever worked after each |
| Damage | 3 a pellet | 6 | 8.5 |
| Rate | one a load | 2.5 a second (every 8 ticks) | about 1.3 a second (every 15 ticks) |
| Holds | 1 | 7 | 8 |
| Reload | 3.6 s | 3.45 s, the magazine | a round at a time: 0.4 s to start, 0.65 s a round, 0.85 s to finish (6.45 s from empty) |
| Spread, hip / aimed | 8° / 6° | 3° / 1° | 2° / 0.25° |
| Range | 36 blocks | 56 | 108 |
| The view aimed | narrowed to 0.9 | 0.85 | 0.78 |
| Round | Buckshot Shell | Paper Cartridge | Rifle Round |
| Takes | the scopes and the Laser Sight | the Light and Vertical Grips and the four bayonets | the barrel attachments, the Light and Tactical Grips, the four bayonets, the scopes and the Laser Sight |

The reload times and the rates are the owner's animations'.

**Crafting** (a crafting table; the guns switch, as every gun):
- **Flatboat Gun:** a block of iron and two iron ingots over a brass ingot, a lever and a plank, the only gun's recipe to take a block of iron.
- **Windlass Musket:** two iron ingots and a brass ingot over a flint, a lever and a plank; the flint for its lock, as the other flintlocks.
- **Millstone Rifle:** a steel ingot, an iron ingot and a brass ingot over a lever and a plank: the lever rifle of the steel tier.

**How they fire:** as the other bullet guns, and the Flatboat Gun as the other shotguns: each bullet or pellet goes from the eye along the look, strayed by the spread, to the first creature or block in its way. The Flatboat Gun's shell and the Windlass Musket's magazine go in at the end of their reloads; the Millstone Rifle loads a round at a time, and a shot cuts the loading short. Nothing else follows a hit.

**What you see:** the owner's animations.
- **Flatboat Gun:** each shot kicks it and works the hammer. To reload, the chamber is swung out to the left, throwing the spent shell; the left hand carries a new shell up into it, and the chamber is swung home.
- **Windlass Musket:** each shot kicks it with a flash at the pan, throwing a case, and the drum across the breech is turned for the next. To reload, the left hand draws the magazine out to the left and away, brings it back and slaps it home, and the drum is turned.
- **Millstone Rifle:** each shot kicks it, throwing a case, and the lever is swung down and back. To reload, the gun is tipped and each round pushed in; as the reload ends a case is thrown and the lever worked.
- **Spent rounds:** the Flatboat Gun's animation cues one as its chamber opens; the Windlass Musket's one with each shot; the Millstone Rifle's one with each shot and one more as its reload ends.
- **Sounds:** the Culverin's shot (the Flatboat Gun), the Line Musket's (the Windlass Musket) and the Bastion Rifle's (the Millstone Rifle). The Flatboat Gun's and the Millstone Rifle's animations cue the magazine going in as the shell or round goes in; each plays the shell-in sound there (`GUN_SOUND_ALIASES`). The library's shots not used yet are silenced takes, rocket, flamethrower and jetpack sounds and wind-ups, or carry tags naming another source or tool (slices 11A and 12C), so none was added.

**How the models were built:**
- **Flatboat Gun:**
  - **Texture:** its item model names the owner's `punt_gun.png`, not a texture of its own name; `tools/guns.py` now takes a gun's texture by another name where the owner's model names one (`OWN_TEXTURES`).
  - **Chamber:** the block behind the barrel (the main part's 3rd to 9th elements, the handle on its right among them), swung out to the left about a hinge along its lower left edge.
  - **Shell:** no part of the owner's model is named for it. A buckshot shell is drawn into a free corner of its texture and carried into the chamber from the left; the renderer shows it only while the reload moves it.
  - **Hammer:** the owner's hammer part.
  - **Empty bone:** the `no_sights` bone only its inspection moves holds nothing.
  - **Sights:** the owner's peep ring on the chamber's top; a scope stands on the owner's rail in its place.
  - **Aiming:** held 6 px further out (`"eye_relief"`): the back of its stock came 2.4 px past the eye. Held there, the nearest of it is 3.6 px away.
- **Windlass Musket:**
  - **Drum:** the main part's 25th and 27th to 35th elements, turned about the axis of the magazine that runs through it.
  - **Magazine:** the owner's magazine part, the tube through the drum.
  - **Bolt:** the block behind the drum with the plate and knob on top (the main part's 6th, 8th and 9th elements).
  - **Hammer:** the owner's hammer part.
  - **Flash:** no part of the owner's model is named for it; a burst of priming fire is drawn at the pan, as for the other flintlocks.
  - **Grips:** the owner named its Vertical Grip part `grip_vert`, which the Vertical Grip now finds too. Its grips and bayonets use the owner's musket grip and bayonet textures.
  - **Sights:** the notch between the two posts on the drum. The owner made no rail for a scope.
  - **Aiming:** held 3 px further out: the back of its stock came to the eye. Held there, it is 3 px away.
- **Millstone Rifle:**
  - **Lever:** the loop under the lock (the main part's element the owner named "lever"), turned about its front.
  - **Barrel:** its standard barrel is the owner's part, so the barrel attachments fit it.
  - **Empty bone:** the `bolt` bone its draw moves holds nothing.
  - **Sights:** the owner's peep ring at the back; the scopes stand on the receiver in its place.
  - **Aiming:** held 5 px further out: as it kicks, the back of its stock came 1.7 px past the eye. Held there, it is 3.3 px away.
- **The arms:** chosen in first-person previews, with the game's rule that leaves an arm out while any of it is within a tenth of a block of the eye, to cover as little of the screen as they can through each gun's hold, shot, reload and inspection.
  - **The Flatboat Gun's right hand** rests where the Drover Rifle's does: the owner's idle pose for it holds the gun as the Winnie's (the Drover Rifle's) holds it.
  - **The Flatboat Gun's left arm,** as it carries the shell up to the chamber, covers about a quarter of the view at the height of the reach and about an eighth for the half second after. Every direction tried for it did the same: the owner's animation brings the hand up beside the eye there.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the chamber and shell, the drum, magazine and flash, and the lever through their animations; each with every attachment it takes; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Buckshot Shell, the Paper Cartridge and the Rifle Round; nothing new.
- **Parts:** iron (a block of it for the Flatboat Gun), brass, a lever and a plank; a flint (the Windlass Musket); steel (the Millstone Rifle).
- **Their place:** the Flatboat Gun with the shotguns, past brass; the Windlass Musket with the black powder guns, the first of them to hold more than two; the Millstone Rifle with the lever rifles, past steel.

**Balance:** starting numbers, at point blank with every shot (and pellet) landing, over a load and its reload.
- **Flatboat Gun:** 48 a shot, the most of any shotgun in one pull (the Reverb's 35); about 13.3 a second, as the Highwayman's (13.7). It fires once a load and reaches 36 blocks, as the Crackling.
- **Windlass Musket:** 6 a shot; about 7.2 a second, above the Line Musket's (3.6), the Bellmouth's (6.4) and the Culverin's (6.8), below the Fowler's (9).
- **Millstone Rifle:** 8.5 a shot, harder than the Drover Rifle (6.5) and the Longhorn Rifle (8); about 5.8 a second, as the Lookout Rifle's, between the Drover's (5) and the Longhorn's (6.3).
- **A round:** each spends one a shot, the Flatboat Gun one shell of sixteen pellets; nothing converts back.

**Save compatibility:** new items `jugcraft:flatboat_gun`, `windlass_musket` and `millstone_rifle`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Flatboat Gun's chamber swings about a hinge chosen here:** the owner's model gave no pivot for it.
- **The Flatboat Gun's left arm as the shell goes in:** about a quarter of the view for a moment (above).
- **The Windlass Musket's `glow` bone shows nothing:** its shot moves it, a glow at the pan the owner's mod drew; the priming flash stands for it.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 13D: a revolving launcher, a carbine revolver and a musket of light
The next of the owner's guns, taken up after slice 13C (on 14 October 2026, with slice 13C up for review, the owner asked "Ok what sohuld we work on next" and, offered this set first among others, answered "Yeah lets do slice 13D then when thats done suggest 2 and 3 again"): three more of the guns whose item models name plain stone as their base texture, the Fencer Thumper, the Pax and the Laser Musket. A revolving grenade launcher, a revolver that takes a stock and an energy musket, they follow slices 13A to 13C's frontier guns.

| | Fencepost Thumper | Parley Revolver | Lantern Musket |
|---|---|---|---|
| The owner's gun | Fencer Thumper | Pax | Laser Musket |
| What it is | a revolving grenade launcher: a short, wide barrel over a pump with a handle out to its left, an eight-sided cylinder behind the barrel, a hammer and a sloping stock | a single-action revolver: a barrel, a cylinder, a hammer and a grip; the owner made three stocks for it | an energy musket: a long barrel caged in rings, a glass core glowing at the breech and a stock |
| Held | in both hands | in one hand; in both, as a carbine, with a stock fitted | in both hands |
| What it fires | grenades, any kind (as the Trench Lobber, [slice 9G](#slice-9g-the-trench-lobbers-grenades)) | Rifle Rounds | a beam on Energy Cells, through every creature in its line (as the Beam Pistol's, [slice 8D](#slice-8d-the-energy-weapons)) |
| Damage | the grenade's own burst: a frag Grenade's 16 at its centre | 10 | 12 to each creature in the line |
| Rate | about 1.1 a second (every 18 ticks), the pump worked after each | 1.25 a second (every 16 ticks) | about 2.2 a second (every 9 ticks) |
| Holds | 4 grenades | 6 | 3 charges, 900 JE each |
| Reload | a grenade at a time: 0.6 s to start, 1.1 s a grenade, 0.95 s to finish (5.95 s from empty) | a round at a time: 0.5 s to start, 0.65 s a round, 0.7 s to finish (5.1 s from empty) | 2.2 s, the core |
| Spread, hip / aimed | 3° / 1.25° | 2.5° / 0.6° | 2° / 0.3° |
| Range | 20 (how far a level shot carries, as the Bullfrog's) | 72 blocks | 96 |
| The view aimed | narrowed to 0.9 | 0.9 | 0.8 |
| Takes | the Light and Vertical Grips (on its pump) and the four bayonets | the barrel attachments and the three stocks | nothing (the owner made it none) |

The reload times and the rates are the owner's animations'. The Thumper's four chambers are chosen here: the owner's cylinder is eight-sided and shows no chambers on its faces.

**Crafting** (a crafting table; the guns switch, as every gun):
- **Fencepost Thumper:** two iron ingots and a brass ingot over a brass ingot, a lever and a plank, with a brass ingot under them for its cylinder (without it, the grid is the Marshal Revolver's).
- **Parley Revolver:** a steel ingot and two iron ingots over a lever and a brass ingot: the revolver of the steel tier.
- **Lantern Musket:** two steel ingots and a lantern over a plank, an advanced circuit and a brass ingot; the machines switch too, as the other energy weapons.

**How they fire:**
- **Fencepost Thumper:** as the Trench Lobber and the Bullfrog: each shot lobs a grenade of the kind the cylinder holds, which goes off where it lands as that kind does and breaks no block. It loads any grenade, the one in the other hand first, a grenade at a time; loading another kind first empties the cylinder into the inventory, as the Lobber's magazine is. A shot cuts the loading short.
- **Parley Revolver:** as the other bullet guns, loaded a round at a time.
- **Lantern Musket:** as the Beam Pistol and the Solenoid Rifle: a beam from the eye along the look to the first block in range, through every creature in its line, each taking its damage. Its reload draws three charges from the Energy Cells in the inventory and leaves the cells.

**The Parley Revolver as a carbine:** with a stock fitted (the Light, Weighted or Wooden Stock, each the owner's own for it) it is held in both hands:
- **Its animations:** the owner's carbine_ set in place of its own: the draw, the idle with the left hand under the barrel, the shots, the inspection and the reload ([`GunAnimations`](../../src/client/java/io/github/jimbozoomer/jugcraft/client/guns/GunAnimations.java)). The change shows from its next animation; a stock goes on in a crafting grid, so taking the gun up again draws it as a carbine.
- **Seen from outside:** both arms come up to it, as to a rifle (`GunPose`).
- **Two guns at once:** a carbine is not one of two guns at once ([slice 10G](#slice-10g-two-guns-at-once)): with one, the other hand's gun does not fire, and its tooltip no longer says it pairs (`GunItem.carbine`, `JugcraftGuns.CARBINES`).
- **Its numbers:** the stock's, as on any gun; aimed, the stock is left out of the view past halfway, as every stock is ([slice 9F](#slice-9f-the-aiming-polish)).

**What you see:** the owner's animations.
- **Fencepost Thumper:** each shot kicks it and the hammer falls; the pump is worked back and forward, the left hand riding it, and the cylinder turns a chamber as the pump comes back (the owner's "rotate_cylinder" cue; the turn is drawn here from code, eased over three ticks). To reload, the barrel is tipped down about the hinge under the cylinder's front, raising the cylinder's back out of the frame; the left hand pushes each grenade in; the barrel is snapped shut and the pump racked.
- **Parley Revolver:** each shot kicks it up and the hammer is cocked. To reload, the cylinder swings out to the left and the left hand comes in to push each round in, throwing a case for each; the cylinder is swung home. As a carbine, the aimed shot cants the gun.
- **Lantern Musket:** each shot kicks it. To reload, the core is drawn out down and away and a new one put in and turned home.
- **Spent rounds:** the Parley Revolver's reload throws a case for each round it loads. The Thumper and the Lantern Musket throw none.
- **Sounds:** the Trench Lobber's shot (the Fencepost Thumper), the Goshawk Revolver's (the Parley Revolver) and the Linesman's (the Lantern Musket). The Thumper's animation cues the magazine going in as each grenade goes in; it plays the shell-in sound there, as the Bullfrog does. The Lantern Musket's core clanks home with the Squall Rifle's clank, not the ramrod's sound its cue names (`GUN_SOUND_ALIASES`). The Parley Revolver's shot cues the magazine-in sound as its hammer is cocked, and each round goes in with it, as the Tabby Revolver's and the Goshawk Revolver's do.

**How the models were built:**
- **Fencepost Thumper:**
  - **Barrel:** the main part's 5th to 14th elements, with the cylinder and the pump on it, tipped about the knuckle under the cylinder's front (the 12th). The owner's groups name a barrel, a cylinder and a pump, but their origins do not place them on the model (its groups also carry a musket's names: ram, ball, sights), so the hinge and the cylinder's middle are chosen from the elements.
  - **Cylinder:** the 15th to 18th elements, turned a quarter at each shot's cue about its middle (`GunEffects.cylinderTurn`); each client keeps the turn for the guns it draws.
  - **Pump:** the 19th to 22nd elements, the slide and the handle out to its left; the left hand rides it on a `left_arm2` bone, as the Bellmouth's does.
  - **Hammer:** the owner's hammer part; the main part's 30th element is the same plane and is left out.
  - **Attachments:** its grips ride the pump and its bayonets the barrel. `tools/guns.py`'s `"mounts"` can now name an attachment as well as a slot.
  - **Empty bones:** the `bolt`, `flash` and `lid` bones its animations move hold nothing.
  - **Sights:** the notch between the two posts on the back of the barrel.
  - **Aiming:** held 5.5 px further out (`"eye_relief"`): the hammer, fallen forward at the shot, came 1.6 px past the eye. Held there, it is 3.9 px away.
- **Parley Revolver:**
  - **Cylinder:** the owner's drum part, swung out to the left about the crane under it, as the Tabby Revolver's.
  - **Hammer:** the owner's hammer part, the flat plane at the back.
  - **Stocks:** the owner's three, behind the grip.
  - **Empty bones:** the `bolt`, `seal` and `shell` bones hold nothing; `gun_body2`, which the carbine's aimed shot cants, holds the whole gun.
  - **Left hand:** placed where it pushes a round into the swung-out cylinder; the carbine's hold then puts it under the barrel ahead of the frame.
  - **Sights:** the notch at the back over the post at the front.
  - **Aiming:** its nearest point stays 4.9 px from the eye; it is held where the hip view holds it.
- **Lantern Musket:**
  - **Textures:** one part on three of the owner's textures: its own, the glass and the core's three-frame glow (its first frame, as the Solenoid Rifle's glow).
  - **Core:** the 22nd and 23rd elements, the glass tube at the breech and the glow inside it.
  - **Glass:** most of its pixels are about four-fifths opaque. The guns are drawn as cutouts, so those show fully opaque; its clear windows show the glow.
  - **Empty bone:** the `bolt` bone only its draw names holds nothing.
  - **Sights:** the notch between the posts at the breech over the post at the muzzle.
  - **Aiming:** held 3.5 px further out: the back of its stock came within half a pixel of the eye. Held there, it is 4 px away.
- **The arms:** chosen in first-person previews, with the game's rule that leaves an arm out while any of it is within a tenth of a block of the eye, to cover as little of the screen as they can through each gun's hold, shot, reload and inspection; the Parley Revolver's left arm through its loading and its carbine hold.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the barrel, cylinder, pump and hammer, the drum, the core; the Parley Revolver with a stock and its carbine hold; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** grenades (any kind), the Rifle Round and the Energy Cell; nothing new.
- **Parts:** iron, brass, a lever and a plank (the Thumper); steel, iron and brass (the Parley Revolver); steel, an advanced circuit, a lantern, brass and a plank (the Lantern Musket).
- **Their place:** the Thumper with the grenade launchers, of iron and brass (the Bullfrog's are iron and copper, the Trench Lobber's steel); the Parley Revolver with the revolvers, past steel; the Lantern Musket with the energy weapons, past advanced circuits, its cells filled at the Charging Station or the Cell Rack.

**Balance:** starting numbers, at point blank with every shot landing, over a load and its reload.
- **Fencepost Thumper:** a frag Grenade's 16 a shot; about 7.4 a second, as the Bullfrog's (7.1), half the Trench Lobber's (15.6). Four in the cylinder, topped up a grenade at a time.
- **Parley Revolver:** 10 a shot, the hardest of the revolvers (the Goshawk Revolver's 9); about 6.6 a second, above the Goshawk's (5.6) and the Tabby Revolver's (5.4).
- **Lantern Musket:** 12 a shot to each creature in its line; about 11.6 a second, below the Solenoid Rifle's (12.1) and the Votive Rifle's (13.1), above the Beam Pistol's (9.2). It reaches 96 blocks, further than any energy weapon but the Solenoid Rifle (128).
- **Charge:** 900 JE a shot, 2,700 a magazine: a full Energy Cell (10,000 JE) loads eleven shots.
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:fencepost_thumper`, `parley_revolver` and `lantern_musket`; nothing saved changes. A Parley Revolver's stock is a fitted attachment, as on any gun. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Thumper's hinge and cylinder middle are chosen here:** the owner's groups did not place them.
- **The Thumper's four chambers are chosen here,** and each client turns its cylinder for the guns it draws: two players may see it at different chambers, which look the same.
- **A Parley Revolver becomes a carbine from its next animation:** fitting a stock takes it out of the hand, so in play that is its draw.
- **The Lantern Musket's glass draws opaque** where the owner's texture is four-fifths so.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 14A: the brass sidearms
The next of the owner's guns, taken up after slice 13D (on 14 October 2026, with slice 13D up for review, the owner was offered again their treated-brass guns or two small fixes, and answered "Lets do option 2 and then option 3"): the first three of the fourteen guns in their library whose item models name treated brass as their base texture, the Howler, the M22 Waltz and the Osgood 50. A heavy pistol, a target pistol and a break-top revolver, they are the brass sidearms. (The library's Grandle OG also names treated brass, but holds only a barrel's parts, with no body of its own, so it is not counted.)

| | Holler Pistol | Minuet Pistol | Half-Crown Revolver |
|---|---|---|---|
| The owner's gun | Howler | M22 Waltz | Osgood 50 |
| What it is | a heavy pistol: a long barrel, a box magazine ahead of the trigger guard, a bolt with a knob out to either side and a ring sight at the back; the owner made stocks, grips, bayonets and magazines for it | a light target pistol: a slim barrel in a shroud, a slide at the back of its top, a ring sight on a rail and the magazine in the grip | a break-top revolver: a barrel over a lug, a cylinder, a top strap, a hammer and a grip; the owner made three stocks for it |
| Held | in both hands, the left under the magazine | in one hand | in one hand; in both, as a carbine, with a stock fitted |
| What it fires | Rifle Rounds | Light Rounds | Rifle Rounds |
| Damage | 12 | 3.5 | 9 |
| Rate | about 0.9 a second (every 22 ticks), the bolt worked after each | 4 a second (every 5 ticks) | about 2.2 a second (every 9 ticks) |
| Holds | 10 | 10 | 6 |
| Reload | 2.9 s, the magazine | 2.4 s, the magazine | a round at a time: 0.45 s to start, 0.65 s a round, 0.7 s to finish (5.05 s from empty) |
| Spread, hip / aimed | 3° / 0.6° | 2° / 0.25° | 2.5° / 0.6° |
| Range | 80 blocks | 64 | 64 |
| The view aimed | narrowed to 0.88 | 0.9 | 0.9 |
| Takes | the barrel attachments, the Extended and Speed Magazines, the three stocks, the Light and Tactical Grips, the four bayonets, and the Medium Scope, Reflex Sight and Laser Sight | the barrel attachments, the Extended Magazine and the four sights | the barrel attachments and the three stocks |

The reload times and the rates are the owner's animations'. The Holler Pistol takes every sight but the Long Scope: there is no room for that one's pieces in its atlas (its model, below).

**Crafting** (a crafting table; the guns switch, as every gun): steel and brass, as the library's other treated-brass guns, and a lever.
- **Holler Pistol:** two steel ingots and a brass ingot over a lever and a brass ingot: its barrel is two steel ingots long.
- **Minuet Pistol:** a steel ingot and a brass ingot over a lever and a brass ingot. It is small: it fits the inventory's own crafting grid too.
- **Half-Crown Revolver:** a steel ingot, a brass ingot and a silver ingot over a lever and a brass ingot; the silver is for the coin it is named for.

**How they fire:** as the other bullet guns; the Half-Crown Revolver loaded a round at a time.

**The Half-Crown Revolver as a carbine:** as the Parley Revolver ([slice 13D](#slice-13d-a-revolving-launcher-a-carbine-revolver-and-a-musket-of-light)): with a stock fitted (the Light, Weighted or Wooden Stock, each the owner's own for it) it is held in both hands, plays the owner's carbine_ animations, both arms come up to it seen from outside, and it is not one of two guns at once (`JugcraftGuns.CARBINES`). Nothing new was needed for it.

**What you see:** the owner's animations.
- **Holler Pistol:** each shot kicks it; it is canted and its bolt drawn back and let go. To reload, it is turned up, the left hand draws the magazine out from below and puts it back in, and the right hand works the bolt.
- **Minuet Pistol:** each shot kicks it and its slide comes back. To reload, the slide is locked back, the magazine drops from the grip and the left hand drives it home, and the slide is let go.
- **Half-Crown Revolver:** each shot kicks it and the hammer falls. To reload, the barrel is tipped down about the hinge at the front of the frame, raising the cylinder, which turns as the left hand pushes each round in; the barrel is snapped shut.
- **Spent rounds:** the Holler Pistol and the Minuet Pistol throw a case at each shot; the Half-Crown Revolver's reload throws one for each round it loads.
- **Sounds:** the owner's brass pistol shot (the Sapper Revolver's and the Tuppence Pistol's) for the Holler Pistol, their iron pistol shot (the Warden Pistol's) for the Minuet Pistol, and their brass revolver shot (the Marshal Revolver's and the Tabby Revolver's) for the Half-Crown Revolver. Each animation's own cues play as they name: the magazine out and in, the slide racked, the bolt pulled and let go.

**How the models were built:**
- **Where the guns turn:** each gun's body turns about the model's origin (owner point (8, 0, 8), under the middle of the gun), not about the grip in the right hand as the earlier guns' do. About the grip, the Holler Pistol's long barrel swung the back of the gun into the view as it reloaded, and the Minuet Pistol's reload brought the back of its slide up into the view; about the origin each stays in the lower part of the view. This was chosen in first-person previews: the library does not hold the owner's own pivots.
- **Holler Pistol:**
  - **Magazine:** the owner's magazine part, ahead of the trigger guard; the left hand holds it from below. The reload's second magazine bone, which the owner's reload only hides, holds nothing, so the magazine drawn out is the one put back.
  - **Bolt:** the owner's bolt part, a knob out to either side.
  - **Empty bones:** the `magazine_2` and `no_sights` bones (the inspection tips the second) hold nothing.
  - **Atlas:** its own texture is 128 x 128, as big as an atlas may be (`tools/check_mod_data.py`'s texture rule). Beside it there is room for the pieces of only three of the four sights, so it goes without the marksman's Long Scope (`ATTACHMENTS` `"not_on"`).
  - **Sights:** the ring at the back over the post at the muzzle.
  - **Aiming:** held 5 px further out (`"eye_relief"`): the back of its receiver came 1.2 px past the eye. Held there, it is 3.8 px away.
- **Minuet Pistol:**
  - **Slide:** the owner's "receiver" part (the animations call it the bolt).
  - **Magazine:** only the base plate under the grip, which the reload drops; its `magazine_2` bone, which every other animation hides, holds nothing.
  - **Left hand:** placed where it drives the magazine home.
  - **Sights:** the ring on the rail over the post at the muzzle.
  - **Aiming:** held 3 px further out: the back of its slide came within 0.85 px of the eye. Held there, it is 3.85 px away.
- **Half-Crown Revolver:**
  - **Barrel:** the owner's barrel part with the main part's 3rd to 5th and 10th to 14th elements (the lug under the barrel, the core of the cylinder, the top strap over it and both sights), tipped about the hinge at the bottom front of the frame.
  - **Cylinder:** the owner's drum part, on the barrel, turned about its own middle as the rounds go in.
  - **Hammer:** the owner's hammer part.
  - **Empty bones:** the `bolt`, `seal` and `shell` bones hold nothing.
  - **Left hand:** placed where it pushes a round into the raised cylinder; the carbine's hold then puts it under the barrel.
  - **Sights:** the notch at the back of the top strap over the post at the front.
  - **Aiming:** its nearest point stays 4 px from the eye; it is held where the hip view holds it.
- **The arms:** chosen in first-person previews, with the game's rule that leaves an arm out while any of it is within a tenth of a block of the eye, to cover as little of the screen as they can through each gun's hold, shot, reload and inspection; the Minuet Pistol's left arm through its reload, and the Half-Crown Revolver's through its loading and its carbine hold.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload, draw and inspection; the Holler's bolt and magazine, the Minuet's slide and magazine, the Half-Crown's barrel opened about its hinge, its cylinder and hammer; the Half-Crown with a stock and its carbine hold; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Light Round and the Rifle Round; nothing new.
- **Parts:** steel, brass and a lever; the Half-Crown Revolver a silver ingot too.
- **Their place:** past steel, with the hand guns: the Holler Pistol the heaviest of the pistols, the Minuet Pistol the truest held in one hand, the Half-Crown Revolver the quickest of the revolvers that fire rifle rounds.

**Balance:** starting numbers, at point blank with every shot landing, over a load and its reload.
- **Holler Pistol:** 12 a shot, the hardest of the pistols (the Bulldog Pistol's 11); about 9.4 a second, as the Warden Pistol's (9.4) and the Ashfall Pistol's (9.3), in fewer and heavier shots.
- **Minuet Pistol:** 3.5 a shot; about 7.5 a second, between the Penny Pistol's (7.3) and the Caisson Pistol's (7.8). Aimed, it strays 0.25°, less than any other gun held in one hand (the Spikedriver's 0.3°).
- **Half-Crown Revolver:** 9 a shot, as the Goshawk Revolver's; about 7.4 a second, the most of the revolvers (the Parley Revolver's 6.6).
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:holler_pistol`, `minuet_pistol` and `half_crown_revolver`; nothing saved changes. A Half-Crown Revolver's stock is a fitted attachment, as on any gun. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **Where each gun turns is chosen here:** the model's origin, from previews, not the owner's own pivots.
- **The Holler Pistol takes no Long Scope:** its atlas has room for only three sights' pieces.
- **The Holler Pistol's reload puts the same magazine back:** the owner's reload moves one magazine and hides the second.
- **A Half-Crown Revolver becomes a carbine from its next animation,** as the Parley Revolver does.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 14B: the brass carbines
The next three of the owner's treated-brass guns, taken up after slice 14A (on 14 October 2026 the owner asked "Begin the next part always be offering to start the next part at the end begin 14B"): the Howler Conversion, the Waltz Conversion and the Grandle. The two conversions are slice 14A's Howler and M22 Waltz made over into carbines held in both hands; the Grandle is a carbine fed from a box magazine. They are the brass carbines.

| | Holler Carbine | Minuet Carbine | Grange Carbine |
|---|---|---|---|
| The owner's gun | Howler Conversion | Waltz Conversion | Grandle |
| What it is | the Howler with a long shroud over its barrel, its bolt worked by the shot itself | the M22 Waltz with a long barrel, a slide at the back of its top, a magazine ahead of its trigger and a lever under its barrel | a carbine with a wooden handguard, a stock's wrist and no pistol grip, a box magazine under its receiver and a bolt handle on its right; the owner made every attachment for it |
| Held | in both hands, the left under the magazine | in both hands, the left under the magazine | in both hands, the right at the stock's wrist and the left on the handguard |
| What it fires | Rifle Rounds | Light Rounds | Rifle Rounds |
| Damage | 9 | 5 | 7 |
| Rate | 2 a second (every 10 ticks) | 4 a second (every 5 ticks) | about 2.9 a second (every 7 ticks) |
| Holds | 10 | 10 | 12 |
| Reload | 2.9 s, the magazine | 2.3 s, the magazine | 2.25 s, the magazine |
| Spread, hip / aimed | 2.5° / 0.4° | 2° / 0.2° | 2.5° / 0.35° |
| Range | 96 blocks | 80 | 96 |
| The view aimed | narrowed to 0.85 | 0.88 | 0.85 |
| Takes | nothing: the owner made no attachment for it | the three stocks and the four sights | the barrel attachments, the Extended and Speed Magazines, the three stocks, the Light and Tactical Grips, the four bayonets and the four sights |

The reload times are the owner's animations'.

**Crafting** (a crafting table; the guns switch, as every gun): steel, brass and a lever, as slice 14A's.
- **Holler Carbine:** three steel ingots over a brass ingot, a lever and a brass ingot: its barrel is three steel ingots long.
- **Minuet Carbine:** two steel ingots and a brass ingot over a brass ingot and a lever: its barrel is two, as the pistol it is made from is small.
- **Grange Carbine:** three steel ingots over a lever and a brass ingot, and a brass ingot under that.

**The conversions and their pistols:** each fires its pistol's rounds, as many to a magazine, reaches further and strays less aimed. The Holler Carbine fires more than twice as fast as the Holler Pistol, as its shot works its bolt, and hits less hard; the Minuet Carbine hits harder than the Minuet Pistol. They are crafted as guns of their own, not made from their pistols, so a pistol's fitted attachments and rounds are never lost to a conversion.

**What you see:** the owner's animations.
- **Holler Carbine:** the Holler Pistol's, but for its shot: each kicks it and its bolt runs back and home, with no hand on it. To reload, it is turned up, the left hand draws the magazine out from below and puts it back in, and the right hand works the bolt.
- **Minuet Carbine:** each shot kicks it and its slide comes back. To reload, it is tipped and rolled over, the lever under its barrel swings down, the magazine drops out and comes back up, the left hand slaps it home, and the lever swings back up.
- **Grange Carbine:** each shot kicks it and its bolt comes back. To reload, it is rolled over and its bolt locked back, the magazine drops out and the left hand slaps it home, and the bolt is let go.
- **Spent rounds:** each throws a case at each shot.
- **Sounds:** the conversions keep their pistols' shots, the owner's brass pistol shot for the Holler Carbine and their iron pistol shot for the Minuet Carbine; the Grange Carbine fires their old rifle shot (the Ranger Rifle's and the Ammonite Carbine's). The library's carabine, pistol and revolver shots carry tags naming other sources, so none of them is used. Each animation's own cues play as they name: the magazine out and in, the slap, the bolt, the lever.

**How the models were built:**
- **Where the guns turn:** about the model's origin (owner point (8, 0, 8)), as slice 14A's guns, checked in first-person previews of each reload and inspection. The Holler Carbine's animations are the Holler Pistol's.
- **Holler Carbine:**
  - **Magazine:** the owner's Howler Conversion has no magazine part, though its reload draws one out from below and puts it back. Its magazine bone holds the Howler's own magazine part (`tools/guns.py` `part_source()`: a part entry `howler/stan_mag` names another of the owner's guns' parts). The two guns share the receiver it stands under, and it stands where it stands on the Howler, ahead of the trigger guard. Its pieces come from the Howler's texture, packed beside the Howler Conversion's own in its atlas.
  - **Bolt:** the owner's bolt part, a knob out to either side.
  - **Empty bones:** the `magazine_2` and `no_sights` bones (its reload hides the one and its inspection tips the other) hold nothing.
  - **Sights:** the ring at the back over the post at the muzzle.
  - **Aiming:** held 5.5 px further out (`"eye_relief"`): the back of its top rail came 1.7 px past the eye. Held there, it is 3.8 px away.
- **Minuet Carbine:** its main part holds it all.
  - **Slide:** the 19th, 20th, 23rd, 26th and 27th elements, the slide and the knob on its left side (the animations call it the bolt).
  - **Lever:** the 34th to 39th elements, turned about the hinge at their back, where the owner's model turns them.
  - **Magazine:** the 40th to 42nd elements (the animations' core).
  - **Empty bones:** the `magazine`, `magazine_2`, `seal` and `scriptures` bones hold nothing: the model has no part for them. Its shot only sways the last two.
  - **Sights:** the ring on the rail over the post at the front of the rail.
  - **Aiming:** held 2 px further out: the back of its frame came within 1.6 px of the eye. Held there, it is 3.6 px away.
- **Grange Carbine:**
  - **Hold:** it has no pistol grip; the right hand holds the stock's wrist behind the trigger guard, and the left the handguard, where its reload draws it back to the magazine.
  - **Bolt:** the owner's bolt part, the handle on its right.
  - **Magazine:** the owner's; its `magazine_2` bone holds nothing.
  - **Sights:** the ring at the back over the post at the front.
  - **Aiming:** held 4 px further out: the back end of its stock came 0.3 px past the eye. Held there, it is 3.7 px away.
- **The arms:** chosen in first-person previews, with the game's rule that leaves an arm out while any of it is within a tenth of a block of the eye, to cover as little of the screen as they can through each gun's hold, shot, reload and inspection. The Holler Carbine keeps the Holler Pistol's.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload and inspection; the Holler Carbine's bolt and borrowed magazine, the Minuet Carbine's slide, lever and magazine, the Grange Carbine's bolt and magazine; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Light Round and the Rifle Round; nothing new.
- **Parts:** steel, brass and a lever.
- **Their place:** past steel, with the carbines: the conversions are the long guns of slice 14A's pistols, the Grange Carbine the brass carbine that takes every attachment.

**Balance:** starting numbers, at point blank with every shot landing, over a load and its reload.
- **Holler Carbine:** 9 a shot; about 12.2 a second, as the Kestrel Rifle's (12.2), above the Holler Pistol's 9.4.
- **Minuet Carbine:** 5 a shot; about 11.0 a second, the most of the guns that fire light rounds a pull at a time (the Sentry Pistol's 9.8). Aimed, it strays 0.2°, the least of the guns that fire light rounds.
- **Grange Carbine:** 7 a shot; about 13.8 a second, as the Picket Rifle's (13.9), below the Goldbristle Carbine's (14.2).
- **A round:** each spends one a shot; nothing converts back.

**Save compatibility:** new items `jugcraft:holler_carbine`, `minuet_carbine` and `grange_carbine`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Holler Carbine's magazine is the Howler's:** its own model has none. If the owner makes one for it, it can take its place.
- **The Holler Carbine takes no attachment:** the owner made none for it, not even a stand-in for the sights, so it takes no scope either.
- **The Minuet Carbine's seal and scriptures:** its shot sways two bones the model has no part for; they hold nothing.
- **Where each gun turns is chosen here:** the model's origin, from previews, not the owner's own pivots.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Slice 14C: the brass quick-firers
Three more of the owner's treated-brass guns, taken up after slice 14B (on 15 October 2026, offered the next part, the owner answered "yes start 14C"): the Cogloader, the Jackhammer and the Libertas. Their animations are one family, the same shot and inspection made for three guns: an automatic rifle fed from a pan magazine, an automatic shotgun and a quick-firing rifle. They are the brass quick-firers.

| | Sprocket Gun | Navvy Shotgun | Freeholder Rifle |
|---|---|---|---|
| The owner's gun | Cogloader | Jackhammer | Libertas |
| What it is | a rifle with a flat pan magazine on its top, a long cooled barrel, a frame of rear sight posts and a stock's wrist instead of a pistol grip | a short gun with a perforated barrel shroud, a box magazine, a pistol grip and a bolt handle across its top | a rifle with glowing green coils along its barrel, a ring sight and a D-shaped trigger loop |
| Held | in both hands: the right at the stock's wrist, the left under the fore-end | in both hands: the right on the grip, the left under the fore-end | in both hands: the right through the trigger loop, the left under the fore-end |
| What it fires | Rifle Rounds, as long as the trigger is held | Buckshot Shells, six pellets each, as long as the trigger is held | Rifle Rounds, a pull a shot |
| Damage | 4.5 | 2.5 a pellet | 6 |
| Rate | 5 a second (every 4 ticks) | 2.5 a second (every 8 ticks) | 4 a second (every 5 ticks) |
| Holds | 40 | 8 | 20 |
| Reload | 3.15 s, the pan | 2.6 s, the magazine | 2.65 s, the magazine |
| Spread, hip / aimed | 4° / 1.5° | 6° / 4° | 2.5° / 0.3° |
| Range | 80 blocks | 24 | 104 |
| The view aimed | narrowed to 0.85 | 0.9 | 0.8 |
| Takes | the barrel attachments, the three stocks, the Light and Tactical Grips, the four bayonets and the four sights | the barrel attachments, the Extended and Speed Magazines, the three stocks, the Light and Tactical Grips and the four bayonets | the Extended and Speed Magazines and the Medium Scope, Reflex Sight and Laser Sight |

The reload times are the owner's animations'. The Navvy Shotgun takes no scope: its sights are part of its body, and the owner made no stand-in for them. The Freeholder Rifle goes without the Long Scope, as the Holler Pistol does (its model, below).

**Crafting** (a crafting table; the guns switch, as every gun): steel, brass and a lever, as slices 14A's and 14B's.
- **Sprocket Gun:** three steel ingots over a hopper, a lever and a brass ingot, and a brass ingot under the lever: the hopper is for the pan that feeds it.
- **Navvy Shotgun:** a steel ingot, a brass ingot and a steel ingot over a lever and a brass ingot: it is short.
- **Freeholder Rifle:** two steel ingots and an emerald over a brass ingot, a lever and a brass ingot: the emerald is for its green coils.

**How they fire:** as the other bullet guns; the Sprocket Gun and the Navvy Shotgun as long as the trigger is held, as the automatic guns. The Navvy Shotgun is the first automatic gun to fire shells: each shot is six pellets, as a shotgun's.

**What you see:** the owner's animations.
- **Sprocket Gun:** each shot kicks it and its bolt comes back. To reload, it is rolled over, the left hand lifts the pan off to the left and sets it back on, slaps it and works the bolt on the gun's left, and the pan is wound a full turn about its middle.
- **Navvy Shotgun and Freeholder Rifle:** each shot kicks it and its bolt comes back. To reload, it is rolled over and its bolt locked back, the magazine drops out and the left hand slaps it home, and the bolt is let go.
- **Spent rounds:** each throws a case at each shot.
- **Sounds:** the owner's scorched rifle shot (the Garrison Rifle's and the Borer's) for the Sprocket Gun, their combat shotgun shot (the Breacher's) for the Navvy Shotgun, and their scorched sniper shot (the Picket Rifle's and the Lament Rifle's) for the Freeholder Rifle. None carries a tag naming another source. Each animation's own cues play as they name: the magazine out and in, the slap, the bolt.

**How the models were built:**
- **Where the guns turn:** about the model's origin (owner point (8, 0, 8)), as slices 14A's and 14B's guns, checked in first-person previews of each reload and inspection.
- **Sprocket Gun:**
  - **Pan:** the owner's magazine part, flat on the gun's top. Its reload moves two bones the owner's animations name: the magazine bone lifts the pan off and puts it back, and the cogloader_magazine bone under it, which holds the pan, winds it a turn. Both turn about the pan's middle.
  - **Bolt:** the owner's bolt part, the handle on its left.
  - **Empty bones:** the `magazine_2` and `seal` bones hold nothing (its shot and inspection sway the seal, which the model has no part for).
  - **Sights:** the frame of posts at the back of the pan over the post at the muzzle; the line of sight passes just over the pan.
  - **Aiming:** held 3.5 px further out (`"eye_relief"`): the back end of its stock came within 0.4 px of the eye. Held there, it is 3.9 px away.
- **Navvy Shotgun:**
  - **Bolt:** the owner's bolt part, the handle across its top.
  - **Grip and magazine:** the owner's; a fitted stock takes the grip's place, as on the owner's other guns with a grip part.
  - **Empty bones:** the `magazine_2` and `seal` bones hold nothing.
  - **Sights:** the ring at the back over the post at the front, both part of its body.
  - **Aiming:** held 2 px further out: the top of its grip came within 2 px of the eye. Held there, it is 4 px away.
- **Freeholder Rifle:**
  - **Bolt:** it has no bolt part, and none of its body's pieces slides as one (its top carries the sights; the pieces on its sides are flat), so its bolt bone holds nothing, as its `magazine_2` and `seal` bones do.
  - **Magazine:** the owner's, ahead of the trigger loop.
  - **Atlas:** its own texture is 128 x 128, as big as an atlas may be; beside it there is room for the pieces of only three of the four sights, so it goes without the Long Scope, as the Holler Pistol does.
  - **Sights:** the ring at the back over the post at the front of its top.
  - **Aiming:** held 2 px further out: the back of its receiver came within 1.8 px of the eye. Held there, it is 3.8 px away.
- **The arms:** chosen in first-person previews, with the game's rule that leaves an arm out while any of it is within a tenth of a block of the eye, to cover as little of the screen as they can through each gun's hold, shot, reload and inspection.
- **Checked** in first-person, side and orthographic previews: idle, aimed, fired, through each reload and inspection; the Sprocket Gun's pan lifted off, set back and wound, its bolt; the Navvy Shotgun's bolt and magazine; the Freeholder Rifle's magazine; and the nearest point of each gun to the eye through its aimed shot.

**Connections:**
- **Rounds:** the Rifle Round and the Buckshot Shell; nothing new.
- **Parts:** steel, brass and a lever; the Sprocket Gun a hopper, the Freeholder Rifle an emerald.
- **Their place:** past steel. The Sprocket Gun with the automatic rifles, the Navvy Shotgun with the shotguns, the Freeholder Rifle with the rifles fired a pull at a time.

**Balance:** starting numbers, at point blank with every shot landing, over a load and its reload.
- **Sprocket Gun:** 4.5 a shot; about 16.4 a second, between the Garrison Rifle's (17.1) and the Borer's (16.1).
- **Navvy Shotgun:** 15 a shell with every pellet landing; about 22.2 a second, between the Mule's (23.3) and the Breacher's (21.8).
- **Freeholder Rifle:** 6 a shot; about 16.2 a second, second of the guns that fire rifle rounds a pull at a time to the Undertone Rifle's 17.3. It fires them quicker than any other such gun, every 5 ticks to the Patchwork Carbine's 6.
- **A round:** each spends one a shot (a shell for the Navvy Shotgun's six pellets); nothing converts back.

**Save compatibility:** new items `jugcraft:sprocket_gun`, `navvy_shotgun` and `freeholder_rifle`; nothing saved changes. `guns.enabled=false` turns their recipes off; the items stay registered.

**Known limits:**
- **The Navvy Shotgun takes no scope:** its sights are part of its body.
- **The Freeholder Rifle takes no Long Scope:** its atlas has room for only three sights' pieces.
- **The Freeholder Rifle's bolt does not show:** its model has no part for it.
- **The Sprocket Gun's seal:** its shot and inspection sway a bone the model has no part for; it holds nothing.
- **Where each gun turns is chosen here:** the model's origin, from previews, not the owner's own pivots.
- **Not played:** none of it has been played yet. The numbers want play to set.

## Connections
- **Existing input producers:** copper, iron and gunpowder (vanilla); lead nuggets (the lead switch's lead); brass nuggets (brass, from zinc); paper and planks.
- **Existing output consumer:** combat. The guns kill what drops loot for every branch.
- **Technology connection:** the rounds are made from the mod's lead and brass; the guns from early metals.
- **Energy connection (slice 8D):** the energy weapons' Energy Cells fill at the Charging Station, from the machines' energy system (JE). Their recipes need the machines switch, and their parts (steel, advanced circuits, copper cable) come from the machines' tree, so they come after the steel tier. The Cell Rack (slice 10E) fills six cells at once from the same cables.
- **Magic connection:** none yet.
- **Reachable entry path:**
  - copper and iron need only mining and smelting; gunpowder comes from creepers;
  - lead and zinc ores are worldgen (their switches, on by default);
  - so every gun and round is reachable in early survival, and nothing circular gates them.
- **Required vs optional:** all optional; no other feature needs a gun.
- **Trade and solo routes:** solo, crafted. Trading rounds between players works as with any item.
- **How the specialty stays useful:** a strong ranged option that costs ammunition. Melee and bows stay free to use.

## Balance and automation
- **Damage per round:** Light 2, Rifle 6, Buckshot up to 20 at point blank.
  - A bow at full draw does about 6 to 10 with no ammunition cost beyond arrows.
  - The Midge's 13 damage a second, held, empties a 20-round magazine in 3 seconds for 40 damage, then 2.35 s of reloading.
- **Costs:**
  - 8 light rounds cost a copper ingot, a lead nugget and a gunpowder;
  - a rifle round costs a quarter of a brass nugget, a lead nugget and a gunpowder;
  - a buckshot shell a quarter of each, plus paper.
- **Metal accounting:** rounds and guns hold no metal units (`tools/check_mod_data.py`). Nothing turns them back into metal, so there is no conversion loop.
- **Server-side rate:** each player has a trigger credit refilling at one shot per interval, banking up to two (`GunShots.BURST`); with two guns, one for each hand (slice 10G). A late packet does not lose a shot, and a fast client gains none.
- **Charging (slices 8D and 10E):** the Charging Station and the Cell Rack put into a cell exactly the JE they take from cables. Nothing is lost or gained, so charging is no loop.
- **Starting numbers,** not tuned in play. The owner may want higher-tier guns to outclass these clearly.

## Multiplayer and persistence
- **Server authority:** the client only asks, with `GunShotPayload` (one per shot) and `GunReloadPayload`, each saying only which hand's gun (slice 10G). The server (`guns/GunShots`):
  - checks the gun in that hand (the other hand's only beside a one-handed gun in the main hand, slice 10G), that the player is alive and not a spectator, the loaded rounds and the trigger credit;
  - computes every bullet from the player's server-side position and look;
  - spends rounds, takes them from the inventory and deals the damage.
- **Ally and protection checks:** a bullet skips allies, the player's own mount and marker stands. It skips anything other code refuses through `AttackEntityCallback`, such as the walled town's protection.
- **Prediction:** the client plays the shot, sound and kick at once for responsiveness. A refused shot is only a sound. The counter allows for shots not yet answered and writes them off after half a second.
- **Reloads:**
  - A reload is held on the server per player, one gun at a time, and stops if the gun leaves its hand.
  - A magazine loads at the end of its time.
  - A shell-at-a-time reload loads one shell after each shell's time, and a shot cuts it short.
  - The player's own client times its animation from the same numbers.
- **Others' animations:** the server tells the clients that see the shooter (not the shooter's own) with `GunActionPayload`, and they play the shot or reload on that gun.
- **Spin-up (slice 8C):** the Thresher's client sends `GunSpinPayload` (no data) each tick its trigger is held. The server keeps, per player, when the run began and its last word, and refuses the Thresher's shots until the run is 15 ticks old (2 forgiven for uneven packets). A gap of more than 4 ticks ends the run. A client that claims to hold the trigger gains nothing it could not by holding it; the run's age, the rounds and the rate are the server's.
- **Grenades and flame (slice 8C):** the server spawns the Lobber's Grenade from its own copy of the player's position and look, and works out the Stoker's jet the same way, with the same ally and protection checks as a bullet.
- **Rockets (slice 10A):** the server spawns the rocket guns' rockets from its own copy of the player's position and look, at the gun's speed and with the gun's fuse; the burst is the server's. A rocket in flight saves what is left of its fuse.
- **Energy weapons (slice 8D):**
  - The server draws a reload's charge from its own copy of the player's cells.
  - It works out the beam and the arcs from its copy of the player's position and look, with the same ally and protection checks as a bullet. An arc asks other code about the creatures it would strike, in turn, and no others.
  - `GunTracePayload` (the shooter's id, the kind of shot and up to three points) tells the clients that see the shooter, and the shooter's own, where the shot went. It is used only to draw the shot.
- **Persistence:** the rounds loaded are a data component on the gun, `jugcraft:loaded_rounds` (0 to 64). An Energy Cell's charge is the shared `jugcraft:energy` component (slice 8D). Its attachments are another, `jugcraft:attachments` (a list of up to four attachment ids, oldest first; an id no longer known is ignored). The Trench Lobber's and the Bullfrog's kind of grenade is `jugcraft:loaded_grenade` (slice 9G: an item id, absent for the frag Grenade; one no longer known is read as the frag Grenade). GeckoLib gives each gun a stable animation id the first time the server ticks it. Nothing else is saved.
- **Attachments and authority:** attachments are fitted only by the crafting recipes, which the server runs. The server reads a gun's numbers from its own copy of the stack (`GunItem.spec(stack)`), for the shot, the trigger rate, the reload and the rounds it may load; the client uses the same numbers only to predict.
- **Disconnect:** clears that player's trigger credits, reload and spin.
- **The Cell Rack (slice 10E):**
  - A cell goes in or comes out only on the server, through the game's own block use: the server checks the player can reach the block before the rack is asked. The client only swings the arm.
  - The server picks the cradle from the point used and its own copy of the rack's cells, and takes the cell from the player's own hand, one at a time.
  - Charging runs in the rack's server tick; the clients that see it get its cells, for drawing, when one goes in or out and once a second while it charges.
  - Hoppers move cells by the container rules above, on the server.
  - It saves its buffer and its cells; a cell keeps its charge as the shared `jugcraft:energy` component.
  - Each tick, a rack looks at its six cradles and charges up to six cells; it loads no chunks.
- **Two guns at once (slice 10G):** the server fires or reloads the other hand's gun only while the player holds a one-handed gun in each hand, with a trigger credit for each hand; one reload at a time. The action and trace payloads say which hand's gun, only to play and draw it.
- **Raider gunners (slice 10F):** the server alone works out a gunner's shots, from its own position toward its target, with the same bullet code as a player's. The clients that see it are told of each shot and reload (`GunActionPayload`, the gunner's id), only to play them. A gunner's magazine count lives in its AI and is not saved; one loaded from a save starts with a full magazine.
- **Disable:** a new switch, `guns.enabled` (config `jugcraft.properties`), gates the guns', rounds' and attachments' recipes (twenty-nine with slice 5: sixteen guns and rounds, eleven attachments, and the fitting and removal recipes). Items and the attachments component stay registered, so saved guns, rounds and attachments survive with it off.

## The shared parts it uses
- **Items:** `JugcraftRegistry.item` for every gun, round and attachment; the Combat tab.
- **Recipes:** two special crafting recipes, `jugcraft:gun_attachment` and `jugcraft:gun_attachment_removal`, built as the Skeleton Key's copying is (`CustomRecipe`).
- **Config:** the `guns` feature switch (`JugcraftConfig.FEATURES`, `tools/materials.py`).
- **Damage type:** `jugcraft:bullet`, tagged `minecraft:is_projectile` (Projectile Protection works against it) and `minecraft:bypasses_cooldown` (each shot counts). Slice 8C adds `jugcraft:flame`, tagged `minecraft:is_fire`, `minecraft:no_knockback` and `minecraft:bypasses_cooldown`. The field chemistry branch's thermite and chlorine share the first two tag files, so `tools/guns.py` writes those files with their entries included (`field_chemistry.damage_type_tags()`).
- **Grenades (slice 8C):** the Trench Lobber fires the field chemistry branch's Grenade through its `GrenadeEntity` and `Warhead`, as the grenade launcher does.
- **Rockets (slice 10A):** the Earthmover and the Skylark Rifle fire the rocketry branch's High-Explosive Rockets as its `CombatRocket`s, the Rocket Launcher's, with a fuse the rocket now carries (`CombatRocket.fuse`), and burst as its do (`Blast`).
- **Energy (slice 8D):** the Energy Cell is the tools' `Chargeable`: the `jugcraft:energy` component, the capacity modules, the Charging Station, the amber charge bar and the JE tooltip line (`PoweredToolItem`). Slice 8D's damage type, `jugcraft:zap`, joins `minecraft:no_knockback` and `minecraft:bypasses_cooldown`.
- **Sounds:** in `sounds.json`, through `tools/generate_material_data.py`.
- **Icons:** the item-icon maps (`tools/item_icons/`, `docs/ITEM_ICONS.md`) for the rounds.
- **Keys:** Jugcraft's key category, beside the party key.
- **Frameworks:** GeckoLib 5.5.7, already a required, approved dependency (`docs/FRAMEWORKS.md`); this is the first Jugcraft code to use it.
- **ArmsMotion** animates only Jugcraft's melee arms (`ArmItem`), and a gun is not one.
  - GeckoLib owns everything a gun does: the gun's parts and, in first person, the arms that hold it.
  - In third person (slice 6), `GunPose` raises the arms through the same two hooks that pose ArmsMotion's: `ArmsRenderStateMixin` and `ArmsHumanoidModelMixin`.
  - The off hand plays no part: guns fire from the main hand only.
- **Casting (Concordance Chunk A, ticket A-07, 11 October 2026):** a gun in the main hand owns the player's arms, so no cast's pose plays over the gun or its reload.
  - A cast and a reload cannot begin together: a cast needs a wand in the main hand, and a reload stops when the gun leaves it.
  - What could outlast a switch from the wand was Spell Engine's release clip (up to 14 ticks) or its fade of an ended cast. Player Animation Library lays those over the arms after `GunPose`, and in first person (Spell Engine's default) hides the gun as the game draws it while they play.
  - `client/CastPoses` stops those clips, and the Vigil's offering gesture, on any player holding a gun. It runs at the end of each client tick, in first and third person, for every player the client sees. No mixin. `CircleClient` keeps the circle's gesture off such a player itself, by asking `CastPoses.gesturesYield`.
  - [The Concordance presentation record](arcane-concordance-presentation.md#one-owner-for-the-arms-chunk-a-ticket-a-07) has the details and the test (`ConcordanceJourneyClientGameTests.aCastGivesWayToAGunOrAnArm`, not yet run).
- **Particles:** the casings are registered as the Fog Machine's fog is (`FabricParticleTypes.simple()`, a provider on the client), and so is the Laser Sight's dot (slice 9E), which shows whatever the particle setting (`simple(true)`).

## How the owner's models became GeckoLib models
The owner supplied each gun's parts as Blockbench Java item models (`Guns/models/special/<gun>/<part>.json`) and Bedrock animations made for GeckoLib models that were not in the upload. `tools/guns.py`:
1. **Builds one bone per animated part,** placing each of the owner's elements as one GeckoLib cube with the same corners, the same UV at each corner and the same turn about the same point:

   | Gun | Bones |
   |---|---|
   | Midge | `gun_body2` > `gun_body` (main); `bolt`, `barrels`, `magazine` (the standard magazine) |
   | Carbine | `gun_body` (main and standard barrel); `bolt`, `magazine`, and an empty `magazine_2` (its animations keep it at scale 0) |
   | Thunderpipe | `gun_body` (main and standard grip); `barrels`; an empty `bolt`; and `shell` |

   The Thunderpipe's reload moves a `shell` bone that had no part. It gets a 2 × 2 × 5 px shell, red paper on a brass head, drawn into an empty corner of the atlas copy. Its rest place is the breech less the loop's last offset, so the loop slides it home. Every other animation hides it except the inspect, which leaves it at that rest place, so the renderer draws it only while an animation moves it.
2. **Places the arm bones.** The animations move `right_arm` and `left_arm`, whose models were not supplied:
   - **Hierarchy:** each is a child of `gun_body`, so the hands follow the gun. This fit the Thunderpipe's shell-carrying left hand best of the arrangements tried (1.1 px, against 1.2 to 2.1).
   - **Pivot:** each pivot is the hand, placed so the idle pose's offsets bring it to the grip (right) and the fore-end or magazine (left).
   - **The arm:** each idle pose turns the arm bone so its -y points straight back at the camera, which showed the arms end-on as big slabs. So each arm runs instead toward a `<side>_shoulder` locator 10 px from the hand, down, back and out (`"arms"` in `BUILDS`, in the bone's own frame). The directions were chosen in a first-person preview of the idle pose, and the renderer turns the player's arm from -y onto the locator.
   - **Rest points (owner pixels):**
     - Midge: right hand (8, 2.2, 14.8), left (7.2, 0.6, 10.7);
     - Carbine: right (8, 1.2, 16.0), left (8, 2.8, 7.0);
     - Thunderpipe: right (8, 1.6, 17.0), left (8, 2.8, 9.5).
   - **Gun body:** turns about the right hand's grip.
3. **Copies the files unchanged:** the atlases (the Thunderpipe's with the shell added), the animations (byte for byte) and the sounds. One part is moved as it is read: the Whistler's light stock, which stands off the gun ([slice 9A](#slice-9a-the-marksman-rifles)).
4. **Checks the result:** it re-bakes every model the way GeckoLib 5.5.7 does (`GeometryCube`, `VertexSet`, `GeometryQuadUvs`) and compares each face with the owner's. Run `python3 tools/guns.py --check`; `tools/check_mod_data.py` runs it too. It was shown to fail on a flipped UV, a moved cube and a turned face.

The animations' sound cues map to the library's sounds as follows:
- `gun_rustle` and `rustle`: gun_rustle
- `rack`, `bolt`, `bolt_pull`, `bolt_release`: the sound of the same name
- `reload_mag_out`, `reload_mag_in`: mag_out, mag_in (the Thunderpipe's `reload_mag_in` plays the shell insert)
- `slap`, `reload_end`: the sound of the same name

An empty click plays the Rusty Gnat's copper_jam. Each gun's shot is its own `fire.ogg`. Of the animations' particle cues, `eject_casing` throws a spent casing ([slice 6](#slice-6-the-guns-in-use)); the others show nothing.

## Dependencies and assets
- **No new dependencies.** GeckoLib 5.5.7 is already required (`distribution/frameworks.lock.json`).
- **The owner's files,** from the owner asset library (`art/owner-library/originals/Blocks/`). Their SHA-256 begins:

| Gun | Library file | SHA-256 (first 16) |
|---|---|---|
| rust_midge | `Guns/models/item/rusty_gnat.json` (display transforms) | `605fd78cc682e5fa` |
| rust_midge | `Guns/item/rusty_gnat.png` | `2cf1804a29b889d8` |
| rust_midge | `Guns/item/rusty_gnat.animation.json` | `188ed3e28158cc07` |
| rust_midge | `Guns/models/special/rusty_gnat/main.json` | `d00bacd6fbd29ef7` |
| rust_midge | `Guns/models/special/rusty_gnat/bolt.json` | `7a88e2dd891f3576` |
| rust_midge | `Guns/models/special/rusty_gnat/barrel.json` | `4fcaeeb5be426afc` |
| rust_midge | `Guns/models/special/rusty_gnat/stan_mag.json` | `f8f36fe847727c96` |
| rust_midge | `Guns/sounds/item/rusty_gnat/fire.ogg` | `02c2854e36a0f377` |
| patchwork_carbine | `Guns/models/item/makeshift_rifle.json` | `5a28bbf2e7e1bdcf` |
| patchwork_carbine | `Guns/item/makeshift_rifle.png` | `db7bd9ccf55c6a45` |
| patchwork_carbine | `Guns/item/makeshift_rifle.animation.json` | `1d9c970f64578bb7` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/main.json` | `a501f77774546816` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/stan_barrel.json` | `bcfeb9a40b33e5f7` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/bolt.json` | `c0b217fd56b381d0` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/stan_mag.json` | `a26c7b8753d94492` |
| patchwork_carbine | `Guns/sounds/item/makeshift_rifle/fire.ogg` | `9c4469e45b9b77e4` |
| thunderpipe | `Guns/models/item/boomstick.json` | `4c1a97aa58a16ed0` |
| thunderpipe | `Guns/item/boomstick.png` (copied with the shell added) | `d06afbd303f6c1e6` |
| thunderpipe | `Guns/item/boomstick.animation.json` | `933bbc2a481f37e1` |
| thunderpipe | `Guns/models/special/boomstick/main.json` | `26a760935188b36d` |
| thunderpipe | `Guns/models/special/boomstick/stan_grip.json` | `c1600f32e348032e` |
| thunderpipe | `Guns/models/special/boomstick/barrel.json` | `5cfbb08d0c0ed36f` |
| thunderpipe | `Guns/sounds/item/boomstick/fire.ogg` | `17a57973db433feb` |
| warden_pistol | `Guns/models/item/defender_pistol.json` | `bf3301680bc96f45` |
| warden_pistol | `Guns/item/defender_pistol.png` | `ad202f717aeba7ad` |
| warden_pistol | `Guns/item/defender_pistol.animation.json` | `12cfc09f3808047f` |
| warden_pistol | `Guns/models/special/defender_pistol/main.json` | `2876ee81c267a62b` |
| warden_pistol | `Guns/models/special/defender_pistol/stan_barrel.json` | `19ad2fd0bd8b90fb` |
| warden_pistol | `Guns/models/special/defender_pistol/receiver.json` | `36e10152e2d99dc9` |
| warden_pistol | `Guns/models/special/defender_pistol/stan_mag.json` | `7ab12d526d23f4cc` |
| warden_pistol | `Guns/sounds/item/iron_pistol/fire.ogg` | `6a0fe18dfc3165d5` |
| riveter_smg | `Guns/models/item/greaser_smg.json` | `8f0509099a336944` |
| riveter_smg | `Guns/item/greaser_smg.png` | `25c4edd154a19db3` |
| riveter_smg | `Guns/item/greaser_smg.animation.json` | `f3652168b8a19a97` |
| riveter_smg | `Guns/models/special/greaser_smg/main.json` | `18913c7642478b66` |
| riveter_smg | `Guns/models/special/greaser_smg/stan_barrel.json` | `71ade7f1ea1cb5e2` |
| riveter_smg | `Guns/models/special/greaser_smg/sights.json` | `8b2ddcda44cffbc1` |
| riveter_smg | `Guns/models/special/greaser_smg/bolt.json` | `55f84a9152c3883b` |
| riveter_smg | `Guns/models/special/greaser_smg/stan_mag.json` | `0d2d5d33f5974d07` |
| riveter_smg | `Guns/sounds/item/greaser_smg/fire.ogg` | `201fd4258dea84a2` |
| haymaker | `Guns/models/item/bruiser.json` | `26242eca95ba17df` |
| haymaker | `Guns/item/bruiser.png` | `3bd5c461326585b5` |
| haymaker | `Guns/item/bruiser.animation.json` | `2e58fe5e8cf7fa97` |
| haymaker | `Guns/models/special/bruiser/main.json` | `3d2bb3bdad310541` |
| haymaker | `Guns/models/special/bruiser/barrel.json` | `ea98ae5f5582c0ce` |
| haymaker | `Guns/sounds/item/bruiser/fire.ogg` | `b965c68e659f88e3` |
| longhorn_rifle | `Guns/models/item/marlin.json` | `e55dbcff4031e78b` |
| longhorn_rifle | `Guns/item/marlin.png` | `103b847aa693002d` |
| longhorn_rifle | `Guns/item/marlin.animation.json` | `142ef222c32d4677` |
| longhorn_rifle | `Guns/models/special/marlin/main.json` | `c6ad3d720abbf099` |
| longhorn_rifle | `Guns/models/special/marlin/stan_barrel.json` | `6fb89e7f959d1014` |
| longhorn_rifle | `Guns/models/special/marlin/sights.json` | `8f5fcc8e50769101` |
| longhorn_rifle | `Guns/models/special/marlin/bolt.json` | `eff14fef76793cd0` |
| longhorn_rifle | `Guns/sounds/item/heavy_rifle/fire.ogg` | `389a087108dc1565` |
| drover_rifle | `Guns/models/item/winnie.json` | `19ff6562ae558f77` |
| drover_rifle | `Guns/item/winnie.png` | `8f41f0400c45d5d1` |
| drover_rifle | `Guns/item/winnie.animation.json` | `949f1214d786712c` |
| drover_rifle | `Guns/models/special/winnie/main.json` | `ff44e4d5052b6601` |
| drover_rifle | `Guns/models/special/winnie/stan_barrel.json` | `73e379d0f13ab28e` |
| drover_rifle | `Guns/models/special/winnie/sights.json` | `7e42e8d0e4b10629` |
| drover_rifle | `Guns/models/special/winnie/hammer.json` | `d92ff90ae9bcb8e2` |
| drover_rifle | `Guns/sounds/item/cowboy/fire.ogg` | `27ab9051ec48ba94` |
| coach_gun | `Guns/models/item/callwell.json` | `4bffc3664e0333c3` |
| coach_gun | `Guns/item/callwell.png` | `6c7198a2cc8a4f19` |
| coach_gun | `Guns/item/callwell.animation.json` | `ab04e81b8490abec` |
| coach_gun | `Guns/models/special/callwell/main.json` | `1c1681b7bdadd76f` |
| coach_gun | `Guns/sounds/item/brass_shotgun/fire.ogg` | `95eee6d27b87d35a` |
| duelling_pistol | `Guns/models/item/flintlock_pistol.json` | `2d67c5d0470be932` |
| duelling_pistol | `Guns/item/flintlock_pistol.png` | `3217c9593149ede6` |
| duelling_pistol | `Guns/item/flintlock_pistol.animation.json` | `61b3660ac8829525` |
| duelling_pistol | `Guns/models/special/flintlock_pistol/main.json` | `1d94ca1c4f73eae0` |
| duelling_pistol | `Guns/models/special/flintlock_pistol/hammer.json` | `8bf431167c8b7557` |
| duelling_pistol | `Guns/sounds/item/blackpowder/fire.ogg` | `a3d3d49a332d034f` |
| line_musket | `Guns/models/item/musket.json` | `e6231ba72c95949a` |
| line_musket | `Guns/item/musket.png` | `2e80294309d87927` |
| line_musket | `Guns/item/musket.animation.json` | `3de10c7807e5940c` |
| line_musket | `Guns/models/special/musket/main.json` | `2e24d5f0620e9051` |
| line_musket | `Guns/models/special/musket/hammer.json` | `059525f269d8120c` |
| line_musket | `Guns/models/special/musket/flash.json` | `7a6ab5acaca6669f` |
| line_musket | `Guns/sounds/item/blackpowder/fire.ogg` | `a3d3d49a332d034f` |
| bellmouth | `Guns/models/item/blunderbuss.json` | `7d99e9a7cedc85b3` |
| bellmouth | `Guns/item/blunderbuss.png` | `4e90eca70ba48802` |
| bellmouth | `Guns/item/blunderbuss.animation.json` | `82acaf92b7654690` |
| bellmouth | `Guns/models/special/blunderbuss/main.json` | `611befd4a9d8e37d` |
| bellmouth | `Guns/models/special/blunderbuss/hammer.json` | `9365a5b0b9c90007` |
| bellmouth | `Guns/sounds/item/blackpowder/fire.ogg` | `a3d3d49a332d034f` |
| bulldog_pistol | `Guns/models/item/brawler.json` | `19bb23c2931a096b` |
| bulldog_pistol | `Guns/item/brawler.png` | `dbe617e6c29df0fa` |
| bulldog_pistol | `Guns/item/brawler.animation.json` | `53db80f081bad631` |
| bulldog_pistol | `Guns/models/special/brawler/main.json` | `a87efb54a5dfa9a6` |
| bulldog_pistol | `Guns/models/special/brawler/sights.json` | `1ea6c6e568000c20` |
| bulldog_pistol | `Guns/models/special/brawler/stan_barrel.json` | `cca50cc97edb2156` |
| bulldog_pistol | `Guns/models/special/brawler/silencer.json` | `5b7dc75b7263886e` |
| bulldog_pistol | `Guns/models/special/brawler/advanced_silencer.json` | `5696639c4c522be6` |
| bulldog_pistol | `Guns/models/special/brawler/muzzle_brake.json` | `95806dbe499f4ecc` |
| bulldog_pistol | `Guns/models/special/brawler/ext_barrel.json` | `dc2ffd8c37523ffa` |
| bulldog_pistol | `Guns/sounds/item/heavier_rifle/fire.ogg` | `4e17f5a1b2891ee7` |
| bulldog_pistol | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| bulldog_pistol | `Guns/item/medium_scope.png` | `543abecf859783be` |
| bulldog_pistol | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| marshal_revolver | `Guns/models/item/longarm.json` | `4016ba18bfc6ea41` |
| marshal_revolver | `Guns/item/longarm.png` | `5d18c5e07c4d9c51` |
| marshal_revolver | `Guns/item/longarm.animation.json` | `5f674f97668f4171` |
| marshal_revolver | `Guns/models/special/longarm/main.json` | `c8e1be01ce6db4a1` |
| marshal_revolver | `Guns/models/special/longarm/light_stock.json` | `b7b6c99e1eabb3e9` |
| marshal_revolver | `Guns/models/special/longarm/heavy_stock.json` | `5802184529940e27` |
| marshal_revolver | `Guns/models/special/longarm/wooden_stock.json` | `024b6e752bbda387` |
| marshal_revolver | `Guns/sounds/item/brass_revolver/fire.ogg` | `9ede81f3520dfd5c` |
| sapper_revolver | `Guns/models/item/trenchur.json` | `15a8d9614307f216` |
| sapper_revolver | `Guns/item/trenchur.png` | `0f63212b8c65910f` |
| sapper_revolver | `Guns/item/trenchur.animation.json` | `b544565c99687f16` |
| sapper_revolver | `Guns/models/special/trenchur/main.json` | `0c3466c9a365bde5` |
| sapper_revolver | `Guns/models/special/trenchur/stan_barrel.json` | `c363f55f825e192c` |
| sapper_revolver | `Guns/models/special/trenchur/silencer.json` | `ea896d0260bd5c99` |
| sapper_revolver | `Guns/models/special/trenchur/advanced_silencer.json` | `93a7f63f689826a8` |
| sapper_revolver | `Guns/models/special/trenchur/muzzle_brake.json` | `51079aa07d948cdc` |
| sapper_revolver | `Guns/models/special/trenchur/ext_barrel.json` | `e7a17c0c04695cb5` |
| sapper_revolver | `Guns/models/special/trenchur/drum.json` | `4ecbe2b43bfa9686` |
| sapper_revolver | `Guns/models/special/trenchur/hammer.json` | `7f282421e7442161` |
| sapper_revolver | `Guns/sounds/item/brass_pistol/fire.ogg` | `4fca376e2ece67b2` |
| sentry_pistol | `Guns/models/item/mak_mkii.json` | `778a1ffa4d8edcb0` |
| sentry_pistol | `Guns/item/mak_mkii.png` | `3ad3c11e8c8536d4` |
| sentry_pistol | `Guns/item/mak_mkii.animation.json` | `348e069b4f2c2ea9` |
| sentry_pistol | `Guns/models/special/mak_mkii/main.json` | `d1f0838085668b60` |
| sentry_pistol | `Guns/models/special/mak_mkii/stan_barrel.json` | `334be33051671a7c` |
| sentry_pistol | `Guns/models/special/mak_mkii/silencer.json` | `e0a1523dd2125508` |
| sentry_pistol | `Guns/models/special/mak_mkii/advanced_silencer.json` | `8b344b1076efc100` |
| sentry_pistol | `Guns/models/special/mak_mkii/muzzle_brake.json` | `ad543140f3e045ca` |
| sentry_pistol | `Guns/models/special/mak_mkii/ext_barrel.json` | `ff50cbcca19985b3` |
| sentry_pistol | `Guns/models/special/mak_mkii/stock_light.json` | `b544ae0859b827dd` |
| sentry_pistol | `Guns/models/special/mak_mkii/stock_weighted.json` | `9541e05fcd508c3a` |
| sentry_pistol | `Guns/models/special/mak_mkii/stock_wooden.json` | `cb89ac77f024bbc5` |
| sentry_pistol | `Guns/models/special/mak_mkii/bolt.json` | `b6d64068ac7633ea` |
| sentry_pistol | `Guns/models/special/mak_mkii/stan_mag.json` | `68eec43cf4511460` |
| sentry_pistol | `Guns/models/special/mak_mkii/ext_mag.json` | `5dfd941f128e67c7` |
| sentry_pistol | `Guns/models/special/mak_mkii/speed_mag.json` | `e68829e7b2a4157e` |
| sentry_pistol | `Guns/sounds/item/scrapper/fire.ogg` | `43c9d4ec929bf949` |
| garrison_rifle | `Guns/models/item/stigg.json` | `4f71c7a1e9c54670` |
| garrison_rifle | `Guns/item/stigg.png` | `f12b4953835d635e` |
| garrison_rifle | `Guns/item/stigg.animation.json` | `9cb1d94020b07c0b` |
| garrison_rifle | `Guns/models/special/stigg/main.json` | `a1ab811dcfd676ef` |
| garrison_rifle | `Guns/models/special/stigg/stan_barrel.json` | `16ce9ed6b1e65725` |
| garrison_rifle | `Guns/models/special/stigg/silencer.json` | `6597002d409ef021` |
| garrison_rifle | `Guns/models/special/stigg/advanced_silencer.json` | `2e0c0ddc5f56fd09` |
| garrison_rifle | `Guns/models/special/stigg/muzzle_brake.json` | `8c570f6fc62d30f2` |
| garrison_rifle | `Guns/models/special/stigg/ext_barrel.json` | `1374203582838b9f` |
| garrison_rifle | `Guns/models/special/stigg/light_stock.json` | `14ed6e823afad363` |
| garrison_rifle | `Guns/models/special/stigg/heavy_stock.json` | `0fa8203ccafb2bfe` |
| garrison_rifle | `Guns/models/special/stigg/wooden_stock.json` | `764077024a9dee88` |
| garrison_rifle | `Guns/models/special/stigg/light_grip.json` | `6b25f349084e3e5d` |
| garrison_rifle | `Guns/models/special/stigg/iron_bayonet.json` | `2e91ccdf78e54541` |
| garrison_rifle | `Guns/models/special/stigg/anthralite_bayonet.json` | `e7d0871dc4ed329d` |
| garrison_rifle | `Guns/models/special/stigg/diamond_bayonet.json` | `78d269ce8bbad1d1` |
| garrison_rifle | `Guns/models/special/stigg/netherite_bayonet.json` | `6ffcfd0ec3b2db2c` |
| garrison_rifle | `Guns/models/special/stigg/sights.json` | `b9ca3b99c184a13e` |
| garrison_rifle | `Guns/models/special/stigg/bolt.json` | `fbcd8d0ef1e6efb8` |
| garrison_rifle | `Guns/models/special/stigg/stan_mag.json` | `3666313bd2e09241` |
| garrison_rifle | `Guns/models/special/stigg/ext_mag.json` | `a47fa93c930ec65b` |
| garrison_rifle | `Guns/models/special/stigg/speed_mag.json` | `0e4af455fd9996c7` |
| garrison_rifle | `Guns/sounds/item/scorched_rifle/fire.ogg` | `5a52d37065a09a8c` |
| garrison_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| garrison_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| garrison_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| breacher | `Guns/models/item/combat_shotgun.json` | `2fc142dbd4169607` |
| breacher | `Guns/item/combat_shotgun.png` | `baaa56e1d903b470` |
| breacher | `Guns/item/combat_shotgun.animation.json` | `49f830a638325df7` |
| breacher | `Guns/models/special/combat_shotgun/main.json` | `54405f48c1ca58bd` |
| breacher | `Guns/models/special/combat_shotgun/stan_barrel.json` | `375d948a4ae30699` |
| breacher | `Guns/models/special/combat_shotgun/silencer.json` | `093b1c36157e4784` |
| breacher | `Guns/models/special/combat_shotgun/advanced_silencer.json` | `0ced06857fce8f97` |
| breacher | `Guns/models/special/combat_shotgun/muzzle_brake.json` | `08060b30cfe228f6` |
| breacher | `Guns/models/special/combat_shotgun/ext_barrel.json` | `bd6f8273d790a67b` |
| breacher | `Guns/models/special/combat_shotgun/light_stock.json` | `3ecd41c01242e40a` |
| breacher | `Guns/models/special/combat_shotgun/heavy_stock.json` | `fb2cfb2e67aff647` |
| breacher | `Guns/models/special/combat_shotgun/wooden_stock.json` | `4e49d54950d46cf9` |
| breacher | `Guns/models/special/combat_shotgun/light_grip.json` | `eb60da8774e2ca15` |
| breacher | `Guns/models/special/combat_shotgun/iron_bayonet.json` | `9d95979a6450a213` |
| breacher | `Guns/models/special/combat_shotgun/anthralite_bayonet.json` | `06ccaf4a9398da15` |
| breacher | `Guns/models/special/combat_shotgun/diamond_bayonet.json` | `6da76c11cd9eeceb` |
| breacher | `Guns/models/special/combat_shotgun/netherite_bayonet.json` | `9d67082048066726` |
| breacher | `Guns/models/special/combat_shotgun/sights.json` | `8b000ceb82c12040` |
| breacher | `Guns/models/special/combat_shotgun/bolt.json` | `065f5f326def6331` |
| breacher | `Guns/models/special/combat_shotgun/stan_mag.json` | `80070bbacb8591fe` |
| breacher | `Guns/models/special/combat_shotgun/ext_mag.json` | `98853daee63e9562` |
| breacher | `Guns/models/special/combat_shotgun/speed_mag.json` | `b506b666fbcb2457` |
| breacher | `Guns/sounds/item/combat_shotgun/fire.ogg` | `155925e837a3d40b` |
| breacher | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| breacher | `Guns/item/medium_scope.png` | `543abecf859783be` |
| breacher | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| trench_lobber | `Guns/models/item/hammer_gl.json` | `afdff981c85abed1` |
| trench_lobber | `Guns/item/hammer_gl.png` | `da0d14786f55c8d9` |
| trench_lobber | `Guns/item/hammer_gl.animation.json` | `a85d5ec884878b42` |
| trench_lobber | `Guns/models/special/hammer_gl/main.json` | `638ea51d2be8758e` |
| trench_lobber | `Guns/models/special/hammer_gl/light_stock.json` | `78b44846f3ceb5a8` |
| trench_lobber | `Guns/models/special/hammer_gl/heavy_stock.json` | `8f4e53429bc119ab` |
| trench_lobber | `Guns/models/special/hammer_gl/wooden_stock.json` | `42eeb43c691d99ab` |
| trench_lobber | `Guns/models/special/hammer_gl/no_sights.json` | `e14b796ec52bbc48` |
| trench_lobber | `Guns/models/special/hammer_gl/sights.json` | `6eca3a9cad91aef5` |
| trench_lobber | `Guns/models/special/hammer_gl/stan_mag.json` | `bc851c25650d2420` |
| trench_lobber | `Guns/models/special/hammer_gl/ext_mag.json` | `c98c1814f402f6c1` |
| trench_lobber | `Guns/models/special/hammer_gl/speed_mag.json` | `6d43f3ab4e7c2184` |
| trench_lobber | `Guns/sounds/item/grenade_launcher/fire.ogg` | `447fa85801d894e8` |
| trench_lobber | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| trench_lobber | `Guns/item/medium_scope.png` | `543abecf859783be` |
| trench_lobber | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| thresher | `Guns/models/item/gattaler.json` | `de8a948136fcce8c` |
| thresher | `Guns/item/gattaler.png` | `334c1948cabbb708` |
| thresher | `Guns/item/gattaler.animation.json` | `044717fa8ec7bc5c` |
| thresher | `Guns/models/special/gattaler/main.json` | `2ce0defa34b2e442` |
| thresher | `Guns/models/special/gattaler/barrels.json` | `552e7b0b2839b0ac` |
| thresher | `Guns/sounds/item/new_rifle/fire.ogg` | `267e176ad9bae07a` |
| stoker | `Guns/models/item/kiln_gun.json` | `591967b76e526e0b` |
| stoker | `Guns/item/kiln_gun.png` | `0534a10b7b6bf764` |
| stoker | `Guns/item/kiln_gun.animation.json` | `c3c8cbe2a44ed3c9` |
| stoker | `Guns/models/special/kiln_gun/main.json` | `98b35b85d243be27` |
| stoker | `Guns/models/special/kiln_gun/light_stock.json` | `b757939fe8d38015` |
| stoker | `Guns/models/special/kiln_gun/heavy_stock.json` | `9782a4f888836168` |
| stoker | `Guns/models/special/kiln_gun/wooden_stock.json` | `1cbfee18215b6665` |
| stoker | `Guns/sounds/item/flamethrower/fire_2.ogg` | `d0c649ff0323c3ef` |
| stoker | `Guns/item/spitfire_flame.png` | `c81c31bd15a64d7c` |
| beam_pistol | `Guns/models/item/raygun.json` | `eabb17b32f05d0bf` |
| beam_pistol | `Guns/item/raygun.png` | `26f663da7ee4f261` |
| beam_pistol | `Guns/item/raygun.animation.json` | `aec8a6d829deb6cb` |
| beam_pistol | `Guns/models/special/raygun/main.json` | `0361d1c522be9303` |
| beam_pistol | `Guns/models/special/raygun/light_stock.json` | `79d7b701e5136212` |
| beam_pistol | `Guns/models/special/raygun/heavy_stock.json` | `00db5395f7e929c9` |
| beam_pistol | `Guns/models/special/raygun/wooden_stock.json` | `b7c28931765d8969` |
| beam_pistol | `Guns/sounds/item/raygun/fire.ogg` | `7ef2b258e9b23920` |
| beam_pistol | `Guns/item/raygun_stocks.png` | `9f743802b4b7a394` |
| stormlock_rifle | `Guns/models/item/teslock_rifle.json` | `b2d5f0e911d1f60c` |
| stormlock_rifle | `Guns/item/teslock_rifle.png` | `94abf372c6030f27` |
| stormlock_rifle | `Guns/item/teslock_rifle.animation.json` | `3a767e17c45cb9ed` |
| stormlock_rifle | `Guns/models/special/teslock_rifle/main.json` | `6220540eb08c9387` |
| stormlock_rifle | `Guns/models/special/teslock_rifle/light_grip.json` | `d397719510cf667b` |
| stormlock_rifle | `Guns/models/special/teslock_rifle/vert_grip.json` | `889d65938098d2a9` |
| stormlock_rifle | `Guns/models/special/teslock_rifle/iron_bayonet.json` | `be5714d02ea3b11b` |
| stormlock_rifle | `Guns/models/special/teslock_rifle/anthralite_bayonet.json` | `495f6a6b8aa2aa5f` |
| stormlock_rifle | `Guns/models/special/teslock_rifle/diamond_bayonet.json` | `c807a0c5d4b15866` |
| stormlock_rifle | `Guns/models/special/teslock_rifle/netherite_bayonet.json` | `7959a433f4e968e1` |
| stormlock_rifle | `Guns/models/special/teslock_rifle/sights.json` | `1259d32391b33f5b` |
| stormlock_rifle | `Guns/sounds/item/shock/fire.ogg` | `cfa59666eb34df2c` |
| stormlock_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| stormlock_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| stormlock_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| linesman | `Guns/models/item/arc_worker.json` | `f090a626888a1427` |
| linesman | `Guns/item/arc_worker.png` | `7d99a59b4d3b339e` |
| linesman | `Guns/item/arc_worker.animation.json` | `7a55c3895bb318db` |
| linesman | `Guns/models/special/arc_worker/main.json` | `9069cacf981c68a4` |
| linesman | `Guns/models/special/arc_worker/light_stock.json` | `d53252f4c667df9d` |
| linesman | `Guns/models/special/arc_worker/heavy_stock.json` | `967629ff9f95c264` |
| linesman | `Guns/models/special/arc_worker/wooden_stock.json` | `115a9b4ef3d03ee0` |
| linesman | `Guns/models/special/arc_worker/sights.json` | `081b859a198fba2a` |
| linesman | `Guns/sounds/item/laser/fire.ogg` | `9ce6c4df8b513390` |
| linesman | `Guns/item/rusty_gnat.png` | `2cf1804a29b889d8` |
| linesman | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| linesman | `Guns/item/medium_scope.png` | `543abecf859783be` |
| linesman | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| picket_rifle | `Guns/models/item/m3_marksman.json` | `f1ef00294618bcae` |
| picket_rifle | `Guns/item/m3_marksman.png` | `e6b9eeea82ab582f` |
| picket_rifle | `Guns/item/m3_marksman.animation.json` | `f0f6a67bff8f16fc` |
| picket_rifle | `Guns/models/special/m3_marksman/main.json` | `bc1536e41a3a27e5` |
| picket_rifle | `Guns/models/special/m3_marksman/sights.json` | `320b7c64b684e96e` |
| picket_rifle | `Guns/models/special/m3_marksman/stan_barrel.json` | `12fbfc809cdbe14b` |
| picket_rifle | `Guns/models/special/m3_marksman/silencer.json` | `f27123c5f0c5a0a6` |
| picket_rifle | `Guns/models/special/m3_marksman/advanced_silencer.json` | `501113b1269f5c68` |
| picket_rifle | `Guns/models/special/m3_marksman/muzzle_brake.json` | `6198ae0cffe6a423` |
| picket_rifle | `Guns/models/special/m3_marksman/ext_barrel.json` | `678b3435899b5732` |
| picket_rifle | `Guns/models/special/m3_marksman/light_grip.json` | `971e71cb000e969c` |
| picket_rifle | `Guns/models/special/m3_marksman/iron_bayonet.json` | `3293c99f3298ae80` |
| picket_rifle | `Guns/models/special/m3_marksman/anthralite_bayonet.json` | `12278c61768c133e` |
| picket_rifle | `Guns/models/special/m3_marksman/diamond_bayonet.json` | `4359c1879a8e3d51` |
| picket_rifle | `Guns/models/special/m3_marksman/netherite_bayonet.json` | `f01bc97fd08ab627` |
| picket_rifle | `Guns/models/special/m3_marksman/bolt.json` | `3d478b65457e46a8` |
| picket_rifle | `Guns/models/special/m3_marksman/stan_mag.json` | `081781046abbecc1` |
| picket_rifle | `Guns/models/special/m3_marksman/ext_mag.json` | `8a316d79e6dde6db` |
| picket_rifle | `Guns/models/special/m3_marksman/speed_mag.json` | `31f5efe58b2b3fde` |
| picket_rifle | `Guns/sounds/item/scorched_sniper/fire.ogg` | `624b2460a3e39921` |
| ranger_rifle | `Guns/models/item/mk43_rifle.json` | `9f0fab0f05b8e4b9` |
| ranger_rifle | `Guns/item/mk43_rifle.png` | `a1efff906c69b334` |
| ranger_rifle | `Guns/item/mk43_rifle.animation.json` | `ad6f9a10e4df7239` |
| ranger_rifle | `Guns/models/special/mk43_rifle/main.json` | `8869a43484de682c` |
| ranger_rifle | `Guns/models/special/mk43_rifle/stan_grip.json` | `2a7f8b93a13f60ee` |
| ranger_rifle | `Guns/models/special/mk43_rifle/light_stock.json` | `bdb3c4a8f7635e16` |
| ranger_rifle | `Guns/models/special/mk43_rifle/heavy_stock.json` | `ad00c61472bdeea6` |
| ranger_rifle | `Guns/models/special/mk43_rifle/wooden_stock.json` | `c0702e6b06587c69` |
| ranger_rifle | `Guns/models/special/mk43_rifle/light_grip.json` | `38b1c261cb0c90a6` |
| ranger_rifle | `Guns/models/special/mk43_rifle/iron_bayonet.json` | `c9546fa642585d41` |
| ranger_rifle | `Guns/models/special/mk43_rifle/anthralite_bayonet.json` | `89888802a4497956` |
| ranger_rifle | `Guns/models/special/mk43_rifle/diamond_bayonet.json` | `d237ac7c7346f824` |
| ranger_rifle | `Guns/models/special/mk43_rifle/netherite_bayonet.json` | `a557f14d54843273` |
| ranger_rifle | `Guns/models/special/mk43_rifle/stan_mag.json` | `1359bb99b00c9fcb` |
| ranger_rifle | `Guns/models/special/mk43_rifle/ext_mag.json` | `fa205c62f65939db` |
| ranger_rifle | `Guns/models/special/mk43_rifle/speed_mag.json` | `6ed2ab4c57c04991` |
| ranger_rifle | `Guns/sounds/item/old_rifle/fire.ogg` | `d684ae9a09c9c12c` |
| kestrel_rifle | `Guns/models/item/whistler.json` | `8429cd08e20c88a6` |
| kestrel_rifle | `Guns/item/whistler.png` | `4a1bd233c0f81437` |
| kestrel_rifle | `Guns/item/whistler.animation.json` | `67236a76ed1956fb` |
| kestrel_rifle | `Guns/models/special/whistler/main.json` | `d2a3f7ccbdc663b7` |
| kestrel_rifle | `Guns/models/special/whistler/stan_barrel.json` | `d68f47a93d0589f6` |
| kestrel_rifle | `Guns/models/special/whistler/silencer.json` | `7c05a2a52319be72` |
| kestrel_rifle | `Guns/models/special/whistler/advanced_silencer.json` | `f6910e527d37fc08` |
| kestrel_rifle | `Guns/models/special/whistler/muzzle_brake.json` | `c7d031c9f079eedb` |
| kestrel_rifle | `Guns/models/special/whistler/ext_barrel.json` | `4ba3a81e66aabc42` |
| kestrel_rifle | `Guns/models/special/whistler/stan_grip.json` | `42ddd6a3b3a48b15` |
| kestrel_rifle | `Guns/models/special/whistler/light_stock.json` | `9af6c40abcb41953` |
| kestrel_rifle | `Guns/models/special/whistler/heavy_stock.json` | `024f49e889212619` |
| kestrel_rifle | `Guns/models/special/whistler/wooden_stock.json` | `e741af78fd941719` |
| kestrel_rifle | `Guns/models/special/whistler/light_grip.json` | `5a158e12bdfe7e06` |
| kestrel_rifle | `Guns/models/special/whistler/iron_bayonet.json` | `733ccc838333d852` |
| kestrel_rifle | `Guns/models/special/whistler/anthralite_bayonet.json` | `351b26e635adb2ec` |
| kestrel_rifle | `Guns/models/special/whistler/diamond_bayonet.json` | `803dcc3cd2cfaafb` |
| kestrel_rifle | `Guns/models/special/whistler/netherite_bayonet.json` | `c4c4bc3a0e4fbf77` |
| kestrel_rifle | `Guns/models/special/whistler/sights.json` | `8b88d550996656f4` |
| kestrel_rifle | `Guns/sounds/item/iron_rifle/fire.ogg` | `e4a92914502ffd1c` |
| kestrel_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| kestrel_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| kestrel_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| rattler_pistol | `Guns/models/item/auvtomag.json` | `de5159b6960d2679` |
| rattler_pistol | `Guns/item/auvtomag.png` | `b212208d41e4b043` |
| rattler_pistol | `Guns/item/auvtomag.animation.json` | `dd5d796656f62062` |
| rattler_pistol | `Guns/models/special/auvtomag/main.json` | `eb191dfba993bffb` |
| rattler_pistol | `Guns/models/special/auvtomag/stan_barrel.json` | `fa7648e49973d557` |
| rattler_pistol | `Guns/models/special/auvtomag/silencer.json` | `22c9850a0236af50` |
| rattler_pistol | `Guns/models/special/auvtomag/advanced_silencer.json` | `2d497f71348c2ce3` |
| rattler_pistol | `Guns/models/special/auvtomag/muzzle_brake.json` | `038beb67c8dbd9ab` |
| rattler_pistol | `Guns/models/special/auvtomag/ext_barrel.json` | `6b1a08ea09edb4fd` |
| rattler_pistol | `Guns/models/special/auvtomag/receiver.json` | `2e34a18e135945ff` |
| rattler_pistol | `Guns/models/special/auvtomag/sights.json` | `aae76abd69f12c65` |
| rattler_pistol | `Guns/models/special/auvtomag/no_sights.json` | `f95797101760f89d` |
| rattler_pistol | `Guns/models/special/auvtomag/stan_mag.json` | `25a51f89da2625bd` |
| rattler_pistol | `Guns/models/special/auvtomag/ext_mag.json` | `a7d8f2a888ee3461` |
| rattler_pistol | `Guns/models/special/auvtomag/speed_mag.json` | `574b248499a1a35c` |
| rattler_pistol | `Guns/sounds/item/iron_rifle/enchanted_fire.ogg` | `ec646e1be4185e5e` |
| rattler_pistol | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| rattler_pistol | `Guns/item/medium_scope.png` | `543abecf859783be` |
| rattler_pistol | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| bronco_smg | `Guns/models/item/jr_wristbreaker.json` | `97dd6dbf15640210` |
| bronco_smg | `Guns/item/jr_wristbreaker.png` | `8968124e606e2100` |
| bronco_smg | `Guns/item/jr_wristbreaker.animation.json` | `bf8b3331ddb2577d` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/main.json` | `5a0011790d5a29d3` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/stan_barrel.json` | `2c66f81c1115217a` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/silencer.json` | `e52236d565008d1b` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/advanced_silencer.json` | `b165c4bda6d33b9c` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/muzzle_brake.json` | `f4e0ab737b2674a3` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/ext_barrel.json` | `04b9968999bb2d00` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/bolt.json` | `a8ae9b57b2f2f3b3` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/stan_mag.json` | `98da59b297ea0dea` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/ext_mag.json` | `9330692f68bdf53e` |
| bronco_smg | `Guns/models/special/jr_wristbreaker/speed_mag.json` | `5a688d68d4063088` |
| bronco_smg | `Guns/sounds/item/new_rifle/fire_2.ogg` | `dba5369742b5ecf1` |
| squall_rifle | `Guns/models/item/gale.json` | `b328803c68c19928` |
| squall_rifle | `Guns/item/gale.png` | `0cd567177e66e335` |
| squall_rifle | `Guns/item/gale.animation.json` | `ed5d3f3017f1ac88` |
| squall_rifle | `Guns/models/special/gale/main.json` | `f6b003c119501251` |
| squall_rifle | `Guns/models/special/gale/stan_grip.json` | `079689636b79f492` |
| squall_rifle | `Guns/models/special/gale/light_stock.json` | `ecf0ceb67d4b5d65` |
| squall_rifle | `Guns/models/special/gale/heavy_stock.json` | `a14f97f62f69967a` |
| squall_rifle | `Guns/models/special/gale/wooden_stock.json` | `2a86be4ace47454c` |
| squall_rifle | `Guns/models/special/gale/light_grip.json` | `926c5281f72f5e6f` |
| squall_rifle | `Guns/models/special/gale/iron_bayonet.json` | `5c3e57d04d109b81` |
| squall_rifle | `Guns/models/special/gale/anthralite_bayonet.json` | `324e5068e9569b21` |
| squall_rifle | `Guns/models/special/gale/diamond_bayonet.json` | `3d1a50dd388d9179` |
| squall_rifle | `Guns/models/special/gale/netherite_bayonet.json` | `199b71fee56e19c0` |
| squall_rifle | `Guns/models/special/gale/sights.json` | `ffc1cca96f33d14f` |
| squall_rifle | `Guns/sounds/item/airgun/fire.ogg` | `ee6701ec0f70a3fc` |
| squall_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| squall_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| squall_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| spikedriver | `Guns/models/item/railworker.json` | `979cbe87514a0ff6` |
| spikedriver | `Guns/item/railworker.png` | `258bbe596771c65f` |
| spikedriver | `Guns/item/railworker.animation.json` | `1ce55851ba0cf1e0` |
| spikedriver | `Guns/models/special/railworker/main.json` | `4d06672e81efe1b0` |
| spikedriver | `Guns/models/special/railworker/light_stock.json` | `11e8cd7f765a62ed` |
| spikedriver | `Guns/models/special/railworker/heavy_stock.json` | `2b247b9edd81a27d` |
| spikedriver | `Guns/models/special/railworker/wooden_stock.json` | `2f1b2250c77e4cae` |
| spikedriver | `Guns/models/special/railworker/stan_mag.json` | `82496e671ec161be` |
| spikedriver | `Guns/models/special/railworker/ext_mag.json` | `8277666b3144da76` |
| spikedriver | `Guns/models/special/railworker/speed_mag.json` | `eb2efdd181bca11c` |
| spikedriver | `Guns/sounds/item/rail/fire.ogg` | `cf761effacf65fe0` |
| seam_cutter | `Guns/models/item/cr4k_mining_laser.json` | `54082c197dd1a56b` |
| seam_cutter | `Guns/item/cr4k_mining_laser.png` | `25b7f19e590ed7e4` |
| seam_cutter | `Guns/item/cr4k_mining_laser.animation.json` | `0d19d1770fe55c8d` |
| seam_cutter | `Guns/models/special/cr4k_mining_laser/main.json` | `3e1a7de18359a849` |
| seam_cutter | `Guns/sounds/item/laser/fire_2.ogg` | `a585dc4d697303f0` |
| caisson_pistol | `Guns/models/item/hyperbaria.json` | `76d8d5a55a2bdee2` |
| caisson_pistol | `Guns/item/hyperbaria.png` | `9553bf873ddd3f9b` |
| caisson_pistol | `Guns/item/hyperbaria.animation.json` | `4292c3320858a0a2` |
| caisson_pistol | `Guns/models/special/hyperbaria/main.json` | `5b79a0290ad9e960` |
| caisson_pistol | `Guns/models/special/hyperbaria/bolt.json` | `5eca6988a624610e` |
| caisson_pistol | `Guns/sounds/item/plasma/fire.ogg` | `fef225846fbbfe9a` |
| sledge | `Guns/models/item/killer_23.json` | `09b5baef810a83a2` |
| sledge | `Guns/item/killer_23.png` | `ce9268d08cf92ec4` |
| sledge | `Guns/item/killer_23.animation.json` | `4d9b1ce6014ba4a4` |
| sledge | `Guns/models/special/killer_23/main.json` | `1bf423412a7ce963` |
| sledge | `Guns/models/special/killer_23/stan_barrel.json` | `d0d9e338bc8be7db` |
| sledge | `Guns/models/special/killer_23/silencer.json` | `8df2d926fea81707` |
| sledge | `Guns/models/special/killer_23/advanced_silencer.json` | `b0b93c10a6d8dadc` |
| sledge | `Guns/models/special/killer_23/muzzle_brake.json` | `61595f40c72658ec` |
| sledge | `Guns/models/special/killer_23/ext_barrel.json` | `69f7a7c1b9e651c2` |
| sledge | `Guns/models/special/killer_23/light_stock.json` | `262f8bcc7481281f` |
| sledge | `Guns/models/special/killer_23/heavy_stock.json` | `25b9ab892b6bd0d4` |
| sledge | `Guns/models/special/killer_23/wooden_stock.json` | `9cb0e34b8be28ba8` |
| sledge | `Guns/models/special/killer_23/light_grip.json` | `a6da77b8b5fcde5a` |
| sledge | `Guns/models/special/killer_23/iron_bayonet.json` | `643202f095c61508` |
| sledge | `Guns/models/special/killer_23/anthralite_bayonet.json` | `0bbcc0ff58811bb9` |
| sledge | `Guns/models/special/killer_23/diamond_bayonet.json` | `d66dd08f49626ebb` |
| sledge | `Guns/models/special/killer_23/netherite_bayonet.json` | `d26d8286e2e1c9d2` |
| sledge | `Guns/models/special/killer_23/bolt.json` | `5f7a031fa371e34e` |
| sledge | `Guns/sounds/item/makeshift_rifle/enchanted_fire.ogg` | `e40d4c4627d61011` |
| highwayman | `Guns/models/item/turnpike.json` | `09b5baef810a83a2` |
| highwayman | `Guns/item/turnpike.png` | `63e9b3911c1b9cd4` |
| highwayman | `Guns/item/turnpike.animation.json` | `78706c1f096e7dfe` |
| highwayman | `Guns/models/special/turnpike/main.json` | `1fd72a5ebe338f7b` |
| highwayman | `Guns/models/special/turnpike/stan_barrel.json` | `7914706e051168e5` |
| highwayman | `Guns/models/special/turnpike/silencer.json` | `f4c1c5a5bacb7d7e` |
| highwayman | `Guns/models/special/turnpike/advanced_silencer.json` | `727f8bda385304b7` |
| highwayman | `Guns/models/special/turnpike/muzzle_brake.json` | `9eba81a037491c05` |
| highwayman | `Guns/models/special/turnpike/ext_barrel.json` | `b3f54959959ae705` |
| highwayman | `Guns/models/special/turnpike/light_stock.json` | `848c245a1c716f68` |
| highwayman | `Guns/models/special/turnpike/heavy_stock.json` | `e8058d26c949f966` |
| highwayman | `Guns/models/special/turnpike/wooden_stock.json` | `a943065e49b05c51` |
| highwayman | `Guns/models/special/turnpike/light_grip.json` | `dc2e8c9153f8f8a7` |
| highwayman | `Guns/models/special/turnpike/iron_bayonet.json` | `49299e9d33758f66` |
| highwayman | `Guns/models/special/turnpike/anthralite_bayonet.json` | `e053b5945d2a8e32` |
| highwayman | `Guns/models/special/turnpike/diamond_bayonet.json` | `f69220679380377d` |
| highwayman | `Guns/models/special/turnpike/netherite_bayonet.json` | `62b06b54184abf96` |
| highwayman | `Guns/models/special/turnpike/sights.json` | `d69f11270dbdffbe` |
| highwayman | `Guns/models/special/turnpike/bolt.json` | `33eecd726a57a504` |
| highwayman | `Guns/sounds/item/boomstick/enchanted_fire.ogg` | `32dec23df85f1862` |
| highwayman | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| highwayman | `Guns/item/medium_scope.png` | `543abecf859783be` |
| highwayman | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| throttle | `Guns/models/item/venturi.json` | `8d0478b0b34a0da8` |
| throttle | `Guns/item/venturi.png` | `905b853f1d100147` |
| throttle | `Guns/item/venturi.animation.json` | `47f32a37b3699805` |
| throttle | `Guns/models/special/venturi/main.json` | `994bdd0d8fbcfb06` |
| throttle | `Guns/models/special/venturi/stan_grip.json` | `38d8f2992379adc3` |
| throttle | `Guns/models/special/venturi/light_stock.json` | `293184dd354446fd` |
| throttle | `Guns/models/special/venturi/heavy_stock.json` | `240d02923eef20a2` |
| throttle | `Guns/models/special/venturi/wooden_stock.json` | `6d3cca4fe24a984a` |
| throttle | `Guns/models/special/venturi/sights.json` | `3358a59f3ad5ba95` |
| throttle | `Guns/models/special/venturi/bolt.json` | `bae989b521fabc11` |
| throttle | `Guns/sounds/item/plasma_shotgun/fire.ogg` | `70022fd04919c03f` |
| throttle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| throttle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| throttle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| energy_cell | `Guns/item/energy_cell.png` | `857c3e9d98c18f97` |
| energy_cell_empty | `Guns/item/empty_cell.png` | `b06623e4de7b9b8f` |
| shared | `Guns/sounds/item/bolt/bolt.ogg` | `1cf1102f6ba52725` |
| shared | `Guns/sounds/item/bolt_pull/bolt_pull.ogg` | `dbbda8b00abcab8c` |
| shared | `Guns/sounds/item/bolt_release/bolt_release.ogg` | `7c1096f545d72ec3` |
| shared | `Guns/sounds/item/gun_rustle/gun_rustle.ogg` | `aeec657cb4c25acd` |
| shared | `Guns/sounds/item/gun_sounds/insert.ogg` | `cbc0479276e5262c` |
| shared | `Guns/sounds/item/lever/lever.ogg` | `0b6c3fb22142e42e` |
| shared | `Guns/sounds/item/mag_in/mag_in.ogg` | `9595cc14d1209f85` |
| shared | `Guns/sounds/item/mag_out/mag_out.ogg` | `5f805eaadc8fd476` |
| shared | `Guns/sounds/item/gun_sounds/metal.ogg` | `3c48586e2406bdea` |
| shared | `Guns/sounds/item/gun_sounds/pump.ogg` | `6cc42f310726dff5` |
| shared | `Guns/sounds/item/gun_sounds/pump_half.ogg` | `df22da6f35b95faa` |
| shared | `Guns/sounds/item/rack/rack.ogg` | `aa98a804ed7809ab` |
| shared | `Guns/sounds/item/reload_end/reload_end.ogg` | `c1db5357e55ed953` |
| shared | `Guns/sounds/item/rusty_gnat/copper_jam.ogg` | `d1b93136045c83cc` |
| shared | `Guns/sounds/item/slap/slap.ogg` | `ed6fbb36974a444a` |

Slice 5's files: each gun's attachment parts, and each attachment's item model and texture (copied to `textures/item/guns/attachments/<name>.png`):

| Gun or attachment | Library file | SHA-256 (first 16) |
|---|---|---|
| rust_midge | `Guns/models/special/rusty_gnat/stock_light.json` | `f89f9862600c31ef` |
| rust_midge | `Guns/models/special/rusty_gnat/stock_weighted.json` | `a0339d40d9cc62cf` |
| rust_midge | `Guns/models/special/rusty_gnat/stock_wooden.json` | `8925d4a0861f75a2` |
| rust_midge | `Guns/models/special/rusty_gnat/silencer.json` | `7992f7b7c39283d9` |
| rust_midge | `Guns/models/special/rusty_gnat/advanced_silencer.json` | `4f8e878fa1f24ff2` |
| rust_midge | `Guns/models/special/rusty_gnat/muzzle_brake.json` | `ebd59126a5057c32` |
| rust_midge | `Guns/models/special/rusty_gnat/ext_barrel.json` | `0fa9b18a2b500014` |
| rust_midge | `Guns/models/special/rusty_gnat/ext_mag.json` | `d3fb272f16cdba67` |
| rust_midge | `Guns/models/special/rusty_gnat/speed_mag.json` | `6c1e0d6dbb380fc6` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/silencer.json` | `dffc0edb1e71755e` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/advanced_silencer.json` | `91afc30e748e26b0` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/muzzle_brake.json` | `f71cac197c938957` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/ext_barrel.json` | `c77d09365b3319fc` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/stock_light.json` | `dac577681811ba5e` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/stock_weighted.json` | `9015f21f7e8a80a0` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/stock_wooden.json` | `c2fe96d4aaa65326` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/light_grip.json` | `d4ca3eaba519db7c` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/vertical_grip.json` | `4745ab80863d07a0` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/ext_mag.json` | `79e5c531276a42f6` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/speed_mag.json` | `223c1ed7dde033ce` |
| thunderpipe | `Guns/models/special/boomstick/stock_light.json` | `741f67115e17eb74` |
| thunderpipe | `Guns/models/special/boomstick/stock_weighted.json` | `d2b1bde81916a3ea` |
| thunderpipe | `Guns/models/special/boomstick/stock_wooden.json` | `17478321875df40d` |
| thunderpipe | `Guns/models/special/boomstick/grip_light.json` | `69354d23e651023f` |
| thunderpipe | `Guns/models/special/boomstick/grip_vertical.json` | `606dfa6f76926cd6` |
| thunderpipe | `Guns/models/special/boomstick/silencer.json` | `817c4466213b999c` |
| thunderpipe | `Guns/models/special/boomstick/advanced_silencer.json` | `f63167ba66948f9d` |
| thunderpipe | `Guns/models/special/boomstick/muzzle_brake.json` | `843ea4ed4bebdece` |
| thunderpipe | `Guns/models/special/boomstick/ext_barrel.json` | `dab35bd7189c1fc2` |
| warden_pistol | `Guns/models/special/defender_pistol/silencer.json` | `7a8d0cfd65d78b73` |
| warden_pistol | `Guns/models/special/defender_pistol/advanced_silencer.json` | `ca7ee9d5a5f47d8e` |
| warden_pistol | `Guns/models/special/defender_pistol/muzzle_brake.json` | `feb08e673e6f0537` |
| warden_pistol | `Guns/models/special/defender_pistol/ext_barrel.json` | `5f915c9b732d162b` |
| warden_pistol | `Guns/models/special/defender_pistol/ext_mag.json` | `b04afe2b90af3acd` |
| warden_pistol | `Guns/models/special/defender_pistol/speed_mag.json` | `0b48d5f83ddb9d4f` |
| riveter_smg | `Guns/models/special/greaser_smg/silencer.json` | `ae24d4720cdab105` |
| riveter_smg | `Guns/models/special/greaser_smg/advanced_silencer.json` | `3eb2f44c0d7119d1` |
| riveter_smg | `Guns/models/special/greaser_smg/muzzle_brake.json` | `ef6827ac9edbf547` |
| riveter_smg | `Guns/models/special/greaser_smg/ext_barrel.json` | `25b9012c673d7378` |
| riveter_smg | `Guns/models/special/greaser_smg/light_stock.json` | `985e1ffda3e8b38c` |
| riveter_smg | `Guns/models/special/greaser_smg/heavy_stock.json` | `77f98977d10dfb6e` |
| riveter_smg | `Guns/models/special/greaser_smg/wooden_stock.json` | `d09f18d87edd8dd8` |
| riveter_smg | `Guns/models/special/greaser_smg/ext_mag.json` | `479107b02452fd12` |
| riveter_smg | `Guns/models/special/greaser_smg/speed_mag.json` | `d942ea99e194948b` |
| haymaker | `Guns/models/special/bruiser/silencer.json` | `d7fa4a7a73675bd0` |
| haymaker | `Guns/models/special/bruiser/advanced_silencer.json` | `2fedfc9db6cb22d1` |
| haymaker | `Guns/models/special/bruiser/muzzle_brake.json` | `598d5dde49e75d04` |
| haymaker | `Guns/models/special/bruiser/ext_barrel.json` | `569b881c2d9d7d1b` |
| longhorn_rifle | `Guns/models/special/marlin/silencer.json` | `06bf50eaea127309` |
| longhorn_rifle | `Guns/models/special/marlin/advanced_silencer.json` | `4b7fe982ca0d6f0f` |
| longhorn_rifle | `Guns/models/special/marlin/muzzle_brake.json` | `ebeda0ce9770bfb1` |
| longhorn_rifle | `Guns/models/special/marlin/ext_barrel.json` | `b15a641d1ee802e9` |
| longhorn_rifle | `Guns/models/special/marlin/light_stock.json` | `35101c5b5528e6c1` |
| longhorn_rifle | `Guns/models/special/marlin/heavy_stock.json` | `581c80a1bd6b41f1` |
| longhorn_rifle | `Guns/models/special/marlin/wooden_stock.json` | `2af1dbde0f745927` |
| longhorn_rifle | `Guns/models/special/marlin/light_grip.json` | `266eff6b2f577212` |
| longhorn_rifle | `Guns/models/special/marlin/vert_grip.json` | `9179d93c68a1ff68` |
| silencer | `Guns/models/item/silencer.json` | `049d959e9534d047` |
| texture `baffled_silencer` | `Guns/models/item/advanced_silencer.json` | `4c139fb12152d923` |
| muzzle_brake | `Guns/models/item/muzzle_brake.json` | `47dff9ea7cd62512` |
| texture `extended_barrel` | `Guns/models/item/extended_barrel.json` | `c5d63e72f9e28a5f` |
| texture `extended_magazine` | `Guns/models/item/extended_mag.json` | `ef32296bd1f9a1cc` |
| texture `speed_magazine` | `Guns/models/item/speed_mag.json` | `990831d677e8b769` |
| texture `light_stock` | `Guns/models/item/light_stock.json` | `709b176621240c73` |
| texture `weighted_stock` | `Guns/models/item/weighted_stock.json` | `1e0aa4edf441a53d` |
| texture `wooden_stock` | `Guns/models/item/wooden_stock.json` | `dceb43555135ec17` |
| light_grip | `Guns/models/item/light_grip.json` | `8965dcdf94effedb` |
| vertical_grip | `Guns/models/item/vertical_grip.json` | `31667ebc84eaa55d` |
| texture `muzzle_devices` | `Guns/item/greaser_smg_barrels.png` | `e912ec7c95188909` |
| texture `baffled_silencer` | `Guns/item/advanced_silencer.png` | `f94615d5c4cdd84c` |
| texture `extended_barrel` | `Guns/item/extended_barrel.png` | `2fc5027a3f329c25` |
| texture `extended_magazine` | `Guns/item/extended_mag.png` | `0b34bd0828783d13` |
| texture `speed_magazine` | `Guns/item/carabine.png` | `305297d9327603b1` |
| texture `light_stock` | `Guns/item/light_stock.png` | `2b4a27580b4871ae` |
| texture `weighted_stock` | `Guns/item/greaser_smg_stocks.png` | `247deccc5517be15` |
| texture `wooden_stock` | `Guns/item/musket_stocks.png` | `848fb17c2a71ad8d` |
| texture `grips` | `Guns/item/carabine_grips.png` | `e54eb16b4a01aa0b` |
| light_round_casing | `Guns/item/small_copper_casing.png` | `534d4d0375da46cc` |
| rifle_round_casing | `Guns/item/large_brass_casing.png` | `2b43b9961479bdbb` |
| buckshot_shell_casing | `Guns/item/shotgun_shell.png` | `89657710d20d041d` |
| flash_0 | `Big Cannons and Mounted Guns/textures/muzzleflash.png` | `9e6b36b22790ec34` |
| flash_1 | `Big Cannons and Mounted Guns/textures/muzzleflash2.png` | `31029be6554ebd9f` |
| flash_2 | `Big Cannons and Mounted Guns/textures/muzzleflash3.png` | `5c695fc1d9c5b365` |
| flash_3 | `Big Cannons and Mounted Guns/textures/muzzleflash4.png` | `189ef124242eaa98` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/iron_bayonet.json` | `fd7d5b1b45d681a4` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/anthralite_bayonet.json` | `815aea98eeb13043` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/diamond_bayonet.json` | `46735b82875edf0e` |
| patchwork_carbine | `Guns/models/special/makeshift_rifle/netherite_bayonet.json` | `5ffc2005b15bff2e` |
| thunderpipe | `Guns/models/special/boomstick/iron_bayonet.json` | `dedc0f2f291b357f` |
| thunderpipe | `Guns/models/special/boomstick/anthralite_bayonet.json` | `58e120d3a0dc3692` |
| thunderpipe | `Guns/models/special/boomstick/diamond_bayonet.json` | `f093c45484d3a732` |
| thunderpipe | `Guns/models/special/boomstick/netherite_bayonet.json` | `52793ba001a50581` |
| longhorn_rifle | `Guns/models/special/marlin/iron_bayonet.json` | `5ea1cf38b1cae83b` |
| longhorn_rifle | `Guns/models/special/marlin/anthralite_bayonet.json` | `114add26956515e8` |
| longhorn_rifle | `Guns/models/special/marlin/diamond_bayonet.json` | `ada6f62d7bd00777` |
| longhorn_rifle | `Guns/models/special/marlin/netherite_bayonet.json` | `6e046c9c441e1c77` |
| drover_rifle | `Guns/models/special/winnie/silencer.json` | `46a106c68f3b9acc` |
| drover_rifle | `Guns/models/special/winnie/advanced_silencer.json` | `0ba9ea154fd1c29e` |
| drover_rifle | `Guns/models/special/winnie/muzzle_brake.json` | `35232221bcd934fd` |
| drover_rifle | `Guns/models/special/winnie/ext_barrel.json` | `c3102b0d3a24de87` |
| drover_rifle | `Guns/models/special/winnie/light_grip.json` | `2c40e0b3c5c89703` |
| drover_rifle | `Guns/models/special/winnie/iron_bayonet.json` | `25131e44a4015957` |
| drover_rifle | `Guns/models/special/winnie/anthralite_bayonet.json` | `689d50f88cc9bfd1` |
| drover_rifle | `Guns/models/special/winnie/diamond_bayonet.json` | `267d49add46a2579` |
| drover_rifle | `Guns/models/special/winnie/netherite_bayonet.json` | `f102098b23e5dff6` |
| drover_rifle | `Guns/item/advanced_silencer.png` | `f94615d5c4cdd84c` |
| drover_rifle | `Guns/item/carabine_grips.png` | `e54eb16b4a01aa0b` |
| drover_rifle | `Guns/item/greaser_smg_barrels.png` | `e912ec7c95188909` |
| drover_rifle | `Guns/item/makeshift_rifle_bayonets.png` | `dfc93f5032504095` |
| coach_gun | `Guns/models/special/callwell/light_grip.json` | `1d9cf66979d3b6c6` |
| coach_gun | `Guns/models/special/callwell/iron_bayonet.json` | `0f47d60920ffe6cd` |
| coach_gun | `Guns/models/special/callwell/anthralite_bayonet.json` | `87f6acec33f5554b` |
| coach_gun | `Guns/models/special/callwell/diamond_bayonet.json` | `ad1f37ed70be7c3f` |
| coach_gun | `Guns/models/special/callwell/netherite_bayonet.json` | `83ba23f2e811940b` |
| coach_gun | `Guns/item/carabine_grips.png` | `e54eb16b4a01aa0b` |
| coach_gun | `Guns/item/makeshift_rifle_bayonets.png` | `dfc93f5032504095` |
| duelling_pistol | `Guns/models/special/flintlock_pistol/light_stock.json` | `024de83c7d6232ed` |
| duelling_pistol | `Guns/models/special/flintlock_pistol/heavy_stock.json` | `d478328ccbef3984` |
| duelling_pistol | `Guns/models/special/flintlock_pistol/wooden_stock.json` | `0cf64a73c5e531d2` |
| duelling_pistol | `Guns/item/flintlock_stocks.png` | `f08f21f3c71aafed` |
| line_musket | `Guns/models/special/musket/light_stock.json` | `a608540600bd9c78` |
| line_musket | `Guns/models/special/musket/heavy_stock.json` | `3d715a98ab3195fd` |
| line_musket | `Guns/models/special/musket/wooden_stock.json` | `cafc11238b17b3d3` |
| line_musket | `Guns/models/special/musket/light_grip.json` | `eb4cf964a249f4fc` |
| line_musket | `Guns/models/special/musket/vert_grip.json` | `3a8ebd80047712e9` |
| line_musket | `Guns/models/special/musket/iron_bayonet.json` | `d723788a1b9f6e99` |
| line_musket | `Guns/models/special/musket/anthralite_bayonet.json` | `1eaccdbb7a9b5fbd` |
| line_musket | `Guns/models/special/musket/diamond_bayonet.json` | `291bb6a540371af6` |
| line_musket | `Guns/models/special/musket/netherite_bayonet.json` | `267a94385517eb04` |
| line_musket | `Guns/item/musket_bayonets.png` | `0830d15655ef543b` |
| line_musket | `Guns/item/musket_stocks.png` | `848fb17c2a71ad8d` |
| line_musket | `Guns/item/musket_grips.png` | `7f451a500ba89baa` |
| bellmouth | `Guns/models/special/blunderbuss/light_grip.json` | `9b57e017c6ec6be9` |
| bellmouth | `Guns/models/special/blunderbuss/vert_grip.json` | `1c12cccb8864994d` |
| bellmouth | `Guns/models/special/blunderbuss/iron_bayonet.json` | `486b32647347a744` |
| bellmouth | `Guns/models/special/blunderbuss/anthralite_bayonet.json` | `51b6c59e0ab545bb` |
| bellmouth | `Guns/models/special/blunderbuss/diamond_bayonet.json` | `289974a1f5a20182` |
| bellmouth | `Guns/models/special/blunderbuss/netherite_bayonet.json` | `b69269d8a63c8f4e` |
| bellmouth | `Guns/item/musket_bayonets.png` | `0830d15655ef543b` |
| bellmouth | `Guns/item/musket_grips.png` | `7f451a500ba89baa` |
| iron_bayonet | `Guns/models/item/iron_bayonet.json` | `6daa8ca0d9223b8e` |
| steel_bayonet | `Guns/models/item/anthralite_bayonet.json` | `c8e0836f6f6e3098` |
| diamond_bayonet | `Guns/models/item/diamond_bayonet.json` | `fcc0759915935a9a` |
| netherite_bayonet | `Guns/models/item/netherite_bayonet.json` | `89e257ea581a1a6e` |
| iron_bayonet | `Guns/item/iron_bayonet.png` | `b3bc20be7702ac39` |
| steel_bayonet | `Guns/item/anthralite_bayonet.png` | `a9d69f394988fc4f` |
| diamond_bayonet | `Guns/item/diamond_bayonet.png` | `2262785d6c0da5d0` |
| netherite_bayonet | `Guns/item/netherite_bayonet.png` | `f7a49396eac13816` |
| riveter_smg | `Guns/models/special/greaser_smg/no_sights.json` | `d2c1b829eee2515b` |
| riveter_smg | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| riveter_smg | `Guns/item/medium_scope.png` | `543abecf859783be` |
| riveter_smg | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| longhorn_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| longhorn_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| longhorn_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| drover_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| drover_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| drover_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| long_scope | `Guns/models/item/long_scope.json` | `bae03e351ad5a43a` |
| medium_scope | `Guns/models/item/medium_scope.json` | `1b6f5f95ac5d8401` |
| reflex_sight | `Guns/models/item/reflex_sight.json` | `0c08e5f6e11cff90` |
| long_scope | `Guns/models/item/long_scope.scmeta` | `6e82f87ecc2adc09` |
| medium_scope | `Guns/models/item/medium_scope.scmeta` | `6e82f87ecc2adc09` |
| reflex_sight | `Guns/models/item/reflex_sight.scmeta` | `bbd4957a6b334df6` |
| long_scope | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| medium_scope | `Guns/item/medium_scope.png` | `543abecf859783be` |
| reflex_sight | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| long_scope_reticle2 | `Guns/effect/long_scope_reticle2.png` | `03fd80678ffafd2f` |
| scope_vignette | `Guns/effect/scope_vignette.png` | `bb5e4a463e62511e` |
| scope_vignette_circle | `Guns/effect/scope_vignette_circle.png` | `87e34da0197d0647` |
| red_dot_reticle | `Guns/effect/red_dot_reticle.png` | `8ebc215134886a8f` |

Slice 9E's files: the guns' tactical grip parts and the Laser Sight's textures merged into their atlases; the Tactical Grip's item model (the Vertical Grip's, three of its pieces); the Laser Sight's item model, line of sight and textures (copied to `textures/item/guns/attachments/laser_sight.png` and `laser_beam.png`); and the laser's dot (copied to `textures/particle/laser_dot.png`):

| Gun or attachment | Library file | SHA-256 (first 16) |
|---|---|---|
| riveter_smg | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| riveter_smg | `Guns/item/laser.png` | `aac5f06e892e350a` |
| longhorn_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| longhorn_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| drover_rifle | `Guns/models/special/winnie/tact_grip.json` | `2f9009485f2da067` |
| drover_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| drover_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| coach_gun | `Guns/models/special/callwell/tact_grip.json` | `ffe6dbf7c158aee8` |
| bulldog_pistol | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| bulldog_pistol | `Guns/item/laser.png` | `aac5f06e892e350a` |
| garrison_rifle | `Guns/models/special/stigg/tact_grip.json` | `952c182314f713c3` |
| garrison_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| garrison_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| breacher | `Guns/models/special/combat_shotgun/tact_grip.json` | `204b2902815848f7` |
| stormlock_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| stormlock_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| linesman | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| linesman | `Guns/item/laser.png` | `aac5f06e892e350a` |
| picket_rifle | `Guns/models/special/m3_marksman/tact_grip.json` | `d6df0ef5e64587d3` |
| ranger_rifle | `Guns/models/special/mk43_rifle/tact_grip.json` | `0f7a7b95848ac428` |
| kestrel_rifle | `Guns/models/special/whistler/tact_grip.json` | `fc22f38e2c00920e` |
| kestrel_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| kestrel_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| rattler_pistol | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| rattler_pistol | `Guns/item/laser.png` | `aac5f06e892e350a` |
| squall_rifle | `Guns/models/special/gale/tact_grip.json` | `bae35e1c5165671e` |
| squall_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| squall_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| sledge | `Guns/models/special/killer_23/tact_grip.json` | `26b9feb1a4af5396` |
| highwayman | `Guns/models/special/turnpike/tact_grip.json` | `f70142850dae3798` |
| highwayman | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| highwayman | `Guns/item/laser.png` | `aac5f06e892e350a` |
| throttle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| throttle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| tactical_grip | `Guns/models/item/vertical_grip.json` | `31667ebc84eaa55d` |
| laser_sight | `Guns/models/item/laser_sight.json` | `f35a59647a131853` |
| laser_sight | `Guns/models/item/laser_sight.scmeta` | `bbd4957a6b334df6` |
| laser_sight | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| laser_beam | `Guns/item/laser.png` | `aac5f06e892e350a` |
| laser_dot | `Guns/effect/red_dot_reticle.png` | `8ebc215134886a8f` |
| earthmover | `Guns/models/item/dozier_rl.json` | `63d00eb0a827fb67` |
| earthmover | `Guns/item/dozier_rl.png` | `5657bbdc3a8a3439` |
| earthmover | `Guns/item/dozier_rl.animation.json` | `693ac6d3023f7fdb` |
| earthmover | `Guns/models/special/dozier_rl/main.json` | `9befa3a2d032c594` |
| earthmover | `Guns/models/special/dozier_rl/light_grip.json` | `60b02278a04d905e` |
| earthmover | `Guns/models/special/dozier_rl/iron_bayonet.json` | `a09fafdff56a8463` |
| earthmover | `Guns/models/special/dozier_rl/anthralite_bayonet.json` | `08e43ce44448d9ab` |
| earthmover | `Guns/models/special/dozier_rl/diamond_bayonet.json` | `00a8d5654bb1c113` |
| earthmover | `Guns/models/special/dozier_rl/netherite_bayonet.json` | `e90897f94e73cb50` |
| earthmover | `Guns/models/special/dozier_rl/tact_grip.json` | `3869c5684edd9cf2` |
| earthmover | `Guns/models/special/dozier_rl/sights.json` | `d4d1e33596063d35` |
| earthmover | `Guns/models/special/dozier_rl/no_sights.json` | `0f57a68c735f2bce` |
| earthmover | `Guns/models/special/dozier_rl/drum.json` | `2c83c014c5b5b8b9` |
| earthmover | `Guns/models/special/dozier_rl/fire.json` | `72926890def3209b` |
| earthmover | `Guns/sounds/item/bazooka/fire.ogg` | `a9e62ea7c9505223` |
| earthmover | `Guns/item/makeshift_rifle_bayonets.png` | `dfc93f5032504095` |
| earthmover | `Guns/item/gyrojet_flames.png` | `43a56fbf383f811d` |
| earthmover | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| earthmover | `Guns/item/medium_scope.png` | `543abecf859783be` |
| earthmover | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| skylark_rifle | `Guns/models/item/rocket_rifle.json` | `557660f19086b817` |
| skylark_rifle | `Guns/item/rocket_rifle.png` | `28052631d94139a0` |
| skylark_rifle | `Guns/item/rocket_rifle.animation.json` | `d4867634de241831` |
| skylark_rifle | `Guns/models/special/rocket_rifle/main.json` | `aaa54e882d00e11f` |
| skylark_rifle | `Guns/models/special/rocket_rifle/stan_grip.json` | `714d2b06d46d861f` |
| skylark_rifle | `Guns/models/special/rocket_rifle/light_stock.json` | `318acf78ff10c499` |
| skylark_rifle | `Guns/models/special/rocket_rifle/heavy_stock.json` | `b314e47c26174b39` |
| skylark_rifle | `Guns/models/special/rocket_rifle/wooden_stock.json` | `bd235dbacdd678c6` |
| skylark_rifle | `Guns/models/special/rocket_rifle/light_grip.json` | `2654e1e8c2aef143` |
| skylark_rifle | `Guns/models/special/rocket_rifle/iron_bayonet.json` | `2d1ce950f2bdff7b` |
| skylark_rifle | `Guns/models/special/rocket_rifle/anthralite_bayonet.json` | `400ff8507b2e7701` |
| skylark_rifle | `Guns/models/special/rocket_rifle/diamond_bayonet.json` | `b91dc36e220376e3` |
| skylark_rifle | `Guns/models/special/rocket_rifle/netherite_bayonet.json` | `70ecd0ae5a674313` |
| skylark_rifle | `Guns/models/special/rocket_rifle/tact_grip.json` | `7805d665908a34da` |
| skylark_rifle | `Guns/sounds/item/rocket_rifle/fire.ogg` | `556d63860044e3fa` |
| bullfrog | `Guns/models/item/blooper.json` | `4c1a97aa58a16ed0` |
| bullfrog | `Guns/item/blooper.png` | `1aa3f8793e2765dd` |
| bullfrog | `Guns/item/blooper.animation.json` | `0b35dfd6e8f498e5` |
| bullfrog | `Guns/models/special/blooper/main.json` | `43066e89de7dd35d` |
| bullfrog | `Guns/models/special/blooper/stan_grip.json` | `02d52ff17856ff7b` |
| bullfrog | `Guns/models/special/blooper/light_stock.json` | `80f930b8de776f2c` |
| bullfrog | `Guns/models/special/blooper/heavy_stock.json` | `b0b8ffb348d441d8` |
| bullfrog | `Guns/models/special/blooper/wooden_stock.json` | `7628d562e8f031a4` |
| bullfrog | `Guns/models/special/blooper/light_grip.json` | `a0ce087c15e7b2ee` |
| bullfrog | `Guns/models/special/blooper/iron_bayonet.json` | `60b740d90fb57c8b` |
| bullfrog | `Guns/models/special/blooper/anthralite_bayonet.json` | `b35fdd4eff7516ac` |
| bullfrog | `Guns/models/special/blooper/diamond_bayonet.json` | `4ad2349f30c86a0b` |
| bullfrog | `Guns/models/special/blooper/netherite_bayonet.json` | `cfc3dd242790decf` |
| bullfrog | `Guns/models/special/blooper/tact_grip.json` | `8302ff80288cf592` |
| bullfrog | `Guns/models/special/blooper/barrel.json` | `08657d0212eccb9d` |
| bullfrog | `Guns/sounds/item/airgun/fire_2.ogg` | `bea9865e81d9d5ff` |
| solenoid_rifle | `Guns/models/item/gauss_rifle.json` | `ca192fcb75dd02db` |
| solenoid_rifle | `Guns/item/gauss_rifle.png` | `45d0f3d19d57ccc2` |
| solenoid_rifle | `Guns/item/gauss_rifle.animation.json` | `269a3f8f592f4172` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/main.json` | `37ddce9d5c71b48b` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/light_stock.json` | `a059da7598e902e7` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/heavy_stock.json` | `de18c99c52451169` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/wooden_stock.json` | `dcecc9a6ea92be9b` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/light_grip.json` | `717af25a4f9e5ba4` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/iron_bayonet.json` | `d2a90a9b1c229e44` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/anthralite_bayonet.json` | `4100a6a9ab09d68a` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/diamond_bayonet.json` | `c40a994f9cb54ad9` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/netherite_bayonet.json` | `df8eaee8d97302e1` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/tact_grip.json` | `3263cbf2152a6f58` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/sights.json` | `27cb11fef333465a` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/stan_mag.json` | `c4b6d297e01e3fab` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/ext_mag.json` | `450dc32ae44bea38` |
| solenoid_rifle | `Guns/models/special/gauss_rifle/speed_mag.json` | `b2f0c4c8a8c7b85d` |
| solenoid_rifle | `Guns/sounds/item/gauss/fire.ogg` | `1361b447db689391` |
| solenoid_rifle | `Guns/item/cogloader_stocks.png` | `05014df66161cc85` |
| solenoid_rifle | `Guns/item/cogloader_grips.png` | `3d9f4d6c23da68dd` |
| solenoid_rifle | `Guns/item/makeshift_rifle_bayonets.png` | `dfc93f5032504095` |
| solenoid_rifle | `Guns/item/gauss_rifle_animated.png` | `15c409a7cdb43ae8` |
| solenoid_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| solenoid_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| solenoid_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| solenoid_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| solenoid_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| votive_rifle | `Guns/models/item/plasgun.json` | `a964b6376649e083` |
| votive_rifle | `Guns/item/plasgun.png` | `956bb37c4b331cc0` |
| votive_rifle | `Guns/item/plasgun.animation.json` | `81583df6b14c36e3` |
| votive_rifle | `Guns/models/special/plasgun/main.json` | `7209fb38ea4d9e61` |
| votive_rifle | `Guns/models/special/plasgun/light_stock.json` | `364acdc1dcb692fe` |
| votive_rifle | `Guns/models/special/plasgun/heavy_stock.json` | `5d71023b9fffa627` |
| votive_rifle | `Guns/models/special/plasgun/wooden_stock.json` | `4fe343c79b826c72` |
| votive_rifle | `Guns/models/special/plasgun/light_grip.json` | `790a818404c1685c` |
| votive_rifle | `Guns/models/special/plasgun/iron_bayonet.json` | `ce37cf8b8868b130` |
| votive_rifle | `Guns/models/special/plasgun/anthralite_bayonet.json` | `fa012df8c358d732` |
| votive_rifle | `Guns/models/special/plasgun/diamond_bayonet.json` | `1c89f3de5501f255` |
| votive_rifle | `Guns/models/special/plasgun/netherite_bayonet.json` | `43a2563fccc04d17` |
| votive_rifle | `Guns/models/special/plasgun/tact_grip.json` | `f06dbf59fd6f3eb4` |
| votive_rifle | `Guns/models/special/plasgun/sights.json` | `b29d1f9da73c1c98` |
| votive_rifle | `Guns/models/special/plasgun/stan_mag.json` | `2561195014969f7a` |
| votive_rifle | `Guns/models/special/plasgun/ext_mag.json` | `10dc25d7222d8d47` |
| votive_rifle | `Guns/models/special/plasgun/speed_mag.json` | `a823ca7ad785abd3` |
| votive_rifle | `Guns/sounds/item/nervepinch/fire.ogg` | `ee6701ec0f70a3fc` |
| votive_rifle | `Guns/item/makeshift_rifle_bayonets.png` | `dfc93f5032504095` |
| votive_rifle | `Guns/item/plasgun_animated.png` | `d900da41c28da7a5` |
| votive_rifle | `Guns/item/seal_2.png` | `be02ff1786425224` |
| votive_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| votive_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| votive_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| votive_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| votive_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| glowmouth | `Guns/models/item/plasmabuss.json` | `6b552335e0fd40a1` |
| glowmouth | `Guns/item/plasmabuss.png` | `20af21e557f3ad13` |
| glowmouth | `Guns/item/plasmabuss.animation.json` | `dcf2bbd800298a90` |
| glowmouth | `Guns/models/special/plasmabuss/main.json` | `7b398da45de8edf4` |
| glowmouth | `Guns/sounds/item/plasma/fire_2.ogg` | `ef0c65b09e2ccb1f` |
| mule | `Guns/models/item/super_shotgun.json` | `7f978eed05bab99c` |
| mule | `Guns/item/super_shotgun.png` | `679f1595d8f6ff61` |
| mule | `Guns/item/super_shotgun.animation.json` | `1778b2045baa4997` |
| mule | `Guns/models/special/super_shotgun/main.json` | `3ce941a1af568f73` |
| mule | `Guns/sounds/item/boomstick/fire.ogg` | `17a57973db433feb` |
| fowler | `Guns/models/item/doublet.json` | `7d99e9a7cedc85b3` |
| fowler | `Guns/item/doublet.png` | `07e6674d971b999e` |
| fowler | `Guns/item/doublet.animation.json` | `2737ae5d5a0e6d41` |
| fowler | `Guns/models/special/doublet/main.json` | `f2b13d3341de47db` |
| fowler | `Guns/models/special/doublet/light_grip.json` | `9b57e017c6ec6be9` |
| fowler | `Guns/models/special/doublet/vert_grip.json` | `1c12cccb8864994d` |
| fowler | `Guns/models/special/doublet/iron_bayonet.json` | `bc96735d037135d5` |
| fowler | `Guns/models/special/doublet/anthralite_bayonet.json` | `94b4ab18302ddd6c` |
| fowler | `Guns/models/special/doublet/diamond_bayonet.json` | `600b360323f7f871` |
| fowler | `Guns/models/special/doublet/netherite_bayonet.json` | `280a23d8ab84e284` |
| fowler | `Guns/models/special/doublet/hammer.json` | `9365a5b0b9c90007` |
| fowler | `Guns/sounds/item/blackpowder/fire.ogg` | `a3d3d49a332d034f` |
| fowler | `Guns/item/musket_grips.png` | `7f451a500ba89baa` |
| fowler | `Guns/item/blunderbuss.png` | `4e90eca70ba48802` |
| culverin | `Guns/models/item/handcannon.json` | `c318d0a476bd45e9` |
| culverin | `Guns/item/handcannon.png` | `f1665e75eb347c25` |
| culverin | `Guns/item/handcannon.animation.json` | `7a7de8cc1d0ad440` |
| culverin | `Guns/models/special/handcannon/main.json` | `92eb04b64a051f0e` |
| culverin | `Guns/sounds/item/cannon/fire.ogg` | `c114b809e9f48ac7` |
| undertone_rifle | `Guns/models/item/sculk_resonator.json` | `3ff2ed331d9b94cf` |
| undertone_rifle | `Guns/item/sculk_resonator.png` | `b35a6aa19f5dfd45` |
| undertone_rifle | `Guns/item/sculk_resonator.animation.json` | `7a566d034cd616af` |
| undertone_rifle | `Guns/models/special/sculk_resonator/main.json` | `7a460590a0d01cf2` |
| undertone_rifle | `Guns/models/special/sculk_resonator/light_stock.json` | `1ac9d3a81f761a60` |
| undertone_rifle | `Guns/models/special/sculk_resonator/heavy_stock.json` | `3caa4bedab27b9d0` |
| undertone_rifle | `Guns/models/special/sculk_resonator/wooden_stock.json` | `17403340deb4cb05` |
| undertone_rifle | `Guns/models/special/sculk_resonator/sights.json` | `b175689484aba3e4` |
| undertone_rifle | `Guns/sounds/item/sculk/fire.ogg` | `85de3c45c216ac33` |
| undertone_rifle | `Guns/item/sculk_resonator_animated.png` | `47928bb64fe10405` |
| undertone_rifle | `Guns/item/sculk_resonator_tendril.png` | `f0308d7adf219ff3` |
| undertone_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| undertone_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| undertone_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| undertone_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| undertone_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| murmur_smg | `Guns/models/item/whispers.json` | `7c251587fa6a6125` |
| murmur_smg | `Guns/item/whispers.png` | `14baed2c3daccc19` |
| murmur_smg | `Guns/item/whispers.animation.json` | `1a4e7aebfcdfdc63` |
| murmur_smg | `Guns/models/special/whispers/main.json` | `e851766e1fc12454` |
| murmur_smg | `Guns/models/special/whispers/sights.json` | `c8a9167f4d564c60` |
| murmur_smg | `Guns/models/special/whispers/stan_mag.json` | `a9a77dbdc2ce0c57` |
| murmur_smg | `Guns/models/special/whispers/ext_mag.json` | `a89afb7374464cda` |
| murmur_smg | `Guns/models/special/whispers/speed_mag.json` | `4fd06a95c053fec6` |
| murmur_smg | `Guns/sounds/item/beam/fire.ogg` | `d91dc843b166ee89` |
| murmur_smg | `Guns/item/sculk_resonator_animated.png` | `47928bb64fe10405` |
| murmur_smg | `Guns/item/sculk_resonator_tendril.png` | `f0308d7adf219ff3` |
| murmur_smg | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| murmur_smg | `Guns/item/medium_scope.png` | `543abecf859783be` |
| murmur_smg | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| murmur_smg | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| murmur_smg | `Guns/item/laser.png` | `aac5f06e892e350a` |
| reverb | `Guns/models/item/echoes_2.json` | `e4787c9ac8b6fbb0` |
| reverb | `Guns/item/echoes_2.png` | `2e641f85ab279f05` |
| reverb | `Guns/item/echoes_2.animation.json` | `e5de14b2c8b5892a` |
| reverb | `Guns/models/special/echoes_2/main.json` | `b7d6622f3bd8607f` |
| reverb | `Guns/models/special/echoes_2/light_grip.json` | `850d49db9a890b0f` |
| reverb | `Guns/models/special/echoes_2/tact_grip.json` | `9047010470b9de13` |
| reverb | `Guns/models/special/echoes_2/sights.json` | `4e78164816f69ed2` |
| reverb | `Guns/models/special/echoes_2/barrels.json` | `3eb29916c81c2f11` |
| reverb | `Guns/sounds/item/shulker/fire.ogg` | `6c116009b62adfb6` |
| reverb | `Guns/item/echoes_2_animated.png` | `6676d701518c7e77` |
| reverb | `Guns/item/sculk_resonator_tendril.png` | `f0308d7adf219ff3` |
| reverb | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| reverb | `Guns/item/medium_scope.png` | `543abecf859783be` |
| reverb | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| reverb | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| reverb | `Guns/item/laser.png` | `aac5f06e892e350a` |
| tusker | `Guns/models/item/mangalitsa.json` | `3b718fdba7e37961` |
| tusker | `Guns/item/mangalitsa.png` | `5932c3999fe3a79d` |
| tusker | `Guns/item/mangalitsa.animation.json` | `1653eac0310a6434` |
| tusker | `Guns/models/special/mangalitsa/main.json` | `65ba9a64b06a42d7` |
| tusker | `Guns/models/special/mangalitsa/stan_barrel.json` | `57e7209fd7bfb37a` |
| tusker | `Guns/models/special/mangalitsa/silencer.json` | `caa27d0e191217ee` |
| tusker | `Guns/models/special/mangalitsa/advanced_silencer.json` | `afd21245d95e8bcb` |
| tusker | `Guns/models/special/mangalitsa/muzzle_brake.json` | `38be7dd7430ebe87` |
| tusker | `Guns/models/special/mangalitsa/ext_barrel.json` | `1859adcc0607642b` |
| tusker | `Guns/models/special/mangalitsa/stan_grip.json` | `4c2ded8896ec5c2a` |
| tusker | `Guns/models/special/mangalitsa/light_stock.json` | `3d7965a0b2c8b833` |
| tusker | `Guns/models/special/mangalitsa/heavy_stock.json` | `71ba643205e27425` |
| tusker | `Guns/models/special/mangalitsa/wooden_stock.json` | `c2ab3a822fc5ae67` |
| tusker | `Guns/models/special/mangalitsa/light_grip.json` | `b61ea782821149ec` |
| tusker | `Guns/models/special/mangalitsa/iron_bayonet.json` | `8ea339a612cb3ea0` |
| tusker | `Guns/models/special/mangalitsa/anthralite_bayonet.json` | `eb6d94e8a189df13` |
| tusker | `Guns/models/special/mangalitsa/diamond_bayonet.json` | `9fc1ebe93a8e1fd3` |
| tusker | `Guns/models/special/mangalitsa/netherite_bayonet.json` | `d00e68b8c79f8701` |
| tusker | `Guns/models/special/mangalitsa/tact_grip.json` | `8a64cfe5bac1d6aa` |
| tusker | `Guns/models/special/mangalitsa/sights.json` | `8c6b70c2df8e6aa1` |
| tusker | `Guns/models/special/mangalitsa/bolt.json` | `60747d73758f24c8` |
| tusker | `Guns/sounds/item/bruiser/fire.ogg` | `b965c68e659f88e3` |
| tusker | `Guns/item/medium_scope.png` | `543abecf859783be` |
| tusker | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| tusker | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| tusker | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| tusker | `Guns/item/laser.png` | `aac5f06e892e350a` |
| cinder_repeater | `Guns/models/item/vulcanic_repeater.json` | `b9125e1f6eea07a9` |
| cinder_repeater | `Guns/item/vulcanic_repeater.png` | `ac06e87f6e0ce67c` |
| cinder_repeater | `Guns/item/vulcanic_repeater.animation.json` | `f87e8ccf739df2a2` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/main.json` | `e58651fbad5e20fe` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/stan_barrel.json` | `18f9892432d8f92e` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/silencer.json` | `84047c1e5e38ffae` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/advanced_silencer.json` | `d682900ab6c8e6c7` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/muzzle_brake.json` | `6add479bcc525d07` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/ext_barrel.json` | `94e765ba67ab2029` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/stan_grip.json` | `f4e61a24a73a5211` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/light_stock.json` | `19f8134bc0fbde5b` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/heavy_stock.json` | `46f96c59da0f802e` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/wooden_stock.json` | `22abcb5a5d501c50` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/light_grip.json` | `23b8d9d3042bc7c4` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/iron_bayonet.json` | `de5b918afcf17a6a` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/anthralite_bayonet.json` | `7b04be3fed98386f` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/diamond_bayonet.json` | `b641d0f4424750e2` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/netherite_bayonet.json` | `5d077897c3f61474` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/tact_grip.json` | `2cde178cdc0047b1` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/sights.json` | `d6dadad0b39d780e` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/stan_mag.json` | `d32493eaa5e4dbf5` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/ext_mag.json` | `6c41d1a0a772826e` |
| cinder_repeater | `Guns/models/special/vulcanic_repeater/speed_mag.json` | `c6787f30e64125dd` |
| cinder_repeater | `Guns/sounds/item/rusty_gnat/enchanted_fire.ogg` | `98bda9d70ff73b2e` |
| cinder_repeater | `Guns/item/advanced_silencer.png` | `f94615d5c4cdd84c` |
| cinder_repeater | `Guns/item/greaser_smg_barrels.png` | `e912ec7c95188909` |
| cinder_repeater | `Guns/item/makeshift_rifle_bayonets.png` | `dfc93f5032504095` |
| cinder_repeater | `Guns/item/pyroclastic_flow_animated.png` | `e545c8e219a385dd` |
| cinder_repeater | `Guns/item/medium_scope.png` | `543abecf859783be` |
| cinder_repeater | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| cinder_repeater | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| cinder_repeater | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| cinder_repeater | `Guns/item/laser.png` | `aac5f06e892e350a` |
| bastion_rifle | `Guns/models/item/trotters.json` | `09b5baef810a83a2` |
| bastion_rifle | `Guns/item/trotters.png` | `d7b58c369c085e79` |
| bastion_rifle | `Guns/item/trotters.animation.json` | `eedb968a76105c92` |
| bastion_rifle | `Guns/models/special/trotters/main.json` | `c70596031446de86` |
| bastion_rifle | `Guns/models/special/trotters/stan_barrel.json` | `5818e254d131f360` |
| bastion_rifle | `Guns/models/special/trotters/silencer.json` | `222377ec474b548e` |
| bastion_rifle | `Guns/models/special/trotters/advanced_silencer.json` | `ab2a54d3e36e33cb` |
| bastion_rifle | `Guns/models/special/trotters/muzzle_brake.json` | `fcbf95b9aecc7b6e` |
| bastion_rifle | `Guns/models/special/trotters/ext_barrel.json` | `6f8d5284a1533349` |
| bastion_rifle | `Guns/models/special/trotters/stan_grip.json` | `dd6c9d1d23db8aaf` |
| bastion_rifle | `Guns/models/special/trotters/light_stock.json` | `9ba9b56ce2d4c751` |
| bastion_rifle | `Guns/models/special/trotters/heavy_stock.json` | `746dc046b8479f6e` |
| bastion_rifle | `Guns/models/special/trotters/wooden_stock.json` | `5b61788e4991065c` |
| bastion_rifle | `Guns/models/special/trotters/light_grip.json` | `83ed5958e4069dbf` |
| bastion_rifle | `Guns/models/special/trotters/iron_bayonet.json` | `cff3a98dab456107` |
| bastion_rifle | `Guns/models/special/trotters/anthralite_bayonet.json` | `ff6a7201b70e469c` |
| bastion_rifle | `Guns/models/special/trotters/diamond_bayonet.json` | `134b49bee0bc39de` |
| bastion_rifle | `Guns/models/special/trotters/netherite_bayonet.json` | `30660f0094b998f2` |
| bastion_rifle | `Guns/models/special/trotters/tact_grip.json` | `234bf250031eab84` |
| bastion_rifle | `Guns/models/special/trotters/sights.json` | `43e0d8daaffc14e5` |
| bastion_rifle | `Guns/models/special/trotters/bolt.json` | `33eecd726a57a504` |
| bastion_rifle | `Guns/sounds/item/heavier_rifle/fire.ogg` | `4e17f5a1b2891ee7` |
| bastion_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| bastion_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| bastion_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| bastion_rifle | `Guns/item/turnpike.png` | `63e9b3911c1b9cd4` |
| bastion_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| bastion_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| ashfall_pistol | `Guns/models/item/pyroclastic_flow.json` | `0a44299b25d91ca9` |
| ashfall_pistol | `Guns/item/pyroclastic_flow.png` | `cad666fe4ff7b438` |
| ashfall_pistol | `Guns/item/pyroclastic_flow.animation.json` | `2154c87cd09ab045` |
| ashfall_pistol | `Guns/models/special/pyroclastic_flow/main.json` | `d507577566cc3426` |
| ashfall_pistol | `Guns/models/special/pyroclastic_flow/light_stock.json` | `6edc7c6d7e8d9f14` |
| ashfall_pistol | `Guns/models/special/pyroclastic_flow/heavy_stock.json` | `c96eff8261605db9` |
| ashfall_pistol | `Guns/models/special/pyroclastic_flow/wooden_stock.json` | `abbd590274156868` |
| ashfall_pistol | `Guns/models/special/pyroclastic_flow/sights.json` | `eb9f25722efdc7c7` |
| ashfall_pistol | `Guns/sounds/item/scrapper/fire.ogg` | `43c9d4ec929bf949` |
| ashfall_pistol | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| ashfall_pistol | `Guns/item/medium_scope.png` | `543abecf859783be` |
| ashfall_pistol | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| ashfall_pistol | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| ashfall_pistol | `Guns/item/laser.png` | `aac5f06e892e350a` |
| goldbristle_carbine | `Guns/models/item/freyr.json` | `4ede6f40b897e035` |
| goldbristle_carbine | `Guns/item/freyr.png` | `8e782f6171e1932a` |
| goldbristle_carbine | `Guns/item/freyr.animation.json` | `246889ab5525ade2` |
| goldbristle_carbine | `Guns/models/special/freyr/main.json` | `209a685fda57aa81` |
| goldbristle_carbine | `Guns/models/special/freyr/stan_barrel.json` | `da6aea2611ef154f` |
| goldbristle_carbine | `Guns/models/special/freyr/silencer.json` | `a182c8c5d807a94d` |
| goldbristle_carbine | `Guns/models/special/freyr/advanced_silencer.json` | `8193f1d0db820e32` |
| goldbristle_carbine | `Guns/models/special/freyr/muzzle_brake.json` | `0ef1ca8af2c81db8` |
| goldbristle_carbine | `Guns/models/special/freyr/ext_barrel.json` | `564a329496a675ca` |
| goldbristle_carbine | `Guns/models/special/freyr/stan_grip.json` | `f3757d0a698ec8a3` |
| goldbristle_carbine | `Guns/models/special/freyr/light_stock.json` | `f02b0774d36ebb92` |
| goldbristle_carbine | `Guns/models/special/freyr/heavy_stock.json` | `26ef78e0ad4baef9` |
| goldbristle_carbine | `Guns/models/special/freyr/wooden_stock.json` | `9e511841276a85bd` |
| goldbristle_carbine | `Guns/models/special/freyr/light_grip.json` | `c60a5c775d0a12bf` |
| goldbristle_carbine | `Guns/models/special/freyr/iron_bayonet.json` | `2adfc1bb31888311` |
| goldbristle_carbine | `Guns/models/special/freyr/anthralite_bayonet.json` | `2616d6c2855560d9` |
| goldbristle_carbine | `Guns/models/special/freyr/diamond_bayonet.json` | `42a4d915b7d016f1` |
| goldbristle_carbine | `Guns/models/special/freyr/netherite_bayonet.json` | `b3e5d122bc3b296e` |
| goldbristle_carbine | `Guns/models/special/freyr/tact_grip.json` | `01ed9f55f20cc7fe` |
| goldbristle_carbine | `Guns/models/special/freyr/sights.json` | `138c293da67d9e69` |
| goldbristle_carbine | `Guns/models/special/freyr/stan_mag.json` | `d0d3c69e9f30449f` |
| goldbristle_carbine | `Guns/models/special/freyr/ext_mag.json` | `e738571809ab0cb3` |
| goldbristle_carbine | `Guns/models/special/freyr/speed_mag.json` | `1eaa64761a4be46a` |
| goldbristle_carbine | `Guns/sounds/item/makeshift_rifle/fire.ogg` | `9c4469e45b9b77e4` |
| goldbristle_carbine | `Guns/item/pyroclastic_flow_animated.png` | `e545c8e219a385dd` |
| goldbristle_carbine | `Guns/item/medium_scope.png` | `543abecf859783be` |
| goldbristle_carbine | `Guns/item/jackhammer.png` | `2198cf8cfda3a00b` |
| goldbristle_carbine | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| goldbristle_carbine | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| goldbristle_carbine | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| goldbristle_carbine | `Guns/item/laser.png` | `aac5f06e892e350a` |
| crackling | `Guns/models/item/ribs_glory.json` | `09b5baef810a83a2` |
| crackling | `Guns/item/ribs_glory.png` | `80cc857ba4bed14e` |
| crackling | `Guns/item/ribs_glory.animation.json` | `6b171cd061b4cf10` |
| crackling | `Guns/models/special/ribs_glory/main.json` | `21fee5276d1f7f4a` |
| crackling | `Guns/models/special/ribs_glory/light_stock.json` | `5367eec501f05546` |
| crackling | `Guns/models/special/ribs_glory/heavy_stock.json` | `3e553ba3e75a264a` |
| crackling | `Guns/models/special/ribs_glory/wooden_stock.json` | `d7878e4a562097dd` |
| crackling | `Guns/models/special/ribs_glory/light_grip.json` | `00657cfdd58440cc` |
| crackling | `Guns/models/special/ribs_glory/iron_bayonet.json` | `f76862e3a29aa4ee` |
| crackling | `Guns/models/special/ribs_glory/anthralite_bayonet.json` | `934ca6d1a899ab35` |
| crackling | `Guns/models/special/ribs_glory/diamond_bayonet.json` | `90e057fb4b4f6278` |
| crackling | `Guns/models/special/ribs_glory/netherite_bayonet.json` | `547e931af27cf393` |
| crackling | `Guns/models/special/ribs_glory/tact_grip.json` | `e116a28a23644cef` |
| crackling | `Guns/models/special/ribs_glory/bolt.json` | `650fcf80f52af042` |
| crackling | `Guns/sounds/item/brass_shotgun/fire.ogg` | `95eee6d27b87d35a` |
| chitin_smg | `Guns/models/item/carapice.json` | `9c7fd2500de2eec0` |
| chitin_smg | `Guns/item/carapice.png` | `dbb4252f2a894972` |
| chitin_smg | `Guns/item/carapice.animation.json` | `51cfe99c8591815e` |
| chitin_smg | `Guns/models/special/carapice/main.json` | `4c8827ca7b72b9ce` |
| chitin_smg | `Guns/models/special/carapice/sights.json` | `05ae7722c245bee9` |
| chitin_smg | `Guns/models/special/carapice/no_sights.json` | `d00a8489b4a21b5b` |
| chitin_smg | `Guns/models/special/carapice/receiver.json` | `2b23d62be150547f` |
| chitin_smg | `Guns/sounds/item/greaser_smg/fire.ogg` | `201fd4258dea84a2` |
| chitin_smg | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| chitin_smg | `Guns/item/medium_scope.png` | `543abecf859783be` |
| chitin_smg | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| chitin_smg | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| chitin_smg | `Guns/item/laser.png` | `aac5f06e892e350a` |
| hermit_rifle | `Guns/models/item/shellurker.json` | `2706d7f49c7aa19e` |
| hermit_rifle | `Guns/item/shellurker.png` | `d3f7c77e46cdc7b0` |
| hermit_rifle | `Guns/item/shellurker.animation.json` | `d6ede559f8613f4f` |
| hermit_rifle | `Guns/models/special/shellurker/main.json` | `bc593446eb3b40de` |
| hermit_rifle | `Guns/models/special/shellurker/light_stock.json` | `e0f565b913ead028` |
| hermit_rifle | `Guns/models/special/shellurker/heavy_stock.json` | `b531eaab81b6035d` |
| hermit_rifle | `Guns/models/special/shellurker/wooden_stock.json` | `e8c7d791f589adfc` |
| hermit_rifle | `Guns/models/special/shellurker/sights.json` | `b0f71acd3eb9d1c3` |
| hermit_rifle | `Guns/models/special/shellurker/no_sights.json` | `21a1d0e8ee891834` |
| hermit_rifle | `Guns/models/special/shellurker/receiver.json` | `e0d4428d618163a8` |
| hermit_rifle | `Guns/sounds/item/shulker/fire.ogg` | `6c116009b62adfb6` |
| hermit_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| hermit_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| hermit_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| hermit_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| hermit_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| borer | `Guns/models/item/weevil.json` | `99dd246512007be7` |
| borer | `Guns/item/weevil.png` | `cb35813e29e094d1` |
| borer | `Guns/item/weevil.animation.json` | `34f343b3ee93ee6c` |
| borer | `Guns/models/special/weevil/main.json` | `33630abf4a584e59` |
| borer | `Guns/models/special/weevil/barrel.json` | `c10feb4040b6441a` |
| borer | `Guns/sounds/item/scorched_rifle/fire.ogg` | `5a52d37065a09a8c` |
| penny_pistol | `Guns/models/item/scrapper.json` | `21e152cbd79e9c8b` |
| penny_pistol | `Guns/item/scrapper.png` | `4b201474a9f76fac` |
| penny_pistol | `Guns/item/scrapper.animation.json` | `2a445942c866c32f` |
| penny_pistol | `Guns/models/special/scrapper/main.json` | `a9e665b9a24327d5` |
| penny_pistol | `Guns/models/special/scrapper/stan_barrel.json` | `a7ac5be3bd9db225` |
| penny_pistol | `Guns/models/special/scrapper/silencer.json` | `2573c1ffaf457b35` |
| penny_pistol | `Guns/models/special/scrapper/advanced_silencer.json` | `886cf44af0d75cd6` |
| penny_pistol | `Guns/models/special/scrapper/muzzle_brake.json` | `0e54f551bcd37faf` |
| penny_pistol | `Guns/models/special/scrapper/ext_barrel.json` | `95b4ffcb3c28a307` |
| penny_pistol | `Guns/models/special/scrapper/stock_light.json` | `928176a958050cbb` |
| penny_pistol | `Guns/models/special/scrapper/stock_weighted.json` | `8fd96de36e3892de` |
| penny_pistol | `Guns/models/special/scrapper/stock_wooden.json` | `ef9cc93675dbc076` |
| penny_pistol | `Guns/models/special/scrapper/bolt.json` | `af9d4beda116e8e3` |
| penny_pistol | `Guns/models/special/scrapper/magazine.json` | `f55f5b3cfddd85e9` |
| penny_pistol | `Guns/sounds/item/scrapper/fire.ogg` | `43c9d4ec929bf949` |
| conductor_carbine | `Guns/models/item/llr_director.json` | `09798a72127536ec` |
| conductor_carbine | `Guns/item/llr_director.png` | `8a2f74fa11c2f83c` |
| conductor_carbine | `Guns/item/llr_director.animation.json` | `1dc09471a798d468` |
| conductor_carbine | `Guns/models/special/llr_director/main.json` | `49c5ee4df4a29c1b` |
| conductor_carbine | `Guns/models/special/llr_director/stan_barrel.json` | `eb8b5efc67bb36b7` |
| conductor_carbine | `Guns/models/special/llr_director/silencer.json` | `09d76df8c82a5648` |
| conductor_carbine | `Guns/models/special/llr_director/advanced_silencer.json` | `da08405f175ab121` |
| conductor_carbine | `Guns/models/special/llr_director/muzzle_brake.json` | `f99b17974871a561` |
| conductor_carbine | `Guns/models/special/llr_director/ext_barrel.json` | `46a16469071e9355` |
| conductor_carbine | `Guns/models/special/llr_director/stan_grip.json` | `a750638fe7ebf39b` |
| conductor_carbine | `Guns/models/special/llr_director/light_stock.json` | `cb3c92a98da87f0c` |
| conductor_carbine | `Guns/models/special/llr_director/heavy_stock.json` | `0047edd3962254fb` |
| conductor_carbine | `Guns/models/special/llr_director/wooden_stock.json` | `09857fee6970de53` |
| conductor_carbine | `Guns/models/special/llr_director/light_grip.json` | `14e8b0239090a07f` |
| conductor_carbine | `Guns/models/special/llr_director/iron_bayonet.json` | `0c9c50f6b4316fa1` |
| conductor_carbine | `Guns/models/special/llr_director/anthralite_bayonet.json` | `7b1bd727128cd6f3` |
| conductor_carbine | `Guns/models/special/llr_director/diamond_bayonet.json` | `934f55ecc996141d` |
| conductor_carbine | `Guns/models/special/llr_director/netherite_bayonet.json` | `9320d8d34d81562d` |
| conductor_carbine | `Guns/models/special/llr_director/tact_grip.json` | `5201427684b90f89` |
| conductor_carbine | `Guns/models/special/llr_director/sights.json` | `607fccf24d378b43` |
| conductor_carbine | `Guns/models/special/llr_director/bolt.json` | `9f0d9706556a4047` |
| conductor_carbine | `Guns/models/special/llr_director/stan_mag.json` | `18f2d9d5e8877805` |
| conductor_carbine | `Guns/models/special/llr_director/ext_mag.json` | `32f38e3c90a45996` |
| conductor_carbine | `Guns/models/special/llr_director/speed_mag.json` | `a7013fb565b9e5c7` |
| conductor_carbine | `Guns/sounds/item/cowboy/fire.ogg` | `27ab9051ec48ba94` |
| conductor_carbine | `Guns/item/medium_scope.png` | `543abecf859783be` |
| conductor_carbine | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| conductor_carbine | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| conductor_carbine | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| conductor_carbine | `Guns/item/laser.png` | `aac5f06e892e350a` |
| magpie_smg | `Guns/models/item/birdfeeder.json` | `8429cd08e20c88a6` |
| magpie_smg | `Guns/item/birdfeeder.png` | `3cc8b6b118573d76` |
| magpie_smg | `Guns/item/birdfeeder.animation.json` | `8a92217edeb8d38f` |
| magpie_smg | `Guns/models/special/birdfeeder/main.json` | `6b943e8f79267f7d` |
| magpie_smg | `Guns/models/special/birdfeeder/stan_barrel.json` | `a88be4a66163bb69` |
| magpie_smg | `Guns/models/special/birdfeeder/silencer.json` | `30087e7395b41398` |
| magpie_smg | `Guns/models/special/birdfeeder/advanced_silencer.json` | `9de85a52c23c14bd` |
| magpie_smg | `Guns/models/special/birdfeeder/muzzle_brake.json` | `3b53d4f60956b9ba` |
| magpie_smg | `Guns/models/special/birdfeeder/ext_barrel.json` | `02b0663c089438de` |
| magpie_smg | `Guns/models/special/birdfeeder/stan_grip.json` | `dabd49c2bf747140` |
| magpie_smg | `Guns/models/special/birdfeeder/light_stock.json` | `86c316af237e6767` |
| magpie_smg | `Guns/models/special/birdfeeder/heavy_stock.json` | `c2dfd8977c08195c` |
| magpie_smg | `Guns/models/special/birdfeeder/wooden_stock.json` | `50a9fc933952076c` |
| magpie_smg | `Guns/models/special/birdfeeder/light_grip.json` | `4523908e739390b2` |
| magpie_smg | `Guns/models/special/birdfeeder/iron_bayonet.json` | `6a4885d3bd01c51a` |
| magpie_smg | `Guns/models/special/birdfeeder/anthralite_bayonet.json` | `b71dae536862507e` |
| magpie_smg | `Guns/models/special/birdfeeder/diamond_bayonet.json` | `f6c5e0a1647b8980` |
| magpie_smg | `Guns/models/special/birdfeeder/netherite_bayonet.json` | `ae417f32ed193f9b` |
| magpie_smg | `Guns/models/special/birdfeeder/tact_grip.json` | `a61801acb7b1c981` |
| magpie_smg | `Guns/models/special/birdfeeder/sights.json` | `272376847534fb3b` |
| magpie_smg | `Guns/models/special/birdfeeder/stan_mag.json` | `fd939e0dfe7633d4` |
| magpie_smg | `Guns/sounds/item/rusty_gnat/fire.ogg` | `02c2854e36a0f377` |
| magpie_smg | `Guns/item/medium_scope.png` | `543abecf859783be` |
| magpie_smg | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| magpie_smg | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| magpie_smg | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| magpie_smg | `Guns/item/laser.png` | `aac5f06e892e350a` |
| tuppence_pistol | `Guns/models/item/umax_pistol.json` | `9b9c754f3cf31fdd` |
| tuppence_pistol | `Guns/item/umax_pistol.png` | `d8e0c22865b815e1` |
| tuppence_pistol | `Guns/item/umax_pistol.animation.json` | `15f4b095663a3308` |
| tuppence_pistol | `Guns/models/special/umax_pistol/main.json` | `122aca6c82019740` |
| tuppence_pistol | `Guns/models/special/umax_pistol/stan_grip.json` | `5e0d57e2614fc281` |
| tuppence_pistol | `Guns/models/special/umax_pistol/light_stock.json` | `baa4ed15ee654792` |
| tuppence_pistol | `Guns/models/special/umax_pistol/heavy_stock.json` | `174aa819a0e63619` |
| tuppence_pistol | `Guns/models/special/umax_pistol/wooden_stock.json` | `0bc4ab27612e8975` |
| tuppence_pistol | `Guns/models/special/umax_pistol/sights.json` | `c96c4dce7d304682` |
| tuppence_pistol | `Guns/sounds/item/brass_pistol/fire.ogg` | `4fca376e2ece67b2` |
| tuppence_pistol | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| tuppence_pistol | `Guns/item/medium_scope.png` | `543abecf859783be` |
| tuppence_pistol | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| tuppence_pistol | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| tuppence_pistol | `Guns/item/laser.png` | `aac5f06e892e350a` |
| goshawk_revolver | `Guns/models/item/ultra_knight_hawk.json` | `93458ed13b5680ca` |
| goshawk_revolver | `Guns/item/ultra_knight_hawk.png` | `b6cab422c02babfc` |
| goshawk_revolver | `Guns/item/ultra_knight_hawk.animation.json` | `c371c22506a0c358` |
| goshawk_revolver | `Guns/models/special/ultra_knight_hawk/main.json` | `9c9aad7e25c381a5` |
| goshawk_revolver | `Guns/models/special/ultra_knight_hawk/drum.json` | `a81080ecf6dc7bc1` |
| goshawk_revolver | `Guns/models/special/ultra_knight_hawk/hammer.json` | `58774cfb703f962c` |
| goshawk_revolver | `Guns/sounds/item/heavier_rifle/fire.ogg` | `4e17f5a1b2891ee7` |
| lament_rifle | `Guns/models/item/forlorn_hope.json` | `4920c31eb3990723` |
| lament_rifle | `Guns/item/forlorn_hope.png` | `6a8de09c3a97ead9` |
| lament_rifle | `Guns/item/forlorn_hope.animation.json` | `3d17e3c23fa8c9c0` |
| lament_rifle | `Guns/models/special/forlorn_hope/main.json` | `d924d7d8f6cae1c4` |
| lament_rifle | `Guns/models/special/forlorn_hope/light_stock.json` | `155015336966502e` |
| lament_rifle | `Guns/models/special/forlorn_hope/heavy_stock.json` | `59ff86b904ed88a2` |
| lament_rifle | `Guns/models/special/forlorn_hope/wooden_stock.json` | `600df85a131f0a4e` |
| lament_rifle | `Guns/models/special/forlorn_hope/light_grip.json` | `2cbd3296de0c8d96` |
| lament_rifle | `Guns/models/special/forlorn_hope/vert_grip.json` | `a69d15791a05afd5` |
| lament_rifle | `Guns/models/special/forlorn_hope/iron_bayonet.json` | `74ad1a806ce9d247` |
| lament_rifle | `Guns/models/special/forlorn_hope/anthralite_bayonet.json` | `1be3a36ebd5efa8e` |
| lament_rifle | `Guns/models/special/forlorn_hope/diamond_bayonet.json` | `53b9c58bfc89cb0b` |
| lament_rifle | `Guns/models/special/forlorn_hope/netherite_bayonet.json` | `f0178f1629a90903` |
| lament_rifle | `Guns/models/special/forlorn_hope/sights.json` | `51ace3a81d2dd533` |
| lament_rifle | `Guns/sounds/item/scorched_sniper/fire.ogg` | `624b2460a3e39921` |
| lament_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| lament_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| lament_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| lament_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| lament_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| tabby_revolver | `Guns/models/item/micina.json` | `15a8d9614307f216` |
| tabby_revolver | `Guns/item/micina.png` | `c6d71af39617c4f7` |
| tabby_revolver | `Guns/item/micina.animation.json` | `932aa4c715d612d6` |
| tabby_revolver | `Guns/models/special/micina/main.json` | `9b424faeaa1cd635` |
| tabby_revolver | `Guns/models/special/micina/stan_barrel.json` | `a418c0135eff4acd` |
| tabby_revolver | `Guns/models/special/micina/silencer.json` | `fbcad38b4e75586b` |
| tabby_revolver | `Guns/models/special/micina/advanced_silencer.json` | `80689a482a56cc1f` |
| tabby_revolver | `Guns/models/special/micina/muzzle_brake.json` | `2995268809fafc70` |
| tabby_revolver | `Guns/models/special/micina/ext_barrel.json` | `dc67c7c3cc250be1` |
| tabby_revolver | `Guns/models/special/micina/drum.json` | `0f0ff66dcb3434f5` |
| tabby_revolver | `Guns/models/special/micina/hammer.json` | `828238e794952f3a` |
| tabby_revolver | `Guns/sounds/item/brass_revolver/fire.ogg` | `9ede81f3520dfd5c` |
| lookout_rifle | `Guns/models/item/red_raydar.json` | `d5043024891f5def` |
| lookout_rifle | `Guns/item/red_raydar.png` | `f717867517542253` |
| lookout_rifle | `Guns/item/red_raydar.animation.json` | `acea19226e70fa7c` |
| lookout_rifle | `Guns/models/special/red_raydar/main.json` | `ec18a73c2cde2610` |
| lookout_rifle | `Guns/models/special/red_raydar/light_stock.json` | `ffc72c01b2a0488b` |
| lookout_rifle | `Guns/models/special/red_raydar/heavy_stock.json` | `acd98b0582c14627` |
| lookout_rifle | `Guns/models/special/red_raydar/wooden_stock.json` | `66e3aa715589f318` |
| lookout_rifle | `Guns/models/special/red_raydar/light_grip.json` | `61211a5101ac7f2f` |
| lookout_rifle | `Guns/models/special/red_raydar/iron_bayonet.json` | `0983c6383dfd1bcb` |
| lookout_rifle | `Guns/models/special/red_raydar/anthralite_bayonet.json` | `42c256791d7fc830` |
| lookout_rifle | `Guns/models/special/red_raydar/diamond_bayonet.json` | `ad0cb4a036864c71` |
| lookout_rifle | `Guns/models/special/red_raydar/netherite_bayonet.json` | `24cc60e07759e79e` |
| lookout_rifle | `Guns/models/special/red_raydar/tact_grip.json` | `8a15ac84ca898daf` |
| lookout_rifle | `Guns/models/special/red_raydar/sights.json` | `aa6035a4b8541d73` |
| lookout_rifle | `Guns/sounds/item/heavy_rifle/fire.ogg` | `389a087108dc1565` |
| lookout_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| lookout_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| lookout_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| lookout_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| lookout_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| ammonite_carbine | `Guns/models/item/spirulida.json` | `24dd5fda329a4c8f` |
| ammonite_carbine | `Guns/item/spirulida.png` | `534455aeb4f99102` |
| ammonite_carbine | `Guns/item/spirulida.animation.json` | `8807bbe25f371839` |
| ammonite_carbine | `Guns/models/special/spirulida/main.json` | `02e70c50c0b3b8e2` |
| ammonite_carbine | `Guns/models/special/spirulida/sights.json` | `2152ddc0b46a1aad` |
| ammonite_carbine | `Guns/models/special/spirulida/light_stock.json` | `a8b80ed540c65d9b` |
| ammonite_carbine | `Guns/models/special/spirulida/heavy_stock.json` | `2926ebb2256f5eb4` |
| ammonite_carbine | `Guns/models/special/spirulida/wooden_stock.json` | `28ded3465978fe73` |
| ammonite_carbine | `Guns/sounds/item/old_rifle/fire.ogg` | `d684ae9a09c9c12c` |
| fencepost_carbine | `Guns/models/item/fencer_carabine.json` | `ffa260b1360b4b32` |
| fencepost_carbine | `Guns/item/fencer_carabine.png` | `5c35a35f1e4ea61c` |
| fencepost_carbine | `Guns/item/fencer_carabine.animation.json` | `042163727e5f741e` |
| fencepost_carbine | `Guns/models/special/fencer_carabine/main.json` | `9ca1ed42452b5a47` |
| fencepost_carbine | `Guns/models/special/fencer_carabine/light_grip.json` | `d9dc1a3bc6263bf6` |
| fencepost_carbine | `Guns/models/special/fencer_carabine/iron_bayonet.json` | `9c6808e8d21117d9` |
| fencepost_carbine | `Guns/models/special/fencer_carabine/anthralite_bayonet.json` | `119ecb9ebc26fea4` |
| fencepost_carbine | `Guns/models/special/fencer_carabine/diamond_bayonet.json` | `4554b531058af7d8` |
| fencepost_carbine | `Guns/models/special/fencer_carabine/netherite_bayonet.json` | `e0f1a82b403ac930` |
| fencepost_carbine | `Guns/models/special/fencer_carabine/tact_grip.json` | `6081c7c952e7f207` |
| fencepost_carbine | `Guns/models/special/fencer_carabine/hammer.json` | `b810d36b3242b1b0` |
| fencepost_carbine | `Guns/sounds/item/iron_rifle/fire.ogg` | `e4a92914502ffd1c` |
| waystation_shotgun | `Guns/models/item/callwell_terminal.json` | `fe8ac73f362dc54f` |
| waystation_shotgun | `Guns/item/callwell_terminal.png` | `4183d551f83dffd0` |
| waystation_shotgun | `Guns/item/callwell_terminal.animation.json` | `8ffa05490858e199` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/main.json` | `899a29c536b3fa71` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/stan_barrel.json` | `3de7ee67547b18f6` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/silencer.json` | `fb00d6e9a758c4ff` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/advanced_silencer.json` | `c11554dde63ea113` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/muzzle_brake.json` | `ad334c219da7ca55` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/ext_barrel.json` | `fef56cd59efd427d` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/light_grip.json` | `134e942f35933f9a` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/iron_bayonet.json` | `51ec0e6c2de55208` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/anthralite_bayonet.json` | `dca846b29a362d4f` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/diamond_bayonet.json` | `6b09f5894c24ca36` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/netherite_bayonet.json` | `a3b2f6855e7a8deb` |
| waystation_shotgun | `Guns/models/special/callwell_terminal/tact_grip.json` | `990034371ab282e4` |
| waystation_shotgun | `Guns/sounds/item/brass_shotgun/fire.ogg` | `95eee6d27b87d35a` |
| highball_rifle | `Guns/models/item/saketini.json` | `d83b819c2c9a4036` |
| highball_rifle | `Guns/item/saketini.png` | `048f70e9a97fe923` |
| highball_rifle | `Guns/item/saketini.animation.json` | `47948b84854488de` |
| highball_rifle | `Guns/models/special/saketini/main.json` | `2cccc9a95567c8e0` |
| highball_rifle | `Guns/models/special/saketini/light_grip.json` | `33f72bf1821cfc71` |
| highball_rifle | `Guns/models/special/saketini/iron_bayonet.json` | `2f99ea34b6301f65` |
| highball_rifle | `Guns/models/special/saketini/anthralite_bayonet.json` | `450aa9780315e0d2` |
| highball_rifle | `Guns/models/special/saketini/diamond_bayonet.json` | `b0f8eac636b6a649` |
| highball_rifle | `Guns/models/special/saketini/netherite_bayonet.json` | `29b7f03f0b298cae` |
| highball_rifle | `Guns/models/special/saketini/tact_grip.json` | `9672af6808382d42` |
| highball_rifle | `Guns/models/special/saketini/sights.json` | `1dcb5b05a4e28785` |
| highball_rifle | `Guns/models/special/saketini/stan_barrel.json` | `d06cd42bc3ddcec6` |
| highball_rifle | `Guns/models/special/saketini/silencer.json` | `18824fdb27c492f4` |
| highball_rifle | `Guns/models/special/saketini/advanced_silencer.json` | `952e8a8a20666ec3` |
| highball_rifle | `Guns/models/special/saketini/muzzle_brake.json` | `a1fcf19059083d2c` |
| highball_rifle | `Guns/models/special/saketini/ext_barrel.json` | `6612cf1ca4061713` |
| highball_rifle | `Guns/models/special/saketini/hammer.json` | `8a0491f25aa8f35d` |
| highball_rifle | `Guns/sounds/item/new_rifle/fire.ogg` | `267e176ad9bae07a` |
| highball_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| highball_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| highball_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| highball_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| highball_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| flatboat_gun | `Guns/models/item/big_bore.json` | `7546eba8f6df2b04` |
| flatboat_gun | `Guns/item/punt_gun.png` | `abb919cf7c153739` |
| flatboat_gun | `Guns/item/big_bore.animation.json` | `9456c0bbdc1a0e99` |
| flatboat_gun | `Guns/models/special/big_bore/main.json` | `48466951bbba8066` |
| flatboat_gun | `Guns/models/special/big_bore/sights.json` | `865b4d1913707ffa` |
| flatboat_gun | `Guns/models/special/big_bore/no_sights.json` | `4fafa6ef1b60d03f` |
| flatboat_gun | `Guns/models/special/big_bore/hammer.json` | `6bbca2cbdad3718b` |
| flatboat_gun | `Guns/sounds/item/cannon/fire.ogg` | `c114b809e9f48ac7` |
| flatboat_gun | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| flatboat_gun | `Guns/item/medium_scope.png` | `543abecf859783be` |
| flatboat_gun | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| flatboat_gun | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| flatboat_gun | `Guns/item/laser.png` | `aac5f06e892e350a` |
| windlass_musket | `Guns/models/item/repeating_musket.json` | `cb2907bc7653c358` |
| windlass_musket | `Guns/item/repeating_musket.png` | `896f9d6945847057` |
| windlass_musket | `Guns/item/repeating_musket.animation.json` | `f00cfaeee6da6a9e` |
| windlass_musket | `Guns/models/special/repeating_musket/main.json` | `4a63a97a8931aaf3` |
| windlass_musket | `Guns/models/special/repeating_musket/grip_light.json` | `cbcec1846bfe5165` |
| windlass_musket | `Guns/models/special/repeating_musket/grip_vert.json` | `41d59e39dda99b4d` |
| windlass_musket | `Guns/models/special/repeating_musket/iron_bayonet.json` | `9ccd44ae2a94e81b` |
| windlass_musket | `Guns/models/special/repeating_musket/anthralite_bayonet.json` | `7d12beb71578b1dd` |
| windlass_musket | `Guns/models/special/repeating_musket/diamond_bayonet.json` | `eae89e0edffc52fb` |
| windlass_musket | `Guns/models/special/repeating_musket/netherite_bayonet.json` | `930e0a15e547243f` |
| windlass_musket | `Guns/models/special/repeating_musket/magazine.json` | `732ae9835ff6ed52` |
| windlass_musket | `Guns/models/special/repeating_musket/hammer.json` | `bf5507bbcc5bd23e` |
| windlass_musket | `Guns/sounds/item/blackpowder/fire.ogg` | `a3d3d49a332d034f` |
| windlass_musket | `Guns/item/musket_bayonets.png` | `0830d15655ef543b` |
| windlass_musket | `Guns/item/musket_grips.png` | `7f451a500ba89baa` |
| millstone_rifle | `Guns/models/item/winnie_millend.json` | `3a5daaa5d34cca2e` |
| millstone_rifle | `Guns/item/winnie_millend.png` | `eb44fba86f42e85b` |
| millstone_rifle | `Guns/item/winnie_millend.animation.json` | `55dc415d784a9ba2` |
| millstone_rifle | `Guns/models/special/winnie_millend/main.json` | `3bbefbaba9d806c2` |
| millstone_rifle | `Guns/models/special/winnie_millend/hammer.json` | `be66a48f42b58c44` |
| millstone_rifle | `Guns/models/special/winnie_millend/stan_barrel.json` | `bc49dd3a75c2dbd7` |
| millstone_rifle | `Guns/models/special/winnie_millend/silencer.json` | `bf1ce71d5659cbe6` |
| millstone_rifle | `Guns/models/special/winnie_millend/advanced_silencer.json` | `667af6f7937a5033` |
| millstone_rifle | `Guns/models/special/winnie_millend/muzzle_brake.json` | `28f96ea00d937707` |
| millstone_rifle | `Guns/models/special/winnie_millend/ext_barrel.json` | `c1fcdcaae7b9a5b1` |
| millstone_rifle | `Guns/models/special/winnie_millend/light_grip.json` | `eb96a3b5facbadc2` |
| millstone_rifle | `Guns/models/special/winnie_millend/iron_bayonet.json` | `8221f395a25333e5` |
| millstone_rifle | `Guns/models/special/winnie_millend/anthralite_bayonet.json` | `cc1e92c5a79a902d` |
| millstone_rifle | `Guns/models/special/winnie_millend/diamond_bayonet.json` | `06430dbc1ebba073` |
| millstone_rifle | `Guns/models/special/winnie_millend/netherite_bayonet.json` | `80e8597a73a5b675` |
| millstone_rifle | `Guns/models/special/winnie_millend/tact_grip.json` | `23fa52e4ca420b52` |
| millstone_rifle | `Guns/models/special/winnie_millend/sights.json` | `ab7d105d36a760f8` |
| millstone_rifle | `Guns/sounds/item/heavier_rifle/fire.ogg` | `4e17f5a1b2891ee7` |
| millstone_rifle | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| millstone_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| millstone_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| millstone_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| millstone_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |
| fencepost_thumper | `Guns/models/item/fencer_thumper.json` | `3d3cc900694fff66` |
| fencepost_thumper | `Guns/item/fencer_thumper.png` | `265f3046d417ceb6` |
| fencepost_thumper | `Guns/item/fencer_thumper.animation.json` | `ad9d07accadf78b0` |
| fencepost_thumper | `Guns/models/special/fencer_thumper/main.json` | `b416dc445c874e80` |
| fencepost_thumper | `Guns/models/special/fencer_thumper/iron_bayonet.json` | `fca12e8827f3bc63` |
| fencepost_thumper | `Guns/models/special/fencer_thumper/anthralite_bayonet.json` | `f684d8e47c0607cb` |
| fencepost_thumper | `Guns/models/special/fencer_thumper/diamond_bayonet.json` | `495f46a6c177e36f` |
| fencepost_thumper | `Guns/models/special/fencer_thumper/netherite_bayonet.json` | `722f93fb9323837a` |
| fencepost_thumper | `Guns/models/special/fencer_thumper/light_grip.json` | `4908065855d8a91a` |
| fencepost_thumper | `Guns/models/special/fencer_thumper/vert_grip.json` | `861529ab91616b44` |
| fencepost_thumper | `Guns/models/special/fencer_thumper/hammer.json` | `e5cfc09ca9296e2a` |
| fencepost_thumper | `Guns/sounds/item/grenade_launcher/fire.ogg` | `447fa85801d894e8` |
| parley_revolver | `Guns/models/item/pax.json` | `3d9b0a55bc933e24` |
| parley_revolver | `Guns/item/pax.png` | `85e845d1eb80467f` |
| parley_revolver | `Guns/item/pax.animation.json` | `a72b9433509ecd9d` |
| parley_revolver | `Guns/models/special/pax/main.json` | `bc2f2f636d5c55ed` |
| parley_revolver | `Guns/models/special/pax/stan_barrel.json` | `f03bf4557b032c5f` |
| parley_revolver | `Guns/models/special/pax/silencer.json` | `23a5d17a056bf5e1` |
| parley_revolver | `Guns/models/special/pax/advanced_silencer.json` | `b60375010e5e81d7` |
| parley_revolver | `Guns/models/special/pax/muzzle_brake.json` | `d68e9f273822c417` |
| parley_revolver | `Guns/models/special/pax/ext_barrel.json` | `1cdfe0ee362623cd` |
| parley_revolver | `Guns/models/special/pax/light_stock.json` | `205612b23b3e7e24` |
| parley_revolver | `Guns/models/special/pax/heavy_stock.json` | `5c6638e436e65f89` |
| parley_revolver | `Guns/models/special/pax/wooden_stock.json` | `5548e0536662d35a` |
| parley_revolver | `Guns/models/special/pax/drum.json` | `4c824dce6091cd7d` |
| parley_revolver | `Guns/models/special/pax/hammer.json` | `b9a73debc945764b` |
| parley_revolver | `Guns/sounds/item/heavier_rifle/fire.ogg` | `4e17f5a1b2891ee7` |
| lantern_musket | `Guns/models/item/laser_musket.json` | `615461c8162afd48` |
| lantern_musket | `Guns/item/laser_musket.png` | `7324b5d528d5d2d5` |
| lantern_musket | `Guns/item/laser_musket.animation.json` | `e6e4a68072b64240` |
| lantern_musket | `Guns/models/special/laser_musket/main.json` | `56e4997b9a41c315` |
| lantern_musket | `Guns/sounds/item/laser/fire.ogg` | `9ce6c4df8b513390` |
| lantern_musket | `Guns/item/animated_laser_musket.png` | `a1ec8740abdb7c6b` |
| lantern_musket | `Guns/item/laser_musket_glass.png` | `a2f2f66e1dd97ef5` |
| holler_pistol | `Guns/models/item/howler.json` | `b70321061cb27fcf` |
| holler_pistol | `Guns/item/howler.png` | `768f46a178640ddd` |
| holler_pistol | `Guns/item/howler.animation.json` | `260af6cffc3df5f9` |
| holler_pistol | `Guns/models/special/howler/main.json` | `6af62ded19d3ac96` |
| holler_pistol | `Guns/models/special/howler/stan_barrel.json` | `f97e0e16ad3a6612` |
| holler_pistol | `Guns/models/special/howler/silencer.json` | `8950cf2c1968e464` |
| holler_pistol | `Guns/models/special/howler/advanced_silencer.json` | `b3ad85d938549251` |
| holler_pistol | `Guns/models/special/howler/muzzle_brake.json` | `5abd4ce47a9541bb` |
| holler_pistol | `Guns/models/special/howler/ext_barrel.json` | `4ee21e193423017d` |
| holler_pistol | `Guns/models/special/howler/light_stock.json` | `4ddd371a4b9b6f4e` |
| holler_pistol | `Guns/models/special/howler/heavy_stock.json` | `56679384f8db7b6c` |
| holler_pistol | `Guns/models/special/howler/wooden_stock.json` | `9f404bfb31bef4fb` |
| holler_pistol | `Guns/models/special/howler/light_grip.json` | `5e613540dc7cdfaf` |
| holler_pistol | `Guns/models/special/howler/iron_bayonet.json` | `6c48435db3e5ae3a` |
| holler_pistol | `Guns/models/special/howler/anthralite_bayonet.json` | `92a78ccb9e996013` |
| holler_pistol | `Guns/models/special/howler/diamond_bayonet.json` | `a92c373ff358bba1` |
| holler_pistol | `Guns/models/special/howler/netherite_bayonet.json` | `08ba9b8cf483cbaf` |
| holler_pistol | `Guns/models/special/howler/tact_grip.json` | `8051ddea719b68e5` |
| holler_pistol | `Guns/models/special/howler/sights.json` | `444b57a8d4360454` |
| holler_pistol | `Guns/models/special/howler/bolt.json` | `6df05adb7ccae09a` |
| holler_pistol | `Guns/models/special/howler/stan_mag.json` | `1f0c7cb0c4096001` |
| holler_pistol | `Guns/models/special/howler/ext_mag.json` | `b41c2924f80dd887` |
| holler_pistol | `Guns/models/special/howler/speed_mag.json` | `29105cbb000654c9` |
| holler_pistol | `Guns/sounds/item/brass_pistol/fire.ogg` | `4fca376e2ece67b2` |
| holler_pistol | `Guns/item/medium_scope.png` | `543abecf859783be` |
| holler_pistol | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| holler_pistol | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| holler_pistol | `Guns/item/laser.png` | `aac5f06e892e350a` |
| minuet_pistol | `Guns/models/item/m22_waltz.json` | `f17ed40cab2cf903` |
| minuet_pistol | `Guns/item/m22_waltz.png` | `6d1bd268dfac4115` |
| minuet_pistol | `Guns/item/m22_waltz.animation.json` | `21cf81319ce79baa` |
| minuet_pistol | `Guns/models/special/m22_waltz/main.json` | `f2a74ecf1d161879` |
| minuet_pistol | `Guns/models/special/m22_waltz/stan_barrel.json` | `7296e4ba7d8bdfb9` |
| minuet_pistol | `Guns/models/special/m22_waltz/silencer.json` | `3018eb984b96244b` |
| minuet_pistol | `Guns/models/special/m22_waltz/advanced_silencer.json` | `87099918de6d15c2` |
| minuet_pistol | `Guns/models/special/m22_waltz/muzzle_brake.json` | `487d6206a2d97a64` |
| minuet_pistol | `Guns/models/special/m22_waltz/ext_barrel.json` | `be23cd9e96e04097` |
| minuet_pistol | `Guns/models/special/m22_waltz/sights.json` | `b73431bf4d41baf6` |
| minuet_pistol | `Guns/models/special/m22_waltz/receiver.json` | `8f180bcab1c5b84f` |
| minuet_pistol | `Guns/models/special/m22_waltz/stan_mag.json` | `f1c9e2fb7bcff096` |
| minuet_pistol | `Guns/models/special/m22_waltz/ext_mag.json` | `082462f181871de3` |
| minuet_pistol | `Guns/sounds/item/iron_pistol/fire.ogg` | `6a0fe18dfc3165d5` |
| minuet_pistol | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| minuet_pistol | `Guns/item/medium_scope.png` | `543abecf859783be` |
| minuet_pistol | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| minuet_pistol | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| minuet_pistol | `Guns/item/laser.png` | `aac5f06e892e350a` |
| half_crown_revolver | `Guns/models/item/osgood_50.json` | `e4f93511bcf87454` |
| half_crown_revolver | `Guns/item/osgood_50.png` | `9c26dd19d436f65d` |
| half_crown_revolver | `Guns/item/osgood_50.animation.json` | `7cb635d1930d4e45` |
| half_crown_revolver | `Guns/models/special/osgood_50/main.json` | `3275319e1934b7cd` |
| half_crown_revolver | `Guns/models/special/osgood_50/light_stock.json` | `c7cbac829ca84704` |
| half_crown_revolver | `Guns/models/special/osgood_50/heavy_stock.json` | `a23c8ac4710d949d` |
| half_crown_revolver | `Guns/models/special/osgood_50/wooden_stock.json` | `9cdc3263a68ec3fe` |
| half_crown_revolver | `Guns/models/special/osgood_50/stan_barrel.json` | `93f858143c7f84e4` |
| half_crown_revolver | `Guns/models/special/osgood_50/silencer.json` | `cea1db4438f3ccfa` |
| half_crown_revolver | `Guns/models/special/osgood_50/advanced_silencer.json` | `80f5784da196be8f` |
| half_crown_revolver | `Guns/models/special/osgood_50/muzzle_brake.json` | `2f8d931f6a1e1c99` |
| half_crown_revolver | `Guns/models/special/osgood_50/ext_barrel.json` | `06f633d71ca8e4ee` |
| half_crown_revolver | `Guns/models/special/osgood_50/drum.json` | `7fa91cc59e74ce06` |
| half_crown_revolver | `Guns/models/special/osgood_50/hammer.json` | `23c0224325ee8d98` |
| half_crown_revolver | `Guns/sounds/item/brass_revolver/fire.ogg` | `9ede81f3520dfd5c` |
| holler_carbine | `Guns/models/item/howler_conversion.json` | `cd9d9d76eee49fcb` |
| holler_carbine | `Guns/item/howler_conversion.png` | `cf22317ac487ac39` |
| holler_carbine | `Guns/item/howler_conversion.animation.json` | `a55726b4a2658208` |
| holler_carbine | `Guns/models/special/howler_conversion/main.json` | `979db3903a20975b` |
| holler_carbine | `Guns/models/special/howler_conversion/stan_barrel.json` | `682f66f9ca01841a` |
| holler_carbine | `Guns/models/special/howler_conversion/sights.json` | `c14ca49cd2cd9da4` |
| holler_carbine | `Guns/models/special/howler_conversion/bolt.json` | `cb86d0e7f4cf5aa0` |
| holler_carbine | `Guns/models/special/howler/stan_mag.json` | `1f0c7cb0c4096001` |
| holler_carbine | `Guns/sounds/item/brass_pistol/fire.ogg` | `4fca376e2ece67b2` |
| holler_carbine | `Guns/item/howler.png` | `768f46a178640ddd` |
| minuet_carbine | `Guns/models/item/waltz_conversion.json` | `b3b6d43c130a5481` |
| minuet_carbine | `Guns/item/waltz_conversion.png` | `047cf85ab365e901` |
| minuet_carbine | `Guns/item/waltz_conversion.animation.json` | `9370427b5d4420b5` |
| minuet_carbine | `Guns/models/special/waltz_conversion/main.json` | `747d3230aa208b7e` |
| minuet_carbine | `Guns/models/special/waltz_conversion/light_stock.json` | `7d954a24b072ab0e` |
| minuet_carbine | `Guns/models/special/waltz_conversion/heavy_stock.json` | `23b3acadd76359e5` |
| minuet_carbine | `Guns/models/special/waltz_conversion/wooden_stock.json` | `a1c8bc09f2a871ed` |
| minuet_carbine | `Guns/models/special/waltz_conversion/sights.json` | `e1597adeeebb2ed5` |
| minuet_carbine | `Guns/sounds/item/iron_pistol/fire.ogg` | `6a0fe18dfc3165d5` |
| minuet_carbine | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| minuet_carbine | `Guns/item/medium_scope.png` | `543abecf859783be` |
| minuet_carbine | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| minuet_carbine | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| minuet_carbine | `Guns/item/laser.png` | `aac5f06e892e350a` |
| grange_carbine | `Guns/models/item/grandle.json` | `c66593ffde7a056b` |
| grange_carbine | `Guns/item/grandle.png` | `b9aeedd07a230010` |
| grange_carbine | `Guns/item/grandle.animation.json` | `00528c620dda2d26` |
| grange_carbine | `Guns/models/special/grandle/main.json` | `cf2136ce6990a328` |
| grange_carbine | `Guns/models/special/grandle/stan_barrel.json` | `0197a341bb4e61aa` |
| grange_carbine | `Guns/models/special/grandle/silencer.json` | `4a95cd84a0158d4c` |
| grange_carbine | `Guns/models/special/grandle/advanced_silencer.json` | `7695ef649b257e63` |
| grange_carbine | `Guns/models/special/grandle/muzzle_brake.json` | `e03fa8ab8f457601` |
| grange_carbine | `Guns/models/special/grandle/ext_barrel.json` | `b3ad11cd1d641fb3` |
| grange_carbine | `Guns/models/special/grandle/light_stock.json` | `8939deb3d0665084` |
| grange_carbine | `Guns/models/special/grandle/heavy_stock.json` | `5c7fb8025fe4efb8` |
| grange_carbine | `Guns/models/special/grandle/wooden_stock.json` | `7e78c522585f8077` |
| grange_carbine | `Guns/models/special/grandle/light_grip.json` | `880cbee82daa7f7c` |
| grange_carbine | `Guns/models/special/grandle/iron_bayonet.json` | `8d0006f4abb52a25` |
| grange_carbine | `Guns/models/special/grandle/anthralite_bayonet.json` | `2a72b79d513fc2e9` |
| grange_carbine | `Guns/models/special/grandle/diamond_bayonet.json` | `b7f05a773b22c5c6` |
| grange_carbine | `Guns/models/special/grandle/netherite_bayonet.json` | `922a28f199ccd827` |
| grange_carbine | `Guns/models/special/grandle/tact_grip.json` | `79f3d8cde481ee9e` |
| grange_carbine | `Guns/models/special/grandle/sights.json` | `b480903472926df1` |
| grange_carbine | `Guns/models/special/grandle/no_sights.json` | `719909411988aac2` |
| grange_carbine | `Guns/models/special/grandle/bolt.json` | `2c6065f5233cf70c` |
| grange_carbine | `Guns/models/special/grandle/stan_mag.json` | `91e3b7b0836db229` |
| grange_carbine | `Guns/models/special/grandle/ext_mag.json` | `de362dabe31e767a` |
| grange_carbine | `Guns/models/special/grandle/speed_mag.json` | `039a498c5efb4704` |
| grange_carbine | `Guns/sounds/item/old_rifle/fire.ogg` | `d684ae9a09c9c12c` |
| grange_carbine | `Guns/item/medium_scope.png` | `543abecf859783be` |
| grange_carbine | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| grange_carbine | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| grange_carbine | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| grange_carbine | `Guns/item/laser.png` | `aac5f06e892e350a` |
| sprocket_gun | `Guns/models/item/cogloader.json` | `afdff981c85abed1` |
| sprocket_gun | `Guns/item/cogloader.png` | `71810fd3fc7c0cb2` |
| sprocket_gun | `Guns/item/cogloader.animation.json` | `34747fadbeff9c11` |
| sprocket_gun | `Guns/models/special/cogloader/main.json` | `bfaa92b9b3195d05` |
| sprocket_gun | `Guns/models/special/cogloader/stan_barrel.json` | `225148dd2623f619` |
| sprocket_gun | `Guns/models/special/cogloader/silencer.json` | `f472b4dfa762ad6d` |
| sprocket_gun | `Guns/models/special/cogloader/advanced_silencer.json` | `b793d0d629534450` |
| sprocket_gun | `Guns/models/special/cogloader/muzzle_brake.json` | `3cd54cb3ef1de269` |
| sprocket_gun | `Guns/models/special/cogloader/ext_barrel.json` | `984a1433468fb410` |
| sprocket_gun | `Guns/models/special/cogloader/light_stock.json` | `e79fb89da3c88066` |
| sprocket_gun | `Guns/models/special/cogloader/heavy_stock.json` | `24dabda8530aa5d6` |
| sprocket_gun | `Guns/models/special/cogloader/wooden_stock.json` | `9f3924171f1875e3` |
| sprocket_gun | `Guns/models/special/cogloader/light_grip.json` | `ad5862a01a437055` |
| sprocket_gun | `Guns/models/special/cogloader/iron_bayonet.json` | `215932fd2aaa5802` |
| sprocket_gun | `Guns/models/special/cogloader/anthralite_bayonet.json` | `a6b591f0ca0436c6` |
| sprocket_gun | `Guns/models/special/cogloader/diamond_bayonet.json` | `e034e7edb1ecf421` |
| sprocket_gun | `Guns/models/special/cogloader/netherite_bayonet.json` | `4978562caff71bc8` |
| sprocket_gun | `Guns/models/special/cogloader/tact_grip.json` | `3debca5d359b9fd8` |
| sprocket_gun | `Guns/models/special/cogloader/sights.json` | `2c5362ec880c91d3` |
| sprocket_gun | `Guns/models/special/cogloader/bolt.json` | `44d8e4e405d2e4fc` |
| sprocket_gun | `Guns/models/special/cogloader/magazine.json` | `705858dd2839b1e6` |
| sprocket_gun | `Guns/sounds/item/scorched_rifle/fire.ogg` | `5a52d37065a09a8c` |
| sprocket_gun | `Guns/item/makeshift_rifle_bayonets.png` | `dfc93f5032504095` |
| sprocket_gun | `Guns/item/medium_scope.png` | `543abecf859783be` |
| sprocket_gun | `Guns/item/long_scope_texture.png` | `e69191017eb081ad` |
| sprocket_gun | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| sprocket_gun | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| sprocket_gun | `Guns/item/laser.png` | `aac5f06e892e350a` |
| navvy_shotgun | `Guns/models/item/jackhammer.json` | `bfd5eb7ffc2a950d` |
| navvy_shotgun | `Guns/item/jackhammer.png` | `2198cf8cfda3a00b` |
| navvy_shotgun | `Guns/item/jackhammer.animation.json` | `38c0ab9e9239f119` |
| navvy_shotgun | `Guns/models/special/jackhammer/main.json` | `1e31cf33db2a1bf7` |
| navvy_shotgun | `Guns/models/special/jackhammer/stan_barrel.json` | `c5f2a8cdc823ee83` |
| navvy_shotgun | `Guns/models/special/jackhammer/silencer.json` | `81a90dc19de3256b` |
| navvy_shotgun | `Guns/models/special/jackhammer/advanced_silencer.json` | `51e88b550abfd321` |
| navvy_shotgun | `Guns/models/special/jackhammer/muzzle_brake.json` | `10e9453b32c0b870` |
| navvy_shotgun | `Guns/models/special/jackhammer/ext_barrel.json` | `81c9d548e22ce884` |
| navvy_shotgun | `Guns/models/special/jackhammer/stan_grip.json` | `276f7c936bf66c9c` |
| navvy_shotgun | `Guns/models/special/jackhammer/light_stock.json` | `b4e8bf9ae4ae35de` |
| navvy_shotgun | `Guns/models/special/jackhammer/heavy_stock.json` | `1f0af1c2d818acab` |
| navvy_shotgun | `Guns/models/special/jackhammer/wooden_stock.json` | `f847aedcac2727ca` |
| navvy_shotgun | `Guns/models/special/jackhammer/light_grip.json` | `4bfe4ee31c4f0220` |
| navvy_shotgun | `Guns/models/special/jackhammer/iron_bayonet.json` | `9406c32986faa8e2` |
| navvy_shotgun | `Guns/models/special/jackhammer/anthralite_bayonet.json` | `c87e249920212c08` |
| navvy_shotgun | `Guns/models/special/jackhammer/diamond_bayonet.json` | `427135423505b6c1` |
| navvy_shotgun | `Guns/models/special/jackhammer/netherite_bayonet.json` | `d80748990b9ee2a2` |
| navvy_shotgun | `Guns/models/special/jackhammer/tact_grip.json` | `6e40954a1cea505b` |
| navvy_shotgun | `Guns/models/special/jackhammer/bolt.json` | `fbcddf88c1f409d3` |
| navvy_shotgun | `Guns/models/special/jackhammer/stan_mag.json` | `4f9ea74efcb8c508` |
| navvy_shotgun | `Guns/models/special/jackhammer/ext_mag.json` | `ed42e46480e7913e` |
| navvy_shotgun | `Guns/models/special/jackhammer/speed_mag.json` | `bc4061e4c23b4e9c` |
| navvy_shotgun | `Guns/sounds/item/combat_shotgun/fire.ogg` | `155925e837a3d40b` |
| freeholder_rifle | `Guns/models/item/libertas.json` | `e9a8eee00e777540` |
| freeholder_rifle | `Guns/item/libertas.png` | `0929828ebf746e69` |
| freeholder_rifle | `Guns/item/libertas.animation.json` | `15a6545ab6bf408f` |
| freeholder_rifle | `Guns/models/special/libertas/main.json` | `116179cd8f0500ca` |
| freeholder_rifle | `Guns/models/special/libertas/sights.json` | `50ae78c6f0e80ce3` |
| freeholder_rifle | `Guns/models/special/libertas/stan_mag.json` | `e2e553a8d803d28c` |
| freeholder_rifle | `Guns/models/special/libertas/ext_mag.json` | `445ba427cd5e9562` |
| freeholder_rifle | `Guns/models/special/libertas/speed_mag.json` | `7e857ec330699da5` |
| freeholder_rifle | `Guns/sounds/item/scorched_sniper/fire.ogg` | `624b2460a3e39921` |
| freeholder_rifle | `Guns/item/medium_scope.png` | `543abecf859783be` |
| freeholder_rifle | `Guns/item/relex_sight.png` | `c8a38dbce7c266b0` |
| freeholder_rifle | `Guns/item/laser_sight.png` | `1e0a3342068bc5df` |
| freeholder_rifle | `Guns/item/laser.png` | `aac5f06e892e350a` |

- **The bolt sound's tag:** `bolt.ogg` carries Vorbis tags naming another source ("All Epic Infantry Assault Rifle Reload Sounds (Fortnite)"). The owner, asked about the eight tagged sounds in the library, answered "I have the rights", so it is used like the rest. None of the other sounds used here carries such a tag.
- **The scopes' reticles and lens rims (slice 7b):** the owner uploaded them on 8 October 2026 ("heres reticles and vignette"). Two files in that upload carry embedded Photoshop metadata:
  - `red_dot_reticle.png`'s editing history shows it saved inside a Just Enough Guns mod source folder in February 2024;
  - `muzzle_flash.png` (not used here) was made in Photoshop in 2021.

  Shown this and asked what the scopes should use, the owner answered that they made these files and to use them. Asked the same day whether the "Big Cannons and Mounted Guns" folder is theirs (slice 6's flash frames come from it, and it holds a `wariumlogo.png`), they confirmed it is. The [library README](../../art/owner-library/README.md#effect-textures-8-october-2026) records both.
- **Names:** Jugcraft's own, under the license policy's fan-homage rules. No `scguns:` reference reaches the game: the converter writes its own files with `jugcraft:` paths, and the animations name no resources.
- **Drawn here:** the round icons, as maps (`tools/item_icons/light_round.txt`, `rifle_round.txt`, `buckshot_shell.txt`, `paper_cartridge.txt`), and the props' pixels in the atlas copies (the Thunderpipe's and the Highwayman's shells, the Longhorn's cartridge, slice 4's muzzle-loaders' balls, ramrods and flashes, the Stoker's fuel can, the Linesman's and the Glowmouth's cells, the Skylark Rifle's rocket, the Bullfrog's grenade, the Fowler's balls, ramrod and flash, and the Culverin's ball and flash). The Stoker's flame is the owner's pilot flame, copied pixel for pixel into its atlas corner.
- **The Cell Rack (slice 10E):** its block model is built here from boxes (`cell_rack_model()` in `tools/guns.py`) on the power gear's electric-look textures, the Charging Station's. Nothing new is drawn, and no owner file is used; the cells in it are the Energy Cell's own icons.

## Verification
- **Run locally (7 October 2026):**
  - `python3 tools/guns.py`: writes the GeckoLib files, then PASS on the check. Every face of the 3 guns' parts comes back at the same corners with the same UVs and turns.
  - `python3 tools/generate_material_data.py`: run twice; the second run changed nothing.
  - `python3 tools/check_mod_data.py`: PASS (1551 material IDs), with `check_guns`.
  - `python3 scripts/check_repository.py`, `python3 tools/check_icon_maps.py`: PASS. The icon checker warns that the Light Round and the Buckshot Shell are a little shorter (7 and 8 steps) than the "short" family's 9 to 12. They are small rounds.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Game tests (written; they run in CI):**
  - `GunsGameTests` (server): every gun and round registered; a carbine shot hurts its target by 6 and spends a round; an empty gun and an empty hand do not fire; the trigger rate holds and two quick shots both land; creative spends nothing; a magazine reload loads from the inventory after its time and not before; no reload without rounds or room; a shell reload loads one at a time; a shot cuts a shell reload short; switching away stops a reload; pellets land together; bullets are projectiles.
  - `GunsClientGameTests` (client): end to end through the real keys, for each gun:
    - draw, aim, fire at a husk (the server lands it and spends a round), reload part way and done (one round from the inventory), inspect;
    - the husk stands seven blocks off; a gun whose aimed cone is wider than the husk there fires from where it is not (`0.45 / tan(aimed spread)` blocks, the husk's half width and the hit test's reach past it), so one shot cannot miss by chance. Added after the Culverin's five pellets all went past it once ([#355's comment](https://github.com/jimbozoomer-byte/jugcraft/pull/355#issuecomment-6159532662)): it fires from 3.7 blocks; 14 other wide-spread guns from 2.5 to 6.4.
    - the Thunderpipe's shell reload part way; each gun in third person; the inventory.
    - Screenshots `jugcraft_guns_*`.
- **CI on `33823d0c0` (Build run 37651397033):** every job passed: the mod build with the server game tests, the build without the optional integrations, and the client test.
  - The client test's log: the Midge took the husk from 200 to 198 health and spent a round; the Carbine took it from 198 to 192; the Thunderpipe from 192 to 174.5 (7 of its 8 pellets). Each reload then loaded one round and left 31 in the inventory.
  - **Screenshots read:** the guns in the owner's textures; your arms rising from the bottom of the screen to the grip and fore-end; the sight on the crosshair when aiming; the reloads and inspects; each gun in the right hand in third person; the guns and rounds in the inventory.
  - **Fixed after reading them:** the Thunderpipe's spare shell showed, out of place, during its inspect; and the counter read one round low until the next shot settled it (it now settles each answer as it comes). Their run is in the pull request.
- **Slice 2, run locally (7 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the six guns' parts comes back at the same corners with the same UVs and turns; each hand is where `BUILDS` puts it in its pose; each shoulder locator is 10 px from its hand, below it.
  - `python3 tools/generate_material_data.py`: run twice; the second run changed nothing.
  - `python3 tools/check_mod_data.py`: PASS (1554 material IDs), with `check_guns`; `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **First-person previews** of each new gun held, aimed and part way through its reload, drawn with the game's hand transforms; and the hand-to-part distances above.
- **Slice 2 game tests (written; they run in CI):**
  - `GunsGameTests` adds: the Warden Pistol and the Riveter SMG each land one shot's damage and spend a round; the Haymaker loads a shell at a time, stops at the two shells there are, then its pellets land together. "Every gun registered" now expects six guns.
  - `GunsClientGameTests` runs every gun, so the new three are drawn, aimed, fired at the husk, reloaded and inspected through the real keys too.
- **Slice 3, run locally (7 October 2026):**
  - `python3 tools/guns.py`: PASS for all nine guns, the lever loops and the Callwell's barrels included (each element on its bone re-bakes to the owner's).
  - `python3 tools/generate_material_data.py`: run twice; the second run changed nothing.
  - `python3 tools/check_mod_data.py`: PASS (1557 material IDs), with `check_guns`; `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:** side and top views of the levers swinging and the Coach Gun breaking open; first-person views held, aimed and reloading, which led to the lowered right hands. The preview now clips what crosses the near plane, as the game does, instead of dropping it.
- **Slice 3 game tests (written; they run in CI):** `GunsGameTests` adds: the Longhorn and the Drover each land one shot's damage and spend a round, and the Drover loads three rounds one at a time; the Coach Gun's pellets land together and spend one barrel. "Every gun registered" expects nine guns. The client test runs every gun.
- **Slice 4, run locally (7 October 2026):**
  - `python3 tools/guns.py`: PASS for all twelve guns (the hammers re-bake to the owner's parts; the drawn props are skipped by the face check and drawn into empty corners only).
  - `python3 tools/generate_material_data.py` twice (the second run changed nothing) and `python3 tools/generate_textures.py` for the cartridge's icon. That generator also rewrote 24 unrelated flower textures; those were left out of the change.
  - `python3 tools/check_mod_data.py`: PASS (1561 material IDs), with `check_guns`; `python3 scripts/check_repository.py` and `python3 tools/check_icon_maps.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:** side and top views of the shots (hammer, flash) and the reloads (ball, ramrod); first-person views held, aimed and through the Duelling Pistol's reload, which found the known limit above and shrank the flash from 2 px to 1.
- **Slice 4 game tests (written; they run in CI):** `GunsGameTests` adds: the Line Musket lands its 14, will not fire again empty, and loads one cartridge after its reload and not before; the Bellmouth's balls land together at close range. "Every gun registered" expects twelve guns and four rounds. The client test runs every gun; its husk now has 1000 health, enough for all twelve, and cannot be knocked back, and the player's aim is set afresh before each gun (on the first CI run the eleven shots before it had kicked the view up over the husk's head, so the Line Musket's shot passed over it).
- **Slice 5, run locally (7 October 2026):**
  - `python3 tools/guns.py`: PASS for all twelve guns with their attachment bones: every attachment part re-bakes to the owner's, face for face, and no prop's atlas corner overlaps a part's texture.
  - `python3 tools/generate_material_data.py`: run twice; the second run changed nothing.
  - `python3 tools/check_mod_data.py`: PASS (1572 material IDs), with `check_guns`, which now also holds `JugcraftGuns.ATTACHMENTS`, `SLOTS` and `ACCEPTS` to `tools/guns.py`. Changing one attachment's number or one gun's list in the Java made it fail, as it should.
  - `python3 scripts/check_repository.py`: PASS. A check for recipes sharing a pattern found none for the eleven new ones.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:** side views of the seven guns bare and with four sets of attachments fitted, drawn from the converted models; each attachment sits where the owner modelled it, and the replaced barrels, magazines and the Thunderpipe's grip are hidden.
- **Slice 5 game tests (written; they run in CI):**
  - `GunsGameTests` adds: attachments fit and come off through the crafting recipes (an Extended Magazine gives the Rust Midge 30 rounds and a 54-tick reload; a Speed Magazine replaces it, leaving it in the grid, for a 31-tick reload; a Silencer fits beside it at 35% volume and 95% damage; the Thunderpipe refuses a magazine, a gun a second Silencer, shears a bare gun; shears take the Silencer off and stay); an Extended Magazine loads 30 rounds after its longer reload and not in the Midge's own time, and with it taken off the gun keeps and fires them but will not reload; a silenced shot lands 95% of the damage. "Every gun registered" checks the eleven attachments.
  - `GunsClientGameTests` adds each gun that takes attachments held and aimed with two sets fitted, checks that the client sees a fitted Extended Magazine's 30 rounds, and shows the attachments and a fitted Patchwork Carbine in the inventory.
- **Slice 6, run locally (7 October 2026):**
  - `python3 tools/guns.py`: PASS. The new locators changed the seven models with barrel attachments and nothing else. The casing and flash textures are the owner's files unchanged, and every particle cue in the animations is one the guns know.
  - `python3 tools/generate_material_data.py`: it added only the three casing particle definitions.
  - `python3 tools/check_mod_data.py`: PASS (1572 material IDs), with `check_guns`, which now also holds `GunLooks` and `JugcraftGuns.CASING_AMMO` to `tools/guns.py` and checks the hooks are called. `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - Not seen yet: the flash, the casings and the pose in the game. The CI screenshots are the first look.
- **CI on `f866b051c` (Build run 37694286546):** every job passed: the mod build and server game tests, the build without the optional integrations, and all three client shards.
  - The client test's log, for all twelve guns:
    - aimed, the field of view modifier went from 1.0 to the gun's zoom (0.92 down to 0.75);
    - fired, 4 to 6 flash frames were drawn;
    - each gun whose animations cue a casing threw one or more (the Line Musket has no cue);
    - in third person, the player was posed holding a gun for 3,516 frames.
  - **Screenshots read:**
    - the view narrowed when aimed (the wall larger than in slice 5's shots);
    - the Line Musket's and Bellmouth's flash and white cloud at the muzzle;
    - the Longhorn's flash seen from in front;
    - the gun raised in front of the chest seen from in front;
    - from behind, the Longhorn held in both hands and the Warden Pistol in one, the other arm down.
  - **Seen, and recorded above as a known limit:** aimed, the gun hides most of its flash. The Bellmouth's cloud fills the front view at the moment of the shot.
  - **The first run failed, not on this slice's code:** an earlier test in the same shard had left the HUD hidden, which hides the hand. The test now shows the HUD first and puts back what it found.
- **Slice 6 game tests (written; they run in CI):** `GunsClientGameTests`, for every gun:
  - aimed, the field of view modifier is narrowed (or the test says the mixin never ran);
  - fired, at least one muzzle flash frame is drawn;
  - fired and reloaded, a casing (or a puff) is thrown if its animation cues one;
  - in third person, each gun is held and fired (screenshots `_third_person` and `_third_person_fired`), and the player is posed holding it; the Longhorn and the Warden Pistol are also shown from behind.
- **Slice 7, run locally (7 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of every attachment part, the bayonets and the five guns' shared-texture parts included, re-bakes to the owner's through the gun's atlas. The atlases are the owner's files placed as the layout says, over no pixel the gun's own texture uses.
  - `python3 tools/generate_material_data.py`: the four bayonets' items, models, recipes and names.
  - `python3 tools/check_mod_data.py`: PASS (1576 material IDs). Its texture rule first failed the atlases at 256 × 128, which led to packing into free room. `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:** side views of the five guns bare and with each kind of attachment, and of the bayonets on four guns, drawn from the converted models. The bayonet blades are one-sided planes (GeckoLib draws them from both sides).
- **CI on `c6b72208b` (Build run 37697733190):** every job passed: the mod build and server game tests (the bayonet and shared-texture tests among them), the build without the optional integrations, and all three client shards.
  - The client test's log: a Steel Bayonet stab with the V key took the husk from 876 to 871, the bayonet's 5.
  - **Screenshots read:**
    - the Drover Rifle, Coach Gun, Duelling Pistol, Line Musket and Bellmouth with their newly fitting attachments, drawn from their merged atlases with no missing or garbled texture;
    - the Patchwork Carbine thrust forward at the husk in the stab.
  - **The first run failed, not on this slice's code:** `ArmsVIIIGameTests.harpoonHaulsItsCatch`, the intermittent failure #257 fixes. That fix is ported into this branch.
- **Slice 7 game tests (written; they run in CI):**
  - `GunsGameTests` adds:
    - a Patchwork Carbine's Iron Bayonet strikes a pig two blocks ahead for 4, and cannot stab again at once;
    - a bare carbine cannot stab;
    - a bayonet does not reach a pig seven blocks off;
    - the Drover Rifle, Line Musket, Coach Gun, Duelling Pistol and Bellmouth each take a shared-texture attachment through the crafting recipe;
    - the Netherite Bayonet's smithing recipe loads;
    - "Every gun registered" checks the fifteen attachments.
  - `GunsClientGameTests` adds a third fitted set (Muzzle Brake, Wooden Stock, Iron Bayonet) on every gun that takes attachments (now all twelve), and a Steel Bayonet stab with the V key that hurts the husk.
- **Slice 7b, run locally (8 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three scopes' bodies re-bakes to the owner's, moved onto each gun and drawn through its packed atlas pieces. Each atlas holds every piece where the layout says, on no pixel the gun's own texture uses and on no other piece. Only the Longhorn's, Drover's and Riveter's models and atlases changed.
  - `python3 tools/generate_material_data.py`: wrote the scopes' items, item models (their lenses on the owner's reticle and rim textures), recipes and names.
  - `python3 tools/check_mod_data.py`: PASS (1579 material IDs). `check_guns` now also checks `GunLooks.OPTICS`, the five-attachment cap and `GunMouseMixin`.
  - `python3 scripts/check_repository.py`: PASS.
  - **Recipes:** none of the three shares a pattern with another recipe.
  - **Java:** a syntax parse only.
  - **Previews:** side views of the three guns bare and with each scope, drawn from the converted models: each scope stands on the receiver (on the Riveter's rail), and the iron sights are gone.
- **Slice 7b in CI** ([run 37731926805](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37731926805), on 08c3e8f9c): every check passed.
  - One client shard first failed in `FlailClientGameTests` (the flail ball sank 0.08 blocks, past the 0.0625 allowed), which this slice does not touch. It passed when re-run; the cause is noted on #265.
  - **Screenshots:**
    - Through the Long and Medium Scopes, the owner's reticle and lens rim frame a narrowed view, black at the sides, and the gun is gone.
    - The Reflex Sight keeps the gun in view, its dot in the middle.
    - Held, each scope stands on the Longhorn's receiver.
  - **The test's log**, from the run on #268, which carries this slice:
    - the Long Scope narrowed the view to 0.3 and drew the view through it for 30 frames;
    - the Medium Scope narrowed it to 0.5, with 28 frames;
    - a mouse movement through the Long Scope was scaled to 0.3. The test's window turned the player 0.0°, so the turn itself is not shown.
- **Slice 7b game tests (written; they run in CI):**
  - `GunsGameTests` adds `scopesFitTheGunsMadeForThem`:
    - the three scope recipes load, and the three guns take each scope;
    - a Long Scope on the Longhorn halves its aimed spread and adds a quarter to its hip spread;
    - a Medium Scope takes the Long Scope's place, which stays in the grid;
    - a Reflex Sight fits beside a Silencer, Extended Magazine and Wooden Stock on the Riveter;
    - the Patchwork Carbine takes none;
    - an attachment in each of the five slots saves.
    - "Every gun registered" checks the eighteen attachments.
  - `GunsClientGameTests` aims through each scope on the Longhorn at the husk:
    - the field of view narrows by the scope's zoom (0.3, 0.5, 0.85);
    - the view through the scope (Long, Medium) or the reflex dot is drawn;
    - through the Long Scope, a mouse movement turns the player more slowly, where the test's window lets the mouse turn the player at all (the log says which).
    - Screenshots `jugcraft_guns_<scope>` and `jugcraft_guns_<scope>_aimed`.
- **Slice 8, run locally (8 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts re-bakes to the owner's. Each hand is where `BUILDS` puts it in its pose, with each shoulder locator 10 px below it. The Bulldog's atlas grew to 128 px to hold the scopes' pieces.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and shot sounds; run again after the last change, it changed nothing more.
  - `python3 tools/check_mod_data.py`: PASS (1869 material IDs).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only.
  - **Previews,** approximating the game's first-person hands: each gun's reload, frame by frame. Side views of the Bulldog's reload, in the gun's own frame, show:
    - the opened barrel standing just clear of the frame;
    - the round going into the breech bullet first;
    - each left arm coming up from below the screen while it shows.
- **Slice 8 game tests (written; they run in CI):**
  - `GunsGameTests` adds `handGunsLandAndLoad`:
    - the Bulldog lands its 11 and spends its round, does not fire empty, and loads one rifle round in its reload time and not before;
    - the Marshal lands its 5 and loads a round at a time: the two rounds there are, and no more;
    - the Sapper lands its 4.5.
    - "Every gun registered" now counts fifteen guns.
  - `GunsClientGameTests` already takes every gun through its steps:
    - held and aimed;
    - fired at the husk, with a muzzle flash;
    - reloaded part way and done, throwing casings where the animations cue them;
    - inspected, seen in third person and in the inventory, and held with each set of its attachments fitted.
    - The new guns' screenshots are `jugcraft_guns_bulldog_pistol_*`, `jugcraft_guns_marshal_revolver_*` and `jugcraft_guns_sapper_revolver_*`.
  - The inventory screenshot is now two, `jugcraft_guns_inventory` (the guns and rounds) and `jugcraft_guns_inventory_attachments`: fifteen guns, four rounds and eighteen attachments no longer fit one inventory.
- **Slice 8 in CI** ([run 37734950290](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37734950290), on c6f8c5772): every check passed.
  - `mod`: the build and the server game tests passed, `handGunsLandAndLoad` among them.
  - `client` (shard 0): `GunsClientGameTests` passed. Its log:
    - **Bulldog Pistol:** aimed, the view narrowed to 0.9; fired, 6 flash frames, the husk 870.5 → 859.5 and rounds 1 → 0; reloaded 1, 31 Rifle Rounds left; 1 casing thrown.
    - **Marshal Revolver:** aimed, 0.85; fired, 5 flash frames, the husk 859.5 → 854.5 and rounds 6 → 5; reloaded 6, 31 Light Rounds left; no casings (its animations cue none).
    - **Sapper Revolver:** aimed, 0.9; fired, 5 flash frames, the husk 854.5 → 850.0 and rounds 6 → 5; reloaded 6, 31 left; 4 casings thrown.
  - **Screenshots:**
    - Mid-reload, the Bulldog's left hand brings the brass round up to the opened breech, the round on its fingertips.
    - The Marshal's and Sapper's reloads raise the gun close, and the arms cover much of the screen, as the previews showed.
    - Aimed, the gun hand's fist covers the lower middle of the screen, over the sights. The Warden Pistol, Duelling Pistol and Longhorn Rifle do the same in that run, so this predates slice 8 ([Rollout](#rollout-and-open-questions)).
    - Both inventory screenshots are drawn.
- **Slice 8B, run locally (8 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts re-bakes to the owner's, each hand is where `BUILDS` puts it in its pose, and the scopes' pieces fit the free room of each 128 px atlas.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and shot sounds; run again after the last change, it changed nothing more.
  - `python3 tools/check_mod_data.py`: PASS (1872 material IDs).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only.
  - **Previews:**
    - first-person views of each gun idle, aimed and mid-reload or mid-pump (approximating the game's hands);
    - side views of each with a Long Scope, and with a Reflex Sight, Silencer, Extended Magazine and Light Stock fitted.
- **Slice 8B game tests:**
  - `GunsGameTests` adds `serviceArmsLandAndLoad`:
    - the Sentry lands its 5, and its magazine reload loads the round it was short in its reload time and not before;
    - two Garrison shots an interval apart both land;
    - at close range the Breacher's pellets land together.
    - "Every gun registered" now counts eighteen guns.
  - `GunsClientGameTests` takes the three through every gun's steps, with screenshots `jugcraft_guns_sentry_pistol_*`, `jugcraft_guns_garrison_rifle_*` and `jugcraft_guns_breacher_*`.
- **Slice 8B in CI** ([run 37737317739](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37737317739), on 793fac2a5): every check passed.
  - `mod`: the build and the server game tests passed, `serviceArmsLandAndLoad` among them.
  - `client` (shard 1): `GunsClientGameTests` passed. Its log:
    - **Sentry Pistol:** aimed, the view narrowed to 0.9; fired, 10 flash frames, the husk 847.0 → 842.0 and rounds 8 → 7; reloaded 8, 31 Light Rounds left; 1 casing thrown.
    - **Garrison Rifle:** aimed, 0.85; fired, 9 flash frames, the husk 842.0 → 838.0 and rounds 30 → 29; reloaded 30, 31 Rifle Rounds left; 1 casing thrown.
    - **Breacher:** aimed, 0.92; fired, 10 flash frames, the husk 838.0 → 814.0 (all eight pellets) and rounds 6 → 5; reloaded 6, 31 Buckshot Shells left; 1 casing thrown.
  - **Screenshots:**
    - Each of the three is held, aimed, fired, reloaded and inspected, seen in third person and fitted with each set of its attachments. Aimed with a stock fitted, the Garrison's and Breacher's stocks fill the lower middle of the screen, below the sights.
    - **The Garrison Rifle's aimed shot filled the screen with the gun:** its bolt slid back past the eye ([above](#slice-8b-the-service-arms)). 6a9e55b5d holds it 4 px further out aimed.
- **The Garrison held further out, in CI** ([run 37740859066](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37740859066), on e26e2d9a8): every check passed.
  - **Aimed and fired,** the back of the gun sits small under the crosshair with the husk in view, where the shot had filled the screen. With a stock fitted, the stock is smaller too.
  - **The log** is as before for all three: the Garrison took 4 from the husk and reloaded 30, and the Breacher took 24.
- **Slice 8C, run locally (9 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts re-bakes to the owner's, each hand is where `BUILDS` puts it in its pose, and the fuel can's and flame's corners of the Stoker's atlas are clear of every part.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names, shot sounds, the pump sounds and the flame damage type with its tags.
  - `python3 tools/check_mod_data.py`: PASS (1912 material IDs), now checking `JugcraftGuns.SHOTS`, `SPIN_UP` and `PER_ITEM`, the flame's death message and that `GunRenderer` hides every prop bone.
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only.
  - **Previews:**
    - first-person views of each gun idle, aimed, firing and part way through its reload (approximating the game's hands), which placed the arms and sights;
    - side views of the Stoker's flame through a shot and its can out mid-reload.
- **Slice 8C game tests (written; they run in CI):**
  - `HeavyGunsGameTests`:
    - `heavyWeaponsFireTheirOwnAmmunition`: the Lobber fires Grenades, the Stoker blaze powder at four bursts a powder, the Thresher rifle rounds; the flame is fire, counts each burst and does not knock back.
    - `lobberLobsAGrenade`: a shot spends a grenade and lobs the shooter's Grenade, which bursts on the pig seven blocks off; the shooter is unhurt and the floor whole.
    - `thresherSpinsUpBeforeItFires`: refused at once and half way through the spin-up, fired once spun up, refused again after the trigger is let go.
    - `stokerSetsCreaturesAlightNotBlocks`: the pig in the jet takes the burst and burns; one beside the shooter and one past the reach are untouched; no fire block anywhere.
    - `reloadsTakeAmmunitionByTheItem`: three powders load twelve bursts; a top-up one burst short takes one whole powder; the Lobber loads the two grenades there are.
  - "Every gun registered" now counts twenty-one guns.
  - `GunsClientGameTests` takes the three through every gun's steps:
    - it gives each gun's ammunition by its item id (a Grenade, blaze powder);
    - it holds the Thresher's trigger through its spin-up;
    - it allows the Thresher more than one round spent, and counts a reload's items by the rounds each loads.
    - Screenshots: `jugcraft_guns_trench_lobber_*`, `jugcraft_guns_thresher_*` and `jugcraft_guns_stoker_*`.
- **Slice 8C in CI** ([run 37989013678](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37989013678), on e9f53d806): every check passed.
  - `mod`: the build and the server game tests passed, the five `HeavyGunsGameTests` among them.
  - `client` (shard 2): `GunsClientGameTests` passed. Its log:
    - **Trench Lobber:** aimed, the view narrowed to 0.9; fired, 5 flash frames, the husk 833.5 → 821.3 (the Grenade's burst) and rounds 6 → 5; reloaded 6, 31 Grenades left.
    - **Thresher:** aimed, 0.95; held through its spin-up and fired, 10 flash frames, the husk 821.3 → 815.3 and rounds 60 → 58 (two shots in the moment the trigger was held); reloaded 60, 30 Rifle Rounds left.
    - **Stoker:** aimed, 0.95; fired, 6 flash frames, the husk 815.3 → 813.3 and bursts 32 → 31; reloaded 32, 31 blaze powder left. Its animations' casing cue puffed smoke once, as it has no casing.
  - **Screenshots:**
    - The Lobber is held, aimed, fired, reloaded and inspected as the owner's animations show it. With the Extended Magazine fitted, its count reads 1 / 9.
    - The Stoker points at the husk seen from outside.
    - **The Thresher's left hand and forearm filled the right half of the screen** in first person (held, aimed and fired), from the carry handle by the eye.
    - **Seen from outside, the Thresher pointed at the sky** (its transform's 68.25° tilt, [above](#slice-8c-the-heavy-weapons)).
    - **The Stoker is held close,** as the owner's transform holds it.
  - **The fix** (24f52c601): the Thresher's left hand holds the front plate (a first-person preview of it idle, firing and through its reload and inspection leaves the view clear), and its holder's arms hang lower by the tilt.
- **The fix in CI** ([run 37992650952](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37992650952), on 24f52c601): every check passed.
  - `optional integrations absent` passed on its one re-run. Its first attempt failed a biome test this slice does not touch: four mossy maples laid no moss carpet. The pull request's comment of 9 October has the cause, vanilla's `attached_to_logs` decorator, whose rolls depend on where the test is placed, and a proposed patch for the biome tests.
  - **Screenshots** (`client` shard 2):
    - In first person the Thresher sits at the lower right, held, aimed, fired, mid-reload and inspected; no hand covers the view.
    - Seen from outside, it is carried at the hip with its barrels toward the camera, the way its holder looks, not at the sky.
- **Arms by the eye** ([run 37999611685](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37999611685), on afea6a3d8): every check passed.
  - `mod` passed on its one re-run. Its first attempt failed the thrown arms' javelin test, which this change does not touch; the 8C pull request's comment of 9 October has what is known of it.
  - Every gun's shots and reloads look as before. The Lobber's left sleeve shows at the lower left of its fired frame.
  - In slice 8D's next run (below), the Beam Pistol's mid-reload view, which its left arm had filled, is clear.
- **Slice 8D, run locally (9 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts re-bakes to the owner's, and each hand is where `BUILDS` puts it in its pose. The cell's corner of the Linesman's atlas is clear, the Linesman's stocks' pixels pack beside its own texture, and the Energy Cell's art is the owner's, unchanged. No other gun's atlas changed with the packing.
  - `python3 tools/generate_material_data.py`: wrote the three guns' and the Energy Cell's items, item models, recipes (each on the guns and machines switches), names, shot sounds, and the zap damage type with its tags.
  - `python3 tools/check_mod_data.py`: PASS (1916 material IDs), now checking `JugcraftGuns.CHARGE`, the arcs' numbers, the Energy Cell's capacity, `GunLooks.FLASH_TINTS` and the new names.
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only.
  - **Previews:**
    - first-person views of each gun idle, aimed and through its reload (approximating the game's hands), which placed the arms, and the Beam Pistol's sight and its 4 px of eye relief;
    - side views of the Beam Pistol's barrel breaking open, the Stormlock's latch, cylinder and lever, and the Linesman's cell going in (which moved the latch's hinge to its front);
    - each gun with its attachments.
- **Slice 8D game tests** (their CI run is below):
  - `EnergyGunsGameTests`:
    - `energyWeaponsRunOnCells`: the three load from the Energy Cell, a chargeable item, each round its charge, a magazine's worth within a cell; the Beam Pistol fires a beam, the others arcs; a zap pushes nothing back, counts each shot, and is neither a projectile nor fire.
    - `reloadsDrawChargeFromCells`: cells of 1,000 and 10,000 JE are 27 rounds; they fill an empty Beam Pistol and are left at 0 and 7,800 JE, still in their slots. On spent cells the Stormlock's reload does not start; a cell of 1,500 JE then loads two of its charges and is spent.
    - `beamPassesThroughCreatures`: the beam takes its damage from both pigs in its line and stops at a wall, the pig behind it untouched; the shot spends a round.
    - `arcLeapsBetweenCreatures`: the arc takes 9 from its mark, then 5.4 and 3.24 from the pigs two and three blocks on; the pig nine blocks away is untouched.
    - `linesmanFindsCreaturesInItsCone`: the Linesman's arc strikes the pig eight degrees off its aim; a pig out of its cone is untouched.
    - `chargingStationFillsACell`: a Charging Station fills an Energy Cell on its cradle.
  - "Every gun registered" now counts twenty-four guns.
  - `GunsClientGameTests` takes the three through every gun's steps with two full Energy Cells:
    - it checks that each shot is drawn (`GunTracePayload`);
    - it checks that the reload drew the spent rounds' charge from the cells and left both cells;
    - screenshots `jugcraft_guns_beam_pistol_*`, `jugcraft_guns_stormlock_rifle_*` and `jugcraft_guns_linesman_*`; the inventory shot holds a charged cell and a spent one.
- **Slice 8D in CI** ([run 37996214844](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37996214844), on 6d97724a3): every check passed.
  - `mod` and `optional integrations absent`: the build and the server game tests passed. There are 1186 now, 1180 before; the six `EnergyGunsGameTests` are the new ones.
  - `client` (shard 2): `GunsClientGameTests` passed. Its log:
    - **Beam Pistol:** aimed, the view narrowed to 0.9; fired, 9 flash frames and its beam drawn, the husk 801.2 → 795.2 and rounds 8 → 7; reloaded 8, its two cells left at 19,600 JE of 20,000 (one round's 400).
    - **Stormlock Rifle:** aimed, 0.8; fired, 10 flash frames and its arc drawn, the husk 795.2 → 786.2 and rounds 5 → 4; reloaded 5, 19,250 JE left (one round's 750).
    - **Linesman:** aimed, 0.95; fired, 10 flash frames and its arc drawn, the husk 786.2 → 782.2 and rounds 6 → 5; reloaded 6, 19,750 JE left (one round's 250).
  - **Screenshots:**
    - Held, each gun sits at the lower right, and the counter reads the shots in the cells: 50, 26 and 80 from two full cells.
    - Seen from outside, each shot's flash is the cyan-white of `FLASH_TINT`.
    - Mid-reload, the Stormlock's left hand comes up to the middle of the view as it loads a round, and the Linesman's holds the gun's side. Each gun is inspected as the owner's animations show it, and the counter then reads 49, 25 and 79 shots left in the cells.
    - The inventory shows a charged Energy Cell with its bar; the tooltip covers the rest of that row.
    - The beam and the arcs do not show in the fired shots, which are taken the moment the shot leaves; the client counted each one drawn.
    - **Mid-reload, the Beam Pistol's view was filled** edge to edge with one brown, the player skin's colour in shade, with the gun drawn in front: the camera was inside the left arm's box. The next run shows it gone (below).
    - **The Stormlock's fired shot** shows no gun, no crosshair and no icon in its hotbar slot, for that one frame. Its flash count (10 frames) and its other shots show it drawn. The next run did not show it again (below); its cause is not known.
    - **Aimed:** as with the other guns, the gun's back sits below the crosshair. The Beam Pistol's body rises over the middle of the target, and the Linesman's broad back fills the lower middle of the view; both are noted as known limits.
- **The next run** ([run 37999913579](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37999913579), on d0cd56890, with slice 8C's arm safeguard merged): every check passed. Its log gives the same damage, charge and traces as above.
  - Mid-reload, the Beam Pistol's view is clear: the scene, the gun and its right hand. The left arm, which had held the camera, is left out for those frames.
  - The Stormlock's fired frame shows the gun, the crosshair and its icon in the hotbar.
  - The Trench Lobber's left sleeve shows at the lower left of its fired frame, not over half the view.
- **Slice 9A, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts, their attachments' included, re-bakes to the owner's; each hand is where `BUILDS` puts it in its pose; every bone the owner's animations move exists.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and shot sounds; run again after the last change, it changed nothing more.
  - `python3 tools/check_mod_data.py`: PASS (1919 material IDs), with `check_guns` (the three guns' numbers, attachments, looks and eye relief in Java).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:**
    - first-person views of each gun idle, aimed, fired from the hip and aimed, mid-reload and mid-inspection (approximating the game's hands), which set the 2 px of eye relief;
    - side views of each reload: the Picket's and Ranger's magazines leaving and seating, the Ranger's handle lifted, drawn back and slammed home, and the Kestrel's bolt held back;
    - the nearest point of each gun to the eye through its aimed shot, at 0, 2 and 4 px of relief.
- **Slice 9A game tests (written; they run in CI):**
  - `MarksmanGunsGameTests`:
    - `marksmanRiflesAimTrue`: the three are registered with their numbers, fire one bullet a pull from Rifle Rounds, and, aimed, stray no more than the Longhorn Rifle and reach at least as far; each recipe loads; the Kestrel takes the three scopes, the Picket and the Ranger none.
    - `marksmanRiflesLandAndLoad`: a shot from each lands its damage on a pig and spends a round; the Ranger's magazine reload loads the round it lacked after its reload time and not before; the empty Kestrel is loaded with a clip of eight from the inventory after its own.
  - "Every gun registered" now counts twenty-seven guns.
  - `GunsClientGameTests` takes the three through every gun's steps: drawn, aimed, fired at the husk, reloaded, inspected, fitted with each attachment set it takes, and held in third person. Screenshots `jugcraft_guns_picket_rifle_*`, `jugcraft_guns_ranger_rifle_*` and `jugcraft_guns_kestrel_rifle_*`.
- **Slice 9A in CI** ([run 38031106724](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38031106724), on 0e7ea642a): every check passed.
  - `mod` and `optional integrations absent`: the build and the server game tests passed. There are 1188 now, 1186 before; the two `MarksmanGunsGameTests` are the new ones.
  - `client` (the guns' shard): `GunsClientGameTests` passed. Its log:
    - **Picket Rifle:** aimed, the view narrowed to 0.7; fired, 6 flash frames, the husk 784.8 → 776.8 and rounds 10 → 9; reloaded 10, 31 Rifle Rounds left; 3 casings thrown.
    - **Ranger Rifle:** aimed, 0.75; fired, 6 flash frames, the husk 776.8 → 766.8 and rounds 10 → 9; reloaded 10, 31 left; 3 casings.
    - **Kestrel Rifle:** aimed, 0.7; fired, 6 flash frames, the husk 766.8 → 757.8 and rounds 8 → 7; reloaded 8, 31 left; no casings, as its animations cue none.
  - **Screenshots:**
    - Held, each rifle sits at the lower right in the owner's textures, the Ranger's handle along the right of its top cover.
    - Aimed, the Picket's and Kestrel's peep sights and the Ranger's rear sight sit on the crosshair over the husk, the Ranger's handle up and to the right of it. As on the other rifles, the gun's back and the right fist fill the lower middle of the view.
    - Mid-reload, each rifle is rolled toward the left hand as the owner's animations show it, the Ranger's handle lifted at the top of the view. Each is inspected as the owner's animations show it; partway through, the right sleeve swings near the camera at the lower right.
    - Fitted, the counters read 1 / 15 with the Extended Magazine on the Picket and the Ranger, and the fitted rifles keep their sights on the crosshair aimed.
    - Seen from outside, each is raised along the look, and its flash shows at the muzzle when it fires.
- **Slice 9B, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts, their attachments' included, re-bakes to the owner's; each hand is where `BUILDS` puts it in its pose; every bone the owner's animations move exists.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and shot sounds, and the `clank` event.
  - `python3 tools/check_mod_data.py`: PASS (1922 material IDs), with `check_guns` (the three guns' numbers, attachments, looks, eye relief and the `clank` alias in Java).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:**
    - first-person views of each gun idle, aimed, fired from the hip and aimed, mid-reload and mid-inspection, and of the one-handed guns' reloads frame by frame; these set the eye relief and the left hands;
    - side views of the Squall's canister through its reload and of its gauge;
    - the nearest point of each gun to the eye through its aimed shot, at 0 to 6 px of relief.
- **Slice 9B game tests (written; they run in CI):**
  - `AutomaticGunsGameTests`:
    - `automaticWeaponsFireWhileHeld`: the three are registered with their numbers, fire one bullet at a time from Light Rounds, automatically; each recipe loads; the Rattler and the Squall take the three scopes, the Bronco none.
    - `automaticWeaponsLandAndLoad`: each fires two shots at once and a third one interval later, all landing on a pig and spending a round each; the empty Squall's canister reload loads forty rounds from the inventory after its reload time and not before.
  - "Every gun registered" now counts thirty guns.
  - `GunsClientGameTests` takes the three through every gun's steps; screenshots `jugcraft_guns_rattler_pistol_*`, `jugcraft_guns_bronco_smg_*` and `jugcraft_guns_squall_rifle_*`.
- **Slice 9B in CI** ([run 38032059311](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38032059311), on 6f41efbe8): every check passed.
  - `mod` and `optional integrations absent`: the build and the server game tests passed. There are 1190 now, 1188 before; the two `AutomaticGunsGameTests` are the new ones.
  - `client` (the guns' shard): `GunsClientGameTests` passed. Its log:
    - **Rattler Pistol:** aimed, the view narrowed to 0.9; fired, 7 flash frames, the husk 755.4 → 752.4 and rounds 20 → 19; reloaded 20, 31 Light Rounds left; 1 casing thrown.
    - **Bronco SMG:** aimed, 0.9; fired, 6 flash frames, the husk 752.4 → 748.9 and rounds 25 → 24; reloaded 25, 31 left; no casings, as its animations cue none.
    - **Squall Rifle:** aimed, 0.85; fired, 6 flash frames, the husk 748.9 → 746.4 and rounds 40 → 39; reloaded 40, 31 left; no casings, as its animations cue none.
  - **Screenshots:**
    - Held, each sits at the lower right in the owner's textures: the Rattler dark steel, the Bronco and the Squall copper-bright.
    - Aimed, each gun's back sits on the crosshair over the husk. As on the other one-handed guns, the Rattler's and the Bronco's right fist and forearm fill the lower middle of the view below it; so does the Squall's.
    - Mid-reload, the Rattler and the Bronco are lifted toward the upper right and the right forearm fills that side, as the previews showed. Partway through the inspection, the Bronco is turned toward the camera and fills the middle of the view for a moment.
    - Fitted, the counters read 1 / 30 with the Extended Magazine on the Rattler and 1 / 38 on the Bronco, and the fitted guns keep their backs on the crosshair aimed.
    - Seen from outside, each is raised along the look, and its flash shows at the muzzle when it fires.
- **Slice 9C, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts, the Spikedriver's magazines and stocks included, re-bakes to the owner's; each hand is where `BUILDS` puts it in its pose; every bone the owner's animations move exists.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and shot sounds.
  - `python3 tools/check_mod_data.py`: PASS (1925 material IDs), with `check_guns` (the three guns' numbers, shots, charge, attachments, looks, eye relief and the Seam Cutter's tilt in Java).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:**
    - first-person views of each gun idle, aimed, fired from the hip and aimed, mid-reload and mid-inspection;
    - side views of the Spikedriver's lever pull with the left hand, its magazine leaving, its shot and each attachment; of the Seam Cutter's core leaving, its carry handle and its arms; and of the Caisson Pistol's tank leaving and returning and its bolt drawn back;
    - the nearest point of the Spikedriver and the Caisson Pistol to the eye through their aimed shots, at 0, 2 and 4 px of relief (the Spikedriver also at 6);
    - the left arm's coverage of the view through the one-handed guns' reloads and inspections, against a search over its direction.
- **Slice 9C game tests (written; they run in CI):**
  - `SecondEnergyGunsGameTests`:
    - `secondEnergyWeaponsRunOnCells`: the three are registered with their numbers and load a magazine's charge from at most one Energy Cell; each recipe loads; the Spikedriver and the Seam Cutter fire beams, the Caisson Pistol an arc; only the Seam Cutter fires while the trigger is held; the Spikedriver takes both magazines and the three stocks, the other two nothing.
    - `spikedriverDrivesThroughItsLine`: a shot passes through two pigs, 12 to each, stops at a wall before a third, and spends a charge; with an Extended Magazine an empty Spikedriver loads nine charges, not before the magazine's longer reload is up, drawing 7,200 JE from a full cell and leaving the cell.
    - `seamCutterBurnsWhileHeld`: two shots at once and a third an interval later, each burning through both pigs in its line and spending a charge.
    - `caissonArcSeeksWider`: with a pig a block to the side six blocks ahead, the Stormlock's arc finds nothing; the Caisson Pistol's strikes that pig and leaps to the one beside it, which takes 60%, and leaves the pig far off to the side alone.
  - "Every gun registered" now counts thirty-three guns.
  - `GunsClientGameTests` takes the three through every gun's steps; screenshots `jugcraft_guns_spikedriver_*`, `jugcraft_guns_seam_cutter_*` and `jugcraft_guns_caisson_pistol_*`.
- **Slice 9C in CI** ([run 38034553601](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38034553601), on fdab3382d): every check passed.
  - **Server game tests** (`mod`, `optional integrations absent`): 1194 passed, 1190 before; the four new ones are `SecondEnergyGunsGameTests`.
  - **`GunsClientGameTests`** took the three guns through every step:
    - **Spikedriver:** aimed, the view narrowed to 0.85; fired, 6 flash frames and its beam drawn, the husk 737.98 → 725.98 and charges 6 → 5; reloaded 6, 19,200 JE left in its two cells; 1 burst of sparks.
    - **Seam Cutter:** aimed, 0.95; fired, 5 flash frames and its beam drawn, the husk 725.98 → 724.48 and charges 60 → 59; reloaded 60, 19,900 JE left; no sparks, as its animations cue none.
    - **Caisson Pistol:** aimed, 0.9; fired, 6 flash frames and its arc drawn, the husk 724.48 → 719.48 and charges 10 → 9; reloaded 10, 19,700 JE left; 3 bursts of sparks.
  - **Screenshots:**
    - Held, each sits at the lower right in the owner's textures: the Spikedriver dark steel with the rings standing up on its back, the Seam Cutter copper-bright at the hip with the left hand on its core, the Caisson Pistol with its tall tank upright. The counters show the shots left in the cells: 25, 200 and 66.
    - Aimed, the Spikedriver's and the Caisson Pistol's backs sit on the crosshair over the husk, the right fist and forearm below them (the aiming polish item); the Caisson's tank rises above the crosshair. The Seam Cutter, with no sights, stays at the hip in the narrowed view.
    - Mid-reload, the Spikedriver is lifted to the right with the left hand at its side and the Seam Cutter tipped up. The Caisson Pistol's left hand comes in large across the middle of the view, above the gun, as the tank is changed.
    - Mid-inspection, the Caisson Pistol's sleeve covers the right third of the view, as the previews showed.
    - Fitted, the Spikedriver's counter reads 1 / 9 with the Extended Magazine and 1 / 6 with the Speed Magazine and with a stock alone; aimed, it keeps its back on the crosshair.
    - Seen from outside, the Spikedriver and the Caisson Pistol are raised along the look and the Seam Cutter is carried low at the hip; each flashes at the muzzle when it fires.
- **Slice 9D, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts, their attachments' included, re-bakes to the owner's; each hand is where `BUILDS` puts it; every bone the owner's animations move exists; the Highwayman's shell is drawn into an empty corner of its atlas copy.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and shot sounds.
  - `python3 tools/check_mod_data.py`: PASS (1928 material IDs), with `check_guns` (the three guns' numbers, attachments, looks, eye relief and sound aliases in Java). `GunLooks.EYE_RELIEF` and `GunAnimations.GUN_SOUND_ALIASES` grew past `Map.of`'s ten pairs and are now `Map.ofEntries`; the check reads each gun's sound aliases as one map, as the Throttle has two.
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:**
    - first-person views of each gun idle, aimed, fired from the hip and aimed, mid-reload and mid-inspection;
    - side views of each gun with each of its attachments, of the Sledge's and the Throttle's reloads, of the Highwayman's shot working its bolt and of its shell carried in through its loop;
    - the nearest point of each gun to the eye through its aimed shot, at 0 to 6 px of relief, and which part it is (the top of the grip on all three).
- **Slice 9D game tests (written; they run in CI):**
  - `PumpGunsGameTests`:
    - `pumpShotgunsAreRegistered`: the three are registered with their numbers, fire eight pellets a shot from Buckshot Shells a pull at a time and load a shell at a time; each recipe loads; the Sledge hits hardest, the Highwayman holds the most, reaches furthest and aims steadiest, the Throttle fires quickest; the Highwayman and the Throttle take the scopes, the Sledge none; the Throttle takes only the stocks and scopes, the others the barrel attachments, the light grip and the bayonets too.
    - `pumpShotgunsLandAndLoad`: side by side, each fires its one shell at a pig three blocks off, the pellets landing together and the shell spent; then each loads its full tube from the inventory, the first shell after its opening and one shell's time, every shell after its whole reload, and no more than the tube holds.
    - `highwaymanKeepsItsPelletsTogether`: aimed at pigs thirteen blocks off, standing a block up so that no pellet strays into the floor first, all eight of the Highwayman's pellets land (its 2.5° cone is narrower than the pig there), and fewer of the Sledge's (6°).
    - `highwaymanAndGarrisonRifleCraftApart` (added 14 October 2026, with the Highwayman's second brass ingot): the Highwayman's grid gives the Highwayman and the Garrison Rifle's the Garrison Rifle; the Highwayman's old grid, the Garrison Rifle's mirrored, gives the Garrison Rifle.
  - "Every gun registered" now counts thirty-six guns.
  - `GunsClientGameTests` takes the three through every gun's steps; screenshots `jugcraft_guns_sledge_*`, `jugcraft_guns_highwayman_*` and `jugcraft_guns_throttle_*`.
- **Slice 9D in CI** ([run 38036998697](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38036998697), on 631cc3a84): every check passed.
  - **Server game tests** (`mod`, `optional integrations absent`): 1197 passed, 1194 before; the three new ones are `PumpGunsGameTests`.
  - **`GunsClientGameTests`** took the three guns through every step:
    - **Sledge:** aimed, the view narrowed to 0.9; fired, 8 flash frames, the husk 728.49 → 704.49 (6 of its 8 pellets) and shells 4 → 3; reloaded 4, 31 Buckshot Shells left; no hulls, as its animations cue none.
    - **Highwayman:** aimed, 0.8; fired, 7 flash frames, the husk 704.49 → 680.49 (all 8 pellets) and shells 7 → 6; reloaded 7; no hulls.
    - **Throttle:** aimed, 0.88; fired, 8 flash frames, the husk 680.49 → 659.49 (7 pellets) and shells 6 → 5; reloaded 6; 1 hull thrown.
  - **Screenshots:**
    - Held, each sits at the lower right in the owner's textures: the Sledge with its red fore-end, the Highwayman long and dark, the Throttle with its sight ring standing up.
    - Aimed, each gun's back sits on the crosshair over the husk, the right fist below it (the aiming polish item).
    - Mid-reload, the Sledge's left hand and sleeve fill the left half of the view across the crosshair as it pushes a shell in (the known limit above); the Highwayman's left hand comes in large at the right; the Throttle is tipped up to the right.
    - Fitted, the counters read 1 / 4, 1 / 7 and 1 / 6. Aimed with the Light or the Weighted Stock, the Sledge's and the Highwayman's stocks come up under the eye as a dark block across the bottom of the view, as the Weighted Stock does on the Riveter SMG and the Garrison Rifle (run 38034553601): an item for the aiming polish pull request.
    - Seen from outside, each is raised along the look, and its flash shows at the muzzle when it fires.
- **Slice 9E, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the ten guns' tactical grip parts and of the twelve guns' Laser Sights re-bakes to the owner's; every bone the guns had is as it was (the laser's pieces are packed after the scopes'); the dot's texture is the owner's red dot unchanged.
  - `python3 tools/generate_material_data.py`: wrote the two attachments' items, item models, recipes and names, and the dot's particle.
  - `python3 tools/check_mod_data.py`: PASS (1930 material IDs), with `check_guns` (the attachments' numbers, the guns that take them, and the Laser Sight's zoom and dot in Java).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:** each tactical grip on its gun from the side; the Laser Sight on the guns from the side and in first person, held and aimed; the Breacher's atlas with and without the beam, and the room left in it.
- **Slice 9E game tests (written; they run in CI):**
  - `TacticalAttachmentsGameTests`:
    - `tacticalAttachmentsFitTheirGuns`: both recipes load; the Tactical Grip fits the ten guns with the owner's tactical grip parts and no other, the Laser Sight every gun that takes the scopes but the Breacher and the Trench Lobber.
    - `tacticalAttachmentsSteadyTheGun`: on a Kestrel Rifle the Tactical Grip leaves 90% of the spread from the hip and 80% of the kick, and the Laser Sight beside it 70% of that spread again, the aimed spread as it was; a Long Scope takes the Laser Sight's place, leaving it in the grid; the Breacher takes no Laser Sight, nor the Patchwork Carbine a Tactical Grip.
  - "Every gun registered" now counts twenty attachments.
  - `GunsClientGameTests`:
    - a fourth set of attachments, the Tactical Grip and the Laser Sight, on each gun that takes either, held and aimed (screenshots `jugcraft_guns_<gun>_fitted_4*`); a set a gun takes none of is skipped;
    - the Laser Sight on the Longhorn Rifle: held, it draws its dots, the last where the gun points (logged, with its distance from the eye); aimed, the view narrows to 0.9 and its dot shows on the middle of the screen (screenshots `jugcraft_guns_laser_sight*`).
- **Slice 9E in CI** ([run 38038657220](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38038657220), on 815a60bb8): every check passed, the `optional integrations absent` job on its one re-run.
  - **Server game tests** (`mod`, `optional integrations absent`): 1199 passed, 1197 before; the two new ones are `TacticalAttachmentsGameTests`.
  - **The re-run:** the job's first attempt failed on one test this slice does not touch, `ArmsVIIIGameTests.javelinStrikesAndComesDown`. The javelin flew on its arc and came down three blocks past its pig, as it had once before on 9 October. The test passed in the same run's `mod` job and on the re-run. The pull request's comment has the details.
  - **`GunsClientGameTests`:**
    - Held on the Longhorn Rifle, the Laser Sight drew 20 dots in 20 ticks, the last 6.69 blocks from the eye, on the husk seven blocks off.
    - Aimed, the view narrowed to 0.9 and its dot was drawn (27 frames), with no view through a scope. The Long Scope, Medium Scope and Reflex Sight drew no laser dots.
    - The fourth set, the Tactical Grip and the Laser Sight, went on each of the 17 guns that take either, held and aimed.
  - **Screenshots:**
    - Held, the Laser Sight stands on each gun's top as a dark housing with its red front and blue light. The Tactical Grip is under the fore-end, behind the left hand.
    - Aimed, the eye looks along the laser's left side. Its housing fills the view just right of the crosshair and hides part of the husk (the known limit above).
- **Slice 9F, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS (nothing in it changed).
  - `python3 tools/check_mod_data.py`: PASS (1930 material IDs).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews** (first person, approximating the game's hands), set beside the CI screenshots of run 38036998697, which the full-size arms reproduced:
    - every gun with sights aimed, its arms at full size and at half size, and four of them at three fifths;
    - on four guns, the right arm left out instead, which showed the left arm as large;
    - on three guns, the arms pointed instead at a body's shoulders, at where the shoulders were at the hip, and straight down, all of which left the fist as large;
    - on three guns, 4, 8 and 12 pixels more eye relief;
    - the three stocks on each of the 22 guns that take them, aimed, drawn and left out;
    - the ease at 0, ¼, ½, ¾ and full aim;
    - the aimed shots of the Longhorn Rifle, Drover Rifle, Highwayman and Sledge, whose hands work a lever, a bolt and a pump.
- **Slice 9F game tests (written; they run in CI):** `GunsClientGameTests`:
  - each gun's arms are drawn at their full size held and at half size aimed, the Thresher's and the Seam Cutter's at full size both ways;
  - with each set of attachments that has a stock, the stock is drawn held and left out aimed (counted frames; logged).
- **Slice 9F in CI** ([run 38040183992](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38040183992), on 030e8230a): every check passed.
  - **Server game tests** (`mod`, `optional integrations absent`): 1199 passed, as before; the slice adds none.
  - **`GunsClientGameTests`**, the one client class the job ran (the only one this slice changes):
    - The 34 guns with sights drew their arms at size 1 held and 0.5 aimed; the Thresher and the Seam Cutter, which have none, at 1 both ways.
    - The 66 sets of attachments with a stock, on the 22 guns that take one: each stock was drawn in every frame held (left out in none) and left out in 37 to 44 frames aimed.
  - **Screenshots** (each gun's own aimed shot and all 115 aimed shots with attachments):
    - Aimed, each gun's back sits on the crosshair with its fist small below it, and no stock comes up under the eye. On the Linesman, the arm that filled the lower left of the view in slice 9E's run no longer shows.
    - The Stoker's and the Trench Lobber's own bodies still fill the lower middle aimed, as they did in slice 9E's run: they are the guns' parts, not the arms.
    - **Not explained:** the Riveter SMG with its second set (Extended Barrel, Speed Magazine, Weighted Stock), aimed (`0208_jugcraft_guns_riveter_smg_fitted_2_aimed`), shows both arms in a pose its other aimed shots do not: the right arm lies across the lower right of the view and the left arm stands beside the gun. The gun itself is where it is in the others, and its stock was left out (40 frames). In slice 9E's run the stock covered that part of the view. In the previews at half size, no frame of the Riveter's animations (idle, draw, shoot, aimed shot, reload, inspect), nor its model with none, puts the arms there with the gun at rest. The nearest are the inspect's arms about a second in, where the gun is turned. The test neither inspects nor reloads there.
- **Slice 9G, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS.
  - `python3 tools/generate_material_data.py`: wrote the Lobber's new tooltip, and the names of the reload's "No grenades to load." and the tooltip's "Loaded with" line.
  - `python3 tools/check_mod_data.py`: PASS (1930 material IDs), with both names required.
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 9G game tests (written; they run in CI):** `LobberGrenadesGameTests`:
  - `onlyTheLobberTakesGrenades`: of the guns, only the Lobber takes grenades. A new Lobber holds frag Grenades and records no kind; one recorded as chlorine holds chlorine; one recorded as an unknown item or as stone holds frag Grenades.
  - `lobberLoadsTheGrenadeInTheOtherHand`: an empty Lobber, frag Grenades in the inventory and three chlorine grenades in the other hand, loads the three chlorine grenades and leaves the frag Grenades.
  - `lobberSwapsOneKindForAnother`: a Lobber of two frag Grenades, smoke grenades in the other hand, puts the two back and loads six smoke grenades. Then, with one smoke grenade in it and only frag Grenades to hand, it loads those and puts the smoke grenade back.
  - `lobberKeepsToItsKind`: an empty Lobber that last held thermite grenades, frag Grenades ahead of thermite grenades in the inventory, loads thermite.
  - `lobberLobsTheKindItHolds`: a Lobber of chlorine grenades lobs a chlorine grenade of the shooter's at a pig seven blocks off; a chlorine cloud hangs where it lands, and it hurts the pig.
  - Slice 8C's `lobberLobsAGrenade` and `reloadsTakeAmmunitionByTheItem` still load and fire frag Grenades.
- **Slice 9G in CI** ([run 38042577642](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38042577642), on 48b38825d): every check passed.
  - **Server game tests** (`mod`, `optional integrations absent`): 1204 passed, 1199 before; the five new ones are `LobberGrenadesGameTests`, which compiled and passed in both jobs.
  - **`GunsClientGameTests`**, the one client class the job ran (it covers `GunsClient`, which this slice changes). The Trench Lobber's steps went as in slice 9F's run, with frag Grenades:
    - aimed, the view narrowed to 0.9; fired, 6 flash frames, and the husk 825.5 → 812.79 from the Grenade's burst, rounds 6 → 5;
    - reloaded, 6 rounds from the inventory, 31 Grenades left;
    - with its three sets of attachments, each stock was drawn held and left out aimed (35 or 36 frames).
  - **Screenshots:**
    - The counter's second line names the grenade the next reload would load: "32 Grenade" held, aimed and fired, "Reloading" mid-reload, "31 Grenade" after it, and "0 Grenade" with the attachment sets, whose test leaves no grenades in the inventory.
    - Fired aimed, the screenshot caught the left sleeve across the lower left corner of the view as the shot works the pump; in slice 9F's run it was small beside the gun. The frame differs from run to run, and this slice changes nothing about how the gun or the arms are drawn.
  - **Not covered by CI:** no client test loads a chemical grenade, so the counter and the tooltip naming one, and a chemical grenade lobbed from the Lobber, were not seen on a client (the server tests check the loading and the chlorine cloud).
- **Slice 10A, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS, before and after the rebase. Every face of the three guns' parts re-bakes to the owner's, the attachments' included; the Skylark Rifle's atlas is the first frame of the owner's two; the new props are drawn the right way round.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and tooltips, and their shots' sounds; after the rebase it changed nothing.
  - `python3 tools/check_mod_data.py`: PASS, 1933 material IDs on slice 9G's branch and 2152 after the rebase onto `main` (which holds much more since #277), with `check_guns` (the three guns' numbers, shots, rocket speeds and attachments in Java, and their looks and sounds on the client).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews** (first person, approximating the game's hands, and from the side):
    - each gun idle, aimed, fired, through each reload, draw and inspection;
    - the Earthmover with each scope, the Laser Sight (which runs into its drum), a grip and a bayonet; the Skylark Rifle and the Bullfrog with a stock and a grip, aimed with the stock left out;
    - the Skylark Rifle's rocket through its carry, and the Bullfrog's grenade through its reload;
    - the nearest point of each gun to the eye through its aimed shot: the Skylark Rifle's came to 0.78 px with no eye relief and 3.78 px with its 3 px, the Bullfrog's to 2.38 px;
    - the left arms' directions against others through the reloads (`armfit.py`, and by eye); the usual ones stayed.
- **Slice 10A game tests (written; they run in CI):**
  - `LaunchersGameTests`:
    - `launchersAreRegistered`: each is registered with its numbers and its recipe loads. The rocket guns fire High-Explosive Rockets, a rocket a round, the Skylark Rifle's faster and further and steadier aimed; the Earthmover holds four and the Skylark one. The Bullfrog lobs grenades, one a reload. None takes a barrel or magazine attachment; the Earthmover takes the scopes and the others the stocks.
    - `rocketsBurstOnWhatTheyHit`: side by side, the Earthmover and the Skylark Rifle each fire at a pig nine blocks off: a round spent, one rocket of the shooter's at the gun's speed, its fuse the range over the speed, not homing. Each pig is hurt; the shooters are not; the floor is whole; no rocket is left flying.
    - `rocketBurstsWhenItsFuseRunsOut`: a rocket held still over a pig, its fuse set to ten ticks (a later fuse set after it does not put it off), is whole with the pig at seven ticks and has burst, hurting the pig, by fourteen.
    - `bullfrogLobsAGrenade`: a Bullfrog shot spends its grenade and lobs one of the shooter's, which bursts on the pig seven blocks off and spares the shooter and the floor.
    - `earthmoverLoadsHighExplosiveRockets`: with only homing rockets an empty Earthmover does not start a reload; with six High-Explosive Rockets it loads four once its reload's time is up, not before, and leaves two.
  - "Every gun registered" now counts thirty-nine guns. Slice 9E's `tacticalAttachmentsFitTheirGuns` counts the launchers among the guns with the owner's tactical grip parts, and the Earthmover among those the Laser Sight does not fit. Slice 9G's `onlyTheLobberTakesGrenades` is now `onlyTheGrenadeGunsTakeGrenades`: the Lobber and the Bullfrog.
  - `GunsClientGameTests` takes the three new guns through its steps as it takes every gun: held, aimed, fired at the husk, reloaded from the inventory, inspected, with each set of attachments they take, seen from outside, and in the inventory.
- **Slice 10A in CI** ([run 38065020714](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38065020714), on 963f4451e merged into `main` at 4b36d2254): every check passed.
  - **Server game tests** (`mod`, `optional integrations absent`): 1283 passed in each. `main`'s own run of 4b36d2254 ran 1278, so the five more are `LaunchersGameTests`, which compiled and passed in both jobs. That run of `main` failed the javelin test (`javelin_strikes_and_comes_down`) in `optional integrations absent`; here it passed. It is not this slice's test.
  - **`GunsClientGameTests`** (the client job's first shard; it covers `GunsClient`, which this slice changes). Each of the three guns went through its steps:
    - its arms drawn at full size held and half size aimed;
    - aimed, the view narrowed to 0.85 (Earthmover), 0.8 (Skylark Rifle) and 0.92 (Bullfrog);
    - fired at the husk, each spending a round: the Earthmover's rocket burst took the husk from 663.8 to 644.51, the Skylark Rifle's from 644.51 to 625.14 and the Bullfrog's grenade from 625.14 to 612.19 (6, 7 and 6 flash frames);
    - reloaded from the inventory: four High-Explosive Rockets into the Earthmover and one into the Skylark Rifle, 31 left each time, and one Grenade into the Bullfrog, 31 left;
    - where their animations cue a casing, the Earthmover and the Skylark Rifle puffed smoke, once each; the Bullfrog's animations cue none;
    - with each set of attachments they take; the Skylark Rifle's and the Bullfrog's stocks were drawn held and left out aimed (36 or 37 frames).
  - **Screenshots** (the guns' own fifteen, the 22 with attachments and the six from outside):
    - **Earthmover:** held, the tube's back fills the right half of the view below the crosshair, the drum's front beside it; aimed, its sight stands on the husk at the crosshair and the tube still fills the right half; reloading, the launcher lies rolled over across the bottom right; inspected, its drum is turned toward the eye.
    - **Skylark Rifle:** held at the right of the view; aimed, the back of its receiver stands under the crosshair, its own wooden grip running down from it, and the husk shows above; reloading, the left hand is up by the opened gun with a grey steel shape in it, the rocket.
    - **Bullfrog:** held at the lower right, its leaf sight standing up; aimed, the leaf sight frames the husk on the crosshair; reloading, the left arm crosses the lower middle of the view as the barrel is racked.
    - **From outside:** the Earthmover is held on the shoulder, and fired, its backblast flares behind the tube as the flash shows at the muzzle; the Skylark Rifle and the Bullfrog are held at the chest.
    - **With attachments:** aimed, no fitted stock comes up under the eye.
- **Slice 10B, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts re-bakes to the owner's, the attachments' included; the Solenoid's and the Votive's glowing textures are the first frames of the owner's three; every other gun's files came out unchanged, its atlas included.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and tooltips, and their shots' sounds.
  - `python3 tools/check_mod_data.py`: PASS (2155 material IDs), with `check_guns` (the three guns' numbers, shots, charges and attachments in Java, and their looks, eye relief and sounds on the client).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews** (first person, approximating the game's hands, and from the side):
    - each gun idle, aimed, fired, through each reload, draw and inspection;
    - the Solenoid and Votive Rifles each with four sets of attachments, together every scope, the Laser Sight, each stock, both grips, two bayonets and both magazines;
    - the Glowmouth's charge through its loop, and its crank through the reload's end;
    - the nearest point of each gun to the eye through its aimed shot, with no eye relief and with theirs: the Solenoid's 0.93 px and 1.93 px, the Votive's -0.03 px and 1.97 px, the Glowmouth's -0.74 px and 2.26 px;
    - how much of the view the left arm covers through the Solenoid's reload, against three other rifles' (above), and the left arm's direction against others through it (`armfit.py`).
- **Slice 10B game tests (written; they run in CI):**
  - `CoilPlasmaGunsGameTests`:
    - `coilAndPlasmaRunOnCells`: each is registered with its numbers, loads a magazine's charge from one Energy Cell, and its recipe loads. The Solenoid and Votive Rifles fire beams and the Glowmouth an arc; only the Votive fires while the trigger is held, and only the Glowmouth is loaded a charge at a time. No other energy weapon reaches as far as the Solenoid. The two rifles take their fifteen attachments; the Glowmouth takes none.
    - `solenoidDrivesThroughItsLine`: a Solenoid shot takes 14 from each of two pigs in its line and stops at a wall, sparing the pig behind it, and spends a charge; emptied, it then loads five charges from a full cell after its reload's time, and the cell keeps the rest (4,500 JE).
    - `votiveRifleBurnsWhileHeld`: two shots at once and a third an interval later, each through both pigs in its line, spending three charges.
    - `glowmouthLoadsAChargeAtATime`: from a cell of 2,000 JE an empty Glowmouth holds one charge halfway through loading its second, then two, and leaves 500 JE.
    - `glowmouthArcLeapsFromWideOfItsAim`: its arc finds the pig a block off its aim six blocks out, strikes it for 10 and leaps for 6 to the pig two blocks beside it; the pig far off to the side is untouched.
  - "Every gun registered" now counts forty-two guns, and slice 9E's `tacticalAttachmentsFitTheirGuns` counts the Solenoid and Votive Rifles among the guns with the owner's tactical grip parts.
  - `GunsClientGameTests` takes the three new guns through its steps as it takes every gun: held, aimed, fired at the husk (each shot drawn, `GunTracePayload`), reloaded from Energy Cells, inspected, with each set of attachments they take, seen from outside, and in the inventory.
- **Slice 10B in CI, first run** ([run 38068295432](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38068295432), on 452140910): every check passed but one server test outside this slice.
  - **Server game tests:** 1287 in each job. This branch stands on slice 10A's, which stands on `main` from before https://github.com/jimbozoomer-byte/jugcraft/pull/298, so it holds 1277 of `main`'s tests, slice 10A's five and this slice's five.
    - All 1287 passed in `optional integrations absent`, `CoilPlasmaGunsGameTests` among them.
    - In `mod`, all but one passed. The one was the Arms VIII javelin test: "The pig at 1.50 2.00 7.50 is not struck yet (come down at 1.47 2.00 9.58 …)". It failed the same way on `main` at 4b36d2254. Its fix is https://github.com/jimbozoomer-byte/jugcraft/pull/303, and e5c264d25 ports that fix's `ThrownArm` change here.
  - **`GunsClientGameTests`** (the client job's first shard). Each of the three guns went through its steps:
    - its arms drawn at full size held and half size aimed;
    - aimed, the view narrowed to 0.75 (Solenoid Rifle), 0.85 (Votive Rifle) and 0.95 (Glowmouth);
    - fired at the husk, each drew its shot (a beam or an arc, and 8 flash frames) and spent a round. The Solenoid's beam took the husk from 606.44 to 592.44, the Votive's from 592.44 to 589.44 and the Glowmouth's arc from 589.44 to 579.44: 14, 3 and 10;
    - reloaded from two full Energy Cells (20,000 JE): the Solenoid's charge left 18,900 JE, the Votive's 19,800 and the Glowmouth's 19,250, so 1,100, 200 and 750 a charge;
    - where their animations cue a casing, an energy weapon vents sparks: through the shot and the reload, once from the Solenoid and three times each from the Votive and the Glowmouth;
    - with each set of attachments the two rifles take; their stocks were drawn held and left out aimed (41 to 47 frames).
  - **Screenshots** (the guns' own fifteen, the 16 with attachments and the six from outside):
    - **Solenoid Rifle:** held at the right of the view, the battery's glowing plate on its side. Aimed, its rear sight stands under the crosshair on the husk, the back of its stock below and the battery to the left. Reloading, the left arm comes up over the right half of the view (see the known limits). Inspected, the gun is turned up toward the eye, the left hand on it.
    - **Votive Rifle:** held at the right, its windows glowing green. Aimed, the dark back of its receiver, with a teal window, stands under the crosshair, the husk above. Reloading, it is lowered and turned at the bottom right, the left hand under it. Inspected, it is raised and turned to show its side.
    - **Glowmouth:** held at the lower right. Aimed, its back stands under the crosshair with the husk above, a green glowing part at each side. Reloading, it lies at the bottom right with its crank, the left hand at it. Inspected, it is tilted toward the eye.
    - **From outside:** each is held at the chest; fired, a flash shows at the muzzle.
    - **With attachments:** aimed, no fitted stock comes up under the eye. With the Laser Sight, aimed, its box stands just right of the crosshair, over the husk's side, as on every gun that takes it.
- **Slice 10B in CI, second run** ([run 38070317842](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38070317842), on e5c264d25, with the javelin fix ported): every check passed. All 1287 server game tests passed in each job, the javelin test's five throws among them, and the four client shards passed.
- **Slice 10C, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts re-bakes to the owner's, the Fowler's grips and bayonets included; every other gun's files came out unchanged, its atlas included.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and tooltips, and their shots' sounds.
  - `python3 tools/check_mod_data.py`: PASS (2158 material IDs), with `check_guns` (the three guns' numbers and attachments in Java, and their looks, eye relief, props and sounds on the client). Its check that the renderer knows every prop bone read only names without digits; it now reads the Fowler's `ball2`.
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews** (first person, approximating the game's hands, and from the side):
    - each gun idle, aimed, fired, through each reload, draw and inspection;
    - the Mule's barrels through the break, the Fowler's two balls and its ramrod down each barrel, and the Culverin's ball, ramrod and left arm through its reload;
    - the Fowler with its grips and bayonets, from the side and held, aimed, fired, reloading and inspected;
    - the nearest point of each gun to the eye through its aimed shot, with no eye relief and with theirs: the Mule's -0.53 px and 1.97 px, the Fowler's -0.82 px and 2.18 px, the Culverin's 1.95 px (it needs none);
    - the Culverin's left arm through its reload, for how much of the view it covers, against other directions that still run to the shoulder (`armfit_down.py`; see the known limits).
- **Slice 10C game tests (written; they run in CI):**
  - `DoubleBarrelGunsGameTests`:
    - `doubleBarrelsAreRegistered`: each is registered with its numbers, fires a spread of pellets a pull of the trigger at a time, loads all at once, and its recipe loads. The Mule holds two Buckshot Shells, the Fowler two Paper Cartridges and the Culverin one. The Mule loads fastest; the Fowler reaches furthest and takes longest to load; the Culverin's balls hit hardest and reach least far. The Fowler takes the grips and the bayonets; the Mule and the Culverin take nothing.
    - `doubleBarrelsFireBothBarrelsAndLoad`: side by side, each fires at a pig three blocks off: its pellets land together and a round is spent; the Mule and the Fowler then fire their second barrel. Each then loads from the inventory: halfway through its reload nothing is in yet, and as it ends every round is, two left in the inventory.
  - "Every gun registered" now counts forty-five guns.
  - `GunsClientGameTests` takes the three new guns through its steps as it takes every gun: held, aimed, fired at the husk, reloaded from the inventory, inspected, the Fowler with each set of attachments it takes, seen from outside, and in the inventory.
- **Slice 10C in CI** ([run 38074671680](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38074671680), on a81a06fa7): every check passed.
  - **Server game tests:** 1289 in each job, slice 10B's 1287 and this slice's two. All passed, `DoubleBarrelGunsGameTests` among them.
  - **`GunsClientGameTests`** (the client job's first shard). Each of the three guns went through its steps:
    - its arms drawn at full size held and half size aimed;
    - aimed, the view narrowed to 0.92 (Mule), 0.88 (Fowler) and 0.94 (Culverin);
    - fired at the husk, each spending a round: the Mule took it from 588.57 to 571.07 (seven of its ten pellets), the Fowler to 547.07 (all eight balls) and the Culverin to 527.07 (four of its five), with 6, 7 and 6 flash frames;
    - reloaded from the inventory: two Buckshot Shells into the Mule, two Paper Cartridges into the Fowler and one into the Culverin, 31 left each time;
    - where their animations cue a casing, the Mule threw spent shells (three through the shot and reload), the Fowler puffed smoke from its lock once and the Culverin three times;
    - the Fowler with each set of attachments it takes (the Light Grip, the Vertical Grip, the Iron Bayonet), held and aimed.
  - **Screenshots** (the guns' own fifteen, the Fowler's six with attachments and the six from outside):
    - **Mule:** held at the lower right, pointing at the husk. Aimed, the back of its frame stands under the crosshair on the husk, its wooden grip below. Reloading, it has sunk out of the view, the left hand coming up at the bottom (see the known limits). Inspected, the left arm fills the right of the view.
    - **Fowler:** held at the right. Aimed, the back of its barrels and the rib stand under the crosshair, the brass hammer to the right. Fired, a cloud of smoke wraps the gun's back and the priming flash shows above the lock. Reloading, the gun is tipped up and the left hand reaches over the muzzles. Inspected, it is turned to show its side.
    - **Culverin:** held in the right hand, pointing at the husk. Aimed, its breech stands under the crosshair. Fired, smoke and the flash. Reloading, the gun is tipped up and the left arm comes down from the top right with the ball. Inspected, it is turned to show its cock.
    - **With attachments:** the grips sit under the fore-end, behind the left hand, and the bayonet does not show from the hip or aimed, as on the Bellmouth and the Line Musket with theirs in the same run.
    - **From outside:** the Mule and the Fowler are held at the chest in both hands; the Culverin in the right hand, the left arm at the side. Fired, the Fowler and the Culverin wrap themselves in smoke.
- **Slice 10D, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every face of the three guns' parts re-bakes to the owner's, their attachments' included, less the tendrils' edges that have no area; every other gun's files came out unchanged, its atlas included.
  - `python3 tools/generate_material_data.py`: wrote the three guns' items, item models, recipes, names and tooltips, and their shots' sounds.
  - `python3 tools/check_mod_data.py`: PASS (2161 material IDs), with `check_guns` (the three guns' numbers and attachments in Java, and their looks, eye relief and sounds on the client).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews** (first person, approximating the game's hands, and from the side and front):
    - each gun idle, aimed (the arms at half size), fired, through each reload, draw and inspection;
    - each gun's bolt, tendrils and magazine through its shot and reload, and the Reverb's barrels turning;
    - the Undertone Rifle with each stock, scope and the Laser Sight; the Murmur SMG with both magazines, a scope and the Laser Sight; the Reverb with both grips, a scope and the Laser Sight;
    - the nearest point of each gun to the eye through its aimed shot, with no eye relief: the Undertone Rifle's 0.76 px, the Murmur SMG's and the Reverb's 1.61 px; with theirs, 2.76 px and 2.61 px.
- **Slice 10D game tests (written; they run in CI):**
  - `SculkGunsGameTests`:
    - `sculkGunsAreRegistered`: each is registered with its numbers, fires bullets, and its recipe loads. The Undertone Rifle fires one rifle round a pull from twelve; the Murmur SMG fires light rounds while the trigger is held, as often as any gun; the Reverb fires ten pellets a barrel from two buckshot shells loaded a shell at a time. Each takes its stocks, magazines or grips, the scopes and the Laser Sight.
    - `sculkGunsFireAndLoad`: side by side, each fires its last round at a pig three blocks off: the rifle's and the machine gun's shot takes one round's damage, the Reverb's pellets land together. Each then loads from the inventory: the magazines have nothing in halfway through their reload and are full as it ends; the Reverb has one shell in after its first shell's time and both after its reload; two rounds are left each time.
  - "Every gun registered" now counts forty-eight guns, and slice 9E's `tacticalAttachmentsFitTheirGuns` counts the Reverb among the guns with the owner's tactical grip parts.
  - `GunsClientGameTests` takes the three new guns through its steps as it takes every gun: held, aimed, fired at the husk, reloaded from the inventory, inspected, with each set of attachments they take, seen from outside, and in the inventory.
- **Slice 10D in CI** ([run 38076348750](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38076348750), on 2c33aa609: the slice with slice 10C's CI record merged in): every check passed.
  - **Server game tests:** 1291 in each job, slice 10C's 1289 and this slice's two. All passed, `SculkGunsGameTests` among them.
  - **`GunsClientGameTests`** (the client job's first shard). Each of the three guns went through its steps:
    - its arms drawn at full size held and half size aimed;
    - aimed, the view narrowed to 0.82 (Undertone Rifle), 0.9 (Murmur SMG) and 0.9 (Reverb);
    - fired at the husk, each spending a round: the Undertone Rifle took it from 523.29 to 514.29 (its 9), the Murmur SMG to 511.29 (its 3) and the Reverb to 483.29 (eight of its ten pellets, 28), with 8 flash frames each;
    - reloaded from the inventory: twelve Rifle Rounds into the Undertone Rifle, twenty-four Light Rounds into the Murmur SMG and two Buckshot Shells into the Reverb, 31 left each time;
    - where their animations cue a casing, the Undertone Rifle threw one through its shot and reload, the Murmur SMG three and the Reverb three;
    - with each set of attachments it takes, held and aimed: the Undertone Rifle's three stocks, each left out aimed (in 44, 44 and 43 frames, none held), and the Laser Sight; the Murmur SMG's Extended Magazine (its counter read 1 / 36), Speed Magazine and Laser Sight; the Reverb's Light Grip, and its Tactical Grip with the Laser Sight.
  - **Screenshots** (the guns' own fifteen, eighteen with attachments and six from outside):
    - **Undertone Rifle:** held at the lower right, pointing at the husk. Aimed, the back of its receiver stands under the crosshair, its peep ring just above around the husk's head, the tendrils fanned out to either side. Reloading, the gun is tipped and rolled, the left arm reaching up under it. Inspected, it is turned to show its glowing cells.
    - **Murmur SMG:** held at the right. Aimed, its back stands under the crosshair, the peep ring above at the husk's head. Reloading, the gun is tipped up, the left arm under it and its crystal glowing in its window. Inspected, it is turned to show its side.
    - **Reverb:** held at the right. Aimed, its back stands under the crosshair, the husk's head in the notch between its rear posts, thin tendrils to either side and the right hand below. Reloading, the gun is rolled to one side, its barrels turned aside, the left arm reaching up to it. Inspected, it is turned to show its side.
    - **With attachments:** aimed, no stock comes up under the eye. With the Laser Sight, aimed, its box stands just right of the crosshair, over the husk's side, as on every gun that takes it.
    - **From outside:** each is held at the chest in both hands; fired, a flash shows at its muzzle.
- **Slice 10E, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the Cell Rack's block models (lit and not), blockstates, item, loot table, recipe and names, and put it in the pickaxe's tag.
  - `python3 tools/check_mod_data.py`: PASS (2162 material IDs), with `check_guns` (the cradles in `CellRackBlock` and `CellRackRenderer` against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews** of the block model: from the front, from three-quarters on either side, from behind and from above, unlit and lit, with an Energy Cell standing in each cradle (its first frame, turned upright as the renderer turns it).
- **Slice 10E game tests (written; they run in CI):**
  - `CellRackGameTests`:
    - `cellRackIsRegistered`: the block, its item and its recipe; cables reach its buffer from all six sides; a point on its front picks the cradle nearest it, facing north, and turned to face south, east and west.
    - `cellRackChargesSeveralCellsAtOnce`: three spent cells beside a full one, the buffer full. After five ticks the three hold the same charge and the rack is lit; the full cell holds the same. Once they are full it is dark and its buffer holds 50,000 less 30,000 JE: what the cells took. A cell on its own then takes no more than 512 JE a tick.
    - `playersStandCellsInTheirCradles`: a player, through the server's block use, stands a cell in the upper right cradle, then the next beside it when that is full, a cell from the hand each time; with an empty hand, pointing at the lower left, they take the nearest cell, the upper middle one, into their inventory.
    - `hoppersLoadCellsAndTakeFullOnes`: a hopper above holding a stick and two cells stands the cells in the first two cradles and keeps the stick; a hopper below takes the full cell and leaves the half-charged one.
    - `cellRackKeepsAndDropsItsCells`: saved and loaded, it keeps its buffer and each cell's charge; broken, it drops itself and both cells, each with its charge.
- **Slice 10E in CI** ([run 38080528309](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38080528309), on 773951609: the slice, slice 10D's CI record merged in, and a fix to one of its tests): every check passed.
  - **Server game tests:** 1296 in each job, slice 10D's 1291 and this slice's five. All passed, `CellRackGameTests` among them.
  - **The first run** ([run 38079033583](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38079033583), on 2a8fc5985) compiled, and 1295 of the 1296 passed in each job. `hoppersLoadCellsAndTakeFullOnes` failed: it put two Energy Cells in one hopper slot, but cells stack to one, so the hopper held only one. 773951609 gives each cell its own slot, and has the hand test hold one cell at a time.
  - **Client game tests:** the changed files chose `GunsClientGameTests` and `PeepoCompanionClientTests`, and both passed. Neither has a step for the rack, so no screenshot shows it.
- **Slice 10F, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the gunner's name and its loot table.
  - The raider uniforms (`tools/raiders.py`): the gunner's drawn; the other three came out byte for byte as before.
  - `python3 tools/check_mod_data.py`: PASS (2162 material IDs), with `check_guns` (`MobGuns` against tools/guns.py, and each arm a gun that fires bullets) and `check_raiders` (the gunner's stats and the party table against tools/raiders.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Previews:** the four raider uniforms from the front, the gunner's beside the others.
- **Slice 10F game tests (written; they run in CI):**
  - `RaiderGunnerGameTests`:
    - `raiderGunnersAreRegistered`: the gunner is registered with its gunfire and carries one of the arms; a hundred gunners carry all three between them; at every raid level the gunners take the places of grunts, so the party is as large as before; and its loot table loads.
    - `raiderGunnerShootsWhatItHunts`: a gunner with a Garrison Rifle shoots the pig it hunts, nine blocks off; the pig loses whole hits of half the rifle's damage, and the gunner's magazine goes down.
    - `raiderGunnersSpareRaiders`: a gunner with a Sentry Pistol hits the pig past a grunt standing in the line of fire, and the grunt is not hurt.
    - `raiderGunnerReloadsWhenEmpty`: a gunner with a Breacher fires its six shells at the pig, then reloads an empty magazine.
    - `raiderGunnersGunDropsEmpty`: killed by a player (its drop chance made certain for the test), a gunner drops its Sentry Pistol with nothing loaded, and no rounds but up to three Light Rounds.
  - `raidIsWonWhenEveryRaiderFalls` (the raiders' test) counts a level 1 party of two grunts, a gunner, a grenadier and an officer.
  - `GunsClientGameTests`: three gunners, one with each arm, stand four blocks off facing the player with nothing in hand; the test checks that the gun hold posed them, and takes the screenshot `jugcraft_guns_raider_gunners`.
- **Slice 10F in CI** ([run 38080939471](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38080939471), on f3a8a3b7e: the slice, with slice 10E's test fix merged in): every check passed.
  - **Server game tests:** 1301 in each job, slice 10E's 1296 and this slice's five. All passed, `RaiderGunnerGameTests` among them, and the raid test with its new party.
  - **Client game tests:** the changed files chose `GunsClientGameTests`, `JugcraftClientGameTests`, `LairClientGameTests` and `SpindleLoftClientGameTests`, and all four passed. In `GunsClientGameTests` the three gunners were posed holding their guns for 324 frames.
  - **Screenshot** `jugcraft_guns_raider_gunners`: the three gunners stand before the brick wall facing the player, in grey-green greatcoats with the brass rounds across the chest, field caps and goggles. The left one holds the Sentry Pistol raised in one hand, the middle one the Garrison Rifle at the chest in both hands, the right one the Breacher in both hands, its long barrel running out toward the player.
- **Slice 10G, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. Every gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the one-handed guns' tooltip line.
  - `python3 tools/check_mod_data.py`: PASS (2162 material IDs), with `check_guns` (`JugcraftGuns.ONE_HANDED` and `DUAL_SPREAD` against tools/guns.py, and the tooltip's name).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 10G game tests (all passed in CI, below):**
  - `DualGunsGameTests`:
    - `oneHandedGunsPair`: the thirteen one-handed guns are guns; a Sentry Pistol is one-handed, a Garrison Rifle and a stick are not; a pistol in each hand is two guns at once, a rifle and a pistol are not, and nor is a pistol in the other hand alone.
    - `twoGunsAreNotAimed`: with a gun in each hand, using either hand's gun aims neither; with one pistol, it aims.
    - `bothGunsFireEachAtItsRate`: a Sentry Pistol and a Warden Pistol each fire two shots in one tick and are refused a third, every shot landing, each gun spending its own two rounds.
    - `otherHandFiresOnlyBesideAOneHandedGun`: a pistol in the other hand neither fires nor reloads beside a rifle, nor fires alone; beside a pistol it fires, landing its shot.
    - `oneGunReloadsAtATime`: while the main gun's magazine is out the other gun neither fires nor begins its reload; once the first is full the other reloads in its own time, both from the same Light Rounds.
    - `eitherGunsShotCutsAShellReloadShort`: a shot of the other gun cuts the Marshal Revolver's round-at-a-time reload short.
    - `otherGunsReloadStopsWithoutTwoGuns`: the other gun's reload stops when a stick takes the main gun's place; nothing is loaded or taken.
    - `otherHandsBeamPistolFires`: a Beam Pistol in the other hand fires its beam, landing its damage.
  - `GunsClientGameTests`, through the keys:
    - with a Sentry Pistol and a Warden Pistol, left click fires the first; right click, held, fires the second once and aims neither;
    - G pressed twice reloads the main gun, then the other;
    - right click held fires a Rattler Pistol in the other hand again and again;
    - seen from outside, both arms are raised;
    - screenshots: `jugcraft_guns_dual_held`, `_main_fired`, `_other_fired`, `_reloading`, `_third_person` and `_third_person_back`.
- **Slice 10G in CI** ([run 38083220287](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38083220287), on 0a5aa9392: the slice, with slice 10F's branch merged in): every check passed.
  - **Server game tests:** 1309 in each job, slice 10F's 1301 and this slice's eight. All passed, `DualGunsGameTests` among them.
  - **Client game tests:** the changed files chose `GunsClientGameTests` alone, and it passed. Its two-gun step logged:
    - left click, then right click held: rounds 8 to 7 and 12 to 11, 13 frames of flash, the husk from 45.5 to 36.5 health (the Sentry Pistol's 5 and the Warden Pistol's 4), and neither gun aimed;
    - G pressed twice: both guns full again (8 and 12), 30 of the 32 Light Rounds left;
    - right click held for 15 ticks with a Rattler Pistol in the other hand: 20 rounds to 14, six shots;
    - in third person, the player posed with a gun in each hand for 400 frames.
  - **Screenshots:**
    - `jugcraft_guns_dual_held`: a pistol in each hand at the bottom of the screen, the Warden Pistol on the left and the Sentry Pistol on the right, each in its own hand. Each gun's counter is on its own side of the hotbar: 12 / 12 on the left beyond the off-hand slot, 8 / 8 on the right.
    - `_main_fired`: the Sentry Pistol's flash, its counter at 7 / 8.
    - `_other_fired`: taken in the tick right click went down, before the shot, so the Warden Pistol's counter still reads 12 / 12. The counts logged above show the shot.
    - `_reloading`: the Sentry Pistol tipped in its reload, its counter reading "Reloading", the Warden Pistol's at 11 / 12.
    - `_third_person` and `_third_person_back`: the player from in front, both arms raised with a gun in each hand at the chest, and from behind.
- **Slice 11A, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations, textures and the Cinder Repeater's shot sound; every other gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2165 material IDs), with `check_guns` (the numbers, attachments, sound aliases and looks against tools/guns.py, and now `JugcraftGuns.INCENDIARY` and `GunShots.BURN_SECONDS`).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 11A game tests (all passed in CI, below):**
  - `NetherGunsGameTests`:
    - `netherGunsAreRegistered`: each is registered with its numbers, its recipe loads, and it fires bullets that set what they hit alight, as no other gun's do; the Tusker is a shotgun of eight pellets and six shells loaded a shell at a time, the Cinder Repeater an automatic of twenty light rounds, the Bastion Rifle a rifle of five rounds loaded a round at a time; each takes the stocks, grips, barrels, bayonets and scopes, the Cinder Repeater the magazines too.
    - `netherGunsFireAndLoad`: each fires its last round at a pig three blocks off, the Tusker's pellets landing together and the others' one shot taking one round's damage; then each loads from the inventory in its own time, a magazine at a time or a round at a time, two rounds left over.
    - `netherGunsSetWhatTheyHitAlight`: each Nether gun's shot sets its pig alight and the fire hurts it again within two seconds; an Undertone Rifle's shot leaves its pig unlit and hurts it no more; no block catches fire.
  - `GunsGameTests` counts 51 guns; `TacticalAttachmentsGameTests` the nineteen that take the Tactical Grip.
- **Slice 11A in CI** ([run 38086867951](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38086867951), on 38a4d0a99): every check passed.
  - **Server game tests:** 1312 in each job, slice 10G's 1309 and this slice's three. All passed, `NetherGunsGameTests` among them.
  - **Client game tests:** the changed files chose eight classes (`ArmorSetsClientGameTests`, `ArmorTiersClientGameTests`, `ArmsVIIClientGameTests`, `ArmsVIIIClientGameTests`, `BlueprintClientGameTests`, `GunsClientGameTests`, `KnightArmorClientGameTests` and `MaterialSetsClientGameTests`), and all eight passed. In `GunsClientGameTests` each new gun, in its turn:
    - held and aimed, its arms drawn at full size and then half, the view narrowed to 0.9 (Tusker), 0.88 (Cinder Repeater) and 0.8 (Bastion Rifle);
    - fired at the husk, each spending one round: the Tusker took it from 465.59 to 441.59 (all eight pellets, 24), the Cinder Repeater 3.5 and the Bastion Rifle 14, with 5 or 6 frames of flash;
    - reloaded from the inventory, 31 rounds left each time, throwing 1, 3 and 1 casings;
    - with each set of attachments it takes, held and aimed, the stocks left out aimed (in 34 or 35 frames, none held).
  - **The fire in play:** the husk lost 3 more health between the Tusker's shot and the Cinder Repeater's, and again before the Bastion Rifle's, as it did after the Stoker's burst; after the guns before them it lost none.
  - **Screenshots:**
    - **Held:** each at the lower right pointing at the husk, its name over the hotbar: the Tusker with the left hand on its pump, the Cinder Repeater with the left hand under it, the Bastion Rifle with its ladder sight standing up from the receiver.
    - **Aimed:** the Tusker's peep ring and the Cinder Repeater's ring on the husk over the crimson backs of their receivers; the Bastion Rifle's ladder over the husk, with its bolt, a gold-lit diamond, and the bolt's handle just under the crosshair.
    - **Reloading:** the Tusker tipped to take a shell and the Cinder Repeater rolled with its magazine out, the husk burning behind both. The Bastion Rifle's left arm comes up across the view to the breech with its round.
    - **Inspection, attachments and third person:** each turned over in the hand; each with its stocks, barrel devices, bayonet, Tactical Grip and Laser Sight; and held in both hands seen from outside, flashing as it fires.
- **Slice 11B, run locally (10 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2168 material IDs), with `check_guns` (the numbers, attachments, the one-handed guns, sound aliases, looks and `JugcraftGuns.INCENDIARY` against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 11B game tests (all passed in CI, below):**
  - `NetherGunsGameTests`, now for all six Nether guns:
    - `netherGunsAreRegistered`: each is registered with its numbers, its recipe loads, and it fires bullets that set what they hit alight, as no other gun's do. The Ashfall Pistol is a pistol held in one hand, of eight light rounds; the Goldbristle Carbine a carbine of twelve rifle rounds, one a pull; the Crackling a shotgun of six pellets and five shells loaded a shell at a time. The Goldbristle Carbine takes all that slice 11A's take, and the magazines; the Ashfall Pistol only the stocks, the scopes and the Laser Sight; the Crackling only the stocks, the grips and the bayonets.
    - `secondNetherGunsFireAndLoad`: as `netherGunsFireAndLoad`, for slice 11B's three.
    - `secondNetherGunsSetWhatTheyHitAlight`: as `netherGunsSetWhatTheyHitAlight`, for slice 11B's three, beside an Undertone Rifle.
  - `GunsGameTests` counts 54 guns; `DualGunsGameTests` the fourteen held in one hand; `TacticalAttachmentsGameTests` the twenty-one that take the Tactical Grip.
- **Slice 11B in CI** ([run 38092224778](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38092224778), on 4aba95d34): every check passed.
  - **Server game tests:** 1314 in each job, slice 11A's 1312 and this slice's two. All passed, `NetherGunsGameTests` among them.
  - **Client game tests:** the changed files chose the same eight classes as slice 11A's, and all eight passed. In `GunsClientGameTests` each new gun, in its turn:
    - held and aimed, its arms drawn at full size and then half, the view narrowed to 0.9 (Ashfall Pistol), 0.85 (Goldbristle Carbine) and 0.9 (Crackling);
    - fired at the husk, each spending one round, with 6 frames of flash: the Ashfall Pistol took it from 433.92 to 427.92 (6), the Goldbristle Carbine 7, and the Crackling 24 (all six pellets);
    - reloaded from the inventory, 31 rounds left each time; the Ashfall Pistol and the Goldbristle Carbine threw 3 casings each, the Crackling none, as its animations cue none;
    - with each set of attachments it takes, held and aimed, the stocks left out aimed (in 35 to 37 frames, none held).
  - **The fire in play:** the husk lost 3 more health after each Nether gun's shot before the next gun fired: after the Tusker's, the Cinder Repeater's, the Bastion Rifle's (the last gun in slice 11A's run, so its fire showed only now), the Ashfall Pistol's and the Goldbristle Carbine's. The Crackling fired last. After the Reverb's shot, before them, it lost none.
  - **Screenshots:**
    - **Held:** each at the lower right pointing at the husk, its name over the hotbar: the Ashfall Pistol in the right hand alone, the Goldbristle Carbine with its magazine out to the left and the left hand under it, the Crackling with the left hand under its pump.
    - **Aimed:** the Ashfall Pistol's notch and the Goldbristle Carbine's ring on the husk over the crimson backs of their receivers; the Crackling's posts on the husk over its narrow back and the right hand, as the Sledge's in the same run.
    - **Reloading:** the Ashfall Pistol and the Goldbristle Carbine tipped in their reloads, the husk burning behind both. The Crackling's left arm comes up across the middle of the view with its shell, as the Sledge's does in the same run.
    - **Inspection, attachments and third person:** each turned over in the hand; each with the stocks; the Goldbristle Carbine with its barrel devices, magazines, bayonet and grips, the Crackling with its grips and bayonet, the pistol and the carbine with the Laser Sight. Seen from outside, the Ashfall Pistol is raised in one hand and the others in both, each flashing as it fires.
- **Slice 12A, run locally (13 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2192 material IDs), with `check_guns` (the numbers, attachments, looks, tilts and `JugcraftGuns.LEVITATING` and `GunShots.LIFT_TICKS` against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 12A game tests (all passed in CI, below):**
  - `EndGunsGameTests`:
    - `endGunsAreRegistered`: each is registered with its numbers, its recipe loads, and it fires bullets that make what they hit float, as no other gun's do, and sets nothing alight. Each is an automatic held in both hands, loaded a magazine at a time: the Chitin SMG of twenty-four light rounds, the Hermit Rifle of twenty rifle rounds, the Borer of forty. The Chitin SMG takes the scopes and the Laser Sight, the Hermit Rifle the stocks too, the Borer nothing.
    - `endGunsFireAndLoad`: as `netherGunsFireAndLoad` (whose helpers it shares), for the three: each fires its last round at a pig and lands it, then loads from the inventory in its own time.
    - `endGunsMakeWhatTheyHitFloat`: each End gun's shot gives its pig Levitation, and three quarters of a second on it has risen off the floor; an Undertone Rifle's shot beside them leaves its pig standing. Once the spell has ended, each pig is back on the floor, unhurt by the fall.
  - `GunsGameTests` counts 57 guns.
- **Slice 12A in CI** ([run 38433095782](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38433095782), on b1f946f3b): every check passed.
  - **Server game tests:** 1382 in each job, the `mod` job's and the one without the optional integrations. All passed, `EndGunsGameTests` among them.
  - **Client game tests:** the changed files chose `GunsClientGameTests` alone, and it passed. In it each new gun, in its turn:
    - held and aimed, its arms drawn at full size and then half (the Borer's at full size both times, as a gun without sights), the view narrowed to 0.88 (Chitin SMG), 0.85 (Hermit Rifle) and 0.95 (Borer);
    - fired at the husk, each spending one round, with 5 frames of flash: the Chitin SMG took it from 390.47 to 387.47 (3), the Hermit Rifle 5 and the Borer 5.5;
    - reloaded from the inventory, 31 rounds left each time; the Chitin SMG and the Hermit Rifle threw 3 casings each, the Borer none, as its animations cue none;
    - with each set of attachments it takes, held and aimed: the Hermit Rifle with each stock, the stock left out aimed (in 33 frames, none held); both with the Laser Sight.
  - **After the shots:** the husk lost nothing more between the End guns' shots, so no fall hurt it. Between the Crackling's shot and the Chitin SMG's it lost 3, the Crackling's fire.
  - **Screenshots:**
    - **Held:** each at the lower right pointing at the husk, its name over the hotbar. The Chitin SMG and the Hermit Rifle show the ring on top and the shulker box and its cables under the receiver, the Hermit Rifle its curled shell at the muzzle. The Borer is at the hip, its crank out to the left and its pale barrel toward the husk.
    - **Aimed:** the Chitin SMG's and the Hermit Rifle's rings over the husk's head, the Hermit Rifle's shell either side. The Borer stays at the hip, the view narrowed a little.
    - **Reloading:** the Chitin SMG's and the Hermit Rifle's left arms come up over the right of the view (see the known limits); the Borer is pushed out and tipped, its box out.
    - **Inspection, attachments and third person:** each turned up toward the eye. The Laser Sight stands on the receiver of each, as it does on the Solenoid and Votive Rifles, the Ashfall Pistol and the Goldbristle Carbine in the same run. Seen from outside, the Chitin SMG and the Hermit Rifle are raised in both hands and the Borer carried low at the hip, each flashing as it fires.
- **Slice 12B, run locally (13 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2195 material IDs), with `check_guns` (the numbers, attachments, the one-handed guns, looks and eye relief against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 12B game tests (all passed in CI, below):**
  - `CopperGunsGameTests`:
    - `copperGunsAreRegistered`: each is registered with its numbers, its recipe loads, and it fires plain bullets, one a shot, loaded a magazine at a time. The Penny Pistol is a pistol held in one hand, of ten light rounds, and two of them are two guns at once; the Conductor Carbine a carbine of eight rifle rounds, one a pull; the Magpie SMG an automatic of thirty-two light rounds. The Penny Pistol takes the barrels' attachments and the stocks only; the other two those, the grips, the bayonets, the scopes and the Laser Sight, and the Conductor Carbine the magazines too.
    - `copperGunsFireAndLoad`: as `netherGunsFireAndLoad` (whose helpers it shares), for the three.
    - `pennyPistolsFireFromBothHands`: a Penny Pistol in each hand fires from each and lands both shots, each spending a round of its own.
  - `GunsGameTests` counts 60 guns; `DualGunsGameTests` the fifteen held in one hand; `TacticalAttachmentsGameTests` the twenty-three that take the Tactical Grip.
- **Slice 12B in CI** ([run 38457083489](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38457083489), on 3f4ac9b4e): every check passed.
  - **Server game tests:** 1385 in each job, the `mod` job's and the one without the optional integrations. All passed, `CopperGunsGameTests` among them.
  - **Client game tests:** the changed files chose `GunsClientGameTests` alone, and it passed. In it each new gun, in its turn:
    - held and aimed, its arms drawn at full size and then half, the view narrowed to 0.9 (Penny Pistol), 0.82 (Conductor Carbine) and 0.88 (Magpie SMG);
    - fired at the husk, each spending one round, with 7, 6 and 6 frames of flash: the Penny Pistol took it from 344.77 to 340.77 (4), the Conductor Carbine 8 and the Magpie SMG 2.5;
    - reloaded from the inventory, 31 rounds left each time; the Penny Pistol threw 3 casings, the other two none, as their animations cue none;
    - with each set of attachments it takes, held and aimed: each with each stock, the stock left out aimed (in 34 to 36 frames, none held); the Conductor Carbine's extended magazine holding 12; the long guns with the Tactical Grip and the Laser Sight.
  - **Between the shots:** the husk lost nothing between the Borer's shot and the Penny Pistol's, or between the copper guns'.
  - **Screenshots:**
    - **Held:** each at the lower right pointing at the husk, its name over the hotbar. The Penny Pistol's hand wraps most of it (see the known limits), its long magazine standing out to the left; the Conductor Carbine shows its slide and front post; the Magpie SMG its long magazine along the left of the view.
    - **Aimed:** each raised to the middle of the view, the husk's head over it. The Penny Pistol's and the Magpie SMG's magazines stand out to the left.
    - **Reloading:** the Penny Pistol's magazine drawn out of its side; the Conductor Carbine lowered, the left hand below it; the Magpie SMG tipped, its magazine out and the left hand over it at the left of the view.
    - **Inspection:** each turned toward the eye to be looked over.
    - **Attachments:** each barrel part at the muzzle and each stock behind the grip. The Laser Sight stands on the receiver aimed as it does on the Riveter SMG, the Garrison, Squall and Hermit Rifles and the Chitin SMG in the same run.
    - **Third person:** each raised toward the camera, the Penny Pistol in one hand, each flashing as it fires.
- **Slice 12C, run locally (13 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2198 material IDs), with `check_guns` (the numbers, attachments, the one-handed guns, looks and eye relief against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 12C game tests (all passed in CI, below):**
  - `MixedGunsGameTests`:
    - `mixedGunsAreRegistered`: each is registered with its numbers, its recipe loads, and it fires plain bullets, one a pull. The Tuppence Pistol is a pistol of eight light rounds and the Goshawk Revolver a revolver of six rifle rounds, each held in one hand and loaded a round at a time, and the two in the two hands are two guns at once; the Lament Rifle a rifle of five rifle rounds, loaded a magazine at a time, hitting harder and reaching further than the Goshawk Revolver. The Tuppence Pistol takes the stocks, the scopes and the Laser Sight; the Lament Rifle those, the grips and the bayonets; the Goshawk Revolver nothing.
    - `mixedGunsFireAndLoad`: as `netherGunsFireAndLoad` (whose helpers it shares), for the three: the two hand guns with one round in after their first round's time.
    - `tuppenceAndGoshawkFireFromBothHands`: a Tuppence Pistol in the main hand and a Goshawk Revolver in the other each fire and land their shots, each spending a round of its own.
  - `GunsGameTests` counts 63 guns; `DualGunsGameTests` the seventeen held in one hand.
- **Slice 12C in CI** ([run 38468187979](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38468187979), on 53c96a3fd): every check passed.
  - **Server game tests:** 1388 in each job, the `mod` job's and the one without the optional integrations. All passed, `MixedGunsGameTests` among them.
  - **Client game tests:** the changed files chose `GunsClientGameTests` alone, and it passed. In it each new gun, in its turn:
    - held and aimed, its arms drawn at full size and then half, the view narrowed to 0.9 (Tuppence Pistol), 0.9 (Goshawk Revolver) and 0.7 (Lament Rifle);
    - fired at the husk, each spending one round, with 10 frames of flash: the Tuppence Pistol took it from 354.88 to 349.88 (5), the Goshawk Revolver 9 and the Lament Rifle 16;
    - reloaded from the inventory, 31 rounds left each time; over its shot and reload the Tuppence Pistol threw 1 casing and the Goshawk Revolver 3, the Lament Rifle none, as its animations cue none;
    - with each set of attachments it takes, held and aimed: each with each stock, the stock left out aimed (in 52 or 53 frames, none held); the Lament Rifle with the grips and a bayonet; both with the Laser Sight.
  - **Between the shots:** the husk lost nothing between the Magpie SMG's shot and the Tuppence Pistol's, or between the three guns'.
  - **Screenshots:**
    - **Held:** each at the lower right pointing at the husk, its name over the hotbar: the Tuppence Pistol's barrel over its tube; the Goshawk Revolver's long white barrel; the Lament Rifle's barrel hung with sculk, its echo-shard window glowing.
    - **Aimed:** each raised to the middle of the view, the husk's head over it. As the Penny Pistol's did in slice 12B's run, the two hand guns' backs rise over the crosshair (the first-person previews draw them lower).
    - **Fired:** the Tuppence Pistol's receiver block, driven back, fills the middle of the view for a moment; the Goshawk Revolver kicks up.
    - **Reloading and inspection:** the Tuppence Pistol rolled; the Goshawk Revolver rolled, the left hand in; the Lament Rifle turned up; each turned toward the eye to be looked over.
    - **Attachments:** each stock behind the grip; the Laser Sight stands on the receiver aimed as it does on the other guns in the same run.
    - **Third person:** the two hand guns held out in one hand and the rifle across the body, each flashing as it fires.
- **Slice 13A, run locally (14 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2201 material IDs), with `check_guns` (the numbers, attachments, the one-handed guns, looks and eye relief against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 13A game tests (all passed in CI, below):**
  - `FrontierGunsGameTests`:
    - `frontierGunsAreRegistered`: each is registered with its numbers, its recipe loads, and it fires plain bullets, one a pull. The Tabby Revolver is a revolver of six light rounds, held in one hand and loaded a round at a time, and two of them are two guns at once; the Lookout Rifle a rifle of eight rifle rounds loaded a round at a time; the Ammonite Carbine a carbine of sixteen rifle rounds, loaded a magazine at a time; the Lookout Rifle reaches furthest and the Tabby Revolver least. The Tabby Revolver takes the barrel attachments only; the Lookout Rifle the stocks, the Light and Tactical Grips, the bayonets, the scopes and the Laser Sight; the Ammonite Carbine the stocks only.
    - `frontierGunsFireAndLoad`: as `netherGunsFireAndLoad` (whose helpers it shares), for the three: the revolver and the lever rifle with one round in after their first round's time.
    - `tabbyRevolversFireFromBothHands`: a Tabby Revolver in each hand fires from each and lands both shots, each spending a round of its own.
  - `GunsGameTests` counts 66 guns; `DualGunsGameTests` the eighteen held in one hand; `TacticalAttachmentsGameTests` the twenty-four that take the Tactical Grip.
- **Slice 13A in CI** ([run 38489671371](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38489671371), on bbe529f28): every check passed.
  - **Server game tests:** 1391 in each job, the `mod` job's and the one without the optional integrations. All passed, `FrontierGunsGameTests` among them.
  - **Client game tests:** the changed files chose eight classes over five shards, and all passed. `GunsClientGameTests` ran alone in its shard; in it each new gun, in its turn:
    - held and aimed, its arms drawn at full size and then half, the view narrowed to 0.9 (Tabby Revolver), 0.8 (Lookout Rifle) and 0.85 (Ammonite Carbine);
    - fired at the husk, each spending one round, with 6 frames of flash: the Tabby Revolver took it from 321.94 to 315.94 (6), the Lookout Rifle 7 and the Ammonite Carbine 5;
    - reloaded from the inventory, 31 rounds left each time; over its shot and reload the Tabby Revolver threw 4 casings, the Lookout Rifle 3 and the Ammonite Carbine 1;
    - with each set of attachments it takes, held and aimed: the Tabby Revolver with the Silencer, the Extended Barrel and the Muzzle Brake; the two long guns with each stock, the stock left out aimed (in 34 or 35 frames, none held); the Lookout Rifle with the Light Grip, a bayonet, and the Tactical Grip and the Laser Sight.
  - **Between the shots:** the husk lost nothing between the Lament Rifle's shot and the Tabby Revolver's, or between the three guns'.
  - **Screenshots:**
    - **Held:** each at the lower right pointing at the husk, its name over the hotbar: the Tabby Revolver's slotted barrel; the Lookout Rifle's barrel over its fore-end, the left hand at the front; the Ammonite Carbine's copper-banded barrels.
    - **Aimed:** each raised to the middle of the view. The Lookout Rifle's and the Ammonite Carbine's sights meet the crosshair under the husk's head. As the other hand guns' did, the Tabby Revolver's cylinder rises over the crosshair and covers the husk's body.
    - **Fired:** the Ammonite Carbine's cocking piece, driven back, covers the husk's body for a moment.
    - **Reloading and inspection:** the Tabby Revolver rolled, the left hand in; the Lookout Rifle rolled, the left hand over it; the Ammonite Carbine tipped; each turned toward the eye to be looked over, the Ammonite Carbine filling the right of the view.
    - **Attachments:** each barrel part at the Tabby Revolver's muzzle; each stock behind the grip; the Laser Sight stands on the receiver aimed as it does on the Longhorn and Drover Rifles in the same run. The bayonet is hidden behind the left hand.
    - **Third person:** the Tabby Revolver held out in one hand and the long guns raised, each flashing as it fires.
- **The Lookout Rifle's loading sound, run locally (14 October 2026):** it had played the magazine-in sound for each round, where the other lever rifles play the shell-in sound; it now plays theirs. `python3 tools/guns.py` PASS, `python3 tools/check_mod_data.py` PASS, `python3 scripts/check_repository.py` PASS; Java syntax parse only.
- **Slice 13B, run locally (14 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2564 material IDs), with `check_guns` (the numbers, attachments, the one-handed guns, looks and eye relief against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
- **Slice 13B game tests (all passed in CI, below):**
  - `FrontierGunsIIGameTests`:
    - `frontierGunsIIAreRegistered`: each is registered with its numbers, its recipe loads, it fires plain bullets one pull a shot, held in both hands, and it takes the grips and the bayonets. The Fencepost Carbine is a carbine of seven rifle rounds and the Waystation Shotgun a shotgun of eight shells, eight pellets a shot, each loaded a round at a time; the Highball Rifle a single-shot rifle, hitting hardest and reaching furthest of the three, the shotgun least. The barrel attachments fit the shotgun and the rifle, not the carbine; the scopes and the Laser Sight the rifle alone.
    - `frontierGunsIIFireAndLoad`: as `netherGunsFireAndLoad` (whose helpers it shares), for the three: the carbine and the shotgun with one round in after their first round's time, the rifle with none halfway through its reload.
  - `GunsGameTests` counts 69 guns; `TacticalAttachmentsGameTests` the twenty-seven that take the Tactical Grip.
- **Slice 13B in CI** ([run 38500777704](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38500777704), on d1f20f7fe): every check passed.
  - **Server game tests:** 1588 in each job, the `mod` job's and the one without the optional integrations. All passed, `FrontierGunsIIGameTests` among them.
  - **Client game tests:** all five shards passed. `GunsClientGameTests` ran alone in its shard, taking each new gun through holding, aiming, firing at the husk, reloading from the inventory, inspection and its attachments. Its checks passed for each: the arms' sizes, the narrowed view, the hit and the round spent, the reload, and the casings its animations cue. The job's text log was too large to read here, so its numbers are not quoted.
  - **Screenshots:**
    - **Held:** each at the lower right pointing at the husk, its name over the hotbar.
    - **Aimed and fired:** each raised to the middle of the view. The Fencepost Carbine's and the Highball Rifle's sights meet the crosshair on the husk's chest. The Waystation Shotgun's sights meet it at the husk's head, with the right hand over its legs.
    - **Reloading:** the Waystation Shotgun and the Highball Rifle rolled, each hand at its work. The Fencepost Carbine's right arm covered the right half of the view, with the gun out of sight behind it. It was chosen again after this run ([above](#slice-13b-more-frontier-guns)).
    - **Inspection:** each turned toward the eye to be looked over.
    - **Attachments:** each barrel part at the muzzle of the Waystation Shotgun and the Highball Rifle; the grips under the fore-end; the Laser Sight on the Highball Rifle as on the Longhorn, Kestrel, Lookout and Lament Rifles in the same run.
    - **Third person:** each raised, flashing as it fires.
- **The Fencepost Carbine's right arm chosen again, run locally (14 October 2026):** `python3 tools/guns.py` PASS, `python3 tools/check_mod_data.py` PASS, `python3 scripts/check_repository.py` PASS. Its first-person previews were drawn with the game's near-eye rule at every hundredth of a second of the reload.
- **Slice 13B on 643c6fe3b in CI** ([run 38506772653](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38506772653); the new arm and the Waystation Shotgun's own recipe): every check passed.
  - **Server game tests:** 1588 in the `mod` job, all passed; the job without the optional integrations passed too.
  - **Client game tests:** all five shards passed, `GunsClientGameTests` among them.
  - **The Fencepost Carbine's reload screenshot:** the gun rolled with its lid open, the right arm out of the view.
- **The Fencepost Carbine's and the Waystation Shotgun's loading sound, run locally (14 October 2026):** each had played the magazine-in sound for each round or shell, where the other lever guns play the shell-in sound; each now plays theirs. `python3 tools/guns.py` PASS, `python3 tools/check_mod_data.py` PASS, `python3 scripts/check_repository.py` PASS; Java syntax parse only.
- **Slice 13C, run locally (14 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2567 material IDs), with `check_guns` (the numbers, attachments, the one-handed guns, looks and eye relief against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Recipes:** every shaped recipe in the mod's data compared, mirrors included: none of the three guns' matches another.
- **Slice 13C game tests (all passed in CI, below):**
  - `FrontierGunsIIIGameTests`:
    - `frontierGunsIIIAreRegistered`: each is registered with its numbers, its recipe loads, and it fires plain bullets one pull a shot, held in both hands. The Flatboat Gun is a single shot of sixteen pellets of buckshot and takes the scopes and the Laser Sight only; the Windlass Musket a repeater of seven paper cartridges loaded a magazine at a time, taking the Light and Vertical Grips and the bayonets but no scope; the Millstone Rifle a rifle of eight rifle rounds loaded a round at a time, hitting harder and working slower than the Drover Rifle and taking what it takes. The rifle reaches furthest and the punt gun least.
    - `frontierGunsIIIFireAndLoad`: as `netherGunsFireAndLoad` (whose helpers it shares), for the three: the punt gun and the musket with nothing in halfway through their reloads, the rifle with one round in after its first round's time.
  - `GunsGameTests` counts 72 guns; `TacticalAttachmentsGameTests` the twenty-eight that take the Tactical Grip.
- **Slice 13C in CI** ([run 38511296312](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38511296312), on 43088608d): every check passed.
  - **Server game tests:** 1598 in the `mod` job, all passed, `FrontierGunsIIIGameTests` among them; the job without the optional integrations passed too.
  - **Client game tests:** all five shards passed. `GunsClientGameTests` took each new gun through holding, aiming, firing at the husk, reloading from the inventory, inspection and its attachments, and its checks passed for each. The job's text log was too large to read here, so its numbers are not quoted.
  - **Screenshots:**
    - **Held:** each at the lower right pointing at the husk, its name over the hotbar; the Windlass Musket's magazine tube standing out to the left across the view's lower middle.
    - **Aimed:** each raised to the middle of the view, its sight on the crosshair at the husk's chest.
    - **Fired:** the Windlass Musket flashes at its pan, to the right of its drum, and smokes.
    - **Reloading:** the Flatboat Gun tipped with its chamber out to the left; the Windlass Musket lowered, the left hand drawing the magazine away; the Millstone Rifle tipped, the left hand below it.
    - **Inspection:** each turned toward the eye to be looked over, the Flatboat Gun and the Windlass Musket filling a side of the view.
    - **Attachments:** the Laser Sight on top of the Flatboat Gun and the Millstone Rifle, as on the Highball Rifle; the Windlass Musket's grips and bayonet hidden behind the left hand, as the Lookout Rifle's bayonet is.
    - **Third person:** the Flatboat Gun's long barrel held out and flashing as it fires; the Windlass Musket and the Millstone Rifle raised, smoking as they fire.
- **Slice 13D, run locally (14 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged (an attachment's own `"mounts"` bone changes none of theirs).
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2660 material IDs), with `check_guns` (the numbers, attachments, the one-handed guns and now the carbines, looks, eye relief and the sound aliases against tools/guns.py). Its first run caught the two new sound aliases missing from `GunAnimations`; they were added.
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files. The GeckoLib calls the carbine idle and the cylinder cue use (`AnimationTest.getDataOrDefault`, `DataTickets.ANIMATABLE_INSTANCE_ID`) were read in GeckoLib 5.5.7's source.
  - **Recipes:** every shaped recipe in the mod's data compared, mirrors included. The Thumper's first grid was the Marshal Revolver's; with a brass ingot more under it, none of the three guns' matches another.
- **Slice 13D game tests (all passed in CI, below):**
  - `FrontierGunsIVGameTests`:
    - `frontierGunsIVAreRegistered`: each is registered with its numbers, its recipe loads, and it is one pull a shot. The Fencepost Thumper lobs grenades, four loaded one at a time, held in both hands, and takes the grips and the bayonets; the Parley Revolver fires rifle rounds, six loaded one at a time, held in one hand and a carbine with a stock, and takes the barrel attachments and the stocks; the Lantern Musket fires a beam on Energy Cells, three charges a magazine, held in both hands, reaching further than every energy weapon but the Solenoid Rifle, and takes nothing.
    - `parleyRevolverIsACarbineWithAStock`: two bare Parley Revolvers are two guns at once and the other hand's fires; with a Wooden Stock on one, it is a carbine, the two are not two guns at once, the other hand's does not fire, and the stock steadies it; without it again, it is one-handed.
    - `fencepostThumperLoadsAGrenadeAtATime`: an empty Thumper with three Grenades holds one halfway through the second's time and all three when its reload ends, none left over; a shot then spends one and lobs a Grenade of the shooter's, which bursts on the pig seven blocks off and leaves the floor whole.
    - `lanternMusketDrivesThroughItsLine`: a shot takes its damage from both pigs in its line and stops at the wall, spending a charge; an empty one loads nothing before its reload's time and its three charges after, the cell keeping the rest.
  - `GunsGameTests` counts 75 guns; `DualGunsGameTests` the nineteen held in one hand; `LobberGrenadesGameTests` the three grenade guns, the Thumper among them.
- **Slice 13D in CI** ([run 38634736533](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38634736533), on d00700826): every check passed after one re-run.
  - **Server game tests:** 1653 in the `mod` job, all passed, `FrontierGunsIVGameTests` among them.
  - **The job without the optional integrations failed once** on `SpodumeneGameTests.prospectAndDrill`, a test this slice does not touch, and passed on its one re-run. It reads only the drill's chunk, as the fluorite test does (slice 13C's record): in about one random placement in 64 both its ores fall across a chunk border from the drill ([the comment](https://github.com/jimbozoomer-byte/jugcraft/pull/373#issuecomment-6178187056)).
  - **Client game tests:** all five shards passed. `GunsClientGameTests` took each new gun through holding, aiming, firing at the husk, reloading from the inventory, inspection and its attachments, and its checks passed for each.
  - **Screenshots** (from the last 2,500 lines of the guns shard's log; the whole log was too large to read here):
    - **Held:** each at the lower right pointing at the husk, the Parley Revolver small in the right hand alone.
    - **Aimed:** each raised to the middle of the view, its sight on the crosshair at the husk.
    - **Reloading:** the Thumper's and the Lantern Musket's left arm comes across the middle of the view as it loads, the owner's animations' reach; the Parley Revolver is rolled over, the right arm across the lower middle.
    - **Inspection:** each turned to be looked over, the Parley Revolver raised to the upper right over the arm.
    - **The carbine:** with each stock fitted (with a silencer, an extended barrel or a muzzle brake), the Parley Revolver's left hand comes up under its barrel; without a stock it does not.
    - **Attachments:** the Thumper's grips and bayonet are hidden behind the left hand, as the Windlass Musket's are.
    - **Third person:** each raised and flashing as it fires.
- **Slice 14A, run locally (14 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged (the Long Scope's `"not_on"` names only the Holler Pistol). Its first run stopped on the Holler Pistol: its own texture is a whole 128 px atlas, and the four sights' pieces did not fit beside it; without the Long Scope's they do.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2663 material IDs), with `check_guns` (the numbers, attachments, the one-handed guns and the carbines, looks, eye relief and the sound aliases against tools/guns.py).
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Recipes:** every shaped recipe in the mod's data compared, mirrors included: none of the three guns' grids matches another.
- **Slice 14A game tests (all passed in CI, below):**
  - `BrassSidearmsGameTests`:
    - `brassSidearmsAreRegistered`: each is registered with its numbers, its recipe loads, and it is one pull a shot, firing bullets. The Holler Pistol fires rifle rounds, ten a magazine, held in both hands, harder than any other pistol, and takes every attachment the owner made for it and every sight but the Long Scope; the Minuet Pistol fires light rounds, ten a magazine, held in one hand, and aimed strays less than any other gun held in one hand, and takes the barrel attachments, the Extended Magazine and the four sights; the Half-Crown Revolver fires rifle rounds, six loaded one at a time, held in one hand and a carbine with a stock, and takes what the Parley Revolver takes.
    - `oneHandedSidearmsPairUntilStocked`: two Minuet Pistols are two guns at once and the other hand's fires; a bare Half-Crown Revolver pairs with a Minuet Pistol; with a Weighted Stock on it, it is a carbine, the two are not two guns at once, the other hand's does not fire, and the stock steadies its aim; without it again, it is one-handed.
    - `halfCrownRevolverLoadsARoundAtATime`: an empty Half-Crown Revolver with three Rifle Rounds holds one halfway through the second's time and all three when its reload ends, none left over; a shot then spends one and takes its damage from the pig.
    - `hollerPistolChangesItsMagazine`: an empty Holler Pistol with fifteen Rifle Rounds loads nothing before its reload's time and ten after it, leaving five; a shot then takes its damage from the pig.
  - `GunsGameTests` counts 78 guns; `DualGunsGameTests` the twenty-one held in one hand; `TacticalAttachmentsGameTests` the twenty-nine guns the Tactical Grip fits, the Holler Pistol among them.
- **Slice 14A's first CI run** ([run 38651258803](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38651258803), on 5e60bf941), in the job without the optional integrations, failed two server tests:
  - `TacticalAttachmentsGameTests.tacticalAttachmentsFitTheirGuns`: "The holler_pistol takes the Tactical Grip". Its list of the guns the grip fits did not name the Holler Pistol, which the owner made a tactical grip for; it does now.
  - `FluoriteGameTests.prospectAndDrill`: "Prospector missed fluorite", the chance failure https://github.com/jimbozoomer-byte/jugcraft/pull/376 fixes (its survey read only the drill's chunk). That fix is carried here too, line for line, for the fluorite and spodumene tests.
- **Slice 14A in CI** ([run 38652346741](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38652346741), on f4cd9232e, with both fixes): every check passed, on its first run.
  - **Server game tests:** 1657 in the `mod` job, all passed, `BrassSidearmsGameTests` and `TacticalAttachmentsGameTests` among them.
  - **The job without the optional integrations** and **the repository check:** passed.
  - **Client game tests:** all five shards passed. `GunsClientGameTests` took each new gun through holding, aiming, firing at the husk, reloading from the inventory, inspection and its attachments, and its checks passed for each.
  - **Screenshots** (from the last 2,500 lines of the guns shard's log; the whole log was too large to read here):
    - **Held:** each at the lower right pointing at the husk, the Minuet Pistol and the Half-Crown Revolver in the right hand alone.
    - **Aimed:** each raised to the middle of the view, its sight on the crosshair at the husk.
    - **Reloading:** the Holler Pistol is turned up and its left arm comes across the lower left as it draws the magazine, the owner's animation's reach; the Minuet Pistol is rolled over at the lower right; the Half-Crown Revolver's barrel is tipped down, raising the cylinder.
    - **Inspection:** each turned to be looked over at the right.
    - **Attachments:** the stocks, scopes and Laser Sight sit on each gun where they should; with a stock fitted, the Half-Crown Revolver's left hand comes up under its barrel.
    - **Third person:** each raised and flashing as it fires.
- **Slice 14B, run locally (14 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged. Its new `part_source()` reads a part entry `howler/stan_mag` from the Howler's folder; the Holler Carbine's atlas holds its own texture and the Howler magazine's pieces.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2666 material IDs), with `check_guns`.
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Recipes:** every shaped recipe in the mod's data compared, mirrors included: none of the three guns' grids matches another. (This branch still carries the Highwayman's old grid, which matches the Garrison Rifle's mirrored; https://github.com/jimbozoomer-byte/jugcraft/pull/377 fixes that on `main`.)
- **Slice 14B game tests (all passed in CI, below):**
  - `BrassCarbinesGameTests`:
    - `brassCarbinesAreRegistered`: each is registered with its numbers, its recipe loads, and it is one pull a shot, firing bullets from a magazine, held in both hands. Each conversion fires its pistol's rounds, as many a magazine, reaches further and strays less aimed; the Holler Carbine fires quicker than the Holler Pistol and hits less hard, and the Minuet Carbine hits harder than the Minuet Pistol. The Holler Carbine takes nothing; the Minuet Carbine takes the stocks and the four sights; the Grange Carbine fires rifle rounds, twelve a magazine, and takes all nineteen attachments the owner made for it.
    - `grangeCarbineChangesItsMagazine`: an empty Grange Carbine with twenty Rifle Rounds loads nothing before its reload's time and twelve after it, leaving eight; a shot then takes its damage from the pig.
    - `minuetCarbineLoadsLightRounds`: an empty Minuet Carbine with ten Light Rounds and ten Rifle Rounds loads the Light Rounds and leaves the Rifle Rounds; a shot then takes its damage from the pig.
  - `GunsGameTests` counts 81 guns; `TacticalAttachmentsGameTests` the thirty guns the Tactical Grip fits, the Grange Carbine among them.
- **Slice 14B in CI** ([run 38657383418](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38657383418), on d0001d964): every check passed, on its first run.
  - **Server game tests:** 1660 in the `mod` job, all passed, `BrassCarbinesGameTests` and `TacticalAttachmentsGameTests` among them.
  - **The job without the optional integrations** and **the repository check:** passed.
  - **Client game tests:** all five shards passed. `GunsClientGameTests` took each new gun through holding, aiming, firing at the husk, reloading from the inventory, inspection and its attachments, and its checks passed for each.
  - **Screenshots** (from the last 2,500 lines of the guns shard's log; the whole log was too large to read here):
    - **Held:** each at the lower right pointing at the husk, in both hands.
    - **Aimed:** each raised to the middle of the view, its sight on the crosshair at the husk.
    - **Reloading:** the Holler Carbine is turned up and its left arm comes across the lower left as it draws the magazine, the owner's animation's reach, as the Holler Pistol's; the Minuet and Grange Carbines are rolled over at the lower right.
    - **Inspection:** each turned to be looked over at the right.
    - **Attachments:** the Minuet Carbine's stocks and Laser Sight and the Grange Carbine's stocks and sights sit where they should. (The Holler Carbine takes none.)
    - **Third person:** each raised and flashing as it fires.
- **Slice 14C, run locally (15 October 2026):**
  - `python3 tools/guns.py`: PASS. It wrote the three guns' models, animations and textures (their shots are sounds already copied); every other gun's files came out unchanged (the Long Scope's `"not_on"` now names the Freeholder Rifle too). Its first run stopped on the Freeholder Rifle: its own texture is a whole 128 px atlas, and the four sights' pieces did not fit beside it; without the Long Scope's they do. `--check` passed after the last run.
  - `python3 tools/generate_material_data.py`: wrote the guns' item models, definitions, recipes, names and sounds.
  - `python3 tools/check_mod_data.py`: PASS (2669 material IDs), with `check_guns`.
  - `python3 scripts/check_repository.py`: PASS.
  - **Java:** a syntax parse only: 0 errors in the changed files.
  - **Recipes:** every shaped recipe in the mod's data compared, mirrors included: none of the three guns' grids matches another.
- **Slice 14C game tests (all passed in CI, below):**
  - `BrassQuickFirersGameTests`:
    - `brassQuickFirersAreRegistered`: each is registered with its numbers, its recipe loads, and it fires bullets from a magazine, held in both hands. The Sprocket Gun fires rifle rounds as long as the trigger is held, forty a pan, and takes all seventeen attachments the owner made for it, the four sights among them; the Navvy Shotgun fires six pellets a shell as long as the trigger is held, eight shells a magazine, the only automatic gun that fires shells, and takes its fifteen and no scope; the Freeholder Rifle fires rifle rounds a pull at a time, twenty a magazine, quicker than any other gun that fires rifle rounds a pull at a time, and takes both magazines and every sight but the Long Scope.
    - `sprocketGunChangesItsPan`: an empty Sprocket Gun with fifty Rifle Rounds loads nothing before its reload's time and forty after it, leaving ten; a shot then takes its damage from the pig.
    - `navvyShotgunChangesItsMagazine`: an empty Navvy Shotgun with twelve Buckshot Shells loads eight, leaving four; a shot then spends one, and at close range its pellets land together on the pig.
  - `GunsGameTests` counts 84 guns; `TacticalAttachmentsGameTests` the thirty-two guns the Tactical Grip fits, the Sprocket Gun and the Navvy Shotgun among them.
- **Slice 14C in CI** ([run 38659879209](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38659879209), on e786153dd): every check passed, on its first run.
  - **Server game tests:** 1663 in the `mod` job, all passed, `BrassQuickFirersGameTests` and `TacticalAttachmentsGameTests` among them.
  - **The job without the optional integrations** and **the repository check:** passed.
  - **Client game tests:** all five shards passed. `GunsClientGameTests` took each new gun through holding, aiming, firing at the husk, reloading from the inventory, inspection and its attachments, and its checks passed for each.
  - **Screenshots** (from the last 2,500 lines of the guns shard's log; the whole log was too large to read here):
    - **Held:** each at the lower right pointing at the husk, in both hands.
    - **Aimed:** each raised to the middle of the view. The Sprocket Gun's pan lies level across the crosshair at the husk; the Navvy Shotgun's butt fills the lower middle, the husk's head between its two rear posts at the crosshair; the Freeholder Rifle's sight is on the crosshair at the husk.
    - **Reloading:** the Sprocket Gun is tipped up at the lower middle, its top to the view; the Navvy Shotgun and the Freeholder Rifle are rolled over at the lower right.
    - **Inspection:** each turned to be looked over at the right.
    - **Attachments:** the Laser Sight sits on the Sprocket Gun's pan and on the Freeholder Rifle, and stands to the right of the crosshair aimed, as on the earlier guns; the Navvy Shotgun's bayonet sits under its muzzle; the Extended Magazine shows thirty rounds on the Freeholder Rifle and twelve shells on the Navvy Shotgun. At the hip the muzzle attachments point away from the view and the stocks are behind it, so they show little.
    - **Third person:** each raised and flashing as it fires.
- **Not run:** the client by hand, a two-client dedicated server, and play.

## World and event applicability
No worldgen, structures, bosses or seasonal content. Guns, rounds and the Cell Rack come from crafting (and the creative tab). The one loot is slice 10F's: raider gunners, in raids and raider camps, may drop their service arms (their equipment's drop chance) and rounds (the mob loot table `jugcraft:entities/raider_gunner`).

## Rollout and open questions
- **Names:** proposed here (Rust Midge, Patchwork Carbine, Thunderpipe, Warden Pistol, Riveter SMG, Haymaker, Longhorn Rifle, Drover Rifle, Coach Gun, Duelling Pistol, Line Musket, Bellmouth, Bulldog Pistol, Marshal Revolver, Sapper Revolver, Sentry Pistol, Garrison Rifle, Breacher, Trench Lobber, Thresher, Stoker, Beam Pistol, Stormlock Rifle, Linesman, Picket Rifle, Ranger Rifle, Kestrel Rifle, Rattler Pistol, Bronco SMG, Squall Rifle, Spikedriver, Seam Cutter, Caisson Pistol, Sledge, Highwayman, Throttle, Earthmover, Skylark Rifle, Bullfrog, Solenoid Rifle, Votive Rifle, Glowmouth, Mule, Fowler, Culverin, Undertone Rifle, Murmur SMG, Reverb, Tusker, Cinder Repeater, Bastion Rifle, Ashfall Pistol, Goldbristle Carbine, Crackling, Chitin SMG, Hermit Rifle, Borer, Penny Pistol, Conductor Carbine, Magpie SMG, Tuppence Pistol, Goshawk Revolver, Lament Rifle, Tabby Revolver, Lookout Rifle, Ammonite Carbine, Fencepost Carbine, Waystation Shotgun, Highball Rifle, Flatboat Gun, Windlass Musket, Millstone Rifle, Fencepost Thumper, Parley Revolver, Lantern Musket, Holler Pistol, Minuet Pistol, Half-Crown Revolver, Holler Carbine, Minuet Carbine, Grange Carbine, Sprocket Gun, Navvy Shotgun, Freeholder Rifle, Light Round, Rifle Round, Buckshot Shell, Paper Cartridge, Energy Cell, Cell Rack, Raider Gunner; the attachments keep plain names: Silencer, Baffled Silencer, Muzzle Brake, Extended Barrel, Extended Magazine, Speed Magazine, Light Stock, Weighted Stock, Wooden Stock, Light Grip, Vertical Grip, the four bayonets, Long Scope, Medium Scope, Reflex Sight, Tactical Grip, Laser Sight). The owner may rename them before release; IDs are stable only after release.
- **The arms:** placed from the animations' own evidence, without the models they were made for. The CI screenshots show where they sit; the rest points and arm directions above are the knobs.
- **The gun sets the owner chose on 8 October 2026** are all built: the revolvers (slice 8), the service arms (slice 8B), the heavy weapons (slice 8C) and the energy weapons (slice 8D).
- **The gun sets the owner chose on 10 October 2026** are all built, each its own pull request: the marksman rifles (slice 9A), the automatic weapons (slice 9B), the second energy weapons (slice 9C) and the pump shotguns (slice 9D).
- **The second round the owner chose on 10 October 2026,** each its own pull request: the launchers (slice 10A), the coil and plasma guns (slice 10B), the double-barrels (slice 10C) and the sculk guns (slice 10D); a rack that charges several Energy Cells at once (slice 10E), enemies with guns (slice 10F) and pistols in both hands (slice 10G). The three follow-ups of the first round are slices 9E, 9F and 9G.
- **Aimed, the gun hand covered the sights** in the CI screenshots from 8 October, and a fitted stock came up under the eye: slice 9F draws the arms at half size aimed and leaves a fitted stock out ([above](#slice-9f-the-aiming-polish)).
- **Not yet:**
  - the jam the Gnat's sound suggests;
  - other mobs with guns: the owner's Scorched Guns 2-style gun mobs, once their models are in the library (slice 10F gives the raider gunners the service arms);
  - the Whizzbanger, which waits for the model its animations were made for ([above](#the-whizzbanger-waits));
  - the Fencer Thumper, which waits for its cylinder to turn with each shot ([above](#the-fencer-thumper-waits));
  - the guns beyond these sets.
- **Balance:** the numbers are starting points for the owner to set.
