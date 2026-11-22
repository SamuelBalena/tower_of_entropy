# S002.11 — Materiais de armaduras e equipamentos

`item.properties` mapeia couro, malha, ferro, ouro, diamante e netherite. O `currentRenderedItemId` do Oculus seleciona rugosidade, cor especular e resposta da normal. O R3 passa armadura pela camada `HumanoidArmorLayer`, que o Oculus intercepta; o shader deixa o material original quando o ID desconhecido chega. A integração roda em `gbuffers_entities` e `gbuffers_hand`.

O material é um fallback por item, sem amostragem LabPBR de entidades. Os passes de skin, tint, glint e trims continuam usando a composição do jogo. Só armaduras e ferramentas mapeadas recebem a resposta.

A galeria de QA usa `material_armor`, `material_mining` e a suíte R3 Classic/Slim. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

[Galeria de armaduras](assets/s002_expansion/initial-material_armor.png)
