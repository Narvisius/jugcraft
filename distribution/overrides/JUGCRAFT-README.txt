JUGCRAFT
========

Import the .mrpack in Modrinth App and confirm installation. The launcher installs
Minecraft 26.3, Fabric Loader 0.19.5, Jugcraft, and the exact library files listed
in JUGCRAFT-LIBRARIES.json. Internet access is required during installation.

This is an early-development candidate. Automated compilation and packaging do
not establish gameplay or multiplayer compatibility. See the repository's
framework and testing documents for the actual verification status.

Jade and JEI provide inspection and recipe support. Other libraries are available
for development; their presence does not mean every Jugcraft model, spell, or
screen already uses them. Iris is included without a shader pack. Choose a
compatible shader pack with Resourcify if desired: open Resource Packs or the
Iris Shader Packs screen and use the + button to browse and download. No shader
is automatically selected or downloaded for you.

Diorama is installed automatically on clients: M opens the map, comma creates a
waypoint, U lists waypoints. B remains the Jugcraft encyclopedia. Older saved
Diorama bindings are preserved; set New Waypoint to comma in Controls if needed.
N opens the backpack.
The minimap is off by default on new installs; the fullscreen map still works.
You can re-enable the minimap in Diorama settings. Existing choices are preserved.
Diorama and Resourcify are not needed on dedicated servers.

Use a launcher/server installer that honors the pack's client/server file flags.
Client-only libraries are not required on a dedicated server. Opening the website
alone or copying the standalone Jugcraft JAR does not download dependencies.

Sources, library licenses, contributor instructions, and update information:
https://github.com/jimbozoomer-byte/jugcraft
https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/FRAMEWORKS.md
https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/DISTRIBUTION.md
https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/SERVER_RELEASES.md

Updates: new packs with JUGCRAFT-UPDATE.json check the owner's release channel
at the Minecraft title menu. Choose JUGCRAFT Updates > Download update, then
Apply update & quit. Wait a moment and press Play again in Modrinth. The same
instance is updated; controls, settings, worlds and extra mods stay in place.
No EXE, launch hook or public Modrinth pack listing is needed. If you tested the
old EXE, remove its instance Pre-launch hook. Older packs need initial migration;
importing a historical pack does not add the new updater retroactively.
