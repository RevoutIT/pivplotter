package de.revout.pi.vplotter.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import de.revout.pi.vplotter.converter.Section;
import de.revout.pi.vplotter.lang.Dictionary;
import de.revout.pi.vplotter.main.Driver;
import de.revout.pi.vplotter.model.Model;
import de.revout.pi.vplotter.saves.SettingsManager;
import de.revout.pi.vplotter.saves.VPlotterPropertiesManager;
import de.revout.pi.vplotter.version.Version;


public class MainView extends JFrame {

	private MainView mainView = this;
	private final String PROPERTYSUFFIX = ".plotterconf"; //$NON-NLS-1$
	public final static Color COLOR1 = new Color(162, 182, 210);
	public final static Color COLOR2 = new Color(137, 164, 201);
	public final static Color COLOR3 = new Color(171,198,222);
	private StackLayout stackLayout;
	private JPanel page0;
	private JPanel page1;
	private JPanel page2;
	private JPanel page3;
	private JMenuItem loadFile;
	private JMenuItem newProcess;
	private JMenuItem goToStart;
	private JMenu plotter;
	private JMenu config;
	private JMenu recentFiles;
	private JMenu file;
	private List<String> lastFiles;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MainView() {
		
		init();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				Driver.getCurrent().gpioStop();
			}
		});
	}



	private void init() {
		lastFiles = new ArrayList<>();
		Driver.getCurrent().loadProperty();
		setTitle(Version.number+" "+ Dictionary.getCurrent().getString("MainView.TITLE") + SettingsManager.getCurrent().getValue( SettingsManager.KEY.CONFIG)); //$NON-NLS-1$
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		createMenu();
		stackLayout = new StackLayout();
		setLayout(stackLayout);
		createPages();
		setVisible(true);
	}
	
	private void createPages() {

		//Lehrseite
		page0 = new HomePage();
		page0.setBackground(new Color(162,182,210));
		
		//Filter wählen
		page1 = new FilterPanel(this);
		page1.setBackground(new Color(162,182,210));
		
		//Vorschau
		page2 = new PlotterPreview(this);
		page2.setBackground(new Color(162,182,210));
		
		//Live View
		page3 = new LiveView(this);
		page3.setBackground(new Color(162,182,210));
		
		
		stackLayout.addLayoutComponent("0", page1);
		stackLayout.addLayoutComponent("1", page1);

		changePage(page0);
	}

	public void newProcess() {
		Model.getCurrent().newProcess();
		createPages();
		validate();
		repaint();
	}

	public void changePage(JPanel paramPage) {
		stackLayout.showComponent(paramPage, this);
		if (Driver.getCurrent().isUsed()) {
			goToStart.setEnabled(true);
		}
		recentFiles.setEnabled(true);
		loadFile.setEnabled(true);
		newProcess.setEnabled(true);
		config.setEnabled(true);
		plotter.setEnabled(true);
		
		if (paramPage==page0) {
			newProcess.setEnabled(false);
		}else {
			if (paramPage==page1) {
				if (Model.getCurrent().getImage()==null) {
					if (Model.getCurrent().getSvgImage()!=null) {
						changePage(page2);
					}else {
						changePage(page0);
					}
				}
			}
			recentFiles.setEnabled(false);
			loadFile.setEnabled(false);
			if (paramPage==page2) {
				plotter.setEnabled(false);
			}
			if (paramPage==page3) {
				config.setEnabled(false);
				plotter.setEnabled(false);
			}
		}
		
	}
	
	private void createMenu() {
		JMenuBar bar = new JMenuBar();
		
		file = new JMenu(Dictionary.getCurrent().getString("MainView.FILE")); //$NON-NLS-1$
		file.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.File").charAt(0)); //$NON-NLS-1$
		bar.add(file);
		
		config = new JMenu(Dictionary.getCurrent().getString("MainView.CONFIG")); //$NON-NLS-1$
		config.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.Config").charAt(0)); //$NON-NLS-1$
		bar.add(config);
		
		plotter = new JMenu(Dictionary.getCurrent().getString("MainView.PLOTTER")); //$NON-NLS-1$
		plotter.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.Plotter").charAt(0)); //$NON-NLS-1$
		bar.add(plotter);

		// Mainview neu malen, wenn Menü ausgewählt wurde
		file.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mainView.repaint();				
			}
		});
		config.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mainView.repaint();
			}
		});
		plotter.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mainView.repaint();
			}
		});
		//PageLocation
		JMenuItem pageLocation = new JMenuItem(Dictionary.getCurrent().getString("MainView.ShowPage")); //$NON-NLS-1$
		pageLocation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Driver.getCurrent().showPageLocation();
				goToStart.setEnabled(true);
			}
		});
		plotter.add(pageLocation);
		
		
		// Test
		JMenuItem test = new JMenuItem(Dictionary.getCurrent().getString("MainView.TEST")); //$NON-NLS-1$
		test.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.Test").charAt(0)); //$NON-NLS-1$
		test.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Driver.getCurrent().test();
				goToStart.setEnabled(true);
			}
		});
		plotter.add(test);

		// Motoren off
		JMenuItem motorsOff = new JMenuItem(Dictionary.getCurrent().getString("MainView.OFF")); //$NON-NLS-1$
		motorsOff.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.MotorsOff").charAt(0)); //$NON-NLS-1$
		motorsOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Driver.getCurrent().motorsOff();
			}
		});
		plotter.add(motorsOff);

		// Motoren on
		JMenuItem motorsOn = new JMenuItem(Dictionary.getCurrent().getString("MainView.ON")); //$NON-NLS-1$
		motorsOn.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.MotorsOn").charAt(0)); //$NON-NLS-1$
		motorsOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Driver.getCurrent().motorsOn();
			}
		});
		plotter.add(motorsOn);
		
		//GoToStart
		goToStart = new JMenuItem(Dictionary.getCurrent().getString("MainView.GoToStart")); //$NON-NLS-1$
		goToStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Driver.getCurrent().goTo(Driver.getCurrent().getStartPoint());
			}
		});
		plotter.add(goToStart);
		goToStart.setEnabled(false);
				
		
		// New
		newProcess = new JMenuItem(Dictionary.getCurrent().getString("MainView.New")); //$NON-NLS-1$
		newProcess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newProcess();
			}
		});
		file.add(newProcess);

		// Load File Menü
		loadFile = new JMenuItem(Dictionary.getCurrent().getString("MainView.LoadFile")); //$NON-NLS-1$
		loadFile.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.LoadFile").charAt(0)); //$NON-NLS-1$
		loadFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadFile();
			}
		});
		file.add(loadFile);

		// Recent Used Files Menü
		recentFiles = new JMenu(Dictionary.getCurrent().getString("MainView.RecentUsed")); //$NON-NLS-1$
		recentFiles.setMnemonic(Dictionary.getCurrent().getString("MainView.MnemonicRecentUsed").charAt(0)); //$NON-NLS-1$
		recentFiles.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mainView.repaint();
			}
		});
		file.add(recentFiles);

		loadRecent();
		
		// Exit Menü
		JMenuItem exit = new JMenuItem(Dictionary.getCurrent().getString("MainView.EXIT")); //$NON-NLS-1$
		exit.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.Exit").charAt(0)); //$NON-NLS-1$
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		file.add(exit);

		// LoadConfig Menü
		JMenuItem loadConfig = new JMenuItem(Dictionary.getCurrent().getString("MainView.LOAD")); //$NON-NLS-1$
		loadConfig.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.LoadConfig").charAt(0)); //$NON-NLS-1$
		loadConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser c = new JFileChooser();
				c.setFileFilter(new FileFilter() {
					@Override
					public String getDescription() {
						return "*" + PROPERTYSUFFIX; //$NON-NLS-1$
					}
					@Override
					public boolean accept(File f) {
						return f.getName().endsWith(PROPERTYSUFFIX) || f.isDirectory();
					}
				});
				c.setCurrentDirectory(new File("./conf/")); //$NON-NLS-1$
				c.showOpenDialog(mainView);
				File file = c.getSelectedFile();
				if (file != null) {
					VPlotterPropertiesManager.getCurrent().load(file);
					mainView.setTitle(Dictionary.getCurrent().getString("MainView.TITLE") //$NON-NLS-1$
							+ SettingsManager.getCurrent().getValue( SettingsManager.KEY.CONFIG));
				}
			}
		});
		config.add(loadConfig);

		// NewConfig Menü
		JMenuItem newConfig = new JMenuItem(Dictionary.getCurrent().getString("MainView.NEW")); //$NON-NLS-1$
		newConfig.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.NewConfig").charAt(0)); //$NON-NLS-1$
		newConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser c = new JFileChooser();
				c.setFileFilter(new FileFilter() {
					@Override
					public String getDescription() {
						return "*" + PROPERTYSUFFIX; //$NON-NLS-1$
					}

					@Override
					public boolean accept(File f) {
						return f.getName().endsWith(PROPERTYSUFFIX) || f.isDirectory();
					}
				});
				c.setCurrentDirectory(new File("./conf/")); //$NON-NLS-1$
				c.showSaveDialog(mainView);
				File file = c.getSelectedFile();
				if (file != null) {
					if (!file.getName().endsWith(PROPERTYSUFFIX)) {
						file = new File(file.getAbsolutePath() + PROPERTYSUFFIX);
					}
					VPlotterPropertiesManager.getCurrent().newConfig(file);
				}
			}
		});
		config.add(newConfig);

		// EditConfig Menü
		JMenuItem editConfig = new JMenuItem(Dictionary.getCurrent().getString("MainView.EDIT")); //$NON-NLS-1$
		editConfig.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.EditConfig").charAt(0)); //$NON-NLS-1$
		editConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser c = new JFileChooser();
				c.setFileFilter(new FileFilter() {
					@Override
					public String getDescription() {
						return "*" + PROPERTYSUFFIX; //$NON-NLS-1$
					}

					@Override
					public boolean accept(File f) {
						return f.getName().endsWith(PROPERTYSUFFIX) || f.isDirectory();
					}
				});
				c.setCurrentDirectory(new File("./conf/")); //$NON-NLS-1$
				c.showOpenDialog(mainView);
				File file = c.getSelectedFile();
				if (file != null) {
					try {
						Desktop.getDesktop().open(file);
					} catch (IOException e1) {
						e1.printStackTrace();
						try {
							Runtime.getRuntime().exec("notepad.exe " + file); //$NON-NLS-1$
						} catch (IOException e2) {
							JOptionPane.showConfirmDialog(mainView, e2.getMessage(),
									Dictionary.getCurrent().getString("LoadingPanel.Error"), JOptionPane.CLOSED_OPTION, //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
							e2.printStackTrace();
						}
					}
				}
			}
		});
		config.add(editConfig);

		setJMenuBar(bar);
	}
	
	private void loadFile() {
		String oldFile = SettingsManager.getCurrent().getValue( SettingsManager.KEY.FILEPATH);
		String filename = getFilePath();
		if(filename==null) {
			return;
		}
		
		if(!filename.equals(oldFile)) {
			if(!lastFiles.contains(filename)) {
				lastFiles.add(filename);
				saveLastOpenFiles();
			}
			Model.getCurrent().setSameFile(false);
		}
		setImage(filename);
	}
	

	private String getFilePath() {
		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		
		if (SettingsManager.getCurrent().getValue( SettingsManager.KEY.FILEPATH)!=null) {
			jfc.setCurrentDirectory(new File(SettingsManager.getCurrent().getValue( SettingsManager.KEY.FILEPATH)));
		}

		jfc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return Dictionary.getCurrent().getString("LoadingPanel.ChoosePicture"); //$NON-NLS-1$
			}

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".gif") || f.getName().toLowerCase().endsWith(".jpg")|| f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".bmp") || f.getName().toLowerCase().endsWith(".svg"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}
		});
		int returnValue = jfc.showOpenDialog(mainView);

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			try {
				SettingsManager.getCurrent().setValue( SettingsManager.KEY.FILEPATH, selectedFile.getPath());
			} catch (IOException e) {
				// TODO Meldung
				e.printStackTrace();
			}
			return selectedFile.getAbsolutePath();
		}
		return null;
	}
	
	private void saveLastOpenFiles() {
		try {
			while(lastFiles.size()>5) {
				lastFiles.remove(0);
			}
			SettingsManager.getCurrent().setValue( SettingsManager.KEY.LAST_USED, String.join(";", lastFiles));
			SettingsManager.getCurrent().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void setImage(String filename) {
		if(filename.endsWith(".svg")) {
			mainView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if(Model.getCurrent().generateDataFromSVG(filename)) {
				previewImage();
				changePage(page2);
			}
			mainView.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}else {
			try {
				BufferedImage img = ImageIO.read(new File(filename));
				Model.getCurrent().setImage(img);
				Model.getCurrent().setOriginalImage(img);
				changePage(page1);
			} catch (IOException e) {
				// TODO Meldung
				e.printStackTrace();
			}
		}
		
	}
	
	private void loadRecent() {
		recentFiles.removeAll();
		lastFiles.clear();
		if(SettingsManager.getCurrent().getValue( SettingsManager.KEY.LAST_USED)!=null){
			lastFiles.addAll(Arrays.asList(SettingsManager.getCurrent().getValue( SettingsManager.KEY.LAST_USED).split(";")));
		}
		
		for(String file:lastFiles) {
			JMenuItem filerItem = new JMenuItem(file);
			filerItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setImage(file);
				}
			});
			recentFiles.add(filerItem);
		}
	}

	public JPanel getPage0() {
		return page0;
	}

	public JPanel getPage1() {
		return page1;
	}

	public JPanel getPage2() {
		return page2;
	}

	public JPanel getPage3() {
		return page3;
	}

	public JMenu getPlotter() {
		return plotter;
	}
	
	public void previewImage() {
		Graphics g = Model.getCurrent().getSvgImage().getGraphics();
		int lastX = 0;
		int lastY = 0;
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, Model.getCurrent().getSvgImage().getWidth()-1,Model.getCurrent().getSvgImage().getHeight()-1);
		g.setColor(Color.BLACK);
		Model.getCurrent().setSectionList(Model.getCurrent().createSectionList());
		if (!Model.getCurrent().getSectionList().isEmpty()) {
			for (Section sec : Model.getCurrent().getSectionList()) {
				lastX = (int) sec.getX();
				lastY = (int) sec.getY();
				if (!sec.getLineData().isEmpty()) {
					double[] v = Arrays.asList(sec.getLineData().split(" ")).stream().mapToDouble(Double::parseDouble).toArray(); //$NON-NLS-1$
					for (int i = 0; i < v.length; i = i + 2) {
						int x = (int) v[i];
						int y = (int) v[i + 1];
						g.drawLine(lastX, lastY, x, y);
						lastX = x;
						lastY = y;
					}
				}
			}
		}
	}

}
