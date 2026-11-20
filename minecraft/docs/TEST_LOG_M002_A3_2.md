# M002.A3.2 — Testes e evidências

14/11/2026. Ambiente: Java Microsoft 17 x64, Minecraft 1.20.1, Forge 47.4.28, Polymesh 1.0.0 e CPM 0.6.26a. Nenhuma ferramenta nova instalada. Testes em `run-global`, mundo vanilla existente `Global Survival one`, configurações QA herdadas de 6 chunks, simulação 5 e limite de 60 FPS; isso não representa uma medição de FPS.

## Método e ocorrências

O driver usa controles remapeáveis de Minecraft e servidor integrado real. Paredes/precipícios são fixtures do save de QA; não são dependências de ativação do mod. Para o teste de colisão de avanço contra parede, envia-se a intenção pesada pela rede, evitando confundir o clique contextual de mineração com um ataque autorizado. Há teste adversarial de posição separado dos movimentos legítimos.

- A primeira rodada encontrou a espera CPM durante Sprint e falhas do próprio driver ao reabrir a tela a cada tick. Foi preservada em `FIRST_PASS.json`.
- O início imediato do Roll expôs perda de um passo: a contagem no ClientTick consumia tempo antes da entrada efetiva. A rodada com 2,3214 blocos está em `SECOND_PASS.json`; a contagem passou ao evento efetivo de movimento.
- O teste inicial de borda esperava um afastamento maior que a geometria do suporte amostrado. A asserção passou a exigir chão, Y preservado e avanço reduzido, sem relaxar a proteção física.
- O teste de impulso externo revelou reconciliação indevida. A captura de impulsos autorizados foi movida para antes do tick do jogador e passou a considerar seu decaimento vanilla. Não se concedem créditos com base em posições declaradas pelo cliente.
- Encadeamentos mantêm a autorização sem um pacote de liberação intermediário. Impulsos externos não são zerados no início de cada continuação.
- A sincronização final usa confirmação após o pacote vanilla do último passo e um watchdog, em vez de cortar a previsão por um relógio de chegada diferente. O limite de descontinuidade do observador foi alinhado ao envelope existente do Roll, preservando as validações cumulativas.

## Escopo da validação

As verificações incluem Roll parado, andando, Sprint, diagonais e parede; A/B/C/pesado; bloqueio manual; avanço; borda; spam; cancelamento por Roll; força externa; tentativa de posição fora do envelope; primeira pessoa; Slim; reconexão. As regressões de dano, i-frames e ações vanilla são registradas separadamente. Compilação, testes técnicos e aprovação visual são resultados distintos.

Fontes e resultados detalhados em `animation_pipeline/reports/m002_a3_2`. As fontes de QA ficam fora do JAR normal. O rollback do A3.1, incluindo JAR e fontes, está em `.downloads/m002_a3_2/baseline`.

## Comandos

```powershell
.\scripts\gradle.ps1 runClient -PtoeMovementTest --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeMovementTest -PtoeMovementIframes --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeMovementTest -PtoeMovementDirections --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeA1Test -PtoeGlobalRegression -PtoeMovementRegression --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeGlobalCoverage -PtoeMovementRegression -PtoeMovementVanilla --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeGlobalTest -PtoeMovementRegression --offline --console=plain
.\scripts\gradle.ps1 clean build -PtoeR31 --offline --console=plain
.\scripts\gradle.ps1 clean build --offline --console=plain
node animation_pipeline/scripts/audit-a3-2.cjs
```

Não há homologação de servidor dedicado, rede remota ou latência artificial. A aprovação estética do Roll remapeado e da sensação de comprometimento permanece com o usuário. Nenhum marco posterior foi iniciado.
<!-- RESULTS -->

## Resultados finais

| Bateria | Resultado |
|---|---:|
| movement | 142/142 |
| directions | 27/27 |
| combat | 145/145 |
| iframes | 24/24 |
| vanilla | 16/16 |
| worlds | 24/24 |
| Build normal | passou — 20s |
| Build R3.1 | passou — 23s |
| Auditoria de recursos/preservação | passou |

Avanços físicos medidos, por diferenças de posições registradas: **A 0.180, B 0.220, C 0.280, pesado 0.320 bloco**. Combo total 0.680 bloco; os limites de colisão reduzem essa distância quando necessário.

O controle voltou nas primeiras amostras aproximadamente 0,80 s após V, incluindo autorização/transporte locais, com 14 passos físicos e blend de saída de 80 ms. A amostragem das posições é de 10 Hz. Nos primeiros 500 ms de retomada em movimento não houve Idle intermediário nas amostras; Sprint retornou após a retomada vanilla. Colisões/queda posteriores no cenário não fazem parte dessa janela de transição.

A/B/C mantiveram compromisso contínuo. Após cancelar cada golpe pelo Roll, a distância própria da esquiva permaneceu 2,5 blocos. Avanços legítimos tiveram zero correções de posição; as tentativas adversariais foram rejeitadas separadamente. Impulsos externos e knockback passaram nos cenários testados; isso não homologa toda combinação de mods/forças.

JAR normal: `build/libs/towerofentropy-0.0.1.jar`, 1996935 bytes, SHA-256 `3FC7D25421854CA10212C5E8316214F704B6E179DC70565892502D831A9CF19C`. O experimental foi preservado em `.downloads/m002_a3_2/experimental/`. Não se instalaram dependências novas nem se alterou autenticação.

[RESULTS.json](../animation_pipeline/reports/m002_a3_2/RESULTS.json) reúne as métricas e a preservação. [Capturas críticas](../animation_pipeline/reports/m002_a3_2/REVIEW/README.md) mostram Roll, retorno à locomoção, ataques e Slim. A inspeção encontrou a pose final de Roll em pé nas capturas; empunhadura e clipes existentes permaneceram. A sensação de gameplay e a aprovação estética ficam reservadas ao usuário.
