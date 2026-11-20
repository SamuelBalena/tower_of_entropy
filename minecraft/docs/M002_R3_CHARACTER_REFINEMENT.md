# M002.R3 — Refinamento do Steve deformável

Data: 12/11/2026. Ambiente: Windows, Minecraft 1.20.1, Forge 47.4.28, JDK Microsoft 17.0.20.1, Gradle 8.8, ForgeGradle 6.0.54, CPM 0.6.26a e Polymesh 1.0.0. Blender 5.2 já instalado. Nenhuma biblioteca nova foi instalada nesta etapa.

## Resultado e escopo

O perfil experimental `-PtoeR3` acrescenta flexão ponderada à cintura do Steve existente. O mod normal continua usando CPM. O laboratório opera em `run-r3/`, com cópias dos mundos e modelos; não substitui R1, R2, o mundo normal ou os arquivos Godot. A aprovação estética continua dependendo do usuário.

Validação final: R3 30/30, referência R2 29/29, perfil CPM 28/28, estresse 4/4, combate 132/132 e sincronização visual 18/18; reinício e reconexão passaram. Build experimental passou em 13 s e normal limpo em 20 s. Foram arquivadas 76 capturas. Não ficou cliente de teste aberto.

Consulte [testes executados](TEST_LOG_M002_R3.md) e [comparador de capturas nativas](../experiments/m002_r3/COMPARE.html). As imagens são do Minecraft, não renders Blender ou imagens geradas. Fontes e derivados de terceiros permanecem locais e ignorados pelo Git; não publicar o JAR experimental com esses movimentos.

## Diagnóstico e alterações

O R2 reutilizava o GLB R1, com dez ossos e torso inteiramente rígido no osso `1`. Cotovelos e joelhos já tinham pesos reais. Alterar apenas a rotação do torso não produziria uma curva contínua. Não havia necessidade de substituir o renderer ou criar um personagem novo.

O script abre uma cópia do `.blend` aprovado, preserva doze objetos, dimensões, materiais e ilhas UV, e executa:

- Adição de `pelvis` e `waist`; `1` continua como peito. Hierarquia: pelve → cintura → peito → cabeça/ombros; coxas seguem a pelve. Antebraços e canelas preservam seus pais.
- Cinco cortes nas arestas verticais de `body` e `body_outer`: cada objeto passa de 8 para 28 vértices de modelagem, com 26 faces e nenhuma borda aberta. Não há cortes novos em cabeça, braços ou joelhos.
- Pesos lineares peito/cintura na metade superior e cintura/pelve na inferior. Os anéis superiores das pernas passam a compartilhar influência da pelve com a base do torso, reduzindo a separação da cintura.
- Recalculo de normais do torso; preservação da transformação inversa transposta do renderer R2, inclusive no espelhamento e attachment. Skinning linear igual ao Polymesh, sem depender de dual quaternion exclusivo do Blender.

No GLB exportado são 816 vértices ponderados contra 736 no R2, e 128 vértices com influências misturadas contra 64. O validador encontra 64 vértices misturados nas duas malhas do torso; a auditoria Java mede 32 em `body` (exclui `body_outer`). Pesos somam 1; matrizes inversas de bind são verificadas por produto com a matriz global de repouso. A auditoria do modelo realmente carregado no Minecraft confirma deslocamento do torso em relação à hipótese de peito rígido.

### Movimentos preservados

As rotações adicionais vêm dos canais originais `pelvis` e `spine_02` das mesmas UAL. As diferenças pelve/peito completas chegam a aproximadamente 62–64 graus no Jog/Sword/Roll; transferi-las integralmente para um torso cúbico curto exageraria a deformação. Aplica-se slerp relativo ao peito, com intensidades registradas:

| Movimento original | Duração exportada | Retenção relativa | Máximo pelve/peito R3 |
|---|---:|---:|---:|
| Idle_Loop | 2,500 s | 0,15 | 3,491° |
| Jog_Fwd_Loop | 0,933333 s | 0,35 | 21,575° |
| Roll | 1,466667 s | 0,55 | 35,197° |
| Sword_Regular_A | 0,433333 s | 0,35 | 21,572° |

Quaternions permitem inclinação frontal, lateral e torção; não se inventaram clipes substitutos. As orientações globais dos dez ossos anteriores, incluindo cabeça e extremidades, são preservadas nos tempos das amostras originais, com erro máximo matricial inferior a 0,000001. As posições foram adaptadas por cinemática direta para ligar pelve, cintura e peito: cabeça e ombros acompanham o pescoço, sem alterar sua orientação global. Portanto, preservação de orientação não significa trajetórias de posição idênticas ao torso rígido. A interpolação hierárquica entre amostras também pode variar ligeiramente. O lift visual existente no Roll permanece; nenhuma física ou colisão foi alterada.

### UV, skin e empunhadura

As ilhas originais Classic 64×64 são interpoladas nos novos cortes, sem repintar ou substituir a textura. Cabeça, camadas externas, braços e pernas conservam suas UVs; torso usa as mesmas regiões. A textura aplicada no jogo é a skin atual do jogador Classic; a fixture visual fixa Steve apenas no cache local de PlayerInfo, sem alterar GameProfile, autenticação ou arquivos de skin. Slim usa fallback.

A espada vanilla mantém o attachment R2 ao osso `103`, com o anchor original `303`. O braço segue a posição refinada do ombro; não há uma segunda espada vanilla, pois o evento só é cancelado após desenhar corpo e item com sucesso. As capturas de Sword em três tempos ajudam a conferir a empunhadura.

Armadura em qualquer slot, item na mão secundária ou capa habilitada fazem o R3 liberar o evento ao CPM/vanilla, com aviso na actionbar. O equipamento não some silenciosamente. Ao remover o equipamento, o R3 retoma. Primeira pessoa, Slim, poses incompatíveis e jogador remoto continuam no caminho aprovado. A capa foi testada com uma entrada temporária no cache local de textura; isso valida o fallback, não a deformação de uma capa real. Não foi criado sistema de armaduras.

## Comparação visual e limites

O comparador apresenta CPM, o GLB R2 intacto e o GLB R3 com câmera, iluminação e tempos iguais: Idle 25/30 s, Jog 7/30 s, Sword 6/30 s e Roll 22/30 s. Quatro orientações (0/90/180/270 graus) mostram frente, costas e ambos os lados. Sword também tem amostras 2/30 e 10/30 s para os perfis ponderados. O Roll mostra a flexão acentuada dos membros, sem alegar cobertura visual de cada frame. Uma execução R3 de estresse captura ainda os máximos do ângulo pelve/peito de origem: Jog 10/30 s, Sword 5/30 s e Roll 14/30 s, com duas laterais no Roll.

A inspeção nativa mostra curva gradual na camiseta/cintura no Jog e Sword, enquanto o R2 mantém o torso como bloco rígido. Idle permanece próximo do anterior. A cintura continua ligada nas poses registradas; não foi observado um novo buraco grande ou deslocamento de ilha UV. Braços e joelhos continuam dobrando. A espada aparece uma vez e acompanha a mão nos tempos registrados. Textura e silhueta continuam reconhecíveis como Steve.

Persistem achatamento de volume pelo LBS em flexões fortes, interpenetração de partes no Roll e variação de densidade dos pixels quando a superfície curva. O Roll herdado continua com pose muito baixa, podendo aproximar-se do chão; não há root motion ou adequação da caixa de colisão. O refinamento não corrige universalmente todas as poses nem promete qualidade idêntica ao Epic Fight. As camadas externas mantêm o pequeno afastamento herdado. Não há crossfade/foot planting novos, multiplayer deformável homologado ou equipamento deformável. São limites para adoção definitiva, não problemas ocultos por um build verde.

## Reprodução e preservação

Na pasta `minecraft`:

```powershell
.\scripts\start-r3.ps1 -Interactive
```

Esse comando verifica hashes, gera apenas o novo derivado quando necessário e abre o laboratório isolado. A fixture interativa prepara Steve/espada e plataforma descartável em `TOE_R3`; não usar esse mundo como save de jogo. Use F5, `/toe_renderlab skinned`, `/toe_renderlab off`, `/toe_renderlab idle|jog|roll|sword`. O modo selecionado acompanha CPM; V e J permanecem os controles aprovados. `/toe_combat on` habilita o ataque real e `/toe_combat off` retorna ao ataque visual. Para comparação automática: `-Mode cpm`, `-Mode r2` ou `-Mode r3` sem `-Interactive`.

Arquivos novos:

- `animation_pipeline/scripts/prepare-r3.cjs`: inventário de preservação, backup local, extração dos canais originais da coluna, cópia de recursos R3.
- `animation_pipeline/scripts/blender-r3-refine.py`: refinamento reproduzível do Blend R1; somente saídas R3.
- `animation_pipeline/scripts/validate-r3.cjs`: pesos, UVs, bind, hierarquia, duração e orientação global comparadas ao R2.
- `experiments/m002_r3/java/`: renderer/comandos experimentais, auditoria e fixture visual; não entra no build normal.
- `scripts/start-r3.ps1`: execução e preparação isoladas; usa o runtime Polymesh oficial previamente validado no R2.
- `animation_pipeline/scripts/collect-r3.cjs`: consolida resultados completos, exige os 132 checks de combate e copia capturas reais com SHA-256.
- `animation_pipeline/converted/m002_r3/`: Blend, GLB, canais e recursos derivados, fora do Git.
- `animation_pipeline/reports/M002_R3_*.json` e `M002_R3_visual/`: verificações e capturas.

Há backup e hashes de 54 arquivos aprovados em `.downloads/m002_r3/backup/` e `M002_R3_BASELINE.json`. O validador anterior também verifica os 171 arquivos do inventário Godot. Nenhuma classe de gameplay foi modificada.

## Recomendação

Manter R3 como candidato experimental reversível. A flexão ponderada do torso está funcional com a biblioteca existente; não há motivo demonstrado para criar outro renderer ou instalar outra biblioteca. Para promover futuramente: aprovação estética do usuário, revisão quadro a quadro de Roll/volume, homologação de skins variadas, equipamento, estados vanilla, render remoto e desempenho em outras máquinas. Não substituir CPM nem iniciar M002.2 nesta entrega.
