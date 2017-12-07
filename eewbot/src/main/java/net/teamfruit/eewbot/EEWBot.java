package net.teamfruit.eewbot;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.teamfruit.eewbot.dispatcher.EEWDispatcher;
import net.teamfruit.eewbot.dispatcher.NTPDispatcher;
import net.teamfruit.eewbot.dispatcher.QuakeInfoDispather;
import net.teamfruit.eewbot.registry.Channel;
import net.teamfruit.eewbot.registry.Config;
import net.teamfruit.eewbot.registry.ConfigurationRegistry;
import net.teamfruit.eewbot.registry.Permission;
import sx.blah.discord.Discord4J.Discord4JLogger;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;

public class EEWBot {
	public static EEWBot instance;

	public static final Logger LOGGER = new Discord4JLogger(EEWBot.class.getName());
	public static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Channel.class, new Channel.ChannelTypeAdapter())
			.setPrettyPrinting()
			.create();

	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, r -> new Thread(r, "EEWBot-communication-thread"));
	private final ConfigurationRegistry<Config> config = new ConfigurationRegistry<>(Paths.get("config.json"), () -> new Config(), Config.class);
	private final ConfigurationRegistry<Map<Long, List<Channel>>> channels = new ConfigurationRegistry<>(Paths.get("channels.json"), () -> new ConcurrentHashMap<Long, List<Channel>>(), new TypeToken<Map<Long, Collection<Channel>>>() {
	}.getType());
	private final ConfigurationRegistry<Map<String, Permission>> permissions = new ConfigurationRegistry<>(Paths.get("permission.json"), () -> new HashMap<String, Permission>() {
		{
			put("owner", Permission.ALL);
			put("everyone", Permission.DEFAULT_EVERYONE);
		}
	}, new TypeToken<Map<String, Permission>>() {
	}.getType());

	private final RequestConfig reqest = RequestConfig.custom()
			.setConnectTimeout(1000*10)
			.setSocketTimeout(10000*10)
			.build();
	private final HttpClient http = HttpClientBuilder.create().setDefaultRequestConfig(this.reqest).build();
	private IDiscordClient client;

	public void initialize() throws IOException {
		this.config.init();
		this.channels.init();
		this.permissions.init();

		if (getConfig().isDebug())
			((Discord4JLogger) EEWBot.LOGGER).setLevel(Discord4JLogger.Level.DEBUG);

		if (StringUtils.isEmpty(getConfig().getToken())) {
			LOGGER.info("Please set a token");
			return;
		}

		this.client = new ClientBuilder()
				.withToken(getConfig().getToken())
				.registerListeners(new DiscordEventListener(), new EEWEventListener())
				.login();

		this.executor.scheduleAtFixedRate(NTPDispatcher.INSTANCE, 0, getConfig().getTimeFixDelay()>=3600 ? getConfig().getTimeFixDelay() : 3600, TimeUnit.SECONDS);
		this.executor.scheduleAtFixedRate(EEWDispatcher.INSTANCE, 10, getConfig().getKyoshinDelay()>=1 ? getConfig().getKyoshinDelay() : 1, TimeUnit.SECONDS);
		this.executor.scheduleAtFixedRate(QuakeInfoDispather.INSTANCE, 0, getConfig().getQuakeInfoDelay()>=10 ? getConfig().getQuakeInfoDelay() : 10, TimeUnit.SECONDS);
		EEWBot.LOGGER.info("Hello");
	}

	public ScheduledExecutorService getExecutor() {
		return this.executor;
	}

	public Config getConfig() {
		return this.config.getElement();
	}

	public Map<Long, List<Channel>> getChannels() {
		return this.channels.getElement();
	}

	public Map<String, Permission> getPermissions() {
		return this.permissions.getElement();
	}

	public ConfigurationRegistry<Config> getConfigRegistry() {
		return this.config;
	}

	public ConfigurationRegistry<Map<Long, List<Channel>>> getChannelRegistry() {
		return this.channels;
	}

	public ConfigurationRegistry<Map<String, Permission>> getPermissionsRegistry() {
		return this.permissions;
	}

	public HttpClient getHttpClient() {
		return this.http;
	}

	public IDiscordClient getClient() {
		return this.client;
	}

	public static void main(final String[] args) throws Exception {
		instance = new EEWBot();
		instance.initialize();
	}
}
