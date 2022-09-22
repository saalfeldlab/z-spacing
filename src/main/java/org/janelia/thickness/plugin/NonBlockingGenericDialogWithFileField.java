/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2022 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.janelia.thickness.plugin;

import fiji.util.gui.GenericDialogPlus;
import ij.gui.NonBlockingGenericDialog;
import ij.io.OpenDialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class NonBlockingGenericDialogWithFileField extends NonBlockingGenericDialog {
    public NonBlockingGenericDialogWithFileField(String title) {
        super(title);
    }


    public void addFileField(String label, String defaultPath) {
        this.addFileField(label, defaultPath, 20);
    }

    public void addFileField(String label, String defaultPath, int columns) {
        this.addStringField(label, defaultPath, columns);
            TextField text = (TextField)this.stringField.lastElement();
            GridBagLayout layout = (GridBagLayout)this.getLayout();
            GridBagConstraints constraints = layout.getConstraints(text);
            Button button = new Button("Browse...");
            FileListener listener = new FileListener("Browse for " + label, text);
            button.addActionListener(listener);
            button.addKeyListener(this);
            Panel panel = new Panel();
            panel.setLayout(new FlowLayout(0, 0, 0));
            panel.add(text);
            panel.add(button);
            layout.setConstraints(panel, constraints);
            this.add(panel);
    }

    private static class FileListener implements ActionListener {
        String title;
        TextField text;

        public FileListener(String title, TextField text) {
            this.title = title;
            this.text = text;
        }

        public void actionPerformed(ActionEvent e) {
            String fileName = null;
            File dir = new File(this.text.getText());
            if (!dir.isDirectory()) {
                if (dir.exists()) {
                    fileName = dir.getName();
                }

                dir = dir.getParentFile();
            }

            while(dir != null && !dir.exists()) {
                dir = dir.getParentFile();
            }

            OpenDialog dialog;
            if (dir == null) {
                dialog = new OpenDialog(this.title, fileName);
            } else {
                dialog = new OpenDialog(this.title, dir.getAbsolutePath(), fileName);
            }

            String directory = dialog.getDirectory();
            if (directory != null) {
                fileName = dialog.getFileName();
                this.text.setText(directory + File.separator + fileName);
            }
        }
    }
}
