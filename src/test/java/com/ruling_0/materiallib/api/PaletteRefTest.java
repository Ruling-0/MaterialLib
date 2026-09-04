package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/// Pins what [PaletteRef] accepts and the sprite name it derives from a resolved texture path.
class PaletteRefTest {

    @Test
    void aNegativeColumnIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("testmod", "metals", -1));
    }

    /// The modid and name take the identifier rules: non-null, non-empty, free of ':' and whitespace.
    @Test
    void anInvalidModidOrNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef(null, "metals", 0));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("", "metals", 0));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("test:mod", "metals", 0));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("testmod", null, 0));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("testmod", "", 0));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("testmod", "hot metals", 0));
    }

    /// The resolved path's ':' becomes a '/', leaving the baked name the one colon a ResourceLocation allows.
    @Test
    void theBakedNameFoldsTheResolvedPathIntoOneResourceLocation() {
        PaletteRef palette = new PaletteRef("testmod", "metals", 4);

        assertEquals("materiallib:mlbaked/examplemod/materials/dull/gear/testmod/metals/4",
            palette.bakedIconName("examplemod:materials/dull/gear"));
    }
}
