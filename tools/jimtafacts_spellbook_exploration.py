"""Original exploration item art, compass frames and supplied encoded lore pages."""
import json,re,base64
NAMES=('furled_map','furled_map_ancient','furled_map_citadel','furled_map_ice_spider_den','wayward_compass','ruined_book','unchained_book','cinderous_soulcaller')
def items():return ['jimta_spellbooks_'+n for n in NAMES]
def generate(definition,model,dump,RES,ART,lang):
 texts=json.loads((ART/'lang/en_us.json').read_text(encoding='utf-8'))
 for n in NAMES:
  data=definition('irons_spellbooks:item/'+n,'jimta_spellbooks_'+n)
  if n=='wayward_compass':
   source=json.loads((ART/'models/item/wayward_compass.json').read_text(encoding='utf-8'));data={'model':{'type':'minecraft:range_dispatch','property':'minecraft:compass','target':'lodestone','wobble':True,'fallback':{'type':'minecraft:model','model':model('irons_spellbooks:item/wayward_compass')},'entries':[{'threshold':o['predicate']['angle'],'model':{'type':'minecraft:model','model':model(o['model'])}} for o in source['overrides']]}}
  dump(RES/f'assets/jugcraft/items/jimta_spellbooks_{n}.json',data);lang['item.jugcraft.jimta_spellbooks_'+n]=texts.get('item.irons_spellbooks.'+n,n.replace('_',' ').title())
 # Literal source text, statically decoded from UnchainedBookItem.class; no supplied code executed.
 pages=['Even those who never thought themselves to be magically inclined began to feel their spirits be tugged towards the anomaly. Of those who did, they could only feel a visceral dread begin to seep from their bones and out into every crevice of their bodies.', 'Only too late did they realize that every soul on that battlefield suddenly lost free will; their right to live had been claimed by the demigod.']
 dump(RES/'jugcraft/jimtafacts_spellbooks/exploration.json',{'unchained_pages':pages})
