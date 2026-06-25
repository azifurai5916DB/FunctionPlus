package com.example.functionplus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FunctionPlusLogCommand implements CommandExecutor {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final FunctionPlus plugin;

    public FunctionPlusLogCommand(FunctionPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        int limit = DEFAULT_LIMIT;
        if (args.length >= 1) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c件数は数値で指定してください。");
                return true;
            }
        }

        if (limit <= 0) {
            player.sendMessage("§c件数は1以上で指定してください。");
            return true;
        }
        limit = Math.min(limit, MAX_LIMIT);

        List<Map<?, ?>> logs = plugin.getRecentLogs(limit);
        if (logs.isEmpty()) {
            player.sendMessage("§7FunctionPlus のログはまだありません。");
            return true;
        }

        Collections.reverse(logs);
        player.sendMessage("§6FunctionPlus 実行履歴 §7(最新 " + logs.size() + " 件)");
        for (Map<?, ?> log : logs) {
            player.sendMessage(formatLog(log));
        }
        return true;
    }

    private String formatLog(Map<?, ?> log) {
        return "§7[" + value(log, "timestamp") + "] §f"
                + value(log, "player") + " §e/" + value(log, "command")
                + " §7target=" + value(log, "target")
                + " loc=" + value(log, "world") + " "
                + value(log, "x") + "," + value(log, "y") + "," + value(log, "z");
    }

    private String value(Map<?, ?> log, String key) {
        Object value = log.get(key);
        return value == null ? "-" : value.toString();
    }
}
