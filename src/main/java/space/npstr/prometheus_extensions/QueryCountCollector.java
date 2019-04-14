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

package space.npstr.prometheus_extensions;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import java.util.ArrayList;
import java.util.List;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.listener.SingleQueryCountHolder;

public class QueryCountCollector extends Collector {

	private final SingleQueryCountHolder queryCountHolder;

	public QueryCountCollector(SingleQueryCountHolder queryCountHolder) {
		this.queryCountHolder = queryCountHolder;
	}

	@Override
	public List<MetricFamilySamples> collect() {

		List<MetricFamilySamples> mfs = new ArrayList<>();
		List<String> labelNames = List.of("datasource");

		CounterMetricFamily select = new CounterMetricFamily("jdbc_query_select_total",
			"Total select queries", labelNames
		);
		mfs.add(select);
		CounterMetricFamily insert = new CounterMetricFamily("jdbc_query_insert_total",
			"Total insert queries", labelNames
		);
		mfs.add(insert);
		CounterMetricFamily update = new CounterMetricFamily("jdbc_query_update_total",
			"Total update queries", labelNames
		);
		mfs.add(update);
		CounterMetricFamily delete = new CounterMetricFamily("jdbc_query_delete_total",
			"Total delete queries", labelNames
		);
		mfs.add(delete);
		CounterMetricFamily other = new CounterMetricFamily("jdbc_query_other_total",
			"Total other queries", labelNames
		);
		mfs.add(other);
		CounterMetricFamily total = new CounterMetricFamily("jdbc_query_total",
			"Total queries", labelNames
		);
		mfs.add(total);

		CounterMetricFamily statement = new CounterMetricFamily("jdbc_statement_total",
			"Total statements", labelNames
		);
		mfs.add(statement);
		CounterMetricFamily prepared = new CounterMetricFamily("jdbc_prepared_total",
			"Total prepared statements", labelNames
		);
		mfs.add(prepared);
		CounterMetricFamily callable = new CounterMetricFamily("jdbc_callable_total",
			"Total callable statements", labelNames
		);
		mfs.add(callable);

		CounterMetricFamily success = new CounterMetricFamily("jdbc_success_total",
			"Total successful queries", labelNames
		);
		mfs.add(success);
		CounterMetricFamily failure = new CounterMetricFamily("jdbc_failure_total",
			"Total failed queries", labelNames
		);
		mfs.add(failure);

		CounterMetricFamily time = new CounterMetricFamily("jdbc_time_total_seconds",
			"Total query execution time", labelNames
		);
		mfs.add(time);

		for (var entry : this.queryCountHolder.getQueryCountMap().entrySet()) {
			String datasourceName = entry.getKey();
			QueryCount queryCount = entry.getValue();
			List<String> labels = List.of(datasourceName);

			select.addMetric(labels, queryCount.getSelect());
			insert.addMetric(labels, queryCount.getInsert());
			update.addMetric(labels, queryCount.getUpdate());
			delete.addMetric(labels, queryCount.getDelete());
			other.addMetric(labels, queryCount.getOther());
			total.addMetric(labels, queryCount.getTotal());

			statement.addMetric(labels, queryCount.getStatement());
			prepared.addMetric(labels, queryCount.getPrepared());
			callable.addMetric(labels, queryCount.getCallable());

			success.addMetric(labels, queryCount.getSuccess());
			failure.addMetric(labels, queryCount.getFailure());

			time.addMetric(labels, queryCount.getTime() / MILLISECONDS_PER_SECOND);
		}

		return mfs;
	}
}

