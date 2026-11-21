# S001 — Fantasy Shader Foundation

> A integração com o mod completo foi validada na [S001.1](S001_1_OCULUS_POLYMESH_COMPATIBILITY.md). As observações de incompatibilidade e de instalação isolada abaixo registram o estado original da S001.

## Shaderpack

A S001 é o primeiro shaderpack original do Tower of Entropy. A fundação combina luz quente no início e fim do dia, sombras que preservam a leitura do terreno e uma paleta viva com um toque violeta nas áreas menos iluminadas. Os passes usam as texturas, cores de bioma e geometria voxel existentes. O mesmo tratamento de cor se aplica aos três perfis.

- [Pasta editável](../shaderpacks/Tower%20of%20Entropy%20-%20Fantasy%20Foundation/)
- [ZIP para a pasta shaderpacks](../shaderpacks/Tower%20of%20Entropy%20-%20Fantasy%20Foundation.zip)

A geometria do terreno combina lightmap vanilla, incidência solar e mapa de sombras direcional. O céu interpola zênite e horizonte por horário, intensidade de chuva e posição do sol. A água combina a textura e luz vanilla com Fresnel. Os perfis mais altos acrescentam ondulação de normais e uma reflexão analítica do céu. O passe final faz antialiasing espacial, gradação, exposição e contraste. Bloom por amostragem de pontos brilhantes e profundidade de campo por distância ao centro da mira são opções do pack. A profundidade de campo sai desligada nos três perfis. Um ensaio complementar abriu o mundo com a opção ligada e renderizou a floresta com o passe ativo. [Captura com profundidade de campo](assets/s001/forest-dof.png) · [Log](../.logs/s001-dof.log)

| Perfil | Mapa de sombras | Amostras por teste de sombra | Água | Bloom |
| --- | ---: | ---: | --- | --- |
| Leve | 512² | 1 | Cor, Fresnel e horizonte | Desligado |
| Equilibrado | 1024² | 9 | Ondulação e horizonte | Sutil |
| Alto | 2048² | 25 | Ondulação e reflexão do gradiente do céu | Amostragem mais ampla |

O menu de opções do Oculus recebe os perfis e grupos em português e inglês. Exposição, saturação, contraste e intensidade mágica são independentes da configuração de qualidade. Os efeitos estão em `shaders/lib`, os passes em `shaders`, e as opções e traduções em `shaders.properties` e `shaders/lang`. O GLSL foi escrito para este projeto.

## Instalação

Use uma instância separada para a S001 com Minecraft Java 1.20.1, JDK 17 e Forge 47.4.28. Coloque Embeddium 0.3.31 e Oculus 1.8.0 em `mods`. Coloque o ZIP intacto em `shaderpacks`, selecione o pack no menu de shaders e escolha o perfil em suas opções. O manifesto do Oculus 1.8.0 fixa Minecraft 1.20.1 e declara Embeddium de cliente a partir de 0.3.1 como dependência opcional. O par usado no ensaio inclui Embeddium 0.3.31. O perfil usa somente essa implementação de otimização de terreno. [Forge 47.4.28](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) · [Embeddium 0.3.31](https://www.curseforge.com/minecraft/mc-mods/embeddium/files/5681725) · [Oculus 1.8.0](https://www.curseforge.com/minecraft/mc-mods/oculus/files/6020952) · [Formato de opções Iris](https://github.com/IrisShaders/docs/blob/main/src/content/docs/current/Reference/Shaders.Properties/shader_settings.mdx)

O ZIP contém `shaders/` na raiz e usa separadores portáteis. Para reconstruí-lo em `minecraft/` com JDK 17:

```powershell
$pack = 'shaderpacks/Tower of Entropy - Fantasy Foundation'
jar --create --file "$pack.zip" --no-manifest -C $pack shaders
```

## Compatibilidade com Tower of Entropy

**Oculus 1.8.0 e a C002.2 atual falham juntos durante a inicialização.** A C002.2 usa um coremod para ampliar de 12 para 16 posições o cache de vínculos de textura do Blaze3D. O mixin `iris$increaseMaximumAllowedTextureUnits` do Oculus também transforma o inicializador de `GlStateManager`. Ele procura o literal 12 com `require=1` e tenta substituí-lo por 128. O Forge aplica o coremod da C002.2 antes desse mixin, e o Oculus aborta por encontrar zero ocorrências elegíveis. A execução falhou antes de carregar o pack ou abrir um mundo. Selecionar Fast, Fancy ou Fabulous e desligar o pack no menu não evita uma falha de mixin nessa etapa. [Log da reprodução](../.logs/s001-smoke-retry.log) · [Coremod da C002.2](../src/main/resources/coremods/player_fade.js)

A inspeção do Oculus 1.7.0 encontrou o mesmo literal 12 e a mesma exigência de uma injeção. Esse artefato foi inspecionado em bytecode. [Oculus 1.7.0](https://www.curseforge.com/minecraft/mc-mods/oculus/files/5299671)

A análise do JAR Oculus também mostrou que `ExtendedShader.apply` vincula seus framebuffers antes ou depois dos translúcidos. A C002.2 captura a silhueta do jogador em um alvo próprio e, no Fabulous, injeta a composição na cadeia vanilla. Mesmo com o conflito de inicialização resolvido, esses dois caminhos ainda exigiriam um ensaio específico para detectar rebind do alvo durante o flush, validar teste de profundidade e confirmar que o alpha entra uma vez na composição. São pontos para verificação posterior, pois a execução integrada nunca chegou a essa fase.

Os testes gráficos da S001 executam Forge, Embeddium e Oculus em uma fixture standalone. O renderer do mundo é o dessa pilha, e a fixture usa armadura vanilla para medir equipamento e desenhar trim e glint. Classic/Slim, animações, A001, mira, combate e natação com Oculus ativo permanecem bloqueados na integração. O teste standalone com água não representa a locomoção omnidirecional do R3. A execução separada com o mod completo e sem Oculus passou no Fancy. O R3 desenhou a armadura em espaço aberto com opacidade 1,0. No túnel obteve 0,12 com 827 desenhos de armadura e 827 composições durante a amostra. Ao sair, chegou a 1,0 e o contador de composição ficou parado. As cinco suítes locais de movimento horizontal, locomoção, arbitragem de ações, combate e Roll passaram. [Túnel](assets/s001/integrated-camera_tunnel.png) · [Saída do túnel](assets/s001/integrated-camera_restore.png) · [Log sem Oculus](../.logs/s001-integrado-delivery.log)

## Medições em jogo

Computador de QA em Windows 11, Intel Core i5-10400F, NVIDIA GeForce GTX 1050 Ti de 4 GB, driver 582.28, OpenGL 4.6, JDK 17, resolução 854 × 480 e distância de renderização de oito chunks. O VSync ficou desligado. A opção de limite de quadros foi configurada no máximo e as leituras acima de 260 FPS confirmaram que o teto efetivo ficou livre.

Cada perfil usou uma cópia nova do mesmo save, com câmera, horário e geometria reproduzidos pela fixture. A primeira rodada mostrou efeitos da reutilização de um save e dos itens descartados durante as transições. A rodada da tabela usa saves novos, desliga os drops de blocos e limpa itens da área. O servidor integrado deixa 48 ticks de aquecimento antes de medir cada cena por 60 ticks. O formato de cada célula é **FPS médio / 1% low em FPS / p95 de frame time em ms**. O 1% low é o inverso do tempo médio do 1% de quadros mais lentos.

| Cena | Desligado | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: | ---: |
| Floresta | 795 / 163 / 2,09 | 409 / 135 / 4,20 | 371 / 111 / 4,64 | 339 / 87 / 5,03 |
| Planície | 1321 / 308 / 1,11 | 669 / 259 / 2,36 | 619 / 227 / 2,97 | 554 / 211 / 3,04 |
| Caverna | 1383 / 340 / 0,94 | 734 / 284 / 1,81 | 734 / 286 / 1,89 | 665 / 266 / 2,22 |
| Superfície da água | 1460 / 403 / 0,84 | 697 / 281 / 2,00 | 730 / 274 / 1,81 | 416 / 157 / 3,82 |
| Debaixo d'água | 1347 / 356 / 1,12 | 832 / 331 / 1,53 | 839 / 294 / 1,46 | 833 / 301 / 1,45 |
| Noite com tochas | 1476 / 432 / 0,80 | 823 / 311 / 1,46 | 818 / 298 / 1,49 | 799 / 297 / 1,54 |
| 60 mobs | 753 / 239 / 1,92 | 320 / 156 / 4,74 | 313 / 140 / 4,85 | 338 / 191 / 4,56 |
| Armadura | 1023 / 383 / 1,30 | 614 / 287 / 2,05 | 532 / 211 / 3,08 | 540 / 216 / 2,41 |
| Corredor | 1070 / 380 / 1,32 | 593 / 271 / 2,33 | 602 / 223 / 2,03 | 524 / 261 / 2,55 |
| Saída do corredor | 1226 / 452 / 0,99 | 637 / 301 / 1,93 | 580 / 221 / 2,86 | 574 / 271 / 2,23 |
| Entardecer | 1568 / 459 / 0,78 | 710 / 262 / 2,09 | 737 / 275 / 1,66 | 699 / 329 / 1,81 |
| Chuva | 907 / 301 / 1,31 | 548 / 220 / 2,28 | 514 / 206 / 2,88 | 524 / 220 / 2,63 |

Todas as colunas usam Forge, Embeddium e Oculus no mesmo roteiro de renderização. Na coluna Desligado, o Oculus está carregado com o shaderpack inativo. A cena Corredor mede a geometria com câmera vanilla na fixture standalone. A opacidade contextual foi medida na execução separada do mod completo sem Oculus. Foram 48 amostras com dados completos. Na floresta, o FPS médio caiu de 795 com o pack desligado para 409 no Leve, 371 no Equilibrado e 339 no Alto. O resultado por cena varia com a carga e o agendamento de quadros. Os números representam cenas pequenas em resolução de QA e uma rodada por perfil.

- [Log desligado](../.logs/s001-clean-off.log)
- [Log Leve](../.logs/s001-clean-leve.log)
- [Log Equilibrado](../.logs/s001-clean-equilibrado.log)
- [Log Alto](../.logs/s001-clean-alto.log)
- [Código da fixture](../validation/s001/java/com/towerofentropy/client/qa/ShaderBenchmark.java)

## Capturas

A câmera da floresta foi fixa para comparar paleta e sombreamento entre os quatro estados. São capturas diretas do framebuffer do jogo.

| Desligado | Leve | Equilibrado | Alto |
| --- | --- | --- | --- |
| [PNG](assets/s001/forest-off.png) | [PNG](assets/s001/forest-leve.png) | [PNG](assets/s001/forest-equilibrado.png) | [PNG](assets/s001/forest-alto.png) |

- [Entardecer no Leve](assets/s001/golden_hour-leve.png)
- [Chuva no Equilibrado](assets/s001/rain-equilibrado.png)
- [Superfície da água no Alto](assets/s001/water-alto.png)
- Visibilidade subaquática. [Desligado](assets/s001/underwater-off.png) · [Alto](assets/s001/underwater-alto.png)
- [Caverna no Equilibrado](assets/s001/cave-equilibrado.png)
- [Noite com tochas no Alto](assets/s001/night_torches-alto.png)
- [Armadura vanilla com trim no Alto](assets/s001/armor-alto.png)

## Build e empacotamento

O build Forge offline passou, incluindo `reobfJar`, em um diretório separado de auditoria. A inspeção confirmou os 16 arquivos de modelos e poses e zero classes de QA. O JAR distribuído anteriormente permaneceu com SHA-256 `1F4F77C4CF747363315A4A048341EEBFF370BF844BF4A215F20E83C2D4D11887`. O ZIP S001 tem SHA-256 `8B155490F9981D2AD773A8601438C8C10425BC099BC186F76E037ABCE7B059BD`. [Build Forge](../.logs/s001-build.log) · [Auditoria](../.logs/s001-jar-audit.log)

## Limitações e próximos passos

A reflexão da água usa o céu. Reflexos da geometria, refração de cena e reflexos em espaço de tela ficam para outra etapa. A sombra direcional usa profundidade opaca e recortes de alpha das superfícies. O antialiasing espacial reduz arestas no quadro atual. Artefatos temporais, custo em resoluções maiores, outros drivers e dimensões fora do Overworld precisam de testes próprios.

Antes da S002 integrada, dedicar um teste de compatibilidade à propriedade do cache de textura e ao alvo de composição do Oculus. O roteiro de aceitação precisa incluir a armadura e os itens do R3, o par minério/12% e espaço aberto/100%, além de medir água e partículas em profundidade. Para a S002 artística, separar as paletas de céu, névoa e água por bioma e interpolá-las nas fronteiras. O perfil Leve deve continuar como referência visual e de custo.
