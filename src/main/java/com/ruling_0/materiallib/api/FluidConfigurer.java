package com.ruling_0.materiallib.api;

import net.minecraftforge.fluids.Fluid;

/// Configures the Forge [Fluid] MaterialLib registers for one material in one fluid [Shape].
///
/// Set through [FluidShapeBuilder#configureFluid], invoked once per served material at resolve, immediately after
/// that material's fluid is created and newly registered. Never invoked for a fluid name already registered by
/// another mod, which is reused unmodified. Typical uses are `setTemperature`, `setGaseous`, `setLuminosity`,
/// `setDensity`, and `setViscosity`; MaterialLib defines no properties for these, so a configurer reads whatever
/// [Property]s it needs from `material`.
@FunctionalInterface
public interface FluidConfigurer {

    /// Configures `fluid`, freshly created and registered for `material`.
    void configure(Material material, Fluid fluid);
}
