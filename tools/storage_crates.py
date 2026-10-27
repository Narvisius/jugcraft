"""Tiered mixed storage; shared resources, recipes and handbook from the owner's selected costs."""
import json
import shutil
from pathlib import Path
from steampunk_models import box
import model_writer
from materials import METALS

TIERS = [('bronze', 54), ('steel', 108), ('invar', 162), ('stainless_steel', 216),
         ('aluminum', 270), ('titanium', 324), ('tungsten', 378)]
OWNER_FRAME = 'art/owner-library/originals/Blocks/crate_block_site_1_new.png'


def display(metal):
    return metal.replace('_', ' ').title() + ' Storage Crate'


def recipes():
    result = {}
    for index, (metal, slots) in enumerate(TIERS):
        block = metal + '_crate'
        conditions = [dict(condition='jugcraft:feature_enabled', feature=f)
                      for f in dict.fromkeys(['machines', METALS[metal]['feature']])]
        def shaped(suffix, pattern, center, output):
            result[block + suffix] = dict(type='minecraft:crafting_shaped', category='misc', pattern=pattern,
                key=dict(P='#c:plates/' + metal, C=center), result=dict(id='jugcraft:' + output, count=1),
                **{'fabric:load_conditions': conditions})
        shaped('_upgrade' if index else '', [' P ', 'PCP', ' P '],
               'jugcraft:' + TIERS[index-1][0] + '_crate' if index else 'minecraft:chest', block)
        if index:
            shaped('_direct', ['PPP', 'PCP', 'PPP'], 'minecraft:chest', block)
            shaped('_kit', [' P ', 'PCP', ' P '], 'minecraft:iron_nugget', metal + '_storage_crate_upgrade')
    return result


def crate_model(metal, tier):
    # Geometry is shared by every tier; crate_resource supplies the metal palette.
    panel, frame = 'storage_crate_panel', 'storage_crate_frame'
    parts = [box((1,1,1),(15,15,15),'storage_crate_seam')]
    thick = 2
    for x in (0, 16-thick):
        for z in (0, 16-thick):
            parts.append(box((x,0,z),(x+thick,16,z+thick),frame))
    # Four posts and eight rails form a connected frame along all twelve edges.
    for y in (0,16-thick):
        for edge in (0,16-thick):
            parts.append(box((thick,y,edge),(16-thick,y+thick,edge+thick),frame))
            parts.append(box((edge,y,thick),(edge+thick,y+thick,16-thick),frame))
    # Three broad horizontal plates on the walls; matching boards across lid and base.
    width = (16-2*thick)/3
    for i in range(3):
        lo = thick+i*width+.25
        hi = thick+(i+1)*width-.25
        parts += [box((thick,lo,.5),(16-thick,hi,1),panel),
                  box((thick,lo,15),(16-thick,hi,15.5),panel),
                  box((.5,lo,thick),(1,hi,16-thick),panel),
                  box((15,lo,thick),(15.5,hi,16-thick),panel),
                  box((thick,.5,lo),(16-thick,1,hi),panel),
                  box((thick,15,lo),(16-thick,15.5,hi),panel)]
    # The same simple latch on every tier, without tier-specific ribs or handles.
    parts += [box((6.5,6,.05),(9.5,10,.8),frame),
              box((7.25,6.5,0),(8.75,9.5,.2),'storage_crate_seam')]
    # Thin straps sit against the boards, below the outer rim. Their full end caps
    # overlap the corner fittings after rotation, hiding the cut ends inside metal.
    for x in (.45,15.3):
        parts.append(box((x,-1,7.25),(x+.25,17,8.75),frame,
                         rotation=('x',45,(x+.125,8,8))))
    parts.append(box((7.25,-1,15.3),(8.75,17,15.55),frame,
                     rotation=('z',45,(8,8,15.425))))
    return parts


def crate_resource(metal, tier):
    """Use palette swatches from the existing metal sprite, avoiding its tiled motif."""
    closed = model_writer.slice_model(metal+'_crate', crate_model(metal,tier), [(0,0,0)])[0]
    for element in closed:
        for direction, face in element['faces'].items():
            material = face['texture']
            if material == '#storage_crate_seam':
                uv = [1.1,1.1,1.9,1.9]  # Deepest metal shade in the recessed board joints.
            elif material == '#storage_crate_frame':
                uv = [3.1,3.1,3.9,3.9]  # Darker matching metal for rim and diagonal straps.
            elif direction == 'up':
                uv = [5.1,12.1,5.9,12.9]  # Lit edge of each separate board.
            elif direction == 'down':
                uv = [3.1,3.1,3.9,3.9]
            else:
                uv = [3.1,7.1,5.9,8.9]  # Clean broad metal face without embossed squares.
            face['uv'] = uv
    textures = {name:'jugcraft:block/'+metal+'_block' for name in
                ('storage_crate_panel','storage_crate_frame','storage_crate_seam','particle')}
    return dict(parent='minecraft:block/block', textures=textures, elements=closed)



def write_all(write, assets, data, lang):
    root = Path(__file__).resolve().parents[1]
    target = assets / 'textures/block/storage_crate_frame.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(root / OWNER_FRAME, target)
    for tier, (metal, slots) in enumerate(TIERS):
        block = metal + '_crate'
        write(assets/'models/block'/f'{block}.json', crate_resource(metal, tier))
        write(assets/'blockstates'/f'{block}.json',dict(variants={
            f'facing={f}':dict(model='jugcraft:block/'+block,y=y) for f,y in model_writer.FACING_Y.items()}))
        write(assets/'items'/f'{block}.json',dict(model=dict(type='minecraft:model',model='jugcraft:block/'+block)))
        write(data/'loot_table/blocks'/f'{block}.json',dict(type='minecraft:block',pools=[dict(rolls=1,
            entries=[dict(type='minecraft:item',name='jugcraft:'+block)],condition=dict(type='minecraft:survives_explosion'))]))
        lang['block.jugcraft.'+block] = display(metal)
        if tier:
            kit = metal + '_storage_crate_upgrade'
            parts = [box((3,3,7),(13,13,9),metal+'_block'),box((5,5,6),(11,11,7),'sp_iron'),
                     box((7,3,5),(9,13,6),metal+'_block'),box((3,7,5),(13,9,6),metal+'_block')]
            textures = {t: 'jugcraft:block/'+t for t in model_writer.texture_names(parts)}
            textures['particle'] = 'jugcraft:block/'+metal+'_block'
            write(assets/'models/item'/f'{kit}.json',dict(parent='minecraft:block/block',textures=textures,elements=model_writer.scaled_elements(parts)))
            write(assets/'items'/f'{kit}.json',dict(model=dict(type='minecraft:model',model='jugcraft:item/'+kit)))
            lang['item.jugcraft.'+kit] = display(metal)+' Upgrade Kit'
    for name, recipe in recipes().items(): write(data/'recipe/storage_crates'/f'{name}.json', recipe)
    for tag in ['mineable/pickaxe', 'needs_stone_tool']:
        path = data.parent/'minecraft/tags/block'/f'{tag}.json'
        obj = json.loads(path.read_text()) if path.exists() else dict(values=[])
        crates = ['jugcraft:'+m+'_crate' for m,_ in TIERS]
        obj['values'] = [value for value in obj['values'] if value not in crates]
        # Vehicle bays run after storage in the full generator. Preserve that order
        # when this writer is called alone against an already generated tag.
        index = next((i for i, value in enumerate(obj['values'])
                      if value in ('jugcraft:mecha_workbench', 'jugcraft:tank_workbench')), len(obj['values']))
        obj['values'][index:index] = crates
        write(path, obj)
    lang.update({
        'config.jade.plugin_jugcraft.storage_crate':'Storage crate capacity',
        'tooltip.jugcraft.storage_crate.capacity':'%s slots · %s× single-chest capacity',
        'tooltip.jugcraft.storage_crate.upgrade':'Use on %s to expand to %s slots. Contents are preserved.',
        'message.jugcraft.storage_crate.requires':'This upgrade requires a %s.',
        'screen.jugcraft.storage_crate.search':'Search items…',
        'screen.jugcraft.storage_crate.clear':'Clear search',
        'screen.jugcraft.storage_crate.no_matches':'No matching items',
        'screen.jugcraft.storage_crate.used':'%s / %s slots used',
        'screen.jugcraft.storage_crate.matches':'Matches: %s · %s / %s used',
        'screen.jugcraft.storage_crate.deposit_hint':'Shift-click stores in all slots.'})


def chapter():
    pages = []
    for i, (metal, slots) in enumerate(TIERS):
        previous = display(TIERS[i-1][0]) if i else 'ordinary chest'
        pages.append(dict(title=display(metal),icon='jugcraft:'+metal+'_crate',text=[
            f'{slots} mixed-item slots in one block: {slots//27} single chests. Normal stack limits, no power. Scroll through the grid or search by item name or ID; search does not move items. Hoppers, pipes and comparators access the whole inventory.',
            f'Craft four {metal.replace("_"," ")} plates around an empty {previous}.' +
            (f' Or craft eight plates around an ordinary chest to build this tier directly. Four plates around an iron nugget make an upgrade kit: use it on a placed {previous} to preserve its contents and name.' if i else ''),
            'Crates open beneath solid blocks. Breaking spills their contents; they are not portable filled containers. Search hides empty slots: clear it for manual placement, or shift-click to store across the whole crate.']))
    return dict(title='Storage Crates',icon='jugcraft:bronze_crate',pages=pages)
