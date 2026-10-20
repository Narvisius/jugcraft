"""Adapt block-entity identities in runtime NBT copies. Originals are never changed."""
import nouveau_nbt


def adapt(raw,replace):
    def current(value):
        value=replace(value)
        return 'minecraft:iron_chain' if value=='minecraft:chain' else value
    kind,name,root=nouveau_nbt.read(nouveau_nbt.adapt(raw,current))
    palette=root.get('palette',(9,(10,[])))[1][1]
    for block in root.get('blocks',(9,(10,[])))[1][1]:
        data=block.get('nbt')
        if not data:continue
        nbt=data[1];index=block['state'][1];state=palette[index] if index<len(palette) else {};id=state.get('id',(8,''))[1]
        if id.endswith('_wall_hanging_sign') or id.endswith('_hanging_sign'):nbt['id']=(8,'minecraft:hanging_sign')
        elif id.endswith('_wall_sign') or id.endswith('_sign'):nbt['id']=(8,'minecraft:sign')
        elif any(id.endswith('_'+n) for n in ('gingerbarrel','metal_barrel','cabinet','shelf')):nbt['id']=(8,'jugcraft:kitchen_cabinet')
        elif id in {'jugcraft:cavern_'+n for n in ('nuclear_furnace_component','quarry','confection_oven','conversion_crucible','hologram_projector')}:nbt['id']=(8,'jugcraft:cavern_device')
        elif id=='jugcraft:beast_capsid':nbt['id']=(8,'jugcraft:beast_capsid')
        elif id in {'jugcraft:cavern_'+n for n in ('azure_magnet','scarlet_magnet','magnetic_activator','magnetic_levitation_rail','nuclear_bomb','nuclear_siren','siren_light','tesla_bulb','drain','copper_valve')}|{'jugcraft:beast_'+n for n in ('void_worm_beak','sculk_boomer','end_pirate_anchor_winch','end_pirate_ship_wheel')}:nbt['id']=(8,'jugcraft:cavern_control')
        # Runtime structure copies never execute commands embedded in an owner data archive.
        if 'Command' in nbt:nbt['Command']=(8,'');nbt['auto']=(1,0);nbt['powered']=(1,0)
        if id=='minecraft:spawner':
            nbt['id']=(8,'minecraft:mob_spawner');nbt['SpawnCount']=(2,min(2,nbt.get('SpawnCount',(2,2))[1]));nbt['MaxNearbyEntities']=(2,min(8,nbt.get('MaxNearbyEntities',(2,8))[1]));nbt['MinSpawnDelay']=(2,max(200,nbt.get('MinSpawnDelay',(2,200))[1]))
    return nouveau_nbt.write((kind,name,root))
