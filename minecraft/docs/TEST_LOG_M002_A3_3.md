# M002.A3.3 — registro de testes

Data: 14/11/2026. Windows, Microsoft JDK 17.0.20.1 x64, Minecraft Java 1.20.1, Forge 47.4.28, Gradle 8.8. Dependências locais já existentes, execução offline, sem novas instalações. Cliente OpenGL real com servidor integrado; resolução 854×480, distância de renderização 6 / simulação 5 no perfil de QA. Não é benchmark de desempenho.

## Procedimentos reproduzíveis

Na pasta `minecraft`, usando `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/gradle.ps1` como prefixo:

```text
runClient -PtoeRefinementTest --offline --console=plain
runClient -PtoeA1Test -PtoeGlobalRegression -PtoeRefinementRegression --offline --console=plain
runClient -PtoeRefinementTest -PtoeRefinementIframes --offline --console=plain
runClient -PtoeGlobalCoverage -PtoeRefinementRegression -PtoeRefinementVanilla --offline --console=plain
runClient -PtoeGlobalCoverage -PtoeRefinementRegression -PtoeRefinementCoverage --offline --console=plain
runClient -PtoeGlobalTest -PtoeRefinementRegression --offline --console=plain
clean build -PtoeR31 --offline --console=plain
clean build --offline --console=plain
```

Os perfis de teste incluem fixtures que alteram somente mundos locais de QA em `run-global`; não são perfis para abrir mundos pessoais. O build normal exclui essas classes. A bateria principal usa Survival, controles mapeados reais para ataques/pulo/Roll e solicitações adversariais explícitas nos casos identificados. Outras baterias herdadas usam fixtures Creative e eventos controlados; não são todas sessões manuais de gameplay.

## Cobertura e evidências

- `REFINEMENT_RUNTIME.json`: 21 casos, posições a cada tick, entrada de pulo, trava de liberação, clipe e tempo visual. A/B/C e pesado; avanço e compromisso; parede/borda; Roll cancelando A/pesado; pulo em cada golpe; tecla segurada; remapeamento; Auto-Jump; pacote forjado; knockback; impulso vertical; queda; Roll/Sprint; Slim; reconexão.
- `regression_a1/RUNTIME.json`: combo, pausa/reinício, dano leve/pesado, alternância de ataques, cancelamento de impacto, entradas remapeadas/contextuais, item sem suporte, shield, menu/chat, morte/respawn, reconexão e sincronismo visual dos impactos.
- `IFRAMES.json`: limites 2/3/9/10 ticks, dano corpo a corpo e projétil hostil; lava/queda/void não são protegidos. Janela original 3 ≤ idade < 10.
- `vanilla/RUNTIME.json`: picareta, arco, consumo, natação, salto normal, offhand, primeira pessoa e baú/contexto.
- `global/GLOBAL_RUNTIME.json`: inicialização automática em mundos existentes, Classic/Slim, troca de mundo e reabertura. O renderizador padrão funciona sem comandos de laboratório.
- `coverage/RUNTIME.json`: circuito herdado de 28 contextos A3, incluindo crouch, salto/queda, água, dano/morte, arco, consumo, tocha, madeira, direções, combate e contextos de fallback.
- `PRESERVATION_AND_KEYFRAMES.json`: hashes de assets e código preservados; quadros de referência do pesado.
- `ARTIFACT.json`: recursos empacotados, SHA-256 e exclusão de QA/R3.1 no JAR normal.

Relatórios em [m002_a3_3](../animation_pipeline/reports/m002_a3_3/); logs em `.logs/m002-a3-3-*.log`. O launcher de PowerShell pode encerrar com código 1 por mensagens nativas no stderr mesmo quando Gradle registra `BUILD SUCCESSFUL`; aprovação requer também o JSON de cada bateria, sem falhas.

## Falhas intermediárias e correções

A primeira bateria curta passou 52/54 verificações (`QUICK_FIRST_PASS.json`). O cenário Auto-Jump contava indevidamente um salto normal após o término do teste; a janela de medição foi corrigida. O salto adversarial era detectado, mas o pacote com `onGround=false` ainda cancelava a ação antes da reconciliação. O guard passou a reconciliar também antes das checagens de tick do jogador e no final do servidor, com tolerância curta e condicionada ao apoio. A bateria principal final passou integralmente.

A primeira regressão herdada passou 146/149 verificações (`regression_a1/FIRST_PASS.json`). Seu alvo deslocado lateralmente saiu do arco de 90° com o avanço maior do último golpe; o cenário de reinício enviava a nova ação no limite de recuperação, quando ainda podia ser descartada no fim de C. Para este perfil, o alvo de medição de dano foi alinhado à frente e recebeu resistência a knockback, e a nova pressão foi deslocada para depois da recuperação, com verificação após seu término. Não houve alteração de dano, alcance, seleção de alvo ou duração leve para acomodar o teste. As versões históricas do cenário permanecem selecionadas pelos perfis antigos.

## Avaliação visual e limites

Capturas reais do pesado parado e após Sprint mostram o fim do golpe e a volta à postura em pé com espada na mão. A seleção inclui combo, borda e Slim. A primeira inspeção não encontrou pose truncada nos quadros selecionados. Não se afirma ausência de defeitos em todos os quadros apenas com PNGs. Aprovação estética e sensação em jogo permanecem com o usuário.

Servidor dedicado, multiplayer remoto/latência artificial, pausas prolongadas de frames e todas as interações com outros mods não foram homologados. R3.1 teve compilação experimental; não se afirma QA visual completo dessa variante. A auditoria Godot cobre os 171 assets inventariados, não todo arquivo não inventariado daquele projeto. Nenhum arquivo Godot foi editado por este marco.

A migração dos padrões antigos foi exercitada pelo carregamento real do mundo existente. Para personalizações, a bateria verifica o predicado que exclui a tupla personalizada; não executa uma matriz de todos os valores possíveis nem outro mundo personalizado dedicado a essa política.

Os logs mantêm avisos de tangentes GLTF geradas, API Gradle depreciada e consulta Realms sem sessão válida no ambiente de desenvolvimento. Não bloquearam os testes locais; não se alterou autenticação para contorná-los. As baterias registram separadamente erros do renderizador.

<!-- RESULTS -->

## Resultados finais

| Bateria | Resultado |
|---|---:|
| refinements | 147/147 |
| combat | 145/145 |
| iframes | 24/24 |
| vanilla | 16/16 |
| coverage | 43/43 |
| worlds | 24/24 |
| Build normal | passou — 19s |
| Build R3.1 | passou — 24s |
| Auditoria de preservação e JAR | passou |

**399 verificações finais aprovadas**, além das duas compilações e auditorias. Distâncias reais, por diferença de posições a cada tick: A **0.400**, B **0.500**, C **0.600**, pesado **0.700**, combo **1.500 bloco**. Arredondamento exibido a 0,001; os JSONs preservam os valores completos.

Pesado: todas as 5 verificações de recuperação registraram **35 ticks / 1,75 s**, com impacto único no tick 12. As amostras após o fim retornam a Idle parado e a Sprint quando mantidos W/Sprint. A redução de controle em relação aos 47 ticks anteriores é **600 ms**. O trecho final visual dura **250 ms**.

- `wall_no_horizontal_reconcile`: Status[start=51078, distance=0.7, measured=0.19999998807907104, corrections=0]
- `edge_no_horizontal_reconcile`: Status[start=51229, distance=0.4, measured=0.14492187499999998, corrections=0]
- `cancel_a_roll`: Status[busy=false, started=true, startTick=51382, x=0.0, z=1.0, distance=2.499999999999999, reason=complete]
- `cancel_heavy_roll`: Status[busy=false, started=true, startTick=51532, x=0.0, z=1.0, distance=2.499999999999999, reason=complete]
- `hold_jump_no_buffer`: held maxY=100.0
- `remap_jump_no_buffer`: held maxY=100.0
- `autojump_blocked`: maxY=100.0 before moving=100.0
- `server_jump_rejected_without_cancel`: rejections=1 Status[attacking=false, started=false, accepted=1, impacts=1, hits=0, startedTick=52729, impactTick=52733, targetId=-1]

I-frames, dano bruto leve **4**, pesado **6**, combo **12 HP** e impactos únicos passaram nos cenários controlados. A prova de dano usa alvo alinhado e sem knockback para isolar o cálculo; não implica acerto automático em alvos móveis ou fora do arco.

JAR normal: `build/libs/towerofentropy-0.0.1.jar`, **2005968 bytes**, 108 entradas, SHA-256 `0A3FB1F0D29BC98A63EDAC656AD6D05408794ED8ACAA933D3B90581C1EF134DB`. Sem classes de QA ou R3.1 no artefato normal. O experimental está separado em `.downloads/m002_a3_3/experimental/`.

[Resultados estruturados](../animation_pipeline/reports/m002_a3_3/RESULTS.json) e [capturas selecionadas](../animation_pipeline/reports/m002_a3_3/REVIEW/README.md). Conclusão técnica aprovada nos cenários medidos; homologações e aprovação estética listadas acima continuam pendentes. Nenhum próximo marco iniciado.
