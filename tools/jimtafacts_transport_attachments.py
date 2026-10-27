"""Preserve the source plug mesh and pixels; native six-face routing seals and shared facades."""
from pathlib import Path
import hashlib,json,shutil,copy
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcrafttransport'
SILICON=SOURCE.parent/'buildcraftsilicon'
COLORS=['clear','white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
RGB=[0xFFFFFF,0xF9FFFE,0xF9801D,0xC74EBD,0x3AB3DA,0xFED83D,0x80C71F,0xF38BAA,0x474F52,0x9D9D97,0x169C9C,0x8932B8,0x3C44AA,0x835432,0x5E7C16,0xB02E26,0x1D1D21]
def items():
 import jimtafacts_factory_wires
 return jimtafacts_factory_wires.items() | {'jimta_pipe_blocker','jimta_pipe_power_adaptor','jimta_pipe_facade_solid','jimta_pipe_facade_hollow'}|{'jimta_'+kind+color for kind in ('lens_','filter_lens_') for color in COLORS}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def bridge():
 # Original plugs end at x=4.01; the native six-pixel pipe core begins at x=5.
 # A short connector seats the supplied part on the narrower native core.
 return {'from':[4,6,6],'to':[6.01,10,10],'faces':{face:{'texture':'#all','uv':[6,6,10,10]} for face in ('up','down','north','south','east','west')}}
def generate(shared=True):
 import jimtafacts_factory_wires
 jimtafacts_factory_wires.generate(shared)
 src=SOURCE/'textures/pipes/plug.png';dst=RES/'assets/jugcraft/textures/block/jimtafacts/transport/plug.png';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
 original=SOURCE/'models/plugs/blocker.json';model=json.loads(original.read_text());model['textures']={'all':'jugcraft:block/jimtafacts/transport/plug','particle':'#all'}
 model['elements'].append(bridge())
 dump(RES/'assets/jugcraft/models/item/jimta_pipe_blocker.json',model)
 dump(RES/'assets/jugcraft/items/jimta_pipe_blocker.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_pipe_blocker'}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_transport/pipe_blocker.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['jugcraft:jimta_pipe_structure'],'result':{'id':'jugcraft:jimta_pipe_blocker','count':4}})
 copies=[];lang={'item.jugcraft.jimta_pipe_blocker':'Pipe Blocker'}
 adapter_source=SOURCE/'models/plugs/power_adapter.json';adapter=json.loads(adapter_source.read_text())
 adapter['textures']={'all':'jugcraft:block/jimtafacts/transport/power_adapter','particle':'#all'}
 adapter['elements'].append(bridge())
 dump(RES/'assets/jugcraft/models/item/jimta_pipe_power_adaptor.json',adapter)
 dump(RES/'assets/jugcraft/items/jimta_pipe_power_adaptor.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_pipe_power_adaptor'}})
 adapter_pixels=SOURCE/'textures/pipes/power_adapter.png';adapter_target=RES/'assets/jugcraft/textures/block/jimtafacts/transport/power_adapter.png';shutil.copyfile(adapter_pixels,adapter_target)
 dump(RES/'data/jugcraft/recipe/jimtafacts_transport/pipe_power_adaptor.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['jugcraft:jimta_pipe_structure','jugcraft:basic_circuit','jugcraft:copper_wire','jugcraft:iron_gear'],'result':{'id':'jugcraft:jimta_pipe_power_adaptor'}})
 lang['item.jugcraft.jimta_pipe_power_adaptor']='Pipe Power Adaptor'
 for variant in ('solid','hollow'):
  bounds=[([0,0,0],[16,16,2])] if variant=='solid' else [([0,0,0],[16,4,2]),([0,12,0],[16,16,2]),([0,4,0],[4,12,2]),([12,4,0],[16,12,2])]
  name='jimta_pipe_facade_'+variant
  elements=[{'from':lo,'to':hi,'faces':{face:{'texture':'#all'} for face in ('up','down','north','south','east','west')}} for lo,hi in bounds]
  dump(RES/f'assets/jugcraft/models/item/{name}.json',{'parent':'minecraft:block/block','textures':{'all':'minecraft:block/stone','particle':'#all'},'elements':elements,'display':{'gui':{'rotation':[20,150,0],'translation':[0,0,0],'scale':[.8,.8,.8]}}})
  dump(RES/f'assets/jugcraft/items/{name}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+name}})
  lang['item.jugcraft.'+name]=variant.title()+' Factory Facade'
 dump(RES/'data/jugcraft/recipe/jimtafacts_transport/facade_conversion.json',{'type':'jugcraft:jimta_factory_facade'})
 dump(RES/'data/jugcraft/recipe/jimtafacts_transport/facade_phase.json',{'type':'jugcraft:jimta_factory_facade_phase'})
 guide=SILICON/'compat/buildcraft/guide/en_us/item/plug_facade.md'
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/transport-facade-adaptation.json',{'source_guide':guide.relative_to(ROOT).as_posix(),'source_sha256':hashlib.sha256(guide.read_bytes()).hexdigest(),'adaptation':'Source solid/hollow connection semantics and single-item crafting conversion implemented natively. Sampled vanilla/mod block appearance is resolved in game; two-pixel plate and four rails leave an eight-pixel opening. Item preview uses stone; tooltip identifies the actual stored sample. Existing shared covers retain legacy connection-permitting behavior. No original pixels modified; two-state phases use adjacent matching-color control-network relays; dye plus a second sampled facade configures the on appearance, or glass selects transparency. Physical solid/hollow routing stays fixed across visual phases.'})
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/transport-power-adaptor-copies.json',{'copies':[
  {'source':adapter_source.relative_to(ROOT).as_posix(),'runtime':'src/main/resources/assets/jugcraft/models/item/jimta_pipe_power_adaptor.json','source_sha256':hashlib.sha256(adapter_source.read_bytes()).hexdigest(),'changes':'Texture namespace/source #all key corrected; original parts/UV retained with a short connecting stem to the narrower native pipe core.'},
  {'source':adapter_pixels.relative_to(ROOT).as_posix(),'runtime':adapter_target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(adapter_pixels.read_bytes()).hexdigest(),'changes':'None'}],
  'adaptation':'One power-only face on wood/diamond-wood item/fluid extractors; original cable-driven extraction role with native JE. 2048 JE buffer, 256 JE per insertion; at most 4x pulse, 8 JE per extra item or 25 mB actually moved. Charge follows the removed fitting. No legacy MJ/RF currency.'})
 for name in ('lens','overlay_lens'):
  source=SILICON/f'textures/plugs/{name}.png';target=RES/f'assets/jugcraft/textures/block/jimtafacts/transport/{name}.png';shutil.copyfile(source,target)
  copies.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'None'})
 lens_source=SILICON/'models/plugs/lens.json';lens=json.loads(lens_source.read_text())
 for filter in (False,True):
  for color,rgb in zip(COLORS,RGB):
   elements=copy.deepcopy(lens['cutout'])
   if not filter or color!='clear':elements+=copy.deepcopy(lens['translucent'])
   for element in elements:
    tinted=element.pop('colour',None);element.pop('visible',None)
    for face in element['faces'].values():
     if tinted:face['texture']='#lens';face['tintindex']=0
     else:face['texture']=face['texture'].strip("'")
     if 'rotation' in face:face['rotation']*=90
   textures={'main':'jugcraft:block/jimtafacts/transport/lens','lens':'minecraft:block/water_flow' if color=='clear' else 'jugcraft:block/jimtafacts/transport/overlay_lens','particle':'#main'}
   name='jimta_'+('filter_lens_' if filter else 'lens_')+color
   dump(RES/f'assets/jugcraft/models/item/{name}.json',{'parent':'minecraft:block/block','textures':textures,'elements':elements,'display':{'gui':{'rotation':[30,135,0],'translation':[5,0,0],'scale':[1,1,1]}}})
   dump(RES/f'assets/jugcraft/items/{name}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+name,'tints':[{'type':'minecraft:constant','value':rgb|0xFF000000}]}})
   ingredients=['jugcraft:jimta_lens_'+color,'minecraft:redstone'] if filter else ['minecraft:glass_pane','minecraft:iron_nugget','minecraft:iron_nugget']+([] if color=='clear' else ['minecraft:'+color+'_dye'])
   dump(RES/f'data/jugcraft/recipe/jimtafacts_transport/{name.removeprefix("jimta_")}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':ingredients,'result':{'id':'jugcraft:'+name}})
   lang['item.jugcraft.'+name]=color.replace('_',' ').title()+(' Filter Lens' if filter else ' Transport Lens')
 copies.append({'source':lens_source.relative_to(ROOT).as_posix(),'source_sha256':hashlib.sha256(lens_source.read_bytes()).hexdigest(),'runtime_family':'models/item/jimta_[filter_]lens_*.json','changes':'Original bounds/UVs retained; source dynamic expressions resolved to 17 explicit route colors, rotation quarters converted to degrees, native item tint and inventory display added. Clear filter omits the source-disabled glass pane.'})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text());v.update(lang);dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/transport-attachment-copies.json',{'copies':[{'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'changes':'None'},{'source':original.relative_to(ROOT).as_posix(),'runtime':'src/main/resources/assets/jugcraft/models/item/jimta_pipe_blocker.json','source_sha256':hashlib.sha256(original.read_bytes()).hexdigest(),'changes':'Texture namespace and source #all key corrected. Original parts/UV retained with a short connecting stem to the narrower native pipe core.'}],'adaptation':'Six saved faces on factory item/fluid/power conduits, including active extraction, sorting and disposal endpoints. Blockers close visual arms and actual transfer edges; cached direct ports recheck fittings. Shared sampled facades retain appearance and inventories remain in their existing block entities.'})
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/transport-lens-copies.json',{'copies':copies,'adaptation':'34 removable colored/clear lenses and matching route filters on through-routing factory item pipes, including lapis, daizuli and emzuli. Exit and entry faces apply in order, including injection. Buffered extraction/disposal endpoints accept blockers, facades and applicable power adaptors, not route-color lenses. Cargo components remain unchanged.'})
if __name__=='__main__':generate()
