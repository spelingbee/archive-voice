package com.backapi.backapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    private String answer;
    private String question;

    @JsonProperty("session_id")
    private Long sessionId;

    private List<SourceChunkResponse> sources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceChunkResponse {
        @JsonProperty("document_id")
        private Integer documentId;
        private String filename;
        @JsonProperty("chunk_index")
        private Integer chunkIndex;
        @JsonProperty("chunk_text")
        private String chunkText;
        private Double similarity;
    }
}
