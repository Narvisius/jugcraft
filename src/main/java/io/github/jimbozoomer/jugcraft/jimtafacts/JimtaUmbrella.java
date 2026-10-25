package io.github.jimbozoomer.jugcraft.jimtafacts;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
/** Original held/open model with native shield handling; gliding is server bounded in JimtaAbilities. */
public final class JimtaUmbrella extends ShieldItem {
    public JimtaUmbrella(Properties properties) {
        super(properties.delayedComponent(DataComponents.BLOCKS_ATTACKS,registry->new BlocksAttacks(.25F,1F,
                List.of(new BlocksAttacks.DamageReduction(90F,Optional.empty(),0F,1F)),
                new BlocksAttacks.ItemDamageFunction(3F,0F,0F),Optional.of(registry.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                Optional.of(SoundEvents.SHIELD_BLOCK),Optional.of(SoundEvents.SHIELD_BREAK))));
    }
}
