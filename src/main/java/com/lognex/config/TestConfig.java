package com.lognex.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Setter
@Getter
public class TestConfig {
    private List<GroupDefinition> groups;
}

