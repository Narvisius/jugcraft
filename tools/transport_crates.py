"""Item-only open wooden transport crates. Original geometry; owner wood imported by owner_art.py."""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/peepo_companion'

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf8')

def build(iron):
    elements=[]
    def box(a,b,texture='wood'):
        if iron:
            a=[8+(a[0]-8)*1.06,a[1]*1.06,8+(a[2]-8)*1.1]
            b=[8+(b[0]-8)*1.06,b[1]*1.06,8+(b[2]-8)*1.1]
        uv=[0,0,16,4] if texture=='wood' else [0,0,3,3] if texture=='rivet' else [0,0,16,16]
        elements.append({'from':a,'to':b,'faces':{face:{'texture':'#'+texture,'uv':uv} for face in ('up','down','north','south','east','west')}})
    for x in (1,4.5,8,11.5): box([x,1,3],[x+3.5,2,13])
    for x in (1,13.8):
        for z in (3,11.8): box([x,2,z],[x+1.2,10.95,z+1.2])
    for y in (2,5.2,8.4):
        for z in (2.5,12.5): box([.5,y,z],[15.5,y+2.4,z+1])
        for x in (.5,14.5): box([x,y,3.5],[x+1,y+2.4,12.5])
    # Outside handholds meet the fingers while the solid wall stays clear of the hand.
    for right in (False,True):
        for a,b in [([-.65,8.8,6.2],[.49,9.5,6.9]),([-.65,8.8,9.1],[.49,9.5,9.8]),([-.65,8.8,6.9],[-.2,9.5,9.1])]:
            if right:a,b=[16-b[0],a[1],a[2]],[16-a[0],b[1],b[2]]
            box(a,b,'iron' if iron else 'wood')
    # Slightly raised nail heads, deliberately separated from the wood faces.
    for x in (1.6,14.4):
        for y in (3,6.2,9.4):
            for z in (2.37,13.51): box([x-.17,y-.17,z],[x+.17,y+.17,z+.12],'iron')
    if iron:
        # A folded angle wraps each actual corner, with separate flanges sharing only an edge.
        # Mirroring one assembly keeps all four corners identical and avoids coplanar overlays.
        for flip_x in (False,True):
            for flip_z in (False,True):
                def corner(a,b,material='iron'):
                    if flip_x: a,b=[16-b[0],a[1],a[2]],[16-a[0],b[1],b[2]]
                    if flip_z: a,b=[a[0],a[1],16-b[2]],[b[0],b[1],16-a[2]]
                    box(a,b,material)
                corner([.18,1.65,2.17],[1.55,11.08,2.48])
                corner([.18,1.65,2.48],[.48,11.08,3.85])
                # Top corner shoes wrap over the rim, just above the wooden posts.
                corner([.18,10.98,2.17],[2.5,11.25,3.6])
                corner([.18,10.98,3.6],[1.5,11.25,4.6])
                for y in (1.7,5.85,10.2):
                    corner([1.55,y,2.18],[3.2,y+.78,2.47])
                    corner([.19,y,3.85],[.47,y+.78,5.15])
                    # Small proud bolt heads on both faces of the corner assembly.
                    for x in (.85,2.6):corner([x-.16,y+.22,2.02],[x+.16,y+.54,2.16],'rivet')
                    for z in (3.1,4.6):corner([.03,y+.22,z-.16],[.17,y+.54,z+.16],'rivet')
        for z in (2.18,13.53):box([3.2,1.7,z],[12.8,2.48,z+.29],'iron')
        for x in (.19,15.52):box([x,1.7,5.15],[x+.29,2.48,10.85],'iron')
    name='iron_mob_transport_crate' if iron else 'mob_transport_crate'
    transforms={
        'gui':{'rotation':[25,225,0],'translation':[0,1,0],'scale':[.85,.85,.85]},
        'ground':{'translation':[0,3,0],'scale':[.55,.55,.55]},
        'fixed':{'rotation':[0,180,0],'translation':[0,1,0],'scale':[.8,.8,.8]},
        'thirdperson_righthand':{'rotation':[90,0,0],'translation':[6,0,-2.5],'scale':[.6,.6,.6]},
        'thirdperson_lefthand':{'rotation':[90,0,0],'translation':[6,0,-2.5],'scale':[.6,.6,.6]},
        'firstperson_righthand':{'rotation':[10,25,0],'translation':[0,1,-2],'scale':[.5,.5,.5]},
        'firstperson_lefthand':{'rotation':[10,25,0],'translation':[0,1,-2],'scale':[.5,.5,.5]}}
    write(ASSETS/'models/item'/f'{name}.json',{'parent':'minecraft:block/block','textures':{'particle':'jugcraft:block/transport_crate_wood','wood':'jugcraft:block/transport_crate_wood','iron':'peepo_companion:block/transport_crate_iron','rivet':'peepo_companion:block/transport_crate_iron'},'elements':elements,'display':transforms})
    write(ASSETS/'items'/f'{name}.json',{'model':{'type':'minecraft:composite','models':[{'type':'minecraft:model','model':f'peepo_companion:item/{name}'},{'type':'minecraft:special','base':f'peepo_companion:item/{name}','model':{'type':'peepo_companion:crate_occupants'}}]}})

def main():
    image=Image.new('RGBA',(16,16),(139,150,154,255))
    for y in range(16):
        for x in range(16):
            value=196 if x==1 else 96 if x in (0,15) else 165 if x in (2,3) else 139
            image.putpixel((x,y),(value,min(255,value+7),min(255,value+10),255))
    path=ASSETS/'textures/block/transport_crate_iron.png';path.parent.mkdir(parents=True,exist_ok=True);image.save(path)
    build(False);build(True)

if __name__=='__main__':main()
