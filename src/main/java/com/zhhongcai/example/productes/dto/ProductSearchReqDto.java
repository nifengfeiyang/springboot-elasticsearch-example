package com.zhhongcai.example.productes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-18 下午12:01
 * @Description:
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ApiModel(description = "站内商品请求参数")
public class ProductSearchReqDto {
    @ApiModelProperty(value = "店铺id")
    private Long shopId;
    @ApiModelProperty(value = "型号或sku")
    private String spuOrSku;
    @ApiModelProperty(value = "品牌id")
    private Long brandId;
    @ApiModelProperty(value = "供应商id")
    private Long providerId;
    @ApiModelProperty(value = "仓库地址")
    private String warehouseLocation;
    @ApiModelProperty(value = "关键字")
    private String keyword;
    @ApiModelProperty(value = "品类id")
    private Long categoryId;
    @ApiModelProperty(value = "语言")
    private String language;
    @ApiModelProperty(value = "币别")
    private String currency;
    @ApiModelProperty(value = "价格from")
    private Double priceFrom;
    @ApiModelProperty(value = "价格to")
    private Double priceTo;
    @ApiModelProperty(value = "库存from")
    private Integer quantityFrom;
    @ApiModelProperty(value = "库存to")
    private Integer quantityTo;
    @ApiModelProperty(value = "价格降幅：全部：-1,百分之1-99")
    private Integer discountPercent;
    @ApiModelProperty(value = "上架时间")
    private Integer onSaleTime;
    @ApiModelProperty(value = "排序字段: 上架时间: online_time,库存: quantity,销量:sales_volume,价格:retail_price")
    private String sortItem;
    @ApiModelProperty(value = "排序:ASC,DESC")
    private String sortType;

    @ApiModelProperty(value = "第几页")
    private Integer pageNum;
    @ApiModelProperty(value = "每页条数")
    private Integer pageSize;
}
