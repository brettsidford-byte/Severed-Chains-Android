#version 320 es

uniform mat4 projection;

layout(location = 0) in vec2 inPos;
layout(location = 0) out vec2 vertUv;
layout(location = 1) in vec2 inUv;

void main()
{
    gl_Position = projection * vec4(inPos, 1.0, 1.0);
    vertUv = inUv;
}
