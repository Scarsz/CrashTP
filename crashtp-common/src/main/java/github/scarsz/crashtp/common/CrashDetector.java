package github.scarsz.crashtp.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class CrashDetector {

    private final Map<UUID, Long> joinTimes = new HashMap<>();
    private final Map<UUID, AtomicInteger> badJoins = new HashMap<>();
    private final BiConsumer<Level, String> logConsumer;
    private final Function<UUID, String> uuidTranslator;
    private final Consumer<UUID> unstuckConsumer;

    public CrashDetector(BiConsumer<Level, String> logConsumer, Function<UUID, String> uuidTranslator, Consumer<UUID> unstuck) {
        this.logConsumer = logConsumer;
        this.uuidTranslator = uuidTranslator;
        this.unstuckConsumer = unstuck;
    }

    public long noticePlayerJoin(UUID uuid) {
        long timestamp = System.currentTimeMillis();
        joinTimes.put(uuid, timestamp);
        return timestamp;
    }

    public void noticePlayerQuit(UUID uuid) {
        Long joinTime = joinTimes.get(uuid);
        if (joinTime == null) {
            logConsumer.accept(Level.WARNING, "Player " + uuidTranslator.apply(uuid) + " has no join time???");
            return;
        }

        // make sure the play time was <= 5 seconds
        if (System.currentTimeMillis() - joinTime <= TimeUnit.SECONDS.toMillis(5)) {
            int badJoins = this.badJoins.computeIfAbsent(uuid, v -> new AtomicInteger()).incrementAndGet();

            if (badJoins >= 2) {
                logConsumer.accept(Level.INFO, "Attempting to unstuck player " + uuidTranslator.apply(uuid));
                unstuckConsumer.accept(uuid);
            }
        }
    }

    public void noticePlayerOkay(UUID uuid, long originalJoinTime) {
        if (joinTimes.containsKey(uuid) && joinTimes.get(uuid) == originalJoinTime) {
            badJoins.remove(uuid);
        }
    }

}
