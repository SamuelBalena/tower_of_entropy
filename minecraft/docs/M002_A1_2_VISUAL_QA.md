# M002.A1.2 — QA visual comparativo

13/11/2026. Melhorias verificadas dentro do Minecraft/Forge, com Polymesh R3. A aprovação estética final é do usuário.

Abra [COMPARATOR.html](../animation_pipeline/reports/m002_a1_2/COMPARATOR.html) no navegador local. Escolha o mesmo movimento e ângulo nos dois painéis; deixe **Antes A1.1** à esquerda e **Depois A1.2** à direita. Há reprodução 1×, 0,5×, 0,25× e 0,1×, avanço por quadro e marcas de troca de clipe/impacto. Não requer servidor, biblioteca externa ou instalação.

## Evidência e alinhamento

São 5.460 PNG reais, 60 sequências: dez roteiros × três ângulos × duas versões. Frontal, lateral e traseiro usam o mesmo cenário, iluminação fixa diurna e parâmetros de câmera. Os roteiros incluem combo completo/retorno Idle, pesado/retorno Idle, C → Jog, pausa/reinício, cancelamento de cada leve/pesado, Jog → pesado e pesado → Jog.

A versão anterior usa o caminho legado preservado pelo parâmetro QA `toe.a12.legacy`, com os mesmos assets. A captura ocorre a 20 Hz; o comparador alinha o início do primeiro ataque pelo relógio registrado. A precisão temporal é de aproximadamente 50 ms, não identidade subquadro entre JVMs. Câmera lenta repete PNG existentes, sem gerar quadros. Captura por tick pode mostrar o último frame renderizado antes do estado mais recente do servidor. Marcas são **tentativas de impacto**, não necessariamente dano aplicado.

As capturas posteriores usam a configuração final de 120/140/60 ms, antes da otimização do caminho estável. O cliente final foi comparado ao compositor dessas capturas em 1.183 quadros: erro máximo de matriz 7,75×10⁻⁷, anchor idêntico e correção vertical com diferença máxima de 1,64×10⁻⁷ bloco. Portanto a otimização preserva numericamente as poses publicadas. Evidência: `EQUIVALENCE.json`.

## Resultados observáveis

| Transição, frontal | Maior rotação entre ossos, primeiro intervalo capturado: antes → depois | Maior passo nos primeiros 200 ms: antes → depois |
|---|---:|---:|
| Idle → pesado | 176,62° → 11,06° | 176,62° → 105,86° |
| Jog → pesado | 174,01° → 13,33° | 174,01° → 131,18° |
| C → Jog | 111,81° → 10,56° | 111,81° → 46,83° |
| Pesado → Jog | 107,28° → 10,31° | 107,28° → 45,71° |

São máximos de orientação dos ossos entre amostras de cerca de 50 ms, não velocidade angular constante nem uma nota estética. A entrada abrupta diminuiu nos três ângulos. O movimento original pesado continua rápido depois da entrada: não afirmamos eliminar todo pico ou interseção.

A/B/C mantêm as curvas originais: 6.216 rotações comparadas na mesma fase do GLB, erro máximo 0,0000968°. Diferenças de picos entre capturas não provam mudança de velocidade: por exemplo A → B frontal registra 27,46° antes e 34,19° depois por diferença da fase amostrada. Não foi mantido crossfade genérico entre leves.

Nos 12 cancelamentos (quatro ações × três ângulos), a sequência posterior vai diretamente do ataque para Roll, sem Idle intermediário. As poses do primeiro Roll também foram comparadas ao antigo Idle, com diferença de 87–157°, confirmando alteração real da pose, não somente do nome do clipe. O preview pendente pode ficar em tempo zero até o callback original CPM; física e proteção não foram antecipadas.

## Pés, empunhadura e deformações

| Apoio estabelecido, fonte indicando chão | Afastamento positivo médio antes | Depois |
|---|---:|---:|
| Sword B | 0,20602 bloco | 0,00341 bloco (−98,35%) |
| Sword C | 0,01422 bloco | 0 positivo; sola entre −0,05379 e −0,00484 bloco |

Filtro: fase ≥120 ms, confiança >0,99 e blend concluído. B: 76/77 amostras antes/depois; C: 124/127. O valor negativo de C evidencia penetração residual da camada externa; **não significa contato perfeito**. Em B o mínimo posterior foi −0,00868 e o máximo 0,01876 bloco. Nos 18 quadros aéreos de C o resíduo de correção ficou abaixo de 0,000513 bloco; o salto não foi preso ao chão.

Na inspeção das capturas e no comparador, o apoio vertical de B fica mais próximo do plano e o retorno à locomoção perde a troca instantânea de pose. O movimento horizontal dos pés continua vindo do clipe; não há bloqueio de pés ou root motion físico. Terreno irregular e todas as possíveis interseções ainda exigem avaliação em gameplay.

As 2.730 amostras posteriores contêm matrizes finitas. Erro máximo de escala: 0,00000195; comprimento de ossos: 0,000000611; restrição da empunhadura: 0,000000270. Não houve geometria, UV ou pesos novos. Essas verificações afastam estiramento introduzido pelo blend, mas não certificam ausência de interseções em todo instante contínuo.

## Roteiro de aprovação estética

1. Em 1× e depois 0,25×, compare pesado frontal/lateral desde aproximadamente 0,5 s; avalie entrada e giro completo, não apenas o primeiro quadro.
2. Compare `Jog → pesado`, `C → Jog` e `Pesado → Jog`, observando ombros, tronco e continuidade dos pés.
3. Compare combo lateral durante B e o giro/salto de C; verifique se o abaixamento visual parece natural e se a penetração residual é aceitável.
4. Compare os quatro cancelamentos, principalmente C → Roll. Observe a mistura curta e eventual espera em pose zero.
5. Confirme a empunhadura em todos os ângulos e o retorno para Idle após recuperação.

Os fallbacks mantêm suas limitações anteriores; não há homologação estética dos novos clipes em CPM/R3.1, multiplayer remoto ou terreno irregular. Nenhuma aprovação estética foi inferida dos testes automatizados.

Dados detalhados: [COMPARISON.json](../animation_pipeline/reports/m002_a1_2/COMPARISON.json), [desempenho](../animation_pipeline/reports/m002_a1_2/PERFORMANCE.json), [testes](TEST_LOG_M002_A1_2.md).
