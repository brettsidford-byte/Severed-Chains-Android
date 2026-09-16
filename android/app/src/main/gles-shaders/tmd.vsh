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
} _78;

layout(std140) uniform lighting
{
    Light lights[128];
} _99;

layout(std140) uniform clutAnimation
{
    vec4 clutAnimations[1024];
} _250;

layout(std140) uniform transforms
{
    mat4 camera;
    mat4 projection;
} _323;

layout(std140) uniform projectionInfo
{
    float znear;
    float zfar;
    float zdiffInv;
    float projectionMode;
} _341;

uniform float modelIndex;
uniform vec2 tpageOverride;
uniform vec2 clutOverride;

layout(location = 0) in vec4 inPos;
layout(location = 6) in float inFlags;
layout(location = 1) in vec3 inNorm;
layout(location = 5) in vec4 inColour;
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
    bool coloured = (vs_out.vertFlags & 4) != 0;
    bool textured = (vs_out.vertFlags & 2) != 0;
    bool lit = (vs_out.vertFlags & 1) != 0;
    int _82 = int(modelIndex);
    ModelTransforms t;
    t.model = _78.modelTransforms[_82].model;
    t.screenOffset = _78.modelTransforms[_82].screenOffset;
    int _101 = int(modelIndex);
    Light l;
    l.lightDirection = _99.lights[_101].lightDirection;
    l.lightColour = _99.lights[_101].lightColour;
    l.backgroundColour = _99.lights[_101].backgroundColour;
    if (lit)
    {
        float range = 1.0;
        if (textured)
        {
            range = 2.0;
        }
        vec3 _153 = clamp(clamp((l.lightColour * clamp(l.lightDirection * vec4(inNorm, 1.0), vec4(0.0), vec4(8.0)).xyz) + l.backgroundColour.xyz, vec3(0.0), vec3(8.0)) * inColour.xyz, vec3(0.0), vec3(range));
        vs_out.vertColour.x = _153.x;
        vs_out.vertColour.y = _153.y;
        vs_out.vertColour.z = _153.z;
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
            vec4 anim = _250.clutAnimations[clutAnimationIndex];
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
        bool _279 = vs_out.vertBpp == 0;
        bool _286;
        if (!_279)
        {
            _286 = vs_out.vertBpp == 1;
        }
        else
        {
            _286 = _279;
        }
        if (_286)
        {
            int widthDivisor = 1 << (2 - vs_out.vertBpp);
            vs_out.widthMultiplier = 1.0 / float(widthDivisor);
            vs_out.widthMask = widthDivisor - 1;
            vs_out.indexShift = vs_out.vertBpp + 2;
            vs_out.indexMask = int(pow(16.0, float(vs_out.vertBpp + 1)) - 1.0);
        }
    }
    gl_Position = (_323.camera * t.model) * pos;
    vs_out.viewspaceZ = gl_Position.z;
    if (_341.projectionMode == 1.0)
    {
        float z = clamp(gl_Position.z, 0.0, 65536.0);
        if (z != 0.0)
        {
            vec4 _362 = gl_Position;
            vec2 _364 = _362.xy * (_341.zfar / z);
            gl_Position.x = _364.x;
            gl_Position.y = _364.y;
        }
    }
    if (_341.projectionMode == 2.0)
    {
        vs_out.depthOffset = t.screenOffset.z * _341.zdiffInv;
    }
    else
    {
        if (t.screenOffset.z != 0.0)
        {
            gl_Position.z += t.screenOffset.z;
        }
    }
    vec4 _396 = gl_Position;
    vec2 _398 = _396.xy + t.screenOffset.xy;
    gl_Position.x = _398.x;
    gl_Position.y = _398.y;
    gl_Position = _323.projection * gl_Position;
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
