#version 320 es
layout(triangles_adjacency) in;
layout(max_vertices = 3, triangle_strip) out;

layout(std140) uniform projectionInfo
{
    float znear;
    float zfar;
    float zdiffInv;
    float projectionMode;
} _17;

uniform int usePs1Depth;

layout(location = 4) in vec3 tmdDepthIn[6];
layout(location = 1) flat in float tmdControlAIn[6];
layout(location = 0) out vec2 tmdUv;
layout(location = 0) in vec2 tmdUvIn[6];
layout(location = 1) flat out float tmdControlA;
layout(location = 2) flat out float tmdControlB;
layout(location = 2) flat in float tmdControlBIn[6];
layout(location = 3) out vec4 tmdColour;
layout(location = 3) in vec4 tmdColourIn[6];
layout(location = 4) out vec2 tmdDepth;

void emit(int i, float depth)
{
    tmdUv = tmdUvIn[i];
    tmdControlA = tmdControlAIn[i];
    tmdControlB = tmdControlBIn[i];
    tmdColour = tmdColourIn[i];
    tmdDepth = tmdDepthIn[i].yz;
    gl_Position = gl_in[i].gl_Position;
    if (usePs1Depth != 0)
    {
        gl_Position.z = depth;
    }
    EmitVertex();
}
void main()
{
    if (_17.zfar >= (tmdDepthIn[0].x * 2.0))
    {
        return;
    }
    if (_17.zfar >= (tmdDepthIn[2].x * 2.0))
    {
        return;
    }
    if (_17.zfar >= (tmdDepthIn[4].x * 2.0))
    {
        return;
    }
    float avgDepth = (gl_in[0].gl_Position.z + gl_in[2].gl_Position.z) + gl_in[4].gl_Position.z;
    int vertexCount = 3;
    if ((int(tmdControlAIn[0]) & 16) != 0)
    {
        if (_17.zfar >= (tmdDepthIn[1].x * 2.0))
        {
            return;
        }
        avgDepth += gl_in[1].gl_Position.z;
        vertexCount++;
    }
    avgDepth /= float(vertexCount);
    int param = 0;
    float param_1 = avgDepth;
    emit(param, param_1);
    int param_2 = 2;
    float param_3 = avgDepth;
    emit(param_2, param_3);
    int param_4 = 4;
    float param_5 = avgDepth;
    emit(param_4, param_5);
    EndPrimitive();
}
