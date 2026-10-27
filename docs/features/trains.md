# Trains

Status: implemented (pending CI and review).
Proposal issue: none. Earlier the owner said: "I don't want to do the train we would need to make a framework for the entire train I do want to later at some point... make a whole train setup and framework". On 26 October 2026 they asked for the suggested items in order ("then do 2-9 in order"); the sixth was "trains framework (then the armoured train)". This is the framework. The armoured train is the next step ([raiders.md](raiders.md), idea 4).

**This change's choices:** the speeds, fuel use, car sizes, recipes and the three vehicles below, listed for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: the petrochemical tier (diesel engines, steel plates and gears), beside the Landship
Primary specialty and supported player role: transport and logistics

## Player experience

Three vehicles and a tool, all running on **any rails**: plain, powered, detector and activator rails, and other mods' rails, round curves and up slopes as Minecraft lays them.

| Item | What it is |
| --- | --- |
| **Diesel Locomotive** | 4.5 blocks long. An olive hood with a brass band, a headlamp and an exhaust stack, and an open-windowed cab at the back for two. |
| **Passenger Coach** | 4 blocks long. A red coach with open windows and two facing benches; seats four. |
| **Cargo Wagon** | 3.5 blocks long. A plank goods van with sliding doors and a corrugated roof; 27 slots, like a chest. |
| **Coupler** | Couples cars into a train, or uncouples them. |

| Action | How |
| --- | --- |
| Set on the line | Use the item on a rail: it stands there facing along the rail the way you face. The line must be clear of other trains for its length. |
| Ride | Use the locomotive or a coach. The locomotive's first rider drives. |
| Drive | Forwards notches the throttle up, to four; back notches it down through idle into two notches of reverse; jump brakes (and sets idle). The cab shows the throttle, the speed and the fuel. |
| Couple | Set a car on the line just behind the train's last car (within 2.5 blocks of where it belongs) and use the Coupler on it. It is drawn up to the coupling distance. Up to eight cars behind one locomotive. |
| Uncouple | Sneak and use the Coupler on a car: it and every car behind it are uncoupled. On the locomotive: every car. |
| Load goods | Use the Cargo Wagon, anywhere along the line. |
| Refuel | Use a diesel, premium diesel or RP-1 kerosene bucket on the locomotive (+1000 mB, 8000 mB tank). |
| Pick up | Hit it until it drops (40 damage; the locomotive 80). In creative it just goes. A wagon's goods spill. |

- **Speed:** each notch is 0.08 blocks a tick (1.6 a second), so full throttle is 6.4 blocks a second, in reverse up to 3.2. The train gathers 0.004 blocks a tick each tick, coasts down at 0.002 with the throttle at idle, and brakes at 0.02. Reversing brakes to a stand first.
- **The whole train moves as one.** The locomotive moves every coupled car the same distance along its own stretch of line, so cars keep their spacing round curves. It pulls or pushes them.
- **Stopping:** the train stops at the end of the line, or where its leading end would run into another train's car. The throttle goes to idle and the driver is told.
- **Fuel:** each notch burns 2 mB a second, so a full tank lasts about 17 minutes at full throttle. With an empty tank the train coasts to a stand.
- **Breaking the rail** under a car's middle knocks the car off as an item, with its goods, and uncouples it and the cars behind it.
- **Curves:** a car stands between two points 0.6 blocks in from its ends, so it leans into curves and up slopes. Vanilla's one-block corners are tight for a 4-block car: its ends cut across the corner.

## Connections

- **Input producers:** steel plates and gears, the Diesel Engine, glass panes, wool, a chest, tripwire hooks and minecarts. Fuel comes from the refinery.
- **Output consumer:** travel and carrying goods.
- **Recipes** (shaped, behind the `machines` feature):
  - Diesel Locomotive: `PGP / PEP / WMW`, where P is a steel plate, G a glass pane, E a Diesel Engine, W a steel gear and M a minecart.
  - Passenger Coach: `PGP / PWP / _M_`, where W is any wool.
  - Cargo Wagon: `PCP / _M_`, where C is a chest.
  - Coupler: `_H / P_`, where H is a tripwire hook.
- **Optional:** yes. Nothing else needs a train.
- The Engineer's Handbook has a Railways chapter: "Trains" and "Coupling and fuel".

## Balance

- Fuel is only used up, never made. A train carries goods but makes nothing.
- There is no damage: a train stops at another train, and runs through creatures and players without hurting them.

## Multiplayer and persistence

- **Server authority:** the server moves the train. The client only sends the driver's keys (`TrainInputPayload`: throttle -1, 0 or 1, and brake), when they change and every 5 ticks. The server ignores anyone but the driver and stops listening if no keys arrive for 10 ticks.
- **Saving:** each car saves its place on the line (rail, direction, distance along), its locomotive and its damage; the locomotive saves its cars in order, its speed, fuel and throttle; the wagon saves its goods.
- **Unloaded cars:** if a coupled car is not loaded, the train waits for it. After 10 seconds it leaves it, and the cars behind it, uncoupled where they stand.
- **Rendering:** the cars' positions and rotations are synced as for any entity and smoothed between ticks. The wheels turn by how far the car moved, worked out on each client, so there is no extra networking.
- New registrations, stable from now on: the entities `jugcraft:locomotive`, `passenger_car` and `cargo_wagon`; the items `locomotive`, `passenger_car`, `cargo_wagon` and `coupler`; the payload `jugcraft:train_input`.

## Dependencies and assets

- No new dependencies. All art is original, and nothing is copied.
  - The models are boxes in `tools/trains.py`, exported as quads to `assets/jugcraft/train_quads.json` (the locomotive, coach and wagon bodies and one wheel). They reuse existing textures: dieselpunk olive, hazard, lamp, exhaust and grille; the giants' steel, band, pulley and red paint; Kaiserworks brass; bunker corrugated steel; and aged pine, aged maple and cedar planks.
  - `client/TrainRenderer` draws the body and turns a pair of wheels at each axle.
  - The four icons are 16×16 maps in `tools/item_icons/` ([ITEM_ICONS.md](../ITEM_ICONS.md)).
  - The sounds are vanilla's: minecart riding, bucket and chain.
- `tools/check_mod_data.py` checks that the Java numbers (sizes, seats, speeds, fuel, coupling, the exhaust, the wheels and axles) match `tools/trains.py`.
- Two generated tables gain the four new items by hand, since this machine has no Minecraft jar or game to rebuild them:
  - `data/jugcraft/alchemy/values.json`: every existing record is kept. The new items get the builder's own rules applied to their recipes.
  - `docs/audits/survival-item-registry.json`: the four new item IDs. The snapshot game test's next export will confirm them.

## Verification

Server game tests (`TrainGameTests`, on a line of rails laid in the empty 48 × 48 `tatterlace_arena` test structure):

| Test | What it checks |
| --- | --- |
| `aPlaceOnTheLineFollowsCurvesAndEndsWithTheLine` | A place on the line moves four blocks along a straight line; round a corner it heads east; it stops at the end of the line, forwards and backwards, and says how far it went. |
| `aLocomotivePullsItsCoupledWagon` | At notch 2 the locomotive runs more than 3 blocks in 3 seconds, burns fuel, and its coupled wagon keeps exactly the coupling distance behind. |
| `aTrainStopsAtTheEndOfTheLine` | At full throttle its front stops at the end of the line, standing at idle. |
| `aTrainStopsShortOfAnotherTrain` | Running at a standing wagon, it stops with its front within half a block short of the wagon's end, and the wagon does not move. |
| `aCarCouplesOnlyJustBehindTheTrain` | A coach far down the line does not couple; one just behind does, at the coupling distance; it cannot couple twice; uncoupling frees it. |
| `breakingTheRailKnocksTheCarOffWithItsGoods` | Breaking the rail under a cargo wagon drops the wagon and its diamonds. |
| `theDriverWorksTheThrottleAndBrake` | Holding forwards notches the throttle up to four; braking brings the train to a stand at idle. |
| `aTrainKeepsItsPlacesAndGoodsThroughASave` | The locomotive's place, cars and fuel, and the wagon's place, coupling and goods, survive a save and load. |

Client game test (`TrainClientGameTests`): a locomotive, a coach and a wagon coupled on rails over grass, photographed from the side (`jugcraft_train_side`), the front (`jugcraft_train_front`) and behind the wagon (`jugcraft_train_rear`). It fails if the coach and wagon do not both couple.

### Results

Local, without Gradle or a game: the generator, the data checks, the Python unit tests and the Java syntax parse; see the PR. I reviewed offline renders of the three models and the icons.

CI: see the PR.

Not run: driving a train in a real client, and a two-player dedicated server.

## World and event applicability

Not applicable: trains are crafted and placed by players only.

## Rollout and open questions

- The speeds, fuel use, sizes and recipes await the owner's review.
- **Next:** the armoured train ([raiders.md](raiders.md), idea 4), built on this framework: an armoured locomotive and a turret car reusing the Landship's cannon.
- Later, if wanted: signals and stations, a station timetable, a goods loader that fills wagons from chests, longer curved track pieces, and collisions with creatures.

## History

Before this there were no trains. The raiders record's next idea (4) was an armoured train, which the owner put off until there was "a framework for the entire train".
