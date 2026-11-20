# Protótipos comparativos de animações de parede

19 de novembro de 2026

## Comparação no Minecraft

O Polymesh R3 abre em **ORIGINAL**. Os três comandos são locais ao cliente, mostram confirmação no chat e valem até desconectar. Use F5 com uma skin Classic ou Slim nas condições usuais do R3.

| Comando | Aderência e deslizamento | Subida |
| --- | --- | --- |
| `/toe animprofile original` | `UAL_wall_slide` | `UAL_ledge_climb` |
| `/toe animprofile experimental` | `UAL_wall_slide_push` | `UAL_ledge_climb_ual2` |
| `/toe animprofile misto` | `UAL_wall_slide` | `UAL_ledge_climb_ual2` |

`UAL_ledge_hang` e o ciclo `UAL_jump_start` → `UAL_jump_loop` → `UAL_jump_land` continuam em todos os perfis. O cliente troca o clipe no mesmo modelo e usa o blend existente. Alternar durante uma subida mantém o tempo decorrido da fase autorizada pelo servidor. Os clipes novos só aparecem nos estados de deslizamento e subida correspondentes. A troca não envia pedido de gameplay e a mudança de clipe não altera velocidade, colisão, vigor ou checagem de espaço.

## Origem e adaptação

| Fonte analisada | Duração de origem | Clipe derivado |
| --- | ---: | --- |
| UAL1 · `Unreal-Godot/UAL1_Standard.glb` · `Push_Loop` | 2,667 s | `UAL_wall_slide_push`, 2,667 s |
| UAL2 · `Unreal-Godot/UAL2_Standard.glb` · `ClimbUp_1m` | 0,667 s | `UAL_ledge_climb_ual2`, 0,900 s |

Os dois GLBs originais são as variantes in-place com 65 juntas, distribuídas com `License.txt` CC0 1.0 nos respectivos pacotes. O sampler existente verificou os nomes exatos, as durações, as juntas usadas no mapeamento e a ausência de translação animada da raiz. Os derivados usam as 12 juntas do Steve.

- **Push_Loop.** As rotações amostradas do tronco, dos braços e dos cotovelos são transferidas sobre a base de parede. O ritmo das pernas vem do clipe de deslizamento atual. O retarget limita abertura dos braços e alcance sagital e fecha a junção do loop. Na medição Blender do Classic sem a camada externa, a ponta avançada de cada braço chegou a cerca de 0,315–0,316 bloco do centro, a aproximadamente 0,005 da parede nominal a 0,320 bloco.
- **ClimbUp_1m.** A flexão dos braços e a sequência de pernas da fonte são combinadas com a orientação e os extremos do clipe R3. O primeiro segmento da amostra acompanha os 11 ticks de elevação física e o restante acompanha os sete ticks sobre a borda. O primeiro e o último quadros preservam suspensão e final em pé. Nenhuma translação do asset move o jogador.
- **Folga na borda.** O Blender avaliou cabeça, tronco e braços deformados, com as camadas externas, em cada quadro da subida. Em relação ao envelope do clipe original, a contribuição amostrada foi mantida em 21 de 28 quadros, reduzida em um e suprimida em seis. O teste usa uma tolerância de 0,002 bloco no envelope avaliado. A amostra nominal sem camadas externas manteve cabeça e tronco livres da parede abaixo do topo. O cruzamento máximo dos braços, cerca de 0,067 bloco, já ocorria na subida original.

O pacote acrescenta dois clipes a cada variante e verifica malhas, nós, materiais, texturas, skinning, índices e bytes dos 38 clipes anteriores. Os modelos derivados Classic e Slim somam 40 clipes cada. A cópia Blender de comparação está em `animation_pipeline/converted/wall-prototypes/steve-a3-motion-review.blend`. O manifesto de origem e a auditoria de folga estão na mesma pasta.

## Arquivos alterados

- Pipeline em `prepare-wall-prototypes.cjs`, `blender-a3.py`, `package-expansion.cjs` e `build.gradle`.
- Seleção em `ClientWallVisual.java`, `VisualContext.java`, `PlayerRendererR3.java` e `R3VisualPose.java`.
- Comandos locais em `RendererCommands.java`.
- QA em `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore`. Perfis de teste em `run-wall-prototype-qa`, com saves copiados e saídas separadas.

## Reprodução do QA

Use cópias limpas de um save de QA chamado `Global Survival one` em `run-wall-prototype-qa/classic/saves`, `run-wall-prototype-qa/slim/saves` e `run-wall-prototype-qa/regression/saves`, conforme o teste escolhido. Execute na pasta `minecraft`.

- Comparação Classic com `./scripts/gradle.ps1 runClient --offline -PtoeExpansionTest -PtoeWallPrototypeQa`.
- Comparação Slim com os mesmos argumentos e `-PtoeGlobalSlim`.
- Regressão Slim com `./scripts/gradle.ps1 runClient --offline -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeWallPrototypeQa`.

## Limitações e revisão visual

A checagem de folga usa um plano nominal e quadros amostrados. Distância real à parede, camadas externas da skin, interpolação e trocas de perfil podem mudar o contato aparente. Os clipes usam restrições no bake, com retarget parcial. Eles não fazem IK contínuo das mãos no bloco. O perfil aparece no corpo local em terceira pessoa do R3. Os fallbacks de primeira pessoa, jogadores remotos e equipamento seguem o comportamento existente.

Compare no mesmo trecho com os três comandos. Observe as duas mãos durante a descida, a cabeça e os braços na passagem pela borda e a pose final dos pés. Em seguida, faça um Wall Jump para conferir a saída da pose de parede. As animações continuam experimentais e aguardam avaliação visual do usuário.

## Resultados

- Build Forge final aprovado. Os 20 recursos anteriores conferem byte a byte com o JAR de referência. O JAR inclui os dois derivados e exclui as classes de QA.
- Integração Classic **23/23** e Slim **23/23**, com zero erros de renderização. Os testes executaram os comandos, confirmaram a seleção em aderência, suspensão e subida, e verificaram que alternar de MISTO para ORIGINAL durante a subida preserva a fase. Confirmaram o ciclo normal nos Wall Jumps e Sprint nas oito direções, socos no vazio e contra entidades e mineração contínua.
- Regressão Slim **13/13**, com espada, Roll, arco, salto e reconexão. As cinco suítes existentes também passaram, incluindo 43 casos de movimento horizontal, 18 de combate e 104 de Roll.
- Capturas novas em `animation_pipeline/converted/wall-prototype-qa`. As pranchas laterais Classic incluem [Push adaptado](../animation_pipeline/converted/wall-prototype-qa/classic/screenshots/experimental-slide.png) e [subida adaptada](../animation_pipeline/converted/wall-prototype-qa/classic/screenshots/experimental-climb.png). As capturas históricas permaneceram intactas. Nenhum commit foi criado.

SHA-256 do JAR `F29877D0332F9E7C830EC9FA3631BA980BA494E01BCFEEF32292A1F5A8DFD098`.
