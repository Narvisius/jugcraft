"""Authored native ecology for the larger supplied creature roster.

Each row is a deliberate habitat, silhouette, food and disposition, not inferred
gameplay from filenames. Bosses have explicit encounter routes rather than
being placed in ordinary passive spawn pools.
"""
# name shape movement habitat width height health mood feed ride
ROWS='''
grizzly_bear bear land forest 1.4 1.5 40 neutral salmon no
roadrunner bird land desert .45 .65 10 passive wheat_seeds no
bone_serpent snake land nether 1.3 1.2 60 hostile bone no
gazelle deer land savanna .8 1.4 18 passive wheat no
crocodile reptile amphibious swamp 1.3 .65 36 predator chicken no
fly insect air plains .25 .25 4 passive rotten_flesh no
hummingbird bird air meadow .3 .3 6 passive sugar no
orca whale water ocean 2.2 1.5 70 predator salmon no
sunbird bird air mountain 1.5 1.3 40 neutral blaze_powder yes
gorilla ape land jungle 1.1 1.8 40 neutral beast_banana no
crimson_mosquito insect air nether .9 1.1 22 hostile rotten_flesh no
rattlesnake snake land desert .6 .4 14 neutral rabbit no
endergrade insect air end .9 .8 24 passive chorus_fruit yes
hammerhead_shark shark water ocean 1.5 .8 32 predator cod no
lobster crab water beach .55 .35 8 passive cod no
komodo_dragon reptile land jungle 1.1 .6 28 predator chicken no
capuchin_monkey ape land jungle .55 .85 14 neutral beast_banana no
centipede_head insect land cave .8 .5 24 hostile rotten_flesh no
warped_toad frog land nether 1.2 1.0 30 neutral warped_fungus yes
moose deer land snowy 1.3 2.1 36 neutral wheat no
mimicube cube land end 1 1 30 hostile chorus_fruit no
raccoon rodent land forest .6 .55 14 neutral sweet_berries no
blobfish fish water deep_ocean .5 .35 8 passive cod no
seal mammal amphibious snowy .8 .65 18 passive cod no
cockroach insect land cave .35 .25 6 passive sugar no
shoebill bird amphibious swamp .65 1.2 18 neutral salmon no
elephant elephant land savanna 2.2 2.8 80 neutral beast_acacia_blossom yes
soul_vulture bird air nether .9 1 28 hostile bone no
snow_leopard cat land snowy .95 .9 30 predator rabbit no
spectre fish air end 1.5 .7 25 passive glow_berries yes
crow bird air forest .45 .5 10 neutral wheat_seeds no
alligator_snapping_turtle shell amphibious swamp 1 .6 30 neutral salmon no
mungus mushroom land mushroom .9 1.3 24 passive red_mushroom no
mantis_shrimp crab water ocean .8 .6 24 neutral cod no
guster tornado air desert 1 1.8 32 hostile sand no
warped_mosco insect air nether 1.6 2.5 120 hostile warped_fungus no
straddler reptile land nether 1.3 1.8 40 hostile crimson_fungus no
stradpole fish water nether .45 .35 8 passive crimson_fungus no
emu ostrich land savanna .8 1.6 22 neutral wheat_seeds no
platypus mammal amphibious river .6 .45 14 passive spider_eye no
dropbear bear land forest .8 1.1 26 hostile rabbit no
tasmanian_devil mammal land forest .65 .6 16 neutral rotten_flesh no
kangaroo kangaroo land savanna .9 1.8 30 neutral carrot yes
cachalot_whale whale water deep_ocean 3 2.2 140 neutral cod no
leafcutter_ant insect land jungle .25 .25 6 neutral oak_leaves no
enderiophage insect air end .8 1.2 20 hostile chorus_fruit no
bald_eagle bird air mountain .7 .8 20 neutral salmon no
tiger cat land jungle 1.3 1.2 40 predator porkchop no
tarantula_hawk insect air desert .9 .8 24 neutral spider_eye no
void_worm worm air summoned 2.4 2.4 260 hostile chorus_fruit no
frilled_shark shark water deep_ocean 1.1 .55 24 predator cod no
mimic_octopus octopus water ocean .7 .65 14 passive cod no
seagull bird air beach .5 .55 10 neutral cod no
froststalker reptile land snowy .9 1.2 28 predator salmon no
tusklin boar land snowy 1.5 1.4 50 neutral brown_mushroom yes
laviathan reptile amphibious nether 2.2 1.6 80 neutral crimson_fungus yes
cosmaw fish air end 1.2 1 32 neutral chorus_fruit yes
toucan bird air jungle .55 .7 12 passive beast_banana no
maned_wolf mammal land savanna .9 1.3 22 neutral sweet_berries no
anaconda snake amphibious jungle 1.1 .6 40 predator chicken no
anteater mammal land savanna 1 .8 24 neutral beast_gongylidia no
rocky_roller shell land badlands 1.1 1 32 neutral iron_nugget no
flutter butterfly air meadow .6 .7 12 passive flowering_azalea no
gelada_monkey ape land mountain .8 1.1 24 neutral wheat no
jerboa rodent land desert .35 .5 8 passive wheat_seeds no
terrapin shell amphibious river .5 .4 12 passive seagrass no
comb_jelly jellyfish water ocean .6 .7 8 passive kelp no
cosmic_cod fish air end .5 .35 10 passive chorus_fruit no
bunfungus mushroom land mushroom 1.4 2 60 neutral brown_mushroom no
bison boar land plains 1.5 1.7 45 neutral wheat no
giant_squid octopus water deep_ocean 2.4 2 80 neutral cod no
sea_bear bear amphibious summoned 1.5 1.5 80 hostile cod no
devils_hole_pupfish fish water desert .3 .25 6 passive wheat_seeds no
catfish fish water river 1 .6 18 neutral cod no
flying_fish fish water ocean .5 .3 8 passive kelp no
skelewag shark water deep_ocean 1.3 1.2 36 hostile cod no
rain_frog frog land desert .4 .3 10 passive slime_ball no
potoo bird air jungle .45 .7 10 passive spider_eye no
mudskipper fish amphibious swamp .5 .35 12 neutral tropical_fish no
rhinoceros rhino land savanna 1.7 1.8 65 neutral wheat no
sugar_glider rodent air forest .4 .4 10 passive sweet_berries no
farseer sentinel air summoned 1.3 2.8 180 hostile ender_pearl no
skreecher ape land cave .8 1.4 24 hostile rotten_flesh no
underminer humanoid land cave .6 1.9 30 neutral iron_ingot no
murmur humanoid land cave .7 2.5 50 hostile rotten_flesh no
skunk rodent land forest .65 .5 14 neutral sweet_berries no
banana_slug shell land forest .45 .25 8 passive brown_mushroom no
blue_jay bird air forest .35 .4 8 passive wheat_seeds no
caiman reptile amphibious swamp .85 .45 22 neutral chicken no
triops crab water river .35 .25 8 passive seagrass no
'''


def profiles():
    result=[]
    for row in ROWS.strip().splitlines():
        name,shape,movement,habitat,w,h,hp,mood,food,ride=row.split()
        result.append(dict(source=name,id='beast_'+name,shape=shape,movement=movement,habitat=habitat,width=float(w),height=float(h),health=int(hp),mood=mood,feed=('jugcraft:' if food.startswith('beast_') else 'minecraft:')+food,mount=ride=='yes',model='beast_'+name,clips=['idle','walk','fly','swim','sit','attack'],armorTextures={}))
    return result
