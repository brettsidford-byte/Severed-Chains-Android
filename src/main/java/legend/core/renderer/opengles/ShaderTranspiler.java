package legend.core.renderer.opengles;

import legend.core.memory.types.IntRef;
import legend.core.renderer.ShaderStage;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.spvc.SpvcReflectedResource;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.util.spvc.Spv.SpvDecorationBinding;
import static org.lwjgl.util.spvc.Spv.SpvDecorationLocation;
import static org.lwjgl.util.spvc.Spvc.*;

/** Desktop-native GLSL to GLSL ES transpilation used only by the LWJGL GLES backend. */
public final class ShaderTranspiler {
  private ShaderTranspiler() { }

  public static String transpile(final String source, final ShaderStage shaderStage, final IntRef uniformIndex) {
    return decompileSpirvToGles(compileShaderToSpirv(source, shaderStage), uniformIndex);
  }

  private static ByteBuffer compileShaderToSpirv(final String source, final ShaderStage shaderStage) {
    final long compiler = shaderc_compiler_initialize();
    final long options = shaderc_compile_options_initialize();
    shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5);
    shaderc_compile_options_set_auto_map_locations(options, true);
    shaderc_compile_options_set_auto_bind_uniforms(options, true);

    final int stage = switch(shaderStage) {
      case VERTEX -> shaderc_vertex_shader;
      case GEOMETRY -> shaderc_geometry_shader;
      case FRAGMENT -> shaderc_fragment_shader;
    };
    final long result = shaderc_compile_into_spv(compiler, source, stage, "shader.glsl", "main", options);
    if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
      throw new RuntimeException("Shaderc compilation failed: " + shaderc_result_get_error_message(result));
    }
    final ByteBuffer nativeBuf = shaderc_result_get_bytes(result);
    final ByteBuffer copy = ByteBuffer.allocateDirect(nativeBuf.remaining());
    copy.put(nativeBuf).flip();
    shaderc_result_release(result);
    shaderc_compile_options_release(options);
    shaderc_compiler_release(compiler);
    return copy;
  }

  private static String decompileSpirvToGles(final ByteBuffer spirvBuffer, final IntRef uniformIndex) {
    try(final MemoryStack stack = MemoryStack.stackPush()) {
      final IntBuffer spirvInts = spirvBuffer.asIntBuffer();
      final PointerBuffer contextBuffer = stack.mallocPointer(1);
      final PointerBuffer parsedIrBuffer = stack.mallocPointer(1);
      final PointerBuffer compilerBuffer = stack.mallocPointer(1);
      spvc_context_create(contextBuffer);
      final long context = contextBuffer.get(0);
      spvc_context_parse_spirv(context, spirvInts, spirvInts.remaining(), parsedIrBuffer);
      final long parsedIr = parsedIrBuffer.get(0);
      spvc_context_create_compiler(context, SPVC_BACKEND_GLSL, parsedIr, SPVC_CAPTURE_MODE_TAKE_OWNERSHIP, compilerBuffer);
      final long compiler = compilerBuffer.get(0);
      final PointerBuffer optionsBuffer = stack.mallocPointer(1);
      spvc_compiler_create_compiler_options(compiler, optionsBuffer);
      final long options = optionsBuffer.get(0);
      spvc_compiler_options_set_bool(options, SPVC_COMPILER_OPTION_GLSL_ES, true);
      spvc_compiler_options_set_uint(options, SPVC_COMPILER_OPTION_GLSL_VERSION, 320);
      spvc_compiler_install_compiler_options(compiler, options);

      final PointerBuffer resourcesBuffer = stack.mallocPointer(1);
      Spvc.spvc_compiler_create_shader_resources(compiler, resourcesBuffer);
      final long resources = resourcesBuffer.get(0);
      final PointerBuffer resourceListBuffer = stack.mallocPointer(1);
      final PointerBuffer resourceCountBuffer = stack.mallocPointer(1);
      spvc_resources_get_resource_list_for_type(resources, SPVC_RESOURCE_TYPE_GL_PLAIN_UNIFORM, resourceListBuffer, resourceCountBuffer);
      removeBindings(compiler, resourceListBuffer.get(0), resourceCountBuffer.get(0), SpvDecorationLocation, uniformIndex);
      spvc_resources_get_resource_list_for_type(resources, SPVC_RESOURCE_TYPE_UNIFORM_BUFFER, resourceListBuffer, resourceCountBuffer);
      removeBindings(compiler, resourceListBuffer.get(0), resourceCountBuffer.get(0), SpvDecorationBinding, uniformIndex);
      final PointerBuffer sourceBuffer = stack.mallocPointer(1);
      spvc_compiler_compile(compiler, sourceBuffer);
      final String result = sourceBuffer.getStringUTF8(0);
      spvc_context_destroy(context);
      return result;
    }
  }

  private static void removeBindings(final long compiler, final long list, final long count,
                                     final int decoration, final IntRef uniformIndex) {
    for(int i = 0; i < count; i++) {
      final SpvcReflectedResource resource = SpvcReflectedResource.create(list + i * SpvcReflectedResource.SIZEOF);
      spvc_compiler_unset_decoration(compiler, resource.id(), decoration);
      uniformIndex.incr();
    }
  }
}
