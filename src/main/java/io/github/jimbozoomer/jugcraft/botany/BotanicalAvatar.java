package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Six source Avatar rod behaviors, powered from the real machine mana buffer. */
public final class BotanicalAvatar {
    private static AABB bounds(BlockPos a,BlockPos b){return new AABB(a.getX(),a.getY(),a.getZ(),b.getX(),b.getY(),b.getZ());}

    public static final Set<String> RODS=Set.of("dirt_rod","tornado_rod","divining_rod","fire_rod","missile_rod","rainbow_rod");
    private final BotanicalWorkshop host;private int ticks;private final Map<UUID,Long> boosted=new HashMap<>();
    BotanicalAvatar(BotanicalWorkshop host){this.host=host;}
    public static boolean rod(ItemStack stack){return RODS.stream().anyMatch(n->stack.is(Jugtania.item(n)));}
    InteractionResult use(ServerPlayer p,InteractionHand hand){var held=p.getItemInHand(hand);
        if(held.isEmpty()){if(!host.getItem(0).isEmpty()){p.getInventory().placeItemBackInInventory(host.removeItemNoUpdate(0),net.minecraft.util.Prediction.SERVER_ONLY);host.changed();}else p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Give the Avatar a rod, then feed it mana. Redstone pauses it."));return InteractionResult.SUCCESS;}
        if(!rod(held)||!host.getItem(0).isEmpty())return InteractionResult.PASS;host.setItem(0,held.copyWithCount(1));if(!p.hasInfiniteMaterials())held.shrink(1);host.changed();return InteractionResult.SUCCESS;
    }
    private boolean allowed(ServerPlayer p,BlockPos at){var l=p.level();return l.hasChunkAt(at)&&l.getWorldBorder().isWithinBounds(at)&&Authority.mayChange(l,p,at)&&ClaimTransfers.allowed(l,host.getBlockPos(),at);}
    void tick(ServerLevel level,ServerPlayer owner){
        boosted.entrySet().removeIf(e->e.getValue()<=level.getGameTime());if(level.hasNeighborSignal(host.getBlockPos()))return;ticks=(ticks+1)%24000;
        var rod=host.getItem(0);if(rod.isEmpty())return;var at=host.getBlockPos();var direction=host.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        if(rod.is(Jugtania.item("dirt_rod"))&&ticks%4==0&&level.getRandom().nextInt(8)==0){var dest=at.relative(direction);if(host.mana()>=75&&allowed(owner,dest)&&level.isEmptyBlock(dest)&&level.noCollision(new AABB(dest))&&level.setBlockAndUpdate(dest,Blocks.DIRT.defaultBlockState()))host.draw(75);}
        else if(rod.is(Jugtania.item("fire_rod"))&&ticks%300==0&&host.mana()>=900){if(BotanicalRodEffect.ring(owner,Vec3.atBottomCenterOf(at)))host.draw(900);}
        else if(rod.is(Jugtania.item("missile_rod"))&&ticks%3==0&&host.mana()>=40){
            var enemies=level.getEntitiesOfClass(LivingEntity.class,new AABB(at).inflate(12),e->BotanicalCombatRods.enemy(owner,e)&&allowed(owner,e.blockPosition()));
            enemies.sort(Comparator.comparingDouble(e->e.blockPosition().distSqr(at)));if(!enemies.isEmpty()&&BotanicalRodEffect.missile(owner,enemies.getFirst(),Vec3.atCenterOf(at).add(0,2,0)))host.draw(40);
        }else if(rod.is(Jugtania.item("divining_rod"))&&ticks%200==0&&host.mana()>=3000){
            boolean any=false;int count=0;for(var p:level.getEntitiesOfClass(ServerPlayer.class,new AABB(at).inflate(18))){if(++count>8)break;if(allowed(owner,p.blockPosition())&&Authority.mayChange(level,p,at))any|=BotanicalDiviner.begin(p,at,18);}if(any)host.draw(3000);
        }else if(rod.is(Jugtania.item("tornado_rod"))&&host.mana()>=350){
            for(var p:level.getEntitiesOfClass(ServerPlayer.class,new AABB(at).inflate(5.5,3.5,5.5))){if(boosted.containsKey(p.getUUID())||p.isShiftKeyDown()||p.isSpectator()||!p.isAlive()||!allowed(owner,p.blockPosition())||!Authority.mayChange(level,p,at))continue;
                var motion=p.getDeltaMovement();if(p.isFallFlying()?motion.length()<=.2||motion.length()>=5:motion.y<=.3||motion.y>=2)continue;
                if(!host.draw(350))break;BotanicalRods.avatarBoost(p);boosted.put(p.getUUID(),level.getGameTime()+80);level.sendParticles(ParticleTypes.CLOUD,p.getX(),p.getY(),p.getZ(),8,.25,.25,.25,.05);
            }
        }else if(rod.is(Jugtania.item("rainbow_rod"))&&ticks%5==0&&host.mana()>=100){
            var end=at.relative(direction,20);var area=bounds(at,end).inflate(direction.getAxis()==Direction.Axis.X?0:1,1,direction.getAxis()==Direction.Axis.Z?0:1);int changed=0;
            outer:for(var p:level.getEntitiesOfClass(ServerPlayer.class,area)){if(p.isSpectator()||!p.isAlive()||!Authority.mayChange(level,p,at))continue;var below=p.blockPosition().below();
                for(var floor:BlockPos.betweenClosed(below.offset(-2,0,-2),below.offset(2,0,2))){if(changed>=32||host.mana()<4)break outer;if(!area.contains(Vec3.atCenterOf(floor.above()))||!allowed(owner,floor))continue;var state=level.getBlockState(floor);
                    if(!state.isAir()&&!(state.is(Jugtania.block("bifrost"))&&state.getValue(BotanicalTerrainRods.Bifrost.AVATAR_LIFE)>0))continue;
                    if(level.setBlockAndUpdate(floor,Jugtania.block("bifrost").defaultBlockState().setValue(BotanicalTerrainRods.Bifrost.AVATAR_LIFE,10))){host.draw(4);changed++;}
                }
            }
        }
    }
    void save(ValueOutput out){out.putInt("avatar_ticks",ticks);var entries=out.childrenList("avatar_boosts");boosted.forEach((id,until)->{var e=entries.addChild();e.store("player",UUIDUtil.CODEC,id);e.putLong("until",until);});}
    void load(ValueInput in){ticks=Math.clamp(in.getIntOr("avatar_ticks",0),0,24000);boosted.clear();for(var e:in.childrenListOrEmpty("avatar_boosts")){var id=e.read("player",UUIDUtil.CODEC).orElse(null);if(id!=null&&boosted.size()<128)boosted.put(id,e.getLongOr("until",0));}}
    public static final class Block extends BotanicalBlock {
        Block(Properties p){super("avatar",p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.HORIZONTAL_FACING);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite());}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.level.block.Block.box(3,0,3,13,16,13);}
    }
}
