/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeParserFullQaTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.localgov.support.LocalGovernmentNoticeUrlValidator;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeParserProfileRow;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "SANEB_LOCAL_GOV_PARSER_QA", matches = "true")
class LocalGovernmentNoticeParserFullQaTest {

    private static final int EXPECTED_SOURCE_COUNT = 244;
    private static final int QA_CONCURRENCY = 4;
    private static final int MAX_TRANSPORT_ATTEMPTS = 3;
    private static final int SESSION_BOOTSTRAP_ATTEMPTS = 4;
    private static final long RETRY_DELAY_MILLIS = 750L;
    private static final int MAX_RESPONSE_BYTES = 3 * 1024 * 1024;
    private static final String BROWSER_COMPATIBLE_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final Pattern SOURCE_INSERT_PATTERN = Pattern.compile(
            "INSERT INTO local_government_notice_sources\\s*\\(.*?\\) VALUES \\((.*?)\\) ON CONFLICT DO NOTHING;",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern SQL_STRING_PATTERN = Pattern.compile("'((?:''|[^'])*)'");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(20\\d{2})[.\\-/년\\s]+(\\d{1,2})[.\\-/월\\s]+(\\d{1,2})"
    );
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{2})[.\\-/\\s]+(\\d{1,2})[.\\-/\\s]+(\\d{1,2})(?!\\d)"
    );
    private static final Pattern TIME_ONLY_PATTERN = Pattern.compile(
            "^\\s*(?:[01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?\\s*$"
    );
    private static final Pattern SCRIPT_PATH_PATTERN = Pattern.compile(
            "['\"]((?:https?://|/|\\./|\\.\\./)[^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REVIEWED_URL_PATTERN = Pattern.compile(
            "\\(\\s*'(LGS-\\d{6})',\\s*'(https?://[^']+)'"
    );
    private static final Pattern BROWSER_HTTP1_BLOCK_PATTERN = Pattern.compile(
            "SET request_profile_code = 'BROWSER_HTTP1'.*?WHERE public_code IN \\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern LEGACY_BROWSER_BLOCK_PATTERN = Pattern.compile(
            "SET request_profile_code = 'LEGACY_BROWSER'.*?WHERE public_code IN \\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TLS12_BROWSER_BLOCK_PATTERN = Pattern.compile(
            "SET request_profile_code = 'TLS12_BROWSER'.*?WHERE public_code IN \\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern SESSION_BROWSER_BLOCK_PATTERN = Pattern.compile(
            "SET request_profile_code = 'SESSION_BROWSER'.*?WHERE public_code IN \\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern COLLECTION_ENDPOINT_PATTERN = Pattern.compile(
            "SET collection_endpoint_url = '([^']+)'.*?WHERE public_code = '(LGS-\\d{6})'",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern NON_NOTICE_TITLE_PATTERN = Pattern.compile(
            "^(홈|로그인|로그아웃|회원가입|검색|목록|이전|다음|처음|끝|더보기|전체보기|바로가기|사이트맵)$"
    );
    private static final Pattern FILE_LINK_PATTERN = Pattern.compile(
            ".*\\.(pdf|hwp|hwpx|doc|docx|xls|xlsx|ppt|pptx|zip|jpg|jpeg|png|gif)(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NON_NOTICE_LINK_PATTERN = Pattern.compile(
            ".*(download|filedown|rss|login|logout|sitemap).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LEGAL_SOURCE_URL_PATTERN = Pattern.compile(
            ".*(eminwon|emiryangminwon|emwp|emws|saeol/gosi|selectgosi|/gosi([/.?]|$)|publicnotice|searchgosi"
                    + "|ofraction|notancmt|not_ancmt|seolcontent|section=gosi|bcd=gosi"
                    + "|DOM_000008902001002001|main/news/announce\\.jsp"
                    + "|sc/portal/sokchonews/notification).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PRESS_BOARD_PATTERN = Pattern.compile(
            ".*(보도자료|언론보도|브리핑).*"
    );
    private static final Pattern LEGAL_BOARD_PATTERN = Pattern.compile(
            ".*(고시.?공고|고시공고|입법.?행정예고|새올전자민원).*"
    );
    private static final Pattern SUPPORT_BOARD_PATTERN = Pattern.compile(
            ".*(지원사업|사업.?공고|지원.?모집|소상공인|중소기업|창업).*"
    );
    private static final Pattern DETAIL_LINK_PATTERN = Pattern.compile(
            ".*(view|detail|selectbbsnttview|dataSid=|nttId=|nttNo=|articleId=|articleNo=|boardSeq=|bbsSeq=|jsb_key=|[?&](seq|idx|no|id)=\\d+|/\\d{3,}(?:[/?#].*)?$).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STRUCTURED_CONTAINER_PATTERN = Pattern.compile(
            ".*(item|list|board|notice|bbs|row|card|post|subject|content|cont|box|unit).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final List<String> LINK_ATTRIBUTE_NAMES = List.of(
            "href", "data-url", "data-href", "data-action", "data-link", "data-view-url"
    );
    private static final Map<String, String> POST_FORM_BODIES = Map.of(
            "LGS-000089",
            "pageIndex=&jndinm=OfrNotAncmtEJB&context=NTIS&method=selectListOfrNotAncmt"
                    + "&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_mgt_no=&homepage_pbs_yn=Y"
                    + "&subCheck=Y&not_ancmt_se_code=01%2C02%2C03%2C04%2C05%2C06%2C07"
                    + "&title=%EA%B3%A0%EC%8B%9C%EA%B3%B5%EA%B3%A0&cha_dep_code_nm=&initValue="
                    + "&countYn=Y&list_gubun=&not_ancmt_sj=&cgg_code=&not_ancmt_cn=&dept_nm="
                    + "&epcCheck=Y&yyyy=&nodate_recent_mm=&ofr_pageSize=10&Key=B_Subject&temp="
            ,
            "LGS-000239",
            "pageIndex=1&jndinm=OfrNotAncmtEJB&context=NTIS&method=selectListOfrNotAncmt"
                    + "&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_mgt_no=&homepage_pbs_yn=Y"
                    + "&subCheck=Y&not_ancmt_se_code=01%2C02%2C03%2C04%2C07"
                    + "&title=%EA%B3%A0%EC%8B%9C%EA%B3%B5%EA%B3%A0&cha_dep_code_nm=&initValue="
                    + "&countYn=Y&list_gubun=&not_ancmt_sj=&ofr_pageSize=10&yyyy="
    );
    private static final List<ParserProfile> PROFILES = List.of(
            new ParserProfile("SAEOL_GOSI", "table tbody tr", "td a", "td:last-child", "td a", "yyyy-MM-dd", "AUTO", null, null, null),
            new ParserProfile("SPRING_BBS", "table tbody tr", "td a", "td:nth-last-child(2)", "td a", "yyyy-MM-dd", "AUTO", null, null, null),
            new ParserProfile("JSP_BBS", "table tbody tr", "td a", "td:nth-last-child(2)", "td a", "yyyy.MM.dd", "AUTO", null, null, null),
            new ParserProfile("TC_GOSI", "table tbody tr", "td a", "td:nth-last-child(2)", "td a", "yyyy-MM-dd", "AUTO", null, null, null),
            new ParserProfile("GENERIC_TABLE", "table tbody tr", "td a", "td:last-child", "td a", null, "AUTO", null, null, null),
            new ParserProfile("GENERIC_LIST", "ul li, ol li", "a", "time, .date", "a", null, "AUTO", null, null, null),
            new ParserProfile(
                    "SAFE_BOARD_VIEW", "table tbody tr",
                    "td.title a, td.list_tit a, td a[onclick*=boardView]", "td.date, td.list_date",
                    "td.title a, td.list_tit a, td a[onclick*=boardView]", null,
                    "SAFE_TEMPLATE", "boardView", 7,
                    "/portal/bbs/view.do?mId={arg:6}&bIdx={arg:4}&ptIdx={arg:5}"
            ),
            new ParserProfile(
                    "SAFE_BOARD_VIEW_SITE", "table tbody tr",
                    "td.title a, td.list_tit a, td a[onclick*=boardView]", "td.date, td.list_date",
                    "td.title a, td.list_tit a, td a[onclick*=boardView]", null,
                    "SAFE_TEMPLATE", "boardView", 8,
                    "/portal/bbs/view.do?mId={arg:7}&bIdx={arg:5}&ptIdx={arg:6}"
            ),
            new ParserProfile(
                    "SAFE_YH_BOARD_POST", "table tbody tr",
                    "td.list_tit a[data-req-get-p-idx], td a[data-req-get-p-idx]", "td.list_date, td.date",
                    "td.list_tit a[data-req-get-p-idx], td a[data-req-get-p-idx]", null,
                    "SAFE_TEMPLATE", null, null,
                    "/portal/board/post/view.do?bcIdx={query:bcIdx}&mid={query:mid}&idx={attr:data-req-get-p-idx}"
            ),
            new ParserProfile(
                    "SAFE_ICMS_BOARD", "table tbody tr",
                    "td a[onclick*=fn_icms_navi_common]",
                    "td.regdate, td:nth-of-type(4), td[data-table-type=date]",
                    "td a[onclick*=fn_icms_navi_common]", null,
                    "SAFE_TEMPLATE", "fn_icms_navi_common", 2,
                    "/index.do?menu_id={query:menu_id}&menu_link=/icms/bbs/selectBoardArticle.do"
                            + "&bbsId={input:bbsId}&nttId={arg:2}"
            ),
            new ParserProfile(
                    "SAFE_OPENWORKS_BOARD", "table tbody tr", "td a[onclick*=jsView]",
                    "td:nth-last-child(2)", "td a[onclick*=jsView]", null,
                    "SAFE_TEMPLATE", "jsView", 4,
                    "/web/board/BD_board.view.do?bbsCd={arg:1}&seq={arg:2}"
            ),
            new ParserProfile(
                    "SAFE_BD_SELECT_BBS", "table tbody tr",
                    "td.subject a[onclick*=fnView], td a[onclick*=fnView]", "td.date, td:nth-last-child(3)",
                    "td.subject a[onclick*=fnView], td a[onclick*=fnView]", null,
                    "SAFE_TEMPLATE", "fnView", 6,
                    "/www/user/bbs/BD_selectBbs.do?q_bbsCode={arg:1}&q_bbscttSn={arg:2}"
                            + "&q_currPage={arg:5}&q_pClCode={arg:6}"
            ),
            new ParserProfile(
                    "SAFE_ICMS_BOARD_EXTENDED", "table tbody tr",
                    "td a[onclick*=fn_icms_navi_common]",
                    "td.regdate, td:nth-of-type(4), td[data-table-type=date]",
                    "td a[onclick*=fn_icms_navi_common]", null,
                    "SAFE_TEMPLATE", "fn_icms_navi_common", 5,
                    "/index.do?menu_id={query:menu_id}&menu_link=/icms/bbs/selectBoardArticle.do"
                            + "&bbsId={input:bbsId}&nttId={arg:2}"
            ),
            new ParserProfile(
                    "SAFE_GOTO_VIEW", "table tbody tr", "td a[onclick*='goTo.view']",
                    "td.list_date, td.date", "td a[onclick*='goTo.view']", null,
                    "SAFE_TEMPLATE", "goTo.view", 4,
                    "view.do?mId={arg:4}&bIdx={arg:2}&ptIdx={arg:3}"
            ),
            new ParserProfile(
                    "SAFE_GOTO_VIEW_EXTENDED", "table tbody tr", "td a[onclick*='goTo.view']",
                    "td.list_date, td.date", "td a[onclick*='goTo.view']", null,
                    "SAFE_TEMPLATE", "goTo.view", 5,
                    "view.do?mId={arg:4}&bIdx={arg:2}&ptIdx={arg:3}"
            ),
            new ParserProfile(
                    "SAFE_YANGCHEON_SEOL", "table.basic-list tbody tr",
                    "td.subject a[href*=doSeolContentDeailView]", "td:nth-of-type(5)",
                    "td.subject a[href*=doSeolContentDeailView]", null,
                    "SAFE_TEMPLATE", "doSeolContentDeailView", 1,
                    "seolContentDeailView.do?not_ancmt_mgt_no={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_ANSAN_BBS", "table tbody tr", "td.p-subject a[onclick*=fnGoDetail]",
                    "td:nth-of-type(4)", "td.p-subject a[onclick*=fnGoDetail]", null,
                    "SAFE_TEMPLATE", "fnGoDetail", 1,
                    "/www/common/bbs/selectBbsDetail.do?bbs_code={query:bbs_code}&bbs_seq={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_GWD_BULLETIN", "table tbody tr", "td.skinTb-sbj a[onclick*=goPage]",
                    "td.skinTb-date", "td.skinTb-sbj a[onclick*=goPage]", null,
                    "SAFE_TEMPLATE", "goPage", 1, "/portal/bulletin/notice?articleSeq={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_SANGJU_GOSI", "table tbody tr", "td.tal a[onclick*=fnDetail]",
                    "td:nth-of-type(5)", "td.tal a[onclick*=fnDetail]", null,
                    "SAFE_TEMPLATE", "fnDetail", 1, "/gosi/detail.tc?mn={input:mn}&mgtNo={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_GORYEONG_BOARD", "table tbody tr",
                    "td.bL_tableTitle a[onclick*=fn_articleLink]", "td:nth-of-type(4)",
                    "td.bL_tableTitle a[onclick*=fn_articleLink]", null,
                    "SAFE_TEMPLATE", "fn_articleLink", 1,
                    "/kor/boardView.do?IDX={query:IDX}&BRD_ID={query:BRD_ID}&BOARD_IDX={arg:1}&page=1"
            ),
            new ParserProfile(
                    "SAFE_SEOUL_NOTICE", "table tbody tr",
                    "td.sib-lst-type-basic-subject a[href*=fnTbbsView]", "td:nth-of-type(4)",
                    "td.sib-lst-type-basic-subject a[href*=fnTbbsView]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "fnTbbsView", 1,
                    "/news/news_notice.do?bbsId={query:bbsId}&bbsNo={query:bbsNo}&nttNo={arg:1}"
            ),
            new ParserProfile(
                    "JUNGGU_NOTICE_TABLE", "div.board_list table tbody tr",
                    "td.tal p.title a", "td:nth-of-type(3)", "td.tal p.title a", null,
                    "AUTO", null, null, null
            ),
            new ParserProfile(
                    "DOBONG_NOTICE_TABLE", "table tbody tr",
                    "td[data-cell-header='제목'] a", "td[data-cell-header='등록일']",
                    "td[data-cell-header='제목'] a", "yyyy.MM.dd", "AUTO", null, null, null
            ),
            new ParserProfile(
                    "NOWON_NOTICE_TABLE", "table tbody tr", "td.cell-subject a", "td.cell-date",
                    "td.cell-subject a", "yyyy-MM-dd", "AUTO", null, null, null
            ),
            new ParserProfile(
                    "SAFE_SEODAEMUN_NOTICE", "table.boardList tbody tr", "td.aleft a[href*=goView]",
                    "td:nth-of-type(4)", "td.aleft a[href*=goView]", "yyyy.MM.dd",
                    "SAFE_TEMPLATE", "goView", 1,
                    "/news/notice/notice.do?mode=view&sdmBoardSeq={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_GWANAK_NOTICE", "table.list tbody tr",
                    "td:nth-of-type(3) a[onclick*=doBbsFView]", "td:nth-of-type(7)",
                    "td:nth-of-type(3) a[onclick*=doBbsFView]", "yyyyMMdd",
                    "SAFE_TEMPLATE", "doBbsFView", 1,
                    "/site/gwanak/ex/bbsNew/View.do?not_ancmt_mgt_no={arg:1}&typeCode={query:typeCode}"
            ),
            new ParserProfile(
                    "SAFE_SAEOL_EMINWON", "table tbody tr",
                    "td:nth-of-type(3) a[onclick*=searchDetail]", "td:nth-of-type(5)",
                    "td:nth-of-type(3) a[onclick*=searchDetail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "searchDetail", 1,
                    "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y"
                            + "&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                            + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}&subCheck=Y"
            ),
            new ParserProfile(
                    "SAFE_SAEOL_EMINWON_LEGACY",
                    "table[summary*=고시공고] tr:has(td:nth-of-type(3) a[onclick*=searchDetail])",
                    "td:nth-of-type(3) a[onclick*=searchDetail]", "td:nth-of-type(5)",
                    "td:nth-of-type(3) a[onclick*=searchDetail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "searchDetail", 1,
                    "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y"
                            + "&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                            + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}"
                            + "&subCheck={query:subCheck}"
            ),
            new ParserProfile(
                    "SAFE_SAEOL_EMINWON_COMPACT",
                    "table.board1 tbody tr:has(td:nth-of-type(2) a[onclick*=searchDetail])",
                    "td:nth-of-type(2) a[onclick*=searchDetail]", "td:nth-of-type(4)",
                    "td:nth-of-type(2) a[onclick*=searchDetail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "searchDetail", 1,
                    "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y"
                            + "&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                            + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}&subCheck=Y"
            ),
            new ParserProfile(
                    "SAFE_SAEOL_EMINWON_HREF",
                    "table.table1 tbody tr:has(td.title a[href*=searchDetail])",
                    "td.title a[href*=searchDetail]", "td:nth-of-type(5)",
                    "td.title a[href*=searchDetail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "searchDetail", 1,
                    "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y"
                            + "&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                            + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}"
                            + "&subCheck={query:subCheck}"
            ),
            new ParserProfile(
                    "SAFE_EGOV_DETAIL_BUTTON",
                    "table tbody tr:has(td.subject button[onclick*=fn_search_detail])",
                    "td.subject button[onclick*=fn_search_detail]", "td.regDate",
                    "td.subject button[onclick*=fn_search_detail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "fn_search_detail", 1, "view.do?nttId={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_EGOV_DETAIL_CELL",
                    "table.table-default tbody tr:has(td.subject a[onclick*=fn_search_detail])",
                    "td.subject a[onclick*=fn_search_detail]", "td[data-cell-header=\"등록일\"]",
                    "td.subject a[onclick*=fn_search_detail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "fn_search_detail", 1, "view.do?notAncmtMgtNo={arg:1}"
            ),
            new ParserProfile(
                    "JUNGNANG_CONTEST_BOARD",
                    "table.inc_head tbody tr.noticeTitlte",
                    "td.tit a[href*='/portal/bbs/view/']", "td:nth-of-type(3)",
                    "td.tit a[href*='/portal/bbs/view/']", "yyyy-MM-dd",
                    "AUTO", null, null, null
            ),
            new ParserProfile(
                    "SAFE_PYEONGTAEK_BOARD_RENEWAL",
                    "table tbody tr:has(td.col_title a[onclick*=boardViewRenewal])",
                    "td.col_title a[onclick*=boardViewRenewal]", "td.col_date",
                    "td.col_title a[onclick*=boardViewRenewal]", "yyyy.MM.dd",
                    "SAFE_TEMPLATE", "boardViewRenewal", 7,
                    "/pyeongtaek/board/post/view.do?bcIdx={arg:4}&idx={arg:5}&mid={arg:6}"
            ),
            new ParserProfile(
                    "SAFE_EGOV_BOARD_BUTTON",
                    "table tbody tr:has(td.board__table--title button[onclick*=fn_search_detail])",
                    "td.board__table--title button[onclick*=fn_search_detail]", "td.board__table--date",
                    "td.board__table--title button[onclick*=fn_search_detail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "fn_search_detail", 1, "view.do?nttId={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_EGOV_DATA_BUTTON",
                    "table tbody tr:has(td.board__table--title button[data-ntt-id])",
                    "td.board__table--title button[data-ntt-id]", "td.board__table--date",
                    "td.board__table--title button[data-ntt-id]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", null, null, "view.do?nttId={attr:data-ntt-id}"
            ),
            new ParserProfile(
                    "RFC_BLOGLIST_NOTICE", "div.bloglist-wrap > ul > li:has(a[href*=dataSid])",
                    "span.btxt", "span.date", "a[href*=dataSid]", "yyyy.MM.dd",
                    "AUTO", null, null, null
            ),
            new ParserProfile(
                    "GURYE_BOARD_NOTICE", "table tbody tr:has(td.tit a[href*=nttId])",
                    "td.tit a[href*=nttId]", "td.date", "td.tit a[href*=nttId]", "yyyy-MM-dd",
                    "AUTO", null, null, null
            ),
            new ParserProfile(
                    "GUNWI_NOTICE_TABLE", "table.tbl_board tbody tr", "td.subject a", "td.date",
                    "td.subject a", "yyyy-MM-dd", "AUTO", null, null, null
            ),
            new ParserProfile(
                    "SAFE_EGOV_DETAIL", "table tbody tr", "td.subject a[onclick*=fn_search_detail]",
                    "td.regDate", "td.subject a[onclick*=fn_search_detail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "fn_search_detail", 1, "view.do?nttId={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_DONGGU_ARTICLE", "div.notice_list > ul > li:not(.thead)",
                    "p.subject a[onclick*='article.view']", "p.date",
                    "p.subject a[onclick*='article.view']", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "article.view", 1, "newsNotice/{arg:1}"
            ),
            new ParserProfile(
                    "SAFE_PAJU_SUMMARY", "ul.summary-list > li", "a.contentTip strong.subject",
                    "ul.article-info > li:last-child", "a.contentTip[onclick*=jsView]", null,
                    "SAFE_TEMPLATE", "jsView", 4, "BD_board.view.do?bbsCd={arg:1}&seq={arg:2}"
            ),
            new ParserProfile(
                    "SAFE_GWD_GOPAGE2", "table tbody tr", "td.skinTb-sbj a[href*=goPage2]",
                    "td.skinTb-date", "td.skinTb-sbj a[href*=goPage2]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "goPage2", 1,
                    "/portal/openadmin/adminnews/notice?articleSeq={arg:1}"
            ),
            new ParserProfile(
                    "SAFE_YANGYANG_READ", "table tbody tr", "td.skinTb-sbj a[onclick*=pf_readForm]",
                    "td.skinTb-date", "td.skinTb-sbj a[onclick*=pf_readForm]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "pf_readForm", 1,
                    "/gw/portal/yyc_news_notice?mode=readForm"
                            + "&boardCode={input:boardCode}&articleSeq={arg:1}"
            ),
            new ParserProfile(
                    "EGOV_DIRECT_NOTICE_TABLE", "table tbody tr", ".list_subject a", "td.date",
                    ".list_subject a", "yyyy-MM-dd", "AUTO", null, null, null
            ),
            new ParserProfile(
                    "SUBJECT_NOTICE_TABLE", "table tbody tr",
                    "td.subject > a, td.title > a, td.bb-list-title > a",
                    "td.date, td.bb-list-publish-date, td:nth-of-type(4)",
                    "td.subject > a, td.title > a, td.bb-list-title > a",
                    null, "AUTO", null, null, null
            ),
            new ParserProfile(
                    "SEONGBUK_EMINWON_TABLE",
                    "table.p-table.simple tbody.text_center > tr:has(> td.p-subject > a)",
                    "td.p-subject > a", "td:last-child", "td.p-subject > a", "yyyy-MM-dd",
                    "AUTO", null, null, null
            ),
            new ParserProfile(
                    "CHANGWON_GOSI_TABLE",
                    "table.t3 tbody.tb > tr:has(> td.tal > a.a1)",
                    "td.tal > a.a1", "td:nth-last-child(2)", "td.tal > a.a1", "yyyy-MM-dd",
                    "AUTO", null, null, null
            ),
            new ParserProfile(
                    "SCMS_CARD_NOTICE", "ul.lst1 > li.li1", "strong.t1",
                    ".wrap1t3 > span.t3:first-child, .t3wrap > span.t3:nth-child(2)",
                    "a.a1", null, "AUTO", null, null, null
            ),
            new ParserProfile(
                    "RFC3_BOARD_NOTICE", "table tbody tr",
                    "td.title > a[href*='/board/view.']",
                    "td[data-cell-header='작성일'], td.date, td:nth-of-type(4)",
                    "td.title > a[href*='/board/view.']", null,
                    "AUTO", null, null, null
            ),
            new ParserProfile(
                    "SAFE_SAEOL_EMINWON_CELL",
                    "tr:has(> td:nth-of-type(3)[onclick*=searchDetail])",
                    "td:nth-of-type(3)[onclick*=searchDetail]", "td:nth-of-type(5)",
                    "td:nth-of-type(3)[onclick*=searchDetail]", "yyyy-MM-dd",
                    "SAFE_TEMPLATE", "searchDetail", 1,
                    "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS"
                            + "&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                            + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}&subCheck=Y"
            ),
            new ParserProfile(
                    "DAEJEON_EMINWON_AGGREGATOR",
                    "table tbody tr:has(td.subject a[onclick*=popupCenterNew])",
                    "td.subject a[onclick*=popupCenterNew]", "td.date",
                    "td.subject a[onclick*=popupCenterNew]", "yyyy-MM-dd",
                    "AUTO", null, null, null
            ),
            new ParserProfile("HEURISTIC_NOTICE", null, null, null, null, null, "AUTO", null, null, null)
    );
    private static final Map<String, JsonQaProfile> JSON_PROFILES = Map.ofEntries(
            Map.entry("LGS-000117", new JsonQaProfile(
                    "CHUNCHEON_NOTICE_JSON", "noticeList", "notAncmtSj", "pbsHopYmd", "notAncmtMgtNo",
                    "/cityhall/administrative-info/notice-info/notice-announcement/view/?notAncmtMgtNo={value}"
            )),
            Map.entry("LGS-000183", new JsonQaProfile(
                    "DAMYANG_NOTICE_JSON", "RSLT_DATA.boardContentsList", "dataTitle", "registerDate", "dataSid",
                    "/board/detail?dataSid={value}&boardId=BBS_0000001&domainId=DOM_0000001"
                            + "&contentsSid=1&menuCd=DOM_000000190001001000"
            ))
    );

    private final SSLContext windowsSslContext = selectWindowsSslContext();
    private final HttpClient defaultHttpClient = createHttpClient(null);
    private final HttpClient browserHttp1Client = createHttpClient(HttpClient.Version.HTTP_1_1);
    private final HttpClient tls12HttpClient = createTls12HttpClient();
    private final CookieManager sessionCookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient sessionBrowserHttpClient = createSessionHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path qaOutputDirectory = Path.of(System.getProperty(
            "saneb.local-gov-qa.output-dir",
            "build/reports/local-government-parser-qa"
    ));
    private final LocalGovernmentNoticeCollector parserSupport = new LocalGovernmentNoticeCollector(
            new LocalGovernmentNoticeUrlValidator(),
            new AnnouncementSourceIdentityNormalizer(),
            objectMapper,
            1000,
            MAX_RESPONSE_BYTES,
            1,
            "saneB-parser-qa"
    );
    private final Map<String, Semaphore> domainSemaphores = new ConcurrentHashMap<>();

    /**
     * 정적 seed의 모든 지자체 URL을 한 번씩 요청하고 파서 후보별 추출 결과를 보고서로 저장합니다.
     *
     * @throws Exception URL 검사 또는 보고서 저장 실패
     */
    @Test
    void inspectAllSeededLocalGovernmentNoticeSources() throws Exception {
        List<SourceSeed> sources = selectSourceSeedList();
        Set<String> requestedSourceCodes = selectRequestedSourceCodes();
        if (!requestedSourceCodes.isEmpty()) {
            sources = sources.stream()
                    .filter(source -> requestedSourceCodes.contains(source.publicCode()))
                    .toList();
            assertThat(sources).hasSize(requestedSourceCodes.size());
        } else {
            assertThat(sources).hasSize(EXPECTED_SOURCE_COUNT);
        }
        if (requestedSourceCodes.isEmpty() || requestedSourceCodes.contains("LGS-000001")) {
            assertThat(sources)
                    .filteredOn(source -> "LGS-000001".equals(source.publicCode()))
                    .singleElement()
                    .extracting(SourceSeed::noticeUrl)
                    .isEqualTo("https://www.seoul.go.kr/news/news_notice.do?bbsId=001&bbsNo=277");
        }

        List<QaResult> results = inspectAll(sources);
        assertThat(results).hasSize(sources.size());
        assertThat(results).extracting(QaResult::publicCode).doesNotHaveDuplicates();

        Files.createDirectories(qaOutputDirectory);
        writeCsv(qaOutputDirectory.resolve("지자체_파서_전수_QA.csv"), results);
        writeSummary(qaOutputDirectory.resolve("지자체_파서_전수_QA_요약.md"), results);
    }

    /**
     * 격리 재현이 필요한 관리코드를 환경변수에서 조회합니다.
     *
     * @return 요청된 관리코드 집합, 전체 검사이면 빈 집합
     */
    private Set<String> selectRequestedSourceCodes() {
        String configuredCodes = System.getenv("SANEB_LOCAL_GOV_PARSER_QA_CODES");
        if (configuredCodes == null || configuredCodes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configuredCodes.split(","))
                .map(String::trim)
                .filter(code -> code.matches("LGS-\\d{6}"))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 지자체 URL을 제한된 병렬도로 검사합니다.
     *
     * @param sources 지자체 URL seed 목록
     * @return 관리코드 순서로 정렬된 QA 결과
     * @throws Exception 병렬 실행 실패
     */
    private List<QaResult> inspectAll(List<SourceSeed> sources) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(QA_CONCURRENCY)) {
            List<Future<QaResult>> futures = sources.stream()
                    .map(source -> executor.submit(() -> inspectWithRetry(source)))
                    .toList();
            List<QaResult> results = new ArrayList<>();
            for (Future<QaResult> future : futures) {
                results.add(future.get());
            }
            return results.stream()
                    .sorted(Comparator.comparing(QaResult::publicCode))
                    .toList();
        }
    }

    /**
     * 일시적 통신 실패만 지연을 두고 최대 세 번 요청하며 파서·의미 오류는 재시도하지 않습니다.
     *
     * @param source 지자체 URL seed
     * @return 최종 QA 결과
     */
    private QaResult inspectWithRetry(SourceSeed source) {
        QaResult result = null;
        for (int attempt = 1; attempt <= MAX_TRANSPORT_ATTEMPTS; attempt++) {
            result = inspect(source);
            if (!isRetryableTransportFailure(result) || attempt == MAX_TRANSPORT_ATTEMPTS) {
                return result;
            }
            try {
                Thread.sleep(RETRY_DELAY_MILLIS * attempt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return result;
            }
        }
        return result;
    }

    /**
     * 재시도로 회복 가능한 네트워크 오류와 제한적인 HTTP 상태인지 확인합니다.
     *
     * @param result 첫 번째 QA 결과
     * @return 재시도 대상이면 true
     */
    private boolean isRetryableTransportFailure(QaResult result) {
        if ("NETWORK_ERROR".equals(result.errorCode())) {
            return true;
        }
        if (!"HTTP_ERROR".equals(result.errorCode()) || result.httpStatus() == null) {
            return false;
        }
        int status = result.httpStatus();
        return status == 408 || status == 425 || status == 429 || status >= 500;
    }

    /**
     * 단일 URL을 요청하고 모든 실행 가능한 정적 파서 결과를 비교합니다.
     *
     * @param source 지자체 URL seed
     * @return 최적 파서와 추출 품질
     */
    private QaResult inspect(SourceSeed source) {
        long startedAt = System.nanoTime();
        URI uri;
        try {
            uri = URI.create(source.requestUrl());
        } catch (RuntimeException exception) {
            return failure(source, "URL_ERROR", null, exception.getMessage(), startedAt);
        }
        Semaphore domainSemaphore = domainSemaphores.computeIfAbsent(
                String.valueOf(uri.getHost()).toLowerCase(Locale.ROOT),
                ignored -> new Semaphore(1)
        );
        try {
            domainSemaphore.acquire();
            if (usesSessionBrowser(source)) {
                int bootstrapStatus = prepareBrowserSession(source);
                if (bootstrapStatus < 200 || bootstrapStatus >= 400) {
                    return failure(
                            source,
                            "HTTP_ERROR",
                            bootstrapStatus,
                            "세션 준비 HTTP " + bootstrapStatus,
                            startedAt
                    );
                }
            }
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", usesBrowserCompatibleRequest(source)
                            ? BROWSER_COMPATIBLE_USER_AGENT : "saneB-notice-collector/1.0")
                    .header("Accept", JSON_PROFILES.containsKey(source.publicCode())
                            ? "application/json,text/plain;q=0.9,*/*;q=0.5"
                            : "text/html,application/xhtml+xml")
                    .header("Accept-Language", "ko-KR,ko;q=0.9");
            if (source.collectionEndpointUrl() != null && !source.collectionEndpointUrl().isBlank()) {
                requestBuilder.header("Referer", source.noticeUrl());
            }
            String postFormBody = POST_FORM_BODIES.get(source.publicCode());
            HttpRequest request = postFormBody == null
                    ? requestBuilder.GET().build()
                    : requestBuilder
                            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                            .POST(HttpRequest.BodyPublishers.ofString(postFormBody))
                            .build();
            QaHttpResponse response = "LEGACY_BROWSER".equals(source.requestProfileCode())
                    ? sendLegacyRequest(source, uri)
                    : sendHttpClientRequest(source, request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return failure(source, "HTTP_ERROR", response.statusCode(), "HTTP " + response.statusCode(), startedAt);
            }
            if (response.body().length > MAX_RESPONSE_BYTES) {
                return failure(source, "RESPONSE_TOO_LARGE", response.statusCode(), "응답 크기 제한 초과", startedAt);
            }
            JsonQaProfile jsonProfile = JSON_PROFILES.get(source.publicCode());
            if (jsonProfile != null) {
                ProfileResult result = inspectJsonProfile(source, response.body(), jsonProfile);
                SemanticQaResult semanticResult = selectDeclaredSemanticQa(source);
                return new QaResult(
                        source.publicCode(), source.sidoName(), source.sigunguName(), source.institutionName(),
                        source.noticeUrl(), selectStatus(result), result.profileCode(), result.discoveredCount(),
                        result.validCount(), result.invalidCount(), response.statusCode(), null, null,
                        String.join(" | ", result.samples()), elapsedMillis(startedAt),
                        semanticResult.sourceBoardTypeCode(), semanticResult.collectionPolicyCode(),
                        semanticResult.semanticStatusCode(), semanticResult.semanticReasonCode()
                );
            }
            Document document = Jsoup.parse(
                    new ByteArrayInputStream(response.body()),
                    null,
                    response.uri().toString()
            );
            writeHtmlSnapshot(source, response.uri(), document);
            List<ProfileResult> profileResults = PROFILES.stream()
                    .map(profile -> inspectProfile(source, document, profile))
                    .toList();
            ProfileResult best = selectBestProfile(profileResults);
            String status = selectStatus(best);
            SemanticQaResult semanticResult = inspectSourceSemantics(source, document);
            return new QaResult(
                    source.publicCode(), source.sidoName(), source.sigunguName(), source.institutionName(),
                    source.noticeUrl(), status, best.profileCode(), best.discoveredCount(), best.validCount(),
                    best.invalidCount(), response.statusCode(), null, null, String.join(" | ", best.samples()),
                    elapsedMillis(startedAt), semanticResult.sourceBoardTypeCode(),
                    semanticResult.collectionPolicyCode(), semanticResult.semanticStatusCode(),
                    semanticResult.semanticReasonCode()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(source, "INTERRUPTED", null, "검사가 중단되었습니다.", startedAt);
        } catch (IOException exception) {
            return failure(source, "NETWORK_ERROR", null, exception.getMessage(), startedAt);
        } catch (RuntimeException exception) {
            return failure(source, "PARSER_ERROR", null, exception.getMessage(), startedAt);
        } finally {
            if (domainSemaphore.availablePermits() == 0) {
                domainSemaphore.release();
            }
        }
    }

    /**
     * 파서 재현을 위해 최종 응답 URI와 디코딩된 HTML을 관리코드별로 저장합니다.
     *
     * @param source 지자체 URL seed
     * @param responseUri 리다이렉트 적용 후 최종 응답 URI
     * @param document 디코딩된 HTML 문서
     * @throws IOException 진단 HTML 저장 실패
     */
    private void writeHtmlSnapshot(SourceSeed source, URI responseUri, Document document) throws IOException {
        Path htmlDirectory = qaOutputDirectory.resolve("html");
        Files.createDirectories(htmlDirectory);
        String snapshot = "<!-- requested: " + source.requestUrl() + System.lineSeparator()
                + "final: " + responseUri + " -->" + System.lineSeparator()
                + document.outerHtml();
        Files.writeString(
                htmlDirectory.resolve(source.publicCode() + ".html"),
                snapshot,
                StandardCharsets.UTF_8
        );
    }

    /**
     * 한 HTML 문서에 단일 정적 파서 프로필을 적용합니다.
     *
     * @param source 지자체 URL seed
     * @param document HTML 문서
     * @param profile 파서 프로필
     * @return 발견·유효·무효 건수와 표본
     */
    private ProfileResult inspectProfile(SourceSeed source, Document document, ParserProfile profile) {
        if ("HEURISTIC_NOTICE".equals(profile.profileCode())) {
            return inspectHeuristicProfile(document, profile);
        }
        if ("DAEJEON_EMINWON_AGGREGATOR".equals(profile.profileCode())) {
            return inspectDaejeonEminwonProfile(document, profile);
        }
        Elements rows = document.select(profile.listItemSelector());
        int validCount = 0;
        int invalidCount = 0;
        List<String> samples = new ArrayList<>();
        for (Element row : rows) {
            Element titleElement = row.selectFirst(profile.titleSelector());
            Element dateElement = row.selectFirst(profile.dateSelector());
            Element linkElement = row.selectFirst(profile.linkSelector());
            String title = titleElement == null ? null : titleElement.text().trim();
            LocalGovernmentNoticeCollector.ResolvedLink resolvedLink = linkElement == null
                    ? null : parserSupport.selectResolvedLink(linkElement, profile.toRow(), source.noticeUrl());
            String rawLink = resolvedLink == null ? null : resolvedLink.rawLink();
            String link = resolvedLink == null ? null : resolvedLink.absoluteLink();
            LocalDate date = dateElement == null
                    ? null : parsePostedDate(dateElement.text(), profile.datePattern());
            if ((title == null || title.isBlank()) && linkElement == null
                    && (dateElement == null || dateElement.text().isBlank())) {
                continue;
            }
            if (date != null && !isRecentPostedDate(date)) {
                continue;
            }
            if (title == null || title.isBlank() || title.length() > 500
                    || rawLink == null || rawLink.isBlank() || "#".equals(rawLink)
                    || rawLink.toLowerCase(Locale.ROOT).startsWith("javascript:")
                    || link == null || !(link.startsWith("http://") || link.startsWith("https://"))
                    || !isDistinctDetailLink(source.noticeUrl(), rawLink, link)
                    || date == null) {
                invalidCount++;
                continue;
            }
            validCount++;
            if (samples.size() < 3) {
                samples.add(title.replaceAll("\\s+", " ").trim() + " / " + date + " / " + link);
            }
        }
        return new ProfileResult(
                profile.profileCode(), validCount + invalidCount, validCount, invalidCount, List.copyOf(samples)
        );
    }

    /**
     * 대전시 통합 목록의 구청별 전자민원 링크를 운영 수집기와 같은 허용 목록으로 검사합니다.
     *
     * @param document 대전시 통합 목록 문서
     * @param profile 대전시 전자민원 프로필
     * @return 발견·유효·무효 건수와 표본
     */
    private ProfileResult inspectDaejeonEminwonProfile(Document document, ParserProfile profile) {
        Elements rows = document.select(profile.listItemSelector());
        int validCount = 0;
        int invalidCount = 0;
        List<String> samples = new ArrayList<>();
        for (Element row : rows) {
            Element titleElement = row.selectFirst(profile.titleSelector());
            Element dateElement = row.selectFirst(profile.dateSelector());
            LocalGovernmentNoticeCollector.ResolvedLink link =
                    parserSupport.selectDaejeonEminwonLink(titleElement);
            String title = titleElement == null ? null : titleElement.text().trim();
            LocalDate date = dateElement == null
                    ? null : parsePostedDate(dateElement.text(), profile.datePattern());
            if ((title == null || title.isBlank()) && titleElement == null && dateElement == null) {
                continue;
            }
            if (date != null && !isRecentPostedDate(date)) {
                continue;
            }
            if (title == null || title.isBlank() || date == null || link == null) {
                invalidCount++;
                continue;
            }
            validCount++;
            if (samples.size() < 3) {
                samples.add(title + " / " + date + " / " + link.absoluteLink());
            }
        }
        return new ProfileResult(
                profile.profileCode(), validCount + invalidCount, validCount, invalidCount, List.copyOf(samples)
        );
    }

    /**
     * 목록 내부 fragment를 상세 URL로 오인하지 않고 동일 기관의 별도 상세 URL만 허용합니다.
     *
     * @param noticeUrl 목록 URL
     * @param rawLink 목록에서 읽은 원본 링크
     * @param absoluteLink 절대 URL로 변환한 링크
     * @return 별도 상세 URL이면 true
     */
    private boolean isDistinctDetailLink(String noticeUrl, String rawLink, String absoluteLink) {
        if (rawLink.startsWith("#")) {
            return false;
        }
        try {
            URI sourceUri = URI.create(noticeUrl);
            URI linkUri = URI.create(absoluteLink);
            if (!normalizeHost(sourceUri.getHost()).equals(normalizeHost(linkUri.getHost()))) {
                return false;
            }
            return !Objects.equals(sourceUri.getPath(), linkUri.getPath())
                    || !Objects.equals(sourceUri.getQuery(), linkUri.getQuery());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 상세 링크 주변에서 등록일을 찾는 제한형 휴리스틱 파서를 검사합니다.
     *
     * @param document HTML 문서
     * @param profile 휴리스틱 프로필
     * @return 유효 공고 링크와 표본
     */
    private ProfileResult inspectHeuristicProfile(Document document, ParserProfile profile) {
        int validCount = 0;
        Set<String> seenLinks = new HashSet<>();
        List<String> samples = new ArrayList<>();
        String sourceHost = normalizeHost(URI.create(document.location()).getHost());
        for (Element anchor : document.select(
                "a[href], a[onclick], a[data-url], a[data-href], a[data-action], a[data-link], a[data-view-url]"
        )) {
            String title = selectLinkTitle(anchor);
            ResolvedLink resolvedLink = selectResolvedLink(anchor);
            String rawLink = resolvedLink == null ? "" : resolvedLink.rawLink();
            String link = resolvedLink == null ? "" : resolvedLink.absoluteLink();
            if (!isNoticeLinkCandidate(title, rawLink, link, sourceHost)
                    || !isDistinctDetailLink(document.location(), rawLink, link)
                    || !seenLinks.add(link)) {
                continue;
            }
            LocalDate date = selectNearbyDate(anchor);
            if (date == null || !isRecentPostedDate(date)) {
                continue;
            }
            validCount++;
            if (samples.size() < 3) {
                samples.add(title + " / " + date + " / " + link);
            }
        }
        return new ProfileResult(profile.profileCode(), validCount, validCount, 0, List.copyOf(samples));
    }

    /**
     * DB에 등록할 JSON 필드 매핑과 동일한 규칙으로 endpoint 응답을 검사합니다.
     *
     * @param source 지자체 URL seed
     * @param body JSON 응답
     * @param profile JSON QA 프로필
     * @return 발견·유효·무효 건수와 표본
     */
    private ProfileResult inspectJsonProfile(SourceSeed source, byte[] body, JsonQaProfile profile) {
        try {
            JsonNode items = selectJsonPath(objectMapper.readTree(body), profile.itemsPath());
            if (items == null || !items.isArray()) {
                return new ProfileResult(profile.profileCode(), 0, 0, 0, List.of());
            }
            int validCount = 0;
            int invalidCount = 0;
            List<String> samples = new ArrayList<>();
            for (JsonNode item : items) {
                String title = item.path(profile.titleField()).asText("").trim();
                LocalDate date = parseDate(item.path(profile.dateField()).asText(null), "yyyy-MM-dd");
                String linkValue = item.path(profile.linkField()).asText("").trim();
                String link = resolveJsonLink(source.noticeUrl(), profile.linkTemplate(), linkValue);
                if (title.isBlank() && date == null && linkValue.isBlank()) {
                    continue;
                }
                if (date != null && !isRecentPostedDate(date)) {
                    continue;
                }
                if (title.isBlank() || date == null || link == null) {
                    invalidCount++;
                    continue;
                }
                validCount++;
                if (samples.size() < 3) {
                    samples.add(title + " / " + date + " / " + link);
                }
            }
            return new ProfileResult(
                    profile.profileCode(), validCount + invalidCount, validCount, invalidCount, List.copyOf(samples)
            );
        } catch (IOException exception) {
            return new ProfileResult(profile.profileCode(), 0, 0, 1, List.of());
        }
    }

    /**
     * 점으로 구분된 JSON 객체 경로를 순서대로 조회합니다.
     *
     * @param root JSON root
     * @param path 점 구분 경로
     * @return 경로의 JSON node 또는 null
     */
    private JsonNode selectJsonPath(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode current = root;
        for (String name : path.split("\\.")) {
            current = current.path(name);
            if (current.isMissingNode() || current.isNull()) {
                return null;
            }
        }
        return current;
    }

    /**
     * JSON 식별자를 검증된 동일 기관 링크 template에 적용합니다.
     *
     * @param noticeUrl 사용자용 목록 URL
     * @param template 링크 template
     * @param value JSON 식별자
     * @return 상세 URL 또는 null
     */
    private String resolveJsonLink(String noticeUrl, String template, String value) {
        if (value == null || value.isBlank() || template == null || !template.contains("{value}")) {
            return null;
        }
        try {
            URI baseUri = URI.create(noticeUrl);
            URI resolvedUri = baseUri.resolve(template.replace("{value}", value));
            return normalizeHost(baseUri.getHost()).equals(normalizeHost(resolvedUri.getHost()))
                    ? resolvedUri.toString() : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 휴리스틱 파서가 사용할 수 있는 동일 기관 도메인의 상세 링크인지 확인합니다.
     *
     * @param title 링크 제목
     * @param rawLink 원본 href
     * @param absoluteLink 절대 URL
     * @param sourceHost 수집원 host
     * @return 공고 링크 후보이면 true
     */
    private boolean isNoticeLinkCandidate(String title, String rawLink, String absoluteLink, String sourceHost) {
        if (title.length() < 5 || title.length() > 200 || NON_NOTICE_TITLE_PATTERN.matcher(title).matches()
                || FILE_LINK_PATTERN.matcher(title).matches() || title.startsWith("RSS ")) {
            return false;
        }
        String lowerRawLink = rawLink.toLowerCase(Locale.ROOT);
        if (rawLink.isBlank() || FILE_LINK_PATTERN.matcher(lowerRawLink).matches()
                || NON_NOTICE_LINK_PATTERN.matcher(lowerRawLink).matches()
                || !DETAIL_LINK_PATTERN.matcher(lowerRawLink).matches()) {
            return false;
        }
        try {
            URI linkUri = URI.create(absoluteLink);
            String scheme = linkUri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && sourceHost.equals(normalizeHost(linkUri.getHost()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 링크 요소에서 공고 제목을 선택합니다.
     *
     * @param element 링크 요소
     * @return 정규화된 제목
     */
    private String selectLinkTitle(Element element) {
        Element conciseTitle = element.selectFirst(".title, .tit, .subject, .t1, strong");
        String title = (conciseTitle == null ? element.text() : conciseTitle.text())
                .replaceAll("\\s+", " ").trim();
        if (title.isBlank()) {
            title = element.attr("title").replaceAll("\\s+", " ").trim();
        }
        return title;
    }

    /**
     * href와 안전한 data 속성에서 상세 URL 후보를 해석합니다.
     *
     * @param element 링크 요소
     * @return 해석된 링크 또는 null
     */
    private ResolvedLink selectResolvedLink(Element element) {
        for (String attributeName : LINK_ATTRIBUTE_NAMES) {
            ResolvedLink resolvedLink = resolveLinkValue(element, element.attr(attributeName).trim());
            if (resolvedLink != null) {
                return resolvedLink;
            }
        }
        return resolveLinkValue(element, element.attr("onclick").trim());
    }

    /**
     * 직접 URL 또는 스크립트 문자열 내부의 명시적 URL을 절대 URL로 변환합니다.
     *
     * @param element 기준 링크 요소
     * @param rawValue 원본 속성값
     * @return 해석된 링크 또는 null
     */
    private ResolvedLink resolveLinkValue(Element element, String rawValue) {
        if (rawValue == null || rawValue.isBlank() || "#".equals(rawValue)) {
            return null;
        }
        String candidate = rawValue;
        if (candidate.toLowerCase(Locale.ROOT).startsWith("javascript:") || candidate.contains("(")) {
            Matcher matcher = SCRIPT_PATH_PATTERN.matcher(candidate);
            if (!matcher.find()) {
                return null;
            }
            candidate = matcher.group(1);
        }
        candidate = candidate.replace(" ", "%20");
        try {
            URI resolvedUri = URI.create(element.baseUri()).resolve(candidate);
            String scheme = resolvedUri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }
            return new ResolvedLink(candidate, resolvedUri.toString());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 링크의 상위 HTML 영역을 최대 여섯 단계까지 확인해 등록일을 찾습니다.
     *
     * @param anchor 공고 상세 링크
     * @return 등록일 또는 null
     */
    private LocalDate selectNearbyDate(Element anchor) {
        Element current = anchor;
        for (int depth = 0; depth < 6 && current != null; depth++) {
            if (!isStructuredContainer(current)) {
                current = current.parent();
                continue;
            }
            String text = current.text();
            if (text.length() > 2000) {
                return null;
            }
            for (Element dateElement : current.select(
                    "time, .date, .regdate, .wdate, [class*=date], [class*=reg-date], [class*=write-date]"
            )) {
                LocalDate date = parseDate(dateElement.text(), null);
                if (date != null) {
                    return date;
                }
            }
            List<LocalDate> dates = selectDateList(text);
            if (text.length() <= 500 && dates.size() == 1) {
                return dates.getFirst();
            }
            current = current.parent();
        }
        return null;
    }

    /**
     * 문자열에 포함된 모든 4자리 연도 날짜를 추출합니다.
     *
     * @param text 목록 행 텍스트
     * @return 날짜 목록
     */
    private List<LocalDate> selectDateList(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        List<LocalDate> dates = new ArrayList<>();
        while (matcher.find()) {
            try {
                dates.add(LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                ));
            } catch (RuntimeException ignored) {
                // 잘못된 날짜 한 건은 무시하고 같은 행의 다른 날짜를 확인합니다.
            }
        }
        Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(text);
        while (shortMatcher.find()) {
            try {
                dates.add(LocalDate.of(
                        2000 + Integer.parseInt(shortMatcher.group(1)),
                        Integer.parseInt(shortMatcher.group(2)),
                        Integer.parseInt(shortMatcher.group(3))
                ));
            } catch (RuntimeException ignored) {
                // 잘못된 날짜 한 건은 무시하고 같은 행의 다른 날짜를 확인합니다.
            }
        }
        return dates;
    }

    /**
     * 날짜를 함께 읽을 수 있는 반복 행·목록·카드 컨테이너인지 확인합니다.
     *
     * @param element 링크 상위 요소
     * @return 구조화 컨테이너이면 true
     */
    private boolean isStructuredContainer(Element element) {
        String tagName = element.tagName();
        return "tr".equals(tagName) || "ul".equals(tagName) || "li".equals(tagName)
                || "article".equals(tagName) || "dl".equals(tagName)
                || STRUCTURED_CONTAINER_PATTERN.matcher(element.className()).matches();
    }

    /**
     * URL 비교를 위해 www 접두사를 제거한 host를 반환합니다.
     *
     * @param host URL host
     * @return 정규화 host
     */
    private String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    }

    /**
     * 현재 운영 대상에서 사용할 수 있는 최근 등록일인지 확인합니다.
     *
     * @param postedDate 공고 등록일
     * @return 오늘 이전 1년 범위이면 true
     */
    private boolean isRecentPostedDate(LocalDate postedDate) {
        LocalDate today = LocalDate.now();
        return !postedDate.isBefore(today.minusYears(1)) && !postedDate.isAfter(today);
    }

    /**
     * 날짜 또는 당일 신규 공고의 시각 전용 표기를 등록일로 변환합니다.
     *
     * @param text 날짜 또는 시각 문자열
     * @param configuredPattern 파서 날짜 패턴
     * @return 변환된 등록일 또는 null
     */
    private LocalDate parsePostedDate(String text, String configuredPattern) {
        LocalDate parsedDate = parseDate(text, configuredPattern);
        if (parsedDate != null) {
            return parsedDate;
        }
        return text != null && TIME_ONLY_PATTERN.matcher(text).matches()
                ? LocalDate.now()
                : null;
    }

    /**
     * 운영 수집기와 동일한 규칙으로 날짜를 변환합니다.
     *
     * @param text 날짜 원문
     * @param configuredPattern 파서 날짜 패턴
     * @return 날짜 또는 null
     */
    private LocalDate parseDate(String text, String configuredPattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (configuredPattern != null) {
            try {
                return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern(configuredPattern));
            } catch (DateTimeParseException ignored) {
                // 운영 수집기와 동일하게 숫자 추출 규칙을 한 번 더 적용합니다.
            }
        }
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                );
            } catch (RuntimeException exception) {
                return null;
            }
        }
        Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(text);
        if (!shortMatcher.find()) {
            return null;
        }
        try {
            return LocalDate.of(
                    2000 + Integer.parseInt(shortMatcher.group(1)),
                    Integer.parseInt(shortMatcher.group(2)),
                    Integer.parseInt(shortMatcher.group(3))
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 최적 파서의 유효 추출률로 QA 상태를 결정합니다.
     *
     * @param best 최적 파서 결과
     * @return PASS, PARTIAL 또는 PARSER_UNSUPPORTED
     */
    private String selectStatus(ProfileResult best) {
        if (best.validCount() == 0) {
            return "PARSER_UNSUPPORTED";
        }
        return best.invalidCount() == 0 ? "PASS" : "PARTIAL";
    }

    /**
     * 정적 파서 통과 결과를 우선하고, 정적 파서가 미통과할 때만 휴리스틱 결과를 사용합니다.
     *
     * @param results 파서별 결과
     * @return 최종 추천 파서 결과
     */
    private ProfileResult selectBestProfile(List<ProfileResult> results) {
        Comparator<ProfileResult> comparator = Comparator.comparingInt(ProfileResult::validCount)
                .thenComparing(Comparator.comparingInt(ProfileResult::invalidCount).reversed());
        return results.stream()
                .filter(result -> !"HEURISTIC_NOTICE".equals(result.profileCode()))
                .filter(result -> "PASS".equals(selectStatus(result)))
                .max(comparator)
                .orElseGet(() -> results.stream().max(comparator).orElseThrow());
    }

    /**
     * HTML의 페이지 제목·경로 표식과 정적 출처 분류를 함께 사용해 게시판 의미를 확인합니다.
     *
     * @param source 지자체 URL seed
     * @param document 수집한 HTML 문서
     * @return 파싱 결과와 분리된 게시판 의미 검증 결과
     */
    private SemanticQaResult inspectSourceSemantics(SourceSeed source, Document document) {
        SemanticQaResult declared = selectDeclaredSemanticQa(source);
        String evidence = String.join(" ",
                document.title(),
                document.select("meta[property=og:title]").attr("content")
        ).replaceAll("\\s+", " ").trim();

        if ("PRESS_RELEASE".equals(declared.sourceBoardTypeCode())) {
            return new SemanticQaResult(
                    declared.sourceBoardTypeCode(), declared.collectionPolicyCode(),
                    "EXCLUDED", "PRESS_RELEASE_SOURCE"
            );
        }
        if (PRESS_BOARD_PATTERN.matcher(evidence).matches()
                && !LEGAL_BOARD_PATTERN.matcher(evidence).matches()
                && !SUPPORT_BOARD_PATTERN.matcher(evidence).matches()) {
            return new SemanticQaResult(
                    declared.sourceBoardTypeCode(), declared.collectionPolicyCode(),
                    "SEMANTIC_MISMATCH", "PRESS_RELEASE_PAGE_EVIDENCE"
            );
        }
        if ("GENERAL_NOTICE".equals(declared.sourceBoardTypeCode())) {
            return new SemanticQaResult(
                    declared.sourceBoardTypeCode(), declared.collectionPolicyCode(),
                    "KEYWORD_FILTER_REQUIRED", "GENERAL_NOTICE_STATIC_KEYWORD_POLICY"
            );
        }
        if ("LEGAL_NOTICE".equals(declared.sourceBoardTypeCode())
                && (LEGAL_BOARD_PATTERN.matcher(evidence).matches()
                || "public_notice_board".equals(source.pageTypeCode())
                || LEGAL_SOURCE_URL_PATTERN.matcher(source.noticeUrl()).matches())) {
            return new SemanticQaResult(
                    declared.sourceBoardTypeCode(), declared.collectionPolicyCode(),
                    "MATCHED", "LEGAL_NOTICE_PAGE_EVIDENCE"
            );
        }
        if ("SUPPORT_RECRUITMENT".equals(declared.sourceBoardTypeCode())
                && (SUPPORT_BOARD_PATTERN.matcher(evidence).matches()
                || Set.of("small_business_support_page", "dedicated_small_business_board")
                        .contains(source.pageTypeCode()))) {
            return new SemanticQaResult(
                    declared.sourceBoardTypeCode(), declared.collectionPolicyCode(),
                    "MATCHED", "SUPPORT_RECRUITMENT_PAGE_EVIDENCE"
            );
        }
        return new SemanticQaResult(
                declared.sourceBoardTypeCode(), declared.collectionPolicyCode(),
                "REVIEW_REQUIRED", "BOARD_EVIDENCE_INSUFFICIENT"
        );
    }

    /**
     * V56 정적 분류와 같은 규칙으로 출처의 게시판 유형과 수집 정책을 산출합니다.
     *
     * @param source 지자체 URL seed
     * @return 정적 출처 분류 결과
     */
    private SemanticQaResult selectDeclaredSemanticQa(SourceSeed source) {
        String boardTypeCode = selectSourceBoardTypeCode(source);
        String policyCode = selectCollectionPolicyCode(source);
        String semanticStatusCode = switch (boardTypeCode) {
            case "PRESS_RELEASE" -> "EXCLUDED";
            case "GENERAL_NOTICE" -> "KEYWORD_FILTER_REQUIRED";
            case "LEGAL_NOTICE", "SUPPORT_RECRUITMENT" -> "MATCHED";
            default -> "REVIEW_REQUIRED";
        };
        return new SemanticQaResult(
                boardTypeCode,
                policyCode,
                semanticStatusCode,
                "STATIC_SOURCE_CLASSIFICATION"
        );
    }

    /**
     * 정적 seed와 공식 URL 패턴으로 게시판 유형을 선택합니다.
     *
     * @param source 지자체 URL seed
     * @return 게시판 유형 코드
     */
    private String selectSourceBoardTypeCode(SourceSeed source) {
        if ("LGS-000084".equals(source.publicCode())) {
            return "PRESS_RELEASE";
        }
        if (Set.of("small_business_support_page", "dedicated_small_business_board")
                .contains(source.pageTypeCode())) {
            return "SUPPORT_RECRUITMENT";
        }
        if ("public_notice_board".equals(source.pageTypeCode())
                || LEGAL_SOURCE_URL_PATTERN.matcher(source.noticeUrl()).matches()) {
            return "LEGAL_NOTICE";
        }
        return "GENERAL_NOTICE";
    }

    /**
     * 게시판 유형에 맞는 수집 정책을 선택합니다.
     *
     * @param source 지자체 URL seed
     * @return 수집 정책 코드
     */
    private String selectCollectionPolicyCode(SourceSeed source) {
        return switch (selectSourceBoardTypeCode(source)) {
            case "LEGAL_NOTICE", "SUPPORT_RECRUITMENT" -> "COLLECT_ALL";
            case "GENERAL_NOTICE" -> "KEYWORD_FILTERED";
            default -> "EXCLUDED";
        };
    }

    /**
     * V29 migration에서 지자체 URL seed를 읽습니다.
     *
     * @return 지자체 URL seed 목록
     * @throws IOException migration 읽기 실패
     */
    private List<SourceSeed> selectSourceSeedList() throws IOException {
        String migration = Files.readString(
                Path.of("src/main/resources/db/migration/V29__seed_local_government_notice_sources.sql"),
                StandardCharsets.UTF_8
        );
        String hardeningMigration = selectHardeningMigrationText();
        Map<String, String> reviewedUrls = selectReviewedUrlMap(hardeningMigration);
        Map<String, String> collectionEndpoints = selectCollectionEndpointMap(hardeningMigration);
        Set<String> browserHttp1Sources = selectBrowserHttp1SourceCodes(hardeningMigration);
        Set<String> legacyBrowserSources = selectLegacyBrowserSourceCodes(hardeningMigration);
        Set<String> tls12BrowserSources = selectTls12BrowserSourceCodes(hardeningMigration);
        Set<String> sessionBrowserSources = selectSessionBrowserSourceCodes(hardeningMigration);
        Matcher blockMatcher = SOURCE_INSERT_PATTERN.matcher(migration);
        List<SourceSeed> sources = new ArrayList<>();
        int sequence = 1;
        while (blockMatcher.find()) {
            List<String> values = selectSqlStrings(blockMatcher.group(1));
            if (values.size() < 14) {
                throw new IllegalStateException("지자체 URL seed 형식을 해석할 수 없습니다: " + sequence);
            }
            String publicCode = "LGS-" + String.format(Locale.ROOT, "%06d", sequence++);
            sources.add(new SourceSeed(
                    publicCode, values.get(2), values.get(4), values.get(6),
                     reviewedUrls.getOrDefault(publicCode, values.get(8)),
                     collectionEndpoints.get(publicCode),
                     sessionBrowserSources.contains(publicCode)
                             ? "SESSION_BROWSER"
                             : tls12BrowserSources.contains(publicCode)
                             ? "TLS12_BROWSER"
                             : legacyBrowserSources.contains(publicCode)
                             ? "LEGACY_BROWSER"
                             : browserHttp1Sources.contains(publicCode) || collectionEndpoints.containsKey(publicCode)
                                    ? "BROWSER_HTTP1" : "DEFAULT",
                    values.get(9)
            ));
        }
        return List.copyOf(sources);
    }

    /**
     * V32 이후 지자체 URL 및 요청 방식 보정 migration을 적용 순서대로 합칩니다.
     *
     * @return 지자체 수집 보정 migration 원문
     * @throws IOException migration 목록 또는 파일 읽기 실패
     */
    private String selectHardeningMigrationText() throws IOException {
        Path migrationDirectory = Path.of("src/main/resources/db/migration");
        try (var paths = Files.list(migrationDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> selectMigrationVersion(path) >= 32)
                    .sorted(Comparator.comparingInt(this::selectMigrationVersion))
                    .map(path -> {
                        try {
                            return Files.readString(path, StandardCharsets.UTF_8);
                        } catch (IOException exception) {
                            throw new MigrationReadException(path, exception);
                        }
                    })
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (MigrationReadException exception) {
            throw exception.selectCause();
        }
    }

    /**
     * Flyway 파일명에서 숫자 버전을 추출합니다.
     *
     * @param path migration 파일 경로
     * @return 숫자 버전, 버전 형식이 아니면 -1
     */
    private int selectMigrationVersion(Path path) {
        Matcher matcher = Pattern.compile("^V(\\d+)__.*\\.sql$").matcher(path.getFileName().toString());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /**
     * V32 이후 migration에서 검증한 URL 보정값을 읽습니다.
     *
     * @param migration V32 migration 원문
     * @return 관리코드별 보정 URL
     */
    private Map<String, String> selectReviewedUrlMap(String migration) {
        Matcher matcher = REVIEWED_URL_PATTERN.matcher(migration);
        Map<String, String> reviewedUrls = new LinkedHashMap<>();
        while (matcher.find()) {
            if (matcher.group(2).startsWith("http://") || matcher.group(2).startsWith("https://")) {
                reviewedUrls.put(matcher.group(1), matcher.group(2));
            }
        }
        return Map.copyOf(reviewedUrls);
    }

    /**
     * V32 이후 migration에서 브라우저 호환 HTTP/1.1 적용 대상을 읽습니다.
     *
     * @param migration V32 migration 원문
     * @return 관리코드 집합
     */
    private Set<String> selectBrowserHttp1SourceCodes(String migration) {
        Matcher blockMatcher = BROWSER_HTTP1_BLOCK_PATTERN.matcher(migration);
        Set<String> sourceCodes = new HashSet<>();
        while (blockMatcher.find()) {
            sourceCodes.addAll(selectSqlStrings(blockMatcher.group(1)));
        }
        return Set.copyOf(sourceCodes);
    }

    /**
     * V32 이후 migration에서 구형 공공사이트 호환 요청 적용 대상을 읽습니다.
     *
     * @param migration V32 이후 migration 원문
     * @return 관리코드 집합
     */
    private Set<String> selectLegacyBrowserSourceCodes(String migration) {
        Matcher blockMatcher = LEGACY_BROWSER_BLOCK_PATTERN.matcher(migration);
        Set<String> sourceCodes = new HashSet<>();
        while (blockMatcher.find()) {
            sourceCodes.addAll(selectSqlStrings(blockMatcher.group(1)));
        }
        return Set.copyOf(sourceCodes);
    }

    /**
     * V32 이후 migration에서 TLS 1.2 고정 요청 적용 대상을 읽습니다.
     *
     * @param migration V32 이후 migration 원문
     * @return 관리코드 집합
     */
    private Set<String> selectTls12BrowserSourceCodes(String migration) {
        Matcher blockMatcher = TLS12_BROWSER_BLOCK_PATTERN.matcher(migration);
        Set<String> sourceCodes = new HashSet<>();
        while (blockMatcher.find()) {
            sourceCodes.addAll(selectSqlStrings(blockMatcher.group(1)));
        }
        return Set.copyOf(sourceCodes);
    }

    /**
     * V32 이후 migration에서 세션 고정 브라우저 요청 적용 대상을 읽습니다.
     *
     * @param migration V32 이후 migration 원문
     * @return 관리코드 집합
     */
    private Set<String> selectSessionBrowserSourceCodes(String migration) {
        Matcher blockMatcher = SESSION_BROWSER_BLOCK_PATTERN.matcher(migration);
        Set<String> sourceCodes = new HashSet<>();
        while (blockMatcher.find()) {
            sourceCodes.addAll(selectSqlStrings(blockMatcher.group(1)));
        }
        return Set.copyOf(sourceCodes);
    }

    /**
     * V32 이후 migration에서 별도 수집 endpoint를 읽습니다.
     *
     * @param migration V32 migration 원문
     * @return 관리코드별 수집 endpoint
     */
    private Map<String, String> selectCollectionEndpointMap(String migration) {
        Matcher matcher = COLLECTION_ENDPOINT_PATTERN.matcher(migration);
        Map<String, String> endpoints = new LinkedHashMap<>();
        while (matcher.find()) {
            endpoints.put(matcher.group(2), matcher.group(1));
        }
        return Map.copyOf(endpoints);
    }

    /**
     * SQL 블록의 작은따옴표 문자열을 순서대로 추출합니다.
     *
     * @param valuesBlock VALUES 내부 문자열
     * @return SQL 문자열 값 목록
     */
    private List<String> selectSqlStrings(String valuesBlock) {
        Matcher matcher = SQL_STRING_PATTERN.matcher(valuesBlock);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(matcher.group(1).replace("''", "'"));
        }
        return values;
    }

    /**
     * Stream 내부에서 발생한 migration 읽기 오류를 원래 IOException으로 전달합니다.
     */
    private static final class MigrationReadException extends RuntimeException {

        private final IOException cause;

        /**
         * migration 읽기 오류를 생성합니다.
         *
         * @param path 읽기에 실패한 migration 경로
         * @param cause 원래 읽기 오류
         */
        private MigrationReadException(Path path, IOException cause) {
            super("migration 읽기 실패: " + path, cause);
            this.cause = cause;
        }

        /**
         * 원래 IOException을 반환합니다.
         *
         * @return migration 읽기 오류
         */
        private IOException selectCause() {
            return cause;
        }
    }

    /**
     * 전체 QA 결과를 UTF-8 CSV로 저장합니다.
     *
     * @param path 출력 경로
     * @param results QA 결과
     * @throws IOException 저장 실패
     */
    private void writeCsv(Path path, List<QaResult> results) throws IOException {
        StringBuilder csv = new StringBuilder(
                "\uFEFF관리코드,시도,시군구,기관명,URL,QA상태,추천파서,발견,유효,무효,HTTP,"
                        + "오류코드,오류메시지,표본,응답시간ms,게시판유형,수집정책,의미검증상태,의미검증사유\n"
        );
        for (QaResult result : results) {
            csv.append(Arrays.asList(
                            result.publicCode(), result.sidoName(), result.sigunguName(), result.institutionName(),
                             result.noticeUrl(), result.statusCode(), result.profileCode(), result.discoveredCount(),
                             result.validCount(), result.invalidCount(), result.httpStatus(), result.errorCode(),
                             result.errorMessage(), result.samples(), result.elapsedMillis(),
                             result.sourceBoardTypeCode(), result.collectionPolicyCode(),
                             result.semanticStatusCode(), result.semanticReasonCode()
                    ).stream().map(this::csvValue).collect(Collectors.joining(",")))
                    .append('\n');
        }
        Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
    }

    /**
     * QA 상태와 추천 파서 집계를 Markdown으로 저장합니다.
     *
     * @param path 출력 경로
     * @param results QA 결과
     * @throws IOException 저장 실패
     */
    private void writeSummary(Path path, List<QaResult> results) throws IOException {
        Map<String, Long> statusCounts = results.stream().collect(Collectors.groupingBy(
                QaResult::statusCode,
                LinkedHashMap::new,
                Collectors.counting()
        ));
        Map<String, Long> profileCounts = results.stream()
                .filter(result -> result.profileCode() != null && !result.profileCode().isBlank())
                .collect(Collectors.groupingBy(QaResult::profileCode, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> semanticStatusCounts = results.stream().collect(Collectors.groupingBy(
                QaResult::semanticStatusCode,
                LinkedHashMap::new,
                Collectors.counting()
        ));
        Map<String, Long> boardTypeCounts = results.stream().collect(Collectors.groupingBy(
                QaResult::sourceBoardTypeCode,
                LinkedHashMap::new,
                Collectors.counting()
        ));
        StringBuilder markdown = new StringBuilder();
        markdown.append("# 지자체 파서 전수 QA 요약\n\n")
                .append("- 검사 대상: ").append(results.size()).append("곳\n")
                .append("- 운영 DB 수집 여부: 없음\n")
                .append("- 필수 추출값: 제목, 등록일, 원문 URL\n\n")
                .append("## 상태 집계\n\n| 상태 | 건수 |\n|---|---:|\n");
        statusCounts.forEach((status, count) -> markdown.append('|').append(status).append('|').append(count).append("|\n"));
        markdown.append("\n## 게시판 유형 집계\n\n| 유형 | 건수 |\n|---|---:|\n");
        boardTypeCounts.forEach((status, count) -> markdown.append('|').append(status).append('|').append(count).append("|\n"));
        markdown.append("\n## 의미 검증 집계\n\n| 상태 | 건수 |\n|---|---:|\n");
        semanticStatusCounts.forEach((status, count) -> markdown.append('|').append(status).append('|').append(count).append("|\n"));
        markdown.append("\n## 추천 파서 집계\n\n| 파서 | 건수 |\n|---|---:|\n");
        profileCounts.forEach((profile, count) -> markdown.append('|').append(profile).append('|').append(count).append("|\n"));
        markdown.append("\n## 미통과 목록\n\n| 관리코드 | 기관 | 상태 | 오류 |\n|---|---|---|---|\n");
        results.stream().filter(result -> !"PASS".equals(result.statusCode())).forEach(result -> markdown
                .append('|').append(result.publicCode())
                .append('|').append(escapeMarkdown(result.institutionName()))
                .append('|').append(result.statusCode())
                .append('|').append(escapeMarkdown(result.errorCode() == null ? "추출률 미달" : result.errorCode()))
                .append("|\n"));
        markdown.append("\n## 게시판 의미 확인 필요 목록\n\n")
                .append("| 관리코드 | 기관 | 게시판 유형 | 의미 상태 | 사유 |\n")
                .append("|---|---|---|---|---|\n");
        results.stream()
                .filter(result -> Set.of("SEMANTIC_MISMATCH", "REVIEW_REQUIRED")
                        .contains(result.semanticStatusCode()))
                .forEach(result -> markdown
                        .append('|').append(result.publicCode())
                        .append('|').append(escapeMarkdown(result.institutionName()))
                        .append('|').append(result.sourceBoardTypeCode())
                        .append('|').append(result.semanticStatusCode())
                        .append('|').append(result.semanticReasonCode())
                        .append("|\n"));
        Files.writeString(path, markdown.toString(), StandardCharsets.UTF_8);
    }

    /**
     * CSV 셀 값을 안전하게 이스케이프합니다.
     *
     * @param value 셀 값
     * @return CSV 문자열
     */
    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return '"' + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + '"';
    }

    /**
     * Markdown 표 구분자를 이스케이프합니다.
     *
     * @param value 표 값
     * @return 이스케이프 문자열
     */
    private String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    /**
     * URL 요청 실패 결과를 생성합니다.
     *
     * @param source 지자체 URL seed
     * @param errorCode 오류 코드
     * @param httpStatus HTTP 상태
     * @param errorMessage 오류 메시지
     * @param startedAt 시작 시각
     * @return 실패 QA 결과
     */
    private QaResult failure(
            SourceSeed source,
            String errorCode,
            Integer httpStatus,
            String errorMessage,
            long startedAt
    ) {
        return new QaResult(
                source.publicCode(), source.sidoName(), source.sigunguName(), source.institutionName(),
                source.noticeUrl(), "FAILED", null, 0, 0, 0, httpStatus, errorCode,
                errorMessage == null ? "" : errorMessage, "", elapsedMillis(startedAt),
                selectSourceBoardTypeCode(source), selectCollectionPolicyCode(source),
                "NOT_EVALUATED", errorCode
        );
    }

    /**
     * 경과 시간을 밀리초로 반환합니다.
     *
     * @param startedAt 시작 nano time
     * @return 경과 밀리초
     */
    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    /**
     * 표준 Java HTTP 클라이언트로 QA 요청을 수행합니다.
     *
     * @param source 지자체 URL seed
     * @param request 전송할 요청
     * @return 전송 방식에 독립적인 QA 응답
     * @throws IOException 네트워크 또는 응답 읽기 오류
     * @throws InterruptedException 요청 중단
     */
    private QaHttpResponse sendHttpClientRequest(SourceSeed source, HttpRequest request)
            throws IOException, InterruptedException {
        HttpResponse<byte[]> response = selectHttpClient(source)
                .send(request, HttpResponse.BodyHandlers.ofByteArray());
        return new QaHttpResponse(response.statusCode(), response.body(), response.uri());
    }

    /**
     * 세션 고정형 사이트의 공식 홈페이지를 먼저 호출해 QA 요청 쿠키를 확보합니다.
     *
     * @param source 지자체 URL seed
     * @return 홈페이지 HTTP 상태
     * @throws IOException 네트워크 오류
     * @throws InterruptedException 요청 중단
     */
    private int prepareBrowserSession(SourceSeed source) throws IOException, InterruptedException {
        URI noticeUri = URI.create(source.noticeUrl());
        URI homepageUri = noticeUri.resolve("/");
        int lastStatus = 0;
        IOException lastException = null;
        for (int attempt = 1; attempt <= SESSION_BOOTSTRAP_ATTEMPTS; attempt++) {
            sessionCookieManager.getCookieStore().removeAll();
            HttpRequest request = HttpRequest.newBuilder(homepageUri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", BROWSER_COMPATIBLE_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .GET()
                    .build();
            try {
                lastStatus = sessionBrowserHttpClient.send(
                        request,
                        HttpResponse.BodyHandlers.discarding()
                ).statusCode();
                if (lastStatus >= 200 && lastStatus < 400) {
                    return lastStatus;
                }
            } catch (IOException exception) {
                lastException = exception;
            }
        }
        if (lastStatus == 0 && lastException != null) {
            throw lastException;
        }
        return lastStatus;
    }

    /**
     * 구형 공공 웹서버와 호환되는 URLConnection으로 QA 요청을 수행합니다.
     *
     * @param source 지자체 URL seed
     * @param uri 요청 URI
     * @return 전송 방식에 독립적인 QA 응답
     * @throws IOException 네트워크 또는 응답 읽기 오류
     */
    private QaHttpResponse sendLegacyRequest(SourceSeed source, URI uri) throws IOException {
        if (POST_FORM_BODIES.containsKey(source.publicCode())) {
            throw new IOException("구형 공공사이트 호환 요청은 GET 방식만 지원합니다.");
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        if (connection instanceof HttpsURLConnection httpsConnection && windowsSslContext != null) {
            httpsConnection.setSSLSocketFactory(windowsSslContext.getSocketFactory());
        }
        connection.setConnectTimeout(20_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", BROWSER_COMPATIBLE_USER_AGENT);
        connection.setRequestProperty("Accept", JSON_PROFILES.containsKey(source.publicCode())
                ? "application/json,text/plain;q=0.9,*/*;q=0.5"
                : "text/html,application/xhtml+xml");
        connection.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.5");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        if (source.collectionEndpointUrl() != null && !source.collectionEndpointUrl().isBlank()) {
            connection.setRequestProperty("Referer", source.noticeUrl());
        }
        try {
            int statusCode = connection.getResponseCode();
            byte[] responseBody = new byte[0];
            if (statusCode >= 200 && statusCode < 300) {
                try (InputStream inputStream = connection.getInputStream()) {
                    responseBody = inputStream.readNBytes(MAX_RESPONSE_BYTES + 1);
                }
            }
            return new QaHttpResponse(statusCode, responseBody, URI.create(connection.getURL().toString()));
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 브라우저 수준의 요청 헤더가 필요한 프로필인지 확인합니다.
     *
     * @param source 지자체 URL seed
     * @return 브라우저 호환 헤더 적용 대상이면 true
     */
    private boolean usesBrowserCompatibleRequest(SourceSeed source) {
        return "BROWSER_HTTP1".equals(source.requestProfileCode())
                || "LEGACY_BROWSER".equals(source.requestProfileCode())
                || "TLS12_BROWSER".equals(source.requestProfileCode())
                || usesSessionBrowser(source);
    }

    /**
     * 홈페이지 세션을 확보한 뒤 게시판을 요청하는 QA 대상인지 확인합니다.
     *
     * @param source 지자체 URL seed
     * @return 세션 브라우저 프로필이면 true
     */
    private boolean usesSessionBrowser(SourceSeed source) {
        return "SESSION_BROWSER".equals(source.requestProfileCode());
    }

    /**
     * Windows에서는 운영체제 인증서 저장소를 사용하고 그 외 환경에서는 JVM 기본 저장소를 사용합니다.
     *
     * @return QA HTTP 클라이언트
     */
    private HttpClient createHttpClient(HttpClient.Version version) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (version != null) {
            builder.version(version);
        }
        if (windowsSslContext != null) {
            builder.sslContext(windowsSslContext);
        }
        return builder.build();
    }

    /**
     * 출처 도메인별 쿠키를 유지하는 HTTP/1.1 QA 클라이언트를 생성합니다.
     *
     * @return 세션 브라우저 QA 클라이언트
     */
    private HttpClient createSessionHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(sessionCookieManager);
        if (windowsSslContext != null) {
            builder.sslContext(windowsSslContext);
        }
        return builder.build();
    }

    /**
     * TLS 1.2만 허용하는 구형 공공 HTTPS QA 클라이언트를 생성합니다.
     *
     * @return TLS 1.2 전용 QA 클라이언트
     */
    private HttpClient createTls12HttpClient() {
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setProtocols(new String[]{"TLSv1.2"});
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .sslParameters(sslParameters);
        if (windowsSslContext != null) {
            builder.sslContext(windowsSslContext);
        }
        return builder.build();
    }

    /**
     * Windows 인증서 저장소를 QA TLS 연결에 사용할 수 있도록 SSLContext를 생성합니다.
     *
     * @return Windows SSLContext, 사용할 수 없으면 null
     */
    private SSLContext selectWindowsSslContext() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            return null;
        }
        try {
            KeyStore windowsRoot = KeyStore.getInstance("Windows-ROOT");
            windowsRoot.load(null, null);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
            );
            trustManagerFactory.init(windowsRoot);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * QA 대상의 요청 프로필에 맞는 HTTP 클라이언트를 선택합니다.
     *
     * @param source 지자체 URL seed
     * @return 요청 클라이언트
     */
    private HttpClient selectHttpClient(SourceSeed source) {
        if (usesSessionBrowser(source)) {
            return sessionBrowserHttpClient;
        }
        if ("TLS12_BROWSER".equals(source.requestProfileCode())) {
            return tls12HttpClient;
        }
        return "BROWSER_HTTP1".equals(source.requestProfileCode()) ? browserHttp1Client : defaultHttpClient;
    }

    private record ParserProfile(
            String profileCode,
            String listItemSelector,
            String titleSelector,
            String dateSelector,
            String linkSelector,
            String datePattern,
            String linkStrategyCode,
            String linkFunctionName,
            Integer linkFunctionArgumentCount,
            String linkUrlTemplate
    ) {

        /**
         * 운영 수집기와 동일한 파서 프로필 row로 변환합니다.
         *
         * @return 운영 파서 프로필 row
         */
        LocalGovernmentNoticeParserProfileRow toRow() {
            return new LocalGovernmentNoticeParserProfileRow(
                    profileCode, profileCode, "GENERIC_TABLE",
                    listItemSelector, titleSelector, dateSelector, linkSelector, datePattern,
                    "HTML", null, null, null, null, null,
                    linkStrategyCode, linkFunctionName, linkFunctionArgumentCount, linkUrlTemplate, true
            );
        }
    }

    private record SourceSeed(
            String publicCode,
            String sidoName,
            String sigunguName,
            String institutionName,
            String noticeUrl,
            String collectionEndpointUrl,
            String requestProfileCode,
            String pageTypeCode
    ) {

        /**
         * 별도 endpoint가 있으면 endpoint를, 없으면 사용자용 목록 URL을 요청합니다.
         *
         * @return 실제 QA 요청 URL
         */
        String requestUrl() {
            return collectionEndpointUrl == null || collectionEndpointUrl.isBlank()
                    ? noticeUrl : collectionEndpointUrl;
        }
    }

    private record JsonQaProfile(
            String profileCode,
            String itemsPath,
            String titleField,
            String dateField,
            String linkField,
            String linkTemplate
    ) {
    }

    private record ResolvedLink(String rawLink, String absoluteLink) {
    }

    private record ProfileResult(
            String profileCode,
            int discoveredCount,
            int validCount,
            int invalidCount,
            List<String> samples
    ) {
    }

    private record QaHttpResponse(int statusCode, byte[] body, URI uri) {
    }

    private record QaResult(
            String publicCode,
            String sidoName,
            String sigunguName,
            String institutionName,
            String noticeUrl,
            String statusCode,
            String profileCode,
            int discoveredCount,
            int validCount,
            int invalidCount,
            Integer httpStatus,
            String errorCode,
            String errorMessage,
            String samples,
            long elapsedMillis,
            String sourceBoardTypeCode,
            String collectionPolicyCode,
            String semanticStatusCode,
            String semanticReasonCode
    ) {
    }

    private record SemanticQaResult(
            String sourceBoardTypeCode,
            String collectionPolicyCode,
            String semanticStatusCode,
            String semanticReasonCode
    ) {
    }
}
