package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Judging Stand's entry: who entered the hand-carved pumpkin standing on it. It is cleared when that
 * pumpkin goes, so a stand holds one entry at a time and nobody can take over an entered carving.
 */
public class JudgingStandBlockEntity extends BlockEntity {
	private @Nullable UUID entrant;
	private String entrantName = "";

	public JudgingStandBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.JUDGING_STAND_ENTITY, pos, state);
	}

	public Optional<UUID> entrant() {
		return Optional.ofNullable(entrant);
	}

	public String entrantName() {
		return entrantName;
	}

	public void enter(UUID player, String name) {
		entrant = player;
		entrantName = name;
		setChanged();
	}

	public void clear() {
		if (entrant != null) {
			entrant = null;
			entrantName = "";
			setChanged();
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		entrant = input.read("entrant", UUIDUtil.CODEC).orElse(null);
		entrantName = input.getStringOr("entrant_name", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (entrant != null) {
			output.store("entrant", UUIDUtil.CODEC, entrant);
			output.putString("entrant_name", entrantName);
		}
	}
}
