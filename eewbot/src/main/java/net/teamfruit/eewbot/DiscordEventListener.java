package net.teamfruit.eewbot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class DiscordEventListener {

	@EventSubscriber
	public void onMessageReceived(final MessageReceivedEvent e) {
		final String msg = e.getMessage().getContent();
		if (msg.startsWith("!eew")) {
			final String[] args = msg.split(" ");
			if (args.length<=1)
				BotUtils.reply(e, "引数が不足しています！");
			else {
				final Command command = EnumUtils.getEnum(Command.class, args[1]);
				if (command!=null)
					command.onCommand(e, ArrayUtils.subarray(args, 2, args.length+1));
			}
		}
	}

	@EventSubscriber
	public void onChannelDelete(final ChannelDeleteEvent e) {
		final Collection<Channel> channels = EEWBot.channels.get(e.getGuild().getLongID());
		if (channels!=null) {
			final long id = e.getChannel().getLongID();
			for (final Iterator<Channel> it = channels.iterator(); it.hasNext();)
				if (it.next().getId()==id)
					it.remove();
			try {
				EEWBot.saveConfigs();
			} catch (final ConfigException ex) {
				EEWBot.LOGGER.error("Error on channel delete", ex);
			}
		}
	}

	public static enum Command {
		register {
			@Override
			public void onCommand(final MessageReceivedEvent e, final String[] args) {
				final long serverid = e.getGuild().getLongID();
				final long channelid = e.getChannel().getLongID();
				Collection<Channel> channels = EEWBot.channels.get(serverid);
				Channel channel = BotUtils.getChannel(serverid, channelid);
				if (channels==null)
					channels = new ArrayList<>();
				if (channel==null)
					channel = new Channel(channelid);

				if (args.length<=0)
					channel.eewAlart = true;
				else if (args.length%2!=0)
					BotUtils.reply(e, "引数が不足しています");
				else {
					final Field[] fields = Channel.class.getFields();
					for (int i = 0; i<args.length; i += 2) {
						for (final Field line : fields) {
							if (line.getName().equals(args[i]))
								try {
									line.setBoolean(channel, BooleanUtils.toBoolean(args[i+1]));
								} catch (IllegalArgumentException|IllegalAccessException ex) {
									BotUtils.reply(e, "エラが発生しました");
									EEWBot.LOGGER.error("Reflection error", ex);
								}
						}
					}
				}

				channels.add(channel);
				EEWBot.channels.put(serverid, channels);
				try {
					EEWBot.saveConfigs();
				} catch (final ConfigException ex) {
					BotUtils.reply(e, "ConfigException");
					EEWBot.LOGGER.error("Save error", ex);
				}
				BotUtils.reply(e, "チャンネルを設定しました");
			}
		},
		unregister {
			@Override
			public void onCommand(final MessageReceivedEvent e, final String[] args) {
				final Collection<Channel> channels = EEWBot.channels.get(e.getGuild().getLongID());
				if (channels!=null) {
					final long id = e.getChannel().getLongID();
					for (final Iterator<Channel> it = channels.iterator(); it.hasNext();)
						if (it.next().getId()==id)
							it.remove();
					try {
						EEWBot.saveConfigs();
					} catch (final ConfigException ex) {
						EEWBot.LOGGER.error("Error on channel delete", ex);
					}
				} else
					BotUtils.reply(e, "このチャンネルには設定がありません");
			}
		},
		reload {
			@Override
			public void onCommand(final MessageReceivedEvent e, final String[] args) {
				try {
					EEWBot.loadConfigs();
					BotUtils.reply(e, ":ok:");
				} catch (final ConfigException ex) {
					BotUtils.reply(e, "エラーが発生しました");
					EEWBot.LOGGER.error("Load error", ex);
				}
			}
		};

		public abstract void onCommand(MessageReceivedEvent e, String[] args);
	}

}