"""Original 16x16 companion clipboard icon; owner-style flat tones and material outlines."""
from pathlib import Path
from PIL import Image, ImageDraw
im=Image.new("RGBA",(16,16));d=ImageDraw.Draw(im)
d.rectangle((3,2,12,14),fill="#4a3523");d.rectangle((4,3,11,13),fill="#bf9256")
d.rectangle((5,4,10,12),fill="#ece1b7");d.rectangle((5,1,10,3),fill="#394d3a");d.rectangle((6,1,9,2),fill="#78b96a")
for y in [6,8,10]:d.line((6,y,9,y),fill="#698257")
p=Path(__file__).resolve().parents[1]/"src/main/resources/assets/peepo_companion/textures/item/companion_planner.png"
p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
