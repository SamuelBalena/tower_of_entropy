# S001.7.1 — Antisserrilhado e estabilidade de imagem

## Técnica e integração

O menu **Antisserrilhado** oferece **Desligado** e **FXAA**. FXAA vem habilitado inicialmente nos três presets. A qualidade limita a busca a três, quatro ou cinco posições por direção, conforme Leve, Equilibrado e Alto. Método e intensidade podem ser escolhidos independentemente. Oculus conserva os ajustes do shader.

O `final.fsh` recebe o `colortex0` depois que Oculus termina os passes de composição do mundo, com saída para o framebuffer principal. No Oculus 1.8.0, o pipeline chama `FinalPassRenderer.renderFinalPass` na finalização do mundo. O coremod C002.2 compõe o personagem em `colortex0` antes dos pós-processamentos. O HUD do Minecraft é desenhado depois do passe final. Os alvos do SSR, o depth do mundo, as passagens de água e lava e a composição contextual do personagem seguem seus caminhos existentes. O shader usa a profundidade para pesar a cobertura, mantendo o tratamento subaquático e a gradação no final. [Programa final do Iris](https://shaders.properties/current/reference/programs/final/)

A implementação espacial testa contraste local, estima o eixo da borda na vizinhança 3 × 3 e busca os dois extremos do contorno. O filtro verifica o sinal do contraste e limita o deslocamento a menos de meio pixel. A segunda diferença de profundidade favorece silhuetas e reduz a intensidade nas texturas sobre um mesmo plano. Em regiões de baixo contraste ele retorna a amostra original. A técnica segue a família FXAA de Timothy Lottes com pesos próprios e seleção guiada por profundidade. [Descrição técnica do FXAA](https://developer.download.nvidia.com/assets/gamedev/files/sdk/11/FXAA_WhitePaper.pdf)

O filtro trabalha exclusivamente com o quadro atual. TAA, histórico temporal e SMAA ficam fora desta versão e do menu. O FXAA pode suavizar contornos de alta frequência, mas folhas, grades e objetos com cobertura subpixel ainda podem cintilar durante movimento. Capturas estáticas demonstram redução espacial e preservação de detalhes, com evidência de movimento separada.

Na S001.3, as cópias nas bordas vinham de amostras espaciais deslocadas, principalmente do bloom discreto a 4 ou 6 pixels. Nesta etapa, a busca do FXAA estima o contorno e o deslocamento da amostra final fica abaixo de meio pixel. Nos quadros capturados durante a rotação, as silhuetas mantiveram um contorno único. [Investigação da S001.3](S001_3_REMOVE_GHOSTING.md)

## Capturas reais

Mesma câmera, geometria e hora em 854 × 480. A rotação foi aplicada em todo o trecho medido, e os recortes foram ampliados em 3× por vizinho mais próximo. Folhas finas e degraus contra o céu exibem os contornos suavizados. O movimento e as partículas podem apresentar variação entre quadros.

| Cena | Desligado | FXAA |
| --- | --- | --- |
| Folhas, grama e cercas | [Captura](assets/s001_7_1/aa_forest-Desligado.png) | [Captura](assets/s001_7_1/aa_forest-FXAA.png) |
| Escadas, telhados e relevo | [Captura](assets/s001_7_1/aa_roof-Desligado.png) | [Captura](assets/s001_7_1/aa_roof-FXAA.png) |
| Personagem, armaduras e 36 mobs | [Captura](assets/s001_7_1/material_armor-Desligado.png) | [Captura](assets/s001_7_1/material_armor-FXAA.png) |
| Água | [Captura](assets/s001_7_1/material_water-Desligado.png) | [Captura](assets/s001_7_1/material_water-FXAA.png) |
| Lava | [Captura](assets/s001_7_1/material_lava-Desligado.png) | [Captura](assets/s001_7_1/material_lava-FXAA.png) |
| Estrelas | [Captura](assets/s001_7_1/aa_stars-Desligado.png) | [Captura](assets/s001_7_1/aa_stars-FXAA.png) |
| Rotação contínua | [Captura](assets/s001_7_1/aa_motion-Desligado.png) | [Captura](assets/s001_7_1/aa_motion-FXAA.png) |
| Vidro e transparência | [Captura](assets/s001_7_1/material_glass-Desligado.png) | [Captura](assets/s001_7_1/material_glass-FXAA.png) |

[Cerca desligado](assets/s001_7_1/aa_forest-recorte-Desligado.png) · [Cerca FXAA](assets/s001_7_1/aa_forest-recorte-FXAA.png) · [Telhado desligado](assets/s001_7_1/aa_roof-recorte-Desligado.png) · [Telhado FXAA](assets/s001_7_1/aa_roof-recorte-FXAA.png)

[Movimento desligado no tick 60](assets/s001_7_1/motion-60-Desligado.png) · [FXAA no tick 60](assets/s001_7_1/motion-60-FXAA.png) · [Desligado no tick 84](assets/s001_7_1/motion-84-Desligado.png) · [FXAA no tick 84](assets/s001_7_1/motion-84-FXAA.png)

## Performance

Cada célula registra FPS médio, 1% low, p95 e p99 em ms. Equipamento GTX 1050 Ti, VSync desligado e 8 chunks. Foram 48 ticks de aquecimento e 60 ticks de medição. As rodadas são individuais, e o servidor integrado influencia os tempos. O pedido de 1920 × 1080 retornou o framebuffer real indicado abaixo. A → B mede materiais, B → C mede FXAA e A → C mede o impacto combinado.

### 854 × 480

| Cena | S001.7 (A) | Materiais sem AA (B) | Materiais + FXAA (C) |
| --- | ---: | ---: | ---: |
| Floresta e cercas | 393 / 261 / 3,34 / 3,66 | 399 / 261 / 3,32 / 3,50 | 370 / 247 / 3,53 / 3,86 |
| Construções e relevo | 379 / 266 / 3,45 / 3,65 | 390 / 237 / 3,34 / 3,58 | 362 / 243 / 3,61 / 3,79 |
| Galeria metálica | 413 / 176 / 3,29 / 3,76 | 414 / 182 / 3,53 / 4,44 | 403 / 222 / 3,38 / 4,30 |
| Multidão | 211 / 146 / 5,71 / 6,32 | 198 / 134 / 6,51 / 7,22 | 208 / 148 / 6,09 / 6,46 |
| Água | 382 / 258 / 3,47 / 3,75 | 386 / 262 / 3,46 / 3,63 | 368 / 196 / 3,70 / 4,22 |
| Lava | 408 / 227 / 3,33 / 3,73 | 403 / 91 / 3,28 / 3,60 | 384 / 131 / 3,54 / 4,07 |
| Caverna | 393 / 263 / 3,49 / 3,75 | 396 / 250 / 3,46 / 3,84 | 376 / 215 / 3,81 / 4,40 |
| Rotação | 392 / 142 / 3,44 / 3,94 | 398 / 195 / 3,41 / 3,74 | 369 / 125 / 3,73 / 4,60 |

### 1280 × 720

| Cena | S001.7 (A) | Materiais sem AA (B) | Materiais + FXAA (C) |
| --- | ---: | ---: | ---: |
| Floresta e cercas | 203 / 89 / 6,84 / 8,31 | 202 / 106 / 7,07 / 8,00 | 190 / 78 / 7,33 / 8,99 |
| Construções e relevo | 196 / 95 / 6,94 / 8,37 | 197 / 75 / 7,09 / 8,38 | 187 / 99 / 7,44 / 8,35 |
| Galeria metálica | 207 / 145 / 6,27 / 6,78 | 209 / 143 / 6,26 / 6,65 | 201 / 146 / 6,11 / 6,48 |
| Multidão | 182 / 49 / 7,31 / 10,13 | 172 / 65 / 8,59 / 10,80 | 187 / 109 / 6,98 / 8,42 |
| Água | 194 / 144 / 6,19 / 6,62 | 194 / 142 / 6,20 / 6,77 | 187 / 140 / 6,32 / 6,83 |
| Lava | 209 / 162 / 5,85 / 6,08 | 209 / 154 / 5,87 / 6,26 | 199 / 156 / 5,96 / 6,25 |
| Caverna | 197 / 150 / 6,00 / 6,52 | 199 / 160 / 5,86 / 6,13 | 188 / 153 / 6,11 / 6,34 |
| Rotação | 203 / 96 / 5,88 / 6,29 | 204 / 107 / 5,89 / 6,14 | 194 / 100 / 6,06 / 6,42 |

### 1604 × 881

| Cena | S001.7 (A) | Materiais sem AA (B) | Materiais + FXAA (C) |
| --- | ---: | ---: | ---: |
| Floresta e cercas | 139 / 68 / 9,68 / 10,42 | 139 / 77 / 9,41 / 11,04 | 131 / 60 / 9,86 / 14,09 |
| Construções e relevo | 134 / 47 / 10,32 / 11,34 | 135 / 99 / 9,08 / 9,81 | 128 / 95 / 9,67 / 10,30 |
| Galeria metálica | 142 / 105 / 8,52 / 8,98 | 143 / 118 / 8,05 / 8,38 | 137 / 103 / 9,00 / 9,26 |
| Multidão | 133 / 52 / 9,60 / 12,93 | 138 / 103 / 8,68 / 9,47 | 133 / 75 / 9,19 / 10,18 |
| Água | 132 / 105 / 8,60 / 9,22 | 133 / 109 / 8,44 / 9,06 | 128 / 97 / 9,41 / 9,69 |
| Lava | 143 / 117 / 7,96 / 8,41 | 141 / 105 / 8,29 / 9,10 | 134 / 102 / 9,13 / 9,60 |
| Caverna | 133 / 103 / 8,50 / 9,04 | 134 / 86 / 8,76 / 9,68 | 125 / 96 / 9,40 / 9,76 |
| Rotação | 138 / 65 / 8,24 / 8,90 | 137 / 66 / 8,73 / 9,43 | 129 / 55 / 9,31 / 9,77 |

### Vila junto ao lago em 854 × 480

O roteiro dedicado atravessou a mesma sequência até a vila nas três variantes, com distância 8, VSync desligado e Equilibrado. Cada célula usa as mesmas unidades da matriz principal. Os picos do servidor integrado ainda contribuem para a dispersão de 1% low e p95.

| Cena | S001.7 (A) | Materiais sem AA (B) | Materiais + FXAA (C) |
| --- | ---: | ---: | ---: |
| Vila junto ao lago | 487 / 272 / 2,78 / 3,50 | 473 / 227 / 3,18 / 3,71 | 465 / 241 / 2,88 / 4,01 |

### Presets com FXAA em 720p

| Cena | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: |
| Floresta e cercas | 244 / 87 / 5,97 / 7,31 | 190 / 78 / 7,33 / 8,99 | 131 / 68 / 9,80 / 11,58 |
| Construções e relevo | 245 / 76 / 6,19 / 7,62 | 187 / 99 / 7,44 / 8,35 | 129 / 71 / 9,39 / 11,23 |
| Galeria metálica | 284 / 160 / 4,70 / 5,02 | 201 / 146 / 6,11 / 6,48 | 135 / 102 / 8,74 / 9,02 |
| Multidão | 204 / 103 / 6,78 / 8,78 | 187 / 109 / 6,98 / 8,42 | 132 / 69 / 8,82 / 9,05 |
| Água | 279 / 180 / 4,71 / 5,39 | 187 / 140 / 6,32 / 6,83 | 128 / 107 / 9,17 / 9,29 |
| Lava | 281 / 190 / 4,58 / 5,04 | 199 / 156 / 5,96 / 6,25 | 132 / 98 / 8,99 / 9,31 |
| Caverna | 256 / 180 / 4,81 / 5,17 | 188 / 153 / 6,11 / 6,34 | 126 / 88 / 9,25 / 10,22 |
| Rotação | 264 / 120 / 4,78 / 5,31 | 194 / 100 / 6,06 / 6,42 | 130 / 57 / 9,15 / 9,94 |

### GPU e VRAM

Médias de amostras globais do NVML na janela dos três segundos antes do resultado de quadro, incluindo processos do desktop.

| Framebuffer e cena | S001.7 | Materiais sem AA | Materiais + FXAA |
| --- | ---: | ---: | ---: |
| 854 × 480 · Floresta e cercas | 99% / 1417 MiB | 99% / 1437 MiB | 98% / 1414 MiB |
| 854 × 480 · Multidão | 58% / 1416 MiB | 54% / 1437 MiB | 57% / 1414 MiB |
| 854 × 480 · Água | 99% / 1416 MiB | 99% / 1437 MiB | 99% / 1414 MiB |
| 854 × 480 · Lava | 93% / 1417 MiB | 82% / 1436 MiB | 99% / 1414 MiB |
| 1280 × 720 · Floresta e cercas | 89% / 1450 MiB | 96% / 1436 MiB | 100% / 1432 MiB |
| 1280 × 720 · Multidão | 96% / 1461 MiB | 94% / 1469 MiB | 99% / 1460 MiB |
| 1280 × 720 · Água | 100% / 1462 MiB | 98% / 1469 MiB | 100% / 1463 MiB |
| 1280 × 720 · Lava | 100% / 1462 MiB | 100% / 1469 MiB | 100% / 1461 MiB |
| 1604 × 881 · Floresta e cercas | 97% / 1476 MiB | 100% / 1471 MiB | 96% / 1477 MiB |
| 1604 × 881 · Multidão | 100% / 1494 MiB | 100% / 1485 MiB | 99% / 1484 MiB |
| 1604 × 881 · Água | 100% / 1503 MiB | 100% / 1486 MiB | 100% / 1483 MiB |
| 1604 × 881 · Lava | 100% / 1492 MiB | 100% / 1494 MiB | 100% / 1482 MiB |

## Regressões

Os roteiros dedicados passaram com os shaders ativos. O de água incluiu seis cenários, com SSR de troncos e uma vila junto ao lago. A vila registrou 465 FPS médios e 2,88 ms no p95 a 854 × 480 nesta rodada. O de lava incluiu quatro cenários, e o de céu percorreu sete horários em uma bateria de 14 cenas. Óptica da água, lava, sol e lua, nuvens, iluminação e sombras coincidem byte a byte com a S001.7. [Reflexos do rio](assets/s001_7_1/river-ssr.png) · [Vila](assets/s001_7_1/village-water.png) · [Lava no Nether](assets/s001_7_1/lava-nether.png) · [Entardecer](assets/s001_7_1/sky-sunset.png) · [Noite](assets/s001_7_1/sky-night.png)

Com o mundo aberto, o roteiro partiu de Equilibrado a 854 × 480 e recarregou o Alto com FXAA a 1280 × 720. O log registrou o novo perfil, o novo framebuffer e a resolução de sombras 2048. O build Forge offline também passou. [Antes da troca](assets/s001_7_1/before-resize.png) · [Depois da troca](assets/s001_7_1/after-resize.png)

A suíte integrada de câmera e armaduras passou em 95 verificações, incluindo Classic e Slim, acabamento e glint, roll, combate, natação em oito direções e a composição Oculus a 12% no túnel com recuperação até 100% em espaço livre.

O comando `.\gradlew runClient` sincronizou o ZIP final byte a byte, carregou o shader no Oculus e abriu a galeria com o HUD nítido. [Galeria no runClient](assets/s001_7_1/main-hud.png)

O comando `.\gradlew runClient -PtoeNoOculus` abriu o menu do Forge 47.4.28 e encerrou com sucesso. [Menu sem Oculus](assets/s001_7_1/forge-native.png)

A suíte de parkour e combate aprovou 30 verificações com o shader ativo, incluindo sprint em oito direções, saltos, parede, beiral e clipes de ataque com armadura Polymesh R3. Uma tentativa anterior foi interrompida por um bloqueio do arquivo TOML de câmera no Windows. A repetição em uma instância de QA separada completou a suíte.

A aprovação final fica com o usuário.
