package aurelienribon.libgdx.polygoneditor;

import aurelienribon.libgdx.ImageModel;
import aurelienribon.libgdx.ImageModelIo;
import aurelienribon.ui.components.ArStyle;
import aurelienribon.ui.components.PaintedPanel;
import aurelienribon.ui.css.Style;
import aurelienribon.ui.css.swing.SwingStyle;
import aurelienribon.utils.notifications.AutoListModel;
import aurelienribon.utils.notifications.ObservableList;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FilenameUtils;
import res.Res;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class MainWindow extends javax.swing.JFrame {
	private final Canvas canvas;
	private final ObservableList<ImageModel> images = new ObservableList<ImageModel>();

	public MainWindow(final Canvas canvas, Component canvasCmp) {
		this.canvas = canvas;

		setContentPane(new PaintedPanel());
		getContentPane().setLayout(new BorderLayout());
		initComponents();
		renderPanel.add(canvasCmp, BorderLayout.CENTER);

		addBtn.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {add();}});
		deleteBtn.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {delete();}});
		saveBtn.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {save();}});
		loadBtn.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {load();}});

		spriteOpacitySlider.setChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {canvas.spriteOpacity = spriteOpacitySlider.getValue();}
		});
		drawTrianglesChk.setActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {canvas.drawTriangles = drawTrianglesChk.isSelected();}
		});
		drawBoundingBoxChk.setActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {canvas.drawBoundingBox = drawBoundingBoxChk.isSelected();}
		});

		imagesList.setModel(new AutoListModel<ImageModel>(images));
		imagesList.setCellRenderer(emittersListCellRenderer);
		imagesList.addListSelectionListener(emittersListSelectionListener);
		emittersListSelectionListener.valueChanged(null);

		SwingStyle.init();
		ArStyle.init();
		Style.registerCssClasses(getContentPane(), ".rootPanel");
		Style.registerCssClasses(renderPanel, ".titledPanel", "#renderPanel");
		Style.registerCssClasses(projectPanel, ".titledPanel", "#projectPanel");
		Style.registerCssClasses(optionsPanel, ".titledPanel", "#optionsPanel");
		Style.registerCssClasses(headerPanel, ".headerPanel");
		Style.registerCssClasses(versionLabel, ".versionLabel");
		Style.apply(getContentPane(), new Style(Res.getUrl("css/style.css")));
	}

	private final ListCellRenderer emittersListCellRenderer = new DefaultListCellRenderer() {
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			ImageModel img = (ImageModel) value;
			label.setText(img.file.getPath());
			return label;
		}
	};

	private final ListSelectionListener emittersListSelectionListener = new ListSelectionListener() {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			ImageModel img = (ImageModel) imagesList.getSelectedValue();
			canvas.setImage(img);
			deleteBtn.setEnabled(img != null);
			saveBtn.setEnabled(img != null);
		}
	};

	private void add() {
		JFileChooser chooser = new JFileChooser(".");
		chooser.setDialogTitle("Choose one or more images");
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileFilter(new FileFilter() {
			@Override public String getDescription() {return "Images (png, jpg)";}
			@Override public boolean accept(File f) {
				if (f.isDirectory()) return true;
				String ext = FilenameUtils.getExtension(f.getName());
				return ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg");
			}
		});

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			for (File file : chooser.getSelectedFiles()) {
				try {
					ImageModel img = new ImageModel(file);
					images.add(img);
					Collections.sort(images, new Comparator<ImageModel>() {
						@Override public int compare(ImageModel o1, ImageModel o2) {
							String s1 = o1.file.getPath();
							String s2 = o2.file.getPath();
							if (s1.compareToIgnoreCase(s2) < 0) return -1;
							if (s1.compareToIgnoreCase(s2) > 0) return 1;
							return 0;
						}
					});
					imagesList.setSelectedValue(img, rootPaneCheckingEnabled);
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(this, "Cannot get the canonical path of file:\n" + file.getPath());
				}
			}
		}
	}

	private void delete() {
		ImageModel img = (ImageModel) imagesList.getSelectedValue();
		images.remove(img);
		imagesList.clearSelection();
	}

	private void load() {
		JFileChooser chooser = new JFileChooser(".");
		chooser.setDialogTitle("Choose the file to read");

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				File file = chooser.getSelectedFile();
				images.clear();
				images.addAll(ImageModelIo.load(file));
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Cannot load the project, reason is:\n" + ex.getMessage());
			}
		}
	}

	private void save() {
		JFileChooser chooser = new JFileChooser(".");
		chooser.setDialogTitle("Choose the file to write or overwrite");

		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				File file = chooser.getSelectedFile();
				ImageModelIo.save(file, images);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Cannot save the project, reason is:\n" + ex.getMessage());
			}
		}
	}

	// -------------------------------------------------------------------------
	// Generated stuff
	// -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        renderPanel = new javax.swing.JPanel();
        projectPanel = new javax.swing.JPanel();
        headerPanel = new javax.swing.JPanel();
        jToolBar4 = new javax.swing.JToolBar();
        addBtn = new javax.swing.JButton();
        deleteBtn = new javax.swing.JButton();
        jToolBar7 = new javax.swing.JToolBar();
        saveBtn = new javax.swing.JButton();
        loadBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        imagesList = new javax.swing.JList();
        optionsPanel = new javax.swing.JPanel();
        spriteOpacitySlider = new aurelienribon.libgdx.polygoneditor.CompactSlider();
        drawTrianglesChk = new aurelienribon.libgdx.polygoneditor.CompactCheckBox();
        jLabel1 = new javax.swing.JLabel();
        drawBoundingBoxChk = new aurelienribon.libgdx.polygoneditor.CompactCheckBox();
        jLabel2 = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Polygon Editor");

        renderPanel.setLayout(new java.awt.BorderLayout());

        projectPanel.setLayout(new java.awt.BorderLayout());

        headerPanel.setLayout(new java.awt.BorderLayout());

        jToolBar4.setFloatable(false);
        jToolBar4.setRollover(true);

        addBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/gfx/ic_add.png"))); // NOI18N
        addBtn.setText("Add image(s)");
        addBtn.setFocusable(false);
        addBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        addBtn.setMargin(new java.awt.Insets(2, 3, 2, 3));
        jToolBar4.add(addBtn);

        deleteBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/gfx/ic_delete.png"))); // NOI18N
        deleteBtn.setFocusable(false);
        deleteBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar4.add(deleteBtn);

        headerPanel.add(jToolBar4, java.awt.BorderLayout.WEST);

        jToolBar7.setFloatable(false);
        jToolBar7.setRollover(true);

        saveBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/gfx/ic_save.png"))); // NOI18N
        saveBtn.setText("Save");
        saveBtn.setFocusable(false);
        saveBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        saveBtn.setMargin(new java.awt.Insets(2, 3, 2, 3));
        jToolBar7.add(saveBtn);

        loadBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/gfx/ic_open.png"))); // NOI18N
        loadBtn.setText("Load");
        loadBtn.setFocusable(false);
        loadBtn.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jToolBar7.add(loadBtn);

        headerPanel.add(jToolBar7, java.awt.BorderLayout.EAST);

        projectPanel.add(headerPanel, java.awt.BorderLayout.NORTH);

        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        imagesList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        imagesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(imagesList);

        projectPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        spriteOpacitySlider.setValue(0.5F);

        drawTrianglesChk.setSelected(true);
        drawTrianglesChk.setText("Draw triangles");

        jLabel1.setText("Image opacity");

        drawBoundingBoxChk.setSelected(true);
        drawBoundingBoxChk.setText("Draw bounding box");

        javax.swing.GroupLayout optionsPanelLayout = new javax.swing.GroupLayout(optionsPanel);
        optionsPanel.setLayout(optionsPanelLayout);
        optionsPanelLayout.setHorizontalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(optionsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spriteOpacitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(optionsPanelLayout.createSequentialGroup()
                        .addComponent(drawTrianglesChk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(drawBoundingBoxChk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        optionsPanelLayout.setVerticalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(spriteOpacitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(drawTrianglesChk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(drawBoundingBoxChk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/gfx/logo.png"))); // NOI18N

        versionLabel.setText("v0.1.0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(renderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(projectPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                    .addComponent(optionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(versionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(projectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(optionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(versionLabel, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addComponent(renderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 463, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addBtn;
    private javax.swing.JButton deleteBtn;
    private aurelienribon.libgdx.polygoneditor.CompactCheckBox drawBoundingBoxChk;
    private aurelienribon.libgdx.polygoneditor.CompactCheckBox drawTrianglesChk;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JList imagesList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JToolBar jToolBar7;
    private javax.swing.JButton loadBtn;
    private javax.swing.JPanel optionsPanel;
    private javax.swing.JPanel projectPanel;
    private javax.swing.JPanel renderPanel;
    private javax.swing.JButton saveBtn;
    private aurelienribon.libgdx.polygoneditor.CompactSlider spriteOpacitySlider;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables

}
