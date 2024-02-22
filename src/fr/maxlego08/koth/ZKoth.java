package fr.maxlego08.koth;

import fr.maxlego08.koth.api.Koth;
import fr.maxlego08.koth.api.KothStatus;
import fr.maxlego08.koth.api.KothTeam;
import fr.maxlego08.koth.api.KothType;
import fr.maxlego08.koth.api.events.KothCapEvent;
import fr.maxlego08.koth.api.events.KothCatchEvent;
import fr.maxlego08.koth.api.events.KothLooseEvent;
import fr.maxlego08.koth.api.events.KothSpawnEvent;
import fr.maxlego08.koth.api.events.KothStartEvent;
import fr.maxlego08.koth.api.events.KothStopEvent;
import fr.maxlego08.koth.api.events.KothWinEvent;
import fr.maxlego08.koth.api.utils.ScoreboardConfiguration;
import fr.maxlego08.koth.hook.teams.NoneHook;
import fr.maxlego08.koth.save.Config;
import fr.maxlego08.koth.zcore.enums.Message;
import fr.maxlego08.koth.zcore.utils.Cuboid;
import fr.maxlego08.koth.zcore.utils.ZUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ZKoth extends ZUtils implements Koth {

    private final KothPlugin plugin;
    private final String fileName;
    private final KothType kothType;
    private final ScoreboardConfiguration cooldownScoreboard;
    private final ScoreboardConfiguration startScoreboard;
    private final int cooldownStart;
    private final int stopAfterSeconds;
    private final Map<UUID, Integer> playersValues = new HashMap<>();
    private final boolean enableStartCapMessage;
    private final boolean enableLooseCapMessage;
    private final boolean enableEverySecondsCapMessage;
    private String name;
    private int captureSeconds;
    private Location minLocation;
    private Location maxLocation;
    private List<String> startCommands = new ArrayList<>();
    private List<String> endCommands = new ArrayList<>();
    private KothStatus kothStatus = KothStatus.STOP;
    private KothTeam kothTeam = new NoneHook();
    private Player currentPlayer;
    private AtomicInteger remainingSeconds;
    private TimerTask timerTask;
    private TimerTask timerTaskStop;

    public ZKoth(KothPlugin plugin, String fileName, KothType kothType, String name, int captureSeconds, Location minLocation, Location maxLocation,
                 List<String> startCommands, List<String> endCommands, ScoreboardConfiguration cooldownScoreboard, ScoreboardConfiguration startScoreboard,
                 int cooldownStart, int stopAfterSeconds, boolean enableStartCapMessage, boolean enableLooseCapMessage, boolean enableEverySecondsCapMessage) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.kothType = kothType;
        this.name = name;
        this.captureSeconds = captureSeconds;
        this.minLocation = minLocation;
        this.maxLocation = maxLocation;
        this.startCommands = startCommands;
        this.endCommands = endCommands;
        this.startScoreboard = startScoreboard;
        this.cooldownScoreboard = cooldownScoreboard;
        this.cooldownStart = cooldownStart;
        this.stopAfterSeconds = stopAfterSeconds;
        this.enableStartCapMessage = enableStartCapMessage;
        this.enableLooseCapMessage = enableLooseCapMessage;
        this.enableEverySecondsCapMessage = enableEverySecondsCapMessage;
    }

    public ZKoth(KothPlugin plugin, String fileName, KothType kothType, String name, int captureSeconds, Location minLocation, Location maxLocation) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.kothType = kothType;
        this.name = name;
        this.captureSeconds = captureSeconds;
        this.minLocation = minLocation;
        this.maxLocation = maxLocation;
        this.startScoreboard = new ScoreboardConfiguration();
        this.cooldownScoreboard = new ScoreboardConfiguration();
        this.cooldownStart = 300;
        this.stopAfterSeconds = 3600;
        this.enableStartCapMessage = true;
        this.enableLooseCapMessage = true;
        this.enableEverySecondsCapMessage = false;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public KothType getKothType() {
        return this.kothType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Location getMinLocation() {
        return this.minLocation;
    }

    @Override
    public Location getMaxLocation() {
        return this.maxLocation;
    }

    @Override
    public Cuboid getCuboid() {
        return new Cuboid(this.maxLocation, this.minLocation);
    }

    @Override
    public boolean isEnableStartCapMessage() {
        return this.enableStartCapMessage;
    }

    @Override
    public boolean isEnableLooseCapMessage() {
        return this.enableLooseCapMessage;
    }

    @Override
    public boolean isEnableEverySecondsCapMessage() {
        return this.enableEverySecondsCapMessage;
    }

    @Override
    public int getStopAfterSeconds() {
        return this.stopAfterSeconds;
    }

    @Override
    public Location getCenter() {
        Cuboid cuboid = getCuboid();
        return cuboid.getCenter();
    }

    @Override
    public List<String> getStartCommands() {
        return this.startCommands;
    }

    @Override
    public List<String> getEndCommands() {
        return this.endCommands;
    }

    @Override
    public void move(Location minLocation, Location maxLocation) {
        this.minLocation = minLocation;
        this.maxLocation = maxLocation;
    }

    @Override
    public int getCaptureSeconds() {
        return captureSeconds;
    }

    @Override
    public void setCaptureSeconds(int captureSeconds) {
        this.captureSeconds = captureSeconds;
    }

    @Override
    public ScoreboardConfiguration getCooldownScoreboard() {
        return this.cooldownScoreboard;
    }

    @Override
    public ScoreboardConfiguration getStartScoreboard() {
        return this.startScoreboard;
    }

    @Override
    public KothStatus getStatus() {
        return this.kothStatus;
    }

    @Override
    public void spawn(CommandSender sender, boolean now) {

        if (this.minLocation == null || this.maxLocation == null) {
            message(sender, Message.SPAWN_ERROR);
        } else if (this.kothStatus == KothStatus.COOLDOWN) {
            message(sender, Message.SPAWN_COOLDOWN);
        } else if (this.kothStatus == KothStatus.START) {
            message(sender, Message.SPAWN_ALREADY);
        } else {
            if (now) spawnNow();
            else spawn();
        }

    }

    @Override
    public void spawn(boolean now) {
        if (this.minLocation == null || this.maxLocation == null || this.kothStatus != KothStatus.STOP) return;
        if (now) spawnNow();
        else spawn();
    }

    @Override
    public void stop(CommandSender sender) {

        if (this.kothStatus != KothStatus.START) {
            message(sender, Message.EVENT_DISABLE);
            return;
        }

        KothStopEvent event = new KothStopEvent(this);
        event.call();

        if (event.isCancelled()) return;

        broadcast(Message.EVENT_STOP);

        if (this.timerTask != null) {
            this.timerTask.cancel();
        }

        this.kothStatus = KothStatus.STOP;
        this.currentPlayer = null;
        this.timerTask = null;
        this.remainingSeconds = null;
        this.playersValues.clear();
        // this.resetBlocks();
        if (this.timerTaskStop != null) this.timerTaskStop.cancel();

        // this.plugin.getHologram().end(this);
    }

    private void spawn() {
        this.resetData();

        this.kothStatus = KothStatus.COOLDOWN;
        this.remainingSeconds = new AtomicInteger(this.cooldownStart);

        KothStartEvent event = new KothStartEvent(this);
        event.call();

        if (event.isCancelled()) return;

        this.timerTask = scheduleFix(this.plugin, 0, Config.enableDebug ? 10 : 1000, (task, isCancelled) -> {

            if (!isCancelled) {
                task.cancel();
                return;
            }

            if (this.kothStatus != KothStatus.COOLDOWN) {
                task.cancel();
                return;
            }

            int currentRemainingSeconds = this.remainingSeconds.get();

            if (Config.displayMessageCooldown.contains(currentRemainingSeconds)) {
                broadcast(Message.EVENT_COOLDOWN);
            }

            if (currentRemainingSeconds <= 0) {
                this.timerTask.cancel();
                this.spawnNow();
                return;
            }

            this.remainingSeconds.decrementAndGet();
        });
    }

    private void spawnNow() {

        this.resetData();
        this.kothStatus = KothStatus.START;

        KothSpawnEvent event = new KothSpawnEvent(this);
        event.call();

        if (event.isCancelled()) return;

        this.remainingSeconds = new AtomicInteger(this.captureSeconds);

        this.broadcast(Message.EVENT_START);

        // this.changeBlocks(Config.noOneCapturingMaterial, true);

        /*if (this.kothType == KothType.POINT_COUNT) {
            this.startschedule();
        }*/

        Koth koth = this;
        Timer timer = new Timer();
        this.timerTaskStop = new TimerTask() {
            @Override
            public void run() {
                // plugin.getHologram().end(koth);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    stop(Bukkit.getConsoleSender());
                });
            }
        };
        timer.schedule(this.timerTaskStop, this.stopAfterSeconds * 1000L);

        // this.plugin.getHologram().start(this);

        /*if (Config.discordWebhookConfig != null) {
            Config.discordWebhookConfig.send(this.plugin, this);
        }*/

        this.startCommands.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaceKothInformations(command)));
    }

    private void resetData() {
        this.playersValues.clear();
        this.currentPlayer = null;
    }

    @Override
    public void playerMove(Player player, KothTeam kothTeam) {

        if (this.kothStatus != KothStatus.START) return;

        this.kothTeam = kothTeam;
        Cuboid cuboid = this.getCuboid();

        if (this.currentPlayer == null && cuboid.contains(player.getLocation())) {

            this.currentPlayer = player;
            this.startCap(player);
            // this.plugin.getHologram().update(this);

        } else if (this.currentPlayer != null && !cuboid.contains(this.currentPlayer.getLocation())) {

            KothLooseEvent event = new KothLooseEvent(this.currentPlayer, this);
            event.call();

            if (event.isCancelled()) return;

            // this.plugin.getHologram().update(this);
            broadcast(Message.EVENT_LOOSE);

            if (this.timerTask != null) {
                this.timerTask.cancel();
            }

            // this.changeBlocks(Config.noOneCapturingMaterial, true);
            this.remainingSeconds = new AtomicInteger(this.captureSeconds);
            this.timerTask = null;
            this.currentPlayer = null;
        }
    }

    @Override
    public int getCooldownStart() {
        return this.cooldownStart;
    }

    private void startCap(Player player) {

        if (this.currentPlayer == null)
            return;

        KothCatchEvent event = new KothCatchEvent(this, player, this.captureSeconds);
        event.call();

        if (event.isCancelled()) {
            this.currentPlayer = null;
            return;
        }

        if (enableStartCapMessage) {
            broadcast(Message.EVENT_CATCH);
        }

        int captureSeconds = event.getCaptureSeconds();
        captureSeconds = captureSeconds < 0 ? 30 : captureSeconds;
        this.remainingSeconds = new AtomicInteger(captureSeconds);
        Cuboid cuboid = getCuboid();

        // this.changeBlocks(Config.onePersonneCapturingMaterial, false);
        // this.plugin.getHologram().update(this);

        scheduleFix(this.plugin, 0, 1000, (task, isCancelled) -> {

            this.timerTask = task;

            if (!isCancelled) {
                task.cancel();
                return;
            }

            if (this.kothStatus != KothStatus.START) {
                task.cancel();
                return;
            }

            int currentRemainingSeconds = this.remainingSeconds.get();

            if (this.currentPlayer != null) {
                if (!this.currentPlayer.isValid() || !this.currentPlayer.isOnline()
                        || !cuboid.contains(this.currentPlayer.getLocation())) {
                    this.currentPlayer = null;
                    // this.plugin.getHologram().update(this);
                }
            }

            if (this.currentPlayer == null) {

                KothLooseEvent kothLooseEvent = new KothLooseEvent(null, this);
                kothLooseEvent.call();

                if (kothLooseEvent.isCancelled()) {
                    return;
                }

                if (this.timerTask != null) {
                    this.timerTask.cancel();
                }

                // this.changeBlocks(Config.noOneCapturingMaterial, true);
                this.timerTask = null;
                this.currentPlayer = null;
                this.remainingSeconds = new AtomicInteger(this.captureSeconds);

                if (enableLooseCapMessage) {
                    broadcast(Message.EVENT_LOOSE);
                }

                // this.plugin.getHologram().update(this);
                return;

            }

            if (Config.displayMessageKothCap.contains(currentRemainingSeconds)) {
                broadcast(Message.EVENT_TIMER);
            } else if (enableEverySecondsCapMessage) {
                broadcast(Message.EVENT_EVERYSECONDS);
            }

            if (this.hasWin()) {

                this.endKoth(task, cuboid, player);

            } else {

                KothCapEvent capEvent = new KothCapEvent(this, player, this.remainingSeconds.get(), this.kothTeam.getFactionTag(player));
                capEvent.callEvent();

                switch (this.kothType) {
                    case CAPTURE:
                    default:
                        this.remainingSeconds.decrementAndGet();
                        break;
                    case SCORE:
                        // case TIMER:
                        this.playersValues.put(this.currentPlayer.getUniqueId(), this.getValue(this.currentPlayer) + 1);
                        break;
                }

                // this.plugin.getHologram().update(this);
            }
        });
    }

    public void endKoth(TimerTask task, Cuboid cuboid, Player player) {

        KothWinEvent kothWinEvent = new KothWinEvent(this, this.currentPlayer);
        kothWinEvent.call();

        if (kothWinEvent.isCancelled()) return;

        // this.plugin.getHologram().end(this);
        task.cancel();
        broadcast(Message.EVENT_WIN);

        /* Gestion des loots */

        this.endCommands.forEach(command -> {
            if (command.contains("%online-player%")) {
                for (Player cPlayer : this.kothTeam.getOnlinePlayer(player)) {
                    String finaleCommand = replaceMessage(command);
                    finaleCommand = finaleCommand.replace("%online-player%", cPlayer.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), papi(finaleCommand, cPlayer));
                }
            } else {
                command = replaceMessage(command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), papi(command, player));
            }
        });

        Location center = cuboid.getCenter();
        World world = center.getWorld();
        while (center.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR)) {
            center = center.getBlock().getRelative(BlockFace.DOWN).getLocation();
        }

        /*if (this.itemStacks.size() != 0) {
            switch (this.type) {
                case CHEST:
                    location.getBlock().setType(Material.CHEST);
                    Chest chest = (Chest) location.getBlock().getState();

                    this.getItemStacks().forEach(itemStack -> chest.getInventory().addItem(itemStack));

                    Bukkit.getScheduler().runTaskLater(ZPlugin.z(), () -> {
                        location.getBlock().setType(Material.AIR);
                    }, 20 * Config.removeChestSec);
                    break;
                case DROP:
                    location.add(0.5, 0.3, 0.5);
                    this.getItemStacks().forEach(itemStack -> {

                        Item item = world.dropItem(location, itemStack);
                        Vector vector = item.getVelocity();
                        vector.setZ(0);
                        vector.setY(0.5);
                        vector.setX(0);
                        item.setVelocity(vector);

                    });
                    break;
                case INVENTORY:
                    this.getItemStacks().forEach(itemStack -> give(this.currentPlayer, itemStack));
                    break;
                case NONE:
                    break;
                default:
                    break;
            }
        }*/

        this.kothStatus = KothStatus.STOP;
        this.currentPlayer = null;
        this.timerTask = null;
        this.remainingSeconds = null;
        this.playersValues.clear();
        // this.resetBlocks();
        if (this.timerTaskStop != null)
            this.timerTaskStop.cancel();
    }

    public int getValue(Player player) {
        return player == null ? 0 : this.playersValues.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean hasWin() {

        switch (this.kothType) {
            case CAPTURE:
                return this.remainingSeconds != null && this.remainingSeconds.get() <= 0;
            case SCORE:
                return this.currentPlayer != null && this.getValue(this.currentPlayer) >= this.captureSeconds;
            /*case TIMER:
                return this.currentPlayer == null ? false : this.getValue(this.currentPlayer) >= this.maxSecondsCap;*/
            default:
                return false;
        }
    }

    private String replaceMessage(String string) {

        string = string.replace("%playerName%", this.currentPlayer != null ? this.currentPlayer.getName() : Config.noPlayer);

        return replaceKothInformations(string);
    }

    private String replaceKothInformations(String string) {

        Location centerLocation = getCenter();

        string = string.replace("%name%", this.name);
        string = string.replace("%world%", centerLocation.getWorld().getName());
        string = string.replace("%minX%", String.valueOf(minLocation.getBlockX()));
        string = string.replace("%minY%", String.valueOf(minLocation.getBlockY()));
        string = string.replace("%minZ%", String.valueOf(minLocation.getBlockZ()));
        string = string.replace("%maxX%", String.valueOf(maxLocation.getBlockX()));
        string = string.replace("%maxY%", String.valueOf(maxLocation.getBlockY()));
        string = string.replace("%maxZ%", String.valueOf(maxLocation.getBlockZ()));
        string = string.replace("%centerX%", String.valueOf(centerLocation.getBlockX()));
        string = string.replace("%centerY%", String.valueOf(centerLocation.getBlockY()));
        string = string.replace("%centerZ%", String.valueOf(centerLocation.getBlockZ()));

        return string;
    }

}
