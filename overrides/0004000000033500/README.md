# Otimizações de Hotspots - Title ID 0004000000033500

Este diretório contém os códigos otimizados para o jogo (Title ID: `0004000000033500`).

## 1. Hotspot 1: `f_00272694` (Rotação de Matrizes 3D)
- **Arquivo de origem:** `code021.c` (linhas ~97020 a 97861)
- **Arquivo isolado:** `f_00272694_matrix_rotation.c`
- **Otimização realizada:**
  - Eliminação de temporários com bitcasting `ctx->vfp[]` (stack spilling do TinyCC).
  - Uso de variáveis locais `float` (`v0..v5`, `s0..s3`, `c0..c2`) mantidas diretamente nos registradores `s0-s31` / `d0-d31` do ARM64.
  - Eliminação de checagens redundantes de FTZ por instrução.
- **Resultados:**
  - Tamanho em bytes: de `71.948 bytes` para `67.552 bytes` (-4.396 bytes).
  - CPU no Simpleperf: de `5.22%` para `0.33%` (-93.7% de overhead).

## 2. Hotspot 2: `f_0031FE84` (Máquina de Estados de Áudio)
- **Arquivo de origem:** `code032.c` (linhas ~803 a 1084, blocos dos estados 2 e 3)
- **Arquivo isolado:** `f_0031FE84_audio_state_machine.c`
- **Otimização realizada:**
  - Redução de stack spills nos blocos dos estados 2 (`L_0031FEF0`) e 3 (`L_0031FF0C`).
  - Chamada direta preservando registrador e teste de retorno imediato (`if (ctx->r[0] == 0) goto L_0031FEEC;`).
  - Eliminação de recálculo redundante de flags N/Z/C/V e de múltiplos pares store/load em stack.
- **Resultados:**
  - Tamanho em bytes: de `13.296 bytes` para `12.760 bytes` (-536 bytes, -134 instruções).
  - Amostras no Simpleperf: de `1.066` para `630` amostras (-40.9% de tempo de CPU na função).
  - Ganho de FPS medido na mesma cena: de `15.2 - 18.3 FPS` para `18.5 - 19.7 FPS`.

## Localização dos Arquivos
- **No Dispositivo Android (Ativo em Execução):**
  - Cache compilado: `/data/user/0/com.fearkov.zakuro/files/cache/3dsrecomp/0004000000033500/code032.c`
  - Cache compilado: `/data/user/0/com.fearkov.zakuro/files/cache/3dsrecomp/0004000000033500/code021.c`
  - Pasta persistente de Overrides do Zakuro: `/data/user/0/com.fearkov.zakuro/files/overrides/0004000000033500/`
- **No Repositório Git (Workspace):**
  - `overrides/0004000000033500/code032.c`
  - `overrides/0004000000033500/code021.c`
  - `overrides/0004000000033500/f_0031FE84_audio_state_machine.c`
  - `overrides/0004000000033500/f_00272694_matrix_rotation.c`
