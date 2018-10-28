package com.wangjigroup.bigmiddle.productes.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangjigroup.bigmiddle.product.topic.SelectProductTopic;
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
 * @Date: Create in 18-9-13 下午2:54
 * @Description:
 */

@Component
public class SelectProductListener {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ProductIndexService productIndexService;

    @KafkaListener(topics = SelectProductTopic.TOPIC)
    public void listen(String request) {
        logger.info("收到选品消息{}", request);
        SelectProductTopic topic = null;
        try {
            UserContext.putUserInfo("mq", -1L);
            topic = objectMapper.readValue(request, SelectProductTopic.class);

            int ret = productIndexService.updateProductIndex(topic);
            logger.info("处理选品消息消息: ret=" + ret);
        } catch (IOException e) {
            logger.error("request=" + request + "处理选品消息异常:" + e.getMessage(), e);
        }
    }
}
