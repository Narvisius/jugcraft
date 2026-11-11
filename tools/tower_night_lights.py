"""The Tri-Pillar's night lighting (Justin approved the night lighting sheet on 5 November 2026): the tower's ceiling lights
(the Ceiling Light Panel and the Sealed Linear Ceiling Light, tower/TowerLightBlock) get a night mode, and the tower
switches the ones on its two crew floors at night (tower/TowerNightLights): on round the night watch's posts, night in
the halls and corridors, low where the off-shift crew sleep in the open, off in rooms nobody is in at night.

  write_assets(write, assets)   the lamps' models in each mode (the diffuser glowing at the mode's light; for off, an
                                unlit diffuser, greyer, with no glow) and their blockstates with "mode"; called at the end
                                of tower_realism_runtime.write_assets, which writes the lamps' own models
  plan(blocks, crew)            each lamp's night mode on the crew floors (the layout's "night_lights", only those not
                                on: [x, y, z, mode]), tower_tripillar.export

A lamp's floor is the crew floor under its ceiling (the ground floor's ceilings at 4, 6 and 9 over y 1; the upper
floor's at 15 over y 12). Rooms are the floor's walkable regions with the doors as their bounds (tripillar_crew.walkable);
the ground floor's hall is its largest. A lamp over a region that opens onto the sky (outside: the apron under the
intake face's overhang, the entrance canopy) is never switched: outdoor lights stay on all night. In order: a lamp within ON_REACH of a night-watch post's chair or spots, in that
post's room, stays on (the rounds, patrols, don't light their whole round); in the hall, low within LOW_REACH of a sleep
spot, else night; in a room someone sleeps in, low if it is a big room (a lounge, the cafe), off if not (the crew rest
room, the manager's office); in a room a round or a watch goes through, night; anywhere else off."""
from __future__ import annotations

import json
import math
from collections import Counter, deque
from pathlib import Path

from PIL import Image

import tripillar_crew as CREW

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
LAMPS = ("ceiling_light_panel", "linear_ceiling_light")
MODES = {"on": 15, "night": 12, "low": 10, "off": 0}
OPAL, OPAL_OFF = "tower_fixture_opal", "tower_fixture_opal_off"
FLOORS = {1: (4, 6, 9), 12: (15,)}                   # crew floor -> the lamp heights over it
ON_REACH, LOW_REACH = 7.0, 6.0
ROUNDS = ("hall_patrol", "data_hall_rounds", "pad_rounds", "janitor_north")
BIG_ROOM = 150                                       # cells: a lounge or the cafe, not a small room


# ------------------------------------------------------------------ the models

def mode_model(model, mode):
    """A lamp's model in a night mode: the diffuser's glow at the mode's light, or unlit and greyer when off."""
    out = json.loads(json.dumps(model))
    if mode == "off":
        out["textures"][OPAL_OFF] = f"jugcraft:block/{OPAL_OFF}"
    for e in out["elements"]:
        if "light_emission" not in e:
            continue
        if mode == "off":
            del e["light_emission"]
            for face in e["faces"].values():
                if face["texture"] == f"#{OPAL}":
                    face["texture"] = f"#{OPAL_OFF}"
        else:
            e["light_emission"] = min(e["light_emission"], MODES[mode])
    return out


def off_texture(assets):
    """The unlit diffuser: the opal greyed and a shade darker (a diffuser with no light behind it)."""
    im = Image.open(assets / "textures/block" / f"{OPAL}.png").convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            grey = (r + g + b) / 3
            px[x, y] = (int((grey * 0.6 + r * 0.4) * 0.80), int((grey * 0.6 + g * 0.4) * 0.80), int((grey * 0.6 + b * 0.4) * 0.82), a)
    return im


def write_assets(write=None, assets=ASSETS):
    assets = Path(assets)
    if write is None:
        import model_writer
        write = model_writer.write
    off_texture(assets).save(assets / "textures/block" / f"{OPAL_OFF}.png", optimize=True)
    for lamp in LAMPS:
        model = json.loads((assets / "models/block" / f"{lamp}.json").read_text())
        for mode in MODES:
            if mode != "on":
                write(assets / "models/block" / f"{lamp}_{mode}.json", mode_model(model, mode))
        variants = {}
        for mode in MODES:
            name = f"jugcraft:block/{lamp}" + ("" if mode == "on" else f"_{mode}")
            if lamp == "ceiling_light_panel":           # no facing: the same for all four
                for facing in ("north", "east", "south", "west"):
                    variants[f"facing={facing},mode={mode}"] = {"model": name}
            else:
                for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                    variants[f"facing={facing},mode={mode}"] = {"model": name, **({"y": angle} if angle else {})}
        write(assets / "blockstates" / f"{lamp}.json", {"variants": variants})


# ------------------------------------------------------------------ the night modes

def rooms(b, y):
    """The floor's walkable regions (doors bound them), largest first, and each cell's region."""
    rid, out = {}, []
    for x in range(-64, 65):
        for z in range(-64, 65):
            c = (x, y, z)
            if c in rid or not CREW.walkable(b, *c):
                continue
            comp, q = [c], deque([c])
            rid[c] = -1
            while q:
                p = q.popleft()
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    n = (p[0] + dx, y, p[2] + dz)
                    if n not in rid and CREW.walkable(b, *n):
                        rid[n] = -1
                        comp.append(n)
                        q.append(n)
            out.append(comp)
    out.sort(key=len, reverse=True)
    for i, comp in enumerate(out):
        for c in comp:
            rid[c] = i
    return out, rid


def work_cells(post):
    """Where a post's crew work on its floor (none for the security detail outside: CREW.OUTSIDE)."""
    if post["kind"] in CREW.OUTSIDE:
        return []
    return [tuple(post["seat"])] if post["kind"] == "seat" else [tuple(s[:3]) for s in post["spots"]]


def plan(b, crew):
    """The lamps switched at night: [x, y, z, mode] for each lamp on the crew floors not kept on (b: the finished
    tower's blocks; crew: the layout's crew list with its routines)."""
    out = []
    for y, heights in FLOORS.items():
        regs, rid = rooms(b, y)
        tops = {}
        for (bx, by, bz) in b:
            if by > y and by > tops.get((bx, bz), -999):
                tops[(bx, bz)] = by
        def sky(x, z):                               # nothing of the tower at the floor's level or over it: open sky
            return (x, z) not in tops and (x, y, z) not in b
        outside = {i for i, comp in enumerate(regs)
                   if any(sky(c[0] + dx, c[2] + dz) for c in comp for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
        watch = [c for p in crew if p["night"] and p["id"] not in ROUNDS for c in work_cells(p) if c[1] == y]
        watched = {rid.get(c) for c in watch} | {rid.get(tuple(p["stand"])) for p in crew if p["night"] and p.get("stand") and p["stand"][1] == y}
        rounds = {rid.get(c) for p in crew if p["id"] in ROUNDS for c in work_cells(p) if c[1] == y}
        sleep = [tuple(p["sleep"]["seat"]) for p in crew if "sleep" in p and p["sleep"]["seat"][1] == y]
        slept = {rid.get(tuple(p["sleep"]["stand"])) for p in crew if "sleep" in p and p["sleep"]["seat"][1] == y}
        for (x, ly, z), state in sorted(b.items()):
            if CREW.name(state) not in tuple(f"jugcraft:{l}" for l in LAMPS) or ly not in heights:
                continue
            room = rid.get((x, y, z))
            if room is None:                         # over furniture: the nearest floor cell within two
                near = [(dx * dx + dz * dz, rid[(x + dx, y, z + dz)]) for dx in range(-2, 3) for dz in range(-2, 3) if (x + dx, y, z + dz) in rid]
                room = min(near)[1] if near else None
            if room in outside:
                continue                             # outdoors (under a canopy): on all night
            d_watch = min((math.hypot(x - c[0], z - c[2]) for c in watch), default=99.0)
            d_sleep = min((math.hypot(x - c[0], z - c[2]) for c in sleep), default=99.0)
            if room is not None and room in watched and d_watch <= ON_REACH:
                mode = "on"
            elif room == 0:
                mode = "low" if d_sleep <= LOW_REACH else "night"
            elif room is not None and room in slept:
                mode = "low" if len(regs[room]) > BIG_ROOM else "off"
            elif room is not None and (room in watched or room in rounds):
                mode = "night"
            else:
                mode = "off"
            if mode != "on":
                out.append([x, ly, z, mode])
    return out


if __name__ == "__main__":
    write_assets()
    print("lamp models and blockstates written")
