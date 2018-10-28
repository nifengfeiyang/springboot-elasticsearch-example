package com.wangjigroup.bigmiddle.productes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.wangjigroup.bigmiddle.productes.dto.Page;
import com.wangjigroup.bigmiddle.productes.dto.ProductIndex;
import com.wangjigroup.bigmiddle.productes.dto.ProductSearchReqDto;
import com.wangjigroup.bigmiddle.productes.dto.ProductSearchRespDto;
import com.wangjigroup.bigmiddle.productes.dto.SelectingSkuDto;
import com.wangjigroup.common.exception.ServiceException;
import com.wangjigroup.common.service.Request;
import com.wangjigroup.common.service.Response;
import com.wangjigroup.common.service.ResponseCode;
import com.wangjigroup.common.user.context.UserContext;
import com.wangjigroup.common.user.context.UserInfo;
import com.wangjigroup.inventory.client.client.WarehouseClient;
import com.wangjigroup.inventory.client.dto.WarehouseDTO;
import com.wangjigroup.product.client.ProviderGoodsClient;
import com.wangjigroup.product.client.dto.common.ItemInfoDTO;
import com.wangjigroup.product.client.dto.common.ProductDTO;
import com.wangjigroup.product.client.dto.rq.GoodsQueryRq;
import com.wangjigroup.user.center.client.dto.client.UserCenterClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-17 下午5:02
 * @Description:
 */
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Value("${product.index.name}")
    private String indexName;

    private Logger logger = LoggerFactory.getLogger(ProductSearchServiceImpl.class);

    @Resource
    private RestHighLevelClient restHighLevelClient;

    private ProviderGoodsClient providerGoodsClient = new ProviderGoodsClient();

    private UserCenterClient userCenterClient = new UserCenterClient();

    private WarehouseClient warehouseClient = new WarehouseClient();

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Page<ProductSearchRespDto> search(ProductSearchReqDto reqDto) throws IOException {
        //TODO
        UserContext.putUserInfo("search", -1L);

        SearchSourceBuilder sb = buildSearchSourceBuilder(reqDto);
        if (sb == null) {
            return buildFromSearchResponse(reqDto.getShopId(), null, reqDto.getPageNum(), reqDto.getPageSize());
        }
        SearchRequest searchRequest = new SearchRequest(new String[]{indexName}, sb);
        SearchResponse response = restHighLevelClient.search(searchRequest);

        return buildFromSearchResponse(reqDto.getShopId(), response, reqDto.getPageNum(), reqDto.getPageSize());
    }

    private SearchSourceBuilder buildSearchSourceBuilder(ProductSearchReqDto reqDto) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //审核通过
        boolQuery.filter(QueryBuilders.termQuery("skuInfos.status", 1));
        if (StringUtils.isNotBlank(reqDto.getSpuOrSku())) {
            GoodsQueryRq reqData = new GoodsQueryRq();
            reqData.setItemCodeOrSku(reqDto.getSpuOrSku());
            Response<List<Long>> resp = providerGoodsClient.findItemIds(new Request<>(reqData));
            if (resp == null || resp.getCode() == ResponseCode.FAIL) {
                throw new ServiceException("查询商品信息异常");
            }
            if (CollectionUtils.isEmpty(resp.getData())) {
                return null;
            }

            boolQuery.filter(QueryBuilders.termsQuery("skuInfos.skuId", resp.getData()));
        }
        //品牌
        Optional.ofNullable(reqDto.getBrandId()).ifPresent(brandId ->
                boolQuery.filter(QueryBuilders.termQuery("brandId", brandId)));
        //供应商
        Optional.ofNullable(reqDto.getProviderId()).ifPresent(providerId ->
                boolQuery.filter(QueryBuilders.termQuery("providerId", providerId)));
        //TODO 仓库地址
        if (StringUtils.isNotBlank(reqDto.getWarehouseLocation())) {
            List<Long> warehouseIds = getWarehouseIds(reqDto.getWarehouseLocation());
            if (CollectionUtils.isEmpty(warehouseIds)) {
                return null;
            }
            boolQuery.filter(QueryBuilders.nestedQuery("inventoryInfos",
                    QueryBuilders.termsQuery("inventoryInfos.warehouseId", warehouseIds), ScoreMode.None));
        }
        //TODO 关键字 品类 品牌?
        if (StringUtils.isNotBlank(reqDto.getKeyword())) {
            boolQuery.filter(QueryBuilders.multiMatchQuery(reqDto.getKeyword(), "productModel", "productName"));
        }
        //品类
        Optional.ofNullable(reqDto.getCategoryId()).ifPresent(categoryId ->
                boolQuery.filter(QueryBuilders.wildcardQuery("categoryIdPath", "*" + categoryId + "*")));
        //语言
        if (StringUtils.isNotBlank(reqDto.getLanguage())) {
            boolQuery.filter(QueryBuilders.termQuery("language", reqDto.getLanguage()));
        }
        //币别
        if (StringUtils.isNotBlank(reqDto.getCurrency())) {
            boolQuery.filter(QueryBuilders.termQuery("currency", reqDto.getCurrency()));
        }
        //价格from
        Optional.ofNullable(reqDto.getPriceFrom()).ifPresent(priceFrom -> boolQuery.filter(
                QueryBuilders.rangeQuery("skuInfos.retailPrice").from(priceFrom)));
        //价格to
        Optional.ofNullable(reqDto.getPriceTo()).ifPresent(priceTo -> boolQuery.filter(
                QueryBuilders.rangeQuery("skuInfos.retailPrice").to(priceTo)));
        //库存from
        Optional.ofNullable(reqDto.getQuantityFrom()).ifPresent(quantityFrom -> boolQuery.filter(
                QueryBuilders.nestedQuery("inventoryInfos.itemQuantities",
                        QueryBuilders.rangeQuery("inventoryInfos.itemQuantities.quantity").from(quantityFrom), ScoreMode.None)));
        //库存to
        Optional.ofNullable(reqDto.getQuantityTo()).ifPresent(quantityTo -> boolQuery.filter(
                QueryBuilders.nestedQuery("inventoryInfos.itemQuantities",
                        QueryBuilders.rangeQuery("inventoryInfos.itemQuantities.quantity").from(quantityTo), ScoreMode.None)));
        //价格降幅
        Optional.ofNullable(reqDto.getDiscountPercent()).ifPresent(discountPercent -> {
            if (discountPercent > 0) {
                boolQuery.filter(QueryBuilders.rangeQuery("skuInfos.discount").from((double) discountPercent / -100.0));
            }
        });
        //上架时间
        Optional.ofNullable(reqDto.getOnSaleTime()).ifPresent(onSaleTime -> {
            String time = sdf.format(new Date(System.currentTimeMillis() - onSaleTime * 3600000 * 24L));
            boolQuery.filter(QueryBuilders.rangeQuery("skuInfos.onlineTime").from(time));
        });

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        if (!CollectionUtils.isEmpty(boolQuery.filter())) {
            sourceBuilder.query(boolQuery);
        }

        //排序
        SortOrder sortOrder = SortOrder.DESC.toString().equalsIgnoreCase(reqDto.getSortType()) ? SortOrder.DESC : SortOrder.ASC;

        if (StringUtils.isNotBlank(reqDto.getSortItem())) {
            if ("quantity".equalsIgnoreCase(reqDto.getSortItem())) {
                //库存
                sourceBuilder.sort(SortBuilders.fieldSort("inventoryInfos.itemQuantities.quantity")
                        .sortMode(SortMode.SUM).order(sortOrder));
            } else if ("sales_volume".equalsIgnoreCase(reqDto.getSortItem())) {
                //销量
                sourceBuilder.sort(SortBuilders.fieldSort("skuInfos.salesVolume").sortMode(SortMode.SUM).order(sortOrder));
            } else if ("retail_price".equalsIgnoreCase(reqDto.getSortItem())) {
                //价格
                sourceBuilder.sort(SortBuilders.fieldSort("skuInfos.retailPrice").sortMode(SortMode.MIN).order(sortOrder));
            } else {
                //默认按上架时间升序
                sourceBuilder.sort(SortBuilders.fieldSort("skuInfos.onlineTime").sortMode(SortMode.MAX).order(sortOrder));
            }
        } else {
            //默认按上架时间升序
            sourceBuilder.sort(SortBuilders.fieldSort("skuInfos.onlineTime").sortMode(SortMode.MAX).order(sortOrder));
        }
        //分页
        sourceBuilder.from((reqDto.getPageNum() - 1) * reqDto.getPageSize()).size(reqDto.getPageSize());
//        sourceBuilder.explain(true);
        logger.info("sourceBuilder: " + sourceBuilder.toString());
        return sourceBuilder;
    }

    private Page<ProductSearchRespDto> buildFromSearchResponse(Long shopId, SearchResponse response, Integer pageNum,
                                                               Integer pageSize) throws IOException {
        Page<ProductSearchRespDto> page = new Page<>();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);

        if (response != null) {
            logger.info("response=" + response.toString());
            if (RestStatus.OK.equals(response.status())) {
                int length = response.getHits().getHits().length;
                if (length > 0) {
                    //总数
                    page.setTotalRecord(response.getHits().totalHits);
                    page.setTotalPage(page.getTotalRecord() / pageSize + (page.getTotalRecord() % pageSize > 0 ? 1 : 0));

                    Set<Long> warehouseIds = Sets.newHashSet();
                    Set<Long> providerIds = Sets.newHashSet();
                    List<Long> productIds = Lists.newArrayListWithCapacity(length);

                    List<ProductSearchRespDto> data = Lists.newArrayListWithCapacity(length);
                    for (int i = 0; i < response.getHits().getHits().length; i++) {
                        ProductSearchRespDto dto = buildProductSearchRespDto(shopId, response.getHits().getHits()[i]);

                        Optional.ofNullable(dto.getWarehouseLocation()).ifPresent(warehouseLocation ->
                                Splitter.on(",").omitEmptyStrings().split(dto.getWarehouseLocation()).forEach(warehouseId -> warehouseIds.add(Long.parseLong(warehouseId))));
                        providerIds.add(dto.getProviderId());
                        productIds.add(dto.getProductId());

                        data.add(dto);
                    }
                    ForkJoinPool pool = null;
                    try {
                        UserInfo user = UserContext.getCurrentUser();
                        pool = new ForkJoinPool(3);
                        ForkJoinTask<Map<Long, ProductDTO>> f = pool.submit(() -> {
                            UserContext.putUserInfo(user);
                            return ProductSearchServiceImpl.this.productDTOMap(productIds);
                        });
                        ForkJoinTask<Map<Long, String>> f2 = pool.submit(() -> {
                            UserContext.putUserInfo(user);
                            return ProductSearchServiceImpl.this.providerMap(providerIds);
                        });
                        ForkJoinTask<Map<Long, WarehouseDTO>> f3 = pool.submit(() -> {
                            UserContext.putUserInfo(user);
                            return ProductSearchServiceImpl.this.warehouseMap(warehouseIds);
                        });
                        Map<Long, ProductDTO> productMap = f.join();
                        Map<Long, String> providerMap = f2.join();
                        Map<Long, WarehouseDTO> warehouseMap = f3.join();
                        for (ProductSearchRespDto dto : data) {
                            ProductDTO productDto = productMap.get(dto.getProductId());
                            dto.setImageUrl(productDto.getMainImage());
                            dto.setCurrency(productDto.getCurrency());
                            dto.setProviderName(providerMap.get(dto.getProviderId()));

                            dto.setWarehouseLocation(warehouseLocation(warehouseMap, dto.getWarehouseLocation()));
                        }
                    } finally {
                        if (pool != null) {
                            try {
                                pool.shutdown();
                            } catch (Exception e) {
                                logger.error("forkJoinPool shutdown error: " + e.getMessage(), e);
                            }
                        }
                    }

                    page.setResults(data);
                }
            } else {
                logger.error("查询商品失败: " + response);
                throw new ServiceException("查询商品失败");
            }
        }

        return page;
    }

    private List<Long> getWarehouseIds(String addressCode) {
        Response<List<WarehouseDTO>> resp = warehouseClient.findByAddressCode(new Request<>(addressCode));
        if (resp == null || resp.getCode() == ResponseCode.FAIL) {
            logger.error("查询仓库信息失败:" + (resp == null ? "" : resp.getMsg()));
            throw new ServiceException("查询仓库信息失败");
        }
        return resp.getData().stream().map(WarehouseDTO::getId).collect(Collectors.toList());
    }

    private String warehouseLocation(Map<Long, WarehouseDTO> warehouseMap, String warehouseLocation) {
        if (StringUtils.isNotBlank(warehouseLocation)) {
            StringBuilder location = new StringBuilder("");
            for (String warehouseId : warehouseLocation.split(",")) {
                Optional.ofNullable(warehouseMap.get(Long.valueOf(warehouseId))).ifPresent(w -> {
                    Optional.ofNullable(w.getCountryName()).ifPresent(location::append);
                    Optional.ofNullable(w.getProvinceName()).ifPresent(location::append);
                    Optional.ofNullable(w.getCityName()).ifPresent(location::append);
                    location.append(",");
                });
            }
            if (location.length() > 1) {
                location.deleteCharAt(location.length() - 1);
            }
            return location.toString();
        }
        return null;
    }

    private Map<Long, WarehouseDTO> warehouseMap(Set<Long> warehouseIds) {
        if (CollectionUtils.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        Response<Map<Long, WarehouseDTO>> warehouseResp = warehouseClient.findWarehouse(new Request<>(Lists.newArrayList(warehouseIds)));
        if (warehouseResp == null || warehouseResp.getCode() == ResponseCode.FAIL) {
            logger.error("查询仓库信息失败:" + (warehouseResp == null ? "" : warehouseResp.getMsg()));
            throw new ServiceException("查询仓库信息失败");
        }
        return warehouseResp.getData();
    }

    private Map<Long, String> providerMap(Set<Long> providerIds) {
        if (CollectionUtils.isEmpty(providerIds)) {
            return Collections.emptyMap();
        }
        Response<Map<Long, String>> providerResp = userCenterClient.batchGetNameById(new Request(Lists.newArrayList(providerIds)));
        if (providerResp == null || providerResp.getCode() == ResponseCode.FAIL) {
            logger.error("查询供应商信息失败:" + (providerResp == null ? "" : providerResp.getMsg()));
            throw new ServiceException("查询供应商信息失败");
        }
        return providerResp.getData();
    }

    private Map<Long, ProductDTO> productDTOMap(List<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        Response<Map<Long, ProductDTO>> productResp = providerGoodsClient.findProductByProductIds(new Request<>(productIds));
        if (productResp == null || productResp.getCode() == ResponseCode.FAIL) {
            logger.error("查询商品信息失败:" + (productResp == null ? "" : productResp.getMsg()));
            throw new ServiceException("查询商品信息失败");
        }
        return productResp.getData();
    }

    private ProductSearchRespDto buildProductSearchRespDto(Long shopId, SearchHit searchHit) throws IOException {
        ProductIndex productIndex = objectMapper.readValue(searchHit.getSourceAsString(), ProductIndex.class);
        ProductSearchRespDto dto = new ProductSearchRespDto();
        dto.setProductId(productIndex.getProductId());
        dto.setCurrency(productIndex.getCurrency());
        dto.setName(productIndex.getProductName());
        dto.setSpu(productIndex.getProductModel());
        dto.setProviderId(productIndex.getProviderId());
        dto.setBrandId(productIndex.getBrandId());
        dto.setCategoryId(productIndex.getCategoryId());

        if (!CollectionUtils.isEmpty(productIndex.getInventoryInfos())) {
            Integer sumQuantity = 0;
            Set<Long> warehouseIds = Sets.newHashSet();
            for (ProductIndex.InventoryInfo inventoryInfo : productIndex.getInventoryInfos()) {
                for (ProductIndex.ItemQuantity itemQuantity : inventoryInfo.getItemQuantities()) {
                    sumQuantity += itemQuantity.getQuantity();
                }
                warehouseIds.add(inventoryInfo.getWarehouseId());
            }
            dto.setQuantity(sumQuantity);
            dto.setWarehouseLocation(Joiner.on(",").skipNulls().join(warehouseIds));
        }
        if (!CollectionUtils.isEmpty(productIndex.getSkuInfos())) {
            double minRetailPrice = Double.MAX_VALUE;
            double maxRetailPrice = 0.0;
            long sumSalesVolume = 0;
            int status = 0;
            for (ProductIndex.SkuInfo skuInfo : productIndex.getSkuInfos()) {
                if (minRetailPrice > skuInfo.getRetailPrice()) {
                    minRetailPrice = skuInfo.getRetailPrice();
                }
                if (maxRetailPrice < skuInfo.getRetailPrice()) {
                    maxRetailPrice = skuInfo.getRetailPrice();
                }
                sumSalesVolume += skuInfo.getSalesVolume();
                if (shopId != null) {
                    if (skuInfo.getSelectedShops() != null) {
                        if (skuInfo.getSelectedShops().contains(shopId)) {
                            status++;
                        }
                    }
                    if (skuInfo.getSelectingShops() != null) {
                        if (skuInfo.getSelectingShops().contains(shopId)) {
                            status++;
                        }
                    }
                }
            }
            if (shopId != null) {
                dto.setStatus(status > 0 ? (status >= productIndex.getSkuInfos().size() ? 2 : 1) : null);
            }
            dto.setMinRetailPrice(String.valueOf(minRetailPrice));
            dto.setMaxRetailPrice(String.valueOf(maxRetailPrice));
            dto.setSalesVolume(sumSalesVolume);
        }

        return dto;
    }

    @Override
    public Map<Integer, Long> priceDiscountPercentCount(List<Integer> discountPercentList) throws IOException {
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        for (int i = 0; i < discountPercentList.size(); i++) {
            Integer from = i > 0 ? discountPercentList.get(i) : null;
            Integer to = i == 0 ? discountPercentList.get(1) : null;
            multiSearchRequest.add(buildPriceDiscountPercentCountRequest(from, to));
        }
        logger.info("buildPriceDiscountPercentCountRequest multiSearchRequest=" + multiSearchRequest.requests());
        MultiSearchResponse multiResponse = restHighLevelClient.multiSearch(multiSearchRequest);
        logger.info("priceDiscountPercentCount multiResponse=" + multiResponse.toString());

        Map<Integer, Long> countMap = Maps.newHashMapWithExpectedSize(discountPercentList.size());
        for (int i = 0; i < multiResponse.getResponses().length; i++) {
            MultiSearchResponse.Item item = multiResponse.getResponses()[i];
            SearchResponse response = item.getResponse();
            if (RestStatus.OK.equals(response.status())) {
                countMap.put(discountPercentList.get(i), response.getHits().totalHits);
            }
        }
        return countMap;
    }

    private SearchRequest buildPriceDiscountPercentCountRequest(Integer discountPercentFrom, Integer discountPercentTo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuInfos.discount").includeLower(true);
        Optional.ofNullable(discountPercentFrom).ifPresent(df -> rangeQueryBuilder.from(df / -100.0));
        Optional.ofNullable(discountPercentTo).ifPresent(dt -> rangeQueryBuilder.to(dt / -100.0));

        sourceBuilder.query(rangeQueryBuilder).size(0);
        sourceBuilder.fetchSource(false);

        logger.info("buildPriceDiscountPercentCountRequest sourceBuilder=" + sourceBuilder.toString());
        return new SearchRequest(new String[]{indexName}, sourceBuilder);
    }

    @Override
    public List<SelectingSkuDto> listSelectingSku(List<Long> productIds, Long shopId) throws IOException {
        UserContext.putUserInfo("search", -1L);

        SearchSourceBuilder sb = new SearchSourceBuilder();
        sb.query(QueryBuilders.termsQuery("productId", productIds));
        //TODO 条数
        sb.size(300);

        logger.info("listSelectingSku request= " + sb.toString());
        SearchResponse response = restHighLevelClient.search(new SearchRequest(new String[]{indexName}, sb));
        logger.info("listSelectingSku response= " + response.toString());

        if (RestStatus.OK.equals(response.status())) {
            int size = response.getHits().getHits().length;
            if (size > 0) {
                List<SelectingSkuDto> result = Lists.newArrayListWithCapacity(size);
                Set<Long> warehouseIds = Sets.newHashSet();
                for (int i = 0; i < size; i++) {
                    result.addAll(buildSelectingSkuDto(shopId, response.getHits().getHits()[i]));
                }
                Set<Long> providerIds = Sets.newHashSet();
                List<Long> skuIds = Lists.newArrayListWithCapacity(result.size());
                result.forEach(dto -> {
                    Splitter.on(",").omitEmptyStrings().split(dto.getWarehouseLocation()).forEach(warehouseId -> warehouseIds.add(Long.parseLong(warehouseId)));
                    skuIds.add(dto.getSkuId());
                    providerIds.add(dto.getProviderId());
                });

                ForkJoinPool pool = null;
                try {
                    UserInfo user = UserContext.getCurrentUser();
                    pool = new ForkJoinPool(3);
                    ForkJoinTask<Map<Long, ItemInfoDTO>> f = pool.submit(() -> {
                        UserContext.putUserInfo(user);
                        return ProductSearchServiceImpl.this.itemMap(skuIds);
                    });
                    ForkJoinTask<Map<Long, String>> f2 = pool.submit(() -> {
                        UserContext.putUserInfo(user);
                        return ProductSearchServiceImpl.this.providerMap(providerIds);
                    });
                    ForkJoinTask<Map<Long, WarehouseDTO>> f3 = pool.submit(() -> {
                        UserContext.putUserInfo(user);
                        return ProductSearchServiceImpl.this.warehouseMap(warehouseIds);
                    });
                    Map<Long, ItemInfoDTO> itemMap = f.join();
                    Map<Long, String> providerMap = f2.join();
                    Map<Long, WarehouseDTO> warehouseMap = f3.join();
                    result.forEach(dto -> {
                        dto.setWarehouseLocation(warehouseLocation(warehouseMap, dto.getWarehouseLocation()));

                        ItemInfoDTO item = itemMap.get(dto.getSkuId());
                        dto.setImageUrl(item.getMainImgUrl());
                        dto.setAttribute(item.getAttrValues());
                        dto.setProviderName(providerMap.get(dto.getProviderId()));
                    });
                } finally {
                    if (pool != null) {
                        try {
                            pool.shutdown();
                        } catch (Exception e) {
                            logger.error("forkJoinPool shutdown error: " + e.getMessage(), e);
                        }
                    }
                }


                return result;
            }
        } else {
            logger.error("查询失败:" + response);
            throw new ServiceException("查询失败");
        }
        return null;
    }

    private Map<Long, ItemInfoDTO> itemMap(List<Long> skuIds) {
        Response<Map<Long, ItemInfoDTO>> itemResp = providerGoodsClient.findSimpleItemInfo(new Request<>(skuIds));
        if (itemResp == null || itemResp.getCode() == ResponseCode.FAIL) {
            logger.error("查询商品信息失败:" + (itemResp == null ? "" : itemResp.getMsg()));
            throw new ServiceException("查询商品信息失败");
        }
        return itemResp.getData();
    }

    private List<SelectingSkuDto> buildSelectingSkuDto(Long shopId, SearchHit searchHit) throws IOException {
        ProductIndex productIndex = objectMapper.readValue(searchHit.getSourceAsString(), ProductIndex.class);

        List<SelectingSkuDto> list = Lists.newArrayListWithCapacity(productIndex.getSkuInfos().size());
        productIndex.getSkuInfos().forEach(sku -> {
            SelectingSkuDto dto = new SelectingSkuDto();
            dto.setName(productIndex.getProductName());
            dto.setCurrency(productIndex.getCurrency());
            dto.setSpu(productIndex.getProductModel());
            dto.setSku(sku.getSku());
            dto.setProductId(productIndex.getProductId());
            dto.setWarehouseLocation("");
            dto.setStatus(0);
            if (sku.getSelectingShops() != null) {
                if (sku.getSelectingShops().contains(shopId)) {
                    dto.setStatus(1);
                }
            }
            if (sku.getSelectedShops() != null) {
                if (sku.getSelectedShops().contains(shopId)) {
                    dto.setStatus(1);
                }
            }

            int quantity = 0;
            Set<Long> warehouseIds = Sets.newHashSet();
            for (ProductIndex.InventoryInfo inventoryInfo : productIndex.getInventoryInfos()) {
                for (ProductIndex.ItemQuantity itemQuantity : inventoryInfo.getItemQuantities()) {
                    if (itemQuantity.getItemId() == sku.getSkuId()) {
                        quantity += itemQuantity.getQuantity();
                        warehouseIds.add(inventoryInfo.getWarehouseId());
                    }
                }
            }
            dto.setWarehouseLocation(Joiner.on(",").skipNulls().join(warehouseIds));
            dto.setQuantity(quantity);

            dto.setRetailPrice(String.valueOf(sku.getRetailPrice()));
            dto.setSkuId(sku.getSkuId());
            dto.setProviderId(productIndex.getProviderId());
            dto.setCategoryId(productIndex.getCategoryId());
            dto.setBrandId(productIndex.getBrandId());
            dto.setSalesVolume((long) sku.getSalesVolume());
            list.add(dto);
        });

        return list;
    }

    @Override
    public List<Long> searchProductId(ProductSearchReqDto request) throws IOException {
        SearchSourceBuilder sb = buildSearchSourceBuilder(request);
        if (sb == null) {
            return Collections.emptyList();
        }
        //只取出productId
        sb.fetchSource("productId", "");

        SearchRequest searchRequest = new SearchRequest(new String[]{indexName}, sb);
        SearchResponse response = restHighLevelClient.search(searchRequest);
        logger.info("response = " + response);

        List<Long> productIds = Lists.newArrayListWithCapacity(request.getPageSize());
        if (RestStatus.OK.equals(response.status())) {
            int length = response.getHits().getHits().length;
            if (length > 0) {
                for (int i = 0; i < response.getHits().getHits().length; i++) {
                    productIds.add(Long.parseLong(response.getHits().getHits()[i].getSourceAsMap().get("productId").toString()));
                }
            }
        } else {
            logger.error("查询商品失败: " + response);
            throw new ServiceException("查询商品失败");
        }

        return productIds;
    }
}
