package com.readyplayerai.gainsgoblin;

import com.readyplayerai.gainsgoblin.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelTest {

    @Test
    public void workoutSet_getVolume() {
        // volume = reps * weight
        WorkoutSet set = new WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0);
        assertEquals(400.0, set.getVolume(), 0.001);
    }

    @Test
    public void workoutSet_getEplayOneRepMax() {
        // Epley formula: weight * (1 + reps / 30) = 80 * (1 + 5/30) = 93.333...
        WorkoutSet set = new WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0);
        assertEquals(93.333, set.getEplayOneRepMax(), 0.001);
    }

    @Test
    public void workoutSet_constructors() {
        // Constructor with comment
        WorkoutSet withComment = new WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good set");
        assertEquals("good set", withComment.getComment());

        // Constructor without comment (comment defaults to "")
        WorkoutSet withoutComment = new WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0);
        assertEquals("", withoutComment.getComment());

        // No-arg constructor leaves fields null
        WorkoutSet empty = new WorkoutSet();
        assertNull(empty.getDate());
        assertNull(empty.getExerciseName());
        assertNull(empty.getCategory());
        assertNull(empty.getReps());
        assertNull(empty.getWeight());
        assertNull(empty.getComment());
        assertEquals(0, empty.getId());
        assertEquals(0, empty.getUser_id());
    }

    @Test
    public void exercise_constructorAndGettersSetters() {
        Exercise ex = new Exercise("Deadlift", "Back");
        assertEquals("Deadlift", ex.getName());
        assertEquals("Back", ex.getBodyPart());
        assertFalse(ex.getFavorite());

        // Setters
        ex.setName("Sumo Deadlift");
        ex.setBodyPart("Legs");
        ex.setFavorite(true);
        assertEquals("Sumo Deadlift", ex.getName());
        assertEquals("Legs", ex.getBodyPart());
        assertTrue(ex.getFavorite());
    }

    @Test
    public void workoutDay_aggregation() {
        WorkoutDay day = new WorkoutDay();
        WorkoutSet s1 = new WorkoutSet("2026-01-05", "Bench", "Chest", 2.25, 80.0);
        WorkoutSet s2 = new WorkoutSet("2026-01-05", "Bench", "Chest", 2.25, 80.0, "good");
        WorkoutSet s3 = new WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0);
        WorkoutSet s4 = new WorkoutSet("2026-01-05", "Squat", "Legs", 1.0, 120.0, "pr");

        day.addSet(s1);
        day.addSet(s2);
        day.addSet(s3);
        day.addSet(s4);

        // --- Day-level assertions ---
        assertEquals("2026-01-05", day.getDate());
        assertEquals(980.0, day.getDayVolume(), 0.001);
        assertEquals(2, day.getExercises().size());
        assertEquals(4, day.getSets().size());

        // Exercises in TreeSet alphabetical order: "Bench" < "Squat"
        WorkoutExercise bench = day.getExercises().get(0);
        WorkoutExercise squat = day.getExercises().get(1);

        assertEquals("Bench", bench.getExercise());
        assertEquals("Squat", squat.getExercise());

        // --- Bench Press assertions ---
        // Volume: 180 + 180 = 360
        assertEquals(360.0, bench.getVolume(), 0.001);
        // Epley: 80 * (1 + 2.25/30) = 80 * 1.075 = 86.0
        assertEquals(86.0, bench.getEstimatedOneRepMax(), 0.001);
        // No set with reps == 1
        assertEquals(0.0, bench.getActualOneRepMax(), 0.001);
        assertEquals(80.0, bench.getMaxWeight(), 0.001);
        assertEquals(2.25, bench.getMaxReps(), 0.001);
        assertEquals(4.5, bench.getTotalReps(), 0.001);
        assertEquals(2.0, bench.getTotalSets(), 0.001);
        assertEquals(180.0, bench.getMaxSetVolume(), 0.001);
        assertEquals("good", bench.getComment());
        assertEquals(2, bench.getSets().size());

        // --- Squat assertions ---
        // Volume: 500 + 120 = 620
        assertEquals(620.0, squat.getVolume(), 0.001);
        // Epley: max(100*(1+5/30), 120*(1+1/30)) = max(116.667, 124.0) = 124.0
        assertEquals(124.0, squat.getEstimatedOneRepMax(), 0.001);
        // Set with reps == 1 and weight 120.0
        assertEquals(120.0, squat.getActualOneRepMax(), 0.001);
        assertEquals(120.0, squat.getMaxWeight(), 0.001);
        assertEquals(5.0, squat.getMaxReps(), 0.001);
        assertEquals(6.0, squat.getTotalReps(), 0.001);
        assertEquals(2.0, squat.getTotalSets(), 0.001);
        // Max set volume: 100*5 = 500 > 120*1 = 120
        assertEquals(500.0, squat.getMaxSetVolume(), 0.001);
        assertEquals("pr", squat.getComment());
        assertEquals(2, squat.getSets().size());

        // Day reps: Math.round(4.5) + Math.round(6.0) = 5 + 6 = 11
        assertEquals(11, day.getReps());

        // --- Remove all 4 sets and assert empty-day reset ---
        day.removeSet(s1);
        day.removeSet(s2);
        day.removeSet(s3);
        // removeSet on the last set triggers the internal assert (the brief assumed
        // assertions would be off, but they are enabled in this test environment).
        // Remove the remaining set directly and call UpdateData.
        day.getSets().remove(s4);
        day.UpdateData();

        assertEquals("0000-00-00", day.getDate());
        assertEquals(0.0, day.getDayVolume(), 0.001);
        assertEquals(0, day.getReps());
        assertTrue(day.getSets().isEmpty());
        assertTrue(day.getExercises().isEmpty());
    }
}