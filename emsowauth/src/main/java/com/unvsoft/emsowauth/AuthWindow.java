/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unvsoft.emsowauth;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dcm4che.tool.common.CLIUtils;
import org.dcm4che.tool.unvscp.UnvSCP;
import org.dcm4che.tool.unvscp.data.CAuthWebResponse;
import org.dcm4che.tool.unvscp.media.UnvWebClient;
import org.ini4j.Ini;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class AuthWindow extends javax.swing.JDialog {

    private static ResourceBundle rb = ResourceBundle.getBundle("com.unvsoft.emsowauth.messages");
    private static Ini ini;

    private final class EmsowAuthenticator extends Thread {
        public EmsowAuthenticator() {
            this.setDaemon(true);
        }
        @Override
        public void run() {
            Component cmpInFocus = null;
            String username = tfUsername.getText().trim();
            String password = new String(pfPassword.getPassword());
            boolean save = chbSaveUsername.isSelected();

            try {
                for (Component c : AuthWindow.this.getContentPane().getComponents()) {
                    if (c.isFocusOwner()) {
                        cmpInFocus = c;
                    }
                    c.setEnabled(false);
                }

                if (username.isEmpty()) {
                    cmpInFocus = tfUsername;
                    throw new RuntimeException("Username cannot be empty");
                }
                if (password.isEmpty()) {
                    cmpInFocus = pfPassword;
                    throw new RuntimeException("Password cannot be empty");
                }

                authenticate(AuthWindow.this.url, username, password);

                ini.put("general", "~USERNAME", username);
                ini.put("general", "~PASSWORD", password);
                if (save) {
                    ini.put("general", "USERNAME", username);
                }

                try {
                    ini.store();
                } catch(IOException ioe) {
                    System.err.println(ioe);
                    ioe.printStackTrace();
                }

                System.out.println("User \"" + username + "\" is authorized.");
                System.exit(0);
            } catch(Exception e) {
                System.err.println(e);
                String errText = e.getMessage().replaceFirst("(?s);\\s*(<|\\{).*", "").replace(": ", ":\n");
                JOptionPane.showMessageDialog(rootPane, errText, "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                for (Component c : AuthWindow.this.getContentPane().getComponents()) {
                    c.setEnabled(true);
                }
                if (cmpInFocus != null) {
                    cmpInFocus.requestFocus();
                }
            }
        }

        private CAuthWebResponse authenticate(String url, String username, String password) throws IOException {
            UnvWebClient webClient = new UnvWebClient(url);
            webClient.setBasicParams(username, password, "C-ECHO", null);
            webClient.sendPostRequest();

            return webClient.parseJsonResponse(CAuthWebResponse.class);
        }
    }

    /**
     * Creates new form AuthWindow
     */
    private String url;
    public AuthWindow(String url, String username) {
        initComponents();
        // Window alignment - center screen
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point cp = ge.getCenterPoint();
        Dimension windowSize = getSize();
        setLocation(cp.x - windowSize.width / 2, cp.y - windowSize.height / 2);

        URL imgUrl = getClass().getClassLoader().getResource("com/unvsoft/emsowauth/resources/icon.png");
        if (imgUrl != null) {
            setIconImage(new ImageIcon(imgUrl).getImage());
        }

        this.url = url;
        if (username != null && !"".equals(username.trim())) {
            tfUsername.setText(username);
            pfPassword.grabFocus();
        }

        for (Component c : this.getContentPane().getComponents()) {
            c.addKeyListener(new KeyAdapter(){
                @Override
                public void keyPressed(KeyEvent evt) {
                    vkEscapePressed(evt);
                }
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblUsername = new javax.swing.JLabel();
        tfUsername = new javax.swing.JTextField();
        lblPassword = new javax.swing.JLabel();
        pfPassword = new javax.swing.JPasswordField();
        chbSaveUsername = new javax.swing.JCheckBox();
        bRun = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("EMSOW Bridge Authentication");
        setAlwaysOnTop(true);
        setName("emsowauth"); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });

        lblUsername.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblUsername.setText("EMSOW username:");

        tfUsername.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        tfUsername.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfUsernameActionPerformed(evt);
            }
        });
        tfUsername.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tfUsernameFocusGained(evt);
            }
        });

        lblPassword.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblPassword.setText("EMSOW password:");

        pfPassword.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        pfPassword.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pfPasswordActionPerformed(evt);
            }
        });
        pfPassword.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                pfPasswordFocusGained(evt);
            }
        });

        chbSaveUsername.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        chbSaveUsername.setText("Save username");
        chbSaveUsername.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                chbSaveUsernameKeyPressed(evt);
            }
        });

        bRun.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        bRun.setText("Run Bridge");
        bRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bRunActionPerformed(evt);
            }
        });
        bRun.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                bRunKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(chbSaveUsername)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(bRun, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(tfUsername)
                    .addComponent(lblUsername, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
                    .addComponent(pfPassword)
                    .addComponent(lblPassword, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(15, 15, 15))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lblUsername)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfUsername, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addComponent(lblPassword)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pfPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chbSaveUsername)
                    .addComponent(bRun))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tfUsernameFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfUsernameFocusGained
        tfUsername.selectAll();
    }//GEN-LAST:event_tfUsernameFocusGained

    private void pfPasswordFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_pfPasswordFocusGained
        pfPassword.selectAll();
    }//GEN-LAST:event_pfPasswordFocusGained

    private void bRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRunActionPerformed
        new EmsowAuthenticator().start();
    }//GEN-LAST:event_bRunActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        System.err.println("User cancelled");
        System.exit(1);
    }//GEN-LAST:event_formWindowClosed

    private void tfUsernameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfUsernameActionPerformed
        pfPassword.grabFocus();
    }//GEN-LAST:event_tfUsernameActionPerformed

    private void pfPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pfPasswordActionPerformed
        bRun.doClick();
    }//GEN-LAST:event_pfPasswordActionPerformed

    private void chbSaveUsernameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_chbSaveUsernameKeyPressed
        vkEnterPressed(evt);
    }//GEN-LAST:event_chbSaveUsernameKeyPressed

    private void bRunKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_bRunKeyPressed
        vkEnterPressed(evt);
    }//GEN-LAST:event_bRunKeyPressed

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        vkEscapePressed(evt);
    }//GEN-LAST:event_formKeyPressed

    private void vkEnterPressed(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            bRun.doClick();
        }
    }

    private void vkEscapePressed(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            this.dispose();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AuthWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AuthWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AuthWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AuthWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        final CommandLine cl;
        try {
            cl = parseCommandLine(args);
            ini = new Ini(new File(cl.getOptionValue("ini-file")));
            //Trim spaces between keys and values
            ini.getConfig().setStrictOperator(true);
        } catch (ParseException e) {
            System.err.println("emsowauth: " + e.getMessage());
            System.err.println(rb.getString("try"));
            System.exit(1);
            return;
        } catch (Exception e) {
            System.err.println("emsowauth: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
            return;
        }

        UnvSCP.setSslWhiteList(cl);
        UnvSCP.setEmsowSessionName(cl);

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AuthWindow(cl.getOptionValue("emsow-url"), cl.getOptionValue("username")).setVisible(true);
            }
        });
    }

    @SuppressWarnings({"static-access", "CallToThreadDumpStack"})
    public static CommandLine parseCommandLine(String[] args) throws ParseException{
        Options opts = new Options();
        CLIUtils.addCommonOptions(opts);
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("file")
                .withDescription(rb.getString("ini-file"))
                .withLongOpt("ini-file")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("url")
                .withDescription(rb.getString("emsow-url"))
                .withLongOpt("emsow-url")
                .create());
        opts.addOption(OptionBuilder
                .hasOptionalArg()
                .withArgName("domain1.name[, domain2.name]")
                .withDescription(rb.getString("ssl-whitelist"))
                .withLongOpt("ssl-whitelist")
                .create("w"));
        opts.addOption(OptionBuilder
                .hasArg()
                .withDescription(rb.getString("username"))
                .withLongOpt("username")
                .create("u"));
        opts.addOption(OptionBuilder
                .hasArg()
                .withDescription(rb.getString("emsow-session-name"))
                .withLongOpt("emsow-session-name")
                .create("s"));

        CommandLine cl = CLIUtils.parseComandLine(args, opts, rb, AuthWindow.class);
        if (!cl.hasOption("ini-file")) {
            throw new MissingOptionException(rb.getString("missing-ini"));
        }
        if (!cl.hasOption("emsow-url")) {
            throw new MissingOptionException(rb.getString("missing-emsow-url"));
        }
        return cl;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bRun;
    private javax.swing.JCheckBox chbSaveUsername;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JLabel lblUsername;
    private javax.swing.JPasswordField pfPassword;
    private javax.swing.JTextField tfUsername;
    // End of variables declaration//GEN-END:variables
}
