package com.wangjigroup.bigmiddle.productes.service;

import com.wangjigroup.bigmiddle.product.topic.CancelTheSelectionTopic;
import com.wangjigroup.bigmiddle.product.topic.ReSelectProductTopic;
import com.wangjigroup.bigmiddle.product.topic.SelectProductTopic;

import java.io.IOException;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-24 下午7:19
 * @Description:
 */
public interface ProductIndexService {

    /**
     * 取消选品时更新索引
     *
     * @param topic
     * @return
     */
    int updateProductIndex(CancelTheSelectionTopic topic) throws IOException;

    /**
     * 重新选品时更新索引
     *
     * @param topic
     * @return
     */
    int updateProductIndex(ReSelectProductTopic topic) throws IOException;

    /**
     * 选品时更新索引
     *
     * @param topic
     * @return
     */
    int updateProductIndex(SelectProductTopic topic) throws IOException;
}
