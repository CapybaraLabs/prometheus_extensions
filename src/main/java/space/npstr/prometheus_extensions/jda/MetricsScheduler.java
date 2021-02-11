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

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs certain jobs periodically, only when the shard manager is fully up
 */
class MetricsScheduler {

	private static final Logger log = LoggerFactory.getLogger(MetricsScheduler.class);

	private final ScheduledExecutorService scheduler;
	private final ShardManager shardManager;

	MetricsScheduler(final ScheduledExecutorService scheduler, final ShardManager shardManager) {
		this.scheduler = scheduler;
		this.shardManager = shardManager;
	}

	public void schedule(final Runnable runnable, final Duration period) {
		this.scheduler.scheduleAtFixedRate(() -> {
			try {
				final boolean anyDisconnectedShards = this.shardManager.getShardCache().stream()
					.anyMatch(jda -> jda.getStatus() != JDA.Status.CONNECTED);

				if (!anyDisconnectedShards) {
					runnable.run();
				}
			} catch (final Exception e) {
				log.warn("Failed to run metrics job", e);
			}
		}, 0, period.getSeconds(), TimeUnit.SECONDS);
	}
}
