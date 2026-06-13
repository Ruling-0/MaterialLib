package com.ruling_0.materiallib.api;

final class TestShape implements Shape {

    private final String modid;
    private final String name;
    private final String oreDict;

    TestShape(String modid, String name) {
        this(modid, name, name);
    }

    TestShape(String modid, String name, String oreDict) {
        this.modid = modid;
        this.name = name;
        this.oreDict = oreDict;
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public String getOreDict() { return oreDict; }

    @Override
    public String toString() {
        return "TestShape[" + modid + ":" + name + "]";
    }
}
