package de.revout.pi.vplotter.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Graphics;
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

    private static final long serialVersionUID = 1L;
    private static final String PROPERTY_SUFFIX = ".plotterconf";

    public static final Color COLOR1 = new Color(162, 182, 210);
    public static final Color COLOR2 = new Color(137, 164, 201);
    public static final Color COLOR3 = new Color(171, 198, 222);

    private StackLayout stackLayout;
    private JPanel homePage;
    private JPanel filterPanel;
    private JPanel previewPanel;
    private JPanel liveViewPanel;
    private JMenuItem loadFileItem;
    private JMenuItem newProcessItem;
    private JMenuItem goToStartItem;
    private JMenu plotterMenu;
    private JMenu configMenu;
    private JMenu recentFilesMenu;
    private JMenu fileMenu;
    private List<String> lastFiles;

    public MainView() {
        init();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Driver.getCurrent().gpioStop();
            }
        });
    }

    private void init() {
        lastFiles = new ArrayList<>();
        Driver.getCurrent().loadProperty();
        setTitle(Version.number + " " + Dictionary.getCurrent().getString("MainView.TITLE") 
                + SettingsManager.getCurrent().getValue(SettingsManager.KEY.CONFIG));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        createMenu();
        stackLayout = new StackLayout();
        setLayout(stackLayout);
        createPages();
        setVisible(true);
    }

    private void createPages() {
        // Home page
        homePage = new HomePage();
        homePage.setBackground(COLOR1);

        // Filter panel
        filterPanel = new FilterPanel(this);
        filterPanel.setBackground(COLOR1);

        // Preview panel
        previewPanel = new PlotterPreview(this);
        previewPanel.setBackground(COLOR1);

        // Live view panel
        liveViewPanel = new LiveView(this);
        liveViewPanel.setBackground(COLOR1);

        // Register pages im StackLayout mit eindeutigen Keys
        stackLayout.addLayoutComponent("home", homePage);
        stackLayout.addLayoutComponent("filter", filterPanel);
        stackLayout.addLayoutComponent("preview", previewPanel);
        stackLayout.addLayoutComponent("live", liveViewPanel);

        // Zeige zunächst die Home-Page
        changePage(homePage);
    }

    public void newProcess() {
        Model.getCurrent().newProcess();
        createPages();
        validate();
        repaint();
    }

    public void changePage(JPanel page) {
        stackLayout.showComponent(page, this);
        boolean used = Driver.getCurrent().isUsed();
        goToStartItem.setEnabled(used);
        recentFilesMenu.setEnabled(true);
        loadFileItem.setEnabled(true);
        newProcessItem.setEnabled(true);
        configMenu.setEnabled(true);
        plotterMenu.setEnabled(true);

        if (page == homePage) {
            newProcessItem.setEnabled(false);
        } else {
            if (page == filterPanel) {
                if (Model.getCurrent().getImage() == null) {
                    if (Model.getCurrent().getSvgImage() != null) {
                        changePage(previewPanel);
                    } else {
                        changePage(homePage);
                    }
                }
            }
            recentFilesMenu.setEnabled(false);
            loadFileItem.setEnabled(false);
            if (page == previewPanel) {
                plotterMenu.setEnabled(false);
            }
            if (page == liveViewPanel) {
                configMenu.setEnabled(false);
                plotterMenu.setEnabled(false);
            }
        }
    }

    private void createMenu() {
        JMenuBar bar = new JMenuBar();

        fileMenu = new JMenu(Dictionary.getCurrent().getString("MainView.FILE"));
        fileMenu.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.File").charAt(0));
        bar.add(fileMenu);

        configMenu = new JMenu(Dictionary.getCurrent().getString("MainView.CONFIG"));
        configMenu.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.Config").charAt(0));
        bar.add(configMenu);

        plotterMenu = new JMenu(Dictionary.getCurrent().getString("MainView.PLOTTER"));
        plotterMenu.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.Plotter").charAt(0));
        bar.add(plotterMenu);

        // Einfacher Repaint-Listener für alle Menüs
        ChangeListener repaintListener = e -> repaint();
        fileMenu.addChangeListener(repaintListener);
        configMenu.addChangeListener(repaintListener);
        plotterMenu.addChangeListener(repaintListener);

        // Plotter-Menüeinträge
        JMenuItem pageLocation = new JMenuItem(Dictionary.getCurrent().getString("MainView.ShowPage"));
        pageLocation.addActionListener(e -> {
            try {
				changePage(getLiveViewPanel());
				Driver.getCurrent().showPageLocation();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
            goToStartItem.setEnabled(true);
        });
        plotterMenu.add(pageLocation);

        JMenuItem testItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.TEST"));
        testItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.Test").charAt(0));
        testItem.addActionListener(e -> {
            try {
				changePage(getLiveViewPanel());
            	Driver.getCurrent().test();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
            goToStartItem.setEnabled(true);
        });
        plotterMenu.add(testItem);

        JMenuItem motorsOffItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.OFF"));
        motorsOffItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.MotorsOff").charAt(0));
        motorsOffItem.addActionListener(e -> Driver.getCurrent().motorsOff());
        plotterMenu.add(motorsOffItem);

        JMenuItem motorsOnItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.ON"));
        motorsOnItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.MotorsOn").charAt(0));
        motorsOnItem.addActionListener(e -> Driver.getCurrent().motorsOn());
        plotterMenu.add(motorsOnItem);

        JMenuItem penAwayItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.PanAway"));
        penAwayItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.PanAway").charAt(0));
        penAwayItem.addActionListener(e -> Driver.getCurrent().penAway());
        plotterMenu.add(penAwayItem);

        JMenuItem penDrawItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.PanDraw"));
        penDrawItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.PanDraw").charAt(0));
        penDrawItem.addActionListener(e -> Driver.getCurrent().penDraw());
        plotterMenu.add(penDrawItem);

        
        goToStartItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.GoToStart"));
        goToStartItem.addActionListener(e -> Driver.getCurrent().goTo(Driver.getCurrent().getStartPoint()));
        plotterMenu.add(goToStartItem);
        goToStartItem.setEnabled(false);

        // File-Menüeinträge
        newProcessItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.New"));
        newProcessItem.addActionListener(e -> newProcess());
        fileMenu.add(newProcessItem);

        loadFileItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.LoadFile"));
        loadFileItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.LoadFile").charAt(0));
        loadFileItem.addActionListener(e -> loadFile());
        fileMenu.add(loadFileItem);

        recentFilesMenu = new JMenu(Dictionary.getCurrent().getString("MainView.RecentUsed"));
        recentFilesMenu.setMnemonic(Dictionary.getCurrent().getString("MainView.MnemonicRecentUsed").charAt(0));
        recentFilesMenu.addChangeListener(repaintListener);
        fileMenu.add(recentFilesMenu);

        loadRecentFiles();

        JMenuItem exitItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.EXIT"));
        exitItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.Exit").charAt(0));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // Config-Menüeinträge
        JMenuItem loadConfigItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.LOAD"));
        loadConfigItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.LoadConfig").charAt(0));
        loadConfigItem.addActionListener(e -> loadConfig());
        configMenu.add(loadConfigItem);

        JMenuItem newConfigItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.NEW"));
        newConfigItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.NewConfig").charAt(0));
        newConfigItem.addActionListener(e -> newConfig());
        configMenu.add(newConfigItem);

        JMenuItem editConfigItem = new JMenuItem(Dictionary.getCurrent().getString("MainView.EDIT"));
        editConfigItem.setMnemonic(Dictionary.getCurrent().getString("MainView.Mnemonic.EditConfig").charAt(0));
        editConfigItem.addActionListener(e -> editConfig());
        configMenu.add(editConfigItem);

        setJMenuBar(bar);
    }

    private FileFilter createConfigFileFilter() {
        return new FileFilter() {
            @Override
            public String getDescription() {
                return "*" + PROPERTY_SUFFIX;
            }
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(PROPERTY_SUFFIX);
            }
        };
    }

    private void loadConfig() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(createConfigFileFilter());
        chooser.setCurrentDirectory(new File("./conf/"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            VPlotterPropertiesManager.getCurrent().load(file);
            setTitle(Dictionary.getCurrent().getString("MainView.TITLE") 
                    + SettingsManager.getCurrent().getValue(SettingsManager.KEY.CONFIG));
        }
    }

    private void newConfig() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(createConfigFileFilter());
        chooser.setCurrentDirectory(new File("./conf/"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(PROPERTY_SUFFIX)) {
                file = new File(file.getAbsolutePath() + PROPERTY_SUFFIX);
            }
            VPlotterPropertiesManager.getCurrent().newConfig(file);
        }
    }

    private void editConfig() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(createConfigFileFilter());
        chooser.setCurrentDirectory(new File("./conf/"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException ex) {
                ex.printStackTrace();
                try {
                    Runtime.getRuntime().exec("notepad.exe " + file);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), 
                        Dictionary.getCurrent().getString("LoadingPanel.Error"), JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadFile() {
        String oldFile = SettingsManager.getCurrent().getValue(SettingsManager.KEY.FILEPATH);
        String filename = getFilePath();
        if (filename == null) {
            return;
        }
        if (!filename.equals(oldFile)) {
            if (!lastFiles.contains(filename)) {
                lastFiles.add(filename);
                saveLastOpenFiles();
            }
            Model.getCurrent().setSameFile(false);
        }
        setImage(filename);
    }

    private FileFilter createImageFileFilter() {
        return new FileFilter() {
            @Override
            public String getDescription() {
                return Dictionary.getCurrent().getString("LoadingPanel.ChoosePicture");
            }
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".png") || name.endsWith(".gif") 
                    || name.endsWith(".jpg") || name.endsWith(".jpeg") 
                    || name.endsWith(".bmp") || name.endsWith(".svg");
            }
        };
    }

    private String getFilePath() {
        JFileChooser chooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        String currentPath = SettingsManager.getCurrent().getValue(SettingsManager.KEY.FILEPATH);
        if (currentPath != null) {
            chooser.setCurrentDirectory(new File(currentPath));
        }
        chooser.setFileFilter(createImageFileFilter());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                SettingsManager.getCurrent().setValue(SettingsManager.KEY.FILEPATH, selectedFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return selectedFile.getAbsolutePath();
        }
        return null;
    }

    private void saveLastOpenFiles() {
        try {
            while (lastFiles.size() > 5) {
                lastFiles.remove(0);
            }
            SettingsManager.getCurrent().setValue(SettingsManager.KEY.LAST_USED, String.join(";", lastFiles));
            SettingsManager.getCurrent().save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setImage(String filename) {
        if (filename.toLowerCase().endsWith(".svg")) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if (Model.getCurrent().generateDataFromSVG(filename)) {
                previewImage();
                changePage(previewPanel);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else {
            try {
                BufferedImage img = ImageIO.read(new File(filename));
                Model.getCurrent().setImage(img);
                Model.getCurrent().setOriginalImage(img);
                changePage(filterPanel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadRecentFiles() {
        recentFilesMenu.removeAll();
        lastFiles.clear();
        String lastUsed = SettingsManager.getCurrent().getValue(SettingsManager.KEY.LAST_USED);
        if (lastUsed != null) {
            lastFiles.addAll(Arrays.asList(lastUsed.split(";")));
        }
        for (String file : lastFiles) {
            JMenuItem item = new JMenuItem(file);
            item.addActionListener(e -> setImage(file));
            recentFilesMenu.add(item);
        }
    }

    public JPanel getHomePage() {
        return homePage;
    }

    public JPanel getFilterPanel() {
        return filterPanel;
    }

    public JPanel getPreviewPanel() {
        return previewPanel;
    }

    public JPanel getLiveViewPanel() {
        return liveViewPanel;
    }

    public JMenu getPlotterMenu() {
        return plotterMenu;
    }

    public void previewImage() {
        BufferedImage svgImage = Model.getCurrent().getSvgImage();
        if (svgImage == null) return;
        Graphics g = svgImage.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, svgImage.getWidth() - 1, svgImage.getHeight() - 1);
        g.setColor(Color.BLACK);
        Model.getCurrent().setSectionList(Model.getCurrent().createSectionList());
        if (!Model.getCurrent().getSectionList().isEmpty()) {
            for (Section sec : Model.getCurrent().getSectionList()) {
                int lastX = (int) sec.getX();
                int lastY = (int) sec.getY();
                if (!sec.getLineData().isEmpty()) {
                    double[] values = Arrays.stream(sec.getLineData().split(" "))
                                              .mapToDouble(Double::parseDouble)
                                              .toArray();
                    for (int i = 0; i < values.length; i += 2) {
                        int x = (int) values[i];
                        int y = (int) values[i + 1];
                        g.drawLine(lastX, lastY, x, y);
                        lastX = x;
                        lastY = y;
                    }
                }
            }
        }
        g.dispose();
    }
}
