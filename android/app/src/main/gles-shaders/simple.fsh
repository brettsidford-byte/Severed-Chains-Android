#version 320 es
precision mediump float;
precision highp int;

layout(location = 0, binding = 0) uniform highp sampler2D tex;
uniform highp vec2 shiftUv;
uniform highp vec4 recolour;

layout(location = 0) out highp vec4 outColour;
layout(location = 0) in highp vec2 vertUv;

void main()
{
    outColour = texture(tex, vertUv + shiftUv) * recolour;
}
