package com.wangjigroup.bigmiddle.productes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-18 下午12:01
 * @Description:
 */
@Data
@ApiModel(description = "站内商品查询结果")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSearchRespDto {
    @ApiModelProperty(value = "图片")
    private String imageUrl;
    @ApiModelProperty(value = "商品名称")
    private String name;
    @ApiModelProperty(value = "型号")
    private String spu;
    @ApiModelProperty(value = "最低售价")
    private String minRetailPrice;
    @ApiModelProperty(value = "最高售价")
    private String maxRetailPrice;
    @ApiModelProperty(value = "币别")
    private String currency;
    @ApiModelProperty(value = "库存数量")
    private Integer quantity;
    @ApiModelProperty(value = "供应商名称")
    private String providerName;
    @ApiModelProperty(value = "仓库所在地")
    private String warehouseLocation;
    @ApiModelProperty(value = "productId")
    private Long productId;
    @ApiModelProperty(value = "选品状态：未选-0,部分选品-1,已选-2")
    private Integer status;
    @ApiModelProperty(value = "sku数量")
    private Long skuCount;
    @ApiModelProperty(value = "销量")
    private Long salesVolume;

    private Long brandId;
    private Long categoryId;
    private Long providerId;
}
