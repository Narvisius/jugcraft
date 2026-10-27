"""Original consumable and lore assets, native mana-potion brewing and music discs."""
import json
MATERIALS=('frozen_bone','divine_pearl','dragonskin','arcane_rune','cooldown_rune','protection_rune','eldritch_manuscript','frosted_helve','permafrost_shard','energized_core','chained_book','mithril_weave','divine_soulshard','dead_king_phylactery','dead_king_phylactery_shard','disc_fragment_flame_still_burns')
SPECIAL=('netherward_tincture','tincture_of_forgetfulness','archevoker_logbook_translated','archevoker_logbook_untranslated','music_disc_dead_king_lullaby','music_disc_flame_still_burns','music_disc_whispers_of_ice')
POTIONS=('instant_mana_one','instant_mana_two','instant_mana_three','instant_mana_four')
def items():return ['jimta_spellbooks_'+n for n in (*MATERIALS,*SPECIAL,'chronicle','workshop_guide')]
def generate(copy,definition,texture,dump,RES,ART,lang):
 import jimtafacts_guides
 jimtafacts_guides.generate(False)
 lang.update({'item.jugcraft.jimta_spellbooks_chronicle':'Workshop Chronicle','item.jugcraft.jimta_spellbooks_workshop_guide':'Workshop Guide'})
 source=ART.parents[1]/'data/irons_spellbooks';texts=json.loads((ART/'lang/en_us.json').read_text(encoding='utf-8'))
 for n in (*MATERIALS,*SPECIAL):
  dump(RES/f'assets/jugcraft/items/jimta_spellbooks_{n}.json',definition('irons_spellbooks:item/'+n,'jimta_spellbooks_'+n));lang['item.jugcraft.jimta_spellbooks_'+n]=texts.get('item.irons_spellbooks.'+n,n.replace('_',' ').title())
 lang['item.jugcraft.jimta_spellbooks_archevoker_logbook_untranslated']="Untranslated Archevoker's Logbook"
 for name in ('dead_king_lullaby','flame_still_burns','whispers_of_ice'):
  lang['item.jugcraft.jimta_spellbooks_music_disc_'+name]='Music Disc — '+name.replace('_',' ').title()
 for k,v in texts.items():
  if k.startswith('item.irons_spellbooks.archevoker_log.'):lang[k.replace('item.irons_spellbooks.','item.jugcraft.jimta_spellbooks_')]=v
 dump(RES/'jugcraft/jimtafacts_spellbooks/lore.json',{'materials':list(MATERIALS),'special':list(SPECIAL),'potions':list(POTIONS)})
 for i,name in enumerate(POTIONS):
  base='minecraft:awkward' if i==0 else 'jugcraft:jimta_spellbooks_'+POTIONS[i-1]
  reagent=['jugcraft:jimta_spellbooks_arcane_essence','minecraft:glowstone_dust','minecraft:amethyst_shard','minecraft:amethyst_cluster'][i]
  for form in ('potion','splash_potion','lingering_potion'):
   dump(RES/f'data/jugcraft/recipe/jimtafacts/brewing/{form}_{name}.json',{'type':'minecraft:brewing','input':{'item':'minecraft:'+form,'potion_contents':{'potions':base}},'output':{'id':'minecraft:'+form,'components':{'minecraft:potion_contents':{'potion':'jugcraft:jimta_spellbooks_'+name}}},'reagent':{'item':reagent}})
   lang['item.minecraft.'+form+'.effect.jimta_spellbooks_'+name]=('Splash ' if form=='splash_potion' else 'Lingering ' if form=='lingering_potion' else '')+'Potion of Focus '+str(i+1)
  for form,out,reagent in [('potion','splash_potion','gunpowder'),('splash_potion','lingering_potion','dragon_breath')]:
   dump(RES/f'data/jugcraft/recipe/jimtafacts/brewing/{out}_{name}_conversion.json',{'type':'minecraft:brewing','input':{'item':'minecraft:'+form,'potion_contents':{'potions':'jugcraft:jimta_spellbooks_'+name}},'output':{'id':'minecraft:'+out,'components':{'minecraft:potion_contents':{'potion':'jugcraft:jimta_spellbooks_'+name}}},'reagent':{'item':'minecraft:'+reagent}})
 sounds={}
 source_sounds=json.loads((ART/'sounds.json').read_text(encoding='utf-8'))
 for path in sorted((source/'jukebox_song').glob('*.json'),key=lambda p:p.as_posix()):
  data=json.loads(path.read_text(encoding='utf-8'));old=data['sound_event'];old=old['sound_id'] if isinstance(old,dict) else old;key=old.split(':')[1];new='jimtafacts.spellbooks.'+key
  data['sound_event']='jugcraft:'+new;dump(RES/f'data/jugcraft/jukebox_song/jimtafacts/{path.name}',data)
  entry=source_sounds[key];entry=json.loads(json.dumps(entry));
  for i,sound in enumerate(entry['sounds']):
   name=sound if isinstance(sound,str) else sound['name'];name=name.split(':')[-1]
   copy(ART/f'sounds/{name}.ogg',RES/f'assets/jugcraft/sounds/jimtafacts/spellbooks/{name}.ogg');mapped='jugcraft:jimtafacts/spellbooks/'+name
   if isinstance(sound,str):entry['sounds'][i]=mapped
   else:sound['name']=mapped
  sounds[new]=entry
 dump(RES/'jugcraft/jimtafacts_spellbooks/music_sounds.json',sounds)
 path=RES/'assets/jugcraft/sounds.json';all_sounds=json.loads(path.read_text(encoding='utf-8'));all_sounds.update(sounds);dump(path,all_sounds)
 lang['effect.jugcraft.jimtafacts_instant_mana']='Restore Focus'
