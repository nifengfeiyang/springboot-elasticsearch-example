package com.zhhongcai.example.productes.service;


import com.zhhongcai.example.productes.dto.Page;
import com.zhhongcai.example.productes.dto.ProductSearchReqDto;
import com.zhhongcai.example.productes.dto.ProductSearchRespDto;
import com.zhhongcai.example.productes.dto.SelectingSkuDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-17 下午5:01
 * @Description:
 */
public interface ProductSearchService {

    /**
     * 站内商品查询
     *
     * @param reqDto
     * @return
     * @throws IOException
     */
    Page<ProductSearchRespDto> search(ProductSearchReqDto reqDto) throws Exception;

    /**
     * 价格降幅数量统计
     *
     * @param reqDto
     * @return
     */
    Map<Integer, Long> priceDiscountPercentCount(ProductSearchReqDto reqDto) throws Exception;

    /**
     * sku选品列表
     *
     * @param productIds
     * @param shopId
     * @return
     * @throws IOException
     */
    List<SelectingSkuDto> listSelectingSku(List<Long> productIds, Long shopId) throws Exception;

    List<Long> searchProductId(ProductSearchReqDto request) throws Exception;
}
