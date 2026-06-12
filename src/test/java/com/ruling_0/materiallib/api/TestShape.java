package com.ruling_0.materiallib.api;

final class TestShape implements Shape {

    private final String modid;
    private final String name;

    TestShape(String modid, String name) {
        this.modid = modid;
        this.name = name;
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public String getOreDict() { return name; }

    @Override
    public String toString() {
        return "TestShape[" + modid + ":" + name + "]";
    }
}
