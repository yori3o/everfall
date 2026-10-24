package com.yori3o.everfall.mc.compat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.yori3o.everfall.core.profile.ParticleProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;

/** Texture dimensions and per-frame opaque row bounds; never runs in the frame loop. */
public final class TextureMetadata {

    public record Dimensions(int width, int height) {}

    public record Frames(int width, int height, List<ParticleProfile.FrameBounds> bounds) {}

    public static Frames readFrames(String id, int frameCount, ResourceManager resources) throws IOException {
        try (InputStream stream = open(id, resources)) {
            BufferedImage image = ImageIO.read(stream);

            if (image == null) throw new IOException("Unsupported particle image format");

            if (image.getHeight() % frameCount != 0) throw new IOException(
                    "Image height " + image.getHeight() + " is not divisible by frameCount " + frameCount);
            int height = image.getHeight() / frameCount;
            List<ParticleProfile.FrameBounds> bounds = new ArrayList<>(frameCount);

            for (int frame = 0; frame < frameCount; frame++) {
                int top = height, bottom = 0;

                for (int row = 0; row < height; row++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        if ((image.getRGB(x, frame * height + row) >>> 24) != 0) {
                            top = Math.min(top, row);
                            bottom = row + 1;
                            break;
                        }
                    }
                }

                bounds.add(new ParticleProfile.FrameBounds(bottom == 0 ? 0 : top, bottom));
            }

            return new Frames(image.getWidth(), height, List.copyOf(bounds));
        }
    }

    private TextureMetadata() {
    }

    public static Dimensions read(String id) throws IOException {
        var minecraft = Minecraft.getInstance();
        return read(id, minecraft == null ? null : minecraft.getResourceManager());
    }

    public static Dimensions read(String id, ResourceManager resources) throws IOException {
        try (InputStream stream = open(id, resources)) {
            return read(stream);
        }
    }

    private static InputStream open(String id, ResourceManager resources) throws IOException {
        var identifier = Ids.parse(id);

        if (resources == null) {
            var minecraft = Minecraft.getInstance();

            if (minecraft != null) resources = minecraft.getResourceManager();
        }

        if (resources != null) {
            var resource = resources.getResource(identifier);

            if (resource.isPresent()) return resource.get().open();
        }

        String path = "/assets/" + identifier.getNamespace() + "/" + identifier.getPath();
        InputStream stream = TextureMetadata.class.getResourceAsStream(path);

        if (stream == null) throw new IOException("Missing particle texture: " + id);
        return stream;
    }

    public static Dimensions read(InputStream stream) throws IOException {
        try (var input = new MemoryCacheImageInputStream(stream)) {
            var readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) throw new IOException("Unsupported particle image format");
            ImageReader reader = readers.next();

            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0) throw new IOException("Invalid particle image dimensions");
                return new Dimensions(width, height);
            } finally {
                reader.dispose();
            }
        }
    }
}
