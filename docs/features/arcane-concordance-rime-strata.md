# Arcane Concordance: Rime and Strata (Rimekeeping, Stratawrighting; Rimebind, Brace and Preserve)

Status: implemented for Concordance Chunk B, unit 3 (tickets B-05, B-06 and B-07 of
[the contract](concordance-chunk-b.md)), on branch `claude/chunk-b-winter-stone`, based on main `feb6d04fb` (#352)
with the contract and unit 1 beneath it; not merged. CI run 38519234934 on `06dc5ea5f` built it and passed every job:
its server game tests among the 1639 required, with and without the optional integrations, and its client game tests.
They passed again, with the status shots' fix, in runs 38543250503 (units 1 to 3) and 38549876709 (units 1 to 4); the
fixed shots show the status icons (runs 38534127130 and 38549876709). Unit 5 (B-09) gives Brace and Rimebind their
part in the Yeti King's fight, makes his column Rimekeeping's elective cold work and adds a spell-bar shot at GUI scale
1; its tests pass in CI (run 38591108210, units 1 to 5). Not played by hand or with two clients; see Verification for
what has run.
Proposal issue: none; the owner chose Chunk B on 13 October 2026 ("Ok lets begin Chunk B"), from
[the expansion plan's delivery chunks](../plans/concordance-expansion-2026-10-10/09-claude-delivery-chunks.md) (B2).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Practitioner stage (Jugcraft Workshops), once First Light is understood, beside Ember. Rime
and Strata are the second and fourth of the four elemental Principles (Ember, Rime, Tempest, Strata) the Concordance
named on 5 October.
Primary specialty and supported player role: the Rimekeepers ("preservation, cooling and barriers") and the
Stratawrights ("soil care, shaping and mineral sensing"). Defense by control (Rimebind), support (Brace) and workshop
utility (Preserve).

## Player experience

- **Rimekeeping** is a new research entry. Once First Light is understood, **sneak and use** snow or ice to examine it
  (a snowball, snow block, ice, packed ice or blue ice; sneaking, a snowball is examined, not thrown). Examine something
  that holds fire as well (Hearthbinding's specimens: coal, charcoal, a torch, a campfire and the rest), in either
  order, and Rimekeeping is observed. It is **understood** by any one of:
  - studying snow or ice at a **Lampwright's Bench**;
  - **arriving in the Glacier Hall**, by any route (the Frost Horn, the whirl);
  - **bottling a brew that carries Rime by hand** at an Alembic Crucible (a snowball in water, stirred, then a glass
    bottle; the Alembic Arts understood);
  - reading someone's Research Notes.

  The hall and the brew are the "winter survey": First Light alone gates them, so either can come before snow is ever
  examined, and counts once it is.
- Understood, Rimekeeping teaches **Rimebind** and **Preserve**. **Mastery** is three kinds of cold work, each counted
  once: Preserve a crucible holding a mixture while it is cold, Preserve one while it is warm or hotter, and Rimebind a
  hostile creature. Only someone who understands the **Alembic Arts** puts water in a crucible by hand, so mastery needs
  them too. Mastered, both cost a point less Focus.
- **Stratawrighting** is the other new entry. Once First Light is understood, sneak and use **granite, diorite,
  andesite, tuff or raw Thallite** to examine it. The same examination also begins the Balancewrights' **Assay** (these
  are raw matter too). Three different ones observe it; studying one at the bench, or reading notes, understands it.
  Cobblestone, stone, cobbled deepslate and deepslate are never examined, so a sneaking player can still bridge with
  them.
- Understood, Stratawrighting teaches **Brace**. **Mastery** is three ways of anchoring: Brace someone standing on soil
  (grass, dirt, mud, moss), Brace someone standing on bare stone, sand or gravel, and have a Brace you cast hold against
  a blow. Any of three electives can stand for one: bracing a party member, completing a ritual at a Circle Anchor
  (Circle Lore; Root and Rivet's codex entry is linked from Stratawrighting's Understood page: Concordance Chunk C,
  C-04; not merged; its tests passed in CI, run 38693262154), or taking a creature's blow in Earthbound Thallite armour on natural ground. Mastered,
  Brace costs a point less.
- The three invocations, cast with any Concordance instrument like First Light's and Ember's:

| Invocation | Role | What it does | Focus (mastered) | Cast | Cooldown | Learned | Tunings | Counter |
|---|---|---|---|---|---|---|---|---|
| Rimebind | defense | the first creature on a 16-block ray moves a fifth slower for 3 s (**Rimebound**), then **thaws** for 2 s, in which no one's Rimebind takes it | 4 (3) | 0.5 s | 2 s | Rimekeeping understood | Extend | slows, never roots; at most 3 s bound in every 5, however many cast; milk clears it; the Yeti King and the Cinder Tyrant are bound half as long; the Ender Dragon, Wither, Warden and Elder Guardian shrug it off |
| Brace | support | you and your party within 4 blocks are **Braced** for 5 s: the next blow or harmful push moves each of you half as far | 3 (2) | 0.4 s | 8 s | Stratawrighting understood | Widen | one blow spends it; never stacks; lessens no harm; one's own Flashstep and a friend's helpful push ignore it; a teleport, a portal or a lair's mist moves the braced as anyone |
| Preserve | utility | the Alembic Crucible you touch (4 blocks) keeps its heat band (cold, warm, hot or searing) for 60 s (90 s or 120 s with a focus of water worn, unit 4), whatever burns or goes out beneath it | 3 (2) | 0.6 s | 3 s | Rimekeeping understood | none | only a crucible you could change by hand; a second cast starts the hold again, never adds; nothing else is held: no chest, barrel, machine, food or item |

- **Rimebound** is a new status with the owner's frost icon; **Thawing** is hidden (no icon on the HUD, no particles),
  though the inventory lists it, as it lists every effect. **Braced** shows the owner's weight glyph. Brace is not the
  war fork's Brace (harder blows against a charging foe) nor a tower shield's steadiness; its codex page and counter say
  so.
- Jade, and the crucible's reading when used with an empty hand, show the seconds a held crucible has left.
- Two new codex categories, **Winter** (Rimekeeping, Rimebound, Preserve) and **Stone** (Stratawrighting, Braced), and
  an Invocations page each for Rimebind, Brace and Preserve, open as each is learned. The journal shows both entries'
  progress like any entry's, and the Starbound Conclave counts the Rimekeepers and the Stratawrights among its
  traditions.

## How it works

- Data, generated by `tools/concordance_rime.py` and `tools/concordance_strata.py` and merged by `tools/concordance.py`:
  research `rimekeeping` and `stratawrighting`; practice gates `winter_survey` (First Light), `rimework` (Rimekeeping)
  and `anchoring` (Stratawrighting); components `rimegrip`, `rimehold` and `bracing` (authored: no player composes with
  them, B-P6); invocations and Spell Engine spells `rimebind` (frost school), `preserve` (frost school) and `brace`
  (arcane school: Spell Power has no earth school); item tags `#jugcraft:rime_specimens` and
  `#jugcraft:strata_specimens` (both in `#jugcraft:concordance_specimens`; the second also nested inside
  `#jugcraft:assay_specimens`); the block tag `#jugcraft:concordance/preservable` (the crucible only); eight
  advancements; codex pages; lang; sounds.
- Compositions in the shared grammar, compiled under every instrument's limits: `ray struck rimegrip`
  (`ray/struck:status/harmful`), `here allies bracing` (`here/allies:status/helpful`; `allies` is the caster and their
  party, so the party is the consent, B-P7) and `touch struck rimehold` (`touch/struck:alteration/helpful`). Each
  signature is new. Thirteen invocations of the sixteen an instrument's tunings can hold.
- Java, `concordance/rime` (`Rime`, `RimeSurvey`, `Rimebind`) and `concordance/strata` (`Strata`, `Brace`), registered
  beside Ember whatever the concordance switch (a saved effect needs its id). Changes to shared code:
  - **The effect boundary** (`ConcordanceEffects`). After a status takes, `Rimebind.bound` starts the Thaw and
    `Brace.braced` remembers who put the Brace on and records the caster's ground. An alteration in Rime's school
    (`spell_power:frost`) holds a crucible (`Rime.preserve`) and is dispatched before the default alteration, which
    removes fire; `accepts` chooses only a crucible for it. A harmful push reads knockback resistance with the Brace and
    spends it; a helpful one reads it without (`Brace.resistanceWithout`) and leaves it (B-P12).
  - **Rimebound** carries an attribute modifier, `jugcraft:rimebound`, −0.2 movement speed multiplied on the total, as
    the agriculture hexes do. The **Thaw** (`jugcraft:rime_thaw`) lasts the binding's own time plus 40 ticks, and a
    boundary Guard refuses Rimebound while it lasts: refused, the cast fizzles and costs nothing.
  - **Braced** carries `jugcraft:braced`, +0.5 knockback resistance, exclusive. Who put it on is kept in a non-saved
    attachment, `jugcraft:brace_caster`. See the spending table below.
  - **Preserve** sets the crucible's `held_until` (game time). While held, the crucible's heat still moves a degree a
    tick toward its source, but `Band.keep` clamps it inside the band it was in; when the time passes the hold is
    forgotten. A recast restarts the hold. The crucible accepts holds of up to `Rime.MAX_HOLD_TICKS` (2400, the
    boundary's longest time) and cuts anything further ahead, so unit 4's water foci (B-P24) fit unchanged.
  - **The crucible** tells `RimeSurvey.bottled` of every dose bottled by hand; one that carries Rime is the workshop
    study. Its empty-hand reading and its Jade snapshot carry the seconds held.
  - **The Glacier Hall** is caught by Fabric's `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL` (lairs move
    players with `teleportTo`): no lair code changes.
  - **Examination** says why a snow or stone specimen matters (`message.jugcraft.concordance.examine.cold`, `.weight`).
  - **Strata's practice** listens to rituals completed (`Rituals.listen`: every participant present) and to damage
    (`AFTER_DAMAGE`: an Earthbound wearer struck by a creature on natural ground, not wholly blocked). Ground is read
    with Thallite's own tags, `ThalliteGear.LIVING_GROUND` and `EARTHEN_GROUND`; nothing is added to them (B-P28).

### Research keys

Each key is a piece of evidence, saved as `practice:<activity>:<key>` and counted once.

| Activity (gate) | Key | How it is recorded | Needs beyond the gate |
|---|---|---|---|
| `jugcraft:winter_survey` (First Light understood) | `glacier_hall` | arriving in the Glacier Hall's dimension | a Frost Horn blown at night on snow, or a friend's whirl |
| | `crucible` | bottling by hand a dose whose Rime is above 0 | the Alembic Arts understood (to work the crucible by hand) |
| `jugcraft:rimework` (Rimekeeping understood) | `preserve:cold` | a Preserve that holds a crucible holding a mixture, in the cold band | the Alembic Arts understood, to put water in a crucible by hand (`CrucibleBlockEntity.use`); a crucible with water or a brew in it |
| | `preserve:heated` | the same, in the warm, hot or searing band | the same, over a heat source (a magma block keeps it warm, a campfire hot) |
| | `restraint` | a Rimebound that takes on a hostile creature (`Enemy`) at normal tolerance | a monster within 16 blocks |
| | `elective` (elective; unit 4 as `frost_boon`, shared since unit 5, B-P56) | a Frost boon arm's slow that takes on a hostile creature at normal tolerance (`weapons/ArmItem.boon` → `Rime.frostBoon`, on `Rimebind.restrains`, restraint's targets); or, since unit 5, the Yeti King's charge broken on an ice column, for the player its column deed names (`YetiKingEntity.baited` → `Rime.column`). One key: after either, the other adds nothing | a Frost boon arm: the Glacier Maul or Rimeclaw (the Yeti King's trophies) or the creative-only Frost Knight Greatsword; or a part in his fight already (B-P3) |
| `jugcraft:anchoring` (Stratawrighting understood) | `ground:living` | a Brace that takes on someone standing on living ground | nothing: oneself on grass counts |
| | `ground:earthen` | the same on other natural ground (stone, sand, gravel) | nothing |
| | `held` | a Brace the caster put on is spent by a push | a blow or a harmful push to meet it |
| | `ally` (elective) | a Brace that takes on another player | another player in the party |
| | `foundation` (elective) | a ritual completed at a Circle Anchor, for every participant present | Circle Lore and a circle |
| | `rooted` (elective) | struck by a creature, not wholly blocked, in Earthbound Thallite on natural ground | Earthbound Thallite armour (the thallite switch) |

Rimekeeping's mastery needs three kinds of cold work: its own three keys, of which the elective can stand for one (a
Frost boon arm's slow, [the winter record](arcane-concordance-winter.md), or since unit 5 the Yeti King's column: one
key between them, so with Rimebind's restraint they make two kinds and a Preserve is always needed, B-P56); both
Preserve keys need the Alembic Arts: a crucible's mixture changes only by hand, which needs them, or by a formula an
alembist set, while pipes fill only its water tank and hoppers only the buffer a formula draws on. `check_rime` holds
the keys to that, and `check_progression` holds the progression graph's `rimework` route to the same research.
Stratawrighting's needs three of its six, and its own three need nothing past the research and a blow to brace against,
so neither Thallite nor another player is ever required (`check_strata` holds both rules). A Rime key can be stored
while Rimekeeping is still unmet, when fire is examined first; the journal does not show an unmet entry.

### When a Brace is spent

A Brace is spent by the first push it meets and stays in force until that server tick ends (B-P30), so a whole blow is
braced whatever order its parts come in. A push from a cast arrives in its packet, between ticks: a Brace spent there
goes when the next tick starts, so it braces no blow of that tick.

| What meets the Brace | Braced? | Spends it? |
|---|---|---|
| A blow vanilla knocks back (it took full damage this tick, is not `#minecraft:no_knockback`, and was not wholly stopped by a shield) | yes, with the extra knockback of a sprint, a Knockback enchantment or a mob's attack that follows in the same tick | yes, at the end of the tick |
| Another blow in the same tick | yes | already spent |
| A harmful Concordance push | yes | yes |
| The Leaf Blower's stream (another player only under PvP) | yes, the first tick's push | yes |
| Vanilla knockback that comes without a damaging blow (the review names a mace smash's bystanders) | yes | no: an accepted exception (B-P30) |
| A helpful push: one's own Flashstep, a friend's helpful push | no: read without the Brace | no |
| A scripted push that asks through `Brace.spend` (the Yeti King's every push, since unit 5, B-09) | returns the share to let through, 1 − 0.5 a level | yes |
| A teleport, a portal, the Grey Mist, the hall's edge toll, a whirl | not a push: the Brace changes nothing | no |
| Damage | never lessened | no |

### Tolerance of the lair keepers

Only the Yeti King and the Cinder Tyrant join `#jugcraft:concordance/resistant` (B-P8); Vesperine, Madame Tatterlace,
the Mire Hag and the Crypt Lich do not (`check_rime` holds it). Resistance halves every harmful Concordance status and
push on them, not Rimebind's alone:

| Effect | Yeti King | Cinder Tyrant |
|---|---|---|
| Rimebound (Rimebind) | 60 → 30 ticks, Thaw 70 | 60 → 30 ticks, Thaw 70 |
| Smoulder (Cinderbolt, Hearthflare, the Fire Bangle) | 60 → 30 ticks | already nothing: he is immune to fire |
| Composed Dazzle | 60 → 30 ticks | 60 → 30 ticks |
| The Ice Breaker wave's Slowness | 40 → 20 ticks | 40 → 20 ticks |
| Concordance pushes | already nothing (knockback resistance 1.0) | already nothing |

The Yeti moves by fixed steps, not by his movement-speed attribute, so Rimebind's slow alone does nothing to his charge.
Since unit 5 (B-09) a binding that takes holds his wind-up (the one under way unless it is about to strike, else his
next) 4 ticks longer instead, once each Thaw (B-P54), and only a Brace lessens his push; `aRimeboundChargeKeepsItsPace`
shows that his charge keeps its pace when bound (passed in CI, run 38591108210)
([the Yeti King's record](yeti-king.md#optional-counters-concordance-chunk-b-b-09)).

## Changes from the plan

- Rime's research steps (02:131-137): "a warm surface" is something that holds fire, examined; "preserve an allowed
  sample" and "compare warm and cold preparations" are Preserve on a cold and on a heated crucible; restraint is
  Rimebind on a hostile creature at normal tolerance; "finish a preservation workshop or study Glacier Hall" is the
  winter survey (a Rime brew bottled by hand, or arriving in the hall). The Yeti column charge as an elective came
  with B-09 in unit 5, sharing the Frost boon's key (B-P56).
- Strata's (02:139-144): "assay existing Thallite and ordinary stone" is the shared examination; "use a brace or
  earth-bound tool on an allowed substrate" and "compare living and earthen ground" are the ground keys and Rooted
  Thallite; "a stable ritual foundation" is the `foundation` elective. **Deferred:** an ordinary civic project or a
  structural boss opening.
- Rimebind's targets follow the shared boundary (monsters; creatures the caster could strike by hand; players only under
  PvP and outside the party), not "hostile ordinary creature" only; only a hostile creature counts as restraint (B-P34).
- Preserve holds an Alembic Crucible's heat band only (02:77's "owned workstation"): no reagent, item or machine (B-P13,
  B-P35).
- Against the contract: Stratawrighting has its own specimen tag inside Assay's (revised B-P15); Rimekeeping is
  understood by the winter survey rather than a compare-two-brews step (revised B-P16); a Brace lasts out the tick it is
  spent in (B-P30); the owner's icons are used at their own sizes (B-P31); Jade's hold moved here from B-08; there is no
  Thallite Focus and no crafted upgrade in this unit (B-P14: the water foci in unit 4).
- **No bench-free Adept.** Arriving in the Glacier Hall understands Rimekeeping without a study (02:136: either route
  advances core understanding), but its mastery needs the Alembic Arts, which are understood at the bench or from
  notes, so the Lampwright's Bench stays unavoidable at Adept
  ([the mandatory table](arcane-concordance-progression.md)). The unit first claimed a bench-free Adept because the
  progression graph's `rimework` route left the Alembic Arts out; the unit review found it. The route and the keys now
  ask for them, and `aRimekeeperReachesAdeptByTheGlacierHall` fills a real crucible.

## Connections

- Input producer: First Light (both entries need it understood); snow and ice (snow golems, snow in cold biomes, water
  that freezes again); Hearthbinding's fire specimens; granite, diorite, andesite and tuff (stone veins underground);
  raw Thallite where the thallite switch is on; the Lampwright's Bench; the Frost Horn and the Glacier Hall; the Alembic
  Crucible and the Alembic Arts; the Focus pool.
- Output consumer: players and creatures through the shared effect boundary (control and protection); the Alembic
  Crucible (a band held while stirring, the loop unit 4's Winter Brine uses: a cold stir keeps its Rime, B-08); B-09's
  Yeti counters, built in unit 5 (`Brace.spend` on his every push, `Brace.caster` for the deed, Rimebind's held
  wind-up, the column's elective); unit 4's travel kit
  ([the winter record](arcane-concordance-winter.md)): a Lesser Focus of Water or a Focus of Water worn makes Preserve
  hold 90 or 120 seconds (`RimeGear.holdTicks`, B-P24, B-P50), and the Water Bangle gives its wearer Brace's own Braced
  after 30 seconds with none, as nobody's cast (B-P25, B-P49).
- Technology connection: the crucible's automation is unchanged by a hold (hoppers still feed it, pipes still fill it);
  Jade shows the hold.
- Magic connection: two more traditions for the Conclave; one examination serves Assay and Stratawrighting; Circle
  Lore's rituals and Earthbound Thallite are Strata electives; Hearthbinding's specimens observe Rimekeeping.
- Reachable entry path: First Light understood → a snowball and coal → the bench (or the hall, or a bottled Rime brew) →
  the Alembic Arts understood (the bench or notes), to fill a crucible by hand → Preserve a cold crucible and a warm one
  and Rimebind a zombie. Granite, diorite and andesite → the bench → Brace on grass and on stone and take a zombie's
  blow braced. Nothing needs the Nether or the End, and Strata completes with the thallite switch off (`check_mod_data`
  runs the progression graph both ways; `strataNeedsNoThallite`).
- Solo, trade and cooperative routes: entirely solo; Research Notes can be shared; bracing a party member is one
  elective.
- Specialty use without other branches: understanding needs only First Light; mastery also needs the Alembic Arts, as
  Preserve's keys want a crucible with water in it, filled by hand.

## Balance

- Each invocation costs at least what its composition costs a composer, and its cooldown is never shorter than the
  composition's (the loader and `check_mod_data`). Focus is charged once a cast, when Spell Engine consumes its cost,
  however many it braces.
- **Rimebind** (4 Focus, 2 s): a 20% slow for 3 s, then 2 s in which no one's Rimebind takes the creature, so at most 3
  s bound in every 5 (60%), shared by every caster: the Thaw sits on the target. Vanilla Slowness I is 15%. Only Extend
  tunes it (+8 blocks of reach); no tuning may lengthen or deepen the slow (`check_rime`). It is defense: the benchmark
  never casts a utility (B-P33).
- **Brace** (3 Focus, 8 s): half the knockback of one blow or harmful push, for at most 5 s; it never lessens harm and
  never stacks. With netherite, Rooted, the Ice Breaker and a shield, one blow can reach full knockback resistance:
  proposed as tolerable for one blow in five seconds (B-P11). Two blows in one tick are both braced (B-P30).
- **Preserve** (3 Focus, 3 s): a fixed 60 s, not scaled by Spell Power (an alteration takes no duration), never added
  to. Recasting each minute holds one crucible indefinitely, but only within 4 blocks of the caster, and it gives no
  band a heat source cannot. Nothing is made, so there is no conversion loop and `check_economy` is untouched.
- **Benchmark** (B-P32): it now models Rimebound (a fifth of a foe's steps a level, then the thaw), starts no cast that
  would change nothing, casts a defense with a harmful step at any foe in reach, and measures knockback taken without
  moving anyone. Two acceptance checks are added: Rimebind's control stays within a binding for each binding and thaw in
  a minute, plus one (7.8 foe-seconds), and Brace takes knockback off beside the unkillable brute. A scratch harness
  (javac 21 over this tree's generated data; not CI) read 6.6 for the first and 5.2 against 5.6 bare-handed for the
  second, every earlier cell unchanged. CI's game test (run 38519234934, `ConcordanceBaselineGameTests`) logged the
  same, the acceptance checks held, and [the baselines record](arcane-concordance-baselines.md) now holds its tables.
  The three new rows, from that log:

| Character | Isolated target | Clustered group | Mobile opponent | Protected target | Objective defense |
|---|---|---|---|---|---|
| Only jugcraft:brace | lost 8.8 s | lost 5.2 s | lost 37.5 s | lost 7.6 s | lost 8.8 s |
| Only jugcraft:preserve | lost 9.3 s | lost 8.4 s | won 40.4 s, 4 HP | lost 6.6 s | lost 8.8 s |
| Only jugcraft:rimebind | lost 11.0 s | lost 7.8 s | lost 40.5 s | lost 9.1 s | lost 8.8 s |

| Character | Sustained (dmg/s) | Burst (5 s) | Survival (s) | Movement (blocks/min) | Control (foe-s/min) | Efficiency (dmg/Focus) | Support (per ally/min) | Knockback (blocks/min) |
|---|---|---|---|---|---|---|---|---|
| Only jugcraft:brace | 1.88 | 9.4 | 6.9 | 0.0 | 0.0 | 0.00 | 0.0 | 5.2 |
| Only jugcraft:preserve | 1.88 | 9.4 | 7.5 | 0.0 | 0.0 | 0.00 | 0.0 | 5.6 |
| Only jugcraft:rimebind | 1.71 | 8.5 | 9.0 | 0.0 | 6.6 | 0.00 | 0.0 | 5.6 |

Preserve, never cast in a fight, matches Kindle's and Revelation's rows. Rimebind alone gives the most control of any
character (6.6 foe-seconds a minute) and wins no encounter; Brace alone wins none either, and its casts cost it
survival time (6.9 s against Preserve's 7.5).

## Multiplayer and persistence

- Server authority: every cast runs on the server through the invocation path (instrument, research, Focus and rate
  checks). Preserve asks `Authority.mayChange` for the person behind it (claims, protected towns, spawn protection,
  adventure mode; an absent caster is refused). Rimebind follows the friendly-fire rules and `Authority.mayStrike`, as
  every harmful effect does. Brace reaches only the caster's party. Evidence is recorded on the server; no client claim
  counts.
- Persistence:
  - **Knowledge:** the new research ids and keys sit inside the existing versioned record; the schema and codec are
    unchanged.
  - **Effects:** Rimebound, the Thaw and Braced save with the creature in vanilla's `active_effects`, so the grace
    survives a logout. Their attribute modifiers are not re-added when the effects load: vanilla restores the saved
    `attributes` first, then puts `active_effects` back without `onEffectAdded` (mc263src `LivingEntity.java:780-786`),
    so the modifiers come back only because they were saved among the attributes. That they are saved there is assumed
    from 1.20.5 to 1.21 (`AttributeInstance`'s packing is not in the mirror) and checked by `rimeAndBraceSurviveASave`,
    which passed in CI (run 38519234934): the modifiers came back.
  - **Not saved:** `jugcraft:brace_caster` (a Brace carried through a relog keeps its time and credits no one) and the
    set of Braces spent this tick (at most one tick long).
  - **The crucible's `held_until`** (game time) is written only while held, read with a default of 0, and the crucible
    still stamps version 1. A hold runs on the world clock: a crucible unloaded while held finds its minute over when it
    loads. A forged hold far in the future is cut to 2400 ticks on its next tick.
- Disable behaviour: with `concordance.enabled=false`, examining and casting are refused like every Concordance action,
  and the passive hooks record nothing: arriving in the Glacier Hall (the lairs are not switched off with it) and a blow
  taken in Earthbound Thallite. The effects, the attachment and the tags stay registered.
- Two blows landing in one tick are both braced; a save made inside the tick a Brace is spent can keep it.

## Dependencies and assets

No new dependency. Framework use: **Spell Engine** (three spells), **Spell Power** (Rime's frost school, which marks
Preserve's alteration), **Fabric API** (the level-change, damage and server-tick events; an attachment), **Modonomicon**
(the Winter and Stone categories and three invocation pages), **Jade** (optional: the seconds held).

**Provenance.** The icons and Brace's release are from the owner's magic collection
(`art/owner-library/originals/Magic`, supplied 8 October 2026,
[MAGIC_ASSETS.md](../../art/owner-library/MAGIC_ASSETS.md)). The owner says they used other Minecraft mods as a starting
base and then "completely redid and remade all the stuff" ([PROVENANCE.md](../../art/owner-library/PROVENANCE.md)); this
record repeats that declaration, which is not an independent rights audit. The project uses the files under the owner's
project-use authorization ([LICENSE_POLICY.md](../../LICENSE_POLICY.md), owner-supplied collections). The source
folders' names (`ars_jimbaux`, `ars_nouveau`) are kept here as labels; nothing in the game uses them. The imported files
carry no credits or notices (the PNGs have no text chunks; each Ogg's comment is `ENCODER=REAPER` only), and
`catalog/legacy-metadata.csv` has no row for them. They are byte-for-byte copies under Jugcraft names, at their own
sizes, written by `tools/owner_art.py` from `OWNER_FILES` in `tools/concordance_rime.py` and
`tools/concordance_strata.py` and checked byte for byte by `check_mod_data`; `owner_art.py` refuses two different
sources for one runtime path. `python3 tools/owner_art.py --provenance rime` and `--provenance strata` print this table:

| Runtime file (under `assets/jugcraft`) | Owner's file | SHA-256 of the owner's file | The owner's meaning |
|---|---|---|---|
| `textures/spell/rimebind.png` | `originals/Magic/assets/ars_jimbaux/textures/item/freeze.png` | `cc2830c6a0d77fb35e5bbaa4ec49c7b8fbf317e9374e1365660b6ef97f27584c` | Freeze |
| `textures/spell/preserve.png` | `originals/Magic/assets/ars_jimbaux/textures/item/extend_time.png` | `a04f6d0c3a71822a512deaf30ed15a4e62ec09f45e99e9552eec468a269f44d5` | Extend Time |
| `textures/mob_effect/rimebound.png` | `originals/Magic/assets/ars_jimbaux/textures/mob_effect/freezing.png` | `69e0fae8f2203495ba7b06b48b28dea5ccb01c3bdcce5958edafe1a4bda7bb7e` | Freezing |
| `textures/mob_effect/rime_thaw.png` | `originals/Magic/assets/ars_nouveau/textures/mob_effect/soaked.png` | `b32b31adf8276981b5b7d6932de0d59b27312d348df499438c95a808ff825760` | Soaked (named in the owner's `ars_jimbaux` lang) |
| `textures/spell/brace.png` | `originals/Magic/assets/ars_jimbaux/textures/item/gravity.png` | `ffea3d92ab0ca3370ea3287c5829e3f396694419e202afe759d29b8eddd3c930` | Gravity (weight) |
| `textures/mob_effect/braced.png` | `originals/Magic/assets/ars_jimbaux/textures/mob_effect/gravity.png` | `ffea3d92ab0ca3370ea3287c5829e3f396694419e202afe759d29b8eddd3c930` | the same image: the owner's own glyph and status pair |
| `sounds/concordance/gaia_1.ogg` | `originals/Magic/assets/ars_jimbaux/sounds/gaia_1.ogg` | `236c4f69eca1bf904cea9257562483cbf5327d84a668242c7503e985c949f0ae` | earth |
| `sounds/concordance/gaia_2.ogg` | `originals/Magic/assets/ars_jimbaux/sounds/gaia_2.ogg` | `7558cd611e6103be9268777dbbbdffea350531a644320e9c756e4fbdd2fef318` | earth |
| `sounds/concordance/gaia_3.ogg` | `originals/Magic/assets/ars_jimbaux/sounds/gaia_3.ogg` | `7845aa4bf0046b56b8895953817553eba46034e6916d5d40e8a9e9dc0a3d74e2` | earth |
| `sounds/concordance/gaia_4.ogg` | `originals/Magic/assets/ars_jimbaux/sounds/gaia_4.ogg` | `673c8c28f3f88fda25ac6fdedddc5f91822fb0d46b37c4973859a3f662d9d11c` | earth |

Also taken from the owner's files, as data rather than copies: Brace's release list, the four files the owner's
`gaia_family` event plays (`assets/ars_jimbaux/sounds.json`). No language file is imported.

No scaling (B-P31): Spell Engine draws a spell icon into 16×16 from the whole texture (upstream, Spell Engine 1.10.9),
and vanilla draws status icons at 18×18, stretching the 16×16 ones as it already does for `giant.png` and `shrunk.png`.
How they look in game is seen only in CI's 480×270 previews: the spell bar shows its slots 2 to 6, and since the toasts
are cleared the HUD shows Rimebound's and Braced's icons and the inventory all three (Verification). Alternatives
offered at the checkpoint: `shield.png` and `shielding.png` for Brace (but shielding reads as "reduces damage", which
Brace never does), `frozen.png` (the owner's "cannot heal") and `delay.png` for Preserve.

Drawn by code, in the Concordance's D, A and E like Ember's (`tools/concordance_sounds.py`): `rime_gather` (Rimebind's
and Preserve's cast: high wind under glassy voices), `rimebind` (a crystalline crack and falling glassy tone),
`preserve` (a soft held fifth: a hold, not a blow) and `strata_gather` (Brace's cast: a dry grind with gravel ticks). No
ice or cold casting recording was found in the owner's sound families.

## Verification

Run for this unit where it was written, on 14 October 2026 (no Minecraft jar is available there, so no Minecraft build
was compiled or run there):

- `python3 tools/owner_art.py --check`, `python3 -B tools/check_mod_data.py` and `python3 scripts/check_repository.py`
  pass on each of the unit's commits.
- New audits `check_rime`, `check_strata` and `check_rime_strata_assets`, beside the existing research, invocation,
  progression, Conclave, delivery and owner-art audits they must also satisfy. **Mutation-tested** on an export of the
  unit's game-test commit: 33 injected faults were each reported and the clean tree passes. Among them: a fourth Rime
  key required; a bridging block as a specimen; the survey gated by Rimekeeping; a renamed Strata key; Rimebind's work
  set to 1 or a Prolong tuning; Preserve dispatched after the dousing, missing from `accepts`, a hold of 1300 ticks, a
  chest made preservable, `Rime.preserve` called from elsewhere, an inventory touched in `hold`; `brace_caster` made
  persistent; the harmful-only test removed from the push; Vesperine or the Mire Hag made resistant; the Thaw or the
  bottling hook missing; Jade's lang key renamed; Brace's counter without the war fork; the Guard dropped;
  Rimekeepers missing from the Conclave; the Glacier Hall alternative removed from the graph (the "later stage" problem
  returned, at that commit); two sources for one owner file; one pixel of an owner icon changed; and each of the
  benchmark's five mirrored constants.
- The progression graph (`check_progression`) reaches every stage alone, without the Nether or the End, and the
  mandatory table and journey routes it writes were regenerated.
- The benchmark, by a scratch javac harness (see Balance); not the game test.
- **After the unit review** (same day): the three checks pass again. The review's new audit rules were mutation-tested
  on an export: the Alembic Arts dropped from the graph's `rimework` route, a Preserve key needing nothing, the Leaf
  Blower spending no Brace, no settling at a tick's start, the crucible's snapshot or the client's crucible branch
  without the hold, and the spell bar paragraph changed. Each of the seven was reported, and the clean export passes.

**CI, run 38519234934** (workflow_dispatch on `06dc5ea5f`, units 1 and 3; not merged): all nine jobs succeeded.
`mod` logged "All 1639 required tests passed" in 3.994 min ("BUILD SUCCESSFUL in 6m 3s"), and
`optional integrations absent` the same in 3.930 min. The server log names no test that passed, but every server game
test below is registered and required, so each passed as one of the 1639. `choose client tests` chose every client
test class (163) in five shards, and each shard succeeded; a failing class fails its shard, so
`ConcordanceRimeStrataClientGameTests` passed in shard 2 and `GlacierHallClientGameTests` in shard 3. No WARN or ERROR
line in the server logs names Rime, Strata, Brace or Rimebind.

Game tests added (all passed in that run):

- `ConcordanceRimeStrataGameTests` (15): `rimeAndStrataTeachTheirInvocations`,
  `snowAndFireObserveRimekeepingInEitherOrder`, `eitherRouteUnderstandsRimekeeping`,
  `aRimeBrewBottledIsTheWorkshopStudy`, `variedColdWorkMastersRimekeeping`, `preserveCountsTheCrucibleBand`,
  `rimebindOnAHostileCreatureIsRestraint`, `strataNeedsNoThallite`, `braceReadsTheGroundItStandsOn`,
  `anAllyARitualAndRootedThalliteAreElective`, `commonBuildingBlocksAreNoSpecimens`, `preserveHoldsOneBandAndExpires`,
  `preservationCannotWrapArbitraryInventories` (09:96), `preserveIsChargedOnceAndCountsOnce`,
  `rimeAndStrataResearchSurviveASave`.
- `ConcordanceEffectGameTests` (+10): `rimebindThawsBeforeItBindsAgain`, `repeatedSlowDoesNotPermanentlyImmobilize`
  (09:96; three casters for 15 s), `onlyTheYetiAndTheTyrantResist`, `braceHoldsTheWholeBlow`,
  `braceIsTakenBeforeTheBlow`, `harmfulPushesSpendTheBraceAndHelpfulOnesDoNot`, `braceDoesNotCancelBoundaryEnforcement`
  (09:96), `rimeAndBraceSurviveASave`, `preserveNeverRemovesABlock`, `preserveIsRefusedWhereItsCasterMayNotBuild`.
- `ConcordanceInvocationGameTests` (+2): `focusIsChargedOnce` (09:96), `braceDoesNotShortenFlashstep`.
- `ConcordanceProgressionGameTests`: `theLiveGraphIsWhole` names the new nodes; new
  `winterAndStoneAskOnlyForFirstLight`, `oneExaminationServesAssayAndStratawrighting`,
  `aRimekeeperReachesAdeptByTheGlacierHall`.
- `ThalliteGameTests`: `thalliteIsAStrataSpecimen`.
- `ConcordancePersistenceGameTests`: `blockEntitiesStampTheirVersion` adds the crucible; new
  `theCrucibleKeepsPreservesHoldThroughASave` (a round trip, saves from before, a forged hold cut).
- `GlacierHallClientGameTests` (CI's client shard; the game-test server has no lair dimension): arriving is the field
  study, the only proof that Fabric's level change fires on a lair teleport; a braced player is still moved by the edge
  toll, the Grey Mist and the whirl, and the toll does not spend the Brace.
- `ConcordanceRimeStrataClientGameTests` (new): the six icons are in the client's resources; screenshots of the spell
  bar, the status HUD and the inventory (Thawing listed), for people to look at. Nothing judges them.

The screenshots of run 38519234934 (480×270 previews in the job log, shard 2):
- `jugcraft_rime_strata_spell_bar`: Spell Engine's spell bar at the bottom left, slots 2 to 6 visible; the first is cut
  off by the screen's left edge (only a sliver of its icon). See the spell bar's place under Rollout.
- `jugcraft_rime_strata_status_hud` and `jugcraft_rime_strata_status_inventory`: five advancement toasts (Cold Against
  Warmth, Rimekeeping, Strata, Read the Stone, Against the Dark) cover the top right, where the HUD draws effect icons
  and the inventory lists effects (JEI had moved its list aside). Neither shot shows Rimebound, Thawing or Braced,
  although the test's check that the client had all three passed, so "Thawing listed" was not shown. The test now
  clears the toasts before each shot (`7d6d0b0d3`, the commit after `06dc5ea5f`).

The screenshots of run 38549876709 (`b012875ab`, units 1 to 4; 480×270 previews in shard 3's log), with the fix:
- `jugcraft_rime_strata_spell_bar`: as before, slots 2 to 6 at the bottom left and the first cut off by the screen's
  left edge, a sliver of its icon showing; the chat says the player reached First Light, Rimekeeping and
  Stratawrighting. No toast is in view.
- `jugcraft_rime_strata_status_hud`: no toasts; at the top right two status icons, Braced's (the cream slab with teal
  corners) above Rimebound's (the blue icy hand), as `braced.png` and `rimebound.png`. Thawing draws none, as its
  status hides its icon. A few grey-brown squares hang in the sky, probably Braced's particles (its colour).
- `jugcraft_rime_strata_status_inventory`: the inventory with JEI's list to its right; between them three icons,
  Braced, Rimebound and Thawing (the blue drop, `rime_thaw.png`), without names, as the inventory draws its effects as
  icons alone where the space is narrow. So "Thawing listed" is shown as its icon. The inventory and JEI's list do not
  overlap.

Run 38534127130's shard 1 (`0e7de6bf1`) showed the same three shots.

Unit 5 (B-09, B-11; passed in CI, run 38591108210 on `25644502a`, shard 3): `ConcordanceRimeStrataClientGameTests`
takes the spell bar again at GUI scale 1 and logs its width (`jugcraft_rime_strata_spell_bar_scale1`; the bar's place
is B-P60). In that run's previews the default-scale bar still has its first slot cut by the screen's left edge; at GUI
scale 1 all six slots stand whole left of the hotbar, each with its key number above it. `_status_hud` shows Braced's
icon above Rimebound's at the top right, as before. The Braced and Rimebound codex entries each gain an "Against the Yeti King" page, and the Rimekeeping entry's
Understood page names the column. `YetiKingGameTests` tests the counters and the column's cold work
([the Yeti King's record](yeti-king.md#verification)).

Not run: any client by hand (the icons, sounds, codex pages, particles), a person looking at the screenshots (the
descriptions above are of the previews), two clients on a dedicated server (Brace on a party member, Rimebind between
players under PvP), and play by hand. `./gradlew build` and `./gradlew runClientGameTest` ran only in CI.

## World and event applicability

No worldgen, creatures, loot or seasons. Snow and ice come from cold biomes and Jugcraft's winter snow, which leave
nothing behind when the season ends; the Glacier Hall route needs a Frost Horn, whose recipe sits under the
`agriculture` switch (with it off, the hall route disappears and the bench remains). Effects obey spawn protection,
protected towns and build permission.

## Rollout and open questions

- New stable ids: research `jugcraft:rimekeeping`, `jugcraft:stratawrighting`; practices `jugcraft:winter_survey`,
  `jugcraft:rimework`, `jugcraft:anchoring` and their evidence keys (saved); advancements
  `jugcraft:concordance_{rimekeeping,stratawrighting}_{encountered,observed,understood,mastered}`; components
  `jugcraft:rimegrip`, `jugcraft:bracing`, `jugcraft:rimehold`; invocations and spells `jugcraft:rimebind`,
  `jugcraft:brace`, `jugcraft:preserve`; effects `jugcraft:rimebound`, `jugcraft:rime_thaw`, `jugcraft:braced`, and the
  attribute modifiers `jugcraft:rimebound` and `jugcraft:braced`; the non-saved attachment `jugcraft:brace_caster`; the
  crucible's field `held_until`; item tags `jugcraft:rime_specimens`, `jugcraft:strata_specimens`; block tag
  `jugcraft:concordance/preservable`; sounds `jugcraft:concordance.rime_gather`, `.rimebind`, `.preserve`,
  `.strata_gather`, `.brace`; codex categories `winter`, `stone`.
- Save compatibility: additive; no migration. **Downgrade:** an older build keeps the unknown research entries and
  relearns no spells from them, and ignores `held_until` (the crucible is simply not held). An older build reads a
  creature's `active_effects` as one list, so an unknown effect id there can drop all of that creature's effects. Their
  attribute modifiers, restored from the saved `attributes`, then stay with no effect to take them off, and milk cannot
  clear them: a creature that was rimebound keeps a fifth off its speed, and one that was braced +0.5 knockback
  resistance, for good. Remove them by command:
  `/attribute <target> minecraft:movement_speed modifier remove jugcraft:rimebound` and
  `/attribute <target> minecraft:knockback_resistance modifier remove jugcraft:braced` (and likewise any other dropped
  effect's modifier). Back up a world before moving it to an older build.
- Sneak-use in the air now examines snow, ice, granite, diorite, andesite, tuff and raw Thallite: a sneaking player
  cannot throw a snowball. One bench study of granite can understand Assay and Stratawrighting at once; Assay becomes
  easier to begin, and its mastery is unchanged.
- Milk clears the Thaw with Rimebound, so a drinker can be bound again at once.
- **The spell bar's nine keys** (the open spell-order question from #250): Spell Engine's spell bar gives keys to its
  first 9 spells only and leaves the rest off it (`SpellHotbar.update`), and Jugcraft offers a player's invocations
  sorted by id, then an inscribed spell. Thirteen invocations exist, so a player who learns more than nine loses the
  last ones: with First Light mastered and Hearthbinding and Rimekeeping understood, Revelation and Rimebind get no key;
  Brace (Stratawrighting) pushes Preserve off too, and Hearthflare on top of that (Hearthbinding mastered) Lanternward;
  an inscribed spell is always last. A later fix orders or caps the container (`ConcordanceSpells.containers`) or
  offers a loadout, once the owner chooses; until then `check_invocations` holds
  [the invocations record](arcane-concordance-invocations.md) to the count.
- **The spell bar's place** (a known limit, seen in CI's previews of runs 38519234934 and 38549876709): Spell Engine
  centres its bar on a point left of the screen's bottom centre (`HudConfig.defaultHotBar`: `Origin.BOTTOM`, offset
  (-170, -11)), 20 GUI pixels a slot, so its left edge is at width/2 - 170 - 10 × spells. CI's window is 427 GUI pixels
  wide, so a bar of 5 or more spells runs off the left edge (the shot's first slot is cut off). At 480 (1920×1080 at the
  automatic GUI scale) a bar of 7 reaches the edge exactly and 8 or more run off it. Thirteen invocations exist, and the
  bar keys up to 9. A player can move the bar in Spell Engine's HUD settings; Jugcraft changes no configuration. The
  question is B-P60 ([the contract's decisions](concordance-chunk-b.md#decisions)).
- For the owner's checkpoint: a Brace lasting out its tick (B-P30); the icon choices (B-P31) and the soaked icon's
  folder label; the knockback measure (B-P32); Rimebind's targets following the shared boundary (B-P34); "Stonebrace" as
  a display name instead of "Brace", to keep it apart from the war fork's trait; and the spell bar's place (B-P60):
  whether Jugcraft should set a default that keeps a full bar on screen, or leave it to each player's Spell Engine
  settings.
- For unit 4, answered there ([the winter record](arcane-concordance-winter.md)): B-P24's 120 s hold from a focus worn
  with the Initiate's Wand exceeds that wand's duration limit (1200 ticks), and unit 4 records why a worn focus may
  (B-P50: the hold is the crucible's state, cut at 2400); its key `frost_boon` is an elective (B-P53; since unit 5 the
  key is `elective`, which the Yeti King's column shares, B-P56), and it adds no brew key.
- Later: Strata's civic project and boss opening; words a player can compose for Rime and Strata (B-P6); Tempest. (The
  Yeti column elective came with B-09 in unit 5.)
