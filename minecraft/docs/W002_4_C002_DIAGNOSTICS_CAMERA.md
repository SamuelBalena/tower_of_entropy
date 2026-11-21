# W002.4 + C002 — Diagnóstico de desenvolvimento e câmera

## Diagnóstico de desenvolvimento

O padrão é determinado no início da JVM pelo ambiente Forge. Os perfis de desenvolvimento do ForgeGradle iniciam com coleta ativa no primeiro jogador conectado. A execução de um JAR numa instalação normal do Forge inicia com coleta desativada. A opção JVM `-Dtoe.combat.diagnostics.default=false` desativa a seleção automática também no desenvolvimento. Use `true` para ativá-la explicitamente em uma instalação normal. No ForgeGradle, a mesma opção pode ser passada como `-PtoeCombatDiagnosticsDefault=false`. A escolha independe de TOMLs antigos.

- `/toe_combat diagnostics off` fecha a janela atual e interrompe a coleta imediatamente. A sessão do servidor não volta a selecionar um jogador automaticamente, inclusive após logout ou respawn.
- `/toe_combat diagnostics on` seleciona o jogador que executou o comando e abre uma nova janela. Os comandos exigem permissão de operador. Se outra pessoa estava sendo acompanhada, a janela anterior é fechada.
- A coleta ativa acompanha um jogador por vez. Ela mantém o jogador acompanhado ao renascer e encerra sua janela ao desconectar. Um novo servidor integrado começa uma sessão própria.

O logger padrão do Minecraft/Forge recebe os resumos com a marca `TOE_W0023` a cada 200 ticks de jogo (cerca de dez segundos a 20 TPS) e no fechamento de uma janela parcial. A rotação e a retenção seguem a instalação do Minecraft. A coleta usa o logger existente, sem arquivo próprio. Os agregados incluem postura, ameaça, transições, pacotes, Roll, erros, candidatos da consulta normal, um censo de alvos elegíveis por janela com arma sacada e tempo médio derivável e máximo das buscas e dos ticks do servidor. Uma janela registra quantidade, soma e máximo em microssegundos. Os campos da coleta omitem nomes, UUIDs e coordenadas.

As amostras são acumuladas em instâncias pequenas, sem fila por tick. A janela pode receber novos acumuladores quando houver um caso de diagnóstico definido. Câmera e R3 continuam com as seções existentes do profiler Forge. Para a sessão real, recolha `logs/latest.log` do servidor e, quando necessário, o ZIP de **F3+L** em `debug/profiling`.

### Overhead medido em Forge

A mesma fixture da W002.3 foi executada sequencialmente em dois perfis, com a coleta desligada e ligada. Cada fase mediu 90 ticks após 30 de aquecimento, com servidor integrado, seis chunks e limite de 60 FPS. A execução desligada gerou zero resumos. A ligada gerou oito resumos, um ao final de cada janela controlada.

| Cena com 30 zumbis | Tick médio desligado → ligado | Quadro p95 desligado → ligado |
| --- | ---: | ---: |
| COMBAT estável | 2,808 → 2,905 ms | 19,579 → 18,989 ms |
| TAB rápido | 2,512 → 2,760 ms | 19,347 → 19,133 ms |
| Ataques e Roll | 3,281 → 3,053 ms | 19,300 → 19,310 ms |

Em COMBAT estável, 17 buscas normais somaram 128 µs e o censo dos 30 alvos levou 36 µs. No TAB rápido o censo levou 52 µs. São tempos completos das consultas instrumentadas, com o censo separado. A diferença observada no tick médio de COMBAT estável foi de 0,097 ms. Uma rodada de cada modo e a variação entre cenas limitam a atribuição dessa diferença ao diagnóstico. O tempo de quadro ficou com p95 próximo de 19 ms nos dois modos.

A rodada da câmera confirmou uma janela inicial fechada pelo comando `off`, nenhuma reativação automática antes de `on`, janelas de 200 ticks e fechamento parcial no logout. Logs em `.logs/c002-off.log`, `.logs/c002-on.log` e `.logs/c002-camera.log`.

## Câmera C002

A versão anterior traçava oito raios paralelos nas quinas de um volume. Uma forma estreita podia ficar entre os raios. A C002 consulta a geometria sólida nos limites do deslocamento do olho até a posição desejada. Cada caixa da forma de colisão recebe uma expansão de 0,10 bloco por face. O teste contínuo do segmento do olho contra as caixas expandidas calcula o primeiro contato e deixa uma margem adicional de 0,02 bloco ao longo da trajetória. Assim, portas, escadas e quinas usam suas formas de colisão reais. Vegetação sem colisão fica fora desse teste.

A trajetória combina distância, altura e lateral do ombro selecionado. Paredes laterais e cantos podem encurtar o deslocamento desse ombro, mantendo sua seleção. A retração é aplicada imediatamente quando muda o limite seguro. A volta usa a fração disponível com amortecimento exponencial em função do tempo decorrido, com constante de 90 ms. Distância desejada e troca de ombro mantêm sua suavização de 85 ms. No corredor de um bloco, a composição fica mais próxima do jogador, como mostra a captura da fixture. A distância temporária pode ficar abaixo de dois blocos. A preferência salva continua entre dois e oito.

A câmera segue o olho do controlador do jogador, com suavização limitada do deslocamento lateral e vertical. O cálculo fica fora da geometria e das articulações do R3. A mira usa a mesma convergência. Na busca longa de entidades para a mira, o volume pesquisado termina no bloco encontrado pelo raio, dentro do alcance já existente. A varredura usa o índice de blocos do mundo, reaproveita o objeto de cálculo e visita as caixas sem materializar uma lista delas.

COMBAT conserva aproximação de 0,40 bloco e mira vermelho-escuro, com retorno à preferência quando o estado termina. Os controles ↑ e ↓ aproximam e afastam. As teclas ← e → selecionam o ombro. Remapeamentos, primeira pessoa e câmera frontal seguem seus caminhos atuais.

### Integração futura com o R3

A âncora espacial da câmera é o controlador do jogador. A futura composição de armadura pode evoluir no renderer sem deslocar a âncora ou alterar a geometria de colisão da câmera. O caminho existente de renderização com armadura continua valendo nesta rodada. A auditoria do JAR confere os assets R3 aprovados.

### Resultados da câmera

O ensaio em Minecraft passou em 23/23. Incluiu os comandos de diagnóstico, campo aberto, parede traseira, corredor de um bloco, quina, porta fechada, teto baixo, escada, sala pequena, troca para os dois ombros, remapeamento do afastamento para O, remoção do obstáculo, COMBAT no corredor, Roll, ataque pesado no ar, frontal, primeira pessoa e vegetação sem colisão. O Roll percorreu 2,50 blocos. O ataque pesado chegou ao impacto após um salto com pico de 1,20 bloco. Três checagens matemáticas adicionais passaram para a forma entre os raios, saída do volume inicial e equivalência do amortecimento com passos de tempo diferentes.

A parede traseira limitou a câmera a 1,38 bloco, o corredor a 1,62 e o teto baixo a 0,74. Sob esse teto, a cabeça ocupa grande parte do quadro. Ao remover a parede depois de aumentar manualmente o zoom para 4,50, o retorno passou por 4,02 e 4,41 antes de estabilizar em 4,50. O primeiro ensaio amostrava esse retorno tarde demais, quando faltavam apenas 0,017 bloco. O reteste registrou dois pontos mais cedo para verificar a progressão.

| Consulta de colisão | Oito raios | C002 |
| --- | ---: | ---: |
| Aberto | 123,81 µs | 42,95 µs |
| Parede | 104,64 µs | 32,62 µs |
| Corredor | 49,97 µs | 15,84 µs |
| Quina | 37,02 µs | 12,90 µs |
| Porta | 37,45 µs | 24,03 µs |
| Teto | 26,50 µs | 16,31 µs |
| Escada | 30,42 µs | 36,50 µs |

O microensaio comparou as duas consultas para o mesmo segmento no cliente Forge, com ordem alternada. Foram 18 pares em aberto e 32 em cada outra cena da tabela. Mede a consulta, excluindo o render e a convergência da mira. A escada foi mais cara nessa amostra. Esses tempos representam as geometrias e a distância da fixture.

Capturas em `.logs/c002-camera-final/screenshots`. O log do reteste está em `.logs/c002-camera-replay.log`. As checagens matemáticas estão em `.logs/c002-sweep-unit.log`. As capturas anteriores continuam nos próprios perfis.

## Arquivos e validação

- `combat/CombatDiagnostics.java`, `CombatCommands.java` e `WeaponReadiness.java` mantêm o padrão Forge, os comandos e as janelas de diagnóstico.
- `client/camera/ShoulderCamera.java` e `CameraSweep.java` integram a varredura e usam a mesma suavização e convergência.
- `build.gradle`, `validation/w002/java/com/towerofentropy/client/camera/C002RuntimeTest.java` e `validation/w002/java/com/towerofentropy/combat/CombatCrowdAudit.java` configuram os ensaios isolados.

- Build Forge 1.20.1 (47.4.28) concluído em Gradle offline e JDK 17. O JAR contém as classes novas de runtime e zero fixtures de QA. Os 16 modelos e arquivos de poses empacotados corresponderam aos recursos gerados aprovados.
- Suítes locais concluídas para movimento horizontal (43 casos), locomoção, arbitragem de ações, combate (60 casos) e Roll (104 casos).
- Classic e Slim passaram em 59/59 cada. A distância em COMBAT aberto mediu 4,00 → 3,60 → aproximadamente 4,00 blocos nas duas variantes. As capturas de 854 × 480 registraram 20 pixels vermelhos no centro durante COMBAT e zero nas amostras antes e depois. Logs em `.logs/c002-classic.log` e `.logs/c002-slim.log`.
- A regressão Slim passou em 50/50 para W001, 36/36 para W001.1/C001.3 e 29/29 para expansão e parkour. Ela cobre movimento, saltos, Wall Jump, Wall Grab, Ledge Climb, combos, dano e controles de câmera. Log em `.logs/c002-regression.log`.

JAR Forge em `build/libs/towerofentropy-0.0.1.jar`. SHA-256 `14594543FFC0B6CC16E0074F5EBCFE65615F978EED080FD6DB9C0FAB74F3BA8B`.

A validação usa Forge com servidor integrado. A opção explícita de desligar foi exercitada no ForgeGradle. O carregamento pelo launcher de distribuição e uma sessão dedicada com múltiplos jogadores ficaram fora dos ensaios. As capturas representam cenários roteirizados. A percepção de enquadramento durante sessões longas, situações em que o olho já começa dentro de uma forma sólida, combinações de mods com colisões próprias e ambientes reais com movimentos simultâneos seguem como pendências visuais.
