/*
 * MIT License
 *
 * Copyright (c) 2021 Dennis Neufeld
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

package space.npstr.prometheus_extensions.d4j;

import discord4j.common.store.Store;
import discord4j.common.store.action.read.CountTotalAction;
import discord4j.common.store.action.read.ReadActions;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.lifecycle.DisconnectEvent;
import discord4j.gateway.ShardInfo;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import space.npstr.prometheus_extensions.DiscordMetrics;

public class D4JMetrics {

	private static final Logger log = LoggerFactory.getLogger(D4JMetrics.class);

	private final DiscordMetrics discordMetrics;
	private final GatewayDiscordClient gatewayDiscordClient;

	private final Set<Long> unavailableGuilds = ConcurrentHashMap.newKeySet();

	public D4JMetrics(DiscordMetrics discordMetrics, GatewayDiscordClient gatewayDiscordClient) {
		this.discordMetrics = discordMetrics;
		this.gatewayDiscordClient = gatewayDiscordClient;

		trackUnavailableGuilds();

		Duration interval = Duration.ofMinutes(1);

		instrumentUnavailableGuilds(interval);
		instrumentSessionStartLimits(interval);
		instrumentEntities(interval);
		instrumentConnectedVoiceChannels();

		instrumentEvents();
		instrumentCloseCodes();
	}

	private void trackUnavailableGuilds() {
		this.gatewayDiscordClient.getEventDispatcher()
			.on(GuildDeleteEvent.class)
			.filter(GuildDeleteEvent::isUnavailable)
			.map(event -> event.getGuildId().asLong())
			.doOnNext(unavailableGuilds::add)
			.doOnError(t -> log.warn("Failed to add unavailable guild", t))
			.retry()
			.subscribe();

		this.gatewayDiscordClient.getEventDispatcher()
			.on(GuildCreateEvent.class)
			.map(event -> event.getGuild().getId().asLong())
			.doOnNext(unavailableGuilds::remove)
			.doOnError(t -> log.warn("Failed to remove unavailable guild", t))
			.retry()
			.subscribe();
	}

	private void instrumentUnavailableGuilds(Duration interval) {
		Mono.just(1).repeat().delayElements(interval)
			.map(__ -> this.unavailableGuilds.size())
			.doOnNext(count -> this.discordMetrics.getUnavailableGuilds().set(this.unavailableGuilds.size()))
			.doOnError(t -> log.warn("Failed to instrument unavailable guilds", t))
			.subscribe();
	}

	private void instrumentSessionStartLimits(Duration interval) {
		Mono.just(1).repeat().delayElements(interval)
			.flatMap(__ -> gatewayDiscordClient.rest().getGatewayService().getGatewayBot()
				.onErrorResume(t -> {
					log.warn("Failed to fetch gateway/bot", t);
					return Mono.empty();
				})
			)
			.doOnNext(gatewayData -> gatewayData.shards().toOptional().ifPresent(recommendedShards ->
				this.discordMetrics.getRecommendedShardCount().set(recommendedShards)
			))
			.flatMap(gatewayData -> gatewayData.sessionStartLimit().toOptional().map(Mono::just).orElse(Mono.empty()))
			.doOnNext(sessionStartLimitData -> {
				this.discordMetrics.getSessionStartLimitTotal().set(sessionStartLimitData.total());
				this.discordMetrics.getSessionStartLimitRemaining().set(sessionStartLimitData.remaining());
			})
			.doOnError(t -> log.warn("Failed to fetch session start limits", t))
			.subscribe();
	}

	private void instrumentConnectedVoiceChannels() {
		// TODO
	}

	private void instrumentEntities(Duration interval) {
		instrumentEntity(interval, ReadActions.countChannels(), "Channel");
		instrumentEntity(interval, ReadActions.countEmojis(), "Emoji");
		instrumentEntity(interval, ReadActions.countGuilds(), "Guild");
		instrumentEntity(interval, ReadActions.countMembers(), "Member");
		instrumentEntity(interval, ReadActions.countMessages(), "Message");
		instrumentEntity(interval, ReadActions.countPresences(), "Presence");
		instrumentEntity(interval, ReadActions.countRoles(), "Role");
		instrumentEntity(interval, ReadActions.countUsers(), "User");
		instrumentEntity(interval, ReadActions.countVoiceStates(), "VoiceState");
	}

	private void instrumentEntity(Duration interval, CountTotalAction action, String label) {
		Store store = gatewayDiscordClient.getGatewayResources().getStore();
		Mono.just(1).repeat().delayElements(interval)
			.filterWhen(__ -> allShardsUp()
				.onErrorResume(t -> {
					logEntityCountError(t, label);
					return Mono.just(false);
				})
			)
			.flatMap(__ -> Mono.from(store.execute(action))
				.onErrorResume(t -> logEntityCountError(t, label))
			)
			.doOnNext(count -> doInstrument(count, label))
			.doOnError(t -> logEntityCountError(t, label))
			.subscribe();
	}

	private void doInstrument(long count, String label) {
		this.discordMetrics.getDiscordEntities().labels(label).set(count);
	}

	private <T> Mono<T> logEntityCountError(Throwable throwable, String label) {
		log.warn("Failed to count entities {}", label, throwable);
		return Mono.empty();
	}

	private Mono<Boolean> allShardsUp() {
		return this.gatewayDiscordClient.getGatewayResources().getShardCoordinator().getConnectedCount()
			.map(connectedCount -> connectedCount >= countShards());
	}

	private long countShards() {
		return this.gatewayDiscordClient.getGatewayClientGroup().getShardCount();
	}


	private void instrumentEvents() {
		this.gatewayDiscordClient.getEventDispatcher()
			.on(Event.class)
			.doOnNext(event -> this.discordMetrics.getEvents().labels(event.getClass().getSimpleName()).inc())
			.doOnError(t -> log.warn("Failed to track event", t))
			.retry()
			.subscribe();
	}

	private void instrumentCloseCodes() {
		this.gatewayDiscordClient.getEventDispatcher()
			.on(DisconnectEvent.class)
			.doOnNext(this::instrumentCloseCode)
			.doOnError(t -> log.warn("Failed to track close code", t))
			.retry()
			.subscribe();
	}

	private void instrumentCloseCode(final DisconnectEvent event) {
		int code = event.getStatus().getCode();
		this.discordMetrics.getCloseCodes().labels(Integer.toString(code)).inc();

		log.info("Shard {} websocket closed with {} {}",
			formatShard(event.getShardInfo()), code, event.getStatus().getReason().orElse("No reason"),
			event.getCause().orElse(null)
		);
	}

	private String formatShard(ShardInfo shardInfo) {
		return "[" + shardInfo.getIndex() + " / " + shardInfo.getCount() + "]";
	}
}
