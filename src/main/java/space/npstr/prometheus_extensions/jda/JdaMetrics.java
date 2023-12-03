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

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
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
import net.dv8tion.jda.api.requests.Route;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import space.npstr.prometheus_extensions.DiscordMetrics;

/**
 * Register and extract various metrics from JDA
 */
public class JdaMetrics {

	private final ShardManager shardManager;
	private final MetricsScheduler metricsScheduler;
	private final DistinctUsersCounter distinctUsersCounter;
	private final DiscordMetrics discordMetrics;

	private final Gauge distinctUsers;

	public JdaMetrics(ShardManager shardManager, ScheduledExecutorService scheduler) {
		this(shardManager, scheduler, PrometheusRegistry.defaultRegistry);
	}

	/**
	 * @param shardManager the shard manager of your JDA bot
	 * @param scheduler    some of the JDA metrics are rather costly to calculate, especially on larger bots -
	 *                     those calculations shall be run on the passed in executor
	 */
	public JdaMetrics(
		ShardManager shardManager, ScheduledExecutorService scheduler,
		PrometheusRegistry registry
	) {
		this.shardManager = shardManager;
		this.metricsScheduler = new MetricsScheduler(scheduler, shardManager);
		this.distinctUsersCounter = new DistinctUsersCounter(shardManager);
		this.discordMetrics = new DiscordMetrics(registry);
		final var metricsEventListener = new PrometheusMetricsEventListener(registry, discordMetrics);
		this.shardManager.addEventListener(metricsEventListener);

		this.distinctUsers = Gauge.builder()
			.name("jda_distinct_users_current")
			.help("Total distinct users")
			.register(registry);


		registerMetricsJobs();
	}

	public int getDistinctUsers() {
		GaugeSnapshot.GaugeDataPointSnapshot datapoint = this.distinctUsers.collect().getDataPoints().stream()
			.findFirst().orElse(null);
		if (datapoint == null) {
			return 0;
		}
		return (int) datapoint.getValue();
	}

	private void registerMetricsJobs() {
		final Duration period = Duration.ofMinutes(1);

		this.metricsScheduler.schedule(this::countDistinctUsers, period);
		this.metricsScheduler.schedule(this::countConnectedVoiceChannels, period);
		this.metricsScheduler.schedule(this::countEntities, period);
		this.metricsScheduler.schedule(this::sessionStartLimits, period, false);
	}

	private void sessionStartLimits() {
		DataObject data = fetchSessionStartLimit(shardManager.getShards().stream().findAny().orElseThrow());
		DataObject sessionStartLimit = data.getObject("session_start_limit");
		int total = sessionStartLimit.getInt("total");
		int remaining = sessionStartLimit.getInt("remaining");

		this.discordMetrics.getSessionStartLimitTotal().set(total);
		this.discordMetrics.getSessionStartLimitRemaining().set(remaining);
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
			.filter(GuildVoiceState::inAudioChannel)
			.count();
		this.discordMetrics.getVoiceChannelsConnected().set(count);
	}

	private void countEntities() {
		this.discordMetrics.getDiscordEntities().labelValues("Category")
			.set(countShardEntities(JDA::getCategoryCache));
		this.discordMetrics.getDiscordEntities().labelValues("Guild")
			.set(countShardEntities(JDA::getGuildCache));
		this.discordMetrics.getDiscordEntities().labelValues("PrivateChannel")
			.set(countShardEntities(JDA::getPrivateChannelCache));
		this.discordMetrics.getDiscordEntities().labelValues("TextChannel")
			.set(countShardEntities(JDA::getTextChannelCache));
		this.discordMetrics.getDiscordEntities().labelValues("User")
			.set(countShardEntities(JDA::getUserCache));
		this.discordMetrics.getDiscordEntities().labelValues("VoiceChannel")
			.set(countShardEntities(JDA::getVoiceChannelCache));

		this.discordMetrics.getDiscordEntities().labelValues("Emote")
			.set(countGuildEntities(Guild::getEmojiCache));
		this.discordMetrics.getDiscordEntities().labelValues("Role")
			.set(countGuildEntities(Guild::getRoleCache));

		countUnavailableGuilds(this.discordMetrics.getUnavailableGuilds());
	}

	private long countShardEntities(Function<JDA, CacheView<?>> toCacheView) {
		return this.shardManager.getShards().stream()
			.map(toCacheView)
			.mapToLong(CacheView::size)
			.sum();
	}

	private long countGuildEntities(Function<Guild, CacheView<?>> toCacheView) {
		return this.shardManager.getShards().stream()
			.map(JDA::getGuildCache)
			.flatMap(CacheView::stream)
			.map(toCacheView)
			.mapToLong(CacheView::size)
			.sum();
	}

	private void countUnavailableGuilds(Gauge unavailableGuilds) {
		for (JDA jda : this.shardManager.getShards()) {
			int shardId = jda.getShardInfo().getShardId();
			int size = jda.getUnavailableGuilds().size();
			unavailableGuilds.labelValues(Integer.toString(shardId)).set(size);
		}
	}
}
