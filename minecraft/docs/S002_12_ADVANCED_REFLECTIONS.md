# S002.12 — Reflexos de superfícies sólidas

O terreno transmite normal octaédrica, peso de reflexão e profundidade de vista em `colortex6`. `solid_reflections.glsl` verifica a correspondência de profundidade e busca interseções com passos limitados e quatro refinamentos. A confiança cai na borda da tela, à distância e com erro de profundidade. O sombreado de materiais contém a contribuição ambiente caso a busca falhe. Projeção e reconstrução são compartilhadas com `water_optics.glsl`.

Objetos fora da tela, transparências e geometria invisível ao buffer opaco ficam fora do SSR sólido. A reflexão usa o mesmo quadro. A água usa seus próprios critérios de busca no caminho aprovado.

A galeria de QA usa `material_metals`, `material_ice`, `expansion_rain` e `waterside_village`. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

[Galeria metálica](assets/s002_expansion/initial-material_metals.png) · [Água na vila](assets/s002_expansion/waterside_village.png) · [Cena submersa](assets/s002_expansion/water_submerged.png)
