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
}

struct ScoreDefinition: Identifiable {
    let id: String
    let shortName: String
    let title: String
    let helper: String
    let fields: [ScoreField]
    let classify: (Double) -> (String, String)
}

struct ContentView: View {
    private let scores = ScoreLibrary.all

    @State private var selectedScoreID = "sofa"
    @State private var menuExpanded = false
    @State private var selections: [String: Int] = [:]
    @State private var lastResult: ScoreResult?
    @State private var patientContext = ""
    @State private var criteria: Set<String> = []

    private let teal = Color(red: 15 / 255, green: 118 / 255, blue: 110 / 255)
    private let darkTeal = Color(red: 17 / 255, green: 94 / 255, blue: 89 / 255)
    private let bg = Color(red: 247 / 255, green: 250 / 255, blue: 252 / 255)
    private let text = Color(red: 15 / 255, green: 23 / 255, blue: 42 / 255)
    private let muted = Color(red: 71 / 255, green: 85 / 255, blue: 105 / 255)

    private var selectedScore: ScoreDefinition {
        scores.first { $0.id == selectedScoreID } ?? scores[0]
    }

    var body: some View {
        ZStack {
            bg.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    header
                    scorePicker
                    calculatorCard
                    resultCard
                    noteCard
                }
                .padding(.horizontal, 18)
                .padding(.top, 12)
                .padding(.bottom, 24)
            }
        }
        .dynamicTypeSize(.medium)
    }

    private var header: some View {
        HStack(spacing: 10) {
            Image(uiImage: UIImage(named: "logo_accert") ?? UIImage())
                .resizable()
                .scaledToFit()
                .frame(width: 74, height: 34)

            VStack(alignment: .leading, spacing: 1) {
                Text("UTI Score")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(text)
                Text("Calculadoras e justificativa.")
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 4)
        .padding(.bottom, 2)
    }

    private var scorePicker: some View {
        VStack(spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.18)) {
                    menuExpanded.toggle()
                }
            } label: {
                HStack(spacing: 8) {
                    Text("Escore: \(selectedScore.shortName)")
                        .font(.system(size: 15, weight: .bold))
                    Image(systemName: menuExpanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 13, weight: .bold))
                    Spacer(minLength: 0)
                }
                .foregroundStyle(darkTeal)
                .padding(.horizontal, 14)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(.white)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
            }
            .buttonStyle(.plain)

            if menuExpanded {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(scores) { score in
                        Button {
                            selectedScoreID = score.id
                            lastResult = nil
                            withAnimation(.easeInOut(duration: 0.18)) {
                                menuExpanded = false
                            }
                        } label: {
                            Text(score.shortName)
                                .font(.system(size: 13, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .frame(height: 38)
                                .foregroundStyle(score.id == selectedScoreID ? .white : darkTeal)
                                .background(score.id == selectedScoreID ? teal : .white)
                                .clipShape(RoundedRectangle(cornerRadius: 19))
                                .overlay(RoundedRectangle(cornerRadius: 19).stroke(Color(.systemGray5), lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var calculatorCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(selectedScore.title)
                .font(.system(size: 21, weight: .bold))
                .foregroundStyle(text)
            Text(selectedScore.helper)
                .font(.system(size: 13))
                .foregroundStyle(muted)
                .padding(.bottom, 2)

            ForEach(selectedScore.fields) { field in
                VStack(alignment: .leading, spacing: 6) {
                    Text(field.label)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(text)

                    Menu {
                        ForEach(Array(field.options.enumerated()), id: \.offset) { index, option in
                            Button(option.label) {
                                selections[field.id] = index
                            }
                        }
                    } label: {
                        HStack {
                            Text(optionLabel(for: field))
                                .font(.system(size: 15))
                                .foregroundStyle(text)
                                .lineLimit(1)
                                .minimumScaleFactor(0.85)
                            Spacer()
                            Image(systemName: "chevron.up.chevron.down")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(muted)
                        }
                        .padding(.horizontal, 12)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }

            Button("Calcular escore") {
                calculate()
            }
            .font(.system(size: 16, weight: .bold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(teal)
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .padding(14)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
    }

    private var resultCard: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text("Resultado")
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(muted)
            if let result = lastResult {
                Text(result.scoreText)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(text)
                Text(result.classification)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(darkTeal)
                Text(result.risk)
                    .font(.system(size: 13))
                    .foregroundStyle(muted)
            } else {
                Text("Preencha e calcule")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(text)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
    }

    private var noteCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Justificativa automatica")
                .font(.system(size: 21, weight: .bold))
                .foregroundStyle(text)
            TextField("Ex.: insuficiencia respiratoria aguda, pneumonia grave", text: $patientContext, axis: .vertical)
                .lineLimit(2...4)
                .padding(12)
                .background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))

            ForEach(ScoreLibrary.criteria, id: \.self) { item in
                Toggle(item, isOn: Binding(
                    get: { criteria.contains(item) },
                    set: { isOn in
                        if isOn { criteria.insert(item) } else { criteria.remove(item) }
                    }
                ))
                .font(.system(size: 14))
            }

            Text(noteText)
                .font(.system(size: 14))
                .foregroundStyle(text)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))

            ShareLink(item: noteText) {
                Text("Compartilhar justificativa")
                    .font(.system(size: 15, weight: .bold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 46)
                    .foregroundStyle(darkTeal)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
            }
        }
        .padding(14)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray5), lineWidth: 1))
    }

    private var noteText: String {
        var parts: [String] = []
        let context = patientContext.trimmingCharacters(in: .whitespacesAndNewlines)
        var text = "Paciente "
        text += context.isEmpty ? "em avaliacao para suporte intensivo" : "com \(context)"
        if let result = lastResult {
            text += ", apresentando \(result.summary)"
        }
        let selectedCriteria = ScoreLibrary.criteria.filter { criteria.contains($0) }
        if !selectedCriteria.isEmpty {
            text += ". Associa " + selectedCriteria.map { $0.lowercased() }.joined(separator: ", ")
        }
        text += ". Mantem criterios clinicos para acompanhamento em unidade de terapia intensiva, com necessidade de monitorizacao continua, intervencoes oportunas e reavaliacao seriada conforme evolucao."
        parts.append(text)
        return parts.joined()
    }

    private func binding(for field: ScoreField) -> Binding<Int> {
        Binding(
            get: { selections[field.id, default: 0] },
            set: { selections[field.id] = $0 }
        )
    }

    private func optionLabel(for field: ScoreField) -> String {
        let index = min(selections[field.id, default: 0], field.options.count - 1)
        return field.options[index].label
    }

    private func calculate() {
        let score = selectedScore.fields.reduce(0.0) { total, field in
            let index = min(selections[field.id, default: 0], field.options.count - 1)
            return total + field.options[index].points
        }
        let classification = selectedScore.classify(score)
        lastResult = ScoreResult(
            scoreText: score.formattedScore + " pontos",
            classification: classification.0,
            risk: classification.1,
            summary: "\(selectedScore.shortName) \(score.formattedScore) pontos (\(classification.0.lowercased()))"
        )
    }
}

struct ScoreResult {
    let scoreText: String
    let classification: String
    let risk: String
    let summary: String
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
        ScoreDefinition(id: "sofa", shortName: "SOFA", title: "SOFA", helper: "Avalia disfuncao organica em seis sistemas. Selecione a pior variavel recente.", fields: [
            field("resp", "Respiratorio - PaO2/FiO2", [(">= 400", 0), ("< 400", 1), ("< 300", 2), ("< 200 com suporte", 3), ("< 100 com suporte", 4)]),
            field("coag", "Coagulacao - plaquetas", [(">= 150 mil", 0), ("< 150 mil", 1), ("< 100 mil", 2), ("< 50 mil", 3), ("< 20 mil", 4)]),
            field("liver", "Hepatico - bilirrubina", [("< 1,2 mg/dL", 0), ("1,2 a 1,9", 1), ("2,0 a 5,9", 2), ("6,0 a 11,9", 3), (">= 12", 4)]),
            field("cardio", "Cardiovascular", [("PAM >= 70 mmHg", 0), ("PAM < 70", 1), ("Dopamina <= 5 ou dobutamina", 2), ("Vasoativo moderado", 3), ("Vasoativo alto", 4)]),
            field("cns", "Neurologico - Glasgow", [("15", 0), ("13 a 14", 1), ("10 a 12", 2), ("6 a 9", 3), ("< 6", 4)]),
            field("renal", "Renal - creatinina ou diurese", [("< 1,2 mg/dL", 0), ("1,2 a 1,9", 1), ("2,0 a 3,4", 2), ("3,5 a 4,9 ou diurese < 500", 3), (">= 5,0 ou diurese < 200", 4)])
        ], classify: sofaClass),

        ScoreDefinition(id: "apache", shortName: "APACHE II", title: "APACHE II", helper: "Estimativa de gravidade nas primeiras 24 horas de UTI. Risco exibido e aproximado.", fields: [
            field("temp", "Temperatura", [("36 a 38,4 C", 0), ("34 a 35,9 C", 1), ("32 a 33,9 C", 2), ("30 a 31,9 C", 3), ("<= 29,9 C", 4), (">= 41 C", 4)]),
            field("map", "Pressao arterial media", [("70 a 109", 0), ("50 a 69", 2), ("<= 49", 4), ("110 a 129", 2), (">= 160", 4)]),
            field("hr", "Frequencia cardiaca", [("70 a 109", 0), ("55 a 69", 2), ("40 a 54", 3), ("<= 39", 4), ("110 a 139", 2), (">= 180", 4)]),
            field("rr", "Frequencia respiratoria", [("12 a 24", 0), ("10 a 11", 1), ("6 a 9", 2), ("<= 5", 4), ("25 a 34", 1), (">= 50", 4)]),
            field("oxygen", "Oxigenacao", [("PaO2 >= 70 ou A-a < 200", 0), ("PaO2 61 a 70", 1), ("PaO2 55 a 60", 3), ("PaO2 < 55 ou A-a >= 500", 4)]),
            field("gcs", "Glasgow", [("15", 0), ("12", 3), ("10", 5), ("8", 7), ("6", 9), ("3", 12)]),
            field("age", "Idade", [("<= 44", 0), ("45 a 54", 2), ("55 a 64", 3), ("65 a 74", 5), (">= 75", 6)])
        ], classify: apacheClass),

        ScoreDefinition(id: "glasgow", shortName: "Glasgow", title: "Glasgow Coma Scale", helper: "Selecao por toque das respostas ocular, verbal e motora.", fields: [
            field("eye", "Abertura ocular", [("Espontanea", 4), ("Ao chamado", 3), ("A dor", 2), ("Nenhuma", 1)]),
            field("verbal", "Resposta verbal", [("Orientada", 5), ("Confusa", 4), ("Palavras inapropriadas", 3), ("Sons incompreensiveis", 2), ("Nenhuma", 1)]),
            field("motor", "Resposta motora", [("Obedece comandos", 6), ("Localiza dor", 5), ("Retirada a dor", 4), ("Flexao anormal", 3), ("Extensao anormal", 2), ("Nenhuma", 1)])
        ], classify: glasgowClass),

        ScoreDefinition(id: "curb65", shortName: "CURB-65", title: "CURB-65", helper: "Estratificacao de pneumonia adquirida na comunidade.", fields: yesNoFields([("confusion", "Confusao mental"), ("urea", "Ureia elevada"), ("rr", "FR >= 30 irpm"), ("bp", "PAS < 90 ou PAD <= 60"), ("age", "Idade >= 65 anos")]), classify: curbClass),
        ScoreDefinition(id: "wells", shortName: "Wells", title: "Wells para TEP", helper: "Probabilidade clinica de tromboembolismo pulmonar.", fields: [
            field("dvt", "Sinais clinicos de TVP", [("Nao", 0), ("Sim", 3)]),
            field("alternative", "TEP mais provavel", [("Nao", 0), ("Sim", 3)]),
            field("hr", "FC > 100 bpm", [("Nao", 0), ("Sim", 1.5)]),
            field("immob", "Imobilizacao/cirurgia recente", [("Nao", 0), ("Sim", 1.5)]),
            field("previous", "TVP/TEP previo", [("Nao", 0), ("Sim", 1.5)]),
            field("hemoptysis", "Hemoptise", [("Nao", 0), ("Sim", 1)]),
            field("cancer", "Cancer ativo", [("Nao", 0), ("Sim", 1)])
        ], classify: wellsClass),
        ScoreDefinition(id: "qsofa", shortName: "qSOFA", title: "qSOFA", helper: "Triagem rapida de risco em infeccao suspeita.", fields: yesNoFields([("rr", "FR >= 22 irpm"), ("mental", "Alteracao do nivel de consciencia"), ("bp", "PAS <= 100 mmHg")]), classify: qsofaClass)
    ]

    private static func field(_ id: String, _ label: String, _ raw: [(String, Double)]) -> ScoreField {
        ScoreField(id: id, label: label, options: raw.map { FieldOption(label: $0.0, points: $0.1) })
    }

    private static func yesNoFields(_ raw: [(String, String)]) -> [ScoreField] {
        raw.map { field($0.0, $0.1, [("Nao", 0), ("Sim", 1)]) }
    }

    private static func sofaClass(_ score: Double) -> (String, String) {
        if score <= 6 { return ("Disfuncao leve a moderada", "Risco estimado baixo a moderado; interpretar pela tendencia e contexto clinico.") }
        if score <= 9 { return ("Disfuncao importante", "Risco estimado aumentado, com necessidade de vigilancia intensiva.") }
        if score <= 12 { return ("Alto risco", "Risco estimado alto de mortalidade e deterioracao.") }
        return ("Risco muito elevado", "Risco estimado muito alto, compativel com disfuncao multiorganica grave.")
    }

    private static func apacheClass(_ score: Double) -> (String, String) {
        if score < 15 { return ("Gravidade menor", "Risco historico menor; interpretar conforme diagnostico e tendencia.") }
        if score < 25 { return ("Gravidade intermediaria", "Risco estimado aumentado.") }
        if score < 35 { return ("Gravidade alta", "Risco estimado alto.") }
        return ("Gravidade muito alta", "Risco estimado muito alto.")
    }

    private static func glasgowClass(_ score: Double) -> (String, String) {
        if score >= 13 { return ("Leve", "Interpretar junto de sedacao, intubacao e causa metabolica/neurologica.") }
        if score >= 9 { return ("Moderado", "Rebaixamento moderado do nivel de consciencia.") }
        return ("Grave", "Sugere rebaixamento importante e necessidade de suporte intensivo conforme contexto.")
    }

    private static func curbClass(_ score: Double) -> (String, String) {
        if score <= 1 { return ("Baixo risco", "Mortalidade baixa; considerar contexto clinico.") }
        if score == 2 { return ("Risco intermediario", "Considerar internacao hospitalar.") }
        return ("Alto risco", "Considerar UTI, especialmente com instabilidade ou falencia organica.")
    }

    private static func wellsClass(_ score: Double) -> (String, String) {
        if score <= 4 { return ("TEP improvavel", "Baixa/intermediaria probabilidade conforme modelo utilizado.") }
        return ("TEP provavel", "Probabilidade clinica aumentada para TEP.")
    }

    private static func qsofaClass(_ score: Double) -> (String, String) {
        if score >= 2 { return ("Maior risco de mau desfecho", "Avaliar sepse, disfuncao organica e necessidade de escalonamento.") }
        return ("Menor risco pelo qSOFA", "Nao exclui sepse; seguir avaliacao clinica e laboratorial.")
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
