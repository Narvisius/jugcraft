"""Original beige switchgear/transformer art for the existing 64-part energy exchange.

Native JSON geometry and opaque instrument textures, OpenAI Codex (GPT-6), MIT.
No gameplay, cable, cargo, registry, language or world changes. Static instruments
have real percentage scales and blank readouts; the client adds actual JE telemetry.
"""
from pathlib import Path
import argparse
import hashlib
import json
import math

from PIL import Image, ImageDraw, ImageFont
import model_writer as mw

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "tower_power_"
PAINT = (191, 185, 164, 255)
INK = (27, 29, 28, 255)
WHITE = (235, 235, 220, 255)
LAYOUT = {
    "version": 1, "units": "anchor-relative model pixels", "front": "north/-z",
    "static_face_z": .15, "overlay_z": .05,
    "socket": {"part": 26, "center": [40, 24, 0], "contact": [36.8, 20.8, 43.2, 27.2]},
    "dials": [
        {"channel": "storage_percent", "pivot": [21, 35, .05], "radius": 2.35, "start": 210, "sweep": 240, "min": 0, "max": 100},
        {"channel": "load_percent", "pivot": [27, 35, .05], "radius": 2.35, "start": 210, "sweep": 240, "min": 0, "max": 100},
        {"channel": "storage_percent", "pivot": [54.35, 26.6, .05], "radius": 2.75, "start": 90, "sweep": 90, "min": 0, "max": 100},
        {"channel": "load_percent", "pivot": [61.35, 26.6, .05], "radius": 2.75, "start": 90, "sweep": 90, "min": 0, "max": 100}],
    "needle_axes": {"x": "-cos(angle)", "y": "sin(angle)"},
    "displays": [
        {"channel": "stored", "rect": [18.2, 24.6, 29.8, 29.4], "center_x": 24, "lines_y": [28, 26.1], "font_height": .62},
        {"channel": "demand_load", "rect": [50.2, 35.6, 61.8, 40.4], "center_x": 56, "lines_y": [39, 37.1], "font_height": .62}],
    "lamps": [
        {"channel": "linked", "center": [35.6, 40, .05], "radius": .48, "color": "green"},
        {"channel": "charged", "center": [35.6, 36.8, .05], "radius": .48, "color": "amber"},
        {"channel": "low_power", "center": [35.6, 33.6, .05], "radius": .48, "color": "red"}]
}


def font(size, bold=False):
    for name in (("arialbd.ttf", "DejaVuSans-Bold.ttf") if bold else ("arial.ttf", "DejaVuSans.ttf")):
        for base in (Path("C:/Windows/Fonts"), Path("/usr/share/fonts/truetype/dejavu")):
            path = base / name
            if path.is_file(): return ImageFont.truetype(str(path), size)
    raise FileNotFoundError("Arial or DejaVu Sans is required for original readable instrument labels")


def text_fit(draw, point, text, size, width, fill=WHITE, bold=True, anchor="mm"):
    while draw.textlength(text, font=font(size, bold)) > width and size > 8: size -= 1
    draw.text(point, text, font=font(size, bold), fill=fill, anchor=anchor)


def plate(lines, pale=False):
    img = Image.new("RGBA", (512, 128), WHITE if pale else INK)
    d = ImageDraw.Draw(img)
    d.rectangle((3, 3, 508, 124), outline=(119, 122, 116, 255), width=3)
    d.rectangle((8, 8, 503, 119), outline=(215, 211, 193, 255) if not pale else (90, 93, 87, 255), width=1)
    for y in (13, 114):
        for x in (14, 497):
            d.ellipse((x-3, y-3, x+3, y+3), fill=(144, 148, 142, 255))
            d.line((x-2, y, x+2, y), fill=(40, 43, 42, 255), width=1)
    for i, line in enumerate(lines):
        text_fit(d, (256, 64+(i-(len(lines)-1)/2)*44), line, 38 if len(lines)==1 else 32, 456, INK if pale else WHITE)
    return img


def dial():
    img = Image.new("RGBA", (512, 512), INK); d = ImageDraw.Draw(img)
    d.ellipse((24, 24, 488, 488), fill=(105, 111, 107, 255))
    d.ellipse((35, 35, 477, 477), fill=(237, 237, 222, 255))
    for i in range(41):
        a = math.radians(210-240*i/40); r0=184 if i%10==0 else 198
        d.line((256+math.cos(a)*r0,256-math.sin(a)*r0,256+math.cos(a)*216,256-math.sin(a)*216),fill=INK,width=5 if i%10==0 else 3)
        if i%10==0:
            text_fit(d,(256+math.cos(a)*151,256-math.sin(a)*151),str(int(i/40*100)),25,75,INK)
    text_fit(d,(256,345),"%",35,100,INK)
    text_fit(d,(256,387),"PERCENT",17,170,INK)
    # Deliberately no needle, hub, current reading, fictitious voltage or frequency.
    return img


def relay():
    img = Image.new("RGBA", (512,512), (237,237,222,255));d=ImageDraw.Draw(img)
    cx,cy=74,436;radius_x=2.75/4.5*512;radius_y=2.75/4*512
    for i in range(21):
        a=math.radians(90-90*i/20);fraction=.84 if i%5==0 else .91
        d.line((cx+math.cos(a)*radius_x*fraction,cy-math.sin(a)*radius_y*fraction,cx+math.cos(a)*radius_x,cy-math.sin(a)*radius_y),fill=INK,width=5 if i%5==0 else 3)
        if i%10==0:text_fit(d,(cx+math.cos(a)*radius_x*.69+18,cy-math.sin(a)*radius_y*.69-12),str(i*5),29,90,INK)
    text_fit(d,(405,88),"%",42,80,INK)
    return img


def textures():
    result={"paint":Image.new("RGBA",(256,256),PAINT),"dark_paint":Image.new("RGBA",(256,256),(78,82,77,255)),
            "black":Image.new("RGBA",(256,256),INK),"display":Image.new("RGBA",(512,256),(12,20,19,255)),
            "dial":dial(),"relay":relay()}
    # Quiet rolled-paint sheen; no random flecks, rust or artificial cabinet grid.
    for key in ("paint","dark_paint"):
        d=ImageDraw.Draw(result[key]);c=PAINT if key=="paint" else (78,82,77,255)
        for x in range(256):
            delta=round(2*math.sin(x/256*math.pi))
            d.line((x,0,x,255),fill=tuple(min(255,v+delta) for v in c[:3])+(255,))
    labels={"incoming":["INCOMING","BUS METERING"],"protection":["PROTECTION","STATUS RELAYS"],
            "distribution":["TOWER","DISTRIBUTION"],"storage":["STORED ENERGY"],"demand":["DEMAND  /  JE PER TICK"],
            "storage_pct":["STORAGE %"],"load_pct":["LOAD %"],"linked":["LINK VERIFIED"],"charged":["RESERVE FULL"],
            "low_power":["LOW POWER"],"selector":["METER SELECTOR"],"service":["SERVICE  /  LOCKED"],
            "cable":["SEALED CABLE ENTRY"],"cooling":["SEALED TRANSFORMER","COOLING BANK"],
            "main":["DRONE TOWER","ENERGY EXCHANGE"],"access":["SERVICE ACCESS"],"relay_storage":["STORAGE"],"relay_load":["LOAD"]}
    for name,lines in labels.items():result[name]=plate(lines,pale=name in ("storage_pct","load_pct","selector","service","cable","relay_storage","relay_load"))
    for name,colour in {"lamp_green":(29,69,45,255),"lamp_amber":(89,62,22,255),"lamp_red":(79,25,23,255)}.items():
        img=Image.new("RGBA",(128,128),INK);d=ImageDraw.Draw(img)
        d.ellipse((5,5,123,123),fill=(101,106,101,255));d.ellipse((15,15,113,113),fill=colour)
        d.arc((23,21,105,105),185,255,fill=tuple(min(255,c+20) for c in colour[:3])+(255,),width=4)
        result[name]=img
    return {PREFIX+k:v for k,v in result.items()}


def geometry():
    out=[]
    def box(a,b,t="paint",rotation=None,front=None):
        assert all(a[i]<b[i] for i in range(3)),(a,b)
        tex=PREFIX+t if t in ("paint","dark_paint","black") else "tower_detail_"+t
        if front:tex={"*":tex,"north":PREFIX+front+"!"}
        out.append((a,b,tex,{"rotation":rotation}) if rotation else (a,b,tex))
    def tag(x0,y0,x1,y1,label,z=.45):box((x0,y0,z),(x1,y1,1.45),"black",front=label)
    def cylinder(axis,center,lo,hi,r,t="steel"):
        k="xyz".index(axis);axes=[v for v in range(3) if v!=k];origin=list(center);origin[k]=(lo+hi)/2
        for first,second,angle in ((r,r*math.tan(math.pi/8),0),(r*math.tan(math.pi/8),r,0),(r,r*math.tan(math.pi/8),45),(r*math.tan(math.pi/8),r,45)):
            a=origin.copy();b=origin.copy();a[k]=lo;b[k]=hi
            for j,w in zip(axes,(first,second)):a[j]-=w;b[j]+=w
            box(tuple(a),tuple(b),t,(axis,angle,tuple(origin)) if angle else None)
    def bolt(x,y,z=.3):
        box((x-.25,y-.25,z),(x+.25,y+.25,z+.45),"light_steel")
        box((x-.16,y-.035,z-.03),(x+.16,y+.035,z+.08),"dark")
    def window(rect,face="display"):
        x0,y0,x1,y1=rect
        box((x0-.35,y0-.35,.35),(x1+.35,y1+.35,1.45),"black")
        box((x0,y0,.15),(x1,y1,.45),"black",front=face)
        for x in (x0-.2,x1+.2):
            for y in (y0-.2,y1+.2):bolt(x,y,.12)
    def selector(x,y):
        cylinder("z",(x,y,0),.35,1.5,1.15,"dark")
        box((x-.42,y-.9,.02),(x+.42,y+.9,.6),"black")
        box((x-.12,y+.2,0),(x+.12,y+.7,.1),"light_steel")
    # Full existing 5x5 foundation; all upper solids remain inside its sparse footprint.
    box((.2,0,.2),(79.8,3.5,79.8),"concrete")
    for x in (16.5,60.5):box((x,3.3,3),(x+3,6.5,61.5),"dark")
    for z in (4,18,59):box((16.5,6,z),(63.5,7.8,z+2.2),"steel")
    box((16.4,6.5,2.4),(63.6,47,17.6),"dark_paint")
    box((16.5,47,.6),(63.5,47.8,18),"paint")
    # Three distinctly instrumented cubicles with physical folded leaves and service gear.
    for x,label in ((17,"incoming"),(33,"protection"),(49,"distribution")):
        box((x,8.2,1.25),(x+14,46.4,2.15),"paint")
        box((x+.3,8.6,2.15),(x+13.7,45.9,3),"paint")
        tag(x+.8,43,x+13.2,45.9,label,.9)
        for yy in (13,36.5):
            cylinder("y",(x+.4,0,1),yy,yy+2.2,.38,"light_steel")
        for yy in (19.2,22.5):box((x+12.1,yy,.45),(x+12.8,yy+.7,1.5),"dark")
        box((x+12.2,19.2,.12),(x+12.7,23.2,.8),"light_steel")
        for yy in (9.2,10,10.8):box((x+1.6,yy,.85),(x+10.9,yy+.27,1.35),"dark")
        for xx in (x+.9,x+13.1):
            for yy in (9,45.8):bolt(xx,yy,.88)
    for cx,label in ((21,"storage_pct"),(27,"load_pct")):
        window((cx-2.75,32.25,cx+2.75,37.75),"dial")
        tag(cx-2.65,30.7,cx+2.65,31.75,label,.5)
    window((18.2,24.6,29.8,29.4))
    tag(18.2,23,29.8,24.1,"storage",.5)
    for x in (21,27):selector(x,17.2)
    tag(18.4,14,29.4,15.3,"selector",.5)
    tag(18.4,11.7,29.4,13,"service",.5)
    # Protection windows are labeled relay cases, with actual-state pilot lenses only.
    for y,label,lens in ((40,"linked","lamp_green"),(36.8,"charged","lamp_amber"),(33.6,"low_power","lamp_red")):
        box((34.4,y-1.1,.55),(45.6,y+1.1,1.5),"black")
        box((34.95,y-.65,.15),(36.25,y+.65,.5),"black",front=lens)
        tag(36.8,y-.65,44.9,y+.65,label,.4)
    # Same neutral universal gland: its boot encloses the three unmodified cable arms.
    box((33.5,17.5,1.25),(46.5,30.5,2),"dark")
    box((34,18,.6),(46,30,1.25),"light_steel")
    box((36.8,20.8,0),(43.2,27.2,.65),"rubber")
    box((36.5,20.5,.65),(43.5,27.5,1.4),"dark")
    for x in (34.7,45.3):
        for y in (18.7,29.3):bolt(x,y,.05)
    tag(35,30.7,45,31.8,"cable",.3)
    selector(36.4,13.5);selector(43.6,13.5)
    tag(34.3,10.9,45.7,12.1,"service",.5)
    window((50.2,35.6,61.8,40.4));tag(50.2,41,61.8,42.3,"demand",.5)
    window((50.5,26,55,30),"relay");window((57.5,26,62,30),"relay")
    tag(50.4,24.4,55.1,25.5,"relay_storage",.5);tag(57.4,24.4,62.1,25.5,"relay_load",.5)
    for x in (52.2,58.8):selector(x,17)
    tag(50.2,13.5,61.8,14.8,"selector",.5);tag(50.2,11.5,61.8,12.8,"access",.5)
    # Compact sealed transformer behind the switchboard; no porcelain fantasy spires.
    box((18,8,20),(62,43.2,61.5),"dark_paint")
    box((17.4,43.3,19.5),(62.6,45.1,62),"paint")
    for x in (20,30,40,50,60):
        for z in (20.2,60.9):box((x-.35,45.2,z-.35),(x+.35,45.7,z+.35),"light_steel")
    for x in (20,58):
        for z in (24,55):
            box((x,7.5,z),(x+2,10,z+3),"steel")
            box((x-.6,7.5,z-.6),(x+2.6,8.1,z+3.6),"light_steel")
    # Cooling banks: real thin fins, capped headers and connected return branches.
    for x,edge in ((4,18),(66,62)):
        for z in range(24,60,3):box((x,13,z),(x+10,39,z+.65),"dark_paint")
        for y in (12,40):
            cylinder("z",(x+5,y,0),22,61,1.15,"steel")
            for z in (25,57):cylinder("x",(0,y,z),min(x+5,edge),max(x+5,edge),.9,"steel")
        for z in (25,56):box((x+3,3.3,z),(x+7,12.1,z+2.5),"dark")
        for z in (25,57):
            cylinder("z",(x+5,40,0),z-.4,z+.4,1.6,"light_steel")
    # Low roof ducts occupy the three existing upper part cells and join the cover below.
    for x in (24,40,56):
        box((x-2.1,45,36),(x+2.1,48.4,43),"dark_paint")
        box((x-4.4,48.1,33.5),(x+4.4,51,45.5),"paint")
        for z in (35,37,39,41,43):box((x-3.4,50.9,z),(x+3.4,51.4,z+.65),"dark")
        for xx in (x-3.8,x+3.8):box((xx-.3,51,34),(xx+.3,51.6,34.6),"light_steel")
    # Rear casing construction, drain plug and connected service pipe.
    for x in (20,30,40,50,60):box((x,10,61.5),(x+1.2,42.1,62.4),"paint")
    box((27,31,61.6),(53,38,62.5),"black")
    # South-facing plate uses the same original label artwork, mapped upright.
    out.append(((28,32,62.4),(52,37,62.7),{"*":"tower_detail_dark","south":PREFIX+"cooling!"}))
    cylinder("x",(0,10,54),17,24,1,"steel")
    cylinder("x",(0,10,54),16.4,17.3,1.7,"dark")
    box((16.1,8.8,52.8),(16.5,11.2,55.2),"light_steel")
    return out


def write_textures(root=ROOT):
    originals=root/"art/tower-power-exchange/originals";runtime=root/"src/main/resources/assets/jugcraft/textures/block"
    originals.mkdir(parents=True,exist_ok=True);runtime.mkdir(parents=True,exist_ok=True)
    entries=[]
    for name,img in textures().items():
        source=originals/(name+".png");target=runtime/source.name;img.save(source);target.write_bytes(source.read_bytes())
        entries.append({"source":source.relative_to(root).as_posix(),"runtime":target.relative_to(root).as_posix(),"size":list(img.size),"sha256":hashlib.sha256(source.read_bytes()).hexdigest()})
    provenance={"author":"OpenAI Codex (GPT-6)","license":"MIT","date":"2026-10-08","source":"Original deterministic code-native geometry and instrument drawings; no screenshot or third-party pixels imported.",
                "references":["tools/plant_panels.py: authored Jugcraft cabinet proportions, engraved labels, 240-degree percentage instruments and proud meter cases","src/client/java/io/github/jimbozoomer/jugcraft/client/LivePanelRenderer.java: static faces at rest with client-only truthful overlays","art/owner-library/README.md and catalog/files.csv reviewed; no owner-library pixels imported"],
                "shared_materials":"Existing tower_detail steel/concrete/rubber textures referenced unchanged; original provenance remains art/tower-v3-sample/README.md.","layout":LAYOUT,"textures":entries}
    mw.write(root/"art/tower-power-exchange/provenance.json",provenance)
    mw.write(root/"src/main/resources/assets/jugcraft/tower_power_exchange_panels.json",LAYOUT)


def write_assets(root=ROOT):
    from tower_exchanges import footprint
    write_textures(root);assets=root/"src/main/resources/assets/jugcraft";elements=geometry()
    lookup={t:"jugcraft:block/"+t for t in mw.texture_names(elements)};lookup["particle"]="jugcraft:block/"+PREFIX+"paint"
    parts=mw.split_model("energy_exchange",elements,footprint(True))
    for part,els in enumerate(parts):mw.write(assets/"models/block"/f"energy_exchange_part{part}.json",{"parent":"minecraft:block/block","ambientocclusion":False,"textures":lookup,"elements":els})
    variants={f"facing={facing},part={p}":{"model":f"jugcraft:block/energy_exchange_part{p}","y":angle} for p in range(64) for facing,angle in mw.FACING_Y.items()}
    mw.write(assets/"blockstates/energy_exchange.json",{"variants":variants})
    item=mw.scaled_elements(elements);mw.finish_elements(item)
    mw.write(assets/"models/item/energy_exchange.json",{"parent":"minecraft:block/block","ambientocclusion":False,"textures":lookup,"elements":item,"display":{"gui":{"rotation":[25,-135,0],"translation":[0,0,0],"scale":[.65,.65,.65]},"ground":{"scale":[.3,.3,.3]},"fixed":{"scale":[.6,.6,.6]}}})
    print(f"Wrote energy exchange only:64 native parts,{len(elements)} authored solids,{len(textures())} original textures.")


def preview():
    import numpy as np
    import armor_preview as raster
    from tower_exchanges import exported_assembly
    _,faces,_=exported_assembly("energy_exchange",True);images={f.tex:np.array(Image.open(f.tex).convert("RGBA")) for f in faces};panels=[]
    for label,view,scale in (("INSTRUMENT FRONT",(0,0),16),("FRONT / COOLING",(32,18),10),("REAR / SEALED TRANSFORMER",(155,18),10)):
        camera=raster.camera(view)[0];quads=[(np.array(f.pts),np.array(f.uvs),np.array(f.normal),images[f.tex]) for f in faces if np.dot(f.normal,camera)>0]
        img=raster.render(quads,view,scale=scale,background=(61,65,65),lighting="blockbench")
        panel=Image.new("RGB",(1500,1120),(61,65,65));panel.paste(img,((1500-img.width)//2,60+(1000-img.height)//2));d=ImageDraw.Draw(panel)
        d.text((28,18),label+"  |  EXPORTED JSON + PNG",font=font(23,True),fill=WHITE)
        d.text((28,1081),"Native asset review: static faces are blank; Minecraft supplies live JE figures and needles.",font=font(20),fill=(202,207,201))
        panels.append(panel)
    output=ROOT/"build/drone_tower/power_exchange_switchgear_native.png";output.parent.mkdir(parents=True,exist_ok=True);raster.sheet(panels,3).save(output);print(output)
    close=[(np.array(f.pts),np.array(f.uvs),np.array(f.normal),images[f.tex]) for f in faces
           if f.normal[2]<-.99 and min(p[0] for p in f.pts)>=16 and max(p[0] for p in f.pts)<=64
           and min(p[1] for p in f.pts)>=6 and max(p[1] for p in f.pts)<=48 and max(p[2] for p in f.pts)<=18]
    detail=raster.render(close,(0,0),scale=28,background=(61,65,65),lighting="blockbench")
    detail.save(output.with_name("power_exchange_instruments_native.png"));print(output.with_name("power_exchange_instruments_native.png"))


if __name__=="__main__":
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument("--write",action="store_true");parser.add_argument("--check",action="store_true");parser.add_argument("--preview",action="store_true");args=parser.parse_args()
    if args.write:write_assets()
    if args.check:
        from tower_exchanges import check_assets
        check_assets()
    if args.preview:preview()
