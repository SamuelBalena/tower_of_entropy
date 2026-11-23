# M003.E1.1 — Precisão e recuperação no Tower Animation Studio

Abra o Studio com **F8** ou **`/toe_animstudio`** dentro do mundo. Selecione a articulação na lista ou no modelo. As ligações realçadas mostram os descendentes que acompanham a rotação.

## Rotação e vistas

| Modo anatômico | Movimento do braço |
| --- | --- |
| Azul, Frente/Trás | Positivo levanta à frente. Negativo leva para trás. |
| Verde, Abrir/Fechar | Positivo afasta o braço do tronco em ambos os lados. Negativo aproxima. |
| Vermelho, Torção | Gira em torno do comprimento do braço na pose corrente. |

**Modo** alterna entre o referencial anatômico e os eixos locais X (vermelho), Y (verde) e Z (azul). Cotovelos e joelhos apresentam apenas a flexão no modo anatômico. As articulações do tronco e da cabeça apresentam inclinação e giro. As pernas têm controles de movimento frontal, abertura e torção.

Arraste sobre o anel. Os arcos atrás do pivô aparecem mais fracos e o anel ativo permanece realçado. O Inspector mostra os ângulos do modo atual e aceita valores em graus com **Enter**. **Snap** percorre livre, 1°, 5° e 15°. Segure **Shift** para reduzir a sensibilidade do arraste. **Esc** restaura a pose anterior durante a operação. Ao soltar o botão, o gesto registra uma ação de histórico e, com Auto Key, registra uma chave no quadro corrente. Use **Ctrl+Z** para desfazer e **Ctrl+Y** ou **Ctrl+Shift+Z** para refazer.

**Câmera** abre as opções Livre, Frente, Costas, Esquerda, Direita e Superior. Arraste com o botão direito para orbitar. Use a vista lateral para conferir o movimento frontal do braço e a vista de frente para conferir a abertura. Use **Foco** para centralizar a articulação.

A M003.E1 desenhava linhas de eixo a partir da articulação e transformava diretamente `dx - dy` em ângulo. Esse cálculo dava a mesma resposta ao movimento diagonal do cursor em qualquer plano de rotação. Na M003.E1.1, o anel amostra um círculo no plano perpendicular ao eixo e o projeta com a câmera do viewport. O arraste mede a diferença assinada dos ângulos no plano a partir do clique inicial e acumula incrementos ao cruzar ±180°. Para a vista quase de perfil do anel, o movimento usa a tangente projetada no ponto selecionado.

O Studio infere a vertical a partir de quadril e tronco, a lateral a partir dos ombros e a frente pelo produto vetorial. Ele usa o vetor da articulação ao filho para definir o comprimento de cada membro. Os eixos são convertidos ao espaço local pela inversa da matriz global da pose de referência. A rotação parte do quaternion do início do gesto e entra na avaliação normal da hierarquia.

| Articulações | Referência anatômica |
| --- | --- |
| Braços e pernas | Azul perpendicular ao comprimento e à frente, verde perpendicular ao comprimento e à lateral externa e vermelho ao longo do membro. O eixo verde tem sinal espelhado entre os lados. |
| Cotovelos e joelhos | Flexão azul no plano sagital do membro. O sinal positivo aponta na direção da flexão de cada tipo de junta. |
| Cabeça, tronco, cintura e quadril | Vermelho em torno da vertical, verde em torno da frente e azul em torno da lateral. |

## Projetos e recuperação

O nome e o indicador de alterações aparecem no editor. **Salvar** substitui o projeto ativo após confirmação. **Salvar +** grava outro arquivo com nome livre. O status informa a conclusão ou o erro. **Abrir pasta** na biblioteca leva a `config/towerofentropy/animation_studio/projects/` da instância.

A biblioteca mostra variante, duração e FPS. **Duplicar** cria um arquivo editável. **Importar** valida o JSON externo e registra uma cópia com nome livre. **Exportar** escreve uma cópia JSON em um destino novo. **Excluir** pede confirmação. O Studio preserva projetos incompatíveis na pasta.

Antes de substituir conteúdo, o Studio mantém a versão anterior daquele arquivo em `.backups/`. O rascunho automático usa `.recovery/`, com intervalo de 15 segundos durante a edição pendente e uma gravação por vez. O rascunho inclui a pose pendente do quadro corrente. Na abertura, **Recuperar** carrega uma cópia editável para salvar com outro nome. **Ignorar** mantém o rascunho disponível pela biblioteca e **Descartar** o remove.

O schema 1 continua usando os identificadores de osso, as poses locais, os quaternions e as curvas existentes. Em modo local, o Inspector compõe `Rx · Ry · Rz`. Em modo anatômico, ele faz a mesma composição na base do osso e converte o resultado de volta ao espaço local. Os ângulos Euler admitem representações equivalentes, especialmente perto de ±90°. Os anéis foram calibrados para o esqueleto R3 Classic e Slim. As opções de vista usam a projeção ortográfica do viewport. O JSON exportado destina-se à troca de projetos entre editores.

## Validação no Minecraft

Execute a partir de `minecraft/`, fornecendo um modelo de instância de QA que contenha o mundo `Global Survival one`. O roteiro copia esse mundo para diretórios isolados e inicia `runClient` nas duas configurações.

```powershell
.\gradlew.bat --offline build
.\validation\m003e1\verify-studio.ps1 -Template '.logs/s002-verified/854x480-F' -RunRoot '.logs/m003e11-studio'
```

`StudioProjectCases` verifica os ângulos ±15°, ±45°, 90° e composições de três eixos por equivalência de quaternions. Os casos geométricos cobrem o cruzamento de ±180°, mais de uma volta e o modificador de precisão. No Forge, o teste passa o arraste pelo `MouseHandler` e confere o deslocamento das articulações nas matrizes que alimentam o skinning. Ele distingue o avanço e recuo dos braços, a abertura espelhada e a torção que preserva a posição do cotovelo. Também testa o cancelamento e a alternância de modo.

O fluxo gera `teste_bracos_precisao` em Classic com Oculus e Slim sem Oculus. O braço direito tem cinco chaves em 60 quadros a 30 FPS, da pose relaxada até a pose combinada e de volta ao repouso. A reprodução passa a 1× e a 0,25×. A suíte também reabre o `teste_aceno` da M003.E1 e verifica as chaves antes de salvar novamente. Os casos de persistência cobrem conflito de nome, falha de escrita, backup, importação, exportação, duplicação, exclusão confirmada e recuperação após reabrir o Studio. [Projeto Classic teste_bracos_precisao](../validation/m003e1/examples/teste_bracos_precisao.json)

| Captura do framebuffer do Minecraft | Classic com Oculus | Slim sem Oculus |
| --- | --- | --- |
| Braço direito à frente, vista esquerda | [Classic](assets/m003_e1_1/classic-braco-direito-frente.png) | [Slim](assets/m003_e1_1/slim-braco-direito-frente.png) |
| Braço esquerdo aberto, vista traseira | [Classic](assets/m003_e1_1/classic-braco-esquerdo-abertura.png) | [Slim](assets/m003_e1_1/slim-braco-esquerdo-abertura.png) |
| Controles anatômicos em vista orbital | [Classic](assets/m003_e1_1/classic-anatomico-orbital.png) | [Slim](assets/m003_e1_1/slim-anatomico-orbital.png) |
| Ambos os braços para trás, vista direita | [Classic](assets/m003_e1_1/classic-bracos-para-tras.png) | [Slim](assets/m003_e1_1/slim-bracos-para-tras.png) |
| Pose combinada com torção e cotovelos flexionados | [Classic](assets/m003_e1_1/classic-bracos-torcao.png) | [Slim](assets/m003_e1_1/slim-bracos-torcao.png) |

O build offline passou. Parkour e combate passaram as 30 verificações após reexecutar a suíte com a configuração de câmera da instância de QA já migrada. A primeira execução parou por um bloqueio de arquivo do Windows durante a migração do Forge.

A suíte de câmera, armaduras e Polymesh passou nas 95 verificações do Classic e nas 95 do Slim, incluindo o roll. Duas rodadas do Slim tiveram uma direção cardinal ausente em cada amostra de nado. O cálculo existente incluiu zero no denominador nesses casos e sinalizou `diagonal_speed`. Uma rodada em nova instância, com VSync desativado e limite de 144 FPS, registrou as oito direções e obteve razão diagonal/cardinal de 1,0215, dentro da tolerância de 10%.

Na suíte de iluminação, a diferença RGB máxima atrás da parede opaca foi zero, o vidro transmitiu luz e a tocha em um slot comum de inventário produziu diferença zero. O cenário da lanterna nas costas gerou capturas com a opção ativada e desativada.

A aprovação visual e de usabilidade fica com o usuário no Minecraft.
