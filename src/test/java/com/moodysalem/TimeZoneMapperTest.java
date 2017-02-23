package com.moodysalem;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.AssertJUnit.*;

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

        for (TestData data: testData) {
            String timezone = TimezoneMapper.tzNameAt(data.getLat(), data.getLng());

            String error = String.format(EXPECT_ERROR, data.getName(), data.getLat(), data.getLng(),
                    data.getExpectedTimezone(), timezone);
            try {
                assertTrue(error, data.getExpectedTimezone().equals(timezone));
                succeeded.incrementAndGet();
            } catch (AssertionError e) {
                failed.incrementAndGet();
                LOG.warning(e.getMessage());
            }
        }

        final int total = succeeded.get() + failed.get();
        LOG.info(String.format("Failed: %s out of %s; %s accurate", failed, total,
            Math.round(((((double) succeeded.get()) / (total)) * 100)) + "%"));

        // no more than 5% failure
        assertTrue("No more than 5% of the tests may fail", ((double) failed.get()) / total < 0.05);
    }

}
