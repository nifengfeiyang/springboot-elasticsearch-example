package com.zhhongcai.example.productes.service;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-24 下午7:19
 * @Description:
 */
public interface ProductIndexService {
    /**
     * 选品时更新索引
     *
     * @param topic
     * @return
     */
    int updateProductIndex(String topic) throws Exception;
}
