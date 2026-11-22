# S002.5 — Vegetação e vento

`vegetation.glsl` calcula um campo contínuo de duas oscilações e rajadas em coordenadas do mundo, com aumento de intensidade sob chuva e trovão. O mesmo deslocamento é aplicado ao terreno e ao shadow pass. Os IDs 11201–11207 distinguem plantas baixas, copas, flores e culturas, vinhas, vegetação aquática, hastes e plantas duplas. O terreno e o mapa de sombras aplicam as mesmas funções.

Os IDs são extensíveis via `block.properties`. Formas personalizadas precisam de um grupo com âncora compatível. O comportamento depende da informação de centro de bloco `at_midBlock`. O movimento atual altera vértices; as normais geométricas de base são usadas na iluminação.

A galeria de QA usa `expansion_dry`, `expansion_wind`, `aa_forest`; duas capturas com a mesma câmera ajudam a conferir a animação. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

A câmera ficou fixa entre os dois instantes. [Vento no tick 100](assets/s002_expansion/wind-100.png) · [Vento no tick 120](assets/s002_expansion/wind-120.png)
