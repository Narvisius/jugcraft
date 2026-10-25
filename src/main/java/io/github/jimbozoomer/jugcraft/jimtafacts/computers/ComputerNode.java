package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.control.*;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Saved computer state with a finite native instruction interpreter. No host files, Lua/JVM loading or HTTP. */
public final class ComputerNode extends BlockEntity implements Container {
    public static final int MAX_LINES = 128, MAX_NETWORK = 128, INTERVAL = 5;
    private static final Map<MinecraftServer, Budget> BUDGET = new WeakHashMap<>();
    private static final Set<ComputerNode> LOADED_MODEMS = Collections.newSetFromMap(new WeakHashMap<>());
    private UUID owner;
    private NonNullList<ItemStack> items = NonNullList.withSize(18, ItemStack.EMPTY);
    private String program = "print Hello, Jugcraft!\nstop", output = "JUGCRAFT Workshop OS\nNative bounded instructions. Select Help to begin.";
    private boolean running, moving;
    private int cursor, waiting, selected;
    private long nextInstructionTick;
    private long animationStart = -100, nextMonitorReform;
    private int animation = 0; // 1..6 move Direction ordinal+1; 7/8 turn; 9/10 swing left/right.
    private BlockPos monitorAnchor;
    private int monitorWidth = 1, monitorHeight = 1, monitorX, monitorY;
    private final Set<Integer> channels = new TreeSet<>(Set.of(0));
    private final ArrayDeque<Message> inbox = new ArrayDeque<>();
    private int replyChannel;
    private int displayColour=0xE5F4DF;
    private final ComputerCanvas canvas=new ComputerCanvas();
    private final ComputerWorkspace workspace=new ComputerWorkspace();
    private record Touch(BlockPos monitor,int x,int y) {}
    private final ArrayDeque<Touch> touches=new ArrayDeque<>();
    private boolean visualModemActive;
    private long nextCommandTick;
    private long nextAudioTick;
    private record Message(int channel, int reply, String text, BlockPos sender) {}
    private int[] signals = new int[6];
    public final SimpleEnergyStorage energy = new SimpleEnergyStorage(16000, 1024, 1024, this::setChanged);
    private static final class Budget { long tick = -1; int operations, messages, audio; }
    public ComputerNode(BlockPos pos, BlockState state) { super(WorkshopComputers.NODE, pos, state); }
    public String kind() { return ((ComputerBlock) getBlockState().getBlock()).kind; }
    public boolean turtle() { return kind().startsWith("turtle_"); }
    public boolean computer() { return kind().startsWith("computer_") || turtle(); }
    public String program() { return program; }
    public String output() {
        if (monitorAnchor != null && !monitorAnchor.equals(worldPosition) && level != null && level.isLoaded(monitorAnchor)
                && level.getBlockEntity(monitorAnchor) instanceof ComputerNode origin) return origin.output;
        return output;
    }
    public ItemStack upgrade(int side) { return turtle() ? items.get(16 + Math.clamp(side, 0, 1)) : ItemStack.EMPTY; }
    public int animation() { return animation; }
    public boolean modemActive() { return level != null && level.isClientSide() ? visualModemActive : !channels.isEmpty(); }
    public float animationFraction(float partial) { return level == null ? 1 : (float) Math.clamp((level.getGameTime() - animationStart + partial) / 8.0, 0, 1); }
    public int monitorWidth() { return monitorWidth; }
    public int monitorHeight() { return monitorHeight; }
    public int displayColour() { return displayColour; }
    public ComputerCanvas canvas(){return canvas;}
    private ComputerNode displayOrigin(){
        return monitorAnchor!=null&&!monitorAnchor.equals(worldPosition)&&level!=null&&level.isLoaded(monitorAnchor)
                &&level.getBlockEntity(monitorAnchor) instanceof ComputerNode origin&&sameOwner(origin)?origin:this;
    }
    private void draw(java.util.function.Consumer<ComputerCanvas> action){var origin=displayOrigin();action.accept(origin.canvas);origin.setChanged();origin.sync();}
    private void displayColour(int colour) {
        if(monitorAnchor!=null&&!monitorAnchor.equals(worldPosition)&&level.isLoaded(monitorAnchor)
                && level.getBlockEntity(monitorAnchor) instanceof ComputerNode origin && sameOwner(origin)) { origin.displayColour(colour); return; }
        displayColour=colour&0xFFFFFF;setChanged();sync();
    }
    public boolean touch(ServerPlayer player,BlockHitResult hit) {
        if(!kind().equals("monitor_advanced")||!permitted(player)||hit.getDirection()!=getBlockState().getValue(ComputerBlock.FACING)) return false;
        var right=getBlockState().getValue(ComputerBlock.FACING).getCounterClockWise();
        double dx=hit.getLocation().x-worldPosition.getX()-.5, dz=hit.getLocation().z-worldPosition.getZ()-.5;
        int x=monitorX*16+Math.clamp((int)((dx*right.getStepX()+dz*right.getStepZ()+.5)*16),0,15);
        int y=monitorY*16+Math.clamp((int)((1-(hit.getLocation().y-worldPosition.getY()))*16),0,15);
        var origin=displayOrigin();
        if(origin.canvas.active()) {
            // Match the top-left origin, aspect-preserving scale and margins used by the monitor renderer.
            double horizontal=monitorX+dx*right.getStepX()+dz*right.getStepZ()+.42;
            double vertical=monitorY+.86-(hit.getLocation().y-worldPosition.getY());
            double scale=Math.min((origin.monitorWidth-.16)/(ComputerCanvas.WIDTH*6),(origin.monitorHeight-.28)/(ComputerCanvas.HEIGHT*9));
            if(horizontal<0||vertical<0||horizontal>=ComputerCanvas.WIDTH*6*scale||vertical>=ComputerCanvas.HEIGHT*9*scale)return false;
            x=1+(int)(horizontal/(6*scale));y=1+(int)(vertical/(9*scale));
        }
        boolean delivered=false;
        for(var pos:network(player)) if(level.getBlockEntity(pos) instanceof ComputerNode target&&target.computer()&&target.permitted(player)&&target.touches.size()<16) {
            target.touches.addLast(new Touch(origin.worldPosition,x,y));delivered=true;
        }
        return delivered;
    }
    public boolean monitorOrigin() { return monitorAnchor == null || worldPosition.equals(monitorAnchor); }
    boolean sameOwner(ComputerNode other) { return owner != null && owner.equals(other.owner); }
    public boolean running() { return running; }
    public int signal(Direction direction) { return signals[direction.ordinal()]; }
    public int cursor() { return cursor; }
    public void owner(UUID id) { owner = id; setChanged(); }
    public boolean permitted(ServerPlayer player) {
        return owner != null && (owner.equals(player.getUUID()) || JugcraftParties.sameParty(owner, player.getUUID()))
                && (!kind().equals("computer_command") || player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                && level == player.level() && !isRemoved() && player.isAlive() && !player.isSpectator()
                && Authority.mayChange(player.level(), player, worldPosition);
    }
    public void open(ServerPlayer player) {
        if (owner == null && Authority.mayChange(player.level(), player, worldPosition)) owner(player.getUUID());
        if (!permitted(player)) return;
        player.openMenu(new SimpleMenuProvider((id, inv, who) -> new ComputerTerminal.Menu(id, inv, this),
                Component.literal(getBlockState().getBlock().getName().getString())));
    }
    public void saveProgram(String text) {
        if (text.length() > WorkshopComputers.MAX_PROGRAM || text.split("\\R", -1).length > MAX_LINES)
            throw new IllegalArgumentException("Program limit: 128 lines / 8192 characters.");
        stop(); program = text; cursor = 0; waiting = 0; setChanged();
    }
    public void start() { if (computer()) { cursor = 0; waiting = 0; running = true; append("Program started."); setChanged(); } }
    public void stop() {
        running = false; waiting = 0; Arrays.fill(signals, 0); setChanged();
        if (level != null) {
            if (getBlockState().getValue(ComputerBlock.LIT)) level.setBlock(worldPosition, getBlockState().setValue(ComputerBlock.LIT, false), Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock()); sync();
        }
    }
    public void append(String line) {
        output = output + "\n" + line;
        if (output.length() > 4096) output = output.substring(output.length() - 4096);
        setChanged();
    }
    private void display(String text) {
        if (monitorAnchor != null && !monitorAnchor.equals(worldPosition) && level.isLoaded(monitorAnchor)
                && level.getBlockEntity(monitorAnchor) instanceof ComputerNode origin && sameOwner(origin)) { origin.display(text); return; }
        boolean wasCanvas=canvas.active();canvas.deactivate();
        String next = text.length() <= 4096 ? text : text.substring(text.length() - 4096);
        if (!next.equals(output)) { output = next; setChanged(); sync(); }
        else if(wasCanvas){setChanged();sync();}
    }
    void monitorLayout(BlockPos anchor, int width, int height, int x, int y) {
        nextMonitorReform = level.getGameTime() + 20;
        if (anchor.equals(monitorAnchor) && width == monitorWidth && height == monitorHeight && x == monitorX && y == monitorY) return;
        monitorAnchor = anchor.immutable(); monitorWidth = width; monitorHeight = height; monitorX = x; monitorY = y;
        int connections = (x > 0 ? 1 : 0) | (x < width - 1 ? 2 : 0) | (y > 0 ? 4 : 0) | (y < height - 1 ? 8 : 0);
        level.setBlock(worldPosition, getBlockState().setValue(ComputerMonitors.CONNECTIONS, connections), Block.UPDATE_CLIENTS);
        setChanged(); sync();
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (kind().startsWith("monitor_") && server.getGameTime() >= nextMonitorReform) ComputerMonitors.reform(this);
        if (wireless()) LOADED_MODEMS.add(this); else LOADED_MODEMS.remove(this);
        if (!running || !computer() || server.getGameTime() < nextInstructionTick
                || Math.floorMod(server.getGameTime() + worldPosition.asLong(), INTERVAL) != 0) return;
        ServerPlayer actor = Authority.answering(server, owner);
        if (actor == null || !permitted(actor)) return;
        if (waiting > 0) { waiting -= INTERVAL; setChanged(); return; }
        Budget budget = BUDGET.computeIfAbsent(server.getServer(), ignored -> new Budget());
        if (budget.tick != server.getServer().getTickCount()) { budget.tick = server.getServer().getTickCount(); budget.operations = 0; budget.messages = 0;budget.audio=0; }
        if (budget.operations >= 64 || energy.getAmount() < 1) return;
        ++budget.operations;
        nextInstructionTick = server.getGameTime() + INTERVAL;
        String[] lines = program.split("\\R", -1);
        if (cursor >= lines.length || cursor >= MAX_LINES) { stop(); return; }
        String instruction = lines[cursor++].strip();
        energy.setAmount(energy.getAmount() - 1);
        try { execute(instruction, actor); }
        catch (IllegalArgumentException failure) { append("Line " + cursor + ": " + failure.getMessage()); stop(); }
        if (!isRemoved()) {
            setChanged(); boolean lit = running;
            if (getBlockState().getValue(ComputerBlock.LIT) != lit) server.setBlock(worldPosition, getBlockState().setValue(ComputerBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    /** One bounded instruction. Physical operations recheck owner permission at both endpoints on every call. */
    private void execute(String instruction, ServerPlayer actor) {
        if (instruction.isBlank() || instruction.startsWith("#") || instruction.startsWith("--")) return;
        String[] words = instruction.split("\\s+", 4); String op = words[0].toLowerCase(Locale.ROOT);
        switch (op) {
            case "print" -> append(workspace.text(rest(instruction)));
            case "set" -> workspace.set(word(words,1),workspace.value(word(words,2)));
            case "add" -> workspace.set(word(words,1),(long)workspace.get(word(words,1))+workspace.value(word(words,2)));
            case "subtract" -> workspace.set(word(words,1),(long)workspace.get(word(words,1))-workspace.value(word(words,2)));
            case "multiply" -> workspace.set(word(words,1),(long)workspace.get(word(words,1))*workspace.value(word(words,2)));
            case "divide" -> {int divisor=workspace.value(word(words,2));if(divisor==0)throw new IllegalArgumentException("Division by zero.");workspace.set(word(words,1),workspace.get(word(words,1))/divisor);}
            case "ifvar" -> {
                String[] args=instruction.split("\\s+");int left=workspace.get(word(args,1)),right=workspace.value(word(args,3));
                boolean match=switch(word(args,2)){case "=="->left==right;case "!="->left!=right;case "<"->left<right;case "<="->left<=right;case ">"->left>right;case ">="->left>=right;default->throw new IllegalArgumentException("Comparison: == != < <= > >=");};
                int destination=integer(args,4,1,MAX_LINES);if(match)cursor=destination-1;
            }
            case "programs" -> append(workspace.list());
            case "store" -> {workspace.save(word(words,1),program);append("Program saved on this computer.");}
            case "runfile" -> {program=workspace.load(word(words,1));cursor=0;waiting=0;append("Running "+word(words,1));}
            case "deletefile" -> workspace.delete(word(words,1));
            case "energyvar" -> workspace.set(word(words,1),energy.getAmount());
            case "mailvar" -> workspace.set(word(words,1),inbox.size());
            case "countvar" -> workspace.set(word(words,1),ComputerPeripherals.count(this,actor,word(words,2),word(words,3)));
            case "readvar" -> {
                DyeColor channel=color(word(words,2));int total=0,count=0;
                for(BlockPos at:network(actor)){var state=level.getBlockState(at);if(state.getBlock() instanceof SensorBlock&&state.getValue(Channels.CHANNEL)==channel&&allowed(actor,SensorBlock.target(at,state))){int value=SensorBlock.percent(level,at,state);if(value>=0){total+=value;count++;}}}
                workspace.set(word(words,1),count==0?-1:total/count);
            }
            case "stop" -> stop();
            case "command" -> {
                if(!kind().equals("computer_command")||Authority.isStandIn(actor)||!actor.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                    throw new IllegalArgumentException("Commands require a Command Computer and its present operator owner.");
                if(level.getGameTime()<nextCommandTick){cursor--;waiting=INTERVAL;break;}
                String command=rest(instruction);if(command.isBlank()||command.length()>512)throw new IllegalArgumentException("Command length: 1..512 characters.");
                if(energy.getAmount()<100){cursor--;waiting=20;break;}
                nextCommandTick=level.getGameTime()+20;energy.setAmount(energy.getAmount()-100);
                var source=actor.createCommandSourceStack().withPosition(Vec3.atCenterOf(worldPosition))
                        .withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER)
                        .withCallback((success,result)->append("Command "+(success?"completed":"failed")+": "+result));
                actor.level().getServer().getCommands().performPrefixedCommand(source,command);
            }
            case "wait" -> waiting = integer(words, 1, 1, 1200);
            case "goto" -> cursor = integer(words, 1, 1, MAX_LINES) - 1;
            case "if" -> { // if <side> <minimum signal> <line>
                Direction side = side(word(words, 1));
                if (!allowed(actor, worldPosition.relative(side))) throw new IllegalArgumentException("Protected or unloaded signal input.");
                int value = actor.level().getSignal(worldPosition.relative(side), side);
                if (value >= integer(words, 2, 0, 15)) cursor = integer(words, 3, 1, MAX_LINES) - 1;
            }
            case "signal" -> {
                Direction side = side(word(words, 1)); int value = integer(words, 2, 0, 15);
                BlockPos neighbor = worldPosition.relative(side);
                if (!allowed(actor, neighbor)) throw new IllegalArgumentException("Protected or unloaded signal target.");
                signals[side.ordinal()] = value; level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
            case "read" -> {
                DyeColor channel = color(word(words, 1)); int total = 0, count = 0;
                for (BlockPos pos : network(actor)) {
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof SensorBlock && state.getValue(Channels.CHANNEL) == channel
                            && allowed(actor, SensorBlock.target(pos, state))) {
                        int percent = SensorBlock.percent(level, pos, state); if (percent >= 0) { total += percent; count++; }
                    }
                }
                append(channel.getName() + ": " + (count == 0 ? "no sensor" : (total / count) + "%"));
            }
            case "channel" -> {
                DyeColor channel = color(word(words, 1)); boolean on = on(word(words, 2)); int changed = 0;
                for (BlockPos pos : network(actor)) {
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof ChannelSwitch relay && state.getValue(Channels.CHANNEL) == channel) {
                        boolean allAllowed = true; for (Direction side : Direction.values()) allAllowed &= allowed(actor, pos.relative(side));
                        if (allAllowed && relay.switchTo(level, pos, state, on)) changed++;
                    }
                }
                append("Switched " + changed + " " + channel.getName() + " relays.");
            }
            case "relay" -> {
                int value = integer(words, 1, 0, 15);
                for (BlockPos pos : network(actor)) if (level.getBlockEntity(pos) instanceof ComputerNode node && node.kind().equals("redstone_relay")) {
                    boolean allAllowed = true; for (Direction side : Direction.values()) allAllowed &= allowed(actor, pos.relative(side));
                    if (allAllowed && node.permitted(actor)) { Arrays.fill(node.signals, value); node.setChanged(); level.updateNeighborsAt(pos, node.getBlockState().getBlock()); }
                }
            }
            case "display" -> { String text=workspace.text(rest(instruction));for (ComputerNode node : devices(actor, "monitor")) node.display(text); }
            case "cursor", "ink", "write", "clear", "scroll", "blit" -> {
                Set<ComputerNode> displays=new LinkedHashSet<>();for(var monitor:devices(actor,"monitor"))displays.add(monitor.displayOrigin());
                if(displays.isEmpty())throw new IllegalArgumentException("No permitted monitor connected.");
                for(var monitor:displays){
                    switch(op){
                        case "cursor" -> {int x=integer(words,1,1,48),y=integer(words,2,1,24);monitor.draw(c->c.cursor(x,y));}
                        case "ink" -> {int fore=color(word(words,1)).getId(),back=color(word(words,2)).getId();
                            if(monitor.kind().equals("monitor_normal")){fore=0;back=15;}final int f=fore,b=back;monitor.draw(c->c.ink(f,b));}
                        case "write" -> {String text=workspace.text(rest(instruction));monitor.draw(c->c.write(text));}
                        case "clear" -> monitor.draw(ComputerCanvas::clear);
                        case "scroll" -> {int rows=integer(words,1,-24,24);monitor.draw(c->c.scroll(rows));}
                        case "blit" -> {if(!monitor.kind().equals("monitor_advanced"))throw new IllegalArgumentException("Blit requires advanced monitors.");
                            String text=word(words,1),fore=word(words,2),back=word(words,3);monitor.draw(c->c.blit(text,fore,back));}
                    }
                }
            }
            case "colour", "color" -> { int rgb=color(word(words,1)).getTextureDiffuseColor();for(var monitor:devices(actor,"monitor_advanced")) monitor.displayColour(rgb); }
            case "waittouch" -> {
                int line=integer(words,1,1,MAX_LINES);Touch event=touches.peekFirst();
                if(event==null) { cursor--;waiting=INTERVAL; } else {
                    workspace.setPair("touch_x",event.x,"touch_y",event.y);touches.removeFirst();
                    append("Monitor "+event.monitor.toShortString()+" touched at "+event.x+", "+event.y);cursor=line-1;
                }
            }
            case "beep" -> { for (ComputerNode node : devices(actor, "speaker"))
                actor.level().playSound(null, node.worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, .5F, 1F);
                if (hasUpgrade(WorkshopComputers.BLOCKS.get("speaker").asItem())) actor.level().playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, .5F, 1F);
            }
            case "audio" -> {
                if(level.getGameTime()<nextAudioTick){cursor--;waiting=INTERVAL;break;}
                var audioBudget=BUDGET.get(actor.level().getServer());if(audioBudget.audio>=4){cursor--;waiting=INTERVAL;break;}
                byte[] clip=ComputerAudio.parse(rest(instruction));var speakers=devices(actor,"speaker");
                if(hasUpgrade(WorkshopComputers.BLOCKS.get("speaker").asItem()))speakers.add(this);
                if(speakers.isEmpty())throw new IllegalArgumentException("Audio needs a connected speaker or speaker upgrade.");
                if(energy.getAmount()<20){cursor--;waiting=20;break;}
                nextAudioTick=level.getGameTime()+20;energy.setAmount(energy.getAmount()-20);
                audioBudget.audio++;
                for(var speaker:speakers.stream().limit(4).toList())ComputerAudio.play(actor,speaker.worldPosition,clip);
            }
            case "printpage", "printpages", "printbook" -> {
                var printers = devices(actor, "printer");
                if (printers.isEmpty()) throw new IllegalArgumentException("No permitted printer connected.");
                printers.getFirst().printDocument(output, op);
            }
            case "save" -> disk(actor, true);
            case "load" -> disk(actor, false);
            case "send" -> transmit(actor, 0, 0, rest(instruction));
            case "transmit" -> transmit(actor, integer(words, 1, 0, 65535), integer(words, 2, 0, 65535), word(words, 3));
            case "reply" -> transmit(actor, replyChannel, 0, rest(instruction));
            case "listen" -> {
                int channel = integer(words, 1, 0, 65535);
                if (!channels.contains(channel) && channels.size() >= 16) throw new IllegalArgumentException("At most 16 open modem channels.");
                channels.add(channel); append("Listening on " + channel); setChanged(); sync();
            }
            case "unlisten" -> { if (word(words, 1).equals("all")) channels.clear(); else channels.remove(integer(words, 1, 0, 65535)); setChanged(); sync(); }
            case "receive" -> {
                Message message = inbox.pollFirst();
                if (message == null) { cursor--; waiting = 20; }
                else { replyChannel = message.reply; append("[channel " + message.channel + "; reply " + message.reply + "; " + message.sender.toShortString() + "] " + message.text); }
            }
            case "ifmail" -> {
                int channel = integer(words, 1, 0, 65535);
                if (inbox.stream().anyMatch(message -> message.channel == channel)) cursor = integer(words, 2, 1, MAX_LINES) - 1;
            }
            case "select" -> { requireTurtle(); selected = integer(words, 1, 1, 16) - 1; }
            case "selectedvar" -> { requireTurtle(); workspace.set(word(words,1), selected + 1); }
            case "slotvar", "spacevar" -> {
                requireTurtle(); ItemStack stack=items.get(integer(words,2,1,16)-1);
                workspace.set(word(words,1), op.equals("slotvar") ? stack.getCount() : stack.isEmpty() ? 64 : Math.max(0,stack.getMaxStackSize()-stack.getCount()));
            }
            case "comparevar" -> { requireTurtle();workspace.set(word(words,1),ItemStack.isSameItemSameComponents(items.get(selected),items.get(integer(words,2,1,16)-1))?1:0); }
            case "transferslot" -> { requireTurtle(); transferSlot(integer(words,1,1,16)-1,integer(words,2,1,64)); }
            case "detectvar", "compareblockvar" -> {
                requireTurtle(); BlockPos target=worldPosition.relative(side(word(words,2)));
                if(!allowed(actor,target))throw new IllegalArgumentException("Protected or unloaded inspection target.");
                BlockState state=level.getBlockState(target);
                boolean result=op.equals("detectvar") ? !state.isAir() : items.get(selected).getItem() instanceof BlockItem item && state.is(item.getBlock());
                workspace.set(word(words,1),result?1:0);
            }
            case "inspect" -> {
                requireTurtle();BlockPos target=worldPosition.relative(side(word(words,1)));
                if(!allowed(actor,target))throw new IllegalArgumentException("Protected or unloaded inspection target.");
                BlockState state=level.getBlockState(target);
                append(BuiltInRegistries.BLOCK.getKey(state.getBlock())+" "+state.getValues());
            }
            case "refuel" -> { if(!refuel())throw new IllegalArgumentException("No coal/charcoal or less than 2000 JE of battery space."); }
            case "equip", "unequip" -> { requireTurtle(); changeUpgrade(word(words, 1), op.equals("equip")); }
            case "craft" -> { requireTurtle(); craft(actor); }
            case "attack" -> { requireTurtle(); attack(actor, side(word(words, 1))); }
            case "turn" -> { requireTurtle();String turn=word(words,1);if(!turn.equals("left")&&!turn.equals("right"))throw new IllegalArgumentException("Turn left or right.");boolean right = turn.equals("right");
                if (!right && !words[1].equals("left")) throw new IllegalArgumentException("Use turn left or turn right.");
                Direction face = getBlockState().getValue(ComputerBlock.FACING);
                level.setBlock(worldPosition, getBlockState().setValue(ComputerBlock.FACING, right ? face.getClockWise() : face.getCounterClockWise()), Block.UPDATE_ALL);
                animate(right ? 8 : 7);
            }
            case "move" -> { requireTurtle(); move(actor, side(word(words, 1))); }
            case "dig" -> { requireTurtle(); dig(actor, worldPosition.relative(side(word(words, 1)))); }
            case "place" -> { requireTurtle(); place(actor, worldPosition.relative(side(word(words, 1)))); }
            case "till" -> { requireTurtle(); till(actor,worldPosition.relative(side(word(words,1)))); }
            case "pull", "push" -> transfer(actor, side(word(words, 1)), integer(words, 2, 1, 64), op.equals("pull"));
            case "ports" -> {
                var ports = ComputerPeripherals.ports(this,actor);
                if(ports.isEmpty()) append("No permitted inventory/tank ports connected.");
                ports.forEach((name, port) -> append(name+" -> "+port.pos().toShortString()));
            }
            case "items", "fluids" -> ComputerPeripherals.describe(this,actor,word(words,1),op.equals("fluids"));
            case "moveitems", "movefluid" -> ComputerPeripherals.move(this,actor,word(words,1),word(words,2),integer(words,3,1,op.equals("movefluid")?1000:64),op.equals("movefluid"));
            default -> throw new IllegalArgumentException("Unknown native instruction '" + op + "'. Lua/CraftOS is not supported; use Help.");
        }
    }
    public boolean refuel() {
        if (energy.getAmount() > energy.getCapacity() - 2000) return false;
        for (ItemStack stack : items.subList(0,16)) if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            stack.shrink(1); energy.setAmount(energy.getAmount() + 2000); setChanged(); return true;
        }
        return false;
    }
    private static String rest(String line) { int split = line.indexOf(' '); return split < 0 ? "" : line.substring(split + 1); }
    private static String word(String[] words, int index) { if (index >= words.length) throw new IllegalArgumentException("Missing instruction argument."); return words[index]; }
    private static int integer(String[] words, int index, int min, int max) {
        try { int value = Integer.parseInt(word(words, index)); if (value >= min && value <= max) return value; }
        catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("Argument " + index + " must be " + min + ".." + max + ".");
    }
    private static boolean on(String word) { if (word.equals("on")) return true; if (word.equals("off")) return false; throw new IllegalArgumentException("Use on or off."); }
    private static DyeColor color(String value) { for (DyeColor color : DyeColor.values()) if (color.getName().equals(value)) return color; throw new IllegalArgumentException("Unknown dye channel."); }
    private Direction side(String name) {
        Direction forward = getBlockState().getValue(ComputerBlock.FACING);
        return switch (name) { case "front", "forward" -> forward; case "back" -> forward.getOpposite(); case "left" -> forward.getCounterClockWise(); case "right" -> forward.getClockWise(); case "up" -> Direction.UP; case "down" -> Direction.DOWN; default -> throw new IllegalArgumentException("Side: front/back/left/right/up/down."); };
    }
    private void requireTurtle() { if (!turtle()) throw new IllegalArgumentException("This instruction requires a turtle."); }
    public static boolean validUpgrade(ItemStack stack) {
        return stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.DIAMOND_SHOVEL)
                || stack.is(Items.DIAMOND_SWORD) || stack.is(Items.DIAMOND_HOE) || stack.is(Items.CRAFTING_TABLE)
                || stack.is(WorkshopComputers.BLOCKS.get("speaker").asItem())
                || stack.is(WorkshopComputers.BLOCKS.get("wireless_modem_normal").asItem())
                || stack.is(WorkshopComputers.BLOCKS.get("wireless_modem_advanced").asItem());
    }
    private boolean hasUpgrade(Item item) { return turtle() && (upgrade(0).is(item) || upgrade(1).is(item)); }
    private boolean wireless() { return kind().startsWith("wireless_modem") || hasUpgrade(WorkshopComputers.BLOCKS.get("wireless_modem_normal").asItem()) || hasUpgrade(WorkshopComputers.BLOCKS.get("wireless_modem_advanced").asItem()); }
    private boolean advancedWireless() { return kind().equals("wireless_modem_advanced") || hasUpgrade(WorkshopComputers.BLOCKS.get("wireless_modem_advanced").asItem()); }
    private void changeUpgrade(String side, boolean equip) {
        int slot = side.equals("left") ? 16 : side.equals("right") ? 17 : -1;
        if (slot < 0) throw new IllegalArgumentException("Use equip/unequip left or right.");
        if (equip) {
            if (!items.get(slot).isEmpty() || !validUpgrade(items.get(selected))) throw new IllegalArgumentException("Upgrade slot occupied or selected item is not a turtle upgrade.");
            items.set(slot, items.get(selected).split(1));
        } else {
            List<ItemStack> after = copyItems(); ItemStack stack = after.get(slot);
            if (!stack.isEmpty() && insert(after, stack, stack.getCount()) != stack.getCount()) throw new IllegalArgumentException("No room to remove upgrade.");
            after.set(slot, ItemStack.EMPTY); for (int i = 0; i < 18; i++) items.set(i, after.get(i));
        }
        setChanged(); sync();
    }
    private void animate(int id) { animation = id; animationStart = level.getGameTime(); nextInstructionTick = animationStart + 8; setChanged(); sync(); }
    private int toolSlot(BlockState target) {
        if (turtle()) for (int i = 16; i < 18; i++) if (!items.get(i).isEmpty() && items.get(i).isCorrectToolForDrops(target)) return i;
        return selected;
    }
    private void craft(ServerPlayer actor) {
        if (!hasUpgrade(Items.CRAFTING_TABLE) || energy.getAmount() < 20) throw new IllegalArgumentException("Crafting needs a fitted crafting-table upgrade and 20 JE.");
        List<ItemStack> after = copyItems();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, after.subList(0, 9));
        var found = actor.level().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, actor.level());
        if (found.isEmpty()) throw new IllegalArgumentException("Slots 1..9 do not form a crafting recipe.");
        ItemStack made = found.get().value().assemble(input);
        if (made.isEmpty()) throw new IllegalArgumentException("This recipe has no output.");
        var remainders = found.get().value().getRemainingItems(input);
        for (int i = 0; i < 9; i++) if (!after.get(i).isEmpty()) after.get(i).shrink(1);
        if (insert(after, made, made.getCount()) != made.getCount()) throw new IllegalArgumentException("No room for crafted output.");
        for (ItemStack remainder : remainders) if (insert(after, remainder, remainder.getCount()) != remainder.getCount()) throw new IllegalArgumentException("No room for crafting remainders.");
        for (int i = 0; i < 18; i++) items.set(i, after.get(i)); energy.setAmount(energy.getAmount() - 20); setChanged(); sync();
    }
    private void attack(ServerPlayer actor, Direction side) {
        int slot = upgrade(0).is(Items.DIAMOND_SWORD) ? 16 : upgrade(1).is(Items.DIAMOND_SWORD) ? 17 : -1;
        BlockPos target = worldPosition.relative(side);
        if (slot < 0 || energy.getAmount() < 30 || !allowed(actor, target)) throw new IllegalArgumentException("Attack needs a fitted diamond sword, 30 JE and an unprotected target.");
        var targets = actor.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new AABB(target),
                entity -> entity.isAlive() && !entity.isSpectator() && !(entity instanceof Player));
        for (var entity : targets) if (Authority.mayStrike(actor, entity)) {
            var start = Vec3.atCenterOf(worldPosition).add(side.getStepX() * .65, side.getStepY() * .65, side.getStepZ() * .65);
            var hit = actor.level().clip(new ClipContext(start, entity.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
            if (hit.getType() != HitResult.Type.MISS && !hit.getBlockPos().equals(target)) continue;
            if (entity.hurtServer(actor.level(), actor.damageSources().playerAttack(actor), 7)) {
                items.get(slot).hurtAndBreak(1, actor, EquipmentSlot.MAINHAND); energy.setAmount(energy.getAmount() - 30); animate(slot == 16 ? 9 : 10);
            }
            return;
        }
    }
    private boolean allowed(ServerPlayer actor, BlockPos pos) { return level instanceof ServerLevel server && server.isLoaded(pos) && Authority.mayChange(server, actor, pos); }
    private void transferSlot(int destination,int requested) {
        if(destination==selected)return;
        ItemStack from=items.get(selected),to=items.get(destination);
        if(from.isEmpty()||!to.isEmpty()&&!ItemStack.isSameItemSameComponents(from,to))return;
        int moved=Math.min(requested,Math.min(from.getCount(),from.getMaxStackSize()-to.getCount()));
        if(moved<=0)return;
        if(to.isEmpty())items.set(destination,from.split(moved));else{to.grow(moved);from.shrink(moved);}
        setChanged();
    }
    private void till(ServerPlayer actor,BlockPos target) {
        int slot=upgrade(0).is(Items.DIAMOND_HOE)?16:upgrade(1).is(Items.DIAMOND_HOE)?17:-1;
        if(slot<0||energy.getAmount()<20||!allowed(actor,target)||!allowed(actor,target.above())||!level.getBlockState(target.above()).isAir())
            throw new IllegalArgumentException("Tilling needs a fitted diamond hoe, 20 JE, permission and air above the soil.");
        BlockState soil=level.getBlockState(target),result;
        if(soil.is(Blocks.COARSE_DIRT))result=Blocks.DIRT.defaultBlockState();
        else if(soil.is(Blocks.DIRT)||soil.is(Blocks.GRASS_BLOCK)||soil.is(Blocks.DIRT_PATH))result=Blocks.FARMLAND.defaultBlockState();
        else throw new IllegalArgumentException("This soil cannot be tilled.");
        if(!level.setBlock(target,result,Block.UPDATE_ALL))throw new IllegalArgumentException("Soil did not change.");
        energy.setAmount(energy.getAmount()-20);items.get(slot).hurtAndBreak(1,actor,EquipmentSlot.MAINHAND);animate(slot==16?9:10);
        level.playSound(null,target,SoundEvents.HOE_TILL.value(),SoundSource.BLOCKS,.7f,1);
    }
    List<BlockPos> network(ServerPlayer actor) {
        List<BlockPos> devices = new ArrayList<>(); Set<BlockPos> seen = new HashSet<>(); ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition); seen.add(worldPosition);
        while (!queue.isEmpty() && seen.size() <= MAX_NETWORK) {
            BlockPos pos = queue.removeFirst();
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (seen.size() >= MAX_NETWORK || !seen.add(next) || !allowed(actor, next)
                        || !io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level,pos,next)) continue;
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof DataCableBlock) queue.add(next);
                else if (state.getBlock() instanceof ComputerBlock) {
                    if (level.getBlockEntity(next) instanceof ComputerNode node && node.permitted(actor)) { devices.add(next); queue.add(next); }
                } else if (state.getBlock() instanceof SensorBlock || state.getBlock() instanceof ChannelSwitch || state.getBlock() instanceof LogicControllerBlock) devices.add(next);
            }
        }
        return devices;
    }
    private List<ComputerNode> devices(ServerPlayer actor, String prefix) {
        List<ComputerNode> result = new ArrayList<>();
        if (kind().startsWith(prefix)) result.add(this);
        for (BlockPos pos : network(actor)) if (level.getBlockEntity(pos) instanceof ComputerNode node && node.kind().startsWith(prefix)) result.add(node);
        return result;
    }
    public void disk(ServerPlayer actor, boolean save) {
        List<ComputerNode> drives = new ArrayList<>(); drives.add(this); drives.addAll(devices(actor, "disk_drive"));
        for (ComputerNode drive : drives) for (ItemStack stack : drive.items) if (WorkshopComputers.disk(stack)) {
            if (save) { stack.set(WorkshopComputers.DOCUMENT, program); drive.setChanged(); append("Program saved to disk."); }
            else { String text = stack.getOrDefault(WorkshopComputers.DOCUMENT, ""); if (text.isBlank()) throw new IllegalArgumentException("This disk is empty."); saveProgram(text); append("Program loaded; press Run."); }
            return;
        }
        throw new IllegalArgumentException("Insert a floppy disk here or in a connected disk drive.");
    }
    private void printDocument(String text, String operation) {
        int paperCount = ComputerDocuments.pages(text).size();
        if(operation.equals("printpage") && paperCount>1) throw new IllegalArgumentException("Text needs "+paperCount+" pages. Use printpages or printbook, or shorten it.");
        List<ItemStack> after = copyItems();
        if (!items.get(17).isEmpty() || !consume(after, Items.PAPER, paperCount) || !consume(after, Items.INK_SAC, paperCount)
                || !operation.equals("printpage") && !consume(after, Items.STRING, 1)
                || operation.equals("printbook") && !consume(after, Items.LEATHER, 1))
            throw new IllegalArgumentException("Printer needs "+paperCount+" paper and ink; bound pages also string, books also leather. Empty output slot 18.");
        String kind = operation.equals("printbook") ? "printed_book" : operation.equals("printpages") ? "printed_pages" : "printed_page";
        var page = new ItemStack(WorkshopComputers.ITEMS.get(kind)); page.set(WorkshopComputers.DOCUMENT, text);
        for (int i = 0; i < 18; i++) items.set(i, after.get(i));
        items.set(17, page); setChanged(); updatePeripheralLight();
    }
    private static boolean consume(List<ItemStack> inventory, Item item, int count) {
        for (int i = 0; i < 16 && count > 0; i++) if (inventory.get(i).is(item)) {
            int removed = Math.min(count, inventory.get(i).getCount()); inventory.get(i).shrink(removed); count -= removed;
        }
        return count == 0;
    }
    private void transmit(ServerPlayer actor, int channel, int reply, String message) {
        if (message.length() > 512) throw new IllegalArgumentException("Modem message limit: 512 characters.");
        Budget budget = BUDGET.computeIfAbsent(actor.level().getServer(), ignored -> new Budget());
        if (budget.messages >= 8) { cursor--; waiting = INTERVAL; return; }
        budget.messages++;
        Set<GlobalPos> delivered = new HashSet<>();
        for (BlockPos pos : network(actor)) if (level.getBlockEntity(pos) instanceof ComputerNode node && node.computer())
            node.acceptMessage(this, actor, channel, reply, message, delivered);
        List<ComputerNode> senders = devices(actor, "wireless_modem");
        if (wireless()) senders.addFirst(this);
        if (!senders.isEmpty()) {
            ComputerNode sender = senders.getFirst(); int examined = 0, reached = 0;
            for (ComputerNode receiver : LOADED_MODEMS) {
                if (++examined > 512 || reached >= 32) break;
                if(receiver==sender||receiver.isRemoved()||!(receiver.level instanceof ServerLevel remote)
                        ||remote.getServer()!=actor.level().getServer()||!remote.isLoaded(receiver.worldPosition))continue;
                boolean advanced=sender.advancedWireless()&&receiver.advancedWireless();
                if(remote!=level&&!advanced)continue;
                if(remote==level&&receiver.worldPosition.distSqr(sender.worldPosition)>(advanced?256*256:64*64))continue;
                // A remote identity answers only protection questions; packets cannot execute commands or move the owner.
                ServerPlayer remoteActor=Authority.remoteModemActor(remote,actor);
                if(!receiver.permitted(remoteActor))continue;
                if(remote!=level){if(energy.getAmount()<10)break;energy.setAmount(energy.getAmount()-10);}
                reached++;
                if (receiver.computer()) receiver.acceptMessage(this, remoteActor, channel, reply, message, delivered);
                for (BlockPos pos : receiver.network(remoteActor)) if (remote.getBlockEntity(pos) instanceof ComputerNode target && target.computer())
                    target.acceptMessage(this, remoteActor, channel, reply, message, delivered);
            }
        }
        append("Channel " + channel + ": delivered to " + delivered.size() + " permitted loaded computers.");
    }
    private void acceptMessage(ComputerNode sender, ServerPlayer actor, int channel, int reply, String text, Set<GlobalPos> delivered) {
        if (this == sender || !channels.contains(channel) || inbox.size() >= 16 || !permitted(actor) || !delivered.add(GlobalPos.of(level.dimension(),worldPosition))) return;
        inbox.addLast(new Message(channel, reply, text, sender.worldPosition.immutable()));
    }
    private void move(ServerPlayer actor, Direction direction) {
        BlockPos next = worldPosition.relative(direction);
        if (energy.getAmount() < 20 || !allowed(actor, next) || !level.getBlockState(next).isAir()
                || !level.getFluidState(next).isEmpty() || !level.noCollision(new AABB(next))) throw new IllegalArgumentException("Turtle path blocked, protected, occupied or needs 20 JE.");
        var saved = saveWithoutMetadata(level.registryAccess()); var state = getBlockState();
        if (!level.setBlock(next, state, Block.UPDATE_ALL)) throw new IllegalArgumentException("Turtle could not place its destination.");
        if (!(level.getBlockEntity(next) instanceof ComputerNode destination)) {
            level.setBlock(next, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            throw new IllegalArgumentException("Turtle destination was not created; original cargo retained.");
        }
        destination.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        destination.inbox.addAll(inbox); destination.replyChannel = replyChannel;
        destination.energy.setAmount(destination.energy.getAmount() - 20);
        destination.animate(direction.ordinal() + 1);
        moving = true;
        if (!level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
            // Destination receives no drops during rollback; source still owns the original inventory.
            destination.moving = true; level.setBlock(next, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL); moving = false;
            throw new IllegalArgumentException("Turtle move rolled back.");
        }
    }
    private void dig(ServerPlayer actor, BlockPos target) {
        if (!allowed(actor, target)) throw new IllegalArgumentException("Protected or unloaded mining target.");
        BlockState state = level.getBlockState(target); int toolIndex = toolSlot(state); ItemStack tool = items.get(toolIndex);
        long toolEnergy = tool.getItem() instanceof io.github.jimbozoomer.jugcraft.tools.PoweredToolItem powered ? powered.energyPerBlock(tool) : 0;
        if (toolEnergy > 0 && io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(tool) < toolEnergy)
            throw new IllegalArgumentException("Selected powered tool needs more charge.");
        if (energy.getAmount() < 30 || state.isAir() || state.getDestroySpeed(level, target) < 0
                || state.getDestroySpeed(level, target) > 50 || level.getBlockEntity(target) != null
                || state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) throw new IllegalArgumentException("Cannot mine target with selected tool, permission or power.");
        List<ItemStack> drops = Block.getDrops(state, actor.level(), target, null, actor, tool);
        List<ItemStack> next = copyItems();
        for (ItemStack drop : drops) if (insert(next, drop, drop.getCount()) != drop.getCount()) throw new IllegalArgumentException("Turtle storage full.");
        if (!level.destroyBlock(target, false, actor)) throw new IllegalArgumentException("Mining failed.");
        for (int i = 0; i < 18; i++) items.set(i, next.get(i));
        if (toolEnergy > 0) io.github.jimbozoomer.jugcraft.tools.Chargeable.drain(items.get(toolIndex), toolEnergy);
        else if (!tool.isEmpty() && tool.isDamageableItem()) items.get(toolIndex).hurtAndBreak(1, actor, EquipmentSlot.MAINHAND);
        energy.setAmount(energy.getAmount() - 30); setChanged(); if (toolIndex >= 16) animate(toolIndex == 16 ? 9 : 10);
    }
    private void place(ServerPlayer actor, BlockPos target) {
        ItemStack stack = items.get(selected);
        if (energy.getAmount() < 20 || !allowed(actor, target) || !level.getBlockState(target).isAir()
                || !level.getFluidState(target).isEmpty()
                || !(stack.getItem() instanceof BlockItem item) || item.getBlock() instanceof EntityBlock
                || item.getBlock() instanceof DoorBlock || item.getBlock() instanceof BedBlock || item.getBlock() instanceof DoublePlantBlock)
            throw new IllegalArgumentException("Select a plain building block; target must be empty, permitted and powered.");
        BlockState state = item.getBlock().defaultBlockState();
        var shape = state.getCollisionShape(level, target);
        if (!state.canSurvive(level, target) || !shape.isEmpty() && !level.noCollision(shape.move(target.getX(), target.getY(), target.getZ()).bounds()))
            throw new IllegalArgumentException("Block cannot survive or would collide.");
        if (level.setBlock(target, state, Block.UPDATE_ALL)) { stack.shrink(1); energy.setAmount(energy.getAmount() - 20); setChanged(); }
    }
    private List<ItemStack> copyItems() { return new ArrayList<>(items.stream().map(ItemStack::copy).toList()); }
    private static int insert(List<ItemStack> target, ItemStack sample, int maximum) {
        int remaining = maximum;
        for (int i = 0; i < 16 && remaining > 0; i++) {
            ItemStack current = target.get(i);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, sample)) continue;
            int n = Math.min(remaining, sample.getMaxStackSize() - current.getCount()); if (n <= 0) continue;
            if (current.isEmpty()) target.set(i, sample.copyWithCount(n)); else current.grow(n); remaining -= n;
        }
        return maximum - remaining;
    }
    private void transfer(ServerPlayer actor, Direction side, int count, boolean pull) {
        BlockPos target = worldPosition.relative(side);
        if (!allowed(actor, target)||!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level,worldPosition,target)) throw new IllegalArgumentException("Protected or unloaded inventory.");
        Storage<ItemVariant> adjacent = ItemStorage.SIDED.find(level, target, side.getOpposite());
        if (adjacent == null) throw new IllegalArgumentException("No item transfer port on that side.");
        if (pull) {
            int scanned = 0;
            for (var view : adjacent) {
                if (++scanned > 64) break; if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                List<ItemStack> after = copyItems(); ItemStack sample = view.getResource().toStack();
                int n = insert(after, sample, (int) Math.min(count, view.getAmount())); if (n == 0) continue;
                try (Transaction tx = Transaction.openOuter()) {
                    if (adjacent.extract(view.getResource(), n, tx) != n) continue;
                    tx.commit(); for (int i = 0; i < 18; i++) items.set(i, after.get(i)); setChanged(); append("Pulled " + n); return;
                }
            }
        } else {
            ItemStack stack = items.get(selected); if (stack.isEmpty()) return;
            try (Transaction tx = Transaction.openOuter()) {
                int n = (int) adjacent.insert(ItemVariant.of(stack), Math.min(count, stack.getCount()), tx);
                if (n > 0) { tx.commit(); stack.shrink(n); setChanged(); append("Pushed " + n); }
            }
        }
    }
    private void sync() { if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
    private void updatePeripheralLight() {
        if (level == null || level.isClientSide()) return;
        boolean lit = kind().equals("printer") ? !items.get(17).isEmpty()
                : kind().equals("disk_drive") ? items.stream().anyMatch(WorkshopComputers::disk) : running;
        if (getBlockState().getValue(ComputerBlock.LIT) != lit)
            level.setBlock(worldPosition, getBlockState().setValue(ComputerBlock.LIT, lit), Block.UPDATE_CLIENTS);
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int n) { ItemStack stack = ContainerHelper.removeItem(items, slot, n); if (!stack.isEmpty()) { setChanged(); updatePeripheralLight(); if (slot >= 16 && turtle()) sync(); } return stack; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { if (slot >= 0 && slot < items.size()) { items.set(slot, stack); setChanged(); updatePeripheralLight(); if (slot >= 16 && turtle()) sync(); } }
    @Override public boolean stillValid(Player player) { return player instanceof ServerPlayer p && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64 && permitted(p); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!moving && level != null && !level.isClientSide()) { Containers.dropContents(level, pos, this); items.clear(); }
    }
    @Override public void setRemoved() { LOADED_MODEMS.remove(this); super.setRemoved(); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Nearby clients need the monitor text only, never computer programs, ownership or storage inventories.
        var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries); out.putBoolean("display_only", true);
        if (kind().startsWith("monitor")) { out.putString("output", output); out.putInt("display_colour",displayColour);saveMonitor(out);canvas.save(out); }
        if (turtle()) {
            out.store("left_upgrade", ItemStack.OPTIONAL_CODEC, upgrade(0)); out.store("right_upgrade", ItemStack.OPTIONAL_CODEC, upgrade(1));
            out.putInt("animation", animation); out.putLong("animation_start", animationStart);
            out.putBoolean("modem_active", !channels.isEmpty());
        }
        return out.buildResult();
    }
    @Override protected void loadAdditional(ValueInput in) {
        if (in.getBooleanOr("display_only", false)) {
            output = bounded(in.getStringOr("output", ""), 4096); displayColour=in.getIntOr("display_colour",0xE5F4DF)&0xFFFFFF; loadMonitor(in);canvas.load(in);
            items.set(16, in.read("left_upgrade", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
            items.set(17, in.read("right_upgrade", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
            animation = Math.clamp(in.getIntOr("animation", 0), 0, 10); animationStart = in.getLongOr("animation_start", -100);
            visualModemActive = in.getBooleanOr("modem_active", false); return;
        }
        super.loadAdditional(in); items = NonNullList.withSize(18, ItemStack.EMPTY); ContainerHelper.loadAllItems(in, items);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null); program = bounded(in.getStringOr("program", ""), WorkshopComputers.MAX_PROGRAM);
        output = bounded(in.getStringOr("output", ""), 4096); cursor = Math.clamp(in.getIntOr("cursor", 0), 0, MAX_LINES);
        waiting = Math.clamp(in.getIntOr("waiting", 0), 0, 1200); selected = Math.clamp(in.getIntOr("selected", 0), 0, 15);
        running = in.getBooleanOr("running", false); energy.setAmount(in.getLongOr("energy", 0));
        loadMonitor(in);
        canvas.load(in);
        workspace.load(in);
        channels.clear();
        for (int value : in.read("channels", com.mojang.serialization.Codec.intRange(0,65535).listOf(0,16)).orElse(List.of(0))) {
            if (value >= 0 && value <= 65535) channels.add(value); if (channels.size() == 16) break;
        }
        inbox.clear(); touches.clear(); replyChannel = 0;displayColour=in.getIntOr("display_colour",0xE5F4DF)&0xFFFFFF;
        for (Direction side : Direction.values()) signals[side.ordinal()] = Math.clamp(in.getIntOr("signal_" + side.getName(), 0), 0, 15);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); ContainerHelper.saveAllItems(out, items); if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
        out.putString("program", program); out.putString("output", output); out.putInt("cursor", cursor); out.putInt("waiting", waiting);
        out.putInt("selected", selected); out.putBoolean("running", running); out.putLong("energy", energy.getAmount());
        out.putInt("display_colour",displayColour);
        saveMonitor(out);
        canvas.save(out);
        workspace.save(out);
        out.store("channels", com.mojang.serialization.Codec.intRange(0,65535).listOf(0,16), List.copyOf(channels));
        for (Direction side : Direction.values()) out.putInt("signal_" + side.getName(), signals[side.ordinal()]);
    }
    private static String bounded(String text, int max) { return text.length() > max ? text.substring(0, max) : text; }
    private void saveMonitor(ValueOutput out) {
        if (monitorAnchor != null) out.putLong("monitor_anchor", monitorAnchor.asLong());
        out.putInt("monitor_width", monitorWidth); out.putInt("monitor_height", monitorHeight); out.putInt("monitor_x", monitorX); out.putInt("monitor_y", monitorY);
    }
    private void loadMonitor(ValueInput in) {
        monitorAnchor = in.getLong("monitor_anchor").map(BlockPos::of).orElse(null);
        monitorWidth = Math.clamp(in.getIntOr("monitor_width", 1), 1, 8); monitorHeight = Math.clamp(in.getIntOr("monitor_height", 1), 1, 6);
        monitorX = Math.clamp(in.getIntOr("monitor_x", 0), 0, 7); monitorY = Math.clamp(in.getIntOr("monitor_y", 0), 0, 5);
    }
}
