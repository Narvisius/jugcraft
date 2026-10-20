package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A wreath on one physical face of a door. That face can lie inside the door's block cell when it swings. */
public final class DoorWreath extends BlockAttachedEntity {
	private static final EntityDataAccessor<BlockPos> DOOR = SynchedEntityData.defineId(DoorWreath.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Boolean> FRONT = SynchedEntityData.defineId(DoorWreath.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<BlockState> WREATH = SynchedEntityData.defineId(DoorWreath.class, EntityDataSerializers.BLOCK_STATE);
	public static final double THICKNESS = 4.5 / 16.0;
	private Direction facing = Direction.NORTH;

	public DoorWreath(EntityType<? extends DoorWreath> type, Level level) {
		super(type, level, BlockPos.ZERO);
		noPhysics = true;
	}

	@Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DOOR, BlockPos.ZERO).define(FRONT, true).define(WREATH, Blocks.AIR.defaultBlockState());
	}

	/** Front means the face looking opposite FACING when closed; opening rotates that same material face. */
	public static Direction frontFace(BlockState door) {
		Direction face = door.getValue(DoorBlock.FACING).getOpposite();
		return !door.getValue(DoorBlock.OPEN) ? face : door.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT
				? face.getCounterClockWise() : face.getClockWise();
	}

	public boolean isFront() { return entityData.get(FRONT); }
	@Override public BlockPos getPos() { return entityData.get(DOOR); }
	public BlockState wreathState() { return entityData.get(WREATH); }
	@Override public Direction getDirection() { return facing; }

	/** No blocks are moved or replaced, and each half/face has room for only one wreath. */
	public static @Nullable DoorWreath attach(ServerLevel level, BlockPos door, boolean front, BlockState wreath) {
		if (!(level.getBlockState(door).getBlock() instanceof DoorBlock) || !(wreath.getBlock() instanceof AutumnWreathBlock)) return null;
		if (!level.getEntitiesOfClass(DoorWreath.class, new AABB(door).inflate(1),
				e -> e.getPos().equals(door) && e.isFront() == front).isEmpty()) return null;
		DoorWreath entity = new DoorWreath(JugcraftAgriculture.DOOR_WREATH, level);
		entity.entityData.set(DOOR, door.immutable());
		entity.entityData.set(FRONT, front);
		entity.entityData.set(WREATH, wreath);
		entity.recalculateBoundingBox();
		return entity.survives() && level.addFreshEntity(entity) ? entity : null;
	}

	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
		super.onSyncedDataUpdated(data);
		if (data == DOOR || data == FRONT || data == WREATH) recalculateBoundingBox();
	}

	/** Position packets contain the ring's centre, not the anchor. Keep the separately synced door position. */
	@Override public void setPos(double x, double y, double z) {
		setPosRaw(x, y, z);
		if (entityData != null) recalculateBoundingBox();
	}

	@Override protected void recalculateBoundingBox() {
		if (!(wreathState().getBlock() instanceof AutumnWreathBlock) || !level().hasChunkAt(getPos())) return;
		pos = getPos();
		BlockState door = level().getBlockState(pos);
		if (!(door.getBlock() instanceof DoorBlock)) return;
		AABB panel = door.getShape(level(), pos).bounds().move(pos);
		Direction face = isFront() ? frontFace(door) : frontFace(door).getOpposite();
		facing = face;
		double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
		switch (face) {
			case NORTH -> z = panel.minZ - THICKNESS / 2;
			case SOUTH -> z = panel.maxZ + THICKNESS / 2;
			case WEST -> x = panel.minX - THICKNESS / 2;
			case EAST -> x = panel.maxX + THICKNESS / 2;
			default -> throw new IllegalStateException("Vertical door face");
		}
		double halfX = face.getAxis() == Direction.Axis.X ? THICKNESS / 2 : 7.0 / 16.0;
		double halfZ = face.getAxis() == Direction.Axis.Z ? THICKNESS / 2 : 7.0 / 16.0;
		setPosRaw(x, pos.getY() + 0.5, z);
		double bottom = wreathState().getBlock() instanceof HollyWreathBlock ? 1.0 : 0.5;
		setBoundingBox(new AABB(x - halfX, pos.getY() + bottom / 16.0, z - halfZ,
				x + halfX, pos.getY() + 15.0 / 16.0, z + halfZ));
	}

	@Override public boolean survives() {
		return wreathState().getBlock() instanceof AutumnWreathBlock && level().getBlockState(getPos()).getBlock() instanceof DoorBlock
				&& level().noBlockCollision(this, getBoundingBox().deflate(1.0E-6)) && level().noBorderCollision(this, getBoundingBox());
	}

	@Override public void tick() {
		// Door block updates drive both sides directly, so the wreath snaps with the vanilla door without interpolation.
		recalculateBoundingBox();
		if (level() instanceof ServerLevel server && server.hasChunkAt(getPos()) && !isRemoved() && !survives()) {
			discard();
			dropItem(server, null);
		}
	}

	@Override public ItemStack getPickResult() {
		ItemStack stack = new ItemStack(wreathState().getBlock());
		if (wreathState().getBlock() instanceof AutumnWreathBlock && !(wreathState().getBlock() instanceof HollyWreathBlock))
			stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(AutumnWreathBlock.FLOWERS, wreathState()));
		return stack;
	}

	@Override public void dropItem(ServerLevel level, @Nullable Entity breaker) {
		level.playSound(null, getPos(), SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
		if (level.getGameRules().get(GameRules.ENTITY_DROPS) && !(breaker instanceof Player player && player.hasInfiniteMaterials()))
			spawnAtLocation(level, getPickResult());
	}

	@Override public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (!level().mayInteract(player, getPos())) return InteractionResult.FAIL;
		ItemStack stack = player.getItemInHand(hand);
		var mums = AutumnWreathBlock.Mums.of(stack);
		if (!(wreathState().getBlock() instanceof HollyWreathBlock) && mums != null) {
			if (mums == wreathState().getValue(AutumnWreathBlock.FLOWERS)) return InteractionResult.PASS;
			if (!level().isClientSide()) {
				entityData.set(WREATH, wreathState().setValue(AutumnWreathBlock.FLOWERS, mums));
				stack.consume(1, player);
				level().playSound(null, getPos(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
				level().gameEvent(player, GameEvent.BLOCK_CHANGE, getPos());
			}
			return InteractionResult.SUCCESS;
		}
		// A wreath must not prevent using its door. Sneaking keeps decoration/item placement available.
		BlockState door = level().getBlockState(getPos());
		if (!player.isSecondaryUseActive() && door.getBlock() instanceof DoorBlock block && block.type().canOpenByHand()) {
			if (!level().isClientSide()) block.setOpen(player, level(), door, getPos(), !door.getValue(DoorBlock.OPEN));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("front", isFront());
		output.store("wreath", BlockState.CODEC, wreathState());
	}

	@Override protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DOOR, pos);
		entityData.set(FRONT, input.getBooleanOr("front", true));
		entityData.set(WREATH, input.read("wreath", BlockState.CODEC).filter(s -> s.getBlock() instanceof AutumnWreathBlock)
				.orElse(JugcraftAgriculture.block("holly_wreath").defaultBlockState()));
		recalculateBoundingBox();
	}
}
