/*
 * MIT License
 *
 * Copyright (c) 2019-2021 Dennis Neufeld
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package space.npstr.prometheus_extensions.jda;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;

/**
 * Register and extract various metrics from JDA
 */
public class JdaMetrics {

	private final ShardManager shardManager;
	private final MetricsScheduler metricsScheduler;
	private final DistinctUsersCounter distinctUsersCounter;


	private final Gauge distinctUsers;
	private final Gauge voiceChannelsConnected;
	private final Gauge discordEntities;
	private final Gauge unavailableGuilds;

	private final Gauge sessionStartLimitTotal;
	private final Gauge sessionStartLimitRemaining;

	public JdaMetrics(final ShardManager shardManager, final ScheduledExecutorService scheduler) {
		this(shardManager, scheduler, CollectorRegistry.defaultRegistry);
	}

	/**
	 * @param shardManager the shard manager of your JDA bot
	 * @param scheduler    some of the JDA metrics are rather costly to calculate, especially on larger bots -
	 *                     those calculations shall be run on the passed in executor
	 */
	public JdaMetrics(
		final ShardManager shardManager, final ScheduledExecutorService scheduler,
		final CollectorRegistry registry
	) {
		this.shardManager = shardManager;
		this.metricsScheduler = new MetricsScheduler(scheduler, shardManager);
		this.distinctUsersCounter = new DistinctUsersCounter(shardManager);
		final var metricsEventListener = new PrometheusMetricsEventListener(registry);
		this.shardManager.addEventListener(metricsEventListener);

		this.distinctUsers = Gauge.build()
			.name("discord_distinct_users_current")
			.help("Total distinct users")
			.register(registry);

		this.voiceChannelsConnected = Gauge.build()
			.name("discord_voicechannels_connected_current")
			.help("How many voice channel is the bot connected to")
			.register(registry);

		this.discordEntities = Gauge.build()
			.name("discord_entities_current")
			.help("How many entities are present")
			.labelNames("type")
			.register(registry);

		this.unavailableGuilds = Gauge.build()
			.name("discord_unavailable_guilds_current")
			.help("How many guilds are unavailable")
			.register(registry);


		this.sessionStartLimitTotal = Gauge.build()
			.name("discord_session_start_limit_total")
			.help("Maximum session start limit")
			.register(registry);

		this.sessionStartLimitRemaining = Gauge.build()
			.name("discord_session_start_limit_remaining")
			.help("Remaining session starts")
			.register(registry);

		registerMetricsJobs();
	}

	public int getDistinctUsers() {
		return ((Double) this.distinctUsers.get()).intValue();
	}

	private void registerMetricsJobs() {
		final Duration period = Duration.ofMinutes(1);

		this.metricsScheduler.schedule(this::countDistinctUsers, period);
		this.metricsScheduler.schedule(this::countConnectedVoiceChannels, period);
		this.metricsScheduler.schedule(this::countEntities, period);
		this.metricsScheduler.schedule(this::sessionStartLimits, period);
	}

	private void sessionStartLimits() {
		DataObject data = fetchSessionStartLimit(shardManager.getShards().stream().findAny().orElseThrow());
		DataObject sessionStartLimit = data.getObject("session_start_limit");
		int total = sessionStartLimit.getInt("total");
		int remaining = sessionStartLimit.getInt("remaining");

		this.sessionStartLimitTotal.set(total);
		this.sessionStartLimitRemaining.set(remaining);
	}

	/**
	 * 80% copied over from {@link net.dv8tion.jda.api.utils.SessionControllerAdapter#getShardedGateway(JDA)}
	 */
	private DataObject fetchSessionStartLimit(JDA api) {
		return new RestActionImpl<DataObject>(api, Route.Misc.GATEWAY_BOT.compile()) {
			@Override
			public void handleResponse(Response response, Request<DataObject> request) {
				if (response.isOk()) {
					request.onSuccess(response.getObject());
				} else if (response.code == 401) {
					api.shutdownNow();
					request.onFailure(new LoginException("The provided token is invalid!"));
				} else {
					request.onFailure(response);
				}
			}
		}.priority().submit().join();
	}

	private void countDistinctUsers() {
		this.distinctUsers.set(this.distinctUsersCounter.count());
	}

	private void countConnectedVoiceChannels() {
		final long count = this.shardManager.getGuildCache().stream()
			.map(Guild::getSelfMember)
			.map(Member::getVoiceState)
			.filter(Objects::nonNull)
			.filter(GuildVoiceState::inVoiceChannel)
			.count();
		this.voiceChannelsConnected.set(count);
	}

	private void countEntities() {
		this.discordEntities.labels("Category")
			.set(countShardEntities(JDA::getCategoryCache));
		this.discordEntities.labels("Guild")
			.set(countShardEntities(JDA::getGuildCache));
		this.discordEntities.labels("PrivateChannel")
			.set(countShardEntities(JDA::getPrivateChannelCache));
		this.discordEntities.labels("TextChannel")
			.set(countShardEntities(JDA::getTextChannelCache));
		this.discordEntities.labels("User")
			.set(countShardEntities(JDA::getUserCache));
		this.discordEntities.labels("VoiceChannel")
			.set(countShardEntities(JDA::getVoiceChannelCache));

		this.discordEntities.labels("Emote")
			.set(countGuildEntities(Guild::getEmoteCache));
		this.discordEntities.labels("Role")
			.set(countGuildEntities(Guild::getRoleCache));

		this.unavailableGuilds.set(countUnavailableGuilds());
	}

	private long countShardEntities(final Function<JDA, CacheView<?>> toCacheView) {
		return this.shardManager.getShards().stream()
			.map(toCacheView)
			.mapToLong(CacheView::size)
			.sum();
	}

	private long countGuildEntities(final Function<Guild, CacheView<?>> toCacheView) {
		return this.shardManager.getShards().stream()
			.map(JDA::getGuildCache)
			.flatMap(CacheView::stream)
			.map(toCacheView)
			.mapToLong(CacheView::size)
			.sum();
	}

	private long countUnavailableGuilds() {
		return this.shardManager.getShards().stream()
			.map(jda -> (JDAImpl) jda)
			.mapToLong(jda -> jda.getUnavailableGuilds().size())
			.sum();
	}
}
