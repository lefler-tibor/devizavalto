package hu.gov.allamkincstar.devizavalto;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DevizaValto extends javax.swing.JFrame implements ActionListener {

    private static final String XML_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    private static final String CURRENCY = "currency";
    private static final String RATE = "rate";
    private float forintArfolyam;

    public DevizaValto() {
        initComponents();

        jButton1.addActionListener(this);
        jButton2.addActionListener(this);
        jComboBox1.addActionListener(this);
    }

    /**
     * A formon található két gomb bármilyikének megnyomásakor lefutó metódus.
     * A tallózás gomb megnyomásakor:
     *      1. A megfelelő URL-ről letöltődik a friss devizaárfolyamokat
     *         tartalmazó xml.
     *      2. Megnyílik egy fájlkiválasztó ablak, ahol a frissen leszedett xml
     *         vagy egy korábbi változat kiválasztható.
     *      3. A kiválasztott xml adataiból feltöltődik a devizanemeket és
     *         árfolyamaikat tartalmazó értéklista.
     *
     * Az átváltás gomb megnyomásakor az átváltandó összeg és a kiválasztott
     * valuta alapján megtörténik a forintba történő átváltás.
     *
     * @param ae Esemény, melyet a tallóz vagy az átváltás gomb megnyomása vált
     *           ki.
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        String command = ae.getActionCommand();

        if ("atvaltas".equals(command)) {
            //Ha van mit átváltani (azaz van érték az átváltandó mezőbe), akkor a beírt összeget
            //osztom a kiválasztott deviza árfolyamával, amjd a kapott értéket szorzom a forintnál
            //megtalálható árfolyammal. (5 forintra kerekítésre figyelni.)           
            if (jComboBox1.getSelectedItem() != null
                    && !jComboBox1.getSelectedItem().toString().isEmpty()
                    && jTextField1.getText() != null
                    && !jTextField1.getText().isEmpty()) {
                float atvaltando = Float.parseFloat(jTextField1.getText());
                float kivalasztottArfolyam = Float.parseFloat(jComboBox1.getSelectedItem().toString().substring(jComboBox1.getSelectedItem().toString().indexOf("-") + 1));
                long atvaltottOsszegForintban = Math.round(((atvaltando / kivalasztottArfolyam) * forintArfolyam) / 5) * 5L;
                
                NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
                jLabel6.setText(format.format(atvaltottOsszegForintban));
            }

        } else if ("talloz".equals(command)) {
            //Friss árfolyamadatok leszedése a netről
            String letoltottXmlHelye = arfolyamXmlLeszed();

            //Fájlválasztó indítása csak xml fájlokra
            JFileChooser fileChooser = new JFileChooser(letoltottXmlHelye);
            fileChooser.setDialogTitle("XML kiválasztása");
            fileChooser.setAcceptAllFileFilterUsed(false);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("XML fájlok", "xml");
            fileChooser.addChoosableFileFilter(filter);

            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File konyvtar = fileChooser.getSelectedFile();
                jTextField3.setText(konyvtar.getAbsolutePath());

                //Lenyíló értéklista feltöltése xml-ből (devizaárfolyamok)
                java.util.List<String> arfolyamok = xmlOlvas(konyvtar);
                for (String a : arfolyamok) {
                    jComboBox1.addItem(a);
                }
            }
        
        } else if ("comboBoxChanged".equals(command)) {
            jLabel6.setText("?");
        } 
    }

    /**
     * Az árfolyam adatokat tartalmazó xml letöltése.
     *
     * @return A letöltött xml állomány elérési útvonala.
     */
    private String arfolyamXmlLeszed() {
        String absoluthPath = null;
        URL url;
        try {
            url = new URL(XML_URL);

            absoluthPath = urlOlvasasa(url);

        } catch (MalformedURLException ex) {
            Logger.getLogger(DevizaValto.class.getName()).log(Level.SEVERE, null, ex);
        }

        return absoluthPath;
    }

    /**
     * A paraméterként megadott URL-en található devizaárfolyamokat tartalmazó
     * xml állomány letöltését végző eljárás. A letöltött állomány nevében
     * másodperc szintig benne van a letöltés időpontja.
     *
     * @param url URL tipusú objektum, amely devizaárfolyamokat tartalmazó xml
     * állomány elérését tartalmazza.
     * @return A letöltött xml állomány elérési útvonala.
     */
    private String urlOlvasasa(URL url) {
        String absoluthPathVissza = null;
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream())) {
            File arfolyamXml = new File("arfolyam" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xml");
            absoluthPathVissza = arfolyamXml.getAbsolutePath();

            try (FileOutputStream fileOutputStream = new FileOutputStream(arfolyamXml)) {
                FileChannel fileChannel = fileOutputStream.getChannel();
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }

        } catch (IOException ex) {
            Logger.getLogger(DevizaValto.class.getName()).log(Level.SEVERE, null, ex);
        }
        return absoluthPathVissza;
    }

    /**
     * A paraméterként megadott devizaárfolyamokat tartalmazó xml állományból a
     * szükséges adatokat (devizanem és árfolyam) beletölti egy listába.
     *
     * @param xmlFajl A feldolgozandó xml állomány.
     * @return A feldolgozott állomány alapján előállított lista.
     */
    private java.util.List<String> xmlOlvas(File xmlFajl) {
        java.util.List<String> arfolyamok = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlFajl);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("Cube");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element elem = (Element) nodeList.item(i);
                if (elem.getAttribute(CURRENCY) != null && !elem.getAttribute(CURRENCY).isEmpty()) {
                    arfolyamok.add(elem.getAttribute(CURRENCY) + "-" + elem.getAttribute(RATE));

                    if ("HUF".equalsIgnoreCase(elem.getAttribute(CURRENCY))) {
                        forintArfolyam = Float.valueOf(elem.getAttribute(RATE));
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            java.util.logging.Logger.getLogger(DevizaValto.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
        }

        return arfolyamok;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jComboBox1 = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel6 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Devizaváltó");

        jLabel1.setText("Valuta választása");

        jLabel2.setText("Átváltandó összeg");

        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField1KeyTyped(evt);
            }
        });

        jButton1.setText("Átváltás");
        jButton1.setActionCommand("atvaltas");

        jLabel4.setText("Devizaárfolyam xml kiválasztása");
        jLabel4.setToolTipText("");

        jTextField3.setEditable(false);

        jButton2.setText("...");
        jButton2.setActionCommand("talloz");

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel5.setText("DEVIZAVÁLTÓ");

        jSeparator1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jSeparator1.setPreferredSize(new java.awt.Dimension(0, 1));

        jSeparator2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jSeparator2.setName(""); // NOI18N
        jSeparator2.setOpaque(true);
        jSeparator2.setPreferredSize(new java.awt.Dimension(0, 1));

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("?");
        jLabel6.setToolTipText("");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Átváltott összeg");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel1)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(76, 76, 76))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel5))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(jTextField3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton2))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 1, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addGap(6, 6, 6))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyTyped
        //Csak számokat és a törlő karaktereket lehet használni.
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
            evt.consume();            
        }
        //Ha változtat a beírt értéken, aakor a kiszámolt össszeg ürüljön.
        if (Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
            jLabel6.setText("?");
        }
    }//GEN-LAST:event_jTextField1KeyTyped

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DevizaValto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new DevizaValto().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField3;
    // End of variables declaration//GEN-END:variables

}
