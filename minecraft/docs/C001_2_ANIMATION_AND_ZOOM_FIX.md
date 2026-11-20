# C001.2 — Animação de espada em blocos e controles de zoom

## Espada em blocos

O caminho de combate já interceptava espada contra entidades ou no ar. Quando o alvo era um bloco, `CombatInput` deixava `MultiPlayerGameMode` cuidar da quebra e do swing vanilla. A política que separa ferramentas de socos reconhecia a espada como equipamento especializado. Como o caminho do bloco ainda não selecionava nenhum clipe dedicado, o R3 deixava o vanilla desenhar o gesto. Plantas instantâneas podiam chegar ao primeiro quadro com o bloco já removido e sem mineração em andamento.

A seleção agora registra a intenção no evento cliente `LeftClickBlock`, nas fases START e CLIENT_HOLD, antes de perder o estado do bloco. O renderer seleciona o corte quando essa intenção coincide com um swing real da mão que segura a espada. O mesmo caminho funciona com instaquebra e mineração prolongada. A seleção depende do evento e da mão ativa, sem lista de IDs de vegetação. Cliques de uso continuam limpando a intenção pendente.

O R3 reutiliza `UAL_sword`, o primeiro golpe leve existente. Para blocos, o progresso vem do swing real do Minecraft e a pose é composta com a locomoção. Quando a mão ativa é a esquerda, o R3 espelha a parte superior. A ordem de prioridade mantém o Roll, os golpes confirmados pelo servidor e os estados aprovados de parkour. O R3 cancela a etapa de desenho vanilla somente após completar seu próprio desenho.

O corte de bloco é local e visual. O clique continua pela mineração vanilla, sem criar pedido de golpe, avanço físico ou avanço do combo. Contra entidades e no ar, a espada segue pelo fluxo de combate anterior. Ferramentas sem clipe dedicado, Jab/Cross elegível, posicionamento dos itens, modelos Classic/Slim e preferências de câmera conservam a integração existente.

## Zoom

A revisão encontrou a operação de aproximação correta em código, com passo negativo de 0,5 bloco e clamp entre 2 e 8. A automação da C001 já conseguia chegar a 2 blocos ao acionar diretamente o Key Binding. A versão anterior não tinha repetição controlada pelo mod e uma opção salva podia continuar atribuindo colchetes ao comando. A tecla e o estado exatos da instalação em que o problema foi percebido não foram reproduzidos. A versão desta rodada verifica em conjunto a configuração, as teclas, o valor-alvo e a posição interpolada.

- **↑** aproxima e **↓** afasta. Um toque muda 0,5 bloco.
- A pressão contínua começa com um passo, espera 8 ticks e repete a cada 4 ticks. As repetições que o sistema operacional envia enquanto a tecla está pressionada são descartadas.
- O mínimo é 2, o máximo é 8 e o padrão é 4. B continua trocando o ombro. O ponto central e a interpolação da C001.1 permanecem.
- Alt + roda para cima aproxima. Alt + roda para baixo afasta. Sem Alt, a roda segue pela hotbar.

A primeira carga do arquivo de configuração local com esta versão migra os colchetes correspondentes gravados sem modificadores. Um remapeamento diferente, inclusive um que tenha modificador, é preservado. A revisão fica em `zoomBindingVersion` no TOML para permitir que o jogador volte a mapear os colchetes posteriormente. As opções de distância, altura, offset, lado e outras teclas ficam disponíveis como antes. Cada tecla pode ser redefinida pelo menu Controles.

## Arquivos

- `ClientSwingPose.java`, `VisualContext.java`, `PlayerRendererR3.java` e `UpperBodyPose.java` conectam o swing de bloco ao corte de espada.
- `CameraInput.java` e `CameraSettings.java` registram, migram e controlam a repetição das novas teclas.
- `SwordZoomRuntimeTest.java`, `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore` isolam os cenários, os saves e as capturas de QA.

## Limites

O corte muda a apresentação do jogador local coberta pelo R3. Primeira pessoa, câmera de outro jogador e estados em que o R3 usa o renderer vanilla por equipamento ou postura mantêm a apresentação correspondente. O fallback para outras ferramentas continua sendo usado quando não existe clipe próprio. As validações visuais pelo usuário permanecem pendentes.

## Validação

As cinco suítes de lógica existentes passaram: `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1`. O build Forge final também passou. A auditoria confirmou 16 entradas de modelos e poses idênticas ao pacote anterior e zero classes de QA no JAR.

Os cenários novos exercitam grama alta e curta, papoula, cogumelo marrom, samambaia, arbusto seco, folhas de carvalho e obsidiana. Verificam que o R3 desenha o clipe sem desenho vanilla concorrente, que a quebra continua vanilla e que o contador de golpes aceitos pelo combo fica estável nos oito blocos. Folhas e obsidiana exercitam a mineração mantida. O cenário compara o dano no servidor contra alvos passivo e hostil e complementa os testes de espada no ar, Jab/Cross e ferramentas da matriz anterior.

O zoom verifica o TOML, as teclas efetivamente carregadas, a distância renderizada em espaço livre, a interpolação nos dois sentidos, os limites, a troca de ombro e o controle mantido com repetições adicionais simuladas. O jogo continua limitando a distância efetiva se houver um obstáculo atrás do personagem. Os dois sentidos de Alt + roda foram testados pelo mesmo handler usado em jogo, com o estado de Alt injetado pelo teste. A roda normal foi verificada chamando a entrada de rolagem de `MouseHandler` e observando a troca de slot. A tentativa de rolagem física automatizada não chegou ao cliente nesta janela de QA.

A primeira execução encontrou quadros de fallback durante mineração mantida. A seleção passou a verificar o estado ativo de mineração da mão principal para manter o corte durante essas janelas. O cenário inicial de zumbi também ficou inconclusivo e foi preparado novamente com alcance e área de mira livres. Os registros e capturas das tentativas anteriores permanecem separados.

Os perfis gravam em `run-c001-2-qa/` e `animation_pipeline/converted/c001-2-qa/`. Os relatórios anteriores continuam nos diretórios históricos.

```powershell
.\scripts\gradle.ps1 build --offline --console=plain
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeC0012Qa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeC0012Qa -PtoeC0012Only -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeGlobalSlim -PtoeC0012Qa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeC0012Qa -PtoeC0012Restart -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeC0012Qa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeCombatTest -PtoeC0012LegacyQa -x downloadAssets
```

- JAR: `build/libs/towerofentropy-0.0.1.jar`
- SHA-256: `383F9A42BCFB897A31D9813AC626BAD0526608672F93A6940EC7D009D1DD822B`

### Resultados atuais

O Slim passou em **35/35** no cenário novo e **32/32** na política geral de ferramentas. Nos dois alvos do cenário novo, o servidor registrou exatamente um ataque aceito, um impacto e um acerto. O zumbi ficou com 16,064 pontos de vida e a vaca com 6. A verificação de persistência passou em **2/2 por perfil**, com distância física de 5,5 após reiniciar o processo. O Classic migrou os dois padrões antigos. O Slim migrou a aproximação e preservou o K personalizado para afastar.

A regressão atual do R3 passou em **18/18**. O teste manteve arco, Roll e consumo e observou a flecha acertar o alvo e parar diante da cobertura. A repetição de movimento usa o perfil novo `-PtoeExpansionTest -PtoeC0012Qa -PtoeC0012Movement`, sem alterar o limite das asserções existentes. O perfil também mede separadamente a diferença de yaw armazenada pelo controlador entre dois ticks. O contador de ângulo existente usa o yaw já interpolado de quadros de renderização, portanto seu intervalo pode variar em relação ao tick do jogador.

### Comparação com a suíte histórica de combate

A suíte histórica M002.0 registrou **103/132**, o mesmo total da C001.1. Das 29 divergências, 28 tinham o mesmo nome nos dois registros. Permanecem as expectativas antigas sobre modo desligado, gesto CPM, espada alternativa, ausência de avanço físico e ausência de fila de golpes. As asserções históricas e o combate em produção ficaram intactos.

O cenário `off_before_impact_no_player_displacement` passou nesta rodada e `animation_off_no_player_displacement` registrou uma divergência nova de 0,10 bloco. Na C001.1 ocorria o inverso, com os mesmos 0,10 bloco. Os dois cenários passaram nas asserções de cancelamento e vida do alvo em ambas as versões, com zero impactos. Os comandos de teste chegam ao servidor depois de solicitar o ataque e podem cair antes ou depois da primeira aplicação de avanço. A comparação indica sensibilidade ao momento do cancelamento. O resultado isolado de deslocamento fica registrado como investigação, sem regressão de dano confirmada.

- Suíte atual de regressão R3: `.logs/c0012-regression.log`.
- Suite histórica: `.logs/c0012-legacy-combat.log`.
- Referência anterior: `.logs/c0011-legacy-combat.log`.

### Repetição de movimento

A primeira rodada Classic registrou sete direções no salto. A rodada Slim registrou oito direções e mediu 36,4° entre amostras de yaw renderizado na reversão aérea. A repetição isolada passou em **30/30**, com **8/8** direções, **35,7°** no indicador renderizado e **35,0°** no máximo do controlador em 14 amostras aéreas. As asserções e o controlador histórico ficaram intactos. A repetição e a medição por tick distinguem a variação temporal das amostras de uma alteração no limite real de yaw. O resultado fica em `.logs/c0012-movement-retest.log`.

O Classic limpo também passou em **35/35**. Os oito cenários de bloco mantiveram o contador de combo intacto e absorveram o desenho vanilla. A vaca e o zumbi receberam um único impacto cada, consistente com o resultado Slim. A execução reutilizada anterior deixou um mob na área de mira e contabilizou golpes nele em alguns cenários de plantas. A rodada Classic final removeu os mobs residuais na preparação antes de cada caso e preservou as capturas das execuções anteriores em suas pastas separadas. O resultado final está em `.logs/c0012-classic-clean-fixture.log`. As capturas anteriores à C001.2 foram preservadas.
