#version 320 es
precision mediump float;
precision highp int;

layout(location = 0, binding = 0) uniform highp sampler2D tex;

layout(location = 0) out highp vec4 outColour;
layout(location = 0) in highp vec2 vertUv;

void main()
{
    outColour = texture(tex, vertUv);
    outColour.w = 1.0;
}
