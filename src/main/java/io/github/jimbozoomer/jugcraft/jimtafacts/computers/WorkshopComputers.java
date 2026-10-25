package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.control.DataCableBlock;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Native, bounded workshop computers using the owner's original computer/peripheral art. */
public final class WorkshopComputers {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final String[] DEVICES = {"computer_normal", "computer_advanced", "computer_command", "disk_drive",
            "monitor_normal", "monitor_advanced", "printer", "redstone_relay", "speaker", "turtle_normal", "turtle_advanced",
            "wired_modem", "wired_modem_full", "wireless_modem_normal", "wireless_modem_advanced"};
    public static BlockEntityType<ComputerNode> NODE;
    public static DataComponentType<String> DOCUMENT;
    public static final int MAX_PROGRAM = 8192;
    private WorkshopComputers() {}

    public static void register() {
        DOCUMENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_computer_document"),
                DataComponentType.<String>builder().persistent(Codec.string(0, MAX_PROGRAM))
                        .networkSynchronized(ByteBufCodecs.stringUtf8(MAX_PROGRAM)).build());
        for (String name : DEVICES) block(name, false);
        block("cable", true);
        NODE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_computer"),
                FabricBlockEntityTypeBuilder.create(ComputerNode::new, BLOCKS.entrySet().stream()
                        .filter(e -> !e.getKey().equals("cable")).map(Map.Entry::getValue).toArray(Block[]::new)).build());
        EnergyStorage.SIDED.registerForBlockEntity((node, side) -> node.energy, NODE);
        for (String name : new String[]{"disk", "treasure_disk", "pocket_computer_normal", "pocket_computer_advanced",
                "printed_page", "printed_pages", "printed_book"}) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            var properties = new Item.Properties().setId(key).stacksTo(1);
            if (name.equals("treasure_disk")) properties.component(DOCUMENT, "print JUGCRAFT workshop computer\nprint See Help for the native instruction set.\nstop");
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new DocumentItem(properties, name)));
        }
        ComputerTerminal.register();
        ComputerAudio.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> BLOCKS.values().forEach(out::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> ITEMS.values().forEach(out::accept));
    }

    private static void block(String name, boolean cable) {
        var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_computer_" + name));
        var properties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2).noOcclusion().setId(key);
        Block block = cable ? new DataCableBlock(properties) : name.startsWith("monitor_") ? new ComputerMonitors.MonitorBlock(properties, name) : new ComputerBlock(properties, name);
        BLOCKS.put(name, Registry.register(BuiltInRegistries.BLOCK, key, block));
        var itemKey = ResourceKey.create(Registries.ITEM, key.identifier());
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }

    public static boolean disk(ItemStack stack) { return stack.is(ITEMS.get("disk")) || stack.is(ITEMS.get("treasure_disk")); }
    public static final class DocumentItem extends Item {
        public final String kind;
        DocumentItem(Properties properties, String kind) { super(properties); this.kind = kind; }
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
            if(!kind.startsWith("pocket_computer")||context.getPlayer()==null||!context.getPlayer().isShiftKeyDown()
                    ||!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ComputerNode node)||!node.computer())return InteractionResult.PASS;
            if(context.getPlayer() instanceof ServerPlayer player) {
                if(!node.permitted(player))return InteractionResult.FAIL;
                net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,context.getItemInHand(),tag->{
                    tag.putString("jimta_remote_dimension",player.level().dimension().identifier().toString());tag.putLong("jimta_remote_pos",node.getBlockPos().asLong());
                });
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Pocket terminal linked. Use to connect within 64 blocks; sneak-use air for the offline editor."));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer server) {
                ItemStack stack = player.getItemInHand(hand);
                boolean pocket = kind.startsWith("pocket_computer");
                if(pocket&&!player.isShiftKeyDown()) {
                    var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                    if(data.contains("jimta_remote_pos")) {
                        var pos=net.minecraft.core.BlockPos.of(data.getLongOr("jimta_remote_pos",0));
                        if(!data.getStringOr("jimta_remote_dimension","").equals(server.level().dimension().identifier().toString())
                                ||pos.distSqr(player.blockPosition())>64*64||!server.level().isLoaded(pos)
                                ||!(server.level().getBlockEntity(pos) instanceof ComputerNode target)||!target.computer()||!target.permitted(server)) {
                            server.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Linked computer is out of range, unloaded or protected. Sneak-use for offline editing."));return InteractionResult.FAIL;
                        }
                        server.openMenu(new SimpleMenuProvider((id,inv,who)->ComputerTerminal.Menu.remote(id,inv,target,hand,stack),net.minecraft.network.chat.Component.literal("Remote Workshop Terminal")));
                        return InteractionResult.SUCCESS;
                    }
                }
                server.openMenu(new SimpleMenuProvider((id, inv, who) -> new ComputerTerminal.Menu(id, inv, hand, stack, !pocket),
                        net.minecraft.network.chat.Component.literal(pocket ? "Pocket Workshop Terminal" : "Computer Document")));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
