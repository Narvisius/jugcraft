"""Shared-material storage shells with supported, centered pipe fittings."""
from starter_chemistry_models import STEEL, SILVER, OLIVE, CHROME
from steampunk_models import box
from model_writer import scaled_elements, texture_names, finish_elements

def tank_model(vacuum=False):
    parts = [box((1,0,1),(15,2,15),STEEL), box((2,2,2),(14,14,14),SILVER),
             box((1,14,1),(15,16,15),STEEL)]
    for y in ([3,7,11] if vacuum else [3,11]):
        parts.append(box((1.75,y,1.75),(14.25,y+1,14.25),OLIVE))
    # Every fitting ends at the adjacent block's pipe center. No decorative dead-end pipes.
    parts += [box((5,5,0),(11,11,2.1),STEEL),box((6,6,0),(10,10,.25),CHROME),
              box((5,5,13.9),(11,11,16),STEEL),box((6,6,15.75),(10,10,16),CHROME),
              box((0,5,5),(2.1,11,11),STEEL),box((0,6,6),(.25,10,10),CHROME),
              box((13.9,5,5),(16,11,11),STEEL),box((15.75,6,6),(16,10,10),CHROME),
              box((5,0,5),(11,2.1,11),CHROME),box((5,13.9,5),(11,16,11),CHROME)]
    elements=scaled_elements(parts)
    finish_elements(elements)
    return {'parent':'minecraft:block/block','textures':{
        'particle':'jugcraft:block/'+SILVER, **{name:'jugcraft:block/'+name for name in texture_names(parts)}},
        'elements':elements}
