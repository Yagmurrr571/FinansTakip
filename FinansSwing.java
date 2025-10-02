import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class FinansSwing extends JFrame {
    private DefaultListModel<String> listeModel;
    private JList<String> liste;
    private JTextField miktarField;
    private JComboBox<String> turBox, kategoriBox, filtreBox;
    private final String DOSYA_ADI = "veriler.txt";

    private ArrayList<Islem> islemler;
    private GrafikPanel toplamGrafik, filtreGrafik;
    private JLabel mesajLabel;

    static class Islem {
        String tur, kategori;
        double miktar;
        Color renk;
        Islem(String tur, String kategori, double miktar) {
            this.tur = tur;
            this.kategori = kategori;
            this.miktar = miktar;
            this.renk = Color.GRAY;
        }
    }

    public FinansSwing() {
        super("Finans Takip");
        islemler = new ArrayList<>();
        loadData();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000,550);
        setLayout(new BorderLayout());

        // Sol panel
        JPanel solPanel = new JPanel();
        solPanel.setLayout(new GridLayout(9,1,5,5));

        turBox = new JComboBox<>(new String[]{"Gelir","Gider"});
        kategoriBox = new JComboBox<>(new String[]{"Genel","Yemek","Ulaşım","Kira","Eğlence"});
        filtreBox = new JComboBox<>(new String[]{"Hepsi","Genel","Yemek","Ulaşım","Kira","Eğlence"});
        miktarField = new JTextField();
        JButton ekleBtn = new JButton("Ekle");

        solPanel.add(new JLabel("Tür:"));
        solPanel.add(turBox);
        solPanel.add(new JLabel("Kategori:"));
        solPanel.add(kategoriBox);
        solPanel.add(new JLabel("Miktar:"));
        solPanel.add(miktarField);
        solPanel.add(ekleBtn);
        solPanel.add(new JLabel("Filtrele:"));
        solPanel.add(filtreBox);

        // Orta panel: liste
        listeModel = new DefaultListModel<>();
        liste = new JList<>(listeModel);
        JScrollPane scroll = new JScrollPane(liste);

        // Sağ panel: grafik
        toplamGrafik = new GrafikPanel(islemler, "Hepsi");
        filtreGrafik = new GrafikPanel(islemler, "Hepsi");

        JPanel sagPanel = new JPanel();
        sagPanel.setLayout(new GridLayout(2,1));
        sagPanel.add(toplamGrafik);
        sagPanel.add(filtreGrafik);

        add(solPanel, BorderLayout.WEST);
        add(scroll, BorderLayout.CENTER);
        add(sagPanel, BorderLayout.EAST);

        // Mesaj kutusu (espirili)
        mesajLabel = new JLabel("Hoşgeldiniz! Bugün bütçenizi takip edebilirsiniz.");
        mesajLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mesajLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(mesajLabel, BorderLayout.SOUTH);

        updateListe();

        // İşlem ekleme
        ekleBtn.addActionListener(e -> {
            try {
                String tur = (String) turBox.getSelectedItem();
                String kategori = (String) kategoriBox.getSelectedItem();
                double miktar = Double.parseDouble(miktarField.getText());
                Islem i = new Islem(tur, kategori, miktar);
                islemler.add(i);
                saveIslem(i);
                updateListe();
                miktarField.setText("");
                toplamGrafik.startAnimation();
                filtreGrafik.startAnimation((String) filtreBox.getSelectedItem());
            } catch(Exception ex) {
                JOptionPane.showMessageDialog(this,"Geçersiz miktar!");
            }
        });

        // Filtreleme
        filtreBox.addActionListener(e -> {
            String secilen = (String) filtreBox.getSelectedItem();
            filtreGrafik.startAnimation(secilen);
        });
    }

    private void updateListe() {
        listeModel.clear();
        double toplamGelir = 0;
        double toplamGider = 0;
        for(Islem i : islemler) {
            listeModel.addElement(i.tur + " (" + i.kategori + "): " + i.miktar);
            if(i.tur.equals("Gelir")) toplamGelir += i.miktar;
            else toplamGider += i.miktar;
        }
        listeModel.addElement("=== TOPLAM ===");
        listeModel.addElement("Gelir: " + toplamGelir);
        listeModel.addElement("Gider: " + toplamGider);
        listeModel.addElement("Bakiye: " + (toplamGelir - toplamGider));

        // Espirili mesaj
        String mesaj = "";
        if(toplamGider > toplamGelir * 0.8) {
            mesaj = "Vay canına, bu ay bol gezmişsiniz gibi! 🍕✈️";
        } else if(toplamGelir > toplamGider * 2) {
            mesaj = "Cebin dolu, kutlama zamanı! 🤑";
        } else {
            mesaj = "Dikkat, bu ay biraz dikkatli harcayın. 💸";
        }
        mesajLabel.setText(mesaj);
    }

    private void loadData() {
        try {
            File f = new File(DOSYA_ADI);
            if(f.exists()) {
                Scanner sc = new Scanner(f);
                while(sc.hasNextLine()) {
                    String[] parts = sc.nextLine().split(",");
                    if(parts.length==3) {
                        islemler.add(new Islem(parts[0], parts[1], Double.parseDouble(parts[2])));
                    }
                }
                sc.close();
            }
        } catch(Exception e) { System.out.println("Dosya okunamadı: " + e.getMessage()); }
    }

    private void saveIslem(Islem i) {
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(DOSYA_ADI,true)))) {
            out.println(i.tur + "," + i.kategori + "," + i.miktar);
        } catch(IOException e) { System.out.println("Kaydedilemedi: "+e.getMessage()); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FinansSwing f = new FinansSwing();
            f.setVisible(true);
        });
    }

    // ---------------- Grafik Panel ----------------
    class GrafikPanel extends JPanel {
        private ArrayList<Islem> islemler;
        private Map<String, Color> kategoriRenk;
        private Map<Islem, Integer> currentAngles;
        private javax.swing.Timer timer;
        private String filtre = "Hepsi";

        public GrafikPanel(ArrayList<Islem> islemler, String filtre) {
            this.islemler = islemler;
            this.filtre = filtre;
            setPreferredSize(new Dimension(400,200));

            kategoriRenk = new HashMap<>();
            kategoriRenk.put("Genel", Color.GRAY);
            kategoriRenk.put("Yemek", Color.ORANGE);
            kategoriRenk.put("Ulaşım", Color.CYAN);
            kategoriRenk.put("Kira", Color.RED);
            kategoriRenk.put("Eğlence", Color.MAGENTA);

            currentAngles = new HashMap<>();
            for(Islem i : islemler) currentAngles.put(i, 0);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    setToolTipText(getTooltipText(e.getX(), e.getY()));
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    String detay = getTooltipText(e.getX(), e.getY());
                    if(detay != null) JOptionPane.showMessageDialog(GrafikPanel.this, detay, "İşlem Detayı", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            timer = new javax.swing.Timer(15, e -> {
                boolean repaintNeeded = false;
                double toplam = toplamMiktar(filtre);
                for(Islem i : islemler) {
                    if(!filtre.equals("Hepsi") && !i.kategori.equals(filtre)) continue;
                    int targetAngle = (int)((i.miktar / toplam) * 360);
                    int current = currentAngles.getOrDefault(i, 0);
                    if(current < targetAngle) {
                        currentAngles.put(i, Math.min(current + 4, targetAngle));
                        i.renk = i.tur.equals("Gelir") ? new Color(Math.min(255,current*2),255,Math.min(255,current*2)) :
                                new Color(255,Math.min(255,current*2),Math.min(255,current*2));
                        repaintNeeded = true;
                    }
                }
                if(repaintNeeded) repaint();
            });
        }

        public void startAnimation() { startAnimation(this.filtre); }

        public void startAnimation(String filtre) {
            this.filtre = filtre;
            for(Islem i : islemler) currentAngles.put(i, 0);
            timer.start();
        }

        private double toplamMiktar(String filtre) {
            double toplam = 0;
            for(Islem i : islemler) {
                if(filtre.equals("Hepsi") || i.kategori.equals(filtre)) toplam += i.miktar;
            }
            return toplam == 0 ? 1 : toplam;
        }

        private String getTooltipText(int x, int y) {
            int width = getWidth(), height = getHeight();
            int diameter = Math.min(width, height) - 50;
            int centerX = 25 + diameter/2;
            int centerY = 25 + diameter/2;
            int dx = x - centerX;
            int dy = y - centerY;
            double distance = Math.sqrt(dx*dx + dy*dy);
            if(distance > diameter/2) return null;
            double angle = Math.toDegrees(Math.atan2(dy, dx));
            angle = (angle < 0) ? angle + 360 : angle;
            double start = 0;
            double toplam = toplamMiktar(filtre);
            for(Islem i : islemler) {
                if(!filtre.equals("Hepsi") && !i.kategori.equals(filtre)) continue;
                double sliceAngle = (i.miktar / toplam) * 360;
                if(angle >= start && angle <= start + sliceAngle) {
                    double yuzde = (i.miktar/toplam)*100;
                    return i.tur + " (" + i.kategori + "): " + i.miktar + " (" + String.format("%.1f",yuzde) + "%)";
                }
                start += sliceAngle;
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int startAngle = 0;
            int width = getWidth(), height = getHeight();
            int diameter = Math.min(width, height) - 50;

            for(Islem i : islemler) {
                if(!filtre.equals("Hepsi") && !i.kategori.equals(filtre)) continue;
                int angle = currentAngles.getOrDefault(i, 0);
                g.setColor(i.renk != null ? i.renk : kategoriRenk.getOrDefault(i.kategori, i.tur.equals("Gelir") ? Color.GREEN : Color.RED));
                g.fillArc(25,25,diameter,diameter,startAngle,angle);
                startAngle += angle;
            }

            double toplamGelir = islemler.stream()
                    .filter(i->i.tur.equals("Gelir") && (filtre.equals("Hepsi") || i.kategori.equals(filtre)))
                    .mapToDouble(i->i.miktar).sum();
            double toplamGider = islemler.stream()
                    .filter(i->i.tur.equals("Gider") && (filtre.equals("Hepsi") || i.kategori.equals(filtre)))
                    .mapToDouble(i->i.miktar).sum();

            g.setColor(Color.BLACK);
            g.drawString("Gelir: " + toplamGelir, 30, diameter + 50);
            g.drawString("Gider: " + toplamGider, 30, diameter + 65);
            g.drawString("Filtre: " + filtre, 30, diameter + 80);
        }
    }
}

