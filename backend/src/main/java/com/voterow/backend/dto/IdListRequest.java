package com.voterow.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class IdListRequest {
    private List<Long> userIds;
}