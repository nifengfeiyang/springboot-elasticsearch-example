package com.zhhongcai.example.productes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zhhongcai.example.productes.dto.Page;
import com.zhhongcai.example.productes.dto.ProductIndex;
import com.zhhongcai.example.productes.dto.ProductSearchReqDto;
import com.zhhongcai.example.productes.dto.ProductSearchRespDto;
import com.zhhongcai.example.productes.dto.SelectingSkuDto;
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
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.valuecount.ParsedValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
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

/**
 * @Author: caizhh
 * @Date: Create in 18-10-17 下午5:02
 * @Description:
 */
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Value("${product.index.name}")
    private String indexName;

    @Value("${discountPercentList}")
    private String discountPercentList;

    private Logger logger = LoggerFactory.getLogger(ProductSearchServiceImpl.class);

    @Resource
    private RestHighLevelClient restHighLevelClient;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Page<ProductSearchRespDto> search(ProductSearchReqDto reqDto) throws Exception {
        SearchSourceBuilder sb = buildSearchSourceBuilder(reqDto);
        if (sb == null) {
            return buildFromSearchResponse(reqDto.getShopId(), null, reqDto.getPageNum(), reqDto.getPageSize());
        }
        SearchRequest searchRequest = new SearchRequest(new String[]{indexName}, sb);
//        searchRequest.scroll(new TimeValue(1, TimeUnit.MINUTES));
        SearchResponse response = restHighLevelClient.search(searchRequest);

        return buildFromSearchResponse(reqDto.getShopId(), response, reqDto.getPageNum(), reqDto.getPageSize());
    }

    private SearchSourceBuilder buildSearchSourceBuilder(ProductSearchReqDto reqDto) throws Exception {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (!needQuery(reqDto, boolQuery)) {
            return null;
        }

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
                        .setNestedSort(new NestedSortBuilder("inventoryInfos.itemQuantities"))
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
        sourceBuilder.from((reqDto.getPageNum() - 1) * reqDto.getPageSize())
                .size(reqDto.getPageSize());
        logger.info("sourceBuilder: " + sourceBuilder.toString());
        return sourceBuilder;
    }

    private boolean needQuery(ProductSearchReqDto reqDto, BoolQueryBuilder boolQuery) throws Exception {
        Optional.ofNullable(reqDto.getShopId()).orElseThrow(() -> new Exception("店铺id不能为空"));

        //审核通过
        boolQuery.filter(QueryBuilders.termQuery("skuInfos.status", 1));

        //未选及部分选
        if (reqDto.getStatus() != null && reqDto.getStatus() == 0) {
            boolQuery.should(QueryBuilders.termsQuery("skuInfos.selectedShops", Lists.newArrayList(reqDto.getShopId())))
                    .should(QueryBuilders.termsQuery("skuInfos.selectingShops", Lists.newArrayList(reqDto.getShopId())))
                    .minimumShouldMatch(1);
        }

        if (StringUtils.isNotBlank(reqDto.getSpuOrSku())) {
           boolQuery.filter(QueryBuilders.termsQuery("skuInfos.skuId", reqDto.getSpuOrSku()));
        }
        //品牌
        Optional.ofNullable(reqDto.getBrandId()).ifPresent(brandId ->
                boolQuery.filter(QueryBuilders.termQuery("brandId", brandId)));
        //供应商
        Optional.ofNullable(reqDto.getProviderId()).ifPresent(providerId ->
                boolQuery.filter(QueryBuilders.termQuery("providerId", providerId)));
        //TODO 仓库地址
        if (StringUtils.isNotBlank(reqDto.getWarehouseLocation())) {
            boolQuery.filter(QueryBuilders.nestedQuery("inventoryInfos",
                    QueryBuilders.termsQuery("inventoryInfos.warehouseId", reqDto.getWarehouseLocation()), ScoreMode.None));
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
        RangeQueryBuilder priceRange = null;
        //价格from
        if (reqDto.getPriceFrom() != null) {
            priceRange = QueryBuilders.rangeQuery("skuInfos.retailPrice").from(reqDto.getPriceFrom());
        }
        //价格to
        if (reqDto.getPriceTo() != null) {
            if (priceRange == null) {
                priceRange = QueryBuilders.rangeQuery("skuInfos.retailPrice").to(reqDto.getPriceTo());
            } else {
                priceRange.to(reqDto.getPriceTo());
            }
        }
        if (priceRange != null) {
            boolQuery.filter(priceRange);
        }

        RangeQueryBuilder quantityRange = null;
        //库存from
        if (reqDto.getQuantityFrom() != null) {
            quantityRange = QueryBuilders.rangeQuery("inventoryInfos.itemQuantities.quantity").from(reqDto.getQuantityFrom());
        }
        //库存to
        if (reqDto.getQuantityTo() != null) {
            if (quantityRange == null) {
                quantityRange = QueryBuilders.rangeQuery("inventoryInfos.itemQuantities.quantity").to(reqDto.getQuantityTo());
            } else {
                quantityRange.to(reqDto.getQuantityTo());
            }
        }
        if (quantityRange != null) {
            boolQuery.filter(QueryBuilders.nestedQuery("inventoryInfos.itemQuantities", quantityRange, ScoreMode.None));
        }

        //价格降幅
        Optional.ofNullable(reqDto.getDiscountPercent()).ifPresent(discountPercent -> {
            if (discountPercent > 0) {
                boolQuery.filter(QueryBuilders.rangeQuery("skuInfos.discount").to((double) discountPercent / -100.0)
                        .includeLower(true).includeUpper(false));
            }
        });
        //上架时间
        Optional.ofNullable(reqDto.getOnSaleTime()).ifPresent(onSaleTime -> {
            if (onSaleTime > 0) {
                String time = sdf.format(new Date(System.currentTimeMillis() - onSaleTime * 3600000 * 24L));
                boolQuery.filter(QueryBuilders.rangeQuery("skuInfos.onlineTime").from(time));
            }
        });
        return true;
    }

    private Page<ProductSearchRespDto> buildFromSearchResponse(Long shopId, SearchResponse response, Integer pageNum,
                                                               Integer pageSize) throws Exception {
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
                    page.setResults(data);
                }
            } else {
                logger.error("查询商品失败: " + response);
                throw new Exception("查询商品失败");
            }
        }

        return page;
    }
    private ProductSearchRespDto buildProductSearchRespDto(Long shopId, SearchHit searchHit) throws Exception {
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
    public Map<Integer, Long> priceDiscountPercentCount(ProductSearchReqDto reqDto) throws Exception {

        reqDto.setPageNum(null);
        reqDto.setPageSize(null);

        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        String[] percentList = discountPercentList.split(",");

        Map<Integer, Long> countMap = Maps.newHashMapWithExpectedSize(percentList.length);
        for (int i = 0; i < percentList.length; i++) {
            Integer from = (i == 0 ? Integer.valueOf(percentList[1]) : null);
            Integer to = (i == 0 ? 0 : Integer.valueOf(percentList[i]));
            countMap.put(Integer.valueOf(percentList[i]), 0L);

            SearchRequest req = buildPriceDiscountPercentCountRequest(reqDto, from, to, i);
            Optional.ofNullable(req).ifPresent(multiSearchRequest::add);
        }
        logger.info("buildPriceDiscountPercentCountRequest multiSearchRequest=" + multiSearchRequest.requests());
        if (CollectionUtils.isEmpty(multiSearchRequest.requests())) {
            return countMap;
        }
        MultiSearchResponse multiResponse = restHighLevelClient.multiSearch(multiSearchRequest);
        logger.info("priceDiscountPercentCount multiResponse=" + multiResponse.toString());

        for (int i = 0; i < multiResponse.getResponses().length; i++) {
            MultiSearchResponse.Item item = multiResponse.getResponses()[i];
            SearchResponse response = item.getResponse();
            if (RestStatus.OK.equals(response.status())) {
                for (int j = 0; j < percentList.length; j++) {
                    Aggregation data = response.getAggregations().getAsMap().get("productId_" + j);
                    if (data != null) {
                        countMap.put(Integer.valueOf(percentList[j]), ((ParsedValueCount) data).getValue());
                    }
                }
            }
        }
        return countMap;
    }

    private SearchRequest buildPriceDiscountPercentCountRequest(ProductSearchReqDto dto, Integer from, Integer to, int index) throws Exception {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (!needQuery(dto, boolQuery)) {
            return null;
        }
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuInfos.discount").includeUpper(true);
        Optional.ofNullable(from).ifPresent(df -> rangeQueryBuilder.from(df == 0 ? 0 : df / -100.0));
        Optional.ofNullable(to).ifPresent(dt -> rangeQueryBuilder.to(dt == 0 ? 0 : dt / -100.0));
        boolQuery.must(rangeQueryBuilder);
        sourceBuilder.query(boolQuery);
        sourceBuilder.fetchSource(false);
        sourceBuilder.size(0);
//        sourceBuilder.trackTotalHits(false);

        sourceBuilder.aggregation(AggregationBuilders.count("productId_" + index).field("productId"));

        logger.info("buildPriceDiscountPercentCountRequest sourceBuilder=" + sourceBuilder.toString());
        return new SearchRequest(new String[]{indexName}, sourceBuilder);
    }

    @Override
    public List<SelectingSkuDto> listSelectingSku(List<Long> productIds, Long shopId) throws Exception {

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

                return result;
            }
        } else {
            logger.error("查询失败:" + response);
            throw new Exception("查询失败");
        }
        return null;
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
    public List<Long> searchProductId(ProductSearchReqDto request) throws Exception {
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
            throw new Exception("查询商品失败");
        }

        return productIds;
    }
}
