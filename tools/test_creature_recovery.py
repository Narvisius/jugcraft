"""Data integrity and original-pixel checks for the October 19 recovery."""
import hashlib
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/jugcraft'
MODELS=ASSETS/'recovered_creatures'

class CreatureRecoveryTests(unittest.TestCase):
    def test_unknown_locals_never_reuse_legacy_parameter_aliases(self):
        from recover_creature_animation import runtime_locals
        result=runtime_locals('float f3 = entity.unsupportedHairPhysics(); float hairZ = Maths.rad(180.0f - f3 / 2.0f);',{'f3':'yaw'})
        self.assertNotIn('f3',result)
        self.assertNotIn('hairZ',result)
    def test_all_rotten_sound_events_resolve_supplied_ogg_files(self):
        sounds=json.loads((ASSETS/'sounds.json').read_text());checked=0
        for key,value in sounds.items():
            if not key.startswith('rotten.'):continue
            for sound in value['sounds']:
                name=sound if isinstance(sound,str) else sound['name']
                path=ASSETS/'sounds'/(name.removeprefix('jugcraft:')+'.ogg')
                self.assertTrue(path.is_file(),name)
                self.assertEqual(b'OggS',path.read_bytes()[:4],name);checked+=1
        self.assertEqual(48,checked)
    def test_rotten_items_have_atlas_sources_and_current_egg_models(self):
        sources=json.loads((ROOT/'src/main/resources/assets/minecraft/atlases/blocks.json').read_text())['sources']
        for folder in ('block','item'):
            self.assertTrue(any(s.get('source')=='menagerie/rotten/'+folder for s in sources))
        eggs=list((ASSETS/'items').glob('rotten_*_spawn_egg.json'))
        self.assertEqual(13,len(eggs))
        for egg in eggs:
            self.assertIn(json.loads(egg.read_text())['model']['model'],['minecraft:item/'+name+'_spawn_egg' for name in ('zombie','skeleton','endermite','wolf')])
    def test_every_binding_resolves_original_geometry_and_texture(self):
        bindings=json.loads((MODELS/'bindings.json').read_text())
        self.assertEqual(167,len(bindings))
        for ident,binding in bindings.items():
            for variant in binding['variants'].values():
                for age in variant.values():
                    self.assertTrue((MODELS/(age['model'].removeprefix('recovered/')+'.json')).is_file(),ident)
                    if age['skin'].startswith('minecraft:'):
                        self.assertEqual('mutant_creeper_minion',ident)
                        self.assertEqual('minecraft:entity/creeper/creeper',age['skin'])
                    else:self.assertTrue((ASSETS/'textures'/(age['skin']+'.png')).is_file(),ident)

    def test_hierarchies_and_clips_have_no_dangling_bones(self):
        for path in MODELS.glob('*.json'):
            if path.stem=='bindings':continue
            d=json.loads(path.read_text());seen={'$mesh'}
            for part in d['parts']:
                self.assertIn(part['parent'],seen,path.name+': '+part['name'])
                self.assertNotIn(part['name'],seen,path.name)
                seen.add(part['name'])
            for action,clip in d['actions'].items():
                self.assertGreater(clip['length'],0,path.name+': '+action)
                for channel in clip['channels']:
                    self.assertIn(channel['bone'],seen,path.name)
                    times=[f['time'] for f in channel['frames']]
                    self.assertEqual(sorted(times),times,path.name)
                    self.assertLessEqual(max(times),clip['length']+.00001,path.name)

    def test_runtime_texture_pixels_are_supplied_files(self):
        report=json.loads((ROOT/'art/owner-library/catalog/creature-recovery-2026-10-19/bindings-report.json').read_text())
        manifest=json.loads((ROOT/'art/owner-library/catalog/creature-recovery-2026-10-19/source-manifest.json').read_text())
        originals={(row['collection'],row['path']):row['sha256'] for row in manifest}
        self.assertEqual([],report['issues'])
        for pair in report['textureCopies']:
            runtime=ASSETS/'textures'/(pair['runtime']+'.png')
            self.assertEqual(originals[(pair['collection'],pair['sourcePath'])],pair['sha256'])
            self.assertEqual(pair['sha256'],hashlib.sha256(runtime.read_bytes()).hexdigest(),pair['runtime'])

    def test_transparent_overlays_keep_a_complete_base_skin(self):
        bindings=json.loads((MODELS/'bindings.json').read_text())
        for species,variant,base in [('beast_crocodile','crocodile_crown','crocodile_0'),
                                     ('beast_grizzly_bear','grizzly_bear_honey','grizzly_bear'),
                                     ('beast_grizzly_bear','grizzly_bear_snowy','grizzly_bear'),
                                     ('beast_moose','moose_snowy_antlered','moose_antlered'),
                                     ('beast_seal','seal_crying','seal_brown_0'),
                                     ('beast_raccoon','raccoon_bandana','raccoon'),
                                     ('cavern_nucleeper','nucleeper_glass','nucleeper'),
                                     ('cavern_caramel_cube','caramel_cube_large_outside','caramel_cube_large')]:
            for age in ('adult','baby'):
                pair=bindings[species]['variants'][variant][age]
                self.assertTrue(pair['skin'].endswith('/'+base),(species,pair['skin']))
                self.assertTrue(pair['layers'],species)
                for layer in pair['layers']:
                    self.assertTrue((ASSETS/'textures'/(layer['skin']+'.png')).is_file(),layer)

    def test_array_initializers_and_else_if_keep_correct_conditions(self):
        from recover_creature_animation import scopes,guard_at
        text='AdvancedModelBox[] a = new AdvancedModelBox[]{this.tail};\nthis.walk();\n'
        self.assertEqual(1,guard_at(scopes(text),text.index('this.walk')))
        text='if (baby) {\n A();\n} else if (swim) {\n B();\n} else {\n C();\n}\n'
        guards=scopes(text)
        self.assertIn("['||', 'baby', 'swim']",str(guard_at(guards,text.index('C();'))))

    def test_source_conditions_use_resolved_runtime_locals(self):
        from recover_creature_animation import scopes,guard_at,runtime_locals
        text='float runProgress = entity.prevChargeProgress + (entity.chargeProgress - entity.prevChargeProgress) * partialTick;\nif (runProgress > 0.0f) {\n A();\n} else {\n B();\n}\n'
        variables=runtime_locals(text,{})
        self.assertIn('charge',str(variables['runProgress']))
        self.assertIn('charge',str(guard_at(scopes(text,variables),text.index('A();'))))
        self.assertIn("'!'",str(guard_at(scopes(text,variables),text.index('B();'))))

    def test_walk_and_run_blends_resolve_source_leap_and_glide_getters(self):
        from recover_creature_animation import runtime_locals
        setup='float runProgress = entity.getRunAmount(partialTick); float leapProgress = entity.getLeapProgress(partialTick); float walkAmount = limbSwingAmount * (1.0f - runProgress) * (1.0f - leapProgress); float glideProgress = entity.prevGlideProgress + (entity.glideProgress - entity.prevGlideProgress) * partialTick; float walkSwingAmount = (1.0f - glideProgress * 0.2f) * limbSwingAmount;'
        values=runtime_locals(setup,{'limbSwingAmount':'move'})
        self.assertIn('jump',str(values['walkAmount']))
        self.assertIn('run',str(values['walkAmount']))
        self.assertIn('fly',str(values['walkSwingAmount']))

if __name__=='__main__':unittest.main()
