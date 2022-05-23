// WARNING: THIS PLUGIN WAS MY FIRST PLUGIN AFTER TAKING A LONG BREAK FROM CODING
// AND I ALREADY SUCK AT CODING AND OOP ANYWAY SO
// IT'S SAFE TO SAY THIS IS CERTIFIED SPAGHETTI CODE LOL
// SORRY FOR USING TRANSLATEALTERNATECOLORCODES I HAVE RECENTLY LEARNED PPL HATE THAT CUZ READABILITY I GUESS
// ALSO YES I'M AWARE OF ALL THE DUMB TRY CATCHES LMAO

package io.github.turpcoding.easyreport;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;

public class EasyReport extends JavaPlugin {
    private static EasyReport instance = null;
    public static EasyReport getInstance() {
        // Lazy instantiating.
        if (instance == null)
            instance = (EasyReport) Bukkit.getPluginManager().getPlugin("EasyReport");
        return instance;
    }

    private FileConfiguration customConfig = null;
    private File customConfigFile = null;

    @Override
    public void onEnable() {

        // Copy embedded config.yml to the data folder if config.yml doesn't exist yet.
        saveDefaultConfig();
        // Copy embedded staff.yml to the data folder if staff.yml doesn't exist yet.

        saveDefaultCustomConfig();
        Objects.requireNonNull(this.getCommand("report"), "Check the command's name on 'plugin.yml'.").setExecutor(new ReportCommand());

        Bukkit.getLogger().info("[EasyReport] Plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (getConfig().getBoolean("discord.enabled"))
            try {
                DiscordWebhookAPI.executeWebhook("INFO", "Plugin disabled.", Color.BLUE);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
    }

    public void reloadCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "staff.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

        // Look for defaults in the jar
        Reader defConfigStream = new InputStreamReader(Objects.requireNonNull(this.getResource("staff.yml"), "staff.yml is null."), StandardCharsets.UTF_8);

        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
        customConfig.setDefaults(defConfig);

    }

    public FileConfiguration getCustomConfig() throws UnsupportedEncodingException {
        if (customConfig == null) {
            reloadCustomConfig();
        }
        return customConfig;
    }

    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
            System.out.println("'customConfig' or 'customConfigFile' is null.");
            System.out.println("'customConfig' or 'customConfigFile' is null.");
            return;
        }
        try {
            getCustomConfig().save(customConfigFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save config to " + customConfigFile, ex);
        }
    }

    public void saveDefaultCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "staff.yml");
        }
        if (!customConfigFile.exists()) {
            this.saveResource("staff.yml", false);
        }
    }

    public EasyReport() {}
}