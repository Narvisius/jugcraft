package io.github.jimbozoomer.jugcraft.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Deterministic startup-clock checks; no actual JVM launch or wall-clock sleeping. */
public class PeerClientStartupGameTests {
    private static final long BUDGET = Duration.ofMinutes(5).toNanos();

    @GameTest(maxTicks = 20)
    public void coldResourceLoadingWaitsForTheActualPeerEntrypoint(GameTestHelper helper) throws Exception {
        try (Fixture fixture = new Fixture()) {
            Files.writeString(fixture.path.resolve("peer-initialized"), "");
            PeerClientStartup.await(() -> {
                fixture.ticks++;
                fixture.now += Duration.ofSeconds(50).toNanos();
                if (fixture.ticks == 3) fixture.enter();
            }, fixture.process, fixture.path, () -> fixture.now, BUDGET);
            helper.assertTrue(fixture.ticks == 3, "Initialization alone is not readiness; 150 seconds of cold loading can finish");
            PeerClientStartup.await(() -> { throw new AssertionError("Ready peer must not wait again"); },
                    fixture.process, fixture.path, () -> fixture.now, BUDGET);
            helper.succeed();
        }
    }

    @GameTest(maxTicks = 20)
    public void startupDeadlineIsBoundedAndIncludesPeerEvidence(GameTestHelper helper) throws Exception {
        try (Fixture fixture = new Fixture()) {
            Files.writeString(fixture.path.resolve("peer-initialized"), "");
            Files.writeString(fixture.path.resolve("peer.log"), "Still loading the independent client atlas");
            AssertionError failure = null;
            try {
                PeerClientStartup.await(() -> {
                    fixture.ticks++;
                    fixture.now += Duration.ofMinutes(1).toNanos();
                }, fixture.process, fixture.path, () -> fixture.now, BUDGET);
            } catch (AssertionError e) { failure = e; }
            helper.assertTrue(failure != null, "A living peer that never reaches its test entrypoint times out");
            helper.assertTrue(fixture.ticks == 5, "Startup stops at its five-minute deadline");
            helper.assertTrue(failure.getMessage().contains("initialized=true")
                    && failure.getMessage().contains("Still loading the independent client atlas"),
                    "The failure identifies the startup phase and includes the peer log");
            helper.succeed();
        }
    }

    @GameTest(maxTicks = 20)
    public void peerExitFailsImmediatelyWithItsLog(GameTestHelper helper) throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.process.alive = false;
            Files.writeString(fixture.path.resolve("peer.log"), "Peer failed to initialize");
            AssertionError failure = null;
            try {
                PeerClientStartup.await(() -> { throw new AssertionError("Exited peer must not wait"); },
                        fixture.process, fixture.path, () -> fixture.now, BUDGET);
            } catch (AssertionError e) { failure = e; }
            helper.assertTrue(failure != null && failure.getMessage().contains("exited during startup: 42")
                    && failure.getMessage().contains("Peer failed to initialize"),
                    "An exited process reports its exit code and log without spending the startup budget");
            helper.succeed();
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Path path = Files.createTempDirectory("jugcraft-peer-startup-test-");
        final StubProcess process = new StubProcess();
        long now;
        int ticks;
        Fixture() throws Exception {}
        void enter() {
            try { Files.writeString(path.resolve("peer-entered"), ""); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        }
        @Override public void close() throws Exception {
            Files.deleteIfExists(path.resolve("peer-entered"));
            Files.deleteIfExists(path.resolve("peer-initialized"));
            Files.deleteIfExists(path.resolve("peer.log"));
            Files.delete(path);
        }
    }

    private static final class StubProcess extends Process {
        boolean alive = true;
        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public InputStream getInputStream() { return InputStream.nullInputStream(); }
        @Override public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        @Override public int waitFor() { throw new UnsupportedOperationException(); }
        @Override public int exitValue() { if (alive) throw new IllegalThreadStateException(); return 42; }
        @Override public void destroy() { alive = false; }
        @Override public boolean isAlive() { return alive; }
    }
}
