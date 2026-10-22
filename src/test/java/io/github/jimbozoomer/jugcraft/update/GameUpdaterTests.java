package io.github.jimbozoomer.jugcraft.update;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.nio.channels.FileChannel;
import java.time.Duration;

/** Offline real-file transactions in disposable folders; never touches a player profile. */
public final class GameUpdaterTests {
    private static int assertions;
    private static void check(boolean condition,String text){assertions++;if(!condition)throw new AssertionError(text);}
    private interface Action {void run() throws Exception;}
    private static void rejects(Action action)throws Exception{boolean failed=false;try{action.run();}catch(Exception e){failed=true;}check(failed,"Expected rejection");}
    private static void put(Path path,String text)throws Exception{Files.createDirectories(path.getParent());Files.writeString(path,text);}
    private static String hash(String text)throws Exception{return UpdateApplier.hash(text.getBytes(StandardCharsets.UTF_8));}
    private static Path fixture(Path parent)throws Exception{
        Path root=Files.createTempDirectory(parent,"instance with spaces-");Path w=root.resolve(UpdateApplier.WORK);
        put(root.resolve(UpdateApplier.BASELINE),"old inventory");put(root.resolve("mods/jugcraft.jar"),"old mod");put(root.resolve("mods/retired.jar"),"retired");
        put(root.resolve("options.txt"),"player keybinds");put(root.resolve("config/personal.json"),"settings");put(root.resolve("saves/home/level.dat"),"world");put(root.resolve("mods/personal.jar"),"own mod");
        put(w.resolve("stage/mods/jugcraft.jar"),"new mod");put(w.resolve("stage/mods/added.jar"),"new library");put(w.resolve("next.json"),"new inventory");
        Properties p=new Properties();p.setProperty("schema","1");p.setProperty("count","3");p.setProperty("baseline.old",hash("old inventory"));p.setProperty("baseline.new",hash("new inventory"));
        String[][] rows={{"jugcraft.jar",hash("old mod"),hash("new mod")},{"retired.jar",hash("retired"),"missing"},{"added.jar","missing",hash("new library")}};
        for(int i=0;i<rows.length;i++){p.setProperty(i+".path","mods/"+rows[i][0]);p.setProperty(i+".old",rows[i][1]);p.setProperty(i+".new",rows[i][2]);}
        UpdateApplier.write(w.resolve("plan.properties"),p);return root;
    }
    private static void preserved(Path r)throws Exception{
        check(Files.readString(r.resolve("options.txt")).equals("player keybinds"),"Keybinds preserved");
        check(Files.readString(r.resolve("config/personal.json")).equals("settings"),"Config preserved");
        check(Files.readString(r.resolve("saves/home/level.dat")).equals("world"),"World preserved");
        check(Files.readString(r.resolve("mods/personal.jar")).equals("own mod"),"Unmanaged mods preserved");
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0&&args[0].equals("hold")){for(int i=0;i<1000&&!Files.exists(Path.of(args[1]));i++)Thread.sleep(50);return;}
        Path temp=Files.createTempDirectory("jugcraft-updater-test-");
        try{
            Path a=fixture(temp);UpdateApplier.apply(a,i->{});
            check(Files.readString(a.resolve("mods/jugcraft.jar")).equals("new mod"),"New mod installed");
            check(!Files.exists(a.resolve("mods/retired.jar")),"Obsolete managed mod removed");
            check(Files.readString(a.resolve("mods/added.jar")).equals("new library"),"New library installed");
            check(Files.readString(a.resolve(UpdateApplier.BASELINE)).equals("new inventory"),"Inventory committed");preserved(a);
            Path committed=fixture(temp);put(committed.resolve(UpdateApplier.WORK+"/result.txt/fixture"),"block optional status write");
            UpdateApplier.apply(committed,i->{});
            check(Files.readString(committed.resolve(UpdateApplier.BASELINE)).equals("new inventory"),"Optional status-write failure cannot undo/report a committed update as rollback");
            Path b=fixture(temp);rejects(()->UpdateApplier.apply(b,i->{if(i==1)throw new IllegalStateException("injected disk failure");}));
            check(Files.readString(b.resolve("mods/jugcraft.jar")).equals("old mod"),"Rollback restores old jar");
            check(Files.exists(b.resolve("mods/retired.jar")),"Rollback restores removed jar");
            check(!Files.exists(b.resolve("mods/added.jar")),"Rollback does not invent new jar");preserved(b);
            Path c=fixture(temp);
            try{UpdateApplier.apply(c,i->{if(i==2)throw new AssertionError("simulated process death");});}catch(AssertionError expected){}
            check(Files.exists(c.resolve(UpdateApplier.WORK+"/journal.properties")),"Crash leaves recovery journal");
            UpdateApplier.recover(c);check(Files.readString(c.resolve("mods/jugcraft.jar")).equals("old mod"),"Crash recovery restores original");
            check(!Files.exists(c.resolve("mods/added.jar")),"Crash recovery removes newly added jar");preserved(c);
            Path d=fixture(temp);put(d.resolve(UpdateApplier.WORK+"/stage/mods/jugcraft.jar"),"corrupt");rejects(()->UpdateApplier.apply(d,i->{}));
            check(Files.readString(d.resolve("mods/jugcraft.jar")).equals("old mod"),"Bad payload changes nothing");
            Path e=fixture(temp);put(e.resolve("mods/jugcraft.jar"),"player edited");rejects(()->UpdateApplier.apply(e,i->{}));
            check(Files.readString(e.resolve("mods/jugcraft.jar")).equals("player edited"),"Conflict preserved");
            for(String name:List.of("../evil.jar","mods/../../evil.jar","mods/evil.jar:stream","mods/CON.jar","options.txt"))rejects(()->UpdateApplier.child(temp,name));
            rejects(()->UpdateManifest.approvedUrl("https://example.com/evil.jar","v1"));
            rejects(()->UpdateManifest.approvedUrl("https://github.com/other/repo/releases/download/v1/mod.jar","v1"));
            check(UpdateManifest.compare("0.1.0-alpha.20261021.10","0.1.0-alpha.20261021.9")>0,"Natural release ordering");
            check(UpdateManifest.compare("0.1.0-alpha.20261021.2","0.1.0-alpha.20261021.3")<0,"No downgrade");
            String json="{\"schemaVersion\":1,\"updaterVersion\":1,\"releaseVersion\":\"test.1\",\"minecraft\":\"26.3\",\"fabricLoader\":\"0.19.5\",\"java\":25,\"files\":[{\"path\":\"mods/jugcraft.jar\",\"sha512\":\""+hash("jar")+"\",\"size\":3,\"url\":\""+UpdateManifest.RELEASES+"pack-test.1/jugcraft-client.jar\",\"policy\":\"managed\"}]}";
            var manifest=UpdateManifest.parse(json.getBytes(StandardCharsets.UTF_8));check(manifest.files().size()==1,"Valid manifest accepted");
            for(String bad:List.of(json.replace("mods/jugcraft.jar","options.txt"),json.replace("\"size\":3","\"size\":-3"),json.replace("\"updaterVersion\":1","\"updaterVersion\":99"),json.replace("jimbozoomer-byte/jugcraft","other/repo"))) rejects(()->UpdateManifest.parse(bad.getBytes(StandardCharsets.UTF_8)));
            rejects(()->manifest.compatible(new UpdateManifest("test.2","26.4",manifest.loader(),25,manifest.files())));
            Path f=fixture(temp);String java=Path.of(System.getProperty("java.home"),"bin","java").toString();String cp=System.getProperty("java.class.path");
            Process parent=new ProcessBuilder(java,"-cp",cp,GameUpdaterTests.class.getName(),"hold",f.resolve("release-parent").toString()).start();
            check(args.length==1&&Files.isRegularFile(Path.of(args[0])),"Test the packaged standalone helper");
            Process helper=new ProcessBuilder(java,"-jar",args[0],f.toString(),Long.toString(parent.pid()),"apply").redirectErrorStream(true).redirectOutput(f.resolve("helper-test.log").toFile()).start();
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            while(!Files.exists(f.resolve(UpdateApplier.WORK+"/ready.txt"))&&helper.isAlive()&&System.nanoTime()<deadline)Thread.sleep(20);
            check(parent.isAlive(),"Fake game still alive at handshake");
            check(Files.readString(f.resolve("mods/jugcraft.jar")).equals("old mod"),"Helper never writes while game is alive");
            put(f.resolve("release-parent"),"exit now");
            check(helper.waitFor(20,TimeUnit.SECONDS)&&helper.exitValue()==0,"Helper commits after game exits");
            check(Files.readString(f.resolve("mods/jugcraft.jar")).equals("new mod"),"Real subprocess installed update");preserved(f);
            // Parent has exited, but another process (this harness) retains the OS lock.
            // This deterministically models delayed Windows cleanup without relying on a timing fluke.
            Path delayed=fixture(temp);var delayedWork=delayed.resolve(UpdateApplier.WORK);
            Process delayedHelper;
            try(var held=FileChannel.open(delayedWork.resolve("game.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var heldLock=held.lock()) {
                delayedHelper=new ProcessBuilder(java,"-jar",args[0],delayed.toString(),Long.toString(parent.pid()),"apply").redirectErrorStream(true).redirectOutput(delayed.resolve("helper-test.log").toFile()).start();
                waitReady(delayed,delayedHelper);
                Thread.sleep(400);
                check(delayedHelper.isAlive(),"Delayed lock release is retried after parent exit");
                check(Files.readString(delayed.resolve("mods/jugcraft.jar")).equals("old mod"),"No managed writes before game lock acquisition");
                rejects(()->UpdateApplier.startupLease(delayed));
                try(var ch=FileChannel.open(delayedWork.resolve("helper.lock"),StandardOpenOption.WRITE);var l=ch.tryLock()) {check(l==null,"Helper lock remains held throughout wait");}
            }
            check(delayedHelper.waitFor(20,TimeUnit.SECONDS)&&delayedHelper.exitValue()==0,"Delayed lock eventually permits update");
            check(Files.readString(delayed.resolve("mods/jugcraft.jar")).equals("new mod"),"Delayed handoff installs verified payload");
            String handoff=Files.readString(delayed.resolve("helper-test.log"));
            check(handoff.contains("parent pid=")&&handoff.contains("Game lock acquired after")&&handoff.contains("Completed apply"),"Handoff records PIDs, elapsed wait and success");preserved(delayed);

            Path busy=fixture(temp);var busyWork=busy.resolve(UpdateApplier.WORK);
            try(var held=FileChannel.open(busyWork.resolve("game.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var heldLock=held.lock()) {
                Process blocked=new ProcessBuilder(java,"-jar",args[0],busy.toString(),Long.toString(parent.pid()),"apply").redirectErrorStream(true).redirectOutput(busy.resolve("helper-test.log").toFile()).start();
                waitReady(busy,blocked);
                check(blocked.waitFor(45,TimeUnit.SECONDS)&&blocked.exitValue()!=0,"Persistent conflicting process times out");
                check(Files.readString(busy.resolve("mods/jugcraft.jar")).equals("old mod")&&Files.exists(busy.resolve("mods/retired.jar"))&&!Files.exists(busy.resolve("mods/added.jar")),"Timeout leaves all managed files unchanged");
                check(Files.readString(busy.resolve(UpdateApplier.BASELINE)).equals("old inventory")&&!Files.exists(busyWork.resolve("journal.properties")),"Timeout never begins transaction");
                check(Files.readString(busyWork.resolve("result.txt")).contains("Timed out waiting"),"Timeout accurately reports lock wait failure");preserved(busy);
            }
            Path launch=fixture(temp);var launchWork=launch.resolve(UpdateApplier.WORK);
            check(UpdateApplier.startupLease(launch)==null,"Pending transaction requests recovery before taking game lock");
            try(var ch=FileChannel.open(launchWork.resolve("game.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var l=ch.tryLock()) {check(l!=null,"Rejected recovery launch does not retain game lock");}
            Files.delete(launchWork.resolve("plan.properties"));
            try(var lease=UpdateApplier.startupLease(launch)) {check(lease!=null,"Clean launch retains a game lease");rejects(()->UpdateApplier.startupLease(launch));}
            try(var ch=FileChannel.open(launchWork.resolve("game.lock"),StandardOpenOption.WRITE)) {
                ch.close();long start=System.nanoTime();rejects(()->UpdateApplier.awaitGameLock(ch,Duration.ofSeconds(30)));
                check(System.nanoTime()-start<TimeUnit.SECONDS.toNanos(1),"IO failure is not treated as lock contention");
            }
            System.out.println("PASS: "+assertions+" offline in-game updater assertions");
        }finally{try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
    private static void waitReady(Path root,Process helper)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(!Files.exists(root.resolve(UpdateApplier.WORK+"/ready.txt"))&&helper.isAlive()&&System.nanoTime()<deadline)Thread.sleep(20);
        check(helper.isAlive()&&Files.exists(root.resolve(UpdateApplier.WORK+"/ready.txt")),"Helper acquired update lock and signaled readiness");
    }
}
