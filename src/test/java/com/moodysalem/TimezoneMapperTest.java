package com.moodysalem;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.AssertJUnit.assertTrue;

public class TimezoneMapperTest {
    private static final String EXPECT_ERROR = "City: %s, Lat: %s, Lng: %s, Expected: %s, Got: %s";
    private static final Logger LOG = Logger.getLogger(TimezoneMapperTest.class.getName());

    private static class Test {
        private String name, expectedTimezone;
        private final double lat, lng;

        private Test(final String name, final double lat, final double lng, final String expectedTimezone) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
            this.expectedTimezone = expectedTimezone;
        }
    }

    private static final List<Test> tests = new LinkedList<>();

    static {
        try (final InputStream is = TimezoneMapperTest.class.getClassLoader().getResourceAsStream("test-cases.csv");
             final CSVParser cp = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(is))) {
            for (final CSVRecord cr : cp) {
                tests.add(
                        new Test(
                                cr.get("name"),
                                Double.parseDouble(cr.get("lat")),
                                Double.parseDouble(cr.get("lng")),
                                cr.get("zone")
                        )
                );
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to parse test file", e);
        }
    }

    @org.testng.annotations.Test
    public void testTzNameAt() {
        final AtomicInteger succeeded = new AtomicInteger(0),
                failed = new AtomicInteger(0);

        for (final Test test : tests) {
            final String timezone = TimezoneMapper.tzNameAt(test.lat, test.lng);

            if (test.expectedTimezone.equals(timezone)) {
                succeeded.incrementAndGet();
            } else {
                failed.incrementAndGet();
                LOG.warning(
                        String.format(
                                EXPECT_ERROR,
                                test.name, test.lat, test.lng, test.expectedTimezone, timezone
                        )
                );
            }
        }

        final int total = succeeded.get() + failed.get();
        LOG.info(String.format("Failed: %s out of %s; %s accurate", failed, total,
                Math.round(((((double) succeeded.get()) / (total)) * 100)) + "%"));

        // no more than 5% failure
        assertTrue("No more than 5% of the tests may fail", ((double) failed.get()) / total < 0.05);
    }

}