package com.publiccms.logic.component.template;

import static com.publiccms.common.base.AbstractFreemarkerView.CONTEXT_SITE;
import static com.publiccms.common.base.AbstractFreemarkerView.exposeSite;
import static com.publiccms.common.constants.CommonConstants.getDefaultPageBreakTag;
import static com.publiccms.common.tools.ExtendUtils.getExtendMap;
import static com.publiccms.logic.component.site.SiteComponent.getFullFileName;
import static com.publiccms.logic.component.template.TemplateCacheComponent.CONTENT_CACHE;
import static com.sanluan.common.tools.FreeMarkerUtils.generateFileByFile;
import static com.sanluan.common.tools.FreeMarkerUtils.generateStringByFile;
import static com.sanluan.common.tools.FreeMarkerUtils.generateStringByString;
import static org.apache.commons.lang3.StringUtils.splitByWholeSeparator;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import com.publiccms.common.api.Cache;
import com.publiccms.common.base.AbstractTaskDirective;
import com.publiccms.common.base.AbstractTemplateDirective;
import com.publiccms.entities.cms.CmsCategory;
import com.publiccms.entities.cms.CmsCategoryAttribute;
import com.publiccms.entities.cms.CmsCategoryModel;
import com.publiccms.entities.cms.CmsCategoryModelId;
import com.publiccms.entities.cms.CmsContent;
import com.publiccms.entities.cms.CmsContentAttribute;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.logic.component.site.DirectiveComponent;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.service.cms.CmsCategoryAttributeService;
import com.publiccms.logic.service.cms.CmsCategoryModelService;
import com.publiccms.logic.service.cms.CmsCategoryService;
import com.publiccms.logic.service.cms.CmsContentAttributeService;
import com.publiccms.logic.service.cms.CmsContentService;
import com.publiccms.logic.service.cms.CmsPlaceService;
import com.publiccms.views.pojo.CmsPageMetadata;
import com.publiccms.views.pojo.CmsPlaceMetadata;
import com.sanluan.common.base.Base;
import com.sanluan.common.handler.PageHandler;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

/**
 * 
 * TemplateComponent 模板处理组件
 *
 */
public class TemplateComponent extends Base implements Cache {
    public static String INCLUDE_DIRECTORY = "include";

    private String directivePrefix;

    private Configuration adminConfiguration;
    private Configuration webConfiguration;
    private Configuration taskConfiguration;

    @Autowired
    private CmsContentAttributeService contentAttributeService;
    @Autowired
    private CmsCategoryAttributeService categoryAttributeService;
    @Autowired
    private CmsContentService contentService;
    @Autowired
    private CmsCategoryModelService categoryModelService;
    @Autowired
    private CmsCategoryService categoryService;
    @Autowired
    private SiteComponent siteComponent;
    @Autowired
    private MetadataComponent metadataComponent;
    @Autowired
    private CmsPlaceService placeService;

    /**
     * 创建静态化页面
     * 
     * @param templatePath
     * @param filePath
     * @param model
     * @return
     * @throws TemplateException
     * @throws IOException
     */
    public String createStaticFile(SysSite site, String templatePath, String filePath, Integer pageIndex,
            CmsPageMetadata metadata, Map<String, Object> model) throws IOException, TemplateException {
        if (notEmpty(filePath)) {
            if (null == model) {
                model = new HashMap<String, Object>();
            }
            if (null == metadata) {
                metadata = metadataComponent.getTemplateMetadata(siteComponent.getWebTemplateFilePath() + templatePath);
            }
            model.put("metadata", metadata);
            exposeSite(model, site);
            filePath = generateStringByString(filePath, webConfiguration, model);
            model.put("url", site.getSitePath() + filePath);
            if (notEmpty(pageIndex) && 1 < pageIndex) {
                int index = filePath.lastIndexOf('.');
                filePath = filePath.substring(0, index) + '_' + pageIndex + filePath.substring(index, filePath.length());
            }
            generateFileByFile(templatePath, siteComponent.getWebFilePath(site, filePath), webConfiguration, model);
        }
        return filePath;
    }

    /**
     * 内容页面静态化
     * 
     * @param entity
     * @param category
     * @param templatePath
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    public boolean createContentFile(SysSite site, CmsContent entity, CmsCategory category, CmsCategoryModel categoryModel) {
        if (null != site && null != entity) {
            if (null == category) {
                category = categoryService.getEntity(entity.getCategoryId());
            }
            if (null == categoryModel) {
                categoryModel = categoryModelService
                        .getEntity(new CmsCategoryModelId(entity.getCategoryId(), entity.getModelId()));
            }
            if (null != categoryModel && null != category && !entity.isOnlyUrl()) {
                try {
                    if (site.isUseStatic() && notEmpty(categoryModel.getTemplatePath())) {
                        String url = site.getSitePath() + createContentFile(site, entity, category, true,
                                getFullFileName(site, categoryModel.getTemplatePath()), null, null);
                        contentService.updateUrl(entity.getId(), url, true);
                    } else {
                        Map<String, Object> model = new HashMap<String, Object>();
                        model.put("content", entity);
                        model.put("category", category);
                        model.put(CONTEXT_SITE, site);
                        String url = site.getDynamicPath()
                                + generateStringByString(category.getContentPath(), webConfiguration, model);
                        contentService.updateUrl(entity.getId(), url, false);
                    }
                    return true;
                } catch (IOException | TemplateException e) {
                    log.error(e.getMessage());
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 内容页面静态化
     * 
     * @param entity
     * @param category
     * @param templatePath
     * @param filePath
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    public String createContentFile(SysSite site, CmsContent entity, CmsCategory category, boolean createMultiContentPage,
            String templatePath, String filePath, Integer pageIndex) throws IOException, TemplateException {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("content", entity);
        model.put("category", category);

        CmsContentAttribute attribute = contentAttributeService.getEntity(entity.getId());
        if (null != attribute) {
            Map<String, String> map = getExtendMap(attribute.getData());
            map.put("text", attribute.getText());
            map.put("source", attribute.getSource());
            map.put("sourceUrl", attribute.getSourceUrl());
            map.put("wordCount", String.valueOf(attribute.getWordCount()));
            model.put("attribute", map);
        } else {
            model.put("attribute", attribute);
        }
        if (empty(filePath)) {
            filePath = category.getContentPath();
        }

        if (null != attribute && notEmpty(attribute.getText())) {
            String[] texts = splitByWholeSeparator(attribute.getText(), getDefaultPageBreakTag());
            if (createMultiContentPage) {
                for (int i = 1; i < texts.length; i++) {
                    PageHandler page = new PageHandler(i + 1, 1, texts.length, null);
                    model.put("text", texts[i]);
                    model.put("page", page);
                    createStaticFile(site, templatePath, filePath, i + 1, null, model);
                }
                PageHandler page = new PageHandler(pageIndex, 1, texts.length, null);
                model.put("page", page);
                model.put("text", texts[page.getPageIndex() - 1]);
            } else {
                PageHandler page = new PageHandler(pageIndex, 1, texts.length, null);
                model.put("page", page);
                model.put("text", texts[page.getPageIndex() - 1]);
            }
        }
        return createStaticFile(site, templatePath, filePath, 1, null, model);
    }

    /**
     * 分类页面静态化
     * 
     * @param entity
     * @param templatePath
     * @param filePath
     * @param pageIndex
     * @param totalPage
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    public boolean createCategoryFile(SysSite site, CmsCategory entity, Integer pageIndex, Integer totalPage) {
        if (entity.isOnlyUrl()) {
            categoryService.updateUrl(entity.getId(), entity.getPath(), false);
        } else if (notEmpty(entity.getPath())) {
            try {
                if (site.isUseStatic() && notEmpty(entity.getTemplatePath())) {
                    String url = site.getSitePath() + createCategoryFile(site, entity,
                            getFullFileName(site, entity.getTemplatePath()), entity.getPath(), pageIndex, totalPage);
                    categoryService.updateUrl(entity.getId(), url, true);
                } else {
                    Map<String, Object> model = new HashMap<String, Object>();
                    model.put("category", entity);
                    model.put(CONTEXT_SITE, site);
                    String url = site.getDynamicPath() + generateStringByString(entity.getPath(), webConfiguration, model);
                    categoryService.updateUrl(entity.getId(), url, false);
                }
            } catch (IOException | TemplateException e) {
                log.error(e.getMessage());
                return false;
            }
            return true;
        }
        return false;

    }

    /**
     * 分类页面静态化
     * 
     * @param entity
     * @param templatePath
     * @param filePath
     * @param pageIndex
     * @param totalPage
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    public String createCategoryFile(SysSite site, CmsCategory entity, String templatePath, String filePath, Integer pageIndex,
            Integer totalPage) throws IOException, TemplateException {
        Map<String, Object> model = new HashMap<String, Object>();
        if (empty(pageIndex)) {
            pageIndex = 1;
        }
        model.put("category", entity);
        CmsCategoryAttribute attribute = categoryAttributeService.getEntity(entity.getId());
        if (null != attribute) {
            Map<String, String> map = getExtendMap(attribute.getData());
            map.put("title", attribute.getTitle());
            map.put("keywords", attribute.getKeywords());
            map.put("description", attribute.getDescription());
            model.put("attribute", map);
        } else {
            model.put("attribute", attribute);
        }

        if (notEmpty(totalPage) && pageIndex + 1 <= totalPage) {
            for (int i = pageIndex + 1; i <= totalPage; i++) {
                model.put("pageIndex", i);
                createStaticFile(site, templatePath, filePath, i, null, model);
            }
        }

        model.put("pageIndex", pageIndex);
        return createStaticFile(site, templatePath, filePath, 1, null, model);
    }

    private void exposePlace(SysSite site, String templatePath, CmsPlaceMetadata metadata, Map<String, Object> model) {
        int pageSize = 10;
        if (notEmpty(metadata.getSize())) {
            pageSize = metadata.getSize();
        }
        if (pageSize > 0) {
            model.put("page", placeService.getPage(site.getId(), null, templatePath, null, null, null, getDate(),
                    CmsPlaceService.STATUS_NORMAL, false, null, null, 1, pageSize));
        }
        model.put("metadata", metadata);
        exposeSite(model, site);
    }

    /**
     * 静态化页面片段
     * 
     * @param filePath
     * @return
     * @throws TemplateException
     * @throws IOException
     */
    public void staticPlace(SysSite site, String templatePath, CmsPlaceMetadata metadata) throws IOException, TemplateException {
        if (notEmpty(templatePath)) {
            Map<String, Object> model = new HashMap<String, Object>();
            exposePlace(site, templatePath, metadata, model);
            String placeTemplatePath = INCLUDE_DIRECTORY + templatePath;
            generateFileByFile(getFullFileName(site, placeTemplatePath), siteComponent.getWebFilePath(site, placeTemplatePath),
                    webConfiguration, model);
        }
    }

    /**
     * 输出页面片段
     * 
     * @param filePath
     * @return
     * @throws TemplateException
     * @throws IOException
     */
    public String printPlace(SysSite site, String templatePath, CmsPlaceMetadata metadata) throws IOException, TemplateException {
        StringWriter writer = new StringWriter();
        printPlace(writer, site, templatePath, metadata);
        return writer.toString();
    }

    /**
     * 输出页面片段
     * 
     * @param filePath
     * @return
     * @throws TemplateException
     * @throws IOException
     */
    public void printPlace(Writer writer, SysSite site, String templatePath, CmsPlaceMetadata metadata)
            throws IOException, TemplateException {
        if (notEmpty(templatePath)) {
            Map<String, Object> model = new HashMap<String, Object>();
            exposePlace(site, templatePath, metadata, model);
            generateStringByFile(writer, getFullFileName(site, INCLUDE_DIRECTORY + templatePath), webConfiguration, model);
        }
    }

    @Autowired
    private void init(FreeMarkerConfigurer freeMarkerConfigurer, DirectiveComponent directiveComponent)
            throws IOException, TemplateModelException {
        Map<String, Object> freemarkerVariables = new HashMap<String, Object>();
        adminConfiguration = freeMarkerConfigurer.getConfiguration();
        for (Entry<String, AbstractTemplateDirective> entry : directiveComponent.getTemplateDirectiveMap().entrySet()) {
            freemarkerVariables.put(directivePrefix + entry.getKey(), entry.getValue());
        }
        freemarkerVariables.putAll(directiveComponent.getMethodMap());
        adminConfiguration.setAllSharedVariables(new SimpleHash(freemarkerVariables, adminConfiguration.getObjectWrapper()));

        webConfiguration = new Configuration(Configuration.getVersion());
        File webFile = new File(siteComponent.getWebTemplateFilePath());
        webFile.mkdirs();
        webConfiguration.setDirectoryForTemplateLoading(webFile);
        copyConfig(adminConfiguration, webConfiguration);
        Map<String, Object> webFreemarkerVariables = new HashMap<String, Object>(freemarkerVariables);
        webFreemarkerVariables.put(CONTENT_CACHE, new NoCacheDirective());
        webConfiguration.setAllSharedVariables(new SimpleHash(webFreemarkerVariables, webConfiguration.getObjectWrapper()));

        taskConfiguration = new Configuration(Configuration.getVersion());
        File taskFile = new File(siteComponent.getTaskTemplateFilePath());
        taskFile.mkdirs();
        taskConfiguration.setDirectoryForTemplateLoading(taskFile);
        copyConfig(adminConfiguration, taskConfiguration);
        for (Entry<String, AbstractTaskDirective> entry : directiveComponent.getTaskDirectiveMap().entrySet()) {
            freemarkerVariables.put(directivePrefix + entry.getKey(), entry.getValue());
        }
        taskConfiguration.setAllSharedVariables(new SimpleHash(freemarkerVariables, taskConfiguration.getObjectWrapper()));
    }

    private void copyConfig(Configuration source, Configuration target) {
        target.setNewBuiltinClassResolver(source.getNewBuiltinClassResolver());
        target.setTemplateUpdateDelayMilliseconds(source.getTemplateUpdateDelayMilliseconds());
        target.setDefaultEncoding(source.getDefaultEncoding());
        target.setLocale(source.getLocale());
        target.setBooleanFormat(source.getBooleanFormat());
        target.setDateTimeFormat(source.getDateTimeFormat());
        target.setDateFormat(source.getDateFormat());
        target.setTimeFormat(source.getTimeFormat());
        target.setNumberFormat(source.getNumberFormat());
        target.setOutputFormat(source.getOutputFormat());
        target.setURLEscapingCharset(source.getURLEscapingCharset());
        target.setLazyAutoImports(source.getLazyAutoImports());
        target.setTemplateExceptionHandler(source.getTemplateExceptionHandler());
    }

    @Override
    public void clear() {
        adminConfiguration.clearTemplateCache();
        clearTemplateCache();
    }

    public void clearTemplateCache() {
        webConfiguration.clearTemplateCache();
        taskConfiguration.clearTemplateCache();
    }

    public Configuration getWebConfiguration() {
        return webConfiguration;
    }

    public Configuration getTaskConfiguration() {
        return taskConfiguration;
    }

    public Configuration getAdminConfiguration() {
        return adminConfiguration;
    }

    public void setDirectivePrefix(String directivePrefix) {
        this.directivePrefix = directivePrefix;
    }
}
