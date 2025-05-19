package io.improt.vai.frame;

import com.openai.models.ReasoningEffort;
import io.improt.vai.backend.App;
import io.improt.vai.frame.actions.NewProjectAction;
import io.improt.vai.frame.actions.OpenPathAction;
import io.improt.vai.frame.component.FileViewerPanel;
import io.improt.vai.frame.component.ProjectPanel;
import io.improt.vai.frame.component.ActiveFilesPanel;
import io.improt.vai.frame.component.RecentActiveFilesPanel;
import io.improt.vai.frame.dialogs.CreatePlanDialog; 
import io.improt.vai.frame.dialogs.FeaturesDialog;
import io.improt.vai.frame.dialogs.RepairDialog;
import io.improt.vai.frame.dialogs.ResizableMessageHistoryDialog;
import io.improt.vai.mapping.SubWorkspace; 
import io.improt.vai.llm.Tasks; 
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;
import io.improt.vai.util.AudioUtils;
import io.improt.vai.util.FileUtils;
import io.improt.vai.util.MessageHistoryManager;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.TreePath;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Timer;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;


public class ClientFrame extends JFrame implements ActiveFilesPanel.FileSelectionListener {

    private final RecentActiveFilesPanel recentActiveFilesPanel;
    private ByteArrayOutputStream byteArrayOutputStream;
    private FileViewerPanel fileViewerPanel = null;
    private final JComboBox<String> modelCombo;
    private final JMenu recentActiveFilesMenu;
    public static boolean pasting = false;
    private final JSlider reasoningSlider;
    private final JLabel statusBarLabel;
    private boolean isRecording = false;
    private final JButton recordButton;
    private ProjectPanel projectPanel;
    private TargetDataLine audioLine;
    private final JTextArea textArea;
    private long recordingStartTime;
    private final JMenu recentMenu;
    private final JButton submitButton;
    private Thread recordingThread;
    private Timer recordingTimer;
    private File currentFile;
    private File waveFile;
    private App backend;
    

    public ClientFrame() {
        super("Vai");
        applyTheme();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setResizable(true);

        try {
            setIconImage(Toolkit.getDefaultToolkit().getImage("/images/icon.png"));
        } catch (Exception e) {
            System.err.println("Icon 'icon.ico' not found, using default Java icon.");
        }

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.decode("#FFFFFF"));
        menuBar.setForeground(Color.decode("#202124"));

        JMenu fileMenu = new JMenu("File");
        recentMenu = new JMenu("Recent");
        recentActiveFilesMenu = new JMenu("Recent Active Files"); 
        populateRecentMenu();
        populateMenu(); 

        JMenu configMenu = new JMenu("Config");
        JMenu featuresMenu = new JMenu("Features");
        JMenuItem configureFeaturesItem = new JMenuItem("Configure Features");
        configureFeaturesItem.addActionListener(e -> {
            FeaturesDialog featuresDialog = new FeaturesDialog(this);
            featuresDialog.setVisible(true);
        });
        featuresMenu.add(configureFeaturesItem);

        JMenu contextMenu = new JMenu("Context"); 

        JMenuItem createPlanItem = new JMenuItem("Create Plan..."); 
        createPlanItem.addActionListener(e -> handleCreatePlanAction());
        contextMenu.add(createPlanItem); 

        JMenuItem manageWorkspacesItem = new JMenuItem("Manage Workspaces...");
        manageWorkspacesItem.addActionListener(e -> {
            openWorkspaceMapperPanel();
        });
        contextMenu.add(manageWorkspacesItem);

        JMenuItem newProjectItem = new JMenuItem("New Project...");
        JMenuItem openDirItem = new JMenuItem("Open Directory...");
        JMenuItem openPathItem = new JMenuItem("Open Path...");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        JMenuItem exitItem = new JMenuItem("Exit");
        JMenuItem configureItem = new JMenuItem("Configure...");

        newProjectItem.addActionListener(new NewProjectAction(this));
        openDirItem.addActionListener(e -> {
            backend.showOpenWorkspaceDialog(this);
            projectPanel.refreshTree(backend.getCurrentWorkspace());
            populateRecentMenu();
            updateTitle();
        });

        openPathItem.addActionListener(new OpenPathAction(this));
        exitItem.addActionListener(e -> System.exit(0));
        configureItem.addActionListener(e -> new ConfigureFrame(this));
        refreshItem.addActionListener(e -> projectPanel.refreshTree(backend.getCurrentWorkspace()));

        fileMenu.add(newProjectItem);
        fileMenu.add(openDirItem);
        fileMenu.add(openPathItem);
        fileMenu.add(recentMenu);
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();

        JMenuItem openProjectDirItem = new JMenuItem("Open Project Directory");
        openProjectDirItem.addActionListener(e -> openProjectDirectory());
        fileMenu.add(openProjectDirItem);

        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        configMenu.add(configureItem);

        menuBar.add(fileMenu);
        menuBar.add(configMenu);
        menuBar.add(contextMenu); 
        menuBar.add(featuresMenu);
        menuBar.add(recentActiveFilesMenu);
        setJMenuBar(menuBar);

        // Panels configuration
        projectPanel = new ProjectPanel();
        backend = new App(this);
        backend.init();
        projectPanel.init(backend);
        ActiveFilesPanel activeFilesPanel = new ActiveFilesPanel(backend);
        activeFilesPanel.setFileSelectionListener(this);
        recentActiveFilesPanel = new RecentActiveFilesPanel();
        fileViewerPanel = new FileViewerPanel();

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.decode("#F1F3F4"));
        rightPanel.add(fileViewerPanel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(Color.decode("#F1F3F4"));

        textArea = new JTextArea(5, 40);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane textScrollPane = new JScrollPane(textArea);
        textScrollPane.setPreferredSize(new Dimension(100, 150));
        inputPanel.add(textScrollPane, BorderLayout.CENTER);

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem pasteMenuItem = new JMenuItem("Paste");
                    pasteMenuItem.addActionListener(pasteAction -> handlePaste());
                    popup.add(pasteMenuItem);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        textArea.getInputMap().put(KeyStroke.getKeyStroke("control V"), "paste");
        textArea.getActionMap().put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!pasting) {
                    handlePaste();
                }
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.decode("#F1F3F4"));
        recordButton = createRecordButton();
        bottomPanel.add(recordButton);

        reasoningSlider = new JSlider(0, 2, 1);
        reasoningSlider.setMajorTickSpacing(1);
        reasoningSlider.setPaintTicks(true);
        reasoningSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        Font tinyFont = new Font("Segoe UI", Font.PLAIN, 10);
        JLabel low = new JLabel("Low");
        JLabel med = new JLabel("Medium");
        JLabel hi = new JLabel("High");
        low.setFont(tinyFont);
        med.setFont(tinyFont);
        hi.setFont(tinyFont);
        labelTable.put(0, low);
        labelTable.put(1, med);
        labelTable.put(2, hi);
        reasoningSlider.setLabelTable(labelTable);
        reasoningSlider.setPreferredSize(new Dimension(150, 50));
        reasoningSlider.setVisible(false);
        bottomPanel.add(reasoningSlider);

        List<String> modelNames = App.getInstance().getLLMRegistry().getRegisteredModelNames();
        Collections.reverse(modelNames);
        modelCombo = new JComboBox<>(modelNames.toArray(new String[0]));

        if(modelNames.contains("Gemini Pro")){
            modelCombo.setSelectedItem("Gemini Pro");
            updateReasoningSliderVisibility();
        }

        modelCombo.addActionListener(e -> {
            updateRecordButtonState();
            updateReasoningSliderVisibility();
        });
        bottomPanel.add(modelCombo);


        JButton submitButton = createSubmitButton();
        bottomPanel.add(submitButton);
        this.submitButton = submitButton;

        inputPanel.add(bottomPanel, BorderLayout.SOUTH);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        JSplitPane mainSplitPane = createLeftPanel(activeFilesPanel, rightPanel);
        getContentPane().add(mainSplitPane, BorderLayout.CENTER);

        statusBarLabel = new JLabel("Ready");
        getContentPane().add(statusBarLabel, BorderLayout.SOUTH);

        projectPanel.getTree().addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            File selectedFile = projectPanel.pathToFile(path);
            if (selectedFile != null && selectedFile.isFile()) {
                fileViewerPanel.displayFile(selectedFile);
                this.currentFile = selectedFile;
            } else {
                fileViewerPanel.clear();
                this.currentFile = null;
            }
        });

        addSubmitShortcut(submitButton);
        updateTitle();
        updateRecordButtonState();

        setVisible(true);
    }

    private void applyTheme() {
        getContentPane().setBackground(Color.decode("#F1F3F4"));
        setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    private void updateReasoningSliderVisibility() {
        String selectedModel = (String) modelCombo.getSelectedItem();
        if (selectedModel != null) {
            IModelProvider provider = backend.getLLMProvider(selectedModel);

            if (provider instanceof OpenAIClientBase) {
                reasoningSlider.setVisible(((OpenAIClientBase) provider).supportsReasoningEffort());
            } else {
                reasoningSlider.setVisible(false);
            }

            this.revalidate();
            this.repaint();
        }
    }

    private void updateRecordButtonState() {
        String selectedModel = (String) modelCombo.getSelectedItem();
        if (selectedModel != null) {
            IModelProvider provider = backend.getLLMProvider(selectedModel);

            if (provider != null && !provider.supportsAudio()) {
                System.out.println("Disabling record button for model: " + selectedModel);
                recordButton.setEnabled(false);
                recordButton.setVisible(false);
                recordButton.setToolTipText("Audio recording disabled for model: " + selectedModel);
            } else {
                System.out.println("Enabling record button for model: " + selectedModel);
                recordButton.setVisible(true);
                recordButton.setEnabled(true);
                recordButton.setToolTipText(null); 
            }
        } else {
            recordButton.setEnabled(false); 
            recordButton.setToolTipText(null);
        }
    }

    private JButton createRecordButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(30, 30)); 
        Border roundedBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5); 
        button.setBorder(roundedBorder);

        button.setText("●"); 
        button.setFont(new Font("Arial", Font.BOLD, 16)); 

        button.setBackground(Color.gray); 
        button.setOpaque(true);
        button.setBorderPainted(false); 

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (!isRecording) {
                        startRecording();
                        button.setBackground(Color.red); 
                    } else {
                        stopRecording(true, true); 
                        button.setBackground(Color.gray); 
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (isRecording) {
                        stopRecording(false, false); 
                        button.setBackground(Color.gray); 
                    }
                }
            }
        });
        return button;
    }

    private void startRecording() {
        try {
            waveFile = createWavFile();
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Line not supported: " + info);
                return;
            }

            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            byteArrayOutputStream = new ByteArrayOutputStream(); 

            audioLine.start();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            startTimer();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isRecording) {
                    int bytesRead = audioLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            });
            recordingThread.start();

            System.out.println("Start recording to " + waveFile.getAbsolutePath());
        } catch (LineUnavailableException ex) {
            System.err.println("Line unavailable: " + ex);
            ex.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void stopRecording(boolean saveFile, boolean submit) {
        isRecording = false;
        stopTimer();

        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
        audioLine = null; 

        if (recordingThread != null) {
            try {
                recordingThread.join(); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            recordingThread = null; 
        }

        if (saveFile) {
            try {
                byte[] audioData = byteArrayOutputStream.toByteArray();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
                AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, getAudioFormat(), audioData.length / getAudioFormat().getFrameSize()); 
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, waveFile);
                System.out.println("Recording saved to " + waveFile.getAbsolutePath());

                if (submit) {
                    handleSubmitPromptWav();
                }

            } catch (IOException ex) {
                System.err.println("Error saving recording: " + ex);
                ex.printStackTrace();
            }
        } else {
            if (waveFile != null && waveFile.exists()) {
                boolean delete = waveFile.delete();
                System.out.println("Recording cancelled and temporary file deleted - " + delete);
            }
        }

        if (byteArrayOutputStream != null) {
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            byteArrayOutputStream = null; 
        }
        statusBarLabel.setText("Ready"); 
    }

    private void handleSubmitPromptWav() {
        File promptWav = this.waveFile;
        if (promptWav != null && promptWav.exists()) {
            backend.getActiveFileManager().addFile(promptWav);
            String promptText = "Review the audio (`Prompt.wav`) and follow instructions within it.";
            String selectedModel = (String) modelCombo.getSelectedItem();
            if (selectedModel != null) {
                Runnable retryAction = () -> backend.getLLM().submitRequest(selectedModel, promptText);
                try {
                    backend.getLLM().submitRequest(selectedModel, promptText);
                } catch (RuntimeException ex) {
                    showLLMErrorPopup("1 LLM Error: " + ex.getMessage(), retryAction);
                }
            } else {
                System.out.println("No model selected, cannot submit prompt.");
                backend.getActiveFileManager().removeFile(promptWav); 
                return; 
            }
            backend.getActiveFileManager().removeFile(promptWav);
        }
    }

    private File createWavFile() throws IOException {
        File vaiDir = FileUtils.getWorkspaceVaiDir(backend.getCurrentWorkspace());
        return new File(vaiDir, "Prompt.wav");
    }

    private AudioFormat getAudioFormat() {
        return AudioUtils.getAudioFormat();
    }

    private void startTimer() {
        recordingTimer = new Timer(100, e -> {
            long elapsedTimeSeconds = (System.currentTimeMillis() - recordingStartTime) / 1000;
            long minutes = (elapsedTimeSeconds / 60) % 60;
            long seconds = elapsedTimeSeconds % 60;
            statusBarLabel.setText(String.format("Recording: %02d:%02d", minutes, seconds) + ". Right-click to cancel."); 
        });
        recordingTimer.start();
    }

    private void stopTimer() {
        if (recordingTimer != null && recordingTimer.isRunning()) {
            recordingTimer.stop();
            recordingTimer = null;
        }
    }

    private void handlePaste() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);

                File tempImageFile = backend.saveImageAsTempFile(image);
                if (tempImageFile != null) {
                    backend.getActiveFileManager().addFile(tempImageFile);
                    fileViewerPanel.displayFile(tempImageFile); 
                    projectPanel.refreshTree(backend.getCurrentWorkspace()); 
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to save image from clipboard.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error pasting image: " + ex.getMessage(), "Paste Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            pasting = true;
            try {
                String text = (String) clipboard.getData(DataFlavor.stringFlavor);
                String selectedText = textArea.getSelectedText();
                if (selectedText != null) {
                    textArea.setText(textArea.getText().replace(selectedText, text)); 
                } else {
                    int caretPosition = textArea.getCaretPosition();
                    textArea.replaceRange(text, caretPosition, caretPosition);
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to paste: " + ex.getMessage(), "Paste Error", JOptionPane.ERROR_MESSAGE);
            }
            pasting = false;
        }
    }

    @NotNull
    private JButton createSubmitButton() {
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            this.submit(null);
        });
        return submitButton;
    }

    @NotNull
    private JSplitPane createLeftPanel(ActiveFilesPanel activeFilesPanel, JPanel rightPanel) {
        JSplitPane verticalSplitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, activeFilesPanel, projectPanel);
        verticalSplitPane1.setDividerLocation(180);
        verticalSplitPane1.setResizeWeight(0);
        verticalSplitPane1.setOneTouchExpandable(true);

        JSplitPane verticalSplitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, verticalSplitPane1, recentActiveFilesPanel);
        verticalSplitPane2.setDividerLocation(180 + 276);
        verticalSplitPane2.setResizeWeight(0);
        verticalSplitPane2.setOneTouchExpandable(true);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, verticalSplitPane2, rightPanel);
        mainSplitPane.setDividerLocation(400); 

        return mainSplitPane;
    }

    @Override
    public void onFileSelected(File file) {
        fileViewerPanel.displayFile(file);
        this.currentFile = file;
    }

    public FileViewerPanel getFileViewerPanel() {
        return this.fileViewerPanel;
    }

    public ProjectPanel getProjectPanel() {
        return this.projectPanel;
    }

    public void populateRecentMenu() {
        recentMenu.removeAll();

        List<String> recentProjects = FileUtils.loadRecentProjects();

        if (recentProjects.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("No Recent Projects");
            emptyItem.setEnabled(false);
            recentMenu.add(emptyItem);
            return;
        }

        for (String path : recentProjects) {
            JMenuItem projectItem = new JMenuItem(formatProjectName(path));
            projectItem.setToolTipText(path);
            projectItem.addActionListener(e -> {
                File workspace = new File(path);
                if (workspace.exists() && workspace.isDirectory()) {
                    backend.openWorkspace(workspace);
                    projectPanel.refreshTree(backend.getCurrentWorkspace());
                    populateRecentMenu();
                    updateTitle();
                } else {
                    JOptionPane.showMessageDialog(this, "The project directory does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                    List<String> updatedRecentProjects = new ArrayList<>(FileUtils.loadRecentProjects());
                    updatedRecentProjects.remove(path);
                    FileUtils.saveRecentProjects(updatedRecentProjects);
                    populateRecentMenu();
                }
            });
            recentMenu.add(projectItem);
        }
    }

    private void populateMenu() {
        JMenuItem clearRecentFilesItem = new JMenuItem("Clear Recent Files");
        JMenuItem hack = new JMenuItem("Test Berzfad"); 
        JMenuItem messages = new JMenuItem("Messages");

        clearRecentFilesItem.addActionListener(e -> {
            int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear all recent files?", "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                File currentWorkspace = backend.getCurrentWorkspace();
                FileUtils.saveRecentlyActiveFiles(new ArrayList<>(), currentWorkspace);
                recentActiveFilesPanel.refresh();
            }
        });

        hack.addActionListener(e -> {
            RepairDialog repairDialog = new RepairDialog(this, "Hello world", "Exception");
            repairDialog.setVisible(true);
            backend.getLLM().handleCodeResponse(repairDialog.getCorrectedCode());
        });

        messages.addActionListener(e -> {
            MessageHistoryManager historyManager = new MessageHistoryManager(App.getInstance().getCurrentWorkspace());
            SwingUtilities.invokeLater(() -> {
                ResizableMessageHistoryDialog dialog = new ResizableMessageHistoryDialog(historyManager);
                dialog.setVisible(true);
            });
        });
        
        recentActiveFilesMenu.add(clearRecentFilesItem);
        recentActiveFilesMenu.add(hack);
        recentActiveFilesMenu.add(messages);
    }

    private String formatProjectName(String path) {
        File file = new File(path);
        StringBuilder nameBuilder = new StringBuilder("...");
        File parent = file.getParentFile();
        if (parent != null) {
            File grandParent = parent.getParentFile();
            if (grandParent != null) {
                nameBuilder.append(grandParent.getName()).append("/");
            }
            nameBuilder.append(file.getName());
        } else {
            nameBuilder.append(file.getName());
        }
        return nameBuilder.toString();
    }

    public RecentActiveFilesPanel getRecentActiveFilesPanel() {
        return recentActiveFilesPanel;
    }

    public void setLLMPrompt(String prompt) {
        textArea.setText(prompt);
        textArea.setBackground(new Color(144, 238, 144)); 

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            private boolean isReset = false;

            @Override
            public void insertUpdate(DocumentEvent e) {
                resetBackground();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetBackground();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetBackground();
            }

            private void resetBackground() {
                if (!isReset) {
                    textArea.setBackground(Color.WHITE);
                    isReset = true;
                    textArea.getDocument().removeDocumentListener(this);
                }
            }
        });
    }

    public void updateTitle() {
        File currentWorkspace = backend.getCurrentWorkspace();
        if (currentWorkspace != null && currentWorkspace.exists()) {
            setTitle("Vai - " + currentWorkspace.getAbsolutePath());
        } else {
            setTitle("Vai");
        }
    }

    public void showLLMErrorPopup(String errorMsg, Runnable retryAction) {
        int option = JOptionPane.showOptionDialog(
                this,
                errorMsg,
                "LLM Execution Error",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new Object[]{"Retry", "Cancel"},
                "Retry"
        );
        if (option == JOptionPane.YES_OPTION) {
            retryAction.run();
        }
    }

    private void openProjectDirectory() {
        File currentWorkspace = backend.getCurrentWorkspace();
        if (currentWorkspace != null && currentWorkspace.exists()) {
            try {
                Desktop.getDesktop().open(currentWorkspace);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Unable to open project directory.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, "No project directory is currently open.", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addSubmitShortcut(JButton submitButton) {
        KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlEnter, "submitAction");
        getRootPane().getActionMap().put("submitAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitButton.doClick();
            }
        });
    }

    public JComboBox<String> getModelList() {
        return modelCombo;
    }
    
    public void refreshFileViewer() {
        if (this.currentFile != null) {
            fileViewerPanel.displayFile(this.currentFile);
        }
    }

    public File getCurrentFile() {
        return this.currentFile;
    }
    private HelpOverlayFrame helpOverlayFrame;

    public void openChatDialog(boolean audioInput) {
        helpOverlayFrame = new HelpOverlayFrame();

        if (audioInput) {
            helpOverlayFrame.startAudioRecording();
        }

        helpOverlayFrame.setVisible(true);
    }

    public boolean isChatDialogClosed() {
        return helpOverlayFrame == null || !helpOverlayFrame.isVisible();
    }

    private void openWorkspaceMapperPanel() {
        if (App.getInstance().getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(this, "Please open a workspace first.", "No Workspace", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFrame frame = new JFrame("Manage Workspaces - " + App.getInstance().getCurrentWorkspace().getName());
        WorkspaceMapperPanel panel = new WorkspaceMapperPanel(); 
        frame.setContentPane(panel);
        frame.setSize(1000, 700); 
        frame.setLocationRelativeTo(this); 
        frame.setVisible(true);
    }

    private void handleCreatePlanAction() {
        if (ClientFrame.isModelRunning) {
            JOptionPane.showMessageDialog(this, "A model operation is already in progress. Please wait.", "Operation in Progress", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String currentTextFromMainArea = textArea.getText();
        List<SubWorkspace> availableLocalSubWorkspaces; // Renamed for clarity
        if (App.getInstance().getCurrentWorkspace() != null) {
            availableLocalSubWorkspaces = App.getInstance().getSubWorkspaces();
        } else {
            availableLocalSubWorkspaces = Collections.emptyList();
        }

        CreatePlanDialog planDialog = new CreatePlanDialog(ClientFrame.this, currentTextFromMainArea, availableLocalSubWorkspaces);
        planDialog.setVisible(true);

        if (planDialog.isSubmitted()) {
            String planText = planDialog.getPlanText(); 
            this.appendLLMPrompt("User Request: " + planText + "\n"); // Added newline
            if (planText != null && !planText.trim().isEmpty()) {
                String selectedModelName = (String) modelCombo.getSelectedItem();
                if (selectedModelName == null) {
                    JOptionPane.showMessageDialog(this, "Please select a model first.", "No Model Selected", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                List<String> selectedLocalSubworkspaceNames = planDialog.getSelectedLocalSubworkspaceNames();
                List<CreatePlanDialog.ExternalSubWorkspaceSelection> selectedExternalSubWorkspaces = planDialog.getSelectedExternalSubWorkspaces();
                
                Tasks tasks = new Tasks();
                boolean contextMapped = tasks.queryRepositoryMap(planText, selectedLocalSubworkspaceNames, selectedExternalSubWorkspaces);

                if (contextMapped) {
                    statusBarLabel.setText("Context files updated based on plan. Review and submit.");
                } else {
                    statusBarLabel.setText("Plan processed. Context may not have changed.");
                }
            } else {
                statusBarLabel.setText("Plan creation cancelled or plan was empty.");
            }
        }
    }


    public static boolean isModelRunning = false;
    public void submit(Runnable onComplete) {
        if (isModelRunning) {
            System.out.println("Ignoring submit -- model is currently running.");
            return;
        }
        this.submitButton.setEnabled(false);
        ClientFrame.isModelRunning = true; 

        new Thread(() -> {
            try {
                String model = (String) modelCombo.getSelectedItem();
                if (model == null) {
                    System.out.println("Must select a model");
                    return;
                }
                String prompt = textArea.getText();

                IModelProvider provider = backend.getLLMProvider(model);
                ReasoningEffort reasoningEffort = null;

                if (provider instanceof OpenAIClientBase) {
                    if (((OpenAIClientBase) provider).supportsReasoningEffort()) {
                        int sliderValue = reasoningSlider.getValue();
                        switch(sliderValue) {
                            case 0:
                                reasoningEffort = ReasoningEffort.LOW;
                                break;
                            case 2:
                                reasoningEffort = ReasoningEffort.HIGH;
                                break;
                            default:
                                reasoningEffort = ReasoningEffort.MEDIUM;
                        }
                    }
                    App.getInstance().setReasoningEffort(reasoningEffort);
                }

                final String finalModel = model;
                final String finalPrompt = prompt;
                Runnable retryAction = () -> App.getInstance().getLLM().submitRequest(finalModel, finalPrompt);

                App.getInstance().getLLM().submitRequest(model, prompt); 
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete); 
                }
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                String currentModel = (String) modelCombo.getSelectedItem(); 
                String currentPrompt = textArea.getText(); 
                Runnable actualRetryAction = () -> App.getInstance().getLLM().submitRequest(currentModel, currentPrompt);
                SwingUtilities.invokeLater(() -> showLLMErrorPopup("2 LLM Error: " + ex.getMessage(), actualRetryAction));

            } finally {
                 ClientFrame.isModelRunning = false; 
                 SwingUtilities.invokeLater(() -> this.submitButton.setEnabled(true)); 
            }
        }).start();
    }

    public void appendLLMPrompt(String s) {
        textArea.setText(textArea.getText() + s);
    }
}
