package com.evolution.dropfile.installer;

import javax.swing.*;
import java.nio.file.Path;

public class Installer {
    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose installation directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            System.exit(0);
        }

        Path installDir = chooser.getSelectedFile().toPath();

        System.out.println(installDir);
    }
}
