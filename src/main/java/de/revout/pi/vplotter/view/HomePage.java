package de.revout.pi.vplotter.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

public class HomePage extends JPanel {

    private static final long serialVersionUID = 1L;

    public HomePage() {
        // Setzt den GridBagLayout als Layoutmanager der HomePage
        setLayout(new GridBagLayout());
        
        // Linkes Panel
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(MainView.COLOR1);
        add(leftPanel, createGbc(0, 0, 3, 1));
        
        // Rechtes Panel
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(MainView.COLOR2);
        add(rightPanel, createGbc(1, 0, 1, 1));
    }
    
    /**
     * Erstellt und konfiguriert GridBagConstraints.
     * 
     * @param gridx   Die Spaltenposition im GridBagLayout.
     * @param gridy   Die Zeilenposition im GridBagLayout.
     * @param weightx Das Gewicht zur Verteilung extra horizontalen Raumes.
     * @param weighty Das Gewicht zur Verteilung extra vertikalen Raumes.
     * @return Konfiguriertes GridBagConstraints-Objekt.
     */
    private GridBagConstraints createGbc(int gridx, int gridy, double weightx, double weighty) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }
}
