"""Control electronics (batch 36, docs/features/control-electronics.md): data cables, sensors, relays and the logic
controller.

Java: control/ (JugcraftControl, DataCableBlock, SensorBlock, RelayBlock, LogicControllerBlock and its block entity,
menu and client screen). tools/check_mod_data.py keeps the numbers here and in Java the same.

Channels are the sixteen dye colours. Sensors and relays show theirs as a coloured lamp; one small model per colour,
turned by the blockstate. All art is drawn here (dark graphite casings, the cyan electronics look).
"""
import random

from PIL import Image

MOD = "jugcraft"

# Seconds-scale numbers (ticks): sensors re-read, controllers re-evaluate.
SENSOR_INTERVAL = 20
CONTROLLER_INTERVAL = 20
RULES = 8
THRESHOLD_STEP = 5
MAX_CABLES = 1_024

# The dye colours in DyeColor order, and the RGB the lamps and the controller screen use for them.
CHANNEL_COLORS = {
    "white": (0xF0, 0xF0, 0xF0), "orange": (0xF9, 0x80, 0x1D), "magenta": (0xC7, 0x4E, 0xBD),
    "light_blue": (0x3A, 0xB3, 0xDA), "yellow": (0xFE, 0xD8, 0x3D), "lime": (0x80, 0xC7, 0x1F),
    "pink": (0xF3, 0x8B, 0xAA), "gray": (0x47, 0x4F, 0x52), "light_gray": (0x9D, 0x9D, 0x97),
    "cyan": (0x16, 0x9C, 0x9C), "purple": (0x89, 0x32, 0xB8), "blue": (0x3C, 0x44, 0xAA),
    "brown": (0x83, 0x54, 0x32), "green": (0x5E, 0x7C, 0x16), "red": (0xB0, 0x2E, 0x26), "black": (0x1D, 0x1D, 0x21),
}

BLOCKS = {
    "data_cable": "Data Cable",
    "sensor": "Sensor",
    "relay": "Relay",
    "logic_controller": "Logic Controller",
    # Batch 37: the control room.
    "control_monitor": "Control Monitor",
    "alarm": "Alarm Klaxon",
}
ITEMS = {"control_remote": "Control Remote"}
# Batch 37 numbers: the monitor's relink interval, the controller's history (samples, passes per sample), the alarm's
# interval and the remote's range.
MONITOR_RELINK = 40
HISTORY = 24
HISTORY_EVERY = 5
ALARM_INTERVAL = 30
REMOTE_RANGE = 256

FACINGS = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
CABLE_LO, CABLE_HI = 6, 10  # 4 pixels thick


def blocks():
    return list(BLOCKS)


def items():
    return list(ITEMS)


def rid(path):
    return f"{MOD}:{path}"


def _faces(texture, sides=("north", "east", "south", "west", "up", "down"), **overrides):
    return {side: {"texture": overrides.get(side, texture)} for side in sides}


def _box(frm, to, texture, **overrides):
    return {"from": list(frm), "to": list(to), "faces": _faces(texture, **overrides)}


def sensor_model():
    """Facing north: a graphite plate on the block behind (south), a small housing and the channel lamp in front."""
    return [
        _box((3, 3, 14), (13, 13, 16), "#casing"),
        _box((5, 5, 11), (11, 11, 14), "#frame"),
        _box((6, 6, 10.5), (10, 10, 11), "#frame", north="#lamp"),
        # A little cyan read-out window on the housing's top.
        _box((6, 11, 11.5), (10, 11.5, 13.5), "#frame", up="#glow"),
    ]


def relay_model():
    """A graphite box with the channel lamp and an on/off indicator on each side."""
    elements = [_box((1, 0, 1), (15, 14, 15), "#casing", up="#frame")]
    for side in ("north", "east", "south", "west"):
        for (x0, y0, x1, y1), texture in (((5, 8, 11, 12), "#lamp"), ((6, 3, 10, 6), "#state")):
            elements.append(_on_side(side, x0, y0, x1, y1, texture))
    # A redstone terminal on top.
    elements.append(_box((6, 14, 6), (10, 15, 10), "#frame", up="#state"))
    return elements


def _on_side(side, x0, y0, x1, y1, texture):
    """A thin plate standing half a pixel proud of the 1-px inset box's face on {side}."""
    if side == "north":
        return _box((x0, y0, 0.5), (x1, y1, 1), "#frame", north=texture)
    if side == "south":
        return _box((16 - x1, y0, 15), (16 - x0, y1, 15.5), "#frame", south=texture)
    if side == "east":
        return _box((15, y0, x0), (15.5, y1, x1), "#frame", east=texture)
    return _box((0.5, y0, 16 - x1), (1, y1, 16 - x0), "#frame", west=texture)


def monitor_model():
    """Facing north: a 3-pixel panel against the wall behind (south), its face the screen."""
    return [_box((0, 0, 13), (16, 16, 16), "#frame", north="#screen")]


def alarm_model():
    """A graphite base with the channel lamp on each side, an amber dome on top and a cap."""
    elements = [_box((3, 0, 3), (13, 3, 13), "#casing"),
                _box((4.5, 3, 4.5), (11.5, 9, 11.5), "#dome"),
                _box((6, 9, 6), (10, 10.5, 10), "#frame")]
    for side in ("north", "east", "south", "west"):
        elements.append(_alarm_lamp(side))
    return elements


def _alarm_lamp(side):
    if side == "north":
        return _box((6.5, 0.5, 2.5), (9.5, 2.5, 3), "#frame", north="#lamp")
    if side == "south":
        return _box((6.5, 0.5, 13), (9.5, 2.5, 13.5), "#frame", south="#lamp")
    if side == "east":
        return _box((13, 0.5, 6.5), (13.5, 2.5, 9.5), "#frame", east="#lamp")
    return _box((2.5, 0.5, 6.5), (3, 2.5, 9.5), "#frame", west="#lamp")


def controller_model():
    """Facing north: a cyan-look cabinet with the rule screen, a key strip and a status lamp, and a vent on top."""
    return [
        _box((1, 0, 2), (15, 15, 15), "#casing", up="#vent"),
        _box((2, 5, 1.5), (14, 13, 2), "#frame", north="#screen"),
        _box((2, 1.5, 1.5), (11, 4, 2), "#frame", north="#keys"),
        _box((12, 1.5, 1.5), (14, 4, 2), "#frame", north="#glow"),
    ]


def write_all(write, assets, data, lang, condition, self_drop):
    models = assets / "models" / "block"
    base_textures = {"casing": rid("block/el_casing"), "frame": rid("block/el_frame"), "glow": rid("block/el_glow_cyan"),
                     "particle": rid("block/el_casing")}
    for block, name in BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = name
        write(data / "loot_table" / "blocks" / f"{block}.json", self_drop(block))

    # Data cable: a core and an arm towards each connected side, like the power cables.
    cable = {"cable": rid("block/data_cable"), "particle": rid("block/data_cable")}
    lo, hi = CABLE_LO, CABLE_HI
    write(models / "data_cable_core.json", {"textures": cable, "elements": [
        {"from": [lo, lo, lo], "to": [hi, hi, hi], "faces": {f: {"uv": [lo, lo, hi, hi], "texture": "#cable"}
                                                             for f in ("north", "east", "south", "west", "up", "down")}}]})
    arm_faces = {"north": {"uv": [lo, lo, hi, hi], "texture": "#cable"},
                 "east": {"uv": [0, lo, lo, hi], "texture": "#cable"}, "west": {"uv": [0, lo, lo, hi], "texture": "#cable"},
                 "up": {"uv": [lo, 0, hi, lo], "texture": "#cable"}, "down": {"uv": [lo, 0, hi, lo], "texture": "#cable"}}
    write(models / "data_cable_arm.json", {"textures": cable, "elements": [
        {"from": [lo, lo, 0], "to": [hi, hi, lo], "faces": arm_faces}]})
    parts = [{"apply": {"model": rid("block/data_cable_core")}}]
    for direction, rotation in FACINGS.items():
        parts.append({"when": {direction: "true"}, "apply": {"model": rid("block/data_cable_arm"), **rotation}})
    write(assets / "blockstates" / "data_cable.json", {"multipart": parts})
    write(assets / "models" / "item" / "data_cable.json", {"parent": "minecraft:block/block", "textures": cable, "elements": [
        {"from": [lo, lo, 0], "to": [hi, hi, 16], "faces": {
            "north": {"uv": [lo, lo, hi, hi], "texture": "#cable"}, "south": {"uv": [lo, lo, hi, hi], "texture": "#cable"},
            "east": {"uv": [0, lo, 16, hi], "texture": "#cable"}, "west": {"uv": [0, lo, 16, hi], "texture": "#cable"},
            "up": {"uv": [lo, 0, hi, 16], "texture": "#cable"}, "down": {"uv": [lo, 0, hi, 16], "texture": "#cable"}}}]})
    write(assets / "items" / "data_cable.json", {"model": {"type": "minecraft:model", "model": rid("item/data_cable")}})

    # Sensor and relay: one model per channel colour (the relay also lit and unlit).
    sensor_variants, relay_variants = {}, {}
    for color in CHANNEL_COLORS:
        lamp = rid(f"block/control_lamp_{color}")
        write(models / f"sensor_{color}.json", {"parent": "minecraft:block/block",
                                                 "textures": {**base_textures, "lamp": lamp},
                                                 "elements": sensor_model()})
        for facing, rotation in FACINGS.items():
            sensor_variants[f"channel={color},facing={facing}"] = {"model": rid(f"block/sensor_{color}"), **rotation}
        for powered in (False, True):
            suffix = "on" if powered else "off"
            write(models / f"relay_{color}_{suffix}.json", {
                "parent": "minecraft:block/block",
                "textures": {**base_textures, "lamp": lamp, "state": rid(f"block/control_relay_{suffix}")},
                "elements": relay_model()})
            relay_variants[f"channel={color},powered={str(powered).lower()}"] = {"model": rid(f"block/relay_{color}_{suffix}")}
    write(assets / "blockstates" / "sensor.json", {"variants": sensor_variants})
    write(assets / "blockstates" / "relay.json", {"variants": relay_variants})
    write(assets / "items" / "sensor.json", {"model": {"type": "minecraft:model", "model": rid("block/sensor_white")}})
    write(assets / "items" / "relay.json", {"model": {"type": "minecraft:model", "model": rid("block/relay_white_off")}})

    # Logic controller: four horizontal facings.
    write(models / "logic_controller.json", {"parent": "minecraft:block/block", "textures": {
        **base_textures, "vent": rid("block/el_vent"), "screen": rid("block/el_screen_cyan_on"),
        "keys": rid("block/control_keys")}, "elements": controller_model()})
    write(assets / "blockstates" / "logic_controller.json", {"variants": {
        f"facing={facing}": {"model": rid("block/logic_controller"), **rotation}
        for facing, rotation in FACINGS.items() if facing not in ("up", "down")}})
    write(assets / "items" / "logic_controller.json", {"model": {"type": "minecraft:model", "model": rid("block/logic_controller")}})

    # Control monitor: a loose panel shows a standby pattern; panels of a formed screen are plain dark glass.
    monitor_variants = {}
    for part, screen in (("loose", "control_monitor_loose"), ("formed", "control_monitor_glass")):
        write(models / f"control_monitor_{part}.json", {"parent": "minecraft:block/block", "textures": {
            **base_textures, "screen": rid(f"block/{screen}")}, "elements": monitor_model()})
    for facing, rotation in FACINGS.items():
        if facing in ("up", "down"):
            continue
        for part in range(7):
            monitor_variants[f"facing={facing},part={part}"] = {
                "model": rid(f"block/control_monitor_{'loose' if part == 0 else 'formed'}"), **rotation}
    write(assets / "blockstates" / "control_monitor.json", {"variants": monitor_variants})
    write(assets / "items" / "control_monitor.json", {"model": {"type": "minecraft:model", "model": rid("block/control_monitor_loose")}})

    # Alarm: one model per channel colour, lit and unlit.
    alarm_variants = {}
    for color in CHANNEL_COLORS:
        for powered in (False, True):
            suffix = "on" if powered else "off"
            write(models / f"alarm_{color}_{suffix}.json", {"parent": "minecraft:block/block", "textures": {
                **base_textures, "lamp": rid(f"block/control_lamp_{color}"), "dome": rid(f"block/control_alarm_{suffix}")},
                "elements": alarm_model()})
            alarm_variants[f"channel={color},powered={str(powered).lower()}"] = {"model": rid(f"block/alarm_{color}_{suffix}")}
    write(assets / "blockstates" / "alarm.json", {"variants": alarm_variants})
    write(assets / "items" / "alarm.json", {"model": {"type": "minecraft:model", "model": rid("block/alarm_red_off")}})

    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{item}")}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})

    lang[f"message.{MOD}.alarm.on"] = "Alarm sounding (channel %s)"
    lang[f"message.{MOD}.alarm.off"] = "Alarm quiet (channel %s)"
    lang[f"message.{MOD}.control_remote.bound"] = "Remote bound to the logic controller at %s, %s, %s"
    lang[f"message.{MOD}.control_remote.unbound"] = "Use the remote on a logic controller first"
    lang[f"message.{MOD}.control_remote.out_of_range"] = "The bound controller is out of range"
    lang[f"message.{MOD}.control_remote.on"] = "Channel %s switched ON"
    lang[f"message.{MOD}.control_remote.off"] = "Channel %s switched OFF"
    lang[f"tooltip.{MOD}.control_remote.channel"] = "Channel: %s"
    lang[f"tooltip.{MOD}.control_remote.bound"] = "Bound to the controller at %s, %s, %s"
    lang[f"tooltip.{MOD}.control_remote.unbound"] = "Not bound: use it on a logic controller"
    lang[f"message.{MOD}.channel"] = "Channel: %s"
    lang[f"message.{MOD}.sensor"] = "Sensor: %s%% full, channel %s"
    lang[f"message.{MOD}.sensor.none"] = "Sensor: nothing to read here (channel %s)"
    lang[f"message.{MOD}.relay.on"] = "Relay on (channel %s)"
    lang[f"message.{MOD}.relay.off"] = "Relay off (channel %s)"
    lang[f"screen.{MOD}.logic_controller.if"] = "IF"
    lang[f"screen.{MOD}.logic_controller.then"] = "THEN"
    lang[f"screen.{MOD}.logic_controller.on"] = "ON"
    lang[f"screen.{MOD}.logic_controller.off"] = "OFF"

    recipes = data / "recipe"
    write(recipes / "data_cable.json", shaped(condition, ["PPP", "FFF", "PPP"], {
        "P": rid("plastic_sheet"), "F": rid("optical_fibre")}, "data_cable", 12))
    write(recipes / "sensor.json", shaped(condition, [" G ", "CXC", "PPP"], {
        "G": "minecraft:glass_pane", "C": rid("copper_wire"), "X": rid("microchip"), "P": rid("plastic_sheet")}, "sensor", 2))
    write(recipes / "relay.json", shaped(condition, [" R ", "CXC", "PPP"], {
        "R": "minecraft:repeater", "C": rid("copper_wire"), "X": rid("microchip"), "P": rid("plastic_sheet")}, "relay", 2))
    write(recipes / "logic_controller.json", shaped(condition, ["SNS", "XQX", "SCS"], {
        "S": "#c:plates/steel", "N": rid("network_terminal"), "X": rid("microchip"), "Q": rid("processor"),
        "C": rid("data_cable")}, "logic_controller", 1))
    write(recipes / "control_monitor.json", shaped(condition, ["PGP", "GXG", "PCP"], {
        "P": rid("plastic_sheet"), "G": "minecraft:glass_pane", "X": rid("microchip"), "C": rid("data_cable")},
        "control_monitor", 6))
    write(recipes / "alarm.json", shaped(condition, [" D ", "NXN", "PCP"], {
        "D": "minecraft:orange_stained_glass", "N": "minecraft:note_block", "X": rid("microchip"),
        "P": rid("plastic_sheet"), "C": rid("data_cable")}, "alarm", 1))
    write(recipes / "control_remote.json", shaped(condition, ["  A", " X ", "PBP"], {
        "A": rid("copper_wire"), "X": rid("processor"), "P": rid("plastic_sheet"), "B": "minecraft:stone_button"},
        "control_remote", 1))


def shaped(condition, pattern, key, result, count):
    return {"fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "redstone",
            "pattern": pattern, "key": key, "result": {"id": rid(result), "count": count}}


# ------------------------------------------------------------------ textures (16x16)

GRAPHITE = [(22, 25, 31), (32, 36, 44), (44, 49, 59), (62, 68, 80)]
CYAN = [(18, 72, 86), (28, 128, 148), (56, 200, 218), (144, 242, 250)]


def _img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb) + (255,)


def data_cable():
    """A dark plastic sheath with a cyan optical core running along it, and fine sheath noise."""
    rng = random.Random(3601)
    img = _img()
    for y in range(16):
        for x in range(16):
            c = GRAPHITE[2] if rng.random() < 0.8 else GRAPHITE[1]
            if 7 <= y <= 8:
                c = CYAN[2] if (x + y) % 5 else CYAN[3]
            elif y in (6, 9):
                c = CYAN[0]
            img.putpixel((x, y), c + (255,))
    return img


def lamp(rgb):
    """A round channel lamp in a graphite bezel, shaded from the channel colour with a highlight."""
    img = _img()
    for y in range(16):
        for x in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d < 40:
                f = 1.15 if d < 8 else 1.0 if d < 22 else 0.78
                img.putpixel((x, y), _shade(rgb, f))
            elif d < 56:
                img.putpixel((x, y), GRAPHITE[3] + (255,))
            else:
                img.putpixel((x, y), GRAPHITE[1] + (255,))
    for x, y in ((5, 5), (6, 5), (5, 6)):
        img.putpixel((x, y), (255, 255, 255, 255))
    return img


def relay_state(on):
    """The relay's indicator: a bar, deep red when off and bright redstone red when on."""
    img = _img()
    lit = [(120, 20, 16), (220, 40, 30), (255, 110, 90)]
    dark = [(40, 10, 10), (64, 16, 14), (84, 24, 20)]
    ramp = lit if on else dark
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            c = GRAPHITE[3] if edge else ramp[2] if (on and 5 <= x <= 10 and 5 <= y <= 10) else ramp[1] if not edge else ramp[0]
            img.putpixel((x, y), c + (255,))
    return img


def keys():
    """A strip of small dark keys with cyan legends."""
    rng = random.Random(3602)
    img = _img()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), GRAPHITE[0] + (255,))
    for row in range(3):
        for col in range(5):
            x0, y0 = 1 + col * 3, 2 + row * 5
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 2):
                    img.putpixel((x, y), GRAPHITE[3] + (255,))
            if rng.random() < 0.7:
                img.putpixel((x0, y0 + 1), CYAN[2] + (255,))
    return img


def monitor_glass():
    """A formed screen's panel: dark cyan-black glass with a faint scanline; no border, so six tile into one."""
    img = _img()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (8, 26, 30, 255) if y % 4 else (12, 34, 40, 255))
    return img


def monitor_loose():
    """A loose panel: a bezel round dark glass showing a standby pattern."""
    img = monitor_glass()
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), GRAPHITE[3] + (255,))
    for x in range(4, 12):
        img.putpixel((x, 7), CYAN[1] + (255,))
    for x in (4, 7, 10):
        img.putpixel((x, 9), CYAN[2] + (255,))
    return img


def alarm_dome(on):
    """The klaxon's dome: amber glass, glowing when sounding."""
    img = _img()
    ramp = [(200, 110, 20), (250, 170, 40), (255, 230, 140)] if on else [(90, 50, 14), (130, 76, 20), (170, 110, 40)]
    for y in range(16):
        for x in range(16):
            c = ramp[2] if (x - 5) ** 2 + (y - 4) ** 2 < 6 else ramp[1] if x in range(3, 13) else ramp[0]
            img.putpixel((x, y), c + (255,))
    return img


def control_remote():
    """64x64 (tools/hd_art.py): a handheld remote: a dark casing, a cyan screen, two buttons and a whip antenna."""
    import hd_art as hd
    from hd_art import Canvas
    c = Canvas()
    c.capsule((40, 6), (36, 24), 1.2, hd.CHROME)  # antenna
    c.disc((40, 5), 2.2, hd.RED, 0.8)
    c.box((30, 38), 13, 22, 0, hd.GUNMETAL, bevel=4.0)
    c.box((30, 28), 9, 7, 0, hd.GLASS, bevel=1.5)  # screen
    for y in (26, 29):
        c.line((24, y), (36, y), (120, 240, 250))
    c.disc((24, 44), 3.4, hd.RED, 0.6)
    c.disc((36, 44), 3.4, hd.OLIVE, 0.6)
    c.box((30, 54), 9, 2.2, 0, hd.RUBBER, bevel=1.0)  # grip
    return c.finish()


HD_ITEMS = {"control_remote": control_remote}


def draw_all(save):
    save(monitor_glass(), "block", "control_monitor_glass")
    save(monitor_loose(), "block", "control_monitor_loose")
    save(alarm_dome(False), "block", "control_alarm_off")
    save(alarm_dome(True), "block", "control_alarm_on")
    for item, draw in HD_ITEMS.items():
        save(draw(), "item", item)
    save(data_cable(), "block", "data_cable")
    for color, rgb in CHANNEL_COLORS.items():
        save(lamp(rgb), "block", f"control_lamp_{color}")
    save(relay_state(False), "block", "control_relay_off")
    save(relay_state(True), "block", "control_relay_on")
    save(keys(), "block", "control_keys")
