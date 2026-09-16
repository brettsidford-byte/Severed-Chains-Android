#version 320 es

struct VS_OUT
{
    vec2 vertUv;
    vec2 vertTpage;
    vec2 vertClut;
    int vertBpp;
    vec4 vertColour;
    int vertFlags;
    int translucency;
    float widthMultiplier;
    int widthMask;
    int indexShift;
    int indexMask;
    float viewspaceZ;
    float depth;
    float depthOffset;
};

struct ModelTransforms
{
    mat4 model;
    vec4 screenOffset;
};

struct Light
{
    mat4 lightDirection;
    mat3 lightColour;
    vec4 backgroundColour;
};

layout(std140) uniform transforms2
{
    ModelTransforms modelTransforms[128];
} _103;

layout(std140) uniform lighting
{
    Light lights[128];
} _124;

layout(std140) uniform clutAnimation
{
    vec4 clutAnimations[1024];
} _328;

layout(std140) uniform transforms
{
    mat4 camera;
    mat4 projection;
} _400;

layout(std140) uniform projectionInfo
{
    float znear;
    float zfar;
    float zdiffInv;
    float projectionMode;
} _418;

uniform int ctmdFlags;
uniform float modelIndex;
uniform vec3 battleColour;
uniform vec2 tpageOverride;
uniform vec2 clutOverride;

layout(location = 0) in vec4 inPos;
layout(location = 6) in float inFlags;
layout(location = 5) in vec4 inColour;
layout(location = 1) in vec3 inNorm;
layout(location = 3) in float inTpage;
layout(location = 4) in float inClut;
layout(location = 2) in vec2 inUv;
layout(location = 0) out vec2 tmdUv;
layout(location = 1) flat out float tmdControlA;
layout(location = 2) flat out float tmdControlB;
layout(location = 3) out vec4 tmdColour;
layout(location = 4) out vec3 tmdDepth;
VS_OUT vs_out;

void main()
{
    vec4 pos = vec4(inPos.xyz, 1.0);
    vs_out.vertTpage = vec2(0.0);
    vs_out.vertClut = vec2(0.0);
    vs_out.vertBpp = 0;
    vs_out.translucency = 0;
    vs_out.vertColour = vec4(1.0);
    vs_out.depthOffset = 0.0;
    vs_out.vertFlags = int(inFlags);
    bool ctmd = (ctmdFlags & 32) != 0;
    bool uniformLit = (ctmdFlags & 16) != 0;
    bool _70 = (vs_out.vertFlags & 8) != 0;
    bool _77;
    if (!_70)
    {
        _77 = (ctmdFlags & 2) != 0;
    }
    else
    {
        _77 = _70;
    }
    bool translucent = _77;
    bool coloured = (vs_out.vertFlags & 4) != 0;
    bool textured = (vs_out.vertFlags & 2) != 0;
    bool lit = (vs_out.vertFlags & 1) != 0;
    int _107 = int(modelIndex);
    ModelTransforms t;
    t.model = _103.modelTransforms[_107].model;
    t.screenOffset = _103.modelTransforms[_107].screenOffset;
    int _126 = int(modelIndex);
    Light l;
    l.lightDirection = _124.lights[_126].lightDirection;
    l.lightColour = _124.lights[_126].lightColour;
    l.backgroundColour = _124.lights[_126].backgroundColour;
    bool _142 = (textured && translucent) && (!lit);
    bool _148;
    if (_142)
    {
        _148 = ctmd || uniformLit;
    }
    else
    {
        _148 = _142;
    }
    if (_148)
    {
        vec3 _157 = inColour.xyz * battleColour;
        vs_out.vertColour.x = _157.x;
        vs_out.vertColour.y = _157.y;
        vs_out.vertColour.z = _157.z;
        if (vs_out.vertColour.x > 2.0)
        {
            vs_out.vertColour.x = mod(vs_out.vertColour.x, 2.0);
        }
        if (vs_out.vertColour.y > 2.0)
        {
            vs_out.vertColour.y = mod(vs_out.vertColour.y, 2.0);
        }
        if (vs_out.vertColour.z > 2.0)
        {
            vs_out.vertColour.z = mod(vs_out.vertColour.z, 2.0);
        }
    }
    else
    {
        if (lit)
        {
            float range = 1.0;
            if (textured)
            {
                range = 2.0;
            }
            vec3 _235 = clamp(clamp((l.lightColour * clamp(l.lightDirection * vec4(inNorm, 1.0), vec4(0.0), vec4(8.0)).xyz) + l.backgroundColour.xyz, vec3(0.0), vec3(8.0)) * inColour.xyz, vec3(0.0), vec3(range));
            vs_out.vertColour.x = _235.x;
            vs_out.vertColour.y = _235.y;
            vs_out.vertColour.z = _235.z;
        }
        else
        {
            if (coloured)
            {
                vs_out.vertColour = inColour;
            }
            else
            {
                vs_out.vertColour = vec4(1.0);
            }
        }
    }
    int intTpage = int(inTpage);
    vs_out.vertBpp = (intTpage >> 7) & 3;
    vs_out.translucency = (intTpage >> 5) & 3;
    if (textured)
    {
        if (tpageOverride.x == 0.0)
        {
            vs_out.vertTpage = vec2(float((intTpage & 15) * 64), float(((intTpage & 16) != 0) ? 256 : 0));
        }
        else
        {
            vs_out.vertTpage = tpageOverride;
        }
        if (clutOverride.x == 0.0)
        {
            int intClut = int(inClut);
            vs_out.vertClut = vec2(float((intClut & 63) * 16), float(intClut >> 6));
        }
        else
        {
            vs_out.vertClut = clutOverride;
        }
        for (int clutAnimationIndex = 0; clutAnimationIndex < 1024; clutAnimationIndex++)
        {
            vec4 anim = _328.clutAnimations[clutAnimationIndex];
            if (anim.x == (-1.0))
            {
                break;
            }
            if (all(equal(anim.xy, vs_out.vertClut)))
            {
                vs_out.vertClut = anim.zw;
                break;
            }
        }
        bool _357 = vs_out.vertBpp == 0;
        bool _364;
        if (!_357)
        {
            _364 = vs_out.vertBpp == 1;
        }
        else
        {
            _364 = _357;
        }
        if (_364)
        {
            int widthDivisor = 1 << (2 - vs_out.vertBpp);
            vs_out.widthMultiplier = 1.0 / float(widthDivisor);
            vs_out.widthMask = widthDivisor - 1;
            vs_out.indexShift = vs_out.vertBpp + 2;
            vs_out.indexMask = int(pow(16.0, float(vs_out.vertBpp + 1)) - 1.0);
        }
    }
    gl_Position = (_400.camera * t.model) * pos;
    vs_out.viewspaceZ = gl_Position.z;
    if (_418.projectionMode == 1.0)
    {
        float z = clamp(gl_Position.z, 0.0, 65536.0);
        if (z != 0.0)
        {
            vec4 _439 = gl_Position;
            vec2 _441 = _439.xy * (_418.zfar / z);
            gl_Position.x = _441.x;
            gl_Position.y = _441.y;
        }
    }
    if (_418.projectionMode == 2.0)
    {
        vs_out.depthOffset = t.screenOffset.z * _418.zdiffInv;
    }
    else
    {
        if (t.screenOffset.z != 0.0)
        {
            gl_Position.z += t.screenOffset.z;
        }
    }
    vec4 _473 = gl_Position;
    vec2 _475 = _473.xy + t.screenOffset.xy;
    gl_Position.x = _475.x;
    gl_Position.y = _475.y;
    gl_Position = _400.projection * gl_Position;
    vs_out.vertUv = inUv;
    vs_out.depth = gl_Position.z;
    tmdUv = vs_out.vertUv;
    int material = vs_out.vertBpp | (vs_out.translucency << 2);
    int tpageX = int(vs_out.vertTpage.x) / 64;
    int tpageY = int(vs_out.vertTpage.y) / 256;
    int clutX = int(vs_out.vertClut.x) / 16;
    int clutY = int(vs_out.vertClut.y);
    tmdControlA = float((((vs_out.vertFlags & 255) | (material << 8)) | (tpageX << 12)) | (tpageY << 16));
    tmdControlB = float(clutX | (clutY << 6));
    tmdColour = vs_out.vertColour;
    tmdDepth = vec3(vs_out.viewspaceZ, vs_out.depth, vs_out.depthOffset);
}
