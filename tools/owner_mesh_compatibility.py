"""Bound runtime UVs and separate depth-conflicting faces without editing source art.

Legacy reversed boxes describe vessel interiors. Keep those as supplied; do not
sort their bounds or seal their mouths. Their separate visual review remains open.
"""
import copy
from menagerie_content import ASSET, ROOT, read, write
from model_writer import fit_uvs, finish_elements


def main():
    changed=[]
    for scope in ('household','menagerie/ecology','menagerie/mutant','menagerie/beast','menagerie/landscape','menagerie/cavern'):
        for p in (ASSET/'models'/scope).rglob('*.json'):
            before=read(p)
            if not before.get('elements'):continue
            after=copy.deepcopy(before);elements=after['elements']
            fit_uvs(elements)
            if all(all(e['from'][i]<=e['to'][i] for i in range(3)) for e in elements):
                finish_elements(elements)
            if after!=before:write(p,after);changed.append(p.relative_to(ASSET).as_posix())
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/mesh-compatibility.json',
          {'changed':changed,'changes':'Runtime-only UV bounds and 0.1-pixel separation of conflicting surfaces. Original images/models retained. Reversed hollow interiors remain for rendered review.'})
    print('Repaired runtime meshes:',len(changed))


if __name__=='__main__':main()
