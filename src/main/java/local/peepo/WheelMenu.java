package local.peepo;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative wheel readouts; the wheel has no fuel or item inventory. */
public final class WheelMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 7;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public WheelMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public WheelMenu(int id, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(GeneratorWheel.MENU, id);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.access = access;
        addDataSlots(data);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }

    public int energy() { return data.get(0); }
    public boolean running() { return data.get(1) != 0; }
    public int reservePercent() { return data.get(2); }
    public int outputRate() { return data.get(3); }
    public int occupantId() { return (data.get(5) & 0xffff) | ((data.get(6) & 0xffff) << 16); }
    public int occupantKind() { return data.get(4); }

    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, GeneratorWheel.BLOCK);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!(index < 27 ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false)))
            return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
