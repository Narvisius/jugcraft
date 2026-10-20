"""The supplied glyph vocabulary in Jugcraft's existing visual composition grammar."""
from nouveau_content import CATALOG, LANG, id_of

DELIVERIES = {"self": ("here", 0), "touch": ("touch", 6), "projectile": ("ray", 24),
              "pantomime": ("ray", 24), "underfoot": ("here", 0), "rune": ("touch", 6), "orbit": ("here", 0), "linger": ("touch", 6), "wall": ("touch", 6)}
MODIFIERS = {"amplify": ("magnitude", 50), "dampen": ("dampen", 50), "extend_time": ("duration", 100),
             "duration_down": ("duration_down", 50), "aoe": ("radius", 1), "fortune": ("fortune", 1),
             "extract": ("extract", 1), "sensitive": ("sensitive", 1), "randomize": ("randomize", 1),
             "pierce": ("pierce", 1), "accelerate": ("accelerate", 1), "decelerate": ("decelerate", 1), "split": ("split", 1)}
SELECTIONS = {"burst": ("creatures", 3, 8)}
TERMINATIONS = {}
STATUS = {"slowfall": ("slow_falling", 0, False),
          "wither": ("wither", 0, True)}
HARMFUL = {"harm", "hex", "wither", "snare", "ignite", "bubble", "cold_snap", "flare", "crush", "cut", "fangs",
           "explosion", "lightning", "wind_shear", "wind_burst", "pull", "gravity"}
WORLD = {"break", "grow", "harvest", "evaporate", "fell", "craft", "ender_inventory", "place_block", "toss",
         "pickup", "redstone_signal", "intangible", "phantom_block", "smelt", "animate_block"}


def write_all(write, assets, data, lang):
    research = {"schema": 1, "principle": "radiance", "tradition": "lampwrights", "stage": "initiate",
                "icon": "jugcraft:nouveau_source_gem", "requires": [],
                "states": {stage: {"any": [{"type": "examine", "specimens": "jugcraft:nouveau_source_gem"}]} for stage in ("encountered", "observed", "understood", "mastered")},
                "unlocks": {}}
    write(data / "concordance/research/nouveau_arcana.json", research)
    lang["research.jugcraft.nouveau_arcana"] = "Jug Nouveau Arcana"
    requirement = {"research": "jugcraft:nouveau_arcana", "state": "encountered"}
    for item, entry in CATALOG["items"].items():
        if entry["kind"] != "glyph": continue
        name = item.removeprefix("glyph_")
        definition = {"schema": 1, "requires": requirement, "capacity": 1, "focus": 1}
        if name in DELIVERIES:
            form, distance = DELIVERIES[name]
            slot, body = "delivery", {"form": form, "range": distance}
        elif name in MODIFIERS:
            aspect, amount = MODIFIERS[name]
            slot, body = "modifier", {"aspect": aspect, "amount": amount}
        elif name in SELECTIONS:
            pick, radius, targets = SELECTIONS[name]
            slot, body = "selection", {"pick": pick, "radius": radius, "targets": targets}
        elif name in TERMINATIONS:
            pulses, interval = TERMINATIONS[name]
            slot, body = "termination", {"pulses": pulses, "interval": interval}
        else:
            slot = "operation"
            body = {"principle": "radiance", "intent": "harmful" if name in HARMFUL else "helpful"}
            if name in STATUS:
                status, amplifier, harmful = STATUS[name]
                body.update(effect="status", status="minecraft:" + status, magnitude=amplifier, duration=200)
            elif name == "harvest": body.update(effect="harvesting")
            else: body.update(effect="nouveau_block" if name in WORLD else "nouveau", status="jugcraft:nouveau/" + name, magnitude=4 if name in {"harm", "heal"} else 2, duration=20 if name=="delay" else 200)
            definition["focus"] = 0 if name == "cut" else 2
        definition.update(slot=slot)
        definition[slot] = body
        key = "nouveau_" + name
        write(data / "concordance/component" / (key + ".json"), definition)
        lang["component.jugcraft." + key] = entry["name"]
        lang["component.jugcraft." + key + ".description"] = LANG.get("ars_jimbaux.glyph_desc." + item, LANG.get("ars_jimbaux.glyph_desc." + name, entry["name"]))
    for name, entry in CATALOG["items"].items():
        if entry["kind"] not in {"spellbook", "instrument"}: continue
        tier = 0 if name.startswith("novice") else 2 if name.startswith(("archmage", "creative")) else 1
        write(data / "concordance/instrument" / (entry["id"] + ".json"), {"schema": 1, "item": "jugcraft:" + entry["id"],
              "capacity": [16, 32, 64][tier], "targets": [32, 48, 64][tier], "work": [160, 256, 384][tier], "branches": 2, "duration": 2400})
    # Native tags and data-driven instrument selection share the same identity.
    import json
    for tag in ("concordance_instruments", "concordance/instrument"):
        path = data / "tags/item" / (tag + ".json")
        value = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
        value["values"] = list(dict.fromkeys(value["values"] + ["jugcraft:" + e["id"] for e in CATALOG["items"].values() if e["kind"] in {"instrument", "spellbook"}]))
        write(path, value)
    for kind in ("nouveau", "nouveau_block", "nouveau_creature"):
        lang["compose.jugcraft.explain.operation." + kind] = "%s"
    for aspect, _ in MODIFIERS.values(): lang["compose.jugcraft.aspect." + aspect] = aspect.replace("_", " ").title()


def registry_definitions():
    """Expose the same generated definitions to the shared Concordance catalog/checker."""
    from pathlib import Path
    captured, labels = {}, {}
    write_all(lambda path, value: captured.update({path.as_posix(): value}), Path('__nouveau_catalog__/assets'), Path('__nouveau_catalog__/data'), labels)
    research = captured['__nouveau_catalog__/data/concordance/research/nouveau_arcana.json']
    research = {**research, 'name': 'Jug Nouveau Arcana', 'states': {key:value['any'] for key,value in research['states'].items()}}
    research.pop('schema', None)
    research['advancements'] = {state: ('Jug Nouveau Arcana', 'Use a spell instrument or glyph; its workings are available immediately.', 'task', 'jugcraft:nouveau_source_gem') for state in research['states']}
    components = {Path(path).stem: {**value, 'name': labels['component.jugcraft.'+Path(path).stem], 'text': labels['component.jugcraft.'+Path(path).stem+'.description']} for path,value in captured.items() if '/component/' in path}
    for value in components.values(): value.pop('schema', None)
    return {'nouveau_arcana':research}, components
