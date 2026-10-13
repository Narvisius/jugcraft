"""Author the block-native woven tiki torch and its optional light definition."""
from pathlib import Path
import json
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
A=ROOT/'assets/peepo_companion'
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')
def box(lo,hi,texture):
    # Full item geometry extends above y=16; implicit UVs would leave the sprite and smear its edges.
    # Explicit UVs also keep the clipped placed halves and held basket visually consistent.
    return {'from':lo,'to':hi,'faces':{side:{'texture':'#'+texture,'uv':[0,0,16,16]} for side in ['up','down','north','south','east','west']}}
# Original pixel-art basket weave: alternating over/under reed strips and fine dark seams.
im=Image.new('RGB',(16,16))
for y in range(16):
    for x in range(16):
        band=(y//3)%2
        seam=(x+y+band*3)%8
        cross=(x-y)%8
        if seam==0:c=(126,103,55)
        elif seam==1:c=(226,207,145)
        elif cross<2:c=(164,140,82)
        else:
            grain=((x*7+y*3)%5)-2
            c=(194+grain,170+grain,108+grain)
        im.putpixel((x,y),c)
tex=A/'textures/block/tiki_weave.png';tex.parent.mkdir(parents=True,exist_ok=True);im.save(tex)
textures={'particle':'peepo_companion:block/tiki_weave','shaft':'minecraft:block/stripped_oak_log','weave':'peepo_companion:block/tiki_weave','binding':'minecraft:block/oak_planks','cap':'minecraft:block/polished_blackstone','wick':'minecraft:block/black_wool','flame':'minecraft:block/orange_wool','flame_tip':'minecraft:block/yellow_wool'}
full=[box([7.2,0,7.2],[8.8,28.9,8.8],'shaft'),box([6.95,11.45,6.95],[9.05,12.15,9.05],'binding')]
# A continuous straight core supports the burner. The open flare occupies only the last six pixels.
for y in range(12,18):
    spread=.65+(y-12)*.23
    for sx in [-1,1]:
        for sz in [-1,1]:
            x,z=8+sx*spread,8+sz*spread
            full.append(box([x-.34,y,z-.34],[x+.34,y+1,z+.34],'shaft'))
full += [box([5.79,17.7,5.79],[10.21,18.25,10.21],'binding'),
    box([5.875,18,5.45],[10.125,27.5,10.55],'weave'),box([5.45,18,5.875],[10.55,27.5,10.125],'weave'),
    box([5.195,27.5,5.195],[10.805,28.05,10.805],'cap'),box([5.875,28.05,5.875],[10.125,28.55,10.125],'cap'),
    box([6.725,28.55,6.725],[9.275,29,9.275],'cap'),box([7.55,29,7.55],[8.45,29.5,8.45],'wick'),
    # Orange outer tongues and a taller yellow core, all inside the original two-block height.
    box([7.35,29.45,7.35],[8.65,30.1,8.65],'flame'),
    box([7.6,30.1,7.6],[8.4,30.9,8.4],'flame_tip'),
    box([7.25,30.1,7.45],[7.8,30.8,8.05],'flame'),
    box([7.35,30.8,7.55],[7.7,31.15,7.95],'flame_tip'),
    box([8.4,30.1,7.85],[8.75,31.2,8.4],'flame'),
    box([8.45,31.2,7.95],[8.65,31.65,8.25],'flame_tip'),
    box([7.7,30.9,7.7],[8.3,31.5,8.3],'flame_tip'),
    box([7.8,31.5,7.8],[8.12,31.95,8.12],'flame_tip')]
# Clip at the block boundary so each half's geometry stays within its own cell.
for name,base in [('lower',0),('upper',16)]:
    elements=[]
    for e in full:
        lo,hi=e['from'].copy(),e['to'].copy()
        lo[1]=max(lo[1],base);hi[1]=min(hi[1],base+16)
        if lo[1]>=hi[1]:continue
        lo[1]-=base;hi[1]-=base
        elements.append({**e,'from':lo,'to':hi})
    write(A/f'models/block/tiki_torch_{name}.json',{'parent':'minecraft:block/block','textures':textures,'elements':elements})
p=A/'models/item/tiki_torch.json';item=json.loads(p.read_text());item['textures']=textures;item['elements']=full;write(p,item)
write(A/'dynamiclights/item/tiki_torch.json',{'match':{'items':'peepo_companion:tiki_torch'},'luminance':14,'water_sensitive':True})
