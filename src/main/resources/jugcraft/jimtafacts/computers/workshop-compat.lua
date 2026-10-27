-- Jugcraft adapter around the supplied read-only ROM. No host paths, native modules or HTTP.
local nativeLoad = _jugcraft_load
_jugcraft_load = nil
local romRead, romIndex = _jugcraft_rom_read, _jugcraft_rom_index
_jugcraft_rom_read, _jugcraft_rom_index = nil, nil
local files, directories = {}, { rom = true }
for name in romIndex:gmatch('[^\r\n]+') do
    files['rom/' .. name] = name
    local dir = 'rom/' .. name
    while dir:find('/') do dir = dir:match('^(.*)/[^/]+$'); directories[dir] = true end
end
local function path(value)
    if type(value) ~= 'string' or #value > 256 then error('Invalid virtual path', 3) end
    local parts = {}
    for part in value:gmatch('[^/]+') do
        if part == '..' then if #parts == 0 then error('Path escapes computer storage', 3) end; table.remove(parts)
        elseif part ~= '.' then parts[#parts + 1] = part end
    end
    return table.concat(parts, '/')
end
function load(source, name, mode, environment)
    if type(source) ~= 'string' or #source > 131072 or source:find('\27', 1, true) then return nil, 'Only bounded Lua source text is supported' end
    if mode and not mode:find('t', 1, true) then return nil, 'Binary chunks are disabled' end
    return nativeLoad(source, name or '@program', 't', environment or _G)
end
loadstring = load
-- Enough read-only introspection for ROM argument diagnostics; no hooks, upvalues or registry access.
local romRegistry={}
debug = { getmetatable = getmetatable, getinfo = function() return nil end, getregistry = function() return romRegistry end, traceback = function(message) return tostring(message or '') end }
local localOpen, localList, localExists, localDelete = fs.open, fs.list, fs.exists, fs.delete
local function diskPath(value)
    local mount,file=value:match('^(disk_disk_drive_[^/]+)/*(.*)$')
    if not mount then return nil end
    local name=mount:sub(6)
    if peripheral.isPresent(name) and peripheral.call(name,'isDiskPresent') then return name,file end
end
function fs.combine(...) return path(table.concat({...}, '/')) end
function fs.getName(value) return path(value):match('[^/]+$') or '' end
function fs.getDir(value) return path(value):match('^(.*)/[^/]+$') or '' end
function fs.exists(value) value=path(value);local drive,file=diskPath(value);if drive then return file=='' or file=='program.lua' end;return value=='' or files[value]~=nil or directories[value]~=nil or not value:find('/') and #value<=32 and localExists(value) or false end
function fs.isDir(value) value=path(value);local drive,file=diskPath(value);return value=='' or directories[value]==true or drive~=nil and file=='' end
function fs.isReadOnly(value) value=path(value);return value=='rom' or value:sub(1,4)=='rom/' end
function fs.list(value)
    value=path(value)
    if value=='' then local result=localList('');result[#result+1]='rom';for _,name in ipairs(peripheral.getNames()) do if peripheral.getType(name)=='drive' then local mount=peripheral.call(name,'getMountPath');if mount then result[#result+1]=mount end end end;table.sort(result);return result end
    local drive,file=diskPath(value);if drive and file=='' then return {'program.lua'} end
    if not directories[value] then error('Not a directory: '..value,2) end
    local found={};local prefix=value..'/'
    for name in pairs(files) do if name:sub(1,#prefix)==prefix then found[name:sub(#prefix+1):match('^[^/]+')]=true end end
    local result={};for name in pairs(found) do result[#result+1]=name end;table.sort(result);return result
end
function fs.open(value,mode)
    value=path(value)
    local drive,file=diskPath(value)
    if drive then
        if file~='program.lua' then return nil,'Disk holds one program.lua document' end
        if mode~='r' and mode~='w' and mode~='a' then return nil,'Disk modes: r, w, a' end
        local text=mode=='w' and '' or peripheral.call(drive,'readData');local offset,closed=1,false;local handle={}
        local function check() if closed then error('File is closed',3) end end
        if mode=='r' then
            function handle.readAll() check();local result=text:sub(offset);offset=#text+1;return result end
            function handle.readLine() check();if offset>#text then return nil end;local stop=text:find('\n',offset,true);local line=text:sub(offset,stop and stop-1 or #text);offset=stop and stop+1 or #text+1;return line end
            function handle.read(count) check();count=count or 1;if type(count)~='number' or count<1 or count>3500 or count%1~=0 then error('Read count: 1..3500') end;if offset>#text then return nil end;local result=text:sub(offset,offset+count-1);offset=offset+#result;return result end
        else
            function handle.write(value) check();value=tostring(value);if #text+#value>3500 then error('Disk Lua document limit: 3500 characters',2) end;text=text..value end
            function handle.writeLine(value) handle.write(tostring(value)..'\n') end
            function handle.flush() check();peripheral.call(drive,'writeData',text) end
        end
        function handle.close() check();if mode~='r' then handle.flush() end;closed=true end
        return handle
    end
    if fs.isReadOnly(value) then
        if mode~='r' and mode~='rb' then return nil,'ROM is read-only' end
        if not files[value] then return nil,'No such ROM file' end
        local text=romRead(files[value]);if not text then return nil,'ROM resource unavailable' end
        local position,closed=1,false
        local function check() if closed then error('File is closed',3) end end
        return {
            readAll=function() check();local rest=text:sub(position);position=#text+1;return rest end,
            readLine=function() check();if position>#text then return nil end;local ending=text:find('\n',position,true);local line=text:sub(position,ending and ending-1 or #text);position=ending and ending+1 or #text+1;return line:gsub('\r$','') end,
            read=function(count) check();if position>#text then return nil end;local amount=count or 1;if type(amount)~='number' or amount<1 or amount>131072 or amount%1~=0 then error('Invalid read count',2) end;local part=text:sub(position,position+amount-1);position=position+#part;if mode=='rb' and count==nil then return part:byte() end;return part end,
            close=function() check();closed=true end,
        }
    end
    return localOpen(value,mode)
end
function fs.delete(value) value=path(value);if fs.isReadOnly(value) then error('ROM is read-only',2) end;local drive,file=diskPath(value);if drive then if file~='program.lua' then error('Cannot remove disk mount',2) end;return peripheral.call(drive,'writeData','') end;return localDelete(value) end
function fs.getSize(value) local handle,why=fs.open(value,'r');if not handle then error(why,2) end;local length=#handle.readAll();handle.close();return length end
function fs.getDrive(value) return fs.isReadOnly(value) and 'rom' or 'computer' end
function fs.getCapacity(value) return fs.isReadOnly(value) and nil or 65536 end
function fs.getFreeSpace(value)
    if fs.isReadOnly(value) then return 0 end
    local size=0;for _,name in ipairs(localList('')) do size=size+fs.getSize(name) end;return math.max(0,65536-size)
end
function fs.copy(from,to) local input,why=fs.open(from,'r');if not input then error(why,2) end;local text=input.readAll();input.close();local output,reason=fs.open(to,'w');if not output then error(reason,2) end;output.write(text);output.close() end
function fs.move(from,to) if fs.isReadOnly(from) then error('ROM is read-only',2) end;fs.copy(from,to);fs.delete(from) end
function loadfile(name,mode,environment)
    local handle,reason=fs.open(name,'r');if not handle then return nil,reason end
    local text=handle.readAll();handle.close();return load(text,'@'..path(name),mode or 't',environment or _G)
end
function dofile(name) local fn,reason=loadfile(name);if not fn then error(reason,2) end;return fn() end
local loaded,loading={},{}
package={loaded=loaded,path='rom/modules/main/?.lua;rom/modules/main/?/init.lua'}
function require(name)
    if type(name)~='string' or #name>100 or not name:match('^[%w_.-]+$') then error('Invalid ROM module name',2) end
    if loaded[name]~=nil then return loaded[name] end
    if loading[name] then error('Circular module dependency: '..name,2) end
    local file='rom/modules/main/'..name:gsub('%.','/')..'.lua'
    if not fs.exists(file) then file='rom/modules/main/'..name:gsub('%.','/')..'/init.lua' end
    local fn,why=loadfile(file);if not fn then error(why,2) end
    loading[name]=true;local ok,value=pcall(fn,name);loading[name]=nil;if not ok then error(value,2) end
    loaded[name]=value==nil and true or value;return loaded[name]
end
function os.loadAPI(name)
    local environment=setmetatable({}, {__index=_G});local fn,why=loadfile(name,'t',environment)
    if not fn then return false,why end;local ok,failure=pcall(fn);if not ok then return false,failure end
    local exports={};for key,value in pairs(environment) do exports[key]=value end
    _G[fs.getName(name):gsub('%.lua$','')]=exports;return true
end
function os.unloadAPI(name) _G[name]=nil end
function os.version() return 'Jugcraft Workshop OS / bounded Lua ROM' end
function os.getComputerID() local ok,value=_jugcraft_request('identity:');if not ok then error(value,2) end;return tonumber(value) end
os.computerID=os.getComputerID
function os.getComputerLabel() local ok,value=_jugcraft_request('label:');if not ok then error(value,2) end;return value~='' and value or nil end
os.computerLabel=os.getComputerLabel
function os.setComputerLabel(value) if value~=nil and (type(value)~='string' or #value>64) then error('Label: at most 64 characters',2) end;local ok,why=_jugcraft_request('label:'..(value or ''));if not ok then error(why,2) end end
function os.shutdown() jugcraft.exec('stop') end
function os.run(environment,name,...)
    setmetatable(environment,{__index=_G});local fn,why=loadfile(name,'t',environment);if not fn then print(why);return false end
    local ok,failure=pcall(fn,...);if not ok then print(failure) end;return ok
end
function printError(...) return print(...) end
function sleep(seconds)
    local timer=os.startTimer(seconds or 0)
    repeat local _,id=os.pullEvent('timer');if id==timer then return end until false
end
os.sleep=sleep
for _,api in ipairs({'colors','vector','parallel','textutils','settings','keys','disk'}) do
    local ok,why=os.loadAPI('rom/apis/'..api..'.lua');if not ok then error('ROM API '..api..': '..tostring(why)) end
end
colours=colors
-- Terminal methods use the saved server canvas, never a client-controlled world operation.
local nativeTerm, redirected, blink = {}, nil, false
local function terminalCall(method,...)
    local args={method}
    for i=1,select('#',...) do
        local value=tostring(select(i,...))
        if value:find('[\t\r\n]') then error('Terminal arguments must be single-line',3) end
        args[#args+1]=value
    end
    local ok,value=_jugcraft_request('terminal:'..table.concat(args,'\t'))
    if not ok then error(value,3) end
    return value
end
for _,method in ipairs({'clear','clearLine','setCursorPos','setTextColor','setBackgroundColor','scroll','blit'}) do
    nativeTerm[method]=function(...) terminalCall(method,...) end
end
function nativeTerm.write(value)
    value=tostring(value):gsub('[\t\r\n]',' ')
    for i=1,#value,512 do terminalCall('write',value:sub(i,i+511)) end
end
for _,method in ipairs({'getSize','getCursorPos'}) do
    nativeTerm[method]=function() local a,b=terminalCall(method):match('^(%-?%d+)\t(%-?%d+)$');return tonumber(a),tonumber(b) end
end
for _,method in ipairs({'getTextColor','getBackgroundColor'}) do nativeTerm[method]=function() return tonumber(terminalCall(method)) end end
function nativeTerm.isColor() return terminalCall('isColor')=='true' end
function nativeTerm.setCursorBlink(value) blink=not not value end
function nativeTerm.getCursorBlink() return blink end
nativeTerm.isColour=nativeTerm.isColor
nativeTerm.setTextColour=nativeTerm.setTextColor;nativeTerm.getTextColour=nativeTerm.getTextColor
nativeTerm.setBackgroundColour=nativeTerm.setBackgroundColor;nativeTerm.getBackgroundColour=nativeTerm.getBackgroundColor
redirected=nativeTerm;term={}
for method in pairs(nativeTerm) do term[method]=function(...) return redirected[method](...) end end
function term.current() return redirected end
function term.native() return nativeTerm end
function term.redirect(target)
    if type(target)~='table' or target==term then error('Expected a terminal object',2) end
    for method in pairs(nativeTerm) do if type(target[method])~='function' then error('Terminal is missing '..method,2) end end
    local previous=redirected;redirected=target;return previous
end
local function newline()
    local _,y=term.getCursorPos();local _,height=term.getSize()
    if y>=height then term.scroll(1);y=height-1 end
    term.setCursorPos(1,y+1)
end
function write(value)
    value=tostring(value);local lines=0
    while #value>0 do
        local x=term.getCursorPos();local width=term.getSize()
        if x>width then newline();lines=lines+1;x=1 end
        local stop=value:find('\n',1,true)
        local count=math.min(width-x+1,stop and stop-1 or #value)
        if count>0 then term.write(value:sub(1,count));value=value:sub(count+1) end
        if value:sub(1,1)=='\n' then newline();lines=lines+1;value=value:sub(2) end
    end
    return lines
end
local logPrint=print
function print(...)
    local values={};for i=1,select('#',...) do values[i]=tostring(select(i,...)) end
    local text=table.concat(values,'\t');logPrint(text)
    local lines=write(text:gsub('\t','    '));newline();return lines+1
end
function read(replace)
    term.setCursorBlink(true)
    local line='';local startX,startY=term.getCursorPos();local width=term.getSize()
    while true do
        local event,value=os.pullEvent()
        if event=='jugcraft_line' then line=value;break
        elseif event=='char' and #line<256 then line=line..value
        elseif event=='key' and value==keys.backspace then line=line:sub(1,-2)
        elseif event=='key' and (value==keys.enter or value==keys.numPadEnter) then break end
        local shown=replace and tostring(replace):sub(1,1):rep(#line) or line
        term.setCursorPos(startX,startY);term.write((' '):rep(math.max(0,width-startX+1)));term.setCursorPos(startX,startY)
        term.write(shown:sub(-math.max(1,width-startX)))
    end
    term.setCursorPos(startX,startY);write(replace and tostring(replace):sub(1,1):rep(#line) or line);newline();term.setCursorBlink(false);return line
end
_HOST='Jugcraft Workshop computer'
shell={}
local runningProgram='workshop'
function shell.dir() return '' end
function shell.path() return '.:rom/programs:rom/programs/turtle' end
function shell.aliases() return {ls='list',dir='list'} end
function shell.getCompletionInfo() return {} end
function shell.getRunningProgram() return runningProgram end
function shell.resolve(name) return path(name) end
function shell.resolveProgram(name)
    for _,prefix in ipairs({'','rom/programs/','rom/programs/turtle/'}) do
        for _,suffix in ipairs({'','.lua'}) do local candidate=path(prefix..name..suffix);if fs.exists(candidate) and not fs.isDir(candidate) then return candidate end end
    end
end
function shell.programs()
    local found={};for _,dir in ipairs({'rom/programs','rom/programs/turtle'}) do for _,name in ipairs(fs.list(dir)) do if name:sub(-4)=='.lua' then found[name:sub(1,-5)]=true end end end
    local result={};for name in pairs(found) do result[#result+1]=name end;table.sort(result);return result
end
function shell.run(name,...)
    local target=shell.resolveProgram(name);if not target then printError('No such program: '..tostring(name));return false end
    local previous=runningProgram;runningProgram=target
    local args={...};args[0]=target;local ok=os.run({shell=shell,arg=args},target,...);runningProgram=previous;return ok
end
shell.execute=shell.run
settings.define('shell.autocomplete',{default=false,type='boolean'})
settings.define('shell.autocomplete_hidden',{default=false,type='boolean'})
settings.define('bios.strict_globals',{default=false,type='boolean'})
if fs.exists('.settings') then settings.load('.settings') end
local ok,kind=_jugcraft_request('kind:');if ok and kind~='' and kind:sub(1,7)~='turtle_' then turtle=nil end
