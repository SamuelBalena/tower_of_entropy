# S001.7.3 — Antisserrilhado avançado

## Pipeline e técnica

O Oculus compõe o mundo, as transparências e o Polymesh antes dos passes composite. O SMAA 1x lê a cor do mundo em `colortex0`. O passe `composite` grava bordas de luminância em `colortex1` (RG8, limpo a cada quadro), e `composite1` grava pesos de mistura em `colortex2` (RGBA8). `final` resolve a vizinhança usando a cor original e os pesos. Cada passe escreve em um alvo distinto daqueles que lê. Os alvos 4 e 5 seguem disponíveis para a água. A absorção subaquática e a gradação continuam depois da resolução. O Minecraft desenha sua interface depois do passe final. [Passes do Iris](https://shaders.properties/current/reference/programs/overview/)

O código GLSL de referência e as tabelas binárias R8/RG8 vêm do SMAA oficial, revisão `71c806a838bdd7d517df19192a20f0c61b3ca29d`. O fonte foi normalizado para UTF-8 e sem espaços finais. A licença MIT segue no diretório `shaders/lib/smaa`. A integração detecta bordas com contraste local, busca os extremos, identifica segmentos diagonais, usa as áreas de cobertura pré-calculadas e aplica os pesos conforme a direção dominante no pixel. A métrica recebe as dimensões efetivas do framebuffer. [SMAA de referência](https://github.com/iryoku/smaa/tree/71c806a838bdd7d517df19192a20f0c61b3ca29d)

**Antisserrilhado** oferece Desligado, FXAA e SMAA 1x. Leve seleciona FXAA, Equilibrado seleciona SMAA High e Alto seleciona SMAA Ultra. O controle de intensidade do FXAA atua no modo FXAA. O seletor de método também pode ser usado manualmente, com armazenamento pelo Oculus.

A S001.3 removia rastros espaciais causados por amostras deslocadas e bloom. Este resolve usa pesos de cobertura apenas em contornos identificados. Não há histórico. Um TAA estável exigiria reprojeção e vetores confiáveis também para o Polymesh, transparência e geometria animada. O SMAA 1x ainda está sujeito à cintilação de detalhes subpixel em movimento. [Investigação da S001.3](S001_3_REMOVE_GHOSTING.md)

## Capturas e avaliações

As amostras de 854 × 480 foram produzidas no Forge com o mesmo roteiro. Os dois recortes de cerca foram ampliados por vizinho mais próximo.
[FXAA anterior, recorte 3×](assets/s001_7_3/fence-fxaa-3x.png) · [SMAA, recorte 3×](assets/s001_7_3/fence-smaa-3x.png)

| Cena | Desligado | FXAA | SMAA |
| --- | --- | --- | --- |
| aa_forest | [aa_forest-off.png](assets/s001_7_3/aa_forest-off.png) | [aa_forest-fxaa.png](assets/s001_7_3/aa_forest-fxaa.png) | [aa_forest-smaa.png](assets/s001_7_3/aa_forest-smaa.png) |
| aa_roof | [aa_roof-off.png](assets/s001_7_3/aa_roof-off.png) | [aa_roof-fxaa.png](assets/s001_7_3/aa_roof-fxaa.png) | [aa_roof-smaa.png](assets/s001_7_3/aa_roof-smaa.png) |
| aa_motion | [aa_motion-off.png](assets/s001_7_3/aa_motion-off.png) | [aa_motion-fxaa.png](assets/s001_7_3/aa_motion-fxaa.png) | [aa_motion-smaa.png](assets/s001_7_3/aa_motion-smaa.png) |
| material_armor | [material_armor-off.png](assets/s001_7_3/material_armor-off.png) | [material_armor-fxaa.png](assets/s001_7_3/material_armor-fxaa.png) | [material_armor-smaa.png](assets/s001_7_3/material_armor-smaa.png) |
| material_water | [material_water-off.png](assets/s001_7_3/material_water-off.png) | [material_water-fxaa.png](assets/s001_7_3/material_water-fxaa.png) | [material_water-smaa.png](assets/s001_7_3/material_water-smaa.png) |
| material_lava | [material_lava-off.png](assets/s001_7_3/material_lava-off.png) | [material_lava-fxaa.png](assets/s001_7_3/material_lava-fxaa.png) | [material_lava-smaa.png](assets/s001_7_3/material_lava-smaa.png) |
| aa_stars | [aa_stars-off.png](assets/s001_7_3/aa_stars-off.png) | [aa_stars-fxaa.png](assets/s001_7_3/aa_stars-fxaa.png) | [aa_stars-smaa.png](assets/s001_7_3/aa_stars-smaa.png) |

Na cerca, o SMAA suavizou os degraus diagonais no encontro com o céu e conservou a textura no interior da madeira. Os quadros da rotação permitem conferir as silhuetas com a câmera em movimento. Elementos finos continuam sujeitos à mudança de cobertura entre quadros.
[motion-60-fxaa.png](assets/s001_7_3/motion-60-fxaa.png) · [motion-60-smaa.png](assets/s001_7_3/motion-60-smaa.png)
[motion-84-fxaa.png](assets/s001_7_3/motion-84-fxaa.png) · [motion-84-smaa.png](assets/s001_7_3/motion-84-smaa.png)
Telhado a 3× por vizinho mais próximo. [roof-off-3x.png](assets/s001_7_3/roof-off-3x.png) · [roof-fxaa-3x.png](assets/s001_7_3/roof-fxaa-3x.png) · [roof-smaa-3x.png](assets/s001_7_3/roof-smaa-3x.png)

## Desempenho e validação

GTX 1050 Ti, 854 × 480, oito chunks, VSync desligado, 48 ticks de aquecimento e 60 ticks medidos. A comparação isola o antisserrilhado. Cada célula traz FPS médio, 1% low, p95 e p99 em ms.

| Cena | FXAA anterior | SMAA 1x |
| --- | ---: | ---: |
| Floresta e cerca | 356,22 / 107,25 / 4,15 / 5,66 | 349,10 / 147,23 / 4,15 / 4,86 |
| Personagem e armaduras | 201,80 / 101,68 / 5,93 / 6,25 | 191,30 / 78,89 / 6,71 / 8,09 |
| Vila junto ao lago | 464,08 / 270,36 / 2,87 / 3,15 | 446,21 / 200,79 / 3,05 / 3,90 |
| Água | 374,80 / 258,59 / 3,39 / 3,68 | 359,23 / 218,44 / 4,07 / 4,41 |
| Lava | 396,18 / 235,96 / 3,33 / 4,09 | 376,26 / 167,39 / 3,73 / 4,48 |

No recorte da floresta a média caiu cerca de 2%, e o p95 ficou em 4,15 ms nos dois modos. A vila caiu cerca de 4%. O servidor integrado e a compilação de chunks influenciam principalmente o 1% low. A documentação de integração registra as três dimensões efetivas, GPU, VRAM e as regressões. [Matriz integrada](S002_1_CINEMATIC_FOG.md)

A aprovação artística fica com o usuário.
