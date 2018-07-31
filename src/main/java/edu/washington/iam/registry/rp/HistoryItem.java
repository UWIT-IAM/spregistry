package edu.washington.iam.registry.rp;

import java.util.*;
import java.text.DateFormat;

public class HistoryItem {


    private String effectiveDate;
    private List<ChangeItem> changes;

    private class ChangeItem
    {
        private String propertyName;
        private Object oldValue;
        private Object newValue;
        private int changeType;

        private void LocalInit(){

            propertyName = null;
            oldValue = null;
            newValue = null;

            changeType = 0;
        }

        private ChangeItem(String propertyName, Object oldValue, Object newValue, int changeType) {

            LocalInit();

            this.propertyName = propertyName;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.changeType = changeType;

        }
    }

    private void localInit () {
        effectiveDate = null;
        changes = new Vector();

    }



    public HistoryItem (String effectiveDate) {

        localInit();

        this.effectiveDate = effectiveDate;

    }


    //add a new instance of something that changed
    public void AddItem(String propertyName, Object oldValue, Object newValue){

        ChangeItem myItem = new ChangeItem(propertyName, oldValue, newValue, 1);
        changes.add(myItem);

    }


}



