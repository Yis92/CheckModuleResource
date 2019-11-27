package com.yis.check.bean


class FileResource extends BaseResource {

    private String path
    private String md5
    private String fileName


    FileResource() {
        isValueType = false
    }

    String getPath() {
        return path
    }

    void setPath(String path) {
        this.path = path
    }

    String getMd5() {
        return md5
    }

    void setMd5(String md5) {
        this.md5 = md5
    }

    String getFileName() {
        return fileName
    }

    void setFileName(String fileName) {
        this.fileName = fileName
    }

    String getUniqueId() {
        return "file@" + lastDirectory + "/" + fileName
    }

    @Override
    String belongFilePath() {
        return path
    }

    @Override
    boolean compare(BaseResource obj) {
        if (obj instanceof FileResource) {
            FileResource target = (FileResource) obj
            if (this.getUniqueId() == target.getUniqueId()) {
                if (this.getMd5() == null && target.getMd5() == null) {
                    return true
                } else {
                    return this.getMd5() == target.getMd5()
                }
            }
        }
        return false
    }
}