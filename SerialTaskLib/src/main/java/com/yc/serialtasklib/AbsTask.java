package com.yc.serialtasklib;

public abstract class AbsTask implements IDispatchRunnable {

    public AbsTask() {

    }

    public abstract void onCancel();

    public String getCategory() {
        Class<? extends AbsTask> aClass = this.getClass();
        return aClass.getName();
    }
}
