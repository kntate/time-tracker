package com.uline.table.indexer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import com.uline.table.indexer.GitRepoService.PullRequestInfo;
import com.uline.table.indexer.GitRepoService.WorkItem;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class GitRepoServiceTest {

    private RestTemplate rt;
    private GitRepoService service;

    private void printPullRequestInfo(PullRequestInfo pr) {
        System.out
            .print(pr.id + "|" + pr.developer + "|" + pr.closedDate + "|" + pr.isComplete() + "|");
        pr.workItems.forEach(
            wi -> System.out.print(wi.id + " " + wi.type + " " + wi.status + " " + wi.title + ","));
        System.out.println();
    }

    @Before
    public void setUp() {

        Logger logger =
            (Logger) LoggerFactory.getLogger(org.springframework.web.client.RestTemplate.class);
        logger.setLevel(Level.OFF);

        rt = new RestTemplate();
        ClientHttpRequestInterceptor clientHttpRequestInterceptor = new ClientHttpRequestInterceptor() {

            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
                HttpHeaders headers = request.getHeaders();
                headers.add("Authorization", "Basic X2plbmtpbnM6amVua2luczNydiFjMyE=");
                return execution.execute(request, body);
            }

        };
        rt.setInterceptors(Arrays.asList(clientHttpRequestInterceptor));

    }

    @Test
    public void testStuff() {

//        String[] repoIds = {"e0529f65-3775-4a8d-a1a3-b77338cc8a4e",
//            "123f52d2-9655-4341-9b99-c4f1e6cd77fc", "50b1667c-ad14-4029-99e1-319c763a48a5",
//            "f86bb0e8-dd82-40d2-ba78-28e9139a1caf", "58b3712e-d332-4185-9877-463fd5ce25e1"};


        String communicationService = "1c85df37-c121-4766-9793-48893cd5ce98";
        String wmsUtilities = "58b3712e-d332-4185-9877-463fd5ce25e1";
        String shippingDoc = "848f0456-ce41-4478-a59d-6de84f8386e1";
        String as400api = "406311d4-cf96-4dc7-829b-381305bf170d";
        String[] repoIds = {shippingDoc};

        //String[] repoIds = {"981f004b-ad03-43d3-a835-c46464b6762b"}; // po-services

        for (String repoId : repoIds) {
            service = new GitRepoService();
            service.repoId = repoId;
            service.rt = this.rt;

            List<PullRequestInfo> prs = service.processRepo(LocalDateTime.of(2019, 2, 15, 12, 0));



            prs.stream().forEach(pr -> {
                if (!pr.isComplete()) {

                    String prViewUrl =
                        service.BASE_URL + "Uline/_git/" + pr.reponame + "/pullrequest/" + pr.id;

                    System.out.println(pr.closedDate + " " + pr.developer + " " + prViewUrl
                            + " is tied to an incomplete work item!"
                        // + "\n" + pr.description + "\n" + pr + "\n"
                    );

                    Stream<WorkItem> wis = pr.workItems.stream()
                        .filter(wi -> !"Closed".equals(wi.status) && !"Resolved".equals(wi.status));

                    wis.forEach(System.out::println);

                    System.out.println();
                }
            });



            Map<String, WorkItem> workItems = new TreeMap<>();

            prs.stream().forEach(pr -> pr.workItems.stream().forEach(wi -> wi.devs.add(pr.developer)));
            prs.stream().forEach(pr -> pr.workItems.stream().forEach(wi -> workItems.put(wi.id, wi)));

            workItems.entrySet().stream().forEach(wi -> {
                WorkItem value = wi.getValue();
                if (!"Task".equals(value.type)) {
                    System.out.println("http://tfs.ulinedm.com:8080/tfs/Uline/Uline/_workitems?id=" + value.id
                        + " |" + value.status + "|" + value.type + "|" + value.title + "|" + value.devs);
                }
            });

            System.out.println();
        }
    }

}
