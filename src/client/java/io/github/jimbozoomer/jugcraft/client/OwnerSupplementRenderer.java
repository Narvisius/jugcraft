package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

final class OwnerSupplementRenderer implements BlockEntityRenderer<OwnerSupplementData,OwnerSupplementRenderer.State> {
 private record Captive(net.minecraft.nbt.CompoundTag data,net.minecraft.world.entity.Entity entity){}
 private final java.util.Map<OwnerSupplementData,Captive> captives=new java.util.WeakHashMap<>();
 private final net.minecraft.client.renderer.entity.EntityRenderDispatcher entities;
 private static int nextVisualId=-4000000;
 private final OwnerMobHeadModel heads=new OwnerMobHeadModel();
 private final OwnerCannonModel cannon=new OwnerCannonModel();
 private final OwnerGlobeRenderer globes=new OwnerGlobeRenderer();
 private final OwnerBuntingRenderer buntings=new OwnerBuntingRenderer();
 private final ItemModelResolver items;private final OwnerSupplementAnimations animated=new OwnerSupplementAnimations();
 OwnerSupplementRenderer(BlockEntityRendererProvider.Context c){items=c.itemModelResolver();entities=c.entityRenderer();}
 OwnerSupplementRenderer(){var mc=net.minecraft.client.Minecraft.getInstance();items=mc.getItemModelResolver();entities=mc.getEntityRenderDispatcher();}
 static void register(){OwnerAntiqueMaps.register();OwnerGlobeTextures.register();OwnerCannonPreview.register();OwnerQuiverRenderer.register();net.minecraft.client.renderer.entity.EntityRenderers.register(OwnerPlunderer.TYPE,OwnerPlundererRenderer::new);OwnerCannonBoat.TYPES.forEach((wood,type)->net.minecraft.client.renderer.entity.EntityRenderers.register(type,c->new OwnerCannonBoatRenderer(c,wood)));net.minecraft.client.renderer.entity.EntityRenderers.register(OwnerDispenserCart.TYPE,c->new net.minecraft.client.renderer.entity.MinecartRenderer(c,net.minecraft.client.model.geom.ModelLayers.MINECART));net.minecraft.client.renderer.entity.EntityRenderers.register(OwnerPignata.TYPE,OwnerPignataRenderer::new);net.minecraft.client.renderer.entity.EntityRenderers.register(OwnerHatStand.TYPE,OwnerHatStandRenderer::new);net.minecraft.client.renderer.entity.EntityRenderers.register(OwnerFieldProjectiles.TYPE,net.minecraft.client.renderer.entity.ThrownItemRenderer::new);BlockEntityRenderers.register(OwnerAssemblyMotion.TYPE,OwnerMovingAssemblyRenderer::new);OwnerBubbleVisuals.register();net.minecraft.client.renderer.entity.EntityRenderers.register(OwnerCannon.SEAT,net.minecraft.client.renderer.entity.NoopRenderer::new);MenuScreens.register(OwnerCannonMenu.TYPE,OwnerCannonScreen::new);net.minecraft.client.renderer.entity.EntityRenderers.register(OwnerCannon.PEARL,net.minecraft.client.renderer.entity.ThrownItemRenderer::new);net.minecraft.client.renderer.entity.EntityRenderers.register(OwnerCannon.BALL,net.minecraft.client.renderer.entity.ThrownItemRenderer::new);net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource(){
  public int color(net.minecraft.world.level.block.state.BlockState state){return 0xFF385DC6;}
  public int colorInWorld(net.minecraft.world.level.block.state.BlockState state,net.minecraft.client.renderer.block.BlockAndTintGetter level,net.minecraft.core.BlockPos pos){return level.getBlockEntity(pos) instanceof OwnerSupplementData data?0xFF000000|data.spikePotion.getColor():0xFF385DC6;}
 }),OwnerSupplementaries.BLOCKS.get("bamboo_spikes"));OwnerBlackboardScreen.register();OwnerSupplementTextScreen.register();OwnerSupplementHeld.register();OwnerAltimeterRenderer.register();OwnerConfettiParticle.register();OwnerPartyHatLayer.register();for(var type:OwnerSupplementMenu.TYPES.values())MenuScreens.register(type,OwnerSupplementScreen::new);MenuScreens.register(OwnerSupplementMenu.NOTICE,OwnerSupplementScreen::new);BlockEntityRenderers.register(OwnerSupplementaries.DATA,OwnerSupplementRenderer::new);}
 static final class State extends BlockEntityRenderState {boolean statueHeadless;final ItemStackRenderState statueHat=new ItemStackRenderState();net.minecraft.resources.Identifier statueSkin,statueCape;boolean statueSlim;net.minecraft.client.renderer.entity.state.EntityRenderState cannonRider;net.minecraft.core.Direction cannonMount=net.minecraft.core.Direction.UP;float bubbleScale=1,bubblePhase,headMouth,headRotation,cannonYaw,cannonPitch,cannonFuse,cannonCooldown;boolean cannonLoaded;boolean way,antique;final int[] wayAngles={0,0};final String[] wayTexts={"",""};final ItemStackRenderState[] wayBoards={new ItemStackRenderState(),new ItemStackRenderState()};final java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> wayPost=new java.util.ArrayList<>();OwnerGlobeRenderer.Appearance globe;float globeRotation;final int[] buntings={-1,-1,-1,-1};net.minecraft.world.level.block.entity.BannerPatternLayers flagPatterns;final ItemStackRenderState item=new ItemStackRenderState(),ship=new ItemStackRenderState();final java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> shell=new java.util.ArrayList<>();byte[] board;boolean boardGlow;String lettering="",statuePose="standing";boolean waving;int ink;String name="";float yaw,time,day,progress,bellowsHeight,springOffset;final java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> springParts=new java.util.ArrayList<>();int power,liquidColor;float liquidFill,mobScale;net.minecraft.client.renderer.entity.state.EntityRenderState mob;final java.util.List<ItemStackRenderState> cookies=new java.util.ArrayList<>();boolean flowers,frame,wall,twoFaces,booksUpright;final java.util.List<ItemStackRenderState> books=new java.util.ArrayList<>();net.minecraft.core.Direction direction=net.minecraft.core.Direction.UP;net.minecraft.resources.Identifier sandTexture;final java.util.List<java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>> plants=new java.util.ArrayList<>();final int[] colors=new int[3];}
 @SuppressWarnings({"rawtypes","unchecked"}) private static net.minecraft.world.level.block.state.BlockState with(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.block.state.properties.Property p,String v){return p==null?s:s.setValue(p,(Comparable)p.getValue(v).orElseThrow());}
 // Flags and moving spring heads extend outside their root cell; retain distance culling.
 @Override public boolean shouldRenderOffScreen(){return true;}
 @Override public int getViewDistance(){return 64;}
 @Override public State createRenderState(){return new State();}
 @Override public void extractRenderState(OwnerSupplementData d,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
  BlockEntityRenderState.extractBase(d,s,overlay);OwnerStatueSkins.extract(d,s);s.cannonMount=OwnerCannon.mount(d.getBlockState());s.cannonYaw=d.cannonYaw;s.cannonPitch=d.cannonPitch;s.cannonLoaded=!d.getItem(0).isEmpty();s.cannonFuse=Math.max(0,d.cannonFuse-partial);s.cannonCooldown=Math.max(0,d.cannonCooldown-partial);java.util.Arrays.fill(s.buntings,-1);if(d.block().function.equals("rope_bunting"))for(int i=0;i<4;i++)if(d.getItem(i).getItem() instanceof OwnerBuntings.Pennant pennant)s.buntings[i]=pennant.color.getId();s.name=d.block().name;s.flagPatterns=d.block().function.equals("flag")?d.flagPatterns:null;s.yaw=180-HouseholdBlock.facing(d.getBlockState()).toYRot();s.time=d.getLevel()==null?0:d.getLevel().getGameTime()+partial;
  if(s.name.equals("bubble_block")){s.bubbleScale=(float)Math.clamp(1-.9*Math.pow(.6,Math.max(0,s.time-d.pulseUntil)),.1,1);var p=d.getBlockPos();s.bubblePhase=Math.floorMod(p.getX()*7L+p.getY()*9L+p.getZ()*13L,100)/100F;}
  s.cannonRider=null;
  if(d.block().function.equals("cannon")&&d.getLevel()!=null){
   var mc=net.minecraft.client.Minecraft.getInstance();
   for(var player:d.getLevel().players())if(player.getVehicle() instanceof OwnerCannonSeat seat&&seat.anchor.equals(d.getBlockPos())&&!player.isInvisible()&&!(player==mc.player&&mc.options.getCameraType().isFirstPerson())){
    var rider=entities.extractEntity(player,partial);if(rider instanceof net.minecraft.client.renderer.entity.state.HumanoidRenderState living){living.bodyRot=0;living.yRot=0;living.xRot=0;living.walkAnimationPos=0;living.walkAnimationSpeed=0;living.isPassenger=false;}s.cannonRider=rider;break;
   }
  }
  if(OwnerMobHeads.head(d)){float elapsed=Math.clamp((s.time-d.pulseUntil)*.5F,0,1);s.headMouth=d.power>0?elapsed:1-elapsed;s.headRotation=d.block().name.contains("wall")?180-HouseholdBlock.facing(d.getBlockState()).toYRot():-Float.parseFloat(HouseholdBlock.value(d.getBlockState(),"rotation"))*22.5F;}
  s.globe=d.block().function.equals("globe")?OwnerGlobeRenderer.appearance(d):null;s.globeRotation=s.globe==null?0:OwnerGlobes.rotation(d,s.time);
  s.ship.clear();if(s.name.equals("jar_boat")){var display=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);display.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,io.github.jimbozoomer.jugcraft.Jugcraft.id("supps_jar_boat_ship"));items.updateForTopItem(s.ship,display,ItemDisplayContext.NONE,d.getLevel(),null,0);}
  s.way=OwnerWaySigns.sign(d);s.wayPost.clear();if(s.way){
   if(OwnerWaySigns.base(d.wayBase))net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(d.wayBase).collectParts(net.minecraft.util.RandomSource.create(0),s.wayPost);
   for(int i=0;i<2;i++){s.wayBoards[i].clear();s.wayAngles[i]=d.wayAngles[i];s.wayTexts[i]=d.wayTexts[i];if(d.getItem(i).getItem() instanceof OwnerWaySigns.Board board){var display=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);display.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,io.github.jimbozoomer.jugcraft.Jugcraft.id("supps_way_sign_"+board.wood+"_placed"));items.updateForTopItem(s.wayBoards[i],display,ItemDisplayContext.NONE,d.getLevel(),null,0);}}
  }
  var held=d.getItem(0);s.waving=HouseholdBlock.flag(d.getBlockState(),"powered");s.statuePose=held.isEmpty()?"standing":held.is(net.minecraft.tags.ItemTags.SWORDS)?"sword":held.is(net.minecraft.tags.ItemTags.PICKAXES)||held.is(net.minecraft.tags.ItemTags.AXES)||held.is(net.minecraft.tags.ItemTags.SHOVELS)||held.is(net.minecraft.tags.ItemTags.HOES)?"tool":held.is(net.minecraft.tags.ItemTags.CANDLES)?"candle":"holding";
  s.statueHeadless=false;s.statueHat.clear();
  if(s.name.equals("statue")&&d.statueHoliday!=0){
   if(d.statueHoliday==1){s.statueHeadless=true;if(held.isEmpty())s.statuePose="holding";else items.updateForTopItem(s.statueHat,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CARVED_PUMPKIN),ItemDisplayContext.FIXED,d.getLevel(),null,0);}
   else items.updateForTopItem(s.statueHat,new net.minecraft.world.item.ItemStack(OwnerSupplementaries.ITEMS.get("confetti_popper")),ItemDisplayContext.HEAD,d.getLevel(),null,0);
  }
  s.antique=d.antiqueText;s.lettering=d.block().function.equals("doormat")?d.message:"";s.ink=d.textColor;
  s.board=d.block().function.equals("blackboard")?d.boardPixels.clone():null;s.boardGlow=d.boardGlow;s.springParts.clear();if(d.block().function.equals("spring_arm")){
   boolean extending=HouseholdBlock.flag(d.getBlockState(),"extending");float age=Math.clamp(s.time-d.pulseUntil,0,2);s.springOffset=extending?-1+age*.5F:-age*.5F;
   var head=OwnerSupplementaries.BLOCKS.get("spring_launcher_head").defaultBlockState();var facing=head.getBlock().getStateDefinition().getProperty("facing");head=with(head,facing,HouseholdBlock.facing(d.getBlockState()).getName());
   net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(head).collectParts(net.minecraft.util.RandomSource.create(0),s.springParts);
  }
  s.bellowsHeight=OwnerSupplementData.bellowsHeight(d.power,d.pulseUntil,s.time);s.mob=null;s.liquidFill=0;s.cookies.clear();
  if(d.captive!=null&&d.getLevel()!=null){var cached=captives.get(d);if(cached==null||!cached.data.equals(d.captive)||cached.entity.level()!=d.getLevel()){
   var visual=net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,d.getLevel().registryAccess(),d.captive),d.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD,net.minecraft.world.entity.EntityProcessor.NOP);
   if(visual!=null){visual.setId(nextVisualId--);visual.setPos(d.getBlockPos().getX()+.5,d.getBlockPos().getY()+.125,d.getBlockPos().getZ()+.5);visual.setCustomNameVisible(false);cached=new Captive(d.captive.copy(),visual);captives.put(d,cached);}
  }if(cached!=null){s.mob=entities.extractEntity(cached.entity,0);s.mob.lightCoords=s.lightCoords;s.mobScale=Math.min(.8F,.65F/Math.max(.1F,Math.max(cached.entity.getBbWidth(),cached.entity.getBbHeight())));}}else captives.remove(d);
  if(d.block().function.equals("jar")){
   s.liquidFill=(float)d.fluid.amount/(3*net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);s.liquidColor=GlassTankRenderer.color(d.fluid.variant.getFluid());
   for(int i=0;i<d.getContainerSize();i++)if(!d.getItem(i).isEmpty()){var cookie=new ItemStackRenderState();items.updateForTopItem(cookie,d.getItem(i),ItemDisplayContext.FIXED,d.getLevel(),null,i);s.cookies.add(cookie);}
  }
  s.books.clear();s.booksUpright=d.block().name.equals("book_pile_horizontal");
  if(d.block().function.equals("books"))for(int slot=0;slot<4;slot++){
   var stack=d.getItem(slot);var type=OwnerSupplementBooks.type(stack);if(type==null)continue;
   var visual=type.visuals().get(Math.floorMod(d.getBlockPos().hashCode()+slot*17,type.visuals().size()));
   var display=stack.copyWithCount(1);display.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,visual.model());display.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,visual.glint());
   var state=new ItemStackRenderState();items.updateForTopItem(state,display,ItemDisplayContext.FIXED,d.getLevel(),null,slot);s.books.add(state);
  }
  if(d.softPortions>0){s.liquidFill=d.block().function.equals("goblet")?1:(float)d.softPortions*OwnerSoftLiquids.units(d.softDrink)/9;s.liquidColor=OwnerSoftLiquids.color(d.softDrink);}

  s.day=d.getLevel()==null?0:d.getLevel().getDefaultClockTime()+partial;s.twoFaces=HouseholdBlock.flag(d.getBlockState(),"two_faced");s.direction=HouseholdBlock.facing(d.getBlockState());s.power=d.power;s.progress=d.progress;s.sandTexture=null;
  if(d.block().function.equals("hourglass")){var info=OwnerSupplementData.sandData(d.getItem(0));if(info!=null){
   float elapsed=Math.max(0,s.time-d.snapshotTick)/Math.max(1,OwnerSupplementData.duration(d.getItem(0)));
   if(s.direction==net.minecraft.core.Direction.UP)s.progress=Math.clamp(s.progress+elapsed,0,1);else if(s.direction==net.minecraft.core.Direction.DOWN)s.progress=Math.clamp(s.progress-elapsed,0,1);
   String tex=info.has("texture")?info.get("texture").getAsString():d.getItem(0).getItem() instanceof net.minecraft.world.item.BlockItem b?net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock()).toString():"minecraft:block/sand";
   var id=net.minecraft.resources.Identifier.parse(tex);s.sandTexture=net.minecraft.resources.Identifier.fromNamespaceAndPath(id.getNamespace(),"textures/"+(id.getPath().contains("/")?id.getPath():"block/"+id.getPath())+".png");
  }}
  s.flowers=d.block().function.equals("flowers");s.frame=d.block().function.equals("frame");s.wall=HouseholdBlock.value(d.getBlockState(),"face").equals("wall");s.plants.clear();
  if(s.flowers||s.frame)for(int i=0;i<(s.frame?1:3);i++){
   var parts=new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
   if(d.getItem(i).getItem() instanceof net.minecraft.world.item.BlockItem item){var block=item.getBlock().defaultBlockState();var mc=net.minecraft.client.Minecraft.getInstance();mc.getModelManager().getBlockStateModelSet().get(block).collectParts(net.minecraft.util.RandomSource.create(i),parts);s.colors[i]=OwnerBlockTints.color(block,d.getLevel(),d.getBlockPos(),0);}
   s.plants.add(parts);
  }
  items.updateForTopItem(s.item,s.statueHeadless&&d.getItem(0).isEmpty()?new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.JACK_O_LANTERN):(d.block().function.equals("display")||d.block().function.equals("statue"))?d.getItem(0):net.minecraft.world.item.ItemStack.EMPTY,ItemDisplayContext.FIXED,d.getLevel(),null,0);
 }
 @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
  if(s.name.equals("enderman_head")||s.name.equals("enderman_wall_head")||s.name.equals("spider_head")||s.name.equals("spider_wall_head")){heads.draw(s,pose,out);return;}
  if(s.name.equals("bubble_block")){OwnerBubbleVisuals.draw(s,pose,out);return;}
  if(s.name.equals("cannon")){cannon.draw(s,pose,out);return;}
  if(s.way){
   if(!s.wayPost.isEmpty())out.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),s.wayPost,new int[0],s.lightCoords,OverlayTexture.NO_OVERLAY,0);
   var font=net.minecraft.client.Minecraft.getInstance().font;
   for(int i=0;i<2;i++){if(s.wayBoards[i].isEmpty())continue;pose.pushPose();pose.translate(.5,i==0?.5:0,.5);pose.rotateDegrees(Axis.YP,-s.wayAngles[i]*22.5F);
    pose.pushPose();pose.translate(0,.5,0);s.wayBoards[i].submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    for(int face=0;face<2;face++){pose.pushPose();pose.translate(0,.285,face==0?-.126:-.0615);pose.rotateDegrees(Axis.YP,face==0?180:0);pose.scale(.009F,-.009F,.009F);var line=OwnerAntiqueInk.ink(net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(s.wayTexts[i],98)),s.antique).getVisualOrderText();out.submitText(pose,-font.width(line)/2F,-4,line,false,net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET,s.boardGlow?net.minecraft.util.LightCoordsUtil.FULL_BRIGHT:s.lightCoords,s.ink,0,0);pose.popPose();}
    pose.popPose();
   }return;
  }
  if(!s.ship.isEmpty()){pose.pushPose();pose.translate(.5,.3125,.5);pose.rotateDegrees(Axis.YP,s.yaw);pose.rotateDegrees(Axis.XP,(float)Math.sin(s.time/20)*1.7F);pose.translate(0,.5,0);s.ship.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
  globes.draw(s,pose,out);animated.draw(s,pose,out);buntings.draw(s,pose,out);OwnerFlagRenderer.draw(s,pose,out);
  if(!s.books.isEmpty()){
   pose.pushPose();pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,s.yaw);pose.translate(-.5,0,-.5);
   for(int i=0;i<s.books.size();i++){pose.pushPose();if(s.booksUpright)pose.translate(.5+((s.books.size()-1)*.5-i)*.25,.5,.5);else{pose.translate(.3125,.125+i*.25,.5);pose.rotateDegrees(Axis.ZP,90);}s.books.get(i).submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
   pose.popPose();
  }
  if(!s.springParts.isEmpty()){pose.pushPose();pose.translate(s.direction.getStepX()*s.springOffset,s.direction.getStepY()*s.springOffset,s.direction.getStepZ()*s.springOffset);out.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),s.springParts,new int[0],s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
  if(!s.lettering.isEmpty()){
   var font=net.minecraft.client.Minecraft.getInstance().font;pose.pushPose();pose.translate(.5,1.02/16,.5);pose.rotateDegrees(Axis.YP,s.yaw);pose.rotateDegrees(Axis.XP,-90);pose.scale(.009F,-.009F,.009F);
   var lines=s.lettering.split("\n",-1);for(int i=0;i<Math.min(3,lines.length);i++){var line=OwnerAntiqueInk.ink(net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(lines[i],90)),s.antique).getVisualOrderText();out.submitText(pose,-font.width(line)/2F,(i-1)*10-4,line,false,net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET,s.boardGlow?net.minecraft.util.LightCoordsUtil.FULL_BRIGHT:s.lightCoords,s.ink,0,0);}pose.popPose();
  }
  if(s.board!=null){
   byte[] pixels=s.board.clone();int light=s.boardGlow?net.minecraft.util.LightCoordsUtil.FULL_BRIGHT:s.lightCoords;
   pose.pushPose();pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.YP,180-s.direction.toYRot());pose.translate(-.5,-.5,-.5);
   out.submitCustomGeometry(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/newest/supps/block/blackboard.png")),(matrix,buffer)->{
    for(int row=0;row<16;row++)for(int col=0;col<16;col++){
     int ink=pixels[row*16+col];if(ink==0)continue;int color=OwnerBlackboards.color(ink);float right=1-col/16F,left=right-1/16F,top=1-row/16F,bottom=top-1/16F,z=11.24F/16F;
     float[][] corners={{left,bottom,.5F,1},{left,top,.5F,0},{right,top,1,0},{right,bottom,1,1}};
     for(var v:corners)buffer.addVertex(matrix,v[0],v[1],z).setColor(color).setUv(v[2],v[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,0,0,-1);
    }
   });pose.popPose();
  }
  if(s.cannonRider!=null){
   var dir=OwnerCannon.direction(s.cannonYaw,s.cannonPitch);var mount=Vec3.atLowerCornerOf(s.cannonMount.getUnitVec3i()).scale(.0625);
   pose.pushPose();pose.translate(.5+mount.x+dir.x*.40625,.5+mount.y+dir.y*.40625,.5+mount.z+dir.z*.40625);pose.rotateDegrees(Axis.YP,180-s.cannonYaw);pose.rotateDegrees(Axis.XP,-90-s.cannonPitch);pose.translate(0,-1.75,0);entities.getRenderer(s.cannonRider).submit(s.cannonRider,pose,out,camera);pose.popPose();
  }
  if(s.mob!=null){pose.pushPose();pose.translate(.5,.125,.5);pose.scale(s.mobScale,s.mobScale,s.mobScale);entities.getRenderer(s.mob).submit(s.mob,pose,out,camera);pose.popPose();}
  for(int i=0;i<s.cookies.size();i++){pose.pushPose();pose.translate(.5,.14+i*.045,.5);pose.rotateDegrees(Axis.XP,90);pose.rotateDegrees(Axis.ZP,i*35);pose.scale(.5F,.5F,.5F);s.cookies.get(i).submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
  if(s.frame&&!s.plants.isEmpty())out.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),s.plants.get(0),new int[]{s.colors[0]},s.lightCoords,OverlayTexture.NO_OVERLAY,0);
  if(s.flowers){pose.pushPose();pose.translate(.5,.30,.5);pose.rotateDegrees(Axis.YP,s.yaw);if(s.wall)pose.translate(0,0,-.31);
   for(int i=0;i<s.plants.size();i++){if(s.plants.get(i).isEmpty())continue;pose.pushPose();pose.translate((i-1)*.31,0,0);pose.scale(.48F,.65F,.48F);pose.translate(-.5,0,-.5);out.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),s.plants.get(i),new int[]{s.colors[i]},s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}pose.popPose();}
  if(s.name.equals("statue")&&!s.item.isEmpty()){pose.pushPose();pose.translate(.5,.75,.5);pose.rotateDegrees(Axis.YP,s.yaw);pose.scale(.5F,.5F,.5F);if(s.statuePose.equals("sword")||s.statuePose.equals("tool")){pose.translate(s.statuePose.equals("sword")?-.35:-.4,s.statuePose.equals("sword")?1.0625:1.25,0);pose.rotateDegrees(Axis.ZP,135);}pose.translate(0,.5,-.5);s.item.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();return;}
  if(s.item.isEmpty())return;pose.pushPose();pose.translate(.5,s.name.equals("pedestal")?1.2:.5,.5);pose.rotateDegrees(Axis.YP,s.yaw);
  if(s.name.equals("item_shelf")){pose.translate(0,.15,.26);pose.scale(.45F,.45F,.45F);}else{pose.rotateDegrees(Axis.YP,s.time*2);pose.scale(.5F,.5F,.5F);}
  s.item.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
 }
}
