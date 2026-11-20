# Testes M000

Data: 2026-11-12, America/Sao_Paulo.

## Resultados confirmados

| Verificacao | Resultado |
|---|---|
| java e javac | Microsoft OpenJDK 17.0.20.1, JVM 64 bits |
| Gradle wrapper oficial | Gradle 8.8 executado |
| ForgeGradle resolvido | 6.0.54 (faixa original MDK 6.0–6.2) |
| Forge | 1.20.1-47.4.28 |
| MDK SHA-1 | Confere com a pagina oficial |
| CPM SHA-512 | Confere com os metadados oficiais Modrinth |
| Build | BUILD SUCCESSFUL in 13s; codigo de saida 0 |
| Artefato | build/libs/towerofentropy-0.0.1.jar, 4662 bytes |
| JAR | Classe com/towerofentropy/TowerOfEntropy.class e META-INF/mods.toml presentes |
| Java bytecode | Major version 61 (Java 17) |
| Metadados processados | ID towerofentropy, Minecraft [1.20.1], Forge [47.4.28,48) |
| Testes automatizados | test NO-SOURCE; nenhum teste de gameplay criado |

Comando de compilacao: `.\scripts\gradle.ps1 build --console=plain`, que executa `gradlew.bat build --console=plain` com JAVA_HOME e GRADLE_USER_HOME locais ao processo.

## Ocorrencias resolvidas

1. Download inicial bloqueado pela restricao de rede do sandbox. Download oficial concluido na execucao com acesso ampliado autorizado.
2. JAVA_HOME vazio no ambiente inicial. Script passou a definir o JDK 17 existente em cada execucao, sem instalacao adicional ou mudanca global.
3. Primeiras tentativas do script interrompidas pelo tratamento de stderr nativo do Windows PowerShell 5 como erro terminante. Corrigido para usar o codigo de saida nativo; compilacao posterior concluida.
4. Consulta Win32_OperatingSystem via CIM sem permissao; identificacao basica do Windows e arquitetura obtida via System.Environment.

Logs brutos locais ignorados pelo Git: `.logs/build.log`, `.logs/build-first-attempt.log`, `.logs/runClient.log`. Nao contêm uma suite de testes: registram execucoes das ferramentas. Avisos informativos de ferramentas podem aparecer como NativeCommandError no PowerShell; o resultado final usa o exit code e o estado do Gradle.

## Limites de escopo

Sem integracao CPM, importacao de animacoes, sistemas de RPG/combate, alteracoes de skin/camera, testes de servidor dedicado ou testes em TLauncher. Nenhuma autenticacao foi modificada. Nao foi iniciado M001.
## Cliente de desenvolvimento — concluido

Comando: `.\scripts\gradle.ps1 runClient --console=plain`.
Resultado: `BUILD SUCCESSFUL in 1m 58s`, codigo de saida 0, apos fechamento normal da janela pelo teste.

Evidencias:
- Janela confirmada: `Minecraft* Forge 1.20.1`.
- Forge 47.4.28 / Minecraft 1.20.1 / Microsoft Java 17.0.20.1.
- OpenGL 4.6 inicializado na NVIDIA GeForce GTX 1050 Ti.
- `Creating FMLModContainer instance for com.towerofentropy.TowerOfEntropy` no debug.log.
- Recursos, texturas e audio inicializados; fechamento registrado como `Stopping!`.

Nao foi criado mundo nem realizado teste de gameplay. A confirmacao usa titulo de janela e logs, sem inspecao visual por captura de tela.

Avisos observados: ajustes automaticos de configuracoes iniciais Forge, avisos mods.toml de bibliotecas internas, schema union dos recursos, sampler de shader ausente e APIs Gradle depreciadas para uma futura versao 9. Nenhum impediu esta execucao com o wrapper 8.8 oficial. O cliente informou falha de autorizacao Realms (`Failed to parse into SignedJWT: 0`) na sessao padrao de desenvolvimento; servicos Realms/online nao foram validados e autenticacao nao foi alterada.

SHA-256 do JAR gerado nesta execucao: `0119c14bf764bdc921f44df6991c54edd7cf35b50f050695113f6aed6d7400cf`. O manifesto original inclui timestamp, portanto outra compilacao pode gerar hash diferente.

M000 concluido: ambiente verificado, build aprovado, cliente iniciado e encerrado normalmente. M001 nao iniciado.