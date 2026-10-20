package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Native model data recovered from the owner's October 19 exports, without UV reinterpretation. */
public final class RecoveredWildlifeModel extends EntityModel<RecoveredWildlifeRenderer.State> {
    private final String name;
    private final Map<String, ModelPart> parts;
    private final Map<String,String> parents=new HashMap<>();
    private final Map<String, KeyframeAnimation> actions = new HashMap<>();
    private final ModelPart look;
    private final JsonArray motions;
    private final JsonArray expressions;
    private final JsonArray poses;

    public static JsonObject read(String name) {
        try (var reader = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(Jugcraft.id(name.startsWith("recovered/")
                        ? "recovered_creatures/" + name.substring(10) + ".json"
                        : "wildlife_models/" + name + ".json")).openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read supplied wildlife model " + name, exception);
        }
    }

    public RecoveredWildlifeModel(String name) { this(name, read(name)); }

    private RecoveredWildlifeModel(String name, JsonObject data) {
        super(bake(data), Set.of("jellyfish","recovered/beasts_modelspectre","recovered/beasts_modelcosmicjelly","recovered/caves_gummybearmodel").contains(name)
                ? net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucent
                : net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        this.name = name;
        motions = data.has("motions") ? data.getAsJsonArray("motions") : new JsonArray();
        expressions = data.has("expressions") ? data.getAsJsonArray("expressions") : new JsonArray();
        poses = data.has("poses") ? data.getAsJsonArray("poses") : new JsonArray();
        parts = new HashMap<>();
        var lookup = root.createPartLookup();
        for (var element : data.getAsJsonArray("parts")) {
            String bone = element.getAsJsonObject().get("name").getAsString();
            parts.put(bone, lookup.apply(bone));
            parents.put(bone,element.getAsJsonObject().get("parent").getAsString());
        }
        // Vanilla's lookup reserves "root" for the part on which it is called.
        // The authored animation root is the child named root, not the mesh wrapper.
        String rootName = data.get("animationRoot").getAsString();
        var animationRoot = rootName.equals("root") && root.hasChild("root") ? root.getChild("root") : root;
        if (root.hasChild("root")) parts.put("root", root.getChild("root"));
        look = parts.get(data.get("headLook").getAsString());
        var animationLookup = animationRoot.createPartLookup();
        data.getAsJsonObject("actions").entrySet().forEach(entry -> {
            var definition = animation(entry.getValue().getAsJsonObject());
            var present = new LinkedHashMap<String, List<AnimationChannel>>();
            definition.boneAnimations().forEach((bone, channels) -> {
                if (animationLookup.apply(bone) != null) present.put(bone, channels);
            });
            actions.put(entry.getKey(), new AnimationDefinition(definition.lengthInSeconds(),
                    definition.looping(), present).bake(animationRoot));
        });
    }

    private static float f(JsonArray array, int index) { return array.get(index).getAsFloat(); }

    private static ModelPart bake(JsonObject data) {
        var mesh = new MeshDefinition();
        var definitions = new HashMap<String, PartDefinition>();
        definitions.put("$mesh", mesh.getRoot());
        for (var element : data.getAsJsonArray("parts")) {
            var part = element.getAsJsonObject();
            var cubes = CubeListBuilder.create();
            for (var cubeElement : part.getAsJsonArray("cubes")) {
                var cube = cubeElement.getAsJsonObject();
                var box = cube.getAsJsonArray("box");
                var uv = cube.getAsJsonArray("uv");
                var inflate = cube.getAsJsonArray("inflate");
                cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                        .mirror(cube.get("mirror").getAsBoolean())
                        .addBox(f(box, 0), f(box, 1), f(box, 2), f(box, 3), f(box, 4), f(box, 5),
                                new CubeDeformation(f(inflate, 0), f(inflate, 1), f(inflate, 2)));
            }
            var pose = part.getAsJsonArray("pose");
            var parent = definitions.get(part.get("parent").getAsString());
            String name = part.get("name").getAsString();
            definitions.put(name, parent.addOrReplaceChild(name, cubes,
                    PartPose.offsetAndRotation(f(pose, 0), f(pose, 1), f(pose, 2), f(pose, 3), f(pose, 4), f(pose, 5))));
        }
        var size = data.getAsJsonArray("textureSize");
        var layer=LayerDefinition.create(mesh, size.get(0).getAsInt(), size.get(1).getAsInt());
        if(data.has("vanillaCreeperMinion"))layer=layer.apply(new net.minecraft.client.model.BabyModelTransform(false,9,0,Set.of("head")));
        return layer.bakeRoot();
    }

    private static AnimationDefinition animation(JsonObject clip) {
        var builder = AnimationDefinition.Builder.withLength(clip.get("length").getAsFloat());
        // Several supplied poses are marked looping with a zero-length timeline.
        // Treat them as static poses; modulo zero would otherwise produce NaN transforms.
        if (clip.get("loop").getAsBoolean() && clip.get("length").getAsFloat() > 0) builder.looping();
        for (var element : clip.getAsJsonArray("channels")) {
            var channel = element.getAsJsonObject();
            String target = channel.get("target").getAsString();
            var frames = new ArrayList<Keyframe>();
            for (var frameElement : channel.getAsJsonArray("frames")) {
                var frame = frameElement.getAsJsonObject();
                var v = frame.getAsJsonArray("vector");
                var vector = switch (target) {
                    case "POSITION" -> KeyframeAnimations.posVec(f(v, 0), f(v, 1), f(v, 2));
                    case "ROTATION" -> KeyframeAnimations.degreeVec(f(v, 0), f(v, 1), f(v, 2));
                    case "SCALE" -> KeyframeAnimations.scaleVec(f(v, 0), f(v, 1), f(v, 2));
                    default -> throw new IllegalArgumentException(target);
                };
                frames.add(new Keyframe(frame.get("time").getAsFloat(), vector,
                        frame.get("interpolation").getAsString().equals("CATMULLROM")
                                ? AnimationChannel.Interpolations.CATMULLROM : AnimationChannel.Interpolations.LINEAR));
            }
            var destination = switch (target) {
                case "POSITION" -> AnimationChannel.Targets.POSITION;
                case "ROTATION" -> AnimationChannel.Targets.ROTATION;
                case "SCALE" -> AnimationChannel.Targets.SCALE;
                default -> throw new IllegalArgumentException(target);
            };
            builder.addAnimation(channel.get("bone").getAsString(),
                    new AnimationChannel(destination, frames.toArray(Keyframe[]::new)));
        }
        return builder.build();
    }

    private void visible(String name, boolean visible) {
        var part = parts.get(name);
        if (part != null) part.visible = visible;
    }

    public boolean poseHand(com.mojang.blaze3d.vertex.PoseStack pose, boolean left) {
        String bone=null;
        for(String candidate:left?List.of("left_arm","arm_left","leftArm"):List.of("right_arm","arm_right","rightArm"))
            if(parts.containsKey(candidate)){bone=candidate;break;}
        if(bone==null)return false;
        var chain=new ArrayList<ModelPart>();
        for(int i=0;bone!=null&&!bone.equals("$mesh")&&i<64;i++){var part=parts.get(bone);if(part==null)return false;chain.add(part);bone=parents.get(bone);}
        for(int i=chain.size()-1;i>=0;i--)chain.get(i).translateAndRotate(pose);
        return true;
    }

    @Override public void setupAnim(RecoveredWildlifeRenderer.State state) {
        super.setupAnim(state);
        for (var part : allParts()) { part.visible = true; part.skipDraw = false; }
        if (name.startsWith("recovered/")) {
            recovered(state);
            return;
        }
        // These are alternate meshes, not decoration: showing both destroys the face/shell.
        for (String hidden : List.of("asleep", "sleep", "angry", "angrySnout", "head_angry", "snout_angry",
                "shearedBody", "saucyArm", "honeyArm", "berryArm", "snout_berries", "snout_honey",
                "left_arm_berries", "right_arm_berries", "left_arm_honey", "right_arm_honey", "rolled", "mound")) visible(hidden, false);
        for (String saddle : List.of("saddle", "saddleFront", "bridle")) visible(saddle, state.saddled);
        for (String chest : List.of("chest", "chests", "leftChest", "rightChest")) visible(chest, state.chested);
        visible("reinsR", state.saddled && state.ridden);
        visible("reinsL", state.saddled && state.ridden);
        if (name.equals("catfish")) { visible("hat", false); visible("open", false); }
        if (name.equals("rhino")) {
            visible("big_horn", !state.isBaby); visible("small_horn", !state.isBaby); visible("baby_horn", state.isBaby);
        }
        if (name.equals("snake")) visible("tail4", state.skin.contains("rattle"));
        if (name.equals("snail") && state.isBaby && parts.containsKey("eyes")) {
            var eyes = parts.get("eyes"); eyes.xScale = eyes.yScale = eyes.zScale = 1.5F;
        }
        String moving = state.flying ? "fly" : state.isInWater && actions.containsKey("swim") ? "swim" : "walk";
        if (state.aquatic && !state.isInWater && actions.containsKey("flop")) moving = "flop";
        float movement = Mth.clamp(state.walkAnimationSpeed * 4, 0, 1);
        if (state.flying || moving.equals("flop")) movement = 1;
        if (state.aquatic && state.isInWater) movement = Math.max(0.2F, movement);
        long time = (long)(state.ageInTicks * 50);
        if (state.sitting && actions.containsKey("sit")) {
            actions.get("sit").apply(time, 1);
        } else {
            var motion = actions.get(moving);
            var idle = actions.get("idle");
            if (idle != null) idle.apply(time, motion == null ? 1 : 1 - movement);
            if (motion != null) motion.apply(time, movement);
        }
        if (look != null && !state.sitting) {
            look.xRot += Mth.clamp(state.xRot, -35, 35) * Mth.DEG_TO_RAD;
            look.yRot += Mth.clamp(state.yRot, -60, 60) * Mth.DEG_TO_RAD;
        }
    }

    private void recovered(RecoveredWildlifeRenderer.State state) {
        // Equipment geometry is part of several original meshes, but must not
        // appear until the corresponding equipment is actually installed.
        if (name.endsWith("modelelephant")) {
            visible("cabin", state.saddled);
            visible("left_chest", state.chested);
            visible("right_chest", state.chested);
        }
        if (name.endsWith("candicornmodel")) visible("saddle", state.saddled);
        if (name.endsWith("modelgrizzlybear")) {
            visible("hat", state.skin.contains("freddy"));
            visible("microphone", state.skin.contains("freddy"));
        }
        for(var element:poses){
            var pose=element.getAsJsonObject();var part=parts.get(pose.get("bone").getAsString());if(part==null)continue;
            if(pose.has("when") && expression(pose.get("when"),state,0)==0)continue;
            float weight=pose.has("weight")?Mth.clamp(expression(pose.get("weight"),state,0),0,1):switch(pose.get("state").getAsString()){case "sit" -> state.sitProgress;case "swim" -> state.isInWater?1:0;case "fly" -> state.flying?1:0;default -> 0;};
            var v=pose.getAsJsonArray("value");
            if(pose.get("target").getAsString().equals("rotation")){part.xRot+=f(v,0)*weight;part.yRot+=f(v,1)*weight;part.zRot+=f(v,2)*weight;}
            else{part.x+=f(v,0)*weight;part.y+=f(v,1)*weight;part.z+=f(v,2)*weight;}
        }
        float amount = state.sitting ? 0 : Mth.clamp(state.walkAnimationSpeed, 0, 1);
        if (state.flying || state.aquatic && state.isInWater) amount = Math.max(.3F, amount);
        for (var element : motions) {
            var motion = element.getAsJsonObject();
            if (motion.has("when") && expression(motion.get("when"), state, 0) == 0) continue;
            var part = parts.get(motion.get("bone").getAsString());
            if (part == null) continue;
            float clock = expression(motion.get("clock"),state,0);
            float weight = expression(motion.get("amount"),state,0);
            float speed = expression(motion.get("speed"),state,0), degree = expression(motion.get("degree"),state,0);
            boolean invert = motion.get("invert").getAsBoolean();
            String kind = motion.get("kind").getAsString();
            if (kind.equals("bob")) {
                float value = Mth.sin(clock * speed) * degree * weight;
                part.y += invert ? -Math.abs(value) : value - degree * weight;
            } else {
                float value = Mth.cos(clock * speed + expression(motion.get("phase"),state,0)) * degree * weight;
                value += expression(motion.get("bias"),state,0) * weight;
                if(invert)value=-value;
                switch (kind) { case "walk" -> part.xRot += value; case "swing" -> part.yRot += value; case "flap" -> part.zRot += value; }
            }
        }
        for (var element : expressions) {
            var e = element.getAsJsonObject(); var part = parts.get(e.get("bone").getAsString());
            if (part == null) continue;
            if(e.has("when") && expression(e.get("when"),state,0)==0)continue;
            String target = e.get("target").getAsString(), op = e.get("op").getAsString();
            float old = switch (target) { case "xRot" -> part.xRot; case "yRot" -> part.yRot; case "zRot" -> part.zRot; case "xScale" -> part.xScale; case "yScale" -> part.yScale; case "zScale" -> part.zScale; case "x" -> part.x; case "y" -> part.y; default -> part.z; };
            float value = expression(e.get("value"), state, 0);
            if (op.equals("+=")) value += old; else if (op.equals("-=")) value = old - value;
            if (!Float.isFinite(value)) continue;
            switch (target) { case "xRot" -> part.xRot=value; case "yRot" -> part.yRot=value; case "zRot" -> part.zRot=value; case "xScale" -> part.xScale=value; case "yScale" -> part.yScale=value; case "zScale" -> part.zScale=value; case "x" -> part.x=value; case "y" -> part.y=value; case "z" -> part.z=value; }
        }
        var clip = actions.get(state.action);
        if (clip != null && state.actionTime >= 0) clip.apply((long)(state.actionTime * 50), 1);
        RecoveredMutantPoses.apply(name,state,parts);
        if (name.endsWith("modelmurmurhead")) {
            // Keep the source's paired rest rotations together. Applying only Z
            // inverts the hair; the entity-specific lag solver is not bound yet.
            for (String bone : java.util.List.of("backHair", "rightHair", "leftHair")) {
                var hair = parts.get(bone);
                if (hair == null) continue;
                if (state.ageInTicks > 5) {
                    hair.xRot += 174 * Mth.DEG_TO_RAD;
                    hair.zRot -= Mth.PI;
                }
                if (bone.equals("backHair")) hair.xRot += Mth.cos(state.ageInTicks * .05F) * .05F - .05F;
                else hair.zRot += (Mth.cos(state.ageInTicks * .05F + 1) * .05F + .05F) * (bone.equals("leftHair") ? -1 : 1);
            }
        }
        boolean suppliedLook = expressions.asList().stream().anyMatch(e -> {
            String text = e.getAsJsonObject().get("value").toString();
            return text.contains("\"pitch\"") || text.contains("\"yaw\"");
        });
        if (look != null && !state.sitting && !suppliedLook) {
            look.xRot += Mth.clamp(state.xRot, -35, 35) * Mth.DEG_TO_RAD;
            look.yRot += Mth.clamp(state.yRot, -60, 60) * Mth.DEG_TO_RAD;
        }
    }

    private static float expression(JsonElement value, RecoveredWildlifeRenderer.State state, int depth) {
        if (depth > 32) return 0;
        if (value.isJsonPrimitive()) {
            var p=value.getAsJsonPrimitive(); if (p.isNumber()) return p.getAsFloat();
            return switch(p.getAsString()) { case "flap" -> state.flap; case "swoop" -> state.swoop; case "motion_pitch" -> state.motionPitch; case "charge" -> state.charge; case "combat" -> state.combat; case "jump" -> state.jump; case "squish" -> state.squish; case "propeller" -> state.propeller; case "body_yaw" -> state.sourceYaw; case "ground" -> state.onGround ? 1 : 0; case "stand" -> state.onGround && state.isInWater ? 1 : 0; case "sleep" -> state.sleeping ? 1 : 0; case "run" -> state.running ? 1 : 0; case "land" -> state.isInWater ? 0 : 1; case "segment" -> state.segmentIndex; case "walk" -> state.walkAnimationPos; case "move" -> state.sitting ? 0 : state.walkAnimationSpeed; case "age" -> state.ageInTicks; case "pitch" -> state.xRot; case "yaw" -> state.yRot; case "sit" -> state.sitProgress; case "swim" -> state.isInWater ? 1 : 0; case "fly" -> state.flying ? 1 : 0; case "baby" -> state.isBaby ? 1 : 0; case "freddy" -> state.skin.contains("freddy") ? 1 : 0; case "eat" -> state.actionTime >= 0 && (state.action.contains("eat") || state.action.contains("chew")) ? 1 : 0; default -> 0; };
        }
        var a=value.getAsJsonArray();float left=expression(a.get(1),state,depth+1);
        if(a.get(0).getAsString().equals("select"))return expression(a.get(left!=0?2:3),state,depth+1);
        float right=a.size()>2?expression(a.get(2),state,depth+1):0;
        return switch(a.get(0).getAsString()) { case "+" -> left+right; case "-" -> left-right; case "*" -> left*right; case "/" -> right==0?0:left/right; case "cos" -> Mth.cos(left); case "sin" -> Mth.sin(left); case "abs" -> Math.abs(left); case "min" -> Math.min(left,right); case "max" -> Math.max(left,right); case "!" -> left==0?1:0; case "&&" -> left!=0&&right!=0?1:0; case "||" -> left!=0||right!=0?1:0; case "==" -> left==right?1:0; case "!=" -> left!=right?1:0; case ">" -> left>right?1:0; case "<" -> left<right?1:0; case ">=" -> left>=right?1:0; case "<=" -> left<=right?1:0; default -> 0; };
    }
}
