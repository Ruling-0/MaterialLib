package com.ruling_0.materiallib;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean registerExamples;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        registerExamples = configuration.getBoolean(
            "registerExamples",
            Configuration.CATEGORY_GENERAL,
            false,
            "Register the example content: materials TestIron and TestGold with item, block, fluid, and " +
                "fluid-container shapes, a Test family, and a gear crafting recipe. A demonstration of the " +
                "MaterialLib API, intended for development.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
