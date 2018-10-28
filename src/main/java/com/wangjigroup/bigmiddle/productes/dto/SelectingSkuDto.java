package com.wangjigroup.bigmiddle.productes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-22 上午11:12
 * @Description:
 */
@Data
@ApiModel(description = "sku选品列表")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SelectingSkuDto {
    @ApiModelProperty(value = "图片")
    private String imageUrl;
    @ApiModelProperty(value = "商品名称")
    private String name;
    @ApiModelProperty(value = "型号")
    private String spu;
    @ApiModelProperty(value = "SKU")
    private String sku;
    @ApiModelProperty(value = "属性")
    private String attribute;
    @ApiModelProperty(value = "供应商售价")
    private String retailPrice;
    @ApiModelProperty(value = "币别")
    private String currency;
    @ApiModelProperty(value = "库存数量")
    private Integer quantity;
    @ApiModelProperty(value = "销量")
    private Long salesVolume;
    @ApiModelProperty(value = "供应商名称")
    private String providerName;
    @ApiModelProperty(value = "仓库所在地")
    private String warehouseLocation;
    @ApiModelProperty(value = "productId")
    private Long productId;
    @ApiModelProperty(value = "选品状态：未选-0,已选-1")
    private Integer status;
    private Long skuId;
    private Long brandId;
    private Long categoryId;
    private Long warehouseId;
    private Long providerId;
}
