"""Batch 1: native workshop models and receipts derived from the existing powered recipe source.

Owner library catalog inspected: hammer and wirecutter references; native cuboid tools instead share
Jugcraft's existing sp_wood/sp_iron/sp_copper surfaces. No copied third-party texture or runtime.
"""
import json
from machines import machine_recipes
import model_writer

STATIONS = ['workshop_bench', 'small_ceramic_kiln', 'small_crucible_furnace']
BLOCKS = STATIONS + ['crucible_bellows']
NAMES = dict(zip(STATIONS, ['Workshop Bench', 'Small Ceramic Kiln', 'Small Crucible Furnace'])) | {
    'workshop_hammer': 'Workshop Hammer', 'wire_cutters': 'Wire Cutters', 'soldering_tool': 'Soldering Tool',
    'unfired_ceramic_lining': 'Unfired Ceramic Lining', 'basic_ceramic_lining': 'Basic Ceramic Lining',
    'unfired_ceramic_crucible': 'Unfired Ceramic Crucible', 'ceramic_crucible': 'Ceramic Crucible',
    'unfired_insulator': 'Unfired Insulator', 'crucible_heat_insert':'Crucible Heat Insert', 'crucible_bellows':'Crucible Bellows',
}

def processes():
    source = machine_recipes()
    rows = []
    allowed = {'metal_press': {f'jugcraft:{m}_plate' for m in ['iron','copper','tin','zinc','lead','bronze','brass']},
               'wire_drawer': {'jugcraft:copper_wire'}, 'circuit_assembler': {'jugcraft:basic_circuit'},
               'alloy_smelter': {'jugcraft:bronze_ingot','jugcraft:brass_ingot','jugcraft:solder_ingot'}}
    tools = {'metal_press': 'workshop_hammer', 'wire_drawer': 'wire_cutters', 'circuit_assembler': 'soldering_tool'}
    for machine, outputs in allowed.items():
        for recipe in source[machine]:
            if recipe['output'] not in outputs: continue
            name = recipe['output'].split(':')[1]
            parts = recipe.get('inputs') or [(recipe['input'], 1)]
            row = dict(type='jugcraft:starter_working', station=2 if machine == 'alloy_smelter' else 0,
                ingredients=[dict(ingredient=i, count=c) for i,c in parts],
                result=dict(id=recipe['output'], count=recipe['count']), time=recipe['ticks'],
                heated=machine in ('alloy_smelter','circuit_assembler'))
            if machine in tools: row['tool'] = dict(id='jugcraft:'+tools[machine])
            row['fabric:load_conditions'] = conditions(recipe['features'])
            rows.append((name, row))
    for name, feed, product, count, ticks in [
        ('fire_lining','unfired_ceramic_lining','basic_ceramic_lining',1,400),
        ('fire_crucible','unfired_ceramic_crucible','ceramic_crucible',1,600),
        ('fire_insulators','unfired_insulator','ceramic_insulator',2,200)]:
        rows.append((name, dict(type='jugcraft:starter_working', station=1,
            ingredients=[dict(ingredient='jugcraft:'+feed,count=2 if count==2 else 1)],
            result=dict(id='jugcraft:'+product,count=count),time=ticks,heated=True,
            **{'fabric:load_conditions': conditions(['machines'])})))
    rows.append(('silicon',dict(type='jugcraft:starter_working',station=2,
        ingredients=[dict(ingredient='minecraft:quartz',count=1),dict(ingredient='minecraft:charcoal',count=1)],
        result=dict(id='jugcraft:silicon',count=1),time=200,heated=True,high_heat=True,
        **{'fabric:load_conditions':conditions(['machines','silicon'])})))
    return rows

def conditions(features):
    return [dict(condition='jugcraft:feature_enabled',feature=f) for f in features]

def box(a,b,t): return (a,b,t)

def vessel(material, y=3, size=10):
    lo,hi = 8-size/2,8+size/2
    return [box((lo,y,lo),(hi,y+2,hi),material),
            box((lo,y+2,lo),(lo+2,y+9,hi),material), box((hi-2,y+2,lo),(hi,y+9,hi),material),
            box((lo+2,y+2,lo),(hi-2,y+9,lo+2),material), box((lo+2,y+2,hi-2),(hi-2,y+9,hi),material)]

def station_model(kind, lit, upgraded=False):
    wood,iron,brick,clay = 'sp_wood','sp_iron','sp_firebrick','clay'
    fire = 'dr_amber' if lit else 'dp_gunmetal'
    if kind == 0:
        m = [box((-16,12,1),(16,15,15),wood), box((-15,3,3),(15,4.5,13),wood)]
        for x in (-14,11):
            for z in (2,11): m.append(box((x,0,z),(x+3,12,z+3),wood))
        # End rails, iron vise with jaw gap, clay work mat and copper soldering cradle.
        m += [box((-16,10,2),(-14,12,14),wood),box((14,10,2),(16,12,14),wood),
              box((3,15,3),(12,16,12),'sp_iron_plate'),box((3,16,3),(5,18,12),iron),box((9,16,3),(11,18,12),iron),
              box((-12,15,3),(-3,15.5,12),clay),box((-1,15,7),(1,17,11),'sp_copper')]
        return m
    m = [box((1,0,1),(15,2,15),brick),box((1,2,4),(15,14,15),brick),
         box((1,2,1),(4,14,4),brick),box((12,2,1),(15,14,4),brick),
         box((4,2,3),(12,11,4),'dp_gunmetal'),box((5,3,2.7),(11,7,3),fire),
         box((1,14,1),(15,16,15),brick),box((3,9,1),(13,10,3),iron)]
    for x in (5,8,11): m.append(box((x,3,1.5),(x+.7,8,2.5),iron))
    if kind == 1:
        m += [box((4,16,4),(12,17,12),clay),box((7,17,7),(9,18,9),iron)]
    else:
        m += vessel(clay,16,12)
        m += [box((3,25,3),(13,27,5),clay),box((3,25,11),(13,27,13),clay),
              box((3,25,5),(5,27,11),clay),box((11,25,5),(13,27,11),clay),
              box((1,20,6),(3,22,10),iron),box((13,20,6),(15,22,10),iron)]
    if kind==2 and upgraded:
        m += [box((0,17,0),(16,19,2),iron),box((0,17,14),(16,19,16),iron),
              box((0,17,2),(2,19,14),iron),box((14,17,2),(16,19,14),iron)]
    return m

def bellows_model(stage):
    top=12-stage
    wood,iron,leather='sp_wood','sp_iron','balloon_leather'
    m=[box((2,0,3),(5,4,14),wood),box((11,0,3),(14,4,14),wood),
       box((1,4,3),(15,5,15),wood),box((2,5,4),(14,top,14),leather),
       box((1,top,3),(15,top+1,15),wood),box((6,6,0),(10,9,4),iron),
       box((6,6,14),(10,10,16),iron)]
    for y in range(6,top,2):
        m += [box((1.5,y,4),(2,y+.7,14),leather),box((14,y,4),(14.5,y+.7,14),leather),
              box((2,y,14),(14,y+.7,14.5),leather)]
    return m

def item_model(name):
    mat = 'clay' if name.startswith('unfired') else 'el_white'
    if name == 'crucible_heat_insert': return vessel('sp_firebrick',2,14)
    if 'crucible' in name: return vessel(mat)
    if 'lining' in name: return [box((2,3,3),(14,6,13),mat),box((2,6,3),(4,11,13),mat),box((12,6,3),(14,11,13),mat)]
    if name == 'unfired_insulator': return [box((5,3,5),(11,5,11),mat),box((6,5,6),(10,12,10),mat),box((4,7,4),(12,9,12),mat)]
    if name == 'workshop_hammer':
        return [box((7,1,7),(9,12,9),'sp_wood'),box((3,11,5),(13,15,11),'sp_iron')]
    if name == 'soldering_tool':
        return [box((6,1,6),(10,7,10),'sp_wood'),box((7,7,7),(9,13,9),'sp_copper'),box((7.5,13,7.5),(8.5,15,8.5),'sp_iron')]
    return [box((3,1,7),(6,8,9),'sp_wood'),box((10,1,7),(13,8,9),'sp_wood'),
            box((5,7,7),(11,10,9),'sp_iron'),box((4,10,7),(7,15,9),'sp_iron'),box((9,10,7),(12,15,9),'sp_iron')]

def textures(elements):
    return {**{t: ('minecraft:block/clay' if t=='clay' else 'jugcraft:block/'+t)
                for t in model_writer.texture_names(elements)}, 'particle':'jugcraft:block/sp_firebrick'}

def write_all(write, assets, data, lang):
    def craft(name, ingredients, result=None, count=1):
        write(data/'recipe'/f'starter_workshop/{name}.json', dict(type='minecraft:crafting_shapeless',
            ingredients=ingredients, result=dict(id='jugcraft:'+(result or name),count=count),
            **{'fabric:load_conditions':conditions(['machines'])}))
    def shaped(name, pattern, key):
        write(data/'recipe'/f'starter_workshop/{name}.json', dict(type='minecraft:crafting_shaped',pattern=pattern,key=key,
            result=dict(id='jugcraft:'+name), **{'fabric:load_conditions':conditions(['machines'])}))
    craft('workshop_bench',['minecraft:crafting_table']+['#minecraft:planks']*4+['minecraft:cobblestone']*2)
    shaped('workshop_hammer',['II',' S'],dict(I='minecraft:iron_ingot',S='minecraft:stick'))
    shaped('wire_cutters',['I I','S S'],dict(I='minecraft:iron_ingot',S='minecraft:stick'))
    craft('crucible_heat_insert',['minecraft:bricks']*2+['jugcraft:basic_ceramic_lining'])
    craft('crucible_bellows',['#minecraft:planks']*4+['minecraft:leather']*2+['jugcraft:iron_shaft'])
    craft('soldering_tool',['minecraft:copper_ingot','minecraft:stick','minecraft:clay_ball'])
    shaped('small_ceramic_kiln',[' B ','BFB',' B '],dict(B='minecraft:bricks',F='minecraft:furnace'))
    shaped('small_crucible_furnace',['B B','BFB'],dict(B='minecraft:bricks',F='minecraft:furnace'))
    shaped('unfired_ceramic_lining',['CCC',' C '],dict(C='minecraft:clay_ball'))
    shaped('unfired_ceramic_crucible',['C C','C C','CCC'],dict(C='minecraft:clay_ball'))
    craft('unfired_insulator',['minecraft:clay_ball'],count=2)
    craft('insulated_copper_cable',['jugcraft:copper_wire']*6+['jugcraft:tin_ingot']+['jugcraft:ceramic_insulator']*2,result='copper_cable',count=8)
    for name, recipe in processes(): write(data/'recipe'/f'starter_workshop/{name}.json',recipe)
    for name, label in NAMES.items():
        lang[('block.' if name in BLOCKS else 'item.')+'jugcraft.'+name]=label
        if name == 'crucible_bellows':
            for stage in range(4):
                elements=bellows_model(stage)
                part=model_writer.split_model(name,elements,[(0,0,0)])[0]
                model_writer.finish_closed(part)
                write(assets/'models/block'/f'{name}_{stage}.json',dict(parent='minecraft:block/block',textures=textures(elements),elements=part))
            elements=bellows_model(0)
            write(assets/'models/item'/f'{name}.json',dict(parent='minecraft:block/block',textures=textures(elements),elements=model_writer.scaled_elements(elements)))
            write(assets/'blockstates'/f'{name}.json',dict(variants={f'facing={face},pump={stage}':dict(model=f'jugcraft:block/{name}_{stage}',y=rotation)
                for face,rotation in model_writer.FACING_Y.items() for stage in range(4)}))
            write(data/'loot_table/blocks'/f'{name}.json',dict(type='minecraft:block',pools=[dict(rolls=1,
                entries=[dict(type='minecraft:item',name='jugcraft:'+name)],condition=dict(type='minecraft:survives_explosion'))]))
        elif name not in STATIONS:
            elements=item_model(name)
            write(assets/'models/item'/f'{name}.json',dict(parent='minecraft:block/block',textures=textures(elements),elements=model_writer.scaled_elements(elements)))
        else:
            kind=STATIONS.index(name); footprint=[(0,0,0),(-1,0,0)] if kind==0 else [(0,0,0),(0,1,0)] if kind==2 else [(0,0,0)]
            variants={}
            for upgraded in (False,True):
                suffix='_upgraded' if upgraded and kind==2 else ''
                for lit in (False,True):
                    elements=station_model(kind,lit,upgraded); parts=model_writer.split_model(name,elements,footprint)
                    for index,part in enumerate(parts):
                        model_writer.finish_closed(part)
                        write(assets/'models/block'/f'{name}{suffix}_part{index}{"_on" if lit else ""}.json',
                            dict(parent='minecraft:block/block',textures=textures(elements),elements=part))
                    for facing,rotation in model_writer.FACING_Y.items():
                        for part in (0,1):
                            variants[f'facing={facing},lit={str(lit).lower()},part={part},upgraded={str(upgraded).lower()}']=dict(
                                model=f'jugcraft:block/{name}{suffix}_part{min(part,len(footprint)-1)}'+('_on' if lit else ''),y=rotation)
            elements=station_model(kind,False)
            write(assets/'models/item'/f'{name}.json',dict(parent='minecraft:block/block',textures=textures(elements),elements=model_writer.scaled_elements(elements)))
            write(assets/'blockstates'/f'{name}.json',dict(variants=variants))
            write(data/'loot_table/blocks'/f'{name}.json',dict(type='minecraft:block',pools=[dict(rolls=1,
                entries=[dict(type='minecraft:item',name='jugcraft:'+name)],condition=dict(type='minecraft:survives_explosion'))]))
        write(assets/'items'/f'{name}.json',dict(model=dict(type='minecraft:model',model='jugcraft:item/'+name)))
    labels = {'work':'Start Work','working':'Stop Work','cancel':'Cancel batch','preview':'Recipe preview →',
              'heat':'Heat: %s s','lining':'Lining','vessel':'Pot',
              'status.0':'Working','status.1':'Insert recipe ingredients','status.2':'Fit the matching tool',
              'status.3':'Install lining and crucible','status.4':'Add heating fuel','status.5':'Output is full',
              'status.6':'Click Start Work','status.7':'Feature disabled','status.8':'Station is incomplete', 'status.9':'Install heat insert', 'status.10':'Attach bellows', 'status.11':'Supply airflow',
              'pump':'Start Pumping','stop_pumping':'Stop Pumping','insert':'Insert','air.0':'Airflow: stopped','air.1':'Airflow: manual','air.2':'Airflow: shaft'}
    lang.update({'screen.jugcraft.workshop.'+k:v for k,v in labels.items()})
    for tool, names in [('axe',[STATIONS[0],'crucible_bellows']),('pickaxe',STATIONS[1:])]:
        path = data.parent/'minecraft/tags/block/mineable'/f'{tool}.json'
        tag = json.loads(path.read_text()) if path.exists() else dict(values=[])
        tag['values'] += ['jugcraft:'+n for n in names if 'jugcraft:'+n not in tag['values']]
        write(path,tag)

def chapter():
    return dict(title='Starter workshop',icon='jugcraft:workshop_bench',pages=[
        dict(title='From clay to alloys',icon='jugcraft:small_ceramic_kiln',text=[
            'Fire clay balls in an ordinary furnace for bricks. Four brick blocks and a furnace make the Small Ceramic Kiln. Shape four clay balls into a lining and seven into a crucible. Fire them in the kiln for 20 and 30 seconds. Shape one clay ball into two insulator blanks; fire the pair for 10 seconds.',
            'Build the Small Crucible Furnace from four brick blocks and a furnace. Leave two blocks of height clear. Install the fired lining and crucible in its screen; both last indefinitely. Select bronze, brass or solder, insert the metals and ordinary fuel. Bronze/brass take 10 seconds; solder takes 6. One coal supplies 80 seconds of active heat.',
            'Hoppers can insert ingredients and fuel and remove finished products and empty fuel containers. A blocked output pauses without spending fuel. Remaining heat and partial batches survive saving. Cancel returns the reserved ingredients if their slots have room; breaking either part returns the station, parts and ingredients once.']),
        dict(title='A bench and three tools',icon='jugcraft:workshop_hammer',text=[
            'Make the two-block-wide bench with a crafting table, four planks and two cobblestone. A hammer needs two iron and a stick; cutters need two iron and two sticks; the soldering tool needs copper and a stick. Tools are reusable and never wear out.',
            'Choose a recipe on the left, follow the preview, insert ingredients and the matching tool, then click Start Work. Click Stop Work or leave the bench menu to pause; hoppers cannot work for you. One ingot makes one iron, copper, tin, zinc, lead, bronze or brass plate in 5 seconds. One copper ingot makes three wires in 5 seconds.',
            'For a basic circuit, use existing silicon, three wires and one solder ingot with the soldering tool and fuel. A circuit takes 10 seconds. Work remains enabled until you click Stop Work or leave the menu. Powered machines keep the same yields and can run without a player at the bench. Mine Overworld quartz and use the upgraded crucible described on the next page for silicon.']),
        dict(title='Quartz and starter silicon',icon='jugcraft:quartz_ore',text=[
            'Look beneath Highland, Shield, Basin, windswept hills, Stony Peaks and Stony Shore. Smaller veins occur beneath ordinary forests, birch forests, taigas, Woodland, Coniferous Forest, Maple Woods and Aspen Glade. Mine with a stone pickaxe or better; drops are ordinary quartz. Centers lie between Y -16 and 96, most often near 40. Rocky outcrops may mark a nearby vein.',
            'Install a heat insert (two brick blocks and a fired lining) in the existing crucible. Make bellows from four planks, two leather and an iron shaft; place them on solid ground against the furnace lower block, with the nozzle touching it. Select silicon: one quartz and one charcoal make one silicon in ten seconds, plus separate furnace fuel.',
            'Click Start Pumping once; click again or close the menu to stop. A rear shaft supplies 8 KE/t for unattended airflow, with the same yield and speed. Missing fuel or airflow and full output pause without wasting heating fuel. Parts never wear out. Alloys still work without airflow. The Blast Furnace fallback remains during preview review; the Arc Furnace gives two silicon per quartz.']),
        dict(title='Connecting the first machine' ,icon='jugcraft:copper_cable',text=[
            'Six wires, a tin ingot and two fired insulators make eight copper cables. Two copper ingots and one tin make four directly; adding molded cable insulation makes sixteen. The bronze-blend recipe remains available. Starter ceramics need the machines feature, not drones.',
            'Use these same alloys and components for existing casings, generators, shafts and powered machines. Nothing here requires a circuit just to make the first furnace, kiln or bench. Advanced ceramics and mechanical expansion remain separate.'])])
