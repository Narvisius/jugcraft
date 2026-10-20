"""New articulated submarine geometry using explicit regions of the owner's unchanged atlas."""
from menagerie_content import ASSET,write


def box(origin,size,uv):
    x,y,w,h=uv
    return {'origin':origin,'size':size,'uv':{face:{'uv':[x,y],'uv_size':[w,h]} for face in ('north','south','east','west','up','down')}}


def main():
    copper=[75,101,34,29];dark=[172,74,27,13];porthole=[34,99,36,36]
    bones=[{'name':'root','pivot':[0,0,0]},{'name':'hull','parent':'root','pivot':[0,12,0],'cubes':[box([-13,3,-21],[26,21,42],copper),box([-10,1,-19],[20,2,38],dark),box([-10,24,-8],[20,7,17],copper),box([-7,31,-4],[14,2,10],dark),box([-10,6,-23],[20,18,2],porthole),box([-5,25,-9],[10,4,1],[105,75,20,8])]},
           {'name':'skids','parent':'root','pivot':[0,0,0],'cubes':[box([-15,0,-18],[3,3,39],dark),box([12,0,-18],[3,3,39],dark),box([-15,3,-14],[3,3,4],dark),box([12,3,-14],[3,3,4],dark)]},
           {'name':'rudder','parent':'root','pivot':[0,14,22],'cubes':[box([-1,5,20],[2,20,9],copper),box([-12,13,20],[24,2,8],copper)]},
           {'name':'propeller','parent':'root','pivot':[0,13,30],'cubes':[box([-2,11,28],[4,4,5],dark),box([-10,12,30],[20,2,1],dark),box([-1,3,30],[2,20,1],dark)]}]
    write(ASSET/'geckolib/models/entity/menagerie/cavern_submarine.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.jugcraft.cavern_submarine','texture_width':512,'texture_height':512,'visible_bounds_width':4,'visible_bounds_height':3,'visible_bounds_offset':[0,1,0]},'bones':bones}]})
    write(ASSET/'geckolib/animations/entity/menagerie/cavern_submarine.animation.json',{'format_version':'1.8.0','animations':{'idle':{'loop':True,'animation_length':2,'bones':{}},'drive':{'loop':True,'animation_length':1,'bones':{'propeller':{'rotation':[0,0,'query.anim_time * 720']},'rudder':{'rotation':[0,'math.sin(query.anim_time * 180) * 5',0]}}}}})

    from wildlife_geometry import apply_skin
    model={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.jugcraft.beast_straddleboard','texture_width':128,'texture_height':128,'visible_bounds_width':3,'visible_bounds_height':1,'visible_bounds_offset':[0,.2,0]},'bones':[{'name':'root','pivot':[0,0,0]},{'name':'board','parent':'root','pivot':[0,2,0],'cubes':[{'origin':[-5,1,-13],'size':[10,2,26],'_skin_role':'fur'},{'origin':[-4,2,-17],'size':[8,2,4],'_skin_role':'fur'},{'origin':[-4,2,13],'size':[8,2,4],'_skin_role':'fur'},{'origin':[-1,-2,5],'size':[2,3,6],'_skin_role':'dark'}]}]}]}
    write(ASSET/'geckolib/models/entity/menagerie/beast_straddleboard.geo.json',apply_skin(model,'menagerie/beast/entity/straddleboard'))
    write(ASSET/'geckolib/animations/entity/menagerie/beast_straddleboard.animation.json',{'format_version':'1.8.0','animations':{'idle':{'loop':True,'animation_length':2,'bones':{}},'drive':{'loop':True,'animation_length':2,'bones':{'board':{'rotation':[0,0,'math.sin(query.anim_time * 180) * 3']}}}}})


if __name__=='__main__':main()
