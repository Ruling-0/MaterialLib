package com.ruling_0.materiallib.api;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

/// An [IIconRegister] that hands out a stub icon per path and remembers what was registered, so icon binding can
/// be exercised without a live client. Icon paths dedupe the way a real
/// [net.minecraft.client.renderer.texture.TextureMap] does, so a repeated path returns the same icon.
final class RecordingRegister implements IIconRegister {

    final Map<String, IIcon> registered = new HashMap<>();

    @Override
    public IIcon registerIcon(String path) {
        return registered.computeIfAbsent(path, FakeIcon::new);
    }

    private record FakeIcon(String name) implements IIcon {

        @Override
        public int getIconWidth() { return 16; }

        @Override
        public int getIconHeight() { return 16; }

        @Override
        public float getMinU() { return 0; }

        @Override
        public float getMaxU() { return 1; }

        @Override
        public float getInterpolatedU(double u) {
            return 0;
        }

        @Override
        public float getMinV() { return 0; }

        @Override
        public float getMaxV() { return 1; }

        @Override
        public float getInterpolatedV(double v) {
            return 0;
        }

        @Override
        public String getIconName() { return name; }
    }
}
