package org.views;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.example.SlackUtils.countUtils;
import static org.example.SlackUtils.getNextKeyUtils;
import static org.messaging.Blocks.TextImageBlock;
import static org.messaging.Blocks.ThreeButtonBlock;
import static org.messaging.PostUpdateUtils.updateResponseAsync;

import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.PlainTextInputElement;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * QUERY INTERACTIVE VIEW
 * - query page
 * - get count query
 * - get next key query
 * - delete this session
 * - close this session
 */
@Slf4j
@Builder
public class QueryView {

    @NonNull
    private final String channelId;

    @NonNull
    private String requestId;

    @NonNull
    private final String messageTs;

    public enum ViewType {
        GET_NEXT_REQUEST,
        GET_NEXT_RESPONSE,
        GET_COUNT_REQUEST,
        GET_COUNT_RESPONSE,
        NO_QUERY,
    }

    @Builder.Default
    private static final String REDIS_LOGO_URL =
        "https://avatars.githubusercontent.com/u/1529926?s=200&v=4";

    @Builder.Default
    private final List<LayoutBlock> blocks = new ArrayList<>();

    /**
     * updates query views
     *
     * @param viewType  - type of view
     * @param queryText - query text, depends on view type
     */
    public void start(ViewType viewType, String queryText) {
        log.info("QueryView for channel " + channelId);
        blocks.add(
            HeaderBlock
                .builder()
                .text(PlainTextObject.builder().text("Request Id : " + requestId).build())
                .build()
        );

        blocks.add(DividerBlock.builder().build());
        String headerText = "", inputActionId = "", inputPlaceHolder = "";
        if (viewType == ViewType.GET_NEXT_REQUEST || viewType == ViewType.GET_NEXT_RESPONSE) {
            headerText = "Get Next Request for " + requestId;
        } else if (
            viewType == ViewType.GET_COUNT_REQUEST || viewType == ViewType.GET_COUNT_RESPONSE
        ) {
            headerText = "Get Count Request for " + requestId;
        }

        if (viewType == ViewType.GET_NEXT_REQUEST) {
            inputActionId = "buttonBlock-query-view-getnext-response-" + randomAlphanumeric(10);
            inputPlaceHolder = "[Prefix Key] [Count of Keys]\n\nEx. 'HTTPSESSION 10'";
        } else if (
            viewType == ViewType.GET_COUNT_REQUEST || viewType == ViewType.GET_COUNT_RESPONSE
        ) {
            inputActionId = "buttonBlock-query-view-getcount-response-" + randomAlphanumeric(10);
            inputPlaceHolder =
                "[Prefix Key] [Count of Keys]\n\nEx. '!root 10'\n\nUse '!root' for root\n\nDefault value of Count of Keys is 10";
        }

        if (viewType != ViewType.NO_QUERY) {
            blocks.add(
                HeaderBlock
                    .builder()
                    .text(PlainTextObject.builder().text(headerText).build())
                    .build()
            );

            if (viewType == ViewType.GET_COUNT_RESPONSE || viewType == ViewType.GET_COUNT_REQUEST) {
                blocks.addAll((countUtils(requestId + " " + queryText)));
            } else if (viewType == ViewType.GET_NEXT_RESPONSE) {
                blocks.addAll((getNextKeyUtils(requestId + " " + queryText)));
            }

            blocks.add(
                InputBlock
                    .builder()
                    .label(PlainTextObject.builder().text("Insert key to search: ").build())
                    .optional(false)
                    .dispatchAction(true)
                    .element(
                        PlainTextInputElement
                            .builder()
                            .actionId(inputActionId)
                            .placeholder(PlainTextObject.builder().text(inputPlaceHolder).build())
                            .multiline(true)
                            .build()
                    )
                    .build()
            );
        }

        blocks.add(
            ThreeButtonBlock(
                "Get Count",
                "query-view-getcount-request",
                "primary",
                "Delete Session",
                "delete-session",
                "danger",
                "Close",
                "delete-message",
                "danger"
            )
        );
        updateResponseAsync(blocks, channelId, requestId, messageTs);
    }
}
