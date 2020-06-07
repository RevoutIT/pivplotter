package de.revout.pi.vplotter.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.revout.pi.vplotter.imagefilter.Filter;
import de.revout.pi.vplotter.model.Model;
import de.revout.pi.vplotter.setting.ComboSetting;
import de.revout.pi.vplotter.setting.Setting;
import de.revout.pi.vplotter.setting.SliderSetting;

public class FilterBox extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	private Filter filter;
	private FilterBox filterBox=this;

	public FilterBox(Filter paramFilter, FilterPanel paramFilterPanel) {
		
		/// basic settings for the box
		filter = paramFilter;
		GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout(gridBagLayout);
		Border border = BorderFactory.createLineBorder(Color.black);
		setBackground(Color.white);
		setBorder(border);

		// Remove button creation
		ImageIcon icon = null;
		try {
			icon = new ImageIcon(ImageIO.read(FilterBox.class.getResourceAsStream("close2.png")));
		} catch (IOException e1) {
			
		}
		JButton remove = new JButton(icon);
		remove.setBackground(Color.white);
		remove.setBorderPainted(false);
		remove.setPreferredSize(new Dimension(icon.getIconWidth(),icon.getIconHeight()));
		
		addComponent(remove, gridBagLayout, 11, 0, 1, 1, 1, 1);

		// Name of the filter
		JLabel filtername = new JLabel(paramFilter.getName());
		filtername.setFont(new Font("Verdana", 1, 12)); //$NON-NLS-1$
		addComponent(filtername, gridBagLayout, 0, 0, 1, 1, 10, 3);
		

		int counter = 1;
		for (Setting setting : filter.getSettings()) {
			/// Setting name
			JLabel header = new JLabel(setting.getName() + "     "); //$NON-NLS-1$
			header.setFont(new Font("Verdana", 0, 12)); //$NON-NLS-1$
			addComponent(header, gridBagLayout, 0, counter, 1, 1, 5, 1);
		
			if (setting instanceof ComboSetting) {
				ComboSetting comboSetting = (ComboSetting) setting;
				JComboBox<String> combo = new JComboBox<>();
				for (String value : comboSetting.getValueList()) {
					combo.addItem(value);
				}
				combo.setSelectedItem(comboSetting.getValue());
				combo.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						Model.getCurrent().editFilterSetting(filter,setting,(String) combo.getSelectedItem());
					}
				});
				
				addComponent(combo, gridBagLayout, 1, counter, 1, 1, 5, 1);

			} else if (setting instanceof SliderSetting) {
				SliderSetting sliderSetting = (SliderSetting) setting;
				JSlider slider = new JSlider(sliderSetting.getMin(), sliderSetting.getMax(), Integer.parseInt(sliderSetting.getValue()));
				int tick = sliderSetting.getTick();
				slider.setMinorTickSpacing(tick);
				slider.setSnapToTicks(true);
				slider.setLabelTable(slider.createStandardLabels(tick));
				slider.addChangeListener(new ChangeListener() {
 
					@Override
					public void stateChanged(ChangeEvent e) {
						Model.getCurrent().editFilterSetting(filter,setting,"" + slider.getValue());
					}
				});
				slider.setBackground(Color.white);
				addComponent(slider, gridBagLayout, 1, counter, 1, 1, 5, 1);
			}
			counter++;
		}
		remove.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Model.getCurrent().setSameFile(false);
				paramFilterPanel.removeFilter(filterBox);
				paramFilterPanel.validate();
			}
		});
		
		setVisible(true);
	}
	
	private void addComponent(Component c,GridBagLayout paramLayout, int x, int y, int width, int height, double weightx, double weighty) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.gridheight = height;
		gbc.weightx = weightx;
		gbc.weighty = weighty;
		paramLayout.setConstraints(c, gbc);
		add(c);
	}

	public Filter getFilter() {
		return filter;
	}

}
