package com.uline.table.indexer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.web.client.RestTemplate;

public class GitRepoService {

    class PullRequestInfo {

        LocalDateTime closedDate;
        String description;
        String developer;
        int id;

        public String reponame;
        public String url;
        List<WorkItem> workItems = new ArrayList<>();

        /**
         * @return
         */
        public boolean hasBugOrStory() {
            return workItems.stream().anyMatch(wi -> storyItemTypes.contains(wi.type));
        }

        public boolean isComplete() {

            boolean allComplete = workItems.stream()
                .allMatch(wi -> "Closed".equals(wi.status) || "Resolved".equals(wi.status));

            boolean hasBugOrStory = hasBugOrStory();

            return !workItems.isEmpty() && hasBugOrStory && allComplete;
        }

        @Override
        public String toString() {

            return ToStringBuilder.reflectionToString(this);
        }
    }

    class WorkItem {

        Set<String> devs = new HashSet<>();
        String id;
        String parentUrl;
        String status;
        String title;
        String type;
        String url;

        @Override
        public String toString() {

            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static final String BASE_URL = "http://tfs.ulinedm.com:8080/tfs/Uline/";

    private static Set<String> storyItemTypes = new HashSet<>();

    static {
        storyItemTypes.add("Bug");
        storyItemTypes.add("User Story");
    }

    public String repoId;
    public RestTemplate rt;

    private void fetchPullRequestWorkItems(PullRequestInfo pr) {

        int pullRequestId = pr.id;

        String url = "http://tfs.ulinedm.com:8080/tfs/Uline/_apis/git/repositories/" + repoId
        + "/pullrequests/" + pullRequestId + "/workitems";

        Map result = rt.getForObject(url, HashMap.class);

        int count = (Integer) result.get("count");

        List<Map> value = (List<Map>) result.get("value");

        for (int i = 0; i < count; i++) {

            String workItemUrl = (String) value.get(i).get("url");

            WorkItem wi = fetchWorkItem(workItemUrl);

            pr.workItems.add(wi);
        }

        if (!pr.hasBugOrStory() && !pr.workItems.isEmpty() && null != pr.workItems.get(0).parentUrl) {
            WorkItem parent = fetchWorkItem(pr.workItems.get(0).parentUrl);
            pr.workItems.add(parent);
        }
    }

    /**
     * @param workItemUrl
     * @return
     */
    private WorkItem fetchWorkItem(String workItemUrl) {
        Map workItem = rt.getForObject(workItemUrl + "?$expand=relations", HashMap.class);
        Map fields = (Map) workItem.get("fields");

        Object workItemType = fields.get("System.WorkItemType");
        Object workItemState = fields.get("System.State");


        WorkItem wi = new WorkItem();
        wi.status = workItemState.toString();
        wi.type = workItemType.toString();
        wi.url = workItemUrl;
        wi.title = String.valueOf(fields.get("System.Title"));
        wi.id = workItem.get("id").toString();

        if (!storyItemTypes.contains(wi.type)) {
            List<Map> relations = (List) workItem.get("relations");
            if (null != relations) {
                for (Map relation : relations) {
                    if ("System.LinkTypes.Hierarchy-Reverse".equals(relation.get("rel"))) {
                        wi.parentUrl = relation.get("url").toString();
                    }
                }
            }
        }

        return wi;
    }

    /**
     * @param asOfDateTime TODO
     *
     */
    public List<PullRequestInfo> processRepo(LocalDateTime asOfDateTime) {

        Map result = rt.getForObject(
            BASE_URL + "_apis/git/repositories/" + repoId
                + "/pullRequests?status=Completed&creatorId=&reviewerId=&%24top=100&%24skip=0&sourceRefName=&targetRefName=",
            HashMap.class);

        List<Map> value = (List<Map>) result.get("value");

        List<PullRequestInfo> prs = new ArrayList<>();

        for (int i = 0; i < value.size(); i++) {
            Map rawPullRequestData = value.get(i);

            PullRequestInfo pr = mapPullRequest(rawPullRequestData);

            if (pr.closedDate.isAfter(asOfDateTime)
                && "refs/heads/master".equals(rawPullRequestData.get("targetRefName"))) {
                prs.add(pr);
            }
        }

        prs.stream().forEach(this::fetchPullRequestWorkItems);

        return prs;
    }

    /**
     * @param rawPullRequestData
     * @return
     */
    private PullRequestInfo mapPullRequest(Map rawPullRequestData) {
        PullRequestInfo pr = new PullRequestInfo();

        Map creator = (Map) rawPullRequestData.get("createdBy");
        Map repository = (Map) rawPullRequestData.get("repository");

        String closedDate = String.valueOf(rawPullRequestData.get("closedDate"));
        LocalDateTime datetime =
            LocalDateTime.parse(closedDate.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        pr.closedDate = datetime;
        pr.developer = creator.get("displayName").toString();
        pr.description = String.valueOf(rawPullRequestData.get("description"));
        pr.url = rawPullRequestData.get("url").toString();
        pr.reponame = repository.get("name").toString();
        pr.id = Integer.valueOf(rawPullRequestData.get("pullRequestId").toString());
        return pr;
    }
}
