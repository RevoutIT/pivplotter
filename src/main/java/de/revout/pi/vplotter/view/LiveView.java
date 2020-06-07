package de.revout.pi.vplotter.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import de.revout.pi.vplotter.converter.Pair;
import de.revout.pi.vplotter.lang.Dictionary;
import de.revout.pi.vplotter.main.Driver;
import de.revout.pi.vplotter.main.DriverMoveObserverIf;
import de.revout.pi.vplotter.model.Model;

public class LiveView extends JPanel implements DriverMoveObserverIf{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel leftPanel;
	private JPanel rightPanel;
	private MainView mainView;
	private boolean pause;
	private JProgressBar progressBar;
	private JButton stopButton;
	private JButton cancelButton;
	private JButton playButton;


	public LiveView(MainView paramMainview) {
		mainView=paramMainview;
		UIManager.put("ProgressBar.selectionForeground", Color.BLACK); //$NON-NLS-1$
		UIManager.put("ProgressBar.selectionBackground", Color.BLACK); //$NON-NLS-1$
		
		Driver.getCurrent().addDriverMoveObserverIf(this);
		
		pause = false;		
		initialize();
	}


	private void initialize() {
		
		GridBagLayout filterPanelBagLayout = new GridBagLayout();
		setLayout(filterPanelBagLayout);
		
		leftPanel = new JPanel();
		leftPanel.setBackground(MainView.COLOR1);
		createConstraints(filterPanelBagLayout, leftPanel, 0, 0, 5, 1);
		GridBagLayout leftBagLayout = new GridBagLayout();
		leftPanel.setLayout(leftBagLayout);

		rightPanel = new JPanel();
		rightPanel.setBackground(MainView.COLOR2);
		createConstraints(filterPanelBagLayout, rightPanel, 1, 0, 1, 1);
		GridBagLayout rightBagLayout = new GridBagLayout();
		rightPanel.setLayout(rightBagLayout);
		
		add(leftPanel);
		add(rightPanel);

		LivePlotterView livePlotterView = new LivePlotterView();
		createConstraints(leftBagLayout, livePlotterView, 0, 0, 1, 1);
		leftPanel.add(livePlotterView);
		

		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(MainView.COLOR2);
		createConstraints(rightBagLayout, bottomPanel, 0, 2, 1, 1);
		GridBagLayout bottomBagLayout = new GridBagLayout();
		bottomPanel.setLayout(bottomBagLayout);
		
		progressBar = new JProgressBar(0,100);
		progressBar.setStringPainted(true);
		progressBar.setForeground(Color.GREEN);
		progressBar.setBackground(Color.WHITE);
		
		stopButton = new JButton(Dictionary.getCurrent().getString("ControlPanel.BtnStop")); //$NON-NLS-1$
		playButton = new JButton(Dictionary.getCurrent().getString("ControlPanel.BtnPause")); //$NON-NLS-1$
		playButton.setEnabled(true);
		stopButton.setEnabled(false);

		cancelButton =  new JButton(Dictionary.getCurrent().getString("ControlPanel.BtnCancel")); //$NON-NLS-1$
		
		stopButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Driver.getCurrent().stop();
				mainView.changePage(mainView.getPage1());
				stopButton.setEnabled(false);
				playButton.setEnabled(true);
				playButton.setText(Dictionary.getCurrent().getString("ControlPanel.BtnPause")); //$NON-NLS-1$
				pause = false;
			}
		});
		
		
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Driver.getCurrent().stop();
				Driver.getCurrent().goTo(Driver.getCurrent().getStartPoint());
			}
		});
		
		playButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(!pause) {
					playButton.setText(Dictionary.getCurrent().getString("ControlPanel.BtnContinue")); //$NON-NLS-1$
					stopButton.setEnabled(true);
					Driver.getCurrent().setPause(true);
					pause = true;
				}else {
					playButton.setText(Dictionary.getCurrent().getString("ControlPanel.BtnPause")); //$NON-NLS-1$
					Driver.getCurrent().setPause(false);
					stopButton.setEnabled(false);
					pause = false;
				}

			}
		});

		if (Model.getCurrent().getImage()!=null) {
			ImagePanel imagePanel = new ImagePanel();
			createConstraints(rightBagLayout, imagePanel, 0, 0, 1, 1,GridBagConstraints.PAGE_START,GridBagConstraints.BOTH);
			rightPanel.add(imagePanel);
		}else {
			PlotterView plotterView = new PlotterView();
			createConstraints(rightBagLayout, plotterView, 0, 0, 1, 1,GridBagConstraints.PAGE_START,GridBagConstraints.BOTH);
			rightPanel.add(plotterView);
		}

		createConstraints(bottomBagLayout, playButton, 0, 0, 1, 1);
		createConstraints(bottomBagLayout, cancelButton, 0, 1, 1, 1);
		createConstraints(bottomBagLayout, stopButton, 0, 2, 1, 1);
		
		createConstraints(rightBagLayout, progressBar, 0, 1, 1, 3,GridBagConstraints.PAGE_END,GridBagConstraints.HORIZONTAL);

		bottomPanel.add(playButton);
		bottomPanel.add(cancelButton);
		bottomPanel.add(stopButton);
		
		
		rightPanel.add(bottomPanel);
		rightPanel.add(progressBar);
	}
	
	@Override
	public void init() {
		playButton.setEnabled(true);
		stopButton.setEnabled(false);
		stopButton.setText(Dictionary.getCurrent().getString("ControlPanel.BtnStop")); //$NON-NLS-1$
		progressBar.setValue(0);
	}
	
	@Override
	public void currentMove(int paramState, Pair paramToPoint, int paramStepCount, int paramActualStep) {
		double progress = ((double)paramActualStep/(double)paramStepCount)*100;
		progressBar.setValue((int)progress);
	}

	@Override
	public void finish() {
		progressBar.setValue(101);
		stopButton.setText(Dictionary.getCurrent().getString("ControlPanel.BtnFinish")); //$NON-NLS-1$
		playButton.setEnabled(false);
		stopButton.setEnabled(true);
	}


	private void createConstraints(GridBagLayout paramBagLayout, Component paramComponent, int paramx, int paramy,
		double paramWeightx, double paramWeighty,int paramAnchor,int paramFill) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.gridx = paramx;
		constraints.gridy = paramy;
		constraints.weightx = paramWeightx;
		constraints.weighty = paramWeighty;
		constraints.anchor = paramAnchor;
		constraints.fill = paramFill;
		paramBagLayout.setConstraints(paramComponent, constraints);
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
