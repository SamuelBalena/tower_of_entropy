# M002.A3.3 — refinamentos de combate

14/11/2026. Minecraft 1.20.1, Forge 47.4.28, Microsoft JDK 17.0.20.1 x64, Gradle 8.8. Polymesh R3 continua padrão; CPM e R3.1 experimental preservados. Este marco contém somente os três ajustes solicitados.

## Avanços configuráveis

| Golpe | Padrão anterior | Novo padrão | Janela física | Impacto |
|---|---:|---:|---|---:|
| A | 0,18 | 0,40 bloco | 4 passos a partir do tick 1 | tick 4 |
| B | 0,22 | 0,50 bloco | 4 passos a partir do tick 2 | tick 5 |
| C | 0,28 | 0,60 bloco | 6 passos a partir do tick 10 | tick 14 |
| Pesado | 0,32 | 0,70 bloco | 6 passos a partir do tick 8 | tick 12 |

`CombatConfig` centraliza os padrões. Permanecem configuráveis em `saves/<mundo>/serverconfig/towerofentropy-combat-server.toml`, seção `attackMovement`: `lightABlocks`, `lightBBlocks`, `lightCBlocks`, `heavyBlocks`. Intervalo individual anterior 0–0,75 bloco preservado. Combo nominal: 1,50 bloco em terreno livre.

`CombatConfigMigration` migra uma única vez a tupla completa dos quatro padrões antigos e grava `defaultsRevision=1`. Se qualquer distância tiver sido personalizada, mantém a tupla inteira e registra a revisão. O marcador permite escolher depois até os valores antigos sem remigração. A migração foi exercitada no mundo de QA existente; não se alterou uma configuração personalizada para forçar o resultado.

O mecanismo A3.2 permanece byte a byte: direção autorizada pelo servidor no início de cada golpe, orçamento de distância por ação, passos por `Entity.move`, colisões vanilla, sondagem de apoio na borda, compromisso de WASD e validação de impulsos externos. Sem auto-aim, perseguição, hitbox maior ou teleporte no avanço normal. Paredes, entidades e bordas podem reduzir a distância. O Roll elimina fila, impactos e avanço pendentes pelo cancelamento existente.

## Recuperação final do pesado

O asset permanece o recorte original de `Sword_Heavy_Combo`, quadros fonte **60–130**, 71 amostras a 30 Hz, 2.333 ms. Preparação, giro e golpe até o quadro local **45** (fonte 105, 1,50 s) mantêm a mesma velocidade. O impacto continua no tick 12 / 600 ms, muito antes da alteração temporal.

A inspeção dos quadros de tronco, raiz e braço, seguida das capturas durante execução, delimitou o trecho final local 45–70 para a volta da postura inclinada à postura em pé. `PRESERVATION_AND_KEYFRAMES.json` registra amostras desse trecho; não houve exclusão de quadros nem reexportação de malha/animação.

`CombatMove.visualTime` percorre o trecho final original de 833 ms em **250 ms**, dentro da referência de 200–300 ms. Para `u=clamp((t−1,5)/0,25,0,1)`, a fração amostrada é `0,3u+2,1u²−1,4u³`. A função é monotônica, preserva a inclinação temporal na entrada e na saída da recuperação, e alcança o quadro final original em 1,75 s. Somente a amostragem da ação pesada autorizada usa o remapeamento; previews diagnósticos podem continuar mostrando o asset original completo.

O servidor encerra a ação no tick **35 / 1,75 s**, antes 47 / 2,35 s discretizados. Redução efetiva de controle: 12 ticks / 600 ms. Não se acelerou o golpe inteiro. O blend existente de retorno de 140 ms continua em paralelo à retomada do controle; não acrescenta outro período de bloqueio. As durações leves 9/11/40 ticks e os quatro impactos 4/5/14/12 foram preservados. Não há impacto extra na recuperação.

As medições nativas registraram liberação com idade 35 ticks, parado e após Sprint. Após soltar, a reprodução voltou a Idle; com W/Sprint mantidos, passou à locomoção vanilla e ao Sprint. Os quadros críticos estão na [seleção de capturas](../animation_pipeline/reports/m002_a3_3/REVIEW/README.md). Capturas não demonstram sozinhas ausência de snapping entre todos os quadros; a aprovação da sensação em velocidade normal continua com o usuário.

## Pulo durante ataques

`ClientAttackJumpGate` intercepta `MovementInputUpdateEvent` após o processamento dos controles, consultando `ClientAttackMotion.active()` — compromisso pendente e ação autorizada. Usa a `KeyMapping` atual de pulo, limpa o pedido voluntário e os temporizadores vanilla de Auto-Jump e repetição do salto. Os campos mapeados Forge 1.20.1 `f_108613_` / `f_20954_` são resolvidos por `ObfuscationReflectionHelper`, sem alterar opções do usuário. A validação inclui remapeamento para R e Auto-Jump ativado.

Se o jogador segurou pulo durante o ataque, mantém uma trava de liberação: encerrar o ataque não dispara salto enfileirado. Soltar e pressionar novamente restaura o pulo. A trava não altera a velocidade vertical do mundo. Logout limpa o estado. O Roll continua cancelando normalmente; segurar pulo durante o cancelamento também exige nova pressão, em vez de gerar salto involuntário.

`AttackJumpGuard` valida o estado real no servidor. Um `LivingJumpEvent` voluntário durante ataque apoiado é rejeitado, preservando velocidade horizontal autorizada e registrando a altura de apoio. Uma janela de reconciliação de até três ticks absorve os pacotes transitórios do salto rejeitado antes da checagem antiga de ação no solo. Somente esse caso registrado pode gerar correção excepcional da posição recebida. Não é o mecanismo do avanço normal.

A correção exige apoio sólido, ataque ativo e ausência de queda ou impulso externo conhecido. Knockback e velocidade vertical autoritativa concedem tolerância, e queda abaixo do apoio encerra a correção. Gravidade e empurrões não são congelados. Um pacote forjado de salto foi rejeitado sem cancelar o impacto; knockback, impulso vertical e queda passaram nos cenários testados. Isso não equivale a um anticheat completo nem à homologação de todas as fontes externas de movimento.

A elegibilidade anterior das ações permanece: este marco não habilita ataques/combo no ar. O bloqueio não afeta exploração sem ataque. Dano, i-frames e regras de Roll permanecem inalterados.

## Compatibilidade e entrega

Protocolo interno 8 (antes 7): cliente e servidor devem usar esta versão juntos. Sem mudança de autenticação, novas dependências, novos golpes, stamina ou mana. O backup A3.2 está em `.downloads/m002_a3_3/baseline/`. Assets e 12 arquivos centrais preservados foram auditados contra o backup; o inventário de 171 assets Godot também passou por hashes.

JAR normal: `build/libs/towerofentropy-0.0.1.jar`. Experimental separado: `.downloads/m002_a3_3/experimental/`. Não instalar as duas variantes simultaneamente. R3.1 não foi promovido nem recebeu homologação estética adicional.

Procedimentos, falhas iniciais corrigidas, resultados finais e limitações estão no [registro de testes](TEST_LOG_M002_A3_3.md). Não se iniciou outro marco.
