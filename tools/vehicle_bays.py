"""Owner's vehicle-bay brief: original supported gantries, existing Jugcraft metal textures.
Run independently; no owner-library original is edited. Native cuboids preserve pixel density.
"""
from pathlib import Path
import json
import model_writer as mw

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
ASSETS = ROOT / 'assets/jugcraft'
BLOCKS = ('mecha_workbench', 'tank_workbench')
SILVER, DARK, AMBER, BLACK, SCREEN = 'dw_steel_plate', 'dp_gunmetal', 'dr_amber', 'dp_rubber', 'el_screen_cyan_on'

def footprint(height):
    return [(-x,0,z) for z in range(6) for x in range(6)] + [(-x,y,z) for y in range(1,height) for z in (0,5) for x in (0,5)] + [(-x,height-1,z) for z in range(1,5) for x in (0,5)]

def model(height):
    out=[]
    def b(x,y,z,X,Y,Z,t): out.append(((x,y,z),(X,Y,Z),t))
    # Deck: bevelled perimeter, flush inset working plates, bolted rail channels.
    for z in range(6):
        for x in range(6):
            a=-x*16;c=z*16
            b(a,0,c,a+16,2,c+16,DARK)
            b(a+.4,2,c+.4,a+15.6,3,c+15.6,SILVER if 0<x<5 and 0<z<5 else DARK)
    for x in (-79,13):
        b(x,3,16,x+2,4,80,AMBER)
    for z in (1,92):
        for x in range(-64,0,8): b(x,3,z,x+4,3.7,z+2,AMBER)
    top=height*16
    # Four rooted pillars, collars, vertical cable conduits and diagonal foot bracing.
    for x in (0,-80):
        for z in (0,80):
            b(x+1,3,z+1,x+15,6,z+15,DARK)
            b(x+4,6,z+4,x+12,top-3,z+12,DARK)
            b(x+3,7,z+3,x+5,top-8,z+5,SILVER)
            b(x+3,10,z+2.5,x+13,13,z+3.5,AMBER)
            b(x+6,14,z+3,x+10,top-10,z+4,BLACK)
            b(x+2,top-10,z+2,x+14,top-6,z+14,SILVER)
            b(x+1,top-6,z+1,x+15,top-2,z+15,DARK)
            b(x+5,top-5,z,x+11,top-3,z+2,AMBER)
    # Rails run into the post heads. Carriage rollers actually sit on these rails.
    for x in (0,-80):
        b(x+2,top-4,4,x+14,top,92,DARK)
        b(x+6,top-.8,4,x+10,top,92,SILVER)
    # Front control cabinet, electrical socket flush to east face, keyboard and display.
    b(1,3,1,15,13,15,DARK)
    b(2,5,.5,14,12,2,SILVER)
    b(3,7,0,13,12,.7,SCREEN+'!')
    b(2,13,3,14,14,12,SILVER)
    b(3,14,4,13,14.5,9,SCREEN+'!')
    b(14,5,4,16,11,12,BLACK)
    b(15.8,6,5,16,10,11,SCREEN+'!')
    # Rear tool lockers tie into the deck, between the rear supports.
    for x in (-62,-40,-18):
        b(x,3,83,x+18,15,94,DARK)
        b(x+1,5,82.5,x+17,13,83.5,SILVER)
        b(x+7,8,82,x+11,10,82.5,BLACK)
    return out

def carriage(height):
    top=height*16
    # Crossbeam is below the fixed side rails, with roller shoes at both ends.
    out=[((-76,top-5,40),(12,top-1.5,48),DARK),((-68,top-4.5,39.4),(4,top-2,40.2),SILVER),
         ((-43,top-8,37),(-21,top-1,51),AMBER),((-39,top-9,38),(-25,top-7,50),DARK),
         ((-34,top-17,42),(-30,top-8,46),SILVER),((-38,top-19,40),(-26,top-16,48),DARK),
         ((-39,top-24,40),(-36,top-18,48),SILVER),((-28,top-24,40),(-25,top-18,48),SILVER)]
    for x in (-80,0):
        out += [((x+1,top-8,38),(x+15,top-4.2,50),DARK),((x+2,top-3,39),(x+14,top-.9,49),AMBER)]
    return out

def arm(height):
    # Mounted on the rear post at (-72, *, 88). The entire articulated assembly pivots at its shoulder.
    y=height*16-16
    return [((-76,y-3,84),(-68,y+3,92),DARK),((-74,y-2,67),(-70,y+2,87),AMBER),
            ((-76,y-4,63),(-68,y+4,69),SILVER),((-73,y-15,63),(-69,y-1,67),AMBER),
            ((-76,y-17,60),(-66,y-13,70),DARK),((-75,y-23,61),(-72,y-16,64),SILVER),
            ((-70,y-23,67),(-67,y-16,70),SILVER)]

def write():
    from kinetic_rotors import quads
    writer=mw.StyleWriter(ASSETS)
    animations={}
    names=[]
    for name,h in [('mecha_workbench',6),('tank_workbench',3)]:
        names.append('jugcraft:'+name)
        static=model(h); moving=carriage(h)+arm(h)
        textures={n:mw.rid('block/'+n) for n in mw.texture_names(static+moving)}
        textures['particle']=mw.rid('block/'+DARK)
        for i,part in enumerate(mw.split_model(name,static,footprint(h))):
            writer.model(f'{name}_part{i}',{'ambientocclusion':False,'textures':textures,'elements':part})
        writer.large_states(name,len(footprint(h)),False)
        writer.item_model(name,{'parent':'minecraft:block/block','textures':textures,'elements':mw.scaled_elements(static+moving)})
        writer.item(name,mw.rid('item/'+name))
        animations[name+'_carriage']=quads(carriage(h))
        animations[name+'_arm']=quads(arm(h))
        mw.write(ROOT/'data/jugcraft/loot_table/blocks'/f'{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+name}],'condition':{'type':'minecraft:survives_explosion'}}]})
        keys={'P':'#c:plates/steel','E':'jugcraft:diesel_engine','C':'jugcraft:basic_circuit','R':'minecraft:piston','B':'jugcraft:steel_block'}
        mw.write(ROOT/'data/jugcraft/recipe'/f'{name}.json',{'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'machines'}],
            'type':'minecraft:crafting_shaped','category':'misc','pattern':['PRP','ECE','BBB' if h==6 else 'BPB'],'key':keys,'result':{'id':'jugcraft:'+name,'count':1}})
        mw.write(ROOT/'data/jugcraft/advancement/recipes/machines'/f'{name}.json',{'parent':'minecraft:recipes/root','criteria':{'has_steel':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'jugcraft:steel_block'}]}}},'requirements':[['has_steel']],'rewards':{'recipes':['jugcraft:'+name]}})
    mw.write(ASSETS/'vehicle_bay_quads.json',animations)
    for tag in ['mineable/pickaxe','needs_iron_tool']:
        path=ROOT/'data/minecraft/tags/block'/f'{tag}.json'; data=json.loads(path.read_text(encoding='utf-8'))
        data['values']=list(dict.fromkeys(data['values']+names));mw.write(path,data)
    path=ASSETS/'lang/en_us.json';lang=json.loads(path.read_text(encoding='utf-8'))
    lang.update({'block.jugcraft.mecha_workbench':'Mecha Assembly Workbench','block.jugcraft.tank_workbench':'Tank Assembly Workbench',
        'message.jugcraft.bay.blocked':'Assembly bay needs clear, permitted space at %s, %s, %s.',
        'tooltip.jugcraft.bay.fitout':'%s / %s',
        'screen.jugcraft.bay.plan_help':'Build a vehicle from its existing components. An inserted refit kit selects its own chassis.',
        'screen.jugcraft.bay.armor_help':'Reinforced: +50% damage threshold, -10% speed. Costs 8 steel plates. Replacing armor does not refund materials.',
        'screen.jugcraft.bay.drive_help.0':'Standard: normal speed and fuel use.',
        'screen.jugcraft.bay.drive_help.1':'Overdrive: +20% speed, double driving fuel. Costs 2 steel gears and 4 redstone.',
        'screen.jugcraft.bay.drive_help.2':'Economy: -15% speed, half driving fuel. Costs 2 steel gears and 4 redstone.',
        'screen.jugcraft.bay.start':'Build / refit','screen.jugcraft.bay.cancel':'Cancel',
        'screen.jugcraft.bay.start_help':'Assembly: 10 seconds, 12,800 JE. Refit: 5 seconds, 6,400 JE. Inputs lock while working. Retrieve the finished kit and place it on clear ground.',
        'screen.jugcraft.bay.cancel_help':'Unlocks the actual inputs. Energy already spent is not refunded.',
        'screen.jugcraft.bay.materials':'COMPONENTS','screen.jugcraft.bay.refit':'REFIT','screen.jugcraft.bay.output':'KIT'})
    for i,v in enumerate(['Diesel','Armoured','Howitzer','Landship']):lang[f'screen.jugcraft.bay.plan.{i}']=v
    for i,v in enumerate(['Standard armor','Reinforced armor']):lang[f'screen.jugcraft.bay.armor.{i}']=v
    for i,v in enumerate(['Standard','Overdrive','Economy']):lang[f'screen.jugcraft.bay.drive.{i}']=v
    for i,v in enumerate(['Std','Fast','Eco']):lang[f'screen.jugcraft.bay.drive_short.{i}']=v
    for i,v in enumerate(['Ready to assemble','Add previewed components','Waiting for power','Take finished kit','Assembly in progress','Machines disabled','Wrong vehicle kit','Clear or repair the bay']):lang[f'screen.jugcraft.bay.status.{i}']=v
    mw.write(path,lang)
    print('Vehicle bays: 6x6x6 mecha, 6x6x3 tank; supported gantries, recipes, animation geometry and translations.')

if __name__=='__main__':write()
