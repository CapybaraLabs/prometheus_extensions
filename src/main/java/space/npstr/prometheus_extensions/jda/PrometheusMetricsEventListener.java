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
import io.prometheus.client.Counter;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Collect metrics from events happening on the shard
 */
class PrometheusMetricsEventListener extends ListenerAdapter {

	private final Counter events;

	PrometheusMetricsEventListener(final CollectorRegistry registry) {
		this.events = Counter.build()
			.name("discord_events_received_total")
			.help("All events that JDA provides us with by class")
			.labelNames("class")
			.register(registry);
	}

	@Override
	public void onGenericEvent(final GenericEvent event) {
		this.events.labels(event.getClass().getSimpleName()).inc();
	}
}
