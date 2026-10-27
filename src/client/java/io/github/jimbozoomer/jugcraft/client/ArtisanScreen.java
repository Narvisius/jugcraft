package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.furniture.*;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * A visual grid editor: no commands or JSON required, with front/top previews and a local library. In the workshop look
 * (docs/features/new-screen-styles.md): the previews and the part's measurements on the glass, the features that are
 * switched on lit, and the cost in the title strip. Laid out at 440 x 284 and scaled down to fit a smaller window.
 */
public final class ArtisanScreen extends Screen implements ScreenKit.Fitted {
    private final BlockPos bench;
    private FurnitureDesign design=FurnitureDesign.stool();
    private int selected,libraryIndex=-1;
    private List<FurnitureDesign> library=new ArrayList<>();
    private boolean libraryReadable=true;
    private String message="Choose a part, then adjust its position and size.";
    private EditBox name;
    private float scale=1;
    private int left,top;
    private static final int W=440,H=284;
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("artisan");
    private static final int[] COLORS={0xFFB68B52,0xFF705132,0xFFD8C697,0xFFB0BCC0,0xFFE6E3D9,0xFF87C2C9,0xFF85898D,0xFF8F57A5};
    public ArtisanScreen(BlockPos bench){super(Component.literal("Artisan's Workstation"));this.bench=bench;try{library=ArtisanLibrary.load();}catch(java.io.IOException e){message=e.getMessage();libraryReadable=false;}}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        scale=Math.min(1F,Math.min((width-8F)/W,(height-8F)/H));left=Math.round((width/scale-W)/2);top=Math.round((height/scale-H)/2);
        name=new EditBox(font,left+12,top+30,244,18,Component.literal("Design name"));name.setMaxLength(40);name.setValue(design.name());name.setResponder(v->{if(!v.isBlank())try{design=new FurnitureDesign(v,design.parts(),design.seat(),design.storage(),design.light(),design.effect(),design.sound());}catch(IllegalArgumentException ignored){}});addRenderableWidget(name);
        button(268,30,76,"Build",()->{ClientPlayNetworking.send(new ArtisanFurniture.Request(bench,design.json(),false));message="Build requested; check the server message for the result.";});
        button(350,30,78,"Recycle held",()->{ClientPlayNetworking.send(new ArtisanFurniture.Request(bench,"",true));message="Recycle requested (75% materials); hold custom furniture.";});
        button(12,55,56,"Stool",()->preset(0));button(72,55,56,"Table",()->preset(1));button(132,55,56,"Lamp",()->preset(2));button(192,55,64,"Cabinet",()->preset(3));
        button(268,55,76,"Save design",this::save);button(350,55,78,"Read held",()->{var s=minecraft.player.getMainHandItem();if(s.has(ArtisanFurniture.DESIGN)){design=FurnitureDesign.safe(s.get(ArtisanFurniture.DESIGN));selected=0;rebuild();}else message="Hold a custom furniture item to read its design.";});
        button(12,188,38,"<",()->{selected=Math.floorMod(selected-1,design.parts().size());rebuild();});button(54,188,38,">",()->{selected=(selected+1)%design.parts().size();rebuild();});
        button(98,188,76,"Duplicate",()->{if(design.parts().size()>=FurnitureDesign.MAX_PARTS){message="Maximum 32 parts.";return;}var parts=new ArrayList<>(design.parts());parts.add(parts.get(selected));replace(parts);selected=parts.size()-1;rebuild();});
        button(178,188,78,"Remove part",()->{if(design.parts().size()==1){message="Keep at least one part.";return;}var parts=new ArrayList<>(design.parts());parts.remove(selected);selected=Math.min(selected,parts.size()-1);replace(parts);rebuild();});
        for(int n=0;n<6;n++){final int axis=n;int y=82+n*20;button(358,y,26,"-",()->adjust(axis,-1));button(402,y,26,"+",()->adjust(axis,1));}
        button(268,207,160,FurnitureDesign.MATERIALS[part().material()],()->{var p=part();edit(new FurnitureDesign.Part(p.x(),p.y(),p.z(),p.w(),p.h(),p.d(),(p.material()+1)%FurnitureDesign.MATERIALS.length,p.sprite()));});
        button(268,231,160,part().sprite()?"Shape: flat sprite":"Shape: cuboid",()->{var p=part();edit(new FurnitureDesign.Part(p.x(),p.y(),p.z(),p.w(),p.h(),1,p.material(),!p.sprite()));});
        button(12,212,76,"Seat: "+design.seat(),()->features((design.seat()+2)%18,design.storage(),design.light(),design.effect(),design.sound())).setSelected(design.seat()>0);
        button(94,212,78,design.storage()?"Storage: 27":"Storage: off",()->features(design.seat(),!design.storage(),design.light(),design.effect(),design.sound())).setSelected(design.storage());
        button(178,212,78,"Light: "+design.light(),()->features(design.seat(),design.storage(),design.light()==0?7:design.light()==7?15:0,design.effect(),design.sound())).setSelected(design.light()>0);
        button(12,235,76,"Particles: "+design.effect(),()->features(design.seat(),design.storage(),design.light(),(design.effect()+1)%4,design.sound())).setSelected(design.effect()>0);
        button(94,235,78,"Sound: "+design.sound(),()->features(design.seat(),design.storage(),design.light(),design.effect(),(design.sound()+1)%4)).setSelected(design.sound()>0);
        button(178,235,78,"Library >",this::loadNext);
        button(268,261,76,"Delete saved",this::deleteSaved);button(350,261,78,"Done",this::onClose);
    }
    private FurnitureDesign.Part part(){return design.parts().get(selected);}
    private ScreenKit.TextButton button(int x,int y,int w,String text,Runnable action){return addRenderableWidget(new ScreenKit.TextButton(left+x,top+y,w,18,theme,Component.literal(text),b->action.run()));}
    private void rebuild(){clearWidgets();init();}
    private void replace(List<FurnitureDesign.Part> parts){design=new FurnitureDesign(design.name(),parts,design.seat(),design.storage(),design.light(),design.effect(),design.sound());}
    private void edit(FurnitureDesign.Part p){var parts=new ArrayList<>(design.parts());parts.set(selected,p);replace(parts);rebuild();}
    private void adjust(int axis,int delta){var p=part();int[] v={p.x(),p.y(),p.z(),p.w(),p.h(),p.d()};v[axis]+=delta;try{edit(new FurnitureDesign.Part(v[0],v[1],v[2],v[3],v[4],v[5],p.material(),p.sprite()));}catch(IllegalArgumentException e){message=e.getMessage();}}
    private void features(int seat,boolean storage,int light,int effect,int sound){design=new FurnitureDesign(design.name(),design.parts(),seat,storage,light,effect,sound);rebuild();}
    private void preset(int preset){
        design=switch(preset){
            case 1->new FurnitureDesign("Oak table",List.of(new FurnitureDesign.Part(0,13,0,16,3,16,0,false),new FurnitureDesign.Part(1,0,1,3,13,3,0,false),new FurnitureDesign.Part(12,0,1,3,13,3,0,false),new FurnitureDesign.Part(1,0,12,3,13,3,0,false),new FurnitureDesign.Part(12,0,12,3,13,3,0,false)),0,false,0,0,0);
            case 2->new FurnitureDesign("Workshop lamp",List.of(new FurnitureDesign.Part(3,0,3,10,2,10,3,false),new FurnitureDesign.Part(7,2,7,2,8,2,3,false),new FurnitureDesign.Part(3,10,3,10,6,10,4,false)),0,false,15,0,1);
            case 3->new FurnitureDesign("Wood cabinet",List.of(new FurnitureDesign.Part(1,0,1,14,14,14,1,false),new FurnitureDesign.Part(2,2,0,12,10,1,0,false),new FurnitureDesign.Part(12,6,0,1,2,1,3,false)),0,true,0,0,0);
            default->FurnitureDesign.stool();};selected=0;rebuild();
    }
    private void save(){if(!libraryReadable){message="Repair the preserved library file before saving.";return;}try{var updated=new ArrayList<>(library);updated.removeIf(d->d.name().equals(design.name()));updated.add(design);ArtisanLibrary.save(updated);library=updated;message="Saved locally: "+design.name()+" ("+library.size()+"/64)";}catch(java.io.IOException e){message=e.getMessage();}}
    private void deleteSaved(){if(!libraryReadable)return;try{var updated=new ArrayList<>(library);if(!updated.removeIf(d->d.name().equals(design.name()))){message="This design is not saved.";return;}ArtisanLibrary.save(updated);library=updated;libraryIndex=-1;message="Removed saved copy; current design kept.";}catch(java.io.IOException e){message=e.getMessage();}}
    private void loadNext(){if(library.isEmpty()){message="Your library is empty. Save a design first.";return;}libraryIndex=(libraryIndex+1)%library.size();design=library.get(libraryIndex);selected=0;message="Loaded "+design.name()+" · "+(libraryIndex+1)+"/"+library.size();rebuild();}
    private MouseButtonEvent scaled(MouseButtonEvent e){return new MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo());}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){return super.mouseClicked(scaled(e),twice);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return super.mouseReleased(scaled(e));}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return super.mouseDragged(scaled(e),dx/scale,dy/scale);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float delta){}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xCF111A20);g.pose().pushMatrix();g.pose().scale(scale,scale);
        ScreenKit.body(g,theme,left,top,W,H);
        // The name and the presets, the part's measurements and the message line each on their glass.
        ScreenKit.pane(g,theme,left+6,top+24,W-12,54);ScreenKit.pane(g,theme,left+262,top+79,172,123);ScreenKit.pane(g,theme,left+8,top+259,254,20);
        ScreenKit.title(g,font,theme,left,top,title,libraryReadable?theme.ok():theme.warn(),W-140);
        ScreenKit.chip(g,font,theme,left+W-5,top+1,"Cost: "+design.cost()+" materials",theme.text());
        preview(g,12,82,false);preview(g,142,82,true);
        g.text(font,"Part "+(selected+1)+" / "+design.parts().size(),left+12,top+178,theme.label(),false);
        int[] values={part().x(),part().y(),part().z(),part().w(),part().h(),part().d()};String[] labels={"Position X","Position Y","Position Z","Width","Height","Depth"};
        for(int i=0;i<6;i++){g.text(font,labels[i],left+268,top+87+i*20,theme.text(),false);g.text(font,""+values[i],left+386,top+87+i*20,theme.progress(),false);}
        String clipped=font.plainSubstrByWidth(message,246);g.text(font,clipped,left+12,top+265,libraryReadable?theme.text():theme.warn(),false);
        super.extractRenderState(g,Math.round(mx/scale),Math.round(my/scale),delta);g.pose().popMatrix();
    }
    @Override public ScreenRectangle panelArea(){return new ScreenRectangle(Math.round(left*scale),Math.round(top*scale),Math.round(W*scale),Math.round(H*scale));}
    private void preview(GuiGraphicsExtractor g,int x,int y,boolean overhead){
        x+=left;y+=top;ScreenKit.pane(g,theme,x,y,114,92);g.text(font,overhead?"TOP":"FRONT",x+5,y+4,theme.progress(),false);
        int gx=x+16,gy=y+22,size=4,line=(theme.dim()&0x00FFFFFF)|0x70000000;for(int i=0;i<=16;i++){g.fill(gx+i*size,gy,gx+i*size+1,gy+65,line);g.fill(gx,gy+i*size,gx+65,gy+i*size+1,line);}
        for(int i=0;i<design.parts().size();i++){var p=design.parts().get(i);int px=gx+p.x()*size,py=gy+(overhead?p.z():16-p.y()-p.h())*size,w=p.w()*size,h=(overhead?p.d():p.h())*size;g.fill(px,py,px+w,py+h,COLORS[p.material()]);if(i==selected){int c=theme.progress();g.fill(px,py,px+w,py+1,c);g.fill(px,py+h-1,px+w,py+h,c);g.fill(px,py,px+1,py+h,c);g.fill(px+w-1,py,px+w,py+h,c);}}
    }
}
