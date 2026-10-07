/* Jugcraft World Designer — original MIT code. Shared browser/Node model and pack compiler. */
(function (root) {
  'use strict';
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const copy = value => JSON.parse(JSON.stringify(value));
  const identifier = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
  function createDesign() {
    const width = 65, step = 32, heights = [];
    for (let z = 0; z < width; z++) for (let x = 0; x < width; x++) {
      const dx = (x - 32) / 32, dz = (z - 32) / 32;
      const ridge = 53 * Math.exp(-((dx + .48) ** 2 * 24 + (dz - .22) ** 2 * 3));
      const hill = 26 * Math.exp(-((dx - .55) ** 2 * 13 + (dz + .4) ** 2 * 9));
      heights.push(Math.round(77 + ridge + hill + 3 * Math.sin(x / 6) * Math.sin(z / 8)));
    }
    return {schema: 1, name: 'The first valley', grid: {origin_x: -1024, origin_z: -1024, step, width, heights},
      biomes: Array(width * width).fill(''), spawn: {x: 0, z: 0}, town: null, structures: []};
  }
  function index(d, x, z) {
    const g = d.grid;
    return clamp(Math.round((z - g.origin_z) / g.step), 0, g.width - 1) * g.width + clamp(Math.round((x - g.origin_x) / g.step), 0, g.width - 1);
  }
  function height(d, x, z) {
    const g = d.grid, u = clamp((x - g.origin_x) / g.step, 0, g.width - 1), v = clamp((z - g.origin_z) / g.step, 0, g.width - 1);
    const ix = Math.min(Math.floor(u), g.width - 2), iz = Math.min(Math.floor(v), g.width - 2), tx = u - ix, tz = v - iz;
    return (g.heights[iz*g.width+ix]*(1-tx)+g.heights[iz*g.width+ix+1]*tx)*(1-tz)
      +(g.heights[(iz+1)*g.width+ix]*(1-tx)+g.heights[(iz+1)*g.width+ix+1]*tx)*tz;
  }
  function weight(d, x, z) {
    const g=d.grid, end=(g.width-1)*g.step;
    const t=clamp(Math.min(x-g.origin_x,z-g.origin_z,g.origin_x+end-x,g.origin_z+end-z)/(2*g.step),0,1);
    return t*t*(3-2*t);
  }
  function paint(d, x, z, tool, radius, strength, target, biome) {
    const g=d.grid, prior=tool==='smooth' ? g.heights.slice() : null;
    const minX=clamp(Math.floor((x-radius-g.origin_x)/g.step),0,g.width-1), maxX=clamp(Math.ceil((x+radius-g.origin_x)/g.step),0,g.width-1);
    const minZ=clamp(Math.floor((z-radius-g.origin_z)/g.step),0,g.width-1), maxZ=clamp(Math.ceil((z+radius-g.origin_z)/g.step),0,g.width-1);
    for(let iz=minZ; iz<=maxZ; iz++) for(let ix=minX; ix<=maxX; ix++) {
      const dist=Math.hypot(g.origin_x+ix*g.step-x,g.origin_z+iz*g.step-z);
      if(dist>radius) continue;
      const i=iz*g.width+ix, falloff=.5+.5*Math.cos(Math.PI*dist/radius), h=g.heights[i];
      if(tool==='biome') d.biomes[i]=biome;
      else if(tool==='erase') d.biomes[i]='';
      else if(tool==='raise'||tool==='lower') g.heights[i]=clamp(h+(tool==='raise'?1:-1)*strength*falloff,40,256);
      else if(tool==='flatten'||tool==='river') {
        g.heights[i]=h+((tool==='river'?59:target)-h)*Math.min(1,strength/8)*falloff;
        if(tool==='river'&&falloff>.3) d.biomes[i]='minecraft:river';
      } else if(tool==='smooth') {
        let sum=0,n=0;
        for(let dz=-1;dz<=1;dz++) for(let dx=-1;dx<=1;dx++) {sum+=prior[clamp(iz+dz,0,g.width-1)*g.width+clamp(ix+dx,0,g.width-1)];n++;}
        g.heights[i]=h+(sum/n-h)*falloff*Math.min(1,strength/8);
      }
      g.heights[i]=Math.round(g.heights[i]*100)/100;
    }
  }
  function snapTown(x,z,size) { const half=size/2; return {x:Math.floor((x-half+8)/16)*16+half,z:Math.floor((z-half+8)/16)*16+half}; }
  function validateCatalog(c) {
    const ids=a=>Array.isArray(a)&&a.length>0&&a.every(v=>typeof v==='string'&&identifier.test(v));
    if(!c||c.schema!==1||c.minecraft!=='26.3') return ['Load a catalog exported by Jugcraft on Minecraft 26.3 first.'];
    if(!ids(c.biomes)||!Array.isArray(c.structures)||c.structures.some(s=>!s||typeof s.id!=='string'||!identifier.test(s.id)||!Array.isArray(s.biomes)||s.biomes.some(b=>typeof b!=='string'||!identifier.test(b)))) return ['The catalog has invalid biome or structure records.'];
    if(!Number.isInteger(c.townSize)||c.townSize<16||c.townSize>1024||c.townSize%16!==0) return ['The catalog has an invalid city size.'];
    if(!Array.isArray(c.packFormat)||c.packFormat.length!==2||c.packFormat.some(n=>!Number.isInteger(n)||n<0)||!c.noiseSettings?.noise_router?.final_density||!c.noiseSettings.noise_router.chunk_surface_level||!c.biomeSource?.type) return ['The catalog is missing world-generation settings. Export a new catalog from the game.'];
    return [];
  }
  // Opening a saved work-in-progress must remain possible even when its placements need repair.
  function validateShape(d) {
    if(!d||d.schema!==1||typeof d.name!=='string'||d.name.length<1||d.name.length>80) return ['Invalid design name or version.'];
    const g=d.grid, point=p=>p&&Number.isInteger(p.x)&&Number.isInteger(p.z)&&Math.abs(p.x)<=2000000&&Math.abs(p.z)<=2000000;
    if(!g||!Number.isInteger(g.width)||g.width<5||g.width>129||![4,8,16,32,64,128].includes(g.step)||!Number.isInteger(g.origin_x)||!Number.isInteger(g.origin_z)||Math.abs(g.origin_x)>1000000||Math.abs(g.origin_z)>1000000) return ['Invalid grid bounds or resolution.'];
    if(!Array.isArray(g.heights)||g.heights.length!==g.width*g.width||g.heights.some(h=>!Number.isFinite(h)||h<40||h>256)||!Array.isArray(d.biomes)||d.biomes.length!==g.width*g.width||d.biomes.some(b=>typeof b!=='string'||b!==''&&!identifier.test(b))) return ['Invalid terrain or biome samples.'];
    if(!point(d.spawn)||(d.town!==null&&!point(d.town))||!Array.isArray(d.structures)||d.structures.length>32||d.structures.some(s=>!point(s)||typeof s.id!=='string'||!identifier.test(s.id))) return ['Invalid landmark records.'];
    return [];
  }
  function validate(d,catalog, exporting=false) {
    const errors=[];
    if(!d||d.schema!==1) return ['Unsupported design version.'];
    if(typeof d.name!=='string'||d.name.length<1||d.name.length>80) errors.push('Name must contain 1–80 characters.');
    const g=d.grid;
    if(!g||!Number.isInteger(g.width)||g.width<5||g.width>129||![4,8,16,32,64,128].includes(g.step)
      ||!Number.isInteger(g.origin_x)||!Number.isInteger(g.origin_z)||Math.abs(g.origin_x)>1000000||Math.abs(g.origin_z)>1000000) return [...errors,'Invalid grid bounds or resolution.'];
    if(!Array.isArray(g.heights)||g.heights.length!==g.width*g.width||g.heights.some(h=>!Number.isFinite(h)||h<40||h>256)) errors.push('Heights must be 40–256, with one value at each grid point.');
    if(!Array.isArray(d.biomes)||d.biomes.length!==g.width*g.width||d.biomes.some(b=>typeof b!=='string'||b!==''&&!identifier.test(b))) errors.push('Invalid biome paint data.');
    if(errors.length) return errors;
    const validPoint=p=>p&&Number.isInteger(p.x)&&Number.isInteger(p.z)&&weight(d,p.x,p.z)===1;
    if(!validPoint(d.spawn)) errors.push('Place spawn inside the design, away from the feathered border.');
    else if(height(d,d.spawn.x,d.spawn.z)<65) errors.push('Spawn must be on dry land (height 65 or above).');
    if(d.town!==null && !validPoint(d.town)) errors.push('Place the city inside the design.');
    if(!Array.isArray(d.structures)||d.structures.length>32) return [...errors,'A design supports at most 32 structure markers.'];
    const all=[];
    if(d.town && validPoint(d.town)) {
      const size=catalog?.townSize||192;
      const snapped=snapTown(d.town.x,d.town.z,size);
      if(snapped.x!==d.town.x||snapped.z!==d.town.z) errors.push('City marker must match the town grid; place it again with the City tool.');
      all.push({...d.town,r:size/2,label:'City'});
      if(height(d,d.town.x,d.town.z)<65) errors.push('City must stand above sea level.');
    }
    const seen=new Set();
    d.structures.forEach((s,i)=>{
      if(!validPoint(s)||typeof s.id!=='string'||!identifier.test(s.id)||((s.x%16)+16)%16!==8||((s.z%16)+16)%16!==8) {errors.push(`Structure ${i+1} needs a valid ID and a start-chunk center inside the design.`);return;}
      const key=`${s.id}:${s.x}:${s.z}`; if(seen.has(key)) errors.push(`Structure ${i+1} duplicates another marker.`); seen.add(key);
      all.push({...s,r:96,label:`Structure ${i+1}`});
      if(exporting) {
        const definition=Array.isArray(catalog?.structures)?catalog.structures.find(a=>a?.id===s.id):null;
        if(!definition) errors.push(`Missing installed structure: ${s.id}`);
        else {
          const painted=d.biomes[index(d,s.x,s.z)];
          if(!painted||!Array.isArray(definition.biomes)||!definition.biomes.includes(painted)) errors.push(`${s.id}: paint a permitted biome at its marker before export.`);
        }
      }
    });
    all.forEach((p,i)=>{
      for(const [dx,dz] of [[-p.r,-p.r],[p.r,p.r],[-p.r,p.r],[p.r,-p.r]]) if(weight(d,p.x+dx,p.z+dz)!==1) {errors.push(`${p.label} footprint reaches the feathered border.`);break;}
      if(validPoint(d.spawn)&&Math.abs(p.x-d.spawn.x)<p.r+24&&Math.abs(p.z-d.spawn.z)<p.r+24) errors.push(`${p.label} is too close to spawn.`);
      all.slice(i+1).forEach(q=>{if(Math.abs(p.x-q.x)<p.r+q.r&&Math.abs(p.z-q.z)<p.r+q.r) errors.push(`${p.label} overlaps ${q.label}.`);});
    });
    if(exporting) {
      const catalogErrors=validateCatalog(catalog);
      if(catalogErrors.length) errors.push(...catalogErrors);
      else {const available=new Set(catalog.biomes); for(const b of new Set(d.biomes.filter(Boolean))) if(!available.has(b)) errors.push(`Missing installed biome: ${b}`);}
      if(new Set(d.biomes.filter(Boolean)).size>512) errors.push('A design supports at most 512 painted biomes.');
    }
    return [...new Set(errors)];
  }
  function packFiles(design,catalog) {
    const errors=validate(design,catalog,true); if(errors.length) throw new Error(errors.join('\n'));
    const d=copy(design), palette=[...new Set(d.biomes.filter(Boolean))];
    if(!palette.length) palette.push(catalog.biomes.includes('minecraft:plains')?'minecraft:plains':catalog.biomes[0]);
    const cells=d.biomes.map(b=>b?palette.indexOf(b)+1:0), settings=copy(catalog.noiseSettings), router=settings.noise_router;
    const files={}, put=(path,obj)=>{files[path]=JSON.stringify(obj,null,2)+'\n';};
    const density={type:'jugcraft:design_density',grid:d.grid,input:router.final_density};
    const surface={type:'jugcraft:design_density',grid:d.grid,input:router.chunk_surface_level,surface:true};
    router.final_density='jugcraft:designer/land'; router.chunk_surface_level='jugcraft:designer/surface';
    if(settings.aquifers) settings.aquifers.surface_level='jugcraft:designer/surface';
    const source={type:'jugcraft:design',schema:1,base:catalog.biomeSource,grid:d.grid,palette,cells,spawn:[d.spawn.x,0,d.spawn.z]};
    if(d.town) source.town=[d.town.x,0,d.town.z];
    put('pack.mcmeta',{pack:{description:`Jugcraft World Designer: ${d.name}`,min_format:catalog.packFormat,max_format:catalog.packFormat}});
    put('data/jugcraft/worldgen/density_function/designer/land.json',density);
    put('data/jugcraft/worldgen/density_function/designer/surface.json',surface);
    put('data/jugcraft/worldgen/noise_settings/designer.json',settings);
    put('data/jugcraft/worldgen/world_preset/designed.json',{dimensions:{
      'minecraft:overworld':{type:'minecraft:overworld',generator:{type:'minecraft:noise',biome_source:source,settings:'jugcraft:designer'}},
      'minecraft:the_nether':{type:'minecraft:the_nether',generator:{type:'minecraft:noise',biome_source:{type:'minecraft:multi_noise',preset:'minecraft:nether'},settings:'minecraft:nether'}},
      'minecraft:the_end':{type:'minecraft:the_end',generator:{type:'minecraft:noise',biome_source:{type:'minecraft:the_end'},settings:'minecraft:end'}}}});
    put('data/minecraft/tags/worldgen/world_preset/normal.json',{replace:false,values:['jugcraft:designed']});
    d.structures.forEach((s,i)=>put(`data/jugcraft/worldgen/structure_set/designer/pin_${i}.json`,{
      structures:[{structure:s.id,weight:1}],placement:{type:'jugcraft:design_pin',chunk_x:Math.floor(s.x/16),chunk_z:Math.floor(s.z/16)}}));
    put('jugcraft-design.json',d);
    files['READ-ME.txt']='Requires Jugcraft with World Designer and the same Minecraft/mod collection used to export the catalog.\nCreate a NEW world; add this ZIP in Data Packs, enable it, then choose Jugcraft Designed in World Type. Keep structures enabled.\nThe painted surface sits above original deep caves; outer two grid cells blend to procedural terrain.\nStructure pins choose start chunks, not exact building corners; biome and terrain constraints still apply. Native random structures remain enabled. One Jugcraft walled city is supported.\nDo not replace this pack in an established world: already generated chunks are not regenerated.\n';
    return files;
  }
  // Small standards-based ZIP writer (stored entries). No downloaded ZIP library or network dependency.
  function zip(files) {
    const encode=new TextEncoder(), entries=[], parts=[]; let offset=0;
    const crc=bytes=>{let c=0xffffffff;for(const b of bytes){c^=b;for(let k=0;k<8;k++) c=(c>>>1)^((c&1)?0xedb88320:0);}return (c^0xffffffff)>>>0;};
    for(const [path,text] of Object.entries(files)) {
      const name=encode.encode(path), data=encode.encode(text), checksum=crc(data), header=new Uint8Array(30+name.length), view=new DataView(header.buffer);
      view.setUint32(0,0x04034b50,true);view.setUint16(4,20,true);view.setUint16(6,0x800,true);view.setUint16(12,33,true);
      view.setUint32(14,checksum,true);view.setUint32(18,data.length,true);view.setUint32(22,data.length,true);view.setUint16(26,name.length,true);header.set(name,30);
      entries.push({name,data,checksum,offset});parts.push(header,data);offset+=header.length+data.length;
    }
    const start=offset;
    for(const e of entries) {
      const header=new Uint8Array(46+e.name.length),v=new DataView(header.buffer);
      v.setUint32(0,0x02014b50,true);v.setUint16(4,20,true);v.setUint16(6,20,true);v.setUint16(8,0x800,true);v.setUint16(14,33,true);
      v.setUint32(16,e.checksum,true);v.setUint32(20,e.data.length,true);v.setUint32(24,e.data.length,true);v.setUint16(28,e.name.length,true);v.setUint32(42,e.offset,true);header.set(e.name,46);parts.push(header);offset+=header.length;
    }
    const end=new Uint8Array(22),v=new DataView(end.buffer);v.setUint32(0,0x06054b50,true);v.setUint16(8,entries.length,true);v.setUint16(10,entries.length,true);v.setUint32(12,offset-start,true);v.setUint32(16,start,true);parts.push(end);
    const result=new Uint8Array(offset+22);let cursor=0;for(const p of parts){result.set(p,cursor);cursor+=p.length;}return result;
  }
  const api={createDesign,copy,index,height,weight,paint,snapTown,validate,validateShape,validateCatalog,packFiles,zip};
  if(typeof module!=='undefined') module.exports=api;
  root.JugcraftDesign=api;
})(globalThis);
