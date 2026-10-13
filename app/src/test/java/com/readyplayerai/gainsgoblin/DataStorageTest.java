package com.readyplayerai.gainsgoblin;

import com.readyplayerai.gainsgoblin.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class DataStorageTest {

    @Test
    public void initKnownExercises_hasEntries() {
        DataStorage ds = new DataStorage();
        ds.initKnownExercises();
        assertTrue(ds.getKnownExercises().size() > 30);
        Exercise first = ds.getKnownExercises().get(0);
        assertEquals("Flat Barbell Bench Press", first.getName());
        assertEquals("Chest", first.getBodyPart());
        // Every exercise from initKnownExercises has favorite = Boolean.FALSE
        assertFalse(first.getFavorite());
        for (Exercise e : ds.getKnownExercises()) {
            assertFalse(e.getFavorite());
        }
    }

    @Test
    public void csvToSets_convertsRows() {
        DataStorage ds = new DataStorage();

        // Build the raw list matching the CSV column order:
        // Date, Exercise, Category, Weight (kg), Reps, Comment
        List csvList = new ArrayList();
        csvList.add(new String[]{"Date", "Exercise", "Category", "Weight (kg)", "Reps", "Comment"});
        // 5-column row (no comment)
        csvList.add(new String[]{"2026-01-05", "Bench Press", "Chest", "80.0", "5"});
        // 6-column row (with comment)
        csvList.add(new String[]{"2026-01-05", "Squat", "Legs", "100.0", "5", "tough"});

        ds.csvToSets(csvList);

        assertEquals(2, ds.getSets().size());

        // First row: 5 columns -> comment defaults to ""
        WorkoutSet first = ds.getSets().get(0);
        assertEquals("2026-01-05", first.getDate());
        assertEquals("Bench Press", first.getExerciseName());
        assertEquals("Chest", first.getCategory());
        assertEquals(5.0, first.getReps(), 0.001);
        assertEquals(80.0, first.getWeight(), 0.001);
        assertEquals("", first.getComment());

        // Second row: 6 columns -> comment is parsed
        WorkoutSet second = ds.getSets().get(1);
        assertEquals("2026-01-05", second.getDate());
        assertEquals("Squat", second.getExerciseName());
        assertEquals("Legs", second.getCategory());
        assertEquals(5.0, second.getReps(), 0.001);
        assertEquals(100.0, second.getWeight(), 0.001);
        assertEquals("tough", second.getComment());
    }

    @Test
    public void setsToEverything_groupsByDate() {
        DataStorage ds = new DataStorage();
        ds.getSets().add(new WorkoutSet("2026-01-04", "Bench", "Chest", 5.0, 80.0));
        ds.getSets().add(new WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0));
        ds.getSets().add(new WorkoutSet("2026-01-05", "Bench", "Chest", 3.0, 80.0));

        ds.setsToEverything();

        // days is a TreeSet with both dates in ascending order
        assertEquals(2, ds.getDays().size());
        assertTrue(ds.getDays().contains("2026-01-04"));
        assertTrue(ds.getDays().contains("2026-01-05"));

        // workoutDays should have 2 entries
        assertEquals(2, ds.getWorkoutDays().size());

        // First day (2026-01-04) has only Bench
        WorkoutDay day0 = ds.getWorkoutDays().get(0);
        assertEquals("2026-01-04", day0.getDate());
        assertEquals(1, day0.getSets().size());
        assertEquals("Bench", day0.getSets().get(0).getExerciseName());

        // Second day (2026-01-05) has Squat and Bench
        WorkoutDay day1 = ds.getWorkoutDays().get(1);
        assertEquals("2026-01-05", day1.getDate());
        assertEquals(2, day1.getSets().size());

        // Each day groups only its own date's sets
        for (WorkoutDay day : ds.getWorkoutDays()) {
            for (WorkoutSet s : day.getSets()) {
                assertEquals(day.getDate(), s.getDate());
            }
        }
    }

    @Test
    public void getDayPosition_foundAndNotFound() {
        DataStorage ds = new DataStorage();
        ds.getSets().add(new WorkoutSet("2026-01-04", "Bench", "Chest", 5.0, 80.0));
        ds.getSets().add(new WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0));
        ds.setsToEverything();

        assertEquals(0, ds.getDayPosition("2026-01-04"));
        assertEquals(1, ds.getDayPosition("2026-01-05"));
        assertEquals(-1, ds.getDayPosition("2026-01-06"));
    }

    @Test
    public void getExercisePosition_foundAndNotFound() {
        DataStorage ds = new DataStorage();
        ds.getSets().add(new WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0));
        ds.getSets().add(new WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0));
        ds.setsToEverything();

        assertEquals(0, ds.getExercisePosition("2026-01-05", "Bench"));
        assertEquals(1, ds.getExercisePosition("2026-01-05", "Squat"));
        assertEquals(-1, ds.getExercisePosition("2026-01-05", "NonExistent"));
        assertEquals(-1, ds.getExercisePosition("2026-01-06", "Bench"));
    }

    @Test
    public void modifySets_truncatesTimestamp() {
        DataStorage ds = new DataStorage();
        WorkoutSet s = new WorkoutSet("2026-01-05T21:32:00", "Bench", "Chest", 5.0, 80.0, "");
        ds.getSets().add(s);
        ds.modifySets();
        assertEquals("2026-01-05", s.getDate());
    }

    @Test
    public void csvToKnownExercises_addsNewAndSkipsExisting() {
        DataStorage ds = new DataStorage();
        ds.initKnownExercises();
        int baselineSize = ds.getKnownExercises().size();

        // Add a day with an exercise NOT in the hardcoded list and one that IS
        WorkoutSet s1 = new WorkoutSet("2026-01-05", "My Custom Row", "Back", 10.0, 80.0);
        WorkoutSet s2 = new WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0);
        ds.getSets().add(s1);
        ds.getSets().add(s2);
        ds.setsToEverything();

        ds.csvToKnownExercises();

        // Should have exactly one more exercise than baseline (My Custom Row added, Deadlift already present)
        assertEquals(baselineSize + 1, ds.getKnownExercises().size());

        // "My Custom Row" should be present exactly once
        int customCount = 0;
        for (Exercise e : ds.getKnownExercises()) {
            if (e.getName().equals("My Custom Row")) {
                customCount++;
                assertEquals("Back", e.getBodyPart());
            }
        }
        assertEquals(1, customCount);
    }

    @Test
    public void setFavoriteExercise_and_isExerciseFavorite() {
        DataStorage ds = new DataStorage();
        ds.initKnownExercises();

        // Set favorite to true
        ds.setFavoriteExercise("Deadlift", true);
        assertTrue(ds.isExerciseFavorite("Deadlift"));

        // Set favorite back to false
        ds.setFavoriteExercise("Deadlift", false);
        assertFalse(ds.isExerciseFavorite("Deadlift"));

        // Unknown exercise returns false
        assertFalse(ds.isExerciseFavorite("NonExistent"));
    }

    @Test
    public void doesExerciseExist_knownAndUnknown() {
        DataStorage ds = new DataStorage();
        ds.initKnownExercises();

        assertTrue(ds.doesExerciseExist("Deadlift"));
        assertFalse(ds.doesExerciseExist("NonExistent"));
    }

    @Test
    public void getExerciseCategory_knownAndUnknown() {
        DataStorage ds = new DataStorage();
        ds.initKnownExercises();

        assertEquals("Back", ds.getExerciseCategory("Deadlift"));
        assertEquals("", ds.getExerciseCategory("NonExistent"));
    }

    @Test
    public void deleteExercise_onlyExercise_removesDay() {
        DataStorage ds = new DataStorage();
        WorkoutSet s = new WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0);
        ds.getSets().add(s);
        ds.initKnownExercises();
        ds.setsToEverything();

        assertFalse(ds.getWorkoutDays().isEmpty());

        ds.deleteExercise("Deadlift");

        // Day had only Deadlift sets -> day is removed
        assertTrue(ds.getWorkoutDays().isEmpty());
        // Deadlift removed from knownExercises
        assertFalse(ds.doesExerciseExist("Deadlift"));
    }

    @Test
    public void deleteExercise_twoExercises_keepsDay() {
        DataStorage ds = new DataStorage();
        WorkoutSet s1 = new WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0);
        WorkoutSet s2 = new WorkoutSet("2026-01-05", "Bench Press", "Chest", 5.0, 80.0);
        ds.getSets().add(s1);
        ds.getSets().add(s2);
        ds.initKnownExercises();
        ds.setsToEverything();
        // Add "Bench Press" to knownExercises (it's not in the hardcoded list)
        ds.csvToKnownExercises();

        assertEquals(1, ds.getWorkoutDays().size());

        ds.deleteExercise("Deadlift");

        // Day survives with only Bench Press
        assertEquals(1, ds.getWorkoutDays().size());
        assertEquals(1, ds.getWorkoutDays().get(0).getExercises().size());
        assertEquals("Bench Press", ds.getWorkoutDays().get(0).getExercises().get(0).getExercise());

        // All remaining sets belong to Bench Press
        for (WorkoutSet set : ds.getWorkoutDays().get(0).getSets()) {
            assertEquals("Bench Press", set.getExerciseName());
        }

        // Deadlift removed from knownExercises
        assertFalse(ds.doesExerciseExist("Deadlift"));
    }

    @Test
    public void deleteExerciseGetSets_returnsMatchingSets() {
        DataStorage ds = new DataStorage();
        WorkoutSet s1 = new WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0);
        WorkoutSet s2 = new WorkoutSet("2026-01-05", "Bench Press", "Chest", 5.0, 80.0);
        ds.getSets().add(s1);
        ds.getSets().add(s2);
        ds.initKnownExercises();
        ds.setsToEverything();

        List<WorkoutSet> deadliftSets = ds.deleteExerciseGetSets("Deadlift");
        assertEquals(1, deadliftSets.size());
        assertEquals("Deadlift", deadliftSets.get(0).getExerciseName());

        List<WorkoutSet> benchSets = ds.deleteExerciseGetSets("Bench Press");
        assertEquals(1, benchSets.size());
        assertEquals("Bench Press", benchSets.get(0).getExerciseName());

        List<WorkoutSet> nonexistent = ds.deleteExerciseGetSets("NonExistent");
        assertTrue(nonexistent.isEmpty());
    }

    @Test
    public void editExercise_updatesNamesAndCategories() {
        DataStorage ds = new DataStorage();
        ds.initKnownExercises();

        WorkoutSet s = new WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0);
        ds.getSets().add(s);
        ds.setsToEverything();

        ds.editExercise("Deadlift", "Deadlift New", "Upper Back");

        // knownExercises updated
        assertFalse(ds.doesExerciseExist("Deadlift"));
        assertTrue(ds.doesExerciseExist("Deadlift New"));
        assertEquals("Upper Back", ds.getExerciseCategory("Deadlift New"));

        // Top-level day sets updated
        for (WorkoutSet set : ds.getWorkoutDays().get(0).getSets()) {
            assertEquals("Deadlift New", set.getExerciseName());
            assertEquals("Upper Back", set.getCategory());
        }

        // Nested WorkoutExercise entries and their sets updated
        for (WorkoutExercise ex : ds.getWorkoutDays().get(0).getExercises()) {
            assertEquals("Deadlift New", ex.getExercise());
            for (WorkoutSet set : ex.getSets()) {
                assertEquals("Deadlift New", set.getExerciseName());
                assertEquals("Upper Back", set.getCategory());
            }
        }
    }

    @Test
    public void editExerciseGetSets_returnsUpdatedSets() {
        DataStorage ds = new DataStorage();
        ds.initKnownExercises();

        WorkoutSet s = new WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0);
        ds.getSets().add(s);
        ds.setsToEverything();

        List<WorkoutSet> updatedSets = ds.editExerciseGetSets("Deadlift", "Deadlift New", "Upper Back");

        for (WorkoutSet set : updatedSets) {
            assertEquals("Deadlift New", set.getExerciseName());
            assertEquals("Upper Back", set.getCategory());
        }
    }
}