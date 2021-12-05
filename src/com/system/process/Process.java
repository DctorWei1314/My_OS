package com.system.process;

import com.system.UI.Init_UI;

import java.util.LinkedList;

/*
尽可能的模拟实际的操作系统环境
每一个进程资源都有独立内存空间资源，且通过中断正在sleep的CPUwithTimer线程完成模拟系统调用，Process与Kernel无任何关系,不需要知道系统调用的细节
*/
public class Process implements Runnable{
    public SystemCall systemCall;
    public int parameter;
    public byte[] privateSpace;//系统调用的类型与参数信息需要现从此处获得
    LinkedList<Task> taskList = new LinkedList<Task>();//Process的动作任务列表,根据该列表决定Process何时create_process,exit_process,send,receive
    //模拟进程私有的目态用户空间

    public Pcb pcb;
    //模拟进程在管态的pcb控制块
    public Process(int pid,int father_pid,int priorty){
        pcb = new Pcb(pid,father_pid,priorty);
    }

    public void addTask(Task task){
        taskList.add(task);
    }
    //由主线程根据UI反馈实时动态添加任务
    @Override
    public void run() {
        while (true){
//            System.out.println("PCB.stop:"+pcb.stop);
            System.out.println("1");
            if(pcb.stop){
                try {
                    synchronized (pcb.processRuning){
                        pcb.processRuning.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Task task =null;
            for(int i=0;i<taskList.size();i++){
                if(taskList.get(i).isDone){
                    taskList.remove(i);
                    continue;
                }
                if(taskList.get(i).time <= pcb.livingTime){
                    task = taskList.get(i);
                    System.out.println(task.type);
                    break;
                }
            }
            //系统调用
            if(task!=null){
                dealsystemCall(task);
            }
            if(pcb.exit)break;
        }
}
    private void dealsystemCall(Task task){
        task.isDone = true;
        systemCall = task.type;
        if(systemCall == SystemCall.create_process){
            privateSpace = task.content;
            parameter = task.parameter;
            if(!gotoKernel(task))return;
            try {
                synchronized (pcb.processRuning){
                    pcb.processRuning.wait();
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        else if(systemCall == SystemCall.exit_process){
            if(!gotoKernel(task))return;
            pcb.exit = true;
        }
        else if(systemCall == SystemCall.send){
            privateSpace = task.content;
            parameter = task.parameter;
            if(!gotoKernel(task))return;
            try {
                synchronized (pcb.processRuning){
                    pcb.processRuning.wait();
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        else if(systemCall == SystemCall.receive){
            if(!gotoKernel(task))return;
            try {
                synchronized (pcb.processRuning){
                    pcb.processRuning.wait();
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        else if(systemCall == SystemCall.require_disk_read){
            parameter = task.parameter;
            if(!gotoKernel(task))return;
            try {
                synchronized (pcb.processRuning){
                    pcb.processRuning.wait();
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }else if(systemCall == SystemCall.require_disk_write){
            parameter = task.parameter;
            if(!gotoKernel(task))return;
            try {
                synchronized (pcb.processRuning){
                    pcb.processRuning.wait();
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    private boolean gotoKernel(Task task){
        Init_UI.CUP_List.get(pcb.CPU_ID-1).setLastRequsetPid(pcb.pid);
        if(Init_UI.CUPRUNNIG_List.get(pcb.CPU_ID-1).getState() == Thread.State.TIMED_WAITING){
            Init_UI.CUPRUNNIG_List.get(pcb.CPU_ID-1).interrupt();
        }
        else {
            task.isDone = false;
            return false;//如果CUPWithTime已经醒了,先不执行
        }
        return true;
    }
    @Override
    public String toString() {
        return "PID:"+pcb.pid+"#"+pcb.livingTime+"#"+pcb.priority+"        ";
    }
}
