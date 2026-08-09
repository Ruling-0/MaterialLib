package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

class ShapeFluidInContainerTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");
    private final ShapeFluid liquid = new ShapeFluid("testmod", "liquid", "Liquid %s");
    private final ShapeFluid gas = new ShapeFluid("testmod", "gas", "Gaseous %s");

    @Test
    void selectFluidPicksTheOnlyShapeThatServesTheMaterial() {
        Material iron = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        gas.bindServedMaterials(new Material[] { iron });

        assertSame(gas, ShapeFluidInContainer.selectFluid(iron, List.of(liquid, gas)));
    }

    @Test
    void selectFluidPrefersAnEarlierListedShapeOverALaterOne() {
        Material iron = registry.newMaterial("testmod", "TestIron", texture)
            .build();
        liquid.bindServedMaterials(new Material[] { iron });
        gas.bindServedMaterials(new Material[] { iron });

        assertSame(liquid, ShapeFluidInContainer.selectFluid(iron, List.of(liquid, gas)));
    }

    @Test
    void selectFluidReturnsNullWhenNoListedShapeServesTheMaterial() {
        Material iron = registry.newMaterial("testmod", "TestIron", texture)
            .build();

        assertNull(ShapeFluidInContainer.selectFluid(iron, List.of(liquid, gas)));
    }

    @Test
    void aSubclassCanHoldMultipleFluidsInFallbackOrder() {
        TestContainer cell = new TestContainer(List.of(liquid, gas));

        assertEquals(List.of(liquid, gas), cell.getFluidShapes());
    }

    @Test
    void aNonFluidShapeIsRejectedAtConstruction() {
        List<Shape> fluidShapes = List.of(liquid, new TestShape("testmod", "plate"));

        assertThrows(IllegalArgumentException.class, () -> new TestContainer(fluidShapes));
    }

    @Test
    void theHandleConstructorRejectsANullHandle() {
        assertThrows(NullPointerException.class, () -> new TestContainer(List.of(liquid), null));
    }

    private static final class TestContainer extends ShapeFluidInContainer {

        TestContainer(List<Shape> fluidShapes) {
            super("testmod", "cell", "%s Cell", fluidShapes, (ItemStack) null, 1000, "cell");
        }

        TestContainer(List<Shape> fluidShapes, EmptyContainerHandle emptyContainer) {
            super("testmod", "cell", "%s Cell", fluidShapes, emptyContainer, 1000, "cell");
        }
    }
}
