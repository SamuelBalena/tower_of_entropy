# S002.9 — Sombras suaves

`shadow.glsl` calcula a penumbra usando uma busca de quatro bloqueadores e a separação em profundidade. Estende o PCF existente com raio limitado e correção do plano receptor. A borda do mapa preserva o fade de alcance e a margem de amostragem.

O tamanho da penumbra é uma estimativa local. Os modos variável e amplo exigem filtro suave ou alto; a resolução continua ajustável. O desligamento da penumbra variável retorna ao filtro escolhido no menu de sombras.

A galeria de QA usa `fog_forest_dawn`, `expansion_window`, `expansion_wind` e `material_metals`. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)
