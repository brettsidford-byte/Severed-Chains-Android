#version 320 es

layout(std140) uniform transforms
{
    mat4 camera;
    mat4 projection;
} _19;

layout(std140) uniform transforms2
{
    mat4 model;
} _29;

layout(location = 0) in vec3 inPos;
layout(location = 0) out vec2 vertUv;
layout(location = 1) in vec2 inUv;

void main()
{
    gl_Position = ((_19.projection * _19.camera) * _29.model) * vec4(inPos, 1.0);
    vertUv = inUv;
}
