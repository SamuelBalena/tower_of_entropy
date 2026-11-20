# W002.1 — Recovery, Roll pelo Sprint e câmera de combate

## Recovery

A W002 só retimava os últimos 15 ticks do terceiro leve e os últimos quatro do pesado. No pesado, os primeiros 1,50 s do clipe ainda corriam no tempo original, apesar de o dano ocorrer a 0,60 s. A mistura de saída do R3 também continuava usando a duração anterior. A W002.1 mantém as poses anteriores ao impacto e alguns ticks de continuidade do golpe, acelera a fase final e encurta a mistura para a locomoção.

| Golpe | Impacto | Até o início da retimagem | Recovery final W002 → W002.1 | Fim lógico W002 → W002.1 |
| --- | --- | --- | --- | --- |
| Terceiro leve | Tick 14, 0,70 s | Tick 17, 0,85 s | 18 → 9 ticks, 0,90 → 0,45 s | 35 → 26 ticks |
| Pesado | Tick 12, 0,60 s | Tick 16, 0,80 s | 18 → 9 ticks, 0,90 → 0,45 s | 34 → 25 ticks |

O relógio visual preserva a parte inicial do clipe, acelera progressivamente o retorno e atinge a última pose no novo fim lógico. A mistura de saída padrão para Idle/Walk passa de 140 para 70 ms nesses dois golpes. Na saída para clipes de salto, passa de 120 para 60 ms. Os primeiros dois leves, seus encadeamentos e o buffer continuam com as regras anteriores. Impacto, dano e hitbox continuam vindo do mesmo resolvedor servidor. O lunge terrestre ocupa os mesmos ticks. O limite horizontal do pesado iniciado no ar usa o novo fim autorizado, inclusive na aterrissagem quando ela acontece durante o golpe. Gravidade, colisões e knockback seguem a física existente.

## Roll e Sprint

A W002 verificava `keySprint.isDown()` no fim do tick. Nesse ponto, a movimentação vanilla já podia ter aplicado Sprint. Com a opção de alternar Sprint, `isDown()` representa o estado alternado e o segundo pressionamento físico pode desligar esse estado. Isso pode suprimir a borda que disparava Roll. São dois problemas identificados no código. A configuração e o motivo de rejeição da sessão original do usuário não foram capturados.

O cliente acompanha os eventos físicos de teclado e mouse pelo mapeamento configurado e trata a intenção antes do tick de movimentação. O Sprint direcional e as duas consultas vanilla à tecla usam o estado físico durante `COMBAT`. Enquanto espera a resposta do Roll, por até quatro ticks, o cliente segura o deslocamento horizontal voluntário. No reteste anterior, um passo de caminhada entre o pedido e a resposta consumia parte do limite do Roll, que depois era cancelado por limites de movimento. O cliente espera a confirmação de término após os 14 ticks de deslocamento antes de devolver a locomoção. O servidor continua decidindo autorização, postura, compromisso de ataque e disponibilidade do Roll. Quando o Roll termina, segurar Sprint permite correr e soltar termina a corrida de combate. Fora de combate vale a semântica vanilla da tecla. A mudança mantém o Roll manual, a física de pulo e as regras atuais de Roll.

O diagnóstico opcional `-Dtoe.combat.roll.trace=true` registra pressionamento, estado local, resultado do pedido no servidor e motivo de término. O canal de ação usa versão 12 para manter o relógio visual do cliente compatível com a duração enviada pelo servidor.

## Mira e câmera

A mira personalizada da câmera traseira conserva o desenho e a posição. Em `COMBAT` ela recebe um tom vermelho escuro e um ponto central discreto, com transição de cor suavizada. `EXPLORATION` e `ARMED` usam a aparência normal. A cor segue `ClientWeaponState.inCombat()`, o mesmo estado confirmado pelo servidor que habilita a aproximação.

A distância-alvo traseira é a preferência manual menos 0,75 bloco em combate, limitada a 2 blocos antes da correção de clipping. Uma preferência de 4 blocos vai a 3,25 e retorna a 4 ao sair de combate. As setas continuam alterando a preferência e selecionando o ombro durante o combate. O offset transversal usa a composição da distância manual. A colisão por raios, a convergência da mira, a câmera frontal e a primeira pessoa continuam usando seus caminhos existentes.

## Arquivos

- `combat/CombatMove.java` e `client/R3VisualPose.java` ajustam o relógio do clipe e a mistura de saída.
- `animation/ActionKeys.java`, `animation/ActionNetwork.java`, `client/CombatSprint.java`, `client/ClientRollMotion.java`, `client/DirectionalSprint.java`, `combat/RollController.java` e `coremods/directional_sprint.js` integram o pressionamento físico, a autorização e o diagnóstico de Roll.
- `client/camera/CameraCrosshair.java` e `client/camera/ShoulderCamera.java` aplicam feedback ao estado confirmado.
- `build.gradle`, `validation/w002/java/com/towerofentropy/combat/WeaponDynamicRuntimeTest.java`, `scripts/test-combat.ps1` e `scripts/tests/CombatRulesTest.java` integram e validam a W002.1.

## Validação

- Forge 1.20.1, JDK 17, Gradle offline.
- Suítes de movimento, combate, Roll, locomoção e arbitragem de ações.
- Capturas da rodada em `animation_pipeline/converted/w0021-qa/`.
- JAR Forge em `build/libs/towerofentropy-0.0.1.jar`.


Classic e Slim passaram em **47/47** cada, respectivamente com Ctrl padrão em modo mantido e K remapeado em modo alternado. A fixture envia eventos Forge de tecla em um cliente real com servidor integrado. Nos dois cenários, o servidor confirmou o Roll com 2,50 blocos, o cliente reproduziu `UAL_roll` no R3 e a janela de invulnerabilidade ficou ativa. O Sprint retomou com a tecla mantida e encerrou ao soltá-la enquanto a direção permanecia pressionada. O pesado aéreo manteve o limite horizontal após a aterrissagem e liberou movimento ao fim do novo recovery.

O zoom traseiro mediu **4,00 → 3,25 → aproximadamente 4,00** blocos. O mínimo de 2 blocos, o zoom manual durante combate e os dois ombros passaram. Uma leitura dos 21×21 pixels centrais das capturas de ambos os modelos identificou 20 pixels vermelhos em `COMBAT` e zero na exploração, com arma sacada sem aggro e após a saída. Capturas e logs de cada fase estão sob os perfis `classic-verified` e `slim-verified`, e nos logs `.logs/w0021-*.log`.

A regressão Slim registrou **50/50** em W001, **36/36** em W001.1/C001.3 e **28/29** na expansão. Passaram os ataques contra entidades, a interação com ferramentas, os ataques aéreos e através de vegetação, a retenção de dano por parede sólida, o combo A/B/C, as câmeras frontal e primeira pessoa, o recorte por obstáculo, Sprint Jump, Wall Jump, Wall Grab e Ledge Climb. O cenário `air_reversal` virou na direção esperada, porém mediu 38,5° entre amostras visuais contra um limite de 36°. A mesma checagem já oscilou em rodadas anteriores. Os limites e as asserções foram preservados.

O diagnóstico inicial registrou `movement bounds` no término do Roll. A repetição com o deslocamento pendente suspenso e a confirmação de término registrou `complete` nas duas variantes. O primeiro ensaio mediu o retorno da câmera antes do fim da suavização e só produziu PNGs de 1×1 por causa da janela minimizada. As capturas válidas foram geradas em perfis novos.

As suítes locais passaram, com 60 checagens de geometria e tempo de combate, 104 de Roll, 43 de movimento e as suítes de locomoção e arbitragem de ações. A comparação de sensação dos golpes em jogo e a reprodução com a configuração pessoal do usuário seguem pendentes. A validação de rede usou servidor integrado, com um cliente. Mouse remapeado e servidor dedicado com dois clientes ficaram fora da cobertura executada.

O build Forge final passou. A auditoria registrou 16/16 arquivos de modelos e poses idênticos aos de referência e zero classes de QA no JAR. O SHA-256 é `751F7F07A708B7A661186812332D9F4EFE45A9A6AC7A153DA9D4CC390BF83DDE`.
