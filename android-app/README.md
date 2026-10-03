# Zakuro Android ARM64 (MVP scaffold)

Este diretório contém o primeiro scaffold do APK Android usando `NativeActivity` e o entrypoint `android_main` do crate Rust `zakuro`.

## Build da biblioteca nativa

Na raiz do repositório:

```bash
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/30.0.16248370"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$HOME/.cargo/bin:$PATH"

cargo ndk -t arm64-v8a -P 26 build -p zakuro \
  --no-default-features --features android --lib
```

A saída é empacotada como `lib/arm64-v8a/libmain.so` no APK pelo script de empacotamento.

Para gerar o APK assinado com uma chave de debug local:

```bash
./android-app/build-apk.sh
keytool -genkeypair -v -keystore android-app/build/debug.keystore \
  -storepass android -keypass android -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname 'CN=Android Debug,O=Android,C=US'
```

O artefato assinado de teste é `build/zakuro-arm64-debug.apk`.

O logo fornecido está configurado como ícone do aplicativo em múltiplas densidades Android (`mipmap-*`).

## Interface XML adaptativa

O launcher agora usa `MainActivity` com layout XML adaptativo (`layout/` e `layout-land/`), dimensões para telas grandes (`values-sw600dp/`) e:

- botão para selecionar uma ROM `.3ds`, `.cci` ou `.cxi` pelo seletor de documentos do Android;
- cópia da ROM selecionada para o armazenamento privado do app;
- `ProgressBar` de progresso e status da preparação;
- botão **Iniciar recompilação**, que abre o frontend nativo Rust/winit original, preservado em `NativeActivity`.

O frontend nativo continua sendo responsável pela recompilação real via `3dsrecomp` e mantém todas as funções existentes do emulador. A tela XML é a entrada adaptativa; o progresso detalhado da compilação continua disponível no fluxo nativo do Zakuro.

## Preparar o ambiente de compilação

Com internet disponível, execute uma vez na raiz do módulo Android:

```bash
./android-app/setup-build-env.sh
./android-app/build-apk.sh
```

O script instala/seleciona Android SDK 35, Build Tools 35, NDK 27.2, JDK, `cargo-ndk` e gera o APK ARM64 em `android-app/build/apk-aligned.apk`.

## Acesso amplo a arquivos e permissões

O app solicita a permissão especial `MANAGE_EXTERNAL_STORAGE` (Android 11+) e permissões legadas de armazenamento em tempo de execução. Na primeira execução ou ao tocar em "Conceder acesso a arquivos", o app abre um diálogo explicativo e encaminha o usuário diretamente à tela de configuração do sistema para habilitar o acesso aos arquivos. Concedida a permissão, o aviso na tela inicial desaparece dinamicamente.

## Limitações desta etapa

- O APK ainda inicializa o frontend nativo do Zakuro; a seleção SAF de ROM e os controles touch virtuais serão adicionados no próximo incremento.
- O build é somente `arm64-v8a`.
- A API mínima é 26, pois o áudio usa AAudio através do `cpal`.
- O usuário deve fornecer uma ROM própria e compatível com o runtime.
