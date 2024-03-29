package com.tracer.news.news.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracer.news.config.redis.RedisNews;
import com.tracer.news.config.redis.RedisService;
import com.tracer.news.news.client.KeywordServiceClient;
import com.tracer.news.news.client.TimelineServiceClient;
import com.tracer.news.news.dto.ClusterPageDto;
import com.tracer.news.news.dto.CountPerPressDto;
import com.tracer.news.news.dto.NewsIdDto;
import com.tracer.news.news.dto.NewsListDto;
import com.tracer.news.news.entity.News;
import com.tracer.news.news.entity.NewsKeyword;
import com.tracer.news.news.entity.Shortcut;
import com.tracer.news.news.mapping.NewsPressMapping;
import com.tracer.news.news.repository.NewsKeywordRepository;
import com.tracer.news.news.repository.NewsRepository;
import com.tracer.news.news.repository.ShortcutRepository;
import com.tracer.news.news.vo.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsService.class);
    private final NewsRepository newsRepository;
    private final ShortcutRepository shortcutRepository;
    private final NewsKeywordRepository newsKeywordRepository;
    private final KeywordServiceClient keywordServiceClient;
    private final TimelineServiceClient timelineServiceClient;
    private final RedisService redisService;

    @Transactional
    public ResNewsSearch newsSearch(ReqNewsSearch reqNewsSearch) {
        ResNewsSearch resNewsSearch = new ResNewsSearch();


//        PageRequest pageRequest =PageRequest.of(reqNewsSearch.getOffset(), reqNewsSearch.getLimit(), Sort.by("newsDate","newsTime").descending());
        Sort sort = Sort.by(
                Sort.Order.desc("newsDate"),
                Sort.Order.desc("newsTime")
        );
        List<News> newsTitleAndNewsContentPage = new ArrayList<>();
        List<News> newsTitlePage = new ArrayList<>();
        List<News> newsContentPage = new ArrayList<>();

        if(!redisService.checkKeys(reqNewsSearch.getWord())){
            StringBuilder sb = new StringBuilder();
            sb.append("+");
            sb.append(reqNewsSearch.getWord());
            sb.append("*");
            newsTitleAndNewsContentPage =
                    newsRepository.findByNewTitleLikeAndNewsContentLike(sb.toString(), sb.toString());
            redisService.setValues(reqNewsSearch.getWord(), newsTitleAndNewsContentPage);
            LOGGER.info("Title, Content : {}", "Input Redis");
            newsTitlePage =
                    newsRepository.findByNewTitleLikeAndNewsContentNotLike(sb.toString(), sb.toString());
            redisService.setValues(reqNewsSearch.getWord(), newsTitlePage);
            LOGGER.info("Title : {}", "Input Redis");
            newsContentPage =
                    newsRepository.findByNewTitleNotLikeAndNewsContentLike(sb.toString(), sb.toString());
            redisService.setValues(reqNewsSearch.getWord(), newsContentPage);
            LOGGER.info("Content : {}", "Input Redis");
        }else{
            LOGGER.info("ALREADY IN REDIS : {}", "OK");
            newsTitleAndNewsContentPage = redisService.getValues(reqNewsSearch.getWord(), 0);
            if(newsTitleAndNewsContentPage == null) newsTitleAndNewsContentPage= new ArrayList<>();
            newsTitlePage = redisService.getValues(reqNewsSearch.getWord(), 1);
            if(newsTitlePage == null) newsTitlePage = new ArrayList<>();
            newsContentPage = redisService.getValues(reqNewsSearch.getWord(), 2);
            if(newsContentPage == null) newsContentPage = new ArrayList<>();
        }
        if(reqNewsSearch.getType() == 1){
            newsContentPage.clear();
        }else if(reqNewsSearch.getType() == 2){
            newsTitlePage.clear();
        }

        List<NewsListDto> newsList = new ArrayList<>();
        for (News n:
                newsTitleAndNewsContentPage) {
            newsList.add(
                    NewsListDto.builder()
                            .newsId(n.getNewsId())
                            .newsTitle(n.getNewTitle())
                            .newsContent(n.getNewsContent())
                            .newsSource(n.getNewsSource())
                            .newsReporter(n.getNewsReporter())
                            .newsPress(n.getNewsPress())
                            .newsThumbnail(n.getNewsThumbnail())
                            .newsDate(n.getNewsDate())
                            .newsTime(n.getNewsTime())
                            .newsType(n.getNewsType().name())
                            .newsTypeCode(n.getNewsType().getCode())
                            .build()
            );
        }

        for (News n:
                newsTitlePage) {
            newsList.add(
                    NewsListDto.builder()
                            .newsId(n.getNewsId())
                            .newsTitle(n.getNewTitle())
                            .newsContent(n.getNewsContent())
                            .newsSource(n.getNewsSource())
                            .newsReporter(n.getNewsReporter())
                            .newsPress(n.getNewsPress())
                            .newsThumbnail(n.getNewsThumbnail())
                            .newsDate(n.getNewsDate())
                            .newsTime(n.getNewsTime())
                            .newsType(n.getNewsType().name())
                            .newsTypeCode(n.getNewsType().getCode())
                            .build()
            );
        }

        for (News n:
                newsContentPage) {
            newsList.add(
                    NewsListDto.builder()
                            .newsId(n.getNewsId())
                            .newsTitle(n.getNewTitle())
                            .newsContent(n.getNewsContent())
                            .newsSource(n.getNewsSource())
                            .newsReporter(n.getNewsReporter())
                            .newsPress(n.getNewsPress())
                            .newsThumbnail(n.getNewsThumbnail())
                            .newsDate(n.getNewsDate())
                            .newsTime(n.getNewsTime())
                            .newsType(n.getNewsType().name())
                            .newsTypeCode(n.getNewsType().getCode())
                            .build()
            );
        }

        if(reqNewsSearch.getNewsPressList()!=null){
            List<String> press = reqNewsSearch.getNewsPressList().stream().map(p -> p.getNewsPress()).collect(Collectors.toList());
            newsList = newsList.stream()
                    .filter(news -> press.contains(news.getNewsPress()))
                    .collect(Collectors.toList());
//            newsTitleAndNewsContentPage = newsTitleAndNewsContentPage.stream()
//                    .filter(news -> press.contains(news.getNewsPress()))
//                    .collect(Collectors.toList());
//            newsTitlePage = newsTitlePage.stream()
//                    .filter(news -> press.contains(news.getNewsPress()))
//                    .collect(Collectors.toList());
//            newsContentPage = newsContentPage.stream()
//                    .filter(news -> press.contains(news.getNewsPress()))
//                    .collect(Collectors.toList());
        }

        if(reqNewsSearch.getNewsStartDt() != null && reqNewsSearch.getNewsEndDt() != null){
            Comparator<NewsListDto> comparator = Comparator.comparing(NewsListDto::getNewsDate)
                    .thenComparing(NewsListDto::getNewsTime).reversed();

            newsList = newsList.stream()
                    .filter(news ->
                            news.getNewsDate().isAfter(reqNewsSearch.getNewsStartDt())
                                    && news.getNewsDate().isBefore(reqNewsSearch.getNewsEndDt()))
                    .sorted(comparator)
                    .collect(Collectors.toList());

//            newsTitleAndNewsContentPage = newsTitleAndNewsContentPage.stream()
//                    .filter(news ->
//                            news.getNewsDate().isAfter(reqNewsSearch.getNewsStartDt())
//                                    && news.getNewsDate().isBefore(reqNewsSearch.getNewsEndDt()))
//                    .sorted(comparator)
//                    .collect(Collectors.toList());
//
//            newsTitlePage = newsTitlePage.stream()
//                    .filter(news ->
//                            news.getNewsDate().isAfter(reqNewsSearch.getNewsStartDt())
//                                    && news.getNewsDate().isBefore(reqNewsSearch.getNewsEndDt()))
//                    .sorted(comparator)
//                    .collect(Collectors.toList());
//
//            newsContentPage = newsContentPage.stream()
//                    .filter(news ->
//                            news.getNewsDate().isAfter(reqNewsSearch.getNewsStartDt())
//                                    && news.getNewsDate().isBefore(reqNewsSearch.getNewsEndDt()))
//                    .sorted(comparator)
//                    .collect(Collectors.toList());
        }


//        List<RedisNews> rn = redisService.getValues(reqNewsSearch.getWord());
//        for (RedisNews r:
//             rn) {
//            LOGGER.info("id : {}",r.getNewsId());
//        }

        Long totalCount = newsList.stream().count();
        List<NewsListDto> list = newsList.stream()
                .skip(reqNewsSearch.getOffset() * reqNewsSearch.getLimit())
                .limit(reqNewsSearch.getLimit())
                .collect(Collectors.toList());
        LOGGER.info("skip : {}" , list.size());


        Integer totalPage = 0;
        if(reqNewsSearch.getLimit() != null && reqNewsSearch.getLimit() != 0){
            totalPage = totalCount%reqNewsSearch.getLimit() == 0 ? (int)(totalCount/reqNewsSearch.getLimit()) : (int)(totalCount/ reqNewsSearch.getLimit()) + 1;
        }

        resNewsSearch.setList(list);
        resNewsSearch.setTotalPage(totalPage);
        resNewsSearch.setTotalCount(totalCount);

        return resNewsSearch;
    }

    @Transactional
    public List<CountPerPressDto> newsCount(ReqNewsSearch reqNewsSearch) {
        ResNewsSearch resNewsSearch = new ResNewsSearch();

        StringBuilder sb = new StringBuilder();

//        PageRequest pageRequest =PageRequest.of(reqNewsSearch.getOffset(), reqNewsSearch.getLimit(), Sort.by("newsDate","newsTime").descending());
        Sort sort = Sort.by(
                Sort.Order.desc("newsDate"),
                Sort.Order.desc("newsTime")
        );
        if(!redisService.checkKeys(reqNewsSearch.getWord())){
            LOGGER.info("Error : {}", "NO REDIS");
            return null;
        }

        List<News> newsTitleAndNewsContentPage = redisService.getValues(reqNewsSearch.getWord(), 0);
        if(newsTitleAndNewsContentPage == null) newsTitleAndNewsContentPage= new ArrayList<>();
        List<News> newsTitlePage = redisService.getValues(reqNewsSearch.getWord(), 1);
        if(newsTitlePage == null) newsTitlePage = new ArrayList<>();
        List<News> newsContentPage = redisService.getValues(reqNewsSearch.getWord(), 2);
        if(newsContentPage == null) newsContentPage = new ArrayList<>();

        if(reqNewsSearch.getType() == 1){
            newsContentPage.clear();
        }else if(reqNewsSearch.getType() == 2){
            newsTitlePage.clear();
        }

        if(reqNewsSearch.getNewsStartDt() != null && reqNewsSearch.getNewsEndDt() != null){
            newsTitleAndNewsContentPage = newsTitleAndNewsContentPage.stream()
                    .filter(news ->
                            news.getNewsDate().isAfter(reqNewsSearch.getNewsStartDt())
                                    && news.getNewsDate().isBefore(reqNewsSearch.getNewsEndDt()))
                    .sorted(Comparator.comparing(News::getNewsDate).reversed())
                    .sorted(Comparator.comparing(News::getNewsTime).reversed())
                    .collect(Collectors.toList());

            newsTitlePage = newsTitlePage.stream()
                    .filter(news ->
                            news.getNewsDate().isAfter(reqNewsSearch.getNewsStartDt())
                                    && news.getNewsDate().isBefore(reqNewsSearch.getNewsEndDt()))
                    .sorted(Comparator.comparing(News::getNewsDate).reversed())
                    .sorted(Comparator.comparing(News::getNewsTime).reversed())
                    .collect(Collectors.toList());

            newsContentPage = newsContentPage.stream()
                    .filter(news ->
                            news.getNewsDate().isAfter(reqNewsSearch.getNewsStartDt())
                                    && news.getNewsDate().isBefore(reqNewsSearch.getNewsEndDt()))
                    .sorted(Comparator.comparing(News::getNewsDate).reversed())
                    .sorted(Comparator.comparing(News::getNewsTime).reversed())
                    .collect(Collectors.toList());
        }

        List<News> newsList = new ArrayList<>();
        newsList.addAll(newsTitleAndNewsContentPage);
        newsList.addAll(newsTitlePage);
        newsList.addAll(newsContentPage);

        List<NewsPressMapping> pressList = newsRepository.findDistinctBy();

        List<CountPerPressDto> countPerPressList = pressList.stream()
                .map(p -> new CountPerPressDto(p.getNewsPress(), newsList.stream().filter(n -> n.getNewsPress().equals(p.getNewsPress())).count()))
                .sorted(Comparator.comparing(CountPerPressDto::getCount).reversed())
                .collect(Collectors.toList());

        return countPerPressList;
    }

    @Transactional
    public ResShortcut shortcut(Long newsId){
        Optional<Shortcut> shortcut = shortcutRepository.findByNewsNewsId(newsId);
        ResShortcut resShortcut = null;
        if(shortcut.isPresent()){
            resShortcut = ResShortcut.builder()
                    .shortcutId(shortcut.get().getShortcutId())
                    .content1st(shortcut.get().getContent1st())
                    .content2nd(shortcut.get().getContent2nd())
                    .content3rd(shortcut.get().getContent3rd())
                    .build();
        }
        
        return resShortcut;
    }

    @Transactional
    public List<ResNews> dailyNews(){
        // keywordId 10개 받아오기
        ObjectMapper om = new ObjectMapper();
        List<ResNewsKeyword> list = om.convertValue(keywordServiceClient.newsKeyword().getBody(), new TypeReference<List<ResNewsKeyword>>() {});
        List<ResNews> newsList = list.stream()
                .map( k -> {
                    Optional<NewsKeyword> newsKeyword = newsKeywordRepository.findTop1ByNewsKeywordPKKeywordId(k.getKeywordId());
                    ResNews resNews = new ResNews();
                    if (newsKeyword.isPresent()) {

//                        Optional<News> N = newsKeyword.stream()
//                                .map(n -> n.getNewsKeywordPK().getNews())
//                                .filter(n -> n.getNewsDate().equals(LocalDate.of(2023,3,31)))
//                                .findFirst();
                        News news = newsKeyword.get().getNewsKeywordPK().getNews();
                        resNews = ResNews.builder()
                                .newsContent(news.getNewsContent())
                                .newsThumbnail(news.getNewsThumbnail())
                                .newsDate(news.getNewsDate())
                                .newsTitle(news.getNewTitle())
                                .newsType(news.getNewsType().name())
                                .newsTime(news.getNewsTime())
                                .newsSource(news.getNewsSource())
                                .newsId(news.getNewsId())
                                .newsPress(news.getNewsPress())
                                .newsReporter(news.getNewsReporter())
                                .newsTypeCode(news.getNewsType().getCode())
                                .build();

                    }
                    return resNews;
                }).collect(Collectors.toList());
        return newsList;
    }

    @Transactional
    public ResNewsSearch clusterNews(ReqCluster reqCluster){
        // 클러스터별 news 리스트 불러오기
        ObjectMapper om = new ObjectMapper();
        ClusterPageDto clusterPageDto = om.convertValue(timelineServiceClient.clusterNews(reqCluster).getBody(), ClusterPageDto.class);
        List<NewsIdDto> list = clusterPageDto.getList();
        List<Long> ids = list.stream().map(n -> n.getNewsId()).collect(Collectors.toList());
        List<News> news = newsRepository.findByNewsIdIn(ids);

        List<NewsListDto> newsList = news.stream()
                .map( n -> NewsListDto.builder()
                        .newsContent(n.getNewsContent())
                        .newsThumbnail(n.getNewsThumbnail())
                        .newsDate(n.getNewsDate())
                        .newsTitle(n.getNewTitle())
                        .newsType(n.getNewsType().name())
                        .newsTime(n.getNewsTime())
                        .newsSource(n.getNewsSource())
                        .newsId(n.getNewsId())
                        .newsPress(n.getNewsPress())
                        .newsReporter(n.getNewsReporter())
                        .newsTypeCode(n.getNewsType().getCode())
                        .build())
                .collect(Collectors.toList());
        ResNewsSearch resNewsSearch = new ResNewsSearch();
        resNewsSearch.setTotalCount(clusterPageDto.getTotalCount());
        resNewsSearch.setTotalPage(clusterPageDto.getTotalPage());
        resNewsSearch.setList(newsList);
        return resNewsSearch;
    }
}
