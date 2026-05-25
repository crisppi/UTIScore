package br.com.accertconsult.UTIScore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(248, 250, 252);
    private static final int SURFACE = Color.WHITE;
    private static final int FIELD_BG = Color.rgb(250, 252, 255);
    private static final int TEXT = Color.rgb(15, 23, 42);
    private static final int MUTED = Color.rgb(71, 85, 105);
    private static final int LINE = Color.rgb(229, 234, 242);
    private static final int ACCENT = Color.rgb(37, 99, 235);
    private static final int ACCENT_DARK = Color.rgb(30, 64, 175);
    private static final int WARNING = Color.rgb(180, 83, 9);
    private static final int EMPTY_RESULT_TEXT_SP = 20;
    private static final int RESULT_TEXT_SP = 24;

    private final List<ScoreSpec> specs = new ArrayList<>();
    private final Map<String, FieldView> currentFields = new LinkedHashMap<>();
    private final Map<String, ScoreResult> savedResults = new LinkedHashMap<>();
    private final List<CheckBox> criteriaChecks = new ArrayList<>();

    private Button menuButton;
    private GridLayout tabHost;
    private LinearLayout formHost;
    private TextView scoreTitle;
    private TextView scoreHelper;
    private TextView resultScore;
    private TextView resultClass;
    private TextView resultRisk;
    private EditText beneficiaryContext;
    private TextView noteText;
    private ScoreSpec currentSpec;
    private boolean scoreMenuExpanded = false;

    private final DecimalFormat oneDecimal = new DecimalFormat("0.#");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        buildSpecs();
        buildScreen();
        selectSpec(specs.get(0));
        updateNote();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setPadding(0, statusBarHeight(), 0, 0);
        scroll.setClipToPadding(true);

        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(4), 0, dp(6));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_accert);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(88), dp(40));
        logoLp.setMargins(0, 0, dp(9), 0);
        header.addView(logo, logoLp);

        LinearLayout titleBlock = column();
        TextView brand = text("UTI Score", 22, TEXT, Typeface.BOLD);
        titleBlock.addView(brand);

        TextView subtitle = text("Calculadoras e justificativa.", 12, MUTED, Typeface.NORMAL);
        subtitle.setPadding(0, 0, 0, 0);
        titleBlock.addView(subtitle);

        header.addView(titleBlock, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header, matchWrap());

        menuButton = secondaryButton("Escore: SOFA");
        menuButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        menuButton.setPadding(dp(14), 0, dp(14), 0);
        menuButton.setOnClickListener(v -> toggleScoreMenu());
        LinearLayout.LayoutParams menuLp = matchWrap();
        menuLp.setMargins(0, dp(14), 0, dp(8));
        root.addView(menuButton, menuLp);

        tabHost = new GridLayout(this);
        tabHost.setColumnCount(2);
        tabHost.setPadding(0, 0, 0, dp(10));
        tabHost.setVisibility(View.GONE);
        root.addView(tabHost, matchWrap());

        LinearLayout calcCard = card();
        scoreTitle = mediumText("", 20, TEXT);
        scoreHelper = text("", 12, MUTED, Typeface.NORMAL);
        scoreHelper.setPadding(0, dp(4), 0, dp(12));
        formHost = column();
        calcCard.addView(scoreTitle);
        calcCard.addView(scoreHelper);
        calcCard.addView(formHost);

        Button calculate = primaryButton("Calcular escore");
        calculate.setOnClickListener(v -> calculateCurrent());
        LinearLayout.LayoutParams calcParams = matchWrap();
        calcParams.setMargins(0, dp(10), 0, 0);
        calcCard.addView(calculate, calcParams);
        root.addView(calcCard);

        LinearLayout resultCard = card();
        TextView resultLabel = mediumText("Resultado", 12, MUTED);
        resultScore = mediumText("Preencha e calcule", EMPTY_RESULT_TEXT_SP, TEXT);
        resultClass = mediumText("", 14, ACCENT_DARK);
        resultRisk = text("", 12, MUTED, Typeface.NORMAL);
        resultRisk.setPadding(0, dp(5), 0, 0);
        resultCard.addView(resultLabel);
        resultCard.addView(resultScore);
        resultCard.addView(resultClass);
        resultCard.addView(resultRisk);
        root.addView(resultCard);

        LinearLayout noteCard = card();
        TextView noteTitle = mediumText("Justificativa automatica", 20, TEXT);
        TextView noteHelp = text("Use um contexto curto e marque criterios assistenciais. O texto se atualiza com os escores ja calculados.", 12, MUTED, Typeface.NORMAL);
        noteHelp.setPadding(0, dp(4), 0, dp(12));
        patientContext = new EditText(this);
        patientContext.setHint("Ex.: insuficiencia respiratoria aguda, pneumonia grave, choque septico");
        patientContext.setSingleLine(false);
        patientContext.setMinLines(2);
        patientContext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        patientContext.setTextColor(TEXT);
        patientContext.setHintTextColor(Color.rgb(148, 163, 184));
        patientContext.setTextSize(15);
        patientContext.setBackground(cardBg(Color.rgb(248, 250, 252), LINE, dp(8)));
        patientContext.setPadding(dp(12), dp(10), dp(12), dp(10));
        patientContext.setOnFocusChangeListener((v, hasFocus) -> updateNote());

        LinearLayout criteria = column();
        criteria.setPadding(0, dp(12), 0, dp(4));
        addCriterion(criteria, "Insuficiencia respiratoria aguda");
        addCriterion(criteria, "Necessidade de oxigenoterapia, VNI ou ventilacao mecanica");
        addCriterion(criteria, "Instabilidade hemodinamica ou uso de droga vasoativa");
        addCriterion(criteria, "Rebaixamento do nivel de consciencia");
        addCriterion(criteria, "Sepse, choque ou disfuncao organica");
        addCriterion(criteria, "Monitorizacao continua e risco de deterioracao");
        addCriterion(criteria, "Pos-operatorio de alto risco");
        addCriterion(criteria, "Necessidade de dialise ou suporte renal intensivo");

        noteText = text("", 15, TEXT, Typeface.NORMAL);
        noteText.setLineSpacing(dp(2), 1.0f);
        noteText.setBackground(cardBg(FIELD_BG, LINE, dp(8)));
        noteText.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        buttons.setPadding(0, dp(12), 0, 0);

        Button copy = secondaryButton("Copiar");
        copy.setOnClickListener(v -> copyNote());
        Button share = secondaryButton("Compartilhar");
        share.setOnClickListener(v -> shareNote());
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(48), 1);
        half.setMargins(0, 0, dp(6), 0);
        buttons.addView(copy, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(48), 1);
        half2.setMargins(dp(6), 0, 0, 0);
        buttons.addView(share, half2);

        noteCard.addView(noteTitle);
        noteCard.addView(noteHelp);
        noteCard.addView(beneficiaryContext, matchWrap());
        noteCard.addView(criteria);
        noteCard.addView(noteText, matchWrap());
        noteCard.addView(buttons, matchWrap());
        root.addView(noteCard);

        Button privacy = secondaryButton("Politica de privacidade");
        privacy.setOnClickListener(v -> showPrivacyPolicy());
        root.addView(privacy, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        setContentView(scroll);
    }

    private void addCriterion(LinearLayout host, String label) {
        CheckBox check = new CheckBox(this);
        check.setText(label);
        check.setTextColor(TEXT);
        check.setTextSize(14);
        check.setPadding(0, 0, 0, dp(2));
        check.setOnCheckedChangeListener((buttonView, isChecked) -> updateNote());
        criteriaChecks.add(check);
        host.addView(check, matchWrap());
    }

    private void buildTabs() {
        tabHost.removeAllViews();
        if (menuButton != null && currentSpec != null) {
            menuButton.setText("Escore: " + currentSpec.shortName + "  v");
        }
        for (ScoreSpec spec : specs) {
            Button b = new Button(this);
            b.setText(spec.shortName);
            b.setAllCaps(false);
            b.setTextSize(12);
            b.setTextColor(spec == currentSpec ? Color.WHITE : ACCENT_DARK);
            b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            b.setBackground(cardBg(spec == currentSpec ? ACCENT : Color.WHITE, spec == currentSpec ? ACCENT : LINE, dp(18)));
            b.setMinHeight(0);
            b.setMinWidth(0);
            b.setSingleLine(true);
            b.setPadding(dp(8), 0, dp(8), 0);
            b.setOnClickListener(v -> {
                scoreMenuExpanded = false;
                if (tabHost != null) tabHost.setVisibility(View.GONE);
                selectSpec(spec);
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(38);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(0, 0, dp(8), dp(8));
            tabHost.addView(b, lp);
        }
    }

    private void toggleScoreMenu() {
        scoreMenuExpanded = !scoreMenuExpanded;
        tabHost.setVisibility(scoreMenuExpanded ? View.VISIBLE : View.GONE);
        if (currentSpec != null) {
            menuButton.setText("Escore: " + currentSpec.shortName + (scoreMenuExpanded ? "  ^" : "  v"));
        }
    }

    private void selectSpec(ScoreSpec spec) {
        currentSpec = spec;
        currentFields.clear();
        buildTabs();
        formHost.removeAllViews();
        scoreTitle.setText(spec.name);
        scoreHelper.setText(spec.helper);

        for (FieldSpec field : spec.fields) {
            if (field.type == FieldType.OPTION) {
                addOptionField(field);
            } else {
                addCheckField(field);
            }
        }

        ScoreResult previous = savedResults.get(spec.id);
        if (previous == null) {
            resultScore.setText("Preencha e calcule");
            resultScore.setTextSize(EMPTY_RESULT_TEXT_SP);
            resultScore.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            resultClass.setText("");
            resultRisk.setText("");
        } else {
            showResult(previous);
        }
    }

    private void addOptionField(FieldSpec field) {
        TextView label = mediumText(field.label, 13, TEXT);
        label.setPadding(0, dp(8), 0, dp(5));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, field.options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setBackground(cardBg(FIELD_BG, LINE, dp(8)));
        spinner.setPadding(dp(8), 0, dp(8), 0);
        FieldView fieldView = new FieldView(field, spinner, null);
        currentFields.put(field.id, fieldView);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(TEXT);
                    ((TextView) view).setTextSize(14);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        formHost.addView(label);
        formHost.addView(spinner, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
    }

    private void addCheckField(FieldSpec field) {
        CheckBox check = new CheckBox(this);
        check.setText(field.label);
        check.setTextColor(TEXT);
        check.setTextSize(14);
        check.setPadding(0, dp(4), 0, dp(4));
        currentFields.put(field.id, new FieldView(field, null, check));
        formHost.addView(check, matchWrap());
    }

    private void calculateCurrent() {
        if (currentSpec == null) return;
        Map<String, FieldValue> values = new LinkedHashMap<>();
        for (Map.Entry<String, FieldView> entry : currentFields.entrySet()) {
            FieldView fv = entry.getValue();
            values.put(entry.getKey(), fv.value());
        }
        ScoreResult result = currentSpec.calculator.calculate(values);
        savedResults.put(currentSpec.id, result);
        showResult(result);
        updateNote();
    }

    private void showResult(ScoreResult result) {
        resultScore.setText(result.scoreText);
        resultScore.setTextSize(RESULT_TEXT_SP);
        resultScore.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultClass.setText(result.classification);
        resultRisk.setText(result.risk);
    }

    private void updateNote() {
        if (noteText == null) return;
        String context = beneficiaryContext == null ? "" : beneficiaryContext.getText().toString().trim();
        StringBuilder text = new StringBuilder();
        text.append("Beneficiário ");
        if (context.length() > 0) {
            text.append("com ").append(context);
        } else {
            text.append("em avaliacao para suporte intensivo");
        }

        if (!savedResults.isEmpty()) {
            text.append(", apresentando ");
            int count = 0;
            for (ScoreResult result : savedResults.values()) {
                if (count > 0) text.append("; ");
                text.append(result.shortSummary);
                count++;
            }
        }

        List<String> criteria = selectedCriteria();
        if (!criteria.isEmpty()) {
            text.append(". Associa ");
            for (int i = 0; i < criteria.size(); i++) {
                if (i > 0 && i == criteria.size() - 1) {
                    text.append(" e ");
                } else if (i > 0) {
                    text.append(", ");
                }
                text.append(criteria.get(i).toLowerCase(Locale.ROOT));
            }
        }

        text.append(". Mantem criterios clinicos para acompanhamento em unidade de terapia intensiva, com necessidade de monitorizacao continua, intervencoes oportunas e reavaliacao seriada conforme evolucao.");
        noteText.setText(text.toString());
    }

    private List<String> selectedCriteria() {
        List<String> selected = new ArrayList<>();
        for (CheckBox check : criteriaChecks) {
            if (check.isChecked()) selected.add(check.getText().toString());
        }
        return selected;
    }

    private void copyNote() {
        updateNote();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Justificativa UTI Score", noteText.getText().toString()));
        Toast.makeText(this, "Justificativa copiada", Toast.LENGTH_SHORT).show();
    }

    private void shareNote() {
        updateNote();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, noteText.getText().toString());
        startActivity(Intent.createChooser(intent, "Compartilhar justificativa"));
    }

    private void showPrivacyPolicy() {
        new AlertDialog.Builder(this)
                .setTitle("Politica de privacidade")
                .setMessage(privacyPolicyText())
                .setPositiveButton("Entendi", null)
                .show();
    }

    private String privacyPolicyText() {
        return "Ultima atualizacao: 18/05/2026\n\n"
                + "O UTI Score e um aplicativo de calculadoras clinicas e geracao de justificativa assistencial. O app nao exige cadastro, nao solicita login e nao coleta dados pessoais em servidores.\n\n"
                + "Dados inseridos no app\n"
                + "As informacoes digitadas ou selecionadas pelo usuario, incluindo contexto clinico, criterios assistenciais e resultados dos escores, sao usadas apenas para calcular e montar a justificativa exibida na tela. Esses dados permanecem no proprio aparelho durante o uso e nao sao enviados automaticamente para a Accert Consult ou para terceiros.\n\n"
                + "Compartilhamento pelo usuario\n"
                + "Quando o usuario toca em Copiar, o texto e colocado na area de transferencia do dispositivo. Quando toca em Compartilhar, o Android abre os aplicativos disponiveis no aparelho para que o usuario escolha para onde enviar o texto. Nesses casos, o tratamento dos dados passa a depender do aplicativo escolhido pelo usuario.\n\n"
                + "Permissoes, analytics e publicidade\n"
                + "O app nao solicita permissao de internet, localizacao, camera, microfone, contatos ou arquivos. Tambem nao utiliza publicidade, rastreadores, analytics, Firebase ou ferramentas de monitoramento de comportamento.\n\n"
                + "Uso clinico\n"
                + "Os escores apresentados sao ferramentas de apoio e nao substituem avaliacao medica, protocolos institucionais ou diretrizes aplicaveis. O usuario e responsavel por validar as informacoes antes de usar ou compartilhar a justificativa.\n\n"
                + "Contato\n"
                + "Em caso de duvidas sobre privacidade, entre em contato com a Accert Consult.";
    }

    private void buildSpecs() {
        specs.add(sofa());
        specs.add(apache());
        specs.add(glasgow());
        specs.add(curb65());
        specs.add(wells());
        specs.add(qsofa());
        specs.add(cha2ds2());
        specs.add(hasBled());
        specs.add(childPugh());
        specs.add(timi());
    }

    private void configureSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(SURFACE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private int statusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private ScoreSpec sofa() {
        ScoreSpec s = new ScoreSpec("sofa", "SOFA", "SOFA", "Avalia disfuncao organica em seis sistemas. Selecione a pior variavel recente.");
        s.option("resp", "Respiratorio - PaO2/FiO2", pts(0, 1, 2, 3, 4),
                ">= 400",
                "< 400",
                "< 300",
                "< 200 com suporte ventilatorio",
                "< 100 com suporte ventilatorio");
        s.option("coag", "Coagulacao - plaquetas", pts(0, 1, 2, 3, 4),
                ">= 150 mil",
                "< 150 mil",
                "< 100 mil",
                "< 50 mil",
                "< 20 mil");
        s.option("liver", "Hepatico - bilirrubina", pts(0, 1, 2, 3, 4),
                "< 1,2 mg/dL",
                "1,2 a 1,9 mg/dL",
                "2,0 a 5,9 mg/dL",
                "6,0 a 11,9 mg/dL",
                ">= 12 mg/dL");
        s.option("cardio", "Cardiovascular", pts(0, 1, 2, 3, 4),
                "PAM >= 70 mmHg",
                "PAM < 70 mmHg",
                "Dopamina <= 5 ou dobutamina",
                "Dopamina > 5 ou noradrenalina/adrenalina <= 0,1",
                "Dopamina > 15 ou noradrenalina/adrenalina > 0,1");
        s.option("cns", "Neurologico - Glasgow", pts(0, 1, 2, 3, 4),
                "15",
                "13 a 14",
                "10 a 12",
                "6 a 9",
                "< 6");
        s.option("renal", "Renal - creatinina ou diurese", pts(0, 1, 2, 3, 4),
                "< 1,2 mg/dL",
                "1,2 a 1,9 mg/dL",
                "2,0 a 3,4 mg/dL",
                "3,5 a 4,9 mg/dL ou diurese < 500 mL/dia",
                ">= 5,0 mg/dL ou diurese < 200 mL/dia");
        s.calculator = values -> {
            int score = sum(values);
            String cls = score <= 6 ? "Disfuncao leve a moderada" : score <= 9 ? "Disfuncao importante" : score <= 12 ? "Alto risco" : "Risco muito elevado";
            String risk = score <= 6 ? "Risco estimado baixo a moderado; interpretar pela tendencia e contexto clinico."
                    : score <= 9 ? "Risco estimado aumentado, com necessidade de vigilancia intensiva."
                    : score <= 12 ? "Risco estimado alto de mortalidade e deterioracao."
                    : "Risco estimado muito alto, compatível com disfuncao multiorganica grave.";
            return result("SOFA", fmt(score), cls, risk, "SOFA " + score + " pontos (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private ScoreSpec apache() {
        ScoreSpec s = new ScoreSpec("apache", "APACHE II", "APACHE II", "Estimativa de gravidade nas primeiras 24 horas de UTI. Risco exibido e aproximado.");
        s.option("temp", "Temperatura", pts(0, 1, 2, 3, 4, 1, 3, 4), "36 a 38,4 C", "34 a 35,9 C", "32 a 33,9 C", "30 a 31,9 C", "<= 29,9 C", "38,5 a 38,9 C", "39 a 40,9 C", ">= 41 C");
        s.option("map", "Pressao arterial media", pts(0, 2, 4, 2, 3, 4), "70 a 109", "50 a 69", "<= 49", "110 a 129", "130 a 159", ">= 160");
        s.option("hr", "Frequencia cardiaca", pts(0, 2, 3, 4, 2, 3, 4), "70 a 109", "55 a 69", "40 a 54", "<= 39", "110 a 139", "140 a 179", ">= 180");
        s.option("rr", "Frequencia respiratoria", pts(0, 1, 2, 4, 1, 3, 4), "12 a 24", "10 a 11", "6 a 9", "<= 5", "25 a 34", "35 a 49", ">= 50");
        s.option("oxygen", "Oxigenacao", pts(0, 1, 3, 4, 2, 3), "PaO2 >= 70 ou A-a < 200", "PaO2 61 a 70", "PaO2 55 a 60", "PaO2 < 55 ou A-a >= 500", "A-a 200 a 349", "A-a 350 a 499");
        s.option("ph", "pH arterial", pts(0, 1, 2, 3, 4, 3, 4), "7,33 a 7,49", "7,50 a 7,59", "7,25 a 7,32", "7,15 a 7,24", "< 7,15", "7,60 a 7,69", ">= 7,70");
        s.option("na", "Sodio", pts(0, 1, 2, 3, 4, 2, 3, 4), "130 a 149", "150 a 154", "120 a 129", "111 a 119", "<= 110", "155 a 159", "160 a 179", ">= 180");
        s.option("k", "Potassio", pts(0, 1, 2, 4, 1, 3, 4), "3,5 a 5,4", "3,0 a 3,4", "2,5 a 2,9", "< 2,5", "5,5 a 5,9", "6,0 a 6,9", ">= 7,0");
        s.option("creat", "Creatinina", pts(0, 2, 2, 3, 4), "0,6 a 1,4", "< 0,6", "1,5 a 1,9", "2,0 a 3,4", ">= 3,5");
        s.check("arf", "Insuficiencia renal aguda: dobrar pontos da creatinina", 0);
        s.option("hct", "Hematocrito", pts(0, 1, 2, 4, 2, 4), "30 a 45,9%", "46 a 49,9%", "20 a 29,9%", "< 20%", "50 a 59,9%", ">= 60%");
        s.option("wbc", "Leucocitos", pts(0, 1, 2, 4, 2, 4), "3 a 14,9 mil", "15 a 19,9 mil", "1 a 2,9 mil", "< 1 mil", "20 a 39,9 mil", ">= 40 mil");
        s.option("gcs", "Glasgow", pts(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), "15", "14", "13", "12", "11", "10", "9", "8", "7", "6", "5", "4", "3");
        s.option("age", "Idade", pts(0, 2, 3, 5, 6), "<= 44", "45 a 54", "55 a 64", "65 a 74", ">= 75");
        s.option("chronic", "Saude cronica", pts(0, 2, 5), "Sem insuficiencia organica grave/imunossupressao", "Pos-operatorio eletivo com condicao cronica grave", "Clinico ou pos-operatorio de urgencia com condicao cronica grave");
        s.calculator = values -> {
            int score = sum(values);
            if (values.get("arf").checked) {
                int creat = (int) values.get("creat").points;
                score += creat;
            }
            String cls = score < 15 ? "Gravidade menor" : score < 25 ? "Gravidade intermediaria" : score < 35 ? "Gravidade alta" : "Gravidade muito alta";
            String risk = apacheRisk(score);
            return result("APACHE II", fmt(score), cls, risk, "APACHE II " + score + " pontos (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private ScoreSpec glasgow() {
        ScoreSpec s = new ScoreSpec("glasgow", "Glasgow", "Glasgow Coma Scale", "Selecao por toque das respostas ocular, verbal e motora.");
        s.option("eye", "Abertura ocular", pts(4, 3, 2, 1), "Espontanea", "Ao chamado", "A dor", "Nenhuma");
        s.option("verbal", "Resposta verbal", pts(5, 4, 3, 2, 1), "Orientada", "Confusa", "Palavras inapropriadas", "Sons incompreensiveis", "Nenhuma");
        s.option("motor", "Resposta motora", pts(6, 5, 4, 3, 2, 1), "Obedece comandos", "Localiza dor", "Retirada a dor", "Flexao anormal", "Extensao anormal", "Nenhuma");
        s.calculator = values -> {
            int score = sum(values);
            String cls = score >= 13 ? "Trauma/alteracao leve" : score >= 9 ? "Moderado" : "Grave";
            String risk = score <= 8 ? "Sugere rebaixamento importante e necessidade de protecao de via aerea conforme contexto." : "Interpretar junto de sedacao, intubacao e causa metabolica/neurologica.";
            return result("Glasgow", fmt(score), cls, risk, "Glasgow " + score + " (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private ScoreSpec curb65() {
        ScoreSpec s = new ScoreSpec("curb65", "CURB-65", "CURB-65", "Estratificacao de pneumonia adquirida na comunidade.");
        s.check("confusion", "Confusao mental", 1);
        s.check("urea", "Ureia > 7 mmol/L ou BUN > 19 mg/dL", 1);
        s.check("rr", "FR >= 30 irpm", 1);
        s.check("bp", "PAS < 90 ou PAD <= 60 mmHg", 1);
        s.check("age", "Idade >= 65 anos", 1);
        s.calculator = values -> {
            int score = sum(values);
            String cls = score <= 1 ? "Baixo risco" : score == 2 ? "Risco intermediario" : "Alto risco";
            String risk = score <= 1 ? "Mortalidade baixa; considerar tratamento ambulatorial se contexto permitir."
                    : score == 2 ? "Risco intermediario; considerar internacao hospitalar."
                    : "Alto risco; considerar UTI, especialmente com instabilidade ou falencia organica.";
            return result("CURB-65", fmt(score), cls, risk, "CURB-65 " + score + " ponto(s) (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private ScoreSpec wells() {
        ScoreSpec s = new ScoreSpec("wells", "Wells", "Wells para TEP", "Probabilidade clinica de tromboembolismo pulmonar.");
        s.check("dvt", "Sinais clinicos de TVP", 3);
        s.check("alternative", "TEP e diagnostico mais provavel que alternativa", 3);
        s.check("hr", "Frequencia cardiaca > 100 bpm", 1.5);
        s.check("immob", "Imobilizacao recente ou cirurgia nas ultimas 4 semanas", 1.5);
        s.check("previous", "TVP/TEP previo", 1.5);
        s.check("hemoptysis", "Hemoptise", 1);
        s.check("cancer", "Cancer ativo", 1);
        s.calculator = values -> {
            double score = sumDouble(values);
            String cls = score <= 4 ? "TEP improvavel (modelo dicotomico)" : "TEP provavel (modelo dicotomico)";
            String risk = score < 2 ? "Baixa probabilidade no modelo de tres categorias."
                    : score <= 6 ? "Probabilidade intermediaria no modelo de tres categorias."
                    : "Alta probabilidade no modelo de tres categorias.";
            return result("Wells", fmt(score), cls, risk, "Wells TEP " + fmt(score) + " ponto(s) (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private ScoreSpec qsofa() {
        ScoreSpec s = new ScoreSpec("qsofa", "qSOFA", "qSOFA", "Triagem rapida de risco em infeccao suspeita fora da UTI.");
        s.check("rr", "FR >= 22 irpm", 1);
        s.check("mental", "Alteracao do nivel de consciencia", 1);
        s.check("bp", "PAS <= 100 mmHg", 1);
        s.calculator = values -> {
            int score = sum(values);
            String cls = score >= 2 ? "Maior risco de mau desfecho" : "Menor risco pelo qSOFA";
            String risk = score >= 2 ? "Sugere risco aumentado; avaliar sepse, disfuncao organica e necessidade de escalonamento." : "Nao exclui sepse; seguir avaliacao clinica e laboratorial.";
            return result("qSOFA", fmt(score), cls, risk, "qSOFA " + score + " (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private ScoreSpec cha2ds2() {
        ScoreSpec s = new ScoreSpec("cha2ds2", "CHA2DS2", "CHA2DS2-VASc", "Risco tromboembolico em fibrilacao atrial nao valvar.");
        s.check("chf", "Insuficiencia cardiaca", 1);
        s.check("htn", "Hipertensao", 1);
        s.check("age75", "Idade >= 75 anos", 2);
        s.check("dm", "Diabetes", 1);
        s.check("stroke", "AVC/AIT/tromboembolismo previo", 2);
        s.check("vascular", "Doenca vascular", 1);
        s.check("age65", "Idade 65 a 74 anos", 1);
        s.check("female", "Sexo feminino", 1);
        s.calculator = values -> {
            int score = sum(values);
            if (values.get("age75").checked && values.get("age65").checked) score -= 1;
            String cls = score == 0 ? "Baixo risco" : score == 1 ? "Risco intermediario" : "Risco aumentado";
            String risk = "Risco anual aproximado de AVC: " + chaRisk(score) + ". Avaliar anticoagulacao conforme diretriz e sangramento.";
            return result("CHA2DS2-VASc", fmt(score), cls, risk, "CHA2DS2-VASc " + score + " (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private ScoreSpec hasBled() {
        ScoreSpec s = new ScoreSpec("hasbled", "HAS-BLED", "HAS-BLED", "Risco de sangramento em beneficiários anticoagulados.");
        s.check("htn", "Hipertensao nao controlada", 1);
        s.check("renal", "Funcao renal alterada", 1);
        s.check("liver", "Funcao hepatica alterada", 1);
        s.check("stroke", "AVC previo", 1);
        s.check("bleeding", "Sangramento previo ou predisposicao", 1);
        s.check("inr", "INR labil", 1);
        s.check("elderly", "Idade > 65 anos", 1);
        s.check("drugs", "Drogas que aumentam sangramento", 1);
        s.check("alcohol", "Alcool", 1);
        s.calculator = values -> {
            int score = sum(values);
            String cls = score >= 3 ? "Alto risco de sangramento" : "Risco nao alto";
            String risk = score >= 3 ? "Exige correcao de fatores modificaveis e seguimento mais proximo; nao e contraindicação automatica a anticoagulacao." : "Manter avaliacao de fatores modificaveis.";
            return result("HAS-BLED", fmt(score), cls, risk, "HAS-BLED " + score + " (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private ScoreSpec childPugh() {
        ScoreSpec s = new ScoreSpec("childpugh", "Child-Pugh", "Child-Pugh", "Classificacao de gravidade da cirrose.");
        s.option("bili", "Bilirrubina", pts(1, 2, 3), "< 2 mg/dL", "2 a 3 mg/dL", "> 3 mg/dL");
        s.option("albumin", "Albumina", pts(1, 2, 3), "> 3,5 g/dL", "2,8 a 3,5 g/dL", "< 2,8 g/dL");
        s.option("inr", "INR ou TP", pts(1, 2, 3), "INR < 1,7 ou TP < 4 s", "INR 1,7 a 2,3 ou TP 4 a 6 s", "INR > 2,3 ou TP > 6 s");
        s.option("ascites", "Ascite", pts(1, 2, 3), "Ausente", "Leve/moderada controlada", "Tensa ou refrataria");
        s.option("enceph", "Encefalopatia", pts(1, 2, 3), "Ausente", "Grau I-II", "Grau III-IV");
        s.calculator = values -> {
            int score = sum(values);
            String cls = score <= 6 ? "Classe A" : score <= 9 ? "Classe B" : "Classe C";
            String risk = cls.equals("Classe A") ? "Doenca compensada ou menor gravidade relativa." : cls.equals("Classe B") ? "Gravidade intermediaria." : "Doenca avancada, maior risco de complicacoes e mortalidade.";
            return result("Child-Pugh", fmt(score), cls, risk, "Child-Pugh " + score + " pontos (" + cls + ")");
        };
        return s;
    }

    private ScoreSpec timi() {
        ScoreSpec s = new ScoreSpec("timi", "TIMI", "TIMI UA/NSTEMI", "Risco em sindrome coronariana aguda sem supra.");
        s.check("age", "Idade >= 65 anos", 1);
        s.check("risk", "Tres ou mais fatores de risco coronariano", 1);
        s.check("stenosis", "Estenose coronariana conhecida >= 50%", 1);
        s.check("aspirin", "Uso de AAS nos ultimos 7 dias", 1);
        s.check("angina", "Dois ou mais episodios de angina em 24h", 1);
        s.check("st", "Desvio de ST", 1);
        s.check("marker", "Marcadores cardiacos positivos", 1);
        s.calculator = values -> {
            int score = sum(values);
            String cls = score <= 2 ? "Baixo risco" : score <= 4 ? "Risco intermediario" : "Alto risco";
            String risk = "Risco aproximado de evento em 14 dias: " + timiRisk(score) + ".";
            return result("TIMI", fmt(score), cls, risk, "TIMI " + score + " (" + cls.toLowerCase(Locale.ROOT) + ")");
        };
        return s;
    }

    private int[] pts(int... values) {
        return values;
    }

    private int sum(Map<String, FieldValue> values) {
        int total = 0;
        for (FieldValue value : values.values()) {
            if (!value.ignoreInDefaultSum) total += (int) value.points;
        }
        return total;
    }

    private double sumDouble(Map<String, FieldValue> values) {
        double total = 0;
        for (FieldValue value : values.values()) {
            if (!value.ignoreInDefaultSum) total += value.points;
        }
        return total;
    }

    private ScoreResult result(String name, String score, String classification, String risk, String summary) {
        return new ScoreResult(name, score + " pontos", classification, risk, summary);
    }

    private String fmt(double value) {
        return oneDecimal.format(value);
    }

    private String apacheRisk(int score) {
        if (score <= 4) return "Mortalidade hospitalar historica aproximada: cerca de 4%.";
        if (score <= 9) return "Mortalidade hospitalar historica aproximada: cerca de 8%.";
        if (score <= 14) return "Mortalidade hospitalar historica aproximada: cerca de 15%.";
        if (score <= 19) return "Mortalidade hospitalar historica aproximada: cerca de 25%.";
        if (score <= 24) return "Mortalidade hospitalar historica aproximada: cerca de 40%.";
        if (score <= 29) return "Mortalidade hospitalar historica aproximada: cerca de 55%.";
        if (score <= 34) return "Mortalidade hospitalar historica aproximada: cerca de 75%.";
        return "Mortalidade hospitalar historica aproximada: acima de 80%.";
    }

    private String chaRisk(int score) {
        String[] risks = {"0,2%", "0,6%", "2,2%", "3,2%", "4,8%", "7,2%", "9,7%", "11,2%", "10,8%", "12,2%"};
        return risks[Math.max(0, Math.min(score, risks.length - 1))];
    }

    private String timiRisk(int score) {
        if (score <= 1) return "4,7%";
        if (score == 2) return "8,3%";
        if (score == 3) return "13,2%";
        if (score == 4) return "19,9%";
        if (score == 5) return "26,2%";
        return "40,9%";
    }

    private LinearLayout card() {
        LinearLayout layout = column();
        layout.setBackground(cardBg(SURFACE, LINE, dp(8)));
        layout.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, 0, 0, dp(14));
        layout.setLayoutParams(lp);
        return layout;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, style);
        tv.setIncludeFontPadding(false);
        return tv;
    }

    private TextView mediumText(String value, int sp, int color) {
        TextView tv = text(value, sp, color, Typeface.NORMAL);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return tv;
    }

    private Button primaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setBackground(cardBg(ACCENT, ACCENT, dp(8)));
        return b;
    }

    private Button secondaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(ACCENT_DARK);
        b.setBackground(cardBg(Color.WHITE, LINE, dp(8)));
        return b;
    }

    private android.graphics.drawable.Drawable cardBg(int fill, int stroke, int radius) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(fill);
        shape.setStroke(dp(1), stroke);
        shape.setCornerRadius(radius);
        return shape;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private enum FieldType {
        OPTION,
        CHECK
    }

    private interface ScoreCalculator {
        ScoreResult calculate(Map<String, FieldValue> values);
    }

    private static class ScoreSpec {
        final String id;
        final String shortName;
        final String name;
        final String helper;
        final List<FieldSpec> fields = new ArrayList<>();
        ScoreCalculator calculator;

        ScoreSpec(String id, String shortName, String name, String helper) {
            this.id = id;
            this.shortName = shortName;
            this.name = name;
            this.helper = helper;
        }

        void option(String id, String label, int[] points, String... options) {
            fields.add(new FieldSpec(id, label, FieldType.OPTION, options, toDouble(points), 0, false));
        }

        void check(String id, String label, double points) {
            boolean ignore = points == 0;
            fields.add(new FieldSpec(id, label, FieldType.CHECK, new String[0], new double[0], points, ignore));
        }

        private double[] toDouble(int[] points) {
            double[] out = new double[points.length];
            for (int i = 0; i < points.length; i++) out[i] = points[i];
            return out;
        }
    }

    private static class FieldSpec {
        final String id;
        final String label;
        final FieldType type;
        final String[] options;
        final double[] optionPoints;
        final double checkPoints;
        final boolean ignoreInDefaultSum;

        FieldSpec(String id, String label, FieldType type, String[] options, double[] optionPoints, double checkPoints, boolean ignoreInDefaultSum) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.options = options;
            this.optionPoints = optionPoints;
            this.checkPoints = checkPoints;
            this.ignoreInDefaultSum = ignoreInDefaultSum;
        }
    }

    private static class FieldView {
        final FieldSpec spec;
        final Spinner spinner;
        final CheckBox check;

        FieldView(FieldSpec spec, Spinner spinner, CheckBox check) {
            this.spec = spec;
            this.spinner = spinner;
            this.check = check;
        }

        FieldValue value() {
            if (spec.type == FieldType.OPTION) {
                int pos = spinner.getSelectedItemPosition();
                double points = spec.optionPoints[Math.max(0, Math.min(pos, spec.optionPoints.length - 1))];
                return new FieldValue(points, false, spec.ignoreInDefaultSum);
            }
            boolean checked = check.isChecked();
            return new FieldValue(checked ? spec.checkPoints : 0, checked, spec.ignoreInDefaultSum);
        }
    }

    private static class FieldValue {
        final double points;
        final boolean checked;
        final boolean ignoreInDefaultSum;

        FieldValue(double points, boolean checked, boolean ignoreInDefaultSum) {
            this.points = points;
            this.checked = checked;
            this.ignoreInDefaultSum = ignoreInDefaultSum;
        }
    }

    private static class ScoreResult {
        final String name;
        final String scoreText;
        final String classification;
        final String risk;
        final String shortSummary;

        ScoreResult(String name, String scoreText, String classification, String risk, String shortSummary) {
            this.name = name;
            this.scoreText = scoreText;
            this.classification = classification;
            this.risk = risk;
            this.shortSummary = shortSummary;
        }
    }
}
