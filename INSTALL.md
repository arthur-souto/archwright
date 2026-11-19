# Instalando o Archwright

O Archwright é um app de desktop: você escolhe nome, versões e dependências, e ele grava um projeto Spring Boot com a arquitetura montada e as fitness functions. O instalador já leva o Java dentro, então **não precisa instalar JDK** para usar o app (o projeto gerado, sim, precisa de JDK para compilar).

## 1. Baixar

Pegue o instalador do seu sistema na página de [Releases](../../releases) do repositório:

| Sistema | Arquivo |
|---|---|
| macOS (Apple Silicon: M1, M2, M3…) | `Archwright-1.0.0-macos-arm64.dmg` |
| macOS (Intel) | `Archwright-1.0.0-macos-x64.dmg` |
| Windows 10 ou 11 | `Archwright-1.0.0-windows-x64.msi` |
| Linux (Debian, Ubuntu e derivados) | `archwright_1.0.0_amd64-linux-x64.deb` |

As releases são pré-lançamentos (instaladores sem assinatura). Se a sua versão ainda não tiver instalador, gere o do seu sistema seguindo o [README](README.md#gerando-o-instalador). Nos arquivos, o `1.0.0` é só o número do instalador; a versão do Archwright é a da release.

## 2. Instalar

Os instaladores **ainda não são assinados**, então o sistema mostra um aviso de "desenvolvedor não identificado". O app é o mesmo; o aviso só significa que ainda não pagamos a assinatura.

### macOS

1. Dê dois cliques no `.dmg`. Ele **não instala sozinho**: só abre uma janela com o app e um atalho para a pasta Applications. **Arraste o Archwright para a pasta Applications** (essa é a instalação). Depois, ejete o disco no Finder, pelo botão de ejetar ao lado do volume "Archwright". Se abrir o app de dentro da janela do `.dmg`, ele roda do disco e some quando você ejetar.
2. Na primeira vez, **clique com o botão direito** no app, escolha **Abrir** e confirme em **Abrir**.
3. Se o macOS bloquear mesmo assim (Sequoia ou mais novo): **Ajustes do Sistema > Privacidade e Segurança**, role até o aviso do Archwright e clique em **Abrir Mesmo Assim**.
4. Alternativa pelo Terminal: `xattr -dr com.apple.quarantine /Applications/Archwright.app`

Depois da primeira vez, abre normalmente pelo Launchpad.

### Windows

1. Dê dois cliques no `.msi`.
2. Se aparecer "O Windows protegeu o seu PC" (SmartScreen), clique em **Mais informações** e depois em **Executar assim mesmo**.
3. Siga o instalador (pode escolher a pasta). Ele cria um atalho no menu Iniciar, na pasta **Archwright**.

### Linux

    sudo apt install ./archwright_*.deb

O app aparece no menu de aplicativos. Em outras distribuições, rode a partir do código (veja o README).

## 3. Usar

1. **Projeto:** nome (artifact), group, pacote base e estilo de arquitetura (modular ou em camadas). A árvore de pacotes à direita muda enquanto você digita; clique num pacote para ver para que ele serve.
2. **Versões:** Spring Boot e Java. A lista vem do Maven Central; sem internet, o app usa a lista que vem com ele.
3. **Dependências:** escolha um kit (Básico ou Nenhum), ajuste as dependências, marque os extras (Docker Compose, GitHub Actions) e deixe as fitness functions ligadas.
4. **Exportar:** escolha ZIP ou pasta, escolha onde gravar e clique em **Gerar projeto**. O botão **Abrir pasta** leva até o resultado.

O app lembra as últimas escolhas (group, estilo, kit, formato, destino e tema).

## 4. Rodar o projeto gerado

Dentro da pasta do projeto (descompacte o ZIP antes), com JDK na versão que você escolheu:

    ./mvnw verify

Isso compila, roda os testes e as fitness functions (ArchUnit). Se alguém violar a arquitetura, o build quebra.

## Problemas comuns

| Sintoma | O que fazer |
|---|---|
| "A pasta de destino não existe" | Escolha uma pasta que já exista, pelo botão **Escolher…**. |
| "Já existe um arquivo com esse nome" | Confirme para substituir ou escolha outra pasta. |
| "A pasta já existe e não está vazia" | No formato pasta, o destino precisa estar livre; mude o nome do artifact ou apague a pasta. |
| Aviso "Sem conexão" na tela de versões | O app usou a lista embutida; as versões mais novas podem faltar. |
| O app não abre no macOS | Veja os passos 2 a 4 da seção macOS acima. |

## Desinstalar

- **macOS:** arraste o Archwright da pasta Applications para a Lixeira.
- **Windows:** Configurações > Aplicativos > Archwright > Desinstalar.
- **Linux:** `sudo apt remove archwright`.
