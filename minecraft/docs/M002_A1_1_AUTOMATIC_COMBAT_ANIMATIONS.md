# M002.A1.1 — Animações automáticas de combate

Minecraft 1.20.1, Forge 47.4.28, Java 17. Não é mais necessário executar `/toe_combat on` para usar as animações de espada.

## Gameplay padrão

AUTO é o modo inicial de cada jogador. Uma `SwordItem` na mão principal ativa o conjunto de espada: ATTACK executa A → B → C, USE executa o pesado quando não há interação prioritária, e V executa o Roll físico. As KeyMappings do Minecraft continuam sendo utilizadas, incluindo remapeamentos. O jogador não precisa ser operador para a ativação automática; permissões continuam restritas aos comandos de diagnóstico.

Trocar para outro item em AUTO cancela ataque/Roll ativo, fila e impactos pendentes, e libera as ações vanilla. Reequipar uma espada cria uma sessão nova. A validação do servidor também acontece ao receber a intenção, permitindo o primeiro clique sem esperar uma mensagem de ativação separada.

Mineração mirando blocos continua vanilla. USE conserva prioridade para qualquer bloco, entidades com interação e itens no offhand; escudo continua utilizável e impede o pesado enquanto equipado. Menus, inventário, chat e overlays mantêm os bloqueios de entrada anteriores.

## Modos de diagnóstico

| Comando | Efeito |
|---|---|
| `/toe_combat auto` | Restaura a seleção automática por equipamento. |
| `/toe_combat on` | Força a sessão para testes permitidos, preservando validação de arma/estado dos ataques. |
| `/toe_combat off` | Impede a reativação automática, libera ATTACK/USE vanilla e impede Roll personalizado por V. |
| `/toe_combat status` | Mostra modo, ativação efetiva, ação, fila e Roll. |

ON/OFF continuam por jogador e transitórios, como os comandos anteriores. Ao reconectar, reiniciar ou reaparecer após morte, o padrão volta a AUTO. ON não permite ataques customizados com ferramentas sem conjunto ou em estados inválidos. A disponibilidade de Roll sem espada em ON permanece um recurso de diagnóstico já existente. J e comandos de animação explícitos continuam como diagnóstico legado; não são controles obrigatórios de gameplay. OFF não desliga o renderizador R3 — para esse diagnóstico existe `/toe_renderlab off`.

## Arquitetura

`CombatController` separa preferência AUTO/ON/OFF da sessão de ação. O tick do jogador no servidor reconcilia equipamento/vida/espectador e cria/remove sessões. `requestAttack` e `RollController.request` também reconciliam antes de aceitar uma ação. `VanillaAttackGuard` consulta a elegibilidade efetiva para evitar dano vanilla duplicado no primeiro ataque.

`ClientCombatState` recebe o modo e calcula disponibilidade local a partir dele e da espada equipada. A seleção local serve à interceptação; o servidor continua decidindo início, arma, estado, alvo, impacto e cancelamento. Protocolo de rede 5: servidor e cliente devem usar o mesmo build. Não houve alteração do formato de configurações de dano ou de balanceamento.

As seguintes regras ficaram intactas: fila de duas entradas, continuidade de 24 ticks, durações A/B/C/pesado, impactos nos ticks 4/5/14/12, dano 4/6 HP, física do Roll de 2,5 blocos em 14 ticks e proteção no intervalo `3 <= idade < 10`. `CombatConfig`, `CombatMove`, `SwordTargeting`, `AttackState`, `RollRules` e `RollDamageFilter` foram preservados por hash.

## Preservação e limites

Polymesh R3 continua padrão, sem comando de laboratório. Nenhum clipe, modelo, geometria, UV ou peso foi alterado; o GLB de runtime com os sete clipes também permanece byte a byte idêntico ao A1. R3.1 e Godot foram preservados. CPM segue como ponte/fallback.

Armadura, capa, offhand, primeira pessoa, Slim e demais situações não homologadas continuam no fallback visual anterior. Os novos gestos B/C/pesado não foram acrescentados ao CPM. AUTO seleciona o conjunto pela arma, sem ampliar a cobertura visual desses fallbacks. Homologação remota com servidor dedicado permanece pendente.

O targeting também permanece o do A1: dano customizado somente contra os monstros hostis vanilla elegíveis. AUTO não acrescenta dano contra jogadores, animais/passivos ou entidades de outros mods. Com espada em AUTO/ON, o caminho vanilla de ataque a entidades é suprimido; OFF restaura esse caminho para diagnóstico. Essa limitação herdada não foi ampliada nem rebalanceada neste marco.

Resultados e limitações estéticas: [QA visual](M002_A1_1_VISUAL_QA.md) e [registro de testes](TEST_LOG_M002_A1_1.md).
