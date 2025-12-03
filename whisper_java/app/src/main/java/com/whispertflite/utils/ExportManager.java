package com.whispertflite.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import androidx.core.content.FileProvider;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ExportManager handles exporting transcriptions in multiple formats (PDF, DOCX, MD)
 */
public class ExportManager {
    private static final String TAG = "ExportManager";
    private static final String AUTHORITY = "com.whispertflite.fileprovider";
    
    private final Context context;
    private final File exportDir;

    public interface ExportCallback {
        void onSuccess(File file, String format);
        void onError(String error);
    }

    public ExportManager(Context context) {
        this.context = context;
        // Create exports directory in app's external files directory
        this.exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Whisper_Exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
    }

    /**
     * Export transcription to specified format
     * @param transcription The text to export
     * @param format The export format: "pdf", "docx", or "md"
     * @param callback Callback for success/error
     */
    public void exportTranscription(String transcription, String format, ExportCallback callback) {
        if (transcription == null || transcription.trim().isEmpty()) {
            callback.onError("No transcription to export");
            return;
        }

        new Thread(() -> {
            try {
                File exportedFile;
                switch (format.toLowerCase()) {
                    case "pdf":
                        exportedFile = exportToPDF(transcription);
                        break;
                    case "docx":
                        exportedFile = exportToDOCX(transcription);
                        break;
                    case "md":
                        exportedFile = exportToMarkdown(transcription);
                        break;
                    default:
                        callback.onError("Unsupported format: " + format);
                        return;
                }
                callback.onSuccess(exportedFile, format);
            } catch (Exception e) {
                Log.e(TAG, "Export failed", e);
                callback.onError("Export failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Export transcription to PDF format
     */
    private File exportToPDF(String transcription) throws IOException {
        String fileName = generateFileName(transcription, "pdf");
        File pdfFile = new File(exportDir, fileName);

        PdfWriter writer = new PdfWriter(pdfFile);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add title
        Paragraph title = new Paragraph("Echo AI Transcription")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Add timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        Paragraph timestampPara = new Paragraph("Generated: " + timestamp)
                .setFontSize(10)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(timestampPara);

        // Add spacing
        document.add(new Paragraph("\n"));

        // Add transcription content
        Paragraph content = new Paragraph(transcription)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT);
        document.add(content);

        document.close();
        Log.d(TAG, "PDF exported: " + pdfFile.getAbsolutePath());
        return pdfFile;
    }

    /**
     * Export transcription to DOCX format
     */
    private File exportToDOCX(String transcription) throws IOException {
        String fileName = generateFileName(transcription, "docx");
        File docxFile = new File(exportDir, fileName);

        XWPFDocument document = new XWPFDocument();

        // Add title
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("Echo AI Transcription");
        titleRun.setBold(true);
        titleRun.setFontSize(20);

        // Add timestamp
        XWPFParagraph timestampPara = document.createParagraph();
        timestampPara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
        XWPFRun timestampRun = timestampPara.createRun();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        timestampRun.setText("Generated: " + timestamp);
        timestampRun.setItalic(true);
        timestampRun.setFontSize(10);

        // Add blank line
        document.createParagraph();

        // Add transcription content
        XWPFParagraph contentPara = document.createParagraph();
        XWPFRun contentRun = contentPara.createRun();
        contentRun.setText(transcription);
        contentRun.setFontSize(12);

        // Write to file
        try (FileOutputStream out = new FileOutputStream(docxFile)) {
            document.write(out);
        }
        document.close();

        Log.d(TAG, "DOCX exported: " + docxFile.getAbsolutePath());
        return docxFile;
    }

    /**
     * Export transcription to Markdown format
     */
    private File exportToMarkdown(String transcription) throws IOException {
        String fileName = generateFileName(transcription, "md");
        File mdFile = new File(exportDir, fileName);

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Echo AI Transcription\n\n");
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        markdown.append("**Generated:** ").append(timestamp).append("\n\n");
        markdown.append("---\n\n");
        markdown.append("## Transcription\n\n");
        markdown.append(transcription).append("\n");

        // Write to file
        try (FileOutputStream out = new FileOutputStream(mdFile)) {
            out.write(markdown.toString().getBytes());
        }

        Log.d(TAG, "Markdown exported: " + mdFile.getAbsolutePath());
        return mdFile;
    }

    /**
     * Generate a unique filename with content snippet and timestamp
     */
    private String generateFileName(String transcription, String extension) {
        // Create snippet from first 3-4 words (max 30 chars)
        String snippet = "transcription";
        if (transcription != null && !transcription.trim().isEmpty()) {
            // Take first 50 chars to find words
            String cleanText = transcription.trim().replaceAll("[^a-zA-Z0-9\\s]", "");
            String[] words = cleanText.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(words.length, 4); i++) {
                if (sb.length() + words[i].length() > 25) break;
                if (sb.length() > 0) sb.append("_");
                sb.append(words[i]);
            }
            if (sb.length() > 0) {
                snippet = sb.toString();
            }
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                .format(new Date());
        return snippet + "_" + timestamp + "." + extension;
    }

    /**
     * Share the exported file
     */
    public void shareFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(context, AUTHORITY, file);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(getMimeType(file));
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        Intent chooser = Intent.createChooser(shareIntent, "Share Transcription");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    /**
     * Get MIME type for file
     */
    private String getMimeType(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (fileName.endsWith(".md")) {
            return "text/markdown";
        }
        return "*/*";
    }

    /**
     * Get the exports directory
     */
    public File getExportDir() {
        return exportDir;
    }
}
