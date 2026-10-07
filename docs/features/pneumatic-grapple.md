# Pneumatic grapple

Status: implemented on `feature/grapple-30` (batch 30), stacked on `feature/refinery-29`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, rejected the cutting torch ("it can't compete with a diamond pickaxe; we need to be more creative") and asked Claude to rethink weak ideas. The grapple replaces it: something vanilla cannot do, run on chemistry.
Owner: jimbozoomer-byte
Target milestone and tier: nitrogen chemistry (after the air separation unit).
Primary specialty and supported player role: exploring, building high, and fighting.

## Player experience
- **Pneumatic Grapple:** a harpoon gun that runs on compressed nitrogen.
  - Holds 4,000 mB. Use it on anything holding nitrogen (the air separation unit, a gas holder, a tank) to fill it, like the scuba tank with oxygen.
  - Each shot uses 25 mB (160 shots a fill) and has half a second's cooldown.
- **The hook** flies up to 32 blocks on a line drawn back to your hand.
  - **In a block:** it reels you in fast along the line. Fall damage is cancelled while you are reeled. On arrival it lets go with a little hop, so you can climb a cliff or the side of a tower in one shot.
  - **In a mob:** it drags the mob to you. Bosses and anything with 100 health or more (iron golems) are too heavy: the hook falls away.
  - **Players:** another player is hooked only where you could hurt them (PvP on), and never a party member.
  - **Letting go:** use the grapple again. It also lets go after 5 seconds in a block or 3 in a mob, if you switch away from the grapple, or if the line runs out.
- Advancement: **Reel Me In** (make a grapple). Handbook page in the gear chapter.

## Connections
- Existing input producer: nitrogen from the air separation unit (8 mB/t from the air), gas holders and tanks; gaskets (rubber), steel plates, a fluid tank.
- Output consumer: the player. The first real consumer of the air separation unit's nitrogen beyond ammonia.
- Technology connection: Fabric fluid transfer for filling; the shared party API for who may be hooked.
- Magic connection: none. Required vs optional: optional.

## Balance and automation
- Nitrogen is cheap (the ASU makes it from air at 64 JE/t), so the cost is mostly the crafting and setting up nitrogen; a fill is 4,000 mB, 500 ticks of the ASU.
- The grapple does not mine, place or break blocks. It moves its owner and light mobs only.
- Dragging is capped (100 health) so it is not a boss tool, and it never hooks party members.

## Multiplayer and persistence
- Everything is decided on the server. The owner's pull is sent to their client as motion (like a knockback), so the client never moves itself; fall distance is reset on the server.
- The hook is not saved: after a reload (or if its owner is gone) it removes itself.
- Recipe follows the `machines` feature switch.

## Dependencies and assets
No new dependencies. The icon (32x32) and the hook texture are drawn by `tools/grapple.py`; the hook and its line are drawn by `client/GrappleHookRenderer` (the line in the same way as the string lights). All original.

## Verification
- `tools/check_mod_data.py`: the numbers in `JugcraftGrapple` match `tools/grapple.py`; the item, entity, names and textures exist.
- Game tests (CI):
  - `grappleFillsFromAGasHolder`: fills to 4,000 mB and leaves the rest;
  - `grappleFiresAndLetsGo`: a shot costs 25 mB and puts a hook out; using it again lets go without cost; an empty grapple does not fire;
  - `grappleReelsItsOwnerToAWall`: the hook bites a wall, the owner is pulled toward it and their fall distance is cleared;
  - `grappleDragsLightMobsOnly`: a pig is dragged in; an iron golem is not hooked.
- Client screenshot: `jugcraft_pneumatic_grapple` (in hand, with the hook out on its line).
- Not run: client play (how the reel feels, the line from first person), two players, PvP.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- Speeds, range and the 100-health cap are first values for the owner to tune.
- A zip-line (riding a line between two anchors) was considered and left out of this batch.
