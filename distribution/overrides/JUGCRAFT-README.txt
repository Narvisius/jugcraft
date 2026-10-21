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
compatible shader pack separately if desired.

Diorama is installed automatically on clients: M opens the map, comma creates a
waypoint, U lists waypoints. B remains the Jugcraft encyclopedia. Older saved
Diorama bindings are preserved; set New Waypoint to comma in Controls if needed.
N opens the backpack.
Diorama is not needed on dedicated servers.

Use a launcher/server installer that honors the pack's client/server file flags.
Client-only libraries are not required on a dedicated server. Opening the website
alone or copying the standalone Jugcraft JAR does not download dependencies.

Sources, library licenses, contributor instructions, and update information:
https://github.com/jimbozoomer-byte/jugcraft
https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/FRAMEWORKS.md
https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/DISTRIBUTION.md
https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/SERVER_RELEASES.md

Updates: a standalone file import does not subscribe to future releases. For
ongoing server play use the owner's Modrinth Server Project Play link once it is
configured; its linked pack prompts for required updates before launch.
