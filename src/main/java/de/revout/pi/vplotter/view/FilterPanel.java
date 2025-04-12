package de.revout.pi.vplotter.view;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import de.revout.pi.vplotter.imagefilter.Filter;
import de.revout.pi.vplotter.lang.Dictionary;
import de.revout.pi.vplotter.model.Model;
import de.revout.pi.vplotter.model.Update;
import de.revout.pi.vplotter.saves.SettingsManager;
import de.revout.pi.vplotter.saves.SettingsManager.KEY;

public class FilterPanel extends JPanel implements Update {

	private JPanel leftPanel;
	private JPanel rightPanel;
	private FilterPanel filterPanel = this;
	private GridBagLayout viewPortLayout;
	private int i=0;
	private JPanel jPanel;
	private MainView mainView;
	private ImagePanel imagePanel;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FilterPanel(MainView paramMainView) {
		Model.getCurrent().register(this);
		mainView=paramMainView;
		init();
	}

	private void init() {
		
		GridBagLayout filterPanelBagLayout = new GridBagLayout();
		setLayout(filterPanelBagLayout);
		
		leftPanel = new JPanel();
		leftPanel.setBackground(MainView.COLOR1);
		createConstraints(filterPanelBagLayout, leftPanel, 0, 0, 5, 1);
		imagePanel = new ImagePanel();
		GridBagLayout leftBagLayout = new GridBagLayout();
		leftPanel.setLayout(leftBagLayout);
		createConstraints(leftBagLayout, imagePanel, 0, 0, 1, 1);
		leftPanel.add(imagePanel);

		rightPanel = new JPanel();
		rightPanel.setBackground(MainView.COLOR2);
		createConstraints(filterPanelBagLayout, rightPanel, 1, 0, 1, 1);

		add(leftPanel);
		add(rightPanel);

		GridBagLayout rightBagLayout = new GridBagLayout();
		rightPanel.setLayout(rightBagLayout);

		JPanel viewPort = new JPanel();
		viewPortLayout = new GridBagLayout();
		viewPort.setLayout(viewPortLayout);
		jPanel = new JPanel();
		GridBagLayout filterBoxGridBagLayout = new GridBagLayout();
		jPanel.setLayout(filterBoxGridBagLayout);
		JPanel jPanel2 = new JPanel();
		createConstraintsFilterBox(viewPortLayout, jPanel, 0, 0, 1, 1);
		createConstraints(viewPortLayout, jPanel2, 0, 1, 1, 100);
		viewPort.add(jPanel);
		viewPort.add(jPanel2);
		
		JScrollPane jScrollPane = new JScrollPane(viewPort);
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jScrollPane.setPreferredSize(new Dimension(50,50));
		JComboBox<String> jComboBox = new JComboBox<String>();

		jComboBox.addItem("");
		for (Filter filter : Model.getCurrent().getFilters()) {
			jComboBox.addItem(filter.getName());
		}

		jComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Filter filter = null;
				if (jComboBox.getSelectedIndex() > 0) {
					filter = Model.getCurrent().getFilters().get(jComboBox.getSelectedIndex() - 1).createNewFilter();
					FilterBox filterBox = new FilterBox(filter, filterPanel);
					createConstraintsFilterBox(filterBoxGridBagLayout, filterBox, 0, i, 1, 1);
					jPanel.add(filterBox);
					Model.getCurrent().addFilter(filter);
					i++;
					Model.getCurrent().setSameFile(false);
				}
			}
		});

		createConstraints(rightBagLayout, jComboBox, 0, 0, 1, 0.1);
		createConstraints(rightBagLayout, jScrollPane, 0, 1, 1, 3);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(MainView.COLOR2);
		createConstraints(rightBagLayout, bottomPanel, 0, 2, 1, 1);

		GridBagLayout bottomBagLayout = new GridBagLayout();
		
		bottomPanel.setLayout(bottomBagLayout);

		JButton continueButton = new JButton(Dictionary.getCurrent().getString("FilterPanel.BtnContinue"));//$NON-NLS-1$
		continueButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// svg file erstellen
				next();
			}
		});
		JButton cleanButton = new JButton(Dictionary.getCurrent().getString("FilterPanel.BtnClear"));//$NON-NLS-1$
		cleanButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (delete()) {
					Model.getCurrent().removeAllFilter();
					jPanel.removeAll();
					Model.getCurrent().setSameFile(false);
				}
			}
		});

		createConstraints(bottomBagLayout, cleanButton, 0, 0, 1, 1);
		createConstraints(bottomBagLayout, continueButton, 0, 1, 1, 1);

		bottomPanel.add(cleanButton);
		bottomPanel.add(continueButton);

		rightPanel.add(jComboBox);
		rightPanel.add(jScrollPane);
		rightPanel.add(bottomPanel);
	}

	private boolean delete() {
		String[] optinons = { Dictionary.getCurrent().getString("FilterPanel.OptionYes"), //$NON-NLS-1$
				Dictionary.getCurrent().getString("FilterPanel.OptionNo") }; //$NON-NLS-1$
		int delete = JOptionPane.showOptionDialog(filterPanel.getParent(),
				Dictionary.getCurrent().getString("FilterPanel.OptionMessage"), //$NON-NLS-1$
				Dictionary.getCurrent().getString("FilterPanel.OptionName"), JOptionPane.DEFAULT_OPTION, //$NON-NLS-1$
				JOptionPane.WARNING_MESSAGE, null, optinons, optinons[0]);
		if (delete == 0) {
			return true;
		}
		return false;
	}

	public void removeFilter(FilterBox paramFilterBox) {
		Model.getCurrent().removeFilter(paramFilterBox.getFilter());
		jPanel.remove(paramFilterBox);
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

	private void createConstraintsFilterBox(GridBagLayout paramBagLayout, Component paramComponent, int paramx, int paramy,	double paramWeightx, double paramWeighty) {
			GridBagConstraints constraintsRight = new GridBagConstraints();
			constraintsRight.insets = new Insets(5, 5, 5, 5);
			constraintsRight.gridx = paramx;
			constraintsRight.gridy = paramy;
			constraintsRight.weightx = paramWeightx;
			constraintsRight.weighty = paramWeighty;
			constraintsRight.fill = GridBagConstraints.HORIZONTAL;
			paramBagLayout.setConstraints(paramComponent, constraintsRight);
	}
	
	public void next() {
		try {
		if (Model.getCurrent().checkIfGrey()) {
			String path = Model.getCurrent().saveBMPTemp();
			mainView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if (path != null) {
				try {
					String potracePath = SettingsManager.getCurrent().getValue(KEY.POTRACE);
					if(potracePath==null || !Files.exists(Paths.get(potracePath))){
						JOptionPane.showMessageDialog(mainView, Dictionary.getCurrent().getString("Potrace.Search")); //$NON-NLS-1$
						potracePath = selectPathForPotrace();
						if(potracePath!=null && Files.exists(Paths.get(potracePath))){
							SettingsManager.getCurrent().setValue(KEY.POTRACE,potracePath);
							SettingsManager.getCurrent().save();
						}
					}
					if(potracePath!=null && Files.exists(Paths.get(potracePath))){
						
						if (Model.getCurrent().generateSVGFromBMP(potracePath,path)) {
							mainView.previewImage();
							mainView.changePage(mainView.getPreviewPanel());
						}else {
							JOptionPane.showConfirmDialog(mainView, Dictionary.getCurrent().getString("SVGConvert.ERROR"),
									Dictionary.getCurrent().getString("ERROR"), JOptionPane.CLOSED_OPTION, //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						}
					}else {
						JOptionPane.showMessageDialog(mainView, Dictionary.getCurrent().getString("Potrace.NotFound")); //$NON-NLS-1$
					}
				} finally {
					mainView.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		} else {
			JOptionPane.showMessageDialog(mainView, Dictionary.getCurrent().getString("Error.Image.Nogray")); //$NON-NLS-1$
		}
		}catch(Exception exc) {
			JOptionPane.showMessageDialog(mainView, exc.getMessage()); //$NON-NLS-1$
		}
	}
	
	private String selectPathForPotrace() {
		JFileChooser c = new JFileChooser();
		c.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "*"; //$NON-NLS-1$
			}
			@Override
			public boolean accept(File f) {
				return !f.isDirectory();
			}
		});
		c.setCurrentDirectory(new File(".")); //$NON-NLS-1$
		c.showOpenDialog(mainView);
		File file = c.getSelectedFile();
		if(file!=null) {
			return file.getAbsolutePath();
		}
		return null;
	}


	@Override
	public void update() {
		filterPanel.validate();
		filterPanel.repaint();
	}

	public ImagePanel getImagePanel() {
		return imagePanel;
	}

}
