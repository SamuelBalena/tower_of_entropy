# M003.E1 — Tower Animation Studio

No mundo, pressione **F8** ou execute **`/toe_animstudio`**. O atalho é configurável na categoria Tower of Entropy. O comando é registrado no cliente.

Para experimentar a sequência de 2 segundos pronta, copie o exemplo validado para o diretório de projetos da instância e abra-o pela biblioteca. [Projeto Classic teste_aceno](../validation/m003e1/examples/teste_aceno.json)

1. Abra **Ajustes** para escolher Classic ou Slim, a pose neutra ou Idle, a duração e o FPS de edição. Para reproduzir o exemplo `teste_aceno`, defina 2 segundos e 30 FPS.
2. Escolha um osso no painel **Esqueleto** ou clique em sua articulação no viewport. Arraste um anel colorido em torno do pivô. Para os braços, azul é frontal, verde abre e fecha lateralmente e vermelho torce. **Modo** alterna para os eixos locais X/Y/Z. Digite valores no Inspector e pressione **Enter** para aplicá-los.
3. Na timeline, coloque o playhead em 0, 0,50, 1,00, 1,50 e 2,00 segundos. Ajuste o braço direito e use **+ Key** a cada pose. Arraste marcadores para mudar seu tempo e use **Apagar** para excluir a chave selecionada.
4. Use **Play** ou **Espaço** para reproduzir ou pausar. **Loop** repete a sequência e **Vel.** alterna entre 0,25×, 0,50×, 1×, 1,50× e 2×.
5. Salve o projeto e reabra-o pela biblioteca. **Salvar +** cria outra cópia. A biblioteca permite renomear, duplicar, excluir, importar e exportar projetos JSON. A opção **Recuperar** lista rascunhos preservados.

Botão direito arrastado orbita a câmera. Botão do meio ou Shift com botão direito move o enquadramento. A roda aproxima ou afasta. **Ossos** alterna os controles do esqueleto e **Foco** centraliza a articulação selecionada.

Pressione **Shift** durante o arraste para reduzir a sensibilidade. **Snap** alterna entre livre, 1°, 5° e 15°. **Câmera** abre as vistas fixas e a orbital. [Controles e persistência da M003.E1.1](M003_E1_1_ANIMATION_STUDIO_PRECISION.md)

| Atalho | Ação |
| --- | --- |
| Espaço | Reproduzir ou pausar |
| Ctrl + S | Salvar |
| Ctrl + Z | Desfazer |
| Ctrl + Y ou Ctrl + Shift + Z | Refazer |
| Delete | Apagar a chave selecionada |
| Home | Voltar ao início |
| Esc | Cancelar o arraste ativo, sair do campo ou fechar o editor. Alterações pendentes abrem uma confirmação ao fechar. |

## Pose e reprodução

A árvore vem das juntas dos skins do `RuntimeModel`. Ela exibe os 12 ossos do R3, incluindo quadril, cintura, ombros, cotovelos, pernas e joelhos. Identificadores internos são usados nas chaves. O Inspector expressa a rotação em graus no modo ativo e a translação da raiz em unidades do modelo. Translações da raiz são limitadas a duas unidades por eixo.

A pose salva é um deslocamento relativo à referência local. A opção Idle copia o primeiro instante de `UAL_idle` com um controlador exclusivo do editor. A avaliação recompõe as matrizes globais dos descendentes na hierarquia original e envia o resultado ao `GltfModelRenderer`. A prévia usa os pesos, malhas e UVs dos GLBs de runtime e a skin do jogador ou uma skin Steve da variante correspondente.

Com Auto Key, uma edição cria ou atualiza a chave do osso no quadro corrente. Uma pose pendente pode ser registrada com **+ Key**. Navegar ou iniciar a reprodução registra a pose pendente no quadro de origem e mostra essa ação na barra de estado. A curva de saída de cada chave pode ser linear, constante ou suave. A rotação interpola por SLERP. A reprodução usa segundos reais e o FPS define a grade de edição. Ao mudar o FPS, chaves existentes são arredondadas para a nova grade. Colisões e chaves fora da duração precisam ser resolvidas antes da alteração.

O editor é uma `Screen` cliente com sessão isolada. No singleplayer, a tela pausa a simulação do mundo, enquanto a reprodução continua pelo relógio do editor. Um arraste contínuo cria uma entrada de histórico. As matrizes de pose são reutilizadas e recalculadas ao mudar a pose ou o tempo.

## Projetos JSON

Os projetos ficam em `config/towerofentropy/animation_studio/projects/` dentro da instância do Minecraft. A persistência usa a fila de I/O do jogo. Para salvar, são verificados modelo, assinatura do esqueleto, quadros, curvas e valores finitos. O salvamento prepara um arquivo temporário no mesmo diretório. **Salvar +** exige um nome livre. **Salvar** pede confirmação antes de substituir o projeto ativo e guarda a versão anterior. A substituição usa movimentação atômica quando suportada.

| Campo | Schema 1 |
| --- | --- |
| `schema` | Inteiro `1` |
| `name` | Nome do projeto |
| `model` | Identificador do recurso Classic ou Slim |
| `skeleton` | Assinatura dos nomes, da hierarquia e das matrizes locais de referência |
| `idle` | Se verdadeiro, a referência é `UAL_idle` no instante zero |
| `frames`, `fps`, `loop` | Duração em quadros, grade de edição e reprodução em loop |
| `tracks[].bone` | Identificador do osso da track |
| `tracks[].keyframes[]` | Chaves com `frame`, `curve` e `transform`. A curva de saída pode ser `LINEAR`, `STEP` ou `SMOOTH`. |
| `transform` | Deslocamento local `x/y/z` e quaternion unitário `qx/qy/qz/qw` |

Cada arquivo identifica sua variante, mesmo quando os nomes e a topologia coincidem. Ao abrir, o editor carrega a variante registrada e verifica a assinatura. A M003.E1 salva o projeto editável. Camadas, importação de clipes, IK e exportação para o runtime pertencem às próximas etapas. A abertura desta versão ocorre dentro de um mundo.

Em versões futuras, uma migração de schema pode adicionar uma lista de camadas acima das tracks, armazenar referências para clipes e normalizar a composição local antes da resolução da hierarquia. IK pode atuar como uma etapa dessa composição, antes do skinning. Um exportador futuro pode converter poses locais em canais do runtime e encaminhar a escolha do clipe para o sistema de gameplay.

## Reproduzir a validação

Execute a partir de `minecraft/` com a instância de QA que contém o mundo `Global Survival one`.

```powershell
.\validation\m003e1\verify-studio.ps1 -Template '.logs/s002-verified/854x480-F'
```

A suíte abre o editor pelo atalho dentro do mundo, aciona os controles reais da tela e cria `teste_aceno` com cinco chaves no braço direito. Ela verifica picking, gizmo, rotações locais, propagação para os descendentes, translação da raiz, rotação da câmera, zoom, arraste de uma chave após editar sua pose, exclusão de chaves, desfazer/refazer, Auto Key, SLERP e loop. A mesma sequência salva, fecha, reabre pelo comando cliente, executa novamente, testa confirmação ao sair, rejeita JSON inválido e exercita Salvar +, renomear e excluir uma cópia.

Os casos JVM em `StudioProjectCases` passaram para as três curvas, preservação da curva existente com Auto Key, reamostragem de FPS, rejeição de valores não finitos e restrição da translação à raiz.

O fluxo passou em Classic com Oculus e em Slim com `-PtoeNoOculus`. O framebuffer chegou a 1604 × 881 neste ambiente. O pedido de 1920 × 1080 ficou sujeito aos limites da janela.

| Framebuffer | Classic com Oculus | Slim sem Oculus |
| --- | --- | --- |
| 854 × 480 | [Captura](assets/m003_e1/classic-854x480.png) | [Captura](assets/m003_e1/slim-854x480.png) |
| 1280 × 720 | [Captura](assets/m003_e1/classic-1280x720.png) | [Captura](assets/m003_e1/slim-1280x720.png) |
| 1604 × 881 | [Captura](assets/m003_e1/classic-1604x881.png) | [Captura](assets/m003_e1/slim-1604x881.png) |

## Desempenho no cliente

| Configuração | Quadros do viewport | FPS do cliente no fim da amostra | Avaliação média por pose | Renderização média por quadro | Entrada do gizmo | Heap usado pelo cliente |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Classic com Oculus | 420 | 143 | 0,062 ms | 0,397 ms | 0,319 ms | 1.006 MiB |
| Slim sem Oculus | 351 | 102 | 0,114 ms | 1,116 ms | 0,305 ms | 990 MiB |

As amostras foram coletadas no fluxo de edição e reprodução de 854 × 480. O tempo de avaliação conta poses recalculadas. O tempo de render distingue a submissão do modelo, com skinning por CPU e envio de buffers. O tempo de gizmo cobre uma interação do teste, da seleção do controle ao fim do arraste. FPS e heap são indicadores do cliente inteiro nessa amostra. As duas linhas representam configurações e sequências distintas.

## Regressões

O build offline passou. A suíte de parkour e combate aprovou 30 verificações. Câmera, armaduras e Polymesh passaram 95 verificações em Classic e 95 em Slim. A suíte de iluminação executou pares com parede, vidro, inventário e lanterna traseira. Os 3.048 pontos atrás da parede apresentaram diferença RGB máxima zero. A base verificada foi `39933dd2a31e026753944ddf53158c58b44bd945`.
