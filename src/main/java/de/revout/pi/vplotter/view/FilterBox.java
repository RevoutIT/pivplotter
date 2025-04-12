package de.revout.pi.vplotter.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import de.revout.pi.vplotter.imagefilter.Filter;
import de.revout.pi.vplotter.model.Model;
import de.revout.pi.vplotter.setting.ComboSetting;
import de.revout.pi.vplotter.setting.Setting;
import de.revout.pi.vplotter.setting.SliderSetting;

public class FilterBox extends JPanel {

    private static final long serialVersionUID = 1L;
    private final Filter filter;

    public FilterBox(Filter filter, FilterPanel filterPanel) {
        this.filter = filter;
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // Remove-Button erstellen
        ImageIcon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(FilterBox.class.getResourceAsStream("close2.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JButton removeButton = new JButton(icon);
        removeButton.setBackground(Color.WHITE);
        removeButton.setBorderPainted(false);
        if (icon != null) {
            removeButton.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
        }
        addComponent(removeButton, 11, 0, 1, 1, 1, 1);

        // Filtername anzeigen
        JLabel nameLabel = new JLabel(filter.getName());
        nameLabel.setFont(new Font("Verdana", Font.BOLD, 12));
        addComponent(nameLabel, 0, 0, 1, 1, 10, 3);

        int row = 1;
        for (Setting setting : filter.getSettings()) {
            // Beschriftung für die Einstellung
            JLabel headerLabel = new JLabel(setting.getName() + "     ");
            headerLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
            addComponent(headerLabel, 0, row, 1, 1, 5, 1);

            if (setting instanceof ComboSetting) {
                ComboSetting comboSetting = (ComboSetting) setting;
                JComboBox<String> comboBox = new JComboBox<>();
                for (String value : comboSetting.getValueList()) {
                    comboBox.addItem(value);
                }
                comboBox.setSelectedItem(comboSetting.getValue());
                comboBox.addActionListener(e -> 
                    Model.getCurrent().editFilterSetting(filter, setting, (String) comboBox.getSelectedItem())
                );
                addComponent(comboBox, 1, row, 1, 1, 5, 1);
            } else if (setting instanceof SliderSetting) {
                SliderSetting sliderSetting = (SliderSetting) setting;
                int sliderValue = Integer.parseInt(sliderSetting.getValue());
                JSlider slider = new JSlider(sliderSetting.getMin(), sliderSetting.getMax(), sliderValue);
                int tick = sliderSetting.getTick();
                slider.setMinorTickSpacing(tick);
                slider.setSnapToTicks(true);
                slider.setLabelTable(slider.createStandardLabels(tick));
                slider.addChangeListener(e -> 
                    Model.getCurrent().editFilterSetting(filter, setting, String.valueOf(slider.getValue()))
                );
                slider.setBackground(Color.WHITE);
                addComponent(slider, 1, row, 1, 1, 5, 1);
            }
            row++;
        }

        removeButton.addActionListener(e -> {
            Model.getCurrent().setSameFile(false);
            filterPanel.removeFilter(this);
            filterPanel.validate();
        });

        setVisible(true);
    }

    /**
     * Fügt eine Komponente in ein GridBagLayout-basiertes Panel mit den angegebenen Parametern hinzu.
     *
     * @param comp     Die hinzuzufügende Komponente
     * @param gridx    Spaltenposition
     * @param gridy    Zeilenposition
     * @param gridwidth  Anzahl der Spalten, die die Komponente einnehmen soll
     * @param gridheight Anzahl der Zeilen, die die Komponente einnehmen soll
     * @param weightx  Gewichtung in X-Richtung
     * @param weighty  Gewichtung in Y-Richtung
     */
    private void addComponent(Component comp, int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        add(comp, gbc);
    }

    public Filter getFilter() {
        return filter;
    }
}
