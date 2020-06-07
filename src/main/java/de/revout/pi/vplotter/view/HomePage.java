package de.revout.pi.vplotter.view;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

public class HomePage extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HomePage() {
		GridBagLayout filterPanelBagLayout = new GridBagLayout();
		setLayout(filterPanelBagLayout);

		JPanel leftPanel = new JPanel();
		leftPanel.setBackground(MainView.COLOR1);
		createConstraints(filterPanelBagLayout, leftPanel, 0, 0, 3, 1);

		JPanel rightPanel = new JPanel();
		rightPanel.setBackground(MainView.COLOR2);
		createConstraints(filterPanelBagLayout, rightPanel, 1, 0, 1, 1);

		add(leftPanel);
		add(rightPanel);
	}

	private void createConstraints(GridBagLayout paramBagLayout, Component paramComponent, int paramx, int paramy,
			double paramWeightx, double paramWeighty) {
		GridBagConstraints constraintsRight = new GridBagConstraints();
		constraintsRight.insets = new Insets(5, 5, 5, 5);
		constraintsRight.gridx = paramx;
		constraintsRight.gridy = paramy;
		constraintsRight.weightx = paramWeightx;
		constraintsRight.weighty = paramWeighty;
		constraintsRight.fill = GridBagConstraints.BOTH;
		paramBagLayout.setConstraints(paramComponent, constraintsRight);
	}
}
