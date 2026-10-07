"""Draw an original menacing skull into Kook's box-UV atlas; no sampled vanilla artwork.
Requires Pillow. Logical UVs stay 512x512; four physical pixels per model texel.
"""
from pathlib import Path
import base64,io,json
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
TEX=ROOT/'src/main/resources/assets/jugcraft/textures/entity/scary'
u,v=256,80
bone=(133,213,240,255);shadow=(65,135,172,255);dark=(9,25,43,255);light=(196,243,253,255)
face=Image.new('RGBA',(32,32),shadow);d=ImageDraw.Draw(face)
d.polygon([(6,1),(25,1),(30,7),(29,19),(25,23),(25,29),(21,31),(10,31),(6,28),(6,23),(2,19),(1,8)],fill=bone)
d.line([(6,2),(24,2),(28,6)],fill=light,width=2)
# Deep, asymmetrical angular sockets; diagonal upper edges form a threatening brow.
d.polygon([(3,9),(8,8),(14,12),(13,19),(7,21),(3,17)],fill=dark)
d.polygon([(18,12),(24,8),(29,9),(28,18),(23,21),(18,18)],fill=dark)
d.rectangle((9,14,10,15),fill=(39,101,143,255));d.rectangle((22,14,23,15),fill=(39,101,143,255))
d.polygon([(15,18),(18,23),(13,23)],fill=dark)
d.polygon([(6,23),(11,25),(21,25),(26,22),(25,28),(21,30),(10,30),(7,28)],fill=dark)
for x in [9,13,17,21]:
 d.rectangle((x,25,x+1,28),fill=light)
d.line([(8,21),(11,22)],fill=light,width=1);d.line([(23,22),(26,20)],fill=light,width=1)
for name in ['space_kook','space_kook_red','glow','glow_red']:
 p=TEX/f'{name}.png';im=Image.open(p).convert('RGBA')
 if im.size==(512,512):im=im.resize((2048,2048),Image.Resampling.NEAREST)
 ImageDraw.Draw(im).rectangle((u*4,v*4,(u+32)*4-1,(v+16)*4-1),fill=bone)
 im.paste(face,((u+8)*4,(v+8)*4));im.save(p)
model=ROOT/'art/scary-creatures/Space Kook Edit 1.bbmodel';doc=json.loads(model.read_text())
for tex in doc['textures']:
 if tex.get('name')=='space_kook.png':
  tex['source']='data:image/png;base64,'+base64.b64encode((TEX/'space_kook.png').read_bytes()).decode()
  tex['width']=2048;tex['height']=2048
model.write_text(json.dumps(doc,indent=2)+'\n',encoding='utf-8')
print('Updated skull atlases and editable model.')
