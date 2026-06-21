#version 330 core

in float vAlpha;
out vec4 FragColor;

void main()
{
    // Realistic rain: slightly blue-grey, semi-transparent
    FragColor = vec4(0.6, 0.65, 0.75, vAlpha * 0.4);
}
