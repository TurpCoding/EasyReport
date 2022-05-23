package io.github.turpcoding.easyreport;

import java.awt.*;
import java.io.IOException;

public class DiscordWebhookAPI {

    public static void executeWebhook(String title, String embedContent, Color color) throws IOException {
        String webhookURL = EasyReport.getInstance().getConfig().getString("discord.webhook-url");
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookURL);
        discordWebhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle(title)
                .setDescription(embedContent)
                .setColor(color));
        discordWebhook.execute();
    }

    public DiscordWebhookAPI() {}
}