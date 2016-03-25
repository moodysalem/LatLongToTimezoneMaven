package com.moodysalem;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.AssertJUnit.assertTrue;

public class TimeZoneMapperTest {

    private static final String EXPECT_ERROR = "City: %s, Lat: %s, Lng: %s, Expected: %s, Got: %s";
    private static final Logger LOG = Logger.getLogger(TimeZoneMapperTest.class.getName());

    public static class TestData {
        private String name;
        private double lat;
        private double lng;
        private String expectedTimezone;

        public TestData(String name, double lat, double lng, String expectedTimezone) {
            if (name == null || expectedTimezone == null) {
                throw new NullPointerException();
            }
            this.name = name;
            this.lat = lat;
            this.lng = lng;
            this.expectedTimezone = expectedTimezone;
        }

        public String getName() {
            return name;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }

        public String getExpectedTimezone() {
            return expectedTimezone;
        }
    }

    private static final List<TestData> testData = new ArrayList<>();

    static {
        try (InputStream is = TimeZoneMapperTest.class.getClassLoader().getResourceAsStream("test-cases.csv");
             CSVParser cp = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(is))) {
            for (CSVRecord cr : cp) {
                testData.add(new TestData(cr.get("name"),
                    Double.parseDouble(cr.get("lat")), Double.parseDouble(cr.get("lng")),
                    cr.get("zone")));
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to parse test file", e);
        }
    }

    @Test
    public void runTestCases() {
        final AtomicInteger succeeded = new AtomicInteger(0), failed = new AtomicInteger(0);

        final Consumer<TestData> tester = (d) -> {
            String timezone = TimezoneMapper.tzNameAt(d.getLat(), d.getLng());
            ZoneId zone = TimezoneMapper.tzAt(d.getLat(), d.getLng());

            String error = String.format(EXPECT_ERROR, d.getName(), d.getLat(), d.getLng(), d.getExpectedTimezone(), timezone);
            try {
                assertTrue(error, d.getExpectedTimezone().equals(timezone));
                assertTrue(error, ZoneId.of(d.getExpectedTimezone()).equals(zone));
                succeeded.incrementAndGet();
            } catch (AssertionError e) {
                failed.incrementAndGet();
                LOG.warning(e.getMessage());
            }
        };

        testData.forEach(tester);

        // hand entered test cases
        tester.accept(new TestData("Chicago, IL", 41.8369, -87.6847, "America/Chicago"));

        final int total = succeeded.get() + failed.get();
        LOG.info(String.format("Failed: %s out of %s; %s accurate", failed, total,
            Math.round(((((double) succeeded.get()) / (total)) * 100)) + "%"));

        // no more than 5% failure
        assertTrue("No more than 5% of the tests may fail", ((double) failed.get()) / total < 0.05);
    }

}
