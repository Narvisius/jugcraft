package io.github.jimbozoomer.jugcraft.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.function.LongSupplier;

/** Independent client startup is bounded separately from the unchanged network/gameplay deadlines. */
final class PeerClientStartup {
    // The expanded entity models exceed 2 GB during baking; use the primary
    // test client's existing heap allowance for the independent client too.
    static final String HEAP_OPTION = "-Xmx4G";
    private static final long STARTUP_NANOS = Duration.ofMinutes(5).toNanos();

    private PeerClientStartup() {}

    static void await(Runnable waitTick, Process peer, Path coordination) {
        await(waitTick, peer, coordination, System::nanoTime, STARTUP_NANOS);
    }

    static void await(Runnable waitTick, Process peer, Path coordination, LongSupplier clock, long budgetNanos) {
        // Initialization/atlas loading is not a login handshake. Only the peer's
        // actual test entrypoint marks readiness, after those startup phases.
        long started = clock.getAsLong();
        while (!Files.exists(coordination.resolve("peer-entered"))) {
            if (!peer.isAlive()) throw failure(coordination, "Second client exited during startup: " + peer.exitValue());
            if (clock.getAsLong() - started >= budgetNanos)
                throw failure(coordination, "Second client did not finish startup within "
                        + Duration.ofNanos(budgetNanos).toSeconds() + " seconds; initialized="
                        + Files.exists(coordination.resolve("peer-initialized")));
            waitTick.run();
        }
        System.out.println("Owner peer finished startup after " + (clock.getAsLong() - started) / 1_000_000_000L
                + " seconds; starting normal login deadline.");
    }

    static AssertionError failure(Path coordination, String message) {
        var tail = new ArrayDeque<String>();
        try (var lines = Files.lines(coordination.resolve("peer.log"))) {
            lines.forEach(line -> { if (tail.size() == 120) tail.removeFirst(); tail.addLast(line); });
        } catch (IOException e) { tail.addLast("Could not read peer log: " + e); }
        return new AssertionError(message + "\nSecond-client log tail:\n" + String.join("\n", tail));
    }
}
