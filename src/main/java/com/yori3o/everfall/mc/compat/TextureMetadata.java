package com.yori3o.everfall.mc.compat;

import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;

/** Header-only texture inspection; never runs in the frame loop. */
public final class TextureMetadata {
    public record Dimensions(int width, int height) {}

    private TextureMetadata() {}

    public static Dimensions read(String id) throws IOException {
        var minecraft = Minecraft.getInstance();
        return read(id, minecraft == null ? null : minecraft.getResourceManager());
    }

    public static Dimensions read(String id, ResourceManager resources) throws IOException {
        var identifier = Ids.parse(id);
        if (resources != null) {
            var resource = resources.getResource(identifier);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open()) {
                    return read(stream);
                }
            }
        }
        // Loader initialization can precede the first resource-pack reload.
        String path = "/assets/" + identifier.getNamespace() + "/" + identifier.getPath();
        try (InputStream stream = TextureMetadata.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing particle texture: " + id);
            return read(stream);
        }
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
