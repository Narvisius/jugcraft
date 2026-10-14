"""Factory expansion links into the shared encyclopedia, without a second protocol."""
import json, sys, unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import encyclopedia_routes
import industrial_forms

class EncyclopediaCatalogTests(unittest.TestCase):
    def setUp(self):
        assets = ROOT / 'src/main/resources/assets/jugcraft'
        self.book = json.loads((assets / 'encyclopedia/en_us.json').read_text(encoding='utf-8'))
        self.handbook = json.loads((assets / 'handbook/en_us.json').read_text(encoding='utf-8'))

    def test_catalog_is_generated_from_current_shared_guides(self):
        self.assertEqual(self.book, encyclopedia_routes.build(self.handbook))
        self.assertEqual([r['id'] for r in self.book['routes']], ['technology', 'magic'])

    def test_every_technology_link_resolves_to_a_real_page(self):
        route = self.book['routes'][0]
        for entry in route['chapters'] + route['milestones']:
            link = entry['link']
            self.assertEqual(link['type'], 'handbook')
            page = self.handbook['chapters'][link['chapter']]['pages'][link['page']]
            self.assertTrue(page['title'])
            self.assertTrue(entry['icon'])

    def test_every_expanded_machine_is_discoverable(self):
        pages = [p for c in self.handbook['chapters'] for p in c['pages']]
        icons = {p.get('icon') for p in pages}
        for name in industrial_forms.FORMS:
            self.assertIn('jugcraft:' + name, icons, name)

    def test_retained_acid_is_explicit_in_process_guide(self):
        page = next(p for c in self.handbook['chapters'] for p in c['pages']
                    if p.get('icon') == 'jugcraft:chemical_reactor')
        text = ' '.join(page['text'])
        self.assertIn('1000 mB charge is retained', text)
        self.assertIn('including in the dropped reactor', text)

if __name__ == '__main__':
    unittest.main()
