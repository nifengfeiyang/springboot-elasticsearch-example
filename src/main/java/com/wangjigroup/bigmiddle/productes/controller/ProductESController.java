package com.wangjigroup.bigmiddle.productes.controller;

import com.wangjigroup.bigmiddle.productes.dto.AjaxRes;
import com.wangjigroup.bigmiddle.productes.dto.ProductSearchReqDto;
import com.wangjigroup.bigmiddle.productes.service.ProductSearchService;
import com.wangjigroup.common.service.Response;
import com.wangjigroup.common.service.ResponseCode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
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
    @ApiOperation("站内商品查询")
    public AjaxRes search(@ModelAttribute ProductSearchReqDto request) throws IOException {
        if (request == null) {
            return new AjaxRes(AjaxRes.FAIL, "请求参数不能为空");
        }
        return new AjaxRes(productSearchService.search(request));
    }

    @PostMapping("/searchProductId")
    @ApiOperation("品类选品查询商品id")
    public Response<List<Long>> searchProductId(@RequestBody ProductSearchReqDto request) throws IOException {
        Response response = new Response();
        if (request == null) {
            response.setCode(ResponseCode.FAIL);
            response.setMsg("请求参数不能为空");
            return response;
        }
        try {
            response.setData(productSearchService.searchProductId(request));
        } catch (Exception e) {
            logger.error("searchProductId ERROR: " + e.getMessage(), e);
            response.setCode(ResponseCode.FAIL);
            response.setMsg(e.getMessage());
        }
        return response;
    }

    @GetMapping("/priceDiscountPercentCount")
    @ApiOperation("价格降幅数量统计")
    public AjaxRes priceDiscountPercentCount(@ApiParam(value = "百分比列表: [0,5,10,20,30,40] ", name = "percentList", required = true, allowMultiple = true)
                                             @RequestParam(name = "percentList") List<Integer> percentList) throws IOException {
        if (CollectionUtils.isEmpty(percentList)) {
            return new AjaxRes(AjaxRes.FAIL, "请求参数不能为空");
        }
        return new AjaxRes(productSearchService.priceDiscountPercentCount(percentList));
    }

    @GetMapping("/listSelectingSku")
    @ApiOperation("sku选品")
    public AjaxRes listSelectingSku(@ApiParam(value = "产品id", name = "productIds", required = true, allowMultiple = true)
                                    @RequestParam(name = "productIds") List<Long> productIds,
                                    @ApiParam(value = "店铺id", name = "shopId", required = true)
                                    @RequestParam(name = "shopId") Long shopId) throws IOException {
        if (CollectionUtils.isEmpty(productIds)) {
            return new AjaxRes(AjaxRes.FAIL, "请求参数不能为空");
        }
        if (shopId == null) {
            return new AjaxRes(AjaxRes.FAIL, "请求参数店铺id不能为空");
        }
        return new AjaxRes(productSearchService.listSelectingSku(productIds, shopId));
    }
}
