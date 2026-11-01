package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Exact capacity storage and a read-only, paginated posted-book viewer. */
final class OwnerSupplementScreen extends AbstractContainerScreen<OwnerSupplementMenu> {
 private int page;
 OwnerSupplementScreen(OwnerSupplementMenu menu,Inventory inv,Component title){super(menu,inv,title,menu.notice?224:176,menu.notice?218:170);inventoryLabelY=76;}
 @Override protected void init(){super.init();if(menu.notice){addRenderableWidget(Button.builder(Component.literal("<"),b->page=Math.max(0,page-1)).bounds(leftPos+14,topPos+189,30,20).build());addRenderableWidget(Button.builder(Component.literal(">"),b->page=Math.min(panels().size()-1,page+1)).bounds(leftPos+180,topPos+189,30,20).build());}}
 private List<Component> pages(){var stack=menu.source.getItem(0);var written=stack.get(DataComponents.WRITTEN_BOOK_CONTENT);if(written!=null)return written.getPages(minecraft.isTextFilteringEnabled());var writable=stack.get(DataComponents.WRITABLE_BOOK_CONTENT);if(writable!=null)return writable.pages().stream().map(p->(Component)Component.literal(p.get(minecraft.isTextFilteringEnabled()))).toList();return List.of(stack.getHoverName());}
 private List<List<net.minecraft.util.FormattedCharSequence>> panels(){
  var panels=new java.util.ArrayList<List<net.minecraft.util.FormattedCharSequence>>();
  for(var text:pages()){var lines=font.split(text,imageWidth-40);if(lines.isEmpty())panels.add(List.of());else for(int start=0;start<lines.size();start+=14)panels.add(List.copyOf(lines.subList(start,Math.min(lines.size(),start+14))));}
  return panels;
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);}
 @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){if(!menu.notice)super.extractLabels(g,x,y);}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
  int x=leftPos,y=topPos;g.fill(x,y,x+imageWidth,y+imageHeight,0xFF392A20);g.fill(x+2,y+2,x+imageWidth-2,y+imageHeight-2,0xFFB68D59);
  if(menu.notice){
   g.fill(x+10,y+11,x+imageWidth-10,y+181,0xFFE7D7AD);g.fill(x+15,y+15,x+19,y+19,0xFF9B633A);g.fill(x+imageWidth-19,y+15,x+imageWidth-15,y+19,0xFF9B633A);
   var held=menu.source.getItem(0);var mapId=held.get(DataComponents.MAP_ID);
   if(mapId!=null&&minecraft.level!=null){var data=net.minecraft.world.item.MapItem.getSavedData(mapId,minecraft.level);if(data!=null){var map=new net.minecraft.client.renderer.state.MapRenderState();minecraft.getMapRenderer().extractRenderState(mapId,data,map);g.pose().pushMatrix();g.pose().translate(x+48,y+28);g.map(map);g.pose().popMatrix();return;}}
   var pages=panels();page=Math.clamp(page,0,Math.max(0,pages.size()-1));
   if(!pages.isEmpty()){g.enableScissor(x+20,y+25,x+imageWidth-20,y+171);int lineY=y+25;for(var line:pages.get(page)){g.text(font,line,x+20,lineY,0xFF30251E,false);lineY+=10;}g.disableScissor();}
   g.text(font,Component.literal((page+1)+" / "+Math.max(1,pages.size())),x+92,y+195,0xFF30251E,false);return;
  }
  g.fill(x+5,y+17,x+imageWidth-5,y+74,0xFF75583C);
  for(var slot:menu.slots){int sx=x+slot.x,sy=y+slot.y;g.fill(sx-1,sy-1,sx+17,sy+17,0xFF382B20);g.fill(sx,sy,sx+17,sy+17,0xFFE1CBA7);g.fill(sx,sy,sx+16,sy+16,0xFF8E806B);}
 }
}
