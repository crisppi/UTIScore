import SwiftUI
import UIKit

struct FieldOption: Identifiable, Hashable {
    let id = UUID()
    let label: String
    let points: Double
}

struct ScoreField: Identifiable {
    let id: String
    let label: String
    let options: [FieldOption]
    let ignoreInDefaultSum: Bool
}

struct ClinicalReference: Identifiable {
    let id: String
    let title: String
    let citation: String
    let url: URL
}

struct ScoreDefinition: Identifiable {
    let id: String
    let shortName: String
    let title: String
    let helper: String
    let reference: ClinicalReference
    let fields: [ScoreField]
    let evaluate: ([String: Int]) -> ScoreResult
}

struct ScoreResult {
    let scoreText: String
    let classification: String
    let risk: String
    let summary: String
}

enum BrandLinks {
    static let email = "contato@accertconsult.com.br"
    static let fullCare = URL(string: "https://www.accertconsult.com.br/fullcare")!
    static let site = URL(string: "https://www.accertconsult.com.br")!
    static let linkedIn = URL(string: "https://www.linkedin.com/company/accert-consult/")!
}

struct ContentView: View {
    private let scores = ScoreLibrary.all

    @State private var selectedScoreID = "sofa"
    @State private var menuExpanded = false
    @State private var selections: [String: Int] = [:]
    @State private var savedResults: [String: ScoreResult] = [:]
    @State private var beneficiaryContext = ""
    @State private var criteria: Set<String> = []
    @State private var privacyVisible = false
    @State private var referencesVisible = false
    @State private var copiedVisible = false
    @State private var noteVisible = false

    private let accent = Color(red: 37 / 255, green: 99 / 255, blue: 235 / 255)
    private let accentDark = Color(red: 30 / 255, green: 64 / 255, blue: 175 / 255)
    private let accentSoft = Color(red: 219 / 255, green: 234 / 255, blue: 254 / 255)
    private let bg = Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255)
    private let text = Color(red: 15 / 255, green: 23 / 255, blue: 42 / 255)
    private let muted = Color(red: 71 / 255, green: 85 / 255, blue: 105 / 255)
    private let fieldBg = Color(red: 250 / 255, green: 252 / 255, blue: 255 / 255)

    private var selectedScore: ScoreDefinition {
        scores.first { $0.id == selectedScoreID } ?? scores[0]
    }

    private var currentResult: ScoreResult? {
        savedResults[selectedScore.id]
    }

    var body: some View {
        ZStack {
            bg.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    header
                    brandIntro
                    scorePicker
                    if !menuExpanded {
                        calculatorCard
                        resultCard
                        noteCard
                        privacyButton
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
                .padding(.bottom, 22)
            }
        }
        .dynamicTypeSize(.medium)
        .sheet(isPresented: $privacyVisible) {
            PrivacyPolicyView()
        }
        .sheet(isPresented: $referencesVisible) {
            ClinicalReferencesView(references: ScoreLibrary.references)
        }
        .alert("Avaliacao copiada", isPresented: $copiedVisible) {
            Button("OK", role: .cancel) {}
        }
    }

    private var header: some View {
        HStack(spacing: 16) {
            Image(uiImage: UIImage(named: "logo_accert") ?? UIImage())
                .resizable()
                .scaledToFit()
                .frame(width: 74, height: 34)

            VStack(alignment: .leading, spacing: 1) {
                Text("UTI Score Auditoria")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(text)
                Text("Avaliacao de necessidade de UTI.")
                    .font(.system(size: 11))
                    .foregroundStyle(muted)
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 4)
        .padding(.bottom, 2)
    }

    private var brandIntro: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Desenvolvido por Accert Consult")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(text)
            Text("Especialistas em Auditoria Medica e Inteligencia em Saude")
                .font(.system(size: 11))
                .foregroundStyle(muted)
                .padding(.bottom, 2)

            Link(destination: BrandLinks.fullCare) {
                footerButtonLabel("Conheca o FullCare", systemImage: "cross.case")
            }
            .buttonStyle(.plain)

            Button {
                openEmail(subject: "Solicitacao de demonstracao do FullCare")
            } label: {
                footerButtonLabel("Solicitar Demonstracao", systemImage: "calendar.badge.plus")
            }
            .buttonStyle(.plain)

            HStack(spacing: 8) {
                Button {
                    openEmail(subject: "Contato pelo UTI Score Auditoria")
                } label: {
                    footerButtonLabel("Fale Conosco", systemImage: "envelope")
                }
                .buttonStyle(.plain)

                Link(destination: BrandLinks.linkedIn) {
                    footerButtonLabel("LinkedIn", systemImage: "person.2")
                }
                .buttonStyle(.plain)

                Link(destination: BrandLinks.site) {
                    footerButtonLabel("Site", systemImage: "globe")
                }
                .buttonStyle(.plain)
            }
        }
        .padding(11)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
    }

    private var scorePicker: some View {
        VStack(spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.18)) {
                    menuExpanded.toggle()
                }
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "slider.horizontal.3")
                        .font(.system(size: 13, weight: .bold))
                        .frame(width: 28, height: 28)
                        .foregroundStyle(.white)
                        .background(accentDark)
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 1) {
                        Text("Selecionar escore")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundStyle(accentDark.opacity(0.78))
                        Text(selectedScore.shortName)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(accentDark)
                            .lineLimit(1)
                            .minimumScaleFactor(0.82)
                    }

                    Spacer(minLength: 0)

                    Image(systemName: menuExpanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 12, weight: .bold))
                        .frame(width: 30, height: 30)
                        .foregroundStyle(accentDark)
                        .background(.white.opacity(0.78))
                        .clipShape(Circle())
                }
                .padding(.horizontal, 12)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(accentSoft)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(accent.opacity(0.28), lineWidth: 1))
            }
            .buttonStyle(.plain)

            if menuExpanded {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(scores) { score in
                        Button {
                            selectedScoreID = score.id
                            noteVisible = false
                            withAnimation(.easeInOut(duration: 0.18)) {
                                menuExpanded = false
                            }
                        } label: {
                            Text(score.shortName)
                                .font(.system(size: 11, weight: .medium))
                                .lineLimit(1)
                                .minimumScaleFactor(0.78)
                                .frame(maxWidth: .infinity)
                                .frame(height: 32)
                                .foregroundStyle(score.id == selectedScoreID ? .white : accentDark)
                                .background(score.id == selectedScoreID ? accent : .white)
                                .clipShape(RoundedRectangle(cornerRadius: 19))
                                .overlay(RoundedRectangle(cornerRadius: 19).stroke(Color(.systemGray5), lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    private var calculatorCard: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(selectedScore.title)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(text)
            Text(selectedScore.helper)
                .font(.system(size: 11))
                .foregroundStyle(muted)
                .padding(.bottom, 2)

            referenceRow(selectedScore.reference)

            ForEach(selectedScore.fields) { field in
                VStack(alignment: .leading, spacing: 5) {
                    Text(field.label)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(text.opacity(0.92))

                    Menu {
                        ForEach(Array(field.options.enumerated()), id: \.offset) { index, option in
                            Button(option.label) {
                                selections[field.id] = index
                            }
                        }
                    } label: {
                        HStack {
                            Text(optionLabel(for: field))
                                .font(.system(size: 13))
                                .foregroundStyle(text)
                                .lineLimit(1)
                                .minimumScaleFactor(0.78)
                            Spacer()
                            Image(systemName: "chevron.up.chevron.down")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(muted)
                        }
                        .padding(.horizontal, 11)
                        .frame(maxWidth: .infinity)
                        .frame(height: 38)
                        .background(fieldBg)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }

            Button("Calcular escore") {
                calculate()
            }
            .font(.system(size: 14, weight: .medium))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 42)
            .background(accent)
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .padding(11)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
    }

    private var resultCard: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text("Resultado")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(muted)
            if let result = currentResult {
                Text(result.scoreText)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(text)
                Text(result.classification)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(accentDark)
                Text(result.risk)
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
                referenceRow(selectedScore.reference)
            } else {
                Text("Preencha e calcule")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(text)
            }
        }
        .padding(11)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
    }

    private var noteCard: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text("Necessidade de UTI")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(text)

            Text("Quadro clinico que motivou a avaliacao")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(text)

            TextField("Ex.: paciente com dispneia e necessidade de VNI", text: $beneficiaryContext, axis: .vertical)
                .lineLimit(2...4)
                .foregroundStyle(text)
                .tint(accentDark)
                .padding(10)
                .background(fieldBg)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))

            Text("Sinais de necessidade de UTI")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(text)

            ForEach(ScoreLibrary.criteria, id: \.self) { item in
                Toggle(item, isOn: Binding(
                    get: { criteria.contains(item) },
                    set: { isOn in
                        if isOn { criteria.insert(item) } else { criteria.remove(item) }
                    }
                ))
                .font(.system(size: 12))
            }

            Button(currentResult == nil ? "Calcule o escore acima" : noteVisible ? "Atualizar avaliacao" : "Avaliar necessidade de UTI") {
                withAnimation(.easeInOut(duration: 0.18)) {
                    noteVisible = true
                }
            }
            .font(.system(size: 14, weight: .medium))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 42)
            .background(currentResult == nil ? muted.opacity(0.45) : accent)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(.top, 2)
            .disabled(currentResult == nil)

            if noteVisible {
                Text("Avaliacao pronta para copiar")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(text)
                    .transition(.opacity)

                Text(noteText)
                    .font(.system(size: 12))
                    .foregroundStyle(text)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(fieldBg)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
                    .transition(.opacity)

                HStack(spacing: 12) {
                    Button {
                        UIPasteboard.general.string = noteText
                        copiedVisible = true
                    } label: {
                        Text("Copiar")
                            .font(.system(size: 13, weight: .medium))
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                    }
                    .foregroundStyle(accentDark)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))

                    ShareLink(item: noteText) {
                        Text("Compartilhar")
                            .font(.system(size: 13, weight: .medium))
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                    }
                    .foregroundStyle(accentDark)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
                }
                .transition(.opacity)
            }
        }
        .padding(11)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
    }

    private var privacyButton: some View {
        VStack(spacing: 8) {
            Button {
                referencesVisible = true
            } label: {
                footerButtonLabel("Referencias clinicas", systemImage: "book.closed")
            }
            .buttonStyle(.plain)

            Button {
                privacyVisible = true
            } label: {
                footerButtonLabel("Politica de privacidade", systemImage: "lock.shield")
            }
            .buttonStyle(.plain)
        }
    }

    private func footerButtonLabel(_ title: String, systemImage: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.system(size: 13, weight: .semibold))
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 42)
        .foregroundStyle(accentDark)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
    }

    private func referenceRow(_ reference: ClinicalReference) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text("Fonte clinica")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(muted)
            Text(reference.citation)
                .font(.system(size: 11))
                .foregroundStyle(muted)
                .fixedSize(horizontal: false, vertical: true)
            Link("Abrir referencia", destination: reference.url)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(accentDark)
        }
        .padding(9)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(accentSoft.opacity(0.45))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private var noteText: String {
        let context = beneficiaryContext.trimmingCharacters(in: .whitespacesAndNewlines)
        var text = context.isEmpty ? "Motivo da avaliacao: quadro clinico em avaliacao para suporte intensivo." : "Motivo da avaliacao: \(context)."
        if let result = currentResult {
            text += " Escore avaliado: \(selectedScore.shortName) - \(result.summary)."
        }
        let selectedCriteria = ScoreLibrary.criteria.filter { criteria.contains($0) }
        if !selectedCriteria.isEmpty {
            text += " Sinais clinicos marcados: " + selectedCriteria.map { $0.lowercased() }.joined(separator: ", ") + "."
        }
        if selectedCriteria.isEmpty {
            text += " Avaliacao: pelos dados informados, nao ha indicacao objetiva de UTI neste momento. Manter acompanhamento e reavaliar se houver piora clinica."
        } else {
            text += " Avaliacao: ha indicacao de UTI para monitorizacao continua, intervencoes oportunas e reavaliacao seriada conforme evolucao clinica."
        }
        return text
    }

    private func optionLabel(for field: ScoreField) -> String {
        let index = min(selections[field.id, default: 0], field.options.count - 1)
        return field.options[index].label
    }

    private func calculate() {
        savedResults[selectedScore.id] = selectedScore.evaluate(selections)
    }

    private func openEmail(subject: String) {
        let encodedSubject = subject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? subject
        if let url = URL(string: "mailto:\(BrandLinks.email)?subject=\(encodedSubject)") {
            UIApplication.shared.open(url)
        }
    }
}

struct PrivacyPolicyView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                Text(Self.policyText)
                    .font(.system(size: 14))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
            }
            .navigationTitle("Politica de privacidade")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Entendi") {
                        dismiss()
                    }
                }
            }
        }
    }

    private static let policyText = """
    Ultima atualizacao: 18/05/2026

    O UTI Score Auditoria e um aplicativo de calculadoras clinicas e avaliacao de necessidade de UTI. O app nao exige cadastro, nao solicita login e nao coleta dados pessoais em servidores.

    Dados inseridos no app
    As informacoes digitadas ou selecionadas pelo usuario, incluindo contexto clinico, criterios assistenciais e resultados dos escores, sao usadas apenas para calcular e montar a avaliacao exibida na tela. Esses dados permanecem no proprio aparelho durante o uso e nao sao enviados automaticamente para a Accert Consult ou para terceiros.

    Compartilhamento pelo usuario
    Quando o usuario toca em Copiar, o texto e colocado na area de transferencia do dispositivo. Quando toca em Compartilhar, o iOS abre os aplicativos disponiveis no aparelho para que o usuario escolha para onde enviar o texto. Nesses casos, o tratamento dos dados passa a depender do aplicativo escolhido pelo usuario.

    Permissoes, analytics e publicidade
    O app nao solicita permissao de internet, localizacao, camera, microfone, contatos ou arquivos. Tambem nao utiliza publicidade, rastreadores, analytics, Firebase ou ferramentas de monitoramento de comportamento.

    Uso clinico
    Os escores apresentados sao ferramentas de apoio e nao substituem avaliacao medica. O usuario e responsavel por validar as informacoes antes de usar ou compartilhar a avaliacao.

    Contato
    Em caso de duvidas sobre privacidade, entre em contato com a Accert Consult.
    """
}

struct ClinicalReferencesView: View {
    @Environment(\.dismiss) private var dismiss
    let references: [ClinicalReference]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("As calculadoras sao ferramentas de apoio e devem ser interpretadas por profissional habilitado no contexto clinico completo.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(red: 71 / 255, green: 85 / 255, blue: 105 / 255))

                    ForEach(references) { reference in
                        VStack(alignment: .leading, spacing: 5) {
                            Text(reference.title)
                                .font(.system(size: 15, weight: .semibold))
                            Text(reference.citation)
                                .font(.system(size: 13))
                                .foregroundStyle(Color(red: 71 / 255, green: 85 / 255, blue: 105 / 255))
                            Link("Abrir fonte", destination: reference.url)
                                .font(.system(size: 13, weight: .semibold))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(11)
                        .background(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
                    }
                }
                .padding(16)
            }
            .background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
            .navigationTitle("Referencias clinicas")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Entendi") {
                        dismiss()
                    }
                }
            }
        }
    }
}

enum ScoreLibrary {
    static let criteria = [
        "Insuficiencia respiratoria aguda",
        "Necessidade de oxigenoterapia, VNI ou ventilacao mecanica",
        "Instabilidade hemodinamica ou uso de droga vasoativa",
        "Rebaixamento do nivel de consciencia",
        "Sepse, choque ou disfuncao organica",
        "Monitorizacao continua e risco de deterioracao",
        "Pos-operatorio de alto risco",
        "Necessidade de dialise ou suporte renal intensivo"
    ]

    static let all: [ScoreDefinition] = [
        sofa(),
        apache(),
        glasgow(),
        curb65(),
        wells(),
        qsofa(),
        cha2ds2(),
        hasBled(),
        childPugh(),
        timi()
    ]

    static let references: [ClinicalReference] = [
        sofaReference,
        apacheReference,
        glasgowReference,
        curb65Reference,
        wellsReference,
        qsofaReference,
        cha2ds2Reference,
        hasBledReference,
        childPughReference,
        timiReference
    ]

    private static let sofaReference = ref(
        "SOFA",
        "Vincent JL et al. The SOFA score to describe organ dysfunction/failure. Intensive Care Med. 1996.",
        "https://pubmed.ncbi.nlm.nih.gov/8844239/"
    )

    private static let apacheReference = ref(
        "APACHE II",
        "Knaus WA et al. APACHE II: a severity of disease classification system. Crit Care Med. 1985.",
        "https://pubmed.ncbi.nlm.nih.gov/3928249/"
    )

    private static let glasgowReference = ref(
        "Glasgow Coma Scale",
        "Teasdale G, Jennett B. Assessment of coma and impaired consciousness. Lancet. 1974.",
        "https://pubmed.ncbi.nlm.nih.gov/4136544/"
    )

    private static let curb65Reference = ref(
        "CURB-65",
        "Lim WS et al. Defining community acquired pneumonia severity on presentation to hospital. Thorax. 2003.",
        "https://pubmed.ncbi.nlm.nih.gov/12728155/"
    )

    private static let wellsReference = ref(
        "Wells para TEP",
        "Wells PS et al. Derivation of a simple clinical model for pulmonary embolism probability. Thromb Haemost. 2000.",
        "https://pubmed.ncbi.nlm.nih.gov/10744147/"
    )

    private static let qsofaReference = ref(
        "qSOFA",
        "Seymour CW et al. Assessment of clinical criteria for sepsis. JAMA. 2016.",
        "https://pubmed.ncbi.nlm.nih.gov/26903335/"
    )

    private static let cha2ds2Reference = ref(
        "CHA2DS2-VASc",
        "Lip GYH et al. Refining clinical risk stratification for stroke and thromboembolism in atrial fibrillation. Chest. 2010.",
        "https://pubmed.ncbi.nlm.nih.gov/19762550/"
    )

    private static let hasBledReference = ref(
        "HAS-BLED",
        "Pisters R et al. A novel user-friendly score to assess bleeding risk in atrial fibrillation. Chest. 2010.",
        "https://pubmed.ncbi.nlm.nih.gov/20299623/"
    )

    private static let childPughReference = ref(
        "Child-Pugh",
        "Pugh RN et al. Transection of the oesophagus for bleeding oesophageal varices. Br J Surg. 1973.",
        "https://pubmed.ncbi.nlm.nih.gov/4541913/"
    )

    private static let timiReference = ref(
        "TIMI UA/NSTEMI",
        "Antman EM et al. The TIMI risk score for unstable angina/non-ST elevation MI. JAMA. 2000.",
        "https://pubmed.ncbi.nlm.nih.gov/10938172/"
    )

    private static func sofa() -> ScoreDefinition {
        let fields = [
            field("resp", "Respiratorio - PaO2/FiO2", [(">= 400", 0), ("< 400", 1), ("< 300", 2), ("< 200 com suporte ventilatorio", 3), ("< 100 com suporte ventilatorio", 4)]),
            field("coag", "Coagulacao - plaquetas", [(">= 150 mil", 0), ("< 150 mil", 1), ("< 100 mil", 2), ("< 50 mil", 3), ("< 20 mil", 4)]),
            field("liver", "Hepatico - bilirrubina", [("< 1,2 mg/dL", 0), ("1,2 a 1,9 mg/dL", 1), ("2,0 a 5,9 mg/dL", 2), ("6,0 a 11,9 mg/dL", 3), (">= 12 mg/dL", 4)]),
            field("cardio", "Cardiovascular", [("PAM >= 70 mmHg", 0), ("PAM < 70 mmHg", 1), ("Dopamina <= 5 ou dobutamina", 2), ("Dopamina > 5 ou noradrenalina/adrenalina <= 0,1", 3), ("Dopamina > 15 ou noradrenalina/adrenalina > 0,1", 4)]),
            field("cns", "Neurologico - Glasgow", [("15", 0), ("13 a 14", 1), ("10 a 12", 2), ("6 a 9", 3), ("< 6", 4)]),
            field("renal", "Renal - creatinina ou diurese", [("< 1,2 mg/dL", 0), ("1,2 a 1,9 mg/dL", 1), ("2,0 a 3,4 mg/dL", 2), ("3,5 a 4,9 mg/dL ou diurese < 500 mL/dia", 3), (">= 5,0 mg/dL ou diurese < 200 mL/dia", 4)])
        ]
        return ScoreDefinition(id: "sofa", shortName: "SOFA", title: "SOFA", helper: "Avalia disfuncao organica em seis sistemas. Selecione a pior variavel recente.", reference: sofaReference, fields: fields) { selections in
            let score = sum(fields, selections)
            let cls = score <= 6 ? "Disfuncao leve a moderada" : score <= 9 ? "Disfuncao importante" : score <= 12 ? "Alto risco" : "Risco muito elevado"
            let risk = score <= 6 ? "Risco estimado baixo a moderado; interpretar pela tendencia e contexto clinico." : score <= 9 ? "Risco estimado aumentado, com necessidade de vigilancia intensiva." : score <= 12 ? "Risco estimado alto de mortalidade e deterioracao." : "Risco estimado muito alto, compativel com disfuncao multiorganica grave."
            return result("SOFA", score, cls, risk)
        }
    }

    private static func apache() -> ScoreDefinition {
        let fields = [
            field("temp", "Temperatura", [("36 a 38,4 C", 0), ("34 a 35,9 C", 1), ("32 a 33,9 C", 2), ("30 a 31,9 C", 3), ("<= 29,9 C", 4), ("38,5 a 38,9 C", 1), ("39 a 40,9 C", 3), (">= 41 C", 4)]),
            field("map", "Pressao arterial media", [("70 a 109", 0), ("50 a 69", 2), ("<= 49", 4), ("110 a 129", 2), ("130 a 159", 3), (">= 160", 4)]),
            field("hr", "Frequencia cardiaca", [("70 a 109", 0), ("55 a 69", 2), ("40 a 54", 3), ("<= 39", 4), ("110 a 139", 2), ("140 a 179", 3), (">= 180", 4)]),
            field("rr", "Frequencia respiratoria", [("12 a 24", 0), ("10 a 11", 1), ("6 a 9", 2), ("<= 5", 4), ("25 a 34", 1), ("35 a 49", 3), (">= 50", 4)]),
            field("oxygen", "Oxigenacao", [("PaO2 >= 70 ou A-a < 200", 0), ("PaO2 61 a 70", 1), ("PaO2 55 a 60", 3), ("PaO2 < 55 ou A-a >= 500", 4), ("A-a 200 a 349", 2), ("A-a 350 a 499", 3)]),
            field("ph", "pH arterial", [("7,33 a 7,49", 0), ("7,50 a 7,59", 1), ("7,25 a 7,32", 2), ("7,15 a 7,24", 3), ("< 7,15", 4), ("7,60 a 7,69", 3), (">= 7,70", 4)]),
            field("na", "Sodio", [("130 a 149", 0), ("150 a 154", 1), ("120 a 129", 2), ("111 a 119", 3), ("<= 110", 4), ("155 a 159", 2), ("160 a 179", 3), (">= 180", 4)]),
            field("k", "Potassio", [("3,5 a 5,4", 0), ("3,0 a 3,4", 1), ("2,5 a 2,9", 2), ("< 2,5", 4), ("5,5 a 5,9", 1), ("6,0 a 6,9", 3), (">= 7,0", 4)]),
            field("creat", "Creatinina", [("0,6 a 1,4", 0), ("< 0,6", 2), ("1,5 a 1,9", 2), ("2,0 a 3,4", 3), (">= 3,5", 4)]),
            field("arf", "Insuficiencia renal aguda: dobrar pontos da creatinina", [("Nao", 0), ("Sim", 0)], ignore: true),
            field("hct", "Hematocrito", [("30 a 45,9%", 0), ("46 a 49,9%", 1), ("20 a 29,9%", 2), ("< 20%", 4), ("50 a 59,9%", 2), (">= 60%", 4)]),
            field("wbc", "Leucocitos", [("3 a 14,9 mil", 0), ("15 a 19,9 mil", 1), ("1 a 2,9 mil", 2), ("< 1 mil", 4), ("20 a 39,9 mil", 2), (">= 40 mil", 4)]),
            field("gcs", "Glasgow", [("15", 0), ("14", 1), ("13", 2), ("12", 3), ("11", 4), ("10", 5), ("9", 6), ("8", 7), ("7", 8), ("6", 9), ("5", 10), ("4", 11), ("3", 12)]),
            field("age", "Idade", [("<= 44", 0), ("45 a 54", 2), ("55 a 64", 3), ("65 a 74", 5), (">= 75", 6)]),
            field("chronic", "Saude cronica", [("Sem insuficiencia organica grave/imunossupressao", 0), ("Pos-operatorio eletivo com condicao cronica grave", 2), ("Clinico ou pos-operatorio de urgencia com condicao cronica grave", 5)])
        ]
        return ScoreDefinition(id: "apache", shortName: "APACHE II", title: "APACHE II", helper: "Estimativa de gravidade nas primeiras 24 horas de UTI. Risco exibido e aproximado.", reference: apacheReference, fields: fields) { selections in
            var score = sum(fields, selections)
            if selected("arf", fields, selections) > 0 {
                score += selected("creat", fields, selections)
            }
            let cls = score < 15 ? "Gravidade menor" : score < 25 ? "Gravidade intermediaria" : score < 35 ? "Gravidade alta" : "Gravidade muito alta"
            return result("APACHE II", score, cls, apacheRisk(Int(score)))
        }
    }

    private static func glasgow() -> ScoreDefinition {
        let fields = [
            field("eye", "Abertura ocular", [("Espontanea", 4), ("Ao chamado", 3), ("A dor", 2), ("Nenhuma", 1)]),
            field("verbal", "Resposta verbal", [("Orientada", 5), ("Confusa", 4), ("Palavras inapropriadas", 3), ("Sons incompreensiveis", 2), ("Nenhuma", 1)]),
            field("motor", "Resposta motora", [("Obedece comandos", 6), ("Localiza dor", 5), ("Retirada a dor", 4), ("Flexao anormal", 3), ("Extensao anormal", 2), ("Nenhuma", 1)])
        ]
        return ScoreDefinition(id: "glasgow", shortName: "Glasgow", title: "Glasgow Coma Scale", helper: "Selecao por toque das respostas ocular, verbal e motora.", reference: glasgowReference, fields: fields) { selections in
            let score = sum(fields, selections)
            let cls = score >= 13 ? "Trauma/alteracao leve" : score >= 9 ? "Moderado" : "Grave"
            let risk = score <= 8 ? "Sugere rebaixamento importante e necessidade de protecao de via aerea conforme contexto." : "Interpretar junto de sedacao, intubacao e causa metabolica/neurologica."
            return result("Glasgow", score, cls, risk, unit: "")
        }
    }

    private static func curb65() -> ScoreDefinition {
        let fields = yesNoFields([("confusion", "Confusao mental"), ("urea", "Ureia > 7 mmol/L ou BUN > 19 mg/dL"), ("rr", "FR >= 30 irpm"), ("bp", "PAS < 90 ou PAD <= 60 mmHg"), ("age", "Idade >= 65 anos")])
        return ScoreDefinition(id: "curb65", shortName: "CURB-65", title: "CURB-65", helper: "Estratificacao de pneumonia adquirida na comunidade.", reference: curb65Reference, fields: fields) { selections in
            let score = sum(fields, selections)
            let cls = score <= 1 ? "Baixo risco" : score == 2 ? "Risco intermediario" : "Alto risco"
            let risk = score <= 1 ? "Mortalidade baixa; considerar tratamento ambulatorial se contexto permitir." : score == 2 ? "Risco intermediario; considerar internacao hospitalar." : "Alto risco; considerar UTI, especialmente com instabilidade ou falencia organica."
            return result("CURB-65", score, cls, risk)
        }
    }

    private static func wells() -> ScoreDefinition {
        let fields = [
            field("dvt", "Sinais clinicos de TVP", [("Nao", 0), ("Sim", 3)]),
            field("alternative", "TEP e diagnostico mais provavel que alternativa", [("Nao", 0), ("Sim", 3)]),
            field("hr", "Frequencia cardiaca > 100 bpm", [("Nao", 0), ("Sim", 1.5)]),
            field("immob", "Imobilizacao recente ou cirurgia nas ultimas 4 semanas", [("Nao", 0), ("Sim", 1.5)]),
            field("previous", "TVP/TEP previo", [("Nao", 0), ("Sim", 1.5)]),
            field("hemoptysis", "Hemoptise", [("Nao", 0), ("Sim", 1)]),
            field("cancer", "Cancer ativo", [("Nao", 0), ("Sim", 1)])
        ]
        return ScoreDefinition(id: "wells", shortName: "Wells", title: "Wells para TEP", helper: "Probabilidade clinica de tromboembolismo pulmonar.", reference: wellsReference, fields: fields) { selections in
            let score = sum(fields, selections)
            let cls = score <= 4 ? "TEP improvavel (modelo dicotomico)" : "TEP provavel (modelo dicotomico)"
            let risk = score < 2 ? "Baixa probabilidade no modelo de tres categorias." : score <= 6 ? "Probabilidade intermediaria no modelo de tres categorias." : "Alta probabilidade no modelo de tres categorias."
            return result("Wells", score, cls, risk)
        }
    }

    private static func qsofa() -> ScoreDefinition {
        let fields = yesNoFields([("rr", "FR >= 22 irpm"), ("mental", "Alteracao do nivel de consciencia"), ("bp", "PAS <= 100 mmHg")])
        return ScoreDefinition(id: "qsofa", shortName: "qSOFA", title: "qSOFA", helper: "Triagem rapida de risco em infeccao suspeita fora da UTI.", reference: qsofaReference, fields: fields) { selections in
            let score = sum(fields, selections)
            let cls = score >= 2 ? "Maior risco de mau desfecho" : "Menor risco pelo qSOFA"
            let risk = score >= 2 ? "Sugere risco aumentado; avaliar sepse, disfuncao organica e necessidade de escalonamento." : "Nao exclui sepse; seguir avaliacao clinica e laboratorial."
            return result("qSOFA", score, cls, risk, unit: "")
        }
    }

    private static func cha2ds2() -> ScoreDefinition {
        let fields = [
            field("chf", "Insuficiencia cardiaca", [("Nao", 0), ("Sim", 1)]),
            field("htn", "Hipertensao", [("Nao", 0), ("Sim", 1)]),
            field("age75", "Idade >= 75 anos", [("Nao", 0), ("Sim", 2)]),
            field("dm", "Diabetes", [("Nao", 0), ("Sim", 1)]),
            field("stroke", "AVC/AIT/tromboembolismo previo", [("Nao", 0), ("Sim", 2)]),
            field("vascular", "Doenca vascular", [("Nao", 0), ("Sim", 1)]),
            field("age65", "Idade 65 a 74 anos", [("Nao", 0), ("Sim", 1)]),
            field("female", "Sexo feminino", [("Nao", 0), ("Sim", 1)])
        ]
        return ScoreDefinition(id: "cha2ds2", shortName: "CHA2DS2", title: "CHA2DS2-VASc", helper: "Risco tromboembolico em fibrilacao atrial nao valvar.", reference: cha2ds2Reference, fields: fields) { selections in
            var score = sum(fields, selections)
            if selected("age75", fields, selections) > 0 && selected("age65", fields, selections) > 0 {
                score -= 1
            }
            let cls = score == 0 ? "Baixo risco" : score == 1 ? "Risco intermediario" : "Risco aumentado"
            return result("CHA2DS2-VASc", score, cls, "Risco anual aproximado de AVC: \(chaRisk(Int(score))). Avaliar anticoagulacao conforme diretriz e sangramento.", unit: "")
        }
    }

    private static func hasBled() -> ScoreDefinition {
        let fields = yesNoFields([("htn", "Hipertensao nao controlada"), ("renal", "Funcao renal alterada"), ("liver", "Funcao hepatica alterada"), ("stroke", "AVC previo"), ("bleeding", "Sangramento previo ou predisposicao"), ("inr", "INR labil"), ("elderly", "Idade > 65 anos"), ("drugs", "Drogas que aumentam sangramento"), ("alcohol", "Alcool")])
        return ScoreDefinition(id: "hasbled", shortName: "HAS-BLED", title: "HAS-BLED", helper: "Risco de sangramento em beneficiários anticoagulados.", reference: hasBledReference, fields: fields) { selections in
            let score = sum(fields, selections)
            let cls = score >= 3 ? "Alto risco de sangramento" : "Risco nao alto"
            let risk = score >= 3 ? "Exige correcao de fatores modificaveis e seguimento mais proximo; nao e contraindicacao automatica a anticoagulacao." : "Manter avaliacao de fatores modificaveis."
            return result("HAS-BLED", score, cls, risk, unit: "")
        }
    }

    private static func childPugh() -> ScoreDefinition {
        let fields = [
            field("bili", "Bilirrubina", [("< 2 mg/dL", 1), ("2 a 3 mg/dL", 2), ("> 3 mg/dL", 3)]),
            field("albumin", "Albumina", [("> 3,5 g/dL", 1), ("2,8 a 3,5 g/dL", 2), ("< 2,8 g/dL", 3)]),
            field("inr", "INR ou TP", [("INR < 1,7 ou TP < 4 s", 1), ("INR 1,7 a 2,3 ou TP 4 a 6 s", 2), ("INR > 2,3 ou TP > 6 s", 3)]),
            field("ascites", "Ascite", [("Ausente", 1), ("Leve/moderada controlada", 2), ("Tensa ou refrataria", 3)]),
            field("enceph", "Encefalopatia", [("Ausente", 1), ("Grau I-II", 2), ("Grau III-IV", 3)])
        ]
        return ScoreDefinition(id: "childpugh", shortName: "Child-Pugh", title: "Child-Pugh", helper: "Classificacao de gravidade da cirrose.", reference: childPughReference, fields: fields) { selections in
            let score = sum(fields, selections)
            let cls = score <= 6 ? "Classe A" : score <= 9 ? "Classe B" : "Classe C"
            let risk = cls == "Classe A" ? "Doenca compensada ou menor gravidade relativa." : cls == "Classe B" ? "Gravidade intermediaria." : "Doenca avancada, maior risco de complicacoes e mortalidade."
            return result("Child-Pugh", score, cls, risk)
        }
    }

    private static func timi() -> ScoreDefinition {
        let fields = yesNoFields([("age", "Idade >= 65 anos"), ("risk", "Tres ou mais fatores de risco coronariano"), ("stenosis", "Estenose coronariana conhecida >= 50%"), ("aspirin", "Uso de AAS nos ultimos 7 dias"), ("angina", "Dois ou mais episodios de angina em 24h"), ("st", "Desvio de ST"), ("marker", "Marcadores cardiacos positivos")])
        return ScoreDefinition(id: "timi", shortName: "TIMI", title: "TIMI UA/NSTEMI", helper: "Risco em sindrome coronariana aguda sem supra.", reference: timiReference, fields: fields) { selections in
            let score = sum(fields, selections)
            let cls = score <= 2 ? "Baixo risco" : score <= 4 ? "Risco intermediario" : "Alto risco"
            return result("TIMI", score, cls, "Risco aproximado de evento em 14 dias: \(timiRisk(Int(score))).", unit: "")
        }
    }

    private static func field(_ id: String, _ label: String, _ raw: [(String, Double)], ignore: Bool = false) -> ScoreField {
        ScoreField(id: id, label: label, options: raw.map { FieldOption(label: $0.0, points: $0.1) }, ignoreInDefaultSum: ignore)
    }

    private static func yesNoFields(_ raw: [(String, String)]) -> [ScoreField] {
        raw.map { field($0.0, $0.1, [("Nao", 0), ("Sim", 1)]) }
    }

    private static func ref(_ title: String, _ citation: String, _ url: String) -> ClinicalReference {
        ClinicalReference(id: title, title: title, citation: citation, url: URL(string: url)!)
    }

    private static func selected(_ id: String, _ fields: [ScoreField], _ selections: [String: Int]) -> Double {
        guard let field = fields.first(where: { $0.id == id }) else { return 0 }
        let index = min(selections[id, default: 0], field.options.count - 1)
        return field.options[index].points
    }

    private static func sum(_ fields: [ScoreField], _ selections: [String: Int]) -> Double {
        fields.reduce(0) { total, field in
            if field.ignoreInDefaultSum { return total }
            let index = min(selections[field.id, default: 0], field.options.count - 1)
            return total + field.options[index].points
        }
    }

    private static func result(_ name: String, _ score: Double, _ classification: String, _ risk: String, unit: String = " pontos") -> ScoreResult {
        let scoreText = score.formattedScore + unit
        return ScoreResult(scoreText: scoreText, classification: classification, risk: risk, summary: "\(name) \(score.formattedScore)\(unit) (\(classification.lowercased()))")
    }

    private static func apacheRisk(_ score: Int) -> String {
        if score <= 4 { return "Mortalidade hospitalar historica aproximada: cerca de 4%." }
        if score <= 9 { return "Mortalidade hospitalar historica aproximada: cerca de 8%." }
        if score <= 14 { return "Mortalidade hospitalar historica aproximada: cerca de 15%." }
        if score <= 19 { return "Mortalidade hospitalar historica aproximada: cerca de 25%." }
        if score <= 24 { return "Mortalidade hospitalar historica aproximada: cerca de 40%." }
        if score <= 29 { return "Mortalidade hospitalar historica aproximada: cerca de 55%." }
        if score <= 34 { return "Mortalidade hospitalar historica aproximada: cerca de 75%." }
        return "Mortalidade hospitalar historica aproximada: acima de 80%."
    }

    private static func chaRisk(_ score: Int) -> String {
        let risks = ["0,2%", "0,6%", "2,2%", "3,2%", "4,8%", "7,2%", "9,7%", "11,2%", "10,8%", "12,2%"]
        return risks[max(0, min(score, risks.count - 1))]
    }

    private static func timiRisk(_ score: Int) -> String {
        if score <= 1 { return "4,7%" }
        if score == 2 { return "8,3%" }
        if score == 3 { return "13,2%" }
        if score == 4 { return "19,9%" }
        if score == 5 { return "26,2%" }
        return "40,9%"
    }
}

extension Double {
    var formattedScore: String {
        if truncatingRemainder(dividingBy: 1) == 0 {
            return String(Int(self))
        }
        return String(format: "%.1f", self)
    }
}
