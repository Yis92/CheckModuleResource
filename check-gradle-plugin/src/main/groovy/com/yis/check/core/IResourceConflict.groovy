package com.yis.check.core

interface IResourceConflict {

    /**
     * 遍历所有的资源文件
     * @param file
     */
    void traverseResources(File file)

    /**
     * 处理冲突资源
     * @param project
     * @return
     */
    String disposeConflictResource(File file,String variantName)
}