JUGCRAFT workshops, spellbooks, aircraft, furniture and protected graves — release candidate.

- Adds the current Jimtafacts implementations: accessories and wearable models; digital item/fluid storage and crafting; transport pipes, gates, robots and industrial equipment; native workshop computers and peripherals; spellbooks, magical equipment and encounters using the supplied art and animations.
- Adds seven aircraft with flight, fuel, equipment, cargo and retained models; 267 furniture blocks, 18 associated items, 28 paintings and 317 furniture recipes.
- Adds owner-recoverable graves. Personal/global keep-inventory and boss-lair recovery keep their existing behavior; recovery preserves item components, refuses other players and cannot duplicate previously recovered contents.
- Includes the integrated crafting/economy, projectile targeting, visitor combat and pet/multipart claim-protection fixes from the predecessor PRs, plus the latest workshop and machine interfaces already on main.
- Repairs recipe collisions with vanilla sticks and conduits, composes spell defenses with existing equipment, and gives digital machinery its intended item/fluid automation ports.

The full source collections are not completely ported. Larger spatial storage/entity migration, Lua/CraftOS, remaining factory mappings and some encounter presentation remain follow-up work. Nine preview items still have explicitly deferred survival acquisition. See docs/features/jimtafacts-remaining.md and the individual feature records for exact boundaries; do not advertise full source-mod compatibility.

Existing towns, inventories and worlds are preserved. Added natural structures require new chunks; this candidate does not reset the Nether or regenerate occupied terrain. Back up the world, player data and configs before upgrading; restoring that backup with the previous artifacts is the rollback for newly saved content.

Existing players: accept the update in JUGCRAFT's title-menu updater, apply and quit, wait for completion, then relaunch the same Modrinth profile. Settings, keybinds, saves and extra mods stay in place. No new launcher profile or public Modrinth pack listing is required.

Local validation passed 268 Python tests, 389 combined server regressions, 79 updater assertions, the complete model/data audit and six rendered-client test classes. The corrected statue/equipped-model gallery, companion automation/population checks and three framework server tests also passed. The full GitHub Build/Foundation checks remain the release gates; no hosted two-account playtest is claimed. Publication alone does not activate the player channel: obtain owner restart confirmation, give players a full 30-second warning, back up and update the hosted server, verify startup/join, and only then promote the matching client update.

AI assistance: OpenAI Codex (user-selected Astra High).
