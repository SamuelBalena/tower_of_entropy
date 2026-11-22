# S002.2 — Raios solares volumétricos

`sun_rays.glsl` integra a visibilidade do sol ao longo do raio da câmera até a primeira superfície. Reutiliza as matrizes e o mapa de sombras do sol e pesa as amostras pela camada baixa da neblina S002.1, elevação solar, chuva e direção de observação. Os níveis usam 5, 10 e 16 amostras fixas no segmento.

A visibilidade termina no alcance do shadow map. A exposição por lightmap reduz o efeito no interior e já limita raios em cavernas profundas. A camada de água conserva seu caminho de fog original. No enquadramento de teste da floresta, a contribuição visual dos raios é discreta.

A galeria de QA usa `fog_forest_dawn`, `expansion_window`, `cave`. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

[Floresta com efeitos novos desligados](assets/s002_expansion/forest-ExpansionOff.png) · [Floresta no Equilibrado](assets/s002_expansion/forest-Equilibrado.png)
