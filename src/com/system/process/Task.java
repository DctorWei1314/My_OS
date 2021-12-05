package com.system.process;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Task {
    public boolean isDone =false;
    SystemCall type;//任务类型,根据任务类型决定操作行为
    byte[] content;
    int parameter;
    int time;//决定任务在process的生命周期中的那个时间点执行
    public Task(SystemCall type,int time){
        this.type = type;
        this.time = time;
    }
    public Task(SystemCall type,int time,int parameter){
        this.type = type;
        this.time = time;
        this.parameter = parameter;
    }
    public Task(SystemCall type,int time,int parameter,byte[] content){
        this.type = type;
        this.time = time;
        this.parameter = parameter;
        this.content = content;
    }
}