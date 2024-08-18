package org.example.testSNMP.classes;

import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.Address;

public class SnmpListener implements ResponseListener
{
    private Snmp snmpManager;
    public SnmpListener (Snmp s){
        snmpManager = s;
    }


    @Override
    public void onResponse(ResponseEvent responseEvent) {
        ((Snmp)responseEvent.getSource()).cancel(responseEvent.getRequest(), this);
        System.out.println("Réponse reçue (PDU): "+responseEvent.getResponse());
        synchronized(snmpManager)
        {
            snmpManager.notify();
        }
    }
}
