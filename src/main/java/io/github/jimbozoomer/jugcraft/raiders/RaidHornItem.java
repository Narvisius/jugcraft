package io.github.jimbozoomer.jugcraft.raiders;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Raider War Horn (raider extras): made from three officers' insignia, it calls a raid on purpose. Blown in the
 * Overworld with raids on, not in peaceful and with no raid under way, it brings one at the world's raid level against
 * the blower's base (or the town, if they are near it) and is used up. Otherwise it says why not and is kept.
 */
public class RaidHornItem extends Item {
	public RaidHornItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer blower)) {
			return InteractionResult.SUCCESS;
		}
		String refusal = RaiderRaids.enabled() ? server.dimension() != Level.OVERWORLD ? "dimension"
				: server.getDifficulty() == Difficulty.PEACEFUL ? "peaceful" : RaiderRaids.raids(server).isEmpty() ? null : "under_way" : "off";
		if (refusal != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.raid_horn." + refusal));
			return InteractionResult.FAIL;
		}
		RaiderRaids.Raid raid = RaiderRaids.start(server, RaiderRaids.objectiveFor(server, blower), RaiderRaids.raidLevel(server),
				server.getRandom());
		if (raid == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.raid_horn.no_room"));
			return InteractionResult.FAIL;
		}
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 8.0F, 1.0F);
		if (!player.getAbilities().instabuild) {
			player.getItemInHand(hand).shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.raid_horn").withStyle(ChatFormatting.GRAY));
	}
}
