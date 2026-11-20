# M002.A3 — Compatibilidade e procedência

13/11/2026. A2 foi conferido como tecnicamente concluído antes deste lote. Seus arquivos, quatro ciclos, controle por movimento real e fase compartilhada foram reaproveitados. Nenhum arquivo Godot foi editado.

## Fontes verificadas

UAL1/UAL2 Standard locais usam CC0 1.0, conforme seus `License.txt`, autoria Quaternius. Foram mantidos os hashes conhecidos de ambos os GLBs. Originais continuam em `animation_pipeline/input/UAL1` e `UAL2`; seus derivados novos ficam apenas em `converted/m002_a3`.

Human Archer FREE é de **Kevin Iglesias**, não CC0. Foram verificados o PDF local e a [documentação oficial do pacote](https://www.keviniglesias.com/documentation/Human%20Archer%20Animations%20FREE.pdf), que remete à Standard Asset Store EULA, permite uso comercial e proíbe revenda isolada. A [página oficial Godot/Unreal](https://kevdev.itch.io/human-archer-animations-free) identifica as fontes Blender e os movimentos incluídos. A utilização aqui é incorporada ao personagem do mod, em desenvolvimento local; FBX, rig humano, PDF e GLBs intermediários não são incluídos como biblioteca pública de assets. Os termos próprios desse pacote não são substituídos pela licença do código do mod.

O ZIP Blender de referência foi copiado para a área ignorada e somente o arquivo masculino foi extraído. Foram copiados oito FBX necessários, não texturas/modelos humanos completos. As fontes femininas equivalentes foram localizadas e não duplicadas desnecessariamente.

`AS_MinerSwingingPicaxe.uasset` foi localizado em `assets/source/Free100Animations/Free100Animations/Content/ThirdPerson/Anims/100/RPG-NPCAnims/` do Godot original. Não havia licença local identificável na varredura do pacote nem editor Unreal/exportador compatível disponível neste ambiente. UASSET não é GLB/FBX nem entrada do sampler Polymesh. Não foi instalado um extrator desconhecido ou simulada uma conversão. **Integração bloqueada; mineração com picareta permanece vanilla.** Isso não afirma que a conversão seja impossível em outro ambiente autorizado.

## Conversão em lote

`prepare-a3.cjs` reutiliza em memória o sampler anterior e interrompe sua execução antes de qualquer escritor de projetos antigos. A lista completa de nomes reais, duração, arquivos, hashes e amostras está em `SELECTED_ASSETS.json`.

`blender-a3.py` abre o `.blend` R3 aprovado, reutiliza somente o escritor de matrizes e grava outro arquivo. Não executa os trechos históricos de criação de geometria. `package-a3.cjs` acrescenta animações a uma cópia do runtime A2, em **steve_a3.glb**. Os nove clipes anteriores e os dados de nodes, meshes, skins, materiais, UVs, pesos e bind matrices são preservados. R3/R3.1 e os arquivos A2 continuam disponíveis.

Foram preparados 25 candidatos novos. O recurso final contém 30 clipes: nove anteriores e 21 adicionais. **Quantidade empacotada não é cobertura funcional**: `Hit_Head` não recebe evento automático de headshot; quatro candidatos convertidos de interação/BowIdle02 ficaram fora do runtime. Consulte a tabela de cobertura.

## Adaptações necessárias

- UAL mantém as convenções de coordenadas e o rig R3 anteriores. As translações de root não movimentam a entidade. Agachamento, aterrissagem e morte aproveitam somente deslocamento vertical visual limitado da pelve, para não manter a pose baixa suspensa na altura de repouso.
- Salto: somente a parte final de `Jump_Start` é amostrada na saída real do chão; `Jump_Loop` acompanha o tempo no ar. `Jump_Land` é comprimido em aproximadamente 200 ms e não segura a movimentação. A morte acompanha `deathTime` vanilla e chega à pose final no fluxo normal, sem adiar respawn.
- Natação recebe ajuste vertical visual de 0,4 unidade do modelo. Oxigênio, colisão, velocidade e gravidade continuam vanilla; não há IK de água ou alinhamento perfeito da malha à caixa física.
- Os FBX Archer têm compensação constante de eixo/escala no `B-root`, diferente da pose de referência exportada. A primeira adaptação mostrou cabeça girada em 90°. A normalização usa a compensação no referencial de repouso, sem aplicá-la como movimento do Steve. Os eixos de flexão Archer são reconstruídos pelos vetores de braços/pernas; copiar somente Euler X do UAL mantinha cotovelos incorretos.
- Os FBX exportados pelo Blender usam amostragem a 25 Hz: ciclos de arco de referência de aproximadamente 1,333 s aparecem no GLB intermediário com 1,320 s; Load/Release com 0,800 s. Esses tempos reais estão documentados, sem afirmar igualdade com os aliases Godot. Os movimentos são reamostrados ao bake R3.
- O arco utiliza a mão de suporte da fonte e o renderer de itens vanilla com a entidade, preservando a propriedade visual de carregamento. A direção visual é alinhada à mira real. A mistura tentativa com pernas de outra pose não foi aceita: durante mira em movimento, agachado, no ar/água ou em ângulos extremos, usa-se fallback vanilla. Isso mantém o arco jogável sem afirmar suporte completo dessas combinações.
- Consume/torch usam a mão visual correspondente à fonte; o slot e o funcionamento do item continuam os mesmos. Dano/consumo/corte de madeira podem compor o tronco com os membros inferiores do estado físico, sem modificar a hierarquia ou criar renderizador novo.

## Arquivos e reprodução

```powershell
node animation_pipeline/scripts/inspect-a3.cjs
& 'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' --background --python animation_pipeline/scripts/archer-fbx-a3.py -- $PWD
node animation_pipeline/scripts/prepare-a3.cjs
& 'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' --background --python animation_pipeline/scripts/blender-a3.py -- $PWD
node animation_pipeline/scripts/package-a3.cjs
node animation_pipeline/scripts/validate-a3.cjs
```

O primeiro comando registra o baseline apenas se ainda não existir. O build prepara o empacotamento de derivados existentes, sem baixar pacotes ou reconverter os originais silenciosamente. Fonte/derivados e mundos de QA continuam ignorados pelo Git. Nenhuma nova ferramenta ou dependência foi instalada.
