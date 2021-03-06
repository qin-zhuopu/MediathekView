/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.gui.dialog;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import mSearch.tool.FilenameUtils;
import mediathek.config.Daten;
import mediathek.config.Icons;
import mediathek.config.MVColor;
import mediathek.daten.DatenAbo;
import mediathek.file.GetFile;
import mediathek.tool.EscBeenden;
import mediathek.tool.GuiFunktionen;
import mediathek.tool.MVMessageDialog;

public class DialogEditAbo extends javax.swing.JDialog {

    private final DatenAbo aktAbo;
    private JTextField[] textfeldListe;
    private final JComboBox<String> comboboxPSet = new JComboBox<>();
    private final JComboBox<String> comboboxSender = new JComboBox<>();
    private final JComboBox<String> comboboxPfad = new JComboBox<>();
    private final JCheckBox checkBoxEingeschaltet = new JCheckBox();
    private final JSlider sliderDauer = new JSlider(0, 100, 0);
    private final JLabel labelDauer = new JLabel("0");
    public boolean ok = false;
    private final JFrame parent;

    public DialogEditAbo(final JFrame parent, boolean modal, Daten d, DatenAbo aktA) {
        super(parent, modal);
        initComponents();
        this.parent = parent;
        aktAbo = aktA;
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        comboboxPSet.setModel(new javax.swing.DefaultComboBoxModel<>(Daten.listePset.getListeAbo().getObjectDataCombo()));
        comboboxSender.setModel(new javax.swing.DefaultComboBoxModel<>(GuiFunktionen.addLeerListe(Daten.filmeLaden.getSenderNamen())));
        // Zeilpfad ========================
        ArrayList<String> pfade = Daten.listeAbo.getPfade();
        if (!pfade.contains(aktAbo.arr[DatenAbo.ABO_ZIELPFAD])) {
            pfade.add(0, aktAbo.arr[DatenAbo.ABO_ZIELPFAD]);
        }
        comboboxPfad.setModel(new javax.swing.DefaultComboBoxModel<>(pfade.toArray(new String[pfade.size()])));
        comboboxPfad.setEditable(true);
        checkPfad();
        ((JTextComponent) comboboxPfad.getEditor().getEditorComponent()).setOpaque(true);
        ((JTextComponent) comboboxPfad.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                checkPfad();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkPfad();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkPfad();
            }

        });
        // =====================
        jButtonBeenden.addActionListener(e -> {
            if (check()) {
                beenden();
            } else {
                MVMessageDialog.showMessageDialog(parent, "Filter angeben!", "Leeres Abo", JOptionPane.ERROR_MESSAGE);
            }
        });
        jButtonAbbrechen.addActionListener(e -> beenden());
        getRootPane().setDefaultButton(jButtonBeenden);
        new EscBeenden(this) {
            @Override
            public void beenden_() {
                beenden();
            }
        };
        jButtonHelp.setIcon(Icons.ICON_BUTTON_HELP);
        jButtonHelp.addActionListener(e -> new DialogHilfe(parent, true, new GetFile().getHilfeSuchen(GetFile.PFAD_HILFETEXT_DIALOG_ADD_ABO)).setVisible(true));

        if (comboboxPSet.getModel().getSize() == 0) {
            // dann gibts kein Set zum Aufzeichnen
            new DialogAboNoSet(parent, d).setVisible(true);
        } else {
            setExtra();
        }
    }

    @Override
    public void setVisible(boolean vis) {
        if (comboboxPSet.getModel().getSize() == 0) {
            // dann gibts kein Set zum Aufzeichnen
            beenden();
        } else {
            super.setVisible(vis);
        }
    }

    private void checkPfad() {
        String s = ((JTextComponent) comboboxPfad.getEditor().getEditorComponent()).getText();
        if (!s.equals(FilenameUtils.checkDateiname(s, false /*pfad*/))) {
            comboboxPfad.getEditor().getEditorComponent().setBackground(MVColor.DOWNLOAD_FEHLER.color);
        } else {
            comboboxPfad.getEditor().getEditorComponent().setBackground(Color.WHITE);
        }
    }

    private void setExtra() {
        textfeldListe = new JTextField[DatenAbo.MAX_ELEM];
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 10, 10, 5);
        jPanelExtra.setLayout(gridbag);
        int zeile = 0;
        for (int i = 0; i < DatenAbo.MAX_ELEM; ++i) {
            addExtraFeld(i, gridbag, c, jPanelExtra);
            ++zeile;
            c.gridy = zeile;
        }
    }

    private void addExtraFeld(int i, GridBagLayout gridbag, GridBagConstraints c,
            JPanel panel) {
        //Label
        c.gridx = 0;
        c.weightx = 0;
        JLabel label;
        if (i == DatenAbo.ABO_SENDER || i == DatenAbo.ABO_THEMA || i == DatenAbo.ABO_TITEL || i == DatenAbo.ABO_THEMA_TITEL || i == DatenAbo.ABO_IRGENDWO) {
            label = new JLabel("  " + DatenAbo.COLUMN_NAMES[i] + ": ");
            label.setForeground(Color.BLUE);
        } else {
            label = new JLabel(DatenAbo.COLUMN_NAMES[i] + ": ");
        }
        gridbag.setConstraints(label, c);
        panel.add(label);
        //Textfeld
        c.gridx = 1;
        c.weightx = 10;
        if (i == DatenAbo.ABO_PSET) {
            comboboxPSet.setSelectedItem(aktAbo.arr[i]);
            //falls das Feld leer war, wird es jetzt auf den ersten Eintrag gesetzt
            aktAbo.arr[DatenAbo.ABO_PSET] = comboboxPSet.getSelectedItem().toString(); // damit immer eine Set eingetragen ist!
            gridbag.setConstraints(comboboxPSet, c);
            panel.add(comboboxPSet);
        } else if (i == DatenAbo.ABO_SENDER) {
            comboboxSender.setSelectedItem(aktAbo.arr[i]);
            gridbag.setConstraints(comboboxSender, c);
            panel.add(comboboxSender);
        } else if (i == DatenAbo.ABO_ZIELPFAD) {
            comboboxPfad.setSelectedItem(aktAbo.arr[i]);
            gridbag.setConstraints(comboboxPfad, c);
            panel.add(comboboxPfad);
        } else if (i == DatenAbo.ABO_MINDESTDAUER) {
            sliderDauer.setValue(aktAbo.mindestdauerMinuten);
            labelDauer.setText(String.valueOf(aktAbo.mindestdauerMinuten));
            sliderDauer.addChangeListener(e -> labelDauer.setText("  " + sliderDauer.getValue() + " "));
            JPanel p = new JPanel(new BorderLayout());
            p.add(sliderDauer, BorderLayout.CENTER);
            p.add(labelDauer, BorderLayout.EAST);
            gridbag.setConstraints(p, c);
            panel.add(p);
        } else if (i == DatenAbo.ABO_EINGESCHALTET) {
            checkBoxEingeschaltet.setSelected(Boolean.parseBoolean(aktAbo.arr[i]));
            gridbag.setConstraints(checkBoxEingeschaltet, c);
            panel.add(checkBoxEingeschaltet);
        } else {
            JTextField textfeld = new JTextField();
            textfeldListe[i] = textfeld;
            if (i == DatenAbo.ABO_NR
                    || i == DatenAbo.ABO_DOWN_DATUM) {
                textfeld.setEditable(false);
            }
            if (i == DatenAbo.ABO_NAME) {
                textfeld.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        textfeldListe[DatenAbo.ABO_NAME].setBackground(textfeldListe[DatenAbo.ABO_NAME].getText().isEmpty() ? Color.red : Color.white);
                        jButtonBeenden.setEnabled(!textfeldListe[DatenAbo.ABO_NAME].getText().isEmpty());
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        textfeldListe[DatenAbo.ABO_NAME].setBackground(textfeldListe[DatenAbo.ABO_NAME].getText().isEmpty() ? Color.red : Color.white);
                        jButtonBeenden.setEnabled(!textfeldListe[DatenAbo.ABO_NAME].getText().isEmpty());
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        textfeldListe[DatenAbo.ABO_NAME].setBackground(textfeldListe[DatenAbo.ABO_NAME].getText().isEmpty() ? Color.red : Color.white);
                        jButtonBeenden.setEnabled(!textfeldListe[DatenAbo.ABO_NAME].getText().isEmpty());
                    }
                });
            }
            textfeld.setText(aktAbo.arr[i]);
            gridbag.setConstraints(textfeld, c);
            panel.add(textfeld);
        }
    }

    private boolean check() {
        DatenAbo test = new DatenAbo();
        get(test);
        if (test.isEmpty()) {
            ok = false;
        } else {
            get(aktAbo);
            ok = true;
        }
        return ok;
    }

    private void get(DatenAbo abo) {
        for (int i = 0; i < DatenAbo.MAX_ELEM; ++i) {
            switch (i) {
                case (DatenAbo.ABO_ZIELPFAD):
                    abo.arr[DatenAbo.ABO_ZIELPFAD] = comboboxPfad.getSelectedItem().toString();
                    break;
                case (DatenAbo.ABO_PSET):
                    abo.arr[DatenAbo.ABO_PSET] = comboboxPSet.getSelectedItem().toString();
                    break;
                case (DatenAbo.ABO_SENDER):
                    abo.arr[DatenAbo.ABO_SENDER] = comboboxSender.getSelectedItem().toString();
                    break;
                case (DatenAbo.ABO_EINGESCHALTET):
                    abo.arr[DatenAbo.ABO_EINGESCHALTET] = Boolean.toString(checkBoxEingeschaltet.isSelected());
                    break;
                case (DatenAbo.ABO_MINDESTDAUER):
                    abo.setMindestDauerMinuten(sliderDauer.getValue());
                    break;
                case (DatenAbo.ABO_NR):
                case (DatenAbo.ABO_DOWN_DATUM):
                    break;
                default:
                    abo.arr[i] = textfeldListe[i].getText().trim();
                    break;
            }
        }
    }

    private void beenden() {
        this.dispose();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanelExtra = new javax.swing.JPanel();
        jButtonAbbrechen = new javax.swing.JButton();
        jButtonBeenden = new javax.swing.JButton();
        jButtonHelp = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout jPanelExtraLayout = new javax.swing.GroupLayout(jPanelExtra);
        jPanelExtra.setLayout(jPanelExtraLayout);
        jPanelExtraLayout.setHorizontalGroup(
            jPanelExtraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 513, Short.MAX_VALUE)
        );
        jPanelExtraLayout.setVerticalGroup(
            jPanelExtraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 503, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(jPanelExtra);

        jButtonAbbrechen.setText("Abbrechen");

        jButtonBeenden.setText("Ok");

        jButtonHelp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/muster/button-help.png"))); // NOI18N
        jButtonHelp.setToolTipText("Hilfe anzeigen");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonBeenden, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAbbrechen)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonHelp)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonAbbrechen, jButtonBeenden});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonBeenden)
                    .addComponent(jButtonAbbrechen)
                    .addComponent(jButtonHelp))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAbbrechen;
    private javax.swing.JButton jButtonBeenden;
    private javax.swing.JButton jButtonHelp;
    private javax.swing.JPanel jPanelExtra;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

}
