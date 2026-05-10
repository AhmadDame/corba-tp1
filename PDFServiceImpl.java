import PDFModule.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.encryption.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

public class PDFServiceImpl extends PDFServicePOA {

    public byte[] fusionner(byte[] pdf1, byte[] pdf2) {
        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            merger.addSource(new ByteArrayInputStream(pdf1));
            merger.addSource(new ByteArrayInputStream(pdf2));
            merger.setDestinationStream(out);
            merger.mergeDocuments(null);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public byte[] decouper(byte[] pdf, int pageDebut, int pageFin) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            Splitter splitter = new Splitter();
            splitter.setStartPage(pageDebut);
            splitter.setEndPage(pageFin);
            splitter.setSplitAtPage(pageFin - pageDebut + 1);
            List<PDDocument> pages = splitter.split(doc);
            PDDocument result = pages.get(0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            result.save(out);
            result.close();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public byte[] supprimerPage(byte[] pdf, int numeroPage) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            doc.removePage(numeroPage - 1);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public byte[] ajouterMotDePasse(byte[] pdf, String password) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp =
                new StandardProtectionPolicy(password, password, ap);
            spp.setEncryptionKeyLength(128);
            doc.protect(spp);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public byte[] convertirEnImage(byte[] pdf, int numeroPage) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(numeroPage - 1, 150);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public String extraireTexte(byte[] pdf) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(doc);
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de l'extraction";
        }
    }

    public byte[] creerPDF(String contenu) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.beginText();
            cs.newLineAtOffset(50, 750);
            for (String ligne : contenu.split("\n")) {
                cs.showText(ligne);
                cs.newLineAtOffset(0, -20);
            }
            cs.endText();
            cs.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
