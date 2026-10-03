#include "recomp.h"
#include <string.h>

/*
 * Hotspot: f_0011638C (Cópia rápida de blocos de 112 bytes)
 * Title ID: 00040000000EC300 (The Legend of Zelda: A Link Between Worlds)
 *
 * Registradores de entrada:
 *   ctx->r[0]: src pointer
 *   ctx->r[1]: dst pointer
 *   ctx->r[14]: return address
 *
 * Retorno:
 *   ctx->r[0]: dst pointer
 */
void f_0011638C(Context *ctx) {
    uint32_t dst = ctx->r[1];
    uint32_t src = ctx->r[0];
    uint32_t len = 0x70; // 112 bytes
    uint32_t dst_end = dst + len - 1;
    uint32_t src_end = src + len - 1;

    // Fast-path: cópia direta de memória em páginas mapeadas
    if ((dst >> 12) == (dst_end >> 12) && (src >> 12) == (src_end >> 12)) {
        uint8_t *p_dst = ctx->write_pages[dst >> 12];
        uint8_t *p_src = ctx->read_pages[src >> 12];
        if (LIKELY(p_dst != NULL && p_src != NULL)) {
            memmove(p_dst + (dst & 0xFFF), p_src + (src & 0xFFF), len);
            ctx->r[0] = dst;
            RETURN_TO(ctx->r[14]);
        }
    }

    // Fallback: cópia palavra por palavra segura
    for (uint32_t i = 0; i < len; i += 4) {
        mem_write32(ctx, dst + i, mem_read32(ctx, src + i));
    }
    ctx->r[0] = dst;
    RETURN_TO(ctx->r[14]);
}
