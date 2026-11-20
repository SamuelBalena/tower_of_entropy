# C001 — Câmera em terceira pessoa

A perspectiva traseira do F5 usa a câmera oficial sobre o ombro para o jogador local. O ciclo de F5 continua sendo controlado pelo Minecraft e mantém a primeira pessoa e a terceira pessoa frontal. Espectador, entidades de câmera de outros jogadores e pose de dormir continuam com o enquadramento vanilla.

## Arquitetura e hooks

Em Forge 1.20.1, `GameRenderer.renderLevel` resolve o alvo de interação com `pick` antes de chamar `Camera.setup`. A câmera vanilla primeiro calcula a posição interpolada dos olhos e a rotação de visão. Só depois aplica o deslocamento traseiro de quatro blocos e o recorte para blocos. O evento `ViewportEvent.ComputeCameraAngles` ocorre tarde e controla os ângulos da vista. A posição da câmera e do bloco que ela ocupa é atualizada pelo próprio `Camera`.

O coremod registrado em `META-INF/coremods.json` atua no ramo da terceira pessoa de `Camera.setup`. Ele usa os nomes mapeados pelo Forge e exige encontrar um único deslocamento vanilla. Quando a perspectiva traseira está ativa, `ShoulderCamera` calcula a posição a partir do ponto interpolado, aplica o deslocamento nos eixos locais e atualiza a câmera pelos próprios setters, através de `CameraPoseAccess`. As outras perspectivas percorrem o código vanilla. O ciclo de perspectivas, a rotação do jogador e seus pacotes permanecem na lógica do jogo.

Distância, offset vertical e troca de ombro usam suavização exponencial pelo tempo real entre quadros, com constante de tempo de 85 ms. A posição interpolada do jogador e os vetores de visão são amostrados a cada quadro. A rotação do mouse entra imediatamente no cálculo, inclusive ao inverter a direção. A câmera converge para o alvo conforme a geometria do enquadramento. A proteção de colisão básica testa oito raios de canto ao longo do deslocamento completo e limita o movimento quando encontra um bloco.

O Polymesh R3 continua desenhando Steve Classic e Slim em terceira pessoa pelos eventos que já utilizava. O ponto de vista é alterado antes de renderizar o mundo, sem transformar a geometria, as poses ou a seleção dos clipes do personagem.

## Controles e arquivo local

| Controle | Ação |
| --- | --- |
| F5 | Percorre as perspectivas vanilla. A traseira abre o modo sobre o ombro. |
| B | Troca de ombro. V continua reservado ao Roll. |
| [ / ] | Aproxima / afasta em passos de 0,5 bloco. |
| Alt + roda | Ajusta a distância com a câmera traseira ativa. A roda sem Alt seleciona a hotbar normalmente. |

B, [ e ] são remapeáveis em Controles, na categoria Tower of Entropy. Preferências ficam em `config/towerofentropy-camera-client.toml`, uma configuração Forge CLIENT. Controles visuais não exigem pacote ou configuração no servidor.

| Opção | Padrão | Faixa |
| --- | ---: | --- |
| `distance` | 4,0 blocos | 2,0 a 8,0 |
| `shoulderOffset` | 0,85 bloco | 0,0 a 1,5 |
| `heightOffset` | 0,25 bloco | −0,5 a 1,0 |
| `rightShoulder` | `true` | ombro direito ou esquerdo |

## Mira e autoridade

O PNG original de 13 × 13 usa marfim, ferro escuro e transparência. O evento `RenderGuiOverlayEvent.Pre` substitui a camada da mira apenas no modo sobre o ombro. A origem é calculada a partir do centro da GUI escalada e a mira respeita a opção de esconder a interface. As outras perspectivas mantêm suas próprias regras para a mira.

A câmera aponta para o alvo que o Minecraft selecionou pelos olhos do jogador. Quando não há alvo dentro do alcance de interação, um raio visual sai desses mesmos olhos e termina no primeiro bloco ou entidade alcançável pelo raio, até 96 blocos. Essa referência movimenta apenas a vista. A seleção de alvos, a autoridade do servidor, o alcance, o arco da espada e o lançamento de flechas preservam a lógica existente. A origem das flechas continua no personagem, com gravidade e dispersão vanilla. O ponto central representa a direção de mira. A trajetória ainda cai com a distância como em primeira pessoa.

## Arquivos

- `ShoulderCamera.java` calcula o ponto de vista e a convergência.
- `CameraSettings.java`, `CameraInput.java` e `TowerOfEntropy.java` registram configuração, atalhos e persistência local.
- `CameraPoseAccess.java`, `shoulder_camera.js` e `coremods.json` integram a câmera a `Camera.setup`.
- `CameraCrosshair.java` e `textures/gui/shoulder_crosshair.png` desenham a mira. `lang/en_us.json` e `lang/pt_br.json` traduzem os atalhos.
- `CameraRuntimeTest.java`, `ExpansionRuntimeTest.java`, `A3RuntimeTest.java`, `build.gradle` e `.gitignore` isolam os cenários, os saves e as capturas de QA.

## Limites e etapas seguintes

O recorte da C001 é básico. Perto de paredes, o mínimo configurado pode encolher até o ponto de vista caber, e um obstáculo pode encobrir a referência de mira visível pelo personagem. Oclusão em corredores, correção avançada de clipping, transparência e primeira pessoa emergencial ficam para a C002. A aplicação centralizada da pose permite acrescentar esses ajustes sem misturá-los aos atalhos ou à mira. A C003 pode introduzir uma camada separada de tremor antes da aplicação da pose. A avaliação visual final fica com o usuário.

## Validação

O perfil Classic passou em 56/56 verificações. Foram 16 da câmera, 11 de socos e 29 de movimentação, combate e parkour. A rotina percorreu o F5, mediu as duas posições laterais e a interpolação, verificou limites de 2 e 8 blocos e leu a configuração persistida em disco. A mira foi verificada em pixels no centro do framebuffer, em 854 × 480 e 1031 × 641. O caso de parede conferiu o alinhamento da câmera com o alvo vanilla e mediu o recorte geométrico.

As cinco suítes existentes passaram: `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1`.

Os cenários Forge usam cópias do mundo `Global Survival one` em `run-c001-camera-qa/{classic,slim,regression}`. As capturas ficam em `animation_pipeline/converted/c001-camera-qa/`. Os perfis anteriores gravam em seus próprios diretórios.

A partir de `minecraft`, com JDK 17, o mundo copiado para cada perfil e os caches locais do Forge:

```powershell
.\scripts\gradle.ps1 build --offline --console=plain
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeC001Qa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeGlobalSlim -PtoeC001Qa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeC001Qa -x downloadAssets
```

Classic e Slim passaram em 56/56 cada, sem erros do R3. As capturas `right-default.png` e `left-resized.png` registram os dois lados. A inspeção da roda confirmou que o evento do Forge ocorre antes da troca de slot. O uso físico de Alt + roda fica para conferência manual.

O teste de arco encontrou a flecha do jogador em Z ≈ 8,95 no alvo em linha livre e em Z ≈ 4,95 na face da parede. O servidor registrou o consumo de uma flecha nos dois casos. Na primeira configuração do cenário de cobertura, com a parede a 1,5 bloco, a flecha deixou de existir até a leitura tardia e o inventário terminou com 64 flechas. O cenário foi repetido com a parede mais distante e a captura inicial foi preservada em `regression-initial`.

O build final passou em `.logs/c001-build-final.log`. A auditoria do pacote conferiu o coremod, a mira e a ausência de classes de QA. Os GLBs de produção Classic e Slim correspondem byte a byte aos modelos anteriores.

- JAR: `build/libs/towerofentropy-0.0.1.jar`
- SHA-256: `2E34BB683D9ACCAF2F6F280DA20976799936728BB61CBE728AA77F74E99F9820`

A regressão final passou em 18/18. Incluiu espada, Roll, arco, consumo, tocha, agachamento, salto e reinicialização do R3. Os registros completos estão em `.logs/c001-classic.log`, `.logs/c001-slim.log` e `.logs/c001-regression-final.log`.
