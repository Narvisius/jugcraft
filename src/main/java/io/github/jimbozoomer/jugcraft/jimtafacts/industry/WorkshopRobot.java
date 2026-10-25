package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.PoweredToolItem;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Small owner-bound flying worker. Every work step is bounded, loaded-only and checked against current claims. */
public final class WorkshopRobot extends PathfinderMob {
    private static final EntityDataAccessor<String> PROGRAM=SynchedEntityData.defineId(WorkshopRobot.class,EntityDataSerializers.STRING);
    private static final TagKey<Block> ORES=TagKey.create(Registries.BLOCK,Identifier.parse("c:ores"));
    public static final int CAPACITY=20_000,STEP_COST=32;
    private UUID owner;
    private BlockPos home=BlockPos.ZERO,area=BlockPos.ZERO,input,output,goal;
    private Direction inputSide=Direction.UP,outputSide=Direction.UP;
    private String action="";
    private int charge,cursor,goalAge,blueprintCell;
    private boolean running=true;
    public final SimpleContainer cargo=new SimpleContainer(27){
        @Override public boolean stillValid(Player player){return isAlive()&&ownedBy(player)&&distanceToSqr(player)<64;}
    };
    public final SingleFluidStorage fluid=new SingleFluidStorage(){
        @Override protected long getCapacity(FluidVariant variant){return 16*FluidConstants.BUCKET;}
    };
    public WorkshopRobot(EntityType<? extends WorkshopRobot> type,Level level){super(type,level);setNoGravity(true);setPersistenceRequired();}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PROGRAM,"clean");}
    public void configure(UUID owner,BlockPos home){this.owner=owner;this.home=home.immutable();this.area=home.above().immutable();}
    public String program(){return entityData.get(PROGRAM);}
    public void program(String value){if(WorkshopRobots.PROGRAMS.contains(value)){entityData.set(PROGRAM,value);goal=null;cursor=0;}}
    public boolean ownedBy(Player p){return owner!=null&&owner.equals(p.getUUID());}
    public int energy(){return charge;}
    public boolean link(int type,BlockPos pos,Direction face){
        if(pos.distSqr(home)>32*32)return false;
        if(type==0){output=pos.immutable();outputSide=face;}else if(type==1){input=pos.immutable();inputSide=face;}else{area=pos.immutable();cursor=0;}
        goal=null;return true;
    }
    private ServerPlayer actor(ServerLevel level){return Authority.answering(level,owner);}
    private boolean allowed(ServerLevel level,BlockPos pos){
        return level.hasChunkAt(pos)&&ClaimTransfers.allowed(level,home,pos)&&Authority.mayChange(level,actor(level),pos);
    }
    private boolean inArea(BlockPos p){return Math.abs(p.getX()-area.getX())<=4&&Math.abs(p.getZ()-area.getZ())<=4&&p.getY()>=area.getY()-1&&p.getY()<=area.getY()+7;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public void tick(){
        setNoGravity(true);
        if(level() instanceof ServerLevel level&&owner!=null){
            if(tickCount%10==0)charge(level);
            if(!running||actor(level)==null||!allowed(level,home)){goal=null;setDeltaMovement(Vec3.ZERO);}
            else {
                if(charge<STEP_COST+40){goal=home.above();action="dock";}
                else if(goal==null&&tickCount%10==0)choose(level);
                if(goal!=null){
                    Vec3 destination=Vec3.atCenterOf(goal).add(0,action.equals("dock")?0:.75,0),delta=destination.subtract(position());
                    if(!level.hasChunkAt(goal)||++goalAge>240){goal=null;goalAge=0;setDeltaMovement(Vec3.ZERO);}
                    else if(delta.lengthSqr()<2.25){setDeltaMovement(Vec3.ZERO);if(tickCount%10==0){work(level);goal=null;goalAge=0;}}
                    else if(charge>0){setDeltaMovement(delta.normalize().scale(.18));charge--;setYRot((float)(Math.atan2(-delta.x,delta.z)*180/Math.PI));}
                }else setDeltaMovement(Vec3.ZERO);
            }
        }
        super.tick();
    }
    private void charge(ServerLevel level){
        if(distanceToSqr(Vec3.atCenterOf(home))>9||!allowed(level,home)||charge>=CAPACITY)return;
        EnergyStorage storage=EnergyStorage.SIDED.find(level,home,Direction.UP);
        if(storage==null)return;
        try(Transaction tx=Transaction.openOuter()){long n=storage.extract(Math.min(512,CAPACITY-charge),tx);if(n>0){tx.commit();charge+=(int)n;}}
    }
    private boolean carrying(){for(int i=program().equals("crafter")?9:1;i<27;i++)if(!cargo.getItem(i).isEmpty())return true;return false;}
    private void select(BlockPos at,String task){goal=at.immutable();action=task;goalAge=0;}
    private void choose(ServerLevel level){
        String p=program();
        if(output!=null&&carrying()&&!Set.of("builder","stripes","planter").contains(p)){select(output,"unload");return;}
        if(fluid.amount>0&&output!=null){select(output,"fluid_out");return;}
        if(p.equals("carrier")||p.equals("delivery")){if(input!=null)select(input,"load");return;}
        if(p.equals("fluid_carrier")){if(input!=null)select(input,"fluid_in");return;}
        if(p.equals("crafter")){select(home.above(),"craft");return;}
        if(p.equals("builder")){chooseBlueprint(level);return;}
        if(p.equals("clean")||p.equals("picker")){var items=new ArrayList<ItemEntity>();
            level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),new AABB(area).inflate(5),
                    e->e.isAlive()&&!e.hasPickUpDelay()&&inArea(e.blockPosition()),items,24);
            for(var item:items){UUID reserved=((AlchemicalItemAccess)item).jugcraft$pickupTarget();if((reserved==null||reserved.equals(owner))&&allowed(level,item.blockPosition())){select(item.blockPosition(),"pickup");return;}}return;}
        if(Set.of("knight","butcher","bomber").contains(p)){var targets=new ArrayList<LivingEntity>();
            level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),new AABB(area).inflate(6),
                    e->validTarget(level,e),targets,16);if(!targets.isEmpty())select(targets.getFirst().blockPosition(),"fight");return;}
        for(int scan=0;scan<8;scan++){int i=Math.floorMod(cursor++,9*9*9);var pos=area.offset(i%9-4,(i/81)-1,(i/9)%9-4);
            if(!allowed(level,pos))continue;var state=level.getBlockState(pos);
            if(candidate(p,state,level,pos)){select(pos,p);return;}}
    }
    private boolean candidate(String p,BlockState state,ServerLevel level,BlockPos pos){
        return switch(p){
            case "miner" -> state.is(ORES);
            case "lumberjack" -> state.is(BlockTags.LOGS);
            case "leafcutter" -> state.is(BlockTags.LEAVES);
            case "shovelman" -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case "breaker" -> !state.isAir()&&!state.getCollisionShape(level,pos).isEmpty();
            case "harvester" -> state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state);
            case "farmer" -> (state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT_PATH))&&level.getBlockState(pos.above()).isAir();
            case "planter" -> state.isAir()&&level.getBlockState(pos.below()).is(Blocks.FARMLAND);
            case "builder","stripes" -> state.isAir()&&!level.getBlockState(pos.below()).isAir();
            case "pump" -> state.getFluidState().isSource()&&(state.getFluidState().is(FluidTags.WATER)||state.getFluidState().is(FluidTags.LAVA))&&state.getBlock() instanceof BucketPickup;
            default -> false;
        };
    }
    private void work(ServerLevel level){
        if(goal==null||charge<STEP_COST||!allowed(level,goal)||!allowed(level,blockPosition()))return;
        boolean changed=switch(action){
            case "dock" -> false;
            case "load" -> transfer(level,goal,inputSide,true);
            case "unload" -> transfer(level,goal,outputSide,false);
            case "fluid_in" -> transferFluid(level,goal,inputSide,true);
            case "fluid_out" -> transferFluid(level,goal,outputSide,false);
            case "pickup" -> pickup(level);
            case "fight" -> fight(level);
            case "craft" -> craft(level);
            case "blueprint" -> placeBlueprint(level,goal);
            case "planter","builder","stripes" -> place(level,goal,action.equals("planter"));
            case "farmer" -> till(level,goal);
            case "harvester" -> harvest(level,goal);
            case "pump" -> pump(level,goal);
            default -> mine(level,goal);
        };if(changed)charge-=STEP_COST;
    }
    private List<ItemStack> copy(){return new ArrayList<>(cargo.getItems().stream().map(ItemStack::copy).toList());}
    private void apply(List<ItemStack> stacks){for(int i=0;i<27;i++)cargo.setItem(i,stacks.get(i));}
    private static int insert(List<ItemStack> slots,ItemStack item,int first){
        int remaining=item.getCount();for(int i=first;i<slots.size()&&remaining>0;i++){var held=slots.get(i);
            if(!held.isEmpty()&&!ItemStack.isSameItemSameComponents(held,item))continue;
            int n=Math.min(remaining,item.getMaxStackSize()-held.getCount());if(n<=0)continue;
            if(held.isEmpty())slots.set(i,item.copyWithCount(n));else held.grow(n);remaining-=n;}return remaining;
    }
    private boolean mine(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);var tool=cargo.getItem(0);var actor=actor(level);
        if(!candidate(program(),state,level,pos)||state.isAir()||state.getDestroySpeed(level,pos)<0||state.getDestroySpeed(level,pos)>50
                ||level.getBlockEntity(pos)!=null||!toolReady(tool)||state.requiresCorrectToolForDrops()&&!tool.isCorrectToolForDrops(state))return false;
        List<ItemStack> next=copy();for(var stack:Block.getDrops(state,level,pos,null,actor,tool))if(insert(next,stack,1)>0)return false;
        if(!level.destroyBlock(pos,false,actor))return false;
        apply(next);damageTool(actor);return true;
    }
    private static boolean toolReady(ItemStack tool){return tool.getItem() instanceof PoweredToolItem powered?Chargeable.energy(tool)>=powered.energyPerBlock(tool):!(tool.getItem() instanceof IndustrialHandTools.ElectricHoe)||Chargeable.energy(tool)>=50;}
    private void damageTool(ServerPlayer actor){var tool=cargo.getItem(0);if(tool.getItem() instanceof PoweredToolItem powered)Chargeable.drain(tool,powered.energyPerBlock(tool));
        else if(tool.getItem() instanceof IndustrialHandTools.ElectricHoe)Chargeable.drain(tool,50);
        else if(!tool.isEmpty()&&tool.isDamageableItem())tool.hurtAndBreak(1,actor,EquipmentSlot.MAINHAND);}
    private boolean till(ServerLevel level,BlockPos pos){
        if(!candidate("farmer",level.getBlockState(pos),level,pos)||!cargo.getItem(0).is(ItemTags.HOES)||!toolReady(cargo.getItem(0)))return false;
        if(level.setBlock(pos,Blocks.FARMLAND.defaultBlockState(),Block.UPDATE_ALL)){damageTool(actor(level));return true;}return false;
    }
    private boolean place(ServerLevel level,BlockPos pos,boolean crops){
        if(!candidate(crops?"planter":"builder",level.getBlockState(pos),level,pos))return false;
        for(int i=1;i<27;i++){var stack=cargo.getItem(i);BlockState state;
            if(crops){Block crop=stack.getItem() instanceof BlockItem seed&&seed.getBlock() instanceof CropBlock?seed.getBlock():stack.is(Items.WHEAT_SEEDS)?Blocks.WHEAT:stack.is(Items.CARROT)?Blocks.CARROTS:stack.is(Items.POTATO)?Blocks.POTATOES:stack.is(Items.BEETROOT_SEEDS)?Blocks.BEETROOTS:null;
                if(crop==null)continue;state=crop.defaultBlockState();}
            else {if(!(stack.getItem() instanceof BlockItem item)||item.getBlock() instanceof EntityBlock||item.getBlock() instanceof DoorBlock
                        ||item.getBlock() instanceof BedBlock||item.getBlock() instanceof DoublePlantBlock)continue;state=item.getBlock().defaultBlockState();}
            var shape=state.getCollisionShape(level,pos);
            if(!state.canSurvive(level,pos)||!shape.isEmpty()&&!level.noCollision(shape.move(pos.getX(),pos.getY(),pos.getZ()).bounds()))continue;
            if(level.setBlock(pos,state,Block.UPDATE_ALL)){stack.shrink(1);return true;}
        }return false;
    }
    private boolean harvest(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);if(!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state))return false;
        var next=copy();for(var stack:Block.getDrops(state,level,pos,null,actor(level),cargo.getItem(0)))if(insert(next,stack,1)>0)return false;
        // Harvest consumes the complete crop; a planter supplies the next seed. No free mature-to-young loop.
        if(!level.destroyBlock(pos,false,actor(level)))return false;apply(next);return true;
    }
    private Blueprint blueprint(){return cargo.getItem(0).getItem() instanceof BlueprintItem?Blueprint.get(BlueprintItem.idOf(cargo.getItem(0)),false):null;}
    private void chooseBlueprint(ServerLevel level){
        Blueprint plan=blueprint();if(plan==null||plan.size()==0||plan.large())return;
        for(int scan=0;scan<8;scan++){int cell=Math.floorMod(cursor++,plan.size());BlockPos at=area.offset(plan.offset(cell,Rotation.NONE));
            if(at.distSqr(home)>1024||!allowed(level,at)||!level.getBlockState(at).isAir())continue;
            blueprintCell=cell;select(at,"blueprint");return;}
    }
    private boolean placeBlueprint(ServerLevel level,BlockPos at){
        Blueprint plan=blueprint();if(plan==null||blueprintCell>=plan.size()||!area.offset(plan.offset(blueprintCell,Rotation.NONE)).equals(at)||!level.getBlockState(at).isAir())return false;
        BlockState wanted=plan.state(blueprintCell,Rotation.NONE);TagKey<Block> tag=plan.anyOf(blueprintCell);
        for(int i=1;i<27;i++){ItemStack stack=cargo.getItem(i);if(!(stack.getItem() instanceof BlockItem item))continue;
            Block block=item.getBlock();if(block instanceof EntityBlock||block instanceof DoorBlock||block instanceof BedBlock||block instanceof DoublePlantBlock||!Blueprint.allowed(block))continue;
            if(tag==null?block!=wanted.getBlock():!block.defaultBlockState().is(tag))continue;
            BlockState state=tag==null?wanted:io.github.jimbozoomer.jugcraft.drone.BuildJobs.like(wanted,block);
            // A copied waterlogged property must not create water. Double slabs cost both physical slabs.
            if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false);
            int cost=state.hasProperty(SlabBlock.TYPE)&&state.getValue(SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE?2:1;
            var shape=state.getCollisionShape(level,at);if(stack.getCount()<cost||!state.canSurvive(level,at)||!shape.isEmpty()&&!level.noCollision(shape.move(at.getX(),at.getY(),at.getZ()).bounds()))continue;
            if(level.setBlock(at,state,Block.UPDATE_ALL)){stack.shrink(cost);return true;}
        }return false;
    }
    private boolean pickup(ServerLevel level){
        var list=new ArrayList<ItemEntity>();level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),getBoundingBox().inflate(2),
                e->e.isAlive()&&!e.hasPickUpDelay(),list,16);
        for(var item:list){UUID reserved=((AlchemicalItemAccess)item).jugcraft$pickupTarget();if(reserved!=null&&!reserved.equals(owner)||!allowed(level,item.blockPosition())||!hasLineOfSight(item))continue;
            var next=copy();var stack=item.getItem();int remaining=insert(next,stack,1);if(remaining==stack.getCount())continue;
            apply(next);if(remaining==0)item.discard();else item.setItem(stack.copyWithCount(remaining));return true;}return false;
    }
    private boolean transfer(ServerLevel level,BlockPos pos,Direction side,boolean pull){
        var storage=ItemStorage.SIDED.find(level,pos,side);if(storage==null)return false;
        if(pull){int inspected=0;for(var view:storage){if(++inspected>64)break;if(view.isResourceBlank()||view.getAmount()<=0)continue;
                if(program().equals("delivery")&&!cargo.getItem(0).isEmpty()&&!view.getResource().isOf(cargo.getItem(0).getItem()))continue;
                int count=(int)Math.min(16,view.getAmount());var next=copy();int accepted=count-insert(next,view.getResource().toStack(count),1);if(accepted==0)continue;
                try(var tx=Transaction.openOuter()){if(storage.extract(view.getResource(),accepted,tx)!=accepted)continue;tx.commit();apply(next);return true;}}
        }else for(int i=program().equals("crafter")?9:1;i<27;i++){var stack=cargo.getItem(i);if(stack.isEmpty())continue;
            try(var tx=Transaction.openOuter()){long n=storage.insert(ItemVariant.of(stack),Math.min(16,stack.getCount()),tx);if(n>0){tx.commit();stack.shrink((int)n);return true;}}}
        return false;
    }
    private boolean transferFluid(ServerLevel level,BlockPos pos,Direction side,boolean pull){
        var storage=FluidStorage.SIDED.find(level,pos,side);if(storage==null)return false;
        try(var tx=Transaction.openOuter()){long moved=StorageUtil.move(pull?storage:fluid,pull?fluid:storage,v->true,FluidConstants.BUCKET,tx);if(moved>0){tx.commit();return true;}}return false;
    }
    private boolean pump(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);if(!candidate("pump",state,level,pos))return false;
        var variant=FluidVariant.of(state.getFluidState().is(FluidTags.WATER)?Fluids.WATER:Fluids.LAVA);
        try(var tx=Transaction.openOuter()){if(fluid.insert(variant,FluidConstants.BUCKET,tx)!=FluidConstants.BUCKET)return false;
            var bucket=((BucketPickup)state.getBlock()).pickupBlock(actor(level),level,pos,state);if(bucket.isEmpty())return false;tx.commit();return true;}
    }
    private boolean validTarget(ServerLevel level,LivingEntity entity){
        if(entity==this||!entity.isAlive()||!inArea(entity.blockPosition())||!allowed(level,entity.blockPosition())||entity instanceof Player||entity instanceof TamableAnimal)return false;
        return (program().equals("butcher")?entity instanceof Animal animal&&!animal.isBaby():entity instanceof Enemy)
                &&Authority.mayStrike(level,actor(level),owner,entity);
    }
    private boolean fight(ServerLevel level){
        if(Authority.isStandIn(actor(level)))return false;
        var targets=new ArrayList<LivingEntity>();level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(3),e->validTarget(level,e),targets,8);
        if(targets.isEmpty())return false;
        boolean bomber=program().equals("bomber");if(!toolReady(cargo.getItem(0))||bomber&&!cargo.getItem(0).is(Items.GUNPOWDER))return false;
        boolean hit=false;for(var target:targets){if(!hasLineOfSight(target))continue;hit|=target.hurtServer(level,damageSources().mobAttack(this),bomber?6:4);if(!bomber)break;}
        if(hit){if(bomber){cargo.getItem(0).shrink(1);level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,getX(),getY(),getZ(),1,0,0,0,0);}else damageTool(actor(level));}return hit;
    }
    private boolean craft(ServerLevel level){
        var grid=CraftingInput.of(3,3,cargo.getItems().subList(0,9));var found=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level);
        if(found.isEmpty())return false;var recipe=found.get().value();var result=recipe.assemble(grid);if(result.isEmpty())return false;
        var next=copy();for(int i=0;i<9;i++)if(!next.get(i).isEmpty())next.get(i).shrink(1);
        if(insert(next,result,9)>0)return false;
        for(var remaining:recipe.getRemainingItems(grid))if(!remaining.isEmpty()&&insert(next,remaining,9)>0)return false;
        apply(next);return true;
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(!(level() instanceof ServerLevel level))return InteractionResult.SUCCESS;
        if(owner==null&&Authority.mayChange(level,player,blockPosition()))configure(player.getUUID(),blockPosition().below());
        if(!ownedBy(player)||!Authority.mayChange(level,player,blockPosition()))return InteractionResult.FAIL;
        var held=player.getItemInHand(hand);String board=WorkshopRobots.program(held);
        if(board!=null){program(board);player.sendOverlayMessage(Component.literal("Robot program: "+board+". Cards are reusable."));return InteractionResult.SUCCESS;}
        if(held.is(WorkshopRobots.LINKER)){CustomData.update(DataComponents.CUSTOM_DATA,held,t->t.putString("Robot",getUUID().toString()));player.sendOverlayMessage(Component.literal("Robot selected. Use on a block to link; shift-use in the air switches output/input/work area."));return InteractionResult.SUCCESS;}
        if(held.is(Items.REDSTONE)&&charge<CAPACITY){charge=Math.min(CAPACITY,charge+1000);held.consume(1,player);return InteractionResult.SUCCESS;}
        if(player.isShiftKeyDown()){running=!running;goal=null;player.sendOverlayMessage(Component.literal(running?"Robot resumed.":"Robot paused."));return InteractionResult.SUCCESS;}
        player.openMenu(new SimpleMenuProvider((id,inventory,p)->ChestMenu.threeRows(id,inventory,cargo),
                Component.literal(program()+" robot · "+charge+" JE")));return InteractionResult.SUCCESS;
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit){
        super.dropCustomDeathLoot(level,source,recentlyHit);for(var stack:cargo.removeAllItems())if(!stack.isEmpty())spawnAtLocation(level,stack);
        spawnAtLocation(level,WorkshopRobots.SHELLS.get(program()));
        // Internal fluid cannot become unbounded item drops; empty the finite tank on destruction.
        fluid.variant=FluidVariant.blank();fluid.amount=0;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);if(owner!=null)out.store("RobotOwner",UUIDUtil.CODEC,owner);out.store("Home",BlockPos.CODEC,home);out.store("Area",BlockPos.CODEC,area);
        if(input!=null)out.store("Input",BlockPos.CODEC,input);if(output!=null)out.store("Output",BlockPos.CODEC,output);
        out.putInt("InputFace",inputSide.ordinal());out.putInt("OutputFace",outputSide.ordinal());out.putString("Program",program());out.putInt("Charge",charge);out.putBoolean("Running",running);
        ContainerHelper.saveAllItems(out,cargo.getItems());out.store("Fluid",FluidVariant.CODEC,fluid.variant);out.putLong("FluidAmount",fluid.amount);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);owner=in.read("RobotOwner",UUIDUtil.CODEC).orElse(null);home=in.read("Home",BlockPos.CODEC).orElse(blockPosition());area=in.read("Area",BlockPos.CODEC).filter(p->p.distSqr(home)<=1024).orElse(home.above());
        input=in.read("Input",BlockPos.CODEC).filter(p->p.distSqr(home)<=1024).orElse(null);output=in.read("Output",BlockPos.CODEC).filter(p->p.distSqr(home)<=1024).orElse(null);
        inputSide=Direction.values()[Math.floorMod(in.getInt("InputFace").orElse(1),6)];outputSide=Direction.values()[Math.floorMod(in.getInt("OutputFace").orElse(1),6)];
        program(in.getStringOr("Program","clean"));charge=Math.clamp(in.getInt("Charge").orElse(0),0,CAPACITY);running=in.getBooleanOr("Running",true);
        ContainerHelper.loadAllItems(in,cargo.getItems());fluid.variant=in.read("Fluid",FluidVariant.CODEC).orElse(FluidVariant.blank());fluid.amount=fluid.variant.isBlank()?0:Math.clamp(in.getLong("FluidAmount").orElse(0L),0,16*FluidConstants.BUCKET);
    }
}
