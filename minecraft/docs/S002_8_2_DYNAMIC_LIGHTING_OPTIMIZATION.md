# S002.8.2 — Desempenho da luz dinâmica de equipamentos

## Referência e método

A referência é a S002.8.1 publicada no commit `60c02215beccbac2690bf272fe83141be62f0ee9`. A matriz extrai do Git tanto o ZIP quanto os fontes Java e recursos dessa revisão. As duas versões usam a mesma galeria, os mesmos mapas copiados para instâncias isoladas e oito chunks de distância. O VSync fica desligado e a comparação ligada/desligada altera somente `DYNAMIC_LIGHT`.

A rodada inicial com a GTX 1050 Ti, 1280 × 720 e o preset Equilibrado mediu 194,88 FPS com luz e 271,21 FPS sem luz no corredor estreito. São 5,13 ms e 3,69 ms por quadro, respectivamente, cerca de 1,44 ms adicionais. A execução anterior de 108,96 contra 198,02 FPS usava aquecimento de 65 ticks. A diferença entre as duas amostras motivou rodadas alternadas e janelas maiores.

Use o script abaixo a partir de `minecraft/`. O comando espera uma instância de QA Forge com o mapa de referência e uma pasta de saída ainda livre. A matriz usa 150 ticks de aquecimento e 120 ticks de coleta por cena, com três repetições alternadas de corredor e parede a 720p. Ela registra o framebuffer real, FPS, 1% low, p95, p99, uso de GPU e VRAM quando disponíveis.

```powershell
.\validation\s001\benchmark-equipped-lighting.ps1 `
    -Template '.logs/s002-verified/854x480-F' -ProfileLighting
```

O perfil CPU mede o intervalo dos eventos de início e fim da renderização no cliente. O perfil GPU usa um par de timestamps OpenGL a cada 16 quadros. O leitor recolhe somente resultados prontos. Mede o intervalo da renderização, que inclui mais passes além dos materiais com luz equipada. O perfil da ponte separa coleta de fontes, atualização de colisão e preparação/envio do buffer. Os tempos da ponte no CSV são totais por janela. Divida-os pelos quadros ativos para obter a média por quadro. O tempo de envio mede submissão na CPU. Os timestamps da GPU incluem a transferência no intervalo renderizado.

## Travessia na GPU

Na S002.8.1, cada raio consultava um texel por voxel visitado e decodificava as três coordenadas da caixa com módulo e divisão em ponto flutuante. A S002.8.2 usa `texelFetch`, endereços inteiros e operações de bits para extrair os mesmos limites quantizados. A travessia rejeita segmentos fora da grade antes do laço. A resolução do receptor, a ordem dos passos, o máximo de iterações e o teste de interseção foram preservados.

O experimento com máscaras de ocupação de grupos de 2 × 2 × 2 teve resultados mistos. Em relação à versão com endereços inteiros, a diferença do custo GPU ligado/desligado foi cerca de 0,08 ms menor no corredor e 0,09 ms maior na parede. A comparação inicial também incluía alterações na ponte CPU. O release usa o caminho inteiro mais simples e o formato RGBA32F de 256 × 49 já publicado. A oclusão continua na resolução completa, no passe de materiais.

A variante de diagnóstico com o shader e a ponte da S002.8.1 executou o mesmo cálculo de iluminação com a travessia desativada. No corredor a 720p, ela mediu 250,42 FPS e 3,85 ms de GPU. A diferença reúne o custo combinado da travessia, das leituras da grade e dos testes de caixa.

O experimento removeu somente a condição `!equippedLightVisible(...)` de uma cópia do ZIP de referência. O benchmark aceita um arquivo de teste em `-ShaderPack` e a árvore Java correspondente em `-BaselineSources`. Para a comparação da grade, `-FullCollisionScan -ProfileLighting` solicita a varredura por tick durante a rodada de QA.

## Atualização e envios na CPU

A S002.8.1 percorria o volume de 12.167 células a cada tick com fontes ativas e quando a origem mudava. A grade incremental reaproveita a parte sobreposta ao deslocar a janela e consulta as faixas novas. Atualizações de blocos locais e confirmadas pelo servidor invalidam a célula e sua vizinhança. Eventos de chunks invalidam a coluna afetada e suas bordas. Formas dinâmicas são reavaliadas no tick. A troca de mundo e o recarregamento de tags invalidam o cache. Se os hooks de alteração não estiverem instalados, a atualização retoma a consulta completa por tick.

Alterações nas colisões são enviadas em uma faixa que cobre as linhas afetadas, com uma única chamada. Movimentos grandes ou mudanças dispersas podem exigir todas as linhas. O upload dos metadados das fontes continua separado por frame. OpenGL 4.5 permite escrever diretamente na textura. O caminho anterior de bind e restauração atende os demais contextos.

O A/B da grade usou o mesmo shader experimental, o mesmo formato de upload e duas janelas com 120 ticks de medição. Apenas a varredura completa foi alternada. No corredor, as consultas de volume caíram de 1.460.121 para 189 e o tempo da atualização da colisão caiu de 77,73 para 1,92 ms por janela, cerca de 0,060 para 0,0015 ms por quadro ativo. Na cena com caminhada e lanterna traseira, as consultas caíram de 1.460.058 para 3.757 e a atualização caiu de 77,72 para 4,77 ms por janela. Os processos enviaram 334 linhas de colisão na caminhada e zero no corredor já aquecido. O FPS total variou entre as duas rodadas. O ganho isolado aqui está no trabalho da CPU.

## Qualidade e compatibilidade

A posição, cor, emissão, raio, atenuação e variação da chama continuam usando as fórmulas da S002.8.1. Os presets Leve, Equilibrado e Alto mantêm os limites de uma, duas e quatro fontes. A API `DynamicLightAttachments` e os encaixes do Polymesh R3 seguem o contrato publicado. A lanterna nas costas e o segundo attachment de cinto são cenários de QA para a integração futura. As limitações das caixas envolventes em blocos compostos continuam aplicáveis.

## Resultados a 720p no Equilibrado

Cada valor é a mediana de três rodadas. Os tempos são em milissegundos por quadro. O 1% low é a inversão do tempo médio do 1% de quadros mais lentos.

| Cena | Versão | Luz | FPS | Tempo médio | p95 | p99 | 1% low | GPU |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Corredor | S002.8.1 | Ligada | 212,35 | 4,709 | 7,029 | 9,251 | 86,77 | 4,474 |
| Corredor | S002.8.1 | Desligada | 263,51 | 3,795 | 4,774 | 5,122 | 173,38 | 3,641 |
| Corredor | S002.8.2 | Ligada | 214,83 | 4,655 | 6,386 | 8,049 | 98,46 | 4,461 |
| Corredor | S002.8.2 | Desligada | 263,88 | 3,790 | 5,042 | 5,621 | 162,83 | 3,659 |
| Parede | S002.8.1 | Ligada | 217,44 | 4,599 | 5,866 | 6,526 | 142,62 | 4,460 |
| Parede | S002.8.1 | Desligada | 249,86 | 4,002 | 4,903 | 5,537 | 167,08 | 3,842 |
| Parede | S002.8.2 | Ligada | 218,64 | 4,574 | 5,953 | 6,270 | 126,57 | 4,434 |
| Parede | S002.8.2 | Desligada | 251,22 | 3,981 | 5,365 | 5,723 | 167,32 | 3,838 |

No corredor com luz, a diferença foi de **+2,48 FPS e −0,054 ms por quadro**. O custo adicional em relação à luz desligada passou de 0,914 para 0,865 ms. O p95 caiu 0,643 ms e o p99 caiu 1,202 ms. O p99 da candidata ficou entre 7,828 e 8,246 ms, enquanto o da referência ficou entre 8,628 e 9,687 ms. A diferença de 0,013 ms no tempo médio da GPU é pequena diante da variação entre execuções. Os dados sustentam a melhora na cauda do corredor e na atualização CPU medida isoladamente. Na parede, os ganhos foram menores e o 1% low recuou.

O diagnóstico sem travessia apresentou cerca de 0,62 ms a menos de GPU que a mediana da referência com luz no corredor. A travessia e o teste de caixas respondem pela maior parte do custo adicional observado nesse cenário. O endereçamento inteiro manteve o custo médio da GPU perto da referência.

Na rodada de cenas adicionais, duas lanternas passaram de 185,69 para 189,73 FPS e de 5,385 para 5,271 ms por quadro. A lanterna traseira ficou em 204,62 e 205,69 FPS, respectivamente. Caverna, floresta à noite e vila também tiveram pares com luz ligada e desligada. A divisória abriu essa rodada e mostrou grande variação com a luz ligada e com a luz desligada. É uma amostra de uma única execução por versão.

Na rodada Slim com o preset Alto a 720p, a lanterna traseira sozinha mediu 180,38 FPS e 5,373 ms de GPU. Duas lanternas nas mãos mediram 169,42 FPS e 5,746 ms. Com as duas mãos e os attachments das costas e do cinto, a cena mediu 142,30 FPS e 6,880 ms. Os tempos representam a renderização completa das respectivas cenas. [Quatro fontes](assets/s0028_2/four-sources-slim.png)

| Framebuffer real | Corredor | FPS S002.8.1 | FPS S002.8.2 | Tempo médio S002.8.1 | Tempo médio S002.8.2 | p99 S002.8.1 | p99 S002.8.2 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 854 × 480 | Ligada | 290,27 | 319,83 | 3,445 | 3,127 | 8,891 | 5,926 |
| 854 × 480 | Desligada | 402,50 | 402,29 | 2,484 | 2,486 | 3,715 | 3,739 |
| 1604 × 881 | Ligada | 156,52 | 159,20 | 6,389 | 6,281 | 10,900 | 10,723 |
| 1604 × 881 | Desligada | 196,67 | 195,51 | 5,085 | 5,115 | 6,798 | 7,203 |

As resoluções extremas tiveram uma rodada por versão, também com pares para parede e duas lanternas. A resolução nominal de 1920 × 1080 produziu 1604 × 881 no cliente. O CSV reúne 156 medições, incluindo a matriz, os diagnósticos e os ensaios de equipamento. [Dados brutos e diagnósticos](S002_8_2_DYNAMIC_LIGHTING_BENCHMARK.csv)

[Duas lanternas, S002.8.1](assets/s0028_2/dual-baseline.png) · [Duas lanternas, S002.8.2](assets/s0028_2/dual-optimized.png) · [Coluna, S002.8.1](assets/s0028_2/column-baseline.png) · [Coluna, S002.8.2](assets/s0028_2/column-optimized.png) · [Divisória, S002.8.1](assets/s0028_2/partition-baseline.png) · [Divisória, S002.8.2](assets/s0028_2/partition-optimized.png)

## Comparação visual

Na segunda rodada a 720p, as capturas do corredor e da parede com luz desligada foram idênticas byte a byte entre as versões. Com luz ligada, 57.600 pixels amostrados em cada cena apresentaram diferença absoluta média de 0,068 no corredor e 0,018 na parede, por canal em escala de 0 a 255. A diferença máxima foi de dois níveis por canal em ambas as cenas. Essas capturas incluem a chama animada.

A comparação com uma lanterna estática na mão principal produziu o mesmo PNG nas duas versões. As capturas em terceira pessoa mostram poses diferentes por terem sido coletadas em instantes distintos da animação. [Lanterna na mão, S002.8.1](assets/s0028_2/lantern-main-baseline.png) · [Lanterna na mão, S002.8.2](assets/s0028_2/lantern-main-optimized.png) · [Lanterna nas costas, S002.8.1](assets/s0028_2/back-baseline.png) · [Lanterna nas costas, S002.8.2](assets/s0028_2/back-optimized.png)

[Corredor com luz, S002.8.1](assets/s0028_2/corridor-baseline-on.png) · [Corredor com luz, S002.8.2](assets/s0028_2/corridor-optimized-on.png) · [Corredor sem luz, S002.8.1](assets/s0028_2/corridor-baseline-off.png) · [Corredor sem luz, S002.8.2](assets/s0028_2/corridor-optimized-off.png)

O teste numérico da parede em pedra manteve diferença RGB máxima zero nos 3.048 pontos atrás do obstáculo. A face frontal ganhou 14,26 níveis de vermelho em média. O vidro transmitiu luz nos pontos de fundo e a tocha no inventário manteve diferença zero. Os limiares da S002.8.1 foram usados diretamente.

[Parede com luz, S002.8.1](assets/s0028_2/wall-baseline-on.png) · [Parede com luz, S002.8.2](assets/s0028_2/wall-optimized-on.png) · [Parede sem luz, S002.8.1](assets/s0028_2/wall-baseline-off.png) · [Parede sem luz, S002.8.2](assets/s0028_2/wall-optimized-off.png) · [Vidro com luz](assets/s0028_2/glass-on.png) · [Vidro sem luz](assets/s0028_2/glass-off.png)

No teste incremental, o servidor removeu a parede e a colocou de volta com o jogador parado. A abertura elevou o vermelho médio dos 3.048 pontos de fundo em 0,688 nível. Após recolocar a parede, a amostra voltou a corresponder exatamente à imagem inicial. A ponte registrou 342 consultas de células e enviou 44 linhas de colisão durante a janela completa. [Parede inicial](assets/s0028_2/wall-toggle-3.png) · [Abertura](assets/s0028_2/wall-toggle-24.png) · [Parede restaurada](assets/s0028_2/wall-toggle-51.png)

## Regressões

| Suíte | Resultado |
| --- | --- |
| Equipamentos e obstáculos, Classic | Passou pelas cenas de pedra, madeira, portas abertas e fechadas, slabs, escadas, grades, cercas, alçapões, folhas, troca de mão e remoção do item. |
| Equipamentos, Slim | Passou pelas cenas de uma, duas e quatro fontes no Alto, pela troca de mão e pela caminhada com troca de perspectiva. |
| Materiais e ambiente | Completou 12 cenas de renderização, com LabPBR, água submersa, lava, aurora, vento, chuva, neve, Nether e End. |
| Câmera no túnel | Registrou opacidade de 0,12 e composição do personagem. |
| Anti-aliasing | Executou a cena em movimento tanto com SMAA quanto com FXAA. |
| Parkour e combate | 30 verificações aprovadas, incluindo deslocamento, clips de parede e armaduras A001. |
| Lanterna traseira em combate | Classic e Slim confirmaram um roll completo no servidor e a animação do ataque de espada no cliente, ambos com o attachment ativo. [Roll Classic](assets/s0028_2/back-roll-classic.png) · [Ataque Slim](assets/s0028_2/back-attack-slim.png) |
| Câmera, armaduras e Polymesh | 95 verificações aprovadas em Classic e 95 em Slim, incluindo natação. |
| Build | `gradlew --offline build` e compilação standalone da galeria concluíram com sucesso. O JAR contém a grade e o hook de atualização. As classes de QA ficam na configuração de desenvolvimento. |
| Inicialização | `runClient` e `runClient -PtoeNoOculus` abriram o menu e encerraram com código zero. |

## Limites e próximos experimentos

Os testes cobrem o hardware e o framebuffer registrados. Um buffer de colisão inteiro de 16 bits por canal comportaria as coordenadas quantizadas e exigiria outro caminho para os metadados em ponto flutuante. Seu custo de acesso e troca de samplers fica em aberto. A matriz renderizou a oclusão em resolução integral. Em um piso contínuo, a profundidade do receptor pode ser praticamente igual nos dois lados de uma borda de sombra. Um protótipo com resolução reduzida precisaria usar também a visibilidade da fonte e passar pela mesma amostragem atrás da parede.

Uma próxima rodada pode medir a travessia isoladamente nos passes de materiais e repetir o A/B em outros drivers. Os dados atuais derivam de timestamps do intervalo de renderização e das variantes de diagnóstico.

## ZIP instalável

Os 67 arquivos do ZIP correspondem aos do shaderpack editável. O SHA-256 do pacote e da cópia instalada pelo build de desenvolvimento é `3AE4F0D0C409FD6E2FE38C3C9F8765AAF2AC757BBAEE999E3CD1BF73E081ADB0`. [Baixar o ZIP](../shaderpacks/Tower%20of%20Entropy%20-%20Fantasy%20Foundation.zip)
