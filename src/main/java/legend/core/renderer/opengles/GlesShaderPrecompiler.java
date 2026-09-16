package legend.core.renderer.opengles;

import legend.core.memory.types.IntRef;
import legend.core.renderer.ShaderStage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Host build tool that turns the desktop GLSL sources into device-ready GLSL ES. */
public final class GlesShaderPrecompiler {
  private GlesShaderPrecompiler() { }

  public static void main(final String[] args) throws IOException {
    if(args.length != 2) throw new IllegalArgumentException("Expected input and output directories");
    final Path input = Path.of(args[0]);
    final Path output = Path.of(args[1]);
    Files.createDirectories(output);
    try(var files = Files.list(input)) {
      for(final Path source : files.filter(Files::isRegularFile).toList()) {
        final ShaderStage stage = stage(source.getFileName().toString());
        final String gles = ShaderTranspiler.transpile(Files.readString(source), stage, new IntRef());
        Files.writeString(output.resolve(source.getFileName()), gles);
      }
    }
  }

  private static ShaderStage stage(final String name) {
    if(name.endsWith(".vsh")) return ShaderStage.VERTEX;
    if(name.endsWith(".gsh")) return ShaderStage.GEOMETRY;
    if(name.endsWith(".fsh")) return ShaderStage.FRAGMENT;
    throw new IllegalArgumentException("Unknown shader stage: " + name);
  }
}
