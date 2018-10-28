package com.wangjigroup.bigmiddle.productes.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangjigroup.bigmiddle.product.topic.ReSelectProductTopic;
import com.wangjigroup.bigmiddle.productes.service.ProductIndexService;
import com.wangjigroup.common.user.context.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 选品消息
 *
 * @Author: caizhh
 * @Date: Create in 18-9-21 上午10:26
 * @Description:
 */
@Component
public class ReSelectProductListener {
    private static Logger logger = LoggerFactory.getLogger(ReSelectProductListener.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ProductIndexService productIndexService;

    /**
     * 处理选品消息
     *
     * @param request
     */
    @KafkaListener(topics = ReSelectProductTopic.TOPIC)
    public void listen(String request) {
        logger.info("收到选品消息: " + request);
        UserContext.putUserInfo("mq", -1L);
        ReSelectProductTopic topic;
        try {
            topic = objectMapper.readValue(request, ReSelectProductTopic.class);

            int ret = productIndexService.updateProductIndex(topic);
            logger.info("处理选品消息消息: ret=" + ret);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
