# Otimizações de Hotspots - Title ID 00040000000EC300

Este diretório contém os códigos otimizados para o jogo **The Legend of Zelda: A Link Between Worlds** (Title ID: `00040000000EC300`).

## 1. Hotspot: `f_0011638C` (Cópia rápida de memória - 112 bytes)
- **Arquivo de origem:** `code001.c` (linhas ~22896 a 22940)
- **Arquivo isolado:** `f_0011638C_memcpy.c`
- **Otimização realizada:**
  - Fast-path direto via `memmove` usando `ctx->write_pages` e `ctx->read_pages`.
  - Tratamento correto de swap de registradores (`r0 = src`, `r1 = dst`, retorno `r0 = dst`).
  - Fallback por palavra caso a cópia cruze fronteira de páginas.
- **Resultados:**
  - Amostras no Simpleperf: queda de `3.14%` (554 amostras) para `1.24%` (116 amostras) — redução de >60% do overhead da função.
  - Taxa de quadros (FPS):
    - Cenas 2D e menus: 60 FPS estáveis.
    - Cenas 3D pesadas: ganho de 6.0 FPS base para 8.3 - 11.6 FPS (+20% a +35%).

## Localização dos Arquivos
- **No Dispositivo Android (Ativo em Execução):**
  - Cache compilado: `/data/user/0/com.fearkov.zakuro/files/cache/3dsrecomp/00040000000EC300/code001.c`
  - Pasta persistente de Overrides: `/data/user/0/com.fearkov.zakuro/files/overrides/00040000000EC300/`
- **No Repositório Git (Workspace):**
  - `overrides/00040000000EC300/f_0011638C_memcpy.c`
  - `overrides/00040000000EC300/code001.c`
  - `patches/00040000000EC300_hotspot_memcpy.patch`
