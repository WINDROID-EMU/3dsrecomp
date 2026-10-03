# Zakuro Android ARM64

Aplicativo Android ARM64 (`arm64-v8a`) para executar o frontend nativo Rust/winit do Zakuro, um emulador HLE de Nintendo 3DS com suporte à recompilação AOT por meio do `3dsrecomp`.

> **Estado do projeto:** build funcional de debug. O APK é destinado a testes e usa uma chave de assinatura debug; para distribuição, gere uma chave de release própria.

## Requisitos para compilar

### Sistema

- Linux 64-bit;
- conexão com a internet para baixar dependências do Rust, Android SDK/NDK e o repositório `3dsrecomp`;
- pelo menos 8 GB de espaço livre recomendados para SDK, NDK, cache do Cargo e artefatos;
- pelo menos 8 GB de RAM recomendados para a compilação nativa.

### Ferramentas obrigatórias

| Componente | Versão/configuração usada | Observação |
|---|---:|---|
| JDK | 21 ou compatível | O comando `javac` precisa estar no `PATH`. |
| Rust | 1.95.0 | Fixado em `../rust-toolchain.toml`. |
| Rust target | `aarch64-linux-android` | Necessário para o APK ARM64. |
| `cargo-ndk` | 4.x | Compila o crate Rust para Android. |
| Android SDK Platform | API 35 | Usado pelo `aapt2` e pelo `android.jar`. |
| Android Build Tools | 35.0.0 ou compatível | Fornece `aapt2`, `d8`, `zipalign` e `apksigner`. |
| Android NDK | 27.2.12479018 | Fornece o linker/clang para ARM64. |
| Utilitários | `curl`, `unzip`, `zip`, `keytool` | Usados pelos scripts e pela assinatura debug. |

O `Cargo.toml` também baixa dependências Git do projeto `3dsrecomp`; portanto, o acesso ao GitHub deve estar disponível durante o primeiro build.

## Preparar o ambiente

Na raiz do repositório:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk-headless curl unzip zip
```

Instale Rust/rustup e o target Android:

```bash
rustup toolchain install 1.95.0 --profile minimal
rustup target add --toolchain 1.95.0 aarch64-linux-android
rustup default 1.95.0
cargo install cargo-ndk --locked
```

Configure o Android SDK, o NDK e o Build Tools em `$HOME/android-sdk`. O script fornecido tenta instalar os componentes automaticamente:

```bash
./android-app/setup-build-env.sh
```

O script instala/seleciona:

- Android SDK Platform 35;
- Android Build Tools 35.0.0;
- Android NDK 27.2.12479018;
- `platform-tools`;
- `cargo-ndk`;
- target Rust `aarch64-linux-android`.

Se o `sdkmanager` apresentar erro de arquivo corrompido (`bad_record_mac` ou `unknown archive`), repita o script em uma rede estável ou instale os pacotes diretamente pelo Android Studio/SDK Manager. Antes de compilar, confirme:

```bash
$HOME/android-sdk/build-tools/35.0.0/aapt2 version
$HOME/android-sdk/build-tools/35.0.0/d8 --version
test -f "$HOME/android-sdk/platforms/android-35/android.jar"
test -x "$HOME/android-sdk/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
```

## Compilar o APK

Execute na raiz do repositório:

```bash
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.2.12479018"
export ANDROID_BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$HOME/.cargo/bin:$PATH"

./android-app/build-apk.sh
```

O script realiza as seguintes etapas:

1. compila o crate Rust `zakuro` com `cargo ndk` para `arm64-v8a`;
2. copia a biblioteca nativa para `lib/arm64-v8a/libmain.so`;
3. compila os recursos XML com `aapt2`;
4. gera `R.java`;
5. compila `MainActivity.java` e `NativeActivity.java`;
6. gera `classes.dex` com `d8`;
7. empacota a biblioteca e o DEX no APK;
8. executa `zipalign`.

O resultado não assinado fica em:

```text
android-app/build/apk-aligned.apk
```

## Assinar para testes

Gere uma chave debug local, caso ainda não exista:

```bash
keytool -genkeypair -v \
  -keystore android-app/build/debug.keystore \
  -storepass android -keypass android \
  -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname 'CN=Android Debug,O=Android,C=US'
```

Assine o APK:

```bash
$ANDROID_BUILD_TOOLS/apksigner sign \
  --ks android-app/build/debug.keystore \
  --ks-pass pass:android \
  --ks-key-alias androiddebugkey \
  --key-pass pass:android \
  --out android-app/build/zakuro-arm64-debug.apk \
  android-app/build/apk-aligned.apk
```

Valide a assinatura e o alinhamento:

```bash
$ANDROID_BUILD_TOOLS/apksigner verify --verbose \
  android-app/build/zakuro-arm64-debug.apk

$ANDROID_BUILD_TOOLS/zipalign -c -v 4 \
  android-app/build/zakuro-arm64-debug.apk
```

## Instalar no dispositivo

O dispositivo deve usar Android API 26 ou superior e arquitetura ARM64:

```bash
adb install -r android-app/build/zakuro-arm64-debug.apk
```

O pacote Android é `com.fearkov.zakuro`.

## Funções do aplicativo

### Tela inicial XML adaptativa

A tela inicial é uma `MainActivity` Java com layout XML e recursos adaptativos para:

- celulares e tablets (`values-sw600dp`);
- orientação retrato e paisagem (`layout` e `layout-land`);
- logo do Zakuro;
- seleção de ROM pelo seletor de documentos do Android;
- exibição do nome da ROM selecionada;
- cópia da ROM para o armazenamento privado do aplicativo;
- `ProgressBar` de status/progresso da preparação da ROM;
- botão **Iniciar recompilação**, que abre o frontend nativo original.

Formatos aceitos na tela de seleção: `.3ds`, `.cci` e `.cxi`. O filtro do seletor usa conteúdo binário; o usuário deve selecionar uma ROM compatível.

### Frontend nativo Rust/winit

O frontend nativo original é preservado em `NativeActivity` e continua responsável por:

- inicializar o runtime Rust do Zakuro;
- abrir a biblioteca de jogos;
- executar títulos compatíveis de Nintendo 3DS;
- usar renderização Vulkan/OpenGL conforme a configuração disponível;
- reproduzir áudio por AAudio/`cpal` quando suportado;
- aceitar teclado, mouse e controles compatíveis;
- exibir as duas telas do 3DS;
- abrir o menu nativo de configurações;
- iniciar a recompilação AOT de um título por meio do `3dsrecomp`;
- exibir o progresso detalhado dos jobs de recompilação no frontend nativo.

### Recompilação

A recompilação real é feita pelo código Rust existente e pelo crate `recomp3ds`. Ela gera código nativo AOT para acelerar títulos compatíveis. O frontend nativo mantém o fluxo original de recompilação e seus jobs em segundo plano.

A barra de progresso (`ProgressBar`) da tela XML representa o estado da preparação/entrada da ROM. O progresso detalhado do job de `3dsrecomp` é exibido no fluxo nativo do Zakuro, depois que o frontend é aberto.

## Permissões de Armazenamento

O aplicativo gerencia a permissão de acesso completo a arquivos em tempo de execução (`MANAGE_EXTERNAL_STORAGE` para Android 11+ e `READ/WRITE_EXTERNAL_STORAGE` para legados):

- **Solicitação Automática**: ao abrir o app, se a permissão ainda não foi concedida, é apresentado um diálogo explicativo direcionando direto para a tela do sistema **Acesso a todos os arquivos** do Zakuro.
- **Botão na Tela**: um botão "Conceder acesso a arquivos" permanece visível e desaparece automaticamente assim que a permissão é confirmada.
- **Seletor de ROM**: caso o usuário tente selecionar um arquivo sem a permissão, o app solicita novamente o acesso para garantir leitura e gravação da recompilação sem falhas.

## Arquitetura Android

- `MainActivity.java`: launcher XML, seleção/preparação da ROM e abertura do frontend;
- `NativeActivity.java`: wrapper da `android.app.NativeActivity` original;
- `AndroidManifest.xml`: declara Activities, ícone, API mínima, API alvo e permissões;
- `res/layout/activity_main.xml`: layout principal;
- `res/layout-land/activity_main.xml`: variante de paisagem;
- `res/values-sw600dp/dimens.xml`: dimensões para telas grandes;
- `res/mipmap-*`: ícones do launcher;
- `build-apk.sh`: build completo do APK;
- `setup-build-env.sh`: preparação do ambiente.

## Limitações conhecidas

- O APK gerado é somente `arm64-v8a`; não há variante armeabi-v7a, x86 ou x86_64.
- O APK debug usa uma chave de teste e não deve ser publicado como release.
- É necessário fornecer ROM própria, compatível e legalmente obtida; nenhuma ROM é incluída.
- A disponibilidade e a velocidade da recompilação dependem do título, do dispositivo e do compilador nativo disponível.
- Nem todos os jogos de 3DS são compatíveis com o runtime atual.
- A instalação da permissão especial de acesso amplo a arquivos depende da versão e da fabricante do Android.
