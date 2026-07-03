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
            "Register example content, for demonstrating design functionality.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
