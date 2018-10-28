package com.wangjigroup.bigmiddle.productes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductIndex {
    private long productId;
    private String productModel;
    private String productName;
    private long brandId;
    private long categoryId;
    private String categoryIdPath;
    private long providerId;
    private String language;
    private String currency;
    private Integer status;
    private String warehouseLocation;
    private String warehouseLocationPath;
    private String detail;
    private String keyword;
    private List<SkuInfo> skuInfos;
    private List<InventoryInfo> inventoryInfos;

    public ProductIndex() {
        this.skuInfos = Lists.newArrayList();
        this.inventoryInfos = Lists.newArrayList();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkuInfo {
        private long skuId;
        private String sku;
        private double tagPrice;
        private double retailPrice;
        private int status;
        private List<Long> selectedShops;
        private List<Long> selectingShops;
        private double discount;
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date createTime;
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date onlineTime;
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date offlineTime;
        private int salesVolume;

        public SkuInfo() {
            this.selectedShops = Lists.newArrayList();
            this.selectingShops = Lists.newArrayList();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InventoryInfo {

        private long warehouseId;
        private List<ItemQuantity> itemQuantities;

        public InventoryInfo() {
        }

        public InventoryInfo(long warehouseId, List<ItemQuantity> itemQuantities) {
            this.warehouseId = warehouseId;
            this.itemQuantities = itemQuantities;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemQuantity {
        private long itemId;
        private int quantity;

        public ItemQuantity() {
        }

        public ItemQuantity(long itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }


}
