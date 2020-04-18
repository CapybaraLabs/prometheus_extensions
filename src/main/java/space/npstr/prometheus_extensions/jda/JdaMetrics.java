/*
 * MIT License
 *
 * Copyright (c) 2019 Dennis Neufeld
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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheView;

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
	}

	private void countDistinctUsers() {
		this.distinctUsers.set(this.distinctUsersCounter.count());
	}

	private void countConnectedVoiceChannels() {
		final long count = this.shardManager.getGuildCache().stream()
			.map(Guild::getSelfMember)
			.filter(Objects::nonNull)
			.map(Member::getVoiceState)
			.filter(Objects::nonNull)
			.filter(GuildVoiceState::inVoiceChannel)
			.count();
		this.voiceChannelsConnected.set(count);
	}

	private void countEntities() {
		this.discordEntities.labels("Category")
			.set(countEntities(JDA::getCategoryCache));
		this.discordEntities.labels("Emote")
			.set(countEntities(JDA::getEmoteCache));
		this.discordEntities.labels("Guild")
			.set(countEntities(JDA::getGuildCache));
		this.discordEntities.labels("PrivateChannel")
			.set(countEntities(JDA::getPrivateChannelCache));
		this.discordEntities.labels("Role")
			.set(countEntities(JDA::getRoleCache));
		this.discordEntities.labels("TextChannel")
			.set(countEntities(JDA::getTextChannelCache));
		this.discordEntities.labels("User")
			.set(countEntities(JDA::getUserCache));
		this.discordEntities.labels("VoiceChannel")
			.set(countEntities(JDA::getVoiceChannelCache));
	}

	private long countEntities(final Function<JDA, CacheView<?>> toCacheView) {
		return this.shardManager.getShards().stream()
			.map(toCacheView)
			.mapToLong(CacheView::size)
			.sum();
	}
}
