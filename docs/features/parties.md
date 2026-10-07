# Parties

Status: implemented. Commands and API from @Narvisius's prototype (#31, merged through #84); the Party screen, clickable invites, `/party admin` and configurable limits finish it (pull request pending). Not yet play-tested with two clients.
Proposal issue: #21. Built as a community prototype.
Owner: @Narvisius
Target milestone and tier: Discovery (shared infrastructure, no crafting)
Primary specialty and supported player role: every player; cooperation between specialties

## Player experience
Team up with friends using `/party`. Parties are the shared "who is working together" rule. Later features use it: blueprints shared with your party (#23), drone depots that help party members' builds, and future machine access, companions and PvP consent.

| Command | Who | Does |
| --- | --- | --- |
| `/party` or `/party info` | anyone | Shows your party (members, leader ★, who is offline) or your pending invites |
| `/party create` | anyone not in a party | Starts a party with you as leader |
| `/party invite <player>` | leader, or anyone not in a party | Invites an online player. Creates your party if you have none. Invites expire after 5 minutes |
| `/party accept` / `/party decline` | invited player | Answers the most recent invite |
| `/party leave` | member | Leaves. A leaving leader hands over to the longest-standing member; the last member leaving ends the party |
| `/party kick <name>` | leader | Removes a member (works for offline members, by last known name) |
| `/party leader <name>` | leader | Makes another member the leader |
| `/party disband` | leader | Ends the party |
| `/party admin list` | operator (level 2) | Lists every party with its members and leader |
| `/party admin kick <name>` | operator | Removes that player from whatever party they are in |
| `/party admin leader <name>` | operator | Makes that player the leader of their party |
| `/party admin disband <name>` | operator | Ends the party that player is in |

Everyone in the party is told about joins, departures, kicks and leader changes. Operator actions work while parties are turned off, so an operator can tidy saved parties.

**Invites in chat** end with clickable **[Accept]** and **[Decline]**, which run the same commands.

**Party screen** (the Party key, **P** by default, under the Jugcraft controls category), in the red-and-graphite look of the Drone Tower and Blueprint screens:
- Members in join order, with a green or grey light for online or offline, the leader marked, and "(you)".
- The leader gets LEAD and KICK beside every other member, and DISBAND; a member gets LEAVE.
- A name box and INVITE (for the leader, or anyone not in a party).
- The latest invite with ACCEPT and DECLINE (and how many more are waiting).

Every button runs the ordinary `/party` command, so the server checks it exactly as if it were typed. The server sends the screen's contents when the player joins, when they open it, when their party changes and when they get or answer an invite.

## Connections
- Existing input producer: none. This is infrastructure; no resources are involved.
- Existing output consumer: none on `main` yet. First planned users are the Blueprint System (#23) and the Drone Depot.
- Technology connection: automated systems ask `JugcraftParties.mayServe(...)`.
- Magic connection: none yet; future bound servants and companions can use the same rule.
- Reachable entry path: available from the start; no items or research.
- Which connections are required vs optional; trade and solo routes: nothing requires a party. A player without a party is a party of one, and everything works solo.
- How this specialty stays useful without mastering every other branch: not a specialty; it lets specialists cooperate.
- For infrastructure/cosmetics, supported systems and reason resource links do not apply: this is a shared API that keeps every feature from inventing its own team system (`MACHINE_ROADMAP.md` lists ownership and access as missing).

## Shared API (`party/`)
Call the static methods on `JugcraftParties`. Don't reach into `PartyManager` from other features.

| Method | Meaning |
| --- | --- |
| `sameParty(a, b)` | Same party (a player is always "same party" as themselves) |
| `isLeader(player)`, `partyMembers(player)`, `partyId(player)` | Role, members in join order (just the player when solo), party id |
| `mayServe(systemOwner, systemMode, jobOwner, jobMode)` | **The one Personal/Party rule for all automation.** A system always serves its owner's jobs. It serves another player's job only when the system and the job are both `UseMode.PARTY` and the owners share a party |
| `addListener(listener)` | Called once per change with the affected players, so features can drop cached jobs immediately |

`UseMode` is `PERSONAL` or `PARTY`. Every automated system that offers a Personal/Party switch uses this enum and `mayServe`.

## Balance and automation
- No items, currencies or stat bonuses. Parties only decide who is trusted to work together.
- Limits, set in `config/jugcraft.properties` (read when the server starts):

  | Setting | Default | Allowed |
  | --- | --- | --- |
  | `parties.max_size` | 8 | 2 to 64 |
  | `parties.invite_minutes` | 5 | 1 to 60 |
  | `parties.invites_per_minute` | 10 | 1 to 60 |

  A value that isn't a number in range is logged and the default used. Lowering the size never removes anyone: a party over it just can't take new members.
- Only the leader can invite once a party exists.

## Multiplayer and persistence
- **Server-authoritative.** Clients send commands only; `PartyManager` checks every rule. The screen's state message (`PartyStatePayload`) carries only names and flags for the receiving player's own party and invites. Requests for it are answered at most every 5 ticks per player.
- **Lookups are map reads.** Nothing scans players or the world.
- **Saving:** `<world>/jugcraft/parties.txt`, a small versioned text file.
  - Saved every 30 seconds when something changed, and when the server stops.
  - Writes go to a temporary file that is then moved into place.
  - Invites are not saved; they expire within minutes.
- **Damaged or newer-format file:** the server logs an error, starts with no parties and leaves the file untouched for an operator. Saves then go to `parties.txt.recovered`.
- **Hand-edited file:** a player listed in two parties is kept in the first only, and a missing leader is repaired.
- **Disable:** `parties.enabled=false` in `config/jugcraft.properties` makes everyone a party of one and refuses party commands. Saved parties are kept and come back when it is turned on again.
- **Offline members** stay members.

## Dependencies and assets
Fabric API only (command API v2, lifecycle events, networking, key bindings). No textures or models; the screen is drawn with fills and text. Messages are in the generated language file (`tools/party.py`). The checker verifies every `PartyManager.Result` has a message.

## Verification
Actually run for this PR (30 September 2026):
- `python3 tools/generate_material_data.py`: generated files up to date.
- `python3 tools/check_mod_data.py`: PASS. A negative test (removing one party message) was reported as expected.
- `python scripts/check_repository.py`: PASS.
- **Party logic run in plain Java (JDK 21):** 78 checks passed, 0 failed. They cover the lifecycle, the size cap, leader hand-over, kicks, expiry, the rate limit, decline, every `mayServe` combination, disabled mode, listener counts, save/load, and damaged, duplicate and unknown file content. The harness is not committed because the repo has no JUnit; the same cases are in `PartyGameTests`.
- **Compile check against the real Minecraft 26.3 server jar** (SHA-1 `33680f5f…`, Mojang's download), Fabric API 0.161.0+26.3 (Modrinth, SHA-1 `53f9ee02…`) and Fabric Loader 0.19.3:
  - `party/*`, `JugcraftConfig` and `PartyGameTests` compile with no errors.
  - This used JDK 21 with the class-file versions lowered, because JDK 25 wasn't available, and a stub `Jugcraft` and `@GameTest`.
  - Compiling all of `src/main` the same way reports only the existing access-widened `CreativeModeTabs` fields, which Loom handles in the real build.
  - It is not a substitute for `./gradlew build`.

Not run (no JDK 25 or Gradle access in the sandbox): `./gradlew build`, the game tests on a headless server, client launch, and a two-client dedicated-server test. CI on the PR is the first real build.

## World and event applicability
No worldgen, dimensions, creatures, loot or seasonal content. PvP, friendly fire and claim integration are deliberately out of scope.

## Rollout and open questions
Remaining work:
- A two-client dedicated-server playtest (invite, accept, leave, kick, reconnect, restart).

Open questions:
- Is 8 members the right default?
- Should any member be able to invite, or only the leader?
- Should parties have a name and colour?
