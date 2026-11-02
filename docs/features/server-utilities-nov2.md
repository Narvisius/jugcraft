# Server utilities and custom NPCs — local work, November 2

Owner requested public /warp, /spawn, nearby /trade; OP /setwarp, /setspawn, /god, /invsee; visual protection tool and custom guards/NPCs with player-name skins. OpenAI Codex implements utilities/protection; owner-authorized Claude Code implements NPC/guard helpers for local integration. No commit, publication or live operation.

Implemented and locally validated:
- World-persistent named destinations, destination collision checks and five-second teleport cooldown. Boss instances retain their exit lifecycle.
- Nearby (8 blocks + line of sight) held-stack trades with a native offer/Confirm/Cancel window: both confirm unchanged full stack components, 30-second expiry, cancel on changed availability. No escrow or orphaned inventory on disconnect/restart. Hoverable offer details.
- Persistent OP god mode; OP read-only inventory snapshot; no grant of operator permissions.
- OP protection surveyor: left/right corners, /protect create/remove/list and command corner fallback. Dimension-specific cuboids reuse TownProtection for builds, containers, public doors, explosions/fire/pistons and shared automation authority. Selection gold, protection red; held-tool actionbar status and nearby town boundary marks.

NPCs/guards/skins are integrated; focused permission, transaction, persistence and rendered checks passed. /setspawn currently defines the public command destination, not bed or vanilla respawn rules.

First six server tests passed (NBT persistence, permissions, boundary protection, god toggle, blocked warp, mutual trade consent and changed offers). Rendered trade consent and read-only inspection, plus the protection-tool walkthrough, passed in a disposable local client world. Logs: build/server-utilities-boundary-test.log. NPC common and client sources compile; the final integrated run passed.

## Player commands
- `/warp`: list named public destinations. `/warp <name>` travels to one.
- `/spawn`: travel to the operator-defined spawn destination, otherwise the vanilla world spawn.
- `/trade <player>`: hold the stack to offer first. Both players must be within 8 blocks and see each other. The window shows the two exact stacks; both click Confirm (or `/trade accept`). Cancel/closing/moving away/changing either stack aborts. Current scope is one held stack per player, not a multi-slot or money exchange.

## Operator commands
Permission level 2 or higher, matching existing town administration.
- `/setwarp <name>`, `/delwarp <name>`, `/setspawn`.
- `/god [player]`: toggle persistent damage immunity for yourself or an online player; does not grant operator permissions.
- `/invsee <player>`: read-only inventory/equipment snapshot. Reopen to refresh; never transfers items.
- `/protect tool`: Protection Surveyor. Left click sets corner 1, right click corner 2. Hold it to see red protection boundaries, gold selection and protected/unprotected status.
- `/protect pos1`, `/protect pos2`: set corners at your position instead of clicking.
- `/protect create <name>`, `/protect list`, `/protect remove <name>`.
- Regions use inclusive XYZ coordinates, persist per dimension and do not modify blocks. Public doors/workstations retain town-style interaction; stored inventories remain protected. Creative operators retain the existing editing exemption. Existing player claims remain enforced too.

No new dependencies, backend services, Bukkit server, plugins or asset downloads are introduced for utility commands.

## Custom NPCs and guards
Claude's implementation is integrated locally, with Codex review hardening. See [custom NPCs](custom-npcs.md) for the full reference. Create an NPC where you stand with `/npc create Town Guide` or a guard with `/guard create Market Guard`. The new NPC becomes your selection. Use `/npc skin player <MinecraftName>`, `/npc say <text>`, `/npc post`, and `/npc patrol add` (at each waypoint). `/npc select` selects a nearby custom NPC; `/npc list` shows IDs; `/npc remove` removes your selection. Editing does not replace town residents or rewrite buildings.

Six NPC server tests passed in the integrated checkout: permissions/commands, entity save-load, monster targeting, physical patrol traversal, removal tombstones, and mocked asynchronous skin resolution including stale/failed replies. The real Mojang lookup is not claimed as tested. Combined rendered client check passed (build/npc-final-validation.log). The final screenshots confirm custom skins are no longer obscured by seasonal costumes. Twelve focused server tests passed across utility and NPC runs; the client walkthrough exercised trade consent/transfer, read-only inventory, region visualization and custom-NPC rendering. This is focused validation, not the entire repository suite. Actual Mojang skin retrieval and visual classic/slim comparison remain owner-playtest follow-ups.
