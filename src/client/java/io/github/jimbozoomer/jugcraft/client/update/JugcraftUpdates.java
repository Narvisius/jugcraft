package io.github.jimbozoomer.jugcraft.client.update;

import io.github.jimbozoomer.jugcraft.update.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Opt-in pack updater: no network for standalone JAR installs, development or servers. */
public final class JugcraftUpdates {
    public enum State { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, READY, APPLYING, ERROR }
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Jugcraft updates");t.setDaemon(true);return t;});
    public static volatile State state=State.IDLE;
    public static volatile String message="Check for a new JUGCRAFT release.",current="",latest="";
    public static volatile float progress;
    private static Path root;
    private static UpdateManifest installed,next;
    private static byte[] baseline,nextBytes;
    private static boolean checked,offered;
    private static volatile boolean showOffer;
    private JugcraftUpdates() {}
    public static void register() {
        var loader=FabricLoader.getInstance(); root=loader.getGameDir().toAbsolutePath().normalize();
        if(loader.isDevelopmentEnvironment()||!Files.isRegularFile(root.resolve(UpdateApplier.BASELINE))) return;
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{
            if(screen instanceof TitleScreen) Screens.getWidgets(screen).add(Button.builder(Component.literal("JUGCRAFT Updates"),b->{offered=true;showOffer=false;client.gui.setScreen(new UpdateScreen(screen));})
                .bounds(6,6,126,20).build());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.level!=null||!(client.gui.screen() instanceof TitleScreen)) return;
            if(!checked) {checked=true;check();}
            if(showOffer&&!offered) {offered=true;showOffer=false;client.gui.setScreen(new UpdateScreen(client.gui.screen()));}
        });
    }
    private static void fail(Exception e) {
        Jugcraft.LOGGER.warn("JUGCRAFT update: {}",e.toString());
        message=Objects.requireNonNullElse(e.getMessage(),"Update could not be completed."); state=State.ERROR;
    }
    public static boolean busy() {return state==State.CHECKING||state==State.DOWNLOADING||state==State.APPLYING;}
    public static synchronized void check() {
        if(busy()||state==State.READY)return;
        state=State.CHECKING;message="Checking JUGCRAFT's release channel...";
        WORKER.submit(()->{
            try {
                UpdateApplier.safe(root.resolve(UpdateApplier.BASELINE));
                UpdateApplier.require(Files.size(root.resolve(UpdateApplier.BASELINE))<=1048576,"Oversized local inventory");
                baseline=Files.readAllBytes(root.resolve(UpdateApplier.BASELINE)); installed=UpdateManifest.parse(baseline); current=installed.version();
                var loader=FabricLoader.getInstance();
                UpdateApplier.require(loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals(installed.minecraft())
                    &&loader.getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString().equals(installed.loader())
                    &&Runtime.version().feature()==installed.javaVersion(),"This instance's platform differs from its pack inventory");
                var channel=UpdateManifest.object(fetch(UpdateManifest.CHANNEL,1048576));
                UpdateApplier.require(channel.get("schemaVersion").getAsInt()==1,"Unsupported update channel");
                latest=UpdateManifest.version(UpdateManifest.string(channel,"releaseVersion"));
                String url=UpdateManifest.string(channel,"manifestUrl");
                UpdateApplier.require(url.equals(UpdateManifest.RELEASES+"pack-"+latest+"/player-update.json"),"Unapproved release manifest");
                nextBytes=fetch(url,1048576);
                UpdateApplier.require(UpdateApplier.hash(nextBytes).equals(UpdateManifest.digest(UpdateManifest.string(channel,"manifestSha512"))),"Release manifest failed verification");
                next=UpdateManifest.parse(nextBytes); UpdateApplier.require(next.version().equals(latest),"Channel version mismatch"); installed.compatible(next);
                if(UpdateManifest.compare(latest,current)<=0) {message=latest.equals(current)?"You're up to date.":"This instance is newer than the public channel.";state=State.CURRENT;return;}
                String previous="";Path result=root.resolve(UpdateApplier.WORK).resolve("result.txt");UpdateApplier.safe(result);
                if(Files.isRegularFile(result)&&Files.size(result)<8192){String text=Files.readString(result);if(text.startsWith("Update failed:"))previous=text+" Retry the download. ";}
                message=previous+"A new version is ready. Download it when you're ready."; state=State.AVAILABLE;showOffer=true;
            } catch(Exception e) {fail(e);}
        });
    }
    private static InputStream connect(String address) throws IOException {
        URI uri=URI.create(address);
        for(int redirects=0;redirects<6;redirects++) {
            UpdateApplier.require("https".equals(uri.getScheme())&&uri.getUserInfo()==null&&uri.getPort()==-1,"Unsafe redirect");
            UpdateApplier.require(Set.of("github.com","release-assets.githubusercontent.com","objects.githubusercontent.com","cdn.modrinth.com","raw.githubusercontent.com").contains(uri.getHost()),"Unapproved download redirect");
            HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection(); c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","JUGCRAFT-game-updater/1");
            int status=c.getResponseCode();
            if(status>=300&&status<400) {String location=c.getHeaderField("Location");c.disconnect();UpdateApplier.require(location!=null,"Missing redirect");uri=uri.resolve(location);continue;}
            if(status!=200){c.disconnect();throw new IOException("Update service returned "+status+". Try again later.");}
            return new FilterInputStream(c.getInputStream()) {public void close() throws IOException {try {super.close();} finally {c.disconnect();}}};
        }
        throw new IOException("Too many download redirects");
    }
    private static byte[] fetch(String url,int limit) throws IOException {
        try(var in=connect(url)) {byte[] result=in.readNBytes(limit+1);UpdateApplier.require(result.length<=limit,"Oversized response");return result;}
    }
    public static synchronized void download() {
        if(state!=State.AVAILABLE)return;
        state=State.DOWNLOADING;progress=0;
        WORKER.submit(()->{
            try {
                Path work=root.resolve(UpdateApplier.WORK);UpdateApplier.safe(work);Files.createDirectories(work);
                UpdateApplier.require(!Files.exists(work.resolve("plan.properties"))&&!Files.exists(work.resolve("journal.properties")),"Another update is pending; restart to recover it");
                Properties plan=new Properties();plan.setProperty("schema","1");plan.setProperty("baseline.old",UpdateApplier.hash(baseline));plan.setProperty("baseline.new",UpdateApplier.hash(nextBytes));
                var names=new LinkedHashSet<>(installed.files().keySet());names.addAll(next.files().keySet());int count=0,done=0;
                Set<String> folded=new HashSet<>();for(String name:names)UpdateApplier.require(folded.add(name.toLowerCase(Locale.ROOT)),"Case-only mod renames require manual review");
                // Validate every owned file, including unchanged ones, before staging an update.
                for(var old:installed.files().values()) UpdateApplier.require(UpdateApplier.matches(UpdateApplier.child(root,old.path()),old.hash()),"Managed mod was removed or changed: "+old.path()+". Restore it before updating.");
                for(String name:names) {
                    var old=installed.files().get(name);var fresh=next.files().get(name);Path target=UpdateApplier.child(root,name);
                    if(old==null) UpdateApplier.require(!Files.exists(target),"An unmanaged mod uses the new filename: "+name);
                    if(old!=null&&fresh!=null&&old.hash().equals(fresh.hash())){progress=(float)++done/names.size();continue;}
                    plan.setProperty(count+".path",name);plan.setProperty(count+".old",old==null?"missing":old.hash());plan.setProperty(count+".new",fresh==null?"missing":fresh.hash());count++;
                    if(fresh!=null) {
                        message="Downloading "+name.substring(5); Path staged=UpdateApplier.child(work.resolve("stage"),name);
                        if(!UpdateApplier.matches(staged,fresh.hash())) {
                            Files.createDirectories(staged.getParent());Path partial=staged.resolveSibling(staged.getFileName()+".part");UpdateApplier.safe(partial);
                            long size=0,deadline=System.nanoTime()+Duration.ofMinutes(15).toNanos();
                            try(var in=connect(fresh.url());var out=Files.newOutputStream(partial)) {
                                byte[] buffer=new byte[65536];int n;
                                while((n=in.read(buffer))>=0) {size+=n;UpdateApplier.require(size<=fresh.size()&&System.nanoTime()<deadline,"Download exceeded its size/time limit");out.write(buffer,0,n);progress=(done+(float)size/fresh.size())/names.size();}
                            }
                            UpdateApplier.require(size==fresh.size()&&UpdateApplier.matches(partial,fresh.hash()),"Downloaded mod failed verification: "+name);
                            Files.move(partial,staged,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
                        }
                    }
                    progress=(float)++done/names.size();
                }
                plan.setProperty("count",Integer.toString(count));UpdateApplier.atomic(work.resolve("next.json"),nextBytes);
                UpdateApplier.write(work.resolve("downloaded.properties"),plan);
                message="Download verified. Apply & quit, wait a moment, then press Play in Modrinth.";state=State.READY;
            }catch(Exception e){fail(e);}
        });
    }
    public static synchronized void applyAndQuit() {
        if(state!=State.READY||Minecraft.getInstance().level!=null)return;
        state=State.APPLYING;message="Preparing to quit safely. Your settings and worlds will stay in place.";
        WORKER.submit(()->{
            Path work=root.resolve(UpdateApplier.WORK);Process helper=null;
            try {
                var plan=UpdateApplier.read(work.resolve("downloaded.properties"));
                UpdateApplier.write(work.resolve("plan.properties"),plan);helper=UpdateBootstrap.start(root,"apply");
                long deadline=System.nanoTime()+Duration.ofSeconds(15).toNanos();
                while(!Files.exists(work.resolve("ready.txt"))) {
                    UpdateApplier.require(helper.isAlive()&&System.nanoTime()<deadline,"Updater could not start; Minecraft has stayed open");Thread.sleep(100);
                }
                UpdateApplier.require(Files.readString(work.resolve("ready.txt")).equals(Long.toString(helper.pid())),"Updater handshake failed");
                Minecraft.getInstance().execute(()->Minecraft.getInstance().stop());
            }catch(Exception e){if(helper!=null)helper.destroy();try{Files.deleteIfExists(work.resolve("plan.properties"));}catch(IOException ignored){}fail(e);}
        });
    }
}
