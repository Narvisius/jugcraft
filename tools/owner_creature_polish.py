"""Refresh only reconstructed creature geometry/UVs, preserving registry IDs and supplied rigs."""
from copy import deepcopy
from menagerie_content import ASSET, RES, ROOT, read, write, animal_geometry
from wildlife_geometry import apply_skin, refine_shape

def main():
    import wildlife_geometry, beasts_content, mutations_content
    wildlife_geometry.main()
    recovered_path=ASSET/'wildlife_models/bindings.json'
    recovered=read(recovered_path) if recovered_path.exists() else {}
    count=sum(s['id'].startswith('wildlife_') and s['id'] not in recovered
              for s in read(RES/'jugcraft/menagerie/catalog.json')['species'])
    for group in ('beast','cavern','landscape','mutant','ecology'):
        path=RES/f'jugcraft/menagerie/{group}.json';catalog=read(path)
        for s in catalog['species']:
            count+=1
            if group=='mutant':mutations_content.rig(s)
            elif group!='ecology':beasts_content.sculpt(s)
            else:
                animal_geometry(s);base=read(ASSET/f"geckolib/models/entity/menagerie/{s['id']}.geo.json");refine_shape(base,s)
                from owner_creature_refinements import refine
                refine(base,s)
                from wildlife_geometry import refine_animations
                ap=ASSET/f"geckolib/animations/entity/menagerie/{s['model']}.animation.json"
                animation=read(ap);refine_animations(animation,base);write(ap,animation)
                for v in s['variants']:
                    for field,skin in (('model','texture'),('babyModel','babyTexture')):
                        write(ASSET/f"geckolib/models/entity/menagerie/{v[field]}.geo.json",apply_skin(deepcopy(base),v[skin]))
        write(path,catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/creature-polish.json',{'reconstructedSpecies':count,'sourceBitmapsEdited':False,'approach':'Per-face aspect-matched opaque atlas panels preserve larger markings; explicit face maps remain authoritative. Native long-neck, reptile, fish/fluke, ray, tusk and shell refinements. Supplied rigs are not regenerated.','acceptance':'Native interpretations still require owner visual acceptance; missing original source geometry is not recovered.'})
    print('Updated reconstructed species:',count)

if __name__=='__main__':main()
