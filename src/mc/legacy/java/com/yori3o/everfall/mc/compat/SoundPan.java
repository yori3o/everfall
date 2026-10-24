package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.mc.mixin.ChannelAccessor;
import com.yori3o.everfall.mc.mixin.SoundEngineAccessor;
import com.yori3o.everfall.mc.mixin.SoundManagerAccessor;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.SOFTSourceSpatialize;
import org.slf4j.LoggerFactory;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;

/** Only touch our source, on Minecraft's sound executor after its initial channel setup. */
public final class SoundPan {

    private static boolean warned;

    private SoundPan() {
    }

    public static void apply(SoundManager manager, SoundInstance sound, float requestedPan) {
        float pan = Math.max(-0.5f, Math.min(0.5f, requestedPan));
        var engine = ((SoundManagerAccessor) manager).everfall$soundEngine();
        var handle = ((SoundEngineAccessor) engine).everfall$channels().get(sound);

        if (handle == null) return;

        handle.execute(channel -> {
            int source = ((ChannelAccessor) channel).everfall$source();

            if (AL.getCapabilities().AL_SOFT_source_spatialize) {
                // OpenAL normally leaves stereo OGG sources unpositioned. Force spatialization for these too.
                AL10.alSourcei(source, SOFTSourceSpatialize.AL_SOURCE_SPATIALIZE_SOFT, AL10.AL_TRUE);
            } else if (!warned) {
                warned = true;
                LoggerFactory.getLogger("everfall").warn(
                        "[Everfall] OpenAL stereo spatialization unavailable; stereo particle sounds may remain centered");
            }

            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            // Keep the angular spread gentle for both mono and spatialized stereo sources.
            double angle = pan * Math.PI / 12.0;
            AL10.alSource3f(source, AL10.AL_POSITION, (float) Math.sin(angle), 0.0f, -(float) Math.cos(angle));
        });
    }
}
