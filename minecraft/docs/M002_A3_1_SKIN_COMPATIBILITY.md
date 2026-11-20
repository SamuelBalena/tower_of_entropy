# M002.A3.1 — Classic e Slim

## Derivação

O Classic aprovado não foi remodelado. SHA-256 do GLB A3: `C6050564F0B1AB138C331ED424FC3D4E04FFE01E7C4C8A68C35202504DA55E80`.

`package-a3-slim.cjs` cria outro arquivo, `steve_a3_slim.glb`. Só altera atributos POSITION e TEXCOORD_0 de `limb_2`, `limb_2_outer`, `limb_3` e `limb_3_outer`. Os braços internos passam de 4 para 3 pixels; mangas externas mantêm inflação de 0,25 pixel em cada lado, totalizando 3,5 pixels. Altura, profundidade e subdivisões não mudam.

Os UVs 64×64 são remapeados por face: frente e costas passam a 3 pixels, laterais continuam com 4, tampas passam a 3×4. São mantidas as origens oficiais das ilhas de braço/manga de cada lado. Não se comprime a textura inteira nem se amostra uma ilha Classic de quatro pixels em um braço Slim.

Nodes, bind matrices, índices, pesos, materiais, textura incorporada de referência e canais dos 30 clipes permanecem iguais. O shader recebe a textura real do jogador, não força Steve/Alex. Nenhum sampler ou original UAL/Archer é editado.

## Esqueleto e itens

A variante reutiliza o rig articulado R3. Não cria um segundo retargeting ou muda os pivôs das animações aprovadas. O estreitamento é dirigido para dentro da largura anterior, preservando a borda próxima ao tronco e a continuidade dos anéis de cotovelo. O ajuste não pretende reproduzir cada deslocamento de pivô do renderer rígido vanilla: conserva as articulações do R3.

O centro visual da mão muda meio pixel para dentro. Somente na variante Slim, o item acompanha esse deslocamento no referencial do antebraço; o arco usa a posição de empunhadura correspondente. Transformações Classic e propriedades dos itens permanecem iguais. Os limites de mira/movimento do arco e o fallback de equipamento são herdados do A3.

## Seleção e segurança

`default` seleciona Classic e `slim` seleciona a derivada, automaticamente a cada desenho. A troca de skin reconstrói o sampler/estado visual do modelo apropriado. Recursos ausentes deixam o desenho para CPM/vanilla. Exceção Slim bloqueia só essa variante até reativação/reconexão; não substitui o Classic por uma malha defeituosa.

Primeira pessoa e equipamento não suportado continuam no caminho de compatibilidade. Os perfis históricos A2/R3 anteriores não passam a ganhar Slim retroativamente. Nenhum modelo de usuário é sobrescrito.

## Validação

`animation_pipeline/reports/m002_a3_1/PRESERVATION.json` confere larguras, preservação binária fora dos atributos autorizados, mesmos clipes/rig, código de combate e 171 arquivos originais Godot. A diferença binária do derivado limita-se a 524 bytes de posições/UVs de braços.

Os testes usam primeiro a skin natural atribuída pelo cliente, depois texturas vanilla Classic/Slim injetadas **somente no driver de QA** para comparação determinística. Produção não injeta essas texturas. Capturas e resultados ficam na pasta `m002_a3_1`.

Renderização e integridade técnica não constituem aprovação estética. Skins personalizadas arbitrárias, camadas com transparência incomum, resource packs e multiplayer remoto não receberam homologação abrangente. A avaliação estética final permanece com o usuário.
