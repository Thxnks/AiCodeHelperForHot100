package com.yupi.aicodehelper.agent.core;

import java.util.ArrayList;
import java.util.List;

public class TaskRecord {

    private long id;
    private String subject;
    private String description;
    private TaskStatus status;
    private List<Long> blockedBy = new ArrayList<>();
    private List<Long> blocks = new ArrayList<>();
    private String owner;

    public TaskRecord() {
    }

    public TaskRecord(long id, String subject, String description, TaskStatus status, List<Long> blockedBy,
                      List<Long> blocks, String owner) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.blockedBy = blockedBy == null ? new ArrayList<>() : new ArrayList<>(blockedBy);
        this.blocks = blocks == null ? new ArrayList<>() : new ArrayList<>(blocks);
        this.owner = owner;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public List<Long> getBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(List<Long> blockedBy) {
        this.blockedBy = blockedBy == null ? new ArrayList<>() : new ArrayList<>(blockedBy);
    }

    public List<Long> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Long> blocks) {
        this.blocks = blocks == null ? new ArrayList<>() : new ArrayList<>(blocks);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isReady() {
        return status == TaskStatus.PENDING && blockedBy.isEmpty();
    }
}
