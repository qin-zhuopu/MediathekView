/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.gui;

import mediathek.tool.MVFilmSize;
import mediathek.config.MVConfig;
import com.jidesoft.utils.SystemInfo;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import javax.swing.*;
import mSearch.daten.DatenFilm;
import mSearch.filmeSuchen.ListenerFilmeLaden;
import mSearch.filmeSuchen.ListenerFilmeLadenEvent;
import mSearch.tool.*;
import mediathek.MediathekGui;
import mediathek.config.Icons;
import mediathek.controller.MVUsedUrl;
import mediathek.controller.starter.Start;
import mediathek.config.Daten;
import mediathek.daten.DatenAbo;
import mediathek.daten.DatenDownload;
import mediathek.daten.DatenPset;
import mediathek.gui.dialog.DialogBeendenZeit;
import mediathek.gui.dialog.DialogEditAbo;
import mediathek.gui.dialog.DialogEditDownload;
import mediathek.tool.*;

public class GuiDownloads extends PanelVorlage {

//    private final MVFilmInfo filmInfoHud;
    private long lastUpdate = 0;
    private boolean showAbos = true;
    private boolean showDownloads = true;
    private static final String COMBO_DISPLAY_ALL = "alles";
    private static final String COMBO_DISPLAY_DOWNLOADS_ONLY = "nur Downloads";
    private static final String COMBO_DISPLAY_ABOS_ONLY = "nur Abos";
    private ToolBar toolBar;
    private boolean loadFilmlist = false;
    /**
     * The internally used model.
     */
    private TModelDownload model;

    public GuiDownloads(Daten d, JFrame parentComponent) {
        super(d, parentComponent);
        initComponents();

        if (SystemInfo.isWindows()) {
            // zum Abfangen der Win-F4 für comboboxen
            InputMap im = cbDisplayCategories.getInputMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "einstellungen");
            ActionMap am = cbDisplayCategories.getActionMap();
            am.put("einstellungen", new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Daten.dialogEinstellungen.setVisible(true);
                }
            });
        }

        tabelle = new MVTable(MVTable.TableType.DOWNLOADS);
        jScrollPane1.setViewportView(tabelle);

        setupDescriptionPanel();

        init();
        tabelle.initTabelle();
        tabelle.setSpalten();
        if (tabelle.getRowCount() > 0) {
            tabelle.setRowSelectionInterval(0, 0);
        }
        addListenerMediathekView();
        cbDisplayCategories.setModel(getDisplaySelectionModel());
        cbDisplayCategories.addActionListener(new DisplayCategoryListener());

        toolBar = new ToolBar(daten, MediathekGui.TABS.TAB_DOWNLOADS);
        jPanelToolBar.setLayout(new BorderLayout());
        jPanelToolBar.add(toolBar, BorderLayout.CENTER);
        setToolbarVisible();
//        SwingUtilities.invokeLater(this::downloadsAktualisieren);//////
    }

    private void setupDescriptionPanel() {
        PanelFilmBeschreibung panelBeschreibung = new PanelFilmBeschreibung(daten, tabelle, false/*film*/);
        jPanelBeschreibung.add(panelBeschreibung, BorderLayout.CENTER);
    }

    @Override
    public void isShown() {
        super.isShown();
        if (!solo) {
            Daten.mediathekGui.getStatusBar().setIndexForLeftDisplay(MVStatusBar.StatusbarIndex.DOWNLOAD);
        }
        updateFilmData();
        Listener.notify(Listener.EREIGNIS_DOWNLOAD_BESCHREIBUNG_ANZEIGEN, PanelFilmBeschreibung.class.getSimpleName());

    }

    public void aktualisieren() {
        downloadsAktualisieren();
    }

    public void filmAbspielen() {
        filmAbspielen_();
    }

    public void guiFilmMediensammlung() {
        mediensammlung();
    }

    public void starten(boolean alle) {
        filmStartenWiederholenStoppen(alle, true /* starten */);
    }

    public void startAtTime() {
        filmStartAtTime();
    }

    public void stoppen(boolean alle) {
        filmStartenWiederholenStoppen(alle, false /* starten */);
    }

    public void wartendeStoppen() {
        wartendeDownloadsStoppen();
    }

    public void vorziehen() {
        downloadsVorziehen();
    }

    public void zurueckstellen() {
        downloadLoeschen(false);
    }

    public void loeschen() {
        downloadLoeschen(true);
    }

    public void aufraeumen() {
        downloadsAufraeumen();
    }

    public void aendern() {
        downloadAendern();
    }

    public void filmGesehen() {
        daten.history.setGesehen(true, getSelFilme(), Daten.listeFilmeHistory);
    }

    public void filmUngesehen() {
        daten.history.setGesehen(false, getSelFilme(), Daten.listeFilmeHistory);
    }

    public void invertSelection() {
        tabelle.invertSelection();
    }

    //===================================
    //private
    //===================================
    private void setToolbarVisible() {
        toolBar.setVisible(Boolean.parseBoolean(MVConfig.get(MVConfig.SYSTEM_TOOLBAR_ALLES_ANZEIGEN)));
    }

    private void init() {
        //Tabelle einrichten
        ActionMap am = tabelle.getActionMap();
        InputMap im = tabelle.getInputMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "aendern");
        am.put("aendern", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                downloadAendern();
            }
        });

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "tabelle");
        this.getActionMap().put("tabelle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tabelle.requestFocusSelelct(jScrollPane1);
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "download");
        this.getActionMap().put("download", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filmStartenWiederholenStoppen(false, true /* starten */);
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "info");
        this.getActionMap().put("info", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!Daten.filmInfo.isVisible()) {
                    Daten.filmInfo.showInfo();
                }
            }
        });

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "url-copy");
        this.getActionMap().put("url-copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tabelle.getSelectedRow();
                if (row >= 0) {
                    GuiFunktionen.copyToClipboard(tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row),
                            DatenDownload.DOWNLOAD_URL).toString());
                }
            }
        });

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "mediensammlung");
        this.getActionMap().put("mediensammlung", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tabelle.getSelectedRow();
                if (row >= 0) {
                    MVConfig.add(MVConfig.SYSTEM_MEDIA_DB_DIALOG_ANZEIGEN, Boolean.TRUE.toString());
                    Daten.dialogMediaDB.setVis();

                    DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF);
                    if (datenDownload != null) {
                        Daten.dialogMediaDB.setFilter(datenDownload.arr[DatenDownload.DOWNLOAD_TITEL]);
                    }

                }
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "gesehen");
        this.getActionMap().put("gesehen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filmGesehen();
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "ungesehen");
        this.getActionMap().put("ungesehen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filmUngesehen();
            }
        });
        panelBeschreibungSetzen();

        final CellRendererDownloads cellRenderer = new CellRendererDownloads();
        tabelle.setDefaultRenderer(Object.class, cellRenderer);
        tabelle.setDefaultRenderer(Datum.class, cellRenderer);
        tabelle.setDefaultRenderer(MVFilmSize.class, cellRenderer);
        tabelle.setDefaultRenderer(Integer.class, cellRenderer);

        model = new TModelDownload(new Object[][]{}, DatenDownload.COLUMN_NAMES);
        tabelle.setModel(model);
        tabelle.addMouseListener(new BeobMausTabelle());
        tabelle.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateFilmData();
            }
        });

        tabelle.getTableHeader().addMouseListener(new BeobTableHeader(tabelle, DatenDownload.COLUMN_NAMES, DatenDownload.spaltenAnzeigen,
                new int[]{DatenDownload.DOWNLOAD_BUTTON_START, DatenDownload.DOWNLOAD_BUTTON_DEL, DatenDownload.DOWNLOAD_REF},
                new int[]{DatenDownload.DOWNLOAD_BUTTON_START, DatenDownload.DOWNLOAD_BUTTON_DEL},
                true /*Icon*/));

        Daten.filmeLaden.addAdListener(new ListenerFilmeLaden() {
            @Override
            public void start(ListenerFilmeLadenEvent event) {
                loadFilmlist = true;
            }

            @Override
            public void fertig(ListenerFilmeLadenEvent event) {
                loadFilmlist = false;
                Daten.listeDownloads.filmEintragen();
                if (Boolean.parseBoolean(MVConfig.get(MVConfig.SYSTEM_ABOS_SOFORT_SUCHEN))) {
                    downloadsAktualisieren();
                } else {
                    reloadTable(); // damit die Filmnummern richtig angezeigt werden
                    // ToDo beim Neuladen ändert sich die Filmnummer aber der Link datenDonwnload.film existiert ja
                    // noch, funktioniert auch damit, stimmt nur die FilmNr nicht
                }
            }
        });
    }

    private void addListenerMediathekView() {
        Listener.addListener(new Listener(Listener.EREIGNIS_TOOLBAR_VIS, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                setToolbarVisible();
            }
        });
        Listener.addListener(new Listener(Listener.EREIGNIS_BLACKLIST_GEAENDERT, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                if (Boolean.parseBoolean(MVConfig.get(MVConfig.SYSTEM_ABOS_SOFORT_SUCHEN))
                        && Boolean.parseBoolean(MVConfig.get(MVConfig.SYSTEM_BLACKLIST_AUCH_ABO))) {
                    // nur auf Blacklist reagieren, wenn auch für Abos eingeschaltet
                    downloadsAktualisieren();
                }
            }
        });
        Listener.addListener(new Listener(new int[]{Listener.EREIGNIS_BLACKLIST_AUCH_FUER_ABOS,
            Listener.EREIGNIS_LISTE_ABOS}, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                if (Boolean.parseBoolean(MVConfig.get(MVConfig.SYSTEM_ABOS_SOFORT_SUCHEN))) {
                    downloadsAktualisieren();
                }
            }
        });
        Listener.addListener(new Listener(new int[]{Listener.EREIGNIS_LISTE_DOWNLOADS,
            Listener.EREIGNIS_REIHENFOLGE_DOWNLOAD, Listener.EREIGNIS_RESET_INTERRUPT}, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                reloadTable();
                daten.allesSpeichern(); // damit nichts verloren geht
            }
        });
        Listener.addListener(new Listener(new int[]{Listener.EREIGNIS_START_EVENT}, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                Daten.listeDownloads.setModelProgressAlleStart(model);
                tabelle.fireTableDataChanged(true /*setSpalten*/);
                setInfo();
            }
        });
        Listener.addListener(new Listener(Listener.EREIGNIS_GEO, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                tabelle.fireTableDataChanged(true /*setSpalten*/);
                setInfo();
            }
        });
        Listener.addListener(new Listener(new int[]{Listener.EREIGNIS_ART_DOWNLOAD_PROZENT}, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                if (lastUpdate < (new Date().getTime() - 500)) {
                    // nur alle 500ms aufrufen
                    lastUpdate = new Date().getTime();
                    Daten.listeDownloads.setModelProgress(model);
                    // ist ein Kompromiss: beim Sortieren nach Progress wird die Tabelle nicht neu sortiert!
                    //tabelle.fireTableDataChanged(true /*setSpalten*/);
                }
            }
        });
        Listener.addListener(new Listener(Listener.EREIGNIS_DOWNLOAD_BESCHREIBUNG_ANZEIGEN, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                panelBeschreibungSetzen();
            }
        });
    }

    private void panelBeschreibungSetzen() {
        jPanelBeschreibung.setVisible(Boolean.parseBoolean(MVConfig.get(MVConfig.SYSTEM_DOWNOAD_BESCHREIBUNG_ANZEIGEN)));
    }

    private synchronized void reloadTable() {
        // nur Downloads die schon in der Liste sind werden geladen
        stopBeob = true;
        tabelle.getSpalten();

        Daten.listeDownloads.getModel(model, showAbos, showDownloads);
        tabelle.setSpalten();
        stopBeob = false;
        updateFilmData();
        setInfo();
    }

    private void mediensammlung() {
        DatenDownload datenDownload = getSelDownload();
        MVConfig.add(MVConfig.SYSTEM_MEDIA_DB_DIALOG_ANZEIGEN, Boolean.TRUE.toString());
        Daten.dialogMediaDB.setVis();

        if (datenDownload != null) {
            Daten.dialogMediaDB.setFilter(datenDownload.arr[DatenDownload.DOWNLOAD_TITEL]);
        }
    }

    private synchronized void downloadsAktualisieren() {
        if (loadFilmlist) {
            // wird danach automatisch gemacht
            return;
        }
        // erledigte entfernen, nicht gestartete Abos entfernen und neu nach Abos suchen
        Daten.listeDownloads.abosAuffrischen();
        Daten.listeDownloads.abosSuchen(parentComponent);
        reloadTable();
        if (Boolean.parseBoolean(MVConfig.get(MVConfig.SYSTEM_DOWNLOAD_SOFORT_STARTEN))) {
            // und wenn gewollt auch gleich starten
            filmStartenWiederholenStoppen(true /*alle*/, true /*starten*/, false /*fertige wieder starten*/);
        }
    }

    private synchronized void downloadsAufraeumen() {
        // abgeschlossene Downloads werden aus der Tabelle/Liste entfernt
        // die Starts dafür werden auch gelöscht
        Daten.listeDownloads.listePutzen();
    }

    private synchronized void downloadsAufraeumen(DatenDownload datenDownload) {
        // abgeschlossene Downloads werden aus der Tabelle/Liste entfernt
        // die Starts dafür werden auch gelöscht
        Daten.listeDownloads.listePutzen(datenDownload);
    }

    private ArrayList<DatenDownload> getSelDownloads() {
        ArrayList<DatenDownload> arrayDownloads = new ArrayList<>();
        int rows[] = tabelle.getSelectedRows();
        if (rows.length > 0) {
            for (int row : rows) {
                DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF);
                arrayDownloads.add(datenDownload);
            }
        } else {
            new HinweisKeineAuswahl().zeigen(parentComponent);
        }
        return arrayDownloads;
    }

    private DatenDownload getSelDownload() {
        DatenDownload datenDownload = null;
        int row = tabelle.getSelectedRow();
        if (row >= 0) {
            datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF);
        } else {
            new HinweisKeineAuswahl().zeigen(parentComponent);
        }
        return datenDownload;
    }

    private synchronized void downloadAendern() {
        DatenDownload datenDownload = getSelDownload();
        if (datenDownload == null) {
            return;
        }
        boolean gestartet = false;
        if (datenDownload.start != null) {
            if (datenDownload.start.status >= Start.STATUS_RUN) {
                gestartet = true;
            }
        }
        DatenDownload datenDownloadKopy = datenDownload.getCopy();
        DialogEditDownload dialog = new DialogEditDownload(parentComponent, true, datenDownloadKopy, gestartet);
        dialog.setVisible(true);
        if (dialog.ok) {
            datenDownload.aufMichKopieren(datenDownloadKopy);
            reloadTable();
        }
    }

    private void downloadsVorziehen() {
        ArrayList<DatenDownload> arrayDownloads = getSelDownloads();
        if (arrayDownloads.isEmpty()) {
            return;
        }
        Daten.listeDownloads.downloadsVorziehen(arrayDownloads);
    }

    private void zielordnerOeffnen() {
        DatenDownload datenDownload = getSelDownload();
        if (datenDownload == null) {
            return;
        }
        String s = datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD];
        DirOpenAction.zielordnerOeffnen(parentComponent, s);
    }

    private void filmAbspielen_() {
        DatenDownload datenDownload = getSelDownload();
        if (datenDownload == null) {
            return;
        }
        String s = datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME];
        OpenPlayerAction.filmAbspielen(parentComponent, s);
    }

    private void filmLoeschen_() {
        DatenDownload datenDownload = getSelDownload();
        if (datenDownload == null) {
            return;
        }
        // Download nur löschen wenn er nicht läuft
        if (datenDownload.start != null) {
            if (datenDownload.start.status < Start.STATUS_FERTIG) {
                MVMessageDialog.showMessageDialog(parentComponent, "Download erst stoppen!", "Film löschen", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        try {
            File file = new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME]);
            if (!file.exists()) {
                MVMessageDialog.showMessageDialog(parentComponent, "Die Datei existiert nicht!", "Film löschen", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int ret = JOptionPane.showConfirmDialog(parentComponent,
                    datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME], "Film Löschen?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.OK_OPTION) {

                // und jetzt die Datei löschen
                SysMsg.sysMsg(new String[]{"Datei löschen: ", file.getAbsolutePath()});
                if (!file.delete()) {
                    throw new Exception();
                }
            }
        } catch (Exception ex) {
            MVMessageDialog.showMessageDialog(parentComponent, "Konnte die Datei nicht löschen!", "Film löschen", JOptionPane.ERROR_MESSAGE);
            Log.errorLog(915236547, "Fehler beim löschen: " + datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME]);
        }
    }

    private void downloadLoeschen(boolean dauerhaft) {
        try {
            ArrayList<DatenDownload> arrayDownloads = getSelDownloads();
            if (arrayDownloads.isEmpty()) {
                return;
            }
            int[] rows = tabelle.getSelectedRows();
            String zeit = new SimpleDateFormat("dd.MM.yyyy").format(new Date());

            ArrayList<DatenDownload> arrayDownloadsLoeschen = new ArrayList<>();
            LinkedList<MVUsedUrl> urlAboList = new LinkedList<>();

            for (DatenDownload datenDownload : arrayDownloads) {
                if (dauerhaft) {
                    arrayDownloadsLoeschen.add(datenDownload);
                    if (datenDownload.istAbo()) {
                        // ein Abo wird zusätzlich ins Logfile geschrieben
                        urlAboList.add(new MVUsedUrl(zeit,
                                datenDownload.arr[DatenDownload.DOWNLOAD_THEMA],
                                datenDownload.arr[DatenDownload.DOWNLOAD_TITEL],
                                datenDownload.arr[DatenDownload.DOWNLOAD_HISTORY_URL]));
                    }
                } else {
                    // wenn nicht dauerhaft
                    datenDownload.zurueckstellen();
                }
            }
            if (!urlAboList.isEmpty()) {
                daten.erledigteAbos.zeilenSchreiben(urlAboList);
            }
            Daten.listeDownloads.downloadLoeschen(arrayDownloadsLoeschen);
            reloadTable();
//            // ausrichten
//            tabelle.setSelRow(rows[0]);
        } catch (Exception ex) {
            Log.errorLog(451203625, ex);
        }
    }

    private void filmStartAtTime() {
        // bezieht sich immer auf "alle"
        // Film der noch keinen Starts hat wird gestartet
        // Film dessen Start schon auf fertig/fehler steht wird wieder gestartet
        // wird immer vom Benutzer aufgerufen
        ArrayList<DatenDownload> listeAllDownloads = new ArrayList<>();
        ArrayList<DatenDownload> listeUrlsDownloadsAbbrechen = new ArrayList<>();
        ArrayList<DatenDownload> listeDownloadsStarten = new ArrayList<>();
        // ==========================
        // erst mal die Liste nach der Tabelle sortieren
        if (tabelle.getRowCount() == 0) {
            return;
        }
        for (int i = 0; i < tabelle.getRowCount(); ++i) {
            // um in der Reihenfolge zu starten
            DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(i), DatenDownload.DOWNLOAD_REF);
            listeAllDownloads.add(datenDownload);
            Daten.listeDownloads.remove(datenDownload);
            Daten.listeDownloads.add(datenDownload);
        }
        // ========================
        // und jetzt abarbeiten
        for (DatenDownload download : listeAllDownloads) {
            // ==========================================
            // starten
            if (download.start != null) {
                if (download.start.status == Start.STATUS_RUN) {
                    // dann läuft er schon
                    continue;
                }
                if (download.start.status > Start.STATUS_RUN) {
                    // wenn er noch läuft gibts nix
                    // wenn er schon fertig ist, erst mal fragen vor dem erneuten Starten
                    //TODO in auto dialog umwandeln!
                    int a = JOptionPane.showConfirmDialog(parentComponent, "Film nochmal starten?  ==> " + download.arr[DatenDownload.DOWNLOAD_TITEL],
                            "Fertiger Download", JOptionPane.YES_NO_OPTION);
                    if (a != JOptionPane.YES_OPTION) {
                        // weiter mit der nächsten URL
                        continue;
                    }
                    listeUrlsDownloadsAbbrechen.add(download);
                    if (download.istAbo()) {
                        // wenn er schon feritg ist und ein Abos ist, Url auch aus dem Logfile löschen, der Film ist damit wieder auf "Anfang"
                        daten.erledigteAbos.urlAusLogfileLoeschen(download.arr[DatenDownload.DOWNLOAD_HISTORY_URL]);
                    }
                }
            }
            listeDownloadsStarten.add(download);
        }
        // ========================
        // jetzt noch die Starts stoppen
        Daten.listeDownloads.downloadAbbrechen(listeUrlsDownloadsAbbrechen);

        // und die Downloads starten oder stoppen
        //alle Downloads starten/wiederstarten
        DialogBeendenZeit dialogBeenden = new DialogBeendenZeit(Daten.mediathekGui, daten, listeDownloadsStarten);
        dialogBeenden.setVisible(true);
        if (dialogBeenden.applicationCanTerminate()) {
            // fertig und beenden
            Daten.mediathekGui.beenden(false /*Dialog auf "sofort beenden" einstellen*/, dialogBeenden.isShutdownRequested());
        }

        reloadTable();
    }

    private void filmStartenWiederholenStoppen(boolean alle, boolean starten /* starten/wiederstarten oder stoppen */) {
        filmStartenWiederholenStoppen(alle, starten, true /*auch fertige wieder starten*/);
    }

    private void filmStartenWiederholenStoppen(boolean alle, boolean starten /* starten/wiederstarten oder stoppen */, boolean fertige /*auch fertige wieder starten*/) {
        // bezieht sich immer auf "alle" oder nur die markierten
        // Film der noch keinen Starts hat wird gestartet
        // Film dessen Start schon auf fertig/fehler steht wird wieder gestartet
        // bei !starten wird der Film gestoppt
        // wird immer vom Benutzer aufgerufen
        ArrayList<DatenDownload> listeDownloadsLoeschen = new ArrayList<>();
        ArrayList<DatenDownload> listeDownloadsStarten = new ArrayList<>();
        ArrayList<DatenDownload> listeDownloadsMarkiert = new ArrayList<>();

        if (tabelle.getRowCount() == 0) {
            return;
        }

        // ==========================
        // erst mal die Liste nach der Tabelle sortieren
        if (starten && alle) {
            //Liste in der Reihenfolge wie in der Tabelle sortieren
            for (int i = 0; i < tabelle.getRowCount(); ++i) {
                DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(i), DatenDownload.DOWNLOAD_REF);
                Daten.listeDownloads.remove(datenDownload);
                Daten.listeDownloads.add(datenDownload);
            }
        }

        // ==========================
        // die URLs sammeln
        if (alle) {
            for (int i = 0; i < tabelle.getRowCount(); ++i) {
                DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(i), DatenDownload.DOWNLOAD_REF);
                listeDownloadsMarkiert.add(datenDownload);
            }
        } else {
            listeDownloadsMarkiert = getSelDownloads();
        }
        if (!starten) {
            // dann das Starten von neuen Downloads etwas Pausieren
            daten.starterClass.pause();
        }
        // ========================
        // und jetzt abarbeiten
        int antwort = -1;
        for (DatenDownload download : listeDownloadsMarkiert) {
            if (starten) {
                // ==========================================
                // starten
                if (download.start != null) {
                    if (download.start.status == Start.STATUS_RUN
                            || !fertige && download.start.status > Start.STATUS_RUN) {
                        // wenn er noch läuft gibts nix
                        // fertige bleiben auch unverändert
                        continue;
                    }
                    if (download.start.status > Start.STATUS_RUN) {
                        // wenn er schon fertig ist, erst mal fragen vor dem erneuten Starten
                        //TODO in auto dialog umwandeln!
                        if (antwort == -1) {
                            // nur einmal fragen
                            String text;
                            if (listeDownloadsMarkiert.size() > 1) {
                                text = "Es sind bereits fertige Filme dabei,\n"
                                        + "diese nochmal starten?";
                            } else {
                                text = "Film nochmal starten?  ==> " + download.arr[DatenDownload.DOWNLOAD_TITEL];
                            }
                            antwort = JOptionPane.showConfirmDialog(parentComponent, text,
                                    "Fertiger Download", JOptionPane.YES_NO_CANCEL_OPTION);
                        }
                        if (antwort == JOptionPane.CANCEL_OPTION) {
                            //=============================
                            //dann wars das
                            return;
                        }
                        if (antwort == JOptionPane.NO_OPTION) {
                            // weiter mit der nächsten URL
                            continue;
                        }
                        listeDownloadsLoeschen.add(download);
                        if (download.istAbo()) {
                            // wenn er schon feritg ist und ein Abos ist, Url auch aus dem Logfile löschen, der Film ist damit wieder auf "Anfang"
                            daten.erledigteAbos.urlAusLogfileLoeschen(download.arr[DatenDownload.DOWNLOAD_HISTORY_URL]);
                        }
                    }
                }
                listeDownloadsStarten.add(download);
            } else if (download.start != null) {
                // ==========================================
                // stoppen
                // wenn kein s -> dann gibts auch nichts zum stoppen oder wieder-starten
                if (download.start.status <= Start.STATUS_RUN) {
                    // löschen -> nur wenn noch läuft, sonst gibts nichts mehr zum löschen
                    listeDownloadsLoeschen.add(download);
                }
            }
        }
        // ========================
        // jetzt noch die Starts stoppen
        Daten.listeDownloads.downloadAbbrechen(listeDownloadsLoeschen);
        // und die Downloads starten oder stoppen
        if (starten) {
            //alle Downloads starten/wiederstarten
            DatenDownload.startenDownloads(daten, listeDownloadsStarten);
        }
        reloadTable();
    }

    private void wartendeDownloadsStoppen() {
        // es werden alle noch nicht gestarteten Downloads gelöscht
        ArrayList<DatenDownload> listeStopDownload = new ArrayList<>();
        for (int i = 0; i < tabelle.getRowCount(); ++i) {
            DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(i), DatenDownload.DOWNLOAD_REF);
            if (datenDownload.start != null) {
                if (datenDownload.start.status < Start.STATUS_RUN) {
                    listeStopDownload.add(datenDownload);
                }
            }
        }
        Daten.listeDownloads.downloadAbbrechen(listeStopDownload);
    }

    private void setInfo() {
        // Infopanel setzen
        Daten.mediathekGui.getStatusBar().setTextForLeftDisplay();
    }

    /**
     * Return the model used for the display categories {@link javax.swing.JComboBox}.
     *
     * @return The selection model.
     */
    private DefaultComboBoxModel<String> getDisplaySelectionModel() {
        return new DefaultComboBoxModel<>(new String[]{COMBO_DISPLAY_ALL, COMBO_DISPLAY_DOWNLOADS_ONLY, COMBO_DISPLAY_ABOS_ONLY});
    }

    private void updateFilmData() {
        if (isShowing()) {
            DatenFilm aktFilm = null;
            final int selectedTableRow = tabelle.getSelectedRow();
            if (selectedTableRow >= 0) {
                final DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(selectedTableRow), DatenDownload.DOWNLOAD_REF);
                if (datenDownload != null) {
                    aktFilm = datenDownload.film;
                }
            }
            Daten.filmInfo.updateCurrentFilm(aktFilm);
        }
    }

    private ArrayList<DatenFilm> getSelFilme() {
        ArrayList<DatenFilm> arrayFilme = new ArrayList<>();
        int rows[] = tabelle.getSelectedRows();
        if (rows.length > 0) {
            for (int row : rows) {
                DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF);
                if (datenDownload.film != null) {
                    arrayFilme.add(datenDownload.film);
                }
            }
        } else {
            new HinweisKeineAuswahl().zeigen(parentComponent);
        }
        return arrayFilme;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelToolBar = new javax.swing.JPanel();
        javax.swing.JPanel jPanelFilter = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        cbDisplayCategories = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        javax.swing.JTable jTable1 = new javax.swing.JTable();
        jPanelBeschreibung = new javax.swing.JPanel();

        javax.swing.GroupLayout jPanelToolBarLayout = new javax.swing.GroupLayout(jPanelToolBar);
        jPanelToolBar.setLayout(jPanelToolBarLayout);
        jPanelToolBarLayout.setHorizontalGroup(
            jPanelToolBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 378, Short.MAX_VALUE)
        );
        jPanelToolBarLayout.setVerticalGroup(
            jPanelToolBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 25, Short.MAX_VALUE)
        );

        jLabel1.setText("Anzeigen:");

        javax.swing.GroupLayout jPanelFilterLayout = new javax.swing.GroupLayout(jPanelFilter);
        jPanelFilter.setLayout(jPanelFilterLayout);
        jPanelFilterLayout.setHorizontalGroup(
            jPanelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFilterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(5, 5, 5)
                .addComponent(cbDisplayCategories, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelFilterLayout.setVerticalGroup(
            jPanelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFilterLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jLabel1))
            .addComponent(cbDisplayCategories, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jScrollPane1.setViewportView(jTable1);

        jPanelBeschreibung.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        jPanelBeschreibung.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanelFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanelBeschreibung, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelBeschreibung, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cbDisplayCategories;
    private javax.swing.JPanel jPanelBeschreibung;
    private javax.swing.JPanel jPanelToolBar;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    public class BeobMausTabelle extends MouseAdapter {

        private Point p;
        DatenDownload datenDownload = null;

        @Override
        public void mouseClicked(MouseEvent arg0) {
            if (arg0.getButton() == MouseEvent.BUTTON1) {
                if (arg0.getClickCount() == 1) {
                    p = arg0.getPoint();
                    int row = tabelle.rowAtPoint(p);
                    int column = tabelle.columnAtPoint(p);
                    if (row >= 0) {
                        buttonTable(row, column);
                    }
                } else if (arg0.getClickCount() > 1) {
                    downloadAendern();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent arg0) {
            p = arg0.getPoint();
            int row = tabelle.rowAtPoint(p);
            if (row >= 0) {
                datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF);
            }
            if (arg0.isPopupTrigger()) {
                showMenu(arg0);
            }
        }

        @Override
        public void mouseReleased(MouseEvent arg0) {
            p = arg0.getPoint();
            int row = tabelle.rowAtPoint(p);
            if (row >= 0) {
                datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF);
            }
            if (arg0.isPopupTrigger()) {
                showMenu(arg0);
            }
        }

        private void buttonTable(int row, int column) {
            if (row != -1) {
                datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF);
                if (tabelle.convertColumnIndexToModel(column) == DatenDownload.DOWNLOAD_BUTTON_START) {
                    // filmStartenWiederholenStoppen(boolean alle, boolean starten /* starten/wiederstarten oder stoppen */)
                    if (datenDownload.start != null && !datenDownload.isDownloadManager()) {
                        if (datenDownload.start.status == Start.STATUS_FERTIG) {
                            filmAbspielen_();
                        } else if (datenDownload.start.status == Start.STATUS_ERR) {
                            // Download starten
                            filmStartenWiederholenStoppen(false, true /*starten*/);
                        } else {
                            // Download stoppen
                            filmStartenWiederholenStoppen(false, false /*starten*/);
                        }
                    } else {
                        // Download starten
                        filmStartenWiederholenStoppen(false, true /*starten*/);
                    }
                } else if (tabelle.convertColumnIndexToModel(column) == DatenDownload.DOWNLOAD_BUTTON_DEL) {
                    if (datenDownload.start != null) {
                        if (datenDownload.start.status >= Start.STATUS_FERTIG) {
                            downloadsAufraeumen(datenDownload);
                        } else {
                            // Download dauerhaft löschen
                            downloadLoeschen(true);
                        }
                    } else {
                        // Download dauerhaft löschen
                        downloadLoeschen(true);
                    }
                }
            }
        }

        private void showMenu(MouseEvent evt) {
            p = evt.getPoint();
            int nr = tabelle.rowAtPoint(p);
            if (nr >= 0) {
                tabelle.setRowSelectionInterval(nr, nr);
            }
            JPopupMenu jPopupMenu = new JPopupMenu();

            //Film vorziehen
            int row = tabelle.getSelectedRow();
            boolean wartenOderLaufen = false;
            if (row >= 0) {
                DatenDownload download = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF);
                if (download.start != null) {
                    if (download.start.status <= Start.STATUS_RUN) {
                        wartenOderLaufen = true;
                    }
                }
            }
            // Download starten
            JMenuItem itemStarten = new JMenuItem("Download starten");
            itemStarten.setIcon(Icons.ICON_MENUE_DOWNOAD_STARTEN);
            itemStarten.setEnabled(!wartenOderLaufen);
            jPopupMenu.add(itemStarten);
            itemStarten.addActionListener(arg0 -> filmStartenWiederholenStoppen(false /* alle */, true /* starten */));

            // Download stoppen
            JMenuItem itemStoppen = new JMenuItem("Download stoppen");
            itemStoppen.setIcon(Icons.ICON_MENUE_DOWNOAD_STOP);
            itemStoppen.setEnabled(wartenOderLaufen);
            jPopupMenu.add(itemStoppen);
            itemStoppen.addActionListener(arg0 -> filmStartenWiederholenStoppen(false /* alle */, false /* starten */));

            //#######################################
            jPopupMenu.addSeparator();
            //#######################################

            JMenuItem itemVorziehen = new JMenuItem("Download vorziehen");
            itemVorziehen.setIcon(Icons.ICON_MENUE_VORZIEHEN);
            jPopupMenu.add(itemVorziehen);
            itemVorziehen.addActionListener(arg0 -> downloadsVorziehen());
            JMenuItem itemLoeschen = new JMenuItem("Download zurückstellen");
            itemLoeschen.setIcon(Icons.ICON_MENUE_DOWNLOAD_ZURUECKSTELLEN);
            jPopupMenu.add(itemLoeschen);
            itemLoeschen.addActionListener(arg0 -> downloadLoeschen(false /* dauerhaft */));
            //dauerhaft löschen
            JMenuItem itemDauerhaftLoeschen = new JMenuItem("Download aus Liste entfernen");
            itemDauerhaftLoeschen.setIcon(Icons.ICON_MENUE_DOWNOAD_LOESCHEN);
            jPopupMenu.add(itemDauerhaftLoeschen);
            itemDauerhaftLoeschen.addActionListener(arg0 -> downloadLoeschen(true /* dauerhaft */));
            //Download ändern
            JMenuItem itemAendern = new JMenuItem("Download ändern");
            itemAendern.setIcon(Icons.ICON_MENUE_DOWNLOAD_AENDERN);
            jPopupMenu.add(itemAendern);
            itemAendern.addActionListener(arg0 -> downloadAendern());

            //#######################################
            jPopupMenu.addSeparator();
            //#######################################

            JMenuItem itemAlleStarten = new JMenuItem("alle Downloads starten");
            itemAlleStarten.setIcon(Icons.ICON_MENUE_DOWNLOAD_ALLE_STARTEN);
            jPopupMenu.add(itemAlleStarten);
            itemAlleStarten.addActionListener(arg0 -> filmStartenWiederholenStoppen(true /* alle */, true /* starten */));
            JMenuItem itemAlleStoppen = new JMenuItem("alle Downloads stoppen");
            itemAlleStoppen.setIcon(Icons.ICON_MENUE_DOWNOAD_STOP);
            jPopupMenu.add(itemAlleStoppen);
            itemAlleStoppen.addActionListener(arg0 -> filmStartenWiederholenStoppen(true /* alle */, false /* starten */));
            JMenuItem itemWartendeStoppen = new JMenuItem("wartende Downloads stoppen");
            itemWartendeStoppen.setIcon(Icons.ICON_MENUE_DOWNOAD_STOP);
            jPopupMenu.add(itemWartendeStoppen);
            itemWartendeStoppen.addActionListener(arg0 -> wartendeDownloadsStoppen());
            JMenuItem itemAktualisieren = new JMenuItem("Liste der Downloads aktualisieren");
            itemAktualisieren.setIcon(Icons.ICON_MENUE_AKTUALISIEREN);
            jPopupMenu.add(itemAktualisieren);
            itemAktualisieren.addActionListener(arg0 -> downloadsAktualisieren());
            JMenuItem itemAufraeumen = new JMenuItem("Liste der Downloads aufräumen");
            itemAufraeumen.setIcon(Icons.ICON_MENUE_CLEAR);
            jPopupMenu.add(itemAufraeumen);
            itemAufraeumen.addActionListener(arg0 -> downloadsAufraeumen());

            //#######################################
            jPopupMenu.addSeparator();
            //#######################################

            // Film abspielen
            JMenuItem itemPlayerDownload = new JMenuItem("gespeicherten Film (Datei) abspielen");
            itemPlayerDownload.setIcon(Icons.ICON_MENUE_FILM_START);

            itemPlayerDownload.addActionListener(e -> filmAbspielen_());
            jPopupMenu.add(itemPlayerDownload);
            // Film löschen
            JMenuItem itemDeleteDownload = new JMenuItem("gespeicherten Film (Datei) löschen");
            itemDeleteDownload.setIcon(Icons.ICON_MENUE_DOWNOAD_LOESCHEN);

            itemDeleteDownload.addActionListener(e -> filmLoeschen_());
            jPopupMenu.add(itemDeleteDownload);
            // Zielordner öffnen
            JMenuItem itemOeffnen = new JMenuItem("Zielordner öffnen");
            itemOeffnen.setIcon(Icons.ICON_MENUE_FILE_OPEN);
            jPopupMenu.add(itemOeffnen);
            itemOeffnen.addActionListener(e -> zielordnerOeffnen());

            //#######################################
            jPopupMenu.addSeparator();
            //#######################################

            //Abo ändern
            JMenu submenueAbo = new JMenu("Abo");
            JMenuItem itemChangeAbo = new JMenuItem("Abo ändern");
            JMenuItem itemDelAbo = new JMenuItem("Abo löschen");
            if (datenDownload == null) {
                submenueAbo.setEnabled(false);
                itemChangeAbo.setEnabled(false);
                itemDelAbo.setEnabled(false);
            } else if (datenDownload.film == null) {
                submenueAbo.setEnabled(false);
                itemChangeAbo.setEnabled(false);
                itemDelAbo.setEnabled(false);
            } else {
                final DatenAbo datenAbo = Daten.listeAbo.getAboFuerFilm_schnell(datenDownload.film, false /*die Länge nicht prüfen*/);
                if (datenAbo == null) {
                    submenueAbo.setEnabled(false);
                    itemChangeAbo.setEnabled(false);
                    itemDelAbo.setEnabled(false);
                } else {
                    // dann können wir auch ändern
                    itemDelAbo.addActionListener(e -> Daten.listeAbo.aboLoeschen(datenAbo));
                    itemChangeAbo.addActionListener(e -> {
                        stopBeob = true;
                        DialogEditAbo dialog = new DialogEditAbo(Daten.mediathekGui, true, daten, datenAbo);
                        dialog.setVisible(true);
                        if (dialog.ok) {
                            Daten.listeAbo.aenderungMelden();
                        }
                        stopBeob = false;
                    });
                }
            }
            submenueAbo.add(itemDelAbo);
            submenueAbo.add(itemChangeAbo);
            jPopupMenu.add(submenueAbo);

            //#######################################
            jPopupMenu.addSeparator();
            //#######################################

            // Film in der MediaDB suchen
            JMenuItem itemDb = new JMenuItem("Titel in der Mediensammlung suchen");
            itemDb.addActionListener(e -> mediensammlung());
            jPopupMenu.add(itemDb);

            // URL abspielen
            JMenuItem itemPlayer = new JMenuItem("Film (URL) abspielen");
            itemPlayer.addActionListener(e -> {
                int nr1 = tabelle.rowAtPoint(p);
                if (nr1 >= 0) {
                    DatenPset gruppe = Daten.listePset.getPsetAbspielen();
                    if (gruppe != null) {
                        DatenDownload datenDownload1 = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(nr1), DatenDownload.DOWNLOAD_REF);
                        if (datenDownload1 != null) {
                            if (datenDownload1.film != null) {
                                DatenFilm filmDownload = datenDownload1.film.getCopy();
                                // und jetzt die tatsächlichen URLs des Downloads eintragen
                                filmDownload.arr[DatenFilm.FILM_URL] = datenDownload1.arr[DatenDownload.DOWNLOAD_URL];
                                filmDownload.arr[DatenFilm.FILM_URL_RTMP] = datenDownload1.arr[DatenDownload.DOWNLOAD_URL_RTMP];
                                filmDownload.arr[DatenFilm.FILM_URL_KLEIN] = "";
                                filmDownload.arr[DatenFilm.FILM_URL_RTMP_KLEIN] = "";
                                // und starten
                                daten.starterClass.urlMitProgrammStarten(gruppe, filmDownload, "" /*Auflösung*/);
                            }
                        }
                    } else {
                        String menuPath;
                        if (SystemInfo.isMacOSX()) {
                            menuPath = "MediathekView->Einstellungen…->Aufzeichnen und Abspielen->Set bearbeiten";
                        } else {
                            menuPath = "Datei->Einstellungen->Set bearbeiten";
                        }
                        MVMessageDialog.showMessageDialog(parentComponent, "Bitte legen Sie im Menü \"" + menuPath + "\" ein Programm zum Abspielen fest.",
                                "Kein Videoplayer!", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
            jPopupMenu.add(itemPlayer);

            // URL kopieren
            JMenuItem itemUrl = new JMenuItem("URL kopieren");
            KeyStroke ctrlU = KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            itemUrl.setAccelerator(ctrlU);
            itemUrl.addActionListener(e -> {
                int nr1 = tabelle.rowAtPoint(p);
                if (nr1 >= 0) {
                    GuiFunktionen.copyToClipboard(
                            tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(nr1),
                                    DatenDownload.DOWNLOAD_URL).toString());
                }
            });
            jPopupMenu.add(itemUrl);

            // Infos
            JMenuItem itemInfo = new JMenuItem("Filminformation anzeigen");
            itemInfo.addActionListener(e -> {
                if (!Daten.filmInfo.isVisible()) {
                    Daten.filmInfo.showInfo();
                }
            });
            jPopupMenu.add(itemInfo);

            // ######################
            // Menü anzeigen
            jPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    /**
     * This class filters the shown table items based on the made selection.
     */
    private class DisplayCategoryListener implements ActionListener {

        @Override
        @SuppressWarnings("unchecked")
        public void actionPerformed(ActionEvent e) {
            JComboBox<String> box = (JComboBox<String>) e.getSource();
            final String action = (String) box.getSelectedItem();

            switch (action) {
                case COMBO_DISPLAY_ALL:
                    showAbos = true;
                    showDownloads = true;
                    break;

                case COMBO_DISPLAY_DOWNLOADS_ONLY:
                    showAbos = false;
                    showDownloads = true;
                    break;

                case COMBO_DISPLAY_ABOS_ONLY:
                    showAbos = true;
                    showDownloads = false;
                    break;
            }

            reloadTable();
        }
    }
}
