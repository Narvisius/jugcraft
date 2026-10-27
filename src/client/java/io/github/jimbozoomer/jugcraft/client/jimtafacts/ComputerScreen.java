package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.FittedContainerScreen;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.ComputerTerminal;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.WorkshopComputers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original supplied computer surround, readable editable program, live output and ordinary inventory. */
public final class ComputerScreen extends FittedContainerScreen<ComputerTerminal.Menu> {
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("computer");
    private MultiLineEditBox editor;
    private MultiLineEditBox log;
    private String accepted = "", lastOutput = "";
    private boolean received;
    private final java.util.List<Button> actions = new java.util.ArrayList<>();
    private Button previous, next;
    private Button programToggle,storeFile,loadFile,deleteFile,previousFiles,nextFiles;
    private EditBox fileName;
    private EditBox terminalInput;
    private Button sendInput;
    private Button terminalToggle,luaExample;
    private Button keyboardToggle;
    private boolean rawKeyboard;
    private boolean browsing;
    private int filePage,seenLoadRevision;
    private String armed="";
    private final java.util.List<Button> fileRows=new java.util.ArrayList<>();
    public ComputerScreen(ComputerTerminal.Menu menu, Inventory inventory, Component title) { super(menu, inventory, title, 360, 288); }
    @Override protected void init() {
        String draft=editor==null?menu.program:editor.getValue();String name=fileName==null?"program":fileName.getValue();
        super.init(); titleLabelY = 10000; inventoryLabelY = 10000;
        editor = new MultiLineEditBox.Builder().setX(leftPos + 16).setY(topPos + 32)
                .setShowBackground(false).setTextColor(theme.text()).setCursorColor(theme.progress())
                .setPlaceholder(Component.literal("print Hello, Jugcraft!\nstop"))
                .build(font, 155, 125, Component.literal("Computer program"));
        editor.setCharacterLimit(WorkshopComputers.MAX_PROGRAM); editor.setValue(draft); addRenderableWidget(editor);
        log = new MultiLineEditBox.Builder().setX(leftPos + 190).setY(topPos + 32)
                .setShowBackground(false).setTextColor(theme.ok()).setCursorColor(theme.ok())
                .build(font, 155, 103, Component.literal("Computer output"));
        log.setCharacterLimit(8192); log.setValue(menu.output); addRenderableWidget(log);
        terminalInput=addRenderableWidget(new EditBox(font,leftPos+190,topPos+140,115,17,Component.literal("Input for running Lua program")));
        terminalInput.setMaxLength(256);
        sendInput=addRenderableWidget(new ScreenKit.TextButton(leftPos+309,topPos+140,36,17,theme,Component.literal("Send"),b->{ClientPlayNetworking.send(new ComputerTerminal.Edit(menu.containerId,9,terminalInput.getValue()));terminalInput.setValue("");}));
        actions.clear();
        String[] labels = {"Save", "Run", "Stop", "To disk", "From disk", "Refuel", "Help"};
        for (int i = 0; i < labels.length; i++) {
            int action = i;
            actions.add(addRenderableWidget(new ScreenKit.TextButton(leftPos + 9 + i * 49, topPos + 174, 47, 18,theme,Component.literal(labels[i]), button -> {
                ClientPlayNetworking.send(new ComputerTerminal.Edit(menu.containerId, action, action == 0 ? editor.getValue() : ""));
                if (action == 0) accepted = editor.getValue();
            })));
        }
        previous=addRenderableWidget(new ScreenKit.TextButton(leftPos+16,topPos+174,145,18,theme,Component.literal("Previous page"),b->ClientPlayNetworking.send(new ComputerTerminal.Edit(menu.containerId,7,""))));
        next=addRenderableWidget(new ScreenKit.TextButton(leftPos+190,topPos+174,145,18,theme,Component.literal("Next page"),b->ClientPlayNetworking.send(new ComputerTerminal.Edit(menu.containerId,8,""))));
        luaExample=addRenderableWidget(new ScreenKit.TextButton(leftPos+190,topPos+4,90,16,theme,Component.literal("ROM shell"),b->{if(!armed.equals("lua-template")){armed="lua-template";b.setMessage(Component.literal("Replace draft?"));return;}editor.setValue("--!lua\nshell.run('shell')");armed="";b.setMessage(Component.literal("ROM shell"));browsing=false;}));
        terminalToggle=addRenderableWidget(new ScreenKit.TextButton(leftPos+105,topPos+4,78,16,theme,Component.literal("Terminal"),b->{menu.terminalView=!menu.terminalView;browsing=false;}));
        keyboardToggle=addRenderableWidget(new ScreenKit.TextButton(leftPos+190,topPos+4,90,16,theme,Component.literal("Line input"),b->{rawKeyboard=!rawKeyboard;setFocused(null);}));
        programToggle=addRenderableWidget(new ScreenKit.TextButton(leftPos+286,topPos+4,66,16,theme,Component.literal("Programs"),b->{browsing=!browsing;armed="";}));
        fileName=addRenderableWidget(new EditBox(font,leftPos+16,topPos+32,155,18,Component.literal("Saved program name")));
        fileName.setMaxLength(32);fileName.setValue(name);fileName.setResponder(v->armed="");
        fileRows.clear();
        for(int i=0;i<4;i++){
            int row=i;fileRows.add(addRenderableWidget(new ScreenKit.TextButton(leftPos+16,topPos+55+i*19,155,17,theme,Component.empty(),b->{int index=filePage*4+row;if(index<menu.files.size())fileName.setValue(menu.files.get(index));})));
        }
        previousFiles=addRenderableWidget(new ScreenKit.TextButton(leftPos+16,topPos+132,24,16,theme,Component.literal("<"),b->{filePage=Math.max(0,filePage-1);armed="";}));
        nextFiles=addRenderableWidget(new ScreenKit.TextButton(leftPos+147,topPos+132,24,16,theme,Component.literal(">"),b->{filePage=Math.min(3,filePage+1);armed="";}));
        storeFile=addRenderableWidget(new ScreenKit.TextButton(leftPos+16,topPos+151,49,17,theme,Component.literal("Store"),b->fileAction(0)));
        loadFile=addRenderableWidget(new ScreenKit.TextButton(leftPos+69,topPos+151,49,17,theme,Component.literal("Load"),b->fileAction(1)));
        deleteFile=addRenderableWidget(new ScreenKit.TextButton(leftPos+122,topPos+151,49,17,theme,Component.literal("Delete"),b->fileAction(2)));
    }
    private void fileAction(int action){
        String name=fileName.getValue(),key=action+":"+name;
        boolean confirm=action==2||action==0&&menu.files.contains(name)||action==1&&!editor.getValue().equals(accepted);
        if(confirm&&!armed.equals(key)){armed=key;return;}
        ClientPlayNetworking.send(new ComputerTerminal.FileEdit(menu.containerId,action,name,action==0?editor.getValue():""));armed="";
        // A successful server acknowledgment replaces the editor; a failed load preserves its draft.
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        if(menu.loadRevision!=seenLoadRevision){seenLoadRevision=menu.loadRevision;editor.setValue(menu.program);accepted=menu.program;browsing=false;}
        // Preserve unsaved edits when live output arrives; disk/load updates replace only an unmodified editor.
        if ((!received || editor.getValue().equals(accepted)) && !menu.program.equals(accepted)) {
            editor.setValue(menu.program); accepted = menu.program;
        }
        received = !menu.output.equals("Connecting to the workshop terminal…");
        if (!menu.output.equals(lastOutput)) { log.setValue(menu.output); lastOutput = menu.output; }
        editor.active = !menu.readOnly;
        terminalInput.visible=sendInput.visible=menu.hardware&&!menu.readOnly;
        terminalInput.active=sendInput.active=menu.running;
        browsing&=menu.fileStorage&&!menu.readOnly;
        programToggle.visible=menu.fileStorage&&!menu.readOnly;programToggle.setMessage(Component.literal(browsing?"Editor":"Programs"));
        editor.visible=!browsing;
        fileName.visible=fileName.active=browsing;
        for(int i=0;i<fileRows.size();i++){
            int index=filePage*4+i;var button=fileRows.get(i);button.visible=browsing;button.active=index<menu.files.size();
            button.setMessage(Component.literal(index<menu.files.size()?menu.files.get(index):"—"));
        }
        filePage=Math.min(filePage,Math.max(0,(menu.files.size()-1)/4));
        previousFiles.visible=nextFiles.visible=storeFile.visible=loadFile.visible=deleteFile.visible=browsing;
        previousFiles.active=filePage>0;nextFiles.active=(filePage+1)*4<menu.files.size();
        storeFile.active=fileName.getValue().matches("[a-zA-Z0-9_.-]{1,32}")&&!java.util.Set.of(".","..","rom").contains(fileName.getValue());loadFile.active=deleteFile.active=menu.files.contains(fileName.getValue());
        storeFile.setMessage(Component.literal(armed.equals("0:"+fileName.getValue())?"Replace?":"Store"));
        loadFile.setMessage(Component.literal(armed.equals("1:"+fileName.getValue())?"Discard?":"Load"));
        deleteFile.setMessage(Component.literal(armed.equals("2:"+fileName.getValue())?"Delete?":"Delete"));
        actions.forEach(button->button.visible=!menu.readOnly);
        previous.visible=next.visible=menu.readOnly;
        terminalToggle.visible=menu.hardware&&!menu.readOnly;terminalToggle.setMessage(Component.literal(menu.terminalView?"Editor":"Terminal"));
        luaExample.visible=!menu.readOnly&&!menu.terminalView;
        keyboardToggle.visible=menu.terminalView;keyboardToggle.setMessage(Component.literal(rawKeyboard?"Direct keys":"Line input"));
        if(menu.terminalView){
            editor.visible=log.visible=programToggle.visible=false;
            actions.forEach(button->button.visible=false);actions.get(2).visible=true;actions.get(2).setX(leftPos+286);actions.get(2).setY(topPos+4);
            terminalInput.setX(leftPos+16);terminalInput.setY(topPos+247);terminalInput.setWidth(270);sendInput.setX(leftPos+294);sendInput.setY(topPos+247);
            terminalInput.visible=sendInput.visible=!rawKeyboard;
        }else{
            log.visible=true;actions.get(2).setX(leftPos+9+2*49);actions.get(2).setY(topPos+174);
            terminalInput.setX(leftPos+190);terminalInput.setY(topPos+140);terminalInput.setWidth(115);sendInput.setX(leftPos+309);sendInput.setY(topPos+140);
        }
        super.extractRenderState(g,x,y,delta);
    }
    @Override protected void background(GuiGraphicsExtractor g,int x,int y,float delta) {
        ScreenKit.frame(g,theme,leftPos,topPos,imageWidth,imageHeight,menu);
        if(menu.terminalView){
            g.text(font,Component.literal("Workshop OS"),leftPos+12,topPos+8,0xFFE4EAE6,false);
            g.fill(leftPos+12,topPos+26,leftPos+348,topPos+236,0xFF111713);
            int count=Math.min(1152,Math.min(menu.canvasText.length(),Math.min(menu.canvasForeground.length(),menu.canvasBackground.length())));
            for(int i=0;i<count;i++){
                int cx=leftPos+17+i%48*6,cy=topPos+30+i/48*8;
                int front=Character.digit(menu.canvasForeground.charAt(i),16),back=Character.digit(menu.canvasBackground.charAt(i),16);
                g.fill(cx,cy,cx+6,cy+8,palette(back));
                char letter=menu.canvasText.charAt(i);if(letter!=' '){g.pose().pushMatrix();g.pose().translate(cx,cy);g.pose().scale(.8F,.8F);g.text(font,String.valueOf(letter),0,0,palette(front),false);g.pose().popMatrix();}
            }
            g.text(font,menu.energy+" JE · "+(menu.running?"Running":"Stopped"),leftPos+16,topPos+271,theme.ok(),false);return;
        }
        ScreenKit.pane(g,theme,leftPos+5,topPos+26,174,141);
        ScreenKit.pane(g,theme,leftPos+179,topPos+26,176,141);
        g.text(font, title, leftPos + 12, topPos + 8, 0xFFE4EAE6, false);
        g.text(font, menu.running ? "RUNNING" : "READY", leftPos + 190, topPos + 161, menu.running ? 0xFF93E3A8 : 0xFFABB5AD, false);
        if(browsing)g.text(font,(filePage+1)+" / "+Math.max(1,(menu.files.size()+3)/4)+" · "+menu.files.size()+" saved",leftPos+47,topPos+136,0xFFB3C5B8,false);
        g.text(font, menu.hardware ? "Device / disk / turtle storage" : "Portable program editor", leftPos + 11, topPos + 194, 0xFFD1D7D2, false);
        g.text(font, "Inventory", leftPos + 188, topPos + 194, 0xFFD1D7D2, false);
        if (menu.hardware) for (int i = 0; i < 18; i++) slot(g, leftPos + 11 + i % 9 * 18, topPos + 203 + i / 9 * 18);
        for (int i = 0; i < 27; i++) slot(g, leftPos + 187 + i % 9 * 18, topPos + 203 + i / 9 * 18);
        for (int i = 0; i < 9; i++) slot(g, leftPos + 187 + i * 18, topPos + 261);
        g.text(font, menu.hardware ? menu.energy + " / 16000 JE" : "Save preserves this item's program", leftPos + 12, topPos + 251, 0xFF9ACDB1, false);
        if (menu.turtle) g.text(font, "Slots 17/18: left/right upgrades", leftPos + 12, topPos + 267, 0xFFB3C5B8, false);
    }
    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF161A18); g.outline(x, y, 18, 18, 0xFF646F67);
    }
    private static int palette(int color){return 0xFF000000|net.minecraft.world.item.DyeColor.byId(Math.clamp(color,0,15)).getTextureDiffuseColor();}
    private void key(String kind,int code){ClientPlayNetworking.send(new ComputerTerminal.Edit(menu.containerId,10,kind+"\t"+code));}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){
        if(menu.terminalView&&rawKeyboard&&menu.running&&event.input()!=256){key("key",event.input());return true;}
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(net.minecraft.client.input.KeyEvent event){
        if(menu.terminalView&&rawKeyboard&&menu.running&&event.input()!=256){key("key_up",event.input());return true;}
        return super.keyReleased(event);
    }
    @Override public boolean charTyped(net.minecraft.client.input.CharacterEvent event){
        if(menu.terminalView&&rawKeyboard&&menu.running){if(event.codepoint()>=32&&event.codepoint()<=126)key("char",event.codepoint());return true;}
        return super.charTyped(event);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {}
}
