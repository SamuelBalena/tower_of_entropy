# M002.R3 — Registro de testes

Data: 12/11/2026. Testes reais em Windows, cliente Forge 1.20.1/47.4.28, Java 17, CPM e Polymesh 1.0.0. Mundo descartável `run-r3/saves/TOE_R3`; suíte de combate usa a cópia `run-r3/saves/New World`. Original `run/`, R1, R2 e Godot preservados.

## Validações estruturais

`node animation_pipeline/scripts/validate-r3.cjs`: PASS. 54 arquivos aprovados preservados, 12 ossos, 816 vértices ponderados, 128 misturados, 64 no torso base+externo. Erro máximo da soma dos pesos: 2,98e-8; erro máximo bind × inversa: 7,55e-15. Erro máximo de orientação global dos dez ossos anteriores: 9,20e-7 nos tempos originais. Quatro durações iguais às anteriores e nenhuma borda aberta nos doze objetos. Extensões de repouso idênticas ao R2 (erro zero), UVs da cabeça/membros idênticas, cantos UV originais do torso presentes e todas as normais unitárias. Evidência: `animation_pipeline/reports/M002_R3_VALIDATION.json`.

`node animation_pipeline/scripts/validate-actions.cjs`: PASS; nove artefatos aprovados e 171 arquivos Godot preservados. `test-combat.ps1`: 18 regras PASS; `test-actions.ps1` e `test-locomotion.ps1`: PASS. Scripts não alteram gameplay.

## Execuções no Minecraft

Resultados consolidados e medições ficam em `animation_pipeline/reports/M002_R3_TEST_RESULTS.json` e capturas em `M002_R3_visual/`; logs brutos locais em `.logs/r3-*.log/json`.

| Verificação | Resultado |
|---|---:|
| Perfil CPM, poses equivalentes + fallback/reconexão | 28/28 |
| GLB R2 intacto no mesmo wrapper | 29/29 |
| R3, incluindo auditoria real de flexão do torso | 30/30 |
| Regressão completa M002.0 com R3 ativo | 132/132 |
| Sincronização visual no impacto servidor | 18/18 |
| Reinício em nova JVM, estado desligado | 1/1 |
| Máxima flexão da coluna: Jog, Sword e duas laterais Roll | 4/4 |
| Build experimental | passou, 13 s |
| Build normal limpo final | passou, 20 s |

Foram arquivadas 76 capturas PNG com hashes no consolidado, incluindo as 48 poses equivalentes dos três perfis. A inspeção das quatro imagens de estresse confirma continuidade da cintura e as limitações de volume/proximidade do chão no Roll. Não foi identificado novo buraco grande ou erro grave de UV nessas amostras.

JAR normal final: `build/libs/towerofentropy-0.0.1.jar`, 93.440 bytes, 50 entradas, SHA-256 `414A22B0F603BB4A552675599787C1B2594FDCF6D306094EEA18248A52FAEFE6`. Inspeção ZIP: sem classes `renderlab`, Polymesh incorporado, GLB, Blend, FBX ou modelos CPM privados. O JAR experimental fica somente em `.logs/r3-artifacts/`, fora do versionamento; não é distribuição pública.

Na regressão foram registrados 7.547 desenhos R3, média de 0,504 ms de CPU e zero erros de renderização. Impacto continua no tick 4. Caminhada de referência e caminhada atacando percorreram os mesmos 8,63060733762779 blocos. Não houve alteração dos sistemas aprovados.

```powershell
.\scripts\gradle.ps1 runClient -PtoeR3 --console=plain
.\scripts\gradle.ps1 runClient -PtoeR3 -PtoeR3Mode=r2 --console=plain
.\scripts\gradle.ps1 runClient -PtoeR3 -PtoeR3Mode=cpm --console=plain
.\scripts\gradle.ps1 runClient -PtoeR3 -PtoeCombatTest --console=plain
.\scripts\gradle.ps1 runClient -PtoeR3 -PtoeR3Restart --console=plain
.\scripts\gradle.ps1 runClient -PtoeR3 -PtoeR3Stress --console=plain
.\scripts\gradle.ps1 build -PtoeR3 --console=plain
.\scripts\gradle.ps1 clean build --console=plain
```

As verificações visuais automáticas medem desenho efetivo, um item por desenho, ausência de segundo corpo vanilla, GLerror zero, playback, primeiro jogador/remoto, retorno por comando C2S ao CPM, primeira pessoa, fallbacks armadura/offhand/capa, retomada e reconexão. A auditoria usa o modelo carregado pelo Polymesh, não somente o GLB no disco. Reinício usa uma JVM nova e verifica renderer/combate/observador desligados.

Primeira execução R3: 29/30. O teste `off_restores_cpm` tirava o contador imediatamente antes da viagem assíncrona do comando ao servidor; alguns frames ainda desenhavam até o comando retornar. CPM idle já estava ativo e as camadas nativas restauradas. Corrigiu-se a fixture para iniciar a contagem após 15 ticks, verificando também `enabled == false`; o comportamento do comando não precisou mudar. O resultado inicial fica arquivado, sem ocultar a falha.

A consolidação inicialmente leu logs PowerShell UTF-16 como UTF-8. O coletor foi corrigido para detectar o BOM e consolidou com sucesso; isso não foi uma falha do build. Não foi necessário instalar dependências nem alterar autenticação.

O teste de capa injeta uma referência de textura vanilla no cache local de PlayerInfo para testar a decisão de fallback. Não prova render/deformação de uma capa de conta real. O jogador remoto é uma réplica cliente, suficiente para confirmar que seu evento não é cancelado pelo R3; não é homologação multiplayer em servidor dedicado.

## Desempenho e inspeção visual

O consolidado registra média e máximo de CPU do bloco Polymesh por perfil. São medições instrumentadas de laboratório a 60 FPS, com snapshots e aquecimento; não são tempos de GPU, percentis ou benchmark de FPS. Comparação R2/R3 usa o mesmo wrapper R3 e varia apenas o GLB. O renderer CPM não é cronometrado: no perfil `cpm`, a métrica mede somente as 302 chamadas Polymesh da seção posterior de testes de fallback, não o custo de desenhar CPM.

| GLB no wrapper R3 | Chamadas | Média CPU | Máximo CPU observado |
|---|---:|---:|---:|
| R2 | 3.786 | 0,483 ms | 8,774 ms |
| R3 | 3.792 | 0,553 ms | 6,683 ms |

Diferença média observada: +0,070 ms (+14,54%) neste ensaio. Os 816 vértices R3 representam +10,87% sobre os 736 do R2. Não se deduz ganho de desempenho a partir do máximo menor, nem se garante esse delta fora deste ambiente.

Foram inspecionadas capturas reais de Idle, Jog, Sword, Roll e equipamento. O comparador HTML permite alternar clip e ângulo nos três perfis. Há flexão contínua da cintura R3, sem nova falha grave de UV nas amostras; persistem limitações de volume e sobreposição em flexão extrema. A avaliação estética final é do usuário. Cobertura visual não equivale a inspeção de todos os frames ou de todas as skins.

## Limites da regressão

A suíte M002.0 exercita comandos, dano servidor, geometria, rejeições, spam, sessão, alcance, arco e fluxo cliente/CPM. O trace R3 confere a fase visual no instante do impacto sem alterar o servidor. V/J e seus handlers originais ficam intactos; não se alega teste físico de todas as teclas por interação humana. Movimento/colisão vanilla e sistema de combate não receberam alterações. Nenhuma nova mecânica ou M002.2 foi implementada.
