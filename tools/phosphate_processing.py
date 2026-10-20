"""Package 5: abstract phosphate batches and crop-specialist fertilizers.

Uses existing machines. Values describe gameplay, not real chemical procedures.
"""
from biorefinery import liquid

ITEMS = {
    'ground_phosphate': 'Ground Phosphate',
    'wet_superphosphate': 'Wet Superphosphate',
    'triple_superphosphate': 'Triple Superphosphate',
    'nitrogen_fertilizer': 'Nitrogen Field Fertilizer',
    'mixed_farm_blend': 'Mixed-farm Fertilizer',
    'organic_fertilizer_carrier': 'Organic Fertilizer Carrier',
    'gypsum_rich_residue': 'Gypsum-rich Residue',
    'washed_gypsum': 'Washed Gypsum',
}
FLUIDS = {
    'phosphate_slurry': liquid('Phosphate Slurry', [(54,51,37),(91,87,62),(133,128,94),(177,174,139)]),
    'phosphoric_acid': liquid('Industrial Phosphoric Acid', [(58,71,65),(96,120,108),(144,163,151),(190,206,194)]),
}

def rec(name, cap, items=(), fluids=(), results=(), fluid_results=(), ticks=240, power=48):
    return dict(name='phosphate_'+name, capability='jugcraft:'+cap, items=list(items), fluids=list(fluids),
                results=list(results), fluid_results=list(fluid_results), ticks=ticks, energy_per_tick=power,
                features=['machines','phosphate'])

RECIPES = {
    'chemical_dissolution_chamber': [
        rec('dissolve','phosphate_dissolution',[('jugcraft:ground_phosphate',2)], [('jugcraft:sulfuric_acid',1000)],
            fluid_results=[('jugcraft:phosphate_slurry',1000,0)],ticks=320),
    ],
    'aluminate_filter_press': [
        rec('filter','phosphate_filtration',fluids=[('jugcraft:phosphate_slurry',1000)],
            results=[('jugcraft:gypsum_rich_residue',2)],
            fluid_results=[('jugcraft:phosphoric_acid',750,0),('minecraft:water',250,1)]),
    ],
    'bulk_chemical_reactor': [
        rec('wet_superphosphate','phosphate_formulation',[('jugcraft:ground_phosphate',1)],
            [('jugcraft:phosphoric_acid',250)],[('jugcraft:wet_superphosphate',2)]),
        rec('nutrient_solution','phosphate_formulation',[('jugcraft:mixed_farm_blend',1)],
            [('minecraft:water',1000)],fluid_results=[('jugcraft:nutrient_solution',1000,0)]),
    ],
    'mineral_conditioner': [
        rec('compost_carrier','phosphate_preparation',[('jugcraft:organic_compost',1)],
            [('minecraft:water',250)],[('jugcraft:organic_fertilizer_carrier',4)],ticks=160),
        rec('fiber_carrier','phosphate_preparation',[('jugcraft:digestate_fiber',2)],
            [('minecraft:water',250)],[('jugcraft:organic_fertilizer_carrier',1)],ticks=160),
        rec('wash_gypsum','phosphate_preparation',[('jugcraft:gypsum_rich_residue',2)],
            [('minecraft:water',500)],[('jugcraft:washed_gypsum',1)],
            [('jugcraft:spent_mineral_wash',500,0)]),
    ],
    'alumina_calciner': [
        rec('dry_fertilizer','phosphate_drying',[('jugcraft:wet_superphosphate',1)],
            results=[('jugcraft:triple_superphosphate',1)],fluid_results=[('minecraft:water',125,0)]),
        rec('prepare_binder','phosphate_drying',[('jugcraft:washed_gypsum',1)],
            results=[('jugcraft:gypsum_binder',1)],ticks=320),
    ],
}
REACTOR_RECIPES = [
    rec('nitrogen_blend','unused',[('jugcraft:ammonium_nitrate',1),('jugcraft:organic_fertilizer_carrier',1)],
        results=[('jugcraft:nitrogen_fertilizer',2)],ticks=160),
    rec('battery_precursor','unused',fluids=[('jugcraft:phosphoric_acid',250),('jugcraft:hydrogen_fluoride',500)],
        results=[('jugcraft:fluorophosphate_precursor',1)],ticks=280,power=64),
]
for recipe in REACTOR_RECIPES: recipe.pop('capability')
# Drying returns the 125 mB held in each wet intermediate, not newly created water.
RECIPES['alumina_calciner'][0]['source'] = 125

MACHINE_RECIPES = {
    'pulverizer': [dict(name='phosphate_grinding',input='jugcraft:phosphate',
        output='jugcraft:ground_phosphate',count=1,ticks=160,features=['machines','phosphate'])],
}
TANK_NAMES = {'fluoride_gas': 'Process outlet (HF / phosphate slurry)'}
GUIDE_PAGES = [
    dict(title='Phosphate into useful supplies',icon='jugcraft:ground_phosphate',text=[
        'Pulverize one existing Phosphate into one Ground Phosphate. The Chemical Dissolution Chamber combines two ground phosphate and 1000 mB ordinary sulfuric acid into 1000 mB slurry. Its existing process outlet now serves either HF or slurry; empty it when changing recipes.',
        'The shared Filter Press converts 1000 mB slurry into 750 mB Industrial Phosphoric Acid (first fluid outlet), 250 mB water (second) and two Gypsum-rich Residue (first item output). Nothing is vented: blocked byproducts stop production. Use the recipe picker, previews and persistent Lock recipe.',
        'The Bulk Chemical Reactor combines one ground phosphate and 250 mB acid into two Wet Superphosphate. The shared Industrial Oven dries each into one Triple Superphosphate, returning 125 mB water. Recipes are abstract game batches, not real manufacturing instructions.']),
    dict(title='Choose a fertilizer for your farm',icon='jugcraft:mixed_farm_blend',text=[
        'Nitrogen Field Fertilizer gives four bone-meal applications to tagged grains, leaves and fibre crops; Triple Superphosphate gives four to tagged roots, legumes and fruiting crops. Either gives one application to other crops. Mixed-farm Fertilizer gives three to every eligible crop. Vanilla growth rules still decide the result; these are growth applications, not guaranteed harvests.',
        'Use on a crop or ground to reach a 5 x 5 area, one block up or down. Tall plants count once. An item is only consumed if growth succeeds. Decorations and trees are excluded. The tooltip and use message identify the selected specialty.',
        'The Conditioner makes four Organic Fertilizer Carriers from one Organic Compost and 250 mB water, or one carrier from two Digestate Fiber and 250 mB water. The compact Chemical Reactor blends one ammonium nitrate and one carrier into two Nitrogen Field Fertilizers. Craft one nitrogen fertilizer, one Triple Superphosphate and one carrier into two Mixed-farm Fertilizers.',
        'Existing bone meal, ordinary fertilizer, ammonium nitrate and rich-soil compost routes stay available. Compost still offers passive soil improvement; the carrier route consumes the unplaced block instead. Crop groups live in fertilizer_nitrogen_crops and fertilizer_phosphate_crops block tags for datapack extensions.']),
    dict(title='Sprinklers and hydroponics',icon='jugcraft:sprinkler',text=[
        'Load one fertilizer type at a time into a sprinkler by hand, hopper or item pipe. It stores up to 16. New blends keep their crop-group effect when the existing water-powered fertilizer pulse runs. Ordinary Fertilizer keeps its previous effect; old saves retain their stored count.',
        'Use an empty hand to read the loaded fertilizer type and count. Sneak-use with an empty hand retrieves the fertilizer and any stored buckets so you can change the load. Sprinklers do not bypass town protection for the new blends. No background crop scanning or chunk loading is added.',
        'The Bulk Reactor dissolves one Mixed-farm Fertilizer with 1000 mB water into 1000 mB existing Nutrient Solution for hydroponics. This gives ordinary nutrient solution, without a hidden crop bonus. The earlier ordinary-fertilizer nutrient recipe remains useful.']),
    dict(title='Gypsum and battery integration',icon='jugcraft:plasterboard',text=[
        'Condition two Gypsum-rich Residue with 500 mB water into one Washed Gypsum and 500 mB Spent Mineral Wash. The Industrial Oven prepares one washed gypsum into one existing Gypsum Binder. Keep the wash for the existing settling/recovery route; raw residue cannot be placed as a building block.',
        'Craft one binder with two sand into two Plaster blocks, or two binder, two paper and one Digestate Fiber into four Plasterboard blocks. Both have matching slabs and stairs. Existing calcium-sulfate binder and concrete recipes remain available.',
        'The compact Chemical Reactor can also turn 250 mB Industrial Phosphoric Acid and 500 mB HF into one existing Fluorophosphate Precursor for the battery chain. The earlier direct phosphate route remains available. This industrial acid is not a food-grade cola ingredient.']),
]

def write_extras(write, assets, data, lang, condition):
    import agriculture
    import herbs
    import fruit_crops
    crafts = {
        'mixed_blend': (['jugcraft:nitrogen_fertilizer','jugcraft:triple_superphosphate','jugcraft:organic_fertilizer_carrier'],'mixed_farm_blend',2),
        'plaster': (['jugcraft:gypsum_binder','minecraft:sand','minecraft:sand'],'plaster',2),
        'plasterboard': (['jugcraft:gypsum_binder']*2+['minecraft:paper']*2+['jugcraft:digestate_fiber'],'plasterboard',4),
    }
    for name,(ingredients,result,count) in crafts.items():
        write(data/'recipe'/f'phosphate_{name}.json',{'fabric:load_conditions':condition('machines')+condition('phosphate'),
            'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,
            'result':dict(id='jugcraft:'+result,count=count)})
    nitrogen = {'corn','ornamental_corn','sunflower','flax','cotton','cabbage','oats','barley','rice','lettuce','spinach'} | set(herbs.TALL_CROPS)
    roots = {'beans','peas','sweet_potato','onion','garlic','turnip','mandrake','tomato','pepper','cucumber','eggplant','zucchini','radish','ginger'} | set(fruit_crops.BUSHES)
    for group, names, vanilla in [('nitrogen',nitrogen,['wheat']),('phosphate',roots,['carrots','potatoes','beetroots','pumpkin_stem','melon_stem'])]:
        values = ['minecraft:'+name for name in vanilla]
        values += [dict(id='jugcraft:'+info['block'],required=False)
                   for name,info in {**agriculture.CROPS,**agriculture.TALL_CROPS}.items() if name in names]
        if group=='nitrogen':
            values.append(dict(id='jugcraft:cotton_crop',required=False))
        if group=='phosphate':
            values += [dict(id='jugcraft:'+agriculture.stem(name),required=False) for name in agriculture.GOURDS]
        write(data/'tags/block'/f'fertilizer_{group}_crops.json',dict(replace=False,values=values))
    lang.update({
        'tooltip.jugcraft.nitrogen_fertilizer':'5x5 crops: 4 growth applications to grains, leaves and fibre; 1 to other crops.',
        'tooltip.jugcraft.triple_superphosphate':'5x5 crops: 4 growth applications to roots, legumes and fruiting crops; 1 to others.',
        'tooltip.jugcraft.mixed_farm_blend':'5x5 crops: 3 growth applications per crop. Useful for mixed farms.',
        'message.jugcraft.fertilizer_no_growth':'No eligible crops grew; fertilizer kept.',
        'message.jugcraft.fertilizer_applied':'%s: %s plants treated.',
        'message.jugcraft.sprinkler_loaded':'Water: %s mB | Fertilizer: %s x %s. Sneak with empty hand to unload.',
        'message.jugcraft.sprinkler_wrong_fertilizer':'Fertilizer hopper full or holding another type. Sneak with empty hand to unload.',
        'message.jugcraft.no_fertilizer':'Empty',
    })

def draw_all(save):
    import item_icons
    for name in ITEMS: save(item_icons.draw(name),'item',name)
