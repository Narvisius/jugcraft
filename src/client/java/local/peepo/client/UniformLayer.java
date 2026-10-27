package local.peepo.client;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import local.peepo.CompanionUniforms.Type;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Small, prebuilt costume geometry follows the existing animated bones, including sleep and work. */
final class UniformLayer extends RenderLayer<PeepoState,PeepoModel> {
    /** Reuse real cloth, canvas, straw, leather and hardware surfaces, never wool. */
    private enum Material {
        CLOTH("minecraft:textures/entity/banner/banner_base.png",2/64F,2/64F,18/64F,34/64F),
        CANVAS("jugcraft:textures/block/dz_canvas.png",2/16F,4/16F,14/16F,15/16F),
        STRAW("jugcraft:textures/block/scarecrow_straw.png",0,0,1,1),
        LEATHER("jugcraft:textures/block/pi_leather.png",0,0,1,1),
        METAL("minecraft:textures/block/iron_block.png",2/16F,2/16F,14/16F,14/16F);
        final Identifier texture;final float u0,v0,u1,v1;
        Material(String texture,float u0,float v0,float u1,float v1){
            this.texture=Identifier.parse(texture);this.u0=u0;this.v0=v0;this.u1=u1;this.v1=v1;
        }
    }
    private record Face(float[] vertices,float nx,float ny,float nz,float shade) {}
    private static final class Box {
        final List<Face> faces;final int color;
        Box(float x,float y,float z,float w,float h,float d,int color){
            float X=x+w,Y=y+h,Z=z+d;this.color=color;
            faces=List.of(face(new float[]{x,y,z,x,y,Z,x,Y,Z,x,Y,z}),face(new float[]{X,y,Z,X,y,z,X,Y,z,X,Y,Z}),
                face(new float[]{x,y,z,X,y,z,X,y,Z,x,y,Z}),face(new float[]{x,Y,Z,X,Y,Z,X,Y,z,x,Y,z}),
                face(new float[]{X,y,z,x,y,z,x,Y,z,X,Y,z}),face(new float[]{x,y,Z,X,y,Z,X,Y,Z,x,Y,Z}));
        }
    }
    private static final Map<Type,Map<String,Map<Material,List<Box>>>> PEEPO=batch(create(false)),JUGHEAD=batch(create(true));
    private record Piece(Material material,Box shape) {}
    UniformLayer(PeepoRenderer renderer){super(renderer);}
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState s,float yaw,float pitch){
        if(s.isInvisible || s.pumpkin || s.uniform==Type.NONE)return;
        var parts=(s.jughead?JUGHEAD:PEEPO).get(s.uniform);if(parts==null)return;
        for(var entry:parts.entrySet()){
            pose.pushPose();getParentModel().uniformBone(entry.getKey()).translateAndRotate(pose);
            for(var group:entry.getValue().entrySet()){
                var material=group.getKey();var boxes=group.getValue();
                collector.submitCustomGeometry(pose,RenderTypes.entityCutout(material.texture),(matrix,c)->{
                    for(var box:boxes)for(var face:box.faces)drawFace(matrix,c,face,box.color,material,light);
                });
            }
            pose.popPose();
        }
    }
    private static Map<Type,Map<String,List<Piece>>> create(boolean jug){
        var result=new EnumMap<Type,Map<String,List<Piece>>>(Type.class);
        for(var type:Type.values())if(type!=Type.NONE){
            var m=new LinkedHashMap<String,List<Piece>>();result.put(type,m);
            int blue=0x36567E,navy=0x283B57,straw=0xDCBF68,brown=0x694832,white=0xF5F0DF,red=0xB83E35,green=0x507159,metal=0xB4BEC2;
            switch(type){
                case FARMER -> {
                    ring(m,-4.5F,3.8F,3.3F,.2F,jug?.9F:0,Material.STRAW,0xFFFFFF);
                    if(jug)crown(m,-4.5F,2.85F,2.45F,Material.STRAW,0xFFFFFF);
                    else add(m,"head",-2.5F,-5.65F,-1.9F,5,1.15F,3.8F,Material.STRAW,0xFFFFFF);
                    ring(m,-4.85F,jug?2.89F:2.55F,jug?2.49F:1.95F,.27F,jug?.86F:0,Material.LEATHER,0xFFFFFF);
                    add(m,"body",-3.08F,11,-2.23F,6.16F,1.8F,4.46F,blue);
                    add(m,"body",-1.8F,9.55F,-2.25F,3.6F,1.45F,.23F,blue);
                    add(m,"body",-1,10,-2.51F,2,.65F,.15F,navy);
                    for(float x:new float[]{-2.1F,1.6F}){
                        add(m,"body",x,9,-2.28F,.5F,2.1F,.22F,blue);add(m,"body",x,9,2.13F,.5F,2.1F,.22F,blue);
                        add(m,"body",x+.1F,9.8F,-2.51F,.3F,.3F,.13F,Material.METAL,straw);
                    }
                    cuffs(m,blue);add(m,"body",2.65F,10.65F,.8F,1,.95F,1.35F,Material.LEATHER,0xFFFFFF);
                }
                case FISHERMAN -> {
                    ring(m,-4.45F,3.35F,2.85F,.22F,jug?.9F:0,Material.CANVAS,0xFFFFFF);
                    if(jug)crown(m,-4.45F,2.85F,2.45F,Material.CANVAS,0xFFFFFF);
                    else add(m,"head",-2.65F,-5.55F,-2,5.3F,1.1F,4,Material.CANVAS,0xFFFFFF);
                    add(m,"head",2.7F,-4.75F,-2.3F,.18F,.55F,.3F,red);
                    vest(m,0xFFFFFF);
                    for(float x:new float[]{-2.5F,1.2F})add(m,"body",x,10.6F,-2.48F,1.3F,.8F,.3F,Material.CANVAS,0xD5C39D);
                    // Woven creel at the hip; alternating bands keep it legible at this scale.
                    add(m,"body",2.75F,10.3F,.8F,1.5F,1.8F,1.9F,Material.LEATHER,0xFFFFFF);
                    for(int i=0;i<3;i++)add(m,"body",2.72F,10.35F+i*.55F,.77F,1.56F,.25F,1.96F,Material.STRAW,0xC9B68E);
                }
                case CHEF -> {
                    // The base body's top is y=9: the jacket must enclose it, not share that plane.
                    add(m,"body",-3.1F,8.86F,-2.24F,6.2F,3.79F,4.48F,white);
                    add(m,"body",-2.15F,10,-2.46F,4.3F,2.75F,.24F,0xE8E2D5);
                    add(m,"body",-1,11.1F,-2.66F,2,.8F,.22F,white);
                    add(m,"body",-2.6F,9.05F,-2.43F,5.2F,.35F,.25F,red);
                    add(m,"body",-.35F,9.35F,-2.48F,.7F,.8F,.2F,red);sleeves(m,white);
                    // Jug base top is -9.21: seat the toque directly on it, leaving the jug exposed.
                    chefHat(m,jug?-9.18F:-4.3F,white);
                }
                case MECHANIC -> {
                    add(m,"body",-3.1F,8.86F,-2.24F,6.2F,3.84F,4.48F,navy);sleeves(m,navy);cuffs(m,navy);
                    add(m,"body",-.12F,9.2F,-2.35F,.24F,2.3F,.15F,Material.METAL,metal);
                    add(m,"body",1.2F,9.8F,-2.4F,1.3F,.8F,.2F,blue);
                    add(m,"body",-3.18F,11.7F,-2.32F,6.36F,.5F,4.64F,Material.LEATHER,0xFFFFFF);
                    add(m,"body",-.45F,11.75F,-2.57F,.9F,.4F,.25F,Material.METAL,0xCE9960);
                    // Exposed wrench on the belt, no goggles or eye coverings.
                    add(m,"body",-2.45F,10.8F,-2.65F,.25F,1.7F,.2F,Material.METAL,metal);
                    add(m,"body",-2.7F,10.5F,-2.65F,.75F,.3F,.2F,Material.METAL,metal);
                    ring(m,-4.45F,3,2.5F,.2F,jug?.9F:0,Material.CLOTH,navy);
                    if(jug)crown(m,-4.45F,2.75F,2.4F,Material.CLOTH,navy);
                    else add(m,"head",-2.5F,-5.1F,-1.9F,5,.65F,3.8F,navy);
                    add(m,"head",-2,-4.38F,-3.45F,4,.18F,1.1F,blue);
                }
                case PORTER -> {
                    vest(m,0x91BC8B);cuffs(m,brown);
                    ring(m,-4.45F,3,2.5F,.2F,jug?.9F:0,Material.CLOTH,green);
                    if(jug)crown(m,-4.45F,2.75F,2.4F,Material.CLOTH,green);
                    else add(m,"head",-2.5F,-5.2F,-1.9F,5,.75F,3.8F,green);
                    add(m,"head",-2,-4.4F,-3.7F,4,.2F,1.3F,green);
                    add(m,"body",-2.55F,9.3F,2.25F,5.1F,3.5F,2.1F,Material.CANVAS,0xD2B990);
                    add(m,"body",-2.65F,9.25F,2.3F,5.3F,.9F,2.2F,Material.LEATHER,0xFFFFFF);
                    add(m,"body",-1.2F,10.55F,4.36F,2.4F,1.55F,.45F,Material.CANVAS,0xFFFFFF);
                    for(float x:new float[]{-2,1.5F})add(m,"body",x,9,-2.45F,.5F,3,.22F,Material.LEATHER,0xFFFFFF);
                    add(m,"body",-.3F,9.7F,4.52F,.6F,.65F,.12F,Material.METAL,metal);
                }
                case BAKER -> {
                    jacket(m,0xDFD4BC);sleeves(m,0xDFD4BC);apron(m,white);
                    hat(m,jug,3.15F,2.7F,Material.CLOTH,white);
                    if(!jug)add(m,"head",-2.5F,-5.55F,-1.95F,5,.45F,3.9F,white);
                    // Soft square mitts follow the hands, leaving held tools in the same grip.
                    for(String arm:List.of("left_arm","right_arm")){
                        add(m,arm,-.65F,1.95F,-.67F,1.3F,1.0F,1.34F,Material.CANVAS,0xE2BB82);
                        add(m,arm,-.68F,1.85F,-.70F,1.36F,.25F,1.4F,red);
                    }
                    add(m,"body",1.4F,10.65F,-2.64F,.65F,.4F,.1F,0xFFFFFF);
                }
                case RANCH_HAND -> {
                    vest(m,0xA48360);cuffs(m,brown);
                    hat(m,jug,3.8F,3.15F,Material.LEATHER,0xFFFFFF);
                    ring(m,-4.7F,2.9F,2.45F,.18F,jug?.86F:0,Material.CLOTH,0xC89455);
                    add(m,"body",-2.7F,9.04F,-2.43F,5.4F,.32F,.22F,red);
                    add(m,"body",-.75F,9.33F,-2.46F,1.5F,.55F,.22F,red);
                    add(m,"body",-.35F,9.86F,-2.46F,.7F,.38F,.22F,red);
                    add(m,"body",1.45F,10.2F,-2.54F,.5F,.5F,.16F,Material.METAL,0xD6B661);
                }
                case BEEKEEPER -> {
                    jacket(m,0xE8DABB);sleeves(m,0xE8DABB);cuffs(m,0xE8DABB);
                    hat(m,jug,3.6F,3.15F,Material.CANVAS,0xFFF2C9);
                    // Closed fabric sides/back meet the brim; only the front has a metal veil.
                    for(float x:new float[]{-3.35F,3.1F})add(m,"head",x,-4.25F,-3.35F,.25F,4.35F,6.1F,0xE8DABB);
                    add(m,"head",-3.35F,-4.25F,2.75F,6.7F,4.35F,.25F,0xE8DABB);
                    add(m,"head",-3.36F,-4.28F,-3.56F,6.72F,.19F,.24F,Material.METAL,0x6B7473);
                    add(m,"head",-3.36F,-.08F,-3.56F,6.72F,.2F,.24F,Material.METAL,0x6B7473);
                    for(float x:new float[]{-3.36F,3.2F})add(m,"head",x,-4.09F,-3.56F,.16F,4.01F,.24F,Material.METAL,0x6B7473);
                    // Offset crossing wires in depth so the mesh cannot fight with itself.
                    for(float x:new float[]{-2.55F,-.45F,.45F,2.55F})
                        add(m,"head",x,-4.09F,-3.42F,.07F,4.01F,.06F,Material.METAL,0x6B7473);
                    add(m,"head",-3.2F,-1.85F,-3.48F,6.4F,.07F,.05F,Material.METAL,0x6B7473);
                    add(m,"head",-3.35F,.1F,-3.5F,6.7F,.22F,6.5F,0xE8DABB);
                    for(String arm:List.of("left_arm","right_arm"))add(m,arm,-.56F,2.0F,-.59F,1.12F,.85F,1.18F,0xE8DABB);
                    add(m,"body",-.12F,9.2F,-2.36F,.24F,3.05F,.15F,0x97846C);
                }
                case RUNNER -> {
                    jacket(m,0xBB443D);sleeves(m,0xBB443D);
                    add(m,"body",-3.14F,11.6F,-2.29F,6.28F,1.1F,4.58F,navy);cuffs(m,navy);
                    for(float x:new float[]{-2.5F,2.05F})add(m,"body",x,9.2F,-2.4F,.45F,2.4F,.14F,white);
                    // Fit the raised brow contour rather than hovering over the wider lower head.
                    add(m,"head",-2.77F,-3.8F,-2.62F,5.54F,.4F,.35F,white);
                    add(m,"head",-2.77F,-3.8F,1.06F,5.54F,.4F,.24F,white);
                    for(float x:new float[]{-2.77F,2.57F})add(m,"head",x,-3.8F,-2.27F,.2F,.4F,3.33F,white);
                    add(m,"head",-2.95F,-3.78F,.68F,.5F,.3F,.66F,red);
                    add(m,"head",-3.0F,-3.52F,.8F,.35F,.6F,.25F,white);
                    add(m,"body",-.9F,9.6F,-2.42F,1.8F,1.2F,.18F,white);
                    add(m,"body",-.13F,9.8F,-2.64F,.26F,.8F,.12F,navy);
                }
                case ALCHEMIST -> {
                    jacket(m,0x68508B);sleeves(m,0x68508B);
                    add(m,"body",-3.16F,12.0F,-2.3F,6.32F,.8F,4.6F,0x594276);
                    add(m,"body",-3.18F,11.35F,-2.34F,6.36F,.4F,4.68F,Material.LEATHER,0xFFFFFF);
                    hat(m,jug,3.25F,2.8F,Material.CLOTH,0x68508B);
                    // Goggles rest on the hat above the eyes; the jug keeps its neck opening.
                    for(float x:new float[]{-1.8F,.6F}){
                        add(m,"head",x,-4.87F,-2.85F,1.2F,.55F,.35F,Material.METAL,0xC99F64);
                        add(m,"head",x+.13F,-4.77F,-2.91F,.94F,.34F,.1F,0x8DC5BB);
                    }
                    add(m,"head",-.6F,-4.65F,-2.82F,1.2F,.15F,.25F,Material.LEATHER,0xFFFFFF);
                    for(int i=0;i<3;i++){
                        float x=-1.65F+i*1.3F;
                        add(m,"body",x,11.0F,-2.85F,.65F,1.0F,.55F,i==0?0x9DC8BC:i==1?0xD0A15C:0xB6A0CF);
                        add(m,"body",x+.16F,10.76F,-2.74F,.33F,.25F,.33F,Material.LEATHER,0xFFFFFF);
                    }
                }
                case ORCHARD_KEEPER -> {
                    vest(m,0x98B376);apron(m,0xE6D3A6);
                    hat(m,jug,3.25F,2.75F,Material.CLOTH,0x64834E);
                    add(m,"head",-2.2F,-4.38F,-3.5F,4.4F,.19F,1.1F,0x64834E);
                    add(m,"body",2.85F,10.2F,.7F,1.55F,2.1F,2.05F,Material.STRAW,0xDCC499);
                    // Top/bottom bands extend past the wicker's y=10.2/12.3 planes.
                    for(float y:new float[]{10.14F,11.15F,12.16F})add(m,"body",2.81F,y,.66F,1.63F,.2F,2.13F,Material.LEATHER,0xFFFFFF);
                    add(m,"body",3.15F,9.85F,1.15F,.9F,.55F,.9F,red);
                    add(m,"body",3.5F,9.6F,1.5F,.15F,.28F,.15F,Material.LEATHER,0xFFFFFF);
                }
                case GROUNDSKEEPER -> {
                    jacket(m,0x55725B);sleeves(m,0x55725B);
                    add(m,"body",-3.16F,12.0F,-2.3F,6.32F,.8F,4.6F,0x4B6851);
                    hat(m,jug,3.6F,3.15F,Material.CLOTH,0xD5B449);
                    add(m,"body",-.15F,9.2F,-2.37F,.3F,3.1F,.18F,0xD5B449);
                    for(float x:new float[]{-2.5F,1.15F})add(m,"body",x,10.75F,-2.45F,1.35F,.75F,.23F,0x45624C);
                    for(String leg:List.of("left_leg","right_leg")){
                        add(m,leg,-.51F,-.24F,-.55F,1.02F,1.12F,1.1F,0x514A35);
                        add(m,leg,-.54F,.65F,-.83F,1.08F,.45F,1.4F,0x514A35);
                    }
                }
                default -> {}
            }
        }
        return result;
    }
    private static void add(Map<String,List<Piece>> m,String bone,float x,float y,float z,float w,float h,float d,int color){add(m,bone,x,y,z,w,h,d,Material.CLOTH,color);}
    private static void add(Map<String,List<Piece>> m,String bone,float x,float y,float z,float w,float h,float d,Material material,int color){
        m.computeIfAbsent(bone,b->new ArrayList<>()).add(new Piece(material,new Box(x,y,z,w,h,d,color)));
    }
    private static void sleeves(Map<String,List<Piece>> m,int color){for(String side:List.of("left_arm","right_arm"))add(m,side,-.62F,-.2F,-.65F,1.24F,1,1.3F,color);}
    private static void cuffs(Map<String,List<Piece>> m,int color){for(String side:List.of("left_leg","right_leg"))add(m,side,-.47F,-.18F,-.5F,.94F,.6F,1,color);}
    private static void jacket(Map<String,List<Piece>> m,int color){add(m,"body",-3.1F,8.86F,-2.24F,6.2F,3.84F,4.48F,color);}
    private static void apron(Map<String,List<Piece>> m,int color){
        add(m,"body",-2.2F,9.7F,-2.5F,4.4F,3.1F,.22F,Material.CANVAS,color);
        add(m,"body",-.95F,11.05F,-2.74F,1.9F,.75F,.18F,Material.CANVAS,0xFFFFFF);
        for(float x:new float[]{-1.8F,1.4F})add(m,"body",x,8.94F,-2.42F,.4F,.9F,.18F,color);
    }
    private static void hat(Map<String,List<Piece>> m,boolean jug,float x,float z,Material material,int color){
        ring(m,-4.45F,x,z,.2F,jug?.9F:0,material,color);
        if(jug)crown(m,-4.45F,2.8F,2.4F,material,color);
        else{
            add(m,"head",-2.65F,-5.1F,-2.05F,5.3F,.65F,4.1F,material,color);
            add(m,"head",-2.35F,-5.4F,-1.8F,4.7F,.3F,3.6F,material,color);
        }
    }
    private static void vest(Map<String,List<Piece>> m,int color){
        add(m,"body",-3.1F,9.1F,2.14F,6.2F,3.4F,.23F,Material.CANVAS,color);
        for(float x:new float[]{-3.1F,1.05F})add(m,"body",x,9.1F,-2.27F,2.05F,3.4F,.25F,Material.CANVAS,color);
        for(float x:new float[]{-3.12F,2.9F})add(m,"body",x,9.1F,-2.12F,.22F,3.4F,4.26F,Material.CANVAS,color);
        add(m,"body",-3.15F,12.2F,-2.3F,6.3F,.3F,4.6F,Material.LEATHER,0xFFFFFF);
    }
    private static void ring(Map<String,List<Piece>> m,float y,float x,float z,float height,float hole,Material material,int color){
        if(hole==0){add(m,"head",-x,y,-z,x*2,height,z*2,material,color);return;}
        add(m,"head",-x,y,-z,x-hole,height,z*2,material,color);add(m,"head",hole,y,-z,x-hole,height,z*2,material,color);
        add(m,"head",-hole,y,-z,hole*2,height,z-hole,material,color);add(m,"head",-hole,y,hole,hole*2,height,z-hole,material,color);
    }
    /** Three shallow crown tiers stay below the jug shoulder at y=-5.28, around its thin neck only. */
    private static void crown(Map<String,List<Piece>> m,float brim,float x,float z,Material material,int color){
        ring(m,brim-.32F,x,z,.32F,.9F,material,color);
        ring(m,brim-.54F,x-.05F,z-.03F,.22F,.9F,material,color);
        ring(m,brim-.68F,x-.1F,z-.06F,.14F,.9F,material,color);
    }
    /** Square band, straight sides and two stepped crown layers in the same cuboid style as the mob. */
    private static void chefHat(Map<String,List<Piece>> m,float bottom,int color){
        add(m,"head",-2.15F,bottom-.85F,-1.65F,4.3F,.85F,3.3F,color);
        add(m,"head",-2.45F,bottom-2.2F,-1.9F,4.9F,1.35F,3.8F,color);
        add(m,"head",-3.1F,bottom-3.55F,-2.4F,6.2F,1.35F,4.8F,color);
        add(m,"head",-2.65F,bottom-4.12F,-2.05F,5.3F,.57F,4.1F,color);
    }
    private static Face face(float[] v){
        float ax=v[3]-v[0],ay=v[4]-v[1],az=v[5]-v[2],bx=v[6]-v[0],by=v[7]-v[1],bz=v[8]-v[2];
        float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        nx/=length;ny/=length;nz/=length;
        for(int i=0;i<v.length;i++)v[i]/=16;
        return new Face(v,nx,ny,nz,Math.clamp(.81F-ny*.17F-nz*.05F,.64F,1F));
    }
    /** Group once by animated bone/material; no mesh building or material filtering per frame. */
    private static Map<Type,Map<String,Map<Material,List<Box>>>> batch(Map<Type,Map<String,List<Piece>>> source){
        var result=new EnumMap<Type,Map<String,Map<Material,List<Box>>>>(Type.class);
        source.forEach((type,parts)->{
            var bones=new LinkedHashMap<String,Map<Material,List<Box>>>();result.put(type,bones);
            parts.forEach((bone,pieces)->{
                var groups=new EnumMap<Material,List<Box>>(Material.class);bones.put(bone,groups);
                for(var piece:pieces)groups.computeIfAbsent(piece.material,k->new ArrayList<>()).add(piece.shape);
            });
        });
        return result;
    }
    private static void drawFace(PoseStack.Pose p,VertexConsumer c,Face face,int color,Material material,int light){
        float shade=face.shade;var v=face.vertices;
        int tint=0xFF000000|((int)((color>>16&255)*shade)<<16)|((int)((color>>8&255)*shade)<<8)|(int)((color&255)*shade);
        for(int i=0;i<4;i++)c.addVertex(p,v[i*3],v[i*3+1],v[i*3+2]).setColor(tint)
            .setUv(i==0 || i==3?material.u0:material.u1,i<2?material.v0:material.v1)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p,face.nx,face.ny,face.nz);
    }
}
