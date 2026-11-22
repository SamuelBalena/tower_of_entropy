# S002.2–S002.13 — Expansão gráfica

A expansão parte da Fantasy Foundation S002.1 em Minecraft Java 1.20.1, Forge 47.4.28, Oculus 1.8.0, Embeddium 0.3.31 e Java 17. As alterações de renderização ficam no shaderpack. Os novos comandos de QA ficam em `validation/s001` e usam cópias do mundo de teste.

## Marcos

| Marco | Resultado no shaderpack | Relatório |
| --- | --- | --- |
| S002.2 | Raios de sol no volume da neblina com visibilidade do shadow map | [Raios solares](S002_2_VOLUMETRIC_SUN_RAYS.md) |
| S002.3 | Oclusão e contato a partir da profundidade da geometria opaca | [Oclusão ambiente](S002_3_AMBIENT_OCCLUSION.md) |
| S002.4 | Umidade gradual, poças procedurais e ondulações | [Superfícies molhadas](S002_4_WET_SURFACES.md) |
| S002.5 | Vento no terreno vanilla e no mapa de sombras, incluindo vegetação aquática | [Vegetação](S002_5_DYNAMIC_VEGETATION.md) |
| S002.6 | Bloom seletivo de emissivos, lava e mapas LabPBR | [Bloom](S002_6_SELECTIVE_BLOOM.md) |
| S002.7 | Neblina regional do Nether e atmosfera fria do End | [Dimensões](S002_7_DIMENSION_ATMOSPHERES.md) |
| S002.8 | Parcial. Luz visual do item mais luminoso entre as duas mãos, aproximada à câmera | [Luz dinâmica](S002_8_DYNAMIC_LIGHTING.md) |
| S002.9 | PCF com raio variável estimado pelos bloqueadores | [Sombras](S002_9_ADVANCED_SHADOWS.md) |
| S002.10 | Nevasca com neblina fria, deriva de neve vanilla e redução de aurora | [Inverno](S002_10_WINTER_WEATHER.md) |
| S002.11 | Parcial. Fallback especular por ID para armaduras e ferramentas vanilla, incluindo a camada de armadura usada pelo R3 | [Armaduras](S002_11_ARMOR_PBR.md) |
| S002.12 | SSR para superfícies opacas elegíveis, com confiança espacial e contribuição ambiente | [Reflexos](S002_12_ADVANCED_REFLECTIONS.md) |
| S002.13 | SMAA 1x e FXAA mantidos, passes e buffers condicionais. TAA avaliado em nível de arquitetura | [Otimização](S002_13_GRAPHICS_OPTIMIZATION.md) |

## Organização dos passes

| Recurso | Uso |
| --- | --- |
| `colortex0` | Cor principal. O passe `deferred` aplica oclusão e reflexos antes da água e das transparências. |
| `colortex1`, `colortex2` | Bordas e pesos do SMAA, na ordem original dos passes. |
| `colortex3` | RGB da emissão, filtrado no resolve final. |
| `colortex4`, `colortex5` | Cópia da cena opaca e tabela de céu da água. |
| `colortex6` | Normal octaédrica, peso refletivo e profundidade de vista. |
| Profundidade | A expansão opaca lê `depthtex0` antes da translucência. A água continua lendo `depthtex1`. |
| Shadow map | Amostrado pelos raios e pelo filtro solar. O passe `shadow` deforma a vegetação com a mesma função do terreno. |

O Leve usa FXAA, raios com cinco amostras, oclusão com quatro amostras e umidade básica. O Equilibrado usa SMAA High, dez amostras de raios, oito de SSAO e SSR seletivo. O Alto usa SMAA Ultra, 16 amostras de raios, 12 de SSAO e maior orçamento de SSR. As traduções dos controles ficam em `lang/en_us.lang` e `lang/pt_br.lang`. [Referência de passes do Iris](https://shaders.properties/current/reference/shadersproperties/ordering/)

## Galerias reproduzíveis

Os nomes de cena podem ser passados para `benchmark-atmosphere.ps1 -Scenes`. O roteiro recria a geometria, posição da câmera, horário e clima nas cópias do save. As duas mãos, a sombra do personagem e a pose dependem do roteiro da cena.

| Área | Cenas |
| --- | --- |
| Floresta, raios, neblina matinal e copas | `fog_forest_dawn`, `fog_forest_noon`, `aa_forest` |
| Janela, interior de pedra, caverna e contato | `expansion_window`, `material_stone`, `cave`, `camera_tunnel` |
| Vila, água, rotação e caverna alagada | `waterside_village`, `water_cave`, `water_turn`, `water_submerged` |
| Chuva na pedra e na madeira, poças, plantas baixas, cultivo e plantas aquáticas | `expansion_dry`, `expansion_rain`, `expansion_wind` |
| Lanterna, emissivos da noite, lava e tocha em movimento | `expansion_window`, `material_emission`, `night_torches`, `material_lava`, `expansion_held_light` |
| Cinco biomas do Nether e o End | `expansion_nether_wastes`, `expansion_nether_crimson`, `expansion_nether_warped`, `expansion_nether_soul`, `expansion_nether_basalt`, `material_end` |
| Nevasca e aurora | `expansion_snow`, `expansion_snow_clear`, `aurora_snowy_plains` |
| Armaduras, equipamentos, metais, gelo e combate | `material_armor`, `material_mining`, `material_metals`, `material_ice`, `combat`, `aa_motion` |

Faça o build offline e o empacotamento a partir de `minecraft/`. Passe um diretório de instância de QA com `saves` e `options.txt` em `-Template`. A versão anterior pode ser obtida do commit `13f580356228984d39291b48fd06a3c5c48fbbe2`. O roteiro copia o save para cada execução.

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat --offline build
.\validation\s001\package-shader.ps1
.\validation\s001\benchmark-atmosphere.ps1 -Template $template -Baseline $baselineZip `
  -Variant Before,ExpansionOff,Leve,Equilibrado,Alto -Size 1280x720 `
  -Warmup 120 -Scenes 'fog_forest_dawn,expansion_rain,expansion_snow,material_end,material_armor,aa_motion' `
  -RunRoot '.logs/s002-expansion-check'
.\validation\s001\export-atmosphere-results.ps1 -RunRoot '.logs/s002-expansion-check' -Output '.logs/s002-expansion-check.csv'
```

Use `-Variant Equilibrado -EffectMatrix 'SUN_RAYS=fog_forest_dawn,SELECTIVE_BLOOM=expansion_window'` para pares ligado e desligado na mesma execução. `-DisableEffect` permite uma execução independente com uma opção desligada. O modo `-LabPbr` instala o resource pack demonstrativo na cópia do save. A exportação usa resolução efetiva, FPS, média do pior 1%, p95, p99, uso aproximado de GPU a 1 Hz e pico de VRAM quando disponíveis.

## Medições a 720p na primeira integração

A primeira rodada usou GTX 1050 Ti, framebuffer real de 1280 × 720, oito chunks, VSync desligado, 150 ticks de aquecimento e 60 de amostragem por cena. Cada modo percorreu 14 cenas. Os valores abaixo são FPS médio. O CSV identifica o SHA-256 de cada shaderpack e inclui 1% low, p95, p99, carga de GPU e VRAM. [Matriz inicial de 70 medições](S002_GRAPHICS_EXPANSION_720P_INITIAL.csv)

| Cena | S002.1 | Efeitos desligados | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: | ---: | ---: |
| Floresta ao amanhecer | 134,47 | 133,93 | 142,96 | 100,84 | 68,60 |
| Chuva na galeria | 129,98 | 130,69 | 140,23 | 102,26 | 71,95 |
| Armaduras e mobs | 159,25 | 159,88 | 166,14 | 126,41 | 87,53 |
| Rotação da câmera | 164,73 | 164,34 | 175,30 | 131,71 | 90,63 |

O pacote da primeira integração registrou cerca de 25% de redução de FPS na floresta e 21% na chuva no Equilibrado em relação à S002.1. O Alto usa todas as qualidades visuais elevadas ao mesmo tempo. As medições dessa tabela antecedem os ajustes finais no céu do End, na neve próxima e no descarte de passes e buffers desativados. A rodada inicial herdou a dimensão anterior na cena da vila. A coluna de dimensão conserva essa informação, e a galeria final encaminha os cenários de água e câmera para o Overworld.

## Versão do shaderpack entregue

O commit de renderização é `a822b9f32118079bc3e45ab965981b3c13ac9fb0`. O ZIP sincronizado tem 67 entradas conferidas byte a byte no diretório do shaderpack. O SHA-256 do ZIP é `E086EDCC3CA0771017A0B1285EFEB75DACC1EBD053B8D4208A8D64B5B1C0A317`.

A versão entregue teve 97 medições em 36 cenas distintas nos framebuffers de 854 × 480, 1280 × 720 e 1604 × 881. A matriz de cinco configurações cobre 14 cenas da integração inicial. As demais cenas têm cobertura por preset, por efeito ou por suíte de regressão, conforme o CSV. A solicitação de 1920 × 1080 produziu 1604 × 881 no framebuffer da máquina. A tabela usa FPS médio, 1% low, p95 e p99 para a mesma floresta ao amanhecer. [Medições da versão entregue](S002_GRAPHICS_EXPANSION_BENCHMARK.csv)

| Framebuffer | Preset | FPS | 1% low | p95 em ms | p99 em ms |
| --- | --- | ---: | ---: | ---: | ---: |
| 854 × 480 | Equilibrado | 179,51 | 123,28 | 7,579 | 7,934 |
| 1280 × 720 | Leve | 147,32 | 100,84 | 8,764 | 9,389 |
| 1280 × 720 | Equilibrado | 97,12 | 78,62 | 11,951 | 12,300 |
| 1280 × 720 | Alto | 66,99 | 41,87 | 16,596 | 18,464 |
| 1280 × 720 | Efeitos novos desligados | 134,34 | 101,61 | 9,181 | 9,575 |
| 1604 × 881 | Leve | 103,76 | 67,65 | 11,388 | 11,741 |
| 1604 × 881 | Equilibrado | 68,03 | 58,57 | 16,423 | 16,867 |
| 1604 × 881 | Alto | 47,02 | 41,71 | 23,416 | 23,845 |

No Equilibrado a 720p, a galeria de chuva registrou 98,28 FPS, p95 de 12,549 ms e p99 de 13,362 ms. A nevasca registrou 117,58 FPS, p95 de 9,998 ms e p99 de 10,669 ms. O Equilibrado na floresta teve 28% menos FPS que a versão entregue com os efeitos novos desligados. A repetição registrou 97,95 FPS na floresta e 100,43 FPS na chuva.

## Custo por efeito

Os 13 pares usaram o Equilibrado em 1280 × 720. O cliente recarregou o shader com a opção individual em zero e manteve o restante do preset. Cada janela usou 120 ticks de aquecimento e 60 de amostragem. A diferença de tempo médio por quadro é a diferença entre `1000 / FPS` nos dois estados. Cada par teve uma rodada, por isso valores pequenos ou negativos incluem variação de carga e de amostragem. [Métricas completas dos pares](S002_GRAPHICS_EXPANSION_BENCHMARK.csv)

| Efeito | Cena | FPS ligado | FPS desligado | Diferença em ms |
| --- | --- | ---: | ---: | ---: |
| Raios solares | fog_forest_dawn | 97,75 | 120,88 | +1,96 |
| SSAO | expansion_window | 120,70 | 125,42 | +0,31 |
| Sombras de contato | material_stone | 144,12 | 146,69 | +0,12 |
| Superfícies molhadas | expansion_rain | 100,86 | 101,89 | +0,10 |
| Reflexos de poças | expansion_rain | 98,71 | 98,14 | -0,06 |
| Vento da vegetação | expansion_dry | 104,81 | 105,90 | +0,10 |
| Bloom seletivo | expansion_window | 114,15 | 117,05 | +0,22 |
| Atmosfera dimensional | material_end | 540,23 | 517,33 | -0,08 |
| Luz segurada | expansion_held_light | 122,10 | 122,74 | +0,04 |
| Penumbra variável | fog_forest_dawn | 107,41 | 107,97 | +0,05 |
| Clima de inverno | expansion_snow | 113,72 | 116,56 | +0,21 |
| Materiais de equipamento | material_armor | 114,44 | 106,33 | -0,67 |
| Reflexos sólidos | material_metals | 105,54 | 108,77 | +0,28 |

Os raios solares tiveram o maior custo positivo na matriz de 13 pares. No Leve, a integração usa cinco amostras, contra dez no Equilibrado e 16 no Alto. A repetição com o piso próximo à câmera, dentro do alcance da tocha, mediu 149,54 FPS com luz segurada e 250,62 FPS sem ela, uma diferença de 2,70 ms por quadro naquela cena. [Tocha ligada](assets/s002_expansion/held-light-on.png) · [Desligada](assets/s002_expansion/held-light-off.png)

## Capturas

Na primeira integração, a câmera da floresta foi repetida nas cinco configurações. [S002.1](assets/s002_expansion/forest-Before.png) · [Efeitos desligados](assets/s002_expansion/forest-ExpansionOff.png) · [Leve](assets/s002_expansion/forest-Leve.png) · [Equilibrado](assets/s002_expansion/forest-Equilibrado.png) · [Alto](assets/s002_expansion/forest-Alto.png)

A versão candidata inclui o passe de céu texturizado do End e o controle de neve perto da câmera. [Janela e lanterna](assets/s002_expansion/expansion_window.png) · [Chuva](assets/s002_expansion/expansion_rain.png) · [Nevasca](assets/s002_expansion/expansion_snow.png) · [Céu do End](assets/s002_expansion/material_end.png) · [Água da vila no Overworld](assets/s002_expansion/waterside_village.png)

A primeira integração também gerou capturas da galeria de materiais. [Armaduras R3](assets/s002_expansion/initial-material_armor.png) · [Metais](assets/s002_expansion/initial-material_metals.png) · [Lava](assets/s002_expansion/initial-material_lava.png)

## Regressões e compatibilidade

O build Forge offline passou após as suítes de cliente. As inicializações padrão por `runClient` e `runClient -PtoeNoOculus` chegaram ao menu Forge e encerraram com código 0. O ZIP copiado para a instância de desenvolvimento tem o mesmo SHA-256 do pacote instalado. Parkour e combate concluíram 30 verificações. A suíte C002.1 com A001 concluiu 95 no Classic e 95 no Slim, incluindo natação, roll, trims, glint e troca de equipamento. A galeria de câmera registrou 12% de opacidade e as verificações em área aberta registraram recuperação para 100%. [Classic com trim e glint](assets/s002_expansion/classic-trim-glint.png) · [Slim com transparência contextual](assets/s002_expansion/slim-contextual-alpha.png)

O resource pack LabPBR Demo foi carregado na rodada de 10 cenas. Água submersa, rotação na água, materiais, aurora e as duas sequências de oito quadros foram capturados no cliente. A sequência de rotação é evidência visual de movimento e a inspeção de qualquer rastro temporal permanece parte da revisão artística. [Cena submersa](assets/s002_expansion/water_submerged.png) · [Emissivos com LabPBR](assets/s002_expansion/material_emission.png)

A última galeria concluiu as seis cenas de interface, iluminação noturna, mineração, gelo, natação, caverna inundada e câmera. O túnel no Overworld registrou 12% de opacidade e 572 composições na janela de amostragem. As telas do HUD e do inventário foram capturadas no cliente. [HUD](assets/s002_expansion/hud.png) · [Inventário](assets/s002_expansion/inventory.png) · [Chuva vista de um interior coberto](assets/s002_expansion/covered-rain.png)

## Limites da implementação

A luz segurada tem alcance curto e pode vazar junto a paredes. A integração de armaduras usa identificadores confiáveis do Oculus e uma resposta por material. O SSR e as sombras de contato dependem da geometria visível em tela. O sombreado de poças e a espessura da neblina usam aproximações de exposição pelo lightmap. O TAA requer vetores de movimento para vegetação, R3, fluidos e transparências para evitar acúmulo incorreto. O shader usa SMAA 1x e FXAA para esses caminhos.

Uma próxima otimização a medir é o acúmulo dos raios solares em resolução reduzida, com reconstrução sensível à profundidade. Para testar TAA, a prioridade técnica é obter vetores de movimento para a geometria deformada, as entidades R3 e as superfícies translúcidas.

A aprovação artística fica com o usuário.
