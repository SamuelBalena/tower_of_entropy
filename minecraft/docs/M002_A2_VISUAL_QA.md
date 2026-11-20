# M002.A2 — QA visual da locomoção

13/11/2026. Implementação técnica com avaliação estética ainda pendente do usuário. Estados direcionais sem clipe adequado continuam explicitamente pendentes.

Abra o [comparador completo](../animation_pipeline/reports/m002_a2/COMPARATOR.html), escolha o mesmo roteiro/ângulo nos dois painéis e mantenha A1.2 à esquerda e A2 à direita. A [seleção compacta](../animation_pipeline/reports/m002_a2/review/INDEX.html) contém 12 pares de PNG, sem precisar copiar as milhares de capturas para uma revisão inicial.

## Evidência

São **4.320 PNG reais**, em 120 sequências: 20 roteiros × três ângulos × duas versões. Captura a **10 Hz**, com reprodução 1×, 0,5×, 0,25× e 0,1×. Câmera lenta repete os quadros capturados; não cria poses intermediárias. Nenhuma ferramenta nova de vídeo foi instalada.

Os roteiros abrangem Idle, marcha lenta, Jog, Sprint/parada, marcha reversa, lados, diagonal frontal, inversão rápida, partida/parada, variação de velocidade, espada equipada, retorno de ataque/Roll, parede, lajes, pulo, agachamento, sequência de terreno/Sprint → Jog e sprint remapeado. Vistas frontal, lateral e traseira usam a mesma câmera orbital de QA e iluminação diurna fixa.

Antes e depois são alinhados pelo tick do roteiro, com resolução de 100 ms. As JVMs são separadas: a fase do sampler e o último frame renderizado podem diferir dentro desse intervalo. A legenda pode mostrar um estado recém-atualizado e o clipe do frame anterior. Não se trata de vídeo contínuo nem alinhamento subquadro.

O caminho anterior mantém o comportamento A1.2: Jog genérico em movimentos simples e fallback durante Sprint. O caminho posterior exibe os novos Walk/Sprint. Na marcha para trás e nos lados o comparador identifica **FALLBACK**; o antigo controller Polymesh fica armazenado sem representar o corpo desenhado. Suas métricas de sola/pose não são usadas para avaliar esses frames.

## Resultados observados

- Walk tem marcha mais ereta e cadência distinta do Jog. Foi exercitado por movimento vanilla reduzido sobre soul sand, sem tecla nova.
- Jog aprovado foi mantido. Diagonais frontais selecionam o ciclo pela direção efetiva; inverter para trás entrega o corpo ao fallback.
- Sprint usa o ciclo UAL1, acompanhando o estado vanilla. Remapear Sprint para R no QA continuou ativando a mesma animação; o binding foi restaurado depois.
- Walk → Jog → Sprint foi observado no roteiro de variação de velocidade. A sequência extra confirma Sprint → Jog com avanço mantido. A mudança de terreno dessa fixture também causa parada/fallback transitórios: não deve ser confundida com uma sequência contínua perfeita dos quatro ciclos.
- Ataque e Roll tiveram prioridade e retornaram à locomoção correspondente. Não foram criadas animações de combate.
- Contra a parede, avanço pressionado com deslocamento efetivo nulo retorna a Idle. Pulo e agachamento entregam o corpo ao fallback. Em lajes há alternância com estados especiais vanilla; não há IK de terreno.

A mistura configurada e testada é de **140 ms**, com fase compartilhada e offsets de apoio derivados da fonte. Não foram alterados os keyframes. Walk tem costura de orientação praticamente nula; Sprint conserva aproximadamente **0,647°** de diferença máxima entre endpoints. Não foi feita uma edição artificial do último frame para esconder essa diferença.

## Passos versus distância

Medição no trecho estável lateral, entre 1,5 e 2,7 s do roteiro, 13 amostras por ciclo selecionado. Distância vem da posição real da entidade; ciclos vêm do avanço do relógio da animação, incluindo wrap.

| Movimento | Velocidade observada | Distância por ciclo visual |
|---|---:|---:|
| Walk | 2,508 blocos/s | 2,792 blocos |
| Jog | 4,314 blocos/s | 4,020 blocos |
| Sprint | 5,608 blocos/s | 3,754 blocos |

Isso documenta a relação de cadência efetivamente aplicada. Não prova que cada pé esteja imóvel no chão. Root motion não é aplicado ao jogador; deslizamento horizontal residual depende da adaptação das proporções UAL ao Steve e deve ser julgado em 1×. Não afirmamos foot locking ou calibração universal para toda velocidade/terreno.

## Contato dos pés e limites

Altura da sola inclui as camadas externas aprovadas, em relação ao plano da entidade. Valores negativos indicam penetração, positivos afastamento. Mesma janela estável lateral:

| Movimento | Menor Y | Maior Y | Afastamento positivo médio |
|---|---:|---:|---:|
| Walk | −0,03908 | 0,00725 | 0,00073 bloco |
| Jog | 0,05219 | 0,16019 | 0,10270 bloco |
| Sprint | −0,02249 | 0,13385 | 0,06549 bloco |

Walk permanece próximo do plano, com penetração residual da camada externa. Jog mantém a geometria/animação anteriores e ainda tem folga visual relevante. Jog e Sprint possuem fases aéreas legítimas na fonte; a tabela não separa apoio e voo e não deve ser lida como erro médio de contato. Não foi aplicada correção vertical indiscriminada para esconder todos os valores positivos.

A auditoria verifica matrizes finitas, ausência de estiramento novo e restrição da empunhadura nas poses realmente desenhadas. Geometria, UVs e pesos são idênticos aos aprovados. Isso não substitui revisão de interseções pontuais, deslizamento, giro do corpo ou textura durante reprodução.

## Roteiro de aprovação

1. Compare Walk lateral/frontal em 1× e 0,25×; observe ritmo, braços e contato.
2. Compare Sprint nos três ângulos e a passagem de volta para Jog/parada.
3. Em variação de velocidade, observe continuidade dos pés e ausência de reinício no zero.
4. Compare inversão e laterais: avalie a fronteira entre R3 e fallback, ciente de que os clipes direcionais permanecem pendentes.
5. Revise espada equipada, retorno de ataque e retorno do Roll. Observe mãos/arma e tronco.
6. Revise lajes, pulo e agachamento; não há homologação de toda topografia ou estado especial.

Dados: [VISUAL_ANALYSIS.json](../animation_pipeline/reports/m002_a2/VISUAL_ANALYSIS.json), [assets](M002_A2_ANIMATION_ASSETS.md) e [testes/desempenho](TEST_LOG_M002_A2.md). A aprovação estética não foi inferida dos testes automatizados.
