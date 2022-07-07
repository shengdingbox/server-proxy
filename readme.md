### Parameters

The following is a list of parameters that can be configured

+ log: 一个布尔参数名称，用于将输入和目标 URL 记录到 servlet 日志。
+ forwardip: 用于启用客户端 IP 转发的布尔参数名称
+ preserveHost: 保持 HOST 参数不变的布尔参数名称
+ preserveCookies: 保持 COOKIES 原样的布尔参数名称
+ http.protocol.handle-redirects: 具有自动处理重定向的布尔参数名称
+ http.socket.timeout: 一个整数参数名称，用于设置套接字连接超时（毫秒）
+ http.read.timeout: 一个整数参数名称，用于设置套接字读取超时（毫秒）
+ http.connectionrequest.timeout: 一个整数参数名，用于设置连接请求超时时间（毫秒）
+ http.maxConnections: 设置最大连接数的整数参数名称
+ useSystemProperties: 一个布尔参数，是否使用 JVM 定义的系统属性来配置各种网络方面。
+ targetUri: 要代理到的目标（目标）URI 的参数名称。