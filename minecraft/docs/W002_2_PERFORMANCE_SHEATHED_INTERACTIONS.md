# W002.2 — Desempenho de combate e interações com arma guardada

## Diagnóstico

O ensaio de referência executa a W002.1 em um cliente Forge com servidor integrado e o mundo da fixture W002. O travamento severo relatado segue sem reprodução nessa fixture. A causa raiz permanece em aberto.

A inspeção confirmou três custos evitáveis. A cada cinco ticks com arma sacada, o servidor percorria os mobs da região e montava uma lista de alvos válidos, embora só precisasse detectar o primeiro. A W001 consultava por reflexão o estado de quebra em todo tick com arma equipada. O R3 percorria os componentes JSON de dois quadros e convertia posição e rotação do pulso durante a renderização, inclusive nos clipes de combate. Os pacotes da arma já eram deduplicados por postura e modo. O modificador de velocidade já usava um ID fixo.

A consulta agora usa o índice espacial de entidades e termina ao encontrar a primeira ameaça válida. O raio de 24 blocos, a consulta a cada cinco ticks, a exigência de alvo real e o tempo de graça continuam aplicáveis. A entrada no bloqueio de mineração aborta o progresso existente. As proteções de clique, velocidade e quebra também são verificadas no caminho normal da W001. O R3 mantém os valores de pulso já decodificados por quadro e invalida o cache ao trocar o modelo. A velocidade de combate muda na entrada e na saída de COMBAT.

## Controles e câmera

- **TAB** é o padrão de sacar e guardar no menu de teclas. O cliente trata o evento de pressionamento e ignora a repetição do teclado. Na primeira abertura, o H sem modificador gravado pela versão anterior migra para TAB. Outros atalhos permanecem gravados. Depois da migração, H também pode ser escolhido no menu.
- Quando a ação da arma e a lista de jogadores compartilham a tecla, a arma tem prioridade durante o gameplay. Para acessar a lista enquanto TAB controla a arma, remapeie a lista em **Opções → Controles → Teclas**. O atalho da arma também pode ser remapeado.
- A câmera traseira aproxima **0,40 bloco** em COMBAT. Com a preferência em 4,00, o alvo vai a 3,60 e retorna suavemente a 4,00. O alvo continua limitado a 2,00 blocos antes da correção de clipping. As setas continuam ajustando distância e ombro. A mira fica vermelha enquanto o servidor confirma COMBAT. Um mob passivo sem aggro fica fora dessa regra.

## Equipamento efetivo

- **SHEATHED** mantém o item real na hotbar e a representação nas costas em R3. Cliente e servidor resolvem a mão principal vazia para os caminhos vanilla de mineração, dano e interação. A velocidade base, a verificação de ferramenta correta, os encantamentos e a durabilidade seguem o comportamento de mão vazia. Status do jogador e efeitos vanilla de mineração continuam aplicáveis.
- Socos usam o ataque vanilla e as poses Jab/Cross existentes nos contextos elegíveis. Combo, ataque aéreo de espada e animação de espada exigem equipamento efetivamente sacado.
- **DRAWN** usa a pilha equipada, mantém combos e ataques aéreos e aplica a W001 de novo. O resolvedor existente continua permitindo acertos através de vegetação não sólida e bloqueando acertos através de paredes sólidas. As duas transições de seis ticks bloqueiam a mineração até estabilizar a postura.
- A opção de trocar o item para a outra mão opera sobre a pilha física. O protocolo do canal de ações passa a 13. Cliente e servidor devem usar a mesma versão.

## Medições e reprodução

A fixture registra intervalos entre quadros, tempo de trabalho do tick do servidor integrado e médias de CPU da pose e do pipeline R3. Os perfis são sequenciais. VSync a 60 Hz limita a comparação de FPS ao impacto de quadros atrasados. As primeiras janelas são curtas e sujeitas a carregamento, aquecimento do JVM e gravação de capturas.

| Cenário Classic | Quadro p95, ms | Tick médio, ms | Pose R3 média, ms |
| --- | ---: | ---: | ---: |
| Sem arma | 19,53 → 19,03 | 3,17 → 2,86 | 0,040 → 0,032 |
| Arma guardada, início da sessão | 22,30 → 71,33 | 24,57 → 34,96 | 0,392 → 0,301 |
| Arma sacada sem mob | 71,43 → 28,90 | 34,84 → 27,43 | 0,224 → 0,196 |
| Arma sacada, zumbi sem alvo | 26,79 → 30,19 | 28,16 → 16,14 | 0,219 → 0,193 |
| Ataque com mobs passivos próximos | 21,73 → 22,08 | 7,31 → 7,83 | 0,132 → 0,111 |
| COMBAT, zumbi com alvo | 21,26 → 19,77 | 5,15 → 4,52 | 0,077 → 0,053 |
| Tempo de graça após perder aggro | 20,03 → 19,98 | 3,49 → 3,46 | 0,050 → 0,050 |
| Roll em COMBAT | 21,97 → 20,39 | 4,10 → 3,57 | 0,085 → 0,074 |
| Ataque pesado aéreo em COMBAT | 19,99 → 19,23 | 3,84 → 3,68 | 0,076 → 0,060 |
| Transições repetidas | 19,36 → 19,09 | 3,46 → 3,22 | 0,051 → 0,037 |

As setas comparam W002.1 e W002.2. Nas transições repetidas, 799 e 800 quadros tiveram média de 16,75 e 16,73 ms, respectivamente. O máximo foi de 22,36 e 22,99 ms. A rodada instrumentada inicial da W002.2 registrou um quadro de 279,53 ms durante uma janela que também coletava as métricas detalhadas do servidor. A rodada final moveu essa coleta para uma janela posterior. O resultado de arma guardada durante o aquecimento teve apenas 2 ticks de servidor na referência e 11 na revisão atual. Os resultados de partida variam e precisam de mais repetições para comparação.

Na coleta inicial de 201 ticks, `toeThreatScan` executou 40 vezes e representou 0,74% da seção pai do profiler. Houve cinco publicações, cinco envios de estado e cinco mudanças de atributo. Os mobs da automação têm IA desativada e recebem um alvo explícito no servidor. Esses resultados cobrem o custo das transições e dos clipes no mundo de QA. O cenário do usuário com perseguição natural permanece pendente.

Os logs completos estão em `.logs/w0022-baseline.log`, `.logs/w0022-classic.log`, `.logs/w0022-slim.log` e `.logs/w0022-compare.log`. As capturas e o perfil de contadores ficam em `animation_pipeline/converted/w0021-qa/` nos diretórios cujo nome contém `w0022`.

Para coletar um travamento no perfil em que foi observado:

1. Use o mesmo mundo, posição, distância de renderização, resolução, mods e ajuste de VSync em cada execução. Selecione a câmera traseira e espere o carregamento de chunks estabilizar.
2. Use **F3+L** e deixe a coleta de dez segundos terminar em cada cenário. Capture mão vazia, arma guardada, arma sacada sem mob, arma sacada perto de mob passivo, zumbi perseguindo, entradas e saídas repetidas de COMBAT, ataques e Roll. Para a saída natural, mantenha a coleta até acabar a janela de graça.
3. Guarde os ZIPs da pasta `debug/profiling`, `logs/latest.log` e o horário do travamento. Nas árvores procure `toeThreatScan`, `toeR3`, `toeR3Pose` e `toeShoulderCamera`. No servidor, os contadores `toeWeaponPacket`, `toeWeaponTransition` e `toeCombatSpeedChange` registram envios, publicações e mudanças do modificador no intervalo. Em servidor dedicado, `/jfr start` e `/jfr stop` também permitem coletar ticks, GC e atividade de rede.

## Arquivos

- `combat/EffectiveEquipment.java`, `WeaponReadiness.java`, `WeaponBlockProtection.java`, `CombatController.java` e os registros em `META-INF/coremods.json` e `coremods/effective_equipment.js` resolvem a mão efetiva, a transição de equipamento, a mineração e a busca de ameaças.
- `animation/ActionKeys.java`, `animation/VisualTuning.java`, `animation/ActionNetwork.java`, `client/ClientWeaponState.java` e `client/CombatInput.java` tratam o atalho, a sincronização e os caminhos de entrada.
- `client/PlayerRendererR3.java` e `client/camera/ShoulderCamera.java` registram os custos de renderização e câmera, preservam a arma nas costas e aplicam o novo offset.
- `build.gradle` e as fixtures em `validation/w002`, `validation/expansion` e `validation/a3` mantêm os novos ensaios em perfis de QA separados.

## Validação e artefato

- Build Forge 1.20.1 (47.4.28), Gradle offline e JDK 17. A auditoria conferiu 16/16 arquivos de modelo e pose com os recursos gerados e encontrou zero classes de QA no JAR.
- Suítes locais de movimento horizontal (43 casos), locomoção, arbitragem de ações, combate (60 casos) e Roll (104 casos) concluídas.
- Classic e Slim passaram nas 59 checagens da rodada inicial W002.2. A câmera Classic mediu 4,00 → 3,60 → 3,99 blocos. As capturas de 854 × 480 registraram 20 pixels vermelhos na região central da mira em COMBAT em ambas as variantes, com zero nos três quadros de controle de cada variante. As capturas anteriores permanecem nos próprios perfis.
- Espada encantada guardada, com velocidade de mineração igual à mão vazia, soco de 1 ponto, terra quebrada por entrada no cliente, poses de soco, alavanca acionada e troca de mão sem perda do item ou durabilidade. A fixture também confirmou a rejeição do combo guardado.
- A primeira passagem da regressão registrou 36/36 em W001.1/C001.3 e 29/29 na expansão de movimento e parkour. A fixture W001 preparava os bastões marcados pelo datapack guardados, embora verificasse o bloqueio em DRAWN. A preparação passou a sacar todos os itens marcados usados nesse ensaio, com as asserções de bloqueio preservadas.

- A rodada Classic final passou em 60/60. Ela carregou a opção legada H e confirmou sua migração para TAB. O trecho separado de contadores confirmou quatro publicações, quatro pacotes e quatro mudanças de velocidade em 201 ticks.
- A regressão final Slim passou em 50/50 na W001, 36/36 na W001.1/C001.3 e 29/29 em expansão e parkour. O log está em `.logs/w0022-regression-final.log`.

Validação de rede executada com servidor integrado e um cliente. Servidor dedicado com dois clientes e o cenário visual do travamento original seguem pendentes.

JAR Forge em `build/libs/towerofentropy-0.0.1.jar`. SHA-256 `9EDEEA4E9F33277EB1CFE1646EF0125870D359AEAB7DB1E1E586A224C0FA3D2F`.
