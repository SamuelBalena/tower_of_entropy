const fs=require('fs'),path=require('path');const root=path.resolve(__dirname,'../..'),r=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R3_QA_RESULTS.json'))),raw=JSON.parse(fs.readFileSync(path.join(root,'.logs/qa-results.json')));
const f=(x,n=2)=>Number.isFinite(x)?x.toFixed(n):'indisponível',row=(c,p)=>r.rows.find(x=>x.clip===c&&x.profile===p),range=a=>f(Math.min(...a),0)+'–'+f(Math.max(...a),0),roll3=row('roll','r3'),roll31=row('roll','r31'),deltaFrame=roll31.meanFrameMs-roll3.meanFrameMs,deltaFps=100*(roll31.meanFps/roll3.meanFps-1),deltaDraw=roll31.meanDrawCpuMs-roll3.meanDrawCpuMs;
const table=r.rows.map(x=>`| ${x.clip} | ${x.profile==='r31'?'R3.1':'R3'} | ${f(x.meanFps,1)} | ${f(x.onePercentLowFps,1)} | ${f(x.meanFrameMs,3)} | ${f(x.meanDrawCpuMs,3)} | ${f(x.meanGpuTimestampSpanMs,3)} |`).join('\n');
const variation=r.rows.map(x=>`| ${x.clip} | ${x.profile==='r31'?'R3.1':'R3'} | ${range(x.fpsRange)} | ${range(x.lowRange)} | ${x.totalGcMs} | ${x.totalJitCompileMs} |`).join('\n');
let report=`# M002.R3.QA — Validação visual e desempenho

13/11/2026. **Recomendação: manter R3 como referência do laboratório, com R3.1 opcional para revisão estética.** A R3.1 recupera espessura dos cotovelos/joelhos, mas mantém o contato coxa/abdômen e custa mais CPU no Roll. A adoção definitiva não está justificada apenas pelo desempenho deste cenário. Correções pontuais, em uma futura decisão autorizada, parecem mais proporcionais que promover toda a camada atual. Nenhuma correção foi implementada neste QA; CPM permanece padrão.

## Resultados quantitativos

Três repetições por combinação, 10 segundos medidos cada, com ordem alternada. Agregação dos intervalos reais entre frames das três janelas; FPS médio = 1000 / tempo médio. **1% low** = 1000 / média do 1% de intervalos mais lentos (arredondado para cima); não é o inverso do percentil 99 nem a média de FPS instantâneos.

| Animação | Variante | FPS médio | 1% low FPS | Quadro médio ms | Desenho CPU ms/frame | Intervalo GPU ms |
|---|---|---:|---:|---:|---:|---:|
${table}

No Roll: diferença de **${f(deltaFps,1)}% no FPS médio**, **+${f(deltaFrame,3)} ms por quadro** e **+${f(deltaDraw,3)} ms no trecho CPU de desenho**. O custo extra médio de quadro corresponde a ${f(100*deltaFrame/(1000/60),2)}% de um orçamento de 16,67 ms; isso não prevê a perda de FPS em um mundo pesado ou em outra resolução. Neste cenário leve há ampla folga para 60 FPS nas duas variantes. O delta de quadro acompanha de perto o delta CPU de desenho, consistente com custo adicional do skinning/morph, sem substituir um profiling completo.

| Alternativa | Avaliação |
|---|---|
| Manter R3 | Recomendado agora como referência: menor custo de Roll, comportamento conhecido; preserva o achatamento já documentado. |
| Adotar R3.1 integralmente | Melhora o volume e é viável a 60 FPS neste cenário, mas cobra custo mensurável e não resolve o contato central. Manter apenas como opção de avaliação até aprovação estética. |
| Correções pontuais | Direção proporcional para uma futura decisão: concentrar o ganho nos cotovelos/joelhos e revisar o contato, buscando menor custo. Nenhuma dessas alterações foi feita neste QA. |

### Repetições e variabilidade

| Animação | Variante | Faixa FPS médio, 3 janelas | Faixa 1% low | GC total ms | JIT total ms |
|---|---|---:|---:|---:|---:|
${variation}

Idle, Jog e Sword usam o **mesmo GLB R3** nas duas variantes. Diferenças nesses controles são variação do ensaio, não ganho da malha R3.1. O 1% low é sensível a pausas do processo e ao desktop; não se atribui cada diferença à malha. A interpretação principal combina repetição, custo CPU e comportamento dos controles.

## Condições e instrumentação

- Windows 11; Intel Core i5-10400F @ 2,90 GHz, 12 processadores lógicos; ${r.metadata.glRenderer}.
- OpenGL ${r.metadata.glVersion}; Java ${r.metadata.java}; Minecraft 1.20.1, Forge 47.4.28, Polymesh 1.0.0 e CPM já existentes.
- Resolução real ${r.metadata.resolution.join('×')}; render/simulação ${r.metadata.renderDistance}/${r.metadata.simulationDistance} chunks; gráficos ${r.metadata.graphics}; mesmas opções de nuvens, partículas, FOV, skin e espada. VSync desligado; opção de FPS 260, que corresponde ao modo sem limitação efetiva neste cliente. FPS são quadros produzidos pela aplicação, não taxa física do monitor.
- Cópia isolada em run-qa/saves/TOE_QA: plataforma, posição fixa, terceira pessoa, câmera fixa, corpo lateral a 90° para os benchmarks, luz diurna congelada e sem mobs próximos. Jog é executado no lugar. Ataque é visual; não há disparos de combate ou física de Roll.
- Aquecimento global de **${r.metadata.warmupSeconds} s**, exercitando os oito casos quatro vezes, mais 3 s descartados em cada troca. Três rodadas, ordem R3/R3.1, R3.1/R3 e R3/R3.1, total de 24 janelas e 240 s de medição. Roll/Sword repetem seus clipes a velocidade original para medir custo contínuo, sem pausa de Idle entre ciclos.
- Intervalos de quadro: relógio monotônico entre RenderTick.START consecutivos. Trecho CPU de desenho: contador existente em torno de corpo/arma. A instrumentação não muda os assets e é idêntica para as variantes.
- GPU: timestamps OpenGL assíncronos no início/fim do trecho RenderTick, sem esperar resultados sincronicamente. Medem intervalo na timeline da GPU para esse trecho, podendo incluir espera de submissão; **não são utilização de GPU, tempo exclusivo de shaders ou latência de apresentação**. CPU de processo e duração do evento também estão em RESULTS.json; a porcentagem de processo é normalizada a um núcleo e inclui servidor integrado/JIT.
- Não foram medidas utilização física da GPU, VRAM, potência, temperatura, memória residente nem latência input-to-photon. Não se inventaram esses números.

### Aquecimento e compilação

O piloto com 48 s de aquecimento revelou compilação JIT durante medições (3.546 ms acumulados no primeiro Roll R3). Seus números foram descartados da tabela final. O cliente foi reexecutado com 192 s de aquecimento, medindo novamente todas as combinações. A tabela de variabilidade registra o JIT residual real; não se esconde eventual compilação remanescente e nenhuma janela final foi excluída depois de ver os resultados. Gradle, carregamento de mundo, capturas PNG e gravação dos resultados ficam fora das janelas medidas.

Este é um ensaio de desenvolvimento em um desktop, não um laboratório com clocks/temperatura fixos ou processos externos suspensos. A resolução baixa e o cenário simples destacam custo de CPU; os valores absolutos não representam desempenho de exploração/combate em um mundo completo. Os tempos de GPU são uma métrica auxiliar e não sustentam uma conclusão de utilização.

## Comparação visual

Pares abaixo são PNGs nativos selecionados do ensaio R3.1 aprovado tecnicamente, preservados sem edição. Dentro de cada par, câmera, instante, skin, luz e ângulo são iguais. São evidências existentes selecionadas para o QA; o replay a 1× foi capturado novamente neste benchmark, com sua própria vista frontal ligeiramente elevada.

| Pose | R3 | R3.1 |
|---|---|---|
| Idle, frente | ![R3 Idle](M002_R3_QA_EVIDENCE/images/r3-idle-0.png) | ![R3.1 Idle](M002_R3_QA_EVIDENCE/images/r31-idle-0.png) |
| Jog, lateral | ![R3 Jog](M002_R3_QA_EVIDENCE/images/r3-jog-90.png) | ![R3.1 Jog](M002_R3_QA_EVIDENCE/images/r31-jog-90.png) |
| Sword, lateral | ![R3 Sword](M002_R3_QA_EVIDENCE/images/r3-sword-270.png) | ![R3.1 Sword](M002_R3_QA_EVIDENCE/images/r31-sword-270.png) |
| Roll, quadro 4 / 0,100 s, cotovelo | ![R3 cotovelo](M002_R3_QA_EVIDENCE/images/r3-roll-f4-0.png) | ![R3.1 cotovelo](M002_R3_QA_EVIDENCE/images/r31-roll-f4-0.png) |
| **Roll, quadro 23 / 0,733 s, coxa/abdômen, frente** | ![R3 contato](M002_R3_QA_EVIDENCE/images/r3-roll-f23-0.png) | ![R3.1 contato](M002_R3_QA_EVIDENCE/images/r31-roll-f23-0.png) |
| Mesmo contato, lateral | ![R3 contato lateral](M002_R3_QA_EVIDENCE/images/r3-roll-f23-90.png) | ![R3.1 contato lateral](M002_R3_QA_EVIDENCE/images/r31-roll-f23-90.png) |
| Roll, quadro 26 / 0,833 s, joelho | ![R3 joelho](M002_R3_QA_EVIDENCE/images/r3-roll-f26-90.png) | ![R3.1 joelho](M002_R3_QA_EVIDENCE/images/r31-roll-f26-90.png) |

R3.1 preserva melhor a seção dos membros, reduzindo o aspecto fino/achatado em flexões fortes. O contato na região central permanece; recuperar espessura pode até torná-lo mais evidente. Idle/Jog/Sword mantêm a aparência anterior, pois usam exatamente a mesma geometria R3. A espada acompanha a mão e a skin continua reconhecível; não houve nova malha neste QA.

### Reprodução a velocidade normal

[Abrir reprodução lado a lado a 1×](M002_R3_QA_EVIDENCE/PLAYBACK.html). Foram capturados ${r.playback.captures} PNGs no cliente, ${r.playback.profiles.map(p=>p.frames+' em '+p.profile).join(' e ')}, fora da medição. Cada sequência cobre aproximadamente 0 a 1,43 s do Roll original de 1,467 s. O quadro final não é mantido por um intervalo extra. O viewer usa timestamps da captura e permite pausar em 0,733 s; não mede FPS do jogo.

As sequências mostram o agrupamento/contacto em vários frames consecutivos, aproximadamente de 0,63 a 0,87 s. **Inferência temporal:** o defeito persiste por cerca de 0,23 s e pode ser percebido a 1× na vista frontal; não é restrito a uma única imagem congelada. Isso não equivale a um teste perceptual com pessoas. A revisão foi feita sobre os PNGs e seus timestamps; a experiência subjetiva a 1× e a aprovação estética definitiva ficam com o usuário. Não se afirma ter realizado essa aprovação humana.

## Preservação e reprodução

${r.preservationFiles} hashes de fontes, modelos, scripts e configurações anteriores conferidos. O inventário Godot também é verificado ao fechar o QA. Nenhum arquivo de combate/animação original foi alterado, nenhum modelo foi reconstruído e CPM continua padrão. Somente perfil QA, instrumentação e documentos foram adicionados. O cenário/options isolados são descartáveis; configurações run-r3 e run-r31 foram preservadas.

Para reproduzir, na pasta minecraft: <code>.\\scripts\\run-qa.ps1</code>. Logs/intervalos individuais ficam em .logs/qa-results.json; o piloto em .logs/qa-pilot-results.json. Resumo versionável: animation_pipeline/reports/M002_R3_QA_RESULTS.json. O build final normal deve estar sem classes QA/renderer experimental; a inspeção é registrada no encerramento.

O pacote compartilhável contém este relatório, 14 PNGs estáticos, ${r.playback.captures} PNGs da sequência, viewer offline e RESULTS.json. Não inclui GLB, modelos CPM, clipes de terceiros ou dependências. Não foi iniciada outra etapa.

## Encerramento

Build normal limpo concluído. ${r.normalJarAudit?`${r.normalJarAudit.entries} entradas e ${r.normalJarAudit.bytes} bytes no JAR; inspeção confirmou exclusão de classes QA, renderers experimentais e assets privados. SHA-256: ${r.normalJarAudit.sha256}.`:''} Os 171 arquivos do inventário Godot e os ${r.preservationFiles} hashes aprovados foram conferidos. Nenhum cliente ficou aberto. A aprovação estética permanece pendente com o usuário.
`;
fs.writeFileSync(path.join(root,'docs/M002_R3_QA_REPORT.md'),report);fs.writeFileSync(path.join(root,'docs/M002_R3_QA_EVIDENCE/REPORT.md'),report.replaceAll('M002_R3_QA_EVIDENCE/',''));console.log('QA report generated',r.rows.length,'rows');
