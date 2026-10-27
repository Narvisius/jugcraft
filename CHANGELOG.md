# Changelog

Every change to Jugcraft, newest first, grouped by pull request. For what each thing does in game, see [docs/TECH_TREE.md](docs/TECH_TREE.md). For a full inventory with APIs and file locations, see [docs/WHAT_EXISTS.md](docs/WHAT_EXISTS.md).

**Testing:** automated server and rendered-client game tests run in CI. See the exact feature/integration commit for results; human two-client playtesting and release readiness remain unverified.

## Unreleased

No numbered release yet. Entries on feature branches remain proposed until their pull requests merge.

### Unmerged: Herobrine (encounters, marks, shoreline pyramids, schedule)
- **Herobrine:** a still, staring figure with glowing white eyes. He appears to a player 28–52 blocks off (14–28 underground), where they could see him if they turned their head. He is gone when anyone comes within 12 blocks, when they stare him down for 2 seconds, or when he is hit. Now and then he stands right behind them, with a heartbeat only they hear and 3 seconds of Darkness, and is gone once they turn and see him. The apparition never directly damages players or blocks, speaks, wears a name tag or is saved; the separate mark builder leaves the structures described below. Record: [herobrine.md](docs/features/herobrine.md).
- **Four ways to be seen at a distance:** a watcher on a ridge, in a clearing or just inside a cave mouth in a hillside (placed so a group can share the sighting); a follower who walks the player's own path, freezes while watched, and whose faint footsteps stop when they stop; a second look that finds him gone or moved; and eyes at the far end of a tunnel behind an underground player.
- **He leaves marks, and they can be taken back:** after a sighting the player really saw, sometimes (35% on the surface, 75% underground) a mark where he stood: a redstone-torch shrine, a little mossy or sand pyramid, a leafless grove (only leaves the world grew), a straight 2×2 tunnel (natural stone and soil only, stopping short of ore and water) or a lone sign, each with a cryptic message. Marks go only onto wild-looking land: loaded, unclaimed, outside the town, nothing plainly built and no block entity within 6 blocks. Planning reads no block until every chunk it could read is loaded. That is a heuristic, not proof, so groves and tunnels (the marks that take blocks away) fail closed: they happen only inside land an operator approved with `/jugcraft herobrine wilds approve <radius>`. Shrines, pyramids and signs only add, into air or plain ground cover. They are built a few blocks a tick, each block rechecked so a mark never overwrites a player's work, and every change is saved so `/jugcraft herobrine marks undo <id>` restores only blocks still as he left them. `herobrine.marks=off` stops new ones.
- **Shoreline pyramids and a trail to them:** rarely, a hollow stepped brick pyramid, 31 across and 16 high, rises on level wild land by the water. It has a landward doorway, twelve windows, and a chamber with an altar, signs and a lectern holding one surveyor's journal: nothing valuable, nothing that refills. A sighting across water may begin a trail: a bare grove, then a torch-lit tunnel whose sign gives the way and distance to the nearest finished pyramid. Each step counts only once its mark is finished, and a pyramid stopped part-built is never pointed to. Pyramids are built bottom up through the same rechecked, undoable work. Undo now restores only blocks still exactly as he left them, every property and contents included (a sign's both sides, style, dye, glow and wax), with a short list of changes the world makes on its own.
- **Sky islands and surface ruins:** rare floating landmarks, each a rock of weathered stone carrying a haunted building with rooms and a way through: a ruined castle, a manor in a dead forest, a hanging monastery whose two cells sit on islands joined by chained bridges with a shrine hung beneath, a fortress on an upside-down mountain, a castle shattered into five drifting pieces, and a watching keep. They are lit only by a few soul lanterns and candles. Each goes only into a sky that is loaded, unclaimed, outside the town and empty, with 6 blocks of margin. It must be at least 24 blocks above anything below, well under the top of the world, and 1024 blocks from another. On the ground he may leave a watching hut whose window faces you, an unfinished house like a village house, or a ritual circle in a clearing. Everything he builds persists through approach and the end of the event; you can explore, mine and change it. Explicit operator undo preserves player edits. Record: [herobrine-sky.md](docs/features/herobrine-sky.md).
- **Underground and underwater discoveries:** nine cave layouts include torch trails, shelters, watching rooms, sealed stairs, pillar grids, broken bridges, balconies, shrines and empty halls. Four source-water glowstone patterns add rings, crosses, pyramids and trails. A depth-aware exploration director attempts discoveries more often underground, with vertical spacing for different cave levels; quiet-season attempts are six times farther apart. Structures persist, claims and towns are protected, and excavation requires approved wilderness. Combined validation passed 70 native tests, 393 scheduler assertions and two rendered galleries covering all 22 new variants. Record: [herobrine-depths.md](docs/features/herobrine-depths.md).
- **Jugcraft notices him:** town guards turn to look at him for a moment and never target him. Styxhexenhammer, to anyone who has seen him, makes it plain he knows what they saw and will not explain it. A quiet Cursed Mirror reflection (`HerobrineEncounters.reflection`) waits for the mirror's branch to call it.
- **Only real sightings count:** a witness must have had him on screen, unobstructed, while attentive (no menu, not idle, asleep or in danger), and anyone who really saw him counts. A player's first encounter is never a close one, and each player has one encounter at a time.
- **The schedule:** OpenAI Codex/Astra's `HerobrineScheduleDirector`, by the owner's plan, implements the `HerobrineDirector` seam and is installed at server start. It keeps high activity through 3 November inclusive and is rarer from 4 November. Operators also have `/jugcraft herobrine status|sighting|scare`, and `herobrine.haunting=off` stops scheduled encounters.
- **Checked:** all 40 Herobrine game tests and 393 scheduler assertions pass natively, and automated rendered and two-client tests pass. CI and a human playtest have not run yet.
- **His look:** the game's own default player texture, read at run time, under a full-bright eyes layer of four white pixels. No game asset is copied.

### Unmerged: Keep your inventory in boss fights
- **Dying in a lair costs nothing:** as with the keepInventory rule, but only in lairs (every boss fight). No items or experience fall; you respawn outside with every slot as it was (armour and offhand worn), the same held slot and all your experience. Dying anywhere else is unchanged. No grave is ever placed in a lair; Grave Goods still catch anything another mod drops there. Record: [lair-keep-inventory.md](docs/features/lair-keep-inventory.md).

### Unmerged: Arcane Concordance, Chunk D (pact hall)
- **A killed Gathering Shade keeps its holder's things:** what it carried goes into its Spirit Anchor once (or drops where it fell, even with block drops turned off, if the anchor is not loaded or keeps someone else's agreement), it leaves its holder's roster, and the anchor says it was lost. Its holder's next use of the anchor calls another shade under the same agreement and Bound Will, at no cost; a full roster refuses and says so. A shade called again while the old one was only unloaded makes the old one, when it loads, leave its load in the anchor once and depart. Record: [arcane-concordance-workers.md](docs/features/arcane-concordance-workers.md).
- **A seal says why it failed:** not yours or your party's (or not where you may build), already kept, 32 Bound Wills, or a full roster (counted before anything is made); nothing is recorded, spent or called. A newly sealed shade is on the roster at once; one replaced, or loading while switched off, takes no room on it.
- **Spirit Anchors have owners:** a new anchor records who placed it, and only they or their party may seal at it, where they may build; an anchor placed before keeps its old rule plus that check; a stand-in never seals, suspends or ends one, and placing one records no owner.
- **Ending without breaking:** an agreement's holder can end it, once, leaving the anchor standing and the shade's load in it (the method; the pact screens of the next units give it an action).
- **A familiar no longer fades for days its person is logged out:** the bond fades by 4 a day of its person's own play time away, in this dimension or another, counted before a visit grows it; play while the Concordance is switched off does not count while the familiar is loaded (one not loaded then counts it at its first look after the switch is on). A bond saved before starts counting at its first look. The codex page "The Bond" says so.
- **One pact record:** a Bound Will now carries its pact (where it is kept, why it is held, who benefits, its spirit, its upkeep, and the terms and day's count it was sealed with) and the offerings paid ahead for that upkeep; suspension is the record's. A copied anchor does nothing, says another agreement covers it and releases nothing when broken; an anchor from before claims its record once, at its own place, with any suspension it kept; one whose Bound Will was ended elsewhere forgets its terms and may be sealed again. Breaking an anchor while the Concordance is switched off releases nothing. Upkeep (for Chunk D's pacts; the anchor has none) falls due on the Spire's model: looked at only with the Concordance on and the holder within 64 blocks, at most one period a look, a due date passed while nobody looked becoming the moment of return; it warns, gives a grace, suspends without loss and resumes a whole period on when an offering is paid (at once, even in the tick of a look), never owing time away, held or switched off: an absence still counts when its holder suspends and resumes the pact before their first look back, and a gap of a minute (1,200 ticks) or less is not an absence, so a due date or grace end inside it counts. Ending gives the escrow back once, to the living holder in person; the last eight departures of each holder are kept. Record: [concordance-chunk-d.md](docs/features/concordance-chunk-d.md).
- Unit 1 (D-01, D-02): built and pushed, with the unit review's twelve fixes. Its local checks pass (`tools/owner_art.py --check`, `tools/check_mod_data.py`, `scripts/check_repository.py`, `tools/concordance_journey.py --check`, `tools/check_icon_maps.py`, the World Designer and script tests, `git diff --check`), and 52 of its 53 audit mutations are reported. CI run 40067977680 on `da185dd0d` passed every job: the build, its 29 new server game tests among the rest (with and without the optional integrations) and every client class. Not played by hand or with two clients. Contract, proposals (D-P1 to D-P16) and evidence: [concordance-chunk-d.md](docs/features/concordance-chunk-d.md).
- **Stolas's Regard** (D-03): research found through Styxhexenhammer's hints or, without him, through the codex and a feather; examine, cultivate, assay; the journal's Pacts section. Styx tells each listener their own next step and the sky step, and teaches nothing. Record: [stolas-pact.md](docs/features/stolas-pact.md).
- **The Pact Perch and Stolas's terms** (D-04): a perch you build and own; Stolas arrives, speaks, and names every term before anything is spent; Accept pays the Open Hand Compact (2 amethyst shards, 3 feathers and 8 Astral Resonance) and seals in one step; every refusal spends nothing and says why. Provisional body: the owner's Flashjack rig at 1.4×.
- **Stolas's pact serves, and is kept** (D-05): Star-reckoning recalls a pattern you observed, once a game day, through the one recall path; an observation while the pact is active masters Stolas's Regard; one offering a week paid ahead (at most four), a day's grace, suspension with nothing lost; ending returns what was paid ahead once; breaking the perch ends nothing and the pact can be seated again; switched off, everything waits. Balance note: a non-master may recall the Harvest Moon (Hero of the Village) daily and the Winter Crown every other day; the once-a-day quota is the only hard limit, since stockpiled or traded astrolabes pay too (one keeper's own observatory averages about 2.09 resonance a day, 3.59 with the Winter Crown, which with seasons off is observable all year).
- Unit 2 (D-03, D-04, D-05): built on its own local branch from unit 1's records, then merged here after unit 4 and pushed. Its local checks pass (`tools/owner_art.py --check`, `tools/check_mod_data.py`, `scripts/check_repository.py`, `tools/concordance_journey.py --check`, `tools/check_icon_maps.py`, the World Designer and script tests, `git diff --check`), and all 80 of its audit mutations are reported. `./gradlew build`, its 46 new server game tests and its client test had not run where it was built (CI was its first compile). Since pushed: CI run 40090825848 on `92868ff82` (units 1, 2 and 4) compiled it and passed every client shard, its client test among them, but failed two of its server tests in both server jobs, each a defect of the test (`aBrokenPactDefinitionIsReported` read Stolas's offer after refusing it on purpose; `talkingGrantsNoResearch` assumed an empty inventory), fixed in `8a3425f9e`, whose run 40094972598 passed every job. At the merge, Stolas, like the Gathering Shade, takes no room on his holder's roster when replaced or loaded while the Concordance is switched off. Not played by hand or with two clients. Proposals D-P17 to D-P50 and evidence: [concordance-chunk-d.md](docs/features/concordance-chunk-d.md).
- Unit 2's unit review (pushed with it): a warning is no longer renewed by leaving for a minute (the grace left carries over, its end moved on by the time away; proposal D-P50); a perch set by a machine seals for nobody; a long offering or pattern id no longer disconnects its holder; a pact whose perch was broken while switched off keeps no obligation; Star-reckoning's terms can only be personal; a visitor loaded while switched off leaves; a decline is shown on the action bar.
- **A Workshop Hand carries a Courier Post's requests:** once you have mastered Stolas's Regard, a Spirit Anchor bound to your Courier Post seals a Workshop Hand, a spirit on the Gathering Shade's body with a wicker pack. With the Clockwork Porter's own decision it carries the post's exact requests, an Alembic Crucible's own shortage among them, for you and your party only, bound for places within 12 blocks of its anchor and taken from containers inside that area, from 06:00 to 18:00, up to 32 loads a day of at most 64 items each; other requests it leaves to other couriers, and so it does cargo stranded at the post that is more than it carries (a porter's) or must go back to a container beyond its area. Its hold, its hours and its day's loads stop only new claims: a load it has claimed it finishes. Killed, or its agreement ended, its cargo waits at the post for another courier or one recovery; killed, its holder calls another at no cost. Each delivery is a spirit's service, so the Binding Arts still need a familiar's, a spirit's and a construct's. Records: [arcane-concordance-logistics.md](docs/features/arcane-concordance-logistics.md), [arcane-concordance-workers.md](docs/features/arcane-concordance-workers.md).
- **The Porter Key binds a Spirit Anchor to a post:** a key set on a Courier Post, used on an anchor that keeps no agreement, binds it (sneaking unbinds it), only for the anchor's owner or their party (an anchor placed before Chunk D, with no owner recorded: whoever may build there), only to a post they may use within 12 blocks, and never at a copied anchor; reading an anchor that keeps a hand's agreement names its post, or says it is gone (the hand then says it waits outside its agreement). A key used on an anchor no longer acts there as an empty hand, or, sneaking, takes it for a route's container. Clockwork Porters behave exactly as before: their courier decision is now shared with the hand.
- Unit 4 (D-08): built on another local branch beside unit 2, then merged onto unit 1 and pushed. Its local checks pass (`tools/owner_art.py --check`, `tools/check_mod_data.py`, `scripts/check_repository.py`, `tools/concordance_journey.py --check`, `tools/check_icon_maps.py`, the World Designer and script tests, `git diff --check`), and all 42 of its audit mutations are reported, as are the nine its unit review's fixes add. CI compiled it: runs 40079227985 and 40082852322 failed its twenty workshop-hand tests at their first step, because a test helper compared an item's use with the SUCCESS constant, which 26.3's `ItemStack.useOn` never returns (fixed in the helper); run 40085012234 on `86c4e7127` passed every job, its 21 new server game tests and its client companion check among them, after one re-run of a client shard that had failed on main's `PeepoPopulationClientTests` cow-breeding flake (outside Chunk D). Not played by hand or with two clients. Contract, proposals (D-P70 to D-P83), the As built note and the schema review: [concordance-chunk-d.md](docs/features/concordance-chunk-d.md).
- **Stolas's character:** a raven adapted from the owner's Flashjack rig (its skin recoloured texel for texel, his eyes lit, nine clips of his own beside the owner's five), who arrives, listens, looks offerings over, assents, refuses and departs, each gesture fired by the server with a sound and a subtitle (from sound files already in the game; no voice); answers marked with a tick or a cross; leaving, he makes his departing gesture and is gone two seconds later, never saved meanwhile; Jade names a perch's or an anchor's pact, service, state and next offering, and nobody. Record: [stolas-pact.md](docs/features/stolas-pact.md).
- **The roster:** every pact, agreement, familiar, spirit, construct and courier request of yours on one screen (at most 64 rows; the rest counted), with suspend, resume, end (asked first, the keyboard on Keep it), cancel and recover, each checked again by the server; the journal lists your pacts and departures; bind its key in Controls. A replaced Workshop Hand loading while the Concordance is off no longer takes a roster place. Record: [arcane-concordance-workers.md](docs/features/arcane-concordance-workers.md).
- Unit 3 (D-06, D-07): built on its own local branch from the merged units 1, 4 and 2, then merged into the pact hall at `554d71ab1` and pushed. Its local checks pass (`tools/owner_art.py --check`, `tools/check_mod_data.py`, `scripts/check_repository.py`, `tools/concordance_journey.py --check`, `tools/check_icon_maps.py`, the World Designer and script tests, `git diff --check`), and all 104 of its audit mutations are reported. Since pushed: CI run 40120355940 on `554d71ab1` failed to compile its roster client test (`LOGGER.info` with a generic `computeOnClient` result was ambiguous, two errors), fixed in `040d73ece`, whose run 40121823509 passed every job: its 18 new server game tests (the unit review's one included) with and without the optional integrations (`optional integrations absent` after one re-run: `TurkeyGameTests.hensLayEggs`, from #112 and untouched by Chunk D, failed there once and passed in that run's `mod` job), its new client test and its additions to two others. Not played by hand or with two clients. Proposals D-P90 to D-P121 and evidence: [concordance-chunk-d.md](docs/features/concordance-chunk-d.md).
- Unit 3's unit review (pushed with it): Stolas's departure always ends on time, even where his chunk has stopped ticking him, and one who starts watching him as he leaves sees him already gone; his listening, inspecting and departing sounds are mono files, so they come from him and fade with distance; an action refused as too fast is answered once in its gate's span, not for every packet; a Cancel with no cargo in transit no longer mentions cargo; places in the mod's lairs and Planariums are named, not shown as ids.
- Unit 5 (D-09): no new block, item or saved data. The composer now shows subtitles over itself (unseen: CI has no sound device). A solo pact is now known to need Celestial Attunement (only an observatory's keeper gathers the compact's resonance), and the codex says so. One server test walks a player from Styx to Stolas's departure. A measured pact hall load card, opted in: in run 40133540741, on CI's runner (an AMD EPYC 9V45, 4 logical CPUs, Java 25.0.4.1), the hall added +0.098 ms to the median server tick and +0.311 ms to the p95, net of the test's own work, and +0 MiB retained heap beyond the baseline's change, within the proposed budget. The pact's client test now photographs Stolas from his front and takes a timed sequence through his refusal; the roster's photographs a workshop hand carrying a request's three copper ingots, frozen with its cargo aboard. A new client test starts a second client program beside the first on one CI machine, as main's owner-collections network test does: a stranger at the holder's perch sees Stolas arrive, assent and depart, and is refused accepting there (`not_permitted`), ending the holder's pact (`not_holder`) and naming it from the roster (`not_listed`), while the holder's pact seals once and ends once. A manual Build run can also run the load cards and the pact's client tests without the optional integrations (workflow changes for the owner's review). CI run 40133540741 on `48d99c904`: every job passed on the first attempt. Three faults its pictures showed, which its tests had passed, are fixed since: Stolas's own Jade line no longer reads "entity.minecraft.", and the second client waits for the world to show and brings the refusal into view before its pictures (`b773913c4` and `d00fa2167`; run 40140478331: every job passed, and the fixed pictures were viewed). Not played by a person; two automated clients on one machine, no two people or accounts. Contract: [concordance-chunk-d.md](docs/features/concordance-chunk-d.md#unit-5-evidence-and-handoff-d-09).

### Unmerged: Arcane Concordance, Chunk C (living workshop)
- **The Alembic Crucible says what it is waiting for:** used with an empty hand, a crucible following a formula now ends its status line with the reason: water short in its tank, an ingredient missing (or the same one prepared otherwise), the wrong heat, Preserve's hold, no bottle or bowl, an output that is full or holds another brew, or a batch to bottle before a repeat. Jade says the same and now shows the tank's water in parts. A shortage or a blockage shows a want sign once when it begins. Record: [arcane-concordance-alchemy.md](docs/features/arcane-concordance-alchemy.md).
- **A formula newer rules cannot make stays set:** when a reload or a data pack refuses one of its steps, the crucible keeps the formula at its step, says which step and why, does nothing (not even bottling a finished batch) and carries on when the rules change back. Before, it was cleared with only a sound. A saved formula this build cannot read is kept and reported, not dropped. Setting a written formula the rules refuse says which step and why; setting one that would harm names the harmful statuses and any searing stir, and the risk line (when it is set, and on the Inspect page) also names the harm of a mixture in the crucible that the formula did not make, since it can still be bottled by hand. Record: [arcane-concordance-alchemy.md](docs/features/arcane-concordance-alchemy.md).
- **Only someone who could break a crucible may work it:** pouring, adding, stirring, bottling, and setting or clearing its formula now ask the same protection as breaking the block (claims, protected towns, adventure mode, spawn protection). Tasting, the Assay Glass and recording onto a blank formula stay open. Record: [arcane-concordance-authority.md](docs/features/arcane-concordance-authority.md).
- **Peepo's crucible status:** it says Full only when the output is full or holds another brew (before, whenever the output held anything, though the crucible kept filling a matching stack), asks for nothing while the formula cannot be followed (showing Recipe unavailable), and shows Occupied while a batch waits to be bottled before a repeat. Record: [companion-farm-stand-crucible.md](docs/features/companion-farm-stand-crucible.md).
- **The encyclopedia's Inspect page reads a crucible:** looking at an Alembic Crucible within your reach, it shows its heat and what heats it, Preserve's hold, its volume and tank, the formula's step, why it waits, the risk line and its slots, and, with an Assay Glass carried, what the glass would tell you at your research level. Record: [arcane-concordance-first-route.md](docs/features/arcane-concordance-first-route.md).
- **The Stillroot Poultice:** two Mendvetch in a part of water, stirred warm four times, bottle in a bowl as a Stillroot Poultice, Regeneration II for 22.5 seconds (the generator's model). Contaminant now fouls Verdance: each unit of it a part takes 4.00 units of Verdance, so one bone meal stirred in leaves only Regeneration I, and a searing stir spoils a one-part batch. The Assay Glass shows what the contaminant takes, and a master's glass names what fouled it. Every Verdance brew that carries contaminant heals less from now on; bottles already made keep what they carry. Record: [arcane-concordance-alchemy.md](docs/features/arcane-concordance-alchemy.md).
- **Healing credit for a poultice:** a Stillroot Poultice put on a friend in a lair fight (Vesperine, Madame Tatterlace, the Yeti King, the Cinder Tyrant) earns its helper healing credit for each heal of its Regeneration that gives back hurt the fight did, one health a heal; four heals qualify, as four health of Concordance healing does. One on yourself, one put on outside the fight, and a second that changes nothing earn nothing. Each keeper's dossier says so. Record: [lair-encounter-credit.md](docs/features/lair-encounter-credit.md).
- **The Kiln Draught:** one magma cream (or two sugar) in a part of water, stirred hot twice, bottles as a Kiln Draught of Fire Resistance for 76.55 (or 86.1) seconds by the model; under it the Cinder Kiln's slag does not burn and the Cinder Tyrant's flame does not take. Carrying a Kiln Seal, the kiln's dossier names it beside the Salamander Charm. JEI's Mendvetch, magma cream and sugar pages and two new codex entries name the products. Record: [cinder-kiln.md](docs/features/cinder-kiln.md).
- **Winter Brine is named on its bottle,** as the two new products are on theirs. A named brew does not stack with one bottled before, so an automated crucible pauses on an old one in its output and says so.
- **Every dose of a batch is the same:** a batch in several parts of water gave its last dose what did not divide evenly, so its bottles could differ by a thousandth and an automated crucible stopped on its own second bottle ("holds something else"). Each dose now carries the same share, so they stack; what does not divide is lost. Preserve's hold is given as the reason a stir waits only while it is what keeps the heat away. Record: [arcane-concordance-alchemy.md](docs/features/arcane-concordance-alchemy.md).
- **The formula's tooltip says "use":** a written formula is set by a plain use; the tooltip said "sneak", and sneaking with an item in hand skips the crucible.
- Unit 1 (C-01, C-02): every local check passes, and every CI job passed in run 38684732031 (through `0b1039312`, after one re-run of a client shard for main's Peepo cow-breeding flake, which PR #357, since merged in main (`40381834a`), fixes). Not played by hand or with two clients. The chunk's contract, decisions and evidence: [concordance-chunk-c.md](docs/features/concordance-chunk-c.md).
- **A ritual circle says everything a rite still needs:** looking at a Circle Anchor, the encyclopedia's Inspect page lists everything the rite your offerings answer still needs before it can begin, all at once: offerings short, each pylon's charge for a step and for the whole rite, pylons that will not lend, where you stand, your Focus, the light and the circle's faults. Then the rite's stages and what each pays, the backlash included. A pylon short only of the whole run is a warning; a start still asks one step's charge. A rite you have not learned is never named there or in Jade. Record: [arcane-concordance-rituals.md](docs/features/arcane-concordance-rituals.md).
- **Spent apart from held:** while a rite runs, the Inspect page and Jade show what it has spent (Focus and Ley Charge) apart from the offerings it still holds. Jade's gathering line shows "Gathering: 1 of 2" where it said "?", and lists the first three offerings in the anchor.
- **Faults marked where they stand:** reading a circle with an empty hand or `/jugcraft concordance circle` (at most once a second), or a refused start, marks every faulted part with a sign. Fault positions in chat and on the Inspect page also say where they are from the anchor.
- **One interruption message:** a ritual that breaks off says in one line why, where the offerings are, and the Focus and Ley Charge lost. Before, the reason and "the offerings are still in the anchor" were sent in the same tick, so the second probably replaced the first on the action bar.
- **Root and Rivet:** a new rite of the Lesser Circle, taught by Circle Lore understood. A Thallite plate, 4 copper ingots and 4 Mendvetch become a Rooted Thallite Foundation (6 Focus, 32 Ley Charge from the pylons, 8 seconds), the base of the Crucible Stand (unit 3); nothing turns it back, and the Assayer's Scale never forms it (with main's Scale, which values every default item, it can be dissolved at its catalogued worth). It has a codex entry with its stage and payment table (linked from Stratawrighting's), JEI's ritual and information pages, and a fresh 16×16 icon, which the owner did not ask to be redrawn at the checkpoint (C-D7). The plate needs the `machines` and `thallite` switches. Completing it counts as a Stratawright's foundation for each participant present. Record: [arcane-concordance-rituals.md](docs/features/arcane-concordance-rituals.md).
- Unit 2 (C-03, C-04): every local check passes, and every CI job passed in run 38693262154 (on `23f28b90e`). Not played by hand or with two clients. Contract: [concordance-chunk-c.md](docs/features/concordance-chunk-c.md).
- **The Crucible Stand:** crafted from a Rooted Thallite Foundation, six iron ingots and two copper ingots (with the `machines` and `thallite` switches on), it sits under an Alembic Crucible and takes JE from a cable on any face but its top. Following a formula it heats the crucible for the next stir: warm 65, hot 120 or searing 175 degrees, for 8, 16 or 32 JE a tick, its copper coil lit while it pays. Fed less than that, it still brings the crucible to the band for the same JE a degree, holding it on the ticks it cannot pay, and its coil stays lit. It pays nothing while the crucible waits for anything but heat (water, an ingredient, a bottle, room, a refused formula), while Preserve holds it, or while the crucible is already hot enough. With no formula, an empty-hand use by someone who may change it sets off, warm, hot or searing. An Electric Pump piped into the crucible fills its tank; a campfire or any other heat beneath works as before. Its cladding, frame and coil textures are the owner's treated iron and copper coil, byte for byte; its slab is Jugcraft's thallite block. Heat is never stored or turned back into JE or anything else. Record: [crucible-stand.md](docs/features/crucible-stand.md).
- **Crucibles ask the couriers themselves, and a Peepo brings only what no courier is bringing:** a crucible waiting for an ingredient files one request, as whoever set its formula (while they are here), at the nearest Courier Post within 16 blocks they may use, for exactly what it lacks (a ground reagent stays ground), as much as fits in it, and nothing while its buffer is full of other things; a Clockwork Porter fetches it into the crucible. A Peepo working the same crucible subtracts what a courier holding that request is still bringing, so the two do not bring the same need twice while a courier holds the request. A full crucible keeps the rest aboard until there is room, an unloaded one waits with the cargo kept, and clearing or changing the formula or breaking the crucible turns the request back. With its setter's sixteen requests open it says so and asks again later. A courier's notes now say "where it was going is full" and "where it was going was broken". Record: [arcane-concordance-logistics.md](docs/features/arcane-concordance-logistics.md).
- Unit 3 (C-05, C-06): every local check passes, and each of its 53 audit mutations is reported, as are the unit review's 21 for its fixes. In CI run 38720814268 (on `2f13fed54`) the build, every server game test (with and without the optional integrations) and four of five client shards passed; the fifth failed on a defect in a Peepo client check of an unloaded crucible, fixed in `9098c06b1` (unit 5's combined fix), after which every job passed in run 38731825181. Not played by hand or with two clients. Contract: [concordance-chunk-c.md](docs/features/concordance-chunk-c.md).
- **The garden shows its workings:** looking at a Verdant Bed or a crop on one, the encyclopedia's Inspect page shows the bed's moisture and nutrients, the crop's step and pace, the verdict growth would reach, and each factor against what the plant needs (least, best and most) with how far its reading reaches: the bed's own moisture, the light as sky and lamps (the greater counting), the kinds of plant within 2 blocks, and the working magic in the 5 by 5 round it. Jade on a ripe crop now shows its last verdict, "As it grew"; a bed forgets a gone plant's verdict on its next random tick, and at once when another crop is set on it or it is grafted. Record: [arcane-concordance-ecology.md](docs/features/arcane-concordance-ecology.md).
- **The canteen waters Verdant Beds,** as the codex already said: a Gourd Canteen's sip wets a dry bed to 7 where you may build, and is spent only when it wets. An item held on a bed is now its own use, so a seed plants and fertilizer works without sneaking; an empty hand still reads the bed. JEI shows what a Mulch Maw makes of each thing it eats, in quarters of a nutrient. Record: [arcane-concordance-ecology.md](docs/features/arcane-concordance-ecology.md).
- **A mixed bed:** a Dewmoss with a Gloamcap in the 5 by 5 round it, or a Gloamcap with a Dewmoss, grows 25% faster. Only the other kind counts, never copies, and a step costs what it always did, so a mixed bed makes no nutrient.
- **Harvest strain:** each harvest strains its Verdant Bed by one, up to 3, whatever took the crop (a hand, a Gleaner, a sickle, a reap, the Crop Harvester, breaking it). Strain counts only against a plant's disturbance limit: a Sunpetal on a quiet bed still thrives at 1 or 2 and grows at half pace at 3, and a Gloamcap still needs its magic. A bed rests a strain away while nothing grows on it, and compost (bone meal, fertilizer, a Mulch Maw's nutrient) and a Greenwarden's empty hand on the bed or on its growing crop (once every 30 seconds a bed) ease it too. It never kills and never reads industrial work. Bone meal and fertilizer now work on a growing crop by a plain use; before, the crop only reported. Record: [arcane-concordance-ecology.md](docs/features/arcane-concordance-ecology.md).
- **Dewlit:** a Greenwarden can graft Dewlit onto a Sunpetal's bed by using a Dewmoss on the crop: the Dewmoss is spent, and a Sunpetal on that bed bears shade (light from 9, best from 11) but wants a wetter bed (moisture from 4, best from 5), through every harvest, replanting and restart until another plant grows there. Its produce, cost and alchemy are unchanged. The codex's Organisms entry gains "Mixed Beds and Strain" and "Grafting" pages. Record: [arcane-concordance-ecology.md](docs/features/arcane-concordance-ecology.md).
- Unit 4 (C-07, C-08, C-09): every local check passes, and every one of its 83 audit mutations is reported. Its first CI run, joined with unit 3 (run 38726242149 on the merge `1c7dc577c`), passed every job, its server and client tests included. Not played by hand or with two clients. Contract: [concordance-chunk-c.md](docs/features/concordance-chunk-c.md).
- Unit 5 (C-10): no new block, item or behaviour. The living workshop's route from a manual start (crafted Mendvetch, a campfire crucible, lantern-filled pylons, then a powered crucible a porter supplies and a grafted garden) is checked step by step by `tools/concordance_journey.py` and played with real blocks in a server game test. A measured small-workshop load card, opted in: in run 38774859294, on CI's runner (an AMD EPYC 7763, 4 logical CPUs, Java 25), the workshop added +0.240 ms to the median server tick and +0.550 ms to the p95, net of the test's own work, and +0 MiB retained heap beyond the baseline's change; a manual Build run can ask for it (a workflow change for the owner's review). In the same run's `mod` job (its runner: 4 processors, amd64, Java VM 25.0.4.1+1-LTS), a Gleaner short of Verdance took 333 µs a pulse for its Heart search, over the proposed 250 µs line, so a change is proposed and not applied. A client test (passed in shard 4) takes shots of the whole workshop, the stand lit and dark, the crucible's Inspect page on its stand and Root and Rivet under way; the workshop, the lit stand and the crucible's Inspect page were viewed; the dark stand and Root and Rivet's Inspect pages were not. A combined fix: the Peepo unloaded-crucible check keeps its companion loaded (`9098c06b1`). CI run 38774859294 on `bb856f873` (units 1 to 5): every job passed, after one re-run of a client shard for main's Peepo cow-breeding flake. Not played by hand or with two clients. Contract: [concordance-chunk-c.md](docs/features/concordance-chunk-c.md#unit-5-evidence-and-handoff-c-10).
- **The owner's checkpoint (23 October 2026), recorded as Accepted decisions C-D5 to C-D8:** the balance of hands-on preparation and automation stays as built: the campfire crucible and carrying by hand stay a complete route, and the Crucible Stand and the couriers stay optional conveniences paid in JE (the stand) or in Ley Charge, which electricity can give (the couriers) (C-D5). The next product family is Gloam Ink (Gloamcap and an approved Hollow carrier, by covered extraction, for research notes, safe links and ward inscriptions; excess contamination yields an unusable sludge, recoverable through a mundane route) (C-D6); when it is built is not yet decided. The Crucible Stand's look fits (C-D7); the owner did not ask for the Rooted Thallite Foundation's icon to be redrawn, so it stays. Chunks B and C are reviewed as two stacked pull requests, Chunk B's against main and Chunk C's against Chunk B's (C-D8). The load budget and the workflow change (`bfb08770b`, the manual-only `load_card` input and job) remain for the owner's review in Chunk C's pull request. Contract: [concordance-chunk-c.md](docs/features/concordance-chunk-c.md#decisions).

### Unmerged: Arcane Concordance, Chunk B (winter and stone)
- **Support earns the Yeti King's reward:** as at Vesperine and Madame Tatterlace, a player now qualifies for their own roll of his loot by healing another player, with the Concordance, 4 of the hurt his side's blows did them, or by a Concordance ward on another player that soaks 4 of those blows; a tamed creature's blow on him or a whelp counts for its owner; and a player who already has a part in his fight (a blow, healing or a ward) and baits his Avalanche Charge into an ice column earns that deed, once each a fight (a late arrival he merely turns on earns nothing by it). The blow that fells him now counts even if it is the striker's only one, and each fall pays each player once: before, a second report of his death paid everyone again. One who died in his fight is paid once back on their feet, and one who left the game in it when they next join. Freezing has no attacker, so healing it earns nothing. Record: [lair-encounter-credit.md](docs/features/lair-encounter-credit.md).
- **Support earns the Cinder Tyrant's reward:** likewise, a player now qualifies for their own roll of his loot by healing another player, with the Concordance, 4 of the hurt his blows and his own fire did them, or by a Concordance ward on another player that soaks 4 of those blows; a tamed creature's blow on him or a Cinderling counts for its owner; the blow that fells him counts even if it is the striker's only one; and each fall pays each player once (before, a second report of his death paid everyone again). One who died in his fight is paid once back on their feet, and one who left the game in it when they next join. He names no objective of his own, and his slag's heat and burning have no attacker, so healing what they took earns nothing. Record: [lair-encounter-credit.md](docs/features/lair-encounter-credit.md).
- **The Glacier Hall's dossier:** carry a Frost Horn and the encyclopedia's Expeditions page shows the Glacier Hall's rite (at night in the Overworld, standing on snow or ice), the horn's recipe as a grid, the Seasonal Stall's snow blocks where a town stands and they are on sale, that the horn works all year, and the party a lair holds. Meet or beat the Yeti King and it adds how support earns a share (his whelps, healing, wards, and the column baited by one already in his fight), the two trophies of which a first win brings one at random, his loot with JEI's words, the cheaper horn made with Yeti Fur and the way home. A first win over any lair's keeper, the Yeti King's included, now completes the First Light route's lair step. The words every dossier shares no longer say "her" (proposed, B-P2). Record: [arcane-concordance-first-route.md](docs/features/arcane-concordance-first-route.md).
- **JEI says where the Yeti King's loot comes from:** with JEI installed, Yeti Fur, the Yeti Mitten, the Yeti King's Crown, the Glacier Maul and the Rimeclaw each have an information page: the Glacier Hall and how the horn opens it, and each share's chance with the default data (a 15% chance of one of his two trophies, 7.5% each, and always one of the two on a player's first victory over him). Record: [concordance-first-loadout.md](docs/features/concordance-first-loadout.md).
- **The Cinder Kiln's dossier:** carry a Kiln Seal and the encyclopedia's Expeditions page shows the Cinder Kiln's rite (pressed into a magma block in the Nether, or in the Volcano or the Cinder Barrens, at any hour), the seal's recipe as a grid, that the seal works all year, and the party a lair holds. Meet or beat the Cinder Tyrant and it adds how support earns a share (his Cinderlings, healing and wards; not what burning or his slag took, and no objective of his own), the two trophies of which a first win brings one at random, his loot with JEI's words, the cheaper seal made with Tyrant Scales and the way home. A first win over him also completes the First Light route's lair step. Record: [arcane-concordance-first-route.md](docs/features/arcane-concordance-first-route.md).
- **JEI says where the Cinder Tyrant's loot comes from:** with JEI installed, the Tyrant Scale, the Salamander Charm, the Tyrant's Crest, the Cinderbrand and the Magmaw each have an information page: the Cinder Kiln and how the seal opens it, and each share's chance with the default data (a 15% chance of one of his two trophies, 7.5% each, and always one of the two on a player's first victory over him). His pages say a share is earned by a blow, healing or a ward, naming no objective. Record: [concordance-first-loadout.md](docs/features/concordance-first-loadout.md).
- **End-gun Levitation no longer lifts the Yeti King or his whelps:** an End gun's bullet still hurts them, but its float (and any other Levitation, a shulker's bullet or a potion) does not take on them. Before, a whelp rose off the snow as any creature does, and the King floated up as he died. Record: [yeti-king.md](docs/features/yeti-king.md).
- **The Yeti King's ice and whelps stay with his fight:** a player who leaves the Glacier Hall mid-fight and comes back finds no block of ice, icicle, spike or whelp left from before; each now lasts only while he counts it his. Before, those left in parts of the hall that stopped without unloading came back with the player: an icicle or a block of ice fell or flew on and could strike, and a whelp served him again. Record: [yeti-king.md](docs/features/yeti-king.md).
- **The Yeti King's charge:** a run that would carry his body into an ice column's flared foot now ends there with him stunned, as a straight run into the column does; before, a glancing run could slide him round the foot unstunned. Record: [yeti-king.md](docs/features/yeti-king.md).
- **Unit 2: the Glacier Hall and the Yeti King's fight tested.** New server and client game tests hold the horn kept on every refusal and answered on any of its ground and on Peaceful; the hall reached in a world made with the World Designer's generator, the Cinder Kiln too, created and reopened; a real restart with a player inside a Glacier Hall, who is sent home and paid a reward kept from before once; and his fight against the chunk's completion evidence: his charge at the columns, the dais, the ramp, a den's wall and his leash, the same every run; a solo fighter, a solo caster and a party of two baiting him into a column; his snow drifting back and his kin and ice gone on a reset, at his fall and after an unload; wool and leather against his frost; and End-gun Levitation. Their first CI run (38534127130) found the charge and the ice faults fixed above. In run 38543250503 every job passed: all 1658 required server game tests with and without the optional integrations, and every client test class. Not played by hand or with two clients. Records: [yeti-king.md](docs/features/yeti-king.md), [glacier-hall.md](docs/features/glacier-hall.md).
- **Unit 3: Rime and Strata.** Once First Light is understood, two new research entries open. **Rimekeeping**: sneak and use snow or ice, and something that holds fire, to observe it; understand it at a Lampwright's Bench, by arriving in the Glacier Hall, or by bottling a brew that carries Rime by hand at an Alembic Crucible; master it by three kinds of cold work. **Stratawrighting**: sneak and use granite, diorite, andesite, tuff or raw Thallite (the same examination begins Assay; cobblestone, stone and deepslate never examine, so you can still bridge); understand it at the bench; master it by three ways of anchoring (bracing a party member, a ritual completed or a blow taken in Earthbound Thallite can stand for one). Thallite is never required.
- **Three invocations** (13 of 16), cast with any Concordance instrument: **Rimebind** (defense, 4 Focus) slows the first creature on a 16-block ray by a fifth for 3 seconds, then it thaws for 2 and no one's Rimebind takes it; **Brace** (support, 3 Focus) gives you and your party within 4 blocks half the knockback of the next blow or harmful push for 5 seconds, and one blow spends it; **Preserve** (utility, 3 Focus) keeps an Alembic Crucible you could change by hand in its heat band for 60 seconds, shown in Jade and the crucible's reading, and holds nothing else. Mastery lowers each by a point.
- **The Yeti King and the Cinder Tyrant are now resistant to Concordance control:** Rimebind's slow, Dazzle and the Ice Breaker's slow last half as long on them, and Smoulder on the Yeti (the Tyrant is immune to fire). Two new codex categories, Winter and Stone. The icons and Brace's sound are the owner's own art and recordings, copied unchanged; Rime's and Strata's casting cues are drawn.
- Built and tested in CI with unit 1 (run 38519234934, not merged): every job passed, all 1639 required server game tests with and without the optional integrations and every client test class. The status screenshots were hidden by advancement toasts, so the test now clears them; since then (runs 38534127130 and 38549876709) they show Braced and Rimebound on the HUD, and Braced, Rimebound and Thawing as icons in the inventory. Spell Engine's spell bar runs off the screen's left edge with 5 or more spells in CI's small window, and with 8 or more at 1920×1080; its HUD settings can move it. Not played by hand or with two clients yet. Record: [arcane-concordance-rime-strata.md](docs/features/arcane-concordance-rime-strata.md); the chunk's contract and decisions: [concordance-chunk-b.md](docs/features/concordance-chunk-b.md).
- **Unit 4: Winter Brine and Frostward.** At an Alembic Crucible with nothing burning beneath it, three snowballs in a part of water, stirred cold three times, bottle as **Winter Brine**: a draught of **Frostward** for 48.75 seconds (60 seconds a unit of Rime, at most 2 minutes; milk ends it). While it lasts no frost gathers on you (powder snow, the Frost Horn's arrival, a snow werewolf's frostbite) and the Yeti King's blizzard and breath frost do not take; his blows still land, and his Yeti Mitten stays the lasting answer. Every Rime brew now gives Frostward instead of Resistance, and any stir above the cold band thaws part of the Rime (warm keeps 60%, hot 30%, searing none), in amethyst and gloamcap brews too; Rime draughts already bottled keep their Resistance, and written formulas replay. JEI's snowball, amethyst shard and gloamcap pages and a new codex entry say so, and the Glacier Hall's rite in the encyclopedia names Winter Brine and the Mitten. Frostward's icon is the owner's, copied unchanged.
- **The Rimekeeper's travel kit,** the owner's water art under the owner's names, made at a crafting table from Overworld things: **Nereid's** (light), **Aquamancer's** (medium) and **Winterguard's** (heavy, on the owner's own model) sets, each no stronger than the Pyromaniac's, leather and iron, with no Spell Power. Wearing any piece, powder snow and hard cold never freeze or hurt you; you still sink in powder snow, and the Yeti King's frost still lands and slows you. A **Lesser Focus of Water** or **Focus of Water** in the Spell Focus slot makes Preserve hold 90 or 120 seconds, and the **Water Bangle** braces a Rimekeeper who has gone 30 seconds without a Brace (nobody's cast: it teaches nothing).
- **A Frost boon slow is Rimekeeping practice:** slowing a hostile creature with the Glacier Maul, the Rimeclaw or the creative-only Frost Knight Greatsword is an elective kind of cold work for a Rimekeeper, and can stand for one of the three mastery asks. The boon, Thallite and the trophies keep every number.
- Built and tested in CI with units 1 to 3 (run 38549876709, not merged): every job passed, all 1676 required server game tests with and without the optional integrations, unit 4's 18 new ones among them, and every client test class, the new winter one included. Its screenshots show Frostward's icon on the HUD, the three sets worn on stands, a zombie and the player, and the fifteen icons; the Water Bangle's shot is covered by advancement toasts, so it does not show Braced (fixed in unit 5: run 38591108210 shows it). Not played by hand or with two clients yet. Record: [arcane-concordance-winter.md](docs/features/arcane-concordance-winter.md).
- **Unit 5: Brace and Rimebind against the Yeti King** (optional; neither is needed to win): only a Brace now lessens his throws. Braced, the next of his blows, slams, charges or glacial spikes throws you half as far, once however many of them land in that tick; knockback resistance from anything else (netherite, Earthbound Thallite) still does not lessen his throws (a blow's own small knockback, and his boulders' and icicles', are the game's as before). The Water Bangle's Brace counts too. A Brace you put on another player that takes one of his throws earns your share of his reward; one on yourself, or the Water Bangle's, earns nothing. Rimebind's slow does not hold his steps; a binding instead holds his wind-up (the one under way unless it is about to strike, else his next) 4 ticks longer, frost-locked, once each time he thaws (a starting value, not yet tuned). The Braced and Rimebound codex entries and the Glacier Hall's dossier say so. Record: [yeti-king.md](docs/features/yeti-king.md).
- **The Yeti King's column is Rimekeeping practice:** drawing his charge into an ice column, as the player it was aimed at, having already taken part in his fight, is now an elective kind of cold work for a Rimekeeper, the same kind as a Frost boon slow: the two count as one, so mastering Rimekeeping still needs a Preserve (and so the Alembic Arts). The Rimekeeper advancement, the codex and the player guide name it. The elective's saved evidence key is renamed `elective` (unit 4's `frost_boon`, never released); a world saved by unit 4's unmerged build keeps a `frost_boon` key as a kind of its own. Record: [arcane-concordance-rime-strata.md](docs/features/arcane-concordance-rime-strata.md).
- **The encyclopedia's home shows each route card whole:** in CI's small window (427×240, run 38519234934) the Technology & Magic Guides' home cut the Magic card's link to "Open Magic: the Arcane Concorda…" and stopped both overviews mid-line. By the layout's arithmetic (glyph widths estimated, not seen), 1920×1080 at the automatic GUI scale also stopped the overviews mid-line; its link fitted. Each card is now as tall as its whole overview and link, the link wraps, and the cards scroll (the wheel, a scroll bar or Tab) when the window is too small; larger windows look as before. Merging main f904df3fd (#415's book-style guides directory) replaced these cards; the card fix (d22a55830), its check_mod_data check and the `jugcraft_encyclopedia_home_end` and `jugcraft_encyclopedia_home_scale1` shots were dropped in the merge. On main's directory a route's overview is a wrapped paragraph in a scrolling page; its home tab cuts a title with "…", and by the layout's arithmetic (glyph widths estimated) CI's 427×240 window leaves 134 pixels for a title, so "Magic: the Arcane Concordance" (about 158) will be cut there: calculated, not yet seen in a screenshot. Record: [jugcraft-encyclopedia.md](docs/features/jugcraft-encyclopedia.md).
- Built and tested in CI with units 1 to 4 (run 38591108210 on `25644502a`, not merged): every job passed, all 1682 required server game tests with and without the optional integrations, unit 5's six new ones among them, and every client test class, unit 5's changed ones included. Its first attempt failed two tests outside Chunk B, both passing when re-run: a large nuclear plant's radiation reading in the build without the optional integrations (0.104 uSv/h against under 0.1; two nuclear tests on main build at one spot, and the same test passed in the other build) and a Peepo delivery client test that fails now and then (#357, since merged into main and this branch, fixes it). The client tests picture his charge into a column and his reeling, the spell bar at GUI scale 1 (all six slots on screen), the Water Bangle's Brace with Braced's icon and no toasts, and the guides' home cards whole when scrolled to the end and at GUI scale 1. The inventory shot does not show the foci or the Water Bangle (the slot column beside the paper doll drew no item; the server's checks are the proof that they are slotted), and the two dossier shots fell outside the part of the log that could be read. The large purple shape in unit 4's bangle shot was the Brace's own particles rising through the first-person camera, as expected. Not played by hand or with two clients. Evidence: [concordance-chunk-b.md](docs/features/concordance-chunk-b.md#unit-5-evidence-and-handoff-b-09-b-11).
- The chunk's contract, decisions and evidence: [concordance-chunk-b.md](docs/features/concordance-chunk-b.md).
- **The server game tests no longer run out of memory:** with main's #488 merged, all 2,575 of this branch's server tests ran, then the suite stopped with `OutOfMemoryError: Java heap space` on its last one (run 40414783975; main's 2,477 fit). A game-test mock player's connection ends in Netty's EmbeddedChannel, which keeps every packet written to it until something reads it; no test does, and the players stay logged in for the whole run, so their join packets and every later player's join message stayed in the heap. A test-only hook (`MockPlayerChannels`) now empties those channels each tick and logs how many packets it released; real and integrated-server connections are untouched. The game itself is unchanged.

### Unmerged: Costumed villains are unmasked
- Beat a **Spectral Cosmonaut**, **Drowned Diver**, **Emerald Phantom** or **Onyx Warden** and the costume comes off: one of three crooks for each villain stands caught where it fell, named, with an excuse everyone near hears, until they are led away 10 seconds later.
- The costume's mask drops as a trophy with the crook's name on it, and the player earns **Mystery Solved**. A villain that dies any other way keeps its secret. Record: [costumed-villains-unmasking.md](docs/features/costumed-villains-unmasking.md).

### Unmerged: A server load benchmark
- A new server game test, `ServerLoadGameTests`, times what a tick of a representative load costs the server: a factory of 80 working processing machines and a crowd of 24 costumed villains. It logs the numbers ("Server load: ...") and fails if either load's tick grows past a whole server tick. Memory and client frame times are not measured yet. Record: [server-load-benchmark.md](docs/features/server-load-benchmark.md).

### Unmerged: Trains
- A **Diesel Locomotive**, a **Passenger Coach** (four seats) and a **Cargo Wagon** (27 slots) run on any rails, round curves and up slopes. Use one on a rail to set it on the line.
- Ride in the locomotive's cab to drive: forwards and back work a four-notch throttle (two in reverse), jump brakes. It burns diesel or kerosene (a bucket refuels it).
- The **Coupler** couples cars into a train, up to eight behind one locomotive, and uncouples them. The whole train moves as one and stops at the end of the line or at another train.
- Breaking the rail under a car knocks it off as an item, with its goods. The Engineer's Handbook has a Railways chapter. Record: [trains.md](docs/features/trains.md).

### Unmerged: Cola and MMH
- **Cola:** the Chemical Reactor purifies Industrial Phosphoric Acid over charcoal into a vial of **Food-grade Phosphoric Acid**. The vial crafts with caramel, sugar, cinnamon, vanilla and lemon into **Cola Syrup**. The reactor carbonates the syrup with water and carbon dioxide into three bottles of **Cola**, which give Speed for 45 seconds and leave the bottle.
- **MMH:** the Bulk Chemical Reactor methylates hydrazine with methyl chloride into **Monomethylhydrazine (MMH)**. The reactor seals it into a **Sealed MMH Tank**, and the Rocket Workshop makes four Storable Bipropellant Motors from an MMH tank and an N2O4 tank (hydrazine makes three).
- Two new handbook pages. Quantities await review. Record: [cola-and-mmh.md](docs/features/cola-and-mmh.md).

### Unmerged: The drone depot terminal in the language file
- Every word on the **Drone Depot Terminal**'s screen (title, tabs, every readout and warning, the mode key) now comes from the language file (`screen.jugcraft.terminal.*`), so it can be translated. In English it reads as before, except that the FLEET tab names each tier by its drone ("T1 Courier Quad Drone"), the JOBS tab names missing materials by their item names, and the mode key reads PERSONAL or PARTY from the start. Record: [drone-terminal-lang.md](docs/features/drone-terminal-lang.md).

### Unmerged: The trade look
- Every screen where Jugs or goods change hands has the Exchange's walnut and brass: the **town shops**, the **Jug Teller**, the **personal safe** and the **barter station** and **energy vendor**, with the Exchange and market chest. It has a brass-rimmed walnut panel, slate ledgers and slot trays, gold sums, and brass and walnut plates for keys. The shop's Buy and Sell are folder tabs; the Jug Teller's chosen name is in brass; a personal shop's state is green when ready and red when something is in the way. Every button id and slot is unchanged.
- The **market chest** (332 wide) scales down to fit a narrower window.
- The screen fit check opens all of them. Record: [trade-look.md](docs/features/trade-look.md).

### Unmerged: The blueprint table fits a small window
- The **blueprint table**'s drafting board (410 or 440 × 250) scales down to fit a small window, as the reactor console and storage tank do. Its tabs, register, turning preview, stamps, paste box and the mouse scale with it. Before, it ran off the edges of the smallest GUI (320 × 240). Its look is unchanged.
- The screen fit check opens it too. Record: [blueprint-table-fit.md](docs/features/blueprint-table-fit.md).

### Unmerged: The workshop, aircraft and furniture screens in the looks
- The screens PR #475 brought in take the machine looks: the **lathe**, **fluid services** and **aircraft** the industrial look; the **workshop engines**, **chute**, **robot programming table** and **equipment charger**, and the **artisan's workstation** the workshop look; the **transformer** and **computer** the electric look; the **compact reactor** the advanced look. Each has the look's casing, glass panes, a status lamp by the title and keys that light up when they are on (the transformer's mode, open chute outputs, the chosen robot program, a running reactor, the artisan's features). The gate parameters, heat and kinetic generators, civil and matter workshops keep the electric look, with keys in it in place of vanilla buttons. Every slot, button id and payload is unchanged.
- The **computer** (360 × 288), **compact reactor**, **gate parameters** and the larger **aircraft** now scale down to fit a small window, slots and keys with them (a new `FittedContainerScreen`).
- The scribing desk keeps its scroll forge; the personal safe and shops keep their panels until the trade look.
- The screen fit check now opens all 15 at the smallest GUI, every aircraft layout and both workshop table kinds. Record: [new-screen-styles.md](docs/features/new-screen-styles.md).

### Changed: The benches in the workshop look

### Unmerged: Fixed: the tower cost check on Linux
- `tools/tower_costs.py` reads the recipe files in name order. Where several crafting recipes make the same item and none is named after it, the check takes the first; the file system's own order differs between Windows and Linux, so since the Jimtafacts batch it worked out five drone tower blocks' values differently on Linux (CI) and failed `check_mod_data.py` there. The values themselves are unchanged.

### Unmerged: The Crown of Thunder and the Thunder Vane (the Storm Roc, part 1)
- **The Thunder Vane:** two feathers, a lightning rod, three copper ingots and a compass. Raise it in the Overworld in a storm, in the mountains (the Highland, the Karst Pinnacles, the high country, any mountain, peak or hill), standing at 120 or higher under the open sky: lightning strikes it (only light and thunder), it is used up, and the wind takes you to the Crown of Thunder. Grey Mist tears open where you stood, and for 60 seconds anyone can follow through it. Anywhere else it only says why, and nothing is spent.
- **The Crown of Thunder,** a lair above the clouds under a storm that never breaks: a terrace with the Grey Mist home, a summit 36 blocks across rimmed by boulders, four copper Storm Rods on plinths (charged, they burn storm-blue), two ledges below its east and west edges with stairs back up, and the Roc's nest of woven branches on a crag to the north. A fall past the ledges is caught by the mist and costs the lair's toll, never belongings.
- The summit is empty for now: the Storm Roc, his fledglings and his loot are part 2. Record: [crown-of-thunder.md](docs/features/crown-of-thunder.md).

### Unmerged: Fixed: the server game tests slowing to a stop since the Jimtafacts batch
- The accessories' per-player check (`JimtaAbilities.tick`) read the block under every online player's feet, every time it ran, whatever they wore. Through an unloaded chunk that read loads, or even generates, the chunk on the spot. The game tests leave about 1,300 unticked mock players at the world origin, so the check ran every tick for each of them. Since the batch, the server's tick time has grown from about 10 ms to about 1 s over a test run, and CI's `mod` and `optional integrations absent` jobs have hit their 90-minute limit on main and on every pull request. This read is the only new per-player code that touches the world whatever is worn; that it caused the slowdown is the best-supported explanation, which CI's run of this change will confirm or refute.
- The ground is now read only when snowshoes are worn, and only from a chunk that is already loaded. Snowshoes behave as before.


### Unmerged: The fall's wild harvest in the landscape biomes (follow-up to the seasonal biome audit)
- **The fall grows wild in the landscape biomes too.** The 19 fall and Halloween wild patches grow in the landscape biomes of their kinds: chestnut trees, chanterelles and acorn squash in its woods, cranberries in its swamps, butternut squash and Cinderella pumpkins in its fields, wild mandrake in the Ominous Woods. So do the graveyard flowers. The bottle gourd and the Jarrahdale pumpkin (savanna, jungle and windswept kinds) do not, and every other wild crop is as it was.
- **The Hag Stone** opens the Lantern Fen from the landscape swamps (the Bayou, Bog, Marsh, Wetland and the rest).
- Built on the seasonal biome audit's landscape kinds, with one more, the dark woods. Record: [seasonal-biome-audit.md](docs/features/seasonal-biome-audit.md#follow-up-the-falls-wild-patches-the-graveyard-flowers-and-the-hag-stone).

### Unmerged: Wildlife tidy-up (landscape biomes)
- **Removed** the 107 `#jugcraft:wildlife_has_*` and `blacklist/wildlife_blacklist_*` biome tags copied from the owner's naturalist data, which nothing read; `tools/menagerie_content.py` no longer copies them. Creatures are placed as before.
- **Corrected** the wildlife record: every landscape biome lists its own spawns (the "95 with none" was a miscount), so no spawns were added. A data check now holds both.
- Record: [landscape-wildlife.md, the tidy-up](docs/features/landscape-wildlife.md#follow-up-the-tidy-up).

### Unmerged: Wildlife in the landscape biomes (follow-up to the seasonal biome audit)
- **The menagerie's creatures come to the landscape biomes.** Before, none of its 277 species reached any of the 159 landscape biomes: they carry none of vanilla's biome tags, and every species first asks for `#minecraft:is_overworld` (or the Nether's or End's). Now deer, bears and boars live in the landscape woods; alligators and capybaras in its swamps; lions, elephants and zebras on its savannas; scorpions in its deserts; tigers in its tropics; whales and rays in its ocean; mammoths and yetis in its snowy land; the landscape collection's own gob and garch in its caves; and the Nether's and End's creatures in its Nether and End biomes.
- Each landscape biome's habitats come from its kind, and biome by biome for arid and magic land (`tools/landscape_habitats.py`, `#jugcraft:landscape_habitat/*`). Only the menagerie reads them: trees, crops and structures there are as they were. Record: [landscape-wildlife.md](docs/features/landscape-wildlife.md).

### Unmerged: Seasonal biome audit
- **The landscape biomes now have seasons:** in the 54 temperate landscape woods, fields, hills and swamps, grass and leaves turn with the year as in Jugcraft's own biomes. 47 of them take winter snow when `seasons.snow` is on; the 7 mild ones (Mediterranean, lavender, redwood, warm shrub, orchid swamp) do not.
- **Halloween and autumn creatures reach them too:** wisps rise over the landscape swamps on Halloween nights. On full moons, werewolves stalk the landscape woods, snow werewolves its snowy land and shadow werewolves the Ominous Woods, where the Howling Totem rite also works. Squirrels and wild turkeys come to its woods and open country.
- **Every Jugcraft biome now needs a seasons decision:** the data check fails on a new biome without one, so a future collection cannot miss the seasons. The audit of every seasonal system against the newer collections is in [seasonal-biome-audit.md](docs/features/seasonal-biome-audit.md).

### Unmerged: The Pumpkin King, his Jack-o'-Straws and his loot (part 2)
- **The Pumpkin King** hangs on his post in every Crowfield and tears free when a player steps into the field. Straw burns: fire hurts him half as much again. His health scales with the party (400, up to 1000), and he fights with a Scythe Sweep, a Gourd Toss onto a ring of embers and a Crow Storm; at half health the Crowning puts him back on his post and two Jack-o'-Straws climb out of the rows, and he adds the Vine Snare and the Head Roll; below a quarter, the Last Harvest. Every attack has a wind-up to read.
- **His rule, the Harvest:** every 22 seconds (then 18, then 15) he calls two or three King's Gourds round his foe, which ripen in 10 seconds. Pick them all in time and he is bereft for 8 seconds: every blow does half as much again, fire twice, and his Jacks stand dazed. A gourd that ripens through lights and splits, and a Jack-o'-Straw climbs out (at most four; beyond that its fire heals him a little).
- **His loot,** rolled for each participant: the King's Ember (a long fuel, and with an heirloom pumpkin a King's Lantern that never goes out), a chance of one of his two new Arms VII trophies (the Hallowreap scythe and the Crowbill war pick, with the new **Fright** boon: a struck monster, never a boss, runs in panic; certain on a player's first win), the Crowward Charm (crows leave crops alone near whoever holds it in the offhand), the Pumpkin King's Crown (a costume), 300 experience shared out and the advancement Uncrowned. During Halloween, a roll of candy and a better chance of the crown; everything is kept after the event. Record: [pumpkin-king.md](docs/features/pumpkin-king.md).


### Unmerged: The Crowfield and the Harvest Crown (the Pumpkin King, part 1)
- **The Harvest Crown:** three heirloom pumpkins over two gold ingots and a hay bale. Use it at night on a scarecrow that wears a head, out under the sky, with at least 8 pumpkins or gourds growing round it: the crows burst up crying and Grey Mist takes you to the Crowfield. The scarecrow and the crops are left as they were. Like the Witching Season's rituals it works any night of the year, unless the server keeps it to Halloween (`lairs.off_season=off`).
- **The Crowfield:** a pumpkin field at dusk, walled all round with corn too thick to pass, under a vast harvest moon. Arrive in a farmyard behind a fence and farm gate; rows of pumpkins and heirlooms lie across the field, with hay stacks and a tipped cart for cover, and eight King's Gourds grow on hills in a ring round the King's Post, where the Pumpkin King will hang (part 2).
- **King's Gourds:** green until the King ripens them (part 2); any player may pick one while it ripens. Record: [crowfield.md](docs/features/crowfield.md); his plan is in [BOSSES.md](docs/branches/BOSSES.md#the-pumpkin-king-the-plan-being-built).

### Unmerged: Fixed: the server game tests slowing to a stop since the Jimtafacts batch
- The accessories' per-player check (`JimtaAbilities.tick`) read the block under every online player's feet, every time it ran, whatever they wore. Through an unloaded chunk that read loads, or even generates, the chunk on the spot. The game tests leave about 1,300 unticked mock players at the world origin, so the check ran every tick for each of them. Since the batch, the server's tick time has grown from about 10 ms to about 1 s over a test run, and CI's `mod` and `optional integrations absent` jobs have hit their 90-minute limit on main and on every pull request. This read is the only new per-player code that touches the world whatever is worn; that it caused the slowdown is the best-supported explanation, which CI's run of this change will confirm or refute.
- The ground is now read only when snowshoes are worn, and only from a chunk that is already loaded. Snowshoes behave as before.

### Unmerged: The benches in the workshop look
- The **Cooking Pot**, **Lampwright's Bench**, **Farm Stand**, **cave survey desk** and **Carving Knife** screens take the workshop machines' look: brass casing, orange glass, a status lamp by the title and keys that light up when they are on (the tool in use, Mirror, the candle, the crate being priced, a study you can cancel). The farm stand's owner is in a chip by the title; the survey desk's words are now translatable. Slots, buttons, button ids and labels are unchanged.
- The blueprint table keeps its own drafting-sheet look.
- The screen fit check now opens the five benches at the smallest GUI too. Record: [bench-screens.md](docs/features/bench-screens.md).

### Changed: The recipe page in its machine's look, and a screen fit check
- The industrial forms' **recipe page** takes the forms' industrial look instead of its own teal. It has a status lamp by "<machine>: recipes" (green while a recipe is locked, amber when the recipes were reloaded), the list and the details in panes, and keys in the look: the chosen recipe, Automatic and the locked recipe's Lock key are lit. Every function and button id is unchanged.
- The **storage tank** screen (352 wide) now scales itself down to fit a narrower window, as the reactor console and remote shutdown panel do.
- A new client game test, `ScreenFitClientGameTests`, opens every machine screen, form screen, the recipe page and the eight other machine screens at the smallest GUI Minecraft gives (320 × 240, a 640 × 480 window), checks that each fits the window, and photographs some. Record: [screen-fit-check.md](docs/features/screen-fit-check.md).

### Unmerged: Seasonal biome audit
- **The landscape biomes now have seasons:** in the 54 temperate landscape woods, fields, hills and swamps, grass and leaves turn with the year as in Jugcraft's own biomes. 47 of them take winter snow when `seasons.snow` is on; the 7 mild ones (Mediterranean, lavender, redwood, warm shrub, orchid swamp) do not.
- **Halloween and autumn creatures reach them too:** wisps rise over the landscape swamps on Halloween nights. On full moons, werewolves stalk the landscape woods, snow werewolves its snowy land and shadow werewolves the Ominous Woods, where the Howling Totem rite also works. Squirrels and wild turkeys come to its woods and open country.
- **Every Jugcraft biome now needs a seasons decision:** the data check fails on a new biome without one, so a future collection cannot miss the seasons. The audit of every seasonal system against the newer collections is in [seasonal-biome-audit.md](docs/features/seasonal-biome-audit.md).

### Unmerged: Guns, slice 15C (three diamond-steel long guns)
- **Three guns** from the owner's own GeckoLib models and animations, the next of their diamond-steel guns, each held in both hands and fed from a box magazine: the **Hawker Rifle**, a rifle of twelve rifle rounds whose magazine stands out of its side, passing a point of armour; the **Impetus Shotgun**, fed from a box of six shells, eight pellets a pull, knocking back what it hits; and the **Quill Rifle**, an automatic of thirteen light rounds, silenced the length of its barrel (no muzzle flash), passing 1.5 points of armour.
- Made from stainless steel, a diamond and a lever; planks for the Hawker and the Impetus, redstone and an iron ingot for the Quill. Each takes the magazines, stocks, grips, bayonets and scopes the owner made for it; the Quill no barrel attachment, the Hawker no Medium Scope.
- The inspect key does nothing with the Impetus, whose owner's model has no inspection.
- The owner's GeckoLib models of the three are added to the library. The guns' converter now reads bones the owner turned as a whole, and keyframes written as `{"vector": [...]}`; a gun whose owner's texture is larger than 128 px keeps its size.
- Record: [guns.md, slice 15C](docs/features/guns.md#slice-15c-three-diamond-steel-long-guns).

### Unmerged: Guns, heavy bullets: knockback and armour piercing
- A gun's bullets can now knock back what they hit further than a plain hit, and pass through some of its armour, as the owner's gun data asks. The **Jawbreaker** is the first: its shots push about twice as far as a plain hit (a second push of 0.6) and pass through 4.5 points of armour; a creature without armour takes its damage as before.
- Record: [guns.md, the heavy-bullets follow-up](docs/features/guns.md#follow-up-heavy-bullets-knockback-and-armour-piercing).

### Unmerged: Guns, no flash from a gun silenced by its build
- The **Gallant Rifle**, silenced the length of its barrel, no longer shows a muzzle flash when fired, as a gun with a Silencer fitted shows none. The guns' client test checks it.
- Record: [guns.md, the silenced-flash follow-up](docs/features/guns.md#follow-up-no-flash-from-a-gun-silenced-by-its-build).

### Unmerged: Guns, slice 15B (three diamond-steel sidearms)
- **Three guns** from the owner's own GeckoLib models and animations, the next of their diamond-steel guns, each held in one hand, and in both with a stock fitted, as a carbine: the **Kreuzer Pistol**, a quick pistol of thirteen light rounds, its magazine ahead of its trigger; the **Psalm SMG**, a machine pistol of twenty-five light rounds, ten a second; and the **Jawbreaker**, a heavy magnum of four rifle rounds, 12 a shot. Each carries a wax seal and a strip of scripture that swing as it fires.
- Made from stainless steel, a diamond and a lever; redstone for the Psalm. Each takes the barrel attachments and stocks; the Kreuzer and the Psalm the magazines, the Jawbreaker the scopes and the Laser Sight.
- The owner's GeckoLib models of the three are added to the library.
- Record: [guns.md, slice 15B](docs/features/guns.md#slice-15b-three-diamond-steel-sidearms).

### Unmerged: Guns, slice 15A (the first diamond-steel guns)
- **Three guns** from the owner's own GeckoLib models and animations, the first of their diamond-steel guns, each held in both hands and fed from a box magazine: the **Vesper Rifle**, a heavy marksman's rifle of eight rifle rounds that hits hard and reaches 144 blocks; the **Onrush SMG**, a submachine gun of twenty-three light rounds, ten a second; and the **Gallant Rifle**, an automatic rifle of seventeen, silenced the length of its barrel. The Vesper and the Gallant carry a wax seal and a strip of scripture that swing as they fire.
- Made from stainless steel, a diamond and a lever (Jugcraft has no diamond steel); planks for the Vesper and the Onrush, a Baffled Silencer for the Gallant. Each takes the magazines, stocks, grips, bayonets and scopes the owner made for it; the Gallant no barrel attachment, the Vesper no Medium Scope.
- The owner's GeckoLib models of the three are added to the library. The guns' converter now draws a GeckoLib model's parts from that model's own texture.
- Record: [guns.md, slice 15A](docs/features/guns.md#slice-15a-the-first-diamond-steel-guns).

### Unmerged: Guns, slice 14F (the Old Grange Carbine)
- **One gun** from the owner's own GeckoLib model and animations, the last of their treated-brass guns: the **Old Grange Carbine**, the Grange Carbine's older make, with a longer barrel, loaded from the top with a stripper clip of seven rifle rounds. Slower than the Grange Carbine, it hits harder, reaches further and aims straighter. Held in both hands.
- Made from steel, brass, a lever and planks. It takes the Grange Carbine's attachments but the magazines; its scopes stand on its own rail ahead of the receiver.
- The guns' converter now reads a gun the owner modelled in GeckoLib, checked against the item-model parts it shares with their Grandle. The owner's model is added to the library (`Guns/geo/item/gun/grandle_og.geo.json`).
- Record: [guns.md, slice 14F](docs/features/guns.md#slice-14f-the-old-grange-carbine).

### Unmerged: Guns, slice 14E (a flamethrower and a fire shotgun)
- **Two guns** from the owner's models and animations, the last of their treated-brass guns but an earlier make of the Grange Carbine, each held in both hands: the **Firedrake**, a short flamethrower with two fuel tanks slung under it and a pilot flame always lit, whose wide, close jet burns blaze powder and sets creatures alight; and the **Harrow**, four barrels before a drum, whose four buckshot pellets and burst of flame set what they hit alight.
- Each is made from steel, brass, a lever and a fire charge; the Firedrake two buckets for its tanks. The Harrow takes the three stocks; the Firedrake nothing.
- Record: [guns.md, slice 14E](docs/features/guns.md#slice-14e-a-flamethrower-and-a-fire-shotgun).

### Unmerged: Guns, slice 14D (a crossbow-gun, a molten rifle and a drum gun)
- **Three guns** from the owner's models and animations, more of their treated-brass guns, each held in both hands: the **Quarrel Carbine**, a repeating crossbow-gun of eight rifle rounds whose strings snap forward with each shot; the **Hinterland Rifle**, a two-pronged rifle of molten stone whose shots set what they hit alight and whose two top lids flick open as it fires; and the **Cloudburst Gun**, a drum-fed automatic of thirty rifle rounds, held and aimed at the hip as the Thresher is.
- Each is made from steel, brass and a lever, with a crossbow, a magma block or a lightning rod. The Quarrel Carbine takes the magazines, the stocks and the four sights; the Hinterland Rifle the Medium Scope and the Reflex Sight; the Cloudburst Gun nothing.
- **Fixed:** fired aimed, the Cloudburst Gun and the Borer play their shot from the hip (the owner made them no aimed shot). And guns held at the hip no longer swing an arm's sleeve across the left of the view as they fire: the guns' animations no longer ease into each other over a tick, an ease that could begin from the arms' rest pose.
- Record: [guns.md, slice 14D](docs/features/guns.md#slice-14d-a-crossbow-gun-a-molten-rifle-and-a-drum-gun).


### Unmerged: The Exchange (bank, markets, land and shares, in Jugs)
- Press **N** (or `/jugcraft exchange`) for the **Exchange**, a walnut-and-brass ledger with seven tabs. It deals only in **Jugs**, the town's credit: no new currency.
- **Banking and loans:** a one-time **starter loan** (100 Jugs) for new players, a flat 10% fee, due in 10 days. While you owe, a quarter of your market earnings repays the bank; overdue borrowers may sell but not buy. Paying off on time raises your credit limit.
- **Contracts:** pay another player once, daily or weekly, with what they promise in return.
- **Commodities and Requests:** list items at a fixed price or by auction (the Exchange holds them, so sales go through while you are away), as yourself or for a company you control; post paid buy requests or adverts. Daily inflation from traded prices.
- **Market Chest:** a clerk's counter a company's controller buys from the Exchange. Goods put in it are listed for the company at the prices set on its screen; sales go to the treasury.
- **Land:** a vanilla-style map of the Overworld chunks around you; buy chunks (25 Jugs, 10% more for each you own), sell them at a price or by auction, trust builders. Claimed land is protected; the town cannot be claimed.
- **Equities:** charter a company (250 Jugs) with a logo you draw or upload, offer shares to the public, issue more later, trade them on a bid/ask book; the majority holder pays dividends and runs company land.
- **Poker tables:** three craftable 4x2 tables (the Gilded Oval, the Saloon and the Clockwork Dealer, vanilla recipes) that deal Texas hold'em for Jugs to up to six players, with blinds, side pots and a turn timer. Chips are Jugs held in escrow.
- **Casino:** craftable blackjack, craps, American roulette and baccarat tables, ten slot machines (Fruit, Clockwork, Gem Mine, Progressive Jackpot, Nether Furnace, Sunken Treasure, Creeper Blast, Pub Fruit, Shark Frenzy, Golden Dragon), a Casino Arcade (video poker, keno, mines, crash, hi-lo), a Plinko board, a prize wheel, a coin pusher, a lottery kiosk selling tickets and scratch cards, and a Derby Table where miniature horses race under a dome, with casino rules and real odds. The machines glow and sound like real ones, each with its own themed screen; owners set limits, clocks and who is the house, and see their takings. The player who places a game is the house; the central bank lends an owner who cannot pay a winner what they lack.
- **Server settings:** operators change every Exchange number (fees, loan terms, sales tax, land prices and limits, company limits, poker blinds and buy-ins, casino limits and clocks) from the Economy tab's Admin page or `/jugcraft exchange admin set`.
- The **Exchange Guide**, a book of its own (given on first join, or `/jugcraft exchange guide`), explains it all. Record: [finance-exchange.md](docs/features/finance-exchange.md).

### Fixed: the Highwayman's recipe
- **The Highwayman now takes a second brass ingot,** under the first. Its grid was the Garrison Rifle's mirrored, and a crafting table matches a shaped grid either way round, so only one of the two guns could be made from it.
- Record: [guns.md, slice 9D's crafting](docs/features/guns.md#slice-9d-the-pump-shotguns).

### Added: Guns, slice 14C (the brass quick-firers)
- **Three guns** from the owner's models and animations, more of their treated-brass guns, each held in both hands: the **Sprocket Gun**, an automatic rifle of forty rifle rounds fed from a pan magazine on its top, which its reload lifts off, sets back and winds; the **Navvy Shotgun**, the first automatic shotgun, eight shells of six pellets in a box magazine; and the **Freeholder Rifle**, a rifle of twenty rifle rounds that fires a pull at a time quicker than any other.
- Each is made from steel, brass and a lever, the Sprocket Gun with a hopper and the Freeholder Rifle with an emerald. The Sprocket Gun and the Navvy Shotgun take the barrel attachments, the stocks, the grips (the Tactical Grip among them) and the bayonets, the Navvy Shotgun its magazines too; the Sprocket Gun takes the four sights, the Freeholder Rifle its magazines and every sight but the Long Scope, and the Navvy Shotgun no scope.
- Record: [guns.md, slice 14C](docs/features/guns.md#slice-14c-the-brass-quick-firers).

### Added: Guns, slice 14B (the brass carbines)
- **Three guns** from the owner's models and animations, more of their treated-brass guns, each held in both hands: the **Holler Carbine**, the Holler Pistol made over with a long shrouded barrel, ten rifle rounds and a bolt its shot works, so it fires more than twice as fast; the **Minuet Carbine**, the Minuet Pistol made over with a long barrel, ten light rounds in a magazine ahead of its trigger and a lever under its barrel; and the **Grange Carbine**, a carbine of twelve rifle rounds fed from a box magazine.
- Each is made from steel, brass and a lever. The Holler Carbine takes no attachment (the owner made none for it); the Minuet Carbine takes the stocks and the four sights; the Grange Carbine takes every attachment the owner made for it, the Tactical Grip among them.
- The owner's Howler Conversion has no magazine part of its own, so the Holler Carbine shows its Howler's (`tools/guns.py` can now take a part from another of the owner's guns).
- Record: [guns.md, slice 14B](docs/features/guns.md#slice-14b-the-brass-carbines).

### Added: Guns, slice 14A (the brass sidearms)
- **Three guns** from the owner's models and animations, the first of their treated-brass guns: the **Holler Pistol**, a heavy pistol of ten rifle rounds held in both hands, fed from a magazine ahead of its trigger, its bolt worked after each shot; the **Minuet Pistol**, a light target pistol of ten light rounds held in one hand, the truest of the one-handed guns when aimed; and the **Half-Crown Revolver**, a break-top revolver of six rifle rounds loaded one at a time, held in one hand, which with a stock fitted is held in both as a carbine, as the Parley Revolver is.
- Each is made from steel, brass and a lever, the Half-Crown Revolver with a silver ingot too. The Holler Pistol takes the barrel attachments, both magazines, the stocks, the grips, the bayonets and every sight but the Long Scope; the Minuet Pistol the barrel attachments, the Extended Magazine and the four sights; the Half-Crown Revolver the barrel attachments and the stocks.
- Record: [guns.md, slice 14A](docs/features/guns.md#slice-14a-the-brass-sidearms).

### Added: Guns, slice 13D (a revolving launcher, a carbine revolver and a musket of light)
- **Three guns** from the owner's models and animations: the **Fencepost Thumper**, a revolving grenade launcher of four grenades loaded one at a time, its barrel tipped down to load and its pump turning the cylinder after each shot; the **Parley Revolver**, a heavy single-action revolver of six rifle rounds held in one hand, which with a stock fitted is held in both as a carbine and plays the owner's carbine animations; and the **Lantern Musket**, an energy musket whose beam passes through every creature in its line, three shots to a core.
- The Fencepost Thumper is made from iron, brass, a lever and a plank; the Parley Revolver from steel, iron, brass and a lever; the Lantern Musket from steel, a lantern, a plank, an advanced circuit and brass. The Thumper takes the Light and Vertical Grips and the bayonets; the Parley Revolver the barrel attachments and the stocks; the Lantern Musket nothing.
- A carbine (the Parley Revolver with a stock) is not one of two guns held at once.
- Record: [guns.md, slice 13D](docs/features/guns.md#slice-13d-a-revolving-launcher-a-carbine-revolver-and-a-musket-of-light).

### Added: Guns, slice 13C (a punt gun, a repeater and a heavier rifle)
- **Three guns** from the owner's models and animations: the **Flatboat Gun**, a punt gun whose chamber swings out to take one shell, one blast of sixteen pellets and a long reload; the **Windlass Musket**, a flintlock repeater of seven paper cartridges, its drum turned between shots and its magazine changed whole; and the **Millstone Rifle**, a heavy lever-action rifle of eight rifle rounds loaded a round at a time, slower than the Drover Rifle and harder hitting.
- The Flatboat Gun is made from a block of iron, iron, brass, a lever and a plank; the Windlass Musket from iron, brass, a flint, a lever and a plank; the Millstone Rifle from steel, iron, brass, a lever and a plank. The Flatboat Gun takes the scopes and the Laser Sight; the Windlass Musket the Light and Vertical Grips and the bayonets; the Millstone Rifle what the Drover Rifle takes.
- Record: [guns.md, slice 13C](docs/features/guns.md#slice-13c-a-punt-gun-a-repeater-and-a-heavier-rifle).

### Added: Guns, slice 13B (more frontier guns)
- **Three guns** from the owner's models and animations: the **Fencepost Carbine**, a lever-action carbine of seven rifle rounds loaded a round at a time through a lid in its stock; the **Waystation Shotgun**, a lever-action shotgun of eight shells with a drum under its action, loaded a shell at a time; and the **Highball Rifle**, a single-shot rifle whose barrel swings aside to be loaded, one heavy, true shot at long range.
- The Fencepost Carbine and the Waystation Shotgun are made from iron, brass, a lever and planks, as the lever guns; the Highball Rifle from steel, a lever and a plank. Each takes the grips and the bayonets; the shotgun and the rifle the barrel attachments too, and the rifle the scopes and the Laser Sight.
- The owner's Fencer Thumper waits for its cylinder to turn with each shot.
- Record: [guns.md, slice 13B](docs/features/guns.md#slice-13b-more-frontier-guns).

### Added: Guns, slice 13A (the frontier guns)
- **Three guns** from the owner's models and animations: the **Tabby Revolver**, a gunmetal revolver held in one hand, its cylinder swung out to load a round at a time, tossed end over end as it is drawn; the **Lookout Rifle**, a lever-action rifle of eight rifle rounds loaded a round at a time, made to take a scope; and the **Ammonite Carbine**, a carbine of sixteen rifle rounds from a long curved magazine, its barrels caged in copper bands. The Tabby Revolver pairs with any other one-handed gun.
- The Tabby Revolver is made from iron, brass, a lever and a plank; the Lookout Rifle from iron, brass, a lever and planks; the Ammonite Carbine from copper, iron, a lever and a plank. The Tabby Revolver takes the barrel attachments; the Lookout Rifle the stocks, grips, bayonets, scopes and Laser Sight; the Ammonite Carbine the stocks.
- Record: [guns.md, slice 13A](docs/features/guns.md#slice-13a-the-frontier-guns).

### Unmerged: The other machine screens in their looks
- **Eight more screens take the look of their tech level**, with the machine screens' status lamp, plain status and help:
  - the prospector in the workshop look;
  - the rocket pad and the storage tank in the industrial look;
  - the logic controller, reactor console and remote shutdown panel in the advanced look;
  - the drone depot terminal and drone tower in a drone variant of it (the tower's graphite and red).
- **Simpler to use:**
  - the rocket pad gets hint icons and help on its slots and a Launch key, with the last result by the title;
  - the logic controller gets headings, keys with tooltips, lit rules and channel readings;
  - the storage tank's readouts are grouped, with a fill bar and an outlet lamp;
  - the drone keys light when they can be used.
- Buttons, slots, menus and saves are unchanged. Record: [other-machine-screens.md](docs/features/other-machine-screens.md).

### Unmerged: Machine screens in five looks by tech level
- **Every machine screen takes the look of its tech level**, after the owner's five references:
  - workshop brass and orange for the first machines;
  - industrial steel and green for steel, oil and the industrial forms;
  - an electric blue device for the power gear;
  - a lab blueprint for chemistry;
  - an advanced cyan hologram for electronics and nuclear fuel.
- **Simpler to read:**
  - a status lamp by the title;
  - one word for what the machine is doing, and the fix when it is stuck ("Connect a cable or a generator.", "Take the results from OUT, or turn on Eject.");
  - progress with the time left;
  - a help box saying what the machine is for.
- **Controls are icon buttons with tooltips**, along the top of the help box. The six face buttons move behind one Sides button, which shows the faces with a colour key. Batteries and solar panels show a big charge gauge with its percentage. Empty slots show IN and OUT labels and hint icons, and say what goes in them when hovered. The decorative keyboard is gone.
- Industrial forms share the layout: pause, cancel, recipes and vent or drain icons, and their tool sockets in the help box. Slot positions, menus and saves are unchanged. Record: [machine-screens.md](docs/features/machine-screens.md).

### Added: The Alpha Werewolf (boss 8, part 2)
- **The Alpha Werewolf,** the Moonlit Ring's boss: the oldest of the werewolves, twice the height of a man, a silver-grey mane over a hunched black back, a scarred muzzle, a torn ear and eyes that burn moon-white. He crouches on the Howling Stone and wakes when a player who may fight him steps off the camp onto the turf, or strikes him: 400 health, scaled for the party, armour 8. His hide halves every blow but silver's. His hunting ground is the clearing, its spokes and the bell clearings; the camp is safe.
- **His pack:** three **Packmates**, young werewolves drawn as the brown werewolves are but smaller, lope out of the trees as he wakes: a 4-damage bite, no hide, silver half as much again. A sprig of wolfsbane in hand keeps them from biting. Killed, they stay dead until the Pack Gathers.
- **His rule, the Hunt:** every 24 seconds he marks his quarry with a short howl (named on his bar), bounds off to hide in a bell clearing that is not their nearest, tearing a **trail of raked turf** as he goes (a new lair-only block that knits back to what it was 30 seconds later), and lies in wait out of reach for 10 seconds; then a shadow spreads under the quarry and he springs onto it for 14, knocking them flat. **Ring the Warden Bell of his clearing first** and he is flushed: he staggers out, his hide fails for 8 seconds (silver half as much again) and his pack cowers. Any other bell tolls over an empty clearing. A sprinting player anywhere in the clearing reaches any bell in time, as the audit walks.
- **The Chase:** the Claw Rake, the Pounce (onto a shadow) and the Rending Howl (Weakness, and his pack runs faster). **At half health the Pack Gathers:** he howls on the stone and his pack is called back up to three. **The Moon's Fury** adds the Tree Leap (gone into the canopy, then dropping onto a shadow behind his foe) and the Hamstring Bite. **Below a quarter, the Last Hunt:** 3 seconds into every ambush he slips to another clearing, leaving a second trail.
- **His loot, each participant's own:** the **Alpha Pelt** (four leather, or a Howling Totem), a trophy (Moonfang or the Howler, Arms VII's, certain on a first kill), the **Packfinder Charm** (in the offhand, every werewolf within 32 blocks glows), the **Alpha's Hood** (a costume), 300 experience shared, and the advancement Hunter's Moon. When he falls, Grey Mist opens on the Howling Stone.
- 13 server game tests and a client test with 12 pictures; `tools/check_mod_data.py` checks him against `tools/alpha_werewolf.py` and the ring. Record: [alpha-werewolf.md](docs/features/alpha-werewolf.md).

### Added: The Moonlit Ring and the Howling Totem (boss 8, the Alpha Werewolf, part 1)
- **The Moonlit Ring,** an eighth lair: a clearing 44 blocks across in an old wood under a huge full moon, walled by the trunks of giant oaks too close to pass between, their canopy of Moonwood Leaves closing overhead round the edge and opening onto the moon. A path of trodden earth runs round it and spokes run from the Howling Stone at its centre (a mossy outcrop three high) to three bell clearings, west, north and east, and to the Hunters' Camp at its south edge, where you arrive: a lean-to of hides, a cold firepit, werewolf rugs and a lantern on a stump, the trail behind it full of Grey Mist. Fallen logs and boulders are its cover. Every open cell of the clearing and the bell clearings has block light 7 or more.
- **Its fixtures:** a **Warden Bell** in each bell clearing, an old silver alarm bell under a little roofed frame, which any player may ring: it tolls across the ring and swings for two seconds. Until part 2's Alpha hunts there, ringing one does nothing more.
- **The Howling Totem** (a werewolf's pelt of any kind, three bones and two silver ingots) opens it: use it at night in the Overworld's woods, under the open sky, standing on the woodland floor, with no wolfsbane within six blocks. Any night, not only a full moon's, all year. It howls, something far off answers, and you come to in the camp; anyone who uses the Grey Mist where it was used follows for a minute. By day, out of the woods, under a roof or leaves, off the woodland floor, near wolfsbane or outside the Overworld it says why and is kept. Record: [moonlit-ring.md](docs/features/moonlit-ring.md).

### Added: The Iron Dreadnought (boss 7, part 2)
- **The Iron Dreadnought,** the Foundry Trench's boss: a walking fortress of riveted gunmetal on two piston legs, tesla coils on his shoulders, a rivet battery, a mortar in his back and a searchlight for an eye. He stands dormant in his assembly gantry and wakes when a player who may fight him stands on the pit's floor, or strikes him: 440 health, scaled for the party, armour 12. His arena is the pit and the gantry; the dugout and the fire trench are safe.
- **His rule, the discharge:** while his vents are shut his plating lets only a quarter of a blow through. Every 20 seconds he charges his coils for four and a half seconds and the pylon nearest his foe lights (crown violet, breaker lamp amber, named on his bar). Throw its breaker and the bolt is earthed: he overloads, his vents open for 6 seconds and every blow lands in full. Leave it, and it runs down the pylon's lane: 8 damage within 3 blocks of it.
- **The Advance:** the Piston Punch, the Rivet Fan (cover stops it), the Trench Charge (into anything solid, he is stunned with a vent knocked open) and the Coil Arc, which jumps between players who stand close. **At half health, the Torn Plate:** he tears a plate from his hull and fouls a breaker (three uses knock it loose), and from then on two pylons light. **The Overcharge** adds the Mortar, the Blowdown and his **Sparkers**, little coil crawlers that discharge against whoever they reach. **Below a fifth, Red Line:** his vents jam open and he hastens.
- **His loot, each participant's own:** Salvaged Coil (cheaper Sealed Orders, or redstone), a trophy (the Dynamo Halberd or the Piston Hammer, Arms VII's, certain on a first kill), the **Grounding Strap** (in the offhand, lightning, power lines, Tesla guns and his arcs do half: the new damage type tag `#jugcraft:electric`, applied by a small mixin), the **Dreadnought Visor** (a costume), 300 experience shared, and the advancement Grounded. When he falls, Grey Mist opens in his gantry.
- 19 server game tests and a client test with 14 pictures; `tools/check_mod_data.py` checks him against `tools/iron_dreadnought.py` and the trench. Record: [iron-dreadnought.md](docs/features/iron-dreadnought.md).

### Added: The Foundry Trench and Sealed Orders (boss 7, the Iron Dreadnought, part 1)
- **The Foundry Trench,** a seventh lair: a ruined test pit sunk behind the front line under a sky of drifting smoke, 47 blocks by 41 of blast-scorched concrete eight blocks below the rim, walled with reinforced concrete and blast doors. Three grounded pylons of steel lattice stand at its west, east and south, each crowned with a tesla coil, a breaker at its foot facing the pit and a copper ground terminal in the floor before it, with a hazard-striped lane from the pit's middle to each. Three concrete bastions and a landship's burnt-out hull are its cover; steel catwalks run along its side walls, and the Dreadnought's assembly gantry stands in its north wall. A fire trench of sandbags and duckboards runs along its south rim, a ramp of sandbag steps goes down into the pit, and the dugout off it is where you arrive and where Grey Mist waits in the back doorway. Every open cell of the pit's floor has block light 7 or more.
- **Its fixtures:** the breakers, which any player may use; until part 2 there is no current, and a breaker only sparks. The crowns, breakers' lamps and terminals can burn live, and a breaker can be thrown or fouled by a plate of armour: part 2's fight does all of it.
- **Sealed Orders** (three paper, a steel plate, copper wire and a honeycomb) open it: use them on a Field Telephone that stands in a trench (four or more sandbags, revetments or duckboards round it) under the open sky, at any hour. The bell rings, a whistle shrills, and you go over the top; anyone who uses the open line or the telephone follows for a minute. Under a roof, out of a trench or outside the Overworld they say why and are kept. Record: [foundry-trench.md](docs/features/foundry-trench.md).
### Added: The Cydonian Sentinel (part 3 of the owner's Mars boss)
- **The Cydonian Sentinel,** a construct four blocks tall of red Martian stone and rusted iron round a core of red light, waits on its plinth on the Sanctum's crater rim and steps down when a player stands on the crater's floor. 480 health, scaled for the party; armour 12.
- **Its rule, the Red Storm:** every 35 seconds (sooner in each later phase) it plants its arms and the storm's side is named; then a scouring wind crosses the crater for 6 seconds, hurting and pushing everyone not in the lee of a standing stone or the rim. A sheltered place is always within 12 blocks.
- **Its weak point, the core:** charging its Core Lance its chest plates open: the core takes double harm, and a strong blow in the wind-up staggers it.
- **Three phases and an enrage:** the Sentinel (Hammerfall, Shard Volley, Quake Line, Core Lance); the Shell Cracks and the Red Storm (Shard Sentries, Sentry Call, Storm Shards); the Unbound Core (Sweeping Lance, Shard Burst, Starfall, then it sinks for a moment); and the Red Giant (a third quicker, storms every 15 seconds).
- **Its loot, per participant:** Cydonian Shards (a cheaper Altar Keystone, or rare earth oxide from the Crusher), the Lowfall Core (fall slowly in the offhand), the Sentinel's Visor (a costume), 400 experience, the advancement Red Planet, and a chance of a new Arms VII trophy with the **Rust** boon (a foe's armour counts 30% less for 4 s): the **Corelance**, a pike, or the **Redstar Cleaver**, a battle axe. The Red Mist home opens in the crater when it falls. Record: [cydonian-sentinel.md](docs/features/cydonian-sentinel.md).

### Added: The Cydonian Sanctum and the Altar Keystone (part 2 of the owner's Mars boss)
- **The Altar Keystone,** made in the factories (titanium, tungsten, an advanced circuit) or by the Concordance (dreamglass and an astrolabe). Set into the Ancient Altar at night, the three altars kindle with red fire, a column of red light rises, and the **red mist** opens onto Mars for 60 seconds.
- **The Cydonian Sanctum,** a lair on Mars under a butterscotch sky with two moons: thin air (Jump Boost I, half fall damage), a gate chamber with the Red Mist home, the **Dust Gallery** with dust vents that puff on a rhythm and three **star dials** that light the **Star Door**'s constellations and grind it open, and the crater where the Cydonian Sentinel waits (part 3). Record: [cydonian-sanctum.md](docs/features/cydonian-sanctum.md).

### Added: Adam-ondi-Ahman, the valley and its altar (part 1 of the owner's Mars boss)
- **A quiet valley near the start of every new world,** 800 to 1,600 blocks away: rolling prairie grass, wildflowers and goldenrod, lone oaks and birches, grazing animals and few monsters, with four seasons. **Rare valleys** grow elsewhere (`adam_ondi_ahman.spacing`, `adam_ondi_ahman.chance`). Modelled on the real Adam-ondi-Ahman in northern Missouri.
- **A knoll of three stacked stone altars** at each valley's heart, with a path up it, the log footings of two cabins, a fallen chimney, a spring and a ring of gathering stones at its foot. On the top lies the **Ancient Altar**, carved with a red star: unbreakable, and dormant until part 2's Altar Keystone wakes it.
- The plan for all three parts, the Mars dungeon and the Cydonian Sentinel included: [branches/BOSSES.md](docs/branches/BOSSES.md). Record: [adam-ondi-ahman.md](docs/features/adam-ondi-ahman.md).


### Added: The Howitzer Walker, playable
- Owner accepted the updated local playtest on October 13: aim without holding fire, no close-ground aim freeze, and hide the pilot model/equipment while mounted. Implementation and correction record: [Howitzer Walker](docs/features/howitzer-walker.md).
- **The owner's Howitzer Walker is in the game:** a tall, two-legged artillery walker in riveted olive plate with a big howitzer in an open cradle, made from a Diesel Walker beside the Armoured Walker (`SDS / PWP / R R`: two steel blocks, a dispenser, steel plates, the Diesel Walker and pistons). It walks, turns, climbs and refuels as the other walkers do.
- **Mouse aims; hold use to fire the howitzer:** on the pilot's own Range Finder mark, else on the block they look at. The whole walker turns to face it and the barrel elevates (15 degrees down to 60 up) to the arc that lands a Heavy Shell there, about 115 blocks at most. It fires once laid, every 4 seconds, a shell from the pilot's inventory; the blast is damage-only, as every gun's.
- **Jump** crouches, then springs about three blocks up. **Attack stomps:** a hop (or, in the air, a slam) whose landing hurts and throws everything within 3.5 blocks for 14 damage.
- **Its animations, drawn from the owner-reviewed clips:** the floaty, bouncy walk; the shot's recoil, rocking hull, cartoon star burst and rolling smoke puffs; and the jump's crouch, tuck and squashy landing, fitted to the real leap. Game tests and a screenshot scene. Record: [howitzer-walker.md](docs/features/howitzer-walker.md).

### Unmerged: Formed-machine screens keep the POWER label on large stores
- The terminal's power line dropped its "POWER" label once the stored and total power were too wide to fit beside it. The Gas Burning Generator's 120,000 JE store hit this whenever it held 10,000 JE or more. The line now shows the stored power alone in that case ("POWER 120k"); the bar and its tooltip still give the store's size.
- The foundation, Separator and generator records now say they are merged, and the generator's record gives its CI results. Record: [industrial-gas-burning-generator.md](docs/features/industrial-gas-burning-generator.md).

### Changed: The Cinder Kiln lit brighter, and the Cinder Tyrant's seams widened
- **The kiln's bowl is lit.** Its hidden lights hung three blocks over the floor, eight apart and at level 11, which left the floor between them as dark as 0 and half of it at 4 or under. Now a staggered lattice at full strength hangs two blocks over it, so every cell of the bowl's floor has at least 8 (10 at the middle of its range, 13 under a light), and the ring round the bowl is as bright. The audit floods the kiln's light as the game does and holds the floor to 8, and the client test reads the game's own light there. Record: [cinder-kiln.md](docs/features/cinder-kiln.md).
- **A hot Tyrant reads across the kiln.** His seams were a model pixel wide and flush between his plates; now the magma wells a pixel over the joints between his back plates and his tail's, rims the foot of his plates and frames his flank plates, painted brighter. Quenched, all of it goes dark, so hot and quenched are told apart at a glance. The Molten Heart's cores wrap the wider seams. Record: [cinder-tyrant.md](docs/features/cinder-tyrant.md).
- The owner chose both at the Tyrant's checkpoint ("Go with recommendations"). His fight, numbers and loot are unchanged.

### Added: The Crypt Lich (boss 6, part 2)
- **The Crypt Lich,** the Archive of Last Names' boss: a crowned skeleton a head taller than a man, in a scholar's robe gone to mould hung with chains of tarnished gold, eyes of cold blue flame, a crown of black iron with soul-fire on its points, and a thighbone staff. He waits on his throne and wakes when a player stands on the hall's floor or strikes him: 400 health scaled for the party, armour 8, never harmed by Wither, poison, water or falls. Left alone, he returns to his throne, healed, and the names are cut again.
- **His rule, the phylactery:** soul chains run from the three grave seals to his phylactery and to him, and while they hold every blow is turned aside. One name at a time is lit, on its stone, its seal and his bar; use its seal and it breaks, and the next lights. A dark name's seal holds and a Restless Dead climbs out of that alcove's sarcophagus. Break all three and the phylactery cracks: he is exposed for 12 seconds, then the chains reforge in a new order.
- **The Warded King:** Grave Sweep, Bone Lanes (bones burst up the lane he calls), the Soul Bolt (strike it back to stagger or hurt him) and Raise the Dead (at most four walk). **At half health, the Erasure:** he scrapes a name from its stone, and its seal holds until it is read back at the Lectern of Last Names (or 30 seconds pass). **The Unwritten:** two lanes at once, the Soul Drain (a pillar breaks it) and the Grave Pull (its dark part is the way out). **Below a fifth, the Last Name:** the phylactery shatters, his chains are gone, and he is faster and raises his dead every 12 seconds.
- **His loot, each participant's own:** Grave Dust (a cheaper Writ of Exhumation, or bone meal), one of Arms VII's Soulreaver or Gravewarden (now dropping; certain on a first kill), the Emptied Phylactery (in the offhand, Wither never takes hold of you) and the Lich's Crown (a costume), with shared experience and **Nameless**. Breaking a seal, reading a name back or striking back a bolt counts as taking part. When he falls his crown rolls from his skull and Grey Mist opens on the dais where the phylactery stood. Record: [crypt-lich.md](docs/features/crypt-lich.md).

### Added: The Archive of Last Names and the Writ of Exhumation (boss 6, the Crypt Lich, part 1)
- **The Archive of Last Names,** a sixth lair: a vaulted crypt deep under the graves, a hall 41 blocks square under a ribbed vault of deepslate, its walls bays of bookshelves and ossuary niches, soul lanterns hung on chains, four pairs of pillars in its quarters and ledger stones in its floor. Four lanes paved with polished deepslate and inlaid with bone run from the dais at its heart to three alcoves (the Sexton's, the Crowned's behind the bone throne, the Scribe's) and to the Lectern of Last Names at the stair's foot. Grey Mist fills the stair's way back up behind the landing. Every open cell of the hall's floor has block light 7 or more.
- **Its fixtures:** in each alcove a name-stone carved with the name's rune (a spade, a crown, a quill), dark, lit or scraped blank, over a grave seal of red wax pressed with the same rune; the phylactery on the dais, an urn of bone and gold burning with soul-fire; and the Lectern of Last Names, which reads the names and cuts a scraped one again. In part 2 the Crypt Lich binds the seals and scrapes the names.
- **The Writ of Exhumation** (three paper, two bones, a bottle of ectoplasm and an ink sac) opens it: serve it on a stone sarcophagus in a crypt, under a roof, at any hour. The lid grinds aside, blue fire and grey mist well up, and you go down the stair; anyone who uses the mist or the open tomb follows for a minute, and the lid grinds shut when the mist is gone. Under the open sky or outside the Overworld it says why and is kept.

### Added: The Mire Hag (boss 5, part 2)
- **The Mire Hag,** the Lantern Fen's boss: a crone of the deep fen bent nearly double, in a shawl of grey moss over rotted sackcloth, eyes of marsh-light green, leaning on a staff of bog oak with a little lantern swinging from its crook. She waits on her porch over her cauldron and wakes when a player comes into the marsh or strikes her: 360 health scaled for the party, armour 6, never harmed by poison, water or falls. Left alone, she climbs back onto her porch, healed, and the lamps burn again.
- **Her rule, the light finds her:** her **Likenesses** walk the fen with her, two at first, each just as she is but casting no shadow and leaving no ripple. Any blow or anything thrown bursts one into marsh gas, and it forms again 10 seconds later. In a lamp's light her Likenesses fade (none walks into it of its own accord) and her outline shows; light a lamp that has gone out and she is shown for 5 seconds wherever she is.
- **The Lure:** Clawing Swipe, Root Lanes (roots burst along three lanes and hold you fast), the Poison Brew (a pool of venom; its Poison never kills) and the Likeness Exchange. **At 55%, Lights Out:** two lamps gutter out and a Root Knot crawls along the ring to put out a third, unless it is cut down. **The Drowned Dark:** up to four Likenesses, the Mire Grasp and the Snuff. **Below a fifth, the Last Light:** her lantern flares, her Likenesses are gone and she is faster.
- **Her loot, each participant's own:** Hagmoss (a cheaper Hag Stone, light grey dye or compost), one of Arms VII's Hagthorn or Bogfang (now dropping; certain on a first kill), the Hag's Eye (in the offhand, invisible creatures near you glow) and the Hag's Shawl (a costume), with shared experience and **Seen Through**. When she falls her lantern goes out and Grey Mist opens in the marsh's middle. Record: [mire-hag.md](docs/features/mire-hag.md).

### Added: The Lantern Fen and the Hag Stone (boss 5, the Mire Hag, part 1)
- **The Lantern Fen,** a fifth lair: a drowned fen floating in a night of mist, black water knee-deep over mud in a marsh 44 blocks across, tussocks and drowned stumps in it, dead willows hung with shroud moss on the banks round it, motes of marsh-light in the air and low mist over the water. Boardwalks of willow planks a block over the mud ring the marsh, join four lamp platforms and run north to the Mire Hag's hut on stilts, her cauldron on its porch, and south down a jetty to the arrival, a punt moored at its end; the Grey Mist home hangs between two willows behind it.
- **Four fen lamps,** the fen's working fixture: great iron lanterns on willow posts, burning with a yellow-green marsh-light at 15. Any player in the fen lights one that has gone out; in part 2 the Hag puts them out. Every open cell of the marsh has block light 8 or more.
- **The Hag Stone** (a flint, string, two mud, two gold and a fermented spider eye) opens it: at night, standing in shallow water or on mud in any swamp, the Floodplain or the Muskeg, look through it. A lantern burns far off, the water stills and mist rises from it, which others can follow through for a minute. Refused, it says why and is kept. The Hag herself and her loot are part 2. Record: [lantern-fen.md](docs/features/lantern-fen.md).
- **The data audit now finds a tag named inside a biome tag** (the stone's lands name `#c:is_swamp`); it looked for it among the wrong folder.

### Added: Guns, slice 12C (the lone guns)
- **Three guns** from the owner's models and animations: the **Tuppence Pistol**, a copper pistol held in one hand, the tube under its barrel loaded a round at a time; the **Goshawk Revolver**, a heavy iron revolver held in one hand, its cylinder swung out to load a round at a time; and the **Lament Rifle**, a sculk-grown bolt-action rifle of five rifle rounds, the hardest-hitting and furthest-reaching of the rifles. The two hand guns pair with any other one-handed gun.
- The Tuppence Pistol is made from copper, iron and a lever; the Goshawk Revolver from iron and a lever; the Lament Rifle, as the sculk guns, from steel, a lever, echo shards and sculk. The Tuppence Pistol takes the stocks, scopes and Laser Sight; the Lament Rifle the grips and bayonets too; the Goshawk Revolver none.
- The owner's Whizzbanger waits for the model its animations were made for.
- Record: [guns.md, slice 12C](docs/features/guns.md#slice-12c-the-lone-guns).

### Added: Guns, slice 12B (the copper guns)
- **Three copper guns** from the owner's copper models and animations, early guns of copper and iron as the Rust Midge is: the **Penny Pistol**, held in one hand (so it pairs with another one-handed gun), fed from a long magazine at its left side; the **Conductor Carbine**, a slide-action carbine of eight rifle rounds, its slide worked after every shot; and the **Magpie SMG**, an automatic of thirty-two light rounds from a long magazine at its left side.
- Each is made from copper ingots, an iron ingot and a lever; the Penny Pistol's recipe fits the inventory's own grid. The Penny Pistol takes the barrel attachments and the stocks; the other two the grips, bayonets, scopes and Laser Sight too, and the Conductor Carbine the magazines.
- Record: [guns.md, slice 12B](docs/features/guns.md#slice-12b-the-copper-guns).

### Added: Guns, slice 12A (the End guns)
- **Three End guns** from the owner's purpur models and animations, each fed from a shulker box with the shulker peering out of it, whose bullets make what they hit float for a second, as a shulker's do (the game's own Levitation; each hit starts it again): the **Chitin SMG**, an automatic of twenty-four light rounds; the **Hermit Rifle**, an automatic of twenty rifle rounds, its muzzle in a curled shell; and the **Borer**, a machine gun of forty rifle rounds carried at the hip, charged with a crank.
- Each is made from steel, a lever, a shulker shell (two for the Borer) and a purpur block. The Chitin SMG takes the scopes and Laser Sight, the Hermit Rifle the stocks too; the Borer takes none.
- Record: [guns.md, slice 12A](docs/features/guns.md#slice-12a-the-end-guns).

### Unmerged: Arcane Concordance, Chunk A (compose without commands, support shares a lair boss's reward, the encyclopedia's first route)
- **The Spell Composer:** hold a wand, press **J** and choose **Compose** (or bind *Open the Spell Composer* in Controls; it starts unbound). Pick from the words you have learned, by kind, or type the spell; words not yet learned are counted, never named. A moment after you stop editing, the server checks the spell and says what it does and costs, or every reason it cannot work, marking the words concerned. **Inscribe** writes on the wand exactly the spell it checked, and it joins the spell bar as before. With GuiLib it opens as a workspace; without it, or with *Simple journal* chosen, as a plain screen the narrator reads, worked from the keyboard. The `/jugcraft concordance compose` commands answer through the same service and stay as a diagnostic path. Record: [arcane-concordance-composer.md](docs/features/arcane-concordance-composer.md).
- **Support earns a lair boss's reward:** at Vesperine and Madame Tatterlace a player now qualifies for their own roll of the loot by healing another player, with the Concordance, 4 of the hurt the fight's creatures did them; by a Concordance ward on another player that soaks 4 health of those creatures' blows (`touch struck ward` is one); by relighting one of Vesperine's wards in her Harvest; or by cutting another player free of Madame Tatterlace's Binding Thread; as well as by any blow, as before (the Yeti King and the Cinder Tyrant join them in Chunk B, above). Healing or warding yourself, and healing past full health, count for nothing. One who died in the fight is paid once back on their feet, one who left the game in it when they next join, and each fall pays each player once. Record: [lair-encounter-credit.md](docs/features/lair-encounter-credit.md).
- **The Jugcraft Encyclopedia's first route:** press **B** (remappable), or the scroll button beside the recipe book in your inventory, to open the encyclopedia; no item is needed. Its Discover page runs from a glowing specimen to a spell of your own and a ward for a friend: each step says whether it is done, next, can be done now, later or not available on this server, and its ways show the server's own recipes as crafting grids, the town's sales where a town stands, and the codex entries you have opened. Spellcraft shows the wand in hand and what you can cast and compose; a lair's dossier appears once you carry its token; opened while looking at a Circle Anchor or Ley Pylon, it shows that device's state; a Keys page lists every Jugcraft key. Research, words and spells you have not met are not named (First Light, the route's own, is), and a lair stays hidden until you carry its token. Record: [arcane-concordance-first-route.md](docs/features/arcane-concordance-first-route.md).
- **How the first loadout is won:** the first magic loadout (the Initiate's Wand, a Kindled Lantern, the Pyromancer's or Pyromaniac's set, the Lesser Focus of Fire and the Fire Bangle) is all made in survival, and JEI now says beside the recipes how the Adept's Wand and the lair bosses' loot are won. No recipe is added: the owner's 18 armour tiers (the Wayfarer among them) stay creative-only until the owner decides how they are won. Record: [concordance-first-loadout.md](docs/features/concordance-first-loadout.md).
- **A cast's pose gives way to a gun or an arm:** switch from the wand to a gun or a Jugcraft arm and the cast's release or fading pose no longer plays over the gun, its reload or the arm's stroke, in either view and on every player you see; the circle's and the Vigil's gestures give way to a gun as they already did to an arm. Record: [arcane-concordance-presentation.md](docs/features/arcane-concordance-presentation.md#one-owner-for-the-arms-chunk-a-ticket-a-07).
- The chunk's contract, decisions and evidence: [concordance-chunk-a.md](docs/features/concordance-chunk-a.md).

### Added: The Jugcraft Encyclopedia, first slice (entry points and route home)
- **Open the Jugcraft Encyclopedia without an item:** press **K** (remappable in Controls under Jugcraft), or click the new **book button on the survival inventory** beside the recipe book. Esc returns to the game or the inventory.
- **A home with both routes,** *Machinery and Technology* and *Magic: the Arcane Concordance*, each with an overview. Open a route for its milestones and chapters. Technology's nine Progression stages and seventeen chapters open the Engineer's Handbook at that page, and closing it returns to the route. Magic's six milestones and seventeen codex categories open the Arcane Concordance codex there, without its book item; another link opens the Concordance Journal.
- Generated by `tools/encyclopedia.py` from the handbook's and codex's own tables; `check_mod_data.py` checks every item and link. No quests, rewards, route graphs or saved data yet; the Engineer's Handbook item is unchanged. Proposed for review; the Java was not compiled where it was written (see the PR). Record: [jugcraft-encyclopedia.md](docs/features/jugcraft-encyclopedia.md#first-slice-entry-points-and-route-home).

### Added: The Cinder Tyrant (boss 4, part 2)
- **The Cinder Tyrant,** the Cinder Kiln's boss: a salamander lord the size of a cart, armoured in obsidian plates with magma glowing in their seams. He waits sunk in the crucible, only his crest over the slag, and wakes when a player steps down onto the bowl's floor or strikes him: 440 health scaled for the party, armour 10, never hurt by fire. Left alone, he sinks back, healed, and the kiln resets.
- **His rule, the heat is his:** hot, he takes half of every blow. Turn a sluice and bait his Body Slam into its flooded trough, and the steam quenches him for 8 seconds: his seams go dark, blows do a quarter more and he slows.
- **The Kiln:** Tail Sweep, Ember Spit (gobs of magma that leave fire), Body Slam, Kiln Breath and Mantle Shed (Cinderlings, little salamanders of slag that gutter out in water). **At half health, the Eruption:** he roars from the forge's lip while the heat channel surges over its banks; then Cinder Rain and the Lava Wave (jump it or stand on a shelf), a sluice at a time choked with slag, and a surge every 30 seconds. **Below a fifth, the Molten Heart:** faster, five gobs, a wider breath. Fire Resistance stops his fire, never his blows.
- **His loot, each participant's own:** Tyrant Scales (into magma cream, or a cheaper Kiln Seal), one of Arms VII's Cinderbrand or Magmaw (now dropping; certain on a first kill), the Salamander Charm (in the offhand, hot ground never burns you) and the Tyrant's Crest (a costume), with shared experience and **Tempered**. When he falls the crucible cools to obsidian and Grey Mist opens in the bowl. Record: [cinder-tyrant.md](docs/features/cinder-tyrant.md).

### Added: The Cinder Kiln and the Kiln Seal (boss 4, the Cinder Tyrant, part 1)
- **The Cinder Kiln,** a fourth lair: a kiln under a volcano, a dome of kiln brick ribbed with blackstone over a bowl of cracked basalt 44 blocks across, its vent open to a smoky sky. A forge mouth in the north wall pours molten slag over its lip, down a heat channel to a crucible three blocks deep where the Cinder Tyrant will wait. The slag burns whoever stands in it (Fire Resistance stops it).
- **Three sluice gates** in the walls, the kiln's working fixture: turn any gate's wheel and its trough floods for 10 seconds, then drains while the gate fills again for 20. A flooded trough puts out the burning. Four raised shelves round the bowl stay out of the slag's reach. You arrive on a ledge in the south wall and walk down a curving stair with no step over half a block; the Grey Mist home hangs behind the ledge.
- **The Kiln Seal** (four obsidian, a magma block, two blaze powder, two gold) opens it: press it into a magma block in the Nether, or in the Volcano or the Cinder Barrens, at any hour. The magma cracks open into a vent of sparks and smoke that others can follow through for a minute. Refused, it says why and is kept. The Tyrant himself and his loot are part 2. Record: [cinder-kiln.md](docs/features/cinder-kiln.md).
- **CI: the client tests share five jobs instead of four.** With the kiln's client test the estimate for the busiest of four jobs passed the 25 minutes the sharing check allows; with five it is about 20 a job ([docs/TESTING.md](docs/TESTING.md)).

### Added: The boss playbook (how a boss is built)
- **[docs/branches/BOSS_PLAYBOOK.md](docs/branches/BOSS_PLAYBOOK.md)** writes down, at the owner's request, the order the Yeti King was built in, as the plan for building every boss.
- Each boss is two pull requests: its lair and the summoning that opens it, then the boss, his adds and his loot. The steps run from the survey and the plan to the numbers, model, art, data, Java, audit, tests and record, then CI and the pictures. Each step names the files the Yeti King made at it.
- It also records what the Yeti King taught: checking every API against 26.3 before CI, porting an open fix for a flaky test, and aiming the cameras from where the boss is. It carries the planning pack's shared lair contract and what "done" means for a boss. Docs only.

### Added: Arms VIII fix (a thrown arm no longer flies through a foe it stops just short of)
- **A thrown arm strikes a foe it ends a tick just short of.** Before, it sometimes flew through. From a flight's third tick, the sweep that finds what a thrown thing hits widens each box it looks for, but finds only the boxes it enters. An arm that ended a tick inside the next tick's widening, short of the foe itself, started that tick inside the box and flew on. Now each tick but the first starts by striking a foe whose widened box the arm is already inside.
- This made the steel javelin's game test fail now and then, on `main` and on pull requests that do not touch the thrown arms. A new test sets a javelin going slowly at a still pig to end a tick 0.02 short of it, and it must strike. Record: [arms-viii.md](docs/features/arms-viii.md).

### Added: Arcane Concordance, Wayfaring part 1c (the belt and boots hidden under armour, and a setting)
- **Armour hides the worn belt and boots,** as the owner asked: the belt under a chestplate or leggings, the boots under boots. An elytra hides nothing. They come back as soon as the armour comes off.
- **A new setting, Show my worn trinkets** (Concordance settings, on by default), hides your own belt and boots, and everyone who sees you sees your choice; you see theirs.
- A small render element does both, inside the same render definitions. Your client sends the setting to the server when you join a world and whenever you save it; the server keeps it on you (not saved: it is sent again each time you join), shows it to everyone who sees you and takes a change at most twice a second. Record: [arcane-concordance-trinkets.md](docs/features/arcane-concordance-trinkets.md).

### Added: Guns, slice 11B (the rest of the Nether guns)
- **Three more Nether guns** from the owner's crimson, blackstone and gold models and animations, whose bullets set what they hit alight as slice 11A's do: the **Ashfall Pistol**, held in one hand (so it pairs with another one-handed gun), the tank under its barrel changed to reload; the **Goldbristle Carbine**, one rifle round a pull from a magazine at its left side; and the **Crackling**, a long gold pump shotgun of six heavy pellets, loaded a shell at a time.
- Each is made from steel, gold, a lever and gilded blackstone. The Ashfall Pistol takes the stocks, scopes and Laser Sight; the Goldbristle Carbine everything slice 11A's guns take, and the magazines; the Crackling the stocks, grips and bayonets, its owner-made barrel attachments being too short for its barrel.
- Record: [guns.md, slice 11B](docs/features/guns.md#slice-11b-the-rest-of-the-nether-guns).

### Added: Guns, slice 11A (the Nether guns)
- **Three new guns** from the owner's crimson, blackstone and gold models and animations, whose bullets set what they hit alight for four seconds (never a block): the **Tusker**, a pump shotgun of eight pellets loaded a shell at a time; the **Cinder Repeater**, an automatic firing light rounds from a magazine of twenty; and the **Bastion Rifle**, a heavy rifle whose trapdoor breech flips open after every shot, loaded a round at a time.
- Each is made from steel, a lever, gold and gilded blackstone from the Nether's bastion remnants. Each takes the stocks, grips, barrel attachments, bayonets, scopes and the Laser Sight; the Cinder Repeater the magazines too.
- Record: [guns.md, slice 11A](docs/features/guns.md#slice-11a-the-nether-guns).

### Added: Guns, slice 10G (two guns at once)
- **A pistol in each hand:** with a one-handed gun in each hand (thirteen of the guns: the pistols, revolvers, the Haymaker, the Bronco SMG, the Spikedriver and the Culverin), left click fires the main hand's and right click the other's, a press a shot, held for an automatic. Each keeps its own rate, magazine, sound and flash; neither is aimed, and each strays half as far again from the hip.
- G reloads the main gun, or the other when the main one is full; one gun reloads at a time. Each gun's counter shows on its own side of the hotbar. Seen from outside both arms are raised; in first person the other hand's gun has its own arm, drawn mirrored. The one-handed guns say in their tooltips that they pair.
- The shot, reload, action and trace payloads now carry the hand.
- Record: [guns.md, slice 10G](docs/features/guns.md#slice-10g-two-guns-at-once).

### Added: Guns, slice 10F (enemies with guns)
- **A new raider, the Raider Gunner,** who carries one of the owner's service arms (the Sentry Pistol, Garrison Rifle or Breacher) and fires it: the gun's own bullets at half their damage, in bursts, with the gun's magazine and reload time. Its bullets pass through raiders.
- Gunners march in raids in place of some of the grunts (parties are as large as before), and one holds each raider camp. While the guns are switched off, grunts come in their places.
- A player's kill may win its gun (8.5%, empty) and a few of its rounds. Mobs holding a gun are now posed holding it as players are.
- The raiders' record: the party table, the camp's garrison, the gunner's loot, and the level 5 party's size (14, not 15).
- Record: [guns.md, slice 10F](docs/features/guns.md#slice-10f-enemies-with-guns); [raiders.md](docs/features/raiders.md).

### Added: Guns, slice 10E (the Cell Rack)
- **A new block, the Cell Rack:** two shelves of three cradles that charge every Energy Cell standing in them at once from cables, sharing up to 1,024 JE a tick evenly, no more than the Charging Station's 512 to one cell. Stand a cell in the cradle you point at, take one out with an empty hand; hoppers above or beside it put cells in, and a hopper below takes out only full ones.
- Made from the Charging Station's parts, a steel plate in place of its lamp; it needs the guns and machines switches, as the Energy Cell does. It is drawn in the power gear's electric look, the cells standing upright in it.
- Also corrects the record's Charging Station time for a cell (slice 8D): 20 to 40 ticks, not seconds.
- Record: [guns.md, slice 10E](docs/features/guns.md#slice-10e-the-cell-rack).

### Added: Guns, slice 10D (the sculk guns)
- **Three new guns** from the owner's sculk-teal models and animations, each with sculk tendrils that sway as it is handled: the **Undertone Rifle**, one hard shot a pull from a magazine of twelve rifle rounds; the **Murmur SMG**, ten light rounds a second while the trigger is held; and the **Reverb**, a double-barrel of ten-pellet buckshot whose barrels turn aside to take a shell at a time.
- Each is made from steel, a lever, sculk and an echo shard from the deep dark's ancient cities. Each takes the scopes and the Laser Sight; the Undertone Rifle the stocks, the Murmur SMG the magazines, the Reverb the light and tactical grips.
- Record: [guns.md, slice 10D](docs/features/guns.md#slice-10d-the-sculk-guns).

### Added: Guns, slice 10C (the double-barrels)
- **Three new guns** from the owner's models and animations, each firing a spread of pellets and loaded all at once: the **Mule**, a sawn-off double-barrel that breaks open to load both shells; the **Fowler**, a double-barrelled flintlock with a ball rammed down each barrel; and the **Culverin**, a stubby hand cannon fired one-handed.
- The Mule fires Buckshot Shells, the Fowler and the Culverin Paper Cartridges. The Fowler takes the grips and the bayonets; the others take nothing.
- Record: [guns.md, slice 10C](docs/features/guns.md#slice-10c-the-double-barrels).

### Added: Guns, slice 10B (coil and plasma)
- **Three new energy weapons** from the owner's models and animations, on the Energy Cells: the **Solenoid Rifle**, a coil rifle whose heavy beam passes through every creature in its line and reaches the farthest of the energy weapons; the **Votive Rifle**, a plasma rifle that burns for as long as the trigger is held; and the **Glowmouth**, a plasma blunderbuss loaded a charge at a time, whose arc leaps from its mark to two more creatures close by.
- The two rifles take magazines, stocks, grips, bayonets, the scopes and the Laser Sight; the Glowmouth takes nothing.
- Record: [guns.md, slice 10B](docs/features/guns.md#slice-10b-coil-and-plasma).

### Added: Guns, slice 10A (the launchers)
- **Three new guns** from the owner's models and animations: the **Earthmover**, a shoulder rocket launcher with a drum of four rockets; the **Skylark Rifle**, a break-open rifle that fires one rocket at a time, faster and further; and the **Bullfrog**, a stubby grenade launcher loaded a grenade at a time.
- The rocket guns fire the rocketry branch's High-Explosive Rockets, which burst where they hit, or at the end of the gun's range, as the Rocket Launcher's do; they hurt living things only and break no block. The Bullfrog loads any grenade, as the Trench Lobber does.
- A rocket in flight now saves what is left of its fuse.
- Record: [guns.md, slice 10A](docs/features/guns.md#slice-10a-the-launchers).
### Added: Industrial factory package 2, part 2 (the Gas Burning Generator)
- **The Gas Burning Generator**, the starter factory's hydrogen generator: four wide, five deep and four tall, from the owner's art brief. A gas engine and generator sit on a steel skid: the round intake with its fan, an arched pale heat shield over the olive combustion section, a guarded coupling, the ribbed generator housing, and a tall terminal cabinet with mint lamps and the power socket.
- **Burns hydrogen at the owner's selected 128 JE/t**: a millibucket a tick, 128 JE each, so a bucket lasts 50 seconds. It is piped in at the capped inlet low on its right side, and its power leaves at the cabinet's socket at the back.
- **Wastes nothing**: it makes only what its store has room for, so its fuel follows the demand, and its screen says when the fuel tank is empty or the store is full.
- **Built from steel**: 4 steel plates, a steel gear, an electric motor, a basic circuit, a Tinplate Tank and a Machine Casing. Unlike the Fuel Cell, it needs no aluminum.
- Its coupling and fan spin up and run down; its lamps and sight glasses light while it runs. Methane, at 256 JE/t, comes with the methane Infuser.
- **Machine forms can now be generators**: a form can list the fuels it burns and give power out of a power port. The Gas Turbine itself still never burns hydrogen. Record: [industrial-gas-burning-generator.md](docs/features/industrial-gas-burning-generator.md).

### Added: Industrial factory package 2, part 1 (the Electrolytic Separator)
- **The Electrolytic Separator**, the first big industrial machine: two wide, two deep and three tall, from the owner's art brief. A tall olive cell housing stands between two white collection towers, with its control box, bus connection, lye return and outlet collars.
- **Built from steel and copper**: 4 steel plates, 2 Steel Tanks, copper cable, a basic circuit and a Machine Casing, 20 steel plates in all. It needs none of the Electrolytic Cell's aluminum or advanced circuit, so hydrogen and chlorine come earlier.
- **What it splits**, at 256 JE/t:
  - a bucket of water gives 500 mB of hydrogen and 250 mB of oxygen in 40 s;
  - a bucket of brine gives 250 mB of hydrogen, 250 mB of chlorine and 500 mB of lye in 20 s.
- **Ports where the model shows them**: water or brine in low at the back; hydrogen, oxygen or chlorine, and lye out at their collars on the front; power at the bus connection.
- **It keeps its lye**: a full lye tank stops brine while water still runs, and the screen says "Lye tank is full".
- The Electrolytic Cell keeps its own recipes, but **its brine now also takes 20 s** (102,400 JE a bucket, was 10 s), the owner's choice to match the Separator. JEI shows the Separator's recipes in their own category, and the Engineer's Handbook has its page.
- **Machine forms**: each now numbers only its own blocks (12 for the Separator), and a working form lights its lamps and strips on every block, not just its controller. The formed-machine screen wraps a long state and fits its power line. Record: [industrial-electrolytic-separator.md](docs/features/industrial-electrolytic-separator.md).

### Added: Industrial factory package 1 (the shared machine foundation)
- **The groundwork for the industrial factory's big machines** (package 1 of the owner's roadmap in PR #302); nothing new to craft yet. A machine **form** describes one installation within 2 to 6 blocks on each axis: its parts, the clearance its moving parts need, its open access space, its ports, its tool sockets and what it can process. Placing one checks every position first and names whatever is in the way; breaking any part removes it all and drops it once.
- **Pipes, cables, conveyors and hoppers reach a form only at its ports.** Tool sockets (catalyst beds, patterns, molds) are never reachable from outside, and a tool cannot be taken out while a batch needs it.
- **Batches are atomic and saved.** Inputs move into the batch and room for every result is reserved before anything is spent; restarts, unloads and recipe reloads carry on the same batch; Cancel returns only the untransformed inputs. Larger profiles run two or four batches side by side at 90 or 80 percent of the energy each.
- **A screen that says what to fix:** one of eight states (unformed, idle, waiting for input, a tool or power, warming, processing, output blocked) and the tank, slot or socket concerned, such as "Hydrogen tank is full".
- Fluid recipes can now ask for a capability or a reusable tool; the original machines keep exactly the recipes they had. Machines that later add tanks can declare how old saves map onto the new tanks, so an output never loads into an input. Record: [industrial-machine-foundation.md](docs/features/industrial-machine-foundation.md).

### Added: Arcane Concordance, Wayfaring part 1b (the belt and boot drawn on the body)
- **The Leather Belt and the Amphibian Boot now show when worn,** as the owner drew them: a leather strap round the waist with a gold buckle in front, and on each foot a green boot with a white cuff, a grey toe cap and a fin on the heel. You see them in third person and on the inventory's figure; other players should see them too (not yet tried with two clients). The charms and the Ice Breaker (the owner drew no worn sheet for it) are not drawn.
- The owner's two worn sheets are imported byte for byte, with their sources and checksums recorded; the boxes are fitted to them (the sheets came without geometry) and kept off the skin's layers and vanilla armour by the same gaps as Jugcraft's 3D armour, so they should not flicker against them up close (armour, drawn a little toward the camera, shows through them from far off). Trinkets' data-driven renderer draws them: no Java. Record: [arcane-concordance-trinkets.md](docs/features/arcane-concordance-trinkets.md).

### Added: Arcane Concordance, Wayfaring (the owner's belt, boots and charms; trinkets part 1)
- **Eight worn things from the owner's relic and reliquary art,** put on once you understand Relic Lore: the **Leather Belt** (a second **Charm** slot), the **Angelic Feather**, **Kraken Shell** and **Infernal Claws** (a fall's, drowning's or fire's harm taken from your food instead of your health, half a food point a point, when your food bar can pay; the feather also jumps a little higher), the **Angelheart Vial** (a death save on 2 hearts, used up) and the **Phoenix Down** (a death save at full health; it becomes an Angelic Feather), and on your feet the **Amphibian Boot** (faster swimming, longer breath) and the **Ice Breaker** (a little knockback resistance, and a hard landing throws back and slows the hostile creatures round you).
- Two new Trinkets slots with the owner's icons, **Charm** and **Feet** (two), and Trinkets' Belt slot. Two of a kind never add up. A death save never answers the void, a held totem or a death in a dream.
- The kinetic belt that links pulleys is now called the **Drive Belt** (its id is unchanged), so the two belts are told apart.
- The icons and slot icons are the owner's files byte for byte (the three animation sidecars with their line ends made LF), with their sources and checksums recorded; the item models and names are taken from the owner's files. Record: [arcane-concordance-trinkets.md](docs/features/arcane-concordance-trinkets.md).

### Added: Arcane Concordance, Ember part 2 (the Hearthbinder's regalia, from the owner's fire art)
- **Lesser Focus of Fire and Focus of Fire,** the owner's own: worn in a new **Spell Focus** slot, +2 and +4 fire Spell Power, so Cinderbolt and Hearthflare burn hotter.
- **Fire Bangle:** worn in one of two new **Bracelet** slots; once you understand Hearthbinding, your melee blows leave the creature smouldering for 3 seconds, wherever you may harm it.
- **Two fire sets, worn as the owner's 3D model** (Jugcraft's first GeckoLib armour), half a point of fire Spell Power a piece: the **Pyromaniac's Hood, Tunic, Pants and Shoes** (cloth, wool alone, in the owner's light fire texture) and the **Pyromancer's Hat, Robes, Leggings and Boots** (wool and gold, leather's protection). At most the regalia gives +6 fire: Cinderbolt 6, Hearthflare 7. Spell Power's Sunfire cannot be put on either set.
- Hearthflare now plays the owner's fire recordings. Every file is the owner's, copied as supplied, with its source and checksum recorded. The heavy fire set, the Caster Tome and the fire familiar wait for later slices. Record: [arcane-concordance-ember-regalia.md](docs/features/arcane-concordance-ember-regalia.md).
- **Combat baselines:** a geared Hearthbinder (+6 fire) joins the benchmark and wins all five fights; the same spells with no fire gear win three. The benchmark now lets Hearthguard's Fire Resistance land as it does in game; before, it kept restarting the cast instead of fighting. Record: [arcane-concordance-baselines.md](docs/features/arcane-concordance-baselines.md).

### Added: Arcane Concordance, Ember part 1 (Hearthbinding and four fire spells)
- **A new research entry, Hearthbinding** (the Hearthbinders' Principle, Ember): examine coal, charcoal, a torch or other things that hold fire once First Light is understood; study one at the Lampwright's Bench. Mastered by lighting three kinds of hearth with Hearthspark.
- **Four fire invocations,** cast with any Concordance instrument: **Hearthspark** lights an unlit campfire, candle or candle cake up to 16 blocks away (never placing fire); **Hearthguard** gives 30 s of Fire Resistance; **Cinderbolt** deals fire damage that grows with fire Spell Power and leaves the target **smouldering**; **Hearthflare** (mastered) bursts round you. Every one obeys claims, towns, spawn protection and the PvP rules.
- **Smoulder,** a new status: the creature burns at vanilla's pace while it lasts; water, rain, Fire Resistance and milk answer it.
- Original spell icons, a status icon and five synthesised sounds; a new Hearth codex category. Record: [arcane-concordance-ember.md](docs/features/arcane-concordance-ember.md).

### Added: Guns, slice 7 (bayonets and the shared-texture guns)
- **Four bayonets,** each the owner's art: **Iron**, **Steel** (the owner's anthralite one), **Diamond** and **Netherite** (a smithing upgrade). Under the barrel, in place of a grip. **V stabs** for 4 to 6 damage within reach, every 12 ticks; the server deals the blow. Seven guns take them.
- **Five more guns take attachments:** the Drover Rifle, Coach Gun, Duelling Pistol, Line Musket and Bellmouth. The owner drew their parts on shared textures; each gun's atlas now packs those into its free room, never over 128 × 128.
- Scopes wait for the owner's reticle textures. Record: [guns.md, slice 7](docs/features/guns.md#slice-7-bayonets-and-the-shared-texture-guns).

### Added: The Yeti King (boss 3, part 2)
- **The Yeti King**, a great white ape of the glacier with a crown of blue ice, waits slumped on his throne in the Glacier Hall and leaps down onto the frozen lake when a player steps onto it. He has 420 health, scaled up for a party.
- **His rule, the snow is his:** his Ground Slam blasts the lake's drift snow bare to slick glare ice round where he lands, for 12 seconds before it drifts back; never the trampled snow round the columns, at the ramp's foot or before his dais.
- **Phase 1, the Hunt:** Maul Swipe, Boulder Throw, Ground Slam, Frost Breath and the Avalanche Charge. Charge him into an ice column and he reels for 3 seconds, taking a third more damage.
- **At half health, the King's Roar:** he roars from his dais, a blizzard chills everyone near and two Yeti Whelps climb out of the dens. Then come Icicle Fall, Glacial Spikes and Kin Call.
- **Below a fifth, the Fury of the Peaks:** his crown blazes and his eyes burn blue, his cooldowns are a third shorter, his slam bares a wider ring and a charge that misses the columns is followed by another.
- **Left alone for 10 seconds** he climbs back onto his throne, healed, and the snow drifts back.
- **His loot is each participant's own:**
  - Yeti Fur, which makes a cheaper Frost Horn or two white wool;
  - one of Arms VII's two Yeti King trophies, the **Glacier Maul** or the **Rimeclaw**, with their Frost boon, certain on a first kill;
  - the Yeti Mitten, which keeps its wearer from freezing from the offhand;
  - the Yeti King's Crown, a costume;
  - shared experience and the advancement Abominable.
- When he falls, the snow drifts back over his lake and Grey Mist opens in its middle. Animated with GeckoLib. Record: [yeti-king.md](docs/features/yeti-king.md).

### Added: The Glacier Hall and the Frost Horn (boss 3, the Yeti King, part 1)
- **The Frost Horn** (a goat horn, two gold ingots, two leather and two snow blocks) opens the Glacier Hall:
  - blow it at night in the Overworld, standing on snow or ice. A roar answers, the snow at your feet splits, and you fall through and land on the hall's ledge with frost on your skin. The horn is used up;
  - for 60 seconds a whirl of white mist stays open in the snow, and anyone who uses it follows;
  - by day, off the snow, outside the Overworld, where a whirl is already open or with every hall taken, it says why and is kept. It works all year: the Yeti King is no Witching Season boss.
- **The Glacier Hall** is a vast cavern in the heart of a glacier:
  - a ledge high in the south wall under a snow-choked crevasse, where you land, with Grey Mist in an arch of blue ice beside you as the way home;
  - a snow ramp down to a frozen lake 41 blocks across, crusted with drift snow, scoured to slick glare ice at its shore and middle, with trampled snow round four great ice columns;
  - the King's throne of ice on a dais of blue ice steps, under an arch of two mammoth tusks, with his frozen hoard heaped about it;
  - two dens in the side walls, giant icicles on the vault and a crack open to the pale sky.
- Six lair-only blocks: unbreakable, no items, no drops. The Mist Gate draws a whirl of snow for the hall. Record: [glacier-hall.md](docs/features/glacier-hall.md).

### Added: Madame Tatterlace (boss 2, part 2)
- **Madame Tatterlace**, a great spider seamstress, waits sewing on the white silk over the Spindle Loft's doily and lowers herself onto it when a player steps onto the lace. She has 360 health, scaled up for a party.
- **Her rule, the floor is her work:** she unravels rings and wedges of the doily, which drop away into the dark and are knitted back 12 seconds later; never the band round a spool or the tape's foot.
- **Phase 1, the Fitting:** Needlepoint, Thimble Toss, the Binding Thread (strike it to snap it), the Lace Snare and the Spool Roll.
- **At half health, Taking In the Seams:** she climbs into the threads and spits six egg sacs round the doily. Then come Pin Rain, Unravel, the Drop Strike (she lies open on the lace after it) and her Brood of spiderlings.
- **Below a fifth, Frenzied Stitching:** her cuffs glow red, her cooldowns are a third shorter and she unravels two segments at once.
- **Left alone for 10 seconds** she knits her doily whole and goes back to her sewing, healed.
- **Her loot is each participant's own:**
  - Gossamer Silk, which makes a cheaper Cursed Spindle or three string;
  - the **Needle Rapier**, an Arms VII trophy with the Stitch boon, certain on a first kill;
  - the Golden Thimble, which turns a projectile aside every 15 seconds from the offhand;
  - Tatterlace's Headdress, a costume;
  - shared experience and the advancement Unravelled.
- When she falls, her doily is whole again and Grey Mist opens in its middle.
- What every lair boss shares (who may fight one, party health, the damage and health settings) is now one class for both bosses. Animated with GeckoLib. Record: [tatterlace.md](docs/features/tatterlace.md).

### Added: The Spindle Loft and the Cursed Spindle (boss 2, part 1)
- **The Cursed Spindle** (two gold ingots, an amethyst shard, two spider eyes, three string and a stick) opens the Spindle Loft:
  - use it on a **Spinning Wheel** at night in the Overworld. You prick your finger, fall asleep and wake on the loft's pincushion, blind for a moment. The spindle is used up;
  - for 60 seconds the wheel spins wild, and anyone who uses it with an empty hand follows;
  - by day, out of season, on a wheel already spinning or with every loft taken, it says why and is kept.
- **The Spindle Loft** is a colossal sewing room's attic seen at a spider's size:
  - a tomato pincushion stuck with pins taller than a house, where you arrive, with Grey Mist in a needle's eye beside you as the way home;
  - a measuring tape sloping down to a lace doily 41 blocks across, hung over darkness between four giant thread spools;
  - a thimble and the blades of a pair of shears beside the doily;
  - threads up to the rafters, a grimy skylight, and the Spider's Larder's cocoons and egg sacs.
- Twelve lair-only blocks: unbreakable, no items, no drops.
- The lair framework now takes a second lair: a lair with no moon, hooks run as an instance closes, and words for coming into each lair. Record: [spindle-loft.md](docs/features/spindle-loft.md).

### Added: Vesperine, the Last Reaper (boss 1, part 2)
- **Vesperine** waits on the Bone Throne of every Hollow Acre and rises when a player steps into the Mown Circle. She has 400 health, scaled up for a party.
- **Her skulls, Dirge and Requiem**, halve every blow she takes while both live. They fire homing Grief Bolts that a player can strike back.
- **Phase 1:** the Reaping Arc, the Harvest Lunge, the Scythe Throw (unarmed, she takes a quarter more) and the Grave Call's thralls.
- **At half health, the Last Toll** turns the moon red and re-forms her skulls. Then come the Twin Beam, Crop Circles and the Shadow Step.
- **At a quarter, Death's Harvest:** souls stream from the black wheat to heal her until they are struck down or the four ward braziers are lit again, then she slams down.
- **Left alone for 10 seconds** she returns to her throne, healed.
- **Her loot is each participant's own:**
  - Reaper's Shade, which makes the Shade Wreath (a cheaper Mourning Wreath) and the Reaper's Hood;
  - the **Vesper Scythe**, an Arms VII trophy with the Harvest boon, certain on a first kill;
  - the Dirge and Requiem skull trophies, which glow and are worn as costumes;
  - the Reaper's Hood;
  - shared experience and the advancement The Last Harvest.
- When she falls, Grey Mist opens in the circle.
- Settings: `lairs.boss_health`, `lairs.boss_damage`, `lairs.event_loot`. Animated with GeckoLib, which was already pinned. Record: [vesperine.md](docs/features/vesperine.md).

### Added: The Hollow Acre: the lairs and the Last Rites (boss 1, part 1)
- **The Last Rites** open the Hollow Acre:
  - at night, at any Jugcraft headstone in the Overworld, with four lit candles round it and a **Mourning Wreath** (four mourning flowers round a vine) laid on it;
  - ring the **Death Knell** (a gold ingot over an iron nugget over a bone) beside it.
  - The wreath is taken, and a gate of grey mist stands over the grave for 60 seconds: the ringer goes through, and anyone who uses the gate follows, four at most.
  - When a step is missing the knell says which, and nothing is used up.
- **The Hollow Acre**, a pocket dimension of its own: a floating island of black earth under an endless starless night and a vast harvest moon. Players arrive at the graveyard's Lych Gate, then cross a field of black wheat and weathered headstones to the Mown Circle, its arena ringed with soul braziers. At the north end stands a ruined bone chapel, with the Bone Throne at its open side. Vesperine comes in part 2.
- **The lairs' rules**, shared with the Spindle Loft to come:
  - every ritual places the lair fresh into its own instance;
  - nothing in a lair can be built or broken;
  - the lych gate's Grey Mist or `/jugcraft lair leave` takes a player back to exactly where they stood;
  - straying off the island costs 4 health, never below half a heart;
  - dying there keeps everything as **Grave Goods**, handed back on respawning;
  - empty instances close after 30 seconds, and a restart closes all of them.
- Settings: `lairs.instances`, `lairs.party_size`, `lairs.gate_seconds`, `lairs.off_season`. Record: [hollow-acre.md](docs/features/hollow-acre.md).

### Added: Armor in Blockbench, and the owner's sets rebuilt from their renders
- **Blockbench projects for the armor sets:** `tools/bbmodel.py` writes any 3D armor set as a Blockbench project and reads one back into the game's worn models and atlas. A set can now *be* a project (`art/armor/<set>.bbmodel`): the owner opens it in Blockbench, edits it there, and what they save is what the game draws. Every set round-trips exactly (`python3 tools/bbmodel.py check`).
- **The Sentinel rebuilt to match the owner's three renders:** its helm is now the owner's 9×9×9 box with their keyhole and meander art copied texel for texel; its right pauldron is their bent plate measured off the front and back views; its boots rise in teeth like a crown; the coat, gorget and thigh plates follow the renders. The texture is drawn two texels to a pixel, as the owner's is. 42 parts where it had 96, and no part of the wearer shows.
- **The Frost Knight rebuilt to match the owner's front render:** its helm is now the owner's 9×9×9 box with their face copied texel for texel (the eye band turning up at its ends, the nose bar, the grille), crowned with their crystals as measured and coloured, a tall one rising 14 pixels; the round mane of feathers gave way to the compact frost the render shows, a big tuft flaring out on the left and spikes down the right, carrying its texels; an ice cross on the back of the helm; the legs' fronts carry the owner's texels. 102 parts where it had 139.
- **The Reaper matched to the owner's render again:** its white V clasp sits just under the hood's opening as drawn, and its pouch is the render's big square on the left hip; the robe, its strips and the pouch carry the render's texels.
- **The Paladin and the Templar as Blockbench projects:** the mail sleeve now shows under each pauldron, as in the owner's renders (two lames there were taken off).
- **All thirteen of the owner's sets are Blockbench projects:** the Dread Knight, Valkyrie, Wayfarer, Spartan, Berserker, Wight King, Banana and Scarab as they were (they draw the same), alongside the five above.
- **Matching tools:** the owner's renders are kept under `art/armor/references/`, and `tools/armor_reference.py` fits cameras to them and compares or lifts texels from them. Record: [blockbench-armor.md](docs/features/blockbench-armor.md).

### World Designer
- An offline browser map editor sculpts terrain, paints installed biomes, places spawn and one Jugcraft walled city, and pins native village/structure start chunks.
- `/jugcraft design export` supplies the editor and live mod catalog. Its exported datapack adds the **Jugcraft Designed** preset for new worlds; existing generated chunks are not edited.
- Reuses native generation and Jugcraft's town systems. Read the [guide](docs/WORLD_DESIGNER.md) and [actual test evidence and limits](docs/features/world-designer.md).

### Added: Thallite, slice 2: gear
- **Thallite tools and armor:** a sword, pickaxe, axe, shovel and hoe, and a helmet, chestplate, leggings and boots, crafted from thallite ingots like iron's and repaired with them. They mine and protect as iron does, last a little less (200 uses a tool) and enchant best of all (18).
- **Regrowth:** thallite gear you wear or hold gets back one use every 5 seconds while you stand on living soil (grass, dirt, podzol, moss, mud, rooted dirt or farmland), up to 75% of full, with a faint green sparkle. It never brings back a broken piece.
- **Earthbound armor:** an Earthbinding Template (rooted dirt, four thallite nuggets and four gold nuggets), a thallite piece and a gold ingot at a smithing table bind the piece into its gold-trimmed Earthbound twin, for good, keeping its enchantments, wear and name. Earthbound pieces are **Rooted**: on natural ground (soil, stone, sand or gravel) each takes 7.5% off knockback, 30% for a full set, and with two or more worn Regrowth works on stone, sand and gravel too.
- Both traits are named in the tooltip and described while Shift is held. Two handbook pages. Arms, the arrow, horse armor and magic come in later slices. Record: [thallite.md](docs/features/thallite.md).

### Added: Armor designs of 8 October (Berserker, Paladin, Templar, Sentinel, Frost Knight, Wight King, Reaper, Banana, Scarab), three swords, a scythe and a shield
- **Nine more armor sets from the owner's own designs,** each worn as a detailed 3D model (most of them 80 to 110 parts) with four 16×16 icons of its own:
  - **Berserker:** an open white cap with a red crest spilling over the brow, a square horn standing up from each side and a toothed jaw frame; a white keyed breastplate with raised red bands, stepped red pauldrons with white trim, flanged and studded bracers; thigh guards in four bands of white and red stepping out over dark mail; grey boots with red soles.
  - **Paladin:** a white great helm with a raised H on its visor, a comb and a purple sprig; a shield-shaped breastplate with a raised cross, spiralled square pauldrons with fanning lames, elbow fans, cuffs and gloves; hinged tassets with spiral bosses and blue gems over flaring purple cloth; banded greaves with knee cops and layered sabatons.
  - **Templar:** the Paladin's build in slate with pale reliefs and dark red cloth: a barred great helm under a pale gable that reaches past its sides, and a pale crest curling up from the crown.
  - **Sentinel:** a gold bucket helm with a raised keyhole visor, ribbed cheeks, a rim round its top, a little house-shaped loop on its crown and a meander in raised gold bars behind; a black coat under a two-tier gold gorget with a pale strip down to a boss, and a baldric from the left shoulder; a great pauldron on the right shoulder, one thick gold plate bent at the shoulder with a pale hook by the helm and a step at its foot, banded gold, dark and cream, and three flat gold plates on the left; a square gold stud on the arm, a gold bracer, banded sleeves and gold gauntlets; a black skirt with rimmed plates, gold on the left thigh and dark on the right; gold boots with chequered cuffs. Rebuilt in more detail at the owner's word, and its right pauldron then copied from the owner's renders plate for plate and texel for texel.
  - **Frost Knight:** a white great helm wrapped in a mane of frost feathers, a white gable over its brow, a dark eye slit turning up at its ends and crossed by a ridge over a grille of dark slots, crowned with ice crystals (the tallest 9.5 pixels above the head) and an ice cross behind; a white cuirass with a navy strap, a navy pauldron on the left with a white key spiral and a stepped white rim, and on the right a rounded mass of frost with feathers hanging down the arm and a feathered drape behind; white legs in stacked front and side lames under a navy belt with an ice gem, a navy flap over the left hip, banded knees with cops and wings; white boots with frost at the ankles. Its helm and legs reworked at the owner's word, then its frost rebuilt as feathers and its shoulders detailed as the renders draw them.
  - **Wight King:** a slate helm with a black skull's face, a crown of seven icicles and two tall antlers rising 11 pixels above the head; a cuirass with a V collar meeting at a cyan gem and two dark straps crossing over the belly; a jagged right shoulder of slate shards and a layered left one; banded legs under a dark belt with a cyan buckle; banded boots with pointed toes.
  - **Reaper:** a deep hood whose opening is an arch stepped into the cloth, three grey bars in its black void; a dark robe under the hood's ragged cloth, a white clasp, a strap stepping across the chest to a framed pouch; grey shoulder plates under a white V and white bands on the right arm, open white cages round the left upper arm and fist; robe strips of different lengths over a dark under-robe; wrapped boots. Rebuilt at the owner's word, its crescents now its weapon.
  - **Banana:** a banana costume: a tall yellow box round the head with a hole for the face and a brown stem on top, a yellow tube round the body and legs, and the banana's brown end sticking out at the right foot; the arms and feet bare.
  - **Scarab:** a gold-and-lapis Egyptian set: a striped nemes with lappets either side of the face and a cobra on the brow, a gold cuirass with a stepped collar, lapis bands and belt, striped square shoulder guards, a kilt with a lapis key on each leg's hinged panel, and tall chequered boots. Its icons are the owner's own.
- **Their weapons** (Arms VII set arms, epic, creative only): the **Sentinel Longsword**, gold with a jet-set guard, fights as a longsword and marks what it strikes (it glows for 4 s); the **Frost Knight Greatsword**, a blade of glowing ice with a crossguard of frost, fights as a greatsword and slows what it strikes (Slowness II, 3 s); the **Wight King Zweihander**, a long slate blade with a jagged guard and a glowing cyan gem, fights as a zweihander and heals its wielder half a heart a hit; the **Reaper Scythe**, a short scythe whose crescent is five bone-white links round slate hollows, one for each hand as the owner drew it, fights as a kama and withers what it strikes (Wither, 3 s); and the **Sentinel Shield**, the owner's four-pointed gold star in 3D with a raised diamond frame and a chequered heart, which blocks as a steel heater shield does and lasts twice as long.
- **Beside the owner's other sets, in other strengths** (helmet to boots): Berserker 3, 8, 7, 3, mended with quartz; Paladin 4, 8, 6, 3, long wear and good enchanting, mended with amethyst shards; Templar 3, 9, 7, 3, fire resistant, mended with netherite; Sentinel 4, 8, 7, 3, tough, mended with gold; Frost Knight 3, 8, 7, 3, mended with blue ice; Wight King 4, 8, 7, 3, the steadiest of the cold sets, mended with packed ice; Reaper 3, 8, 6, 3, good enchanting but less toughness, mended with bone; Banana 2, 6, 5, 2 (a costume: iron's defense, the best enchanting), mended with yellow wool; Scarab 3, 8, 6, 3, gold's enchanting but less toughness, mended with lapis lazuli.
- **No recipe or drop yet:** creative tab (Combat) only, until the owner decides. The names are placeholders. Record: [armor-designs-8-october.md](docs/features/armor-designs-8-october.md).

### Added: Four armor designs (Dread Knight, Valkyrie, Wayfarer, Spartan)
- **Four new armor sets from the owner's own designs,** each worn as a 3D model with four 16×16 icons of its own:
  - **Dread Knight:** a near-black great helm crowned with light grey merlons, banded pauldrons with small spikes, a mottled muscle plate, a riveted strip skirt and banded greaves.
  - **Valkyrie:** a gold laurel wreath with a white feathered wing at each temple (the face shows), a white muscle cuirass with red cloth wound on the shoulders and red streamers, gold-banded bracers, a studded leather skirt and winged greaves.
  - **Wayfarer:** a deep navy hood edged in teal (the face shows), a cloak with a silver clasp hanging longer on the right, a studded leather kilt and brown boots with winged ankles.
  - **Spartan:** a gold Corinthian helm with a T-shaped face and a red and orange crest sweeping to the right shoulder, a gold muscle cuirass, red cloth and a cape over the right shoulder, a scrolled gold pauldron on the left, a studded leather skirt and gold greaves with knee caps.
- **Beside the owner's other sets, in other strengths** (helmet to boots): Dread Knight 4, 9, 7, 3, toughness 3.5, fire resistant, mended with netherite; Valkyrie 3, 8, 7, 3, enchantability 24, mended with phantom membranes; Wayfarer 3, 8, 6, 3, the best enchanting (30) and longest wear, mended with leather; Spartan 4, 8, 7, 3, toughness 3.5, mended with bronze ingots. Starting numbers for the owner to set.
- **No recipe or drop yet:** creative tab (Combat) only, until the owner decides. The names are placeholders. Record: [four-armor-designs.md](docs/features/four-armor-designs.md).

### Added: Pharaoh Armor
- **Pharaoh Armor, the last of the owner's five new armor tiers:** gold and teal plate with red gems over tan linen, worn as a 3D model in the owner's own design: a nemes headdress whose striped side flaps rise into rounded teal humps above the crown, with the uraeus on the brow over a tan face plate; a broad collar with a red gem, a breastplate framing a teal square and gold-framed teal bracers; a skirt of striped lames dipping in a V; sandal-greaves. Four 16×16 icons of its own.
- **Beside the others, in other strengths:** defense 4, 9, 6 and 3 (helmet to boots), toughness 3.0, knockback resistance 0.1, durability 451, 656, 615 and 533, enchantability 22. Fire resistant, and repaired with gold ingots.
- **No recipe or drop yet:** creative tab only, until the owner decides. Record: [pharaoh-armor.md](docs/features/pharaoh-armor.md).

### Added: Sunset Gem Armor
- **Sunset Gem Armor, the fourth of the owner's new armor tiers:** a set in a sunset gradient, cream at the crown to coral at the feet, worn as a 3D model in the owner's own design: a crown of gem shards trailing sparkles, shard wings rising from the shoulders, a striped breastplate, and a striped skirt with red flaps and shards at the hips.
- **Beside the others, in other strengths:** defense 3, 8, 7 and 3 (helmet to boots), toughness 3.0, knockback resistance 0.1, durability 528, 768, 720 and 624, enchantability 25: it lasts longest and enchants best of all. Not fire resistant, and repaired with amethyst shards.
- **No recipe or drop yet:** creative tab only, until the owner decides. Record: [sunset-gem-armor.md](docs/features/sunset-gem-armor.md).

### Added: Hades Armor and the Hades Scythe
- **Hades Armor, the third of the owner's new armor tiers:** dark slate plate worn as a 3D model in the owner's own design: a narrow helm with a beaked visor and two great horns, layered pauldrons rising toward the outside, a V of bars on the breastplate, a flared plate skirt, and a blood-red tabard from chest to hem.
- **Beside the other two, in other strengths:** defense 3, 8, 7 and 3 (helmet to boots), toughness 4.0, knockback resistance 0.2, durability 462, 672, 630 and 546, enchantability 12. Fire resistant, and repaired with netherite ingots.
- **The Hades Scythe,** its weapon: a scythe in the owner's design (a near-black snath, red grip wraps, a diamond pommel and a curved slate blade) that fights as every scythe does and withers what it strikes. It is the first arm of a new line, the owner's armor sets.
- **No recipe or drop yet:** creative tab only, until the owner decides. Record: [hades-armor.md](docs/features/hades-armor.md).

### Added: Reforged White Diamond Armor
- **Reforged White Diamond Armor, the second of the owner's new armor tiers:** an icy white and pale cyan set worn as a 3D model in the owner's own design: a big V crest and rising wing bars over a charcoal face plate, wide winged pauldrons over a lavender-edged V on the chest, a long skirt of tassets in an A over light and lavender stripes, and diamond plates on the toes.
- **Beside Bloodthorn, in other strengths:** defense 4, 8, 7 and 3 (helmet to boots), toughness 3.0, knockback resistance 0.1, durability 495, 720, 675 and 585, enchantability 20. Not fire resistant, and repaired with diamonds.
- **No recipe or drop yet:** creative tab only, until the owner decides. Record: [reforged-white-diamond-armor.md](docs/features/reforged-white-diamond-armor.md).

### Added: Bloodthorn Armor
- **Bloodthorn Armor, the first of the owner's new armor tiers:** a crimson plate set worn as a 3D model in the owner's own design, with a fan of thorn-like spikes behind the helm, layered pauldrons and diamond plates on the forearms and knees.
- **A step above netherite:** defense 3, 9, 7 and 3 (helmet to boots), toughness 3.5, knockback resistance 0.15, durability 440, 640, 600 and 520, enchantability 15. Fire resistant, and repaired with netherite ingots.
- **No recipe or drop yet:** the owner will decide later whether bosses drop it or it is crafted, so for now it is in the creative tab only. Record: [bloodthorn-armor.md](docs/features/bloodthorn-armor.md).

### Added: Knight armor for bronze and steel
- **Bronze and steel armor wear the owner's own knight design,** as real 3D models much bigger than vanilla armor, with many parts sticking out:
  - a helm wider than the head, with a big tilted crest plate in chevrons, horn and cheek fins, a nasal bar, eye slits and a gold collar;
  - a forward-angled chevron plate over the chest, layered pauldrons, and vambraces with flared cuffs;
  - a leather belt over a dark under-layer, and a skirt of plate bands to the ground, hung from the legs so it moves with them.
- **Steel** is the design as drawn, in light steel grey with mid and dark greys in hammered strips. **Bronze** is its steam-age make: warm copper-bronze with brass trim, rows of brass rivets, a brass knob on the crest and a brass belt buckle.
- **New 16×16 inventory icons** for all eight pieces, drawn to match, and the handbook's Bronze and Steel Gear page describes the new look.
- **Any armor piece can now have a 3D model:** boxes of any size, at any angle, on any body part, drawn on players, mobs and armor stands, with vanilla's glint when enchanted. The exosuit's 3D parts use the same layer and look as before.
- **Nothing else changes:** same items, IDs, stats and recipes, so armor you already have just looks new. Trims stay on the item but do not show on the 3D models, and babies wear none.
- The old steampunk and kaiserpunk looks are no longer worn by bronze and steel armor; Steampunk Armor and Kaiser Armor (below) keep them. Record: [knight-armor.md](docs/features/knight-armor.md).

### Framework foundation and Jugcraft Complete
- One pinned dependency manifest makes animation, spellcasting, UI, texture, and creature frameworks available to contributors and AI agents; see [FRAMEWORKS.md](docs/FRAMEWORKS.md).
- The Build workflow produces an importable Modrinth `.mrpack` with the original Jugcraft JAR and hashed upstream library downloads, plus standalone release dependency metadata. No startup downloader or third-party JARs are added to Jugcraft.
- Optional Jade support displays shared machines' energy and processing progress from server data. Fabric Loader is raised to 0.19.5 to satisfy the selected actual artifacts; Minecraft stays 26.3.
- Actual verification and unperformed playtests are recorded in [framework-foundation.md](docs/features/framework-foundation.md). Publication on Modrinth remains a maintainer release step.

### Added: The fall fair in Minecraft's own look
- The owner found that nothing in the fall fair looked like Minecraft (its eyes, mouths, brass and bulbs). Every fair texture is now drawn at vanilla's density, 16 texels to a block, and scaled up to its file's size, so models and UVs are unchanged.
- **High Striker:** painted planks, a brass rail and bell lit like a gold block, flat glass lamps, a plain scale with ticks and a pixel-star sign. **Ring Toss:** a plank crate banded in red and white, bottles lit like vanilla glass and a striped ring.
- **The plushes** (pumpkin, jumbo pumpkin, ghost, bat, black cat, squirrel and werewolf, and their acorn, bow, leaf, rosette and stem) are wool-like felt with square pixel faces.
- **The piñatas:** paper fringe in tiers, cut along each tier's foot, torn to newsprint; the pumpkin's carved face, the bat's eyes and fangs, scalloped wings, the rope and the blindfold.
- **The balloons:** the basket's wicker, leather, planks, steel, brass, coils, tank, gauges and rope; the Harvest Stripes and Harvest Moon envelopes in flat gores with pixel maple leaves, a pixel moon, a witch and bats; the Jack-o'-Lantern's carved face; the flame; the mooring post.
- **Items:** the striker, mallet, ring, piñatas, stick, blindfold, three balloons, burner and pibal are 16-by-16 pixel icons.
- The Ferris wheel is left to the art-fix work (#219). Before and after: [fall_fair_minecraft.jpg](docs/images/fall_fair_minecraft.jpg).

### Added: Clean textures, set 15: the Witching Season's last noisy props
- The dark wood of the broom rack, curiosity cabinet, iron-bound coffin, ribcage bookcase, haunted dining chair, silk spool stack and grandfather clock is long-grain planks, as vanilla planks, in place of blotches.
- Flat, without speckle: the coffin's and chair's velvet, the brain vat console's brass, the lightning harness's copper, the dustpan's tin, the witch's lantern's violet glass and both pillar candles' wax.
- The effigy ashes and the hearth ash item are soft clumped ash with a few charred flecks and embers set in place. The egg sacs' web mat is drawn as threads.
- The workshop's tinted brew, fume and wax textures (the cauldron's brew, its fumes and the candelabra's candles) are smooth, without per-pixel grain.
- The props are drawn in the clean style (`flora_art.QUIET`), switched on for just these builders. The Beating Heart Jar keeps its original look, which the owner loves. The 25 props the owner's art review redid (#219), the faces, moons, webs, pumpkins, plushes, garlands and glows are unchanged. IDs and models are unchanged. Before and after: [clean_textures_set15.jpg](docs/images/clean_textures_set15.jpg).

### Added: Clean textures, set 14: the town and the Pixel Hollows
- The 18 townsfolk wear clean cloth in the manner of the vanilla skins: lit in its upper half, a shade darker below and at the hem, with no speckle. Their hair is flat with neat strands. Each keeps the face, hair style and outfit it had. The raiders, which share the painter, are unchanged.
- The Retro Trader keeps his buffalo-check flannel. His hair and beard are drawn in strands, not a checkerboard, his trousers are a plain navy plaid and his sneakers are plain black.
- Circuitstone is dark slate in thin streaks, as vanilla deepslate, under the same copper traces. The bricks are laid as deepslate bricks and polished circuitstone is smooth.
- The Jug Teller (the town's ATM) is flat cast iron and brass, lit along the top and left.
- The Pixel Hollows map is plain parchment with its folds.
- The arcade cabinet parts, crystals, pixel lamp, shard and map marker were already flat and are unchanged. So are the guide screenshots, the tower outlines and the machine screens, which are pictures rather than textures. IDs and models are unchanged. Before and after: [clean_textures_set14.jpg](docs/images/clean_textures_set14.jpg).

### Added: Clean textures, set 13: tower and drones (the noisy few)
- The control screen and holo table, loose and formed, are flat navy glass behind their grid lines and glows, with plain steel sides lit along the top.
- The cargo packager's white panels are flat with a short sheen.
- Reinforced concrete keeps its soft mottling, seam and tie holes, with a few spaced pores in place of a per-pixel jitter.
- The survey stake is straight-grained wood under its red band.
- Silicon carbide armour is left to the art fixes PR (#219), which redraws the tower's armour plates. The hangar and landing pads, drones and other tower blocks were already clean. IDs and models are unchanged.

### Added: Clean textures, set 12: dieselpunk war machines (the noisy few)
- Kaiserworks marble (plain, fluted and the column end) is calm cream in soft clumps with one smooth grey vein, like vanilla calcite. The black station tiles are flat with a lit edge, and their cream tiles are clumped marble.
- The glass tank's fluid is near-white in soft clumps with even diagonal ripples. Construction foam is clumped with evenly spaced pores.
- The artillery deck and wicker are left to the art fixes PR (#219), which reworks their painters, along with the 22 textures it fixes. The other war-machine tiles were already clean. IDs and models are unchanged.

### Added: Clean textures, set 11: weapons and armour (the powders)
- Four noisy powders are clean 16×16 heaps in the manner of the set 9 dusts, following the item icon rules (#201): ammonium perchlorate, iodine, silver iodide and cement mix.
- Left to other work:
  - the arms icons, which #201 redraws;
  - the four textures #219 fixes;
  - the bronze and steel armour, which gets the owner's new 3D knight look in #215's follow-up.
- The exosuits, the rocketry items and the construction items are already clean pixel art and are unchanged. IDs and models are unchanged.

### Added: Clean textures, set 10: machines
- Every bronze-framed machine front loses its speckle.
  - The steel plate is one tone with a few soft clumps, recessed under the frame's lit top and left edge.
  - Windows are recessed, with their contents in soft clumps instead of random pixels.
  - A running machine's glow (fire, arc light, a crystal's shine) rises in smooth bands to its brightest at the bottom, like a furnace.
  - The crusher's jaws are two neat rows of teeth, the sieve is a crossed mesh, the circuit assembler shows gold traces, and the boilers are round and lit at the top left.
- The machine casings, tanks, pump sides, plinths, the geothermal tank and stack, the crucible, the solar panel, the belts and the conveyor are cleaned the same way. Bricks take one tone a brick, lit along the top.
- The five refinery machines whose fronts were committed as pictures in the old style (catalytic reformer, chemical mixer, crystal grower, oil sand extractor, vacuum distillation unit) are now drawn by the same code.
- The leftover materials match set 9:
  - salt, phosphate, lepidolite, monazite, sawdust, lithium carbonate, rare earth oxide and thermite are clean heaps;
  - bitumen, silicon, coke, borax and ferroboron are lumpy chunks;
  - bauxite, oil sand and tincal are evenly speckled rock.
- The owner's library (#207) was used as a style reference only: many of its machine files match other mods' textures, so none is copied. IDs and models are unchanged.

### Added: Clean textures, set 9: metal parts, petrochemistry and deposits
- The ores, storage blocks, raw ores, ingots and nuggets are left to the material sets (#218), as the owner chose on 6 October 2026. This set cleans the rest of the materials.
- Metal parts are drawn in `tools/material_style.py`:
  - dusts are clean heaps;
  - plates have an outline and a sheen;
  - gears have eight square teeth;
  - wire is wound on a spool;
  - washed ore is a clean chunk with drops of water.
- Bronze blend and sulfur dust are clean heaps too.
- Petrochemistry:
  - The fluid buckets share a new iron pail with the fluid in its mouth.
  - Flowing fluids run in soft streaks, and still fluids are mostly one tone with lighter swells.
  - Guncotton, PVC resin, alumina, titanium sponge, asphalt, asphalt binder, fertilizer, the cracking catalyst and the turbocharger lose their random speckle.
- The four resource deposits are packed rubble of stone and ore lumps, in the manner of gravel. IDs and models are unchanged.

### Added: Clean textures, set 8: crops (outside the tree work)
- Only the crop textures that open PRs #195 (wood repaint) and #206 (new trees) don't touch are changed. Trees, logs, wood, leaves, planks and saplings are left to those PRs.
  - The squash and gourd skins vary in soft clumps with even stripes; the warty gourd keeps its warts.
  - The cooking-pot soup is a clumped surface with a few glints.
  - The turnip lantern's lid stalks are lit at the top.
  - The sauerkraut's shreds run in even lines.
- `gourd_side` now paints cleanly by default. IDs and models are unchanged.

### Added: Clean textures, set 7: the fall fair
- The fall fair's high-resolution textures (the midway games and plush prizes, the piñatas and the hot-air balloons) lose their soft mottling and fine noise. They are painted in the clean, cartoon style of the werewolves:
  - Every tone sits in a few flat bands. Brass, glass and glowing bulbs shade in clean steps, and felt, enamel, canvas and wood are even.
  - Piñata fringes hang at even lengths, and the torn holes are round.
  - The squirrel plush's tail has a cream band of points along its tip.
- `tools/fur_paint.py` gains `clean_painter` and `clean_ramp`, used only by the fair's modules. Faces, stitches, stripes and painted designs keep their drawn detail. IDs and models are unchanged.
- The Ferris wheel is left to the art fixes PR (#219), which redraws it so it no longer flashes; its textures are main's here.

### Added: Clean textures, set 6: fall food and kitchen
- The pies and hearth oven, the Harvest Feast Table, the candy kitchen, the cider mill, the preserves pantry and autumn foraging lose their per-pixel speckle and are painted in the manner of the vanilla blocks with `tools/block_style.py`.
  - The oven is vanilla-style bricks, its stone is stone and its embers a heap of glowing coals.
  - Pie tops are a woven lattice, each strip lit along one edge, over a clumped filling, and the cut side has clean crust layers.
  - Wood is planks: the feast table, cider press, barrel staves and pantry shelves.
  - Apple leaves are clumps with gaps, lit along their tops, as vanilla leaves. Graniteware is flecked evenly, as vanilla diorite.
  - Raw pies, slices, dough, rock candy and burnt sugar are lit at their upper left instead of speckled.
- IDs and models are unchanged.

### Added: Clean textures, set 5: the Halloween harvest and festivities
- The Halloween harvest and festivities lose their per-pixel speckle and are painted in the manner of the vanilla blocks with `tools/block_style.py`. That covers the giant pumpkins and the heirloom pumpkins, the Harvest Scale, scarecrow, corn shocks and ornamental corn, gourd birdhouse and mums, the regatta and trick-or-treat costumes, the Judging Stand, festivity candles and ghosts, the Headless Horseman, wisps and trebuchet, the corn maze and ghost hunting, and the knitting, chandlery, firework launcher, spirit board, theremin, ofrenda, mooncakes and broomstick.
  - Pumpkin skins vary in soft clumps; the giant pumpkin stays mirror-symmetric.
  - Wood is planks, and straw and bark are streaks. The gravestone is stone and the trebuchet's counterweight cobblestone.
  - Ornamental corn ears are one colour with an accent kernel. The mums are round blooms over a leafy mound, and marigold petals lie in neat clumps.
  - Sugar skulls, pan de muerto, popcorn balls, guts and fizz rocks are drawn as shapes instead of scattered pixels.
- `gourd_side` in `tools/festival_textures.py` gains an opt-in `clean` mode, used here only, so the festival gourds of a later set are unchanged until that set. Faces, letters and cut-outs keep their drawn detail. IDs and models are unchanged.

### Added: Clean textures, set 4: Halloween decorations, batches 8 to 14
- The next seven batches lose their per-pixel speckle and are painted in the manner of the vanilla blocks with `tools/block_style.py`: the mad scientist's lab, the yard inflatables, porch witch, poseable skeleton, grasping hands, spooky sign and dead tree, the archway and lanterns, the brazier and the shadow-puppet lamp's wood, the party games (jump-scare trap, costume runway, bowling, scoreboard, candy cache, ghost bell, fortune teller), the bonfire and hayride, the treats, and the costumes and costume trunk.
  - Surfaces are one ground tone with small clumps of its neighbours. Wood is planks.
  - Bark, hair, straw and brushed steel are streaks.
  - The archway is mossy stone bricks, the bonfire ring cobblestone, and the brazier's coals and the trunk's clothes are heaps.
  - Sweets get evenly set sugar crystals and raisins.
  - The costumes' black cloth, fur and bat wings are clumped instead of speckled.
- Faces, letters, cards and cut-outs keep their drawn detail. IDs and models are unchanged.
- The Shadow Puppet Lamp's brass, candle and paper panels, the floating witch hat's candle (it shares the lamp's candle) and the Specimen Jar's eye are left to the art fixes PR (#219), which redraws them.

### Added: Clean textures, set 3: Halloween decorations, batches 1 to 7
- The first seven batches of Halloween decorations lose their per-pixel speckle and are painted in the manner of the vanilla blocks, with `bs.fill` and the other painters in `tools/block_style.py`. This covers the string lights, candy bowl, coffin, haunted portraits, fog machine, luminaria, floating candles, sconce, bunting, cauldron and hex brews, apothecary shelf, crystal ball, grimoire, broom, bobbing tub, pumpkin crate, hay bale seat, autumn wreath, leaf piles, rocking chair, lurking eyes, silhouette windows, music box, giant spider, chandelier, pipe organ, suit of armour, dust sheet, spirit mirror, curtains and creepy doll.
  - Each surface is one ground tone with small clumps of its neighbours.
  - Wood is planks; straw, husk and bristles are streaks.
  - The candy bowl is a heap of round foil sweets with candy corn on top, and the ornamental corn is rows of plump kernels.
  - Leaves are shaded along their lower edges; lace is eyelets; the plume is feathers in bands.
- Faces, cut-outs and portraits keep their drawn detail. Every pattern tiles, and IDs and models are unchanged.

### Added: Clean textures, set 2: the graveyard
- The memorial stones (marble, slate, granite and sandstone in all four weathering stages, their carved, rough and knotwork faces), the chippings and flower beds, bronze and iron, oak and roof slates, the crypt set, the grave mound, the Mourning Angel, the Pop-Up Skeleton (its skull now cute, with no nose hole) and the graveyard flora, painted in the manner of the vanilla blocks so they sit beside them: stone as stone (granite speckled like vanilla granite, rough granite like cobblestone), oak as planks, chippings as gravel, beds as dirt, and moss and lichen in clumps as on mossy cobblestone; no salt-and-pepper speckle and no flat fills. Every pattern tiles, so blocks still join up. IDs and models are unchanged.

### Added: Clean textures, set 1: the fall creatures
- The owner asked for every texture to lose its noise and look cleaner, flatter and cuter, set by set (15 sets, after the reference pictures). Set 1 is the fall creatures: the **werewolves**, their rugs and pelts (big flat locks of fur in neat rows instead of fine hairs), the **turkeys** and the roast turkey dinner, **squirrels** and acorns, the **crow**, the **Pumpkling**, the **Hay Golem** and the **Bat House**: flat tones and regular patterns, no random speckle. The Bat House's planks and guano, the turkey's bone and skin and the silver platter are painted in the manner of the vanilla blocks (`tools/block_style.py`). IDs and models are unchanged.

### Added: Arms icons in the owner's 16×16 style
- **Every arm's inventory icon is redrawn at 16×16** in the owner's manner: on the diagonal, a one-pixel outline in each part's own dark tone, flat tones lit from the top left, chunky parts that read at a glance. Each kind is one map of letters in `tools/arms_icons/` that the owner can edit.
- **The big arms show the whole weapon**: the longsword, greatsword, zweihander, executioner, battleblade, moonblade, sabre, nodachi and halberd fit the full diagonal whole, every part present, instead of cropped.
- **The 3D models in the hand stay**; bronze becomes the owner's tan bronze and steel a dark blue-grey, in the icons and models alike, which also re-tints the bows, arbalests, shields and some variants. Record: [arms-icons-16.md](docs/features/arms-icons-16.md).
- **[ITEM_ICONS.md](docs/ITEM_ICONS.md): rules for every item icon**, measured from these maps: 16×16 at vanilla's scale, the whole item with every part present and never cropped, a one-pixel outline in each part's own dark, light from the top left, flat tones, each metal its own palette, vanilla kinds in vanilla's form drawn fresh. `tools/check_icon_maps.py` checks the maps, the materials they declare (`tools/icon_materials.py`) and the item icons' sizes, and runs with its self-test in CI through `tools/check_mod_data.py`.
- **New item icons are 16×16 only**: ART_DIRECTION.md and CLAUDE.md now say so, `tools/hd_art.py` draws no new icons, and the 122 larger icons are frozen in `tools/legacy_item_icons.txt` until each is redrawn; CI fails any other item icon larger than 16×16.
- **The 17 war machines' icons are redrawn at 16×16** after merging main, whose 32×32 icons failed that rule: the five tower guns, the Siege Mortar, Self-Propelled Howitzer, Flak Gun, Range Finder, the four shells, the Landship, the Diesel Walker, the Zeppelin and the Observation Balloon. Each is a map in `tools/item_icons/` (drawn by `tools/item_icons.py`, replacing `tools/gun_icons.py`) and keeps its old cue; the legacy list is unchanged.

### Rules: fan homages allowed
- Jugcraft may now include fan homages: creatures, characters and things inspired by films, shows, cartoons and games, with changed names and the owner's approval. Their textures, models, sounds and code must still be made for Jugcraft or licensed; nothing ripped or copied from the source. See [LICENSE_POLICY.md](LICENSE_POLICY.md#fan-homages).

### Added: New trees, batch 1
- **Ten new trees, from the tree roster** ([TREES.md](docs/branches/TREES.md)):
  - **Firs:** stunted firs and thin bog firs, narrow subalpine fir spires and low fir bushes.
  - **The tamarack:** a thin larch, gold in autumn and bare in winter, over the Muskeg, Bog and Fen.
  - **Grey dead snags**, straight and bent, in the dead and burnt biomes, and on the Cinder Barrens' and Wasteland's coarse dirt.
  - **Willow bushes**, and **young aspens** standing in the Hallowed Bog's water.
  - **The swamp cedar**, in the Wetland and Ghost Forest, and the moss-hung **bigleaf maple** of the Temperate Rainforest and Redwood Forest.
- **Cedar, a new wood** from the owner's painted western red cedar: log, wood, stripped log and wood, planks, stairs, slab, fence and fence gate, sage-green cedar leaves and a cedar sapling that grows a cedar, drawn as the other woods are. Its wood is darkened a little so it sits apart from vanilla spruce and the cypress.
- **29 biomes change their trees.** Besides the new trees, firs replace vanilla spruces in the Maple Woods, Redwood Forest and Lake District and join the Seasonal Forest; larches join the Coniferous Forest, Shield and Lake District; aspens replace the Shield's oaks and the Snowpetal Grove's birches and join the Snowy Forest; fallen dead and larch logs, willows, maples, azaleas and a rare great oak join where the roster says; and the Wasteland gets a tree try in every chunk.
- **A wood's recipes follow any switch that grows it:** `jugcraft:feature_enabled` takes an `"or"` list, so the larch's recipes stay on with either Alpine Spawn or the biomes, and the chestnut's with either agriculture or the biomes.
- New chunks only; nothing saved is renamed or removed. Record: [trees-batch-1.md](docs/features/trees-batch-1.md). Not yet played.

### Added: Wood repaint
- **Every wood the mod adds is redrawn** in its colour from the owner's 24 painted woods, matched to the closest painting: bark, log ends, stripped logs and planks, so its stairs, slabs, fences and gates follow too.
- **Drawn as vanilla draws wood:**
  - **Bark:** long vertical furrows, with no rings across the trunk.
  - **Log ends:** square growth rings.
  - **Stripped wood:** straight grain.
  - **Planks:** four lit boards with staggered joints.
- **Every tree's leaves**, in every season's look, are redrawn in vanilla's fine speckle; needles, blossom, fronds and bare twigs each in their own way.
- **The owner's second set** of eight painted woods: the jacaranda takes its mauve wood, and the other seven wait in a bank for new trees (cedar, plane, walnut, wenge, elm, hickory and yew).
- **[NATURAL_TEXTURES.md](docs/NATURAL_TEXTURES.md):** how these woods were drawn, as the rule for every future wood and natural texture.
- Original textures drawn by code (`tools/wood_style.py`); nothing of vanilla's is recoloured. Record: [wood-repaint.md](docs/features/wood-repaint.md).

### Added: Thallite, slice 1
- **Thallite, the Earth school's green metal** (the owner's chartreuse set): Thallite Ore and Deepslate Thallite Ore in every Overworld biome (veins of 7, 4 a chunk, Y −32 to 48), and rich pockets in Lush Caves and the Glowcap Grotto (6 more veins of 9 a chunk) that show in the cave walls. A stone pickaxe mines it; each ore drops one raw thallite, with Fortune as on vanilla's ores.
- **Smelt it** in a furnace or blast furnace (0.7 xp) into **Thallite Ingots**, which say "Green as a new shoot." Nuggets, ingots and blocks, and raw thallite and raw blocks, go 9 to 1 both ways. No alloy, on purpose.
- **Through the machines:** the Crusher and Pulverizer give 2 a block (the Pulverizer a 10% iron dust too), the Ore Washer 3 and acid leaching 4. The Ore Drill and Prospector find it. **Thallite plates** come from the Metal Press, or by hand at 2 ingots a plate.
- Its own switch, `thallite`: off stops its worldgen and recipes and keeps every block and item. Worldgen reaches new chunks only. A metal's worldgen entry may now name its biomes (as rocks do), placed through a `jugcraft:has_ore/<feature>` biome tag. Gear, arms, the arrow, horse armor and magic come in later slices. Record: [thallite.md](docs/features/thallite.md).

### Added: Material sets: tools, ingots and ores redrawn
- **Every metal's ingot and nugget in vanilla's form:** the ingot is the owner's own, recoloured for each metal and never redrawn, and the nugget a shard drawn fresh from memory; one shared ingot and nugget for all fourteen metals, each in its own five-tone ramp, meant to sit beside vanilla's in an inventory as one set. A client test sets them beside vanilla's iron, gold and copper for the owner to judge. Bronze and steel take the owner's chosen palettes; the other twelve stand on the same ladder, each kept clear of the others and of vanilla's iron, gold and copper.
- **Storage blocks** in the owner's four-panel inlay; **raw ores and raw blocks** as lumpy chunks and packed lumps in each ore's own tones.
- **Ores on vanilla's own stone and deepslate**: each ore block is now vanilla's rock, referenced by name and never copied, with our ore's chunky blobs as a cut-out layer on top, so it matches the rock round it. Three blob layouts take turns.
- **Bronze and steel tools** (sword, pickaxe, axe, shovel, hoe, paxel) redrawn in the same style. Art only: IDs, recipes, drops and worldgen are unchanged and worlds need no migration; the maps are `tools/material_icons/`. Record: [material-sets.md](docs/features/material-sets.md).

### Added: Steampunk Armor and Kaiser Armor
- **The stylized armor looks return as two sets of their own:**
  - **Steampunk Armor:** an aviator cap with teal goggles, a pressure gauge and a copper boiler on the back.
  - **Kaiser Armor:** a black spiked helmet, a field-grey tunic over a steel cuirass, and jackboots: the parade dress of the Winged Cog.
  - Each protects exactly as bronze or steel armor does: the same defense, toughness, durability, enchantability and repair.
- **Made at a smithing table:**
  - a **Steampunk Pattern**, a bronze piece and a copper ingot make the Steampunk piece;
  - a **Kaiser Pattern** (made with an Imperial Crest), a steel piece and a gold ingot make the Kaiser piece;
  - the same pattern and an ingot of the metal turn it back.
  - Enchantments, wear, name, trims and plating carry over. Each pattern craft makes 4, one for each piece of a set.
- **Bronze and steel armor keep their current look in this PR,** so for now each looks exactly like its Steampunk or Kaiser twin. Their IDs, stats and recipes are unchanged, and nothing needs converting. Their new 3D look comes in a following PR.
- The **Goggles On** and **On Parade** advancements (a Kaiser piece also counts for Suited Up), and two handbook pages. Record: [steampunk-and-kaiser-armor.md](docs/features/steampunk-and-kaiser-armor.md).

### Added: Arms VIII, batch 59
- **Thrown arms** in bronze and steel, each an arm in the hand that you can also throw: hold use to wind it back and let go, as you throw a trident. What it strikes takes its damage (up to 8, the trident's), more with Sharpness and the like, and it comes down where it struck as itself, enchantments and wear kept, to be picked up again.
  - **Javelin:** flies far and straight.
  - **Francisca:** a throwing axe that tumbles end over end and knocks a raised shield down for 3 seconds; chops wood in the hand.
  - **Chakram:** a bladed ring that flies flat for 12 blocks, cutting every foe on its way out and back, and returns to your hand.
  - **Harpoon:** keeps its speed underwater and hauls what it strikes towards you, out of the saddle.
- Each flies as its own 3D model, with its own moves in the hand and a wind-up pose. Handbook page, the **Let Fly** advancement, six game tests and a client test with real throws. Record: [arms-viii.md](docs/features/arms-viii.md).

### Added: Trait details on Shift
- **Shorter tooltips, with details on Shift:** gear with traits now lists each trait by name, then "Hold Shift for details". Holding Shift expands the tooltip with a brief description under each name. See [trait-details.md](docs/features/trait-details.md).
- **The arms use it first:** every kind's trait (Backstab, Parry, Execute and the rest), Two-Handed, the weapon arts, and the Arms VII boons, lines and trophies. The longbows, arbalests and shields use it too. Thallite's gear is next.

### Bunker and trench interiors, batch 59
- **Trench Periscope:** two blocks tall, its mirror head looking over the parapet. A comparator reads how many hostile mobs it sees (within 64 blocks, 45 degrees either side, in clear view, every second). Look through it to mark the nearest one as a Range Finder would, for your guns and fire control; sneak to clear the mark.
- **Map Table:** a campaign map on a table. Use it to list the target marks plotted within 256 blocks (who, how far, which way, how long ago); sneak-use it to lay a Fire Control Table within 4 blocks on the next one.
- **Gas Curtain:** a wet blanket across a doorway, two blocks tall, that you walk through. Let down, it keeps chlorine and smoke out of everything behind it; use it to roll it up or let it down.
- **Field Kitchen:** an iron stove that heats a Cooking Pot on top. It burns logs, coal, charcoal and coke, glows and smokes while lit, and only starts a new piece of fuel while a pot stands on it. Hoppers can fuel it.
- **Trench Stew:** beef, a potato and a carrot from the Cooking Pot: 10 food and five seconds of Regeneration.
- **Corrugated Iron** (with slab and stairs), **Timber Shoring**, the caged **Bunker Lamp** (hangs or stands) and the **Bunker Bunk** (sit on the lower bunk; no spawn point).
- New `Spotting.near` lists marks for the map table. Record: [bunker-interiors.md](docs/features/bunker-interiors.md).

### Faster CI: pull requests run only the client tests they need
- The Build workflow's new `choose client tests` job (`tools/select_client_tests.py`) picks the client game test classes that show what a pull request changed. Docs- or data-only changes run none. Build files, mixins and the test harness still run them all. Rules: [TESTING.md](docs/TESTING.md#current-foundation).
- `main` and manual runs still run every class, shared between the three client jobs by rough running time instead of every third class.
- Gradle's downloads are cached between runs.

### No more see-through or flickering models: machines, guns, vehicles, balloons and props (art fixes)
- **Closed from every side:** the grand mortar and the other tower guns, the siege mortar, howitzer and flak gun, the landship, the Diesel Walker, the zeppelin, the observation balloon's envelope and the searchlight no longer show the world through gaps in their barrels, rings, decks and hulls. The exporter drew whole 16-pixel patches of a face away wherever a smaller part sat on part of it; it now removes only what is really covered.
- **No more flashing textures:** collars, muzzle rings, hazard rims, caps, gear teeth, handwheels, the joins between a big machine's blocks and spinning shafts no longer flicker, close up or far away. Every exported model now keeps differently drawn faces at least 0.1 pixel apart (was 0.02, which failed beyond about 30 blocks), including turned parts.
- **Two-sided sheets** (string lights, pennants and garland leaves, floating-candle, Aura Candle, bonfire, witch-fire and Harvest Effigy flames, grapple and zipline lines, the drone depot's doors, the blueprint placement ghost and the quad-drawn decorations) no longer fight themselves. The floating candles' flames read the same way round from behind again.
- **No strips of other textures** on faces that reached past their own (trebuchets, the blueprint table, dead trees, the console desk, weathervanes and others); such faces keep their texture's density, so the drone depot terminal and console desk screens show their screens.
- **Witching Season props closed:** the Coffin Wardrobe, Curiosity Cabinet, oddity jars, Iron-Bound Coffin, sarcophagi, candelabra, Bone Throne, Ribcage Bookcase, Grandfather Clock and the rest no longer show holes where a box left a face out; the Horned Skull Cauldron's hollow is lined walls; the Colossal Skull keeps its open sockets with its front closed beside them; the Farm Stand, Harvest Effigy and tall flowers no longer flicker where their blocks meet.
- **No slits at spinning parts:** the hand crank's axle, the solar tracker's pivot and the heliostat's shaft meet their still parts with no gap.
- A new check, `tools/art_check.py` (run by `check_mod_data.py`), keeps it that way. Rules: [ART_DIRECTION.md](docs/ART_DIRECTION.md#rules-for-everything). Record: [see-through-and-flicker-fixes.md](docs/features/see-through-and-flicker-fixes.md).

### Steel plates, armour plate, hazard plating and concrete redrawn (art fixes)
- The owner called the steel plate blocks and blast-proof concrete horrific and said bastion concrete looks good. These blocks are redrawn the way bastion is: a few flat shades, no random speckle, no rust, and a single split seam where two blocks meet instead of a doubled dark frame round every block.
- **Dieselworks steel set** in one cool blue-grey steel: **Weathered Steel Plate** is one brushed sheet a block with a soft edge, so a wall shows quiet joints rather than a grid of framed tiles; **Riveted Steel Plate** is framed by raised rivets that line up evenly across every joint; the **Ribbed Steel Pillar** is ribbed and shaded like a round column; the **Riveted Band** is a dark strap with a rivet row; the **Steel Grating** is a bar grating whose slots carry on evenly from block to block, with a rail at the top and bottom of each half, so grating slabs no longer end in open prongs; the **Porthole Window** sits in a steel sheet with a steel ring; and the **Steel I-Beam** and the **Amber Cage Lamp**'s base and cap are the same steel, without the old brown frame and rust mark. Slabs and stairs follow, and the set shows as metal on maps. The giant machines, the zeppelin and the vehicles keep their own steel.
- **Steel Armor Plate** (drone tower) is clean 32 px pixel art instead of noise: one thick gunmetal plate a block with chamfered corners that meet round a round bolt boss wherever four plates join, with no dark band from top to bottom. **Hazard Plating**, which the tower lays round armour pads, door bays and deck edges, is now the same plate with a flat band of yellow-and-black stripes, so rim and armour meet as one steel.
- **Blast-Proof Concrete** is heavy cast concrete in two smooth lifts a block, darker and cooler than bastion, with tie holes on an even, staggered lattice. **Concrete** is its lighter, warmer, smooth sibling: one lift a block and two faint form marks, no speckle. Their slabs and stairs follow.
- The rules behind this, taken from the blocks the owner liked and rejected, are in [ART_DIRECTION.md](docs/ART_DIRECTION.md#tiling-building-blocks). Block IDs, recipes and model shapes are unchanged; the models now point at the new textures, so placed blocks just look new. The `jugcraft_dieselworks` screenshot's front row now shows the steel plates' slabs and stairs, and `jugcraft_foam_sprayer` ends with the armour plate and hazard plating with their slabs and stairs.

### The sawmill's blade turns and the sieve is a real vibrating screen (art fixes)
- **Sawmill:** the giant sawmill's blade is now a toothed 24-tooth disc that **turns while the sawmill works** (and stands still when it is idle), its front teeth cutting down into the log. It spins up and runs down smoothly instead of jumping, even on weak power. The checkerboard texture is gone: the blade is plain ground steel with expansion slots and an amber maker's plate you can watch go round. It turns at a steady pace that never looks as if it runs backwards, even at low frame rates. It runs in a slot in the bed under a red hood that leaves the front teeth bare, on an arbor in two pillow-block bearings, belted from the motor, whose pulley turns too. Each bearing face and duct joint is drawn once, whole (no doubled, squashed nut where a block seam cut it). The dust duct and hood no longer stick out of the machine's blocks (no more stretched, flickering textures there), and the amber running lamp and gauge sit on a control box at the front, so the lamp lights while it runs.
- **Sieve:** rebuilt as a vibrating screen. A red screen box on blue coil springs tilts from a feed hopper at the back down to a hazard-striped lip at the front; gravel rides over a grizzly of real steel bars onto a fine woven-wire deck with steel cross bars (the weave, the bolts and the lip's hazard stripes run on evenly, with no seam down the middle), and a vibrator motor on a full-width beam **spins its yellow eccentric weights while the sieve works**. Totes of fines, flint and nuggets stand at the front, a pan under the deck catches the fines, and the control box with the gauge and the running lamp is on the master block.
- The turning parts are drawn by the machines' renderer (`client/MachineRotors`, from `machine_rotor_quads.json`) in both machine styles; the classic pack's sawmill leaves a slot for the blade and its sieve hides the weights. The item icons show the blade. Only machines placed whole show them; a one-block copy from before batch 44 keeps its old look. IDs, blockstates, recipes and behaviour are unchanged. Record: [dieselpunk-giants.md](docs/features/dieselpunk-giants.md#turning-parts-sawmill-and-sieve-5-october-2026).

### The Flying Eyeball, the Specimen Jar's eye, the Shadow Puppet Lamp and the Ferris wheel (art fixes)
- **Flying Eyeball:** no more glowing white square behind its iris. The iris is now a cut-out disc on the eye's own white, which is a warmer ivory, with a small white glint inside the disc. Its wings no longer flicker (each was two faces fighting in one plane) and are repainted clean, in lit and shaded panels between the fingers; the item's wings are two faces 0.1 pixel apart, so they cannot fight either.
- **Specimen Jar:** the eye is rounder and drawn properly on every side as it turns (it used to show a black-topped blue cube most of the time), with a clean white, a few placed veins, a blue iris and a round pupil, its nerve hanging straight down. The jar itself is unchanged.
- **Shadow Puppet Lamp:** no more flashing: each paper panel is one face (it was drawn twice in one plane, fighting its mirror image), and the corners close. Each shadow now lies flat on the walls its light actually reaches, cut to the open wall and to what the flame can see, so it never stands across a wall, hangs over a doorway or falls where no light reaches. A shadow crossing a doorway, a corner or a pillar's edge slides smoothly across it, shown on each side, instead of vanishing or jumping to the next wall in one frame, and it fades out at the end of its range. The paper, silhouettes, shadows, brass, wood and candle are repainted clean at 32 × 32 and 16 × 16.
- **Ferris wheel:** its textures no longer flash as you move. Every part samples its texture at about four texels to a pixel (thin rims and trims used to squeeze a whole 64 × 64 picture), the textures of the wheel and cars are repainted clean in the same red, cream and brass, and no two faces share a plane any more: mitred rim lengths, a sixteen-sided hub, separate planes for the rings, spokes and ties, a closed pivot bar ending inside the rims and one face for each valance. The rims show three bands a pixel each (a warm gold edge, the red and a shaded red); the brass, cream steel and axle are banded along each member (a lit edge, the fill and, where wide enough, a shaded edge), with bevelled plates on squarer faces such as the bearings and the brass cap over the axle. The booth keeps its earlier brass and steel, now as its own textures (`ferris_wheel_booth_brass`, `ferris_wheel_booth_steel`).
- New client screenshots for CI: the eyeballs at night and from behind, the eyeball item held in the hand and in the hotbar, the eye's jar close up, the lamp close up and in a narrow corridor with a doorway (each twice, a few ticks apart), and the stopped Ferris wheel twice from 0.05 block apart. Records: [haunted-house-props.md](docs/features/haunted-house-props.md), [more-halloween.md](docs/features/more-halloween.md), [even-more-fall-additions.md](docs/features/even-more-fall-additions.md). IDs, recipes and behaviour are unchanged.

### Runebound arms as real 3D models (art fixes)
- **The Runebound Nodachi, Moonblade, Staff and War Hammer are now smooth 3D models in the hand**, not stacks of pixel boxes, after the owner's "make like a nicer 3d model ... really be cool and special":
  - **Moonblade:** a crescent of violet moon steel ground to a bright edge, a raised bead of glowing runes following the crescent, a crescent-moon guard with glowing horn tips round a heart crystal in a silver bezel, a spiral cord grip and a faceted crystal pommel.
  - **Nodachi:** a continuous curve whose edge is tempered in a glowing wave with a white line along it, a short panel of glowing runes by the habaki, a gold habaki, an oval tsuba with a ring of light round its rim and a silk-wrapped tsuka with windows of ray skin.
  - **Staff:** dark ironwood with a glowing helix winding up each half, a leather grip between iron collars, and at each end three iron claws holding a floating crystal.
  - **War Hammer:** a flared, chamfered head with a glowing diamond cut into each side, a white diamond raised in it and a stave through both, a moon-gold band, a curved beak, a top spike, langets, an iron-banded haft and a spiral cord grip.
- Curves shade smoothly and edges stay crisp. The details are shapes, not stretched pixels: the Moonblade's glow is a raised bead and the Nodachi's a glowing tempered edge, so each stays a steady line at a distance instead of a dark slot that flickers in and out as the arm turns, and the temper line, sigil and tsuka diamonds stay clean however close they are. The runes, temper and crystals glow at full light in the dark and pulse gently. Every part is closed: nothing to see through, nothing poking through anything else. They are held exactly where the old models were, in either hand, first and third person, and take the enchantment glint.
- **New icons**, rendered from the same models in flat tones as bright as their steel siblings.
- Item ids, recipes and stats are unchanged. Built on Fabric API's model loading and renderer API (already part of fabric-api, so no new dependency): `client/MeshItemModels.java` reads the meshes from `tools/arms_mesh.py`. The old box models stay in the files as a fallback. Record: [arms-vii.md](docs/features/arms-vii.md#runebound-meshes).

### Witching Season props and bigger jars (art fixes)
- **Giant's Beating Heart**: the Beating Heart Jar three times over, 3 × 3 × 3, placed and broken as one: the same jar of red murk, iron lid, label and slowly turning heart on its brass stand, just bigger. A slow redstone clock (40, 50, 60 or 72 beats a minute; use it to change), each beat a two-tick signal of 15 from its first block and a deep heartbeat, stopped by a signal from below; the heart swells with each beat as the jar's does. Glass, a Beating Heart Jar, brass ingots and a block of redstone.
- **Tall Specimen Jar** (the Specimen Jar twice over, 1 × 2 × 1) and **Specimen Tank** (three times over, 3 × 3 × 3): the same jar, glowing fluid, iron fittings and specimen, just bigger, its eye, tentacle, pumpkin or brain bobbing among rising bubbles. Sneak-use to change the specimen; broken, they keep it. Their items are the Specimen Jar's with the specimen in it, a little bigger in a slot. Made from the smaller jar with iron and glass.
- **Horned Skull Cauldron**: the pot is closed and lined, so looking in shows a dark iron well instead of the sky through its walls; the ram skull has square, Minecraft-like eye sockets and its brew glow is a flat square on them. **Skulls everywhere** (the Colossal Skull, Bone Throne, Skull Footstool, sarcophagi, bone piles, ossuary wall) have square sockets in place of #191's round ones with a glint; the lit Vertebra Floor Lamp's sockets glow whole, and the Colossal Skull's night glow shows at last (it was drawn behind the back of its sockets).
- **Bat in a Jar**: a wider, taller jar and a slightly smaller bat, so its wings stay inside the glass. The oddity jars' lids, bases and knobs are clean beveled iron, their murk lies in even bands and their labels are neat lines of ink (the Beating Heart Jar keeps its first look, which the owner loves).
- **No more see-through props**: faces closed on the Colossal Vertebra, Skull and Rib, Curiosity Cabinet, Bell Jar, the four candelabra, Coffin Wardrobe, Iron-Bound Coffin, Ribcage Bookcase, Grandfather Clock, Farm Stand and the Egg Sac Cluster's sacs (its item showed them as hollow boxes); flat cut-outs drawn by the client are one plane each instead of two that flickered; clean wrought iron on the candelabra, moth case, witchlights and Harvest Moon Lamp.
- **No more flicker**: the Iron-Bound Coffin's bands and corner caps no longer lie flush with its velvet when open, nor the Grandfather Clock's finials with its pediment; the Lab Table patient's flashing eyes show the bright middle of their glow. Rules added to [ART_DIRECTION.md](docs/ART_DIRECTION.md#rules-for-everything); record in [witchs-workshop.md](docs/features/witchs-workshop.md#6-the-bigger-jars).

### Big guns, Landship, walker and balloons (art fixes)
- **Works with the art core's exporter:** the gaps you could see through in the guns, the Landship and the Diesel Walker, and the flicker inside each of their parts, are closed by the exact hidden-face exporter (see "No more see-through or flickering models" above). These models need it and are regenerated with it. What follows is what that exporter cannot fix: moving parts, textures, decals, the balloons and the icons.
- **Clean gun steel with real bores**: every big gun's and tower gun's barrel is plain gun steel instead of a grid of bolted panels, each muzzle has one round dark bore, and port covers and hazard signs are drawn whole instead of in fragments. Yellow, khaki and olive housings are seamless painted armour with one weld seam a course; roofs, housings and brakes are long rolled steel plates instead of short offset blocks that read as brick.
- **Nothing cuts through as a gun aims or fires**: the Siege Mortar's deck meets its base and its rails clear the cradle; the Triple Battery's sleeves and housing clear its drum; the Bastion Autocannon and Fortress Rifle have mantlet slots in their roofs; the Self-Propelled Howitzer has a low engine deck the gun swings over and its exhausts behind the crew; the Flak Gun's cradle clears its pedestal. When a gun fires, only its barrel recoils, back through a cradle that stays put, so breeches no longer punch into decks and roofs.
- **Raised barrels stay drawn**: the Grand Mortar, Siege Mortar, Self-Propelled Howitzer, Flak Gun and Landship are drawn whenever any part of them is in view, so a raised barrel no longer vanishes when you look up at it from close by.
- **Landship and Diesel Walker**: no slit under the Landship's turret and no flicker on its muzzle; its smokestacks are shorter, so the cannon no longer passes through them when the turret turns to the rear. The walker's thighs, chest walls, knees and drill no longer share faces with their neighbours, so it no longer flickers as it walks.
- **Observation Balloon**: a smooth, closed envelope with three tail lobes and its own clean canvas texture (gore seams, red and cream bands, the stencilled serial reading level), instead of stair-stepped boxes with gaps.
- **Hot-air balloons have their own basket back**: since batch 51 they had been drawn with the Observation Balloon's small basket and rigging (two parts shared one name); their burner frame, fuel tanks, gauge and load cables show again.
- **Smooth rise**: the Observation Balloon, pibals and hot-air balloons now glide up on every client instead of stepping; the hot-air balloons' envelopes, rope and flame are drawn in the opaque pass so they can't show through themselves.
- **New icons** for the eight guns, the four shells, the balloon, the Range Finder, the Landship, the Diesel Walker and the Zeppelin: 32x32 pixel art with a closed outline, shading and round bores, each gun with a silhouette of its own (the Grand Mortar on its tower, the Fortress Rifle's long barrel and range finder, the Siege Mortar's railed deck).
- IDs, recipes and numbers are unchanged. Record: [big-guns-art-fixes.md](docs/features/big-guns-art-fixes.md); rules in [ART_DIRECTION.md](docs/ART_DIRECTION.md#big-models-drawn-as-quads-guns-vehicles-and-balloons).

### No see-through rims on held sprites (art fixes)
- The Power and Ronin Katanas' outlines and glow fringes, the Rocket Launcher's and the HE, homing and line-throwing rockets' outlines, and the scuba mask's, scuba tank's and free runners' outlines are opaque: they were drawn part-transparent, so the sprite's rim was see-through in the hand. The katanas' fringe is now the blade's own bright glow.

### The flails' balls swing (art fixes)
- **The bronze and steel flails' chain and spiked ball now swing freely** (the owner, 5 October 2026: "flails should have an animated ball that actually flails around"). The ball hangs from the handle's eye under gravity, trails as you walk, turn or look round, is flung round overhead and whips past after a blow, then swings on and settles. It works in third person, in first person (the guard is held a little higher so the ball hangs in sight), on armor stands and mobs, in either hand, and keeps the enchantment glint.
- **A cleaner ball that keeps out of its holder:** a rounded core with a crown of eight spikes and one below, in two tones, on a chain of four oval links; it keeps clear of its holder's own hips, legs and arms as they move, and nothing in it shares a face plane, so nothing flickers.
- **The Bonecarved Flail swings too:** its spine of vertebrae and a horned skull with square, Minecraft-style eye sockets and a row of square teeth (no nose holes).
- Client-side drawing only (`client/arms/FlailHeads.java`, a small chain simulation on the arms motion's own hooks); no ids, recipes or numbers change, nothing is saved or sent, and the icons are unchanged (PR #201 redraws them). The ball has no collision with the world. Record: [arms-restyle.md](docs/features/arms-restyle.md#the-flails-head-swings-5-october-2026). New client test `FlailClientGameTests` (not yet run in CI), which also walks the player and fails if the ball sinks into the body.

### Cute, clean creature decorations: batches 15 to 20 repainted (#191)
- **Smooth fire**: the Ember Bed's hearth now burns in smooth bands of colour on a glow of coals, and the Horned Skull Cauldron's ram skull has big round sockets and no nostrils.
- **Every creature prop of batches 15 to 20 repainted** in a clean, cute style after the owner's reference pictures: flat colour in two or three tones with lit and shaded edges and no speckle. The **Monster Head** is a bright green Frankenstein head with a blunt black fringe and sleepy closed eyes that open glowing when it wakes; the **Flying Eyeball**, plushes, moths and jar oddities are clean and glossy; skulls (the **Colossal Skull** too) have big round sockets with a glint and no nose holes; bones are smooth cream; the gargoyles have round eyes and little fangs; the **Crawling Hand** is the monster's green; the cocoon sleeps, the clock's ghost says "oo" and the **Harvest Moon** has a sleeping smile.
- Shared painters in `tools/cute_art.py`; the rules are in [ART_DIRECTION.md](docs/ART_DIRECTION.md#creatures-and-faces-cute-and-clean). Block IDs and states are unchanged, so placed props keep working and just look new.

### Pumpkin Night, Halloween decorations batch 20 (#190)
- **Red Kuri** and **Kabocha** pumpkins join the heirlooms: seeds from grass, wild patches and the Halloween Peddler; their own hand-carved pumpkins, glowing ember orange and greenish gold; **Red Kuri Soup** and **Kabocha Tempura** in the Cooking Pot; pumpkin pie, heads and the trebuchet as the other heirlooms.
- **Farm Stand**: an owner's two-block stall of six crates under a striped awning. The owner stocks and prices them in Jugs; anyone in reach buys one item at a time and the Jugs go straight to the owner, every sale checked on the server. Only its owner takes it down.
- **Pumpkin Vine Garland** and **Autumn Leaf Garland**, strung between String Light Hooks with warm bulbs that glow when a hook is lit.
- **Harvest Effigy**: a three-block wicker man in a dyeable cloak who wears a carved pumpkin. Lit at night, he burns for 30 seconds; players near get **Harvest Cheer** (Regeneration and Luck) once a night; rain puts him out. Burnt through, he leaves **Effigy Ashes** that give **Hearth Ash**, a weak fertilizer.
- **Singing Pumpkins** in four voices (bass, tenor, alto, soprano), tuned like note blocks over two octaves and sung by redstone, their mouths opening as they sing; the voices are Jugcraft's own, synthesised. Record: [pumpkin-night.md](docs/features/pumpkin-night.md).

### The Laboratory, the Larder and the Dining Room, Halloween decorations batch 19 (#188)
- **Lightning Harness** (fires on a strong signal, a Tesla Coil's arc or lightning, cracking arcs down to a Lab Table and waking its patient), **Brain-Vat Console** (an analogue memory cell: it remembers the strongest signal at its back until a side clears it) and **Crawling Hand** (drums its fingers; powered, it scuttles).
- **Silk Cocoon** (a 9-slot larder hung from a ceiling that wriggles when opened), **Egg Sac Cluster** (pulsing sacs on any faces, like glow lichen; spiderlings skitter out at night), **Web Drape** (a 2 × 2 web curtain that slows you) and **Silk Spool Stack** (three spools, each dyed to any colour).
- **Haunted Dining Chair** (sits you; at night slides out toward a player near), **Floating Table Setting** (laid for dinner, tea or a feast, bobbing over the table, its candle lit with flint) and **Grandfather Clock** (hands on the time of day, the moon's phase in its arch, a pulse and the hour struck each hour, a comparator reading the hour, and a face at the glass at midnight).
- **Witchlight Lamp-Post**, **Path Stake** and **Hanging Witchlight** (wake as a player comes near, linger, then sleep; redstone keeps them awake; five dye colours).
- **Yard Silhouette** (six black cut-out figures whose eyes glow at night, in sixteen turns) and the **Harvest Moon Lamp** (2 × 2; its face shows tonight's moon, which a comparator reads). Record: [laboratory-larder-dining.md](docs/features/laboratory-larder-dining.md).

### The Crypt and the Ossuary, Halloween decorations batch 18 (#186)
- **Iron-Bound Coffin** (54 slots; locks to a **Skeleton Key** cut from a **Key Blank**, and then opens only for someone holding that key and refuses hoppers and pipes; keys copy onto blanks at a crafting table) and the **Coffin Wardrobe** (a skeleton mannequin behind glass that swaps the armour you wear for the armour it holds; cursed pieces stay on).
- **Stone, Deepslate and Blackstone Sarcophagi** (27 slots under a lid that slides aside over a skeleton; the Stonemason's Chisel carves the lid as a knight, a lady or a skull; at night, shut, they sometimes knock).
- **Bone Throne** (a seat whose crest's eyes glow while sat in at night), **Ribcage Bookcase** (a chiseled bookshelf that powers enchanting tables), **Skull Footstool** and **Vertebra Floor Lamp**.
- **The Buried Colossus**: a 2 × 2 × 2 **Colossal Skull** whose jaw drops on redstone, **Colossal Ribs** that meet as an arch, **Colossal Vertebrae** that line up as a spine, and a **Colossal Femur**.
- **Gargoyle Sentinel** (watches for monsters within 16 blocks and gives a signal by their distance), **Gargoyle Rainspout** (pours rain into a cauldron below) and **Chimera Finial** (a rain and storm sensor that spreads its wings in a storm). Record: [crypt-and-ossuary.md](docs/features/crypt-and-ossuary.md).

### The Witch's Workshop, Halloween decorations batch 17 (#183)
- The [Witching Season plan](docs/features/witching-season.md): twenty Halloween and fall prop sets from the owner's reference pictures, in four batches, then two bosses (a scythe-bearing reaper and a spider seamstress) in their own pocket-dimension lairs. What the uploaded pocket-dimension and animation mods and the Soulslike Weaponry boss page taught, and how Jugcraft does the same without new dependencies.
- **Horned Skull Cauldron** (holds water or three bottles of one potion with nothing gained or lost; bubbles and fumes over heat; the **Brew Ladle** wafts a lasting potion onto up to four players near at a quarter duration; ingredients float in it) and the **Ember Bed** heat source.
- **Wrought-iron candelabra**: the Floor Candelabrum, Table Candelabrum, Wall Girandole and Branching Chandelier, in six waxes and four flames, lit by flint and steel, burning arrows or redstone, with wax drips that grow while lit.
- **Enchanted Broom** (anointed with Flying Ointment, it sweeps dropped items into a **Dustpan**) and the **Broom Rack**.
- **Curiosity Cabinet** (nine places behind glazed doors), **Bell Jar** and **Moth Display Case** (the moths stir at night).
- **Oddity jars**: eyeballs that watch, a beating heart that is a redstone clock, a bat that wakes, a two-headed snake and a drumming hand.
- Sculpted props may use 128 × 128 textures (`docs/ART_DIRECTION.md`; the audit allows it). Record: [witchs-workshop.md](docs/features/witchs-workshop.md).

### Arms VII, batch 56 (#184)
- **32 named variant arms,** each fighting as its kind does:
  - **Crafted styles** at a smithing table, from a steel arm, the style's pattern and a material (enchantments and wear kept): **gilded** (gold; enchants as gold), **ironclad** (dieselpunk; lasts twice as long), **bonecarved** (bone; harder against the undead) and **runebound** (glowing runes; marks foes so they glow).
  - **Boss trophies** for eight bosses still to be made, two each with a boon: the Yeti King (frost), the Cinder Tyrant (ember), the Mire Hag (venom), the Crypt Lich (drain, wither), the Iron Dreadnought (shock), the Alpha Werewolf (howl), the Storm Roc (gale) and the Abyssal Leviathan (tide). Their loot tables are ready; creative-only until the bosses exist.
- Glowing parts light up in the dark. A boss brainstorm: [branches/BOSSES.md](docs/branches/BOSSES.md). Record: [arms-vii.md](docs/features/arms-vii.md).

### Arms restyle (#180)
- **Every arm redrawn:**
  - **Icons:** crisp pixel-art icons (32 or 48 pixels, on the diagonal, flat tones, outlined);
  - **In the hand:** a 3D model with real thickness: thin blades with a raised ridge, chunky guards, round grips, deep heads;
  - **Palettes:** copper-bronze with brass and leather; blued steel with gunmetal, rubber and brass rivets.
- **The longbows, arbalests and shield faces** are redrawn to match.
- **Refined:** bevelled two-tone blades with a glint; the pike's langets; the quarterstaff's shod ends; the kama, war pick, katar and kusarigama held larger.
- **Simplified (second pass):** plainer rapier and sabre hilts; bigger, cleaner halberd, bill, pike and war hammer heads; no stray rivets or stones on the heads; a one-grip katar; the kusarigama's chain clear of its handle.
- After studying how Simply Swords, Epic Knights and RPG Style More Weapons make weapons that read well; nothing of theirs is copied. Record: [arms-restyle.md](docs/features/arms-restyle.md).

### Arms VI, batch 55 (#179)
- **Katana:** quick, clean cuts, and its art **Seven Cuts**: seven cuts in a breath across every foe ahead, each leaving an arc of colour in the air (crimson from bronze, pale gold from steel).
- **Brazier mace:** a burning brazier on a haft, with a flickering flame. It sets what it hits alight and lights campfires, candles and the ground.
- **Longbow and arbalest:** a tall bow and a crossbow with a metal prod. Each shot hits harder and flies flatter than a bow's or crossbow's, though they deal no more a second. Drawn and wound in three steps, as vanilla's are.
- **Heater and tower shields,** built in 3D: the heater is quick to raise; the tower covers your flanks and braces you, but is heavy and slow.
- After the owner's reference sheets (studied for their look; nothing is copied). Handbook pages, the **War Kit** advancement, five game tests and a client test. Record: [arms-vi.md](docs/features/arms-vi.md).

### Arms V, batch 48 (#173)
- **Weapon arts:** six new arms in bronze and steel, each with a special move used with the use key, its own animation and its own way of dealing damage. Then a few seconds before it is ready again (shown on the hotbar); plain blows are not held back.
  - **twinblade** (two-handed), **Cyclone**: three spins, each striking every foe all round and drawing them in;
  - **nodachi** (two-handed), **Iaido**: a dash; every foe passed is cut a moment later, all at once;
  - **earthbreaker** (two-handed), **Leap Slam**: leap and slam where you land, hardest at the centre and harder from a height; throws foes up, and the leap costs no fall damage of its own;
  - **katar**, **Flurry**: five quick jabs that all land, then a driving finish;
  - **moonblade** (two-handed), **Crescent**: a wave runs ahead through every foe in line until a wall stops it;
  - **kusarigama**, **Chain Lash**: the chain catches the first foe in line up to 9 blocks off, hauls it in, and the sickle reaps it.
- Every art runs on the server and is seen by every player nearby. Against one foe an art is no better than plain blows; the data check holds it to that.
- Handbook pages, the **Weapon Art** advancement, seven game tests and an end-to-end client test. Record: [arms-v.md](docs/features/arms-v.md).

### The haunted house's props, Halloween decorations batch 16 (#175)
- **Flying Eyeball** (hovers on bat wings and stares at the nearest player), **Pillar Candles** in ivory and black (vanilla candles, one to four in a dripping cluster), **Spider Web** (on any face), and the **Monster's Head** (wakes on a redstone signal: jaw open, eyes glowing, bolts sparking).
- Five **harvest plushes** for the midway's prize table: owl, hedgehog, acorn, corn and maple leaf.
- Sculpted from the owner's reference pictures; 64 × 64 textures. Six game tests, client screenshots. Record: [haunted-house-props.md](docs/features/haunted-house-props.md).

### The churchyard's ornaments, Halloween decorations batch 15 (#174)
- **Gargoyle** on a granite plinth (a graveyard monument: it weathers and takes an inscription), **Bone Pile** (heaps to four layers), **Ossuary Wall** (skulls and long bones), **Giant Bone Hand** (clenches on a redstone signal) and **Witch's Lantern** (violet glass, standing or hanging).
- Sculpted from the owner's reference pictures; 64 × 64 textures. Six game tests, client screenshots. Record: [churchyard-ornaments.md](docs/features/churchyard-ornaments.md).

### The graveyard flora (#172)
- **Seventeen plants for a haunted churchyard**, sculpted like hand-built plants (bent stems, cut-out leaves and petals at angles, bells and berries as little boxes) on 64 × 64 textures: spider lily, snowdrop, deadly nightshade, bleeding heart and the glowing ghost pipe; black rose, foxglove, funeral lily and asphodel (two tall); withered grass and the ghost fern with their tall forms; dead man's fingers; grave moss; shroud moss hanging in strands; creeping ivy.
- **The mandrake:** wild, or grown from its root; a ripe one screams when pulled, sickening bare-headed players within 8 blocks (Mind Your Ears). Its root makes Flying Ointment.
- They grow in the haunted biomes and some of vanilla's. Grave vases take them by colour. Eight game tests, client screenshots. Record: [graveyard-flora.md](docs/features/graveyard-flora.md).

### Arms IV, batch 47 (#170)

### Added: The Armoured Walker, batch 58
- **Armoured Walker:** a heavy walker made from the owner's own Blender model:
  - an octagonal riveted blue-grey hull with a framed gun port and cannon
  - amber lamps, a chain slung across the front and a roof pouch
  - a jointed tool arm and a piston ram arm
  - thigh slabs, angled shins and hinged feet
- Piloted like the Diesel Walker:
  - **hold use:** fires the hull cannon (Heavy Shells, damage only, every 2 seconds)
  - **attack:** rams with the piston (16 damage, throws hard)
  - takes 90 damage to knock down
  - crafted by upgrading a Diesel Walker with steel and pistons
- The **Raider Walker** now uses the same model in raider paint. It rams with its piston and lobs grenades from its hull gun.
- Three game tests and a screenshot scene. Record: [armoured-walker.md](docs/features/armoured-walker.md).

### Added: The raider faction, batch 57
- **Raiders:**
  - the **Grunt** (cleaver)
  - the **Grenadier** (lobs small grenades from range)
  - the **Officer**: rallies raiders near them. Their fall routs the rest. A player's kill takes their **Raider Insignia**.
  - the **Raider Walker**: a raider-built Diesel Walker that punches and fires a shoulder grenade launcher
  - the **Raider Blimp**: cruises over its target and drops bombs
- **Raids:**
  - After a player has played three days, at most once every three days per world, a party gathers 48–64 blocks away and marches on their base (or the town if they are near it).
  - A raid bar shows how much of the party is left. Beating a raid raises the raid level (up to 5: more raiders, blimps from level 2, a walker from level 3). Ignored raids withdraw.
- **Never griefs:** raiders break no blocks, and every grenade and bomb is a damage-only blast that spares raiders. Sentry guns, flak and town guards fight them.
- **Switch:** `raiders.enabled=false` or `raiders.raids=off`. `raiders.walkers`, `raiders.blimps`, `raiders.grace_days` and `raiders.interval_days` tune it.
- **Siege Ladders:** a grunt stuck at a wall props up a ladder and climbs it. It needs mob griefing on, and the ladder crumbles after a minute, dropping nothing.
- **Raider War Horn** (three insignia and a goat horn) calls a raid on purpose. The **Beat Them Back** advancement goes to everyone who sees a raid through.
- **Raider camps:** rare sandbagged camps in the plains, savanna and badlands, never within 512 blocks of the world spawn. Each has tents, a campfire, a supply barrel and a garrison of four to clear.
- Ten game tests and a screenshot scene. Record: [raiders.md](docs/features/raiders.md).

### Added: Fire control, batch 56
- **Fire Control Table:** link up to 8 guns to it with **Fire Control Wire**. It lays every linked gun that has nobody at its controls. Its modes:
  - **Hold:** the guns stand still.
  - **Converge:** every gun lays on the table's target (your Range Finder mark), and a redstone pulse into the table fires one round from each.
  - **Parallel:** as Converge, but the guns' shells land 6 blocks apart across the line of fire.
  - **Creeping Barrage:** as Converge, but each salvo after the first lands 5 blocks further down range, six steps, then starts again.
  - **Sentry:** each gun fires by itself at the nearest hostile mob in the table's sector (90°, 180°, 270° or all round). It never fires within 12 blocks of the gun, or at a mob with a player within 8 blocks of it.
- A gunner aboard a linked gun with no mark of their own has it laid on the table's point, and fires it when they choose.
- Guns the table fires use shells from ready racks only. A comparator reads how many linked guns are ready. A ringing field telephone can give the order to fire.
- Five game tests and a screenshot scene. Record: [fire-control.md](docs/features/fire-control.md).

### Added: Fortifications, batch 55
- **Bastion Concrete** (block, slab, stairs and Jugcraft's first **wall**, which joins diagonally like vanilla's), the crenellated **Bastion Parapet**, the **Steel Ladder** and a redstone-only **Blast Door**.
- **Ammo Hoist:** stack hoists into a shaft. What goes in climbs to the top and into the container on or beside it. Nothing can pull items back out.
- **Ready Rack:** holds shells beside a gun. A gunner with no shells draws from any rack within 2 blocks of the gun. It shows how full it is.
- **Extras:**
  - a strapped timber **Bunker Door** that opens by hand
  - a redstone **Sliding Gate**: panels side by side or stacked open together
  - a **Bastion Parapet Corner**
  - a **Bastion Embrasure** with a gun slit
- Five game tests and a screenshot scene. Record: [fortifications.md](docs/features/fortifications.md).

### Added: Tower guns, batch 54
- Five heavy emplacements for the top of a tower, after the owner's reference picture of a heavy mortar on a turntable mount: a concrete plinth, a railed turntable, a yellow cradle and a fat black barrel.
  - For a 3x3 top: the **Bastion Mortar** and the twin-barrelled **Bastion Autocannon**.
  - For a 5x5 top: the **Grand Mortar**, the long **Fortress Rifle** (out to about 210 blocks) and the three-barrelled **Triple Battery**.
- Each needs a solid top under its whole footprint, and is crewed and aimed like the big guns.
- The Grand Mortar fires the new **Great Shell** (2 Heavy Shells and TNT). Its burst reaches 7 blocks; like every shell, it hurts creatures and never breaks blocks.
- Three game tests and a screenshot scene. Record: [tower-guns.md](docs/features/tower-guns.md).

### Added: Clean steampunk textures, batch 53
- The steampunk textures (`sp_*`) get the same clean style as the dieselpunk ones: flat fills, bevelled plates with rivets, banded sheens, and wear only as a few placed marks. Iron and brass plates, wrought iron, brass, copper, tanks, planks, firebrick, glass, red iron, the hopper inside, water, lava, leaves, bark and soil lose their per-pixel noise.
- Record: [clean-steampunk-textures.md](docs/features/clean-steampunk-textures.md).

### Added: Clean dieselpunk textures, batch 52
- Every dieselpunk texture is redrawn in a cleaner style: flat fills from short palettes, bevelled panels, recessed insets and two-by-two bolts, with wear only as a few small marks at corners and seams. No more per-pixel noise, and rust is now a small stain under a bolt rather than the whole surface. This covers the giants, Dieselworks blocks, zeppelin, Diesel Walker, Landship, trench works, big guns and the dieselpunk machine textures. The mechs and vehicles also get a clean soot texture for their smokestacks and openings, and the kaiserpunk brass and black lacquer lose their specks.
- Rust Plate, Riveted Rust Plate, Ribbed Rust Pillar and Rust Grating are now called Weathered Steel Plate, Riveted Steel Plate, Ribbed Steel Pillar and Steel Grating. Their IDs are unchanged, so placed blocks and items keep working.
- Record: [clean-dieselpunk-textures.md](docs/features/clean-dieselpunk-textures.md).

### Added: Big guns, batch 51
- **Siege Mortar**, **Self-Propelled Howitzer**, **Flak Gun** and **Observation Balloon**, with Heavy Shells, Flak Shells and a **Range Finder** to mark targets up to 256 blocks away.
- Guns turn to a marked target (the gunner's, or a nearby spotter's) and work out the elevation that lands the shell there. Without a mark they fire where the gunner looks. Flak bursts beside flyers.
- Every burst hurts creatures and never breaks blocks. Five game tests and a screenshot scene. Record: [big-guns.md](docs/features/big-guns.md).

### Added: Trench works, batch 50
- Sandbags, timber revetment, duckboards and barbed wire. The wire slows and cuts whatever pushes through it; sneak to cross carefully.
- **Field telephones:** power one's back and every telephone on its dye channel within 256 blocks rings and gives a redstone signal from its front.
- **Searchlights:** a turning, tilting lamp that throws a long beam. Redstone switches it off.
- Three game tests and a screenshot scene. Record: [trench-works.md](docs/features/trench-works.md).

### Added: Arms IV, batch 47
- **Five ornate arms** in bronze and steel, drawn after the owner's reference sheets (nothing of them is copied): set stones (a garnet in bronze, a lit green phosphor stone in steel), flat-toned heads with bright edges, winged guards, lit rims and glints of light.
  - **labrys** (two-handed): its finishing blow whirls right round, striking up to 6 foes about you;
  - **battleblade** (two-handed): each hit wears every piece of the foe's armor;
  - **war fork** (two-handed thrust): half again as much damage to a foe charging at you;
  - **kama**: use on grass, ferns, vines or leaves to cut them all, 3 by 3 by 3;
  - **war pick**: mines stone and ore as its metal's pickaxe.
- Handbook pages, the **Masterwork** advancement, six game tests. The data check now also fails if an arm's id is taken elsewhere. Record: [arms-iv.md](docs/features/arms-iv.md).

### Arms III, batch 46 (#167)
- **Two-handed weapons**, after studying the Fiery Combat add-on the owner sent (nothing of it is used). Greatswords, war hammers, glaives, battle axes, scythes, quarterstaves, pikes and the new kinds now swing with both hands:
  - a click starts the swing, and the blow lands as the swing comes round, on the frame the animation lands it, not on the click;
  - the blow strikes every foe in an arc (up to 2 to 5 by kind);
  - you are slowed while the swing is in the air;
  - the last attack of each combo is a finishing blow, 25% stronger;
  - a shield in the off hand stops the swing.
- The server decides every swing and blow, and refuses vanilla's instant hit with these arms.
- **Four new two-handers** in bronze and steel:
  - **zweihander:** the widest cleave, and a guard;
  - **maul:** its finishing blow shakes the ground;
  - **executioner's sword:** half again as much against a foe at 30% health or less;
  - **bill:** hooks foes towards you and drags riders from the saddle.
- Handbook pages, the **Two-Hander** advancement, seven game tests and an end-to-end client test. Record: [arms-iii.md](docs/features/arms-iii.md).

### Arms II, batch 45 (#164)
- **Eight more arms** in bronze and steel, each with a trait of its own:
  - **dagger:** quick stabs; half again as much from behind;
  - **sabre:** 3 more damage from the saddle;
  - **estoc:** pierces armor, up to 6 more against heavy armor;
  - **battle axe:** chops wood as an axe and breaks a shield's guard;
  - **flail:** a hit slows the foe for 2 seconds;
  - **scythe:** wide sweeps, and reaps and replants ripe crops 3 by 3;
  - **quarterstaff:** knockback and a parry;
  - **pike:** the longest reach (5 blocks), half again against riders and their mounts.
- Each has its own art, guard, combo and first-person strokes. None out-damages its metal's sword per second, which the data check now enforces.
- Handbook pages, the **Armory** advancement, seven game tests. Record: [arms-ii.md](docs/features/arms-ii.md).

### The leaf blower, fall addition 30 (#139)
- **Leaf Blower:** a dieselpunk electric leaf blower in the powered tools' olive drab, gunmetal and chrome, charged at the Charging Station (40,000 JE; 4 JE a tick blowing, 6 vacuuming).
- **Blow** (hold use): a stream 8 blocks long blows items along and nudges mobs (other players only where PvP allows), puts out candles, and **herds leaf piles** a layer at a time into heaps against whatever stops them. Gone with the Wind.
- **Vacuum** (sneak): draws items in and takes leaf piles and leaf litter up into your inventory, a layer for an item, for the composter.
- Only where its user may build. Eight game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-leaf-blower).

### The hot-air balloon fiesta, fall addition 29 (#139)
- **Hot-air balloons** in three designs: Harvest Stripes, a Jack-o'-Lantern special shape with carved faces, and Harvest Moon with a witch across the moon. A wicker basket for four under a twin-coil burner; fuel it with coal, charcoal or coke.
- **Fly by heat:** the pilot fires the burner (jump) and opens the vent (back). It can't be steered: it drifts on **winds that blow different ways at different heights**, eight layers from sea level, the lowest two roughly opposite, so you can fly out low and come home higher (The Box). Gauges show height, heat, wind and fuel.
- **Pibals** to see the winds aloft; **Mooring Posts** to tether a balloon for rides; a **night glow** while the burners fire. Up, Up and Away, The Box, Mass Ascension.
- Envelopes are smooth turned surfaces painted as 768 × 384 wraps; the rest at 64 × 64. Ten game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-hot-air-balloon-fiesta).

### The piñata party, fall addition 28 (#139)
- **Three piñatas** of crepe-paper fringe: a jack-o'-lantern Pumpkin, a seven-pointed Star with tassels, and a winged Bat. Hang one from the underside of a block, and anyone can fill it with anything, up to nine stacks.
- **Swing at it:** a charged swing is a hit (the red-and-white **Piñata Stick** counts two); it swings on its rope, tears at half its hits, and bursts in confetti on the last, spraying its contents out. Piñata Party.
- **The Blindfold** blacks out your view but for a sliver; burst one blindfolded for Blind Luck.
- Painted at 64 × 64. Five game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-piñata-party).

### The Ferris wheel, fall addition 27 (#139)
- **A fairground big wheel**, real-life sized: place the **Ferris Wheel** booth and the wheel rises over it, 16 blocks high, two lattice A-frames, red trussed rims with 128 bulbs, and eight cars (pumpkin, cranberry, mustard and spruce) seating two each.
- **Kinetic power turns it:** a hand crank, shaft or engine against the booth; 12 KE a tick is full speed, a turn in 40 seconds. One player can crank while friends ride.
- **Ride it:** use the booth to board the car at the bottom; sneak to get off and you're set down by the booth. A block in a car's way jams it. Round and Round, and Two to a Car for riding round together.
- It takes the planned ghost train's place (the Haunted Hayride and Jump-Scare Trap already cover that). Painted at 64 × 64. Seven game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-ferris-wheel).

### The fall fair midway, fall addition 26 (#139)
- **The High Striker:** a red-and-white fairground tower five blocks tall with lamps up its front and a bell on top. Hit its pad with a **Carnival Mallet**: the puck climbs as far as the blow was strong, lighting the lamps with a rising note. Ring the bell (a fully charged swing, better still a falling one) to win a prize (Ring the Bell).
- **Ring Toss:** a crate of nine bottles. Toss **Toss Rings** at it from three blocks or more; one over a bottle's neck is a ringer and wins a prize (Ringer!).
- **Plush prizes:** a pumpkin, a ghost, a bat, a black cat, a squirrel, a rare werewolf and the 1-in-102 **Jumbo Pumpkin Plush** (Jackpot). Squeeze one and it squeaks. Step Right Up for any prize.
- Painted at 64 × 64. Eight game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-fall-fair-midway).

### The Pumpkling, fall addition 25 (#139)
- **Wake a carved pumpkin:** use a Wisp in a Jar or a bottle of Ectoplasm on a hand-carved pumpkin with a face, and it hops up as a **Pumpkling**, a pet on little vine legs wearing the face you carved (Little Jack).
- It **follows** you, comes to you from afar, and **sits and stays** when you use it with an empty hand. A torch lights its face (a soul torch, blue); treats heal it.
- **It guards crops:** crows keep away from crops near it, as from a scarecrow. A glass bottle settles it back into its pumpkin.
- Six game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-pumpkling).

### Squirrels and acorns, fall addition 24 (#139)
- **Squirrels**, red and grey, come to forests and taiga by day. They bound about with their bushy tails, climb tree trunks, and bolt when hurt.
- **Acorns** drop from oak and dark oak leaves. Plant one on grass for an oak sapling, or roast it into **Roasted Acorns**.
- A squirrel takes an acorn lying near, carries it off and **buries it**; one in four grows into an oak. Nuts tempt and breed squirrels (Nuts About Squirrels).
- Five game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#squirrels-and-acorns).

### Full-moon werewolves, fall addition 23 (#139)
- **Werewolves** come out of forests and taiga only on full-moon nights, howling: hulking, hunched wolf-men with long snouts, open jaws, long clawed arms and glowing eyes. They leap at players and villagers, shrug off half of any blow and heal, and are gone at dawn.
- **Three kinds, three tiers:**
  - the **Brown Werewolf** (I) raids livestock too, howls its pack to the hunt and flees when badly hurt;
  - the **Snow Werewolf** (II), in snowy woods, bites with frostbite and runs faster on snow;
  - the rare **Shadow Werewolf** (III), the alpha, steps out of the shadows behind its prey; its howl brings Darkness and drives the pack into a frenzy, and a sprig of wolfsbane won't stop it (Leader of the Pack).
- **Silver** hurts them two and a half times as much and stops their healing: a **Silver Dagger** and **Silver Arrows**. Slaying one with silver earns Silver Lining.
- **Wolfsbane**, a wild flower of taiga and forest (plantable and potted), wards them off: they won't hunt anyone holding a sprig or near it (Not Tonight).
- Each kind drops its own **pelt**: two make that kind's **rug**, or cut one into leather. Thirteen game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#full-moon-werewolves).

### Flying broomstick, fall addition 22 (#139)
- **Flying Broomstick**: a Witch's Broom anointed with Flying Ointment and two feathers. Use it to get on; it flies where you look (forward, back to brake, jump to climb), up to about 10 blocks a second, a quarter faster in a witch hat. It hovers where you leave it.
- **Ointment is its fuel**: 2 minutes of flight each, up to 6, burnt only in the air. Run dry, it sinks gently to the ground. Anoint a broom with Flying Ointment; sneak-use takes it back with its flight.
- No fall damage while riding, and slow falling when you get off in the air. The server checks each broom's flight and throws off a rider who moves it impossibly.
- Two advancements (Up and Away; Over the Moon, a challenge), five game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-flying-broomstick).

### Hex brews, fall addition 21 (#139)
- **Hex brews at the Bubbling Cauldron.** Over a fire, a brew takes a hex ingredient: green and a brown mushroom make the **Shrinking Draught**, orange and beans the **Giant's Draught**, purple and a phantom membrane **Flying Ointment**. A hex brew fills three bottles.
- **Shrinking** makes you half size for 3 minutes, small enough for one-block gaps. **Giant** makes you 1.6 times your size for 3 minutes, with a block more reach and a higher step, and is refused where there is no room to grow. A shrunk player with no room to grow back stays small until there is. **Flying Ointment** gives 30 seconds of slow falling.
- Two advancements (Drink Me, Fee-Fi-Fo-Fum), three game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#hex-brews).

### Graveyard pack 4, grounds (#139)
- **Kerbed Grave** in polished granite and marble chippings with an open book; **Planted Grave** whose flower bed goes to weeds as it is neglected; **Memorial Bench** with an inscribed plaque that seats a player; **Open Grave**, freshly dug with boards, straps, a spoil heap, a spade and a waiting cross.
- **Grave Vase**: small flowers make a bouquet of their colour (Flowers for the Dead). Fresh flowers calm the graves within 3 blocks to half the spirits; they wilt in about a day. An open grave stirs spirits twice as often.
- **Cemetery Lamp Post**: three blocks of cast iron, its lantern lit after dark and out by day.
- Game and client tests. Record: [graveyard.md](docs/features/graveyard.md#grounds).

### Graveyard pack 3, buildings (#139)
- **Family Mausoleum**: an 85-block marble temple front with a portico of Tuscan columns, the family name on its frieze, and a room inside with twelve crypt fronts that each take their own inscription, an altar under a stained-glass window and a sanctuary lamp.
- **Lych Gate** of oak and slate on rubble walls, its tie beam inscribed; **Cemetery Gateway** of granite piers with lanterns and a wrought-iron arch bearing the cemetery's name; **Columbarium** of six inscribed niches.
- **Bronze Mausoleum Door**, opened by hand. Doorways and passages are left open for doors and gates of your own.
- Buildings weather, wax and stir spirits like headstones and break as one, keeping every inscription. Game and client tests. Record: [graveyard.md](docs/features/graveyard.md#buildings).

### Graveyard pack 2, monuments (#139)
- **Six monuments**: a four-block **Grand Obelisk** in granite, a **Draped Urn**, the **Angel at the Tomb** (a grieving angel kneeling at an altar tomb, two blocks wide), a four-block **Trumpeting Angel** on a fluted column, an iron **Mortsafe** caged over a grave, and a bronze **Faithful Hound** watching on its plinth. The angels, urn and hound are sculpted.
- They weather, wax, take epitaphs and stir spirits like the headstones; the mortsafe rusts and the hound grows verdigris.
- Three game tests, client screenshots. Record: [graveyard.md](docs/features/graveyard.md#monuments).

### Graveyard pack 1, headstones (#139)
- **Nine life-sized, finely carved headstones** in four stones: a **gothic** marble headstone, two New England **slates** (willow and urn; winged skull), a child's **lamb** stone, a **broken column**, a three-block **Celtic high cross**, a **rustic scroll** on a granite boulder, a two-block **table tomb** and a **ledger stone**.
- **They weather**: clean, worn, mossy, overgrown (ivy at the last). A brush scrubs a stage off, honeycomb waxes them, an axe takes the wax off, bone meal ages them. The letters fade as the stone weathers.
- **The Stonemason's Chisel** opens an epitaph screen of four lines, checked on the server; each line is cut as large as it fits. A named Name Tag cuts the first line. Epitaphs go with the broken headstone.
- **Neglected graves stir restless spirits more often**; a well-kept churchyard is quiet.
- Two advancements, four game tests, client screenshots. Record: [graveyard.md](docs/features/graveyard.md).

### Added: Landship, batch 49
- **Landship:** a rideable kaiserpunk crawler tank for three. It has a rhomboid hull with animated tracks running round each side, a crest-bearing hub, sponson guns, smokestacks, and a turret whose cannon follows where the driver looks.
- Drive with the movement keys; it climbs steps and bridges narrow trenches. Attack fires the cannon, using cannon shells; hold use for the side guns. It burns diesel or kerosene.
- The shells' burst hurts creatures and never breaks blocks. Three game tests and a screenshot scene. Record: [landship.md](docs/features/landship.md).

### Added: Kaiserworks, batch 48
- 26 imperial building blocks to go with the dieselpunk set: black lacquer, riveted black and gilt-trimmed plate, polished brass, a gilt key-pattern frieze, an imperial crest (our own made-up empire), fluted marble and black iron columns, polished marble, station tiles, see-through wrought-iron lattice, leaded glass and gas lamps.
- Made from plates, calcite, glass and gold trim, never back into metal. Game test and a screenshot scene. Record: [kaiserworks.md](docs/features/kaiserworks.md).

### Added: Diesel Walker, batch 47
- **Diesel Walker:** a rideable dieselpunk mech, about four blocks tall, with an open cockpit, a glowing core, a big fist and a drill arm. Walk with the movement keys (it climbs one-block steps), jump, hold use to drill the block you look at and press attack to punch. It burns diesel or kerosene from buckets. The server drives it from the pilot's keys.
- The drill breaks one block at a time as the pilot would by hand, so protected land and break checks still apply. It is no quarry.
- Four game tests and a screenshot scene. Record: [diesel-walker.md](docs/features/diesel-walker.md).

### Added: Zeppelin, batch 46
- **Zeppelin:** a rideable dieselpunk airship for four. You steer it with the movement keys, jump to climb and sprint to sink. It burns diesel or kerosene from buckets and has a 27-slot cargo hold. It hovers where it is left and sinks gently when out of fuel. The server flies it from the pilot's keys.
- Two game tests and a screenshot scene. Record: [zeppelin.md](docs/features/zeppelin.md).

### Added: Dieselworks, batch 45
- 25 building blocks in the giants' look: rust, riveted, patina, perforated, red iron and copper dome plate (most with slabs and stairs), riveted band, skid iron, ribbed pillars, see-through rust grating for catwalks, steel I-beams, porthole windows and amber cage lamps.
- Made from metal plates (one plate per block), never back into metal. Game test and a screenshot scene. Record: [dieselworks.md](docs/features/dieselworks.md).

### Added: Dieselpunk giants, batch 44
- Sixteen one-block machines are now big, detailed multi-blocks (from 2x2x2 up to the 5x3x3 rocket workshop, with long ones like the 2x2x5 sawmill and the 4x1x2 wire drawer) in a weathered dieselpunk look: rust, patina, perforated covers, ribbed coil stacks, banded domes, copper pipes with hex fittings and amber glow.
- Machines already built in a world stay one block and keep working (`compact` state); placing the item builds the full machine.
- Game tests for forming, the compact copies and breaking. Record: [dieselpunk-giants.md](docs/features/dieselpunk-giants.md).

### Test fix: each giant tree's own fewest logs (#157)
- `BiomeGameTests.bigTreesGrow` holds each giant to the fewest logs it can grow: a giant redwood 85, a giant mahogany 37 (a trunk two wide and at least 22 or 10 tall). #134's single floor of 30 let a stunted redwood pass. Test only; nothing in game changes. Record: [big-trees-and-rainforests.md](docs/features/big-trees-and-rainforests.md).

### Added: Arms motion, batch 43
- **New animations for every arm of batch 42**, seen by everyone and in first person.
  - Each kind has a guard: the side-on stance, two hands on the greatswords, hammers and polearms, the rapier en garde. Each kind also has a combo of attacks with anticipation, the blow, follow-through and a settle. The torso turns and bends, the head counters, and the feet step.
  - In first person, the held arm takes a guard and a stroke for each attack.
  - The spear and lance keep vanilla's arms and add the body.
- Keys joined by Hermite splines so the motion flows through them. Timed on vanilla's own swing, so there is no networking. Client only, with no allocation per frame and the arms only beyond 32 blocks; four client mixins.
- Learned from the combat animation mods and packs the owner sent (Better Combat, Malfu, Player Animation Library, Fresh Animations and Fresh Moves, Mo' Bends, NdRz's weapons); nothing of theirs is used. Record: [arms-motion.md](docs/features/arms-motion.md).

### Added: Liquid fuels, batch 43
- **RP-1 kerosene**, hydrocracked from heavy fuel oil and hydrogen in the catalytic cracker. It is jet fuel too, burning in the gas turbine and the advanced engine.
- **Cryogenic Liquefier**: condenses oxygen into **liquid oxygen**.
- **Kerosene and liquid oxygen tanks** (chemical reactor) make **liquid rocket motors**: three rocket motors from a tank of each and two nozzles, with no solid propellant.
- Advancement, handbook pages, game test. Record: [liquid-fuels.md](docs/features/liquid-fuels.md).

### Added: Booster rails, batch 42
- **Booster rail:** a powered rail with rocket thrusters, loaded with solid propellant (8 boosts each). It kicks a minecart to full speed, from a standstill or uphill, and holds it there for 10 seconds, so long tracks and climbs need far fewer powered rails. Minecarts keep their normal speed limit.
- Handbook page, game test. Record: [booster-rails.md](docs/features/booster-rails.md).

### Added: Rocket launcher, batch 41
- **Rocket launcher** with **high-explosive** and **homing rockets** (rocket workshop). Rockets fly straight, burst on impact and hurt living things only: no block is ever broken. Homing rockets lock on to the hostile mob nearest the crosshair and steer into it.
- Handbook page, two game tests. Record: [rocket-launcher.md](docs/features/rocket-launcher.md).

### Added: Zipline, batch 40
- **Zipline anchors** and the **line-throwing rocket:** fire a steel line between two anchors up to 96 blocks apart with a clear path, then use an anchor to ride the line to the other end. Steeper lines are faster; sneak to let go.
- Handbook page, game test. Record: [zipline.md](docs/features/zipline.md).

### Added: Rocket post, batch 39
- **Rocket pads** send up to nine stacks to another pad up to 4096 blocks away: a **delivery rocket** (used up) and a **flight plan** (kept) naming the target pad. Launch from the pad's screen or with a redstone pulse.
- Deliveries are saved with the world. One whose target area isn't loaded waits and lands when the area loads again; nothing is force-loaded.
- Handbook page, game test. Record: [rocket-post.md](docs/features/rocket-post.md).

### Added: Rocketry, batch 38
- **Rocket Workshop** and propellant chemistry: ammonium perchlorate, iodine (from kelp), silver iodide and solid propellant; rocket casings, nozzles, guidance units and motors.
- **Survey rocket:** surveys ores and oil under 7x7 chunks. **Cloud-seeding** and **clear-sky rockets:** five minutes of rain or clear sky, with a shared cooldown. **Signal flares:** tell nearby players where you are. **Illumination flares:** make hostile mobs glow.
- Advancements, a Rocketry handbook chapter, game test. Record: [rocketry.md](docs/features/rocketry.md).

### Added: Arms, batch 42
- **Nine kinds of arms in bronze and steel:** longsword (parries 60% of a blow from in front), greatsword (two-handed, long reach), rapier (quick thrusts, a light parry), flanged mace and war hammer (break a shield's guard for 3 and 5 seconds; the hammer knocks back), glaive (sweeps at 4.25 blocks), halberd (thrusts through every target in line at 4.5), spear and lance (charge like vanilla's spears; the lance hits harder and unhorses riders).
- Each has its own swing (26.3's whack and stab, from 5 to 12 ticks), reach and in-hand size; holding use parries or charges. Every trait is a vanilla item component, so the server runs and checks them as it does its own weapons, with no per-tick code.
- 64x64 sprites, shared in-hand models, handbook pages, an advancement, three game tests and client screenshots. After studying Epic Knights and Simply Swords; nothing of theirs is used. Record: [arms.md](docs/features/arms.md).

### Added: Control room, batch 37
- **Control Monitor:** six panels form a 3x2 wall screen. Cabled to a logic controller, it shows every channel's reading, a bar, a two-minute graph and ON/OFF.
- **Alarm Klaxon:** a controller switches it like a relay; it lights and sounds.
- **Control Remote:** bind it to a controller and flip a channel by hand from up to 256 blocks away.
- Advancement, handbook page, game test. Record: [control-electronics.md](docs/features/control-electronics.md).

### Added: Tall sides on diagonal walls
- **Diagonal walls rise to meet what is above them.** When the block above covers all of a diagonal wall's sides and arms (another wall, a full block, a slab), they rise to the top of the block as a vanilla wall's sides do, so a diagonal wall two high has no slot between its layers. A tall straight diagonal run has no post, as a tall straight wall has none.
- One property, `tall`, on the diagonal walls: 2,048 states each, 32,768 more in all. Two more server game tests.

### Added: Diagonal walls
- **Walls join diagonally.** All 32 of vanilla's walls join a wall a diagonal step away with a low wall side at 45 degrees, on the same rule as fences: neither may join straight into the corner between them, so a block in the corner keeps them apart.
- **Posts follow vanilla's rule.** A wall that runs straight on along a diagonal has no post, as a straight wall has none, unless something above calls for one (a torch, a block, a wall's post). Ends, corners and junctions keep their posts.
- **Swapped, not enlarged** (the Diagonal Fences mod's approach, in Jugcraft's own code). Vanilla's walls keep their own states. A wall that joins diagonally becomes `jugcraft:diagonal_<wall>` while it does, and turns back when its last diagonal goes. It drops, picks and is named as the vanilla wall, and is in `#minecraft:walls`. Its sides are low or none, never tall. This adds 32,768 block states instead of the 155,520 that diagonal properties on vanilla's walls would add.
- **Mobs** no longer try to step diagonally between two blocks joined diagonally, and a block with diagonals breaks into the particles of its shape without the arms.
- Shapes are worked out once for each set of straight sides, not once per state, and blocks with alike shapes share their diagonal shapes. This applies to fences, panes and bars too.
- Five more server game tests and three wall screenshots. Record: [diagonal-connections.md](docs/features/diagonal-connections.md).

### Added: Control electronics, batch 36
- **Data Cable**, **Sensor**, **Relay** and **Logic Controller**. Sensors read how full a tank, battery, machine or chest is and report on a dye-colour channel. The controller's eight rules ("IF red above 90% THEN blue OFF") switch relays, whose redstone runs machines. It gives a dead band when two rules pair up.
- Advancement, handbook pages, game test. Record: [control-electronics.md](docs/features/control-electronics.md).

### Added: Gas storage, batch 35
- **Gas Cylinder:** carries 8 buckets of one gas. Use it on a tank, pipe or machine to fill it, sneak to empty it; used in the air it tops up a scuba tank or grapple in the other hand. Machines and the fluid filter treat it like a bucket.
- **Ammonia Chiller:** freezes a bucket of water into ice with 5 mB of ammonia, anywhere (even the Nether), and presses four ice into packed ice and four packed ice into blue ice.
- Advancement, handbook pages, game tests. Record: [gas-storage.md](docs/features/gas-storage.md).

### Added: Electroplating, batch 34
- **Electroplating Bath:** plates a tool, weapon or piece of armor with an ingot in sulfuric acid, repairing it fully without experience. **Nickel** makes it half as durable again, **silver** gives a sword or axe Smite III, **gold** makes armor count as gold for piglins. Plating again with the same metal repairs it again.
- Advancement, handbook page, tooltips, game test. Record: [electroplating.md](docs/features/electroplating.md).

### Added: Hydroponics, batch 33
- **Hydroponic Bay:** grows a seed or cutting into a harvest every 30 seconds on power and **nutrient solution** (fertilizer in water, from the chemical reactor), giving the seed back. It needs no soil, sunlight or water source, so it works underground or in any dimension. It grows the vanilla crops, cotton and every agriculture crop.
- Advancement, handbook page, game test. Record: [hydroponics.md](docs/features/hydroponics.md).

### Added: Construction chemistry, batch 32
- **Foam Sprayer:** fills up to 12 open blocks (air, water, lava, plants) where you aim, from 16 blocks away, with construction foam from foam canisters (chemical reactor: plastic and ammonia). Bridge gaps, seal caves, stop floods and lava. Respects spawn protection and the walled town.
- **Cement** (calcite or bone block, clay and sand, smelted) sets foam into **concrete**; concrete also crafts from cement, gravel and water. **Blast-proof concrete** (concrete round a rebar) is as blast-proof as obsidian. Both come as slabs and stairs.
- Two advancements, two handbook pages, a game test. Record: [construction-chemistry.md](docs/features/construction-chemistry.md).

### Added: Diagonal connections (framework)
- **Fences, glass panes and bars join diagonally.** Two of a kind a diagonal step apart join with a rail, pane or bars at 45 degrees, as long as neither joins straight into the corner between them. This covers vanilla's 14 fences, 17 panes and 9 iron and copper bars, and all 14 Jugcraft fences, including the wrought-iron cemetery fence.
- Diagonals update live (placing, breaking, filling the corner), are part of the outline and collision, and turn with rotated or mirrored structures. Switch: `diagonal_connections.enabled`.
- The framework: four block-state properties on every fence and bars block, the tag `#jugcraft:connects_diagonally`, and a generator (`tools/diagonal_connections.py`) that gives any tagged block its 45-degree arm model from its own side model. Seven game tests and a client screenshot test. Record: [diagonal-connections.md](docs/features/diagonal-connections.md).

### Added: Field chemistry, batch 31
- **Chemical grenades** for hand or launcher: **chlorine** (a cloud that hurts what breathes, through armor), **smoke** (mobs lose their target; players inside can't see), **thermite** (a burning pool that never lights blocks) and the **flashbang** (blinds players, staggers mobs, no damage). None breaks a block; other players only where PvP is on, never party members.
- **Gas mask:** keeps out chlorine, smoke and the flash; the filter wears in gas and is repaired with charcoal. A sealed scuba set also works while it has oxygen.
- **Medicines** from the chemical reactor: **first aid kit** (four hearts, 10 s cooldown), **antidote** (clears harmful effects, keeps good ones), **stimulant** (Speed II and Haste II for a minute, with hunger).
- Three advancements, three handbook pages, five game tests. Record: [field-chemistry.md](docs/features/field-chemistry.md).

### Added: Pneumatic grapple, batch 30
- **Pneumatic Grapple:** a harpoon gun on compressed nitrogen (fill it at the air separation unit or a gas holder; 25 mB a shot). The hook flies up to 32 blocks: in a block it reels you in with no fall damage and a hop at the end; in a mob it drags the mob to you (not bosses or golems, never party members). Use it again to let go.
- Advancement, handbook page, four game tests, a screenshot. Record: [pneumatic-grapple.md](docs/features/pneumatic-grapple.md).

### Added: Refinery upgrades, batch 29
- **Hydrotreater:** diesel + hydrogen → **premium diesel** (a quarter more power everywhere diesel burns) + hydrogen sulfide; gasoline + a tenth bioethanol → **premium gasoline** (448,000 JE a bucket in the gas turbine).
- **Sulfur recovery:** the chemical reactor turns 200 mB of hydrogen sulfide into a sulfur dust, so oil feeds the acid recipes.
- **Heat Recovery Unit:** set against a running diesel generator or gas turbine, it makes 30% of their power again from the exhaust, using water and a little lubricant.
- Two advancements, handbook pages, six game tests. Record: [refinery-upgrades.md](docs/features/refinery-upgrades.md).

### Added: Room for the Retro Game Shop in every village (fix for #53)
- **Every new village gets its Retro Game Shop.** Village houses are built inside their street's plot, and some villages were laid out with no plot big enough for the 9 by 8 shop (roughly one village in thirty, going by CI runs and a model of the placer). Such a village is now laid out again, up to 8 layouts; a world seed still makes the same village.
- The village shop test now generates four rounds of the five village types (20 villages) instead of five. In the last round the shop is withheld from each village's first layout (a test-only switch), so every one of those villages must be laid out again and still get exactly one shop.

### The walled town and Jugs
- **A walled medieval town near the start of every new world**, 170–300 blocks away on the flattest dry ground: about 160 blocks across, with a stone curtain wall, 13 round towers and three gatehouses, a church with two spires, a market square with a fountain and six stalls, a bank, a town hall, and 44 half-timbered houses and shops. It is built into chunks as they load, levelled inside the wall and blended into the land outside.
- **Townsfolk:** 32 named, player-shaped townspeople in 18 original skins. Nobody can hurt them. They include shopkeepers, guards who fight monsters in town, strollers, a priest, the mayor, and decorators who change the town's decor for the seasons, Halloween, the Harvest Feast and December.
- **Protection:** players can't break or place blocks in the town, or use items on them; explosions, fire and pistons can't change it; hostile mobs don't spawn inside the wall. Operators in creative mode are exempt, and `town.protection=off` lifts it.
- **Jugs**, the town's credit, kept per player by the server: the General Store buys farm and mine goods, and the Seasonal Stall, Curiosities and the Florist sell decoration and fun items. **Jug Tellers** (ATMs) in the bank send Jugs to other players. There is no profit loop between the shops.
- Commands: `/jugcraft town`, `/jugcraft jugs`, and for operators `town place`, `town theme`, `jugs give|take`. Record: [walled-town.md](docs/features/walled-town.md).

### Added: Parties finished (from #31)
- **Party screen** on the P key: members with online lights, the leader and you marked; LEAD, KICK, DISBAND or LEAVE; invite by name; accept or decline the latest invite. Every button runs the ordinary `/party` command.
- **Clickable [Accept] and [Decline]** on invites in chat.
- **`/party admin list | kick | leader | disband`** for operators (level 2), on any party.
- **Limits in the server config:** `parties.max_size`, `parties.invite_minutes`, `parties.invites_per_minute` (defaults 8, 5 and 10, as before).
- Commands and the shared party API are @Narvisius's from #31, already on main through #84.
### #53 Pixel Hollows and the Retro Trader
- **Pixel Hollows:** a rare cave biome deep under the driest land, lined with **circuitstone** and lit only by scattered, faintly glowing **pixel crystal clusters**, with an original chiptune hum. It holds 1.5× the usual copper and redstone (and tin). New building blocks: circuitstone, polished circuitstone, circuitstone bricks and the **pixel lamp**; clusters drop **pixel shards**. One mixin adds the biome to the Overworld (Fabric API has no Overworld biome API).
- **Retro Trader:** a villager profession at the new two-block-tall **arcade cabinet**. He sells a **Pixel Hollows Map** (use it to mark the nearest cave), circuitstone, lamps and shards, and buys shards back without any profit loop. Trades are 26.1+ data files.
- **Retro Game Shop:** a small storefront in every new village (one per village, all five village types; not zombie villages), with the cabinet and a villager inside.
- **Loot:** the cluster and cabinet use the 26.x loot format (#34 fixed the other tables on main), and new game tests check ore Silk Touch and Fortune and double slabs.
- **Dedicated-server check (CI):** a real client joins the game's own dedicated server, opens a machine and trades with the Retro Trader over the network, leaves and rejoins; and a world is saved and reopened with its trader, machine contents and cabinet intact. The two-client checklist is in [docs/TESTING.md](docs/TESTING.md#dedicated-server-and-two-clients).
- Seventeen game tests, eight client screenshots, a "Dead Pixels" advancement. Records: [pixel-hollows.md](docs/features/pixel-hollows.md), [retro-trader.md](docs/features/retro-trader.md).

### Fix: dyeing sky lanterns (pull request pending)
- **Sky lanterns take dye again.** Minecraft 26.3 dyes leather by a recipe per item, not by the `minecraft:dyeable` tag, so the lantern had no way to be dyed. It now has its own dyeing recipe (a lantern and any dye) and a water cauldron washes the dye out. A new game test checks both.

### Agriculture: fall additions 20, the Día de Muertos ofrenda (pull request pending, stacked on the theremin)
- **Ofrenda:** a three-tier home altar under an embroidered cloth. Set out up to six offerings; with flowers, a light, bread, a sugar skull and a drink it is complete: it glows and, at night, welcomes the restless spirits near, who come to it and show themselves, calm (Remembered).
- **Cempasúchil Marigolds** grow wild with the mums (orange dye, Marigold Petals to strew a path); **Papel Picado** flags for the wall; **Sugar Skulls**; and **Pan de Muerto** to bake.
- Meant as a respectful remembrance, not a fright. New server game tests and a client test with screenshots.

### Agriculture: fall additions 19, the theremin (pull request pending, stacked on wild turkeys)
- **Theremin:** a walnut cabinet on slender legs with a copper pitch antenna and volume loop, a speaker grille and a magic-eye tube, made with the Wire Drawer's copper wire and a note block.
- Switched on (or powered), it sings for the nearest creature within eight blocks: higher the nearer they come to the antenna, over two octaves with a vibrato, its eye glowing green (Good Vibrations).
- Comparators read how near the nearest creature is, playing or not, so it doubles as a proximity sensor. New server game tests and a client test with screenshots.

### Agriculture: fall additions 18, wild turkeys (pull request pending, stacked on the Spirit Board)
- **Wild Turkeys:** flocks of three to five come to forests, taiga, plains and meadows by day. Toms are bronze with a red wattle and a great chestnut tail; hens are brown. A tom with an audience struts: tail fanned, breast puffed, wings down, a gobble.
- Seeds and corn tempt and breed them (Gobble Gobble); hens lay eggs; each drops a raw turkey and feathers, and they flutter down instead of falling.
- **Roast Turkey:** roast a raw turkey in a furnace, smoker or on a campfire and set it on the table: six servings, eaten by hand or carved off with a Carving Knife (Carving the Bird), the drumsticks first, then the breast, then the carcass and a bone.
- New server game tests and a client test with screenshots.

### Agriculture: fall additions 17, the Spirit Board (pull request pending, stacked on pie baking)
- **Spirit Board:** a lettered talking board with a walnut planchette. By candlelight, up to four players rest their fingers on it; the nearest restless spirit answers YES and spells its name and the one thing it wishes for, then GOODBYE (Is Anybody There?). More hands, faster letters; no spirit, NO.
- Give a revealed spirit what it wished for (a pie, a candle, cider, a sweater, candy, an apple, a rose or a pumpkin) and it is laid to rest: experience and Luck (Unfinished Business).
- The planchette slides, eases and swivels over a 64 by 48 lettered face; comparators read YES and NO. New server game tests and a client test with screenshots.

### Agriculture: fall additions 16, pie baking (pull request pending, stacked on knitting)
- **Hearth Oven:** a brick bread oven fed coal, charcoal, coke or logs. It heats to 100 degrees and bakes a raw pie while at 50 or more: baked at 600 points (As Easy as Pie), burnt at 1,200. The pie shows in its mouth, going golden, then black. Comparators read it.
- **Pies:** Pastry Dough (wheat and an egg) with two of a filling and sugar makes a raw Apple, Pumpkin Cream, Cranberry, Sweet Potato or Chestnut pie. Baked, a pie is placed like a cake and eaten or cut with a Carving Knife a slice at a time; a Burnt Pie is barely food.
- 26.3 has no `Level.fuelValues()`, so the oven burns the generators' fuels and a log tag rather than vanilla's furnace fuel list. New server game tests and a client test with screenshots.

### Agriculture: fall additions 15, knitting (pull request pending, stacked on the Hay Golem)
- **Spinning Wheel:** put a skein of wool on the distaff and work the treadle (or pulse it with redstone): four turns spin it into four balls of yarn in its colour, set out where a hopper can take them. It unravels knitwear back into yarn, less a ball.
- **Knitting Needles:** with yarn in the other hand, knit a row at a time into a Knit Beanie, Wool Socks, or one of five sweaters (plain, striped, pumpkin, bat, autumn leaf), coloured the blend of its rows (Knit One, Purl Two). Sneak to change project or unpick.
- **Knitwear** is worn and shows in its colour, keeps out powder snow, and takes dye (and washes clean in a cauldron) as leather does in 26.3. Two pieces by a lit campfire make you cosy (Regeneration I); a beanie, sweater and socks earn Snug as a Bug.
- The checker's recipe audit knows 26.3's dyeing recipes. New server game tests and a client test with screenshots.

### Agriculture: fall additions 14, the Hay Golem (pull request pending, stacked on the Bat House)
- **Hay Golem:** build a T of four hay bales and put a carved pumpkin (or jack o'lantern, or a hand-carved pumpkin with a face) on top, and it comes to life (Man of Straw).
- A walking scarecrow: crows keep off crops within eight blocks of it, twelve with a lit head.
- It tends the ripe crops round its post, replanting them from their drops, and carries the harvest to the chest, barrel or hopper under its post. Lead it with wheat to move its post.
- Wheat heals it; shears take it apart again; fire hurts it double.
- All decided on the server; the client draws its head and carving. The checker compares its numbers with Java. New server game tests (one walks it to a crop with its own AI) and a client test with screenshots.

### Agriculture: fall additions 13, the Bat House (pull request pending, stacked on autumn foraging)
- **Bat House:** a slatted roost to hang on a wall. Bats roost in it by day and pour out at dusk (Night Shift); at dawn the nearest bats come back in, up to four, each leaving a guano on its tray. A house with room gains a bat at dusk now and then. Scoop the guano with an empty hand; comparators read the bats.
- **Bat Guano:** fertilizes the crops in a 3x3 patch (a dose of bone meal each), and four make a phosphate.
- `FertilizerItem` takes its area and doses, so superphosphate and guano share one rule.
- All decided on the server. The checker compares the house's numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 12, autumn foraging (pull request pending, stacked on the candy kitchen)
- **Wild mushrooms:** chanterelles, porcini, puffballs, fly agarics and the glowing jack o'lantern mushroom grow in patches on forest floors (each in its own biomes), on soil. They spread in the shade, up to five of a kind together; bone meal spreads them in any light.
- **Fairy rings:** on a full-moon night a mushroom may sprout a ring of its kind round it. Stand in a ring's centre on a full-moon night for Luck II (once a night) and Away with the Fairies.
- **Foraging Basket:** a wicker bundle for mushrooms, berries, nuts and wild fruit. Mushrooms picked with it in hand go straight in; all five earn Forager.
- **Dishes:** Sautéed Chanterelles, Roasted Porcini, Fried Puffball, and Forager's Stew from the Cooking Pot.
- All decided on the server. The checker compares the spreading, rings, soil and biomes with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 11, the candy kitchen (pull request pending, stacked on #33)
- **Candy Kettle:** a copper sugar pot with a candy thermometer. Fill it before it boils with a water bottle (syrup) or milk (cream), up to four sugar, up to two flavours (chocolate, berry, glow berry, honey, cranberry, spiced or chestnut) and any dyes. Over a fire it climbs through the candy stages, ringing a bell at each; the hottest it reaches decides the candy, so taking it off the heat holds it.
- **Candy Tray:** pour onto it, and break the candy up once set. Syrup makes rock candy (grown for a day), candy corn (poured in up to three coloured layers), salt water taffy (pulled four times while warm, or it sets hard), hard candy and lollipops, and caramel; cream makes fudge, cream caramels and toffee. Too hot burns it.
- Flavoured candy is named for its flavours and gives their short effects when eaten. All candy counts as candy for Candy Bowls and Bags.
- All decided on the server; the client draws the syrup and the thermometer's needle. The checker compares the stages, rates and flavours with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 10, face paint (pull request pending, stacked on ghost hunting)
- **Face Paint Kit:** a tin palette and brush, good for 16 faces, that paints one of six designs: a skull, a jack o'lantern, a black cat, a vampire, a witch or a scarecrow. Use it on a friend to paint them at once, hold use to paint yourself, sneak to turn the dial.
- The paint shows on the face for everyone who can see you and lasts until your head goes under water, or you die. A painted face counts as a costume for trick-or-treating and the costume contest.
- The paint is a Fabric data attachment on the player, set on the server and sent to the clients that see them. The checker compares the kit and designs with Java. New server game tests and a client test with a screenshot of each design.

### Agriculture: fall additions 9, ghost hunting (pull request pending, stacked on the corn maze)
- **Restless spirits** rise from gravestones and grave mounds at night (graves now take random ticks), a few at most near, and drift about their graves, unseen.
- **Spirit Lantern:** held in either hand, it reveals every spirit within 12 blocks to everyone near; so does a Revealing candle's glow. A revealed spirit fades into view, moans, and shies away from anyone close, though never out of its haunt, so it can be cornered. Blows pass through it; it fades at dawn.
- **Ectoplasm:** a glass bottle catches a revealed spirit (earning Ghost Hunter). Ectoplasm is the chandlery's new **Ghostly** scent: a Ghostly candle turns the players in its aura invisible, and the wax pot now hands back a scent's bottle.
- All decided on the server; clients are told only when a spirit shows, and draw it. The checker compares the spirits and the lantern with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 8, the corn maze (pull request pending, stacked on the Harvest Feast Table)
- **Corn Maze Gate:** choose a size (tiny 7 by 7 to large 19 by 19) and use it holding corn kernels: it carves a new maze, one way through, and plants three-tall **maze corn** along its walls, a kernel a stalk, with a **finish post** at the exit. It never replaces a block.
- **Running it:** walk out through the gate to start the clock and reach the finish post to stop it. The server follows every runner and voids a run for flying, climbing over the corn, leaving the maze, taking too long or a shortcut. The best times go on the gate's board, with a prize ribbon the first time a runner places and the advancement A-maze-ing.
- Maze corn gives back its kernel. The checker compares the maze with Java. New server game tests (perfect mazes, planting, timed and voided runs, the corn) and a client test with screenshots.

### Agriculture: fall additions 7, the Harvest Feast Table (pull request pending, stacked on the sky lantern festival)
- **Harvest Feast Table:** a long trestle table built a length at a time; lengths end to end join into one table. Each length holds two dishes of up to eight servings of any food or drink, drawn heaped on their plates; eat a serving with an empty hand.
- **The feast:** different foods on the table plus everyone who has eaten there in the last two minutes. A good meal gives Regeneration, a feast Absorption, a harvest feast Haste and Luck, and a grand feast Health Boost and the advancement Harvest Home, shared with every recent diner close by.
- All decided on the server; clients only draw the dishes. The checker compares the table and its tiers with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 6, the sky lantern festival (pull request pending, stacked on spooky fireworks)
- **Sky Lanterns:** paper lanterns, dyed any colour and named in an anvil to carry a wish. Let one go and it rises glowing, drifting on a wind every lantern shares, so lanterns let go together drift together; it burns out after two minutes or so. A blow puts one out.
- **The lantern festival:** eight lanterns let go within 32 blocks in two minutes, by one player or many, fill the sky: everyone near gets Luck for five minutes and the advancement A Sky Full of Wishes. Once a day in one place.
- **Mooncakes** (red bean, chestnut, pumpkin), baked four at a time in the Cooking Pot. Eaten outdoors on a full-moon night, they give Luck too.
- All decided on the server; clients only draw the lanterns. The checker compares the lanterns, festival and mooncakes with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 5, spooky fireworks (pull request pending, stacked on crows and working scarecrows)
- **Spooky fireworks** that burst into pictures drawn in coloured sparks: a **bat**, a **jack o'lantern**, a **ghost** and a **skull**. Every player sees the picture the right way round, since each client draws it facing them. Crafted from paper, one to three gunpowder and the picture's ingredients; glowstone dust makes them twinkle. They hurt and break nothing, and dispensers fire them.
- **Show Launcher:** nine tubes of up to sixteen rockets each (spooky or vanilla), fired in sequence, in volleys of three or as a finale of all nine at once, fanned out across the sky. Start and stop it by hand or with redstone; hoppers can load it.
- All decided on the server; a burst is one small message to the players who can see it, and each client draws the sparks. The checker compares the rockets, launcher and pictures with Java and checks all 24 rocket recipes. New server game tests and a client test with screenshots.

### Agriculture: fall additions 4, crows and working scarecrows (pull request pending, stacked on the preserves pantry)
- **Crows** come to fields by day in flocks of two or three. They wheel over the field, cawing, then drop onto a ripe crop and peck it three growth stages back (only while the `mob_griefing` rule is on). Crops under a roof, tall crops, gourds and bushes are safe.
- **Scarecrows now work:** crows leave the crops within 4 blocks of one alone, 8 when it wears a pumpkin head, 12 when the head is lit. A crow after a crop takes flight when a scarecrow goes up beside it.
- Crows also fly off from a player who comes close (sneak to get closer), from a blow, and at nightfall. They drop feathers, for arrows and the Featherfall candle scent.
- All decided on the server; spawning follows the `spawn_mobs` rule, and nothing loads a chunk. The checker compares the crows' and scarecrows' numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 3, the preserves pantry (pull request pending, stacked on the cider mill)
- **Preserves in Mason Jars**, cooked in the Cooking Pot: sweet berry jam, apple butter (cooked down in cider), pumpkin butter (with mulling spices), cranberry preserves, glow berry jelly (Night Vision), and pickled beets, pickled peppers (Fire Resistance) and corn relish in new **Cider Vinegar**. A jar holds four servings, eaten one at a time; the last leaves the jar.
- **Unsealed jars spoil** three days after they were cooked. The **Canning Kettle** seals them: fill it with water, set it over a fire, stand up to four fresh jars in it, and twenty seconds at a rolling boil seals them. Sealed jars keep until opened, stack, and wear a gingham cap.
- **Pantry Shelf:** an open cupboard that shows off six jars.
- All decided on the server; the kettle's water and jars and the shelf's jars are drawn by each client. The Cooking Pot now stamps a jar with when it was cooked. The checker compares the pantry with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 2, the cider mill (pull request pending, stacked on the chandlery)
- **Apple trees:** wild in plains and flower-rich places, or grown from **apple seeds**. Their leaves blossom and then hang with ripe apples to pick, about a Minecraft day apart, without the tree being cut down.
- **Cider Press:** load up to eight apples and turn the crank to grind them, one at a time, into pulp. Then turn the screw: four turns press the juice into the trough (a serving an apple) and knock out the pomace. Bottle the juice as **Sweet Cider**.
- **Cider Barrel:** pour sweet cider in; in a day it ferments into **Sparkling Cider**, in three it matures into **Aged Cider**, and a chalk mark on the barrel shows which. Broken, it keeps its cider, still ageing.
- **Mulled Cider** (sparkling cider and mulling spices in the Cooking Pot), **Apple Cider Donuts**, and **Apple Pomace** for pigs, compost and seeds. Each cider gives a short effect: Haste, Jump Boost, Absorption, Regeneration.
- All decided on the server; the press's apples, pulp, screw and juice are drawn by each client. The chestnut tree's fruiting now shares its code with the apple tree. The checker compares the press, barrel and tree with Java, and makes sure no bottle is made from nothing. New server game tests and a client test with screenshots.

### Agriculture: fall additions 1, the chandlery (pull request pending, stacked on batch 14)
- **Wax Melting Pot:** a copper pot set over a fire. Melt honeycomb (beeswax) or rotten flesh (tallow) in it, then stir in dyes (mixed as on leather), up to two scents, glowstone dust (a stronger aura, a faster burn) and redstone (a longer burn).
- **Aura Candles:** dip string in the wax to start a candle, and dip it again once each layer has cooled (dipped while warm, the layer slides off), up to four layers. Each layer makes it taller and brighter, widens its aura (5, 8, 12, 16 blocks) and adds its wax's colour, scents and burn time. Layers of different pots combine their scents; a third scent muddles the candle.
- **Lit, a candle works like a small beacon:** every four seconds it gives everyone in range its scents' effects (Speed, Jump Boost, Night Vision, Slow Falling, Water Breathing, Fire Resistance, Haste or Regeneration), or wards off monsters, makes creatures glow, or speeds up crops. It burns down as it burns and goes out for good; broken, it keeps what is left.
- All decided on the server; each client draws the wax, the candle's colour and height and its tinted flame. The checker compares the waxes, scents and numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 14, costumes (pull request pending, stacked on batch 13)
- **Six outfits**, worn on the head and drawn over the whole body: a **Vampire Cape** that flares as you walk and wraps round you when you sneak, **Mummy Wraps**, a **Skeleton Suit** whose bones glow in the dark, a **Werewolf Mask** with fur, claws and a tail, **Cat Ears and Tail** (the tail sways), and **Bat Wings** that spread and flap when you jump.
- Each counts as a trick-or-treat costume (and for the costume contest); costumed mobs wear them during the event.
- **Costume Trunk:** keeps nine costumes; use it with an empty hand to change into the next one.
- Changing is decided on the server; how outfits move is drawn by each client from boxes generated with their textures. The checker compares the outfits, the trunk and the boxes with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 13, treats (pull request pending, stacked on batch 12)
- **Witch's Brew Punch Bowl:** brew glowing green punch from berries, under rolling dry-ice fog, and ladle it into bottles; a drink makes you glow.
- **Soul Cakes** and the **Barmbrack:** a fruit loaf eaten a slice at a time, one slice hiding a ring; every slice tells a fortune.
- **Pumpkin Spice Latte** (a burst of Speed), **Pumpkin Bread**, **Spiderweb Cupcakes** and **Bat-Wing Cookies**; the cakes, cupcakes and cookies count as treats for Candy Bowls and Candy Bags.
- **Giant Candy:** block-sized props of candy corn, a lollipop, a wrapped sweet and a gumdrop.
- All decided on the server; nothing ticks. The checker compares the bowl's servings, the barmbrack's slices and fortunes, the candy's designs and the drinks' effects with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 12, night events (pull request pending, stacked on batch 11)
- **Trick-or-treaters at your door:** during the Halloween event, village children in costume knock at a Candy Bowl by a lit door; a full bowl earns thank-you gifts, an empty one gets your trees toilet-papered.
- **Toilet Paper Rolls:** throw them over trees and fences; the streamers wash off in the rain.
- **Haunted Hayride:** a four-seat hay wagon on rails; at night its riders hear spooky things from the dark.
- **Halloween Bonfire:** cooks what a campfire cooks, four at a time and twice as fast; toast **Marshmallows** on a stick over it (or a campfire), but not too long.
- All decided on the server; the flames, skewered food and wagon are drawn by each client. The checker compares the numbers, costumes and gifts with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 11, party games (pull request pending, stacked on batch 10)
- **Jump-Scare Trap:** a crate that bursts open and throws up a shrieking ghost on a spring when someone walks up, or a tripwire fires.
- **Costume Contest:** walk the **Costume Runway** in costume while the **Judges' Table** has a round open; everyone else votes by using their favourite, and the most votes win a **Best Costume Ribbon**.
- **Pumpkin Bowling:** roll a **Bowling Pumpkin** down a lane of **Skeleton Pins**; the **Bowling Scoreboard** keeps ten-pin score and stands the pins up again.
- **Candy Cache:** a hollow stump that hides treats like a Candy Bowl, one a night for each finder.
- **Monster Mash Dance Floor:** tiles light up in pulsing Halloween colours from a playing jukebox or redstone; villagers on them dance.
- **Ghost Tag:** ring the **Ghost Bell**; whoever is the ghost glows and tags others by hitting them, harmlessly, with no tag-backs.
- **Fortune Teller's Table:** a tarot card turns, the planchette slides to YES, NO or GOODBYE, and you get one of twenty silly fortunes.
- All decided on the server; the moving parts are drawn by each client. The checker compares the numbers and messages with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 10, lighting and glow (pull request pending, stacked on batch 9)
- **Black Light** and **Glow Paint:** paint skulls, bats, spiders, webs, handprints and eyes on any face; they blaze green-white under a black light nearby.
- **Witch Fire Brazier:** a brazier whose flame turns orange, green, purple or blue with a dye; it burns nothing.
- **Shadow Puppet Lamp:** its turning paper shade throws a bat, a cat and a witch round the walls of the room.
- **Mini Pumpkin Stack** and **Floating Witch Hat:** candle-lit, lit and snuffed like candles; the hats bob and turn in the air.
- The glow, flames, shade, shadows and hats are drawn by each client. The checker compares the numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 9, the yard and porch (pull request pending, stacked on batch 8)
- **Yard Inflatables:** a ghost, a black cat, a pumpkin stack and a spider, two blocks tall, that fill up on a click or redstone, wobble and glow, and sag flat when switched off.
- **Animatronic Porch Witch:** stirs her bubbling pot and follows you with her eyes; walk up and she throws her head back and cackles.
- **Grasping Hands:** rotting hands in a mound of dirt that snatch at the ankles of anything that steps on them (a short, harmless Slowness II); sneak past.
- **Poseable Skeleton:** use it to pose it sitting, waving, lounging or hanging.
- **Bone Wind Chimes:** bones and a little skull under a porch roof that swing and clack, more and louder in rain and storms.
- **Bat and Witch Weathervanes:** turn to point into one wind shared by the whole world, swinging about in storms.
- **Spooky Sign:** painted warnings (BEWARE, KEEP OUT, TURN BACK...) or your own words from a Name Tag or an anvil.
- **Haunted Archway** and **Dead Hollow Tree:** lantern-lit props of several blocks, placed and broken as one.
- The figures, the witch's arm and head, the chimes, the vanes and the sign's words are drawn by each client. The checker compares the numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 8, the mad scientist and monsters (pull request pending, stacked on batch 7)
- **Tesla Coil:** a two-block coil on the electric network (20 JE a tick) that hums, glows and throws harmless violet arcs to other running coils nearby.
- **Lab Table:** a two-block operating table whose sheeted patient sits bolt upright on a redstone signal, and twitches at night.
- **Specimen Jar:** glowing green fluid with an eye, a tentacle, a tiny pumpkin or a brain bobbing in it.
- **Mummy Sarcophagus:** a click or redstone and the lid grinds open, the mummy lurches out with its arms up, then goes back.
- **Raven on a Perch:** watches the nearest player, ruffles and croaks, caws and flaps when used.
- **Black Cat Figure:** swishes its tail, its eyes glow at night, and it arches its back and hisses at anyone who runs past.
- Arcs, the patient, the specimens, the lid and mummy, the raven and the cat's tail and eyes are drawn by each client. The checker compares the numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 7, the haunted house inside (pull request pending, stacked on decorations batch 6)
- **Haunted Chandelier:** eight candles on an iron ring that sways on its chain; at night a draft blows them all out and they relight one by one.
- **Phantom Pipe Organ:** a three-by-two organ, one prop, that plays the opening of Bach's Toccata and Fugue in D minor on a click or redstone, its keys going down by themselves; at night it sometimes plays alone.
- **Suit of Armor:** two blocks of plate on a stand whose helmet slowly turns to watch the nearest player; a red glow in its visor at night.
- **Dust Sheet:** drape it over a chair, stair, slab, chest or bookshelf; it keeps what is under it (a chest's contents too) until you pull it off. At night some sheets seem to breathe.
- **Spirit Mirror:** at night a pale face fades into the glass now and then.
- **Tattered Curtains:** cheesecloth drapes up to eight blocks long that open and shut together and sway in a draft.
- **Creepy Doll:** its head never moves while you watch, but it has turned every time you look back.
- The sway, flames, keys, helmet, sheets, face, curtains and doll's head are drawn by each client. The checker compares the numbers and the organ's tune with Java. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 6, the haunted house and yard (pull request pending, stacked on batch 5)
- **Rocking Chair:** sit in it and it rocks under you; at night, empty, it rocks on its own and creaks.
- **Lurking Eyes:** glowing eyes that peer out of a hedge at night, blink, and vanish when you come within four blocks.
- **Silhouette Window:** a bat, black cat or witch cut-out in orange paper that glows when a lamp lights the far side.
- **Spooky Music Box:** plays an original waltz on note-block sounds while powered by redstone, or once when wound by hand.
- **Giant Fake Spider:** a big hairy spider swaying on a silk thread from a ceiling, branch or cobweb; let its thread out up to four blocks.
- The chair's rocking, the eyes, the windows' glow and the spider's sway are drawn by the client. The checker compares the numbers and the tune with Java. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 5, the harvest party (pull request pending, stacked on batch 4)
- **Bobbing for Apples Tub:** drop apples in, then duck for one with an empty hand: one try in three catches an apple, and the tub splashes a moment between tries.
- **Pumpkin Crate:** shows up to four of your pumpkins, squash, gourds or melons.
- **Hay Bale Seat:** sit on it; it softens falls like a hay block.
- **Autumn Wreath:** chestnut leaves, ornamental corn and mums, for walls and doors; a mum changes its flowers.
- **Leaf Piles:** red, orange and yellow, heaped up to four layers; they soften falls and kick up leaves underfoot.
- The checker compares the numbers, colours and tags with Java and checks every state has a blockstate entry. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 4, the witch's cottage (pull request pending, stacked on batch 3)
- **Bubbling Cauldron:** fill it with a water bucket, then a spider eye, nether wart or glowstone (among others) turns it into a glowing green, purple or orange brew; over a fire it bubbles and steams. An empty bucket pours it out.
- **Apothecary Shelf:** wall shelves of corked jars, tinctures, a little skull and a candle; sneak-use to set them out four ways.
- **Crystal Ball:** a violet-misted orb on a gilt stand; gaze into it and it flares and tells you one of ten fortunes.
- **Grimoire Stand:** an open spellbook on a carved stand; use it to turn through four spreads. Its pages glow faintly and give off motes at night.
- **Witch's Broom:** a twig besom leaning on its bristles.
- The checker compares the numbers, fortunes, spreads and brew tags with Java and checks every state has a model. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 3, the graveyard (pull request pending, stacked on batch 2)
- **Wrought-Iron Cemetery Fence and Gate:** spear-topped iron pickets on finialed posts; a real fence and fence gate (vanilla's fence and gate blocks and tags).
- **Crypt set:** Crypt Stone, Chiseled Crypt Stone (a carved skull), Crypt Stone Pillars (also from the stonecutter) and a stone Crypt Door that opens by hand.
- **Grave Mound:** walk past and a zombie hand claws up out of the earth for three seconds; sneak past and it stays down; redstone holds it up.
- **Mourning Angel:** a two-block marble statue, head bowed into its hands; at night it weeps.
- **Pop-Up Skeleton:** a crate whose skeleton springs out at passers-by (or on a redstone signal).
- The checker compares the scare props' timings with Java, checks every state has a model and that the fence, gate and door are in vanilla's tags. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 2 (pull request pending, stacked on the first five)
- **Luminaria:** a paper bag weighted with sand round a candle, a jack-o'-lantern face cut in its sides. Lit like a candle (light 10); any dye colours it, and it keeps its colour when broken.
- **Floating Candles:** up to four candles hanging in the air, bobbing gently (drawn by the client); 3 light a candle while lit.
- **Skeleton Hand Sconce:** a torch held out from a wall by a bony hand (light 14); snuff it by hand, relight it with flint and steel.
- **Soul-Flame Carvings:** a soul torch lights hand-carved and giant pumpkins with an ice-blue glow (at most light 10) and comes back out as a soul torch. Hand-carved pumpkins gain a `soul` block state (old worlds load with a plain candle).
- **Bat Bunting:** orange and black pennants and paper bats strung between String Light Hooks like the string lights; hooks remember which strand they hold.
- The checker compares the new blocks' numbers and the floating candles' places with Java, checks every state has a model, that the bag has a face cut through its walls and a whole inside (so the face shines with the candlelit far wall), and that the soul-lit icon uses the client's soul colours. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, the first five (pull request pending, stacked on Halloween nights)
- **Jack-o'-Lantern String Lights** strung between **String Light Hooks** (up to 16 blocks; hooks chain); a hook lights from redstone or 1 JE a tick from the electric network, and a strand glows while either end is lit.
- **Candy Bowl:** fill it with treats; each visitor takes one a night (saved on the server), its owner any time.
- **Coffin:** a two-block 27-slot chest whose lid lifts while it is open, and a bed you sneak-use to lie down in and set your spawn (it refuses where beds explode).
- **Haunted Portrait:** four sitters; the pupils follow each player's camera and glow red at night.
- **Fog Machine:** switched on by hand or redstone, it uses 16 JE a tick and rolls ground fog over a 4–16 block radius, drawn by clients with a per-tick cap.
- The checker compares the decorations' numbers and the portraits' eyes with Java and the textures, and checks every block state has a model and every result its message. New server game tests and a client test with screenshots.

### Agriculture: Halloween nights (pull request pending, stacked on the Halloween festivities)
- **Will-o'-wisps** (event nights): glowing wisps drift over swamps and cornfields and flee when you come near (sneak to get closer). A glass bottle catches one in a **Wisp in a Jar** (a lantern, light 13). Advancement **Bottled Light**.
- **Pumpkin Chunkin' Trebuchet** (all year): load a pumpkin, set the release angle (30°–60°), let fly about 50 blocks. The server measures where it lands; a marker shows the distance; a board of the three longest throws gives the Harvest Scale's ribbons once per thrower. Advancement **Pumpkin Chunkin'** at 50 blocks.
- **Candy Bag:** now holds treats like a bundle (candy and cookies only); trick-or-treating fills it, and its tooltip shows tonight's homes.
- **The Harvest Moon:** on the nights of 31 October (new option `halloween.harvest_moon`) during the event, crops and giant pumpkins grow twice as fast and lit carvings throw sparks.
- **The Headless Horseman:** summoned near midnight during the event at a Scarecrow with a lit pumpkin head. A boss with a boss bar, charges and flaming pumpkins that never break blocks; enraged at half health; keeps to his arena; rides off with nothing at dawn or when the event ends. Defeated, he drops the **Horseman's Lantern** and **Horseman's Cloak**. Advancement **Lost His Head**.
- Wisp spawning and sky checks use heightmaps (no light lookups). The checker compares the wisps, trebuchet, Harvest Moon and Horseman numbers with Java, checks every trebuchet state has a model, the ammunition tag matches the throwing factors, the Horseman's loot is player-kill only, every failed summoning has a message, and that no block model face reads outside a see-through texture (26.3 refuses to bake those). New server game tests (including the seasonal rules) and a client test with screenshots.

### Agriculture: the Halloween festivities (pull request pending, stacked on the pumpkin regatta)
- **The carving contest:** put a hand-carved pumpkin on a **Judging Stand** and enter it (its carver only). While the Halloween event runs, every player has one vote per Halloween for someone else's carving; sneak-use a stand for the standings. When the event ends, the top three carvers get the Harvest Scale's ribbons, once (offline winners on their next visit). Votes and prizes are saved with the world.
- **Costumed mobs** (during the event): 15 % of zombies, husks, skeletons, strays and zombie villagers wear a costume hat or carved pumpkin, and drop a sweet when a player kills them.
- **The Halloween Peddler** (during the event): wandering traders arrive in a Witch Hat with four extra Halloween wares for emeralds, from a data-driven trade set.
- **Decorations, all year:** **Rounded**, **Cross** and **Obelisk Gravestones** (stonecutter) engraved with a named Name Tag's name; **Spun Cobweb** (no slowing); **Hanging Ghost**; **Candle Skull** (lights like a candle).
- **Spooky sweets** from the Cooking Pot, eaten even when full: **Glow Gum** (Glowing), **Ghost Taffy** (3 s of invisibility), **Fizz Rocks** (Jump Boost), **Witch's Licorice** (Night Vision).
- The checker compares the contest, costumed mobs, Peddler, gravestones and sweets with Java, checks the Peddler only sells things with another route, and checks `villager_trade` tags. New server game tests (including the seasonal rules: on, off, a restart, no duplicate prizes, earned ribbons kept) and a client test with screenshots.

### Agriculture: the pumpkin regatta and trick-or-treating (pull request pending, stacked on the Halloween harvest)
- **Pumpkin boats:** sneak and use the Carving Knife on top of a giant pumpkin to hollow it out. A full-grown 3×3×3 one becomes a **Pumpkin Barge** (four seats; keeps its weight, carving and torch), a 2×2×2 one a **Pumpkin Racer** (one seat). Lighter boats are faster: racers 1.15–1.30× a boat, barges 0.70–0.95×. Broken, a boat gives back its item with everything it kept. Hollowing gives the guts and (full grown) the giant seeds instead of the pumpkins.
- **The regatta:** a **Regatta Flag** and numbered **Regatta Buoys** on the water make a course. From a pumpkin boat's driver's seat, use the flag: after a countdown, round the buoys in order and come back. The server times the run; the flag keeps a board of the three best times and gives the Harvest Scale's ribbons once per racer. Advancement **Pumpkin Regatta**.
- **Trick-or-treating**, only while the Halloween event runs (by default 20 October to 3 November; operators set `halloween.start`, `halloween.end`, `halloween.timezone` and `halloween.mode` in `config/jugcraft.properties`): use a **Candy Bag** on a villager's door between dusk and midnight, in costume, with a porch light by the door. Each home gives each player one treat a night (candy, caramel, cookies, popcorn balls, caramel apples, rarely a **King-Size Candy Bar**); knocking again gets a harmless prank. Ten homes in a night earn **Full Bag**. Who got what tonight is saved, so a restart or the event ending and starting again gives no second treat; treats and costumes stay after the event.
- **Costumes:** **Witch Hat**, **Ghost Sheet** (draped over the whole wearer, hood to knees, moving with them; you look out through its eye holes) and **Scarecrow Hat**, worn on the head. Hand-carved pumpkins can now be worn like vanilla's carved pumpkin (and fool endermen).
- The checker compares every boat, regatta and trick-or-treat number, tag and table with Java, and allows vanilla tab roots as advancement parents. New server game tests (including the seasonal rules: on, off, a restart, no duplicates, earned treats kept) and a client test with screenshots.

### Agriculture: the Halloween harvest (pull request pending, stacked on Pumpkin Carving)
- **Giant pumpkins:** Giant Pumpkin Seeds plant a vine that sets one fruit and grows it to 2×2×2, then 3×3×3 (faster on moist farmland and when watered; bone meal grows the vine, sets the fruit and feeds it). Full grown it weighs 100–120 kg and gains weight up to 1000 kg until carved. It is one prop: breaking it picks it up whole as a **Giant Pumpkin** item (keeping its size, weight and carving) that places the same pumpkin back; an axe chops it into 9 pumpkins and 1–3 giant seeds. Pistons can't move it.
- **A 48×48 carving face** on each side of a full-grown giant, carved with the Carving Knife through the same screen (smaller cells, bigger brushes, starter faces blown up); a torch lights every block of it.
- **Scooping:** the first cut into a pumpkin also gives 1–2 **Pumpkin Guts** (Pumpkin Soup in the Cooking Pot) and sometimes a giant pumpkin seed.
- **Harvest Scale:** weighs the giant beside it, keeps a board of the three heaviest, gives First, Second and Third Prize Ribbons (trophies) once per pumpkin, and drives a comparator.
- **Pumpkin stencils:** trace a carved side onto a Blank Stencil; hold the stencil in your other hand while carving to press it in.
- **Heirloom pumpkins:** White, Jarrahdale and Cinderella, grown from stems, found wild, carvable into their own hand-carved blocks, and baked into pumpkin pie.
- **Scarecrow** (dye its flannel shirt; it wears any pumpkin on its shoulders for a head, like an armor stand, carving and all, and a lit one lights it), **ornamental corn** (a tall crop; its ears tie into an **Ornamental Corn Bundle** for walls), **Corn Stalks** from tall corn and the **Corn Shock**.
- **Caramel**, **Caramel Apple** (you keep the stick) and **Popcorn Ball**.
- **Bottle Gourd** (found wild), dried into a **Gourd Birdhouse** (stands or hangs) or a **Gourd Canteen** (3 sips of water for giant pumpkins, farmland, fire or a cauldron).
- **Mums** in four colours: flowers for pots, dye and suspicious stew, in wild patches.
- Short grass now drops 24 kinds of seed, still 12.5 % overall. The carving payloads now carry a face size (16 or 48), checked before anything is read. The checker compares the giant pumpkin, scale, canteen, mum and heirloom numbers with Java and checks every giant and scarecrow state has a model. New server game tests and a client test with screenshots, which also carves a giant from a stencil through the screen.

### Agriculture: Pumpkin Carving (pull request pending, stacked on the Festival Crops)
- **Carving Knife** (an iron ingot over a stick): used on the side of a pumpkin, it opens a **16×16 carving screen** for that side, at Minecraft's own pixel size. Cut through, shave the skin or erase; brush sizes 1–3, mirror, four starter faces (Classic, Cat, Ghost, Spooky), undo (Ctrl+Z), reset, a candle preview and an actual-size preview.
- **Hand-Carved Pumpkin:** the first cut lets out the seeds (as shears do) and turns the pumpkin into one that keeps a face on each of its four sides. A knife can't put skin back: carving only goes deeper.
- **Light:** a torch inside lights it, from 4 up to 15 the more is carved out; an empty hand takes the torch back. Broken, it drops with its design and its torch.
- **Roasted Pumpkin Seeds** from the furnace, smoker or campfire.
- **Server checks:** the server checks every carving (the session from using the knife, the knife in hand, reach, build permission, a valid face that only goes deeper). Carvings record their last carver for operators, and `carving.free_draw=false` in `config/jugcraft.properties` allows only the starter faces.
- The checker also verifies the carving numbers against Java, the starter faces and every refusal message, and understands select item models. Ten new game tests, and a client test that carves through the real screen with the mouse and keyboard (after the owner's first play found that clicks on the grid did nothing: 26.3 numbers mouse buttons from 1, and the screen used the old numbers).

### Agriculture: Festival Crops (pull request pending, stacked on the Kitchen Garden)
- **Gourds on stems:** butternut squash, acorn squash and warty gourds grow from stems on farmland and place their gourd beside them, like pumpkins. Stems follow Jugcraft's growth rules, so squash next to beans grows 1.5× as fast. Gourds are blocks for fall displays; 1 gourd → 4 seeds.
- **Turnips**, and the **Turnip Lantern** (a turnip over a torch): a carved turnip that gives light 13, the original jack-o'-lantern.
- **Cranberries**, a bog crop: the bush stands in a water source one block deep over bog soil (block tag `jugcraft:bog_soil`), grows only with open air above, keeps its water when broken, and is picked like sweet berries.
- **The chestnut tree:** a chestnut plants a sapling that grows into a broad tree. Its leaves grow burs that ripen over about a day and are picked with a right-click, without cutting the tree. A chestnut wood set (logs, wood, stripped, planks, stairs, slab, fence, fence gate): any axe strips it, the sawmill saws a log into 6 planks, and it joins vanilla's wood tags, fuel and fire rules.
- **Food:** roasted chestnuts, baked acorn squash, squash pie, candy corn; in the Cooking Pot, butternut squash soup, harvest stew and cranberry sauce.
- **Finding them:** gourds lie on grass, ripe cranberry bushes stand in swamp shallows and chestnut trees grow in forests (new chunks); wild turnips grow in taiga and birch forest. Short grass drops all six new seeds (18 in all, still 12.5 % overall).
- The sickle also picks cranberries and cuts gourds off their stems. Nothing is seasonal: all of it stays after the Halloween and December events are added.
- The checker also verifies gourds, the cranberry and chestnut numbers, bog seeds and every new stage model. Twelve new game tests and a client screenshot test.

### Agriculture: Kitchen Garden (pull request pending, stacked on the Fall Harvest)
- **Trellis:** a square wooden lattice (2 from 5 sticks). It stacks, stands on farmland and blocks movement like a fence.
- **Tomatoes climb trellises**, two blocks tall, and are picked without cutting them down. They grow only into trellis, never into air, and breaking the plant gives back every trellis it grew in.
- **Peppers** grow as a 1-block bush that is picked the same way. **Onion, garlic, cabbage, oats and barley** are 1-block crops, each with a wild plant in fitting biomes.
- **Cooking Pot:** 5 iron ingots and 2 sticks. It cooks while a lit campfire, fire, lava or magma block is directly under it (block tag `jugcraft:heat_sources`). It has six ingredient slots in any order and four result slots, and supports batch cooking, hoppers and comparators. Recipes are data-driven (`jugcraft:pot_cooking`): tomato, onion, vegetable and mushroom barley soups, oat porridge, chili and cabbage rolls.
- **By hand:** garden salad, barley bread, and sauerkraut (cabbage plus Jugcraft salt).
- **Changed:** short grass now drops one Jugcraft seed 12.5 % of the time (vanilla wheat seeds' rate), chosen evenly from all twelve crops. It used to be 2 % per crop, which would have been 24 % with twelve crops.
- The checker also verifies climbing-crop seeds, the Cooking Pot's numbers and recipes (no two with the same ingredients), and loops through every agriculture recipe. Thirteen new game tests and a client screenshot test.

### Agriculture branch: Fall Harvest (pull request pending)
- **New branch:** [docs/branches/AGRICULTURE.md](docs/branches/AGRICULTURE.md) covers the whole Agriculture plan: the crop roster in eight slices, non-industrial farm equipment, connections to the other branches, and rules.
- **Corn grows 3 blocks tall.** You pick it with a right-click, and the stalk stays standing and grows new ears, so cornfields and corn mazes last. From 2 blocks tall it blocks movement like a hedge, which is what makes a maze.
- **Sunflowers** grow 2 blocks tall with a big yellow head, and are picked the same way.
- **Beans, sweet potatoes and flax** are one-block crops. Beans make the crops around them grow 1.5× as fast (legume bonus, block tag `jugcraft:nitrogen_fixing_crops`).
- **Food:** corn, roasted corn, popcorn, sweet potato, baked sweet potato, roasted sunflower seeds and Three Sisters Stew. 2 flax make 1 string.
- **Flint and Bronze Sickles** harvest and replant every ripe crop in 3×3 or 5×5 (vanilla crops too).
- **Seed sources:** wild plants in fitting biomes (new chunks), and short grass (2 % per crop) anywhere.
- Also: composting, pig, chicken and parrot feed, `c:` crop, seed and food tags, and a new `agriculture` feature switch.
- Original textures from `tools/crop_textures.py`. Twelve game tests, plus a client game test with screenshots of a corn maze, the fields and every growth stage.

### Added: biomes branch, Jugcraft regions and nine batches of biomes (stacked on Alpine Spawn)
- **Remaking the Biomes O' Plenty catalog in Jugcraft**, with original art and names: the roster in [docs/branches/BIOMES.md](docs/branches/BIOMES.md) plans 98 biomes in nine batches.
- **Jugcraft regions:** about half the Overworld, in regions about 1 km across, grows the new biomes in place of some vanilla ones; the rest stays vanilla. New settings: `biomes.enabled`, `biomes.region_size`, `biomes.region_share`.
- **Batch 1, the seasonal forests:** Coniferous Forest, Snowy Coniferous Forest, Maple Woods, Seasonal Forest, Aspen Glade, Dead Forest, Tundra, Snowy Forest and Muskeg.
- **New trees and wood:** maples turn red, orange and gold in autumn and stand bare in winter; aspens turn gold; firs stay green; dead wood stands grey. Each has a full wood set (dead wood without a sapling), sawmill and tree farm recipes.
- **Region layouts:** Jugcraft regions come in four layouts (woodland, meadow, wetland, wild), so one vanilla climate can grow different Jugcraft biomes in different regions.
- **Batch 2, fields and meadows** (meadow layout): Field, Flower Meadow, Grassland, Heathland, Lavender Field, Lush Grassland, Prairie, Shrubland and Steppe.
- **New plants:** lavender and tall lavender, goldenrod, heather, orange cosmos (all make dye, the small ones go in flower pots) and clover. **New tree:** the jacaranda, in violet bloom all year, with a full wood set.
- **Batch 3, wetlands** (wetland layout; swamps also elsewhere): Bog (with wild cranberries), Dead Swamp, Lush Swamp, Swamp Woods, Bayou, Floodplain, Ghost Forest, Sludge Mire, Lush River, Fen, Lake District, Quagmire, Marsh and Wetland, with muddy ponds and mud.
- **More plants:** cattails, watergrass (under water) and duckweed (on water). **New tree:** the willow, with hanging leaves and vines, yellow in autumn and bare in winter.
- **Batch 4, warm and dry:** Dryland, Xeric Shrubland, Jacaranda Glade, Lush Desert, Bone Flats, Dry River, Cold Desert, Scrubland, Lush Savanna, Outback, Oasis, Wasteland, Burnt Forest, Mediterranean Forest and Orchard, with their own ground (red sand, coarse dirt, gravel, salt flats). **New trees:** palm and cypress.
- **Batch 5, big trees and rainforests** (mostly the woodland layout): Rainforest, Eucalyptus Forest, Tropics, Subtropics, Dense Forest, Redwood Forest, Temperate Rainforest and Woodland.
- **New trees:** redwood and mahogany (four saplings in a square grow a giant, two blocks wide) and eucalyptus (rainbow-streaked bark), each with a full wood set. **New flowers:** hibiscus and hydrangea.
- **Batch 6, mountains, coasts and volcanoes:** Volcano, Canyon, Highland, Basin, Shield, Karst Pinnacles and Hot Springs (wild layout), Gravel Beach, Dune Beach, Overgrown Beach and Flower Isle (wetland layout), Ice Sheet and Ocean Trench (both). **New plant:** sea oats, which grow on sand.
- **Fixed:** the Cold Desert is no longer buried in snow.
- **Batch 7, wonders and caves:** Cinder Barrens, Elder Vale, Frostlight Garden, Gilded Shrubland, Glimmer Grove, Gloomweald, Hallowed Bog, Highsun Meadow, Mycelial Jungle, Shrine Springs, Snowpetal Grove, Starlit Wood, Toadstool Field, Webwood and Wild Greens, and two cave biomes, the Glowcap Grotto and the Spider Nest. **New plants:** glowcaps and glimmerblooms (both glow), frost irises and snowpetals.
- **Batch 8, the Nether:** Ashfall Wastes, Blighted Sands, Frost Rift, Fungal Thicket, Magma Fields, Marrow Heap, Netherbrush, Quartz Rift and Withered Hollow, rarer than vanilla's five. **New plant:** brambles.
- **Batch 9, the End:** Chorus Reef, Ender Wilds, Outer Flats, Phantom Garden and Rotted Expanse on the outer islands. With it, every biome on the roster is built.

### Added: Alpine Spawn, parts 1 and 2 (stacked on the agriculture pull requests, #53 and #80)
- **New worlds start in Alpine Spawn, at an alpine village.** Alpine Spawn is a large, cool alpine meadow on mountain plateaus; it takes the place of every vanilla meadow and of the cool plateau's forest and taiga. The server moves a new world's spawn to the alpine village nearest the origin, or into the biome when there is no alpine village within 6,400 blocks.
- **Alpine villages are common:** vanilla's taiga villages on a 16-chunk grid (vanilla's is 34), only in this biome.
- **Larches** (part 2): a conifer that changes with the seasons. Its needles are green in spring and summer, turn gold in autumn and fall in winter, leaving bare twigs, then bud green again; each block turns within a week either side, so crowns change gradually. Larches grow among spruces in Alpine Spawn. The tree drops larch saplings, and comes with a full larch wood set (logs, wood, stripped forms, planks, stairs, slab, fence and gate). Larch logs saw into planks in the sawmill, and saplings grow in the tree farm.
- It has seasonal colours and winter snow from the start. Seasonal flowers, bilberries and an alpine winter come in the next parts.
- New settings: `alpine_spawn.enabled` (generation, and the larch's hand recipes) and `alpine_spawn.start` (`on` or `off`: start there).
- Server game tests for its climate entries, tags and villages, and a client test in a real world that it is where the world starts.

### Added: Powered exosuit, batch 28
- Four JE-powered armor pieces (netherite protection, unbreakable): night vision, an energy shield and jetpack, speed, and fall immunity with step assist.
- Two liveries: Vanguard (gunmetal with teal lights) and Ronin (crimson and silver, conical hat, red eyes), with 3D shoulder plates, skirts and hat. Smithing liveries switch between them and keep the charge.
- The crimson Ronin katana. Inspired by Mekanism's MekaSuit (MIT); looks follow the owner's reference images; all art original.

### Added: Gear, weapons and plastic blocks, batch 27
- **Scuba mask and tank:** breathe under water on oxygen (8,000 mB, 400 s); fill the tank from a gas holder or machine.
- **Free runners:** boots with no fall damage and a one-block step.
- **Power katana and power bow:** JE-powered weapons charged at the charging station; the bow fires energy arrows without ammo.
- **Plastic blocks** in all sixteen dye colours, from plastic sheets.
- High-detail art: an animated 32x32 energy katana, and double-resolution scuba gear and free runners.
- Four advancements and handbook pages. Inspired by Mekanism and Mekanism: Additions (MIT); all code and art original.

### Added: Four-ingot ore and bioethanol, batch 26
- **Acid leaching:** an ore and 250 mB of sulfuric acid in the chemical reactor give 4 washed ores (the best ore route).
- **Bioethanol:** 8 crops and a bucket of water ferment into 250 mB in the chemical reactor; it burns in the gas turbine and the advanced engine.
- Inspired by Mekanism (MIT); no new machines.

### Added: Tools, armor and paxels, batch 25
- Bronze and steel swords, pickaxes, axes, shovels, hoes and armor (bronze iron-tier, steel between iron and diamond).
- Paxels (pickaxe, axe and shovel in one) for every tier from wood to netherite, bronze and steel.
- Bronze armor is steampunk (goggles, pressure gauge, boiler); steel armor is kaiserpunk (Pickelhaube, field-grey tunic, jackboots).
- Two advancements and handbook pages. Inspired by Mekanism: Tools (MIT); all code and art original.

### Added: Fewer chemistry machines, batch 24
- Five single-job machines folded into ones that already exist (65 machines down to 60):
  - the distillation tower vacuum-distils heavy fuel oil (was the vacuum distillation unit);
  - the catalytic cracker reforms naphtha, using a catalyst (was the catalytic reformer);
  - the chemical reactor mixes brine and fracking fluid (was the chemical mixer);
  - the **Settling Plant** (the flowback treatment unit, renamed) separates oil sand and bitumen (was the oil sand extractor);
  - the arc furnace pulls silicon boules, with argon (was the crystal grower).
- New uses: the settling plant presses mud into clay; the electrolytic cell splits water into hydrogen and oxygen.

### #80 Seasons (colours, events and winter snow)
- **Grass and leaves change colour with the server's date** in every biome that has four seasons: plains, meadows, forests (dark, dappled and cherry groves included), taigas, windswept hills and swamps.
  - Winter is dull and dormant, spring is fresh green and summer is vanilla.
  - Autumn turns oak leaves gold, orange and red in patches, then russet.
  - Colours change a little each day.
- **Winter snow (opt-in, `seasons.snow=on`):**
  - From December to February, rain falls as snow in those biomes, and up to `seasons.snow_depth` layers settle.
  - The snow melts in spring.
  - It never freezes water, lies on farmland or touches snow you placed.
- **Events on one clock:**
  - the **Harvest Feast** (`harvest_feast`: the US Thanksgiving weekend by default, Canada's, or off);
  - **December** (`december`: 1 December to 6 January by default).
  - Both are announced in chat.
- **`/jugcraft season`** shows the season, day and events. Operators can set a season, preview a date or switch snow on or off until the server stops.
- **Server settings** in `config/jugcraft.properties`: `seasons.mode`, `seasons.hemisphere`, `seasons.timezone`, plus the snow and event settings above.
- The server decides everything; clients never use their own clock. Colours and events save nothing; seasonal snow melts away.
- Server and client game tests, with a screenshot per season and one of winter snow.

### Blueprints and test blocks (same draft PR)
- **Blueprint Table:**
  - LIBRARY of the mod's structures and imported blueprints, with a front view and materials; printing is free.
  - IMPORT takes pasted `.jugbp.json` text, checks it, saves it with the world and shares it with everyone.
- **Placing blueprints:** a hologram preview up to 32 blocks away, a Survey Stake screen (progress, materials, Personal/Party, rotate, remove), and drone building layer by layer.
- **Drone Tower tier 1** now also builds the Energy Exchange and Storage Exchange, adding seven new building blocks and two ports that feed the depot.
- **Seating:** Operator Chairs can be sat on.
- **Creative-only test blocks:** the Creative Energy Cell and the Creative Supply Crate.

### Drone Depot (draft PR, stacked on Parties)
- **New blocks:** Drone Depot Terminal, Control Screen Panel, Hologram Table, Cargo Packager, and the tower-placed Landing Platform, Landing Pad and Supply Pickup Plate (no recipes: the Drone Tower builds the depot).
- **Depot:** part of the Drone Tower. Tier 1 places the base floor, eight pads (5x5 plates forming a pad with a charger port), the supply pickup (3x3 plates with a lift hatch) and the terminal building. A terminal without a tower flies no drones.
- **Drones:** nine tiers, all craftable, each with its own 3D look in the tower's graphite-and-dull-red theme (tier 9 is the Superconducting Ring Lifter). Tiers 5–9 use new parts from real materials: neodymium motors, tilt-rotor nacelles, composite rotors, hydrogen lift cells (chemical reactor), ion emitters, superconducting tape, Stirling cryocoolers and superconducting lift fans.
- **Drone Tower ([docs](docs/features/drone-tower.md)):** a Tower Core on a 15×15 chiseled stone plinth builds the Command Post (tier 1, with a furnished command room). Tiers 2–9 are flown in tile by tile by the depot's own drones, using four kinds of tower module. Tower tier N unlocks drone tier N; the full tower holds 100 drones, each in its own hangar, with seven pickups. Adds 19 building materials (nine with stairs and slabs), five furniture blocks and a tower status screen. Drones dock round the pads, fly their routes, and winch crates up from the pickup (the hatch opens and a lift raises the crate).
- **Command room screens:** a hologram table (nine sections form one table projecting a live depot map) and a screen wall (six panels form one live display), both in the tower's command room. The terminal opens a sci-fi screen (OVERVIEW, FLEET, JOBS, POWER, and a PERSONAL/PARTY button) whose text is fitted to the panel.
- **Pooled power:** standby and working draw, cached. Low power slows flights and never drops cargo.
- **Flights:** timed flight records (not mobs) with terrain-following routes, at most 5 launches per tick, and reservations. Clients get a small snapshot when something changes and move the drones themselves.
- **`BuildJobs`:** the build-job interface for blueprints and later builders, plus a development-only `/dronetest` command.
- **Tests:** tower server and client game tests; server game tests (layout rules, docks, flight paths, and in-world depots that build blocks, re-form pads, respect Party mode and link screens) and a client game test with screenshots, plus checker rules keeping drone numbers in sync.

### Parties (draft PR; proposal #21)
- **`/party` commands:** create, invite, accept, decline, leave, kick, leader and disband.
  - Invites expire after 5 minutes, and each player can send 10 a minute.
  - Parties hold up to 8 members.
- **Shared API (`JugcraftParties`):**
  - `sameParty`, `isLeader`, `partyMembers`, change listeners.
  - `mayServe` with `UseMode` (Personal/Party), which every automated system will use.
- **Saving:** parties are saved in the world folder (`jugcraft/parties.txt`).
- **Feature switch:** `parties.enabled`.
- **Tests:** seven new game tests, plus a checker rule that every party result has a chat message.

### #81 Engineer's Handbook reorganised, batch 23
- The book fits the window; the chapter list is a scrollable contents list where the open chapter shows its pages, and long pages scroll (mouse wheel or arrow keys).
- New **Progression** chapter: the road through the mod in nine stages, each a plan and a numbered chain of the items to make in order.

### #78 Machine screens redesigned, batch 22
- Every machine screen has a themed look: dieselpunk amber, electric green or lab teal, after the machine's model.
- A control terminal says what the machine is for, what it is doing, its progress, power and power rate, and holds the side controls.

### #77 Solar tracker and heliostats, batch 21
- **Solar tracker:** a panel that tilts after the sun, 20 JE/t in one block.
- **Heliostats** and a **solar receiver**: 12 JE/t per heliostat under open sky in the field below the receiver (up to 48), boiling water.

### #76 Joined tanks, glass tanks and gauges, batch 20
- Tinplate and glass tanks touching each other join into one tank (up to 64), filling from the bottom.
- **Glass tank** shows its fluid; **tank gauge** shows any tank's or machine's level in eighths.

### #75 Turbocharger and flywheel, batch 19
- **Turbocharger** in the advanced engine's new slot, with coolant water in its new second tank: up to 1,536 KE/t and 10% more KE from each mB of fuel.
- **Flywheel:** stores up to 2,000,000 KE of rotation and drives its front shaft from it; friction runs it down slowly.

### #74 Explosive weapons, batch 18
- **Guncotton** (2 cotton + 250 mB nitric acid, chemical reactor).
- **Grenades**, thrown by hand, and the **grenade launcher**, which fires them further. The blast hurts living things only: up to 16 damage, walls shield, and no block, armor stand, frame or dropped item is ever touched.
- New switch `explosives.enabled`. An advancement, a handbook page and a game test.

### #73 Flow batteries, batch 17
- **Vanadium electrolyte:** two asphalt binder and a bucket of sulfuric acid in the chemical reactor.
- **Flow battery** (3×3×2): 1,000 JE per mB of electrolyte in it, up to 64,000,000 JE with 64 buckets; 8,192 JE/t in and out. Keeps its electrolyte when broken.
- An advancement, a handbook page and a game test.

### #72 Glass chemistry, batch 16
- **Tincal**, natural borax, in desert and badlands sand; **borax**.
- **Borosilicate glass** (2 sand + borax, alloy smelter) drawn into **optical fibre**, which can replace gold in processors.
- **Ferroboron** (iron + borax): with a rare earth oxide it makes **two** neodymium magnets.
- Multi-input recipes now try the one with the most ingredients first.

### #70 Chlorine and lye, batch 15
- **PVC:** refinery gas + chlorine → vinyl chloride (synthesis converter) → PVC resin (polymerization reactor) → two plastic sheets each (metal press).
- **Soap** from lye and rotten flesh; a bar washes off every status effect.
- Two advancements, a handbook page and game tests.

### #69 Rubber and polymers, batch 14
- **Butadiene** from naphtha (chemical reactor) and **synthetic rubber** from butadiene (polymerization reactor).
- **Gaskets** (rubber + steel plate); rubber belts; gasketed steel pipe, four for two plates.
- An advancement, a handbook page and a game test.

### #68 Oxygen-blown steel and argon, batch 13
- The air separation unit also makes **argon**.
- **Boost gases:** oxygen piped into the steel foundry, or argon into the crystal grower, doubles its speed.

### #65 Nitrogen chemistry, batch 12: air separation, ammonia and nitric acid
- **Air separation unit** (2×2, six tall): splits air into nitrogen and oxygen, four to one, needing only power.
- **Synthesis converter** (3×4×2): Haber–Bosch ammonia (hydrogen + nitrogen) and Ostwald nitric acid (ammonia + oxygen + water).
- New gases nitrogen, oxygen and ammonia; nitric acid with a bucket.
- Ammonia + phosphate → 6 fertilizer; nitric acid etches microchips with half the acid.
- Three advancements, a handbook section and game tests.

### #62 Surface deposits, batch 11
- **Coal, Iron, Copper and Tin Deposits:** flat patches in the top layer of stony hills (windswept hills, stony peaks, stony shores). Picks only break them, for nothing; each block holds 1,000 units.
- **Deposit drill** (3×3, two tall): takes one coal or raw ore of each kind every 15 seconds from the deposits under it and one block round it, and pushes them into a chest, pipe, conveyor or machine beside it. Empty deposit blocks turn to stone.
- `deposits.enabled` switch, an advancement, a handbook page and game tests.

### #60 Advanced power, batch 10: big solar, a four-cylinder engine and tanks that keep their fluid
- **Advanced solar panel:** a white pedestal carrying a 3×3 array of cells, 64 JE/t in full sun (eight solar panels).
- **Advanced combustion engine** (2 long): gasoline or diesel → up to 1,024 KE/t on a shaft; through a magnet dynamo, the best JE per mB of either fuel.
- **Tanks** have a new look (white with checker bands) and **keep their fluid when broken**: the item carries the fluid and amount, shown in its tooltip.
- **Fix:** loot tables now use the Minecraft 26.x format; the old keys were silently ignored, so ore drop counts, the charging station's upper half and slab doubles were wrong. A data check and a game test guard it.
- Two advancements, handbook pages and game tests.

### #58 Farming, batch 9: harvesters, sprinklers and cotton
- **Crop harvester** (1×2): harvests and replants the ripe crops in the 9×9 field in front of it.
- **Sprinkler:** pipe-fed water gives nearby crops extra growth ticks; it also spreads fertilizer from its hopper.
- **Cotton:** a new crop; seeds from sifting coarse dirt; cotton spins into string.
- Three advancements, a Farming handbook chapter and a game test for each.

### #57 Fluid logistics, batch 8: gas holders, valves and filters
- **Gas holder** (3×3×3 Horton sphere): 1,024 buckets of one gas, and only gases.
- **Fluid valve:** a steel pipe segment that a redstone signal closes, splitting the line in two.
- **Fluid filter:** a steel pipe segment whose neighbouring tanks and machines only receive its chosen fluid; set it with a bucket or from a tank beside it (for gases).
- Three advancements, handbook pages and a game test for each.

### #56 Electronics, batch 7: silicon, chips and the cyan look
- **The cyan look** for the electronics tier, following the owner's references: near-black casings with cyan seams, cyan glass that glows while working, cyan screens, violet conduits, and a beige retro computer.
- **Crystal grower** (1×2): 4 silicon + a phosphate dopant → a silicon boule; the sawmill cuts it into 8 **silicon wafers**.
- **Lithography station** (3×2×2, a cleanroom with a monitor bank): wafer + copper wire + sulfuric acid → 4 **microchips**.
- **Processors:** the third circuit tier (circuit assembler).
- **Network terminal:** a beige retro computer that reads out the power network it is cabled to.
- Four advancements, an Electronics handbook chapter and game tests for each.

### #54 Chemistry, batch 6: advanced materials
- **Titanium:** a new mined metal (deep ore, iron pickaxe, `titanium.enabled`). No furnace smelts it.
- **The Kroll process:** raw titanium + coke + 250 mB chlorine → titanium sponge (chemical reactor); the arc furnace melts it into ingots. Chlorine's first use.
- **Leaching:** lepidolite or monazite + sulfuric acid → 2 lithium carbonate or 2 rare earth oxide.
- **Lithium battery bank** (3×2×1, electric look): 32,000,000 JE, 16,384 JE/t out of its front; built from lithium cells and titanium.
- **Neodymium magnets** (alloy smelter), and the **magnet dynamo** and **magnet motor**: 95% each way, four times the copper-wound rates, cyan-banded.
- Four advancements, handbook pages and game tests for each.

### #52 Chemistry, batch 5: electrochemistry and acids
- **Brine** (chemical mixer: salt + water) and the **electrolytic cell** (3×3×2): brine → **chlorine**, **hydrogen** (gases) and **lye**, each out of its own row.
- **Chemical reactor** (2×2×2): sulfur dust + water → **sulfuric acid**.
- **Alumina and real aluminum:** bauxite + lye → 2 alumina; 2 alumina + coke → 2 aluminum ingots in the cell. Two ingots per bauxite, twice the arc furnace.
- **Fertilizer:** phosphate + sulfuric acid; ripens every crop in a 5×5 area.
- **Fuel cell** (one block, electric look): hydrogen → 128 JE/t.
- Five advancements, a Chemistry chapter in the handbook, and a metal audit for fluid recipes.

### #51 Oil line, batch 4: industry, and the electric look
- **Polymerization reactor** (2×2×3): refinery gas → plastic pellets; the metal press makes **plastic sheets**.
- **Asphalt**, **asphalt slab** and **asphalt road line**: walking on them is 1.3× as fast.
- **Diesel engine** (2×2×3): up to 512 KE/t into a shaft line from diesel or heavy fuel oil, burning only for what is used.
- Nine oil **advancements**, a **Fuel Values** handbook page, JEI categories for every **fluid machine**, and the oil audit in [docs/BALANCE.md](docs/BALANCE.md#oil).
- **Electric look** (owner request): cables are 6 px graphite with a glowing green core (emissive), with copper, silver or aluminum collars; the battery box, capacitor bank, charging station, solar panel, electric pump, electric motor and dynamo are restyled in graphite and green light.
- Fix: a dynamo took up to twice its 128 KE/t on a strong shaft line; it is now capped per tick.
- Screenshots: a power-gear scene, and the multi-block showroom spaced to fit the oil machines.

### #50 Oil line, batch 3: fracking and diesel power
- New fluids: **fracking fluid** and **flowback water** (with buckets).
- **Chemical mixer** (2×2×2): water + sand + dried kelp → fracking fluid.
- **Fracking rig** (3×3×5): over shale oil, pumps fracking fluid down and brings up crude oil, refinery gas and flowback water.
- **Flowback treatment unit** (3×1×2): flowback water → clean water (a quarter lost) + salt.
- **Diesel generator** (3×2×2): 256 JE/t from diesel (256 JE/mB) or heavy fuel oil (128 JE/mB).
- **Gas turbine** (4×2×2): 512 JE/t from gasoline (384 JE/mB) or refinery gas (192 JE/mB), with lubricant upkeep.
- Game tests for each.

### #49 Oil line, batch 2: refining
- **Steel fluid pipes** (1,000 mB/t) and the **heavy pump** (1,000 mB/t); a pipe line now carries as much as its slowest pipe.
- New fluids: **naphtha, diesel, heavy fuel oil, lubricant, gasoline** (with buckets) and **refinery gas** (a gas: tanks and pipes only).
- **Distillation tower** (2×2×7): crude oil → gas, naphtha, diesel and heavy fuel oil, each drawn off at its own height.
- **Catalytic cracker** (2×2×4): heavy fuel oil + steam + catalyst → diesel, naphtha and gas. **Cracking catalyst** from bauxite, sand and nickel.
- **Vacuum distillation unit** (2×2×3): heavy fuel oil → lubricant + asphalt binder.
- **Catalytic reformer** (3×2×2): naphtha → gasoline + gas.
- Game tests for each.

### #47 Oil line, batch 1: oil in the world
The first five commits of the dieselpunk Chemistry branch ([plan](docs/branches/CHEMISTRY.md#petrochemistry-the-dieselpunk-oil-line)).
- **Crude oil:** a real fluid with a bucket; slow, thick, never makes new sources; works in every tank and pipe.
- **Fluid processing machines:** machines with input and output tanks, data-driven fluid recipes and tank gauges on their screens.
- **Oil reservoirs:** hidden, finite oil under Overworld chunks (pumpable or shale), fixed by the seed; the prospector reports them.
- **Pumpjack:** a 1×3×3 dieselpunk nodding donkey that pumps crude oil from the reservoir under it.
- **Oil sand extractor:** a 2×2×2 hot-water plant that washes crude oil out of oil sand and bitumen.
- Game tests for each; a new Oil chapter in the handbook.

### #46 Balance review
- New [docs/BALANCE.md](docs/BALANCE.md): every generator, conversion, store and cost in one place, with the loops that were checked.
- Charcoal burns three quarters as long as coal in Jugcraft's generators and engines. This makes tree-farm wood power slightly weaker (net about +545 JE/t per tree farm, down from +737), as the owner chose. Vanilla furnaces are unchanged.

### #44 Conveyor slopes
- **Conveyor Slope:** carries items one block up or down; use it with an empty hand to switch. Slopes join conveyor runs, and items climb and descend them visibly.
- A game test and a client screenshot.

### #43 Advancements
- A **Jugcraft** advancement tab: 22 steps from the first tin to the rocket pack, earned by having each item. Goals for steel, the steel foundry and the large steam engine; a challenge for the rocket pack.
- The handbook's Getting Started chapter lists the steps on its Milestones pages.
- A game test checks the tree loads; the data checker checks every step's items, title and parent.

### #42 Tool upgrades and a 3D rocket pack
- **Upgrade modules** for the powered tools, fitted at the charging station: Overclock, Range (5×5 drilling), Capacity, Silk Touch and Fortune.
- The worn rocket pack is now a 3D model on the wearer's back.
- The mining drill sits higher in first person.
- Four game tests.

### #41 Dieselpunk steel machines
- The steel foundry, capacitor bank, steel tank, ore drill and high-pressure extractor now look dieselpunk: gunmetal and olive paint, hazard stripes, chrome, phosphor gauges and caged lamps, an exhaust stack on the foundry and a diesel motor on the drill. Same footprints and ports; looks only.

### #40 Powered tools (the first dieselpunk gear)
- **Mining Drill:** a JE pickaxe and shovel, faster than netherite; modes for one block, 3×3 or a whole ore vein.
- **Chainsaw:** a JE axe that also cuts leaves and fells whole trees.
- **Rocket Pack:** worn on the chest; hold jump in the air to fly.
- **Charging Station:** a two-block-tall station that charges the tool on its cradle from cables.
- The tools hold JE instead of wearing out; empty, they mine like a bare hand.
- New dieselpunk textures and detailed 3D item models; [docs/ART_DIRECTION.md](docs/ART_DIRECTION.md) records the rule that higher tiers look dieselpunk.
- Five game tests and three client screenshots.

### #38 Conveyors
- **Conveyor:** carries items (drawn riding on it) the way it faces, 2.5 blocks a second, while rotation drives it: 1 KE per conveyor per tick for a whole joined run. Pipes, hoppers, machines and dropped items load it; it unloads into the conveyor or inventory ahead, or onto the ground. It carries players and mobs too.
- **Conveyor Splitter:** sends items left, straight on and right in turn.
- Four game tests and a client screenshot.

### #37 Spinning shafts and closer screenshots
- Shafts, belt pulleys, the hand crank, the electric motor's shaft and the steam engine's flywheel now really spin (a block entity renderer) instead of scrolling a texture. Shafts placed with earlier builds need re-placing to spin.
- The client test photographs a belt-and-motor line and the multi-blocks from closer, in three views.

### #36 Belts and the Electric Motor
- **Belt Pulley** and **Leather Belt:** link two pulleys up to 16 blocks apart to carry rotation; the belt is drawn between them.
- **Electric Motor:** JE → KE at 75%, up to 96 KE/t.
- Three game tests.

### #35 Bigger machines, a spinning wind turbine, the Large Steam Engine and JEI
- Machines can now fill up to 64 blocks. Resized:
  - **Alloy Smelter:** 3×2×6, with a big copper crucible tank pouring into one funnel over the furnace.
  - **Geothermal Generator:** 2×2×2.
  - **Steel Foundry:** 2×2×5.
  - **Coke Oven:** 2×2×2, with its chimney in a block on top.
  - **Wind Turbine:** 9 tall, with a 7-block rotor that spins (block entity renderer); 12–72 JE/t.
- **Large Steam Engine** (2×2×2): 256 KE/t, four times the small one.
- Machine screens: amber energy readout without a shadow; vanilla tooltips on gauges.
- **JEI:** a recipe page per machine (optional; EMI has no 26.3 build yet).
- Multi-blocks placed with earlier builds need re-placing.

### #32 Polish
- Machines with a fire, and the steam engine, smoke and crackle while running.
- Hovering the energy bar or a tank gauge shows exact JE or mB.
- The eject button reads "Eject" (green on, gray off) instead of a cut-off "Eject: off".
- The CI screenshots no longer show the chat log.

### #30 Auto-Crafter
- **Auto-Crafter:** crafts any crafting-table recipe laid out in its 3×3 grid, one every 2 seconds.
  - Each grid slot keeps one item as the pattern, and pipes and hoppers only top up matching slots.
  - Remainders such as empty bottles get their own slot.
- A powered processor with upgrades, sides, eject, redstone and kinetic power; a new grid layout on its screen.
- Three game tests and a client screenshot of its screen.

### #29 Kinetic power
- A mechanical power layer in **KE** per tick. **Iron Shafts** carry it along their axis and **Brass Gearboxes** out of all six sides; both animate while turning.
- Sources:
  - **Hand Crank:** 16 KE/t while cranked.
  - **Steam Engine:** 64 KE/t from fuel and water, burning only while something takes the power.
- Every powered machine runs straight off a shaft (1 KE = 1 JE). The **Dynamo** bridges KE into JE cables at 75%.
- Three game tests, a client screenshot of a running line, and handbook pages under Power.

### #28 Renewable resources
- **Water Wheel** (2 tall): up to 24 JE/t from flowing or falling water beside its wheel, with no fuel.
- **Cobblestone Generator:** one cobblestone a second from water and lava touching it; neither is used up.
- **Tree Farm:** grows a sapling into six logs and gives the sapling back, sometimes with an extra (apple, cocoa beans, …). Recipes are data for all nine vanilla trees.
- Steampunk models (timber water wheel with a coil dynamo, cistern-and-crucible generator, open brass growth cabinet with a grow lamp) and classic models.
- The handbook gains a "Renewables" chapter; its chapter buttons are packed tighter to fit 11 chapters.
- Three game tests.

### #25 Mining and prospecting
- **Geo-Resonance Prospector:** a hand tool that surveys the 3×3 chunks around you. It opens a steampunk-digital screen: a brass instrument with an amber CRT, valve-tube signal bars, a sweeping scan line and a resonance needle gauge.
  - Readings are deliberately vague: 1–5 bars and shallow, middle or deep for each ore family, never a chunk or block.
- **Ore Drill** (2-tall derrick): mines every ore in a 9×9 column below it, down to the bottom of the world, and refills the holes with rock. It gives whole ore blocks, so ore processing still decides the yield. It has upgrades, side configuration, eject and redstone control.
- Two game tests, a prospector screenshot in the client test, and handbook pages under Materials.

### #24 Storage
- **Capacitor Bank** (2×2): 4,000,000 JE. It charges from any side and gives power out of its front sockets at 4,096 JE/t.
- **Steel Tank** (2×2 squat riveted tank): 128 buckets.
- **Item Crate:** 32 stacks of one item, with right-click in and out and support for pipes, hoppers and comparators.
- Steampunk models (Leyden-jar bank, domed tank, banded crate) and classic models.
- Three game tests, a handbook "Storage" chapter, and a feature record.

### #22 Transmitter tiers
- **Silver Cable** (1,024 JE/t) and **Aluminum Cable** (4,096 JE/t). All cable tiers join one network, which runs at its slowest cable.
- **High-Pressure Extractor** (steel): 32 items every 4 ticks, four times the brass extractor.
- There is no faster fluid pipe: pumps (100 mB/t) are the limit, not pipes.
- Two game tests; handbook pages.

### #20 Engineer's Handbook and in-game screenshots
- **Engineer's Handbook** (book + copper ingot): an in-game guide with 9 chapters and 36 pages. Each page gives what a block does, its power use, its crafting grid and example recipes, and you can hover over items.
- The content is generated from the mod's own tables, so it can't go out of date.
- **Client game tests:** CI starts a real game client, builds a showroom of every machine, opens a machine screen and the handbook, and saves screenshots as a build artifact.

### #19 Machine control
- **Upgrades:** every powered processing machine gets two upgrade slots.
  - **Speed Upgrade:** 4 cards make it 3× as fast for twice the energy per item.
  - **Efficiency Upgrade:** 4 cards bring it to 41% of the energy.
  - Both are made from steel.
- **Redstone mode button:** ignored, run with a signal, or run without one.
- **Comparators** read stored energy (generators, battery box) or how full a machine is, from any block of a multi-block.
- Energy readouts are shortened (for example "12.5k / 20k JE") to fit the new slots.
- Four new game tests.

### #18 Steel tier
- **Coke Oven** (2 tall) bakes coal into **Coal Coke**. Coke is a 3,200-tick generator fuel and the carbon for steel.
- **Steel Foundry** (3 tall) turns 1 iron ingot + 1 coke into 1 **steel ingot**.
- Both are unpowered brick multi-blocks with steampunk and classic models.
- New steel items: ingot, nugget, block, plate and gear.
- **Fix:** cables drew a connection arm to every face of a machine, even where no power goes in (for example all around the alloy smelter). Now the arm and the energy flow use the same check.
- Three new game tests, including one for the cable fix.

### Merged 30 September 2026: PRs #8–#17

These were built as a stack, each on the one before, and merged in order (#8 first).

#### #17 Changelog and "What Exists" guide
- Adds this `CHANGELOG.md` and [`docs/WHAT_EXISTS.md`](docs/WHAT_EXISTS.md). WHAT_EXISTS is a map of all content, shared APIs, data formats, file locations and check rules, so contributors and AI agents can build alongside the existing systems.
- Adds feature records for [item logistics](docs/features/item-logistics.md) and [ore processing](docs/features/ore-processing.md).

#### #16 Ore processing depth
- **New machines:**
  - **Pulverizer:** ore → 2 dust, plus a byproduct roll.
  - **Ore Washer:** ore + water → 3 washed ore, which the pulverizer grinds.
  - **Sieve:** gravel → flint; soul sand → soul soil; both with small finds.
  - **Sawmill:** log → 6 planks + sawdust; planks → 3 sticks.
- **Three ore routes:** smelt (×1), crush or pulverize (×2), wash then pulverize (×3).
- **New items:**
  - 10 metal dusts (`c:dusts/<metal>`), which smelt into ingots. Nickel, tungsten and uranium dust use the arc furnace.
  - 10 washed ores.
  - Sawdust (4 → paper).
- **Byproducts:**
  - Machine recipes can list byproducts: chance, count, and an optional feature switch.
  - The pulverizer, sieve and sawmill have two byproduct slots, and a machine waits rather than lose a byproduct.
  - Byproduct pairs follow real ores, for example copper → gold and lead → silver.
- **Looks:** steampunk models (ball mill, washing vat with a water wheel, shaker sieve, sawbench) and classic textures.
- **Balance checks:** only ores get a bonus (×3 at most); byproducts add at most 25% of the input's metal; renewable sieve finds average under a nugget per operation.
- Seven new game tests.

#### #15 Item logistics
- **Side configuration:** every processing machine's screen has six face buttons (front, back, left, right, top, bottom). Each cycles between In, Out, Both and Off. The defaults keep the old hopper behavior.
- **Eject:** when on, the machine pushes its results out of its Out faces, 16 items every 8 ticks.
- **New blocks and items:**
  - **Brass Item Pipe:** 6 px. Items go first to matching sorters, then round-robin to the other inventories.
  - **Pneumatic Extractor:** 16 items every 8 ticks; a redstone signal pauses it.
  - **Item Sorter:** 9-slot filter.
  - **Brass Wrench:** turns machines; sneak to dismantle.
- Three new game tests.
- Fix: a multi-block machine's eject never feeds back into its own other blocks.

#### #14 Machine recipes as data, and in-game tests
- Machine recipes are now real Minecraft recipe types (`jugcraft:crushing`, `alloying`, …), one JSON file each under `data/<ns>/recipe/<type>/`. Data packs can add, change or remove them, and ingredients may be tags.
- In-game tests (Fabric game test API) run in CI with `./gradlew build`.
- **Launch-blocking fixes found by those tests** (before this PR, `main` could not start a server):
  - Fabric Loader 0.18.4 → **0.19.3**, which Fabric API 0.161.0+26.3 requires.
  - Ore worldgen moved to the 26.x format: `worldgen/feature/`, no `config` wrapper, block states as plain IDs.

#### #13 Steampunk machines
- Every machine redrawn as a detailed steampunk model: brass, copper, riveted iron, gauges, gears, valve wheels, and fireboxes that glow while running. Includes 37 original textures.
- The previous look stays available as the built-in resource pack **Jugcraft: Classic Machines**, so the change can be reverted.

#### #12 2×2 alloy smelter and power sockets
- The alloy smelter becomes a 2×2 multi-block with a visible copper **power socket**, the only place cables connect to it.
- Clear power-connection rules for every machine: each one either has a socket or takes power on any face that touches a cable.

#### #11 Multi-block machines
- Machines can occupy several blocks and are placed as one item, in the style of Immersive Engineering. Breaking any part removes the whole machine.
- **Geothermal Generator** (2 wide, lava → 64 JE/t) and **Wind Turbine** (3 tall, 4–24 JE/t by height and weather).

#### #10 Fluids, and tech-mod style cables and pipes
- **Bronze Fluid Pipe**, **Tinplate Tank** (16 buckets) and **Electric Pump**.
- Cables and pipes are thin (4 px) and connect to each other and to anything with energy or fluid storage, as in other tech mods. Items show as 3D segments.

#### #9 Parts and circuits
- **Metal Press** (ingot → plate), **Wire Drawer** (ingot → 3 wires), **Circuit Assembler** (basic and advanced circuits).
- Gears are crafted from four plates.

#### #8 Alloy smelter
- **Alloy Smelter** makes bronze, **brass**, **invar** and **solder** at metal-conserving ratios.

### Merged earlier on 30 September 2026

#### #7 Solar panel and steam generator
- **Solar Panel** (8 JE/t in sun) and **Steam Generator** (coal or bitumen plus water → 64 JE/t), plus a machine roadmap.

#### #6 Electricity and the first machines
- Jugcraft Energy (JE) with a shared `EnergyStorage` API, **Copper Cable** networks, **Coal Generator**, **Battery Box**, **Electric Furnace**, **Crusher** (ore doubling) and the **Arc Furnace** 3×3×3 multiblock.

#### #5 Base materials
- Zinc, lead, silver, nickel, tungsten, uranium, aluminum (bauxite), salt, phosphate, lithium (lepidolite), rare earths (monazite), sulfur, silicon and oil sand (bitumen).
- Feature switches in `config/jugcraft.properties`.

#### #4 Fabric 26.3 scaffold, tin and bronze
- Pinned toolchain and the CI Build workflow.
- Tin ore, raw tin, ingots and blocks, and bronze from a 3:1 copper/tin blend.
- The offline data generator and checker (`tools/`).

#### #1 Vision and development setup
- Project vision, contribution rules, design and architecture documents, and owner-directed Fabric development.
