package com.ruling_0.materiallib.api;

import java.util.List;

final class TestShape implements Shape {

    private final String modid;
    private final String name;
    private final List<String> oreDicts;
    private final List<String> variants;

    TestShape(String modid, String name) {
        this(modid, name, name);
    }

    TestShape(String modid, String name, String... oreDicts) {
        this(modid, name, List.of(), oreDicts);
    }

    TestShape(String modid, String name, List<String> variants, String... oreDicts) {
        this.modid = modid;
        this.name = name;
        this.oreDicts = List.of(oreDicts);
        this.variants = variants;
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public List<String> getOreDicts() { return oreDicts; }

    @Override
    public List<String> getVariants() { return variants; }

    @Override
    public String toString() {
        return "TestShape[" + modid + ":" + name + "]";
    }
}
