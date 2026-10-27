"""Copy supplied read-only Lua ROM resources; never execute them during generation."""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/computers/data/computercraft/lua/rom'
TARGET=ROOT/'src/main/resources/jugcraft/jimtafacts/computers'

def generate():
    manifest=[];names=[]
    for source in sorted(SOURCE.rglob('*')):
        if not source.is_file():continue
        relative=source.relative_to(SOURCE);name=relative.as_posix()
        if source.stat().st_size>131072:raise ValueError(f'ROM resource exceeds bounded loader: {name}')
        target=TARGET/'rom'/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
        names.append(name);manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'None; original SPDX/author notices retained. Loaded only through the isolated Lua runtime.'})
    (TARGET/'rom-index.txt').write_text('\n'.join(names)+'\n',encoding='utf-8')
    (ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/computer-rom-runtime-copies.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':generate()
