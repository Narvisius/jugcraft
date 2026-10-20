"""Indexed player guidance and the complete supplied reference, adapted to Jugcraft controls."""
import re
from nouveau_content import LANG, CATALOG, id_of

TOPICS = [
 ('start','Start a magical workshop','novice_spell_book','Craft a spellbook, an Imbuement Chamber, Source Gems and a Dominion Wand. Owning an instrument is enough to use it; the constellation editor presents its glyphs without a research prerequisite.','Sneak-use a spell instrument to compose. Choose delivery, effects and augments, then save. Casting spends Focus; workshop recipes spend shared Ley Charge. Source values from the original library are converted at 100 Source to one Ley Charge.'),
 ('source','Source and Ley networks','source_jar','Place a Sourcelink near its resource and Source Jars nearby. Agronomic, Vitalic, Volcanic, Alchemical and Mycelial links turn different activity or consumables into charge.','Use the Dominion Wand on a sender, then receivers. Collector relays gather, deposit relays distribute, splitters divide output and warp relays bridge long loaded distances with transfer loss. No relay keeps chunks loaded.'),
 ('recipes','Imbuement and enchanting','enchanting_apparatus','The Imbuement Chamber makes Source Gems and essences slowly without power; Ley Charge accelerates it. The Enchanting Apparatus consumes a central reagent and matching ingredients; nearby Arcane Pedestals also supply ingredients.','Open a station to browse input and output previews, select a recipe and lock it. Keep output space free and connect charge. The Scribe’s Table produces glyphs; the Alteration Table upgrades books, armor, threads and equipment.'),
 ('glyphs','Glyphs and spell delivery','glyph_projectile','Self, Touch and Projectile begin simple spells. Orbit, Runes, Wall, Linger, Underfoot and Pantomime add different delivery patterns.','Add multiple effects and attach augments to the effect they should change. Delay pauses a sequence; Reset returns to the original target; Rewind returns a creature to its recorded state. All delayed effects share the original cast’s limits and permissions.'),
 ('equipment','Enchanter equipment and threads','enchanters_sword','The sword, bow, crossbow, shield, rod, gauntlet, mirror and eye integrate spells with their normal equipment actions. Sneak-use to edit an inscription.','Thread armor at the Alteration Table. Tiers provide more sockets; the strongest equipped copy of each thread supplies its level. Shaper’s Focus follows transformed or moving blocks; Summoning Focus strengthens summons and echoes Self and Orbit spells to them.'),
 ('helpers','Helpers and familiars','drygmy_charm','Drygmys gather creature loot, Whirlisprigs tend gardens, Wixies craft and brew, Amethyst Golems grow crystals, and Alakarkinos handles archaeology and sorting.','Use each charm on its home block. Dominion Wand links select accessible work and storage; item-frame filters apply to deliveries. Binding rituals create familiar scripts without destroying a working helper. Use the script and familiar menu to choose a companion.'),
 ('storage','Storage and couriers','bookwyrm_lectern','Bind Book Wyrms to a Storage Lectern and connect nearby real containers. Search the combined item catalog, withdraw real stacks, craft in the adjacent grid and cycle named inventory groups. Repositories expand storage through their controller.','Starbuncles carry actual items along walkable routes. Set pickup and delivery with the Dominion Wand; apply shades for dedicated transport or a Wixie hat for Potion Jar routes. Extra charms let a courier carry up to four separate stacks; packing returns its cargo and charms. Empty lecterns can link to another within 30 blocks (up to 16 hops). An unlinked adjacent chest imports automatically unless the lectern receives redstone power.'),
 ('potions','Potions and infusion','potion_jar','Potion Jars store doses, Melders combine effects, Diffusers affect a nearby area, and refillable flasks carry several doses. Splash and lingering cannons use filled flasks.','The Alchemist’s Crown selects a stored dose. Infuse draws from an inventory bottle or flask, or adjacent jars for a turret. AOE splashes at the target; Extend Time leaves a bounded lingering cloud. Harmful effects still respect claims and PvP.'),
 ('rituals','Braziers and rituals','ritual_brazier','Place a ritual tablet in a Ritual Brazier and add augment ingredients before starting it. The brazier uses charge only when work occurs; pause it to stop continuous rituals.','Link a brazier to a Brazier Relay within 15 blocks to move its effect area. Augments can widen plant rituals, change weather, strengthen challenges, or select island terrain. Islands also change local biomes where the owner has permission.'),
 ('nature','Archwood and farming','green_archwood_sapling','Explore Archwood forests for four tree colors, their pods, Sourceberries, decorative flora and magical wildlife. Grow Magebloom for fiber and magical agriculture.','Logs, wood, leaves, signs, doors, trapdoors, fences, boats and building variants use native Minecraft block behavior. Plant pods on supported faces. Mature crops can be harvested directly, by helpers, or through rituals.'),
 ('wilds','Wilden and Weald Walkers','wilden_tribute','Wilden Hunters, Stalkers and Guardians inhabit the wilds. Use the summoning ritual with their three trophies to challenge the Chimera.','Awaken Archwood into a Weald Walker that guards its area. Elemental variants have distinct attacks; Waddlers provide smaller companions. Summoned creatures have finite leases and do not duplicate their equipment as loot.'),
 ('automation','Spell automation and redstone','timer_spell_turret','Inscribed parchments configure spell turrets. Timer Turrets expose their interval; Item Detectors expose a threshold and sample filter; Spell Sensors distinguish casts from impacts.','Prisms redirect projectiles in six directions and Void Prisms absorb them. Redstone Relays transmit to linked receivers with a bounded pulse. Devices draw actual inputs, charge and output capacity; they do not use an imaginary inventory.'),
 ('travel','Warps, portals and scrying','stable_warp_scroll','Sneak-use a Warp Scroll to save a destination. A stable scroll is reusable. Drop a bound regular scroll inside an enclosed sourcestone or illusion-block frame near a full Source Jar to open a portal.','Scrying Crystals, Oculi and bound parchment provide remote views. The Enchanter’s Eye casts from a permitted linked crystal. The server validates the destination, collision, ownership and range before transporting or changing anything.'),
 ('planarium','Build inside a Planarium','planarium','Name a Planarium using an anvil or the Name glyph, then sneak-use it to enter its private building cell. Members of the owner’s party can enter.','Touch or strike a boundary to leave. Use the Dominion Wand to set an arrival point. The jar displays a bounded miniature of exposed blocks in its loaded cell; separate owners may reuse a name without sharing a dimension cell.'),
 ('illusions','Illusions and decoration','mirrorweave','Use a block on Mirrorweave to set its appearance, collision and light. Falseweave can be walked through. Ghostweave becomes hidden with Invisibility and visible again with Dispel.','Sense Magic reveals illusions. Light makes an illusion emit light. Skyweave presents a dimension-colored sky view and Dispel switches to its facade. Native signs, paintings, discs, lamps, torches, plushes and ornaments complete a workshop.'),
 ('containment','Containment jars','mob_jar','The Containment ritual stores an eligible nearby creature in a jar with its real saved data. Sneak-use empty-handed to release it where there is room.','Milk cows and mooshrooms through the jar, shear stored sheep, and let chickens produce eggs. Drygmys can study captured specimens. Bosses, players and another owner’s workers cannot be captured. A clock can be displayed inside an empty jar.')
]

def text(value):
    value=value.replace('Ars Nouveau','Jug Nouveau')
    value=re.sub(r'\$\(br2?\)', '\n\n', value)
    return re.sub(r'\$\([^)]*\)', '', value)

def codex():
    result={}
    for n,(key,title,icon,summary,detail) in enumerate(TOPICS):
        result[('nouveau',key)]={'name':title,'description':summary,'x':(n%4)*3,'y':(n//4)*3,'icon':'jugcraft:'+id_of(icon),'condition':None,'pages':[('text',title,summary),('text','In Jugcraft',detail)]}
    result[('nouveau','nouveau_arcana')]={'name':'Jug Nouveau Arcana','description':'Use your supplied instruments immediately','x':0,'y':15,'icon':'jugcraft:nouveau_source_gem','condition':None,'pages':[('text','Jug Nouveau Arcana','Owning a Jug Nouveau instrument or glyph gives access to its vocabulary. No research grind is required. Source Gems begin at the Imbuement Chamber; follow the workshop recipe previews for essences and upgrades.')] }
    grouped={}
    for key,value in LANG.items():
        match=re.match(r'ars_jimbaux\.page[0-9]*\.(.+)',key)
        if match:grouped.setdefault(match[1],[]).append((key,text(value)))
    for n,(key,pages) in enumerate(sorted(grouped.items())):
        spec=CATALOG['items'].get(key,CATALOG['blocks'].get(key,{})); title=spec.get('name',key.replace('_',' ').title())
        result[('nouveau','reference_'+key)]={'name':title,'description':'Supplied field reference and Jugcraft integration notes','x':15+(n%8)*3,'y':(n//8)*3,'icon':'jugcraft:'+spec.get('id','nouveau_annotated_codex'),'condition':None,'pages':[('text','Jugcraft controls','Use the Jug Nouveau overview for current controls. Equipment is usable when owned. Source costs become shared Ley Charge at 100:1; spell mana uses Focus. The following supplied reference describes the content’s intended behavior.')]+[('text',title,value) for _,value in pages]}
    for n,(key,spec) in enumerate((k,v) for k,v in CATALOG['items'].items() if v['kind']=='glyph'):
        pages=[('text',spec['name'],text(LANG.get('ars_jimbaux.glyph_desc.'+key,spec['name'])))]
        pages += [('text','Augments',text(value)) for name,value in LANG.items() if name.startswith('ars_jimbaux.augment_desc.'+key+'_')]
        result[('nouveau','reference_'+key)]={'name':spec['name'],'description':'Glyph and augment reference','x':42+(n%8)*3,'y':(n//8)*3,'icon':'jugcraft:'+spec['id'],'condition':None,'pages':pages}
    result.update(acquisition_entries())
    return result

def write_all(write,assets,data,lang):
    lang['encyclopedia.jugcraft.page.nouveau']='Jug Nouveau'
    write(assets.parents[1]/'jugcraft/nouveau/guide.json',[{'id':key,'title':title,'icon':id_of(icon),'summary':summary,'detail':detail} for key,title,icon,summary,detail in TOPICS])


def acquisition_entries():
    """Actual source recipe inputs and native workstation paths, available before obtaining the output."""
    import json
    from nouveau_content import DATA, adapt_recipe
    from nouveau_progression import STATIONS
    entries={}
    def add(item, title, page):
        if not item.startswith("jugcraft:"):return
        key=item.split(":",1)[1];n=len(entries)
        entry=entries.setdefault(("nouveau",key),{"name":title,"description":"Acquisition and workshop inputs","x":72+n%12*3,"y":n//12*3,"icon":item,"condition":None,"pages":[]})
        entry["pages"].append(page)
    def label(ref):
        if isinstance(ref,list):return " or ".join(label(v) for v in ref)
        if isinstance(ref,dict):return label(ref.get("item","#"+ref["tag"] if "tag" in ref else ref.get("id","")))
        return str(ref).split(":")[-1].removeprefix("nouveau_").replace("_"," ").replace("/"," / ")
    for path in sorted((DATA/"recipe").rglob("*.json")):
        folder,r=adapt_recipe(json.loads(path.read_text(encoding="utf-8")));kind=r.get("type","")
        output=r.get("result",r.get("output"))
        if kind=="caster_tome":output={"id":"jugcraft:nouveau_caster_tome"}
        if not isinstance(output,dict):continue
        item=output.get("id",output.get("item",""));title=label(item).title();rid="jugcraft:nouveau/"+path.relative_to(DATA/"recipe").with_suffix("").as_posix()
        if folder=="recipe" and kind in ("minecraft:crafting_shaped","minecraft:crafting_shapeless"):
            add(item,title,("crafting_recipe",title,"Craft this with the shown ingredients.",rid));continue
        if kind not in STATIONS:continue
        station=STATIONS[kind];inputs=[]
        for key in ("input","reagent"):
            if key in r:inputs.append(r[key])
        for key in ("inputs","ingredients","pedestalItems"):inputs.extend(r.get(key,[]))
        if kind=="caster_tome":inputs=["minecraft:book","minecraft:paper","jugcraft:nouveau_source_gem"]
        detail="At the "+label(station).title()+": "+", ".join(label(v) for v in inputs)+". "
        if kind=="imbuement":detail+="Works slowly without charge; supplying Ley Charge speeds it up. Pedestal catalysts are retained."
        elif kind=="caster_tome":detail+="Select "+(r.get("name") or path.stem.replace("_"," "))+" in the recipe picker. It spends Focus to cast; repeated augments retain the native 64-target, 384-work, six-block-radius and two-minute effect caps."
        else:detail+="Costs "+str((r.get("sourceCost",r.get("source",0))+99)//100)+" Ley Charge. Browse and lock this recipe in the workstation interface."
        add(item,title,("text",r.get("name") or title,detail))
    for color in ("blue","green","red","purple"):
        item="jugcraft:nouveau_stripped_"+color+"_archwood_log"
        add(item,label(item).title(),("text","Strip Archwood","Use an axe on a "+color+" Archwood log; the stripped block retains its axis."))
    add("jugcraft:nouveau_spell_parchment","Spell Parchment",("text","Inscribe a Spell","Put one Blank Parchment in slot 0 of a Scribe’s Table. Hold an inscribed instrument and sneak-use the table to copy its spell onto the parchment."))
    add("jugcraft:nouveau_magebloom","Magebloom",("text","Grow Magebloom","Make Magebloom seeds at the Enchanting Apparatus, plant them on farmland, then harvest the mature crop."))
    add("jugcraft:nouveau_wilden_tribute","Wilden Tribute",("text","Challenge the Chimera","Use a Wilden Summoning ritual with a Wilden Horn, Wing and Spike. Defeat the summoned Chimera to obtain its tribute."))
    return entries
