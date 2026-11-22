# S002.3 — Oclusão ambiente e sombras de contato

`deferred.fsh` reconstrói posição pelo depth buffer. O SSAO estima a normal com o vizinho mais próximo de cada lado e usa 4, 8 ou 12 amostras em espiral. O raio, o limite de diferença de profundidade e a atenuação por distância seguram silhuetas e contraste. As sombras de contato lançam segmentos de até 0,705 bloco na direção solar no Overworld.

Apenas o passe opaco fornece profundidade para os dois efeitos. Oclusores fora da tela são desconhecidos e a precisão depende da distância, do plano de corte e da resolução. O SSAO aplica a visibilidade à cor opaca composta.

A galeria de QA usa `expansion_window`, `material_stone`, `material_armor`. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

[Galeria de pedra com janela, quinas e objetos apoiados](assets/s002_expansion/expansion_window.png)
