package com.zhhongcai.example.productes.controller;

import com.zhhongcai.example.productes.dto.AjaxRes;
import com.zhhongcai.example.productes.dto.ProductSearchReqDto;
import com.zhhongcai.example.productes.service.ProductSearchService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-17 下午4:55
 * @Description:
 */
@RestController
@RequestMapping("/productes")
public class ProductESController extends BaseController {

    @Resource
    private ProductSearchService productSearchService;

    @PostMapping("/search")
    @ApiOperation("商品查询")
    public AjaxRes search(@ModelAttribute ProductSearchReqDto request) throws Exception {
        if (request == null) {
            return new AjaxRes(AjaxRes.FAIL, "请求参数不能为空");
        }
        return new AjaxRes(productSearchService.search(request));
    }

    @PostMapping("/searchProductId")
    @ApiOperation("查询商品id")
    public AjaxRes searchProductId(@RequestBody ProductSearchReqDto request) throws Exception {
        if (request == null) {
            return  new AjaxRes(AjaxRes.FAIL, "请求参数不能为空");
        }
        try {
            return  new AjaxRes(productSearchService.searchProductId(request));
        } catch (Exception e) {
            logger.error("searchProductId ERROR: " + e.getMessage(), e);
        }
        return new AjaxRes();
    }

    @GetMapping("/priceDiscountPercentCount")
    @ApiOperation("价格降幅数量统计")
    public AjaxRes priceDiscountPercentCount(@ApiParam(value = "百分比列表: [0,5,10,20,30,40] ", name = "percentList", required = true, allowMultiple = true)
                                             @RequestParam(name = "percentList") List<Integer> percentList) throws Exception {
        if (CollectionUtils.isEmpty(percentList)) {
            return new AjaxRes(AjaxRes.FAIL, "请求参数不能为空");
        }
        return new AjaxRes(productSearchService.priceDiscountPercentCount(percentList));
    }

    @GetMapping("/listSelectingSku")
    @ApiOperation("sku查询")
    public AjaxRes listSelectingSku(@ApiParam(value = "产品id", name = "productIds", required = true, allowMultiple = true)
                                    @RequestParam(name = "productIds") List<Long> productIds,
                                    @ApiParam(value = "店铺id", name = "shopId", required = true)
                                    @RequestParam(name = "shopId") Long shopId) throws Exception {
        if (CollectionUtils.isEmpty(productIds)) {
            return new AjaxRes(AjaxRes.FAIL, "请求参数不能为空");
        }
        if (shopId == null) {
            return new AjaxRes(AjaxRes.FAIL, "请求参数店铺id不能为空");
        }
        return new AjaxRes(productSearchService.listSelectingSku(productIds, shopId));
    }
}
