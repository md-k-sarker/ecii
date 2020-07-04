package org.dase.ecii.ui;
/*
Written by sarker.
Written at 10/4/18.
*/

import org.dase.ecii.Main;
import org.dase.ecii.util.ConfigParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

public class App {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Path configFilePath;
    private JFileChooser jFileChooser;
    private JPanel jPanelUpper;
    private JPanel jPanelMiddle;
    private JPanel jPanelLower;
    private JFrame frame;
    private JProgressBar jProgressBar;
    private JButton btnRun;
    private JButton btnSelectConfigFile;
    private JButton btnOpenResultFile;
    private JLabel lblSelectConfigFile;
    private JLabel lblStatus;
    private String initialPath;
    private JTextField txtFConfigFile;
    private JTextPane txtPStatus;
    private CustomActionListener customActionListener;

    private void init() {
        initialPath = System.getProperty("user.home") + "/Desktop";
        customActionListener = new CustomActionListener();
    }

    private void initGui() {

        frame = new JFrame();
        frame.setSize(1000, 600);
        frame.setTitle("Efficient Concept Induction");
        frame.setPreferredSize(new Dimension(1000, 600));
        frame.setResizable(true);

        jPanelUpper = new JPanel();
        jPanelUpper.setLayout(new FlowLayout());
        jPanelMiddle = new JPanel();
        jPanelMiddle.setLayout(new BorderLayout());
        jPanelLower = new JPanel();
        jPanelLower.setLayout(new BoxLayout(jPanelLower, BoxLayout.Y_AXIS));
        jPanelLower.setAlignmentX(Component.CENTER_ALIGNMENT);
        jPanelLower.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));


        // Positive
        //lblPos = new JLabel("Positive");
        //lblPos.setLocation(50, 50);
        //jPanel.add(lblPos);

        //JText

        lblSelectConfigFile = new JLabel("Configuration file Path");
        //lblSelectConfigFile.setSize(new Dimension(50, 20));
        //lblSelectConfigFile.setPreferredSize(new Dimension(50, 20));
        jPanelUpper.add(lblSelectConfigFile, FlowLayout.LEFT);

        txtFConfigFile = new JTextField();
        txtFConfigFile.setSize(500, 20);
        txtFConfigFile.setPreferredSize(new Dimension(500, 20));
        jPanelUpper.add(txtFConfigFile, FlowLayout.CENTER);

        // btn
        btnSelectConfigFile = new JButton("Select");
        //btnSelectConfigFile.setSize(new Dimension(50, 20));
        //btnSelectConfigFile.setPreferredSize(new Dimension(50, 20));
        btnSelectConfigFile.addActionListener(customActionListener);
        jPanelUpper.add(btnSelectConfigFile, FlowLayout.RIGHT);

        // btn
        btnRun = new JButton("Run");
        btnRun.addActionListener(new CustomActionListener());
        jPanelMiddle.add(btnRun, BorderLayout.CENTER);
        jPanelMiddle.setSize(new Dimension(450, 60));
        jPanelMiddle.setPreferredSize(new Dimension(450, 60));
        jPanelMiddle.setMaximumSize(new Dimension(450, 60));


        lblStatus = new JLabel("Status");
        //lblStatus.setSize(new Dimension(50, 20));
        //lblStatus.setPreferredSize(new Dimension(50, 20));
        jPanelLower.add(lblStatus);

        txtPStatus = new JTextPane();
        txtPStatus.setAutoscrolls(true);
//        DefaultCaret caret = (DefaultCaret) txtPStatus.getCaret(); // ←
//        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        //txtPStatus.setEditable(false);
        //txtPStatus.setSize(300, 200);
        //txtPStatus.setPreferredSize(new Dimension(300, 200));
        //JScrollPane scrollPane = new JScrollPane(txtPStatus);
//        scrollPane.setVerticalScrollBarPolicy(
//                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        jPanelLower.add(txtPStatus);


        btnOpenResultFile = new JButton("Open Result File");
        btnOpenResultFile.setPreferredSize(new Dimension(50, 20));
        btnOpenResultFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnOpenResultFile.addActionListener(customActionListener);
        jPanelLower.add(btnOpenResultFile);


        // layout
        frame.add(jPanelUpper);
        frame.add(jPanelMiddle);
        frame.add(jPanelLower);
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // config File chooser
        jFileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Configuration File", "config");
        jFileChooser.setFileFilter(filter);
        jFileChooser.setCurrentDirectory(new File(initialPath));
    }

    private class CustomActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == btnSelectConfigFile) {

                int retVal = jFileChooser.showOpenDialog(frame);
                if (retVal == JFileChooser.APPROVE_OPTION) {
                    configFilePath = jFileChooser.getSelectedFile().toPath();
                    txtFConfigFile.setText(configFilePath.toString());
                    logger.info("ConfFilePath: " + configFilePath);
                }
            } else if (e.getSource() == btnRun) {

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        String[] strings = new String[1];
                        strings[0] = txtFConfigFile.getText();
                        try {
                            Main.setTextPane(txtPStatus);
                            Main.main(strings);
                        } catch (Exception ex) {

                        }
                    }
                };

                runnable.run();

            } else if (e.getSource() == btnOpenResultFile) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().edit(new File(ConfigParams.outputResultPath));
                    } catch (IOException e1) {
                        //e1.printStackTrace();
                    }
                }
            }
        }
    }


    public static void main(String[] args) {
        App app = new App();
        app.init();
        app.initGui();
    }
}
