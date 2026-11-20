# M002.A1.2 — Transições de combate

13/11/2026. Polymesh R3 continua padrão, AUTO com espada e controles remapeados preservados. Este marco altera a composição visual e a entrega do cancelamento ao cliente. Não altera dano, duração dos golpes, física, colisões ou i-frames.

## Diagnóstico registrado antes das mudanças

Foram lidos os três relatórios A1.1 e inspecionado seu visualizador com 24 sequências. O diagnóstico prévio está em [DIAGNOSIS_BEFORE.md](../animation_pipeline/reports/m002_a1_2/DIAGNOSIS_BEFORE.md).

- Pesado: troca imediata para a pose do recorte 60–130. `setAnimationTime` do Polymesh limpa o blend nativo; apenas chamar crossfade não preservaria a sincronização autoritativa.
- C → Jog: poses de quadril, tronco e ombros distantes, troca imediata e reinício do loop. A ruptura não vinha de uma desconexão nova do esqueleto.
- Ataque → Roll: estado vazio de combate era enviado antes da aceitação do Roll; durante o registro CPM o seletor ainda escolhia Idle/Jog.
- Pés B/C: o retarget preservava rotações e descartava a translação vertical da pelve da fonte. Em B a pelve original fica entre 0,478–0,571 m, contra 0,917 m no repouso, enquanto os dois `ball` permanecem em aproximadamente 0,0152 m. C contém elevações legítimas; não pode receber apoio permanente. Pesos, geometria e UVs não foram modificados.

## Compositor e configuração

`R3VisualPose` envolve o sampler existente. Captura a pose efetivamente exibida, decompõe matrizes locais em translação/rotação/escala, aplica LERP/SLERP pelo caminho curto com smoothstep e recompõe a hierarquia. Não interpola matrizes globais diretamente. O relógio do clipe continua sendo o do servidor, inclusive durante a mistura.

Arquivo gerado pelo Forge: `config/towerofentropy-visual-client.toml`.

| Parâmetro | Padrão | Uso |
|---|---:|---|
| `heavyEntrySeconds` | 0,120 s | Idle/Jog → pesado |
| `returnToLocomotionSeconds` | 0,140 s | Ataque → Idle/Jog |
| `lightChainSeconds` | 0,000 s | Preserva as curvas A/B/C; não suaviza indiscriminadamente |
| `rollVisualSeconds` | 0,060 s | Pose atual → Roll autorizado |
| `sourceContact.enabled` | true | Correção vertical visual B/C |
| `sourceContact.settleSeconds` | 0,060 s | Resposta exponencial, aproximadamente 95% nesse tempo |
| `sourceContact.maxVisualLoweringBlocks` | 0,270 | Limite de abaixamento visual |

Valores existentes de configuração não são sobrescritos. O teste inicial de 100 ms para pesado foi ajustado para 120 ms após comparação. Um blend de 60 ms entre leves foi descartado: produzia recuperação acelerada desnecessária. A configuração final mantém os movimentos autorais dos leves.

Jog utiliza fase acumulada de locomoção e não reinicia no retorno do ataque. A fase continua avançando durante deslocamento. Não há root motion físico dos ataques.

A espada usa o mesmo antebraço composto do corpo. O anchor de punho interpola rotação e conserva `p + q*K = F`, com K=(-1,10,-2)/16 e F=(-1,6,-2)/16. Isso evita deriva da empunhadura durante a interpolação. O renderer continua cancelando vanilla/CPM somente depois de desenhar com sucesso.

## Cancelamento atômico

O servidor continua limpando o ataque, a fila e os impactos pendentes antes de autorizar Roll. Nesse caminho suprime apenas o pacote intermediário vazio; o pacote de aceitação limpa o estado visual do ataque e ativa Roll conjuntamente. Falha ou exceção no registro envia limpeza explícita.

Durante registro CPM pendente, o Polymesh já exibe a pose inicial do Roll, misturada por 60 ms. O avanço visual e a física continuam aguardando o mesmo callback original. Não antecipamos o início físico nem os ticks de proteção. Um registro CPM frio ainda pode manter essa pose inicial parada. Protocolo passou a 6; cliente e servidor devem usar a mesma versão.

## Apoio visual limitado

`visual-support.json` guarda somente confiança de contato derivada dos `ball` originais, não novos clipes. B: 17 amostras de apoio. C: 61 amostras, das quais 37 apoiadas, 18 parciais e seis aéreas. A confiança cai suavemente entre 15 e 45 mm de elevação na fonte.

O compositor calcula a sola via LBS dos vértices inferiores das pernas e camadas externas. Quando o jogador está no chão e a fonte indica apoio, aplica translação Y uniforme ao modelo, limitada a 0,27 bloco. Nunca escreve posição, velocidade ou colisão da entidade. Ao sair do ataque libera a correção ao longo do blend. Não trava X/Z e não prende os pés durante o salto de C. A soltura é exponencial; existe resíduo submilimétrico nas amostras aéreas.

Isso corrige o afastamento vertical, mas não é IK de terreno nem elimina deslizamento horizontal autoral. A camada externa de C ainda pode penetrar cerca de 0,054 bloco no plano, também observado no modelo anterior. Não houve remodelagem para eliminá-lo.

## Preservação e limites

Godot, GLBs originais, modelos R3/R3.1, UVs, pesos, curvas e assets aprovados foram preservados por hashes. CPM e os comandos de diagnóstico continuam disponíveis. Primeira pessoa, equipamentos e estados não suportados mantêm os fallbacks anteriores. Não foi implementada paridade dos novos clipes B/C/pesado no CPM, renderização R3 remota ou homologação em servidor dedicado com latência.

Veja [QA visual](M002_A1_2_VISUAL_QA.md) e [testes](TEST_LOG_M002_A1_2.md). Aprovação estética permanece com o usuário. Nenhum próximo marco foi iniciado.
