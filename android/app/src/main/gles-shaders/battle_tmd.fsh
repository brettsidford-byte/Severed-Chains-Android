#version 320 es
precision mediump float;
precision highp int;

layout(std140) uniform scissor
{
    highp float scissorX;
    highp float scissorY;
    highp float scissorW;
    highp float scissorH;
} _18;

layout(std140) uniform projectionInfo
{
    highp float znear;
    highp float zfar;
    highp float zdiffInv;
    highp float projectionMode;
} _67;

uniform int ctmdFlags;
uniform int tmdTranslucency;
uniform highp vec2 uvOffset;
layout(location = 6, binding = 3) uniform highp usampler2D tex15;
layout(location = 5, binding = 2) uniform highp sampler2D tex24;
uniform highp float discardTranslucency;
uniform highp vec3 recolour;

layout(location = 4) in highp vec2 tmdDepth;
layout(location = 1) flat in highp float tmdControlA;
layout(location = 0) out highp vec4 outColour;
layout(location = 3) in highp vec4 tmdColour;
layout(location = 0) in highp vec2 tmdUv;
layout(location = 2) flat in highp float tmdControlB;

void main()
{
    bool _24 = gl_FragCoord.x < _18.scissorX;
    bool _37;
    if (!_24)
    {
        _37 = gl_FragCoord.x >= (_18.scissorX + _18.scissorW);
    }
    else
    {
        _37 = _24;
    }
    bool _48;
    if (!_37)
    {
        _48 = gl_FragCoord.y < _18.scissorY;
    }
    else
    {
        _48 = _37;
    }
    bool _61;
    if (!_48)
    {
        _61 = gl_FragCoord.y >= (_18.scissorY + _18.scissorH);
    }
    else
    {
        _61 = _48;
    }
    if (_61)
    {
        discard;
    }
    if (_67.projectionMode == 2.0)
    {
        gl_FragDepth = ((tmdDepth.x - _67.znear) * _67.zdiffInv) + tmdDepth.y;
    }
    else
    {
        gl_FragDepth = gl_FragCoord.z;
    }
    bool ctmd = (ctmdFlags & 32) != 0;
    bool uniformLit = (ctmdFlags & 16) != 0;
    bool _115 = ((int(tmdControlA) & 255) & 8) != 0;
    bool _122;
    if (!_115)
    {
        _122 = (ctmdFlags & 2) != 0;
    }
    else
    {
        _122 = _115;
    }
    bool translucent = _122;
    bool textured = ((int(tmdControlA) & 255) & 2) != 0;
    outColour = tmdColour;
    int translucencyMode = ((int(tmdControlA) >> 10) & 3) + 1;
    bool _148;
    if (translucent)
    {
        _148 = (!textured) || uniformLit;
    }
    else
    {
        _148 = translucent;
    }
    if (_148)
    {
        translucencyMode = tmdTranslucency + 1;
    }
    if (textured)
    {
        bool _161 = ((int(tmdControlA) >> 8) & 3) == 0;
        bool _170;
        if (!_161)
        {
            _170 = ((int(tmdControlA) >> 8) & 3) == 1;
        }
        else
        {
            _170 = _161;
        }
        highp vec4 texColour;
        if (_170)
        {
            ivec2 uv = ivec2(int(vec2(float((int(tmdControlA) >> 12) & 15) * 64.0, float((int(tmdControlA) >> 16) & 1) * 256.0).x + ((tmdUv.x + uvOffset.x) * (1.0 / float(1 << (2 - ((int(tmdControlA) >> 8) & 3)))))), int((vec2(float((int(tmdControlA) >> 12) & 15) * 64.0, float((int(tmdControlA) >> 16) & 1) * 256.0).y + tmdUv.y) + uvOffset.y));
            ivec4 indexVec = ivec4(texelFetch(tex15, uv, 0));
            int p = (indexVec.x >> ((int(vec2(float((int(tmdControlA) >> 12) & 15) * 64.0, float((int(tmdControlA) >> 16) & 1) * 256.0).x + tmdUv.x) & ((1 << (2 - ((int(tmdControlA) >> 8) & 3))) - 1)) << (((int(tmdControlA) >> 8) & 3) + 2))) & ((1 << ((((int(tmdControlA) >> 8) & 3) + 1) * 4)) - 1);
            uint pixel = texelFetch(tex15, ivec2(int(vec2(float(int(tmdControlB) & 63) * 16.0, float((int(tmdControlB) >> 6) & 511)).x + float(p)), int(vec2(float(int(tmdControlB) & 63) * 16.0, float((int(tmdControlB) >> 6) & 511)).y)), 0).x;
            texColour.w = float((pixel >> uint(15)) & 31u) / 31.0;
            texColour.z = float((pixel >> uint(10)) & 31u) / 31.0;
            texColour.y = float((pixel >> uint(5)) & 31u) / 31.0;
            texColour.x = float(pixel & 31u) / 31.0;
        }
        else
        {
            if (((int(tmdControlA) >> 8) & 3) == 2)
            {
                ivec2 uv_1 = ivec2(int(vec2(float((int(tmdControlA) >> 12) & 15) * 64.0, float((int(tmdControlA) >> 16) & 1) * 256.0).x + (tmdUv.x + uvOffset.x)), int((vec2(float((int(tmdControlA) >> 12) & 15) * 64.0, float((int(tmdControlA) >> 16) & 1) * 256.0).y + tmdUv.y) + uvOffset.y));
                uint pixel_1 = texelFetch(tex15, uv_1, 0).x;
                texColour.w = float((pixel_1 >> uint(15)) & 31u) / 31.0;
                texColour.z = float((pixel_1 >> uint(10)) & 31u) / 31.0;
                texColour.y = float((pixel_1 >> uint(5)) & 31u) / 31.0;
                texColour.x = float(pixel_1 & 31u) / 31.0;
            }
            else
            {
                texColour = texture(tex24, tmdUv + uvOffset);
            }
        }
        bool _461 = texColour.w == 0.0;
        bool _467;
        if (_461)
        {
            _467 = texColour.x == 0.0;
        }
        else
        {
            _467 = _461;
        }
        bool _473;
        if (_467)
        {
            _473 = texColour.y == 0.0;
        }
        else
        {
            _473 = _467;
        }
        bool _479;
        if (_473)
        {
            _479 = texColour.z == 0.0;
        }
        else
        {
            _479 = _473;
        }
        if (_479)
        {
            discard;
        }
        bool _487 = (discardTranslucency == 1.0) && translucent;
        bool _493;
        if (_487)
        {
            _493 = texColour.w != 0.0;
        }
        else
        {
            _493 = _487;
        }
        bool _511;
        if (!_493)
        {
            bool _498 = discardTranslucency == 2.0;
            bool _510;
            if (_498)
            {
                bool _502 = !translucent;
                bool _509;
                if (!_502)
                {
                    _509 = texColour.w == 0.0;
                }
                else
                {
                    _509 = _502;
                }
                _510 = _509;
            }
            else
            {
                _510 = _498;
            }
            _511 = _510;
        }
        else
        {
            _511 = _493;
        }
        if (_511)
        {
            discard;
        }
        outColour = clamp(outColour * texColour, vec4(0.0), vec4(1.0));
    }
    else
    {
        bool _525 = (discardTranslucency == 1.0) && translucent;
        bool _534;
        if (!_525)
        {
            _534 = (discardTranslucency == 2.0) && (!translucent);
        }
        else
        {
            _534 = _525;
        }
        if (_534)
        {
            discard;
        }
    }
    highp vec4 _542 = outColour;
    highp vec3 _544 = _542.xyz * recolour;
    outColour.x = _544.x;
    outColour.y = _544.y;
    outColour.z = _544.z;
    if (translucent && (translucencyMode == 1))
    {
        outColour.w = 0.5;
    }
    else
    {
        outColour.w = 1.0;
    }
}
