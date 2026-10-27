# Server load benchmark

Status: implemented (pending CI and review). It measures, it does not change the game.
Proposal issue: none. The TODO asks to "Benchmark server tick time, memory and client frame times under representative player load". On 26 October 2026 the owner asked for the suggested items in order ("then do 2-9 in order"); the eighth was "server load benchmark". This is the server tick part.

Owner: jimbozoomer-byte
Target milestone and tier: project health, no tier
Primary specialty and supported player role: not applicable (hosting)

## What it measures

A server game test, `ServerLoadGameTests.measuredServerLoad`, builds a representative load in the empty 48 × 48 `tatterlace_arena` test area and times what a tick of it costs on the server:

| Load | What it is | How it is timed |
| --- | --- | --- |
| A factory | 16 each of five working processing machines (electric furnaces, crushers, pulverizers, sawmills and sieves), 80 in all, each charged full and fed a stack of something it works | each machine's own block entity ticker, as the level calls it |
| A crowd | 6 each of the four costumed villains, 24 in all, free to think and move | each mob's own tick |

Each load is warmed up for 20 rounds, then timed with `System.nanoTime` over 100 rounds. A round is one tick of the whole load, on top of the ticks the server gives it anyway. The test logs one line:

```
Server load: factory of 80 machines <us> us a tick (<us> us a machine, <n> of them working or done), crowd of 24 costumed villains <us> us a tick (<us> us a mob), over 100 timed rounds after 20 warm-up rounds
```

It asserts that each load's round stays within a whole server tick (50 ms), and that some machines are working. The bound is there to catch a change that makes the work grow many times over, not to time the runner.

This follows the Concordance's workload test ([arcane-concordance-delivery.md](arcane-concordance-delivery.md)): the work is timed directly, so the other tests the server runs at the same time count less than they would in the server's own tick times.

## Results

CI's numbers go here once the pull request's `mod` job has run; see the PR. They are a CI runner's, shared with every other test running at once: an indication, not a profile.

## What it does not measure

- **Memory:** not measured. A heap figure taken inside a test server running hundreds of other tests would not say what Jugcraft holds.
- **Client frame times:** not measured. The client game tests draw with software rendering (Mesa), so their frame times would not stand for a player's.
- **Players:** no real players. A hosted multiplayer test with real clients is still needed to set a player limit ([TODO](../TODO.md)).
- **The world's own ticking:** chunk loading, lighting, vanilla mobs and redstone are not in these numbers.

## Connections, balance and persistence

Not applicable: it adds one server game test and changes no game code, data or saves.

## Verification

- Local, without Gradle or a game: the Java syntax parse and the repository checks; see the PR.
- CI: the `mod` job runs it with the rest of the server game tests.
- Not run: a dedicated server with real players.

## Rollout and open questions

- If the owner wants more of the mod in the numbers, the same test can take more loads: drones flying a depot, item pipes and conveyors, trains, town folk.
- Client frame times would need a hardware-rendered client run, outside CI.
