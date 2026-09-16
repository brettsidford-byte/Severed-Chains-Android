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

uniform highp float translucency;
uniform highp vec2 uvOffset;
layout(location = 7, binding = 3) uniform highp usampler2D tex15;
layout(location = 6, binding = 2) uniform highp sampler2D tex24;
uniform highp float useTextureAlpha;
uniform highp float discardTranslucency;
uniform highp vec3 recolour;
uniform highp float alpha;

layout(location = 10) in highp float depth;
layout(location = 11) in highp float depthOffset;
layout(location = 5) flat in int vertFlags;
layout(location = 0) out highp vec4 outColour;
layout(location = 4) in highp vec4 vertColour;
layout(location = 3) flat in int vertBpp;
layout(location = 1) flat in highp vec2 vertTpage;
layout(location = 0) in highp vec2 vertUv;
layout(location = 6) flat in highp float widthMultiplier;
layout(location = 7) flat in int widthMask;
layout(location = 8) flat in int indexShift;
layout(location = 9) flat in int indexMask;
layout(location = 2) flat in highp vec2 vertClut;

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
        gl_FragDepth = ((depth - _67.znear) * _67.zdiffInv) + depthOffset;
    }
    else
    {
        gl_FragDepth = gl_FragCoord.z;
    }
    bool translucent = ((vertFlags & 8) != 0) || (translucency != 0.0);
    bool textured = (vertFlags & 2) != 0;
    outColour = vertColour;
    int translucencyMode = int(translucency);
    if (textured)
    {
        bool _122 = vertBpp == 0;
        bool _128;
        if (!_122)
        {
            _128 = vertBpp == 1;
        }
        else
        {
            _128 = _122;
        }
        highp vec4 texColour;
        if (_128)
        {
            ivec2 uv = ivec2(int(vertTpage.x + ((vertUv.x + uvOffset.x) * widthMultiplier)), int((vertTpage.y + vertUv.y) + uvOffset.y));
            ivec4 indexVec = ivec4(texelFetch(tex15, uv, 0));
            int p = (indexVec.x >> ((int(vertTpage.x + vertUv.x) & widthMask) << indexShift)) & indexMask;
            uint pixel = texelFetch(tex15, ivec2(int(vertClut.x + float(p)), int(vertClut.y)), 0).x;
            texColour.w = float((pixel >> uint(15)) & 31u) / 31.0;
            texColour.z = float((pixel >> uint(10)) & 31u) / 31.0;
            texColour.y = float((pixel >> uint(5)) & 31u) / 31.0;
            texColour.x = float(pixel & 31u) / 31.0;
        }
        else
        {
            if (vertBpp == 2)
            {
                ivec2 uv_1 = ivec2(int(vertTpage.x + (vertUv.x + uvOffset.x)), int((vertTpage.y + vertUv.y) + uvOffset.y));
                uint pixel_1 = texelFetch(tex15, uv_1, 0).x;
                texColour.w = float((pixel_1 >> uint(15)) & 31u) / 31.0;
                texColour.z = float((pixel_1 >> uint(10)) & 31u) / 31.0;
                texColour.y = float((pixel_1 >> uint(5)) & 31u) / 31.0;
                texColour.x = float(pixel_1 & 31u) / 31.0;
            }
            else
            {
                texColour = texture(tex24, vertUv + uvOffset);
            }
        }
        bool _309 = texColour.w == 0.0;
        bool _334;
        if (_309)
        {
            bool _314 = useTextureAlpha != 0.0;
            bool _333;
            if (!_314)
            {
                bool _320 = texColour.x == 0.0;
                bool _326;
                if (_320)
                {
                    _326 = texColour.y == 0.0;
                }
                else
                {
                    _326 = _320;
                }
                bool _332;
                if (_326)
                {
                    _332 = texColour.z == 0.0;
                }
                else
                {
                    _332 = _326;
                }
                _333 = _332;
            }
            else
            {
                _333 = _314;
            }
            _334 = _333;
        }
        else
        {
            _334 = _309;
        }
        if (_334)
        {
            discard;
        }
        bool _343 = (discardTranslucency == 1.0) && translucent;
        bool _349;
        if (_343)
        {
            _349 = texColour.w != 0.0;
        }
        else
        {
            _349 = _343;
        }
        bool _367;
        if (!_349)
        {
            bool _354 = discardTranslucency == 2.0;
            bool _366;
            if (_354)
            {
                bool _358 = !translucent;
                bool _365;
                if (!_358)
                {
                    _365 = texColour.w == 0.0;
                }
                else
                {
                    _365 = _358;
                }
                _366 = _365;
            }
            else
            {
                _366 = _354;
            }
            _367 = _366;
        }
        else
        {
            _367 = _349;
        }
        if (_367)
        {
            discard;
        }
        outColour = clamp(outColour * texColour, vec4(0.0), vec4(1.0));
    }
    else
    {
        bool _381 = (discardTranslucency == 1.0) && translucent;
        bool _390;
        if (!_381)
        {
            _390 = (discardTranslucency == 2.0) && (!translucent);
        }
        else
        {
            _390 = _381;
        }
        if (_390)
        {
            discard;
        }
    }
    highp vec4 _398 = outColour;
    highp vec3 _400 = _398.xyz * recolour;
    outColour.x = _400.x;
    outColour.y = _400.y;
    outColour.z = _400.z;
    if (alpha != (-1.0))
    {
        if (useTextureAlpha == 0.0)
        {
            outColour.w = alpha;
        }
        else
        {
            outColour.w *= alpha;
            if ((translucencyMode == 2) || (translucencyMode == 3))
            {
                highp float _433 = outColour.w;
                highp vec4 _434 = outColour;
                highp vec3 _436 = _434.xyz * _433;
                outColour.x = _436.x;
                outColour.y = _436.y;
                outColour.z = _436.z;
            }
        }
    }
    else
    {
        if (useTextureAlpha == 0.0)
        {
            if (translucent && (translucencyMode == 1))
            {
                outColour.w = 0.5;
            }
            else
            {
                outColour.w = 1.0;
            }
        }
    }
}
