package com.yis.check.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.yis.check.core.DisposeResourceConflict
import com.yis.check.core.IResourceConflict
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 检测资源冲突
 */
class CheckModuleResourceConflictPlugin implements Plugin<Project> {

    private IResourceConflict mResourceConflict

    @Override
    void apply(Project project) {

        mResourceConflict = new DisposeResourceConflict()

        boolean isLibrary = project.plugins.hasPlugin("com.android.library")
        def variants = isLibrary ?
                ((LibraryExtension) (poject.property("android"))).libraryVariants :
                ((AppExtension) (project.property("android"))).applicationVariants

        project.afterEvaluate {
            variants.forEach { BaseVariantImpl variant ->

                def thisTaskName = "checkResource${variant.name.capitalize()}"
                def thisTask = project.task(thisTaskName)
                thisTask.group = "check"

                thisTask.doLast {

                    long startTime = System.currentTimeMillis()

                    println("开始执行 CheckResource 任务：" + thisTaskName)

                    // 所有资源文件
                    def files = variant.allRawAndroidResources.files

                    files.forEach {
                        file -> mResourceConflict.traverseResources(file)
                    }

                    String resultFile = mResourceConflict.disposeConflictResource(project.rootDir, variant.name)

                    long cost = System.currentTimeMillis() - startTime

                    println("资源冲突检查完毕，耗时 " + cost + " ms，请查看输出文件 $resultFile")
                }
            }
        }
    }
}