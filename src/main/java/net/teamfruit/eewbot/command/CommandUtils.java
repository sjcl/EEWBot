package net.teamfruit.eewbot.command;

import java.util.Optional;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import net.teamfruit.eewbot.EEWBot;
import net.teamfruit.eewbot.i18n.I18nEmbedCreateSpecWrapper;
import net.teamfruit.eewbot.registry.Permission;

public class CommandUtils {

	public static boolean userHasPermission(final EEWBot bot, final long userid, final String command) {
		return bot.getPermissions().values().stream()
				.filter(permission -> permission.getUserid().contains(userid))
				.findAny().orElse(EEWBot.instance.getPermissions().getOrDefault("everyone", Permission.DEFAULT_EVERYONE))
				.getCommand().contains(command);
	}

	public static String getLanguage(final EEWBot bot, final Optional<Snowflake> guildId) {
		return guildId.map(id -> bot.getGuilds().get(id.asLong()))
				.map(guild -> guild.getLang())
				.orElse(bot.getConfig().getDefaultLanuage());
	}

	public static String getLanguage(final EEWBot bot, final MessageCreateEvent event) {
		return getLanguage(bot, event.getGuildId());
	}

	public static String getLanguage(final EEWBot bot, final ReactionAddEvent event) {
		return getLanguage(bot, event.getGuildId());
	}

	public static I18nEmbedCreateSpecWrapper createEmbed(final EmbedCreateSpec spec, final String lang) {
		return new I18nEmbedCreateSpecWrapper(lang, spec)
				.setColor(Color.of(7506394))
				.setAuthor(EEWBot.instance.getUsername(), "https://github.com/Team-Fruit/EEWBot", EEWBot.instance.getAvatarUrl())
				.setFooter("Team-Fruit/EEWBot", "http://i.imgur.com/gFHBoZA.png");
	}

	public static I18nEmbedCreateSpecWrapper createErrorEmbed(final EmbedCreateSpec spec, final String lang) {
		return new I18nEmbedCreateSpecWrapper(lang, spec)
				.setColor(Color.of(255, 64, 64))
				.setAuthor(EEWBot.instance.getUsername(), "https://github.com/Team-Fruit/EEWBot", EEWBot.instance.getAvatarUrl())
				.setFooter("Team-Fruit/EEWBot", "http://i.imgur.com/gFHBoZA.png");
	}
}
