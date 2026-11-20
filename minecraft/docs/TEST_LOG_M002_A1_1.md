# M002.A1.1 — Registro de testes

13/11/2026, Windows. Minecraft 1.20.1, Forge 47.4.28, Microsoft Java 17.0.20.1 x64, Gradle 8.8, Polymesh 1.0.0 e CPM 0.6.26a. Sem instalação de ferramentas ou dependências novas.

## Isolamento

O QA AUTO utiliza uma cópia do mundo anterior em `run-a1-1/saves/TOE_A1_1`. Não altera o mundo normal `run/`, Godot ou fontes de animação. Regressões antigas utilizam o mundo de testes anterior `run-r4/`. As evidências A1.1 são separadas dos relatórios históricos A1/M002.1.

Backup prévio de código/scripts/fixtures em `.downloads/m002_a1_1/backup`; hashes em `BASELINE.json` e `ASSET_BASELINE.json`. Fixtures novas entram apenas com `-PtoeA11Test`. A câmera orbital também é exclusiva dessa fixture, sem código de câmera adicional no mod normal.

## Resultados executados

| Suíte | Resultado | Evidência |
|---|---:|---|
| AUTO, controles naturais, comandos e 24 sequências reais | **147/147** | `RUNTIME.json` |
| Reinício em nova JVM, AUTO com espada salva | **3/3** | `RESTART.json` |
| Regressão A1, incluindo janelas de i-frames | **150/150** | `regression_a1/RUNTIME.json` |
| Regressão física do Roll | **167/167** | `ROLL_REGRESSION.json` |
| Ordem dos clipes e ausência de reinício por tick | **49/49** | `PLAYBACK_AUDIT.json` |
| Visualizador em Edge existente | **6/6** | `VIEWER_TEST.json` |

Resultados ficam em `animation_pipeline/reports/m002_a1_1/`. O resumo estruturado é [RESULTS.json](../animation_pipeline/reports/m002_a1_1/RESULTS.json).

### Fluxo real de entrada

A fixture AUTO usa `KeyMapping.click`, consumido pelo processamento normal de controles do Minecraft, pelo evento Forge e pela rede real. Não envia diretamente `requestAttack` nos cenários naturais. F6/F7 foram usados temporariamente para ATTACK/USE remapeados e depois restaurados. Isso testa o caminho de KeyMapping/engine; não simula um mouse físico pelo Windows.

Foram validados equipar espada sem comando, primeiro ataque, troca para item sem suporte durante ação, ON/OFF/AUTO, pausa e combo, pesado e botão USE mantido, cancelamento de A/B/C/pesado por V, morte/respawn, reconexão e reinício. OFF não é revertido automaticamente enquanto dura a sessão; reconexão restaura AUTO.

### Interações e dano

- Escudo realmente entrou em uso vanilla, sem pesado.
- USE abriu uma interface de baú vanilla, sem ataque customizado.
- Picareta quebrou um bloco pelo caminho normal de mineração.
- Picareta contra Vindicator permaneceu vanilla: saúde 100 → 96.
- OFF com espada permaneceu vanilla: saúde 100 → 94.
- USE mantido com pesado aplicou somente 6 HP: saúde 100 → 94, uma ação/um impacto.
- ATTACK/USE remapeados aplicaram 4 + 6 HP: saúde 100 → 90, duas ações/dois impactos.
- A regressão A1 confirmou 4 HP leve, 6 HP pesado, 12 HP no combo completo e impactos cancelados não aplicados.

Menus/chat, item sem conjunto e prioridades contextuais também permanecem cobertos pela regressão A1. Não se afirma que todos os blocos interativos ou todos os mods de terceiros foram testados.

### Roll e i-frames

A regressão A1 reexecutou as fronteiras 2/3/9/10 ticks com eventos Forge de melee, flecha hostil, lava, queda e vazio. A física reexecutou oito direções, WASD/diagonais, paredes, bordas/gravidade, spam, interrupções, equipamento/primeira pessoa, morte e reconexão. Os 104 checks puros de Roll, 18 de geometria/impacto e as suítes de arbitragem/histerese anteriores também foram reexecutados.

O total físico passou de 168 no A1 para 167: `death_attack_excluded` só era emitido se ainda existisse uma sessão após morte; AUTO já a remove antes desse ponto. Os checks explícitos de morte/respawn e limpeza permanecem. Não é uma falha omitida.

As expectativas antigas de “sessão desligada após entrar” foram atualizadas para “AUTO sem ação antiga”. Os comandos ON/OFF e as configurações de dano continuam compatíveis. A fixture R4 foi ajustada para essa mudança de padrão e sua compilação foi verificada; a comparação visual R4 completa não foi repetida.

## QA visual

Foram produzidas 2.184 imagens reais e comparadas amostras críticas antes/depois nos três ângulos, além de leitura de telemetria e amostragem do GLB. O navegador confirmou carregamento local, seleção, avanço de um quadro e velocidade: em intervalos equivalentes, 1× avançou aproximadamente 0,6333 s e 0,25× avançou 0,1583 s, razão 4. Isso não é benchmark de desempenho do Minecraft.

Há achados visuais reais: entrada abrupta do pesado, C → Jog com grande mudança de pose, um quadro intermediário Idle nos cancelamentos e separação dos pés do solo em B/C. Não foram corrigidos por alteração de animações ou modelo. A análise detalhada e suas limitações estão em [M002_A1_1_VISUAL_QA.md](M002_A1_1_VISUAL_QA.md). A percepção de fluidez em reprodução completa e a aprovação estética continuam com o usuário.

## Preservação

`verify-a11.cjs` confirmou os oito arquivos de modelo/animação selecionados, incluindo GLB de runtime com sete clipes, byte a byte idênticos ao A1. Geometria, UVs, pesos, durações, modelos R3/R3.1 e 171 arquivos Godot permanecem intactos. Configuração/balanceamento, targeting, impactos, regras do Roll e filtro de i-frames também foram preservados por hash.

## Reprodução

```powershell
.\scripts\test-a11.ps1
.\scripts\test-a11.ps1 -Restart
.\scripts\gradle.ps1 runClient -PtoeA1Test -PtoeA11Regression --offline
.\scripts\gradle.ps1 runClient -PtoeRollTest -PtoeA1Regression -PtoeA11Regression --offline
node animation_pipeline/scripts/verify-a11.cjs
node animation_pipeline/scripts/summarize-a11.cjs
.\scripts\gradle.ps1 build -PtoeR31 --offline
.\scripts\gradle.ps1 clean build --offline
```

`test-a11.ps1` verifica o conteúdo do relatório, não apenas o código de saída do cliente. O visualizador pode ser aberto diretamente, sem iniciar o jogo. Nenhum próximo marco foi iniciado. Não houve publicação, instalação em launcher externo ou alteração de autenticação/licenciamento.

## Builds e artefato final

- Build normal `clean build`: **PASS, 18 s**.
- Build experimental R3.1: **PASS, 16 s**.
- Compilação da fixture R4 atualizada: **PASS, 12 s**; não equivale à repetição completa do antigo QA R4.
- JAR normal: `build/libs/towerofentropy-0.0.1.jar`, **408.214 bytes**, 80 entradas.
- SHA-256: `189CDDDB9B8C54A70AEF0EE43D76F8CA8BD79CECAD5ECB2FACA36F62AF33C820`.

A auditoria confirmou que o JAR normal não contém fixtures A11/A1/Roll/R4, renderizador experimental R3.1, visualizador ou screenshots. O GLB empacotado corresponde à cópia de runtime preservada. Há apenas os avisos de APIs depreciadas já conhecidos, sem falha de compilação. [Auditoria do artefato](../animation_pipeline/reports/m002_a1_1/ARTIFACT.json).

O QA técnico não homologa servidor dedicado com latência real, todos os estados de equipamento/fallback ou toda a interação de mods externos. A restrição de targeting a hostis vanilla do A1 continua vigente; não houve ampliação para passivos/PvP.

O build experimental comprova compilação e preservação da variante R3.1; não homologa os novos controles AUTO nesse renderer, que conserva seu conjunto experimental anterior. A validação de gameplay e captura deste marco usa o R3 padrão.
