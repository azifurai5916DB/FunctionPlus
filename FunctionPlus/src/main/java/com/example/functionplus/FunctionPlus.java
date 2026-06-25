package com.example.functionplus;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FunctionPlus extends JavaPlugin {

    private static final DateTimeFormatter LOG_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static FunctionPlus instance;

    public NamespacedKey transparentKey;
    public NamespacedKey frozenKey;
    public NamespacedKey muteKey;

    private File playersFile;
    private FileConfiguration playersConfig;

    private File logsFile;
    private FileConfiguration logsConfig;

    private boolean protocolLibAvailable;

    private final Set<UUID> mobLookEnabledPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;

        transparentKey = new NamespacedKey(this, "transparent");
        frozenKey = new NamespacedKey(this, "frozen");
        muteKey = new NamespacedKey(this, "mute");

        saveDefaultConfig();
        initPlayersConfig();
        initLogsConfig();
        loadMobLookData();

        if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            protocolLibAvailable = true;
        } else {
            getLogger().warning("ProtocolLib が見つかりません。/moblook は使用できません。");
        }

        getServer().getPluginManager().registerEvents(new MobListener(this), this);

        MobCommands mobCommands = new MobCommands(this);
        registerCommand("mobtoumei", mobCommands);
        registerCommand("kotei", mobCommands);
        registerCommand("mobkoe", mobCommands);
        registerCommand("moblook", mobCommands);
        registerCommand("functionpluslog", new FunctionPlusLogCommand(this));

        if (protocolLibAvailable) {
            getServer().getScheduler().runTaskTimer(this, new MobLookTask(this), 20L, 20L);
        }

        getLogger().info("FunctionPlus v1.1 が有効化されました。");
    }

    @Override
    public void onDisable() {
        clearMobLookForOnlinePlayers();
        saveMobLookData();
        saveLogsConfig();
        getLogger().info("FunctionPlus v1.1 が無効化されました。");
    }

    public static FunctionPlus getInstance() {
        return instance;
    }

    public boolean isProtocolLibAvailable() {
        return protocolLibAvailable;
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("plugin.yml にコマンドが定義されていません: " + name);
            return;
        }
        command.setExecutor(executor);
    }

    private void initPlayersConfig() {
        playersFile = new File(getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            playersFile.getParentFile().mkdirs();
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("players.yml の作成に失敗しました: " + e.getMessage());
            }
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    private void initLogsConfig() {
        logsFile = new File(getDataFolder(), "logs.yml");
        if (!logsFile.exists()) {
            logsFile.getParentFile().mkdirs();
            try {
                logsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("logs.yml の作成に失敗しました: " + e.getMessage());
            }
        }
        logsConfig = YamlConfiguration.loadConfiguration(logsFile);
        if (!logsConfig.isList("logs")) {
            logsConfig.set("logs", new ArrayList<>());
            saveLogsConfig();
        }
    }

    public FileConfiguration getPlayersConfig() {
        return playersConfig;
    }

    public void savePlayersConfig() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            getLogger().severe("players.yml の保存に失敗しました: " + e.getMessage());
        }
    }

    public void addLog(Player player, String command, LivingEntity target) {
        addLog(player, command, target == null ? "-" : target.getType().name());
    }

    public void addLog(Player player, String command, String target) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("player", player.getName());
        log.put("world", player.getWorld().getName());
        log.put("x", player.getLocation().getBlockX());
        log.put("y", player.getLocation().getBlockY());
        log.put("z", player.getLocation().getBlockZ());
        log.put("command", command);
        log.put("target", target);
        log.put("timestamp", LocalDateTime.now().format(LOG_TIMESTAMP_FORMAT));

        List<Map<?, ?>> logs = new ArrayList<>(logsConfig.getMapList("logs"));
        logs.add(log);
        logsConfig.set("logs", logs);
        saveLogsConfig();
    }

    public List<Map<?, ?>> getRecentLogs(int limit) {
        List<Map<?, ?>> logs = logsConfig.getMapList("logs");
        int fromIndex = Math.max(0, logs.size() - limit);
        return new ArrayList<>(logs.subList(fromIndex, logs.size()));
    }

    public void saveLogsConfig() {
        try {
            logsConfig.save(logsFile);
        } catch (IOException e) {
            getLogger().severe("logs.yml の保存に失敗しました: " + e.getMessage());
        }
    }

    private void loadMobLookData() {
        if (playersConfig.isConfigurationSection("moblook")) {
            for (String key : playersConfig.getConfigurationSection("moblook").getKeys(false)) {
                if (playersConfig.getBoolean("moblook." + key)) {
                    try {
                        mobLookEnabledPlayers.add(UUID.fromString(key));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
    }

    private void saveMobLookData() {
        playersConfig.set("moblook", null);
        for (UUID uuid : mobLookEnabledPlayers) {
            playersConfig.set("moblook." + uuid, true);
        }
        savePlayersConfig();
    }

    private void clearMobLookForOnlinePlayers() {
        if (!protocolLibAvailable) return;

        MobLookTask task = new MobLookTask(this);
        for (UUID uuid : mobLookEnabledPlayers) {
            Player player = getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                task.clearGlowingForPlayer(player);
            }
        }
    }

    public Set<UUID> getMobLookEnabledPlayers() {
        return mobLookEnabledPlayers;
    }
}
