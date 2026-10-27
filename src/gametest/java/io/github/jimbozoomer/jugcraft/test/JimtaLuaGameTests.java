package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.computers.lua.LuaProcess;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Deferred real child-process regression; never invoked by the resource generators. */
public final class JimtaLuaGameTests {
    @GameTest(maxTicks=600)
    public void isolatedLuaResumesCoroutinesAndBoundsAllocationFailure(GameTestHelper h){
        var worker=LuaProcess.open("assert(vector.new(1,2,3):dot(vector.new(4,5,6))==32); assert(colors.combine(colors.red,colors.blue)==18432); assert(textutils.unserializeJSON('{\"value\":7}').value==7); assert(fs.isReadOnly('rom/programs/turtle/go.lua')); local sum=0; for i=1,5000 do sum=sum+i end; local child=coroutine.create(function() print('child'); print('continued') end); assert(coroutine.resume(child)); print(sum); assert(io==nil and package.loadlib==nil and debug.sethook==nil)");
        var messages=new ArrayList<String>();
        poll(h,worker,messages,0);
    }
    private void poll(GameTestHelper h,LuaProcess worker,List<String> messages,int ticks){
        try{
            h.assertTrue(ticks<450,"Lua execution must finish within the bounded test window");
            var reply=worker.poll();
            if(reply!=null){
                h.assertTrue(reply.status()!=3,"Lua worker returned an error: "+reply.text());
                if(reply.status()==1&&reply.text().startsWith("print:"))messages.add(reply.text());
                if(reply.status()==2){
                    worker.close();
                    h.assertTrue(messages.size()==3&&messages.get(0).equals("print:child")&&messages.get(1).equals("print:continued")&&messages.get(2).startsWith("print:12502500"),"Host calls inside a child coroutine must resume exactly once, with arithmetic preserved");
                    var allocation=LuaProcess.open("print('allocation-ready'); local value=string.rep('x',134217728); print(#value)");
                    allocation(h,allocation,0,false);return;
                }
                worker.resume(true,hostReply(reply.text()));
            }
            h.runAfterDelay(1,()->poll(h,worker,messages,ticks+1));
        }catch(RuntimeException|AssertionError failure){worker.close();throw failure;}
    }
    private void allocation(GameTestHelper h,LuaProcess worker,int ticks,boolean ready){
        if(ticks>=140){worker.close();h.fail("Oversized Lua allocation did not terminate within its deadline");return;}
        try{
            var reply=worker.poll();
            if(reply!=null){
                if(reply.status()==3){worker.close();h.assertTrue(ready,"Runtime must reach allocation sentinel before its bounded failure");h.succeed();return;}
                if(reply.status()==1){h.assertTrue(reply.text().equals("kind:")||reply.text().equals("existsfile:.settings")||reply.text().startsWith("terminal:")||reply.text().equals("print:allocation-ready"),"Oversized allocation cannot emit a result");if(reply.text().equals("print:allocation-ready"))ready=true;}
                h.assertTrue(reply.status()!=2,"Oversized allocation cannot complete");
                worker.resume(true,hostReply(reply.text()));
            }
        }catch(IllegalArgumentException expectedWorkerFailure){worker.close();h.assertTrue(ready,"Worker startup/API failure is not evidence of an allocation bound");h.succeed();return;}
        boolean reached=ready;h.runAfterDelay(1,()->allocation(h,worker,ticks+1,reached));
    }
    @GameTest(maxTicks=600)
    public void originalRomShellListsFilesAndAcceptsExit(GameTestHelper h){
        var worker=LuaProcess.open("assert(shell.run('shell','list','rom')); assert(shell.run('shell')); print('shell-finished')");
        shellPoll(h,worker,0,false);
    }
    private void shellPoll(GameTestHelper h,LuaProcess worker,int ticks,boolean finished){
        try{
            h.assertTrue(ticks<500,"ROM shell must complete its command and interactive exit");
            var reply=worker.poll();
            if(reply!=null){
                h.assertTrue(reply.status()!=3,"ROM shell failed: "+reply.text());
                if(reply.text().equals("print:shell-finished"))finished=true;
                if(reply.status()==2){worker.close();h.assertTrue(finished,"Both original shell paths completed");h.succeed();return;}
                worker.resume(true,reply.text().equals("event:")?"jugcraft_line\t0\t0\texit":hostReply(reply.text()));
            }
            boolean reached=finished;h.runAfterDelay(1,()->shellPoll(h,worker,ticks+1,reached));
        }catch(RuntimeException|AssertionError failure){worker.close();throw failure;}
    }
    private static String hostReply(String request){
        if(request.equals("terminal:getTextColor"))return "1";
        if(request.equals("terminal:getBackgroundColor"))return "32768";
        if(request.equals("kind:"))return "turtle_normal";
        if(request.equals("terminal:getSize"))return "48\t24";
        if(request.equals("terminal:getCursorPos"))return "1\t1";
        if(request.equals("terminal:isColor"))return "true";
        return "";
    }
}
