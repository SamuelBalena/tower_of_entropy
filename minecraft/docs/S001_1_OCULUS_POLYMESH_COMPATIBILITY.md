# S001.1 — Oculus, Embeddium e Polymesh R3

## Causa e reprodução

A falha original ocorre na inicialização de `com.mojang.blaze3d.platform.GlStateManager`, antes do menu. O coremod da C002.2 trocava o literal 12 por 16 no limite superior de `IntStream.range`, usado para criar o cache de vínculos de textura. O mixin `MixinGlStateManager.iris$increaseMaximumAllowedTextureUnits` do Oculus 1.8.0 exige exatamente uma ocorrência do literal 12 e tenta elevá-lo a 128. Como o coremod executa primeiro, o mixin encontra zero. A exceção observada é `MixinTransformerError` causada por `InjectionError` de `@ModifyConstant`. A pilha chega a `GLX.getOpenGLVersionString` durante `Minecraft.<init>`. A revisão do ponto de falha encontrou as duas transformações no mesmo inicializador, com a assinatura e o literal exatos, sem participação de access transformers ou de campos de classes do Polymesh. [Log original e stack trace](../.logs/s001-smoke-retry.log) · [Relatório da S001](S001_FANTASY_SHADER_FOUNDATION.md)

A matriz também reproduziu a falha com o pack desligado. Esse cliente de QA restaurou a troca do literal apenas na cópia gerada de `player_fade.js` e obteve a mesma `InjectionError` antes do menu. A tarefa de recursos foi rodada em seguida para retornar à versão corrigida. [F com transformação antiga](../.logs/s0011-pre-f-off.log) · [Restauração dos recursos de QA](../.logs/s0011-restore-resources.log)

A primeira reprodução com apenas o ajuste do cache chegou ao mundo e carregou o ZIP da S001. Essa etapa isolou a inicialização. Logo depois, a inspeção das classes do Oculus mostrou que `ExtendedShader` e `FallbackShader` chamam `GlFramebuffer.bind` no `apply` e retornam ao alvo principal no `clear`. Isso pode desviar a saída da captura privada ou fazer a composição direta da C002.2 chegar a um alvo que o passe final do shader vai sobrescrever. [Primeira execução após o ajuste do cache](../.logs/s0011-probe.log) · [Bytecode do ExtendedShader](../.logs/s0011-ExtendedShader.txt) · [Bytecode do FallbackShader](../.logs/s0011-FallbackShader.txt)

A primeira ponte registrou chamadas de composição, mas a comparação de 28.016 pixels centrais produziu zero diferença entre 12% e alpha zero. A leitura OpenGL mostrou na captura RGBA `37 148 173 255` e profundidade `0,9203`, com profundidade da cena `0,9853`. O destino ficou com RGBA `19 83 84 255` antes e depois. A inspeção do `MixinShaderInstance` explicou o resultado. Enquanto `shouldOverrideShaders()` é verdadeiro, o Oculus aciona `DepthColorStorage.disableDepthColor` em shaders vanilla que não são shaders do pipeline de mundo do Oculus.

A ponte final compõe logo após o próprio Oculus definir `isRenderingWorld=false` e antes de chamar `CompositeRenderer.renderAll`. Com essa ordem, o pixel do mesmo destino mudou de `19 83 84 255` para `21 91 95 255` com 12% de opacidade e profundidade ainda `0,9853`. O erro OpenGL registrado foi zero. A leitura síncrona de depuração foi retirada da implementação distribuída. [Antes da correção](../.logs/s0011-gpu-probe-retry.log) · [Depois da correção](../.logs/s0011-gpu-fixed.log) · [Bytecode da máscara](../.logs/s0011-iris-depthcolor.txt)

O ensaio inicial com apenas o ajuste do cache registrou uma falha em `vertical_down`. A asserção foi mantida e passou na última rodada com o shader ativo e na rodada Fabulous. Uma tentativa de leitura diagnóstica também terminou por falta de memória nativa da JVM. A leitura foi repetida de forma isolada e os resultados usados acima vieram dessa repetição. [Falha de memória no ensaio diagnóstico](../.logs/s0011-gpu-probe/hs_err_pid18760.log)

## Implementação

- `src/main/resources/coremods/player_fade.js` mantém o literal 12 e acrescenta `Math.max(capacidade, 16)` depois dele. Com Oculus, o mixin mantém seu ponto de injeção e a capacidade fica em 128. Sem Oculus, a C002.2 conserva os 16 vínculos necessários ao Fabulous.
- `src/main/resources/coremods/player_fade_oculus.js` acrescenta pontos de integração específicos para a estrutura do Oculus 1.8.0. Durante a captura do jogador, o shader de entidade continua ativo enquanto a ponte direciona seus desenhos ao alvo privado. A passagem final usa o framebuffer de cor 0 e profundidade da fase após translúcidos. O próprio Oculus cria, atualiza e libera esse framebuffer com os outros alvos da pipeline.
- `src/main/java/com/towerofentropy/client/ContextualPlayerLayer.java` envolve o desenho local já existente, captura cor e profundidade de corpo, A001 e itens juntos, e restaura os vínculos de framebuffer. Na finalização do mundo do Oculus, compõe a silhueta uma vez com alpha premultiplicado, teste de profundidade e escrita de profundidade desabilitada. A composição ocorre antes dos passes finais do pack, portanto passa pela mesma gradação da S001. A rota do Oculus fica inativa quando seu shader de mundo está desligado. As rotas vanilla de Fast, Fancy e Fabulous permanecem disponíveis.
- `src/main/resources/META-INF/coremods.json` registra a ponte. O código Java de produção não carrega classes do Oculus nem depende de reflexão para renderizar. A ponte usa a infraestrutura do Forge em memória. A implementação preserva os arquivos JAR de terceiros e o GLSL da S001.
- `validation/s001/forge.init.gradle` seleciona dependências opcionais somente nos clientes de QA. `ShaderBenchmark.java` reproduz as cenas, registra p95, p99, FPS e contadores de captura, e produz a referência do túnel com alpha 0, 12% e 100%. `CompareS0011Fade.ps1` verifica as três capturas por projeção da cor, e a fixture C002.2 exercita também partículas e o encaminhamento ao Oculus.

## Matriz de isolamento

| Caso | Configuração | Resultado observado |
| --- | --- | --- |
| A | Forge + Embeddium | Menu, mundo e floresta renderizada. [Log](../.logs/s0011-pure-a.log) |
| B | Forge + Embeddium + Oculus, pack desligado | Menu, mundo e floresta renderizada. [Log](../.logs/s0011-pure-b.log) |
| C | Forge + Embeddium + Oculus + S001 | Menu, mundo e floresta renderizada. Oculus reconheceu o Leve. [Log](../.logs/s0011-pure-c.log) |
| D | Forge + Tower of Entropy | Mundo, R3, armadura e transparência nativa até 12%, com retorno a 100%. [Log](../.logs/s0011-d-native.log) |
| E | Forge + Tower of Entropy + Embeddium | Mundo, R3, armadura e rota nativa de 12% a 100%. [Log](../.logs/s0011-e-embeddium.log) |
| F | Forge + Tower of Entropy + Embeddium + Oculus, pack desligado | A transformação antiga falhou antes do menu. A versão corrigida chegou ao mundo, renderizou a armadura e percorreu a rota nativa, com zero chamadas à ponte do Oculus. [Antes](../.logs/s0011-pre-f-off.log) · [Depois](../.logs/s0011-f-off.log) |
| G | Forge + Tower of Entropy + Embeddium + Oculus + S001 | A configuração original falhou antes do menu. Com a ponte final, todos os presets chegaram ao mundo e compuseram armadura e corpo a 12% no túnel e 100% na saída. [Antes](../.logs/s001-smoke-retry.log) · [Leve](../.logs/s0011-g-leve-final.log) · [Equilibrado](../.logs/s0011-g-equilibrado.log) · [Alto](../.logs/s0011-g-alto.log) |

A, B e C usam uma fixture mínima e excluem o código, os recursos, CPM e a biblioteca Polymesh do classpath. D a G usam o mod completo, incluindo os renderizadores e sistemas já aprovados.

## Capturas e validação visual

| Mesmo túnel, preset Leve | Captura |
| --- | --- |
| 0% de referência | [PNG](assets/s001_1/leve-camera_tunnel_clear.png) |
| 12% com o R3 e diamante | [PNG](assets/s001_1/leve-camera_tunnel.png) |
| 100% de referência | [PNG](assets/s001_1/leve-camera_tunnel_opaque.png) |
| Saída do túnel a 100% | [PNG](assets/s001_1/leve-camera_restore.png) |
| Diamante com trim e glint | [PNG](assets/s001_1/leve-armor.png) |

| Preset | Mapa de sombras | Pixels com diferença versus 0% | Estimativa pela projeção da cor |
| --- | ---: | ---: | ---: |
| Leve | 512² | 28.016 / 28.016 | 11,95% |
| Equilibrado | 1024² | 28.016 / 28.016 | 11,90% |
| Alto | 2048² | 28.016 / 28.016 | 11,88% |

A opacidade configurada e registrada pelo jogo foi 0,12. A projeção da cor é uma confirmação aproximada após a gradação do shader. Os três perfis deixaram o minério visível e registraram restauração a 1,0. Na amostra do Leve, foram 892 composições e 892 quadros contabilizados pelo desenho visível. A passagem de profundidade das sombras explica os 1.784 desenhos do R3 na mesma amostra. [Equilibrado no túnel](assets/s001_1/equilibrado-camera_tunnel.png) · [Alto no túnel](assets/s001_1/alto-camera_tunnel.png) · [Saída no Alto](assets/s001_1/alto-camera_restore.png)

A execução Slim com S001 ativa passou em **95/95**. Exercitou skin com recortes alpha, troca Classic/Slim durante a sessão, armaduras completas e mistas, couro tingido, trim, glint, arco, Roll, item translúcido, natação nas oito direções, retorno à terra e troca de perspectiva. Também confirmou que todas as composições contabilizadas na janela do túnel usaram a rota Oculus. A captura com partículas mostra a silhueta, a mira e o minério no mesmo quadro, com poeira magenta próxima e poeira ciano atrás do personagem. [Log Slim](../.logs/s0011-qa-slim.log) · [Partículas no túnel](assets/s001_1/slim-oculus_particles.png) · [Natação diagonal](assets/s001_1/slim-swim-1.png) · [Couro tingido](assets/s001_1/slim-dyed_leather.png) · [Trim e glint](assets/s001_1/slim-trim_glint.png) · [Troca de skin](assets/s001_1/slim-skin-swap.png)

## Desempenho

Computador de QA com Intel Core i5-10400F, GTX 1050 Ti de 4 GB, driver NVIDIA 582.28, JDK 17, Windows 11, resolução 854 × 480 e oito chunks de distância. VSync desligado. A fixture usa cópias do mesmo mundo, mesma câmera e 48 ticks de aquecimento antes de registrar 60 ticks por cena. As células mostram **FPS médio / 1% low em FPS / p95 em ms / p99 em ms**. Esses números valem para as cenas pequenas e fixas da fixture.

| Cena | Sem loader | Embeddium | Oculus desligado | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Floresta | 827 / 183 / 1,91 / 4,58 | 711 / 109 / 2,44 / 7,47 | 587 / 110 / 2,99 / 7,60 | 284 / 90 / 6,07 / 9,92 | 192 / 63 / 10,74 / 13,74 | 303 / 87 / 5,69 / 8,39 |
| Caverna | 1279 / 301 / 1,21 / 2,68 | 1780 / 433 / 0,73 / 2,12 | 1435 / 392 / 0,85 / 2,31 | 569 / 225 / 2,53 / 3,78 | 579 / 235 / 2,54 / 3,73 | 567 / 237 / 2,42 / 3,78 |
| Água | 1160 / 368 / 1,09 / 2,62 | 1464 / 401 / 0,93 / 2,14 | 1116 / 350 / 1,25 / 2,56 | 449 / 172 / 3,65 / 4,86 | 588 / 276 / 2,22 / 3,33 | 609 / 248 / 2,27 / 3,51 |
| 60 mobs | 283 / 163 / 5,30 / 5,58 | 308 / 164 / 4,60 / 5,42 | 733 / 274 / 1,87 / 3,10 | 300 / 162 / 5,01 / 5,50 | 288 / 135 / 4,98 / 6,24 | 305 / 156 / 4,72 / 5,47 |
| Armadura | 667 / 266 / 1,95 / 3,58 | 899 / 348 / 1,54 / 2,58 | 685 / 309 / 2,04 / 2,84 | 233 / 74 / 5,73 / 6,25 | 245 / 134 / 5,59 / 6,61 | 247 / 105 / 5,31 / 6,41 |
| Combate | 473 / 204 / 3,16 / 4,29 | 631 / 235 / 2,38 / 3,68 | 536 / 267 / 2,45 / 3,53 | 210 / 111 / 6,54 / 7,90 | 223 / 147 / 6,08 / 6,65 | 209 / 118 / 6,66 / 7,89 |
| Corredor | 541 / 166 / 3,40 / 4,08 | 897 / 276 / 1,41 / 2,54 | 775 / 237 / 1,78 / 2,73 | 298 / 185 / 4,88 / 5,24 | 303 / 192 / 4,54 / 4,93 | 303 / 198 / 4,60 / 4,94 |

| Configuração | CPU R3 por desenho | Captura | Composição |
| --- | ---: | ---: | ---: |
| Sem loader | 0,617 ms | 0,704 ms | 0,016 ms |
| Embeddium | 0,327 ms | 0,393 ms | 0,014 ms |
| Oculus desligado | 0,379 ms | 0,457 ms | 0,014 ms |
| Leve | 0,779 ms | 0,610 ms | 0,027 ms |
| Equilibrado | 0,742 ms | 0,620 ms | 0,027 ms |
| Alto | 0,755 ms | 0,620 ms | 0,026 ms |

Foram 90 combinações de configuração e cena com dados completos. A cena de combate enviou pedidos reais de ataque ao servidor integrado. O corredor mostrou o custo do shader ativo, com 298 a 303 FPS médios, ante 541 sem carregador adicional e 775 com Oculus e o pack desligado. O resultado por cena variou entre rodadas. A amostra de um percurso por configuração não estabelece uma ordem fixa entre os presets.

[Sem loader](../.logs/s0011-d-native.log) · [Embeddium](../.logs/s0011-e-embeddium.log) · [Oculus desligado](../.logs/s0011-f-off.log) · [Leve](../.logs/s0011-g-leve-final.log) · [Equilibrado](../.logs/s0011-g-equilibrado.log) · [Alto](../.logs/s0011-g-alto.log)

A CPU do R3 inclui o trabalho de geometria e emissão de cada desenho. Com a S001 ativa, o render padrão de sombras faz uma passagem adicional para o mapa de sombras. A composição de opacidade continua uma vez por quadro visível, com o alvo de cor e profundidade do jogador. Os dados de CPU da captura e composição cobrem as seções de código instrumentadas e incluem o envio dos comandos, sem sincronização deliberada para cronometrar a GPU.

## Regressões e artefatos

- Classic com diamante e S001 ativa passou em **30/30** no ensaio de movimento, sprint em oito direções, parkour e golpes. O R3 registrou os clipes de walk, jog, sprint, salto, pouso, Wall Slide, Ledge Hang, Ledge Climb e jab/cross com desenho da armadura. [Log](../.logs/s0011-parkour.log) · [Sprint](assets/s001_1/classic-sprint.png)
- O teste rápido em Fast sem carregadores registrou 12%, com 474 desenhos da armadura e 474 composições nativas na janela do túnel. Fancy foi exercitado na matriz D. [Log Fast](../.logs/s0011-fast-native.log)
- Fabulous sem Oculus passou em **94/94**, com a composição da C002.2, armadura, 12% e restauração, Roll, natação e troca de skin. [Log](../.logs/s0011-qa-fabulous.log) · [Minério em Fabulous](assets/s001_1/fabulous-ore_fade.png)
- As suítes locais de movimento horizontal, locomoção, arbitragem de ações, combate e Roll passaram. O diagnóstico W002.4 permaneceu ativo e seus contadores de renderização e combate foram registrados nos logs.

- A regressão ampla sem Oculus obteve **50/50** na W001, **36/36** na W001.1 e **29/29** em expansão e parkour. [Log](../.logs/s0011-regression.log)

- A suíte original W002 em Classic e com a S001 ativa passou em **59/59**, incluindo estado da arma, combate, Roll, câmera e mineração. [Log](../.logs/s0011-combat-active.log)
- A suíte original C002 com a S001 ativa passou em **26/26**. Ela verificou colisão, ombros, zoom, ataque aéreo, Roll, primeira pessoa e restauração de perspectiva. [Log](../.logs/s0011-camera-active.log)

As suítes em jogo somaram **419 checagens aprovadas**, além das 90 amostras de desempenho.

O build Forge offline passou em `reobfJar`. A auditoria confirmou 16/16 arquivos de modelos e poses e zero classes de QA ou bibliotecas do Oculus/Embeddium incorporadas ao JAR. Os dois carregadores continuam opcionais. A instalação integrada usa o JAR atualizado junto às versões de cliente abaixo e ao ZIP original em `shaderpacks`.

- [JAR Forge atualizado](../build/libs/towerofentropy-0.0.1.jar) · [Build](../.logs/s0011-build.log) · [Auditoria](../.logs/s0011-jar-audit.log)
- [Embeddium 0.3.31](https://www.curseforge.com/minecraft/mc-mods/embeddium/files/5681725) · [Oculus 1.8.0](https://www.curseforge.com/minecraft/mc-mods/oculus/files/6020952)

SHA-256 do JAR `FBDA63A942DE82BDE80FCFDC115615D659E5DA21F6A69620B0DC9DEF39767304`.

SHA-256 do ZIP preservado `8B155490F9981D2AD773A8601438C8C10425BC099BC186F76E037ABCE7B059BD`.

## Escopo de compatibilidade

A combinação validada usa Minecraft 1.20.1, Java 17, Forge 47.4.28, Embeddium 0.3.31, Oculus 1.8.0 e o ZIP original da S001. O pack usa uma saída de cor de mundo por passe. Shaderpacks de terceiros com múltiplos buffers de materiais, composição própria de partículas, profundidade invertida ou outras convenções exigem validação específica. O teste de profundidade usa a superfície disponível na pipeline do Oculus. Transparências que não gravam profundidade não oferecem ordenação exata por pixel nessa rota.

O ensaio ativo usa as opções gráficas efetivas do Oculus. Fabulous vanilla é validado em execução sem Oculus, com sua própria cadeia. Essas duas execuções exercitam pipelines diferentes.

A configuração validada usa a S001 no Overworld. Testes com troca de pack durante a sessão, mudanças de escala e resolução, outras dimensões ou GPUs e dois clientes reais ficam pendentes. Uma futura adaptação a outros packs precisa validar buffers de materiais e ordenação de translucências sem profundidade.

[ZIP S001 original](../shaderpacks/Tower%20of%20Entropy%20-%20Fantasy%20Foundation.zip) · [Pasta editável da S001](../shaderpacks/Tower%20of%20Entropy%20-%20Fantasy%20Foundation/)
