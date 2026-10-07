package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinWardrobeBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinWardrobeBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the armour a Coffin Wardrobe holds on its skeleton mannequin: vanilla's own armour, worn by an invisible armour
 * stand the client keeps on the wardrobe's block entity (never added to the world), drawn at {@value #SCALE} scale on
 * the wardrobe's floor so it fits inside, facing out through the glass. As the spawner draws its mob.
 */
public class CoffinWardrobeRenderer implements BlockEntityRenderer<CoffinWardrobeBlockEntity, CoffinWardrobeRenderer.State> {
	public static final float SCALE = 0.8F;
	public static final float FLOOR = 1.0F;
	private static int nextDisplayId = -1_000_000;
	private final EntityRenderDispatcher entities;

	public static final class State extends BlockEntityRenderState {
		@Nullable EntityRenderState stand;
	}

	public CoffinWardrobeRenderer(BlockEntityRendererProvider.Context context) {
		this.entities = context.entityRenderer();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CoffinWardrobeBlockEntity wardrobe, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(wardrobe, state, crumbling);
		state.stand = null;
		BlockState block = wardrobe.getBlockState();
		if (!(block.getBlock() instanceof CoffinWardrobeBlock) || wardrobe.getLevel() == null) {
			return;
		}
		boolean empty = true;
		for (EquipmentSlot slot : CoffinWardrobeBlockEntity.SLOTS) {
			empty &= wardrobe.get(slot).isEmpty();
		}
		if (empty) {
			return;
		}
		BlockPos pos = wardrobe.getBlockPos();
		ArmorStand stand = wardrobe.display instanceof ArmorStand existing ? existing : null;
		if (stand == null || stand.level() != wardrobe.getLevel()) {
			stand = new ArmorStand(wardrobe.getLevel(), pos.getX() + 0.5, pos.getY() + FLOOR / 16, pos.getZ() + 0.5);
			// Never in the world, so it gets no ID from it; drawing it needs one. Negative IDs never meet a real entity's.
			stand.setId(nextDisplayId--);
			stand.setInvisible(true);
			stand.setNoBasePlate(true);
			wardrobe.display = stand;
		}
		for (EquipmentSlot slot : CoffinWardrobeBlockEntity.SLOTS) {
			ItemStack hung = wardrobe.get(slot);
			if (!ItemStack.matches(stand.getItemBySlot(slot), hung)) {
				stand.setItemSlot(slot, hung.copy());
			}
		}
		float yaw = block.getValue(CoffinWardrobeBlock.FACING).toYRot();
		stand.setYRot(yaw);
		stand.yRotO = yaw;
		stand.yBodyRot = yaw;
		stand.yBodyRotO = yaw;
		stand.yHeadRot = yaw;
		stand.yHeadRotO = yaw;
		state.stand = entities.extractEntity(stand, partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.stand == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, FLOOR / 16, 0.5F);
		pose.scale(SCALE, SCALE, SCALE);
		entities.submit(state.stand, camera, 0.0, 0.0, 0.0, pose, collector);
		pose.popPose();
	}
}
