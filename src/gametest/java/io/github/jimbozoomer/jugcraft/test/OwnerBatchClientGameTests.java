package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.aircraft.*;
import io.github.jimbozoomer.jugcraft.grave.*;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import io.github.jimbozoomer.jugcraft.town.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Isolated visual and real-dimension checks. Never opens or changes the owner's Modrinth profile. */
public final class OwnerBatchClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            server.runCommand("gamerule minecraft:send_command_feedback false");
            c.getInput().pressKey(options->options.keyToggleGui);
            var origin=c.computeOnClient(mc->mc.player.blockPosition());int x=origin.getX(),z=origin.getZ(),y=160;
            server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x-16,y-1,z-16,x+32,y-1,z+32));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var level=p.level();
                level.setBlockAndUpdate(new BlockPos(x,y,z),DeathGraves.MARKER.defaultBlockState());
                var walker=JugcraftTown.GUARD_MECHA.create(level,EntitySpawnReason.COMMAND);walker.snapTo(x+4.5,y,z+.5,0,0);walker.setNoAi(true);level.addFreshEntity(walker);
            });
            server.runCommand("tp @p %f %f %f 165 6".formatted(x+6.5,y+1.7,z+7.5));c.waitTicks(30);c.takeScreenshot("oct22_guard_pilot_and_grave");
            server.runOnServer(s->{for(var e:s.getPlayerList().getPlayers().getFirst().level().getEntitiesOfClass(TownGuardMecha.class,new net.minecraft.world.phys.AABB(x-5,y-5,z-5,x+12,y+12,z+12)))e.discard();});
            int page=0;
            var blocks=CraftedFurniture.BLOCKS.values().stream().toList();
            for(int offset=0;offset<blocks.size();offset+=24){final int start=offset;
                server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-2,y,z-2,x+24,y+5,z+14));
                server.runOnServer(s->{var l=s.getPlayerList().getPlayers().getFirst().level();
                    for(int i=0;i<24&&start+i<blocks.size();i++){var b=blocks.get(start+i);var at=new BlockPos(x+(i%8)*3,y,z+(i/8)*4);var state=b.defaultBlockState();
                        if(b instanceof BedBlock) {state=state.setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT).setValue(BedBlock.FACING,Direction.NORTH);l.setBlock(at,state,2);l.setBlock(at.north(),state.setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD),3);}
                        else if(b.getStateDefinition().getProperty("half")!=null&&b.getStateDefinition().getProperty("half").getValue("upper").isPresent()){
                            var property=(net.minecraft.world.level.block.state.properties.Property)b.getStateDefinition().getProperty("half");
                            l.setBlock(at,state.setValue(property,(Comparable)property.getValue("lower").orElseThrow()),2);
                            l.setBlock(at.above(),state.setValue(property,(Comparable)property.getValue("upper").orElseThrow()),3);
                        }else l.setBlockAndUpdate(at,state);
                    }
                });
                server.runCommand("tp @p %f %f %f 180 30".formatted(x+10.5,y+7.5,z+20.5));c.waitTicks(12);c.takeScreenshot("oct22_furniture_"+(page++));
            }
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-2,y,z-2,x+24,y+5,z+14));
            for(var spec:AircraftSpec.ALL.values()){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var e:p.level().getEntitiesOfClass(Aircraft.class,p.getBoundingBox().inflate(100)))e.discard();
                    var a=JugcraftAircraft.TYPES.get(spec.name()).create(p.level(),EntitySpawnReason.COMMAND);a.snapTo(x+.5,y,z+.5,0,0);a.setOwner(p.getUUID());p.level().addFreshEntity(a);
                    for(int i=0;i<spec.sockets().size();i++){String socket=spec.sockets().get(i).type();if(socket.equals("boiler"))a.inventory().setItem(i,new ItemStack(Items.COAL,16));if(socket.equals("weapon"))a.inventory().setItem(i,new ItemStack(JugcraftAircraft.ITEMS.get("rotary_cannon")));if(socket.equals("banner"))a.inventory().setItem(i,new ItemStack(Items.BANNER.red()));}
                });
                double d=spec.name().equals("warship")?19:10;
                server.runCommand("tp @p %f %f %f 135 12".formatted(x+.5+d*.707,y+(spec.name().equals("warship")?5:1.5),z+.5+d*.707));
                c.waitTicks(25);c.takeScreenshot("oct22_aircraft_"+spec.name());
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var a=p.level().getEntitiesOfClass(Aircraft.class,p.getBoundingBox().inflate(60)).getFirst();p.teleportTo(a.getX()+2,a.getY(),a.getZ()+2);p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,who)->new AircraftMenu(id,inv,a),a.getDisplayName()));});
                c.waitTicks(6);c.takeScreenshot("oct22_cockpit_"+spec.name());
                if(spec.name().equals("cargo_airship")){
                    double[] point=c.computeOnClient(mc->{
                        var screen=mc.gui.screen();var socket=spec.sockets().stream().filter(s->s.type().equals("boiler")).findFirst().orElseThrow();
                        float scale=Math.min(1F,Math.min((screen.width-12F)/spec.width(),(screen.height-12F)/spec.height()));
                        double px=(Math.round((screen.width/scale-spec.width())/2)+socket.x()+8)*scale;
                        double py=(Math.round((screen.height/scale-spec.height())/2)+socket.y()+8)*scale;
                        return new double[]{px/net.minecraft.client.MouseHandler.getScaledXPos(mc.getWindow(),1),py/net.minecraft.client.MouseHandler.getScaledYPos(mc.getWindow(),1)};
                    });
                    c.getInput().setCursorPos(point[0],point[1]);c.waitTicks(1);c.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(4);
                    server.runOnServer(s->{var carried=s.getPlayerList().getPlayers().getFirst().containerMenu.getCarried();if(!carried.is(Items.COAL)||carried.getCount()!=16)throw new AssertionError("Scaled cargo menu did not pick up the actual clicked fuel slot");});
                    c.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(4);
                }
                c.runOnClient(mc->mc.player.closeContainer());
            }
            // Exercise the new projectile's spawn packet and renderer on a real client.
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                var round=new AircraftBullet(p.level(),p);round.setPos(p.position().add(2,2,0));
                round.setDeltaMovement(0,.02,0);p.level().addFreshEntity(round);
            });
            c.waitTicks(5);
            c.runOnClient(mc->{if(mc.level.getEntitiesOfClass(AircraftBullet.class,mc.player.getBoundingBox().inflate(12)).isEmpty())throw new AssertionError("Aircraft cannon round did not reach the client");});
            c.takeScreenshot("oct22_aircraft_projectile_client");
            // Unlike the flat dedicated test fixture, this world has real boss-lair dimensions.
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var original=p.level();var at=p.position();
                var lair=java.util.stream.StreamSupport.stream(s.getAllLevels().spliterator(),false).filter(io.github.jimbozoomer.jugcraft.lair.Lairs::isLair).findFirst().orElseThrow();
                p.teleportTo(lair,0,160,0,Set.of(),0,0,true);
                if(DeathGraves.eligible(p))throw new AssertionError("Boss lair must bypass ordinary death graves");
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,3));
                p.setHealth(0);p.die(lair.damageSources().genericKill());
                if(!GraveStore.get(s).owned(p.getUUID()).isEmpty())throw new AssertionError("Actual boss-dimension death made an ordinary grave");
                // A lair keeps everything in place (lair-keep-inventory.md): nothing drops, so nothing becomes Grave Goods.
                if(p.getAttached(io.github.jimbozoomer.jugcraft.lair.Lairs.GRAVE_GOODS)!=null||p.getInventory().getItem(0).getCount()!=3)throw new AssertionError("Boss death did not keep exactly three diamonds in place: goods="+p.getAttached(io.github.jimbozoomer.jugcraft.lair.Lairs.GRAVE_GOODS));
            });
            c.waitTicks(15);c.runOnClient(mc->mc.player.respawn());c.waitTicks(30);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!p.isAlive()||p.getInventory().countItem(Items.DIAMOND)!=3||!p.getInventory().getItem(0).is(Items.DIAMOND))throw new AssertionError("Boss respawn did not restore exactly three diamonds in their slot: alive="+p.isAlive()+", diamonds="+p.getInventory().countItem(Items.DIAMOND)+", retained="+p.getAttached(io.github.jimbozoomer.jugcraft.lair.Lairs.GRAVE_GOODS));
                if(p.getAttached(io.github.jimbozoomer.jugcraft.lair.Lairs.GRAVE_GOODS)!=null)throw new AssertionError("Restored boss items remain claimable");});
        }
    }
}
