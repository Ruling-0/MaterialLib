package com.ruling_0.materiallib;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean registerExamples;
    public static boolean renderEmissiveLayers;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        registerExamples = configuration.getBoolean(
            "registerExamples",
            Configuration.CATEGORY_GENERAL,
            false,
            "Register example content, for demonstrating design functionality.");
        renderEmissiveLayers = configuration.getBoolean(
            "renderEmissiveLayers",
            "client",
            true,
            "Draw glowing material layers, such as glowing ore overlays. When false those layers are not drawn.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
