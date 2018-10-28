package com.wangjigroup.bigmiddle.productes;

import com.wangjigroup.bigmiddle.product.topic.CancelTheSelectionTopic;
import com.wangjigroup.bigmiddle.product.topic.SelectProductTopic;
import com.wangjigroup.bigmiddle.productes.service.ProductIndexService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProductEsApplicationTests {

    @Resource
    private ProductIndexService productIndexService;

    @Test
    public void testUpdateProductIndex() throws IOException {
        CancelTheSelectionTopic topic = new CancelTheSelectionTopic();
        topic.setProductSkuId(190194210010598950L);
        topic.setProductId(25606375506934268L);
        topic.setShopId(111111L);
        int ret = productIndexService.updateProductIndex(topic);
        Assert.assertTrue(ret > 0);
    }

}
