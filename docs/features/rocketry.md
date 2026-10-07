# Rocketry: propellant chemistry, the rocket workshop and the first rockets

Status: batch 38 is implemented on `feature/rocketry-38`, stacked on `feature/control-room-37`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, asked for basic rocketry with uses other than space launches, holding off on space "til we have more of the terrestrial stuff".
Owner: jimbozoomer-byte
Target milestone and tier: electronics tier (processors, tungsten, steel), after ammonia chemistry.
Primary specialty and supported player role: exploration (prospecting), farming (rain), group play (signals) and night safety (flares).

## Player experience
- **Chemistry** (chemical reactor):
  - **Ammonium perchlorate**: a salt and 250 mB of ammonia make two. The perchlorate step is folded into the reactor.
  - **Iodine**: eight dried kelp and 100 mB of sulfuric acid make one. Iodine was first found in, and long made from, kelp ash.
  - **Silver iodide** (crafted from silver dust and iodine, two): the cloud-seeding agent.
- **Rocket Workshop** (one block, 48 JE/t): up to three ingredients in any slots, like the circuit assembler.

  | Output | Ingredients |
  | --- | --- |
  | 2 solid propellant | 2 ammonium perchlorate, 3 aluminum nuggets, 1 rubber |
  | Rocket casing | 3 steel plates |
  | 2 rocket nozzles | 1 tungsten ingot, 1 steel plate |
  | Guidance unit | 1 processor, 2 microchips, 2 copper wire |
  | Rocket motor | casing, nozzle, 2 solid propellant |
  | Survey rocket | motor, guidance unit, sensor |
  | Cloud-seeding rocket | motor, 2 silver iodide |
  | Clear-sky rocket | motor, 2 guncotton |
  | 4 signal flares | solid propellant, 2 paper, red dye |
  | 4 illumination flares | solid propellant, 2 paper, glowstone dust |

- **Firing** (right-click; open sky above the player needed): the rocket rises as a firework and does its work 2 seconds later, 30 blocks up.
  - **Survey rocket**: surveys the ores and oil under 7x7 chunks (the prospector does 3x3), sampling every fourth column. It shows the prospector's screen. Chunks that aren't loaded are skipped, never generated.
  - **Cloud-seeding rocket**: five minutes of rain, which helps fields and fills cauldrons.
  - **Clear-sky rocket**: five minutes of clear sky.
  - The weather rockets share one two-minute cooldown for the whole server, so they can't be spammed. They only work in dimensions with weather.
  - **Signal flare**: a red star burst. Every player within 512 blocks is told who fired it and where.
  - **Illumination flare**: a white burst; hostile mobs within 48 blocks glow for 30 seconds.
- Advancements **Rocket Science** (build a rocket workshop) and **Eye in the Sky** (assemble a survey rocket). Handbook: a new Rocketry chapter (fuel, the workshop, rockets).

## Connections
- Input producer: ammonia (synthesis converter), salt, kelp, sulfuric acid, aluminum, rubber (polymers), tungsten, steel, processors and microchips, sensors (control electronics), guncotton (field chemistry), silver.
- Output consumer: prospecting, farming (rain), multiplayer signalling, night defence. Later: the rocket post, line-throwing rockets, launchers and booster rails; space launches when the owner decides.
- Technology connection: the prospector's survey and screen, vanilla fireworks for flight. Magic connection: none.
- Required vs optional: optional.

## Balance and automation
- A survey rocket costs about a processor, a tungsten ingot, three steel plates and a sensor. It is consumed, so it is worth it only for a wide look, and gives the same vague 1-5 signals as the prospector.
- Weather control is shared and rate-limited.
- Nothing produced converts back into its inputs. No positive-gain loop.

## Multiplayer and persistence
- Rockets in flight are not saved; one in the air when the server stops is lost, like a firework.
- The weather cooldown is per server session.
- Flares never break blocks. The signal burst explodes 30 blocks up, so its firework damage reaches no one on the ground.

## Dependencies and assets
- No new dependencies.
- All 13 item icons are 64x64 high-detail art drawn with `tools/hd_art.py` in `tools/rocketry.py`.
- The workshop model is dieselpunk: a bench with a rocket in a cradle, a press arm and a welding lamp.
- Flight uses the vanilla firework entity and sound.

## Verification
- `tools/check_mod_data.py`: the numbers in `JugcraftRocketry` match `tools/rocketry.py`, and every item is registered. The recipes pass the usual audits.
- Game test `rocketryWorks` (CI):
  - the workshop assembles a rocket motor;
  - an illumination burst makes a zombie glow but not a pig;
  - the survey rocket's 7x7-chunk survey finds a tin ore placed on a sampled column.
- Not tested in CI:
  - weather rockets (changing the weather would disturb other tests running at the same time, such as solar output);
  - signal messages;
  - firing by hand in play.

## World and event applicability
Not applicable.

## Rollout and open questions
- Next rocketry batches: the rocket post (batch 39, [rocket-post.md](rocket-post.md); deliveries to unloaded areas wait, nothing is force-loaded), line-throwing rockets with ziplines, the rocket launcher (damage only), booster rails, and liquid fuels (kerosene, liquid oxygen).
- Space launches stay on hold until the owner decides.
