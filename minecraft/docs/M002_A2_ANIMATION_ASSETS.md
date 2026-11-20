# M002.A2 — Assets de locomoção

13/11/2026. Fontes locais inspecionadas somente para leitura, incluindo `C:/Users/balen/Desktop/tower-of-entroy/ANIMATION_INVENTORY.md`, inventário M001.0 e os dois GLBs Standard preparados em M001.0. Nenhum arquivo Godot foi modificado.

| Estado | Origem real | Duração fonte | Decisão |
|---|---|---:|---|
| IDLE | UAL1 `Idle_Loop` | 2,500 s | Clipe aprovado preservado |
| WALK | UAL1 `Walk_Loop` | 1,333333 s | Novo derivado, 41 amostras incluindo endpoint |
| JOG | UAL1 `Jog_Fwd_Loop` | 0,933333 s | Clipe aprovado preservado |
| SPRINT | UAL1 `Sprint_Loop` | 0,666667 s | Novo derivado, 21 amostras incluindo endpoint |
| MOVE_BACKWARD | Sem clipe dedicado adequado nos GLBs UAL atuais | — | Estado reconhecido; fallback anterior |
| STRAFE_LEFT/RIGHT | Sem clipes dedicados adequados nos GLBs UAL atuais | — | Estados reconhecidos; fallback anterior |

UAL1 também possui `Walk_Formal_Loop`, não selecionado em favor da marcha comum. UAL2 possui `Walk_Carry_Loop` e `Zombie_Walk_Fwd_Loop`, candidatos especializados que não foram convertidos nem homologados visualmente para marcha neutra. Golpes e knockback não foram tratados como locomoção reversa. Não foi inventado strafe girando um Jog frontal, nem reproduzido um ciclo invertido como prova de marcha para trás.

A seleção não depende apenas dos nomes: foram examinados rig/canais, translações, sinais de contato dos pés e poses convertidas no cliente. Walk apresenta apoio alternado contínuo; Jog/Sprint têm fases aéreas. A aprovação estética das novas animações permanece pendente.

## Pipeline e preservação

UAL1 Standard usa 65 joints e canais de translação/rotação/escala, com root sem deslocamento acumulado nos ciclos escolhidos. São usadas as rotações medidas da cadeia pelvis/spine, braços e pernas. O mesmo escritor de matrizes do R3 aprovado aplica a retenção de pelve/tronco de 0,35 e as convenções de coordenadas existentes. As translações originais de root/pelve são descartadas, como no pipeline aprovado: o deslocamento mundial continua exclusivamente vanilla. Não há root motion físico adicionado.

`prepare-a2.cjs` reutiliza em memória apenas o sampler anterior, interrompido antes dos escritores; gera `converted/m002_a2/poses-new.json`. `blender-a2.py` abre o `.blend` R3 somente para leitura e grava uma cópia nova e o GLB de movimento. Não executa as partes de criação/edição de malha do script histórico; reutiliza somente seu escritor de poses.

`package-a2.cjs` acrescenta os dois canais de animação ao GLB de runtime aprovado, em **outro arquivo**, `converted/m002_a2/resources/assets/towerofentropy/models/gltf/steve_a2.glb`. Os nodes, meshes, skins, materiais, UVs, pesos, inverse bind matrices e o prefixo binário antigo são idênticos. Os sete clipes existentes permanecem byte a byte. O arquivo anterior `steve_r3.glb` continua disponível para diagnóstico e comparação.

Anchors de mãos seguem a convenção original. Os novos ciclos recebem os anchors derivados dos pulsos, além da restrição de empunhadura já validada em A1.2. O controlador usa o mesmo antebraço composto para corpo e espada.

## Fase e contato

A comparação normalizada da diferença de altura entre os dois `ball` indica offsets de fase relativos ao Jog de **0,19 ciclo para Walk** e **0,99 ciclo para Sprint**. Eles são metadados do relógio visual; nenhum keyframe foi modificado. É um alinhamento de apoio aproximado, não uma reconstrução de root motion ou IK.

Na amostragem de 101 instantes da fonte, Walk não apresentou ambos os `ball` acima de 45 mm; Jog apresentou 69 e Sprint 65 amostras aéreas. Alturas máximas individuais: Walk 0,188 m, Jog 0,393 m, Sprint 0,398 m. Não se deve forçar contato permanente nos ciclos de corrida. Dados: [GAIT_ANALYSIS.json](../animation_pipeline/reports/m002_a2/GAIT_ANALYSIS.json).

A geometria alvo continua tendo camadas cúbicas externas e adaptações de comprimento. Pequena penetração em Walk e folga aérea em Jog/Sprint devem ser avaliadas nas capturas; não representam uma promessa de foot locking. Nenhum peso, UV ou vértice foi alterado para corrigi-las. A correção de apoio B/C de A1.2 permanece a mesma, sem ampliá-la indiscriminadamente aos ciclos.

## Arquivos e licença

- Fonte mínima: `animation_pipeline/input/UAL1/UAL1_Standard.glb`, SHA-256 `69591853d817488edaa8fd9bf8fc1d821eaeaf789f8627b3cd23b41c4ed67997`.
- UAL2 foi inspecionado, mas nenhum novo clipe dele foi convertido neste marco.
- Derivados novos em `animation_pipeline/converted/m002_a2/`; entradas e conversões continuam ignoradas pelo Git.
- Scripts, metadados de auditoria e documentação ficam separados dos originais.

Os `License.txt` locais dos pacotes UAL declaram **CC0 1.0 Universal**, autoria Quaternius. Nenhuma licença foi inferida para outros pacotes. Não foram adicionados assets de Epic Fight ou de mods de gameplay. Nenhuma ferramenta ou dependência nova foi instalada; foi utilizado Blender 5.2.3 LTS já existente.

Para regenerar com os arquivos locais existentes:

```powershell
node animation_pipeline/scripts/prepare-a2.cjs
node animation_pipeline/scripts/analyze-a2-gaits.cjs
& 'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' --background --python animation_pipeline/scripts/blender-a2.py -- $PWD
node animation_pipeline/scripts/package-a2.cjs
node animation_pipeline/scripts/validate-a2.cjs
```

O build normal prepara o empacotamento, mas não chama Blender nem reconverte silenciosamente os originais. A cópia de movimento deve ter sido gerada antes.
