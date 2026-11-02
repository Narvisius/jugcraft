package io.github.jimbozoomer.jugcraft.server;
import java.util.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
/** Bounded, dimension-aware operator cuboids sharing the town protection pipeline. */
public final class OperatorRegions {
    private record Selection(String dimension,BlockPos a,BlockPos b){}
    private static final Map<UUID,Selection> SELECTIONS=new HashMap<>();
    public static boolean tool(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("jugcraft_protection_tool",false);}
    public static boolean contains(CompoundTag t,BlockPos p){return p.getX()>=t.getIntOr("x1",0)&&p.getX()<=t.getIntOr("x2",-1)&&p.getY()>=t.getIntOr("y1",0)&&p.getY()<=t.getIntOr("y2",-1)&&p.getZ()>=t.getIntOr("z1",0)&&p.getZ()<=t.getIntOr("z2",-1);}
    public static boolean protects(Level level,BlockPos p){if(level.isClientSide()||level.getServer()==null)return false;String dim=level.dimension().identifier().toString();return ServerUtilities.get(level.getServer()).records.entrySet().stream().anyMatch(e->e.getKey().startsWith("region:")&&dim.equals(e.getValue().getStringOr("dimension",""))&&contains(e.getValue(),p));}
    public static boolean touchesChunk(Level level,int x,int z){if(level.isClientSide()||level.getServer()==null)return false;String dim=level.dimension().identifier().toString();for(var e:ServerUtilities.get(level.getServer()).records.entrySet()){var t=e.getValue();if(e.getKey().startsWith("region:")&&dim.equals(t.getStringOr("dimension",""))&&t.getIntOr("x1",0)<=x+15&&t.getIntOr("x2",-1)>=x&&t.getIntOr("z1",0)<=z+15&&t.getIntOr("z2",-1)>=z)return true;}return false;}
    public static boolean crossesBoundary(Level level,BlockPos a,BlockPos b){if(level.isClientSide()||level.getServer()==null)return false;String dim=level.dimension().identifier().toString();for(var e:ServerUtilities.get(level.getServer()).records.entrySet())if(e.getKey().startsWith("region:")&&dim.equals(e.getValue().getStringOr("dimension",""))&&contains(e.getValue(),a)!=contains(e.getValue(),b))return true;return false;}
    private static int select(ServerPlayer p,BlockPos pos,boolean second){String dim=p.level().dimension().identifier().toString();var old=SELECTIONS.get(p.getUUID());if(old==null||!old.dimension.equals(dim))old=new Selection(dim,pos,pos);SELECTIONS.put(p.getUUID(),new Selection(dim,second?old.a:pos,second?pos:old.b));ServerUtilities.tell(p,"Corner "+(second?2:1)+": "+pos.toShortString()+". /protect create <name> to protect.");return 1;}
    public static void register(){
        CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(Commands.literal("protect").requires(ServerUtilities::op)
            .executes(c->{c.getSource().sendSuccess(()->Component.literal("/protect tool | pos1 | pos2 | create <name> | remove <name> | list. Tool: left corner 1, right corner 2; gold selection, red protected boundary. Doors remain public; containers protected."),false);return 1;})
            .then(Commands.literal("tool").executes(c->{var p=c.getSource().getPlayerOrException();var stack=new ItemStack(Items.GOLDEN_HOE);CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putBoolean("jugcraft_protection_tool",true));stack.set(DataComponents.CUSTOM_NAME,Component.literal("Protection Surveyor"));if(!p.getInventory().add(stack))p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);return 1;}))
            .then(Commands.literal("pos1").executes(c->{var p=c.getSource().getPlayerOrException();return select(p,p.blockPosition(),false);}))
            .then(Commands.literal("pos2").executes(c->{var p=c.getSource().getPlayerOrException();return select(p,p.blockPosition(),true);}))
            .then(Commands.literal("create").then(Commands.argument("name",StringArgumentType.word()).executes(c->create(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"name")))))
            .then(Commands.literal("remove").then(Commands.argument("name",StringArgumentType.word()).executes(c->{var data=ServerUtilities.get(c.getSource().getServer());data.records.remove("region:"+StringArgumentType.getString(c,"name"));data.setDirty();return 1;})))
            .then(Commands.literal("list").executes(c->{c.getSource().sendSuccess(()->Component.literal(String.join(", ",ServerUtilities.get(c.getSource().getServer()).records.keySet().stream().filter(k->k.startsWith("region:")).toList())),false);return 1;}))));
        AttackBlockCallback.EVENT.register((p,l,h,pos,dir)->{if(!tool(p.getItemInHand(h)))return InteractionResult.PASS;if(p instanceof ServerPlayer sp&&ServerUtilities.op(sp.createCommandSourceStack()))select(sp,pos,false);return InteractionResult.SUCCESS;});
        UseBlockCallback.EVENT.register((p,l,h,hit)->{if(!tool(p.getItemInHand(h)))return InteractionResult.PASS;if(p instanceof ServerPlayer sp&&ServerUtilities.op(sp.createCommandSourceStack()))select(sp,hit.getBlockPos(),true);return InteractionResult.SUCCESS;});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(s.overworld().getGameTime()%20!=0)return;for(var p:s.getPlayerList().getPlayers())if(ServerUtilities.op(p.createCommandSourceStack())&&(tool(p.getMainHandItem())||tool(p.getOffhandItem())))visualize(p);});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->SELECTIONS.clear());
    }
    static int create(ServerPlayer p,String name){var sel=SELECTIONS.get(p.getUUID());if(sel==null||!sel.dimension.equals(p.level().dimension().identifier().toString())||!name.matches("[a-zA-Z0-9_-]{1,48}")){ServerUtilities.tell(p,"Select two corners in this dimension; use a short region name.");return 0;}var data=ServerUtilities.get(p.level().getServer());if(data.records.containsKey("region:"+name)){ServerUtilities.tell(p,"That name exists; remove it explicitly before replacing.");return 0;}if(data.records.keySet().stream().filter(k->k.startsWith("region:")).count()>=256){ServerUtilities.tell(p,"Region limit reached (256).");return 0;}var t=box(sel.a,sel.b);t.putString("key","region:"+name);t.putString("dimension",sel.dimension);data.records.put("region:"+name,t);data.setDirty();ServerUtilities.tell(p,"Protected region "+name+". Creative operators can edit; normal town interaction rules apply.");return 1;}
    public static CompoundTag box(BlockPos a,BlockPos b){var t=new CompoundTag();t.putInt("x1",Math.min(a.getX(),b.getX()));t.putInt("x2",Math.max(a.getX(),b.getX()));t.putInt("y1",Math.min(a.getY(),b.getY()));t.putInt("y2",Math.max(a.getY(),b.getY()));t.putInt("z1",Math.min(a.getZ(),b.getZ()));t.putInt("z2",Math.max(a.getZ(),b.getZ()));return t;}
    private static void visualize(ServerPlayer p){var dim=p.level().dimension().identifier().toString();var data=ServerUtilities.get(p.level().getServer());int drawn=0;for(var e:data.records.entrySet())if(e.getKey().startsWith("region:")&&dim.equals(e.getValue().getStringOr("dimension",""))&&drawn++<256)outline(p,e.getValue(),0xFF5555);
        // Show the actual nearby boundary too, including irregular spawn-city columns.
        int marks=0;var center=p.blockPosition();for(int x=-12;x<=12;x+=2)for(int z=-12;z<=12;z+=2){var at=center.offset(x,0,z);if(marks>=80)break;boolean inside=io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(p.level(),at);if(!inside)continue;boolean edge=false;for(var dir:net.minecraft.core.Direction.Plane.HORIZONTAL)if(!io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(p.level(),at.relative(dir,2)))edge=true;if(edge){marks++;p.level().sendParticles(p,new DustParticleOptions(0xFF5555,1),false,false,at.getX()+.5,at.getY()+.3,at.getZ()+.5,1,0,0,0,0);}}
        var sel=SELECTIONS.get(p.getUUID());if(sel!=null&&sel.dimension.equals(dim))outline(p,box(sel.a,sel.b),0xFFD855);
        boolean protectedHere=io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(p.level(),p.blockPosition());p.sendOverlayMessage(Component.literal(protectedHere?"PROTECTED — red borders | gold: selection":"UNPROTECTED — red borders | gold: selection"));
    }
    private static void outline(ServerPlayer p,CompoundTag t,int color){double[] lo={t.getIntOr("x1",0),t.getIntOr("y1",0),t.getIntOr("z1",0)},hi={t.getIntOr("x2",0)+1.,t.getIntOr("y2",0)+1.,t.getIntOr("z2",0)+1.};for(int axis=0;axis<3;axis++){int b=(axis+1)%3,c=(axis+2)%3;for(int side=0;side<4;side++){double[] v=lo.clone();v[b]=(side&1)==0?lo[b]:hi[b];v[c]=(side&2)==0?lo[c]:hi[c];double start=Math.max(lo[axis],(axis==0?p.getX():axis==1?p.getY():p.getZ())-32),end=Math.min(hi[axis],start+64);for(double q=start;q<=end;q+=2){v[axis]=q;if(p.distanceToSqr(v[0],v[1],v[2])<32*32)p.level().sendParticles(p,new DustParticleOptions(color,1),false,false,v[0],v[1],v[2],1,0,0,0,0);}}}}
}
