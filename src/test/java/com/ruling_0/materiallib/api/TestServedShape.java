package com.ruling_0.materiallib.api;

import java.util.List;

final class TestServedShape implements ServedShape {

    private final String modid;
    private final String name;
    private Material[] served = new Material[0];

    TestServedShape(String modid, String name) {
        this.modid = modid;
        this.name = name;
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public List<String> getOreDicts() { return List.of(); }

    @Override
    public void bindServedMaterials(Material[] materials) {
        this.served = materials;
    }

    @Override
    public Material[] getServedMaterials() { return served; }

    @Override
    public String toString() {
        return "TestServedShape[" + modid + ":" + name + "]";
    }
}
