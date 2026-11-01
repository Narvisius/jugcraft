"""Shared starter lessons: practical guidance, not quests, rewards or unlock gates."""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src/main/resources/jugcraft/starter_guidance.json"
def link(title, page): return {"title": title, "page": page}
def route(title, name): return {"title": title, "route": name}
def topic(title, text, items=(), recipes=(), links=()):
    return dict(title=title, text=text, items=list(items), recipes=list(recipes), links=list(links))
LESSONS = {
"technology_start": [
 topic("Start here: your first working machine", [
  "Goal: power an Electric Furnace, then add a Crusher to process ore. You can start without magic, a boss kill or completing a quest.",
  "Use a crafting table and ordinary furnace first. Collect wood, stone, fuel, copper, tin, zinc and redstone. The recipe grids below come from this server; use them for exact quantities.",
  "The inventory counts are a checklist of what you carry now, not permanent quest completion. Trading for parts is an equally valid route."], links=[route("Browse all technology chapters", "technology")]),
 topic("1. Prepare bronze and the casing", [
  "Make Bronze Blend from copper and tin, then smelt the blend into Bronze Ingots. Smelt zinc for the casing. This bootstrap does not need a powered Alloy Smelter.",
  "The Machine Casing is a component used to craft equipment; placing a casing alone does not make a working machine."],
  ["jugcraft:bronze_ingot", "jugcraft:zinc_ingot", "jugcraft:machine_casing"],
  ["jugcraft:bronze_blend", "jugcraft:machine_casing"]),
 topic("2. Generate and connect power", [
  "Craft Copper Cable and a Coal Generator. Put coal or charcoal in the generator's fuel slot. Connect cable to its power port and the receiving machine's power socket.",
  "JE is electrical energy; JE/t is energy per game tick. A generator, a connected cable and one consumer are enough to begin. A Battery Box is optional at this stage."],
  ["jugcraft:coal_generator", "jugcraft:copper_cable"], ["jugcraft:copper_cable", "jugcraft:coal_generator"]),
 topic("3. Make one useful output", [
  "Connect an Electric Furnace and put a smeltable item in its input. Leave output space free. Wait for the progress bar and collect the result: this is your first working powered production line.",
  "If nothing happens, check fuel, available JE, the machine's actual socket, a valid input and free output slots. A cable touching a decorative panel is not necessarily touching an input port."],
  ["jugcraft:electric_furnace"], ["jugcraft:electric_furnace"]),
 topic("4. Expand only where useful", [
  "A Crusher improves ore yield. The Metal Press and Wire Drawer supply parts; the Circuit Assembler supports electronics. A Battery Box buffers power and outputs through its front.",
  "Steel is a parallel capability: Coke Oven plus Steel Foundry. Mechanical shafts and logistics are useful specialties, not a requirement to finish every chapter before advancing.",
  "Choose one next goal: easier ore processing, an automated farm, storage, chemistry or transport. Larger factories are optional throughput upgrades."],
  links=[route("Choose a technology specialty", "technology"), link("Digital storage and automation", "jimta_network")]),
 topic("5. Share materials with magic", [
  "Copper, glass, crops and storage are useful to magical workshops too. Trade surplus components rather than requiring every player to build both branches.",
  "A Ley Pylon and compatible equipment connect workshop charge to magic. Read the relevant apparatus entry before building; electrical JE, Ley Charge and transmutation essence are different resources."],
  links=[link("Explore the magic branches", "magic_start"), link("Find a town mentor", "mentors")])],
"magic_start": [
 topic("Start here: choose your kind of magic", [
  "Magic is not one compulsory ladder. Choose spellcasting, magical workshops and helpers, or transmutation and equipment. You may combine them later.",
  "Owning usable equipment is enough to use it; this guide adds no research gate. Existing research explains discoveries and advances the Concordance. No new items are granted by reading or consulting a mentor."],
  links=[link("First Light: live step-by-step guidance", "discover"), link("Spell composition and controls", "spellcraft")]),
 topic("1. Try a wand", [
  "Collect a luminous specimen such as amethyst, glow berries or glow lichen, plus a stick and copper. Craft the Initiate's Wand using this server's recipe.",
  "Sneak-use your instrument to open the constellation composer. Choose a delivery and an available effect, review its cost, and save. Use the instrument to cast. Start with a simple light spell rather than a large combat combination.",
  "For the research route, sneak-use a luminous specimen to examine it. First Light below shows the server's current requirements and your own progress."],
  ["jugcraft:initiate_wand"], ["jugcraft:initiate_wand"], [link("Open First Light", "discover"), link("Open Spellcraft", "spellcraft")]),
 topic("2. Build a magical workshop", [
  "The Lampwright's Bench supports luminous research. Jug Nouveau offers another entry: a Novice Spell Book and Imbuement Chamber, then Source Gems and a Dominion Wand.",
  "An Imbuement Chamber works slowly without charge. Source Jars, Sourcelinks and Dominion Wand connections support powered workshops and helpers. Use the workshop guide for exact recipes, links and setup."],
  ["jugcraft:lampwright_bench", "jugcraft:nouveau_novice_spell_book"], ["jugcraft:lampwright_bench"], [link("Jug Nouveau: stations, helpers and glyphs", "nouveau")]),
 topic("3. Magic without casting", [
  "Transmutation turns valued supplies into essence and recreates learned permitted materials at their value. It is a building convenience, not a way to print valuables.",
  "Dark/Red Matter equipment, utility artifacts and collectors are a separate specialty. They do not require choosing every spell school first."],
  links=[link("Transmutation and magical equipment", "alchemy_start")]),
 topic("4. Explore schools and workshops", [
  "The Concordance reference contains the current traditions and their apparatus. The imported Workshop Guides cover spellbooks and the other supplied systems. Read these from this same Encyclopedia.",
  "Encounter records appear as you discover their content. An empty bestiary is not a requirement to fight a boss before using ordinary tools or magic."],
  links=[route("All magic reference chapters", "magic"), link("Spellbooks and workshop manuals", "jimta_guides"), link("Expeditions and discovered bosses", "expedition")]),
 topic("5. Pick a first-session goal", [
  "Caster: make a wand and cast a small light spell. Gardener: build an Imbuement Chamber and make a Source Gem. Alchemist: read the Scale and transmutation chain before spending rare materials.",
  "Focus fuels casting; Ley Charge supports compatible apparatus; essence pays for transmutation. They are not interchangeable just because each is magical."], links=[link("Find a mentor", "mentors")])],
"alchemy_start": [
 topic("Start here: materials and convenience", [
  "This branch is useful without spellcasting. Start by inspecting the Assayer's Scale and Philosopher's Stone recipes. Gold, copper, iron, smooth stone slabs and dark oak begin the Scale chain; later equipment has its own material costs."],
  ["jugcraft:assayers_scale", "jugcraft:philosophers_stone"], ["jugcraft:assayers_scale", "jugcraft:philosophers_stone"]),
 topic("The table comes before the portable tablet", [
  "The Transmutation Table requires Dark Matter, obsidian and stone. The portable Tablet is an upgrade made from the table and more advanced materials, not a free starting item.",
  "Use the table's item menu to dissolve supported supplies, learn permitted patterns and select what to create. Creation spends the displayed essence value with no extra surcharge."],
  ["jugcraft:transmutation_table", "jugcraft:transmutation_tablet"], ["jugcraft:transmutation_table", "jugcraft:transmutation_tablet"]),
 topic("Protect the server economy", [
  "Learning and output permission are separate. Building and convenience outputs are allowed; valuable metals, powerful equipment and restricted outputs stay blocked even if you previously learned them.",
  "Condensers and density gems follow the same convenience-output policy. Keep rare gear out of a dissolve slot unless you genuinely want to consume it. Saved balances and learned patterns belong to you."], links=[route("Read magical equipment and alchemy references", "magic")]),
 topic("Choose an upgrade", [
  "Collectors and relays supply essence; condensers create an allowed selected output. Matter tools and utility artifacts provide different jobs. Start with a useful building supply or one tool rather than trying to complete the whole collection.",
  "Consult Ivo at Curios for this lesson. Learning from a mentor costs nothing and never consumes inventory."], links=[link("Back to magic choices", "magic_start")])],
"mentors": [
 topic("Free help, no new unlock requirements", [
  "Town mentors open these same Encyclopedia lessons. You can read them anywhere with the Encyclopedia key or inventory button; visiting town is optional.",
  "At participating shops, click Ask mentor. Sneak-interact with the resident for the lesson without opening their shop. Sleeping residents rest; the Encyclopedia remains available at all hours." ]),
 topic("Kester Barrow - General Store", ["Ask Kester about your first powered machine, fuel, cable connections and what to build next. Normal interaction still opens the shop during opening hours."], links=[link("Kester's technology lesson", "technology_start")]),
 topic("Linnea Barrow - Florist", ["Ask Linnea how to start magic, choose a first instrument, or build a magical garden and workshop. Her shop remains unchanged."], links=[link("Linnea's magic lesson", "magic_start")]),
 topic("Ivo Brook - Curios", ["Ask Ivo about the Scale, transmutation, essence and useful magical gear. No free valuables or mentor rewards are added."], links=[link("Ivo's alchemy lesson", "alchemy_start")]),
 topic("Osric Finch - Town Hall", ["Speak to the mayor for this directory. Academy instructors retain their existing lessons; sneak-interact with the Radiance instructor for the shared magic starting guide."], links=[route("Technology reference", "technology"), route("Magic reference", "magic")])]
}
def generate(write): write(PATH, LESSONS)
if __name__ == "__main__":
    PATH.parent.mkdir(parents=True,exist_ok=True)
    PATH.write_text(json.dumps(LESSONS,indent=2)+"\n",encoding="utf-8")
