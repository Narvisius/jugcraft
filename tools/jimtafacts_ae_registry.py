"""Explicit source registry coverage; an asset's presence never makes its gameplay implemented."""
import json


def generate(source, root, functional):
    functional = set(functional)
    reused = {'silicon': ('jugcraft:silicon', 'Shared Jugcraft silicon used by processors; no duplicate material.'),
              'cable_bus': ('jugcraft:jimta_fluix_glass_cable', 'Source internal multipart host maps to registered native colored cable blocks with saved covers/anchors. Full arbitrary multipart coexistence is not claimed.')}
    limited = {
        'matter_cannon': 'Native one-type 64-round magazine, supplied ammo weights, 255-block loaded ray, four-block penetration bound. Real paint/lumen rounds paint permitted surfaces. Source projectile/upgrades are bounded native adaptations.',
        'color_applicator': 'Original colored model/palettes. Offhand dye recolors vanilla families/sheep for 100 JE; saved 64-ball paint magazine applies original surface-splotch textures. One splotch per air block, not arbitrary layered source splats.',
        'memory_card': 'Copies same-type filter/threshold/polarity or pairs reciprocal P2P ports; original eight-cell identity overlay. Never copies inventories, upgrades, owner or JE.',
        'growth_accelerator': 'JE-powered, bounded loaded certus/amethyst budding attempts with source+target claims; no arbitrary mod randomTick calls.',
        'quartz_glass': 'Original pane/frame textures and source edge masks; vanilla multipart uses block-seeded variation instead of source continuous UV jitter.',
        'quartz_vibrant_glass': 'Same connected source glass, light15.',
        'entropy_manipulator': 'Supplied heat/cool conversions plus current server smelting recipes, with claims and no block-entity mutation. Source fire/TNT fallback is not enabled.',
        'network_tool': 'Native live loaded-network diagnostic, active-device/cell/JE count, 8/32-channel routes, shared wrench and real nine-slot upgrade-card wallet. Held tool hides cable facades.',
        'wireless_terminal': 'Bound native local loaded network, 64-block base range or boosted access-point coverage; no cross-dimension/chunkloading.',
        'wireless_crafting_terminal': 'Bound native local loaded network crafting, boosted access-point coverage; no cross-dimension/chunkloading.',
        'condenser': 'Original 256-unit matter-ball /256,000-unit singularity recipes with actual items/fluids and saved bounded credit. Explicit production modes; source trash mode omitted.',
        'formation_plane': 'Powered native single-block placement or item ejection using actual stock; no multi-block/container placement.',
        'annihilation_plane': 'Powered native single-block mining and unreserved item pickup with real drops and claims; containers refused.',
        'quantum_link': 'Original frame geometry; reciprocal loaded permitted 3x3 bridges share actual item/fluid storage with fees on both ends. Crafting/power services remain local; not full cross-dimensional grid parity.',
        'quantum_entangled_singularity': 'Two actual crafted keys pair two permitted loaded frames; reciprocal UUID identities prevent replaced-core reconnection. Native crafting replaces source explosion entanglement.',
        'quartz_fiber': 'Bounded transactional JE equalization between opposite permitted loaded networks; no item/fluid/channel connectivity.',
        'guide': 'Original guide item opens the shared searchable Jugcraft encyclopedia with native workshop instructions; external GuideMe runtime is not imported.',
        'facade': 'Four source anchors around a solid block craft four material-aware covers. Six independent two-pixel covers per cable, preserved on removal; no source block behavior copied.',
        'cable_anchor': 'Original anchor mesh attached to one of six cable faces; actual consumed and removable support pieces.',
        'cable_energy_acceptor': 'Original compact mesh with existing JE acceptor behavior in its own block space. Arbitrary multipart coexistence remains unsupported.',
        'cable_interface': 'Original compact mesh with existing transactional interface behavior in its own block space. Arbitrary multipart coexistence remains unsupported.',
        'cable_pattern_provider': 'Original compact mesh with existing recipe/batch provider behavior in its own block space. Arbitrary multipart coexistence remains unsupported.',
        'energy_card': 'Native shared tool capacity upgrade: adds base capacity up to twice, permanent fitting.',
        'void_card': 'Explicit excess disposal only for matching items in a partitioned cell; real removable card, transactional transfer/JE checks.',
        'spatial_io_port': 'Bounded journaled static-building exchange, 2³/16³ cells, eight corner pylons, real JE and claims. Durable rollback journal and revision-fenced plot keys. No entities, machines, fluids or arbitrary source dimension parity.',
        'spatial_storage_cell_2': 'Two-block-per-axis static building exchange; server-side plot revision prevents copied-cell duplication.',
        'spatial_storage_cell_16': 'Sixteen-block-per-axis static building exchange; maximum 4,096 supported blocks per operation.',
        'view_cell': 'Configured item-type partition filters an item/crafting/pattern terminal display. It never changes authorization.',
    }
    rows = []
    for path in sorted((source / 'items').glob('*.json')):
        name = path.stem
        if name in functional:
            status, runtime, detail = 'functional_native', 'jugcraft:jimta_' + name, limited.get(name, 'Registered native gameplay/recipe/material or building behavior; final combined tests pending.')
            if name.startswith('fluix_') and name.rsplit('_', 1)[-1] in ('axe', 'hoe', 'shovel', 'pickaxe', 'sword'): detail = 'Original material stats/geometry; innate Fortune1 or Looting1 maintained through native enchantment component, not source NeoForge hook.'
        elif name in reused:
            status = 'reused_existing'; runtime, detail = reused[name]
        else:
            status, runtime = 'pending', None
            if 'cable' in name: detail = 'Source colored/covered/smart/dense cable topology and channel display not yet ported; ordinary fluix cable is implemented separately.'
            elif name.startswith('debug_') or name in ('missing_content', 'wrapped_generic_stack', 'not_so_mysterious_cube'): status, detail = 'internal_source_only', 'Source debug/internal support item, retained in originals; not survival content.'
            elif name.startswith('creative_'): detail = 'Source administrative infinite-resource tool; no survival registration or acquisition. Not part of ordinary player progression.'
            elif 'paint_ball' in name: detail = 'Source paint/lumen projectile and surface-splotch system not yet implemented.'
            elif name.startswith('spatial_'): detail = '128³ migration/chunkloading is not exposed: source large cells and spatial anchor require bounded asynchronous storage and recovery beyond the implemented 2³/16³ static-block exchange.'
            elif name == 'matrix_frame': status, detail = 'internal_source_only', 'Source spatial-dimension boundary block. Native stored plots use a server ledger rather than a player-accessible storage dimension.'
            else: detail = 'Source art/data located; no completed native feature mapping claimed.'
        rows.append({'source_id': 'ae2:' + name, 'source_item': path.relative_to(root).as_posix(), 'status': status, 'runtime_id': runtime, 'implementation': detail})
    dest = root / 'art/owner-library/catalog/jimtafacts-ae-registry.json'
    dest.parent.mkdir(parents=True, exist_ok=True)
    statuses = ['functional_native', 'reused_existing', 'internal_source_only', 'pending']
    dest.write_text(json.dumps({'scope': 'Source AE item-definition files, including internal icons. Local implementation inventory; not test evidence or a full-parity claim.', 'statuses': statuses, 'counts': {s: sum(r['status'] == s for r in rows) for s in statuses}, 'entries': rows}, indent=2) + '\n', encoding='utf-8')
