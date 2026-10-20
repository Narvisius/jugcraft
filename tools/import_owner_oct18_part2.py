"""Preserve and inventory the owner's second batch. Never execute supplied content."""
from pathlib import Path
from collections import Counter
import hashlib,json,shutil

ROOT=Path(__file__).resolve().parents[1]
SOURCE=Path(r'C:/Users/james/Downloads/Magic Mods/Jimbos Heavily Edited Mods 10-18-2026/jimbos_heavily modified mods part 2')
DEST=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2'
CATALOG=ROOT/'art/owner-library/catalog/edited-2026-10-18-part2'

def main():
    files=[];groups={}
    for p in sorted(SOURCE.rglob('*')):
        if not p.is_file():continue
        rel=p.relative_to(SOURCE);data=p.read_bytes();sha=hashlib.sha256(data).hexdigest();target=DEST/rel
        if target.exists():
            if hashlib.sha256(target.read_bytes()).hexdigest()!=sha:raise RuntimeError('Preserved original differs: '+str(rel))
        else:target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
        if hashlib.sha256(target.read_bytes()).hexdigest()!=sha:raise RuntimeError('Copy failed: '+str(rel))
        files.append({'path':rel.as_posix(),'bytes':len(data),'sha256':sha})
        group=groups.setdefault(rel.parts[0],{'files':0,'bytes':0,'extensions':Counter(),'runtimeStatus':'pending implementation'})
        group['files']+=1;group['bytes']+=len(data);group['extensions'][p.suffix]+=1
    CATALOG.mkdir(parents=True,exist_ok=True)
    (CATALOG/'files.json').write_text(json.dumps({'ownerDeclaration':'Owner identifies this batch as their heavily edited work; legacy tags are retained from development and testing. Owner authorizes native Jugcraft implementation.','source':str(SOURCE),'files':files},indent=2)+'\n',encoding='utf-8')
    (CATALOG/'audit.json').write_text(json.dumps({'files':len(files),'bytes':sum(x['bytes'] for x in files),'collections':groups,'compiledClassesAreReferenceOnly':True,'sourceExecuted':False,'runtimeRegistrationIsSeparateFromIntake':True},indent=2)+'\n',encoding='utf-8')
    print('Preserved and SHA-256 verified',len(files),'files in',len(groups),'collections.')
    for name,v in groups.items():print(name,v['files'],v['bytes'])

if __name__=='__main__':main()
