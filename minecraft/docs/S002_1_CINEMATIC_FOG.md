# S002.1 — Neblina cinematográfica e profundidade atmosférica

## Arquitetura

O caminho anterior misturava a cor produzida por `lightingFog()` com o terreno em função da distância, além do recorte de distância do jogo. A névoa atmosférica toma o lugar dessa contribuição intermediária no Overworld e integra o fade de distância do motor em uma única mistura. Com **Desligada**, o shader volta ao caminho da S001.7.1. O caminho mantém a cor e o alpha da superfície separados, e preserva a absorção subaquática, a física e a interface.

A transmissão segue uma queda exponencial com profundidade óptica. Ela soma espalhamento por distância e uma aproximação da densidade baixa ao longo do raio entre câmera e superfície. Leve usa uma amostra, Equilibrado usa três pontos com quadratura de Simpson e Alto usa três pontos interiores. Nos dois presets superiores, a altura da camada varia suavemente no espaço do mundo. A formulação aproxima um meio tridimensional sem raymarch. Luz de céu reduz o efeito em cavernas. A curta distância mantém o contraste para exploração e combate.

## Região, horário e clima

A neblina matinal cresce durante a madrugada e começa a se dissipar pela manhã. A intensidade tem variação diária determinística. Planícies recebem uma névoa suave, florestas recebem um leve tom úmido e pântanos concentram mais densidade perto do terreno. Montanhas enfatizam a distância. Superfícies nevadas recebem um tom mais frio. Regiões secas usam uma camada baixa muito reduzida. Chuva resfria e reforça o espalhamento, e o sinal de trovoada eleva a extinção. À noite, a altura e a fase da lua modulam discretamente a cor do espalhamento.

**Neblina** oferece Desligada, Baixa, Média, Alta e Automática por bioma, com controles de intensidade e qualidade. O caminho regional fica restrito à superfície do Overworld vanilla. O relevo da camada é aproximado por uma faixa de altitude, com modulação espacial, e não segue o chão de cada caverna ou elevação individual. O fade próximo do horizonte deixa a aurora legível acima da névoa. A implementação não usa raios volumétricos com oclusão, sombras volumétricas ou iluminação de blocos pela aurora.

## Capturas e desempenho

A galeria mede a mesma geometria, câmera e configuração básica do shader. Os perfis usam oito chunks de distância na rodada de comparação.

| Cena | Desligada | Automática | Alta |
| --- | --- | --- | --- |
| Floresta ao amanhecer | [fog_forest_dawn-off.png](assets/s002_1/fog_forest_dawn-off.png) | [fog_forest_dawn-auto.png](assets/s002_1/fog_forest_dawn-auto.png) | [fog_forest_dawn-high.png](assets/s002_1/fog_forest_dawn-high.png) |
| Floresta ao meio-dia | [fog_forest_noon-off.png](assets/s002_1/fog_forest_noon-off.png) | [fog_forest_noon-auto.png](assets/s002_1/fog_forest_noon-auto.png) | [fog_forest_noon-high.png](assets/s002_1/fog_forest_noon-high.png) |
| Pântano | [fog_swamp-off.png](assets/s002_1/fog_swamp-off.png) | [fog_swamp-auto.png](assets/s002_1/fog_swamp-auto.png) | [fog_swamp-high.png](assets/s002_1/fog_swamp-high.png) |
| Vale de montanha | [fog_mountain-off.png](assets/s002_1/fog_mountain-off.png) | [fog_mountain-auto.png](assets/s002_1/fog_mountain-auto.png) | [fog_mountain-high.png](assets/s002_1/fog_mountain-high.png) |
| Planície ao amanhecer | [fog_plains_dawn-off.png](assets/s002_1/fog_plains_dawn-off.png) | [fog_plains_dawn-auto.png](assets/s002_1/fog_plains_dawn-auto.png) | [fog_plains_dawn-high.png](assets/s002_1/fog_plains_dawn-high.png) |
| Chuva | [fog_rain-off.png](assets/s002_1/fog_rain-off.png) | [fog_rain-auto.png](assets/s002_1/fog_rain-auto.png) | [fog_rain-high.png](assets/s002_1/fog_rain-high.png) |
| Trovoada | [fog_storm-off.png](assets/s002_1/fog_storm-off.png) | [fog_storm-auto.png](assets/s002_1/fog_storm-auto.png) | [fog_storm-high.png](assets/s002_1/fog_storm-high.png) |
| Caverna | [material_ores-off.png](assets/s002_1/material_ores-off.png) | [material_ores-auto.png](assets/s002_1/material_ores-auto.png) | [material_ores-high.png](assets/s002_1/material_ores-high.png) |
| Nether | [material_nether-off.png](assets/s002_1/material_nether-off.png) | [material_nether-auto.png](assets/s002_1/material_nether-auto.png) | [material_nether-high.png](assets/s002_1/material_nether-high.png) |
| End | [material_end-off.png](assets/s002_1/material_end-off.png) | [material_end-auto.png](assets/s002_1/material_end-auto.png) | [material_end-high.png](assets/s002_1/material_end-high.png) |

Referência do shader anterior na mesma galeria. [fog_mountain-baseline.png](assets/s002_1/fog_mountain-baseline.png)

O cliente integrado foi medido com GTX 1050 Ti, oito chunks e VSync desligado. Cada cena teve 48 ticks de aquecimento e 60 ticks de medição. A amostragem de GPU do `nvidia-smi` é aproximada, a 1 Hz. O CSV usa a média de uso da GPU e o pico de VRAM dentro da janela. Cada configuração teve uma rodada principal. A configuração de aurora isolada em 854 × 480 foi repetida depois do ajuste das cortinas.

A é o FXAA anterior, B é SMAA isolado, C é aurora isolada com FXAA, D é névoa isolada com FXAA, E combina aurora e névoa com FXAA, e F combina todos os efeitos com SMAA. A frequência da aurora foi fixada em 100% para garantir carga reproduzível na cena fria. A tabela abaixo usa FPS médio / 1% low / p95 ms / p99 ms.

| Framebuffer real | Cena | A | B | C | D | E | F |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 854 × 480 | Floresta e cercas | 356,22 / 107,25 / 4,15 / 5,66 | 349,10 / 147,23 / 4,15 / 4,86 | 315,35 / 108,88 / 5,21 / 6,71 | 282,63 / 83,41 / 5,45 / 7,05 | 321,28 / 117,96 / 4,71 / 6,13 | 324,53 / 115,68 / 4,50 / 5,84 |
| 854 × 480 | Snowy Plains à noite | 252,54 / 116,11 / 4,80 / 5,09 | 249,16 / 170,61 / 5,47 / 5,75 | 233,81 / 146,15 / 5,58 / 6,13 | 231,44 / 152,78 / 5,83 / 6,19 | 225,63 / 166,59 / 5,50 / 5,82 | 228,85 / 156,25 / 5,38 / 5,74 |
| 854 × 480 | Floresta ao amanhecer | 280,31 / 207,81 / 4,35 / 4,67 | 266,24 / 63,84 / 5,22 / 8,03 | 258,39 / 174,93 / 4,92 / 5,37 | 245,07 / 145,42 / 5,34 / 5,91 | 241,66 / 141,18 / 5,11 / 5,56 | 246,63 / 161,40 / 4,96 / 5,54 |
| 1280 × 720 | Floresta e cercas | 188,84 / 76,04 / 7,49 / 8,24 | 181,44 / 121,17 / 7,17 / 8,08 | 172,10 / 72,75 / 7,51 / 8,67 | 165,66 / 107,61 / 7,97 / 9,10 | 164,59 / 71,93 / 7,60 / 9,92 | 171,24 / 84,74 / 7,45 / 9,28 |
| 1280 × 720 | Snowy Plains à noite | 127,89 / 92,52 / 9,53 / 10,29 | 126,13 / 103,12 / 9,09 / 9,56 | 119,54 / 59,87 / 9,90 / 10,10 | 116,97 / 92,95 / 10,06 / 10,59 | 112,67 / 79,40 / 10,34 / 11,53 | 115,46 / 95,16 / 10,02 / 10,46 |
| 1280 × 720 | Floresta ao amanhecer | 147,62 / 105,43 / 8,38 / 8,69 | 143,73 / 116,79 / 7,88 / 8,31 | 138,57 / 106,18 / 8,81 / 9,10 | 128,14 / 104,35 / 9,28 / 9,56 | 126,66 / 53,40 / 9,20 / 9,60 | 130,40 / 97,05 / 9,15 / 9,80 |
| 1604 × 881 | Floresta e cercas | 130,06 / 63,00 / 9,78 / 11,07 | 124,22 / 66,53 / 9,90 / 12,28 | 118,52 / 85,33 / 10,59 / 11,28 | 113,74 / 82,16 / 10,68 / 11,53 | 111,51 / 39,58 / 11,28 / 12,31 | 117,46 / 61,77 / 10,62 / 11,94 |
| 1604 × 881 | Snowy Plains à noite | 87,24 / 76,58 / 12,82 / 12,99 | 85,86 / 70,02 / 13,16 / 13,71 | 81,56 / 55,32 / 14,32 / 15,46 | 80,08 / 64,38 / 13,85 / 14,10 | 77,51 / 69,07 / 14,07 / 14,45 | 78,99 / 61,36 / 14,20 / 15,16 |
| 1604 × 881 | Floresta ao amanhecer | 102,93 / 73,04 / 10,89 / 12,00 | 99,89 / 80,94 / 11,27 / 11,82 | 97,55 / 78,95 / 11,66 / 11,97 | 90,30 / 78,13 / 12,34 / 12,70 | 89,68 / 66,42 / 12,33 / 13,17 | 91,24 / 76,93 / 12,36 / 12,73 |

O CSV conserva as dez cenas de cada uma das 18 rodadas, incluindo vila à beira do lago, montanha, água, lava, caverna, multidão e céu noturno. A resolução efetiva pode ser menor que a solicitada. [Matriz completa com GPU e VRAM](S002_ATMOSPHERE_BENCHMARK.csv)

## Regressões e compatibilidade

O build Forge offline passou. A suíte de parkour e combate aprovou 30 verificações. A suíte de câmera e armaduras aprovou 95, incluindo Classic e Slim, glint, roll, natação em oito direções e composição do túnel a 12%, com recuperação para 100%.

Os roteiros existentes passaram em seis cenas de água, quatro de lava, 14 de céu e oito de materiais. A rodada de materiais carregou o recurso opcional LabPBR Demo. A cena submersa continuou usando a atenuação da água. [Cena submersa](assets/s002_1/underwater.png)

No mundo aberto, o Oculus recarregou do preset Equilibrado em 854 × 480 para Alto em 1280 × 720. O log confirmou SMAA, framebuffer e mapa de sombras 2048. As opções de qualidade AA, aurora e névoa ficaram gravadas com valor 3. [Antes da troca](assets/s002_1/preset-before.png) · [Depois da troca](assets/s002_1/preset-after.png)

A rodada a 16 chunks registrou o vale distante e o amanhecer nos ticks 23.980 e 24.020. O acúmulo antes do nascer do sol e a manhã seguinte usam a mesma fase diária de densidade, evitando um salto na virada do relógio. A vista acima das nuvens mostra a aurora e as estrelas separadamente. [Vale a 16 chunks](assets/s002_1/mountain-16chunks.png) · [Antes do nascer do sol](assets/s002_1/before-sunrise.png) · [Depois do nascer do sol](assets/s002_1/after-sunrise.png) · [Aurora acima das nuvens](assets/s002_1/aurora-above-clouds.png)

Os comandos `.\gradlew runClient` e `.\gradlew runClient -PtoeNoOculus` passaram. Na instância padrão, o ZIP sincronizado teve o mesmo SHA-256 do ZIP instalável. A captura do mundo mostra lua, estrelas e aurora com a mira e a barra de itens legíveis. A interface do inventário manteve seus contornos e texto. O comando sem Oculus chegou ao menu Forge 47.4.28. [Cliente padrão](assets/s002_1/runclient-aurora-hud.png) · [Inventário](assets/s002_1/runclient-inventory.png) · [Menu sem Oculus](assets/s002_1/runclient-no-oculus.png)

No túnel, o ensaio integrado registrou 12% de opacidade e 940 composições na janela de captura. Água, lava e combate continuaram em seus caminhos de renderização. [Túnel sem neblina](assets/s002_1/camera_tunnel-B.png) · [Túnel com neblina](assets/s002_1/camera_tunnel-F.png) · [Combate sem neblina](assets/s002_1/combat-B.png) · [Combate com neblina](assets/s002_1/combat-F.png) · [Vila e lago](assets/s002_1/waterside_village-F.png) · [Reflexos](assets/s002_1/material_water-F.png) · [Lava](assets/s002_1/material_lava-F.png) · [Estrelas](assets/s002_1/aa_stars-F.png)

A aprovação artística fica com o usuário.
