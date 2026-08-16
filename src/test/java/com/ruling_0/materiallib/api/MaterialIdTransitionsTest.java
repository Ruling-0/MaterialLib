package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaterialIdTransitionsTest {

    @TempDir
    File dir;

    @Test
    void writeThenLoadComposesTheSingleStep() {
        MaterialIdTransitions.write(dir, 1, 2, Map.of(0, 5), List.of(3));

        Int2IntMap composed = MaterialIdTransitions.load(dir).compose(1, 2);

        assertEquals(5, composed.get(0));
        assertEquals(MaterialMigration.DELETE, composed.get(3));
        assertEquals(2, composed.size());
    }

    @Test
    void composeAcrossVersionsResolvesMoveChainsAndRemovals() {
        MaterialIdTransitions.write(dir, 1, 2, Map.of(0, 1, 1, 0), List.of());
        MaterialIdTransitions.write(dir, 2, 3, Map.of(1, 2, 2, 1), List.of());
        MaterialIdTransitions.write(dir, 3, 4, Map.of(), List.of(2));

        Int2IntMap composed = MaterialIdTransitions.load(dir).compose(1, 4);

        assertEquals(MaterialMigration.DELETE, composed.get(0));
        assertEquals(0, composed.get(1));
        assertEquals(1, composed.get(2));
        assertEquals(3, composed.size());
    }

    @Test
    void composeDropsIndicesThatRoundTripBackToThemselves() {
        MaterialIdTransitions.write(dir, 1, 2, Map.of(0, 1, 1, 0), List.of());
        MaterialIdTransitions.write(dir, 2, 3, Map.of(0, 1, 1, 0), List.of());

        assertTrue(MaterialIdTransitions.load(dir).compose(1, 3).isEmpty());
    }

    @Test
    void composeOfAnEqualSpanIsEmpty() {
        assertTrue(MaterialIdTransitions.load(dir).compose(3, 3).isEmpty());
    }

    @Test
    void loadOfAMissingDirectoryGivesAnEmptyChain() {
        MaterialIdTransitions transitions = MaterialIdTransitions.load(new File(dir, "transitions"));

        assertTrue(transitions.compose(1, 1).isEmpty());
    }

    @Test
    void composeWithAMissingStepFailsLoudly() {
        MaterialIdTransitions.write(dir, 2, 3, Map.of(0, 1), List.of());
        MaterialIdTransitions transitions = MaterialIdTransitions.load(dir);

        assertThrows(IllegalStateException.class, () -> transitions.compose(1, 3));
    }

    @Test
    void composeBackwardsIsRejected() {
        MaterialIdTransitions transitions = MaterialIdTransitions.load(dir);

        assertThrows(IllegalArgumentException.class, () -> transitions.compose(2, 1));
    }

    @Test
    void loadingACorruptFileFailsLoudly() throws Exception {
        Files.write(new File(dir, "v1-to-v2.json").toPath(), "{ not valid json".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdTransitions.load(dir));
    }

    @Test
    void loadingAFileWhoseNameContradictsItsVersionsFailsLoudly() throws Exception {
        Files.write(
            new File(dir, "v1-to-v2.json").toPath(),
            "{\"from\":2,\"to\":3,\"moved\":{},\"removed\":[]}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdTransitions.load(dir));
    }

    @Test
    void loadingANonConsecutiveStepFailsLoudly() throws Exception {
        Files.write(
            new File(dir, "v1-to-v3.json").toPath(),
            "{\"from\":1,\"to\":3,\"moved\":{},\"removed\":[]}".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> MaterialIdTransitions.load(dir));
    }
}
