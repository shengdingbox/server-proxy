package com.icitic.proxy;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 一个代理 servlet，其中目标 URI 是从传入的请求参数模板化的。格式遵循URI 模板 RFC ，“级别 1”。例子：
 targetUri = http://{host}:{port}/{path}
 --具有模板变量。传入的请求必须包含这些名称的查询参数。当请求发送到目标时，它们会被删除。

 */
@SuppressWarnings({"serial"})
public class URITemplateProxyServlet extends ProxyServlet {

    /** Rich:
     * 有一些语法允许代理 arg 是“可选的”可能是一个很好的补充，也就是说，如果不存在就不会失败，
     * 只返回空字符串或给定的默认值。但是我在规范中看不到任何支持这种结构的东西。
     * 从理论上讲，{?host:google.com} 可能会返回 URL 参数“?hostProxyArg=somehost.com”的值（如果已定义），
     * 但如果未定义，则返回“google.com”。同样，{?host} 可以返回 hostProxyArg 的值或空字符串（如果不存在）。
     * 但这不是规范的工作方式。因此，如果为此代理 URL 定义，现在我们将需要一个代理 arg。
     */
    protected static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{(.+?)\\}");
    private static final String ATTR_QUERY_STRING =
            URITemplateProxyServlet.class.getSimpleName() + ".queryString";
    //has {name} parts
    protected String targetUriTemplate;


    @Override
    protected void initTarget() throws ServletException {
        targetUriTemplate = getConfigParam(P_TARGET_URI);
        if (targetUriTemplate == null) {
            throw new ServletException(P_TARGET_URI+" is required.");
        }
        //leave this.target* null to prevent accidental mis-use
    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException {

        //First collect params
        /*
         * 不要使用 servletRequest.getParameter(arg)
         * 因为这通常会读取和使用 servlet InputStream（我们的表单数据存储在其中以用于 POST）。
         * 稍后我们需要 InputStream。所以我们将自己解析查询字符串。
         * 另一个好处是我们可以将代理参数保留在查询字符串中，
         * 而不必将它们添加到 URL 编码的表单附件中。
         */
        String requestQueryString = servletRequest.getQueryString();
        String queryString = "";
        if (requestQueryString != null) {
            queryString = "?" + requestQueryString;//no "?" but might have "#"
        }
        int hash = queryString.indexOf('#');
        if (hash >= 0) {
            queryString = queryString.substring(0, hash);
        }
        List<NameValuePair> pairs;
        try {
            //note: HttpClient 4.2 lets you parse the string without building the URI
            pairs = URLEncodedUtils.parse(new URI(queryString), "UTF-8");
        } catch (URISyntaxException e) {
            throw new ServletException("Unexpected URI parsing error on " + queryString, e);
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        for (NameValuePair pair : pairs) {
            params.put(pair.getName(), pair.getValue());
        }

        //Now rewrite the URL
        StringBuffer urlBuf = new StringBuffer();//note: StringBuilder isn't supported by Matcher
        Matcher matcher = TEMPLATE_PATTERN.matcher(targetUriTemplate);
        while (matcher.find()) {
            String arg = matcher.group(1);
            String replacement = params.remove(arg);//note we remove
            if (replacement == null) {
                throw new ServletException("Missing HTTP parameter "+arg+" to fill the template");
            }
            matcher.appendReplacement(urlBuf, replacement);
        }
        matcher.appendTail(urlBuf);
        String newTargetUri = urlBuf.toString();
        servletRequest.setAttribute(ATTR_TARGET_URI, newTargetUri);
        URI targetUriObj;
        try {
            targetUriObj = new URI(newTargetUri);
        } catch (Exception e) {
            throw new ServletException("Rewritten targetUri is invalid: " + newTargetUri,e);
        }
        servletRequest.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(targetUriObj));

        //Determine the new query string based on removing the used names
        StringBuilder newQueryBuf = new StringBuilder(queryString.length());
        for (Map.Entry<String, String> nameVal : params.entrySet()) {
            if (newQueryBuf.length() > 0) {
                newQueryBuf.append('&');
            }
            newQueryBuf.append(nameVal.getKey()).append('=');
            if (nameVal.getValue() != null) {
                newQueryBuf.append( URLEncoder.encode(nameVal.getValue(), "UTF-8"));
            }
        }
        servletRequest.setAttribute(ATTR_QUERY_STRING, newQueryBuf.toString());

        super.service(servletRequest, servletResponse);
    }

    @Override
    protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
        return (String) servletRequest.getAttribute(ATTR_QUERY_STRING);
    }
}