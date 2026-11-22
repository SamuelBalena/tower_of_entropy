# S002.7 — Atmosferas do Nether e do End

`dimension_atmosphere.glsl` usa a cor de neblina informada pelo Minecraft por bioma no Nether e um perfil violeta moderado no End. `gbuffers_skytextured` aplica o céu frio ao cubo do End. `skydome.glsl` seleciona o céu dimensional antes da lógica de nuvens, sol, lua e aurora do Overworld.

A paleta do Nether acompanha o bioma da câmera. O efeito altera dispersão e céu; a iluminação direta usa os sinais de luz já fornecidos pelo jogo.

A galeria de QA usa `expansion_nether_wastes`, `expansion_nether_crimson`, `expansion_nether_warped`, `expansion_nether_soul`, `expansion_nether_basalt` e `material_end`. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

O Oculus criou o pipeline de cada dimensão durante o ensaio. O corredor manteve a mesma geometria, luzes, tempo e câmera nos cinco biomas. [Nether Wastes](assets/s002_expansion/nether-wastes.png) · [Crimson Forest](assets/s002_expansion/nether-crimson.png) · [Warped Forest](assets/s002_expansion/nether-warped.png) · [Soul Sand Valley](assets/s002_expansion/nether-soul.png) · [Basalt Deltas](assets/s002_expansion/nether-basalt.png) · [End](assets/s002_expansion/material_end.png)
