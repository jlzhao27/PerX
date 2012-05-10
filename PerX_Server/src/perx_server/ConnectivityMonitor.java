/*
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *
 *  The Sun Project JXTA(TM) Software License
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The end-user documentation included with the redistribution, if any, must
 *     include the following acknowledgment: "This product includes software
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology."
 *     Alternately, this acknowledgment may appear in the software itself, if
 *     and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *     not be used to endorse or promote products derived from this software
 *     without prior written permission. For written permission, please contact
 *     Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA", nor may
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United
 *  States and other countries.
 *
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of
 *  the license in source files.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many individuals
 *  on behalf of Project JXTA. For more information on Project JXTA, please see
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 */
package perx_server;

import java.awt.Component;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import net.jxta.endpoint.EndpointService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

/**
 * This frame collects and displays connectivity information from a peergroup.
 */
public class ConnectivityMonitor extends JFrame implements Runnable {

    // Static
    public static final ScheduledExecutorService TheExecutor = Executors.newScheduledThreadPool(5);
    // Attributes
    private final JFrame ThisFrame;
    private PeerGroup ThePeerGroup = null;
    private Future TheMonitorFuture = null;
    private DefaultTableModel LocalRDVs_TM = null;
    private String[] LocalRDV_Col = {"Connected Peers"};
    private static final String[][] EmptyTableContent = new String[0][1];


    /**
     * Creates new form ConnectivityMonitor
     */
    public ConnectivityMonitor(PeerGroup inGroup) {

        // Registering this JFrame
        ThisFrame = this;

        // JFrame initialization
        initComponents();

        // Displaying the frame on the awt queue
        SetDefaultLookAndFeel();
        putScreenAtTheCenter(this);

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                ThisFrame.setVisible(true);
            }
        });

        // Initialization
        ThePeerGroup = inGroup;

        // Setting own default table models
        LocalRDVs_TM = new DefaultTableModel(EmptyTableContent, LocalRDV_Col);
        this.LocalRDVTable.setModel(LocalRDVs_TM);

        TheMonitorFuture = TheExecutor.scheduleWithFixedDelay(this, 0, 1, TimeUnit.SECONDS);

    }

    public static void SetDefaultLookAndFeel() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException Ex) {
            System.err.println(Ex.toString());
        } catch (InstantiationException Ex) {
            System.err.println(Ex.toString());
        } catch (IllegalAccessException Ex) {
            System.err.println(Ex.toString());
        } catch (UnsupportedLookAndFeelException Ex) {
            System.err.println(Ex.toString());

        }

    }

    public static void putScreenAtTheCenter(Component TheComponent) {

        // Retrieving horizontal value
        int WidthPosition = (Toolkit.getDefaultToolkit().getScreenSize().width
                - TheComponent.getWidth()) / 2;

        int HeightPosition = (Toolkit.getDefaultToolkit().getScreenSize().height
                - TheComponent.getHeight()) / 2;

        TheComponent.setLocation(WidthPosition, HeightPosition);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        StatusPane = new javax.swing.JPanel();
        PeerIDLabel = new javax.swing.JLabel();
        PeerNameTextField = new javax.swing.JTextField();
        PeerGroupIDLabel = new javax.swing.JLabel();
        PeerGroupNameTextField = new javax.swing.JTextField();
        PeerIDTextField = new javax.swing.JTextField();
        PeerGroupIDTextField = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        LocalRDVTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        PeerIDLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        PeerIDLabel.setText("Peer");

        PeerNameTextField.setEditable(false);
        PeerNameTextField.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        PeerNameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PeerNameTextFieldActionPerformed(evt);
            }
        });

        PeerGroupIDLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        PeerGroupIDLabel.setText("Peer Group");

        PeerGroupNameTextField.setEditable(false);
        PeerGroupNameTextField.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N

        PeerIDTextField.setEditable(false);
        PeerIDTextField.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N

        PeerGroupIDTextField.setEditable(false);
        PeerGroupIDTextField.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N

        LocalRDVTable.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        LocalRDVTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null}
            },
            new String [] {
                "Connected Peers"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(LocalRDVTable);
        LocalRDVTable.getColumnModel().getColumn(0).setResizable(false);

        javax.swing.GroupLayout StatusPaneLayout = new javax.swing.GroupLayout(StatusPane);
        StatusPane.setLayout(StatusPaneLayout);
        StatusPaneLayout.setHorizontalGroup(
            StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StatusPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(PeerGroupIDTextField)
                    .addComponent(PeerIDTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(StatusPaneLayout.createSequentialGroup()
                        .addComponent(PeerIDLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(PeerNameTextField))
                    .addGroup(StatusPaneLayout.createSequentialGroup()
                        .addComponent(PeerGroupIDLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(PeerGroupNameTextField))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE))
                .addContainerGap())
        );
        StatusPaneLayout.setVerticalGroup(
            StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StatusPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PeerIDLabel)
                    .addComponent(PeerNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PeerIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PeerGroupIDLabel)
                    .addComponent(PeerGroupNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PeerGroupIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(StatusPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(StatusPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed

        // Stopping monitor task
        stopMonitorTask();

    }//GEN-LAST:event_formWindowClosed

    private void PeerNameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PeerNameTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_PeerNameTextFieldActionPerformed

    private synchronized void stopMonitorTask() {

        if (TheMonitorFuture != null) {
            TheMonitorFuture.cancel(false);
        }

    }

    @Override
    protected void finalize() {

        // Stopping monitor task
        stopMonitorTask();

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable LocalRDVTable;
    private javax.swing.JLabel PeerGroupIDLabel;
    private javax.swing.JTextField PeerGroupIDTextField;
    private javax.swing.JTextField PeerGroupNameTextField;
    private javax.swing.JLabel PeerIDLabel;
    private javax.swing.JTextField PeerIDTextField;
    private javax.swing.JTextField PeerNameTextField;
    private javax.swing.JPanel StatusPane;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables

    private void updateTableContent(DefaultTableModel inTM, List<String> inNewContent, String[] inColumns) {

        // Do we have the same number of elements
        if (inTM.getRowCount() == inNewContent.size()) {

            // Sorting new candidates
            Collections.sort(inNewContent);

            // Replacing items that have to be replaced
            for (int i = 0; i < inNewContent.size(); i++) {

                if (inNewContent.get(i).compareTo((String) inTM.getValueAt(i, 0)) != 0) {
                    inTM.setValueAt(inNewContent.get(i), i, 0);
                }

            }

            // Done
            return;

        }

        // We need a new data vector
        String[][] NewContent = new String[inNewContent.size()][1];
        for (int i = 0; i < inNewContent.size(); i++) {
            NewContent[i][0] = inNewContent.get(i);
        }
        inTM.setDataVector(NewContent, inColumns);

    }

    public void run() {

        this.setTitle("Connectivity Monitor - "
                + ThePeerGroup.getPeerName() + " - "
                + ThePeerGroup.getPeerID().toString());

        this.PeerNameTextField.setText(ThePeerGroup.getPeerName());
        this.PeerIDTextField.setText(ThePeerGroup.getPeerID().toString());

        this.PeerGroupNameTextField.setText(ThePeerGroup.getPeerGroupName());
        this.PeerGroupIDTextField.setText(ThePeerGroup.getPeerGroupID().toString());

        RendezVousService TmpRDVS = this.ThePeerGroup.getRendezVousService();
        if (TmpRDVS != null) {

            List<PeerID> Items = TmpRDVS.getLocalRendezVousView();

            // Sorting Peer IDs
            List<String> StrItems = new ArrayList<String>();
            for (int i = 0; i < Items.size(); i++) {
                StrItems.add(Items.get(i).toString());

            }

            updateTableContent(LocalRDVs_TM, StrItems, LocalRDV_Col);

        }
    }
}
