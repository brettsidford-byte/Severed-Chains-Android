#version 330 core
layout (triangles_adjacency) in;
layout (triangle_strip, max_vertices = 3) out;

in vec2 tmdUvIn[];
flat in float tmdControlAIn[];
flat in float tmdControlBIn[];
in vec4 tmdColourIn[];
in vec3 tmdDepthIn[];

out vec2 tmdUv;
flat out float tmdControlA;
flat out float tmdControlB;
out vec4 tmdColour;
out vec2 tmdDepth;

layout(std140) uniform projectionInfo {
  float znear;
  /** PS1 projection plane distance (H) */
  float zfar;
  float zdiffInv;
  /** 0: ortho, 1: PS1 perspective, 2: modern perspective */
  float projectionMode;
};

uniform int usePs1Depth;

void emit(int, float);

void main() {
  // If any vertex is too close to the camera, cull the whole face
  if(zfar >= tmdDepthIn[0].x * 2.0) {
    return;
  }

  if(zfar >= tmdDepthIn[2].x * 2.0) {
    return;
  }

  if(zfar >= tmdDepthIn[4].x * 2.0) {
    return;
  }

  float avgDepth = gl_in[0].gl_Position.z + gl_in[2].gl_Position.z + gl_in[4].gl_Position.z;
  int vertexCount = 3;

  // Quad, check adjacent vertex
  if((int(tmdControlAIn[0]) & 0x10) != 0) {
    if(zfar >= tmdDepthIn[1].x * 2.0) {
      return;
    }

    avgDepth += gl_in[1].gl_Position.z;
    vertexCount++;
  }

  avgDepth /= vertexCount;

  emit(0, avgDepth);
  emit(2, avgDepth);
  emit(4, avgDepth);
  EndPrimitive();
}

void emit(int i, float depth) {
  tmdUv = tmdUvIn[i];
  tmdControlA = tmdControlAIn[i];
  tmdControlB = tmdControlBIn[i];
  tmdColour = tmdColourIn[i];
  tmdDepth = tmdDepthIn[i].yz;

  gl_Position = gl_in[i].gl_Position;

  if(usePs1Depth != 0) {
    gl_Position.z = depth;
  }

  EmitVertex();
}
