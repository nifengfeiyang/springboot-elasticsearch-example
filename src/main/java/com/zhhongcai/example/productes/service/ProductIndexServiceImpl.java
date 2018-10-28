package com.zhhongcai.example.productes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.zhhongcai.example.productes.dto.ProductIndex;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @Author: caizhh
 * @Date: Create in 18-10-24 下午7:23
 * @Description:
 */
@Service
public class ProductIndexServiceImpl implements ProductIndexService {

    private Logger logger = LoggerFactory.getLogger(ProductIndexServiceImpl.class);

    @Value("${product.index.name}")
    private String indexName;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ObjectMapper objectMapper;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    @Override
    public int updateProductIndex(String topic) throws Exception {
//        return updateSkuInfos(topic.getProductId().toString(), topic.getProviderItemId(), topic.getShopId(), new Date(), null);
        return 1;
    }

    private int updateSkuInfos(String productId, Long productSkuId, Long shopId, Date onlineTime, Date offlineTime) throws Exception {
        GetRequest getRequest = new GetRequest(indexName, "_doc", productId);
        String[] includes = new String[]{"skuInfos"};
        String[] excludes = Strings.EMPTY_ARRAY;
        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, includes, excludes);
        getRequest.fetchSourceContext(fetchSourceContext);

        GetResponse getResponse = restHighLevelClient.get(getRequest);

        if (getResponse.isExists()) {
            long version = getResponse.getVersion();
            String sourceAsString = getResponse.getSourceAsString();
            ProductIndex productIndex = objectMapper.readValue(sourceAsString, ProductIndex.class);

            productIndex.getSkuInfos().forEach(sku -> {
                if (productSkuId.equals(sku.getSkuId())) {
                    Optional.ofNullable(onlineTime).ifPresent(ot -> {
                        sku.setOnlineTime(ot);
                        if (!sku.getSelectedShops().contains(shopId)) {
                            sku.getSelectedShops().add(shopId);
                        }
                    });
                    Optional.ofNullable(offlineTime).ifPresent(offt -> {
                        sku.setOfflineTime(offt);
                        if (sku.getSelectedShops().contains(shopId)) {
                            sku.getSelectedShops().remove(shopId);
                        }
                    });
                    if (sku.getSelectingShops().contains(shopId)) {
                        sku.getSelectedShops().remove(shopId);
                    }
                }
            });

            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("skuInfos", productIndex.getSkuInfos());
            UpdateRequest updateRequest = new UpdateRequest(indexName, "_doc", productId)
                    .doc(objectMapper.writeValueAsString(jsonMap), XContentType.JSON);
            updateRequest.version(version);
            try {
                logger.info("updateRequest=" + updateRequest);
                UpdateResponse updateResponse = restHighLevelClient.update(updateRequest);
                logger.info("updateResponse=" + updateResponse);
                updateResponse.getResult();
            } catch (ElasticsearchException e) {
                if (e.status() == RestStatus.CONFLICT) {
                    //TODO
                }
            }

        } else {
            throw new Exception(productId + ": index not exists!!!");
        }
        return 1;
    }

    private int updateSkuInfosByScript(String productId, Long productSkuId, Long shopId, Date onlineTime, Date offlineTime) throws IOException {
        StringBuilder script = new StringBuilder("");
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(5);

        script.append("for( int i = 0; i < ctx._source.skuInfos.size(); i++) { ")
                .append("  if(ctx._source.skuInfos[i]['skuId'] == params.productSkuId) { ");
        Optional.ofNullable(onlineTime).ifPresent(ot -> {
            params.put("onlineTime", sdf.format(onlineTime));
            script.append("    ctx._source.skuInfos[i]['onlineTime'] = params.onlineTime; ");
            script.append("    if(!ctx._source.skuInfos[i]['selectedShops'].contains(params.shopId)) { ")
                    .append("      ctx._source.skuInfos[i]['selectedShops'].add(params.shopId); }");
        });
        Optional.ofNullable(offlineTime).ifPresent(offt -> {
            params.put("offlineTime", sdf.format(offlineTime));
            script.append("    ctx._source.skuInfos[i]['offlineTime'] = params.offlineTime; ");
            script.append("    if(ctx._source.skuInfos[i]['selectedShops'].contains(params.shopId)) { ")
                    .append("      ctx._source.skuInfos[i]['selectedShops'].remove(params.shopId); }");
        });
        script.append("    if(ctx._source.skuInfos[i]['selectingShops'].contains(params.shopId)) { ")
                .append("      ctx._source.skuInfos[i]['selectingShops'].remove(params.shopId); } ");
        script.append("}}");

        params.put("productSkuId", productSkuId);
        params.put("shopId", shopId);
        UpdateRequest updateRequest = new UpdateRequest(indexName, "_doc", productId);
        updateRequest.script(new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, script.toString(), params));

        try {
            logger.info("updateRequest=" + updateRequest);
            UpdateResponse updateResponse = restHighLevelClient.update(updateRequest);
            logger.info("updateResponse=" + updateResponse);
            updateResponse.getResult();
            //TODO 返回结果处理
        } catch (ElasticsearchException e) {
            logger.error("updateSkuInfosByScript ERROR: " + e.getMessage(), e);
            if (e.status() == RestStatus.CONFLICT) {
                //TODO
            }
        }
        return 1;
    }

}
