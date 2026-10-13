package com.readyplayerai.gainsgoblin;

import com.readyplayerai.gainsgoblin.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SerializationGoldenTest {

    // ---- WorkoutSet ----

    @Test
    public void workoutSet_keySet() {
        Gson gson = new Gson();
        WorkoutSet set = new WorkoutSet("2026-01-01", "Bench", "Chest", 5.0, 80.0, "good");
        set.setId(7);
        set.setUser_id(9);

        String json = gson.toJson(set);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        Set<String> expected = new HashSet<String>(Arrays.asList(
            "id", "date", "exercise_name", "category", "reps", "weight", "comment", "user_id"
        ));
        assertEquals(expected, obj.keySet());
        assertEquals(7, obj.get("id").getAsInt());
        assertEquals(9, obj.get("user_id").getAsInt());
    }

    @Test
    public void workoutSet_partialDeserialization() {
        Gson gson = new Gson();
        String partialJson = "{\"date\":\"2026-01-01\",\"exercise_name\":\"Bench\",\"category\":\"Chest\",\"reps\":5.0,\"weight\":80.0}";
        WorkoutSet set = gson.fromJson(partialJson, WorkoutSet.class);

        assertEquals("2026-01-01", set.getDate());
        assertEquals("Bench", set.getExerciseName());
        assertEquals("Chest", set.getCategory());
        assertEquals(5.0, set.getReps(), 0.001);
        assertEquals(80.0, set.getWeight(), 0.001);
        // Missing fields: comment null, id 0, user_id 0
        assertNull(set.getComment());
        assertEquals(0, set.getId());
        assertEquals(0, set.getUser_id());
    }

    @Test
    public void workoutSet_roundTrip() {
        Gson gson = new Gson();
        WorkoutSet original = new WorkoutSet("2026-01-01", "Bench", "Chest", 5.0, 80.0, "good");
        original.setId(7);
        original.setUser_id(9);

        String json = gson.toJson(original);
        WorkoutSet round = gson.fromJson(json, WorkoutSet.class);

        assertEquals(original.getDate(), round.getDate());
        assertEquals(original.getExerciseName(), round.getExerciseName());
        assertEquals(original.getCategory(), round.getCategory());
        assertEquals(original.getReps(), round.getReps(), 0.001);
        assertEquals(original.getWeight(), round.getWeight(), 0.001);
        assertEquals(original.getComment(), round.getComment());
        assertEquals(original.getId(), round.getId());
        assertEquals(original.getUser_id(), round.getUser_id());
    }

    // ---- Exercise ----

    @Test
    public void exercise_keySet() {
        Gson gson = new Gson();
        Exercise ex = new Exercise("Deadlift", "Back");

        String json = gson.toJson(ex);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        Set<String> expected = new HashSet<String>(Arrays.asList("Name", "BodyPart", "favorite"));
        assertEquals(expected, obj.keySet());
        assertFalse(obj.get("favorite").getAsBoolean());
    }

    @Test
    public void exercise_partialDeserialization() {
        Gson gson = new Gson();
        // Legacy JSON without "favorite"
        String partialJson = "{\"Name\":\"Deadlift\",\"BodyPart\":\"Back\"}";
        Exercise ex = gson.fromJson(partialJson, Exercise.class);

        assertEquals("Deadlift", ex.getName());
        assertEquals("Back", ex.getBodyPart());
        assertNull(ex.getFavorite());
    }

    @Test
    public void exercise_roundTrip() {
        Gson gson = new Gson();
        Exercise original = new Exercise("Deadlift", "Back");
        original.setFavorite(true);

        String json = gson.toJson(original);
        Exercise round = gson.fromJson(json, Exercise.class);

        assertEquals(original.getName(), round.getName());
        assertEquals(original.getBodyPart(), round.getBodyPart());
        assertEquals(original.getFavorite(), round.getFavorite());
    }

    // ---- WorkoutDay + nested WorkoutExercise ----

    @Test
    public void workoutDay_nested_keySets() {
        Gson gson = new Gson();

        WorkoutDay day = new WorkoutDay();
        WorkoutSet s1 = new WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good");
        WorkoutSet s2 = new WorkoutSet("2026-01-05", "Bench", "Chest", 3.0, 100.0, "");
        WorkoutSet s3 = new WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0, "");
        WorkoutSet s4 = new WorkoutSet("2026-01-05", "Squat", "Legs", 2.0, 120.0, "hard");
        day.addSet(s1);
        day.addSet(s2);
        day.addSet(s3);
        day.addSet(s4);

        // Set PR flags on the WorkoutExercise entries (after UpdateData)
        for (WorkoutExercise ex : day.getExercises()) {
            ex.setVolumePR(true);
            ex.setHTLT(true);
        }
        day.getExercises().get(0).setActualOneRepMaxPR(true);
        day.getExercises().get(1).setMaxRepsPR(true);

        // Serialize as ArrayList<WorkoutDay>, mirroring production
        ArrayList<WorkoutDay> list = new ArrayList<WorkoutDay>();
        list.add(day);
        String json = gson.toJson(list);

        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        assertEquals(1, arr.size());
        JsonObject dayObj = arr.get(0).getAsJsonObject();

        // WorkoutDay top-level key set
        Set<String> dayExpected = new HashSet<String>(Arrays.asList(
            "Exercises", "Sets", "DayVolume", "Date", "Reps"
        ));
        assertEquals(dayExpected, dayObj.keySet());

        // Nested WorkoutExercise key set
        JsonArray exercises = dayObj.get("Exercises").getAsJsonArray();
        assertEquals(2, exercises.size());

        Set<String> weExpected = new HashSet<String>(Arrays.asList(
            "Sets", "Date", "Exercise", "Volume", "EstimatedOneRepMax",
            "ActualOneRepMax", "MaxWeight", "MaxReps", "TotalReps", "TotalSets",
            "MaxSetVolume", "VolumePR", "ActualOneRepMaxPR", "EstimatedOneRepMaxPR",
            "MaxRepsPR", "MaxWeightPR", "HTLT", "Comment",
            "maxVolumeSet", "maxRepsSet", "maxWeightSet"
        ));

        for (int i = 0; i < 2; i++) {
            JsonObject weObj = exercises.get(i).getAsJsonObject();
            assertEquals(weExpected, weObj.keySet());
            // PR booleans we set serialize as true
            assertTrue(weObj.get("VolumePR").getAsBoolean());
            assertTrue(weObj.get("HTLT").getAsBoolean());
        }
    }

    @Test
    public void workoutDay_roundTrip() {
        Gson gson = new Gson();

        WorkoutDay day = new WorkoutDay();
        WorkoutSet s1 = new WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good");
        WorkoutSet s2 = new WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0, "");
        day.addSet(s1);
        day.addSet(s2);

        ArrayList<WorkoutDay> originalList = new ArrayList<WorkoutDay>();
        originalList.add(day);

        // Serialize using plain Gson (no TypeToken needed for serialization)
        String json = gson.toJson(originalList);

        // Deserialize using TypeToken, exactly like production's loadWorkoutData
        Type type = new TypeToken<ArrayList<WorkoutDay>>(){}.getType();
        ArrayList<WorkoutDay> roundList = gson.fromJson(json, type);

        assertEquals(1, roundList.size());
        WorkoutDay roundDay = roundList.get(0);

        // Day-level fields
        assertEquals(day.getDate(), roundDay.getDate());
        assertEquals(day.getDayVolume(), roundDay.getDayVolume(), 0.001);
        assertEquals(day.getReps(), roundDay.getReps());
        assertEquals(day.getSets().size(), roundDay.getSets().size());
        assertEquals(day.getExercises().size(), roundDay.getExercises().size());

        // Compare sets field by field
        for (int i = 0; i < day.getSets().size(); i++) {
            WorkoutSet origSet = day.getSets().get(i);
            WorkoutSet roundSet = roundDay.getSets().get(i);
            assertEquals(origSet.getDate(), roundSet.getDate());
            assertEquals(origSet.getExerciseName(), roundSet.getExerciseName());
            assertEquals(origSet.getCategory(), roundSet.getCategory());
            assertEquals(origSet.getReps(), roundSet.getReps(), 0.001);
            assertEquals(origSet.getWeight(), roundSet.getWeight(), 0.001);
            assertEquals(origSet.getComment(), roundSet.getComment());
        }

        // Compare exercise fields field by field
        for (int i = 0; i < day.getExercises().size(); i++) {
            WorkoutExercise origEx = day.getExercises().get(i);
            WorkoutExercise roundEx = roundDay.getExercises().get(i);
            assertEquals(origEx.getExercise(), roundEx.getExercise());
            assertEquals(origEx.getDate(), roundEx.getDate());
            assertEquals(origEx.getVolume(), roundEx.getVolume(), 0.001);
            assertEquals(origEx.getEstimatedOneRepMax(), roundEx.getEstimatedOneRepMax(), 0.001);
            assertEquals(origEx.getActualOneRepMax(), roundEx.getActualOneRepMax(), 0.001);
            assertEquals(origEx.getMaxWeight(), roundEx.getMaxWeight(), 0.001);
            assertEquals(origEx.getMaxReps(), roundEx.getMaxReps(), 0.001);
            assertEquals(origEx.getTotalReps(), roundEx.getTotalReps(), 0.001);
            assertEquals(origEx.getTotalSets(), roundEx.getTotalSets(), 0.001);
            assertEquals(origEx.getMaxSetVolume(), roundEx.getMaxSetVolume(), 0.001);
            assertEquals(origEx.getComment(), roundEx.getComment());
        }
    }
}