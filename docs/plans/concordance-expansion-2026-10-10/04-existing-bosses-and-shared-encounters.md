# Existing bosses and the shared encounter contract

**MAIN:** Vesperine and Madame Tatterlace. **OPEN CONTRIBUTION:** Glacier Hall/Frost Horn, PR300 at `e12c1d1ce15b7ee50d13306e7f72bfdfff74df8a`. **PLANNED:** Yeti King encounter. New integrations below are proposals, not changes already made.

## 1. Preserve the shared lair foundation

Lairs already provide voluntary ritual entry, temporary gates, separate dimensions and bounded arenas. Reuse them. Each lair type allocates instances in slots 1,024 blocks apart. Current defaults are eight active instances and four participants, with configurable limits. Boss health scales by 50% for each extra party member up to 2.5× before other health configuration.

Current sessions do not survive a restart. Return information and grave goods do. An empty instance closes after its configured lifecycle; after restart players should recover safely, not resume an implied saved fight. Do not describe “persistent lairs” unless the actual arena-state persistence is implemented and tested.

Ordinary survival building/breaking is restricted; designated fixtures can be interacted with. Boundary enforcement returns players to arrival and applies a nonlethal toll. Existing teleport, food, potion and equipment behavior needs testing before assuming it is exploit-proof.

Every new lair must specify:
- Arrival location, safe staging space, wake condition and return gate.
- Which blocks are fixtures and who may operate them.
- Arena floor ownership and restoration rules.
- Eligible participants and party scaling.
- Reset when nobody eligible remains.
- Cleanup of projectiles, adds, temporary effects and reservations.
- Death, disconnect, restart and exact-once rewards.
- How ordinary melee/ranged players can solve every required mechanic.

## 2. Vesperine, the Last Reaper

### Existing identity and entry

The Hollow Acre is an island roughly 64 by 76 blocks, with a 40-block arena, bone chapel, moon imagery and four wards. At night in the Overworld, Last Rites uses a headstone, four lit candles within four blocks and a mourning wreath within two. The Death Knell is retained; the wreath is consumed on a successful opening. The gate lasts sixty seconds.

The fight is already available outside the seasonal celebration through the configured off-season behavior. Do not turn it into a once-a-year progression requirement.

### Existing combat

Vesperine begins seated and immune, then wakes over two seconds. Base health is 400, normal four-player cap 1,000, armor 10, combat leash about 32 blocks.

Dirge and Requiem are two 80-health skull companions, scaled with the party. While both live, Vesperine receives half damage. Removing one eliminates that reduction. Thrown-scythe recovery gives a further damage opportunity. Their bolts have an explicit tell and can be reflected once.

First phase:
- Scythe arc: about 0.6-second tell, wide 270-degree close arc, 14 damage and brief Wither.
- Lunge: about 0.5-second preparation, up to eight blocks, 10 damage.
- Scythe throw: about 0.7-second preparation, sixteen-block path, damage on outgoing and returning passes.
- Gravecall: 1.5-second preparation, summons three thralls with a bounded maximum.

At half health, Last Toll creates a three-second transition; the boss rises, the moon changes and fallen skulls return at half health. Later patterns include telegraphed beam lines, marked crop circles and a shadowstep followed by a readable arc.

At a quarter health, Death Harvest is a six-second event. Souls move toward the boss and heal it if they arrive. Players can kill souls or relight the four wards. A final radial slam punishes poor positioning. This is already a strong objective phase; preserve its identity.

Reset after the arena loses eligible players heals and clears the encounter, its participants and its fixtures.

### Existing rewards

Damage participants near the boss's death receive their per-player rewards. Shade is a material drop; the scythe has a first-kill guarantee and later chance, with skull/hood drops and shared XP. Shade has cheaper ritual/crafting uses. The scythe's cleave, harvesting heal and charged crescent are existing mechanics, not a new proposal.

### Proposed magical connections

- **Lampwright:** reveal a soul's destination or make one ward's activation state more legible. Ordinary players still see and can use the wards.
- **Hexweaver:** sever one eligible soul tether with the same bounded objective result as intercepting that soul. No deleting the whole phase.
- **Circlewright:** operate a ward remotely within a short allowed range, paying a resource and obeying the same cooldown.
- **Crimson Vigil:** protect a teammate during an exposed activation; effective protection qualifies for contribution.
- **Dreamwalker:** study a post-victory memory in the chapel. This grants elective insight, not exclusive core research.
- **Relic craft:** use shade for a choice between a death-themed utility and an encounter ornament. Keep ordinary equivalents for essential warding.

Do not add direct Hollow damage as mandatory because of theme. Do not permit universal dispel to delete skulls, skip Last Toll or cancel the encounter's lifecycle.

### Verification additions

Show every major attack with default graphics and reduced particles. Test reflection ownership, ward credit, a support-only player, a participant dying during Death Harvest, a return immediately after victory, and all adds disappearing on reset. Check scythe lifesteal against any new Vitae conversion.

## 3. Madame Tatterlace

### Existing identity and arena

The Spindle Loft is a giant sewing room: a doily floor, spool bridges, pincushion arrival and needle-eye return. The Cursed Spindle and spinning-wheel ritual establish the encounter's craft identity. Madame waits suspended above the arena.

Base health is 360, four-player cap 900, armor 8, with fire, poison and web immunity. Preserve these immunities until a specific redesign is justified; do not assume Ember wins because thread should burn.

### Existing combat

The distinctive system is the floor. A two-second fray warning precedes a missing three-block ring or sixty-degree wedge for twelve seconds, with at most two missing segments. Safe spool bands and the tape foot remain available. Restoration must match the arena template and avoid overwriting unauthorized changes; it is not a general world-edit ability.

First phase includes:
- Needlepoint: half-second tell, two short stabs separated by a quarter second, eight damage each.
- Thimble toss: an arcing projectile with two bounces.
- Binding Thread: short tell, a temporary tether that reels the target; a hit to the thread or the intended escape action breaks it.
- Lace Snare: a marked three-by-three restraint area with a limited duration.
- Spool roll: a clear lane attack with heavy knockback.

At half health she retreats upward during a three-second transition and presents six egg sacs. The hanging phase adds pin rain with marked landing points, floor unraveling, a drop with an explicit shadow and recovery opportunity, and a capped brood.

At twenty percent she remains down, shortens cooldowns and combines more floor pressure. Reset restores the arena and removes encounter-owned additions.

### Existing rewards

Gossamer Silk, the Needle Rapier with first-kill protection, Golden Thimble and headdress are existing rewards. Silk can reduce future ritual cost or become string. Fine yarn, decorative lace production and broader loom craft are opportunities, not already implemented outputs.

The rapier's repeated-hit control and the thimble's reflected projectile are existing combat interactions. Audit them before adding another generic reflection accessory.

### Proposed magical connections

- **Tether:** Threadcut interacts with the declared binding entity/fixture. Severing an ally's thread counts as assistance.
- **Strata:** a short Brace reduces ordinary knockback. It cannot disable floor removal or boundary recovery.
- **Rime:** approved temporary footing must be restricted to authored encounter anchors. Never allow arbitrary ice blocks to fill the whole missing arena.
- **Echo:** forecast one fray segment slightly earlier for the caster, while baseline warnings remain fully sufficient.
- **Verdance:** healing and safe positioning help recovery; roots cannot permanently pin a phase-transitioning boss.
- **Workshop extension:** a silk spindle recipe can offer finer decorations, link insulation or ritual pattern convenience. A mundane textile alternative provides necessary infrastructure.

### Verification additions

Test binding escape at different latency, overlapping spool/pin warnings, floor restoration after a reset and restart, and safety at every permanent bridge. A ranged build, mobility build and melee build must all have legitimate openings. Reduced-motion settings cannot remove the only sign that a wedge is about to vanish.

## 4. The Yeti King: preserve the accepted direction

### Part one: open contribution at audit

Glacier Hall is roughly 80×44×88 with a 41-block frozen lake, high southern arrival, a walkable ramp in both directions, four ice columns, protected trampled routes, dens and a tusked throne. Lair-only snow, ice, tusk and hoard blocks are not ordinary farmable world resources.

The Frost Horn is proposed in the contribution with a goat horn, gold, leather and snow. It opens at night while the player stands on snow/ice. A refused ritual should not spend it. The lair works all year.

Do not duplicate the structure, rename its blocks or change the entry recipe while part two is being built without reviewing PR300's latest state.

### Part two: existing plan, not yet an entity

The planned baseline is 420 health, armor 10, a 2.5× party cap, knockback/fall/freezing/web immunities, two-second wake and thirty-block leash.

First phase:
- Wide swipe, 0.6-second tell, twelve damage.
- Snow boulder, 0.8-second tell, a bounded impact radius.
- Ground slam, 0.8-second tell, fourteen damage and knockback.
- Frost breath, one-second tell followed by a short cone and freezing.
- Charge, 0.8-second tell and sixteen-block line. A column collision stuns the boss for three seconds and exposes it to 33% additional damage.

Slam temporarily clears drift snow into slippery glare ice, while authored trampled safe routes remain. This is a terrain-state mechanic inside the lair, not permission to melt an overworld settlement.

At half health, a three-second roar/blizzard transition introduces whelps. Later attacks include marked icicle falls, a spike line and a capped kin call. At twenty percent, fury shortens cooldowns, expands ice pressure and can chain a missed charge. Reset must restore the intended floor and fixtures.

Existing planned rewards are fur, first-kill-protected Glacier Maul/Rimeclaw trophy choice, a freeze-protecting mitten and a cosmetic crown, with XP and advancement. Fur can cheapen a horn or become white wool. Recheck the accepted part-two brief before coding exact drop numbers.

### Proposed Concordance links

Rime study can explain why preserved snow is safer than glare ice. Strata can brace a player during a charge bait. Ember can help ordinary environmental travel, but should not erase every ice mechanic or burn through a scripted immunity. Tempest can provide a deliberate movement option with a real cost.

A preserved reagent or a Thallite preparation can supply an ordinary pre-fight aid. A boss drop offers a distinctive variant, not the first and only cold protection. Build the planned physical fight first, then attach these optional counters.

## 5. Fair contribution and rewards

### Current limitation

The implemented reward qualification is primarily damage participation, including certain adds, plus proximity near victory. Healing, warding and objective actions do not automatically earn equivalent credit. This is a design gap, not evidence of an already verified duplication bug.

### Proposed contribution record

Use one record per participant per encounter instance:
- Actor UUID and authorized owner attribution for helpers.
- Join/last-relevant-action times.
- Capped effective damage.
- Effective healing of encounter-caused missing health.
- Damage actually absorbed/prevented by qualifying protection.
- Distinct objective IDs completed.
- Relevant presence and disconnect/death state.
- Reward receipt state.

A score should determine eligibility, not create a competition for who gets better loot. Once qualified, everyone receives the same category of personal roll. Do not announce damage rankings by default.

Pilot qualification: one meaningful objective OR a modest capped amount of effective combat/support work, plus active participation. Tune the threshold in tests. Do not enshrine “one hit” as universal fairness, nor exclude slow support builds.

Anti-farming rules:
- Self-inflicted friendly damage and heal loops do not qualify.
- Overheal, ward refresh with no absorption and invalid casts score zero.
- Repeatedly toggling the same fixture scores once per legitimate objective occurrence.
- Pet/construct actions credit their registered owner, not arbitrary nearby players.
- Late arrivals cannot receive rewards without participating.
- A qualified player killed just before victory receives a bounded grace policy.
- Logout cannot produce two receipt owners; reconnect resumes eligibility, not a fresh roll.

Persist reward receipts if they must survive a crash; the runtime encounter need not thereby become persistent. Define the write order for qualified receipt, chosen loot, delivery and acknowledgement. If actual crash atomicity is not available, document the recovery strategy and test that no stack exists in two authoritative places.

## 6. Shared encounter APIs to extract only when used

Existing bosses have bespoke behavior and animation. Extract the stable shared seams, not a universal boss language:
- Encounter ID and participants.
- Phase notification and readable telegraph.
- Tagged fixtures with allowed interactions.
- Temporary terrain ownership/restoration.
- Contribution events and reward receipt.
- Cleanup and safe-return hooks.

Proposed interaction labels: reflectable projectile, severable link, groundable conductor, relightable ward, bracable knockback, revealable decoy, charge-stopping column. These are explicit capabilities, not broad elemental rock-paper-scissors.

Every required mechanic has an ordinary interaction. A specialty makes it more convenient or opens a different tactical opportunity; it does not become mandatory. Boss tolerance should reduce or transform control effects rather than silently discard every spell.

## 7. Acceptance for any boss chunk

A boss is complete when it has survival entry, reachable preparation, a readable fight, usable rewards, safe reset/return, documentation and real evidence. A rendered model and a health bar are not an encounter.

Test solo and four players, then the supported configured cap. Include melee, conventional ranged, caster and support. Inspect attack timing against current guns and mobility items. Record actual completion times and failed mechanics. No claim of “balanced” until the human playtest exists.

[Next: expanded encounter roster](05-new-boss-compendium.md)
