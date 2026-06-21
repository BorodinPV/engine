#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in float aAlpha;

uniform mat4 view;
uniform mat4 projection;

out float vAlpha;

void main()
{
    // Billboard: extract camera-right from view matrix
    vec3 camRight = vec3(view[0][0], view[1][0], view[2][0]);

    // 6 vertices per quad (2 triangles): TL, TR, BL, BL, TR, BR
    int vertId = gl_VertexID % 6;
    // Map to quad corner: 0=TL, 1=TR, 2=BL, 3=BL, 4=TR, 5=BR
    float side;
    float vert;
    if (vertId == 0)      { side = -0.5; vert = 0.0; }  // TL
    else if (vertId == 1) { side =  0.5; vert = 0.0; }  // TR
    else if (vertId == 2) { side = -0.5; vert = -1.0; } // BL
    else if (vertId == 3) { side = -0.5; vert = -1.0; } // BL
    else if (vertId == 4) { side =  0.5; vert = 0.0; }  // TR
    else                  { side =  0.5; vert = -1.0; } // BR

    float width = 0.0125;
    float streakLen = 0.2;

    vec3 pos = aPos;
    pos += camRight * side * width;
    pos.y += vert * streakLen;

    gl_Position = projection * view * vec4(pos, 1.0);
    vAlpha = aAlpha;
}
