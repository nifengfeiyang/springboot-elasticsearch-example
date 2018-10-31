# springboot + elasticsearch + kafka 学习实例

- 使用Spring Initializr 创建maven项目:https://start.spring.io/

- 导入到idea

# 本地安装启动elasticsearch和kafka 使用docker方式:

#### elasticsearch
参考：https://www.elastic.co/guide/en/elasticsearch/reference/6.4/docker.html
- docker pull docker.elastic.co/elasticsearch/elasticsearch:6.2.4
- docker run -d -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:6.2.4

启动时报错，执行：　sysctl -w vm.max_map_count=262144

#### kafka

- docker pull wurstmeister/kafka
- docker run -d -p 2181:2181 -p 9092:9092 spotify/kafka


# elasticsearch的CRUD

查看版本:

curl -XGET http://localhost:9200

返回:

```json
{
  "name" : "zDOs--s",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "LgMm46NWSAqOoEnZKCMkNg",
  "version" : {
    "number" : "6.2.4",
    "build_hash" : "ccec39f",
    "build_date" : "2018-04-12T20:37:28.497551Z",
    "build_snapshot" : false,
    "lucene_version" : "7.2.1",
    "minimum_wire_compatibility_version" : "5.6.0",
    "minimum_index_compatibility_version" : "5.0.0"
  },
  "tagline" : "You Know, for Search"
}
```

创建index mapping:

curl -XPUT -H "Content-Type:application/json" http://localhost:9200/product_index -d '
```json
{
    "mappings": {
        "_doc": {
            "properties": {
                "brandId": {
                    "type": "long"
                },
                "categoryId": {
                    "type": "long"
                },
                "categoryIdPath": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "categoryId_path": {
                    "type": "keyword"
                },
                "currency": {
                    "type": "keyword"
                },
                "detail": {
                    "type": "text"
                },
                "inventoryInfos": {
                    "type": "nested",
                    "properties": {
                        "itemQuantities": {
                            "type": "nested",
                            "properties": {
                                "itemId": {
                                    "type": "long"
                                },
                                "quantity": {
                                    "type": "integer"
                                }
                            }
                        },
                        "warehouseId": {
                            "type": "long"
                        }
                    }
                },
                "keyword": {
                    "type": "text"
                },
                "language": {
                    "type": "keyword"
                },
                "productId": {
                    "type": "long"
                },
                "productModel": {
                    "type": "keyword"
                },
                "productName": {
                    "type": "text"
                },
                "providerId": {
                    "type": "long"
                },
                "skuInfos": {
                    "properties": {
                        "createTime": {
                            "type": "date",
                            "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
                        },
                        "discount": {
                            "type": "double"
                        },
                        "offlineTime": {
                            "type": "date",
                            "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
                        },
                        "onlineTime": {
                            "type": "date",
                            "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
                        },
                        "retailPrice": {
                            "type": "double"
                        },
                        "salesVolume": {
                            "type": "integer"
                        },
                        "selectedShops": {
                            "type": "long"
                        },
                        "selectingShops": {
                            "type": "long"
                        },
                        "sku": {
                            "type": "keyword"
                        },
                        "skuId": {
                            "type": "long"
                        },
                        "status": {
                            "type": "integer"
                        },
                        "tagPrice": {
                            "type": "double"
                        },
                        "unselectingShops": {
                            "type": "long"
                        }
                    }
                },
                "status": {
                    "type": "integer"
                }
            }
        }
    }
}
```
'

查看结果：

curl -XGET http://localhost:9200/product_index/_doc/_mapping?pretty

修改index设置(否则插入数据时会报错：FORBIDDEN/12/index-read-only):

curl -XPUT -H "Content-Type:application/json" http://localhost:9200/product_index/_settings -d '
```json
{
    "index": {
        "blocks": {
            "read_only_allow_delete": "false"
        }
    }
}
```
'

插入一条数据:

curl -XPOST -H "Content-Type:application/json" http://localhost:9200/product_index/_doc/228878077351166418 -d '
```json
{
  "productId": 228878077351166418,
  "productModel": "PSKU-icecream2072500032",
  "productName": "3D printing flower Korean version glossy women non-slip sandals sliper indoors/outdoor flip-flop",
  "brandId": 220923510746775563,
  "categoryId": 228525066177937409,
  "categoryIdPath": "228525066177937409",
  "providerId": 200269873509105689,
  "language": "english",
  "currency": "USD",
  "status": 1,
  "detail": null,
  "keyword": null,
  "skuInfos": [
    {
      "skuId": 228878077556687331,
      "sku": "SKU-icecream2072500032",
      "tagPrice": 1000,
      "retailPrice": 900,
      "status": 1,
      "discount": 0,
      "createTime": "2018-07-25 13:59:46",
      "onlineTime": "2018-07-25 14:42:54",
      "offlineTime": null,
      "salesVolume": 0
    }
  ]
}
```
'

查询:

curl -XGET http://localhost:9200/product_index/_doc/228878077351166418?pretty



