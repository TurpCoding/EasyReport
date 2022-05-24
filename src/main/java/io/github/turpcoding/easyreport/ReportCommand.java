package io.github.turpcoding.easyreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

public class ReportCommand implements CommandExecutor {
    private final EasyReport mainClass = EasyReport.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        int argsNumber = args.length;
        // Check if commandSender is a player.
        if (!(commandSender instanceof Player)) {
            try {
                Bukkit.getLogger().log(Level.INFO, "Someone not considered a player entity just tried to use the report command.");
                if (mainClass.getConfig().getBoolean("discord.enabled"))
                    DiscordWebhookAPI.executeWebhook(
                        "WARNING",
                        "Someone not considered a player entity just tried to use the report command.\\nCommand arguments used: " + Arrays.toString(args), Color.YELLOW);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        Player player = (Player) commandSender;
        try {
            // /REPORT HELP -> display help.
            if (argsNumber == 0 || (args[0].equalsIgnoreCase("help") && argsNumber == 1)) {
                if (!permissionCheck(player, "report.help")) {return true;}

                String helpWithoutPrefix = "&e/report help &f- displays all the available commands.\n" +
                        "&e/report &6<player> <reason> &f- reports a player.\n" +
                        "&e/report records &6<player> &f- shows a player's report records.\n" +
                        "&e/report addstaff &6<player> &f- adds a player to the staff list so they can get notified about reports.\n" +
                        "&e/report removestaff &6<player> &f- removes a player from the staff list so they stop getting notified about reports.\n" +
                        "&e/report reload &f- reloads the plugin's 'config.yml' and 'staff.yml' files.\n";

                String helpWithPrefix = "&4&l[EasyReport]&r&c - Turp's Easy & Simple Report Plugin\n" + helpWithoutPrefix;

                String help = ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? helpWithoutPrefix
                                : helpWithPrefix);

                player.sendMessage(help);
                return true;
            }

            // /REPORT RELOAD

            if (args[0].equalsIgnoreCase("reload") && argsNumber == 1) {
                if (!permissionCheck(player, "report.help")) {return true;}

                mainClass.reloadConfig();
                mainClass.reloadCustomConfig();

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ?  "&aTurp's Easy & Simple Report Plugin (EasyReport) reloaded."
                                : "&4&l[EasyReport]&r &aTurp's Easy & Simple Report Plugin (EasyReport) reloaded."));
                return true;
            }

            // /REPORT ADDSTAFF <PLAYER>
            if (args[0].equalsIgnoreCase("addstaff") && argsNumber == 2) {
                if (!permissionCheck(player, "report.addstaff")) {return true;}

                ArrayList<String> staffMembersList = (ArrayList<String>) Objects.requireNonNull(
                        mainClass.getCustomConfig().getStringList("staffMembers"), "'staffMembers' cannot be null.");

                for (String staffMember : staffMembersList) {
                    if (staffMember.equalsIgnoreCase(args[1])) {

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                        ?  "&cError: Player already on the staff list."
                                        : "&4&l[EasyReport]&r &cError: Player already on the staff list."));
                        return true;
                    }
                }

                // Here the username was not on the list so the file is updated.
                ArrayList<String> currentList = (ArrayList<String>) mainClass.getCustomConfig().getStringList("staffMembers");
                currentList.add(args[1]);
                mainClass.getCustomConfig().set("staffMembers", currentList);
                mainClass.saveCustomConfig();

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&aPlayer added to the staff list."
                                : "&4&l[EasyReport]&r &aPlayer added to the staff list."));
                return true;
            }

            // /REPORT REMOVESTAFF <PLAYER>
            if (args[0].equalsIgnoreCase("removestaff") && argsNumber == 2) {
                if (!permissionCheck(player, "report.removestaff")) {return true;}
                ArrayList<String> staffMembersList = (ArrayList<String>) Objects.requireNonNull(
                        mainClass.getCustomConfig().getStringList("staffMembers"), "'staffMembers' cannot be null.");

                // Converting the list to a set to remove possible duplicates.
                Set<String> staffMembersListSet = new HashSet<>(staffMembersList);

                for (String staffMember : staffMembersListSet) {
                    // Here the username was on the list so the file is now being updated.
                    if (staffMember.equalsIgnoreCase(args[1])) {

                        // Remove the player from the set.
                        staffMembersListSet.removeIf(x->x.equalsIgnoreCase(args[1]));

                        // Convert the set back to a list.
                        staffMembersList = new ArrayList<>(staffMembersListSet);

                        mainClass.getCustomConfig().set("staffMembers", staffMembersList);
                        mainClass.saveCustomConfig();

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                        ? "&aRemoved player from the staff list."
                                        : "&4&l[EasyReport]&r &aRemoved player from the staff list."));
                        return true;
                    }
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cError: Player not on the staff list."
                                : "&4&l[EasyReport]&r &cError: Player not on the list."));
                return true;
            }

            // /REPORT RECORDS <PLAYER>
            try {
                if (args[0].equalsIgnoreCase("records") && argsNumber == 2 && mainClass.getConfig().getBoolean("database.enabled")) {
                    if (!permissionCheck(player, "report.records")) {return true;}

                    DatabaseBridge db = new DatabaseBridge();
                    ArrayList<ArrayList<String>> reportRecords = db.getReports(args[1]);

                    // If the ResultSet is empty.
                    if (reportRecords.get(0).get(0).equals("Empty")) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                        ? "&f" + args[1] + " &cdoes not have any records."
                                        : "&4&l[EasyReport]&f " + args[1] + " &cdoes not have any records."));
                        return true;
                    }
                    // Gets the player name from the database instead of arg[1] for guaranteed correct capitalization.
                    String playerBeingQueried = reportRecords.get(0).get(0);

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                    ? "&cReports against &f" + playerBeingQueried + ":"
                                    : "&4&l[EasyReport]&r &cReports against &f" + playerBeingQueried + ":"));

                    for (ArrayList<String> currentRecord : reportRecords) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&fReported on &7" + currentRecord.get(3)  + " &f by &a" + currentRecord.get(1) + "&f for &c" + currentRecord.get(2)));
                    }

                    player.sendMessage(ChatColor.translateAlternateColorCodes(
                            '&', "&cTotal number of reports: &f" + reportRecords.size()));
                    return true;

                } else if (args[0].equalsIgnoreCase("records") && argsNumber == 2 && !(mainClass.getConfig().getBoolean("database.enabled"))) {

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                            ? "&cYou have to first set up a database and configure 'config.yml' accordingly in order to use this feature."
                            : "&4&l[EasyReport]&r &cYou have to first set up a database and configure 'config.yml' accordingly in order to use this feature."));
                    return true;

                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "A general exception while running /report records just got caught.");
                e.printStackTrace();
                return true;
            }

            // /REPORT <PLAYER> <REASON>
            Player reportedPlayer;
            if ((reportedPlayer = Bukkit.getPlayer(args[0])) != null && argsNumber == 2 && !reportedPlayer.equals(player)) {
                if (!permissionCheck(player, "report.report")) {return true;}

                String reportReason = args[1];

                String staffNotificationMsg = MessageFormat.format(
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                        ? "&a{0} &fjust reported &c{1} &ffor &c{2}"
                        : "&4&l[EasyReport]&r &a{0} &fjust reported &c{1} &ffor &c{2}"
                        , player.getName(), reportedPlayer.getName(), reportReason);

                boolean staffNotified = false;
                Player staff;
                for (String staffMember : mainClass.getCustomConfig().getStringList("staffMembers")) {
                    if ((staff = Bukkit.getPlayer(staffMember)) != null) {

                        staff.sendMessage(ChatColor.translateAlternateColorCodes('&', staffNotificationMsg));
                        staffNotified = true;

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cThank you! &fYour report has been received and the staff were notified."
                                : "&4&l[EasyReport]&r &cThank you! &fYour report has been received and the staff were notified."));

                        if (mainClass.getConfig().getBoolean("staffNotificationSound"))
                            staff.playSound(staff.getLocation(), Sound.ENTITY_CAT_HISS, 1.0f, 1.0f);
                    }
                }

                if (!staffNotified) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                    ? "&cYour report went through but there's no staff online so no one got notified."
                                    : "&4&l[EasyReport]&r &cYour report went through but there's no staff online so no one got notified."));
                }

                try {
                    // Save report to a database.
                    if (mainClass.getConfig().getBoolean("database.enabled")) {
                        DatabaseBridge db = new DatabaseBridge();
                        boolean status = db.insertInto(reportedPlayer.getName(), player.getName(), args[1]);

                        int i = 0;
                        // If the insertInto fails, try again until it either works or the counter reaches 5.
                        while (i < 5 && !(status)) {
                            status = db.insertInto(reportedPlayer.getName(), player.getName(), args[1]);
                            i++;
                        }
                    }
                    // Discord webhook stuff if enabled.
                    if (mainClass.getConfig().getBoolean("discord.enabled")) {
                        DiscordWebhookAPI.executeWebhook("REPORT",
                                MessageFormat.format(
                                        "{0} was just reported by {1} for {2}", reportedPlayer.getName(), player.getName(), reportReason)
                                , Color.GREEN);
                    }
                    return true;

                } catch (IOException e) {
                    Bukkit.getLogger().log(Level.WARNING, "An IOException just got caught.");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Internal error."));
                    e.printStackTrace();
                    return true;
                }
            } else if (reportedPlayer != null && argsNumber == 2 && reportedPlayer.equals(player))
            {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cYou cannot report yourself!"
                                : "&4&l[EasyReport] &cYou cannot report yourself!"));
                return true;

            } else if (reportedPlayer == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cThe player you tried to report either doesn't exist or isn't online."
                                : "&4&l[EasyReport] &cThe player you tried to report either doesn't exist or isn't online."));
            } else {return false;}
        } catch (Exception e) {
            try {
                Bukkit.getLogger().log(Level.WARNING, "A general exception just got caught.");
                if (mainClass.getConfig().getBoolean("discord.enabled"))
                    DiscordWebhookAPI.executeWebhook("CRITICAL ERROR", "Internal error.\\n" +
                        " Player who issued the command: " + player.getName(), Color.RED);
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
        return true;
    }

    private boolean permissionCheck(Player player, String perm) {
        if (!(player.hasPermission(perm) || player.hasPermission("report.*"))) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                            ? "&cYou do not have permission to use this command."
                            : "&4&l[EasyReport]&r &cYou do not have permission to use this command."));
            return false;
        }
        return true;
    }
}
