void f_0031FE84(Context *ctx) {
    uint32_t target = ctx->r[15];
    if (LIKELY(target == 0x0031FE84u && !ctx->thumb)) goto L_0031FE84;
dispatch:
    if (!ctx->thumb) switch (target) {
    case 0x0031FE84u: goto L_0031FE84;
    case 0x0031FE88u: goto L_0031FE88;
    case 0x0031FE8Cu: goto L_0031FE8C;
    case 0x0031FE90u: goto L_0031FE90;
    case 0x0031FE94u: goto L_0031FE94;
    case 0x0031FE98u: goto L_0031FE98;
    case 0x0031FE9Cu: goto L_0031FE9C;
    case 0x0031FEA0u: goto L_0031FEA0;
    case 0x0031FEA4u: goto L_0031FEA4;
    case 0x0031FEA8u: goto L_0031FEA8;
    case 0x0031FEACu: goto L_0031FEAC;
    case 0x0031FEB0u: goto L_0031FEB0;
    case 0x0031FEB4u: goto L_0031FEB4;
    case 0x0031FEBCu: goto L_0031FEBC;
    case 0x0031FEC0u: goto L_0031FEC0;
    case 0x0031FEC4u: goto L_0031FEC4;
    case 0x0031FEC8u: goto L_0031FEC8;
    case 0x0031FECCu: goto L_0031FECC;
    case 0x0031FED0u: goto L_0031FED0;
    case 0x0031FED4u: goto L_0031FED4;
    case 0x0031FED8u: goto L_0031FED8;
    case 0x0031FEDCu: goto L_0031FEDC;
    case 0x0031FEE0u: goto L_0031FEE0;
    case 0x0031FEE4u: goto L_0031FEE4;
    case 0x0031FEE8u: goto L_0031FEE8;
    case 0x0031FEECu: goto L_0031FEEC;
    case 0x0031FEF0u: goto L_0031FEF0;
    case 0x0031FEF8u: goto L_0031FEF8;
    case 0x0031FF0Cu: goto L_0031FF0C;
    case 0x0031FF14u: goto L_0031FF14;
    case 0x0031FF24u: goto L_0031FF24;
    }
    ctx->r[15] = target;
    CALL(recomp_call);
    return;
L_0031FE84:
    BUDGET(0x0031FE84u, 1);
    /* 0031FE84 E92D4010 */
    { uint32_t base = ctx->r[13], p = base - 8, fin = base - 8;
    mem_write32(ctx, p, ctx->r[4]); p += 4;
    mem_write32(ctx, p, ctx->r[14]); p += 4;
    ctx->r[13] = fin;
    (void)p; (void)fin; }
L_0031FE88:
    BUDGET(0x0031FE88u, 1);
    /* 0031FE88 E1A04000 */
    {
    uint32_t m = ctx->r[0];
    uint32_t b = m;
    uint32_t r = b;
    ctx->r[4] = r;
    }
L_0031FE8C:
    BUDGET(0x0031FE8Cu, 1);
    /* 0031FE8C E5D00488 */
    { uint32_t base = ctx->r[0], oa = base + 0x488u;
    uint32_t v = mem_read8(ctx, oa);
    ctx->r[0] = v; }
L_0031FE90:
    BUDGET(0x0031FE90u, 1);
    /* 0031FE90 E3500002 */
    {
    uint32_t b = 0x00000002u;
    uint32_t a = ctx->r[0];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
L_0031FE94:
    BUDGET(0x0031FE94u, 1);
    /* 0031FE94 0A000015 */
    if (C_EQ) {
    goto L_0031FEF0;
    }
L_0031FE98:
    BUDGET(0x0031FE98u, 1);
    /* 0031FE98 E3500003 */
    {
    uint32_t b = 0x00000003u;
    uint32_t a = ctx->r[0];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
L_0031FE9C:
    BUDGET(0x0031FE9Cu, 1);
    /* 0031FE9C 0A00001A */
    if (C_EQ) {
    goto L_0031FF0C;
    }
L_0031FEA0:
    BUDGET(0x0031FEA0u, 1);
    /* 0031FEA0 E3500004 */
    {
    uint32_t b = 0x00000004u;
    uint32_t a = ctx->r[0];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
L_0031FEA4:
    BUDGET(0x0031FEA4u, 1);
    /* 0031FEA4 1A000010 */
    if (C_NE) {
    goto L_0031FEEC;
    }
L_0031FEA8:
    BUDGET(0x0031FEA8u, 1);
    /* 0031FEA8 E59F007C */
    { uint32_t base = 0x0031FEB0u, oa = base + 0x7Cu;
    uint32_t v = mem_read32(ctx, oa);
    ctx->r[0] = v; }
L_0031FEAC:
    BUDGET(0x0031FEACu, 1);
    /* 0031FEAC E19000D4 */
    { uint32_t base = ctx->r[0], oa = base + ctx->r[4];
    uint32_t v = (uint32_t)(int32_t)(int8_t)mem_read8(ctx, oa); ctx->r[0] = v; }
L_0031FEB0:
    BUDGET(0x0031FEB0u, 1);
    /* 0031FEB0 E3500002 */
    {
    uint32_t b = 0x00000002u;
    uint32_t a = ctx->r[0];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
L_0031FEB4:
    BUDGET(0x0031FEB4u, 2);
    /* 0031FEB4 BA00000C */
    if (C_LT) {
    goto L_0031FEEC;
    }
    /* 0031FEB8 EBFFCF28 */
    ctx->r[14] = 0x0031FEBCu; ctx->r[15] = 0x00313B60u;
    CALL(f_00313B60);
    RETURNED(0x0031FEBCu);
L_0031FEBC:
    BUDGET(0x0031FEBCu, 1);
    /* 0031FEBC EBFEF469 */
    ctx->r[14] = 0x0031FEC0u; ctx->r[15] = 0x002DD068u;
    CALL(f_002DD068);
    RETURNED(0x0031FEC0u);
L_0031FEC0:
    BUDGET(0x0031FEC0u, 1);
    /* 0031FEC0 EB04BCF0 */
    ctx->r[14] = 0x0031FEC4u; ctx->r[15] = 0x0044F288u;
    CALL(f_0044F288);
    RETURNED(0x0031FEC4u);
L_0031FEC4:
    BUDGET(0x0031FEC4u, 1);
    /* 0031FEC4 E2840B01 */
    {
    uint32_t b = 0x00000400u;
    uint32_t a = ctx->r[4];
    uint32_t r = a + b;
    ctx->r[0] = r;
    }
L_0031FEC8:
    BUDGET(0x0031FEC8u, 1);
    /* 0031FEC8 E2800058 */
    {
    uint32_t b = 0x00000058u;
    uint32_t a = ctx->r[0];
    uint32_t r = a + b;
    ctx->r[0] = r;
    }
L_0031FECC:
    BUDGET(0x0031FECCu, 1);
    /* 0031FECC EB04CDB9 */
    ctx->r[14] = 0x0031FED0u; ctx->r[15] = 0x004535B8u;
    CALL(f_004535B8);
    RETURNED(0x0031FED0u);
L_0031FED0:
    BUDGET(0x0031FED0u, 1);
    /* 0031FED0 E2840E3B */
    {
    uint32_t b = 0x000003B0u;
    uint32_t a = ctx->r[4];
    uint32_t r = a + b;
    ctx->r[0] = r;
    }
L_0031FED4:
    BUDGET(0x0031FED4u, 1);
    /* 0031FED4 EB04DBDA */
    ctx->r[14] = 0x0031FED8u; ctx->r[15] = 0x00456E44u;
    CALL(f_00456E44);
    RETURNED(0x0031FED8u);
L_0031FED8:
    BUDGET(0x0031FED8u, 1);
    /* 0031FED8 E3500000 */
    {
    uint32_t b = 0x00000000u;
    uint32_t a = ctx->r[0];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
L_0031FEDC:
    BUDGET(0x0031FEDCu, 1);
    /* 0031FEDC 12840E3B */
    if (C_NE) {
    {
    uint32_t b = 0x000003B0u;
    uint32_t a = ctx->r[4];
    uint32_t r = a + b;
    ctx->r[0] = r;
    }
    }
L_0031FEE0:
    BUDGET(0x0031FEE0u, 1);
    /* 0031FEE0 1BFEF462 */
    if (C_NE) {
    ctx->r[14] = 0x0031FEE4u; ctx->r[15] = 0x002DD070u;
    CALL(f_00484A08);
    RETURNED(0x0031FEE4u);
    }
L_0031FEE4:
    BUDGET(0x0031FEE4u, 1);
    /* 0031FEE4 E3A00000 */
    {
    uint32_t b = 0x00000000u;
    uint32_t r = b;
    ctx->r[0] = r;
    }
L_0031FEE8:
    BUDGET(0x0031FEE8u, 1);
    /* 0031FEE8 E5C40489 */
    { uint32_t base = ctx->r[4], oa = base + 0x489u;
    mem_write8(ctx, oa, (uint8_t)ctx->r[0]);
    }
L_0031FEEC:
    BUDGET(0x0031FEECu, 1);
    {
        uint32_t sp = ctx->r[13];
        ctx->r[13] = sp + 8;
        ctx->r[4] = mem_read32(ctx, sp);
        RETURN_TO(mem_read32(ctx, sp + 4));
    }
L_0031FEF0:
    BUDGET(0x0031FEF0u, 2);
    ctx->r[0] = mem_read32(ctx, ctx->r[4]);
    ctx->r[14] = 0x0031FEF8u; ctx->r[15] = 0x004558E4u;
    CALL(f_004558E4);
    RETURNED(0x0031FEF8u);
L_0031FEF8:
    BUDGET(0x0031FEF8u, 5);
    ctx->n = 0; ctx->c = 1; ctx->v = 0;
    if (ctx->r[0] == 0) {
        ctx->z = 1;
        goto L_0031FEEC;
    }
    ctx->z = 0;
    ctx->r[0] = 3;
    goto L_0031FF24;
L_0031FF0C:
    BUDGET(0x0031FF0Cu, 2);
    ctx->r[0] = ctx->r[4];
    ctx->r[14] = 0x0031FF14u; ctx->r[15] = 0x00453120u;
    CALL(f_00453120);
    RETURNED(0x0031FF14u);
L_0031FF14:
    BUDGET(0x0031FF14u, 4);
    ctx->n = 0; ctx->c = 1; ctx->v = 0;
    if (ctx->r[0] == 0) {
        ctx->z = 1;
        goto L_0031FEEC;
    }
    ctx->z = 0;
    ctx->r[0] = 4;
L_0031FF24:
    BUDGET(0x0031FF24u, 2);
    mem_write8(ctx, ctx->r[4] + 0x488u, (uint8_t)ctx->r[0]);
    {
        uint32_t sp = ctx->r[13];
        ctx->r[13] = sp + 8;
        ctx->r[4] = mem_read32(ctx, sp);
