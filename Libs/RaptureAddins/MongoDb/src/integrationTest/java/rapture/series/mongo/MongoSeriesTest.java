/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package rapture.series.mongo;

import java.util.Map;

import org.junit.Before;

import rapture.config.MultiValueConfigLoader;
import rapture.config.ValueReader;
import rapture.repo.SeriesRepo;
import rapture.series.SeriesContract;
import rapture.series.SeriesStore;

import com.google.common.collect.ImmutableMap;

public class MongoSeriesTest extends SeriesContract {

    @Override
    public SeriesRepo createRepo() {
        Map<String, String> config = ImmutableMap.of("prefix", "testalicious");
        MongoSeriesStore store = new MongoSeriesStore();
        store.setInstanceName("testorama");
        store.setConfig(config);
        return new SeriesRepo(store);
    }

    @Before
    public void setup() {
        MultiValueConfigLoader.setEnvReader(new ValueReader() {
            @Override
            public String getValue(String property) {
                if (property.equals("MONGODB-DEFAULT")) {
                    return "mongodb://test:test@localhost/test";
                }
                return null;
            }
        });
    }
}