# W001.1 + C001.3 — Combate e câmera

## Golpes aéreos

A validação de espada chamava o predicado de ações de locomoção que exigia chão. O mesmo predicado era consultado novamente durante os ticks do golpe, então liberar apenas a entrada ainda cancelaria o impacto. O avanço físico e a proteção de pulo também estavam associados ao golpe que começou no chão.

A espada agora valida a postura sem exigir chão. O servidor continua sendo responsável pela autorização, pelo tempo do combo e pelo impacto. Golpes iniciados no ar dispensam a sessão de avanço e mantêm o impulso existente. Se um golpe iniciado no chão perder apoio, o avanço termina e o golpe pode continuar. A proteção de pulo do golpe terrestre continua ligada à sessão de avanço. Wall Jump conserva sua trajetória. Aderência, suspensão e subida de borda mantêm prioridade quando seguram fisicamente o jogador.

O inventário dos GLBs Classic e Slim contém os clipes aprovados de espada, salto e parkour, sem um clipe integrado de golpe aéreo de espada. No R3, o ataque no ar reaproveita `UAL_sword` e os clipes subsequentes do mesmo combo. A parte inferior continua no clipe de salto em andamento; os braços e o tronco superior reproduzem o golpe. Os arquivos de animação, rig e geometria permanecem os mesmos.

## Ataque e mineração

A W001 barrava o clique no evento Forge antes de o resolvedor de espada recebê-lo. Agora o clique contra bloco ainda cancela o swing vanilla e a mineração. Quando a mão principal segura uma espada que já usa o combo, o mesmo clique envia o pedido normal de golpe. O comando respeita o controle de pressionamento, a fila e a disponibilidade anterior. Os demais itens marcados continuam no bloqueio de mineração, sem ganhar o combo de espada. A validação W001 no servidor permanece ativa.

O resolvedor servidor já corta o raio de entidade por `ClipContext.Block.COLLIDER` e valida alcance, arco e permissões. Um bloco de grama ou flor pode ser a primeira coisa selecionada pelo raycast de mineração, mas sua forma sem colisão não corta o raio do golpe. Uma parede sólida corta. O cliente não envia um ID de alvo substituto. A seleção de PvP, dano e regras Forge seguem o caminho existente.

## Câmera

A inspeção não encontrou a W002, estados de embainhar ou um zoom contextual que gravasse outra distância. Na câmera anterior, o foco ajustava a convergência angular para manter a mira alinhada. O enquadramento também ficava sujeito ao FOV dinâmico vanilla, que pode reduzir o campo ao perder Sprint no início do golpe, e ao recorte pela forma visual de blocos. O atraso posicional ainda podia mover a câmera no próprio eixo da distância. O enquadramento específico relatado depende da geometria e do estado da cena original.

Na vista traseira C001, o FOV de jogo usa o valor escolhido pelo jogador durante movimento e combate. Efeitos de morte, submersão e luneta conservam seu caminho. A suavização permanece nos eixos transversais à visão. A distância no eixo de visão segue a preferência manual, com recorte pela forma física dos blocos. Uma parede ainda aproxima a câmera temporariamente, com recuperação suavizada ao liberar espaço. A convergência da mira permanece separada da distância. Primeira pessoa e câmera frontal continuam nas rotinas vanilla.

- **←** seleciona o ombro esquerdo e **→** seleciona o direito. Pressionar a mesma seta novamente mantém o lado.
- **↑** aproxima, **↓** afasta, com limites de 2 a 8 blocos e distância inicial de 4.
- O antigo mapeamento B padrão é removido na migração. A entrada legada fica desatribuída por padrão e conserva um remapeamento já personalizado. O lado ativo e os mapeamentos continuam locais.
- Alt + roda conserva o comportamento anterior.

## Arquivos

- `CombatInput.java`, `CombatController.java` e `LocomotionController.java` separam o clique de ataque da tentativa de mineração e aceitam a postura aérea.
- `AttackMovement.java`, `AttackJumpGuard.java`, `ClientAttackMotion.java` e `WallSlide.java` mantêm a sessão terrestre separada da trajetória aérea.
- `PlayerRendererR3.java`, `UpperBodyPose.java` e `VisualContext.java` compõem o ataque no ar.
- `ShoulderCamera.java`, `CameraInput.java`, `CameraSettings.java` e os arquivos de idioma registram a câmera e os controles.
- `AirSwordCameraRuntimeTest.java`, `WeaponBlockRuntimeTest.java`, os testes de câmera da expansão, `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore` organizam os cenários e artefatos isolados.

## Limitações

O desenho de ataque composto vale para o jogador local no Polymesh R3. Estados fora da postura suportada continuam seguindo as regras existentes. A modelagem visual do golpe no ar usa os clipes aprovados disponíveis e fica pendente de revisão no Minecraft. O comportamento de picareta com espada na mão secundária fica fora desta mudança.

## Validação

O build Forge final passou. A auditoria do pacote confirmou 16 arquivos de modelos e poses idênticos aos anteriores, a tag W001 presente e zero classes de QA. Os dois GLBs de produção contêm 40 clipes cada.

Passaram as cinco suítes de lógica `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1`.

A repetição focalizada Classic passou em **33/33**. O salto normal e o Sprint Jump atingiram **102,5019** a partir de Y=100 e registraram um ataque, um impacto e um acerto no servidor cada. O jogador manteve velocidade horizontal no Sprint Jump, e nenhuma sessão de avanço de espada foi ativada no ar. Os cenários com grama e flor permitiram dano ao alvo, mantendo o bloco intacto. O cenário com pedra registrou o golpe, manteve o bloco e bloqueou o dano. O combo no ar sem alvo no sentido de mira livre foi testado no chão com os três clipes A/B/C.

A câmera Classic permaneceu em aproximadamente 4 blocos com o FOV configurado nos quadros avaliados. A parede atrás da câmera reduziu a distância para cerca de 1,38 bloco. A retirada da parede restaurou os 4 blocos. A câmera frontal mediu −4 blocos na mesma projeção longitudinal e a primeira pessoa mediu zero. O Classic passou também em **2/2** na reinicialização, com o padrão B removido e o ombro salvo.

A primeira execução Classic mostrou o segundo cancelamento pelo fallback de locomoção e comparou, na medição da câmera, amostras de tempos diferentes. A repetição corrigiu o cancelamento e mediu a câmera e os olhos no mesmo quadro. A execução inicial também registrou uma divergência de desenho de vaca com dano servidor válido e uma amostra de reversão aérea de 36,4°, já observada na C001.2. Nenhum limite histórico de movimento foi alterado. Os logs iniciais e as capturas foram preservados.

Os saves isolados ficam em `run-w0011-qa/` e as capturas novas em `animation_pipeline/converted/w0011-qa/`. A partir de `minecraft`, os perfis disponíveis são:

```powershell
.\scripts\gradle.ps1 build --offline --console=plain
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeW0011Qa -PtoeW0011Only -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeGlobalSlim -PtoeW0011Qa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeW0011Qa -PtoeW0011Restart -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeW0011Qa -x downloadAssets
```

O Slim passou em **36/36** na matriz nova, **50/50** na W001 atualizada e **29/29** na regressão de movimentação e parkour. A W001 continua exigindo blocos intactos e ausência de swing ou mineração. Sua asserção visual agora exige o clipe de espada no clique legítimo com espada, conforme a nova regra. A matriz Slim acrescentou a mira direta sobre uma entidade e mediu distância e FOV antes e durante o golpe. A execução Slim também passou na checagem de desenho da vaca e no limiar histórico de reversão aérea.

A reinicialização Slim passou em **2/2**. O remapeamento personalizado em K e o ombro esquerdo salvo permaneceram. A regressão do R3 passou em **18/18**, incluindo espada, Roll, arco, consumo, salto e reinicialização. Os logs desta rodada usam o prefixo `.logs/w0011-`. As capturas históricas foram preservadas.

- JAR: `build/libs/towerofentropy-0.0.1.jar`
- SHA-256: `E5B7A6455D16B2CE5BA5B103997FF18CCEF1FAB82560D69F564D0C2592D90A92`
