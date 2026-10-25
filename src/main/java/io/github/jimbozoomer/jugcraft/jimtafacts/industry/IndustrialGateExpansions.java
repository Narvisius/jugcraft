package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Removable source expansion chips; their native signals feed the ordinary gate rule editor. */
public final class IndustrialGateExpansions {
    public static final List<String> KINDS = List.of("light_sensor", "timer", "pulsar", "clock", "fader");
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private IndustrialGateExpansions() {}
    public static void register() {
        for (String kind : KINDS) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_gate_expansion_" + kind));
            ITEMS.put(kind, Registry.register(BuiltInRegistries.ITEM, key, new Chip(new Item.Properties().setId(key), kind)));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> ITEMS.values().forEach(out::accept));
    }
    private static final class Chip extends Item {
        private final String kind;
        Chip(Properties p, String kind) { super(p); this.kind = kind; }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof IndustrialAutomation.GateEntity gate)) return InteractionResult.PASS;
            if (!(c.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
            if (player.isSpectator() || !Authority.mayChange(player.level(), player, c.getClickedPos())) return InteractionResult.FAIL;
            int expansion = KINDS.indexOf(kind) + 1;
            if (gate.expansion() == expansion) {
                gate.adjustExpansion(player.isShiftKeyDown());
                player.sendOverlayMessage(Component.literal("Gate " + kind.replace('_', ' ') + ": " + gate.expansionDescription()));
            } else if (gate.expansion() == 0) {
                gate.installExpansion(expansion);
                if (!player.isCreative()) c.getItemInHand().shrink(1);
                player.sendOverlayMessage(Component.literal("Expansion installed. " + gate.expansionDescription() + ". Sneak with empty hand to remove."));
            } else {
                player.sendOverlayMessage(Component.literal("Remove the existing expansion first: sneak with an empty hand."));
                return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                             java.util.function.Consumer<Component> out, TooltipFlag flag) {
            out.accept(Component.literal(kind.equals("fader") ? "Smooths gate sensor readings; missing sensors stay unavailable." : "Adds a local sensor reading on the selected dye channel."));
            out.accept(Component.literal("Use on gate to install; use again to adjust period, sneak-use chip to change channel."));
        }
    }
}
