# M002.0 — Registro de testes

Data: 2026-11-12 (America/Sao_Paulo). Minecraft 1.20.1, Forge 47.4.28, Microsoft OpenJDK 17.0.20.1 x64, Gradle 8.8 / ForgeGradle 6.0.54, CPM 0.6.26a. Nenhum programa, biblioteca ou mod novo foi instalado.

Arquitetura, arquivos alterados e regras: [M002_0_COMBAT_ARCHITECTURE.md](M002_0_COMBAT_ARCHITECTURE.md).

## Métodos e reprodução

```powershell
.\scripts\test-combat.ps1
.\scripts\test-actions.ps1
.\scripts\test-locomotion.ps1
node animation_pipeline/scripts/validate-actions.cjs
.\scripts\gradle.ps1 build --console=plain
.\scripts\gradle.ps1 runClient --console=plain
```

O script Gradle define JDK 17 e GRADLE_USER_HOME local e executa o wrapper oficial. Não é necessário Gradle global. O build não contém testes JUnit: as regras puras são executadas explicitamente pelos scripts Java acima, e a integração roda no Minecraft real.

Suíte opt-in (não usar em um save de jogo pessoal):

```powershell
.\scripts\gradle.ps1 runClient -PtoeCombatTest --console=plain
.\scripts\gradle.ps1 runClient -PtoeCombatRestartCheck --console=plain
.\scripts\gradle.ps1 runClient -PtoeActionTest --console=plain
```

`toeCombatTest` abre exclusivamente o save descartável preexistente `run/saves/New World`, prepara uma plataforma em torno de (0,100,0), remove entidades não jogadoras da área, cria monstros vanilla sem IA, ajusta hora, visão noturna, inventário e modo Creative, injeta cliques pelos KeyMappings reais e usa o pacote C2S real. As configurações de câmera/HUD/pausa são restauradas; a fixture de mundo/inventário não pretende preservar um save pessoal. O cliente de teste fecha após terminar. Se esse save não existir, a suíte aborta sem criar/alterar outro mundo.

Os alvos básicos são zumbis com armadura base/equipamentos removidos, para isolar os 4 HP brutos. Há também esqueleto, vaca, dois zumbis simultâneos, teste de armadura 10 e teste da janela vanilla de invulnerabilidade. As alterações de fixture não pertencem ao controlador de combate. A suíte normal `runClient` não abre mundos ou cria alvos automaticamente.

O teste de reinício abre um processo novo e verifica ausência de sessões antes de qualquer ativação. O teste M001.3 reaproveita a suíte anterior de animações, sem substituí-la por uma simulação de combate.

## Regras isoladas

`test-combat.ps1`: **18 verificações passaram**, cobrindo frente/trás, alcance exato e excedido, limites do arco, yaw rotacionado, altura, direção indefinida, NaN, ausência de dano pendente, início idempotente, impacto no tick configurado, consumo único, término e ticks atrasados.

`test-actions.ps1` e `test-locomotion.ps1` também passaram, preservando arbitragem, histerese e fallback dos marcos anteriores.

## Integração no cliente real

A primeira execução passou **110/110 verificações**, com encerramento `BUILD SUCCESSFUL in 3m 5s` em `.logs/m002_0-client.log`. Foi acrescentada cobertura de comandos reais, armadura, ataque durante Jog e comparação de deslocamento; o resultado final é registrado abaixo.

**Suíte final: 132/132 verificações passaram**, com `BUILD SUCCESSFUL in 3m 35s` em `.logs/m002_0-client-validated.log`. Deslocamento horizontal estacionário: zero. Caminhada de referência e caminhada com ataque mediram igualmente **8,63060733762779 blocos**, incluindo desaceleração após soltar a tecla. O retorno a Jog durante movimento e a Idle após parar foi confirmado.

Evidências destinadas ao versionamento:

- [132 resultados estruturados](../animation_pipeline/reports/M002_0_RUNTIME_TESTS.json).
- [Preservação dos arquivos aprovados](../animation_pipeline/reports/M002_0_PRESERVATION.json).
- [Espada antes do impacto](../animation_pipeline/reports/M002_0_visual/toe-m002_0-sword-before-impact.png).
- [Ataque contra zumbi](../animation_pipeline/reports/M002_0_visual/toe-m002_0-iron_front.png).
- [Reação/morte vanilla](../animation_pipeline/reports/M002_0_visual/toe-m002_0-normal_death.png).
- [Roll visual com combate ligado](../animation_pipeline/reports/M002_0_visual/toe-m002_0-roll_busy.png).

As capturas são do cliente real, sem síntese ou edição. Uma captura não demonstra empunhadura contínua nem sincronização sob latência remota.

Reinício em um processo separado: **1/1 passou**, sem sessões de combate/locomoção antes de ativação. `BUILD SUCCESSFUL in 59s`, `.logs/m002_0-restart.log`. [Resultado estruturado](../animation_pipeline/reports/M002_0_RESTART_TEST.json).

Regressão final M001.3: **37/37 passaram**, sem alterar a suíte ou os critérios, em `.logs/m002_0-animation-regression-final.log`. [Resultado estruturado](../animation_pipeline/reports/M002_0_ANIMATION_REGRESSION.json). Inclui tecla remapeada, chat/inventário/controles, J visual sem espada, Roll, prioridades, off, salto, retorno a Idle/Jog e reconexão. As opções locais foram restauradas byte a byte ao backup após encerramento normal do cliente.

| Cenário | Evidência verificada automaticamente |
|---|---|
| Modo inicial desligado | nenhuma sessão de combate após login |
| Comandos | árvore antiga preservada; on/off/status de combate registrados; guarda de operador |
| J sem arma, diamante ou ferro somente na mão secundária | pedido recusado; nenhum dano funcional |
| J com ferro e zumbi à frente | animação CPM registrada no cliente; vida 20→16; um impacto/hit |
| Momento do impacto | vida intacta antes; diferença exata de quatro game ticks entre start e impact |
| Fora de alcance, atrás, fora do arco, altura excessiva | uma tentativa, nenhum dano |
| Parede sólida | traçado bloqueado; vida intacta |
| Rotação de yaw e alvo dentro do arco | seleção segundo a orientação do servidor |
| Dois zumbis no setor | somente o mais próximo perde vida |
| Vaca e jogador | vaca intacta; ServerPlayer real recusado pelo predicado de alvo |
| J repetido durante a ação | uma aceitação, uma tentativa e um dano |
| Invulnerabilidade vanilla temporal | dano prévio de 4; golpe de 4 durante a janela é recusado sem ignorar a regra |
| Armadura 10 | dano bruto 4 reduzido pelo vanilla; sem fórmula própria de armadura |
| Alvo com 3 HP | morte normal após o golpe, sem entidade customizada |
| Off antes do impacto, troca de arma, pulo, animação off | cancelamento; nenhum dano posterior |
| Virar ou afastar alvo antes do impacto | seleção reavaliada no momento; miss |
| Comando visual sword com combate ligado | gesto sem criar ataque funcional |
| J após combat off | teste visual anterior; sem dano |
| Roll ativo e tentativa de Sword | pedido recusado, sem interromper Roll ou causar dano |
| V em modo combate | Roll visual, sem dano |
| Término das ações | Idle ou Jog conforme movimento; sem ação pendente |
| Jog com ataque | retorno a Jog, mesma distância da referência sem ataque |
| Reconexão e reinício | sessões experimentais ausentes ao entrar |

As verificações medem vida, contadores lógicos do servidor, gameTime, parâmetros CPM no cliente, estado da locomoção e posições reais. Nenhum pacote fornece alvo ou dano. O teste de exclusão de jogadores usa o ServerPlayer real na regra; não representa uma sessão com dois clientes remotos.

## Preservação

O validador M001.3 confirmou novamente nove modelos/projetos aprovados e 171 arquivos Godot com hashes idênticos, além das durações/rig dos clipes. Os dois binários M001.3 são comparados aos hashes publicados no relatório anterior. Não foram executadas conversões ou exportações novas.

Fontes, modelos CPM, caches, logs e saves continuam fora do versionamento. Scripts, documentação e resultados estruturados são destinados ao versionamento. O JAR não deve conter modelos locais ou classes CPM incorporadas.

## Ocorrências

Os primeiros builds sob restrição de sandbox falharam por leitura negada de um JAR ICU4J existente no cache; houve também mensagens de dependências Java não encontradas na compilação incremental. Um build limpo confirmou a restrição de cache. A execução autorizada do mesmo wrapper fora dessa restrição compilou com sucesso, sem baixar ferramenta desconhecida ou alterar a stack.

O Forge preencheu a nova configuração SERVER com os padrões na primeira abertura. Avisos conhecidos de Realms em sessão dev e depreciações para Gradle 9 não motivaram alterações de autenticação ou versão.

A primeira suíte ampliada passou em 130/132 checks: nos dois checks restantes, o jogador caminhou para fora da plataforma curta e entrou corretamente em fallback de queda, em vez de Idle. O ataque durante Jog, dano e comparação de deslocamento já passavam. A plataforma da fixture foi estendida até Z=24; a repetição mantém os mesmos critérios e não altera o controlador ou a física para esconder a queda.

A primeira regressão M001.3 passou em 35/37 checks. Houve atraso explícito de 2443 ms/48 ticks no servidor integrado: um snapshot do servidor ainda mostrava Idle enquanto o parâmetro CPM já mostrava Sword, e a reativação antes do salto ficou atrasada. Para repetir sem mudar a suíte, as opções locais foram copiadas para backup e temporariamente reduzidas de render/simulação 12/12 para 6/5 e de 120 para 60 FPS. Não foram relaxados os checks nem alterados tempos de combate. As opções foram restauradas após fechar o cliente de teste; o hash do backup e do arquivo restaurado coincide.

## Avaliação visual e homologação pendentes

- Validar continuamente a sensação de contato da espada com o impacto de 0,20 s; o servidor mede a chamada CPM, não o primeiro frame apresentado ao cliente.
- Avaliar empunhadura, cortes de retorno e foot sliding já conhecidos. Não há crossfade novo nem clipe de recuperação.
- Balancear alcance até o centro do alvo, arco de 90°, dano bruto de 4 e intervalo de aceitação. Não há atributos/encantamentos/durabilidade de ataque customizado neste protótipo.
- Testar teclado físico, som percebido, câmera, armaduras, outras skins Classic e diferentes terrenos.
- Servidor dedicado, dois jogadores, mods adicionais e latência/TPS/FPS extremos não homologados. As regras do servidor integrado e a exclusão de jogador não substituem testes remotos.

Sem stamina, invulnerabilidade própria, deslocamento de Roll, combos, armas/inimigos novos ou mods de gameplay adicionais. Godot, autenticação e licenciamento preservados. M002.1 não iniciado.

## Encerramento

Build final: **`BUILD SUCCESSFUL in 15s`**, `.logs/m002_0-final-build.log`, usando o wrapper oficial por `scripts/gradle.ps1`. Regressão encerrada normalmente: `BUILD SUCCESSFUL in 2m 13s`. Artefato: `build/libs/towerofentropy-0.0.1.jar`.

JAR: 93.440 bytes, SHA-256 `5ff76584dbf3caeaab37df6359e0348f7d1148109472c229b4b5923d7d492397`.

Inspeção ZIP do JAR confirmou classes de combate e traduções, sem GLB/FBX, projetos/modelos CPM ou classes `com.tom` incorporados. Os links locais dos documentos foram verificados. Nenhum cliente de teste foi deixado aberto, e as opções temporárias foram restauradas. Permanecem pendentes somente as avaliações visuais/remotas descritas acima, sem início de M002.1.
