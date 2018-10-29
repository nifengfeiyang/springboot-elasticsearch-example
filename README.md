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

查看版本
curl -XGET http://localhost:9200
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

