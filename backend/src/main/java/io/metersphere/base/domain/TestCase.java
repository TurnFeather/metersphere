package io.metersphere.base.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class TestCase implements Serializable {
    private String id;

    private String nodeId;

    private String nodePath;

    private String projectId;

    private String name;

    private String type;

    private String maintainer;

    private String priority;

    private String method;

    private String prerequisite;

    private Long createTime;

    private Long updateTime;

    private String testId;

    private Integer sort;

    private Integer num;

    private String otherTestName;

    private String reviewStatus;

    private String tags;

    //added by yrc at 20210307
    private String networkId;

    private String networkDescription;

    private String networkConstraint;

    private String applicableVersion;

    private String deviceType;

    private String boardType;

    //end

    private static final long serialVersionUID = 1L;
}