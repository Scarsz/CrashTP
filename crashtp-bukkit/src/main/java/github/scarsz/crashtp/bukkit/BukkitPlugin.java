package github.scarsz.crashtp.bukkit;

import github.scarsz.crashtp.common.CrashDetector;
import github.scarsz.crashtp.common.util.NBTUtil;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class BukkitPlugin extends JavaPlugin implements Listener {

    private CrashDetector crashDetector;

    @Override
    public void onEnable() {
        this.crashDetector = new CrashDetector(
                (level, string) -> getLogger().log(level, string),
                uuid -> Bukkit.getOfflinePlayer(uuid).getName(),
                uuid -> {
                    try {
                        this.unstuck(uuid);
                    } catch (IOException e) {
                        getLogger().severe(String.format("Failed to unstuck player %s: %s",
                                Bukkit.getOfflinePlayer(uuid).getName(),
                                e.getMessage()
                        ));
                    }
                }
        );

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.crashDetector = null;
    }

    private void unstuck(UUID uuid) throws IOException {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            // shouldn't happen but just in case
            offlinePlayer.getPlayer().kickPlayer(ChatColor.RED + "Position resetting...");
        }

        World world = Bukkit.getWorlds().get(0);
        File playerDat = new File(world.getWorldFolder(), "playerdata/" + uuid + ".dat");
        Location spawnLocation = world.getSpawnLocation();
        //noinspection deprecation
        NBTUtil.setPlayerCoordinates(
                playerDat,
                world.getUID(),
                world.getEnvironment().getId(),
                spawnLocation.getX(),
                spawnLocation.getY(),
                spawnLocation.getZ(),
                spawnLocation.getYaw(),
                spawnLocation.getPitch()
        );
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        long joinTime = this.crashDetector.noticePlayerJoin(event.getPlayer().getUniqueId());

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> this.crashDetector.noticePlayerOkay(event.getPlayer().getUniqueId(), joinTime),
                20 * 60 // 1 minute
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        long logoutTime = this.crashDetector.noticePlayerQuit(event.getPlayer().getUniqueId());

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> this.crashDetector.noticePlayerLeft(event.getPlayer().getUniqueId(), logoutTime),
                20 * 60 // 1 minute
        );
    }

}
