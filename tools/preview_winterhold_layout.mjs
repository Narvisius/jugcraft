/**
 * Optional dependency-free NBT validation and architectural previews.
 * node tools/preview_winterhold_layout.mjs [--check] [--output-dir docs/images]
 * This reads the committed template; it never launches Minecraft or verifies
 * the Python generator. Shapes/colours are approximate architectural aids.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { gunzipSync, deflateRawSync } from "node:zlib";

function crc32(a) { let c=0xffffffff;for(const b of a){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return (c^0xffffffff)>>>0;}

function readNbt(a) {
 let p=0;const view=new DataView(a.buffer,a.byteOffset,a.byteLength);
 function str(){const n=view.getUint16(p);p+=2;let s="";for(let i=0;i<n;i++)s+=String.fromCharCode(a[p++]);return decodeURIComponent(escape(s));}
 function val(t) {if(t===1)return {byte:view.getInt8(p++)};if(t===3){const n=view.getInt32(p);p+=4;return n;}if(t===6){const n=view.getFloat64(p);p+=8;return n;}if(t===8)return str();if(t===9){const child=a[p++],n=view.getInt32(p);p+=4;return Array.from({length:n},()=>val(child));}if(t===10){const o={};let child;while(child=a[p++]){const name=str();o[name]=val(child);}return o;}throw Error("tag "+t);}
 if(a[p++]!==10||str()!=="")throw Error("root");const root=val(10);if(p!==a.length)throw Error("trailing NBT");return root;
}
function writeNbt(root) {
 const a=[];const byte=n=>a.push(n&255),int=n=>{byte(n>>>24);byte(n>>>16);byte(n>>>8);byte(n);},str=s=>{const u=unescape(encodeURIComponent(s));byte(u.length>>>8);byte(u.length);for(let i=0;i<u.length;i++)byte(u.charCodeAt(i));};
 const tag=v=>typeof v==="object"&&v!==null&&"byte"in v?1:Number.isInteger(v)?3:typeof v==="number"?6:typeof v==="string"?8:Array.isArray(v)?9:10;
 function val(t,v){if(t===1)byte(v.byte);else if(t===3)int(v);else if(t===6){const b=new Uint8Array(8);new DataView(b.buffer).setFloat64(0,v);for(const x of b)byte(x);}else if(t===8)str(v);else if(t===9){const c=v.length?tag(v[0]):0;byte(c);int(v.length);for(const x of v)val(c,x);}else {for(const [k,x]of Object.entries(v)){const c=tag(x);byte(c);str(k);val(c,x);}byte(0);}}
 byte(10);str("");val(10,root);return Uint8Array.from(a);
}


function compressDeflate(raw) { return deflateRawSync(raw); }
function pngEncode(width,height,pixels) {
 const raw=new Uint8Array((width*3+1)*height);for(let y=0;y<height;y++)raw.set(pixels.subarray(y*width*3,(y+1)*width*3),y*(width*3+1)+1);
 const def=compressDeflate(raw),z=new Uint8Array(def.length+6);z.set([120,1]);z.set(def,2);let a=1,b=0;for(const n of raw){a=(a+n)%65521;b=(b+a)%65521;}new DataView(z.buffer).setUint32(z.length-4,(b<<16|a)>>>0);
 const chunks=[];function chunk(type,data){const p=new Uint8Array(data.length+12),v=new DataView(p.buffer);v.setUint32(0,data.length);p.set([...type].map(c=>c.charCodeAt(0)),4);p.set(data,8);v.setUint32(p.length-4,crc32(p.subarray(4,p.length-4)));chunks.push(p);}
 const ihdr=new Uint8Array(13),iv=new DataView(ihdr.buffer);iv.setUint32(0,width);iv.setUint32(4,height);ihdr.set([8,2,0,0,0],8);chunk("IHDR",ihdr);chunk("IDAT",z);chunk("IEND",new Uint8Array());
 const out=new Uint8Array(8+chunks.reduce((n,c)=>n+c.length,0));out.set([137,80,78,71,13,10,26,10]);let p=8;for(const c of chunks){out.set(c,p);p+=c.length;}return out;
}


function verifyAcademy(t,spec) {
 const [sx,sy,sz]=t.size,N=sx*sy*sz,idx=(x,y,z)=>(x*sy+y)*sz+z,grid=new Uint8Array(N),map=new Map(t.blocks.map(b=>[b.pos.join(","),t.palette[b.state]]));
 const id=(x,y,z)=>map.get([x,y,z].join(","))?.id??"minecraft:air";
 for(const b of t.blocks){const s=t.palette[b.state];grid[idx(...b.pos)]=["minecraft:air","minecraft:snow","minecraft:spruce_door"].includes(s.id)?0:1;}
 const free=(x,y,z)=>!grid[idx(x,y,z)];
 const stand=(x,y,z)=>x>=0&&x<sx&&y>=1&&y<sy-1&&z>=0&&z<sz&&free(x,y,z)&&free(x,y+1,z)&&!free(x,y-1,z);
 const visited=new Uint8Array(N),queue=[spec.entry];visited[idx(...spec.entry)]=1;
 for(let p=0;p<queue.length;p++){const [x,y,z]=queue[p];for(const [dx,dz]of [[1,0],[-1,0],[0,1],[0,-1]])for(const dy of [0,1,-1]){const a=x+dx,b=y+dy,c=z+dz,k=idx(a,b,c);if(!visited[k]&&stand(a,b,c)&&(dy<=0||free(x,y+2,z))){visited[k]=1;queue.push([a,b,c]);}}}
 const failures=[];
 if(!stand(...spec.entry))failures.push("Entry has no standing access");
 for(const [name,pos]of Object.entries(spec.rooms))if(!visited[idx(...pos)])failures.push("Unreachable "+name+" "+pos+" block="+id(...pos));
 for(const [name,[pos,want]]of Object.entries(spec.stations)){const [x,y,z]=pos;if(id(x,y,z)!==want)failures.push("Station missing "+name);
 if(![[1,0],[-1,0],[0,1],[0,-1]].some(([dx,dz])=>visited[idx(x+dx,y,z+dz)]))failures.push("Station unreachable "+name);}
 let doors=0,beds=0,signs=0,snow=0;
 for(const b of t.blocks){const [x,y,z]=b.pos,s=t.palette[b.state],props=s.properties??{};
 if(s.id==="minecraft:blue_bed"){beds++;const other=map.get([x,y,z+(props.part==="foot"?-1:1)].join(","));if(other?.id!==s.id||other.properties.part===props.part||id(x,y-1,z)==="minecraft:air")failures.push("bed "+b.pos);}
 if(s.id==="minecraft:spruce_door"&&props.half==="lower"){doors++;const other=map.get([x,y+1,z].join(","));if(other?.id!==s.id||other.properties.half!=="upper"||id(x,y-1,z)==="minecraft:air")failures.push("door "+b.pos);}
 if(s.id==="minecraft:spruce_wall_sign"){signs++;if(id(x,y,z-1)==="minecraft:air")failures.push("sign "+b.pos);}
 if(s.id==="minecraft:snow"){snow++;if(!["minecraft:stone_bricks","minecraft:chiseled_stone_bricks","minecraft:polished_andesite"].includes(id(x,y-1,z)))failures.push("snow "+b.pos);}
 if(b.nbt&&b.nbt.id!=="minecraft:sign")failures.push("Unexpected block NBT "+b.pos);
 if(b.nbt&&["Items","LootTable","owner","Book","book"].some(k=>k in b.nbt))failures.push("forbidden state "+b.pos);
 }
 return {failures,reachable:queue.length,doors,beds,signs,snow,roomCount:Object.keys(spec.rooms).length,blocks:t.blocks.length,palette:t.palette.length};
}


function renderAcademy(t,mode) {
 const w=mode==="plan"?1080:1440,h=mode==="plan"?1220:mode==="bridge"?900:1080,pix=new Uint8Array(w*h*3);
 for(let i=0;i<pix.length;i+=3){pix[i]=20;pix[i+1]=29;pix[i+2]=41;}
 const pixel=(x,y,c)=>{if(x<0||x>=w||y<0||y>=h)return;const p=(Math.floor(y)*w+Math.floor(x))*3;pix[p]=c[0];pix[p+1]=c[1];pix[p+2]=c[2];};
 const rect=(x,y,a,b,c)=>{for(let j=y;j<y+b;j++)for(let i=x;i<x+a;i++)pixel(i,j,c);};
 const poly=(pts,c)=>{let lo=Math.max(0,Math.ceil(Math.min(...pts.map(p=>p[1])))),hi=Math.min(h-1,Math.floor(Math.max(...pts.map(p=>p[1]))));for(let y=lo;y<=hi;y++){const xs=[];for(let i=0;i<pts.length;i++){const a=pts[i],b=pts[(i+1)%pts.length];if((a[1]<=y+.5&&b[1]>y+.5)||(b[1]<=y+.5&&a[1]>y+.5))xs.push(a[0]+(y+.5-a[1])*(b[0]-a[0])/(b[1]-a[1]));}xs.sort((a,b)=>a-b);for(let i=0;i+1<xs.length;i+=2)for(let x=Math.ceil(xs[i]);x<xs[i+1];x++)pixel(x,y,c);}};
 const font={
 A:["01110","10001","10001","11111","10001","10001","10001"],B:["11110","10001","10001","11110","10001","10001","11110"],C:["01111","10000","10000","10000","10000","10000","01111"],D:["11110","10001","10001","10001","10001","10001","11110"],E:["11111","10000","10000","11110","10000","10000","11111"],F:["11111","10000","10000","11110","10000","10000","10000"],G:["01111","10000","10000","10111","10001","10001","01111"],H:["10001","10001","10001","11111","10001","10001","10001"],I:["11111","00100","00100","00100","00100","00100","11111"],J:["00111","00010","00010","00010","10010","10010","01100"],K:["10001","10010","10100","11000","10100","10010","10001"],L:["10000","10000","10000","10000","10000","10000","11111"],M:["10001","11011","10101","10101","10001","10001","10001"],N:["10001","11001","10101","10011","10001","10001","10001"],O:["01110","10001","10001","10001","10001","10001","01110"],P:["11110","10001","10001","11110","10000","10000","10000"],Q:["01110","10001","10001","10001","10101","10010","01101"],R:["11110","10001","10001","11110","10100","10010","10001"],S:["01111","10000","10000","01110","00001","00001","11110"],T:["11111","00100","00100","00100","00100","00100","00100"],U:["10001","10001","10001","10001","10001","10001","01110"],V:["10001","10001","10001","10001","10001","01010","00100"],W:["10001","10001","10001","10101","10101","10101","01010"],X:["10001","10001","01010","00100","01010","10001","10001"],Y:["10001","10001","01010","00100","00100","00100","00100"],Z:["11111","00001","00010","00100","01000","10000","11111"],
 "0":["01110","10011","10101","10101","11001","10001","01110"],"1":["00100","01100","00100","00100","00100","00100","01110"],"2":["01110","10001","00001","00010","00100","01000","11111"],"3":["11110","00001","00001","01110","00001","00001","11110"],"4":["00010","00110","01010","10010","11111","00010","00010"],"5":["11111","10000","10000","11110","00001","00001","11110"],"6":["01110","10000","10000","11110","10001","10001","01110"],"7":["11111","00001","00010","00100","01000","01000","01000"],"8":["01110","10001","10001","01110","10001","10001","01110"],"9":["01110","10001","10001","01111","00001","00001","01110"],"-":["00000","00000","00000","11111","00000","00000","00000"],".":["00000","00000","00000","00000","00000","00100","00100"],":":["00000","00100","00100","00000","00100","00100","00000"],"/":["00001","00010","00010","00100","01000","01000","10000"]};
 const text=(s,x,y,scale=2,c=[226,235,241])=>{for(const ch of s.toUpperCase()){const g=font[ch];if(g)for(let j=0;j<7;j++)for(let i=0;i<5;i++)if(g[j][i]==="1")rect(x+i*scale,y+j*scale,scale,scale,c);x+=6*scale;}};
 const color=s=>{const n=s.id.split(":")[1];return n==="red_leaf_pile"?[142,58,48]:n==="orange_leaf_pile"?[206,113,51]:n==="yellow_leaf_pile"?[211,163,70]:n==="carved_pumpkin"?[191,108,42]:n==="snow"?[235,245,250]:n==="andesite"?[99,111,118]:n==="tuff"?[101,117,107]:n.includes("deepslate")?[50,61,77]:n.includes("amethyst")?[166,123,212]:n.includes("orange_stained_glass")?[202,116,61]:n.includes("glass")?[92,157,190]:n==="bookshelf"?[123,108,77]:n==="ecology_red_maple_leaves"?[146,52,45]:n==="ecology_orange_maple_leaves"?[207,104,39]:n==="ecology_yellow_maple_leaves"?[218,165,58]:n==="ecology_maple_log"?[87,63,40]:s.id.startsWith("jugcraft:")?[179,128,207]:n.includes("lantern")||n==="soul_campfire"?[239,189,106]:n.includes("spruce")||n==="barrel"?[121,93,67]:n.includes("bed")||n==="blue_terracotta"?[70,111,148]:n==="moss_block"||["fern","poppy","dandelion"].includes(n)?[82,124,83]:n.includes("chiseled")?[128,144,157]:n.includes("cracked")?[124,136,145]:[155,168,178];};
 const cells=new Map(t.blocks.filter(b=>t.palette[b.state].id!=="minecraft:air").map(b=>[b.pos.join(","),b]));
 const shade=(c,v)=>c.map(n=>Math.round(n*v));
 text("FROSTWARD COLLEGE",44,28,4);
 text(mode==="plan"?"CORRECTED GROUND FLOOR - CUT ABOVE DOORWAYS":mode==="bridge"?"BRIDGE APPROACH - GENERATED VOXEL PREVIEW":"CORRECTED CAMPUS - GENERATED VOXEL PREVIEW",44,70,2,[137,192,212]);
 if(mode==="plan"){
  const scale=5,ox=72,oz=100,top=new Map();
  for(const b of cells.values()){const [x,y,z]=b.pos;if(y>28)continue;const k=x+","+z;if(!top.has(k)||top.get(k).pos[1]<y)top.set(k,b);}
  for(const b of top.values()){const [x,y,z]=b.pos;rect(ox+x*scale,oz+z*scale,scale,scale,shade(color(t.palette[b.state]),y<24?.55:y===24?.72:1));}
  const mark=(n,x,z)=>{const a=ox+x*scale,b=oz+z*scale;rect(a-11,b-12,24,25,[20,29,41]);text(String(n),a-5,b-8,2);};
  for(const [n,x,z]of [[1,68,143],[2,68,94],[3,27,76],[4,109,92],[5,68,33],[6,43,33],[7,147,34]])mark(n,x,z);
  text("1 BRIDGE    2 COURTYARD    3 ATTAINMENT",44,1095,2);
  text("4 COUNTENANCE    5 ELEMENTS    6 ARCH-MAGE ABOVE",44,1120,2);
  text("7 ARCANAEUM - JOINED BY SCHOLARLY WING",44,1145,2,[137,192,212]);
  text("ACTUAL TEMPLATE CELLS - NOT A MINECRAFT SCREENSHOT",44,1190,2,[154,168,181]);
 }else if(mode==="bridge"){
  // A front-facing depth projection of the committed cells. This is an
  // architectural visibility aid, not a Minecraft screenshot.
  const blocks=[...cells.values()].filter(b=>b.pos[1]>=24).sort((a,b)=>a.pos[2]-b.pos[2]||a.pos[1]-b.pos[1]);
  for(const b of blocks){const [x,y,z]=b.pos,s=t.palette[b.state],k=2.7+2.3*z/160;
   const sx=Math.round(720+(x-68)*k),sy=Math.round(735-(161-z)*1.6-(y-24)*k);
   rect(sx,sy,Math.ceil(k)+1,Math.ceil(k)+1,shade(color(s),.72+.28*z/160));}
  text("BRIDGE / VAULTED GATE / OPEN COURT / DOMINANT HALL",44,825,2,[137,192,212]);
  text("ACTUAL TEMPLATE CELLS - NOT A MINECRAFT SCREENSHOT",44,855,2,[154,168,181]);
 }else{
  const scale=3.7,ox=746,oy=245,project=(x,y,z)=>[ox+(x-z)*scale,oy+(x+z)*scale*.5-y*scale];
  const blocks=[...cells.values()].sort((a,b)=>a.pos[0]+a.pos[1]+a.pos[2]-(b.pos[0]+b.pos[1]+b.pos[2]));
  for(const b of blocks){
   const [x,y,z]=b.pos,s=t.palette[b.state],c=color(s),p=s.properties??{};
   let height=s.id==="minecraft:snow"?.125:s.id.endsWith("_slab")&&p.type==="bottom"?.5:s.id.endsWith("_bed")?.5625:1;
   const pos=(a,b,c)=>project(x+a,y+b,z+c);
   if(!cells.has([x,y,z+1].join(",")))poly([pos(0,0,1),pos(1,0,1),pos(1,height,1),pos(0,height,1)],shade(c,.68));
   if(!cells.has([x+1,y,z].join(",")))poly([pos(1,0,0),pos(1,0,1),pos(1,height,1),pos(1,height,0)],shade(c,.84));
   if(!cells.has([x,y+1,z].join(",")))poly([pos(0,height,0),pos(1,height,0),pos(1,height,1),pos(0,height,1)],c);
  }
  text("CLOISTER / GATEHOUSE / CONNECTED READING WING",44,990,2,[137,192,212]);
  text("ACTUAL TEMPLATE CELLS - COLOURS AND BLOCK SHAPES APPROXIMATED",44,1020,2,[154,168,181]);
  text(`${t.size.join(" X ")} BLOCKS - NOT A MINECRAFT SCREENSHOT`,44,1046,2,[154,168,181]);
 }
 return {width:w,height:h,pixels:pix};
}

const root = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const raw = gunzipSync(fs.readFileSync(path.join(root, "src/main/resources/data/jugcraft/structure/academy/winterhold.nbt")));
const template = readNbt(raw);
const layout = JSON.parse(fs.readFileSync(path.join(root, "tools/winterhold_layout.json"), "utf8"));
assert.deepEqual(writeNbt(template), new Uint8Array(raw), "NBT primitive round-trip");
assert.equal(template.DataVersion, 5023);
assert.deepEqual(template.size, layout.size);
assert.equal(template.entities.length, 0);
assert.equal(new Set(template.blocks.map(b => b.pos.join(","))).size, template.blocks.length);
assert.ok(template.blocks.length < 350000);
for (const b of template.blocks) {
  assert.ok(b.pos.every((v, i) => Number.isInteger(v) && v >= 0 && v < template.size[i]), "position bounds");
  assert.ok(Number.isInteger(b.state) && b.state >= 0 && b.state < template.palette.length, "palette index");
}
for (const entry of template.palette) {
  assert.ok(entry.id.startsWith("minecraft:") || entry.id.startsWith("jugcraft:"));
  assert.ok(!("Name" in entry) && !("Properties" in entry));
}
const result = verifyAcademy(template, layout);
assert.deepEqual(result.failures, []);
assert.equal(result.doors, 8);
assert.equal(result.beds, 16);
const cells = new Map(template.blocks.map(b => [b.pos.join(","), template.palette[b.state].id]));
for (const pos of [[38,32,85], [98,32,85], [68,32,56], [112,46,34], [90,36,25], [133,44,34], [147,44,34], [153,36,42]]) {
  assert.notEqual(cells.get(pos.join(",")) ?? "minecraft:air", "minecraft:air", "enclosure/library");
}
for (const pos of [[32,24,121], [104,24,121], [68,31,85]]) {
  assert.equal(cells.get(pos.join(",")) ?? "minecraft:air", "minecraft:air", "court/corner clearance");
}
assert.ok(["minecraft:andesite", "minecraft:tuff"].includes(cells.get("68,0,85")));
assert.equal(template.blocks.filter(b => template.palette[b.state].id === "jugcraft:conclave_lectern").length, 1);
console.log(JSON.stringify({status: "passed", ...result}));
if (!process.argv.includes("--check")) {
  const i = process.argv.indexOf("--output-dir");
  const out = i >= 0 ? path.resolve(process.argv[i + 1]) : path.join(root, "docs/images");
  fs.mkdirSync(out, {recursive: true});
  for (const mode of ["overview", "plan", "bridge"]) {
    const picture = renderAcademy(template, mode);
    fs.writeFileSync(path.join(out, "frostward-academy-revised-" + mode + ".png"),
      pngEncode(picture.width, picture.height, picture.pixels));
  }
}
