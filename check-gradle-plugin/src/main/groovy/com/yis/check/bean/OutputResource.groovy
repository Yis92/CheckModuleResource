package com.yis.check.bean

class OutputResource {

    private String title
    private boolean expand = false

    private List<OutputResourceDetail> children

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

    List<OutputResourceDetail> getChildren() {
        return children
    }

    void setChildren(List<OutputResourceDetail> children) {
        this.children = children
    }
}