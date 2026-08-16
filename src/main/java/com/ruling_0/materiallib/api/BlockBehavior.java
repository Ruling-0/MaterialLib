package com.ruling_0.materiallib.api;

/// The behavior hooks a block shape may override; see [BlockShapeBuilder#drops], [BlockShapeBuilder#hardness],
/// [BlockShapeBuilder#resistance], [BlockShapeBuilder#harvestLevel], and [BlockShapeBuilder#harvestTool]. Each
/// hook is optional -- a null hook preserves [ShapeBlock]'s default vanilla behavior for that property.
record BlockBehavior(BlockDropFunction drops, BlockFloatFunction hardness, BlockFloatFunction resistance,
                     BlockHarvestLevelFunction harvestLevel, String harvestTool) {

    static final BlockBehavior NONE = new BlockBehavior(null, null, null, null, null);
}
