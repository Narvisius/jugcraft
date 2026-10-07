"""Models for the Pixel Hollows crystal cluster and the Retro Trader's arcade cabinet.

Built from the same box helpers as the machines (steampunk_models.box, cyl); textures from
pixel_hollows_textures ("ph_*" for the cave, "rt_*" for the cabinet). Overlapping boxes never share a
visible face plane. The cabinet is modelled facing north (its screen on the z = 0 side) in a 1 x 2 x 1
structure space, real-life sized: about 0.8 m wide, 0.9 m deep and 2 m tall.
"""
from steampunk_models import box, cyl

CRYSTAL, CRYSTAL_TIP = "ph_crystal", "ph_crystal_tip"


def pixel_crystal_cluster():
    """Square-faceted crystals growing up from the block below: a tall column with a stepped tip, two leaning
    side columns and two stubs (the block state turns it to grow from any face)."""
    m = [box((6, 0, 6), (10, 9, 10), CRYSTAL),
         box((6.75, 9, 6.75), (9.25, 12, 9.25), CRYSTAL_TIP),
         box((7.5, 12, 7.5), (8.5, 13, 8.5), CRYSTAL_TIP),
         box((3, 0, 9), (6, 6, 12), CRYSTAL),
         box((3.75, 6, 9.75), (5.25, 8, 11.25), CRYSTAL_TIP),
         box((10, 0, 3), (13, 5, 6), CRYSTAL),
         box((10.75, 5, 3.75), (12.25, 7, 5.25), CRYSTAL_TIP),
         box((10, 0, 10), (12, 3, 12), CRYSTAL),
         box((4, 0, 4), (6, 3, 6), CRYSTAL)]
    return m


BLACK, SIDE, PANEL, DOOR = "rt_black", "rt_side_art", "rt_control_deck", "rt_coin_door"
SCREEN, MARQUEE, BEZEL, CHROME = "rt_screen", "rt_marquee", "rt_bezel", "dp_chrome"
RED, YELLOW, BLUE = "rt_button_red", "rt_button_yellow", "rt_button_blue"


def arcade_cabinet():
    """An upright arcade cabinet: plinth, a body with a coin door, a control deck with a joystick and four
    buttons, a CRT behind a bezel framed by the side panels' cheeks, a lit marquee and a top cap."""
    side = {"*": BLACK, "east": SIDE, "west": SIDE}
    m = [box((2, 0, 3), (14, 1, 15), BLACK),
         box((2, 1, 4), (14, 13, 15), {**side, "north": DOOR + "!"}),
         # Control deck, overhanging the body.
         box((1.5, 13, 1), (14.5, 15, 8), {"*": BLACK, "up": PANEL + "!", "east": SIDE, "west": SIDE}),
         box((2, 12, 1.5), (14, 13, 4), BLACK),
         box((2, 13, 8), (14, 15, 15), side),
         # Screen housing with the side panels' cheeks forward of the bezel.
         box((2, 15, 7), (14, 27, 15), side),
         box((2, 15, 5), (3, 27, 7), side),
         box((13, 15, 5), (14, 27, 7), side),
         box((3, 15, 6.5), (13, 17, 7), BEZEL),
         box((3, 17, 6.5), (13, 26, 7), {"*": BEZEL, "north": SCREEN + "!"}),
         box((3, 26, 6.5), (13, 27, 7), BEZEL),
         # Marquee and cap.
         box((2, 27, 5), (14, 31, 15), {**side, "north": MARQUEE + "!"}),
         box((1.5, 31, 4.5), (14.5, 32, 15.5), BLACK)]
    # Joystick: chrome shaft and a red ball top. Buttons: two rows of two.
    m += cyl("y", 5, 4, 0.5, 15, 17, CHROME)
    m.append(box((4, 17, 3), (6, 19, 5), RED))
    for x, z, color in ((8, 2.5, RED), (10.5, 2.5, YELLOW), (8, 5, BLUE), (10.5, 5, RED)):
        m.append(box((x, 15, z), (x + 1.5, 15.75, z + 1.5), color))
    return m


CLUSTER_ROTATION = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180},
                    "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}
