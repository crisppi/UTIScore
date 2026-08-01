package br.com.accertconsult.UTIScore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
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
import android.widget.FrameLayout;
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
    private static final int ACCENT_SOFT = Color.rgb(219, 234, 254);
    private static final int WARNING = Color.rgb(180, 83, 9);
    private static final int EMPTY_RESULT_TEXT_SP = 20;
    private static final int RESULT_TEXT_SP = 24;
    private static final String CONTACT_EMAIL = "contato@accertconsult.com.br";
    private static final String FULLCARE_URL = "https://www.accertconsult.com.br/fullcare";
    private static final String SITE_URL = "https://www.accertconsult.com.br";
    private static final String LINKEDIN_URL = "https://www.linkedin.com/company/accert-consult/";

    private final List<ScoreSpec> specs = new ArrayList<>();
    private final Map<String, FieldView> currentFields = new LinkedHashMap<>();
    private final Map<String, ScoreResult> savedResults = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> savedSelections = new LinkedHashMap<>();
    private final List<CheckBox> criteriaChecks = new ArrayList<>();

    private LinearLayout menuButton;
    private TextView menuSelection;
    private TextView menuChevron;
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
        root.setPadding(dp(16), dp(10), dp(16), dp(22));
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(4), 0, dp(2));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_accert);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(74), dp(34));
        logoLp.setMargins(0, 0, dp(16), 0);
        header.addView(logo, logoLp);

        LinearLayout titleBlock = column();
        TextView brand = text("UTI Score Auditoria", 20, TEXT, Typeface.BOLD);
        titleBlock.addView(brand);

        header.addView(titleBlock, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header, matchWrap());

        menuButton = buildScorePicker();
        menuButton.setOnClickListener(v -> toggleScoreMenu());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        menuLp.setMargins(0, dp(10), 0, dp(8));
        root.addView(menuButton, menuLp);

        tabHost = new GridLayout(this);
        tabHost.setColumnCount(2);
        tabHost.setPadding(0, 0, 0, dp(2));
        tabHost.setVisibility(View.GONE);
        root.addView(tabHost, matchWrap());

        LinearLayout calcCard = card();
        scoreTitle = mediumText("", 17, TEXT);
        scoreHelper = text("", 11, MUTED, Typeface.NORMAL);
        scoreHelper.setPadding(0, dp(4), 0, dp(11));
        formHost = column();
        calcCard.addView(scoreTitle);
        calcCard.addView(scoreHelper);
        calcCard.addView(formHost);

        Button calculate = primaryButton("Calcular escore");
        calculate.setOnClickListener(v -> calculateCurrent());
        LinearLayout.LayoutParams calcParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        calcParams.setMargins(0, dp(10), 0, 0);
        calcCard.addView(calculate, calcParams);

        Button clear = secondaryButton("Novo paciente / Limpar tudo");
        clear.setOnClickListener(v -> clearPatient());
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        clearParams.setMargins(0, dp(8), 0, 0);
        calcCard.addView(clear, clearParams);
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
        TextView noteTitle = mediumText("Necessidade de UTI", 20, TEXT);
        TextView noteHelp = text("Informe o quadro clinico e marque sinais de necessidade de UTI.", 12, MUTED, Typeface.NORMAL);
        noteHelp.setPadding(0, dp(4), 0, dp(12));
        beneficiaryContext = new EditText(this);
        beneficiaryContext.setHint("Ex.: paciente com dispneia e necessidade de VNI");
        beneficiaryContext.setSingleLine(false);
        beneficiaryContext.setMinLines(2);
        beneficiaryContext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        beneficiaryContext.setTextColor(TEXT);
        beneficiaryContext.setHintTextColor(Color.rgb(148, 163, 184));
        beneficiaryContext.setTextSize(15);
        beneficiaryContext.setBackground(cardBg(Color.rgb(248, 250, 252), LINE, dp(8)));
        beneficiaryContext.setPadding(dp(12), dp(10), dp(12), dp(10));
        beneficiaryContext.setOnFocusChangeListener((v, hasFocus) -> updateNote());

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

    private void addInstitutionalMenu(LinearLayout root) {
        LinearLayout intro = card();
        TextView developer = mediumText("Desenvolvido por Accert Consult", 16, TEXT);
        TextView specialty = text("Especialistas em Auditoria Medica e Inteligencia em Saude", 12, MUTED, Typeface.NORMAL);
        specialty.setPadding(0, dp(5), 0, dp(12));
        intro.addView(developer);
        intro.addView(specialty);

        Button fullCare = institutionalButton("Conheca o FullCare");
        fullCare.setOnClickListener(v -> openExternal(FULLCARE_URL));
        intro.addView(fullCare, actionButtonParams(0));

        Button demo = institutionalButton("Solicitar Demonstracao");
        demo.setOnClickListener(v -> openEmail("Solicitacao de demonstracao do FullCare"));
        intro.addView(demo, actionButtonParams(8));

        GridLayout actions = new GridLayout(this);
        actions.setColumnCount(3);
        actions.setPadding(0, dp(8), 0, 0);

        addInstitutionalGridButton(actions, "Fale Conosco", () -> openEmail("Contato pelo UTI Score Auditoria"));
        addInstitutionalGridButton(actions, "LinkedIn", () -> openExternal(LINKEDIN_URL));
        addInstitutionalGridButton(actions, "Site", () -> openExternal(SITE_URL));

        intro.addView(actions, matchWrap());
        root.addView(intro);
    }

    private Button institutionalButton(String label) {
        Button b = secondaryButton(label);
        b.setTextSize(13);
        return b;
    }

    private LinearLayout.LayoutParams actionButtonParams(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        lp.setMargins(0, topMargin == 0 ? 0 : dp(topMargin), 0, 0);
        return lp;
    }

    private void addInstitutionalGridButton(GridLayout host, String label, Runnable action) {
        Button button = institutionalButton(label);
        button.setTextSize(12);
        button.setOnClickListener(v -> action.run());
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(40);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(0, 0, dp(8), 0);
        host.addView(button, lp);
    }

    private void openEmail(String subject) {
        String uri = "mailto:" + CONTACT_EMAIL + "?subject=" + Uri.encode(subject);
        openExternal(uri);
    }

    private void openExternal(String uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Nenhum aplicativo disponivel para abrir este link", Toast.LENGTH_SHORT).show();
        }
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

    private LinearLayout buildScorePicker() {
        LinearLayout picker = new LinearLayout(this);
        picker.setOrientation(LinearLayout.HORIZONTAL);
        picker.setGravity(Gravity.CENTER_VERTICAL);
        picker.setPadding(dp(12), 0, dp(12), 0);
        picker.setBackground(cardBg(ACCENT_SOFT, Color.rgb(147, 197, 253), dp(8)));

        View icon = new View(this) {
            private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                linePaint.setColor(Color.WHITE);
                linePaint.setStrokeWidth(dp(2));
                linePaint.setStrokeCap(Paint.Cap.ROUND);
                float left = dp(7);
                float right = getWidth() - dp(7);
                float[] ys = {dp(8), dp(14), dp(20)};
                float[] knobs = {dp(12), dp(18), dp(10)};
                for (int i = 0; i < ys.length; i++) {
                    canvas.drawLine(left, ys[i], right, ys[i], linePaint);
                    canvas.drawCircle(knobs[i], ys[i], dp(2), linePaint);
                }
            }
        };
        icon.setBackground(cardBg(ACCENT_DARK, ACCENT_DARK, dp(14)));
        picker.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        LinearLayout labels = column();
        labels.setPadding(dp(10), 0, 0, 0);
        TextView prompt = mediumText("Selecionar escore", 10, Color.rgb(59, 105, 190));
        menuSelection = mediumText("SOFA", 15, ACCENT_DARK);
        menuSelection.setSingleLine(true);
        labels.addView(prompt);
        labels.addView(menuSelection);
        picker.addView(labels, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        menuChevron = mediumText("⌄", 18, ACCENT_DARK);
        menuChevron.setGravity(Gravity.CENTER);
        menuChevron.setBackground(cardBg(Color.rgb(245, 249, 255), Color.TRANSPARENT, dp(15)));
        picker.addView(menuChevron, new LinearLayout.LayoutParams(dp(30), dp(30)));
        return picker;
    }

    private void buildTabs() {
        tabHost.removeAllViews();
        if (menuSelection != null && currentSpec != null) {
            menuSelection.setText(currentSpec.shortName);
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
            lp.height = dp(32);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(0, 0, dp(8), dp(8));
            tabHost.addView(b, lp);
        }
    }

    private void toggleScoreMenu() {
        scoreMenuExpanded = !scoreMenuExpanded;
        tabHost.setVisibility(scoreMenuExpanded ? View.VISIBLE : View.GONE);
        menuChevron.setText(scoreMenuExpanded ? "⌃" : "⌄");
    }

    private void selectSpec(ScoreSpec spec) {
        saveCurrentSelections();
        currentSpec = spec;
        currentFields.clear();
        buildTabs();
        formHost.removeAllViews();
        scoreTitle.setText(spec.name);
        scoreHelper.setText(spec.helper);
        addReferenceRow(formHost, spec);

        for (FieldSpec field : spec.fields) {
            if (field.type == FieldType.OPTION) {
                addOptionField(field);
            } else {
                addCheckField(field);
            }
        }
        restoreSelections(spec.id);

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

    private void addReferenceRow(LinearLayout host, ScoreSpec spec) {
        String[] reference = referenceFor(spec.id);
        LinearLayout row = column();
        row.setPadding(dp(9), dp(9), dp(9), dp(9));
        row.setBackground(cardBg(Color.rgb(239, 246, 255), Color.TRANSPARENT, dp(8)));

        TextView title = mediumText("Fonte clinica", 10, MUTED);
        TextView citation = text(reference[0], 11, MUTED, Typeface.NORMAL);
        citation.setPadding(0, dp(5), 0, dp(5));
        TextView link = mediumText("Abrir referencia", 11, ACCENT_DARK);
        link.setOnClickListener(v -> openExternal(reference[1]));

        row.addView(title);
        row.addView(citation);
        row.addView(link);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(1));
        host.addView(row, params);
    }

    private String[] referenceFor(String id) {
        switch (id) {
            case "apache":
                return new String[]{"Knaus WA et al. APACHE II: a severity of disease classification system. Crit Care Med. 1985.", "https://pubmed.ncbi.nlm.nih.gov/3928249/"};
            case "glasgow":
                return new String[]{"Teasdale G, Jennett B. Assessment of coma and impaired consciousness. Lancet. 1974.", "https://pubmed.ncbi.nlm.nih.gov/4136544/"};
            case "curb65":
                return new String[]{"Lim WS et al. Defining community acquired pneumonia severity on presentation to hospital. Thorax. 2003.", "https://pubmed.ncbi.nlm.nih.gov/12728155/"};
            case "wells":
                return new String[]{"Wells PS et al. Derivation of a simple clinical model for pulmonary embolism probability. Thromb Haemost. 2000.", "https://pubmed.ncbi.nlm.nih.gov/10744147/"};
            case "qsofa":
                return new String[]{"Seymour CW et al. Assessment of clinical criteria for sepsis. JAMA. 2016.", "https://pubmed.ncbi.nlm.nih.gov/26903335/"};
            case "cha2ds2":
                return new String[]{"Lip GYH et al. Refining clinical risk stratification for stroke and thromboembolism in atrial fibrillation. Chest. 2010.", "https://pubmed.ncbi.nlm.nih.gov/19762550/"};
            case "hasbled":
                return new String[]{"Pisters R et al. A novel user-friendly score to assess bleeding risk in atrial fibrillation. Chest. 2010.", "https://pubmed.ncbi.nlm.nih.gov/20299623/"};
            case "childpugh":
                return new String[]{"Pugh RN et al. Transection of the oesophagus for bleeding oesophageal varices. Br J Surg. 1973.", "https://pubmed.ncbi.nlm.nih.gov/4541913/"};
            case "timi":
                return new String[]{"Antman EM et al. The TIMI risk score for unstable angina/non-ST elevation MI. JAMA. 2000.", "https://pubmed.ncbi.nlm.nih.gov/10938172/"};
            case "saps3": return new String[]{"Moreno RP et al. SAPS 3. Intensive Care Med. 2005.", "https://pubmed.ncbi.nlm.nih.gov/16132892/"};
            case "news2": return new String[]{"Royal College of Physicians. National Early Warning Score (NEWS2). 2017.", "https://www.rcplondon.ac.uk/projects/outputs/national-early-warning-score-news-2"};
            case "mews": return new String[]{"Subbe CP et al. Validation of a modified Early Warning Score. QJM. 2001.", "https://pubmed.ncbi.nlm.nih.gov/11588210/"};
            case "kdigo": return new String[]{"KDIGO Clinical Practice Guideline for Acute Kidney Injury. 2012.", "https://kdigo.org/guidelines/acute-kidney-injury/"};
            case "oxygenation": return new String[]{"ARDS Definition Task Force. Berlin Definition of ARDS. JAMA. 2012.", "https://pubmed.ncbi.nlm.nih.gov/22797452/"};
            case "pesi": return new String[]{"Aujesky D et al. Derivation and validation of PESI. 2005.", "https://pubmed.ncbi.nlm.nih.gov/15665310/"};
            case "rass": return new String[]{"Sessler CN et al. Richmond Agitation-Sedation Scale. 2002.", "https://pubmed.ncbi.nlm.nih.gov/12421743/"};
            case "braden": return new String[]{"Bergstrom N et al. The Braden Scale. Nurs Res. 1987.", "https://pubmed.ncbi.nlm.nih.gov/3299278/"};
            case "sepsis": return new String[]{"Singer M et al. Sepsis-3 definitions. JAMA. 2016.", "https://pubmed.ncbi.nlm.nih.gov/26903338/"};
            default:
                return new String[]{"Vincent JL et al. The SOFA score to describe organ dysfunction/failure. Intensive Care Med. 1996.", "https://pubmed.ncbi.nlm.nih.gov/8844239/"};
        }
    }

    private void addOptionField(FieldSpec field) {
        TextView label = mediumText(field.label, 11, TEXT);
        label.setPadding(0, dp(8), 0, dp(5));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, field.options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setBackground(cardBg(FIELD_BG, LINE, dp(8)));
        spinner.setPadding(dp(7), 0, dp(7), 0);
        FieldView fieldView = new FieldView(field, spinner, null);
        currentFields.put(field.id, fieldView);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(TEXT);
                    ((TextView) view).setTextSize(13);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        formHost.addView(label);
        FrameLayout fieldContainer = new FrameLayout(this);
        fieldContainer.addView(spinner, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        TextView chevron = mediumText("⌃\n⌄", 8, MUTED);
        chevron.setGravity(Gravity.CENTER);
        chevron.setIncludeFontPadding(false);
        chevron.setClickable(false);
        FrameLayout.LayoutParams chevronParams = new FrameLayout.LayoutParams(dp(28), dp(38), Gravity.END);
        chevronParams.setMargins(0, 0, dp(3), 0);
        fieldContainer.addView(chevron, chevronParams);
        formHost.addView(fieldContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));
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
        saveCurrentSelections();
        showResult(result);
        updateNote();
    }

    private void saveCurrentSelections() {
        if (currentSpec == null || currentFields.isEmpty()) return;
        Map<String, Integer> state = new LinkedHashMap<>();
        for (Map.Entry<String, FieldView> entry : currentFields.entrySet()) {
            FieldView field = entry.getValue();
            state.put(entry.getKey(), field.spec.type == FieldType.OPTION
                    ? field.spinner.getSelectedItemPosition() : (field.check.isChecked() ? 1 : 0));
        }
        savedSelections.put(currentSpec.id, state);
    }

    private void restoreSelections(String scoreId) {
        Map<String, Integer> state = savedSelections.get(scoreId);
        if (state == null) return;
        for (Map.Entry<String, Integer> entry : state.entrySet()) {
            FieldView field = currentFields.get(entry.getKey());
            if (field == null) continue;
            if (field.spec.type == FieldType.OPTION) field.spinner.setSelection(entry.getValue());
            else field.check.setChecked(entry.getValue() == 1);
        }
    }

    private void clearPatient() {
        ScoreSpec scoreToKeep = currentSpec;
        savedResults.clear();
        savedSelections.clear();
        beneficiaryContext.setText("");
        for (CheckBox check : criteriaChecks) check.setChecked(false);
        currentSpec = null;
        selectSpec(scoreToKeep);
        updateNote();
        Toast.makeText(this, "Novo paciente iniciado", Toast.LENGTH_SHORT).show();
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
        clipboard.setPrimaryClip(ClipData.newPlainText("Avaliacao UTI Score Auditoria", noteText.getText().toString()));
        Toast.makeText(this, "Avaliacao copiada", Toast.LENGTH_SHORT).show();
    }

    private void shareNote() {
        updateNote();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, noteText.getText().toString());
        startActivity(Intent.createChooser(intent, "Compartilhar avaliacao"));
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
                + "O UTI Score Auditoria e um aplicativo de calculadoras clinicas e avaliacao de necessidade de UTI. O app nao exige cadastro, nao solicita login e nao coleta dados pessoais em servidores.\n\n"
                + "Dados inseridos no app\n"
                + "As informacoes digitadas ou selecionadas pelo usuario, incluindo contexto clinico, criterios assistenciais e resultados dos escores, sao usadas apenas para calcular e montar a avaliacao exibida na tela. Esses dados permanecem no proprio aparelho durante o uso e nao sao enviados automaticamente para a Accert Consult ou para terceiros.\n\n"
                + "Compartilhamento pelo usuario\n"
                + "Quando o usuario toca em Copiar, o texto e colocado na area de transferencia do dispositivo. Quando toca em Compartilhar, o Android abre os aplicativos disponiveis no aparelho para que o usuario escolha para onde enviar o texto. Nesses casos, o tratamento dos dados passa a depender do aplicativo escolhido pelo usuario.\n\n"
                + "Permissoes, analytics e publicidade\n"
                + "O app nao solicita permissao de internet, localizacao, camera, microfone, contatos ou arquivos. Tambem nao utiliza publicidade, rastreadores, analytics, Firebase ou ferramentas de monitoramento de comportamento.\n\n"
                + "Uso clinico\n"
                + "Os escores apresentados sao ferramentas de apoio e nao substituem avaliacao medica. O usuario e responsavel por validar as informacoes antes de usar ou compartilhar a avaliacao.\n\n"
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
        specs.add(saps3());
        specs.add(news2());
        specs.add(mews());
        specs.add(kdigo());
        specs.add(oxygenation());
        specs.add(pesi());
        specs.add(rass());
        specs.add(braden());
        specs.add(sepsis());
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

    private ScoreSpec news2() {
        ScoreSpec s = new ScoreSpec("news2", "NEWS2", "NEWS2", "Deteccao de deterioracao clinica em adultos. Use a escala 2 de SpO2 somente quando formalmente indicada.");
        s.option("rr", "Frequencia respiratoria (irpm)", pts(0,1,2,3,3), "12 a 20", "9 a 11", "21 a 24", "<= 8", ">= 25");
        s.option("spo2", "SpO2 - escala 1", pts(0,1,2,3), ">= 96%", "94 a 95%", "92 a 93%", "<= 91%");
        s.check("oxygen", "Oxigenio suplementar", 2);
        s.option("sbp", "Pressao sistolica (mmHg)", pts(0,1,2,3,3), "111 a 219", "101 a 110", "91 a 100", "<= 90", ">= 220");
        s.option("pulse", "Pulso (bpm)", pts(0,1,1,2,3,3), "51 a 90", "41 a 50", "91 a 110", "111 a 130", "<= 40", ">= 131");
        s.option("mental", "Consciencia", pts(0,3), "Alerta", "Confusao nova, voz, dor ou irresponsivo");
        s.option("temp", "Temperatura (C)", pts(0,1,1,2,3), "36,1 a 38,0", "35,1 a 36,0", "38,1 a 39,0", ">= 39,1", "<= 35,0");
        s.calculator = v -> { int n=sum(v); String c=n>=7?"Risco alto":n>=5?"Risco medio":n>=1?"Risco baixo":"Sem alerta atual"; return result("NEWS2",fmt(n),c,n>=7?"Resposta emergencial e avaliacao clinica imediata.":n>=5?"Revisao urgente e monitorizacao mais frequente.":"Seguir protocolo local; um parametro com 3 pontos tambem exige escalonamento.","NEWS2 "+n+" ("+c.toLowerCase(Locale.ROOT)+")"); };
        return s;
    }

    private ScoreSpec mews() {
        ScoreSpec s = new ScoreSpec("mews", "MEWS", "Modified Early Warning Score", "Rastreio de deterioracao; interpretar conforme protocolo institucional.");
        s.option("sbp","Pressao sistolica (mmHg)",pts(0,1,2,3,3),"101 a 199","81 a 100","71 a 80","<= 70",">= 200");
        s.option("hr","Frequencia cardiaca (bpm)",pts(0,1,2,2,3,3),"51 a 100","41 a 50","101 a 110","111 a 129","<= 40",">= 130");
        s.option("rr","Frequencia respiratoria (irpm)",pts(0,1,2,3,3),"9 a 14","15 a 20","21 a 29","<= 8",">= 30");
        s.option("temp","Temperatura (C)",pts(0,1,2,2),"35,0 a 38,4","38,5 a 38,9","< 35,0",">= 39,0");
        s.option("mental","Consciencia (AVPU)",pts(0,1,2,3),"Alerta","Responde a voz","Responde a dor","Irresponsivo");
        s.calculator=v->{int n=sum(v);String c=n>=5?"Alto risco":n>=3?"Risco aumentado":"Baixo risco";return result("MEWS",fmt(n),c,n>=5?"Avaliacao imediata e considerar cuidados intensivos.":"Reavaliar pela tendencia e protocolo local.","MEWS "+n+" ("+c.toLowerCase(Locale.ROOT)+")");}; return s;
    }

    private ScoreSpec kdigo() {
        ScoreSpec s=new ScoreSpec("kdigo","KDIGO","KDIGO - Lesao renal aguda","Classifica pelo criterio mais grave entre creatinina e diurese.");
        s.option("creat","Creatinina",pts(0,1,2,3),"Sem criterio","1,5-1,9x basal ou aumento >=0,3 mg/dL","2,0-2,9x basal",">=3x basal, >=4 mg/dL ou terapia renal substitutiva");
        s.option("urine","Diurese",pts(0,1,2,3),">=0,5 mL/kg/h","<0,5 mL/kg/h por 6-12 h","<0,5 mL/kg/h por >=12 h","<0,3 mL/kg/h por >=24 h ou anuria >=12 h");
        s.calculator=v->{int n=Math.max((int)v.get("creat").points,(int)v.get("urine").points);String c=n==0?"Sem criterio de LRA":("LRA estagio "+n);String risk=n>=2?"Lesao renal moderada/grave; avaliar complicacoes, causa e suporte especializado.":n==1?"Lesao renal leve; monitorar creatinina e diurese.":"Reavaliar se houver mudanca clinica.";return result("KDIGO",fmt(n),c,risk,"KDIGO "+c);}; return s;
    }

    private ScoreSpec oxygenation() {
        ScoreSpec s=new ScoreSpec("oxygenation","P/F e IO","Oxigenacao: P/F e indice de oxigenacao","Classifique com valores calculados: P/F = PaO2/FiO2; IO = FiO2 x pressao media de via aerea x 100 / PaO2.");
        s.option("pf","Relacao PaO2/FiO2",pts(0,1,2,3),"> 300","201 a 300","101 a 200","<= 100");
        s.option("oi","Indice de oxigenacao",pts(0,1,2,3),"< 5","5 a <15","15 a <25",">= 25");
        s.calculator=v->{int pf=(int)v.get("pf").points,oi=(int)v.get("oi").points,n=Math.max(pf,oi);String c=n==0?"Oxigenacao preservada":n==1?"Comprometimento leve":n==2?"Comprometimento moderado":"Comprometimento grave";return result("Oxigenacao",fmt(n),c,"Interpretar P/F com PEEP/CPAP, suporte ventilatorio, tendencia e contexto clinico.","Oxigenacao: "+c.toLowerCase(Locale.ROOT));}; return s;
    }

    private ScoreSpec pesi() {
        ScoreSpec s=new ScoreSpec("pesi","PESI/sPESI","PESI simplificado (sPESI)","Estratificacao prognostica em embolia pulmonar. Esta tela calcula o sPESI validado.");
        s.check("age","Idade > 80 anos",1);s.check("cancer","Cancer",1);s.check("cardio","Insuficiencia cardiaca ou doenca pulmonar cronica",1);s.check("pulse","Pulso >= 110 bpm",1);s.check("sbp","PAS < 100 mmHg",1);s.check("spo2","SpO2 < 90%",1);
        s.calculator=v->{int n=sum(v);String c=n==0?"Baixo risco":"Risco aumentado";return result("sPESI",fmt(n),c,n==0?"Classe de baixo risco; confirmar elegibilidade pela avaliacao clinica completa.":"Um ou mais criterios: maior risco de mortalidade em 30 dias.","sPESI "+n+" ("+c.toLowerCase(Locale.ROOT)+")");};return s;
    }

    private ScoreSpec rass() {
        ScoreSpec s=new ScoreSpec("rass","RASS","Richmond Agitation-Sedation Scale","Selecione o estado observado apos avaliacao padronizada.");
        s.option("state","Estado",pts(4,3,2,1,0,-1,-2,-3,-4,-5),"+4 Combativo","+3 Muito agitado","+2 Agitado","+1 Inquieto","0 Alerta e calmo","-1 Sonolento","-2 Sedacao leve","-3 Sedacao moderada","-4 Sedacao profunda","-5 Nao despertavel");
        s.calculator=v->{int n=(int)v.get("state").points;String c=n>0?"Agitacao":n==0?"Alerta e calmo":"Sedacao";return result("RASS",(n>0?"+":"")+n,c,"Registrar tendencia e confrontar com a meta individual de sedacao.","RASS "+(n>0?"+":"")+n+" ("+c.toLowerCase(Locale.ROOT)+")");};return s;
    }

    private ScoreSpec braden() {
        ScoreSpec s=new ScoreSpec("braden","Braden","Escala de Braden","Risco de lesao por pressao; quanto menor a pontuacao, maior o risco.");
        s.option("sensory","Percepcao sensorial",pts(1,2,3,4),"Totalmente limitada","Muito limitada","Levemente limitada","Nenhuma limitacao");
        s.option("moisture","Umidade",pts(1,2,3,4),"Constantemente umida","Muito umida","Ocasionalmente umida","Raramente umida");
        s.option("activity","Atividade",pts(1,2,3,4),"Acamado","Confinado a cadeira","Caminha ocasionalmente","Caminha frequentemente");
        s.option("mobility","Mobilidade",pts(1,2,3,4),"Totalmente imovel","Muito limitada","Levemente limitada","Sem limitacoes");
        s.option("nutrition","Nutricao",pts(1,2,3,4),"Muito pobre","Provavelmente inadequada","Adequada","Excelente");
        s.option("friction","Friccao e cisalhamento",pts(1,2,3),"Problema","Problema potencial","Nenhum problema aparente");
        s.calculator=v->{int n=sum(v);String c=n<=9?"Risco muito alto":n<=12?"Risco alto":n<=14?"Risco moderado":n<=18?"Em risco":"Sem risco pelo ponto de corte usual";return result("Braden",fmt(n),c,"Implementar prevencao conforme avaliacao cutanea e protocolo institucional.","Braden "+n+" ("+c.toLowerCase(Locale.ROOT)+")");};return s;
    }

    private ScoreSpec sepsis() {
        ScoreSpec s=new ScoreSpec("sepsis","Sepse/choque","Sepse e choque septico (Sepsis-3)","Aplicar em suspeita ou confirmacao de infeccao; requer avaliacao clinica e laboratorial.");
        s.check("infection","Infeccao suspeita ou documentada",0);s.check("sofa","Aumento agudo do SOFA >= 2 pontos",0);s.check("vaso","Vasopressor necessario para PAM >= 65 mmHg apesar de ressuscitacao adequada",0);s.check("lactate","Lactato > 2 mmol/L apesar de ressuscitacao adequada",0);
        s.calculator=v->{boolean inf=v.get("infection").checked,sof=v.get("sofa").checked,vas=v.get("vaso").checked,lac=v.get("lactate").checked;String c=inf&&sof?(vas&&lac?"Choque septico":"Sepse"):"Criterios Sepsis-3 nao completos";String risk=c.equals("Choque septico")?"Emergencia com alta mortalidade; tratamento e suporte imediatos.":c.equals("Sepse")?"Disfuncao organica associada a infeccao; iniciar manejo e monitorizacao imediatos.":"Nao exclui infeccao grave; reavaliar evolucao, SOFA e diagnosticos alternativos.";return result("Sepsis-3",c.equals("Choque septico")?"2":c.equals("Sepse")?"1":"0",c,risk,"Sepsis-3: "+c.toLowerCase(Locale.ROOT));};return s;
    }

    private ScoreSpec saps3() {
        ScoreSpec s=new ScoreSpec("saps3","SAPS 3","SAPS 3","Modelo complexo de gravidade na admissao. Versao inicial orientativa; valide o resultado no protocolo institucional.");
        s.option("age","Idade",pts(0,5,9,13,18),"< 40","40 a 59","60 a 69","70 a 79",">= 80");
        s.option("los","Tempo hospitalar antes da UTI",pts(0,4,7),"< 14 dias","14 a 27 dias",">= 28 dias");
        s.option("origin","Origem",pts(0,5,8),"Centro cirurgico/recuperacao","Emergencia ou outra unidade","Outra UTI");
        s.option("admission","Tipo de admissao",pts(0,5,8),"Cirurgia eletiva","Clinica","Cirurgia de urgencia");
        s.option("gcs","Glasgow",pts(0,5,10,15),"13 a 15","10 a 12","6 a 9","3 a 5");
        s.option("bili","Bilirrubina",pts(0,4,5),"< 2 mg/dL","2 a 5,9 mg/dL",">= 6 mg/dL");
        s.option("temp","Temperatura",pts(0,3,7),">= 35 C","33 a 34,9 C","< 33 C");
        s.option("creat","Creatinina",pts(0,2,7),"< 1,2 mg/dL","1,2 a 3,4 mg/dL",">= 3,5 mg/dL");
        s.option("hr","Frequencia cardiaca",pts(0,5,7),"50 a 119","120 a 159","< 50 ou >= 160");
        s.option("wbc","Leucocitos",pts(0,2,7),"3 a 14,9 mil","15 a 49,9 mil","< 3 ou >= 50 mil");
        s.option("ph","pH",pts(0,3,8),">= 7,35","7,25 a 7,34","< 7,25");
        s.option("platelets","Plaquetas",pts(0,3,8),">= 100 mil","50 a 99 mil","< 50 mil");
        s.option("sbp","Pressao sistolica",pts(0,5,10),">= 120","70 a 119","< 70 mmHg");
        s.option("oxygen","Oxigenacao",pts(0,6,11),"P/F >= 300","P/F 100 a 299","P/F < 100");
        s.check("comorb","Comorbidade grave (cancer, cirrose, AIDS ou insuficiencia cardiaca)",6);
        s.check("vaso","Uso de droga vasoativa antes da admissao",5);
        s.check("infection","Infeccao aguda na admissao",5);
        s.calculator=v->{int n=sum(v)+16;String c=n<45?"Menor gravidade":n<60?"Gravidade intermediaria":n<75?"Alta gravidade":"Gravidade muito alta";return result("SAPS 3",fmt(n),c,"A conversao em mortalidade depende da equacao regional calibrada; nao usar esta classe isoladamente para decisao.","SAPS 3 "+n+" ("+c.toLowerCase(Locale.ROOT)+")");};return s;
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
        layout.setPadding(dp(11), dp(11), dp(11), dp(11));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, 0, 0, dp(10));
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
        b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setTextColor(Color.WHITE);
        b.setBackground(cardBg(ACCENT, ACCENT, dp(8)));
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(10), 0, dp(10), 0);
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
