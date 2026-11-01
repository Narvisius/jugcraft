"""Reconcile this intake with existing native accessories, without applying incompatible overlays."""
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
NEW=ROOT/'art/owner-library/originals/Edited-2026-10-29/artifacts'
OLD=ROOT/'art/owner-library/originals/Oct23/Jimtafacts'
def main():
 records=[];same=0;overlays=[];cache=[];different=[]
 for f in sorted(NEW.rglob('*')):
  if not f.is_file():continue
  relative=f.relative_to(NEW);previous=OLD/relative
  digest=hashlib.sha256(f.read_bytes()).hexdigest()
  equal=previous.exists() and hashlib.sha256(previous.read_bytes()).hexdigest()==digest
  if equal:same+=1
  elif relative.parts[0]=='.cache':cache.append(relative.as_posix())
  elif relative.parts[0].startswith('overlay_'):overlays.append(relative.as_posix())
  else:different.append(relative.as_posix())
  records.append({'path':relative.as_posix(),'sha256':digest,'sameAsExistingSource':equal})
 report={'files':records,'identicalToOct23Source':same,'generatorCacheOnly':cache,'conditionalOverlays':overlays,'remainingGameplayDifferences':different,
  'runtime':'Existing Jimtafacts accessory behavior and five recovered wearable mesh families retained.',
  'overlayDecisions':{'disable_accessories_curios_trinkets':'Not activated: disables equipment and contradicts owner direction.',
   'remove_slot_restrictions':'Existing common TrinketCallback permits wearable accessories; item tags preserve the supported visible slots.',
   'face_slot':'Optional source alternative; existing head/hat slot supports these unchanged items and their original face meshes.',
   'durability_enchantments':'Not activated: source default and existing native accessories have no durability; enabling an unused durability tag would not add a mechanic.'},
  'validation':'Source reconciliation only; combined gameplay validation pending.'}
 output=ROOT/'art/owner-library/catalog/edited-2026-10-29/artifacts-reconciliation.json';output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
 print('Artifacts reconciliation:',same,'identical;',len(overlays),'conditional overlays;',len(cache),'cache files;',len(different),'unresolved gameplay differences.')
if __name__=='__main__':main()
