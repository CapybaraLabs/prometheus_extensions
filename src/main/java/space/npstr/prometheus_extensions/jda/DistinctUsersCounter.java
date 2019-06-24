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

import gnu.trove.procedure.TObjectProcedure;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.utils.cache.SnowflakeCacheViewImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BiG uGlY hAcKs in here
 */
public class DistinctUsersCounter {

	private static final Logger log = LoggerFactory.getLogger(DistinctUsersCounter.class);

	private final ShardManager shardManager;

	private final AtomicInteger lastUserCount = new AtomicInteger(0);

	DistinctUsersCounter(final ShardManager shardManager) {
		this.shardManager = shardManager;
	}

	public int count() {
		final long now = System.currentTimeMillis();
		final int distinctUsers = countDistinctUsers(this.shardManager.getShards(), this.lastUserCount);
		log.debug("Distinct  users counted: {} in {}ms", distinctUsers, System.currentTimeMillis() - now);
		this.lastUserCount.set(distinctUsers);
		return distinctUsers;
	}

	//TY FredBoat / Shredder
	private int countDistinctUsers(final Collection<JDA> shards, final AtomicInteger expectedUserCount) {
		if (shards.size() == 1) { //a single shard provides a cheap call for getting user cardinality
			return Math.toIntExact(shards.iterator().next().getUserCache().size());
		}

		final int expected = expectedUserCount.get() > 0 ? expectedUserCount.get() : LongOpenHashSet.DEFAULT_INITIAL_SIZE;
		final LongOpenHashSet distinctUsers = new LongOpenHashSet(expected + 10000); //add 10k for good measure
		final TObjectProcedure<User> adder = user -> {
			distinctUsers.add(user.getIdLong());
			return true;
		};
		Collections.unmodifiableCollection(shards).forEach(
			// IMPLEMENTATION NOTE: READ
			// careful, touching the map is in not all cases safe
			// In this case, it just so happens to be safe, because the map is synchronized
			// this means however, that for the (small) duration, the map cannot be used by other threads (if there are any)
				shard -> {
					SnowflakeCacheViewImpl<User> userCache = (SnowflakeCacheViewImpl<User>) shard.getUserCache();
					try (var ignored = userCache.writeLock()) {
						userCache.getMap().forEachValue(adder);
					}
				}
		);
		return distinctUsers.size();
	}
}
