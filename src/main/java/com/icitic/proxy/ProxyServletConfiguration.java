package com.icitic.proxy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.Servlet;

import org.omg.CORBA.OBJ_ADAPTER;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class ProxyServletConfiguration implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
    Environment env;

    public ProxyServletConfiguration() {
    }
    

    @Bean
    public Servlet createProxyServlet() {
        return new ProxyServlet();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final Resource proxyFilePath = resolver.getResource("proxy.json");
        try {
            if (!proxyFilePath.exists()) {
                throw new Exception("无法读取代理配置文件:" + proxyFilePath);
            }
            InputStream config = proxyFilePath.getInputStream();
            JSONArray proxyUrls = JSON.parseObject(config, JSONArray.class);
            for (Object proxyUrl : proxyUrls) {
                JSONObject url = (JSONObject)proxyUrl;
                String servletUrl = url.getString("servletUrl");
                String targetUrl = url.getString("targetUrl");
                String name = url.getString("name");
                BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(ServletRegistrationBean.class);
                beanDefinitionBuilder.addPropertyValue("name", "suit" + name);
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("forwardip",true);
                value.put("preserveCookies","true");
                //value.put("preserveHost","true");
                value.put("http.protocol.handle-redirects","true");
                value.put("targetUri",targetUrl);
                value.put("log","true");
                beanDefinitionBuilder.addPropertyValue("initParameters", value);
                Set<String> urlMappings = new HashSet<>();
                urlMappings.add(servletUrl);
                beanDefinitionBuilder.addPropertyValue("urlMappings", urlMappings);
                beanDefinitionBuilder.addPropertyValue("servlet", this.createProxyServlet());
                registry.registerBeanDefinition(name, beanDefinitionBuilder.getBeanDefinition());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }
}
