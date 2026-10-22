package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Server-owned, opt-in convenience catalogue. Valuation and saved knowledge stay independent. */
public final class TabletOutputs {
    public static final TagKey<Item> ALLOWED = TagKey.create(Registries.ITEM, Jugcraft.id("tablet_outputs"));
    private static long revision;
    static {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {if(success)revision++;});
    }
    public static long revision(){return revision;}
    private TabletOutputs() {}
    public static boolean allowed(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ALLOWED) && Assaying.eligibility(stack).isEmpty();
    }
}
