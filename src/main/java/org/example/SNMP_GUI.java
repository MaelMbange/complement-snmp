package org.example;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

public class SNMP_GUI extends JFrame{
    private JTree treeOID;
    private JPanel PanelInfoOID;
    private JPanel panelBase;
    private JLabel labelOID;
    private JTextPane labelVariable;
    private JTextField textFieldOID;
    private JButton rechercherButton;
    private JButton modifierButton;
    private JLabel labelErrorStatus;
    private JLabel labelErrorValue;
    private JCheckBox asynchroneCheckBox;
    private JButton refreshButton;
    private JTextField textFieldAdresseIP;
    private JButton validerButton;
    private JButton refreshGlobalButton;

    private TransportMapping transport;
    private CommunityTarget target;
    private Address targetAddress;
    private Snmp snmp;
    private PDU pdu;

    public SNMP_GUI(){
        super("SNMP");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        try {
            setContentPane(panelBase);
            initParameters();
            iniTree();
            setButtons();

            pack();
            setVisible(true);
        }
        catch (IOException e) {
           e.printStackTrace();
        }
    }

    private void initParameters() throws IOException {
        this.transport =  new DefaultUdpTransportMapping();
        this.transport.listen();

        this.target = new CommunityTarget();
        this.target.setVersion(SnmpConstants.version1);
        this.target.setCommunity(new OctetString("complement"));

        this.targetAddress = new UdpAddress("127.0.0.1/161");

        this.target.setAddress(this.targetAddress);
        this.target.setRetries(2);
        this.target.setTimeout(1500);

        this.snmp = new Snmp(this.transport);

        this.pdu = new PDU();
        this.pdu.add(new VariableBinding(new OID("0"))); // Commencer à partir de l'OID racine
        this.pdu.setType(PDU.GETNEXT);

        treeOID.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                try {
                    SynchroneRefresh();
                }
                catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void iniTree() throws IOException {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("MIB");

        this.pdu = new PDU();
        this.pdu.add(new VariableBinding(new OID("0"))); // Commencer à partir de l'OID racine
        this.pdu.setType(PDU.GETNEXT);
        OID previous = this.pdu.get(0).getOid();

        while(true){
            ResponseEvent reponse = this.snmp.send(this.pdu,this.target);
            PDU responsePDU = reponse.getResponse();

            if(responsePDU != null){
                VariableBinding variableBinding = responsePDU.get(0);
                if(variableBinding.getOid().compareTo(previous) <= 0){
                    break;
                }

                previous = variableBinding.getOid();
                this.pdu.clear();
                this.pdu.add(variableBinding);

                //System.out.println(variableBinding.getOid() + " : " + variableBinding.getVariable());
                addNode(root, variableBinding.getOid());
            }
        }
        this.treeOID.setModel(new DefaultTreeModel(root));
    }

    private void setButtons(){
        modifierButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Button pushed");
                try{
                    if(asynchroneCheckBox.isSelected()){
                        AsynchroneEdit();
                    }
                    else {
                        SynchroneEdit();
                    }
                } catch (IOException | InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        rechercherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Button pushed");
                try{
                    if(asynchroneCheckBox.isSelected()){
                        AsynchroneRechercher();
                    }
                    else {
                        SynchroneRechercher(textFieldOID.getText());
                    }
                } catch (IOException | InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Button pushed");
                try {
                    SynchroneRefresh();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        validerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                targetAddress = new UdpAddress(textFieldAdresseIP.getText() + "/161");
                target.setAddress(targetAddress);
                System.out.println("IP address set-up to:" + textFieldAdresseIP.getText() );
            }
        });

        refreshGlobalButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    iniTree();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void SynchroneRefresh() throws IOException {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeOID.getLastSelectedPathComponent();
        OID oid = null;

        if (node == null) return;
        if(!(node.getUserObject() instanceof OID)){
            if(labelOID.getText().equalsIgnoreCase("label")) return;
            else oid = new OID(labelOID.getText());
        }
        else
            oid = (OID) node.getUserObject();

        pdu.clear();
        pdu.add(new VariableBinding(oid));
        pdu.setType(PDU.GET);

        ResponseEvent reponse = snmp.send(pdu,target);
        PDU responsePDU = reponse.getResponse();

        if(responsePDU != null){
            VariableBinding variableBinding = responsePDU.get(0);
            labelOID.setText(variableBinding.getOid().toString());
            labelVariable.setText(variableBinding.getVariable().toString());
            labelErrorStatus.setText(responsePDU.getErrorStatusText());
            labelErrorValue.setText("[" + responsePDU.getErrorStatus() + "]");
        }
    }

    private void SynchroneRechercher(String oid) throws IOException {
        pdu.clear();
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GET);

        ResponseEvent reponse = snmp.send(pdu,target);
        PDU responsePDU = reponse.getResponse();

        if(responsePDU != null){
            VariableBinding variableBinding = responsePDU.get(0);
            labelOID.setText(variableBinding.getOid().toString());
            labelVariable.setText(variableBinding.getVariable().toString());
            labelErrorStatus.setText(responsePDU.getErrorStatusText());
            labelErrorValue.setText("[" + responsePDU.getErrorStatus() + "]");
        }
    }

    private void addNode(DefaultMutableTreeNode root, OID oid){
        DefaultMutableTreeNode node = root;

        for(String part: oid.toString().split("\\.")){
            DefaultMutableTreeNode child = findChild(node,part);
            if (child == null) {
                child = new DefaultMutableTreeNode(part);
                node.add(child);
            }
            node = child;
        }
        node.setUserObject(oid);
    }

    private DefaultMutableTreeNode findChild(DefaultMutableTreeNode node, String childName) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (childName.equals(child.getUserObject())) {
                return child;
            }
        }
        return null;
    }

    private void SynchroneEdit() throws IOException {
        System.out.println("SynchroneEdit");

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeOID.getLastSelectedPathComponent();
        OID oid = null;

        if (node == null) return;
        if(!(node.getUserObject() instanceof OID)){
            if(labelOID.getText().equalsIgnoreCase("label")) return;
            else oid = new OID(labelOID.getText());
        }
        else
            oid = (OID) node.getUserObject();

        pdu.setType(PDU.SET);
        pdu.clear();
        pdu.add(new VariableBinding(oid,new OctetString(labelVariable.getText())));

        ResponseEvent reponse = snmp.set(pdu,target);

        if(reponse != null){
            PDU reponsePDU = reponse.getResponse();
            System.out.println("Status réponse = " + reponsePDU.getErrorStatus());
            System.out.println("Status réponse = " + reponsePDU.getErrorStatusText());

            labelErrorStatus.setText(reponsePDU.getErrorStatusText());
            labelErrorValue.setText("[" + reponsePDU.getErrorStatus() + "]");

            List vecReponse = reponsePDU.getVariableBindings();
            for (int i=0; i<vecReponse.size(); i++)
            {
                System.out.println("Elément n°"+ i + " : "+ vecReponse.get(i));
            }
        }
    }

    private void AsynchroneEdit() throws IOException, InterruptedException {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeOID.getLastSelectedPathComponent();
        OID oid = null;

        if (node == null) return;
        if(!(node.getUserObject() instanceof OID)){
            if(labelOID.getText().equalsIgnoreCase("label")) return;
            else oid = new OID(labelOID.getText());
        }
        else
            oid = (OID) node.getUserObject();

        pdu.clear();
        pdu.add(new VariableBinding(new OID(oid), new OctetString(labelVariable.getText())));
        pdu.setType(PDU.SET);

        ResponseListener listener = new ResponseListener() {
            @Override
            public void onResponse(ResponseEvent event){
                ((Snmp)event.getSource()).cancel(event.getRequest(), this);
                System.out.println("Réponse reçue (PDU): "+event.getResponse());
                synchronized(snmp) {
                    snmp.notify();
                }
            }
        };

        snmp.send(pdu, target, null, listener);
        synchronized(snmp) {
            snmp.wait();
        }
    }

    private void AsynchroneRechercher() throws IOException, InterruptedException {
        pdu.clear();
        pdu.add(new VariableBinding(new OID(textFieldOID.getText())));
        pdu.setType(PDU.GET);

        ResponseListener listener = new ResponseListener() {
            @Override
            public void onResponse(ResponseEvent responseEvent) {
                ((Snmp)responseEvent.getSource()).cancel(responseEvent.getRequest(), this);
                System.out.println("Réponse reçue (PDU): "+responseEvent.getResponse());

                // Modifiez le JLabel sur l'EDT
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        PDU responsePDU = responseEvent.getResponse();

                        if(responsePDU != null){
                            VariableBinding variableBinding = responsePDU.get(0);
                            labelOID.setText(variableBinding.getOid().toString());
                            labelVariable.setText(variableBinding.getVariable().toString());
                            labelErrorStatus.setText(responsePDU.getErrorStatusText());
                            labelErrorValue.setText("[" + responsePDU.getErrorStatus() + "]");
                        }
                    }
                });

                synchronized(snmp) {
                    snmp.notify();
                }
            }
        };

        snmp.send(pdu, target, null, listener);
        synchronized(snmp)
        {
            snmp.wait();
        }
    }


    public static void main(String[] args){
        SNMP_GUI gui = new SNMP_GUI();
    }

}
