package com.gpufast.recorder.video.renderer;

/** Simplest possible GL shader that just draws frames as opaque quads. */
public class GlRectDrawer extends GlGenericDrawer {
  private static final String FRAGMENT_SHADER = "void main() {\n"
      + "  gl_FragColor = sample(tc);\n"
      + "}\n";

  private static class ShaderCallbacks implements GlGenericDrawer.ShaderCallbacks {
    @Override
    public void onNewShader(GlShader shader) {}

    @Override
    public void onPrepareShader(GlShader shader, float[] texMatrix, int frameWidth, int frameHeight,
        int viewportWidth, int viewportHeight) {}
  }

  public GlRectDrawer() {
    super(FRAGMENT_SHADER, new ShaderCallbacks());
  }
}
