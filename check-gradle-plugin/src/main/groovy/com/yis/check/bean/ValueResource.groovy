package com.yis.check.bean


class ValueResource extends BaseResource {

    private String resName
    private String resValue
    private String filePath
    private int line

    ValueResource() {
        isValueType = true
    }

    String getFilePath() {
        return filePath
    }

    void setFilePath(String filePath) {
        this.filePath = filePath
    }

    String getResName() {
        return resName
    }

    void setResName(String resName) {
        this.resName = resName
    }

    String getResValue() {
        return resValue
    }

    void setResValue(String resValue) {
        this.resValue = resValue
    }

    String getUniqueId() {
        return "value@" + lastDirectory + "/" + resName
    }

    @Override
    String belongFilePath() {
        return filePath
    }

    int getLine() {
        return line
    }

    void setLine(int line) {
        this.line = line
    }

    @Override
    boolean compare(BaseResource obj) {
        if (obj instanceof ValueResource) {
            ValueResource target = (ValueResource) obj
            return this.getUniqueId() == target.getUniqueId() && this.getResValue() == target.getResValue()
        }
        return false
    }
}