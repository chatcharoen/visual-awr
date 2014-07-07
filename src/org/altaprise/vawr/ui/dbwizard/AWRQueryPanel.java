package org.altaprise.vawr.ui.dbwizard;

import dai.server.dbService.SQLResolver;
import dai.server.dbService.dbconnect;

import dai.shared.businessObjs.DBRec;
import dai.shared.businessObjs.DBRecSet;

import daiBeans.daiComboBox;

import java.awt.Dimension;
import java.awt.LayoutManager;

import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import javax.swing.JTextField;

import org.altaprise.vawr.awrdata.AWRData;
import org.altaprise.vawr.awrdata.db.AWRCollectionSQL;
import org.altaprise.vawr.ui.RootFrame;

public class AWRQueryPanel extends WizardContentBasePanel {
    private JButton jButton_doQuery = new JButton("Query AWR Data");
    private JTextArea textArea_awrData = new JTextArea();
    private JScrollPane scrollPaneTextArea = new JScrollPane(textArea_awrData);
    private JTextField jTextField_connName = new JTextField();
    private JTextField jTextField_dbId = new JTextField();
    private JTextField jTextField_startSnapId = new JTextField();
    private JTextField jTextField_endSnapId = new JTextField();
    private JLabel jLabel_connName = new JLabel("Connection Name:");
    private JLabel jLabel_dbId = new JLabel("Databse ID:");
    private JLabel jLabel_startSnapId = new JLabel("Start Snapshot ID:");
    private JLabel jLabel_endSnapId = new JLabel("End Snapshot ID:");

    public AWRQueryPanel() {
        super();
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AWRQueryPanel(boolean b) {
        super(b);
    }

    public AWRQueryPanel(LayoutManager layoutManager) {
        super(layoutManager);
    }

    public AWRQueryPanel(LayoutManager layoutManager, boolean b) {
        super(layoutManager, b);
    }

    private void jbInit() throws Exception {
        this.setLayout(null);
        this.setSize(new Dimension(760, 429));

        jButton_doQuery.setBounds(new Rectangle(310, 20, 150, 20));
        jButton_doQuery.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    jButton_doQuery_actionPerformed(e);
                }
            });
        scrollPaneTextArea.setBounds(new Rectangle(15, 145, 500, 185));
        scrollPaneTextArea.setSize(new Dimension(500, 185));
        textArea_awrData.setEditable(false);
        jTextField_dbId.setBounds(new Rectangle(115, 50, 140, 20));
        jTextField_dbId.setEditable(false);
        jTextField_startSnapId.setBounds(new Rectangle(115, 80, 140, 20));
        jTextField_startSnapId.setEditable(false);
        jTextField_endSnapId.setBounds(new Rectangle(115, 110, 140, 20));
        jTextField_endSnapId.setEditable(false);
        jTextField_connName.setBounds(new Rectangle(115, 20, 140, 20));
        jTextField_connName.setEditable(false);
        jLabel_connName.setBounds(new Rectangle(10, 25, 100, 15));
        jLabel_dbId.setBounds(new Rectangle(10, 55, 65, 15));
        jLabel_startSnapId.setBounds(new Rectangle(10, 85, 95, 15));
        jLabel_endSnapId.setBounds(new Rectangle(10, 115, 90, 15));

        this.add(jLabel_endSnapId, null);
        this.add(jLabel_startSnapId, null);
        this.add(jLabel_dbId, null);
        this.add(jLabel_connName, null);
        this.add(jTextField_connName, null);
        this.add(jTextField_endSnapId, null);
        this.add(jTextField_startSnapId, null);
        this.add(jTextField_dbId, null);
        this.add(jButton_doQuery, null);
        this.add(scrollPaneTextArea, null);
    }

    protected void doNextOperation() {
        this.jTextField_dbId.setText(SelectDBIdPanel.getSelectedDBId());
        this.jTextField_startSnapId.setText(SnapIdSelectPanel.getStartSnapId());
        this.jTextField_endSnapId.setText(SnapIdSelectPanel.getEndSnapId());
        this.jTextField_connName.setText(DBConnectPanel.getDBConnectName());
    }

    private void doAWRQuery() {
        String textAreaStatus = "";
        //Update the Status in the Text Area
        textAreaStatus += "Running AWR Metrics Query....\n";
        this.textArea_awrData.setText(textAreaStatus);

        //First lets clear any previous AWRData
        AWRData.getInstance().clearAWRData();

        SQLResolver sqlResolver = new SQLResolver();
        try {
            long dbId = Long.parseLong(this.jTextField_dbId.getText());
            long startSnapId =
                Long.parseLong(this.jTextField_startSnapId.getText());
            long endSnapId =
                Long.parseLong(this.jTextField_endSnapId.getText());

            this.textArea_awrData.setText("");
            DBRecSet awrRecSetData =
                    sqlResolver.executeDynamicSQL(dbconnect.getInstance(), AWRCollectionSQL.getMainAWRMetricsSQL(dbId,
                                                                                        startSnapId,
                                                                                        endSnapId));
            //Update the Status in the Text Area
            textAreaStatus += "Parsing AWR Metrics....\n";
            this.textArea_awrData.setText(textAreaStatus);

            AWRData.getInstance().parseDataRecords(awrRecSetData);

            //Update the Status in the Text Area
            textAreaStatus += "Running AWR Memory Metrics Query....\n";
            this.textArea_awrData.setText(textAreaStatus);

            DBRecSet awrMemoryRecSetData =
                    sqlResolver.executeDynamicSQL(dbconnect.getInstance(), AWRCollectionSQL.getMemoryUtilizationSQL(dbId, startSnapId, endSnapId));
            //Update the Status in the Text Area
            textAreaStatus += "Parsing AWR Memory Metrics Query....\n";
            this.textArea_awrData.setText(textAreaStatus);

            AWRData.getInstance().parseMemoryDataRecords(awrMemoryRecSetData);

            //Set the Text Area to the AWR Metrics
            String awrDataTextString = AWRData.getInstance().getAWRDataTextString();
            this.textArea_awrData.setText(AWRData.getInstance().getAWRDataTextString());

        } catch (Exception ex) {
            daiBeans.daiDetailInfoDialog dialog =
                new daiBeans.daiDetailInfoDialog(null, "Error", true,
                                                 ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        this.textArea_awrData.setCaretPosition(0);
    }

    private void jButton_doQuery_actionPerformed(ActionEvent e) {
        if (jTextField_dbId.getText() == null || jTextField_dbId.getText().length() == 0) {
            JOptionPane.showMessageDialog(RootFrame.getFrameRef(),
                                          "Invalid Database ID",
                                          "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (jTextField_startSnapId.getText() == null || jTextField_startSnapId.getText().length() == 0) {
            JOptionPane.showMessageDialog(RootFrame.getFrameRef(),
                                          "Invalid Start Snapshot ID",
                                          "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (jTextField_endSnapId.getText() == null || jTextField_endSnapId.getText().length() == 0) {
            JOptionPane.showMessageDialog(RootFrame.getFrameRef(),
                                          "Invalid End Snapshot ID",
                                          "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        this.doAWRQuery();
    }
}
