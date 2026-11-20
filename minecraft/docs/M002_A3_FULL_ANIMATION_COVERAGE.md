# M002.A3 — Cobertura consolidada de animações

13/11/2026. A2 foi concluído e reaproveitado. Polymesh R3 permanece o renderer padrão com a mesma geometria, UVs, pesos e nove clipes aprovados. Este lote acrescenta movimentos ao mesmo pipeline; não cria um renderer novo ou sistemas de RPG.

**Não é 100% de cobertura.** Há fallbacks deliberados, um candidato de mineração bloqueado e combinações de arco ainda não homologadas. A aprovação estética continua com o usuário durante gameplay real.

## Controle e prioridades

`VisualContext` observa pose, deslocamento, água, vida, `hurtTime`, uso efetivo de itens, equipamento e alvo vanilla. Não escreve velocidade, vida, colisão, oxigênio ou resultados de interação. `HorizontalMotion`/fase do A2 continuam em uso.

Ordem natural: morte → Roll autorizado/cancelamentos → ataques de espada → estado físico/uso de item → locomoção. Natação não vira corrida; saída real do chão escolhe Start/Loop e aterrissagem curta; crouch é o estado vanilla, não crawling. Contextos excluídos e equipamento ainda não suportado entregam o corpo para vanilla/CPM.

Reações duram aproximadamente 200 ms visuais, com intervalo mínimo de 350 ms. O impulso horizontal real após dano pode escolher Knockback; não há hitstun físico. Proteção do Roll e o relógio servidor permanecem os anteriores. Consumir usa o progresso real do item. Árvores usam o loop somente durante mineração efetiva de logs com machado; não há dano de combate próprio para machado.

Arco é visual sobre a mecânica vanilla: preparo pelo tempo real de uso, hold enquanto USE continua e release curto ao soltar. Não passa pelo ataque pesado de espada. O item usa o renderer vanilla com a entidade para manter as propriedades de puxada, encantamentos e modelo. O sprite diagonal vanilla recebeu orientação de montagem para apontar a flecha na direção da mira, sem editar textura/modelo do item.

## Tabela final de cobertura

Licenças: **UAL = Quaternius, CC0 1.0**; **Archer = Kevin Iglesias, EULA própria verificada no PDF oficial**. Detalhes e links em [compatibilidade](M002_A3_ASSET_COMPATIBILITY.md). “Runtime” significa teste em cliente Forge integrado; não homologação multiplayer remota.

| Ação Minecraft | Arquivo/clipe selecionado | Origem/licença | Status | Disparo real | Teste / limite visual |
|---|---|---|---|---|---|
| Parado | `Idle_Loop` | UAL1 / CC0 | IMPLEMENTADO | Deslocamento nulo | A2 preservado; runtime A3 |
| Caminhada lenta | `Walk_Loop` | UAL1 / CC0 | IMPLEMENTADO | Velocidade real baixa | A2 preservado; sem tecla nova |
| Jog | `Jog_Fwd_Loop` | UAL1 / CC0 | IMPLEMENTADO | Deslocamento normal | Folga de pés anterior permanece |
| Sprint | `Sprint_Loop` | UAL1 / CC0 | IMPLEMENTADO | Sprint vanilla | A2 preservado; saltos naturais do ciclo |
| Para trás | `HumanM@Walk01_Backward.fbx` | Archer / EULA | IMPLEMENTADO | Direção real reversa | Runtime; cadência ainda avaliável |
| Lateral esquerda | `HumanM@Walk01_Left.fbx` | Archer / EULA | IMPLEMENTADO | Direção lateral real | Runtime; yaw relativo ao olhar |
| Lateral direita | `HumanM@Walk01_Right.fbx` | Archer / EULA | IMPLEMENTADO | Direção lateral real | Runtime; sem girar Jog para preencher |
| Agachado parado | `Crouch_Idle_Loop` | UAL1 / CC0 | IMPLEMENTADO | `isCrouching()` | Runtime; pose mais profunda que vanilla |
| Agachado avançando | `Crouch_Fwd_Loop` | UAL1 / CC0 | IMPLEMENTADO | Crouch e avanço real | Runtime; sneak/colisão vanilla |
| Agachado reverso/lateral | Sem clipe correspondente | — | FALLBACK | Crouch direcional não frontal | Não reproduz marcha frontal ao contrário |
| Saída do chão | `Jump_Start` | UAL1 / CC0 | IMPLEMENTADO | Transição física para o ar | Trecho final amostrado; não atrasa salto |
| Subida/queda no ar | `Jump_Loop` | UAL1 / CC0 | IMPLEMENTADO | Sem chão | Tempo acompanha ar, não impõe duração |
| Aterrissagem | `Jump_Land` | UAL1 / CC0 | IMPLEMENTADO | Retorno ao chão, baixa velocidade | Aproximadamente 200 ms; andando pode ir direto ao ciclo |
| Água quase parado | `Swim_Idle_Loop` | UAL1 / CC0 | IMPLEMENTADO | Na água, baixa velocidade | Runtime; ajuste vertical visual |
| Natação em movimento | `Swim_Fwd_Loop` | UAL1 / CC0 | IMPLEMENTADO | Movimento real na água | Runtime; oxigênio e física vanilla |
| Dano genérico | `Hit_Chest` | UAL1 / CC0 | IMPLEMENTADO | Vida reduzida e hurt real | Reação curta; sem bloquear controles |
| Dano com impulso | `Hit_Knockback` | UAL2 / CC0 | IMPLEMENTADO | Impulso observado após dano | Confirmado na rodada lateral; não aplica knockback |
| Reação específica de cabeça | `Hit_Head` | UAL1 / CC0 | FALLBACK | Sem evento headshot vanilla confiável | Convertido/disponível, sem disparo automático inventado |
| Morte | `Death01` | UAL1 / CC0 | IMPLEMENTADO | Morte real | Fluxo e botão de respawn preservados |
| Arco parado | `HumanM@BowIdle01.fbx` | Archer / EULA | IMPLEMENTADO | Arco na mão principal, parado | Corpo da fonte; mão de suporte adaptada |
| Preparar arco | `HumanM@BowShot01 - Load.fbx` | Archer / EULA | IMPLEMENTADO | Uso real do arco | Vanilla decide carga/força |
| Manter mira | `HumanM@BowShot01 - Hold.fbx` | Archer / EULA | IMPLEMENTADO | USE mantido após carga | Modelo vanilla de bow puxado |
| Soltar disparo | `HumanM@BowShot01 - Release.fbx` | Archer / EULA | IMPLEMENTADO | Fim de uso válido | Flecha/ammo/projétil vanilla verificados |
| Mira andando/agachado/no ar/água, ângulos extremos | Vanila/CPM | Minecraft / compatibilidade | FALLBACK | Combinações ainda não aceitas visualmente | Arco permanece funcional; não se anuncia layering completo |
| Comer/beber | `Consume` | UAL2 / CC0 | IMPLEMENTADO | UseAnim EAT/DRINK real | Maçã/fome e garrafa vanilla verificadas |
| Segurar tocha parado | `Idle_Torch_Loop` | UAL1 / CC0 | IMPLEMENTADO | Tocha na mão principal, parado | Mão visual da fonte; sem luz dinâmica nova |
| Cortar madeira | `TreeChopping_Loop` | UAL2 / CC0 | IMPLEMENTADO | Machado minerando logs | Provisório; velocidade, partículas e quebra vanilla |
| Minerar com picareta | `AS_MinerSwingingPicaxe.uasset` | Free100Animations / licença não verificada | BLOQUEADO | Ação vanilla de mineração | Falta exportador/licença verificável; bloco vanilla quebrou no teste |
| Interação genérica | `Interact` | UAL1 / CC0 | FALLBACK | Controles vanilla | Candidato convertido, sem associação genérica inadequada |
| Abrir baú | `Chest_Open` | UAL2 / CC0 | FALLBACK | Interface vanilla | UI abre sem atraso; clipe não sobrepõe o menu |
| Coleta automática | `PickUp_Table` | UAL1 / CC0 | FALLBACK | Coleta vanilla | Não equivale a alcançar uma mesa |
| Sentar/levantar | `Sitting_Enter/Idle_Loop/Exit` | UAL1 / CC0 | EXCLUÍDO | Sem ação nativa compatível neste escopo | Não associado a montarias |
| Leves A/B/C e pesado | Clipes A1 aprovados | UAL2 / CC0 | IMPLEMENTADO | ATTACK/USE com espada | Regressões autoritativas preservadas |
| Roll físico | `Roll` | UAL1 / CC0 | IMPLEMENTADO | V / ação autorizada | Física, i-frames e cancelamentos preservados |
| Escada vertical, subida/descida | Nenhum novo | Vanilla/CPM | EXCLUÍDO | `onClimbable()` | Fallback testado |
| Rastejar | Nenhum novo | Vanilla/CPM | EXCLUÍDO | Pose SWIMMING fora d'água | Não confundido com natação |
| Elytra/voo | Nenhum novo | Vanilla/CPM | EXCLUÍDO | Voo/Elytra | Gates preservados; sem clipe substituto |
| Remar/montarias | Nenhum novo | Vanilla/CPM | EXCLUÍDO | Passageiro | Sem animações customizadas de sentar/remar |
| Pescar | Nenhum novo | Vanilla/CPM | EXCLUÍDO | Vara de pesca | Fallback verificado |
| Colocar blocos | Nenhum novo | Vanilla/CPM | EXCLUÍDO | Swing/uso contextual | Não reaproveita Interact para colocação |

## Versão jogável e circuito

Execute `powershell -ExecutionPolicy Bypass -File scripts/play-a3.ps1` e abra o mundo da pasta **TOE_A3** em Singleplayer (o título herdado pode aparecer como New World). Nenhum comando de ativação é necessário. Use skin Classic; F5 é o controle vanilla de perspectiva. Primeira pessoa, armaduras, capa, offhand e mão dominante esquerda mantêm os fallbacks anteriores.

O circuito fica no perfil `run-a3`, separado do mundo normal:

- Spawn `(0,100,0)`: inventário com espada, arco/flechas, ferramentas, maçãs, tochas e blocos; baú de reposição perto de `(-3,100,0)`.
- Sul `z≈18`: alvo de feno para arco; ao norte, corredor de teto baixo em `z=-10..-4` para sneak.
- Leste `x=8..18,z=3..15`: piscina para água/natação; oxigênio continua vanilla.
- Oeste `x=-8/-10,z=4..12`: madeira/pedra para machado/picareta; plataforma/escada de teste mais a oeste para pulo/queda.
- Cercado `x=-20..-14,z=10..16`: zombie vanilla para combate e dano recebido. O mundo de testes usa keepInventory e respawn no spawn; isso não foi aplicado ao mundo normal.

Apresentação: [visualizador local](../animation_pipeline/reports/m002_a3/VIEWER.html) e seleção compacta na mesma pasta. Os PNG são capturas reais; cenas podem mostrar fallback e interfaces vanilla. [Testes](TEST_LOG_M002_A3.md) distinguem execução técnica, limites e aprovação estética pendente.
