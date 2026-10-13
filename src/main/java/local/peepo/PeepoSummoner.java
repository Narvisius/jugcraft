package local.peepo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

public final class PeepoSummoner extends Item {
    private final boolean jughead;
    public PeepoSummoner(Properties properties) { this(properties,false); }
    public PeepoSummoner(Properties properties, boolean jughead) { super(properties); this.jughead=jughead; }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context,
        net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> lines,
        net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(Component.translatable("message.peepo.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSpectator() || !player.getAbilities().mayBuild) return InteractionResult.FAIL;
        if (!(context.getLevel() instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = server.getBlockState(clicked).getCollisionShape(server, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
        ItemStack stack = context.getItemInHand();
        if (pos.distToCenterSqr(player.position()) > 64 || !server.hasChunkAt(pos)
            || !server.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)
            || !server.getWorldBorder().isWithinBounds(pos)) return InteractionResult.FAIL;
        if (server.getEntitiesOfClass(PeepoEntity.class, new AABB(pos).inflate(16)).size() >= 8) {
            player.sendOverlayMessage(Component.translatable("message.peepo.limit")); return InteractionResult.FAIL;
        }
        PeepoEntity peepo = (jughead ? PeepoMod.JUGHEAD : PeepoMod.PEEPO).create(server, EntitySpawnReason.SPAWN_ITEM_USE);
        if (peepo == null) return InteractionResult.FAIL;
        peepo.snapTo(pos.getX()+.5, pos.getY(), pos.getZ()+.5, player.getYRot()+180, 0);
        peepo.yBodyRot = peepo.getYRot(); peepo.setYHeadRot(peepo.getYRot());
        if (!server.noCollision(peepo) || !server.getFluidState(pos).isEmpty()
            || !server.getBlockState(pos.below()).isSolidRender()) {
            player.sendOverlayMessage(Component.translatable("message.peepo.space")); return InteractionResult.FAIL;
        }

        if (!server.addFreshEntity(peepo)) return InteractionResult.FAIL;
        stack.consume(1, player);
        server.sendParticles(ParticleTypes.HEART, peepo.getX(), peepo.getY()+.65, peepo.getZ(), 6,.3,.3,.3,0);

        return InteractionResult.SUCCESS;
    }
}
