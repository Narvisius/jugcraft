package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
public final class HarvestFestivalGameTests {
 @GameTest(maxTicks=60) public void allFoodPlacementRegression(GameTestHelper h){new FoodServingGameTests().everyEdibleHasPlacementAndRoundTrips(h);}
 @GameTest(maxTicks=40) public void foodDropRegression(GameTestHelper h){new FoodServingGameTests().breakAndSupportLossDropOnce(h);}
 @GameTest(maxTicks=40) public void foodPersistenceRegression(GameTestHelper h){new FoodServingGameTests().creativeUnsupportedAndSavedComponents(h);}
 private static BlockPos build(GameTestHelper h,Direction d,int portions){
  var b=HarvestFestival.GIANT_TURKEY;var p=h.absolutePos(new BlockPos(4,2,4));
  for(int i=0;i<18;i++){var q=b.partPos(p,d,i);h.getLevel().setBlock(q.below(2),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(q,b.defaultBlockState().setValue(MultiDecorationBlock.FACING,d).setValue(GiantTurkeyFeastBlock.PART,i).setValue(GiantTurkeyFeastBlock.REMAINING,portions),3);}
  return p;
 }
 private static void use(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,BlockPos p,ItemStack stack){player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.gameMode.useItemOn(player,h.getLevel(),stack,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false));}
 @GameTest(maxTicks=40) public void carvingStopsAt24AndPackingPreservesCount(GameTestHelper h){
  var l=h.getLevel();var b=HarvestFestival.GIANT_TURKEY;var p=build(h,Direction.NORTH,24);var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(p).add(0,0,-2));
  var knife=new ItemStack(JugcraftAgriculture.item("carving_knife"));
  for(int i=0;i<25;i++)use(h,player,b.partPos(p,Direction.NORTH,2),knife);
  h.assertTrue(player.getInventory().countItem(JugcraftAgriculture.item("turkey_slice"))==24,"Exactly 24 slices, never a 25th");
  for(int i=0;i<18;i++)h.assertTrue(l.getBlockState(b.partPos(p,Direction.NORTH,i)).getValue(GiantTurkeyFeastBlock.REMAINING)==0,"All parts agree");
  player.setShiftKeyDown(true);use(h,player,p,ItemStack.EMPTY);
  h.assertTrue(l.getBlockState(p).isAir(),"Packing removes feast");
  ItemStack packed=ItemStack.EMPTY;for(int i=0;i<player.getInventory().getContainerSize();i++){var s=player.getInventory().getItem(i);if(s.is(b.asItem()))packed=s;}
  h.assertTrue(!packed.isEmpty()&&packed.get(DataComponents.BLOCK_STATE).apply(b.defaultBlockState()).getValue(GiantTurkeyFeastBlock.REMAINING)==0,"Empty feast stays empty when packed");h.succeed();
 }
 @GameTest(maxTicks=40) public void secondaryBreakDropsOnePartlyEatenFeastEveryFacing(GameTestHelper h){
  var l=h.getLevel();var b=HarvestFestival.GIANT_TURKEY;
  for(var d:Direction.Plane.HORIZONTAL){var p=build(h,d,11);l.destroyBlock(b.partPos(p,d,17),true);
   var drops=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(p).inflate(8),e->e.getItem().is(b.asItem()));
   h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"One feast dropped for "+d);
   h.assertTrue(drops.getFirst().getItem().get(DataComponents.BLOCK_STATE).apply(b.defaultBlockState()).getValue(GiantTurkeyFeastBlock.REMAINING)==11,"Remaining count preserved");
   for(int i=0;i<18;i++)h.assertTrue(!l.getBlockState(b.partPos(p,d,i)).is(b),"Every part cleared");drops.forEach(e->e.discard());
  }h.succeed();
 }
 @GameTest(maxTicks=40) public void placedPackedFeastRetainsPortionsAndRejectsObstruction(GameTestHelper h){
  var l=h.getLevel();var b=HarvestFestival.GIANT_TURKEY;var p=h.absolutePos(new BlockPos(4,2,4));
  for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)l.setBlock(p.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
  var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(p).add(0,0,-4));player.setYRot(0);
  var packed=new ItemStack(b);packed.set(DataComponents.BLOCK_STATE,net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(GiantTurkeyFeastBlock.REMAINING,11));
  var hit=new BlockHitResult(Vec3.atCenterOf(p.below()).add(0,.5,0),Direction.UP,p.below(),false);
  player.setItemInHand(InteractionHand.MAIN_HAND,packed);
  ((BlockItem)b.asItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,packed,hit));
  h.assertTrue(l.getBlockState(p).is(b),"Packed feast placed");var facing=l.getBlockState(p).getValue(MultiDecorationBlock.FACING);
  for(int i=0;i<18;i++)h.assertTrue(l.getBlockState(b.partPos(p,facing,i)).getValue(GiantTurkeyFeastBlock.REMAINING)==11,"Placement never replenishes portions");
  l.removeBlock(p,false);var obstacle=b.partPos(p,facing,17);l.setBlock(obstacle,Blocks.STONE.defaultBlockState(),3);
  var fresh=new ItemStack(b);player.setItemInHand(InteractionHand.MAIN_HAND,fresh);
  ((BlockItem)b.asItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,fresh,hit));
  h.assertTrue(!l.getBlockState(p).is(b)&&fresh.getCount()==1&&l.getBlockState(obstacle).is(Blocks.STONE),"Obstruction rejects entire placement without consuming or replacing");h.succeed();
 }
 @GameTest(maxTicks=30) public void novemberCostumesAndReachableFoodRecipes(GameTestHelper h){
  for(String d:java.util.List.of("2026-11-01","2026-11-26","2026-11-30"))h.assertTrue(HarvestFestival.costumesOn(java.time.LocalDate.parse(d)),"November costume active");
  for(String d:java.util.List.of("2026-10-31","2026-12-01"))h.assertTrue(!HarvestFestival.costumesOn(java.time.LocalDate.parse(d)),"Costumes revert outside November");
  var catalog=CookingPotRecipe.catalog(h.getLevel().getServer());
  for(var entry:HarvestFestival.FOODS.entrySet()){
   h.assertTrue(FoodServings.entries().containsKey(entry.getValue()),"New food is placeable");
   if(entry.getKey().equals("dinner_roll"))continue;
   var recipe=catalog.get(Jugcraft.id("harvest_"+entry.getKey()));h.assertTrue(recipe!=null,"Cooking recipe loaded for "+entry.getKey());
   h.assertTrue(recipe.output().create().is(entry.getValue()),"Correct output");
  }
  Object[][] meals={
   {"mashed_potatoes_and_gravy",new Item[]{Items.BOWL,Items.BAKED_POTATO,Items.BAKED_POTATO,JugcraftAgriculture.item("onion"),Items.BROWN_MUSHROOM}},
   {"harvest_pea_casserole",new Item[]{Items.BOWL,JugcraftAgriculture.item("peas"),JugcraftAgriculture.item("peas"),Items.BAKED_POTATO,JugcraftAgriculture.item("onion"),Items.BROWN_MUSHROOM}},
   {"candied_sweet_potatoes",new Item[]{Items.BOWL,JugcraftAgriculture.item("baked_sweet_potato"),JugcraftAgriculture.item("baked_sweet_potato"),Items.SUGAR}}};
  for(var meal:meals){var inputs=new java.util.ArrayList<ItemStack>();for(var ingredient:(Item[])meal[1])inputs.add(new ItemStack(ingredient));while(inputs.size()<6)inputs.add(ItemStack.EMPTY);
   var match=CookingPotRecipe.find(h.getLevel().getServer(),inputs);h.assertTrue(match.isPresent()&&match.get().recipe().output().create().is(HarvestFestival.FOODS.get((String)meal[0])),"Actual ingredients match "+meal[0]);}
  h.assertTrue(HarvestFestival.PILGRIM_COAT.getDefaultInstance().get(DataComponents.EQUIPPABLE)!=null,"Wearable without research");h.succeed();
 }
}
