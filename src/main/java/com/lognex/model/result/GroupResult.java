package com.lognex.model.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupResult {
    private String groupName;
    private int testCount;
    private List<Map<String, Object>> tests;
}
