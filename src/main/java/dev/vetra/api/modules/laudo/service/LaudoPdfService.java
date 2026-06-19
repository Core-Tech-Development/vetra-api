package dev.vetra.api.modules.laudo.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import jakarta.enterprise.context.ApplicationScoped;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Generates PDF bytes for a diagnostic laudo.
 * All operations are blocking — callers must use worker threads.
 */
@ApplicationScoped
public class LaudoPdfService {

    private static final Color VETRA_PRIMARY = new Color(31, 111, 91);
    private static final Color VETRA_TEXT = new Color(23, 33, 27);
    private static final Color VETRA_BORDER = new Color(215, 227, 220);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, VETRA_PRIMARY);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, VETRA_PRIMARY);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, VETRA_TEXT);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, VETRA_TEXT);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, VETRA_TEXT);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    public record LaudoPdfData(
            String laudoId,
            String findings,
            String conclusion,
            String recommendations,
            Instant issuedAt,
            String specialistName,
            String specialistCrmv,
            String specialistCrmvState,
            String specialistSpecialty,
            String patientName,
            String patientSpecies,
            String patientBreed,
            String examType,
            String clinicName,
            String diagnosticHypothesis
    ) {}

    /**
     * Generates PDF bytes for a laudo. BLOCKING — must be called on worker pool.
     */
    public byte[] generatePdf(LaudoPdfData data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header
            Paragraph title = new Paragraph("VETRA", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Laudo de Exame Veterinario", SUBTITLE_FONT);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(10);
            document.add(subtitle);

            document.add(new LineSeparator(1f, 100f, VETRA_BORDER, Element.ALIGN_CENTER, -2));
            document.add(new Paragraph(" "));

            // Patient and Clinic Info
            document.add(createSectionTitle("Dados do Paciente"));
            PdfPTable patientTable = createInfoTable();
            addInfoRow(patientTable, "Paciente", safe(data.patientName()));
            addInfoRow(patientTable, "Especie", safe(data.patientSpecies()));
            addInfoRow(patientTable, "Raca", safe(data.patientBreed()));
            addInfoRow(patientTable, "Clinica", safe(data.clinicName()));
            document.add(patientTable);
            document.add(new Paragraph(" "));

            // Exam Info
            document.add(createSectionTitle("Dados do Exame"));
            PdfPTable examTable = createInfoTable();
            addInfoRow(examTable, "Tipo de Exame", safe(data.examType()));
            addInfoRow(examTable, "Hipotese Diagnostica", safe(data.diagnosticHypothesis()));
            document.add(examTable);
            document.add(new Paragraph(" "));

            // Specialist Info
            document.add(createSectionTitle("Especialista Responsavel"));
            PdfPTable specTable = createInfoTable();
            addInfoRow(specTable, "Nome", safe(data.specialistName()));
            addInfoRow(specTable, "CRMV", safe(data.specialistCrmv()) + " / " + safe(data.specialistCrmvState()));
            addInfoRow(specTable, "Especialidade", safe(data.specialistSpecialty()));
            document.add(specTable);
            document.add(new Paragraph(" "));

            // Findings
            if (data.findings() != null && !data.findings().isBlank()) {
                document.add(createSectionTitle("Achados"));
                Paragraph findingsP = new Paragraph(data.findings(), BODY_FONT);
                findingsP.setSpacingAfter(10);
                document.add(findingsP);
            }

            // Conclusion
            if (data.conclusion() != null && !data.conclusion().isBlank()) {
                document.add(createSectionTitle("Conclusao"));
                Paragraph conclusionP = new Paragraph(data.conclusion(), BODY_FONT);
                conclusionP.setSpacingAfter(10);
                document.add(conclusionP);
            }

            // Recommendations
            if (data.recommendations() != null && !data.recommendations().isBlank()) {
                document.add(createSectionTitle("Recomendacoes"));
                Paragraph recsP = new Paragraph(data.recommendations(), BODY_FONT);
                recsP.setSpacingAfter(10);
                document.add(recsP);
            }

            // Footer
            document.add(new Paragraph(" "));
            document.add(new LineSeparator(0.5f, 100f, VETRA_BORDER, Element.ALIGN_CENTER, -2));

            String issuedDate = data.issuedAt() != null ? DATE_FORMATTER.format(data.issuedAt()) : "N/A";
            Paragraph footer = new Paragraph(
                    "Emitido em: " + issuedDate + "  |  ID: " + safe(data.laudoId()),
                    FOOTER_FONT
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(8);
            document.add(footer);

            Paragraph platform = new Paragraph(
                    "Gerado pela plataforma Vetra — vetra.vet.br",
                    FOOTER_FONT
            );
            platform.setAlignment(Element.ALIGN_CENTER);
            document.add(platform);

        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private Paragraph createSectionTitle(String text) {
        Paragraph p = new Paragraph(text, SECTION_FONT);
        p.setSpacingBefore(6);
        p.setSpacingAfter(4);
        return p;
    }

    private PdfPTable createInfoTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{30f, 70f});
        } catch (Exception ignored) {
            // fallback to default widths
        }
        return table;
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorderColor(VETRA_BORDER);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new Color(247, 250, 248));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, VALUE_FONT));
        valueCell.setBorderColor(VETRA_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private String safe(String value) {
        return value != null ? value : "N/A";
    }
}
