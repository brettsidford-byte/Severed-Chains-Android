#version 320 es

layout(location = 0) in vec2 inPos;
layout(location = 0) out vec2 vertUv;
layout(location = 1) in vec2 inUv;

void main()
{
    gl_Position = vec4(inPos.x, inPos.y, 0.0, 1.0);
    vertUv = inUv;
}
