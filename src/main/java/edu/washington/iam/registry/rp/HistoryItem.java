package edu.washington.iam.registry.rp;

import java.util.*;
import java.text.DateFormat;

public class HistoryItem {


    private String effectiveDate;
    private List<ChangeItem> changes;

    private class ChangeItem
    {
        private String propertyName;
        private String oldValue;
        private String newValue;


        private ChangeItem(String propertyName, String oldValue, String newValue) {

            propertyName = null;
            oldValue = null;
            newValue = null;

            this.propertyName = propertyName;
            this.oldValue = oldValue;
            this.newValue = newValue;

        }
    }

    private void localInit () {
        effectiveDate = null;
        changes = new Vector();

    }

    //Element ele, String mdid, boolean edit, String updatedBy, String startTime, String endTime,
    //                         String uuid

    public HistoryItem (String effectiveDate) {

        localInit();

        this.effectiveDate = effectiveDate;

    }


    public void AddItem(String propertyName, String oldValue, String newValue){

        ChangeItem myItem = new ChangeItem(propertyName, oldValue, newValue);
        changes.add(myItem);

    }
}



