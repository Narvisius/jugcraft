"""The original upgrade orbs and six special armor meshes/textures."""
import json
TYPES=('fire','ice','lightning','holy','ender','blood','evocation','nature','mana','cooldown','protection')
ARMOR={
 'wizard_hat':('wizard_armor_hat','wizard','helmet'),
 'paladin_chestplate':('paladin_chestplate','paladin_chestplate','chestplate'),
 'speed_boots':('boots_of_speed','boots_of_speed','boots'),
 'infernal_sorcerer_chestplate':('infernal_sorcerer','infernal_sorcerer','chestplate'),
 'tarnished_helmet':('tarnished_armor','tarnished','helmet'),
 'gold_crown':('tarnished_armor','gold_crown','helmet')}
def items():return ['jimta_spellbooks_'+n for n in ('upgrade_orb',*[t+'_upgrade_orb' for t in TYPES],*ARMOR)]
def generate(copy,definition,texture,dump,RES,ART,lang):
 texts=json.loads((ART/'lang/en_us.json').read_text(encoding='utf-8'))
 for n in ('upgrade_orb',*[t+'_upgrade_orb' for t in TYPES],*ARMOR):
  dump(RES/f'assets/jugcraft/items/jimta_spellbooks_{n}.json',definition('irons_spellbooks:item/'+n,'jimta_spellbooks_'+n))
  lang['item.jugcraft.jimta_spellbooks_'+n]=texts.get('item.irons_spellbooks.'+n,n.replace('_',' ').title())
 for n,(geo,tex,slot) in ARMOR.items():
  copy(ART/f'geo/{geo}.geo.json',RES/f'assets/jugcraft/geckolib/models/armor/jimtafacts/{geo}.geo.json')
  texture('irons_spellbooks:models/armor/'+tex)
  dump(RES/f'data/jugcraft/spell_assignments/jimta_spellbooks_{n}.json',{'access':'NONE','access_param':''})
 dump(RES/'jugcraft/jimtafacts_spellbooks/special_armor.json',{n:{'model':g,'texture':t,'slot':s} for n,(g,t,s) in ARMOR.items()})

 # Source immolate status artwork and source blood-production exclusion tag.
 texture('irons_spellbooks:mob_effect/immolate')
 copy(ART/'textures/mob_effect/immolate.png',RES/'assets/jugcraft/textures/mob_effect/jimtafacts_immolate.png')
 lang['effect.jugcraft.jimtafacts_immolate']='Immolate'
 copy(ART.parents[1]/'data/irons_spellbooks/tags/entity_type/cant_produce_blood.json',RES/'data/jugcraft/tags/entity_type/jimtafacts/cant_produce_blood.json')
