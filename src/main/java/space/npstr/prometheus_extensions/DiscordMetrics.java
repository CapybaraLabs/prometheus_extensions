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

package space.npstr.prometheus_extensions;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class DiscordMetrics {

	private final Gauge voiceChannelsConnected;
	private final Gauge discordEntities;
	private final Gauge unavailableGuilds;

	private final Gauge sessionStartLimitTotal;
	private final Gauge sessionStartLimitRemaining;
	private final Gauge recommendedShardCount;

	private final Counter closeCodes;
	private final Counter events;

	private final Summary discordRestRequests;
	private final Histogram discordRestRequestResponseTime;
	private final Counter discordRestHardFailures;

	public DiscordMetrics(final PrometheusRegistry registry) {
		this.voiceChannelsConnected = Gauge.builder()
			.name("discord_voicechannels_connected_current")
			.help("How many voice channel is the bot connected to")
			.register(registry);

		this.discordEntities = Gauge.builder()
			.name("discord_entities_current")
			.help("How many entities are present")
			.labelNames("type")
			.register(registry);

		this.unavailableGuilds = Gauge.builder()
			.name("discord_unavailable_guilds_current")
			.help("How many guilds are unavailable")
			.labelNames("shard")
			.register(registry);


		this.sessionStartLimitTotal = Gauge.builder()
			.name("discord_session_start_limit_total")
			.help("Maximum session start limit")
			.register(registry);

		this.sessionStartLimitRemaining = Gauge.builder()
			.name("discord_session_start_limit_remaining")
			.help("Remaining session starts")
			.register(registry);

		this.recommendedShardCount = Gauge.builder()
			.name("discord_recommended_shard_count")
			.help("Recommended shard count")
			.register(registry);

		this.events = Counter.builder()
			.name("discord_events_received_total")
			.help("All received events by class")
			.labelNames("class")
			.register(registry);

		this.closeCodes = Counter.builder()
			.name("discord_websocket_close_codes_total")
			.help("Close codes of the main websocket connections")
			.labelNames("code")
			.register(registry);

		this.discordRestRequests = Summary.builder()
			.name("discord_rest_request_seconds")
			.help("Total Discord REST requests sent and their received responses")
			.labelNames("method", "uri", "status", "error")
			.register(registry);

		this.discordRestRequestResponseTime = Histogram.builder()
			.name("discord_rest_request_response_time_seconds")
			.classicExponentialUpperBounds(0.05, 1.2, 20) // TODO use new native histograms when support becomes stable
			.help("Discord REST request response time")
			.register(registry);

		this.discordRestHardFailures = Counter.builder()
			.name("discord_rest_request_hard_failures_total")
			.help("Total Discord REST requests that experienced hard failures (not client response exceptions)")
			.labelNames("method", "uri")
			.register(registry);
	}

	public Gauge getVoiceChannelsConnected() {
		return voiceChannelsConnected;
	}

	public Gauge getDiscordEntities() {
		return discordEntities;
	}

	public Gauge getUnavailableGuilds() {
		return unavailableGuilds;
	}

	public Gauge getSessionStartLimitTotal() {
		return sessionStartLimitTotal;
	}

	public Gauge getSessionStartLimitRemaining() {
		return sessionStartLimitRemaining;
	}

	public Gauge getRecommendedShardCount() {
		return recommendedShardCount;
	}

	public Counter getEvents() {
		return events;
	}

	public Counter getCloseCodes() {
		return closeCodes;
	}

	public Summary getDiscordRestRequests() {
		return discordRestRequests;
	}

	public Histogram getDiscordRestRequestResponseTime() {
		return discordRestRequestResponseTime;
	}

	public Counter getDiscordRestHardFailures() {
		return discordRestHardFailures;
	}
}
