package com.zhhongcai.example.productes;

import com.zhhongcai.example.productes.service.ProductIndexService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProductEsApplicationTests {

    @Resource
    private ProductIndexService productIndexService;

    @Test
    public void testUpdateProductIndex() throws Exception {
        int ret = productIndexService.updateProductIndex("");
        Assert.assertTrue(ret > 0);
    }

}
