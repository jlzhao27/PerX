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
package perx;

import java.awt.Component;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import mapservice.MapService;
import mapservice.WorkRoot;
import mapservice.SlaveNode;
import net.jxta.endpoint.EndpointService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

/**
 * This frame collects and displays connectivity information from a peergroup.
 */
public class PerX_GUI extends JFrame implements Runnable {

    // Static
    public static final ScheduledExecutorService TheExecutor = Executors.newScheduledThreadPool(5);
    // Attributes
    private final JFrame ThisFrame;
    private PerX perx;
    private PeerGroup ThePeerGroup = null;
    private Future TheMonitorFuture = null;
    private DefaultTableModel LocalRDVs_TM, LocalJobToDo_TM, RemoteJobDone_TM;
    private ListSelectionModel selectionModel;
    private String[] LocalRDV_Col = {"Connected Peers"};
    private String[] Local_Jobs_Col = {"Local Jobs"};
    private String[] Remote_Jobs_Col = {"Remote Jobs"};
    private static final String[][] EmptyTableContent = new String[0][1];

    /**
     * Creates new form ConnectivityMonitor
     */
    public PerX_GUI(PerX perx) {
        this.perx = perx;
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
        ThePeerGroup = perx.getPeerGroup();

        // Setting own default table models
        LocalRDVs_TM = new DefaultTableModel(EmptyTableContent, LocalRDV_Col);
        this.LocalRDVTable.setModel(LocalRDVs_TM);

        LocalJobToDo_TM = new DefaultTableModel(EmptyTableContent, Local_Jobs_Col);
        selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.JobsToDo.setModel(LocalJobToDo_TM);
        this.JobsToDo.setSelectionModel(selectionModel);

        RemoteJobDone_TM = new DefaultTableModel(EmptyTableContent, Remote_Jobs_Col);
        this.JobsDone.setModel(RemoteJobDone_TM);

        this.progressBar.setStringPainted(true);
        this.progressBar.setMaximum(100);
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
        jScrollPane3 = new javax.swing.JScrollPane();
        LocalRDVTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        JobsDone = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        JobsToDo = new javax.swing.JTable();
        progressBar = new javax.swing.JProgressBar();
        submitWork = new javax.swing.JButton();
        cancel = new javax.swing.JButton();

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
        jScrollPane3.setViewportView(LocalRDVTable);
        LocalRDVTable.getColumnModel().getColumn(0).setResizable(false);

        JobsDone.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        JobsDone.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null}
            },
            new String [] {
                "Remote Jobs"
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
        jScrollPane2.setViewportView(JobsDone);
        JobsDone.getColumnModel().getColumn(0).setResizable(false);

        javax.swing.GroupLayout StatusPaneLayout = new javax.swing.GroupLayout(StatusPane);
        StatusPane.setLayout(StatusPaneLayout);
        StatusPaneLayout.setHorizontalGroup(
            StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StatusPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(StatusPaneLayout.createSequentialGroup()
                        .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(StatusPaneLayout.createSequentialGroup()
                                .addComponent(PeerIDLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(PeerNameTextField))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StatusPaneLayout.createSequentialGroup()
                                .addComponent(PeerGroupIDLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(PeerGroupNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
                            .addComponent(PeerGroupIDTextField)
                            .addComponent(PeerIDTextField))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE)
                .addContainerGap())
        );

        JobsToDo.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        JobsToDo.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null}
            },
            new String [] {
                "Local Jobs"
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
        jScrollPane4.setViewportView(JobsToDo);
        JobsToDo.getColumnModel().getColumn(0).setResizable(false);

        submitWork.setText("Submit");
        submitWork.setActionCommand("jbutton1");
        submitWork.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitWorkActionPerformed(evt);
            }
        });

        cancel.setText("Cancel");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(progressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addComponent(submitWork)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cancel)
                .addGap(67, 67, 67))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(submitWork)
                    .addComponent(cancel))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(StatusPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(StatusPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    private void submitWorkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitWorkActionPerformed
        this.perx.getMapService().submitWork();
    }//GEN-LAST:event_submitWorkActionPerformed

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
    private javax.swing.JTable JobsDone;
    private javax.swing.JTable JobsToDo;
    private javax.swing.JTable LocalRDVTable;
    private javax.swing.JLabel PeerGroupIDLabel;
    private javax.swing.JTextField PeerGroupIDTextField;
    private javax.swing.JTextField PeerGroupNameTextField;
    private javax.swing.JLabel PeerIDLabel;
    private javax.swing.JTextField PeerIDTextField;
    private javax.swing.JTextField PeerNameTextField;
    private javax.swing.JPanel StatusPane;
    private javax.swing.JButton cancel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton submitWork;
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
        Collections.sort(inNewContent);
        String[][] NewContent = new String[inNewContent.size()][1];
        for (int i = 0; i < inNewContent.size(); i++) {
            NewContent[i][0] = inNewContent.get(i);
        }
        inTM.setDataVector(NewContent, inColumns);
    }

    private void updateSelection(DefaultTableModel tm, ListSelectionModel model, List<String> selection) {
        model.clearSelection();
        for (int i = 0; i < tm.getRowCount(); ++i) {
            if (selection.contains((String) tm.getValueAt(i, 0))) {
                model.addSelectionInterval(i, i);
            }
        }
    }

    public void run() {

        this.setTitle("PerX - "
                + ThePeerGroup.getPeerName() + " - "
                + ThePeerGroup.getPeerID().toString());

        this.PeerNameTextField.setText(ThePeerGroup.getPeerName());
        this.PeerIDTextField.setText(ThePeerGroup.getPeerID().toString());

        this.PeerGroupNameTextField.setText(ThePeerGroup.getPeerGroupName());
        this.PeerGroupIDTextField.setText(ThePeerGroup.getPeerGroupID().toString());
        MapService mapService = this.perx.getMapService();
    
        if (mapService != null) {
             List<String> Items = mapService.getFinder().getPeers(100000);

            // Sorting Peer IDs
            List<String> StrItems = new ArrayList<String>();
            for (int i = 0; i < Items.size(); i++) {
                StrItems.add(Items.get(i));
            }
            updateTableContent(LocalRDVs_TM, StrItems, LocalRDV_Col);
            
            
            double tot = 0, cur = 0;
            List<WorkRoot> items = mapService.getLocalJobs();
            if (items.size() > 0) {
            List<String> jobs = new LinkedList<String>();
            List<String> pending = new LinkedList<String>();
            for (WorkRoot m : items) {
                tot += m.getTotalJobs();
                cur += m.getJobsDone();
                jobs.addAll(m.getWorkRemaining());
                pending.addAll(m.getPendingWork());
            }
            updateTableContent(LocalJobToDo_TM, jobs, Local_Jobs_Col);
            updateSelection(LocalJobToDo_TM, selectionModel, pending);
            this.progressBar.setValue((int) ((cur / tot) * 100));
            }
            List<String> remote = new LinkedList<String>();
            for (String s : mapService.getRemoteJobs()) {
                remote.add("Processing file from: " + s);
            }
            updateTableContent(RemoteJobDone_TM, remote, Remote_Jobs_Col);          
        }

    }
}
