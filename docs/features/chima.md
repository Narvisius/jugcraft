# Chima, the lion cub

Status: implemented on the contributor's fork; CI green; pending review.
Proposal issue: none; requested by the contributor (mmme123-spec) with their own drawings of Chima and its four looks.
Owner: jimbozoomer-byte
Target milestone and tier: none; an early-game wild animal and pet, reachable from the first day.
Primary specialty and supported player role: creatures and pets; anyone who keeps animals.

## Player experience

**Chima** is a lion cub drawn from its designer's sketches. It is nearly all head:
- a great round head with a spiky orange mane all round it, the face on its front: big white eyes and a little cat's mouth;
- a small yellow body on four stubby legs;
- a thin dark tail curling up to a spiky orange star.

**In the wild** it lives on savannas, one or two at a time, on grass in daylight like the other animals there. A wild Chima hunts cows:
- it picks out the nearest one it can see, roars ("rawr!") and runs it down;
- after a kill it rests from hunting for two minutes.

**Taming it is like taming a cat:**
- beef (raw or cooked) tempts it, and each piece fed to a wild one tames it one time in three;
- a tamed Chima is its feeder's;
  - it sits where it was tamed and gets a red collar;
  - its owner's empty hand has it sit or get up;
  - it follows its owner, teleporting to them from 16 blocks away;
  - it defends its owner from whatever hurts them;
- a tame one no longer hunts cows, ignores its owner's blows, and beef heals it.

**Its looks.** What it is given changes its look. The model stays the same; only the texture changes:

| Given | Look |
| --- | --- |
| a jack o'lantern | **pumpkin**: a ribbed pumpkin head with glowing angry eyes and a stem, a little jack o'lantern on its tail |
| rotten flesh | **zombie**: stitched together, green on one side and pink on the other; one angry red-rimmed eye and one hollow; a bloody fang; a grubby body with a pink patch and paws; a stitched ball on its tail |
| a bone | **skeleton**: a round skull with big black eyes, bare ribs and spine, a tail of vertebrae |
| any fish | **shark**: a blue shark's head with a toothy pink mouth, side eyes, gills and a fin; blue in front and sandy behind; a pink star on its tail |

Rules for changing looks:
- the item is used up;
- anyone can change a wild one's look, but only its owner can change a tame one's;
- its owner, sneaking with an empty hand, gives it its own look back.

## Connections

- **Existing input producers:** cows (beef, and its prey); jack o'lanterns, rotten flesh, bones and fish for its looks.
- **Existing output consumers:** none. It is a companion: it guards its owner and makes a look-changing pet.
- **Technology connection:** none.
- **Magic connection:** none.
- **Reachable entry path:** savannas, with beef from any cow; or its spawn egg in creative.
- **Required vs optional connections:** everything is optional. Beef can be traded or farmed.
- **For a pet, why resource links do not apply:** it is a companion and a cosmetic, not a production step.

## Balance and automation

Its numbers are reasonable picks, flagged here for the owner's review:

| Stat | Value | Compared with |
| --- | --- | --- |
| health | 16 | |
| damage | 4 | a cow (10 health) falls in three bites |
| speed | 0.3 | |
| tempt range | 10 blocks | |
| taming | one in 3 per beef | a cat's one in 3 per fish |
| rest after a kill | 2,400 ticks | |
| beef heal | 4 health | |
| follow | starts at 7 blocks, stops at 2.5 | |
| teleport | 16 blocks | |
| spawn weight | 6, groups of 1–2 | in `c:is_savanna` |

Other balance notes:
- It drops nothing, so hunting gives the player no loot.
- The rest after a kill keeps a wild one from emptying a herd.

## Multiplayer and persistence

- **Server authority.** Taming, ownership, sitting and looks are decided on the server, in `Chima.mobInteract`.
- **Ownership.**
  - A tame one's look and sitting are its owner's to change.
  - Its owner's attacks don't hurt it.
- **Saved data.** A tame one never despawns. It saves:
  - `tame` and `owner`;
  - `sitting`;
  - `rest`;
  - `skin` (its look, by name).
- **Unknown looks.** An unknown saved look falls back to its own.
- **Stable IDs:**
  - entity `jugcraft:chima`;
  - item `jugcraft:chima_spawn_egg`;
  - tags `jugcraft:chima_food` and `jugcraft:chima_skins/<look>`.

## Dependencies and assets

There are no new dependencies.

**Art.** The model and textures were made for Jugcraft from the contributor's own drawings:
- the model is `client/chima/ChimaModel.java`;
- the textures come from `tools/chima_textures.py`: five looks, wild and tame, at 256×128.

**Sounds** are vanilla's polar bear sounds, pitched up for a cub.

## Verification

**Server game tests** (`ChimaGameTests`):
- a wild one hunts down a penned cow, then rests;
- beef tames it to its feeder, eating every piece; its owner's empty hand toggles sitting, a stranger's does not;
- each look's item gives that look and is used up; a stranger can't change a tame one's look, its owner can; its owner sneaking with an empty hand restores its own;
- a tame one leaves a penned cow alone for 240 ticks, ignores its owner's blows, and is healed by beef;
- the tags and spawn egg load.

**Client game test** (`ChimaClientGameTests`): screenshots of the five looks and a tame one sitting, seen from the front, from behind and up close.

**Results:** fork CI run [38494479324](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38494479324) on 7a46abac passed: the build, the data checks, every server game test (these five included) with and without optional integrations, and all five client test shards (these screenshots included). The first run failed to compile one test (`EntityType.COW` is `EntityTypes.COW` in 26.3); that was fixed.

**Not yet done:**
- a two-client playtest;
- a human gameplay check of hunting, taming and following.

## World and event applicability

It is a savanna animal, so no cave, boss or dimension rules apply.

The pumpkin and zombie looks suit Halloween, but they are not seasonal: the items work all year, and a look stays after any event ends.

## Rollout and open questions

**For the owner's review:**
- **The name.** "Chima" and the design are the contributor's own. If the name alludes to LEGO's *Legends of Chima*, it is a fan homage, which needs the owner's approval. The art is original either way.
- **The balance numbers above.**

**Known limits:**
- Looks are textures on one model. The pumpkin and skull are rounded boxes, not spheres.
- It has no breeding and no drops.


## Integration with current main — 20 October 2026

The owner selected Chima and underground drilling for a combined merge. Tamed attack damage,
including retaliation, now asks the shared owner claim/PvP rules and cannot harm the owner.
Follow teleports reject unloaded, wet or out-of-border positions and sitting pets stay put.
The combined underground inspection saves and reopens a tamed shark-look Chima beside the rig,
checking identity, ownership, sitting, health and appearance alongside the drill's state.
PNG encoding is made reproducible; all eleven regenerated images were verified pixel-identical
with the contributor's original assets. Integration/repair author: OpenAI Codex (GPT-6).
See [combined evidence](drilling-chima-integration-oct20.md) for current validation.
