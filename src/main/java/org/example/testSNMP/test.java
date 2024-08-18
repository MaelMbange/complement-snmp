package org.example.testSNMP;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class test {

    public static void main(String[] args) throws IOException {
        TransportMapping transport =  new DefaultUdpTransportMapping();
        transport.listen();

        CommunityTarget target = new CommunityTarget();
        target.setVersion(SnmpConstants.version1);
        target.setCommunity(new OctetString("complement"));

        Address targetAddress = new UdpAddress("localhost/161");

        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);

        Snmp snmp = new Snmp(transport);

        // Créer une PDU
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID("0"))); // Commencer à partir de l'OID racine
        pdu.setType(PDU.GETNEXT);

        OID previousOid = pdu.get(0).getOid();

        // Continuer à envoyer des requêtes GETNEXT jusqu'à ce que nous atteignions la fin de l'arbre MIB
        while(true) {
            ResponseEvent response = snmp.send(pdu, target);
            PDU responsePDU = response.getResponse();

            if(responsePDU != null) {
                VariableBinding vb = responsePDU.get(0);
                if(vb.getOid().compareTo(previousOid) <= 0) {
                    break;
                }
                System.out.println(vb.getOid() + " : " + vb.getVariable());
                previousOid = vb.getOid();
                pdu.clear();
                pdu.add(vb);
            } else {
                break;
            }
        }

    }
}
