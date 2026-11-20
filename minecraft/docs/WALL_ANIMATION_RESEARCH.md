# Investigação de animações de parede e correção visual do Wall Jump

19 de novembro de 2026

## Correção aplicada

O servidor já trocava de `SLIDE` para `JUMP` ao autorizar o Wall Jump. O cliente convertia `JUMP` em `UAL_wall_jump`. O clipe começa na pose de escorregar e entrava na seleção antes do salto normal. Além disso, o relógio do salto normal só era renovado pela saída do chão, evento que não ocorre no impulso partindo de uma parede.

Agora a confirmação do servidor encerra o estado visual de parede na mesma atualização e reinicia o relógio do salto. O R3 seleciona `UAL_jump_start` na saída, `UAL_jump_loop` no ar e `UAL_jump_land` no contato com o chão, inclusive se ainda houver deslocamento horizontal após o Wall Jump para longe. A trajetória visual continua após o servidor limpar a fase curta de impulso. Um novo contato autorizado pode selecionar `UAL_wall_slide`, `UAL_ledge_hang` ou `UAL_ledge_climb`. O blend de parede para saída foi reduzido a 70 ms para desfazer a pose de contato sem corte brusco.

A mudança é visual. Os clipes aprovados, o rig, o Polymesh R3, as colisões, as alturas e as regras de vigor foram preservados. `UAL_wall_jump` permanece empacotado como referência e saiu da seleção automática.

## Fontes e metodologia

- **UAL1 e UAL2.** Foram inspecionados os GLBs reais `Unreal-Godot/UAL1_Standard.glb`, `UAL1_Standard_RM.glb`, `UAL2_Standard.glb` e `UAL2_Standard_RM.glb`. Cada pacote também entrega arquivos FBX homônimos sob `Unity/`. A auditoria de movimento usou o GLB, com 31 amostras por clipe, e leu os dois `License.txt` CC0 1.0. O esqueleto das animações tem 65 juntas no skin. O R3 usa 12, portanto novos candidatos exigem retarget, conferência de contato e validação de empacotamento.
- **Free100Animations.** Executei o Unreal Editor 5.8 em uma cópia do projeto e li as sequências nativas e as curvas amostradas de raiz, pelve, mãos e pés. São `.uasset` com `SK_Mannequin`. Uma eventual integração exige confirmar a licença aplicável, exportar pelo Unreal e fazer retarget para o R3. O pacote local não trouxe uma licença identificável. [Metadados com caminhos exatos](../animation_pipeline/reports/wall-research/FREE100_SEQ_METADADOS.txt)
- A busca incluiu Wall, Cling, Ledge, Hang, Slide, Climb, Mantle, Vault e Pull, além de movimentos de apoio e subida com outros nomes. Nos trechos abaixo, amplitude é máximo menos mínimo na trajetória amostrada. Ela indica comportamento e ajuda a descartar falsos positivos, mas não prova contato das mãos com um bloco Minecraft.

## UAL1 e UAL2

As colunas de origem indicam o arquivo e o nome interno do clipe. Os candidatos ainda não retargetados podem ser usados como fonte no Minecraft após adaptação, sem transmitir root motion do asset à física.

| Origem e clipe | Duração | Movimento verificado e comparação com o R3 | Uso |
| --- | ---: | --- | --- |
| UAL1 · `UAL1_Standard.glb` / `UAL1_Standard_RM.glb` · `Push_Loop` | 2,667 s | Empurrar. Mãos praticamente imóveis em altura entre 1,163 e 1,165 m no GLB. Os pés alternam passos. A raiz RM avança 0,8 m no eixo Z. Serve de referência para as mãos de `UAL_wall_slide`, que tem pernas flexionadas e ritmo de suspensão. | CC0, pede retarget da parte superior. Manter as pernas atuais e conferir a posição de ambas as mãos. [Prancha frontal e lateral](../animation_pipeline/reports/wall-research/UAL1_Push_Loop.svg) |
| UAL1 · mesmos GLBs · `Fixing_Kneeling` | 5,200 s | Ajoelhar e manipular. Pelve a 0,398–0,892 m e mãos frequentemente baixas. O movimento depende do chão e difere do corpo suspenso de `UAL_ledge_hang`. | CC0. Descartado para parede e borda. |
| UAL1 · mesmos GLBs · `Jump_Start`, `Jump_Loop`, `Jump_Land` | 1,333 / 2,500 / 1,267 s | Saída, fase aérea e aterrissagem. Sem translação da raiz nas versões amostradas. As variantes R3 aprovadas já cobrem o movimento sem introduzir apoio de parede no ar. | CC0. Disponíveis imediatamente e reutilizadas nesta correção. |
| UAL2 · `UAL2_Standard.glb` / `UAL2_Standard_RM.glb` · `ClimbUp_1m` | 0,667 s | Subida curta. A variante RM move a raiz 1,000 m para cima e 1,676 m para frente. A variante in-place mantém a raiz fixa. A sequência oferece referência para flexão, elevação e passo final. O atual `UAL_ledge_climb` dura 0,900 s e possui fases alinhadas ao percurso do servidor. | CC0. Principal candidata a protótipo após retarget. Ajustar a transição a partir da suspensão e verificar contato das mãos. [Prancha frontal e lateral](../animation_pipeline/reports/wall-research/UAL2_ClimbUp_1m.svg) |
| UAL2 · mesmos GLBs · `Slide_Start`, `Slide_Loop`, `Slide_Exit` | 0,833 / 2,000 / 0,500 s | Deslizamento no chão. No loop, pelve entre 0,044 e 0,059 m e uma mão a até 0,025 m do chão. O RM avança 9 m no loop. A postura não sustenta as mãos na parede. | CC0. Descartados para deslizamento vertical. |
| UAL2 · mesmos GLBs · `NinjaJump_Start`, `NinjaJump_Idle_Loop`, `NinjaJump_Land` | 0,967 / 2,000 / 1,267 s | Outra família de salto, com raiz estacionária. O loop tem excursão vertical das mãos abaixo de 0,055 m e não confirma apoio. Os clipes UAL1 já retargetados cobrem as mesmas fases. | CC0. Exigiriam retarget. Sem motivo técnico para mudar o ciclo aprovado nesta tarefa. |

## Free100Animations

Caminhos relativos a `Free100Animations/Content/`. Em todas as linhas, o arquivo tem o mesmo nome do clipe seguido pela extensão `.uasset`. As sequências estão bloqueadas para uso no mod enquanto a licença seguir sem confirmação. Mesmo com licença, precisam de exportação e retarget do `SK_Mannequin`.

| Pasta e arquivo/clipe | Duração | Movimento verificado e comparação |
| --- | ---: | --- |
| `ThirdPerson/Anims/100/CityAnims/AS_LeaningOnWall.uasset` | 10,400 s | Encostar. Pelve varia 0,8 cm na vertical e as mãos têm pouca excursão. A curva não representa descida nem sustentação por dois braços, ao contrário das poses atuais de parede e borda. |
| `Characters/Mannequins/Anims/Unarmed/Jump/MM_WallJump.uasset` | 0,467 s | Gesto curto de salto. Uma mão tem excursão lateral de 92,5 cm e a outra de 22,5 cm. Pode ser referência de impulso assimétrico, sem evidência de suspensão ou contato sustentado. O salto aprovado do R3 foi mantido. |
| `Characters/Mannequins/Anims/Unarmed/Jump/MM_Fall_Loop.uasset` | 3,000 s | Queda com raiz fixa, mãos assimétricas e excursão dos pés. Referência de queda livre. Não oferece a transição de duas mãos de `UAL_ledge_hang`. |
| `ThirdPerson/Anims/100/WorldInteractionAnims/AS_PullObject.uasset` | 4,100 s | Tração com ambas as mãos, cerca de 1 m de excursão em Y com raiz fixa e apenas 11,3 cm de excursão vertical da pelve. Útil como referência de atuação dos braços, sem a elevação do corpo de uma subida. |
| `ThirdPerson/Anims/100/WorldInteractionAnims/AS_PullLever.uasset` | 4,633 s | Tração assimétrica com pelve com pouca excursão vertical e raiz fixa. As curvas não mostram o corpo subindo sobre uma superfície. |
| `ThirdPerson/Anims/100/RPG-NPCAnims/AS_FishermanPullingLine.uasset` | 4,233 s | Tração repetida. Excursão vertical da pelve de 2,1 cm com raiz fixa, inadequada para a elevação de `UAL_ledge_climb`. |
| `Characters/Mannequins/Anims/Pistol/Jump/MM_Pistol_Jump_Fall_Loop.uasset`, `Characters/Mannequins/Anims/Rifle/Jump/MM_Rifle_Jump_Fall_Loop.uasset`, `Characters/Mannequins/Anims/Rifle/Jump/MM_Rifle_Jump_Fall_Land.uasset` | 2,000 / 3,000 / 0,400 s | Queda e aterrissagem com pose de arma, com translação de raiz nas duas sequências de rifle. Não servem para apoio das mãos. |

## Recomendações

1. Testar `Push_Loop` como referência isolada das mãos e dos cotovelos durante a descida. Manter `UAL_wall_slide` como base para as pernas. Julgar pelo erro de posição das duas mãos em relação à face do bloco em Classic e Slim.
2. Retargetar `ClimbUp_1m` em um derivado de comparação. Testar a variante in-place no mesmo percurso de 18 ticks, alinhando o primeiro apoio às mãos e o fim aos pés sobre a superfície. A prancha mostra o esqueleto da fonte, sem simular o encaixe real na borda.
3. Manter `UAL_ledge_hang` como referência de suspensão. Nenhuma das três fontes examinadas confirmou um ciclo de suspensão de duas mãos pronto para o R3. Para melhorar a entrada, comparar a distância de cada mão à borda durante o blend entre `jump_loop` e `ledge_hang`.

O ciclo de salto aprovado é o único conjunto imediatamente utilizável no R3. `Push_Loop` e `ClimbUp_1m` merecem comparação após adaptação. As sequências do Free100Animations seguem fora do pacote.

## Arquivos alterados

- `ClientWallVisual.java`, `VisualContext.java` e `R3VisualPose.java` corrigem a seleção e o blend.
- `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore` adicionam as verificações e isolam os perfis Classic e Slim em `run-wall-research-qa`.
- Este relatório, a prancha SVG e os metadados do Unreal ficam separados dos recursos de jogo.

## Validação

- Build Forge normal aprovado. O JAR contém as duas variantes parkour e preserva os 17 recursos da base comparada. Nenhuma sequência do Free100Animations, prévia ou classe de QA entrou no JAR.
- Integração Classic **20/20**, sem erros de renderização. O teste observou os três clipes normais nos saltos de chão, vertical e para longe. A reentrada na parede foi testada separadamente da aterrissagem vertical. Confirmou `Wall Slide → Jump → Wall Slide`, `Jump → Ledge Hang`, `Ledge Hang → Ledge Climb` e Sprint nas oito direções.
- Regressão Slim **13/13**, sem erros de renderização. O relatório registrou espada, Roll, soltura do arco com projétil real, salto e reconexão do R3.
- As cinco suítes existentes passaram. Movimento horizontal (43), combate (18), Roll (104), locomoção e janelas de ação.
- Os ápices permaneceram em 2,5019 blocos no salto normal e 2,4369 blocos medidos desde a solicitação de ambos os Wall Jumps. O teste de aderência continuou observando 35 ticks de vigor.
- A validação usou servidor integrado Forge 47.4.28. A revisão estética do blend no jogo permanece aberta. As novas capturas Slim estão em `animation_pipeline/converted/wall-research-qa/runtime`. Nenhuma captura histórica foi sobrescrita e nenhum commit foi criado.

SHA-256 do JAR `0047618CA144DB5F81EE8BCC9D3C3B576A2AA13B0E6EEEE1821D5846DA9C59C3`.
