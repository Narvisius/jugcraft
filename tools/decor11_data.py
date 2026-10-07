"""JSON resources for the eleventh batch of Halloween decorations, party games, from tools/agriculture.py: the
Jump-Scare Trap, the Costume Runway, the Judges' Table and the Best Costume Ribbon, Pumpkin Bowling (Skeleton Pins, the
Bowling Pumpkin and the Bowling Scoreboard), the Candy Cache, the Monster Mash Dance Floor, the Ghost Bell and the
Fortune Teller's Table; their names, messages, loot and tags, and the quads the client draws the moving parts from
(assets/jugcraft/decor11_quads.json).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files and the earlier
batches' (tools/decor_data.py to decor10_data.py). Everything is modelled facing north (its front toward -z); a fallen
Skeleton Pin lies with its head toward north, face up. The Costume Runway is modelled running north-south.
"""
from agriculture import JUMP_SCARE, COSTUME_CONTEST, BOWLING, CANDY_CACHE, DANCE_FLOOR, GHOST_TAG, FORTUNE_TABLE
from decor_data import MOD, HORIZONTAL, rid, turned, box, block_model, self_drop, flat_item
from decor3_data import fitted
from decor9_data import drawn, FULL

BOOLEANS = (False, True)
# A face's name once a standing pin is laid down head-north, face-up (see `laid`).
LAID_FACES = {"north": "up", "up": "north", "south": "down", "down": "south", "east": "east", "west": "west"}


def up_turned(element, side="up"):
    """`element` with its `side` face's texture turned half round (to read from the north)."""
    element["faces"][side]["rotation"] = 180
    return element


# ---------------------------------------------------------------- the jump-scare trap

SCARE_TEXTURES = {"crate": "jump_scare_trap_crate", "front": "jump_scare_trap_front", "lid": "jump_scare_trap_lid",
                  "inside": "jump_scare_trap_inside", "band": "jump_scare_trap_band", "ghost": "jump_scare_trap_ghost",
                  "face": "jump_scare_trap_face", "spring": "jump_scare_trap_spring"}
# Where the lid is hinged (pixels): along its back top edge.
SCARE_HINGE = (8, 11, 15)


def crate():
    """A battered plank crate with iron corners and a painted question mark on its front, open at the top."""
    return [box((1, 0, 1), (15, 1, 15), "#crate", textures={"up": "#inside"}),
            box((1, 1, 1), (15, 11, 2), "#crate", textures={"north": "#front", "south": "#inside"}, uvs={"north": (1, 5, 15, 15)}),
            box((1, 1, 14), (15, 11, 15), "#crate", textures={"north": "#inside"}),
            box((1, 1, 2), (2, 11, 14), "#crate", textures={"east": "#inside"}),
            box((14, 1, 2), (15, 11, 14), "#crate", textures={"west": "#inside"})] + [
        box((x, 0, z), (x + 1.5, 11.25, z + 1.5), "#band") for x in (0.75, 13.75) for z in (0.75, 13.75)]


def lid():
    return [box((1, 11, 1), (15, 12, 15), "#crate", textures={"up": "#lid", "down": "#inside"}, uvs={"up": FULL})]


def scare_ghost():
    """A sheet ghost with its arms flung up, its feet at y 0, for the spring to throw up out of the crate."""
    return [box((4.5, 0, 4.5), (11.5, 6, 11.5), "#ghost"), box((5, 6, 5), (11, 12, 11), "#ghost", textures={"north": "#face"},
                                                                    uvs={"north": FULL}),
            box((2, 7, 7), (4.5, 9, 9), "#ghost"), box((11.5, 7, 7), (14, 9, 9), "#ghost"),
            box((1.5, 9, 7.25), (3, 11, 8.75), "#ghost"), box((13, 9, 7.25), (14.5, 11, 8.75), "#ghost")]


def scare_spring():
    """A coil a block tall; the renderer squashes and stretches it."""
    return [box((7.25, 0, 7.25), (8.75, 16, 8.75), "#spring", faces=("north", "south", "east", "west"))]


# ---------------------------------------------------------------- the costume runway

RUNWAY_TEXTURES = {"carpet": "costume_runway_carpet", "edge": "costume_runway_edge", "light": "costume_runway_light"}


def runway():
    """Red pile carpet between gold-braided edges, a footlight on each edge, running north-south."""
    return [box((1.5, 0, 0), (14.5, 1, 16), "#carpet"), box((0, 0, 0), (1.5, 1.25, 16), "#edge"), box((14.5, 0, 0), (16, 1.25, 16), "#edge"),
            box((0.25, 1.25, 7.25), (1.25, 2.25, 8.75), "#light", light=15), box((14.75, 1.25, 7.25), (15.75, 2.25, 8.75), "#light", light=15)]


# ---------------------------------------------------------------- the judges' table

JUDGES_TEXTURES = {"cloth": "judges_table_cloth", "top": "judges_table_top", "brass": "judges_table_brass", "ballot": "judges_table_ballot",
                   "slot": "judges_table_ballot_slot", "card": "judges_table_card_back", "card_8": "judges_table_card_8",
                   "card_9": "judges_table_card_9", "card_10": "judges_table_card_10"}
JUDGES_CARDS = (("card_9", 3.25), ("card_10", 6.75), ("card_8", 10.25))


def judges_table(open_round):
    """A table draped in red to the floor, a brass bell and a ballot box on it, and three score cards held up to the
    runway while a round is open (lying flat otherwise)."""
    elements = [box((0, 0, 2), (16, 11, 14), "#cloth", textures={"up": "#top"}, uvs={"north": (0, 5, 16, 16), "south": (0, 5, 16, 16)}),
                box((11.5, 11, 9.5), (14, 12.5, 12), "#brass"), box((12.25, 12.5, 10.25), (13.25, 13.5, 11.25), "#brass"),
                box((2, 11, 9), (5.5, 13.5, 12), "#ballot", textures={"up": "#slot"}, uvs={"up": FULL})]
    for card, x in JUDGES_CARDS:
        if open_round:
            elements += [box((x, 11, 4), (x + 3, 15, 4.5), "#card", textures={"north": f"#{card}"}, uvs={"north": FULL, "south": FULL}),
                         box((x + 1.25, 11, 4.5), (x + 1.75, 13, 5.5), "#card")]
        else:
            elements.append(up_turned(box((x, 11, 4), (x + 3, 11.5, 8), "#card", textures={"up": f"#{card}"}, uvs={"up": FULL})))
    return elements


# ---------------------------------------------------------------- pumpkin bowling

PIN_TEXTURES = {"bone": "poseable_skeleton_bone", "skull": "poseable_skeleton_skull", "ribs": "poseable_skeleton_ribs",
                "band": "skeleton_pin_band"}


def pin():
    """A little skeleton standing to attention, as round as a bowling pin: feet, legs, pelvis, ribs with arms at its
    sides, a red ribbon at its neck and its skull."""
    return [box((5.5, 0, 6), (10.5, 1, 10), "#bone"), box((6, 1, 7), (7.5, 5, 9), "#bone"), box((8.5, 1, 7), (10, 5, 9), "#bone"),
            box((5.5, 5, 6.5), (10.5, 6.5, 9.5), "#bone"), box((7.25, 6.5, 7.5), (8.75, 7.5, 8.5), "#bone"),
            box((5.75, 7.5, 6.5), (10.25, 10.25, 9.5), "#bone", textures={"north": "#ribs", "south": "#ribs"}, uvs={"north": FULL, "south": FULL}),
            box((5, 6.5, 7.25), (5.75, 10, 8.75), "#bone"), box((10.25, 6.5, 7.25), (11, 10, 8.75), "#bone"),
            box((6.5, 10.25, 7), (9.5, 11, 9), "#band"),
            box((5.5, 11, 6), (10.5, 15, 10), "#bone", textures={"north": "#skull"}, uvs={"north": FULL})]


def laid(elements):
    """Standing pin `elements` (z from 6 to 10) laid down: head toward north, face up, its back on the floor."""
    out = []
    for element in elements:
        (x0, y0, z0), (x1, y1, z1) = element["from"], element["to"]
        faces = {LAID_FACES[side]: face for side, face in element["faces"].items()}
        out.append({**element, "from": [x0, 10 - z1, 15 - y1], "to": [x1, 10 - z0, 15 - y0], "faces": faces})
    return out


PUMPKIN_TEXTURES = {"rind": "bowling_pumpkin_rind", "holes": "bowling_pumpkin_holes", "top": "bowling_pumpkin_top",
                    "stem": "bowling_pumpkin_stem"}


def bowling_pumpkin():
    """A small, round pumpkin eight pixels across about (8, 4, 8), drilled with three finger holes like a bowling ball."""
    return [box((4.5, 0.5, 4.5), (11.5, 7.5, 11.5), "#rind", textures={"north": "#holes", "up": "#top", "down": "#top"},
                uvs={"north": FULL, "up": FULL, "down": FULL}),
            box((4, 1.5, 5), (12, 6.5, 11), "#rind", faces=("east", "west")), box((5, 1.5, 4), (11, 6.5, 12), "#rind", faces=("south",)),
            box((5, 1.5, 4), (11, 6.5, 4.5), "#holes", faces=("north",), uvs={"north": (2, 3, 14, 13)}),
            box((5, 0, 5), (11, 0.5, 11), "#top", faces=("down", "north", "south", "east", "west")),
            box((5, 7.5, 5), (11, 8, 11), "#top", faces=("up", "north", "south", "east", "west")),
            box((7.5, 8, 7.5), (8.5, 8.75, 8.5), "#stem")]


BOARD_TEXTURES = {"frame": "bowling_scoreboard_frame", "slate": "bowling_scoreboard_slate", "chalk": "bowling_scoreboard_chalk"}


def scoreboard():
    """A slate chalkboard in a wooden frame on two splayed legs, a chalk tray along its foot; the score is drawn on it."""
    f = "#frame"
    return [box((1, 0, 6), (3, 1, 10), f), box((13, 0, 6), (15, 1, 10), f), box((1.5, 1, 7.5), (2.5, 5, 8.5), f),
            box((13.5, 1, 7.5), (14.5, 5, 8.5), f), box((1, 5, 7), (15, 6, 9), f), box((1, 15, 7), (15, 16, 9), f),
            box((1, 6, 7), (2, 15, 9), f), box((14, 6, 7), (15, 15, 9), f),
            box((2, 6, 7.5), (14, 15, 8.5), "#slate", uvs={"north": (2, 1, 14, 10), "south": (2, 1, 14, 10)}),
            box((2, 5.5, 6.25), (14, 6, 7), f), box((4, 6, 6.4), (6, 6.5, 6.9), "#chalk")]


# ---------------------------------------------------------------- the candy cache

CACHE_TEXTURES = {"bark": "candy_cache_bark", "knot": "candy_cache_knot", "rings": "candy_cache_rings", "moss": "candy_cache_moss",
                  "hollow": "candy_cache_hollow"}


def cache():
    """A hollow, mossy stump with roots and a knot-hole in its front."""
    b = "#bark"
    elements = [box((2, 0, 2), (14, 10, 4), b, textures={"north": "#knot", "up": "#rings", "south": "#hollow"}, uvs={"north": (2, 6, 14, 16)}),
                box((2, 0, 12), (14, 10, 14), b, textures={"up": "#rings", "north": "#hollow"}),
                box((2, 0, 4), (4, 9, 12), b, textures={"up": "#rings", "east": "#hollow"}),
                box((12, 0, 4), (14, 9.5, 12), b, textures={"up": "#rings", "west": "#hollow"}),
                box((4, 0, 4), (12, 2, 12), "#hollow", faces=("up",)),
                box((1, 0, 5), (2, 2, 8), b), box((14, 0, 8), (15, 2.5, 11), b), box((6, 0, 1), (9, 1.5, 2), b), box((8, 0, 14), (11, 2, 15), b),
                box((2, 10, 2), (7, 10.5, 4), "#moss"), box((2, 9, 4), (4, 9.5, 7), "#moss"), box((10, 10, 12), (14, 10.5, 14), "#moss")]
    return elements


# ---------------------------------------------------------------- the ghost bell

BELL_TEXTURES = {"wood": "ghost_bell_wood", "brass": "ghost_bell_brass", "iron": "ghost_bell_iron", "ghost": "ghost_bell_ghost",
                 "face": "ghost_bell_face"}
# Where the bell hangs from the arm, and where the ghost clapper hangs inside it (pixels). Both swing side to side,
# about the arm (z).
BELL_PIVOT = (8, 15, 7.25)
CLAPPER_PIVOT = (8, 13, 7.25)


def bell_post():
    """A crooked dark post on a plank foot, its arm reaching out over the bell."""
    w = "#wood"
    return [box((4, 0, 4), (12, 1, 12), w), box((7, 1, 10.5), (9, 6, 12), w), box((7.25, 6, 10.75), (8.75, 11, 12.25), w),
            box((7, 11, 10.5), (9, 16, 12), w), box((7, 15, 6.25), (9, 16, 10.5), w), box((7.5, 14.5, 6.75), (8.5, 15, 7.75), "#iron")]


def bell():
    """The bell: a flared lip, a waisted body and a crown, hung on a loop at the pivot."""
    b = "#brass"
    return [box((5.25, 9, 4.5), (10.75, 9.75, 10), b), box((5.75, 9.75, 5), (10.25, 12.5, 9.5), b), box((6.5, 12.5, 5.75), (9.5, 13.75, 8.75), b),
            box((7.5, 13.75, 6.75), (8.5, 15, 7.75), "#iron")]


def clapper():
    """The clapper: a little sheet ghost on a cord."""
    return [box((7.85, 8, 7.1), (8.15, 13, 7.4), "#iron"), box((7, 5.5, 6.25), (9, 8, 8.25), "#ghost", textures={"north": "#face"},
                                                               uvs={"north": FULL}),
            box((7.25, 4.75, 6.5), (8.75, 5.5, 8), "#ghost")]


# ---------------------------------------------------------------- the fortune teller's table

FORTUNE_TEXTURES = {"cloth": "fortune_table_cloth", "top": "fortune_table_top", "board_left": "fortune_board_left",
                    "board_right": "fortune_board_right", "card": "fortune_card_back", "planchette": "fortune_planchette"}
# The spirit board (pixels): x from 2 to 14 and z from 6.5 to 13.5; the visitor stands to the north, so its left is east.
BOARD = (2, 6.5, 14, 13.5)
# The middle card, the one that turns (x0, z0, x1, z1), and the face-down cards either side of it.
TURNING_CARD = (6.75, 1.75, 9.25, 5.25)
SIDE_CARDS = ((3, 1.75, 5.5, 5.25), (10.5, 1.75, 13, 5.25))
TABLE_TOP = 12.0


def fortune_table():
    """A round table under a purple cloth that hangs to the floor in a gold fringe, a spirit board on it and two tarot
    cards face down; the turning card and the planchette are drawn by the client."""
    c = "#cloth"
    elements = [box((1, 0, 3), (15, 11.5, 13), c, textures={"up": "#top"}), box((3, 0, 1), (13, 11.5, 15), c, textures={"up": "#top"}),
                box((2, 0, 2), (14, 11.5, 14), c, textures={"up": "#top"}),
                box((1, 11.5, 3), (15, TABLE_TOP, 13), "#top"), box((3, 11.5, 1), (13, TABLE_TOP, 15), "#top"),
                box((2, 11.5, 2), (14, TABLE_TOP, 14), "#top")]
    x0, z0, x1, z1 = BOARD
    middle = (x0 + x1) / 2
    elements += [up_turned(box((middle, TABLE_TOP, z0), (x1, TABLE_TOP + 0.25, z1), "#board_left", uvs={"up": FULL})),
                 up_turned(box((x0, TABLE_TOP, z0), (middle, TABLE_TOP + 0.25, z1), "#board_right", uvs={"up": FULL}))]
    for cx0, cz0, cx1, cz1 in SIDE_CARDS:
        elements.append(box((cx0, TABLE_TOP, cz0), (cx1, TABLE_TOP + 0.25, cz1), "#card", uvs={"up": FULL}))
    return elements


def planchette():
    """A heart-shaped wooden planchette about (8, 8), its point toward the far side of the board (+z)."""
    y = TABLE_TOP + 0.25
    return [box((6.75, y, 6.5), (9.25, y + 0.5, 9), "#planchette", uvs={"up": FULL}), box((7.5, y, 9), (8.5, y + 0.5, 10), "#planchette")]


def turning_card():
    cx0, cz0, cx1, cz1 = TURNING_CARD
    return [box((cx0, TABLE_TOP, cz0), (cx1, TABLE_TOP + 0.25, cz1), "#card", uvs={"up": FULL})]


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"

    def item_model(name, model_name):
        write(items / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{model_name}")}})

    scare = JUMP_SCARE["block"]
    write(models / f"{scare}.json", block_model(SCARE_TEXTURES, crate(), SCARE_TEXTURES["crate"]))
    write(states / f"{scare}.json", {"variants": {
        f"facing={f},phase={p},powered={str(w).lower()}": turned(rid(f"block/{scare}"), f)
        for f in HORIZONTAL for p in ("ready", "popped", "resetting") for w in BOOLEANS}})
    write(models / f"{scare}_item.json", block_model(SCARE_TEXTURES, crate() + lid(), SCARE_TEXTURES["crate"]))
    item_model(scare, f"{scare}_item")
    lang[f"block.{MOD}.{scare}"] = JUMP_SCARE["display"]

    run = COSTUME_CONTEST["runway"]
    write(models / f"{run}.json", block_model(RUNWAY_TEXTURES, runway(), RUNWAY_TEXTURES["carpet"]))
    write(states / f"{run}.json", {"variants": {"axis=z": {"model": rid(f"block/{run}")}, "axis=x": turned(rid(f"block/{run}"), "east")}})
    item_model(run, run)
    lang[f"block.{MOD}.{run}"] = COSTUME_CONTEST["runway_display"]

    table = COSTUME_CONTEST["table"]
    for open_round in BOOLEANS:
        write(models / f"{table}{'_open' if open_round else ''}.json", block_model(JUDGES_TEXTURES, judges_table(open_round),
                                                                                   JUDGES_TEXTURES["cloth"]))
    write(states / f"{table}.json", {"variants": {f"facing={f},open={str(o).lower()}": turned(rid(f"block/{table}{'_open' if o else ''}"), f)
                                                  for f in HORIZONTAL for o in BOOLEANS}})
    item_model(table, f"{table}_open")
    lang[f"block.{MOD}.{table}"] = COSTUME_CONTEST["table_display"]
    ribbon = COSTUME_CONTEST["ribbon"]
    flat_item(root, write, ribbon)
    lang[f"item.{MOD}.{ribbon}"] = COSTUME_CONTEST["ribbon_display"]
    contest = {
        "entered": "You're in the costume contest: strut your stuff!",
        "open": "The costume contest is open for %s seconds: walk the runway in costume to enter, and use a contestant with an empty hand to vote",
        "nobody": "Nobody has walked the runway yet (%s seconds left)",
        "standings": "Votes: %s (%s seconds left)",
        "voted": "You vote for %s",
        "moved": "You move your vote to %s",
        "same": "You already vote for %s",
        "own": "You can't vote for yourself",
        "not_in_round": "%s isn't in this contest",
        "no_winner": "The costume contest is over: nobody got a vote",
        "winner": "The costume contest is over! Best costume: %s",
    }
    for key, text in contest.items():
        lang[f"message.{MOD}.{table}.{key}"] = text

    pin_block = BOWLING["pin"]
    write(models / f"{pin_block}.json", block_model(PIN_TEXTURES, pin(), PIN_TEXTURES["bone"]))
    write(models / f"{pin_block}_down.json", block_model(PIN_TEXTURES, laid(pin()), PIN_TEXTURES["bone"]))
    write(states / f"{pin_block}.json", {"variants": {f"down={str(d).lower()},facing={f}": turned(rid(f"block/{pin_block}{'_down' if d else ''}"), f)
                                                      for d in BOOLEANS for f in HORIZONTAL}})
    item_model(pin_block, pin_block)
    lang[f"block.{MOD}.{pin_block}"] = BOWLING["pin_display"]

    pumpkin = BOWLING["pumpkin"]
    write(models / f"{pumpkin}.json", block_model(PUMPKIN_TEXTURES, bowling_pumpkin(), PUMPKIN_TEXTURES["rind"]))
    item_model(pumpkin, pumpkin)
    lang[f"item.{MOD}.{pumpkin}"] = BOWLING["pumpkin_display"]
    lang[f"entity.{MOD}.{pumpkin}"] = BOWLING["pumpkin_display"]

    board = BOWLING["scoreboard"]
    write(models / f"{board}.json", block_model(BOARD_TEXTURES, scoreboard(), BOARD_TEXTURES["frame"]))
    write(states / f"{board}.json", {"variants": {f"facing={f}": turned(rid(f"block/{board}"), f) for f in HORIZONTAL}})
    item_model(board, board)
    lang[f"block.{MOD}.{board}"] = BOWLING["scoreboard_display"]
    for key, text in {"new_game": "New game: the pins are stood up", "frame": "FRAME %s", "score": "SCORE %s", "game_over": "GAME OVER",
                      "ready": "ROLL A PUMPKIN"}.items():
        lang[f"message.{MOD}.{board}.{key}"] = text

    hidden = CANDY_CACHE["block"]
    write(models / f"{hidden}.json", block_model(CACHE_TEXTURES, cache(), CACHE_TEXTURES["bark"]))
    write(states / f"{hidden}.json", {"variants": {f"facing={f},fill={n}": turned(rid(f"block/{hidden}"), f) for f in HORIZONTAL
                                                   for n in range(4)}})
    item_model(hidden, hidden)
    lang[f"block.{MOD}.{hidden}"] = CANDY_CACHE["display"]
    for key, text in {"filled": "%s of %s treats hidden in the cache", "full": "The cache is full", "count": "%s of %s treats in the cache",
                      "taken": "You find a treat!", "empty": "The cache is empty",
                      "had_one": "You've had your treat from this cache tonight"}.items():
        lang[f"message.{MOD}.{hidden}.{key}"] = text

    dance = DANCE_FLOOR["block"]
    write(models / f"{dance}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": rid(f"block/{dance}"), "side": rid(f"block/{dance}_side"), "bottom": rid(f"block/{dance}_side")}})
    write(states / f"{dance}.json", {"variants": {f"distance={d}": {"model": rid(f"block/{dance}")} for d in range(DANCE_FLOOR["reach"] + 1)}})
    item_model(dance, dance)
    lang[f"block.{MOD}.{dance}"] = DANCE_FLOOR["display"]

    bell_block = GHOST_TAG["block"]
    write(models / f"{bell_block}.json", block_model(BELL_TEXTURES, bell_post(), BELL_TEXTURES["wood"]))
    write(states / f"{bell_block}.json", {"variants": {f"facing={f},ringing={str(r).lower()}": turned(rid(f"block/{bell_block}"), f)
                                                       for f in HORIZONTAL for r in BOOLEANS}})
    write(models / f"{bell_block}_item.json", block_model(BELL_TEXTURES, bell_post() + bell() + clapper(), BELL_TEXTURES["brass"]))
    item_model(bell_block, f"{bell_block}_item")
    lang[f"block.{MOD}.{bell_block}"] = GHOST_TAG["display"]
    for key, text in {"running": "%s is the ghost! %s seconds left", "alone": "Ghost Tag needs at least two players nearby",
                      "start": "Ghost Tag! %s is the ghost: run! (%s seconds)", "tagged": "%s is the ghost now!",
                      "no_tag_back": "No tag-backs: tag someone else", "end": "Time's up! %s was the ghost last and loses"}.items():
        lang[f"message.{MOD}.{bell_block}.{key}"] = text

    fortune = FORTUNE_TABLE["block"]
    write(models / f"{fortune}.json", block_model(FORTUNE_TEXTURES, fortune_table(), FORTUNE_TEXTURES["cloth"]))
    write(states / f"{fortune}.json", {"variants": {f"facing={f}": turned(rid(f"block/{fortune}"), f) for f in HORIZONTAL}})
    write(models / f"{fortune}_item.json", block_model(FORTUNE_TEXTURES, fortune_table() + turning_card() + moved_planchette(),
                                                       FORTUNE_TEXTURES["cloth"]))
    item_model(fortune, f"{fortune}_item")
    lang[f"block.{MOD}.{fortune}"] = FORTUNE_TABLE["display"]
    for number, text in enumerate(FORTUNES, 1):
        lang[f"message.{MOD}.fortune.{number}"] = text

    quad_models = {"jump_scare_lid": drawn(lid(), SCARE_TEXTURES), "jump_scare_ghost": drawn(scare_ghost(), SCARE_TEXTURES),
                   "jump_scare_spring": drawn(scare_spring(), SCARE_TEXTURES), "bowling_pumpkin": drawn(bowling_pumpkin(), PUMPKIN_TEXTURES),
                   "ghost_bell": drawn(bell(), BELL_TEXTURES), "ghost_bell_clapper": drawn(clapper(), BELL_TEXTURES),
                   "fortune_planchette": drawn(planchette(), FORTUNE_TEXTURES)}
    write(root / "decor11_quads.json", quad_models)


def moved_planchette():
    """The planchette resting on the middle of the board, for the item model."""
    x0, z0, x1, z1 = BOARD
    dx, dz = (x0 + x1) / 2 - 8, (z0 + z1) / 2 - 8
    return [{**e, "from": [e["from"][0] + dx, e["from"][1], e["from"][2] + dz], "to": [e["to"][0] + dx, e["to"][1], e["to"][2] + dz]}
            for e in planchette()]


FORTUNES = [
    "A pumpkin is in your future. Possibly several.",
    "Beware of stairs in the dark. Also skeletons on stairs.",
    "You will find a treat where you least expect it: your own pocket.",
    "A black cat will cross your path, looking very pleased with itself.",
    "The spirits say: eat the candy corn. All of it.",
    "Someone you know is secretly a werewolf. It's fine. They're nice.",
    "Your next creeper will be a polite one. Maybe.",
    "A ghost admires your sense of style.",
    "The cards foresee a great harvest... of socks.",
    "You will soon be startled by a crate. Avoid crates.",
    "Bats will follow you around. They think you're cool.",
    "Your lucky number is 13. Obviously.",
    "Tonight the moon smiles on you. Smile back.",
    "A witch will offer you a potion. Ask what's in it first.",
    "You will win a contest of costumes, or of looking surprised.",
    "Your fortune is cloudy. Have you tried shaking it?",
    "The spirits are busy. Please leave a message after the shriek.",
    "Great things await you, just past the graveyard.",
    "A skeleton owes you a bone. Collect it.",
    "Something wonderful is coming. It might be soup.",
]


def loot(out, write):
    """Each drops itself once (the Candy Cache spills the treats in it as well, as a Candy Bowl does)."""
    for block in (JUMP_SCARE["block"], COSTUME_CONTEST["runway"], COSTUME_CONTEST["table"], BOWLING["pin"], BOWLING["scoreboard"],
                  CANDY_CACHE["block"], DANCE_FLOOR["block"], GHOST_TAG["block"], FORTUNE_TABLE["block"]):
        write(out / f"{block}.json", self_drop(block))


def tags(tags):
    for block in (JUMP_SCARE["block"], COSTUME_CONTEST["table"], BOWLING["scoreboard"], CANDY_CACHE["block"], FORTUNE_TABLE["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))
    for block in (GHOST_TAG["block"], DANCE_FLOOR["block"], BOWLING["pin"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
