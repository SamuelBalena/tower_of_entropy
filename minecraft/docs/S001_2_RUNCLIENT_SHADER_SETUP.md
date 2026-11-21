# S001.2 — Shaders no runClient

## Iniciar

Em `minecraft/`, com JDK 17 disponível no terminal:

```powershell
.\gradlew runClient
```

O diretório de jogo principal é `C:/Users/balen/Desktop/towerofentropy/minecraft/run`. A instância tinha um mundo existente, configurações próprias e uma pasta `mods` vazia. Oculus e Embeddium eram carregados pelos perfis isolados da S001.1 via script de inicialização de QA. O novo comando usa o diretório principal.

Abra **Options → Video Settings → Shader Packs**. Selecione **Tower of Entropy - Fantasy Foundation.zip**. Em **Shader Pack Settings → Profile**, alterne entre **Light (Leve)**, **Balanced (Equilibrado)** e **High (Alto)**, e aplique. O botão **Shaders: Enabled/Disabled** desliga ou liga os efeitos na própria sessão. Use **Apply** ou **Done** para salvar a seleção.

Para iniciar sem Oculus e Embeddium:

```powershell
.\gradlew runClient -PtoeNoOculus
```

O modo sem carregadores usa o mesmo diretório de jogo. O ZIP e as preferências ficam disponíveis para a próxima sessão com o comando padrão.

## Configuração

- `build.gradle` carrega `gradle/run-client-shaders.gradle` após configurar os cenários de execução.
- O script inclui Embeddium 0.3.31 e Oculus 1.8.0 como `runtimeOnly fg.deobf` no `runClient` principal. Ele reaproveita os JARs da S001.1 em `.downloads/s001` quando presentes. A alternativa usa as versões fixadas no Maven do Modrinth. O ForgeGradle aplica os mappings do ambiente de desenvolvimento. [Embeddium](https://modrinth.com/mod/embeddium/version/UTbfe5d1) · [Oculus](https://modrinth.com/mod/oculus/version/iQ1SwGc3)
- A tarefa de preparação copia somente o ZIP original para `run/shaderpacks/`. A origem permanece em `shaderpacks/`, junto da pasta editável. O carregador usa a cópia de execução e guarda suas opções ao lado dela. O ZIP de origem e a cópia validada têm SHA-256 `8B155490F9981D2AD773A8601438C8C10425BC099BC186F76E037ABCE7B059BD`.
- A tarefa de inicialização cria `run/config/oculus.properties` somente quando o arquivo está ausente. Na primeira execução, seleciona e liga a S001. O Equilibrado é o padrão definido no próprio ZIP. Após isso, o Oculus mantém a seleção e os ajustes do usuário. Outros shaderpacks, `options.txt` e diretórios de saves não fazem parte da sincronização.
- O carregamento automático vale para o comando `runClient` principal. Perfis isolados continuam usando suas próprias dependências e diretórios. O JAR de distribuição e os sistemas R3, A001, C002.2 e W002.4 usam o código já validado na S001.1.

## Validação

O comando padrão abriu a instância principal com Embeddium e Oculus. A aba [Video Settings](../.logs/s0012-video.png) exibiu o botão Shader Packs, e a [lista do Oculus](../.logs/s0012-packs.png) apresentou a S001 já selecionada. O [menu de perfis](../.logs/s0012-profiles.png) confirmou Equilibrado na primeira inicialização.

Na cópia de QA do mundo, apliquei Alto, Leve e Equilibrado. O Oculus registrou os três nomes e recriou o pipeline do Overworld na sessão. Em terceira pessoa, a [captura do túnel](../.logs/s0012-high-world.png) mostra a silhueta translúcida com armadura e o minério sob a mira. O [controle desligado](../.logs/s0012-disabled.png) desativou os shaders e o jogo continuou no mesmo mundo. [Log do comando padrão](../.logs/s0012-main.log)

- Reiniciei com o comando padrão e os JARs locais temporariamente fora do caminho. O Gradle resolveu as versões oficiais no Modrinth. Seus SHA-256 coincidiram com os JARs validados na S001.1. O Oculus iniciou com os shaders ainda desligados. Reativei o Equilibrado pelo menu ao final do teste. [Log do reinício](../.logs/s0012-restart.log) · [Preferência preservada](../.logs/s0012-restart-disabled.png) · [Reativação](../.logs/s0012-reenabled.png)
- O comando com `-PtoeNoOculus` iniciou o Minecraft, abriu o mesmo mundo de QA e desenhou a armadura translúcida pelo caminho nativo. A configuração do Oculus permaneceu byte a byte igual. [Log sem carregadores](../.logs/s0012-no-oculus.log) · [Captura sem carregadores](../.logs/s0012-no-loader-armored.png)
- O save de QA foi arquivado em `.logs/s0012-world-archive`. A versão distribuída do mod manteve SHA-256 `FBDA63A942DE82BDE80FCFDC115615D659E5DA21F6A69620B0DC9DEF39767304`. A S001.1 documenta as regressões de combate, parkour e diagnóstico em jogo. [Validação anterior do renderer](S001_1_OCULUS_POLYMESH_COMPATIBILITY.md)

A regressão isolada Slim com Equilibrado ativo passou nas **95 checagens**, incluindo a rota Oculus a 12%, restauração a 100%, armaduras, troca de skin, Roll, arco e natação em oito direções. [Log da regressão](../.logs/s0012-slim.log)
