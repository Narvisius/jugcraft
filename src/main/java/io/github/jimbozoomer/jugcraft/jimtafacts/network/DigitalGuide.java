package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.guide.encyclopedia.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The supplied guide item enters the shared searchable encyclopedia, with native system instructions. */
public final class DigitalGuide extends Item {
    public static Runnable open = () -> {};
    public DigitalGuide(Properties p) { super(p.stacksTo(1)); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) { if (level.isClientSide()) open.run(); return InteractionResult.SUCCESS; }
    public static void write(ServerPlayer player, EncyclopediaRequestPayload request, PageWriter out) {
        out.heading(Component.literal("Start a digital workshop"));
        out.fact(Component.literal("Connect a powered Energy Acceptor, ME Chest with an item cell, and Terminal using fluix cable. Existing Jugcraft generators supply JE. A hand crank or crystal resonance generator can start a small network."));
        out.fact(Component.literal("Search stored items, click to withdraw, right-click for one, or shift-click to transfer a stack. Store Backpack keeps your hotbar and equipped gear. Contents live inside removable cells."));
        out.heading(Component.literal("Grow and automate"));
        out.fact(Component.literal("A Drive holds ten item/fluid cells. The Fluid Terminal exchanges real containers on your cursor. An Interface receives items/fluids from machines. Import/Export Buses face their external inventory; Storage Buses expose that original inventory without copying it."));
        out.fact(Component.literal("Bus top slots hold real filter samples. Bottom slots take capacity, speed, inverter, fuzzy, redstone, crafting or equal-distribution cards. Extra samples remain safe when capacity cards are removed. Signals normally pause a bus; the redstone card runs it only when powered."));
        out.fact(Component.literal("Ordinary cable carries eight channels; dense cable carries 32. A controller-free network supports eight active devices. Colors keep direct cable runs separate; fluix joins colors. Smart bars show actual routed usage."));
        out.fact(Component.literal("Use terminals, buses, compact interfaces/providers/acceptors, monitors, emitters or P2P tunnels on a free cable face. Six faces are available; each attachment keeps its own settings. Covers and anchors occupy their face. Sneak-wrench removes the selected attachment and returns its contents. Existing separate blocks still work."));
        out.fact(Component.literal("Mounted toggle buses connect their outward ME route only with redstone; inverted buses connect without it. Quartz fiber shares power with the facing network while keeping channels/storage separate. A mounted light tunnel turns its cable lamp on at full brightness when light is received; a standalone tunnel retains analog brightness."));
        out.heading(Component.literal("Patterns and processing"));
        out.fact(Component.literal("Encode crafting patterns in the Crafting Terminal. Put them in a Pattern Provider; a Crafting Unit queues requests and a Molecular Assembler repeats one installed recipe. Larger CPU blocks expand queues; connected accelerators increase bounded throughput."));
        out.fact(Component.literal("The Pattern Encoder switches between machine batches, stonecutting and smithing. Copy real held samples into slots; they are templates. Stonecutting uses first input and chosen output. Smithing uses the top row: template, base, addition. Encoding consumes one blank pattern."));
        out.fact(Component.literal("Machine batches deliver real ingredients to the provider's facing machine and wait for its actual outputs. The machine still needs its own recipe/power. Cancelled delivered ingredients stay in that machine. Crafting patterns always recheck current server recipes."));
        out.heading(Component.literal("Partition and travel"));
        out.fact(Component.literal("Cell Workbench samples restrict new item types without deleting existing contents. Configure a View Cell there, then use it on a terminal to filter the display; sneak-use an empty hand to remove it. A View Cell is not a security boundary."));
        out.fact(Component.literal("Filled buckets partition fluid cells in the Cell Workbench. The I/O Port switches between Empty cell and Fill cell for finite item/fluid cells. Supply cells from above and collect completed cells from another face. Filling waits if matching network stock runs out; you can remove a partially filled cell manually."));
        out.fact(Component.literal("A Void Card explicitly destroys excess matching items in a full, partitioned cell. Use it with that cell in the other hand; sneak-use to remove. Energy Cards permanently increase a held powered tool's capacity, at most twice."));
        out.fact(Component.literal("Bind wireless handsets to a terminal with sneak-use. Access Points extend loaded coverage from 64 to 192 blocks with boosters. Portable cells carry both their inventory and charge in the held item."));
        out.fact(Component.literal("P2P Memory Card: sneak-use an input and use a matching output. Item, fluid, JE, redstone and light routes conserve their real source. ME routes join loaded networks in one dimension."));
        out.fact(Component.literal("Quantum storage: eight rings around each core in a 3×3 plane, powered local networks, two entangled singularities. Hold the pair and use one core, then the other. Both ends must stay loaded and permitted. Cross-dimensional operation follows absent-owner server rules. Crafting/power services remain local."));
        out.heading(Component.literal("Spatial building storage"));
        out.fact(Component.literal("Place eight pylons at the corners around a box of static building blocks. Select opposite corners with a spatial cell, insert it in a powered Spatial I/O Port outside the box, then sneak-use the port. The output cell holds the exchanged plot. Use it on an equal-sized box to restore or exchange again."));
        out.fact(Component.literal("2³ and 16³ cells accept supported static vanilla building blocks. Remove all entities first. Machines, containers, fluids and unstable blocks are refused. Cost is 1,000 JE plus 50 JE per block; clear the output slot before each exchange. Copied stale cells cannot retrieve a plot twice."));
        out.fact(Component.literal("A Spatial Anchor keeps its already-loaded workshop chunk active for 100 JE per second. Limits: four per owner and 32 per server. Redstone, missing power or lost authority stops it. After a server restart, visit the workshop once to reactivate; it does not find or generate remote land."));
        out.heading(Component.literal("Production and safeguards"));
        out.fact(Component.literal("Inscriber dies are reusable. Print silicon and processors, then assemble them with redstone. Name presses rename actual input items. The Charger energizes certus, a meteorite compass and powered tools."));
        out.fact(Component.literal("Condenser: 256 material units make a matter ball; 256,000 make a singularity. The component controls credit capacity. Full output pauses production; stored credit is retained. Formation and annihilation planes work on one permitted loaded block and refuse containers/multi-blocks."));
        out.fact(Component.literal("Networks pause when permission, power or loaded routes disappear. Party access respects claims. Oversized graphs fail closed; nobody receives a second copy of stored contents. A Network Tool reports active devices, cells and JE."));
    }
}
