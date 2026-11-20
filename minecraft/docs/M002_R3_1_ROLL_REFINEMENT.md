# M002.R3.1 — Refinamento do Roll

Execução iniciada em 12/11/2026 e concluída em 13/11/2026. Minecraft 1.20.1, Forge 47.4.28, Java Microsoft 17.0.20.1, CPM 0.6.26a, Polymesh 1.0.0, Blender 5.2.3 LTS já instalado. Nenhuma nova dependência.

## Entrega

Variante experimental independente em `-PtoeR31` / `run-r31/`. O Roll recebe uma camada corretiva de volume em morph targets glTF, executada pelo Polymesh antes do skinning linear. Preservam-se os doze ossos, suas posições e rotações, os quatro clipes e suas durações. Idle/Jog/Sword usam o próprio GLB R3 aprovado, copiado sem modificação, inclusive na execução interativa. CPM continua padrão no build normal e fallback no laboratório.

[Comparador CPM/R2/R3/R3.1](../experiments/m002_r3_1/COMPARE.html) · [Testes e evidências](TEST_LOG_M002_R3_1.md) · [Resultados consolidados](../animation_pipeline/reports/M002_R3_1_TEST_RESULTS.json).

O ganho comprovado é a recuperação da espessura das articulações. **Não se declara resolvida toda sobreposição do Roll.** A adoção estética continua dependendo do usuário; R3 não foi sobrescrito e nenhum sistema Godot ou combate foi alterado.

Validação final: R3.1 58/58, CPM 58/58, R2 56/56, R3 57/57, combate 132/132, sincronização 18/18 e reinício 1/1. Builds experimental e normal passaram. Foram arquivadas 210 capturas; o JAR em `build/libs/` é o normal, sem assets privados. A média CPU do bloco de desenho subiu de 0,529 ms no R3 para 0,809 ms no R3.1 neste ensaio; não é medição de FPS geral.

## Diagnóstico dos 45 quadros

Foram avaliados todos os quadros do `UAL_roll`, de 1 a 45, usando LBS real do modificador, volume fechado, área das seções transversais, ângulos relativos sem a base de repouso e pares de triângulos por BVH. A duração continua 44/30 = 1,4666667 s, consistente com a duração anterior arredondada de 1,467 s.

| Quadro (1-based) | Tempo | Defeito principal e causa |
|---|---:|---|
| 4 | 0,100 s | Cotovelo direito: cerca de 159,7° de flexão; seção cai a 17,6% no LBS. Mistura de rotações, não UV deslocada. |
| 11 | 0,333 s | Cotovelo esquerdo: cerca de 158,5°; seção cai a 18,6%. Mesma contração com proporções cúbicas. |
| 15 | 0,467 s | Máxima diferença pelve/peito, 35,2°. O torso perde apenas cerca de 1% de área; não é a origem dominante do achatamento. |
| 23 | 0,733 s | Quadril muito fechado, rotações relativas até aproximadamente 173,4°. Coxas se aproximam do abdômen; contato entre volumes e proporções do retarget, além de compressão. |
| 26 | 0,833 s | Joelho direito: cerca de 145°; seção mínima de 30,0% no LBS. |
| 27 | 0,867 s | Joelho esquerdo: cerca de 123,9°; seção mínima de 47,0%. |
| 30 | 0,967 s | Recuperação do agrupamento; verificar cintura, mãos e continuidade dos membros. |

Os valores são medidas de seções específicas do modelo, não percentuais de volume de todo o personagem. BVH registra superfícies que se cruzam ou encostam; a contagem depende da triangulação e **não mede volume de interseção**, nem permite comparar diretamente malhas com mais loops. Os JSONs completos `M002_R3_1_DIAGNOSIS_R3.json` e `..._R31.json` registram cada quadro e região.

As matrizes de bind estavam corretas, as proporções de repouso eram estáveis e não houve indício de UV como causa principal da contração. A cabeça grande e as pernas/braços cúbicos do Steve também impõem contatos que não existem no personagem humano da UAL. Não se pode resolver esse conjunto inteiro apenas acrescentando cortes.

## Técnica e alterações

### Geometria, pesos e UV

O script abre o Blend R3, preserva seus doze objetos e adiciona um loop intermediário na transição pelve/coxa de cada perna e de sua camada externa. O R3 tinha um intervalo de dois pixels entre anel de pelve e anel de coxa; o novo anel interpola pesos e UVs nesse intervalo. Cotovelos e joelhos já possuíam vários anéis, portanto não se acrescentou geometria sem função nessas regiões.

Estreitar uma faixa de pesos ou adicionar anéis não resolve a contração do anel central de influência 50/50. A soma linear de duas rotações muito diferentes contrai a seção; em flexões próximas de 160° a espessura cai fortemente mesmo com pesos normalizados. Foram redistribuídos 48 vértices de modelagem nas misturas exatas 50/50 para 60/40 a favor do osso proximal, evitando matrizes quase singulares. Essa alteração, sozinha, ainda não preserva a seção: é combinada com a camada corretiva, sem mudar as proporções em repouso. Só a variante Roll usa esses pesos.

As UVs são mantidas nos mesmos texels da skin Classic 64×64; os cortes de quadril interpolam a ilha existente. Não há textura nova, repintura, conversão de skin ou alteração de transparência das camadas externas. A exportação dos morph normals divide mais vértices por face: o GLB R3.1 tem 1.088 vértices exportados contra 816 no R3. Isso não equivale a reconstruir a geometria do personagem; parte do aumento são duplicatas para normais e UVs.

### Camada corretiva, separada do movimento

Os 45 quadros originais não bastaram para interpolar a primeira prova de conceito. A versão final utiliza 193 amostras corretivas: base a 120 Hz e amostras extras nos intervalos problemáticos. Os tempos são reproduzíveis em `r31-corrective-times.json`. Para cada amostra:

1. Lêem-se as matrizes do GLB R3 com a mesma interpolação SLERP/linear usada no jogo, convertendo eixos e bases de repouso para Blender.
2. Calcula-se, para cada vértice, a matriz LBS ponderada `M = Σ(wj × posej × inverseBindj)`.
3. Extrai-se a rotação de `M` por decomposição polar (SVD). O centro de cada seção segue o LBS, e sua largura/profundidade usam essa rotação sem a contração. Assim a seção mantém seu formato cúbico, sem introduzir escalas nos ossos.
4. Armazena-se como shape key a posição pré-skin `M⁻¹ × posição_desejada`. O glTF anima seus pesos somente em `UAL_roll`. Demais clipes e valores de repouso têm pesos zero.

O Minecraft **continua usando LBS**; ele não ganha um renderer novo. A inversão serve para que seu pipeline existente, morph → LBS, reconstrua a seção desejada. O erro máximo de reconstrução foi inferior a 0,0000006 bloco. Não houve limite de rotação, modificação de pivô, alteração de canais originais ou nova animação de Roll. O validador compara todos os doze ossos e encontra erro zero de orientação e posição em relação ao R3 nos tempos originais.

A primeira prova com alvos derivados do DQ do Blender recuperava os 45 quadros, mas falhava entre alguns deles. Ela foi descartada como versão final. A solução polar regularizada evita a inversão quase singular e a escolha descontínua de hemisfério dessa tentativa. Em 705 tempos avaliados, inclusive entre keys, a área das seções ficou entre **99,96% e 104,91%** do repouso. Isso não é garantia matemática sobre todo instante contínuo; é uma amostragem densa e reproduzível de todo o clipe.

### Suporte comprovado no Minecraft

Antes de adotar a técnica, foram inspecionados `AnimationController`, `GltfModelRenderer` e `CpuSkinningPipeline` do Polymesh local. Há suporte a pesos animados e deltas de posição/normal antes do skinning. A primeira tentativa revelou uma limitação real: accessors sparse sem buffer base, exportados pelo Blender, provocam falha no loader jgltf do runtime. O GLB foi adaptado por script para accessors densos, sem modificar a biblioteca ou instalar dependências.

Após essa adaptação o modelo carregou, os pesos foram avaliados nos 45 quadros dentro da JVM do cliente e as capturas nativas mostraram a espessura recuperada. A auditoria nativa mede o modelo e os canais efetivamente carregados, seguindo a mesma ordem morph → LBS, e registra os resultados em `M002_R3_1_NATIVE_MORPH_AUDIT.json`. As imagens fornecem a confirmação visual; apenas ler o código ou compilar não seria suficiente.

## Comparação e resultado visual

O comparador usa os quadros críticos 4/11/15/23/26/27/30 em quatro ângulos, além das amostras anteriores de Idle, Jog, Sword e Roll. CPM usa o quadro original congelado, exportado com o serializer instalado; R2 e R3 usam seus GLBs intactos. Câmera, skin Steve, luz e tempo são equivalentes.

A seção do cotovelo direito passa de aproximadamente 17,6% para 100% nas amostras medidas; esquerdo de 18,6% para 100%; joelhos de 30,0%/47,0% para perto de 100%. Isso representa área de seção, não promessa de volume total constante ou ausência de colisão. Nas capturas, cotovelos/joelhos conservam espessura em vez de formar uma faixa fina, e o quadril tem uma transição geométrica adicional. A camiseta e os pixels da skin permanecem reconhecíveis.

O quadro 23 continua sendo a limitação mais importante: recuperar volume não afasta automaticamente coxas do abdômen. Alguns contatos podem ficar mais perceptíveis porque os membros deixaram de achatar. O Roll também continua baixo em relação ao chão. Não se alterou a física para esconder isso. É necessária revisão estética antes de promover a variante.

A espada utiliza o mesmo osso `103` e anchor `303`; a camada corretiva preserva posições do pulso e os canais dos ossos. O item é desenhado uma vez. Equipamento não homologado, primeira pessoa, Slim e jogadores remotos continuam no caminho CPM/vanilla, sem implementar armaduras ou capas.

## Reprodução e arquivos

Na pasta `minecraft`:

```powershell
.\scripts\start-r31.ps1 -Interactive
```

O script verifica o backup, gera somente derivados R3.1 quando necessário, materializa os accessors densos, valida e usa `run-r31/`. A fixture prepara uma plataforma e Steve com espada no mundo descartável `TOE_R31`. Comandos preservados: `/toe_renderlab skinned`, `/toe_renderlab off`, `/toe_renderlab roll`, `/toe_combat on|off`; V/J continuam os controles aprovados. Para ensaiar o Roll em movimento automático, ative locomoção/CPM e use V. Os comandos manuais do laboratório servem à inspeção.

Scripts novos:

- `prepare-r31.cjs`: hashes/backup de 68 arquivos aprovados, recursos isolados e projetos CPM dos quadros críticos.
- `blender-r31-diagnose.py`: medidas dos 45 quadros, antes/depois, sem salvar os originais.
- `blender-r31-refine.py`: loop de quadril e bake da camada corretiva.
- `patch-r31-glb.cjs`: dados densos e canais de pesos somente no Roll.
- `validate-r31.cjs`: preservação, pesos, bind, UV, topologia, morphs, duração, todos os ossos e 705 tempos de interpolação; `--sample-only` extrai as matrizes de origem para o bake.
- `collect-r31.cjs`: consolidação de testes e capturas.
- `scripts/start-r31.ps1`: geração/execução isoladas.

Derivados em `animation_pipeline/converted/m002_r3_1/` e backups em `.downloads/m002_r3_1/backup/`, fora do versionamento. Experimento Java em `experiments/m002_r3_1/java/`. Capturas e auditorias em `animation_pipeline/reports/M002_R3_1_*`. Não publicar o JAR experimental com movimentos de terceiros; o artefato final em `build/libs/` é o mod normal, sem esses recursos.

## Recomendação e limites

Manter R3.1 como opção experimental, com rollback para R3/CPM. A recuperação de seção está comprovada, mas a camada custa mais dados e processamento; as medições estão no registro de testes. A interpolação entre amostras ainda precisa de revisão estética quadro a quadro. Normais do pipeline e superfícies muito dobradas podem produzir mudanças de iluminação; não há garantia para todas as skins e camadas externas.

Uma futura correção do contato coxa/abdômen pode exigir limites articulares ou uma camada de afastamento específica ao Steve; não foi aplicada silenciosamente, pois modificaria trajetórias aprovadas. Nenhuma nova mecânica, biblioteca, física, render remoto ou fase seguinte foi implementada. A aprovação estética definitiva é do usuário.
