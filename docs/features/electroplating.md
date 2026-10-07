# Electroplating: the electroplating bath

Status: implemented on `feature/electroplating-34` (batch 34), stacked on `feature/hydroponics-33`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, asked for hydroponics, electroplating and hydrogen/ammonia storage next, each rethought for usefulness.
Owner: jimbozoomer-byte
Target milestone and tier: steel tier, after the electrolytic cell and sulfuric acid (chemistry batches 21 and 22).
Primary specialty and supported player role: gear upkeep for fighters and explorers.

## What was rethought
- **Not a decorative "shiny" variant.** A plating that only changes the look would go unused. Each metal does one thing.
- **A repair station.** Every plating repairs the item fully without experience and without the anvil's "too expensive" limit, so enchanted gear can keep going. That is the everyday use.
- **A use for nickel and silver.** Both ores exist but had few sinks. Gold gains a use beyond golden gear.

## Player experience
- **Electroplating Bath** (one block, 32 JE/t, 4,000 mB sulfuric acid tank):
  - Put a tool, weapon or piece of armor (anything with durability) in the first slot and an ingot in the second, and pipe in sulfuric acid.
  - After 10 seconds it uses the ingot and 100 mB of acid, and the plated item, fully repaired, goes to the output.
  - Upgrades speed it up as for other processors. Results eject to its configured sides.
- **What each metal does** (shown in the item's tooltip):

  | Metal | Effect |
  | --- | --- |
  | Nickel | Maximum durability ×1.5 |
  | Silver | Swords and axes get Smite III (raised to III if lower; higher Smite is kept) |
  | Gold | Armor counts as gold for piglins, so one gold-plated piece keeps them calm |

- An item holds one plating. Plating it again with the **same** metal only repairs it (the bonus is not stacked). A **different** metal is refused, and nothing is used.
- Advancement **Silver Lining** (build an electroplating bath). Handbook page in the Chemistry chapter.

## Connections
- Input producer: sulfuric acid (chemical reactor), nickel, silver and gold ingots, power.
- Output consumer: the player's own gear.
- Technology connection: fluid pipes, item logistics, machine upgrades. Magic connection: none.
- Required vs optional: optional.

## Balance and automation
- Per plating: one ingot, 100 mB of acid and 6,400 JE.
- Repair costs an ingot each time, about what an anvil repair with material costs, but without experience or the cost cap.
- Nickel's ×1.5 applies once: replating does not stack it. Silver only raises Smite to III. No output can be turned back into ingots, acid or power, so there is no loop.

## Multiplayer and persistence
Server-side machine logic, saved like other processors. The plating is a saved, synced item component (`jugcraft:plating`). The piglin check is a server-side mixin on `PiglinAi.isWearingSafeArmor`. The tooltip is client-only.

## Dependencies and assets
No new dependencies. One mixin (`PiglinSafeArmorMixin`). Model (dieselpunk: an open acid tub with nickel and silver anode plates on copper busbars, a sword hanging between them and a rectifier box with a gauge), classic front texture. All original.

## Verification
- `tools/check_mod_data.py`: the numbers in `Electroplating.java` match `tools/electroplating.py`; each metal and its tooltip exist.
- Game test `electroplatingPlatesAndRepairs` (CI):
  - nickel repairs a worn iron sword and makes its maximum durability 1.5 times as high;
  - silver gives a sword Smite III;
  - a nickel-plated sword refuses gold and keeps its inputs;
  - a bath without acid does nothing;
  - the bath refuses dirt to plate and iron to plate with;
  - gold-plated armor on an armor stand counts as gold for piglins; plain iron armor does not.
- Not run: client play, the tooltip in game, live piglins.

## World and event applicability
Not applicable.

## Rollout and open questions
- More metals (chrome for armor toughness, copper) are possible follow-ups.
- Values are first values to tune.
