# S002.8 — Luz dinâmica

`dynamic_light.glsl` usa os dois níveis `heldBlockLightValue` do Oculus e aplica a contribuição do item mais luminoso perto da câmera. A queda inclui distância, orientação, intensidade do item e luz de bloco existente. O cálculo é visual no shader cliente.

A fonte é aproximada à câmera. A cor usa a informação do item no Oculus e recorre ao âmbar quando o carregador fornece uma cor neutra. O shader carece de posição de fontes remotas, de oclusão geométrica da lanterna e de informações de projéteis. Em terceira pessoa pode haver deslocamento aparente e pode ocorrer vazamento junto a paredes próximas. Outras luzes dinâmicas de mods devem ser comparadas separadamente.

A galeria de QA usa `expansion_held_light` com a tocha na mão e caminhada automatizada; `night_torches` para a contribuição estática. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

O cliente confirmou a tocha equipada antes da caminhada. O par com o piso em primeiro plano mostra a queda de luz junto à câmera. [Luz ligada](assets/s002_expansion/held-light-on.png) · [Luz desligada](assets/s002_expansion/held-light-off.png)
