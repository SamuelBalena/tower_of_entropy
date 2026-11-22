# S002.4 — Chuva e umidade

`rain_surface.glsl` combina o `wetness` interpolado do Oculus com luz do céu, temperatura aproximada, orientação e material. Pedra, madeira, solos, metais e folhas respondem com diferentes níveis de absorção, rugosidade e brilho. Poças em superfícies minerais planas usam coordenadas do mundo, ondas locais e reflexo ambiente ou SSR. No nível Alto a água ganha uma ondulação curta de chuva.

A luz do céu é uma aproximação de cobertura. Superfícies sob telhados transparentes ou perto de aberturas podem receber umidade. A opção de umidade depende da qualidade de materiais do terreno. Poças são um efeito de superfície.

A galeria de QA usa `expansion_dry`, `expansion_rain`, `material_rain`, `water_rain`. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

[Chuva sobre madeira e pedra](assets/s002_expansion/expansion_rain.png)

[Interior coberto durante a chuva](assets/s002_expansion/covered-rain.png)
