package com.readyplayerai.gainsgoblin;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

public class CSVFileTest {

    @Test
    public void read_parsesCsvData() {
        CSVFile csvFile = new CSVFile(
            new ByteArrayInputStream("Date,Exercise\n1,Bench\n2026-01-05,Squat,Legs".getBytes())
        );
        List result = csvFile.read();
        assertEquals(3, result.size());

        assertArrayEquals(new String[]{"Date", "Exercise"}, (String[]) result.get(0));
        assertArrayEquals(new String[]{"1", "Bench"}, (String[]) result.get(1));
        assertArrayEquals(new String[]{"2026-01-05", "Squat", "Legs"}, (String[]) result.get(2));
    }

    @Test
    public void read_ioError_throwsRuntimeException() {
        InputStream evil = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("test error");
            }
        };
        CSVFile csvFile = new CSVFile(evil);
        try {
            csvFile.read();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue("Message should start with 'Error in reading CSV file'",
                       e.getMessage().startsWith("Error in reading CSV file"));
        }
    }

    @Test
    public void read_javaSplitSemantics_dropsTrailingEmptyCells() {
        CSVFile csvFile = new CSVFile(
            new ByteArrayInputStream(("x,y,,\n" +
                                      "2018-10-29,Seated Dumbbell Press,Shoulders,18.0,10,,,\n" +
                                      "a,b,c,d,e,comment").getBytes())
        );
        java.util.List result = csvFile.read();
        assertEquals(3, result.size());

        assertArrayEquals(new String[]{"x", "y"}, (String[]) result.get(0));
        assertArrayEquals(new String[]{"2018-10-29", "Seated Dumbbell Press", "Shoulders", "18.0", "10"}, (String[]) result.get(1));
        assertArrayEquals(new String[]{"a", "b", "c", "d", "e", "comment"}, (String[]) result.get(2));
    }
}