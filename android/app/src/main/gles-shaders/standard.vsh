#version 320 es

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
} _55;

layout(std140) uniform lighting
{
    Light lights[128];
} _79;

layout(std140) uniform clutAnimation
{
    vec4 clutAnimations[1024];
} _228;

layout(std140) uniform transforms
{
    mat4 camera;
    mat4 projection;
} _291;

layout(std140) uniform projectionInfo
{
    float znear;
    float zfar;
    float zdiffInv;
    float projectionMode;
} _303;

uniform float modelIndex;
uniform vec2 tpageOverride;
uniform vec2 clutOverride;

layout(location = 0) in vec4 inPos;
layout(location = 5) flat out int vertFlags;
layout(location = 6) in float inFlags;
layout(location = 4) out vec4 vertColour;
layout(location = 1) in vec3 inNorm;
layout(location = 5) in vec4 inColour;
layout(location = 3) in float inTpage;
layout(location = 3) flat out int vertBpp;
layout(location = 1) flat out vec2 vertTpage;
layout(location = 4) in float inClut;
layout(location = 2) flat out vec2 vertClut;
layout(location = 6) flat out float widthMultiplier;
layout(location = 7) flat out int widthMask;
layout(location = 8) flat out int indexShift;
layout(location = 9) flat out int indexMask;
layout(location = 11) out float depthOffset;
layout(location = 0) out vec2 vertUv;
layout(location = 2) in vec2 inUv;
layout(location = 10) out float depth;

void main()
{
    vec4 pos = vec4(inPos.xyz, 1.0);
    vertFlags = int(inFlags);
    bool coloured = (vertFlags & 4) != 0;
    bool textured = (vertFlags & 2) != 0;
    bool lit = (vertFlags & 1) != 0;
    int _59 = int(modelIndex);
    ModelTransforms t;
    t.model = _55.modelTransforms[_59].model;
    t.screenOffset = _55.modelTransforms[_59].screenOffset;
    if (lit)
    {
        int _81 = int(modelIndex);
        Light l;
        l.lightDirection = _79.lights[_81].lightDirection;
        l.lightColour = _79.lights[_81].lightColour;
        l.backgroundColour = _79.lights[_81].backgroundColour;
        float range = 1.0;
        if (textured)
        {
            range = 2.0;
        }
        vec3 _133 = clamp(clamp((l.lightColour * clamp(l.lightDirection * vec4(inNorm, 1.0), vec4(0.0), vec4(8.0)).xyz) + l.backgroundColour.xyz, vec3(0.0), vec3(8.0)) * inColour.xyz, vec3(0.0), vec3(range));
        vertColour.x = _133.x;
        vertColour.y = _133.y;
        vertColour.z = _133.z;
    }
    else
    {
        if (coloured)
        {
            vertColour = inColour;
        }
        else
        {
            vertColour = vec4(1.0);
        }
    }
    int intTpage = int(inTpage);
    vertBpp = (intTpage >> 7) & 3;
    if (textured)
    {
        if (tpageOverride.x == 0.0)
        {
            vertTpage = vec2(float((intTpage & 15) * 64), float(((intTpage & 16) != 0) ? 256 : 0));
        }
        else
        {
            vertTpage = tpageOverride;
        }
        if (clutOverride.x == 0.0)
        {
            int intClut = int(inClut);
            vertClut = vec2(float((intClut & 63) * 16), float(intClut >> 6));
        }
        else
        {
            vertClut = clutOverride;
        }
        for (int clutAnimationIndex = 0; clutAnimationIndex < 1024; clutAnimationIndex++)
        {
            vec4 anim = _228.clutAnimations[clutAnimationIndex];
            if (anim.x == (-1.0))
            {
                break;
            }
            if (all(equal(anim.xy, vertClut)))
            {
                vertClut = anim.zw;
                break;
            }
        }
        bool _254 = vertBpp == 0;
        bool _260;
        if (!_254)
        {
            _260 = vertBpp == 1;
        }
        else
        {
            _260 = _254;
        }
        if (_260)
        {
            int widthDivisor = 1 << (2 - vertBpp);
            widthMultiplier = 1.0 / float(widthDivisor);
            widthMask = widthDivisor - 1;
            indexShift = vertBpp + 2;
            indexMask = int(pow(16.0, float(vertBpp + 1)) - 1.0);
        }
    }
    gl_Position = (_291.camera * t.model) * pos;
    if (_303.projectionMode == 1.0)
    {
        float z = clamp(gl_Position.z, 0.0, 65536.0);
        if (z != 0.0)
        {
            vec4 _324 = gl_Position;
            vec2 _326 = _324.xy * (_303.zfar / z);
            gl_Position.x = _326.x;
            gl_Position.y = _326.y;
        }
    }
    if (_303.projectionMode == 2.0)
    {
        depthOffset = t.screenOffset.z * _303.zdiffInv;
    }
    else
    {
        if (t.screenOffset.z != 0.0)
        {
            gl_Position.z += t.screenOffset.z;
        }
    }
    vec4 _358 = gl_Position;
    vec2 _360 = _358.xy + t.screenOffset.xy;
    gl_Position.x = _360.x;
    gl_Position.y = _360.y;
    gl_Position = _291.projection * gl_Position;
    vertUv = inUv;
    depth = gl_Position.z;
}
