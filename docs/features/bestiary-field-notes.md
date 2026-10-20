# Bestiary field notes

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Expand the bestiary beyond encountered-lair dossiers and consider optional search/favorites." This change expands the bestiary. Search and favorites are left for later: the [field-guide record](constellation-and-field-guide.md) says they wait until the owner approves the chapter and topic organisation.

**This change's choices:** which creatures are listed, what each entry shows, and the cap of 24. The owner can review all three.

Owner: jimbozoomer-byte
Target milestone and tier: any time; it grows with play
Primary specialty and supported player role: exploration and combat; any player who wants to know what they are fighting

## Player experience

The Encyclopedia's **Bestiary & expeditions** chapter (the Expeditions page) now ends with **Field notes**. After the lair dossiers, every Jugcraft creature the player has defeated, or been defeated by, gets its own topic. Each topic shows:

- the creature's name, with its spawn egg as the icon where it has one;
- its **health, attack and armour**, the numbers the game itself gives it (a creature with no melee attack says so);
- **the player's tally:** how many times they have defeated it, and it them.

The list is ordered most-defeated first. It shows up to 24 creatures, then one line counts the rest. A creature the player has never fought is not listed, so the page spoils nothing and invents no lore. A newcomer's Expeditions page is unchanged.

## Connections

- **Input producers:** vanilla's own per-player statistics ("mobs killed" and "killed by"), which every world already keeps, and each creature's registered default attributes.
- **Output consumers:** the Encyclopedia screen. It needs no client change: each heading becomes a topic, as the dossiers' do.
- **Optional:** reading only.

## Multiplayer and persistence

- **Server authority:** the server builds the page from that player's own statistics. The request names no creature and claims nothing, and the existing rate limit applies.
- **Saves:** nothing new is saved. The statistics are vanilla's.
- **New IDs:** none, only six language keys (`encyclopedia.jugcraft.bestiary.*`).
- **Size:** the page keeps to its existing 96-row limit (24 creatures × 3 rows, plus the heading, introduction and the count of the rest).

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py` (including the Encyclopedia's language-key check) and `python scripts/check_repository.py` passed.

**Server game test for CI:** `EncyclopediaGameTests.theBestiaryListsOnlyCreaturesFought` checks that:

- a newcomer has no field notes;
- after three defeats of a Jugcraft creature and one defeat by it, the bestiary counts exactly that, and the Expeditions page shows its heading, its name and the tally.

**Results:** the fork's Build workflow passed on commit ce00dcd4 ([run 39165204518](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39165204518)), including `theBestiaryListsOnlyCreaturesFought`. One client shard first failed when the Modonomicon library's book-sync packet disconnected the test client in `EncyclopediaRoutesClientGameTests` (unrelated to this change); its single re-run passed.

**Not run:** reading the page in a running game client.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
