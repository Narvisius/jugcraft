package io.github.jimbozoomer.jugcraft.update;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/** Java-only, offline commit/recovery helper. Runs from an extracted copy AFTER Minecraft exits. */
public final class UpdateApplier {
    public static final String BASELINE = "JUGCRAFT-UPDATE.json";
    public static final String WORK = ".jugcraft-game-update";
    public static void require(boolean value, String message) throws IOException { if (!value) throw new IOException(message); }
    public static void safe(Path path) throws IOException {
        for (Path p=path.toAbsolutePath().normalize(); p!=null; p=p.getParent()) {
            require(!Files.isSymbolicLink(p), "Linked paths are not supported: " + p);
            if (Files.exists(p, LinkOption.NOFOLLOW_LINKS))
                require(!Files.readAttributes(p, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther(), "Special/reparse paths are not supported: " + p);
        }
    }
    public static void validateName(String name) throws IOException {
        require(name!=null && name.matches("mods/[A-Za-z0-9][A-Za-z0-9._+\\-]*\\.jar"), "Invalid managed mod path");
        require(!name.substring(5).matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])\\..*"), "Reserved mod name");
    }
    public static Path child(Path root, String name) throws IOException {
        validateName(name);
        Path result=root.toAbsolutePath().normalize().resolve(name).normalize();
        require(result.startsWith(root.toAbsolutePath().normalize()), "Path escaped instance"); safe(result); return result;
    }
    public static String hash(Path path) throws IOException {
        safe(path);
        try (var in=Files.newInputStream(path)) {
            MessageDigest digest=MessageDigest.getInstance("SHA-512"); byte[] buffer=new byte[65536]; int n;
            while ((n=in.read(buffer))>=0) digest.update(buffer,0,n);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }
    public static String hash(byte[] bytes) throws IOException {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }
    public static boolean matches(Path path, String digest) throws IOException { return Files.isRegularFile(path) && hash(path).equals(digest); }
    public static void atomic(Path path, byte[] bytes) throws IOException {
        safe(path); Files.createDirectories(path.getParent()); Path temp=path.resolveSibling(path.getFileName()+".new"); safe(temp);
        try (FileOutputStream out=new FileOutputStream(temp.toFile())) { out.write(bytes); out.getFD().sync(); }
        Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    public static void atomicCopy(Path source,Path target)throws IOException {
        safe(source);safe(target);Files.createDirectories(target.getParent());Path temp=target.resolveSibling(target.getFileName()+".new");safe(temp);
        try(var in=Files.newInputStream(source);var out=new FileOutputStream(temp.toFile())){in.transferTo(out);out.getFD().sync();}
        Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    public static Properties read(Path path) throws IOException {
        safe(path); require(Files.size(path)<1024*1024,"Oversized update plan");
        var p=new Properties(); try(var in=Files.newInputStream(path)) {p.load(in);} return p;
    }
    public static void write(Path path, Properties p) throws IOException { var out=new ByteArrayOutputStream(); p.store(out,"JUGCRAFT local transaction"); atomic(path,out.toByteArray()); }
    private static int validate(Path root, Properties p) throws IOException {
        require("1".equals(p.getProperty("schema")),"Unsupported update plan");
        int count=Integer.parseInt(p.getProperty("count")); require(count>=0&&count<=200,"Invalid update count");
        Set<String> paths=new HashSet<>();
        for(int i=0;i<count;i++) {
            String key=i+".", name=p.getProperty(key+"path"); child(root,name);
            require(paths.add(name.toLowerCase(Locale.ROOT)),"Duplicate update path");
            for(String kind:List.of("old","new")) {
                String h=p.getProperty(key+kind); require(h!=null && (h.equals("missing") || h.matches("[0-9a-f]{128}")),"Invalid file hash");
            }
        }
        for(String key:List.of("baseline.old","baseline.new")) require(p.getProperty(key,"").matches("[0-9a-f]{128}"),"Invalid baseline hash");
        return count;
    }
    public static void recover(Path root) throws IOException {
        Path work=root.resolve(WORK), journal=work.resolve("journal.properties"); safe(work);
        if(!Files.exists(journal)) return;
        Properties p=read(journal); int count=validate(root,p);
        require(matches(work.resolve("baseline.backup"),p.getProperty("baseline.old")),"Recovery metadata backup is missing/corrupt");
        // Verify every backup before restoring any files.
        for(int i=0;i<count;i++) if(!p.getProperty(i+".old").equals("missing"))
            require(matches(child(work.resolve("backup"),p.getProperty(i+".path")),p.getProperty(i+".old")),"Recovery mod backup missing/corrupt");
        for(int i=0;i<count;i++) {
            Path target=child(root,p.getProperty(i+".path"));
            if(p.getProperty(i+".old").equals("missing")) Files.deleteIfExists(target);
            else atomicCopy(child(work.resolve("backup"),p.getProperty(i+".path")),target);
        }
        atomic(root.resolve(BASELINE),Files.readAllBytes(work.resolve("baseline.backup")));
        Files.delete(journal);
    }
    public static void apply(Path root, java.util.function.IntConsumer fault) throws IOException {
        Path work=root.resolve(WORK); safe(work); recover(root);
        Properties p=read(work.resolve("plan.properties")); int count=validate(root,p);
        require(matches(root.resolve(BASELINE),p.getProperty("baseline.old")),"Instance inventory changed during download");
        require(matches(work.resolve("next.json"),p.getProperty("baseline.new")),"Next inventory failed verification");
        for(int i=0;i<count;i++) {
            String name=p.getProperty(i+".path"), old=p.getProperty(i+".old"), next=p.getProperty(i+".new");
            Path target=child(root,name);
            require(old.equals("missing")?!Files.exists(target):matches(target,old),"A managed mod changed: "+name);
            if(!next.equals("missing")) require(matches(child(work.resolve("stage"),name),next),"Staged mod failed verification: "+name);
        }
        atomic(work.resolve("baseline.backup"),Files.readAllBytes(root.resolve(BASELINE)));
        for(int i=0;i<count;i++) if(!p.getProperty(i+".old").equals("missing"))
            atomicCopy(child(root,p.getProperty(i+".path")),child(work.resolve("backup"),p.getProperty(i+".path")));
        write(work.resolve("journal.properties"),p);
        try {
            for(int i=0;i<count;i++) {
                Path target=child(root,p.getProperty(i+".path"));
                if(p.getProperty(i+".new").equals("missing")) Files.delete(target);
                else {Files.createDirectories(target.getParent()); Files.move(child(work.resolve("stage"),p.getProperty(i+".path")),target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                fault.accept(i);
            }
            atomic(root.resolve(BASELINE),Files.readAllBytes(work.resolve("next.json")));
            Files.delete(work.resolve("journal.properties"));
        } catch(Exception failure) {recover(root); throw new IOException("Update failed; previous files restored",failure);}
        // Retain only the previous transaction's changed files, not an ever-growing
        // collection of versioned libraries. Cleanup failure never rolls back a commit.
        try {
            Files.deleteIfExists(work.resolve("plan.properties"));
            atomic(work.resolve("result.txt"),"Update installed. Start JUGCRAFT normally.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Set<String> keep=new HashSet<>();for(int i=0;i<count;i++)if(!p.getProperty(i+".old").equals("missing"))keep.add(p.getProperty(i+".path"));
            Path backups=work.resolve("backup/mods");safe(backups);
            if(Files.isDirectory(backups))try(var files=Files.list(backups)){
                for(Path file:files.toList()){String name="mods/"+file.getFileName();validateName(name);safe(file);if(!keep.contains(name))Files.delete(file);}
            }
            safe(work.resolve("downloaded.properties"));Files.deleteIfExists(work.resolve("downloaded.properties"));
        }catch(IOException ignored){} // Harmless extra backup; retry cleanup next update.
    }
    static void log(String message) { System.out.println(java.time.Instant.now()+" [updater pid="+ProcessHandle.current().pid()+"] "+message); }

    /** Only a null tryLock result is contention. IO errors and interruption must escape. */
    static FileLock awaitGameLock(FileChannel channel, Duration timeout) throws IOException, InterruptedException {
        long started=System.nanoTime(),budget=timeout.toNanos();
        require(budget>0,"Invalid game-lock wait deadline");
        for (;;) {
            FileLock lock=channel.tryLock();
            long elapsed=System.nanoTime()-started;
            if(lock!=null) {log("Game lock acquired after "+Duration.ofNanos(elapsed).toMillis()+" ms");return lock;}
            if(elapsed>=budget) {
                log("Game lock timed out after "+Duration.ofNanos(elapsed).toMillis()+" ms");
                throw new IOException("Timed out waiting for this instance's game lock after Minecraft exited ("+timeout.toSeconds()+" seconds). Another game may still be running or Windows may still be releasing its files; no managed files changed");
            }
            Thread.sleep(Math.max(1,Math.min(100,Duration.ofNanos(budget-elapsed).toMillis())));
        }
    }

    public record StartupLease(FileChannel channel, FileLock lock) implements AutoCloseable {
        @Override public void close() throws IOException {try {lock.release();}finally {channel.close();}}
    }

    /** Startup and helper both take helper.lock before game.lock. Null means recovery is needed. */
    public static StartupLease startupLease(Path root) throws IOException {
        Path work=root.resolve(WORK);safe(work);Files.createDirectories(work);
        safe(work.resolve("helper.lock"));safe(work.resolve("game.lock"));
        try(var helperChannel=FileChannel.open(work.resolve("helper.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var helperLock=helperChannel.tryLock()) {
            require(helperLock!=null,"JUGCRAFT is finishing an update; wait before pressing Play again");
            if(Files.exists(work.resolve("plan.properties"))||Files.exists(work.resolve("journal.properties")))return null;
            var channel=FileChannel.open(work.resolve("game.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            try {
                var lock=channel.tryLock();require(lock!=null,"JUGCRAFT is already running in this instance");
                return new StartupLease(channel,lock);
            } catch(IOException|RuntimeException e) {channel.close();throw e;}
        }
    }

    public static void main(String[] args) throws Exception {
        require(args.length==3 && (args[2].equals("apply")||args[2].equals("recover")),"Invalid helper arguments");
        Path root=Path.of(args[0]).toAbsolutePath().normalize(),work=root.resolve(WORK); safe(root); safe(work);
        safe(work.resolve("helper.lock"));
        try(var channel=FileChannel.open(work.resolve("helper.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE); var lock=channel.tryLock()) {
            require(lock!=null,"Another updater is already active");
            try {
                long parent=Long.parseLong(args[1]),started=System.nanoTime();
                log("Starting "+args[2]+"; parent pid="+parent+"; helper lock acquired");
                atomic(work.resolve("ready.txt"),Long.toString(ProcessHandle.current().pid()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                while(ProcessHandle.of(parent).map(ProcessHandle::isAlive).orElse(false)) {
                    require(System.nanoTime()-started<Duration.ofMinutes(15).toNanos(),"Minecraft did not exit; no files changed"); Thread.sleep(200);
                }
                log("Parent pid="+parent+" exited; waited "+Duration.ofNanos(System.nanoTime()-started).toMillis()+" ms; waiting up to 30000 ms for game lock");
                safe(work.resolve("game.lock"));
                try(var gameChannel=FileChannel.open(work.resolve("game.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var gameLock=awaitGameLock(gameChannel,Duration.ofSeconds(30))) {
                    if(args[2].equals("recover")) {recover(root); Files.deleteIfExists(work.resolve("plan.properties"));}
                    else apply(root,i->{});
                    log("Completed "+args[2]);
                }
            } catch(Exception e) {
                log("Failed: "+e.getClass().getSimpleName()+": "+e.getMessage());
                atomic(work.resolve("result.txt"),("Update failed: "+e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                // A rolled-back install may start again and offer a retry. An incomplete
                // rollback retains the plan, and the startup guard blocks that install.
                if(!Files.exists(work.resolve("journal.properties"))) Files.deleteIfExists(work.resolve("plan.properties"));
                throw e;
            } finally {Files.deleteIfExists(work.resolve("ready.txt"));}
        }
    }
}
