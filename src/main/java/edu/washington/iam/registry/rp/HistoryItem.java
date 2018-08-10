package edu.washington.iam.registry.rp;

import java.util.*;

public class HistoryItem {


    private String effectiveDate;
    private List<ChangeItem> changes;
    private String updatedBy;

    public class ChangeItem
    {


        private String objectName;
        private Object oldValue;
        private Object newValue;
        private int changeType;

        private void LocalInit(){

            objectName = null;
            oldValue = null;
            newValue = null;
            changeType = 0;
        }

        private ChangeItem(String objectName, Object oldValue, Object newValue, int changeType) {

            LocalInit();

            this.objectName = objectName;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.changeType = changeType;


        }

        public String getObjectName() {
            return this.objectName;
        }

        public Object getOldValue() {
            return this.oldValue;
        }

        public Object getNewValue() {
            return this.newValue;
        }

        public int getChangeType() {
            return this.changeType;
        }


    }

    private void localInit () {
        effectiveDate = null;
        changes = new Vector();
        updatedBy = "";

    }



    public HistoryItem (String effectiveDate, String updatedBy) {

        localInit();

        this.effectiveDate = effectiveDate;
        this.updatedBy = updatedBy;

    }

    //1 = object changed
    //add a new instance of something that changed
    public void AddChangeItem(String objectName, Object oldValue, Object newValue){

        ChangeItem myItem = new ChangeItem(objectName, oldValue, newValue, 1);
        changes.add(myItem);

    }
    //2 = object added (only new is populated)
    // something added that wasn't there before
    public void AddNewItem(String objectName, Object newValue){

        ChangeItem myItem = new ChangeItem(objectName, null, newValue, 2);
        changes.add(myItem);

    }
    //3 = object removed (only old is populated)
    //something completely removed
    public void AddDeleteItem(String objectName, Object oldValue){

        ChangeItem myItem = new ChangeItem(objectName, oldValue, null, 3);
        changes.add(myItem);

    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public List<ChangeItem> getChanges() {
        return changes;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }


}



