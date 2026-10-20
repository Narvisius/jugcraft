"""Explicit native encounter/companion profiles for all supplied living cave species."""
# name shape movement cave width height health mood food mount
ROWS='''
teletor humanoid air magnetic_caves .7 1.6 28 hostile iron_ingot no
magnetron sentinel land magnetic_caves 1.6 3.2 100 hostile iron_ingot no
boundroid cube air magnetic_caves 1.1 1.1 30 hostile iron_ingot no
ferrouslime cube land magnetic_caves 1.2 1.2 28 hostile iron_nugget no
notor sentinel air magnetic_caves .6 .7 16 neutral iron_nugget no
subterranodon pterosaur air primordial_caves 1.4 1.5 36 neutral salmon yes
vallumraptor raptor land primordial_caves .8 1.4 28 neutral rabbit no
grottoceratops ceratopsian land primordial_caves 2.2 2.0 70 neutral wheat yes
trilocaris crab water primordial_caves .6 .3 10 passive kelp no
tremorsaurus theropod land primordial_caves 2.4 3.4 120 predator porkchop yes
relicheirus theropod land primordial_caves 1.8 3.0 80 neutral wheat yes
luxtructosaurus theropod land summoned 3.5 5.2 300 hostile magma_cream no
atlatitan sauropod land primordial_caves 3.8 5.5 180 neutral wheat yes
nucleeper creeper land toxic_caves .9 1.9 35 hostile uranium no
radgill fish water toxic_caves .5 .35 10 passive kelp no
brainiac humanoid land toxic_caves 1.5 2.7 80 hostile rotten_flesh no
gammaroach insect land toxic_caves .8 .4 20 hostile rotten_flesh no
raycat cat land toxic_caves .7 .7 20 neutral cod no
tremorzilla theropod land summoned 4.2 6.0 400 neutral uranium yes
lanternfish fish water abyssal_chasm .45 .4 8 passive kelp no
sea_pig mammal water abyssal_chasm .45 .3 8 passive kelp no
hullbreaker shark water abyssal_chasm 3.4 2.4 160 hostile cod no
gossamer_worm worm water abyssal_chasm 1.2 .4 12 passive kelp no
tripodfish fish water abyssal_chasm .6 .9 12 passive cod no
deep_one humanoid amphibious abyssal_chasm .75 1.8 30 neutral pearl no
deep_one_knight humanoid amphibious abyssal_chasm .9 2.2 60 neutral pearl no
deep_one_mage humanoid amphibious abyssal_chasm .8 2.0 40 neutral pearl no
mine_guardian sphere water abyssal_chasm .9 .9 26 hostile iron_nugget no
gloomoth butterfly air forlorn_hollows .55 .45 8 passive glow_berries no
underzealot humanoid land forlorn_hollows .65 1.8 30 hostile rotten_flesh no
watcher humanoid land forlorn_hollows .8 2.4 45 hostile rotten_flesh no
corrodent rodent land forlorn_hollows .8 .55 24 hostile rotten_flesh no
vesper bat air forlorn_hollows 1.1 1.3 28 hostile rabbit no
forsaken sentinel land summoned 2.1 3.8 220 hostile rotten_flesh no
sweetish_fish fish water candy_cavity .45 .35 8 passive sugar no
caniac humanoid land candy_cavity 1.0 2.3 45 hostile sugar no
gumbeeper creeper land candy_cavity .8 1.4 28 hostile sugar no
candicorn horse land candy_cavity 1.4 1.8 45 neutral apple yes
gum_worm worm land candy_cavity 2.0 1.5 70 neutral sugar yes
caramel_cube cube land candy_cavity 1.0 1.0 22 neutral sugar no
gummy_bear bear land candy_cavity 1.0 1.2 36 neutral sugar no
licowitch humanoid land candy_cavity .6 1.9 50 hostile sugar no
gingerbread_man humanoid land candy_cavity .45 1.0 14 neutral sugar no
'''


def profiles():
    result=[]
    for row in ROWS.strip().splitlines():
        n,shape,movement,biome,w,h,hp,mood,food,ride=row.split()
        feed=('jugcraft:cavern_' if food in ('uranium','pearl') else 'minecraft:')+food
        result.append(dict(source=n,id='cavern_'+n,name=n.replace('_',' ').title(),shape=shape,movement=movement,habitat='summoned',cave=biome,width=float(w),height=float(h),health=int(hp),mood=mood,feed=feed,mount=ride=='yes',model='cavern_'+n,clips=['idle','walk','fly','swim','sit','attack'],armorTextures={}))
    return result
