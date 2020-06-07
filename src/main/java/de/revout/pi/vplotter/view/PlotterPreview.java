package de.revout.pi.vplotter.view;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.revout.pi.vplotter.converter.VDPConverter;
import de.revout.pi.vplotter.lang.Dictionary;
import de.revout.pi.vplotter.main.Driver;
import de.revout.pi.vplotter.model.Model;
import de.revout.pi.vplotter.saves.SettingsManager;
import de.revout.pi.vplotter.saves.SettingsManager.KEY;

public class PlotterPreview extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel leftPanel;
	private JPanel rightPanel;
	private MainView mainView;


	public PlotterPreview(MainView paramMainview) {
		mainView=paramMainview;
		init();
	}


	private void init() {
		
		GridBagLayout plotterPreviewBagLayout = new GridBagLayout();
		setLayout(plotterPreviewBagLayout);
		
		leftPanel = new JPanel();
		leftPanel.setBackground(MainView.COLOR1);
		createConstraints(plotterPreviewBagLayout, leftPanel, 0, 0, 5, 1);
		GridBagLayout leftBagLayout = new GridBagLayout();
		leftPanel.setLayout(leftBagLayout);

		rightPanel = new JPanel();
		rightPanel.setBackground(MainView.COLOR2);
		createConstraints(plotterPreviewBagLayout, rightPanel, 1, 0, 1, 1);
		GridBagLayout rightBagLayout = new GridBagLayout();
		rightPanel.setLayout(rightBagLayout);
		
		add(leftPanel);
		add(rightPanel);

		PlotterView plotterView = new PlotterView();
		createConstraints(leftBagLayout, plotterView, 0, 0, 1, 1);
		leftPanel.add(plotterView);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(MainView.COLOR2);
		createConstraints(rightBagLayout, bottomPanel, 0, 2, 1, 1);
		GridBagLayout bottomBagLayout = new GridBagLayout();
		bottomPanel.setLayout(bottomBagLayout);
		
		JCheckBox simulationBox = new JCheckBox(Dictionary.getCurrent().getString("OptionPanel.Simulation")); //$NON-NLS-1$
		simulationBox.setBackground(MainView.COLOR2);
		simulationBox.setHorizontalAlignment(SwingConstants.CENTER);
		simulationBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Driver.getCurrent().setSimulation(simulationBox.isSelected());
			}
		});
		
		if("true".equals(SettingsManager.getCurrent().getValue(KEY.SIMULATION))) { //$NON-NLS-1$
			simulationBox.setSelected(true);
			Driver.getCurrent().setSimulation(true);
		}else {
			simulationBox.setSelected(false);
			Driver.getCurrent().setSimulation(false);
		}
		
		JButton cancelButton = new JButton(Dictionary.getCurrent().getString("OptionPanel.BtnCancel")); //$NON-NLS-1$
		JButton startButton = new JButton(Dictionary.getCurrent().getString("OptionPanel.BtnStart")); //$NON-NLS-1$
		
		
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				VDPConverter vdpConverter = new VDPConverter(Model.getCurrent().getSectionList());
				Driver.getCurrent().loadProperty();
				Driver.getCurrent().plotte(vdpConverter.getData(), vdpConverter.getMaxWidth(), vdpConverter.getMaxHeight());
				mainView.changePage(mainView.getPage3());
			}
		});
		
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainView.changePage(mainView.getPage1());
			}
		});

		createConstraints(bottomBagLayout, startButton, 0, 0, 1, 1);
		createConstraints(bottomBagLayout, cancelButton, 0, 1, 1, 1);
		createConstraints(rightBagLayout, simulationBox, 0, 1, 1, 4);

		bottomPanel.add(startButton);
		bottomPanel.add(cancelButton);

		rightPanel.add(bottomPanel);
		rightPanel.add(simulationBox);
	}


	private void createConstraints(GridBagLayout paramBagLayout, Component paramComponent, int paramx, int paramy,
		double paramWeightx, double paramWeighty) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.gridx = paramx;
		constraints.gridy = paramy;
		constraints.weightx = paramWeightx;
		constraints.weighty = paramWeighty;
		constraints.fill = GridBagConstraints.BOTH;
		paramBagLayout.setConstraints(paramComponent, constraints);
	}

}
