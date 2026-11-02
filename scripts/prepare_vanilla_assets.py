"""Fetch the pinned vanilla client resource archive for generation and asset tests.
Never execute it or redistribute it with the repository. Verify Mojang's fixed hash.
"""
import hashlib
from pathlib import Path
from urllib.request import urlopen
URL = 'https://piston-data.mojang.com/v1/objects/e877b6a07acd633fb3bb475002175cec036e7b87/client.jar'
SHA1 = 'e877b6a07acd633fb3bb475002175cec036e7b87'
TARGET = Path.home()/'.gradle/caches/fabric-loom/26.3/minecraft-client.jar'

def prepare():
    if TARGET.is_file() and hashlib.sha1(TARGET.read_bytes()).hexdigest() == SHA1:
        print('Pinned vanilla asset archive verified.'); return
    with urlopen(URL, timeout=60) as response: data=response.read()
    if len(data) != 41483720 or hashlib.sha1(data).hexdigest() != SHA1:
        raise ValueError('Pinned vanilla asset archive checksum/size mismatch')
    TARGET.parent.mkdir(parents=True,exist_ok=True)
    temporary=TARGET.with_suffix('.download')
    temporary.write_bytes(data)
    temporary.replace(TARGET)
    print('Pinned vanilla asset archive downloaded and verified.')

if __name__ == '__main__': prepare()
