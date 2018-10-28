package com.zhhongcai.example.productes.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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

    @KafkaListener(topics = "test")
    public void listen(String request) {
        logger.info("收到消息{}", request);
        logger.info("处理消息消息=" + request);
    }
}
