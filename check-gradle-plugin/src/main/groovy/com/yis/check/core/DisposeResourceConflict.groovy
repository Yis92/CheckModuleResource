package com.yis.check.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yis.check.bean.BaseResource
import com.yis.check.bean.FileResource
import com.yis.check.bean.OutputResource
import com.yis.check.bean.OutputResourceDetail
import com.yis.check.bean.ValueResource
import com.yis.check.utils.MD5Util
import com.yis.check.utils.UrlUtil
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.jdom2.Attribute
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import org.jdom2.located.LocatedElement
import org.jdom2.located.LocatedJDOMFactory

import java.nio.charset.Charset

/**
 * 资源文件冲突处理
 */
class DisposeResourceConflict implements IResourceConflict {

    /**
     * 存放所有的资源文件，会用这个容器中的数据判断是否有资源冲突
     */
    private Map<String, BaseResource> mResourceMap = new HashMap<>()

    /**
     * 存放所有冲突的资源文件
     * key：文件夹名+文件名称
     * value ：冲突的资源文件信息
     */
    private Map<String, List<BaseResource>> mConflictResourceMap = new HashMap<>()

    /**
     * 遍历所有的资源文件
     * @param file
     */
    void traverseResources(File file) {

        if (file.isDirectory()) {
            for (File f in file.listFiles()) {
                traverseResources(f)
            }
        } else {
            // 判断是值类型资源还是文件资源
            boolean isValueType = isValueResource(file)
            if (isValueType) {
                findAndRecordValueResource(file)
            } else {
                findAndRecordFileResource(file)
            }
        }
    }

    /**
     * 是否是值类型的资源
     * @param file
     * @return
     */
    private boolean isValueResource(File file) {
        if (file.parentFile.name == "values" || file.parentFile.name.startsWith("values-")) {
            return true
        }
        return false
    }

    /**
     * 查找所有的 value 类型资源文件
     * @param file
     * @return
     */
    private BaseResource findAndRecordValueResource(File file) {
        String lastDirectory = file.parentFile.name
        String filePath = file.path
        // 构造器
        SAXBuilder saxBuilder = new SAXBuilder()
        saxBuilder.setJDOMFactory(new LocatedJDOMFactory())
        // 获取文档
        Document document = saxBuilder.build(file)
        // 得到根元素: resources
        Element element = document.getRootElement()

        if (element != null) {
            List<Element> children = element.getChildren()
            for (Element item : children) {
                // 得到资源名称 key
                String resName = item.getAttributeValue("name")
                String resValue
                if (item.children == null || item.children.size() == 0) {
                    // 得到资源名称 value
                    resValue = item.getValue()
                } else {
                    resValue = getValueFromElement(item.children)
                }

                ValueResource resource = new ValueResource()
                resource.setResName(resName)
                resource.setResValue(resValue)
                resource.setLastDirectory(lastDirectory)
                resource.setFilePath(filePath)
                if (item instanceof LocatedElement) {
                    resource.setLine(((LocatedElement) item).getLine())
                }
                recordResource(resource)
            }
        }
    }


    private String getValueFromElement(List<Element> elementList) {
        JsonArray jsonArray = new JsonArray()
        for (Element element : elementList) {
            JsonObject jsonObject = new JsonObject()
            jsonObject.addProperty("_name", element.getName())
            List<Attribute> attributes = element.getAttributes()
            JsonArray attributesJsonArray = new JsonArray()
            for (Attribute attribute : attributes) {
                JsonObject attributeObj = new JsonObject()
                attributeObj.addProperty("name", attribute.getName())
                attributeObj.addProperty("value", attribute.getValue())
                attributesJsonArray.add(attributeObj)
            }
            jsonObject.add("_attributes", attributesJsonArray)
            jsonObject.addProperty("_value", element.getValue())
            jsonArray.add(jsonObject)
        }
        return jsonArray.toString()
    }

    /**
     * 获取所有file类型资源文件
     * @param file
     */
    private void findAndRecordFileResource(File file) {
        FileResource resource = new FileResource()
        resource.setPath(file.path)
        resource.setLastDirectory(file.parentFile.name)
        resource.setFileName(file.name)
        resource.setMd5(MD5Util.getMD5(file))
        recordResource(resource)
    }

    /**
     * 记录冲突文件
     * @param resource
     */
    private void recordResource(BaseResource resource) {
        // 如果包含了，那么把相同资源方法一个Map中
        def uniqueId = resource.getUniqueId()
        if (mResourceMap.containsKey(uniqueId)) {
            BaseResource oldOne = mResourceMap.get(uniqueId)

            if (oldOne != null && !oldOne.compare(resource)) {
                List<BaseResource> resources = mConflictResourceMap.get(uniqueId)
                if (resources == null) {
                    resources = new ArrayList<BaseResource>()
                    resources.add(oldOne)
                }
                resources.add(resource)
                mConflictResourceMap.put(uniqueId, resources)
            }
        }
        mResourceMap.put(uniqueId, resource)
    }

    /**
     * 将冲突 HTML 文件拷贝到项目指定路径下
     * @param buildDir
     * @param variantName
     * @return
     */
    private File copyHtmlTemplateToBuildDir(File buildDir, String variantName) {
        File resultHtmlFile = new File(
                buildDir.path + "/" + "outputs" + "/" + "resource_check_result" + "/" + variantName + "_index.html")
        InputStream inputStream = this.getClass().
                getResourceAsStream("/templates/check_resource_conflict_result.html")
        FileUtils.copyInputStreamToFile(inputStream, resultHtmlFile)
        return resultHtmlFile
    }

    private String pretifyName(String content, int targetSize) {
        int size = content.size()
        if (size < targetSize) {
            content += " "
            for (int i = 0; i < targetSize - size; i++) {
                content += "-"
            }
        }
        return content
    }

    @Override
    String disposeConflictResource(File resultFileDir, String variantName) {

        // 打印出所有冲突的资源
        Iterator<Map.Entry<String, List<BaseResource>>> iterator = mConflictResourceMap.
                entrySet().
                iterator()

        List<OutputResource> fileResourceList = new ArrayList<>()
        List<OutputResource> valueResourceList = new ArrayList<>()

        // 把 html 复制到 build 文件夹下
        File resultFile = copyHtmlTemplateToBuildDir(resultFileDir, variantName)

        while (iterator.hasNext()) {
            boolean isValueType
            Map.Entry<String, List<BaseResource>> entry = iterator.next()
            List<BaseResource> valueList = entry.getValue()

            OutputResource outputResource = new OutputResource()
            List<OutputResourceDetail> outputResourceDetailList = new ArrayList<>()

            for (BaseResource value : valueList) {
                String  uniqueId = value.getUniqueId()
                OutputResourceDetail outputResourceDetail = new OutputResourceDetail()
                def resource
                if (value.isValueType()) {
                    isValueType = true
                    resource = (ValueResource) value
                    if (outputResource.getTitle() == null) {
                        outputResource.setTitle(resource.getResName() + " (数量：" + valueList.size() + ")，(id = " + uniqueId + ")")
                    }
                } else {
                    isValueType = false
                    resource = (FileResource) value
                    if (outputResource.getTitle() == null) {
                        outputResource.setTitle(resource.getFileName() + " (数量：" + valueList.size() + ")，(id = " + uniqueId + ")")
                    }
                }
                String modulePath = resource.belongFilePath()
                String relatedFileName = modulePath
                if (modulePath.contains("/")) {
                    relatedFileName =
                            modulePath.substring(modulePath.lastIndexOf("/") + 1)
                }
                if (isValueType) {
                    ValueResource valueResource = (ValueResource) value
                    relatedFileName =
                            relatedFileName + "(Line: " + valueResource.getLine() + ")"
                }
                outputResourceDetail.setTitle(
                        pretifyName(relatedFileName, 50) + "-> " + modulePath)
                outputResourceDetailList.add(outputResourceDetail)
            }
            outputResource.setChildren(outputResourceDetailList)

            if (isValueType) {
                valueResourceList.add(outputResource)
            } else {
                fileResourceList.add(outputResource)
            }
        }

        Gson gson = new GsonBuilder().disableHtmlEscaping().create()

        String template = FileUtils.readFileToString(resultFile,
                Charset.forName("UTF-8"))
        template = template.replaceAll("#File_Resouce_Conflicts#",
                gson.toJson(fileResourceList))
        template = template.replaceAll("#Value_Resouce_Conflicts#",
                gson.toJson(valueResourceList))
        FileUtils.write(resultFile, template, Charset.forName("UTF-8"))

        // 调用浏览器打开M页FileUtils
        UrlUtil.browse("file://$resultFile.path")

        return resultFile
    }
}