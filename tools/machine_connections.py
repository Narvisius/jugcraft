"""Physical sockets at the exact block faces that the server exposes to cables and pipes.

Legacy machines keep their existing all-face connections. Industrial forms use their explicit
port table. A closed gland and a short conduit join each socket to the authored casing, even
when the casing is inset. Coordinates and materials reuse the native machine model pipeline.
"""

FACES = {"north": (2, -1), "south": (2, 1), "west": (0, -1),
         "east": (0, 1), "down": (1, -1), "up": (1, 1)}
FORM_SIDES = {"FRONT": "north", "BACK": "south", "LEFT": "east", "RIGHT": "west",
              "TOP": "up", "BOTTOM": "down"}


def legacy_ports(machine, offsets):
    from machines import STATS, FLUID_STATS
    from large_machines import POWER_PORTS
    if STATS.get(machine, {}).get("capacity", FLUID_STATS.get(machine, {}).get("energy_capacity", 0)) <= 0:
        if machine in {"steel_tank", "gas_holder", "large_steam_engine", "steel_foundry"}:
            return [(p, f, "fluid") for p, f, _ in exterior_ports(offsets)]
        return []
    if machine in POWER_PORTS:
        part, face = POWER_PORTS[machine]
        return [(offsets[part], face, "power")]
    return exterior_ports(offsets)


def exterior_ports(offsets=((0, 0, 0),)):
    """All exposed energy faces for blocks whose storage has no sided restrictions."""
    occupied = set(map(tuple, offsets))
    ports = []
    for offset in offsets:
        for face, (axis, sign) in FACES.items():
            adjacent = list(offset)
            adjacent[axis] += sign
            if tuple(adjacent) not in occupied:
                ports.append((offset, face, "power"))
    return ports


def form_ports(info):
    return [((-column, layer, row), FORM_SIDES[side], "power" if kind.startswith("ENERGY") else
             "fluid" if kind.startswith("FLUID") else "item")
            for _, kind, _, column, row, layer, side in info["ports"]]


def permanent_ports(machine, offsets):
    ports = legacy_ports(machine, offsets)
    if len(offsets) == 1 or len(ports) <= 1:
        return ports
    # One visible service socket on the controller side. Extra legacy all-face
    # hookups are drawn only while connected, keeping open frames readable.
    return sorted(ports, key=lambda p: (sum(abs(v) for v in p[0]),
                  ["east", "west", "south", "north", "down", "up"].index(p[1])))[:1]


def write_dynamic_sockets(root, models, footprints):
    import json
    from pathlib import Path
    from kinetic_rotors import quads
    entries = {}
    for name, offsets in footprints.items():
        if name not in models:
            continue
        fixed = permanent_ports(name, offsets)
        ports = [p for p in legacy_ports(name, offsets) if p not in fixed]
        if not ports:
            continue
        body = models[name]
        entries[name] = [{"offset": p[0], "face": p[1],
                          "quads": quads(with_sockets(body, [p])[len(body):])} for p in ports]
    Path(root).mkdir(parents=True, exist_ok=True)
    (Path(root) / "machine_connection_quads.json").write_text(json.dumps(entries, separators=(",", ":")) + "\n", encoding="utf-8")


def with_model_sockets(model, ports=None):
    """The same fittings for the older JSON-native decorative/terminal generators."""
    from model_writer import element, texture_names
    anchors = [(e["from"], e["to"], "dp_gunmetal") for e in model["elements"]
               if not e.get("rotation") and not e.get("rotations")]
    fittings = with_sockets(anchors, exterior_ports() if ports is None else ports)[len(anchors):]
    textures = {**model["textures"], **{t: f"jugcraft:block/{t}" for t in texture_names(fittings)}}
    return {**model, "textures": textures,
            "elements": model["elements"] + [element(*e[:3], uv=True) for e in fittings]}


def with_sockets(elements, ports):
    """Add closed, block-centred sockets; never change gameplay faces, footprints or moving parts."""
    from steampunk_models import box, span
    result = list(elements)
    from model_writer import texture_names
    materials = set(texture_names(elements))
    power_rim = "el_frame" if "el_frame" in materials else "dp_chrome" if any(t.startswith("dp_") for t in materials) else "sp_brass"
    power_cap = "el_port!" if "el_frame" in materials else "power_port!"
    # Use only fixed, volumetric geometry as a mounting surface. Do not attach to a rotor.
    fixed = [(e[0], e[1]) for e in elements
             if (len(e) < 4 or not e[3].get("rotation"))
             and all(e[1][i] > e[0][i] for i in range(3))]
    for offset, face, kind in ports:
        axis, sign = FACES[face]
        cross = [i for i in range(3) if i != axis]
        low = [v * 16 for v in offset]
        high = [v + 16 for v in low]
        centre = [v + 8 for v in low]
        plane = high[axis] if sign > 0 else low[axis]
        centre[axis] = plane
        inside = -sign
        # The first fixed surface directly behind the socket, in this occupied block.
        candidates = []
        nearby = []
        for a, b in fixed:
            if any(b[i] <= low[i] or a[i] >= high[i] for i in range(3)):
                continue
            if all(a[i] <= centre[i] <= b[i] for i in cross):
                edge = a[axis] if sign < 0 else b[axis]
                distance = (edge - plane) * inside
                if -1 <= distance <= 16:
                    candidates.append(max(0, distance))
            # If the centre line misses (open frame, pedestal), route a small right-angle
            # conduit to the nearest fixed part rather than leaving a floating socket.
            point = [max(max(a[i], low[i]) + .125,
                         min(centre[i], min(b[i], high[i]) - .125)) for i in range(3)]
            distance = sum(abs(point[i] - centre[i]) for i in range(3))
            nearby.append((distance, point))
        # Some legacy footprints reserve air around tall/open machinery. Those cells
        # still accept power in saved factories. Mount their socket to the nearest
        # casing/frame in the whole footprint instead of leaving an unsupported cap.
        if not nearby:
            for a, b in fixed:
                point = [max(a[i] + .125, min(centre[i], b[i] - .125)) for i in range(3)]
                nearby.append((sum(abs(point[i] - centre[i]) for i in range(3)), point))
        if not nearby:
            raise ValueError(f"Socket has no fixed mounting surface: {offset}/{face}")
        reach = min(candidates) + .25 if candidates else 2
        reach = max(1.25, min(16, reach))
        u, v = (centre[i] for i in cross)
        # The rim crosses the exact cell boundary by 1/8 pixel, hiding rounding seams.
        # Eight-pixel collars accept the six-pixel power cable; smaller fluid fittings
        # take the four-pixel pipe. The inset cap makes an unused socket easy to spot.
        radius = 4 if kind == "power" else 3 if kind == "fluid" else 4
        rim = power_rim if kind == "power" else "dp_chrome"
        cap = power_cap if kind == "power" else "dp_rubber"
        bounds = sorted((plane + sign * .125, plane + inside * 1.125))
        result.append(box(*span("xyz"[axis], *bounds, u-radius, u+radius, v-radius, v+radius),
                          {"*": rim, face: cap}))
        stem = sorted((plane + inside, plane + inside * reach))
        result.append(box(*span("xyz"[axis], *stem, u-2.25, u+2.25, v-2.25, v+2.25), "dp_rubber"))
        if not candidates and nearby:
            end = min(nearby, key=lambda pair: pair[0])[1]
            start = centre.copy()
            start[axis] = plane + inside * reach
            for section, along in enumerate([axis, *cross]):
                if abs(end[along] - start[along]) > .125:
                    # Stepped sleeves avoid coplanar UV seams where two elbows overlap.
                    radius = 1.25 + section * .2
                    a = [p-radius for p in start]
                    b = [p+radius for p in start]
                    a[along], b[along] = sorted((start[along], end[along]))
                    result.append(box(a, b, "dp_rubber"))
                    start[along] = end[along]
    return result
