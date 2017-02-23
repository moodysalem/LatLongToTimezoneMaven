package com.moodysalem;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.AssertJUnit.*;

public class TimezoneMapperTest {
    private static final String EXPECT_ERROR = "City: %s, Lat: %s, Lng: %s, Expected: %s, Got: %s";
    private static final Logger LOG = Logger.getLogger(TimezoneMapperTest.class.getName());

    private static int successes;
    private static int failures;

    @BeforeSuite()
    public void setUp() {
        successes = 0;
        failures = 0;
    }

    @AfterSuite
    public void checkSuiteFailurePercentage() {
        int total = successes + failures;
        LOG.info(String.format("Failed: %s out of %s; %s accurate", failures, total,
            Math.round(((((double) successes) / (total)) * 100)) + "%"));

        // no more than 5% failure
        assertTrue("No more than 5% of the tests may fail", ((double) failures) / total < 0.05);
    }

    @DataProvider(name="TimezoneMapperTest")
    public static Object[][] getTestData() {
        List<Object[]> testData = new ArrayList<>();

        try (InputStream is = TimezoneMapperTest.class.getClassLoader().getResourceAsStream("test-cases.csv");
             CSVParser cp = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(is))) {
            for (CSVRecord cr : cp) {
                testData.add(new Object[] {
                    cr.get("name"),
                    Double.parseDouble(cr.get("lat")),
                    Double.parseDouble(cr.get("lng")),
                    cr.get("zone")
                });
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to parse test file", e);
        }

        return testData.toArray(new Object[][] {});
    }

    @Test(dataProvider = "TimezoneMapperTest")
    public void runTestCase(String name, double lat, double lng, String expectedTimezone) {

        String timezone = TimezoneMapper.tzNameAt(lat, lng);
        try {
            assertEquals(timezone, expectedTimezone);
            successes += 1;
        } catch (AssertionError e) {
            String error = String.format(EXPECT_ERROR, name, lat, lng, expectedTimezone, timezone);
            LOG.warning(error);
            failures += 1;
        }
    }
}
