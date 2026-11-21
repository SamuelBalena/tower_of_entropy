# S001.7.2 — Materiais universais vanilla

## Inventário

O cliente Forge 1.20.1 exportou `BuiltInRegistries.BLOCK` após carregar os modelos e os sprites vanilla. O coletor visita os estados possíveis, consulta o `RenderShape`, os layers do modelo, a luz máxima dos estados, fluidos, block entities e sprites com mais de um quadro. O classificador aplica regras de família só a nomes presentes no registry. [Inventário CSV](S001_7_2_VANILLA_BLOCKS.csv)

| Cobertura | Registros |
| --- | ---: |
| Total examinado | 1.003 |
| Renderizáveis ou associados a um fluido | 997 |
| Modelos com família específica | 787 |
| Modelos com dielétrico genérico | 76 |
| Renderer especial, sem modelo de terreno | 131 |
| Água, lava e coluna de bolhas | 3 |
| Técnicos excluídos do mundo visível | 6 |

Os 863 modelos incluem 606 sólidos, 218 com recorte alfa, 38 translúcidos e o tripwire, que tem layer próprio. Há 163 registros com block entity, incluindo os 131 sem modelo de terreno. O inventário marca 36 sprites animados encontrados em modelos, 315 registros com estado que carrega fluido e 88 com algum estado emissivo. Animações de modelos especiais passam pelos seus renderers. Air, cave air, void air, barrier, light e structure void ficam excluídos por ausência de geometria regular no mundo. Marcadores de edição e partículas da interface seguem seus caminhos vanilla.

| ID | Família | Registros |
| --- | --- | ---: |
| 11101 | Pedra e minerais | 71 |
| 11102 | Alvenaria, tijolos e prismarine | 101 |
| 11103 | Madeira | 157 |
| 11104 | Ferro e componentes ferrosos | 21 |
| 11105 | Ouro | 2 |
| 11106 | Cobre e variantes pouco oxidadas | 16 |
| 11107 | Matriz de minérios e metais brutos | 22 |
| 11108 | Solos e materiais pulverulentos | 36 |
| 11109 | Obsidiana e gelo sólido | 4 |
| 11110 | Fibras, carpetes e terracota | 49 |
| 11111 | Vegetação | 158 |
| 11112 | Vidro, gelo translúcido e outros dielétricos transparentes | 38 |
| 11113 | Emissão constante | 16 |
| 11113 somente em `lit=true` | Emissão controlada pelo estado | 39 |
| 11114 | Cobre com pátina | 16 |
| 11115 | Concreto e cerâmica vitrificada | 32 |
| 11116 | Materiais cristalinos e densos | 9 |

Os IDs 11001, 11002 e 11003 continuam reservados aos fluidos. A coluna de bolhas continua desenhando o estado de água do jogo. Um launcher de QA com preferências iniciais já salvas pode reexportar o inventário com `-PtoeMaterialInventory`. O comando de geração abaixo é executado na pasta `minecraft`.

```powershell
.\gradlew --offline -I validation/s001/forge.init.gradle runClient -PtoeS001Embeddium -PtoeNoOculus -PtoeMaterialInventory '-PtoeS001Bench=.logs/vanilla-inventory'
.\validation\s001\classify-vanilla-materials.ps1 -Registry '.logs/vanilla-inventory/vanilla-blocks-runtime.csv'
```

## Resposta dos materiais

O terreno usa uma normal geométrica, rugosidade de 0,84 e F0 de 0,025 para o dielétrico genérico. A resposta usa albedo, luz difusa, Fresnel e um destaque deliberadamente fraco. Famílias opacas escolhem propriedades por identidade, inclusive patina, tecidos e vegetação. A opacidade alfa do recorte continua no mesmo local. O fallback também atende blocos de mods sem classificação. Se o atlas LabPBR 1.3 estiver disponível, seus mapas válidos têm precedência. Normais detalhadas continuam dependendo de `_n`. O pack S001.7 continua opcional e sua seleção permanece do usuário. [Formato LabPBR 1.3](https://shaderlabs.org/wiki/LabPBR_Material_Standard)

O caminho translúcido aplica um brilho angular fraco aos IDs dielétricos conhecidos sem alterar alpha, ordem ou render targets. Os efeitos translúcidos especiais e IDs desconhecidos mantêm o lightmap desse caminho. A água passa antes do material translúcido. A lava passa antes da avaliação sólida. As entidades de bloco que chegam em `gbuffers_block` ganham uma contribuição angular discreta com sua própria textura. Sinais, baús, banners, camas, shulkers, sinos e partes móveis preservam a geometria, tint e o despacho real de cada renderer. Passes especializados de portais e componentes que não passam em `gbuffers_block` mantêm a resposta correspondente. Os materiais do terreno não são aplicados a pele ou armaduras do Polymesh.

A emissão permanente e a marcada pelo estado dão um piso à cor do próprio material. O block light vanilla continua governando os arredores. Redstone desativada, minério e blocos sem ID emissivo não recebem esse piso. O specular LabPBR usa a emissão do mapa quando há dados para o texel. Nenhum mapa adicional é obrigatório.

O padrão nas placas metálicas se parecia com shadow acne. Aumentar o bias e isolar os termos solar e do céu não resolveu. O ID derivado de `mc_Entity` chegava interpolado e era comparado por igualdade exata, então alguns fragmentos trocavam de família dentro de um triângulo. A nova leitura arredonda o ID sólido e usa uma janela de meio ID no translúcido. A comparação a 720p manteve o shader de sombras original e eliminou o padrão observado. [Antes](assets/s001_7_2/metals-original-720.png) · [ID corrigido](assets/s001_7_2/metals-corrected-720.png)

## Capturas e desempenho

Mesma câmera, hora e geometria por coluna. FXAA foi desligado para isolar a resposta do material. As capturas LabPBR usam o pack opcional com os mesmos IDs. Os painéis de vidro, emissores e block entities mostram seus caminhos separados. O pack usa albedo próprio apenas nos dois minérios da demonstração.

| Cena | S001.7 | Materiais | Desativados | LabPBR |
| --- | --- | --- | --- | --- |
| Pedra | [Captura](assets/s001_7_2/material_stone-S0017.png) | [Captura](assets/s001_7_2/material_stone-Materiais.png) | [Captura](assets/s001_7_2/material_stone-Desativados.png) | [Captura](assets/s001_7_2/material_stone-LabPBR.png) |
| Madeira | [Captura](assets/s001_7_2/material_wood-S0017.png) | [Captura](assets/s001_7_2/material_wood-Materiais.png) | [Captura](assets/s001_7_2/material_wood-Desativados.png) | [Captura](assets/s001_7_2/material_wood-LabPBR.png) |
| Ferro e ouro | [Captura](assets/s001_7_2/material_metals-S0017.png) | [Captura](assets/s001_7_2/material_metals-Materiais.png) | [Captura](assets/s001_7_2/material_metals-Desativados.png) | [Captura](assets/s001_7_2/material_metals-LabPBR.png) |
| Minérios e caverna | [Captura](assets/s001_7_2/material_ores-S0017.png) | [Captura](assets/s001_7_2/material_ores-Materiais.png) | [Captura](assets/s001_7_2/material_ores-Desativados.png) | [Captura](assets/s001_7_2/material_ores-LabPBR.png) |
| Gelo e neve | [Captura](assets/s001_7_2/material_ice-S0017.png) | [Captura](assets/s001_7_2/material_ice-Materiais.png) | [Captura](assets/s001_7_2/material_ice-Desativados.png) | [Captura](assets/s001_7_2/material_ice-LabPBR.png) |
| Sol lateral | [Captura](assets/s001_7_2/material_rake-S0017.png) | [Captura](assets/s001_7_2/material_rake-Materiais.png) | [Captura](assets/s001_7_2/material_rake-Desativados.png) | [Captura](assets/s001_7_2/material_rake-LabPBR.png) |
| Vidro, mel e gelo | [Captura](assets/s001_7_2/material_glass-S0017.png) | [Captura](assets/s001_7_2/material_glass-Materiais.png) | [Captura](assets/s001_7_2/material_glass-Desativados.png) | [Captura](assets/s001_7_2/material_glass-LabPBR.png) |
| Emissores à noite | [Captura](assets/s001_7_2/material_emission-S0017.png) | [Captura](assets/s001_7_2/material_emission-Materiais.png) | [Captura](assets/s001_7_2/material_emission-Desativados.png) | [Captura](assets/s001_7_2/material_emission-LabPBR.png) |
| Baú, sino e outros renderers | [Captura](assets/s001_7_2/material_block_entities-S0017.png) | [Captura](assets/s001_7_2/material_block_entities-Materiais.png) | [Captura](assets/s001_7_2/material_block_entities-Desativados.png) | [Captura](assets/s001_7_2/material_block_entities-LabPBR.png) |
| Nether | [Captura](assets/s001_7_2/material_nether-S0017.png) | [Captura](assets/s001_7_2/material_nether-Materiais.png) | [Captura](assets/s001_7_2/material_nether-Desativados.png) | [Captura](assets/s001_7_2/material_nether-LabPBR.png) |
| End | [Captura](assets/s001_7_2/material_end-S0017.png) | [Captura](assets/s001_7_2/material_end-Materiais.png) | [Captura](assets/s001_7_2/material_end-Desativados.png) | [Captura](assets/s001_7_2/material_end-LabPBR.png) |
| Solos, concreto e terracota | [Captura](assets/s001_7_2/material_soils-S0017.png) | [Captura](assets/s001_7_2/material_soils-Materiais.png) | [Captura](assets/s001_7_2/material_soils-Desativados.png) | [Captura](assets/s001_7_2/material_soils-LabPBR.png) |
| Água próxima | [Captura](assets/s001_7_2/material_water-S0017.png) | [Captura](assets/s001_7_2/material_water-Materiais.png) | [Captura](assets/s001_7_2/material_water-Desativados.png) | [Captura](assets/s001_7_2/material_water-LabPBR.png) |
| Lava próxima | [Captura](assets/s001_7_2/material_lava-S0017.png) | [Captura](assets/s001_7_2/material_lava-Materiais.png) | [Captura](assets/s001_7_2/material_lava-Desativados.png) | [Captura](assets/s001_7_2/material_lava-LabPBR.png) |

A comparação usa 854 × 480 e FXAA desligado. Cada célula registra FPS médio / 1% low / p95 / p99. GTX 1050 Ti, VSync desligado, 8 chunks e 60 ticks de amostragem depois de 48 ticks de aquecimento.

| Cena | S001.7 | Materiais | Desativados | LabPBR |
| --- | ---: | ---: | ---: | ---: |
| Galeria metálica | 413 / 176 / 3,29 / 3,76 | 414 / 182 / 3,53 / 4,44 | 411 / 164 / 3,72 / 4,96 | 392 / 125 / 3,62 / 4,86 |
| Multidão | 211 / 146 / 5,71 / 6,32 | 198 / 134 / 6,51 / 7,22 | 189 / 72 / 7,13 / 9,25 | 208 / 131 / 6,43 / 7,09 |
| Caverna | 393 / 263 / 3,49 / 3,75 | 396 / 250 / 3,46 / 3,84 | 418 / 255 / 3,37 / 3,71 | 385 / 250 / 3,44 / 3,85 |
| Água | 382 / 258 / 3,47 / 3,75 | 386 / 262 / 3,46 / 3,63 | 397 / 249 / 3,27 / 3,65 | 376 / 240 / 3,45 / 3,79 |
| Lava | 408 / 227 / 3,33 / 3,73 | 403 / 91 / 3,28 / 3,60 | 430 / 275 / 3,04 / 3,53 | 402 / 237 / 3,23 / 3,81 |

O ZIP intermediário de materiais passou por oito cenas no Forge com Oculus, incluindo metais, armaduras, água, lava e rotação. A geração do inventário e do mapeamento também foi reproduzida byte a byte a partir da exportação do cliente.

A aprovação final fica com o usuário.
