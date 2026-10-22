import gzip
import hashlib
import struct
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import audit_candy_chunks as audit

class CandyChunkAuditTests(unittest.TestCase):
    def root(self):
        return {'sections':(9,(10,[{'biomes':(10,{'palette':(9,(8,[audit.CANDY]))})}])),
                'InhabitedTime':(4,1200),'block_entities':(9,(10,[{'id':(8,'minecraft:chest')}])),
                'Status':(8,'minecraft:full')}

    def test_reports_biome_and_review_flags_without_world_mutation(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'r.-1.2.mca'
            blob=audit.nouveau_nbt.write((10,'',self.root()))
            header=bytearray(8192);header[0:4]=(2*256+1).to_bytes(4,'big')
            data=struct.pack('>I',len(blob)+1)+b'\x01'+blob
            path.write_bytes(header+data+b'\0'*(4096-len(data)))
            before=hashlib.sha256(path.read_bytes()).hexdigest()
            records,errors=audit.scan_region(path)
            self.assertEqual(errors,[]);self.assertEqual(len(records),1)
            self.assertEqual((records[0]['chunkX'],records[0]['chunkZ']),(-32,64))
            self.assertEqual(records[0]['blockEntities'],['minecraft:chest'])
            self.assertFalse(records[0]['automaticResetAllowed'])
            self.assertEqual(before,hashlib.sha256(path.read_bytes()).hexdigest())

    def test_untouched_time_is_not_permission_and_other_biomes_are_ignored(self):
        root=self.root();root['InhabitedTime']=(4,0);root['block_entities']=(9,(10,[]))
        self.assertFalse(audit.inspect_chunk(root)['automaticResetAllowed'])
        root['sections']=(9,(10,[{'biomes':(10,{'palette':(9,(8,['minecraft:plains']))})}]))
        self.assertIsNone(audit.inspect_chunk(root))

    def test_invalid_region_and_unknown_compression_fail_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'r.0.0.mca';header=bytearray(8192);header[0:4]=(10*256+1).to_bytes(4,'big');path.write_bytes(header)
            records,errors=audit.scan_region(path);self.assertEqual(records,[]);self.assertEqual(len(errors),1)
        with self.assertRaises(ValueError):audit.unpack(b'',4)
        with self.assertRaises(ValueError):audit.unpack(b'broken',2)

if __name__=='__main__':unittest.main()
