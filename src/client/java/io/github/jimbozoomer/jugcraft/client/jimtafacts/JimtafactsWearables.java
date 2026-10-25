package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.mesh.*;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Original owner-supplied wearable geometry and animation, adapted to the installed Trinkets API.
 * No source mod classes are loaded. The mesh builders preserve source UVs, dimensions and part poses.
 * Every render submission captures its own animation parameters; cached geometry is posed by the
 * collector, so multiple players/slots cannot overwrite each other's pending render state.
 */
public final class JimtafactsWearables {
    private static final List<String> NAMES = List.of(
            "plastic_drinking_hat", "novelty_drinking_hat", "snorkel", "night_vision_goggles", "superstitious_hat",
            "villager_hat", "cowboy_hat", "anglers_hat", "lucky_scarf", "scarf_of_invisibility", "cross_necklace",
            "panic_necklace", "shock_pendant", "flame_pendant", "thorn_pendant", "charm_of_sinking", "charm_of_shrinking",
            "cloud_in_a_bottle", "obsidian_skull", "antidote_vessel", "universal_attractor", "crystal_heart",
            "helium_flamingo", "chorus_totem", "warp_drive", "digging_claws", "feral_claws", "power_glove",
            "fire_gauntlet", "pocket_piston", "vampiric_glove", "golden_hook", "onion_ring", "pickaxe_heater",
            "withered_bracelet", "aqua_dashers", "bunny_hoppers", "kitty_slippers", "running_shoes", "snowshoes",
            "steadfast_spikes", "flippers", "rooted_boots", "strider_shoes", "whoopee_cushion");
    private static final Map<LivingEntity, PistonMotion> PISTONS = new WeakHashMap<>();
    private JimtafactsWearables() {}

    /** Called after common item registration; renderer factories are recreated on resource reload. */
    public static void register() {
        for (String name : NAMES) {
            var item = BuiltInRegistries.ITEM.getValue(Jugcraft.id("jimta_" + name));
            if (item != Items.AIR) TrinketRendererRegistry.registerRenderer(item, context -> new Renderer(name));
        }
    }

    public static List<String> names() { return NAMES; }

    private static boolean glove(String name) {
        return switch (name) {
            case "digging_claws", "feral_claws", "power_glove", "fire_gauntlet", "pocket_piston", "vampiric_glove",
                    "golden_hook", "onion_ring", "pickaxe_heater", "withered_bracelet" -> true;
            default -> false;
        };
    }

    private static boolean translucent(String name) {
        return switch (name) {
            case "snorkel", "scarf_of_invisibility", "cross_necklace", "panic_necklace", "shock_pendant",
                    "flame_pendant", "thorn_pendant", "charm_of_sinking", "charm_of_shrinking",
                    "cloud_in_a_bottle", "crystal_heart" -> true;
            default -> false;
        };
    }

    private static Identifier texture(String name, boolean slim) {
        String path = switch (name) {
            case "fire_gauntlet", "power_glove", "pocket_piston", "vampiric_glove", "golden_hook", "onion_ring",
                    "pickaxe_heater", "withered_bracelet" -> name + "/" + name + (slim ? "_slim" : "_wide");
            case "night_vision_goggles", "warp_drive" -> name + "/" + name;
            default -> name;
        };
        return textureId(path);
    }

    private static Identifier textureId(String path) {
        return Jugcraft.id("textures/entity/jimtafacts/wearable/" + path + ".png");
    }

    private static Identifier glow(String name, boolean slim, float age) {
        return switch (name) {
            case "fire_gauntlet", "pickaxe_heater" -> textureId(name + "/" + name + (slim ? "_slim" : "_wide") + "_overlay");
            case "night_vision_goggles" -> textureId(name + "/" + name + "_overlay");
            case "warp_drive" -> {
                // Source animation: one deterministic 1-in-10 flicker per age tick, one of four authored overlays.
                Random random = new Random((int) age);
                yield random.nextInt(10) == 0 ? textureId("warp_drive/warp_drive_overlay" + random.nextInt(4)) : null;
            }
            default -> null;
        };
    }

    /** Exact original mesh selection/atlas sizes, including slim arms and boots over existing armour. */
    public static LayerDefinition geometry(String name, boolean slim, HumanoidArm arm, boolean bootsCovered) {
        if (glove(name)) {
            MeshDefinition mesh = switch (name) {
                case "digging_claws", "feral_claws" -> ArmsMeshDefinitions.createClaws(slim);
                case "pocket_piston" -> ArmsMeshDefinitions.createPocketPiston(slim);
                case "golden_hook" -> ArmsMeshDefinitions.createGoldenHook(slim);
                case "onion_ring" -> ArmsMeshDefinitions.createBracelet(slim, 4, -4);
                case "pickaxe_heater" -> ArmsMeshDefinitions.createPickaxeHeater(slim);
                case "withered_bracelet" -> ArmsMeshDefinitions.createBracelet(slim, 3, -5);
                default -> ArmsMeshDefinitions.createGlove(slim);
            };
            int width = name.equals("golden_hook") || name.equals("pickaxe_heater") ? 64 : 32;
            int height = name.equals("pocket_piston") || name.endsWith("claws") ? 16 : 32;
            return LayerDefinition.create(ArmsMeshDefinitions.retainArm(mesh, arm), width, height);
        }
        MeshDefinition mesh = switch (name) {
            case "plastic_drinking_hat", "novelty_drinking_hat" -> HeadMeshDefinitions.createDrinkingHat();
            case "snorkel" -> HeadMeshDefinitions.createSnorkel();
            case "night_vision_goggles" -> HeadMeshDefinitions.createNightVisionGoggles();
            case "superstitious_hat" -> HeadMeshDefinitions.createSuperstitiousHat();
            case "villager_hat" -> HeadMeshDefinitions.createBrimmedHat(CubeListBuilder.create());
            case "cowboy_hat" -> HeadMeshDefinitions.createCowboyHat();
            case "anglers_hat" -> HeadMeshDefinitions.createAnglersHat();
            case "lucky_scarf", "scarf_of_invisibility" -> BodyMeshDefinitions.createScarf();
            case "cross_necklace" -> BodyMeshDefinitions.createCrossNecklace();
            case "panic_necklace" -> BodyMeshDefinitions.createPanicNecklace();
            case "shock_pendant", "flame_pendant", "thorn_pendant" -> BodyMeshDefinitions.createPendant();
            case "charm_of_sinking" -> BodyMeshDefinitions.createCharmOfSinking();
            case "charm_of_shrinking" -> BodyMeshDefinitions.createCharmOfShrinking();
            case "cloud_in_a_bottle" -> BeltMeshDefinitions.createCloudInABottle();
            case "obsidian_skull" -> BeltMeshDefinitions.createObsidianSkull();
            case "antidote_vessel" -> BeltMeshDefinitions.createAntidoteVessel();
            case "universal_attractor" -> BeltMeshDefinitions.createUniversalAttractor();
            case "crystal_heart" -> BeltMeshDefinitions.createCrystalHeart();
            case "helium_flamingo" -> BeltMeshDefinitions.createHeliumFlamingo();
            case "chorus_totem" -> BeltMeshDefinitions.createChorusTotem();
            case "warp_drive" -> BeltMeshDefinitions.createWarpDrive();
            case "aqua_dashers" -> LegsMeshDefinitions.createAquaDashers(bootsCovered ? 1.25F : .5F);
            case "bunny_hoppers" -> LegsMeshDefinitions.createBunnyHoppers();
            case "kitty_slippers" -> LegsMeshDefinitions.createKittySlippers();
            case "running_shoes", "rooted_boots", "strider_shoes" -> LegsMeshDefinitions.createBoots(bootsCovered ? 1.25F : .5F);
            case "snowshoes" -> LegsMeshDefinitions.createSnowshoes();
            case "steadfast_spikes" -> LegsMeshDefinitions.createSteadfastSpikes();
            case "flippers" -> LegsMeshDefinitions.createFlippers();
            case "whoopee_cushion" -> HeadMeshDefinitions.createWhoopeeCushion();
            default -> throw new IllegalArgumentException("No recovered wearable geometry: " + name);
        };
        int width = switch (name) {
            case "plastic_drinking_hat", "novelty_drinking_hat", "snorkel", "superstitious_hat", "lucky_scarf",
                    "scarf_of_invisibility", "cross_necklace", "panic_necklace", "shock_pendant", "flame_pendant",
                    "thorn_pendant", "charm_of_sinking", "charm_of_shrinking", "helium_flamingo", "bunny_hoppers",
                    "kitty_slippers", "snowshoes", "steadfast_spikes", "flippers" -> 64;
            default -> 32;
        };
        int height = switch (name) {
            case "cross_necklace", "panic_necklace", "shock_pendant", "flame_pendant", "thorn_pendant",
                    "charm_of_sinking", "charm_of_shrinking" -> 48;
            case "helium_flamingo", "snowshoes", "flippers" -> 64;
            case "whoopee_cushion" -> 16;
            default -> 32;
        };
        return LayerDefinition.create(mesh, width, height);
    }

    private record Variant(boolean slim, HumanoidArm arm, boolean bootsCovered) {}
    private record Frame(HumanoidRenderState state, HumanoidModel<HumanoidRenderState> source, int slot, float piston) {}

    private static final class Renderer implements TrinketRenderer {
        private final String name;
        private final Map<Variant, WornModel> models = new HashMap<>();
        Renderer(String name) { this.name = name; }
        private WornModel model(Variant variant) { return models.computeIfAbsent(variant, v -> new WornModel(name, v)); }

        @Override @SuppressWarnings("unchecked")
        public void submit(ItemStack stack, TrinketSlotAccess access, EntityModel<? extends LivingEntityRenderState> source,
                PoseStack pose, SubmitNodeCollector collector, int light, LivingEntityRenderState state, float yaw, float pitch) {
            LivingEntity wearer = access.inventory().getAttachment().getEntity();
            if (!(source instanceof HumanoidModel<?>) || !(state instanceof HumanoidRenderState humanoid)
                    || WornDisplay.hidden(wearer) || wearer.isSpectator() || state.isInvisible) return;
            boolean slim = state instanceof AvatarRenderState avatar && avatar.skin.model() == PlayerModelType.SLIM;
            HumanoidArm arm = wornArm(access, humanoid.mainArm);
            WornModel model = model(new Variant(slim, arm, !humanoid.feetEquipment.isEmpty()));
            Frame frame = new Frame(humanoid, (HumanoidModel<HumanoidRenderState>) source, access.index(),
                    name.equals("pocket_piston") ? piston(wearer, state.ageInTicks - wearer.tickCount) : 0);
            Identifier texture = texture(name, slim);
            collector.order(0).submitModel(model, frame, pose, wearableRender(name, texture, stack.hasFoil()), light,
                    OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
            Identifier glow = glow(name, slim, state.ageInTicks);
            if (glow != null) collector.order(0).submitModel(model, frame, pose, model.renderType(glow), 0xF000F0,
                    OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
        }

        @Override
        public void submitFirstPersonRightArm(ItemStack stack, TrinketSlotAccess access, EntityModel<? extends LivingEntityRenderState> source,
                ModelPart arm, PoseStack pose, SubmitNodeCollector collector, int light, LocalPlayer player, boolean main) {
            firstPerson(stack, access, arm, pose, collector, light, player, HumanoidArm.RIGHT);
        }

        @Override
        public void submitFirstPersonLeftArm(ItemStack stack, TrinketSlotAccess access, EntityModel<? extends LivingEntityRenderState> source,
                ModelPart arm, PoseStack pose, SubmitNodeCollector collector, int light, LocalPlayer player, boolean main) {
            firstPerson(stack, access, arm, pose, collector, light, player, HumanoidArm.LEFT);
        }

        private void firstPerson(ItemStack stack, TrinketSlotAccess access, ModelPart sourceArm, PoseStack pose,
                SubmitNodeCollector collector, int light, LocalPlayer player, HumanoidArm arm) {
            if (!glove(name) || !TrinketRenderer.renderInFirstPerson() || WornDisplay.hidden(player)
                    || player.isSpectator() || player.isInvisible() || wornArm(access, player.getMainArm()) != arm) return;
            boolean slim = player.getSkin().model() == PlayerModelType.SLIM;
            WornModel model = model(new Variant(slim, arm, false));
            model.resetPose();
            ModelPart part = arm == HumanoidArm.LEFT ? model.wearable.leftArm : model.wearable.rightArm;
            copyTransform(part, sourceArm);
            if (name.equals("pocket_piston")) setPiston(part, piston(player, 0));
            collector.order(0).submitModelPart(part, pose, wearableRender(name, texture(name, slim), stack.hasFoil()), light, OverlayTexture.NO_OVERLAY, null);
            Identifier glow = glow(name, slim, player.tickCount);
            if (glow != null) collector.order(0).submitModelPart(part, pose, model.renderType(glow), 0xF000F0, OverlayTexture.NO_OVERLAY, null);
        }
    }

    private static net.minecraft.client.renderer.rendertype.RenderType wearableRender(String name, Identifier texture, boolean foil) {
        if (translucent(name)) return foil ? RenderTypes.itemTranslucentGlint(texture) : RenderTypes.entityTranslucent(texture);
        return foil ? RenderTypes.armorCutoutNoCullGlint(texture) : RenderTypes.armorCutoutNoCull(texture);
    }

    private static HumanoidArm wornArm(TrinketSlotAccess access, HumanoidArm main) {
        return access.slotType().group().equals("offhand") ? main.getOpposite() : main;
    }

    private static final class WornModel extends Model<Frame> {
        private final String name;
        private final HumanoidModel<HumanoidRenderState> wearable;
        WornModel(String name, Variant variant) {
            this(name, geometry(name, variant.slim, variant.arm, variant.bootsCovered).bakeRoot());
        }
        private WornModel(String name, ModelPart root) {
            super(root, translucent(name) ? RenderTypes::entityTranslucent : RenderTypes::armorCutoutNoCull);
            this.name = name;
            this.wearable = new HumanoidModel<>(root, translucent(name) ? RenderTypes::entityTranslucent : RenderTypes::armorCutoutNoCull);
        }
        @Override public void setupAnim(Frame frame) {
            wearable.setupAnim(frame.state);
            frame.source.setupAnim(frame.state);
            copyTransform(wearable.root(), frame.source.root());
            copyTransform(wearable.head, frame.source.head);
            copyTransform(wearable.hat, frame.source.hat);
            copyTransform(wearable.body, frame.source.body);
            copyTransform(wearable.leftArm, frame.source.leftArm);
            copyTransform(wearable.rightArm, frame.source.rightArm);
            copyTransform(wearable.leftLeg, frame.source.leftLeg);
            copyTransform(wearable.rightLeg, frame.source.rightLeg);
            poseCharm(wearable.body, name, frame.slot);
            if (name.equals("cloud_in_a_bottle")) {
                ModelPart cloud = wearable.body.getChild("charm").getChild("cloud");
                cloud.yRot = frame.state.ageInTicks / 50F;
                cloud.y = (float) Math.cos(frame.state.ageInTicks / 30F) / 2F;
            }
            if ((name.equals("lucky_scarf") || name.equals("scarf_of_invisibility")) && frame.state instanceof AvatarRenderState avatar) {
                wearable.body.getChild("cloak").xRot = wearable.body.xRot + (6F + avatar.capeLean / 2F + avatar.capeFlap) / 180F * (float) Math.PI;
            }
            if (name.equals("pocket_piston")) {
                setPiston(wearable.leftArm, frame.piston);
                setPiston(wearable.rightArm, frame.piston);
            }
        }
    }

    /** Keep the source mesh's authored scale (notably the 0.51 necklace scale) while following the player's pose. */
    private static void copyTransform(ModelPart target, ModelPart source) {
        var authored = target.getInitialPose();
        target.x = source.x; target.y = source.y; target.z = source.z;
        target.xRot = source.xRot; target.yRot = source.yRot; target.zRot = source.zRot;
        target.xScale = authored.xScale() * source.xScale;
        target.yScale = authored.yScale() * source.yScale;
        target.zScale = authored.zScale() * source.zScale;
    }

    private static void poseCharm(ModelPart body, String name, int slot) {
        float[] pose = switch (name) {
            case "obsidian_skull" -> new float[]{4.5F, -4F, -.5F};
            case "antidote_vessel", "chorus_totem" -> new float[]{4F, -3F, -.5F};
            case "universal_attractor" -> new float[]{2.5F, -3F, 0};
            case "cloud_in_a_bottle" -> new float[]{3F, -3F, -.5F};
            case "crystal_heart" -> new float[]{2.5F, -3.01F, 0};
            case "warp_drive" -> new float[]{3.5F, -3F, -.3F};
            default -> null;
        };
        if (pose == null) return;
        ModelPart charm = body.getChild("charm");
        int index = Math.floorMod(slot, 4);
        charm.setPos(index % 2 == 0 ? pose[0] : -pose[0], 9, index < 2 ? pose[1] : -pose[1]);
        charm.yRot = (index < 2 ? 0 : -(float) Math.PI) + ((index % 2 == 0) ^ (index >= 2) ? pose[2] : -pose[2]);
    }

    private static void setPiston(ModelPart arm, float length) {
        if (arm.hasChild("artifact") && arm.getChild("artifact").hasChild("piston_head"))
            arm.getChild("artifact").getChild("piston_head").y = length * 2F;
    }

    /** Source 2-tick extension, 4-tick delay, 2-tick retraction; presentation only, weakly keyed per wearer. */
    private static float piston(LivingEntity wearer, float partial) {
        PistonMotion motion = PISTONS.computeIfAbsent(wearer, ignored -> new PistonMotion());
        if (motion.lastTick != wearer.tickCount) {
            if (wearer.tickCount - motion.lastTick > 1) { motion.length = 0; motion.remaining = 0; }
            motion.length = Math.clamp(motion.length + (motion.remaining < 2 ? -.5F : .5F), 0, 1);
            if (wearer.isSwinging()) motion.remaining = 6;
            if (motion.remaining > 0) --motion.remaining;
            motion.lastTick = wearer.tickCount;
        }
        float delta = Math.clamp(partial, 0, 1);
        return Math.clamp(motion.length + (motion.remaining + delta < 2 ? -.5F : .5F) * delta, 0, 1);
    }
    private static final class PistonMotion { int lastTick = -1; int remaining; float length; }
}
