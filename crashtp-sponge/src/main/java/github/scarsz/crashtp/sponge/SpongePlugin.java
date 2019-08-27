package github.scarsz.crashtp.sponge;

import com.google.inject.Inject;
import github.scarsz.crashtp.common.CrashDetector;
import github.scarsz.crashtp.common.util.NBTUtil;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "crashtp",
        name = "CrashTP",
        description = "Teleport players to the world spawn when they crash after joining the server",
        authors = {
                "Scarsz"
        }
)
public class SpongePlugin {

    @Inject
    private Logger logger;

    private CrashDetector crashDetector;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        this.crashDetector = new CrashDetector(
                (level, string) -> {
                    switch (level.getName()) {
                        case "SEVERE": logger.error(string);
                        case "WARNING": logger.warn(string);
                        case "INFO": default: logger.info(string);
                    }
                },
                uuid -> getUser(uuid).getName(),
                uuid -> {
                    try {
                        this.unstuck(uuid);
                    } catch (IOException e) {
                        logger.error(String.format("Failed to unstuck player %s: %s",
                                getUser(uuid).getName(),
                                e.getMessage()
                        ));
                    }
                }
        );
    }

    @Listener
    public void onGameStoppingServer(GameStoppingServerEvent event) {
        this.crashDetector = null;
    }

    private void unstuck(UUID uuid) throws IOException {
        User user = getUser(uuid);
        if (user.getPlayer().isPresent()) {
            // shouldn't happen but just in case
            user.getPlayer().get().kick(Text.builder("Position resetting...").color(TextColors.RED).build());
        }

        World world = Sponge.getServer().getWorlds().iterator().next();
        File playerDat = new File(world.getDirectory().toFile(), "playerdata/" + uuid + ".dat");
        Location<World> spawnLocation = world.getSpawnLocation();
        NBTUtil.setPlayerCoordinates(
                playerDat,
                world.getUniqueId(),
                getDimensionId(world),
                spawnLocation.getX(),
                spawnLocation.getY(),
                spawnLocation.getZ(),
                0,
                0
        );
    }

    @Listener(order = Order.PRE)
    public void onPlayerConnect(ClientConnectionEvent.Join event) {
        long joinTime = this.crashDetector.noticePlayerJoin(event.getTargetEntity().getUniqueId());

        Sponge.getScheduler().createTaskBuilder()
                .name("CrashTP - Check if " + event.getTargetEntity().getName() + " is okay")
                .delay(1, TimeUnit.MINUTES)
                .execute(() -> this.crashDetector.noticePlayerOkay(event.getTargetEntity().getUniqueId(), joinTime))
                .submit(this);
    }

    @Listener(order = Order.PRE)
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        this.crashDetector.noticePlayerQuit(event.getTargetEntity().getUniqueId());
    }

    public User getUser(UUID uuid) {
        Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);
        return userStorage.orElseThrow(() -> new RuntimeException("No user storage service present"))
                .get(uuid).orElseThrow(() -> new IllegalArgumentException("Player " + uuid + " not found"));
    }

    private int getDimensionId(World world) {
        return world.getName().endsWith("_the_end")
                ? 1
                : world.getName().endsWith("_nether")
                        ? -1
                        : 0;
    }

}
