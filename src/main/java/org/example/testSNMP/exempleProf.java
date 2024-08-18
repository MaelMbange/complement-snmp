package org.example.testSNMP;

import org.example.testSNMP.classes.SnmpListener;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.logging.Logger;

public class exempleProf {

    public static void get() throws IOException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();

        CommunityTarget target = new CommunityTarget();
        target.setVersion(SnmpConstants.version1);
        target.setCommunity(new OctetString("complement"));

        Address targetAddress = new UdpAddress("127.0.0.1/161");

        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);

        Snmp snmp = new Snmp(transport);

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.1.4.0")));
        pdu.setType(PDU.GET);

        ResponseEvent reponse = snmp.get(pdu,target);

        if(reponse != null){
            PDU reponsePDU = reponse.getResponse();
            System.out.println("Status réponse = " + reponsePDU.getErrorStatus());
            System.out.println("Status réponse = " + reponsePDU.getErrorStatusText());

            List vecReponse = reponsePDU.getVariableBindings();
            for (int i=0; i<vecReponse.size(); i++)
            {
                System.out.println("Elément n°"+ i + " : "+ vecReponse.get(i));
            }
        }
    }

    public static void set() throws IOException {

        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();

        CommunityTarget target = new CommunityTarget();
        target.setVersion(SnmpConstants.version1);
        target.setCommunity(new OctetString("complement"));

        Address targetAddress = new UdpAddress("127.0.0.1/161");

        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);

        PDU pdu = new PDU();
        pdu.setType(PDU.SET);
        pdu.add(new VariableBinding(new OID(new int[]{1,3,6,1,2,1,1,4,0}),new OctetString("titi")));

        Snmp snmp = new Snmp(transport);
        ResponseEvent reponse = snmp.set(pdu,target);

        if(reponse != null){
            PDU reponsePDU = reponse.getResponse();
            System.out.println("Status réponse = " + reponsePDU.getErrorStatus());
            System.out.println("Status réponse = " + reponsePDU.getErrorStatusText());

            List vecReponse = reponsePDU.getVariableBindings();
            for (int i=0; i<vecReponse.size(); i++)
            {
                System.out.println("Elément n°"+ i + " : "+ vecReponse.get(i));
            }
        }

    }

    public static void Aget() throws IOException, InterruptedException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();

        Snmp snmp = new Snmp(transport);

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("complement"));

        Address targetAddress = GenericAddress.parse("udp:127.0.0.1/161");
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version1);

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(new int[] {1,3,6,1,2,1,1,4})));
        pdu.add(new VariableBinding(new OID(new int[] {1,3,6,1,2,1,1,5})));
        pdu.setType(PDU.GETNEXT);

        SnmpListener listener = new SnmpListener(snmp);
        snmp.send(pdu, target, null, listener);
        synchronized(snmp)
        {
            snmp.wait();
        }

        /*ResponseListener listener = new ResponseListener() {
            @Override
            public void onResponse(ResponseEvent responseEvent) {
                ((Snmp)responseEvent.getSource()).cancel(responseEvent.getRequest(), this);
                System.out.println("Réponse reçue (PDU): "+responseEvent.getResponse());

                // Modifiez le JLabel sur l'EDT
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        label.setText("Réponse reçue (PDU): "+responseEvent.getResponse());
                    }
                });

                synchronized(snmpManager) {
                    snmpManager.notify();
                }
            }
        };*/
    }

    public static void Aset() throws IOException, InterruptedException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();

        Snmp snmp = new Snmp(transport);

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("complement"));

        Address targetAddress = GenericAddress.parse("udp:127.0.0.1/161");
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version1);

        PDU pdu = new PDU();
        // Remplacez "newvalue1" et "newvalue2" par les valeurs que vous voulez définir
        pdu.add(new VariableBinding(new OID(new int[] {1,3,6,1,2,1,1,4,0}), new OctetString("titi")));
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

    public static void main(String[] args) throws IOException, InterruptedException {
        //set();
        get();
        //Aget();
        //Aset();
    }
}
