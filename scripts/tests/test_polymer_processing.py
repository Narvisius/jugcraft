"""Budget and material-grade boundaries of the connected polymer factory."""
from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
import polymers as p
import polymer_models as models

class PolymerTests(unittest.TestCase):
    def test_recycling_is_lossy_and_cannot_cross_grades(self):
        for recipe in p.RECIPES['polymer_reclaimer']:
            feed,n=recipe['items'][0];product,m=recipe['results'][0]
            self.assertLess(m,n)
            grade=feed.split(':')[1].split('_')[0]
            self.assertEqual(product,{'hdpe':'jugcraft:hdpe_pellets','ldpe':'jugcraft:ldpe_pellets','pvc':'jugcraft:pvc_resin','rubber':'jugcraft:rubber'}[grade])
            self.assertGreater(recipe['energy_per_tick']*recipe['ticks'],0)
    def test_extrusion_budget_counts_offcuts_once(self):
        for recipe in p.RECIPES['polymer_extruder']:
            feed,n=recipe['items'][0];product,m=recipe['results'][0];scrap,q=recipe['results'][1]
            product_units=3 if product=='jugcraft:flexible_hose' else 1
            self.assertEqual(n,m*product_units+q)
            self.assertTrue(recipe['tool'].endswith('_extrusion_die'))
    def test_polyethylene_requires_distinct_retained_inserts(self):
        recipes=[r for r in p.RECIPES['polymerization_kettle'] if r['name'].startswith('polymerize_')]
        self.assertEqual(len(recipes),2)
        self.assertNotEqual(recipes[0]['tool'],recipes[1]['tool'])
        for r in recipes:self.assertEqual(r['fluids'],[('jugcraft:ethylene',1000)])
    def test_service_pipes_terminate_inside_the_process_body(self):
        for name,factory in models.FACTORIES.items():
            body=factory()
            for points in models.PIPE_PATHS[name].values():
                for a,b in zip(points,points[1:]):self.assertEqual(sum(x!=y for x,y in zip(a,b)),1)
                self.assertTrue(any(all(e[0][i]<=points[-1][i]<=e[1][i] for i in range(3)) for e in body),(name,points[-1]))

if __name__=='__main__':unittest.main()
