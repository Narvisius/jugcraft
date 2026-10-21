"""Check the actual distributable, including client config and mixin classes.

Usage: python tools/check_mixin_jar.py build/libs/jugcraft-<version>.jar
This verifies packaging, not Minecraft startup or mixin application.
"""
import json
import sys
from zipfile import ZipFile


def check(path):
    with ZipFile(path) as jar:
        manifest = json.loads(jar.read('fabric.mod.json'))
        configs = manifest.get('mixins', [])
        assert configs, 'No mixins declared'
        count = 0
        for declaration in configs:
            name = declaration if isinstance(declaration, str) else declaration['config']
            config = json.loads(jar.read(name))
            prefix = config['package'].replace('.', '/') + '/'
            for side in ('mixins', 'client', 'server'):
                for mixin in config.get(side, []):
                    jar.getinfo(prefix + mixin.replace('.', '/') + '.class')
                    count += 1
        print(f'PASS: {len(configs)} declared mixin configs and {count} classes in {path}')


if __name__ == '__main__':
    check(sys.argv[1])
