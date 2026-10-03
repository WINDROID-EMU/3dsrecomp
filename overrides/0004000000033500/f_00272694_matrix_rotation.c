void f_00272694(Context *ctx) {
    uint32_t target = ctx->r[15];
    if (LIKELY(target == 0x00272694u && !ctx->thumb)) goto L_00272694;
dispatch:
    if (!ctx->thumb) switch (target) {
    case 0x00272694u: goto L_00272694;
    case 0x002726F0u: goto L_002726F0;
    case 0x00272718u: goto L_00272718;
    case 0x00272724u: goto L_00272724;
    case 0x00272784u: goto L_00272784;
    case 0x002727A4u: goto L_002727A4;
    case 0x002727ACu: goto L_002727AC;
    case 0x0036C174u: goto L_0036C174;
    case 0x0036C178u: goto L_0036C178;
    case 0x0036C17Cu: goto L_0036C17C;
    case 0x0036C180u: goto L_0036C180;
    case 0x0036C184u: goto L_0036C184;
    case 0x0036C188u: goto L_0036C188;
    case 0x0036C18Cu: goto L_0036C18C;
    case 0x0036C190u: goto L_0036C190;
    case 0x0036C194u: goto L_0036C194;
    case 0x0036C198u: goto L_0036C198;
    case 0x0036C19Cu: goto L_0036C19C;
    case 0x0036C1A0u: goto L_0036C1A0;
    case 0x0036C1A4u: goto L_0036C1A4;
    case 0x0036C1A8u: goto L_0036C1A8;
    case 0x0036C1ACu: goto L_0036C1AC;
    case 0x0036C1B0u: goto L_0036C1B0;
    case 0x0036C1B4u: goto L_0036C1B4;
    case 0x0036C1B8u: goto L_0036C1B8;
    case 0x0036C1BCu: goto L_0036C1BC;
    case 0x0036C1C0u: goto L_0036C1C0;
    case 0x0036C1C4u: goto L_0036C1C4;
    case 0x0036C1C8u: goto L_0036C1C8;
    case 0x0036C1CCu: goto L_0036C1CC;
    case 0x0036C1D0u: goto L_0036C1D0;
    case 0x0036C1D4u: goto L_0036C1D4;
    case 0x0036C1D8u: goto L_0036C1D8;
    case 0x0036C1DCu: goto L_0036C1DC;
    case 0x0036C1E0u: goto L_0036C1E0;
    case 0x0036C1E4u: goto L_0036C1E4;
    case 0x0036C1E8u: goto L_0036C1E8;
    case 0x0036C1ECu: goto L_0036C1EC;
    case 0x0036C1F0u: goto L_0036C1F0;
    case 0x0036C1F4u: goto L_0036C1F4;
    case 0x0036C1F8u: goto L_0036C1F8;
    case 0x0036C1FCu: goto L_0036C1FC;
    case 0x0036C200u: goto L_0036C200;
    case 0x0036C204u: goto L_0036C204;
    case 0x0036C208u: goto L_0036C208;
    case 0x0036C20Cu: goto L_0036C20C;
    case 0x0036C210u: goto L_0036C210;
    case 0x0036C214u: goto L_0036C214;
    case 0x0036C218u: goto L_0036C218;
    case 0x0036C21Cu: goto L_0036C21C;
    case 0x0036C220u: goto L_0036C220;
    case 0x0036C224u: goto L_0036C224;
    case 0x0036C228u: goto L_0036C228;
    case 0x0036C22Cu: goto L_0036C22C;
    case 0x0036C230u: goto L_0036C230;
    case 0x0036C234u: goto L_0036C234;
    case 0x0036C238u: goto L_0036C238;
    case 0x0036C23Cu: goto L_0036C23C;
    case 0x0036C240u: goto L_0036C240;
    case 0x0036C244u: goto L_0036C244;
    case 0x0036C248u: goto L_0036C248;
    case 0x0036C24Cu: goto L_0036C24C;
    case 0x0036C250u: goto L_0036C250;
    case 0x0036C254u: goto L_0036C254;
    }
    ctx->r[15] = target;
    CALL(recomp_call);
    return;
L_00272694:
    BUDGET(0x00272694u, 23);
    /* 00272694 E92D4070 */
    { uint32_t base = ctx->r[13], p = base - 16, fin = base - 16;
    mem_write32(ctx, p, ctx->r[4]); p += 4;
    mem_write32(ctx, p, ctx->r[5]); p += 4;
    mem_write32(ctx, p, ctx->r[6]); p += 4;
    mem_write32(ctx, p, ctx->r[14]); p += 4;
    ctx->r[13] = fin;
    (void)p; (void)fin; }
    /* 00272698 E3510000 */
    {
    uint32_t b = 0x00000000u;
    uint32_t a = ctx->r[1];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
    /* 0027269C E1A04002 */
    {
    uint32_t m = ctx->r[2];
    uint32_t b = m;
    uint32_t r = b;
    ctx->r[4] = r;
    }
    /* 002726A0 ED2D8B04 */
    { uint32_t base = ctx->r[13], p = base - 0x10u;
    mem_write32(ctx, p, ctx->vfp[16]);
    mem_write32(ctx, p + 4, ctx->vfp[17]);
    p += 8;
    mem_write32(ctx, p, ctx->vfp[18]);
    mem_write32(ctx, p + 4, ctx->vfp[19]);
    p += 8;
    ctx->r[13] = base - 0x10u;
    (void)p; }
    /* 002726A4 0A00003E */
    if (C_EQ) {
    goto L_002727A4;
    }
    /* 002726A8 E3510001 */
    {
    uint32_t b = 0x00000001u;
    uint32_t a = ctx->r[1];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
    /* 002726AC 0A00003E */
    if (C_EQ) {
    goto L_002727AC;
    }
    /* 002726B0 E3510002 */
    {
    uint32_t b = 0x00000002u;
    uint32_t a = ctx->r[1];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
    /* 002726B4 1A00003A */
    if (C_NE) {
    goto L_002727A4;
    }
    /* 002726B8 ED930A73 */
    { uint32_t base = ctx->r[3], p = base + 0x1CCu;
    ctx->vfp[0] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002726BC ED9F8A63 */
    { uint32_t base = 0x002726C4u, p = base + 0x18Cu;
    ctx->vfp[16] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002726C0 ED9F9A61 */
    { uint32_t base = 0x002726C8u, p = base + 0x184u;
    ctx->vfp[18] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002726C4 EEBD0AC0 */
    ctx->vfp[0] = vfp_to_s32((double)vfp_s(ctx, 0));
    /* 002726C8 E3A01001 */
    {
    uint32_t b = 0x00000001u;
    uint32_t r = b;
    ctx->r[1] = r;
    }
    /* 002726CC EE100A10 */
    ctx->r[0] = ctx->vfp[0];
    /* 002726D0 ED930A74 */
    { uint32_t base = ctx->r[3], p = base + 0x1D0u;
    ctx->vfp[0] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002726D4 EEBD0AC0 */
    ctx->vfp[0] = vfp_to_s32((double)vfp_s(ctx, 0));
    /* 002726D8 E6BF5070 */
    ctx->r[5] = (uint32_t)(int32_t)(int16_t)ctx->r[0];
    /* 002726DC EE100A10 */
    ctx->r[0] = ctx->vfp[0];
    /* 002726E0 EEB00A48 */
    { ctx->vfp[0] = ctx->vfp[16]; }
    /* 002726E4 E6BF6070 */
    ctx->r[6] = (uint32_t)(int32_t)(int16_t)ctx->r[0];
    /* 002726E8 E1A00004 */
    {
    uint32_t m = ctx->r[4];
    uint32_t b = m;
    uint32_t r = b;
    ctx->r[0] = r;
    }
    /* 002726EC EB03FAD0 */
    ctx->r[14] = 0x002726F0u; ctx->r[15] = 0x00371234u;
    CALL(f_00371234);
    RETURNED(0x002726F0u);
L_002726F0:
    BUDGET(0x002726F0u, 10);
    /* 002726F0 E3560000 */
    {
    uint32_t b = 0x00000000u;
    uint32_t a = ctx->r[6];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
    /* 002726F4 0A000022 */
    if (C_EQ) {
    goto L_00272784;
    }
    /* 002726F8 EE006A90 */
    ctx->vfp[1] = ctx->r[6];
    /* 002726FC EEF80AE0 */
    vfp_set_s(ctx, 1, (float)(double)(int32_t)ctx->vfp[1]);
    /* 00272700 EE608A89 */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 4, 0, 17, 1, 18); else { float a = vfp_s(ctx, 1), b = vfp_s(ctx, 18), acc = vfp_s(ctx, 17); vfp_set_s(ctx, 17, a * b); }
    /* 00272704 EEF48A48 */
    vfp_compare(ctx, vfp_s(ctx, 17), vfp_s(ctx, 16));
    /* 00272708 EEF1FA10 */
    { uint32_t f = *ctx->fpscr; ctx->n = f >> 31; ctx->z = (f >> 30) & 1; ctx->c = (f >> 29) & 1; ctx->v = (f >> 28) & 1; }
    /* 0027270C 0A00001C */
    if (C_EQ) {
    goto L_00272784;
    }
    /* 00272710 EEB00A68 */
    { ctx->vfp[0] = ctx->vfp[17]; }
    /* 00272714 EB040035 */
    ctx->r[14] = 0x00272718u; ctx->r[15] = 0x003727F0u;
    CALL(f_003727F0);
    RETURNED(0x00272718u);
L_00272718:
    BUDGET(0x00272718u, 3);
    /* 00272718 EEB08A40 */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 9, 0, 16, 16, 0); else { ctx->vfp[16] = ctx->vfp[0]; }
    /* 0027271C EEB00A68 */
    { ctx->vfp[0] = ctx->vfp[17]; }
    /* 00272720 EB03FFD3 */
    ctx->r[14] = 0x00272724u; ctx->r[15] = 0x00372674u;
    CALL(f_00372674);
    RETURNED(0x00272724u);
L_00272724:
    BUDGET(0x00272724u, 24);
    {
        uint32_t base = ctx->r[4];
        float sin_val = vfp_s(ctx, 16);
        float cos_val = vfp_s(ctx, 0);

        // Row 0
        union { uint32_t u; float f; } r0_0, r0_2, r0_new0, r0_new2;
        r0_0.u = mem_read32(ctx, base + 0x0u);
        r0_2.u = mem_read32(ctx, base + 0x8u);
        r0_new0.f = r0_0.f * cos_val - r0_2.f * sin_val;
        r0_new2.f = r0_0.f * sin_val + r0_2.f * cos_val;
        mem_write32(ctx, base + 0x0u, r0_new0.u);
        mem_write32(ctx, base + 0x8u, r0_new2.u);

        // Row 1
        union { uint32_t u; float f; } r1_0, r1_2, r1_new0, r1_new2;
        r1_0.u = mem_read32(ctx, base + 0x10u);
        r1_2.u = mem_read32(ctx, base + 0x18u);
        r1_new0.f = r1_0.f * cos_val - r1_2.f * sin_val;
        r1_new2.f = r1_0.f * sin_val + r1_2.f * cos_val;
        mem_write32(ctx, base + 0x10u, r1_new0.u);
        mem_write32(ctx, base + 0x18u, r1_new2.u);

        // Row 2
        union { uint32_t u; float f; } r2_0, r2_2, r2_new0, r2_new2;
        r2_0.u = mem_read32(ctx, base + 0x20u);
        r2_2.u = mem_read32(ctx, base + 0x28u);
        r2_new0.f = r2_0.f * cos_val - r2_2.f * sin_val;
        r2_new2.f = r2_0.f * sin_val + r2_2.f * cos_val;
        mem_write32(ctx, base + 0x20u, r2_new0.u);
        mem_write32(ctx, base + 0x28u, r2_new2.u);

        ctx->vfp[1] = r2_new2.u;
        ctx->vfp[2] = r2_2.u;
        ctx->vfp[3] = r2_new0.u;
    }
L_00272784:
    BUDGET(0x00272784u, 8);
    /* 00272784 E3550000 */
    {
    uint32_t b = 0x00000000u;
    uint32_t a = ctx->r[5];
    uint32_t r = a - b;
    ctx->n = r >> 31; ctx->z = r == 0;
    ctx->c = a >= b; ctx->v = ((a ^ b) & (a ^ r)) >> 31;
    }
    /* 00272788 0A000005 */
    if (C_EQ) {
    goto L_002727A4;
    }
    /* 0027278C EE005A10 */
    ctx->vfp[0] = ctx->r[5];
    /* 00272790 E3A01001 */
    {
    uint32_t b = 0x00000001u;
    uint32_t r = b;
    ctx->r[1] = r;
    }
    /* 00272794 E1A00004 */
    {
    uint32_t m = ctx->r[4];
    uint32_t b = m;
    uint32_t r = b;
    ctx->r[0] = r;
    }
    /* 00272798 EEB80AC0 */
    vfp_set_s(ctx, 0, (float)(double)(int32_t)ctx->vfp[0]);
    /* 0027279C EE200A09 */
    { float a = vfp_s(ctx, 0), b = vfp_s(ctx, 18), acc = vfp_s(ctx, 0); vfp_set_s(ctx, 0, a * b); }
    /* 002727A0 EB03DA1B */
    ctx->r[14] = 0x002727A4u; ctx->r[15] = 0x00369014u;
    CALL(f_00369014);
    RETURNED(0x002727A4u);
L_002727A4:
    BUDGET(0x002727A4u, 2);
    /* 002727A4 ECBD8B04 */
    { uint32_t base = ctx->r[13], p = base;
    ctx->vfp[16] = mem_read32(ctx, p);
    ctx->vfp[17] = mem_read32(ctx, p + 4);
    p += 8;
    ctx->vfp[18] = mem_read32(ctx, p);
    ctx->vfp[19] = mem_read32(ctx, p + 4);
    p += 8;
    ctx->r[13] = base + 0x10u;
    (void)p; }
    /* 002727A8 E8BD8070 */
    { uint32_t base = ctx->r[13], p = base, fin = base + 16;
    ctx->r[13] = fin;
    ctx->r[4] = mem_read32(ctx, p); p += 4;
    ctx->r[5] = mem_read32(ctx, p); p += 4;
    ctx->r[6] = mem_read32(ctx, p); p += 4;
    RETURN_TO(mem_read32(ctx, p)); }
L_002727AC:
    BUDGET(0x002727ACu, 40);
    /* 002727AC ED930A75 */
    { uint32_t base = ctx->r[3], p = base + 0x1D4u;
    ctx->vfp[0] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002727B0 ED941A00 */
    { uint32_t base = ctx->r[4], p = base + 0x0u;
    ctx->vfp[2] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002727B4 E2802FBF */
    {
    uint32_t b = 0x000002FCu;
    uint32_t a = ctx->r[0];
    uint32_t r = a + b;
    ctx->r[2] = r;
    }
    /* 002727B8 EEF00A40 */
    { ctx->vfp[1] = ctx->vfp[0]; }
    /* 002727BC E1A01004 */
    {
    uint32_t m = ctx->r[4];
    uint32_t b = m;
    uint32_t r = b;
    ctx->r[1] = r;
    }
    /* 002727C0 E1A00004 */
    {
    uint32_t m = ctx->r[4];
    uint32_t b = m;
    uint32_t r = b;
    ctx->r[0] = r;
    }
    /* 002727C4 EE610A20 */
    { float a = vfp_s(ctx, 2), b = vfp_s(ctx, 1), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
    /* 002727C8 EDC40A00 */
    { uint32_t base = ctx->r[4], p = base + 0x0u;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    (void)p; }
    /* 002727CC EEF00A40 */
    { ctx->vfp[1] = ctx->vfp[0]; }
    /* 002727D0 ED941A04 */
    { uint32_t base = ctx->r[4], p = base + 0x10u;
    ctx->vfp[2] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002727D4 EE610A20 */
    { float a = vfp_s(ctx, 2), b = vfp_s(ctx, 1), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
    /* 002727D8 EDC40A04 */
    { uint32_t base = ctx->r[4], p = base + 0x10u;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    (void)p; }
    /* 002727DC EEF00A40 */
    { ctx->vfp[1] = ctx->vfp[0]; }
    /* 002727E0 ED941A08 */
    { uint32_t base = ctx->r[4], p = base + 0x20u;
    ctx->vfp[2] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002727E4 EE610A20 */
    { float a = vfp_s(ctx, 2), b = vfp_s(ctx, 1), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
    /* 002727E8 EDC40A08 */
    { uint32_t base = ctx->r[4], p = base + 0x20u;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    (void)p; }
    /* 002727EC EEF00A40 */
    { ctx->vfp[1] = ctx->vfp[0]; }
    /* 002727F0 ED941A01 */
    { uint32_t base = ctx->r[4], p = base + 0x4u;
    ctx->vfp[2] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 002727F4 EE610A20 */
    { float a = vfp_s(ctx, 2), b = vfp_s(ctx, 1), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
    /* 002727F8 EDC40A01 */
    { uint32_t base = ctx->r[4], p = base + 0x4u;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    (void)p; }
    /* 002727FC EEF00A40 */
    { ctx->vfp[1] = ctx->vfp[0]; }
    /* 00272800 ED941A05 */
    { uint32_t base = ctx->r[4], p = base + 0x14u;
    ctx->vfp[2] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 00272804 EE610A20 */
    { float a = vfp_s(ctx, 2), b = vfp_s(ctx, 1), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
    /* 00272808 EDC40A05 */
    { uint32_t base = ctx->r[4], p = base + 0x14u;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    (void)p; }
    /* 0027280C EEF00A40 */
    { ctx->vfp[1] = ctx->vfp[0]; }
    /* 00272810 ED941A09 */
    { uint32_t base = ctx->r[4], p = base + 0x24u;
    ctx->vfp[2] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 00272814 EE610A20 */
    { float a = vfp_s(ctx, 2), b = vfp_s(ctx, 1), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
    /* 00272818 EDC40A09 */
    { uint32_t base = ctx->r[4], p = base + 0x24u;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    (void)p; }
    /* 0027281C EDD40A02 */
    { uint32_t base = ctx->r[4], p = base + 0x8u;
    ctx->vfp[1] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 00272820 EE600A80 */
    { float a = vfp_s(ctx, 1), b = vfp_s(ctx, 0), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
    /* 00272824 EDC40A02 */
    { uint32_t base = ctx->r[4], p = base + 0x8u;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    (void)p; }
    /* 00272828 EDD40A06 */
    { uint32_t base = ctx->r[4], p = base + 0x18u;
    ctx->vfp[1] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 0027282C EE600A80 */
    { float a = vfp_s(ctx, 1), b = vfp_s(ctx, 0), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
    /* 00272830 EDC40A06 */
    { uint32_t base = ctx->r[4], p = base + 0x18u;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    (void)p; }
    /* 00272834 EDD40A0A */
    { uint32_t base = ctx->r[4], p = base + 0x28u;
    ctx->vfp[1] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
    /* 00272838 EE200A80 */
    { float a = vfp_s(ctx, 1), b = vfp_s(ctx, 0), acc = vfp_s(ctx, 0); vfp_set_s(ctx, 0, a * b); }
    /* 0027283C ED840A0A */
    { uint32_t base = ctx->r[4], p = base + 0x28u;
    mem_write32(ctx, p, ctx->vfp[0]);
    p += 4;
    (void)p; }
    /* 00272840 ECBD8B04 */
    { uint32_t base = ctx->r[13], p = base;
    ctx->vfp[16] = mem_read32(ctx, p);
    ctx->vfp[17] = mem_read32(ctx, p + 4);
    p += 8;
    ctx->vfp[18] = mem_read32(ctx, p);
    ctx->vfp[19] = mem_read32(ctx, p + 4);
    p += 8;
    ctx->r[13] = base + 0x10u;
    (void)p; }
    /* 00272844 E8BD4070 */
    { uint32_t base = ctx->r[13], p = base, fin = base + 16;
    ctx->r[13] = fin;
    ctx->r[4] = mem_read32(ctx, p); p += 4;
    ctx->r[5] = mem_read32(ctx, p); p += 4;
    ctx->r[6] = mem_read32(ctx, p); p += 4;
    ctx->r[14] = mem_read32(ctx, p); p += 4;
    (void)p; (void)fin; }
    /* 00272848 EA03E649 */
    goto L_0036C174;
L_0036C174:
    BUDGET(0x0036C174u, 1);
    /* 0036C174 ED2D8B06 */
    { uint32_t base = ctx->r[13], p = base - 0x18u;
    mem_write32(ctx, p, ctx->vfp[16]);
    mem_write32(ctx, p + 4, ctx->vfp[17]);
    p += 8;
    mem_write32(ctx, p, ctx->vfp[18]);
    mem_write32(ctx, p + 4, ctx->vfp[19]);
    p += 8;
    mem_write32(ctx, p, ctx->vfp[20]);
    mem_write32(ctx, p + 4, ctx->vfp[21]);
    p += 8;
    ctx->r[13] = base - 0x18u;
    (void)p; }
L_0036C178:
    BUDGET(0x0036C178u, 1);
    /* 0036C178 EDD11A03 */
    { uint32_t base = ctx->r[1], p = base + 0xCu;
    ctx->vfp[3] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C17C:
    BUDGET(0x0036C17Cu, 1);
    /* 0036C17C EDD13A07 */
    { uint32_t base = ctx->r[1], p = base + 0x1Cu;
    ctx->vfp[7] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C180:
    BUDGET(0x0036C180u, 1);
    /* 0036C180 EDD15A0B */
    { uint32_t base = ctx->r[1], p = base + 0x2Cu;
    ctx->vfp[11] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C184:
    BUDGET(0x0036C184u, 1);
    /* 0036C184 ECB26A04 */
    { uint32_t base = ctx->r[2], p = base;
    ctx->vfp[12] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[13] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[14] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[15] = mem_read32(ctx, p);
    p += 4;
    ctx->r[2] = base + 0x10u;
    (void)p; }
L_0036C188:
    BUDGET(0x0036C188u, 1);
    /* 0036C188 ED91AA00 */
    { uint32_t base = ctx->r[1], p = base + 0x0u;
    ctx->vfp[20] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C18C:
    BUDGET(0x0036C18Cu, 1);
    /* 0036C18C EDD1AA04 */
    { uint32_t base = ctx->r[1], p = base + 0x10u;
    ctx->vfp[21] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C190:
    BUDGET(0x0036C190u, 1);
    /* 0036C190 EE260A0A */
    { float a = vfp_s(ctx, 12), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 0); vfp_set_s(ctx, 0, a * b); }
L_0036C194:
    BUDGET(0x0036C194u, 1);
    /* 0036C194 EE660A8A */
    { float a = vfp_s(ctx, 13), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, a * b); }
L_0036C198:
    BUDGET(0x0036C198u, 1);
    /* 0036C198 EE271A0A */
    { float a = vfp_s(ctx, 14), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 2); vfp_set_s(ctx, 2, a * b); }
L_0036C19C:
    BUDGET(0x0036C19Cu, 1);
    /* 0036C19C EE471A8A */
    { float a = vfp_s(ctx, 15), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 3); vfp_set_s(ctx, 3, acc + a * b); }
L_0036C1A0:
    BUDGET(0x0036C1A0u, 1);
    /* 0036C1A0 ED91AA08 */
    { uint32_t base = ctx->r[1], p = base + 0x20u;
    ctx->vfp[20] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C1A4:
    BUDGET(0x0036C1A4u, 1);
    /* 0036C1A4 EE262A2A */
    { float a = vfp_s(ctx, 12), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 4); vfp_set_s(ctx, 4, a * b); }
L_0036C1A8:
    BUDGET(0x0036C1A8u, 1);
    /* 0036C1A8 EE662AAA */
    { float a = vfp_s(ctx, 13), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 5); vfp_set_s(ctx, 5, a * b); }
L_0036C1AC:
    BUDGET(0x0036C1ACu, 1);
    /* 0036C1AC EE273A2A */
    { float a = vfp_s(ctx, 14), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 6); vfp_set_s(ctx, 6, a * b); }
L_0036C1B0:
    BUDGET(0x0036C1B0u, 1);
    /* 0036C1B0 EE473AAA */
    { float a = vfp_s(ctx, 15), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 7); vfp_set_s(ctx, 7, acc + a * b); }
L_0036C1B4:
    BUDGET(0x0036C1B4u, 1);
    /* 0036C1B4 ECB28A04 */
    { uint32_t base = ctx->r[2], p = base;
    ctx->vfp[16] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[17] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[18] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[19] = mem_read32(ctx, p);
    p += 4;
    ctx->r[2] = base + 0x10u;
    (void)p; }
L_0036C1B8:
    BUDGET(0x0036C1B8u, 1);
    /* 0036C1B8 EDD1AA01 */
    { uint32_t base = ctx->r[1], p = base + 0x4u;
    ctx->vfp[21] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C1BC:
    BUDGET(0x0036C1BCu, 1);
    /* 0036C1BC EE264A0A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 4, 0, 8, 12, 20); else { float a = vfp_s(ctx, 12), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 8); vfp_set_s(ctx, 8, a * b); }
L_0036C1C0:
    BUDGET(0x0036C1C0u, 1);
    /* 0036C1C0 EE664A8A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 4, 0, 9, 13, 20); else { float a = vfp_s(ctx, 13), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 9); vfp_set_s(ctx, 9, a * b); }
L_0036C1C4:
    BUDGET(0x0036C1C4u, 1);
    /* 0036C1C4 EE275A0A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 4, 0, 10, 14, 20); else { float a = vfp_s(ctx, 14), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 10); vfp_set_s(ctx, 10, a * b); }
L_0036C1C8:
    BUDGET(0x0036C1C8u, 1);
    /* 0036C1C8 EE475A8A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 11, 15, 20); else { float a = vfp_s(ctx, 15), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 11); vfp_set_s(ctx, 11, acc + a * b); }
L_0036C1CC:
    BUDGET(0x0036C1CCu, 1);
    /* 0036C1CC EC926A04 */
    { uint32_t base = ctx->r[2], p = base;
    ctx->vfp[12] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[13] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[14] = mem_read32(ctx, p);
    p += 4;
    ctx->vfp[15] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C1D0:
    BUDGET(0x0036C1D0u, 1);
    /* 0036C1D0 ED91AA05 */
    { uint32_t base = ctx->r[1], p = base + 0x14u;
    ctx->vfp[20] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C1D4:
    BUDGET(0x0036C1D4u, 1);
    /* 0036C1D4 EE080A2A */
    { float a = vfp_s(ctx, 16), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 0); vfp_set_s(ctx, 0, acc + a * b); }
L_0036C1D8:
    BUDGET(0x0036C1D8u, 1);
    /* 0036C1D8 EE480AAA */
    { float a = vfp_s(ctx, 17), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, acc + a * b); }
L_0036C1DC:
    BUDGET(0x0036C1DCu, 1);
    /* 0036C1DC EE091A2A */
    { float a = vfp_s(ctx, 18), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 2); vfp_set_s(ctx, 2, acc + a * b); }
L_0036C1E0:
    BUDGET(0x0036C1E0u, 1);
    /* 0036C1E0 EE491AAA */
    { float a = vfp_s(ctx, 19), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 3); vfp_set_s(ctx, 3, acc + a * b); }
L_0036C1E4:
    BUDGET(0x0036C1E4u, 1);
    /* 0036C1E4 EDD1AA09 */
    { uint32_t base = ctx->r[1], p = base + 0x24u;
    ctx->vfp[21] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C1E8:
    BUDGET(0x0036C1E8u, 1);
    /* 0036C1E8 EE082A0A */
    { float a = vfp_s(ctx, 16), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 4); vfp_set_s(ctx, 4, acc + a * b); }
L_0036C1EC:
    BUDGET(0x0036C1ECu, 1);
    /* 0036C1EC EE482A8A */
    { float a = vfp_s(ctx, 17), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 5); vfp_set_s(ctx, 5, acc + a * b); }
L_0036C1F0:
    BUDGET(0x0036C1F0u, 1);
    /* 0036C1F0 EE093A0A */
    { float a = vfp_s(ctx, 18), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 6); vfp_set_s(ctx, 6, acc + a * b); }
L_0036C1F4:
    BUDGET(0x0036C1F4u, 1);
    /* 0036C1F4 EE493A8A */
    { float a = vfp_s(ctx, 19), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 7); vfp_set_s(ctx, 7, acc + a * b); }
L_0036C1F8:
    BUDGET(0x0036C1F8u, 1);
    /* 0036C1F8 ED91AA02 */
    { uint32_t base = ctx->r[1], p = base + 0x8u;
    ctx->vfp[20] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C1FC:
    BUDGET(0x0036C1FCu, 1);
    /* 0036C1FC EE084A2A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 8, 16, 21); else { float a = vfp_s(ctx, 16), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 8); vfp_set_s(ctx, 8, acc + a * b); }
L_0036C200:
    BUDGET(0x0036C200u, 1);
    /* 0036C200 EE484AAA */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 9, 17, 21); else { float a = vfp_s(ctx, 17), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 9); vfp_set_s(ctx, 9, acc + a * b); }
L_0036C204:
    BUDGET(0x0036C204u, 1);
    /* 0036C204 EE095A2A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 10, 18, 21); else { float a = vfp_s(ctx, 18), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 10); vfp_set_s(ctx, 10, acc + a * b); }
L_0036C208:
    BUDGET(0x0036C208u, 1);
    /* 0036C208 EE495AAA */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 11, 19, 21); else { float a = vfp_s(ctx, 19), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 11); vfp_set_s(ctx, 11, acc + a * b); }
L_0036C20C:
    BUDGET(0x0036C20Cu, 1);
    /* 0036C20C EDD1AA06 */
    { uint32_t base = ctx->r[1], p = base + 0x18u;
    ctx->vfp[21] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C210:
    BUDGET(0x0036C210u, 1);
    /* 0036C210 EE060A0A */
    { float a = vfp_s(ctx, 12), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 0); vfp_set_s(ctx, 0, acc + a * b); }
L_0036C214:
    BUDGET(0x0036C214u, 1);
    /* 0036C214 EE460A8A */
    { float a = vfp_s(ctx, 13), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 1); vfp_set_s(ctx, 1, acc + a * b); }
L_0036C218:
    BUDGET(0x0036C218u, 1);
    /* 0036C218 EE071A0A */
    { float a = vfp_s(ctx, 14), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 2); vfp_set_s(ctx, 2, acc + a * b); }
L_0036C21C:
    BUDGET(0x0036C21Cu, 1);
    /* 0036C21C EE471A8A */
    { float a = vfp_s(ctx, 15), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 3); vfp_set_s(ctx, 3, acc + a * b); }
L_0036C220:
    BUDGET(0x0036C220u, 1);
    /* 0036C220 ED91AA0A */
    { uint32_t base = ctx->r[1], p = base + 0x28u;
    ctx->vfp[20] = mem_read32(ctx, p);
    p += 4;
    (void)p; }
L_0036C224:
    BUDGET(0x0036C224u, 1);
    /* 0036C224 EE062A2A */
    { float a = vfp_s(ctx, 12), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 4); vfp_set_s(ctx, 4, acc + a * b); }
L_0036C228:
    BUDGET(0x0036C228u, 1);
    /* 0036C228 EE462AAA */
    { float a = vfp_s(ctx, 13), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 5); vfp_set_s(ctx, 5, acc + a * b); }
L_0036C22C:
    BUDGET(0x0036C22Cu, 1);
    /* 0036C22C EE073A2A */
    { float a = vfp_s(ctx, 14), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 6); vfp_set_s(ctx, 6, acc + a * b); }
L_0036C230:
    BUDGET(0x0036C230u, 1);
    /* 0036C230 EE473AAA */
    { float a = vfp_s(ctx, 15), b = vfp_s(ctx, 21), acc = vfp_s(ctx, 7); vfp_set_s(ctx, 7, acc + a * b); }
L_0036C234:
    BUDGET(0x0036C234u, 1);
    /* 0036C234 EE064A0A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 8, 12, 20); else { float a = vfp_s(ctx, 12), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 8); vfp_set_s(ctx, 8, acc + a * b); }
L_0036C238:
    BUDGET(0x0036C238u, 1);
    /* 0036C238 EE464A8A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 9, 13, 20); else { float a = vfp_s(ctx, 13), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 9); vfp_set_s(ctx, 9, acc + a * b); }
L_0036C23C:
    BUDGET(0x0036C23Cu, 1);
    /* 0036C23C EE075A0A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 10, 14, 20); else { float a = vfp_s(ctx, 14), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 10); vfp_set_s(ctx, 10, acc + a * b); }
L_0036C240:
    BUDGET(0x0036C240u, 1);
    /* 0036C240 EE475A8A */
    if (UNLIKELY(*ctx->fpscr & FPSCR_LEN)) vfp_vector(ctx, 0, 0, 11, 15, 20); else { float a = vfp_s(ctx, 15), b = vfp_s(ctx, 20), acc = vfp_s(ctx, 11); vfp_set_s(ctx, 11, acc + a * b); }
L_0036C244:
    BUDGET(0x0036C244u, 1);
    /* 0036C244 ECBD8B06 */
    { uint32_t base = ctx->r[13], p = base;
    ctx->vfp[16] = mem_read32(ctx, p);
    ctx->vfp[17] = mem_read32(ctx, p + 4);
    p += 8;
    ctx->vfp[18] = mem_read32(ctx, p);
    ctx->vfp[19] = mem_read32(ctx, p + 4);
    p += 8;
    ctx->vfp[20] = mem_read32(ctx, p);
    ctx->vfp[21] = mem_read32(ctx, p + 4);
    p += 8;
    ctx->r[13] = base + 0x18u;
    (void)p; }
L_0036C248:
    BUDGET(0x0036C248u, 1);
    /* 0036C248 E1A01000 */
    {
    uint32_t m = ctx->r[0];
    uint32_t b = m;
    uint32_t r = b;
    ctx->r[1] = r;
    }
L_0036C24C:
    BUDGET(0x0036C24Cu, 1);
    /* 0036C24C ECA10A04 */
    { uint32_t base = ctx->r[1], p = base;
    mem_write32(ctx, p, ctx->vfp[0]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[1]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[2]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[3]);
    p += 4;
    ctx->r[1] = base + 0x10u;
    (void)p; }
L_0036C250:
    BUDGET(0x0036C250u, 1);
    /* 0036C250 EC812A08 */
    { uint32_t base = ctx->r[1], p = base;
    mem_write32(ctx, p, ctx->vfp[4]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[5]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[6]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[7]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[8]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[9]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[10]);
    p += 4;
    mem_write32(ctx, p, ctx->vfp[11]);
    p += 4;
    (void)p; }
L_0036C254:
    BUDGET(0x0036C254u, 1);
    /* 0036C254 E12FFF1E */
    RETURN_TO(ctx->r[14]);
}

