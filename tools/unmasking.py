"""Unmasking (docs/features/costumed-villains-unmasking.md): when a player beats one of the four costumed villains, the
costume comes off and one of three crooks stands caught where it fell, named, with an excuse, until they are led away.
The costume's mask drops as a trophy and the player earns "Mystery Solved".

Java: creatures/scary/Unmasking (the death hook, the masks) and UnmaskedCrook (the crook); client/UnmaskedCrookRenderer
draws the crook as a townsperson. The names and excuses are original, in the spirit of the cartoons the villains are fan
homages to; none is quoted from them.
"""
import item_icons

MOD = "jugcraft"
# How many crooks each villain may turn out to be, and how long one stands caught (ticks). Keep in step with Unmasking
# and UnmaskedCrook (tools/check_mod_data.py checks them).
CROOKS = 3
CAUGHT_TICKS = 200
ADVANCEMENT = "mystery_solved"

# Each villain (its entity id): its mask (item id, name), and the crooks it may turn out to be (name, excuse).
VILLAINS = {
    "space_kook": (("spectral_cosmonaut_helmet", "Spectral Cosmonaut's Helmet"), [
        ("Old Dr. Pemberton, the observatory keeper", "I only wanted the hilltop dark and quiet, for my stars!"),
        ("Mortimer Gale, the rocket-fair barker", "Nobody pays to see a rocket with a real space ghost about. So I was the ghost!"),
        ("Hollis Crane, the moon-rock dealer", "Folk sell their land cheap when a spaceman screams on it at midnight."),
    ]),
    "captain_cutler": (("drowned_diver_helmet", "Drowned Diver's Helmet"), [
        ("Silas Brine, the harbourmaster", "Frighten the fishing boats off, and the wreck's gold is mine to dredge!"),
        ("Wendell Marsh, the bait-shop owner", "With the bay haunted, they all had to buy my lucky lures!"),
        ("Captain Abel Fenwick, retired", "Retired? Me? Not while there's salvage under that water!"),
    ]),
    "phantom_shadow": (("emerald_phantom_mask", "Emerald Phantom's Mask"), [
        ("Agatha Thornby, the tour guide", "A ghost sells twice the tickets. I was only helping the tourist trade along!"),
        ("Rufus Pike, the antique dealer", "Scare the family out, and the manor's heirlooms go cheap at auction!"),
        ("Cornelius Vane, the stage magician", "Chains, smoke and a green lamp: my finest illusion, and you've ruined it!"),
    ]),
    "black_knight": (("onyx_warden_helm", "Onyx Warden's Helm"), [
        ("Gerald Ashby, the museum curator", "With the armour walking the halls, nobody noticed the paintings going missing!"),
        ("Basil Crumb, the castle groundskeeper", "Forty years raking these grounds, and the treasure was under my rake all along!"),
        ("Lady Mildred Hartwell", "My ancestors' castle, my ancestors' gold. A little haunting kept the surveyors away!"),
    ]),
}

ITEMS = {mask: name for (mask, name), _ in VILLAINS.values()}


def write_all(write, assets, data, lang):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    lang[f"entity.{MOD}.unmasked_crook"] = "Unmasked Crook"
    for villain, (_, crooks) in VILLAINS.items():
        assert len(crooks) == CROOKS, villain
        for which, (name, excuse) in enumerate(crooks):
            lang[f"entity.{MOD}.unmasked_crook.{villain}.{which}"] = name
            lang[f"message.{MOD}.unmasked.{villain}.{which}"] = excuse
    lang[f"message.{MOD}.unmasked.reveal"] = "Unmasked! The %s was %s."
    lang[f"message.{MOD}.unmasked.says"] = "%s: \"%s\""
    lang[f"message.{MOD}.unmasked.led_away"] = "%s is led away."
    lang[f"tooltip.{MOD}.unmasked_mask"] = "Pulled off %s"
    lang[f"advancements.{MOD}.{ADVANCEMENT}.title"] = "Mystery Solved"
    lang[f"advancements.{MOD}.{ADVANCEMENT}.description"] = "Unmask a costumed villain"
    write(data / "advancement" / f"{ADVANCEMENT}.json", {
        "parent": "minecraft:adventure/root",
        "display": {
            "icon": {"id": f"{MOD}:spectral_cosmonaut_helmet"},
            "title": {"translate": f"advancements.{MOD}.{ADVANCEMENT}.title"},
            "description": {"translate": f"advancements.{MOD}.{ADVANCEMENT}.description"},
            "frame": "task", "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})


def draw_all(save):
    for item in ITEMS:
        save(item_icons.draw(item), "item", item)
