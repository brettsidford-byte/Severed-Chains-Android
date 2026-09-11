package legend.severedchains.android;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.util.Log;

import java.io.IOException;

/** Android binding description for Severed Chains' standard shader pair. */
public final class AndroidGlesStandardShader {
    private static final String TAG = "SeveredChains";
    private static final String[] BLOCKS = {
        "transforms", "transforms2", "lighting", "clutAnimation", "projectionInfo", "scissor"
    };

    private final int program;
    private final int[] attributes;
    private final boolean uniformBlocksReady;

    private AndroidGlesStandardShader(final int program, final int[] attributes,
                                      final boolean uniformBlocksReady) {
        this.program = program;
        this.attributes = attributes;
        this.uniformBlocksReady = uniformBlocksReady;
    }

    public static AndroidGlesStandardShader load(final AssetManager assets) throws IOException {
        final String vertex = AndroidGlesShaderSource.load(assets, "gfx/shaders/standard.vsh");
        final String fragment = AndroidGlesShaderSource.load(assets, "gfx/shaders/standard.fsh");
        final int program = AndroidGlesResources.createProgram(vertex, fragment);
        if (program == 0) {
            return new AndroidGlesStandardShader(0, new int[0], false);
        }

        final String[] names = {"inPos", "inNorm", "inUv", "inTpage", "inClut", "inColour", "inFlags"};
        final int[] attributes = new int[names.length];
        boolean attributesReady = true;
        for (int i = 0; i < names.length; i++) {
            attributes[i] = GLES30.glGetAttribLocation(program, names[i]);
            attributesReady &= attributes[i] == i;
        }

        boolean blocksReady = true;
        for (int i = 0; i < BLOCKS.length; i++) {
            blocksReady &= AndroidGlesResources.bindUniformBlock(program, BLOCKS[i], i);
        }

        final boolean ready = attributesReady && blocksReady
            && GLES30.glGetError() == GLES30.GL_NO_ERROR;
        if (!ready) {
            Log.e(TAG, "Standard shader bindings incomplete: attributes="
                + attributesReady + ", blocks=" + blocksReady);
        }
        return new AndroidGlesStandardShader(program, attributes, ready);
    }

    public boolean isReady() {
        return program != 0 && uniformBlocksReady;
    }

    public int program() {
        return program;
    }

    public int attribute(final int index) {
        return attributes[index];
    }

    public void delete() {
        AndroidGlesResources.deleteProgram(program);
    }
}
