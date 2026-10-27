# Costumed villains: unmasking

Status: implemented (pending CI and review).
Proposal issue: none. The four costumed villains reached main through #239 ([scary-creatures.md](scary-creatures.md)), with unmasking left as the follow-up: "a beaten spook reveals a villager crook, drops its costume as a trophy". On 26 October 2026 the owner asked for the suggested items in order ("then do 2-9 in order"); the seventh was "costumed villains unmasking".

**This change's choices:** the crooks' names and excuses, how long a crook stands caught, and the masks as trophies, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: the Halloween hunt, any tier
Primary specialty and supported player role: hunting and exploration

## Player experience

When a player beats one of the four costumed villains, the costume comes off:
- **The crook.** Where the villain fell stands the person inside, a townsperson in one of the town's five everyday skins, named for who they turned out to be. Everyone within 32 blocks reads "Unmasked! The Spectral Cosmonaut was Old Dr. Pemberton, the observatory keeper." and the crook's excuse. A villager's grumble plays.
- **Caught.** The crook stands looking at whoever is near for 10 seconds (200 ticks), then is led away in a puff of smoke ("… is led away."). Nothing can hurt them in the meantime; only `/kill` removes one early.
- **The trophy.** The villain's mask drops (when mobs drop loot): the **Spectral Cosmonaut's Helmet**, the **Drowned Diver's Helmet**, the **Emerald Phantom's Mask** or the **Onyx Warden's Helm**. Each says whose it was ("Pulled off Silas Brine, the harbourmaster"). They are trophies to keep or show in an item frame; they cannot be worn.
- **Mystery Solved:** the player who beat it earns the advancement.

A villain that dies any other way (a fall, lava, another mob) keeps its secret: no crook, no mask.

Each villain may turn out to be one of three crooks:

| Villain | Crooks |
| --- | --- |
| Spectral Cosmonaut | Old Dr. Pemberton, the observatory keeper; Mortimer Gale, the rocket-fair barker; Hollis Crane, the moon-rock dealer |
| Drowned Diver | Silas Brine, the harbourmaster; Wendell Marsh, the bait-shop owner; Captain Abel Fenwick, retired |
| Emerald Phantom | Agatha Thornby, the tour guide; Rufus Pike, the antique dealer; Cornelius Vane, the stage magician |
| Onyx Warden | Gerald Ashby, the museum curator; Basil Crumb, the castle groundskeeper; Lady Mildred Hartwell |

The names and excuses are original, in the spirit of the cartoons the villains are fan homages to; none is quoted from them.

## Connections

- **Input:** beating a costumed villain (they spawn at night in the Halloween season, or from spawn eggs).
- **Output:** a trophy and an advancement. The villains' existing drops are unchanged.
- **Optional:** yes. Nothing needs a mask.

## Balance

- The masks are trophies only: no armour, no effect, no recipe uses. They stack to one.
- The villains' own loot (aluminium, brass, steel, scuba and steel gear) is unchanged.
- Only a player's kill unmasks, so a mob farm that kills by other means makes no masks. Crooks leave after 10 seconds, so they never pile up.

## Multiplayer and persistence

- All of it is the server's: the death hook, the crook and its timer. The client only draws the crook with the skin the server names.
- The crook saves how long it has left and its skin, so one caught across a restart is still led away on time.
- New registrations, stable from now on: the entity `jugcraft:unmasked_crook`; the items `spectral_cosmonaut_helmet`, `drowned_diver_helmet`, `emerald_phantom_mask` and `onyx_warden_helm`; the advancement `jugcraft:mystery_solved`.

## Dependencies and assets

- No new dependencies. All art is original, and nothing is copied.
  - The crook reuses the townsfolk's player-shaped model and five everyday skins (`TownsfolkModel`, `textures/entity/townsfolk/townsfolk_1` to `_5`).
  - The four mask icons are 16×16 maps in `tools/item_icons/` ([ITEM_ICONS.md](../ITEM_ICONS.md)).
  - The sound is vanilla's villager "no".
- `tools/unmasking.py` holds the crooks, the masks, the lang and the advancement; `tools/check_mod_data.py` checks the Java against it.
- Two generated tables gain the four masks by hand, since this machine has no Minecraft jar or game to rebuild them:
  - `data/jugcraft/alchemy/values.json`: every existing record is kept; the masks take the labelled one-essence fallback.
  - `docs/audits/survival-item-registry.json`: the four new item IDs. The snapshot game test's next export will confirm them.

## Verification

Server game tests (`UnmaskingGameTests`):

| Test | What it checks |
| --- | --- |
| `theSpectralCosmonautIsUnmasked`, `theDrownedDiverIsUnmasked`, `theEmeraldPhantomIsUnmasked`, `theOnyxWardenIsUnmasked` | Beaten by a player, each villain leaves one named crook and drops its mask once, with the crook's name on it. |
| `aVillainThatDiesOtherwiseKeepsItsSecret` | A villain killed by generic damage leaves no crook and no mask. |
| `theCrookCannotBeHurtAndIsLedAway` | A player's blow does not hurt the crook; it still stands at half time and is gone after its 200 ticks. |

### Results

Local, without Gradle or a game: the generator, the data checks, the Python unit tests and the Java syntax parse; see the PR. I reviewed the four icons at 8×.

CI: see the PR.

Not run: a player unmasking a villain in a real client, and a two-player dedicated server.

## World and event applicability

Wherever the villains are: they spawn naturally only in the Halloween season ([scary-creatures.md](scary-creatures.md)). Unmasking itself works all year, for villains from spawn eggs too.

## Rollout and open questions

- The crooks' names and excuses, the 10 seconds and the trophies await the owner's review.
- Still ideas, not built: traps (nets, a fake trapdoor), and village mysteries where clues point to the crook before the chase.

## History

Before this, a beaten villain simply died and dropped its salvage.
