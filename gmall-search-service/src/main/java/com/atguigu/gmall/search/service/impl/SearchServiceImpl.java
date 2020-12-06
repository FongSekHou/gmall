package com.atguigu.gmall.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuSearchInfo;
import com.atguigu.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    JestClient jestClient;


    @Override
    public List<PmsSkuSearchInfo> list(PmsSearchParam pmsSearchParam) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        /*if(pmsSearchParam.getSkuAttrValueList()!=null){
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchParam.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",pmsSkuAttrValue.getValueId());
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }*/
        String[] valueId = pmsSearchParam.getValueId();
        if(valueId!=null){
            for (String s : valueId) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",s);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        if(StringUtils.isNotBlank(pmsSearchParam.getCatalog3Id())){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",pmsSearchParam.getCatalog3Id());
        }
        if(StringUtils.isNotBlank(pmsSearchParam.getKeyword())){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",pmsSearchParam.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
        }

        searchSourceBuilder.query(boolQueryBuilder);

        // highlight
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);
        // from
        searchSourceBuilder.from(0);
        // size
        searchSourceBuilder.size(20);


        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex("gmall").addType("PmsSkuInfo").build();
        SearchResult execute = null;
        try {
            execute  = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<PmsSkuSearchInfo> pmsSkuSearchInfos = new ArrayList<>();
        List<SearchResult.Hit<PmsSkuSearchInfo, Void>> hits = execute.getHits(PmsSkuSearchInfo.class);
        for (SearchResult.Hit<PmsSkuSearchInfo, Void> hit : hits) {
            PmsSkuSearchInfo source = hit.source;
            Map<String, List<String>> highlight = hit.highlight;
            if(highlight!=null){
                String skuName = highlight.get("skuName").get(0);
                source.setSkuName(skuName);
            }
            pmsSkuSearchInfos.add(source);
        }

        return pmsSkuSearchInfos;
    }
}
