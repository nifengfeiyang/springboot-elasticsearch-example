package com.wangjigroup.bigmiddle.productes.service;

import com.wangjigroup.bigmiddle.productes.dto.Page;
import com.wangjigroup.bigmiddle.productes.dto.ProductSearchReqDto;
import com.wangjigroup.bigmiddle.productes.dto.ProductSearchRespDto;
import com.wangjigroup.bigmiddle.productes.dto.SelectingSkuDto;

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
    Page<ProductSearchRespDto> search(ProductSearchReqDto reqDto) throws IOException;

    /**
     * 价格降幅数量统计
     *
     * @param discountPercentList
     * @return
     */
    Map<Integer, Long> priceDiscountPercentCount(List<Integer> discountPercentList) throws IOException;

    /**
     * sku选品列表
     *
     * @param productIds
     * @param shopId
     * @return
     * @throws IOException
     */
    List<SelectingSkuDto> listSelectingSku(List<Long> productIds, Long shopId) throws IOException;

    List<Long> searchProductId(ProductSearchReqDto request) throws IOException;
}
