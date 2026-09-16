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

uniform int tmdTranslucency;
uniform highp vec2 uvOffset;
layout(location = 5, binding = 3) uniform highp usampler2D tex15;
layout(location = 4, binding = 2) uniform highp sampler2D tex24;
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
    bool translucent = ((int(tmdControlA) & 255) & 8) != 0;
    bool textured = ((int(tmdControlA) & 255) & 2) != 0;
    outColour = tmdColour;
    int translucencyMode = ((int(tmdControlA) >> 10) & 3) + 1;
    if (translucent && (!textured))
    {
        translucencyMode = tmdTranslucency + 1;
    }
    if (textured)
    {
        bool _139 = ((int(tmdControlA) >> 8) & 3) == 0;
        bool _148;
        if (!_139)
        {
            _148 = ((int(tmdControlA) >> 8) & 3) == 1;
        }
        else
        {
            _148 = _139;
        }
        highp vec4 texColour;
        if (_148)
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
        bool _440 = texColour.w == 0.0;
        bool _446;
        if (_440)
        {
            _446 = texColour.x == 0.0;
        }
        else
        {
            _446 = _440;
        }
        bool _452;
        if (_446)
        {
            _452 = texColour.y == 0.0;
        }
        else
        {
            _452 = _446;
        }
        bool _458;
        if (_452)
        {
            _458 = texColour.z == 0.0;
        }
        else
        {
            _458 = _452;
        }
        if (_458)
        {
            discard;
        }
        bool _466 = (discardTranslucency == 1.0) && translucent;
        bool _472;
        if (_466)
        {
            _472 = texColour.w != 0.0;
        }
        else
        {
            _472 = _466;
        }
        bool _490;
        if (!_472)
        {
            bool _477 = discardTranslucency == 2.0;
            bool _489;
            if (_477)
            {
                bool _481 = !translucent;
                bool _488;
                if (!_481)
                {
                    _488 = texColour.w == 0.0;
                }
                else
                {
                    _488 = _481;
                }
                _489 = _488;
            }
            else
            {
                _489 = _477;
            }
            _490 = _489;
        }
        else
        {
            _490 = _472;
        }
        if (_490)
        {
            discard;
        }
        outColour = clamp(outColour * texColour, vec4(0.0), vec4(1.0));
    }
    else
    {
        bool _504 = (discardTranslucency == 1.0) && translucent;
        bool _513;
        if (!_504)
        {
            _513 = (discardTranslucency == 2.0) && (!translucent);
        }
        else
        {
            _513 = _504;
        }
        if (_513)
        {
            discard;
        }
    }
    highp vec4 _521 = outColour;
    highp vec3 _523 = _521.xyz * recolour;
    outColour.x = _523.x;
    outColour.y = _523.y;
    outColour.z = _523.z;
    if (translucent && (translucencyMode == 1))
    {
        outColour.w = 0.5;
    }
    else
    {
        outColour.w = 1.0;
    }
}
