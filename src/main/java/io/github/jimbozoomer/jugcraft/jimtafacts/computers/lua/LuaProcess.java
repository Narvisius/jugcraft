package io.github.jimbozoomer.jugcraft.jimtafacts.computers.lua;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.squiddev.cobalt.LuaState;

/** Four isolated workers maximum; all blocking pipe I/O stays off the server thread. */
public final class LuaProcess implements AutoCloseable {
    public record Reply(int status,String text) {}
    private static final Set<LuaProcess> ACTIVE=ConcurrentHashMap.newKeySet();
    private static final ExecutorService IO=new ThreadPoolExecutor(4,4,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(8),r->{var t=new Thread(r,"Jugcraft Lua I/O");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final ScheduledExecutorService WATCHDOG=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"Jugcraft Lua deadline");t.setDaemon(true);return t;});
    static { WATCHDOG.scheduleAtFixedRate(()->{for(var worker:ACTIVE){var pending=worker.pending;if(pending!=null&&!pending.isDone()&&System.nanoTime()-worker.deadline>=0){worker.failure="Lua response deadline exceeded";worker.close();}}},250,250,TimeUnit.MILLISECONDS); }
    private volatile Process process;
    private volatile boolean closed;
    private volatile String failure;
    private DataInputStream input;
    private DataOutputStream output;
    private volatile CompletableFuture<Reply> pending;
    private volatile long deadline;
    public static synchronized LuaProcess open(String source) {
        if(ACTIVE.size()>=4)throw new IllegalArgumentException("Four Lua computers are already running. Stop one and retry.");
        if(source.length()>8192)throw new IllegalArgumentException("Lua source limit is 8192 characters");
        var worker=new LuaProcess();ACTIVE.add(worker);
        worker.deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        try { worker.pending=CompletableFuture.supplyAsync(()->{
            try {
                String executable=System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")?"java.exe":"java";
                String java=Path.of(System.getProperty("java.home"),"bin",executable).toString();
                String classpath=location(LuaWorker.class)+File.pathSeparator+location(LuaState.class);
                var resources=LuaWorker.class.getResource("/jugcraft/jimtafacts/computers/workshop-compat.lua");
                if(resources!=null&&resources.getProtocol().equals("file")){
                    Path root=Path.of(resources.toURI());for(int i=0;i<4;i++)root=root.getParent();classpath+=File.pathSeparator+root;
                }
                Process launched=new ProcessBuilder(java,"-Xms8m","-Xmx64m","-Xss512k","-XX:MaxMetaspaceSize=64m","-XX:+ExitOnOutOfMemoryError","-cp",classpath,LuaWorker.class.getName()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
                worker.process=launched;
                if(worker.closed){launched.destroyForcibly();throw new IOException("Computer stopped");}
                worker.input=new DataInputStream(launched.getInputStream());worker.output=new DataOutputStream(launched.getOutputStream());
                worker.output.writeUTF(source);worker.output.flush();
                return new Reply(0,"");
            }catch(Exception e){worker.close();throw new CompletionException(e);}
        },IO); } catch(RejectedExecutionException rejected){worker.close();throw new IllegalArgumentException("Lua workers are still shutting down; retry shortly.");}
        return worker;
    }
    private static String location(Class<?> type)throws Exception {
        var url=type.getProtectionDomain().getCodeSource().getLocation();
        if(!url.getProtocol().equals("file"))throw new IOException("Lua needs a local runtime classpath");
        return Path.of(url.toURI()).toString();
    }
    public Reply poll() {
        if(failure!=null)throw new IllegalArgumentException(failure);
        if(pending==null)return null;
        if(!pending.isDone()) {
            if(System.nanoTime()-deadline>=0){close();throw new IllegalArgumentException("Lua worker exceeded its response deadline; stopped without further world actions.");}
            return null;
        }
        var finished=pending;pending=null;
        try{return finished.join();}catch(CompletionException e){close();throw new IllegalArgumentException("Lua worker stopped: "+e.getCause().getClass().getSimpleName());}
    }
    public void resume(boolean success,String text) {
        if(closed||pending!=null)throw new IllegalStateException("Lua worker is not ready");
        if(text.length()>4096)throw new IllegalArgumentException("Lua reply exceeds 4096 characters");
        deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        try { pending=CompletableFuture.supplyAsync(()->{
            try{output.writeBoolean(success);output.writeUTF(text);output.flush();int status=input.readUnsignedByte();String value=input.readUTF();if(status>3||value.length()>4096)throw new IOException("Invalid Lua response");return new Reply(status,value);}
            catch(IOException e){close();throw new CompletionException(e);}
        },IO); } catch(RejectedExecutionException rejected){close();throw new IllegalArgumentException("Lua I/O queue is full; computer stopped.");}
    }
    @Override public void close(){closed=true;var child=process;if(child!=null)child.destroyForcibly();ACTIVE.remove(this);}
    public static void closeAll(){for(var worker:List.copyOf(ACTIVE))worker.close();}
}
