package com.wangjigroup.bigmiddle.productes.config;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-18 上午9:39
 * @Description:
 */
@Configuration
public class ESConfig {
    /**
     * es集群地址
     */
    @Value("${elasticsearch.ip}")
    private String hostNames;

    /**
     * 端口
     */
    @Value("${elasticsearch.port}")
    private Integer port;

    /**
     * 请求协议
     */
    @Value("${elasticsearch.rest.scheme}")
    private String scheme;

    /**
     * @return
     */
    @Bean
    public RestHighLevelClient client() {
        List<HttpHost> hosts = Lists.newArrayList();
        Splitter.on(",").split(hostNames).forEach(hostName -> hosts.add(new HttpHost(hostName, port, scheme)));

        return new RestHighLevelClient(RestClient.builder(hosts.toArray(new HttpHost[hosts.size()])).setDefaultHeaders(new Header[]{}));
    }

}
