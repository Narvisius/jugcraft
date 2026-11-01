package io.github.jimbozoomer.jugcraft.menagerie;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;

/** Original globe spin/comparator/shearing, on the shared household block entity. */
public final class OwnerGlobes {
    private OwnerGlobes(){}
    private static final java.util.Map<ServerLevel,java.util.Map<Integer,byte[]>> PATTERNS=new java.util.WeakHashMap<>();
    private static int season(){var date=io.github.jimbozoomer.jugcraft.season.JugcraftSeasons.settings().effectiveDate(io.github.jimbozoomer.jugcraft.season.JugcraftSeasons.date());return date.getMonthValue()==12?1:date.getMonthValue()==4&&date.getDayOfMonth()==22?2:0;}
    static void pattern(ServerLevel level,OwnerSupplementData data){
        int season=season();if(data.globePixels.length==512&&data.globeSeason==season)return;
        var maps=PATTERNS.computeIfAbsent(level,l->new java.util.HashMap<>());
        data.globePixels=maps.computeIfAbsent(season,k->{
            // Transmit only artwork, never the world seed. This is the source's decorative map, not explored terrain.
            var grid=OwnerGlobePattern.generate(level.getSeed()^((long)level.dimension().identifier().hashCode()<<32),k);
            byte[] pixels=new byte[512];for(int x=0;x<32;x++)for(int y=0;y<16;y++)pixels[x*16+y]=grid[x][y];return pixels;
        }).clone();data.globeSeason=season;data.changed();
    }
    public static float remaining(OwnerSupplementData data,double time){
        double age=Math.max(0,time-data.pulseUntil);
        return (float)Math.max(0,(data.globeSpin+35D/3)*Math.pow(.94,age)-35D/3);
    }
    public static float rotation(OwnerSupplementData data,double time){return (3-Integer.parseInt(HouseholdBlock.value(data.getBlockState(),"rotation")))*90+remaining(data,time);}
    public static int signal(OwnerSupplementData data){return remaining(data,data.getLevel().getGameTime())>0?15:4-Integer.parseInt(HouseholdBlock.value(data.getBlockState(),"rotation"));}
    static void spin(OwnerSupplementData data){
        if(!(data.getLevel() instanceof ServerLevel level))return;
        data.globeSpin=Math.min(9000,remaining(data,level.getGameTime())+450);data.pulseUntil=level.getGameTime();
        int rotation=Integer.parseInt(HouseholdBlock.value(data.getBlockState(),"rotation"));
        level.setBlockAndUpdate(data.getBlockPos(),HouseholdBlock.set(data.getBlockState(),"rotation",Integer.toString((rotation+1)%4)));
        data.changed();level.playSound(null,data.getBlockPos(),OwnerSupplementaries.SOUNDS.get("globe_spin"),SoundSource.BLOCKS,.65F,.9F+level.getRandom().nextFloat()*.2F);
    }
    static void tick(ServerLevel level,OwnerSupplementData data){
        pattern(level,data);
        boolean powered=level.hasNeighborSignal(data.getBlockPos()),was=HouseholdBlock.flag(data.getBlockState(),"powered");
        if(powered!=was){level.setBlockAndUpdate(data.getBlockPos(),HouseholdBlock.set(data.getBlockState(),"powered",Boolean.toString(powered)));if(powered)spin(data);}
        if(data.globeSpin>0&&remaining(data,level.getGameTime())<=0){data.globeSpin=0;data.changed();}
    }
    static InteractionResult use(OwnerSupplementData data,Player player,ItemStack held,InteractionHand hand){
        boolean sepia=data.block().name.equals("globe_sepia");
        if(!sepia&&held.is(OwnerSupplementaries.ITEMS.get("antique_ink"))||sepia&&held.is(OwnerSupplementaries.ITEMS.get("soap"))){
            var packed=data.packed();var original=data.getBlockState();var replacement=OwnerSupplementaries.BLOCKS.get(sepia?"globe":"globe_sepia").defaultBlockState();for(var property:original.getProperties())replacement=HouseholdBlock.set(replacement,property.getName(),HouseholdBlock.value(original,property.getName()));
            if(data.getLevel().setBlockAndUpdate(data.getBlockPos(),replacement)&&data.getLevel().getBlockEntity(data.getBlockPos()) instanceof OwnerSupplementData next){next.owner=data.owner;next.restore(packed);held.consume(1,player);return InteractionResult.SUCCESS;}
            data.getLevel().setBlockAndUpdate(data.getBlockPos(),original);if(data.getLevel().getBlockEntity(data.getBlockPos()) instanceof OwnerSupplementData restored){restored.owner=data.owner;restored.restore(packed);}return InteractionResult.FAIL;
        }
        if(held.getItem() instanceof ShearsItem){data.globeSheared=!data.globeSheared;data.changed();data.getLevel().playSound(null,data.getBlockPos(),SoundEvents.SHEEP_SHEAR,SoundSource.BLOCKS,1,1);return InteractionResult.SUCCESS;}
        if(held.is(Items.NAME_TAG)&&held.has(DataComponents.CUSTOM_NAME)){data.setCustomName(held.get(DataComponents.CUSTOM_NAME));held.consume(1,player);data.changed();return InteractionResult.SUCCESS;}
        spin(data);var p=data.getBlockPos();player.sendOverlayMessage(Component.literal("Coordinates: "+p.getX()+", "+p.getY()+", "+p.getZ()));return InteractionResult.SUCCESS;
    }
    static void register(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(!(entity instanceof WanderingTrader trader)||trader.entityTags().contains("jugcraft.supps_globes"))return;
            var offers=trader.getOffers();
            for(String name:new String[]{"globe","globe_sepia"}){
                var item=OwnerSupplementaries.ITEMS.get(name);if(offers.stream().noneMatch(o->o.getResult().is(item)))
                    offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,name.equals("globe")?10:12),new ItemStack(item),3,0,.05F));
            }
            trader.addTag("jugcraft.supps_globes");
        });
    }
}
