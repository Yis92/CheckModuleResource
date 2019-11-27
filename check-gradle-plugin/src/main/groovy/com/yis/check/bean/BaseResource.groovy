package com.yis.check.bean


abstract class BaseResource {

    protected boolean isValueType
    protected String lastDirectory

    String getLastDirectory() {
        return lastDirectory
    }

    void setLastDirectory(String lastDirectory) {
        this.lastDirectory = lastDirectory
    }

    boolean isValueType() {
        return isValueType
    }

    abstract String getUniqueId()

    abstract String belongFilePath()

    abstract boolean compare(BaseResource obj)
}