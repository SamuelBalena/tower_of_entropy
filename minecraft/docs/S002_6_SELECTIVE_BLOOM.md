# S002.6 — Bloom seletivo

`colortex3` recebe emissão explícita de lava, fontes vanilla classificadas e texels LabPBR emissivos. `bloom.glsl` filtra nove posições locais da máscara após o resolve espacial e antes da gradação. A cor do quadro anterior fica fora do cálculo.

Materiais emissivos pintados no atlas precisam de ID vanilla mapeado ou mapa LabPBR. O raio do brilho é pequeno e medido em pixels. A máscara acompanha os passes de geometria que a escrevem.

A galeria de QA usa `expansion_window`, `material_emission`, `material_lava`, `aa_motion`. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

[Lanterna](assets/s002_expansion/expansion_window.png) · [Galeria de emissivos](assets/s002_expansion/material_emission.png) · [Lava](assets/s002_expansion/initial-material_lava.png)
