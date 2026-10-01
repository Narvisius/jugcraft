# Art direction

Jugcraft's look changes with its tiers, the way real technology did: the early game is brass-and-steam, and the later tiers move towards dieselpunk. Every texture and model is original, drawn by the generators in `tools/`.

## Rules for everything
- Detailed models built from boxes (see `tools/steampunk_models.py`): round prisms, gears, gauges, rivets, pipes. No flat cubes where a real machine would have shape.
- **Things that are big in real life are big in the world.** A turbine, a foundry or a charging station takes several blocks; a hand tool stays in the hand.
- Overlapping boxes never share a visible face plane (that flickers, z-fighting). The generators enforce it: `model_writer.separate_coplanar` runs on every model they write and pushes the smaller of two flush, differently drawn faces out by 0.02 pixels, so a band, dial or trim always draws in front of the body it sits on.
- Textures are 16×16, deterministic (seeded), and opaque on blocks.

## Steampunk: stone, bronze and early steel tiers
Brass, copper and riveted iron; glass portholes and valve wheels; firebrick and wood. Textures start with `sp_` (`tools/steampunk_textures.py`). The classic style pack keeps the older plain look for anyone who prefers it.

## Dieselpunk: steel tier and above
As the tech gets higher tier, it becomes more dieselpunk and less steampunk. The powered tools and the charging station (#40) are the first dieselpunk content.
- **Materials:** gunmetal and olive-drab paint worn through to bare metal at the edges; chrome trim; yellow-and-black hazard stripes; black rubber hoses and grips; bakelite handles; louvred grilles; soot-stained exhaust stacks.
- **Details:** green phosphor gauges, caged amber warning lamps, stencilled serials, heavy bolts rather than decorative rivets.
- Textures start with `dp_` (`tools/dieselpunk_textures.py`); models for tools and stations are in `tools/tool_models.py`.

The steel-tier machines went dieselpunk in #41: the steel foundry, capacitor bank, steel tank, ore drill and high-pressure extractor (`tools/dieselpunk_models.py`, which replaces their entries in `steampunk_models.MODELS`). Their footprints, ports and running lights are unchanged, and the classic style pack keeps their plain look. The coke oven stays brick: it is the bridge into steel.
