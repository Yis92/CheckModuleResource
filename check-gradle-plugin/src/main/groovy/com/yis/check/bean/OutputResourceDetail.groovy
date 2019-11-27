package com.yis.check.bean

class OutputResourceDetail {

    private String title
    private boolean expand = true

    String getTitle() {
        return title
    }

    void setTitle(String title) {
        this.title = title
    }

    boolean isExpand() {
        return expand
    }

    void setExpand(boolean expand) {
        this.expand = expand
    }
}