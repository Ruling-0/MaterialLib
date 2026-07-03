package com.ruling_0.materiallib.api;

import cpw.mods.fml.common.eventhandler.Event;

/// The registration window for the material and shape registries, fired once on `MinecraftForge.EVENT_BUS`
/// during MaterialLib's preInit.
///
/// Mods register all their content -- shapes, materials, families, edits, and shape consumers -- through
/// [MaterialLibAPI] inside a handler for this event. Subscribe the handler during `FMLConstructionEvent`
/// (every mod constructs before any mod's preInit, so a construction-time subscription always precedes the
/// event) and declare `required-after:materiallib`. Once every handler has returned, MaterialLib resolves the
/// registries and registers the backing items, blocks, and fluids, still within its preInit. Handlers run in
/// bus subscription order, so a handler must not read what other mods registered; cross-mod composition goes
/// through name unification, [MaterialEdit]s, and shape consumers instead.
public class MaterialRegistrationEvent extends Event {}
