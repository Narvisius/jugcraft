"""Native guide/Chronicle adaptation; original book pixels, no legacy supporter service."""
import json,hashlib,shutil
from jimtafacts_factory import ROOT,RES,dump
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/assets/irons_spellbooks'
CHAPTERS=[
 ('First discoveries','Explore magic structures for scrolls, essence and equipment. Their resident mages are dangerous. A scroll carries one working; a spellbook keeps inscribed workings for repeated use. Jugcraft uses shared Focus, not a separate mana currency. Equipment you own can be used without a research permission gate.'),
 ('Books and inscription','Craft an Inscription Table and an early spellbook. Place a book and a scroll in its input slots, choose the inscription mode, then take the result. Capacity varies by book. Use the spell selector to choose an inscribed working. The shared spell UI shows its Focus cost and cooldown.'),
 ('Ink and scrolls','The scribing and ink recipes are available in the recipe browser. Start with gathered essence and early ink, then follow the recipes for higher tiers. The native port uses level-one workings balanced around shared Focus; legacy guide spell-level and mana figures are not Jugcraft rules.'),
 ('Armor and casting gear','Wandering Magician armor starts the wearable chain. Later armor and casting implements support particular schools. Compare the actual item tooltips and recipe-browser inputs before upgrading: retained source names do not imply a second progression or power system.'),
 ('Schools','Workings are grouped by school in the spell selector. Choose a practical combination of combat, movement, protection and utility. Shared claims, town rules and PvP settings still apply to magic. Research discoveries add goals and information; they do not prevent you using an item you already possess.'),
 ('Exploration rewards','Archevokers carry their logbooks. Translate an untranslated one at an enchanting table with an Ancient Knowledge Fragment in your other hand. Ancient maps point toward exploration rewards. The Dead King and Ice Spider encounters supply their own rare rewards; these are not ordinary crafting outputs.'),
 ('Advanced workshops','Use shared Jugcraft processing machines and the recipe browser to follow material chains. The Encyclopedia contains digital-network instructions. The counterflow Heat Exchanger uses a start, one to three middles and an end, all facing the same way. Feed hot coolant at the start and water at the end; bottom ports remove cooled coolant and steam.'),
 ('The personal Chronicle','Craft the Chronicle from a book, gold and an arcane ingot. Opening it refreshes your discovery checklist from your inventory and recorded acquisition, crafting and use statistics. It neither grants rewards nor locks equipment. It works offline and does not contact the source mod’s supporter service.'),
 ('Finding the next recipe','Use the recipe browser for exact current ingredients and the Encyclopedia for connections between systems. Some rare books, music, rings and boss materials come from exploration instead of crafting. This handbook adapts the supplied guide to Jugcraft; original guide files remain preserved in the owner library.')
]
MILESTONES=[('arcane_essence','Arcane supplies','Loot magic structures.'),('copper_spell_book','First spellbook','Craft an early spellbook.'),('archevoker_logbook_untranslated','Archevoker logbook','Defeat an Archevoker.'),('archevoker_logbook_translated','Translated notes','Use a fragment at an enchanting table.'),('ancient_knowledge_fragment','Ancient knowledge','Explore magic dungeons.'),('dead_king_phylactery_shard','King’s legacy','Challenge the Dead King.')]
def items():return {'jimta_spellbooks_chronicle','jimta_spellbooks_workshop_guide'}
def generate(shared=True):
 manifest=[]
 for name,sprite in [('chronicle','chronicle'),('workshop_guide','chronicle_old')]:
  source=SOURCE/f'textures/item/{sprite}.png';target=RES/f'assets/jugcraft/textures/item/jimtafacts/{name}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
  manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
  model='jugcraft:item/jimtafacts/'+name;ident='jimta_spellbooks_'+name
  dump(RES/f'assets/jugcraft/models/item/jimtafacts/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/'+name}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':model}})
  ingredients=['minecraft:book','minecraft:gold_ingot','jugcraft:jimta_spellbooks_arcane_ingot'] if name=='chronicle' else ['minecraft:book','minecraft:amethyst_shard']
  dump(RES/f'data/jugcraft/recipe/jimtafacts_guides/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,'result':{'id':'jugcraft:'+ident}})
 source_entries=sorted((SOURCE/'patchouli_books/iss_guide_book/en_us/entries').rglob('*.json'))
 dump(RES/'jugcraft/jimtafacts_spellbooks/guides.json',{'chapters':[{'title':title,'text':text} for title,text in CHAPTERS],'milestones':[{'item':'jugcraft:jimta_spellbooks_'+item,'title':title,'hint':hint} for item,title,hint in MILESTONES]})
 manifest += [{'source':p.relative_to(ROOT).as_posix(),'adaptation':'Topics adapted into native workshop handbook; legacy spell-level/mana and unsupported external service behavior explicitly replaced.'} for p in source_entries]
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/guide-runtime-copies.json',manifest)
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';lang=json.loads(p.read_text(encoding='utf-8'));lang.update({'item.jugcraft.jimta_spellbooks_chronicle':'Workshop Chronicle','item.jugcraft.jimta_spellbooks_workshop_guide':'Workshop Guide','encyclopedia.jugcraft.page.jimta_guides':'Workshop Guides'});dump(p,lang)
 return 2
if __name__=='__main__':print('Generated',generate(),'local guides')
