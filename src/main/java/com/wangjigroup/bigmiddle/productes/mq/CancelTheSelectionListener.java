package com.wangjigroup.bigmiddle.productes.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangjigroup.bigmiddle.product.topic.CancelTheSelectionTopic;
import com.wangjigroup.bigmiddle.productes.service.ProductIndexService;
import com.wangjigroup.common.user.context.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @Author: caizhh
 * @Date: Create in 18-9-17 下午12:01
 * @Description:
 */
@Component
public class CancelTheSelectionListener {

    private static Logger logger = LoggerFactory.getLogger(CancelTheSelectionListener.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ProductIndexService productIndexService;

    /**
     * 处理取消选品消息
     *
     * @param request
     */
    @KafkaListener(topics = CancelTheSelectionTopic.TOPIC)
    public void listen(String request) {
        logger.info("收到取消选品消息: " + request);
        UserContext.putUserInfo("mq", -1L);
        CancelTheSelectionTopic topic;
        try {
            topic = objectMapper.readValue(request, CancelTheSelectionTopic.class);

            int ret = productIndexService.updateProductIndex(topic);
            logger.info("处理取消选品消息: ret=" + ret);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
