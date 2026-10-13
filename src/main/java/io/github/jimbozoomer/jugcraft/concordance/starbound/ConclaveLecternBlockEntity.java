package io.github.jimbozoomer.jugcraft.concordance.starbound;

import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Projects;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
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
 * A Conclave Lectern (roadmap step 23): who placed it and whether it serves them alone or their party (the shared
 * {@link UseMode} switch). The projects themselves live in the world's record ({@link ConclaveProjects}), never here,
 * so breaking a lectern loses nothing.
 */
public class ConclaveLecternBlockEntity extends BlockEntity {
	private @Nullable UUID owner;
	private UseMode mode = UseMode.PERSONAL;

	public ConclaveLecternBlockEntity(BlockPos pos, BlockState state) {
		super(Starbound.LECTERN_ENTITY, pos, state);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	public UseMode mode() {
		return mode;
	}

	public void setMode(UseMode mode) {
		this.mode = mode;
		setChanged();
	}

	/**
	 * The project this lectern serves: its owner's own (personal), or their party's (party mode, while they are in one);
	 * empty without an owner.
	 */
	public Optional<String> projectOwner() {
		if (owner == null) {
			return Optional.empty();
		}
		if (mode == UseMode.PARTY) {
			Optional<UUID> party = JugcraftParties.partyId(owner);
			if (party.isPresent()) {
				return Optional.of(Projects.communal(party.get()));
			}
		}
		return Optional.of(Projects.personal(owner));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		mode = "party".equals(input.getStringOr("mode", "personal")) ? UseMode.PARTY : UseMode.PERSONAL;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putString("mode", mode.id());
	}
}
