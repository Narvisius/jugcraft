import hashlib,json,sys,tempfile,unittest,zipfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import package_updater_recovery as recovery

class RecoveryTests(unittest.TestCase):
    def fixture(self,root):
        def row(name,data):return {'path':'mods/'+name,'sha512':hashlib.sha512(data).hexdigest(),'size':len(data)}
        old={'schemaVersion':1,'releaseVersion':'old.1','minecraft':'26.3','fabricLoader':'0.19.5','java':25,'files':[row('jugcraft.jar',b'old'),row('library.jar',b'library')]}
        new={**old,'releaseVersion':'new.1','files':[row('jugcraft.jar',b'new'),row('library.jar',b'library'),row('added.jar',b'added')]}
        source=root/'pack.mrpack';cache=root/'cache';cache.mkdir();(cache/'added.jar').write_bytes(b'added')
        with zipfile.ZipFile(source,'w') as z:
            z.writestr('client-overrides/JUGCRAFT-UPDATE.json',json.dumps(new));z.writestr('overrides/mods/jugcraft.jar',b'new');z.writestr('overrides/options.txt',b'do not copy')
        return source,cache,old,new
    def test_plain_recovery_reuses_instance_and_preserves_personal_files(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);source,cache,old,new=self.fixture(root);out=root/'recovery.zip'
            result=recovery.build(source,cache,[old],out);self.assertEqual(result['changed'],2);self.assertEqual(result['removed'],[])
            instance=root/'existing';personal=['options.txt','config/personal.json','saves/home/level.dat','mods/personal.jar','resourcepacks/own.zip','shaderpacks/own.zip','servers.dat']
            for name in personal+['mods/jugcraft.jar','mods/library.jar']:
                p=instance/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(b'keep' if name in personal else b'old' if name.endswith('jugcraft.jar') else b'library')
            with zipfile.ZipFile(out) as z:
                for name in z.namelist():
                    if name.startswith('COPY-INTO-JUGCRAFT/'):
                        p=instance/name.removeprefix('COPY-INTO-JUGCRAFT/');p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(name))
            for name in personal:self.assertEqual((instance/name).read_bytes(),b'keep')
            installed=json.loads((instance/'JUGCRAFT-UPDATE.json').read_text());self.assertEqual(installed,new)
            for row in installed['files']:self.assertEqual(hashlib.sha512((instance/row['path']).read_bytes()).hexdigest(),row['sha512'])
            with self.assertRaises(ValueError):recovery.build(source,cache,[old],out)
    def test_corrupt_payload_rejected_before_archive_is_written(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);source,cache,old,new=self.fixture(root);(cache/'added.jar').write_bytes(b'bad');out=root/'recovery.zip'
            with self.assertRaises(ValueError):recovery.build(source,cache,[old],out)
            self.assertFalse(out.exists())
    def test_platform_change_rejected(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);source,cache,old,new=self.fixture(root);old['minecraft']='26.2'
            with self.assertRaises(ValueError):recovery.build(source,cache,[old],root/'out.zip')
