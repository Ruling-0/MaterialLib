package com.ruling_0.materiallib.api;

import cpw.mods.fml.common.eventhandler.Event;

/// The registration window for the material and shape registries, fired once during MaterialLib's preInit.
///
/// Mods register all their content -- shapes, materials, families, edits, and shape consumers -- through
/// [MaterialLibAPI] inside a handler for this event. Subscribe the handler during `FMLConstructionEvent`
/// and declare `required-after:materiallib`. Once every handler has returned, MaterialLib resolves the
/// registries and registers the backing items, blocks, and fluids Handler order across mods is unspecified, so a
/// handler must not read what other mods registered; use [MaterialEdit]s and [FamilyEdit]s instead.
public class MaterialRegistrationEvent extends Event {}
