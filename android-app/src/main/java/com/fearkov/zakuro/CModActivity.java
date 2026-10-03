package com.fearkov.zakuro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CModActivity extends Activity {

    public static final String EXTRA_TITLE_ID = "zakuro_title_id";
    public static final String EXTRA_GAME_NAME = "zakuro_game_name";
    public static final String EXTRA_ROM_PATH = "zakuro_rom_path";

    private String titleId = "0004000000033500";
    private String gameName = "Jogo 3DS";
    private String romPath = "";

    private File overridesDir;
    private File cacheDir;

    private TextView cmodTitle;
    private TextView cmodGameSub;
    private Button recompileWithModsBtn;

    private Button tabModsBtn;
    private Button tabDecompiledBtn;

    private LinearLayout modsContainer;
    private LinearLayout decompiledContainer;
    private LinearLayout emptyModsLayout;

    private ListView modsListView;
    private ListView decompiledListView;
    private EditText searchDecompiledEdit;

    // Editor overlay
    private LinearLayout editorOverlay;
    private TextView editorFileName;
    private TextView editorModeBadge;
    private EditText editorContent;
    private Button editorSaveBtn;
    private Button editorDeleteBtn;
    private Button editorCloseBtn;

    private File currentEditingFile = null;
    private boolean isReadOnly = false;

    private final List<File> modsList = new ArrayList<>();
    private final List<File> decompiledList = new ArrayList<>();
    private final List<File> filteredDecompiledList = new ArrayList<>();

    private ModFilesAdapter modsAdapter;
    private ModFilesAdapter decompiledAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmod);

        if (getIntent().hasExtra(EXTRA_TITLE_ID)) {
            titleId = getIntent().getStringExtra(EXTRA_TITLE_ID);
        }
        if (getIntent().hasExtra(EXTRA_GAME_NAME)) {
            gameName = getIntent().getStringExtra(EXTRA_GAME_NAME);
        }
        if (getIntent().hasExtra(EXTRA_ROM_PATH)) {
            romPath = getIntent().getStringExtra(EXTRA_ROM_PATH);
        }

        // Tentar recuperar do arquivo salvo se não vier via intent
        if (romPath == null || romPath.isEmpty()) {
            File pathFile = new File(getFilesDir(), "roms/selected_path.txt");
            if (pathFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pathFile)))) {
                    romPath = reader.readLine();
                } catch (Exception ignored) {}
            }
        }

        overridesDir = new File(getFilesDir(), "overrides/" + titleId);
        if (!overridesDir.exists()) overridesDir.mkdirs();

        cacheDir = new File(getFilesDir(), "cache/3dsrecomp/" + titleId);

        initViews();
        setupTabs();
        setupEditor();
        loadMods();
        loadDecompiled();
    }

    private void initViews() {
        ImageButton backButton = findViewById(R.id.backButton);
        cmodTitle = findViewById(R.id.cmodTitle);
        cmodGameSub = findViewById(R.id.cmodGameSub);
        recompileWithModsBtn = findViewById(R.id.recompileWithModsBtn);

        tabModsBtn = findViewById(R.id.tabModsBtn);
        tabDecompiledBtn = findViewById(R.id.tabDecompiledBtn);

        modsContainer = findViewById(R.id.modsContainer);
        decompiledContainer = findViewById(R.id.decompiledContainer);
        emptyModsLayout = findViewById(R.id.emptyModsLayout);

        modsListView = findViewById(R.id.modsListView);
        decompiledListView = findViewById(R.id.decompiledListView);
        searchDecompiledEdit = findViewById(R.id.searchDecompiledEdit);

        Button newModBtn = findViewById(R.id.newModBtn);
        Button refreshDecompiledBtn = findViewById(R.id.refreshDecompiledBtn);

        cmodGameSub.setText(gameName + " (" + titleId + ")");

        backButton.setOnClickListener(v -> finish());
        recompileWithModsBtn.setOnClickListener(v -> recompileWithMods());
        newModBtn.setOnClickListener(v -> promptCreateMod(null));
        refreshDecompiledBtn.setOnClickListener(v -> loadDecompiled());

        // Template buttons
        findViewById(R.id.templateHookBtn).setOnClickListener(v -> promptCreateMod("hook"));
        findViewById(R.id.templateReplaceBtn).setOnClickListener(v -> promptCreateMod("replace"));
        findViewById(R.id.templateCheatBtn).setOnClickListener(v -> promptCreateMod("cheat"));

        modsAdapter = new ModFilesAdapter(modsList, true);
        modsListView.setAdapter(modsAdapter);

        decompiledAdapter = new ModFilesAdapter(filteredDecompiledList, false);
        decompiledListView.setAdapter(decompiledAdapter);

        searchDecompiledEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDecompiled(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupTabs() {
        tabModsBtn.setOnClickListener(v -> {
            modsContainer.setVisibility(View.VISIBLE);
            decompiledContainer.setVisibility(View.GONE);
            tabModsBtn.setBackgroundTintList(getColorStateList(R.color.zakuro_blue));
            tabModsBtn.setTextColor(getColor(android.R.color.white));
            tabDecompiledBtn.setBackgroundTintList(getColorStateList(R.color.zakuro_surface));
            tabDecompiledBtn.setTextColor(getColor(R.color.zakuro_blue_dark));
            loadMods();
        });

        tabDecompiledBtn.setOnClickListener(v -> {
            modsContainer.setVisibility(View.GONE);
            decompiledContainer.setVisibility(View.VISIBLE);
            tabDecompiledBtn.setBackgroundTintList(getColorStateList(R.color.zakuro_blue));
            tabDecompiledBtn.setTextColor(getColor(android.R.color.white));
            tabModsBtn.setBackgroundTintList(getColorStateList(R.color.zakuro_surface));
            tabModsBtn.setTextColor(getColor(R.color.zakuro_blue_dark));
            loadDecompiled();
        });
    }

    private void setupEditor() {
        editorOverlay = findViewById(R.id.editorOverlay);
        editorFileName = findViewById(R.id.editorFileName);
        editorModeBadge = findViewById(R.id.editorModeBadge);
        editorContent = findViewById(R.id.editorContent);
        editorSaveBtn = findViewById(R.id.editorSaveBtn);
        editorDeleteBtn = findViewById(R.id.editorDeleteBtn);
        editorCloseBtn = findViewById(R.id.editorCloseBtn);

        editorCloseBtn.setOnClickListener(v -> {
            editorOverlay.setVisibility(View.GONE);
            currentEditingFile = null;
        });

        editorSaveBtn.setOnClickListener(v -> saveCurrentFile());
        editorDeleteBtn.setOnClickListener(v -> confirmDeleteCurrentFile());

        // Snippets
        findViewById(R.id.snipOverride).setOnClickListener(v -> insertSnippet("RECOMP_OVERRIDE(0x00100000) {\n    CALL(RECOMP_ORIGINAL(0x00100000));\n}\n"));
        findViewById(R.id.snipOriginal).setOnClickListener(v -> insertSnippet("CALL(RECOMP_ORIGINAL(0x00100000));"));
        findViewById(R.id.snipReturn).setOnClickListener(v -> insertSnippet("RETURN_TO(ctx->r[14]);"));
        findViewById(R.id.snipRead32).setOnClickListener(v -> insertSnippet("uint32_t val = mem_read32(ctx, 0x08000000);"));
        findViewById(R.id.snipWrite32).setOnClickListener(v -> insertSnippet("mem_write32(ctx, 0x08000000, 999);"));
        findViewById(R.id.snipReg).setOnClickListener(v -> insertSnippet("ctx->r[0]"));
    }

    private void insertSnippet(String snippet) {
        if (isReadOnly) {
            Toast.makeText(this, "Arquivo em modo somente leitura (descompilado).", Toast.LENGTH_SHORT).show();
            return;
        }
        int start = Math.max(editorContent.getSelectionStart(), 0);
        int end = Math.max(editorContent.getSelectionEnd(), 0);
        editorContent.getText().replace(Math.min(start, end), Math.max(start, end), snippet, 0, snippet.length());
    }

    private void loadMods() {
        modsList.clear();
        if (overridesDir.exists()) {
            File[] files = overridesDir.listFiles((dir, name) -> name.endsWith(".c") || name.endsWith(".h"));
            if (files != null) {
                Collections.addAll(modsList, files);
                Collections.sort(modsList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            }
        }
        emptyModsLayout.setVisibility(modsList.isEmpty() ? View.VISIBLE : View.GONE);
        modsAdapter.notifyDataSetChanged();
    }

    private void loadDecompiled() {
        decompiledList.clear();
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".c") || name.endsWith(".h"));
            if (files != null) {
                Collections.addAll(decompiledList, files);
                Collections.sort(decompiledList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            }
        }
        filterDecompiled(searchDecompiledEdit.getText().toString());
    }

    private void filterDecompiled(String query) {
        filteredDecompiledList.clear();
        String q = query.trim().toLowerCase();
        for (File f : decompiledList) {
            if (q.isEmpty() || f.getName().toLowerCase().contains(q)) {
                filteredDecompiledList.add(f);
            }
        }
        decompiledAdapter.notifyDataSetChanged();
    }

    private void openFileInEditor(File file, boolean editable) {
        currentEditingFile = file;
        isReadOnly = !editable;

        editorFileName.setText(file.getName());
        editorModeBadge.setText(editable ? "Mod Editável" : "Decompilado (Leitura)");
        editorModeBadge.setBackgroundResource(editable ? R.drawable.badge_success : R.drawable.badge_background);
        editorSaveBtn.setVisibility(editable ? View.VISIBLE : View.GONE);
        editorDeleteBtn.setVisibility(editable ? View.VISIBLE : View.GONE);
        editorContent.setFocusable(editable);
        editorContent.setFocusableInTouchMode(editable);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                count++;
                if (count > 25000) {
                    sb.append("\n/* [Arquivo muito extenso, exibição truncada aos primeiros 25.000 linhas] */\n");
                    break;
                }
            }
        } catch (Exception e) {
            sb.append("// Erro ao ler arquivo: ").append(e.getMessage());
        }

        editorContent.setText(sb.toString());
        editorOverlay.setVisibility(View.VISIBLE);
    }

    private void saveCurrentFile() {
        if (currentEditingFile == null || isReadOnly) return;
        try (FileOutputStream fos = new FileOutputStream(currentEditingFile)) {
            byte[] bytes = editorContent.getText().toString().getBytes(StandardCharsets.UTF_8);
            fos.write(bytes);
            Toast.makeText(this, "✅ Mod salvo com sucesso (" + bytes.length + " bytes)!", Toast.LENGTH_SHORT).show();
            loadMods();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar mod: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeleteCurrentFile() {
        if (currentEditingFile == null || isReadOnly) return;
        new AlertDialog.Builder(this)
                .setTitle("Excluir Mod")
                .setMessage("Deseja realmente excluir " + currentEditingFile.getName() + "?")
                .setPositiveButton("Excluir", (d, w) -> {
                    if (currentEditingFile.delete()) {
                        Toast.makeText(this, "Mod excluído.", Toast.LENGTH_SHORT).show();
                        editorOverlay.setVisibility(View.GONE);
                        currentEditingFile = null;
                        loadMods();
                    } else {
                        Toast.makeText(this, "Não foi possível excluir o arquivo.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void promptCreateMod(String templateType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Novo Mod em C");

        final EditText input = new EditText(this);
        String defaultName = "mod_patch.c";
        if ("hook".equals(templateType)) defaultName = "hook_speed.c";
        else if ("replace".equals(templateType)) defaultName = "replace_func.c";
        else if ("cheat".equals(templateType)) defaultName = "cheat_lives.c";
        input.setText(defaultName);
        input.setSelectAllOnFocus(true);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Criar", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.endsWith(".c")) name += ".c";
            File target = new File(overridesDir, name);
            if (target.exists()) {
                Toast.makeText(this, "Já existe um mod com este nome!", Toast.LENGTH_SHORT).show();
                return;
            }
            String initialContent = generateTemplateContent(templateType);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(initialContent.getBytes(StandardCharsets.UTF_8));
                loadMods();
                openFileInEditor(target, true);
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao criar mod: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private String generateTemplateContent(String templateType) {
        if ("hook".equals(templateType)) {
            return "/*\n" +
                    " * Hook de Função 3DS\n" +
                    " * Substitua 0x00100000 pelo endereço da função que deseja interceptar.\n" +
                    " */\n" +
                    "#include <stdio.h>\n" +
                    "#include \"overrides.h\"\n\n" +
                    "RECOMP_OVERRIDE(0x00100000) {\n" +
                    "    /* Código executado antes da função original */\n" +
                    "    /* Argumentos nos registradores: ctx->r[0], ctx->r[1]... */\n\n" +
                    "    /* Executa a função original do jogo */\n" +
                    "    CALL(RECOMP_ORIGINAL(0x00100000));\n\n" +
                    "    /* Código executado após o retorno da função original */\n" +
                    "}\n";
        } else if ("replace".equals(templateType)) {
            return "/*\n" +
                    " * Substituição Completa de Função 3DS\n" +
                    " * Substitui o código gerado por uma implementação nativa em C.\n" +
                    " */\n" +
                    "#include \"overrides.h\"\n\n" +
                    "RECOMP_OVERRIDE(0x00100000) {\n" +
                    "    /* Implementação customizada em C */\n" +
                    "    ctx->r[0] = 1; /* Retorno da função */\n\n" +
                    "    /* Retorna ao chamador */\n" +
                    "    RETURN_TO(ctx->r[14]);\n" +
                    "}\n";
        } else if ("cheat".equals(templateType)) {
            return "/*\n" +
                    " * Patch de Memória / Cheat\n" +
                    " * Altera diretamente valores na RAM do 3DS.\n" +
                    " */\n" +
                    "#include \"overrides.h\"\n\n" +
                    "RECOMP_OVERRIDE(0x00100000) {\n" +
                    "    /* Exemplo: Escrever 99 vidas/moedas na memória 0x08001000 */\n" +
                    "    mem_write32(ctx, 0x08001000, 99);\n\n" +
                    "    /* Continua com a função original */\n" +
                    "    CALL(RECOMP_ORIGINAL(0x00100000));\n" +
                    "}\n";
        } else {
            return "/*\n" +
                    " * Mod / Override para 3DS Recomp\n" +
                    " * Consulte docs/overrides.md para detalhes de registradores e memória.\n" +
                    " */\n" +
                    "#include <stdio.h>\n" +
                    "#include \"overrides.h\"\n\n" +
                    "RECOMP_OVERRIDE(0x00100000) {\n" +
                    "    CALL(RECOMP_ORIGINAL(0x00100000));\n" +
                    "}\n";
        }
    }

    private void recompileWithMods() {
        if (romPath == null || romPath.isEmpty()) {
            Toast.makeText(this, "Nenhuma ROM ativa encontrada para recompilar.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Recompilar com Mods")
                .setMessage("Deseja iniciar a recompilação AOT aplicando os arquivos da pasta de Overrides diretamente na biblioteca nativa (.so)?")
                .setPositiveButton("Recompilar Agora", (dialog, which) -> {
                    // Remover o .so antigo para forçar uma nova compilação com os overrides
                    File soFile = new File(getFilesDir(), "3dsrecomp/" + titleId + ".so");
                    if (soFile.exists()) {
                        soFile.delete();
                    }

                    File folder = new File(getFilesDir(), "roms");
                    if (!folder.exists()) folder.mkdirs();

                    File actionFile = new File(folder, "action.txt");
                    try (FileOutputStream actionOut = new FileOutputStream(actionFile)) {
                        actionOut.write("recompile".getBytes(StandardCharsets.UTF_8));
                    } catch (Exception ignored) {}

                    Intent intent = new Intent(this, NativeActivity.class);
                    intent.putExtra("zakuro_selected_rom", romPath);
                    intent.putExtra("zakuro_action", "recompile");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // List Adapter
    private class ModFilesAdapter extends BaseAdapter {
        private final List<File> files;
        private final boolean isEditable;

        ModFilesAdapter(List<File> files, boolean isEditable) {
            this.files = files;
            this.isEditable = isEditable;
        }

        @Override public int getCount() { return files.size(); }
        @Override public Object getItem(int position) { return files.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CModActivity.this).inflate(R.layout.item_cmod_file, parent, false);
            }
            File file = files.get(position);
            TextView itemFileIcon = convertView.findViewById(R.id.itemFileIcon);
            TextView itemFileName = convertView.findViewById(R.id.itemFileName);
            TextView itemFileDetails = convertView.findViewById(R.id.itemFileDetails);
            Button itemActionBtn = convertView.findViewById(R.id.itemActionBtn);

            itemFileName.setText(file.getName());
            boolean isHeader = file.getName().endsWith(".h");
            itemFileIcon.setText(isHeader ? "H" : "C");

            long sizeKb = Math.max(1, file.length() / 1024);
            itemFileDetails.setText(sizeKb + " KB • " + (isEditable ? "Override Customizado" : "Gerado na Descompilação"));

            itemActionBtn.setText(isEditable ? "Editar" : "Ver Código");
            itemActionBtn.setOnClickListener(v -> openFileInEditor(file, isEditable));
            convertView.setOnClickListener(v -> openFileInEditor(file, isEditable));

            return convertView;
        }
    }
}
