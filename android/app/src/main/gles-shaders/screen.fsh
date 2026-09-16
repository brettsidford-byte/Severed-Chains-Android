#version 320 es
precision mediump float;
precision highp int;

uniform highp vec2 resolution;
uniform bool pixelate;
uniform highp vec4 turn_order_bounds;
uniform highp float warp_amount;
uniform highp float vignette_intensity;
uniform highp float vignette_opacity;
layout(location = 0, binding = 0) uniform highp sampler2D screen;
uniform bool enableCrt;
uniform bool roll;
uniform highp float time;
uniform highp float noise_opacity;
uniform highp float roll_size;
uniform highp float roll_speed;
uniform highp float roll_variation;
uniform highp float distort_intensity;
uniform highp float aberration;
uniform highp float grille_opacity;
uniform highp float brightness;
uniform highp float scanlines_opacity;
uniform highp float scanlines_width;
uniform highp float bloom_intensity;
uniform highp float bloom_radius;
uniform highp float bloom_threshold;
uniform highp float noise_speed;
uniform highp float static_noise_intensity;
uniform bool discolor;

layout(location = 0) out highp vec4 frag;
layout(location = 0) in highp vec2 vertUv;
bool clip_warp;

highp vec2 warp(highp vec2 uv)
{
    highp vec2 delta = uv - vec2(0.5);
    highp float delta2 = dot(delta, delta);
    highp float delta4 = delta2 * delta2;
    highp float delta_offset = delta4 * warp_amount;
    return uv + (delta * delta_offset);
}
highp vec2 getActiveResolution(highp vec2 uv)
{
    highp vec2 res = resolution;
    bool _143;
    if (pixelate)
    {
        _143 = res.y <= 480.0;
    }
    else
    {
        _143 = pixelate;
    }
    if (_143)
    {
        bool _153 = uv.x >= turn_order_bounds.x;
        bool _162;
        if (_153)
        {
            _162 = uv.x <= turn_order_bounds.z;
        }
        else
        {
            _162 = _153;
        }
        bool _170;
        if (_162)
        {
            _170 = uv.y >= turn_order_bounds.y;
        }
        else
        {
            _170 = _162;
        }
        bool _179;
        if (_170)
        {
            _179 = uv.y <= turn_order_bounds.w;
        }
        else
        {
            _179 = _170;
        }
        if (_179)
        {
            highp float aspect = res.x / res.y;
            res = vec2((480.0 * aspect) * 1.5, 480.0);
        }
    }
    return res;
}

highp vec4 textureBilinear(highp sampler2D tex, highp vec2 uv)
{
    highp vec2 size = vec2(textureSize(tex, 0));
    highp vec2 texel = (uv * size) - vec2(0.5);
    highp vec2 f = fract(texel);
    highp vec2 ip = (floor(texel) + vec2(0.5)) / size;
    highp vec2 one = vec2(1.0) / size;
    highp vec4 t00 = texture(tex, ip);
    highp vec4 t10 = texture(tex, ip + vec2(one.x, 0.0));
    highp vec4 t01 = texture(tex, ip + vec2(0.0, one.y));
    highp vec4 t11 = texture(tex, ip + one);
    return mix(mix(t00, t10, vec4(f.x)), mix(t01, t11, vec4(f.x)), vec4(f.y));
}

highp vec3 sampleHighPass(highp sampler2D tex, highp vec2 uv, highp vec2 offset, highp float threshold)
{
    highp vec2 sample_uv = uv + offset;
    if (pixelate)
    {
        highp vec2 param = sample_uv;
        highp vec2 res = getActiveResolution(param);
        sample_uv = (floor(sample_uv * res) + vec2(0.5)) / res;
    }
    highp vec2 param_1 = sample_uv;
    highp vec3 color = textureBilinear(tex, param_1).xyz;
    return max(color - vec3(threshold), vec3(0.0));
}

highp vec2 random(inout highp vec2 uv)
{
    uv = vec2(dot(uv, vec2(127.09999847412109375, 311.70001220703125)), dot(uv, vec2(269.5, 183.3000030517578125)));
    return vec2(-1.0) + (fract(sin(uv) * 43758.546875) * 2.0);
}

highp float _noise(highp vec2 uv)
{
    highp vec2 uv_index = floor(uv);
    highp vec2 uv_fract = fract(uv);
    highp vec2 blur = smoothstep(vec2(0.0), vec2(1.0), uv_fract);
    highp vec2 param = uv_index + vec2(0.0);
    highp vec2 _267 = random(param);
    highp vec2 param_1 = uv_index + vec2(1.0, 0.0);
    highp vec2 _275 = random(param_1);
    highp vec2 param_2 = uv_index + vec2(0.0, 1.0);
    highp vec2 _286 = random(param_2);
    highp vec2 param_3 = uv_index + vec2(1.0);
    highp vec2 _294 = random(param_3);
    return (mix(mix(dot(_267, uv_fract - vec2(0.0)), dot(_275, uv_fract - vec2(1.0, 0.0)), blur.x), mix(dot(_286, uv_fract - vec2(0.0, 1.0)), dot(_294, uv_fract - vec2(1.0)), blur.x), blur.y) * 0.5) + 0.5;
}

highp float border(highp vec2 uv)
{
    highp float radius = min(warp_amount, 0.07999999821186065673828125);
    radius = max(min(min(abs(radius * 2.0), 1.0), 1.0), 9.9999997473787516355514526367188e-06);
    highp vec2 abs_uv = (abs((uv * 2.0) - vec2(1.0)) - vec2(1.0)) + vec2(radius);
    highp float dist = length(max(vec2(0.0), abs_uv)) / radius;
    highp float square = smoothstep(0.959999978542327880859375, 1.0, dist);
    return clamp(1.0 - square, 0.0, 1.0);
}

highp float vignette(inout highp vec2 uv)
{
    uv *= (vec2(1.0) - uv);
    highp float vignette_1 = (uv.x * uv.y) * 15.0;
    return pow(vignette_1, vignette_intensity * vignette_opacity);
}

void main()
{
    clip_warp = false;
    frag = vec4(texture(screen, vertUv).xyz, 1.0);
    if (!enableCrt)
    {
        return;
    }
    highp vec2 UV = vertUv;
    highp vec2 param = UV;
    highp vec2 uv = warp(param);
    highp vec2 text_uv = uv;
    highp vec2 roll_uv = vec2(0.0);
    highp float rollTime = roll ? time : 0.0;
    highp vec2 param_1 = uv;
    highp vec2 active_res = getActiveResolution(param_1);
    if (pixelate)
    {
        text_uv = (floor(uv * active_res) + vec2(0.5)) / active_res;
    }
    highp float roll_line = 0.0;
    if (roll || (noise_opacity > 0.0))
    {
        roll_line = smoothstep(0.300000011920928955078125, 0.89999997615814208984375, sin((uv.y * roll_size) - (rollTime * roll_speed)));
        roll_line *= (roll_line * smoothstep(0.300000011920928955078125, 0.89999997615814208984375, sin(((uv.y * roll_size) * roll_variation) - ((rollTime * roll_speed) * roll_variation))));
        roll_uv = vec2((roll_line * distort_intensity) * (1.0 - UV.x), 0.0);
    }
    highp vec4 text;
    if (roll)
    {
        highp vec2 param_2 = (text_uv + (roll_uv * 0.800000011920928955078125)) + (vec2(aberration, 0.0) * 0.100000001490116119384765625);
        text.x = textureBilinear(screen, param_2).x;
        highp vec2 param_3 = (text_uv + (roll_uv * 1.2000000476837158203125)) - (vec2(aberration, 0.0) * 0.100000001490116119384765625);
        text.y = textureBilinear(screen, param_3).y;
        text.z = texture(screen, text_uv + roll_uv).z;
        text.w = 1.0;
    }
    else
    {
        highp vec2 param_4 = text_uv + (vec2(aberration, 0.0) * 0.100000001490116119384765625);
        text.x = textureBilinear(screen, param_4).x;
        highp vec2 param_5 = text_uv - (vec2(aberration, 0.0) * 0.100000001490116119384765625);
        text.y = textureBilinear(screen, param_5).y;
        text.z = texture(screen, text_uv).z;
        text.w = 1.0;
    }
    highp float r = text.x;
    highp float g = text.y;
    highp float b = text.z;
    highp vec2 param_6 = UV;
    uv = warp(param_6);
    if (grille_opacity > 0.0)
    {
        highp float g_r = smoothstep(0.85000002384185791015625, 0.949999988079071044921875, abs(sin(uv.x * (resolution.x * 3.1415927410125732421875))));
        highp float g_g = smoothstep(0.85000002384185791015625, 0.949999988079071044921875, abs(sin(1.0499999523162841796875 + (uv.x * (resolution.x * 3.1415927410125732421875)))));
        highp float g_b = smoothstep(0.85000002384185791015625, 0.949999988079071044921875, abs(sin(2.099999904632568359375 + (uv.x * (resolution.x * 3.1415927410125732421875)))));
        highp float r_linear = pow(r, 2.2000000476837158203125);
        highp float g_linear = pow(g, 2.2000000476837158203125);
        highp float b_linear = pow(b, 2.2000000476837158203125);
        highp float r_masked = mix(r_linear, r_linear * g_r, grille_opacity);
        highp float g_masked = mix(g_linear, g_linear * g_g, grille_opacity);
        highp float b_masked = mix(b_linear, b_linear * g_b, grille_opacity);
        r = pow(clamp(r_masked, 0.0, 1.0), 0.4545454680919647216796875);
        g = pow(clamp(g_masked, 0.0, 1.0), 0.4545454680919647216796875);
        b = pow(clamp(b_masked, 0.0, 1.0), 0.4545454680919647216796875);
    }
    text.x = clamp(r * brightness, 0.0, 1.0);
    text.y = clamp(g * brightness, 0.0, 1.0);
    text.z = clamp(b * brightness, 0.0, 1.0);
    highp float scanlines = 0.5;
    if (scanlines_opacity > 0.0)
    {
        scanlines = smoothstep(scanlines_width, scanlines_width + 0.5, abs(sin(uv.y * (resolution.y * 3.1415927410125732421875))));
        highp vec4 _684 = text;
        highp vec4 _686 = text;
        highp vec3 _693 = mix(_684.xyz, _686.xyz * vec3(scanlines), vec3(scanlines_opacity));
        text.x = _693.x;
        text.y = _693.y;
        text.z = _693.z;
    }
    if (bloom_intensity > 0.0)
    {
        highp vec2 native_res = vec2(textureSize(screen, 0));
        highp vec2 _step = vec2(bloom_radius) / native_res;
        highp vec3 blurred = vec3(0.0);
        highp vec2 param_7 = uv;
        highp vec2 param_8 = vec2(-_step.x, -_step.y);
        highp float param_9 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_7, param_8, param_9) * 1.0);
        highp vec2 param_10 = uv;
        highp vec2 param_11 = vec2(0.0, -_step.y);
        highp float param_12 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_10, param_11, param_12) * 2.0);
        highp vec2 param_13 = uv;
        highp vec2 param_14 = vec2(_step.x, -_step.y);
        highp float param_15 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_13, param_14, param_15) * 1.0);
        highp vec2 param_16 = uv;
        highp vec2 param_17 = vec2(-_step.x, 0.0);
        highp float param_18 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_16, param_17, param_18) * 2.0);
        highp vec2 param_19 = uv;
        highp vec2 param_20 = vec2(0.0);
        highp float param_21 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_19, param_20, param_21) * 4.0);
        highp vec2 param_22 = uv;
        highp vec2 param_23 = vec2(_step.x, 0.0);
        highp float param_24 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_22, param_23, param_24) * 2.0);
        highp vec2 param_25 = uv;
        highp vec2 param_26 = vec2(-_step.x, _step.y);
        highp float param_27 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_25, param_26, param_27) * 1.0);
        highp vec2 param_28 = uv;
        highp vec2 param_29 = vec2(0.0, _step.y);
        highp float param_30 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_28, param_29, param_30) * 2.0);
        highp vec2 param_31 = uv;
        highp vec2 param_32 = vec2(_step.x, _step.y);
        highp float param_33 = bloom_threshold;
        blurred += (sampleHighPass(screen, param_31, param_32, param_33) * 1.0);
        blurred /= vec3(16.0);
        highp vec3 bloom_color = (blurred * 2.0) * bloom_intensity;
        highp vec4 _847 = text;
        highp vec4 _851 = text;
        highp vec3 _855 = (_847.xyz + bloom_color) - (_851.xyz * bloom_color);
        text.x = _855.x;
        text.y = _855.y;
        text.z = _855.z;
    }
    if (noise_opacity > 0.0)
    {
        highp vec2 param_34 = (uv * vec2(2.0, 200.0)) + vec2(10.0, time * noise_speed);
        highp float _noise_1 = smoothstep(0.4000000059604644775390625, 0.5, _noise(param_34));
        highp vec2 param_35 = (ceil(uv * resolution) / resolution) + vec2(time * 0.800000011920928955078125, 0.0);
        highp vec2 _896 = random(param_35);
        roll_line *= ((_noise_1 * scanlines) * clamp(_896.x + 0.800000011920928955078125, 0.0, 1.0));
        highp vec4 _903 = text;
        highp vec4 _905 = text;
        highp vec3 _914 = clamp(mix(_903.xyz, _905.xyz + vec3(roll_line), vec3(noise_opacity)), vec3(0.0), vec3(1.0));
        text.x = _914.x;
        text.y = _914.y;
        text.z = _914.z;
    }
    if (static_noise_intensity > 0.0)
    {
        highp vec2 param_36 = (ceil(uv * resolution) / resolution) + vec2(fract(time));
        highp vec2 _937 = random(param_36);
        highp vec4 _942 = text;
        highp vec3 _945 = _942.xyz + vec3(clamp(_937.x, 0.0, 1.0) * static_noise_intensity);
        text.x = _945.x;
        text.y = _945.y;
        text.z = _945.z;
    }
    highp vec2 param_37 = uv;
    highp vec4 _955 = text;
    highp vec3 _957 = _955.xyz * border(param_37);
    text.x = _957.x;
    text.y = _957.y;
    text.z = _957.z;
    highp vec2 param_38 = uv;
    highp float _966 = vignette(param_38);
    highp vec4 _967 = text;
    highp vec3 _969 = _967.xyz * _966;
    text.x = _969.x;
    text.y = _969.y;
    text.z = _969.z;
    if (clip_warp)
    {
        highp vec2 param_39 = uv;
        text.w = border(param_39);
    }
    highp float saturation = 0.5;
    highp float contrast = 1.2000000476837158203125;
    if (discolor)
    {
        highp vec3 greyscale = vec3((text.x + text.y) + text.z) / vec3(3.0);
        highp vec4 _1002 = text;
        highp vec3 _1007 = mix(_1002.xyz, greyscale, vec3(saturation));
        text.x = _1007.x;
        text.y = _1007.y;
        text.z = _1007.z;
        highp float midpoint = 0.21763764321804046630859375;
        highp vec4 _1016 = text;
        highp vec3 _1025 = ((_1016.xyz - vec3(midpoint)) * contrast) + vec3(midpoint);
        text.x = _1025.x;
        text.y = _1025.y;
        text.z = _1025.z;
    }
    frag = text;
}
