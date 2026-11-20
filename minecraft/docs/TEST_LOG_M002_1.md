# M002.1 — Registro de testes

13/11/2026. Windows 11, Java 17, Minecraft 1.20.1/Forge 47.4.28, Polymesh 1.0.0 e CPM 0.6.26a. Mundo de teste copiado para `run-r4/`, sem modificar os saves aprovados ou Godot.

## Fase A, concluída antes do Roll físico

Gate salvo em `animation_pipeline/reports/M002_R4_PHASE_A_GATE.json`. Cliente começou com R3 e desenhou antes de comandos/observador de compatibilidade. Validação visual 31/31, regressão de combate 132/132, 18 amostras de sincronização e builds normal/experimental passaram. O teste utilizou recursos empacotados, sem `player_models` manual. Não houve incompatibilidade grave que impedisse avançar.

## Testes de regras

`scripts/test-roll.ps1`: 104 verificações de direção, normalização, yaw, distância nominal e limites, incluindo NaN/infinito e intensidades inválidas. `test-combat.ps1`: 18 regras de geometria/linha do tempo de espada. `test-actions.ps1` e `test-locomotion.ps1`: arbitragem e histerese anteriores passaram.

Os arquivos de configuração de dano, targeting, geometria e `AttackState` são verificados por hash contra o backup. As durações/bytes do R3 e os arquivos R3.1 foram preservados. O inventário de 171 arquivos Godot é conferido por `verify-r4.cjs`.

## Execuções nativas e resultados

Resultados finais são consolidados em `animation_pipeline/reports/M002_1_RESULTS.json`, com checks individuais e telemetria em `M002_1_ROLL_RUNTIME.json`, `M002_1_COMBAT_RUNTIME.json`, `M002_1_RENDER_RUNTIME.json` e `M002_1_RESTART_RUNTIME.json`. Capturas ficam em `M002_1_visual/`. Logs brutos locais `.logs/m0021-*.log/json`.

```powershell
.\scripts\gradle.ps1 runClient -PtoeRollTest --console=plain
.\scripts\gradle.ps1 runClient -PtoeR4Test -PtoeCombatTest --console=plain
.\scripts\gradle.ps1 runClient -PtoeR4Test --console=plain
.\scripts\gradle.ps1 runClient -PtoeRollTest -PtoeRollRestart --console=plain
.\scripts\gradle.ps1 build -PtoeR31 --console=plain
.\scripts\gradle.ps1 clean build --console=plain
```

`toeRollTest`/`toeR4Test` adicionam apenas fixtures e diretório de teste; o renderer e a física usados são os do mod normal, sem flags de laboratório/ativação. Os testes estão fora do source set normal e não acompanham seu JAR.

A suíte física cobre oito direções, diagonais, yaw a 90°, entradas V/WASD pelo handler real, parede, borda/gravidade, spam, exclusão mútua, combat off, cancelamento por dano/menu/água, equipamento, primeira pessoa, rejeição de voo/NaN, carregamento frio, morte/respawn e desconexão no meio da esquiva. Os testes por tecla usam `KeyMapping.click` e os keybindings reais, não uma avaliação humana de teclado.

São conferidos distância, término normal `complete`, ausência de deriva/velocidade acumulada, limpeza das sessões e desenho/fallback correto. A telemetria lê cliente/servidor em fases de tick distintas: diferenças transitórias não devem ser rotuladas como uma medida de latência de rede. O estado final deve convergir.

As 132 regressões M002.0 foram mantidas. Duas asserções antigas, que exigiam ausência de deslocamento nos casos Roll, foram substituídas explicitamente pela distância física esperada; dano e demais condições não foram relaxados. O resultado original pré-Roll permanece no relatório de promoção.

Nos dois cenários Roll da suíte antiga, o zumbi foi deslocado lateralmente para x=1,5/z=3,0, ainda alcançável pela espada, mas fora do corredor da esquiva. A posição antiga passava a colidir com o jogador ao final do novo dash, e o empurrão vanilla posterior alterava distância/estado de repouso. As asserções de dano/rejeição/Idle continuam estritas; a distância é 2,5 ±0,08 bloco em corredor livre. Parede/borda e fallback físico são testados separadamente na suíte de Roll.

## Resultados finais

| Verificação | Resultado |
|---|---:|
| Suíte física ampliada, incluindo V/WASD, morte/respawn e desconexão ativa | **166/166** |
| Regressões de combate M002.0 | **132/132** |
| Renderização/fallback, incluindo Roll físico → Idle → Jog | **34/34** |
| Sincronização de dano/visual de Sword | **18/18** |
| Reinício em nova JVM, sem estado preso e com R3 padrão | **1/1** |
| Migração com modelo legado de locomoção, mas sem arquivo de ações | **7/7** |
| Regras puras Roll / espada | **104 / 18** passaram |
| Arbitragem e locomoção anteriores | passaram |
| Build normal limpo final | **BUILD SUCCESSFUL, 18 s** |
| Build experimental R3.1 final | **BUILD SUCCESSFUL, 20 s** |
| Erros de renderização | **zero** |
| Godot | **171 arquivos intactos** |

Nos oito sentidos e no yaw rotacionado, a distância medida foi **2,500 blocos**, com razão final `complete`, sem recorrer ao recuo de segurança. As quatro diagonais terminaram com componentes de aproximadamente ±1,767767 blocos, total unitário normalizado. No cenário de parede, o jogador avançou **0,200 bloco** até o limite de colisão. Na borda percorreu 2,500 blocos horizontalmente e caiu de Y=100 para Y=96 sobre a plataforma inferior. A separação horizontal cliente/servidor no estado final foi **zero** nesses ensaios; isso não é uma garantia de latência remota zero.

O teste visual confirmou `UAL_roll` durante a esquiva, `UAL_idle`/`UAL_jog` após a recuperação e Jog ao retomar WASD. Capturas nativas selecionadas:

- [Roll diagonal](../animation_pipeline/reports/M002_1_visual/m0021-forward_left.png).
- [Roll físico na fase final do clipe](../animation_pipeline/reports/M002_1_visual/r4-r3-physical-roll.png).
- [Retorno a Idle](../animation_pipeline/reports/M002_1_visual/r4-r3-physical-roll-return.png).
- [Jog retomado](../animation_pipeline/reports/M002_1_visual/r4-r3-jog-after-roll.png).
- [Queda na borda](../animation_pipeline/reports/M002_1_visual/m0021-edge.png).

Foram arquivadas **43 capturas**. A imagem junto à parede pode ficar obstruída pela câmera vanilla; a colisão é comprovada pela telemetria, não pela legibilidade dessa captura. As imagens são reais do cliente e não foram sintetizadas ou remodeladas.

JAR normal final: `build/libs/towerofentropy-0.0.1.jar`, **255.000 bytes**, 69 entradas, SHA-256 `EB0A486B31D00186179A0DAB614A87179F2FC0801B2A577E4758B47886699C46`. Inspeção ZIP confirmou o R3 aprovado byte a byte, ambos os recursos de compatibilidade CPM e ausência de R3.1, classes de laboratório e fixtures novas. Polymesh/CPM não foram shaded. O artefato experimental ficou em `.logs/m0021-artifacts/`.

Os 53 arquivos do backup foram conferidos: 45 permanecem idênticos, e oito têm alterações autorizadas de arquitetura/input/arbitragem/testes/metadados. A lógica/configuração de dano e targeting da espada continuam idênticas. Modelos R3/R3.1 e seus fontes Blender permaneceram intactos. Nenhum cliente ficou aberto.

## Problemas encontrados e corrigidos

1. Primeiro ensaio: o cliente usava a diferença entre relógios de nível para contar passos e executava um passo extra. O limite de servidor o detectava e recuava ao ponto seguro. Corrigido para exatamente 14 ticks locais de previsão e orçamento restante calculado a partir da posição atual no servidor. O ensaio seguinte passou em 86/86, com 2,5 blocos e término normal.
2. Asserções de pedidos rejeitados liam o histórico da esquiva anterior. Corrigida a fixture para comparar o tick de início do caso, preservando o histórico diagnóstico.
3. A suíte ampliada encontrou cancelamento real de pedidos V durante marcha lateral/traseira: o observador antigo ainda atravessava sua histerese de fallback. A transição foi ajustada somente enquanto há Roll válido pendente/ativo. A suíte completa foi repetida depois da correção.
4. Uma exceção checked não tratada na nova fixture impediu sua primeira compilação; corrigida no teste antes da execução ampliada. Não foi uma falha de gameplay.
5. A asserção de água esperava deriva zero depois de cancelar. A corrente vanilla continuava corretamente movendo o jogador; corrigiu-se a fixture para exigir liberação completa do Roll e movimento de fluido abaixo da velocidade de esquiva. Os testes em chão seco continuam exigindo ausência de deriva/velocidade residual. A física de água não foi suprimida para satisfazer o teste.
6. Revisão de migração: um arquivo legado de locomoção sem ações podia impedir a escolha do fallback completo. Corrigida a prioridade e executado um teste direcionado de carregamento frio com os arquivos legados presentes e ações ausentes. Os 7/7 checks passaram, e ambos os artefatos foram recompilados depois desse ajuste.

Os resultados iniciais ficam em `.logs/m0021-initial-runtime.json`, `m0021-86-checks.json` e `m0021-expanded-before-selector-fix.json`; não são usados como aprovação final.

## Alcance e limites

Validação realizada com servidor integrado e pacotes C2S/S2C reais, não em servidor dedicado remoto sob perda/latência artificial. A física usa travel/move vanilla; terreno e colisões podem reduzir a distância. O teste de borda confirma queda e não prova todas as formas de terreno, efeitos ou TPS. O watchdog cliente e timeouts não substituem ensaio de rede degradada.

O R3 aprovado mantém seus limites de deformação e render remoto/fallback; nada foi remodelado. O build experimental R3.1 continua disponível e não virou padrão. Nenhuma mudança de autenticação/licenciamento, stamina, invulnerabilidade, combos ou M002.2 foi feita.
