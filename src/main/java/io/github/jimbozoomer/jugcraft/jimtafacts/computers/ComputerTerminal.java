package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Session-bound terminal requests: no coordinates, commands or inventory authority come from the client. */
public final class ComputerTerminal {
    public static MenuType<Menu> TYPE;
    public static final String HELP = "JUGCRAFT Workshop OS — native instructions, one per line\n"
            + "print <text> | wait <ticks 1..1200> | stop | goto <line>\n"
            + "set/add/subtract/multiply/divide <name> <integer or $name> | ifvar <name> <comparison> <value> <line>\n"
            + "readvar <name> <dye> | energyvar/mailvar <name> | countvar <name> <port> <item id>\n"
            + "print/display/write accept $name words. 32 integer registers, values bounded to +/-1 billion.\n"
            + "programs | store/runfile/deletefile <name> — 16 local programs / 65536 characters, saved in the computer.\n"
            + "Programs button: browse, store the editor, load without running, or delete with confirmation.\n"
            + "if <side> <minimum redstone 0..15> <line>\n"
            + "signal <side> <0..15> | read <dye colour>\n"
            + "channel <dye colour> on/off | relay <0..15>\n"
            + "display <text> | beep | printpage/printpages/printbook\n"
            + "colour <dye colour> | waittouch <line> (advanced monitors; sneak-use opens inventory)\n"
            + "waittouch sets touch_x/touch_y registers: cell mode 1..48 / 1..24; plain mode 16 positions per block.\n"
            + "Cell display: cursor <1..48> <1..24> | ink <foreground dye> <background dye> | write <text>\n"
            + "clear | scroll <-24..24> | blit <text> <fore hex> <back hex> (advanced). display returns to plain text.\n"
            + "audio <DFPWM hex bytes>: short 48 kHz clip, <=4096 bytes, +20 JE, once/second, max 4 speakers.\n"
            + "save | load | send <text>\n"
            + "listen <channel> | unlisten <channel/all> | receive | reply <text>\n"
            + "transmit <channel 0..65535> <reply channel> <text> | ifmail <channel> <line>\n"
            + "select <slot 1..16> | pull/push <side> <count 1..64>\n"
            + "ports | items/fluids <port> | moveitems/movefluid <from port> <to port> <amount>\n"
            + "Ports: absolute directions locally, or wired modem x_y_z:direction. Transfer: 64 items / 1000 mB, +10 JE.\n"
            + "Turtle: move <side> | turn left/right | dig/place/attack/till/inspect <side>\n"
            + "selectedvar <name> | slotvar/spacevar/comparevar <name> <slot 1..16>\n"
            + "detectvar/compareblockvar <name> <side> | transferslot <destination slot> <count>\n"
            + "Comparisons write 1 or 0; compare uses the selected item. Slot transfers preserve item data.\n"
            + "equip/unequip left/right (selected slot) | craft (grid in slots 1..9)\n"
            + "Turtle slots 17/18: diamond tools, crafting table, speaker or wireless modem.\n"
            + "Sides: front, back, left, right, up, down. # starts a comment.\n"
            + "1 JE/instruction. Turtle: +20 JE/move/place/till, +30 JE/dig. Tilling wears a fitted hoe.\n"
            + "Use shared JE cables, press Refuel or run refuel: one coal/charcoal gives 2000 JE with space.\n"
            + "Disk: insert here or into a connected disk drive. Printer page: paper + ink.\n"
            + "Each 25x21 page costs paper + ink. Bound pages need string, books also leather. Max 16 pages; output slot 18.\n"
            + "Shared data cables connect sensors, relays and peripherals.\n"
            + "Modems: channel 0 open initially; 16 channels and 16 queued messages, text <=512 chars.\n"
            + "Wireless: 64 blocks; two advanced ends: 256 or cross-dimension while loaded (+10 JE/remote modem).\n"
            + "command <Minecraft command>: Command Computer only; present operator, 100 JE, at most once/second, level 2.\n"
            + "Lua example selects the isolated Lua runtime. Save then Run. Four workers maximum, 64 MB heap each.\nLua: print, sleep, turtle movement/dig/place/select/detect/count, jugcraft.exec/read/write/files/receive.\nLua stops on reload; press Run to restart explicitly. ROM shell, editor, colors, vector, parallel, JSON, virtual files and connected peripherals are available. Terminal offers direct keys or line input. Disks hold one program.lua document. No host files, HTTP or unrestricted external APIs.\n"
            + "Sneak-use pocket on an owned computer to link; use within 64 loaded blocks for remote hardware control.\n"
            + "Sneak-use pocket in air opens its offline program editor.\n"
            + "Owner permission is checked every action; no chunk loading.";
    private ComputerTerminal() {}
    public static void register() {
        TYPE = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("jimta_computer_terminal"), new MenuType<>(Menu::new, FeatureFlags.DEFAULT_FLAGS));
        PayloadTypeRegistry.serverboundPlay().register(Edit.TYPE, Edit.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FileEdit.TYPE, FileEdit.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE, Snapshot.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CanvasSnapshot.TYPE,CanvasSnapshot.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Edit.TYPE, (packet, context) -> {
            var p = context.player();
            if (!(p.containerMenu instanceof Menu m) || m.containerId != packet.menu || !m.stillValid(p)
                    || !RateGate.allow(p, packet.action==10?packet.text.startsWith("char\t")?"jimta_terminal_char":packet.text.startsWith("key_up\t")?"jimta_terminal_up":"jimta_terminal_key":"jimta_terminal", packet.action==10?1:4)) return;
            m.action(p, packet.action, packet.text); m.send(true);
        });
        ServerPlayNetworking.registerGlobalReceiver(FileEdit.TYPE,(packet,context)->{
            var p=context.player();
            if(!(p.containerMenu instanceof Menu m)||m.containerId!=packet.menu||!m.stillValid(p)
                    ||!RateGate.allow(p,"jimta_terminal_files",4))return;
            m.fileAction(packet.action,packet.name,packet.text);m.send(true);
        });
    }
    /** Explicit limits on both filename and source; no client path ever reaches the host filesystem. */
    public record FileEdit(int menu,int action,String name,String text) implements CustomPacketPayload {
        public static final Type<FileEdit> TYPE=new Type<>(Jugcraft.id("jimta_computer_file"));
        public static final StreamCodec<RegistryFriendlyByteBuf,FileEdit> CODEC=new StreamCodec<>(){
            public FileEdit decode(RegistryFriendlyByteBuf b){return new FileEdit(b.readVarInt(),b.readVarInt(),b.readUtf(32),b.readUtf(WorkshopComputers.MAX_PROGRAM));}
            public void encode(RegistryFriendlyByteBuf b,FileEdit p){b.writeVarInt(p.menu);b.writeVarInt(p.action);b.writeUtf(p.name,32);b.writeUtf(p.text,WorkshopComputers.MAX_PROGRAM);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Edit(int menu, int action, String text) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(Jugcraft.id("jimta_computer_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Edit> CODEC = new StreamCodec<>() {
            public Edit decode(RegistryFriendlyByteBuf b) { return new Edit(b.readVarInt(), b.readVarInt(), b.readUtf(WorkshopComputers.MAX_PROGRAM)); }
            public void encode(RegistryFriendlyByteBuf b, Edit p) { b.writeVarInt(p.menu); b.writeVarInt(p.action); b.writeUtf(p.text, WorkshopComputers.MAX_PROGRAM); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record CanvasSnapshot(int menu,String text,String foreground,String background) implements CustomPacketPayload {
        public static final Type<CanvasSnapshot> TYPE=new Type<>(Jugcraft.id("jimta_computer_canvas"));
        public static final StreamCodec<RegistryFriendlyByteBuf,CanvasSnapshot> CODEC=new StreamCodec<>(){
            public CanvasSnapshot decode(RegistryFriendlyByteBuf b){return new CanvasSnapshot(b.readVarInt(),b.readUtf(1152),b.readUtf(1152),b.readUtf(1152));}
            public void encode(RegistryFriendlyByteBuf b,CanvasSnapshot p){b.writeVarInt(p.menu);b.writeUtf(p.text,1152);b.writeUtf(p.foreground,1152);b.writeUtf(p.background,1152);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Snapshot(int menu, String program, String output, boolean running, long energy, boolean hardware, boolean readonly, boolean turtle,boolean fileStorage,int loadRevision,List<String> files) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(Jugcraft.id("jimta_computer_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            public Snapshot decode(RegistryFriendlyByteBuf b) {
                int menu=b.readVarInt();String program=b.readUtf(WorkshopComputers.MAX_PROGRAM),output=b.readUtf(8192);boolean running=b.readBoolean();long energy=b.readVarLong();boolean hardware=b.readBoolean(),readonly=b.readBoolean(),turtle=b.readBoolean(),fileStorage=b.readBoolean();
                int loadRevision=b.readVarInt();int count=b.readVarInt();if(count<0||count>16)throw new IllegalArgumentException("Invalid program shelf size");var files=new java.util.ArrayList<String>(count);for(int i=0;i<count;i++)files.add(b.readUtf(32));
                return new Snapshot(menu,program,output,running,energy,hardware,readonly,turtle,fileStorage,loadRevision,List.copyOf(files));
            }
            public void encode(RegistryFriendlyByteBuf b, Snapshot p) { b.writeVarInt(p.menu); b.writeUtf(p.program, WorkshopComputers.MAX_PROGRAM); b.writeUtf(p.output, 8192); b.writeBoolean(p.running); b.writeVarLong(p.energy); b.writeBoolean(p.hardware); b.writeBoolean(p.readonly); b.writeBoolean(p.turtle);b.writeBoolean(p.fileStorage);b.writeVarInt(p.loadRevision);b.writeVarInt(p.files.size());for(String name:p.files)b.writeUtf(name,32); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static final class Menu extends AbstractContainerMenu {
        private final Inventory playerInventory;
        private final ComputerNode node;
        private final InteractionHand hand;
        private final ItemStack held;
        private final boolean readonly;
        private String notice = "";
        private int page;
        private boolean printed() { return node==null && readonly && held.getItem() instanceof WorkshopComputers.DocumentItem item && item.kind.startsWith("printed_"); }
        private long lastSent = -20;
        public String program = "", output = "Connecting to the workshop terminal…";
        public long energy;
        public boolean running, hardware, readOnly, turtle, fileStorage;
        public boolean terminalView;
        public String canvasText="",canvasForeground="",canvasBackground="";
        public List<String> files=List.of();
        public int loadRevision;
        public Menu(int id, Inventory inventory) { this(id, inventory, null, null, ItemStack.EMPTY, false); }
        public Menu(int id, Inventory inventory, ComputerNode node) { this(id, inventory, node, null, ItemStack.EMPTY, false); }
        public Menu(int id, Inventory inventory, InteractionHand hand, ItemStack held, boolean readonly) { this(id, inventory, null, hand, held, readonly); }
        public static Menu remote(int id,Inventory inventory,ComputerNode node,InteractionHand hand,ItemStack held){return new Menu(id,inventory,node,hand,held,false);}
        private Menu(int id, Inventory inventory, ComputerNode node, InteractionHand hand, ItemStack held, boolean readonly) {
            super(TYPE, id); this.playerInventory = inventory; this.node = node; this.hand = hand; this.held = held; this.readonly = readonly;
            Container container = node == null ? new SimpleContainer(18) : node;
            for (int i = 0; i < 18; i++) addSlot(new Slot(container, i, 12 + i % 9 * 18, 204 + i / 9 * 18) {
                @Override public boolean mayPlace(ItemStack stack) {
                    if ((Menu.this.node != null ? Menu.this.node.turtle() : turtle) && getContainerSlot() >= 16) return ComputerNode.validUpgrade(stack);
                    return getContainerSlot() != 17 && (Menu.this.node != null || playerInventory.player.level().isClientSide() && hardware);
                }
                @Override public int getMaxStackSize() { return (Menu.this.node != null ? Menu.this.node.turtle() : turtle) && getContainerSlot() >= 16 ? 1 : super.getMaxStackSize(); }
                @Override public boolean mayPickup(Player player) { return stillValid(player) && (Menu.this.node != null || player.level().isClientSide() && hardware); }
                @Override public boolean isActive() { return !terminalView&&(Menu.this.node != null || hardware); }
            });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 188 + col * 18, 204 + row * 18){@Override public boolean isActive(){return !terminalView;}});
            for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 188 + i * 18, 262){@Override public boolean isActive(){return !terminalView;}});
        }
        @Override public boolean stillValid(Player player) {
            if (player.level().isClientSide()) return true;
            if (node != null) {
                if(hand==null)return node.stillValid(player);
                return player instanceof ServerPlayer sp&&player.getItemInHand(hand)==held&&node.permitted(sp)
                        &&sp.level().isLoaded(node.getBlockPos())&&sp.level().getBlockEntity(node.getBlockPos())==node
                        &&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(node.getBlockPos()))<=64*64;
            }
            return hand != null && player.isAlive() && !player.isSpectator() && player.getItemInHand(hand) == held;
        }
        private String text() { return node != null ? node.program() : held.getOrDefault(WorkshopComputers.DOCUMENT, ""); }
        private void fileAction(int action,String name,String text){
            if(readonly||node==null||!node.computer())return;
            try{
                switch(action){
                    case 0->{node.storeProgram(name,text);notice="Stored "+name+". The running program is unchanged.";}
                    case 1->{node.loadProgram(name);loadRevision++;notice="Loaded "+name+". Press Run when ready.";}
                    case 2->{node.deleteProgram(name);notice="Deleted "+name+" from this computer's shelf.";}
                    default->notice="Unknown program action.";
                }
            }catch(IllegalArgumentException failure){notice=failure.getMessage();}
        }
        private void action(ServerPlayer player, int action, String text) {
            if (!stillValid(player)) return;
            if (action == 6) { notice = HELP; return; }
            if (printed() && (action==7 || action==8)) { page += action==7?-1:1; notice=""; return; }
            if (readonly) { notice = "This document is read-only."; return; }
            try {
                if(action==10){if(node==null)throw new IllegalArgumentException("Keys need connected hardware");node.submitKey(text);return;}
                if(action==9){if(node==null)throw new IllegalArgumentException("Input needs connected hardware");node.submitInput(text);notice="Input sent.";return;}
                if (action == 0) {
                    if (text.split("\\R", -1).length > ComputerNode.MAX_LINES) throw new IllegalArgumentException("Program limit: 128 lines.");
                    if (node != null) node.saveProgram(text); else held.set(WorkshopComputers.DOCUMENT, text);
                    notice = "Saved.";
                } else if (node != null) {
                    notice = "";
                    switch (action) {
                        case 1 -> node.start();
                        case 2 -> node.stop();
                        case 3 -> node.disk(player, true);
                        case 4 -> node.disk(player, false);
                        case 5 -> notice = node.refuel() ? "Added 2000 JE." : "Insert coal/charcoal, or buffer is full.";
                        default -> notice = "Unknown terminal action.";
                    }
                } else if (action == 1) {
                    StringBuilder preview = new StringBuilder("Pocket preview (no hardware actions):\n");
                    for (String line : text().split("\\R", -1)) {
                        String stripped = line.strip(); if (stripped.equals("stop")) break;
                        if (stripped.startsWith("print ")) preview.append(stripped.substring(6)).append('\n');
                        else if (!stripped.isEmpty() && !stripped.startsWith("#")) preview.append("[hardware/control instruction] ").append(stripped).append('\n');
                        if (preview.length() > 4000) break;
                    }
                    notice = preview.toString();
                } else if (action == 3 || action == 4) {
                    ItemStack disk = ItemStack.EMPTY;
                    for (int i = 0; i < playerInventory.getContainerSize(); i++) {
                        ItemStack candidate = playerInventory.getItem(i);
                        if (WorkshopComputers.disk(candidate)) { disk = candidate; break; }
                    }
                    if (disk.isEmpty()) throw new IllegalArgumentException("Carry a floppy disk in your inventory.");
                    if (action == 3) { disk.set(WorkshopComputers.DOCUMENT, text()); notice = "Saved pocket program to disk."; }
                    else {
                        String stored = disk.getOrDefault(WorkshopComputers.DOCUMENT, "");
                        if (stored.isBlank()) throw new IllegalArgumentException("This disk is empty.");
                        if (stored.split("\\R", -1).length > ComputerNode.MAX_LINES) throw new IllegalArgumentException("Program limit: 128 lines.");
                        held.set(WorkshopComputers.DOCUMENT, stored); notice = "Loaded disk into pocket editor.";
                    }
                    playerInventory.setChanged();
                }
            } catch (IllegalArgumentException failure) { notice = failure.getMessage(); }
        }
        private void send(boolean force) {
            if (!(playerInventory.player instanceof ServerPlayer player) || !stillValid(player)) return;
            long tick = player.level().getGameTime(); if (!force && tick - lastSent < 10) return; lastSent = tick;
            String result = notice.isEmpty() ? node != null ? node.output() : text() : notice;
            String shownProgram=text();
            if(printed()) {
                try {
                    var pages=ComputerDocuments.pages(text()); page=Math.clamp(page,0,pages.size()-1);
                    var lines=java.util.Arrays.asList(pages.get(page).split("\\n",-1));
                    shownProgram=String.join("\n",lines.subList(0,Math.min(11,lines.size())));
                    if(notice.isEmpty()) result=String.join("\n",lines.subList(Math.min(11,lines.size()),lines.size()))+"\n\nPage "+(page+1)+" / "+pages.size();
                } catch(IllegalArgumentException tooLarge) { shownProgram=""; result=tooLarge.getMessage(); }
            }
            // A single maximum-length pocket line plus its preview prefix must still fit the packet.
            if (result.length() > 8192) result = result.substring(0, 8192);
            ServerPlayNetworking.send(player, new Snapshot(containerId, shownProgram, result, node != null && node.running(),
                    node == null ? 0 : node.energy.getAmount(), node != null, readonly, node != null && node.turtle(),node!=null&&node.computer(),loadRevision,node==null?List.of():node.savedPrograms()));
            if(node!=null)ServerPlayNetworking.send(player,new CanvasSnapshot(containerId,node.canvas().text(),node.canvas().foreground(),node.canvas().background()));
        }
        @Override public void broadcastChanges() { super.broadcastChanges(); send(false); }
        @Override public ItemStack quickMoveStack(Player player, int index) {
            if (!stillValid(player) || node == null || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
            Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
            ItemStack stack = slot.getItem(), copy = stack.copy();
            if (index < 18 ? !moveItemStackTo(stack, 18, slots.size(), true) : !moveItemStackTo(stack, 0, 18, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); return copy;
        }
    }
}
