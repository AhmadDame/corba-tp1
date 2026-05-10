import PDFModule.*;
import org.omg.CosNaming.*;
import org.omg.CORBA.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;

public class ServeurHTTP {

    static PDFService service;
    static final String UPLOAD_DIR = "/tmp/corba_pdf/";

    public static void main(String[] args) throws Exception {
        // Créer le dossier temporaire
        new File(UPLOAD_DIR).mkdirs();

        // Connexion CORBA
        ORB orb = ORB.init(args, null);
        NamingContextExt ncRef = NamingContextExtHelper.narrow(
            orb.resolve_initial_references("NameService"));
        service = PDFServiceHelper.narrow(
            ncRef.resolve_str("PDFService"));

        // Démarrer serveur HTTP port 8080
        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0);
        server.createContext("/api",      new ApiHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/",          new StaticHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("=== Serveur Web demarre : http://localhost:8080 ===");
    }

    // Méthode utilitaire pour remplacer readAllBytes() en Java 8
    public static byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    // ── Handler API ─────────────────────────────────────────
    static class ApiHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json");

            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(200, -1);
                return;
            }

            // Parser le multipart/form-data
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            
            // CORRECTION JAVA 8 ICI
            byte[] body = readStream(ex.getRequestBody());

            String response;
            try {
                if (contentType != null &&
                    contentType.contains("multipart/form-data")) {
                    response = handleMultipart(body, contentType);
                } else {
                    // application/x-www-form-urlencoded
                    Map<String, String> params = parseUrlEncoded(new String(body, "UTF-8"));
                    response = handleAction(params.get("action"), params, new HashMap<String, byte[]>());
                }
            } catch (Exception e) {
                response = "{\"status\":\"erreur\",\"message\":\""
                    + e.getMessage() + "\"}";
            }

            byte[] bytes = response.getBytes("UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.getResponseBody().close();
        }

        String handleMultipart(byte[] body, String contentType)
                throws Exception {
            String boundary = "--" + contentType.split("boundary=")[1].trim();
            Map<String, String> fields = new HashMap<String, String>();
            Map<String, byte[]> files  = new HashMap<String, byte[]>();

            byte[] boundaryBytes = boundary.getBytes("ISO-8859-1");
            List<byte[]> parts = splitBytes(body, boundaryBytes);

            for (byte[] part : parts) {
                if (part.length < 4) continue;
                int headerEnd = indexOf(part, "\r\n\r\n".getBytes());
                if (headerEnd < 0) continue;
                String headers = new String(part, 0, headerEnd);
                byte[] content = Arrays.copyOfRange(part, headerEnd + 4, part.length);
                
                if (content.length >= 2 &&
                    content[content.length-2] == '\r' &&
                    content[content.length-1] == '\n') {
                    content = Arrays.copyOf(content, content.length - 2);
                }

                String name = extractHeader(headers, "name");
                String filename = extractHeader(headers, "filename");

                if (filename != null && !filename.isEmpty()) {
                    files.put(name, content);
                } else {
                    fields.put(name, new String(content, "UTF-8"));
                }
            }
            return handleAction(fields.get("action"), fields, files);
        }

        String handleAction(String action,
                            Map<String, String> fields,
                            Map<String, byte[]> files) throws Exception {
            if (action == null)
                return "{\"status\":\"erreur\",\"message\":\"Action manquante\"}";

            switch (action) {
                case "creer": {
                    String contenu = fields.get("contenu");
                    if (contenu == null) contenu = "";
                    byte[] result = service.creerPDF(contenu);
                    String filename = saveFile(result, "cree.pdf");
                    return "{\"status\":\"ok\","
                        + "\"filename\":\"" + filename + "\","
                        + "\"url\":\"/download/" + filename + "\"}";
                }

                case "fusionner": {
                    byte[] pdf1 = files.get("pdf1");
                    byte[] pdf2 = files.get("pdf2");
                    if (pdf1 == null || pdf2 == null)
                        return "{\"status\":\"erreur\",\"message\":\"2 PDFs requis\"}";
                    byte[] result = service.fusionner(pdf1, pdf2);
                    String filename = saveFile(result, "fusion.pdf");
                    return "{\"status\":\"ok\","
                        + "\"filename\":\"" + filename + "\","
                        + "\"url\":\"/download/" + filename + "\"}";
                }

                case "decouper": {
                    byte[] pdf = files.get("pdf");
                    int debut = Integer.parseInt(fields.get("debut") != null ? fields.get("debut") : "1");
                    int fin = Integer.parseInt(fields.get("fin") != null ? fields.get("fin") : "1");
                    byte[] result = service.decouper(pdf, debut, fin);
                    String filename = saveFile(result, "decoupe.pdf");
                    return "{\"status\":\"ok\","
                        + "\"filename\":\"" + filename + "\","
                        + "\"url\":\"/download/" + filename + "\"}";
                }

                case "supprimer": {
                    byte[] pdf = files.get("pdf");
                    int page = Integer.parseInt(fields.get("page") != null ? fields.get("page") : "1");
                    byte[] result = service.supprimerPage(pdf, page);
                    String filename = saveFile(result, "sans_page.pdf");
                    return "{\"status\":\"ok\","
                        + "\"filename\":\"" + filename + "\","
                        + "\"url\":\"/download/" + filename + "\"}";
                }

                case "password": {
                    byte[] pdf = files.get("pdf");
                    String mdp = fields.get("password") != null ? fields.get("password") : "";
                    byte[] result = service.ajouterMotDePasse(pdf, mdp);
                    String filename = saveFile(result, "protege.pdf");
                    return "{\"status\":\"ok\","
                        + "\"filename\":\"" + filename + "\","
                        + "\"url\":\"/download/" + filename + "\"}";
                }

                case "image": {
                    byte[] pdf = files.get("pdf");
                    int page = Integer.parseInt(fields.get("page") != null ? fields.get("page") : "1");
                    byte[] result = service.convertirEnImage(pdf, page);
                    String filename = saveFile(result, "page.png");
                    return "{\"status\":\"ok\","
                        + "\"filename\":\"" + filename + "\","
                        + "\"url\":\"/download/" + filename
                        + "\",\"type\":\"image\"}";
                }

                case "extraire": {
                    byte[] pdf = files.get("pdf");
                    String texte = service.extraireTexte(pdf)
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "");
                    return "{\"status\":\"ok\",\"texte\":\"" + texte + "\"}";
                }

                default:
                    return "{\"status\":\"erreur\",\"message\":\"Action inconnue\"}";
            }
        }

        String saveFile(byte[] data, String name) throws Exception {
            String filename = System.currentTimeMillis() + "_" + name;
            // Files.write est OK en Java 8
            Files.write(Paths.get(UPLOAD_DIR + filename), data);
            return filename;
        }

        Map<String, String> parseUrlEncoded(String body) throws Exception {
            Map<String, String> map = new HashMap<String, String>();
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2)
                    map.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
            }
            return map;
        }

        String extractHeader(String headers, String key) {
            for (String part : headers.split(";")) {
                part = part.trim();
                if (part.startsWith(key + "=")) {
                    return part.substring(key.length() + 2, part.length() - 1);
                }
            }
            return null;
        }

        List<byte[]> splitBytes(byte[] data, byte[] delimiter) {
            List<byte[]> parts = new ArrayList<byte[]>();
            int start = 0;
            for (int i = 0; i <= data.length - delimiter.length; i++) {
                boolean match = true;
                for (int j = 0; j < delimiter.length; j++) {
                    if (data[i+j] != delimiter[j]) {
                        match = false; break;
                    }
                }
                if (match) {
                    parts.add(Arrays.copyOfRange(data, start, i));
                    start = i + delimiter.length;
                    i += delimiter.length - 1;
                }
            }
            parts.add(Arrays.copyOfRange(data, start, data.length));
            return parts;
        }

        int indexOf(byte[] data, byte[] pattern) {
            for (int i = 0; i <= data.length - pattern.length; i++) {
                boolean match = true;
                for (int j = 0; j < pattern.length; j++) {
                    if (data[i+j] != pattern[j]) {
                        match = false; break;
                    }
                }
                if (match) return i;
            }
            return -1;
        }
    }

    // ── Handler Téléchargement ───────────────────────────────
    static class DownloadHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            String filename = path.replace("/download/", "");
            File file = new File(UPLOAD_DIR + filename);

            if (!file.exists()) {
                String msg = "Fichier non trouve";
                ex.sendResponseHeaders(404, msg.length());
                ex.getResponseBody().write(msg.getBytes());
                ex.getResponseBody().close();
                return;
            }

            String mime = filename.endsWith(".png") ? "image/png" : "application/pdf";
            ex.getResponseHeaders().add("Content-Type", mime);
            ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            // CORRECTION JAVA 8 ICI
            byte[] data = readStream(new FileInputStream(file));
            
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.getResponseBody().close();
        }
    }

    // ── Handler HTML statique ────────────────────────────────
    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            File f = new File("index.html");
            // CORRECTION JAVA 8 ICI
            byte[] bytes = readStream(new FileInputStream(f));
            
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.getResponseBody().close();
        }
    }
}
