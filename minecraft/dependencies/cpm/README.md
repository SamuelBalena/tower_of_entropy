# CPM — dependência local de desenvolvimento

Versão exata: CustomPlayerModels-1.20-0.6.26a.jar / mod 0.6.26a.
Fonte oficial: https://modrinth.com/plugin/custom-player-models/version/xz6IfCg1
Compatibilidade: Forge; Minecraft 1.20 e 1.20.1.
SHA-512: 66431b5961acdd3909ed655a75b54d0bf31d7668b4d02c37e69da3ca7cc5be5c5c32515527ce5dd8d98871c790f5f66d9d9767d45ed546a1c8170d6440faa5e6

M000 apenas baixou o JAR. Desde M001.1, build.gradle declara a dependência local via fg.deobf e o runtime carrega CPM. A API pública aplica/restaura o modelo de teste; classes internas do editor exportam o projeto somente com parâmetro de preparação.

O JAR permanece ignorado pelo Git e não é incorporado ao JAR Tower of Entropy. Para restaurá-lo use scripts/download-cpm.ps1. Não copie também o original para run/mods: a dependência Gradle já fornece a versão remapeada ao cliente de desenvolvimento.

Consulte docs/TEST_LOG_M001_1.md para execução, geração dos modelos e limitações.