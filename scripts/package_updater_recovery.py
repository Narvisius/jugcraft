"""Build a plain, offline recovery ZIP for specified released updater inventories.

No executable installer and no profile writes. Only changed managed JARs and the
matching inventory are included; player settings, saves and extra mods stay put.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile
import package_modrinth as pack

def rows(manifest):
    pack.require(manifest.get('schemaVersion')==1,'Unsupported inventory')
    result={}
    for row in manifest['files']:
        name=row['path']
        pack.require(bool(re.fullmatch(r'mods/[A-Za-z0-9][A-Za-z0-9._+\-]*\.jar',name)),'Unsafe managed path')
        pack.require(name.lower() not in {p.lower() for p in result},'Duplicate managed path')
        pack.require(bool(re.fullmatch('[0-9a-f]{128}',row['sha512'])),'Invalid digest')
        result[name]=row
    return result

def build(mrpack,cache,previous,output):
    pack.require(not output.exists(),'Never replace a published recovery archive')
    pack.require(bool(previous),'Specify supported prior releases')
    with zipfile.ZipFile(mrpack) as source:
        encoded=source.read('client-overrides/JUGCRAFT-UPDATE.json');current=json.loads(encoded);new=rows(current)
        changed=set();removed=set();versions=[]
        for manifest in previous:
            old=rows(manifest);versions.append(manifest['releaseVersion'])
            for pin in ('minecraft','fabricLoader','java'):
                pack.require(manifest[pin]==current[pin],'Recovery cannot change platform')
            changed.update(name for name,row in new.items() if old.get(name,{}).get('sha512')!=row['sha512'])
            removed.update(old.keys()-new.keys())
        contents={}
        for name in sorted(changed):
            data=source.read('overrides/mods/jugcraft.jar') if name=='mods/jugcraft.jar' else (cache/Path(name).name).read_bytes()
            pack.require(len(data)==new[name]['size'] and hashlib.sha512(data).hexdigest()==new[name]['sha512'],'Corrupt recovery payload: '+name)
            contents[name]=data
    instructions=f'''JUGCRAFT updater recovery to {current['releaseVersion']}
For existing instances of: {', '.join(versions)}

This is a plain ZIP of replacement mod files, not an installer. Do NOT import it
as a new Modrinth profile. Your original instance and settings stay in place.

1. Close Minecraft completely. Wait one minute for any updater to finish.
2. In Modrinth, open your existing JUGCRAFT instance's folder.
3. If .jugcraft-game-update/journal.properties exists, STOP: there is an unfinished
   transaction requiring recovery. Keep the folder intact and contact support.
   This ZIP handles the reported 'no files changed' lock failure, not partial installs.
4. Copy the CONTENTS of COPY-INTO-JUGCRAFT into that instance folder. Merge the
   mods folder and replace the listed files. Do NOT delete/replace the whole mods
   folder, or copy into Modrinth's global folder. Keep all other files.
5. Remove only the obsolete managed files listed in REMOVE-OLD-MANAGED-FILES.txt
   (normally none). In .jugcraft-game-update, delete plan.properties, ready.txt and
   result.txt if present, ONLY after step 3 confirmed there is no journal.
   Keep backup/, stage/, helper.log, game.lock and helper.lock.
6. Launch the SAME JUGCRAFT profile. Future updates use the corrected helper.

Included: {len(changed)} verified managed JARs and JUGCRAFT-UPDATE.json.
No options, keybinds, configs, saves, screenshots, resource packs, shader packs,
servers.dat or extra mods are included or removed. No extra profile is created.
The inventory is essential: replacing only jugcraft.jar leaves the old recorded hash.
'''
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED) as z:
        for name,data in contents.items():pack.add_entry(z,'COPY-INTO-JUGCRAFT/'+name,data)
        pack.add_entry(z,'COPY-INTO-JUGCRAFT/JUGCRAFT-UPDATE.json',encoded)
        pack.add_entry(z,'READ-ME-FIRST.txt',instructions.encode())
        pack.add_entry(z,'REMOVE-OLD-MANAGED-FILES.txt',('\n'.join(sorted(removed))+'\n' if removed else 'None. Do not delete any mod files.\n').encode())
        pack.add_entry(z,'RECOVERY-MANIFEST.json',pack.json_bytes({'releaseVersion':current['releaseVersion'],'supportedPrevious':versions,'files':[new[n] for n in sorted(changed)],'remove':sorted(removed)}))
    return {'changed':len(changed),'removed':sorted(removed),'supportedPrevious':versions}

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--pack',type=Path,required=True);p.add_argument('--cache',type=Path,required=True);p.add_argument('--previous',type=Path,action='append',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    print(build(a.pack,a.cache,[json.loads(v.read_text()) for v in a.previous],a.output))
