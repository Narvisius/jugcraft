package io.github.jimbozoomer.jugcraft.jimtafacts.computers.lua;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.*;
import org.squiddev.cobalt.function.*;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.lib.CoreLibraries;
import static org.squiddev.cobalt.ValueFactory.*;

/** Private child-process entry point. No Minecraft, filesystem, network, package or JVM API is exposed to Lua. */
public final class LuaWorker {
    private static String request;
    private LuaWorker() {}
    public static void main(String[] ignored) throws Exception {
        try(var input=new DataInputStream(System.in);var output=new DataOutputStream(System.out)) {
            try {
            String source=input.readUTF();
            if(source.length()>8192||source.indexOf('\u001b')>=0)throw new IOException("Only bounded Lua source is accepted");
            var state=LuaState.builder().interruptHandler(()->InterruptAction.SUSPEND).build();
            CoreLibraries.standardGlobals(state);
            org.squiddev.cobalt.lib.Bit32Lib.add(state);
            var globals=state.globals();
            globals.rawset("_jugcraft_load",globals.rawget("load"));
            for(String name:new String[]{"loadfile","dofile","load","loadstring","require","package","io","debug","collectgarbage"})globals.rawset(name,Constants.NIL);
            var romIndex=resource("rom-index.txt",32768);
            var romFiles=new java.util.HashSet<>(romIndex.lines().toList());
            globals.rawset("_jugcraft_rom_index",valueOf(romIndex));
            globals.rawset("_jugcraft_rom_read",LibFunction.create((LuaState vm,LuaValue path)->{
                String name=path.checkString();if(!romFiles.contains(name))return Constants.NIL;
                try{return valueOf(resource("rom/"+name,131072));}catch(IOException failure){throw new LuaError("Bundled ROM file is unavailable");}
            }));
            var bridge=new ResumableVarArgFunction<Void>() {
                @Override protected Varargs invoke(LuaState state,DebugFrame frame,Varargs args)throws LuaError,UnwindThrowable {
                    String value=args.arg(1).checkString();
                    if(value.length()>4096)throw new LuaError("Host request exceeds 4096 characters");
                    request=value;
                    state.interrupt();state.handleInterrupt();
                    throw new IllegalStateException("Host suspension was not honored");
                }
                @Override public Varargs resume(LuaState state,Void saved,Varargs reply){return reply;}
            };
            globals.rawset("_jugcraft_request",bridge);
            String prelude="""
                jugcraft = {}
                function jugcraft.exec(command) return _jugcraft_request('exec:' .. command) end
                function print(...)
                  local parts = {}
                  for i=1,select('#',...) do parts[i]=tostring(select(i,...)) end
                  return _jugcraft_request('print:' .. table.concat(parts,'\t'))
                end
                function sleep(seconds)
                  seconds=tonumber(seconds) or 0
                  return _jugcraft_request('sleep:' .. math.max(0,math.min(seconds,3600)))
                end
                turtle = {}
                for name,command in pairs({forward='move front',back='move back',up='move up',down='move down',
                  turnLeft='turn left',turnRight='turn right',dig='dig front',digUp='dig up',digDown='dig down',
                  place='place front',placeUp='place up',placeDown='place down'}) do
                  local instruction=command
                  turtle[name]=function() return jugcraft.exec(instruction) end
                end
                function turtle.select(slot) return jugcraft.exec('select '..tostring(slot)) end
                function jugcraft.read(name) return _jugcraft_request('readfile:'..name) end
                function jugcraft.write(name,text) return _jugcraft_request('writefile:'..name..':'..text) end
                function jugcraft.files() local ok,text=_jugcraft_request('files:');local result={};if ok then for name in text:gmatch('[^\\n]+') do result[#result+1]=name end end;return result end
                function jugcraft.receive() return _jugcraft_request('mail:') end
                os={sleep=sleep}
                function os.clock() local ok,value=_jugcraft_request('clock:');return ok and tonumber(value) or 0 end
                function turtle.getSelectedSlot() local ok,value=_jugcraft_request('selected:');return ok and tonumber(value) or 1 end
                function turtle.getItemCount(slot) local ok,value=_jugcraft_request('count:'..tostring(slot or turtle.getSelectedSlot()));return ok and tonumber(value) or 0 end
                function turtle.detect() local ok,value=_jugcraft_request('detect:front');return ok and value=='true' end
                function turtle.detectUp() local ok,value=_jugcraft_request('detect:up');return ok and value=='true' end
                function turtle.detectDown() local ok,value=_jugcraft_request('detect:down');return ok and value=='true' end
                function turtle.getFuelLevel() local ok,value=_jugcraft_request('energy:');return ok and tonumber(value) or 0 end
                function turtle.getFuelLimit() return 16000 end
                function turtle.refuel() return jugcraft.exec('refuel') end
                function turtle.transferTo(slot,count) return jugcraft.exec('transferslot '..tostring(slot)..' '..tostring(count or 64)) end
                function turtle.equipLeft() return jugcraft.exec('equip left') end
                function turtle.equipRight() return jugcraft.exec('equip right') end
                function turtle.craft() return jugcraft.exec('craft') end
                for name,command in pairs({attack='attack front',attackUp='attack up',attackDown='attack down',
                  suck='pull front',suckUp='pull up',suckDown='pull down',drop='push front',dropUp='push up',dropDown='push down'}) do
                  local instruction=command
                  turtle[name]=function(count) return jugcraft.exec(instruction..(instruction:sub(1,6)=='attack' and '' or ' '..tostring(count or 64))) end
                end
                redstone={}
                function redstone.getSides() return {'front','back','left','right','up','down'} end
                function redstone.getAnalogInput(side) local ok,value=_jugcraft_request('signalread:'..side);if not ok then error(value,2) end;return tonumber(value) end
                function redstone.getAnalogOutput(side) local ok,value=_jugcraft_request('signalout:'..side);if not ok then error(value,2) end;return tonumber(value) end
                function redstone.setAnalogOutput(side,value) local ok,why=jugcraft.exec('signal '..side..' '..tostring(value));if not ok then error(why,2) end end
                function redstone.getInput(side) return redstone.getAnalogInput(side)>0 end
                function redstone.getOutput(side) return redstone.getAnalogOutput(side)>0 end
                function redstone.setOutput(side,value) redstone.setAnalogOutput(side,value and 15 or 0) end
                redstone.getAnalogueInput=redstone.getAnalogInput;redstone.getAnalogueOutput=redstone.getAnalogOutput
                redstone.setAnalogueOutput=redstone.setAnalogOutput;rs=redstone
                -- Flat, saved computer storage only. No host paths or executable file loading.
                fs={}
                local function filename(name)
                  if type(name)~='string' or #name<1 or #name>32 or not name:match('^[%w_.-]+$') or name=='.' or name=='..' or name=='rom' then error('Use a flat file name (1..32 letters, digits, dot, underscore or dash)',3) end
                  return name
                end
                function fs.list(path) if path~='' and path~='/' then error('Only the root program shelf exists',2) end;return jugcraft.files() end
                function fs.exists(name) local ok,value=_jugcraft_request('existsfile:'..filename(name));return ok and value=='true' end
                function fs.delete(name) local ok,why=_jugcraft_request('deletefile:'..filename(name));if not ok then error(why,2) end end
                function fs.isDir(name) return name=='' or name=='/' end
                function fs.isReadOnly() return false end
                function fs.open(name,mode)
                  name=filename(name);if mode~='r' and mode~='w' and mode~='a' then return nil,'Modes: r, w, a' end
                  local text=''
                  if mode~='w' then local ok,value=jugcraft.read(name);if ok then text=value elseif mode=='r' then return nil,value end end
                  local closed=false;local offset=1;local file={}
                  local function check() if closed then error('File is closed',3) end end
                  if mode=='r' then
                    function file.readAll() check();local value=text:sub(offset);offset=#text+1;return value end
                    function file.readLine() check();if offset>#text then return nil end;local finish=text:find('\\n',offset,true);local value=text:sub(offset,finish and finish-1 or #text);offset=finish and finish+1 or #text+1;return value end
                    function file.read(count) check();count=count or 1;if type(count)~='number' or count<1 or count>4096 or count%1~=0 then error('Read count: 1..4096',2) end;if offset>#text then return nil end;local value=text:sub(offset,offset+count-1);offset=offset+#value;return value end
                  else
                    function file.write(value) check();value=tostring(value);if #text+#value>3500 then error('Lua writable file limit: 3500 characters',2) end;text=text..value end
                    function file.writeLine(value) file.write(tostring(value)..'\\n') end
                    function file.flush() check();local ok,why=jugcraft.write(name,text);if not ok then error(why,2) end end
                  end
                  function file.close() check();if mode~='r' then file.flush() end;closed=true end
                  return file
                end
                local events={};local timers={};local nextTimer=0;local pollSleep=sleep;local rootThread=coroutine.running()
                function os.queueEvent(name,...)
                  if type(name)~='string' or #name>64 then error('Invalid event name',2) end
                  if #events>=64 then error('Event queue is full (64)',2) end
                  events[#events+1]=table.pack(name,...)
                end
                function os.startTimer(seconds)
                  if type(seconds)~='number' or seconds~=seconds or seconds<0 or seconds>3600 then error('Timer: 0..3600 seconds',2) end
                  local count=0;for _ in pairs(timers) do count=count+1 end;if count>=32 then error('At most 32 timers',2) end
                  nextTimer=nextTimer+1;timers[nextTimer]=os.clock()+seconds;return nextTimer
                end
                function os.cancelTimer(id) timers[id]=nil end
                function os.pullEventRaw(filter)
                  if coroutine.running()~=rootThread then return coroutine.yield(filter) end
                  while true do
                    if next(timers)~=nil then local now=os.clock();for id,deadline in pairs(timers) do if deadline<=now and #events<64 then events[#events+1]=table.pack('timer',id);timers[id]=nil end end end
                    while #events>0 do local event=table.remove(events,1);if filter==nil or event[1]==filter or event[1]=='terminate' then return table.unpack(event,1,event.n) end end
                    local ok,text=_jugcraft_request('event:')
                    if ok and text~='' then
                      local name,a,b,c=text:match('^([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)\\t(.*)$')
                      if name=='modem_message' then os.queueEvent(name,'network',tonumber(a),tonumber(b),c)
                      elseif name=='monitor_touch' then os.queueEvent(name,a,tonumber(b),tonumber(c))
                      elseif name=='jugcraft_line' then os.queueEvent(name,c) end
                      if name=='char' then os.queueEvent('char',string.char(tonumber(a)))
                      elseif name=='key' then os.queueEvent('key',tonumber(a),false)
                      elseif name=='key_up' then os.queueEvent('key_up',tonumber(a)) end
                    else pollSleep(0.05) end
                  end
                end
                function os.pullEvent(filter) local result=table.pack(os.pullEventRaw(filter));if result[1]=='terminate' then error('Terminated',0) end;return table.unpack(result,1,result.n) end
                function read() local _,line=os.pullEvent('jugcraft_line');return line end
                peripheral={}
                local function peripheralRequest(...)
                  local fields={...};for i=1,#fields do fields[i]=tostring(fields[i]);if not(i==3 and fields[2]=='writeData' and #fields==3) and fields[i]:find('[\\t\\n\\r]') then error('Invalid peripheral argument',3) end end
                  local ok,value=_jugcraft_request('peripheral:'..table.concat(fields,'\\t'));if not ok then error(value,3) end;return value
                end
                function peripheral.getNames() local result={};for name in peripheralRequest('names'):gmatch('[^\\n]+') do result[#result+1]=name end;return result end
                function peripheral.isPresent(name) for _,value in ipairs(peripheral.getNames()) do if name==value then return true end end;return false end
                function peripheral.getType(name) if not peripheral.isPresent(name) then return nil end;return peripheralRequest(name,'type') end
                function peripheral.getMethods(name)
                  local kind=peripheral.getType(name)
                  if kind=='inventory' then return {'size','list','pushItems','pullItems'}
                  elseif kind=='fluid_storage' then return {'tanks','pushFluid','pullFluid'}
                  elseif kind=='modem' then return {'isWireless','isOpen','open','close','closeAll','transmit'}
                  elseif kind=='drive' then return {'isDiskPresent','hasData','hasAudio','getAudioTitle','playAudio','stopAudio','getDiskID','getMountPath','getDiskLabel','setDiskLabel','ejectDisk','readData','writeData'}
                  elseif kind=='printer' then return {'getPaperLevel','getInkLevel','getPageSize','getCursorPos','newPage','endPage','setCursorPos','setPageTitle','write'}
                  elseif kind=='monitor' then return {'getSize','getCursorPos','isColor','isColour','getTextColor','getTextColour','getBackgroundColor','getBackgroundColour','clear','write','setCursorPos','scroll','blit','setTextColor','setTextColour','setBackgroundColor','setBackgroundColour'}
                  elseif kind=='speaker' then return {'playNote'} end
                  return nil
                end
                function peripheral.call(name,method,...)
                  local text
                  if method=='setDiskLabel' and select(1,...)==nil then text=peripheralRequest(name,method,'') else text=peripheralRequest(name,method,...) end
                  if method=='list' or method=='tanks' then local result={};for slot,id,count in text:gmatch('(%d+)\\t([^\\t]+)\\t(%d+)\\n') do result[tonumber(slot)]=method=='list' and {name=id,count=tonumber(count)} or {name=id,amount=tonumber(count)} end;return result end
                  if method=='getSize' or method=='getPageSize' or method=='getCursorPos' then local x,y=text:match('^(%-?%d+)\\t(%-?%d+)$');return tonumber(x),tonumber(y) end
                  if method=='getDiskLabel' or method=='getMountPath' or method=='getDiskID' or method=='getAudioTitle' then return text~='' and text or nil end
                  if method=='readData' then return text end
                  if text=='true' or text=='false' then return text=='true' end
                  return tonumber(text)
                end
                function peripheral.wrap(name)
                  local methods=peripheral.getMethods(name);if not methods then return nil end
                  local result={};for _,method in ipairs(methods) do local call=method;result[call]=function(...) return peripheral.call(name,call,...) end end;return result
                end
                function peripheral.find(kind,filter)
                  local result={};for _,name in ipairs(peripheral.getNames()) do if peripheral.getType(name)==kind then local object=peripheral.wrap(name);if object and (not filter or filter(name,object)) then result[#result+1]=object end end end
                  return table.unpack(result)
                end
                """;
            var thread=new LuaThread(state,LoadState.load(state,new ByteArrayInputStream((prelude+"\n"+resource("workshop-compat.lua",65536)+"\n"+source).getBytes(StandardCharsets.UTF_8)),"@workshop",globals));
            thread.getDebugState().setHook(new DebugHook(){
                @Override public void onCount(LuaState s,DebugState debug,DebugFrame frame)throws LuaError,UnwindThrowable{s.interrupt();s.handleInterrupt();}
            },false,false,false,2000);
            LuaThread active=thread;
            while(true) {
                boolean success=input.readBoolean();String reply=input.readUTF();request=null;
                try {
                    LuaThread.run(active,varargsOf(valueOf(success),valueOf(reply)));
                    active=state.getCurrentThread();
                    int status=thread.getStatus()==LuaThread.Status.DEAD?2:request==null?0:1;
                    output.writeByte(status);output.writeUTF(request==null?"":request);output.flush();
                    if(status==2)return;
                }catch(LuaError failure){output.writeByte(3);output.writeUTF(bounded(failure.getMessage()));output.flush();return;}
            }
            } catch(Exception failure) { output.writeByte(3);output.writeUTF(bounded(failure.getMessage()));output.flush(); }
        }
    }
    private static String bounded(String text){return text==null?"Lua execution failed":text.substring(0,Math.min(text.length(),4096));}
    private static String resource(String name,int limit)throws IOException{
        try(var stream=LuaWorker.class.getResourceAsStream("/jugcraft/jimtafacts/computers/"+name)){
            if(stream==null)throw new IOException("Missing bundled computer resource: "+name);
            byte[] bytes=stream.readNBytes(limit+1);if(bytes.length>limit)throw new IOException("Bundled computer resource exceeds limit");return new String(bytes,StandardCharsets.UTF_8);
        }
    }
}
