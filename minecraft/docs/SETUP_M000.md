# Configuracao M000

Data: 2026-11-12 (America/Sao_Paulo).

## Inspecao

A pasta aberta `C:\Users\balen\Desktop\towerofentropy` estava vazia na inspecao inicial, sem projeto Godot ou AGENTS.md encontrado. Todos os arquivos deste marco foram criados em `minecraft/`. Nenhum arquivo Godot foi modificado.

Windows NT 10.0.26200.0, sistema de 64 bits. Consulta detalhada via CIM negada pelo ambiente; versao obtida por System.Environment. Git 2.53.0.windows.1 disponivel. Winget disponivel. CLI `code` nao encontrado no PATH; usuario utiliza VS Code, mas extensoes nao foram instaladas nem verificadas.

JDK ja instalado: `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`.
`java -version`: Microsoft OpenJDK 17.0.20.1 LTS, 64-Bit Server VM.
`javac -version`: 17.0.20.1.
JAVA_HOME inicialmente vazio. O script `scripts/gradle.ps1` define JAVA_HOME para o JDK encontrado via javac em cada execucao, sem alterar configuracoes globais do Windows. Nenhum novo JDK foi instalado.

## Fontes e downloads

- Forge: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html — 47.4.28 confirmado como Recommended.
- MDK direto oficial: https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.28/forge-1.20.1-47.4.28-mdk.zip
- MDK SHA-1 publicado e verificado: `eee9145e892bb5ca5fe72323bd81c8522c53417f`.
- CPM: https://modrinth.com/plugin/custom-player-models/version/xz6IfCg1
- Metadados: https://api.modrinth.com/v2/version/xz6IfCg1
- JAR: https://cdn.modrinth.com/data/h1E7sQNL/versions/xz6IfCg1/CustomPlayerModels-1.20-0.6.26a.jar
- CPM SHA-512 verificado: `66431b5961acdd3909ed655a75b54d0bf31d7668b4d02c37e69da3ca7cc5be5c5c32515527ce5dd8d98871c790f5f66d9d9767d45ed546a1c8170d6440faa5e6`.

CPM e compativel com Forge e Minecraft 1.20–1.20.1. Apenas o JAR foi preparado em `dependencies/cpm/`; nao foi colocado em run/mods, declarado como dependencia Gradle ou incorporado ao artefato. Dependencias opcionais nao foram baixadas.

MDK extraido com wrapper original (Gradle 8.8). Configuracao ForgeGradle original `[6.0,6.2)` preservada. Mappings oficiais 1.20.1, toolchain Java 17. Metadados configurados para Tower of Entropy, `com.towerofentropy`, ID `towerofentropy`, versao `0.0.1`, All Rights Reserved. Faixa Minecraft restrita a 1.20.1; Forge 47.4.28 ate antes de 48.

Classes demonstrativas do MDK removidas. Apenas `TowerOfEntropy.java` com anotacao @Mod e construtor vazio permanece. Wrapper, avisos e licenca do MDK preservados; README.txt original e referencia do template, README.md descreve este projeto.

Caches, downloads, JAR CPM, logs brutos, builds e diretorios de execucao ignorados pelo Git. Nao foi criado commit ou publicado repositorio. Documentacao de resultados em TEST_LOG.md.

## Repetir

Na pasta `minecraft`, execute os comandos do README. A primeira execucao requer rede para Gradle, Forge/Maven e arquivos oficiais de Minecraft. Build e runClient usam o ambiente de desenvolvimento padrao Forge, independente de TLauncher. Nenhuma alteracao de autenticacao ou licenca foi feita.
ForgeGradle efetivamente resolvido: 6.0.54. Build e runClient concluidos com sucesso; detalhes e avisos em TEST_LOG.md.
