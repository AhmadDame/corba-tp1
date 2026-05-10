 ClientGUI.javaimport PDFModule.*;
import org.omg.CosNaming.*;
import org.omg.CORBA.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

public class ClientGUI extends JFrame {

    private PDFService service;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // Couleurs
    static final Color BG_DARK    = new Color(26, 26, 46);
    static final Color BG_PANEL   = new Color(22, 33, 62);
    static final Color ACCENT     = new Color(233, 69, 96);
    static final Color BTN_COLOR  = new Color(15, 52, 96);
    static final Color TEXT_COLOR = new Color(238, 238, 238);

    public ClientGUI(PDFService service) {
        this.service = service;
        setTitle("Client PDF CORBA");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        header.setBackground(BG_PANEL);
        header.setBorder(BorderFactory.createMatteBorder(
            0, 0, 2, 0, ACCENT));
        JLabel title = new JLabel("  PDF CORBA Client");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ACCENT);
        JLabel sub = new JLabel("— Connecte au serveur CORBA");
        sub.setForeground(new Color(150, 150, 150));
        header.add(title);
        header.add(sub);
        add(header, BorderLayout.NORTH);

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_PANEL);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(
            0, 0, 0, 1, BTN_COLOR));

        String[] menus = {
            "Creer un PDF",
            "Fusionner",
            "Decouper",
            "Supprimer page",
            "Mot de passe",
            "Convertir en image",
            "Extraire texte"
        };

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BG_DARK);

        // Créer les panels
        mainPanel.add(panelCreer(),   "Creer un PDF");
        mainPanel.add(panelFusionner(), "Fusionner");
        mainPanel.add(panelDecouper(), "Decouper");
        mainPanel.add(panelSupprimer(), "Supprimer page");
        mainPanel.add(panelPassword(), "Mot de passe");
        mainPanel.add(panelImage(),   "Convertir en image");
        mainPanel.add(panelExtraire(), "Extraire texte");

        // Boutons sidebar
        for (String menu : menus) {
            JButton btn = sidebarButton(menu);
            btn.addActionListener(e -> cardLayout.show(mainPanel, menu));
            sidebar.add(btn);
        }
        sidebar.add(Box.createVerticalGlue());

        add(sidebar, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    // ── Helpers UI ──────────────────────────────────────────

    JButton sidebarButton(String text) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(200, 45));
        b.setPreferredSize(new Dimension(200, 45));
        b.setBackground(BG_PANEL);
        b.setForeground(TEXT_COLOR);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(BTN_COLOR);
                b.setForeground(ACCENT);
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(BG_PANEL);
                b.setForeground(TEXT_COLOR);
            }
        });
        return b;
    }

    JPanel basePanel(String titre) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(30, 40, 30, 40));
        JLabel t = new JLabel(titre);
        t.setFont(new Font("Segoe UI", Font.BOLD, 18));
        t.setForeground(ACCENT);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(20));
        return p;
    }

    JButton actionButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(10, 24, 10, 24));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    JTextField styledField(String placeholder) {
        JTextField f = new JTextField(placeholder, 25);
        f.setBackground(BTN_COLOR);
        f.setForeground(TEXT_COLOR);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(26, 74, 138)),
            new EmptyBorder(6, 10, 6, 10)));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setMaximumSize(new Dimension(400, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(170, 170, 170));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    JLabel resultLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(new Color(46, 204, 113));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    File choisirFichier(String titre) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(titre);
        int r = fc.showOpenDialog(this);
        return r == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    File sauvegarderFichier(String nom) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(nom));
        int r = fc.showSaveDialog(this);
        return r == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    void showSuccess(JLabel lbl, String msg) {
        lbl.setForeground(new Color(46, 204, 113));
        lbl.setText("✔ " + msg);
    }

    void showError(JLabel lbl, String msg) {
        lbl.setForeground(new Color(231, 76, 60));
        lbl.setText("✘ " + msg);
    }

    // ── Panels ──────────────────────────────────────────────

    JPanel panelCreer() {
        JPanel p = basePanel("Creer un PDF");
        p.add(label("Contenu du PDF :"));
        p.add(Box.createVerticalStrut(6));
        JTextArea area = new JTextArea(5, 30);
        area.setBackground(BTN_COLOR);
        area.setForeground(TEXT_COLOR);
        area.setCaretColor(Color.WHITE);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setMaximumSize(new Dimension(400, 120));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(scroll);
        p.add(Box.createVerticalStrut(14));
        JButton btn = actionButton("Creer le PDF");
        JLabel res = resultLabel();
        btn.addActionListener(e -> {
            try {
                String contenu = area.getText();
                byte[] result = service.creerPDF(contenu);
                File out = sauvegarderFichier("nouveau.pdf");
                if (out != null) {
                    Files.write(out.toPath(), result);
                    showSuccess(res, "PDF cree : " + out.getName());
                }
            } catch (Exception ex) {
                showError(res, ex.getMessage());
            }
        });
        p.add(btn);
        p.add(Box.createVerticalStrut(12));
        p.add(res);
        return p;
    }

    JPanel panelFusionner() {
        JPanel p = basePanel("Fusionner deux PDFs");
        JLabel f1Label = label("Aucun fichier selectionne");
        JLabel f2Label = label("Aucun fichier selectionne");
        final File[] pdf1 = {null};
        final File[] pdf2 = {null};

        JButton b1 = actionButton("Choisir PDF 1");
        JButton b2 = actionButton("Choisir PDF 2");
        JButton bFus = actionButton("Fusionner");
        JLabel res = resultLabel();

        b1.addActionListener(e -> {
            pdf1[0] = choisirFichier("PDF 1");
            if (pdf1[0] != null) f1Label.setText(pdf1[0].getName());
        });
        b2.addActionListener(e -> {
            pdf2[0] = choisirFichier("PDF 2");
            if (pdf2[0] != null) f2Label.setText(pdf2[0].getName());
        });
        bFus.addActionListener(e -> {
            try {
                if (pdf1[0] == null || pdf2[0] == null) {
                    showError(res, "Choisissez les 2 PDFs");
                    return;
                }
                byte[] r = service.fusionner(
                    Files.readAllBytes(pdf1[0].toPath()),
                    Files.readAllBytes(pdf2[0].toPath()));
                File out = sauvegarderFichier("fusion.pdf");
                if (out != null) {
                    Files.write(out.toPath(), r);
                    showSuccess(res, "Fusionne : " + out.getName());
                }
            } catch (Exception ex) { showError(res, ex.getMessage()); }
        });

        p.add(b1); p.add(Box.createVerticalStrut(4));
        p.add(f1Label); p.add(Box.createVerticalStrut(10));
        p.add(b2); p.add(Box.createVerticalStrut(4));
        p.add(f2Label); p.add(Box.createVerticalStrut(14));
        p.add(bFus); p.add(Box.createVerticalStrut(12));
        p.add(res);
        return p;
    }

    JPanel panelDecouper() {
        JPanel p = basePanel("Decouper un PDF");
        JLabel fLabel = label("Aucun fichier selectionne");
        final File[] pdf = {null};
        JTextField debut = styledField("1");
        JTextField fin   = styledField("1");
        JButton bChoix = actionButton("Choisir PDF");
        JButton bDec   = actionButton("Decouper");
        JLabel res = resultLabel();

        bChoix.addActionListener(e -> {
            pdf[0] = choisirFichier("Choisir PDF");
            if (pdf[0] != null) fLabel.setText(pdf[0].getName());
        });
        bDec.addActionListener(e -> {
            try {
                byte[] r = service.decouper(
                    Files.readAllBytes(pdf[0].toPath()),
                    Integer.parseInt(debut.getText()),
                    Integer.parseInt(fin.getText()));
                File out = sauvegarderFichier("decoupe.pdf");
                if (out != null) {
                    Files.write(out.toPath(), r);
                    showSuccess(res, "Decoupe : " + out.getName());
                }
            } catch (Exception ex) { showError(res, ex.getMessage()); }
        });

        p.add(bChoix); p.add(Box.createVerticalStrut(4));
        p.add(fLabel); p.add(Box.createVerticalStrut(12));
        p.add(label("Page debut :")); p.add(debut);
        p.add(Box.createVerticalStrut(8));
        p.add(label("Page fin :")); p.add(fin);
        p.add(Box.createVerticalStrut(14));
        p.add(bDec); p.add(Box.createVerticalStrut(12));
        p.add(res);
        return p;
    }

    JPanel panelSupprimer() {
        JPanel p = basePanel("Supprimer une page");
        JLabel fLabel = label("Aucun fichier selectionne");
        final File[] pdf = {null};
        JTextField page = styledField("1");
        JButton bChoix = actionButton("Choisir PDF");
        JButton bSup   = actionButton("Supprimer");
        JLabel res = resultLabel();

        bChoix.addActionListener(e -> {
            pdf[0] = choisirFichier("Choisir PDF");
            if (pdf[0] != null) fLabel.setText(pdf[0].getName());
        });
        bSup.addActionListener(e -> {
            try {
                byte[] r = service.supprimerPage(
                    Files.readAllBytes(pdf[0].toPath()),
                    Integer.parseInt(page.getText()));
                File out = sauvegarderFichier("sans_page.pdf");
                if (out != null) {
                    Files.write(out.toPath(), r);
                    showSuccess(res, "Page supprimee : " + out.getName());
                }
            } catch (Exception ex) { showError(res, ex.getMessage()); }
        });

        p.add(bChoix); p.add(Box.createVerticalStrut(4));
        p.add(fLabel); p.add(Box.createVerticalStrut(12));
        p.add(label("Numero de page :")); p.add(page);
        p.add(Box.createVerticalStrut(14));
        p.add(bSup); p.add(Box.createVerticalStrut(12));
        p.add(res);
        return p;
    }

    JPanel panelPassword() {
        JPanel p = basePanel("Ajouter un mot de passe");
        JLabel fLabel = label("Aucun fichier selectionne");
        final File[] pdf = {null};
        JPasswordField mdp = new JPasswordField(20);
        mdp.setBackground(BTN_COLOR);
        mdp.setForeground(TEXT_COLOR);
        mdp.setCaretColor(Color.WHITE);
        mdp.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(26, 74, 138)),
            new EmptyBorder(6, 10, 6, 10)));
        mdp.setMaximumSize(new Dimension(400, 36));
        mdp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton bChoix = actionButton("Choisir PDF");
        JButton bPwd   = actionButton("Proteger");
        JLabel res = resultLabel();

        bChoix.addActionListener(e -> {
            pdf[0] = choisirFichier("Choisir PDF");
            if (pdf[0] != null) fLabel.setText(pdf[0].getName());
        });
        bPwd.addActionListener(e -> {
            try {
                byte[] r = service.ajouterMotDePasse(
                    Files.readAllBytes(pdf[0].toPath()),
                    new String(mdp.getPassword()));
                File out = sauvegarderFichier("protege.pdf");
                if (out != null) {
                    Files.write(out.toPath(), r);
                    showSuccess(res, "PDF protege : " + out.getName());
                }
            } catch (Exception ex) { showError(res, ex.getMessage()); }
        });

        p.add(bChoix); p.add(Box.createVerticalStrut(4));
        p.add(fLabel); p.add(Box.createVerticalStrut(12));
        p.add(label("Mot de passe :")); p.add(mdp);
        p.add(Box.createVerticalStrut(14));
        p.add(bPwd); p.add(Box.createVerticalStrut(12));
        p.add(res);
        return p;
    }

    JPanel panelImage() {
        JPanel p = basePanel("Convertir page en image");
        JLabel fLabel = label("Aucun fichier selectionne");
        final File[] pdf = {null};
        JTextField page = styledField("1");
        JButton bChoix = actionButton("Choisir PDF");
        JButton bImg   = actionButton("Convertir");
        JLabel res = resultLabel();
        JLabel imgPreview = new JLabel();
        imgPreview.setAlignmentX(Component.LEFT_ALIGNMENT);

        bChoix.addActionListener(e -> {
            pdf[0] = choisirFichier("Choisir PDF");
            if (pdf[0] != null) fLabel.setText(pdf[0].getName());
        });
        bImg.addActionListener(e -> {
            try {
                byte[] r = service.convertirEnImage(
                    Files.readAllBytes(pdf[0].toPath()),
                    Integer.parseInt(page.getText()));
                File out = sauvegarderFichier("page.png");
                if (out != null) {
                    Files.write(out.toPath(), r);
                    // Afficher preview
                    ImageIcon icon = new ImageIcon(r);
                    Image scaled = icon.getImage().getScaledInstance(
                        300, -1, Image.SCALE_SMOOTH);
                    imgPreview.setIcon(new ImageIcon(scaled));
                    showSuccess(res, "Image sauvegardee : " + out.getName());
                }
            } catch (Exception ex) { showError(res, ex.getMessage()); }
        });

        p.add(bChoix); p.add(Box.createVerticalStrut(4));
        p.add(fLabel); p.add(Box.createVerticalStrut(12));
        p.add(label("Numero de page :")); p.add(page);
        p.add(Box.createVerticalStrut(14));
        p.add(bImg); p.add(Box.createVerticalStrut(12));
        p.add(res); p.add(Box.createVerticalStrut(10));
        p.add(imgPreview);
        return p;
    }

    JPanel panelExtraire() {
        JPanel p = basePanel("Extraire le texte");
        JLabel fLabel = label("Aucun fichier selectionne");
        final File[] pdf = {null};
        JButton bChoix = actionButton("Choisir PDF");
        JButton bExt   = actionButton("Extraire");
        JLabel res = resultLabel();
        JTextArea output = new JTextArea(8, 35);
        output.setBackground(BTN_COLOR);
        output.setForeground(TEXT_COLOR);
        output.setFont(new Font("Monospaced", Font.PLAIN, 12));
        output.setEditable(false);
        output.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(output);
        scroll.setMaximumSize(new Dimension(500, 200));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        bChoix.addActionListener(e -> {
            pdf[0] = choisirFichier("Choisir PDF");
            if (pdf[0] != null) fLabel.setText(pdf[0].getName());
        });
        bExt.addActionListener(e -> {
            try {
                String texte = service.extraireTexte(
                    Files.readAllBytes(pdf[0].toPath()));
                output.setText(texte);
                showSuccess(res, "Texte extrait avec succes !");
            } catch (Exception ex) { showError(res, ex.getMessage()); }
        });

        p.add(bChoix); p.add(Box.createVerticalStrut(4));
        p.add(fLabel); p.add(Box.createVerticalStrut(14));
        p.add(bExt); p.add(Box.createVerticalStrut(12));
        p.add(res); p.add(Box.createVerticalStrut(10));
        p.add(scroll);
        return p;
    }

    // ── Main ────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}

        try {
            ORB orb = ORB.init(args, null);
            NamingContextExt ncRef = NamingContextExtHelper.narrow(
                orb.resolve_initial_references("NameService"));
            PDFService service = PDFServiceHelper.narrow(
                ncRef.resolve_str("PDFService"));

            SwingUtilities.invokeLater(() -> new ClientGUI(service));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Impossible de se connecter au serveur CORBA !\n"
                + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
