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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class DiscordMetrics {

	private final Gauge distinctUsers;
	private final Gauge voiceChannelsConnected;
	private final Gauge discordEntities;
	private final Gauge unavailableGuilds;

	private final Gauge sessionStartLimitTotal;
	private final Gauge sessionStartLimitRemaining;

	private final Counter closeCodes;
	private final Counter events;

	public DiscordMetrics(final CollectorRegistry registry) {
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

		this.events = Counter.build()
			.name("discord_events_received_total")
			.help("All received events by class")
			.labelNames("class")
			.register(registry);

		this.closeCodes = Counter.build()
			.name("discord_websocket_close_codes_total")
			.help("Close codes of the main websocket connections")
			.labelNames("code")
			.register(registry);
	}

	public Gauge getDistinctUsers() {
		return distinctUsers;
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

	public Counter getEvents() {
		return events;
	}

	public Counter getCloseCodes() {
		return closeCodes;
	}
}
