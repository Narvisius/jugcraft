package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/** Uses owner-authored native layers; the remaining supplied Gecko animals retain their own renderer. */
public final class RecoveredWildlifeRenderer extends MobRenderer<MenagerieAnimal, RecoveredWildlifeRenderer.State, RecoveredWildlifeModel> {
    private final String adult, baby;
    private final JsonObject variants;
    private final float[] sourceScale;
    private final net.minecraft.client.renderer.item.ItemModelResolver itemResolver;
    private final Map<String, RecoveredWildlifeModel> models = new HashMap<>();

    public static final class State extends LivingEntityRenderState {
        String skin, model;
        String action = "";
        float actionTime = -1;
        float sitProgress;
        int segmentIndex;
        int slimeHeads=1;
        long visualSeed;
        float slimeSize=1.2F,sourceHeight,sourceYaw,propeller,charge,combat,jump,squish,flap,swoop,motionPitch;
        Identifier texture, dye;
        com.google.gson.JsonArray skinLayers=new com.google.gson.JsonArray();
        boolean sitting, saddled, chested, ridden, flying, aquatic, powered, running, sleeping, onGround;
        final net.minecraft.client.renderer.item.ItemStackRenderState rightHand=new net.minecraft.client.renderer.item.ItemStackRenderState(),leftHand=new net.minecraft.client.renderer.item.ItemStackRenderState();
    }

    private static JsonObject loadBindings() {
        JsonObject bindings = new JsonObject();
        // Entity types are registered before the first resource-pack reload.
        // Read the fixed type bindings from our own classpath at this stage.
        for (String folder : java.util.List.of("wildlife_models", "recovered_creatures"))
        try (var stream = RecoveredWildlifeRenderer.class.getResourceAsStream("/assets/jugcraft/" + folder + "/bindings.json")) {
            if (stream == null) throw new IllegalStateException("Missing wildlife bindings");
            com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().entrySet()
                    .forEach(e -> bindings.add(e.getKey(), e.getValue()));
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
        return bindings;
    }
    private static final JsonObject BINDINGS = loadBindings();
    public static boolean supports(String id) { return BINDINGS.has(id); }

    public static void register() {
        var bindings = BINDINGS;
        bindings.entrySet().forEach(entry -> {
            var pair = entry.getValue().getAsJsonObject();
            var type = Menagerie.TYPES.get(entry.getKey());
            if (type == null) throw new IllegalStateException("Missing wildlife registration " + entry.getKey());
            EntityRendererRegistry.register(type, context -> new RecoveredWildlifeRenderer(context,
                    pair.get("adult").getAsString(), pair.get("baby").getAsString(),
                    pair.getAsJsonObject("variants"), pair.has("render")?pair.getAsJsonObject("render"):new JsonObject(),
                    Math.min(1.5F, (float)Menagerie.SPECIES.get(entry.getKey()).width() / 2)));
        });
    }

    private RecoveredWildlifeRenderer(EntityRendererProvider.Context context, String adult, String baby, JsonObject variants, JsonObject render, float shadow) {
        super(context, new RecoveredWildlifeModel(adult), shadow);
        this.adult = adult; this.baby = baby;
        this.variants = variants;
        itemResolver=context.getItemModelResolver();
        sourceScale=new float[]{1,1,1};
        if(render.has("scale"))for(int i=0;i<3;i++)sourceScale[i]=render.getAsJsonArray("scale").get(i).getAsFloat();
        if(render.has("glow"))for(var path:render.getAsJsonArray("glow")) {
            var glow=texture(path.getAsString());
            addLayer(new RenderLayer<State,RecoveredWildlifeModel>(this){
                @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,State state,float yaw,float pitch){
                    if(!state.isInvisible)collector.order(1).submitModel(getParentModel(),state,pose,
                        net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(glow),15728880,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor);
                }
            });
        }
        models.put(adult, model);
        addLayer(new RenderLayer<State,RecoveredWildlifeModel>(this){
            @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,State state,float yaw,float pitch){
                if(state.isInvisible)return;
                for(var entry:state.skinLayers){
                    var layer=entry.getAsJsonObject();
                    if(layer.has("frame")&&((int)state.ageInTicks/(state.powered?4:10))%3!=layer.get("frame").getAsInt())continue;
                    if(layer.has("condition")&&state.combat<=0)continue;
                    var texture=texture(layer.get("skin").getAsString());String mode=layer.get("mode").getAsString();
                    var renderType=switch(mode){case "glow" -> net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(texture);case "translucent" -> net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);default -> net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(texture);};
                    int color=layer.has("condition")?((int)(255*state.combat)<<24)|0xffffff:-1;
                    collector.order(2).submitModel(getParentModel(),state,pose,renderType,mode.equals("glow")?15728880:light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,color,null,state.outlineColor);
                }
            }
        });
        if (!baby.equals(adult)) models.put(baby, new RecoveredWildlifeModel(baby));
        variants.entrySet().forEach(entry -> entry.getValue().getAsJsonObject().entrySet().forEach(age -> {
            String key = age.getValue().getAsJsonObject().get("model").getAsString();
            models.computeIfAbsent(key, RecoveredWildlifeModel::new);
        }));
        if (adult.equals("bass")) {
            models.put("mediumbass", new RecoveredWildlifeModel("mediumbass"));
            models.put("largebass", new RecoveredWildlifeModel("largebass"));
        }
        if (adult.equals("blobfishpink")) models.put("blobfishgray", new RecoveredWildlifeModel("blobfishgray"));
        if(adult.equals("recovered/rotten_immortalmodel"))models.put("immortal_overlay",new RecoveredWildlifeModel("recovered/rotten_immortaloverlaymodel"));
        if(adult.equals("recovered/rotten_burnedmodel")||adult.equals("recovered/rotten_immortalmodel"))addLayer(new RenderLayer<State,RecoveredWildlifeModel>(this){
            @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,State state,float yaw,float pitch){
                if(state.isInvisible)return;
                boolean burned=adult.endsWith("burnedmodel");
                if(burned&&state.skin.contains("obsidian")||!burned&&!state.powered)return;
                var overlay=burned?getParentModel():models.get("immortal_overlay");
                var skin=texture(burned?state.skin+"_lava_layer":"menagerie/rotten/entity/immortal/immortal_overlay");
                collector.order(2).submitModel(overlay,state,pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(skin),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor);
            }
        });
        addLayer(new RenderLayer<State, RecoveredWildlifeModel>(this) {
            @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                    State state, float yaw, float pitch) {
                if (state.dye != null && !state.isInvisible) renderColoredCutoutModel(getParentModel(), state.dye,
                        poses, collector, light, state, -1, 1);
            }
        });
        addLayer(new RenderLayer<State,RecoveredWildlifeModel>(this){
            @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,State state,float yaw,float pitch){
                if(state.isInvisible)return;
                getParentModel().setupAnim(state);
                for(boolean left:new boolean[]{false,true}){
                    var item=left?state.leftHand:state.rightHand;if(item.isEmpty())continue;
                    poses.pushPose();
                    if(getParentModel().poseHand(poses,left)){
                        poses.rotateDegrees(com.mojang.math.Axis.XP,-90);poses.rotateDegrees(com.mojang.math.Axis.YP,180);
                        poses.translate((left?-1:1)/16F,.125,-.625);
                        item.submit(poses,collector,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,state.outlineColor);
                    }
                    poses.popPose();
                }
            }
        });
        if (adult.equals("alligator") || adult.equals("firefly")) addLayer(new RenderLayer<State, RecoveredWildlifeModel>(this) {
            @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                    State state, float yaw, float pitch) {
                if (state.isInvisible) return;
                if (adult.equals("alligator")) {
                    var glow = texture("menagerie/wildlife/entity/alligator/alligator" + (state.isBaby ? "_baby" : "") + "_glowmask");
                    collector.order(1).submitModel(getParentModel(), state, poses,
                            net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(glow), light,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
                } else if (!state.isBaby) {
                    int cycle = Math.floorMod((int)state.ageInTicks, 100);
                    int frame = cycle < 30 ? cycle : 0;
                    var uv = new net.minecraft.client.renderer.texture.UvMapping() {
                        public float getU(float u) { return u; }
                        public float getV(float v) { return (v + frame) / 30F; }
                    };
                    collector.order(1).submitModel(getParentModel(), state, poses,
                            net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(texture("menagerie/wildlife/entity/firefly_glow")), light,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, -1, uv, state.outlineColor);
                    collector.order(2).submitModel(getParentModel(), state, poses,
                            net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(texture("menagerie/wildlife/entity/firefly_glow_e")), light,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, -1, uv, state.outlineColor);
                }
            }
        });
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(MenagerieAnimal entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var pair = variants.getAsJsonObject(entity.species().variants().get(entity.variant()).name())
                .getAsJsonObject(state.isBaby ? "baby" : "adult");
        state.skin = pair.get("skin").getAsString();
        state.model = pair.get("model").getAsString();
        state.texture = texture(state.skin);
        state.skinLayers=pair.has("layers")?pair.getAsJsonArray("layers"):new com.google.gson.JsonArray();
        String dye = pair.get("dye").getAsString();
        state.dye = dye.isEmpty() ? null : texture(dye);
        state.sitting = entity.sitting(); state.saddled = entity.saddled(); state.chested = entity.chested();
        state.ridden = entity.isVehicle(); state.flying = (entity.species().flying() || entity.species().id().equals("rotten_scarab") || entity.species().id().equals("fauna_turkey")) && !entity.onGround() && !entity.isInWater();
        state.aquatic = entity.species().aquatic();
        state.powered=entity.level().isThundering();
        state.onGround=entity.onGround();state.running=entity.isSprinting();state.sleeping=entity.isSleeping();
        state.action = entity.recoveredAction();
        state.actionTime = entity.recoveredActionTime(partialTick);
        state.sitProgress = entity.recoveredSitProgress(partialTick);
        state.sourceHeight=entity.getBbHeight();state.sourceYaw=entity.getYRot();state.visualSeed=entity.getUUID().getLeastSignificantBits();
        state.flap=entity.recoveredFlap(partialTick);state.swoop=entity.recoveredSwoop(partialTick);state.motionPitch=-(float)entity.getDeltaMovement().y*57.295776F*(entity.species().source().equals("blue_jay")?2:1);
        state.propeller=entity.recoveredPropeller(partialTick);state.charge=entity.recoveredCharge();state.combat=Math.max(state.charge,net.minecraft.util.Mth.sin(entity.getSwingAnimation(partialTick)*(float)Math.PI));
        state.jump=entity.onGround()?0:net.minecraft.util.Mth.clamp((float)entity.getDeltaMovement().y+0.4F,0,1);state.squish=entity.recoveredSquish();
        if(entity instanceof io.github.jimbozoomer.jugcraft.menagerie.RecoveredFerrouslime slime){state.slimeHeads=slime.heads();state.slimeSize=slime.slimeSize();}
        boolean left=entity.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT;
        itemResolver.updateForLiving(state.rightHand,left?entity.getOffhandItem():entity.getMainHandItem(),net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,entity);
        itemResolver.updateForLiving(state.leftHand,left?entity.getMainHandItem():entity.getOffhandItem(),net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND,entity);
    }

    private static Identifier texture(String path) {
        return path.startsWith("minecraft:")?Identifier.withDefaultNamespace("textures/"+path.substring(10)+".png"):
                Jugcraft.id("textures/" + path + ".png");
    }

    @Override public Identifier getTextureLocation(State state) { return state.texture; }

    @Override protected void scale(State state, PoseStack poses) {
        poses.scale(sourceScale[0],sourceScale[1],sourceScale[2]);
        // Authored baby layers already have infant proportions and size. Only
        // species sharing their adult layer need an additional age transform.
        if (state.isBaby && !state.model.endsWith("baby")) poses.scale(0.5F, 0.5F, 0.5F);
    }

    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        model = models.get(state.model);
        if(state.model.equals("recovered/caves_ferrouslimemodel")){if(!state.isInvisible)RecoveredSlimeVisual.submit(model,state,poses,collector);return;}
        super.submit(state, poses, collector, camera);
    }
}
